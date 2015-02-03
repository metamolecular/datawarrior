package com.actelion.research.chem.mcs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.util.datamodel.IntArray;
import com.actelion.research.util.datamodel.IntVec;

/**
 * 
 * 
 * ListAndIntVec
 * For fast index based access.
 * Every value in the IntArray marks a position where a bit is set in the IntVec.
 * The hash value has to be calculated by the caller of the object!
 * 
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Feb 10, 2012 MvK: Start implementation
 */
public class ListWithIntVec {
	
	private IntVec iv;
	
	private IntArray arr;
	
	private int positionInContainer;
	
	
	public ListWithIntVec() {
	}
	
	public ListWithIntVec(int size) {
		this(size, -1);
	}
	
	/**
	 * 
	 * @param size of the integer array
	 * @param positionInContainer
	 */
	public ListWithIntVec(int size, int positionInContainer) {
		iv = new IntVec(size);
		arr = new IntArray();
		
		this.positionInContainer=positionInContainer;
	}
	
	public ListWithIntVec(ListWithIntVec liv) {
		
		iv = new IntVec(liv.iv);
		
		arr = new IntArray(liv.arr);
	}
	
	public void copyIntoThis(ListWithIntVec liv){
		
		iv.set(0);
		
		System.arraycopy(liv.iv.get(), 0, iv.get(), 0, iv.get().length);
		
		arr.reset();
		
		for (int i = 0; i < liv.arr.length(); i++) {
			arr.add(liv.arr.get(i));
		}
		
	}
	
	/**
	 * Dont't forget to calculate the hash!
	 * @param index
	 * @return false if the bit is already set, true otherwise. 
	 */
	public boolean addBit(int index){
		
		if(iv.isBitSet(index)){
			return false;
		}
		
		iv.setBit(index);
		
		arr.add(index);
		
		return true;
	}
	
	public void addAllBits(ListWithIntVec liv) {
		final int size = liv.size();
		
		for (int i = 0; i < size; i++) {
			addBit(liv.get(i));
		}
		
		calculateHash();
	}
	
	public boolean isOverlap(ListWithIntVec liv) {
		boolean overlap=false;
		
		final int size = liv.size();
		
		for (int i = 0; i < size; i++) {
			if(isBitSet(liv.get(i))){
				overlap=true;
				break;
			}
		}
		
		return overlap;
	}

	
	public boolean equals(Object o) {
		
		IntVec iv2 = ((ListWithIntVec)o).iv;
		
		return iv.equals(iv2);
	}
	
	public boolean isBitSet(int index){
		return iv.isBitSet(index);
	}
	
	public int getBitsSet(){
		
		return iv.getBitsSet();
	}
	
	public int sizeBits(){
		
		return iv.sizeBits();
	}
	
	
	public void calculateHash(){
		iv.calculateHashCode();
	}
	
	public int hashCode(){
		return iv.hashCode();
	}
	
	public int get(int index){
		return arr.get(index);
	}
	
	/**
	 * 
	 * @return number of bits set
	 */
	public int size(){
		return arr.length();
	}
	
	public int getLengthIntVec(){
		return iv.size();
	}
	
	
	public void reset(){
		iv.set(0);
		arr.reset();
	}
	
	/**
	 * The array part of the object.
	 * @return
	 */
    public String toStringArray() {
    	
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < arr.length(); i++) {
        	sb.append(arr.get(i));
            if(i < arr.length()-1){
            	sb.append(" ");
            }
        }
        
        return sb.toString();
    }
	
    public String toString() {
    	
        StringBuilder sb = new StringBuilder();
        
        int si = iv.sizeBits();
        List<Integer> li = new ArrayList<Integer>();
        for (int i = 0; i < si; i++) {
        	if(iv.isBitSet(i))
        		li.add(i);
        }
        
        for (int i = 0; i < li.size(); i++) {
        	sb.append(li.get(i));
            if(i<li.size()-1){
            	sb.append(", ");
            }
        }
        
        return sb.toString();
    }

	public int getPositionInContainer() {
		return positionInContainer;
	}

    public static ListWithIntVec read(InputStream s) throws IOException{
    	    	
    	int positionInContainer = IntArray.parseInteger(s);
    	
    	IntVec iv = IntVec.read(s);
    	
    	IntArray ia = IntArray.read(s);
    	
    	ListWithIntVec liv = new ListWithIntVec();
    	
    	liv.positionInContainer = positionInContainer;
    	
    	liv.iv = iv;
    	
    	liv.arr = ia;
    	
    	return liv;
    	
    }
    
    public String write2String() throws IOException{
    	
    	StringBuilder sb = new StringBuilder();

    	sb.append(positionInContainer);
    	sb.append(" ");
    	
    	sb.append(iv.write2String());
    	
    	sb.append(" ");
    	
    	sb.append(arr.write2String());
    	
    	return sb.toString();
    	
    }

	
}