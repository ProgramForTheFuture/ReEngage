# ReEngage
Eric Rangell is interested in NLS, DCE, (D)DKR, CoEvolution, etc. I am encouraged. Let's see if we can collaborate and build momentum.

# Augterm2024
This effort intends to relocate the code for Augterm 0.21 from Sourceforge to Github and improve it so it can be compiled in a modern Java environment.  A proof of concept has been done which shows how a gamepad can be connected to a Raspberry Pi with a USB adapter (Radio Shack #25-164) and configured as a Linux joystick device.  The Java code defines a Keyset device that will interpret the joystick data and emit KeyTyped events to the Terminal.  In theory any HID (Human Interface) device can be configured for use with the system.

# Requirements
- Raspberry Pi or Linux machine with USB ports, Ethernet Port or Wi-Fi internet connectivity - look into the RetroPie as an option.
- apt-get is used to install joystick and jstest modules
- Java SDK installed, Ant for doing builds
- USB joystick/gamepad adapter - research available options 
- Optional USB Serial Adapter for testing other input devices
- Optional USB MIDI device (ex: Korg NanoKey)

# Desired enhancements
- Fix build.xml so it compiles on the latest version of Java using Ant
- Externalize configuration of range divider for X axis for gamepad support
- Fix bug in test program where Key Typed event always displays 0
- Support for devices that emulate a 3 button mouse
- Support for using a MIDI keyboard as a Keyset with 3 mouse buttons.
- Add a configuration panel to Augterm for testing various keyset devices
- Add an option to echo characters typed on the Keyset to the terminal

# Installation Instructions
- TODO 
