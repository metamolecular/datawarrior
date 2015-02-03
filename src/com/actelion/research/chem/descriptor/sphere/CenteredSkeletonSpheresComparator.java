package com.actelion.research.chem.descriptor.sphere;

import java.util.List;

import com.actelion.research.util.datamodel.ByteVec;

/**
 * 
 * 
 * CenteredSkeletonSpheresComparator
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 28, 2011 MvK: Start implementation
 */
public class CenteredSkeletonSpheresComparator {

	private static final double DEFAULT_SCORE_ZERO = 1.0;
	
	private double [] arrWeighingScheme;
	
	private double devisor;
	
	public CenteredSkeletonSpheresComparator(int depth) {
		this(WeighingSchemeFactory.getDescreasingByFiftyPercent(depth));
	}
	
	public CenteredSkeletonSpheresComparator(double [] arrWeighingScheme) {
		
		this.arrWeighingScheme = new double [arrWeighingScheme.length];
		
		System.arraycopy(arrWeighingScheme, 0, this.arrWeighingScheme, 0, arrWeighingScheme.length);
		
		initDevisor();
	}
	
	public CenteredSkeletonSpheresComparator(CenteredSkeletonSpheresComparator cssc) {
		this(cssc.arrWeighingScheme);
	}
	

	private void initDevisor(){
		devisor=0;
		for (int i = 0; i < arrWeighingScheme.length; i++) {
			devisor+=arrWeighingScheme[i];
		}
	}
	
	
	private double getWeightedSimilarity(double [] arrSim) {
		double sim=0;
		
		int minLen = Math.min(arrSim.length, arrWeighingScheme.length);
		
		for (int i = 0; i < minLen; i++) {
			sim += arrSim[i]*arrWeighingScheme[i];
		}
		
		sim /= devisor;
		
		return sim;
	}
	
	public double getWeightedMaximumSimilarity(List<List<byte[]>> liliDescriptor1, List<List<byte[]>> liliDescriptor2) {
		
		double maxSim=0;
		
		for (List<byte[]> liDescriptor1 : liliDescriptor1) {
			for (List<byte[]> liDescriptor2 : liliDescriptor2) {
				
				double [] arrSim = getSimilarity(liDescriptor1, liDescriptor2);
				
				double sim = getWeightedSimilarity(arrSim);
				
				if(sim>maxSim){
					maxSim=sim;
				}
			}
		}
		
		return maxSim;
	}
	
	private double [] getSimilarity(List<byte[]> liDescriptor1, List<byte[]> liDescriptor2) {
		
		int minLen = Math.min(liDescriptor1.size(), liDescriptor2.size());
		
		double [] arrSim = new double[minLen];
		
		for (int i = 0; i < minLen; i++) {
			
			byte [] a1 = liDescriptor1.get(i);
			
			byte [] a2 = liDescriptor2.get(i);
			
			if(isAllZero(a1) && isAllZero(a2)){
				arrSim[i] = DEFAULT_SCORE_ZERO;
			} else {
				arrSim[i] = ByteVec.getTanimotoDist(a1, a2);	
			}
		}
		
		return arrSim;
	}
	
	private static final boolean isAllZero(byte [] a){
		
		for (int i = 0; i < a.length; i++) {
			if(a[i] != 0)
				return false;
		}
		
		return true;
	}

	public void setWeighingScheme(double[] arrWeighingScheme) {
		this.arrWeighingScheme = arrWeighingScheme;
		initDevisor();
	}
	
	public double[] getWeighingScheme() {
		return arrWeighingScheme;
	}
	
}
