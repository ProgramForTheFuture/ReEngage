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
package org.nlsaugment.augterm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.nlsaugment.driver.KeysetCharacterMap;
import org.nlsaugment.driver.KeysetDriver;
import org.nlsaugment.driver.KeysetNotFoundException;
import org.nlsaugment.event.KeysetAdapter;
import org.nlsaugment.event.KeysetEvent;
import org.nlsaugment.event.KeysetListener;
import org.nlsaugment.net.telnet.TelnetConstants;
import org.nlsaugment.net.telnet.TelnetEventException;
import org.nlsaugment.net.telnet.TelnetInputStream;
import org.nlsaugment.net.telnet.TelnetOption;
import org.nlsaugment.net.telnet.TelnetSocket;
import org.nlsaugment.swing.FormattedNumberField;
import org.nlsaugment.swing.ParameterDialog;
import org.nlsaugment.swing.TerminalPane;

/**
 * Provides a client terminal emulator for the Augment system.  Currently handles
 * a subset of the VAT-0 protocol.  Runs as an applet or as an application.
 *
 * @author Howard Palmer
 * @version $Id: AugTerm.java 135 2005-11-03 04:15:04Z Howard $
 */
public class AugTerm extends JApplet {
  private static final long serialVersionUID = 1;

	private final static int DEFAULT_TERMINAL_ROWS = 24;
	private final static int DEFAULT_TERMINAL_COLUMNS = 80;
  private final static String AUGTERM_VERSION = "0.21";

	private final static String[][] paramInfo = {
			{ "host",		"name or IP address",		"server host" },
			{ "port",		"TCP port number (23)",		"server TCP port" },
			{ "rows",		"integer (24)",				"number of rows in terminal" },
			{ "columns",	"integer (80)",			"number of columns in terminal" }
	};

	private Hashtable<String, String> appParams = null;

	private boolean isApplet = true;

	private JFrame tframe;
	private JScrollPane spane;
	private TerminalPane tpane;
	private JMenuBar menuBar;
	private JMenu telnetMenu;
	private JMenu termMenu;
	private JPanel statusPanel;
	private JLabel statusConnect;

	private KeyListener keyListener;
	private MouseListener mouseListener;

	// These are maintained in tpane coordinates, which have the top line
	// at address 0, while VAT-0 has the bottom line at address 0.
	private Point cwp;
	private Point ttypos;
	private Point bugpos;
	private Point mousePos;
	private int ttyTop;
	private int ttyBottom;
	private boolean positioned;
	private boolean reverseMode;
	private boolean coordMode;
	private boolean mouseReleasing;
	private boolean keyWithMouseButton;
	private int mouseMask;

	private static final int MOUSE_LEFT = 4;
	private static final int MOUSE_MIDDLE = 2;
	private static final int MOUSE_RIGHT = 1;

	private static final byte[] mouseCodes = { 000, 004, 030, 002, 001, 033, 027, 000 };

	private AugtermListener listener = null;
	private TelnetSocket socket = null;
	private String lastHost = null;
	private int lastPort = TelnetConstants.TELNET_PORT;

	public AugTerm() throws HeadlessException {
		super();
	}

