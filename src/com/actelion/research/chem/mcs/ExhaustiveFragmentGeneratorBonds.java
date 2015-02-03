package com.actelion.research.chem.mcs;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.properties.complexity.ContainerFragBondsSolutions;
import com.actelion.research.chem.properties.complexity.IBitArray;
import com.actelion.research.util.SizeOf;
import com.actelion.research.util.datamodel.IntArray;



/**
 * 
 * 
 * ExhaustiveFragmentGeneratorBonds
 * Working on bonds.
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 26, 2011 MvK: Start implementation
 * Feb 10, 2012 MvK: hash tables replaced by direct index comparisons.
 */
public class ExhaustiveFragmentGeneratorBonds {
	
	private static boolean ELUSIVE = false; 
	
	public static final long LIMIT_NEIGHBOURS_SINCE_LAST_ADDED = 50000000;
		
	// This list contains the solutions. Each row in the list contains the solutions for the corresponding number of atom types.
	// Consequently the first two rows are empty.
	private ContainerFragBondsSolutions containerDataFragDefByBonds;
	
	private ExtendedMolecule mol;
	
	private int nBondsMolecule;
	
	private int maximumNumberBondsInFrag;
	
	private IntArray arrIndexReachableNeighbours;
	
	
	private boolean fragmentsGenerated;
		
	private boolean capacityLimitBreakes;
		
	private long solutionAdded;
	
	
	/**
	 * 
	 * @param capacityMaximumNumberBondsInFragment
	 * @param exhaustiveCalculationAllBonds only needed to calculate the capacity for the container.
	 */
	public ExhaustiveFragmentGeneratorBonds(int bits, int capacityMaximumNumberBondsInFragment, boolean exhaustiveCalculationAllBonds) {
		
		containerDataFragDefByBonds = new ContainerFragBondsSolutions(bits, capacityMaximumNumberBondsInFragment, exhaustiveCalculationAllBonds);
				
		arrIndexReachableNeighbours = new IntArray();
		
		if(ELUSIVE)
			System.out.println("ExhaustiveFragmentGeneratorBonds constructor finished, used mem " + SizeOf.usedMemoryMB() + "[MB].");
	

	}
	
	
	public void set(ExtendedMolecule mol, int nMaximumNumberBonds) {
		
		this.mol = mol;
		
		nBondsMolecule = mol.getBonds();
		
		if(nBondsMolecule > containerDataFragDefByBonds.getSizeBinaryArray()){
			throw new RuntimeException("Maximum number of bonds exceeded.");
		}
				
		this.maximumNumberBondsInFrag = Math.min(nMaximumNumberBonds, containerDataFragDefByBonds.getMaximumCapacityBondsInFragment());
				
		fragmentsGenerated = false;
				
		capacityLimitBreakes=false;
		
		containerDataFragDefByBonds.setBondsMolecule(nBondsMolecule);
		
	}

	private void initDataContainerAllSingleBonds(){
		
		containerDataFragDefByBonds.reset();
		
		for (int i = 0; i < nBondsMolecule; i++) {
			IBitArray f = containerDataFragDefByBonds.get();
			
			f.setBit(i);
						
			containerDataFragDefByBonds.addFacultative(f);
		}
	}
	
	
	private void initDataContainerOneSingleBond(int indexBond) {
		
		containerDataFragDefByBonds.reset();
				
		IBitArray f = containerDataFragDefByBonds.get();
		
		f.setBit(indexBond);
					
		containerDataFragDefByBonds.addFacultative(f);
	}
	
	public void generateFragmentsAllBonds(){
		
		initDataContainerAllSingleBonds();
		
		generateFragments();
	}
	
	
	public void generateFragmentsForSingleBond(int indexBond) {
		
		initDataContainerOneSingleBond(indexBond);
		
		generateFragments();
		
	}
	
