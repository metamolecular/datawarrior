package com.actelion.research.chem.descriptor.flexophore.generator;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.calc.ArrayUtilsCalc;
import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.FFMoleculeFunctions;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.calculator.AdvancedTools;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.chem.conf.ConformationSampler;
import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistViz;
import com.actelion.research.chem.descriptor.flexophore.PPNodeViz;
import com.actelion.research.chem.descriptor.flexophore.UnparametrizedAtomTypeException;
import com.actelion.research.chem.redgraph.FFMolSummarizerInteractTable;
import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;
import com.actelion.research.forcefield.mm2.MM2Parameters;

/**
 * 
 * CompleteGraphFunctions
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2004 MvK: Start implementation
 */
public class CompleteGraphFunctions {
	
	private static boolean VERBOSE = false;
	
	
	public static int getConformations2Generate(StereoMolecule mol, int maxnumconf) {
		int conf = 0;

		FFMolecule ff = new FFMolecule(mol);

		int rot = StructureCalculator.getNRotatableBonds(ff);

		double dConf = Math.pow(3,rot);
		
		if(dConf > maxnumconf){
			conf = maxnumconf;
		} else {
			conf = (int)dConf;
		}
		
		return conf;
	}
	
	
	/**
	 * The first field in the conformation histogram array is set to the number of conformations.
	 * @param mol
	 * @return
	 * @throws UnparametrizedAtomTypeException
	 * 23.06.2008 MvK
	 */
	public static MolDistHistViz createNoConfFromInteractionTable(StereoMolecule mol) throws UnparametrizedAtomTypeException {
		
		CGMult cgMult = null;

		FFMolecule ffConformer = new FFMolecule(mol);

		MM2Parameters.getInstance().setAtomClassesForMolecule(ffConformer);
		ClassInteractionStatistics.getInstance().setClassIdsForMolecule(ffConformer);

		FFMolSummarizerInteractTable.getInstance().summarize(ffConformer, false);
		
		
		cgMult = new CGMult(convert(ffConformer), ffConformer);
		
		MolDistHistViz mdh = cgMult.getMolDistHistViz();
			
		for (int i = 0; i < mdh.getNumPPNodes(); i++) {
			for (int j = i+1; j < mdh.getNumPPNodes(); j++) {
				byte [] arrHist = mdh.getDistHist(i,j);
				for (int k = 0; k < arrHist.length; k++) {
					arrHist[k]=0;
				}
				arrHist[0]=(byte)DescriptorHandlerFlexophore.NUM_CONFORMATIONS;
			}
		}
		return mdh;
	}
	
	
	
	public static CGMult createMultipleTSFromInteractionTable(StereoMolecule mol) throws UnparametrizedAtomTypeException {
		
		int numConf = getConformations2Generate(mol, DescriptorHandlerFlexophore.NUM_CONFORMATIONS);
		
		CGMult cg = createMultipleTSFromInteractionTable(mol, numConf);
		return cg;
	}

