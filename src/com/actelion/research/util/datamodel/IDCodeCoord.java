
package com.actelion.research.util.datamodel;

/**
 * 
 * 
 * IDCodeCoord
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Apr 12, 2012 MvK: Start implementation
 */
public class IDCodeCoord {
	
	public int index;
	
	public String idcode;
	
	public String coordinates;

	
	public IDCodeCoord(String idcode) {
		
		this.idcode=idcode;
		
		index=-1;
	}
	
	public IDCodeCoord(String idcode, String coordinates) {
		this.idcode=idcode;
		
		this.coordinates=coordinates;
		
		index=-1;
	}
	
	public IDCodeCoord(int index, String idcode, String coordinates) {
		
		this.index=index;
		
		this.idcode=idcode;
		
		this.coordinates=coordinates;
	}

	public int getIndex() {
		return index;
	}

	public String getIdcode() {
		return idcode;
	}

	public String getCoordinates() {
		return coordinates;
	}

	/**
	 * Taken from idcode.
	 */
	public int hashCode() {
		return idcode.hashCode();
	}
	
}