package com.actelion.research.chem.descriptor.flexophore;

import com.actelion.research.util.graph.complete.ICompleteGraph;

/**
 * 
 * IMolDistHist
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 30 Nov 2010 MvK: Start implementation
 */
public interface IMolDistHist extends ICompleteGraph {
	
	public double getRelMaxDistInHist(int indexAt1, int indexAt2);
	
	public PPNode getNode(int i);
	
	public byte [] getDistHist(int indexAt1, int indexAt2, byte [] arr);
	
	public boolean isInevitablePharmacophorePoint(int indexNode);
	
	public int getNumInevitablePharmacophorePoints();

	
}
