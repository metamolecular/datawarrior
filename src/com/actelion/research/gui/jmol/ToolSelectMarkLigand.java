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
 * @author Joel Freyss
 */
package com.actelion.research.gui.jmol;

import java.awt.event.MouseEvent;
import java.util.*;

import com.actelion.research.chem.*;
import com.actelion.research.chem.calculator.StructureCalculator;

/**
 * 
 */
public class ToolSelectMarkLigand extends AbstractTool {
	
	public ToolSelectMarkLigand() {
		super("Select Ligand");
	}
	
	
	@Override
	public void mouseClicked(MouseEvent e) {

		int atom = viewer.findNearestAtomIndex(e.getX(), e.getY());		
		int bond = viewer.findNearestBondIndex(e.getX(), e.getY());
		if(bond>=0) atom = viewer.getMolecule().getBondAtom(0, bond);
		if(atom<0) return;
		markLigand(viewer, atom);
		
	}
	
	public static void markLigand(final MoleculeViewer viewer, int atom) {
		List<Integer> res = new ArrayList<Integer>();
		final FFMolecule mol = viewer.getMolecule();
		
		mol.setAllAtomFlag(FFMolecule.LIGAND, false);
		mol.setAllAtomFlag(FFMolecule.RIGID, true);
		
		
		int[] atog = StructureCalculator.getAtomToGroups(mol);
		
		for (int i = 0; i < atog.length; i++) {
			if(atog[i] == atog[atom]) res.add(i);
		}
		
		for (int a: res) {
			mol.setAtomFlag(a, FFMolecule.LIGAND, true);			
			mol.setAtomFlag(a, FFMolecule.RIGID, false);			
		}
		mol.reorderAtoms();
//		viewer.saveUndoStep();
		viewer.setMolecule(mol);

	}
	
	public static void markProtein(MoleculeViewer viewer, int atom) {
		
		List<Integer> res = new ArrayList<Integer>();
		FFMolecule mol = viewer.getMolecule();
		
//		mol.setAllAtomFlag(FFMolecule.LIGAND, false);
//		mol.setAllAtomFlag(FFMolecule.RIGID, true);
		
		
		int[] atog = StructureCalculator.getAtomToGroups(mol);
		
		for (int i = 0; i < atog.length; i++) {
			if(atog[i] == atog[atom]) res.add(i);
		}
		
		for (int a: res) {
			mol.setAtomFlag(a, FFMolecule.LIGAND, false);			
			mol.setAtomFlag(a, FFMolecule.RIGID, true);			
		}
		mol.reorderAtoms();
//		viewer.saveUndoStep();		
		viewer.setMolecule(mol);

	}

	
	
}
