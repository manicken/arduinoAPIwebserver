package com.manicken;

import java.io.IOException;
import java.io.File;
import java.io.PrintStream;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JMenu;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import java.awt.Color;

import java.nio.file.Path;

import processing.app.Base;
import processing.app.BaseNoGui;
import processing.app.Editor;
import processing.app.tools.ToolExt;
import processing.app.Sketch;
import processing.app.EditorTab;
import processing.app.syntax.SketchTextArea;
import processing.app.SketchFile;
import processing.app.EditorHeader;
import processing.app.EditorConsole;
import processing.app.syntax.PdeKeywords;

import org.json.*;

//import jdk.tools.jlink.internal.JmodArchive;

//import jdk.javadoc.internal.tool.ToolOption;

import java.util.List;
import java.util.Map;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;

import static processing.app.I18n.tr; // translate (multi language support)

import com.manicken.MyConsoleOutputStream;
import com.manicken.MyWebSocketServer;
import com.manicken.Reflect;
import com.manicken.API_WebServer;
import com.manicken.CustomMenu;

public class IDEhelper {

	public Base base;// for the API uses reflection to get


	public Editor editor;// for the API
	public Sketch sketch; // for the API
	public ArrayList<EditorTab> tabs; // for the API uses reflection to get
	public EditorHeader header; // for the API uses reflection to get
	public Runnable runHandler; // for the API uses reflection to get
	public Runnable presentHandler; // for the API uses reflection to get

	public EditorConsole editorConsole;
	
	public SimpleAttributeSet console_stdOutStyle;
	public SimpleAttributeSet console_stdErrStyle;
	public String outFgColorHex;
	public String outBgColorHex;
	public String errFgColorHex;
	public String errBgColorHex;

	public PdeKeywords pdeKeywords; // for the API uses reflection to get
	public Map<String, String> keywordOldToken; // for the API uses reflection to get
	public String sketchKeywordsFileName = "keywords.txt";
	public String sketchKeywordsTempFileName = "keywords_temp.txt"; // updated by external editor

	public MyWebSocketServer mwss = null;

    public IDEhelper(Editor editor)
    {
		this.editor = editor;

		sketch = this.editor.getSketch();
			
		base = (Base) Reflect.GetField("base", this.editor);
		pdeKeywords = base.getPdeKeywords(); // no need to use reflection here but using reflected base
		keywordOldToken = (Map<String, String>) Reflect.GetField("keywordOldToken", this.pdeKeywords);
		editorConsole = (EditorConsole) Reflect.GetField("console", this.editor);
		console_stdOutStyle = (SimpleAttributeSet) Reflect.GetField("stdOutStyle", this.editorConsole);
		console_stdErrStyle = (SimpleAttributeSet) Reflect.GetField("stdErrStyle", this.editorConsole);
		tabs = (ArrayList<EditorTab>) Reflect.GetField("tabs", this.editor);
		header = (EditorHeader) Reflect.GetField("header", this.editor);
		runHandler = (Runnable) Reflect.GetField("runHandler", this.editor);
		presentHandler = (Runnable) Reflect.GetField("presentHandler", this.editor);

		
	}
	public static void CloseOtherEditors(Editor thisEditor)
	{
		Base _base = (Base) Reflect.GetField("base", thisEditor);
		List<Editor> editors = _base.getEditors();
		boolean anyStopped = false;
		for (int ei = 0; ei < editors.size(); ei++)
		{
			Editor _editor = editors.get(ei);
			if (thisEditor == _editor)
				continue;
			
			_base.handleClose(_editor); // close other
		}
	}

	public static void DoDisconnectOnOtherEditors(Editor thisEditor)
	{
		Base _base = (Base) Reflect.GetField("base", thisEditor);
		List<Editor> editors = _base.getEditors();
		boolean anyStopped = false;
		for (int ei = 0; ei < editors.size(); ei++)
		{
			Editor _editor = editors.get(ei);
			if (thisEditor == _editor)
				continue;
			
			_editor.setVisible(false); // this triggers the componentHidden event 
			_editor.setVisible(true);
			
		}
		// this makes the last editor window topmost
		thisEditor.setVisible(false);
		thisEditor.setVisible(true);
	}

	public String GetArduinoRootDir() {
		try {
			File file = BaseNoGui.getToolsFolder();//new File(API_WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return file.getParentFile().getAbsolutePath();//.getParentFile().getParentFile().getParent();
		} catch (Exception e) { e.printStackTrace(); return ""; }
	}

