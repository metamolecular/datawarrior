package com.actelion.research.chem.properties.complexity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ContainerFragBondsSolutions
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Jun 6, 2013 MvK Start implementation
 */
public class ContainerFragBondsSolutions {
	
	public static boolean ELUSIVE = false; 

	protected static double FACTOR_CAPACITY = 1.7;
	
	protected static int START_CAPACITY = 100;
		
//	private static final int LIMIT_BONDS_CAPACITY_TOP = 30;
//	
//	private static final int CAPACITY_TOP = 1000000;
	
	private ContainerBitArray containerListFragmentDefinedByBonds;
	
	// For each number of bonds one hash map in the list.
	private List<HashMap<IBitArray, IBitArray>> liHMFragmentDefinedByBonds;
	
	private int bondsMolecule;
	
	private int maximumNumberBondsInFragment;
	
	private int bits;
	
//	public ContainerFragBondsSolutions(int maximumNumberBondsInFragment) {
//		
//		this.maximumNumberBondsInFragment = maximumNumberBondsInFragment;
//		
//		int [] arrCapacity = new int [maximumNumberBondsInFragment];
//		
//		arrCapacity[0] = START_CAPACITY;
//		
//		for (int i = 1; i < arrCapacity.length; i++) {
//			int capacity = (int)(arrCapacity[i-1] * FACTOR_CAPACITY);
//			
//			if(i < LIMIT_BONDS_CAPACITY_TOP) {
//				arrCapacity[i] = capacity;	
//			} else {
//				arrCapacity[i] = CAPACITY_TOP;
//			}
//		}
//		
//		init(arrCapacity);
//		
//	}
	
	
	/**
	 * 
	 * @param bits
	 * @param maximumNumberBondsInFragment
	 * @param exhaustiveCalculation only needed to calculate the capacity of the container.
	 */
	public ContainerFragBondsSolutions(int bits, int maximumNumberBondsInFragment, boolean exhaustiveCalculation) {
		
		this.bits = bits;
		
		this.maximumNumberBondsInFragment = maximumNumberBondsInFragment;
		
		int [] arrCapacity = null;
		
		if(exhaustiveCalculation){
			arrCapacity = getCapacityExhaustive(maximumNumberBondsInFragment);
		} else {
			arrCapacity = getCapacityHalf(maximumNumberBondsInFragment);
		}
		
		init(arrCapacity);

		
	}
	
	private int [] getCapacityHalf(int maximumNumberBondsInFragment) {
		
		this.maximumNumberBondsInFragment = maximumNumberBondsInFragment;
		
		int [] arrCapacity = new int [maximumNumberBondsInFragment+1];
		
		arrCapacity[0] = START_CAPACITY;
		
		for (int i = 1; i < maximumNumberBondsInFragment + 1; i++) {
			int capacity = (int)(arrCapacity[i-1] * FACTOR_CAPACITY);
						
			arrCapacity[i] = capacity;	
		}
		
				
		return arrCapacity;

		
	}
	
	private int [] getCapacityExhaustive(int maximumNumberBondsInFragment) {
		
		this.maximumNumberBondsInFragment = maximumNumberBondsInFragment;

		int half = maximumNumberBondsInFragment / 2;
		
		int [] arrCapacity = new int [maximumNumberBondsInFragment];
		
		arrCapacity[0] = START_CAPACITY;
		
		for (int i = 1; i < half + 1; i++) {
			int capacity = (int)(arrCapacity[i-1] * FACTOR_CAPACITY);
						
			arrCapacity[i] = capacity;	
		}
		
		for (int i = half; i < arrCapacity.length; i++) {
			
			int capacity = (int)(arrCapacity[i-1] / FACTOR_CAPACITY);
						
			arrCapacity[i] = capacity;	
		}
				
		return arrCapacity;

		
	}
	
	
	private void init(int [] arrCapacity){
		
		
		int totalCapacity = 0;
		
		if(ELUSIVE)
			System.out.println("ContainerFragBondsSolutions Capacity");
		
		for (int i = 0; i < arrCapacity.length; i++) {
			
			totalCapacity += arrCapacity[i];
			
			if(ELUSIVE)
				System.out.println(arrCapacity[i]);
		}
		
		if(ELUSIVE)
			System.out.println("ContainerFragBondsSolutions totalCapacity " + totalCapacity + ".");
		
		liHMFragmentDefinedByBonds = new ArrayList<HashMap<IBitArray,IBitArray>>();
		
		liHMFragmentDefinedByBonds.add(new HashMap<IBitArray, IBitArray>());

		for (int i = 0; i < arrCapacity.length; i++) {
			
			liHMFragmentDefinedByBonds.add(new HashMap<IBitArray, IBitArray>(arrCapacity[i]));
			
		}
		
		containerListFragmentDefinedByBonds = new ContainerBitArray(bits, totalCapacity);
		
		if(ELUSIVE)
			System.out.println("ContainerFragBondsSolutions constructor finished.");
	}

	public boolean addFacultative(IBitArray f){
		
		boolean added = false;
		
		calculateHash(f);
		
		int bits = getBitsSet(f);
		
		HashMap<IBitArray, IBitArray> hm = liHMFragmentDefinedByBonds.get(bits);
		
		if(hm.containsKey(f)){
			containerListFragmentDefinedByBonds.receycle(f);
		} else {
			hm.put(f, f);
			added = true;
		}
		
		return added;
	}
	
	public int getBitsSet(IBitArray f){
		int bits = 0;
		for (int i = 0; i < bondsMolecule; i++) {
			if(f.isBitSet(i)){
				bits++;
			}
		}
		return bits;
	}
	
	public IBitArray getWithCopy(IBitArray orign){
		
		IBitArray f = containerListFragmentDefinedByBonds.get();
		
		f.copyIntoThis(orign);
		
		return f;
	}
	
	public List<IBitArray> getList(int bonds){
		
		HashMap<IBitArray, IBitArray> hm = liHMFragmentDefinedByBonds.get(bonds);

		
		return new ArrayList<IBitArray>(hm.values());
	}
	

	/**
	 * @return the bondsMolecule
	 */
	public int getBondsMolecule() {
		return bondsMolecule;
	}

	/**
	 * @param bondsMolecule the bondsMolecule to set
	 */
	public void setBondsMolecule(int bondsMolecule) {
		this.bondsMolecule = bondsMolecule;
	}

	public void calculateHash(IBitArray f){
		
		containerListFragmentDefinedByBonds.calculateHash(f);
		
		
	}
	
	public IBitArray get() {
		return containerListFragmentDefinedByBonds.get();
	}
	
	public int getSizeBinaryArray(){
		return containerListFragmentDefinedByBonds.getSizeBinaryArray();
	}
	

	public int getTotalSizeResults (){
		int size = 0;
		
		for (HashMap<IBitArray, IBitArray> hm : liHMFragmentDefinedByBonds) {
			size += hm.size();
		}
				
		return size;
	}
	
	public void reset(){
		for (HashMap<IBitArray, IBitArray> hm : liHMFragmentDefinedByBonds) {
			hm.clear();
		}
		containerListFragmentDefinedByBonds.reset();
	}

	/**
	 * @return the maximumNumberBondsInFragment
	 */
	public int getMaximumCapacityBondsInFragment() {
		return maximumNumberBondsInFragment;
	}
	
	public int getCapacity(){
		return containerListFragmentDefinedByBonds.getCapacity();
	}
	
	public int getAvailable(){
		return containerListFragmentDefinedByBonds.getAvailable();
	}


}
