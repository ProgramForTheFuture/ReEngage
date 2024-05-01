/**
 * Copyright © 2006 Jonathan M. Cheyer
 * All rights reserved.
 *
 * Licensed under GPL Version 2.
 * http://www.gnu.org/licenses/gpl.html
 */
package org.nlsaugment.event;

import java.util.EventListener;

public interface KeysetListener extends EventListener {

  /**
   * Low-level API - method fires when key is pressed
   */
  public void keyPressed(KeysetEvent event);


  /**
   * Low-level API - method fires when key is released
   */
  public void keyReleased(KeysetEvent event);

  /**
   * High-level API - method fires when all the chord keys which
   * have been pressed, have also been released. This generates
   * (through a chord keyset map) a specific character.
   */
  public void keyTyped(KeysetEvent event);
}
