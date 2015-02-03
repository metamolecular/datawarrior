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

import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorHandlerFactory;
import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.chem.descriptor.DescriptorHandlerReactionIndex;
import com.actelion.research.chem.descriptor.DescriptorHandlerStandardFactory;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.table.category.CategoryList;
import com.actelion.research.table.category.DateCategoryNormalizer;
import com.actelion.research.table.category.DefinedCategoryList;
import com.actelion.research.table.category.DoubleCategoryNormalizer;
import com.actelion.research.table.category.PlainCategoryNormalizer;
import com.actelion.research.table.category.RangeCategoryList;
import com.actelion.research.table.category.SortedCategoryList;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.DoubleFormat;
import com.actelion.research.util.UniqueList;

public class CompoundTableModel extends AbstractTableModel
			implements CompoundTableConstants,DescriptorConstants,TableModel {
	private static final long serialVersionUID = 0x20060821;

		// visible standard columns
	private static final int cColumnTypeString = 0x0100;
	private static final int cColumnTypeDouble = 0x0200;
	private static final int cColumnTypeDate = 0x0400;  // date is subtype of float
	private static final int cColumnTypeCategory = 0x0800;
	private static final int cColumnTypeRangeCategory = 0x1800;
		// Range category columns are created as a result of a binning process.
		// They are never analyzed, i.e. neither after record removal nor after save and load
		// Their category lists are, once created, never changed or sorted

		// a comma (',') cannot be a delimiter, because it would interfere with the date format
	private static final String cSeparatorRegex = cEntrySeparator+"|"+cLineSeparator.replace("\n", "\\n")+"| *; *";

	public static final int ATOM_COLOR_MODE_NONE = 0;
	public static final int ATOM_COLOR_MODE_EXPLICIT = 1;
	public static final int ATOM_COLOR_MODE_ALL = 2;

	private CompoundTableHitlistHandler mHitlistHandler;
	private CompoundTableDetailHandler  mDetailHandler;
	private CompoundTableExtensionHandler  mExtensionHandler;
	private ArrayList<ProgressListener> mProgressListener;
	private ArrayList<CompoundTableListener> mCompoundTableListener;
	private ArrayList<HighlightListener>   mHighlightListener;
	private TreeMap<String,Object> mTableExtensionMap;
	private CompoundRecord[]	mRecord,mNonExcludedRecord,mSMPRecord;
	private CompoundRecord		mHighlightedRow,mActiveRow;
	private File				mFile;
	private long				mAllocatedExclusionFlags,mAllocatedCompoundFlags,
								mDirtyCompoundFlags;
	private int					mLastSortColumn,mParseDoubleValueCount,
								mColumns,mRecords,mNonExcludedRecords;
	private int[]				mDisplayableColumnToColumn,mColumnToDisplayableColumn;
	private String				mParseDoubleModifier;
	private float[]				mSimilarityListSMP;
	private CompoundTableColumnInfo[] mColumnInfo;
	private volatile boolean	mSMPProcessWaiting,mSMPStopDescriptorCalculation;
	private AtomicInteger		mSMPRecordIndex,mSMPWorkingThreads,mSSSRecordIndex;
	private DescriptorColumnSpec[] mSMPColumnSpec;
	private int					mSMPErrorCount;
	private AtomicBoolean		mLock;

	/**
	 * This is the single point of assigning DescriptorHandlers to shortNames and, thus, defines
	 * the list of known descriptors for CompoundTableModel based applications.
	 * @param shortName
	 * @return
	 */
	public static DescriptorHandler getDefaultDescriptorHandler(String shortName) {
		return getDefaultDescriptorHandlerFactory().getDefaultDescriptorHandler(shortName);
		}

	/**
	 * This is the DescriptorHandlerFactory used by all CompoundTableModel related applications.
	 * It defines the Descriptors known, i.e. those that are available to the users.
	 * @return
	 */
	public static DescriptorHandlerFactory getDefaultDescriptorHandlerFactory() {
		return DescriptorHandlerStandardFactory.getFactory();
		}


	public CompoundTableModel() {
		mProgressListener = new ArrayList<ProgressListener>();
		mCompoundTableListener = new ArrayList<CompoundTableListener>();
		mHighlightListener = new ArrayList<HighlightListener>();
		mLastSortColumn = -1;
		mLock = new AtomicBoolean(false);
		}

	public void addProgressListener(ProgressListener l) {
		mProgressListener.add(l);
		}

	public void addCompoundTableListener(CompoundTableListener l) {
		mCompoundTableListener.add(l);
		}

	public void addHighlightListener(HighlightListener l) {
		mHighlightListener.add(l);
		}

	public void removeProgressListener(ProgressListener l) {
		mProgressListener.remove(l);
		}

	public void removeCompoundTableListener(CompoundTableListener l) {
		mCompoundTableListener.remove(l);
		}

	public void removeHighlightListener(HighlightListener l) {
		mHighlightListener.remove(l);
		}

	public CompoundTableHitlistHandler getHitlistHandler() {
		return mHitlistHandler;
		}

	public void setHitlistHandler(CompoundTableHitlistHandler h) {
		mHitlistHandler = h;
		}

	public CompoundTableExtensionHandler getExtensionHandler() {
		return mExtensionHandler;
		}

	public void setExtensionHandler(CompoundTableExtensionHandler h) {
		mExtensionHandler = h;
		}

	public CompoundTableDetailHandler getDetailHandler() {
		return mDetailHandler;
		}

	public void setDetailHandler(CompoundTableDetailHandler h) {
		mDetailHandler = h;
		}

	/**
	 * Provides a thread-safe exclusive lock mechanism for new table loading.
	 * The lock is automatically unlocked after finalizeTable() is called.
	 * @return true if was not already locked
	 */
	public boolean lock() {
		return mLock.compareAndSet(false, true);
		}

	/**
	 * Provides a thread-safe lock mechanism for any purpose.
	 */
	public void unlock() {
		mLock.set(false);
		}

	/**
	 * @return true if there are no columns, no rows and no extension data
	 */
	public boolean isEmpty() {
		return (mTableExtensionMap == null || mTableExtensionMap.isEmpty())
				&& mColumns == 0
				&& mRecords == 0;
		}

	/**
	 * Initialize the content of the table model
	 * to an empty table of the given size.
	 * This method may be called from any thread.
	 * @param rows
	 * @param columns
	 */
	public void initializeTable(final int rows, final int columns) {
		if (SwingUtilities.isEventDispatchThread()) {
			initTableEDT(rows, columns);
			}
		else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						initTableEDT(rows, columns);
						}
					} );
				}
			catch (Exception ite) {}
			}
		}

	private void initTableEDT(int rows, int columns) {
		stopDescriptorCalculation();

		boolean fireEvents = (mColumns != 0 || mRecords != 0);

		mColumns = 0;
		mRecords = 0;
		mNonExcludedRecords = 0;
		mAllocatedCompoundFlags = 0;
		mAllocatedExclusionFlags = 0;

		mDisplayableColumnToColumn = null;
		mColumnToDisplayableColumn = null;

		mHighlightedRow = null;
		mActiveRow = null;

		mFile = null;
		mTableExtensionMap = null;

		if (mHitlistHandler != null)
			mHitlistHandler.clearHitlistData();

		if (mDetailHandler != null)
			mDetailHandler.clearDetailData();

		if (fireEvents)
			fireEvents(new CompoundTableEvent(this,
								CompoundTableEvent.cNewTable,
								CompoundTableEvent.cSpecifierNoRuntimeProperties),
					   new TableModelEvent(this, TableModelEvent.HEADER_ROW));

		mRecord = new CompoundRecord[rows];
		mNonExcludedRecord = new CompoundRecord[rows];
		for (int i=0; i<rows; i++)
			mRecord[i] = new CompoundRecord(i, columns);

		mColumnInfo = new CompoundTableColumnInfo[columns];
		for (int i=0; i<columns; i++)
			mColumnInfo[i] = new CompoundTableColumnInfo(null);
		}

	/**
	 * Analyzes all columns after initializing a table model and
	 * after updating its data content. After the data analysis
	 * proper events are sent to inform listeners that the content has changed.
	 * This method may be called from any thread.
	 * @param specifier
	 * @param listener
	 */
	public void finalizeTable(final int specifier, final ProgressListener listener) {
		if (SwingUtilities.isEventDispatchThread()) {
			finalizeTableEDT(specifier, listener);
			}
		else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						finalizeTableEDT(specifier, listener);
						}
					} );
				}
			catch (Exception ite) {}
			}
		}

	private void finalizeTableEDT(int specifier, ProgressListener listener) {
		for (int i=0; i<mColumnInfo.length; i++)
			if (mColumnInfo[i].name == null)
				mColumnInfo[i].name = validateColumnName(mColumnInfo[i].name, null, -1, this);

		allocateDefaultDescriptorColumns();
		analyzeData(0, listener);
		createDisplayableColumnMap();
		compileVisibleRecords();
		CompoundTableEvent cte = new CompoundTableEvent(this, CompoundTableEvent.cNewTable, specifier);
		TableModelEvent tme = new TableModelEvent(this, TableModelEvent.HEADER_ROW);
		fireEvents(cte, tme);
		unlock();
		}

	private void createDisplayableColumnMap() {
		int columns = 0;
		mColumnToDisplayableColumn = new int[mColumnInfo.length];
		for (int column=0; column<mColumnInfo.length; column++)
			if (isColumnDisplayable(column))
				columns++;
		mDisplayableColumnToColumn = new int[columns];
		columns = 0;
		for (int column=0; column<mColumnInfo.length; column++) {
			if (isColumnDisplayable(column)) {
				mColumnToDisplayableColumn[column] = columns;
				mDisplayableColumnToColumn[columns++] = column;
				}
			else {
				mColumnToDisplayableColumn[column] = -1;
				}
			}
		}

	public int convertFromDisplayableColumnIndex(int visibleColumnIndex) {
		return (mDisplayableColumnToColumn == null
			 || visibleColumnIndex == -1) ?
//		   	 || visibleColumnIndex >= mDisplayableColumnToColumn.length) ?
				-1 : mDisplayableColumnToColumn[visibleColumnIndex];
		}

	public int convertToDisplayableColumnIndex(int totalColumnIndex) {
		return (totalColumnIndex == -1) ? -1 : mColumnToDisplayableColumn[totalColumnIndex];
		}

	/**
	 * returns the count of displayable(!!!) columns.
	 */
	public int getColumnCount() {
			// Caution: column count refers to visible columns only!!!
		return (mDisplayableColumnToColumn == null) ? 0 : mDisplayableColumnToColumn.length;
		}

	/**
	 * returns the count of all(!!!) columns.
	 */
	public int getTotalColumnCount() {
		return mColumns;
		}

	/**
	 * Returns display value. Multiple numbers may be summarized if the
	 * columns display mode is not cDisplayModeNormal (e.g. mean, max, sum)
	 * In case of logarithmic view mode a mean value is a geometric mean.
	 * @param row is index in non excluded row list
	 * @param column is index in visible(!!!) column list
	 */
	public Object getValueAt(int row, int column) {
		return getValue(mNonExcludedRecord[row], mDisplayableColumnToColumn[column]);
		}

	/**
	 * Returns display value. Multiple numbers may be summarized if the
	 * columns display mode is not cSummaryModeNormal (e.g. mean, max, sum).
	 * In case of logarithmic view mode a mean value is a geometric mean.
	 * @param record
	 * @param column is index in total column list
	 * @return
	 */
	public String getValue(CompoundRecord record, int column) {
		if ((mColumnInfo[column].type & cColumnTypeDouble) != 0) {
			if (mColumnInfo[column].summaryMode != cSummaryModeNormal) {
				String[] entry = separateEntries(encodeData(record, column));
				if (entry.length > 1) {
					if ((mColumnInfo[column].type & cColumnTypeDate) != 0)
						return DateFormat.getDateInstance().format(new Date(
								86400000*(long)record.mFloat[column]+43200000))+getSummaryModeString(mColumnInfo[column].summaryMode, entry.length);
	
					float value = (mColumnInfo[column].logarithmicViewMode) ?
							(float)Math.pow(10.0, record.mFloat[column])
						  : record.mFloat[column];
	
					String numPart = (mColumnInfo[column].significantDigits == 0) ?
							  DoubleFormat.toString(value)
							: DoubleFormat.toString(value, mColumnInfo[column].significantDigits);
	
					try {   // this is to generate modifier and value count
						tryParseDouble(encodeData(record, column), column);
						} catch (NumberFormatException nfe) {}
	
					return mColumnInfo[column].summaryCountHidden ? mParseDoubleModifier+numPart
							: mParseDoubleModifier+numPart+getSummaryModeString(mColumnInfo[column].summaryMode, mParseDoubleValueCount);
					}
				}

			if (mColumnInfo[column].significantDigits != 0
			 || mColumnInfo[column].excludeModifierValues) {
				String[] entry = separateEntries(encodeData(record, column));
				String[] separator = getEntrySeparators(encodeData(record, column), entry);
				StringBuilder buf = null;
				for (int i=0; i<entry.length; i++) {
					if (entry[i].length() != 0) {
						EntryAnalysis analysis = new EntryAnalysis(entry[i]);
						if (mColumnInfo[column].significantDigits != 0
						 && !analysis.isNaN()) {
							try {
								float value = Float.parseFloat(analysis.getValue());
								entry[i] = analysis.getModifier()
										 + DoubleFormat.toString(value, mColumnInfo[column].significantDigits);
								}
							catch (NumberFormatException nfe) {}
							}
	
						if (mColumnInfo[column].excludeModifierValues
						 && analysis.hasModifier())
							entry[i] = "("+entry[i]+")";
						}
	
					if (entry.length == 1)
						return entry[0];
	
					if (i == 0)
						buf = new StringBuilder(entry[i]);
					else
						buf.append(separator[i-1]).append(entry[i]);
					}
				return buf.toString();
				}
			}

		return encodeData(record, column);
		}

	public String getSummaryModeString(int mode, int valueCount) {
		switch (mode) {
		case cSummaryModeMean:
			return " (mean, n="+valueCount+")";
		case cSummaryModeMedian:
			return " (median, n="+valueCount+")";
		case cSummaryModeMinimum:
			return " (min of "+valueCount+")";
		case cSummaryModeMaximum:
			return " (max of "+valueCount+")";
		case cSummaryModeSum:
			return " (sum of "+valueCount+")";
		default:
			return "";
			}
		}

	/**
	 * Returns a summarized numerical value reflecting one or more entries of the cell.
	 * In case of logarithmic view mode, this is the logarithmic value!!!.
	 * If the column type is neither float nor date, or the cell is empty, NaN is returned.
	 * @param row index referring to visible rows
	 * @param column index referring to all columns
	 * @return 
	 */
	public float getDoubleAt(int row, int column) {
		return mNonExcludedRecord[row].mFloat[column];
		}

	public int getRowCount() {
		return mNonExcludedRecords;
		}

	/**
	 * Returns a summarized numerical value reflecting one or more entries of the cell.
	 * In case of logarithmic view mode, this is the logarithmic value!!!.
	 * If the column type is neither float nor date, or the cell is empty, NaN is returned.
	 * @param row index referring to all rows
	 * @param column index referring to all columns
	 * @return 
	 */
	public float getTotalDoubleAt(int row, int column) {
		return mRecord[row].mFloat[column];
		}

	public String getTotalValueAt(int row, int column) {
		return encodeData(mRecord[row], column);
		}

	/**
	 * Returns a summarized numerical value reflecting one or more entries of the cell.
	 * The view mode (logarithmic or not) does not affect the returned value.
	 * If the column type is neither float nor date, or the cell is empty, NaN is returned.
	 * @param row index referring to all rows
	 * @param column index referring to all columns
	 * @return 
	 */
	public float getTotalOriginalDoubleAt(int row, int column) {
		float value = mRecord[row].mFloat[column];
		return isLogarithmicViewMode(column) ? (float)Math.pow(10.0, value) : value;
		}

	public String encodeDataWithDetail(CompoundRecord record, int column) {
		if (!mColumnInfo[column].hasDetail)
			return encodeData(record, column);

		String[][] detailRef = record.getDetailReferences(column);
		if (detailRef == null)
			return encodeData(record, column);

		String detailSeparator = getColumnProperty(column, cColumnPropertyDetailSeparator);
		if (detailSeparator == null)
			detailSeparator = cDefaultDetailSeparator;

		StringBuilder valueWithDetail = new StringBuilder(encodeData(record, column));
		for (int i=0; i<detailRef.length; i++)
			if (detailRef[i] != null)
				for (int j=0; j<detailRef[i].length; j++)
					valueWithDetail.append(detailSeparator+i+cDetailIndexSeparator+detailRef[i][j]);

		return valueWithDetail.toString();
		}

	/**
	 * Creates a StereoMolecule from this record's content at the specified column. If a molecule is passed,
	 * then this is used as a container; otherwise a new StereoMolecule is created.
	 * If record does not contain a molecule, then null is returned even if mol is not null.
	 * The atomColorMode defines whether and to which extend atoms are in color. For displaying mol on the
	 * screen use ATOM_COLOR_MODE_ALL, for printing use ATOM_COLOR_MODE_EXPLICIT and for copy/paste,
	 * drag&drop, cheminformatics purposes, etc use ATOM_COLOR_MODE_NONE.
	 * If atomColorMode is ATOM_COLOR_MODE_ALL, then molecule itself may be modified(!) depending on current display
	 * settings, e.g. to highlight structural differences to the reference molecule.
	 * @param record
	 * @param column
	 * @param atomColorMode one of ATOM_COLOR_MODE_...
	 * @param mol null or a StereoMolecule to be filled
	 * @return null, if record doesn't contain molecule information
	 */
	public StereoMolecule getChemicalStructure(CompoundRecord record, int column, int atomColorMode, StereoMolecule mol) {
		if (record == null || column == -1)
			return null;

		if (getColumnSpecialType(column).equals(cColumnTypeIDCode)) {
			byte[] idcode = (byte[])record.getData(column);
			if (idcode != null) {
				int index1 = 0;
				while (index1<idcode.length && idcode[index1] != '\n')
					index1++;
				if (index1 != idcode.length) {
					if (mol == null)
						mol = new StereoMolecule();
					new IDCodeParser(true).parse(mol, idcode);
					do {
						int index2 = index1+1;
						while (index2<idcode.length && idcode[index2] != '\n')
							index2++;
						byte[] subIDCode = new byte[index2-index1-1];
						System.arraycopy(idcode, index1+1, subIDCode, 0, index2-index1-1);
						mol.addMolecule(new IDCodeParser(true).getCompactMolecule(subIDCode));
						index1 = index2;
						} while (index1 != idcode.length);
					new CoordinateInventor().invent(mol);
					return mol;
					}

				int coordsColumn = getChildColumn(column, cColumnType2DCoordinates);
				byte[] coords = (byte[])record.getData(coordsColumn);
				if (mol == null)
					mol = new IDCodeParser(true).getCompactMolecule(idcode, coords);
				else
					new IDCodeParser(true).parse(mol, idcode, coords);
				String identifierColumnName = getColumnProperty(column, cColumnPropertyIdentifierColumn);
				if (identifierColumnName != null && mol != null) {
					int identifierColumn = findColumn(identifierColumnName);
					if (identifierColumn != -1) {
						byte[] data = (byte[])record.getData(identifierColumn);
						if (data != null && data.length != 0)
							mol.setName(new String(data));
						}
					}
			   	colorizeAtoms(record, column, atomColorMode, mol);
				return mol;
				}
			}
		else if (getColumnSpecialType(column).equals(cColumnType3DCoordinates)) {
			int idcodeColumn = getParentColumn(column);
			byte[] idcode = (byte[])record.getData(idcodeColumn);
			if (idcode != null) {
				byte[] coords = (byte[])record.getData(column);
				if (mol == null)
					mol = new IDCodeParser(false).getCompactMolecule(idcode, coords);
				else
					new IDCodeParser(false).parse(mol, idcode, coords);
				if (mol != null) {
					String identifier = getIdentifier(record, idcodeColumn);
					if (identifier != null)
						mol.setName(identifier);
					}
				return mol;
				}
			}

		return null;
		}

	/**
	 * Returns the identifier of a parent object from an identifier column, if 
	 * this column exists and contains an identifier.
	 * @param record
	 * @param parentColumn e.g. column containing idcodes
	 * @return null or identifier, e.g. molecule name
	 */
	public String getIdentifier(CompoundRecord record, int parentColumn) {
		String identifierColumnName = getColumnProperty(parentColumn, cColumnPropertyIdentifierColumn);
		if (identifierColumnName != null) {
			int identifierColumn = findColumn(identifierColumnName);
			if (identifierColumn != -1) {
				byte[] data = (byte[])record.getData(identifierColumn);
				if (data != null && data.length != 0)
					return new String(data);
				}
			}
		return null;
		}

	/**
	 * Creates a Reaction object from this record's content at the specified column.
	 * If record does not contain a reaction, then null is returned.
	 * @param record
	 * @param column
	 * @return null, if record doesn't contain reaction information
	 */
	public Reaction getChemicalReaction(CompoundRecord record, int column) {
		if (record == null || column == -1)
			return null;

		if (getColumnSpecialType(column).equals(cColumnTypeRXNCode)) {
			byte[] rxncode = (byte[])record.getData(column);
			if (rxncode != null) {
				return ReactionEncoder.decode(new String(rxncode), true);
				}
			}

		return null;
		}

	/**
	 * Updates an idcode column, 2D-coordinates (if existing) and atom colors (if existing)
	 * from a StereoMolecule. If the idcode changes, then all other child columns of the idcode column
	 * are cleared.
	 * If this method returns true, then call either finalizeChangeCell() after setting one chemistry object
	 * or call finalizeChangeColumn() for the idcode column after setting multiple ones.
	 * @param record
	 * @param mol
	 * @param column idcode column
	 * @return true if at least one of idcode, 2D-coords or atom coloring was updated
	 */
	public boolean setChemicalStructure(CompoundRecord record, StereoMolecule mol, int column) {
		int oldAtoms = new IDCodeParser(false).getAtomCount((byte[])record.getData(column), 0);
		if (oldAtoms != 0 || mol.getAllAtoms() != 0) {
			byte[] newIDCode = null;
			byte[] newCoords = null;
			byte[] newColors = null;
			if (mol.getAllAtoms() != 0) {
				Canonizer canonizer = new Canonizer(mol);
				newIDCode = canonizer.getIDCode().getBytes();
				newCoords = canonizer.getEncodedCoordinates().getBytes();
				int colorInfoColumn = getChildColumn(column, CompoundTableConstants.cColumnTypeAtomColorInfo);
				if (colorInfoColumn != -1)
					newColors = compileAtomColors(mol, canonizer.getGraphIndexes());
				}

			boolean idcodeChanged = false;
			ByteArrayComparator comparator = new ByteArrayComparator();
			if (comparator.compare(newIDCode, (byte[])record.getData(column)) != 0) {
				record.setData(newIDCode, column, true);
				idcodeChanged = true;
				}

			boolean somethingChanged = idcodeChanged;
			for (int childColumn=0; childColumn<getTotalColumnCount(); childColumn++) {
				if (getParentColumn(childColumn) == column) {
					if (cColumnType2DCoordinates.equals(getColumnSpecialType(childColumn))) {
						if (comparator.compare(newCoords, (byte[])record.getData(childColumn)) != 0)
							record.setData(newCoords, childColumn);
						}
					else if (cColumnTypeAtomColorInfo.equals(getColumnSpecialType(childColumn))) {
						if (comparator.compare(newColors, (byte[])record.getData(childColumn)) != 0)
							record.setData(newColors, childColumn);
						}
					else if (idcodeChanged) {
						record.setData(null, childColumn);
						}
					}
				}
			return somethingChanged;
			}

		return false;
		}

	/**
	 * Creates the atom color String as byte[], that lists all atoms if a parent idcode,
	 * which carry an explicit color.
	 * @param mol
	 * @param graphIndex the graphindex a Canonizer instantiated with mol
	 * @return atom color info to be used in cColumnTypeAtomColorInfo column
	 */
	private byte[] compileAtomColors(StereoMolecule mol, int[] graphIndex) {
		StringBuilder sb = new StringBuilder();
		compileAtomColors(mol, graphIndex, sb, "red:", Molecule.cAtomColorRed);
		compileAtomColors(mol, graphIndex, sb, "blue:", Molecule.cAtomColorBlue);
		compileAtomColors(mol, graphIndex, sb, "green:", Molecule.cAtomColorGreen);
		compileAtomColors(mol, graphIndex, sb, "magenta:", Molecule.cAtomColorMagenta);
		compileAtomColors(mol, graphIndex, sb, "orange:", Molecule.cAtomColorOrange);
		compileAtomColors(mol, graphIndex, sb, "darkGreen:", Molecule.cAtomColorDarkGreen);
		compileAtomColors(mol, graphIndex, sb, "darkRed:", Molecule.cAtomColorDarkRed);
		return sb.length() == 0 ? null : sb.toString().getBytes();
		}

	private void compileAtomColors(StereoMolecule mol, int[] graphIndex, StringBuilder sb, String colorName, int atomColor) {
		boolean isFirst = true;
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.getAtomColor(atom) == atomColor) {
				if (isFirst) {
					if (sb.length() != 0)
						sb.append(cEntrySeparator);
					sb.append(colorName+graphIndex[atom]);
					isFirst = false;
					}
				else {
					sb.append(","+graphIndex[atom]);
					}
				}
			}
		}

	/**
	 * Updates an idcode column and 2D-coordinates (if existing) from a StereoMolecule.
	 * All other child columns of the idcode column are cleared.
	 * Call either finalizeChangeCell() after setting one chemistry object or call
	 * finalizeChangeColumn() for the idcode column after setting multiple ones.
	 * @param record
	 * @param mol
	 * @param column idcode column
	 * @return true if something was updated
	 */
	public boolean setChemicalReaction(CompoundRecord record, Reaction rxn, int column) {
		if (rxn == null || rxn.getMolecules() == 0)
			record.setData(null, column);
		else
			record.setData(ReactionEncoder.encode(rxn, false, ReactionEncoder.INCLUDE_MAPPING
															| ReactionEncoder.INCLUDE_COORDS
															| ReactionEncoder.INCLUDE_DRAWING_OBJECTS).getBytes(), column);

		return true;
		}

	public int getTotalRowCount() {
		return mRecords;
		}

	private String decorateWithSummaryMode(String name, int column) {
		switch (mColumnInfo[column].summaryMode) {
		case cSummaryModeMean:
			return "mean of "+name;
		case cSummaryModeMedian:
			return "median of "+name;
		case cSummaryModeMinimum:
			return "min of "+name;
		case cSummaryModeMaximum:
			return "max of "+name;
		case cSummaryModeSum:
			return "sum of "+name;
		default:
			return name;
			}
		}

	/**
	 * Retrieves the visible column name or an alias if one exists.
	 * If the column shows summarized values such as mean, sum or max values,
	 * and if the cell based summary count text is suppressed, then the
	 * column name is modified to indicate, that be have summarized values, e.g.
	 * '<i>mean of </i>IC50'.
	 * @param visColumn visible column index
	 * @return column name or alias
	 */
	public String getColumnName(int visColumn) {
		return getColumnTitleExtended(mDisplayableColumnToColumn[visColumn]);
		}

	/**
	 * Use this method or getColumnTitleExtended() whenever you need to display a column title.
	 * For displayable columns this method returns the alias (if existing) or the
	 * original column name. For non-displayable columns a title is constructed from type and
	 * parent title like <i>Structure [2D-Coordinates]</i> or <i>Reaction [ReactionFp]</i>.
	 * findColumn(String columnName) is guaranteed to correctly return the column index from this title.
	 * @param column absolute (total) column index
	 * @return column display name or null, if column < 0
	 */
	public String getColumnTitle(int column) {
		if (column < 0)
			return null;

		int parent = getParentColumn(column);
		if (parent != -1 && !isColumnDisplayable(column)) {
		   	if (cColumnType3DCoordinates.equals(getColumnSpecialType(column)))
		  		return cColumnType3DCoordinates.equals(mColumnInfo[column].name) ?
		  				"3D-"+mColumnInfo[parent].name : mColumnInfo[column].name;

			return mColumnInfo[parent].name+" ["+getColumnSpecialTypeForDisplay(column)+"]";
			}

		String name = (mColumnInfo[column].alias != null) ? mColumnInfo[column].alias : mColumnInfo[column].name;
		return name;
		}

	/**
	 * Use this method or getColumnTitle() whenever you need to display a column title.
	 * This method works as getColumnTitle(), except for the following:<br>
	 * - If a numerical column's summary mode is set, then this is reflected in the name as <i>mean of myName</i><br>
	 * - It creates names for pseudo columns referring to hitlists as <i>List 'MyList'</i><br>
	 * - Descriptor column names include the word 'Similarity' as <i>Structure Similarity [FragFp]</i><br>
	 * - column == -1 is returned as <i>&ltUnassigned&gt</i><br>
	 * findColumn(String columnName) is guaranteed to correctly return the column index from this title.
	 * @param column absolute (total) column index, pseudo column referring to hitlist, cColumnUnassigned, i.e. -1
	 * @return column name or alias
	 */
	public String getColumnTitleExtended(int column) {
		if (column == -1)
			return cColumnUnassignedItemText;

		if (CompoundTableHitlistHandler.isHitlistColumn(column))
			return cColumnNameRowList+mHitlistHandler.getHitlistName(CompoundTableHitlistHandler.getHitlistFromColumn(column))+"'";

		String parent = getColumnProperty(column, cColumnPropertyParentColumn);
		if (parent != null && !isColumnDisplayable(column))
			return (isDescriptorColumn(column) ? parent+" Similarity" : parent)+" ["+getColumnSpecialTypeForDisplay(column)+"]";

		String name = (mColumnInfo[column].alias != null) ? mColumnInfo[column].alias : mColumnInfo[column].name;
		return decorateWithSummaryMode(name, column);
		}

	/**
	 * Converts any column title created by getColumnName() or getColumnTitleXXX()
	 * including pseudo columns into a normalized column name that may serve to identify
	 * the column within templates or macros. If columnName is <i>cColumnUnassignedItemText</i>, then
	 * <i>cColumnUnassignedCode</i> is returned. If columnName cannot be assigned to a column, then columnName
	 * is returned unchanged.
	 * findColumn(String columnName) is guaranteed to correctly return the column index from this title.
	 * @param columnName
	 * @return no-alias column name, pseudo column name, or cColumnUnassignedCode
	 */
	public String getColumnTitleNoAlias(String columnName) {
		int column = findColumn(columnName);
		if (column == -1)
			return cColumnUnassignedItemText.equals(columnName) ? cColumnUnassignedCode : columnName;
		if (CompoundTableHitlistHandler.isHitlistColumn(column))
			return columnName;
		return mColumnInfo[column].name;
		}

	/**
	 * Returns a column title for normal and pseudo columns that doesn't use alias' and that may serve to identify
	 * the column within templates or macros. If column is -1, then <i>cColumnUnassignedCode</i> is returned.
	 * findColumn(String columnName) is guaranteed to correctly return the column index from this title.
	 * @param column
	 * @return no-alias column name, pseudo column name, or cColumnUnassignedCode
	 */
	public String getColumnTitleNoAlias(int column) {
		if (column == -1)
			return cColumnUnassignedCode;
		if (CompoundTableHitlistHandler.isHitlistColumn(column))
			return cColumnNameRowList+mHitlistHandler.getHitlistName(CompoundTableHitlistHandler.getHitlistFromColumn(column))+"'";
		return mColumnInfo[column].name;
		}

	public String getColumnAlias(int column) {
		return mColumnInfo[column].alias;
		}

	/**
	 * The column description is a single or multiline plain text.
	 * Lines are separated by \n chars.
	 * @param column
	 * @return null or text string
	 */
	public String getColumnDescription(int column) {
		return mColumnInfo[column].description;
		}

	private String getColumnSpecialTypeForDisplay(int column) {
		String specialType = getColumnSpecialType(column);
		if (specialType == null)
			specialType = "<null>";
		if (specialType.equals(cColumnType2DCoordinates))
			specialType = "2D-Coordinates";
		else if (specialType.equals(cColumnType3DCoordinates))
			specialType = "3D-Coordinates";
		return specialType;
		}

	public int findColumn(String columnName) {
		if (columnName == null || mColumnInfo == null)
			return -1;

		columnName = columnName.trim();
		for (int i=0; i<mColumnInfo.length; i++)
			if (mColumnInfo[i].name.equalsIgnoreCase(columnName))
				return i;

		for (int i=0; i<mColumnInfo.length; i++)
			if (mColumnInfo[i].alias != null && mColumnInfo[i].alias.equalsIgnoreCase(columnName))
				return i;

		if (columnName.equals(cColumnUnassignedItemText) || columnName.equals(cColumnUnassignedCode))
			return -1;

		for (int i=0; i<mColumnInfo.length; i++) {
			if (mColumnInfo[i].summaryMode != cSummaryModeNormal) {
				if (mColumnInfo[i].alias != null) {
					if (columnName.equals(decorateWithSummaryMode(mColumnInfo[i].alias, i)))
						return i;
					}
				else {
					if (columnName.equals(decorateWithSummaryMode(mColumnInfo[i].name, i)))
						return i;
					}
				}
			}

/*	  for (int i=0; i<mColumnInfo.length; i++) {
			if (mColumnInfo[i].logarithmicViewMode) {
				if (mColumnInfo[i].alias != null) {
					if (columnName.equals("log("+mColumnInfo[i].alias+")"))
						return i;
					}
				else {
					if (columnName.equals("log("+mColumnInfo[i].name+")"))
						return i;
					}
				}
			}*/

		// to match old OSIRIS test columns "testName.paramName" to new ones "testName.paramName [unit]"
		if (columnName.matches("[\\d\\w\\s_]+\\..+"))	// matches "#.* [*]" with # is one or more digits,letters,whitespace
			for (int i=0; i<mColumnInfo.length; i++)
				if (mColumnInfo[i].name.startsWith(columnName)
		   		 && mColumnInfo[i].name.substring(columnName.length()).matches(" \\[.+]"))
					return i;

		// to match child columns from display name as "Structure [FragFp]"
		if (columnName.matches(".+ \\[.+\\]")) {	// matches "* [*]" with * is one or more visible characters
			for (int i=0; i<mColumnInfo.length; i++) {
				String parent = getColumnProperty(i, cColumnPropertyParentColumn);
				if (parent != null && !isColumnDisplayable(i)) {
					String type = " ["+getColumnSpecialTypeForDisplay(i)+"]";
					if (columnName.equals(parent+type)
					 || (columnName.equals(parent+" Similarity"+type) && isDescriptorColumn(i)))
				   		return i;
					}
				}
			}

		if (columnName.startsWith("3D-")) {
			for (int i=0; i<mColumnInfo.length; i++) {
				if (cColumnType3DCoordinates.equals(getColumnSpecialType(i))) {
					int parent = getParentColumn(i);
					if (parent != -1
					 && columnName.substring(3).equals(mColumnInfo[parent].name))
						return i;
					}
				}
			}

		// to match pseudo columns that refer to lists as "List 'MyList'"
		if (columnName.matches(cColumnNameRowList+".+'")) {	// matches "List '*'" with * is one or more visible characters
			for (int i=0; i<mHitlistHandler.getHitlistCount(); i++)
				if (columnName.equals(cColumnNameRowList+mHitlistHandler.getHitlistName(i)+"'"))
					return CompoundTableHitlistHandler.getColumnFromHitlist(i);
			}

		return -1;
		}

	/**
	 * Assuming that column is a category column, this method returns the number of
	 * distinct category items within the column. If any of the column's cells
	 * contains more than one distinct category items, then the category count
	 * includes a pseudo category 'multiple categories'.
	 * @param column
	 * @return categoryCount; categoryCount+1 in case of multiple categories per cell
	 */
	public int getCategoryCount(int column) {
		if (CompoundTableHitlistHandler.isHitlistColumn(column))
			return 2;

		int listSize = mColumnInfo[column].categoryList.getSize();
		if (mColumnInfo[column].belongsToMultipleCategories)
			listSize++;
		return listSize;
		}

	public float getMinimumValue(int column) {
		return mColumnInfo[column].minValue;
		}

	public float getMaximumValue(int column) {
		return mColumnInfo[column].maxValue;
		}

	public CompoundRecord getRecord(int row) {
		return mNonExcludedRecord[row];
		}

	public CompoundRecord getTotalRecord(int row) {
		return mRecord[row];
		}

	public float getDescriptorSimilarity(CompoundRecord r1, CompoundRecord r2, int column) {
		Object o1 = r1.getData(column);
		Object o2 = r2.getData(column);
		return (o1 == null || o2 == null) ? Float.NaN
					: mColumnInfo[column].descriptorHandler.getSimilarity(o1, o2);
		}

	public File getFile() {
		return mFile;
		}

	/**
	 * @return null or an array of the CompoundTableExtension names, to which associated data exists
	 */
	public String[] getAvailableExtensionNames() {
		if (mTableExtensionMap == null || mTableExtensionMap.size() == 0)
			return null;

		return mTableExtensionMap.keySet().toArray(new String[0]);
		}

	/**
	 * @return null or the CompoundTableExtension data, which was attached to this table model under the given name
	 */
	public Object getExtensionData(String name) {
		return (mTableExtensionMap == null) ? null : mTableExtensionMap.get(name);
		}

	public String encodeData(CompoundRecord record, int column) {
		return (record == null) ? ""
				: (record.getData(column) == null) ? ""
				: (mColumnInfo[column].descriptorHandler == null) ?
				  new String((byte[])record.getData(column))
				: mColumnInfo[column].descriptorHandler.encode(record.getData(column));
		}

	public Object decodeData(String data, int column) {
		return (data == null || data.length() == 0) ? null
				: (mColumnInfo[column].descriptorHandler == null) ?
				  data.getBytes()
				: mColumnInfo[column].descriptorHandler.decode(data);
		}

	public boolean isCellEditable(int row, int column) {
		return false;
		}

	public void setColumnName(String name, int column) {
		if (!name.equals(mColumnInfo[column].name))
			mColumnInfo[column].name = validateColumnName(name, null, -1, this);
		}

	public void setColumnAlias(String alias, int column) {
		if (alias != null
		 && (alias.length() == 0
		  || alias.equals(mColumnInfo[column].name)))
			alias = null;

		if ((alias == null ^ mColumnInfo[column].alias == null)
		 || (mColumnInfo[column].alias != null
		  && !mColumnInfo[column].alias.equals(alias))) {
			mColumnInfo[column].alias = (alias == null) ? null : validateColumnName(alias, null, -1, this);

			TableModelEvent tme = new TableModelEvent(this,
													  TableModelEvent.HEADER_ROW,
													  TableModelEvent.HEADER_ROW,
													  column,
													  TableModelEvent.UPDATE);

			fireEvents(new CompoundTableEvent(this,
								CompoundTableEvent.cChangeColumnName, column), tme);
			}
		}

	public void setColumnDescription(String description, int column) {
		if (description != null
		 && description.length() == 0)
			description = null;
		mColumnInfo[column].description = description;
		}

	public void setValueAt(String value, int row, int column) {
		mNonExcludedRecord[row].setData(decodeData(value, column), column);
		}

	public void setTotalValueAt(String value, int row, int column) {
		// index refers to all records rather than to the visible ones
		mRecord[row].setData(decodeData(value, column), column);
		}

	public void setTotalDataAt(Object value, int row, int column) {
		// index refers to all records rather than to the visible ones
		mRecord[row].setData(value, column);
		}

	public void appendTotalDataAt(byte[] value, int row, int column) {
		// index refers to all records rather than to the visible ones
		mRecord[row].appendData(value, column);
		}

	public void setLogarithmicViewMode(int column, boolean mode) {
		if (mode
		 && column < mColumns	// if we change the mode of already finalized columns
		 && (mColumnInfo[column].minValue <= 0
		  || mColumnInfo[column].dataMin <= 0))
			return;

		if (mColumnInfo[column].logarithmicViewMode != mode) {
			mColumnInfo[column].logarithmicViewMode = mode;

			if (column < mColumns) {	// if we change the mode of already finalized columns
				setupDoubleValues(column, 0);
	
						// mean calculation is different in logarithmic view mode
				TableModelEvent tme = (mColumnInfo[column].summaryMode != cSummaryModeNormal) ?
						new TableModelEvent(this, 0, mNonExcludedRecords-1, mColumnToDisplayableColumn[column], TableModelEvent.UPDATE) : null;
	
				fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column), tme);
				}
			}
		}

	/**
	 * Define the full data range for this numerical column, which must
	 * include all values. The range is specified as non-logarithmic values,
	 * even if the column is set to logarithmic mode. In logarithmic mode
	 * the specified minValue must be larger than 0.0.
	 * @param column
	 * @param minValue null or as float interpretable string value
	 * @param maxValue null or as float interpretable string value
	 */
	public void setValueRange(int column, String minValue, String maxValue) {
		if ((mColumnInfo[column].type & cColumnTypeDouble) == 0)
			return; // cannot set range for non-float columns (beware of date columns)
		if (getColumnProperty(column, cColumnPropertyCyclicDataMax) != null)
			return; // cannot set range for cyclic data

		float min = Float.NaN;
		float max = Float.NaN;
	    try {
	    	if (minValue != null)
	    		min = Float.parseFloat(minValue);
	    	if (maxValue != null)
	    		max = Float.parseFloat(maxValue);
	    	}
	    catch (NumberFormatException nfe) {
	    	return;
	    	}

		if (!Float.isNaN(min) && mColumnInfo[column].logarithmicViewMode && min <= 0)
			return;

		boolean changed = false;
		String oldMinValue = getColumnProperty(column, cColumnPropertyDataMin);
		if (minValue == null) {
			if (oldMinValue != null) {
				setColumnProperty(column, cColumnPropertyDataMin, null);
				changed = true;
				}
			}
		else {
			if (oldMinValue == null || Float.parseFloat(oldMinValue) != min) {
				setColumnProperty(column, cColumnPropertyDataMin, minValue);
				changed = true;
				}
			}
		String oldMaxValue = getColumnProperty(column, cColumnPropertyDataMax);
		if (maxValue == null) {
			if (oldMaxValue != null) {
				setColumnProperty(column, cColumnPropertyDataMax, null);
				changed = true;
				}
			}
		else {
			if (oldMaxValue == null || Float.parseFloat(oldMaxValue) != max) {
				setColumnProperty(column, cColumnPropertyDataMax, maxValue);
				changed = true;
				}
			}

		if (changed) {
			updateMinAndMaxFromDouble(column);
			fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column), null);
			}
		}
	
	/**
	 * Sets the column summary display mode for multiple numerical values.
	 * This may be called for new columns before calling finalizeNewColumns()
	 * or for existing columns. In the latter case the column is re-analyzed and
	 * the proper column change events are fired.
	 * @param column total column index
	 * @param mode one of cDisplayModeNormal,cDisplayModeMean,cDisplayModeMedian
	 */
	public void setColumnSummaryMode(int column, int mode) {
		if (mColumnInfo[column].summaryMode != mode) {
			mColumnInfo[column].summaryMode = mode;

			if (column < mColumns) {	// if we change the mode of already finalized columns
				setupDoubleValues(column, 0);
	
				fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnName, column),
						   new TableModelEvent(this, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW,
								   			   mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
				fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column),
						   new TableModelEvent(this, 0, mNonExcludedRecords-1,
								   			   mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
				}
			}
		}

	/**
	 * This hides the text following any summarizing number, e.g. '(mean of 4)'.
	 * This way data can be copied or exported for further numerical analysis.
	 * @param isHidden
	 */
	public void setColumnSummaryCountHidden(int column, boolean isHidden) {
		if (mColumnInfo[column].summaryCountHidden != isHidden) {
			mColumnInfo[column].summaryCountHidden = isHidden;

			if (column < mColumns) {	// if we change the mode of already finalized columns
				fireEvents(null,
						   new TableModelEvent(this, 0, mNonExcludedRecords-1,
								   			   mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
				}
			}
		}

	/**
	 * Sets the hilite mode for structure columns, which determines whether and how
	 * parts of a molecule are highlighted depending on the molecule of the current row.
	 * @param column total column index
	 * @param mode one of cStructureHiliteModeNone,cStructureHiliteModeMCS
	 */
	public void setStructureHiliteMode(int column, int mode) {
		if (mColumnInfo[column].structureHiliteMode != mode) {
			mColumnInfo[column].structureHiliteMode = mode;

			if (column < mColumns)	// if we change the mode of already finalized columns
				fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column),
						   new TableModelEvent(this, 0, mNonExcludedRecords-1, mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
			}
		}

	/**
	 * Sets the number of displayed significant digits.
	 * @param column
	 * @param digits 0 for original value, 1,2,3,... for rounded value
	 */
	public void setColumnSignificantDigits(int column, int digits) {
		if (mColumnInfo[column].significantDigits != digits) {
			mColumnInfo[column].significantDigits = digits;

			if (column < mColumns)	// if we change the mode of already finalized columns
				fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column),
						   new TableModelEvent(this, 0, mNonExcludedRecords-1, mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
			}
		}

	public void setFile(File file) {
		mFile = file;
		}

	/**
	 * Attaches black box data to this table model. The data is identified by name,
	 * a CompoundTableExtensionHandler must be registered and must know about the extension type
	 * identified by name.
	 * @param name a defined name that the active CompoundTableExtensionHandler knows about
	 */
	public void setExtensionData(String name, Object data) {
		if (mExtensionHandler != null && mExtensionHandler.getID(name) != -1) {
			if (data != null && mTableExtensionMap == null)
				mTableExtensionMap = new TreeMap<String,Object>();
			if (mTableExtensionMap != null)
				mTableExtensionMap.put(name, data);
			fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeExtensionData, mExtensionHandler.getID(name)), null);
			}
		}

	/**
	 * Deselect all records which have a given flag set.
	 * @param flagNo >= 0
	 */
	public void deselectByList(int flagNo) {
		long mask = convertCompoundFlagToMask(flagNo);
		for (int row=0; row<mRecords; row++)
			if ((mRecord[row].mFlags & mask) != 0)
				mRecord[row].mFlags &= ~CompoundRecord.cFlagMaskSelected;

		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeSelection, -1), null);
		}
	
	/**
	 * Select all records which have a given flag set.
	 * @param flagNo >= 0
	 */
	public void selectByList(int flagNo) {
		long mask = convertCompoundFlagToMask(flagNo);
		for (int row=0; row<mRecords; row++)
			if ((mRecord[row].mFlags & mask) != 0)
				mRecord[row].mFlags |= CompoundRecord.cFlagMaskSelected;

		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeSelection, -1), null);
		}
	
	public void invertSelection() {
		long unselectionMask = mAllocatedExclusionFlags
							| CompoundRecord.cFlagMaskSelected;

		for (int row=0; row<mRecords; row++)
			if ((mRecord[row].mFlags & unselectionMask) != 0)
				mRecord[row].mFlags &= ~CompoundRecord.cFlagMaskSelected;
			else
				mRecord[row].mFlags |= CompoundRecord.cFlagMaskSelected;

		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeSelection, -1), null);
		}

	public void addNewRows(int newRowCount) {
		stopDescriptorCalculation();

		CompoundRecord[] record = new CompoundRecord[mRecords + newRowCount];
		for (int row=0; row<mRecords; row++)
			record[row] = mRecord[row];
		for (int row=mRecords; row<record.length; row++)
			record[row] = new CompoundRecord(row, mColumnInfo.length);
		mRecord = record;

		CompoundRecord[] nonExcludedRecord = new CompoundRecord[mRecords + newRowCount];
		for (int row=0; row<mNonExcludedRecords; row++)
			nonExcludedRecord[row] = mNonExcludedRecord[row];
		mNonExcludedRecord = nonExcludedRecord;
		}

	/**
	 * Adds new columns with default column names to the table model.
	 * After populating the column content call finalizeNewColumn().
	 * @param columnCount
	 * @return
	 */
	public int addNewColumns(int columnCount) {
		int firstNewColumn = mColumnInfo.length;
		CompoundTableColumnInfo[] newColumnInfo = new CompoundTableColumnInfo[firstNewColumn+columnCount];

		for (int i=0; i<firstNewColumn; i++)
			newColumnInfo[i] = mColumnInfo[i];
		for (int i=0; i<columnCount; i++)
			newColumnInfo[firstNewColumn+i] = new CompoundTableColumnInfo(validateColumnName("Column "+(firstNewColumn+i), null, -1, this));

		for (int row=0; row<mRecord.length; row++)
			mRecord[row].addColumns(columnCount);

		mColumnInfo = newColumnInfo;

		return firstNewColumn;
		}

	public int addNewColumns(String[] columnName) {
		int firstNewColumn = (mColumnInfo == null) ? 0 : mColumnInfo.length;
		CompoundTableColumnInfo[] newColumnInfo = new CompoundTableColumnInfo[firstNewColumn+columnName.length];

		validateColumnNames(columnName);
		for (int i=0; i<firstNewColumn; i++)
			newColumnInfo[i] = mColumnInfo[i];
		for (int i=0; i<columnName.length; i++)
			newColumnInfo[firstNewColumn+i] = new CompoundTableColumnInfo(columnName[i]);

		if (mRecord == null)
			mRecord = new CompoundRecord[0];

		for (int row=0; row<mRecord.length; row++)
			mRecord[row].addColumns(columnName.length);

		mColumnInfo = newColumnInfo;

		return firstNewColumn;
		}

	/**
	 * Returns an array with one or more columns of the desired type.
	 * @param type
	 * @return null or array of column indices
	 */
	public int[] getSpecialColumnList(String type) {
		if (mColumnInfo == null)
			return null;

		int count = 0;
		for (int column=0; column<mColumnInfo.length; column++)
			if (type.equals(getColumnProperty(column, cColumnPropertySpecialType)))
				count++;

		if (count == 0)
			return null;

		int[] specialColumn = new int[count];
		count = 0;
		for (int column=0; column<mColumnInfo.length; column++)
			if (type.equals(getColumnProperty(column, cColumnPropertySpecialType)))
				specialColumn[count++] = column;

		return specialColumn;
		}

	public void prepareStructureColumns(int idcodeColumn, String idcodeColumnName,
										boolean prepareCoordinates, boolean prepareFFP512) {
		prepareStructureColumns(idcodeColumn, idcodeColumnName, prepareCoordinates,
								prepareFFP512 ? DESCRIPTOR_FFP512.shortName : null);
		}

	public void prepareStructureColumns(int idcodeColumn, String idcodeColumnName,
			boolean prepareCoordinates, String descriptorShortName) {
		int column = idcodeColumn;

		setColumnName(idcodeColumnName, column);
		setColumnProperty(column, cColumnPropertySpecialType, cColumnTypeIDCode);

		if (prepareCoordinates) {
			column++;
			setColumnName(cColumnType2DCoordinates, column);
			setColumnProperty(column, cColumnPropertySpecialType, cColumnType2DCoordinates);
			setColumnProperty(column, cColumnPropertyParentColumn, getColumnTitleNoAlias(idcodeColumn));
			}

		if (descriptorShortName != null) {
			column++;
			setColumnName(descriptorShortName, column);
			setColumnProperty(column, cColumnPropertySpecialType, descriptorShortName);
			setColumnProperty(column, cColumnPropertyParentColumn, getColumnTitleNoAlias(idcodeColumn));
			mColumnInfo[column].addCurrentDescriptorVersion(descriptorShortName);
			}
		}

	private void allocateDefaultDescriptorColumns() {
		int firstIDCodeColumn = -1;
		int firstRXNCodeColumn = -1;
		for (int column=0; column<mColumnInfo.length; column++) {
			String specialType = getColumnSpecialType(column);
			if (specialType != null) {
				if (specialType.equals(cColumnTypeIDCode)) {
					if (firstIDCodeColumn == -1)
						firstIDCodeColumn = column;
					}
				else if (specialType.equals(cColumnTypeRXNCode)) {
					if (firstRXNCodeColumn == -1)
						firstRXNCodeColumn = column;
					}
				}
			}

		boolean fingerprintFound = false;
		boolean reactionIndexFound = false;
		if (firstIDCodeColumn != -1 || firstRXNCodeColumn != -1) {
			for (int column=0; column<mColumnInfo.length; column++) {
				String specialType = getColumnSpecialType(column);
				if (specialType != null) {
					if (firstIDCodeColumn != -1
					 && getParentColumn(column) == firstIDCodeColumn
					 && specialType.equals(DESCRIPTOR_FFP512.shortName)) {
						fingerprintFound = true;
						}
					else if (firstRXNCodeColumn != -1
	   					  && getParentColumn(column) == firstRXNCodeColumn
						  && specialType.equals(DESCRIPTOR_ReactionIndex.shortName)) {
						reactionIndexFound = true;
						}
					}
				}
			}

		if (firstIDCodeColumn != -1 && !fingerprintFound) {
			allocateDescriptorColumn(firstIDCodeColumn, DESCRIPTOR_FFP512.shortName);
			}
		if (firstRXNCodeColumn != -1 && !reactionIndexFound) {
			allocateDescriptorColumn(firstRXNCodeColumn, DESCRIPTOR_ReactionIndex.shortName);
			}
		}

	public int createDescriptorColumn(int parent, String shortName) {
		int column = allocateDescriptorColumn(parent, shortName);
		updateDescriptors();
		createDisplayableColumnMap();
		mColumns = mColumnInfo.length;
		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cAddColumns, column), null);
		return column;
		}

	private int allocateDescriptorColumn(int parent, String shortName) {
		String[] columnName = new String[1];
		columnName[0] = shortName;
		int column = addNewColumns(columnName);
		setColumnProperty(column, cColumnPropertySpecialType, shortName);
		setColumnProperty(column, cColumnPropertyParentColumn, mColumnInfo[parent].name);
		mColumnInfo[column].isDescriptorIncomplete = true;
		mColumnInfo[column].addCurrentDescriptorVersion(shortName);
		return column;
		}

