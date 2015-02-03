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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.util.Platform;

public class JCategoryBrowser extends JFilterPanel
				implements ActionListener,ChangeListener,CompoundTableListener,Runnable {
    private static final long serialVersionUID = 0x20060821;

    private static final String DISABLED_ITEM = "<disabled>";

    public static final int cMaxCheckboxCount = 16;

    static private ImageIcon    sIconLeft;
    static private ImageIcon    sIconRight;

    private JComboBox           mComboBox;
    private JSlider             mSlider;
    private JTextField			mTextFieldCategory;
    private JButton             mButtonLeft,mButtonRight,mPressedButton;
    private Thread              mThread;
    private String				mSelectedItem;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
    public JCategoryBrowser(CompoundTableModel tableModel) {
    	this(tableModel, 0);
    	}

    public JCategoryBrowser(CompoundTableModel tableModel, int exclusionFlag) {
		super(tableModel, -1, exclusionFlag, true);

    	setText("Category Browser", Color.black);

        JPanel contentPanel = new JPanel();
        double[][] size = { {4, TableLayout.PREFERRED, 4, TableLayout.FILL, TableLayout.PREFERRED, 4},
                            {TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4} };
        contentPanel.setLayout(new TableLayout(size));
        contentPanel.setOpaque(false);

        contentPanel.add(new JLabel("Column:"), "1,0");
        mComboBox = new JComboBox() {
            private static final long serialVersionUID = 0x20080611;
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = Math.min(72, size.width);
                return size;
                } 
            };
        mComboBox.setEditable(!isActive());
        contentPanel.add(mComboBox, "3,0,4,0");

        if (isActive()) {
	        if (sIconLeft == null)
	            sIconLeft = new ImageIcon(this.getClass().getResource("/images/buttonLeft.png"));
	        if (sIconRight == null)
	            sIconRight = new ImageIcon(this.getClass().getResource("/images/buttonRight.png"));
	
	        JPanel sliderPanel = new JPanel();
	        sliderPanel.setLayout(new BorderLayout());
	
	        mButtonLeft = createButton(sIconLeft);
	        sliderPanel.add(mButtonLeft, BorderLayout.WEST);
	
	        mButtonRight = createButton(sIconRight);
	        sliderPanel.add(mButtonRight, BorderLayout.EAST);
	
	        mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
	        mSlider.setOpaque(false);
	        mSlider.setPreferredSize(new Dimension(100, mSlider.getPreferredSize().height));
	        mSlider.addChangeListener(this);
	        mSlider.setEnabled(false);
	        sliderPanel.add(mSlider, BorderLayout.CENTER);
	        contentPanel.add(sliderPanel, "1,2,4,2");
        	}
        else {
            contentPanel.add(new JLabel("Category:"), "1,2");
            mTextFieldCategory = new JTextField();
            contentPanel.add(mTextFieldCategory, "3,2,4,2");
        	}

        updateComboBox();

        add(contentPanel, BorderLayout.CENTER);

		mIsUserChange = true;
    	}

	private JButton createButton(ImageIcon icon) {
		JButton b = new JButton(icon) {
            private static final long serialVersionUID = 0x20080128;
            public void processMouseEvent(MouseEvent e) {
                super.processMouseEvent(e);
                processButtonMouseEvent(e);
                }
            };
        b.setEnabled(false);
		if (Platform.isMacintosh()) {
//			b.putClientProperty("Quaqua.Component.visualMargin", new Insets(1,1,1,1));
			b.putClientProperty("Quaqua.Button.style", "bevel");
			}
		return b;
		}

	@Override
	protected void animate(int frame) {
		int category = frame % (mSlider.getMaximum() + 1);
        mSlider.setValue(category);
		}

	@Override
    public boolean isFilterEnabled() {
    	return !(mColumnIndex < 0);
    	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBox) {
            mColumnIndex = DISABLED_ITEM.equals(mComboBox.getSelectedItem()) ?
                      -1 : mTableModel.findColumn((String)mComboBox.getSelectedItem());
            if (mColumnIndex == -1)
                disableSlider();
            else
                enableSlider();

            updateExclusion(false);
			return;
			}

		super.actionPerformed(e);
		}

    private void processButtonMouseEvent(MouseEvent e) {
        if (e.getID() == MouseEvent.MOUSE_PRESSED) {
            mPressedButton = (JButton)e.getSource();
            mThread = new Thread(this);
            mThread.start();
            }
        else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
            mThread = null;
            }
        }

	@Override
    public void run() {
        int delay = 500;
        while (Thread.currentThread() == mThread) {
            adaptSlider();
            try {
                Thread.sleep(delay);
                }
            catch (InterruptedException e) {}
            delay = 50;
            }
        }

    private void adaptSlider() {
        int currentValue = mSlider.getValue();
        if (mPressedButton == mButtonLeft) {
            int newValue = mSlider.getMinimum()-1;
            for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                CompoundRecord record = mTableModel.getTotalRecord(row);
                if (mTableModel.isVisibleNeglecting(record, mExclusionFlag)) {
                    int category = mTableModel.getCategoryIndex(mColumnIndex, record);
                    if (category < currentValue && category > newValue)
                        newValue = category;
                    }
                }
            if (newValue >= mSlider.getMinimum())
                mSlider.setValue(newValue);
            }
        else if (mPressedButton == mButtonRight) {
            int newValue = mSlider.getMaximum()+1;
            for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                CompoundRecord record = mTableModel.getTotalRecord(row);
                if (mTableModel.isVisibleNeglecting(record, mExclusionFlag)) {
                    int category = mTableModel.getCategoryIndex(mColumnIndex, record);
                    if (category > currentValue && category < newValue)
                        newValue = category;
                    }
                }
            if (newValue <= mSlider.getMaximum())
                mSlider.setValue(newValue);
            }
        }

	@Override
    public void stateChanged(ChangeEvent e) {
        updateExclusion(((JSlider)e.getSource()).getValueIsAdjusting());
        }

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows) {
			updateComboBox();
            updateExclusion(false);
			}
        else if (e.getType() == CompoundTableEvent.cAddColumns) {
            updateComboBox();
            }
        else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
            int oldColumnIndex = mColumnIndex;
            mColumnIndex = (mColumnIndex == -1) ? -1 : e.getMapping()[mColumnIndex];
            updateComboBox();
            if (oldColumnIndex != -1 && mColumnIndex == -1)
                updateExclusion(false);
            }
        else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
            updateComboBox();
            }
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int columnIndex = mColumnIndex;
			updateComboBox();	// may set mColumnIndex to -1 if column type changes
			if (e.getSpecifier() == columnIndex) {
				if (mColumnIndex != -1) {
					mSlider.setMaximum(mTableModel.getCategoryCount(mColumnIndex)-1);
					String[] categoryList = mTableModel.getCategoryList(mColumnIndex);
					boolean found = false;
					for (int i=0; i<categoryList.length; i++) {
						if (categoryList[i].equals(mSelectedItem)) {
							mSlider.setValue(i);
							found = true;
							break;
							}
						}
					if (!found)
						mSlider.setValue(0);
					}
	            updateExclusion(false);
				}
			}

		mIsUserChange = true;
		}

	@Override
	public void removePanel() {
		mTableModel.removeCompoundTableListener(this);
		super.removePanel();
		}

	@Override
	public String getInnerSettings() {
		if (DISABLED_ITEM.equals(mComboBox.getSelectedItem()))
			return "<disabled>";
		String value = isActive() ? mTableModel.getCategoryList(mColumnIndex)[mSlider.getValue()]
								  : mTextFieldCategory.getText();
		return (String)mComboBox.getSelectedItem()+"\t"+value;
		}

	@Override
	public void applyInnerSettings(String settings) {
        mColumnIndex = -1;
        if (settings.equals("<disabled>")) {
        	setInverse(false);
            }
        else {
			int index = settings.indexOf('\t');
			if (index != -1) {
				String columnName = settings.substring(0, index);
				String category = settings.substring(index+1);
				if (isActive()) {
					int column = mTableModel.findColumn(columnName);
					if (column != -1) {
						int value = mTableModel.getCategoryIndex(column, category);
						if (value == -1 || value > mSlider.getMaximum())
							value = 0;

						mComboBox.setSelectedItem(columnName);
	                    mSlider.setValue(value);
	                    mSlider.setEnabled(true);
	                    }
					}
				else {
                    mComboBox.setSelectedItem(columnName);
					mTextFieldCategory.setText(settings.substring(index+1));
					}
				}
			}
        updateExclusion(false);
		}

	@Override
	public void innerReset() {
        if (!DISABLED_ITEM.equals(mComboBox.getSelectedItem())) {
            mComboBox.setSelectedItem(DISABLED_ITEM);
            mSlider.setValue(0);
            updateExclusion(false);
            }
		}

    private void updateComboBox() {
        mComboBox.removeActionListener(this);
        mComboBox.removeAllItems();
        mComboBox.addItem(DISABLED_ITEM);
        mComboBox.setSelectedItem(DISABLED_ITEM);
        String[] categoryList = getCategoryList();
        for (int i=0; i<categoryList.length; i++) {
            mComboBox.addItem(categoryList[i]);
            if (mTableModel.findColumn(categoryList[i]) == mColumnIndex)
                mComboBox.setSelectedIndex(i+1);
            }
        mComboBox.addActionListener(this);

        if (DISABLED_ITEM.equals(mComboBox.getSelectedItem())) {
            mColumnIndex = -1;
            disableSlider();
            }
        else {
            enableSlider();
            }
        }

    private void disableSlider() {
    	if (isActive()) {
	        mSlider.removeChangeListener(this);
	        mSlider.setValue(0);
	        mSlider.setEnabled(false);
	        mButtonLeft.setEnabled(false);
	        mButtonRight.setEnabled(false);
    		}
        }

    private void enableSlider() {
    	if (isActive()) {
	        mSlider.setMaximum(mTableModel.getCategoryCount(mColumnIndex)-1);
	        mSlider.setEnabled(true);
	        mSlider.addChangeListener(this);
	        mButtonLeft.setEnabled(true);
	        mButtonRight.setEnabled(true);
    		}
        }

    private String[] getCategoryList() {
        ArrayList<String> categoryColumnList = new ArrayList<String>();
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
            if (mTableModel.isColumnTypeCategory(column))
                categoryColumnList.add(mTableModel.getColumnTitle(column));
        return categoryColumnList.toArray(new String[0]);
        }

    private void updateExclusion(boolean isAdjusting) {
    	if (!isActive())
    		return;

    	if (mColumnIndex < 0) {
        	mSelectedItem = null;
            setText("Category Browser", Color.black);

    		if (isActive())
    			mTableModel.clearCompoundFlag(mExclusionFlag);
            }
        else {
        	mSelectedItem = mTableModel.getCategoryList(mColumnIndex)[mSlider.getValue()];
            if (mTableModel.getColumnSpecialType(mColumnIndex) != null)
                setText(mTableModel.getColumnTitle(mColumnIndex)+" Group: "+(mSlider.getValue()+1), Color.red);
            else
                setText(mSelectedItem, Color.red);
            boolean[] selected = new boolean[mTableModel.getCategoryCount(mColumnIndex)];
            selected[mSlider.getValue()] = true;

            if (isActive())
    			mTableModel.setCategoryExclusion(mExclusionFlag, mColumnIndex, selected, isInverse());
            }

    	fireFilterChanged(FilterEvent.FILTER_UPDATED, isAdjusting);
        }
	}
