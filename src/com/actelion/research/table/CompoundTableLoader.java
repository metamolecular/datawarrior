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

package com.actelion.research.table;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.util.BinaryDecoder;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.FormatHelper;

public class CompoundTableLoader implements CompoundTableConstants,Runnable {
	public static final String DATASET_COLUMN_TITLE = "Dataset Name";
	public static final byte NEWLINE = '\n';			// used in String values of TableModel
	public static final byte[] NEWLINE_BYTES = NEWLINE_STRING.getBytes();

	public static final int	NEW_COLUMN = -1;	// pseudo destination columns for appending/merging data
	public static final int	NO_COLUMN = -2;

	public static final int MERGE_MODE_IS_KEY = 0;
	public static final int MERGE_MODE_APPEND = 1;
	public static final int MERGE_MODE_KEEP = 2;
	public static final int MERGE_MODE_REPLACE = 3;
	public static final int MERGE_MODE_USE_IF_EMPTY = 4;
	public static final int MERGE_MODE_AS_PARENT = 5;	// <- from here merge mode(s) not being available for user selection

	public static final int READ_DATA = 1;		// load data into buffer
	public static final int REPLACE_DATA = 2;	// empty tablemodel and copy data from buffer to tablemodel
	public static final int APPEND_DATA = 4;	// append data from buffer to tablemodel
	public static final int MERGE_DATA = 8;		// merge data from buffer to tablemodel
	public static final int APPLY_TEMPLATE = 16;// apply the loaded template

	private static final int PROGRESS_LIMIT = 50000;
	private static final int PROGRESS_STEP = 200;

	private static final String COLUMN_TITLE_ACTELION_NO = "Actelion No";
	private static final String COLUMN_TITLE_ELN_EXTREF = "ELN/ExtRef";
	private static final String LOOKUP_NAME_BIORESULTS = "Biological Results";
	private static final String LOOKUP_URL_BIORESULTS = "http://ares:8080/dataCenter/measurementSearch.do?actNos=%s&submitSearch=";
	private static final String LOOKUP_NAME_ELB = "ELB from Niobe";
	private static final String LOOKUP_URL_ELB = "http://ares:8080/portal/jsp/displayniobepdf.jsp?labJournal=%s";

	private CompoundTableModel	mTableModel;
	private Frame   			mParentFrame;
	private volatile ProgressController	mProgressController;
	private volatile File		mFile;
	private volatile Reader		mDataReader;
	private volatile int		mDataType,mAction;
	private int					mOldVersionIDCodeColumn,mOldVersionCoordinateColumn,mOldVersionCoordinate3DColumn;
	private boolean				mWithHeaderLine,mAppendRest,mCoordsMayBe3D;
	private volatile boolean	mOwnsProgressController;
	private String				mNewWindowTitle,mVersion;
	private RuntimeProperties	mRuntimeProperties;
	private String[]			mFieldNames;
	private Object[][]			mFieldData;
	private volatile Thread		mThread;
	private int					mAppendDatasetColumn,mFirstNewColumn;
	private int[]				mAppendDestColumn,mMergeDestColumn,mMergeMode;
	private String				mAppendDatasetNameExisting,mAppendDatasetNameNew;
	private ArrayList<String>	mHitlists;
	private ArrayList<String>	mColumnProperties;
	private TreeMap<String,Object> mExtensionMap;
	private HashMap<String,byte[]> mDetails;

	/**
	 * This contructor must be invoked from the EventDispatchThread
	 * @param parent
	 * @param tableModel
	 */
	public CompoundTableLoader(Frame parent, CompoundTableModel tableModel) {
		this(parent, tableModel, null);
		}

