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

package com.actelion.research.datawarrior.action;

import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import com.actelion.research.chem.*;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.gui.*;
import com.actelion.research.table.*;

public class DESARAnalyzer implements Runnable,StructureListener {
	private static final String CORE_FRAGMENT_COLUMN_NAME = "Core Fragment";
	private static final int cTableColumnNone = -1;
	private static final int cTableColumnNew = -2;
	private static final int cTableColumnUnassigned = -3;

	private CompoundTableModel	mTableModel;
	private Frame  				mParentFrame;
	private JProgressDialog		mProgressDialog;
	private StereoMolecule   	mCoreFragment;
	private JCheckBox			mCheckBoxDistinguishStereocenters;
	private boolean				mDistinguishStereocenters;
    private int                 mIDCodeColumn;

    public DESARAnalyzer(Frame owner, CompoundTableModel tableModel) {
		mParentFrame = owner;
		mTableModel = tableModel;
	    }

	public void analyze(int idcodeColumn) {
        mIDCodeColumn = idcodeColumn;

        mCheckBoxDistinguishStereocenters = new JCheckBox("Distinguish Stereoisomers");
        mCheckBoxDistinguishStereocenters.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JDrawDialog drawDialog = new JDrawDialog(mParentFrame, true, "Select Core Fragment");
		drawDialog.setAccessory(mCheckBoxDistinguishStereocenters);
        drawDialog.addStructureListener(this);
		drawDialog.setVisible(true);
		}

	public void structureChanged(StereoMolecule mol) {
	    mCoreFragment = mol;
	    mDistinguishStereocenters = mCheckBoxDistinguishStereocenters.isSelected();

		if (mCoreFragment != null && mCoreFragment.getAllAtoms() != 0) {
			mProgressDialog = new JProgressDialog(mParentFrame);

			Thread t = new Thread(this, "DESARAnalyzer");
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
			}
		}

	public void run() {
		runAnalysis();

		mProgressDialog.stopProgress();
	    mProgressDialog.close(null);
		}

