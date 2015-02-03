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

import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.util.MathUtils;

/**
 * Class used to superimpose molecules
 * 
 */
public class SuperposeTerm extends AbstractTerm {

	private final static double TAPER_COEFFS[] = MathUtils.getTaperCoeffs(1.5, 0);
	private final double weight;
	
	public SuperposeTerm(FFMolecule mol, double w, int[] atoms) {
		super(mol, atoms);
		weight = w;
	}

	private double energy;
	private double rab;
	
	@Override
	public double getFGValue(Coordinates[] gradient) {
		Coordinates ca = getMolecule().getCoordinates(atoms[0]);
		Coordinates cb = getMolecule().getCoordinates(atoms[1]);
		Coordinates cab = ca.subC(cb);
		rab = cab.dist();
		if(rab<.02) rab = .02;
		
		if(weight==0) {
			energy = Math.sqrt(rab);
			if(gradient!=null) {
				double deddt = .5/Math.sqrt(rab);
				double de = deddt/rab;		
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(cab.scaleC(de));
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(cab.scaleC(-de)); 						
			}
			
		} else {
			if(rab>1.5) return 0;
			energy = -.8*MathUtils.evaluateTaper(TAPER_COEFFS, rab);
			if(gradient!=null && rab>0) {
				double deddt = -.8*MathUtils.evaluateDTaper(TAPER_COEFFS, rab);
				double de = deddt/rab;		
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(cab.scaleC(de));
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(cab.scaleC(-de)); 						
			}
		}
		return energy;	
	}

	@Override
	public String toString() {
		return "Superpose " +
			new DecimalFormat("00").format(atoms[0]) + " - " + 
			new DecimalFormat("00").format(atoms[1]) + " -> " + 
			new DecimalFormat("0.00").format( getMolecule().getCoordinates(atoms[0]).distance(getMolecule().getCoordinates(atoms[1]))) + " -> " + 
			new DecimalFormat("0.000").format(energy); 
	}
	
	@Override
	public boolean isExtraMolecular() {
		return true;
	}

}
