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

package com.actelion.research.datawarrior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.mcs.MCS;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableModel;

/**
 * Adds atom coloring to CompoundTableModel
 */
public class DECompoundTableModel extends CompoundTableModel {
    private static final long serialVersionUID = 0x20060904;

    private TreeMap<String,CompoundTableAtomColorInfo>	mColorInfoMap;

	public DECompoundTableModel() {
		super();
		setExtensionHandler(new DECompoundTableExtensionHandler());
		mColorInfoMap = new TreeMap<String,CompoundTableAtomColorInfo>();
		}

	@Override
	public void colorizeAtoms(CompoundRecord record, int idcodeColumn, int atomColorMode, StereoMolecule mol) {
			// priority 1: atoms set explicitly to some color (e.g. pKa coloring)
			// priority 2: atoms matching a substructure are dark red
			// priority 3: atoms matching a simstructure are dark green

        super.colorizeAtoms(record, idcodeColumn, atomColorMode, mol);

        if (atomColorMode == CompoundTableModel.ATOM_COLOR_MODE_ALL) {
	        int hiliteMode = getStructureHiliteMode(idcodeColumn);
	        switch (hiliteMode) {
	        case cStructureHiliteModeFilter:
	        	colorizeAtomsByFilter(record, idcodeColumn, mol);
	        	break;
	        case cStructureHiliteModeCurrentRow:
	        	colorizeAtomsByCurrentRow(idcodeColumn, mol);
	        	break;
	        	}
        	}
		}

	@Override
	public void freeCompoundFlag(int flag) {
		removeFromList(flag);
		super.freeCompoundFlag(flag);
		}

	@Override
	public void clearCompoundFlag(int flag) {
		removeFromList(flag);
		super.clearCompoundFlag(flag);
		}

	@Override
	public void setSubStructureExclusion(int flag, int idcodeColumn, StereoMolecule[] fragment, boolean inverse) {
//	    if (inverse)	we need the colorInfo when toggling inverse
//	    	setAtomColorInfo(idcodeColumn, cStructureHiliteModeFilter, null);
//    	else
    		setAtomColorInfo(idcodeColumn, cStructureHiliteModeFilter, 
		    		new CompoundTableAtomColorInfo(idcodeColumn, CompoundTableAtomColorInfo.TYPE_SSS_FILTER,
                                                   flag, inverse, fragment));
		super.setSubStructureExclusion(flag, idcodeColumn, fragment, inverse);
		}

	@Override
	public void setSimilarityExclusion(int flag, int descriptorColumn,
									   StereoMolecule[] molecule,
									   float[][] similarity, float minSimilarity,
									   boolean inverse, boolean isAdjusting) {
		int idcodeColumn = getParentColumn(descriptorColumn);
//	    if (inverse)	we need the colorInfo when toggling inverse
//	    	setAtomColorInfo(idcodeColumn, cStructureHiliteModeFilter, null);
 //   	else {
    		// molecules will be turned into fragments. Therefore we need to copy...
    		StereoMolecule[] copy = new StereoMolecule[molecule.length];
    		for (int i=0; i<molecule.length; i++)
    			copy[i] = new StereoMolecule(molecule[i]);
    		CompoundTableAtomColorInfo colorInfo = new CompoundTableAtomColorInfo(idcodeColumn,
    				CompoundTableAtomColorInfo.TYPE_SIM_FILTER, flag, inverse, copy);
    		if (molecule.length != 1) {
    			colorInfo.bestMatch = new int[getTotalRowCount()];
    			for (int i=0; i<getTotalRowCount(); i++) {
					int row = getTotalRecord(i).getID();
    				float maxSimilarity = 0f;
    				for (int j=0; j<similarity.length; j++) {
    					if (maxSimilarity < similarity[j][row]) {
    						maxSimilarity = similarity[j][row];
    						colorInfo.bestMatch[row] = j;
    						}
    					}
    				}
    			}

    		setAtomColorInfo(idcodeColumn, cStructureHiliteModeFilter, colorInfo);
//			}

	    super.setSimilarityExclusion(flag, descriptorColumn, molecule, similarity, minSimilarity, inverse, isAdjusting);
		}

	@Override
	public void invertExclusion(int exclusionFlag) {
		for (CompoundTableAtomColorInfo colorInfo:mColorInfoMap.values())
			if (colorInfo.exclusionFlag == exclusionFlag)
			    colorInfo.inverse = !colorInfo.inverse;
	    
		super.invertExclusion(exclusionFlag);
		}

