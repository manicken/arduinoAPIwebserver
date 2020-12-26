package com.manicken;

import processing.app.tools.Tool;
import javax.swing.JMenu;

public class ToolJMenu extends javax.swing.JMenu
{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public Tool tool = null;

	public ToolJMenu(String title, Tool tool)
	{
		super(title);
		this.tool = tool;
	}
}