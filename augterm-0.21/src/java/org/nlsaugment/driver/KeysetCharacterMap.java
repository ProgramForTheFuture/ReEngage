/**
 * Copyright © 2006 Jonathan M. Cheyer
 * All rights reserved.
 * 
 * Licensed under GPL Version 2.
 * http://www.gnu.org/licenses/gpl.html
 */
package org.nlsaugment.driver;

/**
 * The NLS/Augment system maps chord keyset keypresses to its own specific character map, 
 * similar to ASCII but not quite the same.  
 * For more info, see http://chm.cim3.net/cgi-bin/wiki.pl?NlsTechnical/KeysetMap
 */
public final class KeysetCharacterMap {
  private static final char XXX = (char) 65535;  // invalid character
  private static final char ALT = '\033';        // ESC ASCII character
  private static final char CR = '\r';
  private static final char BACKSLASH = '\\';
  private static final char TAB = '\t';
  private static final char QUOTE = '"';
  
  public static final String CASE0 = XXX + "abcdefghijklmnopqrstuvwxyz,.;? ";
  public static final String CASE1 = XXX + "ABCDEFGHIJKLMNOPQRSTUVWYXZ<>:" + BACKSLASH + TAB;
  public static final String CASE2 = XXX + "!" + QUOTE + "#$%&'()@+-*/^0123456789=[]_" + ALT + CR;    
}