/*	public boolean isColumnVisible(int column) {
		String visible = getColumnProperty(column, cColumnPropertyIsVisible);
		if (visible != null)
			return (visible.equals("true"));

		return isColumnDisplayable(column);
		}

	public void setColumnVisibility(int column, boolean isVisible) {
		if (isVisible == isColumnDisplayable(column))
			setColumnProperty(column, cColumnPropertyIsVisible, null);
		else
			setColumnProperty(column, cColumnPropertyIsVisible, isVisible ? "true" : "false");

		createVisibleColumnMap();
		TableModelEvent tme = new TableModelEvent(this,
												  TableModelEvent.HEADER_ROW,
												  TableModelEvent.HEADER_ROW,
												  column,
												  isVisible?TableModelEvent.INSERT:TableModelEvent.DELETE);
		fireEvents(null, tme);
		}*/

	public boolean isColumnDisplayable(int column) {
		String value = mColumnInfo[column].getProperty(cColumnPropertyIsDisplayable);
		if (value != null)
			return !value.equals("false");

		String specialType = getColumnSpecialType(column);
		if (specialType == null)
			return true;
		
		for (int i=0; i<cParentSpecialColumnTypes.length; i++)
			if (specialType.equals(cParentSpecialColumnTypes[i]))
				return true;

		return false;
		}

	public boolean getColumnModifierExclusion(int column) {
		return mColumnInfo[column].hasModifiers
			&& mColumnInfo[column].excludeModifierValues;
		}

	public void setColumnModifierExclusion(int column, boolean value) {
		if (mColumnInfo[column].hasModifiers
		 && mColumnInfo[column].excludeModifierValues != value) {
			mColumnInfo[column].excludeModifierValues = value;

			setupDoubleValues(column, 0);

			fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column),
					   new TableModelEvent(this, 0, mNonExcludedRecords-1, mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
			}
		}

	public int getParentColumn(int column) {
		for (int i=0; i<mColumnInfo.length; i++)
			if (mColumnInfo[i].name.equals(getColumnProperty(column, cColumnPropertyParentColumn)))
				return i;
		return -1;
		}

	public int getChildColumn(int column, String type) {
		for (int i=0; i<mColumnInfo.length; i++)
			if (type.equals(getColumnProperty(i, cColumnPropertySpecialType))
			 && mColumnInfo[column].name.equals(getColumnProperty(i, cColumnPropertyParentColumn)))
				return i;
		return -1;
		}

	public DescriptorHandler getDescriptorHandler(int column) {
		return mColumnInfo[column].descriptorHandler;
		}

	public synchronized void removeColumns(boolean[] removeColumn, int removalCount) {
			// also mark all child columns of removal columns to be removed
		for (int i=0; i<mColumns; i++) {
			if (removeColumn[i]) {
				for (int j=0; j<mColumns; j++) {
					if (!removeColumn[j]) {
						String idColumn = getColumnProperty(j, cColumnPropertyIdentifierColumn);
						if (idColumn != null && idColumn.equals(mColumnInfo[i].name))
							setColumnProperty(j, cColumnPropertyIdentifierColumn, null);

						String refColumn = getColumnProperty(j, cColumnPropertyReferencedColumn);
						if (refColumn != null && refColumn.equals(mColumnInfo[i].name)) {
							setColumnProperty(j, cColumnPropertyReferencedColumn, null);
							setColumnProperty(j, cColumnPropertyReferenceType, null);
							}

						String parent = getColumnProperty(j, cColumnPropertyParentColumn);
						if (parent != null && parent.equals(mColumnInfo[i].name)) {
							removeColumn[j] = true;
							removalCount++;
							}
						}
					}
				}
			}

		// if an indexing thread is running don't allow column removal if it
		// would change the column index of a currently updated descriptor.
		boolean stopDescriptorCalculation = false;
		if (mSMPWorkingThreads != null) {
			int firstRemovalColumn = -1;
			for (int i=0; i<mColumns; i++) {
				if (removeColumn[i]) {
					firstRemovalColumn = i;
					break;
					}
				}
			if (firstRemovalColumn != -1) {
				for (int i=firstRemovalColumn; i<mColumns; i++) {
					if (isDescriptorColumn(i) && !isDescriptorAvailable(i)) {
						stopDescriptorCalculation = true;
						break;
						}
					}
				}
			}

		if (stopDescriptorCalculation) {
			stopDescriptorCalculation();
			}

		CompoundTableColumnInfo[] newColumnInfo = new CompoundTableColumnInfo[mColumns-removalCount];
		int newIndex = 0;
		int[] columnMapping = new int[mColumns];
		for (int i=0; i<mColumns; i++) {
			if (!removeColumn[i]) {
				newColumnInfo[newIndex] = mColumnInfo[i];
				columnMapping[i] = newIndex;
				newIndex++;
				}
			else {
				columnMapping[i] = -1;
				}
			}
		mColumnInfo = newColumnInfo;

		for (int row=0; row<mRecords; row++)
			mRecord[row].removeColumns(removeColumn, removalCount);

		mColumns -= removalCount;

		createDisplayableColumnMap();
		removeUnreferencedDetails();

		TableModelEvent tme = new TableModelEvent(this,
												  TableModelEvent.HEADER_ROW,
												  TableModelEvent.HEADER_ROW,
												  removalCount,
												  TableModelEvent.DELETE);

		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cRemoveColumns, columnMapping), tme);

		if (stopDescriptorCalculation) {
			updateDescriptors();
			}
		}

	/**
	 * Does housekeeping and fresh column analysis after adding new rows.
	 * @param firstRow total row count before calling addNewRows()
	 * @param listener may be null
	 */
	public void finalizeNewRows(int firstRow, ProgressListener listener) {
		analyzeData(firstRow, listener);
		createDisplayableColumnMap();
		compileVisibleRecords();
		Runtime.getRuntime().gc();
		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cAddRows, firstRow),
				   new TableModelEvent(this, firstRow, mNonExcludedRecords-1, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		}

	public void finalizeNewColumns(int firstNewColumn, ProgressListener listener) {
		allocateDefaultDescriptorColumns();
		for (int column=firstNewColumn; column<mColumnInfo.length; column++) {
			analyzeColumn(column, 0, false);
			}
		for (int column=firstNewColumn; column<mColumnInfo.length; column++) {
			if (cColumnTypeIDCode.equals(getColumnSpecialType(column)))
				checkIDCodeVersion(column, 0, listener);
			}
		updateDescriptors();
		createDisplayableColumnMap();
		mColumns = mColumnInfo.length;
		Runtime.getRuntime().gc();
		TableModelEvent tme = new TableModelEvent(this,
												  TableModelEvent.HEADER_ROW,
												  TableModelEvent.HEADER_ROW,
												  0,
												  TableModelEvent.INSERT);
		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cAddColumns, firstNewColumn), tme);
		}

	/**
	 * Call this after interactively editing the content of a displayable cell.
	 * This causes to re-analyze the column, to calculate new descriptors
	 * (if needed) and to send proper events to inform all views.
	 * If child records other than descriptors exist, then these should
	 * also be changed before calling finalizeChangeCell().
	 * @param record The record of which the cell was modified
	 * @param column
	 */
	public void finalizeChangeCell(CompoundRecord record, int column) {
		analyzeColumn(column, 0, false);

		for (int i=0; i<mColumnInfo.length; i++) {
			if (i != column && isDescriptorColumn(i) && getParentColumn(i) == column) {
				record.setData(null, i);
				DescriptorColumnSpec spec = new DescriptorColumnSpec(i);
				updateDescriptor(record, spec, null);
				if (!mColumnInfo[i].isComplete && spec.isComplete)
					analyzeCompleteness(i, 0);
				else
					mColumnInfo[i].isComplete &= spec.isComplete;
				}
			}

		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column),
				   new TableModelEvent(this, 0, mNonExcludedRecords-1, mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
		}

	/**
	 * Needs to be called after changing individual values of one displayable column.
	 * It re-analyzes the data type and completeness. If the column has descriptors
	 * as child columns, then the descriptor update thread is started.
	 * @param column
	 * @param fromIndex first row index
	 * @param toIndex last row index + 1
	 */
	public void finalizeChangeColumn(int column, int fromIndex, int toIndex) {
		analyzeColumn(column, 0, false);

		for (int i=0; i<mColumnInfo.length; i++) {
			if (i != column && isDescriptorColumn(i) && getParentColumn(i) == column) {
				boolean needsUpdate = false;
				for (int row=fromIndex; row<toIndex; row++) {
					mRecord[row].setData(null, i);
					if (mRecord[row].getData(column) != null)
						needsUpdate = true;
					}
				if (needsUpdate) {
					mColumnInfo[i].isDescriptorIncomplete = true;
					updateDescriptors();
					}
				}
			}
		
		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column),
				   new TableModelEvent(this, 0, mNonExcludedRecords-1, mColumnToDisplayableColumn[column], TableModelEvent.UPDATE));
		}

	public void allocateColumnDetail(int column, String name, String type, String source) {
		int count = getColumnDetailCount(column);
		setColumnProperty(column, cColumnPropertyDetailName+count, name);
		setColumnProperty(column, cColumnPropertyDetailType+count, type);
		setColumnProperty(column, cColumnPropertyDetailSource+count, source);
		setColumnProperty(column, cColumnPropertyDetailCount, ""+(count+1));
		}

	public int getColumnDetailCount(int column) {
		String countString = getColumnProperty(column, cColumnPropertyDetailCount);
		int count = 0;
		if (countString != null)
			try { count = Integer.parseInt(countString); } catch (NumberFormatException nfe) {};

		return count;
		}

	public String getColumnDetailName(int column, int detail) {
		return getColumnProperty(column, cColumnPropertyDetailName+detail);
		}

	public String getColumnDetailType(int column, int detail) {
		return getColumnProperty(column, cColumnPropertyDetailType+detail);
		}

	public String getColumnDetailSource(int column, int detail) {
		return getColumnProperty(column, cColumnPropertyDetailSource+detail);
		}

	public void setColumnDetailSource(int column, int detail, String source) {
		setColumnProperty(column, cColumnPropertyDetailSource+detail, source);
			int[] detailList = new int[1];
			detailList[0] = detail;
		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnDetailSource, column, detailList), null);
		}

	public int getColumnSummaryMode(int column) {
		return mColumnInfo[column].summaryMode;
		}

	public boolean isColumnSummaryCountHidden(int column) {
		return mColumnInfo[column].summaryCountHidden;
		}

	public int getStructureHiliteMode(int column) {
		return mColumnInfo[column].structureHiliteMode;
		}

	/**
	 * Returns the number of significant digits used to show numerical values
	 * of this column. If no rounding is applied, then 0 is returned.
	 * @param column
	 * @return 0 or number of significant digits
	 */
	public int getColumnSignificantDigits(int column) {
		return mColumnInfo[column].significantDigits;
		}

	public boolean hasReferencedDetail() {
		for (int column=0; column<mColumns; column++)
			for (int detail=0; detail<getColumnDetailCount(column); detail++)
				if (!getColumnDetailSource(column, detail).equals(CompoundTableDetailHandler.EMBEDDED))
					return true;

		return false;
		}

	public void addOffsetToEmbeddedDetailIDs(int offset) {
		for (int column=0; column<mColumns; column++) {
			for (int detail=0; detail<getColumnDetailCount(column); detail++) {
				if (getColumnDetailSource(column, detail).equals(CompoundTableDetailHandler.EMBEDDED)) {
					for (int row=0; row<mRecords; row++) {
						String[][] reference = mRecord[row].getDetailReferences(column);
						if (reference != null && reference[detail] != null) {
							for (int i=0; i<reference[detail].length; i++) {
								try {
									int id = Integer.parseInt(reference[detail][i]);
									if (id < 0)
										reference[detail][i] = ""+(id-offset);
									else
										reference[detail][i] = ""+(id+offset);
									}
								catch (NumberFormatException nfe) {}
								}
							}
						}
					}
				}
			}
		mDetailHandler.addOffsetToEmbeddedDetailIDs(offset);
		}

	public void removeSelected() {
		for (int row=0; row<mRecords; row++)
			if ((mRecord[row].mFlags & mAllocatedExclusionFlags) != 0)
				mRecord[row].mFlags &= ~CompoundRecord.cFlagMaskSelected;

		removeRecords(CompoundRecord.cFlagMaskSelected);
		}

	public void removeInvisible() {
		removeRecords(mAllocatedExclusionFlags);
		mDirtyCompoundFlags &= ~mAllocatedExclusionFlags;
		}

	public void finalizeDeletion() {
		removeRecords(CompoundRecord.cFlagMaskDeleted);
		}

	private void removeRecords(long mask) {
		if (mActiveRow != null && (mActiveRow.mFlags & mask) != 0)
			setActiveRow(null);

		if (mHighlightedRow != null && (mHighlightedRow.mFlags & mask) != 0)
			setHighlightedRow(null);

		int removalCount = 0;
		boolean visibleChanged = false;
		for (int row=0; row<mRecords; row++) {
			if ((mRecord[row].mFlags & mask) != 0) {
				removalCount++;
				if ((mRecord[row].mFlags & mAllocatedExclusionFlags) == 0)
					visibleChanged = true;
				}
			}

		if (removalCount != 0) {
			removeDeadReferences(mask);

			boolean calculateDescriptors = (mSMPWorkingThreads != null);
			if (calculateDescriptors)
				stopDescriptorCalculation();

			int index = 0;
			int[] mapping = new int[mRecord.length - removalCount];
			CompoundRecord[] newRecord = new CompoundRecord[mRecord.length - removalCount];
			for (int row=0; row<mRecords; row++)
				if ((mRecord[row].mFlags & mask) != 0)
					mRecord[row].mFlags |= CompoundRecord.cFlagMaskDeleted;
				else {
					mapping[index] = mRecord[row].mOriginalIndex;
					newRecord[index] = mRecord[row];
					newRecord[index].mOriginalIndex = index;
					index++;
					}

			mRecords -= removalCount;
			mRecord = newRecord;

			analyzeDataAfterRemoval();

			if (calculateDescriptors)
				updateDescriptors();

			TableModelEvent tme = null;
			if (visibleChanged) {
				compileVisibleRecords();
				tme = new TableModelEvent(this, 0, mNonExcludedRecords-1,
								TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
				}
			fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cDeleteRows, mapping), tme);
			}
		}

	/**
	 * For any column that is referencing IDs in another column, during deletion of rows this method
	 * removes those IDs from the referencing colums, which are going to be deleted from the
	 * corresponding referenced column. It is assumed that the referenced column contains unique keys.
	 * @param deletionMask
	 */
	private void removeDeadReferences(long deletionMask) {
		for (int column=0; column<mColumnInfo.length; column++) {
			int idColumn = findColumn(mColumnInfo[column].getProperty(cColumnPropertyReferencedColumn));
			if (idColumn != -1) {
				TreeSet<String> idSet = new TreeSet<String>();
				for (int row=0; row<mRecords; row++) {
					if ((mRecord[row].mFlags & deletionMask) != 0) {
						String id = encodeData(mRecord[row], idColumn);
						if (id.length() != 0)
							idSet.add(id);
						}
					}

				if (idSet.size() != 0) {
					for (int row=0; row<mRecords; row++) {
						if ((mRecord[row].mFlags & deletionMask) == 0) {
							String references = encodeData(mRecord[row], column);
							if (references.length() != 0) {
								String[] reference = separateEntries(references);
								int count = reference.length;
								for (int i=0; i<reference.length; i++) {
									if (reference[i].length() == 0 || idSet.contains(reference[i])) {
										reference[i] = null;
										count--;
										}
									}
								if (reference.length != count) {
									if (count == 0) {
										mRecord[row].setData(null, column);
										}
									else if (count == 1) {
										for (String ref:reference) {
											if (ref != null) {
												mRecord[row].setData(ref.getBytes(), column);
												break;
												}
											}
										}
									else {
										StringBuilder sb = null;
										for (String ref:reference) {
											if (ref != null) {
												if (sb == null)
													sb = new StringBuilder(ref);
												else
													sb.append(cEntrySeparator + ref);
												}
											}
										mRecord[row].setData(sb.toString().getBytes(), column);
										}
									}
								}
							}
						}
					}
				}
			}
		}

	public synchronized void sort(int column, boolean descending, boolean selectedFirst) {
		boolean calculateDescriptors = (mSMPWorkingThreads != null);
		if (calculateDescriptors)
			stopDescriptorCalculation();

		String specialType = getColumnSpecialType(column);
		if (cColumnTypeIDCode.equals(specialType))
			Arrays.sort(mRecord, new IDCodeComparator(column, descending, selectedFirst));
		else if (mColumnInfo[column].type == cColumnTypeRangeCategory
		 || (mColumnInfo[column].type & cColumnTypeDouble) != 0)
			Arrays.sort(mRecord, new DoubleComparator(column, descending, selectedFirst));
		else
			Arrays.sort(mRecord, new StringComparator(column, descending, selectedFirst));

		mLastSortColumn = descending ? -1 : column;

		compileVisibleRecords();

		if (calculateDescriptors)
			updateDescriptors();

		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeSortOrder, -1),
				   new TableModelEvent(this, 0, mNonExcludedRecords-1,
							TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
		}

	/**
	 * @return last sorted column if order was ascending, otherwise -1
	 */
	public int getLastSortColumn() {
		return mLastSortColumn;
		}

	public boolean isLogarithmicViewMode(int column) {
		return mColumnInfo[column].logarithmicViewMode;
		}

	/**
	 * Returns whether this column is a categoryColumn that has at least one row,
	 * which belongs to more than one category, i.e. which has multiple entries of
	 * which at lest one is different from the other ones.
	 * @param column
	 * @return
	 */
	public boolean isMultiCategoryColumn(int column) {
		return mColumnInfo[column].belongsToMultipleCategories;
		}

	/**
	 * Returns whether this column has at least one row that has more than one
	 * entries, no matter whether these entries are the same or different.
	 * @param column
	 * @return
	 */
	public boolean isMultiEntryColumn(int column) {
		return mColumnInfo[column].hasMultipleEntries;
		}

	public boolean isMultiLineColumn(int column) {
		return mColumnInfo[column].containsMultiLineText;
		}

	public boolean isDescriptorColumn(int column) {
		return (column >= 0) && DescriptorHelper.isDescriptorShortName(getColumnSpecialType(column));
		}
	
	public boolean isDescriptorAvailable(int column) {
		return !mColumnInfo[column].isDescriptorIncomplete;
		}

	public boolean isColumnDataComplete(int column) {
		return mColumnInfo[column].isComplete;
		}

	/**
	 * @param column
	 * @return true if every row has a unique not empty value
	 */
	public boolean isColumnDataUnique(int column) {
		return mColumnInfo[column].isUnique;
		}

	/**
	 * Checks whether the column is considered to contain a category data type,
	 * which is true for numerical or data columns with less than cMaxDateOrDoubleCategoryCount
	 * distinct entries or for text columns with less than cMaxTextCategoryCount entries.
	 * Columns of type RangeCategory also belong to type Category.
	 * If column < 0, then false is returned.
	 * @param column
	 * @return
	 */
	public boolean isColumnTypeCategory(int column) {
		return (column >= 0) && (mColumnInfo[column].type & cColumnTypeCategory) != 0;
		}

	public boolean isColumnTypeRangeCategory(int column) {
		return (column >= 0) && (mColumnInfo[column].type & cColumnTypeRangeCategory) == cColumnTypeRangeCategory;
		}

	/**
	 * Checks whether the column exclusively contains values that can be
	 * numerically interpreted. Accepted are multiple values per cell, values with
	 * modifiers as (<,>=,?), NaN or empty values and date values.
	 * If column < 0, then false is returned.
	 * @param column
	 * @return
	 */
	public boolean isColumnTypeDouble(int column) {
		return (column >= 0) && (mColumnInfo[column].type & cColumnTypeDouble) != 0;
		}

	/**
	 * Checks whether the column exclusively contains values that can be
	 * interpreted as date values. Accepted are multiple values per cell,
	 * empty values and interpretable date values.
	 * If column < 0, then false is returned.
	 * @param column
	 * @return
	 */
	public boolean isColumnTypeDate(int column) {
		return (column >= 0) && (mColumnInfo[column].type & cColumnTypeDate) != 0;
		}

	/**
	 * If a column contains values that cannot be interpreted numerically
	 * or as data values, they are considered type 'String'.
	 * At the same time they may also have type 'Category'.
	 * If column < 0, then false is returned.
	 * @param column
	 * @return
	 */
	public boolean isColumnTypeString(int column) {
		return (column >= 0) && (mColumnInfo[column].type & cColumnTypeString) != 0;
		}

	public boolean isColumnWithModifiers(int column) {
		return mColumnInfo[column].hasModifiers;
		}

	public String getColumnSpecialType(int column) {
		return getColumnProperty(column, cColumnPropertySpecialType);
		}

	public boolean hasDescriptorColumn(int column) {
		for (int i=0; i<mColumnInfo.length; i++)
			if (mColumnInfo[i].descriptorHandler != null
			 && mColumnInfo[column].name.equals(getColumnProperty(i, cColumnPropertyParentColumn)))
				return true;
		return false;
		}

	public void setColumnProperty(int column, String key, String value) {
		mColumnInfo[column].setProperty(key, value);
		}

	public final HashMap<String,String> getColumnProperties(int column) {
		return mColumnInfo[column].getProperties();
		}

	public String getColumnProperty(int column, String key) {
		return mColumnInfo[column].getProperty(key);
		}

	/**
	 * Copies all column properties and view options (significant digits, log view mode)
	 * @param sourceColumn
	 * @param targetColumn
	 * @param targetTableModel
	 */
	public void copyColumnProperties(int sourceColumn, int targetColumn, CompoundTableModel targetTableModel) {
		HashMap<String,String> properties = mColumnInfo[sourceColumn].getProperties();
		for (String key:properties.keySet())
			targetTableModel.setColumnProperty(targetColumn, key, properties.get(key));
		targetTableModel.setColumnSignificantDigits(targetColumn, getColumnSignificantDigits(sourceColumn));
		targetTableModel.setLogarithmicViewMode(targetColumn, isLogarithmicViewMode(sourceColumn));
		}

	/**
	 * Checks whether the column is numerical and has some variation in its values
	 * or whether it contains more than one categories.
	 * @param column
	 * @return true if column getDoubleAt(column) delivers useful and varying values
	 */
	public boolean hasNumericalVariance(int column) {
		if ((mColumnInfo[column].type & cColumnTypeDouble) != 0)
			return (mColumnInfo[column].minValue < mColumnInfo[column].maxValue);
		if ((mColumnInfo[column].type & cColumnTypeCategory) != 0)
			return (mColumnInfo[column].categoryList.getSize() > 1);

		return false;
		}

	public boolean isDoubleColumnAvailable() {
		for (int i=0; i<mColumns; i++)
			if ((mColumnInfo[i].type & cColumnTypeDouble) != 0)
				return true;

		return false;
		}

	public boolean isVisible(CompoundRecord record) {
		return (record.mFlags & mAllocatedExclusionFlags) == 0;
		}

	public boolean isVisibleAndSelected(CompoundRecord record) {
		long mask = mAllocatedExclusionFlags | CompoundRecord.cFlagMaskSelected;
		return (record.mFlags & mask) == CompoundRecord.cFlagMaskSelected;
		}

	public boolean isSelected(int index) {
		return ((mNonExcludedRecord[index].mFlags & CompoundRecord.cFlagMaskSelected) != 0);
		}

	public boolean isVisibleNeglecting(CompoundRecord record, int exclusionFlagNo) {
		long mask = mAllocatedExclusionFlags & ~convertCompoundFlagToMask(exclusionFlagNo);
		return (record.mFlags & mask) == 0;
		}

	public void updateLocalExclusion(int exclusionFlagNo, boolean isAdjusting, boolean recordsLocallyExcluded) {
		long mask = convertCompoundFlagToMask(exclusionFlagNo);
		if (recordsLocallyExcluded)
			mDirtyCompoundFlags |= mask;
		else
			mDirtyCompoundFlags &= ~mask;

		updateVisibleRecords(isAdjusting, exclusionFlagNo);
		}

	public void clearSelected(int index) {
		mNonExcludedRecord[index].mFlags &= ~CompoundRecord.cFlagMaskSelected;
		}

	public void setSelected(int index) {
		mNonExcludedRecord[index].mFlags |= CompoundRecord.cFlagMaskSelected;
		}

	/**
	 * Allocates an opaque flag number to be used for filtering or hitlists.
	 * Flags obtained this way should be returned with freeCompoundFlag() once
	 * they are not used anymore.
	 * @param useForExclusion
	 * @return flagNo >= 0 or -1 if no flag available
	 */
	public int getUnusedCompoundFlag(boolean useForExclusion) {
		int flagNo = CompoundRecord.cFlagFirstUnusedFlagNo;
		long bit = (1L << flagNo);
		while ((bit & mAllocatedCompoundFlags) != 0) {
			if (flagNo == CompoundRecord.cFlagLastUnusedFlagNo)
				return -1;
			bit <<= 1;
			flagNo++;
			}

		mAllocatedCompoundFlags |= bit;
		if (useForExclusion)
			mAllocatedExclusionFlags |= bit;

		return flagNo;
		}

	public void setCompoundFlagDirty(int flagNo) {
		mDirtyCompoundFlags |= (1L << flagNo);
		}

	public void freeCompoundFlag(int flagNo) {
		clearCompoundFlag(flagNo);
		long mask = convertCompoundFlagToMask(flagNo);
		mAllocatedCompoundFlags &= ~mask;
		mAllocatedExclusionFlags &= ~mask;
		}

	public void clearCompoundFlag(int flagNo) {
		long mask = convertCompoundFlagToMask(flagNo);
		if ((mDirtyCompoundFlags & mask) != 0) {
			for (int row=0; row<mRecords; row++)
				mRecord[row].mFlags &= ~mask;

			mDirtyCompoundFlags &= ~mask;

			if (mRecords != 0
			 && (mask & mAllocatedExclusionFlags) != 0)
				updateVisibleRecords(false, flagNo);
			}
		}

	public long convertCompoundFlagToMask(int flagNo) {
		return (flagNo == -1) ? 0L : (1L << flagNo);
		}

	public void invertExclusion(int exclusionFlagNo) {
		long mask = convertCompoundFlagToMask(exclusionFlagNo);
		mDirtyCompoundFlags |= mask;
		for (int row=0; row<mRecords; row++) {
			if ((mRecord[row].mFlags & mask) == 0)
				mRecord[row].mFlags |= mask;
			else
				mRecord[row].mFlags &= ~mask;
			}

		updateVisibleRecords(false, exclusionFlagNo);
		}

	public void setCategoryExclusion(int exclusionFlagNo, int column,
									 boolean[] selection, boolean inverse) {
		long mask = convertCompoundFlagToMask(exclusionFlagNo);
		mDirtyCompoundFlags |= mask;

		CategoryList<?> categoryList = mColumnInfo[column].categoryList;

		for (int row=0; row<mRecords; row++) {
			String[] entry = separateEntries(encodeData(mRecord[row], column));
			if (entry.length == 1) {
				if (selection[categoryList.getIndexOfString(entry[0])] ^ inverse)
					mRecord[row].mFlags &= ~mask;
				else
					mRecord[row].mFlags |= mask;
				}
			else {	  // record may belong to more than one categories
				boolean visible = false;
				for (int i=0; i<entry.length; i++) {
					if (selection[categoryList.getIndexOfString(entry[i])]) {
						visible = true;
						break;
						}
					}

				if (!visible
				 && mColumnInfo[column].belongsToMultipleCategories
				 && selection[categoryList.getSize()]) {
					for (int i=1; i<entry.length; i++) {
						if (!entry[i-1].equals(entry[i])) {
							visible = true;
							break;
							}
						}
					}

				if (visible ^ inverse)
					mRecord[row].mFlags &= ~mask;
				else
					mRecord[row].mFlags |= mask;
				}
			}

		updateVisibleRecords(false, exclusionFlagNo);
		}

	public void initializeDoubleExclusion(int exclusionFlagNo, int column) {
		if (!mColumnInfo[column].isComplete) {
			long mask = convertCompoundFlagToMask(exclusionFlagNo);
			mDirtyCompoundFlags |= mask;
			for (int row=0; row<mRecords; row++)
				if (Float.isNaN(mRecord[row].mFloat[column]))
					mRecord[row].mFlags |= mask;

			updateVisibleRecords(false, exclusionFlagNo);
			}
		}

	public void setStringExclusion(int column, int exclusionFlagNo,
								   String queryString, int type,
								   boolean caseSensitive, boolean inverse) {
		if (!caseSensitive)
			queryString = queryString.toLowerCase();

		String[] queryList = null;
		if (queryString.contains(",")) {
			queryList = queryString.split(",");
			for (int i=0; i<queryList.length; i++)
				queryList[i] = queryList[i].trim();
			}
		else {
			queryList = new String[1];
			queryList[0] = queryString;
			}
			
		long mask = convertCompoundFlagToMask(exclusionFlagNo);
		for (int row=0; row<mRecords; row++) {
			String theString = encodeData(mRecord[row], column);
			if (!caseSensitive)
				theString = theString.toLowerCase();
			boolean found = false;
			for (String query:queryList) {
				switch (type) {
				case cStringExclusionTypeEquals:
					if (theString.equals(query))
						found = true;
					break;
				case cStringExclusionTypeStartsWith:
					if (theString.startsWith(query))
						found = true;
					break;
				case cStringExclusionTypeContains:
					if (theString.indexOf(query) != -1)
						found = true;
					break;
				case cStringExclusionTypeRegEx:
					if (theString.matches(query))
						found = true;
					break;
					}
				if (found)
					break;
				}
			if (inverse ^ found)
				mRecord[row].mFlags &= ~mask;
			else
				mRecord[row].mFlags |= mask;
			}

		mDirtyCompoundFlags |= mask;

		updateVisibleRecords(false, exclusionFlagNo);
		}

	public void setDoubleExclusion(int column, int exclusionFlagNo,
								   float low, float high,
								   boolean inverse, boolean isAdjusting) {
		long mask = convertCompoundFlagToMask(exclusionFlagNo);
		for (int row=0; row<mRecords; row++) {
			float value = mRecord[row].mFloat[column];
			if (Float.isNaN(value) || Float.isInfinite(value)) {
				if (inverse)
					mRecord[row].mFlags &= ~mask;
				else
					mRecord[row].mFlags |= mask;
				}
			else {
				if (inverse ^ (value < low || value > high))
					mRecord[row].mFlags |= mask;
				else
					mRecord[row].mFlags &= ~mask;
				}
			}

		mDirtyCompoundFlags |= mask;

		updateVisibleRecords(isAdjusting, exclusionFlagNo);
		}

	public void setHitlistExclusion(int hitlistIndex, int exclusionFlagNo, boolean inverse) {
		long mask = convertCompoundFlagToMask(exclusionFlagNo);
		if (hitlistIndex == CompoundTableHitlistHandler.HITLISTINDEX_NONE) {
			for (int row=0; row<mRecords; row++) {
				if (inverse)
					mRecord[row].mFlags |= mask;
				else
					mRecord[row].mFlags &= ~mask;
				}
			}
		else {
			long hitlistMask = mHitlistHandler.getHitlistMask(hitlistIndex);
			for (int row=0; row<mRecords; row++) {
				if (inverse ^ ((mRecord[row].mFlags & hitlistMask) != 0))
					mRecord[row].mFlags &= ~mask;
				else
					mRecord[row].mFlags |= mask;
				}
			}

		mDirtyCompoundFlags |= mask;

		updateVisibleRecords(false, exclusionFlagNo);
		}

	/**
	 * Sets the substructure exclusion flags in parallel threads on all available cores.
	 * @param exclusionFlagNo no of allocated filter flag
	 * @param idcodeColumn
	 * @param fragment
	 * @param inverse
	 */
	public void setSubStructureExclusion(final int exclusionFlagNo, final int idcodeColumn, final StereoMolecule[] fragment, final boolean inverse) {
		int threadCount = Runtime.getRuntime().availableProcessors();
		mSSSRecordIndex = new AtomicInteger(mRecord.length*fragment.length);

		final int fingerprintColumn = getChildColumn(idcodeColumn, DESCRIPTOR_FFP512.shortName);
		final long mask = convertCompoundFlagToMask(exclusionFlagNo);

		// These fragments instances are read my multiple threads simultaneously.
		// To prevent collision we must calculate all helper arrays in advance
		for (StereoMolecule f:fragment)
			f.ensureHelperArrays(Molecule.cHelperParities);

		// set flag: excluded as default
		for (int row=0; row<mRecord.length; row++)
			mRecord[row].mFlags |= mask;

		Thread[] worker = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			worker[i] = new Thread("SSS-Matcher "+(i+1)) {
				public void run() {
					SSSearcherWithIndex searcherWithIndex = new SSSearcherWithIndex();

					int combinedIndex = mSSSRecordIndex.decrementAndGet();
					int fragmentIndex = -1;
					while (combinedIndex >= 0) {
						int recordIndex = combinedIndex % mRecord.length;
						if ((mRecord[recordIndex].mFlags & mask) != 0) {
							int newFragmentIndex = combinedIndex / mRecord.length;
							if (fragmentIndex != newFragmentIndex) {
								fragmentIndex = newFragmentIndex;
								searcherWithIndex.setFragment(fragment[fragmentIndex], null);
								}
	
							byte[] idcode = (byte[])mRecord[recordIndex].getData(idcodeColumn);
							if (idcode != null) {
								searcherWithIndex.setMolecule(idcode, (int[])mRecord[recordIndex].getData(fingerprintColumn));
								if (searcherWithIndex.isFragmentInMolecule())
									mRecord[recordIndex].mFlags &= ~mask;
								}
							}

						combinedIndex = mSSSRecordIndex.decrementAndGet();
						}
					}
				};
			worker[i].setPriority(Thread.MIN_PRIORITY);
			worker[i].start();
			}

		for (Thread t:worker)
			try { t.join(); } catch (InterruptedException e) {}

		// optionally invert flag
		if (inverse)
			for (int row=0; row<mRecord.length; row++)
				mRecord[row].mFlags ^= mask;

		mDirtyCompoundFlags |= mask;
		updateVisibleRecords(false, exclusionFlagNo);
		}

	public void setSimilarityExclusion(int exclusionFlagNo, int descriptorColumn,
									   StereoMolecule[] molecule,
									   float[][] similarity, float minSimilarity,
									   boolean inverse, boolean isAdjusting) {
		long mask = convertCompoundFlagToMask(exclusionFlagNo);
		for (int row=0; row<mRecords; row++) {
			Object descriptor = mRecord[row].getData(descriptorColumn);
			if (descriptor == null) {
				if (inverse)
					mRecord[row].mFlags &= ~mask;
				else
					mRecord[row].mFlags |= mask;
				}
			else {
				boolean found = false;
				for (int i=0; i<similarity.length; i++) {
					if (similarity[i][mRecord[row].mOriginalIndex]  >= minSimilarity) {
						found = true;
						break;
						}
					}
				if (inverse ^ found)
					mRecord[row].mFlags &= ~mask;
				else
					mRecord[row].mFlags |= mask;
				}
			}

		mDirtyCompoundFlags |= mask;

		updateVisibleRecords(isAdjusting, exclusionFlagNo);
		}

	/**
	 * May be used by derived classes to set explicit atom color. Checks for child column
	 * of idcodeColumn that contains atom color instructions and modifies mol accordingly.
	 * The atomColorMode defines whether and to which extend atoms are in color. For displaying mol on the
	 * screen use ATOM_COLOR_MODE_ALL, for printing use ATOM_COLOR_MODE_EXPLICIT and for copy/paste,
	 * drag&drop, cheminformatics purposes, etc use ATOM_COLOR_MODE_NONE.
	 * @param record
	 * @param idcodeColumn
	 * @param atomColorMode one of ATOM_COLOR_MODE_...
	 * @param mol molecule that represents the idcode from record's idcodeColumn
	 */
	public void colorizeAtoms(CompoundRecord record, int idcodeColumn, int atomColorMode, StereoMolecule mol) {
		if (atomColorMode != ATOM_COLOR_MODE_NONE) {
			int infoColumn = getChildColumn(idcodeColumn, cColumnTypeAtomColorInfo);
			if (infoColumn != -1) {
				String[] entry = separateEntries(encodeData(record, infoColumn));
				for (int j=0; j<entry.length; j++) {
					if (entry[j].startsWith("red:"))
						setAtomColors(mol, entry[j].substring(4), Molecule.cAtomColorRed);
					else if (entry[j].startsWith("blue:"))
						setAtomColors(mol, entry[j].substring(5), Molecule.cAtomColorBlue);
					else if (entry[j].startsWith("green:"))
						setAtomColors(mol, entry[j].substring(6), Molecule.cAtomColorGreen);
					else if (entry[j].startsWith("magenta:"))
						setAtomColors(mol, entry[j].substring(8), Molecule.cAtomColorMagenta);
					else if (entry[j].startsWith("orange:"))
						setAtomColors(mol, entry[j].substring(7), Molecule.cAtomColorOrange);
					else if (entry[j].startsWith("darkGreen:"))
						setAtomColors(mol, entry[j].substring(10), Molecule.cAtomColorDarkGreen);
					else if (entry[j].startsWith("darkRed:"))
						setAtomColors(mol, entry[j].substring(8), Molecule.cAtomColorDarkRed);
					}
				}
			}
		}

	protected void setAtomColors(StereoMolecule mol, String atomNoList, int color) {
		int index1 = 0;
		while (true) {
			int index2 = atomNoList.indexOf(',', index1+1);
			try {
				int atom = Integer.parseInt((index2 == -1) ?
									atomNoList.substring(index1)
								  : atomNoList.substring(index1, index2));
				if (mol.getAtomColor(atom) == Molecule.cAtomColorBlack)
					mol.setAtomColor(atom, color);
				}
			catch (NumberFormatException e) {
				return;
				}

			if (index2 == -1)
				return;

			index1 = index2+1;
			}
		}

	protected void unselectInvisibleRecords() {
		for (int row=0; row<mRecords; row++)
			if ((mRecord[row].mFlags & mAllocatedExclusionFlags) != 0)
				mRecord[row].mFlags &= ~CompoundRecord.cFlagMaskSelected;
		}

	public float[] createSimilarityList(Object chemObject, int descriptorColumn) {
		Object refDescriptor = mColumnInfo[descriptorColumn].getCachedDescriptor(chemObject);
		DescriptorHandler descriptorHandler = mColumnInfo[descriptorColumn].descriptorHandler;
		float[] similarity = new float[mRecord.length];
		for (int row=0; row<mRecord.length; row++) {
			Object descriptor = mRecord[row].getData(descriptorColumn);
			if (descriptor != null)
				similarity[mRecord[row].mOriginalIndex]
						= (float)descriptorHandler.getSimilarity(refDescriptor, descriptor);
			}
		return similarity;
		}

	/**
	 * Calculates multi-threaded on all available cores the similarities of all records
	 * against the given chemistry object (molecule or reaction) or the given descriptor.
	 * This method returns before the calculation is finished. The calling thread needs to
	 * wait until the stopProgress() is called on the ProgressController and must then
	 * call getSimilarityListSMP() to obtain the result.
	 * @param chemObject null or StereoMolecule/Reaction
	 * @param descriptor null if chemObject != null and vice versa
	 * @param descriptorColumn column containing the descriptor
	 * @param pc the ProgressController informed about progress and when it is done
	 */
	public void createSimilarityListSMP(final Object chemObject, final Object descriptor, final int descriptorColumn, final ProgressController pc) {
		mSimilarityListSMP = new float[mRecord.length];

		new Thread("Similarity Calculator") {
			public void run() {
				final int threadCount = Runtime.getRuntime().availableProcessors();
				mSMPRecordIndex = new AtomicInteger(mRecord.length);
				mSMPWorkingThreads = new AtomicInteger(threadCount);
				mSMPErrorCount = 0;

				final DescriptorHandler dh = mColumnInfo[descriptorColumn].descriptorHandler;

				pc.startProgress("Calculating query descriptor...", 0, 0);
		
				final Object refDescriptor = (descriptor != null) ? descriptor
						: mColumnInfo[descriptorColumn].getCachedDescriptor(chemObject);
				if (pc.threadMustDie()) {
					pc.stopProgress();
					mSimilarityListSMP = null;
					return;
					}
		
				pc.startProgress("Calculating similarities...", 0, mRecord.length);

				for (int i=0; i<threadCount; i++) {
					Thread t = new Thread(getColumnSpecialType(descriptorColumn)+" calculator "+(i+1)) {
						public void run() {
							int recordIndex = mSMPRecordIndex.decrementAndGet();
							while (recordIndex >= 0 && !pc.threadMustDie()) {
								if (pc.threadMustDie())
									break;
		
								try {
									Object descriptor = mRecord[recordIndex].getData(descriptorColumn);
									if (descriptor != null)
										mSimilarityListSMP[mRecord[recordIndex].mOriginalIndex]
												= (float)dh.getSimilarity(refDescriptor, descriptor);
									}
								catch (Exception e) {
									mSMPErrorCount++;
									}
		
								pc.updateProgress(-1);
								recordIndex = mSMPRecordIndex.decrementAndGet();
								}
		
							if (mSMPWorkingThreads.decrementAndGet() == 0) {
								if (pc.threadMustDie())
									mSimilarityListSMP = null;
								else if (mSMPErrorCount != 0)
									pc.showErrorMessage(getColumnSpecialType(descriptorColumn)
											+" similarity calculation failed on "+mSMPErrorCount+" molecules.");
		
								pc.stopProgress();
								}
							}
						};
					t.setPriority(Thread.MIN_PRIORITY);
					t.start();
					}
				}
			}.start();
		}

	/**
	 * Returns the most recent similarity list that was creates by a call to createSimilarityListSMP().
	 * This method should only be called after progressController.stopProgress() has been called
	 * by createSimilarityListSMP(). The cached similarity list is cleared upon this call.
	 * @return similarity list
	 */
	public float[] getSimilarityListSMP() {
		float[] list = mSimilarityListSMP;
		mSimilarityListSMP = null;
		return list;
		}

	/**
	 * Returns a list of visual categories, i.e. the list of distinct entries
	 * plus cTextMultipleCategories if some records contain multiple entries
	 * being different from each other.
	 * @param column
	 * @return
	 */
	public String[] getCategoryList(int column) {
		int listSize = mColumnInfo[column].categoryList.getSize();
		if (mColumnInfo[column].belongsToMultipleCategories)
			listSize++;
		String[] categoryList = new String[listSize];
		for (int i=0; i<mColumnInfo[column].categoryList.getSize(); i++)
			categoryList[i] = mColumnInfo[column].categoryList.getString(i);
		if (mColumnInfo[column].belongsToMultipleCategories)
			categoryList[categoryList.length-1] = cTextMultipleCategories;
		return categoryList;
		}

	/**
	 * Returns the list of distinct entries of this column.
	 * This does not include the pseudo item 'multiple categories'.
	 * @param column
	 * @return distinct entry list
	 */
	public CategoryList<?> getNativeCategoryList(int column) {
		return mColumnInfo[column].categoryList;
		}

	public String[] getCategoryCustomOrder(int column) {
		UniqueList<String> order = mColumnInfo[column].mCategoryCustomOrder;
		return (order == null || order.size() == 0) ? null : order.toArray(new String[0]);
		}

	public void setCategoryCustomOrder(int column, String[] customOrder) {
		if (customOrder == null || customOrder.length == 0) {
			mColumnInfo[column].mCategoryCustomOrder = null;
			}
		else {
			mColumnInfo[column].mCategoryCustomOrder = new UniqueList<String>();
			for (String s:customOrder)
				mColumnInfo[column].mCategoryCustomOrder.add(s);
			}
		mColumnInfo[column].categoryList = setupCategoryList(column);
		assignRecordsToCategories(column);
		fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, column), null);
		}

	public void setHighlightedRow(CompoundRecord record) {
		if (mHighlightedRow != record) {
			mHighlightedRow = record;
			for (int i=0; i<mHighlightListener.size(); i++)
				((HighlightListener)mHighlightListener.get(i)).highlightChanged(record);
			}
		}

	public void setHighlightedRow(int row) {
		setHighlightedRow((row == -1) ? null : mNonExcludedRecord[row]);
		}

	public CompoundRecord getHighlightedRow() {
		return mHighlightedRow;
		}

	public void setActiveRow(CompoundRecord record) {
		if (mActiveRow != record) {
			mActiveRow = record;
			fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeActiveRow, -1), null);
			}
		}

	public void setActiveRow(int row) {
		setActiveRow((row == -1) ? null : mNonExcludedRecord[row]);
		}

	public CompoundRecord getActiveRow() {
		return mActiveRow;
		}

	public int getActiveRowIndex() {
		for (int row=0; row<mNonExcludedRecords; row++)
			if (mNonExcludedRecord[row] == mActiveRow)
				return row;

		return -1;
		}

	public void fireCompoundTableChanged(CompoundTableEvent e) {
		for (int i=mCompoundTableListener.size()-1; i>=0; i--)
			((CompoundTableListener)mCompoundTableListener.get(i)).compoundTableChanged(e);
		}

	/**
	 * Analyzes all column data, determines column types, column completeness,
	 * updates outdated idcodes and starts the descriptor updating thread.
	 * If (firstRow != 0) then we append data. In this case only the rows
	 * from firstRow are analyzed and the previous analysis results are
	 * updated accordingly.
	 * @param firstRow != 0 if rows were appended
	 * @param listener null or listener to be informed of progress
	 */
	private void analyzeData(int firstRow, ProgressListener listener) {
		if (listener != null)
			listener.startProgress("Analyzing columns...", 0, mColumnInfo.length);

		for (int column=0; column<mColumnInfo.length; column++) {
			if (listener != null)
				listener.updateProgress(column+1);

			analyzeColumn(column, firstRow, false);
			}

		for (int column=0; column<mColumnInfo.length; column++)
			if (cColumnTypeIDCode.equals(getColumnSpecialType(column)))
				checkIDCodeVersion(column, firstRow, listener);

		mRecords = mRecord.length;
		mColumns = mColumnInfo.length;

		updateDescriptors();
		}

	private void checkIDCodeVersion(int idcodeColumn, int firstRow, ProgressListener listener) {
		IDCodeParser parser2D = new IDCodeParser(true);
		boolean oldVersionFound = false;
		for (int row=firstRow; row<mRecord.length; row++) {
			byte[] idcode = (byte[])mRecord[row].getData(idcodeColumn);
			if (idcode != null
			 && parser2D.getIDCodeVersion(idcode) != Canonizer.cIDCodeCurrentVersion) {
				oldVersionFound = true;
				break;
				}
			}
		if (oldVersionFound) {
			IDCodeParser parser3D = new IDCodeParser(false);
			int coords2DColumn = this.getChildColumn(idcodeColumn, cColumnType2DCoordinates);
			int coords3DColumn = this.getChildColumn(idcodeColumn, cColumnType3DCoordinates);

			if (listener != null)
				listener.startProgress("Updating ID-codes of '"+this.getColumnTitle(idcodeColumn)+"'...", 0, mRecords);

			StereoMolecule mol = new StereoMolecule();
			for (int row=firstRow; row<mRecord.length; row++) {
				if (listener != null && row % 16 == 15)
					listener.updateProgress(row);

				byte[] idcode = (byte[])mRecord[row].getData(idcodeColumn);
				if (idcode != null) {
					boolean coordsAre3D = false;
					byte[] coords = null;
					if (coords2DColumn != -1)
						coords = (byte[])mRecord[row].getData(coords2DColumn);
					if (coords == null && coords3DColumn != -1) {
						coords = (byte[])mRecord[row].getData(coords3DColumn);
						coordsAre3D = true;
						}
					boolean absolute = parser2D.coordinatesAreAbsolute(coords);
					if (coordsAre3D)
						parser3D.parse(mol, idcode, coords);
					else
						parser2D.parse(mol, idcode, coords);

					Canonizer canonizer = new Canonizer(mol);
					mRecord[row].setData(canonizer.getIDCode().getBytes(), idcodeColumn);
					if (coords2DColumn != -1)
						mRecord[row].setData((coordsAre3D) ? null : canonizer.getEncodedCoordinates(absolute).getBytes(), coords2DColumn);
					if (coords3DColumn != -1)
						mRecord[row].setData((!coordsAre3D) ? null : canonizer.getEncodedCoordinates(absolute).getBytes(), coords3DColumn);
					}
				}
			}
		}

	private void analyzeDataAfterRemoval() {
		for (int column=0; column<mColumns; column++) {
			analyzeColumn(column, 0, true);

			if (getColumnProperty(column, cColumnPropertyDetailCount) != null)
				removeUnusedDetailInfo(column);
			}

		removeUnreferencedDetails();
		}

	private void removeUnreferencedDetails() {
		if (mDetailHandler == null || mDetailHandler.getEmbeddedDetailCount() == 0)
			return;

		boolean[][] containsEmbeddedDetail = new boolean[mColumns][];
		for (int column=0; column<mColumns; column++) {
			String countString = getColumnProperty(column, cColumnPropertyDetailCount);
			try {
				int detailCount = Integer.parseInt(countString);
				for (int i=0; i<detailCount; i++) {
					String detailSource = getColumnProperty(column, cColumnPropertyDetailSource+i);
					if (detailSource.equals(CompoundTableDetailHandler.EMBEDDED)) {
						if (containsEmbeddedDetail[column] == null)
							containsEmbeddedDetail[column] = new boolean[detailCount];
						containsEmbeddedDetail[column][i] = true;
						}
					}
				}
			catch (NumberFormatException e) {}
			}

		TreeSet<String> embeddedReferences = new TreeSet<String>();
		for (int row=0; row<mRecords; row++) {
			if (mRecord[row].mDetailReference != null) {
				for (int column=0; column<mColumns; column++) {
					if (containsEmbeddedDetail[column] != null) {
						String[][] detail = mRecord[row].mDetailReference[column];
						if (detail != null)
							for (int i=0; i<detail.length; i++)
								if (containsEmbeddedDetail[column][i] && detail[i] != null)
									for (int j=0; j<detail[i].length; j++)
										if (!embeddedReferences.contains(detail[i][j]))
											embeddedReferences.add(detail[i][j]);
						}
					}
				}
			}

		if (mDetailHandler.getEmbeddedDetailCount() != embeddedReferences.size()) {
			Iterator<String> iterator = mDetailHandler.getEmbeddedDetailMap().keySet().iterator();
			while (iterator.hasNext())
				if (!embeddedReferences.contains(iterator.next()))
					iterator.remove();
			}
		}

	private void removeUnusedDetailInfo(int column) {
		String countString = getColumnProperty(column, cColumnPropertyDetailCount);
		int detailCount = -1;
		try { detailCount = Integer.parseInt(countString); } catch (NumberFormatException e) { return; }

		boolean[] detailFound = new boolean[detailCount];
		int detailFoundCount = 0;
		for (int row=0; row<mRecords && detailFoundCount<detailCount; row++) {
			String[][] detail = mRecord[row].getDetailReferences(column);
			if (detail != null) {
				for (int i=0; i<detail.length; i++) {
					if (detail[i] != null && !detailFound[i]) {
						detailFound[i] = true;
						detailFoundCount++;
						}
					}
				}
			}
		if (detailFoundCount != detailCount) {
			int[] newDetailIndex = new int[detailCount];
			int newIndex = 0;
			for (int i=0; i<detailCount; i++) {
				newDetailIndex[i] = newIndex;
				if (detailFound[i])
					newIndex++;
				}

			for (int i=0; i<detailCount; i++) {
				if (detailFound[i] && newDetailIndex[i] != i) {
					setColumnProperty(column, cColumnPropertyDetailName+newDetailIndex[i], getColumnProperty(column, cColumnPropertyDetailName+i));
					setColumnProperty(column, cColumnPropertyDetailType+newDetailIndex[i], getColumnProperty(column, cColumnPropertyDetailType+i));
					setColumnProperty(column, cColumnPropertyDetailSource+newDetailIndex[i], getColumnProperty(column, cColumnPropertyDetailSource+i));
					}
				if (!detailFound[i] || newDetailIndex[i] != i) {
					setColumnProperty(column, cColumnPropertyDetailName+i, null);
					setColumnProperty(column, cColumnPropertyDetailType+i, null);
					setColumnProperty(column, cColumnPropertyDetailSource+i, null);
					}
				}

			if (detailFoundCount == 0) {
				setColumnProperty(column, cColumnPropertyDetailCount, null);
				setColumnProperty(column, cColumnPropertyDetailSeparator, null);
				}
			else {
				setColumnProperty(column, cColumnPropertyDetailCount, ""+detailFoundCount);
				}

			for (int row=0; row<mRecords; row++) {
				String[][] detail = mRecord[row].getDetailReferences(column);
				if (detail != null) {
					if (newDetailIndex[detail.length] == 0)
						mRecord[row].setDetailReferences(column, null);
					else {
						String[][] newDetail = new String[newDetailIndex[detail.length]][];
						for (int i=0; i<detailCount; i++)
							if (detailFound[i])
								newDetail[newDetailIndex[i]] = detail[i];
						mRecord[row].setDetailReferences(column, newDetail);
						}
					}
				}

			for (int i=0; i<detailCount; i++)
				if (!detailFound[i])
					newDetailIndex[i] = -1;
			fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cRemoveColumnDetails, column, newDetailIndex), null);
			}
		}

	private void analyzeCompleteness(int column, int firstRow) {
		if (firstRow == 0) {
			mColumnInfo[column].isComplete = true;
			mColumnInfo[column].isCompleteChild = true;
			}

		int parentColumn = getParentColumn(column);
		if (mColumnInfo[column].isComplete
		 || (parentColumn != -1 && mColumnInfo[column].isCompleteChild)) {
			for (int row=firstRow; row<mRecord.length; row++) {
				if (mRecord[row].getData(column) == null) {
					mColumnInfo[column].isComplete = false;
					if (parentColumn == -1)
						return;
					if (mRecord[row].getData(parentColumn) != null) {
						mColumnInfo[column].isCompleteChild = false;
						return;
						}
					}
				}
			}
		}

	/**
	 * Analyzes this column's data, determines column type and completeness.
	 * If (firstRow != 0) then we append data. In this case only the rows
	 * from firstRow are analyzed and the previous analysis results are
	 * updated accordingly. The columns typeChanged flag is set accordingly.
	 * @param column the column to be analyzed
	 * @param firstRow != 0 if rows were appended
	 * @param isAfterDeletion
	 */
	private void analyzeColumn(int column, int firstRow, boolean isAfterDeletion) {
		analyzeCompleteness(column, firstRow);

		if (!isAfterDeletion
		 && mColumnInfo[column].descriptorHandler != null
		 && (!mColumnInfo[column].descriptorHandler.getVersion().equals(getColumnProperty(column, cColumnPropertyDescriptorVersion))
		  || !mColumnInfo[column].isCompleteChild))
			mColumnInfo[column].isDescriptorIncomplete = true;

		if (!isColumnDisplayable(column)
		 || mColumnInfo[column].type == cColumnTypeRangeCategory) {
			return;
			}

		if (mRecord.length == 0) {
			mColumnInfo[column].type = cColumnTypeString; // default column type
			return;
			}

		if (!isAfterDeletion
		 && getColumnProperty(column, cColumnPropertyDetailCount) != null) {
			mColumnInfo[column].hasDetail = true;
			String separator = getDetailSeparator(column);
			for (int row=firstRow; row<mRecord.length; row++)
				mRecord[row].separateDetail(separator, column);
			}

		mColumnInfo[column].containsMultiLineText = false;
		mColumnInfo[column].hasMultipleEntries = false;
		byte[] entrySeparator = cEntrySeparator.getBytes();
		for (int row=firstRow; row<mRecord.length; row++) {
			byte[] data = (byte[])mRecord[row].getData(column);
			if (data != null) {
				for (int i=0; i<data.length; i++) {
					if (data[i] == cLineSeparatorByte) {
						mColumnInfo[column].containsMultiLineText = true;
						mColumnInfo[column].hasMultipleEntries = true;
						break;
						}
					if (data[i] == entrySeparator[0] && !mColumnInfo[column].hasMultipleEntries) {
						boolean entrySeparatorFound = true;
						for (int j=1; j<entrySeparator.length; j++) {
							if (i+j == data.length || data[i+j] != entrySeparator[j]) {
								entrySeparatorFound = false;
								break;
								}
							}
						if (entrySeparatorFound)
							mColumnInfo[column].hasMultipleEntries = true;
						}
					}
				if (mColumnInfo[column].containsMultiLineText)
					break;
				}
			}

		if (setupDoubleValues(column, firstRow))
			mColumnInfo[column].type = cColumnTypeDouble;
		else if (setupDateValues(column, firstRow))
			mColumnInfo[column].type = cColumnTypeDouble + cColumnTypeDate;
		else
			mColumnInfo[column].type = cColumnTypeString; // default column type

		int categoryCountBeforeChange = (mColumnInfo[column].categoryList == null) ?
				0 : mColumnInfo[column].categoryList.getSize();

		mColumnInfo[column].categoryList = setupCategoryList(column);

		// check for uniqueness
		if (!isAfterDeletion || !mColumnInfo[column].isUnique) {
			mColumnInfo[column].isUnique = false;
			if (isColumnDisplayable(column)) {
				if (!mColumnInfo[column].hasMultipleEntries) {
					if (mColumnInfo[column].categoryList != null) {
						mColumnInfo[column].isUnique = (mColumnInfo[column].categoryList.getSize() == mRecord.length
													&& !mColumnInfo[column].categoryList.containsString(""));
						}
					else {
						TreeSet<byte[]> uniqueSet = new TreeSet<byte[]>(new ByteArrayComparator());
						for (int row=0; row<mRecord.length; row++) {
							byte[] data = (byte[])mRecord[row].getData(column);
							if (data == null || !uniqueSet.add(data))
								break;
							}
						mColumnInfo[column].isUnique = (uniqueSet.size() == mRecord.length);
						}
					}
				}
			}
		
		if (mColumnInfo[column].categoryList != null) {

			// check for range categories (before Jun 2014 binBase,binSize,binIsLog were not defined as properties)
			if ((mColumnInfo[column].type & cColumnTypeDouble) == 0) {

				boolean isNewRangeCategories = (mColumnInfo[column].getProperty(cColumnPropertyBinBase) != null);
				boolean binIsLog = "true".equals(mColumnInfo[column].getProperty(cColumnPropertyBinIsLog));

				boolean rangeCategories = true;
				boolean emptyCategoryExists = false;
				BigDecimal min = new BigDecimal(Float.MAX_VALUE);
				BigDecimal max = new BigDecimal(Float.MIN_VALUE);
				BigDecimal dif = null;
				double lowCenter = Double.MAX_VALUE;
				double highCenter = Double.MIN_VALUE;
				for (int i=0; i<mColumnInfo[column].categoryList.getSize(); i++) {
					String category = mColumnInfo[column].categoryList.getString(i);
					if (category.equals(cRangeNotAvailable)) {
						emptyCategoryExists = true;
						continue;
						}
					int index = category.indexOf(cRangeSeparation);
						if (index == -1) {
						rangeCategories = false;
						break;
						}

					try {
						BigDecimal low = new BigDecimal(category.substring(0, index));
						BigDecimal high = new BigDecimal(category.substring(index+cRangeSeparation.length()));
						min = min.min(low);
						max = max.max(low);
						if (!binIsLog) {	// check that all bin sizes are equal
							if (dif == null)
								dif = high.subtract(low);
							else if (!dif.equals(high.subtract(low))) {
								rangeCategories = false;
								break;
								}
							}

						// find lowest and highest bin center
						lowCenter = Math.min(lowCenter, low.doubleValue()+high.doubleValue());
						highCenter = Math.max(highCenter, low.doubleValue()+high.doubleValue());
						}
					catch (NumberFormatException e) {
						rangeCategories = false;
						break;
						}
					}
	
				if (rangeCategories) {
					if (mColumnInfo[column].categoryList.getSize() > 1)
						mColumnInfo[column].type = cColumnTypeRangeCategory;
	
					RangeCategoryList rangeCategoryList = new RangeCategoryList();

					lowCenter /= 2;
					highCenter /= 2;

					if (isNewRangeCategories) {	// for logarithmic categories we need to use the BinGenerator
						BigDecimal binBase = new BigDecimal(mColumnInfo[column].getProperty(cColumnPropertyBinBase));
						BigDecimal binSize = new BigDecimal(mColumnInfo[column].getProperty(cColumnPropertyBinSize));
						BinGenerator bg = new BinGenerator(binBase, binSize, binIsLog, lowCenter, highCenter);
						for (int i=0; i<bg.getBinCount(); i++)
							rangeCategoryList.add(bg.getRangeString(i));
						}
					else {	// old way of producing categories (equally sized bins)
						while (min.compareTo(max) <= 0) {
							String firstPart = min.toString() + CompoundTableModel.cRangeSeparation;
							min = min.add(dif);
							rangeCategoryList.add(firstPart + min.toString());
							}
						}
					if (emptyCategoryExists)
						rangeCategoryList.add(cRangeNotAvailable);
	
					mColumnInfo[column].categoryList = rangeCategoryList;
					}
				}

			// don't consider category columns if the number of categories is beyond maximum
			if ((mColumnInfo[column].type & cColumnTypeRangeCategory) == 0) {
				if (mColumnInfo[column].categoryList.getSize() > 1)
					mColumnInfo[column].type |= cColumnTypeCategory;
				else
					mColumnInfo[column].categoryList = null;
				}

			if (mColumnInfo[column].categoryList != null
			 && (mColumnInfo[column].categoryList.getSize() != categoryCountBeforeChange
			  || (firstRow == 0 && !isAfterDeletion)))	// neither append nor deletion
				assignRecordsToCategories(column);
			}
		}

	private CategoryList<?> setupCategoryList(int column) {

		CategoryList<?> categoryList = null;
		mColumnInfo[column].belongsToMultipleCategories = false;

		if ((mColumnInfo[column].type & cColumnTypeDate) != 0) {
			categoryList = new SortedCategoryList<Float>(new DateCategoryNormalizer());
			}
		else if ((mColumnInfo[column].type & cColumnTypeDouble) != 0) {
			if (mColumnInfo[column].hasModifiers)
				return null;

			categoryList = new SortedCategoryList<Float>(new DoubleCategoryNormalizer(mColumnInfo[column].logarithmicViewMode));
			}
		else if (mColumnInfo[column].mCategoryCustomOrder != null) {
			categoryList = new DefinedCategoryList<String>(mColumnInfo[column].mCategoryCustomOrder, new PlainCategoryNormalizer());
			}
		else {
			categoryList = new SortedCategoryList<String>(new PlainCategoryNormalizer());
			}

		for (int row=0; row<mRecord.length; row++) {
			String[] entry = separateEntries(encodeData(mRecord[row], column));
			for (int i=0; i<entry.length; i++) {
				categoryList.addString(entry[i]);
				if (i != 0 && !entry[i].equals(entry[0]))
					mColumnInfo[column].belongsToMultipleCategories = true;
				}

			if ((mColumnInfo[column].type & cColumnTypeDouble) != 0) {
				if (categoryList.getSize() >= cMaxDateOrDoubleCategoryCount) {
					categoryList = null;
					mColumnInfo[column].belongsToMultipleCategories = false;
					break;
					}
				}
			else {
				if (categoryList.getSize() >= cMaxTextCategoryCount) {
					categoryList = null;
					mColumnInfo[column].belongsToMultipleCategories = false;
					break;
					}
				}
			}

		return categoryList;
		}

	/**
	 * for range categories and non-date/float non-range categories
	 * (min, max and values remain untouched in float- or date-categories)
	 * @param column
	 */
	private void assignRecordsToCategories(int column) {
		if ((mColumnInfo[column].type & (cColumnTypeCategory | cColumnTypeDouble | cColumnTypeDate)) == cColumnTypeCategory) {
			int categoryCount = mColumnInfo[column].categoryList.getSize();
			mColumnInfo[column].minValue = 0.0f;
			mColumnInfo[column].maxValue = mColumnInfo[column].belongsToMultipleCategories ?
											categoryCount+1 : categoryCount;

			for (int row=0; row<mRecord.length; row++)
				mRecord[row].mFloat[column] = 0.5f + calcCategoryIndex(column, mRecord[row]);
			}
		}

	/**
	 * If all entries of the defined cell belong to the same category,
	 * then this category's index is returned. Otherwise, the number of
	 * categories is returned as indication for 'multiple categories'.
	 * In non-date/non-double category columns the returned value is
	 * identical to record.mDouble[column].
	 * @param column
	 * @param record
	 * @return
	 */
	private int calcCategoryIndex(int column, CompoundRecord record) {
		String[] entry = separateEntries(encodeData(record, column));
		CategoryList<?> categoryList = mColumnInfo[column].categoryList;
		int index = categoryList.getIndexOfString(entry[0]);
		for (int i=1; i<entry.length; i++) {
			if (index != categoryList.getIndexOfString(entry[i])) {
				index = categoryList.getSize();
				break;
				}
			}
		return index;
		}

	/**
	 * In columns of type category the record's mDouble[column] is the
	 * zero based index into the category list. However, if the column is at
	 * the same time of type date or float, then the record.mDouble[column]
	 * represents the original value instead. In this case the category index
	 * can be calculated with this method on the fly.
	 * If the record belongs to multiple categories, then the size of the
	 * category list is returned.
	 * @param column is the total column index
	 * @param record
	 * @return category index from all kinds of category columns
	 */
	public int getCategoryIndex(int column, CompoundRecord record) {
		if ((mColumnInfo[column].type & (cColumnTypeDouble | cColumnTypeDate)) == 0)
			return (int)record.mFloat[column];

		return calcCategoryIndex(column, record);
		}

	/**
	 * Provided that column is any kind of category column, this method
	 * returns the list index of value in the category list.
	 * This method does not check for membership in multiple categories.
	 * @param column valid category column
	 * @param value single category item
	 * @return
	 */
	public int getCategoryIndex(int column, String value) {
		return mColumnInfo[column].categoryList.getIndexOfString(value);
		}

	/**
	 * Calculates float values for every record of the specified column.
	 * Considers logarithmic view mode and mean/median settings for the
	 * mean/median calculation, if multiple values are found.
	 * @param column
	 * @param firstRow != 0 if rows were appended
	 * @return true if at least one cell is numerical and the remaining cells are empty
	 */
	private boolean setupDoubleValues(int column, int firstRow) {
		// if the existing data is not float conform, type cannot be float
		if (firstRow != 0 && (mColumnInfo[column].type & cColumnTypeDouble) == 0)
			return false;

		if (firstRow == 0)
			mColumnInfo[column].hasModifiers = false;

		boolean found = false;
		for (int row=mRecord.length-1; row>=firstRow || (!found && row>=0); row--) {
			try {
				mRecord[row].mFloat[column] = tryParseDouble(encodeData(mRecord[row], column), column);
				if (!Float.isNaN(mRecord[row].mFloat[column]))
					found = true;
				}
			catch (NumberFormatException e) {
				return false;
				}
			}

		if (found)
			findMinAndMaxFromDateOrDouble(column, firstRow, false);

		return found;
		}

	/**
	 * Tries to parse a text string to return a float value. The text may contain
	 * NaN values and/or modifiers. It must not contain multiple values.
	 * If the column is set to logarithmic view mode then the logarithmic value
	 * is returned.
	 * @param valueString
	 * @param column to check for viewMode settings
	 * @return float value or NaN
	 * @throws NumberFormatException
	 */
	public float tryParseEntry(String entry, int column) throws NumberFormatException {
		boolean logarithmic = mColumnInfo[column].logarithmicViewMode;
		
		EntryAnalysis analysis = new EntryAnalysis(entry);
		if (analysis.isEmpty() || analysis.isNaN())
			return Float.NaN;

		float v = Float.parseFloat(analysis.getValue());
		if (logarithmic) {
			if (v <= 0)
				return Float.NaN;

			v = (float)Math.log10(v);
			}

		return v;
		}

	/**
	 * Tries to parse a text string to return a float value. The text may contain
	 * multiple values, NaN values and/or modifiers. If it contains multiple values
	 * then depending on the column's settings mean or median are calculated and if
	 * the column is set to logarithmic view mode then this calculation is performed
	 * on the logarithmic values.
	 * If no exception is thrown, mModifier contains the modifier, which
	 * is even meaningful, when multiple values were merged.
	 * @param valueString
	 * @param column to check for mean/median/viewMode settings
	 * @return float value or NaN
	 * @throws NumberFormatException
	 */
	private float tryParseDouble(String valueString, int column) throws NumberFormatException {
		boolean logarithmic = mColumnInfo[column].logarithmicViewMode;

		String[] entry = separateEntries(valueString);
		float valueWithModifierSum = 0.0f;
		float valueWithoutModifierSum = 0.0f;
		float[] valueWithModifier = null;
		float[] valueWithoutModifier = null;
		if (mColumnInfo[column].summaryMode == cSummaryModeMedian) {
			valueWithModifier = new float[entry.length];
			valueWithoutModifier = new float[entry.length];
			}
		int valueWithModifierCount = 0;
		int valueWithoutModifierCount = 0;
		int summaryModifierType = EntryAnalysis.cModifierTypeNone;
		mParseDoubleModifier = "";
		mParseDoubleValueCount = 0;
		for (int i=0; i<entry.length; i++) {
			EntryAnalysis analysis = new EntryAnalysis(entry[i]);
			if (!analysis.isEmpty() && !analysis.isNaN()) {
				boolean hasModifier = (analysis.getModifierType() != EntryAnalysis.cModifierTypeNone);
				if (hasModifier) {
					mColumnInfo[column].hasModifiers = true;
					if (!mColumnInfo[column].excludeModifierValues) {
						if (summaryModifierType == EntryAnalysis.cModifierTypeNone)
							summaryModifierType = analysis.getModifierType();
						else if (summaryModifierType != analysis.getModifierType()) {
							mParseDoubleModifier = "";
							return Float.NaN;  // don't support different modifier types
							}

						if (mParseDoubleModifier.length() == 0)
							mParseDoubleModifier = analysis.getModifier();
						else if (!mParseDoubleModifier.equals(analysis.getModifier()))
							mParseDoubleModifier = EntryAnalysis.getDefaultModifier(analysis.getModifierType());

						entry[i] = analysis.getValue();
						}
					}

				if (!hasModifier
				 || !mColumnInfo[column].excludeModifierValues) {
					float v = Float.parseFloat(entry[i]);
					if (logarithmic) {
						if (v <= 0) {
							mParseDoubleModifier = "";
							return Float.NaN;
							}
						v = (float)Math.log10(v);
						}
	
					switch (mColumnInfo[column].summaryMode) {
					case cSummaryModeMinimum:
						if (valueWithModifierCount == 0)
							valueWithModifierSum = v;
						else
							valueWithModifierSum = Math.min(valueWithModifierSum, v);
						valueWithModifierCount++;
						break;
					case cSummaryModeMaximum:
						if (valueWithModifierCount == 0)
							valueWithModifierSum = v;
						else
							valueWithModifierSum = Math.max(valueWithModifierSum, v);
						valueWithModifierCount++;
						break;
					case cSummaryModeMedian:
						if (hasModifier) {
							valueWithModifier[valueWithModifierCount] = v;
							valueWithModifierCount++;
							}
						else {
							valueWithoutModifier[valueWithoutModifierCount] = v;
							valueWithoutModifierCount++;
							}
						break;
					default:
						if (hasModifier) {
							valueWithModifierSum += v;
							valueWithModifierCount++;
							}
						else {
							valueWithoutModifierSum += v;
							valueWithoutModifierCount++;
							}
						break;
						}
					}
				}
			}

		if (valueWithModifierCount + valueWithoutModifierCount == 0)
			return Float.NaN;

		if (mColumnInfo[column].summaryMode == cSummaryModeMinimum
		 || mColumnInfo[column].summaryMode == cSummaryModeMaximum) {
			mParseDoubleValueCount = valueWithModifierCount;
			return valueWithModifierSum;
			}

		if (mColumnInfo[column].summaryMode == cSummaryModeSum) {
			mParseDoubleValueCount = valueWithModifierCount + valueWithoutModifierCount;
			return valueWithModifierSum + valueWithoutModifierSum;
			}

		if (mColumnInfo[column].summaryMode == cSummaryModeMedian) {
			if (valueWithoutModifierCount != 0) {
				Arrays.sort(valueWithoutModifier, 0, valueWithoutModifierCount);
				valueWithoutModifierSum = ((valueWithoutModifierCount & 1) != 0) ?
					 valueWithoutModifier[valueWithoutModifierCount/2]
				   : (valueWithoutModifier[valueWithoutModifierCount/2-1]
					 +valueWithoutModifier[valueWithoutModifierCount/2])/2;

				// calculate a new median by taking modifier values into account
				// that conflict with the median based on all non-modifier values.
				int combinedValueCount = valueWithoutModifierCount;
				for (int i=0; i<valueWithModifierCount; i++)
					if ((summaryModifierType == EntryAnalysis.cModifierTypeSmaller
					  && valueWithoutModifierSum >= valueWithModifier[i])
					 || (summaryModifierType == EntryAnalysis.cModifierTypeLarger
					  && valueWithoutModifierSum <= valueWithModifier[i]))
						valueWithoutModifier[combinedValueCount++] = valueWithModifier[i];

				mParseDoubleValueCount = combinedValueCount;
				if (combinedValueCount == valueWithoutModifierCount) {
					mParseDoubleModifier = "";
					return valueWithoutModifierSum;
					}

				Arrays.sort(valueWithoutModifier, 0, combinedValueCount);
				return ((combinedValueCount & 1) != 0) ?
					 valueWithoutModifier[combinedValueCount/2]
				   : (valueWithoutModifier[combinedValueCount/2-1]
					 +valueWithoutModifier[combinedValueCount/2])/2;
				}
			else {
				Arrays.sort(valueWithModifier, 0, valueWithModifierCount);
				valueWithModifierSum = ((valueWithModifierCount & 1) != 0) ?
					 valueWithModifier[valueWithModifierCount/2]
				   : (valueWithModifier[valueWithModifierCount/2-1]
					 +valueWithModifier[valueWithModifierCount/2])/2;
				mParseDoubleValueCount = valueWithModifierCount;
				return valueWithModifierSum;
				}
			}

		// it remains cSummaryModeMean
		if (valueWithModifierCount > 1)
			valueWithModifierSum /= (float)valueWithModifierCount;
		if (valueWithoutModifierCount > 1)
			valueWithoutModifierSum /= (float)valueWithoutModifierCount;

		if (valueWithModifierCount == 0) {
			mParseDoubleValueCount = valueWithoutModifierCount;
			return valueWithoutModifierSum;
			}
		if (valueWithoutModifierCount == 0) {
			mParseDoubleValueCount = valueWithModifierCount;
			return valueWithModifierSum;
			}

		// We have summary values from values with modifiers and from values
		// without modifiers. Thus, we need to check for compatibility:
		if ((summaryModifierType == EntryAnalysis.cModifierTypeSmaller
		  && valueWithoutModifierSum < valueWithModifierSum)
		 || (summaryModifierType == EntryAnalysis.cModifierTypeLarger
		  && valueWithoutModifierSum > valueWithModifierSum)) {
			mParseDoubleValueCount = valueWithoutModifierCount;
			mParseDoubleModifier = "";
			return valueWithoutModifierSum;
			}

		mParseDoubleValueCount = valueWithModifierCount + valueWithoutModifierCount;
		return (valueWithoutModifierSum * valueWithoutModifierCount
				 + valueWithModifierSum * valueWithModifierCount)
			 / mParseDoubleValueCount;
		}

	/**
	 * @param column
	 * @param firstRow
	 * @return true if at least one cell is a date and the remaining cells are empty
	 */
	private boolean setupDateValues(int column, int firstRow) {
		// if the existing data is not date conform, type cannot be date
		if (firstRow != 0 && mColumnInfo[column].type != cColumnTypeDate)
			return false;

		boolean found = false;
		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US);
		for (int row=mRecord.length-1; row>=firstRow || (!found && row>=0); row--) {
			String[] entry = separateEntries(encodeData(mRecord[row], column));
			long timeMean = 0;
			int count = 0;
			for (int i=0; i<entry.length; i++) {
				if (entry[i].length() > 0) {
					try {
						Date date = df.parse(entry[i]);
						timeMean += date.getTime();
						count++;
						}
					catch (ParseException e) {
						return false;
						}
					}
				}

			if (count == 0) {
				mRecord[row].mFloat[column] = Float.NaN;
				}
			else {
				found = true;
				mRecord[row].mFloat[column] = (float)((timeMean/count+43200000)/86400000);
				}
			}

		if (found)
			findMinAndMaxFromDateOrDouble(column, firstRow, true);

		return found;
		}

	private void updateVisibleRecords(boolean isAdjusting, int exclusionFlagNo) {
		int oldVisibleRecords = mNonExcludedRecords;
		compileVisibleRecords();

		int mode;
		if (oldVisibleRecords < mNonExcludedRecords)
			mode = TableModelEvent.INSERT;
		else if (oldVisibleRecords > mNonExcludedRecords)
			mode = TableModelEvent.DELETE;
		else
			mode = TableModelEvent.UPDATE;

		fireEvents(new CompoundTableEvent(this,
								CompoundTableEvent.cChangeExcluded, exclusionFlagNo,
								isAdjusting),
				   new TableModelEvent(this, 0, mNonExcludedRecords-1,
								TableModelEvent.ALL_COLUMNS, mode));
		}

	private void compileVisibleRecords() {
		mNonExcludedRecords = 0;
		for (int row=0; row<mRecords; row++) {
			if ((mRecord[row].mFlags & mAllocatedExclusionFlags) == 0) {
				mNonExcludedRecord[mNonExcludedRecords++] = mRecord[row];
				}
			}
		}

	/**
	 * Calculates mColumnInfo[column].dataMin and dataMax from the
	 * mDouble values of all rows. Then mColumnInfo[column].minValue
	 * and maxValue are set from those if column is neither cyclix nor
	 * has an explicitly defined a value range. Otherwise minValue
	 * and maxValue are defined accordingly.
	 * If column is in logarithmic mode then all values are logarithmic ones.
	 * @param column
	 * @param firstRow != 0 if rows were appended
	 * @param isDate true if the column actually contains a date
	 */
	private void findMinAndMaxFromDateOrDouble(int column, int firstRow, boolean isDate) {
		if (firstRow == 0) {
			mColumnInfo[column].dataMin = Float.POSITIVE_INFINITY;
			mColumnInfo[column].dataMax = Float.NEGATIVE_INFINITY;
			mColumnInfo[column].isComplete = true;
			}

		for (int row=firstRow; row<mRecord.length; row++) {
			float value = mRecord[row].mFloat[column];
			if (Float.isNaN(value) || Float.isInfinite(value)) {
				mColumnInfo[column].isComplete = false;
				continue;
				}

			if (mColumnInfo[column].dataMin > value)
				mColumnInfo[column].dataMin = value;
			if (mColumnInfo[column].dataMax < value)
				mColumnInfo[column].dataMax = value;
			}

		if (isDate)
			updateMinAndMaxFromDate(column);
		else
			updateMinAndMaxFromDouble(column);
		}

	/**
	 * mColumnInfo[column].minValue and maxValue are set as follows:
	 * - If the column is a cyclic one, then min and max range from 0.0
	 *   to the explicitly defined cyclic maximum.
	 * - If the column has an explicitly defined data range, then min
	 *   and max are set to this.
	 * - Otherwise min and max are set to mColumnInfo[column].dataMin
	 *   and dataMax which are considered to be up-to-date.
	 * If column is in logarithmic mode then all values are logarithmic ones.
	 * @param column
	 * @param isDate true if the column actually contains a date
	 */
	private void updateMinAndMaxFromDouble(int column) {
		mColumnInfo[column].minValue = mColumnInfo[column].dataMin;
		mColumnInfo[column].maxValue = mColumnInfo[column].dataMax;
		if (getColumnProperty(column, cColumnPropertyCyclicDataMax) != null) {
			if (mColumnInfo[column].minValue > 0.0)
				mColumnInfo[column].minValue = 0.0f;
			float max = Float.parseFloat(getColumnProperty(column, cColumnPropertyCyclicDataMax));
			if (mColumnInfo[column].maxValue < max)
				mColumnInfo[column].maxValue = max;
			}
		else {
			if (getColumnProperty(column, cColumnPropertyDataMin) != null) {
				float min = Float.parseFloat(getColumnProperty(column, cColumnPropertyDataMin));
				if (!mColumnInfo[column].logarithmicViewMode || min > 0.0f) {
					if (mColumnInfo[column].logarithmicViewMode)
						min = (float)Math.log10(min);
					if (mColumnInfo[column].minValue > min)
						mColumnInfo[column].minValue = min;
					}
				}
				
			if (getColumnProperty(column, cColumnPropertyDataMax) != null) {
				float max = Float.parseFloat(getColumnProperty(column, cColumnPropertyDataMax));
				if (!mColumnInfo[column].logarithmicViewMode || max > 0.0f) {
					if (mColumnInfo[column].logarithmicViewMode)
						max = (float)Math.log10(max);
					if (mColumnInfo[column].maxValue < max)
						mColumnInfo[column].maxValue = max;
					}
				}
			}
		}

	/**
	 * 
	 * @param column
	 */
	private void updateMinAndMaxFromDate(int column) {
		mColumnInfo[column].minValue = mColumnInfo[column].dataMin - 0.5f;
		mColumnInfo[column].maxValue = mColumnInfo[column].dataMax + 0.5f;
		}

	private void validateColumnNames(String[] columnName) {
		for (int i=0; i<columnName.length; i++)
			columnName[i] = validateColumnName(columnName[i], columnName, i, this);
		}

	/**
	 * Checks column name for uniqueness and modifies it if needed to be unique.
	 * Considered are column names and aliases of existing columns (if refModel != null)
	 * and optionally a provided list of external names.
	 * @param name desired column name
	 * @param externalName null or external column names to avoid
	 * @param externalNameCount no of external names to consider
	 * @param refModel if not null then name redundancy is checked also against the refModel's columns
	 * @return
	 */
	public static String validateColumnName(String name, String[] externalName, int externalNameCount, CompoundTableModel refModel) {
		if (name == null || name.trim().length() == 0) {
			name = "Column 1";
			}
		else {
			name = name.trim().replaceAll("[\\x00-\\x1F]", "_");
			}

		while (columnNameExists(name, externalName, externalNameCount, refModel)) {
			int index = name.lastIndexOf(' ');
			if (index == -1)
				name = name + " 2";
			else {
				try {
					int suffix = Integer.parseInt(name.substring(index+1));
					name = name.substring(0, index+1) + (suffix+1);
					}
				catch (NumberFormatException nfe) {
					name = name + " 2";
					}
				}
			}

		return name;
		}

	private static boolean columnNameExists(String name, String[] externalName, int externalNameCount, CompoundTableModel refModel) {
		if (refModel != null && refModel.mColumnInfo != null)
			for (int i=0; i<refModel.mColumnInfo.length; i++)
				if (name.equalsIgnoreCase(refModel.mColumnInfo[i].name)
				 || name.equalsIgnoreCase(refModel.mColumnInfo[i].alias))
					return true;

		if (externalName != null)
			for (int i=0; i<externalNameCount; i++)
				if (externalName[i].equalsIgnoreCase(name))
					return true;

		return false;
		}

	public String[] separateEntries(String data) {
		if (data == null || data.length() == 0) {
			String[] entry = { "" };
			return entry;
			}

		return data.split(cSeparatorRegex, -1);
		}

	private String[] getEntrySeparators(String data, String[] entry) {
		String[] separator = new String[entry.length-1];
		int index2 = 0;
		for (int i=0; i<separator.length; i++) {
			int index1 = index2 + entry[i].length();
			index2 = data.indexOf(entry[i+1], index1);
			separator[i] = data.substring(index1, index2);
			}
		return separator;
		}

	/**
	 * @param column
	 * @return cDefaultDetailSeparator if not redefined for this column with column property cColumnPropertyDetailSeparator
	 */
	public String getDetailSeparator(int column) {
		String separator = getColumnProperty(column, cColumnPropertyDetailSeparator);
		return (separator == null) ? cDefaultDetailSeparator : separator;
		}

	private void fireEvents(CompoundTableEvent cte, TableModelEvent tme) {
		if (SwingUtilities.isEventDispatchThread()) {
			try {
				if (cte != null)
					fireCompoundTableChanged(cte);
				if (tme != null)
					fireTableChanged(tme);
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			}
		else {
				// if we are not in the event dispatcher thread we need to use invokeAndWait
			final CompoundTableEvent _cte = cte;
			final TableModelEvent _tme = tme;
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						try {
							if (_cte != null)
								fireCompoundTableChanged(_cte);
							if (_tme != null)
								fireTableChanged(_tme);
							}
						catch (Exception e) {
							e.printStackTrace();
							}
						}
					} );
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			}
		}

	/**
	 * 
	 */
	public void updateDescriptors() {
		if (mSMPProcessWaiting)
			return;

		mSMPProcessWaiting = true;
		mSMPStopDescriptorCalculation = true;

		new Thread("Descriptor Calculation Supervisor") {
			public void run() {
				while (mSMPWorkingThreads != null)
					try { Thread.sleep(100); } catch (InterruptedException e) {}

				mSMPProcessWaiting = false;
				mSMPStopDescriptorCalculation = false;

				int descriptorColumnCount = 0;

				synchronized(CompoundTableModel.this) {
					for (int column=0; column<mColumnInfo.length; column++)
						if (isDescriptorColumn(column)
						 && mColumnInfo[column].isDescriptorIncomplete)
							descriptorColumnCount++;
	
					if (descriptorColumnCount != 0) {
						mSMPColumnSpec = new DescriptorColumnSpec[descriptorColumnCount];
						descriptorColumnCount = 0;
						for (int column=0; column<mColumnInfo.length; column++)
							if (isDescriptorColumn(column)
							 && mColumnInfo[column].isDescriptorIncomplete)
								mSMPColumnSpec[descriptorColumnCount++] = new DescriptorColumnSpec(column);
						}
					}

				if (descriptorColumnCount != 0) {
					int threadCount = Runtime.getRuntime().availableProcessors();
					mSMPWorkingThreads = new AtomicInteger(threadCount);
					mSMPRecordIndex = new AtomicInteger(mRecords);
					mSMPRecord = mRecord;

					for (int i=0; i<mProgressListener.size(); i++)
						mProgressListener.get(i).startProgress("Calculating descriptors...", 0, mSMPRecord.length-1);

					for (int i=0; i<threadCount; i++) {
						Thread t = new Thread("Descriptor Calculator "+(i+1)) {
							public void run() {
								StereoMolecule molecule = new StereoMolecule();	// this is the default
								int recordIndex = mSMPRecordIndex.decrementAndGet();
								while (recordIndex >= 0 && !mSMPStopDescriptorCalculation) {
									for (DescriptorColumnSpec spec:mSMPColumnSpec)
										updateDescriptor(mSMPRecord[recordIndex], spec, molecule);

									for (ProgressListener pl:mProgressListener)
										pl.updateProgress(-1);

									recordIndex = mSMPRecordIndex.decrementAndGet();
									}

								if (mSMPWorkingThreads.decrementAndGet() == 0) {
									synchronized(CompoundTableModel.this) {
										if (!mSMPStopDescriptorCalculation) {
											String errorMsg = null;
											for (DescriptorColumnSpec spec:mSMPColumnSpec) {
												if (spec.isOutdated)
													setColumnProperty(spec.descriptorColumn, cColumnPropertyDescriptorVersion, spec.descriptorHandler.getVersion());

												mColumnInfo[spec.descriptorColumn].isDescriptorIncomplete = false;
												mColumnInfo[spec.descriptorColumn].isComplete = spec.isComplete;
												mColumnInfo[spec.descriptorColumn].isCompleteChild = spec.isCompleteChild;

												if (spec.errorCount != 0) {
													errorMsg = ((errorMsg == null) ? "" : errorMsg + "/n")
															 + "Descriptor '"+ getColumnSpecialType(spec.descriptorColumn)+"' calculation failed in "+spec.errorCount+" cases.";
													}
												}
											if (errorMsg != null)
												for (ProgressListener pl:mProgressListener)
													pl.showErrorMessage(errorMsg);
											}

										if (!mSMPProcessWaiting)
											for (int i=0; i<mProgressListener.size(); i++)
												((ProgressListener)mProgressListener.get(i)).stopProgress();

										if (!mSMPStopDescriptorCalculation)
											for (DescriptorColumnSpec spec:mSMPColumnSpec)
												if (spec.updateCount != 0)
													fireEvents(new CompoundTableEvent(this, CompoundTableEvent.cChangeColumnData, spec.descriptorColumn), null);

										mSMPColumnSpec = null;
										mSMPRecord = null;
										mSMPWorkingThreads = null;	// last thing to do as indication that all threads are done
										}
									}
								}
							};
						t.setPriority(Thread.MIN_PRIORITY);
						t.start();
						}
					}
				}
			}.start();
		}

	private void updateDescriptor(CompoundRecord record, DescriptorColumnSpec spec, StereoMolecule molecule) {
		Object descriptor = null;
		boolean existingDescriptorIsValid = false;
		byte[] chemData = null;
		try {
			byte[] coords = null;
			boolean needsCoords = false;
			synchronized(CompoundTableModel.this) {
				if (!mSMPStopDescriptorCalculation) {
					if (!spec.isOutdated && record.getData(spec.descriptorColumn) != null) {
						existingDescriptorIsValid = true;
						}
					else {
						chemData = (byte[])record.getData(spec.parentColumn);
						if (!spec.isReaction) {
							needsCoords = mColumnInfo[spec.descriptorColumn].descriptorHandler.getInfo().needsCoordinates;
							int coordsColumn = (needsCoords) ? getChildColumn(spec.parentColumn, cColumnType2DCoordinates) : -1;
							coords = (coordsColumn == -1) ? null : (byte[])record.getData(coordsColumn);
							}
						}
					}
				}

			if (chemData != null && !existingDescriptorIsValid) {
				Object chemObject;
				if (spec.isReaction) {
					chemObject = ReactionEncoder.decode(new String(chemData), false);
					}
				else {
					if (molecule == null) {
						chemObject = new IDCodeParser(needsCoords).getCompactMolecule(chemData, coords);
						}
					else {
						new IDCodeParser(needsCoords).parse(molecule, chemData, coords);
						chemObject = molecule;
						}
					}
	
				descriptor = mColumnInfo[spec.descriptorColumn].descriptorHandler.createDescriptor(chemObject);
				}
			}
		catch (Throwable t) {
			t.printStackTrace();
			spec.errorCount++;
			descriptor = null;
			}

		synchronized(CompoundTableModel.this) {
			if (!mSMPStopDescriptorCalculation && !existingDescriptorIsValid) {
				record.setData(descriptor, spec.descriptorColumn);
				spec.updateCount++;
		
				if (descriptor == null) {
					spec.isComplete = false;
					if (chemData != null)
						spec.isCompleteChild = false;
					}
				}
			}
		}

	private synchronized void stopDescriptorCalculation() {
		mSMPStopDescriptorCalculation = true;
		}

	private class DescriptorColumnSpec {
		int descriptorColumn;
		int parentColumn;
		int updateCount;
		int errorCount;
		DescriptorHandler descriptorHandler;
		boolean isOutdated,isComplete,isCompleteChild,isReaction;

		public DescriptorColumnSpec(int descriptorColumn) {
			this.descriptorColumn = descriptorColumn;
			parentColumn = findColumn(getColumnProperty(descriptorColumn, cColumnPropertyParentColumn));
			descriptorHandler = mColumnInfo[descriptorColumn].descriptorHandler;
			isOutdated = !descriptorHandler.getVersion().equals(getColumnProperty(descriptorColumn, cColumnPropertyDescriptorVersion));
			isReaction = cColumnTypeRXNCode.equals(getColumnSpecialType(parentColumn));
			isComplete = true;
			isCompleteChild = true;
			updateCount = 0;
			errorCount = 0;
			}
		}
	}

