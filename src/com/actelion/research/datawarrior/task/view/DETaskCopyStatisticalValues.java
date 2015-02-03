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
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;

public class DETaskCopyStatisticalValues extends DEAbstractViewTask {
	public static final String TASK_NAME = "Copy Statistical Values";

    private static Properties sRecentConfiguration;

	public DETaskCopyStatisticalValues(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		super(parent, mainPane, view);
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
		return (view instanceof VisualizationPanel) ? null : "Only 2D- and 3D-view support copying statistical values.";
		}

	@Override
	public void runTask(Properties configuration) {
        final JVisualization visualization = ((VisualizationPanel)getConfiguredView(configuration)).getVisualization();
		if (SwingUtilities.isEventDispatchThread()) {
			StringSelection theData = new StringSelection(visualization.getStatisticalValues());
	    	Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			}
		else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					StringSelection theData = new StringSelection(visualization.getStatisticalValues());
			    	Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
					}
				} );
			}
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
