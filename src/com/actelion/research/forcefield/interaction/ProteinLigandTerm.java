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
package com.actelion.research.forcefield.interaction;

import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.forcefield.mm2.MM2Parameters;

/**
 * ProteinLigandTerm is used to represent the energy between 2 atoms.
 * 
 * The function can be divider into 3 parts:
 * - Linear part for the very short distances <1A (to avoid extreme derivates)
 * - Statistics derived potential when the data is enough to have a dependable function  
 * - Lennard-Jones 8-4 VDW when stats are not available
 * 
 */
public class ProteinLigandTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	//Taper to the null function close to cutoff distance
	private final static double CUTOFF = PLFunctionSplineCalculator.CUTOFF_STATS - PLFunctionSplineCalculator.DELTA_RADIUS;	
	
	public double rik2;
	private double epsilon, radmin;
	private double energy;	

	//Statistics
	private final PLFunction f; 
	
	private ProteinLigandTerm(FFMolecule mol, int[] atoms, PLFunction f) {
		super(mol, atoms);
		this.f = f;
		
		if(f==null) {
			//VDW from parameters	
			int n1 = getMolecule().getAtomMM2Class(atoms[0]);
			int n2 = getMolecule().getAtomMM2Class(atoms[1]);
			FFParameters.VDWParameters paramPair = parameters.getVDWParameters(n1, n2);		
			if(paramPair!=null) {
				radmin = paramPair.radius;
				epsilon = paramPair.esp;
			}  else {					
				FFParameters.SingleVDWParameters param1 = parameters.getSingleVDWParameters(n1);
				FFParameters.SingleVDWParameters param2 = parameters.getSingleVDWParameters(n2);				
				radmin = (param1.radius + param2.radius);			
				epsilon = Math.sqrt(param1.epsilon * param2.epsilon);				
			}	

		}
		
	}
	
	public static ProteinLigandTerm create(ClassInteractionStatistics stats, FFMolecule mol, int a1, int a2) {		
		PLFunction f = stats.getFunction(mol.getAtomInteractionClass(a1), mol.getAtomInteractionClass(a2));
		if(f==null) return null;
		return new ProteinLigandTerm(mol, new int[]{a1, a2}, f);			
	}
	
	
	@Override
	public final double getFGValue(final Coordinates[] gradient) {
		final Coordinates ci = getMolecule().getCoordinates(atoms[0]);		
		final Coordinates ck = getMolecule().getCoordinates(atoms[1]);				
		final Coordinates cr = ci.subC(ck);
		rik2 = cr.distSq();		
		
		if(rik2>CUTOFF*CUTOFF) {
			energy = 0; 
		} else {
			double de=0;
			double rik = Math.sqrt(rik2);
			if(rik<1) rik = 1;

			if(f!=null) {
				double valDer[] = f.getFGValue(rik);			
				energy = PLFunctionSplineCalculator.FACTOR * valDer[0];	 
				if(gradient!=null) de = PLFunctionSplineCalculator.FACTOR * valDer[1];				
			} else {
				double p2 = radmin * radmin / rik2;
				double p4 = p2 * p2;
				double p8 = p4 * p4;			
				double vdw = epsilon * (p8 - 2*p4);
											
				energy = vdw;	 
				if(gradient!=null) {
					double dvdw = epsilon / rik * -8 * (p8-p4);
					de = dvdw;
				}									
			}

			if(gradient!=null) {
				double deddt = de / rik;
				if(atoms[0]<gradient.length) gradient[atoms[0]].add(cr.scaleC(deddt));
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(cr.scaleC(-deddt));
			}					
		}
		
		return energy;

	}
	
	@Override
	public String toString() {
		return "PL-Term  "+atoms[0] +" - "+atoms[1]+" "+new DecimalFormat("0.000").format(Math.sqrt(rik2))+" -> "+new DecimalFormat("0.0000").format(energy);
	}

	@Override
	public boolean isExtraMolecular() {
		return true;
	}
	

}
