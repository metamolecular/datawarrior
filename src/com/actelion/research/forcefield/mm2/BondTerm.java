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
package com.actelion.research.forcefield.mm2;

import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.forcefield.TermList;

/**
 * 
 */
public final class BondTerm extends AbstractTerm implements Cloneable  {
	private final static FFParameters parameters = MM2Parameters.getInstance();
	
	private final static double BOND_UNIT = 71.94;
	protected double Kb, eq;
	private double rab;	
	private double energy;	

	private BondTerm(FFMolecule mol, int[] atoms, double Kb, double eq) {
		super(mol, atoms);
		this.Kb = Kb;
		this.eq = eq;
	}
	
	public static BondTerm create(TermList tl, int a1, int a2) {
		
		int n1 = tl.getMolecule().getAtomMM2Class(a1);
		int n2 = tl.getMolecule().getAtomMM2Class(a2);
		int[] atoms = new int[]{a1, a2};
		
		FFParameters.BondParameters params = parameters.getBondParameters(n1, n2);
		double Kb = params.fc;
		double eq = tl.getBondDistance(a1, a2);
		if(Kb>0) return new BondTerm(tl.getMolecule(), atoms, Kb, eq);
		return null;
	}
	
	
	@Override
	public final double getFGValue(final Coordinates[] gradient) {
		final Coordinates ca = getMolecule().getCoordinates(atoms[0]);
		final Coordinates cb = getMolecule().getCoordinates(atoms[1]);
		final Coordinates cab = ca.subC(cb);
		rab = cab.dist();
		if(rab==0) rab = 0.1;
		final double dt = rab - eq;
		final double dt2 = dt*dt;
		 
		energy = BOND_UNIT * Kb * dt2;
		
		if(gradient!=null) {
			double deddt = 2 * BOND_UNIT * Kb * dt;
			double de = deddt/rab;		
			if(atoms[0]<gradient.length) gradient[atoms[0]].add(cab.scaleC(de));
			if(atoms[1]<gradient.length) gradient[atoms[1]].add(cab.scaleC(-de)); 						
		}
					
		return energy;
	}
	
	@Override
	public final String toString() {
		return "Bond        " +new DecimalFormat("00").format(atoms[0])+" - "+new DecimalFormat("00").format(atoms[1])+"    " + new DecimalFormat("0.0000").format(eq) + " "+new DecimalFormat("0.0000").format(rab)+ " -> "+new DecimalFormat("0.0000").format(energy);
	}

	public final boolean isUsed() {
		return Kb>0;
	}
	
}
