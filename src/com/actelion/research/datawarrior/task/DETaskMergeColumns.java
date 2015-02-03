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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableModel;

public class DETaskMergeColumns extends ConfigurableTask {
	private static final String PROPERTY_COLUMN_LIST = "columnList";

	public static final String TASK_NAME = "Merge Columns";

	private static Properties sRecentConfiguration;

	private CompoundTableModel	mTableModel;
	private JList				mListColumns;
	private JTextArea			mTextArea;
	private boolean				mIsInteractive;

	public DETaskMergeColumns(DEFrame owner, boolean isInteractive) {
		super(owner, false);
		mTableModel = owner.getTableModel();
		mIsInteractive = isInteractive;
		}

	@Override
	public JPanel createDialogContent() {
		ArrayList<String> columnList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnSpecialType(column) == null)
				columnList.add(mTableModel.getColumnTitle(column));
		String[] itemList = columnList.toArray(new String[0]);
		Arrays.sort(itemList, new Comparator<String>() {
					public int compare(String s1, String s2) {
						return s1.compareToIgnoreCase(s2);
						}
					} );
		JScrollPane scrollPane = null;
		if (mIsInteractive) {
			mListColumns = new JList(itemList);
			scrollPane = new JScrollPane(mListColumns);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			}
		scrollPane.setPreferredSize(new Dimension(240,160));
		JPanel sp = new JPanel();
		sp.add(scrollPane, BorderLayout.CENTER);
		sp.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
		return sp;
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
	public String getDialogTitle() {
		return "Select Columns to be Merged";
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
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		int columnCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnSpecialType(column) == null)
				columnCount++;
		return columnCount >= 2;
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
			int[] column = new int[columnName.length];
			for (int i=0; i<columnName.length; i++) {
				column[i] = mTableModel.findColumn(columnName[i]);
				if (column[i] == -1) {
					showErrorMessage("Column '"+columnName[i]+"' not found.");
					return false;
					}
				}
			for (int i=0; i<column.length; i++) {
				if (mTableModel.getColumnSpecialType(column[i]) != null) {
					showErrorMessage("Column '"+columnName[i]+"' has a special type and cannot be merged.");
					return false;
					}
				}
			for (int i=1; i<column.length; i++) {
				if (!shareSameProperties(column[0], column[i])) {
					showErrorMessage("Columns '"+columnName[0]+"' and  '"+columnName[i]+"' cannot be merged because of incompatible properties.");
					return false;
					}
				}
			}

		return true;
		}

	private boolean shareSameProperties(int column1, int column2) {
		return mTableModel.getColumnProperties(column1).equals(mTableModel.getColumnProperties(column2));
		}

	@Override
	public void runTask(Properties configuration) {
		String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
		int[] column = new int[columnName.length];
		for (int i=0; i<columnName.length; i++)
			column[i] = mTableModel.findColumn(columnName[i]);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++)
			mergeCellContent(mTableModel.getTotalRecord(row), column);
		mTableModel.finalizeChangeColumn(column[0], 0, mTableModel.getTotalRowCount());

		boolean[] removeColumn = new boolean[mTableModel.getTotalColumnCount()];
		for (int i=1; i<column.length; i++)
			removeColumn[column[i]] = true;
		mTableModel.removeColumns(removeColumn, column.length-1);
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
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}

	private void mergeCellContent(CompoundRecord record, int[] column) {
		StringBuffer buf = new StringBuffer(mTableModel.encodeData(record, column[0]));
		String separator = mTableModel.isMultiLineColumn(column[0]) ?
				CompoundTableModel.cLineSeparator : CompoundTableModel.cEntrySeparator;
		for (int i=1; i<column.length; i++) {
			String value = mTableModel.encodeData(record, column[i]);
			if (value.length() != 0) {
				buf.append(separator);
				buf.append(value);
				}
			}

		int[] detailCount = null;
		for (int i=0; i<column.length; i++) {
			String[][] d = record.getDetailReferences(column[i]);
			if (d != null) {
				if (detailCount == null)
					detailCount = new int[d.length];
				for (int j=0; j<d.length; j++)
					if (d[j] != null)
						detailCount[j] += d[j].length;
				}
			}
		String[][] detail = null;
		if (detailCount != null) {
			detail = new String[detailCount.length][];
			int[] index = new int[detailCount.length];
			for (int i=0; i<detailCount.length; i++)
				detail[i] = new String[detailCount[i]];

			for (int i=0; i<column.length; i++) {
				String[][] d = record.getDetailReferences(column[i]);
				if (d != null) {
					for (int j=0; j<d.length; j++)
						if (d[j] != null)
							for (int k=0; k<d[j].length; k++)
								detail[j][index[j]++] = d[j][k];
					}
				}
			}

		record.setData(mTableModel.decodeData(buf.toString(), column[0]), column[0]);
		if (detail != null)
			record.setDetailReferences(column[0], detail);
		}
	}
