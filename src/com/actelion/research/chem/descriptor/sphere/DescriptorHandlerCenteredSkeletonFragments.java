package com.actelion.research.chem.descriptor.sphere;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorEncoder;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorInfo;

/**
 * 
 * 
 * DescriptorHandlerCenteredSkeletonSpheres
 * This descriptor has to be parameterized with a 
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 28, 2011 MvK: Start implementation
 * Feb 2012 MvK: Rewritten and renamed DescriptorHandlerCenteredSkeletonSpheres --> DescriptorHandlerCenteredSkeletonFragments
 */
public class DescriptorHandlerCenteredSkeletonFragments implements DescriptorHandler {
	
	public static final String SEP_TAG_PARAMETER = ".";
	
    /**
     * Separates the descriptor for one substructure from the other.
     */
    protected static final byte SEP_DESCRIPTOR = (byte) 62;

    private static final List<List<int[]>> FAILED_OBJECT = new ArrayList<List<int[]>>();
    
    private static DescriptorHandlerCenteredSkeletonFragments INSTANCE;

    private CenteredSkeletonSpheresComparator centeredSkeletonSpheresComparator;
    
    private CenteredSkeletonSpheresGenerator centeredSkeletonSpheresGenerator;
    
    private int nMaximumFragmentSize;
    
    private int depth;
    
    private DescriptorEncoder descriptorEncoder;
    
    private StereoMolecule frag;
    
    
    
    public DescriptorHandlerCenteredSkeletonFragments() {
    	
    	init(CenteredSkeletonSpheresGenerator.MAX_FRAGMENZ_SIZE, CenteredSkeletonSpheresGenerator.DEPTH, null);
    	
	}
    
    public DescriptorHandlerCenteredSkeletonFragments(
    		int nMaximumFragmentSize,
    		int depth,
    		CenteredSkeletonSpheresComparator centSkelFragComparator) {
    	
    	init(nMaximumFragmentSize, depth, centSkelFragComparator);
    	
	}

    /**
     * 
     * @param tagHeader_Separator_IdcodeFragment Descriptor name followed by the separator and then the idcode of the skeleton fragment.
     */
    public DescriptorHandlerCenteredSkeletonFragments(String tagHeader_Separator_IdcodeFragment) {
    	
    	init(CenteredSkeletonSpheresGenerator.MAX_FRAGMENZ_SIZE, CenteredSkeletonSpheresGenerator.DEPTH, null);
    	
    	int index = tagHeader_Separator_IdcodeFragment.indexOf(SEP_TAG_PARAMETER)+1;
		
        if(index == -1){
        	throw new RuntimeException("No fragment given in constructor tag.");
        }
    	
		String idcode = tagHeader_Separator_IdcodeFragment.substring(index) ;
		
		IDCodeParser parser = new IDCodeParser(false);
		
		setFrag(parser.getCompactMolecule(idcode));
    }
    
    public DescriptorHandlerCenteredSkeletonFragments(DescriptorHandlerCenteredSkeletonFragments dhCSS) {
    	
    	CenteredSkeletonSpheresComparator centSkelFragComparator = new CenteredSkeletonSpheresComparator(dhCSS.centeredSkeletonSpheresComparator);
    	
    	init(dhCSS.getMaximumFragmentSize(), dhCSS.getDepth(), centSkelFragComparator);
    	
    	frag = new StereoMolecule(dhCSS.frag);
    	
    	frag.ensureHelperArrays(Molecule.cHelperRings);
    	
    	
    	
	}
    

    private void init(
    		int nMaximumFragmentSize,
    		int depth,
    		CenteredSkeletonSpheresComparator centSkelFragComparator){
    	
    	this.nMaximumFragmentSize = nMaximumFragmentSize;
    	
    	this.depth = depth;
    	
    	
    	if(centSkelFragComparator==null) {
    		this.centeredSkeletonSpheresComparator = new CenteredSkeletonSpheresComparator(depth);
    	} else {
    		this.centeredSkeletonSpheresComparator = centSkelFragComparator;
    	}
    	
    	descriptorEncoder = new DescriptorEncoder();
    }
    
    public DescriptorInfo getInfo() {
    	
        return DescriptorConstants.DESCRIPTOR_CenteredSkeletonFragments;
    }

    public String getVersion() {
        return "1.0";
    }
    
	public int getDepth() {
		return depth;
	}

	public int getMaximumFragmentSize() {
		return nMaximumFragmentSize;
	}

