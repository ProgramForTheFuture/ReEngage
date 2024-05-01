/**
 * Copyright © 2006 Jonathan M. Cheyer
 * All rights reserved.
 * 
 * Licensed under GPL Version 2.
 * http://www.gnu.org/licenses/gpl.html
 */
package org.nlsaugment.event;

public abstract class KeysetAdapter implements KeysetListener {
  public void keyPressed(KeysetEvent event) {}
  public void keyReleased(KeysetEvent event) {}
  public void keyTyped(KeysetEvent event) {}
}
