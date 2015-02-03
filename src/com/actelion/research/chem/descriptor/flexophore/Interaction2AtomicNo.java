package com.actelion.research.chem.descriptor.flexophore;

import java.util.HashSet;

import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;

/**
 * 
 * Interaction2AtomicNo
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2006 MvK: Start implementation
 * 2008 Jan 16 isAromatic added
 */
public class Interaction2AtomicNo {
	
	
	private static Interaction2AtomicNo INSTANCE;
	
	private static final String PATTERN_AROMATIC = ".*_.*\\{.*\\}.*";
	
	
	private static final String [] ARR_CARBON = {"CALKANE","CALKENE","CALKYNE","CCARBONYL","CCYCLOPROPANE","CCYCLOPROPENE"};
	
	private static final String [] ARR_NITROGEN = {"NAMIDE","NAMINE","NAMMONIUM","NCONNAROMATIC","NGUANIDINE","NIMINE","NIMMONIUM","NNITRILE","NOXAZOLE","NPYRIDINE","NPYRIMIDINE","NPYRROLE","NSULFONAMIDE"};
	
	private static final String [] ARR_OXYGEN = {"OALCOHOL","OAMIDE","OCARBONYL","OCARBOXYL","OENOL","OETHER","OFURAN","OOXO","OPHOSPHATE","OWATER"};
	
	private static final String [] ARR_PHOSPHORUS = {"PPHOSPHATE"};
	
	private static final String [] ARR_SULFUR = {"SSULFONE","STHIOETHER","STHIOL","STHIOPHENE"};
	
	private static final String [] ARR_HALOGEN = {"CL","F","I"};
	
	private static final String [] ARR_HALOGEN_CL = {"CL"};
	
	private static final String [] ARR_HALOGEN_F = {"F"};
	
	private static final String [] ARR_HALOGEN_I = {"I"};
	
	
	private int [] mArrAtomicNo;
	
	private boolean [] mArrIsAromatic;
	
	private boolean [] mArrIsCarbonInteractionType;
	
	private boolean [] mArrIsNitrogenInteractionType;
	
	private boolean [] mArrIsOxygenInteractionType;
	
	private boolean [] mArrIsPhosphorusInteractionType;
	
	private boolean [] mArrIsSulfurInteractionType;
	
	private boolean [] mArrIsHalogenInteractionType;
	
	private boolean [] mArrIsClInteractionType;
	
	private boolean [] mArrIsFInteractionType;
	
	private boolean [] mArrIsIInteractionType;
	
	
	
	private Interaction2AtomicNo(){
		init();
	}
	
	private void init() {
		ClassInteractionStatistics clstat = ClassInteractionStatistics.getInstance();
		
		mArrAtomicNo = new int [clstat.getNClasses()];
		
		mArrIsAromatic = new boolean [clstat.getNClasses()];
		
		mArrIsCarbonInteractionType = getAtomicNumberFroClassInteraction(ARR_CARBON);
		
		mArrIsNitrogenInteractionType = getAtomicNumberFroClassInteraction(ARR_NITROGEN);
		
		mArrIsOxygenInteractionType = getAtomicNumberFroClassInteraction(ARR_OXYGEN);
		
		mArrIsPhosphorusInteractionType = getAtomicNumberFroClassInteraction(ARR_PHOSPHORUS);
				
		mArrIsSulfurInteractionType = getAtomicNumberFroClassInteraction(ARR_SULFUR);
		
		mArrIsHalogenInteractionType = getAtomicNumberFroClassInteraction(ARR_HALOGEN);
		
		mArrIsClInteractionType = getAtomicNumberFroClassInteraction(ARR_HALOGEN_CL);
		
		mArrIsFInteractionType = getAtomicNumberFroClassInteraction(ARR_HALOGEN_F);
		
		mArrIsIInteractionType = getAtomicNumberFroClassInteraction(ARR_HALOGEN_I);
						
		for (int i = 0; i < clstat.getNClasses(); i++) {
			
			mArrAtomicNo[i]=getAtomicNo(i);
			
			String s = clstat.getDescription(i);
			
			mArrIsAromatic[i]=isAromatic(s);
		}
	}
	
