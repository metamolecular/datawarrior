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

/**
 * @author freyssj
 */
public class DiscreteFunction  {
	private final double[] Y;
	
	public DiscreteFunction(int size) {
		Y = new double[size];
	}	
	
	public DiscreteFunction(double[] src) {
		this(src.length);
		System.arraycopy(src, 0, Y, 0, src.length);
	}	

	public DiscreteFunction(int[] src) {
		this(src.length);
		for(int i=0; i<src.length; i++) Y[i] = src[i];
	}	
	
	public double value(double x) {
		int index = distanceToIndex(x);
		return value(index);
	}
	public double value(int index) {
		if(index<0) return Y[0];
		if(index>=Y.length) return 0;
		return Y[index];
	}

	public void setValue(int index, double y) {
		if(index<0 || index>=Y.length) return;
		Y[index] = y;
	}
	
	public double[] getX() {
		double[] tmp = new double[Y.length];
		for(int i=0; i<tmp.length; i++) tmp[i] = (i+.5) * PLFunctionSplineCalculator.DELTA_RADIUS;
		return tmp;
	}

	public double[] getY() { 
		return Y;
	}
	
	 
	//////////////////////////////////////////////////////
	public void normalize() {
		double normalizedSum = 0;
		for(int index=0; index<Y.length; index++) {
			double v = Math.PI * 4.0/3 * (Math.pow(PLFunctionSplineCalculator.DELTA_RADIUS*(index+.5),3)-Math.pow(PLFunctionSplineCalculator.DELTA_RADIUS*(index-.5),3));
			normalizedSum += Y[index] / v;
		}				
		if(normalizedSum==0) return;
		for(int index=0; index<Y.length; index++) {
			double v = Math.PI * 4.0/3 * (Math.pow(PLFunctionSplineCalculator.DELTA_RADIUS*(index+.5),3)-Math.pow(PLFunctionSplineCalculator.DELTA_RADIUS*(index-.5),3));
			Y[index] *= 100 / (v * normalizedSum);
		}
	}

	public void removeArtifacts() {
		for(int index=0; index<Y.length-2; index++) {
			if(Y[index]>0 && Y[index+1]==0) Y[index] = 0;
		}		
	}
	
	public void smoothLinear() {
		double[] copy = new double[Y.length];
		System.arraycopy(Y, 0, copy, 0, Y.length);
		for(int index=0; index<Y.length; index++) {
			Y[index] = 
			//if(index>0) Y[index] = Math.max(Y[index], copy[index-1]*.25);
			//if(index+1<Y.length) Y[index] = Math.max(Y[index], copy[index+1]*.25);
				(index-1>=0? copy[index-1]*.1: 0) + 
				copy[index]*.8 + 
				(index+1<Y.length? copy[index+1]*.1: 0);
		}				
	}
	/*
	public void postProcess() {
		double[] copy = new double[Y.length];
		System.arraycopy(Y, 0, copy, 0, Y.length);
		for(int index=0; index<Y.length; index++) {
			Y[index] = 
				(index-1>=0? copy[index-1]*.1: 0) + 
				copy[index]*.8 + 
				(index+1<Y.length? copy[index+1]*.1: 0);
		}
		
						
	}
	*/
	public void smoothExp() {
		double[] copy = new double[Y.length];
		System.arraycopy(Y, 0, copy, 0, Y.length);
		for(int index=0; index<Y.length; index++) {
			Y[index] = 0; 
			for(int x=0; x<Y.length; x++) {
				Y[index] += copy[x] * Math.exp(-Math.pow((index-x)*PLFunctionSplineCalculator.DELTA_RADIUS, 4))/Math.sqrt(Math.PI);
			}
		}				
	}

	public static final int distanceToIndex(double x) {
		return (int)( .5 + x / PLFunctionSplineCalculator.DELTA_RADIUS);
	}						

		
}
