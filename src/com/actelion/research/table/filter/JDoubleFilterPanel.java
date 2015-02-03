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
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.gui.JPruningBar;
import com.actelion.research.gui.PruningBarEvent;
import com.actelion.research.gui.PruningBarListener;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;

public class JDoubleFilterPanel extends JFilterPanel
				implements ActionListener,CompoundTableListener,FocusListener,KeyListener,PruningBarListener {
	private static final long serialVersionUID = 0x20060904;

	private JPruningBar	mPruningBar;
	private JTextField  mLabelLow,mLabelHigh;
	private boolean		mIsLogarithmic;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
	public JDoubleFilterPanel(CompoundTableModel tableModel) {
		this(tableModel, 0, 0);
		}

	public JDoubleFilterPanel(CompoundTableModel tableModel, int column, int exclusionFlag) {
		super(tableModel, column, exclusionFlag, false);

		if (isActive()) {
			tableModel.initializeDoubleExclusion(mExclusionFlag, column);
	
			mPruningBar = new JPruningBar(mTableModel.getMinimumValue(mColumnIndex),
										  mTableModel.getMaximumValue(mColumnIndex), true, 0);
			mPruningBar.setUseRedColor(!mTableModel.isColumnDataComplete(mColumnIndex) && !isInverse());
			add(mPruningBar, BorderLayout.CENTER);
			mPruningBar.addPruningBarListener(this);
	
			mIsLogarithmic = mTableModel.isLogarithmicViewMode(mColumnIndex);
	
			JPanel p = new JPanel();
			p.setOpaque(false);
			p.setLayout(new GridLayout(1,2));
			mLabelLow = new JTextField();
			mLabelLow.setOpaque(false);
			mLabelLow.setBorder(BorderFactory.createEmptyBorder());
			mLabelLow.addFocusListener(this);
			mLabelHigh = new JTextField();
			mLabelHigh.setOpaque(false);
			mLabelHigh.setBorder(BorderFactory.createEmptyBorder());
			mLabelHigh.setHorizontalAlignment(JTextField.RIGHT);
			mLabelHigh.addFocusListener(this);
			p.add(mLabelLow);
			p.add(mLabelHigh);
			add(p, BorderLayout.SOUTH);
	
			setLabelTextFromPruningBars();
	
			if ((!mTableModel.isColumnTypeDate(mColumnIndex)
			  && !mTableModel.isColumnTypeRangeCategory(mColumnIndex))) {
				mLabelLow.addActionListener(this);
				mLabelHigh.addActionListener(this);
				}
			}
		else {
			JPanel p = new JPanel();
			p.setOpaque(false);
			p.setLayout(new GridLayout(2,2,4,4));
			mLabelLow = new JTextField();
			mLabelLow.addKeyListener(this);
			mLabelHigh = new JTextField();
			mLabelHigh.addKeyListener(this);
			p.add(new JLabel("Low value:"));
			p.add(mLabelLow);
			p.add(new JLabel("High value:"));
			p.add(mLabelHigh);
			add(p, BorderLayout.SOUTH);
			}

		mIsUserChange = true;
		}

	@Override
	public void pruningBarChanged(PruningBarEvent e) {
		updateExclusion(e.isAdjusting());
		}

	@Override
	public boolean isFilterEnabled() {
		return true;
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);

		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows
		 || (e.getType() == CompoundTableEvent.cChangeColumnData && e.getSpecifier() == mColumnIndex)) {
			if (!mTableModel.isColumnTypeDouble(mColumnIndex)
			 || mTableModel.getMinimumValue(mColumnIndex) == mTableModel.getMaximumValue(mColumnIndex)) {
				removePanel();
				return;
				}

			if (mPruningBar.getMinimumValue() != mTableModel.getMinimumValue(mColumnIndex)
			 || mPruningBar.getMaximumValue() != mTableModel.getMaximumValue(mColumnIndex)) {
				float low = mPruningBar.getLowValue();
				float high = mPruningBar.getHighValue();
				boolean lowIsMin = (low == mPruningBar.getMinimumValue());
				boolean highIsMax = (high == mPruningBar.getMaximumValue());

				mPruningBar.setMinAndMax(mTableModel.getMinimumValue(mColumnIndex),
										 mTableModel.getMaximumValue(mColumnIndex));

				if (!lowIsMin || !highIsMax) {
					if (mIsLogarithmic != mTableModel.isLogarithmicViewMode(mColumnIndex)) {
						if (lowIsMin)	// don't calculate in this case to prevent arithmetic discrepancies
							low = mTableModel.getMinimumValue(mColumnIndex);
						else if (mIsLogarithmic)
							low = (float)Math.pow(10.0, low);
						else
							low = (float)Math.log10(low);

						if (highIsMax)	// don't calculate in this case to prevent arithmetic discrepancies
							high = mTableModel.getMaximumValue(mColumnIndex);
						else if (mIsLogarithmic)
							high = (float)Math.pow(10.0, high);
						else
							high = (float)Math.log10(high);
	
						mIsLogarithmic = !mIsLogarithmic;
						}
					mPruningBar.setLowAndHigh(low, high, false);
					}

				setLabelTextFromPruningBars();
				}

			updateExclusion(false);
			mPruningBar.setUseRedColor(!mTableModel.isColumnDataComplete(mColumnIndex) && !isInverse());
			}

		mIsUserChange = true;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (e.getSource() == mLabelLow || e.getSource() == mLabelHigh) {
			labelUpdated((JTextField)e.getSource());
			}
		else if (isActive()) {
			mPruningBar.setUseRedColor(!mTableModel.isColumnDataComplete(mColumnIndex) && !isInverse());
			}
		}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {
		float low = Float.NaN;
		float high = Float.NaN;
		try { low = Float.parseFloat(mLabelLow.getText()); } catch (NumberFormatException nfe) {}
		try { high = Float.parseFloat(mLabelHigh.getText()); } catch (NumberFormatException nfe) {}
		JTextField source = (JTextField)e.getSource();
		boolean isError = (source.getText().length() != 0
						&& (Float.isNaN((source == mLabelLow) ? low : high)
						 || (!Float.isNaN(low) && !Float.isNaN(high) && low >= high)));
		source.setBackground(isError ? Color.RED : Color.WHITE);
		}

	private void labelUpdated(JTextField source) {
		String error = null;
		try {
			float value = Float.parseFloat(source.getText());

			if (mTableModel.isLogarithmicViewMode(mColumnIndex))
				value = (float)Math.log10(value);

			// used to compensate the rounding problem
			float uncertainty = (mPruningBar.getMaximumValue() - mPruningBar.getMinimumValue()) / 100000;

			if (source == mLabelLow
			 && value >= mPruningBar.getMinimumValue() - uncertainty
			 && value <= mPruningBar.getHighValue() + uncertainty) {
				if (value < mPruningBar.getMinimumValue())
					value = mPruningBar.getMinimumValue();
				if (value > mPruningBar.getHighValue())
					value = mPruningBar.getHighValue();
				mPruningBar.setLowValue(value);
				}
			else if (source == mLabelHigh
				  && value <= mPruningBar.getMaximumValue() + uncertainty
				  && value >= mPruningBar.getLowValue() - uncertainty) {
				if (value > mPruningBar.getMaximumValue())
					value = mPruningBar.getMaximumValue();
				if (value < mPruningBar.getLowValue())
					value = mPruningBar.getLowValue();
				mPruningBar.setHighValue(value);
				}
			else {
				error = "out of range";
				}
			}
		catch (NumberFormatException nfe) {
			error = "not a number";
			}

		if (error != null) {
			source.setText(error);
			validate();
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(1000);
						}
					catch (InterruptedException ie) {}
					setLabelTextFromPruningBars();
					}
				}).start();
			}
		}

	private void setLabelTextFromPruningBars() {
		if (mTableModel.isColumnTypeDate(mColumnIndex)) {
			DateFormat df = DateFormat.getDateInstance();
			mLabelLow.setText(df.format(new Date(86400000*(long)(1.0+mPruningBar.getLowValue()))));
			mLabelHigh.setText(df.format(new Date(86400000*(long)mPruningBar.getHighValue())));
			}
		else if (mTableModel.isColumnTypeRangeCategory(mColumnIndex)) {
			String[] categoryList = mTableModel.getCategoryList(mColumnIndex);
			int low = (int)(mPruningBar.getLowValue()+0.49999);
			int high = (int)(mPruningBar.getHighValue()-0.49999);
			mLabelLow.setText((low >= categoryList.length) ? "" : categoryList[low]);
			mLabelHigh.setText((high < 0) ? "" : categoryList[high]);
			}
		else {	// double values
			if (mTableModel.isLogarithmicViewMode(mColumnIndex)) {
				mLabelLow.setText(DoubleFormat.toString(Math.pow(10.0, mPruningBar.getLowValue())));
				mLabelHigh.setText(DoubleFormat.toString(Math.pow(10.0, mPruningBar.getHighValue())));
				}
			else {
				mLabelLow.setText(DoubleFormat.toString(mPruningBar.getLowValue()));
				mLabelHigh.setText(DoubleFormat.toString(mPruningBar.getHighValue()));
				}
			}
		validate();
		}

	@Override
	public void focusGained(FocusEvent e) {}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource() == mLabelLow || e.getSource() == mLabelHigh) {
			labelUpdated((JTextField)e.getSource());
			}
		}
	
	private void updateExclusion(boolean isAdjusting) {
		setLabelTextFromPruningBars();
		float low  = (float)mPruningBar.getLowValue();
		float high = (float)mPruningBar.getHighValue();

		boolean isUserChange = mIsUserChange;
		// setDoubleExclusion causes CompoundTableEvents that interfer with the userchange flag
		// (this is a hack. One might alterntively increment and decrement a userChangeInteger instead of setting a flag to count nested events)

		mTableModel.setDoubleExclusion(mColumnIndex, mExclusionFlag, low, high, isInverse(), isAdjusting);
		mIsUserChange = isUserChange;
		fireFilterChanged(FilterEvent.FILTER_UPDATED, isAdjusting);
		}

	@Override
	public void removePanel() {
		mTableModel.removeCompoundTableListener(this);
		super.removePanel();
		}

	@Override
	public String getInnerSettings() {
		if (isActive()) {
			if (mPruningBar.getLowValue() > mPruningBar.getMinimumValue()
			 || mPruningBar.getHighValue() < mPruningBar.getMaximumValue())
				return Float.toString(mPruningBar.getLowValue())+'\t'+Float.toString(mPruningBar.getHighValue());
			}
		else {
			String low = mLabelLow.getText();
			String high = mLabelHigh.getText();
			if (low.length() != 0)
				try { Float.parseFloat(low); } catch (NumberFormatException nfe) { low = ""; }
			if (high.length() != 0)
				try { Float.parseFloat(high); } catch (NumberFormatException nfe) { high = ""; }
			if (low.length() != 0 || high.length() != 0)
				return low+'\t'+high;
			}

		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (settings != null) {
			int index = settings.indexOf('\t');
			if (index != -1) {
				if (isActive()) {
					mPruningBar.setLowValue(Float.parseFloat(settings.substring(0, index)));
					mPruningBar.setHighValue(Float.parseFloat(settings.substring(index+1)));
					}
				else {
					mLabelLow.setText(settings.substring(0, index));
					mLabelHigh.setText(settings.substring(index+1));
					}
				}
			}
		}

	@Override
	public void innerReset() {
		if (!mTableModel.isColumnDataComplete(mColumnIndex) && isActive())
			removePanel();

		mPruningBar.reset();
		}
	}
