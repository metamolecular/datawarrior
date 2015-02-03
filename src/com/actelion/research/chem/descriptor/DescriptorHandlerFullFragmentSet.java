/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Modest von Korff
 */

package com.actelion.research.chem.descriptor;

import java.util.Arrays;
import java.util.List;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.ExtendedMoleculeFunctions;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.mcs.BondVector2IdCode;
import com.actelion.research.chem.mcs.ExhaustiveFragmentGeneratorBonds;
import com.actelion.research.chem.properties.complexity.BitArray128;
import com.actelion.research.chem.properties.complexity.IBitArray;
import com.actelion.research.util.BurtleHasher;

/**
 * Creates all fragments from a molecule. 
 * Calculates the idcode of a fragment and hashes it into a histogram.
 *
 * Jun 27, 2014 MvK Start implementation
 * Jul 8, 2014 MvK Hash of idcodes instead of unfolded descriptor.
 */
public class DescriptorHandlerFullFragmentSet implements DescriptorHandler<byte[], StereoMolecule> {
	
    private static final int BITS_VECTOR_BONDS = BitArray128.MAX_NUM_BITS;

	public static final int HASH_BITS = 14;
	
	public static final int MAX_BONDS_FRAGS = 7;
	
	public static final boolean CLEAVE_RING_BONDS = true;
	
	public static final boolean ADD_WILD_CARDS = false;
    
    private static final byte[] FAILED_OBJECT = new byte[0];

    private static final int HASH_INIT = 13;

    
    private static DescriptorHandlerFullFragmentSet INSTANCCE;
    
	private boolean addWildCards;
	
	private boolean cleaveRingBonds;
	
	private int maxBondsFrag;
	
	private int hashBits;
	
	private int descriptorFields;
	
	IDCodeParser parser;
	
	private ExhaustiveFragmentGeneratorBonds efg;

	/**
	 * 
	 */
	public DescriptorHandlerFullFragmentSet() {
		this(HASH_BITS, MAX_BONDS_FRAGS, CLEAVE_RING_BONDS, ADD_WILD_CARDS);
	}
	
	public DescriptorHandlerFullFragmentSet(int hashBits, int maxBondsFrag, boolean cleaveRingBonds, boolean addWildCards) {
				
		this.cleaveRingBonds = cleaveRingBonds;
		
		this.addWildCards = addWildCards;
			
		this.maxBondsFrag = maxBondsFrag;
		
		this.hashBits = hashBits;
		
		descriptorFields = (1 << hashBits);
				
		efg = new ExhaustiveFragmentGeneratorBonds(BITS_VECTOR_BONDS, maxBondsFrag, false);
		
		parser = new IDCodeParser();
		
	}
	
	public DescriptorHandlerFullFragmentSet(DescriptorHandlerFullFragmentSet dhFS) {
		
		
		this.cleaveRingBonds = dhFS.cleaveRingBonds;
		
		this.addWildCards = dhFS.addWildCards;
		
		this.maxBondsFrag = dhFS.maxBondsFrag;
		
		this.hashBits = dhFS.hashBits;
		
		this.descriptorFields = dhFS.descriptorFields;
		
		efg = new ExhaustiveFragmentGeneratorBonds(BITS_VECTOR_BONDS, maxBondsFrag, false);
		
		parser = new IDCodeParser();
	}
	
	private int getIndexInDescriptorFromIdCode(String idCode){
		
		int hash = BurtleHasher.hashlittle(idCode, HASH_INIT);
		
		int indexInDescriptor = (hash & BurtleHasher.hashmask(hashBits));
		
		return indexInDescriptor;
		
	}

	
	public byte [] createDescriptor(StereoMolecule mol){
		
		byte [] arrDescriptor = new byte [descriptorFields];
		
		//
		// Create descriptor for original molecule
		//
		add(mol, arrDescriptor);
		
		//
		// Create descriptor for molecule with stripped stereo information
		//
		StereoMolecule molStrippedStereo = new StereoMolecule(mol);
		
		molStrippedStereo.ensureHelperArrays(Molecule.cHelperSymmetryEnantiotopic);
		
		molStrippedStereo.stripIsotopInfo();
		
		molStrippedStereo.ensureHelperArrays(Molecule.cHelperSymmetryEnantiotopic);
		
		
		Canonizer can = new Canonizer(molStrippedStereo);
		
		molStrippedStereo = parser.getCompactMolecule(can.getIDCode());
		
		molStrippedStereo.ensureHelperArrays(Molecule.cHelperSymmetryEnantiotopic);
		
		add(molStrippedStereo, arrDescriptor);
				
		//
		// Create descriptor for molecule with stripped stereo information and all hetero atoms replaced by carbon
		//
		StereoMolecule skel = ExtendedMoleculeFunctions.getConverted2CarbonSkeleton(molStrippedStereo);
		
		skel.ensureHelperArrays(Molecule.cHelperRings);
		
		add(skel, arrDescriptor);
		

		return arrDescriptor;
		
	}
	