class CompoundTableColumnInfo {
	protected volatile boolean	isDescriptorIncomplete;
	protected int				type,summaryMode,significantDigits,structureHiliteMode;
	protected boolean			isComplete,isCompleteChild,isUnique,
								containsMultiLineText,hasDetail,hasMultipleEntries,
								belongsToMultipleCategories,logarithmicViewMode,
								hasModifiers,excludeModifierValues,summaryCountHidden;
	protected float				minValue,maxValue,dataMin,dataMax;
	protected CategoryList<?>	categoryList;
	protected UniqueList<String> mCategoryCustomOrder;
	protected String			name;
	protected String			alias;		  // is treated as runtime property
	protected String			description;	// is treated as runtime property
	private HashMap<String,String> properties;		// belong to table data
	protected DescriptorHandler descriptorHandler;
	private TreeMap<String,DescriptorCacheEntry> descriptorCache;

	protected CompoundTableColumnInfo(String name) {
		this.name = name;
		this.properties = new HashMap<String,String>();
		}

	protected final HashMap<String,String> getProperties() {
		return properties;
		}

	protected String getProperty(String key) {
		return (properties == null) ? null : properties.get(key);
		}

	protected Object getCachedDescriptor(Object chemObject) {
		if (descriptorHandler == null)
			return null;

		if (!(descriptorHandler instanceof DescriptorHandlerFlexophore))
			return descriptorHandler.createDescriptor(chemObject);

		String key = new Canonizer((StereoMolecule)chemObject).getIDCode();
		if (descriptorCache == null)
			descriptorCache = new TreeMap<String,DescriptorCacheEntry>();
		DescriptorCacheEntry entry = descriptorCache.get(key);
		if (entry == null) {
			entry = new DescriptorCacheEntry(descriptorHandler.createDescriptor(chemObject));
			descriptorCache.put(key, entry);
			if (descriptorCache.size() > 100) {
				Map.Entry<String,DescriptorCacheEntry> oldestEntry = null;
				for (Map.Entry<String,DescriptorCacheEntry> de:descriptorCache.entrySet())
					if (oldestEntry == null
					 || oldestEntry.getValue().instantiation > de.getValue().instantiation)
						oldestEntry = de;
				descriptorCache.remove(key);
				}
			}
		return entry.descriptor;
		}

