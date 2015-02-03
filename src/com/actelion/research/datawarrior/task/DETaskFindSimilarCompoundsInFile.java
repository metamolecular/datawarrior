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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.CompoundFileParser;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableModel;

public class DETaskFindSimilarCompoundsInFile extends ConfigurableTask implements ActionListener,Runnable {
	static final long serialVersionUID = 0x20140205;

	private static final String PROPERTY_DESCRIPTOR_COLUMN = "descriptorColumn";
	private static final String PROPERTY_SIMILARITY = "similarity";
	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_IN_FILE_NAME = "inFile";
	private static final String PROPERTY_SIMILAR_FILE_NAME = "similarFile";
	private static final String PROPERTY_DISSIMILAR_FILE_NAME = "dissimilarFile";

	private static final String STRUCTURE_COLUMN_NAME = "Most Similar Structure";
	private static final String SIMILARITY_COLUMN_NAME = "Similarity";

	private static final int MIN_SIMILARITY = 70;
	private static final int DEFAULT_SIMILARITY = 85;
	private static final int MAX_DESCRIPTOR_CACHE_SIZE = 100000;

	public static final String TASK_NAME = "Find Similar Compounds In Other File";

	private static Properties sRecentConfiguration;

	private DEFrame				mSourceFrame;
	private CompoundTableModel	mTableModel;
	private JComboBox			mComboBoxDescriptorColumn;
	private JSlider				mSimilaritySlider;
	private JList				mListColumns;
	private JCheckBox			mCheckBoxSimilarFile,mCheckBoxDissimilarFile;
	private JFilePathLabel		mLabelInFileName,mLabelSimilarFileName,mLabelDissimilarFileName;
	private boolean				mIsInteractive,mCheckOverwriteSim,mCheckOverwriteDissim;

	public DETaskFindSimilarCompoundsInFile(DEFrame parent, boolean isInteractive) {
		super(parent, true);
		mSourceFrame = parent;
		mTableModel = mSourceFrame.getTableModel();
		mIsInteractive = isInteractive;
		mCheckOverwriteSim = true;
		mCheckOverwriteDissim = true;
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
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		boolean descriptorFound = false;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (qualifiesAsDescriptorColumn(column)) {
				descriptorFound = true;
				break;
				}
			}

		if (!descriptorFound) {
			showErrorMessage("No chemical descriptor found.");
			return false;
			}

		return true;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8,
								TableLayout.PREFERRED, 16, TableLayout.PREFERRED, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		JPanel inFilePanel = new JPanel();
		inFilePanel.setLayout(new BorderLayout());
		inFilePanel.add(new JLabel("File:  "), BorderLayout.WEST);
		mLabelInFileName = new JFilePathLabel(!mIsInteractive);
		inFilePanel.add(mLabelInFileName, BorderLayout.CENTER);
		content.add(inFilePanel, "1,1,3,1");

		JButton buttonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		buttonEdit.addActionListener(this);
		content.add(buttonEdit, "5,1");

		content.add(new JLabel("Descriptor:"), "1,3");
		mComboBoxDescriptorColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxDescriptorColumn.setEditable(!mIsInteractive);
		content.add(mComboBoxDescriptorColumn, "3,3,5,3");

		content.add(new JLabel("Similarity limit:", JLabel.RIGHT), "1,5");
		content.add(createSimilaritySlider(), "3,5,5,5");

		JLabel listLabel = new JLabel("<html>Select columns of other file<br>to be copied into this file<br>when structures match:<br><br>(Press Ctrl for multiple selection)</html>");
		listLabel.setVerticalAlignment(SwingConstants.TOP);
		content.add(listLabel, "1,7");

		mListColumns = new JList();
		JScrollPane scrollPane = new JScrollPane(mListColumns);
		content.add(scrollPane, "3,7,5,7");

		mCheckBoxSimilarFile = new JCheckBox("Save similar compounds to file:");
		content.add(mCheckBoxSimilarFile, "1,9");
		mCheckBoxSimilarFile.addActionListener(this);

