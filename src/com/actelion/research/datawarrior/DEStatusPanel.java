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

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.table.CompoundListSelectionModel;
import com.actelion.research.table.CompoundTableModel;

/**
 * Title:		DEStatusPanel.java
 * Description:  Status panel with progress bar and message area for data explorer
 * Copyright:	Copyright (c) 2001
 * Company:	  Actelion Ltd.
 * @author	   Thomas Sander
 * @version 1.0
 */

public class DEStatusPanel extends JPanel
		implements ListSelectionListener,ProgressListener {
	private static final long serialVersionUID = 0x20060904;

	private static final int SHOW_ERROR = 1;
	private static final int START_PROGRESS = 2;
	private static final int UPDATE_PROGRESS = 3;
	private static final int STOP_PROGRESS = 4;

	private CompoundTableModel  mTableModel;
	private int mRecords,mVisible,mSelected;
	private JProgressBar mProgressBar = new JProgressBar();
	private JLabel mSelectedLabel = new JLabel();
	private JLabel mVisibleLabel = new JLabel();
	private JLabel mRecordsLabel = new JLabel();
	private JLabel mProgressLabel = new JLabel();
	private DEProgressPanel mMacroProgressPanel = new DEProgressPanel(true);

	public DEStatusPanel(CompoundTableModel tableModel, DEMainPane mainPane) {
		mTableModel = tableModel;
		mTableModel.addProgressListener(this);

		Font font = new Font("Helvetica", Font.BOLD, 12);

		JLabel LabelSelected = new JLabel("Selected:", SwingConstants.RIGHT);
		JLabel LabelVisible = new JLabel("Visible:", SwingConstants.RIGHT);
		JLabel LabelRecords = new JLabel("Total:", SwingConstants.RIGHT);
		LabelSelected.setFont(font);
		LabelVisible.setFont(font);
		LabelRecords.setFont(font);
		LabelSelected.setForeground(Color.GRAY);
		LabelVisible.setForeground(Color.GRAY);
		LabelRecords.setForeground(Color.GRAY);

		mProgressLabel.setFont(font);
		mSelectedLabel.setFont(font);
		mVisibleLabel.setFont(font);
		mRecordsLabel.setFont(font);

		mProgressBar.setVisible(false);
		mProgressBar.setPreferredSize(new Dimension(80,8));
		mProgressBar.setMaximumSize(new Dimension(80,8));
		mProgressBar.setMinimumSize(new Dimension(80,8));
		mProgressBar.setSize(new Dimension(80,8));

		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.FILL, 60, 60, 60, 60, 60, 60, TableLayout.FILL},
							{4, 4, TableLayout.PREFERRED, 4, 2} };

		setLayout(new TableLayout(size));
		add(mProgressBar, "1,2");
		add(mProgressLabel, "3,1,3,3");
		add(LabelSelected, "4,1,4,3");
		add(mSelectedLabel, "5,1,5,3");
		add(LabelVisible, "6,1,6,3");
		add(mVisibleLabel, "7,1,7,3");
		add(LabelRecords, "8,1,8,3");
		add(mRecordsLabel, "9,1,9,3");
		add(mMacroProgressPanel, "10,1,10,3");
		}

	public DEProgressPanel getMacroProgressPanel() {
		return mMacroProgressPanel;
		}

	public void setNoOfRecords(int no) {
		if (no != mRecords) {
			mRecordsLabel.setText(""+no);
			mRecords = no;
			}
		}

	public void setNoOfVisible(int no) {
		if (no != mVisible) {
			mVisibleLabel.setText(""+no);
			mVisible = no;
			}
		}

	private void setNoOfSelected(int no) {
		if (no != mSelected) {
			mSelectedLabel.setText(""+no);
			mSelected = no;
			}
		}

	public void setRecording(boolean isRecording) {
		mMacroProgressPanel.showMessage(isRecording ? "Recording..." : "");
		}

	public void startProgress(String text, int min, int max) {
		doActionThreadSafe(START_PROGRESS, text, min, max);
		}

	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			setNoOfSelected(((CompoundListSelectionModel)e.getSource()).getSelectionCount());
			}
		}

	public void updateProgress(int value) {
		doActionThreadSafe(UPDATE_PROGRESS, null, value, 0);
		}

	public void stopProgress() {
		doActionThreadSafe(STOP_PROGRESS, null, 0, 0);
		}

	public void showErrorMessage(final String message) {
		doActionThreadSafe(SHOW_ERROR, message, 0, 0);
		}		

	private void doActionThreadSafe(final int action, final String text, final int v1, final int v2) {
		if (SwingUtilities.isEventDispatchThread()) {
			doAction(action, text, v1, v2);
			}
		else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						doAction(action, text, v1, v2);
						}
					});
				}
			catch (Exception e) {}
			}
		}

	private void doAction(final int action, final String text, final int v1, final int v2) {
		switch (action) {
		case SHOW_ERROR:
			Component c = this;
			while (!(c instanceof Frame))
				c = c.getParent();
			JOptionPane.showMessageDialog((Frame)c, text);
			break;
		case START_PROGRESS:
			mProgressBar.setVisible(true);
			mProgressBar.setMinimum(v1);
			mProgressBar.setMaximum(v2);
			mProgressBar.setValue(v1);
			mProgressLabel.setText(text);
			break;
		case UPDATE_PROGRESS:
			int value = (v1 >= 0) ? v1 : mProgressBar.getValue()-v1;
			mProgressBar.setValue(value);
			break;
		case STOP_PROGRESS:
			mProgressLabel.setText("");
			mProgressBar.setValue(mProgressBar.getMinimum());
			mProgressBar.setVisible(false);
			break;
			}
		}
	}
