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

import java.util.*;

import com.actelion.research.chem.*;

/**
 * Abstract Transformation to change coordinates from one referential to an other.
 * It is important for the optimization to implement the transformation itself, but also the gradient and the partial derivates
 *  
 */
public abstract class AbstractTransform implements Cloneable {
	
	protected double[] parameters;
	
	protected AbstractTransform() {}

	/**
	 * Gets the transformated coordinates
	 * @param initial
	 * @return
	 */
	public abstract Coordinates[] getTransformation(Coordinates[] initial);
	
	/**
	 * Gets the transformated coordinates according to the partial cartesian derivatives
	 * (df/dx, df/dy, df/dz)  
	 * @param dinitial
	 * @return
	 */
	public abstract Coordinates[] getPartialTransformation(Coordinates[] dinitial);	
	
	/**
	 * Gets the first derivate of the transformation according to var
	 * (df/dvar)  
	 * @param var
	 * @param initial
	 * @return
	 */
	public abstract Coordinates[] getDTransformation(int var, Coordinates[] initial);	

	
	/**
	 * Randomizes this transformation (used by GA)
	 */
	public abstract void random();	

	/**
	 * Mutates this transformation (used by GA)
	 */
	public abstract AbstractTransform mutate();
	
	
	/**
	 * Crossover this transformation (used by GA)
	 */
	public abstract AbstractTransform crossover(AbstractTransform transform);
	
	/**
	 * @return the specific parameters
	 */
	public double[] getParameters() {return parameters;}
	
	
	/**
	 * The transformation may create rigid groups of atoms. this function populated the int[] atom2group and assign a groupNo to each atom.
	 * 
	 * Forces between atoms of the groups don't need to be considered for the optimization (ex: atoms from the same ring when considering the torsiontransform)
	 * 
	 * 
	 * @param groups
	 * @param n
	 * @return
	 */
	protected abstract int createGroups(int[] atom2group, int n);
	
	/**
	 * Clone the transformation.
	 * By principle, all the attributes but "parameters" are kept as references
	 * @see java.lang.Object#clone()
	 */
	@Override
	public AbstractTransform clone() {
		try {
			AbstractTransform tr = (AbstractTransform) super.clone();
			tr.parameters = new double[parameters.length];
			System.arraycopy(parameters, 0, tr.parameters, 0, parameters.length);
			return tr;		
		} catch(CloneNotSupportedException e) {
			return null;
		}
	}
	
	@Override
	public String toString() {		
		return Arrays.toString(parameters);
	}

}
