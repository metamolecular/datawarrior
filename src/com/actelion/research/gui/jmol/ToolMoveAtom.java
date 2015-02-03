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

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;

/**
 * 
 */
public class ToolMoveAtom extends ToolEdit {
	
	private boolean quickMode;
	private int draggedAtom;
	private Coordinates coordinatesAngstroms;
	private Coordinates before;
	
	public ToolMoveAtom() {
		super("Move Atom");
		quickMode = false;
	}
	public ToolMoveAtom(MoleculeViewer viewer, int atom) {
		super("Move Atom");
		quickMode = true;
		viewer.setTool(this);
		viewer.addMouseListener(this);
		viewer.addMouseMotionListener(this);
		//Point pt = viewer.getMousePosition();
		startMove(atom);
	}
	
	@Override
	protected boolean hover(int atom, int bond) {
		return super.hover(atom, bond) && bond<0;
	}
	
	
	@Override
	protected void activateTool(MoleculeViewer viewer) {
		super.activateTool(viewer);
		draggedAtom = -1;
	}
	

	@Override
	public void mousePressed(MouseEvent e) {
		viewer.getMolecule().reorderAtoms();
		if(viewer==null) return;
		int atom = viewer.findNearestAtomIndex(e.getX(), e.getY());
		startMove(atom);
	}
	public void startMove(int atom) {
		if(viewer==null) return;
		FFMolecule mol = viewer.getMolecule();
		//Select Atom->1st click
		if(atom<0 || mol.isAtomFlag(atom, FFMolecule.RIGID)) return;			
		draggedAtom = atom;		
		before = mol.getCoordinates(draggedAtom);
		viewer.stopRotation(true);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if(viewer==null) return;
		if(draggedAtom>=0){
			draggedAtom = -1;
			optimize(true);
//			viewer.saveUndoStep();
		}
		viewer.stopRotation(false);
		if(quickMode) {
			deactivateTool(viewer);
		}
	}
	
	
	@Override
	public void mouseDragged(MouseEvent e) {
		mouseMoved(e);
	}
	
	
	
	@Override
	public void mouseMoved(MouseEvent e) {
		if(viewer==null) return;
		if(draggedAtom<0) return;
		
		FFMolecule mol = viewer.getMolecule();
		
		Coordinates coordinatesAngstromsBefore = mol.getCoordinates(draggedAtom);
		coordinatesAngstroms = getCoordinatesAngtroms(e, before);
		mol.getCoordinates(draggedAtom).x = coordinatesAngstroms.x;
		mol.getCoordinates(draggedAtom).y = coordinatesAngstroms.y;
		mol.getCoordinates(draggedAtom).z = coordinatesAngstroms.z;
		
		
		for (int i = 0; i < mol.getAllConnAtoms(draggedAtom); i++) {
			int a = mol.getConnAtom(draggedAtom, i);
			
			if(mol.getAtomicNo(a)<=1) {
				Coordinates c = mol.getCoordinates(a).addC(coordinatesAngstroms.subC(coordinatesAngstromsBefore));
				
				mol.getCoordinates(a).x = c.x;				
				mol.getCoordinates(a).y = c.y;				
				mol.getCoordinates(a).z = c.z;				
			}
		}
		viewer.refresh();
		
	}

	
}
