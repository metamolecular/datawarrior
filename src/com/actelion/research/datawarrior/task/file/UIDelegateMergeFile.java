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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableLoader;
import com.actelion.research.table.CompoundTableModel;

public class UIDelegateMergeFile implements ActionListener,ItemListener,TaskUIDelegate,TaskConstantsMergeFile {
	private static final int IS_NOT_DISPLAYABLE = -1;
	private static final int IS_NORMAL_DISPLAYABLE = 0;
	private static final int COLUMN_LINE_SPACING = 4;
	private static final int DESTINATION_ITEM_TRASH = 1;
	private static final String[] DESTINATION_ITEMS = { "<new column>", "<trash it>" };
	private static final String[] DESTINATION_CODES = { DEST_COLUMN_ADD, DEST_COLUMN_TRASH };

	private DEFrame				mParentFrame;
	private DETaskMergeFile		mParentTask;
	private CompoundTableModel  mTableModel;
	private JPanel				mDialogPanel;
	private JComponent			mMatchingPanel;
	private JFilePathLabel		mFilePathLabel;
	private JLabel[]			mLabelSourceColumn;
	private JComboBox[]			mComboBoxUsage;
	private JComboBox[]			mComboBoxOldColumn;
	private JCheckBox			mCheckBoxAppendRows;
	private CompoundTableLoader	mLoader;
	private String[]			mTotalFieldName;
	private boolean 			mIsInteractive;
	private int					mFieldCount;

	public UIDelegateMergeFile(DEFrame parent, DETaskMergeFile parentTask, boolean isInteractive) {
		mParentFrame = parent;
		mParentTask = parentTask;
		mTableModel = parent.getTableModel();
		mIsInteractive = isInteractive;
		}

	@Override
	public JComponent createDialogContent() {
		File file = null;
		if (mIsInteractive) {
			file = askForFile(null);
			if (file == null)
				return null;
			}

		double[][] size1 = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, 8},
							 {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		mDialogPanel = new JPanel();
		mDialogPanel.setLayout(new TableLayout(size1));

		mFilePathLabel = new JFilePathLabel(!mIsInteractive);
		mFilePathLabel.setListener(this);
		JButton buttonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		buttonEdit.addActionListener(this);

		mDialogPanel.add(new JLabel("File:"), "1,1");
		mDialogPanel.add(mFilePathLabel, "3,1");
		mDialogPanel.add(buttonEdit, "5,1");

//		JPanel cbp = new JPanel();
		mCheckBoxAppendRows = new JCheckBox("Append rows not existing in current data", true);
//		cbp.add(mCheckBoxAppendRows);

		if (mIsInteractive)
			updateUIFromFile(file);

		mDialogPanel.add(mCheckBoxAppendRows, "1,5,5,5");

		return mDialogPanel;
		}

	private void updateUIFromFile(File file) {
		mFilePathLabel.setPath(file.getAbsolutePath());

		// parse the file to be merged
		int fileType = FileHelper.getFileType(file.getName());
		mLoader = new CompoundTableLoader(mParentFrame, mParentFrame.getTableModel(), null);
		mLoader.readFile(file, new DERuntimeProperties(mParentFrame.getMainFrame()), fileType, CompoundTableLoader.READ_DATA);

		// create arrays with field names
		mTotalFieldName = mLoader.getFieldNames();
		ArrayList<String> visibleFieldList = new ArrayList<String>();
		for (int i=0; i<mTotalFieldName.length; i++) {
			int displayableType = getDisplayableType(mLoader.getColumnSpecialType(mTotalFieldName[i]));
			if (displayableType != IS_NOT_DISPLAYABLE)
				visibleFieldList.add(mTotalFieldName[i]);
			}
		mFieldCount = visibleFieldList.size();

		// From here: create the panel with all field matching options
		double[] verticalSize = new double[2*mFieldCount+3];
		verticalSize[0] = 8;
		verticalSize[1] = TableLayout.PREFERRED;
		verticalSize[2] = 8;
		for (int i=3; i<verticalSize.length; i+=2) {
			verticalSize[i] = TableLayout.PREFERRED;
			verticalSize[i+1] = COLUMN_LINE_SPACING;
			}
		double[][] size2 = { { 8, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8 }, verticalSize };

			// create lists of potential mapping columns for these column types:
			// non-special and all parent special types (idcode, rxncode, <more?>)
		@SuppressWarnings("unchecked")
		ArrayList<String>[] columnListBySpecialType = new ArrayList[1+CompoundTableModel.cParentSpecialColumnTypes.length];
		for (int i=0; i<=CompoundTableModel.cParentSpecialColumnTypes.length; i++) {
			columnListBySpecialType[i] = new ArrayList<String>();
			for (String item:DESTINATION_ITEMS)
				columnListBySpecialType[i].add(item);
			}
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			int displayableType = getDisplayableType(mTableModel.getColumnSpecialType(column));
			if (displayableType != IS_NOT_DISPLAYABLE)
				columnListBySpecialType[displayableType].add(mTableModel.getColumnTitle(column));
			}