	private void generateFragments(){
		
		int maxNumBondsFrag = Math.min(nBondsMolecule, maximumNumberBondsInFrag);
		
		if(maxNumBondsFrag==1){
			return;
		}
		
		long neighboursTotal = 0;
		long addedTotal = 0;

		int maxSizeParentList = 10000;
		
		
		for (int i = 1; i < maxNumBondsFrag; i++) {
			
			List<IBitArray> liParent = containerDataFragDefByBonds.getList(i);
			
//			if(liParent.size()>maxSizeParentList){
//				Collections.shuffle(liParent);
//						
//				for (int j = liParent.size()-1; j >= maxSizeParentList; j--) {
//					liParent.remove(j);
//				}
//			
//			}
			
			if(ELUSIVE) {
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() bonds  " + i + ". Parents " + liParent.size() + ".");

			}

			long added = 0;
			
			long neighbours = 0;
			
			long neighboursSinceLastAdded = 0;
			
			for (IBitArray fParent : liParent) {
				
				IntArray arrBondsReachable = getAllReachableNeighbourBonds(mol, fParent);
				 
				for (int j = 0; j < arrBondsReachable.length(); j++) {
										
					IBitArray fChildAddedBond = containerDataFragDefByBonds.getWithCopy(fParent);
					
					fChildAddedBond.setBit(arrBondsReachable.get(j));
					
					// System.out.println(fChildAddedBond.toString());
					
					if(containerDataFragDefByBonds.addFacultative(fChildAddedBond)){
						added++;
						neighboursSinceLastAdded = 0;
					}
					neighbours++;
					
					neighboursSinceLastAdded++;
					
					if(neighbours % 50000000 == 0){
						if(ELUSIVE) {
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() " + new Date().toString() + ".");
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() neighbours generated " + neighbours + ".");
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() neighbours generated total " + neighboursTotal + ".");
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() solutions added " + added + ".");
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() solutions added total " + addedTotal + ".");
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() available  " + containerDataFragDefByBonds.getAvailable() + ".");
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() capacity  " + containerDataFragDefByBonds.getCapacity() + ".");
							System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() neighboursSinceLastAdded  " + neighboursSinceLastAdded + ".");
						}
					}
				}
				
				if(neighboursSinceLastAdded > LIMIT_NEIGHBOURS_SINCE_LAST_ADDED) {
					System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments(). Break for fragments with " + i + " bonds. Generated  " + neighboursSinceLastAdded + " neighbours since last add to hash map.");
					break;
				}
				
				
			}
			
			// System.out.println("Bonds\t" + i + "\tparents\t" + liParent.size() + "\tneighbours added\t" + added);

			
			neighboursTotal += neighbours;
			
			addedTotal += added;
			
			if(ELUSIVE) {
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() " + new Date().toString() + ".");
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() neighbours generated " + neighbours + ".");
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() neighbours generated total " + neighboursTotal + ".");
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() solutions added " + added + ".");
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() solutions added total " + addedTotal + ".");
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() available  " + containerDataFragDefByBonds.getAvailable() + ".");
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() capacity  " + containerDataFragDefByBonds.getCapacity() + ".");
				System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() neighboursSinceLastAdded  " + neighboursSinceLastAdded + ".");
			}
		}
		
		fragmentsGenerated = true;
		
		if(ELUSIVE) {
			System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() getTotalSizeResultList() " + containerDataFragDefByBonds.getTotalSizeResults() + ".");
			System.out.println("ExhaustiveFragmentGeneratorBonds generateFragments() solutionAdded " + solutionAdded + ".");
		}
	}

	/**
	 * Returns list with indices for fragments with <code>size</code> bonds. The indices are coded as bit lists.
	 * @param bonds
	 * @return
	 */
	public List<IBitArray> getFragments(int bonds) {
		
		if(!fragmentsGenerated){
			throw new RuntimeException("Fragments have to be generated first. Call generateFragments().");
		}
		
		return containerDataFragDefByBonds.getList(bonds);
	}
	
	/**
	 * get all bonds that can be reached in one step from the input bonds.
	 * @param mol
	 * @param livIndexBond
	 * @return
	 */
	private final IntArray getAllReachableNeighbourBonds(ExtendedMolecule mol, IBitArray livIndexBond){
		
		arrIndexReachableNeighbours.reset();
				
		for (int i = 0; i < nBondsMolecule; i++) {
			
			if(!livIndexBond.isBitSet(i)){
				continue;
			}
			
			final int indexBond = i;
			
			final int indexAtom1 = mol.getBondAtom(0, indexBond);
			
			final int indexAtom2 = mol.getBondAtom(1, indexBond);
			
			final int nConnected1 = mol.getAllConnAtoms(indexAtom1);
			
			for (int j = 0; j < nConnected1; j++) {
				
				int indexAtConn = mol.getConnAtom(indexAtom1, j);
				
				int indexBondConnected = mol.getBond(indexAtom1, indexAtConn);
									
				if(!livIndexBond.isBitSet(indexBondConnected)) {
					
					solutionAdded++;
					
					arrIndexReachableNeighbours.add(indexBondConnected);
				}
			}
			
			final int nConnected2 = mol.getAllConnAtoms(indexAtom2);
			
			for (int j = 0; j < nConnected2; j++) {
				
				int indexAtConn = mol.getConnAtom(indexAtom2, j);
				
				int indexBondConnected = mol.getBond(indexAtom2, indexAtConn);
											
				if(!livIndexBond.isBitSet(indexBondConnected)) {
					
					solutionAdded++;
					
					arrIndexReachableNeighbours.add(indexBondConnected);
				}
			}
		}
		
		return arrIndexReachableNeighbours;
	}

	/**
	 * If true not all index combinations where generated.
	 * Starts with 0 for each new molecule. 
	 * @return
	 */
	public boolean isCapacityLimitBreakes() {
		return capacityLimitBreakes;
	}
	
	
	public int getSizeArrayLIV(){
		
		int maxSizeIntVec = (containerDataFragDefByBonds.getSizeBinaryArray() + Integer.SIZE-1)/ Integer.SIZE;
		
		return maxSizeIntVec;

	}
	
	public int getMaximumCapacityBondsInFragment() {
		return containerDataFragDefByBonds.getMaximumCapacityBondsInFragment();
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