	/**
	 * If this contructor is not invoked from the EventDispatchThread, then a valid
	 * progress controller must be specified.
	 * @param parent
	 * @param tableModel
	 * @param pc
	 */
	public CompoundTableLoader(Frame parent, CompoundTableModel tableModel, ProgressController pc) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mProgressController = pc;
		}

	public void paste() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable theData = clipboard.getContents(this);
		String s;
		try {
			s = (String)(theData.getTransferData(DataFlavor.stringFlavor));
			mWithHeaderLine = analyzeHeaderLine(new StringReader(s));
			mDataReader = new StringReader(s);
			mAction = READ_DATA | REPLACE_DATA;
			mDataType = FileHelper.cFileTypeTextTabDelimited;
			mNewWindowTitle = "Data From Clipboard";
			mRuntimeProperties = null;
			processData();
			}
		catch (Exception e) {
			mTableModel.unlock();
			e.printStackTrace();
			JOptionPane.showMessageDialog(mParentFrame, e.toString());
			}
		}

	public void readFile(URL url, RuntimeProperties properties) {
		try {
			mDataReader = new InputStreamReader(url.openStream());
			}
		catch (IOException e) {
			mTableModel.unlock();
			JOptionPane.showMessageDialog(mParentFrame, "IO-Exception during file retrieval.");
			return;
			}
		mDataType = FileHelper.cFileTypeDataWarrior;
		mAction = READ_DATA | REPLACE_DATA;
		mWithHeaderLine = true;
		mNewWindowTitle = url.toString();
		mRuntimeProperties = properties;
		processData();
		}

	public void readFile(File file, RuntimeProperties properties) {
		readFile(file, properties, FileHelper.cFileTypeDataWarrior, READ_DATA | REPLACE_DATA);
		}

	public void readTemplate(File file, RuntimeProperties properties) {
		readFile(file, properties, FileHelper.cFileTypeDataWarriorTemplate, READ_DATA | APPLY_TEMPLATE);
		}

	public void readFile(File file, RuntimeProperties properties, int dataType) {
		readFile(file, properties, dataType, READ_DATA | REPLACE_DATA);
		}

	public void readFile(File file, RuntimeProperties properties, int dataType, int action) {
		mFile = file;
		try {
			mDataReader = new FileReader(mFile);
			}
		catch (FileNotFoundException e) {
			mTableModel.unlock();
			JOptionPane.showMessageDialog(mParentFrame, "File not found.");
			return;
			}
		mDataType = dataType;
		mAction = action;
		mWithHeaderLine = true;
		mNewWindowTitle = mFile.getName();
		mRuntimeProperties = properties;
		processData();
		}

	public String[] getFieldNames() {
		while (mThread != null)
			try { Thread.sleep(100); } catch (InterruptedException e) {}

		return mFieldNames;
		}

	public void appendFile(int[] destColumn, int datasetColumn, String existingSetName, String newSetName) {
		mAction = APPEND_DATA;
		mAppendDestColumn = destColumn;
		mAppendDatasetColumn = datasetColumn;
		mAppendDatasetNameExisting = existingSetName;
		mAppendDatasetNameNew = newSetName;
		processData();
		}

	/**
	 * Checks whether the columns defined in mergeMode as keys have content that uniquely
	 * identifies every row.
	 * @param mergeMode matching the total previously read column count with at least one MERGE_MODE_IS_KEY entry
	 * @param pl null or progress listener to receive messages
	 * @return true if all 
	 */
	public boolean areMergeKeysUnique(String[] keyColumnName, ProgressListener pl) {
		if (pl != null)
			pl.startProgress("Sorting new keys...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		int[] keyColumn = new int[keyColumnName.length];
		for (int i=0; i<keyColumnName.length; i++) {
			for (int j=0; j<mFieldNames.length; j++) {
				if (mFieldNames[j].equals(keyColumnName[i])) {
					keyColumn[i] = j;
					break;
					}
				}
			}

		TreeSet<byte[]> newKeySet = new TreeSet<byte[]>(new ByteArrayComparator());
		for (int row=0; row<mFieldData.length; row++) {
			if (pl != null && mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				pl.updateProgress(row);

			byte[] key = constructMergeKey((Object[])mFieldData[row], keyColumn);
			if (key != null) {
				if (newKeySet.contains(key))
					return false;

				newKeySet.add(key);
				}
			}
		return true;
		}

	/**
	 * Combines all individual byte arrays from all key columns
	 * separated by TAB codes. If none of the columns contain any data, then null is returned.
	 * @param rowData
	 * @param keyColumn
	 * @return null or row key as byte array
	 */
	private byte[] constructMergeKey(Object[] rowData, int[] keyColumn) {
		int count = keyColumn.length - 1;	// TABs needed
		for (int i=0; i<keyColumn.length; i++) {
			byte[] data = (byte[])rowData[keyColumn[i]];
			if (data != null)
				count += data.length;
			}
		if (count == keyColumn.length - 1)
			return null;

		byte[] key = new byte[count];
		int index = 0;
		for (int i=0; i<keyColumn.length; i++) {
			if (i != 0)
				key[index++] = '\t';
			byte[] data = (byte[])rowData[keyColumn[i]];
			if (data != null)
				for (byte b:data)
					key[index++] = b;
			}

		return key;
		}

	/**
	 * Merges previously read file content into the associated table model.
	 * Prior to this method either paste() or one of the readFile() methods
	 * must have been called. Then areMergeKeysUnique() must have been called
	 * and must have returned true.
	 * @param destColumn
	 * @param mergeMode
	 * @param appendRest
	 */
	public void mergeFile(int[] destColumn, int[] mergeMode, boolean appendRest) {
		mAction = MERGE_DATA;
		mMergeDestColumn = destColumn;
		mMergeMode = mergeMode;
		mAppendRest = appendRest;
		processData();
		}

	private void processData() {
		if (SwingUtilities.isEventDispatchThread()) {
			if (mProgressController == null) {
				mProgressController = new JProgressDialog(mParentFrame);
				mOwnsProgressController = true;
				}
	
			mThread = new Thread(this, "CompoundTableLoader");
			mThread.setPriority(Thread.MIN_PRIORITY);
			mThread.start();
			}
		else {
			run();
			}
		}

	private boolean analyzeHeaderLine(Reader reader) throws Exception {
		BufferedReader theReader = new BufferedReader(reader);

		ArrayList<String> lineList = new ArrayList<String>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				lineList.add(theLine);
				}
			}
		catch (IOException e) {}
		theReader.close();

		if (lineList.size() < 2)
			return false;

		char columnSeparator = (mDataType == FileHelper.cFileTypeTextCommaSeparated) ? ',' : '\t';
		ArrayList<String> columnNameList = new ArrayList<String>();
		String header = lineList.get(0);
		int fromIndex = 0;
		int toIndex;
		do {
			String columnName;
			toIndex = header.indexOf(columnSeparator, fromIndex);

			if (toIndex == -1) {
				columnName = header.substring(fromIndex);
				}
			else {
				columnName = header.substring(fromIndex, toIndex);
				fromIndex = toIndex+1;
				}

			if (columnName.equalsIgnoreCase("smiles")
			 || columnName.equalsIgnoreCase(COLUMN_TITLE_ACTELION_NO)
			 || columnName.equalsIgnoreCase("actelion number")
			 || columnName.equalsIgnoreCase("act_no")
			 || columnName.equalsIgnoreCase("chem lab journal")
			 || columnName.equalsIgnoreCase("chem_lab_journal")
			 || columnName.equalsIgnoreCase("eln")
			 || columnName.equalsIgnoreCase(COLUMN_TITLE_ELN_EXTREF)
			 || columnName.equalsIgnoreCase("ext reference")
			 || columnName.equalsIgnoreCase("ext_reference")
			 || columnName.equalsIgnoreCase("extref")
			 || columnName.equalsIgnoreCase("tube id")
			 || columnName.equalsIgnoreCase("tube_id")
			 || columnName.equalsIgnoreCase("plate position")
			 || columnName.equalsIgnoreCase("inventory no")
			 || columnName.equalsIgnoreCase("inventory barcode")
			 || columnName.equalsIgnoreCase("ai no")
			 || columnName.equalsIgnoreCase("bottle_barcode")
			 || columnName.equalsIgnoreCase("idcode")
			 || columnName.endsWith("[idcode]")
			 || columnName.endsWith("[rxncode]")
			 || columnName.startsWith("fingerprint"))
				return true;

			columnNameList.add(columnName);
			} while (toIndex != -1);

		boolean[] isNotNumerical = new boolean[columnNameList.size()];
		for (int row=1; row<lineList.size(); row++) {
			String theLine = lineList.get(row);
			fromIndex = 0;
			int sourceColumn = 0;
			do {
				String value;
				toIndex = theLine.indexOf(columnSeparator, fromIndex);
				if (toIndex == -1) {
					value = theLine.substring(fromIndex);
					}
				else {
					value = theLine.substring(fromIndex, toIndex);
					fromIndex = toIndex+1;
					}

				if (!isNotNumerical[sourceColumn] && value.length() != 0) {
					try {
						Double.parseDouble(value);
						}
					catch (NumberFormatException e) {
						isNotNumerical[sourceColumn] = true;
						}
					}

				sourceColumn++;
				} while (sourceColumn<columnNameList.size() && toIndex != -1);
			}

		for (int column=0; column<columnNameList.size(); column++) {
			if (!isNotNumerical[column]) {
				try {
					Double.parseDouble(columnNameList.get(column));
					}
				catch (NumberFormatException e) {
					return true;
					}
				}
			}

		return false;
		}

	private boolean readTemplateOnly() {
		mProgressController.startProgress("Reading Template...", 0, 0);

		BufferedReader theReader = new BufferedReader(mDataReader);

		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				if (theLine.equals(cPropertiesStart)) {
					mRuntimeProperties.read(theReader);
					break;
					}
				}
			theReader.close();
			}
		catch (IOException e) {}

		return true;
		}

	private boolean readTextData() {
		mProgressController.startProgress("Reading Data...", 0, 0);

		BufferedReader theReader = new BufferedReader(mDataReader);

		String header = null;
		mVersion = null;
		int rowCount = -1;
		CompoundTableExtensionHandler extensionHandler = mTableModel.getExtensionHandler();
		ArrayList<byte[]> lineList = new ArrayList<byte[]>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				if (theLine.equals(cNativeFileHeaderStart)) {
					rowCount = readFileHeader(theReader);
					if (rowCount > PROGRESS_LIMIT)
						mProgressController.startProgress("Reading Data...", 0, (rowCount > PROGRESS_LIMIT) ? rowCount : 0);
					continue;
					}

				if (extensionHandler != null) {
					String name = extensionHandler.extractExtensionName(theLine);
					if (name != null) {
						if (mExtensionMap == null)
							mExtensionMap = new TreeMap<String,Object>();
						mExtensionMap.put(name, extensionHandler.readData(name, theReader));
						continue;
						}
					}

				if (theLine.equals(cColumnPropertyStart)) {
					readColumnProperties(theReader);
					continue;
					}

				if (theLine.equals(cHitlistDataStart)) {
					readHitlistData(theReader);
					continue;
					}

				if (theLine.equals(cDetailDataStart)) {
					readDetailData(theReader);
					continue;
					}

				if (theLine.equals(cPropertiesStart)) {
					if ((mAction & APPEND_DATA) == 0
					 && (mAction & MERGE_DATA) == 0
					 && mRuntimeProperties != null)
						mRuntimeProperties.read(theReader);

					break;
					}

				if (mDataType != FileHelper.cFileTypeDataWarriorTemplate) {
					if (mWithHeaderLine && header == null) {
						header = theLine;
						}
					else {
						lineList.add(theLine.getBytes());
						if (rowCount > PROGRESS_LIMIT && lineList.size()%PROGRESS_STEP == 0)
							mProgressController.updateProgress(lineList.size());
						}
					}
				}
			theReader.close();
			}
		catch (IOException e) {}

		if (mWithHeaderLine && header == null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, "No header line found.");
					}
				} );
			return false;
			}

		if (mDataType != FileHelper.cFileTypeDataWarriorTemplate)
			processLines(header, lineList);

		return true;
		}

	private void processLines(String header, ArrayList<byte[]> lineList) {
		ArrayList<String> columnNameList = new ArrayList<String>();
		byte columnSeparator = (mDataType == FileHelper.cFileTypeTextCommaSeparated) ? (byte)',' : (byte)'\t';

		if (mWithHeaderLine) {
			int fromIndex = 0;
			int toIndex = 0;
			do {
				String columnName;
				toIndex = header.indexOf(columnSeparator, fromIndex);
				if (toIndex == -1) {
					columnName = header.substring(fromIndex);
					}
				else {
					columnName = header.substring(fromIndex, toIndex);
					fromIndex = toIndex+1;
					}

				String[] type = cParentSpecialColumnTypes;
				for (int i=0; i<type.length; i++) {
					if (columnName.endsWith("["+type[i]+"]")) {
						columnName = columnName.substring(0, columnName.length()-type[i].length()-2);
						if (mColumnProperties == null)
							mColumnProperties = new ArrayList<String>();
						mColumnProperties.add(columnName
								+"\t"+cColumnPropertySpecialType
								+"\t"+type[i]);
						}
					}

				columnNameList.add(normalizeColumnName(columnName));
				} while (toIndex != -1);
			}

		if (mVersion == null)
			createColumnPropertiesForFilesPriorVersion270(columnNameList);

		if (!mWithHeaderLine && lineList.size() > 0) {
			byte[] lineBytes = lineList.get(0);
			columnNameList.add("Column 1");
			int no = 2;
			for (byte b:lineBytes)
				if (b == columnSeparator)
					columnNameList.add("Column "+no++);
			}

		int columnCount = columnNameList.size();

		mFieldNames = new String[columnCount];
		mFieldData = new Object[lineList.size()][columnCount];

		for (int column=0; column<columnCount; column++)
			mFieldNames[column] = columnNameList.get(column);

		boolean[] descriptorValid = new boolean[columnCount];
		DescriptorHandler<?,?>[] descriptorHandler = new DescriptorHandler[columnCount];
		for (int column=0; column<columnCount; column++) {
			descriptorHandler[column] = CompoundTableModel.getDefaultDescriptorHandler(getColumnSpecialType(mFieldNames[column]));
			descriptorValid[column] = descriptorHandler[column] != null
					&& descriptorHandler[column].getVersion().equals(
							getColumnProperty(mFieldNames[column], cColumnPropertyDescriptorVersion));
			}

		mProgressController.startProgress("Processing Records...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		for (int row=0; row<mFieldData.length; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			byte[] lineBytes = lineList.get(row);
			lineList.set(row, null);
			int fromIndex = 0;
			int column = 0;
			do {
				int toIndex = fromIndex;
				while (toIndex<lineBytes.length && lineBytes[toIndex] != columnSeparator)
					toIndex++;

				if (toIndex == fromIndex) {
					mFieldData[row][column] = null;
					}
				else {
					byte[] cellBytes = Arrays.copyOfRange(lineBytes, fromIndex, toIndex);
	
					if (descriptorHandler[column] == null)
						mFieldData[row][column] = convertNewlines(cellBytes);
					else if (descriptorValid[column])
						mFieldData[row][column] = descriptorHandler[column].decode(cellBytes);
					}

				fromIndex = toIndex + 1;
				column++;
				} while (fromIndex<lineBytes.length && column<columnCount);
			}

		if (!mWithHeaderLine)
			deduceColumnTitles();

		if (mColumnProperties == null)	// paste; comma- or TAB-delimited files
			addDefaultLookupColumnProperties();

		handleSmiles();
		if (mVersion == null) // a version entry exists since V3.0
			handlePotentially3DCoordinates();
		}

	private void handleSmiles() {
		int smilesColumn = -1;
		for (int column=0; column<mFieldNames.length; column++) {
			if (mFieldNames[column].equalsIgnoreCase("smiles")) {
				smilesColumn = column;
				break;
				}
			}

		if (smilesColumn == -1)
			return;

			// don't do anything smiles, if there is already a structure column
		if (mColumnProperties != null)
			for (int i=0; i<mColumnProperties.size(); i++)
				if (mColumnProperties.get(i).contains(cColumnPropertySpecialType
												+"\t"+cColumnTypeIDCode))
					return;

		int columnCount = mFieldNames.length;

		String[] newFieldNames = new String[columnCount+1];
		for (int i=0; i<columnCount; i++)
			newFieldNames[i+1] = mFieldNames[i];
		newFieldNames[0] = "Structure";
		mFieldNames = newFieldNames;

		for (int row=0; row<mFieldData.length; row++) {
			Object[] newFieldData = new Object[columnCount+1];
			for (int i=0; i<columnCount; i++)
				newFieldData[i+1] = mFieldData[row][i];
			newFieldData[0] = getIDCodeFromSmiles((byte[])mFieldData[row][smilesColumn]);
			mFieldData[row] = newFieldData;
			}

		if (mColumnProperties == null)
			mColumnProperties = new ArrayList<String>();

		mColumnProperties.add("Structure"
				+"\t"+cColumnPropertySpecialType
				+"\t"+cColumnTypeIDCode);
		}

	private void deduceColumnTitles() {
		if (mFieldData.length == 0)
			return;

		for (int column=0; column<mFieldNames.length; column++) {
			int firstRow = 0;
			while (firstRow<mFieldData.length && mFieldData[firstRow][column] == null)
				firstRow++;
			if (firstRow == mFieldData.length || !(mFieldData[firstRow][column] instanceof byte[]))
				continue;

			boolean isActNo = FormatHelper.isValidActelionNo(new String((byte[])mFieldData[firstRow][column]).trim());
			for (int row=firstRow+1; isActNo && row<mFieldData.length; row++)
				if (mFieldData[row][column] != null)
					isActNo = FormatHelper.isValidActelionNo(new String((byte[])mFieldData[row][column]).trim());

			if (isActNo) {
				mFieldNames[column] = COLUMN_TITLE_ACTELION_NO;
				continue;
				}

			boolean isLabJou = FormatHelper.isValidLabJournalNo(new String((byte[])mFieldData[firstRow][column]).trim());
			for (int row=firstRow+1; isLabJou && row<mFieldData.length; row++)
				if (mFieldData[row][column] != null)
					isLabJou = FormatHelper.isValidLabJournalNo(new String((byte[])mFieldData[row][column]).trim());

			if (isLabJou) {
				mFieldNames[column] = COLUMN_TITLE_ELN_EXTREF;
				continue;
				}
			}
		}

	private void addDefaultLookupColumnProperties() {
		for (int column=0; column<mFieldNames.length; column++) {
			if (COLUMN_TITLE_ACTELION_NO.equalsIgnoreCase(mFieldNames[column])) {
				if (mColumnProperties == null)
					mColumnProperties = new ArrayList<String>();
				mColumnProperties.add(COLUMN_TITLE_ACTELION_NO + "\t" + CompoundTableModel.cColumnPropertyLookupCount + "\t" + "1");
				mColumnProperties.add(COLUMN_TITLE_ACTELION_NO + "\t" + CompoundTableModel.cColumnPropertyLookupName+"0" + "\t" + LOOKUP_NAME_BIORESULTS);
				mColumnProperties.add(COLUMN_TITLE_ACTELION_NO + "\t" + CompoundTableModel.cColumnPropertyLookupURL+"0" + "\t" + LOOKUP_URL_BIORESULTS);
				continue;
				}
			if (COLUMN_TITLE_ELN_EXTREF.equalsIgnoreCase(mFieldNames[column])) {
				if (mColumnProperties == null)
					mColumnProperties = new ArrayList<String>();
				mColumnProperties.add(COLUMN_TITLE_ELN_EXTREF + "\t" + CompoundTableModel.cColumnPropertyLookupCount + "\t" + "1");
				mColumnProperties.add(COLUMN_TITLE_ELN_EXTREF + "\t" + CompoundTableModel.cColumnPropertyLookupName+"0" + "\t" + LOOKUP_NAME_ELB);
				mColumnProperties.add(COLUMN_TITLE_ELN_EXTREF + "\t" + CompoundTableModel.cColumnPropertyLookupURL+"0" + "\t" + LOOKUP_URL_ELB);
				continue;
				}
			}
		}

	private void createColumnPropertiesForFilesPriorVersion270(ArrayList<String> columnNameList) {
			// Native DataWarrior files before V2.7.0 didn't have column properties
			// for column headers 'idcode','idcoordinates','fingerprint_Vxxx'.
			// There types were recognized by the column header only.
		mOldVersionIDCodeColumn = -1;
		mOldVersionCoordinateColumn = -1;
		for (int i=0; i<columnNameList.size(); i++) {
			String columnName = columnNameList.get(i);
			if (columnName.equals("idcode") && !columnHasProperty(columnName)) {
				columnNameList.set(i, "Structure");
				if (mColumnProperties == null)
					mColumnProperties = new ArrayList<String>();
				String structureProperties = cColumnPropertySpecialType
									   +"\t"+cColumnTypeIDCode;
				for (int j=0; j<columnNameList.size(); j++) {
					if (columnName.equals(COLUMN_TITLE_ACTELION_NO)) {
						structureProperties = structureProperties
									   +"\t"+cColumnPropertyIdentifierColumn
									   +"\t"+COLUMN_TITLE_ACTELION_NO;
						break;
						}
					}
				mColumnProperties.add("Structure\t"+structureProperties);
				mOldVersionIDCodeColumn = i;
				}
			if (mOldVersionIDCodeColumn != -1
			 && (columnName.equals("idcoordinates") || columnName.equals("idcoords"))
			 && !columnHasProperty(columnName)) {
				columnNameList.set(i, cColumnType2DCoordinates);
				if (mColumnProperties == null)
					mColumnProperties = new ArrayList<String>();
				mColumnProperties.add(cColumnType2DCoordinates
												+"\t"+cColumnPropertySpecialType
												+"\t"+cColumnType2DCoordinates
												+"\t"+cColumnPropertyParentColumn
												+"\tStructure");
				mOldVersionCoordinateColumn = i;
				}
			if (mOldVersionIDCodeColumn != -1
			 && columnName.startsWith("fingerprint_")
			 && !columnHasProperty(columnName)) {
				columnNameList.set(i, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
				if (mColumnProperties == null)
					mColumnProperties = new ArrayList<String>();
				mColumnProperties.add(DescriptorConstants.DESCRIPTOR_FFP512.shortName
												+"\t"+cColumnPropertySpecialType
												+"\t"+DescriptorConstants.DESCRIPTOR_FFP512.shortName
												+"\t"+cColumnPropertyParentColumn
												+"\tStructure"
												+"\t"+cColumnPropertyDescriptorVersion
												+"\t"+columnName.substring(12));
				}
			}
		if (mOldVersionCoordinateColumn != -1)
			mCoordsMayBe3D = true;
		}

	private boolean columnHasProperty(String columnName) {
		if (mColumnProperties != null) {
			for (int i=0; i<mColumnProperties.size(); i++)
				if (mColumnProperties.get(i).startsWith(columnName+"\t"))
					return true;
			}
		return false;
		}

	private int readFileHeader(BufferedReader theReader) {
		int rowCount = -1;
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cNativeFileHeaderEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cNativeFileVersion)) {
					mVersion = extractValue(theLine);
					continue;
					}

				if (theLine.startsWith("<"+cNativeFileRowCount)) {
					try {
						rowCount = Integer.parseInt(extractValue(theLine));
						}
					catch (NumberFormatException nfe) {}
					continue;
					}
				}
			}
		catch (Exception e) {}
		return rowCount;
		}

	private void readColumnProperties(BufferedReader theReader) {
		mColumnProperties = new ArrayList<String>();
		try {
			String columnName = null;
			String columnProperties = "";
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cColumnPropertyEnd)) {
					mColumnProperties.add(columnName + columnProperties);
					break;
					}

				if (theLine.startsWith("<"+cColumnName)) {
					if (columnProperties.length() > 0)
						mColumnProperties.add(columnName + columnProperties);

					columnName = extractValue(theLine);
					columnProperties = "";
					continue;
					}

				if (theLine.startsWith("<"+cColumnProperty)) {
					String keyAndValue = extractValue(theLine);

					// to support deprecated property cColumnPropertyIsIDCode => "isIDCode"
					if (keyAndValue.equals("isIDCode\ttrue"))
						keyAndValue = cColumnPropertySpecialType+"\t"+cColumnTypeIDCode;

					columnProperties = columnProperties + "\t" + keyAndValue;
					continue;
					}
				}
			}
		catch (Exception e) {
			mColumnProperties = null;
			}
		}

	public String getParentColumnName(String columnName) {
		return getColumnProperty(columnName, cColumnPropertyParentColumn);
		}

	public String getColumnSpecialType(String columnName) {
		return getColumnProperty(columnName, cColumnPropertySpecialType);
		}

	private String getColumnProperty(String columnName, String propertyName) {
		if (mColumnProperties == null)
			return null;

		for (int i=0; i<mColumnProperties.size(); i++) {
			if (mColumnProperties.get(i).startsWith(columnName+"\t")) {
				String properties = mColumnProperties.get(i).substring(columnName.length()+1);
				while (!properties.startsWith(propertyName)) {
					int index = properties.indexOf('\t');
					if (index != -1)
						index = properties.indexOf('\t', index+1);
					if (index == -1)
						return null;
					properties = properties.substring(index+1);
					}
				int index1 = properties.indexOf('\t');
				if (index1 != -1) {
					int index2 = properties.indexOf('\t', index1+1);
					return (index2 == -1) ? properties.substring(index1+1)
										  : properties.substring(index1+1, index2);
					}
				}
			}

		return null;
		}

	private void readHitlistData(BufferedReader theReader) {
		mHitlists = new ArrayList<String>();
		try {
			String hitlistName = null;
			StringBuffer hitlistData = null;
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cHitlistDataEnd)) {
					mHitlists.add(hitlistName + "\t" + hitlistData);
					break;
					}

				if (theLine.startsWith("<"+cHitlistName)) {
					if (hitlistName != null)
						mHitlists.add(hitlistName + "\t" + hitlistData);

					hitlistName = extractValue(theLine);
					hitlistData = new StringBuffer();
					continue;
					}

				if (theLine.startsWith("<"+cHitlistData)) {
					hitlistData.append(extractValue(theLine));
					continue;
					}
				}
			}
		catch (Exception e) {
			mHitlists = null;
			}
		}

	private void readDetailData(BufferedReader theReader) {
		mDetails = new HashMap<String,byte[]>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cDetailDataEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cDetailID)) {
					String detailID = extractValue(theLine);
					BinaryDecoder decoder = new BinaryDecoder(theReader);
					int size = decoder.initialize(8);
					byte[] detailData = new byte[size];
					for (int i=0; i<size; i++)
						detailData[i] = (byte)decoder.read();
					mDetails.put(detailID, detailData);
					}
				}
			}
		catch (Exception e) {
			mDetails = null;
			}
		}

	static public String extractValue(String theLine) {
		int index1 = theLine.indexOf("=\"") + 2;
		int index2 = theLine.indexOf("\"", index1);
		return theLine.substring(index1, index2);
		}

	private byte[] getBytes(String s) {
		return (s == null || s.length() == 0) ? null : s.getBytes();
		}

	private boolean readSDFile() {
		mProgressController.startProgress("Examining Records...", 0, 0);

		SDFileParser sdParser = new SDFileParser(mDataReader);
		String[] fieldNames = sdParser.getFieldNames();
		int fieldCount = fieldNames.length;

		mFieldNames = new String[fieldCount+3];
		mFieldNames[0] = "Structure";
		mFieldNames[1] = cColumnType2DCoordinates;
		mFieldNames[2] = "Molecule Name";   // use record no as default column
		for (int i=0; i<fieldCount; i++)
			mFieldNames[3+i] = normalizeColumnName(fieldNames[i]);

		ArrayList<Object[]> fieldDataList = new ArrayList<Object[]>();

		mOldVersionIDCodeColumn = 0;
		mOldVersionCoordinateColumn = 1;
		mCoordsMayBe3D = true;

		String structureProperties = cColumnPropertySpecialType
							   +"\t"+cColumnTypeIDCode;
		String coordinateProperties = cColumnPropertySpecialType
								+"\t"+cColumnType2DCoordinates
								+"\t"+cColumnPropertyParentColumn
								+"\tStructure";

		int structureIDColumn = (fieldCount != 0
	   						 && (fieldNames[0].equals("ID")
	   						  || fieldNames[0].equals("IDNUMBER")
	   						  || fieldNames[0].equals(COLUMN_TITLE_ACTELION_NO)
	   						  || fieldNames[0].equals("code"))) ? 3 : -1;

		// this takes preference
		for (int i=0; i<fieldCount; i++) {
			if (fieldNames[i].equals(COLUMN_TITLE_ACTELION_NO)
			 || fieldNames[i].equals("EMOL_VERSION_ID")) {
				structureIDColumn = 3 + i;
				}
			}

		sdParser = new SDFileParser(mFile, fieldNames);
		MolfileParser mfParser = new MolfileParser();
		StereoMolecule mol = new StereoMolecule();
		int recordNo = 0;
		int errors = 0;
		boolean molnameFound = false;
		boolean molnameIsDifferentFromFirstField = false;
		int recordCount = sdParser.getRowCount();

		mProgressController.startProgress("Processing Records...", 0, (recordCount != -1) ? recordCount : 0);

		while (sdParser.next()) {
			if (mProgressController.threadMustDie())
				break;
			if (recordCount != -1 && recordNo%PROGRESS_STEP == 0)
				mProgressController.updateProgress(recordNo);

			Object[] fieldData = new Object[mFieldNames.length];

			String molname = null;
			try {
				String molfile = sdParser.getNextMolFile();

				BufferedReader r = new BufferedReader(new StringReader(molfile));
				molname = r.readLine().trim();
				r.readLine();
				String comment = r.readLine();

				// exclude manually CCDC entries with atoms that are in multiple locations.
				if (comment.contains("From CSD data") && !comment.contains("No disordered atoms"))
					throw new Exception("CSD molecule with ambivalent atom location.");
					
				mfParser.parse(mol, molfile);
				if (mol.getAllAtoms() != 0) {
					mol.normalizeAmbiguousBonds();
					mol.canonizeCharge(true);
					Canonizer canonizer = new Canonizer(mol);
					canonizer.setSingleUnknownAsRacemicParity();
					byte[] idcode = getBytes(canonizer.getIDCode());
					byte[] coords = getBytes(canonizer.getEncodedCoordinates());
					fieldData[0] = idcode;
					fieldData[1] = coords;
					}
				}
			catch (Exception e) {
				errors++;
				}

			if (molname.length() != 0) {
				molnameFound = true;
				fieldData[2] = getBytes(molname);
				if (structureIDColumn != -1 && !molname.equals(removeTabs(sdParser.getFieldData(structureIDColumn - 3))))
					molnameIsDifferentFromFirstField = true;
				}

			for (int i=0; i<fieldCount; i++)
				fieldData[3+i] = getBytes(removeTabs(sdParser.getFieldData(i)));

		  /* IDCode conversion validation code
			if (mIDCode[recordNo] != null) {
				StereoMolecule testMol = new IDCodeParser().getCompactMolecule(mIDCode[recordNo], mCoordinates[recordNo]);
				Canonizer testCanonizer = new Canonizer(testMol);
				String testIDCode = testCanonizer.getIDCode();
				if (!testIDCode.equals(new String(mIDCode[recordNo]))) {
					new IDCodeParser().printContent(mIDCode[recordNo], null);
					new IDCodeParser().printContent(testIDCode.getBytes(), null);
					}
				else {
					recordNo--;
					}
				}
		   */

			fieldDataList.add(fieldData);
			recordNo++;
			}

		mFieldData = fieldDataList.toArray(new Object[0][]);

		if (structureIDColumn != -1) {
			structureProperties = structureProperties + "\t" + cColumnPropertyIdentifierColumn + "\t" + mFieldNames[structureIDColumn];
			}
		else if (molnameFound) {
			structureProperties = structureProperties + "\t" + cColumnPropertyIdentifierColumn + "\t" + mFieldNames[2];
			}
		else {
			mFieldNames[2] = "Structure No";
			for (int row=0; row<mFieldData.length; row++)
				mFieldData[row][2] = (""+(row+1)).getBytes();
			}

		mColumnProperties = new ArrayList<String>();
		mColumnProperties.add("Structure\t"+structureProperties);
		mColumnProperties.add(cColumnType2DCoordinates+"\t"+coordinateProperties);

		// if the molname column is redundant, then delete it
		if (structureIDColumn != -1 && (!molnameFound || !molnameIsDifferentFromFirstField)) {
			for (int column=3; column<mFieldNames.length; column++)
				mFieldNames[column-1] = mFieldNames[column];
			mFieldNames = Arrays.copyOf(mFieldNames, mFieldNames.length-1);

			for (int row=0; row<mFieldData.length; row++) {
				for (int column=3; column<mFieldData[row].length; column++)
					mFieldData[row][column-1] = mFieldData[row][column];
				mFieldData[row] = Arrays.copyOf(mFieldData[row], mFieldData[row].length-1);
				}
			}

		if (errors > 0) {
			final String message = ""+errors+" compound structures could not be generated because of molfile parsing errors.";
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			}

		addDefaultLookupColumnProperties();

		handlePotentially3DCoordinates();

		return true;
		}

	private String normalizeColumnName(String columnName) {
		if (columnName.equals("ACT_NO"))
			return COLUMN_TITLE_ACTELION_NO;
		if (columnName.equals("CHEM_LAB_JOURNAL"))
			return COLUMN_TITLE_ELN_EXTREF;
		if (columnName.equals("EXT_REFERENCE"))
			return COLUMN_TITLE_ELN_EXTREF;
		return columnName;
		}

	private String removeTabs(String s) {
		return (s == null) ? null : s.trim().replace('\t', ' ');
		}

	private void handlePotentially3DCoordinates() {
			// SD-Files or native DataWarrior files before version 2.7.0 may end up with
			// 2D- and/or 3D-coordinates in one column (cColumnType2DCoordinates).
			// If we have a mix of 2D and 3D, we need to add a new column and separate the data.
			// If we have 3D only, we need to change column properties accordingly.
		mOldVersionCoordinate3DColumn = -1;

		if (!mCoordsMayBe3D)
			return;

		mProgressController.startProgress("Checking for 3D-coordinates...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		boolean found2D = false;
		boolean found3D = false;
		IDCodeParser parser = new IDCodeParser(false);
		for (int row=0; row<mFieldData.length; row++) {
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			byte[] idcode = (byte[])mFieldData[row][mOldVersionIDCodeColumn];
			byte[] coords = (byte[])mFieldData[row][mOldVersionCoordinateColumn];
			if (idcode != null && coords != null) {
				if (parser.coordinatesAre3D(idcode, coords))
					found3D = true;
				else
					found2D = true;

				if (found2D && found3D)
					break;
				}
			}

		if (!found3D)
			return;

		if (!found2D) {
			mFieldNames[mOldVersionCoordinateColumn] = cColumnType3DCoordinates;
			for (int i=0; i<mColumnProperties.size(); i++) {
				if (mColumnProperties.get(i).startsWith(cColumnType2DCoordinates)) {
					mColumnProperties.set(i, cColumnType3DCoordinates
										+"\t"+cColumnPropertySpecialType
										+"\t"+cColumnType3DCoordinates
										+"\t"+cColumnPropertyParentColumn
										+"\tStructure");
					break;
					}
				}
			return;
			}

		mOldVersionCoordinate3DColumn = mFieldNames.length;
		mFieldNames = Arrays.copyOf(mFieldNames, mOldVersionCoordinate3DColumn+1);
		mFieldNames[mOldVersionCoordinate3DColumn] = cColumnType3DCoordinates;

		mProgressController.startProgress("Separating 2D- from 3D-coordinates...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		for (int row=0; row<mFieldData.length; row++) {
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			mFieldData[row] = Arrays.copyOf(mFieldData[row], mOldVersionCoordinate3DColumn+1);
			byte[] idcode = (byte[])mFieldData[row][mOldVersionIDCodeColumn];
			byte[] coords = (byte[])mFieldData[row][mOldVersionCoordinateColumn];
			if (idcode != null && coords != null) {
				if (parser.coordinatesAre3D(idcode, coords)) {
					mFieldData[row][mOldVersionCoordinate3DColumn] = mFieldData[row][mOldVersionCoordinateColumn];
					mFieldData[row][mOldVersionCoordinateColumn] = null;
					}
				}
			}
		mColumnProperties.add(cColumnType3DCoordinates
				+"\t"+cColumnPropertySpecialType
				+"\t"+cColumnType3DCoordinates
				+"\t"+cColumnPropertyParentColumn
				+"\tStructure");
		}

	private int populateTable() {
		mTableModel.initializeTable(mFieldData.length, mFieldNames.length);

		if (mExtensionMap != null)
			for (String name:mExtensionMap.keySet())
				mTableModel.setExtensionData(name, mExtensionMap.get(name));

		for (int column=0; column<mFieldNames.length; column++)
			mTableModel.setColumnName(mFieldNames[column], column);

		int rowCount = mFieldData.length;

		mProgressController.startProgress("Populating Table...", 0, (rowCount > PROGRESS_LIMIT) ? rowCount : 0);

		for (int row=0; row<rowCount; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (rowCount > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			for (int column=0; column<mFieldNames.length; column++)
				mTableModel.setTotalDataAt(mFieldData[row][column], row, column);
			}

		setColumnProperties(null);

		clearBufferedData();

		if (mDataType == FileHelper.cFileTypeDataWarrior)
			mTableModel.setFile(mFile);

		return rowCount;
		}

	private byte[] getIDCodeFromSmiles(byte[] smiles) {
		if (smiles.length > 0) {
			StereoMolecule mol = new StereoMolecule();
			try {
				new SmilesParser().parse(mol, smiles);
				mol.normalizeAmbiguousBonds();
				mol.canonizeCharge(true);
				Canonizer canonizer = new Canonizer(mol);
				canonizer.setSingleUnknownAsRacemicParity();
				return canonizer.getIDCode().getBytes();
				}
			catch (Exception e) {}
			}

		return null;
		}

	private byte[] convertNewlines(byte[] cellBytes) {
		int index = 0;
		for (int i=0; i<cellBytes.length; i++) {
			boolean found = false;
			if (i <= cellBytes.length-NEWLINE_BYTES.length) {
				found = true;
				for (int j=0; j<NEWLINE_BYTES.length; j++) {
					if (cellBytes[i+j] != NEWLINE_BYTES[j]) {
						found = false;
						break;
						}
					}
				}
			if (found) {
				cellBytes[index++] = NEWLINE;
				i += NEWLINE_BYTES.length-1;
				}
			else {
				cellBytes[index++] = cellBytes[i];
				}
			}

		if (index == cellBytes.length)
			return cellBytes;

		byte[] newBytes = new byte[index];
		for (int i=0; i<index; i++)
			newBytes[i] = cellBytes[i];

		return newBytes;
		}

	public void run() {
		try {
			boolean error = false;
			if ((mAction & READ_DATA) != 0)
				error = !readData();
	
			if ((mAction & REPLACE_DATA) != 0 && !error && !mProgressController.threadMustDie())
				replaceTable();
	
			if ((mAction & APPEND_DATA) != 0 && !error && !mProgressController.threadMustDie())
				appendTable();
	
			if ((mAction & MERGE_DATA) != 0 && !error && !mProgressController.threadMustDie())
				error = mergeTable();
	
			if (mOwnsProgressController) {
				((JProgressDialog)mProgressController).stopProgress();
				((JProgressDialog)mProgressController).close(mParentFrame);
				}
	
			if ((mAction & (REPLACE_DATA | APPEND_DATA | MERGE_DATA | APPLY_TEMPLATE)) != 0
			 && mRuntimeProperties != null
			 && !error)
				mRuntimeProperties.apply();
	
			finalStatus(!error);
			}
		catch (Throwable t) {
			t.printStackTrace();
			}

		mThread = null;
		}

	private boolean readData() {
			// returns true if successful
		clearBufferedData();

		try {
			switch (mDataType) {
			case FileHelper.cFileTypeDataWarriorTemplate:
				return readTemplateOnly();
			case FileHelper.cFileTypeDataWarrior:
			case FileHelper.cFileTypeTextTabDelimited:
			case FileHelper.cFileTypeTextCommaSeparated:
				return readTextData();
			case FileHelper.cFileTypeSD:
				return readSDFile();
				}
			}
		catch (OutOfMemoryError err) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, "Out of memory. Launch this application with Java option -Xms???m or -Xmx???m.");
					}
				} );
			clearBufferedData();
			return false;
			}
		return false;
		}

	private void replaceTable() {
		if (mDataType != FileHelper.cFileTypeDataWarriorTemplate) {
			mFirstNewColumn = 0;
			int rowCount = populateTable();

			if (mProgressController.threadMustDie()) {
				mTableModel.initializeTable(0, 0);
				if (mParentFrame != null)
					mParentFrame.setTitle("no data");
				}
			else {
				if (mParentFrame != null)
					mParentFrame.setTitle(mNewWindowTitle);

				mTableModel.finalizeTable(mRuntimeProperties != null
						   && mRuntimeProperties.size() != 0 ?
								   CompoundTableEvent.cSpecifierNoRuntimeProperties
								 : CompoundTableEvent.cSpecifierDefaultRuntimeProperties,
										  mProgressController);

				populateHitlists(rowCount, 0, null);
				populateDetails();
				}
			}
		}

	private void appendTable() {
		resolveDetailIDCollisions();

		mFirstNewColumn = mTableModel.getTotalColumnCount();
		int newDatasetNameColumns = (mAppendDatasetColumn == NEW_COLUMN) ? 1 : 0;
		int newColumns = newDatasetNameColumns;
		for (int i=0; i<mAppendDestColumn.length; i++)
			if (mAppendDestColumn[i] == NEW_COLUMN)
				newColumns++;

		if (newColumns != 0) {
			String[] columnName = new String[newColumns];
			if (newDatasetNameColumns != 0)
				columnName[0] = DATASET_COLUMN_TITLE;
			newColumns = newDatasetNameColumns;
			for (int i=0; i<mAppendDestColumn.length; i++)
				if (mAppendDestColumn[i] == NEW_COLUMN)
					columnName[newColumns++] = mFieldNames[i];

			int destinationColumn = mTableModel.addNewColumns(columnName);
			
			if (newDatasetNameColumns != 0) {
				mAppendDatasetColumn = destinationColumn++;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++)
					mTableModel.setTotalValueAt(mAppendDatasetNameExisting, row, mAppendDatasetColumn);
				}

			for (int i=0; i<mAppendDestColumn.length; i++)
				if (mAppendDestColumn[i] == NEW_COLUMN)
					mAppendDestColumn[i] = destinationColumn++;

			setColumnProperties(mAppendDestColumn);

			mTableModel.finalizeNewColumns(mFirstNewColumn, mProgressController);
			}

		if (mRuntimeProperties != null) // do this after finalizeNewColumns()
			mRuntimeProperties.learn(); // to also copy the new dataset filter

		int existingRowCount = mTableModel.getTotalRowCount();
		int additionalRowCount = mFieldData.length;
		mTableModel.addNewRows(additionalRowCount);

		mProgressController.startProgress("Appending rows...", 0, additionalRowCount);

		for (int row=0; row<additionalRowCount; row++) {
			int newRow = existingRowCount + row;

			if (mAppendDatasetColumn != NO_COLUMN)
				mTableModel.setTotalValueAt(mAppendDatasetNameNew, newRow, mAppendDatasetColumn);
			for (int column=0; column<mFieldNames.length; column++)
				if (mAppendDestColumn[column] != NO_COLUMN)
					mTableModel.setTotalDataAt(mFieldData[row][column], newRow, mAppendDestColumn[column]);

			mProgressController.updateProgress(row);
			}

		clearBufferedData();

		mTableModel.finalizeNewRows(existingRowCount, mProgressController);

		populateHitlists(additionalRowCount, existingRowCount, null);
		populateDetails();
		}

	private boolean mergeTable() {
		mProgressController.startProgress("Sorting current keys...", 0, mTableModel.getTotalRowCount());

		TreeMap<String,int[]> currentKeyMap = new TreeMap<String,int[]>();

		// construct key column array from mMergeMode
		int keyColumns = 0;
		for (int i=0; i<mMergeMode.length; i++)
			if (mMergeMode[i] == MERGE_MODE_IS_KEY)
				keyColumns++;
		int[] keyColumn = new int[keyColumns];
		keyColumns = 0;
		for (int i=0; i<mMergeMode.length; i++)
			if (mMergeMode[i] == MERGE_MODE_IS_KEY)
				keyColumn[keyColumns++] = i;

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (mProgressController.threadMustDie())
				break;
			if (row % PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			// create combined key from all key columns
			String key = mTableModel.getTotalValueAt(row, mMergeDestColumn[keyColumn[0]]);
			for (int i=1; i<keyColumns; i++)
				key = key.concat("\t").concat(mTableModel.getTotalValueAt(row, mMergeDestColumn[keyColumn[i]]));

			if (key != null && key.length() != 0) {
				int[] rowList = currentKeyMap.get(key);
				if (rowList == null) {
					rowList = new int[1];
					rowList[0] = row;
					}
				else {
					int[] oldRowList = rowList;
					rowList = new int[oldRowList.length+1];
					int i=0;
					for (int oldRow:oldRowList)
						rowList[i++] = oldRow;
					rowList[i] = row;
					}
				currentKeyMap.put(key, rowList);
				}
			}

		if (mProgressController.threadMustDie()) {
			resolveDetailIDCollisions();
			return true;
			}

		if (mProgressController.threadMustDie()) {
			clearBufferedData();
			return true;
			}

		int[][] destRowMap = null;
		if (mHitlists != null)
			destRowMap = new int[mFieldData.length][];

		if (mRuntimeProperties != null)
			mRuntimeProperties.learn();

		int newColumns = 0;
		for (int i=0; i<mMergeDestColumn.length; i++)
			if (mMergeDestColumn[i] == NEW_COLUMN)
				newColumns++;

		mFirstNewColumn = mTableModel.getTotalColumnCount();
		if (newColumns != 0) {
			mProgressController.startProgress("Merging data...", 0, mFieldData.length);

			String[] columnName = new String[newColumns];
			newColumns = 0;
			for (int i=0; i<mMergeDestColumn.length; i++)
				if (mMergeDestColumn[i] == NEW_COLUMN)
					columnName[newColumns++] = mFieldNames[i];

			int destinationColumn = mTableModel.addNewColumns(columnName);
			for (int i=0; i<mMergeDestColumn.length; i++)
				if (mMergeDestColumn[i] == NEW_COLUMN)
					mMergeDestColumn[i] = destinationColumn++;
			}

		int mergedColumns = 0;
		for (int row=0; row<mFieldData.length; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (row % PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			byte[] key = constructMergeKey(mFieldData[row], keyColumn);
			if (key != null) {
				int[] rowList = currentKeyMap.get(new String(key));
				if (rowList != null) {
					for (int destRow:rowList) {
						// In case we have child columns with merge mode MERGE_MODE_AS_PARENT, we need to handle them first.
						for (int column=0; column<mMergeDestColumn.length; column++) {
							if (mMergeDestColumn[column] != NO_COLUMN) {
								if (mMergeMode[column] == MERGE_MODE_AS_PARENT) {
									int parentColumn = getSourceColumn(getParentColumnName(mFieldNames[column]));
									if (mTableModel.getTotalRecord(destRow).getData(mMergeDestColumn[parentColumn]) == null)
										mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);
									}
								}
							}
						for (int column=0; column<mMergeDestColumn.length; column++) {
							if (mMergeDestColumn[column] != NO_COLUMN) {
								switch (mMergeMode[column]) {
								case MERGE_MODE_APPEND:
									mTableModel.appendTotalDataAt((byte[])mFieldData[row][column], destRow, mMergeDestColumn[column]);
									break;
								case MERGE_MODE_REPLACE:
									mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);
									break;
								case MERGE_MODE_USE_IF_EMPTY:
									if (mTableModel.getTotalRecord(destRow).getData(mMergeDestColumn[column]) == null)
										mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);
									break;
								default:	// merge key don't require handling and child column merging was handled before
									break;
									}
								}
							}
						}

					if (destRowMap != null)
						destRowMap[row] = rowList;
	
					mFieldData[row] = null;
					mergedColumns++;
					}
				}
			}

		if (newColumns != 0) {
			setColumnProperties(mMergeDestColumn);

			mTableModel.finalizeNewColumns(mFirstNewColumn, mProgressController);
			}

		if (mProgressController.threadMustDie()) {
			clearBufferedData();
			return true;
			}

		final int existingRowCount = mTableModel.getTotalRowCount();
		final int additionalRowCount = mFieldData.length - mergedColumns;
		if (mAppendRest && additionalRowCount > 0) {

			mTableModel.addNewRows(additionalRowCount);

			int destRow = existingRowCount;

			mProgressController.startProgress("Appending remaining...", 0, additionalRowCount);

			for (int row=0; row<mFieldData.length; row++) {
				if (mFieldData[row] == null)
					continue;

				if (mProgressController.threadMustDie())
					break;

				for (int column=0; column<mMergeDestColumn.length; column++)
					if (mMergeDestColumn[column] != NO_COLUMN)
						mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);

				if (destRowMap != null) {
					destRowMap[row] = new int[1];
					destRowMap[row][0] = destRow;
					}

				mProgressController.updateProgress(destRow - existingRowCount);

				destRow++;
				}
			}

		clearBufferedData();

		if (mAppendRest && additionalRowCount > 0)
			mTableModel.finalizeNewRows(existingRowCount, mProgressController);

		if (destRowMap != null)
			populateHitlists(destRowMap.length, -1, destRowMap);

		populateDetails();

		return false;
		}

	/**
	 * If we append or merge we need to translate column indexes from source to destination tables.
	 * @param appendOrMergeColumn null or source to destination column mapping
	 */
	private void setColumnProperties(int[] appendOrMergeDestColumn) {
		if (mColumnProperties == null)
			return;

		for (String propertyString:mColumnProperties) {
			int index1 = propertyString.indexOf('\t');
			String columnName = propertyString.substring(0, index1);

			int column = getSourceColumn(columnName);
			if (column != NO_COLUMN
			 && appendOrMergeDestColumn != null)
				column = appendOrMergeDestColumn[column];

			if (column != NO_COLUMN
			 && column >= mFirstNewColumn) {
				while (index1 != -1) {
					int index2 = propertyString.indexOf('\t', index1+1);
					String key = propertyString.substring(index1+1, index2);
					index1 = propertyString.indexOf('\t', index2+1);
					String value = (index1 == -1) ?
										propertyString.substring(index2+1)
									  : propertyString.substring(index2+1, index1);

						// in case of merge/append column property references
						// to parent columns may need to be translated
					if (appendOrMergeDestColumn != null) {
						if (key.equals(cColumnPropertyParentColumn)) {
							int parentColumn = appendOrMergeDestColumn[getSourceColumn(value)];
							if (parentColumn == NO_COLUMN)	// visible columns that have a parent (e.g. cluster no)
								value = null;
							else
								value = mTableModel.getColumnTitleNoAlias(parentColumn);
							}
						}

					mTableModel.setColumnProperty(column, key, value);
					}
				}
			}
		}

	private int getSourceColumn(String columnName) {
		for (int j=0; j<mFieldNames.length; j++)
			if (columnName.equals(mFieldNames[j]))
				return j;
		return NO_COLUMN;
		}

	private void populateHitlists(int rowCount, int offset, int[][] destRowMap) {
			// use either offset or destRowMap to indicate mapping of original hitlists to current rows
		if (mHitlists != null) {
			CompoundTableHitlistHandler hitlistHandler = mTableModel.getHitlistHandler();
			for (int list=0; list<mHitlists.size(); list++) {
				String listString = mHitlists.get(list);
				int index = listString.indexOf('\t');
				String name = listString.substring(0, index);
				byte[] data = new byte[listString.length()-index-1];
				for (int i=0; i<data.length; i++)
					data[i] = (byte)(listString.charAt(++index) - 64);

				String uniqueName = hitlistHandler.createHitlist(name, -1, CompoundTableHitlistHandler.EMPTY_LIST, -1, null);
				int flagNo = hitlistHandler.getHitlistFlagNo(uniqueName);
				int dataBit = 1;
				int dataIndex = 0;
				for (int row=0; row<rowCount; row++) {
					if ((data[dataIndex] & dataBit) != 0) {
						if (destRowMap == null)
							mTableModel.getTotalRecord(row+offset).setFlag(flagNo);
						else
							for (int destRow:destRowMap[row])
								mTableModel.getTotalRecord(destRow).setFlag(flagNo);
						}
					dataBit *= 2;
					if (dataBit == 64) {
						dataBit = 1;
						dataIndex++;
						}
					}
				}
			}
		}

	private void populateDetails() {
		CompoundTableDetailHandler detailHandler = mTableModel.getDetailHandler();
		if (detailHandler != null)
			detailHandler.setEmbeddedDetailMap(mDetails);
		}

	private void resolveDetailIDCollisions() {
		if (mDetails != null && mTableModel.getDetailHandler().getEmbeddedDetailCount() != 0) {
						// Existing data as well a new data have embedded details.
						// Adding an offset to the IDs of existing details ensures collision-free merging/appending.
			int highID = 0;
			Iterator<String> iterator = mDetails.keySet().iterator();
			while (iterator.hasNext()) {
				try {
					int id = Math.abs(Integer.parseInt(iterator.next()));
					if (highID < id)
						highID = id;
					}
				catch (NumberFormatException nfe) {}
				}

			if (highID != 0)
				mTableModel.addOffsetToEmbeddedDetailIDs(highID);
			}
		}

	private void clearBufferedData() {
		mFieldNames = null;
		mFieldData = null;
		mColumnProperties = null;
		mAppendDestColumn = null;
		mMergeDestColumn = null;
		}

	/**
	 * Set column properties for biological lookup on column containing 'Actelion No's
	 * and optionally defines this column to be the identifier column for a given structure column.
	 * @param tableModel
	 * @param actNoColumn column containing the Actelion numbers
	 * @param -1 or column containing structures that represent the Actelion numbers
	 */
	public static void setDefaultActelionNoColumnProperties(CompoundTableModel tableModel, int actNoColumn, int structureColumn) {
		tableModel.setColumnProperty(actNoColumn, CompoundTableModel.cColumnPropertyLookupCount, "1");
		tableModel.setColumnProperty(actNoColumn, CompoundTableModel.cColumnPropertyLookupName+"0", LOOKUP_NAME_BIORESULTS);
		tableModel.setColumnProperty(actNoColumn, CompoundTableModel.cColumnPropertyLookupURL+"0", LOOKUP_URL_BIORESULTS);
		if (structureColumn != -1)
			tableModel.setColumnProperty(structureColumn, CompoundTableModel.cColumnPropertyIdentifierColumn, tableModel.getColumnTitleNoAlias(actNoColumn));
		}

	/**
	 * Set column properties for Niobe's ELB entry lookup on column containing 'ELB's
	 * @param tableModel
	 * @param elbColumn column containing the ELBs
	 */
	public static void setDefaultELBColumnProperties(CompoundTableModel tableModel, int elbColumn) {
		tableModel.setColumnProperty(elbColumn, CompoundTableModel.cColumnPropertyLookupCount, "1");
		tableModel.setColumnProperty(elbColumn, CompoundTableModel.cColumnPropertyLookupName+"0", LOOKUP_NAME_ELB);
		tableModel.setColumnProperty(elbColumn, CompoundTableModel.cColumnPropertyLookupURL+"0", LOOKUP_URL_ELB);
		}

	/**
	 * This function serves as a callback function to report the success when the loader thread is done.
	 * Overwrite this, if you need the status after loading.
	 * @param success true if file was successfully read
	 */
	public void finalStatus(boolean success) {}
	}
