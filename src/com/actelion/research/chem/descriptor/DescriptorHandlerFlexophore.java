package com.actelion.research.chem.descriptor;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.flexophore.IMolDistHist;
import com.actelion.research.chem.descriptor.flexophore.MolDistHist;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistEncoder;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistViz;
import com.actelion.research.chem.descriptor.flexophore.PPNode;
import com.actelion.research.chem.descriptor.flexophore.completegraphmatcher.ObjectiveFlexophoreHardMatchUncovered;
import com.actelion.research.chem.descriptor.flexophore.generator.CGMult;
import com.actelion.research.chem.descriptor.flexophore.generator.CompleteGraphFunctions;
import com.actelion.research.util.graph.complete.CompleteGraphMatcher;

/**
 * 
 * DescriptorHandlerFlexophore
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 29 Jan 2009 MvK: Start implementation 
 * 15 Oct 2012 MvK renamed DescriptorHandler3DMM2PPInteract-->DescriptorHandlerFlexophore
 * 19 Apr 2013 MvK major changes in Flexophore encoding decoding
 * 25 Apr 2013 MvK Flexophore version changed --> 3.0
 * 07 May 2013 MvK bug fixes in encoding. Flexophore version changed --> 3.1
 * 15 May 2013 MvK Bug fix for the objective function
 */
public class DescriptorHandlerFlexophore implements DescriptorHandler {
	
	/**
	 * If set in parameter string the descriptor handler will be biased to the query descriptor.
	 */
	public static final String ATTR_BIAS2QUERY = "bias2query";
	
	
	private static final double CORRECTION_FACTOR = 0.44;
	
	private static DescriptorHandlerFlexophore INSTANCE;
	
    private static final int MIN_NUM_ATOMS = 6;
    
    private static final int MAX_NUM_ATOMS = ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE * 3;
    
    public static final int NUM_CONFORMATIONS = 100;
    
    public static final int MAX_NUM_SOLUTIONS = 1000;
	
    public static final MolDistHist FAILED_OBJECT = new MolDistHist();
    
    // Version 3.0 after definition of new interaction types by Joel.
    // 07.05.2013 Version 3.1 after bug fixes in encoding.
    private static final String VERSION = "3.1";

	private ConcurrentLinkedQueue<CompleteGraphMatcher<IMolDistHist>> queueCGM;
	
	private AtomicBoolean objectiveQueryBiased;
		
	private MolDistHistEncoder molDistHistEncoder;
	
	//
	// If you change this, do not forget to change the objective in CompleteGraphMatcher<IMolDistHist> getNewCompleteGraphMatcher().
	// 
	private ObjectiveFlexophoreHardMatchUncovered objectiveCompleteGraphHard;
	
    public DescriptorHandlerFlexophore() {
    	init();
	}
    
    /**
     * Deep copy constructor.
     * @param dhf
     */
    public DescriptorHandlerFlexophore(DescriptorHandlerFlexophore dhf) {
    	init();
    	
    	setObjectiveQueryBiased(dhf.objectiveQueryBiased.get());
	}
   
    private void init(){
    	
    	objectiveQueryBiased = new AtomicBoolean(false);

	   	MolDistHistViz.createIndexTables();
	    	   	
	    queueCGM = new ConcurrentLinkedQueue<CompleteGraphMatcher<IMolDistHist>>();
	    
	    queueCGM.add(getNewCompleteGraphMatcher());
	    
	    molDistHistEncoder = new MolDistHistEncoder();
	    
	    objectiveCompleteGraphHard = new ObjectiveFlexophoreHardMatchUncovered();
    }
    
    
    private CompleteGraphMatcher<IMolDistHist> getNewCompleteGraphMatcher(){
    	
    	ObjectiveFlexophoreHardMatchUncovered objective = new ObjectiveFlexophoreHardMatchUncovered();
    	
	    objective.setQueryBias(objectiveQueryBiased.get());
	    
    	CompleteGraphMatcher<IMolDistHist> cgMatcher = new CompleteGraphMatcher<IMolDistHist>(objective);
	    	
	    cgMatcher.setMaxNumSolutions(MAX_NUM_SOLUTIONS);
	    
	    return cgMatcher;
    }
    
    public DescriptorInfo getInfo() {
        return DescriptorConstants.DESCRIPTOR_Flexophore;
    }

    public String getVersion() {
        return VERSION;
    }
   
