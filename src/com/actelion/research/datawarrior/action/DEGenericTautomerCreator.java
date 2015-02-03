/* * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland * * This file is part of DataWarrior. *  * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the * GNU General Public License as published by the Free Software Foundation, either version 3 of * the License, or (at your option) any later version. *  * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. * See the GNU General Public License for more details. * You should have received a copy of the GNU General Public License along with DataWarrior. * If not, see http://www.gnu.org/licenses/. * * @author Thomas Sander */package com.actelion.research.datawarrior.action;import java.awt.Frame;import com.actelion.research.chem.Canonizer;import com.actelion.research.chem.CanonizerUtil;import com.actelion.research.chem.StereoMolecule;import com.actelion.research.chem.TautomerHelper;import com.actelion.research.chem.io.CompoundTableConstants;import com.actelion.research.gui.JProgressDialog;import com.actelion.research.table.CompoundTableModel;
public class DEGenericTautomerCreator implements Runnable {	private CompoundTableModel	mTableModel;	private Frame  				mParentFrame;	private JProgressDialog		mProgressDialog;	private int					mStructureColumn;
    public DEGenericTautomerCreator(Frame owner, CompoundTableModel tableModel) {		mParentFrame = owner;		mTableModel = tableModel;	    }
	public void create(int structureColumn) {        mStructureColumn = structureColumn;		mProgressDialog = new JProgressDialog(mParentFrame);

		Thread t = new Thread(this, "DEGenericTautomerCreator");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
		}

	public void run() {
		runAnalysis();

		mProgressDialog.stopProgress();
	    mProgressDialog.close(null);
		}

	private void runAnalysis() {
		mProgressDialog.startProgress("Creating generic tautomers...", 0, mTableModel.getTotalRowCount());
        int originalCoordsColumn = mTableModel.getChildColumn(mStructureColumn, CompoundTableConstants.cColumnType2DCoordinates);        final String[] columnTitle1 = { "Tautomer Hash", "1" };        final String[] columnTitle2 = { "Tautomer Hash", "1", "2" };        final String[] columnTitle = (originalCoordsColumn == -1) ? columnTitle1 : columnTitle2;        int firstColumn = mTableModel.addNewColumns(columnTitle);        mTableModel.prepareStructureColumns(firstColumn+1, "Generic Tautomer", true, false);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (mProgressDialog.threadMustDie())
				break;

			mProgressDialog.updateProgress(row+1);
			StereoMolecule molContainer = new StereoMolecule();
			StereoMolecule mol = mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), mStructureColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, molContainer);
			if (mol != null) {			    StereoMolecule tautomer = new TautomerHelper(mol).createGenericTautomer(true);			    if (tautomer != mol) {
    			    Canonizer canonizer = new Canonizer(tautomer, Canonizer.ENCODE_ATOM_CUSTOM_LABELS);    				mTableModel.setTotalValueAt(""+CanonizerUtil.StrongHasher.hash(canonizer.getIDCode()), row, firstColumn);    				mTableModel.setTotalValueAt(canonizer.getIDCode(), row, firstColumn+1);    				if (originalCoordsColumn != -1)
    				    mTableModel.setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstColumn+2);			        }				}
			}
		mTableModel.finalizeNewColumns(firstColumn, mProgressDialog);		}
	}
