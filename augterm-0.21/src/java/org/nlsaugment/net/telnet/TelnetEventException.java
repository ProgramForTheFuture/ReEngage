/*
 * Copyright © 2005 by Howard Palmer.  All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.nlsaugment.net.telnet;

import java.io.IOException;

/**
 * This class extends <code>IOException</code> to provide information about
 * a Telnet protocol event that occurs during Telnet I/O.  These events
 * include changes in the set of enabled Telnet options, and Telnet commands
 * such as IP and AYT.
 *
 * @author Howard Palmer
 * @version $Id: TelnetEventException.java 135 2005-11-03 04:15:04Z Howard $
 * @see {@link org.nlsaugment.net.telnet.TelnetEvent TelnetEvent}
 * @see {@link java.io.IOException IOException}
 */
public class TelnetEventException extends IOException {
  private static final long serialVersionUID = 1;

	private final TelnetEvent event;

	/**
	 * Constructor for <code>TelnetEventException</code> for a specified
	 * <code>TelnetEvent</code> and a <code>null</code> error detail message.
	 * In most cases, this would be the constructor used, since <code>TelnetEvent</code>s
	 * are usually not errors.
	 *
	 * @param event the <code>TelnetEvent</code> to be delivered via this exception.
	 * @see {@link org.nlsaugment.net.telnet.TelnetEvent TelnetEvent}
	 */
	public TelnetEventException(TelnetEvent event) {
		super();
		this.event = event;
	}

	/**
	 * Constructor for <code>TelnetEventException</code>.
	 *
	 * @param s		the error detail string
	 * @param event	 the <code>TelnetEvent</code> to be delivered via this exception.
	 * @see {@link org.nlsaugment.net.telnet.TelnetEvent TelnetEvent}
	 */
	public TelnetEventException(String s, TelnetEvent event) {
		super(s);
		this.event = event;
	}

	/**
	 * Return the <code>TelnetEvent</code> associated with this exception.
	 *
	 * @return the <code>TelnetEvent</code>
	 * @see {@link org.nlsaugment.net.telnet.TelnetEvent TelnetEvent}
	 */
	public TelnetEvent getEvent() {
		return event;
	}

	@Override
	public String toString() {
		return "TelnetEventException:\n" + event.toString();
	}
}
