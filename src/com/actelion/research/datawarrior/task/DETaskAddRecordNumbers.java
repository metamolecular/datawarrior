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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.chem.UniqueStringList;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableModel;


public class DETaskAddRecordNumbers extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Add Row Numbers";

	private static final String PROPERTY_COLUMN_NAME = "columnName";
	private static final String PROPERTY_FIRST_NUMBER = "firstNumber";
	private static final String PROPERTY_VISIBLE_ONLY = "visibleOnly";
	private static final String PROPERTY_CATEGORY = "category";

	private static Properties sRecentConfiguration;

	private DEFrame				mSourceFrame;
    private JTextField          mTextFieldColumnName,mTextFieldFirstNo;
    private JCheckBox           mCheckBoxVisibleOnly,mCheckBoxUseSameForSame;
    private JComboBox           mComboBoxCategory;
    private boolean				mIsInteractive;

	public DETaskAddRecordNumbers(DEFrame sourceFrame, boolean isInteractive) {
		super(sourceFrame, true);
		mSourceFrame = sourceFrame;
		mIsInteractive = isInteractive;
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
	public boolean isConfigurable() {
        return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public JPanel createDialogContent() {
        mTextFieldColumnName = new JTextField("Row No", 12);
        mTextFieldFirstNo = new JTextField("1", 12);
        mCheckBoxVisibleOnly = new JCheckBox("Visible rows only", true);
        mCheckBoxUseSameForSame = new JCheckBox("Use same number for same", false);
        mCheckBoxUseSameForSame.addActionListener(this);
        mComboBoxCategory = new JComboBox();
        mComboBoxCategory.setEnabled(false);
        mComboBoxCategory.setEditable(!mIsInteractive);
        CompoundTableModel tableModel = mSourceFrame.getTableModel();
        for (int column=0; column<tableModel.getTotalColumnCount(); column++)
            if (tableModel.isColumnTypeCategory(column))
                mComboBoxCategory.addItem(tableModel.getColumnTitle(column));
        if (mIsInteractive && mComboBoxCategory.getItemCount() == 0)
        	mCheckBoxUseSameForSame.setEnabled(false);

        JPanel gp = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, mComboBoxCategory.getPreferredSize().height, 4} };
        gp.setLayout(new TableLayout(size));
        gp.add(new JLabel("Title of new column:", JLabel.RIGHT), "1,1");
        gp.add(new JLabel("First number to use:", JLabel.RIGHT), "1,3");
        gp.add(mTextFieldColumnName, "3,1");
        gp.add(mTextFieldFirstNo, "3,3");
        gp.add(mCheckBoxVisibleOnly, "1,5");
        gp.add(mCheckBoxUseSameForSame, "1,6");
        gp.add(mComboBoxCategory, "3,6");

        return gp;
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	String value = mTextFieldColumnName.getText();
    	if (value.length() != 0)
    		configuration.setProperty(PROPERTY_COLUMN_NAME, value);

    	value = mTextFieldFirstNo.getText();
    	if (value.length() != 0) {
	    	try {
	    		configuration.setProperty(PROPERTY_FIRST_NUMBER, ""+Integer.parseInt(value));
	    		}
	    	catch (NumberFormatException nfe) {}
    		}

   		configuration.setProperty(PROPERTY_VISIBLE_ONLY, mCheckBoxVisibleOnly.isSelected() ? "true" : "false");

   		if (mCheckBoxUseSameForSame.isSelected())
   			configuration.setProperty(PROPERTY_CATEGORY, (String)mComboBoxCategory.getSelectedItem());

    	return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COLUMN_NAME);
		mTextFieldColumnName.setText(value == null ? "Record No" : value);

		value = configuration.getProperty(PROPERTY_FIRST_NUMBER);
		mTextFieldFirstNo.setText(value == null ? "1" : value);

		mCheckBoxVisibleOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY)));

		value = configuration.getProperty(PROPERTY_CATEGORY);
		if (value != null) {
			mComboBoxCategory.setSelectedItem(value);
			mCheckBoxUseSameForSame.setSelected(value.equals(mComboBoxCategory.getSelectedItem()));
			mComboBoxCategory.setEnabled(mCheckBoxUseSameForSame.isSelected());
			}
		else {
			mCheckBoxUseSameForSame.setSelected(false);
			mComboBoxCategory.setEnabled(false);
			}
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			String category = configuration.getProperty(PROPERTY_CATEGORY);
			if (category != null) {
				int column = mSourceFrame.getTableModel().findColumn(category);
				if (column == -1) {
					showErrorMessage("Category column '"+category+"' was not found.");
					return false;
					}
				if (!mSourceFrame.getTableModel().isColumnTypeCategory(column)) {
					showErrorMessage("Column '"+category+"' does not contain categories.");
					return false;
					}		
				}
			}

		return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldColumnName.setText("Row No");
		mTextFieldFirstNo.setText("1");
		mCheckBoxVisibleOnly.setSelected(false);
		mCheckBoxUseSameForSame.setSelected(false);
        mComboBoxCategory.setEnabled(false);
		}

	@Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mCheckBoxUseSameForSame) {
            mComboBoxCategory.setEnabled(mCheckBoxUseSameForSame.isSelected());
            return;
            }
		}

	@Override
	public void runTask(Properties configuration) {
        CompoundTableModel tableModel = mSourceFrame.getTableModel();

        String[] columnName = new String[1];
		columnName[0] = configuration.getProperty(PROPERTY_COLUMN_NAME, "Row No");

		int recordNoColumn = tableModel.addNewColumns(columnName);

        int categoryColumn = tableModel.findColumn(configuration.getProperty(PROPERTY_CATEGORY));
        UniqueStringList list = (categoryColumn == -1) ? null : new UniqueStringList();

        boolean visibleOnly = "true".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY));
        int rowCount = visibleOnly ? tableModel.getRowCount() : tableModel.getTotalRowCount();
		String value = configuration.getProperty(PROPERTY_FIRST_NUMBER);
		int firstNo = (value == null) ? 1 : Integer.parseInt(value);
        for (int row=0; row<rowCount; row++) {
            CompoundRecord record = visibleOnly ?
            		tableModel.getRecord(row) : tableModel.getTotalRecord(row);
            String data = (categoryColumn == -1) ?
                    ""+(firstNo+row) : getCategoryNumbers(firstNo, list, tableModel.encodeData(record, categoryColumn));
            record.setData(tableModel.decodeData(data, recordNoColumn), recordNoColumn);
            }

        tableModel.finalizeNewColumns(recordNoColumn, null);
		}

    private String getCategoryNumbers(int firstNo, UniqueStringList list, String categories) {
        String[] entries = mSourceFrame.getTableModel().separateEntries(categories);
        String numbers = "";
        for (String entry:entries) {
            int index = list.getListIndex(entry);
            if (index == -1)
                index = list.addString(entry);
            if (numbers.length() != 0)
                numbers = numbers + CompoundTableModel.cEntrySeparator;
            numbers = numbers + (firstNo+index);
            }
        return numbers;
        }
	}

