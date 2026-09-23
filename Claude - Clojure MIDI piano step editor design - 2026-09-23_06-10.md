# Clojure MIDI piano step editor design

**Exported with:** [AI Chat Exporter](https://ai-chat-exporter.covai.org)  
**Source:** Claude  
**Date:** 9/23/2026, 6:10:52 AM  
**Link:** [https://claude.ai/chat/b2cf4923-dfe3-4dba-b3f8-8277bc605f44?artifact=18af0436-ff54-4790-9398-eaaf9ad0912e](https://claude.ai/chat/b2cf4923-dfe3-4dba-b3f8-8277bc605f44?artifact=18af0436-ff54-4790-9398-eaaf9ad0912e)  
**Model:** claude-sonnet-5  
**Method:** API  

## Prompt:
Please don't write any code.  I want you to help me think through the design of an application in Clojure for step editing of MIDI sequences for piano music, where each hand is recorded separately by playing the chords from written sheet music.  The editor should allow insertion of duration values between notes of a chord for cases where some notes are still held when one is released.  The goal is for speed in user editing of the sequence for each hand.  For separated chords they would just use a numeric keypad to enter durations (ex: .8 for a dotted 8th note).  To insert a duration between notes of a chord, they would press the left arrow, hear each note, and place the duration at the appropriate location so the midi sequence will play what was written in the sheet music.  How does immutability help in this type of application when there is a lot of editing?

## Response:
Let me work through the data model first, since the immutability question only makes sense in light of what's actually being edited.

## Shaping the domain model

For a single hand's sequence, the natural unit is a **chord group**: a cluster of notes that were struck together (or nearly together) and that may be released at different times. Rather than modeling this as a flat MIDI event stream (note-on/note-off pairs with absolute times), it helps editing to model it closer to how a performer thinks:

- A sequence is an ordered list of chord-groups.
- Each chord-group has a set of notes, and a list of "release points" — durations at which some subset of notes lets go, ending when all notes have been released.
- For a plain chord with no internal splitting, there's just one release point: all notes end together after the entered duration.
- When you step into a chord with the left arrow and insert a duration, you're really doing this: pick a subset of the currently-held notes, give them a release duration shorter than the rest, and the group now has two (or more) release points instead of one.

So the "insert duration between notes of a chord" operation is a structural edit on a small nested value — you're splitting one release-point bucket into two and reassigning notes between them. This is exactly the kind of operation that's clean to express as "take this value, produce a new value" — and that's where immutability earns its keep.

## Why immutability specifically helps here

**1. The edit operation is naturally a pure function, and that matches the interaction model.**
Every keystroke — a digit on the numeric keypad, a left-arrow to descend into a chord, an insertion of a sub-duration — is a small, well-defined transformation: `(new-sequence, new-cursor) = edit(old-sequence, old-cursor, keystroke)`. If the sequence is an immutable value, each of these handlers can be a pure function you can test in isolation with a literal chord-group as input and an expected chord-group as output. For a tool whose entire value proposition is *correctness of timing*, being able to write "given this exact chord shape and this exact keystroke, the result must be exactly this" as a deterministic unit test is huge. You are not testing against mutable state that depends on prior calls having mutated things correctly.

**2. Undo/redo is nearly free.**
With mutable editing (splicing linked lists of MIDI events, adjusting pointers), undo means writing an inverse for every operation, or an explicit command/memento pattern, and it's easy to get subtly wrong on the composite operations (like a chord split, which touches multiple notes' release assignments at once). With persistent data structures, undo is just: keep a vector of previous whole-sequence values (or a zipper history) and go back to one. Structural sharing means storing the last N states doesn't cost you N full copies of the piece — only the parts that actually changed are duplicated. For a step editor where the user will make and reconsider many small chord-splitting decisions in a row, cheap undo is not a nicety, it's central to usable "speed editing."

**3. No inconsistent intermediate states.**
A chord split touches several things together: it removes a note from one release-bucket, creates or extends another bucket, and possibly re-sorts release order. If you did this with in-place mutation you could observe (or crash on) a half-updated chord if something goes wrong mid-edit, or if playback reads the structure while editing is in progress. With immutable values, the transformation produces a complete, valid new chord-group in one step from an old, complete, valid one — there's no window where it's half-old/half-new. That invariant ("every value on the undo stack is a fully valid sequence") is worth a lot in a tool whose failure mode is "silently produces wrong MIDI."

**4. Safe handoff to the playback thread.**
Step editing tools like this live and die by instant audio feedback — pressing left-arrow to "hear each note" as you said. That playback (probably on a timer thread or via `future`/`core.async`) needs to read the sequence data without racing the UI thread that's still processing keystrokes. If the sequence is immutable, you just hand the player a reference to the current value at the moment playback starts; the editor can keep mutating "the current value" in its atom afterward, and the player's snapshot is guaranteed never to change underneath it. No locks, no copying, no defensive `deep-copy` calls before playback — which matters when you're trying to get the preview note to sound the instant a key is pressed.

**5. Cheap "what if" exploration.**
Determining exactly where to drop a duration inside a chord (by ear) is inherently a bit exploratory — the user may try a split, listen, and back it out. Because building a candidate new state doesn't touch the old one, you can construct a speculative edit, play it, and simply discard it (never `reset!` the atom) if it's wrong, with zero cleanup cost. That maps very naturally onto "press left arrow to audition placements" as an interaction pattern.

**6. Zippers pair well with this model for cursor-based navigation.**
Since editing is about a cursor moving through a tree (piece → chord-group → note/release-bucket), a functional zipper is a good fit: moving the cursor produces a new zipper value pointing at a new location, without mutating the underlying tree. That's the standard "immutable data + immutable cursor" combo, and it lets you keep the whole navigation/edit history (including cursor position) as an ordinary value you can snapshot for undo, rather than juggling separate mutable "current position" state that has to be kept in sync with a separately-mutated tree.

**7. Equality and diffing are cheap and correct.**
Persistent structures give you structural equality (and Clojure caches hashes on them), so "has anything changed since the last commit" or "is this edit a no-op" are just `=` checks, not custom deep-comparison code you have to write and maintain — useful for things like dirty-flagging a bar for re-save, or detecting that an inserted duration equals zero and should be discarded.

## The overall shape this suggests

Concretely, this points toward: an atom holding `{:sequence ..., :cursor ...}` as one immutable value; keystroke handlers as pure functions `state -> state`; `swap!` to apply them; a history vector of prior states for undo; and playback that just closes over `(:sequence @state)` at the moment it's triggered. None of that is exotic — it's the ordinary Clojure app-state idiom — but it's worth naming explicitly here because the *reason* it fits so well is that the domain object (a hand's chord sequence with sub-chord timing splits) is exactly the kind of small-but-structurally-fiddly nested value that immutable edits, undo-by-history, and safe concurrent reads are designed for.