	protected void setProperty(String key, String value) {
		if (value == null)
			properties.remove(key);
		else
			properties.put(key, value);

		// update or instantiate descriptorHandler
		if (key.equals(CompoundTableModel.cColumnPropertySpecialType)) {
			if (value != null && DescriptorHelper.isDescriptorShortName(value)) {
				descriptorHandler = (value.equals(DescriptorConstants.DESCRIPTOR_ReactionIndex.shortName)) ?
						DescriptorHandlerReactionIndex.getDefaultInstance()
					  : CompoundTableModel.getDefaultDescriptorHandler(value);

				if (descriptorHandler == null)
					properties.remove(key);
				}
			else {
				descriptorHandler = null;
				}
			}
		}

	/**
	 *  update or instantiate descriptorHandler
	 */
	protected void addCurrentDescriptorVersion(String shortName) {
		descriptorHandler = (shortName.equals(DescriptorConstants.DESCRIPTOR_ReactionIndex.shortName)) ?
				DescriptorHandlerReactionIndex.getDefaultInstance()
			  : CompoundTableModel.getDefaultDescriptorHandler(shortName);
		properties.put(CompoundTableModel.cColumnPropertyDescriptorVersion, descriptorHandler.getVersion());
		}
	}

class DescriptorCacheEntry {
	long instantiation;
	Object descriptor;

