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
 * This immutable class represents a Telnet command.  Commands are identified
 * by constants in the <code>TelnetConstants</code> interface.  They include
 * SE, NOP, DM, BRK, IP, AO, AYT, EC, EL, and GA.  WILL, WONT, DO, and DONT, and
 * SB do <i>not</i> occur in a <code>TelnetCommandEvent</code>,
 * but are instead presented in a <code>TelnetOptionEvent</code>.  Note that
 * <code>TelnetInputStream</code> provides an option to insert the Telnet command
 * byte into the received data stream rather than delivering it as a
 * <code>TelnetCommandEvent</code>.  See 
 * {@link org.nlsaugment.net.telnet.TelnetInputStream#setInbandCommands setInbandCommands}.
 * 
 * @author Howard Palmer
 * @version $Id: TelnetCommandEvent.java 135 2005-11-03 04:15:04Z Howard $
 * @see {@link org.nlsaugment.net.telnet.TelnetConstants TelnetConstants}
 * @see {@link org.nlsaugment.net.telnet.TelnetOptionEvent TelnetOptionEvent}
 */
public class TelnetCommandEvent extends TelnetEvent implements TelnetConstants {

	private final byte command;
	
	public TelnetCommandEvent(byte command) {
		super();
		this.command = command;
	}

	public byte getCommand() {
		return command;
	}
	
	@Override
	public String toString() {
		String result;
		switch (command) {
		case SE:
			result = "SE";
			break;
		case NOP:
			result = "NOP";
			break;
		case DM:
			result = "DM";
			break;
		case BRK:
			result = "BRK";
			break;
		case IP:
			result = "IP";
			break;
		case AO:
			result = "AO";
			break;
		case AYT:
			result = "AYT";
			break;
		case EC:
			result = "EC";
			break;
		case EL:
			result = "EL";
			break;
		case GA:
			result = "GA";
			break;
		default:
			result = Integer.toOctalString(command & 0xff);
			break;
		}
		return "<" + result + ">";
	}
}
