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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerFFP512;

public class ODEFileParser extends CompoundFileParser implements CompoundTableConstants,DescriptorConstants {
	private BufferedReader	mReader;
	private String[]		mFieldName;
	private String[]		mFieldData;
	private String          mIDCodeColumnName,mCoordinateColumnName,mFragFpColumnName;
	private int[]			mFieldIndex;
	private int				mIDCodeColumn, mCoordinateColumn,
							mIndexColumn,mOutdatedIndexColumn;

	public ODEFileParser(String fileName) {
		try {
			mReader = new BufferedReader(new FileReader(fileName));
		    }
		catch (FileNotFoundException e) {}
		init();
		}

	public ODEFileParser(File file) {
		try {
    		mReader = new BufferedReader(new FileReader(file));
		    }
		catch (FileNotFoundException e) {}
		init();
		}

	public ODEFileParser(Reader reader) {
		mReader = new BufferedReader(reader);
		init();
		}

	private void init() {
		mIDCodeColumn = -1;
		mCoordinateColumn = -1;
		mIndexColumn = -1;
		mOutdatedIndexColumn = -1;

		ArrayList<String> columnNameList = new ArrayList<String>();
        ArrayList<Integer> columnIndexList = new ArrayList<Integer>();

		String header = null;
		if (mReader != null)
			try {
				header = mReader.readLine();

				// skip file header in case of '.dwar' file
				if (header != null && header.equals(cNativeFileHeaderStart)) {
	                header = mReader.readLine();
	                while (header != null
	                    && !header.equals(cNativeFileHeaderEnd))
	                    header = mReader.readLine();
                    header = mReader.readLine();
                    }

	             // interpret column properties in case of '.dwar' file
                if (header != null && header.equals(cColumnPropertyStart)) {
                    TreeMap<String,Properties> columnProperties = new TreeMap<String,Properties>();

                    header = mReader.readLine();
                    Properties properties = null;
                    String columnName = null;
                    while (header != null
                        && !header.equals(cColumnPropertyEnd)) {

                        if (header.startsWith("<"+cColumnName)) {
                            columnName = extractValue(header);
                            properties = new Properties();
                            }
                        else if (header.startsWith("<"+cColumnProperty)) {
                            String keyAndValue = extractValue(header);
                            if (keyAndValue.equals("isIDCode\ttrue")
                             || keyAndValue.equals(cColumnPropertySpecialType
                                                   +"\t"+cColumnTypeIDCode)) {
                                mIDCodeColumnName = columnName;
                                }
                            else {
                                int index = keyAndValue.indexOf('\t');
                                properties.put(keyAndValue.substring(0, index), keyAndValue.substring(index+1));
                                }
                            }

                        header = mReader.readLine();
                        if (header.startsWith("<"+cColumnName)
                         || header.equals(cColumnPropertyEnd)) {
                            columnProperties.put(columnName, properties);
                            }
                        }

                    if (mIDCodeColumnName != null) {
                        for (String key:columnProperties.keySet()) {
                            Properties props = columnProperties.get(key);
                            if (cColumnType2DCoordinates.equals(props.get(cColumnPropertySpecialType))
                             && mIDCodeColumnName.equals(props.get(cColumnPropertyParentColumn)))
                                mCoordinateColumnName = key;
                            else if (DESCRIPTOR_FFP512.shortName.equals(props.get(cColumnPropertySpecialType))
                                  && DescriptorHandlerFFP512.VERSION.equals(props.get(cColumnPropertyDescriptorVersion))
                                  && mIDCodeColumnName.equals(props.get(cColumnPropertyParentColumn)))
                                mFragFpColumnName = key;
                            }
                        }

                    header = mReader.readLine();
                    }
			    }
			catch (IOException e) {}

		if (header == null) {
			mReader = null;
			return;
			}

		int fromIndex = 0;
		int toIndex = 0;
		int sourceColumn = 0;
		do {
			String columnName;
			toIndex = header.indexOf('\t', fromIndex);
			if (toIndex == -1) {
				columnName = header.substring(fromIndex);
				}
			else {
				columnName = header.substring(fromIndex, toIndex);
				fromIndex = toIndex+1;
				}

			if (mIDCodeColumn == -1 && columnName.equals(mIDCodeColumnName))
			    mIDCodeColumn = sourceColumn;
			else if (mCoordinateColumn == -1 && columnName.equals(mCoordinateColumnName))
			    mCoordinateColumn = sourceColumn;
            else if (mIndexColumn == -1 && columnName.equals(mFragFpColumnName))
                mIndexColumn = sourceColumn;
            else if (columnName.equalsIgnoreCase("idcode") && mIDCodeColumn == -1)
				mIDCodeColumn = sourceColumn;
			else if (columnName.equalsIgnoreCase("idcoordinates") && mCoordinateColumn == -1)
				mCoordinateColumn = sourceColumn;
			else if (columnName.startsWith("fingerprint") && mIndexColumn == -1) {
				if (columnName.endsWith(SSSearcherWithIndex.cIndexVersion))
					mIndexColumn = sourceColumn;
				else
					mOutdatedIndexColumn = sourceColumn;
				}

			if (sourceColumn != mIDCodeColumn
			 && sourceColumn != mCoordinateColumn
			 && sourceColumn != mIndexColumn
			 && sourceColumn != mOutdatedIndexColumn) {
				columnNameList.add(columnName);
				columnIndexList.add(new Integer(sourceColumn));
				}

			sourceColumn++;
			} while (toIndex != -1);

		mFieldName = new String[columnNameList.size()];
		mFieldIndex = new int[columnNameList.size()];
		for (int i=0; i<columnNameList.size(); i++) {
			mFieldName[i] = (String)columnNameList.get(i);
			mFieldIndex[i] = ((Integer)columnIndexList.get(i)).intValue();
			}

		mFieldData = new String[sourceColumn];
		}

