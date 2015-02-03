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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;


public abstract class DETaskAbstractSetViewOptions extends DEAbstractViewTask implements ActionListener,ChangeListener,ItemListener {
	private static final String PROPERTY_VIEW_NAME = "viewName";

	private Properties			mOldConfiguration,mLastGoodConfiguration;
	private boolean				mIgnoreEvents;

	/**
	 * Instantiates this task to be run in the event dispatch thread
	 * @param owner
	 * @param view null or view that is interactively updated
	 */
	public DETaskAbstractSetViewOptions(Frame owner, DEMainPane mainPane, CompoundTableView view) {
		this(owner, mainPane, view, false);
		}

	/**
	 * Instantiates this task to be run in the event dispatch thread
	 * @param owner
	 * @param view null or view that is interactively updated
	 * @param useOwnThread if false, the 
	 */
	public DETaskAbstractSetViewOptions(Frame owner, DEMainPane mainPane, CompoundTableView view, boolean useOwnThread) {
		super(owner, mainPane, view, useOwnThread);
		mIgnoreEvents = false;

		initialize();
		}

	/**
	 * Initialization to be called from the constructor. If a derived class' constructor
	 * defines members, which are needed in a derived initialization (i.e. in getViewConfiguration()),
	 * then override this method with an empty one and to the initialization after the derived
	 * constructor has completed.
	 */
	protected void initialize() {
		if (hasInteractiveView()) {
			mOldConfiguration = getViewConfiguration();
			mLastGoodConfiguration = mOldConfiguration;
			}
		}

	@Override
	public final DEFrame getNewFrontFrame() {
		return null;
		}

	public boolean isIgnoreEvents() {
		return mIgnoreEvents;
		}

	/**
	 * Set temporarily to true, if programmatical changes of checkboxes, popups and sliders
	 * shall not cause calls of getDialogConfiguration() and applyConfiguration().
	 * @param b
	 */
	public void setIgnoreEvents(boolean b) {
		mIgnoreEvents = b;
		}

	public JVisualization getVisualization() {
		CompoundTableView view  = getInteractiveView();
		return (view == null || !(view instanceof VisualizationPanel)) ? null : ((VisualizationPanel)view).getVisualization();
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return getConfiguredViewName(currentConfiguration).equals(getConfiguredViewName(previousConfiguration));
		}

	@Override
	/**
	 * If no view is specified, then this returns the recently applied view settings.
	 * Otherwise it returned the currently active view settings of the specified view.
	 */
	public final Properties getRecentConfiguration() {
		return (!hasInteractiveView())	? getRecentConfigurationLocal() : getViewConfiguration();
		}

	/**
	 * Creates and returns the current configuration, i.e. the respective view settings
	 * of the interactive view.
	 * @return
	 */
	private Properties getViewConfiguration() {
		Properties configuration = new Properties();

		configuration.setProperty(PROPERTY_VIEW_NAME, getInteractiveViewName());

		addViewConfiguration(configuration);
		return configuration;
		}

	/**
	 * Compiles the current configuration, i.e. all settings of the
	 * associated view in the context of this task.
	 * @param configuration is pre-initialized with the view identifying name
	 */
	public abstract void addViewConfiguration(Properties configuration);

	/**
	 * @return sRecentConfiguration of the derived class
	 */
	public abstract Properties getRecentConfigurationLocal();

	@Override
	public final Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		addDialogConfiguration(configuration);
		return configuration;
		}

	/**
	 * Compiles the configuration currently defined by the dialog GUI elements.
	 * @param configuration is pre-initialized with the view identifying name
	 */
	public abstract void addDialogConfiguration(Properties configuration);

	@Override
	public final void setDialogConfigurationToDefault() {
		if (mOldConfiguration != null) {
			setDialogConfiguration(mOldConfiguration);
			}
		else {
			setDialogToDefault();
			}

		enableItems();
		}

	@Override
	public final void setDialogConfiguration(Properties configuration) {
		mIgnoreEvents = true;
		super.setDialogConfiguration(configuration);
		setDialogToConfiguration(configuration);
		enableItems();
		mIgnoreEvents = false;
		if (hasInteractiveView() && isConfigurationValid(configuration, true))
			mLastGoodConfiguration = configuration;
		}

	public abstract void setDialogToConfiguration(Properties configuration);

	@Override
	public final boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		return isViewConfigurationValid(getConfiguredView(configuration), configuration);
		}

	/**
	 * @param view null (if editing macro) or the live view (if executing or editing live)
	 * @param configuration
	 * @return
	 */
	public abstract boolean isViewConfigurationValid(CompoundTableView view, Properties configuration);

	@Override
	public final void runTask(Properties configuration) {
		applyConfiguration(getConfiguredView(configuration), configuration, false);
		}

	public abstract void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting);

	/**
	 * This is supposed to enable/disable UI elements of which the enabling state
	 * depends dynamically on the setting of other UI element.
	 */
	public abstract void enableItems();

	/**
	 * This sets all dialog elements to reasonable default settings.
	 * The method is called only, if no associated view is available.
	 */
	public abstract void setDialogToDefault();

	@Override
	public void doCancelAction() {
		// View settings are immediately changed, when the dialog is modified.
		// Therefore, when pressing 'Cancel', we need to roll back...
		if (hasInteractiveView())
			applyConfiguration(getInteractiveView(), mOldConfiguration, false);
		closeDialog();
		}

	@Override
	public void doOKAction() {
		// View settings are immediately changed, when the dialog is modified.
		// Therefore, when pressing 'OK', we don't need to perform the task.
		if (hasInteractiveView())
			// don't set status OK to avoid running the task, but record task if is recording
			DEMacroRecorder.record(this, getDialogConfiguration());
		else
			// set OK is needed, if a new task was created to keep it with the given configuration
			setStatusOK();
		closeDialog();
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		update(false);
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (!(e.getSource() instanceof JComboBox) || e.getStateChange() == ItemEvent.SELECTED)
			update(false);
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		boolean isAdjusting = (e.getSource() instanceof JSlider) && ((JSlider)e.getSource()).getValueIsAdjusting();
		update(isAdjusting);
		}

	/**
	 * Calls enableItems(), gets the current dialog configuration with getDialogConfiguration(),
	 * checks the configuration with isConfigurationValid() and if that returns true calls applyConfiguration().
	 * If the current dialog configuration is not valid, calls setDialogToConfiguration(lastValidConfiguration)
	 * with disabled events.
	 * This method is called automatically by actionPerformed(),itemStateChanged() and stateChanged()
	 */
	private void update(boolean isAdjusting) {
		if (!mIgnoreEvents) {
			enableItems();
	
			if (hasInteractiveView()) {
				Properties configuration = getDialogConfiguration();
				if (isConfigurationValid(configuration, true)) {
					mLastGoodConfiguration = configuration;
					applyConfiguration(getInteractiveView(), configuration, isAdjusting);
					}
/*
 * TODO: One could consider instead of going back to the last valid configuration
 * (which is not necessarily available, if we edit a task of a macro)
 * catching the message by overriding showErrorMessage() and displaying it in
 * an additional JPanel below the dialog content.		
 */
				else if (mLastGoodConfiguration != null) {
					mIgnoreEvents = true;
					setDialogToConfiguration(mLastGoodConfiguration);
					enableItems();
					mIgnoreEvents = false;
					}
				}
			}
		}

	@Override
	public void showErrorMessage(final String msg) {
		// we have to postpone, because the message dialog should not interfere with the shown popup
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				DETaskAbstractSetViewOptions.super.showErrorMessage(msg);
				}
			});
		}
	}

