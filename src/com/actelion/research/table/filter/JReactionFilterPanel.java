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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorHandlerFFP512;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.gui.StructureListener;
import com.actelion.research.gui.table.ChemistryRenderPanel;
import com.actelion.research.table.CompoundTableModel;

public class JReactionFilterPanel extends JFilterPanel
				implements ChangeListener,ItemListener,MouseListener,StructureListener {
    private static final long serialVersionUID = 0x20061002;

    private static final String	cFilterBySubstructure = "#substructure#";
	private static final String	cFilterBySimilarity = "#similarity#";

    private static final String cItemContains = "contains";
    private static final String cItemIsSimilarTo = "is similar to";
    private static final String cItemDisabled = "<disabled>";

	private Frame               mParentFrame;
	private ChemistryRenderPanel mReactionView;
    private Reaction            mReaction;
	private JComboBox           mComboBox;
	private JSlider			    mSimilaritySlider;
	private float[]             mSimilarity;
    private int[]               mDescriptorColumn;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param parent
	 * @param tableModel
	 * @param rxn
	 */
    public JReactionFilterPanel(Frame parent, CompoundTableModel tableModel, Reaction rxn) {
    	this(parent, tableModel, -1, 0, rxn);
    	}

    public JReactionFilterPanel(Frame parent, CompoundTableModel tableModel, int column, int exclusionFlag, Reaction rxn) {
		super(tableModel, column, exclusionFlag, false);
		mParentFrame = parent;

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JPanel p1 = new JPanel();

		mComboBox = new JComboBox();
        populateCompoBox();
		mComboBox.addItemListener(this);
		p1.add(mComboBox);

		p.add(p1, BorderLayout.NORTH);

		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(0), new JLabel("0"));
		labels.put(new Integer(50), new JLabel("\u00BD"));
		labels.put(new Integer(100), new JLabel("1"));
		mSimilaritySlider = new JSlider(JSlider.VERTICAL, 0, 100, 80);
		mSimilaritySlider.setMinorTickSpacing(10);
		mSimilaritySlider.setMajorTickSpacing(100);
		mSimilaritySlider.setLabelTable(labels);
		mSimilaritySlider.setPaintLabels(true);
		mSimilaritySlider.setPaintTicks(true);
		mSimilaritySlider.setEnabled(false);
		mSimilaritySlider.setPreferredSize(new Dimension(42, 100));
		mSimilaritySlider.addChangeListener(this);
		add(mSimilaritySlider, BorderLayout.EAST);

        if (rxn == null) {
    		mReaction = new Reaction();
    		mReactionView = new ChemistryRenderPanel();
            mReactionView.setChemistry(mReaction);
            }
        else {
        	mReaction = rxn;
    		mReactionView = new ChemistryRenderPanel();
    		mComboBox.setSelectedIndex(1);
	    	updateExclusion();
            }

//        mReactionView.setClipboardHandler(new ClipboardHandler());
        mReactionView.setMinimumSize(new Dimension(100, 100));
        mReactionView.setPreferredSize(new Dimension(100, 100));
        mReactionView.setBackground(getBackground());
//        mReactionView.setEmptyMoleculeMessage("<double-click to edit>");
//        mReactionView.addStructureListener(this);
        mReactionView.addMouseListener(this);
		p.add(mReactionView, BorderLayout.CENTER);

		add(p, BorderLayout.CENTER);

		mIsUserChange = true;
    	}

    private void populateCompoBox() {
        int fingerprintColumn = -1;
        int descriptorCount = 0;
        for (int column=0; column<mTableModel.getColumnCount(); column++) {
            if (mTableModel.isDescriptorColumn(column)
             && mTableModel.getParentColumn(column) == mColumnIndex) {
                descriptorCount++;
                if (mTableModel.getDescriptorHandler(column) instanceof DescriptorHandlerFFP512)
                    fingerprintColumn = column;
                }
            }

        mComboBox.removeAllItems();
        mDescriptorColumn = new int[descriptorCount+((fingerprintColumn == -1) ? 0:1)];
        int descriptorIndex = 0;
        if (fingerprintColumn != -1) {
            mDescriptorColumn[0] = fingerprintColumn;
            mComboBox.addItem(cItemContains);
            descriptorIndex++;
            }
        for (int column=0; column<mTableModel.getColumnCount(); column++) {
            if (mTableModel.isDescriptorColumn(column)
             && mTableModel.getParentColumn(column) == mColumnIndex) {
                mDescriptorColumn[descriptorIndex++] = column;
                mComboBox.addItem(descriptorToItem(mTableModel.getDescriptorHandler(column).getInfo().shortName));
                }
            }
        mComboBox.addItem(cItemDisabled);
        }

    public boolean isFilterEnabled() {
    	return !cItemDisabled.equals(mComboBox.getSelectedItem());
    	}

    public void mouseClicked(MouseEvent e) {
/*	    if (e.getClickCount() == 2) {
			JDrawDialog theDialog = new JDrawDialog(mParentFrame, mStructureView.getMolecule());
	    	theDialog.addStructureListener(mStructureView);
		    theDialog.setVisible(true);
	    	}
*/	    }

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void itemStateChanged(ItemEvent e) {
/*		if (e.getSource() == mComboBox && e.getStateChange() == ItemEvent.SELECTED) {
            String item = (String)mComboBox.getSelectedItem();
			if (item.equals(cFilterBySubstructure)) {
				mSimilaritySlider.setEnabled(false);
				mStructureView.getMolecule().setFragment(true);
                }
            else if (item.equals(cItemDisabled)) {
                mSimilaritySlider.setEnabled(false);
                mStructureView.getMolecule().setFragment(true);
                }
            else {  // similarity
				mSimilaritySlider.setEnabled(true);
				mStructureView.getMolecule().setFragment(false);
				}
		    mStructureView.structureChanged();
		    }
*/		}

	public void stateChanged(ChangeEvent e) {
		updateExclusion();
		}

	public synchronized void structureChanged(StereoMolecule mol) {
		mSimilarity = null;
		mol.removeAtomSelection();
		if (!mol.isFragment())
			mComboBox.setSelectedIndex(1);
		updateExclusion();
		}

	public void updateExclusion() {
/*        String item = (String)mComboBox.getSelectedItem();
		if (!item.equals(cItemDisabled)
         && !mTableModel.isColumnDataAvailable(mDescriptorColumn[mComboBox.getSelectedIndex()])
		 && mStructureView.getMolecule().getAllAtoms() != 0) {
			mComboBox.setSelectedItem(cItemDisabled);
			JOptionPane.showMessageDialog(mParentFrame, "A structure filter cannot be used before the appropriate indexation has completed.");
			}

		if (mStructureView.getMolecule().getAllAtoms() == 0)
			mTableModel.clearCompoundFlag(mExclusionMask);
		else {
			if (item.equals(cItemContains)) {
				mTableModel.setSubStructureExclusion(mExclusionMask, mColumnIndex, mStructureView.getMolecule(), mInverse);
                }
            else if (item.equals(cItemDisabled)) {
                mTableModel.clearCompoundFlag(mExclusionMask);
                }
            else {
				if (mSimilarity == null)
					mSimilarity = mTableModel.createSimilarityList(mStructureView.getMolecule(),
			                            mDescriptorColumn[mComboBox.getSelectedIndex()]);

				mTableModel.setSimilarityExclusion(mExclusionMask,
				        mDescriptorColumn[mComboBox.getSelectedIndex()], mSimilarity,
					    (float)mSimilaritySlider.getValue() / (float)100.0, mInverse,
						mSimilaritySlider.getValueIsAdjusting());
				}
		    }
    	fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
*/		}

	@Override
	public String getInnerSettings() {
/*		if (mStructureView.getMolecule().getAllAtoms() != 0
		 && mComboBox.getSelectedIndex() != mComboBox.getItemCount()-1) {
			StereoMolecule mol = mStructureView.getMolecule();
			String idcode = new Canonizer(mol).getIDCode();
            String item = (String)mComboBox.getSelectedItem();
			if (item.equals(cItemContains))
				settings = attachSetting(settings, cFilterBySubstructure);
			else
				settings = attachSetting(settings, itemToDescriptor(item)+"\t"+mSimilaritySlider.getValue());

			settings = attachSetting(settings, idcode);
			}
*/		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
/*		if (settings != null) {
            String item = (String)mComboBox.getSelectedItem();
			if (settings.startsWith(cFilterBySubstructure)) {
				String idcode = settings.substring(cFilterBySubstructure.length()+1);
				mStructureView.setIDCode(idcode);
				if (mComboBox.getSelectedIndex() != 0)
					mComboBox.setSelectedIndex(0);
				else
					updateExclusion();
				}
			else {
                int index1 = settings.indexOf('\t');
				int index2 = settings.indexOf('\t', index1+1);
                String descriptor = settings.substring(index1);

                    // to be compatible with format prior V2.7.0
                if (descriptor.equals(cFilterBySimilarity))
                    descriptor = new DescriptorHandlerFP512().getShortName();

                int similarity = Integer.parseInt(settings.substring(index1+1, index2));
				mSimilaritySlider.setValue(similarity);
				String idcode = settings.substring(index2+1);
				mStructureView.setIDCode(idcode);
				if (!item.equals(descriptorToItem(descriptor)))
					mComboBox.setSelectedItem(descriptorToItem(descriptor));
				else
					updateExclusion();
				}
			}
*/
		}

    private String descriptorToItem(String descriptor) {
        return cItemIsSimilarTo+" ["+descriptor+"]";
        }

    private String itemToDescriptor(String item) {
        return item.substring(cItemIsSimilarTo.length()+2, item.length()-1);
        }

	@Override
	public void innerReset() {
/*		if (mStructureView.getMolecule().getAllAtoms() != 0
         && mComboBox.getSelectedIndex() != mComboBox.getItemCount()-1)
			mComboBox.setSelectedIndex(mComboBox.getItemCount()-1);
*/
    	}
	}
