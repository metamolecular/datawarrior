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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.vecmath.Point3f;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;


/**
 * Abstract Tool used to manipulate the Canvas
 * 
 */
public abstract class AbstractTool extends MouseAdapter implements MouseMotionListener {

	private String name;
	
	public AbstractTool(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	protected MoleculeViewer viewer = null;

	protected void activateTool(MoleculeViewer viewer) {
		this.viewer = viewer;
	}
	protected void deactivateTool(MoleculeViewer viewer) {
		this.viewer = null;
		
	}

	public Coordinates getCoordinatesAngtroms(MouseEvent e, Coordinates otherAtom) {
		return getCoordinatesAngtroms(e.getX(), e.getY(), otherAtom);
	}
	public Coordinates getCoordinatesAngtroms(double x, double y, Coordinates otherAtom) {
		Point3f	draggedToAngstroms = new Point3f();
		Point3f pa = new Point3f((float) otherAtom.x, (float) otherAtom.y, (float) otherAtom.z);
		viewer.viewer.unTransformPoint(new Point3f((float)x, (float)y,  viewer.viewer.transformPoint(pa).z ), draggedToAngstroms);
		Coordinates coordinatesAngstroms = new Coordinates(draggedToAngstroms.x, draggedToAngstroms.y, draggedToAngstroms.z);
		return coordinatesAngstroms;
	}
	
	@Override
	public void mouseMoved(MouseEvent e) {}
	@Override
	public void mouseDragged(MouseEvent e) {}
	
	protected boolean hover(int atom, int bond) {
		FFMolecule m = viewer.getMolecule();
		return (atom>=0 && atom<m.getNMovables()) || (bond>=0 && m.getConnBond(0, bond)<m.getNMovables() && m.getConnBond(1, bond)<m.getNMovables());
	}
	
	
}
