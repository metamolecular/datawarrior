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

import java.util.EventObject;

public class CompoundTableEvent extends EventObject {
    private static final long serialVersionUID = 0x20060831;

	public static final int cNewTable = 1;
	public static final int cChangeColumnData = 2;	// specifier is column index, change of column values, column type may also have changed
//	public static final int cChangeValueRange = 3;	// specifier is column index, change of value range while keeping column type
	public static final int cAddRows = 4;			// specifier is first new row
    public static final int cDeleteRows = 5;		// mapping is row mapping

	public static final int cAddColumns = 6;		// specifier is column index of first new column
	public static final int cRemoveColumns = 7;

	public static final int cChangeColumnName = 8;	// specifier is column index
	public static final int cRemoveColumnDetails = 9;	// specifier is column, mapping is detail mapping
	public static final int cChangeColumnDetailSource = 10;	// specifier is column, mapping[0] detail

	public static final int cChangeExcluded = 11;	// specifier is exclusionMask
	public static final int cChangeSelection = 12;	// This event is for ListSelectionModel only. Other components listen there
	public static final int cChangeVisibleInView = 13;	// 
	public static final int cChangeSortOrder = 14;
	public static final int cChangeActiveRow = 15;

	public static final int cChangeExtensionData = 21;	// the content data of one of the registered file extensions changed

	public static final int cSpecifierNoRuntimeProperties = 1;		// used if type = cNewTable
	public static final int cSpecifierDefaultRuntimeProperties = 2;	// used if type = cNewTable

	private int		mType,mSpecifier;
	private int[]	mMapping;    // maps new to original columns/rows after column/row removal
	private boolean	mIsAdjusting;

    public CompoundTableEvent(Object source, int type, int specifier) {
		super(source);
		mType = type;
		mSpecifier = specifier;
	    }

    public CompoundTableEvent(Object source, int type, int[] mapping) {
		super(source);
		mType = type;
		mMapping = mapping;
	    }

    public CompoundTableEvent(Object source, int type, int specifier, int[] mapping) {
		super(source);
		mType = type;
		mSpecifier = specifier;
		mMapping = mapping;
	    }

    public CompoundTableEvent(Object source, int type, int specifier, boolean isAdjusting) {
		super(source);
		mType = type;
		mSpecifier = specifier;
		mIsAdjusting = isAdjusting;
	    }

	public int getType() {
		return mType;
		}

	public int getSpecifier() {
		return mSpecifier;
		}

	public int[] getMapping() {
		return mMapping;
		}

	public boolean isAdjusting() {
		return mIsAdjusting;
		}
	}