		mLabelSourceColumn = new JLabel[mFieldCount];
		mComboBoxOldColumn = new JComboBox[mFieldCount];
		mComboBoxUsage = new JComboBox[mFieldCount];

		JPanel mp0 = new JPanel();
		mp0.setLayout(new TableLayout(size2));
		mp0.add(new JLabel("New Column"), "1,1,3,1");
		mp0.add(new JLabel("Current Column"), "4,1");
		mp0.add(new JLabel("Merge Option"), "6,1");
		int layoutPosition = 3;
		boolean selectedFound = false;
		for (int i=0; i<mFieldCount; i++) {
			mLabelSourceColumn[i] = new JLabel(visibleFieldList.get(i));

			int displayableType = getDisplayableType(mLoader.getColumnSpecialType(visibleFieldList.get(i)));
			mComboBoxOldColumn[i] = new JComboBox(columnListBySpecialType[displayableType].toArray());
			for (int j=2; j<columnListBySpecialType[displayableType].size(); j++)
				if (visibleFieldList.get(i).equalsIgnoreCase(columnListBySpecialType[displayableType].get(j)))
					mComboBoxOldColumn[i].setSelectedIndex(j);
			mComboBoxOldColumn[i].addItemListener(this);
			mComboBoxOldColumn[i].setEditable(!mIsInteractive);

			boolean selected = !selectedFound && mComboBoxOldColumn[i].getSelectedIndex() > 1;
			if (selected)
				selectedFound = true;

			mComboBoxUsage[i] = new JComboBox(OPTION_TEXT);
			mComboBoxUsage[i].setSelectedIndex(selected ? CompoundTableLoader.MERGE_MODE_IS_KEY
				 : (displayableType != IS_NORMAL_DISPLAYABLE) ? CompoundTableLoader.MERGE_MODE_USE_IF_EMPTY : CompoundTableLoader.MERGE_MODE_APPEND);
			mComboBoxUsage[i].setEnabled(mComboBoxOldColumn[i].getSelectedIndex() > 1);
			mComboBoxUsage[i].addItemListener(this);

			mp0.add(new JLabel("Assign "), "1,"+layoutPosition);
			mp0.add(mLabelSourceColumn[i], "2,"+layoutPosition);
			mp0.add(new JLabel(" to"), "3,"+layoutPosition);
			mp0.add(mComboBoxOldColumn[i], "4,"+layoutPosition);
			mp0.add(mComboBoxUsage[i], "6,"+layoutPosition);
			layoutPosition += 2;
			}

		if (mMatchingPanel != null)
			mDialogPanel.remove(mMatchingPanel);

		if (mFieldCount <= 8) {
			mMatchingPanel = mp0;
			}
		else {
			JScrollPane mps = new JScrollPane(mp0, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
				private static final long serialVersionUID = 0x20060904;
				@Override
				public Dimension getPreferredSize() {
					return new Dimension(getViewport().getView().getPreferredSize().width+16,
							8*(COLUMN_LINE_SPACING+mComboBoxUsage[0].getPreferredSize().height));
					}
				};
			mMatchingPanel = mps;
			}

