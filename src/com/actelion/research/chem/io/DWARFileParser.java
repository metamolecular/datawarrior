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

package com.actelion.research.chem.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeMap;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerFFP512;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.util.BinaryDecoder;

public class DWARFileParser extends CompoundFileParser implements DescriptorConstants,CompoundTableConstants {

    public static final int MODE_COORDINATES_PREFER_2D = 1;
    public static final int MODE_COORDINATES_PREFER_3D = 2;
    public static final int MODE_COORDINATES_REQUIRE_2D = 3;
    public static final int MODE_COORDINATES_REQUIRE_3D = 4;
    private static final int MODE_COORDINATE_MASK = 7;
    public static final int MODE_BUFFER_HEAD_AND_TAIL = 8;
    public static final int MODE_EXTRACT_DETAILS = 16;

	private String[]		mFieldName;
	private String[]		mFieldData;
	private String			mLine;
	private int[]			mFieldIndex;
    private int             mRecordCount,mMode;
	private int				mIDCodeColumn, mCoordinateColumn,mMoleculeNameColumn,mIndexColumn;
    private TreeMap<String,Properties> mColumnPropertyMap;
	private TreeMap<String,SpecialField> mSpecialFieldMap;
	private TreeMap<String,Integer> mDescriptorColumnMap;
	private ArrayList<String> mHeadOrTailLineList;
	private HashMap<String,byte[]> mDetails;

    /**
     * Constructs a DWARFileParser from a file name with coordinate mode MODE_COORDINATES_PREFER_2D.
     * @param fileName
     * @throws IOException
     */
	public DWARFileParser(String fileName) {
        try {
            mReader = new BufferedReader(new FileReader(fileName));
            mMode = MODE_COORDINATES_PREFER_2D;
            init();
            }
        catch (IOException e) {
            mReader = null;
            }
		}

    /**
     * Constructs a DWARFileParser from a File with coordinate mode MODE_COORDINATES_PREFER_2D.
     * @param file
     * @throws IOException
     */
	public DWARFileParser(File file) {
        try {
            mReader = new BufferedReader(new FileReader(file));
            mMode = MODE_COORDINATES_PREFER_2D;
            init();
            }
        catch (IOException e) {
            mReader = null;
            }
		}

    /**
     * Constructs a DWARFileParser from a Reader with coordinate mode MODE_COORDINATES_PREFER_2D.
     * @param reader
     * @throws IOException
     */
	public DWARFileParser(Reader reader) throws IOException {
		mReader = new BufferedReader(reader);
        try {
            mMode = MODE_COORDINATES_PREFER_2D;
            init();
            }
        catch (IOException e) {
            mReader = null;
            }
		}

    /**
     * Constructs a DWARFileParser from a file name with the specified coordinate mode.
     * @param fileName
     * @param mode one of 4 MODE_COORDINATE... modes
     * @throws IOException
     */
    public DWARFileParser(String fileName, int mode) {
        try {
            mReader = new BufferedReader(new FileReader(fileName));
            mMode = mode;
            init();
            }
        catch (IOException e) {
            mReader = null;
            }
        }

    /**
     * Constructs a DWARFileParser from a File with the specified coordinate mode.
     * @param file
     * @param mode one of 4 MODE_COORDINATE... modes
     * @throws IOException
     */
    public DWARFileParser(File file, int mode) {
        try {
            mReader = new BufferedReader(new FileReader(file));
            mMode = mode;
            init();
            }
        catch (IOException e) {
            mReader = null;
            }
        }

    /**
     * Constructs a DWARFileParser from a Reader with the specified coordinate mode.
     * @param reader
     * @param mode one of 4 MODE_COORDINATE... modes
     * @throws IOException
     */
    public DWARFileParser(Reader reader, int mode) throws IOException {
        mReader = new BufferedReader(reader);
        try {
            mMode = mode;
            init();
            }
        catch (IOException e) {
            mReader = null;
            }
        }

    private String readHeadOrTailLine() throws IOException {
    	String line = mReader.readLine();
    	if ((mMode & MODE_BUFFER_HEAD_AND_TAIL) != 0 && line != null)
    		mHeadOrTailLineList.add(line);
    	return line;
    	}