    public static DescriptorHandlerFlexophore getDefaultInstance(){
    	
    	if(INSTANCE==null){
    		INSTANCE = new DescriptorHandlerFlexophore();
    	}
    	
    	return INSTANCE;
    }
    
    public String encode(Object o) {
    	
    	if(calculationFailed(o)){
    		return FAILED_STRING;
    	}
    	
    	MolDistHist mdh = null;
    	
		if(o instanceof MolDistHist){
			
			mdh = (MolDistHist)o;
			
		} else if(o instanceof MolDistHistViz){
			
			mdh = ((MolDistHistViz)o).getMolDistHist();
			 
		} else {
    		return FAILED_STRING;
		}
		
    	return molDistHistEncoder.encode(mdh);
    	
    }

    public MolDistHist decode(byte[] bytes) {
        try {
			return bytes == null || bytes.length == 0 ? null : Arrays.equals(bytes, FAILED_BYTES) ? FAILED_OBJECT : molDistHistEncoder.decode(bytes);
		} catch (RuntimeException e1) {
			return FAILED_OBJECT;
		}
    }

    public MolDistHist decode(String s) {
        try {
			return s == null || s.length() == 0 ? null
			        : s.equals(FAILED_STRING) ? FAILED_OBJECT
			        :                           molDistHistEncoder.decode(s);
		} catch (RuntimeException e1) {
			return FAILED_OBJECT;
		}
    }

   

    /**
     * Creates the flexophore descriptor of one molecule by splitting
     * the time consuming conformer generation among all available cores.
     * This method should be used when the flexophore of one(!) molecule
     * needs to be calculated quickly. For calculating flexophores of many
     * molecules the preferred method is calling createDescriptor() from
     * multiple threads working on the molecule list.
     * @param mol
     * @return
     */
    public MolDistHist createDescriptorSMT(Object mol, ProgressListener progessListener) {
    	
    	StereoMolecule fragBiggest = (StereoMolecule)mol;
    	fragBiggest.stripSmallFragments();
    	fragBiggest.ensureHelperArrays(StereoMolecule.cHelperCIP);
    	
    	if(fragBiggest.getAtoms() < MIN_NUM_ATOMS){
    		return FAILED_OBJECT;
    	} else if(fragBiggest.getAtoms() > MAX_NUM_ATOMS){
    		return FAILED_OBJECT;
    	}
    	
    	
    	MolDistHist mdh = null;
        try {
            CGMult cgMult = null;
        	cgMult = CompleteGraphFunctions.createMultipleTSFromInteractionTableSMT(fragBiggest, null);
    		mdh = cgMult.getMolDistHist();
		} catch (Exception e) {
			// throw new RuntimeException(e);
		}
        
        if(mdh == null) {
        	return FAILED_OBJECT;
        } else if (mdh.getNumPPNodes() > ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE) {
        	return FAILED_OBJECT;
        }
        
        return mdh;
    }

