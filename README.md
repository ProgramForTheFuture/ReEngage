# ReEngage
Eric Rangell is interested in NLS, DCE, (D)DKR, CoEvolution, etc. I am encouraged. Let's see if we can collaborate and build momentum.

# Augterm2024
This effort intends to relocate the code for Augterm 0.21 from Sourceforge to Github and improve it so it can be compiled in a modern Java environment.  A proof of concept has been done which shows how a gamepad can be connected to a Raspberry Pi with a USB adapter (Radio Shack #25-164) and configured as a Linux joystick device.  The Java code defines a Keyset device that will interpret the joystick data and emit KeyTyped events to the Terminal.  In theory any HID (Human Interface) device can be configured for use with the system.

# Requirements
- Raspberry Pi or Linux machine with USB ports, Ethernet Port or Wi-Fi internet connectivity - look into the RetroPie as an option.
- apt-get is used to install joystick and jstest modules
- Java SDK installed, Ant for doing builds
- USB joystick/gamepad adapter - research available options 
- Optional USB Serial Adapter for testing other input devices
- Optional USB MIDI device (ex: Korg NanoKey)

# Desired enhancements
- Fix build.xml so it compiles on the latest version of Java using Ant
- Externalize configuration of range divider for X axis for gamepad support
- Fix bug in test program where Key Typed event always displays 0
- Support for devices that emulate a 3 button mouse
- Support for using a MIDI keyboard as a Keyset with 3 mouse buttons.
- Add a configuration panel to Augterm for testing various keyset devices
- Add an option to echo characters typed on the Keyset to the terminal

# Installation Instructions
- Note: The software was tested on a Raspberry PI 3 and 4.
- Boot the Pi and connect to the internet without the USB joystick adapter
- ls /dev/input
  You will see a list of input devices. Devices beginning with "js" are joysticks.
- Connect the joystick adapter to a USB port.
- ls /dev/input
  You should see a joystick device such as "js0"
- Optional: sudo apt-get install joystick
            sudo apt-get install jstest-gtk
  This will install joystick support, and a jstest utility 
  for more information see https://linux.die.net/man/1/jstest
- Verify that java and ant are installed
  java --version

  If java is not installed, obtain a Java Development Kit for your system from Oracle:
  https://www.oracle.com/java/technologies/downloads/
 
  If ant is not installed: sudo apt install ant
  (on a Mac: brew install ant)

- Clone the repository.  It will create a directory: ReEngage
- cd ReEngage/augterm-0.21
- ant
  Note: you will get deprecation warnings but the build should be successful
- cd /build/classes
- java org.nlsaugment.augterm.TestKeyset
  This runs the test program.  You should see the message:
  Type the 'z' character on the chord keyset to quit.
- Test each of the gamepad buttons: A, B, C, D, and the right joypad
  A=1 B=2 C=4 D=8 right joypad=16
  To type a letter, convert its position in the alphabet to binary and press the appropriate buttons.
  Verify that you get messages when buttons are pressed and released.
  When all buttons are released you get a Key Typed message, but it always shows a 0.
  To type a z: press right joypad, D, and B (16+8+2)
  The program should return to the bash shell.
- To run Augterm: cd to the ReEngage directory
- cd augterm-0.21/build/augterm-0.21
- java -jar augterm.jar
- From the Augterm menu, select Connect
- Enter a Hostname or IP address, for example: mare.hoardersheaven.net
- Enter the port number, for example: 4201
- Click Enter to connect to the muse.
- You can now test typing characters on the chord keyboard when answering prompts.
  Note that characters you typed will not be echoed back to the terminal.


  
