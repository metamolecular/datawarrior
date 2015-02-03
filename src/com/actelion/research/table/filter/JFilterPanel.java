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

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;

public abstract class JFilterPanel extends JPanel
		implements ActionListener,CompoundTableListener {
	private static final long serialVersionUID = 0x20110325;

	static private final String cInverse = "#inverse#";
	static private ImageIcon sIcon,sInverseIcon,sNotInverseIcon,sAnimateIcon;

	protected CompoundTableModel	mTableModel;
	protected int					mColumnIndex,mExclusionFlag;
	protected boolean				mIsUserChange;	// Is set from derived classes once the UI is complete
													// and is temporarily disabled, whenever a programmatic change occurs.
	private JLabel					mColumnNameLabel;
	private JPanel					mTitlePanel;
	private JButton					mButtonInverse;
	private JPopupMenu				mAnimationPopup;
	private boolean					mIsInverse;
	private Animator				mAnimator;
	private JTextField				mTextFieldFrameDelay;
	private JDialog					mOptionsDialog;
	private ArrayList<FilterListener> mListenerList;

	/**
	 * @param tableModel
	 * @param column
	 * @param exclusionFlag if 0 then this filter is dead, i.e. it doesn't influence row visibility
	 * @param showAnimationOptions
	 */
	public JFilterPanel(CompoundTableModel tableModel, int column, int exclusionFlag, boolean showAnimationOptions) {
		mExclusionFlag = exclusionFlag;
		mTableModel = tableModel;
		mColumnIndex = column;

		setOpaque(false);
		setLayout(new BorderLayout());

		mTitlePanel = new JPanel();
		double[][] size = { {4, TableLayout.FILL, 4, TableLayout.PREFERRED, TableLayout.PREFERRED, 8, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 4} };
		mTitlePanel.setLayout(new TableLayout(size));
		mTitlePanel.setOpaque(false);
		mColumnNameLabel = new JLabel((mColumnIndex == -4) ? "Row list" : isActive() ? mTableModel.getColumnTitle(mColumnIndex) : "") {
			private static final long serialVersionUID = 0x20080128;
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = Math.min(size.width, 72);
				return size;
				}
			};
		mColumnNameLabel.setMaximumSize(new Dimension(50,mColumnNameLabel.getMaximumSize().height));
		mTitlePanel.add(mColumnNameLabel, "1,0");

		if (sIcon == null)
			sIcon = new ImageIcon(this.getClass().getResource("/images/closeButton.png"));
		if (sNotInverseIcon == null)
			sNotInverseIcon = new ImageIcon(this.getClass().getResource("/images/yy16.png"));
		if (sInverseIcon == null)
			sInverseIcon = new ImageIcon(this.getClass().getResource("/images/iyy16.png"));

		JPanel lbp = new JPanel();
		mButtonInverse = createButton(sNotInverseIcon, 18, 18, "inverse", this);
		mButtonInverse.setToolTipText("Invert filter");
		lbp.add(mButtonInverse);
		mTitlePanel.add(lbp, "4,0");

		if (isActive()) {
			JPanel rbp = new JPanel();
			JButton cb = createButton(sIcon, 14, 14, "close", this);
			rbp.add(cb);
			mTitlePanel.add(rbp, "6,0");
			}

		add(mTitlePanel, BorderLayout.NORTH);

		if (showAnimationOptions) {
			mAnimator = new Animator(500);
			showAnimationControls();
			}
		}

	public void addFilterListener(FilterListener l) {
		if (mListenerList == null)
			mListenerList = new ArrayList<FilterListener>();
		mListenerList.add(l);
		}

	public void removeFilterListener(FilterListener l) {
		if (mListenerList != null)
			mListenerList.remove(l);
		if (mListenerList.size() == 0)
			mListenerList = null;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	/**
	 * Informs all listeners of a user induced filter change.
	 * Events are sent only, if mIsUserChange is true.
	 * To make this possible, any derived class
	 * is responsible to set mIsUserChange to true after the construction.
	 * Any derived class must also temporarily set mIsUserChange to false,
	 * if something that was not caused by a direct user interaction
	 * results in an updateExclusion() call or any direct tableModel call,
	 * which changes row exclusion.
	 * @param type
	 * @param isAdjusting
	 */
	public void fireFilterChanged(int type, boolean isAdjusting) {
		if (mListenerList != null && mIsUserChange)
			for (FilterListener l:mListenerList)
				l.filterChanged(new FilterEvent(this, type, isAdjusting));
		}

	/**
	 * Determines whether the filter is actively contributing to row visibility.
	 * Filters that are just used to configure a macro task or already removed filters are inactive.
	 * @return whether the filter is active
	 */
	public boolean isActive() {
		return (mExclusionFlag != 0);
		}

	public static JButton createButton(ImageIcon icon, int w, int h, String command, ActionListener listener) {
		JButton button = new JButton(icon);
		if ("quaqua".equals(System.getProperty("com.actelion.research.laf"))) {
			w += 4;
			h += 3;
			button.putClientProperty("Quaqua.Component.visualMargin", new Insets(1,1,1,1));
			button.putClientProperty("Quaqua.Button.style", "bevel");
			}
		button.setPreferredSize(new Dimension(w, h));
		if (command != null) {
			button.addActionListener(listener);
			button.setActionCommand(command);
			}
		return button;
		}

	/**
	 * This is called to check whether an inversion can be performed.
	 * For disabled filters the state of the inversion checkbox is irrelevant
	 * and interactively toggling its state should not have an effect.
	 * @return true if filter is enabled and can be interactively inverted
	 */
	public abstract boolean isFilterEnabled();

	/**
	 * This must be overwritten by filter panels which are not
	 * directly associated with a not changing column index.
	 */
	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			mColumnIndex = e.getMapping()[mColumnIndex];
			if (mColumnIndex == -1)
				removePanel();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			if (mColumnIndex == e.getSpecifier())
				mColumnNameLabel.setText(mTableModel.getColumnTitle(mColumnIndex));
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("close")) {
			fireFilterChanged(FilterEvent.FILTER_CLOSED, false);
			removePanel();
			return;
			}
		if (e.getActionCommand().equals("options")) {
			showOptionDialog(createAnimationOptionPanel());
			return;
			}
		if (e.getActionCommand().equals("optionsCancel")) {
			mOptionsDialog.setVisible(false);
			mOptionsDialog.dispose();
			return;
			}
		if (e.getActionCommand().equals("optionsOK")) {
			try {
				setFrameDelay(Long.parseLong(mTextFieldFrameDelay.getText()));
				mOptionsDialog.setVisible(false);
				mOptionsDialog.dispose();
				}
			catch (NumberFormatException nfe) {
				}
			return;
			}
		if (e.getActionCommand().equals("start")) {
			fireFilterChanged(FilterEvent.FILTER_ANIMATION_STARTED, false);
			startAnimation();
			return;
			}
		if (e.getActionCommand().equals("stop")) {
			fireFilterChanged(FilterEvent.FILTER_ANIMATION_STOPPED, false);
			stopAnimation();
			return;
			}
		if (e.getSource() == mButtonInverse) {
			fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			setInverse(!mIsInverse);
			return;
			}
		}

	private void showOptionDialog(JPanel content) {
		Component frame = this;
		while (!(frame instanceof Frame))
			frame = frame.getParent();
		
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.setActionCommand("optionsCancel");
		bcancel.addActionListener(this);
		ibp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.setActionCommand("optionsOK");
		bok.addActionListener(this);
		ibp.add(bok);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		buttonPanel.add(ibp, BorderLayout.EAST);

		mOptionsDialog = new JDialog((Frame)frame, "Set Animation Options", true);
		mOptionsDialog.getContentPane().setLayout(new BorderLayout());
		mOptionsDialog.getContentPane().add(content, BorderLayout.CENTER);
		mOptionsDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		mOptionsDialog.getRootPane().setDefaultButton(bok);
		mOptionsDialog.pack();
		mOptionsDialog.setLocationRelativeTo((Frame)frame);
		mOptionsDialog.setVisible(true);
		}

	protected JPanel createAnimationOptionPanel() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));
		p.add(new JLabel("Frame delay (ms):"), "1,1");

		mTextFieldFrameDelay = new JTextField(""+mAnimator.getFrameDelay(), 6);
		p.add(mTextFieldFrameDelay, "3,1");

		return p;
		}

	public boolean isInverse() {
		return mIsInverse;
		}

	public void removePanel() {
		if (mAnimator != null)
			resetAnimation();

		mTableModel.freeCompoundFlag(mExclusionFlag);
		mExclusionFlag = 0;
		Container theParent = getParent();
		theParent.remove(this);
		theParent.getParent().validate();
		theParent.getParent().repaint();
		}

	public int getColumnIndex() {
		return mColumnIndex;
		}

	public void setColumnIndex(int index) {
		mColumnIndex = index;
		}

	protected void setText(String text, Color color) {
		mColumnNameLabel.setForeground(color);
		mColumnNameLabel.setText(text);
		}

	/**
	 * Override this, if a derived filter has extended animation settings.
	 * @return
	 */
	public String getAnimationSettings() {
		return (mAnimator != null) ? (mAnimator.isAnimating() ? "state=running" : "state=stopped")
									+ " delay="+mAnimator.getFrameDelay() : null;
		}

	public void setInverse(boolean b) {
		if (mIsInverse != b) {
			mIsInverse = b;
			mButtonInverse.setIcon(mIsInverse ? sInverseIcon : sNotInverseIcon);
			if (isFilterEnabled() && isActive()) {
				mTableModel.invertExclusion(mExclusionFlag);
				}
			}
		}

	public final String getSettings() {
		String innerSettings = getInnerSettings();
		return isInverse() ? (innerSettings == null ? cInverse : cInverse+'\t'+innerSettings)
						   : (innerSettings == null ? null : innerSettings);
		}

	public void applySettings(String settings) {
		mIsUserChange = false;
		boolean inverse = (settings != null && settings.startsWith(cInverse));

		setInverse(inverse);
		applyInnerSettings(!inverse ? settings : (settings.length() == cInverse.length()) ?
							null : settings.substring(cInverse.length()+1));
		mIsUserChange = true;
		}

	public abstract void applyInnerSettings(String settings);
	public abstract String getInnerSettings();

	public final void applyAnimationSettings(String settings) {
		mIsUserChange = false;
		resetAnimation();
		if (settings != null) {
			boolean running = false;
			for (String setting:settings.split(" ")) {
				int index = setting.indexOf('=');
				if (index != -1) {
					String key = setting.substring(0, index);
					String value = setting.substring(index+1);
					if (key.equals("state"))
						running = value.equals("running");
					else if (key.equals("delay"))
						try { setFrameDelay(Long.parseLong(value)); } catch (NumberFormatException e) {}
					else
						applyAnimationSetting(key, value);
					}
				}
			if (running)
				startAnimation();
			}
		mIsUserChange = true;
		}


	/**
	 * Override this, if a derived filter has extended animation settings.
	 * @return
	 */
	protected void applyAnimationSetting(String key, String value) {}

	protected String attachSetting(String settingList, String setting) {
		if (settingList == null)
			return setting;

		return settingList + "\t" + setting;
		}

	/**
	 * Subclass should disable filtering or change to a setting that includes all records
	 * or if this is not possible then remove entire filter.
	 * This is only called, if the filter is active.
	 */
	public void reset() {
		mIsUserChange = false;
		if (mAnimator != null)
			resetAnimation();

		innerReset();

		setInverse(false);
		mIsUserChange = true;
		}

	/**
	 * Only called, if the filter is active.
	 */
	public abstract void innerReset();

	public void showAnimationControls() {
		if (sAnimateIcon == null && mAnimator != null)
			sAnimateIcon = new ImageIcon(this.getClass().getResource("/images/gear14.png"));

		JPanel mbp = new JPanel();
		JButton button = createButton(sAnimateIcon, 32, 18, "showPopup", this);
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (mAnimationPopup == null)
					showAnimationPopup(e);
				}
			});
		mbp.add(button);
		mTitlePanel.add(mbp, "3,0");
		}

	private void showAnimationPopup(MouseEvent e) {
		JButton button = (JButton)e.getSource();
		mAnimationPopup = new JPopupMenu() {
			private static final long serialVersionUID = 1L;

			@Override
			public void setVisible(boolean b) {
				super.setVisible(b);
				mAnimationPopup = null;
				}
			};
		mAnimationPopup.add(createPopupItem("Start Animation", "start"));
		mAnimationPopup.add(createPopupItem("Stop Animation", "stop"));
		mAnimationPopup.addSeparator();
		mAnimationPopup.add(createPopupItem("Animation Options...", "options"));
		mAnimationPopup.show(button.getParent(),
							 button.getBounds().x,
							 button.getBounds().y + button.getBounds().height);
		}

	private JMenuItem createPopupItem(String title, String command) {
		JMenuItem item = new JMenuItem(title);
		item.setActionCommand(command);
		item.addActionListener(this);
		return item;
		}

	/**
	 * Changes the delay that the animation timer waits between two subsequent calls to animate().
	 */
	public void setFrameDelay(long frameDelay) {
		if (isActive())
			mAnimator.setFrameDelay(frameDelay);
		}

	/**
	 * Stops the animation timer and sets the current frame to 0.
	 */
	public void resetAnimation() {
		if (isActive())
			mAnimator.reset();
		}

	/**
	 * Starts or continues the animation with the current frame rate and number.
	 */
	public void startAnimation() {
		if (isActive())
			mAnimator.start();
		}

	/**
	 * Stops the animation timer without changing the current frame number.
	 */
	public void stopAnimation() {
		if (isActive())
			mAnimator.stop();
		}

	/**
	 * Override this if the derived filter supports animations.
	 * When an animation is running this method is called repeatedly
	 * after waiting for the frame delay with an increasing frame number.
	 * The filter should react by updating its filter settings to reflect
	 * the current frame number.
	 * @param frame
	 */
	protected void animate(int frame) {}

	private class Animator implements Runnable {
		private volatile long mStartMillis,mFrameDelay;
		private Thread mThread;

		public Animator(long frameDelay) {
			mFrameDelay = frameDelay;
			}

		public boolean isAnimating() {
			return mThread != null;
			}

		public long getFrameDelay() {
			return mFrameDelay;
			}

		public void setFrameDelay(long frameDelay) {
			mFrameDelay = frameDelay;
			}

		public void reset() {
			mThread = null;
			}

		public void start() {
			if (mThread == null) {
				mThread = new Thread(this);
				mThread.start();
				mStartMillis = System.currentTimeMillis();
				}
			}

		public void stop() {
			mThread = null;
			}

		@Override
		public void run() {
			while (Thread.currentThread() == mThread) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						int mRecentFrame = -1;
	
						@Override
						public void run() {
							if (mThread != null) {
								long totalDelay = System.currentTimeMillis() - mStartMillis;
								int currentFrame = (int)(totalDelay / mFrameDelay);
								if (mRecentFrame < currentFrame) {
									mRecentFrame = currentFrame;
									mIsUserChange = false;
									animate(currentFrame);
									mIsUserChange = true;
									}
								}
							}
						});
					}
				catch (InvocationTargetException ite) {}
				catch (InterruptedException ie) {}

				try {
					Thread.sleep(mFrameDelay - (System.currentTimeMillis() - mStartMillis) % mFrameDelay);
					}
				catch (InterruptedException ie) {}
				}
			}
		}
	}
