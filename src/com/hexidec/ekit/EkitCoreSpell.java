/*
GNU Lesser General Public License

EkitCore - Base Java Swing HTML Editor & Viewer Class (Spellcheck Version)
Copyright (C) 2000 Howard Kistler

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.hexidec.ekit;

import java.net.URL;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import com.hexidec.ekit.EkitCore;
import com.hexidec.util.Translatrix;

import com.swabunga.spell.engine.*;
import com.swabunga.spell.event.*;
import com.swabunga.spell.swing.*;

/** EkitCoreSpell
  * Extended main application class with additional spellchecking feature
  *
  * @author Howard Kistler
  * @version 1.5
  *
  * REQUIREMENTS
  * Java 2 (JDK 1.5 or higher)
  * Swing Library
  */

public class EkitCoreSpell extends EkitCore implements SpellCheckListener
{
	/* Spell Checker Settings */
	private static String dictFile;
	private SpellChecker spellCheck = null;
	private JSpellDialog spellDialog;

	/** Master Constructor
	  * @param sDocument         [String]  A text or HTML document to load in the editor upon startup.
	  * @param sStyleSheet       [String]  A CSS stylesheet to load in the editor upon startup.
	  * @param sRawDocument      [String]  A document encoded as a String to load in the editor upon startup.
	  * @param sdocSource        [StyledDocument] Optional document specification, using javax.swing.text.StyledDocument.
	  * @param urlStyleSheet     [URL]     A URL reference to the CSS style sheet.
	  * @param includeToolBar    [boolean] Specifies whether the app should include the toolbar.
	  * @param showViewSource    [boolean] Specifies whether or not to show the View Source window on startup.
	  * @param showMenuIcons     [boolean] Specifies whether or not to show icon pictures in menus.
	  * @param editModeExclusive [boolean] Specifies whether or not to use exclusive edit mode (recommended on).
	  * @param sLanguage         [String]  The language portion of the Internationalization Locale to run Ekit in.
	  * @param sCountry          [String]  The country portion of the Internationalization Locale to run Ekit in.
	  * @param base64            [boolean] Specifies whether the raw document is Base64 encoded or not.
	  * @param debugMode         [boolean] Specifies whether to show the Debug menu or not.
	  * @param hasSpellChecker   [boolean] Specifies whether or not this uses the SpellChecker module
	  * @param multiBar          [boolean] Specifies whether to use multiple toolbars or one big toolbar.
	  * @param enterBreak        [boolean] Specifies whether the ENTER key should insert breaks instead of paragraph tags.
	  */
	public EkitCoreSpell(boolean isParentApplet, String sDocument, String sStyleSheet, String sRawDocument, StyledDocument sdocSource, URL urlStyleSheet, boolean includeToolBar, boolean showViewSource, boolean showMenuIcons, boolean editModeExclusive, String sLanguage, String sCountry, boolean base64, boolean debugMode, boolean useSpellChecker, boolean multiBar, String toolbarSeq, boolean enterBreak)
	{
		super(isParentApplet, sDocument, sStyleSheet, sRawDocument, sdocSource, urlStyleSheet, includeToolBar, showViewSource, showMenuIcons, editModeExclusive, sLanguage, sCountry, base64, debugMode, true, multiBar, toolbarSeq, enterBreak);

		/* Create spell checker */
		try
		{
			dictFile = Translatrix.getTranslationString("DictionaryFile");
			SpellDictionary dictionary = new SpellDictionary(dictFile); // uses my custom loader in SpellDictionary
			spellCheck = new SpellChecker(dictionary);
			spellCheck.addSpellCheckListener(this);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		spellDialog = new JSpellDialog(this.getFrame(), Translatrix.getTranslationString("ToolSpellcheckDialog"), true);
	}

	/** Raw/Base64 Document & Style Sheet URL Constructor (Ideal for EkitApplet)
	  * @param sRawDocument      [String]  A document encoded as a String to load in the editor upon startup.
	  * @param sRawDocument      [String]  A document encoded as a String to load in the editor upon startup.
	  * @param includeToolBar    [boolean] Specifies whether the app should include the toolbar.
	  * @param showViewSource    [boolean] Specifies whether or not to show the View Source window on startup.
	  * @param showMenuIcons     [boolean] Specifies whether or not to show icon pictures in menus.
	  * @param editModeExclusive [boolean] Specifies whether or not to use exclusive edit mode (recommended on).
	  * @param sLanguage         [String]  The language portion of the Internationalization Locale to run Ekit in.
	  * @param sCountry          [String]  The country portion of the Internationalization Locale to run Ekit in.
	  * @param enterBreak        [boolean] Specifies whether the ENTER key should insert breaks instead of paragraph tags.
	  */
	public EkitCoreSpell(boolean isParentApplet, String sRawDocument, URL urlStyleSheet, boolean includeToolBar, boolean showViewSource, boolean showMenuIcons, boolean editModeExclusive, String sLanguage, String sCountry, boolean base64, boolean multiBar, String toolbarSeq, boolean enterBreak)
	{
		this(isParentApplet, null, null, sRawDocument, null, urlStyleSheet, includeToolBar, showViewSource, showMenuIcons, editModeExclusive, sLanguage, sCountry, base64, false, true, multiBar, toolbarSeq, enterBreak);
	}

	/** Parent Only Specified Constructor
	  */
	public EkitCoreSpell(boolean isParentApplet)
	{
		this(isParentApplet, null, null, null, null, null, true, false, true, true, null, null, false, false, true, false, EkitCore.TOOLBAR_DEFAULT_SINGLE, false);
	}

	/** Empty Constructor
	  */
	public EkitCoreSpell()
	{
		this(false);
	}

	/* SpellCheckListener methods */
	public void spellingError(SpellCheckEvent event)
	{
		spellDialog.show(event);
	}

	/* Spell checking method (overrides empty method in basic core) */
	public void checkDocumentSpelling(Document doc)
	{
		spellCheck.checkSpelling(new DocumentWordTokenizer(doc));
	}

}
 