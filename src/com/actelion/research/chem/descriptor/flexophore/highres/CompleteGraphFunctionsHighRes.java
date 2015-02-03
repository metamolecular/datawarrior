package com.actelion.research.chem.descriptor.flexophore.highres;

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.ConformationSampler;
import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.chem.descriptor.flexophore.generator.CGMult;
import com.actelion.research.chem.descriptor.flexophore.generator.CompleteGraphFunctions;
import com.actelion.research.chem.redgraph.FFMolSummarizerInteractTable;
import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;
import com.actelion.research.forcefield.mm2.MM2Parameters;

/**
 * 
 * CompleteGraphFunctionsHighRes
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 8 Dec 2010 MvK: Start implementation
 */
public class CompleteGraphFunctionsHighRes {
	
	private static final FFMolSummarizerInteractTable SUMMARIZER_INTERACT_TABLE = FFMolSummarizerInteractTable.getInstance();
	
	private static boolean VERBOSE = false;
	
	

	public static CGMult createMultipleTSFromInteractionTable(StereoMolecule mol, ProgressListener progessListener) {

		int numConf = CompleteGraphFunctions.getConformations2Generate(mol,DescriptorHandlerFlexophore.NUM_CONFORMATIONS);
		
		if(numConf>1)
			numConf = DescriptorHandlerFlexophore.NUM_CONFORMATIONS;
		
		StereoMolecule ster = mol.getCompactCopy();
		
		ConformationSampler confsampler = new ConformationSampler(ster);
		
		confsampler.setVerbose(VERBOSE);

		confsampler.generateConformer(0);
		
		FFMolecule ffConformer = new FFMolecule(ster);
		
		MM2Parameters.getInstance().setAtomClassesForMolecule(ffConformer);
		
		ClassInteractionStatistics.getInstance().setClassIdsForMolecule(ffConformer);

		SUMMARIZER_INTERACT_TABLE.summarizeFineGranularity(ffConformer, false);
		
		CGMult cgMult = new CGMult(CompleteGraphFunctions.convert(ffConformer), ffConformer);

		if(progessListener!=null){
			progessListener.startProgress("", 0, numConf);
		}

		for (int i = 1; i < numConf; i++) {
			confsampler.generateConformer(0);
			
			ffConformer = new FFMolecule(ster);
			
			MM2Parameters.getInstance().setAtomClassesForMolecule(ffConformer);
			
			ClassInteractionStatistics.getInstance().setClassIdsForMolecule(ffConformer);

			SUMMARIZER_INTERACT_TABLE.summarizeFineGranularity(ffConformer, false);
			
			try {
				cgMult.add(CompleteGraphFunctions.convert(ffConformer));
			} catch (Exception ex) {
				ex.printStackTrace();
				Canonizer can = new Canonizer(ster);
				String id = can.getIDCode();

				String e = "cgMult\n" + cgMult.toString();
				e += "ffConformer\n" + ffConformer.toString();
				e += "mol\n" + ster.toString();
				e += "mol idcode\n" + id;
				System.out.println(e + "\n\n");
			}
			
			if(progessListener!=null) {
				progessListener.updateProgress(i);
			}
		}
		
		if(progessListener!=null) 
			progessListener.stopProgress();
		
		return cgMult;
	}
	

}
