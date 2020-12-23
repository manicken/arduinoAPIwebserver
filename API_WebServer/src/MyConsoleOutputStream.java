package com.manicken;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.Timer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.BadLocationException;
import javax.swing.SwingUtilities;

import processing.app.EditorConsole;

public class MyConsoleOutputStream extends ByteArrayOutputStream {

	private SimpleAttributeSet attributes;
	private final PrintStream printStream;
	private final Timer timer;
  
	private volatile EditorConsole editorConsole;
	private volatile boolean newLinePrinted;

	private static MyWebSocketServer mwss;
	Color fgColor;
	String fgColorHex;
	Color bgColor;
	String bgColorHex;

	private static MyConsoleOutputStream out;
	private static MyConsoleOutputStream err;

	public static void DisconnectWebsocketServer()
	{
		try {
			if (mwss != null) mwss.stop();
			System.out.println("terminal capture websocket server was stopped!");
		} catch (Exception e) { System.err.println("cannot stop prev websocket server!!!"); e.printStackTrace();}
	}
	public static void setCurrentEditorConsole(EditorConsole editorConsole, SimpleAttributeSet console_stdOutStyle, SimpleAttributeSet console_stdErrStyle, int webSocketServerPort) {
		try {
			if (mwss != null) mwss.stop();
		} catch (Exception e) { System.err.println("cannot stop prev websocket server!!!"); e.printStackTrace();}
		try {

			mwss = new MyWebSocketServer(webSocketServerPort);
			mwss.start();
		} catch (Exception e) { System.err.println("cannot start redirect websocket server!!!"); e.printStackTrace(); mwss = null; return; }


		
		if (out == null) {
		  out = new MyConsoleOutputStream(console_stdOutStyle, System.out, mwss);
		  System.setOut(new PrintStream(out, true));
	
		  err = new MyConsoleOutputStream(console_stdErrStyle, System.err, mwss);
		  System.setErr(new PrintStream(err, true));
		}
	
		out.setCurrentEditorConsole(editorConsole);
		err.setCurrentEditorConsole(editorConsole);
	  }
  
	public MyConsoleOutputStream(SimpleAttributeSet attributes, PrintStream printStream, MyWebSocketServer mwss) {
	  this.mwss = mwss;
	  this.attributes = attributes;
	  this.printStream = printStream;
	  this.newLinePrinted = false;
	
	  fgColor = StyleConstants.getForeground(attributes);
	  fgColorHex = "#" + Integer.toHexString(fgColor.getRGB() | 0xFF000000).substring(2);
	  bgColor = StyleConstants.getBackground(attributes);
	  bgColorHex = "#" + Integer.toHexString(bgColor.getRGB() | 0xFF000000).substring(2);

	  this.timer = new Timer(100, (e) -> {
		if (editorConsole != null && newLinePrinted) {
		  editorConsole.scrollDown();
		  newLinePrinted = false;
		}
	  });
	  timer.setRepeats(false);
	}
  
	public void setAttibutes(SimpleAttributeSet attributes) {
	  this.attributes = attributes;
	}
  
	public void setCurrentEditorConsole(EditorConsole console) {
	  this.editorConsole = console;
	}
  
	public synchronized void flush() {
	  String text = toString();
  
	  if (text.length() == 0) {
		return;
	  }
  
	  printStream.print(text);
	  printInConsole(text);
  
	  reset();
	}
  
	private void printInConsole(String text) {
	  newLinePrinted = newLinePrinted || text.contains("\n");
	  if (editorConsole != null) {
		SwingUtilities.invokeLater(() -> {
		  //try { editorConsole.insertString(text, attributes); } 
		  //catch (BadLocationException ble) { /*ignore*/ }

		  try { mwss.broadcast("<span style=\"color:"+fgColorHex+";background-color:"+bgColorHex+";\">" + text.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "</span>"); }
		  catch (Exception ex) { /*ignore*/ }

		});
  
		if (!timer.isRunning()) {
		  timer.restart();
		}
	  }
	}
  }