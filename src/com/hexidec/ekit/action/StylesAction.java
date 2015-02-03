/*
GNU Lesser General Public License

StylesAction
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

package com.hexidec.ekit.action;

import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

import com.hexidec.util.Translatrix;

/** Class for handling CSS style events
  */
public class StylesAction extends StyledEditorKit.StyledTextAction
{
	JComboBox parent;

	public StylesAction(JComboBox myParent)
	{
		super("css-style");
		parent = myParent;
	}

	public void actionPerformed(ActionEvent e)
	{
		if(!(this.isEnabled()))
		{
			return;
		}
		JEditorPane editor = getEditor(e);
		if(editor != null)
		{
			String stylename = (String)(parent.getSelectedItem());
			if(stylename == null)
			{
				return;
			}
			else if(stylename.equals(Translatrix.getTranslationString("NoCSSStyle")))
			{
				return;
			}
			boolean replace = false;
			MutableAttributeSet	attr = null;
			SimpleAttributeSet cls = new SimpleAttributeSet();
			cls.addAttribute(HTML.Attribute.CLASS, stylename);
			attr = new SimpleAttributeSet();
			attr.addAttribute(HTML.Tag.FONT, cls);
			MutableAttributeSet inattr = ((HTMLEditorKit)(editor.getEditorKitForContentType("text/html"))).getInputAttributes();
			inattr.addAttributes(attr);
			setCharacterAttributes(editor, attr, replace);
		}
	}
}
