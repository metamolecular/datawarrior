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
package com.actelion.research.forcefield.optimizer;

/**
 * wrapper to a vector of double  
 */
public final class MultiVariate {
	
	/** The vector to optimize */
	public final double[] vector;
	
	/**
	 * Basic constructor with a size
	 * @param size
	 */
	public MultiVariate(int size) {
		vector = new double[size];	
	}
	public MultiVariate(MultiVariate v) {
		vector = new double[v.vector.length];
		System.arraycopy(v.vector, 0, vector, 0, vector.length);	
	}
	
	public final double getNormSq() {
		double res = 0;
		for (int i = 0; i < vector.length; i++) res += vector[i] * vector[i];
		return res;
	}

	public final int getSize() { return vector.length;}
	public final double getNorm() {return Math.sqrt(getNormSq());}
	public final double getRMS() {return Math.sqrt(getNormSq() / vector.length);}	

	public final MultiVariate scale(double scale) {
		MultiVariate variable = new MultiVariate(vector.length);
		for (int i = 0; i < variable.vector.length; i++) variable.vector[i]=vector[i]*scale;
		return variable;
	}

}
