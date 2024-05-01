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
 * Various symbolic constants used by Telnet classes.
 * 
 * @author Howard Palmer
 * @version $Id: TelnetConstants.java 135 2005-11-03 04:15:04Z Howard $
 * @see org.nlsaugment.net.telnet.TelnetSocket
 *
 */
public interface TelnetConstants {
	/**
	 * Standard Telnet server port number.
	 */
	public final static int TELNET_PORT = 23;
	
	public final static int OPTION_TRANSMIT_BINARY		= 0;
	public final static int OPTION_ECHO					= 1;
	public final static int OPTION_SUPPRESS_GO_AHEAD	= 3;
	public final static int OPTION_STATUS				= 5;
	public final static int OPTION_TIMING_MARK			= 6;
	public final static int OPTION_LOGOUT				= 18;
	public final static int OPTION_SEND_LOCATION		= 23;
	public final static int OPTION_TERMINAL_TYPE		= 24;
	public final static int OPTION_END_OF_RECORD		= 25;
	public final static int OPTION_NAWS					= 31;
	public final static int OPTION_TERMINAL_SPEED		= 32;
	public final static int OPTION_TOGGLE_FLOW_CONTROL	= 33;
	public final static int OPTION_LINEMODE				= 34;
	public final static int OPTION_X_DISPLAY_LOCATION	= 35;
	public final static int OPTION_ENVIRON				= 36;
	public final static int OPTION_AUTHENTICATION		= 37;
	public final static int OPTION_NEW_ENVIRON			= 39;
	
	/**
	 * End subnegotiation.
	 */
	public final static byte SE		= (byte)240;
	/**
	 * No operation.
	 */
	public final static byte NOP	= (byte)241;
	/**
	 * Data mark.
	 */
	public final static byte DM		= (byte)242;
	/**
	 * Break.
	 */
	public final static byte BRK	= (byte)243;
	/**
	 * Interrupt process.
	 */
	public final static byte IP		= (byte)244;
	/**
	 * Abort output.
	 */
	public final static byte AO		= (byte)245;
	/**
	 * Are you there?
	 */
	public final static byte AYT	= (byte)246;
	/**
	 * Erase character.
	 */
	public final static byte EC		= (byte)247;
	/**
	 * Erase line.
	 */
	public final static byte EL		= (byte)248;
	/**
	 * Go ahead.
	 */
	public final static byte GA		= (byte)249;
	/**
	 * Begin subnegotiation.
	 */
	public final static byte SB		= (byte)250;
	/**
	 * Indicates sender's desire to begin performing, or confirmation that
	 * it is now performing, the indicated option.
	 */
	public final static byte WILL	= (byte)251;
	/**
	 * Indicates the sender's refusal to perform, or continue performing, the
	 * indicated option.
	 */
	public final static byte WONT	= (byte)252;
	/**
	 * Indicates the sender's request that the receiver perform, or confirmation
	 * that it is expecting the receiver to perform, the indicated option.
	 */
	public final static byte DO		= (byte)253;
	/**
	 * Indicates sender's demand that the receiver stop performing, or
	 * confirmation that it is no longer expecting the receiver to perform,
	 * the indicated opton.
	 */
	public final static byte DONT	= (byte)254;
 	/**
 	 * Interpret as command.
 	 */
 	public final static byte IAC		= (byte)255;

}
