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

package com.actelion.research.datawarrior.task.file;

import java.io.File;
import java.util.Properties;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableLoader;

public class DETaskOpenFile extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Open File";
    private static Properties sRecentConfiguration;

    private DataWarrior mApplication;

    public DETaskOpenFile(DataWarrior application, boolean isInteractive) {
		super(application.getActiveFrame(), "Open DataWarrior-, SD- or Text-File",
				FileHelper.cFileTypeDataWarriorCompatibleData, isInteractive);
		mApplication = application;
		}

    public DETaskOpenFile(DataWarrior application, String filePath) {
		super(application.getActiveFrame(), "Open DataWarrior-, SD- or Text-File",
				FileHelper.cFileTypeDataWarriorCompatibleData, filePath);
		mApplication = application;
		}

	@Override
	public Properties getRecentConfiguration() {
    	return sRecentConfiguration;
    	}

	@Override
	public void setRecentConfiguration(Properties configuration) {
    	sRecentConfiguration = configuration;
    	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		final int filetype = FileHelper.getFileType(file.getName());
		final DEFrame emptyFrame = mApplication.getEmptyFrame(file.getName());
		new CompoundTableLoader(emptyFrame, emptyFrame.getTableModel(), this) {
			public void finalStatus(boolean success) {
				if (success && filetype == FileHelper.cFileTypeDataWarrior) {
					emptyFrame.setDirty(false);
					}
				}
			}.readFile(file, new DERuntimeProperties(emptyFrame.getMainFrame()), filetype);

		return emptyFrame;
		}
	}
