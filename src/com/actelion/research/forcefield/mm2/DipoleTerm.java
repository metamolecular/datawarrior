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

import com.actelion.research.chem.*;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.util.MathUtils;

/**
 * 
 */
public class DipoleTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	public static double DIELECTRIC = 10; //1 in vaccum, 78 in water. Here is a good compromise
	
	private static final double CUTOFF = 10.0;
	private static final double TAPER_CUTOFF = CUTOFF * .9;
	private static final double TAPER_COEFFS[] = MathUtils.getTaperCoeffs(CUTOFF, TAPER_CUTOFF);
	
	private final double Fik;
	private final boolean isInterMolecular;
	private double energy;	

	private DipoleTerm(FFMolecule mol, int[] atoms, double Fik) {
		super(mol, atoms);
		isInterMolecular = (getMolecule().isAtomFlag(atoms[0], FFMolecule.LIGAND) && !getMolecule().isAtomFlag(atoms[2], FFMolecule.LIGAND)) ||
			(getMolecule().isAtomFlag(atoms[2], FFMolecule.LIGAND) && !getMolecule().isAtomFlag(atoms[0], FFMolecule.LIGAND));		
		this.Fik = Fik;
		
	}

	protected static DipoleTerm create(MM2TermList tl, int a1, int a2, int a3, int a4) {
		
		int n1 = tl.getMolecule().getAtomMM2Class(a1);
		int n2 = tl.getMolecule().getAtomMM2Class(a2);
		int n3 = tl.getMolecule().getAtomMM2Class(a3);
		int n4 = tl.getMolecule().getAtomMM2Class(a4);
			
		double Fik = 14.39 / DIELECTRIC 
				* parameters.getDipoleParameters(n1, n2) 
				* parameters.getDipoleParameters(n3, n4);
		
		if(Fik!=0) {
			int[] atoms = new int[]{a1, a2, a3, a4};
			return new DipoleTerm(tl.getMolecule(), atoms, Fik);
		}
		return null;
	}
	

	private double r;
	
	/**
	 * The Dipole Energy equals:
	 * 	E = F * u1 * u2 [cos X - 3 cos alpha1 cos alpha2] / R^3
	 * where:
	 * 	F =  constant
	 *  u1, u2 = moments depending of the bonds
	 *  X = angle
	 *  alpha2, alpha2: angles
	 *  R : distance between bonds
	 * 
	 */
	@Override
	public double getFGValue(final Coordinates[] gradient) {		
		//First Bond
		final Coordinates ci1 = getMolecule().getCoordinates(atoms[0]);
		final Coordinates ci2 = getMolecule().getCoordinates(atoms[1]);
		final Coordinates ci = ci2.subC(ci1);
		double ri2 = ci.distSq();
		if(ri2<0.3) ri2 = 0.3;		
		final Coordinates cq = ci1.addC(ci2).scaleC(0.5);
		 
		//Second Bond
		final Coordinates ck1 = getMolecule().getCoordinates(atoms[2]);
		final Coordinates ck2 = getMolecule().getCoordinates(atoms[3]);							
		final Coordinates ck = ck2.subC(ck1);
		double rk2 = ck.distSq();
		if(rk2<.3) rk2 = .3;
		final Coordinates cm = ck1.addC(ck2).scaleC(0.5);
		
		final Coordinates cr = cq.subC(cm); //Vector between the 2 bonds 						
		double r2 = cr.distSq();
		
		if(r2>CUTOFF*CUTOFF) {
			//More than offset distance, don't compute
			energy = 0;
		} else {	
			if(r2<.3) r2 = .3; //avoid extremes
			
			final double rirkr3 = Math.sqrt(ri2*rk2*r2) * r2;
			final double dotp = ci.dot(ck); //xi*xk + yi*yk + zi*zk = cos X * rk * ri
			final double doti = ci.dot(cr); //xi*xr + yi*yr + zi*zr = cos alpha1 * ri * r
			final double dotk = ck.dot(cr); //xk*xr + yk*yr + zk*zr = cos alpha2 * rk * r
			
			energy = Fik * (dotp - 3.0*doti*dotk/r2) / rirkr3; 


			double taper, dtaper;
			if(r2>TAPER_CUTOFF*TAPER_CUTOFF) {
				r = Math.sqrt(r2);
				taper = MathUtils.evaluateTaper(TAPER_COEFFS, r);
				dtaper = MathUtils.evaluateDTaper(TAPER_COEFFS, r);
			} else {
				taper = 1;
				dtaper = 0;				
			}
			
			if(gradient!=null) {
				final double de = - Fik  / (rirkr3 * r2); 
				final double deddotp = -de * r2;
				final double deddoti = de * 3 * dotk;
				final double deddotk = de * 3 * doti;
				final double dedr = de * (3 * dotp - 15 * doti * dotk / r2); 

				final double enu = dotp*r2 - 3.0 * doti*dotk;
				final double dedrirk = de * enu;
				
				final Coordinates term = cr.scaleC(dedr).addC(ci.scaleC(deddoti)).addC(ck.scaleC(deddotk));
				final Coordinates termi = ci.scaleC(dedrirk/ri2).addC(ck.scaleC(deddotp)).addC(cr.scaleC(deddoti));
				final Coordinates termk = ck.scaleC(dedrirk/rk2).addC(ci.scaleC(deddotp)).addC(cr.scaleC(deddotk));
				
				Coordinates g0, g1, g2, g3;
				if(dtaper==0) {
					g0 = term.scaleC(0.5).subC(termi);
					g1 = term.scaleC(0.5).addC(termi);
					g2 = term.scaleC(-0.5).subC(termk);
					g3 = term.scaleC(-0.5).addC(termk);
				} else {
					Coordinates dtap = cr.scaleC(dtaper*energy/r);
					g0 = term.scaleC(0.5).subC(termi).scaleC(taper).addC(dtap.scaleC(0.5));
					g1 = term.scaleC(0.5).addC(termi).scaleC(taper).addC(dtap.scaleC(0.5));
					g2 = term.scaleC(-0.5).subC(termk).scaleC(taper).subC(dtap.scaleC(0.5));
					g3 = term.scaleC(-0.5).addC(termk).scaleC(taper).subC(dtap.scaleC(0.5));
				}

				if(atoms[0]<gradient.length) gradient[atoms[0]].add(g0);
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(g1);
				if(atoms[2]<gradient.length) gradient[atoms[2]].add(g2);
				if(atoms[3]<gradient.length) gradient[atoms[3]].add(g3);
				
			}
			energy*=taper;

		}		
		return energy;
	}

	
	@Override
	public String toString() {
		return "Dipole    " +
			new DecimalFormat("00").format(atoms[0]) + " - " + 
			new DecimalFormat("00").format(atoms[1]) + " / " + 
			new DecimalFormat("00").format(atoms[2]) + " - " + 
			new DecimalFormat("00").format(atoms[3]) + "    " + 
			new DecimalFormat("0.0000").format(r) + " -> " + 
			new DecimalFormat("0.0000").format(energy);
	}
	
	@Override	
	public boolean isExtraMolecular() {
		return isInterMolecular;
	}
}
