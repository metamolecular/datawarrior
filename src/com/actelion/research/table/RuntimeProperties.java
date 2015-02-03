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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeMap;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.util.BinaryDecoder;
import com.actelion.research.util.BinaryEncoder;

public class RuntimeProperties extends TreeMap<String,Object> implements CompoundTableConstants {
    private static final long serialVersionUID = 0x20061101;

    private static final String cLogarithmicViewMode = "logarithmicView";
    private static final String cSignificantDigits = "significantDigits";
    private static final String cSignificantDigitColumnCount = "significantDigitColumnCount";
    private static final String cModifierValuesExcluded = "modifierValuesExcluded";
    private static final String cCurrentRecord = "currentRecord";
    private static final String cColumnAlias = "columnAlias";
    private static final String cColumnAliasCount = "columnAliasCount";
    private static final String cColumnDescription = "columnDescription";
    private static final String cColumnDescriptionCount = "columnDescriptionCount";
    private static final String cBinaryObject = "isBinaryEncoded";
    private static final String cCustomOrderCount = "customOrderCount";
    private static final String cCustomOrder = "customOrder";
    private static final String cSummaryCountHidden = "summaryCountHidden";

	protected CompoundTableModel	mTableModel;
	protected BufferedWriter		mWriter;

	public RuntimeProperties(CompoundTableModel tableModel) {
        super();
		mTableModel = tableModel;
		}

	public void read(BufferedReader theReader) throws IOException {
		clear();
		while (true) {
			String theLine = theReader.readLine();
			if (theLine.equals(cPropertiesEnd))
				break;

			int index1 = theLine.indexOf('<');
			if (index1 == -1)
				continue;
			int index2 = theLine.indexOf('=', index1+1);
            while (index2 != -1 && theLine.charAt(index2+1) == '=')
                index2 = theLine.indexOf('=', index2+2);
			if (index2 == -1)
				continue;
			int index3 = theLine.indexOf('"', index2+1);
			if (index3 == -1)
				continue;
			int index4 = theLine.indexOf('"', index3+1);
            while (index4 != -1 && theLine.charAt(index4+1) == '"')
                index4 = theLine.indexOf('"', index4+2);
			if (index4 == -1)
				continue;

			String key = theLine.substring(index1+1, index2).replace("==", "=");
			String value = theLine.substring(index3+1, index4).replace("\"\"", "\"");

			if (value.equals(cBinaryObject)) {
                BinaryDecoder decoder = new BinaryDecoder(theReader);
                int size = decoder.initialize(8);
                byte[] detailData = new byte[size];
                for (int i=0; i<size; i++)
                    detailData[i] = (byte)decoder.read();
                theLine = theReader.readLine();
                while (theLine != null
                    && !theLine.equals("</"+key.replace("=", "==")+">"))
                    theLine = theReader.readLine();
                if (theLine != null)
                    put(key, detailData);
                }
			else {
			    put(key, value);
			    }
			}
		}

	protected String getProperty(String key) {
	    return (String)get(key);
	    }

    protected void setProperty(String key, String value) {
        put(key, value);
        }

    protected byte[] getBinary(String key) {
        return (byte[])get(key);
        }

    protected void setBinary(String key, byte[] value) {
        put(key, value);
        }

