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

import java.awt.Frame;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import com.actelion.research.chem.io.CompoundFileHelper;

public class FileHelper extends CompoundFileHelper {
	private Frame mParentFrame;

	public FileHelper(Frame parent) {
		mParentFrame = parent;
		}

	public String selectOption(String message, String title, String[] option) {
        return (String)JOptionPane.showInputDialog(mParentFrame, message, title,
        										   JOptionPane.QUESTION_MESSAGE, null, option, option[0]);
		}

	public void showMessage(String message) {
		JOptionPane.showMessageDialog(mParentFrame, message);
		}

	/**
	 * For compatibility reasons...
	 * @param parent
	 * @param title
	 * @param filetypes
	 * @return
	 */
	public static File getFile(Frame parent, String title, int filetypes) {
		return new FileHelper(parent).selectFileToOpen(title, filetypes);
		}

	public File selectFileToOpen(String title, int filetypes) {
		return selectFileToOpen(title, filetypes, null);
		}

	/**
	 * Shows a file-open-dialog, lets the user choose and returns the selected file.
	 * @param title of the dialog shown
	 * @param filetypes one or more file types defined in CompoundFileHelper
	 * @param initialFileName null or a suggested file name with or without complete path
	 * @return null or selected file
	 */
	public File selectFileToOpen(String title, int filetypes, String initialFileName) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(title);
		fileChooser.setCurrentDirectory(getCurrentDirectory());
		fileChooser.setFileFilter(createFileFilter(filetypes, false));
		if (initialFileName != null) {
			int index = initialFileName.lastIndexOf(File.separatorChar);
			if (index == -1) {
				fileChooser.setSelectedFile(new File(FileHelper.getCurrentDirectory(), initialFileName));
				}
			else {
				String directory = initialFileName.substring(0, index);
				if (new File(directory).exists())
					fileChooser.setSelectedFile(new File(initialFileName));
				else
					fileChooser.setSelectedFile(new File(FileHelper.getCurrentDirectory(), initialFileName.substring(index+1)));
				}
			}
		int option = fileChooser.showOpenDialog(mParentFrame);
		setCurrentDirectory(fileChooser.getCurrentDirectory());
		if (option != JFileChooser.APPROVE_OPTION)
			return null;
		File selectedFile = fileChooser.getSelectedFile();
		if (selectedFile.exists())
			return selectedFile;
		if (selectedFile.getName().contains("."))
			return selectedFile;
		ArrayList<String> list = getExtensionList(filetypes);
		for (String extension:list) {
			File file = new File(selectedFile.getPath()+extension);
			if (file.exists())
				return file;
			}
		return selectedFile;
		}

	/**
	 * Shows a file-save-dialog, lets the user choose and return the file's path and name.
	 * @param title of the dialog shown
	 * @param filetype one of the file types defined in CompoundFileHelper
	 * @param newFileName null or a suggested file name with or without extension
	 * @return null or complete file path and name
	 */
	public String selectFileToSave(String title, int filetype, String newFileName) {
		JFileChooserOverwrite fileChooser = new JFileChooserOverwrite();
		fileChooser.setCurrentDirectory(getCurrentDirectory());
		fileChooser.setFileFilter(createFileFilter(filetype, true));
		fileChooser.setExtension(FileHelper.getExtension(filetype));
		if (newFileName == null) {
			fileChooser.setSelectedFile(new File(FileHelper.getCurrentDirectory(), "Untitled"));
			}
		else {
			int index = newFileName.lastIndexOf(File.separatorChar);
			if (index == -1) {
				fileChooser.setSelectedFile(new File(FileHelper.getCurrentDirectory(), newFileName));
				}
			else {
				String directory = newFileName.substring(0, index);
				if (new File(directory).exists())
					fileChooser.setSelectedFile(new File(newFileName));
				else
					fileChooser.setSelectedFile(new File(FileHelper.getCurrentDirectory(), newFileName.substring(index+1)));
				}
			}
		int option = fileChooser.showSaveDialog(mParentFrame);
		setCurrentDirectory(fileChooser.getCurrentDirectory());
		return (option != JFileChooser.APPROVE_OPTION) ? null : fileChooser.getFile().getPath();
		}
	}