	public boolean isCarbonInteraction(int interactionType){
		
		return mArrIsCarbonInteractionType[interactionType];
		
	}
	
	public boolean isNitrogenInteraction(int interactionType){
		
		return mArrIsNitrogenInteractionType[interactionType];
		
	}
	
	public boolean isOxygenInteraction(int interactionType){
		
		return mArrIsOxygenInteractionType[interactionType];
		
	}
	
	public boolean isPhosphorusInteraction(int interactionType){
		
		return mArrIsPhosphorusInteractionType[interactionType];
		
	}
	
	public boolean isSulfurInteraction(int interactionType){
		
		return mArrIsSulfurInteractionType[interactionType];
		
	}

	public boolean isHalogenInteraction(int interactionType){
		
		return mArrIsHalogenInteractionType[interactionType];
		
	}
	
	public boolean isClInteraction(int interactionType){
		
		return mArrIsClInteractionType[interactionType];
		
	}

	public boolean isFInteraction(int interactionType){
		
		return mArrIsFInteractionType[interactionType];
		
	}

	public boolean isIInteraction(int interactionType){
		
		return mArrIsIInteractionType[interactionType];
		
	}
	
	private int getAtomicNo(int interactionType){
		
		int at = -1;
		
		if(isCarbonInteraction(interactionType)){
			at = 6;
		} else if(isClInteraction(interactionType)){
			at = 17;
		} else if(isFInteraction(interactionType)){
			at = 9;
		} else if(isIInteraction(interactionType)){
			at = 53;
		} else if(isNitrogenInteraction(interactionType)){
			at = 7;
		} else if(isOxygenInteraction(interactionType)){
			at = 8;
		} else if(isPhosphorusInteraction(interactionType)){
			at = 15;
		} else if(isSulfurInteraction(interactionType)){
			at = 16;
		} 
				
		return at;
		
	}
	
	private static boolean isAromatic(String s){
		boolean bArom=false;
		
		if(s.matches(PATTERN_AROMATIC)){
			bArom=true;
		}
		
		return bArom;
	}

	public int getAtomicNumber(int interactionid){
		return mArrAtomicNo[interactionid];
	}
	
	public boolean isAromatic(int interactionid){
		return mArrIsAromatic[interactionid];
	}
	
	private static boolean [] getAtomicNumberFroClassInteraction(String [] arrNameInteraction){
		
		ClassInteractionStatistics clstat = ClassInteractionStatistics.getInstance();
		
		boolean [] arr = new boolean [clstat.getNClasses()];
		
		HashSet<String> hs = new HashSet<String>();
		for (int i = 0; i < arrNameInteraction.length; i++) {
			hs.add(arrNameInteraction[i]);
		}
		
		
		for (int i = 0; i < clstat.getNClasses(); i++) {
			
			String name = getNameFromInteractionDescription(i);
			
			if(hs.contains(name)){
				arr[i]=true;
			}
		}
		
		return arr;
	}
	
	private static String getNameFromInteractionDescription(int interactionType){
		
		String descr =  ClassInteractionStatistics.getInstance().getDescription(interactionType);
		
		int start = descr.indexOf("*");
		
		String name = "";
		
		if(start>-1){
			start++;
			
			int end = descr.indexOf("_");
			
			if(end>-1) {
				name = descr.substring(start, end);
			} else {
				name = descr.substring(start);
			}
		}
		
		return name;
	}
	
	public static Interaction2AtomicNo getInstance(){
		if(INSTANCE==null){
			INSTANCE = new Interaction2AtomicNo();
		}
		
		return INSTANCE;
	}
	


}
