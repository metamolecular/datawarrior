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

package com.actelion.research.datawarrior.task.file;

import info.clearthought.layout.TableLayout;

import java.io.File;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.CompoundTableSaver;

public class DETaskSaveSDFileAs extends DETaskAbstractSaveFile {
    public static final String TASK_NAME = "Save SD-File";

	private static Properties sRecentConfiguration;
	private static final String PROPERTY_SD_VERSION = "version";
	private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
	private static final String PROPERTY_COORDINATE_MODE = "coordinates";
	private static final String[] SD_VERSION_OPTIONS = { "Version 2", "Version 3" };
	private static final String[] SD_VERSION_CODE = { "v2", "v3" };
	private static final String[] COORDINATE_OPTIONS = { "2D", "3D if available" };
	private static final String[] COORDINATE_CODE = { "2D", "prefer3D" };
	private static final int INDEX_PREFER_3D = 1;
	private static final int INDEX_VERSION_3 = 1;
	private static final String OPTION_NO_STRUCTURE = "<none>";

	private Properties mPredefinedConfiguration;
	private JComboBox mComboBoxVersion,mComboBoxStructureColumn,mComboBoxCoordinateMode;

	/**
	 * The logic of this task is different from its parent class DETaskAbstractSaveFile:<br>
	 * - If invoked interactively:<br>
	 * super.getPredefinedConfiguration() is called to show a file selection dialog and to
	 * create a configuration from it. <b>null</b> is returned to also show a configuration dialog.
	 * However, createDialogContent() is overridden to only show the inner dialog content.
	 * getDialogConfiguration() returns inner dialog settings plus the predefined path.<br>
	 * - If invoked as part of a macro:<br>
	 * The behaviour is standard, i.e. the dialog shows outer and inner dialog to configure
	 * the entire task.
	 * @param parent
	 * @param isVersion3 only used, if is interactive
	 * @param isInteractive
	 */
	public DETaskSaveSDFileAs(DEFrame parent, boolean isInteractive) {
		super(parent, "", isInteractive);
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
	public int getFileType() {
		return CompoundFileHelper.cFileTypeSD;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {
			// this requests interactively the file & path from the user and puts it into a properties object
			mPredefinedConfiguration = super.getPredefinedConfiguration();
			if (mPredefinedConfiguration.getProperty(PROPERTY_FILENAME) == null)
				return mPredefinedConfiguration;	// suppress showing follow-up dialog if the file dialog was cancelled
			}

		return null;	// show a configuration dialog
		}

	@Override
	public JPanel createDialogContent() {
		// special handling with SD-files: 
		if (isInteractive()) {
			return createInnerDialogContent();
			}
		else {
			return super.createDialogContent();
			}
		}

	@Override
	public JPanel createInnerDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

		int[] columnList = getTableModel().getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);
		p.add(new JLabel("Structure column:"), "1,1");
		mComboBoxStructureColumn = new JComboBox();
		if (columnList != null)
			for (int column:columnList)
				mComboBoxStructureColumn.addItem(getTableModel().getColumnTitle(column));
		mComboBoxStructureColumn.addItem(OPTION_NO_STRUCTURE);
		mComboBoxStructureColumn.setEditable(!isInteractive());
		p.add(mComboBoxStructureColumn, "3,1");
		
		p.add(new JLabel("SD-file version:"), "1,3");
		mComboBoxVersion = new JComboBox(SD_VERSION_OPTIONS);
		p.add(mComboBoxVersion, "3,3");

		p.add(new JLabel("Atom coordinates:"), "1,5");
		mComboBoxCoordinateMode = new JComboBox(COORDINATE_OPTIONS);
		p.add(mComboBoxCoordinateMode, "3,5");

		return p;
		}

/*	private void enableCoordinateMenu() {
	    for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
	        String specialType = mTableModel.getColumnSpecialType(column);
	        if (specialType != null
	         && structureColumn.equals(mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyParentColumn))) {
	            if (specialType.equals(CompoundTableModel.cColumnType2DCoordinates))
	                mSDColumn2DCoordinates = column;
	            else if (specialType.equals(CompoundTableModel.cColumnType3DCoordinates))
	                mSDColumn3DCoordinates = column;
	            }
	        }
	    if (mSDColumn3DCoordinates != -1) {
	        int option = JOptionPane.showOptionDialog(mParentFrame, "Where applicable, what do you prefer?",
	                "Coordinate Selection", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
	                null, new Object[] { "2D-Coordinates", "3D-Coordinates"}, "2D-Coordinates");
	        if (option == JOptionPane.CLOSED_OPTION) {
				finalStatus(null);
				return;
	        	}
	        mPrefer3D = (option == 1);
	        }
		}*/

	@Override
	public void setDialogConfigurationToDefault() {
		if (!isInteractive())
			super.setDialogConfigurationToDefault();
		mComboBoxStructureColumn.setSelectedIndex(0);
		mComboBoxVersion.setSelectedIndex(1);
		mComboBoxCoordinateMode.setSelectedIndex(0);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (!isInteractive())
			super.setDialogConfiguration(configuration);
		mComboBoxStructureColumn.setSelectedItem(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, (String)mComboBoxStructureColumn.getItemAt(0)));
		mComboBoxVersion.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SD_VERSION), SD_VERSION_CODE, 1));
		mComboBoxCoordinateMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_COORDINATE_MODE), COORDINATE_CODE, 1));
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = isInteractive() ? mPredefinedConfiguration : super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, (String)mComboBoxStructureColumn.getSelectedItem());
		configuration.setProperty(PROPERTY_SD_VERSION, SD_VERSION_CODE[mComboBoxVersion.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_COORDINATE_MODE, COORDINATE_CODE[mComboBoxVersion.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public boolean isConfigurable() {
		if (getTableModel().getTotalRowCount() == 0
		 || getTableModel().getTotalColumnCount() == 0) {
			showErrorMessage("Empty documents cannot be saved.");
			return false;
			}
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnTitle = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, OPTION_NO_STRUCTURE);
		if (isLive && !columnTitle.equals(OPTION_NO_STRUCTURE)) {
			int column = getTableModel().findColumn(columnTitle);
			if (column == -1) {
				showErrorMessage("Column '"+columnTitle+"' not found.");
				return false;
				}
			if (!getTableModel().getColumnSpecialType(column).equals(CompoundTableConstants.cColumnTypeIDCode)) {
				showErrorMessage("Column '"+columnTitle+"' doesn't contain chemical structures.");
				return false;
				}
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public void saveFile(File file, Properties configuration) {
		String columnTitle = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, OPTION_NO_STRUCTURE);
		int column = columnTitle.equals(OPTION_NO_STRUCTURE) ? -1 : getTableModel().findColumn(columnTitle);
		boolean prefer3D = COORDINATE_CODE[INDEX_PREFER_3D].equals(configuration.getProperty(PROPERTY_COORDINATE_MODE));
		boolean version3 = SD_VERSION_CODE[INDEX_VERSION_3].equals(configuration.getProperty(PROPERTY_SD_VERSION));
		int fileType = version3 ? CompoundFileHelper.cFileTypeSDV3 : CompoundFileHelper.cFileTypeSDV2;
		CompoundTableModel tableModel = ((DEFrame)getParentFrame()).getMainFrame().getTableModel();
		JTable table = ((DEFrame)getParentFrame()).getMainFrame().getMainPane().getTable();
		new CompoundTableSaver(getParentFrame(), tableModel, table).saveSDFile(file,  fileType, column, prefer3D);
		}
	}
