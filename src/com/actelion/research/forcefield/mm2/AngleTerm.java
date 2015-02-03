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
import com.actelion.research.forcefield.TermList;

/**
 * 
 */
public final class AngleTerm extends AbstractTerm implements Cloneable {

	private final static FFParameters parameters = MM2Parameters.getInstance();
	
	private static final double ANGLE_UNIT = 0.02191418;
	private FFParameters.AngleParameters params;
	private double angle;
	private double energy;	

	
	
	private AngleTerm(FFMolecule mol, int[] atoms, FFParameters.AngleParameters p){
		super(mol, atoms);
		params = p;
	}
	
	protected static AngleTerm create(TermList tl, int atm1, int atm2, int atm3, boolean isInPlaneAngleEnabled) {
		FFMolecule mol = tl.getMolecule();
		int n1 = mol.getAtomMM2Class(atm1);
		int n2 = mol.getAtomMM2Class(atm2);
		int n3 = mol.getAtomMM2Class(atm3);
		int[] atoms = new int[]{atm1, atm2, atm3};
		int ringSize = mol.getRingSize(atoms);
		if(isInPlaneAngleEnabled && mol.getAllConnAtoms(atm2)==3 && (parameters.getOutOfPlaneBendParameters(n1, n2)!=null || parameters.getOutOfPlaneBendParameters(n3, n2)!=null)) {
			return null; //IN PLANE Angle
		} 
		int nHydro = StructureCalculator.getExplicitHydrogens(mol, atm2);
		if(mol.getAtomicNo(atm1)==1) nHydro--;
		if(mol.getAtomicNo(atm3)==1) nHydro--;
		FFParameters.AngleParameters params = parameters.getAngleParameters(n1, n2, n3, nHydro, ringSize);
		if(params==null) return null;			
		return new AngleTerm(mol, atoms, params);		
	}
	
	
	@Override
	public final double getFGValue(final Coordinates[] gradient) {
		
		final Coordinates ca = getMolecule().getCoordinates(atoms[0]);
		final Coordinates cb = getMolecule().getCoordinates(atoms[1]);
		final Coordinates cc = getMolecule().getCoordinates(atoms[2]);
		
		final Coordinates cab = ca.subC(cb);
		final Coordinates ccb = cc.subC(cb);
		
		double rab2 = cab.distSq();
		double rcb2 = ccb.distSq();
		
		if(rab2<0.2) rab2 = 0.2;
		if(rcb2<0.2) rcb2 = 0.2;
		double cosine = cab.dot(ccb) / Math.sqrt(rab2 * rcb2); //cos(BA, BC)
		angle = RADIAN * FastMath.acos(cosine);
		double dt = angle - params.eq;				
		double dt2 = dt * dt;
//		double dt4 = dt2 * dt2;
//		energy = ANGLE_UNIT * params.fc * dt2 * (1.0 + SANG * dt4);
		energy = ANGLE_UNIT * params.fc * dt2;
		
		if(gradient!=null) {
			Coordinates cp = ccb.cross(cab);
			double rp = cp.dist();
			if(rp==0) {
				rp = 0.00001;
			} 

//			double deddt = ANGLE_UNIT * params.fc * dt * RADIAN * (2.0 + 6.0 * SANG * dt4);
			double deddt = 2*ANGLE_UNIT * params.fc * dt * RADIAN ;
			double terma = - deddt / (rab2*rp);
			double termc =   deddt / (rcb2*rp);
		
			Coordinates g0 = cab.cross(cp).scaleC(terma);
			Coordinates g2 = ccb.cross(cp).scaleC(termc);
			Coordinates g1 = new Coordinates().subC(g0).subC(g2);
			
			if(atoms[0]<gradient.length) gradient[atoms[0]].add(g0);
			if(atoms[1]<gradient.length) gradient[atoms[1]].add(g1);
			if(atoms[2]<gradient.length) gradient[atoms[2]].add(g2);
			
		}
		
		return energy;
	}
	
	public final double getPreferredAngle() {
		return params.eq;
	}
	
	@Override
	public final String toString() {
		return "Angle      " +
			new DecimalFormat("00").format(atoms[0]) + " - " + 
			new DecimalFormat("00").format(atoms[1]) + " - " + 
			new DecimalFormat("00").format(atoms[2]) + "    " + 
			new DecimalFormat("0.0000").format(params.eq) + "  " + 
			new DecimalFormat("0.0000").format(angle) + " -> " + 
			new DecimalFormat("0.0000").format(energy);
	}

	public final boolean isUsed() {
		return params!=null;
	}
}