		mLabelSimilarFileName = new JFilePathLabel(!mIsInteractive);
		content.add(mLabelSimilarFileName, "3,9,5,9");

		mCheckBoxDissimilarFile = new JCheckBox("Save dissimilar compounds to file:");
		content.add(mCheckBoxDissimilarFile, "1,10");
		mCheckBoxDissimilarFile.addActionListener(this);

		mLabelDissimilarFileName = new JFilePathLabel(!mIsInteractive);
		content.add(mLabelDissimilarFileName, "3,10,5,10");

		return content;
		}

	private JComponent createSimilaritySlider() {
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(MIN_SIMILARITY), new JLabel(""+MIN_SIMILARITY+"%"));
		labels.put(new Integer((100+MIN_SIMILARITY)/2), new JLabel(""+((100+MIN_SIMILARITY)/2)+"%"));
		labels.put(new Integer(100), new JLabel("100%"));
		mSimilaritySlider = new JSlider(JSlider.HORIZONTAL, MIN_SIMILARITY, 100, DEFAULT_SIMILARITY);
		mSimilaritySlider.setMinorTickSpacing(1);
		mSimilaritySlider.setMajorTickSpacing(10);
		mSimilaritySlider.setLabelTable(labels);
		mSimilaritySlider.setPaintLabels(true);
		mSimilaritySlider.setPaintTicks(true);
