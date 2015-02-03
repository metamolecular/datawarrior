package com.actelion.research.chem.descriptor.sphere;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.mcs.BondVector2IdCode;
import com.actelion.research.chem.mcs.ExhaustiveFragmentGeneratorBonds;
import com.actelion.research.chem.mcs.ListWithIntVec;
import com.actelion.research.chem.properties.complexity.BitArray128;
import com.actelion.research.chem.properties.complexity.IBitArray;
import com.actelion.research.util.BurtleHasher;
import com.actelion.research.util.datamodel.ByteVec;

/**
 * 
 * 
 * CenteredSkeletonSpheresGenerator
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 28, 2011 MvK: Start implementation
 * Feb 23, 2011 MvK: Spheres changed into exhaustive fragments.
 */
public class CenteredSkeletonSpheresGenerator {
	
    public static final int DEPTH = 8;

    public static final int MAX_FRAGMENZ_SIZE = 5;
    
    private static final int TOTAL_CAPACITY = 100000;
    
    private static final int HASH_BITS = 9;
    
    private static final int HASH_INIT = 13;
    
    protected static final int DESCRIPTOR_SIZE = (1 << HASH_BITS);
    
    private static final int BITS_VECTOR_BONDS = BitArray128.MAX_NUM_BITS;
    
    private ListWithIntVec livBondMaskSpheres;
    
	private ListWithIntVec livNeighbours;

	/**
	 * So many atoms we move away from the center.
	 */
    private int depth;

    /**
     * The maximum size (bonds) of the fragments we will calculate the ahscode for.
     */
    private int nMaximumFragmentSize;
    
    private SSSearcher sss;
	
    private int sizeArrayLIV;
    
    private ExhaustiveFragmentGeneratorBonds efg;
    
    private HashSet<ByteVec> hsIDCode;
    
    private HashSet<ByteVec> hsIDCodeSkeleton;
    
    private boolean storeIdCodes;
    
    
    
	public CenteredSkeletonSpheresGenerator() {
		init();
	}
	
	public CenteredSkeletonSpheresGenerator(CenteredSkeletonSpheresGenerator skpg) {
		init();
		
		depth=skpg.depth;
		
		nMaximumFragmentSize=skpg.nMaximumFragmentSize;
		
		sizeArrayLIV = skpg.sizeArrayLIV;
		
		hsIDCode = skpg.hsIDCode;
		
		hsIDCodeSkeleton = skpg.hsIDCodeSkeleton;
		
		storeIdCodes = skpg.storeIdCodes;
	}
	
	public CenteredSkeletonSpheresGenerator(int nMaximumFragmentSize, int depth) {
		init();
		
		this.nMaximumFragmentSize = nMaximumFragmentSize;
		
		this.depth=depth;
		
	}

	private void init(){
		
		sss = new SSSearcher();
		
		depth=DEPTH;
		
		nMaximumFragmentSize = MAX_FRAGMENZ_SIZE;

		efg = new ExhaustiveFragmentGeneratorBonds(BITS_VECTOR_BONDS, TOTAL_CAPACITY, false);
		
		sizeArrayLIV = efg.getSizeArrayLIV();
		
		livBondMaskSpheres = new ListWithIntVec(sizeArrayLIV);
		
		livNeighbours = new ListWithIntVec(sizeArrayLIV);
		
		hsIDCode = new HashSet<ByteVec>();
		
		hsIDCodeSkeleton = new HashSet<ByteVec>();
		
		storeIdCodes = false;
		
	}
	
	/**
	 * For each map of the fragment one List<byte[]> is added to the descriptor.
	 * @param mol
	 * @param frag
	 * @return
	 */
    public List<List<byte[]>> createDescriptor(StereoMolecule mol, StereoMolecule frag) {
    	
    	hsIDCode.clear();
    	
    	hsIDCodeSkeleton.clear();
    	
    	frag.setFragment(true);
    	
        mol.ensureHelperArrays(Molecule.cHelperRings);
        
    	sss.setMol(frag, mol);
    	
    	if(sss.findFragmentInMolecule()==0){
    		return null;
    	}
    	
    	List<int[]> liArrMap = sss.getMatchList();
    	
    	List<List<byte[]>> liliDescriptor = new ArrayList<List<byte[]>>();
    	
    	for (int[] arrMatchMapAtoms : liArrMap) {
    		List<byte[]> liDescriptor = createDescriptor(mol, arrMatchMapAtoms);
    		
    		liliDescriptor.add(liDescriptor);
		}
    	
    	return liliDescriptor;
    }
	
