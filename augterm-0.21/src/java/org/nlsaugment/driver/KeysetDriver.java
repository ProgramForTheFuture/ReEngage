/**
 * Copyright © 2006 Jonathan M. Cheyer
 * All rights reserved.
 *
 * Licensed under GPL Version 2.
 * http://www.gnu.org/licenses/gpl.html
 */
package org.nlsaugment.driver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.nlsaugment.event.KeysetEvent;
import org.nlsaugment.event.KeysetListener;

/**
 * <p>This class is the driver to read information from the chord keyset. It allows a KeysetListener
 * to register for KeysetEvent occurrences.
 *
 * <p>The chord keyset is an analog device, quite similar to an analog joystick. However, the keyset's
 * cable has a different pinout than a regular analog joystick. Therefore, a very specific hardware
 * environment is required. The chord keyset needs to be connected to a hand-crafted adapter cable that
 * converts the specific pinout of the chord keyset to the pinout of an analog PC gameport. The gameport
 * adapter cable can then be connected to a separate adapter that converts between the gameport and USB.
 * For testing, a RadioShack gameport/USB adapter Model #26-164 was used.
 *
 * <p>Certain assumptions were made in developing this driver. In particular, this works only on Linux
 * (tested on Fedora Core 4). A specific analog game port device file is required (currently /dev/input/js0).
 * The hand-crafted adapter cable maps KEY0 through KEY3 to the wires representing the first four
 * joystick buttons. Since analog joystick cables only allow up to four buttons, the final key (KEY4)
 * is mapped to the wire repesenting the X axis of the gameport.
 *
 * <p>The combination of the hand-crafted adapter cable and the RadioShack adapter yields very specific values
 * for the PRESSED and RELEASED events of KEY4. The range is between approximately -10000 (when pressed)
 * and -16000 (when released). Of course, a different combination of hand-crafted adapter cable
 * or RadioShack adapter would give different values.
 *
 * <p>For more information, see http://chm.cim3.net/cgi-bin/wiki.pl?NlsTechnical/Keyset
 *
 * @author cheyer
 *
 */
public final class KeysetDriver {
  private static final String __deviceFile = "/dev/input/js0"; // TODO: this needs to be configurable
  private static final int __axisDividerValue = -13000;  // a reasonable midpoint between the approximate [-10000, -16000] range)
  private static final KeysetDriver __driver = new KeysetDriver();

  private final ArrayList<KeysetListener> _listeners = new ArrayList<KeysetListener>();
  private ArrayList<JoystickRecord> _history = new ArrayList<JoystickRecord>();
  private boolean _shutdown = false;

  /*
   * The method used by this driver to determine when a KEYSET_TYPED event occurs is as follows:
   * Every time a key is pressed, the key number (KEY0 to KEY4) is stored in both the _pressedState variable
   * and the _currentState variable. When a key is released, only the _currentState is updated. Once the
   * _currentState is back to zero (all keys have been released), then the KEYSET_TYPED event occurs. Any
   * key that was pressed at any time since the previous KEYSET_TYPED event is considered to be active,
   * or taking part, in the event. If a key is pressed, then released, and then pressed again (provided it is
   * not the only key being pressed at the time), then that is a single event and the second press is
   * considered the same as the first press.
   */
  private byte _pressedState = 0;
  private byte _currentState = 0;

  private KeysetDriver() {}

  public static KeysetDriver getInstance() {
    return __driver;
  }

  private void checkKeyset() throws KeysetNotFoundException {
    final File file = new File(__deviceFile);
    if (! file.exists()) {
      throw new KeysetNotFoundException();
    }
  }

  public void register(final KeysetListener listener) throws KeysetNotFoundException {
    checkKeyset();
    this._listeners.add(listener);
  }

  public void start() {
    // TODO: don't allow someone to start unless currently stopped.
    new Thread() {
      public void run() {
        try {
          listen();
        } catch (KeysetNotFoundException e) {
          throw e;
        } catch (Exception e) {
          throw new RuntimeException("exception caught: ", e);
        }
      }
    }.start();
  }

  public synchronized void stop() {
    // TODO: can only stop if currently started.
    this._shutdown = true;
  }

  private void reset() {
    this._pressedState = 0;
    this._currentState = 0;
  }

