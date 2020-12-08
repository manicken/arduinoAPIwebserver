public class OldSettings {
    /*public void LoadDefaultSettings() {
		webServerPort = DefaultWebServerPort;
		autostart = DefaultAutoStart;
		System.out.println("Default Settings Used, serverPort=" + webServerPort + ", autostart=" + autostart);
	}
	
	public File GetSettingsFile() {
		File file = new File(ideh.GetJarFileDir() + "/settings.json"); // works on linux and windows
		if (file.exists()) return file;
		System.out.println("setting file not found!");
		return null;
	}
	
	public void LoadSettings() {
		File file = GetSettingsFile();
		if (file == null) { LoadDefaultSettings(); return;}
		
		String content = "";
		try { content = new Scanner(file).useDelimiter("\\Z").next(); } 
		catch (Exception e) {e.printStackTrace(); LoadDefaultSettings(); return; }
		JSONObject jsonObj = new JSONObject(content);
			
		try {webServerPort = jsonObj.getInt("serverPort");} 
		catch (Exception e) { e.printStackTrace(); webServerPort = DefaultWebServerPort; System.out.println("Default used for serverPort=" + webServerPort);}
		
		try {autostart = jsonObj.getBoolean("autostart");}
		catch (Exception e) { e.printStackTrace(); autostart = DefaultAutoStart; System.out.println("Default used for autostart=" + autostart);}
	}
	
	public void SaveSettings() {
		try {
            // Constructs a FileWriter given a file name, using the platform's default charset
            FileWriter file = new FileWriter(ideh.GetJarFileDir() + "/settings.json");
            StringWriter stringWriter = new StringWriter();
			JSONWriter writer = new JSONWriter(stringWriter);
			writer.object().key("serverPort").value(webServerPort).key("autostart").value(autostart).endObject();

			System.out.println(stringWriter.getBuffer().toString());
			file.write(stringWriter.getBuffer().toString());

			file.close();
        } catch (IOException e) {  e.printStackTrace(); }
	}*/
}
