/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.action;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;


public class DEHelpFrame extends JFrame implements ActionListener, HyperlinkListener {
    private static final long serialVersionUID = 0x20061025;

    private DEFrame 			mParent;
	private JEditorPane			mTextArea;		
	private JEditorPane			mContentArea;
 
	public DEHelpFrame(DEFrame parent) {
		super("DataWarrior Help");
		mParent = parent;

		getContentPane().setLayout(new BorderLayout());	//change this to tableLayout
		
		//1st jEditorPane
		mTextArea = new JEditorPane();
		mTextArea.setEditable(false);
		mTextArea.addHyperlinkListener(this);
		mTextArea.setContentType("text/html");
		try {
		    mTextArea.setPage(getClass().getResource("/html/help/basics.html"));
			}
		catch (IOException ioe) {
			JOptionPane.showMessageDialog(mParent, ioe.getMessage());
			return;
			}

		JScrollPane spTextArea = new JScrollPane(mTextArea);
		spTextArea.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		spTextArea.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spTextArea.setPreferredSize(new Dimension(250, 250));
		
		//2nd jEditorPane
		mContentArea = new JEditorPane();
		mContentArea.setEditable(false);
		mContentArea.addHyperlinkListener(this);
		mContentArea.setContentType("text/html");
		try {
			DataWarrior app = parent.getApplication();
			mContentArea.setPage(getClass().getResource(
					app.isActelion() ? "/html/help/contentActelion.html" : "/html/help/content.html"));
			}
		catch (IOException e) {
			JOptionPane.showMessageDialog(mParent,e.getMessage());
			return;
			}
		JScrollPane spContentArea = new JScrollPane(mContentArea);
		spContentArea.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		spContentArea.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spContentArea.setPreferredSize(new Dimension(250, 250));
		
		JTabbedPane tabbedContentPane = new JTabbedPane();
		tabbedContentPane.addTab("Content", spContentArea);
		
//		Create a split pane with the two scroll panes in it.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedContentPane, spTextArea);
	  	splitPane.setOneTouchExpandable(true);
	  	splitPane.setDividerLocation(250);

//		Provide minimum sizes for the two components in the split pane
	  	Dimension minimumSize = new Dimension(100, 50);
		tabbedContentPane.setMinimumSize(minimumSize);
		spTextArea.setMinimumSize(minimumSize);
		
		getContentPane().add(splitPane, BorderLayout.CENTER);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setSize(980, Math.min(1024, (int)getGraphicsConfiguration().getBounds().getHeight()-32));
		setLocation(new Point((int)(mParent.getLocationOnScreen().getX() + mParent.getSize().getWidth()/2 - this.getSize().getWidth()/2), (int)(mParent.getLocationOnScreen().getY() + mParent.getSize().getHeight()/2 - this.getSize().getHeight()/2)));
		setResizable(true);
		setVisible(true);
		} 
	
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			java.net.URL url = e.getURL();
			try {
				mTextArea.setPage(url);
				}
			catch(IOException ioe) {
				JOptionPane.showMessageDialog(mParent, ioe.getMessage(), "DataWarrior Help", JOptionPane.WARNING_MESSAGE);
				return;
				}
			}
		}
	
	public void actionPerformed(ActionEvent e){	
//		DocumentRenderer DocumentRenderer = new DocumentRenderer();
//		DocumentRenderer.print(mTextArea);
		}
	}