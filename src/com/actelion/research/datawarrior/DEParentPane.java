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

package com.actelion.research.datawarrior;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import com.actelion.research.gui.dock.ShadowBorder;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.RuntimePropertyListener;
import com.actelion.research.table.view.CompoundTableView;

public class DEParentPane extends JComponent implements DetailPopupProvider  {
    private static final long serialVersionUID = 0x20060904;

    private Frame mParentFrame;
    private DatabaseActions mDatabaseActions;
	private DECompoundTableModel mTableModel;
	private JSplitPane mMainSplitPane;
	private DEMainPane mTabbedMainViews;
	private JSplitPane mRightSplitPane;
	private DEPruningPanel mPruningPanel;
	private DEDetailPane mTabbedDetailViews;
	private ArrayList<RuntimePropertyListener> mRPListener;

	public DEParentPane(Frame parent, DECompoundTableModel tableModel, DEDetailPane detailPane, DatabaseActions databaseActions) {
	    mParentFrame = parent;
	    mTableModel = tableModel;
	    mDatabaseActions = databaseActions;

	    setLayout(new BorderLayout());

	    DEStatusPanel statusPanel = new DEStatusPanel(mTableModel, mTabbedMainViews);
		add(statusPanel, BorderLayout.SOUTH);

		mMainSplitPane = new JSplitPane();
		mTabbedDetailViews = new DEDetailPane(mTableModel);
		mTabbedDetailViews.setBorder(new ShadowBorder(1,1,3,6));
		mTabbedMainViews = new DEMainPane(mParentFrame, mTableModel, mTabbedDetailViews, statusPanel, this);
		mRightSplitPane = new JSplitPane();
		mPruningPanel = new DEPruningPanel(mParentFrame, this, mTableModel);
		mPruningPanel.setBorder(new ShadowBorder(7,1,1,6));

	    mMainSplitPane.setBorder(null);
		mMainSplitPane.setOneTouchExpandable(true);
	    mMainSplitPane.setContinuousLayout(true);
	    mMainSplitPane.setResizeWeight(0.75);
		mMainSplitPane.setDividerSize(10);
	    mRightSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
	    mRightSplitPane.setMinimumSize(new Dimension(100, 200));
	    mRightSplitPane.setPreferredSize(new Dimension(100, 200));
		mRightSplitPane.setOneTouchExpandable(true);
	    mRightSplitPane.setContinuousLayout(true);
	    mRightSplitPane.setResizeWeight(0.7);
		mRightSplitPane.setDividerSize(10);
	    add(mMainSplitPane, BorderLayout.CENTER);
	    mMainSplitPane.add(mTabbedMainViews, JSplitPane.LEFT);
	    mMainSplitPane.add(mRightSplitPane, JSplitPane.RIGHT);
	    mRightSplitPane.add(mPruningPanel, JSplitPane.TOP);
	    mRightSplitPane.add(mTabbedDetailViews, JSplitPane.BOTTOM);

		mRPListener = new ArrayList<RuntimePropertyListener>();
		}

	public void addRuntimePropertyListener(RuntimePropertyListener l) {
		mRPListener.add(l);
		}

	public void removeRuntimePropertyListener(RuntimePropertyListener l) {
		mRPListener.remove(l);
		}

	public void fireRuntimePropertyChanged(RuntimePropertyEvent e) {
		for (RuntimePropertyListener l:mRPListener)
			l.runtimePropertyChanged(e);
		}

	public DEMainPane getMainPane() {
		return mTabbedMainViews;
		}

	public DEDetailPane getDetailPane() {
		return mTabbedDetailViews;
		}

	public DEPruningPanel getPruningPanel() {
		return mPruningPanel;
		}

	public DECompoundTableModel getTableModel() {
		return mTableModel;
		}

	public JPopupMenu createPopupMenu(CompoundRecord record, CompoundTableView source, int selectedColumn) {
        JPopupMenu popup = new DEDetailPopupMenu(mTabbedMainViews, record, mPruningPanel, source, mDatabaseActions, selectedColumn);
        return (popup.getComponentCount() == 0) ? null : popup;
        }

	public double getMainSplitting() {
		return (double)mMainSplitPane.getDividerLocation() / (double)(mMainSplitPane.getWidth() - mMainSplitPane.getDividerSize());
		}

	public double getRightSplitting() {
		return (double)mRightSplitPane.getDividerLocation() / (double)(mRightSplitPane.getHeight() - mRightSplitPane.getDividerSize());
		}

	public void setMainSplitting(double l) {
		mMainSplitPane.setDividerLocation(l);
		}

	public void setRightSplitting(double l) {
		mRightSplitPane.setDividerLocation(l);
		}
	}