	public void apply() {
	    RuntimePropertyColumnList list = new RuntimePropertyColumnList(cLogarithmicViewMode);
	    for (int column=list.next(); column != -1; column=list.next())
	        mTableModel.setLogarithmicViewMode(column, true);

	    list = new RuntimePropertyColumnList(cModifierValuesExcluded);
	    for (int column=list.next(); column != -1; column=list.next())
	        mTableModel.setColumnModifierExclusion(column, true);

	    for (int summaryMode=1; summaryMode<cSummaryModeOption.length; summaryMode++) {
    	    list = new RuntimePropertyColumnList(cSummaryModeOption[summaryMode]);
    	    for (int column=list.next(); column != -1; column=list.next())
                mTableModel.setColumnSummaryMode(column, summaryMode);
	        }

	    list = new RuntimePropertyColumnList(cSummaryCountHidden);
	    for (int column=list.next(); column != -1; column=list.next())
            mTableModel.setColumnSummaryCountHidden(column, true);

	    for (int hiliteMode=1; hiliteMode<cHiliteModeOption.length; hiliteMode++) {
    	    list = new RuntimePropertyColumnList(cHiliteModeOption[hiliteMode]);
    	    for (int column=list.next(); column != -1; column=list.next())
                mTableModel.setStructureHiliteMode(column, hiliteMode);
	        }

		String currentRecord = getProperty(cCurrentRecord);
		if (currentRecord != null) {
		    try {	// this is hardly useful for a dataset that differs from the original
				int record = Integer.parseInt(currentRecord);
		        if (record < mTableModel.getRowCount())
		            mTableModel.setActiveRow(record);
		        }
		    catch (NumberFormatException e) {}
			}
		String columnCount = getProperty(cColumnAliasCount);
		if (columnCount != null) {
			try {
				int count = Integer.parseInt(columnCount);
				for (int i=0; i<count; i++) {
					String columnAlias = getProperty(cColumnAlias+"_"+i);
					if (columnAlias != null) {
						int index = columnAlias.indexOf('\t');
						if (index != -1) {
							int column = mTableModel.findColumn(columnAlias.substring(0, index));
							if (column != -1)
								mTableModel.setColumnAlias(columnAlias.substring(index+1), column);
							}
						}
					}
				} catch (NumberFormatException e) {}
			}
		columnCount = getProperty(cColumnDescriptionCount);
		if (columnCount != null) {
			try {
				int count = Integer.parseInt(columnCount);
				for (int i=0; i<count; i++) {
					String columnDescription = getProperty(cColumnDescription+"_"+i);
					if (columnDescription != null) {
						int index = columnDescription.indexOf('\t');
						if (index != -1) {
							int column = mTableModel.findColumn(columnDescription.substring(0, index));
							if (column != -1)
								mTableModel.setColumnDescription(columnDescription.substring(index+1).replace("<NL>", "\n"), column);
							}
						}
					}
				} catch (NumberFormatException e) {}
			}
		columnCount = getProperty(cSignificantDigitColumnCount);
        if (columnCount != null) {
            try {
                int count = Integer.parseInt(columnCount);
                for (int i=0; i<count; i++) {
                    String significantDigits = getProperty(cSignificantDigits+"_"+i);
                    if (significantDigits != null) {
                        int index = significantDigits.indexOf('\t');
                        if (index != -1) {
                            int column = mTableModel.findColumn(significantDigits.substring(0, index));
                            if (column != -1) {
                                try {
                                    int digits = Integer.parseInt(significantDigits.substring(index+1));
                                    mTableModel.setColumnSignificantDigits(column, digits);
                                    } catch (NumberFormatException e) {}
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {}
            }
        columnCount = getProperty(cCustomOrderCount);
        if (columnCount != null) {
            try {
                int count = Integer.parseInt(columnCount);
                for (int i=0; i<count; i++) {
                    String customOrder = getProperty(cCustomOrder+"_"+i);
                    if (customOrder != null) {
                        int index = customOrder.indexOf('\t');
                        if (index != -1) {
                            int column = mTableModel.findColumn(customOrder.substring(0, index));
                            if (column != -1) {
                                String[] customOrderItems = customOrder.substring(index+1).split("\\t");
                                mTableModel.setCategoryCustomOrder(column, customOrderItems);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {}
            }
		}

	public void write(BufferedWriter theWriter) throws IOException {
		learn();
		mWriter = theWriter;
		mWriter.write(cPropertiesStart);
		mWriter.newLine();
		Set<String> keys = keySet();
		for (String key:keys) {
			Object value = get(key);
			if (value instanceof String) {
    			mWriter.write("<"+key.replace("=", "==")+"=\""+((String)getProperty(key)).replace("\"", "\"\"")+"\">");
    			mWriter.newLine();
			    }
			else if (value instanceof byte[]) {
                mWriter.write("<"+key.replace("=", "==")+"=\""+cBinaryObject+"\">");
                mWriter.newLine();

                byte[] data = (byte[])value;
                BinaryEncoder encoder = new BinaryEncoder(theWriter);
                encoder.initialize(8, data.length);
                for (int i=0; i<data.length; i++)
                    encoder.write(data[i]);
                encoder.finalize();

                mWriter.write("</"+key.replace("=", "==")+">");
                mWriter.newLine();
			    }
			}
		mWriter.write(cPropertiesEnd);
		mWriter.newLine();
		}

	protected void learn() {
		clear();
		String columnList = "";
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.isLogarithmicViewMode(column)) {
				if (columnList.length() != 0)
				    columnList = columnList.concat("\t");
				columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
				}
		    }
        if (columnList.length() != 0)
            setProperty(cLogarithmicViewMode, columnList);

        columnList = "";
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            if (mTableModel.getColumnModifierExclusion(column)) {
                if (columnList.length() != 0)
                    columnList = columnList.concat("\t");
                columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                }
            }
        if (columnList.length() != 0)
            setProperty(cModifierValuesExcluded, columnList);

        for (int summaryMode=1; summaryMode<cSummaryModeOption.length; summaryMode++) {
            columnList = "";
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.getColumnSummaryMode(column) == summaryMode) {
                    if (columnList.length() != 0)
                        columnList = columnList.concat("\t");
                    columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                    }
                }
            if (columnList.length() != 0)
                setProperty(cSummaryModeOption[summaryMode], columnList);

            columnList = "";
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.isColumnSummaryCountHidden(column)) {
                    if (columnList.length() != 0)
                        columnList = columnList.concat("\t");
                    columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                    }
                }
            if (columnList.length() != 0)
                setProperty(cSummaryCountHidden, columnList);
        	}

        for (int hiliteMode=1; hiliteMode<cHiliteModeOption.length; hiliteMode++) {
            columnList = "";
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.getStructureHiliteMode(column) == hiliteMode) {
                    if (columnList.length() != 0)
                        columnList = columnList.concat("\t");
                    columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                    }
                }
            if (columnList.length() != 0)
                setProperty(cHiliteModeOption[hiliteMode], columnList);
            }

		CompoundRecord currentRecord = mTableModel.getActiveRow();
		if (currentRecord != null) {
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				if (mTableModel.getTotalRecord(row) == currentRecord) {
					setProperty(cCurrentRecord, ""+row);
					break;
					}
				}
			}

		int columnAliasCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnAlias(column) != null)
				columnAliasCount++;
		if (columnAliasCount != 0) {
			setProperty(cColumnAliasCount, ""+columnAliasCount);
			columnAliasCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getColumnAlias(column) != null)
					setProperty(cColumnAlias+"_"+columnAliasCount++,
							mTableModel.getColumnTitleNoAlias(column)+"\t"+mTableModel.getColumnAlias(column));
			}
		int columnDescriptionCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnDescription(column) != null)
				columnDescriptionCount++;
		if (columnDescriptionCount != 0) {
			setProperty(cColumnDescriptionCount, ""+columnDescriptionCount);
			columnDescriptionCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getColumnDescription(column) != null)
					setProperty(cColumnDescription+"_"+columnDescriptionCount++,
							mTableModel.getColumnTitleNoAlias(column)+"\t"+mTableModel.getColumnDescription(column).replace("\n", "<NL>"));
			}
        int significantDigitColumnCount = 0;
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
            if (mTableModel.getColumnSignificantDigits(column) != 0)
                significantDigitColumnCount++;
        if (significantDigitColumnCount != 0) {
            setProperty(cSignificantDigitColumnCount, ""+significantDigitColumnCount);
            significantDigitColumnCount = 0;
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
                if (mTableModel.getColumnSignificantDigits(column) != 0)
                    setProperty(cSignificantDigits+"_"+significantDigitColumnCount++,
                            mTableModel.getColumnTitleNoAlias(column)+"\t"+mTableModel.getColumnSignificantDigits(column));
            }
		int customOrderCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getCategoryCustomOrder(column) != null)
				customOrderCount++;
		if (customOrderCount != 0) {
			setProperty(cCustomOrderCount, ""+customOrderCount);
			customOrderCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				String[] customOrder = mTableModel.getCategoryCustomOrder(column);
				if (customOrder != null) {
					StringBuilder sb = new StringBuilder(customOrder[0]);
					for (int i=1; i<customOrder.length; i++)
						sb.append('\t').append(customOrder[i]);
					setProperty(cCustomOrder+"_"+customOrderCount++,
							mTableModel.getColumnTitleNoAlias(column)+"\t"+sb.toString());
					}
				}
			}
		}

	class RuntimePropertyColumnList {
	    private String columnList = null;

	    public RuntimePropertyColumnList(String key) {
	        columnList = getProperty(key);
	        }

	    public int next() {
	        while (columnList != null) {
                int index = columnList.indexOf('\t');
                String name = null;
                if (index != -1) {
                    name = columnList.substring(0, index);
                    columnList = columnList.substring(index+1);
                    }
                else {
                    name = columnList;
                    columnList = null;
                    }
                int column = mTableModel.findColumn(name);
                if (column != -1)
                    return column;
	            }
            return -1;
	        }
	    }
	}