	public DescriptorCacheEntry(Object descriptor) {
		this.instantiation = System.currentTimeMillis();
		this.descriptor = descriptor;
		}
	}

class DoubleComparator implements Comparator<CompoundRecord> {
	private int mColumn;
	private boolean mInverse,mSelectedFirst;

	public DoubleComparator(int column, boolean inverse, boolean selectedFirst) {
		mColumn = column;
		mInverse = inverse;
		mSelectedFirst = selectedFirst;
		}

	public int compare(CompoundRecord o1, CompoundRecord o2) {
		if (mSelectedFirst && (o1.isSelected() != o2.isSelected()))
			return o1.isSelected() ? -1 : 1;
		float d1 = o1.mFloat[mColumn];
		float d2 = o2.mFloat[mColumn];
		if (Float.isNaN(d1))
			return (Float.isNaN(d2)) ? 0 : 1;
		if (Float.isNaN(d2))
			return -1;
		int comparison = (d1 < d2) ? -1 : (d1 == d2) ? 0 : 1;
		return (mInverse) ? -comparison : comparison;
		}
	}

class StringComparator implements Comparator<CompoundRecord> {
	private int mColumn;
	private boolean mInverse,mSelectedFirst;

	public StringComparator(int column, boolean inverse, boolean selectedFirst) {
		mColumn = column;
		mInverse = inverse;
		mSelectedFirst = selectedFirst;
		}

