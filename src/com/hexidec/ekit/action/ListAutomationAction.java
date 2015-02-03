/*
GNU Lesser General Public License

ListAutomationAction
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
import java.util.StringTokenizer;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.hexidec.ekit.EkitCore;
import com.hexidec.ekit.component.*;

import com.hexidec.util.Translatrix;

/** Class for automatically creating bulleted lists from selected text
  */
public class ListAutomationAction extends HTMLEditorKit.InsertHTMLTextAction
{
	protected EkitCore parentEkit;
	private HTML.Tag baseTag;
	private String sListType;
	private HTMLUtilities htmlUtilities;

	public ListAutomationAction(EkitCore ekit, String sLabel, HTML.Tag listType)
	{
		super(sLabel, "", listType, HTML.Tag.LI);
		parentEkit    = ekit;
		baseTag       = listType;
		htmlUtilities = new HTMLUtilities(ekit);
	}

	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			JEditorPane jepEditor = (JEditorPane)(parentEkit.getTextPane());
			String selTextBase = jepEditor.getSelectedText();
			int textLength = -1;
			if(selTextBase != null)
			{
				textLength = selTextBase.length();
			}
			if(selTextBase == null || textLength < 1)
			{
				int pos = parentEkit.getCaretPosition();
				parentEkit.setCaretPosition(pos);
				if(ae.getActionCommand() != "newListPoint")
				{
					if(htmlUtilities.checkParentsTag(HTML.Tag.OL) || htmlUtilities.checkParentsTag(HTML.Tag.UL))
					{
						revertList(htmlUtilities.getListItemContainer());
						return;
					}
				}
				String sListType = (baseTag == HTML.Tag.OL ? "ol" : "ul");
				StringBuffer sbNew = new StringBuffer();
				if(htmlUtilities.checkParentsTag(baseTag))
				{
					sbNew.append("<li></li>");
					insertHTML(parentEkit.getTextPane(), parentEkit.getExtendedHtmlDoc(), parentEkit.getTextPane().getCaretPosition(), sbNew.toString(), 0, 0, HTML.Tag.LI);
				}
				else
				{
					boolean isLast = false;
					int caretPos = parentEkit.getCaretPosition();
					if(caretPos == parentEkit.getExtendedHtmlDoc().getLength())
					{
						isLast = true;
					}
					sbNew.append("<" + sListType + "><li></li></" + sListType + ">" + (isLast ? "<p style=\"margin-top: 0\">&nbsp;</p>" : ""));
					insertHTML(parentEkit.getTextPane(), parentEkit.getExtendedHtmlDoc(), parentEkit.getTextPane().getCaretPosition(), sbNew.toString(), 0, 0, (sListType.equals("ol") ? HTML.Tag.OL : HTML.Tag.UL));
					if(true)
					{
						parentEkit.setCaretPosition(caretPos + 1);
					}
				}
				parentEkit.refreshOnUpdate();
			}
			else
			{
				if(htmlUtilities.checkParentsTag(HTML.Tag.OL) || htmlUtilities.checkParentsTag(HTML.Tag.UL))
				{
					revertList(htmlUtilities.getListItemContainer());
					return;
				}
				else
				{
					String sListType = (baseTag == HTML.Tag.OL ? "ol" : "ul");
					HTMLDocument htmlDoc = (HTMLDocument)(jepEditor.getDocument());
					int iStart = jepEditor.getSelectionStart();
					int iEnd   = jepEditor.getSelectionEnd();
					String selText = htmlDoc.getText(iStart, iEnd - iStart);
/*
for(int ch = 0; ch < selText.length(); ch++)
{
	Element elem = htmlDoc.getCharacterElement(iStart + ch);
	System.out.println("elem " + ch + ": " + elem.getName());
	System.out.println("char " + ch + ": " + selText.charAt(ch) + " [" + Character.getNumericValue(selText.charAt(ch)) + "]");
	if(Character.getNumericValue(selText.charAt(ch)) < 0)
	{
		System.out.println("  is space?    " + ((selText.charAt(ch) == '\u0020') ? "YES" : "---"));
		System.out.println("  is return?   " + ((selText.charAt(ch) == '\r') ? "YES" : "---"));
		System.out.println("  is newline?  " + ((selText.charAt(ch) == '\n') ? "YES" : "---"));
		System.out.println("  is nextline? " + ((selText.charAt(ch) == '\u0085') ? "YES" : "---"));
		System.out.println("  is linesep?  " + ((selText.charAt(ch) == '\u2028') ? "YES" : "---"));
		System.out.println("  is parasep?  " + ((selText.charAt(ch) == '\u2029') ? "YES" : "---"));
		System.out.println("  is verttab?  " + ((selText.charAt(ch) == '\u000B') ? "YES" : "---"));
		System.out.println("  is formfeed? " + ((selText.charAt(ch) == '\u000C') ? "YES" : "---"));
	}
}
*/
					StringBuffer sbNew = new StringBuffer();
					sbNew.append("<" + sListType + ">");
					// tokenize on known linebreaks if present, otherwise manually parse on <br> break tags
					if((selText.indexOf("\r") > -1) || (selText.indexOf("\n") > -1))
					{
						String sToken = ((selText.indexOf("\r") > -1) ? "\r" : "\n");
						StringTokenizer stTokenizer = new StringTokenizer(selText, sToken);
						while(stTokenizer.hasMoreTokens())
						{
							sbNew.append("<li>");
							sbNew.append(stTokenizer.nextToken());
							sbNew.append("</li>");
						}
					}
					else
					{
						StringBuffer sbItem = new StringBuffer();
						for(int ch = 0; ch < selText.length(); ch++)
						{
							String elem = (htmlDoc.getCharacterElement(iStart + ch) != null ? htmlDoc.getCharacterElement(iStart + ch).getName().toLowerCase() : "[null]");
							if(elem.equals("br") && sbItem.length() > 0)
							{
								sbNew.append("<li>");
								sbNew.append(sbItem.toString());
								sbNew.append("</li>");
								sbItem.delete(0, sbItem.length());
							}
							else
							{
								sbItem.append(selText.charAt(ch));
							}
						}
					}
					sbNew.append("</" + sListType + ">");
					htmlDoc.remove(iStart, iEnd - iStart);
					insertHTML(jepEditor, htmlDoc, iStart, sbNew.toString(), 1, 1, null);
				}
			}
		}
		catch (BadLocationException ble) {}
	}

	private void revertList(Element element)
	{
//System.out.println("Reverting list " + element.toString());
		if(element == null)
		{
			return;
		}
		int pos = parentEkit.getCaretPosition();
		HTML.Tag tag = htmlUtilities.getHTMLTag(element);
		String source = parentEkit.getSourcePane().getText();
		boolean hit = false;
		String idString;
		int counter = 0;
		do
		{
			hit = false;
			idString = "revertomatictaggen" + counter;
			if(source.indexOf(idString) > -1)
			{
				counter++;
				hit = true;
				if(counter > 10000)
				{
					return;
				}
			}
		} while(hit);
		SimpleAttributeSet sa = new SimpleAttributeSet(element.getAttributes());
		sa.addAttribute("id", idString);
		parentEkit.getExtendedHtmlDoc().replaceAttributes(element, sa, tag);
		parentEkit.refreshOnUpdate();
		source = parentEkit.getSourcePane().getText();
		StringBuffer newHtmlString = new StringBuffer();
		int[] position = htmlUtilities.getPositions(element, source, true, idString);
		if(position == null)
		{
			return;
		}
		for(int i = 0; i < position.length; i++)
		{
			if(position[i] < 0)
			{
				return;
			}
		}
		int beginStartTag = position[0];
		int endStartTag = position[1];
		int beginEndTag = position[2];
		int endEndTag = position[3];
		newHtmlString.append(source.substring(0, beginStartTag));
		String listText = source.substring(endStartTag, beginEndTag);
//System.out.println("Affected text is :" + listText);
		if(parentEkit.getEnterKeyIsBreak())
		{
			listText = listText.replaceAll("<li>", "");
			listText = listText.replaceAll("</li>", "<br/>");
			newHtmlString.append("<br/>" + listText);
		}
		else
		{
			listText = listText.replaceAll("<li>", "<p style=\"margin-top: 0\">");
			listText = listText.replaceAll("</li>", "</p>");
			newHtmlString.append(listText);
		}
//System.out.println("Updated text is :" + listText);
		newHtmlString.append(source.substring(endEndTag, source.length()));
		parentEkit.getTextPane().setText(newHtmlString.toString());
		parentEkit.refreshOnUpdate();
	}
}