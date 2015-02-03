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
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorInfo;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableModel;

/**
 * This class simplifies creating new tasks that calculate some stuff based on an existing
 * chemical structure column with or without the need of an associated descriptor.
 * Typically the configuration doesn't need anything except the definition of the structure
 * column used and one or more calculated columns are finally attached to the table.
 * Derived classes may(!) do some pre-processing and must(!) implement either processRow(),
 * which is expected write calculated values directly into the tablemodel, or getNewColumnValue(),
 * which should return calculated values for specific cells. A post-processing may be used to
 * add views, etc.
 * If the configuration requires more parameters than just the chemical structure, then these
 * methods need to be overriden: getExtendedDialogContent(), getDialogConfiguration(),
 * setDialogConfiguration(), setDialogConfigurationToDefault()
 * and possibly isConfigurable(), isConfigurationValid().
 */
public abstract class DETaskAbstractAddChemProperty extends ConfigurableTask implements ActionListener,Runnable {
	protected static final int DESCRIPTOR_NONE = 0;
	protected static final int DESCRIPTOR_BINARY = 1;
	protected static final int DESCRIPTOR_ANY = 2;

	private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
	private static final String PROPERTY_DESCRIPTOR_SHORT_NAME = "descriptor";
	private static final String PROPERTY_NEW_COLUMN_NAME = "columnName";

	private JComboBox			mComboBoxStructureColumn,mComboBoxDescriptorColumn;
	private JTextField[]		mTextFieldColumnName;

	private volatile CompoundTableModel	mTableModel;
	private volatile int				mDescriptorClass,mStructureColumn,mDescriptorColumn;
	private boolean						mIsInteractive;
	private volatile boolean			mUseMultipleCores,mEditableColumnNames;
	private AtomicInteger				mSMPRecordIndex,mSMPWorkingThreads,mSMPErrorCount;

	public DETaskAbstractAddChemProperty(DEFrame parent, int descriptorClass, boolean editableColumnNames, boolean useMultipleCores, boolean isInteractive) {
		super(parent, true);
		mTableModel = parent.getTableModel();
		mDescriptorClass = descriptorClass;
		mEditableColumnNames = editableColumnNames;
		mUseMultipleCores = useMultipleCores;
		mIsInteractive = isInteractive;
		}

	/**
	 * Derived classes may overwrite this if they need to preprocess all chemistry objects
	 * before actually looping over all to create the calculated value.
	 * Lengthy preprocessings should start and update progress status.
	 * If the preprocessing fails, then call showErrorMessage() return false.
	 * @param configuration
	 * @return true if the preprocessing was successful
	 */
	protected boolean preprocessRows(Properties configuration) {
		return true;
		}

	/**
	 * This method is called after the table is modified with calculated results
	 * and may be overwritten to generate new views etc.
	 * @param tableModel
	 * @param firstNewColumn
	 */
	protected void postprocess(int firstNewColumn) {
		}

	/**
	 * Derived classes may overwrite this to assign column properties to the new columns.
	 * @param tableModel
	 * @param firstNewColumn
	 */
	protected void setNewColumnProperties(int firstNewColumn) {
		}

	/**
	 * If the number of new columns is not known before preprocessing
	 * this method may return 0. In this case column names cannot be
	 * customized by the user. After preprocessing this method must return
	 * the correct number of columns to create.
	 * @return count of new columns or 0 (only before preprocessing)
	 */
	abstract protected int getNewColumnCount();

	abstract protected String getNewColumnName(int column);

	/**
	 * Derived classes must either override this or override processRow() instead.
	 * @param mol is guaranteed to be != null
	 * @param descriptor
	 * @param column (one of the) new column(s)
	 * @return
	 */
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
		return null;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		if (getCompatibleStructureColumnList() == null) {
			String message = "Running '"+getTaskName()+"' requires the presence of chemical structures";
			switch (mDescriptorClass) {
			case DESCRIPTOR_NONE:
				showErrorMessage(message+".");
				break;
			case DESCRIPTOR_BINARY:
				showErrorMessage(message+" with a binary descriptor.");
				break;
			case DESCRIPTOR_ANY:
				showErrorMessage(message+" with any chemical descriptor.");
				break;
				}
			return false;
			}

