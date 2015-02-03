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
package com.actelion.research.chem.calculator;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;


/**
 * Utility class to perform 3D geometry calculations on molecules
 */
public class GeometryCalculator {
	
	public final static Coordinates getCoordinates(FFMolecule mol, int atm) {
		return new Coordinates(mol.getAtomX(atm), mol.getAtomY(atm), mol.getAtomZ(atm));
	}
	public final static Coordinates getCoordinates(Molecule mol, int atm) {
		return new Coordinates(mol.getAtomX(atm), mol.getAtomY(atm), mol.getAtomZ(atm));
	}
	
	public final static void setCoordinates(FFMolecule mol, int atm, Coordinates c) {
		mol.setAtomX(atm, c.x);
		mol.setAtomY(atm, c.y);
		mol.setAtomZ(atm, c.z);
	}

	public final static double getDistance(FFMolecule mol, int atom1, int atom2) {
		return Math.sqrt(getDistanceSquare(mol, atom1, atom2));		
	}
	
	/**
	 * Gets the square of the distance between 2 atoms
	 * @param mol
	 * @param atom1
	 * @param atom2
	 * @return
	 */
	public final static double getDistanceSquare(FFMolecule mol, int atom1, int atom2) {
		double dx = mol.getAtomX(atom1) - mol.getAtomX(atom2);
		double dy = mol.getAtomY(atom1) - mol.getAtomY(atom2);
		double dz = mol.getAtomZ(atom1) - mol.getAtomZ(atom2);
		return dx*dx + dy*dy + dz*dz;		
	}
	
	/**
	 * Gets the Angle between 3 atoms
	 * @param mol
	 * @param u1
	 * @param u2
	 * @param v1
	 * @param v2
	 * @return the angle
	 */
	public final static double getAngle(FFMolecule mol, int a1, int a2, int a3) {
		Coordinates c1 = GeometryCalculator.getCoordinates(mol, a1);
		Coordinates c2 = GeometryCalculator.getCoordinates(mol, a2);
		Coordinates c3 = GeometryCalculator.getCoordinates(mol, a3);

		return c1.subC(c2).getAngle(c3.subC(c2));
	}
	
	public final static double getAngle(Coordinates c1, Coordinates c2, Coordinates c3) {
		return c1.subC(c2).getAngle(c3.subC(c2));
	}
	
	public final static double getAngle(Molecule mol, int a1, int a2, int a3) {
		Coordinates c1 = GeometryCalculator.getCoordinates(mol, a1);
		Coordinates c2 = GeometryCalculator.getCoordinates(mol, a2);
		Coordinates c3 = GeometryCalculator.getCoordinates(mol, a3);

		return c1.subC(c2).getAngle(c3.subC(c2));
	}
		
  	/**
	 * Gets the Dihedral Angle between 4 atoms
	 * @param mol
	 * @param u1
	 * @param u2
	 * @param v1
	 * @param v2
	 * @return the angle
	 */
	public final static double getDihedral(FFMolecule mol, int a1, int a2, int a3, int a4) {
		Coordinates c1 = GeometryCalculator.getCoordinates(mol, a1);
		Coordinates c2 = GeometryCalculator.getCoordinates(mol, a2);
		Coordinates c3 = GeometryCalculator.getCoordinates(mol, a3);
		Coordinates c4 = GeometryCalculator.getCoordinates(mol, a4);
		return c1.getDihedral(c2, c3, c4);
	}
		
	/**
	 * Gets the center of Gravity of a molecule
	 * @param mol
	 * @return
	 */
	public final static Coordinates getCenterGravity(FFMolecule mol) {
		Coordinates c = new Coordinates();
		for(int i=0; i<mol.getAllAtoms(); i++) {
			c.x += mol.getAtomX(i); 
			c.y += mol.getAtomY(i); 
			c.z += mol.getAtomZ(i); 
		}
		c.x /= mol.getAllAtoms();
		c.y /= mol.getAllAtoms();
		c.z /= mol.getAllAtoms();
		
		return c;
	}
	