	public String[] getFieldNames() {
		return mFieldName;
		}

    protected boolean advanceToNext() {
        return moreRecordsAvailable();
        }

    public String getMoleculeName() {
        return null;
        }

    public int getRowCount() {
        return -1;
        }

	public boolean moreRecordsAvailable() {
		if (mReader == null)
			return false;

		String line = null;
		try {
			line = mReader.readLine();
			if (line == null
			 || line.equals(cPropertiesStart)
			 || line.equals(cColumnPropertyStart)
			 ||	line.equals(cHitlistDataStart)
			 || line.equals(cDetailDataStart)) {
			    mReader.close();
				return false;
				}
			}
		catch (IOException e) {
			return false;
			}

		int column = 0;
		int index1 = 0;
		int index2 = line.indexOf('\t');
		while (index2 != -1) {
			mFieldData[column] = line.substring(index1, index2);
			column++;
			index1 = index2+1;
			index2 = line.indexOf('\t', index1);
			}
		mFieldData[column] = line.substring(index1);

		return true;
		}

	public String getIDCode() {
        if (mIDCodeColumn == -1)
            return null;
		String s = mFieldData[mIDCodeColumn];
		return (s == null || s.length() == 0) ? null : s;
		}

	public String getCoordinates() {
        if (mCoordinateColumn == -1)
            return null;
		String s = mFieldData[mCoordinateColumn];
		return (s == null || s.length() == 0) ? null : s;
		}

	public String getIndex() {
        if (mIndexColumn == -1)
            return null;
		String s = mFieldData[mIndexColumn];
		return (s == null || s.length() == 0) ? null : s;
		}

	public String getFieldData(int no) {
		return mFieldData[mFieldIndex[no]];
		}

    static public String extractValue(String theLine) {
        int index1 = theLine.indexOf("=\"") + 2;
        int index2 = theLine.indexOf("\"", index1);
        return theLine.substring(index1, index2);
        }
    }
