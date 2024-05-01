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
 * This class represents a Telnet option.  It can be subclassed to represent
 * options that include additional information, such as the Terminal Type
 * option.
 * 
 * @author Howard Palmer
 * @version $Id: TelnetOption.java 135 2005-11-03 04:15:04Z Howard $
 *
 */
public class TelnetOption {

	/**
	 * Externally visible option states.
	 */
	/**
	 * Option is disabled.
	 */
	public final static int DISABLED = 0;
	/**
	 * Option is enabled.
	 */
	public final static int ENABLED = 1;
	/**
	 * Option is being negotiated.
	 */
	public final static int UNSTABLE = 2;
	
	/**
	 * Internal option states as defined in RFC 1143.
	 */
	/**
	 * The indicated side of the connection believes the option is disabled.
	 */
	protected final static int NO = 0;
	/**
	 * The indicated side of the connection believes the option is enabled.
	 */
	protected final static int YES = 1;
	/**
	 * The indicated side of the connecton wants the option disabled.
	 */
	protected final static int WANTNO = 2;
	/**
	 * The indicated side of the connection wants the option enabled.
	 */
	protected final static int WANTYES = 3;
	
	protected final static int IGNORE = 0;
	protected final static int SEND_WILL = 1;
	protected final static int SEND_WONT = 2;
	protected final static int SEND_DO = 3;
	protected final static int SEND_DONT = 4;
	
	/**
	 * A flag that can be set to indicate that ths instance is associated with
	 * a Telnet connection.
	 */
	private boolean associated = false;
	
	private final int optionCode;
	private final String optionName;
	private final boolean localSupport;
	private final boolean remoteSupport;
	private int us;
	private int him;
	private boolean usq;
	private boolean himq;
	
	/**
	 * Constructor for a Telnet option.
	 * 
	 * @param optionCode the option code as defined in the IANA Assigned Numbers RFC.
	 * @param optionName the option name as defined in the option RFC.
	 * @param localSupport <code>true</code> if the local side is prepared to perform
	 * the option.
	 * @param remoteSupport <code>true</code> if the local side is prepared for the
	 * remote side to perform the option.
	 */
	public TelnetOption(int optionCode, String optionName,
			boolean localSupport, boolean remoteSupport) {
		super();
		this.optionCode = optionCode;
		this.optionName = optionName;
		this.us = NO;
		this.him = NO;
		this.usq = false;
		this.himq = false;
		this.localSupport = localSupport;
		this.remoteSupport = remoteSupport;
	}

	/**
	 * Returns the option code for this option as defined in the IANA Assigned Numbers
	 * RFC.
	 * 
	 * @return an option code
	 */
	public int getOptionCode() {
		return optionCode;
	}
	
	/**
	 * Returns the option name.  This should be the name of the option as specified
	 * in the RFC which defines it.
	 * 
	 * @return the option name
	 */
	public String getOptionName() {
		return optionName;
	}
	
	/**
	 * Gets the local state of the option.
	 * 
	 * @return <code>DISABLED</code>, <code>ENABLED</code>, or <code>UNSTABLE</code>
	 */
	public int getLocalState() {
		int result = UNSTABLE;
		switch (us) {
		case NO:
			result = DISABLED;
			break;
		case YES:
			result = ENABLED;
			break;
		case WANTNO:
		case WANTYES:
			break;
		}
		return result;
	}
	
	/**
	 * Gets the remote state of the option.
	 * 
	 * @return <code>DISABLED</code>, <code>ENABLED</code>, or <code>UNSTABLE</code>
	 */
	public int getRemoteState() {
		int result = UNSTABLE;
		switch (him) {
		case NO:
			result = DISABLED;
			break;
		case YES:
			result = ENABLED;
			break;
		case WANTNO:
		case WANTYES:
			break;
		}
		return result;
	}
	
	/**
	 * Returns <code>true</code> if the option is disabled on the local side.
	 * 
	 * @return <code>true</code> if the option is enabled.
	 */
	public boolean isDisabledLocally() {
		return (getLocalState() == DISABLED);
	}
	
	/**
	 * Returns <code>true</code> if the option is enabled on the local side.
	 * 
	 * @return <code>true</code> if the option is enabled.
	 */
	public boolean isEnabledLocally() {
		return (getLocalState() == ENABLED);
	}
	
	/**
	 * Returns <code>true</code> if the option is unstable on the local side.
	 * 
	 * @return <code>true</code> if the option is unstable.
	 */
	public boolean isUnstableLocally() {
		return (getLocalState() == UNSTABLE);
	}
	
