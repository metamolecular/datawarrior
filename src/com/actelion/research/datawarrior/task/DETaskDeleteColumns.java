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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableModel;

public class DETaskDeleteColumns extends ConfigurableTask {
	private static final String PROPERTY_COLUMN_LIST = "columnList";

	public static final String TASK_NAME = "Delete Columns";

	private static Properties sRecentConfiguration;

	private CompoundTableModel	mTableModel;
	private JList				mListColumns;
	private JTextArea			mTextArea;
	private boolean				mIsInteractive;

	public DETaskDeleteColumns(DEFrame owner, boolean isInteractive) {
		super(owner, false);
		mTableModel = owner.getTableModel();
		mIsInteractive = isInteractive;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Select columns for deletion!"), "1,1");
		JScrollPane scrollPane = null;

		if (mIsInteractive) {
			ArrayList<String> columnList = new ArrayList<String>();
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				columnList.add(mTableModel.getColumnTitle(column));
			String[] itemList = columnList.toArray(new String[0]);
			Arrays.sort(itemList, new Comparator<String>() {
						public int compare(String s1, String s2) {
							return s1.compareToIgnoreCase(s2);
							}
						} );
			mListColumns = new JList(itemList);
			scrollPane = new JScrollPane(mListColumns);
	//		scrollPane.setPreferredSize(new Dimension(240,240));	de-facto limits width when long column names need more space
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			scrollPane.setPreferredSize(new Dimension(240,240));
			}

		content.add(scrollPane, "1,3");
		return content;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalColumnCount() == 0) {
			showErrorMessage("No columns found.");
			return false;
			}
		return true;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties p = new Properties();
		String columnNames = mIsInteractive ?
				  getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && columnNames.length() != 0)
			p.setProperty(PROPERTY_COLUMN_LIST, columnNames);
		return p;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (mIsInteractive)
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mIsInteractive)
			mListColumns.clearSelection();
		else
			mTextArea.setText("");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (columnList == null) {
			showErrorMessage("No columns defined.");
			return false;
			}

		if (isLive) {
			String[] columnName = columnList.split("\\t");
			for (int i=0; i<columnName.length; i++) {
				int column = mTableModel.findColumn(columnName[i]);
				if (column == -1) {
					showErrorMessage("Column '"+columnName[i]+"' not found.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
		boolean[] removeColumn = new boolean[mTableModel.getTotalColumnCount()];
		for (int i=0; i<columnName.length; i++)
			removeColumn[mTableModel.findColumn(columnName[i])] = true;
		mTableModel.removeColumns(removeColumn, columnName.length);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
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
