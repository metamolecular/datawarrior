package com.actelion.research.chem.properties.complexity;

/**
 * IndexHash
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Nov 20, 2014 MvK Start implementation
 */
public abstract class IndexHash {

	private int index;
	
	protected int hash;

	/**
	 * 
	 */
	public IndexHash() {
		
	}
	
	public IndexHash(int index) {
		this.index = index;
	}
		
	public void setHash(int h){
		hash = h;
	}
	
	public int getIndex(){
		return index;
	}
	
	public void setIndex(int index){
		this.index = index;
	}
	
	@Override
	public int hashCode() {
		if(hash==-1){
			throw new RuntimeException("Hash was not calculated");
		}
		
		return hash;
	}
	

}
