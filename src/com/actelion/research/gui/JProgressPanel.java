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

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.actelion.research.calc.ProgressController;

public class JProgressPanel extends JPanel implements ActionListener,ProgressController,Runnable {
    private static final long serialVersionUID = 0x20080207;

	private static final int sActionStart = 4;
	private static final int sActionUpdate = 8;
	private static final int sActionStop = 16;

	private JProgressBar	mProgressBar;
	private volatile boolean mProcessCancelled;
	private volatile int	mProgressMin,mProgressMax,mProgressValue,mAction;
    private volatile long	mLastUpdate;

	public JProgressPanel(boolean showCancelButton) {
		super();
		initialize(showCancelButton);
		}

	public void startProgress(String text, int min, int max) {
			// may be called safely from any thread
//		mBusyText = text;
		mProgressMin = min;
		mProgressValue = min;
		mProgressMax = max;
		mAction |= sActionStart;
        update();
		}

	public void updateProgress(int value) {
			// may be called safely from any thread
		if (mProgressValue < value) {
			mProgressValue = value;
			mAction |= sActionUpdate;
	        update();
			}
		}

	public void stopProgress() {
			// may be called safely from any thread
		mAction |= sActionStop;
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

	public boolean threadMustDie() {
		return mProcessCancelled;
		}

	public void actionPerformed(ActionEvent e) {
		mProcessCancelled = true;
        mAction |= sActionStop;
        update();
		}

	private void initialize(boolean showCancelButton) {
        double[][] sizeWithCancel =    { {8, TableLayout.FILL, 4, TableLayout.PREFERRED, 8},
                                         {TableLayout.FILL, 8, TableLayout.FILL} };
        double[][] sizeWithoutCancel = { {8, TableLayout.FILL, 8},
                                         {TableLayout.FILL, 8, TableLayout.FILL} };
		setLayout(new TableLayout(showCancelButton ? sizeWithCancel : sizeWithoutCancel));

		mProgressBar = new JProgressBar();
		mProgressBar.setVisible(false);
		add(mProgressBar, "1,1");

        if (showCancelButton) {
    		JButton cancelButton = new JButton("Cancel");
    		cancelButton.addActionListener(this);
    		add(cancelButton, "3,0,3,2");
            }
		}

    private void update() {
        long millis = System.currentTimeMillis();
        if ((mAction & ~sActionUpdate) != 0 || millis - mLastUpdate > 100) {
            if (SwingUtilities.isEventDispatchThread()) {
                mLastUpdate = millis;
                run();
                }
            else {
                try {
                    mLastUpdate = millis;
                    SwingUtilities.invokeAndWait(this);
                    }
                catch (Exception e) {}
                }
            }        
        }

	private void showMessage(String message) {
		Component c = this;
		while (!(c instanceof Frame))
			c = c.getParent();
        JOptionPane.showMessageDialog((Frame)c, message);
		}

    public void run() {
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
//          mProgressLabel.setText(mBusyText);
            }
        if ((mAction & sActionUpdate) != 0) {
            mAction &= ~sActionUpdate;
            mProgressBar.setValue(mProgressValue);
            }
        if ((mAction & sActionStop) != 0) {
            mAction &= ~sActionStop;
//          mProgressLabel.setText(mProcessCancelled ? "Cancelled; cleaning up..." : "");
            if (mProcessCancelled)
                mProgressBar.setIndeterminate(true);
            else {
                mProgressBar.setValue(mProgressBar.getMinimum());
                mProgressBar.setVisible(false);
                }
            }
        }
    }
