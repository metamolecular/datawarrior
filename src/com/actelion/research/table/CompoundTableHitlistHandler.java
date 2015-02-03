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

import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

public class CompoundTableHitlistHandler {
	public static final int		EMPTY_LIST = 0;
	public static final int		ALL_IN_LIST = 1;
	public static final int		FROM_VISIBLE = 2;
	public static final int		FROM_HIDDEN = 3;
	public static final int		FROM_SELECTED = 4;
	public static final int		FROM_KEY_SET = 5;

	public static final int		OPERATION_AND = 0;
	public static final int		OPERATION_OR = 1;
	public static final int		OPERATION_XOR = 2;
	public static final int		OPERATION_NOT = 3;

    public static final int     HITLISTINDEX_NONE = -1;
    public static final int     HITLISTINDEX_ANY = -2;

    public static final String  HITLISTNAME_NONE = "<none>";
    public static final String  HITLISTNAME_ANY = "<any>";

	private static final int 	PSEUDO_COLUMN_FIRST_HITLIST = -4;
		// a pseudo columnIndex = PSEUDO_COLUMN_FIRST_HITLIST - hitlistIndex;

	private CompoundTableModel     mTableModel;
	private ArrayList<HitlistInfo> mHitlistInfoList;
	private ArrayList<CompoundTableHitlistListener> mListener;

	/**
	 * Negative column indexes smaller than PSEUDO_COLUMN_FIRST_HITLIST encode and refer to
	 * hitlist indexes rather than compound table columns.
	 * @param column (pseudo) column index that refers to a column, a hitlist or nothing at all
	 * @return true if the column index refers to a hitlist
	 */
	public static boolean isHitlistColumn(int column) {
			// certain negative column numbers actually refer to a hitlist rather than a column
		return (column <= PSEUDO_COLUMN_FIRST_HITLIST);
		}

	 public static int getHitlistFromColumn(int column) {
		return (column == -1) ? HITLISTINDEX_NONE : PSEUDO_COLUMN_FIRST_HITLIST - column;
		}

	public static int getColumnFromHitlist(int hitlist) {
		return (hitlist == HITLISTINDEX_NONE) ? -1 : PSEUDO_COLUMN_FIRST_HITLIST - hitlist;
		}

	public CompoundTableHitlistHandler(CompoundTableModel tableModel) {
		mTableModel = tableModel;
		mHitlistInfoList = new ArrayList<HitlistInfo>();
		mListener = new ArrayList<CompoundTableHitlistListener>();
		}

	public void addCompoundTableHitlistListener(CompoundTableHitlistListener l) {
		mListener.add(l);
		}

	public void removeCompoundTableHitlistListener(CompoundTableHitlistListener l) {
		mListener.remove(l);
		}

	public int getHitlistCount() {
		return mHitlistInfoList.size();
		}

	public String getHitlistName(int i) {
		return mHitlistInfoList.get(i).name;
		}

	public int getHitlistIndex(String name) {
		for (int i=0; i<mHitlistInfoList.size(); i++)
			if (mHitlistInfoList.get(i).name.equals(name))
				return i;
		return -1;
		}

    public int getHitlistFlagNo(String name) {
        for (HitlistInfo info:mHitlistInfoList)
            if (info.name.equals(name))
                return info.flagNo;
        return 0;
        }

    /**
     * Returns the compound record flag that is associated with this hitlist.
     * @param index valid hitlist index (>= 0)
     * @return flagNo
     */
    public int getHitlistFlagNo(int index) {
        return mHitlistInfoList.get(index).flagNo;
        }

    /**
     * Creates a mask containing one all flags of those hitlists specified in index
     * @param index hitlist index or HITLISTINDEX_NONE or HITLISTINDEX_ANY
     * @return mask
     */
    public long getHitlistMask(int index) {
        if (index == HITLISTINDEX_NONE)
            return 0;
        if (index == HITLISTINDEX_ANY) {
            long mask = 0;
            for (int i=0; i<mHitlistInfoList.size(); i++)
                mask |= (1L << mHitlistInfoList.get(i).flagNo);
            return mask;
            }
		return (1L << mHitlistInfoList.get(index).flagNo);
		}

