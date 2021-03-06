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

package com.actelion.research.datawarrior.task.view;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComponent;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.table.view.CompoundTableView;

public class DETaskCloseView extends DEAbstractViewTask {
	public static final String TASK_NAME = "Close View";

    private static Properties sRecentConfiguration;

	private DEMainPane	mMainPane;

	public DETaskCloseView(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		super(parent, mainPane, view);
		mMainPane = mainPane;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return true;
		}

	@Override
	public JComponent createInnerDialogContent() {
		return null;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof DETableView) ? "Table views cannot be closed." : null;
		}

	@Override
	public void runTask(Properties configuration) {
		mMainPane.closeView(getConfiguredViewName(configuration));
		}

	@Override
	public Properties getRecentConfiguration() {
    	return sRecentConfiguration;
    	}

	@Override
	public void setRecentConfiguration(Properties configuration) {
    	sRecentConfiguration = configuration;
    	}
	}