		mDialogPanel.add(mMatchingPanel, "1,3,5,3");
		mDialogPanel.validate();

		Component c = mDialogPanel.getParent();
		while (c != null && !(c instanceof JDialog))
			c = c.getParent();

		if (c != null)
			((JDialog)c).pack();
		}

	private int getDisplayableType(String specialType) {
		if (specialType == null)
			return IS_NORMAL_DISPLAYABLE;

		for (int i=0; i<CompoundTableModel.cParentSpecialColumnTypes.length; i++)
			if (specialType.equals(CompoundTableModel.cParentSpecialColumnTypes[i]))
				return i+1;

		return IS_NOT_DISPLAYABLE;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			File file = askForFile(mParentTask.resolveVariables(mFilePathLabel.getPath()));
			if (file != null) {
				updateUIFromFile(file);
				}
			return;
			}
		if (e.getSource() == mFilePathLabel) {
			updateUIFromFile(new File(mParentTask.resolveVariables(mFilePathLabel.getPath())));
			return;
			}
		}

	private File askForFile(String selectedFile) {
		return new FileHelper(mParentFrame).selectFileToOpen(
				"Merge DataWarrior-, SD- or Text-File", FileHelper.cFileTypeDataWarriorCompatibleData, selectedFile);
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			int comboBoxType = -1;
			int index = 0;
			while (index<mFieldCount) {
				if (e.getSource() == mComboBoxOldColumn[index]) {
					comboBoxType = 1;
					break;
					}
				if (e.getSource() == mComboBoxUsage[index]) {
					comboBoxType = 2;
					break;
					}
				index++;
				}

			if (comboBoxType == 1) {	// column assignment changed
				boolean isDestinationItem = false;
				for (String s:DESTINATION_ITEMS) {
					if (s.equalsIgnoreCase((String)mComboBoxOldColumn[index].getSelectedItem())) {
						isDestinationItem = true;
						break;
						}
					}
				JComboBox cbUsage = mComboBoxUsage[index];
				if (isDestinationItem
				 && OPTION_TEXT[CompoundTableLoader.MERGE_MODE_IS_KEY].equals(cbUsage.getSelectedItem())) {
					if (cbUsage.getSelectedIndex() == CompoundTableLoader.MERGE_MODE_IS_KEY) {
						String newColumnName = mLabelSourceColumn[index].getText();
						int displayableType = getDisplayableType(mLoader.getColumnSpecialType(newColumnName));
						cbUsage.removeItemListener(this);
						cbUsage.setSelectedIndex((displayableType != IS_NORMAL_DISPLAYABLE) ? CompoundTableLoader.MERGE_MODE_USE_IF_EMPTY : CompoundTableLoader.MERGE_MODE_APPEND);
						cbUsage.addItemListener(this);
						}
					}
				cbUsage.setEnabled(mComboBoxOldColumn[index].getSelectedIndex() > 1);
				}
			if (comboBoxType == 2) {	// column role changed
				if (OPTION_TEXT[CompoundTableLoader.MERGE_MODE_APPEND].equals(e.getItem())) {
					String newColumnName = mLabelSourceColumn[index].getText();
					int displayableType = getDisplayableType(mLoader.getColumnSpecialType(newColumnName));
					if (displayableType != IS_NORMAL_DISPLAYABLE) {
						JOptionPane.showMessageDialog(mParentFrame, "Column '"+newColumnName+"' contains a special type and cannot be appended.");
						mComboBoxUsage[index].removeItemListener(this);
						mComboBoxUsage[index].setSelectedIndex(CompoundTableLoader.MERGE_MODE_USE_IF_EMPTY);
						mComboBoxUsage[index].addItemListener(this);
						return;
						}
					}
/*	multiple merge keys are now allowed. 14-Nov-2014
				else if (OPTION_TEXT[CompoundTableLoader.MERGE_MODE_IS_KEY].equals(e.getItem())) {
					for (int i=0; i<mFieldCount; i++) {
						JComboBox cb = mComboBoxUsage[i];
						if (cb != e.getSource() && cb.getSelectedIndex() == CompoundTableLoader.MERGE_MODE_IS_KEY) {
							String newColumnName = mLabelSourceColumn[i].getText();
							int displayableType = getDisplayableType(mLoader.getColumnSpecialType(newColumnName));
							cb.removeItemListener(this);
							cb.setSelectedIndex((displayableType != IS_NORMAL_DISPLAYABLE) ? CompoundTableLoader.MERGE_MODE_USE_IF_EMPTY : CompoundTableLoader.MERGE_MODE_APPEND);
							cb.addItemListener(this);
							}
						}
					}	*/
				}
			}
		}

	public CompoundTableLoader getCompoundTableLoader() {
		return mLoader;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String fileName = mFilePathLabel.getPath();
		if (fileName != null)
			configuration.setProperty(PROPERTY_FILENAME, fileName);

		configuration.setProperty(PROPERTY_COLUMN_COUNT, Integer.toString(mFieldCount));
		for (int i=0; i<mFieldCount; i++) {
			configuration.setProperty(PROPERTY_SOURCE_COLUMN+i, mLabelSourceColumn[i].getText());
			String destColumn = (mComboBoxOldColumn[i].getSelectedIndex() < DESTINATION_ITEMS.length) ?
					DESTINATION_CODES[mComboBoxOldColumn[i].getSelectedIndex()] : (String)mComboBoxOldColumn[i].getSelectedItem();
			configuration.setProperty(PROPERTY_DEST_COLUMN+i, destColumn);
			configuration.setProperty(PROPERTY_OPTION+i, OPTION_CODE[mComboBoxUsage[i].getSelectedIndex()]);
			}

		configuration.setProperty(PROPERTY_APPEND_ROWS, mCheckBoxAppendRows.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (!mIsInteractive) {
			String fileName = configuration.getProperty(PROPERTY_FILENAME);
			File file = (fileName == null) ? null : new File(fileName);
			if (file.exists())
				updateUIFromFile(file);
			}

		try {
			int count = Integer.parseInt(configuration.getProperty(PROPERTY_COLUMN_COUNT, "0"));
			boolean[] fieldFound = new boolean[mFieldCount];
			for (int i=0; i<count; i++) {
				String sourceColumn = configuration.getProperty(PROPERTY_SOURCE_COLUMN+i);
				String destColumn = configuration.getProperty(PROPERTY_DEST_COLUMN+i);
				boolean isDestinationCode = false;
				for (int j=0; j<DESTINATION_CODES.length; j++) {
					if (DESTINATION_CODES[j].equals(destColumn)) {
						destColumn = DESTINATION_ITEMS[j];
						isDestinationCode = true;
						break;
						}
					}
				if (mIsInteractive) {	// use column alias, if available
					if (!isDestinationCode) {
						int column = mParentFrame.getTableModel().findColumn(destColumn);
						if (column != -1)
							destColumn = mParentFrame.getTableModel().getColumnTitle(column);
						}
					}
				String usage = configuration.getProperty(PROPERTY_OPTION+i);

				for (int field=0; field<mFieldCount; field++) {
					String name = mLabelSourceColumn[field].getText();
					if (name.equals(sourceColumn)) {
						mComboBoxOldColumn[field].setSelectedItem(destColumn);
						mComboBoxUsage[field].setSelectedIndex(AbstractTask.findListIndex(usage, OPTION_CODE, 1));
						fieldFound[field] = true;
						break;
						}
					}
				}
			for (int field=0; field<mFieldCount; field++)
				if (!fieldFound[field])
					mComboBoxOldColumn[field].setSelectedItem(DESTINATION_ITEMS[DESTINATION_ITEM_TRASH]);
			}
		catch (NumberFormatException nfe) {}

		mCheckBoxAppendRows.setSelected("true".equals(configuration.getProperty(PROPERTY_APPEND_ROWS, "true")));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		// file default is null and matching options are not available

		mCheckBoxAppendRows.setSelected(true);
		}
}
