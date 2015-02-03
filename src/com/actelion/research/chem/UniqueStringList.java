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

package com.actelion.research.chem;

import java.io.Serializable;
import java.util.ArrayList;

public class UniqueStringList extends SortedStringList implements Serializable {
    static final long serialVersionUID = 0x20060720;

    private ArrayList<String> mOriginalOrder = new ArrayList<String>();
	private ArrayList<Integer> mIndexList = new ArrayList<Integer>();

    public int getSortedListIndex(String s) {
        return super.getListIndex(s);
        }

	public int getListIndex(String s) {
		int index = super.getListIndex(s);
		if (index == -1)
			return -1;

		return mIndexList.get(index).intValue();
		}

	public int addString(String theString) {
		int index = super.addString(theString);
		if (index == -1)
			return -1;

		int position = mOriginalOrder.size();

		mOriginalOrder.add(theString);
		mIndexList.add(index, new Integer(position));

		return position;
		}

    /**
     * @param i list index within list in original order
     * @return string at position i of list in order of the creation
     */
	public String getStringAt(int i) {
		return mOriginalOrder.get(i);
		}

	/**
	 * @param i list index within sorted list
	 * @return string at position i of sorted list
	 */
    public String getSortedStringAt(int i) {
        return super.getStringAt(i);
        }

	public String[] toArray() {
	    return mOriginalOrder.toArray(new String[0]);
	    }

    public String[] toSortedArray() {
        return super.toArray();
        }
    }