    private void init() throws IOException {
    	if ((mMode & MODE_BUFFER_HEAD_AND_TAIL) != 0)
    		mHeadOrTailLineList = new ArrayList<String>();

    	int coordinateMode = mMode & MODE_COORDINATE_MASK;
	
		String line = readHeadOrTailLine();
        if (line == null
         || !line.equals(cNativeFileHeaderStart))
            throw new IOException("no header found");

        mRecordCount = -1;
        line = readHeadOrTailLine();
        while (line != null
            && !line.equals(cNativeFileHeaderEnd)) {

            if (line.startsWith("<"+cNativeFileVersion)) {
                String version = extractValue(line);
                if (!version.equals("3.0")
                 && !version.equals("3.1")
                 && !version.equals(""))
                    throw new IOException("unsupported .dwar file version");
                }

            else if (line.startsWith("<"+cNativeFileRowCount)) {
                try {
                    mRecordCount = Integer.parseInt(extractValue(line));
                    }
                catch (NumberFormatException e) {}
                }
  
            line = readHeadOrTailLine();
            }

        line = readHeadOrTailLine();

        if (line != null
         && line.equals(cFileExplanationStart)) {
            line = readHeadOrTailLine();
            while (line != null
                && !line.equals(cFileExplanationEnd))
                line = readHeadOrTailLine();
            line = readHeadOrTailLine();
        	}

        mColumnPropertyMap = new TreeMap<String,Properties>();

        if (line != null
         && line.equals(cColumnPropertyStart)) {
            line = readHeadOrTailLine();
            String columnName = null;
            while (line != null
                && !line.equals(cColumnPropertyEnd)) {

                if (line.startsWith("<"+cColumnName)) {
                	columnName = extractValue(line);
                	mColumnPropertyMap.put(columnName, new Properties());
                    }

                else if (line.startsWith("<"+cColumnProperty)) {
                    String[] property = extractValue(line).split("\\t");
                    mColumnPropertyMap.get(columnName).setProperty(property[0], property[1]);
                    }

                line = readHeadOrTailLine();
                }

            line = readHeadOrTailLine();
            }

        mSpecialFieldMap = new TreeMap<String,SpecialField>();	// only take those columns that have a special type
        for (String columnName:mColumnPropertyMap.keySet()) {
        	Properties properties = mColumnPropertyMap.get(columnName);
        	String specialType = properties.getProperty(cColumnPropertySpecialType);
        	if (specialType != null)
        		mSpecialFieldMap.put(columnName, new SpecialField(
        				columnName,
        				specialType,
        				properties.getProperty(cColumnPropertyParentColumn),
        				properties.getProperty(cColumnPropertyIdentifierColumn),
        				properties.getProperty(cColumnPropertyDescriptorVersion)
        				));
        	}
        
        ArrayList<String> columnNameList = new ArrayList<String>();
        ArrayList<Integer> columnIndexList = new ArrayList<Integer>();

		if (line == null)
            throw new IOException("unexpected end of file");

		int fromIndex = 0;
		int toIndex = 0;
		int sourceColumn = 0;
		do {
			String columnName;
			toIndex = line.indexOf('\t', fromIndex);
			if (toIndex == -1) {
				columnName = line.substring(fromIndex);
				}
			else {
				columnName = line.substring(fromIndex, toIndex);
				fromIndex = toIndex+1;
				}

            if (mSpecialFieldMap.containsKey(columnName)) {
                mSpecialFieldMap.get(columnName).fieldIndex = sourceColumn;
                }
            else {
    			columnNameList.add(columnName);
    			columnIndexList.add(new Integer(sourceColumn));
                }

			sourceColumn++;
			} while (toIndex != -1);

		mFieldName = new String[columnNameList.size()];
		mFieldIndex = new int[columnNameList.size()];
		for (int i=0; i<columnNameList.size(); i++) {
			mFieldName[i] = (String)columnNameList.get(i);
			mFieldIndex[i] = columnIndexList.get(i).intValue();
			}

		mFieldData = new String[sourceColumn];

        mIDCodeColumn = -1;
        mCoordinateColumn = -1;
        mMoleculeNameColumn = -1;
        mIndexColumn = -1;
        SpecialField idcodeColumn = mSpecialFieldMap.get("Structure");
        if (idcodeColumn == null
         || !idcodeColumn.type.equals(cColumnTypeIDCode)) {
            for (SpecialField specialColumn:mSpecialFieldMap.values()) {
                if (specialColumn.type.equals(cColumnTypeIDCode)) {
                    if (idcodeColumn == null
                     || idcodeColumn.fieldIndex > specialColumn.fieldIndex)
                        idcodeColumn = specialColumn;
                    }
                }
            }
        if (idcodeColumn != null) {
            if (idcodeColumn.idColumn != null) {
                for (int i=0; i<mFieldName.length; i++) {
                    if (idcodeColumn.idColumn.equals(mFieldName[i])) {
                        mMoleculeNameColumn = mFieldIndex[i];
                        break;
                        }
                    }
                }
            mIDCodeColumn = idcodeColumn.fieldIndex;
            for (SpecialField specialColumn:mSpecialFieldMap.values()) {
                if (idcodeColumn.name.equals(specialColumn.parent)) {
                    if (DESCRIPTOR_FFP512.shortName.equals(specialColumn.type)
                     && DescriptorHandlerFFP512.VERSION.equals(specialColumn.version))
                        mIndexColumn = specialColumn.fieldIndex;
                    else if (cColumnType2DCoordinates.equals(specialColumn.type)
                          && (coordinateMode == MODE_COORDINATES_REQUIRE_2D
                           || coordinateMode == MODE_COORDINATES_PREFER_2D
                           || (coordinateMode == MODE_COORDINATES_PREFER_3D && mCoordinateColumn == -1)))
                        mCoordinateColumn = specialColumn.fieldIndex;
                    else if (cColumnType3DCoordinates.equals(specialColumn.type)
                          && (coordinateMode == MODE_COORDINATES_REQUIRE_3D
                           || coordinateMode == MODE_COORDINATES_PREFER_3D
                           || (coordinateMode == MODE_COORDINATES_PREFER_2D && mCoordinateColumn == -1)))
                        mCoordinateColumn = specialColumn.fieldIndex;

                    if (DescriptorHelper.isDescriptorShortName(specialColumn.type)) {
                        if (mDescriptorColumnMap == null)
                            mDescriptorColumnMap = new TreeMap<String,Integer>();
                        mDescriptorColumnMap.put(specialColumn.type, new Integer(specialColumn.fieldIndex));
                        }
                    }
                }
            }
	    }