    /**
     * Create a new hitlist that is either empty or contains rows defined in source.
     * If source is FROM_KEY_SET, then keyColumn and keySet define which column contains
     * the keys that need to match those in keySet for a row being considered a list member.
     * One may specify a category column to enlarge the initial hitlist such that
     * if at least one record of a category belongs to the initial hitlist, then
     * all records of this category will added to the list.
     * @param name intended name for new hitlist
     * @param extentionColumn -1 or category column for hitlist completion
     * @param source EMPTY_LIST,FROM_VISIBLE,FROM_HIDDEN, FROM_SELECTED or FROM_KEY_SET
     * @param keyColumn the column in which to look for keys that make a row a hitlist member
     * @param keySet the set of keys, which if present makes a row a hitlist member
     * @return unique hitlist name which may differ from the intended 'name'
     */
	public String createHitlist(String name, int extentionColumn, int source, int keyColumn, TreeSet<String> keySet) {
		int flagNo = mTableModel.getUnusedCompoundFlag(false);
		if (flagNo == -1)
			return null;

		name = getUniqueName(name);

		mHitlistInfoList.add(new HitlistInfo(name, flagNo));

		if (source == ALL_IN_LIST) {
            for (int row=0; row<mTableModel.getTotalRowCount(); row++)
				mTableModel.getTotalRecord(row).setFlag(flagNo);
			}
		else if (source != EMPTY_LIST) {
            if (extentionColumn == -1) {
                for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
    				CompoundRecord record = mTableModel.getTotalRecord(row);
    				switch (source) {
    				case FROM_VISIBLE:
    					if (mTableModel.isVisible(record))
    						record.setFlag(flagNo);
    					break;
    				case FROM_HIDDEN:
                        if (!mTableModel.isVisible(record))
                            record.setFlag(flagNo);
    					break;
    				case FROM_SELECTED:
                        if (mTableModel.isVisibleAndSelected(record))
                            record.setFlag(flagNo);
    					break;
    				case FROM_KEY_SET:
                        String[] items = mTableModel.separateEntries(mTableModel.encodeData(record, keyColumn));
                        for (String item:items) {
                        	if (keySet.contains(item)) {
	                            record.setFlag(flagNo);
	                            break;
                        		}
                        	}
    					break;
    					}
    				}
                }
            else {
                TreeSet<String> categorySet = new TreeSet<String>();
                for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                    CompoundRecord record = mTableModel.getTotalRecord(row);
                    String[] entry = null; 
                    switch (source) {
                    case FROM_VISIBLE:
                        if (mTableModel.isVisible(record))
                            entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                        break;
                    case FROM_HIDDEN:
                        if (mTableModel.isVisible(record))
                            entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                        break;
                    case FROM_SELECTED:
                        if (mTableModel.isVisibleAndSelected(record))
                            entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                        break;
    				case FROM_KEY_SET:
                        String[] items = mTableModel.separateEntries(mTableModel.encodeData(record, keyColumn));
                        for (String item:items) {
                        	if (keySet.contains(item)) {
                                entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
	                            break;
                        		}
                        	}
    					break;
                        }
                    if (entry != null)
                        for (int i=0; i<entry.length; i++)
                            categorySet.add(entry[i]);
                    }
                for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                    CompoundRecord record = mTableModel.getTotalRecord(row);
                    String[] entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                    for (int i=0; i<entry.length; i++)
                        if (categorySet.contains(entry[i]))
                            record.setFlag(flagNo);
                    }
                }
			}

        mTableModel.setCompoundFlagDirty(flagNo);

        fireEvents(new CompoundTableHitlistEvent(this, CompoundTableHitlistEvent.cAdd, mHitlistInfoList.size()-1));
		return name;
		}

    /**
     * Creates a new hitlist based on a boolean operation on existing hitlists.
     * @param name intended name for new hitlist
     * @param hitlist1
     * @param hitlist2
     * @param operation OPERATION_AND,OPERATION_OR,OPERATION_XOR or OPERATION_NOT
     * @return unique hitlist name which may differ from the intended 'name'
     */
	public String createHitlist(String name, int hitlist1, int hitlist2, int operation) {
		int flagNo = mTableModel.getUnusedCompoundFlag(false);
		if (flagNo == -1)
			return null;

		name = getUniqueName(name);

		mHitlistInfoList.add(new HitlistInfo(name, flagNo));

		long mask1 = getHitlistMask(hitlist1);
        long mask2 = getHitlistMask(hitlist2);
        long maskBoth = (mask1 | mask2);

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
			long availableFlags = (record.mFlags & maskBoth);
			switch (operation) {
			case OPERATION_AND:
				if (availableFlags == maskBoth)
					record.setFlag(flagNo);
				break;
			case OPERATION_OR:
				if (availableFlags != 0)
                    record.setFlag(flagNo);
				break;
			case OPERATION_XOR:
				if (availableFlags == mask1 || availableFlags == mask2)
                    record.setFlag(flagNo);
				break;
			case OPERATION_NOT:
				if (availableFlags == mask1)
                    record.setFlag(flagNo);
				break;
				}
			}

        mTableModel.setCompoundFlagDirty(flagNo);
        
		fireEvents(new CompoundTableHitlistEvent(this, CompoundTableHitlistEvent.cAdd, mHitlistInfoList.size()-1));
		return name;
		}

    /**
     * Add one individual record to a hitlist without firing any events
     * @param record
     * @param flag
     */
    public void addRecordSilent(CompoundRecord record, int flagNo) {
        record.setFlag(flagNo);
        }

    /**
     * Add one individual record to a hitlist and fire change events
     * @param record
     * @param hitlist
     */
    public void addRecord(CompoundRecord record, int hitlist) {
	    int flagNo = getHitlistFlagNo(hitlist);
	    if (!record.isFlagSet(flagNo)) {
	    	record.setFlag(flagNo);

	    	mTableModel.setCompoundFlagDirty(flagNo);

	        fireEvents(new CompoundTableHitlistEvent(this, CompoundTableHitlistEvent.cChange, hitlist));
			}
		}
	
    /**
     * Add one individual record to a hitlist and fire change events
     * @param record
     * @param hitlist
     */
    public void removeRecord(CompoundRecord record, int hitlist) {
	    int flagNo = getHitlistFlagNo(hitlist);
	    if (record.isFlagSet(flagNo)) {
	    	record.clearFlag(flagNo);

	    	mTableModel.setCompoundFlagDirty(flagNo);

	    	fireEvents(new CompoundTableHitlistEvent(this, CompoundTableHitlistEvent.cChange, hitlist));
			}
		}
	
    /**
     * add all selected and visible records to the given hitlist
     * @param hitlist
     */
    public void addSelected(int hitlist) {
	    int flagNo = getHitlistFlagNo(hitlist);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
			if (mTableModel.isVisibleAndSelected(record))
				record.setFlag(flagNo);
			}

        mTableModel.setCompoundFlagDirty(flagNo);

        fireEvents(new CompoundTableHitlistEvent(this, CompoundTableHitlistEvent.cChange, hitlist));
		}
	
	public void removeSelected(int hitlist) {
	    int flagNo = getHitlistFlagNo(hitlist);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
            if (mTableModel.isVisibleAndSelected(record))
				record.clearFlag(flagNo);
			}

        mTableModel.setCompoundFlagDirty(flagNo);

        fireEvents(new CompoundTableHitlistEvent(this, CompoundTableHitlistEvent.cChange, hitlist));
		}

	public void deleteHitlist(String name) {
		int index = indexOf(name);
		if (index != -1) {
			HitlistInfo info = mHitlistInfoList.remove(index);
			mTableModel.freeCompoundFlag(info.flagNo);

			fireEvents(new CompoundTableHitlistEvent(this, CompoundTableHitlistEvent.cDelete, index));
			}
		}

	public String[] getHitlistNames() {
		if (mHitlistInfoList.isEmpty())
			return null;

		String[] name = new String[mHitlistInfoList.size()];
		for (int i=0; i<mHitlistInfoList.size(); i++)
			name[i] = getHitlistName(i);

		return name;
		}

	protected void clearHitlistData() {
			// only to be called from CompoundTableModel on initializeTable()
		mHitlistInfoList.clear();
		}

	public String getUniqueName(String name) {
		while (name.equals(HITLISTNAME_NONE)
            || name.equals(HITLISTNAME_ANY)
            || indexOf(name) != -1) {
			int i = name.lastIndexOf('_');
			if (i == -1)
				name = name.concat("_2");
			else {
				try {
					int no = Integer.parseInt(name.substring(i+1));
					name = name.substring(0, i).concat("_"+(no+1));
					}
				catch (NumberFormatException e) {
					name = name.concat("_2");
					}
				}
			}

		return name;
		}

    private void fireEvents(final CompoundTableHitlistEvent e) {
        if (SwingUtilities.isEventDispatchThread()) {
			for (CompoundTableHitlistListener l:mListener)
				l.hitlistChanged(e);
        	}
        else {
        	try {
	        	SwingUtilities.invokeAndWait(new Runnable() {
	        		public void run() {
	        			for (CompoundTableHitlistListener l:mListener)
	        				l.hitlistChanged(e);
	        			}
	        		} );
        		}
            catch (Exception ee) {
                ee.printStackTrace();
                }
        	}
		}

	private int indexOf(String name) {
		for (int i=0; i<mHitlistInfoList.size(); i++) {
			if (mHitlistInfoList.get(i).name.equals(name))
				return i;
			}
		return -1;
		}
	}


class HitlistInfo {
	public String	name;
	public int 		flagNo;

    public HitlistInfo(String name, int flagNo) {
		this.name = name;
		this.flagNo = flagNo;
    	}
	}