	public String GetJarFileDir() {
		try {
			File file = new File(API_WebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return file.getParent();
		}catch (Exception e) { e.printStackTrace(); return ""; }
	}

	public void InitCustomKeywords()
	{
		loadSketchKeywordsFile();
		loadSketchKeywordsTempFile(); // used by the external tool/editor

		//keywordOldToken.put("Jannik", "KEYWORD2");
		//keywordOldToken.put("Svensson", "LITERAL1");
		//pdeKeywords_fillMissingTokenType(); // only needed after new keywords is added "manually"
		editor.updateKeywords(pdeKeywords); // this applys the changes
	}

	public void mwss_SendWithStyle(String fgColorHex, String bgColorHex, String text) {
		if (mwss == null) return;
		mwss.SendWithHtmlStyle(fgColorHex, bgColorHex, text);
	}
	
	public void SystemOutHookStart(int webSocketServerPort) {
		try {
			if (mwss != null) mwss.stop();
		} catch (Exception e) { System.err.println("cannot stop prev websocket server!!!"); e.printStackTrace();}
		try {
			mwss = new MyWebSocketServer(webSocketServerPort);
			mwss.start();
		} catch (Exception e) { System.err.println("cannot start redirect websocket server!!!"); e.printStackTrace(); mwss = null; return; }

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
				mwss_SendWithStyle(outFgColorHex, outBgColorHex, x + "<br>");
				super.println(x);
			}
		};
		System.setOut(psOut);

