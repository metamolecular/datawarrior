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

package com.actelion.research.datawarrior.task;

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;

/**
 * This class handles the redundancies that all classes operating on a window have:<br>
 * - If the task is started interactively, the referred window is known and should not be part of the dialog.
 * - If the dialog is opened as to edit a macro, the an editable combo box allows to select existing and non existing windows.
 */
public abstract class DEAbstractWindowTask extends ConfigurableTask {
	private static final String PROPERTY_WINDOW_NAME = "viewName";

	private DataWarrior	mApplication;
	private DEFrame		mWindow;
	private JComboBox	mComboBox;

	/**
	 * @param parent
	 * @param application
	 * @param window null, if not interactive. Otherwise the window this task is acting on.
	 */
	public DEAbstractWindowTask(Frame parent, DataWarrior application, DEFrame window) {
		super(parent, false);
		mApplication = application;
		mWindow = window;
		}

	@Override
	public final JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
		        			{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
        p.setLayout(new TableLayout(size));

        p.add(new JLabel("Window name:"), "1,1");
        mComboBox = new JComboBox();
        for (DEFrame w:mApplication.getFrameList())
       		mComboBox.addItem(w.getTitle());
        mComboBox.setEditable(true);
        p.add(mComboBox, "3,1");

        return p;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mWindow == null)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_WINDOW_NAME, mWindow.getTitle());
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_WINDOW_NAME, (String)mComboBox.getSelectedItem());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBox.setSelectedItem(configuration.getProperty(PROPERTY_WINDOW_NAME));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String windowName = configuration.getProperty(PROPERTY_WINDOW_NAME);
		if (windowName == null) {
			showErrorMessage("Window name not defined.");
			return false;
			}
		if (isLive) {
			boolean found = false;
			for (DEFrame w:mApplication.getFrameList()) {
	        	if (w.getTitle().equals(windowName)) {
	        		found = true;
	        		break;
	        		}
	        	}
			if (!found) {
				showErrorMessage("Window '"+windowName+"' not found.");
				return false;
				}
			}

		return true;
		}

	/**
	 * Assuming that the configuration contains a valid view name, this returns named view.
	 * @param configuration
	 * @return
	 */
	public DEFrame getConfiguredWindow(Properties configuration) {
		if (mWindow != null)
			return mWindow;

		String name = configuration.getProperty(PROPERTY_WINDOW_NAME);
		if (mApplication.getActiveFrame().getTitle().equals(name))
			return mApplication.getActiveFrame();	// if there are multiple windows with matching title, prefer the active one

		for (DEFrame w:mApplication.getFrameList())
        	if (w.getTitle().equals(name))
        		return w;

        return null;
		}

	public DataWarrior getApplication() {
		return mApplication;
		}
	}
