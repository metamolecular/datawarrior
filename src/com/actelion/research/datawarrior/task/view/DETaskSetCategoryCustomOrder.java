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

import info.clearthought.layout.TableLayout;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;

import com.actelion.research.chem.SortedStringList;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.table.ChemistryCellRenderer;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.DetailTableCellRenderer;

public class DETaskSetCategoryCustomOrder extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Set Category Custom Order";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_LIST = "list";

    private static Properties sRecentConfiguration;

    private JComboBox			mComboBoxColumn;
    private CompoundTableModel  mTableModel;
    private JRadioButton		mRadioButton;
    private JList				mList;
	private JTextArea			mTextArea;
    private ListCellRenderer	mDefaultRenderer;
    private DefaultListModel	mListModel;
	private int					mDefaultColumn;

	/**
	 * 
	 * @param parent
	 * @param tableModel
	 * @param defaultColumn -1 if not live
	 */
	public DETaskSetCategoryCustomOrder(Frame parent, CompoundTableModel tableModel, int defaultColumn) {
		super(parent, false);
		mTableModel = tableModel;
		mDefaultColumn = defaultColumn;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
        p.setLayout(new TableLayout(size));

		mComboBoxColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifies(mTableModel, column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.addActionListener(this);
		mComboBoxColumn.setEditable(mDefaultColumn == -1);
		p.add(new JLabel("Column:"), "1,1");
		p.add(mComboBoxColumn, "3,1");

		mRadioButton = new JRadioButton("Use custom order");
		mRadioButton.addActionListener(this);
		p.add(mRadioButton, "1,3,3,3");

		p.add(new JLabel("Define order of category items:"), "1,5,3,5");

		JScrollPane scrollPane = null;
		if (mDefaultColumn != -1) {
			mListModel = new DefaultListModel();
	        mList = new JList(mListModel);
	        mList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			mList.setDropMode(DropMode.INSERT);
	        mList.setTransferHandler(new ListTransferHandler());
	        mList.setDragEnabled(true);

	        scrollPane = new JScrollPane(mList);
	        int height = Math.min(640, (1 + mTableModel.getCategoryCount(mDefaultColumn))
	        			* ((mTableModel.getColumnSpecialType(mDefaultColumn) == null) ? 20 : 80));
	        scrollPane.setPreferredSize(new Dimension(240, height));
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			scrollPane.setPreferredSize(new Dimension(240, 240));
			}

        p.add(scrollPane, "1,7,3,7");

        return p;
		}

	public static boolean columnQualifies(CompoundTableModel tableModel, int column) {
		return !tableModel.isColumnTypeDouble(column)
			&&  tableModel.isColumnTypeCategory(column)
			&& !tableModel.isColumnTypeRangeCategory(column);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		String columnName = (String)mComboBoxColumn.getSelectedItem();
		if (mDefaultColumn != -1)
			columnName = mTableModel.getColumnTitleNoAlias(mTableModel.findColumn(columnName));
		configuration.put(PROPERTY_COLUMN, columnName);
		if (mRadioButton.isSelected()) {
			if (mDefaultColumn != -1) {
				StringBuilder sb = new StringBuilder((String)mListModel.elementAt(0));
		    	for (int i=1; i<mListModel.getSize(); i++)
		    		sb.append('\t').append((String)mListModel.elementAt(i));
				configuration.put(PROPERTY_LIST, sb.toString());
				}
			else {
				configuration.put(PROPERTY_LIST, mTextArea.getText().replace('\n', '\t'));
				}
			}
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (mDefaultColumn == -1) {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN));
			if (column == -1)
				mComboBoxColumn.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN));
			else
				mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(column));

			String itemString = configuration.getProperty(PROPERTY_LIST, "");
			mRadioButton.setSelected(itemString.length() != 0);
			mTextArea.setText(itemString.replace('\t', '\n'));
			mTextArea.setEnabled(itemString.length() != 0);
			}
		else {
			mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(mDefaultColumn));
			boolean isCustomOrder = (mTableModel.getCategoryCustomOrder(mDefaultColumn) != null);
			mRadioButton.setSelected(isCustomOrder);
			mList.setEnabled(isCustomOrder);
			updateList(null);
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mDefaultColumn == -1) {
			if (mComboBoxColumn.getItemCount() != 0)
				mComboBoxColumn.setSelectedIndex(0);
			mTextArea.setEnabled(false);
			}
		else {
			mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(mDefaultColumn));
			mList.setEnabled(false);
			}

		mRadioButton.setSelected(false);

		updateList(null);
		}

	private void updateList(String[] customList) {
		int column = mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem());

		if (mDefaultColumn != -1) {
			String specialType = (column == -1) ? null : mTableModel.getColumnSpecialType(column);
	        if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)
	         || CompoundTableModel.cColumnTypeRXNCode.equals(specialType)) {
	        	if (mDefaultRenderer == null)
	        		mDefaultRenderer = mList.getCellRenderer();
	
	            ChemistryCellRenderer renderer = new ChemistryCellRenderer();
	            renderer.setAlternatingRowBackground(DetailTableCellRenderer.TOGGLE_ROW_BACKGROUND);
	            mList.setCellRenderer(renderer);
	            mList.setFixedCellHeight(80);
				}
	        else {
	        	if (mDefaultRenderer != null) {
		            mList.setCellRenderer(mDefaultRenderer);
		            mList.setFixedCellHeight(-1);
	        		}
	        	}

	        mListModel.clear();
			}

		SortedStringList sortedCustomList = null;
		StringBuilder sb = (mDefaultColumn == -1) ? new StringBuilder() : null;

		if (customList != null) {
			sortedCustomList = new SortedStringList();
			for (String customItem:customList) {
				if (mDefaultColumn != -1) {
					mListModel.addElement(customItem);
					}
				else {
					if (sb.length() != 0)
						sb.append('\n');
					sb.append(customItem);
					}
				sortedCustomList.addString(customItem);
				}
			}

		if (column != -1) {
	        for (String item:mTableModel.getCategoryList(column)) {
	        	if (!item.equals(CompoundTableModel.cTextMultipleCategories)
	        	 && (sortedCustomList == null || !sortedCustomList.contains(item))) {
					if (mDefaultColumn != -1) {
						mListModel.addElement(item);
						}
					else {
						if (sb.length() != 0)
							sb.append('\n');
						sb.append(item);
						}
	        		}
	        	}
			}

		if (mDefaultColumn == -1)
			mTextArea.setText(sb.toString());
		}

	@Override
	public boolean isConfigurable() {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifies(mTableModel, column))
				return true;

		return false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
			if (column == -1) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' not found.");
		        return false;
				}
			if (!columnQualifies(mTableModel, column)) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' does not contain categories.");
		        return false;
				}
			}
		return true;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxColumn) {
			updateList(null);
			return;
			}
		if (e.getSource() == mRadioButton) {
			if (mDefaultColumn != -1)
				mList.setEnabled(mRadioButton.isSelected());
			else
				mTextArea.setEnabled(mRadioButton.isSelected());
			return;
			}
		}

	@Override
	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
		String itemString = configuration.getProperty(PROPERTY_LIST, "");
		mTableModel.setCategoryCustomOrder(column, (itemString.length() == 0) ? null : itemString.split("\\t"));
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
