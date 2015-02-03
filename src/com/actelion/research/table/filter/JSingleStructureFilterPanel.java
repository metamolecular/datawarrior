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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.StructureListener;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.CompoundTableModel;

public class JSingleStructureFilterPanel extends JStructureFilterPanel 
				implements DescriptorConstants,StructureListener {
	private static final long serialVersionUID = 0x20060925;

	private JEditableStructureView  mStructureView;
	private boolean 				mDisableEvents;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param parent
	 * @param tableModel
	 * @param mol
	 */
	public JSingleStructureFilterPanel(Frame parent, CompoundTableModel tableModel, StereoMolecule mol) {
		this(parent, tableModel, -1, 0, mol);
		}

	public JSingleStructureFilterPanel(Frame parent, CompoundTableModel tableModel, int column, int exclusionFlag, StereoMolecule mol) {
		super(parent, tableModel, column, exclusionFlag);

		JPanel contentPanel = new JPanel();
		double[][] size = { {4, TableLayout.FILL, 4, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, TableLayout.FILL} };
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
		updateComboBox(cItemContains);
		contentPanel.add(mComboBox, "1,0");

		contentPanel.add(getSimilaritySlider(), "3,0,3,1");
		getSimilaritySlider().setEnabled(((String)mComboBox.getSelectedItem()).startsWith(cItemIsSimilarTo));

		if (mol == null) {
			StereoMolecule fragment = new StereoMolecule();
			fragment.setFragment(true);
			mStructureView = new JEditableStructureView(fragment);
			}
		else {
			mStructureView = new JEditableStructureView(mol);

				// select first similarity item
			for (int i=0; i<mComboBox.getItemCount(); i++) {
				if (((String)mComboBox.getItemAt(i)).startsWith(cItemIsSimilarTo)) {
					mComboBox.setSelectedIndex(i);
					break;
					}
				}
			updateExclusion();
			}

		mStructureView.setClipboardHandler(new ClipboardHandler());
		mStructureView.setMinimumSize(new Dimension(100, 100));
		mStructureView.setPreferredSize(new Dimension(100, 100));
		mStructureView.setBackground(getBackground());
		mStructureView.addStructureListener(this);
		contentPanel.add(mStructureView, "1,1");

		add(contentPanel, BorderLayout.CENTER);

		mIsUserChange = true;
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		super.itemStateChanged(e);

		if (e.getSource() == mComboBox
		 && e.getStateChange() == ItemEvent.SELECTED
		 && !mDisableEvents) {

			mDisableEvents = true;

			String item = (String)mComboBox.getSelectedItem();
			if (item.equals(cItemContains)) {
				getSimilaritySlider().setEnabled(false);
				mStructureView.getMolecule().setFragment(true);
				}
			else if (item.equals(cItemDisabled)) {
				setInverse(false);
				getSimilaritySlider().setEnabled(false);
				mStructureView.getMolecule().setFragment(true);
				}
			else {  // similarity
				getSimilaritySlider().setEnabled(true);
				mStructureView.getMolecule().setFragment(false);
				}
			mStructureView.structureChanged();

			mDisableEvents = false;

			updateExclusion();
			}
		}

	@Override
	public void structureChanged(StereoMolecule mol) {
		if (mDisableEvents)	// avoid recursive calls
			return;

		mDisableEvents = true;

		mSimilarity = null;
		mol.removeAtomSelection();
		if (mol.isFragment()) {
			if (((String)mComboBox.getSelectedItem()).startsWith(cItemIsSimilarTo)) {
				if (((String)mComboBox.getItemAt(0)).equals(cItemContains)) {
					mComboBox.setSelectedItem(cItemContains);
					getSimilaritySlider().setEnabled(false);
					}
				else {
					mol.setFragment(false);
					}
				}
			}
		else {
			if (((String)mComboBox.getSelectedItem()).equals(cItemContains)) {
				boolean found = false;
				for (int i=0; i<mComboBox.getItemCount(); i++) {
					if (((String)mComboBox.getItemAt(i)).startsWith(cItemIsSimilarTo)) {
						mComboBox.setSelectedIndex(i);
						getSimilaritySlider().setEnabled(true);
						found = true;
						break;
						}
					}
				if (!found)
					mol.setFragment(true);
				}
			}

		mDisableEvents = false;

		if (mStructureView.getMolecule().getAllAtoms() == 0)
			setInverse(false);

		updateExclusion();
		}

	@Override
	protected int getStructureCount() {
		return (mStructureView.getMolecule().getAllAtoms() == 0) ? 0 : 1;
		}

	@Override
	protected StereoMolecule getStructure(int i) {
		return (i == 0) ? mStructureView.getMolecule() : null;
		}

	@Override
	protected boolean supportsSSS() {
		return true;
		}

	@Override
	protected boolean supportsSim() {
		return true;
		}

	@Override
	public String getInnerSettings() {
		if (mStructureView.getMolecule().getAllAtoms() != 0) {
			String item = (String)mComboBox.getSelectedItem();
			String settings = item.equals(cItemContains) ? cFilterBySubstructure
					: item.equals(cItemDisabled) ? cFilterDisabled
					: itemToDescriptor(item)+"\t"+getSimilaritySlider().getValue();

			StereoMolecule mol = mStructureView.getMolecule();
			settings = attachSetting(settings, new Canonizer(mol).getIDCode());
			return settings;
			}
		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (settings != null) {
			String desiredItem = null;
			if (settings.startsWith(cFilterBySubstructure)) {
				String idcode = settings.substring(cFilterBySubstructure.length()+1);
				mStructureView.setIDCode(idcode);
				desiredItem = cItemContains;
				}
			else if (settings.startsWith(cFilterDisabled)) {
				String idcode = settings.substring(cFilterDisabled.length()+1);
				mStructureView.setIDCode(idcode);
				setInverse(false);
				desiredItem = cItemDisabled;
				}
			else {
				int index1 = settings.indexOf('\t');
				int index2 = settings.indexOf('\t', index1+1);
				String descriptor = settings.substring(0, index1);

					// to be compatible with format prior V2.7.0
				if (descriptor.equals(cFilterBySimilarity))
					descriptor = DESCRIPTOR_FFP512.shortName;

				int similarity = Integer.parseInt(settings.substring(index1+1, index2));
				getSimilaritySlider().setValue(similarity);
				String idcode = settings.substring(index2+1);
				mStructureView.setIDCode(idcode);
				desiredItem = descriptorToItem(descriptor);
				}

			if (!desiredItem.equals(mComboBox.getSelectedItem()))
				mComboBox.setSelectedItem(desiredItem);
			else
				updateExclusion();
			}
		}
	}
