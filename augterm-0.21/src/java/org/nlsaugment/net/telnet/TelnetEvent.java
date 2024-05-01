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

/**
 * This class represents a Telnet protocol event, such as a Telnet command or
 * a change in the status of a Telnet option.  It is normally delivered to a
 * Telnet program via a <code>TelnetEventException</code>, which is a type of
 * <code>IOException</code> than can occur during Telnet I/O.  This is an
 * abstract class that must be extended to provide information about a
 * particular type of Telnet event.
 * 
 * @author Howard Palmer
 * @version $Id: TelnetEvent.java 135 2005-11-03 04:15:04Z Howard $
 * @see {@link org.nlsaugment.net.telnet.TelnetEventException TelnetEventException}
 */
public abstract class TelnetEvent {

	public TelnetEvent() {
		super();
	}

	public void throwEvent() throws TelnetEventException {
		throw new TelnetEventException(this);
	}
}