  /**
   * <p>Listen to data coming in from the chord keyset device file. Listens forever, or until stop()
   * is called.
   *
   * <p>NOTE: this method is Linux-specific, because it relies on a specific underlying analog gameport driver.
   *
   * <p>Data is read from the port 8 bytes at a time. The data is an 8 byte record defined
   * in the /linux-2.6.x/include/linux/joystick.h file. The underlying Linux analog joystick/gameport
   * driver is being used to actually read data from the device.
   * @throws IOException if device file does not exist or disappears (keyset gets unplugged from USB port)
   */
  private void listen() throws IOException, KeysetNotFoundException {
    checkKeyset();
    final File file = new File(__deviceFile);
    final FileInputStream fis = new FileInputStream(file);
    final byte[] data = new byte[8];
    JoystickRecord previous = null;
    while (true) {
      if (_shutdown) {
        break;
      }
      final int result = fis.read(data);
      if (result == -1) {
        throw new RuntimeException("EOF reached. This should never occur.");
      } else if (result != 8) {
        throw new RuntimeException("invalid number of bytes read: " + result);
      }
      final JoystickRecord jr = JoystickRecord.parseBytes(data);
      if (! jr.equals(previous)) { // only record new event if current value is different than previous value
        previous = jr;
        sendEvent(jr);
      }
    }
  }

  private void sendEvent(final JoystickRecord jr) {
    this._history.add(jr);
    if ((jr.getType() & JoystickRecord.JS_EVENT_INIT) == JoystickRecord.JS_EVENT_INIT) {
      //      System.out.println("initialization:\n" + jr);
      return;  // ignore initialization data, since they are not actual event data
    } else if (jr.getType() == JoystickRecord.JS_EVENT_BUTTON && jr.getValue() == 1) {
      keysetPressed03(jr);
    } else if (jr.getType() == JoystickRecord.JS_EVENT_BUTTON && jr.getValue() == 0) {
      keysetReleased03(jr);
    } else if (jr.getType() == JoystickRecord.JS_EVENT_AXIS && jr.getValue() > __axisDividerValue) {
      keysetPressed4(jr);
    } else if (jr.getType() == JoystickRecord.JS_EVENT_AXIS && jr.getValue() <= __axisDividerValue) {
      keysetReleased4(jr);
    } else {
      throw new RuntimeException("unexpected condition");
    }
  }

  /**
   * This method is called when KEY0, KEY1, KEY2, or KEY3 is pressed.
   * @param jr
   */
  private void keysetPressed03(final JoystickRecord jr) {
    notifyPressed(new KeysetEvent(KeysetEvent.KEYSET_PRESSED, jr.getTime(), jr.getNumber(), KeysetEvent.CHAR_UNDEFINED));
    this._pressedState |= (1 << jr.getNumber());
    this._currentState |= (1 << jr.getNumber());
  }

  /**
   * This method is called when KEY0, KEY1, KEY2, or KEY3 is released.
   * @param jr
   */
  private void keysetReleased03(final JoystickRecord jr) {
    notifyReleased(new KeysetEvent(KeysetEvent.KEYSET_RELEASED, jr.getTime(), jr.getNumber(), KeysetEvent.CHAR_UNDEFINED));
    this._currentState &= ~ (1 << jr.getNumber());
    if (this._currentState == 0) {
      if (this._pressedState == 0) {
        // TODO: I've seen a bug where after running for a while, the keyset
        // driver can sometimes send a keyReleased03() event even though
        // the pressedState is 0. While this is logically impossible (you
        // can't release a key that isn't pressed), I'm not sure why this
        // event is occurring. It might be a problem somewhere within the
        // Linux joystick.h or elsewhere. Needs to be looked into.

        // for now, let's just ignore this situation altogether.
        reset();
        return;
      }
      notifyTyped(new KeysetEvent(KeysetEvent.KEYSET_TYPED, jr.getTime(), KeysetEvent.VK_UNDEFINED, map(this._pressedState)));
      reset();
    }
  }

  /**
   * This method is called when KEY4 is pressed.
   * @param jr
   */
  private void keysetPressed4(final JoystickRecord jr) {
    notifyPressed(new KeysetEvent(KeysetEvent.KEYSET_PRESSED, jr.getTime(), KeysetEvent.KEY4, KeysetEvent.CHAR_UNDEFINED));
    this._pressedState |= 16;
    this._currentState |= 16;
  }

  /**
   * This method is called when KEY4 is released.
   * @param jr
   */
  private void keysetReleased4(final JoystickRecord jr) {
    notifyReleased(new KeysetEvent(KeysetEvent.KEYSET_RELEASED, jr.getTime(), KeysetEvent.KEY4, KeysetEvent.CHAR_UNDEFINED));
    this._currentState &= ~ 16;
    if (this._currentState == 0) {
      if (this._pressedState != 0) {  // hack! TODO: figure out why 0 sometimes occurs
        notifyTyped(new KeysetEvent(KeysetEvent.KEYSET_TYPED, jr.getTime(), KeysetEvent.VK_UNDEFINED, map(this._pressedState)));
      }
      reset();
    }
  }

