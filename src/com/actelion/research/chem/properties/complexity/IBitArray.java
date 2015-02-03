package com.actelion.research.chem.properties.complexity;

/**
 * IBitArray
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Nov 20, 2014 MvK Start implementation
 */
public interface IBitArray {
	
	public void add(IBitArray f);
	
	public void copyIntoThis(IBitArray orign);
	
	public boolean equals(Object obj);
	
	public int getBitsSet();
	
	public int getIndex();
	
	public int getSizeAfterLastBitSet();
	
	public int hashCode();
	
	public boolean isBitSet(int i);
	
	public boolean isOverlap(IBitArray f);
	
	public void reset();
	
	public void setBit(int i);
	
	public void setHash(int h);
	
	public String write2String();
}
