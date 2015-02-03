package com.actelion.research.chem.mcs;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.RingCollection;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.properties.complexity.IBitArray;
import com.actelion.research.chem.shredder.Fragment;

/**
 * 
 * 
 * BondVector2IdCode
 * 
 * Converts the ouput from ExhaustiveFragmentGeneratorBonds into an Actelion idCode.
 * Takes as input the ListWithIntVec objects which is delivered by ExhaustiveFragmentGeneratorBonds
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * 
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Mar 7, 2012 MvK: Start implementation
 * Jul 7, 2014 MvK: Added ring and aromatic info to idcode.
 */
public class BondVector2IdCode {
	
	private StereoMolecule mol;
	
	
	private List<int []> liRingSets;
	
	public BondVector2IdCode(StereoMolecule mol) {
		this.mol = mol;
		
		liRingSets = new ArrayList<int []>();
				
		RingCollection rc = mol.getRingSet();
		
		int rings = rc.getSize();
		
		for (int i = 0; i < rings; i++) {
			
			int [] arrIndexBnd = rc.getRingBonds(i);
					
			liRingSets.add(arrIndexBnd);
			
		}
	}
	
	/**
	 * 
	 * @param fragDefByBonds
	 * @return true if the fragment contains parts of a ring.
	 */
	public boolean containsFragmentOpenRing(IBitArray fragDefByBonds){
		
		boolean openRing = false;
		
		for(int [] arrIndexBnd : liRingSets){
			
			int ccOverlap=0;
			
			for (int i = 0; i < arrIndexBnd.length; i++) {
				
				if(fragDefByBonds.isBitSet(arrIndexBnd[i])){
					ccOverlap++;
				}
			}
			
			if((ccOverlap > 0) && ccOverlap < arrIndexBnd.length) {
				openRing = true;
				break;
			}
		}
		
		return openRing;
		
	}
	
	public String getFragmentIdCode(IBitArray fragDefByBonds){
		
		StereoMolecule frag = convert(fragDefByBonds, false);
		
		Canonizer can = new Canonizer(frag);
		
		String idcode = can.getIDCode();
		
		return idcode; 
	}
	
	public Fragment getFragment(IBitArray fragDefByBonds){
				
		StereoMolecule frag = convert(fragDefByBonds, false);
		
		Canonizer can = new Canonizer(frag);
		
		String idcode = can.getIDCode();
		
		Fragment fragment = new Fragment(idcode);
		
		fragment.setMol(frag);
		
		fragment.setSize(frag.getBonds());
		
		return fragment; 
	}
	
	public Fragment getFragment(IBitArray fragDefByBonds, boolean addWildcards){
		
		StereoMolecule frag = convert(fragDefByBonds, addWildcards);
		
		Canonizer can = new Canonizer(frag);
		
		String idcode = can.getIDCode();
		
		Fragment fragment = new Fragment(idcode);
		
		fragment.setMol(frag);
		
		fragment.setSize(frag.getBonds());
		
		return fragment; 
	}
	
	
	
	private StereoMolecule convert(IBitArray fragDefByBonds, boolean addWildcards){
		
		int bonds = mol.getBonds();
		
		int atoms = mol.getAtoms();
		
		boolean [] arrBonds = new boolean [bonds];
		
		boolean [] arrAtoms = new boolean [atoms]; 
		
		int bondsFragment = 0;
		
		for (int i = 0; i < bonds; i++) {
			if(fragDefByBonds.isBitSet(i)){
				
				arrBonds[i] = true; 
				
				bondsFragment++;	
				
				arrAtoms[mol.getBondAtom(0, i)] = true;
				
				arrAtoms[mol.getBondAtom(1, i)] = true;
				
			}
		}
		
		int atomsFrag = 0;
		for (int i = 0; i < arrAtoms.length; i++) {
			if(arrAtoms[i]){
				atomsFrag++;
			}
		}
		
		
		StereoMolecule fragSubBonds = new StereoMolecule(atomsFrag, bondsFragment);
		
		
		int [] indexAtoms = mol.copyMoleculeByBonds(fragSubBonds, arrBonds, true, null);
				
		
		// Add ring and aromatic info.
		// Added 07.07.2014
		// 
		
		int indexAtomNew = 0;
		for (int i = 0; i < indexAtoms.length; i++) {
			
			
			if(indexAtoms[i]>-1) {
			
			if((mol.getAtomQueryFeatures(indexAtoms[i]) & Molecule.cAtomQFNotChain) > 0){
				
				fragSubBonds.setAtomQueryFeature(indexAtomNew, Molecule.cAtomQFNotChain, true);
				
			} 
			
			if((mol.getAtomQueryFeatures(indexAtoms[i]) & Molecule.cAtomQFAromatic) > 0){
				
				fragSubBonds.setAtomQueryFeature(indexAtomNew, Molecule.cAtomQFAromatic, true);
				
			}

			indexAtomNew++;
			
			}
				
		}
				
		if(addWildcards) {
			
			boolean [] arrAtomCopied2Fragment = new boolean [mol.getAtoms()];
			
			for (int i = 0; i < indexAtoms.length; i++) {
				
				if(indexAtoms[i] > -1)
					arrAtomCopied2Fragment[i] = true;
			}
			
			for (int i = 0; i < indexAtoms.length; i++) {
				
				if(indexAtoms[i] > -1) {
					
					int atIndexOld = i;
					
					int nConnected = mol.getConnAtoms(atIndexOld);
					
					for (int j = 0; j < nConnected; j++) {
						
						int indexAtConn = mol.getConnAtom(atIndexOld, j);
						
						if(!arrAtomCopied2Fragment[indexAtConn]){
							
							int atWildCard = fragSubBonds.addAtom(0);
							
							int atIndexNew = indexAtoms[i];
							
							fragSubBonds.addBond(atIndexNew, atWildCard, Molecule.cBondTypeSingle);
							
							fragSubBonds.setAtomQueryFeature(atWildCard, Molecule.cAtomQFAny, true);
							
						}
					}
				}
			}
		}
	
		fragSubBonds.ensureHelperArrays(Molecule.cHelperRings);
					
		return fragSubBonds;
	}
	
	public String getFragmentIdCodeCarbonSkeleton(IBitArray fragDefByBonds){
				
		StereoMolecule frag = convert(fragDefByBonds, false);
		
		for (int i = 0; i < frag.getAtoms(); i++) {
			frag.setAtomicNo(i, 6);
		}
		
		Canonizer can = new Canonizer(frag);
		
		String idcode = can.getIDCode();
		
		return idcode; 

	}
	
}
