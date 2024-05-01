/**
 * Copyright © 2006 Jonathan M. Cheyer
 * All rights reserved.
 *
 * Licensed under GPL Version 2.
 * http://www.gnu.org/licenses/gpl.html
 */
package org.nlsaugment.augterm;

import org.nlsaugment.driver.KeysetDriver;
import org.nlsaugment.driver.KeysetNotFoundException;
import org.nlsaugment.event.KeysetAdapter;
import org.nlsaugment.event.KeysetEvent;

public class TestKeyset {
  private KeysetDriver _kd;

  public void start() {
    _kd = KeysetDriver.getInstance();
    try {
      _kd.register(new SampleKeysetListener(this));
      _kd.start();
    } catch (KeysetNotFoundException e) {
      System.out.println("keyset is not found.");
    }
  }

  public void stop() {
    _kd.stop();
  }

  public static final void main(String[] args) throws Exception {
    TestKeyset test = new TestKeyset();
    test.start();
    System.out.println("Type the 'z' character on the chord keyset to quit.");
  }
}

class SampleKeysetListener extends KeysetAdapter {
  private TestKeyset _test;
  public SampleKeysetListener(final TestKeyset test) {
    this._test = test;
  }

  public void keyPressed(KeysetEvent event) {
    System.out.println("key " + event.getKeyCode() + " was pressed");
  }

  public void keyReleased(KeysetEvent event) {
    System.out.println("key " + event.getKeyCode() + " was released");
  }

  public void keyTyped(KeysetEvent event) {
    System.out.println("key " + event.getKeyCode() + " was typed\n");
    if (event.getKeyChar() == 'z') {
      _test.stop();
    }
  }
}