    /**
     * For each sphere a byte array is added to the list.
     * @param mol
     * @param arrMapIndexAtCenter
     * @return 
     */
    public List<byte[]> createDescriptor(StereoMolecule mol, int [] arrMapIndexAtCenter) {
    	
    	List<byte[]> liArrDescriptor = new ArrayList<byte[]>();
    	
    	efg.set(mol, nMaximumFragmentSize);
    	
    	initBondMask(arrMapIndexAtCenter, mol);
    	
		final int limitDescriptorCounts = DescriptorHandlerCenteredSkeletonFragments.SEP_DESCRIPTOR-1;
		
		BondVector2IdCode bondVector2IdCode = new BondVector2IdCode(mol); 
		
		for (int i = 0; i < depth; i++) {

			byte [] descriptor = new byte[DESCRIPTOR_SIZE];

			calculateAllReachableNeighbourBonds(mol, livBondMaskSpheres);

			int nNeighbours = livNeighbours.size();

			for (int j = 0; j < nNeighbours; j++) {

				int indexStartBond = livNeighbours.get(j);

				livBondMaskSpheres.addBit(indexStartBond);

				efg.generateFragmentsForSingleBond(indexStartBond);

				for (int size = 2; size < nMaximumFragmentSize + 1; size++) {
					
					List<IBitArray> liIndexSubFrag = efg.getFragments(size);

					for (IBitArray livIndexSubFrag : liIndexSubFrag) {

						String idcode = bondVector2IdCode.getFragmentIdCode(livIndexSubFrag);

						String idcodeSkeleton = bondVector2IdCode.getFragmentIdCodeCarbonSkeleton(livIndexSubFrag);

						if(storeIdCodes) {
							hsIDCode.add(new ByteVec(idcode));
							hsIDCodeSkeleton.add(new ByteVec(idcodeSkeleton));
						}
						
						int hashFrag = getHash(idcode);
						if(descriptor[hashFrag]<limitDescriptorCounts) {
							descriptor[hashFrag]++;	
						}
						
						int hashFragSkeleton = getHash(idcodeSkeleton);
						if(descriptor[hashFragSkeleton]<limitDescriptorCounts) {
							descriptor[hashFragSkeleton]++;	
						}
					}
				}
			}
			
			liArrDescriptor.add(descriptor);
		}
    	
    	
    	return liArrDescriptor;
    }
    
    private static final int getHash(String idcode) {
    	
    	int hash = BurtleHasher.hashlittle(idcode, HASH_INIT);
    	
        hash = (hash & BurtleHasher.hashmask(HASH_BITS));
        
        return hash;
    }
    
    
    private void initBondMask(int [] arrMapIndexAt, StereoMolecule mol){
    	
    	livBondMaskSpheres.reset();
    	
    	boolean [] arrIndexAtoms = new boolean [mol.getAtoms()];
    	
    	for (int i = 0; i < arrMapIndexAt.length; i++) {
    		arrIndexAtoms[arrMapIndexAt[i]]=true;
		}
    	
    	
    	for (int i = 0; i < mol.getBonds(); i++) {
			
    		int indexAtom1 = mol.getBondAtom(0, i);
    		
    		int indexAtom2 = mol.getBondAtom(1, i);
    		
    		if(arrIndexAtoms[indexAtom1] && arrIndexAtoms[indexAtom2]){
    			livBondMaskSpheres.addBit(i);
    		}
		}
    	
    }
    
	private final void calculateAllReachableNeighbourBonds(ExtendedMolecule mol, ListWithIntVec livIndexBond){
		
		livNeighbours.reset();
		
		for (int i = 0; i < livIndexBond.size(); i++) {
			
			final int indexBond = livIndexBond.get(i);
			
			final int indexAtom1 = mol.getBondAtom(0, indexBond);
			
			final int indexAtom2 = mol.getBondAtom(1, indexBond);
			
			final int nConnected1 = mol.getAllConnAtoms(indexAtom1);
			
			for (int j = 0; j < nConnected1; j++) {
				
				int indexAtConn = mol.getConnAtom(indexAtom1, j);
				
				int indexBondConnected = mol.getBond(indexAtom1, indexAtConn);
				
				if(!livIndexBond.isBitSet(indexBondConnected)) {
					livNeighbours.addBit(indexBondConnected);
				}
			}
			
			final int nConnected2 = mol.getAllConnAtoms(indexAtom2);
			
			for (int j = 0; j < nConnected2; j++) {
				
				int indexAtConn = mol.getConnAtom(indexAtom2, j);
				
				int indexBondConnected = mol.getBond(indexAtom2, indexAtConn);
				
				if(!livIndexBond.isBitSet(indexBondConnected)) {
					livNeighbours.addBit(indexBondConnected);
				}
			}
		}
		
	}

	public int getDepth() {
		return depth;
	}

	public int getMaximumFragmentSize() {
		return nMaximumFragmentSize;
	}

	public HashSet<ByteVec> getHsIDCode() {
		return hsIDCode;
	}

	public HashSet<ByteVec> getHsIDCodeSkeleton() {
		return hsIDCodeSkeleton;
	}

	public void setStoreIdCodes(boolean storeIdCodes) {
		this.storeIdCodes = storeIdCodes;
	}

    
}
