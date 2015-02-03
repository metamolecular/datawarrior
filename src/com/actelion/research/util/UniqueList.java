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

package com.actelion.research.util;

import java.io.Serializable;
import java.util.ArrayList;

public class UniqueList<T extends Comparable<? super T>> extends SortedList<T> implements Serializable {
    static final long serialVersionUID = 0x20121016;

    private ArrayList<T> mOriginalOrder = new ArrayList<T>();
	private int[] mOriginalIndex;

    public int getSortedIndex(T s) {
        return super.getIndex(s);
        }

    @Override
	public boolean contains(T object) {
		return super.getIndex(object) != -1;	// uses the faster inherited getIndex() method
		}

    /**
     * When objects were added after the last getIndex() call,
     * then the original-index-map needs to be re-created.
     */
    @Override
	public int getIndex(T s) {
		int index = super.getIndex(s);
		if (index == -1)
			return -1;

		if (mOriginalIndex == null)
			createOriginalIndex();

		return mOriginalIndex[index];
		}

    @Override
	public int add(T s) {
		int index = super.add(s);
		if (index == -1)
			return -1;

		int position = mOriginalOrder.size();

		mOriginalOrder.add(s);
		mOriginalIndex = null;

		return position;
		}

	public int add(int position, T s) {
		int index = super.add(s);
		if (index == -1)
			return -1;

		mOriginalOrder.add(position, s);
		mOriginalIndex = null;

		return position;
		}

	/**
     * @param i list index within list in original order
     * @return string at position i of list in order of the creation
     */
    @Override
	public T get(int i) {
		return mOriginalOrder.get(i);
		}

	/**
	 * @param i list index within sorted list
	 * @return string at position i of sorted list
	 */
    public T getSorted(int i) {
        return super.get(i);
        }

    @Override
	public T[] toArray(T[] e) {
	    return mOriginalOrder.toArray(e);
	    }

    public T[] toSortedArray(T[] e) {
        return super.toArray(e);
        }

    private void createOriginalIndex() {
    	mOriginalIndex = new int[size()];
    	int i=0;
    	for (T t:mOriginalOrder)
    		mOriginalIndex[super.getIndex(t)] = i++;
    	}
	}
