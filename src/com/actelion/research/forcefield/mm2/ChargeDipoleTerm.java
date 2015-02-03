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

import java.text.*;

import com.actelion.research.chem.*;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.util.MathUtils;


/**
 * 
 */
public final class ChargeDipoleTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	private final static double CUTOFF = 9;
	private final static double FACTOR = 1;

	private final static double TAPER_CUTOFF = CUTOFF * .9;
	private final static double TAPER_COEFFS[] = MathUtils.getTaperCoeffs(CUTOFF, TAPER_CUTOFF);
	
	private final double product; 
	private double r2, rkr3, dotk, rk2;
	private double energy;	

	private ChargeDipoleTerm(FFMolecule mol, int[] atoms, double product) {
		super(mol, atoms);
		this.product = product;	
	}
	
	protected static ChargeDipoleTerm create(MM2TermList tl, int a1, int a2, int a3) {
		
		int n1 = tl.getMolecule().getAtomMM2Class(a1);
		int n2 = tl.getMolecule().getAtomMM2Class(a2);
		int n3 = tl.getMolecule().getAtomMM2Class(a3);
		 
		double charge = n3>=0? parameters.getAtomClass(n3).charge: 0; 		
		double product = FACTOR * parameters.getDipoleParameters(n1, n2) * charge;
		if(product!=0) {
			int[] atoms = new int[]{a1, a2, a3};
			return new ChargeDipoleTerm(tl.getMolecule(), atoms, product);
		}
		return null;
	}
	
	
	@Override
	public final double getFGValue(Coordinates[] gradient) {
		Coordinates ci1 = getMolecule().getCoordinates(atoms[0]);
		Coordinates ci2 = getMolecule().getCoordinates(atoms[1]);
		Coordinates ci3 = getMolecule().getCoordinates(atoms[2]);
		Coordinates ci1ci2 = ci2.subC(ci1);
		
		Coordinates cr = ci3.subC(ci1.addC(ci2).scaleC(0.5));

		r2 = cr.distSq();
		if(r2==0) r2 = 0.1;

		if(r2>CUTOFF) energy = 0;
		else {
			rk2 = ci1ci2.distSq();
			rkr3 = Math.sqrt(rk2 * r2) * r2;
			dotk = ci1ci2.dot(cr);
			
			double taper, dtaper;			
			if(r2>TAPER_CUTOFF*TAPER_CUTOFF) {
				//close to the cutoff distance, apply smoothing effect
				double r = Math.sqrt(r2);
			  	taper = MathUtils.evaluateTaper(TAPER_COEFFS, r);
				dtaper = MathUtils.evaluateDTaper(TAPER_COEFFS, r);
			} else {
				taper = 1;
				dtaper = 0;
			}			
			//= product * (B-A).(C-(B+A)/2) / (||B-A|| * ||C-(B+A)/2||^(3/2)) 
			double term = product / rkr3;		
			energy = term * dotk;  
			energy*=taper;
			
			if(gradient!=null) {
				Coordinates t = ci1ci2.addC(cr.scaleC(-3 * dotk / r2)).scaleC(term);
				Coordinates tk = cr.subC(ci1ci2.scaleC(dotk/rk2)).scaleC(term);
				
				Coordinates g0, g1, g2;
				if(dtaper==0) {
					g0 = t.scaleC(0.5).subC(tk);
					g1 = t.scaleC(0.5).addC(tk);
					g2 = t;
				} else {
					Coordinates dtap = cr.scaleC(dtaper);
					g0 = t.scaleC(-0.5).subC(tk).scaleC(taper).subC(dtap.scaleC(.5));
					g1 = t.scaleC(-0.5).addC(tk).scaleC(taper).subC(dtap.scaleC(.5));
					g2 = t.scaleC(taper).addC(dtap);
				}

								
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(g0);
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(g1);
				if(atoms[2]<gradient.length) gradient[atoms[2]].add(g2);								
			}			
		}
		return energy;
	}
	
	@Override
	public final String toString() {
		return "Charge-Dipole    " + atoms[0] + "-" + atoms[1] + " / " + atoms[2] + "   " + 
			new DecimalFormat("0.0000").format(Math.sqrt(rk2)) + " -> " + 
			new DecimalFormat("0.0000").format(energy);
	}

	public final boolean isUsed() {
		return product!=0;
	}
	
}
