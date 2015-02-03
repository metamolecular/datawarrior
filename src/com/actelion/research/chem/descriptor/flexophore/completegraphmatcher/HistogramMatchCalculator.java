package com.actelion.research.chem.descriptor.flexophore.completegraphmatcher;

import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.chem.descriptor.flexophore.IMolDistHist;
import com.actelion.research.chem.descriptor.flexophore.generator.CGMult;

/**
 * 
 * 
 * HistogramMatchCalculator
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 2, 2012 MvK: Start implementation
 * May 15 2013 MvK: Heavy bug detected. Wrong similarity results. reset() added. 
 */
public class HistogramMatchCalculator {
	
	private static final boolean DEBUG = false;

	private static final double FRACTION = 0.05;
	
	private byte [] arrTmpHistMol;
	
	private int [] arrTmpHistMolBlurred;
	
	private byte [] arrTmpHistFrag;
	
	private int [] arrTmpHistFragBlurred;

	
	public HistogramMatchCalculator() {
		
		arrTmpHistMol =  new byte[CGMult.BINS_HISTOGRAM];
		
		arrTmpHistMolBlurred =  new int[CGMult.BINS_HISTOGRAM];
		
		arrTmpHistFrag =  new byte[CGMult.BINS_HISTOGRAM];
		
		arrTmpHistFragBlurred =  new int[CGMult.BINS_HISTOGRAM];
	}
	
	private void reset(){
		for (int i = 0; i < CGMult.BINS_HISTOGRAM; i++) {
			arrTmpHistMol[i]=0;
			arrTmpHistMolBlurred[i]=0;
			arrTmpHistFrag[i]=0;
			arrTmpHistFragBlurred[i]=0;
		}
	}
	
	
	public double getSimilarity(IMolDistHist query, int indexQueryPPPoint1, int indexQueryPPPoint2, IMolDistHist base, int indexBasePPPoint1, int indexBasePPPoint2) {
		
		double sc = 0;
		
		byte [] arr1 = query.getDistHist(indexQueryPPPoint1, indexQueryPPPoint2, arrTmpHistMol);
		
		byte [] arr2 = base.getDistHist(indexBasePPPoint1, indexBasePPPoint2, arrTmpHistFrag);
				
		blurrHistogram(arr1, arrTmpHistMolBlurred);
		blurrHistogram(arr2, arrTmpHistFragBlurred);
		
		sc = getSimilarity(arrTmpHistMolBlurred, arrTmpHistFragBlurred); 
		
		reset();
		
		return sc;

	}
	
	private static void blurrHistogram(byte [] arr, int [] arrBlurred){
		
		for (int i = 1; i < arr.length; i++) {
			arrBlurred[i] = arr[i];
		}
				
		for (int i = 1; i < arr.length; i++) {
			
			if(arr[i]>0){
								
				int v = (int)(arr[i]*FRACTION+0.5);
				
				arrBlurred[i-1] += (byte)(v);
				
				if(i<arr.length-1) {
					arrBlurred[i+1] += (byte)(v);
				}
			}
		}
	}
	
	
	/**
	 * 
	 * @param arr1
	 * @param arr2
	 * @return Similarity value between 0 and 1. 1: histograms are identical.
	 */
	private double getSimilarity(int [] arr1, int [] arr2) {

		if(DEBUG){
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("!!!HistogramMatchCalculator DEBUG mode!!!");
			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			
			return 1.0;
		}
		
		double score = 0;
				    	
		double sumOverlap = 0;
		
		for (int i = 0; i < arr1.length; i++) {
			
			sumOverlap += Math.min(arr1[i], arr2[i]); 
			
		}
				
		score = sumOverlap / DescriptorHandlerFlexophore.NUM_CONFORMATIONS;

		if(score>1)
			score = 1.0;
		
		return score;
	}
	

}
