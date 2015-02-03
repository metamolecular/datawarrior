package com.actelion.research.chem.descriptor;

import java.util.Arrays;

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.flexophore.IMolDistHist;
import com.actelion.research.chem.descriptor.flexophore.MolDistHist;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistEncoder;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistViz;
import com.actelion.research.chem.descriptor.flexophore.completegraphmatcher.ObjectiveFlexophoreHardMatchUncovered;
import com.actelion.research.chem.descriptor.flexophore.generator.CGMult;
import com.actelion.research.chem.descriptor.flexophore.highres.CompleteGraphFunctionsHighRes;
import com.actelion.research.util.graph.complete.CompleteGraphMatcher;

/**
 * 
 * DescriptorHandlerFlexophoreighRes
 * Flexophore descriptor with a finer granularity than the original Flexophore.
 * Hetero atoms in rings are treated separately from the aliphatic ring atoms.
 * Only the create method differs from the Flexophore. The similarity calculation is identical.
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 8 Dec 2010 MvK: Start implementation
 * 12 Oct 2012 MvK: ObjectiveCGMolDistHistViz replaces old similarity calculation.
 */
public class DescriptorHandlerFlexophoreHighRes implements DescriptorHandler {
	
    public static final int NUM_CONFORMATIONS = 50;
    
    public static final MolDistHist FAILED_OBJECT = new MolDistHist();
    
    private static final int MAX_NUM_SOLUTIONS = 1000;
    
    private static final String VERSION = "2.0";

    private static DescriptorHandlerFlexophoreHighRes DH;
    
    private ObjectiveFlexophoreHardMatchUncovered objectiveCompleteGraph;
	
    private CompleteGraphMatcher<IMolDistHist> cgMatcher;
	
	private MolDistHistEncoder molDistHistEncoder;

    private boolean addVizInfo;
    
    public DescriptorHandlerFlexophoreHighRes() {
    	
    	MolDistHistViz.createIndexTables();
    	
    	objectiveCompleteGraph = new ObjectiveFlexophoreHardMatchUncovered();
    	
    	cgMatcher = new CompleteGraphMatcher<IMolDistHist>(objectiveCompleteGraph);
    	
    	cgMatcher.setMaxNumSolutions(MAX_NUM_SOLUTIONS);
    	
    	molDistHistEncoder = new MolDistHistEncoder();
	}
    
    public DescriptorInfo getInfo() {
        return DescriptorConstants.DESCRIPTOR_Flexophore_HighRes;
    }

    public String getVersion() {
        return VERSION;
    }

    public static DescriptorHandlerFlexophoreHighRes getInstance(){
    	synchronized(DescriptorHandlerFlexophoreHighRes.class) {
    		if(DH==null) {
        		DH = new DescriptorHandlerFlexophoreHighRes();
        	}
    	}

    	return DH;
    }
    
    public String encode(Object o) {
    	return calculationFailed(o) ? FAILED_STRING : molDistHistEncoder.encode((MolDistHist)o);
    }

    public MolDistHist decode(byte[] bytes) {
        try {
			return bytes == null || bytes.length == 0 ? null
			        : Arrays.equals(bytes, FAILED_BYTES) ? FAILED_OBJECT
			        :                           molDistHistEncoder.decode(bytes);
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

    public MolDistHist createDescriptor(Object mol) {
    	
    	StereoMolecule fragBiggest = (StereoMolecule)mol;
    	fragBiggest.stripSmallFragments();
    	fragBiggest.ensureHelperArrays(StereoMolecule.cHelperCIP);
    	MolDistHist mdh = null;
        try {
            CGMult cgMult = null;
        	cgMult = CompleteGraphFunctionsHighRes.createMultipleTSFromInteractionTable(fragBiggest, null);
    		mdh = cgMult.getMolDistHistVizFineGranulated().getMolDistHist();
		} catch (Exception e) {
			
			e.printStackTrace();
			// throw new RuntimeException(e);
		}
        return (mdh == null) ? FAILED_OBJECT : mdh;
    }
    
    public MolDistHist createDescriptor(Object mol, ProgressListener progessListener) {
    	
    	StereoMolecule fragBiggest = (StereoMolecule)mol;
    	fragBiggest.stripSmallFragments();
    	fragBiggest.ensureHelperArrays(StereoMolecule.cHelperCIP);
    	MolDistHist mdh = null;
        try {
            CGMult cgMult = null;
        	cgMult = CompleteGraphFunctionsHighRes.createMultipleTSFromInteractionTable(fragBiggest, progessListener);
    		mdh = cgMult.getMolDistHist();
		} catch (Exception e) {
			// throw new RuntimeException(e);
		}
        return (mdh == null) ? FAILED_OBJECT : mdh;
    }

    /**
     * Uses <code>MDHMapper</code> to find the best fitting sub-graphs. 
     * Generates sub Flexophores to find the optimum matching pairs.
     * @param m1
     * @param m2
     * @return
     */
    public float getSimilarity(Object base, Object query) {
    	float sc=0;
        if(base == null
        		|| query == null
        		|| ((MolDistHist)base).getNumPPNodes() == 0
        		|| ((MolDistHist)query).getNumPPNodes() == 0) {
        	sc = 0;
        } else {
        	
        	MolDistHistViz mdhvBase = new MolDistHistViz((MolDistHist)base);
        	MolDistHistViz mdhvQuery = new MolDistHistViz((MolDistHist)query);
	    	
			try {
				
				cgMatcher.set(mdhvBase, mdhvQuery);
				
				sc = (float)cgMatcher.calculateSimilarity();

				
			} catch (Exception e) {
				// should only show unexpected exceptions but not things like: Number of maximum (30) pharmacophores exceeded 36
//				e.printStackTrace();
			}
        }
        return sc;
    }
    
    
    public boolean calculationFailed(Object o) {
        return ((MolDistHist)o).getNumPPNodes() == 0;
    }

	public boolean isAddVizInfo() {
		return addVizInfo;
	}

	public void setAddVizInfo(boolean addVizInfo) {
		this.addVizInfo = addVizInfo;
	}
	
	public DescriptorHandler getDeepCopy() {
		return new DescriptorHandlerFlexophoreHighRes();
	}

}
