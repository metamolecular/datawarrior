package com.actelion.research.chem.properties.complexity;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.util.datamodel.IntArray;

/**
 * 
 * ContainerBitArray
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Jun 6, 2013 MvK Start implementation
 */
public class ContainerBitArray {
	
	private static boolean ELUSIVE = false;

	private static final int CAPACITY_ADD = 1 << 10;
	
	private static final int MAX_CAPACITY_ADD = 1 << 28;
	
	private static final int LIMIT2FULL = 1500000000;
	
	private static final int CAPACITY_FULL = 2000000000;
	
	
	private List<IBitArray> li;
			
	private IntArray arrAvailable;
	
	private int capacityAdd;

	private int bits;
	
	IBitArrayFactory<? extends IBitArray> bitArrayCreator;
	
	/**
	 * 
	 * @param bits is the number of available bits in the binary vector. 
	 * @param capacity
	 */
	public ContainerBitArray(int bits, int capacity) {
		
		if(bits == BitArray128.MAX_NUM_BITS){
			bitArrayCreator = new BitArray128Factory();
		} else {
			throw new RuntimeException("Do not know a factory to construct " + bits + " bits array.");
		}
		
		this.bits = bits;
		
		if(capacity > LIMIT2FULL) {
			capacity = CAPACITY_FULL;
		}
		
		capacityAdd = CAPACITY_ADD;
	
		arrAvailable = new IntArray(capacity);
		
		li = new ArrayList<IBitArray>(capacity);
		
		
		
		addResources(capacity);
	}
	
	@SuppressWarnings("unchecked")
	public void calculateHash(IBitArray f){
		
		((IBitArrayFactory<IBitArray>)bitArrayCreator).calculateHash(f);
	}

	public int getSizeBinaryArray(){
		return bits;
	}
	
	public void reset(){
		
		arrAvailable.reset();
		for (int i = 0; i < li.size(); i++) {
			arrAvailable.add(i);
		}
	}
	
	
	private void addResources(int capacity) {
		
		if(li.size() == Integer.MAX_VALUE){
			new RuntimeException("Maximum capacity reached").toString();
			return;
		}
		
		int indexStart = li.size();
		
		for (int i = 0; i < capacity; i++) {
			
			int index = indexStart+i;
			
			li.add(bitArrayCreator.getNew(index));
			
			arrAvailable.add(index);
			
			if(li.size() == Integer.MAX_VALUE){
				new RuntimeException("Maximum capacity reached").toString();
				break;
			}
		}
	}
	/**
	 * 
	 * @return a fresh (reseted) instance.
	 */
	public IBitArray get(){
		
		if(arrAvailable.length()==0){
			
			addResources(capacityAdd);
			
			if(ELUSIVE){
				System.out.println("ContainerBitArray capacity increased by " + capacityAdd + " objects.");
			}

			if(capacityAdd<MAX_CAPACITY_ADD) {
				capacityAdd <<= 1;
			}
			
		}
		
		int index = arrAvailable.removeLast();
		
		IBitArray bitArray = li.get(index);
		
		bitArray.reset();
		
		return bitArray;
	}
	
	public void receycle(IBitArray liv){
		arrAvailable.add(liv.getIndex());
	}
	
	public IBitArray getWithCopy(BitArray128 orign){
		
		IBitArray liv = get();
		
		liv.copyIntoThis(orign);
		
		return liv;
	}
	
	public int getCapacity(){
		return li.size();
	}
	
	public int getAvailable(){
		return arrAvailable.length();
	}
	
	/**
	 * @return the eLUSIVE
	 */
	public static boolean isELUSIVE() {
		return ELUSIVE;
	}

	/**
	 * @param elusive the eLUSIVE to set
	 */
	public static void setELUSIVE(boolean elusive) {
		ELUSIVE = elusive;
	}

}