Happy to go deeper on any one piece next — e.g., how exactly to represent release-buckets within a chord-group so that "insert duration between notes" is a one-step transformation rather than something that needs renumbering, or how the zipper should be shaped so left/right arrow both move between chords and drill into a chord's internal notes.

## Prompt:
Here is a similar but related project.  I want to use a MIDI keyboard as a chord keyset for a system that simulates Douglas Engelbart's NLS system.  5 MIDI notes would be chosen (ex: E F G A B) so if the user holds down a chord of any of those five notes, then releases them all, the notes that were pressed down between the times when no notes were held down would form a 5 bit value that would become a chord event for the system.  How would Clojure model this data stream and interpret the user's chords?

## Response:
Good — this is a cleaner, more classic problem: turning a raw MIDI event stream into discrete chord codes via a state machine. Let me lay out the pieces.

## The core abstraction: a chord "episode"

Define a fixed mapping from the five chosen pitches to bit positions — say E→bit 0, F→bit 1, G→bit 2, A→bit 3, B→bit 4 — as a simple lookup table. This table is the single source of truth for interpreting notes as chord bits, and everything downstream refers to it rather than hardcoding pitch numbers.

A "chord episode" is the span of time between two moments when none of these five keys are held. Within an episode you need to track two different things, which is a common point of confusion:

- **currently held** — which of the five are down *right now* (needed to know when the episode ends)
- **accumulated** — the union of which of the five were down *at any point* during the episode (this is what becomes the bit value)

These are both just sets, and the second only grows during an episode; it resets to empty when a new episode starts.

## Modeling it as a step function

The natural Clojure shape for this is a small, pure state-transition function: given the current state (held set + accum set) and one incoming MIDI event, produce the next state, and — only on the specific transition where "held" goes from non-empty to empty — also produce an emission: the finished chord code.

So the function has a signature like: *state, event → new-state, maybe-an-emitted-chord*. That's a textbook Mealy machine, and it's a good fit for a few reasons specific to this problem:

- Note-on events only ever add to both `held` and `accum`.
- Note-off events only ever remove from `held`, never from `accum`.
- The emission only fires on the transition where `held` becomes empty, at which point you read `accum`, convert it to an integer via bit-shifting each present pitch's bit position, and reset both sets for the next episode.
- Events for notes outside the five-note set pass through untouched — they don't affect `held`/`accum` at all, so they can be threaded past this function to whatever else consumes ordinary MIDI (this matters if the same keyboard is also being used for normal note input elsewhere in your system).

Because this function's entire behavior is described by those few rules, it's small enough to reason about completely and to unit-test with literal event sequences — you can construct "press E and G, release G, release E" as a plain vector of event maps and assert the exact chord code that comes out, without touching real hardware. That matters a lot here because chord-keyset timing bugs (an event arriving in an unexpected order, a stuck note) are exactly the kind of thing you want caught by a deterministic test rather than by a user noticing NLS is misbehaving.

## Wiring it to a live stream

For live input, you'd have a MIDI receiver callback (via `javax.sound.midi` or a wrapper library) pushing note-on/note-off maps onto a core.async channel as they arrive. Then you process that channel through the step function, maintaining state either:

- as a transducer with the step function's state threaded internally (still written as the same pure state→state logic, just wrapped for streaming), or
- as a simple loop reading off the channel, folding events into an atom holding `{:held ... :accum ...}`, and putting emitted chord codes onto an output channel whenever the step function signals one.

The output is a channel (or lazy seq, for offline replay/testing) of chord codes 0–31 with timestamps — that's the event stream the rest of your NLS simulation consumes as command input, decoupled entirely from MIDI specifics.

## A few modeling decisions worth pinning down explicitly

