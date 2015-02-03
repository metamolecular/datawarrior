package com.actelion.research.chem.properties.complexity;

/**
 * IBitArrayFactory.java
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Nov 20, 2014 MvK Start implementation
 */
public interface IBitArrayFactory<T extends IBitArray> {
		
	public T getNew(int index);
	
	/**
	 * Stores the hash in f.
	 * @param f
	 */
	public void calculateHash(T f);
	
	
	
}
