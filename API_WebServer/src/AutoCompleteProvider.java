
package com.manicken; // dont care about any errors this give

import java.io.InputStream;
import java.io.IOException;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ToolTipManager;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.ToolTipSupplier;
import java.util.Arrays;

public class AutoCompleteProvider
{
	public AutoCompletion ac;
	String rootFolder = "";

	public AutoCompleteProvider(RSyntaxTextArea textArea, String rootFolder)
	{
		this.rootFolder = rootFolder;

		CompletionProvider provider = createCompletionProvider(); // takes it all
		
		
		
		// Install auto-completion onto our text area.
		ac = new AutoCompletion(provider);
		ac.setListCellRenderer(new CCellRenderer());
		ac.setShowDescWindow(true);
		ac.setParameterAssistanceEnabled(true);
		ac.setAutoActivationDelay(100);
		ac.setAutoActivationEnabled(true);
		ac.setAutoCompleteEnabled(true);
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
		InputStream in = cl.getResourceAsStream(rootFolder + "\\c.xml");
		try {
			if (in!=null) {
				cp.loadFromXML(in);
				in.close();
				//System.out.println("*******************File exists1***************");
			}
			else {
				
				File file = new File(rootFolder, "c.xml");
				//System.out.println("@AutoCompleteProvider completeFile:" + file.getAbsolutePath());
				if (file.exists())
				{
					//System.out.println("*******************File exists2***************");
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
		ParameterizedCompletion.Parameter param = new ParameterizedCompletion.Parameter("int", "intVal ");
		param.setDescription("hello world");
		functionCompletionWithLotsOfParameters.setParams(Arrays.asList(
			param,
			new ParameterizedCompletion.Parameter("float", "floatVal "),
			new ParameterizedCompletion.Parameter("string", "stringVal "),
			new ParameterizedCompletion.Parameter("int", "intVal2 "),
			new ParameterizedCompletion.Parameter("float", "floatVal2 "),
			new ParameterizedCompletion.Parameter("string", "stringVal2 ")
			
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