    public MolDistHist createDescriptor(Object mol) {
    	
    	StereoMolecule fragBiggest = (StereoMolecule)mol;
    	fragBiggest.stripSmallFragments();
    	fragBiggest.ensureHelperArrays(StereoMolecule.cHelperCIP);
    	
    	if(fragBiggest.getAtoms() < MIN_NUM_ATOMS){
    		return FAILED_OBJECT;
    	} else if(fragBiggest.getAtoms() > MAX_NUM_ATOMS){
    		return FAILED_OBJECT;
    	}
    	
    	
    	MolDistHist mdh = null;
        try {
            CGMult cgMult = null;
        	cgMult = CompleteGraphFunctions.createMultipleTSFromInteractionTable(fragBiggest);
    		mdh = cgMult.getMolDistHist();
		} catch (Exception e) {
			// throw new RuntimeException(e);
		}
        
        if(mdh == null) {
        	return FAILED_OBJECT;
        } else if (mdh.getNumPPNodes() > ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE) {
        	return FAILED_OBJECT;
        }
        
        return mdh;
    }

   
    public float getSimilarity(Object query, Object base) {
    	float sc=0;
    	
    	
    	if(objectiveQueryBiased.get()){
    		
    		IMolDistHist mdhvBase = (IMolDistHist)base;
        	
			IMolDistHist mdhvQuery = (IMolDistHist)query;
    		
    		
    		sc = (float) getSimilarity(mdhvBase, mdhvQuery);
    		
    		return sc;
    	}
    	
    	
        if(base == null
        		|| query == null
        		|| ((IMolDistHist)base).getNumPPNodes() == 0
        		|| ((IMolDistHist)query).getNumPPNodes() == 0) {
        	sc = 0;
        	
        } else {
        	
        	IMolDistHist mdhvBase = (IMolDistHist)base;
        	
			IMolDistHist mdhvQuery = (IMolDistHist)query;
			
			if(mdhvBase.getNumPPNodes() > ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE){
				
				System.out.println("DescriptorHandlerFlexophore getSimilarity(...) mdhvBase.getNumPPNodes() " + mdhvBase.getNumPPNodes());
				
				return 0;
			} else if(mdhvQuery.getNumPPNodes() > ObjectiveFlexophoreHardMatchUncovered.MAX_NUM_NODES_FLEXOPHORE){
				System.out.println("DescriptorHandlerFlexophore getSimilarity(...) mdhvQuery.getNumPPNodes() " + mdhvQuery.getNumPPNodes());
				return 0;
			}
			
			try {

				if(!objectiveQueryBiased.get()) {
					
					if(((IMolDistHist)query).getNumPPNodes() < ((IMolDistHist)base).getNumPPNodes()){
						
						mdhvQuery =(IMolDistHist)query;
						
						mdhvBase = (IMolDistHist)base;
						
					} else {
						
						mdhvQuery =(IMolDistHist)base;
						
						mdhvBase = (IMolDistHist)query;
						
					}
					
				}
				
				sc = (float)getMinimumSimilarity(mdhvBase, mdhvQuery);
				
			} catch (Exception e) {
				// should only show unexpected exceptions but not things like: Number of maximum (30) pharmacophores exceeded 36
				e.printStackTrace();
			}
        }
        
        return normalizeValue(sc);
    }
    
    private double getMinimumSimilarity(IMolDistHist mdhvBase, IMolDistHist mdhvQuery){
    	
    	double sc = 0;
    	
    	if(mdhvBase.getNumPPNodes() == mdhvQuery.getNumPPNodes()){
    		double s1 = getSimilarity(mdhvBase, mdhvQuery);
    		double s2 = getSimilarity(mdhvQuery, mdhvBase);
    		
    		sc = Math.max(s1, s2);
    	} else {
    		sc = getSimilarity(mdhvBase, mdhvQuery);
    	}
    	return sc;
    }
    
    
    private double getSimilarity(IMolDistHist mdhvBase, IMolDistHist mdhvQuery){
    	
    	CompleteGraphMatcher<IMolDistHist> cgMatcher = queueCGM.poll();
		
		if(cgMatcher == null){
			cgMatcher = getNewCompleteGraphMatcher();
		}
		
		cgMatcher.set(mdhvBase, mdhvQuery);
		
		double sc = (float)cgMatcher.calculateSimilarity();

		queueCGM.add(cgMatcher);
		
		return sc;
    }
    
	public double getSimilarityNodes(PPNode query, PPNode base) {
		return objectiveCompleteGraphHard.getSimilarityNodes(query, base);
	}


	public float normalizeValue(double value) {
		return value <= 0.0f ? 0.0f
			 : value >= 1.0f ? 1.0f
			 : (float)(1.0-Math.pow(1-Math.pow(value, CORRECTION_FACTOR) ,1.0/CORRECTION_FACTOR));
	}

	public boolean calculationFailed(Object o) {
		
		if(o instanceof MolDistHist){
			 return ((MolDistHist)o).getNumPPNodes() == 0;
		} else if(o instanceof MolDistHistViz){
			 return ((MolDistHistViz)o).getNumPPNodes() == 0;
		}
		
		return true;
       
    }

 	public DescriptorHandler getDeepCopy() {
 		
 		DescriptorHandlerFlexophore dh = new DescriptorHandlerFlexophore(this);
 				
		return dh;
	}

	public void setObjectiveQueryBiased(boolean objectiveQueryBiased) {
		
		this.objectiveQueryBiased.set(objectiveQueryBiased);
		
		objectiveCompleteGraphHard.setQueryBias(objectiveQueryBiased);
		
		int size = queueCGM.size();
		
		queueCGM.clear();
		
		for (int i = 0; i < size; i++) {
			queueCGM.add(getNewCompleteGraphMatcher());
		}
		
		
	}
	

}