	private void add(StereoMolecule mol, byte [] arrDescriptor){
		
		BondVector2IdCode bondVector2IdCode = new BondVector2IdCode(mol);

		efg.set(mol, maxBondsFrag);
		
		efg.generateFragmentsAllBonds();
		
		int bonds = mol.getBonds();
				
		int bondsFragments = Math.min(bonds, maxBondsFrag+1);

		for (int i = 0; i < bondsFragments; i++) {
			
			List<IBitArray> liFragDefByBnds = efg.getFragments(i);
			
			for (IBitArray fragDefByBnds : liFragDefByBnds) {
				
				if(!cleaveRingBonds && bondVector2IdCode.containsFragmentOpenRing(fragDefByBnds)){
					continue;
				}
											
				String idcode = bondVector2IdCode.getFragment(fragDefByBnds, addWildCards).getIdcode();
												
				final int index = getIndexInDescriptorFromIdCode(idcode);
							
                if (arrDescriptor[index] < DescriptorEncoder.MAX_COUNT_VALUE)
                	arrDescriptor[index]++;

			}
		}
	}
	
	
	public int getSize(){
		return descriptorFields;
	}
	
	
	/* 
	 * @see com.actelion.research.chem.descriptor.ISimilarityCalculator#getSimilarity(java.lang.Object, java.lang.Object)
	 */
	@Override
	public float getSimilarity(byte [] arr1, byte [] arr2) {
			
		
		float similarity = 0;
		
		float ccOverlap=0;
		
		float ccTotal=0;

		for (int i = 0; i < arr2.length; i++) {
			
			ccOverlap += Math.min(arr1[i], arr2[i]);
			ccTotal += Math.max(arr1[i], arr2[i]);
		
		}
		
        similarity = ccOverlap / ccTotal;
		
		return similarity;
	}
	
	/* (non-Javadoc)
	 * @see com.actelion.research.chem.descriptor.DescriptorHandler#getInfo()
	 */
	@Override
	public DescriptorInfo getInfo() {
		return DescriptorConstants.DESCRIPTOR_FULL_FRAGMENT_SET;
	}

	/* (non-Javadoc)
	 * @see com.actelion.research.chem.descriptor.DescriptorHandler#getVersion()
	 */
	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	

	/* (non-Javadoc)
	 * @see com.actelion.research.chem.descriptor.DescriptorHandler#getDeepCopy()
	 */
	@Override
	public DescriptorHandlerFullFragmentSet getDeepCopy() {
		
		return new DescriptorHandlerFullFragmentSet(this);
	}

    public byte[] decode(String s) {
        return s == null ?               null
             : s.equals(FAILED_STRING) ? FAILED_OBJECT
             :                           new DescriptorEncoder().decodeCounts(s);
        }

    public byte[] decode(byte[] bytes) {
        return bytes == null ?               		null
             : Arrays.equals(bytes, FAILED_BYTES) ? FAILED_OBJECT
             :                           			new DescriptorEncoder().decodeCounts(bytes);
        }

    public String encode(byte[] o) {
        return calculationFailed(o) ? FAILED_STRING
             : new String(new DescriptorEncoder().encodeCounts(o));
        }


	/* (non-Javadoc)
	 * @see com.actelion.research.chem.descriptor.DescriptorHandler#calculationFailed(java.lang.Object)
	 */
    public boolean calculationFailed(byte[] o) {
        return o==null || o.length == 0;
    }

	public static DescriptorHandlerFullFragmentSet getDefaultInstance() {
		
		synchronized(DescriptorHandlerFullFragmentSet.class) {
			
			if (INSTANCCE == null)
				INSTANCCE = new DescriptorHandlerFullFragmentSet();
		}
		
		return INSTANCCE;
		
	}


}
