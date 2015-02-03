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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableModel;


public class DETaskCalculateDescriptor extends ConfigurableTask {
	public static final String TASK_NAME = "Calculate Descriptor";
	private static Properties sRecentConfiguration;

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_DESCRIPTOR = "descriptor";

	private JComboBox	mComboBoxDescriptor,mComboBoxColumn;
	private String		mDescriptor;
	private CompoundTableModel	mTableModel;

	public DETaskCalculateDescriptor(DEFrame parent, String descriptor) {
		super(parent, descriptor == null);
		mTableModel = parent.getTableModel();
		mDescriptor = descriptor;
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
	public Properties getPredefinedConfiguration() {
		if (mDescriptor == null)
			return null;
		int type = DescriptorHelper.getDescriptorType(mDescriptor);
		int[] column = (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) ?
				mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode)
					 : (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION) ?
				mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode)
					 : null;
		if (column != null && column.length == 1) {
			Properties configuration = new Properties();
			configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(column[0]));
			configuration.setProperty(PROPERTY_DESCRIPTOR, mDescriptor);
			return configuration;
			}

		return null;
		}

	@Override
	public JPanel createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		p.setLayout(new TableLayout(size));
		p.add(new JLabel("Descriptor:"), "1,1");
		mComboBoxDescriptor = new JComboBox();
		for (int i=0; i<DescriptorConstants.DESCRIPTOR_LIST.length; i++)
			mComboBoxDescriptor.addItem(DescriptorConstants.DESCRIPTOR_LIST[i].shortName);
		p.add(mComboBoxDescriptor, "3,1");

		if (mDescriptor != null) {
			mComboBoxDescriptor.setSelectedItem(mDescriptor);	
			mComboBoxDescriptor.setEnabled(false);
			}

		p.add(new JLabel("Chemistry Column:"), "1,3");
		mComboBoxColumn = new JComboBox();
		int[] column = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);
		if (column != null)
			for (int i=0; i<column.length; i++)
				mComboBoxColumn.addItem(mTableModel.getColumnTitleNoAlias(column[i]));
		column = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode);
		if (column != null)
			for (int i=0; i<column.length; i++)
				mComboBoxColumn.addItem(mTableModel.getColumnTitleNoAlias(column[i]));
		mComboBoxColumn.setEditable(mDescriptor == null);
		p.add(mComboBoxColumn, "3,3");

		return p;
		}

	@Override
	public boolean isConfigurable() {
		return mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode) != null
			|| mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode) != null;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_DESCRIPTOR, (String)mComboBoxDescriptor.getSelectedItem());
		configuration.setProperty(PROPERTY_COLUMN, (String)mComboBoxColumn.getSelectedItem());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxDescriptor.setSelectedItem(configuration.getProperty(PROPERTY_DESCRIPTOR));
		mComboBoxColumn.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxDescriptor.setSelectedItem(DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			String descriptor = configuration.getProperty(PROPERTY_DESCRIPTOR);
			String columnName = configuration.getProperty(PROPERTY_COLUMN);
			int type = DescriptorHelper.getDescriptorType(descriptor);
			int column = mTableModel.findColumn(columnName);
			if (column == -1) {
				int[] columnList = (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) ?
						mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode)
							 : (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION) ?
						mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode)
							 : null;
				if (columnList != null)
					column = columnList[0];
				}
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' nor any alternative found.");
				return false;
				}
			if (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE
			 && !CompoundTableConstants.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))) {
				showErrorMessage("Column '"+columnName+"' doesn't contain molecules.");
				return false;
				}
			if (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION
			 && !CompoundTableConstants.cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column))) {
				showErrorMessage("Column '"+columnName+"' doesn't contain reactions.");
				return false;
				}
			if (mTableModel.getChildColumn(column, descriptor) != -1) {
				showErrorMessage("Column '"+columnName+"' has already the descriptor '"+descriptor+"'.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		final String descriptor = configuration.getProperty(PROPERTY_DESCRIPTOR);
		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		int column = mTableModel.findColumn(columnName);
		if (column == -1) {
			int type = DescriptorHelper.getDescriptorType(descriptor);
			int[] columnList = (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) ?
					mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode)
						 : (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION) ?
					mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode)
						 : null;
			if (columnList != null)
				column = columnList[0];
			}
		if (column != -1 && mTableModel.getChildColumn(column, descriptor) == -1) {
			if (SwingUtilities.isEventDispatchThread()) {	// is interactive
				mTableModel.createDescriptorColumn(column, descriptor);
				}
			else {
				try {
					final int _column = column;
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							mTableModel.createDescriptorColumn(_column, descriptor);
							}
						});
					}
				catch (Exception e) {}
				}
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
