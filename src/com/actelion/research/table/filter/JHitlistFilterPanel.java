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

package com.actelion.research.table.filter;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableHitlistListener;
import com.actelion.research.table.CompoundTableModel;

public class JHitlistFilterPanel extends JFilterPanel implements ActionListener,CompoundTableHitlistListener {
    private static final long serialVersionUID = 0x20061013;
    private static final String LIST_ANY = "<any>";

	private JComboBox		mComboBox;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
    public JHitlistFilterPanel(CompoundTableModel tableModel) {
		this(tableModel, 0);
    	}

    public JHitlistFilterPanel(CompoundTableModel tableModel, int exclusionFlag) {
		super(tableModel, -4, exclusionFlag, false);	// pass pseudo column for first hitlist

    	JPanel p1 = new JPanel();
        p1.setOpaque(false);
		p1.add(new JLabel("List name:"));

		mComboBox = new JComboBox();
		mComboBox.addItem(CompoundTableHitlistHandler.HITLISTNAME_NONE);
		for (int i=0; i<mTableModel.getHitlistHandler().getHitlistCount(); i++)
			mComboBox.addItem(mTableModel.getHitlistHandler().getHitlistName(i));
        mComboBox.addItem(CompoundTableHitlistHandler.HITLISTNAME_ANY);
        if (isActive())
        	mComboBox.addActionListener(this);
        else
        	mComboBox.setEditable(true);
		p1.add(mComboBox);

		add(p1, BorderLayout.CENTER);

		mIsUserChange = true;
    	}

    @Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBox) {
			updateExclusion();
			return;
			}

		super.actionPerformed(e);
		}

    @Override
    public boolean isFilterEnabled() {
    	return mComboBox.getSelectedIndex() != 0;
    	}

    private int getHitlistIndex() {
        int selectedIndex = mComboBox.getSelectedIndex();
        return (selectedIndex == 0) ?
                    CompoundTableHitlistHandler.HITLISTINDEX_NONE
             : (selectedIndex == mComboBox.getItemCount()-1) ?
                    CompoundTableHitlistHandler.HITLISTINDEX_ANY
             :      selectedIndex - 1;
        }

    private void updateExclusion() {
		if (isActive()) {
			mTableModel.setHitlistExclusion(getHitlistIndex(), mExclusionFlag, isInverse());
	    	fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			}
		}

    @Override
	public String getInnerSettings() {
    	String selected = (String)mComboBox.getSelectedItem();
		return (CompoundTableHitlistHandler.HITLISTNAME_NONE.equals(selected)) ? null
			 : (CompoundTableHitlistHandler.HITLISTNAME_ANY.equals(selected)) ? LIST_ANY
			 : selected;
    	}

	@Override
	public void applyInnerSettings(String settings) {
        if (settings != null
         && !CompoundTableHitlistHandler.HITLISTNAME_NONE.equals(settings)) {		// was this an ancient way of encoding???
            if (LIST_ANY.equals(settings)) {
                mComboBox.setSelectedIndex(mComboBox.getItemCount()-1);
                }
            else {
            	if (!isActive()) {
					mComboBox.setSelectedItem(settings);
            		}
            	else {
	    			for (int i=0; i<mTableModel.getHitlistHandler().getHitlistCount(); i++) {
	    				if (mTableModel.getHitlistHandler().getHitlistName(i).equals(settings)) {
	    					mComboBox.setSelectedIndex(i+1);
	    					break;
	    					}
	    				}
	                }
            	}
            }

        if (isInverse() || mComboBox.getSelectedIndex() != 0)
            updateExclusion();
		}

	@Override
	public void innerReset() {
		if (mComboBox.getSelectedIndex() != 0) {
			mComboBox.setSelectedIndex(0);
			updateExclusion();
			}
		}

    public void compoundTableChanged(CompoundTableEvent e) {
        // avoid the default behaviour;
        }

	public void hitlistChanged(CompoundTableHitlistEvent e) {
		CompoundTableHitlistHandler hitlistHandler = mTableModel.getHitlistHandler();
        int hitlistCount = hitlistHandler.getHitlistCount();
        boolean anySelected = CompoundTableHitlistHandler.HITLISTNAME_ANY.equals(mComboBox.getSelectedItem());
        boolean changedListSelected = (mComboBox.getSelectedIndex()-1 == e.getHitlistIndex());
        boolean update = false;
		if (e.getType() == CompoundTableHitlistEvent.cAdd) {
            update = anySelected;
			mComboBox.insertItemAt(hitlistHandler.getHitlistName(hitlistCount-1), hitlistCount);
			}
		else if (e.getType() == CompoundTableHitlistEvent.cDelete) {
            update = (anySelected || changedListSelected);
            if (changedListSelected)
                mComboBox.setSelectedIndex(0);
            mComboBox.removeItemAt(e.getHitlistIndex()+1);
			}
		else if (e.getType() == CompoundTableHitlistEvent.cChange) {
            update = (anySelected || changedListSelected);
			}

        if (update) {
        	mIsUserChange = false;
            updateExclusion();
        	mIsUserChange = true;
        	}
		}
	}