	public int compare(CompoundRecord o1, CompoundRecord o2) {
		if (mSelectedFirst && (o1.isSelected() != o2.isSelected()))
			return o1.isSelected() ? -1 : 1;
		byte[] s1 = (byte[])o1.getData(mColumn);
		byte[] s2 = (byte[])o2.getData(mColumn);
		if (s1 == null)
			return (s2 == null) ? 0 : 1;
		if (s2 == null)
			return -1;
		int comparison = compare(s1, s2);
		return (mInverse) ? -comparison : comparison;
		}

	private int compare(byte[] s1, byte[] s2) {
		for (int i=0; i<s1.length; i++) {
			if (s2.length == i)
				return 1;
			if (s1[i] != s2[i])
				return (s1[i] < s2[i]) ? -1 : 1;
			}
		return (s2.length > s1.length) ? -1 : 0;
		}
	}

class IDCodeComparator implements Comparator<CompoundRecord> {
	private int mColumn;
	private boolean mInverse,mSelectedFirst;
	private IDCodeParser mParser;

	public IDCodeComparator(int column, boolean inverse, boolean selectedFirst) {
		mColumn = column;
		mInverse = inverse;
		mSelectedFirst = selectedFirst;
		mParser = new IDCodeParser(false);
		}

	public int compare(CompoundRecord o1, CompoundRecord o2) {
		byte[] s1 = (byte[])o1.getData(mColumn);
		byte[] s2 = (byte[])o2.getData(mColumn);
		if (mSelectedFirst && (o1.isSelected() != o2.isSelected()))
			return o1.isSelected() ? -1 : 1;
		if (s1 == null)
			return (s2 == null) ? 0 : 1;
		if (s2 == null)
			return -1;
		int a1 = getAtomCount(s1);
		int a2 = getAtomCount(s2);
		int comparison = (a1 < a2) ? -1 : (a1 == a2) ? compare(s1, s2) : 1;
		return (mInverse) ? -comparison : comparison;
		}

