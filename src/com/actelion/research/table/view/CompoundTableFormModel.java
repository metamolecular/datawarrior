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

package com.actelion.research.table.view;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.gui.form.FormModel;
import com.actelion.research.gui.form.FormObjectFactory;
import com.actelion.research.gui.form.JImageDetailView;
import com.actelion.research.gui.form.JStructure3DFormObject;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableModel;

public class CompoundTableFormModel implements FormModel {
	protected static final String KEY_DETAIL_SEPARATOR = "#D#:";

	private CompoundTableModel	mTableModel;
    private CompoundRecord		mRecord;
    private boolean				mIsEditable;

    public CompoundTableFormModel(CompoundTableModel tableModel, CompoundRecord record) {
        mTableModel = tableModel;
        mRecord= record;
        mIsEditable = false;
    	}

    public void setCompoundRecord(CompoundRecord record) {
        mRecord= record;
    	}

    public CompoundRecord getCompoundRecord() {
        return mRecord;
    	}

    public void setEditable(boolean isEditable) {
    	mIsEditable = isEditable;
    	}

    public int getKeyCount() {
        int keyCount = 0;
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            String specialType = mTableModel.getColumnSpecialType(column);
            if (specialType == null)
                keyCount += 1+mTableModel.getColumnDetailCount(column);
            else if (specialType.equals(CompoundTableModel.cColumnTypeIDCode)
                  || specialType.equals(CompoundTableModel.cColumnTypeRXNCode)
                  || specialType.equals(CompoundTableModel.cColumnType3DCoordinates))
                keyCount++;
            }
        return keyCount;
    	}

	public String getKey(int no) {
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            String specialType = mTableModel.getColumnSpecialType(column);
            if (specialType != null
             && (specialType.equals(CompoundTableModel.cColumnTypeIDCode)
              || specialType.equals(CompoundTableModel.cColumnTypeRXNCode)
              || specialType.equals(CompoundTableModel.cColumnType3DCoordinates))) {
                if (no == 0)
                    return mTableModel.getColumnTitleNoAlias(column);
                no--;
                }
            }

        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            String specialType = mTableModel.getColumnSpecialType(column);
            if (specialType == null) {
                if (no == 0)
                    return mTableModel.getColumnTitleNoAlias(column);

                no--;
                int detailCount = mTableModel.getColumnDetailCount(column);
                if (no < detailCount)
                    return mTableModel.getColumnTitleNoAlias(column)+KEY_DETAIL_SEPARATOR+no;

                no -= detailCount;
                }
            }

		return null;
		}

	public String getTitle(String key) {
		int index = key.indexOf(KEY_DETAIL_SEPARATOR);
		if (index != -1) {
			int column = mTableModel.findColumn(key.substring(0, index));
			if (column == -1)
				return "<detail missing>";

			try {
				int detail = Integer.parseInt(key.substring(index+KEY_DETAIL_SEPARATOR.length()));
				return mTableModel.getColumnDetailName(column, detail);
				}
			catch (NumberFormatException nfe) {
				return null;
				}
			}

		int column = mTableModel.findColumn(key);
		if (column == -1)
            return "<column missing>";

        return mTableModel.getColumnTitle(column);
		}

	public Object getValue(String key) {
		if (mRecord == null)
			return null;

		int index = key.indexOf(KEY_DETAIL_SEPARATOR);
		if (index != -1) {
			int column = mTableModel.findColumn(key.substring(0, index));
			if (column == -1)
				return null;

			try {
				int detail = Integer.parseInt(key.substring(index+KEY_DETAIL_SEPARATOR.length()));
				String reference[][] = mRecord.getDetailReferences(column);
				if (reference != null && reference.length>detail)
					return reference[detail];
				}
			catch (NumberFormatException nfe) {
				return null;
				}
			}

        int column = mTableModel.findColumn(key);
        if (column == -1)
            return null;

        String specialType =  mTableModel.getColumnSpecialType(column);
        if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)
         || CompoundTableModel.cColumnType3DCoordinates.equals(specialType))
            return mTableModel.getChemicalStructure(mRecord, column, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, null);
        if (CompoundTableModel.cColumnTypeRXNCode.equals(specialType))
            return mTableModel.getChemicalReaction(mRecord, column);

        // If the form is editable, we need to return the original value to be edited instead of the decorated or summary value.
		return mIsEditable ? mTableModel.encodeData(mRecord, column) : mTableModel.getValue(mRecord, column);
		}

    public boolean setValue(String key, Object value) {
        if (mRecord == null)
            return false; // should never happen

        int index = key.indexOf(KEY_DETAIL_SEPARATOR);
        if (index != -1)
            return false; // Detail form objects may not be edited currently. Therefore this should never happen.

        int column = mTableModel.findColumn(key);
        if (column == -1)
            return false; // should never happen

        String specialType =  mTableModel.getColumnSpecialType(column);
        if (specialType == null) {
            if (mTableModel.isColumnTypeRangeCategory(column)
             && !mTableModel.getNativeCategoryList(column).containsString((String)value)) {
                // "For columns that contain range categories, you need to type an existing range."
                return false;
                }
            else {
                mRecord.setData(((String)value).getBytes(), column, true);
                mTableModel.finalizeChangeCell(mRecord, column);
                return true;
                }
            }
        else if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
            if (mTableModel.setChemicalStructure(mRecord, (StereoMolecule)value, column)) {
                mTableModel.finalizeChangeCell(mRecord, column);
                return true;
                }
            }
        else if (CompoundTableModel.cColumnTypeRXNCode.equals(specialType)) {
            if (mTableModel.setChemicalReaction(mRecord, (Reaction)value, column)) {
                mTableModel.finalizeChangeCell(mRecord, column);
                return true;
                }
            }
        return false;
        }

	public String getType(String key) {
		int index = key.indexOf(KEY_DETAIL_SEPARATOR);
		if (index != -1) {
			int column = mTableModel.findColumn(key.substring(0, index));
			if (column == -1)
				return FormObjectFactory.TYPE_SINGLE_LINE_TEXT;	// shouldn't ever happen

			try {
				int detail = Integer.parseInt(key.substring(index+KEY_DETAIL_SEPARATOR.length()));
				return mTableModel.getColumnDetailType(column, detail);
				}
			catch (NumberFormatException nfe) {
				return null;
				}
			}

		int column = mTableModel.findColumn(key);
		if (column == -1)
			return FormObjectFactory.TYPE_SINGLE_LINE_TEXT;	// shouldn't ever happen

        String specialType = mTableModel.getColumnSpecialType(column);
        if (specialType != null) {
            if (specialType.equals(CompoundTableModel.cColumnTypeIDCode))
                return FormObjectFactory.TYPE_STRUCTURE;
            else if (specialType.equals(CompoundTableModel.cColumnTypeRXNCode))
                return FormObjectFactory.TYPE_REACTION;
            else if (specialType.equals(CompoundTableModel.cColumnType3DCoordinates))
                return JStructure3DFormObject.FORM_OBJECT_TYPE;
            }
		if (mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath) != null)
			return JImageDetailView.TYPE_IMAGE_FROM_PATH;

		return mTableModel.isMultiLineColumn(column) ?
					FormObjectFactory.TYPE_MULTI_LINE_TEXT : FormObjectFactory.TYPE_SINGLE_LINE_TEXT;
		}
	}
