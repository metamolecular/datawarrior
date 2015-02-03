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

package com.actelion.research.gui;

import javax.swing.*;
import java.io.File;

public class JFileChooserOverwrite extends JFileChooser {
	private static final long serialVersionUID = 20150101L;

	private File	mFile;
	private String	mExtension = null;

	public JFileChooserOverwrite() {
//		super(System.getProperty("user.dir"));
		}

	public void setExtension(String extension) {
		mExtension = extension;
		}

	public File getFile() {
		return mFile;
		}

	public void approveSelection() {
		if (getSelectedFile() != null) {
			String filename = getSelectedFile().getPath();
			if (mExtension != null) {
				int dotIndex = filename.lastIndexOf('.');
				int slashIndex = filename.lastIndexOf(File.separator);
				if (dotIndex == -1
				 || dotIndex < slashIndex)
					filename = filename.concat(mExtension);
		    	else if (!filename.substring(dotIndex).equalsIgnoreCase(mExtension)) {
					JOptionPane.showMessageDialog(this, "uncompatible file name extension.");
				    return;
					}
				}

			mFile = new File(filename);
			if (mFile != null) {
				if (mFile.exists()) {
					int answer = JOptionPane.showConfirmDialog(this,
						"This file already exists. Do you want to replace it?", "Warning",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

					if (answer == JOptionPane.OK_OPTION)
						super.approveSelection();
					}
				else
		        	super.approveSelection();
				}
			}
		}
	}
