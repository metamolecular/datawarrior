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

package com.actelion.research.datawarrior.task.filter;

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.filter.JFilterPanel;

/**
 * This class handles the redundancies that all classes operating on a view have:<br>
 * - If the task is started interactively, the referred view is known and should not be part of the dialog.
 * - If the dialog is opened as to edit a macro, the an editable combo box allows to select existing and non existing view.
 */
public abstract class DEAbstractFilterTask extends ConfigurableTask {
	protected static final String PROPERTY_DUPLICATE = "duplicate";
	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_SETTINGS = "settings";

	private JComboBox			mComboBox;
	private JTextField			mTextFieldIndex;
	private CompoundTableModel	mTableModel;
	private DEPruningPanel		mPruningPanel;
	private JFilterPanel		mFilter;
	private boolean				mIsInteractive;

	/**
	 * @param parent
	 * @param pruningPanel 
	 * @param filter null, if not interactive
	 */
	public DEAbstractFilterTask(Frame parent, DEPruningPanel pruningPanel, JFilterPanel filter) {
		super(parent, false);
		mPruningPanel = pruningPanel;
		mFilter = filter;
		mTableModel = (filter != null) ? filter.getTableModel() : pruningPanel.getTableModel();
		mIsInteractive = (filter != null);
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return (previousConfiguration.getProperty(PROPERTY_COLUMN).equals(currentConfiguration.getProperty(PROPERTY_COLUMN))
			 && previousConfiguration.getProperty(PROPERTY_DUPLICATE).equals(currentConfiguration.getProperty(PROPERTY_DUPLICATE)));
		}

	@Override
	public final JComponent createDialogContent() {
		assert(!mIsInteractive);

		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
		        			{8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8} };
        p.setLayout(new TableLayout(size));

        if (supportsColumnSelection()) {
	        p.add(new JLabel("Column name:", JLabel.RIGHT), "1,1");
	        mComboBox = new JComboBox();
	        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
	        	if (getColumnQualificationError(column) == null)
	        		mComboBox.addItem(mTableModel.getColumnTitleExtended(column));
	        mComboBox.setEditable(true);
	        p.add(mComboBox, "3,1,5,1");
        	}

        mFilter = createFilterUI();
       	p.add(mFilter, "1,3,5,3");

        p.add(new JLabel("Duplicate Filter No:"), "1,5");
        mTextFieldIndex = new JTextField(1);
        p.add(mTextFieldIndex, "3,5");
        p.add(new JLabel("(usually '1')"), "5,5");

        return p;
		}

	/**
	 * Create the filter panel as inner dialog element for task configuration.
	 * @return filter panel, which does influence any row visibility
	 */
	public abstract JFilterPanel createFilterUI();

	public abstract Class<? extends JFilterPanel> getFilterClass();

	public abstract boolean supportsColumnSelection();

	/**
	 * @param column
	 * @return error message if the column doesn't qualify for this filter type
	 */
	protected abstract String getColumnQualificationError(int column);

	@Override
	public Properties getPredefinedConfiguration() {
		if (!mIsInteractive)
			return null;

		Properties configuration = new Properties();
		int column = mFilter.getColumnIndex();
		if (supportsColumnSelection())
			configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(column));
		configuration.setProperty(PROPERTY_DUPLICATE, Integer.toString(1+mPruningPanel.getFilterDuplicateIndex(mFilter, mFilter.getColumnIndex())));
		String filterSettings = mFilter.getSettings();
		if (filterSettings != null)
			configuration.setProperty(PROPERTY_SETTINGS, filterSettings);
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		if (supportsColumnSelection()) {
			String columnName = mTableModel.getColumnTitleNoAlias((String)mComboBox.getSelectedItem());
			if (columnName != null)
				configuration.setProperty(PROPERTY_COLUMN, columnName);
			}
		configuration.setProperty(PROPERTY_DUPLICATE, mTextFieldIndex.getText());
		String filterSettings = mFilter.getSettings();
		if (filterSettings != null)
			configuration.setProperty(PROPERTY_SETTINGS, filterSettings);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (supportsColumnSelection())
			mComboBox.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN, ""));
		mTextFieldIndex.setText(configuration.getProperty(PROPERTY_DUPLICATE, "1"));
		mFilter.applySettings(configuration.getProperty(PROPERTY_SETTINGS));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (supportsColumnSelection()
		 && mComboBox.getItemCount() != 0)
			mComboBox.setSelectedIndex(0);
		mTextFieldIndex.setText("1");
		}

	@Override
	public boolean isConfigurable() {
		if (!supportsColumnSelection())
			return true;	// allow hitlist filters and category browsers any time

		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (getColumnQualificationError(column) == null)
				return true;

		showErrorMessage("No data for this filter type found.");
		return false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		if (supportsColumnSelection() && columnName == null) {
			showErrorMessage("Column name not defined.");
			return false;
			}

		if (isLive && supportsColumnSelection()) {
			int column = mTableModel.findColumn(columnName);
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' not found.");
				return false;
				}
			String error = getColumnQualificationError(column);
			if (error != null) {
				showErrorMessage(error);
				return false;
				}
			}

		try {
			int index = Integer.parseInt(configuration.getProperty(PROPERTY_DUPLICATE, "1")) - 1;
			if (index < 0 || index > 31) {
				showErrorMessage("Duplicate filter number must be a small positive integer.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Duplicate filter number is not numerical");
			return false;
			}

		return true;
		}

	/**
	 * Returns the name of the interactive view that was passed with the constructor.
	 * @return null if task was not created interactively 
	 */
	public JFilterPanel getInteractiveFilter() {
		return mFilter;
		}

	/**
	 * @return true if this task was launched interactively
	 */
	public boolean isInteractive() {
		return mFilter != null;
		}

	/**
	 * @return the task's table model (is never null)
	 */
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	@Override
	public void runTask(Properties configuration) {
		int column = supportsColumnSelection() ? mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN)) : -1;
		int duplicate = Integer.parseInt(configuration.getProperty(PROPERTY_DUPLICATE, "1")) - 1;
		String settings = configuration.getProperty(PROPERTY_SETTINGS);
		JFilterPanel filter = mPruningPanel.getFilter(getFilterClass(), column, duplicate);
		if (filter != null)
			filter.applySettings(settings);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