		PrintStream psErr = new PrintStream(System.out, true) {
			@Override
			public void println(String x) {
				mwss_SendWithStyle(errFgColorHex, errBgColorHex, x + "<br>");
				super.println(x);
			}
		};
		System.setErr(psErr);
	}

	public void ActivateAutoCompleteFunctionality() {
		for (int i = 0; i < tabs.size(); i++) {
			SketchTextArea textArea = tabs.get(i).getTextArea();
			AutoCompleteProvider acp = new AutoCompleteProvider(textArea, GetJarFileDir());
		}
	}

	public void pdeKeywords_fillMissingTokenType() {
		try {

			Reflect.InvokeMethod("fillMissingTokenType", pdeKeywords);
			//Method m = PdeKeywords.class.getDeclaredMethod("fillMissingTokenType");
			//m.setAccessible(true);
			//m.invoke(pdeKeywords);
		} catch (Exception e) { System.err.println("cannot invoke editor_addTab"); e.printStackTrace(); }
	}

	public void pdeKeywords_parseKeywordsTxt(File file) {
		try {
			Reflect.InvokeMethod("parseKeywordsTxt", pdeKeywords, file);
			/*Method m = PdeKeywords.class.getDeclaredMethod("parseKeywordsTxt", File.class);
			m.setAccessible(true);
			m.invoke(pdeKeywords, file);*/
		}
		catch (Exception e) { System.err.println("cannot invoke editor_addTab"); e.printStackTrace(); }
	}

	public void loadSketchKeywordsFile() {
		File file = new File(sketch.getFolder(), sketchKeywordsFileName);
		if (!file.exists()) return;
		pdeKeywords_parseKeywordsTxt(file);
	}

	public void loadSketchKeywordsTempFile() {
		File file = new File(sketch.getFolder(), sketchKeywordsTempFileName);
		if (!file.exists()) return;
		pdeKeywords_parseKeywordsTxt(file);
	}

    public String getFile(String name) {
		File file = new File(sketch.getFolder(), name);
		boolean exists = file.exists();
		if (exists) {
			
			try {
				String content = new Scanner(file).useDelimiter("\\Z").next();
				return content;
			} catch (Exception e) { e.printStackTrace(); return ""; }
		}
		else {
			System.out.println(name + " file not found!");
			return "";
		}
	}

	public void setFile(String name, String contents) {
		try {
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = new FileWriter(sketch.getFolder() + "/" + name);
			file.write(contents);
			file.close();
        } catch (IOException e) { e.printStackTrace(); }
	}
	
	public void RemoveFilesNotInJSON(JSONArray arr, boolean autoConvertMainCppToSketchMainIno) {
		try {
			System.out.println("RemoveFilesNotInJSON");
			ArrayList<String> filesToRemove = new ArrayList<String>();
			
			// this removes files in the sketch that is not present in the 
			// JSONArray. To not interfere with the current sketch.getCodeCount()
			// it stores filenames to be removed in a temporary Array
			for (int i = 0; i < sketch.getCodeCount(); i++) {
				SketchFile sf = sketch.getFile(i);
				if (sf.isPrimary()) continue; // never remove primary sketch ino file
				
				String fileName = sf.getFileName();
				if (!CheckIfFileExistsInJsonArray(fileName, arr, autoConvertMainCppToSketchMainIno))
					filesToRemove.add(fileName); // store it for later
			}
			// now it can remove files 
			for (int i = 0; i < filesToRemove.size(); i++) {
				String fileName = filesToRemove.get(i);
				System.out.println("Removing file:" + fileName);
				removeFile(fileName);
			}
		} catch (Exception e) { e.printStackTrace(); }
	}

	public boolean CheckIfFileExistsInJsonArray(String fileName, JSONArray arr, boolean autoConvertMainCppToSketchMainIno) {
		//System.out.println("CheckIfFileExistsInJsonArray:" + fileName);
		for (int i = 0; i < arr.length(); i++) {
			JSONObject e = arr.getJSONObject(i);
			String name = e.getString("name");

			if (autoConvertMainCppToSketchMainIno && name.toLowerCase().equals("main.cpp"))
				name = editor.getSketch().getName() + ".ino";
			//System.out.println("against: " + name);
			if (name.equals(fileName))
				return true;
		}
		return false;
	}

	public void editor_addTab(SketchFile sketchFile, String contents) {
		try { Reflect.InvokeMethod("addTab", editor, sketchFile, contents); }
		catch (Exception e) { /*ReflectInvokeMethod allready prints errors*/ }
	}

	public void sketch_removeFile(SketchFile sketchFile) {
		try { Reflect.InvokeMethod("removeFile", sketch, sketchFile);}
		catch (Exception e) { /*ReflectInvokeMethod allready prints errors*/ }
	}

	public void editor_removeTab(SketchFile sketchFile) {
		try { Reflect.InvokeMethod("removeTab", editor, sketchFile); }
		catch (Exception e) { /*ReflectInvokeMethod allready prints errors*/ }
	}

	public boolean sketchFile_delete(SketchFile sketchFile) {
		try { return (boolean)Reflect.InvokeMethod2("delete", sketchFile, Reflect.asArr(sketch.getBuildPath().toPath()), Reflect.asArr(Path.class)); } // without asArr >> new Object[]{path}, new Class<?>[]{Path.class}); }
		catch (Exception e) { /*ReflectInvokeMethod allready prints errors*/ return false; }
	}

	public boolean sketchFile_fileExists(SketchFile sketchFile) {
		try { return (boolean)Reflect.InvokeMethod("fileExists", sketchFile); }
		catch (Exception e) { /*ReflectInvokeMethod allready prints errors*/ return false; }
	}
	
	public boolean addNewFile(String fileName, String contents) { // for the API
		File folder;
		try  { folder = sketch.getFolder(); }
		catch (Exception e) { System.err.println(e); return false; }

		//System.out.println("folder: " + folder.toString());
		File newFile = new File(folder, fileName);
		int fileIndex = sketch.findFileIndex(newFile);
		if (fileIndex >= 0) { // file allready exist, just change the contents.
		  tabs.get(fileIndex).setText(contents);
		  System.out.println("file allready exists " + fileName);
		  return true;
		}
		SketchFile sketchFile;
		try { sketchFile = sketch.addFile(fileName); }
		catch (IOException e) { e.printStackTrace(); return false; }
		editor_addTab(sketchFile, contents);
		System.out.println("added new file " + fileName);
		editor.selectTab(editor.findTabIndex(sketchFile));
		
		return true;
	}
	public boolean removeFile(String fileName) { // for the API, so that files could be removed
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
	public boolean renameFile(String oldFileName, String newFileName) { // for the API, so that it can rename files
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
	
	public void verifyCompile() {
		editor.setAlwaysOnTop(false);
		editor.setAlwaysOnTop(true);
		editor.setAlwaysOnTop(false);
		editor.handleRun(false, presentHandler, runHandler);
	}

	public void upload() {
		editor.setAlwaysOnTop(false);
		editor.setAlwaysOnTop(true);
		editor.setAlwaysOnTop(false);
		editor.handleExport(false);
	}
}