  private char map(final byte value) {
    if (value < 1 || value > 31) {
      throw new RuntimeException("invalid value: " + value);
    }
    return KeysetCharacterMap.CASE0.charAt(value);
  }

  private void notifyPressed(final KeysetEvent event) {
    for (KeysetListener kl : this._listeners) {
      kl.keyPressed(event);
    }
  }

  private void notifyReleased(final KeysetEvent event) {
    for (KeysetListener kl : this._listeners) {
      kl.keyReleased(event);
    }
  }

  private void notifyTyped(final KeysetEvent event) {
    for (KeysetListener kl : this._listeners) {
      kl.keyTyped(event);
    }
  }

  /**
   * This corresponds to the js_event struct in /linux-2.6.x/include/linux/joystick.h
   */
  static class JoystickRecord {
    private final long _time;     // a long, to hold unsigned 32 bits
    private final short _value;   // a short, to hold signed 16 bits
    private final short _type;    // a short, to hold unsigned 8 bits
    private final short _number;  // a short, to hold unsigned 8 bits

    public static final int JS_EVENT_BUTTON = 0x01;
    public static final int JS_EVENT_AXIS = 0x02;
    public static final int JS_EVENT_INIT = 0x80;

    private JoystickRecord(final long time, final short value, final short type, final short number) {
      checkType(type);
      checkNumber(number, type);
      checkValue(value, type);
      this._time = time;
      this._value = value;
      this._type = type;
      this._number = number;
    }

    private void checkType(final short type) {
      final short allowed = JS_EVENT_BUTTON | JS_EVENT_AXIS | JS_EVENT_INIT;
      if ((type & allowed) != type || type == 0) {
        throw new RuntimeException("invalid value for type: " + type);
      }
    }

    private void checkNumber(final short number, final short type) {
      if ((type & JS_EVENT_INIT) == JS_EVENT_INIT) {  // ignore check during initialization
        return;
      }
      if (number < 0 || number > 3) {
        throw new RuntimeException("invalid value for number: " + number);
      }
    }

    private void checkValue(final short value, final short type) {
      if (type == JS_EVENT_BUTTON && (value < 0 || value > 1)) {
        throw new RuntimeException("value must be 0 or 1 but is: " + value);
      }
    }

    /**
     * Return the time as a long, which can hold an unsigned 32 bit value.
     */
    public long getTime() {
      return this._time;
    }

    public short getValue() {
      return this._value;
    }

    /**
     * Return the type as a short, which can hold an unsigned byte value.
     */
    public short getType() {
      return this._type;
    }

    /**
     * Return the number as a short, which can hold an unsigned byte value.
     */
    public short getNumber() {
      return this._number;
    }

    public boolean equals(final Object o) {
      if (! (o instanceof JoystickRecord)) {
        return false;
      }
      final JoystickRecord jr = (JoystickRecord) o;
      return this._time == jr._time &&
      this._value == jr._value &&
      this._type == jr._type &&
      this._number == jr._number;
    }

    public int hashCode() {
      return (int) this._time + this._value + this._type + this._number;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("time="); sb.append(this._time); sb.append(", ");
      sb.append("value="); sb.append(this._value); sb.append(", ");
      sb.append("type="); sb.append(this._type); sb.append(", ");
      sb.append("number="); sb.append(this._number); sb.append("\n");
      return sb.toString();
    }

    /**
     * return an unsigned version of the byte
     */
    private static short unsigned(byte b) {
      return (short) ((((short) b) + 256) % 256);
    }

    /**
     * WARNING: this method expects little-endian processor and is not portable!
     */
    public static JoystickRecord parseBytes(final byte[] bytes) {
      if (bytes.length != 8) {
        throw new RuntimeException("invalid byte size: " + bytes.length);
      }
      final long time = (unsigned(bytes[3]) << 24) + (unsigned(bytes[2]) << 16) + (unsigned(bytes[1]) << 8) + unsigned(bytes[0]);
      final short value = (short) ((((short) bytes[5]) << 8) + bytes[4]);
      final short type = unsigned(bytes[6]);
      final short number = unsigned(bytes[7]);
      return new JoystickRecord(time, value, type, number);
    }
  }
}