    /**
     * @return List<List<byte[]>> size of the list equals the number of matching skeletons found in the molecule.
     */
    public Object createDescriptor(Object chemObject) {
    	
        if (!(chemObject instanceof StereoMolecule))
            return null;

        if(frag == null){
        	throw new RuntimeException("No fragment given in descriptor.");
        }
        
        StereoMolecule mol = (StereoMolecule)chemObject;
    	
        if(centeredSkeletonSpheresGenerator==null){
        	centeredSkeletonSpheresGenerator=new CenteredSkeletonSpheresGenerator(nMaximumFragmentSize, depth);
        }
        
    	return centeredSkeletonSpheresGenerator.createDescriptor(mol, frag);
    }

    @SuppressWarnings("unchecked")
	public float getSimilarity(Object o1, Object o2) {
        if (o1 == null || o2 == null)
            return Float.NaN;

        if(frag== null){
            return Float.NaN;
        }
        
        List<List<byte[]>> liliDescriptor1 = (List<List<byte[]>>)o1;
        
        List<List<byte[]>> liliDescriptor2 = (List<List<byte[]>>)o2;
        
        double similarity = centeredSkeletonSpheresComparator.getWeightedMaximumSimilarity(liliDescriptor1, liliDescriptor2);

        // return normalizer.normalize((float)similarity);
        
        return (float)similarity;
    }

    /**
     * 
     * @return unique header tag for the given fragment.
     */
    public String getHeaderTag(){
    	
        if(frag == null){
        	throw new RuntimeException("No fragment given in descriptor handler.");
        }

    	Canonizer can = new Canonizer(frag);
    	
    	String idcode = can.getIDCode();
    	
    	return getInfo().shortName+SEP_TAG_PARAMETER+idcode;
    }
    
	public void setFrag(StereoMolecule frag) {
		this.frag = frag;
	}
	
	public StereoMolecule getFrag() {
		return frag;
	}


	public void setWeighingScheme(double[] arrWeighingScheme) {
		centeredSkeletonSpheresComparator.setWeighingScheme(arrWeighingScheme);
	}

	public boolean calculationFailed(Object o) {
		
		if(o==null || o.equals(FAILED_OBJECT)){
			return true;
		}
		
		return false;
	}
	
	public static boolean isTag(String s) {
		
		if(s==null){
			return false;
		}
		
		String shortName = DescriptorConstants.DESCRIPTOR_CenteredSkeletonFragments.shortName;
		
		return (s.startsWith(shortName)) ? true : false;
	}
	
	/**
	 * 
	 * @param tagHeader_Separator_IdcodeFragment
	 * @return true if the idcode part of the tag is equal to the idcode in this instance.
	 */
	public boolean checkInstance(String tagHeader_Separator_IdcodeFragment) {
		
		int index = tagHeader_Separator_IdcodeFragment.indexOf(SEP_TAG_PARAMETER)+1;
		
        if(index == -1){
        	throw new RuntimeException("No fragment given in constructor tag.");
        }
    	
		String idcode = tagHeader_Separator_IdcodeFragment.substring(index) ;
		
		Canonizer can = new Canonizer(frag);
		
		String idcodeFrag = can.getIDCode();
		
		return (idcode.equals(idcodeFrag)) ? true : false;
	}

	public Object decode(String s) {
		return decode(s.getBytes());
	}
		
	public Object decode(byte[] bytes) {

		byte [] arr = descriptorEncoder.decodeCounts(bytes);
		
		List<List<byte[]>> liliDescriptor =  new ArrayList<List<byte[]>>();
		
		List<byte[]> liDescriptorOneArray = new ArrayList<byte[]>();
		
		int start = 0;
		for (int i = 0; i < arr.length-1; i++) {
			if((arr[i]==SEP_DESCRIPTOR) && (arr[i+1]==SEP_DESCRIPTOR)) {
				
				int length = i-start;
				
				byte [] arrDescriptorOneArray = new byte [length];
				
				System.arraycopy(arr, start, arrDescriptorOneArray, 0, length);
				
				liDescriptorOneArray.add(arrDescriptorOneArray);
				
				start=i+2;
			}
		}
		
		// System.out.println("arr.length " + arr.length + " arr[start-1] " + arr[start-1]);
		
		int length = arr.length-start;
		
		byte [] arrDescriptorOneArrayLast = new byte [length];
		
		System.arraycopy(arr, start, arrDescriptorOneArrayLast, 0, length);
		
		liDescriptorOneArray.add(arrDescriptorOneArrayLast);
		
		for (byte[] descriptorOneArray : liDescriptorOneArray) {
		
			start = 0;
			List<byte[]> liDescriptor = new ArrayList<byte[]>();
			
			for (int i = 0; i < descriptorOneArray.length; i++) {
				if(descriptorOneArray[i]==SEP_DESCRIPTOR) {
					length = i-start;
					
					if(length != CenteredSkeletonSpheresGenerator.DESCRIPTOR_SIZE){
						
						throw new RuntimeException("Error in encoded descriptor.");
					}
					
					byte[] arrDescr = new byte [length];
					
					System.arraycopy(descriptorOneArray, start, arrDescr, 0, length);
					
					liDescriptor.add(arrDescr);
					
					start=i+1;
				}
			}
			
			length = descriptorOneArray.length-start;
			
			if(length != CenteredSkeletonSpheresGenerator.DESCRIPTOR_SIZE){
				throw new RuntimeException("Error in encoded descriptor.");
			}
			
			byte[] arrDescr = new byte [length];
			
			System.arraycopy(descriptorOneArray, start, arrDescr, 0, length);
			
			liDescriptor.add(arrDescr);
						
			liliDescriptor.add(liDescriptor);
		}
		
		return liliDescriptor;
	}

