
package com.manicken;

import processing.app.Base;
import processing.app.BaseNoGui;
import processing.app.Editor;
import processing.app.tools.Tool;
import processing.app.Sketch;
import processing.app.PreferencesData;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JMenuBar;
import javax.swing.MenuElement;

import static processing.app.I18n.tr; // translate (multi language support)

import com.manicken.Reflect;

public class JMenuExt extends JMenu {
    public Tool tool;

    public JMenuExt(String title, Tool tool) {
        super(title);
        this.tool = tool;
    }

}