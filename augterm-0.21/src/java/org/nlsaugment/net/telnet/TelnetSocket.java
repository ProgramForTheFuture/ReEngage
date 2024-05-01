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
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * This class extends the {@link java.net.Socket Socket} class to provide a
 * socket that is specialized for communication using the Telnet protocol,
 * as defined in RFC854.  As with the <code>Socket</code> class, <code>TelnetSocket</code>
 * is to be used on the client side only.  The {@link #TelnetServerSocket TelnetServerSocket}
 * is used for the server side.
 * 
 * @author Howard Palmer
 * @version %I% %G%
 * @see java.net.Socket
 * 
 */
public class TelnetSocket extends Socket implements TelnetConstants, TelnetOptions {
	
	private ArrayList<TelnetOption> optionList = new ArrayList<TelnetOption>();
	private TelnetInputStream inStream = null;
	
	/**
	 * Creates an unconnected Telnet socket, with the system-default type of SocketImpl.
	 * Assumes the {@link #TELNET_PORT TELNET_PORT} as the port number.
	 */
	public TelnetSocket() {
		super();
	}

	/**
	 * Creates a Telnet socket and connects it to the <code>TELNET_PORT</code> on the named
	 * host.
	 * 
	 * @param host		server host DNS name or IP address, or <code>null</code> for
	 * 					the loopback address
	 * @throws UnknownHostException	if the IP address of the host could not be determined.
	 * @throws IOException			if an I/O error occurs when creating the socket.
	 * @throws SecurityException	if a security manager exists and its <code>checkConnect</code>
	 * 								method doesn't allow the operation.
	 * @see java.net.Socket#Socket(java.lang.String, int)
	 */
	public TelnetSocket(String host) throws UnknownHostException,
											IOException,
											SecurityException {
		this(host, TELNET_PORT);
	}

	/**
	 * Creates a Telnet socket and connects it to the specified port number on the named
	 * host.
	 * 
	 * @param host		server host DNS name or IP address, or <code>null</code> for
	 * 					the loopback address.
	 * @param port		the port number.
	 * @throws UnknownHostException	if the IP address of the host could not be determined.
	 * @throws IOException			if an I/O error occurs when creating the socket.
	 * @throws SecurityException	if a security manager exists and its <code>checkConnect</code>
	 * 								method doesn't allow the operation.
	 * @see java.net.Socket#Socket(java.lang.String, int)
	 */
	public TelnetSocket(String host, int port) throws UnknownHostException,
													  IOException,
													  SecurityException {
		super(host, port);
	}

	/**
	 * Creates a Telnet socket and connects it to the specified remote host on the
	 * specified remote port. The TelnetSocket will also bind() to the local address
	 * and port supplied.
	 * 
	 * @param host			server host DNS name or IP address, or <code>null</code> for
	 * 						the loopback address.
	 * @param port			the server port number.
	 * @param localAddr		the local address to be used for the socket.
	 * @param localPort		the local port number to be used for the socket.
	 * @throws IOException			if an I/O error occurs when creating the socket.
	 * @throws SecurityException	if a security manager exists and its <code>checkConnect</code>
	 * 								method doesn't allow the operation.
	 * @see java.net.Socket#Socket(java.net.InetAddress, int, java.net.InetAddress, int)
	 */
	public TelnetSocket(InetAddress host, int port, InetAddress localAddr, int localPort)
	throws IOException, SecurityException {
		super(host, port, localAddr, localPort);
	}

	/**
	 * Creates a stream socket and connects it to the specified port number at
	 * the specified IP address.
	 * 
	 * @param address	the IP address.
	 * @param port		the port number.
	 * @throws IOException			if an I/O error occurs when creating the socket.
	 * @see java.net.Socket#Socket(java.net.InetAddress, int)
	 */
	public TelnetSocket(InetAddress address, int port) throws IOException {
		super(address, port);
	}

	/**
	 * Creates an unconnected Telnet socket, specifying the type of proxy, if any, that
	 * should be used regardless of any other settings.
	 * 
	 * @param proxy		a {@link java.net.Proxy Proxy} object specifying what kind of
	 * 					proxying should be used.
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @see Socket#Socket(java.net.Proxy)
	 */
	public TelnetSocket(Proxy proxy) throws IllegalArgumentException {
		super(proxy);
	}

	/**
	 * Creates an unconnected Socket with a user-specified SocketImpl.
	 * 
	 * @param impl		an instance of a <code>SocketImpl</code> the subclass
	 * 					wishes to use on the Socket.
	 * @throws SocketException	if there is an error in the underlying protocol,
	 * 							such as a TCP error.
	 */
	protected TelnetSocket(SocketImpl impl) throws SocketException {
		super(impl);
	}

	/**
	 * Creates a Telnet socket and connects it to the specified remote host on the
	 * specified remote port. The TelnetSocket will also bind() to the local address
	 * and port supplied.
	 * 
	 * @param host
	 * @param port
	 * @param localAddr
	 * @param localPort
	 * @throws IOException
	 */
	public TelnetSocket(String host, int port, InetAddress localAddr,
			int localPort) throws IOException {
		super(host, port, localAddr, localPort);
	}

	@Override
	public TelnetInputStream getInputStream() throws IOException {
		if (inStream == null) {
			inStream = new TelnetInputStream(this, new BufferedInputStream(
					super.getInputStream()));
		}
		return inStream;
	}
	
	public void addOption(TelnetOption option) {
		TelnetOption other = getOption(option.getOptionName());
		if (other == null) {
			option.associate();
			optionList.add(option);
		} else {
			throw new IllegalStateException("Telnet option "
					+ option.getOptionName() + " is already present");

		}
	}
	
	public TelnetOption getOption(String name) {
		for (TelnetOption opt : optionList) {
			if (opt.getOptionName().equalsIgnoreCase(name)) {
				return opt;
			}
		}
		return null;
	}

	public TelnetOption getOption(int code) {
		for (TelnetOption opt : optionList) {
			if (code == opt.getOptionCode()) {
				return opt;
			}
		}
		return null;
	}
	
	public TelnetOption[] getOptionList() {
		TelnetOption[] opts = new TelnetOption[optionList.size()];
		return optionList.toArray(opts);
	}

	public boolean offerOption(String name) throws SocketTimeoutException, IOException {
		TelnetOption opt = getOption(name);
		if (opt == null) {
			throw new IllegalArgumentException("Unknown option: " + name);
		}
		
		if (!isConnected()) {
			throw new IllegalStateException("Socket must be connected");
		}
		
		byte[] outmsg = null;
		int action = opt.requestLocal(true);
		byte optcode = (byte)opt.getOptionCode();
		switch (action) {
		case TelnetOption.IGNORE:
			break;
		case TelnetOption.SEND_WILL:
			byte[] willmsg = { IAC, WILL, optcode };
			outmsg = willmsg;
			break;
		case TelnetOption.SEND_WONT:
			byte[] wontmsg = { IAC, WONT, optcode };
			outmsg = wontmsg;
			break;
		}
		if (outmsg != null) {
			OutputStream out = getOutputStream();
			out.write(outmsg);
			out.flush();
		}
		
		if (opt.isUnstableLocally()) {
			int oldtmo = getSoTimeout();
			setSoTimeout(1000);
			getInputStream().mark(512);
			byte[] inbuf = new byte[16];
			while (opt.isUnstableLocally()) {
				try {
					inStream.read(inbuf);
				} catch (SocketTimeoutException ste) {
					break;
				} catch (TelnetEventException tee) {
					// Ignore these
				} finally {
					setSoTimeout(oldtmo);
					inStream.reset();				
				}
			}
		}
		
		return opt.isEnabledLocally();
	}

	public boolean requestOption(String name) throws SocketTimeoutException, IOException {
		TelnetOption opt = getOption(name);
		if (opt == null) {
			throw new IllegalArgumentException("Unknown option: " + name);
		}
		
		if (!isConnected()) {
			throw new IllegalStateException("Socket must be connected");
		}
		
		byte[] outmsg = null;
		int action = opt.requestRemote(true);
		byte optcode = (byte)opt.getOptionCode();
		switch (action) {
		case TelnetOption.IGNORE:
			break;
		case TelnetOption.SEND_DO:
			byte[] domsg = { IAC, DO, optcode };
			outmsg = domsg;
			break;
		case TelnetOption.SEND_DONT:
			byte[] dontmsg = { IAC, DONT, optcode };
			outmsg = dontmsg;
			break;
		}
		if (outmsg != null) {
			OutputStream out = getOutputStream();
			out.write(outmsg);
			out.flush();
		}
		
		if (opt.isUnstableRemotely()) {
			int oldtmo = getSoTimeout();
			setSoTimeout(1000);
			getInputStream().mark(512);
			byte[] inbuf = new byte[16];
			while (opt.isUnstableRemotely()) {
				try {
					inStream.read(inbuf);
				} catch (SocketTimeoutException ste) {
					break;
				} catch (TelnetEventException tee) {
					// Ignore these
				} finally {
					setSoTimeout(oldtmo);
					inStream.reset();				
				}
			}
		}
		
		return opt.isEnabledRemotely();
	}
	
}
