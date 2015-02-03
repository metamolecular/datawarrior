package com.actelion.research.chem.descriptor.sphere;

import java.util.Arrays;

/**
 * 
 * 
 * WeighingSchemeFactory
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Mar 1, 2012 MvK: Start implementation
 */
public class WeighingSchemeFactory {
	
	
	public static final double DEFAULT_STEP_SIZE = 10;
	
	public static final double DEFAULT_START = 100;
	
	
	public static double [] getAllOne(int depth){
		
		double [] arr = new double [depth];
		
		for (int i = 0; i < arr.length; i++) {
			arr[i] = 1.0;
		}
		
		return arr;
	}
	
	/**
	 * Nonlinear: 50,15, 12.5 ...
	 * @param depth
	 * @return
	 */
	public static double [] getDescreasingByFiftyPercent(int depth){
		
		double [] arr = new double [depth];
		
		arr[0] = 50;
		
		for (int i = 1; i < arr.length; i++) {
			arr[i] = arr[i-1] / 2.0;
		}
		
		return arr;
	}
	
	public static double [] getDecreasingSteps(int depth, double stepsize){
		
		return getDecreasingSteps(depth, stepsize, stepsize);
	}
	
	public static double [] getDecreasingSteps(int depth, double stepsize, double start){
		
		double [] arr = new double [depth];
		
		arr[depth-1] = start;
		
		for (int i = arr.length-2; i >= 0; i--) {
			arr[i] = arr[i+1]+stepsize;
		}
		
		return arr;
	}
	
	public static double [][] getSampleOfSchemes(int depth){
		
		double [][] arrSchemes = new double [4][depth];
		
		int cc=0;
		
		arrSchemes[cc++]=getAllOne(depth);
		
		arrSchemes[cc++]=getDecreasingSteps(depth, DEFAULT_STEP_SIZE, DEFAULT_START);
		
		arrSchemes[cc++]=getDecreasingSteps(depth, DEFAULT_STEP_SIZE);
		
		arrSchemes[cc++]=getDescreasingByFiftyPercent(depth);
		
		return arrSchemes;
		
	}
	
	public static void main(String[] args) {
		
		int depth = 7;
		
		double [] a = getAllOne(depth);
		
		System.out.println("Different weighing schemes for descriptor weighing of the centered skeleton fragment descriptor\n");
		System.out.println("All one " + Arrays.toString(a));
		System.out.println("\n");

		
		a = getDescreasingByFiftyPercent(depth);
		
		System.out.println("Descreasing by fifty percent " + Arrays.toString(a));
		System.out.println("\n");
		
		
		a = getDecreasingSteps(depth, 10);
		
		System.out.println("Descreasing steps " + Arrays.toString(a));
		System.out.println("\n");
		
		a = getDecreasingSteps(depth, 10, 1000);
		
		System.out.println("Descreasing steps with start " + Arrays.toString(a));
	}

}