	@Override
	public void init() {
		super.init();

		if (isApplet) {
			setLayout(new FlowLayout());
			final JButton startButton = new JButton("Press to start");
			startButton.setActionCommand("start");
			startButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent event) {
					getContentPane().remove(startButton);
					getContentPane().add(new JLabel("Started."));
					AugTerm.this.validate();
					startButton.removeActionListener(this);
					makeAppletGUI();
				}

			});
			getContentPane().add(startButton);
			validate();
		} else {
			makeAppletGUI();
			tpane.requestFocusInWindow();
		}
	}

	/**
	 *
	 */
	@Override
	public void start() {
		super.start();
		final String host = getParameter("host");
		if (host != null) {
			int port = TelnetConstants.TELNET_PORT;
			String portStr = getParameter("port");
			if (portStr != null) {
				try {
					port = Integer.parseInt(portStr);
				} catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(tframe,
							"Invalid integer value for parameter: port",
							"Parameter Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}

			final int portno = port;
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						connectHost(host, portno);
					}
				});
			} catch (InterruptedException iex) {

			} catch (InvocationTargetException ite) {

			}
		}
	}

	/**
	 * Stops the applet.  The terminal window and any current Telnet connection
	 * are closed.
	 */
	@Override
	public void stop() {
		if (tframe != null) {
			tframe.setVisible(false);
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException iox) {

			}
			socket = null;
		}
		setVisible(false);
		super.stop();
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	@Override
	public String getAppletInfo() {
		return "AugTerm: version " + AUGTERM_VERSION;
	}

	@Override
	public String getParameter(String pName) {
		return (appParams == null) ? super.getParameter(pName)
				: appParams.get(pName);
	}

	@Override
	public String[][] getParameterInfo() {
		return paramInfo;
	}

	private void makeAppletGUI() {
		if (tframe == null) {
			tframe = new JFrame(getAppletInfo());
		}

		tframe.setLayout(new BorderLayout());

		listener = new AugtermListener();
		menuBar = new JMenuBar();

		telnetMenu = new JMenu("AugTerm");

		JMenuItem mitem = new JMenuItem("Connect...");
		mitem.setActionCommand("telnet.connect");
		mitem.addActionListener(listener);

		telnetMenu.add(mitem);

		mitem = new JMenuItem("Disconnect");
		mitem.setActionCommand("telnet.disconnect");
		mitem.addActionListener(listener);

		telnetMenu.add(mitem);

		mitem = new JMenuItem("Exit");
		mitem.setActionCommand("telnet.exit");
		mitem.addActionListener(listener);

		telnetMenu.add(mitem);

		menuBar.add(telnetMenu);

		termMenu = new JMenu("Terminal");

		mitem = new JMenuItem("Clear");
		mitem.setActionCommand("term.clear");
		mitem.addActionListener(listener);

		termMenu.add(mitem);

		menuBar.add(termMenu);
		tframe.setJMenuBar(menuBar);

		int rows = DEFAULT_TERMINAL_ROWS;
		int columns = DEFAULT_TERMINAL_COLUMNS;

		String s = getParameter("rows");
		if (s != null) {
			try {
				rows = Integer.parseInt(s);
			} catch (NumberFormatException nfe) {
				System.err.println("Invalid integer value for parameter: rows");
			}
		}
		s = getParameter("columns");
		if (s != null) {
			try {
				columns = Integer.parseInt(s);
			} catch (NumberFormatException nfe) {
				System.err.println("Invalid integer value for parameter: columns");
			}
		}
		tpane = new TerminalPane(rows, columns);

		// Limit the terminal pane to be 80% of the current screen dimensions
		Dimension d = tpane.getPreferredSize();
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice();
		DisplayMode dm = gd.getDisplayMode();
		int maxHeight = (dm.getHeight() * 80) / 100;
		int maxWidth = (dm.getWidth() * 80) / 100;
		if (d.height > maxHeight) {
			d.height = maxHeight;
			tpane.setPreferredSize(d);
		}
		if (d.width > maxWidth) {
			d.width = maxWidth;
			tpane.setPreferredSize(d);
		}

		spane = new JScrollPane(tpane,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Make the scroll pane wide enough to include the vertical scroll bar,
		// whether it is currently showing or not.
		d = spane.getPreferredSize();
		JScrollBar sb = spane.getVerticalScrollBar();
		Dimension sbd = sb.getMaximumSize();
		d.width += sbd.width;
		spane.setPreferredSize(d);

		tframe.add(spane, BorderLayout.CENTER);

		statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));

		statusPanel.add(Box.createRigidArea(new Dimension(16, 0)));

		statusConnect = new JLabel("Not connected");
		statusPanel.add(statusConnect);

		statusPanel.setBackground(new Color(200, 200, 255));

		tframe.add(statusPanel, BorderLayout.SOUTH);

		tframe.pack();
		tframe.setVisible(true);
	}

	private void resetEmulation() {
		ttyTop = 0;
		ttyBottom = tpane.getRows() - 1;
		positioned = false;
		reverseMode = false;
		coordMode = false;
		mouseReleasing = false;
		keyWithMouseButton = false;
		cwp = new Point(0, 0);
		ttypos = new Point(0, 0);
		bugpos = null;
		mousePos = null;
		mouseMask = 0;
	}

	private void parseCmdLine(String[] args) {
		appParams = new Hashtable<String, String>();
		int pos = 0;
		String pname = null;
		for (int i = 0; i < args.length; ++i) {
			if (pname != null) {
				appParams.put(pname, args[i]);
				pname = null;
			} else if (args[i].startsWith("-")) {
				pname = args[i].substring(1);
			} else {
				switch (pos) {
				case 0:
					appParams.put("host", args[i]);
					++pos;
					break;
				case 1:
					appParams.put("port", args[i]);
					++pos;
					break;
				default:
					System.err.println("Argument " + args[i] + " ignored.");
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

/*		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] fonts = ge.getAvailableFontFamilyNames();
		for (String f : fonts) {
			System.out.println(f);
		}
*/
		final AugTerm aterm = new AugTerm();
		aterm.isApplet = false;
		aterm.tframe = new JFrame(aterm.getAppletInfo());
    aterm.tframe.addWindowListener(new WindowAdapter() {
        public void windowClosing(final WindowEvent e) {
          aterm.exit();
        }
      });

    //		aterm.tframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Parse the command line into appParams
		try {
			aterm.parseCmdLine(args);
		} catch (RuntimeException rex) {
			System.err.println(rex);
			System.exit(-1);
		}

		aterm.init();
		aterm.start();
	}

	private final class TelnetReader extends Thread {

		@Override
		public void interrupt() {
			// TODO Auto-generated method stub
			super.interrupt();
		}

		@Override
		public void run() {

			while (socket != null) {
				try {
					TelnetInputStream in = socket.getInputStream();
					int inch = in.read();

					// Check end-of-stream
					if (inch == -1)
						break;

					// Ignore pad characters
					if (inch == 0177)
						continue;

					// Check for printable characters
					if (inch >= 040) {
						if (cwp.x < tpane.getColumns()) {
							char[] ch = Character.toChars(inch);
							tpane.putChar(cwp.y, cwp.x, ch[0], reverseMode);
							++cwp.x;
						}
					} else {
						switch (inch) {
						case 000:
							break;
						case 007:
							Toolkit.getDefaultToolkit().beep();
							break;
						case 010:
							// Should this backspace to the previous line
							if (cwp.x > 0) {
								--cwp.x;
							}
							break;
						case 012:
							if (positioned) {
								++cwp.x;
							} else {
								++cwp.y;
								if (cwp.y >= tpane.getRows()) {
									tpane.scrollUp(0, 0, tpane.getRows() - 1, tpane
											.getColumns() - 1, 1);
									cwp.y = tpane.getRows() - 1;
								}
							}
							break;
						case 015:
							if (positioned) {
								++cwp.x;
							} else {
								cwp.x = 0;
							}
							break;
						case 033:
							// Begin protocol string
							doCommand(socket);
							break;
						}
					}
				} catch (TelnetEventException tee) {
					// System.err.println(tee);
				} catch (IOException iox) {

				} catch (BadLocationException ble) {
					System.err.println(ble);
				}
			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					listener.actionPerformed(new ActionEvent(
							TelnetReader.this, 1, "telnet.disconnect"));
				}
			});
		}

		private void doCommand(TelnetSocket socket)
				throws TelnetEventException, IOException, BadLocationException {
			TelnetInputStream in = socket.getInputStream();
			boolean command = false;
			while (!command) {
				int inch = in.read();
				switch (inch) {
				case -1:
					// Unexpected end of stream
					break;
				case 0177:
					// Pad character
					break;
				case 040:
					// Position
					Point pos = readXY(in);
					ttypos.x = cwp.x;
					ttypos.y = cwp.y;
					cwp = pos;
					positioned = true;
					command = true;
					break;
				case 041:
					// Specify TTY simulation window
					ttyTop = readY(in);
					ttyBottom = readY(in);
					ttypos.x = 0;
					ttypos.y = ttyTop;
					command = true;
					break;
				case 042:
					// Resume TTY window
					if (positioned) {
						cwp = ttypos;
					}
					command = true;
					break;
				case 043:
					// Write a string of blanks
					int n = readX(in);
					final StringBuilder sb = new StringBuilder(n);
					for (int i = 0; i < n; ++i) {
						sb.append(' ');
					}
					try {
						SwingUtilities.invokeAndWait(new Runnable() {

							public void run() {
								try {
									tpane.putString(cwp.y, cwp.x, sb.toString(), reverseMode);
								} catch (BadLocationException ble) {

								}
							}

						});
					} catch (InterruptedException iex) {
					} catch (InvocationTargetException ite) {
					}
					command = true;
					break;
				case 044:
					// Delete selected line
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								try {
									tpane.eraseLine(cwp.y);
								} catch (BadLocationException ble) {
								}
							}
						});
					} catch (InterruptedException iex) {
					} catch (InvocationTargetException ite) {
					}
					command = true;
					break;
				case 045:
					// Insert line
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								try {
									tpane.insertLine(cwp.y);
								} catch (BadLocationException ble) {

								}
							}
						});
					} catch (InterruptedException iex) {
					} catch (InvocationTargetException ite) {
					}
					command = true;
					break;
				case 046:
					// Bug selection
					bugpos = readXY(in);
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								try {
									tpane.highlightCharacter(bugpos.y, bugpos.x);
								} catch (BadLocationException ble) {
								}
							}
						});
					} catch (InterruptedException iex) {
					} catch (InvocationTargetException ite) {
					}
					command = true;
					break;
				case 047:
					// Pop bug
					if (bugpos != null) {
						try {
							SwingUtilities.invokeAndWait(new Runnable() {
								public void run() {
									try {
										tpane.highlightCharacter(bugpos.y, bugpos.x);
										bugpos = null;
									} catch (BadLocationException ble) {
									}
								}
							});
						} catch (InterruptedException iex) {
						} catch (InvocationTargetException ite) {
						}
					}
					command = true;
					break;
				case 050:
					// Clear screen
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								tpane.clear();
							}
						});
					} catch (InterruptedException iex) {
					} catch (InvocationTargetException ite) {
					}
					command = true;
					break;
				case 051:
					// Reset
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								tpane.clear();
								ttyTop = 0;
								ttyBottom = tpane.getRows() - 1;
								ttypos = new Point(0, 0);
								positioned = false;
							}
						});
					} catch (InterruptedException iex) {
					} catch (InvocationTargetException ite) {
					}
					command = true;
					break;
				case 052:
					// Write graphics display
					readSkip(in, 1);
					n = readX(in);
					readSkip(in, 1);
					command = true;
					break;
				case 054:
					// Close printer
					command = true;
					break;
				case 055:
					// Interrogate
					OutputStream out = socket.getOutputStream();
					out.write(034);
					out.write(046);
					sendX(out, tpane.getColumns() - 1);
					sendX(out, tpane.getRows() - 1);
					out.write(046);
					out.write(040);
					out.write(041);
					command = true;
					break;
				case 056:
					// Standout mode on
					reverseMode = true;
					command = true;
					break;
				case 057:
					// Standout mode off
					reverseMode = false;
					command = true;
					break;
				case 060:
					// Coordinate mode off
					coordMode = false;
					command = true;
					break;
				case 061:
					// Coordinate mode on
					coordMode = true;
					command = true;
					break;
				case 063:
				case 064:
					System.err.println("Printer command!");
					command = true;
					break;
				case 065:
					// Scroll
					final int left = readX(in);
					final int right = readX(in);
					final int top = readY(in);
					final int bottom = readY(in);
					final int nlines = readX(in);
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								try {
									if (nlines < 0) {
										tpane.scrollDown(top, left, bottom, right, -nlines);
									} else if (nlines > 0) {
										tpane.scrollUp(top, left, bottom, right, nlines);
									}

								} catch (BadLocationException ble) {
								}
							}
						});
					} catch (InterruptedException iex) {
					} catch (InvocationTargetException ite) {
					}
					command = true;
					break;
				default:
					command = true;
					break;
				}
			}
		}

		private int readX(TelnetInputStream in) throws TelnetEventException, IOException {
			int inch = in.read();
			if (inch == 036) {
				int high = in.read() - 040;
				int low = in.read() - 040;
				inch = ((high & 077) << 6) | (low & 077);
			} else {
				inch -= 040;
			}
			return inch;
		}

		private int readY(TelnetInputStream in) throws TelnetEventException, IOException {
			int y = readX(in);
			y = tpane.getRows() - y - 1;
			return y;
		}

		private Point readXY(TelnetInputStream in) throws TelnetEventException, IOException {
			int x = readX(in);
			int y = readY(in);
			return new Point(x, y);
		}

		private void readSkip(TelnetInputStream in, int n) throws TelnetEventException, IOException {
			for (int i = 0; i < n; ++i) {
				in.read();
			}
		}
	}

	private void sendX(OutputStream out, int x) throws IOException {
		if ((x >= 0) && (x < 93)) {
			out.write((x + 040) & 0xff);
		} else {
			out.write(((x >> 6) + 040) & 0xff);
			out.write((x + 040) & 0xff);
		}
	}

	private void sendY(OutputStream out, int y) throws IOException {
		y = tpane.getRows() - y - 1;
		sendX(out, y);
	}

	private void sendKey(final char ch) {
		int ich = (int) ch;
		try {
			OutputStream out = socket.getOutputStream();
			if (mouseMask != 0) {
				keyWithMouseButton = true;
				switch (mouseMask) {
				case 1:
					// Suppress key
					break;
				case 2:
					if (KeysetCharacterMap.CASE0.indexOf(ch) > 0) {
						ich = KeysetCharacterMap.CASE1.charAt(KeysetCharacterMap.CASE0.indexOf(ch));
					}
					out.write(ich);
					break;
				case 3:
					if (Character.isLetter(ch)) {
						ich &= 037;
					}
					out.write(ich);
					break;
				case 4:
					if (KeysetCharacterMap.CASE0.indexOf(ch) > 0) {
						ich = KeysetCharacterMap.CASE2.charAt(KeysetCharacterMap.CASE0.indexOf(ch));
					}
					out.write(ich);
					break;
				case 5:
					// Suppress key
					break;
				case 6:
				case 7:
					out.write(034);
					out.write(043);
					out.write(mouseMask + 0100);
					Point rowcolPos = tpane
					.translate(mousePos);
					sendX(out, rowcolPos.x);
					sendY(out, rowcolPos.y);
					out.write(ich);
					break;
				}
			} else {
				if (ich == 012) {
					// "Enter" is "OK" in coordinate mode
					if (coordMode) {
						out.write(034);
						out.write(042);
						Point rowcolPos = tpane.getMousePosition();
						if (rowcolPos == null) {
							rowcolPos = new Point(0, 0);
						} else {
							rowcolPos = tpane.translate(rowcolPos);
						}
						sendX(out, rowcolPos.x);
						sendY(out, rowcolPos.y);
						out.write(004);
					} else {
						out.write(ich);
					}
				} else {
					out.write(ich);
					if (ich == 015) {
						out.write((byte)012);
					}
				}
			}
		} catch (IOException iox) {
			System.err.println(iox);
		}
	}

	private void connectHost(String host, int port) {
		try {
			statusConnect.setText("Connecting to " + host);
			socket = new TelnetSocket(host, port);
			lastHost = host;
			lastPort = port;
			TelnetOption echoopt = new TelnetOption(1, "ECHO",
					true, true);
			TelnetOption sgaopt = new TelnetOption(3,
					"SUPPRESS-GO-AHEAD", true, true);
			socket.addOption(echoopt);
			socket.addOption(sgaopt);
			statusConnect.setText("Connected: " + host);

			resetEmulation();
			Thread rdr = new TelnetReader();
			rdr.start();
			keyListener = new KeyAdapter() {

				@Override
				public void keyTyped(KeyEvent event) {
				    if (socket != null) {
						char ch = event.getKeyChar();
            sendKey(ch);
          }
        }
      };

      KeysetDriver kd = KeysetDriver.getInstance();
      KeysetListener keysetListener = new KeysetAdapter() {
        @Override
        public void keyTyped(KeysetEvent event) {
          if (socket != null) {
            char ch = event.getKeyChar();
            sendKey(ch);
          }
        }
      };
      try {
        kd.register(keysetListener);
        kd.start();
      } catch (KeysetNotFoundException e) {
        statusConnect.setText("Connect: " + host + " -- Chord keyset is not connected.");
      }

      tpane.addKeyListener(keyListener);

			tpane.setInputMap(JComponent.WHEN_FOCUSED, null);

			mouseMask = 0;
			mouseReleasing = false;

			mouseListener = new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent event) {
					if (!mouseReleasing) {
						mousePos = event.getPoint();
						if (SwingUtilities.isLeftMouseButton(event)) {
							mouseMask |= MOUSE_LEFT;
						} else if (SwingUtilities
								.isMiddleMouseButton(event)) {
							mouseMask |= MOUSE_MIDDLE;
						} else if (SwingUtilities
								.isRightMouseButton(event)) {
							mouseMask |= MOUSE_RIGHT;
						}
					}
				}

				@Override
				public void mouseReleased(MouseEvent event) {
					if (!mouseReleasing) {
						mouseReleasing = true;
						if ((socket != null)
								&& (mouseCodes[mouseMask] != 0)) {
							try {
								OutputStream out = socket
										.getOutputStream();
								if (coordMode) {
									Point rowcolPos = tpane.translate(mousePos);
									if (keyWithMouseButton) {
										keyWithMouseButton = false;
										switch (mouseMask) {
										case 1:
										case 5:
											out.write(034);
											out.write(042);
											sendX(out, rowcolPos.x);
											sendY(out, rowcolPos.y);
											out.write(mouseCodes[mouseMask]);
											break;
										case 2:
										case 3:
										case 4:
											break;
										case 6:
										case 7:
											out.write(034);
											out.write(043);
											out.write(0100);
											sendX(out, rowcolPos.x);
											sendY(out, rowcolPos.y);
											break;
										}
									} else {
										switch (mouseMask) {
										case 1:
										case 2:
										case 3:
										case 4:
										case 5:
											out.write(034);
											out.write(042);
											sendX(out, rowcolPos.x);
											sendY(out, rowcolPos.y);
											out.write(mouseCodes[mouseMask]);
											break;
										case 6:
											out.write(027);
											break;
										case 7:
											break;
										}
									}
								}
							} catch (IOException iox) {

							}
						}
					}
					if (SwingUtilities.isLeftMouseButton(event)) {
						mouseMask &= ~MOUSE_LEFT;
					} else if (SwingUtilities
							.isMiddleMouseButton(event)) {
						mouseMask &= ~MOUSE_MIDDLE;
					} else if (SwingUtilities
							.isRightMouseButton(event)) {
						mouseMask &= ~MOUSE_RIGHT;
					}
					if (mouseMask == 0) {
						mouseReleasing = false;
					}
				}

			};

			tpane.addMouseListener(mouseListener);

		} catch (AccessControlException ace) {
			JOptionPane
					.showMessageDialog(
							tframe,
							"Connection denied by applet security manager.",
							"Telnet Connect Error",
							JOptionPane.ERROR_MESSAGE);
			statusConnect.setText("Not connected.");
		} catch (UnknownHostException uhe) {
			JOptionPane.showMessageDialog(tframe, "Unknown host.",
					"Telnet Connect Error",
					JOptionPane.ERROR_MESSAGE);
			statusConnect.setText("Not connected.");
		} catch (IOException iox) {
			JOptionPane.showMessageDialog(tframe,
					"Connection failed.",
					"Telnet Connection Error",
					JOptionPane.ERROR_MESSAGE);
			statusConnect.setText("Not connected.");
		}
	}

  private void disconnect() {
    if (socket != null) {
      tpane.removeKeyListener(keyListener);
      tpane.removeMouseListener(mouseListener);
      try {
        socket.close();
      } catch (IOException iox) {

      }
      socket = null;
      statusConnect.setText("Not connected");
    }
  }

  private void exit() {
    disconnect();
    System.exit(0);
  }

	private final class AugtermListener implements ActionListener {

		public void actionPerformed(ActionEvent event) {
			String command = event.getActionCommand();
			if (command.equals("telnet.connect")) {
				ParameterDialog pd = new ParameterDialog(tframe,
						"Telnet Connection Target");
				JTextField host = new JTextField(32);
				if (lastHost != null) {
					host.setText(lastHost);
				}
				pd.addParameter("host", "Enter host name or IP address", host);
				FormattedNumberField port = new FormattedNumberField(
						FormattedNumberField.TYPE_INTEGER,
						FormattedNumberField.FORMAT_PLAIN, new Integer(lastPort),
						new Integer(0), new Integer(65536));
				port.setColumns(5);
				pd.addParameter("port", "TCP port number", port);
				boolean result = pd.getParameters();
				if (result) {
					int portno = ((Number) port.getValue()).intValue();
					connectHost(host.getText(), portno);
				}
			} else if (command.equals("telnet.disconnect")) {
        disconnect();
      } else if (command.equals("telnet.exit")) {
        exit();
			} else if (command.equals("term.clear")) {
				tpane.clear();
				resetEmulation();
			}
		}
	}
}