- **Passthrough vs. exclusivity**: are the five chosen notes *only* chord-keyset input, or can they also sound as normal notes elsewhere? If they're dedicated (which matches Engelbart's original one-hand chord keyset being a separate device from the typewriter-like keyboard), the model is simpler — those note numbers never reach ordinary note-handling logic at all.
- **Re-presses within an episode**: if the user releases one note but keeps others down, then presses it again before the episode ends, nothing changes — it's already in `accum`, and sets don't double-count. No special case needed.
- **Firing point**: you've defined the episode as bounded by all-five-up on both ends, with the code equal to the union of everything pressed in between. That's a reasonable, easy-to-explain rule and is genuinely how you described it. It's worth knowing this differs slightly from Engelbart's original chord keyset behavior, where the code is typically latched at the *first* release after a fully-formed chord (so a "roll" of keys pressed slightly out of sync doesn't retrigger). If you ever want to switch to that stricter interpretation, it only changes when the step function reads `accum` and resets state — it's a one-line change to the transition rule, not a restructuring, because the rest of the model (bit table, held/accum sets, emission channel) stays identical either way.
- **Debounce/noise**: real MIDI hardware occasionally sends a near-instant on/off pair from a light touch. If that's a problem in practice, it's naturally handled as a small filter *before* the step function (dropping events whose on/off gap is below some threshold), keeping the FSM itself simple.

This keeps the whole interpretation layer as one small, pure, well-isolated piece — a fixed pitch→bit table, a several-line transition rule, and a channel of resulting chord codes — with all the MIDI I/O plumbing kept firmly on the outside of it.

## Prompt:
What is your reference for "Engelbart's original chord keyset behavior"

## Response:
Fair question to push on — let me check that rather than just standing behind it from memory.

I don't have a solid citation for that claim, and having now checked, I think I stated it with more confidence than it deserved — and it was likely wrong in the specific way I framed it. Let me correct the record.

The Wikipedia article on chorded keyboards, describing Engelbart's original design, actually confirms the interpretation *you* proposed, not the "first release" variant I described: a keypress is not registered until all depressed keys are released, such that all keys do not need to be pressed at once as long as one key remains depressed throughout, and when the last key is released the corresponding character is output based on all keys that had been depressed since the last character was registered. The article gives a concrete example: pressing and holding key 1, briefly pressing and releasing key 2, then releasing key 1 produces the same output as pressing both simultaneously — i.e., exactly your "union of everything pressed during the episode, fire on all-released" model.

So the "fire on first release" behavior I described isn't Engelbart's design at all — it's a *different* chording scheme. It does exist in the literature (one of the patents I found describes almost exactly that alternative, where the first key released generates the chord signal, with the chord made up of keys pressed just prior to that release), but I had no business attributing it to Engelbart specifically. That looks like a conflation on my part between different chord-keyboard patents I have some general familiarity with, stated as if it were a specific historical fact about the NLS keyset. That was an overconfident, unverified claim, and I shouldn't have presented it as settled history without checking first.

Net effect on the actual design advice: it's good news, not bad — the model you described and that I built the state machine around (fire on all-keys-up, code = union of everything pressed during the episode) **is** the historically accurate Engelbart behavior. The "alternative" I offered as a variant to consider isn't a documented Engelbart mode at all, so you can disregard that part of my earlier answer rather than treating it as a real design option grounded in the original device.

## Prompt:
I appreciate your honesty and I can understand how cosine similarity between patents could conflate your understanding of this concept.  How can you prevent such a hallucination from happening again?

## Response:
Realistically, I can't guarantee it won't happen again — I don't retain anything from this conversation into future ones, so this specific correction doesn't "teach me a lesson" that persists. What I can do is describe the actual mechanism that produced the error and where the real safeguards are.

**What likely happened.** Specific attributed technical claims — "X invented/designed/built Y to work like Z" — are exactly the category where my training can produce a fluent, confident-sounding answer that's actually a blend of adjacent sources: Engelbart's device, several later chord-keyboard patents, general chording-keyboard folk knowledge. Nothing in how that sentence gets generated distinguishes "I recall this specific fact from a specific reliable source" from "this is a plausible-sounding synthesis of related things I've seen." Fluency and confidence of phrasing are not correlated with accuracy in the way they'd be from a careful human writer citing a source they actually have open in front of them.

**The structural fix, and its limit.** The actual corrective isn't "try harder to remember correctly" — it's noticing *in the moment* when a claim is the specific, checkable, attributed kind (a named person did a specific thing a specific way) rather than the general/conceptual kind (how FSMs work, how sets behave), and routing the former through search before stating it as fact rather than after being questioned on it. That's a real, usable heuristic, and I'm supposed to apply it by default for exactly this kind of claim rather than waiting to be asked. I didn't apply it here — I stated a specific historical-design claim from memory as settled fact. That's the actual failure, not a one-off fluke to shrug off.

The honest limit: I can't promise perfect discipline about this any more than a person can promise never to misremember something under time pressure. What's reliable is the fallback you just exercised — push back when a claim seems load-bearing or oddly specific, and I'll check it rather than defend it. That verification loop is a better guarantee than my own self-monitoring, because it doesn't depend on me correctly noticing my own confidence was miscalibrated in the first place.

One practical thing on your end: if you're relying on historical/attribution claims like this for something written up or published, it's worth treating anything I say about "who designed what, specifically how" as a lead to verify rather than a citation, unless I've actually shown you a search result backing it — same as you'd treat an uncredentialed claim from any single source.

## Prompt:
For a person coming to Clojure from non-functional languages, how would you design a learning plan that helps the person map their programming knowledge to functional design concepts?  Suggest projects that may help in this process?

## Response:
Here's a plan built around translation rather than replacement — mapping what the person already knows onto the functional equivalent, rather than asking them to forget their existing mental models. Most Clojure-learning-plan failures come from either drowning someone in "everything is different" (paralyzing) or hiding the differences behind familiar syntax (produces people who write Java-in-parens).

## The mapping strategy, concept by concept

**1. Variables → let-bound names and function locals**
Their instinct: a variable is a labeled box you can put different things into over time. The mapping: in Clojure, a name is a label for a value, permanently, at that point in the code — like a mathematical variable in an equation, not a storage cell. `let` doesn't allocate a box; it just gives a name to a value for the duration of a block. The "reassignment" instinct gets redirected to: define a *new* name (often via recursion) rather than mutate an old one.

**2. Loops → recursion and higher-order functions**
This is usually the biggest early friction point. Map it in two stages, because collapsing them into one step is where people get stuck:
- Stage A: `for`/`while` loops that transform a collection → `map`, `filter`, `reduce`. Someone who's written `for (x : list) { if (cond) result.add(f(x)); }` a thousand times can see `(filter cond (map f list))` as literally the same operation, just named and composed instead of spelled out imperatively.
- Stage B: loops with evolving state (running totals, counters, early exit) → `reduce` with an accumulator, and only later, `loop`/`recur`. Reduce as "the accumulator *is* the loop variable, made explicit as a function argument instead of implicit mutable state" is the key reframe.

**3. Objects/classes → data + functions, kept separate**
Their instinct: bundle data and behavior together, methods live on the object. The mapping: Clojure keeps "what shape is this data" (maps, records) and "what can I do with data of this shape" (plain functions that take the data as an argument) as two separate things. This isn't a limitation — it's why `map`, `filter`, `assoc`, etc. work on *any* map or vector, rather than needing a method defined per class. Point out that this is close to how they'd write utility functions operating on structs in C, or free functions in early C++, if they've touched either.

**4. Inheritance/polymorphism → protocols and multimethods, introduced later**
Don't front-load this. Most beginner projects don't need it. When it comes up, map interfaces (Java/C#) → protocols pretty directly, since the mechanics are genuinely similar (define a set of function signatures, multiple types implement them).

**5. Mutable state (fields, setters) → atoms, refs, and "where state actually lives"**
This is the second big friction point, and it's conceptual, not syntactic. Reframe: instead of state being smeared across many objects that each mutate their own fields, state gets concentrated into a small number of explicit references (usually `atom`s), and *almost everything else in the program is a pure function that doesn't touch them.* The mental shift is from "mutation is everywhere, implicitly" to "mutation is rare, explicit, and grep-able." This is a good moment to revisit the answer from earlier in this conversation about MIDI editing and immutability — it's a concrete case study of exactly this: one atom holding the whole editor state, everything else pure.

**6. Exceptions/null checks → nil-punning and threading**
Map defensive `if (x != null)` chains to Clojure's tendency to let `nil` flow safely through `map`/`filter`/`some->` chains, and later, more structured error handling if the project needs it. Don't introduce this until they've hit the pain of a `NullPointerException`-equivalent organically.

## A learning plan with projects layered on top

**Phase 1 — Syntax and REPL fluency (few days).** No "real" project yet. Goal: get comfortable reading nested parens, using the REPL as a scratchpad, and the very basic vocabulary (`def`, `defn`, `let`, `if`, vectors/maps/keywords). A good forcing function here is small, self-contained katas — FizzBuzz, word frequency counter, run-length encoding — each solved first the way they'd solve it in their old language *conceptually*, then translated. The point isn't the exercises themselves, it's narrating the translation out loud (or in comments) each time.

**Phase 2 — Data transformation pipelines (1–2 weeks).** Project: take a CSV or JSON dataset (something real and a bit messy — game stats, weather data, their own bank export) and do a sequence of `map`/`filter`/`reduce`/`group-by`/`sort-by` transformations to answer questions about it. This is the single best project type for this stage because it's where `for`-loop-shaped thinking gets replaced by pipeline-shaped thinking almost automatically, and the result (a table, a summary) gives immediate, checkable feedback. Encourage them to write it once as one big function, then refactor into a `->>` threaded pipeline of small named steps — that refactor *is* the lesson.

**Phase 3 — State and identity (1–2 weeks).** Project: something with a natural single mutable "world" — a simple text-based game (a grid-based adventure, tic-tac-toe with a CLI), or a small simulation (Conway's Game of Life). Constraint to impose deliberately: the "world" lives in exactly one atom, and every function that computes the next state is pure (`world -> world`), with the atom only touched at the very top level (the game loop) and never inside logic functions. This is where "separate data from behavior" and "concentrate mutation" both get exercised under real pressure, because a game loop is exactly the kind of thing their old instincts would smear mutable state through many objects.

**Phase 4 — A small stateful tool with structure (2–4 weeks).** This is where a project like your MIDI step-editor or NLS chord decoder fits perfectly as a *learning* project, even a toy version — small parser/interpreter, a simple markdown-to-HTML converter, a basic to-do CLI with persistence, or (given your other conversation) literally a toy chord-keyset decoder over a fake event stream. Good because it forces: modeling a domain as immutable data (not classes), writing a state-transition function as a pure `state, event -> state`, and wiring that to some kind of I/O boundary (files, stdin, a channel) where the impurity is pushed to the edges. This is the project type that cements "immutability isn't a restriction, it's what makes the core logic testable and the I/O boundary obvious" — which is the single biggest payoff idea for someone coming from OO/imperative backgrounds.

**Phase 5 (optional) — Concurrency.** Project: something with genuinely concurrent work — a simple web scraper hitting several URLs in parallel, or a chat-room-style toy server. This is where atoms/refs/core.async actually justify themselves, versus feeling like ceremony. Don't do this before Phase 3–4 land, or "why not just use a mutex/lock" objections dominate the discussion unproductively.

## A few teaching heuristics worth stating explicitly to them, early

- **"Everything is a value, including behavior."** Functions are values you pass around like any other data — this one sentence, revisited concretely (`map` taking a function argument, `assoc` on a function-valued map entry) resolves a lot of confusion faster than abstract explanation.
- **Read right-to-left / inside-out at first, then learn `->`/`->>`.** Nested-paren reading is a real skill gap; threading macros are the "fix" but shouldn't be introduced until they've suffered through nested calls enough to appreciate why threading exists.
- **The REPL replaces the debugger, mostly.** People from IDE-heavy backgrounds (Java, C#) often don't reach for the REPL to inspect intermediate values because their instinct is breakpoints. Explicitly show them "evaluate this one `let` binding in isolation" as equivalent to inspecting a variable at a breakpoint.
- **"Where does mutation happen" as a code-review question.** Once they're past Phase 3, get them in the habit of asking that question about their own code — it's the functional-design equivalent of "where are the class boundaries" for OO thinking, and it's the single habit that most predicts whether someone writes idiomatic Clojure a year in.

If it'd help, I can sketch what a Phase 4-style toy project (e.g., a minimal version of the chord-keyset decoder, scoped for a learning exercise rather than production) would look like as a week-by-week build-up, with each step introducing one new concept.

## Prompt:
Please sketch a Phase 4 project using the chord-keyset decoder example that also illustrates how Java interop can be used to integrate a real midi data stream with the Clojure code

## Response:
Good project choice — it naturally forces the "impurity pushed to the edges" lesson because MIDI hardware access *is* impure, stateful, callback-driven Java, sitting right next to the pure FSM core from earlier. Here's a week-by-week build-up.

## Overall shape of what they're building

Three layers, deliberately kept separate so the boundary between "Java interop / impure" and "Clojure / pure" is visible and enforced rather than blurred:

1. **MIDI I/O layer** — thin, Java-interop-heavy, talks to `javax.sound.midi`, does nothing but turn hardware events into plain Clojure maps on a channel, and turn outgoing chord codes into println/log output (or later, real NLS commands).
2. **Interpretation core** — the pure FSM from our earlier conversation: `state, event -> new-state, maybe-emission`. Zero Java interop. Fully unit-testable with plain vectors of event maps.
3. **Wiring/glue** — a `core.async` go-loop or similar that pulls from the MIDI layer's channel, folds through the core FSM, and pushes emissions onward.

The pedagogical point of this shape: by the end, they should be able to point at layer 2 and say "this has never touched a MIDI device and never will," and understand *why* that's valuable — it's directly testable, and it's the same lesson as the editor's pure `state -> state` handlers from Phase 3.

## Week 1 — Pure core, no MIDI at all

Build the FSM exactly as discussed: pitch→bit table, `held`/`accum` sets, a step function, tested against literal event-map sequences (`{:type :note-on :note 64}` etc.) fed in by hand as a vector — no hardware, no channels, no interop. Goal: get the domain logic completely right and completely tested before touching anything stateful or external. This also reinforces Phase 3's lesson (pure `state -> state`) in a slightly richer domain, and sets up a clean regression suite they can run against every later change to layer 1/3 without ever needing a keyboard plugged in.

## Week 2 — Discovering and opening a MIDI device via interop

This is the first real interop week, and it's a good one for teaching Java interop idioms specifically because `javax.sound.midi` is very Java-8-style OO: static methods, instances, listener interfaces.

Concepts to introduce concretely:
- **Static method calls**: `(javax.sound.midi.MidiSystem/getMidiDeviceInfo)` — teach the `Class/staticMethod` interop syntax here, and that it returns a Java array, which they'll need to walk with ordinary Clojure seq functions (`seq`, `map`) — a nice small example of "Java values become sequable the moment you touch them with core functions."
- **Instance method calls**: `(.getName device-info)`, `(.open device)` — the `(.method obj args)` syntax, and the mental model that this is just calling a method on a Java object sitting in a Clojure var.
- **Printing device info and picking the right one** — a very concrete, satisfying "it's alive" moment: list all MIDI devices on the machine, print their names, identify which one is the keyboard.

Deliverable for the week: a small script that opens the chosen MIDI input device and confirms it's open — no event handling yet.

## Week 3 — The listener interface boundary: reify

This is the conceptually richest interop week, because it's where they hit the pattern "Java wants an object implementing an interface, Clojure gives you `reify`."

`javax.sound.midi.Receiver` (or a `Transmitter`'s listener) is exactly this shape: a Java interface with a callback method (`send`) that fires per incoming MIDI message. Teach:
- **`reify`** as "implement a Java interface inline, right here, as a Clojure form" — the callback body is just Clojure code, and can close over Clojure vars (like the output channel from layer 1) the normal way.
- Inside that callback: pulling the actual note number, velocity, and on/off status out of the raw `ShortMessage` object via more instance-method interop (`(.getCommand msg)`, `(.getData1 msg)`), and translating Java's raw command constants (`ShortMessage/NOTE_ON`, `NOTE_OFF`) into the tidy Clojure event maps layer 2 expects.
- **The crucial discipline point**: the `reify` callback should do the absolute minimum — translate the raw event into a plain map and put it on a channel — and nothing else. No FSM logic here. This is the moment to explicitly name what's happening: "we're at the impure boundary, and the job of this boundary is translation only." It's a very concrete instance of the "push impurity to the edges" idea from Phase 3/4 framing.

Deliverable: pressing keys on the real keyboard produces printed event maps in the REPL, live.

## Week 4 — Wiring layers together with core.async

Now connect layer 1's channel to layer 2's pure FSM. Concepts:
- `core.async` basics: `chan`, `>!!`/`put!` from the callback (since it's running on a MIDI thread, not a go-block, so blocking or callback-style put is the right call — a good small lesson in "not everything is a go-block; sometimes you're being called from foreign-thread code and need the non-parking put").
- A `go-loop` (or plain `loop` in a future/thread) that takes events off the channel one at a time and threads them through the Week 1 FSM, using an atom (or the loop's own recur binding) to hold FSM state between events — same "concentrate the mutable state in one place" pattern as the Phase 3 game loop.
- On emission, just print the resulting chord code for now.

Deliverable: play real chords on the keyboard, see decoded 5-bit codes print live, right next to the still-running Week 1 test suite proving the core logic independently.

## Week 5 (stretch) — Output side and a second interop pattern

If they want to go further: turn the emitted chord codes into simulated "typed characters" or NLS-style commands, and optionally demonstrate *outgoing* Java interop by sending something back — e.g., constructing a `ShortMessage` object (`(doto (ShortMessage.) (.setMessage ...))`) and sending it to a `Receiver` to light up a MIDI-controllable LED or play a confirmation beep. This closes the loop and shows that interop is symmetric: reading Java objects apart and building them up are the same handful of techniques (constructors via `Classname.`, `doto` for the common "call several setters" pattern, instance methods) applied in reverse.

## Why this ordering works pedagogically

The sequence deliberately keeps interop *entirely out* of Week 1, so the pure core exists, is trusted, and is regression-tested before any hardware complexity enters. Then it introduces interop in increasing structural complexity — static calls, then instance calls, then implementing an interface, then thread/channel coordination — each week adding exactly one new interop idiom rather than dumping `javax.sound.midi`'s whole surface at once. And the final architecture makes the "why immutability/purity matters" lesson from Phase 3 land a second time, but now against a backdrop of *real* impurity (hardware callbacks, threads) instead of an atom standing in for imagined statefulness — which is usually the point where it stops being an abstract principle and becomes an obviously good engineering call.

## Prompt:
Add an extra-credit project that combines the MIDI chord keyset with mouse and keyboard data to simulate the Line Processor technology that was used to connect NLS workstations to mainframe systems by streaming the chord, mouse, and keyboard events over the wire.

## Response:
Good capstone — this pulls together everything from the earlier weeks (interop, channels, pure/impure separation) and adds a genuinely new concept: multiplexing several independent event sources into one serial protocol, which is the actual systems-design problem the real Line Processor solved.

One caveat up front, given the correction earlier in this conversation: I'm working from general/well-known knowledge that the Line Processor was real NLS hardware multiplexing the keyset, mouse, and keyboard onto a line to the SDS 940 mainframe — but I haven't verified the specific protocol details (framing, byte layout, baud rate, etc.), and I'd invent something wrong if I presented specifics as historical fact. So the sketch below treats the *protocol* as something the student designs themselves in the spirit of what a Line Processor did, rather than a claimed reconstruction of the real one. If historical accuracy of the wire format matters to them, that's worth a research pass (there's likely detail in the NLS/Augmentation Research Center technical reports) before or alongside building this.

## Extra Credit — The Line Processor Simulator

### Concept

The real problem the Line Processor solved: three physically independent input devices (keyset, mouse, keyboard) each producing events at unpredictable times, needing to be combined into a single serial stream sent to a remote mainframe that reconstructs "what the user is doing" from that stream, in order. That's a genuinely different problem from anything in Weeks 1–5 — those were one device, one channel. This project is about **merging multiple independently-timed sources into one framed protocol**, which is a real, transferable systems concept (it's the same shape as multiplexing multiple sensors onto one UART, or multiple clients onto one WebSocket).

### Architecture — four layers instead of three

1. **Three independent input layers**, each following the Week 2–3 pattern already built:
   - Chord keyset (already built — MIDI via `reify`d `Receiver`)
   - Mouse (new interop: AWT mouse listeners)
   - Keyboard (new interop: AWT/Swing key listeners, or a headless global-hook library if they want it to work outside a focused window)

   Each layer's only job, per the discipline established in Week 3, is: translate a raw platform event into a small plain Clojure map and put it on its own channel. No merging, no protocol logic here.

2. **A multiplexer** — the new concept this project adds. Something that takes three input channels and interleaves them into one output stream *in the order events actually occurred*, tagging each with its source. `core.async`'s `alts!` (or `merge`) is the natural tool: loop, `alts!` across the three channels, whichever fires first gets tagged and forwarded. This is worth pausing on explicitly as a concept, since it's new: "select whichever of several channels has something ready" is a different pattern from anything used so far, and it's the direct Clojure analog of what a hardware line processor's own multiplexing logic had to do — accept whichever device raised an interrupt first.

3. **A framing/encoding layer** — turn each tagged event map into a compact byte representation for "the wire": e.g., a leading tag byte (0 = keyset, 1 = mouse, 2 = keyboard), then a small fixed number of payload bytes. This is a good, contained place to introduce **byte-level thinking** in Clojure — `byte-array`, bit-shifting into/out of bytes — which most people coming from higher-level languages haven't done regardless of prior background, so it's a fair concept for everyone rather than an OO-vs-FP translation point.

4. **A "wire" and a receiver ("mainframe") side** — simulate the serial line honestly rather than skipping it: a Java `Socket`/`ServerSocket` pair on localhost (or even just a `java.io.PipedInputStream`/`PipedOutputStream` pair for simplicity), with the encoder writing bytes to one end and a separate process (or just a separate thread/namespace, playing the role of "the SDS 940 side") reading, decoding, and printing a reconstructed log: `"14:02:03.221 KEYSET chord=0b01011  14:02:03.400 MOUSE dx=12 dy=-4  14:02:03.512 KEY down 'a'"`. Making this a real socket rather than an in-process shortcut is worth doing deliberately — it makes tangible that this is actually a wire protocol between two separate systems, which is the whole point of the exercise, and it's a nice easy way to reuse the `javax.sound.midi`-style interop skills on a different Java API (`java.net`).

### Suggested build order

- **Step 1**: Get mouse and keyboard events flowing to channels independently, same shallow-translation-only discipline as the MIDI listener. This is mostly "another `reify`/listener pattern," reinforcing rather than introducing new interop concepts.
- **Step 2**: Multiplexer with `alts!`/`merge`, tested first with three *fake* channels fed synthetic events by hand (again: prove the merge logic correct without live hardware before wiring real input, same testing discipline as Week 1).
- **Step 3**: Design the frame format on paper first — how many bytes, what fields, how the receiver knows where one frame ends and the next begins (fixed-width frames are much easier than delimited ones for a first pass, worth calling out explicitly as a deliberate simplification versus what a real protocol would need for variable-length data).
- **Step 4**: Encoder + socket write.
- **Step 5**: Separate receiver namespace/process: socket read, decode, reconstruct and print the merged event log.

### Why this is a good capstone specifically

It forces the student to compose *everything* from the earlier weeks — pure/impure separation (encoding/decoding logic should be pure functions, byte-array in, map out and vice versa, testable without any socket or device involved), interop (three different flavors: MIDI, AWT, and now `java.net`), and channels (now genuinely needed, not just a convenient wrapper, since `alts!`-style merging is the one thing a plain callback or loop can't do cleanly). And it recreates, in miniature, a real and somewhat under-appreciated piece of engineering history — the actual problem of getting a mouse, a keyboard, and a one-handed chord device to cohabit one link to a shared mainframe was a genuine hardware/protocol design problem in 1968, and building even a toy version of it is a good way to make that concrete rather than abstract.

## Prompt:
How can the mouse buttons be detected in Java?  And how can clicking the middle mouse wheel be detected as a button?

## Response:
Fair to double check given the discussion earlier — let me confirm the API specifics rather than go from memory alone.

Confirmed against the docs — here's how it works, including the middle-button wheel-click question, which is a bit of a historical wart in the API.

## Detecting mouse buttons in general

Standard AWT/Swing gives you this through `MouseListener` (for press/release/click) implemented via `reify`, same pattern as the earlier `Receiver` interop:

```clojure
(reify java.awt.event.MouseListener
  (mousePressed [_ e]
    (let [button (.getButton e)]
      ...))
  (mouseReleased [_ e] ...)
  (mouseClicked [_ e] ...)
  (mouseEntered [_ e] ...)
  (mouseExited [_ e] ...))
```

`.getButton` on the `MouseEvent` returns an int, and you compare it against three constants defined on `MouseEvent` itself: MouseEvent.BUTTON1, BUTTON2, and BUTTON3, with a fourth constant, NOBUTTON, indicating no mouse buttons, used for events like plain movement where no button changed state. In Clojure that's just `(= (.getButton e) java.awt.event.MouseEvent/BUTTON1)` and so on. `getButton()` specifically returns the button which has changed state for that particular event, which is the cleaner API to use — there's an older, uglier `getModifiers()`/bitmask approach that predates `getButton()`, and it's worth skipping: the docs themselves warn there's overlap in the values of ALT_MASK/BUTTON2_MASK and META_MASK/BUTTON3_MASK, which is a real historical footgun (modifier-key masks and button masks share bit values), and it's exactly why `getButton()` was added later as the reliable way to ask "which button." Swing also gives convenience predicates — `SwingUtilities/isLeftMouseButton`, `isMiddleMouseButton`, `isRightMouseButton` — which, as of the fix that landed in the JDK, are implemented as thin wrappers over exactly this `getButton() == MouseEvent.BUTTON1/2/3` check, so reaching for `.getButton` directly and comparing against the constant is equivalent and arguably clearer in Clojure than calling into `SwingUtilities`.

## The middle button / mouse wheel question specifically

Here's the important nuance, and it's a legitimate point of historical confusion in the JDK itself, not something I should gloss over: **BUTTON2 is the middle mouse button** as a physical button — i.e., if the mouse has a middle button that can be pressed as a button (many wheel mice let you click the wheel itself downward as a button), that press fires as `BUTTON2`, not as a wheel event. This is confirmed by an open JDK bug report where a developer asked for clarification and a JDK engineer's own uncertainty is on record: on my linux machine, BUTTON2 refers to the middle mouse button, but is this the case for Windows? — the platform behavior was murky enough that it warranted a bug filing, though the practical answer that's held up across JDK versions is that BUTTON2 = middle button, consistently, across platforms.

But — and this is the part worth being careful about in your Line Processor project — **the wheel scrolling itself (rolling it up/down) is a completely separate event type from a middle-button click.** Scrolling generates "the mouse wheel" event, handled by a different interface, `MouseWheelListener`, not `MouseListener`. So you actually have two physically distinct interactions on a typical wheel mouse, and Java exposes them through two different listener interfaces:

- **Pressing the wheel down as a button** → `MouseListener.mousePressed`, `.getButton()` returns `BUTTON2`.
- **Rolling the wheel** → `MouseWheelListener.mouseWheelMoved`, which gives you a `MouseWheelEvent` with `.getWheelRotation()` (or `.getPreciseWheelRotation()` for finer-grained trackpads/wheels) telling you direction and amount — this is not a "button" at all in the API's model, it's a distinct rotation value.

So for your Line Processor project: if you literally want "clicking the wheel" (pressing it down) treated as a discrete button-press event alongside left/right click, that's just `BUTTON2` off the ordinary `MouseListener` — no special handling needed, it's already unified with left/right click in the same interface. If by "middle mouse wheel click" you instead meant *scroll events*, those need a second listener (`MouseWheelListener`) added to the same component, and you'd need to decide in your protocol design whether to treat a wheel notch as a synthetic "button 4/5 press+release pair" (common convention in a lot of X11-derived software, historically) or as its own event type/tag in your framing layer — worth calling out to the student as a real design decision rather than something Java hands you an obvious answer for, since the API itself keeps these as two separate concepts.

For the multiplexer/framing layer from the Line Processor sketch, that means the mouse input layer likely wants two listeners attached to the same component (`MouseListener` and `MouseWheelListener`), both funneling into the same output channel as tagged maps — e.g. `{:type :mouse-button :button 2 :action :press}` vs. `{:type :mouse-wheel :rotation -1}` — so the merge/multiplexer step downstream doesn't need to know or care that they came from different Java interfaces, only that they're both "mouse layer" events.

## Prompt:
The mouse button logic should operate similar to the chord keyset logic.  It should accumulate which buttons were pressed during an episode, and the release of all buttons should trigger the event, which should have 3 bits defining the mouse status.  Please write a page explaining this to the student and suggest how the Java interop code can map this logic to Clojure.

## Response:
I've published this as a page for the student. It covers:

- The same held/accum episode model, now applied to 3 mouse buttons instead of 5 MIDI notes
- A generic, bit-table-parameterized `chord-step` function — the key lesson being that the original FSM design generalizes across devices if it takes the bit mapping as an argument rather than hardcoding it
- Why `mousePressed`/`mouseReleased` are the right events (and `mouseClicked` is not) for reconstructing held/released state
- The direct mapping from `MouseEvent/BUTTON1-3` to a bit table
- A note on why drag-outside-component behavior doesn't break episode tracking, grounded in the AWT docs we pulled earlier

## Response:
> **Artifact: mouse-button-chording**

> # Mouse Button Chording: Reusing the Keyset's Episode Model
> 
> ## Recap: what an "episode" is
> 
> In the MIDI chord-keyset decoder, we defined a **chord episode** as the span of
> time between two moments when *none* of the five chosen keys are held down.
> During an episode we tracked two sets:
> 
> - **held** — which keys are down *right now* (tells us when the episode ends)
> - **accum** — the union of every key that was down *at any point* during the
>   episode (this becomes the emitted code)
> 
> The episode ends, and a code is emitted, on the transition where `held` goes
> from non-empty back to empty. At that instant we read `accum`, turn it into an
> integer by treating each present key as a bit, and reset both sets for the
> next episode.
> 
> ## Applying the same model to mouse buttons
> 
> A standard mouse gives you three buttons — left, middle, right — which is a
> perfect fit for the same idea, just with 3 bits instead of 5:
> 
> - **held** — which of the three buttons are down right now
> - **accum** — the union of every button pressed since the episode began
> - **emit** — when the last held button is released, read `accum`, encode it
>   as a 3-bit value (0–7), and reset
> 
> So "click left, then while still holding it also press right, then release
> both" produces exactly one event, coded as `left | right`, in the same way
> that rolling two keyset keys in and out of a chord before releasing them all
> produced one 5-bit code. The behavior you want — *buttons pressed together, or
> rolled in and out together, still count as one combined event* — is precisely
> the "accumulate until all-released" rule we already built. Nothing about the
> episode logic itself needs to change; only the **bit table** changes, from
> `{E→0, F→1, G→2, A→3, B→4}` to something like `{LEFT→0, MIDDLE→1, RIGHT→2}`.
> 
> This is worth pausing on as a design lesson: if the step function was written
> to take a bit table as an argument rather than assuming a specific five-note
> mapping, you now have *one* pure FSM that both the keyset and the mouse can
> reuse, just instantiated with different bit tables. That's a strong sign the
> original design was factored well — the "episode" concept turned out to be
> about *input devices with discrete, holdable states in general*, not
> specifically about MIDI notes.
> 
> ```clojure
> ;; One generic, reusable step function:
> (defn chord-step
>   "state = {:held #{} :accum #{}}
>    event = {:code <bit-source-value> :action :on|:off}
>    bit-table = {code -> bit-index}
>    Returns [new-state emitted-value-or-nil]."
>   [bit-table state event]
>   (let [{:keys [code action]} event]
>     (case action
>       :on  (let [state' (-> state
>                              (update :held conj code)
>                              (update :accum conj code))]
>              [state' nil])
>       :off (let [held' (disj (:held state) code)]
>              (if (empty? held')
>                (let [value (reduce (fn [acc c]
>                                       (bit-or acc (bit-shift-left 1 (bit-table c))))
>                                     0 (:accum state))]
>                  [{:held #{} :accum #{}} value])
>                [(assoc state :held held') nil])))))
> ```
> 
> The keyset decoder becomes `(partial chord-step keyset-bit-table)`; the mouse
> decoder becomes `(partial chord-step mouse-bit-table)`. Same function, two
> independent instances, each with its own state atom.
> 
> ## Mapping Java's mouse events onto this model
> 
> The interop layer's whole job — as with the MIDI `Receiver` — is translation
> only: turn a raw Java event into the plain `{:code ... :action :on|:off}` map
> the generic step function expects, and put it on a channel. Nothing about
> episodes, held/accum sets, or bit encoding belongs in this layer.
> 
> `MouseListener`'s `mousePressed`/`mouseReleased` map directly onto `:on`/`:off`
> — they're the mouse equivalent of `NOTE_ON`/`NOTE_OFF`. `mouseClicked` is
> **not** the right event to use here: it only fires for a quick press-and-release
> with no drag, so it would miss exactly the "hold left, add right, release
> left first" case that makes chording interesting. You want the raw
> press/release pair, same as you wanted raw `NOTE_ON`/`NOTE_OFF` rather than
> some higher-level "note played" event from the MIDI side.
> 
> ```clojure
> (reify java.awt.event.MouseListener
>   (mousePressed [_ e]
>     (put! mouse-chan {:code (.getButton e) :action :on}))
>   (mouseReleased [_ e]
>     (put! mouse-chan {:code (.getButton e) :action :off}))
>   (mouseClicked [_ e] nil)
>   (mouseEntered [_ e] nil)
>   (mouseExited  [_ e] nil))
> ```
> 
> `.getButton e` already returns one of `MouseEvent/BUTTON1`, `BUTTON2`, or
> `BUTTON3` — so the bit table for this device is just:
> 
> ```clojure
> (def mouse-bit-table
>   {java.awt.event.MouseEvent/BUTTON1 0   ; left
>    java.awt.event.MouseEvent/BUTTON2 1   ; middle (wheel-click)
>    java.awt.event.MouseEvent/BUTTON3 2}) ; right
> ```
> 
> From here, wiring is identical in shape to the keyset: a loop pulling events
> off `mouse-chan`, threading them through `(chord-step mouse-bit-table state
> event)`, holding `state` in an atom (or as a `loop`/`recur` binding), and
> forwarding each emitted 3-bit value onward — into the same kind of output
> channel the keyset decoder produces, ready to be tagged and merged by the
> Line Processor multiplexer.
> 
> ## One practical wrinkle worth knowing about
> 
> Java's docs note that `MOUSE_DRAGGED` (and, by the same platform mechanics,
> button state generally) is delivered to the component where the button was
> *originally pressed*, for as long as that button stays held — regardless of
> whether the pointer is still over that component. That's actually good news
> for this design: it means a press/release pair stays paired to the same
> listener even if the user's hand drifts off the button during a chorded hold,
> so you don't need to guard against a stray `mouseExited` breaking the episode.
> 
> ## Why this matters for the Line Processor project
> 
> You now have two structurally identical decoders — one for the keyset, one
> for the mouse — each independently pure, each testable with a handful of
> literal event maps, and each producing a small integer code from its own
> episode. The multiplexer from the Line Processor sketch doesn't need to know
> anything about *how* those codes were derived; it just needs to merge and tag
> whichever channel produces a value first. That separation — generic episode
> FSM, device-specific bit table and translation layer, dumb multiplexer on top
> — is the same "push impurity to the edges, keep the core generic and pure"
> principle showing up a second time, now proven out across two different kinds
> of hardware.