	@Override
	public void fireCompoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			updateCurrentRowColorInfo();
			}
		else if (e.getType() == CompoundTableEvent.cNewTable) {
			mColorInfoMap.clear();
			ensureCurrentRowColorInfo(0);
			}
        else if (e.getType() == CompoundTableEvent.cAddColumns) {
        	ensureCurrentRowColorInfo(e.getSpecifier());
            }
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			for (String key:mColorInfoMap.keySet()) {
				CompoundTableAtomColorInfo colorInfo = mColorInfoMap.get(key);
				if (columnMapping[colorInfo.idcodeColumn] == -1)
                    mColorInfoMap.remove(key);
                else
                    colorInfo.idcodeColumn = columnMapping[colorInfo.idcodeColumn];
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			updateCurrentRowColorInfo();
			}

		super.fireCompoundTableChanged(e);
		}

	private void updateCurrentRowColorInfo() {
		ensureCurrentRowColorInfo(0);
		for (CompoundTableAtomColorInfo colorInfo:mColorInfoMap.values()) {
			if (colorInfo.type == CompoundTableAtomColorInfo.TYPE_SIM_TO_CURRENT) {
				CompoundRecord currentRecord = getActiveRow();
				if (currentRecord == null) {
					colorInfo.refMol = null;
					}
				else {
					colorInfo.refMol = new StereoMolecule[1];
					colorInfo.refMol[0] = getChemicalStructure(currentRecord, colorInfo.idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
					}
				}
			}
		}

	private void ensureCurrentRowColorInfo(int firstColumn) {
        for (int column=firstColumn; column<getTotalColumnCount(); column++)
			if (getStructureHiliteMode(column) == cStructureHiliteModeCurrentRow
			 && getAtomColorInfo(column, cStructureHiliteModeCurrentRow) == null)
				prepareCurrentRowAtomColoring(column);
		}

    private void prepareCurrentRowAtomColoring(int idcodeColumn) {
		setAtomColorInfo(idcodeColumn, cStructureHiliteModeCurrentRow,
				new CompoundTableAtomColorInfo(idcodeColumn, CompoundTableAtomColorInfo.TYPE_SIM_TO_CURRENT, -1, false, null));
    	}

    private CompoundTableAtomColorInfo getAtomColorInfo(int idcodeColumn, int type) {
    	return mColorInfoMap.get(cHiliteModeOption[type]+":"+getColumnTitleNoAlias(idcodeColumn));
    	}

    private void setAtomColorInfo(int idcodeColumn, int type, CompoundTableAtomColorInfo colorInfo) {
    	mColorInfoMap.put(cHiliteModeOption[type]+":"+getColumnTitleNoAlias(idcodeColumn), colorInfo);
    	}

    private void colorizeAtomsByFilter(CompoundRecord record, int idcodeColumn, StereoMolecule mol) {
    	CompoundTableAtomColorInfo colorInfo = getAtomColorInfo(idcodeColumn, cStructureHiliteModeFilter);

    	if (colorInfo != null
    	 && colorInfo.type == CompoundTableAtomColorInfo.TYPE_SSS_FILTER
		 && !colorInfo.inverse) {
			StereoMolecule[] fragment = colorInfo.refMol;
    		if (fragment.length != 1 && colorInfo.bestMatch == null) {
    			colorInfo.bestMatch = new int[getTotalRowCount()];
    			Arrays.fill(colorInfo.bestMatch, -1);
    			}
		    SSSearcher searcher = new SSSearcher();
		    if (fragment.length != 1 && colorInfo.bestMatch[record.getID()] == -1) {
		    	int maxFragmentSize = 0;
				for (int i=0; i<fragment.length; i++) {
					if (fragment[i].getAtoms() > maxFragmentSize) {
						searcher.setMol(fragment[i], mol);
						if (searcher.isFragmentInMolecule(SSSearcher.cDefaultMatchMode)) {
							maxFragmentSize = fragment[i].getAtoms();
							colorInfo.bestMatch[record.getID()] = i;
							}
						}
					}
		    	}
		    int maxFragmentIndex = (colorInfo.bestMatch != null) ? colorInfo.bestMatch[record.getID()] : 0;
			searcher.setMol(fragment[maxFragmentIndex], mol);
			searcher.findFragmentInMolecule(SSSearcher.cCountModeOverlapping, SSSearcher.cDefaultMatchMode);
			ArrayList<int[]> matchList = searcher.getMatchList();
			if (matchList != null) {
                for (int[] matching:matchList)
					for (int k=0; k<matching.length; k++)
						if (mol.getAtomColor(matching[k]) == Molecule.cAtomColorBlack)
							mol.setAtomColor(matching[k], Molecule.cAtomColorDarkRed);
				}
            }

		if (colorInfo != null
		 && colorInfo.type == CompoundTableAtomColorInfo.TYPE_SIM_FILTER
		 && !colorInfo.inverse) {
			int bestMatch = (colorInfo.bestMatch == null) ? 0 : colorInfo.bestMatch[record.getID()];
			colorizeByMCS(colorInfo.refMol[bestMatch], mol, Molecule.cAtomColorDarkGreen, false, false);
			}
    	}

    private void colorizeAtomsByCurrentRow(int idcodeColumn, StereoMolecule mol) {
    	CompoundTableAtomColorInfo colorInfo = getAtomColorInfo(idcodeColumn, cStructureHiliteModeCurrentRow);
		if (colorInfo != null
		 && colorInfo.refMol != null
		 && colorInfo.type == CompoundTableAtomColorInfo.TYPE_SIM_TO_CURRENT
		 && !colorInfo.inverse) {
			colorizeByMCS(colorInfo.refMol[0], mol, Molecule.cAtomColorBlue, true, true);
            }
    	}

    private void colorizeByMCS(StereoMolecule refMol, StereoMolecule mol, int color, boolean hiliteBonds, boolean addNonMCSRefMolFragments) {
		MCS mcsSearcher = new MCS();
		mcsSearcher.set(mol, refMol);
		StereoMolecule mcs = mcsSearcher.getMCS();
		if (mcs != null && (mcs.getAllAtoms() >= mol.getAllAtoms() / 2)) {
		    SSSearcher sssSearcher = new SSSearcher();
		    sssSearcher.setMol(mcs, mol);
			boolean[] isMatchingAtom = null;
			boolean[] isMatchingBond = null;
			if (sssSearcher.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) == 1) {
	            int[] molMatch = sssSearcher.getMatchList().get(0);
	    		if (hiliteBonds) {
	    			isMatchingBond = new boolean[mol.getBonds()];
	    			for (int bond=0; bond<mcs.getBonds(); bond++)
	    				isMatchingBond[mol.getBond(molMatch[mcs.getBondAtom(0, bond)], molMatch[mcs.getBondAtom(1, bond)])] = true;
	    			}
	    		else {
	    			isMatchingAtom = new boolean[mol.getAtoms()];
					for (int k=0; k<molMatch.length; k++)
						isMatchingAtom[molMatch[k]] = true;
	    			}
	
	    		if (isMatchingBond != null) {
					for (int bond=0; bond<mol.getBonds(); bond++)
						mol.setBondBackgroundHiliting(bond, !isMatchingBond[bond]);
					}
		
				if (isMatchingAtom != null) {
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (isMatchingAtom[atom]
						 && mol.getAtomColor(atom) == Molecule.cAtomColorBlack)
							mol.setAtomColor(atom, color);
					}
	
				if (addNonMCSRefMolFragments) {
				    sssSearcher.setMolecule(refMol);
					isMatchingAtom = null;
					isMatchingBond = null;
					if (sssSearcher.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) == 1) {
						int originalMolAtomCount = mol.getAtoms();
			            int[] refMatch = sssSearcher.getMatchList().get(0);
	
			            isMatchingBond = new boolean[refMol.getBonds()];
		    			for (int bond=0; bond<mcs.getBonds(); bond++)
		    				isMatchingBond[refMol.getBond(refMatch[mcs.getBondAtom(0, bond)], refMatch[mcs.getBondAtom(1, bond)])] = true;
	
		    			isMatchingAtom = new boolean[refMol.getAtoms()];
						for (int k=0; k<refMatch.length; k++)
							isMatchingAtom[refMatch[k]] = true;
	
		    			int[] molAtom = new int[refMol.getAtoms()];
						for (int atom=0; atom<refMol.getAtoms(); atom++) {
							if (!isMatchingAtom[atom]) {
								molAtom[atom] = refMol.copyAtom(mol, atom, 0, 0);	// TODO correct ESR pre- and post-processing
								}
							}
	
						// add all mcs atom to match refMol atom indices to destination mol atom indices
						for (int atom=0; atom<mcs.getAtoms(); atom++)
							molAtom[refMatch[atom]] = molMatch[atom];
	
						for (int bond=0; bond<refMol.getBonds(); bond++) {
							if (!isMatchingBond[bond]) {
								int destBond = refMol.copyBond(mol, bond, 0, 0, molAtom, true);	// TODO correct ESR pre- and post-processing
								mol.setBondForegroundHiliting(destBond, true);
								}
							}
	
						for (int atom=0; atom<originalMolAtomCount; atom++)
							mol.setAtomMarker(atom, true);
						new CoordinateInventor(CoordinateInventor.MODE_REMOVE_HYDROGEN | CoordinateInventor.MODE_PREFER_MARKED_ATOM_COORDS).invent(mol);
						}
					}
				}
			}
    	}

	private void removeFromList(int exclusionFlag) {
		for (String key:mColorInfoMap.keySet()) {
			CompoundTableAtomColorInfo colorInfo = mColorInfoMap.get(key);
			if (colorInfo.exclusionFlag == exclusionFlag) {
				mColorInfoMap.remove(key);
				return;
				}
			}
		}
	}

class CompoundTableAtomColorInfo {
	static final int TYPE_SSS_FILTER = 1;
	static final int TYPE_SIM_FILTER = 2;
	static final int TYPE_CLUSTERING = 3;
	static final int TYPE_SIM_TO_CURRENT = 4;

    int idcodeColumn;
    int type;
    int exclusionFlag;  // used if atom coloring is associated with a filter
	boolean inverse;
	int[] bestMatch;	// query structure index of best match in case of a multi structure query
	StereoMolecule[] refMol;

	public CompoundTableAtomColorInfo(int column,
                                      int type,
                                      int exclusionFlag,
                                      boolean inverse,
                                      StereoMolecule[] refMol) {
        this.idcodeColumn = column;
		this.type = type;
		this.exclusionFlag = exclusionFlag;
		this.inverse = inverse;
		this.refMol = refMol;
		}
	}
