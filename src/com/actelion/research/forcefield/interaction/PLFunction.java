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

import com.actelion.research.util.ArrayUtils;
import com.actelion.research.util.FastSpline;

/**
 * Class used to represent a Protein Ligand Function
 * 
 */
public class PLFunction {
	
	private String name;
	private int[] occurencesArray = new int[(int)(PLFunctionSplineCalculator.CUTOFF_STATS/PLFunctionSplineCalculator.DELTA_RADIUS)];
	private FastSpline spline, derivate;	

	
	public PLFunction(String name) {
		this.name = name;
	}

	public String getName() {return name;}
	public void setName(String string) {name = string;}

	public int[] getOccurencesArray() {
		return occurencesArray;
	}
	public void setOccurencesArray(int[] occurencesArray) {
		this.occurencesArray = occurencesArray;
	}
	
	public void setSplineFunction(FastSpline spline) {
		this.spline = spline;
		this.derivate = spline.derivative();
	}
	
	public int getTotalOccurences() {
		return ArrayUtils.sum(occurencesArray);		
	}
	
	public double[] getFGValue(double v) {
		if(spline==null) throw new IllegalArgumentException("The spline was not computed for "+name);
		try{		
			double value = spline.value(v);
			double dev = derivate.value(v);
			return new double[]{value, dev};
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid getFGValue for "+name+": "+v);
		}
	}
	
	public FastSpline getSpline() {
		return spline;
	}
	@Override
	public String toString() {
		return "PLFunction: " + name;
	}

}