	private int getAtomCount(byte[] idcode) {
		int atomCount = 0;
		int index = 0;
		while (index < idcode.length) {
			atomCount += mParser.getAtomCount(idcode, index);
			while (index<idcode.length && idcode[index] != '\n')
				index++;
			index++;
			}

			// put valid idcodes with 0 atoms at the end of the list
		return (atomCount == 0) ? Integer.MAX_VALUE : atomCount;
		}

	private int compare(byte[] s1, byte[] s2) {
		for (int i=0; i<s1.length; i++) {
			if (s2.length == i)
				return 1;
			if (s1[i] != s2[i])
				return (s1[i] < s2[i]) ? -1 : 1;
			}
		return (s2.length > s1.length) ? -1 : 0;
		}
	}

class EntryAnalysis {
	// The first modifier of any type is the default one if multiple modifiers
	// of the same type are merged, e.g. for mean generation.
	private static final String[][] cModifier = {{"<=", "<"}, {">=", ">", "H"}};

	protected static final int cModifierTypeNone = -1;
	protected static final int cModifierTypeSmaller = 0;
	protected static final int cModifierTypeLarger = 1;

	private boolean mIsEmpty,mIsNaN;
	private int mModifierType;
	private String mModifier;
	private String mValue;

	protected static String getDefaultModifier(int modifierType) {
		return cModifier[modifierType][0];
		}

