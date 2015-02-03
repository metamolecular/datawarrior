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
package com.actelion.research.forcefield.optimizer;

import com.actelion.research.chem.*;
import com.actelion.research.forcefield.*;


/**
 * Transformation used to optimize the 3D cartesian coordinates of the ligand:
 * (degrees of freedom=3*nAtoms)
 * @author freyssj
 */
public class EvaluableForceField implements IEvaluable {
	protected ForceField forcefield;
	
	public EvaluableForceField(EvaluableForceField e) {
		this.forcefield = e.forcefield;		
	}
	public EvaluableForceField(ForceField forcefield) {
		this.forcefield = forcefield;
	}
	
	@Override
	public void setState(MultiVariate v) {
		FFMolecule mol = forcefield.getMolecule();
		for(int i=0, a = 0; i<v.vector.length; i+=3, a++) {
			if(!mol.isAtomFlag(a, FFMolecule.RIGID)) mol.setCoordinates(a, new Coordinates(v.vector[i], v.vector[i+1], v.vector[i+2]));
		}
	}
	@Override
	public MultiVariate getState() {
		FFMolecule mol = forcefield.getMolecule();
		MultiVariate v = new MultiVariate(mol.getNMovables()*3);
		for(int i=0, a = 0; a<mol.getNMovables(); a++) {
			Coordinates c = mol.getCoordinates(a);
			v.vector[i++] = c.x;
			v.vector[i++] = c.y;
			v.vector[i++] = c.z;
		}
		return v;
	}
	@Override
	public double getFGValue(MultiVariate grad) {
		FFMolecule mol = forcefield.getMolecule();
		//Compute the Gradient in the cartesian referential			
		Coordinates[] g = new Coordinates[mol.getNMovables()];
		for(int i=0; i<g.length; i++) g[i] = new Coordinates(); 
		double e = forcefield.getTerms().getFGValue(g);
	
		for(int i=0, a = 0; i<grad.vector.length; a++) {
			grad.vector[i++] = g[a].x;
			grad.vector[i++] = g[a].y;
			grad.vector[i++] = g[a].z;
		}
		return e;		
	}
	
	/**
	 * @see com.actelion.research.forcefield.optimizer.IEvaluable#clone()
	 */
	@Override
	public EvaluableForceField clone() {
		return new EvaluableForceField(this);
	}

		
}