    /**
     * If you don't read any records after calling this method,
     * don't forget to call close() to close the underlying file.
     * @return whether the file contains chemical structures
     */
    public boolean hasStructures() {
    	return (mIDCodeColumn != -1);
    	}

    /**
     * @return whether the file contains chemical structures with explicit atom coordinates
     */
    public boolean hasStructureCoordinates() {
    	return (mCoordinateColumn != -1);
    	}

    public String[] getFieldNames() {
		return mFieldName;
		}

    public int getRowCount() {
        return mRecordCount;
        }

    /**
     * Provided that the mode contains MODE_BUFFER_HEAD_AND_TAIL, then this method
     * returns a list of all header/footer rows of the DWAR file. If this method is
     * called before all rows have been read, then the header lines including column
     * properties and the column title line are returned. If this method is
     * called before all rows have been read, then all lines after the data table, i.e. the
     * runtime properties, are returned.
     * @return
     */
    public ArrayList<String> getHeadOrTail() {
        return mHeadOrTailLineList;
        }

    /**
     * Provided that the mode contains MODE_EXTRACT_DETAILS, then this method
     * returns a map of all embedded detail objects of the DWAR file.
     * This method must not be called before all rows have been read.
     * @return
     */
    public HashMap<String,byte[]> getDetails() {
    	return mDetails;
    	}

    /**
     * Returns the entire line containing all row data
     * @return
     */
    public String getRow() {
        return mLine;
        }

    protected boolean advanceToNext() {
		if (mReader == null)
			return false;

		mLine = null;
		try {
			mLine = mReader.readLine();
			if (mLine == null
			 || mLine.equals(cPropertiesStart)
			 || mLine.equals(cHitlistDataStart)
			 || mLine.equals(cDetailDataStart)) {
				if ((mMode & MODE_BUFFER_HEAD_AND_TAIL) != 0) {
					mHeadOrTailLineList.clear();
					mHeadOrTailLineList.add(mLine);
					}
				while (mLine != null) {
					if (mLine.equals(cDetailDataStart)
					 && (mMode & MODE_EXTRACT_DETAILS) != 0)
						extractDetails();
					mLine = readHeadOrTailLine();
					}
			    mReader.close();
				return false;
				}
			}
		catch (IOException e) {
			return false;
			}

		int column = 0;
		int index1 = 0;
		int index2 = mLine.indexOf('\t');
		while (index2 != -1) {
			if (column<mFieldData.length)
				mFieldData[column] = mLine.substring(index1, index2).replace(NEWLINE_STRING, cLineSeparator);
			column++;
			index1 = index2+1;
			index2 = mLine.indexOf('\t', index1);
			}
		if (column<mFieldData.length)
			mFieldData[column] = mLine.substring(index1).replace(NEWLINE_STRING, cLineSeparator);

		return true;
		}

