package com.actelion.research.chem.redgraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.actelion.research.calc.ArrayUtilsCalc;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.FFMoleculeFunctions;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.flexophore.IndexCoordinates;
import com.actelion.research.util.datamodel.IntArray;

/**
 * 
 * 
 * FFMolSummarizerInteractTable
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2005 MvK: Start implementation
 * Oct 23, 2012 MvK: added 
 */
public class FFMolSummarizerInteractTable implements IMoleculeSummarizer {

	private static FFMolSummarizerInteractTable INSTANCE;
	
	
	public static final String TAG_ORIGINAL_COORD = "OriginalCoordinates";
	
	//
	// IDCodes for replacing fragments
	//
	
	
	// CF3
	public static final String IDCODE_FRAG_REPL_CF3 = "qC`XBPTaiAIj`H x>xiS~";
	public static final String IDCODE_FRAG_REPL_CF3_WITH_CH3 = "`H@P";
	public static final int [][] ARR_MAP_REPL_CF3 = {{0,0}};  
	
	//
	// IDCodes and indices for deleting dead atoms
	//
	
	// Ester
	public static final String IDCODE_FRAG_ESTER = "qCa@AIJtA@ S~xix>";
	
	// Ester, ether O
	public static final int [] INDEX_ATS2DEL_ESTER = {2};

	// Ether Aromatic-O-Aromatic
	public static final String IDCODE_FRAG_AR_O_AR = "eMHBN`zZZsH";
	
	public static final int [] INDEX_ATS2DEL_FRAG_AR_O_AR_O = {0};

	// Amide, N-Amide
	public static final String IDCODE_FRAG_AMIDE = "sJY@DDefhAc@g@H@ S~xix>S~";
	
	// Amide, Amine N
	public static final int [] INDEX_ATS2DEL_AMIDE = {2};
	
	// Phenyl-O-C-C
	public static final String IDCODE_FRAG_PHENYL_O_C_C = "qCb@AJZ`Nhh ~l(l~:";
	// Phenyl-O-C-C, O
	public static final int [] INDEX_ATS2DEL_PHENYL_O_C_C = {0};

	// Sulfoxide O=S=O
	public static final String IDCODE_FRAG_SULFOXIDE = "QMFI@bMPP (l~l";
	// Sulfoxide O=S=O, S
	public static final int [] INDEX_ATS2DEL_SULFOXIDE = {0};

	// Anilin Ph-N(C)C
	public static final String IDCODE_FRAG_ANILIN = "qCp@BOTAuI@ .ixiS(";
	// Anilin Ph-N(C)C, N
	public static final int [] INDEX_ATS2DEL_ANILIN = {0};

	
	// Fluor
	public static final String IDCODE_FRAG_F = "fHd`A@";
	public static final int [] INDEX_ATS2DEL_F = {0};
	
	// Chlor
	public static final String IDCODE_FRAG_Cl = "fHdPA@";
	public static final int [] INDEX_ATS2DEL_Cl = {0};
	
	// Bromine
	public static final String IDCODE_FRAG_Br = "fHfHA@";
	public static final int [] INDEX_ATS2DEL_Br = {0};

	// Iodine
	public static final String IDCODE_FRAG_I = "fHeXA@";
	public static final int [] INDEX_ATS2DEL_I = {0};

	//
	// IDCodes and index for calculation of centers
	//
	
	// Sulfoxide
	public static final String IDCODE_FRAG_CENTER_SULFOXIDE = "QMFI@bMPP S(.i";
	// Interaction type from =O
	public static final int INDEX_CENTER_SULFOXIDE = 1;

	public static final String IDCODE_FRAG_CENTER_SULFONAMIDE = "qCqPZHAD]XB";
	
	/**
	 * Added 23 Oct 2012 MvK.
	 */
	public static final String IDCODE_FRAG_CENTER_SULFAMIDE = "gJThLX`DQztA@";

	// Imine
	public static final String IDCODE_FRAG_CENTER_IMINE = "qCh@AILtA@ .>.iS(";
	// Interaction type from =O
	public static final int INDEX_CENTER_IMINE = 0;

