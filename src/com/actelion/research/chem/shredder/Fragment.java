package com.actelion.research.chem.shredder;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.util.datamodel.IDCodeCoord;

/**
 * Fragment
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Jan 18, 2013 MvK Start implementation
 */
public class Fragment extends IDCodeCoord {
	
    public static final String TAG_FREQUENCY_ONE_PER_MOLECULE = "FrequencyOnePerMolecule";
    
    public static final String TAG_FREQUENCY = "FrequencySumAll";
    
    public static final String TAG_RELATIVE_FREQUNCY = "Relative frequencySumAll";
    
    public static final String TAG_SIZE = "Size fragment"; 

    // This frequency term is only incremented ones per molecule.
	private int frequencyOnePerMol;
	
	// This frequency term counts all fragments. One molecule can have several identical fragments. 
	private int frequencySumAll;
	
	private int size;
	
	private StereoMolecule mol;
	

	public Fragment(String idcode) {
		super(idcode);
	}

	
	/**
	 * @param idcode
	 * @param coordinates
	 */
	public Fragment(String idcode, String coordinates) {
		super(idcode, coordinates);
	}



	/**
	 * @return the frequencyOnePermol
	 */
	public int getFrequencyOnePerMol() {
		return frequencyOnePerMol;
	}


	/**
	 * @param frequencyOnePermol the frequencyOnePermol to set
	 */
	public void setFrequencyOnePerMol(int frequencyOnePerMol) {
		this.frequencyOnePerMol = frequencyOnePerMol;
	}


	/**
	 * @return the frequency
	 */
	public int getFrequencySumAll() {
		return frequencySumAll;
	}


	/**
	 * @param frequency the frequency to set
	 */
	public void setFrequencySumAll(int frequency) {
		this.frequencySumAll = frequency;
	}

	public void incrementFrequencySumAll() {
		this.frequencySumAll++;
	}
	
	public void incrementFrequencyOnePerMol() {
		this.frequencyOnePerMol++;
	}
	

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}


	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		this.size = size;
	}
	
	public boolean equals(Object o) {
		
		boolean equals = true;
		
		if(!(o instanceof Fragment)){
			return false;
		}
		
		Fragment f = (Fragment) o;
		
		if(!idcode.equals(f.idcode)){
			equals = false;
		}
				
		return equals;
	}
	
	/**
	 * @return the mol
	 */
	public StereoMolecule getMol() {
		return mol;
	}


	/**
	 * @param mol the mol to set
	 */
	public void setMol(StereoMolecule mol) {
		this.mol = mol;
	}



}
