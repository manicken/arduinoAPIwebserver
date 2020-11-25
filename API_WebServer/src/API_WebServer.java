/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2008 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.API_WebServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.*;
import java.net.InetSocketAddress;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

//import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.BaseNoGui;
import processing.app.Editor;
import processing.app.tools.Tool;
import processing.app.Sketch;
import processing.app.EditorTab;
import processing.app.syntax.SketchTextArea;
import processing.app.SketchFile;
import processing.app.EditorHeader;
import processing.app.EditorConsole;
import processing.app.syntax.PdeKeywords;

import java.util.Scanner;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static processing.app.I18n.tr;

import javax.swing.JOptionPane;
import javax.lang.model.util.ElementScanner6;
import javax.swing.*;
import javax.swing.text.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;

import java.nio.file.Path;

import org.json.*;

import java.awt.Desktop;
import java.net.URI;



import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.ToolTipSupplier;
import java.util.Arrays;

class ConfigDialog extends JPanel
{
	private JLabel lblServerport;
	public JCheckBox chkAutostart;
	public JCheckBox chkDebugMode;	
    public JTextField txtServerport;

    public ConfigDialog() {
        //construct components
		lblServerport = new JLabel ("Server Port");
		chkAutostart = new JCheckBox ("Autostart Server at Arduino IDE start");
		chkDebugMode = new JCheckBox ("Activates some debug output");
        txtServerport = new JTextField (5);

        //adjust size and set layout
        setPreferredSize (new Dimension (263, 129));
        setLayout (null);

        //add components
		add (lblServerport);
        add (chkAutostart);
		add (txtServerport);
		add (chkDebugMode);

        //set component bounds (only needed by Absolute Positioning)
        lblServerport.setBounds (5, 5, 100, 25);
        txtServerport.setBounds (85, 5, 100, 25);
		chkAutostart.setBounds (4, 30, 232, 30);
		chkDebugMode.setBounds (4, 65, 232, 30);
    }

}

/**
 * Example Tools menu entry.
 */
public class API_WebServer implements Tool {
	boolean debugPrint = false;
	Editor editor;
	
	Sketch sketch; // for the API
	ArrayList<EditorTab> tabs; // for the API uses reflection to get
	EditorHeader header; // for the API uses reflection to get
	Runnable runHandler; // for the API uses reflection to get
	Runnable presentHandler; // for the API uses reflection to get

	Base base;
	
	PdeKeywords pdeKeywords;
	Map<String, String> keywordOldToken; // only need this
	String sketchKeywordsFileName = "keywords.txt";
	String sketchKeywordsTempFileName = "keywords_temp.txt"; // updated by external editor
	
	JMenu toolsMenu;
	
	HttpServer server;
	
	int DefaultServerPort = 8080;
	boolean DefaultAutoStart = true;
	String thisToolMenuTitle = "API Web Server";
	String rootDir;
	
	int serverPort = 8080; // replaced by code down
	boolean autostart = true; // replaced by code down
	
	boolean started = false;
	public myWebSocketServer cs;

	EditorConsole editorConsole;
	private ConsoleOutputStream2 out;
	private ConsoleOutputStream2 err;
	private SimpleAttributeSet console_stdOutStyle;
	private SimpleAttributeSet console_stdErrStyle;
	String outFgColorHex;
	String outBgColorHex;
	String errFgColorHex;
	String errBgColorHex;

	public synchronized void setCurrentEditorConsole() {
		if (out == null) {
		  out = new ConsoleOutputStream2(console_stdOutStyle, System.out, cs);
		  System.setOut(new PrintStream(out, true));
	
		  err = new ConsoleOutputStream2(console_stdErrStyle, System.err, cs);
		  System.setErr(new PrintStream(err, true));
		}
	
		out.setCurrentEditorConsole(editorConsole);
		err.setCurrentEditorConsole(editorConsole);
	  }