    private void extractDetails() {
		mDetails = new HashMap<String,byte[]>();
		try {
		    while (true) {
				String theLine = readHeadOrTailLine();
				if (theLine == null
				 || theLine.equals(cDetailDataEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cDetailID)) {
					String detailID = extractValue(theLine);
					BinaryDecoder decoder = new BinaryDecoder(mReader);
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

    /**
     * @return the row content of the first column containing chemical structures
     */
	public String getIDCode() {
        if (mIDCodeColumn == -1)
            return null;
		String s = mFieldData[mIDCodeColumn];
		return (s == null || s.length() == 0) ? null : s;
		}

	/**
	 * This returns encoded atom coordinates according to the defined mode.
	 * If the compound file does not contain atom coordinates, then null is returned.
	 * If mode is one of MODE_COORDINATES_REQUIRE... and the required coordinate dimensionality
	 * (2D or 3D) is not available then null is returned.
	 * If mode is one of MODE_COORDINATES_PREFER... and the preferred coordinate dimensionality
	 * (2D or 3D) is not available then coordinates in another dimensionality are returned.
	 */
	public String getCoordinates() {
        if (mCoordinateColumn == -1)
            return null;
		String s = mFieldData[mCoordinateColumn];
		return (s == null || s.length() == 0) ? null : s;
		}

    public String getMoleculeName() {
        if (mMoleculeNameColumn == -1)
            return null;
        String s = mFieldData[mMoleculeNameColumn];
        return (s == null || s.length() == 0) ? null : s;
        }

    public Object getDescriptor(String shortName) {
        Integer column = (mDescriptorColumnMap == null) ? null : mDescriptorColumnMap.get(shortName);
        String s = (column == null) ? null : mFieldData[column.intValue()];
        return (s == null || s.length() == 0) ? super.getDescriptor(shortName)
             : (getDescriptorHandlerFactory() == null) ? null
             : getDescriptorHandlerFactory().getDefaultDescriptorHandler(shortName).decode(s);
        }

    /**
     * @return the String encoded FragFp descriptor of the first column containing chemical structures
     */
	public String getIndex() {
        if (mIndexColumn == -1)
            return null;
		String s = mFieldData[mIndexColumn];
		return (s == null || s.length() == 0) ? null : s;
		}

	public String getFieldData(int no) {
		return mFieldData[mFieldIndex[no]];
		}

	/**
	 * Returns a columnName->SpecialField map of all non-alphanumerical columns.
	 * SpecialField.type is one of the types defined in CompoundTableConstants:
	 * cColumnTypeIDCode,cColumnTypeRXNCode,cColumnType2DCoordinates,cColumnType3DCoordinates,
	 * cColumnTypeAtomColorInfo, and descriptor shortNames;
	 * @return special fields
	 */
	public TreeMap<String,SpecialField> getSpecialFieldMap() {
	    return mSpecialFieldMap;
	    }

	/**
	 * @param fieldIndex is available from special-field-TreeMap by getSpecialFieldMap().get(columnName).fieldIndex
	 * @return String encoded data content of special field, e.g. idcode
	 */
    public String getSpecialFieldData(int fieldIndex) {
        return mFieldData[fieldIndex];
        }

    /**
     * Returns the original column properties of any source column by column name.
     * @param columnName
     * @return
     */
    public Properties getColumnProperties(String columnName) {
    	return mColumnPropertyMap.get(columnName);
    	}

    private String extractValue(String theLine) {
        int index1 = theLine.indexOf("=\"") + 2;
        int index2 = theLine.indexOf("\"", index1);
        return theLine.substring(index1, index2);
        }

	public class SpecialField {
	    public String name;
	    public String type;
	    public String parent;
	    public String idColumn;
	    public String version;
	    public int fieldIndex;

	    public SpecialField(String name, String type, String parent, String idColumn, String version) {
	        this.name = name;
	        this.type = type;
	        this.parent = parent;
	        this.idColumn = idColumn;
	        this.version = version;
	        this.fieldIndex = -1;
	        }
	    }
    }
