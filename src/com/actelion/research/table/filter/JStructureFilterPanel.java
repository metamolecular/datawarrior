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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableModel;

public abstract class JStructureFilterPanel extends JFilterPanel 
				implements ChangeListener,DescriptorConstants,ItemListener {
    private static final long serialVersionUID = 0x20060925;

    protected static final String cFilterBySubstructure = "#substructure#";
    protected static final String cFilterBySimilarity = "#similarity#";
    protected static final String cFilterDisabled = "#disabled#";

    protected static final String cItemContains = "contains";
    protected static final String cItemIsSimilarTo = "is similar to";
    protected static final String cItemDisabled = "<disabled>";

    protected float[][]	mSimilarity;
    protected int[]		mDescriptorColumn;
    protected JComboBox	mComboBox;

    private Frame		mParentFrame;
    private JSlider		mSimilaritySlider;
    private int			mCurrentDescriptorColumn;

    public JStructureFilterPanel(Frame parent, CompoundTableModel tableModel, int column, int exclusionFlag) {
		super(tableModel, column, exclusionFlag, false);
		mParentFrame = parent;
		mCurrentDescriptorColumn = -1;
		}

    protected JSlider getSimilaritySlider() {
    	if (mSimilaritySlider == null) {
			Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
			labels.put(new Integer(0), new JLabel("0"));
			labels.put(new Integer(50), new JLabel("\u00BD"));
			labels.put(new Integer(100), new JLabel("1"));
			mSimilaritySlider = new JSlider(JSlider.VERTICAL, 0, 100, 80);
			mSimilaritySlider.setOpaque(false);
			mSimilaritySlider.setMinorTickSpacing(10);
			mSimilaritySlider.setMajorTickSpacing(100);
			mSimilaritySlider.setLabelTable(labels);
			mSimilaritySlider.setPaintLabels(true);
			mSimilaritySlider.setPaintTicks(true);
			mSimilaritySlider.setPreferredSize(new Dimension(42, 100));
			mSimilaritySlider.addChangeListener(this);
    		}
		return mSimilaritySlider;
    	}

	@Override
	public void innerReset() {
		if (getStructureCount() != 0
         && mComboBox.getSelectedIndex() != mComboBox.getItemCount()-1)
			mComboBox.setSelectedIndex(mComboBox.getItemCount()-1);
		}

    public boolean isFilterEnabled() {
    	return !(cItemDisabled.equals(mComboBox.getSelectedItem()) || getStructureCount() == 0);
    	}

    public void compoundTableChanged(CompoundTableEvent e) {
        super.compoundTableChanged(e);
        if (mColumnIndex == -1) // filter was already removed by super
            return;

		mIsUserChange = false;

        if (e.getType() == CompoundTableEvent.cAddColumns) {
            updateComboBox((String)mComboBox.getSelectedItem());
        	}
        else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
        	// correct column mapping of mColumnIndex is done by JFilterPanel

        	if (!mTableModel.hasDescriptorColumn(mColumnIndex)) {
            	removePanel();
            	return;
            	}

        	String selectedItem = (String)mComboBox.getSelectedItem();
            updateComboBox(selectedItem);
            if (!selectedItem.equals(mComboBox.getSelectedItem()))
            	updateExclusion();
            }
        if (e.getType() == CompoundTableEvent.cAddRows
         || (e.getType() == CompoundTableEvent.cChangeColumnData && e.getSpecifier() == mColumnIndex)) {
        	mSimilarity = null;	// TODO keep old values and calculate changes only
            updateExclusion();
            }
        else if (e.getType() == CompoundTableEvent.cDeleteRows && mSimilarity != null) {
            int[] rowMapping = e.getMapping();
            float[][] newSimilarity = new float[mSimilarity.length][rowMapping.length];
            for (int i=0; i<mSimilarity.length; i++)
            	for (int j=0; j<rowMapping.length; j++)
            		newSimilarity[i][j] = mSimilarity[i][rowMapping[j]];
            mSimilarity = newSimilarity;
            }

		mIsUserChange = true;
    	}

    @Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBox
		 && e.getStateChange() == ItemEvent.SELECTED) {
			int newDescriptorColumn = mDescriptorColumn[mComboBox.getSelectedIndex()];
			if (newDescriptorColumn != -1
			 && newDescriptorColumn != mCurrentDescriptorColumn) {
				mCurrentDescriptorColumn = -1;
				mSimilarity = null;	// TODO cache descriptors
				}
			}
		}

    protected void updateComboBox(String selectedItem) {
        mComboBox.removeItemListener(this);
        mComboBox.removeAllItems();

        if (isActive()) {
	        int fingerprintColumn = mTableModel.getChildColumn(mColumnIndex, DESCRIPTOR_FFP512.shortName);
	        int descriptorCount = 0;
	        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
	            if (mTableModel.isDescriptorColumn(column)
	             && mTableModel.getParentColumn(column) == mColumnIndex)
	                descriptorCount++;
	
	        mDescriptorColumn = new int[((supportsSSS() && fingerprintColumn != -1) ? 1 : 0)
	                                   + (supportsSim() ? descriptorCount : 0) + 1];
	        int itemIndex = 0;
	        if (supportsSSS() && fingerprintColumn != -1) {
	            mDescriptorColumn[itemIndex++] = fingerprintColumn;
	            mComboBox.addItem(cItemContains);
	            }
	        if (supportsSim()) {
		        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
		            if (mTableModel.isDescriptorColumn(column)
		             && mTableModel.getParentColumn(column) == mColumnIndex) {
		                mDescriptorColumn[itemIndex++] = column;
		                mComboBox.addItem(descriptorToItem(mTableModel.getColumnSpecialType(column)));
		                }
		        	}
	            }
	        mComboBox.addItem(cItemDisabled);
	        mDescriptorColumn[itemIndex] = -1;
	        mComboBox.setSelectedIndex(itemIndex);	// <disabled> if selectedItem is not available option
        	}
        else {
	        mDescriptorColumn = new int[(supportsSSS() ? 1 : 0)
	                                  + (supportsSim() ? DescriptorConstants.DESCRIPTOR_LIST.length : 0) + 1];
	        if (supportsSSS())
	            mComboBox.addItem(cItemContains);
	        if (supportsSim())
	        	for (int i=0; i<DescriptorConstants.DESCRIPTOR_LIST.length; i++)
	                mComboBox.addItem(descriptorToItem(DescriptorConstants.DESCRIPTOR_LIST[i].shortName));
	        mComboBox.addItem(cItemDisabled);
        	}

        if (selectedItem.equals(cItemIsSimilarTo)) {
        	for (int i=0; i<mComboBox.getItemCount(); i++) {
        		if (((String)mComboBox.getItemAt(i)).startsWith(cItemIsSimilarTo)) {
        			mComboBox.setSelectedIndex(i);
        			break;
        			}
        		}
        	}
        else {
        	mComboBox.setSelectedItem(selectedItem);
        	}
        mComboBox.addItemListener(this);
        }

	public void stateChanged(ChangeEvent e) {
		updateExclusion();
		}

	public void updateExclusion() {
		if (!isActive())
			return;

		if (!((String)mComboBox.getSelectedItem()).equals(cItemDisabled)
         && !mTableModel.isDescriptorAvailable(mDescriptorColumn[mComboBox.getSelectedIndex()])
		 && getStructureCount() != 0) {
			mComboBox.setSelectedItem(cItemDisabled);
			JOptionPane.showMessageDialog(mParentFrame, "A structure filter cannot be applied and was set to <disabled>,\n" +
			                                            "because the descriptor calculation has not finished yet.");
			}

		if (getStructureCount() == 0)
			mTableModel.clearCompoundFlag(mExclusionFlag);
		else {
			if (((String)mComboBox.getSelectedItem()).equals(cItemContains)) {
				mTableModel.setSubStructureExclusion(mExclusionFlag, mColumnIndex, getStructures(), isInverse());
                }
            else if (((String)mComboBox.getSelectedItem()).equals(cItemDisabled)) {
                mTableModel.clearCompoundFlag(mExclusionFlag);
                }
            else {
                int descriptorColumn = mDescriptorColumn[mComboBox.getSelectedIndex()];

                if (mSimilarity == null)
                	mSimilarity = new float[getStructureCount()][];

            	for (int i=0; i<getStructureCount(); i++) {
            		if (mSimilarity[i] == null) {
            			mSimilarity[i] = createSimilarityList(getStructure(i), descriptorColumn);
            			mCurrentDescriptorColumn = descriptorColumn;

	                    if (mSimilarity[i] == null) {	// user cancelled SMP dialog
	                    	mSimilarity = null;
	                    	break;
	                    	}
            			}
                    }

                if (mSimilarity == null) {
                    mComboBox.setSelectedItem(cItemDisabled);	// treat this as user change, since the user actively cancelled
                    }
                else {
    				mTableModel.setSimilarityExclusion(mExclusionFlag,
    				        mDescriptorColumn[mComboBox.getSelectedIndex()], getStructures(), mSimilarity,
    					    (float)mSimilaritySlider.getValue() / (float)100.0, isInverse(),
    						mSimilaritySlider.getValueIsAdjusting());
                    }
				}
		    }

		fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
		}

	protected float[] createSimilarityList(StereoMolecule mol, int descriptorColumn) {
        return (DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn))
        	 || mTableModel.getTotalRowCount() > 400000) ?

        	// if we have the slow 3DPPMM2 then use a progress dialog
        	createSimilarityListSMP(mol, descriptorColumn)

            // else calculate similarity list in event dispatcher thread
        	: mTableModel.createSimilarityList(mol, descriptorColumn);
		}

	protected abstract boolean supportsSSS();
	protected abstract boolean supportsSim();
	protected abstract int getStructureCount();
	protected abstract StereoMolecule getStructure(int i);

	private StereoMolecule[] getStructures() {
		StereoMolecule[] structures = new StereoMolecule[getStructureCount()];
		for (int i=0; i<structures.length; i++)
			structures[i] = getStructure(i);
		return structures;
		}

	private float[] createSimilarityListSMP(Object chemObject, int descriptorColumn) {
        JProgressDialog progressDialog = new JProgressDialog(mParentFrame) {
            private static final long serialVersionUID = 0x20110325;

            public void stopProgress() {
        		super.stopProgress();
        		close();
        		}
        	};

       	mTableModel.createSimilarityListSMP(chemObject, null, descriptorColumn, progressDialog);
       	progressDialog.setVisible(true);

    	return mTableModel.getSimilarityListSMP();
 		}

    protected String descriptorToItem(String descriptor) {
        return cItemIsSimilarTo+" ["+descriptor+"]";
        }

    protected String itemToDescriptor(String item) {
        return item.substring(cItemIsSimilarTo.length()+2, item.length()-1);
        }
    }
