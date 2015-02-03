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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ItemEvent;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.gui.CompoundCollectionListener;
import com.actelion.research.gui.CompoundCollectionModel;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.CompoundTableModel;

public class JMultiStructureFilterPanel extends JStructureFilterPanel 
				implements CompoundCollectionListener,DescriptorConstants {
    private static final long serialVersionUID = 0x20110519;

	private CompoundCollectionPane<String> mStructurePane;
	private boolean mIsSSS;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param parent
	 * @param tableModel
	 * @param isSSS
	 */
    public JMultiStructureFilterPanel(Frame parent, CompoundTableModel tableModel, boolean isSSS) {
    	this(parent, tableModel, -1, 0, isSSS);
    	}

    public JMultiStructureFilterPanel(Frame parent, CompoundTableModel tableModel, int column, int exclusionFlag, boolean isSSS) {
		super(parent, tableModel, column, exclusionFlag);
		mIsSSS = isSSS;

        JPanel contentPanel = new JPanel();
        double[][] size = { {4, TableLayout.FILL, 4, TableLayout.PREFERRED, 4},
                            {TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4} };
        contentPanel.setLayout(new TableLayout(size));
        contentPanel.setOpaque(false);

		mComboBox = new JComboBox() {
            private static final long serialVersionUID = 0x20080611;
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                size.width = Math.min(72, size.width);
                return size;
                } 
            };
        updateComboBox(isSSS ? cItemContains : cItemIsSimilarTo);
		contentPanel.add(mComboBox, "1,0,3,0");

		if (!isSSS)
			contentPanel.add(getSimilaritySlider(), "3,2");

		mStructurePane = new CompoundCollectionPane<String>(new DefaultCompoundCollectionModel.IDCode(), false) {
            private static final long serialVersionUID = 0x20110520;
            public Dimension getPreferredSize() {
                return new Dimension(100,80);
                } 
			};
		mStructurePane.setCreateFragments(isSSS);
		mStructurePane.setEditable(true);
		mStructurePane.setShowValidationError(true);
		mStructurePane.setClipboardHandler(new ClipboardHandler());
		mStructurePane.getModel().addCompoundCollectionListener(this);
		contentPanel.add(mStructurePane, isSSS ? "1,2,3,2" : "1,2");

		add(contentPanel, BorderLayout.CENTER);

		mIsUserChange = true;
    	}

	public void itemStateChanged(ItemEvent e) {
		super.itemStateChanged(e);

		if (e.getSource() == mComboBox
         && e.getStateChange() == ItemEvent.SELECTED) {
			if (mComboBox.getSelectedItem().equals(cItemDisabled))
				setInverse(false);

			updateExclusion();
			}
		}

	@Override
	public void collectionUpdated(int fromIndex, int toIndex) {
		if (!mIsSSS && isActive()) {
        	if (mStructurePane.getModel().getSize() == 0)
        		mSimilarity = null;
        	else if (mSimilarity != null && fromIndex == 0 && toIndex+1 >= mSimilarity.length)
        		mSimilarity = null;
        	if (mSimilarity != null) {
        		if (mSimilarity.length != mStructurePane.getModel().getSize()) {
        			float[][] newSimilarity = new float[mStructurePane.getModel().getSize()][];
        			for (int i=0; i<fromIndex; i++)
        				newSimilarity[i] = mSimilarity[i];
        			for (int i=toIndex+1; i<newSimilarity.length; i++)
        				newSimilarity[i] = mSimilarity[i];
        			mSimilarity = newSimilarity;
        			}
                int descriptorColumn = mDescriptorColumn[mComboBox.getSelectedIndex()];
    			for (int i=0; i<mSimilarity.length; i++) {
    				if (mSimilarity[i] == null || (i>= fromIndex && i <= toIndex)) {
	                    mSimilarity[i] = createSimilarityList(getStructure(i), descriptorColumn);
	
	                    if (mSimilarity[i] == null) {	// user cancelled SMP dialog
	                        mComboBox.setSelectedItem(cItemDisabled);
	                        break;
	                    	}
	    				}
    				}
        		}
            }

		if (mStructurePane.getModel().getSize() == 0)
			setInverse(false);

		updateExclusion();
		}

	@Override
	protected int getStructureCount() {
		return mStructurePane.getModel().getSize();
		}

	@Override
	protected StereoMolecule getStructure(int i) {
		return mStructurePane.getModel().getMolecule(i);
		}

	@Override
	public boolean supportsSSS() {
		return mIsSSS;
		}

	@Override
	public boolean supportsSim() {
		return !mIsSSS;
		}

	@Override
	public String getInnerSettings() {
		CompoundCollectionModel<String> model = mStructurePane.getModel();
		if (model.getSize() != 0) {
            String item = (String)mComboBox.getSelectedItem();

            String settings = item.equals(cItemDisabled) ? cFilterDisabled
							: mIsSSS ? null : itemToDescriptor(item)+"\t"+getSimilaritySlider().getValue();

			for (int i=0; i<model.getSize(); i++)
				settings = attachSetting(settings, model.getCompound(i));

			return settings;
			}
		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (settings != null) {
            String desiredItem = null;
			if (settings.startsWith(cFilterDisabled)) {
                setInverse(false);
				populateStructures(settings.substring(cFilterDisabled.length()+1));
                desiredItem = cItemDisabled;
                }
			else if (mIsSSS) {
				populateStructures(settings);
				desiredItem = cItemContains;
				}
			else {
                int index1 = settings.indexOf('\t');
				int index2 = settings.indexOf('\t', index1+1);
                String descriptor = settings.substring(0, index1);

                int similarity = Integer.parseInt(settings.substring(index1+1, index2));
				getSimilaritySlider().setValue(similarity);

				populateStructures(settings.substring(index2+1));
				desiredItem = descriptorToItem(descriptor);
				}

			if (!desiredItem.equals(mComboBox.getSelectedItem()))
				mComboBox.setSelectedItem(desiredItem);
			else
				updateExclusion();
			}
		}

	public CompoundCollectionPane<String> getCompoundCollectionPane() {
		return mStructurePane;
		}

	/**
	 * Add idcodes to the collection model without calling updateExclusion().
	 * @param idcodeList TAB-delimited idcode list
	 */
	private void populateStructures(String idcodeList) {
		CompoundCollectionModel<String> model = mStructurePane.getModel();
		model.removeCompoundCollectionListener(this);
		String[] idcode = idcodeList.split("\\t");
		for (int i=0; i<idcode.length; i++)
			model.addCompound(idcode[i]);
		model.addCompoundCollectionListener(this);
		}
	}