	private void runAnalysis() {
		SSSearcherWithIndex searcher = new SSSearcherWithIndex();
		searcher.setFragment(mCoreFragment, null);

		mCoreFragment.ensureHelperArrays(Molecule.cHelperNeighbours);
		String[] coreFragment = new String[mTableModel.getTotalRowCount()];
		String[][] substituent = new String[mTableModel.getTotalRowCount()][mCoreFragment.getAtoms()];

		int multipleMatches = 0;

		mProgressDialog.startProgress("Analyzing substituents...", 0, mTableModel.getTotalRowCount());

        int coordinateColumn = mTableModel.getChildColumn(mIDCodeColumn, CompoundTableModel.cColumnType2DCoordinates);
        int fingerprintColumn = mTableModel.getChildColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		UniqueStringList coreIDCodeList = new UniqueStringList();
        ArrayList<StereoMolecule> coreFragmentList = new ArrayList<StereoMolecule>();
        ArrayList<int[]> coreParitiesList = new ArrayList<int[]>();
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (mProgressDialog.threadMustDie())
				break;

			mProgressDialog.updateProgress(row+1);

			byte[] idcode = (byte[])mTableModel.getTotalRecord(row).getData(mIDCodeColumn);
			if (idcode != null) {
				searcher.setMolecule(idcode, (int[])mTableModel.getTotalRecord(row).getData(fingerprintColumn));
				int matchCount = searcher.findFragmentInMolecule(SSSearcher.cCountModeRigorous, SSSearcher.cDefaultMatchMode);
				if (matchCount > 0) {
					if (matchCount > 1)
						multipleMatches++;

					int[] matchAtom = (int[])searcher.getMatchList().get(0);

					byte[] coords = (byte[])mTableModel.getTotalRecord(row).getData(coordinateColumn);
					StereoMolecule mol = new IDCodeParser(true).getCompactMolecule(idcode, coords);

						// store original fragment atom numbers incremented by 1 in atomMapNo
					for (int i=0; i<matchAtom.length; i++)
						mol.setAtomMapNo(matchAtom[i], i+1, false);

						// mark all atoms belonging to core fragment
					boolean[] isCoreAtom = new boolean[mol.getAllAtoms()];
					for (int i=0; i<matchAtom.length; i++)
						isCoreAtom[matchAtom[i]] = true;

					String stereoInfo = "";
					int[] coreAtomParity = null;
					if (mDistinguishStereocenters) {
						mol.ensureHelperArrays(Molecule.cHelperNeighbours);
						boolean[] isExtendedCoreAtom = new boolean[mol.getAllAtoms()];	// core plus direct neighbours
						for (int i=0; i<matchAtom.length; i++) {
							int atom = matchAtom[i];
							isExtendedCoreAtom[atom] = true;
							for (int j=0; j<mol.getConnAtoms(atom); j++) {
								int connAtom = mol.getConnAtom(atom, j);
								if (!isCoreAtom[connAtom])
									isExtendedCoreAtom[connAtom] = true;
								}
							}

						StereoMolecule extendedCore = new StereoMolecule();	// core plus direct neighbours
						mol.copyMoleculeByAtoms(extendedCore, isExtendedCoreAtom, true, null);

							// change atomicNo of non-core atoms to 'R1'
						for (int atom=0; atom<extendedCore.getAllAtoms(); atom++)
							if (extendedCore.getAtomMapNo(atom) == 0)
								extendedCore.setAtomicNo(atom, 142);	// 'R1'

						extendedCore.ensureHelperArrays(Molecule.cHelperParities);

						boolean stereoCenterFound = false;
						coreAtomParity = new int[matchAtom.length];
						byte[] parityByte = new byte[matchAtom.length];
						for (int atom=0; atom<extendedCore.getAllAtoms(); atom++) {
							int coreAtomNo = extendedCore.getAtomMapNo(atom) - 1;
							if (coreAtomNo != -1) {
								int atomParity = extendedCore.getAtomParity(atom);
								coreAtomParity[coreAtomNo] = atomParity;
								parityByte[coreAtomNo] = (byte)('0'+atomParity);
                                if (atomParity != Molecule.cAtomParityNone)
                                    stereoCenterFound = true;
								if (atomParity == Molecule.cAtomParity1
								 || atomParity == Molecule.cAtomParity2) {
                                    int esrType = extendedCore.getAtomESRType(atom);
                                    if (esrType != Molecule.cESRTypeAbs) {
                                        int esrEncoding = (extendedCore.getAtomESRGroup(atom) << 4)
                                                        + ((esrType == Molecule.cESRTypeAnd) ? 4 : 8);
                                        parityByte[coreAtomNo] += esrEncoding;
                                        coreAtomParity[coreAtomNo] += esrEncoding;
                                        }
                                    }
								}
							}
                        if (stereoCenterFound)
                            stereoInfo = new String(parityByte);
                        else
                            coreAtomParity = null;
						}

					StereoMolecule core = new StereoMolecule();
					mol.copyMoleculeByAtoms(core, isCoreAtom, true, null);
					core.stripStereoInformation();
					coreFragment[row] = new Canonizer(core).getIDCode() + stereoInfo;

					if (coreIDCodeList.addString(coreFragment[row]) != -1) {	// new unique core fragment
						coreFragmentList.add(core);
						coreParitiesList.add(coreAtomParity);
						}

					for (int i=0; i<matchAtom.length; i++)
						mol.setAtomicNo(matchAtom[i], 0);

					StereoMolecule fragment = new StereoMolecule();
					int[] workAtom = new int[mol.getAllAtoms()];
					for (int i=0; i<matchAtom.length; i++) {
						boolean[] isSubstituentAtom = new boolean[mol.getAllAtoms()];
						isSubstituentAtom[matchAtom[i]] = true;
						workAtom[0] = matchAtom[i];
						int current = 0;
						int highest = 0;
						while (current <= highest) {
							for (int j=0; j<mol.getConnAtoms(workAtom[current]); j++) {
								if (current == 0 || !isCoreAtom[workAtom[current]]) {
									int candidate = mol.getConnAtom(workAtom[current], j);
									if (!isSubstituentAtom[candidate]
									 && (current != 0 || !isCoreAtom[candidate])) {
										isSubstituentAtom[candidate] = true;
										workAtom[++highest] = candidate;
										}
									}
								}
							current++;
							}
						mol.copyMoleculeByAtoms(fragment, isSubstituentAtom, false, null);

						if (!mDistinguishStereocenters)
							fragment.stripStereoInformation();

							// if substituent is a ring forming bridge to the startatom
						for (int bond=fragment.getAllBonds()-1; bond>=0; bond--)
							if (fragment.getAtomicNo(fragment.getBondAtom(0, bond)) == 0
							 && fragment.getAtomicNo(fragment.getBondAtom(1, bond)) == 0)
								fragment.deleteBond(bond);

						substituent[row][i] = (highest == 0) ? null : new Canonizer(fragment).getIDCode();
						}
					}
				}
			}