	// Guanidine and urea
	// public static final String IDCODE_FRAG_CENTER_GUANIDINE = "qCx@AYIf`H S~.ixi";
	public static final String IDCODE_FRAG_CENTER_GUANIDINE_UREA = "gCl@@ldsPFRSyxpIpB@ S~.ixi";
	
	// Ester
	public static final String IDCODE_FRAG_CENTER_ESTER = "gC``@dfZ@rb_tS@ S~xi.i";
	
	// Carbonic acid
	public static final String IDCODE_FRAG_CENTER_CARBONICACID = "qCa@AILtA}EP S~xi.i";

	// Phosphoric acid ?P(=O)(O)?
	public static final String IDCODE_FRAG_CENTER_PACID_1 = "sJPhHaxQ{T@vVH";

	// Amide
	// public static final String IDCODE_FRAG_CENTER_AMIDE = "eMhDRVB";
	public static final String IDCODE_FRAG_CENTER_AMIDE = "eMhDRVCJW@";
	
	// Acetal ring C1OCOC1?
	public static final String IDCODE_FRAG_CENTER_ACETAL_RING = "sKP`Adi\\Zj@ZsB";
	
	// Ether or sec amine?
	public static final String IDCODE_FRAG_CENTER_ETHER_SECAMINE = "eM@HzCjfuQaN@P@ ~l(l";
	
	public static final String LEADING_SPACE_DESCRIPTION = "     ";

	public static final String SEP_ATOM_INDEX = ",";
	
	public static final int FLAG_AROMATIC_ATOM = 1<<5;

	public static final int FFMOLECULE_DUMMY_VAL = -1;
	
	
	
	private static final int MAX_RING_SIZE_SINGLE_PPPOINT = 8;
	
	
	public static FFMolSummarizerInteractTable getInstance(){
		
		if(INSTANCE==null){
			INSTANCE=new FFMolSummarizerInteractTable();
		}
		
		return INSTANCE;
	}

	private static void calcCenterNoCarbon(FFMolecule ffMol, String idcodeCenter, boolean skipIfAtomInRing) {
		
		List<Integer> liAtomicNosExcluded = new ArrayList<Integer>();
		liAtomicNosExcluded.add(new Integer(6));
		calcCenter(ffMol, idcodeCenter, liAtomicNosExcluded, skipIfAtomInRing);
		
	}
	/**
	 * 
	 * @param ffMol
	 * @param idcodeCenter
	 * @param liAtomicNosExcluded
	 * @param skipIfAtomInRing true: If the atom is in a ring it will not be considered
	 */
	private static void calcCenter(FFMolecule ffMol, String idcodeCenter, List<Integer> liAtomicNosExcluded, boolean skipIfAtomInRing) {
		
		StereoMolecule mol = ffMol.toStereoMolecule();
		
		mol.ensureHelperArrays(Molecule.cHelperRings);

		
		IDCodeParser parser = new IDCodeParser(true);
		
		StereoMolecule query = parser.getCompactMolecule(idcodeCenter);
		
		query.ensureHelperArrays(Molecule.cHelperRings);
		
		query.setFragment(true);
		
		
		SSSearcher sss = new SSSearcher();
		sss.setMol(query,mol);
		
		int numFrags = sss.findFragmentInMolecule(SSSearcher.cCountModeOverlapping, SSSearcher.cMatchAromDBondToDelocalized);
		
		List<int[]> vecMatchList = null;
		if(numFrags > 0)  {
			vecMatchList = sss.getMatchList();
		} else {
			return;
		}
		
		ArrayUtilsCalc.removeDoubletsIntOrderIndepend(vecMatchList);
		
		// System.out.println("Found: " + liAtomLists.size());

		
		// Extract substructure from molecule
		
		for (Iterator<int []> iter = vecMatchList.iterator(); iter.hasNext();) {
			int [] arrAtomList = iter.next();
			
			Coordinates coord = FFMoleculeFunctions.getCenterGravity(ffMol, arrAtomList);
			
			for (int at = 0; at < arrAtomList.length; at++) {
				
				// If the atom is in a ring it will not be considered
				if(ffMol.getRingSize(arrAtomList[at])>-1)
					continue;
				
				// If the atom was already considered it will not be considered again.
				if(ffMol.isAtomFlag(arrAtomList[at], FFMolecule.FLAG1) && skipIfAtomInRing)
						continue;
				
				// Atom type.
				int iInteractionType = ffMol.getAtomInteractionClass(arrAtomList[at]);

				// MM2 interaction type
				int iMM2Type = ffMol.getAtomMM2Class(arrAtomList[at]);

				int iAtomicNo = ffMol.getAtomicNo(arrAtomList[at]);
				
				ffMol.setAtomFlag(arrAtomList[at], FFMolecule.FLAG1, true);
				
				if(!liAtomicNosExcluded.contains(iAtomicNo)) {
					
					int indexOriginalAtom = arrAtomList[at];
					
					int index = ffMol.addAtom(iAtomicNo);
					ffMol.setAtomInteractionClass(index, iInteractionType);
					ffMol.setAtomMM2Class(index, iMM2Type);
					String sOrigIndex = Integer.toString(indexOriginalAtom);
					ffMol.setAtomChainId(index, sOrigIndex);
					ffMol.setCoordinates(index, coord);
					ffMol.setAtomFlag(index, FFMoleculeFunctions.FLAG_CENTER_ATOM, true);
					ffMol.setPPP(index, arrAtomList);
					
					IndexCoordinates indexCoordinates = new IndexCoordinates(index, indexOriginalAtom, ffMol.getCoordinates(indexOriginalAtom));
					
					ffMol.setAuxiliaryInfo(TAG_ORIGINAL_COORD, indexCoordinates);
					
				}
						
			}
		}
	}
	