	@SuppressWarnings("unchecked")
	public String encode(Object o) {
		
		List<List<byte[]>> liliDescriptor = (List<List<byte[]>>)o;
		
		// Seperates two complete descriptors
		// Will happen if the center defined by the fragment occurs more than once in the molecule. 
		int nSEPDescriptors = liliDescriptor.size()-1;
		
		int nSEPSphereDescriptors = 0;
		
		int nEntries = 0;
		for (int i = 0; i < liliDescriptor.size(); i++) {
			
			List<byte[]> liDescriptor = liliDescriptor.get(i);
		
			nSEPSphereDescriptors += liDescriptor.size()-1;
			
			for (int j = 0; j < liDescriptor.size(); j++) {
				nEntries += liDescriptor.get(j).length;
			}
		}
		
		int size = (nSEPDescriptors*2) + nSEPSphereDescriptors+nEntries;
		
		byte [] arrLiLi =new byte[size];
		
		int indexArrLiLi = 0;
		
		for (int i = 0; i < liliDescriptor.size(); i++) {
			
			List<byte[]> liDescriptor = liliDescriptor.get(i);
			
			for (int j = 0; j < liDescriptor.size(); j++) {
				
				byte [] arr = liDescriptor.get(j);
				
				System.arraycopy(arr, 0, arrLiLi, indexArrLiLi, arr.length);
								
				indexArrLiLi += arr.length;
				
				if(j<liDescriptor.size()-1){
					arrLiLi[indexArrLiLi]=SEP_DESCRIPTOR;
					indexArrLiLi++;
				}
			}
			
			if(i < liliDescriptor.size()-1){
				arrLiLi[indexArrLiLi]=SEP_DESCRIPTOR;
				indexArrLiLi++;
				arrLiLi[indexArrLiLi]=SEP_DESCRIPTOR;
				indexArrLiLi++;
			}
		}
		
		byte [] arr = descriptorEncoder.encodeCounts(arrLiLi);
		
		return new String(arr);
	}
	
	public double[] getWeighingScheme() {
		return centeredSkeletonSpheresComparator.getWeighingScheme();
	}

	public static DescriptorHandlerCenteredSkeletonFragments getDefaultInstance() {
		
		synchronized (DescriptorHandlerCenteredSkeletonFragments.class) {
			
			if (INSTANCE == null) {
				
				INSTANCE = new DescriptorHandlerCenteredSkeletonFragments();
			}
			
		}
		
		return INSTANCE;
	}
	
	public DescriptorHandler getDeepCopy() {
		return new DescriptorHandlerCenteredSkeletonFragments(this);
	}
	
	public static DescriptorHandlerCenteredSkeletonFragments createDescriptorHandler(int nMaxFragmentSize, int depth, String idcodeFragment) throws Exception {
		
		CenteredSkeletonSpheresComparator centSkelFragsComparator = new CenteredSkeletonSpheresComparator(depth);
		
		DescriptorHandlerCenteredSkeletonFragments dhCenteredSkeletonSpheres = new DescriptorHandlerCenteredSkeletonFragments(nMaxFragmentSize, depth, centSkelFragsComparator);
		
		IDCodeParser idCodeParser = new IDCodeParser(false);
		
		StereoMolecule frag = idCodeParser.getCompactMolecule(idcodeFragment);
		
		dhCenteredSkeletonSpheres.setFrag(frag);
		
		return dhCenteredSkeletonSpheres;

	}


}
