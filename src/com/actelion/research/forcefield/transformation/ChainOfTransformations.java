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
package com.actelion.research.forcefield.transformation;


import java.util.Random;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.optimizer.*;

/**
 * 
 */
public class ChainOfTransformations implements Cloneable {
	
	private static final Random random = new Random();

	/** What is being transformed */	
	private final Coordinates[] initial;
	/** The chain of Transformation */
	private final AbstractTransform[] chain;
	/** The number of variable depending of this transformation */
	private final int N;

	private final int[] groups;


	private ChainOfTransformations(ChainOfTransformations c, AbstractTransform[] transform) {
		if(transform==null) transform = c.chain;
		this.chain = new AbstractTransform[transform.length];
		for (int i = 0; i < chain.length; i++) {					
			chain[i] = (AbstractTransform) transform[i].clone();
		}	
		this.initial = c.initial;
		this.N = c.N;
		this.groups = c.groups;
				 
	}

	/**
	 * Constructs a ChainOfTransformations with the initial position of the ligand 
	 * @param chain
	 * @param initial
	 */
	public ChainOfTransformations(AbstractTransform[] chain, FFMolecule initial) {
		this(chain, StructureCalculator.getLigandCoordinates(initial));
	}
	
	/**
	 * Constructs a ChainOfTransformations given an initial position
	 * @param chain
	 * @param initial
	 */
	public ChainOfTransformations(AbstractTransform[] chain, Coordinates[] initial) {
		this.chain = chain;
		this.initial = initial;
		this.groups = new int[initial.length];
		
		//Extract the total number of variables used by this transformation
		int N = 0;
		for (int i = 0; i < chain.length; i++) N += chain[i].parameters.length;		
		this.N = N;
		
		//Extract the groups of atoms
		int n = 0;		
		for (int i = 0; i < chain.length; i++) n = chain[i].createGroups(groups, n); 
	}
		
	/**
	 * Gets the Multivariate (ie the parameters that dictate this transformation)
	 * @return
	 */
	public final MultiVariate getMultivariate() {		
		//Populate the multivariate vector with the parameters of each transformation
		MultiVariate v = new MultiVariate(N);
		int N = 0;
		for (int i = 0; i < chain.length; i++) {
			int l = chain[i].parameters.length;
			System.arraycopy(chain[i].parameters, 0, v.vector, N, l);
			N += l;
		} 		
		return v;
	}

	/**
	 * Sets the Multivariate (ie the parameters that dictate this transformation)
	 * @return
	 */
	public final void setMultivariate(MultiVariate v) {
		int N = 0;
		for (int i = 0; i < chain.length; i++) {
			int l = chain[i].parameters.length;
			System.arraycopy(v.vector, N, chain[i].parameters, 0, l);
			N += l;
		} 				
	}

	/**
	 * Operates the transformation on the ligand's coordinates
	 * @param mol
	 */
	public void transform(FFMolecule mol) {
		StructureCalculator.setLigandCoordinates(mol, getTransformation());
	}

	/**
	 * Gets the Coordinates after the transformation  
	 * = (F1 o F2 o F3)(initial)
	 * @return
	 */
	public Coordinates[] getTransformation() {		
		//Compose the transformation
		Coordinates[] res = initial;
		for (int i = chain.length-1; i>=0; i--) {
			res = chain[i].getTransformation(res);
		}
		return res;
	}

	/**
	 * Gets the derivative according to one of the multivariate
	 * = d(F1 o F2 o F3)/di (initial)
	 * @param var
	 * @return
	 */
	public Coordinates[] getDTransformation(int var) {
		if(chain.length==1) return chain[0].getDTransformation(var, initial);
		Coordinates[] X = initial;
		Coordinates[] dX = null;
		int index = N;	
		boolean found = false;
		for (int i = chain.length-1; i>=0; i--) {
			index -= chain[i].parameters.length;
			if(var>=index && var-index<chain[i].parameters.length) {
				dX = chain[i].getDTransformation(var - index, X);
				found = true;	
			} else if(found) {
				dX = chain[i].getPartialTransformation(dX);
			} else {
				X = chain[i].getTransformation(X);
			} 			
		}
		return dX;
	}

	
	/**
	 * Randomizes all transformation of the chain
	 */
	public void random() {
		for (int i = 0; i < chain.length; i++) {
			chain[i].random();
		}				
	}
	
	private final int getRandomChainIndex() {
		return random.nextInt(chain.length);
	}
	/**
	 * 
	 * @return
	 */	
	public ChainOfTransformations mutate() {
		AbstractTransform[] tr = new AbstractTransform[chain.length];
		

		//Best is to mutate one chain
		int k = getRandomChainIndex();
		AbstractTransform mutation;
		while((mutation = chain[k].mutate()) == null) {k = getRandomChainIndex();}

		for (int i = 0; i < chain.length; i++) {
			if(i!=k) tr[i] = (AbstractTransform) chain[i].clone();
			else tr[i] = mutation;
		}

		return new ChainOfTransformations(this, tr);				
	}
	
	public ChainOfTransformations crossover(ChainOfTransformations other) {		
		AbstractTransform[] tr = new AbstractTransform[chain.length];
		
		
		//Best is to crossover all chains
		for (int i = 0; i < chain.length; i++) {
			tr[i] = chain[i].crossover(other.chain[i]);
			if(tr[i]==null) tr[i] = (AbstractTransform) chain[i].clone();
		}		

		return new ChainOfTransformations(this, tr);				
	}
		
	public double getRMSD(ChainOfTransformations other) {
		double rmsd1 = 0, rmsd2 = 0;
		int n1 = 0, n2 = 0;
		for (int i = 0; i < chain.length; i++) {
			for (int j = 0; j < chain[i].parameters.length; j++) {
				double diff;
				if(chain[i] instanceof TorsionTransform) diff = diffAngle(chain[i].parameters[j], other.chain[i].parameters[j]);				
				else if(chain[i] instanceof TransRotTransform && j>=3) diff = 2*diffAngle(chain[i].parameters[j], other.chain[i].parameters[j]);
				else diff = chain[i].parameters[j] - other.chain[i].parameters[j];
				
				if(chain[i] instanceof TransRotTransform) {rmsd1 += diff*diff; n1++;}
				else {rmsd2 = diff*diff; n2++;}
								
			} 
		}
		double rmsd = 0;
		if(n1>0) rmsd += 2*Math.sqrt(rmsd1/n1);
		if(n2>0) rmsd += Math.sqrt(rmsd2/n2);
		return rmsd;
	}
	
	public static final double diffAngle(double a1, double a2) {
		double d = Math.abs(a1 - a2)%(Math.PI*2); 
		if(d>Math.PI) d = 2*Math.PI-d;
		return d;
	}
	
	
	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ChainOfTransformations clone() {
		return new ChainOfTransformations(this, null); 
	}

		
	
	/**
	 * @return
	 */
	public int[] getGroups() {
		return groups;
	}

	/**
	 * @return
	 */
	public AbstractTransform[] getElements() {
		return chain;
	}

}