	public boolean isSupportedLocally() {
		return localSupport;
	}
	
	/**
	 * Returns <code>true</code> if the option is disabled on the remote side.
	 * 
	 * @return <code>true</code> if the option is enabled.
	 */
	public boolean isDisabledRemotely() {
		return (getRemoteState() == DISABLED);
	}
	
	/**
	 * Returns <code>true</code> if the option is enabled on the remote side.
	 * 
	 * @return <code>true</code> if the option is enabled.
	 */
	public boolean isEnabledRemotely() {
		return (getRemoteState() == ENABLED);
	}
	
	/**
	 * Returns <code>true</code> if the option is unstable on the remote side.
	 * 
	 * @return <code>true</code> if the option is unstable.
	 */
	public boolean isUnstableRemotely() {
		return (getRemoteState() == UNSTABLE);
	}
	
	public boolean isSupportedRemotely() {
		return remoteSupport;
	}
	
	public String toString() {
		return getOptionName();
	}
	
	protected synchronized int receivedWill() {
		int action = SEND_DONT;
		if (remoteSupport) {
			switch (him) {
			case NO:
				him = YES;
				action = SEND_DO;
				break;
			case YES:
				action = IGNORE;
				break;
			case WANTNO:
				// This should not happen
				him = (himq) ? YES : NO;
				himq = false;
				action = IGNORE;
				break;
			case WANTYES:
				if (himq) {
					him = WANTNO;
					himq = false;
				} else {
					him = YES;
				}
			}
		}
		return action;
	}
	
	protected synchronized int receivedWont() {
		int action = IGNORE;
		if (remoteSupport) {
			switch (him) {
			case NO:
				break;
			case YES:
				him = NO;
				action = SEND_DONT;
				break;
			case WANTNO:
				if (himq) {
					him = WANTYES;
					himq = false;
					action = SEND_DO;
				} else {
					him = NO;
				}
				break;
			case WANTYES:
				him = NO;
				himq = false;
				break;
			}
		}
		return action;
	}
	
	protected synchronized int requestRemote(boolean wantEnabled) {
		int action = IGNORE;
		if (wantEnabled && remoteSupport) {
			switch (him) {
			case NO:
				him = WANTYES;
				action = SEND_DO;
				break;
			case YES:
				break;
			case WANTNO:
				himq = true;
				break;
			case WANTYES:
				himq = false;
				break;
			}
		} else {
			switch (him) {
			case NO:
				break;
			case YES:
				him = WANTNO;
				action = SEND_DONT;
				break;
			case WANTNO:
				himq = false;
				break;
			case WANTYES:
				himq = true;
				break;
			}
		}
		return action;
	}
	
	protected synchronized int receivedDo() {
		int action = SEND_WONT;
		if (localSupport) {
			switch (us) {
			case NO:
				us = YES;
				action = SEND_WILL;
				break;
			case YES:
				action = IGNORE;
				break;
			case WANTNO:
				// This should not happen
				us = (usq) ? YES : NO;
				usq = false;
				action = IGNORE;
				break;
			case WANTYES:
				if (usq) {
					us = WANTNO;
					usq = false;
				} else {
					us = YES;
				}
			}
		}
		return action;
	}
	
	protected synchronized int receivedDont() {
		int action = IGNORE;
		if (localSupport) {
			switch (us) {
			case NO:
				break;
			case YES:
				us = NO;
				action = SEND_WONT;
				break;
			case WANTNO:
				if (usq) {
					us = WANTYES;
					usq = false;
					action = SEND_WILL;
				} else {
					us = NO;
				}
				break;
			case WANTYES:
				us = NO;
				usq = false;
				break;
			}
		}
		return action;		
	}
	
	protected synchronized int requestLocal(boolean wantEnabled) {
		int action = IGNORE;
		if (wantEnabled && localSupport) {
			switch (us) {
			case NO:
				us = WANTYES;
				action = SEND_WILL;
				break;
			case YES:
				break;
			case WANTNO:
				usq = true;
				break;
			case WANTYES:
				usq = false;
				break;
			}
		} else {
			switch (us) {
			case NO:
				break;
			case YES:
				us = WANTNO;
				action = SEND_WONT;
				break;
			case WANTNO:
				usq = false;
				break;
			case WANTYES:
				usq = true;
				break;
			}
		}
		return action;
	}
	
	protected void associate() throws IllegalStateException {
		if (associated) {
			throw new IllegalStateException("Illegal sharing of TelnetOption: " + this);
		}
		associated = true;
	}
}
