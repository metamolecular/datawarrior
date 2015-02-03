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

package com.actelion.research.gui;

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.actelion.research.calc.ProgressController;

public class JProgressDialog extends JDialog implements ActionListener,ProgressController,Runnable {
	private static final long serialVersionUID = 0x20070301;

	private static final int sActionDispose = 2;
	private static final int sActionStart = 4;
	private static final int sActionUpdate = 8;
	private static final int sActionStop = 16;

	private JProgressBar	mProgressBar;
	private JLabel			mProgressLabel,mLabelRemainingTime;
	private Frame			mNewFrontFrame;
	private String			mBusyText;
	private volatile boolean mProcessCancelled;
	private volatile int	mProgressMin,mProgressMax,mProgressValue,mAction;
	private volatile long   mProgressStart,mLastUpdate;

	/**
	 * Creates a JProgressDialog and schedule it with invokeLater()
	 * to be set visible without blocking the calling thread.
	 * @param owner
	 */
	public JProgressDialog(Frame owner) {
		this(owner, true);
		}

	/**
	 * Creates a JProgressDialog. If invokeSetVisible is true,
	 * then the dialog is scheduled to be set visible with invokeLater()
	 * without blocking the current thread. Otherwise the caller needs
	 * to call setVisible() manually.
	 * @param owner
	 * @param invokeSetVisible
	 */
	public JProgressDialog(Frame owner, boolean invokeSetVisible) {
			// initialized and shows the modal dialog without blocking the thread
		super(owner, true);
		initialize();
		setLocationRelativeTo(owner);
		if (invokeSetVisible)
			SwingUtilities.invokeLater(new Runnable() { public void run() { setVisible(true); } } );
		}

	public void startProgress(String text, int min, int max) {
			// may be called safely from any thread
		mProgressStart = System.currentTimeMillis();
		mBusyText = text;
		mProgressMin = min;
		mProgressValue = min;
		mProgressMax = max;
		mAction |= sActionStart;
		update();
		}

	/**
	 * Update progress status in an absolute or relative way.
	 * @param value if negative, its abs value is added to current progress.
	 */
	public void updateProgress(int value) {
			// may be called safely from any thread
		int newValue = (value >= 0) ? value : mProgressValue-value;
		if (mProgressValue < newValue) {
			mProgressValue = newValue;
			mAction |= sActionUpdate;
			update();
			}
		}

	public void stopProgress() {
			// may be called safely from any thread
		mAction |= sActionStop;
		update();
		}

	/**
	 * If you need to move a newly created window to the front, then use close(Frame newFrontFrame)
	 */
	public void close() {
		close(null);
		}

	/**
	 * Disposes of the progress dialog and optionally moves the specified
	 * frame to the front. This may be called safely from any thread.
	 * @param newFrontFrame null or frame to be moved to the front
	 */
	public void close(Frame newFrontFrame) {
		mAction |= sActionDispose;
		mNewFrontFrame = newFrontFrame;
		update();
		}

	public void showErrorMessage(final String message) {
		if (SwingUtilities.isEventDispatchThread()) {
			showMessage(message);
			}
		else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						showMessage(message);
						}
					});
				}
			catch (Exception e) {}
			}
		}		

	private void showMessage(String message) {
		JOptionPane.showMessageDialog(this, message);
		}

	public boolean threadMustDie() {
		return mProcessCancelled;
		}

	public void actionPerformed(ActionEvent e) {
		mProcessCancelled = true;
		mAction |= sActionStop;
		update();
		}

	private void initialize() {
		double[][] size = { {8, 200, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8} };
		getContentPane().setLayout(new TableLayout(size));

		mProgressLabel = new JLabel(" ");
		getContentPane().add(mProgressLabel, "1,1,3,1");

		mProgressBar = new JProgressBar();
		mProgressBar.setVisible(false);
		getContentPane().add(mProgressBar, "1,3,3,3");

		mLabelRemainingTime = new JLabel();
		getContentPane().add(mLabelRemainingTime, "1,5");

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		getContentPane().add(cancelButton, "3,5");
		pack();
		}

	private void update() {
		long millis = System.currentTimeMillis();
		if ((mAction & ~sActionUpdate) != 0 || millis - mLastUpdate > 100) {
			if (SwingUtilities.isEventDispatchThread()) {
				mLastUpdate = millis;
				run();
				}
			else {
//				try {
					mLastUpdate = millis;
					SwingUtilities.invokeLater(this);
   //				 }
 //			   catch (Exception e) {}
				}
			}		
		}

	public void run() {
		if ((mAction & sActionDispose) != 0) {
			mAction &= ~sActionDispose;
			setVisible(false);
			dispose();
			if (mNewFrontFrame != null)
				mNewFrontFrame.toFront();
			return;
			}
		if ((mAction & sActionStart) != 0) {
			mAction &= ~sActionStart;
			if (mProgressMin == mProgressMax) {
				mProgressBar.setIndeterminate(true);
				}
			else {
				mProgressBar.setIndeterminate(false);
				mProgressBar.setMinimum(mProgressMin);
				mProgressBar.setMaximum(mProgressMax);
				mProgressBar.setValue(mProgressMin);
				}
			mProgressBar.setVisible(true);
			mProgressLabel.setText(mBusyText);
			}
		if ((mAction & sActionUpdate) != 0) {
			mAction &= ~sActionUpdate;
			mProgressBar.setValue(mProgressValue);
			if (mProgressValue > mProgressMin) {
				long milliesUsed = System.currentTimeMillis() - mProgressStart;
				long milliesToGo = milliesUsed * (mProgressMax - mProgressValue) / (mProgressValue - mProgressMin);
				String timeToGo = (milliesToGo >= 7200000) ? ""+(milliesToGo/3600000)+" hours remaining"
								: (milliesToGo >=  120000) ? ""+(milliesToGo/60000)+" minutes remaining"
								: 							 ""+(milliesToGo/1000)+" seconds remaining";
				mLabelRemainingTime.setText(timeToGo);
				}
			}
		if ((mAction & sActionStop) != 0) {
			mAction &= ~sActionStop;
			mProgressLabel.setText(mProcessCancelled ? "Cancelled; cleaning up..." : "");
			if (mProcessCancelled)
				mProgressBar.setIndeterminate(true);
			else {
				mProgressBar.setValue(mProgressBar.getMinimum());
				mProgressBar.setVisible(false);
				}
			}
		}
	}