//		mSimilaritySlider.setPreferredSize(new Dimension(120, mSimilaritySlider.getPreferredSize().height));
		JPanel spanel = new JPanel();
		spanel.add(mSimilaritySlider);
		return spanel;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#CompareFiles";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String inFileName = configuration.getProperty(PROPERTY_IN_FILE_NAME);
		if (isLive && !isFileAndPathValid(inFileName, false, false))
			return false;

		int index = inFileName.lastIndexOf('.');
		String extension = (index == -1) ? "" : inFileName.substring(index+1).toLowerCase();
		if (!extension.equals("dwar") && !extension.equals("sdf")) {
			showErrorMessage("Input file is neither a DataWarrior file nor an SD-file.");
			return false;
			}

		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (descriptorName.length() == 0) {
			showErrorMessage("Descriptor column not defined.");
			return false;
			}

		if (isLive) {
			if (extension.equals("dwar")) {
				DWARFileParser parser = new DWARFileParser(inFileName);
				boolean hasStructures = parser.hasStructures();
				parser.close();
				if (!hasStructures) {
					showErrorMessage("The DataWarrior input-file doesn't contain chemical structures.");
					return false;
					}
				}
	
			String similarFileName = configuration.getProperty(PROPERTY_SIMILAR_FILE_NAME);
			if (similarFileName != null && !isFileAndPathValid(similarFileName, true, mCheckOverwriteSim))
				return false;
	
			String dissimilarFileName = configuration.getProperty(PROPERTY_DISSIMILAR_FILE_NAME);
			if (dissimilarFileName != null && !isFileAndPathValid(dissimilarFileName, true, mCheckOverwriteDissim))
				return false;
	
			int descriptorColumn = mTableModel.findColumn(descriptorName);
			if (descriptorColumn == -1) {
				showErrorMessage("Descriptor column '"+descriptorName+"' not found.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_IN_FILE_NAME);
		mLabelInFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);
		updateDialogFromFile(mLabelInFileName.getPath());

		value = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN);
		if (value != null) {
			int column = mTableModel.findColumn(value);
			if (column != -1 && qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!mIsInteractive)
				mComboBoxDescriptorColumn.setSelectedItem(value);
			else if (mComboBoxDescriptorColumn.getItemCount() != 0)
				mComboBoxDescriptorColumn.setSelectedIndex(0);
			}
		else if (!mIsInteractive) {
			mComboBoxDescriptorColumn.setSelectedItem("Structure [FragFp]");
			}

		int similarity = DEFAULT_SIMILARITY;
		value = configuration.getProperty(PROPERTY_SIMILARITY);
		if (value != null)
			try { similarity = Math.min(100, Math.max(MIN_SIMILARITY, Integer.parseInt(value))); } catch (NumberFormatException nfe) {}
		mSimilaritySlider.setValue(similarity);

		selectColumnsInList(mListColumns, configuration.getProperty(PROPERTY_COLUMN_LIST), mTableModel);
		mListColumns.clearSelection();
		String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (columnList != null) {
			for (String columnName:columnList.split("\\t")) {
				for (int i=0; i<mListColumns.getModel().getSize(); i++) {
					if (columnName.equals(mListColumns.getModel().getElementAt(i))) {
						mListColumns.addSelectionInterval(i, i);
						break;
						}
					}
				}
			}

		value = configuration.getProperty(PROPERTY_SIMILAR_FILE_NAME);
		mCheckBoxSimilarFile.setSelected(value != null);
		mLabelSimilarFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);

		value = configuration.getProperty(PROPERTY_DISSIMILAR_FILE_NAME);
		mCheckBoxDissimilarFile.setSelected(value != null);
		mLabelDissimilarFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		String path = askForCompoundFile(null);
		mLabelInFileName.setPath(path);
		updateDialogFromFile(path);

		if (mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);
		else if (!mIsInteractive)
			mComboBoxDescriptorColumn.setSelectedItem("Structure [FragFp]");

		mSimilaritySlider.setValue(DEFAULT_SIMILARITY);

		mListColumns.clearSelection();

		mCheckBoxSimilarFile.setSelected(false);
		mLabelSimilarFileName.setPath("");

		mCheckBoxDissimilarFile.setSelected(false);
		mLabelDissimilarFileName.setPath("");
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String fileName = mLabelInFileName.getPath();
		if (fileName != null)
			configuration.setProperty(PROPERTY_IN_FILE_NAME, fileName);

		String descriptorColumn = (String)mComboBoxDescriptorColumn.getSelectedItem();
		if (descriptorColumn != null)
			configuration.setProperty(PROPERTY_DESCRIPTOR_COLUMN, descriptorColumn);

		configuration.setProperty(PROPERTY_SIMILARITY, ""+mSimilaritySlider.getValue());

		Object[] selectedColumn = mListColumns.getSelectedValues();
		if (selectedColumn.length != 0) {
			StringBuilder sb = new StringBuilder((String)selectedColumn[0]);
			for (int i=1; i<selectedColumn.length; i++)
				sb.append('\t').append((String)selectedColumn[i]);
			configuration.setProperty(PROPERTY_COLUMN_LIST, sb.toString());
			}

		if (mCheckBoxSimilarFile.isSelected())
			configuration.setProperty(PROPERTY_SIMILAR_FILE_NAME, mLabelSimilarFileName.getPath());

		if (mCheckBoxDissimilarFile.isSelected())
			configuration.setProperty(PROPERTY_DISSIMILAR_FILE_NAME, mLabelDissimilarFileName.getPath());

		return configuration;
		}

	private String askForCompoundFile(String selectedFile) {
		File file = new FileHelper(getParentFrame()).selectFileToOpen(
				"Open Compound File", FileHelper.cFileTypeSD | FileHelper.cFileTypeDataWarrior, selectedFile);
		return (file == null) ? null : file.getPath();
		}

	/**
	 * If the file is valid, show the file name in the dialog,
	 * extract the list of visible columns and update mColumnList.
	 * @param filePath or null
	 */
	private void updateDialogFromFile(String filePath) {
		boolean fileIsValid = false;

		if (filePath != null && new File(filePath).exists()) {
			String error = updateColumnList(filePath);
			if (error != null)
				showErrorMessage(error);
			else
				fileIsValid = true;
			}

		mLabelInFileName.setPath(fileIsValid ? filePath : null);

		setOKButtonEnabled(fileIsValid);
		}

	/**
	 * Read and analyzes a compound file and updates the mColumnList
	 * @param fileName
	 * @return error message or null
	 */
	private String updateColumnList(String fileName) {
		int index = fileName.lastIndexOf('.');
		String extention = (index == -1) ? "" : fileName.substring(index).toLowerCase();

		ArrayList<String> columnList = new ArrayList<String>();

		if (extention.equals(".sdf")) {
			SDFileParser parser = new SDFileParser(fileName);
			for (String fieldName:parser.getFieldNames())
				columnList.add(fieldName);
			parser.close();
			}
		else if (extention.equals(".ode") || extention.equals(".dwar")) {
			DWARFileParser parser = new DWARFileParser(fileName);
			if (!parser.hasStructures()) {
				parser.close();
				return new File("'"+fileName).getName()+"' does not contain chemical structures.";
				}
			if (parser.getFieldNames() != null)
				for (String fieldName:parser.getFieldNames())
					columnList.add(fieldName);
			parser.close();
			}
		else {
			return new File("'"+fileName).getName()+"' is neither a DataWarrior file nor an SD-file.";
			}

		String[] itemList = columnList.toArray(new String[0]);
		Arrays.sort(itemList, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
				}
			} );
		mListColumns.removeAll();
		mListColumns.setListData(itemList);
		getDialog().pack();
		return null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	private boolean qualifiesAsDescriptorColumn(int column) {
		return DescriptorHelper.isDescriptorShortName(mTableModel.getColumnSpecialType(column));
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			String path = askForCompoundFile(resolveVariables(mLabelInFileName.getPath()));
			if (path != null) {
				mLabelInFileName.setPath(path);
				updateDialogFromFile(path);
				}
			return;
			}

		if (e.getSource() == mCheckBoxSimilarFile) {
			if (mCheckBoxSimilarFile.isSelected()) {
				String filename = new FileHelper(getParentFrame()).selectFileToSave(
						"Save Similar Compounds To File", FileHelper.cFileTypeDataWarrior, "Similar Compounds");
				if (filename != null) {
					mLabelSimilarFileName.setPath(filename);
					mLabelSimilarFileName.setEnabled(true);
					mCheckOverwriteSim = false;
					}
				else {
					mCheckBoxSimilarFile.setSelected(false);
					mLabelSimilarFileName.setPath(null);
					mLabelSimilarFileName.setEnabled(false);
					}
				}
			else {
				mLabelSimilarFileName.setEnabled(false);
				}
			return;
			}

		if (e.getSource() == mCheckBoxDissimilarFile) {
			if (mCheckBoxDissimilarFile.isSelected()) {
				String filename = new FileHelper(getParentFrame()).selectFileToSave(
						"Save Dissimilar Compounds To File", FileHelper.cFileTypeDataWarrior, "Dissimilar Compounds");
				if (filename != null) {
					mLabelDissimilarFileName.setPath(filename);
					mLabelDissimilarFileName.setEnabled(true);
					mCheckOverwriteDissim = false;
					}
				else {
					mCheckBoxDissimilarFile.setSelected(false);
					mLabelDissimilarFileName.setPath(null);
					mLabelDissimilarFileName.setEnabled(false);
					}
				}
			else {
				mLabelDissimilarFileName.setEnabled(false);
				}
			return;
			}
		}

	@Override
	public void runTask(Properties configuration) {
		String fileName = resolveVariables(configuration.getProperty(PROPERTY_IN_FILE_NAME));
		final int descriptorColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN));
		waitForDescriptor(mTableModel, descriptorColumn);
		if (threadMustDie())
			return;

		int intSim = DEFAULT_SIMILARITY;
		String value = configuration.getProperty(PROPERTY_SIMILARITY);
		if (value != null)
			try { intSim = Math.min(100, Math.max(MIN_SIMILARITY, Integer.parseInt(value))); } catch (NumberFormatException nfe) {}
		float similarityLimit = (float)intSim / 100f;

		String simFileName = configuration.getProperty(PROPERTY_SIMILAR_FILE_NAME);
		BufferedWriter simWriter = null;
		if (simFileName != null) {
			try {
				simWriter = new BufferedWriter(new FileWriter(resolveVariables(simFileName)));
				}
			catch (IOException ioe) {}
			}

		String dissimFileName = configuration.getProperty(PROPERTY_DISSIMILAR_FILE_NAME);
		BufferedWriter dissimWriter = null;
		if (dissimFileName != null) {
			try {
				dissimWriter = new BufferedWriter(new FileWriter(resolveVariables(dissimFileName)));
				}
			catch (IOException ioe) {}
			}

		boolean isSDF = fileName.substring(fileName.length()-4).toLowerCase().equals(".sdf");
		int dwarMode = DWARFileParser.MODE_COORDINATES_REQUIRE_2D | DWARFileParser.MODE_EXTRACT_DETAILS;
		if (simWriter != null || dissimWriter != null)
			dwarMode |= DWARFileParser.MODE_BUFFER_HEAD_AND_TAIL;
		CompoundFileParser parser = isSDF ? new SDFileParser(fileName) : new DWARFileParser(fileName, dwarMode);
		parser.setDescriptorHandlerFactory(CompoundTableModel.getDefaultDescriptorHandlerFactory());
		boolean coordsAvailable = (isSDF || ((DWARFileParser)parser).hasStructureCoordinates());

		if (!isSDF) {
			if (simFileName != null)
				writeHeadOrTail((DWARFileParser)parser, simWriter);
			if (dissimFileName != null)
				writeHeadOrTail((DWARFileParser)parser, dissimWriter);
			}

		int records = 0;
		int errors = 0;
		String descriptorType = mTableModel.getColumnSpecialType(descriptorColumn);

		@SuppressWarnings("unchecked")
		final DescriptorHandler<Object,Object> dh = mTableModel.getDescriptorHandler(descriptorColumn);

		TreeMap<String,Object> descriptorCache = new TreeMap<String,Object>();

		int alphaNumColumnCount = 0;
		int[] sourceColumn = null;
		String sourceColumnNames = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (sourceColumnNames != null) {
			String[] sourceColumnName = sourceColumnNames.split("\\t");
			String[] parserColumnName = parser.getFieldNames();
			sourceColumn = new int[sourceColumnName.length];
			for (int i=0; i<sourceColumnName.length; i++) {
				for (int j=0; j<parserColumnName.length; j++) {
					if (sourceColumnName[i].equals(parserColumnName[j])) {
						sourceColumn[alphaNumColumnCount++] = j;
						break;
						}
					}
				}
			}

		int structureColumnCount = coordsAvailable ? 2 : 1;
		int firstNewColumn = mTableModel.addNewColumns(1+structureColumnCount+alphaNumColumnCount);
		int firstNewAlphaNumColumn = firstNewColumn+1+structureColumnCount;

		TreeSet<String> detailReferences = new TreeSet<String>();

		mTableModel.setColumnName(SIMILARITY_COLUMN_NAME+" ["+descriptorType+"]", firstNewColumn);
		mTableModel.prepareStructureColumns(firstNewColumn+1, STRUCTURE_COLUMN_NAME, coordsAvailable, false);
		for (int i=0; i<alphaNumColumnCount; i++) {
			String columnName = parser.getFieldNames()[sourceColumn[i]];
			mTableModel.setColumnName(columnName, firstNewAlphaNumColumn+i);
			if (!isSDF) {
				Properties properties = ((DWARFileParser)parser).getColumnProperties(columnName);
				if (properties != null)
					for (Object key:properties.keySet())
						mTableModel.setColumnProperty(firstNewAlphaNumColumn+i, (String)key, (String)properties.get(key));
				}
			}

		int rowCount = parser.getRowCount();
		startProgress("Processing Compounds From File...", 0, (rowCount == -1) ? 0 : rowCount);
		while (parser.next()) {
			if (threadMustDie())
				break;

			records++;

			String idcode = parser.getIDCode();
			if (idcode == null) {
				errors++;
				continue;
				}

			Object descriptor = descriptorCache.get(idcode);
			if (descriptor == null) {
				descriptor = parser.getDescriptor(descriptorType);

				if (descriptor == null) {
					errors++;
					continue;
					}

				if (descriptorCache.size() < MAX_DESCRIPTOR_CACHE_SIZE)
					descriptorCache.put(idcode, descriptor);
				}

			final float[] similarityList = (descriptorType.equals(DescriptorConstants.DESCRIPTOR_Flexophore.shortName)) ?
					new float[mTableModel.getTotalRowCount()] : null;

					// for the flexophore we pre-calculate similarities on multiple threads...
			if (similarityList != null) {
				int threadCount = Runtime.getRuntime().availableProcessors();
				final AtomicInteger smtIndex = new AtomicInteger(mTableModel.getTotalRowCount());
				final Object _descriptor = descriptor;

				Thread[] t = new Thread[threadCount];
				for (int i=0; i<threadCount; i++) {
					t[i] = new Thread(TASK_NAME+" "+(i+1)) {
						public void run() {
							int index;
							while ((index = smtIndex.decrementAndGet()) >= 0) {
								CompoundRecord record = mTableModel.getTotalRecord(index);
								similarityList[index] = dh.getSimilarity(_descriptor, record.getData(descriptorColumn));
								}
							}
						};
					t[i].start();
					}
				for (int i=0; i<threadCount; i++)
					try { t[i].join(); } catch (InterruptedException ie) {}
				}

			boolean isSimilar = false;
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				CompoundRecord record = mTableModel.getTotalRecord(row);
				float similarity = (similarityList != null) ? similarityList[row]
						: dh.getSimilarity(descriptor, record.getData(descriptorColumn));
				if (similarity >= similarityLimit) {
					int similarityRank = 0;
					if (record.getData(firstNewColumn) != null) {	// if is not the only similarity value, then we have to compare
						String[] similarityText = new String((byte[])record.getData(firstNewColumn)).split("\\n");
						for (int i=0; i<similarityText.length; i++) {
							if (similarity > Float.parseFloat(similarityText[i]))
								break;
							similarityRank++;
							}
						}
					if (similarityRank == 0) {
						String _idcode = parser.getIDCode();
						if (_idcode != null) {
							record.setData(_idcode.getBytes(), firstNewColumn+1);
							if (coordsAvailable) {
								String coords = parser.getCoordinates();
								if (coords != null)
									record.setData(coords.getBytes(), firstNewColumn+2);
								}
							}
						}
					insertBytes(record, firstNewColumn, Float.toString(similarity).getBytes(), similarityRank);

					for (int i=0; i<alphaNumColumnCount; i++) {
						String fieldData = parser.getFieldData(sourceColumn[i]);
						int destColumn = firstNewAlphaNumColumn+i;
						insertBytes(record, destColumn, fieldData.getBytes(), similarityRank);
						if (!isSDF)
							mTableModel.getDetailHandler().extractEmbeddedDetailReferences(destColumn, fieldData, detailReferences);
						}

					isSimilar = true;
					}
				}

			if (simWriter != null && isSimilar)
				writeRecord(isSDF, parser, simWriter);
			if (dissimWriter != null && !isSimilar)
				writeRecord(isSDF, parser, dissimWriter);

			updateProgress(records);
			}

		if (!threadMustDie() && !isSDF && detailReferences.size() != 0) {
			resolveDetailIDCollisions(detailReferences);
			HashMap<String,byte[]> details = ((DWARFileParser)parser).getDetails();
			for (String key:detailReferences)
				mTableModel.getDetailHandler().setEmbeddedDetail(key, details.get(key));
			}

		if (!threadMustDie()) {
			if (!isSDF) {
				if (simFileName != null)
					writeHeadOrTail((DWARFileParser)parser, simWriter);
				if (dissimFileName != null)
					writeHeadOrTail((DWARFileParser)parser, dissimWriter);
				}
			}

		if (simWriter != null)
			try { simWriter.close(); } catch (IOException ioe) {}
		if (dissimWriter != null)
			try { dissimWriter.close(); } catch (IOException ioe) {}

		if (errors != 0)
			showErrorMessage(""+errors+" of "+records+" file records could not be processed and were skipped.");

		mTableModel.finalizeNewColumns(firstNewColumn, getProgressController());
		}

	private void resolveDetailIDCollisions(TreeSet<String> detailReferences) {
		if (mTableModel.getDetailHandler().getEmbeddedDetailCount() != 0) {
						// Existing data as well a new data have embedded details.
						// Adding an offset to the IDs of existing details ensures collision-free merging/appending.
			int highID = 0;
			for (String key:detailReferences) {
				try {
					int id = Math.abs(Integer.parseInt(key));
					if (highID < id)
						highID = id;
					}
				catch (NumberFormatException nfe) {}
				}

			if (highID != 0)
				mTableModel.addOffsetToEmbeddedDetailIDs(highID);
			}
		}

	private void insertBytes(CompoundRecord record, int column, byte[] bytes, int index) {
		// convert cLineSeparators into cEntrySeparators
		int lineCount = 0;
		for (byte b:bytes)
			if (b == CompoundTableConstants.cLineSeparatorByte)
				lineCount++;
		if (lineCount != 0) {
			byte[] old = bytes;
			bytes = new byte[old.length+(lineCount*(CompoundTableConstants.cEntrySeparatorBytes.length-1))];
			int i = 0;
			for (byte b:old) {
				if (b == CompoundTableConstants.cLineSeparatorByte)
					for (byte sb:CompoundTableConstants.cEntrySeparatorBytes)
						bytes[i++] = sb;
				else
					bytes[i++] = b;
				}
			}

		if (record.getData(column) == null) {
			record.setData(bytes, column);
			return;
			}

		byte[] detailSeparator = mTableModel.getDetailSeparator(column).getBytes();

		byte[] oldBytes = (byte[])record.getData(column);
		byte[] newBytes = new byte[oldBytes.length+1+bytes.length];
		int oldLength = getLengthWithoutDetail(oldBytes, detailSeparator);
		int length = getLengthWithoutDetail(bytes, detailSeparator);
		int i = 0;
		int entryIndex = 0;
		for (int j=0; j<oldLength; j++) {
			if (entryIndex == index) {	// we need to insert
				for (int k=0; k<length; k++)
					newBytes[i++] = bytes[k];
				newBytes[i++] = CompoundTableConstants.cLineSeparatorByte;
				entryIndex++;
				}
			newBytes[i++] = oldBytes[j];
			if (oldBytes[j] == CompoundTableConstants.cLineSeparatorByte)
				entryIndex++;
			}
		if (entryIndex < index) {	// we need to append
			newBytes[i++] = CompoundTableConstants.cLineSeparatorByte;
			for (int k=0; k<length; k++)
				newBytes[i++] = bytes[k];
			}

		// attach detail references to the end
		for (int j=oldLength; j<oldBytes.length; j++)
			newBytes[i++] = oldBytes[j];
		for (int k=length; k<bytes.length; k++)
			newBytes[i++] = bytes[k];

		record.setData(newBytes, column);
		}

	private int getLengthWithoutDetail(byte[] data, byte[] separator) {
		for (int i=0; i<=data.length-separator.length; i++) {
			int j=0;
			while (j<separator.length && data[i+j] == separator[j])
				j++;
			if (j == separator.length)
				return i;
			}
		return data.length;
		}

	private void writeHeadOrTail(DWARFileParser parser, BufferedWriter writer) {
		try {
			for (String line:parser.getHeadOrTail()) {
				if (!line.startsWith("<"+CompoundTableConstants.cNativeFileRowCount+"=")) {
					writer.write(line);
					writer.newLine();
					}
				}
			}
		catch (IOException ioe) {}
		}

	private void writeRecord(boolean isSDF, CompoundFileParser parser, BufferedWriter writer) {
		if (isSDF) {
			try {
				writer.write(((SDFileParser)parser).getNextMolFile());
				writer.write(((SDFileParser)parser).getNextFieldData());
				}
			catch (IOException ioe) {}
			}
		else {
			try {
				writer.write(((DWARFileParser)parser).getRow());
				writer.newLine();
				}
			catch (IOException ioe) {}
			}
		}
	}