	protected EntryAnalysis(String entry) {
		mIsEmpty = (entry == null || entry.length() == 0);
		if (!mIsEmpty) {
			mIsNaN = (entry.equals(CompoundTableConstants.cUnknownNumericalValue)
				   || entry.equals("<LOD")
				   || entry.equals(">LOD")
				   || entry.equals("NaN")
				   || entry.equals("#N/A")
				   || entry.equals("N/A")
				   || entry.equals("NA"));
			if (!mIsNaN)
				analyzeModifier(entry);
			}
		}

	protected boolean isEmpty() {
		return mIsEmpty;
		}

	protected boolean isNaN() {
		return mIsNaN;
		}

	protected String getModifier() {
		return mModifier;
		}

	protected int getModifierType() {
		return mModifierType;
		}

	protected boolean hasModifier() {
		return (mModifierType != cModifierTypeNone);
		}

	protected String getValue() {
		return (mValue == null) ? "" : mValue;
		}

	private void analyzeModifier(String entry) {
		for (int type=0; type<cModifier.length; type++) {
			for (int j=0; j<cModifier[type].length; j++) {
				if (entry.startsWith(cModifier[type][j])) {
					mModifierType = type;
					mModifier = cModifier[type][j];
					mValue = entry.substring(mModifier.length());
					return;
					}
				}
			}
		mModifierType = cModifierTypeNone;
		mModifier = "";
		mValue = entry;
		}
	}
