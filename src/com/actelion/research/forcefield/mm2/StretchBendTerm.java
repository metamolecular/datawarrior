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
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.forcefield.FastMath;

/**
 * 
 */
public final class StretchBendTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	private final double eqAngle;
	private final BondTerm t1, t2;
	private final double Kbend;
	private double energy;	

	
	private StretchBendTerm(FFMolecule mol, int[] atoms, double Kbend, double eqAngle, BondTerm t1, BondTerm t2) {
		super(mol, atoms);
		this.Kbend = Kbend;
		this.t1 = t1;
		this.t2 = t2;
		this.eqAngle = eqAngle;
	}
	
	protected static StretchBendTerm create(MM2TermList tl, int a1, int a2, int a3, BondTerm t1, BondTerm t2) {
		int[] atoms = new int[]{a1,a2,a3};
		FFMolecule mol = tl.getMolecule();
		int n1 = mol.getAtomMM2Class(a1);
		int n2 = mol.getAtomMM2Class(a2);
		int n3 = mol.getAtomMM2Class(a3);
		
		int nH = 0;		
		if(mol.getAtomicNo(a1)<=1) nH++;
		if(mol.getAtomicNo(a3)<=1) nH++;
		double Kbend = 2.51118 * parameters.getStretchBendParameter(n2, nH);

		int nHydro = StructureCalculator.getExplicitHydrogens(mol, a2);
		if(mol.getAtomicNo(a1)==1) nHydro--;
		if(mol.getAtomicNo(a3)==1) nHydro--;
		int ringSize = mol.getRingSize(atoms);
		
		FFParameters.AngleParameters angleParams = parameters.getAngleParameters(n1, n2, n3, nHydro, ringSize);
		double eqAngle = angleParams!=null? angleParams.eq: 0; 
		
		
		if(Kbend!=0 && eqAngle>0 && t1!=null && t2!=null) {
			return new StretchBendTerm(mol, atoms, Kbend, eqAngle, t1, t2);
		}
		return null; 
	}
	
	private double dr, dt;
	
	@Override
	public final double getFGValue(Coordinates[] gradient) {
		Coordinates ca = getMolecule().getCoordinates(atoms[0]);
		Coordinates cb = getMolecule().getCoordinates(atoms[1]);
		Coordinates cc = getMolecule().getCoordinates(atoms[2]);
		
		Coordinates cab = ca.subC(cb);
		Coordinates ccb = cc.subC(cb);
		double rab = cab.dist();
		double rcb = ccb.dist();
		
		Coordinates cp = ccb.cross(cab);
		double rp = cp.dist();
		if(rp>0) {
			double cosine = cab.dot(ccb) / (rab*rcb);
			double angle = RADIAN * FastMath.acos(cosine);
			dt = angle - eqAngle;
			
			dr = 0;
			if(getMolecule().getAtomicNo(atoms[0])>1) dr += rab - t1.eq;
			if(getMolecule().getAtomicNo(atoms[2])>1) dr += rcb - t2.eq;
			
			energy = Kbend * dt * dr;
			
			if(gradient!=null) {
				Coordinates ddtda = cab.cross(cp).scaleC(-RADIAN * Kbend/ (rab*rab*rp));
				Coordinates ddtdc = ccb.cross(cp).scaleC(RADIAN * Kbend / (rcb*rcb*rp));
		
				Coordinates ddrda = cab.scaleC(getMolecule().getAtomicNo(atoms[0])>1? Kbend/rab: 0);
				Coordinates ddrdc = ccb.scaleC(getMolecule().getAtomicNo(atoms[2])>1? Kbend/rcb: 0);
		
				Coordinates dedia = ddrda.scaleC(dt).addC(ddtda.scaleC(dr));		
				Coordinates dedic = ddrdc.scaleC(dt).addC(ddtdc.scaleC(dr));
				
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(dedia);
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(new Coordinates().subC(dedia).subC(dedic));
				if(atoms[2]<gradient.length) gradient[atoms[2]].add(dedic);
								
			}
			
		} else {
			energy = 0;
		}
		return energy;
	}
		
	@Override
	public final String toString() {
		return "StrBend    " +new DecimalFormat("00").format(atoms[0]) + " - " + 
			new DecimalFormat("00").format(atoms[1]) + " - " + 
			new DecimalFormat("00").format(atoms[2]) + "     " + 
			new DecimalFormat("0.0000").format(Kbend) + "  " + 
			new DecimalFormat("0.0000").format(eqAngle)+ " -> "+new DecimalFormat("0.0000").format(energy);
	}

	public final boolean isUsed() {
		return Kbend!=0 && eqAngle>0 && t1.eq>0 && t2.eq>0;
	}
	
}
