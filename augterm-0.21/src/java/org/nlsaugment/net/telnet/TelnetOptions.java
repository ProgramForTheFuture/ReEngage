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
import java.net.SocketTimeoutException;

/**
 * This interface provides for manipulation of Telnet options.  It should be
 * implemented by classes which provide Telnet connections.
 * 
 * @author Howard Palmer
 * @version $Id: TelnetOptions.java 135 2005-11-03 04:15:04Z Howard $
 * @see org.nlsaugment.net.telnet.TelnetSocket
 * @see org.nlsaugment.net.telnet.TelnetServerSocket
 * @see org.nlsaugment.net.telnet.TelnetChannel
 */
public interface TelnetOptions {

	/**
	 * Add an option to the set of Telnet options that are recognized on
	 * this connection.  The argument <code>option</code> should be created
	 * specifically for this connection.
	 *
	 * @param option reference to a <code>TelnetOption</code> to be added.
	 * @throws IllegalStateException
	 */
	public void addOption(TelnetOption option);
	
	/**
	 * Return a reference to a <code>TelnetOption</code> instance representing
	 * the named Telnet option for the current connection.  This may be useful
	 * particularly for options which include subnegotiation of additional
	 * information, such as the Terminal Type option.
	 * 
	 * @param name the name of the Telnet option, as specified by its RFC
	 * @return a reference to the {@link org.nlsaugment.net.telnet.TelnetOption}, or
	 * <code>null</code> if the connection does not support the option.
	 */
	public TelnetOption getOption(String name);
	
	/**
	 * Return an array of references to the <code>TelnetOption</code> instances
	 * representing the options supported (but not necessarily enabled) by the
	 * current connection.
	 * 
	 * @return a reference to an array of {@link org.nlsaugment.net.telnet.TelnetOption}s
	 */
	public TelnetOption[] getOptionList();
	
	/**
	 * Request that the remote side of the connection enable the named Telnet
	 * option.  That is, initiate negotiation with 'DO option'.  The connection
	 * will buffer any received data internally until the negotiation is
	 * complete, or until a timeout occurs.  That data will be returned on
	 * subsequent read operations.
	 * 
	 * @param name the name of the Telnet option, as specified by its RFC
	 * @return <code>true</code> if the remote side agrees to enable the option,
	 * or <code>false</code> otherwise.
	 * @throws {@link java.net.SocketTimeoutException SocketTimeoutException}
	 */
	public boolean requestOption(String name) throws SocketTimeoutException, IOException;
	
	/**
	 * Offer to perform the named Telnet option on the local side of the
	 * connection.  That is, initiate negotiation with 'WILL option'.  The
	 * connection will buffer any received data internally until the negotiation
	 * is complete, or until a timeout occurs.  That data will be returned on
	 * subsequent read operations.
	 * 
	 * @param name the name of the Telnet option, as specified by its RFC
	 * @return <code>true</code> if the remote side wants the local side to
	 * enable the option, or <code>false</code> otherwise.
	 * @throws {@link java.net.SocketTimeoutException SocketTimeoutException}
	 */
	public boolean offerOption(String name) throws SocketTimeoutException, IOException;
}