	/**
	 * Gets the Bounds of a molecule
	 * @param molecule
	 * @return an Array of Coordinares [lowerBounds, upperbounds]
	 */
	public final static Coordinates[] getBounds(FFMolecule molecule) {
		if(molecule.getAllAtoms()==0) return new Coordinates[]{new Coordinates(0, 0, 0), new Coordinates(0, 0, 0)};
		Coordinates[] coords = new Coordinates[]{new Coordinates(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE), new Coordinates(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)};
		for(int i=0; i<molecule.getAllAtoms(); i++) {
			coords[0].x = Math.min(coords[0].x, molecule.getAtomX(i)); 
			coords[0].y = Math.min(coords[0].y, molecule.getAtomY(i)); 
			coords[0].z = Math.min(coords[0].z, molecule.getAtomZ(i)); 

			coords[1].x = Math.max(coords[1].x, molecule.getAtomX(i)); 
			coords[1].y = Math.max(coords[1].y, molecule.getAtomY(i)); 
			coords[1].z = Math.max(coords[1].z, molecule.getAtomZ(i)); 
		}
		return coords;
	}		

	/**
	 * Utility function used to calculate bounds given previous bounds and new coordinates 
	 * 
	 * @param bounds
	 * @param coords
	 * @return
	 */	
	public final static Coordinates[] getBounds(Coordinates[] bounds, Coordinates coords) {
		if(bounds==null) bounds = new Coordinates[]{new Coordinates(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE), new Coordinates(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)};
		bounds[0].x = Math.min(bounds[0].x, coords.x); 
		bounds[0].y = Math.min(bounds[0].y, coords.y); 
		bounds[0].z = Math.min(bounds[0].z, coords.z); 

		bounds[1].x = Math.max(bounds[1].x, coords.x); 
		bounds[1].y = Math.max(bounds[1].y, coords.y); 
		bounds[1].z = Math.max(bounds[1].z, coords.z); 

		return bounds;
	}		
	
	/**
	 * Translate a Molecule
	 * @param molecule
	 * @param c
	 */
	public final static void translate(FFMolecule molecule, Coordinates c) {
		for(int i=0; i<molecule.getAllAtoms(); i++) {
			molecule.setAtomX(i, molecule.getAtomX(i)+c.x);
			molecule.setAtomY(i, molecule.getAtomY(i)+c.y);
			molecule.setAtomZ(i, molecule.getAtomZ(i)+c.z);
		}
	}
	
	/**
	 * Translate a Molecule around the origin according to the given normal vector
	 * @param molecule
	 * @param c
	 */
	public final static void rotate(FFMolecule molecule, Coordinates normal, double angle) {
		for(int i=0; i<molecule.getAllAtoms(); i++) {
			Coordinates c = getCoordinates(molecule, i).rotate(normal, angle);
			molecule.setAtomX(i, c.x);
			molecule.setAtomY(i, c.y);
			molecule.setAtomZ(i, c.z);
		}
	}
	
	/**
	 * Given 2 molecules with the same atoms, return a value indicating how 
	 * different are the coordinates of 2 molecules.
	 * 
	 * The Value returned is computer as the average of the square of the difference 
	 * between all the bonds.
	 * Experimentally, a value less than 0.001A means that the 2 molecules are the same and bigger
	 * than 0.007A means that they are different (between... it depends). Statistically 98.5% 
	 * of molecules are outside this range 
	 * 
	 */
	public static double calculateDiff(FFMolecule mol1, FFMolecule mol2) {
		if(mol1.getAllAtoms()==1) return 0;
		double sum = 0;
		int no = 0;
		for(int i=0; i<mol1.getAllAtoms(); i++) {
			//if(mol1.getAtomicNo(i)==6) continue; //Reduce the importance of C
			for(int j=0; j<mol1.getAllConnAtoms(i); j++) {
				int atom2 = mol1.getConnAtom(i, j);
				double diff = getDistance(mol1, i, atom2) - getDistance(mol2, i, atom2);
				sum += diff * diff;
				no++;
			}
		}				
		return sum / no;
	}

	
}
