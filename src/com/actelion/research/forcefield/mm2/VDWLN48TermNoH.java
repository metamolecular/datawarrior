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
import com.actelion.research.util.MathUtils;

/**
 * Smooth VDW using a 4-8 potential for heavy atoms. 
 * Very useful to speed up the docking where accuracy 
 * is not required in the approaching phase
 * 
 * energy = epsilon * (p8 - 2 * p4) where p = radmin / r
 * 
 * @author freyssj
 */
public final class VDWLN48TermNoH extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	private final static double CUTOFF = 7;
	private final static double TAPER_CUTOFF = CUTOFF * .9;
	private final static double TAPER_COEFFS[] = MathUtils.getTaperCoeffs(CUTOFF, TAPER_CUTOFF);
	private double energy;	

	
	//Parameters used 
	private final double epsilon, radmin;
	
	private VDWLN48TermNoH(FFMolecule mol, int[] atoms, double epsilon, double radmin/*, double r1, double r2/*, int iv, int kv*/) {
		super(mol, atoms);
		this.epsilon = epsilon;//*1.169;  //espilon increased to account for missing H
		this.radmin = radmin * Math.pow(2, 1/6.0);
	}
	
	protected static VDWLN48TermNoH create(MM2TermList tl, int a1, int a2) {
		FFMolecule mol = tl.getMolecule();
		int n1 = mol.getAtomMM2Class(a1);
		int n2 = mol.getAtomMM2Class(a2);
		FFParameters.SingleVDWParameters param1 = parameters.getSingleVDWParameters(n1);
		FFParameters.SingleVDWParameters param2 = parameters.getSingleVDWParameters(n2);
		if(param1==null||param2==null) return null;

		FFParameters.VDWParameters paramPair = parameters.getVDWParameters(n1, n2);
		double epsilon, radmin;
		if(paramPair!=null) {
			radmin = paramPair.radius;
			epsilon = paramPair.esp;
		}  else {					
			radmin = (param1.radius + param2.radius);			
			epsilon = Math.sqrt(param1.epsilon * param2.epsilon);				
		}
		
		int[] atoms;
		if(mol.getAtomicNo(a1)==1 || mol.getAtomicNo(a2)==1) return null;
		
		atoms = new int[]{a1, a2};
		
		return new VDWLN48TermNoH(mol, atoms, epsilon, radmin);

	}
	
	
	@Override
	public final double getFGValue(final Coordinates[] gradient) {
		Coordinates ci = getMolecule().getCoordinates(atoms[0]);
		Coordinates ck = getMolecule().getCoordinates(atoms[1]);
		double rik2 = ci.distSquareTo(ck);
		if(rik2<.8) rik2 = .8;
				
		if(rik2>CUTOFF*CUTOFF) {
			energy = 0;
		} else {
			final Coordinates cr = ci.subC(ck);
			final double rik = Math.sqrt(rik2);
			final double p2 = radmin * radmin / rik2;

			final double p4 = p2 * p2;
			final double p8 = p4 * p4;
		
			energy = epsilon * (p8 - 2 * p4);

			double taper, dtaper;
			if(rik2>TAPER_CUTOFF * TAPER_CUTOFF) { 
				//close to the cutoff distance, apply smoothing effect
				taper = MathUtils.evaluateTaper(TAPER_COEFFS, rik);
				dtaper = MathUtils.evaluateDTaper(TAPER_COEFFS, rik);				
			} else {
				taper = 1;
				dtaper = 0; 
			}
			
			if(gradient!=null) {
				double de = epsilon * (p8-p4) * (-8/rik);
				
				if(taper!=1.0) de = energy * dtaper + de * taper;
		  
				final double deddt = de/rik;
				final Coordinates g0 = cr.scaleC(deddt);
				final Coordinates g1 = cr.scaleC(-deddt);

				if(atoms[0]<gradient.length) gradient[atoms[0]].add(g0);
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(g1);
			}
			
			energy *= taper;
			
		}
		return energy;
			
	}
	
	@Override
	public final String toString() {
		return "VDW-LN      " +new DecimalFormat("00").format(atoms[0])+" - "+new DecimalFormat("00").format(atoms[1])+"    " + new DecimalFormat("0.0000").format(radmin) + " "+new DecimalFormat("0.0000").format(getMolecule().getCoordinates(atoms[0]).subC(getMolecule().getCoordinates(atoms[1])).dist())+ " -> "+new DecimalFormat("0.0000").format(energy);
	}

	public final boolean isUsed() {
		return epsilon>0;
	}


}
