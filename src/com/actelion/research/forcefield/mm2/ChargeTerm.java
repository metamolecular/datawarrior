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

import com.actelion.research.chem.*;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.util.*;

/**
 * 
 */
public final class ChargeTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	private final static double CUTOFF = 9;
	private final static double TAPER_CUTOFF = CUTOFF * .9;
	private final static double TAPER_COEFFS[] = MathUtils.getTaperCoeffs(CUTOFF, TAPER_CUTOFF);
	private final static double DIELECTRIC = DipoleTerm.DIELECTRIC;
	
	private final double product; 
	private double energy;	

	private ChargeTerm(FFMolecule mol, int[] atoms, double product) {
		super(mol, atoms);
		this.product = product;
		
	}
	
	protected static ChargeTerm create(MM2TermList tl, int a1, int a2) {
		int[] atoms = new int[]{a1, a2};
		
		int n1 = tl.getMolecule().getAtomMM2Class(a1);
		int n2 = tl.getMolecule().getAtomMM2Class(a2);
		double charge1 = n1>=0? parameters.getAtomClass(n1).charge: 0; 
		double charge2 = n2>=0? parameters.getAtomClass(n2).charge: 0;
		double product = 332.05382 / DIELECTRIC * charge1 * charge2;
		
		if(product!=0) {
			return new ChargeTerm(tl.getMolecule(), atoms, product);
		}
		return null;
	}
	
	private double r;
	
	@Override
	public final double getFGValue(Coordinates[] gradient) {
		Coordinates ca = getMolecule().getCoordinates(atoms[0]);
		Coordinates cb = getMolecule().getCoordinates(atoms[1]);
		Coordinates cab = ca.subC(cb);
		r = cab.dist(); 
		if(r==0) r = 0.1;

		if(r>CUTOFF) energy = 0;
		else {
			energy = product / r;
			
			double taper, dtaper;
			if(r>TAPER_CUTOFF) { 
				//close to the cutoff distance, apply smoothing effect
				taper = MathUtils.evaluateTaper(TAPER_COEFFS, r);
				dtaper = MathUtils.evaluateDTaper(TAPER_COEFFS, r);
			} else {
				taper = 1;
				dtaper = 0; 
			}
			

			if(gradient!=null) {
			double de = energy * dtaper - product/(r*r) * taper;
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(cab.scaleC(de/r));
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(cab.scaleC(-de/r)); 						
			}
			
			
			energy *= taper;
			
		}
		return energy;
	}
		
	@Override
	public final String toString() {
		return "Charge-Charge" +super.toString();
	}

	public final boolean isUsed() {
		return product!=0;
	}

}