		return true;
		}

	@Override
	public JPanel createDialogContent() {
		JPanel extentionPanel = getExtendedDialogContent();

		double[] sizeY = new double[3 + (mDescriptorClass != DESCRIPTOR_NONE? 2:0)
		                              + (extentionPanel != null? 3:0)
									  + (mEditableColumnNames? 2*getNewColumnCount() : 0)];
		int index = 0;
		sizeY[index++] = 8;
		sizeY[index++] = TableLayout.PREFERRED;
		if (mDescriptorClass != DESCRIPTOR_NONE) {
			sizeY[index++] = 4;
			sizeY[index++] = TableLayout.PREFERRED;
			}
		if (extentionPanel != null) {
			sizeY[index++] = 12;
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 8;
			}
		if (mEditableColumnNames) {
			for (int i=0; i<getNewColumnCount(); i++) {
				sizeY[index++] = 4;
				sizeY[index++] = TableLayout.PREFERRED;
				}
			}
		sizeY[index++] = 8;
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8}, sizeY };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		int[] structureColumn = getCompatibleStructureColumnList();

		// create components
		mComboBoxStructureColumn = new JComboBox();
		if (structureColumn != null)
			for (int i=0; i<structureColumn.length; i++)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(structureColumn[i]));
		content.add(new JLabel("Structure column:"), "1,1");
		content.add(mComboBoxStructureColumn, "3,1");
		mComboBoxStructureColumn.setEditable(!mIsInteractive);
		mComboBoxStructureColumn.addActionListener(this);

		index = 3;
		if (mDescriptorClass != DESCRIPTOR_NONE) {
			mComboBoxDescriptorColumn = new JComboBox();
			populateComboBoxDescriptor(structureColumn == null? -1 : structureColumn[0]);
			content.add(new JLabel("Descriptor:"), "1,"+index);
			content.add(mComboBoxDescriptorColumn, "3,"+index);
			index += 2;
			}

		if (extentionPanel != null) {
			content.add(extentionPanel, "1,"+index+",3,"+index);
			index += 3;
			}

		if (mEditableColumnNames) {
			mTextFieldColumnName = new JTextField[getNewColumnCount()];
			for (int i=0; i<getNewColumnCount(); i++) {
				mTextFieldColumnName[i] = new JTextField();
				content.add(new JLabel("New column name:"), "1,"+index);
				content.add(mTextFieldColumnName[i], "3,"+index);
				index += 2;
				}
			}

		return content;
		}

	/**
	 * If the implementation needs additional configuration items, then override this method
	 * to return a panel containing the corresponding control elements without any border.
	 * @return null or JPanel with controls
	 */
	public JPanel getExtendedDialogContent() {
		return null;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxStructureColumn)
			populateComboBoxDescriptor(mTableModel.findColumn((String)mComboBoxStructureColumn.getSelectedItem()));
		}

	private void populateComboBoxDescriptor(int structureColumn) {
		if (mComboBoxDescriptorColumn != null) {
			mComboBoxDescriptorColumn.removeAllItems();
			if (structureColumn != -1) {
				int[] descriptorColumn = getCompatibleDescriptorColumnList(structureColumn);
				if (descriptorColumn != null)
					for (int i=0; i<descriptorColumn.length; i++)
						mComboBoxDescriptorColumn.addItem(mTableModel.getDescriptorHandler(descriptorColumn[i]).getInfo().name);
				}
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int idcodeColumn = selectStructureColumn(configuration);
			if (idcodeColumn == -1) {
				showErrorMessage("Structure column not found.");
				return false;
				}
			if (mDescriptorClass != DESCRIPTOR_NONE) {
				String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
				if (descriptorName == null) {
					showErrorMessage("Descriptor column not defined.");
					return false;
					}
				int descriptorColumn = mTableModel.getChildColumn(idcodeColumn, descriptorName);
				if (descriptorColumn == -1) {
					showErrorMessage("Descriptor '"+descriptorName+"' not found.");
					return false;
					}
				}
			}
		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");
		if (value.length() != 0) {
			int column = mTableModel.findColumn(value);
			if (column != -1) {
				mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(column));

				if (mComboBoxDescriptorColumn != null) {
					value = configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
					if (value != null) {
						int descriptorColumn = mTableModel.getChildColumn(column, value);
						if (descriptorColumn != -1)
							mComboBoxDescriptorColumn.setSelectedItem(value);
						}
					}
				}
			else if (!mIsInteractive) {
				mComboBoxStructureColumn.setSelectedItem(value);
				if (mComboBoxDescriptorColumn != null) {
					value = configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
					if (value != null) {
						mComboBoxDescriptorColumn.setSelectedItem(value);
						}
					}
				}
			}
		else if (!mIsInteractive) {
			mComboBoxStructureColumn.setSelectedItem("Structure");
			}
		else if (mComboBoxStructureColumn.getItemCount() != 0) {
			mComboBoxStructureColumn.setSelectedIndex(0);
			}

		if (mEditableColumnNames) {
			for (int i=0; i<getNewColumnCount(); i++) {
				value = configuration.getProperty(PROPERTY_NEW_COLUMN_NAME+i);
				mTextFieldColumnName[i].setText(value == null ? getNewColumnName(i) : value);
				}
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		else if (!mIsInteractive)
			mComboBoxStructureColumn.setSelectedItem("Structure");
		if (mComboBoxDescriptorColumn != null
		 && mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);
		if (mEditableColumnNames)
			for (int i=0; i<getNewColumnCount(); i++)
				mTextFieldColumnName[i].setText(getNewColumnName(i));
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String structureColumn = (String)mComboBoxStructureColumn.getSelectedItem();
		if (structureColumn != null)
			configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, structureColumn);

		if (mComboBoxDescriptorColumn != null) {
			String descriptorName = (String)mComboBoxDescriptorColumn.getSelectedItem();
			if (descriptorName != null) {
				for (DescriptorInfo d:DescriptorConstants.DESCRIPTOR_LIST) {
					if (d.name.equals(descriptorName)) {
						configuration.setProperty(PROPERTY_DESCRIPTOR_SHORT_NAME, d.shortName);
						break;
						}
					}
				}
			}

		if (mEditableColumnNames)
			for (int i=0; i<getNewColumnCount(); i++)
				if (mTextFieldColumnName[i].getText().length() != 0 && !mTextFieldColumnName[i].getText().equals(getNewColumnName(i)))
					configuration.setProperty(PROPERTY_NEW_COLUMN_NAME+i, mTextFieldColumnName[i].getText());

		return configuration;
		}

	@Override
	public void runTask(Properties configuration) {
		mStructureColumn = selectStructureColumn(configuration);
		String shortName = (mDescriptorClass == DESCRIPTOR_NONE) ? null : configuration.getProperty(PROPERTY_DESCRIPTOR_SHORT_NAME);
		mDescriptorColumn = (shortName == null) ? -1 : mTableModel.getChildColumn(mStructureColumn, shortName);
		if (mDescriptorColumn != -1) {
			waitForDescriptor(mTableModel, mDescriptorColumn);
			if (threadMustDie())
				return;
			}

		if (!preprocessRows(configuration))
			return;
		if (threadMustDie())
			return;

		startProgress("Running '"+getTaskName()+"'...", 0, mTableModel.getTotalRowCount());

		String[] columnName = new String[getNewColumnCount()];
		for (int i=0; i<getNewColumnCount(); i++) {
			columnName[i] = configuration.getProperty(PROPERTY_NEW_COLUMN_NAME+i);
			if (columnName[i] == null)
				columnName[i] = getNewColumnName(i);
			}

		final int firstNewColumn = mTableModel.addNewColumns(columnName);
		setNewColumnProperties(firstNewColumn);

		if (mUseMultipleCores)
			finishTaskMultiCore(firstNewColumn);
		else
			finishTaskSingleCore(firstNewColumn);

		if (!threadMustDie())
			postprocess(firstNewColumn);
		}

	private void finishTaskSingleCore(final int firstNewColumn) {
		int errorCount = 0;

		StereoMolecule containerMol = new StereoMolecule();
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if ((row % 16) == 15)
				updateProgress(row);

			try {
				processRow(row, firstNewColumn, containerMol);
				}
			catch (Exception e) {
				errorCount++;
				}
			}

		if (!threadMustDie() && errorCount != 0)
			showErrorCount(errorCount);

		mTableModel.finalizeNewColumns(firstNewColumn, this);
		}

	private void finishTaskMultiCore(final int firstNewColumn) {
		int threadCount = Runtime.getRuntime().availableProcessors();
		mSMPRecordIndex = new AtomicInteger(mTableModel.getTotalRowCount());
		mSMPWorkingThreads = new AtomicInteger(threadCount);
		mSMPErrorCount = new AtomicInteger(0);

		Thread[] t = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			t[i] = new Thread("Abstract ChemProp Calculator "+(i+1)) {
				public void run() {
					StereoMolecule containerMol = new StereoMolecule();
					int recordIndex = mSMPRecordIndex.decrementAndGet();
					while (recordIndex >= 0 && !threadMustDie()) {
						try {
							processRow(recordIndex, firstNewColumn, containerMol);
							}
						catch (Exception e) {
							mSMPErrorCount.incrementAndGet();
							}

						updateProgress(-1);
						recordIndex = mSMPRecordIndex.decrementAndGet();
						}

					if (mSMPWorkingThreads.decrementAndGet() == 0) {
						if (!threadMustDie() && mSMPErrorCount.get() != 0)
							showErrorCount(mSMPErrorCount.get());

   						mTableModel.finalizeNewColumns(firstNewColumn, DETaskAbstractAddChemProperty.this);
						}
					}
				};
			t[i].setPriority(Thread.MIN_PRIORITY);
			t[i].start();
			}

		// the controller thread must wait until all others are finished
		// before the next task can begin or the dialog is closed
		for (int i=0; i<threadCount; i++)
			try { t[i].join(); } catch (InterruptedException e) {}
		}

	private void showErrorCount(int errorCount) {
		showErrorMessage("The task '"+getTaskName()+"' failed on "+errorCount+" molecules.");
		}

	/**
	 * Derived classes may overwrite this to directly assign values to compound table cells.
	 * The default implementation calls getNewColumnValue() for every new table cell.
	 * @param row
	 * @param containerMol null or container molecule to be used
	 * @param firstNewColumn
	 */
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null)
			for (int i=0; i<getNewColumnCount(); i++)
				mTableModel.setTotalValueAt(getNewColumnValue(mol, getDescriptor(row), i), row, firstNewColumn+i);
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	/**
	 * @return column containing the idcode when the task is running
	 */
	public int getStructureColumn() {
		return mStructureColumn;
		}

	/**
	 * Uses mTableModel.getChemicalStructure() to create a non-colored molecule.
	 * @param row
	 * @param mol null or a container molecule that is filled and returned
	 * @return null or valid molecule
	 */
	public StereoMolecule getChemicalStructure(int row, StereoMolecule mol) {
		return mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), mStructureColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, mol);
		}

	public Object getDescriptor(int row) {
		return (mDescriptorColumn == -1) ? null : mTableModel.getTotalRecord(row).getData(mDescriptorColumn);
		}

	private int selectStructureColumn(Properties configuration) {
		int[] idcodeColumn = getCompatibleStructureColumnList();
		if (idcodeColumn.length == 1)
			return idcodeColumn[0];	// there is no choice
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN));
		for (int i=0; i<idcodeColumn.length; i++)
			if (column == idcodeColumn[i])
				return column;
		return -1;
		}

	private int[] getCompatibleStructureColumnList() {
		int[] structureColumn = null;

		int[] idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumn != null) {
			int count = 0;
			for (int i=0; i<idcodeColumn.length; i++)
				if (mDescriptorClass == DESCRIPTOR_NONE || getCompatibleDescriptorColumnList(idcodeColumn[i]) != null)
					count++;

			if (count != 0) {
				structureColumn = new int[count];
				count = 0;
				for (int i=0; i<idcodeColumn.length; i++)
					if (mDescriptorClass == DESCRIPTOR_NONE || getCompatibleDescriptorColumnList(idcodeColumn[i]) != null)
						structureColumn[count++] = idcodeColumn[i];
				}
			}
		
		return structureColumn;
		}

	private int[] getCompatibleDescriptorColumnList(int parentColumn) {
		int[] descriptorColumn = null;

		int count = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getParentColumn(column) == parentColumn && isCompatibleDescriptorColumn(column))
				count++;

		if (count != 0) {
			descriptorColumn = new int[count];
			count = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getParentColumn(column) == parentColumn && isCompatibleDescriptorColumn(column))
					descriptorColumn[count++] = column;
			}

		return descriptorColumn;
		}

	private boolean isCompatibleDescriptorColumn(int column) {
		if (mTableModel.isDescriptorColumn(column)) {
			DescriptorHandler<?,?> dh = mTableModel.getDescriptorHandler(column);
			if (dh.getInfo().type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) {
				if (mDescriptorClass == DESCRIPTOR_ANY)
					return true;
				if (mDescriptorClass == DESCRIPTOR_BINARY && dh.getInfo().isBinary)
					return true;
				}
			}
		return false;
		}
	}
