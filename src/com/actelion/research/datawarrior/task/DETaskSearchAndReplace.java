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

import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableModel;


public class DETaskSearchAndReplace extends ConfigurableTask implements Runnable {
    public static final long serialVersionUID = 0x20130131;

	public static final String TASK_NAME = "Search And Replace";

    private static final String PROPERTY_COLUMN = "column";
    private static final String PROPERTY_WHAT = "what";
    private static final String PROPERTY_WITH = "with";
    private static final String PROPERTY_CASE_SENSITIVE = "caseSensitive";

    private static final String OPTION_ANY_COLUMN = "<any column>";
    private static final String CODE_ANY_COLUMN = "<any>";

    private static Properties sRecentConfiguration;

    private DEFrame				mParentFrame;
    private CompoundTableModel	mTableModel;
	private JTextField			mTextFieldWhat,mTextFieldWith;
	private JComboBox			mComboBoxColumn;
	private JCheckBox           mCheckBoxCaseSensitive;
	private boolean				mIsInteractive;

	@Override
	public Properties getRecentConfiguration() {
    	return sRecentConfiguration;
    	}

	@Override
	public void setRecentConfiguration(Properties configuration) {
    	sRecentConfiguration = configuration;
    	}

	public DETaskSearchAndReplace(DEFrame owner, boolean isInteractive) {
		super(owner, true);
		mParentFrame = owner;
		mTableModel = mParentFrame.getTableModel();
		mIsInteractive = isInteractive;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() != 0)
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (qualifiesAsColumn(column))
					return true;

        showErrorMessage("Search and replace requires a column with alphanumerical content.");
        return false;
		}

	@Override
    public JPanel createDialogContent() {
		mComboBoxColumn = new JComboBox();
		mComboBoxColumn.addItem(OPTION_ANY_COLUMN);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsColumn(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.setEditable(!mIsInteractive);

		mTextFieldWhat = new JTextField(16);
		mTextFieldWith = new JTextField(16);
		mCheckBoxCaseSensitive = new JCheckBox("case sensitive");

        JPanel p1 = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 12, TableLayout.PREFERRED, 4,
        						TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
        p1.setLayout(new TableLayout(size));

		p1.add(new JLabel("Column:", SwingConstants.RIGHT), "1,1");
		p1.add(mComboBoxColumn, "3,1");
        p1.add(new JLabel("Search:", SwingConstants.RIGHT), "1,3");
        p1.add(mTextFieldWhat, "3,3");
        p1.add(new JLabel("Replace with:", SwingConstants.RIGHT), "1,5");
        p1.add(mTextFieldWith, "3,5");
        p1.add(mCheckBoxCaseSensitive, "3,7");

        return p1;
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	String column = OPTION_ANY_COLUMN.equals(mComboBoxColumn.getSelectedItem()) ? CODE_ANY_COLUMN
    				: mTableModel.getColumnTitleNoAlias(mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem()));
		configuration.setProperty(PROPERTY_COLUMN, column);

    	String what = mTextFieldWhat.getText();
    	if (what.length() != 0)
    		configuration.setProperty(PROPERTY_WHAT, what);

    	configuration.setProperty(PROPERTY_WITH, mTextFieldWith.getText());

    	if (mCheckBoxCaseSensitive.isSelected())
        	configuration.setProperty(PROPERTY_CASE_SENSITIVE, "true");

    	return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COLUMN);
		if (value == null || value.equals(CODE_ANY_COLUMN)) {
			mComboBoxColumn.setSelectedItem(OPTION_ANY_COLUMN);
			}
		else {
			int column = mTableModel.findColumn(value);
			if (column != -1 && qualifiesAsColumn(column))
				mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			}

		mTextFieldWhat.setText(configuration.getProperty(PROPERTY_WHAT, ""));
		mTextFieldWith.setText(configuration.getProperty(PROPERTY_WITH, ""));

		value = configuration.getProperty(PROPERTY_CASE_SENSITIVE);
		mCheckBoxCaseSensitive.setSelected("true".equals(value));
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (configuration.getProperty(PROPERTY_WHAT, "").length() == 0) {
			showErrorMessage("No search string defined.");
			return false;
			}
		if (configuration.getProperty(PROPERTY_WHAT, "").contains("\t")) {
			showErrorMessage("TAB is not allowed in replace string.");
			return false;
			}

		if (isLive) {
			String value = configuration.getProperty(PROPERTY_COLUMN);
			if (!value.equals(CODE_ANY_COLUMN)) {
				int column = mTableModel.findColumn(value);
				if (column == -1) {
					showErrorMessage("Column '"+value+"' not found.");
					return false;
					}
				if (!qualifiesAsColumn(column)) {
					showErrorMessage("Column '"+value+"' is not alphanumerical.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mComboBoxColumn.setSelectedIndex(0);
		mTextFieldWhat.setText("");
		mTextFieldWith.setText("");
		mCheckBoxCaseSensitive.setSelected(false);
		}

	private boolean qualifiesAsColumn(int column) {
		return (mTableModel.getColumnSpecialType(column) == null
			 && (mTableModel.isColumnTypeString(column)
			  || mTableModel.isColumnTypeDouble(column)
			  || mTableModel.isColumnTypeCategory(column))
			 && !mTableModel.isColumnTypeRangeCategory(column));
		}

	@Override
	public void runTask(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COLUMN);
		int targetColumn = (value == null || value.equals(CODE_ANY_COLUMN)) ? -1 : mTableModel.findColumn(value);
		String what = configuration.getProperty(PROPERTY_WHAT, "").replace("\\n", "\n");
		String with = configuration.getProperty(PROPERTY_WITH, "").replace("\\n", "\n");
		boolean isCaseSensitive = "true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE));
		if (!isCaseSensitive)
			what = what.toLowerCase();

		int replacements = 0;
		int columns = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (column == targetColumn || (targetColumn == -1 && mTableModel.isColumnDisplayable(column))) {
				boolean found = false;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					value = mTableModel.getTotalValueAt(row, column);
					if (isCaseSensitive) {
						if (value.contains(what)) {
							mTableModel.setTotalValueAt(value.replace(what, with), row, column);
							replacements++;
							found = true;
							}
						}
					else {
						if (value.toLowerCase().contains(what)) {
							StringBuilder newValue = new StringBuilder();
							String lowerValue = value.toLowerCase();
							int oldValueIndex = 0;
							int index = lowerValue.indexOf(what);
							while (index != -1) {
								if (oldValueIndex < index)
									newValue.append(value.substring(oldValueIndex, index));
		
								newValue.append(with);
								oldValueIndex = index + what.length();
		
								index = lowerValue.indexOf(what, oldValueIndex);
								}
		
							if (oldValueIndex < value.length())
								newValue.append(value.substring(oldValueIndex));
		
							mTableModel.setTotalValueAt(newValue.toString(), row, column);
							replacements++;
							found = true;
							}
						}
					}

				if (found) {
					mTableModel.finalizeChangeColumn(column, 0, mTableModel.getTotalRowCount());
					columns++;
					}
				}
			}

		if (mIsInteractive)
			showInteractiveTaskMessage("'"+what+"' was replaced "+replacements+" times in "+columns+" columns.", JOptionPane.INFORMATION_MESSAGE);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}