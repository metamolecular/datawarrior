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
package com.actelion.research.forcefield;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;

/**
 * A Term is used to calculate the energy for some aspect of the forcefield
 *  
 */
public abstract class AbstractTerm implements Cloneable {
	
	protected static final double RADIAN = 180 / Math.PI;
	
	protected FFMolecule mol;
	protected int atoms[];	
	
	public final FFMolecule getMolecule() {
		return mol;
	}
	
	public AbstractTerm(FFMolecule mol) {
		this.mol = mol;
	}	
	public AbstractTerm(FFMolecule mol, final int[] atoms) {
		this.mol = mol;
		this.atoms = atoms;
	}	
	
	/**
	 * Computes the Force (energy) and Gradient of this term at the corresponding atoms
	 * 
	 * 
	 * @param gradient an array of Coordinates, which will set to the gradient of each atom, (if null, the gradient will not be calculated) 
	 * @return
	 */
	public abstract double getFGValue(final Coordinates[] gradient);
	
	
	@Override
	public String toString() {
		String s = "[";
		for(int i=0; i<atoms.length; i++) {
			if(i>0) s+=", ";
			s += atoms[i] + " " + mol.getCoordinates(atoms[i]);
		}
		return s+"]";
	}
	
	public boolean isExtraMolecular() {
		return false;
	}
	
	@Override
	public AbstractTerm clone() {
		try {
			AbstractTerm t = (AbstractTerm) super.clone();
			t.mol = mol;
			return t;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	

	public final int[] getAtoms() {
		return atoms;
	}
	

	protected final static double sqrt(double d) {
		return d<=0? 0: Math.sqrt(d);
	}


	
	
	


}
