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

/**
 * 
 */
public final class TorsionTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	private static final double TORSION_UNIT = .5;
	protected double initV1, initV2, initV3;
	protected double v1, v2, v3;
	private double energy;	
	
	private TorsionTerm(FFMolecule mol, int[] atoms, FFParameters.TorsionParameters params) {
		super(mol, atoms);
		
		initV1 = v1 = params.v1;
		initV2 = v2 = params.v2;
		initV3 = v3 = params.v3;
		
	}
	
	public static TorsionTerm create(MM2TermList tl, int a1, int a2, int a3, int a4) {
		int[] atoms = new int[]{a1, a2, a3, a4};
		FFMolecule mol = tl.getMolecule();
		int n1 = mol.getAtomMM2Class(a1);
		int n2 = mol.getAtomMM2Class(a2);
		int n3 = mol.getAtomMM2Class(a3);
		int n4 = mol.getAtomMM2Class(a4);
		int ringSize = mol.getRingSize(atoms);
		FFParameters.TorsionParameters params = parameters.getTorsionParameters(n1, n2, n3, n4, ringSize);
		if(params!=null) {
			return new TorsionTerm(mol, atoms, params);
		}
		return null;
	}
	
	public double[] getTorsionTerms() {
		return new double[]{initV1, initV2, initV3};
	}
	
	@Override
	public final double getFGValue(final Coordinates[] gradient) {
		final Coordinates ca = getMolecule().getCoordinates(atoms[0]);
		final Coordinates cb = getMolecule().getCoordinates(atoms[1]);
		final Coordinates cc = getMolecule().getCoordinates(atoms[2]);
		final Coordinates cd = getMolecule().getCoordinates(atoms[3]);

		final Coordinates cba = cb.subC(ca);
		final Coordinates ccb = cc.subC(cb);
		final Coordinates cdc = cd.subC(cc);

		final Coordinates ct = cba.cross(ccb);
		final Coordinates cu = ccb.cross(cdc);
		final Coordinates ctu = ct.cross(cu);			

		double rt2 = ct.distSq();
		if(rt2<0.1) rt2 = 0.1;
		double ru2 = cu.distSq();
		if(ru2<0.1) ru2 = 0.1;

		double rtru = Math.sqrt(rt2*ru2);
		if(rtru==0) {
			energy = 0; 
		} else {
		
			double rcb = ccb.dist();
			if(rcb<0.1) rcb = 0.1;
			double cosine = ct.dot(cu) / rtru; 
			double sine = ccb.dot(ctu) / (rcb*rtru); 
	
			double cosine2 = cosine*cosine - sine*sine; 	//cos(2phi)
			double sine2 = 2*cosine*sine; 				   //sin(2phi)
			double cosine3 = cosine*cosine2 - sine*sine2; //cos(3phi)
			double phi1 = 1 + cosine; 
			double phi2 = 1 - cosine2; //always a minus sign for MM2
			double phi3 = 1 + cosine3; 
			energy = TORSION_UNIT * (v1 * phi1 + v2 * phi2 + v3 * phi3);
			
			if(gradient!=null) {
				double sine3 = cosine*sine2 + sine*cosine2;  //sin(3phi)
				double dphi1 = -sine;
				double dphi2 = 2 * sine2;
				double dphi3 = 3 * -sine3; 
				double dedphi = TORSION_UNIT * (v1 * dphi1 + v2 * dphi2 + v3 * dphi3);
		
				Coordinates cca = cc.subC(ca);
				Coordinates cdb = cd.subC(cb);
		
				Coordinates dedt = ct.cross(ccb).scaleC(dedphi/(rt2*rcb));
				Coordinates dedu = cu.cross(ccb).scaleC(-dedphi/(ru2*rcb));
				
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(dedt.cross(ccb));
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(cca.cross(dedt).addC(dedu.cross(cdc)));
				if(atoms[2]<gradient.length) gradient[atoms[2]].add(dedt.cross(cba).addC(cdb.cross(dedu)));
				if(atoms[3]<gradient.length) gradient[atoms[3]].add(dedu.cross(ccb));
			}			
			
		}
		return energy;
	}

	@Override
	public final String toString() {
		return "Torsion    " +
			new DecimalFormat("00").format(atoms[0]) + " - " + 
			new DecimalFormat("00").format(atoms[1]) + " - " + 
			new DecimalFormat("00").format(atoms[2]) + " - " + 
			new DecimalFormat("00").format(atoms[3]) + "   -> " + 
			new DecimalFormat("0.0000").format(energy);
	}

	public final boolean isUsed() {
		return v1!=0 || v2!=0 || v3!=0;
	}

}
