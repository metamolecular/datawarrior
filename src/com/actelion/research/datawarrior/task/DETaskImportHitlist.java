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

import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.file.DETaskAbstractOpenFile;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableLoader;
import com.actelion.research.table.CompoundTableModel;

public class DETaskImportHitlist extends DETaskAbstractOpenFile implements ActionListener {
	private static final String PROPERTY_LISTNAME = "listName";
	private static final String PROPERTY_KEYCOLUMN = "keyColumn";

	public static final String TASK_NAME = "Import Row List";

    private static Properties sRecentConfiguration;

    private CompoundTableModel mTableModel;
    private String mKeyColumnName,mListName;
    private String[] mPossibleKeyColumn;
    private TreeSet<String> mKeySet;
    private JTextField mFieldHitlistName;
    private JComboBox mComboBox;
    private Properties mCurrentDialogConfiguration;

	public DETaskImportHitlist(DEFrame parent, boolean isInteractive) {
		super(parent, "Open Row List File", FileHelper.cFileTypeTextTabDelimited, isInteractive);
        mTableModel = parent.getTableModel();
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
    public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 4, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Column containing key values:"), "0,0");
		mComboBox = new JComboBox(createDefaultColumns());
		mComboBox.setEditable(!isInteractive());
		content.add(mComboBox, "2,0");

		mFieldHitlistName = new JTextField("imported list");
		content.add(new JLabel("Name of new row list:"), "0,2");
		content.add(mFieldHitlistName, "2,2");

		return content;
    	}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isLive) {
			String fileName = configuration.getProperty(PROPERTY_FILENAME);
			String error = analyzeHitlist(new File(fileName));
			if (error != null) {
				showErrorMessage(error);
				return false;
				}
			}
    	return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();

		mCurrentDialogConfiguration = null;
		fileChanged(null);
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		mCurrentDialogConfiguration = configuration;

		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		File file = (fileName == null) ? null : new File(fileName);

		if (file == null || (isInteractive() && !file.exists()))
			file = askForFile(fileName);

		fileChanged(file);
		}

    public Properties getDialogConfiguration() {
    	Properties configuration = super.getDialogConfiguration();

    	String keyColumn = (String)mComboBox.getSelectedItem();
    	if (keyColumn != null)
    		configuration.setProperty(PROPERTY_KEYCOLUMN, keyColumn);

    	String listName = mFieldHitlistName.getText();
    	if (listName != null)
    		configuration.setProperty(PROPERTY_LISTNAME, listName);

    	return configuration;
    	}

	/**
	 * If the file is valid, analyze file and configure all UI elements
	 * according to file content. Then preselect options according to
	 * current dialog configuration.
	 * @param file or null
	 */
	@Override
	protected void fileChanged(File file) {
		String error = null;
		if (file != null && file.exists()) {
			error = analyzeHitlist(file);
			if (error == null) {
				mComboBox.removeAllItems();
				for (String item:mPossibleKeyColumn)
					mComboBox.addItem(item);

				int keyColumn = mTableModel.findColumn(mKeyColumnName);
				if (keyColumn != -1)
					mComboBox.setSelectedItem(mTableModel.getColumnTitle(keyColumn));

				mFieldHitlistName.setText(mListName);
				}
			else {
	            showErrorMessage(error);
				}
    		}

		boolean fileIsValid = (file != null && file.exists() && error == null);

		if (!fileIsValid) {
            mComboBox.removeAllItems();
			for (String item:createDefaultColumns())
				mComboBox.addItem(item);

			if (mCurrentDialogConfiguration != null) {
				String keyColumn = mCurrentDialogConfiguration.getProperty(PROPERTY_KEYCOLUMN);
				if (keyColumn != null)
					mComboBox.setSelectedItem(keyColumn);
		
				String listName = mCurrentDialogConfiguration.getProperty(PROPERTY_LISTNAME);
				if (listName != null)
					mFieldHitlistName.setText(listName);
				}
			}
		}

	/**
	 * Creates a list of all displayable columns as potential key column.
	 * @return the list
	 */
	private String[] createDefaultColumns() {
		ArrayList<String> columnList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnDisplayable(column))
				columnList.add(mTableModel.getColumnTitle(column));

		return columnList.toArray(new String[0]);
		}

	/**
	 * Read and analyzes a hitlist file and sets mKeySet, mListName, mKeyColumn
	 * @param file
	 * @return error message or null
	 */
	private String analyzeHitlist(File file) {
		mListName = null;
		mKeyColumnName = null;
		BufferedReader theReader = null;
		try {
            theReader = new BufferedReader(new FileReader(file));

            String hitlistNameLine = theReader.readLine();
            mListName = (hitlistNameLine != null
                      && hitlistNameLine.startsWith("<hitlistName=")) ?
                        CompoundTableLoader.extractValue(hitlistNameLine) : null;
            String keyColumnLine = theReader.readLine();
            mKeyColumnName = (keyColumnLine != null
                       && keyColumnLine.startsWith("<keyColumn=")) ?
                        CompoundTableLoader.extractValue(keyColumnLine) : null;
			}
		catch (IOException ioe) {}

		boolean isHitlistFile = (mListName != null && mKeyColumnName != null);

		try {
			if (theReader != null)
				theReader.close();

			theReader = new BufferedReader(new FileReader(file));

            mKeySet = readKeys(theReader);
            theReader.close();

            if (mKeySet.isEmpty())
                return "No keys were found in file '"+file.getName()+"'.";

            if (!isHitlistFile) {
            	for (String key:mKeySet)
            		if (key.indexOf('\t') != -1)
                        return "The file '"+file.getName()+"' cannot be used as row list file.";

            	mListName = "imported list";
                }

        	String maxMatchColumn = null;
        	int maxMatchCount = 0;
            ArrayList<String> columnList = new ArrayList<String>();
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.isColumnDisplayable(column)) {
                	int matchCount = 0;
                	for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                        String[] entry = mTableModel.separateEntries(mTableModel.getTotalValueAt(row, column));
                        for (int i=0; i<entry.length; i++) {
                    		if (entry[i].length() > 0 && mKeySet.contains(entry[i])) {
                    			matchCount++;
                        		break;
                    			}
                			}
                		}
                	if (matchCount != 0) {
                        columnList.add(mTableModel.getColumnTitle(column));
                    	if (maxMatchCount < matchCount) {
                    		maxMatchCount = matchCount;
                    		maxMatchColumn = mTableModel.getColumnTitle(column);
                    		}
                		}
                	}
                }
            if (maxMatchCount == 0)
                return "None of the keys in file '"+file.getName()+"' is present in any data column.";

            if (mKeyColumnName == null || !columnList.contains(mKeyColumnName))
            	mKeyColumnName = maxMatchColumn;

            mPossibleKeyColumn = columnList.toArray(new String[0]);
            Arrays.sort(mPossibleKeyColumn);
            return null;
			}
		catch (IOException ioe) {
            return "Couldn't read file: "+ioe;
            }
		}

	@Override
	public boolean isConfigurable() {
		if (!super.isConfigurable())
			return false;

		if (mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("Cannot import row list if there are no rows.");
			return false;
			}

		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		startProgress("Creating row list", 0, 0);

		String keyColumnName = configuration.getProperty(PROPERTY_KEYCOLUMN);
		boolean found = false;
		for (String key:mPossibleKeyColumn) {
			if (key.equals(keyColumnName)) {
				found = true;
				break;
				}
			}
		if (found)
			mKeyColumnName = keyColumnName;	// otherwise take the default created by analyzeHitlist()
		
		String listName = configuration.getProperty(PROPERTY_LISTNAME);
		if (listName != null)
			mListName = listName;	// otherwise take the default created by analyzeHitlist()

		int keyColumn = mTableModel.findColumn(mKeyColumnName);
        if (mTableModel.getHitlistHandler().createHitlist(mListName, -1, CompoundTableHitlistHandler.FROM_KEY_SET, keyColumn, mKeySet) == null)
			showErrorMessage("Row list '"+mListName+"' could not be created, because\n"
							+"the maximum number of filters/lists is reached.");

        return null;
		}

	private TreeSet<String> readKeys(BufferedReader theReader) throws IOException {
        TreeSet<String> keySet = new TreeSet<String>();
        String key = theReader.readLine();
        while (key != null) {
            keySet.add(key);
            key = theReader.readLine();
            }
        return keySet;
		}
	}
