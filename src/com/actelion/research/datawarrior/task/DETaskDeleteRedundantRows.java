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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableModel;

public class DETaskDeleteRedundantRows extends ConfigurableTask {
	public static final int MODE_MERGE_REDUNDANT = 0;
    public static final int MODE_REMOVE_REDUNDANT = 1;
    public static final int MODE_REMOVE_UNIQUE = 2;

    public static final String[] TASK_NAME = { "Merge Equivalent Rows", "Delete Redundant Rows", "Delete Unique Rows" };

	private static final String PROPERTY_COLUMN_LIST = "columnList";

	private static Properties sRecentConfiguration;

	private CompoundTableModel	mTableModel;
	private JList				mListColumns;
	private JTextArea			mTextArea;
	private int					mMode;
	private boolean				mIsInteractive;

    public DETaskDeleteRedundantRows(DEFrame owner, int mode, boolean isInteractive) {
		super(owner, false);
		mTableModel = owner.getTableModel();
		mMode = mode;
		mIsInteractive = isInteractive;
    	}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Select column(s) to consider for equivalence check!", SwingConstants.CENTER), "1,1");
		content.add(new JLabel("(use <CTRL> for multiple selections)", SwingConstants.CENTER), "1,3");

		ArrayList<String> columnNameList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            String specialType = mTableModel.getColumnSpecialType(column);
            if (specialType == null
             || specialType.equals(CompoundTableModel.cColumnTypeIDCode)
             || specialType.equals(CompoundTableModel.cColumnTypeRXNCode))
			columnNameList.add(mTableModel.getColumnTitle(column));
			}
		JScrollPane scrollPane = null;
		if (mIsInteractive) {
			mListColumns = new JList(columnNameList.toArray());
			scrollPane = new JScrollPane(mListColumns);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			}
		scrollPane.setPreferredSize(new Dimension(220,160));
		content.add(scrollPane, "1,5");

		return content;
	    }

	@Override
	public String getTaskName() {
		return TASK_NAME[mMode];
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() == 0) {
			showErrorMessage(mMode==MODE_MERGE_REDUNDANT ? "Can't merge rows of an empty table."
														 : "Can't delete rows from an empty table.");
			return false;
			}
		if (mTableModel.getTotalRowCount() == 1) {
			showErrorMessage(mMode==MODE_MERGE_REDUNDANT ? "Can't merge rows a single row."
														 : "Can't find redundant rows in a single row.");
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
			int[] column = new int[columnName.length];
			for (int i=0; i<columnName.length; i++) {
				column[i] = mTableModel.findColumn(columnName[i]);
				if (column[i] == -1) {
					showErrorMessage("Column '"+columnName[i]+"' not found.");
					return false;
					}
				}
			for (int i=0; i<column.length; i++) {
				if (!columnQualifies(column[i])) {
					showErrorMessage("Column '"+columnName[i]+"' cannot be used for a redundency check.");
					return false;
					}
				}
			}

		return true;
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

	private boolean columnQualifies(int column) {
		String specialType = mTableModel.getColumnSpecialType(column);
        return (specialType == null
             || specialType.equals(CompoundTableModel.cColumnTypeIDCode)
             || specialType.equals(CompoundTableModel.cColumnTypeRXNCode));
		}

	@Override
	public void runTask(Properties configuration) {
		String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
        int[] columnList = new int[columnName.length];
        boolean[] columnMask = new boolean[mTableModel.getTotalColumnCount()];
        boolean[] columnError = new boolean[mTableModel.getTotalColumnCount()];
		for (int i=0; i<columnList.length; i++) {
			columnList[i] = mTableModel.findColumn(columnName[i]);
			columnMask[columnList[i]] = true;
		    }

		CompoundRecord[] record = new CompoundRecord[mTableModel.getTotalRowCount()];
		for (int row=0; row<mTableModel.getTotalRowCount(); row++)
			record[row] = mTableModel.getTotalRecord(row);

        RedundancyComparator comparator = new RedundancyComparator(mTableModel, columnList);

        Arrays.sort(record, comparator);

        if (mMode == MODE_REMOVE_UNIQUE) {
            boolean isFirstInSet = true;
            for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                boolean isLastInSet = (row+1 == mTableModel.getTotalRowCount() || comparator.compare(record[row], record[row+1]) != 0);

                if (isFirstInSet && isLastInSet)
                    record[row].markForDeletion();

                isFirstInSet = isLastInSet;
                }
            }
        else {
            int firstRow = 0;
            int row = 1;
			while (row<mTableModel.getTotalRowCount()) {
				if (comparator.compare(record[row-1], record[row]) == 0)
				    record[row].markForDeletion();
				else if (mMode == MODE_MERGE_REDUNDANT) {
				    if (firstRow < row-1)
				        mergeRowContent(record, firstRow, row-1, columnMask, columnError);

				    firstRow = row;
				    }
				row++;
			    }
            if (mMode == MODE_MERGE_REDUNDANT && firstRow < row-1)
                mergeRowContent(record, firstRow, row-1, columnMask, columnError);
            }

        mTableModel.finalizeDeletion();
        if (mMode == MODE_MERGE_REDUNDANT) {
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
                if (!columnMask[column]
                 && mTableModel.getColumnSpecialType(column) == null)
                    mTableModel.finalizeChangeColumn(column, 0, mTableModel.getTotalRowCount());
            }

        if (mIsInteractive && mMode == MODE_MERGE_REDUNDANT) {
            boolean errorFound = false;
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (columnError[column]) {
                    errorFound = true;
                    break;
                    }
                }
            if (errorFound) {
                StringBuffer message = new StringBuffer("Some cells with non-matching content could not be merged.\n"
                                                       +"Instead the first cell's value was used. Affected columns are\n");
                for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                    if (columnError[column]) {
                        message.append(mTableModel.getColumnTitle(column)+", ");
                        }
                    }
                message.setLength(message.length()-2);
                message.append("!");
                JOptionPane.showMessageDialog(getParentFrame(),
                                              message.toString(),
                                              "Some Cells Couldn't Be Merged",
                                              JOptionPane.WARNING_MESSAGE);
                }
            }
		}

	private void mergeRowContent(CompoundRecord[] record, int firstRow, int lastRow, boolean[] skipColumn, boolean[] columnError) {
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            if (mTableModel.getColumnSpecialType(column) == null) {
                for (int row=firstRow+1; row<=lastRow; row++) {
                    if (!cellContentMatches(record[firstRow], record[row], column)) {
                        mergeCellContent(record, firstRow, lastRow, column);
                        break;
                        }
                    }
                }
            // don't merge content of special types, but raise warning when content differs
            else if (!columnError[column]) {
                String firstRowData = mTableModel.encodeData(record[firstRow], column);
                for (int row=firstRow+1; row<=lastRow; row++) {
                    String rowData = mTableModel.encodeData(record[row], column);
                    if (firstRowData.length() == 0 && rowData.length() != 0) {
                        record[firstRow].setData(record[row].getData(column), column);
                        break;
                        }
                    if (!firstRowData.equals(rowData)) {
                        columnError[column] = true;
                        break;
                        }
                    }
                }
            }
	    }

	private void mergeCellContent(CompoundRecord[] record, int firstRow, int lastRow, int column) {
	    StringBuffer buf = new StringBuffer(mTableModel.encodeData(record[firstRow], column));
	    String separator = CompoundTableModel.cLineSeparator;
//	    String separator = mTableModel.isMultiLineColumn(column) ?
//	    		CompoundTableModel.cLineSeparator : CompoundTableModel.cEntrySeparator;
        for (int row=firstRow+1; row<=lastRow; row++) {
            buf.append(separator);
            buf.append(mTableModel.encodeData(record[row], column));
            }

        int[] detailCount = null;
        for (int row=firstRow; row<=lastRow; row++) {
            String[][] d = record[row].getDetailReferences(column);
            if (d != null) {
                if (detailCount == null)
                    detailCount = new int[d.length];
                for (int i=0; i<d.length; i++)
                    if (d[i] != null)
                        detailCount[i] += d[i].length;
                }
            }
        String[][] detail = null;
        if (detailCount != null) {
            detail = new String[detailCount.length][];
            int[] index = new int[detailCount.length];
            for (int i=0; i<detailCount.length; i++)
                detail[i] = new String[detailCount[i]];

            for (int row=firstRow; row<=lastRow; row++) {
                String[][] d = record[row].getDetailReferences(column);
                if (d != null) {
                    for (int i=0; i<d.length; i++)
                        if (d[i] != null)
                            for (int j=0; j<d[i].length; j++)
                                detail[i][index[i]++] = d[i][j];
                    }
                }
            }

        record[firstRow].setData(mTableModel.decodeData(buf.toString(), column), column);
        if (detail != null)
            record[firstRow].setDetailReferences(column, detail);
	    }

	private boolean cellContentMatches(CompoundRecord r1, CompoundRecord r2, int column) {
	    if (!mTableModel.encodeData(r1, column).equals(mTableModel.encodeData(r2, column)))
            return false;
	    String[][] d1 = r1.getDetailReferences(column);
        String[][] d2 = r2.getDetailReferences(column);
        if (d1 == null && d2 == null)
            return true;
        if (d1 == null || d2 == null)
            return false;
        if (d1.length != d2.length)
            return false;
        for (int i=0; i<d1.length; i++) {
            if (d1[i] == null && d2[i] == null)
                continue;
            if (d1[i] == null || d2[i] == null)
                return false;
            if (d1[i].length != d2[i].length)
                return false;
            for (int j=0; j<d1[i].length; j++)
                if (!d1[i][j].equals(d2[i][j]))
                    return false;
            }
        return true;
	    }
	}

class RedundancyComparator implements Comparator<CompoundRecord> {
    private CompoundTableModel mTableModel;
	private int[] mColumnList;

	public RedundancyComparator(CompoundTableModel tableModel, int[] columnList) {
        mTableModel = tableModel;
		mColumnList = columnList;
		}

	public int compare(CompoundRecord o1, CompoundRecord o2) {
        int comparison = 0;
		for (int i=0; i<mColumnList.length; i++) {
            String s1 = mTableModel.getValue(o1, mColumnList[i]);
            String s2 = mTableModel.getValue(o2, mColumnList[i]);
            comparison = (s1 == null) ? ((s2 == null) ? 0 : 1)
                       : (s2 == null) ? -1
                       : s1.compareTo(s2);
            if (comparison != 0)
                return comparison;
			}
		return comparison;
		}
	}