	/**
	 * Remove from ff all atoms which corresponds to the selected atoms in the ExtendedMolecule.
	 * The corresponding atom indices have to be given in ifo, Molecule3D.INFO_ATOMGROUP.
	 * @param ff
	 * @param template
	 * @throws Exception
	 */
	public void removedSelected(FFMolecule ff, ExtendedMolecule template) {
		
		ff.setAllAtomFlag(FFMolecule.FLAG1, false);
		
		List<Integer> liInd2Remove = new ArrayList<Integer>();
		for (int i = 0; i < template.getAllAtoms(); i++) {
			if(template.isSelectedAtom(i)){
				liInd2Remove.add(new Integer(i));
			}
		}
		
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			String s = ff.getAtomChainId(i);
			if(s!=null) {
				int index = Integer.parseInt(s);
				
				if(liInd2Remove.contains(new Integer(index))) {
					ff.setAtomFlag(i, FFMolecule.FLAG1, true);
				}
			}
		}
		
		FFMoleculeFunctions.removeFlaggedAtoms(ff);
		
		
	}
	
	public void summarize(FFMolecule mol) {
		summarize(mol, true);
	}
	
	
	/**
	 * 
	 * @param mol the summarized atoms are added to mol and flagged with FLAG_CENTER_ATOM. 
	 *  For each summarized atom a new atom is added. This atom has no bonds.
	 *  The index of the summarized atom is written into group: setAtomGroup().
	 *  The summary is, that all atoms belonging to one pharmacophore point have identical coordinates.    
	 * @param bremoveflagged
	 */
	public void summarize(FFMolecule mol, boolean bremoveflagged) {

		boolean skipIfAtomInRing = true;
		
		mol.setAllAtomFlag(FFMolecule.FLAG1, false);
		mol.setAllAtomFlag(FFMoleculeFunctions.FLAG_CENTER_ATOM, false);
		
		FFMoleculeFunctions.removeHydrogensAndElectronPairs(mol);
		
		// F
		// FFMoleculeFunctions.removeElement(mol, 9);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_F, INDEX_ATS2DEL_F);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_Cl, INDEX_ATS2DEL_Cl);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_Br, INDEX_ATS2DEL_Br);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_I, INDEX_ATS2DEL_I);
		
		// Cl
		// replaceAtoms(mol, 17, 6);
		// Br
		// replaceAtoms(mol, 35, 6);
		// I
		// replaceAtoms(mol, 53, 6);

		List<Integer> liAtsExcluded = new ArrayList<Integer>();
		
		// liAtsExcluded.add(new Integer(16));
		
		calcCenter(mol, IDCODE_FRAG_CENTER_SULFAMIDE,liAtsExcluded, skipIfAtomInRing);
		
		calcCenter(mol, IDCODE_FRAG_CENTER_SULFONAMIDE,liAtsExcluded, skipIfAtomInRing);
		
		calcCenter(mol, IDCODE_FRAG_CENTER_SULFOXIDE, liAtsExcluded, skipIfAtomInRing);
		
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_GUANIDINE_UREA, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_IMINE, skipIfAtomInRing);
		
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_CARBONICACID, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_ESTER, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_PACID_1, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_AMIDE, skipIfAtomInRing);
		
		
		// First the terminal alkyl groups have to be computed, then all the other stuff.
		FFMoleculeFunctions.calcTerminalAlkylGroupsCenter(mol);
		
		calcAromatic(mol);
		
		calcRingCenter(mol);
		
		flagDeadAtoms(mol);
		
		if(bremoveflagged) {
			FFMoleculeFunctions.removeCarbon(mol);
			FFMoleculeFunctions.removeFlaggedAtoms(mol);
		} else {
			FFMoleculeFunctions.flagCarbon(mol);
			FFMoleculeFunctions.flagUnflaggedWithFlagCenter(mol);
		}
		
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if (!mol.isAtomFlag(i, FFMolecule.FLAG1))
				mol.setAtomFlag(i, FFMolecule.LIGAND, true);
		}
			
		
		// For pharmacophore atoms in the graph we have to add an additional atom 
		// which is not connected to any other atoms. So it will be visualized as a ball.
		int atoms = mol.getAllAtoms();
		for(int i=0; i < atoms; i++){
			if(mol.isAtomFlag(i, FFMoleculeFunctions.FLAG_CENTER_ATOM)) {
				if(mol.getConnAtoms(i)>0){
					addFlaggedAtomAndUnflagOrig(mol, i);
				}
			}
		}
	}
	
	/**
	 * In contrary to <code>summarize(FFMolecule mol, boolean bremoveflagged)</code> hetero atoms in rings are treated
	 * separately from the ring and not added to the ring center. 
	 * Only aliphatic ring atoms are summarized into a ring center. 
	 * @param mol
	 * @param bremoveflagged
	 */
	public void summarizeFineGranularity(FFMolecule mol, boolean bremoveflagged) {
		
		boolean skipIfAtomInRing = false;

		mol.setAllAtomFlag(FFMolecule.FLAG1, false);
		mol.setAllAtomFlag(FFMoleculeFunctions.FLAG_CENTER_ATOM, false);
		
		FFMoleculeFunctions.removeHydrogensAndElectronPairs(mol);
		
		// F
		// FFMoleculeFunctions.removeElement(mol, 9);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_F, INDEX_ATS2DEL_F);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_Cl, INDEX_ATS2DEL_Cl);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_Br, INDEX_ATS2DEL_Br);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_I, INDEX_ATS2DEL_I);
		
		// Cl
		// replaceAtoms(mol, 17, 6);
		// Br
		// replaceAtoms(mol, 35, 6);
		// I
		// replaceAtoms(mol, 53, 6);

		List<Integer> liAtsExcluded = new ArrayList<Integer>();
		liAtsExcluded.add(new Integer(16));
		calcCenter(mol, IDCODE_FRAG_CENTER_SULFAMIDE,liAtsExcluded, skipIfAtomInRing);
		
		calcCenter(mol, IDCODE_FRAG_CENTER_SULFONAMIDE, liAtsExcluded, skipIfAtomInRing);
		
		calcCenter(mol, IDCODE_FRAG_CENTER_SULFOXIDE, liAtsExcluded, skipIfAtomInRing);
		
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_GUANIDINE_UREA, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_IMINE, skipIfAtomInRing);
		
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_CARBONICACID, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_ESTER, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_PACID_1, skipIfAtomInRing);
		calcCenterNoCarbon(mol, IDCODE_FRAG_CENTER_AMIDE, skipIfAtomInRing);
		
		
		// First the terminal alkyl groups have to be computed, then all the other stuff.
		FFMoleculeFunctions.calcTerminalAlkylGroupsCenter(mol);
		
		calcAliphaticRingCenter(mol);
		
		flagDeadAtoms(mol);
		
		if(bremoveflagged) {
			FFMoleculeFunctions.removeCarbon(mol);
			FFMoleculeFunctions.removeFlaggedAtoms(mol);
		} else {
			FFMoleculeFunctions.flagCarbon(mol);
			FFMoleculeFunctions.flagUnflaggedWithFlagCenter(mol);
		}
		
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if (!mol.isAtomFlag(i, FFMolecule.FLAG1))
				mol.setAtomFlag(i, FFMolecule.LIGAND, true);
		}
			
		
		// For pharmacophore atoms in the graph we have to add an additional atom 
		// which is not connected to any other atoms. So it will be visualized as a ball.
		int atoms = mol.getAllAtoms();
		for(int i=0; i < atoms; i++){
			if(mol.isAtomFlag(i, FFMoleculeFunctions.FLAG_CENTER_ATOM)) {
				if(mol.getConnAtoms(i)>0){
					addFlaggedAtomAndUnflagOrig(mol, i);
				}
			}
		}
	}

	/**
	 * New atom is added and flagged. The index of the old atom is written into the 
	 * group of the new atom.
	 * @param mol
	 * @param at
	 */
	private static void addFlaggedAtomAndUnflagOrig(FFMolecule mol, int at){
		int indexNew = mol.addAtom(mol.getAtomicNo(at));
		mol.setAtomInteractionClass(indexNew, mol.getAtomInteractionClass(at));
		mol.setAtomMM2Class(indexNew, mol.getAtomMM2Class(at));
		
		String sOrigIndex = Integer.toString(at);
		mol.setAtomChainId(indexNew, sOrigIndex);
		
		mol.setCoordinates(indexNew, mol.getCoordinates(at));
		mol.setAtomFlag(indexNew, FFMoleculeFunctions.FLAG_CENTER_ATOM, true);
		mol.setPPP(indexNew, mol.getPPP(at));
		
		// The original atom is not longer a center.
		mol.setAtomFlag(at, FFMoleculeFunctions.FLAG_CENTER_ATOM, false);
		mol.setAtomFlag(at, FFMolecule.FLAG1, true);
	}
	
	private static void calcAromatic(FFMolecule mol) {

		List<Integer> liIndices2Del = new ArrayList<Integer>();
		List<int []> liRingsMol = mol.getAllRings();
		List<int []> liRings = ArrayUtilsCalc.copyIntArray(liRingsMol);
		
		// fuseRings(liRings);
		
		for (int[] arrIndices : liRings) {
					
			if(arrIndices==null)
				continue;
			
			boolean bIsAromaticRing = true;
			for (int i = 0; i < arrIndices.length; i++) {
				if (!mol.isAromatic(arrIndices[i])) {
					bIsAromaticRing = false;
					break;
				}
			}

			if (bIsAromaticRing) {
				List<Integer> li = calcCenter(mol, arrIndices);
				liIndices2Del.addAll(li);
			}
		}
		for (int i = 0; i < liIndices2Del.size(); i++) {
			int index = liIndices2Del.get(i);
			mol.setAtomFlag(index, FFMolecule.FLAG1, true);
		}

		
	}
	/**
	 * Each atom specified in arrIndices is added to the mol with the center coordinates. 
	 * The atoms from arrIndices are marked to be deleted. The index of the original atom is 
	 * written into the group of the new atom.  
	 * @param mol
	 * @param arrIndices
	 * @return
	 */
	private static List<Integer> calcCenter(FFMolecule mol, int[] arrIndices) { 
		Coordinates coord = FFMoleculeFunctions.getCenterGravity(mol, arrIndices);
		return calcCenter(mol, arrIndices, coord);
	}
	
	/**
	 * 
	 * @param mol
	 * @param arrIndices
	 * @param coord
	 * @return list of atomic indices to be deleted.
	 */
	private static List<Integer> calcCenter(FFMolecule mol, int[] arrIndices, Coordinates coord) { 
		List<Integer> liIndices2Del = new ArrayList<Integer>();
		
		int cc=0;
		// 
		// For each atom a center atom is calculated, because an atom can only keep one atom class and one interaction id.
		// 
		for (int i = 0; i < arrIndices.length; i++) {
			// These atoms will be deleted
			liIndices2Del.add(new Integer(arrIndices[i]));
			int iAtomicNo = mol.getAtomicNo(arrIndices[i]);
			int mm2Type = mol.getAtomMM2Class(arrIndices[i]);
			int interactionType = mol.getAtomInteractionClass(arrIndices[i]);
			// int index = mol.getAllAtoms();
			int index = mol.addAtom(iAtomicNo);
			mol.setAtomInteractionClass(index, interactionType);
			mol.setAtomMM2Class(index, mm2Type);

			mol.setCoordinates(index, coord);
			mol.setAtomFlag(index, FFMoleculeFunctions.FLAG_CENTER_ATOM, true);
			
			String sOrigIndex = Integer.toString(arrIndices[i]);
			mol.setAtomChainId(index, sOrigIndex);
			
			mol.setPPP(index, arrIndices);
			
			cc++;
		}
		// System.out.println("Ring center with " + cc + " different hetero atoms found.");
		return liIndices2Del;
	}

	public static final void flagDeadAtoms(FFMolecule mol) {
		
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_ESTER, INDEX_ATS2DEL_ESTER);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_AMIDE, INDEX_ATS2DEL_AMIDE);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_ANILIN, INDEX_ATS2DEL_ANILIN);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_PHENYL_O_C_C, INDEX_ATS2DEL_PHENYL_O_C_C);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_AR_O_AR, INDEX_ATS2DEL_FRAG_AR_O_AR_O);
		FFMoleculeFunctions.flagSubstructure(mol, IDCODE_FRAG_SULFOXIDE, INDEX_ATS2DEL_SULFOXIDE);
		
		
		// removeFlaggedAtoms(mol);
	}

	/**
	 * All hetero atoms are added as ring center.
	 * @param mol
	 */
	private static void calcRingCenter(FFMolecule mol) {

		List<Integer> liIndices2Del = new ArrayList<Integer>();
		List<int[]> liRingsMol = mol.getAllRings();
		
		// It is not possible to fuse the ringlist direct
		List<int[]> liRings = ArrayUtilsCalc.copyIntArray(liRingsMol);
		// fuseRings(liRings);
		for (int i=0; i<liRings.size();i++) {
			int[] arrIndices = (int[]) liRings.get(i);
			if(arrIndices != null) {
				// Check ring for aromaticity first
				boolean bAromaticRing = true;
				for (int j = 0; j < arrIndices.length; j++) {
					if (!mol.isAromatic(arrIndices[j])) {
						bAromaticRing = false;
						break;
					}
				}
				if(!bAromaticRing) {
					List<Integer> li = calcCenter(mol, arrIndices);
					liIndices2Del.addAll(li);
				}
			}
		}
		
		for (int i = 0; i < liIndices2Del.size(); i++) {
			int index = liIndices2Del.get(i);
			mol.setAtomFlag(index, FFMolecule.FLAG1, true);
		}
	}
	
	/**
	 * Calculates the center of rings. Takes only aliphatic atoms into account.
	 * This includes the aromatic C atoms as well.
	 * @param mol
	 */
	private static void calcAliphaticRingCenter(FFMolecule mol) {

		List<Integer> liIndices2Del = new ArrayList<Integer>();
		List<int[]> liRingsMol = mol.getAllRings();
		
		// It is not possible to fuse the ringlist direct
		List<int[]> liRings = ArrayUtilsCalc.copyIntArray(liRingsMol);
		// fuseRings(liRings);
		for (int i=0; i<liRings.size();i++) {
			int[] arrIndices = liRings.get(i);
			if(arrIndices != null) {
					
				IntArray arr = new IntArray();
				
				for (int j = 0; j < arrIndices.length; j++) {
					if(FFMoleculeFunctions.isAliphaticAtom(mol, arrIndices[j])){
						arr.add(arrIndices[j]);
					}
				}
				
				// System.out.println("calcAliphaticRingCenter aliphatic " + arr.length());
				
				if(arr.length()>0) {
					Coordinates coord = FFMoleculeFunctions.getCenterGravity(mol, arrIndices);
					
					List<Integer> li = calcCenter(mol, arr.get(), coord);
					
					liIndices2Del.addAll(li);
				}
			}
		}
		
		for (int i = 0; i < liIndices2Del.size(); i++) {
			int index = liIndices2Del.get(i);
			mol.setAtomFlag(index, FFMolecule.FLAG1, true);
		}
	}

}