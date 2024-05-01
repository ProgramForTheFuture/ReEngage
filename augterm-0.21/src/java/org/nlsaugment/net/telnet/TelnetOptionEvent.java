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
 * This immutable class represents a change in the status of a Telnet option.
 * 
 * @author Howard Palmer
 * @version $Id: TelnetOptionEvent.java 135 2005-11-03 04:15:04Z Howard $
 * @see {@link org.nlsaugment.net.telnet.TelnetEvent TelnetEvent}
 */
public class TelnetOptionEvent extends TelnetEvent {

	private final TelnetOption option;
	private final boolean local;
	private final boolean subneg;
	
	public TelnetOptionEvent(TelnetOption option, boolean local, boolean subneg) {
		super();
		this.option = option;
		this.local = local;
		this.subneg = subneg;
	}

	public TelnetOption getOption() {
		return option;
	}
	
	public boolean isLocalSide() {
		return local;
	}
	
	public boolean isRemoteSide() {
		return !local;
	}
	
	public boolean isSubnegotiation() {
		return subneg;
	}
	
	@Override
	public String toString() {
		return "TelnetOptionEvent(" + option + "[" + option.getLocalState()
				+ ", " + option.getRemoteState() + "], " + local + ", "
				+ subneg + ")";
	}
}
