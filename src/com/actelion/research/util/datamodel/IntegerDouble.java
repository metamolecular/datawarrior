/**
 * 
 */
package com.actelion.research.util.datamodel;

import java.util.Comparator;

import com.actelion.research.util.Formatter;

/**
 * 
 * 
 * IntegerDouble
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 24, 2011 MvK: Start implementation
 */
public class IntegerDouble {
	
	private int iv;
	
	private double dv;
	
	public IntegerDouble() {
		
	}
	
	/**
	 * @param iv
	 * @param dv
	 */
	public IntegerDouble(int iv, double dv) {
		super();
		this.iv = iv;
		this.dv = dv;
	}
	
	public IntegerDouble(IntegerDouble id) {
		this.iv = id.iv;
		this.dv = id.dv;
	}
	
	
	public int hashCode() {
		return iv;
	}
	
	public int getInt() {
		return iv;
	}
	
	public double getDouble() {
		return dv;
	}
		
	public void setInteger(int iv) {
		this.iv = iv;
	}
	
	public void setDouble(double dv) {
		this.dv = dv;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(iv);
		sb.append("\t");
		sb.append(Formatter.format3(dv));
		
		return sb.toString();
	}
	
	public static Comparator<IntegerDouble> getComparatorDouble(){
		
		return new Comparator<IntegerDouble>() {
			
			public int compare(IntegerDouble id1, IntegerDouble id2) {
				
				if(id1.dv>id2.dv){
					return 1;
				}else if(id1.dv<id2.dv){
					return -1;
				}
				
				return 0;
			}
		};
	}
	
}