		if (coreIDCodeList.getSize() == 0) {
			final String message = "The core fragment is not a substructure of any of your records.";
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			return;
			}

		if (mProgressDialog.threadMustDie())
			return;

			// mark core atoms with varying substituents to require a new column
		int[] substituentColumn = new int[mCoreFragment.getAtoms()];
		String[] constantSubstituent = new String[mCoreFragment.getAtoms()];
		for (int atom=0; atom<mCoreFragment.getAtoms(); atom++) {
			substituentColumn[atom] = cTableColumnNone;	// indication that substituent doesn't vary
			constantSubstituent[atom] = substituent[0][atom];
			for (int row=1; row<mTableModel.getTotalRowCount(); row++) {
				if (substituent[row][atom] == null
				 && constantSubstituent[atom] == null)
					continue;
				if (substituent[row][atom] == null
				 || constantSubstituent[atom] == null
				 || !substituent[row][atom].equals(constantSubstituent[atom])) {
					substituentColumn[atom] = cTableColumnUnassigned;
					break;
					}
				}
			}

			// add R-groups to all atoms of core fragment with varying substituents
		String[] idcodeListWithRGroups = new String[coreIDCodeList.getSize()];
		for (int i=0; i<coreIDCodeList.getSize(); i++) {
		    StereoMolecule core = coreFragmentList.get(i);

				// recreate matchAtom array from stored mapping numbers
			int[] matchAtom = new int[mCoreFragment.getAtoms()];
			for (int atom=0; atom<core.getAtoms(); atom++)
				matchAtom[core.getAtomMapNo(atom) - 1] = atom;

			int substituentNo = 0;
			for (int atom=0; atom<mCoreFragment.getAtoms(); atom++) {
						//	if substituent varies => attach an R group
				if (substituentColumn[atom] == cTableColumnUnassigned) {
					int newAtom = core.addAtom((substituentNo < 3) ?
										142+substituentNo : 126+substituentNo);
					core.addBond(matchAtom[atom], newAtom, 1);
					substituentNo++;
					}
				else {	//	else => attach the non-varying substituent (if it is not null = 'unsubstituted')
					if (constantSubstituent[atom] != null) {
					    StereoMolecule theSubstituent = new IDCodeParser(true).getCompactMolecule(constantSubstituent[atom]);
						core.addSubstituent(theSubstituent, matchAtom[atom]);
						}
					}
				}

			int[] parityList = coreParitiesList.get(i);
			if (parityList != null) {
				for (int atom=0; atom<mCoreFragment.getAtoms(); atom++) {
					int parity = parityList[atom] & 3;
                    int esrType = (parityList[atom] & 0x0C);
                    int esrGroup = (parityList[atom] & 0xF0) >> 4;
                    core.setAtomParity(matchAtom[atom], parity, false);
                    if (esrType != 0) {
                        core.setAtomESR(matchAtom[atom], esrType == 4 ?
                                Molecule.cESRTypeAnd : Molecule.cESRTypeOr,
                                        esrGroup);
				        }
                    }
                }
			new CoordinateInventor().invent(core);	// creates stereo bonds from parities
            core.setStereoBondsFromParity();

			idcodeListWithRGroups[i] = new Canonizer(core).getIDCode();
			}
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (coreFragment[row] != null) {
				int index = coreIDCodeList.getListIndex(coreFragment[row]);
				coreFragment[row] = idcodeListWithRGroups[index];
				}
			}

		String[] newColumnName = new String[1+mCoreFragment.getAtoms()];
		int newColumnCount = 0;
		int coreColumn = mTableModel.findColumn(CORE_FRAGMENT_COLUMN_NAME);
		if (coreColumn != -1) {
            String specialType = mTableModel.getColumnSpecialType(coreColumn);
            if (specialType == null || !specialType.equals(CompoundTableModel.cColumnTypeIDCode))
                coreColumn = -1;
            }
        if (coreColumn == -1) {
            newColumnName[newColumnCount++] = CORE_FRAGMENT_COLUMN_NAME;
            coreColumn = cTableColumnNew;
            }

			// assign varying substituents to existing or new columns
		int varyingSubstituentCount = 0;
		for (int atom=0; atom<mCoreFragment.getAtoms(); atom++) {
			if (substituentColumn[atom] == cTableColumnUnassigned) {
				String columnName = "R"+(++varyingSubstituentCount);
				substituentColumn[atom] = mTableModel.findColumn(columnName);
                if (substituentColumn[atom] != -1) {
                    String specialType = mTableModel.getColumnSpecialType(substituentColumn[atom]);
                    if (specialType == null || !specialType.equals(CompoundTableModel.cColumnTypeIDCode))
                        substituentColumn[atom] = -1;
                    }
				if (substituentColumn[atom] == -1) {
					newColumnName[newColumnCount++] = columnName;
					substituentColumn[atom] = cTableColumnNew;
					}
				}
			}

        if (mProgressDialog.threadMustDie())
            return;

        int firstNewColumn = Integer.MAX_VALUE;
        if (newColumnCount != 0) {
    		String[] resizedNewColumnName = new String[newColumnCount];
    		for (int i=0; i<newColumnCount; i++)
    			resizedNewColumnName[i] = newColumnName[i];
    
    		firstNewColumn = mTableModel.addNewColumns(resizedNewColumnName);
    		newColumnCount = 0;
    		if (coreColumn == cTableColumnNew)
    			coreColumn = firstNewColumn + newColumnCount++;
    		for (int atom=0; atom<mCoreFragment.getAtoms(); atom++)
    			if (substituentColumn[atom] == cTableColumnNew)
    				substituentColumn[atom] = firstNewColumn + newColumnCount++;
            }

		mProgressDialog.startProgress("Extending Table...", 0, mTableModel.getTotalRowCount());

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (mProgressDialog.threadMustDie())
				break;

			mProgressDialog.updateProgress(row+1);

			if (coreFragment[row] != null) {
				mTableModel.setTotalValueAt(coreFragment[row], row, coreColumn);
				for (int atom=0; atom<mCoreFragment.getAtoms(); atom++)
					if (substituentColumn[atom] != cTableColumnNone)
						mTableModel.setTotalValueAt(substituent[row][atom], row, substituentColumn[atom]);
				}
			}

		if (coreColumn < firstNewColumn)
			mTableModel.finalizeChangeColumn(coreColumn, 0, mTableModel.getTotalRowCount());
		for (int atom=0; atom<mCoreFragment.getAtoms(); atom++)
			if (substituentColumn[atom] != cTableColumnNone && substituentColumn[atom] < firstNewColumn)
				mTableModel.finalizeChangeColumn(substituentColumn[atom], 0, mTableModel.getTotalRowCount());

        if (newColumnCount != 0) {
            for (int i=0; i<newColumnCount; i++)
                mTableModel.setColumnProperty(firstNewColumn+i, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);
    
            mTableModel.finalizeNewColumns(firstNewColumn, mProgressDialog);
            }

		if (multipleMatches > 0) {
			final String message = "The core fragment was multiply found in "+multipleMatches+" records. Try avoiding this by specifying less symmetrical core structures";
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			}
		}
	}