	/**
	 * Creates as many threads as conformations.
	 * @param mol
	 * @param progessListener
	 * @return
	 * @throws UnparametrizedAtomTypeException
	 */
	public static CGMult createMultipleTSFromInteractionTableSMT(StereoMolecule mol, ProgressListener progessListener)
	throws UnparametrizedAtomTypeException {

		int numConf = getConformations2Generate(mol,DescriptorHandlerFlexophore.NUM_CONFORMATIONS);
	
		CGMult cg = createMultipleTSFromInteractionTableSMT(mol, numConf, progessListener);
		
		return cg;
	}
	
	
//	public static CGMult createMultipleTSFromInteractionTable(StereoMolecule mol, boolean generateConformersSMT, int numConformations, boolean preserveStructure, ProgressListener progessListener) {
//		
//		try {Thread.sleep(5000);} catch (InterruptedException e) {e.printStackTrace();}
//		
//		return null;
//	}

	
	
	
	/**
	 * 
	 * Creates multiple conformations with TS conformation sampler.
	 * The atom definition for the returned CGMult instance from the Joel Freys class statistics and MM2 types.
	 * Uses only one thread for conformation generation.
	 * @param mol
	 * @param generateConformersSMT if true the conformations are generated in parallel.
	 * @param numConformations
	 * @param preserveStructure
	 * @param progessListener
	 * @return
	 */
	public static CGMult createMultipleTSFromInteractionTable(StereoMolecule mol, int numConformations) {

		CGMult cgMult = null;

		// New instance of StereoMolecule because the 3D coordinates change.
		// MvK 15.03.2007
		StereoMolecule ster = mol.getCompactCopy();
		
		ster.ensureHelperArrays(Molecule.cHelperRings);
		
		facultativeCoordinateInvention(ster);

		ConformationSampler confsampler = new ConformationSampler(ster);
		
		confsampler.setVerbose(false);
		
		if(VERBOSE)
			System.out.println("CompleteGraphFunctions start conformation sampling.");


		for (int i = 0; i < numConformations; i++) {
			
			if(VERBOSE)
				System.out.println("CompleteGraphFunctions create conformation " + i);

			
			confsampler.generateConformer(0);

			StereoMolecule conformer = confsampler.getConformer();
			
			FFMolecule ffConformer = new FFMolecule(conformer);

			MM2Parameters.getInstance().setAtomClassesForMolecule(ffConformer);
			ClassInteractionStatistics.getInstance().setClassIdsForMolecule(ffConformer);
			
			if(VERBOSE) {
				for (int j = 0; j < ffConformer.getAllAtoms(); j++) {
					int interactionType = ffConformer.getAtomInteractionClass(j);
					System.out.println("Atomic no " + ffConformer.getAtomicNo(j) + " interaction type " + interactionType);
				}

				
				System.out.println("CompleteGraphFunctions summarize conformation " + i);
			}
			
			
			FFMolSummarizerInteractTable.getInstance().summarize(ffConformer, false);
			
			try {
				
				if (cgMult == null)
					cgMult = new CGMult(convert(ffConformer), ffConformer);
				else {
					cgMult.add(convert(ffConformer));
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
				Canonizer can = new Canonizer(ster);
				String id = can.getIDCode();
				System.out.println(id);
				System.out.println("\n\n");
			}
			
			
			
			if(VERBOSE)
				System.out.println("CompleteGraphFunctions finished conformation " + i);
		}
				
		return cgMult;
	}
	
	/**
	 * Creates as many threads as <code>numConformations</code>.
	 * @param mol
	 * @param numConformations
	 * @param progessListener
	 * @return
	 */
	public static CGMult createMultipleTSFromInteractionTableSMT(StereoMolecule mol, int numConformations, ProgressListener progessListener) {

		CGMult cgMult = null;

		// New instance of StereoMolecule because the 3D coordinates change.
		// MvK 15.03.2007
		StereoMolecule ster = mol.getCompactCopy();
		
		ster.ensureHelperArrays(Molecule.cHelperRings);
		
		
		facultativeCoordinateInvention(ster);
		
			
		// We calculate the first 3D structure with J.Freyss force field. The 1. structure is for
		// visualization and is smoother than with the self organising structure calculator from TS. 
		List<FFMolecule> li = AdvancedTools.convertMolecule2DTo3D(ster);
		
		FFMolecule ffStruc = li.get(0);
		
		FFMoleculeFunctions.removeHydrogensAndElectronPairs(ffStruc);
		
		MM2Parameters.getInstance().setAtomClassesForMolecule(ffStruc);
		
		ClassInteractionStatistics.getInstance().setClassIdsForMolecule(ffStruc);

		ConformationSampler confsampler = new ConformationSampler(ffStruc.toStereoMolecule());
		
		FFMolSummarizerInteractTable.getInstance().summarize(ffStruc, false);
		
		cgMult = new CGMult(convert(ffStruc), ffStruc);
			
		
		
		if(progessListener!=null){
			progessListener.startProgress("", 0, numConformations);
		}
		
		confsampler.setVerbose(false);

		if(VERBOSE)
			System.out.println("CompleteGraphFunctions start conformation sampling.");

		
		confsampler.generateConformersSMT(numConformations);

		for (int i = 0; i < numConformations; i++) {
			
			if(VERBOSE)
				System.out.println("CompleteGraphFunctions create conformation " + i);

			StereoMolecule conformer = confsampler.getConformer(i);
			
			FFMolecule ffConformer = new FFMolecule(conformer);

			MM2Parameters.getInstance().setAtomClassesForMolecule(ffConformer);
			ClassInteractionStatistics.getInstance().setClassIdsForMolecule(ffConformer);
			
			if(VERBOSE) {
				for (int j = 0; j < ffConformer.getAllAtoms(); j++) {
					int interactionType = ffConformer.getAtomInteractionClass(j);
					System.out.println("Atomic no " + ffConformer.getAtomicNo(j) + " interaction type " + interactionType);
				}

				
				System.out.println("CompleteGraphFunctions summarize conformation " + i);
			}
			
			
			FFMolSummarizerInteractTable.getInstance().summarize(ffConformer, false);
			
			try {
				
				if (cgMult == null)
					cgMult = new CGMult(convert(ffConformer), ffConformer);
				else {
					cgMult.add(convert(ffConformer));
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
				Canonizer can = new Canonizer(ster);
				String id = can.getIDCode();
				System.out.println(id);
				System.out.println("\n\n");
			}
			
			
			if(progessListener!=null) {
				progessListener.updateProgress(i);
			}
			
			if(VERBOSE)
				System.out.println("CompleteGraphFunctions finished conformation " + i);
		}
		
		if(progessListener!=null) 
			progessListener.stopProgress();
		
		return cgMult;
	}
	
	private static void facultativeCoordinateInvention(StereoMolecule ster){
		
		//
		// Invent 2D coordinates if not given.
		//
		List<Coordinates> liCoord = new ArrayList<Coordinates>();
		
		double avrX = 0;
		double avrY = 0;
		double avrZ = 0;
		for (int i = 0; i < ster.getAtoms(); i++) {
			Coordinates c = new Coordinates();
			
			c.x = ster.getAtomX(i);
			c.y = ster.getAtomY(i);
			c.z = ster.getAtomZ(i);
			
			avrX += c.x;
			avrY += c.y;
			avrZ += c.z;
			
			liCoord.add(c);
			
		}
		
		avrX /= liCoord.size();
		avrY /= liCoord.size();
		avrZ /= liCoord.size();
				
		double varX = 0; 
		double varY = 0; 
		double varZ = 0; 
		for (Coordinates c : liCoord) {
			varX += (c.x - avrX)*(c.x - avrX);
			varY += (c.y - avrY)*(c.y - avrY);
			varZ += (c.z - avrZ)*(c.z - avrZ);
		}
		
		if((varX==0) && (varY == 0) && (varZ == 0)){
			
			CoordinateInventor ci = new CoordinateInventor();
			
			ci.invent(ster);
			
			
			if(VERBOSE)
				System.out.println("CompleteGraphFunctions createMultipleTSFromInteractionTable 2D coordinates calculated.");
			
		}
	}
	
	public static ArrayList<MolDistHistViz> createListOfOneConfFlexophores(StereoMolecule mol, boolean generateConformersSMT, int numConformations, ProgressListener progessListener) {

		ArrayList<MolDistHistViz> liMolDistHistViz = new ArrayList<MolDistHistViz>();
		
		StereoMolecule ster = (StereoMolecule)mol.getCompactCopy();
		
		ConformationSampler confsampler = new ConformationSampler(ster);
		
		if(progessListener!=null){
			progessListener.startProgress("", 0, numConformations);
		}
		
	
		if (generateConformersSMT)
			confsampler.generateConformersSMT(numConformations);

		for (int i = 0; i < numConformations; i++) {
			
			
			if (!generateConformersSMT)
				confsampler.generateConformer(0);

			StereoMolecule conformer = generateConformersSMT ? confsampler.getConformer(i) : confsampler.getConformer();
						
			double [] arrFilter = {1};
			
			MolDistHistViz mdhv = createFromOneConf(conformer, DescriptorHandlerFlexophore.NUM_CONFORMATIONS, arrFilter);
			
			liMolDistHistViz.add(mdhv);
			
			if(progessListener!=null) {
				progessListener.updateProgress(i);
			}
			
		}
		
		
		if(progessListener!=null) 
			progessListener.stopProgress();
		
		
		return liMolDistHistViz;
	}
	
	public static MolDistHistViz createFromOneConf(StereoMolecule mol, int conformations, double [] arrFilter) {

		FFMolecule ffMol = new FFMolecule(mol);
		
		if (!FFMoleculeFunctions.isParametrized(ffMol)) {
			throw (new RuntimeException("Not parametrized atom type"));
		}
		

		MM2Parameters.getInstance().setAtomClassesForMolecule(ffMol);
		ClassInteractionStatistics.getInstance().setClassIdsForMolecule(ffMol);

		FFMolSummarizerInteractTable.getInstance().summarize(ffMol);
		
		CompleteGraph cg = convert(ffMol);
		
		CGMult cgMult = new CGMult(cg, ffMol);

		MolDistHistViz mdhv = cgMult.getMolDistHistViz();
		
		for (int i = 0; i < mdhv.getNumPPNodes(); i++) {
			for (int j = i+1; j < mdhv.getNumPPNodes(); j++) {
				byte [] arrHist = mdhv.getDistHist(i,j);
				boolean bF = false;
				for (int k = 0; k < arrHist.length; k++) {
					if(arrHist[k]== conformations){
						if(bF){
							throw new RuntimeException("There should be only one conformation");
						}
						arrHist[k] = (byte)conformations;
						bF = true;
					} 
				}
				
				if(!bF){
					throw new RuntimeException("Error in histogram " + ArrayUtilsCalc.toString(arrHist));
				}

				double [] arrD = ArrayUtilsCalc.filter(arrHist, arrFilter);
				for (int k = 0; k < arrD.length; k++) {
					arrHist[k]=(byte)(arrD[k]+0.5);
				}
				mdhv.setDistHist(i, j, arrHist);
			}
		}
		
		mdhv.realize();
		
		return mdhv;
	}

	/**
	 * Generates a complete graph from a summarised molecule. 
	 * The atoms which are flagged by FFMoleculeFunctions.FLAG_CENTER_ATOM will be used for the summary. 
	 * @param ff
	 * @return
	 */
	public static CompleteGraph convert(FFMolecule ff) {
		CompleteGraph cg = new CompleteGraph();

		for (int i = 0; i < ff.getAllAtoms(); i++) {
			
			if(ff.isAtomFlag(i, FFMoleculeFunctions.FLAG_CENTER_ATOM)) {
				
				if(ff.getAllConnAtoms(i)>0) {
					ExtendedMolecule ext = ff.toExtendedMolecule();
					Canonizer can = new Canonizer(ext);
					String e = "Center atom " + i + " in molecule " + can.getIDCode() + " has " + ff.getAllConnAtoms(i) + " neighbors.";
					throw new RuntimeException(e);
				}
				
				Coordinates coord = new Coordinates(ff.getAtomX(i), ff.getAtomY(i), ff.getAtomZ(i));
				
				// The index of the original atom is taken from the group
				String sIndOriginalAtom = ff.getAtomChainId(i);
								
				int interactionType = ff.getAtomInteractionClass(i);
				
				// System.out.println("interactionType " + interactionType);
				
				if(interactionType > -1) {
					PPNodeViz node = new PPNodeViz(coord, interactionType, Integer.parseInt(sIndOriginalAtom));
					
					cg.addNode(node);
				}
			}
		}
			
		cg.calculateDistances();

		return cg;

	}
}
