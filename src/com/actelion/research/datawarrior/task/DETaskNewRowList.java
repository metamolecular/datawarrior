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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;


public class DETaskNewRowList extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "New Row List";

	public static final int MODE_SELECTED = 0;
	public static final int MODE_VISIBLE = 1;
    public static final int MODE_HIDDEN = 2;
    public static final int MODE_EMPTY = 3;
    public static final int MODE_ALL = 4;
    public static final int MODE_CLIPBOARD = 5;

	private static final String PROPERTY_ID_COLUMN = "idColumn";
	private static final String PROPERTY_EXTENSION_COLUMN = "extensionColumn";
	private static final String PROPERTY_MODE = "mode";
	private static final String PROPERTY_HITLIST_NAME = "listName";

	private static final String DEFAULT_LIST_NAME = "Unnamed Rowlist";

	private static final String[] MODE_TEXT = { "selected rows", "visible rows", "hidden rows", "no rows", "all rows", "IDs in clipboard" };
    private static final String[] MODE_CODE = { "selected", "visible", "hidden", "empty", "all", "clipboard" };

    private static Properties sRecentConfiguration;

    private CompoundTableModel mTableModel;
    private JTextField  mTextFieldHitlistName;
    private JCheckBox   mCheckBoxExtendList;
    private JComboBox   mComboBoxExtentionColumn,mComboBoxMode,mComboBoxIDColumn;
    private int         mFixedMode,mSuggestedIDColumn;

    /**
     * The fixedMode parameter may be used to predefine the configuration's mode setting.
     * If a fixedMode is given then the dialog will not show the mode selection combobox.
     * @param parent
     * @param initialMode -1 or initial mode in dialog
     */
    public DETaskNewRowList(DEFrame parent, int fixedMode) {
		super(parent, true);
        mTableModel = parent.getTableModel();
        mFixedMode = fixedMode;
        mSuggestedIDColumn = -1;
        }

	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mComboBoxMode) {
        	mCheckBoxExtendList.setEnabled(mComboBoxMode.getSelectedIndex() != MODE_EMPTY && mComboBoxMode.getSelectedIndex() != MODE_ALL);
            mComboBoxExtentionColumn.setEnabled(mCheckBoxExtendList.isEnabled() && mCheckBoxExtendList.isSelected());
            mComboBoxIDColumn.setEnabled(mComboBoxMode.getSelectedIndex() == MODE_CLIPBOARD);
            return;
	    	}
        if (e.getSource() == mCheckBoxExtendList) {
            mComboBoxExtentionColumn.setEnabled(mCheckBoxExtendList.isSelected());
            return;
	    	}
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("No rows found");
			return false;
			}
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel sp = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
		        			{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8,
								TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };
        sp.setLayout(new TableLayout(size));

        sp.add(new JLabel("Row list name:"), "1,1");
        mTextFieldHitlistName = new JTextField(16);
        sp.add(mTextFieldHitlistName, "3,1");

        if (mFixedMode == -1) {
	        sp.add(new JLabel("Create row list from:"), "1,3");
	        mComboBoxMode = new JComboBox(MODE_TEXT);
	        mComboBoxMode.addActionListener(this);
	        sp.add(mComboBoxMode, "3,3");
        	}

        if (mFixedMode == -1 || mFixedMode == MODE_CLIPBOARD) {
	        sp.add(new JLabel("Column containing IDs:"), "1,5");
	        ArrayList<String> idColumnList = new ArrayList<String>();
	        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
	            if (qualifiesAsIDColumn(column))
	            	idColumnList.add(mTableModel.getColumnTitle(column));
	        mComboBoxIDColumn = new JComboBox(idColumnList.toArray(new String[0]));
	        mComboBoxIDColumn.setEditable(mFixedMode == -1);
	        sp.add(mComboBoxIDColumn, "3,5");
        	}

        mCheckBoxExtendList = new JCheckBox("Extend list to all rows of same category", false);
        mCheckBoxExtendList.addActionListener(this);
        sp.add(mCheckBoxExtendList, "1,7,3,7");

        sp.add(new JLabel("Category column:", JLabel.RIGHT), "1,9");
        ArrayList<String> categoryColumnList = new ArrayList<String>();
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
            if (mTableModel.isColumnTypeCategory(column))
                categoryColumnList.add(mTableModel.getColumnTitle(column));
        mComboBoxExtentionColumn = new JComboBox(categoryColumnList.toArray(new String[0]));
        mComboBoxExtentionColumn.setEnabled(false);
        mComboBoxExtentionColumn.setEditable(mFixedMode == -1);
        sp.add(mComboBoxExtentionColumn, "3,9");

        return sp;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_HITLIST_NAME, mTextFieldHitlistName.getText());
		int mode = (mFixedMode != -1) ? mFixedMode : mComboBoxMode.getSelectedIndex();
		configuration.setProperty(PROPERTY_MODE, MODE_CODE[mode]);
		if (mode == MODE_CLIPBOARD && mComboBoxIDColumn.getSelectedItem() != null)
			configuration.setProperty(PROPERTY_ID_COLUMN, mTableModel.getColumnTitleNoAlias(
					(String)mComboBoxIDColumn.getSelectedItem()));
        if (mCheckBoxExtendList.isSelected())
        	configuration.setProperty(PROPERTY_EXTENSION_COLUMN, mTableModel.getColumnTitleNoAlias(
        			(String)mComboBoxExtentionColumn.getSelectedItem()));
        return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_SELECTED);
		if (mFixedMode == -1) {	// no fixed mode -> we edit a task as part of a sequence
			mComboBoxMode.setSelectedIndex(mode);
			mTextFieldHitlistName.setText(configuration.getProperty(PROPERTY_HITLIST_NAME, DEFAULT_LIST_NAME));
			}
		else {
			String listName = (mode != mFixedMode) ? MODE_TEXT[mFixedMode] : configuration.getProperty(PROPERTY_HITLIST_NAME, DEFAULT_LIST_NAME);
			mTextFieldHitlistName.setText(mTableModel.getHitlistHandler().getUniqueName(listName));
			}

		String extensionColumn = configuration.getProperty(PROPERTY_EXTENSION_COLUMN, "");
		if (extensionColumn.length() == 0) {
			mCheckBoxExtendList.setSelected(false);
            mComboBoxExtentionColumn.setEnabled(false);
			}
		else {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_EXTENSION_COLUMN));
			if (column != -1 && mTableModel.isColumnTypeCategory(column)) {
				mCheckBoxExtendList.setSelected(true);
				mComboBoxExtentionColumn.setSelectedItem(mTableModel.getColumnTitle(column));
	            mComboBoxExtentionColumn.setEnabled(true);

				}
			else if (mFixedMode == -1) {
				mCheckBoxExtendList.setSelected(true);
				mComboBoxExtentionColumn.setSelectedItem(extensionColumn);
	            mComboBoxExtentionColumn.setEnabled(true);
				}
			}

		if (mComboBoxIDColumn != null) {
			String idColumn = configuration.getProperty(PROPERTY_ID_COLUMN, "");
			if (idColumn.length() != 0) {
				int column = mTableModel.findColumn(idColumn);
				if (column != -1 && qualifiesAsIDColumn(column))
					mComboBoxIDColumn.setSelectedItem(mTableModel.getColumnTitle(column));
				else if (mFixedMode == -1)
					mComboBoxIDColumn.setSelectedItem(idColumn);
				else
					selectSuggestedItemOfComboBoxIDColumn();
				}
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mFixedMode == -1)
			mComboBoxMode.setSelectedIndex(MODE_SELECTED);
		mTextFieldHitlistName.setText((mFixedMode == -1) ? DEFAULT_LIST_NAME : MODE_TEXT[mFixedMode]);
		if (mComboBoxIDColumn != null) {
			if (mFixedMode != -1)
				selectSuggestedItemOfComboBoxIDColumn();
			else if (mComboBoxIDColumn.getItemCount() != 0)
				mComboBoxIDColumn.setSelectedIndex(0);
			}
		mCheckBoxExtendList.setSelected(false);
        mComboBoxExtentionColumn.setEnabled(false);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (configuration.getProperty(PROPERTY_HITLIST_NAME, "").length() == 0) {
			showErrorMessage("No list name specified.");
			return false;
			}

		if (isLive) {
			String columnName = configuration.getProperty(PROPERTY_EXTENSION_COLUMN);
			if (columnName != null) {
				if (mTableModel.findColumn(columnName) == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				}

			if (MODE_CODE[MODE_CLIPBOARD].equals(configuration.getProperty(PROPERTY_MODE))) {
				TreeSet<String> keySet = analyzeClipboard();
				if (keySet == null) {
					showErrorMessage("The clipboard is empty.");
					return false;
					}
				String idColumnName = configuration.getProperty(PROPERTY_ID_COLUMN);
				if (mTableModel.findColumn(idColumnName) == -1 && suggestIDColumn() == -1) {
					showErrorMessage("A column containing IDs was not defined nor found\n"
							+"and no column exists that matches any clipboard content.");
					return false;
					}
				}
			}
		
		return true;
		}

	private void selectSuggestedItemOfComboBoxIDColumn() {
		int column = suggestIDColumn();
		if (column != -1)
        	mComboBoxIDColumn.setSelectedItem(mTableModel.getColumnTitle(column));
		}

	private boolean qualifiesAsIDColumn(int column) {
		return mTableModel.isColumnTypeString(column) && mTableModel.getColumnSpecialType(column) == null;
		}

	/**
	 * Finds the column that has the highest number of clipboard content matches.
	 * @return -1 or valid column index
	 */
	private int suggestIDColumn() {
		if (mSuggestedIDColumn == -1) {
			TreeSet<String> keySet = analyzeClipboard();
			if (keySet != null) {
	        	int maxMatchCount = 0;
	            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
	                if (qualifiesAsIDColumn(column)) {
	                	int matchCount = 0;
	                	for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
	                        String[] entry = mTableModel.separateEntries(mTableModel.getTotalValueAt(row, column));
	                        for (int i=0; i<entry.length; i++) {
	                    		if (entry[i].length() > 0 && keySet.contains(entry[i])) {
	                    			matchCount++;
	                        		break;
	                    			}
	                			}
	                		}
	                	if (matchCount != 0) {
	                    	if (maxMatchCount < matchCount) {
	                    		maxMatchCount = matchCount;
	                    		mSuggestedIDColumn = column;
	                    		}
	                		}
	                	}
	                }
				if (mSuggestedIDColumn == -1)
					mSuggestedIDColumn = -2;	// mark that analysis has been done
				}
			}
		return (mSuggestedIDColumn == -2) ? -1 : mSuggestedIDColumn;
		}

	/**
	 * Analyzes the clipboard content and sets mKeySet, mListName, mKeyColumn
	 * @return set of unique IDs found in clipboard or null, if clipboard is empty
	 */
	private TreeSet<String> analyzeClipboard() {
		TreeSet<String> keySet = null;

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable theData = clipboard.getContents(this);
		String s;
		try {
			s = (String)(theData.getTransferData(DataFlavor.stringFlavor));
			BufferedReader theReader = new BufferedReader(new StringReader(s));

			keySet = new TreeSet<String>();
	        String key = theReader.readLine();
	        while (key != null) {
	        	keySet.add(key);
	            key = theReader.readLine();
	            }

            theReader.close();
    		return keySet.isEmpty() ? null : keySet;
			}
   		catch (Exception e) {
   			return null;
   			}
		}

	@Override
	public void runTask(Properties configuration) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_SELECTED);
		int hitlistMode = (mode == MODE_SELECTED)	? CompoundTableHitlistHandler.FROM_SELECTED
						: (mode == MODE_VISIBLE)	? CompoundTableHitlistHandler.FROM_VISIBLE
						: (mode == MODE_HIDDEN)		? CompoundTableHitlistHandler.FROM_HIDDEN
						: (mode == MODE_CLIPBOARD)	? CompoundTableHitlistHandler.FROM_KEY_SET
						: (mode == MODE_EMPTY)		? CompoundTableHitlistHandler.EMPTY_LIST
						:							  CompoundTableHitlistHandler.ALL_IN_LIST;
						
		String name = configuration.getProperty(PROPERTY_HITLIST_NAME);
		int extensionColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_EXTENSION_COLUMN));

		TreeSet<String> keySet = analyzeClipboard();
		int keyColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_ID_COLUMN));
		if (keyColumn == -1)
			keyColumn = suggestIDColumn();

		if (mTableModel.getHitlistHandler().createHitlist(name, extensionColumn, hitlistMode, keyColumn, keySet) == null)
			showErrorMessage("The maximum number of filters/lists is reached.");
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
