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

package com.actelion.research.datawarrior;

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.actelion.research.util.BrowserControl;

public class DEVersionChecker extends JDialog implements ActionListener {
	private static final long serialVersionUID = 20140209;
	private static final String DATAWARRIOR_VERSION = "v04.01.01";	// format v00.00.00[_beta]
	public static void checkVersion(final Frame parent, final boolean showUpToDateMessage) {
		new Thread(new Runnable() {
			public void run() {
				try {
					Preferences prefs = Preferences.userRoot().node(DataWarrior.PREFERENCES_ROOT);
					String id = ""+prefs.getLong(DataWarrior.PREFERENCES_KEY_FIRST_LAUNCH, 0L);
					String os = System.getProperty("os.name").replace(' ', '_');
					Object content = new URL("http://dwversion.openmolecules.org?what=detail&os="+os
											+"&current="+DATAWARRIOR_VERSION+"&id="+id).getContent();
					if (content != null) {
						BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream)content, "UTF-8"));
						String version = reader.readLine();
						if (version != null) {
							if (version.compareTo(DATAWARRIOR_VERSION) > 0) {
								String updateURL = reader.readLine();
								if (updateURL.startsWith("http")) {
									String text = reader.readLine();
									if (text != null) {
										StringBuilder sb = new StringBuilder(text);
										String line = reader.readLine();
										while (line != null) {
											sb.append("\n").append(line);
											line = reader.readLine();
											}
										text = sb.toString();
	
										new DEVersionChecker(parent, version, updateURL, text).setVisible(true);
										}
									}
								}
							else if (showUpToDateMessage) {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										JOptionPane.showMessageDialog(parent, "Your DataWarrior "+DATAWARRIOR_VERSION+" is up-to-date.");
										}
									});
								}
							}
						}
					}
				catch(Exception e) {
					e.printStackTrace();
					}
				}
			}).start();
		}

	private DEVersionChecker(Frame parent, String version, String updateURL, String text) {
		super(parent, "DataWarrior Update", true);
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16, TableLayout.PREFERRED,
							 16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 24, TableLayout.PREFERRED, 8} };
		getContentPane().setLayout(new TableLayout(size));
		getContentPane().add(new JLabel("A DataWarrior update is available: Version "+version, JLabel.CENTER), "1,1");
		getContentPane().add(new JLabel("(Your installed version is "+DATAWARRIOR_VERSION+")", JLabel.CENTER), "1,3");
		
		JEditorPane ep = new JEditorPane();
		ep.setEditable(false);
		ep.setContentType("text/plain");
		ep.setText(text);
		JScrollPane sp = new JScrollPane(ep, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setPreferredSize(new Dimension(480, 240));

		getContentPane().add(sp, "1,5");

		getContentPane().add(new JLabel("You may download the update at: ", JLabel.CENTER), "1,7");
		getContentPane().add(new JLabel(updateURL, JLabel.CENTER), "1,9");

		JButton urlButton = new JButton("Open Web-Browser");
		urlButton.addActionListener(this);
		urlButton.setActionCommand("open"+updateURL);

		JButton copyButton = new JButton("Copy Link");
		copyButton.addActionListener(this);
		copyButton.setActionCommand("copy"+updateURL);

		JButton closeButton = new JButton("Update Later");
		closeButton.addActionListener(this);

		double[][] bpsize = { {TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED},
							  {TableLayout.PREFERRED} };
		JPanel bp = new JPanel();
		bp.setLayout(new TableLayout(bpsize));
		bp.add(copyButton, "0,0");
		bp.add(urlButton, "2,0");
		bp.add(closeButton, "4,0");
		getContentPane().add(bp, "1,11");

		pack();
		setLocationRelativeTo(parent);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().startsWith("copy")) {
			StringSelection theData = new StringSelection(e.getActionCommand().substring(4));
	    	Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			return;
			}
		if (e.getActionCommand().startsWith("open")) {
			BrowserControl.displayURL(e.getActionCommand().substring(4));
			return;
			}

		setVisible(false);
		}
	}
