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


import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TelnetInputStream extends FilterInputStream implements TelnetConstants {
	
	private enum RcvState {NORMAL, SEENIAC, SUBNEG, SEENWILL, SEENWONT, SEENDO, SEENDONT};
	
	private final TelnetSocket socket;
	private final BufferedInputStream in;
	
	// Data that has not yet been read
	private byte[] unread = null;
	private int unreadPos = 0;
	private int unreadLen = 0;
	private boolean unreadEof = false;
	private TelnetEventException unreadEvent = null;
	
	// Saved receive protocol state from last call to mark()
	private RcvState markRcvState = RcvState.NORMAL;
	private byte[] markUnread = null;
	private int markUnreadPos = 0;
	private int markUnreadLen = 0;
	private boolean markUnreadEof = false;
	private TelnetEventException markUnreadEvent = null;
	
	private boolean inbandCommands = false;
	
	// Current receive protocol state
	private RcvState rcvState = RcvState.NORMAL;

	
	public TelnetInputStream(TelnetSocket socket, BufferedInputStream in) throws IOException {
		super(in);
		this.socket = socket;
		this.in = in;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		super.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		// Save the current receive protocol state, so it can be restored
		// when reset() is called.
		markRcvState = rcvState;
		markUnread = unread;
		markUnreadPos = unreadPos;
		markUnreadLen = unreadLen;
		markUnreadEof = unreadEof;
		markUnreadEvent = unreadEvent;
		super.mark(readlimit);
	}

	/**
	 * Read the next byte from the Telnet input stream and return it as an integer
	 * value in the range 0..255.  If the end of stream is reached, -1 is returned.
	 * This routine will block until an input byte is available.  Furthermore, if
	 * the receive Telnet protocol state machine is parsing some kind of Telnet
	 * command, it may block to receive one or more additional bytes that may be
	 * needed to complete the Telnet command.
	 * 
	 * @return	the next data byte [0..255], or -1 on end of stream
	 * @throws	TelnetEventException if a Telnet command is found in the input
	 * stream (but see also {@link #setInbandCommands(boolean)}).
	 * @throws	IOException if an I/O error occurs
	 * @see {@link InputStream#read()}
	 */
	@Override
	public int read() throws TelnetEventException, IOException {
		
		// Was there an event waiting to be delivered?
		if (unreadEvent != null) {
			TelnetEventException tee = unreadEvent;
			unreadEvent = null;
			throw tee;
		}
		
		if (unreadEof) {
			unreadEof = false;
			return -1;
		}
		
		int inval;
		while (true) {
			// If data is buffered locally, get the next byte from there
			if ((unread != null) && (unreadPos < unreadLen)) {
				inval = unread[unreadPos++];
			} else {
				// Otherwise read the stream
				inval = super.read();
			}
			
			// Handle the common case
			if ((rcvState == RcvState.NORMAL) && ((byte)inval != IAC))
				break;
			
			// Just return EOF if we hit it
			if (inval == -1)
				break;
			
			// Otherwise run the byte through the protocol state machine
			inval = process((byte)inval);
			if (inval != -2)
				break;
			
			// Loop if the state machine returns -2
		}
		return inval;
	}

	/**
	 * Reads up to len bytes of data from the input stream into an array of bytes.
	 * An attempt is made to read as many as len bytes, but a smaller number may
	 * be read. The number of bytes actually read is returned as an integer.  The
	 * number of bytes read may be returned as zero if all the bytes read were
	 * part of a Telnet command.
	 * 
	 * @param b		the buffer to receive the input bytes
	 * @param off	the offset in b at which to start storing input bytes
	 * @param len	the maximum number of bytes to store in b
	 * @throws	TelnetEventException if a Telnet command is found in the input
	 * stream (but see also {@link #setInbandCommands(boolean)}).
	 * @throws	IOException if an I/O error occurs
	 * @see {@link java.io.InputStream#read(byte[], int, int)}
	 */
	@Override
	public int read(byte[] b, int off, int len) throws TelnetEventException, IOException {
		
		// Was there an event waiting to be delivered?
		if (unreadEvent != null) {
			TelnetEventException tee = unreadEvent;
			unreadEvent = null;
			throw tee;
		}
		
		// How about an unread Eof?
		if (unreadEof) {
			unreadEof = false;
			return -1;
		}
		
		int count = 0;
		
		// While there is unread locally buffered data and room in the user buffer
		while (count < len) {
			
			// Any bytes alread buffered?
			if (unreadPos >= unreadLen) {
				// No, get some more
				if ((unread == null) || (unread.length < len)) {
					unread = new byte[len];
				}
				unreadPos = 0;
				unreadLen = 0;
				
				// Read more from the input stream
				int c = super.read(unread, 0, unread.length);
				
				// Check for end of stream
				if (c == -1) {
					if (count > 0) {
						unreadEof = true;
					} else {
						count = -1;
					}
					break;
				}
				
				unreadLen = c;
			} else {
			
				// Get next unread byte
				byte inb = unread[unreadPos++];
				
				// Check common case
				if ((rcvState == RcvState.NORMAL) && (inb != IAC)) {
					// Copy byte to user buffer
					b[off++] = inb;
					++count;
				} else {
					try {
						// Run the byte through the protocol state machine
						int inval = process(inb);
						if (inval != -2) {
							b[off++] = (byte)inval;
							++count;
						}
					} catch (TelnetEventException tee) {
						// State machine generated a Telnet event
						if (count == 0) {
							// Pass it to the user now if no bytes are in the user buffer
							throw tee;
						}
						// Otherwise save it for the next read
						unreadEvent = tee;
						break;
					}
				}
			}
		}
		
		return count;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public synchronized void reset() throws IOException {
		rcvState = markRcvState;
		unread = markUnread;
		unreadPos = markUnreadPos;
		unreadLen = markUnreadLen;
		unreadEof = markUnreadEof;
		unreadEvent = markUnreadEvent;
		super.reset();
	}

	@Override
	public long skip(long arg0) throws IOException {
		// TODO Auto-generated method stub
		return super.skip(arg0);
	}

	/**
	 * Enable or disable inband Telnet commands.  Telnet commands are normally
	 * delivered via a <code>TelnetCommandEvent</code> in a <code>TelnetEventException</code>.
	 * Enabling inband Telnet commands causes a received Telnet command to be returned inband
	 * with received data, as the command byte value defined in <code>TelnetConstants</code>.
	 * Note that the IAC code is stripped, so inband delivery probably will not be
	 * appropriate when the Telnet TRANSMIT-BINARY option is enabled on the remote side.
	 * Note that a quoted IAC (IAC IAC) is always delivered inband as a single IAC code,
	 * and this is correct, even with TRANSMIT-BINARY.
	 * <p>
	 * Inband delivery is disabled by default.
	 * 
	 * @param enable <true> to enable inband delivery.
	 */
	public void setInbandCommands(boolean enable) {
		inbandCommands = enable;
	}
	
	// Return -2 if b is part of a Telnet command
	private int process(byte b) throws TelnetEventException, IOException {
		int result = b & 0xff;
		switch (rcvState) {
		case NORMAL:
			if ((byte)b == IAC) {
				rcvState = RcvState.SEENIAC;
				result = -2;
			}
			break;
		case SEENIAC:
			switch ((byte)b) {
			case SE:
			case NOP:
			case DM:
			case BRK:
			case IP:
			case AO:
			case AYT:
			case EC:
			case EL:
			case GA:
				rcvState = RcvState.NORMAL;
				if (!inbandCommands || (b == SE)) {
					// Deliver command via exception
					TelnetCommandEvent tce = new TelnetCommandEvent(b);
					tce.throwEvent();
				}
				// Else return command inband
				break;
			case SB:
				rcvState = RcvState.SUBNEG;
				result = -2;
				break;
			case WILL:
				rcvState = RcvState.SEENWILL;
				result = -2;
				break;
			case WONT:
				rcvState = RcvState.SEENWONT;
				result = -2;
				break;
			case DO:
				rcvState = RcvState.SEENDO;
				result = -2;
				break;
			case DONT:
				rcvState = RcvState.SEENDONT;
				result = -2;
				break;
			case IAC:
				// Quoted IAC.
				rcvState = RcvState.NORMAL;
				break;
			default:
				break;
			}
			break;
		case SUBNEG:
			rcvState = RcvState.NORMAL;
			TelnetOption opt = socket.getOption(result);
			TelnetOptionEvent toe = new TelnetOptionEvent(opt, true, true);
			toe.throwEvent();
			break;
		case SEENWILL:
		case SEENWONT:
		case SEENDO:
		case SEENDONT:
			byte[] outmsg = null;
			int action = TelnetOption.IGNORE;
			opt = socket.getOption(result);
			if (opt == null) {
				if (rcvState == RcvState.SEENWILL) {
					action = TelnetOption.SEND_DONT;
				} else if (rcvState == RcvState.SEENDO) {
					action = TelnetOption.SEND_WONT;
				}
			} else {
				switch (rcvState) {
				case SEENWILL:
					action = opt.receivedWill();
					break;
				case SEENWONT:
					action = opt.receivedWont();
					break;
				case SEENDO:
					action = opt.receivedDo();
					break;
				case SEENDONT:
					action = opt.receivedDont();
					break;
				}
			}
			switch (action) {
			case TelnetOption.IGNORE:
				opt = null;
				break;
			case TelnetOption.SEND_WILL:
				byte[] willmsg = { IAC, WILL, b };
				outmsg = willmsg;
				break;
			case TelnetOption.SEND_WONT:
				byte[] wontmsg = { IAC, WONT, b };
				outmsg = wontmsg;
				break;
			case TelnetOption.SEND_DO:
				byte[] domsg = { IAC, DO, b };
				outmsg = domsg;
				break;
			case TelnetOption.SEND_DONT:
				byte[] dontmsg = { IAC, DONT, b };
				outmsg = dontmsg;
				break;
			}
			if (outmsg != null) {
				socket.getOutputStream().write(outmsg);
				socket.getOutputStream().flush();
			}
			rcvState = RcvState.NORMAL;
			if (opt != null) {
				toe = new TelnetOptionEvent(opt, false, false);
				toe.throwEvent();					
			}
			result = -2;
			break;
		}
		return result;
	}
}