	public void init(Editor editor) { // required by tool loader
		this.editor = editor;

		editor.addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
			  init();
			}
		});
		
	}
	private void SystemOutHookStart()
	{
		Color fgColor = StyleConstants.getForeground(console_stdOutStyle);
		outFgColorHex = "#" + Integer.toHexString(fgColor.getRGB() | 0xFF000000).substring(2);
		Color bgColor = StyleConstants.getBackground(console_stdOutStyle);
		outBgColorHex = "#" + Integer.toHexString(bgColor.getRGB() | 0xFF000000).substring(2);

		fgColor = StyleConstants.getForeground(console_stdErrStyle);
		errFgColorHex = "#" + Integer.toHexString(fgColor.getRGB() | 0xFF000000).substring(2);
		bgColor = StyleConstants.getBackground(console_stdErrStyle);
		errBgColorHex = "#" + Integer.toHexString(bgColor.getRGB() | 0xFF000000).substring(2);

		PrintStream psOut = new PrintStream(System.out, true) {
			@Override
			public void println(String x) {
				cs_SendWithStyle(outFgColorHex, outBgColorHex, x + "<br>");
				super.println(x);
			}
			/*public void print(String x) {
				cs_SendWithStyle(outFgColorHex, outBgColorHex, "print:" + x);
				super.print(x);
			}*/
		};
		System.setOut(psOut);

		PrintStream psErr = new PrintStream(System.out, true) {
			@Override
			public void println(String x) {
				cs_SendWithStyle(errFgColorHex, errBgColorHex, x + "<br>");
				super.println(x);
			}
			/*public void print(String x) {
				cs_SendWithStyle(errFgColorHex, errBgColorHex, x);
				super.print(x);
			}*/
		};
		System.setErr(psErr);
	}
	private void cs_SendWithStyle(String fgColorHex, String bgColorHex, String text)
	{
		if (cs != null)
		{
			try { cs.broadcast("<span style=\"color:"+fgColorHex+";background-color:"+bgColorHex+";\">" + text.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "</span>"); }
			catch (Exception ex) { /*ignore*/ }
		}
	}
	public void run() {// required by tool loader
		LoadSettings();
		startWebServer();
		
		startWebsocketServer();		
		//System.out.println("Hello World!");
	}
	public void startWebsocketServer()
	{
		try {
			cs = new myWebSocketServer(3000);
			cs.start();
			} catch (Exception e)
			{
				System.err.println("cannot start websocket server!!!");
				e.printStackTrace();
			}
	}
	public String getMenuTitle() {// required by tool loader
		return thisToolMenuTitle;
	}
	
	private void init()
	{
		if (started)
		{
			System.out.println("Server is allready running at port " + serverPort);
			return;
		}
		System.out.println("init API_WebServer");
		rootDir = GetArduinoRootDir();
		System.out.println("rootDir="+rootDir);
		try{
			Field f ;
			//Field f = Editor.class.getDeclaredField("sketch");
			//f.setAccessible(true);
			//sketch = (Sketch) f.get(this.editor);
			sketch = this.editor.getSketch();
			
			f = Editor.class.getDeclaredField("base");
			f.setAccessible(true);
			base = (Base) f.get(this.editor);
			
			pdeKeywords = base.getPdeKeywords(); // no need to use reflection here
			
			f = PdeKeywords.class.getDeclaredField("keywordOldToken");
			f.setAccessible(true);
			keywordOldToken = (Map<String, String>) f.get(pdeKeywords);
			
			f = Editor.class.getDeclaredField("console");
			f.setAccessible(true);
			editorConsole = (EditorConsole) f.get(this.editor);

			f = EditorConsole.class.getDeclaredField("stdOutStyle");
			f.setAccessible(true);
			console_stdOutStyle = (SimpleAttributeSet) f.get(this.editorConsole);

			f = EditorConsole.class.getDeclaredField("stdErrStyle");
			f.setAccessible(true);
			console_stdErrStyle = (SimpleAttributeSet) f.get(this.editorConsole);

			f = Editor.class.getDeclaredField("tabs");
			f.setAccessible(true);
			tabs = (ArrayList<EditorTab>) f.get(this.editor);
			
			f = Editor.class.getDeclaredField("header");
			f.setAccessible(true);
			header = (EditorHeader) f.get(this.editor);
			
			f = Editor.class.getDeclaredField("runHandler");
			f.setAccessible(true);
			runHandler = (Runnable) f.get(this.editor);
			
			f = Editor.class.getDeclaredField("presentHandler");
			f.setAccessible(true);
			presentHandler = (Runnable) f.get(this.editor);
			
			f = Editor.class.getDeclaredField("toolsMenu");
			f.setAccessible(true);
			toolsMenu = (JMenu) f.get(this.editor);
			
			int thisToolIndex = GetMenuItemIndex(toolsMenu, thisToolMenuTitle);
			JMenu thisToolMenu = new JMenu(thisToolMenuTitle);		
			toolsMenu.insert(thisToolMenu, thisToolIndex+1);
			toolsMenu.remove(thisToolIndex);
			
			JMenuItem newItem = new JMenuItem("Start/Restart Server");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> run());
			
			newItem = new JMenuItem("Settings");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> ShowConfigDialog());

			newItem = new JMenuItem("Start GUI Tool");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> StartGUItool());

			newItem = new JMenuItem("Init autocomplete");
			thisToolMenu.add(newItem);
			newItem.addActionListener(event -> ActivateAutoCompleteFunctionality());

			started = true;
			
			loadSketchKeywordsFile();
			loadSketchKeywordsTempFile(); // used by the external tool/editor

			//keywordOldToken.put("Jannik", "KEYWORD2");
			//keywordOldToken.put("Svensson", "LITERAL1");
			//keywordOldToken.put("Jannik", "LITERAL1");
			//pdeKeywords_fillMissingTokenType(); // only needed after new keywords is added "manually"
			editor.updateKeywords(pdeKeywords); // this applys the changes
			
		}catch (Exception e)
		{
			sketch = null;
			tabs = null;
			System.err.println("cannot reflect:");
			e.printStackTrace();
			System.err.println("API_WebServer not started!!!");
			return;
		}
		LoadSettings();
		if (autostart)
		{
			startWebServer();
			startWebsocketServer();
			SystemOutHookStart();
			//setCurrentEditorConsole();
		}
		//ActivateAutoCompleteFunctionality();
	}
	public void ActivateAutoCompleteFunctionality()
	{
		for (int i = 0; i < tabs.size(); i++)
		{
			SketchTextArea textArea = tabs.get(i).getTextArea();
			AutoCompleteProvider acp = new AutoCompleteProvider(textArea, GetJarFileDir());
		}
	}
	public void pdeKeywords_fillMissingTokenType()
	{
		try {
			Method m = PdeKeywords.class.getDeclaredMethod("fillMissingTokenType");
			m.setAccessible(true);
			m.invoke(pdeKeywords);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_addTab");
			e.printStackTrace();
		}
	}
	public void pdeKeywords_parseKeywordsTxt(File file)
	{
		try {
			Method m = PdeKeywords.class.getDeclaredMethod("parseKeywordsTxt", File.class);
			m.setAccessible(true);
			m.invoke(pdeKeywords, file);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_addTab");
			e.printStackTrace();
		}
	}
	public void loadSketchKeywordsFile()
	{
		File file = new File(sketch.getFolder(), sketchKeywordsFileName);
		if (!file.exists()) return;
		pdeKeywords_parseKeywordsTxt(file);
	}
	public void loadSketchKeywordsTempFile()
	{
		File file = new File(sketch.getFolder(), sketchKeywordsTempFileName);
		if (!file.exists()) return;
		pdeKeywords_parseKeywordsTxt(file);
	}
	public void StartGUItool()
	{
		try {
			File htmlFile = new File(rootDir + "/hardware/teensy/avr/libraries/Audio/gui/index.html");
			Desktop.getDesktop().browse(htmlFile.toURI());
			System.out.println("Web page opened in browser");
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}
	public void ShowConfigDialog()
	{
		ConfigDialog cd = new ConfigDialog();
		//cd.setPreferredSize(new Dimension(100, 100)); // set in ConfigDialog code
		cd.txtServerport.setText(Integer.toString(serverPort));
		cd.chkAutostart.setSelected(autostart);
		
	   int result = JOptionPane.showConfirmDialog(editor, cd, "API Web Server Config" ,JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			
		if (result == JOptionPane.OK_OPTION) {
			serverPort = Integer.parseInt(cd.txtServerport.getText());
			autostart = cd.chkAutostart.isSelected();
			debugPrint = cd.chkDebugMode.isSelected();
			System.out.println(serverPort + " " + autostart);
			SaveSettings();
		} else {
			System.out.println("Cancelled");
		}
	}
	public int GetMenuItemIndex(JMenu menu, String name)
	{
		//System.out.println("try get menu: " + name);
		for ( int i = 0; i < menu.getItemCount(); i++)
		{
			//System.out.println("try get menu item @ " + i);
			JMenuItem item = menu.getItem(i);
			if (item == null) continue; // happens on seperators
			if (item.getText() == name)
				return i;
		}
		return -1;
	}
	public String GetArduinoRootDir()
	{
	  try{
	    File file = new File(API_WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	    return file.getParentFile().getParentFile().getParentFile().getParent();
	    }catch (Exception e) {
	    e.printStackTrace();
	      return "";
	    }
	}
	public String GetJarFileDir()
	{
	  try{
	    File file = new File(API_WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	    return file.getParent();
	    }catch (Exception e) {
	    e.printStackTrace();
	      return "";
	    }
	}
	
	public void LoadDefaultSettings()
	{
		serverPort = DefaultServerPort;
		autostart = DefaultAutoStart;
		System.out.println("Default Settings Used, serverPort=" + serverPort + ", autostart=" + autostart);
	}
	
	public File GetSettingsFile()
	{
		//File file = new File("tools/API_WebServer/tool/settings.json"); // works on windows
		//if (file.exists()) return file;
		File file = new File(GetJarFileDir() + "/settings.json"); // works on linux and windows
		if (file.exists()) return file;
		System.out.println("setting file not found!");
		return null;
	}
	
	public void LoadSettings()
	{
		File file = GetSettingsFile();
		if (file == null) { LoadDefaultSettings(); return;}
		
		String content = "";
		try { content = new Scanner(file).useDelimiter("\\Z").next(); } 
		catch (Exception e) {e.printStackTrace(); LoadDefaultSettings(); return; }
		JSONObject jsonObj = new JSONObject(content);
			
		try {serverPort = jsonObj.getInt("serverPort");} 
		catch (Exception e) { e.printStackTrace(); serverPort = DefaultServerPort; System.out.println("Default used for serverPort=" + serverPort);}
		
		try {autostart = jsonObj.getBoolean("autostart");}
		catch (Exception e) { e.printStackTrace(); autostart = DefaultAutoStart; System.out.println("Default used for autostart=" + autostart);}
	}
	
	public void SaveSettings()
	{
		try {
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = new FileWriter(GetJarFileDir() + "/settings.json");
            StringWriter stringWriter = new StringWriter();
			JSONWriter writer = new JSONWriter(stringWriter);
			writer.object().key("serverPort").value(serverPort).key("autostart").value(autostart).endObject();

			System.out.println(stringWriter.getBuffer().toString());
			file.write(stringWriter.getBuffer().toString());

			file.close();
        } catch (IOException e) {
            e.printStackTrace();
 
        }
	}
	
	private void startWebServer()
	{
		if (server != null)
			try { server.stop(1); } catch (Exception e) {System.err.println(e + " @ " + e.getStackTrace() + e.getStackTrace()[0].getLineNumber());}
	  try {
		server = HttpServer.create(new InetSocketAddress("localhost", serverPort), 0);
		server.createContext("/", new  MyHttpHandler(editor, this));
		server.setExecutor(null);
		server.start();

		System.out.println(" Server started on port " + serverPort);
	  } catch (Exception e) {
		e.printStackTrace();
	  }
	}
	
	public String getFile(String name)
	{
		File file = new File(sketch.getFolder(), name);
		boolean exists = file.exists();
		if (exists)
		{
			
			try {
				String content = new Scanner(file).useDelimiter("\\Z").next();
				return content;
			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		}
		else
		{
			System.out.println(name + " file not found!");
			return "";
		}
	}
	public void setFile(String name, String contents)
	{
		try {
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = new FileWriter(sketch.getFolder() + "/" + name);
			file.write(contents);
			file.close();
        } catch (IOException e) {
            e.printStackTrace();
 
        }
	}
	
	public void RemoveFilesNotInJSON(JSONArray arr)
	{
		System.out.println("RemoveFilesNotInJSON");
		ArrayList<String> filesToRemove = new ArrayList<String>();
		
		// this removes files in the sketch that is not present in the 
		// JSONArray. To not interfere with the current sketch.getCodeCount()
		// it stores filenames to be removed in a temporary Array
		for (int i = 0; i < sketch.getCodeCount(); i++)
		{
			SketchFile sf = sketch.getFile(i);
			if (sf.isPrimary()) continue; // never remove primary sketch ino file
			
			String fileName = sf.getFileName();
			if (!CheckIfFileExistsInJsonArray(fileName, arr))
				filesToRemove.add(fileName); // store it for later
		}
		// now it can remove files 
		for (int i = 0; i < filesToRemove.size(); i++)
		{
			String fileName = filesToRemove.get(i);
			System.out.println("Removing file:" + fileName);
			removeFile(fileName);
		}
	}
	private boolean CheckIfFileExistsInJsonArray(String fileName, JSONArray arr)
	{
		//System.out.println("CheckIfFileExistsInJsonArray:" + fileName);
		for (int i = 0; i < arr.length(); i++)
		{
			JSONObject e = arr.getJSONObject(i);
			String name = e.getString("name");
			//System.out.println("against: " + name);
			if (name.equals(fileName))
				return true;
		}
		return false;
	}
	
	public void editor_addTab(SketchFile sketchFile, String contents)
	{
		try {
		Method m = Editor.class.getDeclaredMethod("addTab", SketchFile.class, String.class);
		m.setAccessible(true);
		m.invoke(editor, sketchFile, contents);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_addTab");
			e.printStackTrace();
		}
	}
	public void sketch_removeFile(SketchFile sketchFile)
	{
		try {
		Method m = Sketch.class.getDeclaredMethod("removeFile", SketchFile.class);
		m.setAccessible(true);
		m.invoke(sketch, sketchFile);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke sketch_removeFile");
			e.printStackTrace();
		}
	}
	public void editor_removeTab(SketchFile sketchFile)
	{
		try {
		Method m = Editor.class.getDeclaredMethod("removeTab", SketchFile.class);
		m.setAccessible(true);
		m.invoke(editor, sketchFile);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke editor_removeTab");
			e.printStackTrace();
		}
	}
	public boolean sketchFile_delete(SketchFile sketchFile)
	{
		try {
		Method m = SketchFile.class.getDeclaredMethod("delete", Path.class);
		m.setAccessible(true);
		return (boolean)m.invoke(sketchFile, sketch.getBuildPath().toPath());
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke sketchFile_delete");
			e.printStackTrace();
			return false;
		}
	}
	public boolean sketchFile_fileExists(SketchFile sketchFile)
	{
		try {
		Method m = SketchFile.class.getDeclaredMethod("fileExists");
		m.setAccessible(true);
		return (boolean)m.invoke(sketchFile);
		}
		catch (Exception e)
		{
			System.err.println("cannot invoke sketchFile_fileExists");
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean addNewFile(String fileName, String contents) // for the API
	{
		File folder;
		try 
		{
			folder = sketch.getFolder();
		}
		catch (Exception e)
		{
			System.err.println(e);
			return false;
		}
		//System.out.println("folder: " + folder.toString());
		File newFile = new File(folder, fileName);
		int fileIndex = sketch.findFileIndex(newFile);
		if (fileIndex >= 0) { // file allready exist, just change the contents.
		  tabs.get(fileIndex).setText(contents);
		  System.out.println("file allready exists " + fileName);
		  return true;
		}
		SketchFile sketchFile;
		try {
		  sketchFile = sketch.addFile(fileName);
		} catch (IOException e) {
		  // This does not pass on e, to prevent showing a backtrace for
		  // "normal" errors.
		  e.printStackTrace();
		  
		  return false;
		}
		editor_addTab(sketchFile, contents);
		System.out.println("added new file " + fileName);
		editor.selectTab(editor.findTabIndex(sketchFile));
		
		return true;
	}
	public boolean removeFile(String fileName) // for the API, so that files could be removed
	{
		File newFile = new File(sketch.getFolder(), fileName);
		int fileIndex = sketch.findFileIndex(newFile);
		if (fileIndex >= 0) { // file exist
		    SketchFile sketchFile = sketch.getFile(fileIndex);
			boolean neverSavedTab = !sketchFile_fileExists(sketchFile);
			
			if (!sketchFile_delete(sketchFile) && !neverSavedTab) {
				System.err.println("Couldn't remove the file " + fileName);
				return false;
			}
			if (neverSavedTab) {
				// remove the file from the sketch list
				sketch_removeFile(sketchFile);
			}
			editor_removeTab(sketchFile);

			// just set current tab to the main tab
			editor.selectTab(0);

			// update the tabs
			header.repaint();
			return true;
		}
		System.err.println("file don't exists in sketch " + fileName);
		return false;
	}
	public boolean renameFile(String oldFileName, String newFileName) // for the API, so that it can rename files
	{
		File newFile = new File(sketch.getFolder(), oldFileName);
		int fileIndex = sketch.findFileIndex(newFile);
		if (fileIndex >= 0) { // file exist
		  SketchFile sketchFile = sketch.getFile(fileIndex);
		  try {
			sketchFile.renameTo(newFileName);
			// update the tabs
			header.rebuild();
			return true;
		  } catch (IOException e) {
			e.printStackTrace();
		  }
		}
		return false;
	}
	
	public void verifyCompile() 
	{
		editor.setAlwaysOnTop(false);
		editor.setAlwaysOnTop(true);
		editor.setAlwaysOnTop(false);
		editor.handleRun(false, presentHandler, runHandler);
	}
	public void upload()
	{
		editor.setAlwaysOnTop(false);
		editor.setAlwaysOnTop(true);
		editor.setAlwaysOnTop(false);
		editor.handleExport(false);
	}
	public String parseGET(Map<String, String> query)
	{
		String cmd = query.get("cmd");
		//if (!cmd.equals("ping"))
		//	System.out.println("GET request params: " + cmd);
		if (cmd.equals("ping"))
		{
			// do nothing, a OK is default to send back
		}
		else if (cmd.equals("compile"))
		{
			verifyCompile();
			System.out.println("WSAPI compile");
		}
		else if (cmd.equals("upload"))
		{   
			upload();
			System.out.println("WSAPI upload");
		}
		else if (cmd.equals("renameFile"))
		{
			String from = query.get("from");
			if (from == null) { System.out.println("Missing 'from' parameter @ renameFile"); return "Missing 'from' parameter @ renameFile"; }
			String to = query.get("to");
			if (to == null) { System.out.println("Missing 'to' parameter @ renameFile"); return "Missing 'to' parameter @ renameFile"; }
			System.out.println("WSAPI renameFile from:" + from + ", to:" + to);
			renameFile(from, to);
		}
		else if (cmd.equals("removeFile"))
		{
			String name = query.get("fileName");
			if (name == null) { System.out.println("Missing 'fileName' parameter @ removeFile"); return "Missing 'fileName' parameter @ removeFile"; }
			System.out.println("WSAPI removeFile:" + name);
			removeFile(name);
		}
		else if(cmd.equals("getFile"))
		{
			String name = query.get("fileName");
			if (name == null) { System.out.println("Missing 'fileName' parameter @ getFile"); return "Missing 'fileName' parameter @ getFile"; }
			System.out.println("WSAPI getFile:" + name);
			return getFile(name);
		}
		else
			return "unknown GET cmd: " + cmd;

		return "OK"; // default
	}
	public synchronized String parsePOST(String data)
	{
		
		String returnStr = "";
		JSONObject jsonObj = new JSONObject(data);
		Boolean removeOtherFiles = false;
		JSONArray arrFiles = null;
		JSONArray arrKeywords = null;
		
		try { removeOtherFiles = jsonObj.getBoolean("removeOtherFiles"); } catch (Exception e) { returnStr = " >>>warning: removeOtherFiles missing in JSON<<< ";if (debugPrint)e.printStackTrace(); };
		try { arrFiles = jsonObj.getJSONArray("files"); } catch (Exception e) { returnStr += " >>>error: files array missing in JSON<<< "; if (debugPrint)e.printStackTrace();}
		try { arrKeywords = jsonObj.getJSONArray("keywords"); } catch (Exception e) { returnStr += " >>>warning: keywords array missing in JSON<<< "; if (debugPrint)e.printStackTrace();}
		
		if (removeOtherFiles) {
			try{RemoveFilesNotInJSON(arrFiles);}
			catch (Exception e) {e.printStackTrace();}
		}
		if (arrFiles != null)
		{
			for (int i = 0; i < arrFiles.length(); i++)
			{
				JSONObject file = arrFiles.getJSONObject(i);
				String name = "";
				String contents = "";
				try { name = file.getString("name"); } catch (Exception e) { returnStr += " >>>error: fileObject don't contain name<<< ";if (debugPrint)e.printStackTrace(); continue; }
				try { contents = file.getString("contents"); } catch (Exception e) { returnStr += " >>>error: fileObject don't contain contents<<< ";if (debugPrint)e.printStackTrace(); continue; }
				
				if (name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h") || name.endsWith(".hpp") || name.endsWith(".ino"))
					addNewFile(name, contents); // adds a new file to the sketch-project
				else
					setFile(name, contents); // this writes a file without the IDE knowing it
			}
		}
		if (arrKeywords != null)
		{
			StringBuilder sbKeywords = new StringBuilder();
			
			for (int i = 0; i < arrKeywords.length(); i++)
			{
				JSONObject keyword = arrKeywords.getJSONObject(i);
				String token = "";
				String type = "";
				try { token = keyword.getString("token"); } catch (Exception e) { returnStr += " >>>error: keyword don't contain token<<< ";if (debugPrint)e.printStackTrace(); continue; }
				try { type = keyword.getString("type"); } catch (Exception e) { returnStr += " >>>error: keyword don't contain type<<< ";if (debugPrint)e.printStackTrace(); continue; }
				
				sbKeywords.append(token+"\t"+type+"\r\n");
				keywordOldToken.put(token, type);
				
			}
			
			pdeKeywords_fillMissingTokenType();
			editor.updateKeywords(pdeKeywords);
			String sbKeywordsContents = sbKeywords.toString();
			if (debugPrint)	System.out.println("setting new keywords:\n" + sbKeywordsContents);

			if (!sbKeywordsContents.equals(""))
				setFile(sketchKeywordsTempFileName, sbKeywordsContents);
		}
		
		if (debugPrint)	System.out.print(data + "\n");
		editor.handleSave(true);
		if (returnStr.equals("")) returnStr = "OK";
		System.out.println(returnStr);
		return returnStr;
	}
}
class MyHttpHandler implements HttpHandler
{    
	Editor editor;
	API_WebServer api;

	public MyHttpHandler(Editor _editor, API_WebServer _api)
	{
		this.editor = _editor;
		this.api = _api;
	}
	
	@Override    
	public void handle(HttpExchange httpExchange) throws IOException {
		httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		
		String reqMethod = httpExchange.getRequestMethod();
		String htmlResponse = "";
		String requestParamValue=null; 
		
		if(reqMethod.equals("GET"))
		{
			htmlResponse = api.parseGET(queryToMap(httpExchange.getRequestURI().getQuery()));
		}
		else if(reqMethod.equals("POST"))
		{ 
			requestParamValue = handlePostRequest(httpExchange);
			if (requestParamValue.length() == 0)
			{
				System.out.println("HTTP POST don't contain any data!");
				htmlResponse = "";
			}
			else
			{
				htmlResponse = api.parsePOST(requestParamValue);
			}
		}
		else
		{
			System.out.println("unknown reqMethod:" + reqMethod);
			htmlResponse = "unknown reqMethod:" + reqMethod;
		}
		//System.out.println(requestParamValue); // debug
		handleResponse(httpExchange, htmlResponse); 
	}

	public Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<>();
		for (String param : query.split("&")) {
			String[] entry = param.split("=");
			if (entry.length > 1) {
				result.put(entry[0], entry[1]);
			}else{
				result.put(entry[0], "");
			}
		}
		return result;
	}
	
	private String handlePostRequest(HttpExchange httpExchange) {
		if (httpExchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            try{
			httpExchange.sendResponseHeaders(200, 0);
			} catch (Exception e) {
			e.printStackTrace();
			}
			System.out.println("hi");
            return "";
        }
		InputStream input = httpExchange.getRequestBody();
        StringBuilder stringBuilder = new StringBuilder();

        new BufferedReader(new InputStreamReader(input))
                          .lines()
                          .forEach( (String s) -> stringBuilder.append(s + "\n") );

		return stringBuilder.toString();
	}

	private void handleResponse(HttpExchange httpExchange, String htmlResponse)  throws  IOException {
		OutputStream outputStream = httpExchange.getResponseBody();

		// this line is a must
		httpExchange.sendResponseHeaders(200, htmlResponse.length());
		// additional data to send back
		outputStream.write(htmlResponse.getBytes());
		outputStream.flush();
		outputStream.close();
	}
}
class myWebSocketServer extends WebSocketServer {

	public myWebSocketServer(int port) throws UnknownHostException {
	  super(new InetSocketAddress(port));
	}
  
	public myWebSocketServer(InetSocketAddress address) {
	  super(address);
	}
  
	public myWebSocketServer(int port, Draft_6455 draft) {
	  super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
	}
  
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
	  conn.send("Welcome to the WebSocketServer!"); //This method sends a message to the new client
	  broadcast("new connection: " + handshake
		  .getResourceDescriptor()); //This method sends a message to all clients connected
	  System.out.println(
		  conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected!");
	}
  
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
	  broadcast(conn + " disconnected!");
	  System.out.println(conn + " disconnected!");
	}
  
	@Override
	public void onMessage(WebSocket conn, String message) {
	  broadcast(message);
	  System.out.println(conn + ": " + message);
	}
  
	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
	  broadcast(message.array());
	  System.out.println(conn + ": " + message);
	}
  
	@Override
	public void onError(WebSocket conn, Exception ex) {
	  ex.printStackTrace();
	  if (conn != null) {
		// some errors like port binding failed may not be assignable to a specific websocket
	  }
	}
  
	@Override
	public void onStart() {
	  System.out.println("Websocket Server started!");
	  setConnectionLostTimeout(0);
	  setConnectionLostTimeout(100);
	}
  
  }
  class ConsoleOutputStream2 extends ByteArrayOutputStream {

	private SimpleAttributeSet attributes;
	private final PrintStream printStream;
	private final Timer timer;
  
	private volatile EditorConsole editorConsole;
	private volatile boolean newLinePrinted;

	private myWebSocketServer cs;
	Color fgColor;
	String fgColorHex;
	Color bgColor;
	String bgColorHex;
  
	public ConsoleOutputStream2(SimpleAttributeSet attributes, PrintStream printStream, myWebSocketServer cs) {
	  this.cs = cs;
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
		  try { editorConsole.insertString(text, attributes); } 
		  catch (BadLocationException ble) { /*ignore*/ }

		  try { cs.broadcast("<span style=\"color:"+fgColorHex+";background-color:"+bgColorHex+";\">" + text.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "</span>"); }
		  catch (Exception ex) { /*ignore*/ }

		});
  
		if (!timer.isRunning()) {
		  timer.restart();
		}
	  }
	}
  }
  class AutoCompleteProvider
  {
	public AutoCompletion ac;
	String rootFolder = "";
	String completeFile = "";

	public AutoCompleteProvider(RSyntaxTextArea textArea, String rootFolder)
	{
		CompletionProvider provider = createCompletionProvider(); // takes it all
		
		this.rootFolder = rootFolder;
		completeFile = rootFolder + "\\c.xml";
		System.out.println("@AutoCompleteProvider completeFile:" + completeFile);
		// Install auto-completion onto our text area.
		ac = new AutoCompletion(provider);
		ac.setListCellRenderer(new CCellRenderer());
		ac.setShowDescWindow(true);
		ac.setParameterAssistanceEnabled(true);
		ac.install(textArea);

		textArea.setToolTipSupplier((ToolTipSupplier)provider);
		ToolTipManager.sharedInstance().registerComponent(textArea);
		System.out.println("AutoCompleteProvider is now installed");
	}
	  /**
	 * Returns the provider to use when editing code.
	 *
	 * @return The provider.
	 * @see #createCommentCompletionProvider()
	 * @see #createStringCompletionProvider()
	 */
	public CompletionProvider createCodeCompletionProvider() {

		// Add completions for the C standard library.
		DefaultCompletionProvider cp = new DefaultCompletionProvider();

		// First try loading resource (running from demo jar), then try
		// accessing file (debugging in Eclipse).
		ClassLoader cl = getClass().getClassLoader();
		InputStream in = cl.getResourceAsStream(completeFile);
		try {
			if (in!=null) {
				cp.loadFromXML(in);
				in.close();
			}
			else {
				File file = new File(completeFile);
				if (file.exists())
				{
					System.out.println("*******************File exists***************");
					cp.loadFromXML(file);
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		// Add some handy shorthand completions.
		cp.addCompletion(new ShorthandCompletion(cp, "main",
							"int main(int argc, char **argv)"));

		// Add a parameterized completion with a ton of parameters (see #67)
		FunctionCompletion functionCompletionWithLotsOfParameters = new FunctionCompletion(cp, "long", "int");
		functionCompletionWithLotsOfParameters.setParams(Arrays.asList(
			new ParameterizedCompletion.Parameter("int", "intVal"),
			new ParameterizedCompletion.Parameter("float", "floatVal"),
			new ParameterizedCompletion.Parameter("string", "stringVal"),
			new ParameterizedCompletion.Parameter("int", "otherVal1"),
			new ParameterizedCompletion.Parameter("int", "otherVal2"),
			new ParameterizedCompletion.Parameter("int", "otherVal3"),
			new ParameterizedCompletion.Parameter("int", "otherVal4"),
			new ParameterizedCompletion.Parameter("int", "otherVal5"),
			new ParameterizedCompletion.Parameter("int", "otherVal6"),
			new ParameterizedCompletion.Parameter("int", "otherVal7"),
			new ParameterizedCompletion.Parameter("int", "otherVal8"),
			new ParameterizedCompletion.Parameter("int", "otherVal9"),
			new ParameterizedCompletion.Parameter("int", "otherVal9"),
			new ParameterizedCompletion.Parameter("int", "otherVal10"),
			new ParameterizedCompletion.Parameter("int", "otherVal11"),
			new ParameterizedCompletion.Parameter("int", "otherVal12"),
			new ParameterizedCompletion.Parameter("int", "otherVal13"),
			new ParameterizedCompletion.Parameter("int", "otherVal14"),
			new ParameterizedCompletion.Parameter("int", "otherVal15"),
			new ParameterizedCompletion.Parameter("int", "otherVal16"),
			new ParameterizedCompletion.Parameter("int", "otherVal17"),
			new ParameterizedCompletion.Parameter("int", "otherVal18"),
			new ParameterizedCompletion.Parameter("int", "otherVal19"),
			new ParameterizedCompletion.Parameter("int", "otherVal20")
		));
		cp.addCompletion(functionCompletionWithLotsOfParameters);
		return cp;

	}


	/**
	 * Returns the provider to use when in a comment.
	 *
	 * @return The provider.
	 * @see #createCodeCompletionProvider()
	 * @see #createStringCompletionProvider()
	 */
	public CompletionProvider createCommentCompletionProvider() {
		DefaultCompletionProvider cp = new DefaultCompletionProvider();
		cp.addCompletion(new BasicCompletion(cp, "TODO:", "A to-do reminder"));
		cp.addCompletion(new BasicCompletion(cp, "FIXME:", "A bug that needs to be fixed"));
		return cp;
	}

	/**
	 * Returns the completion provider to use when the caret is in a string.
	 *
	 * @return The provider.
	 * @see #createCodeCompletionProvider()
	 * @see #createCommentCompletionProvider()
	 */
	private CompletionProvider createStringCompletionProvider() {
		DefaultCompletionProvider cp = new DefaultCompletionProvider();
		cp.addCompletion(new BasicCompletion(cp, "%c", "char", "Prints a character"));
		cp.addCompletion(new BasicCompletion(cp, "%i", "signed int", "Prints a signed integer"));
		cp.addCompletion(new BasicCompletion(cp, "%f", "float", "Prints a float"));
		cp.addCompletion(new BasicCompletion(cp, "%s", "string", "Prints a string"));
		cp.addCompletion(new BasicCompletion(cp, "%u", "unsigned int", "Prints an unsigned integer"));
		cp.addCompletion(new BasicCompletion(cp, "\\n", "Newline", "Prints a newline"));
		return cp;
	}


	/**
	 * Creates the completion provider for a C editor.  This provider can be
	 * shared among multiple editors.
	 *
	 * @return The provider.
	 */
	public CompletionProvider createCompletionProvider() {

		// Create the provider used when typing code.
		CompletionProvider codeCP = createCodeCompletionProvider();

		// The provider used when typing a string.
		CompletionProvider stringCP = createStringCompletionProvider();

		// The provider used when typing a comment.
		CompletionProvider commentCP = createCommentCompletionProvider();

		// Create the "parent" completion provider.
		LanguageAwareCompletionProvider provider = new
								LanguageAwareCompletionProvider(codeCP);
		provider.setStringCompletionProvider(stringCP);
		provider.setCommentCompletionProvider(commentCP);

		return provider;

	}
  }
  /**
 * The cell renderer used for the C programming language.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class CCellRenderer extends CompletionCellRenderer {

	private Icon variableIcon;
	private Icon functionIcon;


	/**
	 * Constructor.
	 */
	CCellRenderer() {
		variableIcon = getIcon("img/var.png");
		functionIcon = getIcon("img/function.png");
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void prepareForOtherCompletion(JList list,
			Completion c, int index, boolean selected, boolean hasFocus) {
		super.prepareForOtherCompletion(list, c, index, selected, hasFocus);
		setIcon(getEmptyIcon());
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void prepareForVariableCompletion(JList list,
			VariableCompletion vc, int index, boolean selected,
			boolean hasFocus) {
		super.prepareForVariableCompletion(list, vc, index, selected,
										hasFocus);
		setIcon(variableIcon);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void prepareForFunctionCompletion(JList list,
			FunctionCompletion fc, int index, boolean selected,
			boolean hasFocus) {
		super.prepareForFunctionCompletion(list, fc, index, selected,
										hasFocus);
		setIcon(functionIcon);
	}


}