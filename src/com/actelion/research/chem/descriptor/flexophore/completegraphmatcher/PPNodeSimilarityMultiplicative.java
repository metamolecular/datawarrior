package com.actelion.research.chem.descriptor.flexophore.completegraphmatcher;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.descriptor.flexophore.PPNode;
import com.actelion.research.forcefield.interaction.ClassInteractionTable;

/**
 * 
 * MultiplicativeNodeSimilarity
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Jan 7, 2013 MvK Start implementation
 */
public class PPNodeSimilarityMultiplicative {
	
	private static final int SIZE_SIM_MATRIX = 10; 
	
	private static PPNodeSimilarityMultiplicative INSTANCE = null;
	
	private static ClassInteractionTable CIT;
	
	private double threshAtomSimilarity;
	
	private boolean invalidAtomMappping;
	
	private Matrix maSimilarity;
	
	/**
	 * This constructor is used for parallel mode.
	 */
	public PPNodeSimilarityMultiplicative(){
				
		
		maSimilarity = new Matrix(SIZE_SIM_MATRIX, SIZE_SIM_MATRIX);
		
		threshAtomSimilarity = ObjectiveFlexophoreHardMatchUncovered.THRESH_NODE_SIMILARITY;
		if(CIT==null) {
			synchronized(this) {
				CIT = ClassInteractionTable.getInstance();
			}
		}
	}
	
	/**
	 * Use this as constructor for serial mode.
	 * @return
	 */
	public static PPNodeSimilarityMultiplicative getInstance(){
		if(INSTANCE == null) {
			synchronized(PPNodeSimilarityMultiplicative.class) {
				INSTANCE = new PPNodeSimilarityMultiplicative();
			}
		}
		return INSTANCE;
	}

	/**
	 * 
	 * @param query
	 * @param base
	 * @return
	 * @throws Exception
	 */
	public double getSimilarity(PPNode query, PPNode base) {
		
		maSimilarity.set(0);
		
		for (int i = 0; i < query.getInteractionTypeCount(); i++) {
			
			int interactionIdQuery = query.getInteractionId(i);
			
			for (int j = 0; j < base.getInteractionTypeCount(); j++) {
				int interactionIdBase = base.getInteractionId(j);
				double similarity = 1.0 - CIT.getDistance(interactionIdQuery, interactionIdBase);
				maSimilarity.set(i,j,similarity);
			}
		}
		
		double sim = 1.0;
		
		if(base.getInteractionTypeCount() > query.getInteractionTypeCount()) {
			for (int j = 0; j < base.getInteractionTypeCount(); j++) {
				sim *= maSimilarity.getMax(j);
			}
		} else {
			for (int i = 0; i < query.getInteractionTypeCount(); i++) {
				sim *= maSimilarity.getMaxRow(i);
			}
		}
		
		
		if(sim<threshAtomSimilarity){
			invalidAtomMappping = true;
		}
		
		return sim;
	}

	public boolean isInvalidAtomMappping() {
		return invalidAtomMappping;
	}
	
}
