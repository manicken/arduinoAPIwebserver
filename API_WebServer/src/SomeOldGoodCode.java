/*public void GetPrevInstances(API_WebServer thisAPI_WebServer)
	{
		List<Editor> editors = base.getEditors();

		for (int ei = 0; ei < editors.size(); ei++)
		{
			Editor _editor = editors.get(ei);
			if (this.editor == _editor)
			{
				//System.err.println("skipping Editor (because is same): " + editors.get(i).getSketch().getName());
				continue;
			}
			for (int ti = 0; ti < _editor.tools.size(); ti++)
			{
				
				try {
					ToolExt tool = (ToolExt)_editor.tools.get(ti);
					tool.unload();
					System.out.println("tool unload:" + tool.getEditor().getSketch().getName());
					//API_WebServer apiws = (API_WebServer)tool;
					//System.out.println("other extension: " + apiws.thisToolMenuTitle);
				}catch (Exception e) {}
			}

			try {
				Tool tool = internalToolCache.get(thisAPI_WebServer.getClass().getName());
				API_WebServer apiws = (API_WebServer)tool;
				apiws.DisconnectServers();
				System.out.println("other extension: " + apiws.thisToolMenuTitle);

			}catch (Exception e) {e.printStackTrace();}

			
			JMenuBar menubar = editors.get(i).getJMenuBar();
			int existingExtensionsMenuIndex = CustomMenu.GetMenuBarItemIndex(menubar, tr("Extensions"));
			if (existingExtensionsMenuIndex == -1) continue;
			JMenu extensionsMenu = (JMenu)menubar.getSubElements()[existingExtensionsMenuIndex];

			int itemCount = extensionsMenu.getItemCount();

			for (int ei = 0; ei < itemCount; ei++)
			{
				JMenuItem jmenuitem = extensionsMenu.getItem(ei);
				
				if (!jmenuitem.getClass().getName().equals(ToolJMenu.class.getName()))
				{
					//System.err.println("skipping extension (because it don't use ToolJMenu): " + jmenuitem.getText());
					continue;
				}
				
				ToolJMenu item;
				try {
					JMenu jMenu = (javax.swing.JMenu)jmenuitem;
					item = ToolJMenu.class.cast(jMenu);
				}catch (Exception e) {System.err.println("cannot cast " + jmenuitem.getText() + " because JAVA is absolutely RETARDED stupid"); e.printStackTrace(); continue; }
				
				try {
					System.err.println("extension type: " + item.tool.getClass().getName());

					API_WebServer apiws = (API_WebServer)item.tool;

					System.out.println("copy instances from " + thisAPI_WebServer.editor.getSketch().getName() + " to " + apiws.editor.getSketch().getName());

					apiws.CopyInstances(thisAPI_WebServer);
					System.err.println("apiws: " + apiws.thisToolMenuTitle + " " + apiws.instanceIndex);
					//apiws.DisconnectServers();
					

				}catch (Exception e) {System.err.println("cannot cast because java is retarded: " + jmenuitem.getText()); e.printStackTrace(); }
			}
		}
	}*/