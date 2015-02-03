package com.actelion.research.chem.descriptor.flexophore;

import com.actelion.research.chem.Coordinates;

/**
 * IndexCoordinates
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Jan 30, 2013 MvK Start implementation
 */
public class IndexCoordinates {

	private int index;
	
	private int indexOriginalAtom;
	
	private Coordinates coord;
	
	/**
	 * 
	 */
	public IndexCoordinates() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param index
	 * @param coord
	 */
	public IndexCoordinates(int index, int indexOriginalAtom, Coordinates coord) {
		super();
		this.index = index;
		this.indexOriginalAtom = indexOriginalAtom;
		this.coord = coord;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @param index the index to set
	 */
	public void setIndex(int index) {
		this.index = index;
	}
	
	/**
	 * @return the indexOriginalAtom
	 */
	public int getIndexOriginalAtom() {
		return indexOriginalAtom;
	}

	/**
	 * @param indexOriginalAtom the indexOriginalAtom to set
	 */
	public void setIndexOriginalAtom(int indexOriginalAtom) {
		this.indexOriginalAtom = indexOriginalAtom;
	}


	/**
	 * @return the coord
	 */
	public Coordinates getCoord() {
		return coord;
	}

	/**
	 * @param coord the coord to set
	 */
	public void setCoord(Coordinates coord) {
		this.coord = coord;
	}

	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IndexCoordinates [index=");
		builder.append(index);
		builder.append(", index orig=");
		builder.append(indexOriginalAtom);
		builder.append(", coord=");
		builder.append(coord);
		builder.append("]");
		return builder.toString();
	}

	
}
