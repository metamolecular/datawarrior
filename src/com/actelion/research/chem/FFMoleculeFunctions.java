package com.actelion.research.chem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import com.actelion.research.calc.ArrayUtilsCalc;
import com.actelion.research.calc.Matrix;
import com.actelion.research.calc.principalcomponentanalysis.PCA;
import com.actelion.research.chem.calculator.AdvancedTools;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.chem.mcs.ListWithIntVec;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.mm2.MM2Config;
import com.actelion.research.forcefield.optimizer.AlgoConjugateGradient;
import com.actelion.research.forcefield.optimizer.AlgoLBFGS;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.forcefield.optimizer.PreOptimizer;
import com.actelion.research.util.datamodel.IntegerDouble;

/**
 * 
 * 
 * FFMoleculeFunctions
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2006? MvK: Start implementation
 */
public class FFMoleculeFunctions {

	public static final String LEADING_SPACE_DESCRIPTION = "     ";

	public static final int FLAG_CENTER_ATOM = 1<<4;
	
	public static final int FLAG_AROMATIC_ATOM = 1<<5;

	
	private static final NumberFormat NF = new DecimalFormat("0.000");
	
	private static final int STERIC_HINDRANCE_STRONG = 1000;
	
	private static final int STERIC_HINDRANCE_MEDIUM = 100;
	
	private static final int STERIC_HINDRANCE_WEAK = 10;

	private static final double STERIC_HINDRANCE_FACTOR_STRONG = 1.0;
	
	private static final double STERIC_HINDRANCE_FACTOR_MEDIUM = 1.5;
	
	private static final double STERIC_HINDRANCE_FACTOR_WEAK = 2.0;

	
	public static final int PRINCIPAL_COMPONENTS = 3;

	public static final int COLS = 3;

	/**
	 * Applies Joels forcefield with a full optimisation. Adds also Hydrogens.
	 * @param ff
	 * @return
	 */

	public static FFMolecule add3DCoordinates(FFMolecule ff) {
		
		StructureCalculator.deleteHydrogens(ff);
		
		new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(ff)));
		
		StructureCalculator.vibrateLigand(ff, 0.1);

		//Add the job to Mopac RMI
		System.out.println("Add Job");
		ff.reorderHydrogens(); //VERY IMPORTANT!!!
		
		AdvancedTools.optimizeRings(ff);
		
		AdvancedTools.optimizeByVibrating(ff);
		
		
//		FFMolecule ffConf = new FFMolecule(ff);
//				
//		AdvancedTools.optimizeByVibrating(ffConf);
//		
//		AlgoLBFGS algo = new AlgoLBFGS();
//		
//		algo.optimize(new EvaluableForceField(new ForceField(ffConf)));
//		
		FFMoleculeFunctions.removeElectronPairs(ff);
		
		return ff;
	}

	/**
	 * 
	 * @param mol molecule with 3D coordinates. I.e. from ConformationSampler.
	 * @return
	 */
	public static FFMolecule preoptimize(StereoMolecule mol) {
		
		FFMolecule ffConf = new FFMolecule(mol);
		
		StructureCalculator.addHydrogens(ffConf);
		
		AlgoConjugateGradient algoConjugateGradient = new AlgoConjugateGradient();
		
		algoConjugateGradient.optimize(new EvaluableForceField(new ForceField(ffConf)));
		
		AdvancedTools.optimizeByVibrating(ffConf);
		
		AlgoLBFGS algo = new AlgoLBFGS();
		
		algo.setMinRMS(1);
		MM2Config conf = new MM2Config();
		conf.setUseOutOfPlaneAngle(false);
		algo.optimize(new EvaluableForceField(new ForceField(ffConf, conf)));
		
		// algoConjugateGradient.optimize(new EvaluableForceField(new ForceField(ffConf)));
		
		FFMoleculeFunctions.removeElectronPairs(ffConf);
		
		return ffConf;
	}
	
	public static double [][] getAdjacencyMatrix(FFMolecule mol) {
		
		double arr[][]= new double[mol.getAllAtoms()][mol.getAllAtoms()];
		
		for (int i = 0; i < mol.getAllBonds(); i++) {
			
			int indexAtom1 = mol.getBondAtom(0, i);
			
			int indexAtom2 = mol.getBondAtom(1, i);
			
			arr[indexAtom1][indexAtom2]=1;
			arr[indexAtom2][indexAtom1]=1;
			
		}
		
		return arr;
	}

	
	/**
	 * Protons attached to an O.
	 * @param ff
	 * @return
	 */
	public static List<Integer> getAcidicProtons(FFMolecule ff){
		
		List<Integer> liIndexAtomAcidicProt = new ArrayList<Integer>();
		
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			if(ff.getAtomicNo(i)==8){
				
				int nConnected = ff.getAllConnAtoms(i);
				
				for (int j = 0; j < nConnected; j++) {
					
					int indexConnAt = ff.getConnAtom(i, j);
					
					if(ff.getAtomicNo(indexConnAt)==1){
						liIndexAtomAcidicProt.add(indexConnAt);
					}
				}
			}
		}
		
		return liIndexAtomAcidicProt;

	}
	
	/**
	 * Donor atoms. Oxygen with a connected hydrogen.
	 * @param ff
	 * @return
	 */
	public static List<Integer> getDeprotonableAtoms(FFMolecule ff){
		
		List<Integer> liIndexAtomAcidic = new ArrayList<Integer>();
		
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			if(ff.getAtomicNo(i)==8){
				
				int nConnected = ff.getAllConnAtoms(i);
				
				for (int j = 0; j < nConnected; j++) {
					
					int indexConnAt = ff.getConnAtom(i, j);
					
					if(ff.getAtomicNo(indexConnAt)==1){
						liIndexAtomAcidic.add(i);
						break;
					}
				}
			}
		}
		
		return liIndexAtomAcidic;

	}
	
	public static List<Integer> getProtonableAtoms(FFMolecule ff){
		
		List<Integer> liIndexAtomBasic = new ArrayList<Integer>();
		
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			
			if(ff.getAtomicNo(i)==7){
				
				if(isProtonableNitrogen(ff, i)) {
					liIndexAtomBasic.add(i);
					break;
				}
			}
		}
		
		return liIndexAtomBasic;

	}
	
	public static boolean isProtonableNitrogen(FFMolecule ff, int indexAtom){
		boolean protonableN=true;
		
		if(ff.getAtomicNo(indexAtom)!=7){
			return false;
		}
		
		int nConnected = ff.getAllConnAtoms(indexAtom);
		
			
		// Already protonated N?
		boolean hydrogen=false;
		for (int i = 0; i < nConnected; i++) {
			int indexAtomConn = ff.getConnAtom(indexAtom, i);
			
			if(ff.getAtomicNo(indexAtomConn)==1){
				hydrogen=true;
				return true;
			}
		}
		
		if((!hydrogen) && (nConnected==4)){
			return false;
		}
		
		
		int sumBondOrder=0;
		for (int i = 0; i < nConnected; i++) {
			sumBondOrder += ff.getConnBondOrder(indexAtom, i);
		}
		
		if(sumBondOrder==4){
			return false;
		}
		
		return protonableN;
	}
	

	public static boolean has2DCoordinates(FFMolecule ff){
		
		double sumX=0;
		double sumY=0;
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			Coordinates c = ff.getCoordinates(i);
			
			if(c==null){
				continue;
			}
			
			sumX += Math.abs(c.x);
			sumY += Math.abs(c.y);
		}
		
		if(sumX!=0 && sumY!=0){
			return true;
		}
		
		return false;
	}
	
	public static boolean has3DCoordinates(FFMolecule ff){
		
		double sumX=0;
		double sumY=0;
		double sumZ=0;
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			Coordinates c = ff.getCoordinates(i);
			
			if(c==null){
				continue;
			}
			
			sumX += Math.abs(c.x);
			sumY += Math.abs(c.y);
			sumZ += Math.abs(c.z);
		}
		
		if(sumX!=0 && sumY!=0){
			return true;
		}
		
		return false;
	}
	
	public static boolean hasChargedAtom(FFMolecule ff){
		
		boolean charged = false;
		
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			
			if(ff.getAtomCharge(i)!=0){
				charged = true;
				break;
			}
		}
		
		return charged;
	}
	
	/**
	 * Calculates a value for the steric hindrance of the atoms from liIndexAtoms to all other atoms.
	 * @param ff
	 * @param liIndexAtoms
	 * @return
	 */
	public static double getStericHindrance(FFMolecule ff, List<Integer> liIndexAtoms) {
		double hindrance = 0;
		
		HashSet<Integer> hsIndexAtoms = new HashSet<Integer>(liIndexAtoms);
		
		List<Integer> liIndexAtoms2 = new ArrayList<Integer>();
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			if(!hsIndexAtoms.contains(i)){
				liIndexAtoms2.add(i);
			}
		}
		
    	double [][] arrDist = FFMoleculeFunctions.getDistanceArray(ff);

		
		for (int i = 0; i < liIndexAtoms.size(); i++) {
			int indexAt1 = liIndexAtoms.get(i);
			
			for (int j = 0; j < liIndexAtoms2.size(); j++) {
				int indexAt2 = liIndexAtoms2.get(i);
				
				double dist = arrDist[indexAt1][indexAt2];
				
				Element el1 = PeriodicTable.getElement(ff.getAtomicNo(indexAt1));
				Element el2 = PeriodicTable.getElement(ff.getAtomicNo(indexAt2));
				
				double r1 = el1.getVDWRadius();
				
				double r2 = el2.getVDWRadius();
				
				double sumRadii = r1+r2;
				
				double distHindranceStrong = dist * STERIC_HINDRANCE_FACTOR_STRONG;
				
				double distHindranceMedium = dist * STERIC_HINDRANCE_FACTOR_MEDIUM;
				
				double distHindranceWeak = dist * STERIC_HINDRANCE_FACTOR_WEAK;
				
				if(sumRadii>distHindranceStrong ) {
					hindrance += STERIC_HINDRANCE_STRONG;
				} else if(sumRadii>distHindranceMedium ) {
					hindrance += STERIC_HINDRANCE_MEDIUM;
				} else if(sumRadii>distHindranceWeak ) {
					hindrance += STERIC_HINDRANCE_WEAK;
				}
			}
		}
		
		
		return hindrance;
	}

	public static FFMolecule add(FFMolecule ff1, FFMolecule ff2) {
		FFMolecule ff = new FFMolecule(ff1);
		
		
		int [] arrMap = new int [ff2.getAllAtoms()];
		for (int i = 0; i < ff2.getAllAtoms(); i++) {
			int indexAt = ff.addAtom(ff2, i);
			
			arrMap[i]=indexAt;
		}
		
		for (int i = 0; i < ff2.getAllBonds(); i++) {
			int indexAt1 = ff2.getBondAtom(0, i);
			int indexAt2 = ff2.getBondAtom(1, i);
			
			int order = ff2.getBondOrder(i);
			
			int indexAt1New = arrMap[indexAt1];
			int indexAt2New = arrMap[indexAt2];
			
			ff.addBond(indexAt1New, indexAt2New, order);
			
		}
		return ff;
	}
	
	/**
	 * Creates a bond between indexAtom1 and indexAtom2 with the given bond order.
	 * @param ff1
	 * @param indexAtom1
	 * @param ff2
	 * @param indexAtom2
	 * @param bondOrder
	 * @return
	 */
	public static FFMolecule addAndConnect(FFMolecule ff1, int indexAtom1, FFMolecule ff2, int indexAtom2, int bondOrder) {
		FFMolecule ff = new FFMolecule(ff1);
				
		int [] arrMap = new int [ff2.getAllAtoms()];
		for (int i = 0; i < ff2.getAllAtoms(); i++) {
			int indexAt = ff.addAtom(ff2, i);
			
			arrMap[i]=indexAt;
		}
		
		for (int i = 0; i < ff2.getAllBonds(); i++) {
			int indexAt1 = ff2.getBondAtom(0, i);
			int indexAt2 = ff2.getBondAtom(1, i);
			
			int order = ff2.getBondOrder(i);
			
			int indexAt1New = arrMap[indexAt1];
			int indexAt2New = arrMap[indexAt2];
			
			ff.addBond(indexAt1New, indexAt2New, order);
			
		}
		
		ff.addBond(indexAtom1, arrMap[indexAtom2], bondOrder);
		
		return ff;
	}
	
	/**
	 * 
	 * @param ff
	 * @param minSize
	 * @return returns false if molecule contains at least one molecule with allAtoms >= minSize.
	 */
	public static boolean isSmallMolecules(FFMolecule ff, int minSize){
		boolean small=true;
		
		FFMoleculeExtractor extractor = new FFMoleculeExtractor(ff);
		
		int n = extractor.size();
		
		for (int i = 0; i < n; i++) {
			if(extractor.getMolecule(i).getAllAtoms()>=minSize) {
				small=false;
				break;
			}
		}
		
		return small;
	}
	
	public static boolean isPyramedal(FFMolecule ff){
		
		boolean pyramedal=false;
		if(ff.getAllAtoms()==4) {
			for (int i = 0; i < ff.getAllAtoms(); i++) {
				if(ff.getAllConnAtoms(i)==3){
					pyramedal=true;
					break;
				}
			}
		}
		return pyramedal;
	}
	
	public static boolean isTripleBond(FFMolecule ff){
		
		boolean tripleBond=false;
		for (int i = 0; i < ff.getAllBonds(); i++) {
			
			if(ff.getBondOrder(i)==3){
				tripleBond=true;
				break;
			}
		}
		
		return tripleBond;
	}
	
	public static void addMissingHydrogens(FFMolecule ff) {
		for (int i = 0; i < ff.getAtoms(); i++) {
			if(ff.getAtomicNo(i)==8){
				int indNew = ff.addAtom(1);
				ff.addBond(i, indNew, 1);
			}
		}
	}

	public static void writeCharge2Label(FFMolecule ff){
		
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			int charge = ff.getAtomCharge(i);
			if(charge!=0){
				String l = "   " + charge;
				ff.setAtomDescription(i, l);
			} else {
				ff.setAtomDescription(i, "");
			}
		}
		
	}

	private static List<int []> getListFromSSSearcher(StereoMolecule mol, StereoMolecule fragment){
		SSSearcher sss = new SSSearcher();
		sss.setMol(fragment,mol);
		
		int numFrags = sss.findFragmentInMolecule(SSSearcher.cCountModeOverlapping, SSSearcher.cMatchAromDBondToDelocalized);
		List<int []> liAtomLists = new ArrayList<int []>();
		if(numFrags > 0)  {
			liAtomLists = sss.getMatchList();
			// Remove double atomlists
			ArrayUtilsCalc.removeDoubletsIntOrderIndepend(liAtomLists);
		}

		return liAtomLists;
	}



	/**
	 * 
	 * @param ffMol Molecule
	 * @param idCodeQ1 The idcode for the substructure (ode fragment)
	 * @param arrIndexAts2Flag the index for atoms in the idCode1q. The 
	 * corresponding atoms in the molecule will be deleted. 
	 */
	public static void flagSubstructure(FFMolecule ffMol, String idCodeQ1, int [] arrIndexAts2Flag) {
		
		// StereoMolecule mol = convert2StereoMolecule(ffMol);
		
		StereoMolecule mol = ffMol.toStereoMolecule();
		
		mol.ensureHelperArrays(Molecule.cHelperRings);
		

		StereoMolecule query = new StereoMolecule();
		
		query.setFragment(true);
		
		new IDCodeParser().parse(query,idCodeQ1);
			
		SSSearcher sss = new SSSearcher();
		sss.setMol(query,mol);
		
		int numFrags = sss.findFragmentInMolecule(SSSearcher.cCountModeOverlapping, SSSearcher.cMatchAromDBondToDelocalized);
		List<int[]> liAtomLists = new ArrayList<int[]>();
		if(numFrags > 0)  {
			List<int []> vecMatchList = sss.getMatchList();
			for (Iterator<int []> iter = vecMatchList.iterator(); iter.hasNext();) {
				int [] atomlist = iter.next();
				liAtomLists.add(atomlist);
			}
		}
		
		if(liAtomLists.size() == 0) {
			// Exception ex = new Exception("Substructure not found.");
			// ex.printStackTrace();
			return;
		}
		
		// System.out.println("Found: " + liAtomLists.size());

		
		int [] arrAts2DelInMol = new int [arrIndexAts2Flag.length * liAtomLists.size()];
		int cc = 0;
		// Extract substructure from molecule
		for (Iterator<int []> iter = liAtomLists.iterator(); iter.hasNext();) {
			int [] arrAtomList = iter.next();
			
			for (int i = 0; i < arrIndexAts2Flag.length; i++) {
				arrAts2DelInMol[cc++] =  arrAtomList[arrIndexAts2Flag[i]];
			}
		}

		
		for (int i = 0; i < arrAts2DelInMol.length; i++) {
			ffMol.setAtomFlag(arrAts2DelInMol[i], FFMolecule.FLAG1, true);
		}
		
	}
	
	/**
	 * Protonates or deprotonates the specified atom, if necessary and possible, to neutralize a charge.
	 * @param ff
	 * @param indexAtomBasic
	 * @return
	 */
	public static FFMolecule getNeutralized(FFMolecule ff, int indexAtomReactiveCenter) {
		FFMolecule ffNeutral = null;
		
		int charge = ff.getAtomCharge(indexAtomReactiveCenter); 
		
		if(charge==0){
			ffNeutral = new FFMolecule(ff);
			
		} else if (charge==1){
		
			ffNeutral = getDeprotonated(ff, indexAtomReactiveCenter);
			
		} else if (charge==-1){
		
			ffNeutral = getProtonated(ff, indexAtomReactiveCenter);
			
		}
		
		return ffNeutral;
	}

	
	public static FFMolecule convert(String smiles) throws Exception {
		
		StereoMolecule extMol = new StereoMolecule();
		
		new SmilesParser().parse(extMol, smiles);

		// Create a molecule C(=N)O
		FFMolecule mol = new FFMolecule(extMol);

		// Optimize the mol
		PreOptimizer.preOptimize(mol);
		new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(mol)));

		FFMoleculeFunctions.removeHydrogensAndElectronPairs(mol);
		
		return mol;

	}
	
//	public static StereoMolecule convert2StereoMolecule(FFMolecule mol) {
//
//		StereoMolecule ster = new StereoMolecule();
//		copy2StereoMolecule(mol, ster);
//
//		return ster;
//
//	}

//	public static void copy2StereoMolecule(FFMolecule ff, StereoMolecule ster) {
//
//		ster.deleteMolecule();
//		
//		for (int i = 0; i < ff.getAllAtoms(); i++) {
//			ster.addAtom(ff.getAtomicNo(i));
//			ster.setAtomCharge(i, ff.getAtomCharge(i));
//			ster.setAtomX(i, ff.getAtomX(i));
//			ster.setAtomY(i, ff.getAtomY(i));
//			ster.setAtomZ(i, ff.getAtomZ(i));
//		}
//		for (int i = 0; i < ff.getAllBonds(); i++) {
//			ster.addBond(ff.getBondAtom(0, i), ff.getBondAtom(1, i), ff.getBondOrder(i));
//		}
//		ster.ensureHelperArrays(Molecule.cHelperCIP);
//		
//	}
	
	public static void copy(FFMolecule ffSource, FFMolecule ffDestination) {
		ffDestination.clear();
		
		for (int i = 0; i < ffSource.getAllAtoms(); i++) {
			ffDestination.addAtom(ffSource.getAtomicNo(i));
			ffDestination.setAtomCharge(i, ffSource.getAtomCharge(i));
			ffDestination.setAtomX(i, ffSource.getAtomX(i));
			ffDestination.setAtomY(i, ffSource.getAtomY(i));
			ffDestination.setAtomZ(i, ffSource.getAtomZ(i));
		}
		for (int i = 0; i < ffSource.getAllBonds(); i++) {
			ffDestination.addBond(ffSource.getBondAtom(0, i), ffSource.getBondAtom(1, i), ffSource.getBondOrder(i));
		}
	}

	/**
	 * 
	 * @param idCode
	 *            Actelion idCode
	 * @return
	 */
	public static FFMolecule getBiggestFragment(String idCode) {
		StereoMolecule extMol = new StereoMolecule();
		new IDCodeParser().parse(extMol, idCode);
		StereoMolecule [] frags = extMol.getFragments();

		int indexBiggestFrag = 0;

		if (frags.length > 1) {
			int maxAtoms = 0;
			for (int ii = 0; ii < frags.length; ii++) {
				if (frags[ii].getAllAtoms() > maxAtoms) {
					indexBiggestFrag = ii;
					maxAtoms = frags[ii].getAllAtoms();
				}
			}

		}

		FFMolecule mol = new FFMolecule(frags[indexBiggestFrag]);

		// Optimize the mol
		PreOptimizer.preOptimize(mol);
		new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(mol)));
		// FFViewer.viewMolecule(mol);

		return mol;
	}

	public static FFMolecule get(String idCode) {
		StereoMolecule extMol = new StereoMolecule();
		new IDCodeParser().parse(extMol, idCode);

		FFMolecule mol = new FFMolecule(extMol);

		// Optimize the mol
		PreOptimizer.preOptimize(mol);
		new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(mol)));
		// FFViewer.viewMolecule(mol);

		return mol;
	}

	/**
	 * Changes the coordinates with a random value. Gaussian distribution.
	 * @param ff
	 * @param maxLengthDistortion if 0 just a copy of ff will be returned.
	 * @return
	 */
	public static FFMolecule distortCoordinates(FFMolecule ff, double maxLengthDistortion){
		
		FFMolecule ffShaked = new FFMolecule(ff);
		
		if(maxLengthDistortion==0){
			return ffShaked;
		}
		
		Random rnd = new Random();
		
		for (int i = 0; i < ffShaked.getAllAtoms(); i++) {
			
			double [] arrRND = new double [3]; 
			
			for (int j = 0; j < arrRND.length; j++) {
				
				double d = rnd.nextGaussian() * maxLengthDistortion;
				
				if(Math.abs(d)>maxLengthDistortion){
					
					if(d>0)
						d = maxLengthDistortion;
					else
						d = maxLengthDistortion*(-1);
					
				}
				
				arrRND[j]=d;
				
			}
			
			Coordinates c = ffShaked.getCoordinates(i);
			c.x += arrRND[0];
			c.y += arrRND[1];
			c.z += arrRND[2];
						
			ffShaked.setCoordinates(i, c);
		}
		
		
		return ffShaked;
	}
	
	public static void addRNDCoordinates(FFMolecule mol, double maxNoise){
		
		Random rnd = new Random();
		
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			
			double x = mol.getAtomX(i) + (rnd.nextGaussian() * maxNoise);
			
			double y = mol.getAtomY(i) + (rnd.nextGaussian() * maxNoise);
			
			double z = mol.getAtomZ(i) + (rnd.nextGaussian() * maxNoise);
			
			mol.setAtomX(i, x);
			mol.setAtomY(i, y);
			mol.setAtomZ(i, z);
		}
	}
	

	
	private static void setRNDCoordinates(FFMolecule mol){
		double fac = 100;
		Random rnd = new Random();
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			mol.setAtomX(i,rnd.nextDouble() * fac);
			mol.setAtomY(i,rnd.nextDouble() * fac);
			mol.setAtomZ(i,rnd.nextDouble() * fac);
		}
	}
	
	public static void calculateCoordinatesIterative(FFMolecule mol, double [][] arrDist){
		
		
		int maxcycles = 10000;
		int mSize = mol.getAllAtoms();
		
		setRNDCoordinates(mol);
		
		boolean bFinished=false;
		
		double cycleFactor = 1.0;
		
		
		double start = 0;
		int cc=0;
		
		while(!bFinished){
			handleDistanceConstraint(mol, cycleFactor, arrDist);
			
			if(cc==1) {
				start = 0;
				for (int i = 0; i < mSize; i++) {
					for (int j = i+1; j < mSize; j++) {
						start += Math.abs(arrDist[i][j] - getDistanceFromCoord(mol, i,j));
					}
				}
			}
				
			if(cycleFactor <= 0) {
				bFinished=true;
			}else if(cc == maxcycles){
				bFinished = true;
			}
			
			
			cycleFactor -= 1.0/maxcycles;
			
			cc++;
		}
		
		double end = 0;
		for (int i = 0; i < mSize; i++) {
			for (int j = i+1; j < mSize; j++) {
				end += Math.abs(arrDist[i][j] - getDistanceFromCoord(mol, i,j));
			}
		}
		System.out.println("Start: " + start +" end: " + end);
		
		
	}
	
	public static double getDistanceFromCoord(FFMolecule mol, int atom1, int atom2){
		

		double dx = mol.getAtomX(atom2) - mol.getAtomX(atom1);
		double dy = mol.getAtomY(atom2) - mol.getAtomY(atom1);
		double dz = mol.getAtomZ(atom2) - mol.getAtomZ(atom1);
		double distance = Math.sqrt(dx*dx+dy*dy+dz*dz);
		
		
		return distance;
	}
	
	
	private static void handleDistanceConstraint(FFMolecule mol, double cycleFactor, double[][] mDistance) {
		Random random = new Random();
		
		int mSize = mol.getAllAtoms();


		int atom1 = (int)(random.nextDouble() * mSize);
		int atom2 = (int)(random.nextDouble() * mSize);
		while (atom2 == atom1)
			atom2 = (int)(random.nextDouble() * mSize);

		if (atom1 < atom2) {
			int temp = atom1;
			atom1 = atom2;
			atom2 = temp;
		}

		double dx = mol.getAtomX(atom2) - mol.getAtomX(atom1);
		double dy = mol.getAtomY(atom2) - mol.getAtomY(atom1);
		double dz = mol.getAtomZ(atom2) - mol.getAtomZ(atom1);
		double distance = Math.sqrt(dx*dx+dy*dy+dz*dz);

		double distanceFactor = 0.0;
		
		if (distance < mDistance[atom1][atom2]) {
			distanceFactor = (distance - mDistance[atom1][atom2])
					/ (2 * mDistance[atom1][atom2]);
			if (cycleFactor > 1.0)
				cycleFactor = 1.0;
		} else if (distance > mDistance[atom1][atom2]) {
			distanceFactor = (distance - mDistance[atom1][atom2])
					/ (2 * distance);
		}
		
		if (Math.abs(distanceFactor) > 0.0001) {
			double factor = cycleFactor * distanceFactor;
			mol.setAtomX(atom1, mol.getAtomX(atom1)+dx*factor);
			mol.setAtomX(atom2, mol.getAtomX(atom2)-dx*factor);
			mol.setAtomY(atom1, mol.getAtomY(atom1)+dy*factor);
			mol.setAtomY(atom2, mol.getAtomY(atom2)-dy*factor);
			mol.setAtomZ(atom1, mol.getAtomZ(atom1)+dz*factor);
			mol.setAtomZ(atom2, mol.getAtomZ(atom2)-dz*factor);
		}
	}

	public static double [][] getDistanceArray(FFMolecule mol) {
		
		double arr[][]= new double[mol.getAllAtoms()][mol.getAllAtoms()];
		
		for (int i = 0; i < arr.length; i++) {
			for (int j = i+1; j < arr.length; j++) {
				double dx = mol.getAtomX(i) - mol.getAtomX(j);
				double dy = mol.getAtomY(i) - mol.getAtomY(j);
				double dz = mol.getAtomZ(i) - mol.getAtomZ(j);
				double v= Math.sqrt(dx*dx+dy*dy+dz*dz);
				arr[i][j] = v;
				arr[j][i] = v;
			}
		}
		
		return arr;
	}
	
	public static boolean isParametrized(FFMolecule mol) {
		boolean bOk = true;

		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if (mol.getAtomicNo(i) > 1 && mol.getAtomInteractionClass(i) == -1) {
				System.out.println("Atomic no: " + mol.getAtomicNo(i));
				bOk = false;
				break;
			}
		}

		return bOk;
	}
	

	
	public static boolean isConnectedAtoms(FFMolecule ff, List<Integer> liIndexAtoms, int atm2) {
		boolean connected = false;
		
		HashSet<Integer> hsAtomIndex = new HashSet<Integer>(liIndexAtoms);

		int nConnected = ff.getConnAtoms(atm2);

		for (int i = 0; i < nConnected; i++) {
			int indexAtomConnected = ff.getConnAtom(atm2, i);
			
			if(hsAtomIndex.contains(indexAtomConnected)){
				connected = true;
				break;
				
			}
			
		}
	
		return connected;
	}
	
	
	
	/**
	 * 
	 * @param mol
	 * @param atm index of atom
	 * @return false if atom is not C or if one of the neighbours is N, O, F, S, P or Cl.
	 */
	public static boolean isAliphaticAtom(FFMolecule mol, int atm) {
		boolean aliphatic = true;
		
		if(mol.getAtomicNo(atm)!=6)
			return false;
		
		int nConn = mol.getAllConnAtoms(atm);
		
		for (int i = 0; i < nConn; i++) {
			int atmConn = mol.getConnAtom(atm, i);
			
			int atomicNoConn = mol.getAtomicNo(atmConn);
			
			if((atomicNoConn == 7) || 
			   (atomicNoConn == 8) ||
			   (atomicNoConn == 9) ||
			   (atomicNoConn == 15) ||
			   (atomicNoConn == 16) ||
			   (atomicNoConn == 17)) {
				aliphatic = false;
				break;
			}
		}
		
		return aliphatic;
	}

	public static void removeElectronPairs(FFMolecule mol) {
		for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
			if (mol.getAtomicNo(i) < 1)
				mol.deleteAtom(i);
		}
	}
	
	public static void removeHydrogensAndElectronPairs(FFMolecule mol) {
		// Remove hydrogens
		for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
			if (mol.getAtomicNo(i) <= 1)
				mol.deleteAtom(i);
		}
	}

	public static void removeElement(FFMolecule mol, int atomicNo) {
		for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
			if (mol.getAtomicNo(i) == atomicNo)
				mol.deleteAtom(i);
		}
	}

	public static void removeNonAromaticC(FFMolecule mol) {
		for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
			if (mol.getAtomicNo(i) == 6 && !mol.isAromatic(i)
					&& !mol.isAtomFlag(i, FLAG_CENTER_ATOM))
				mol.deleteAtom(i);
		}

	}

	/**
	 * Removes all Carbon atoms, except the FLAG_CENTER_ATOM is set.
	 * 
	 * @param mol
	 *            Molecule
	 */
	public static void removeCarbon(FFMolecule mol) {
		for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
			if (mol.getAtomicNo(i) == 6 && !mol.isAtomFlag(i, FLAG_CENTER_ATOM))
				mol.deleteAtom(i);
		}

	}
	public static void flagCarbon(FFMolecule mol) {
		for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
			if (mol.getAtomicNo(i) == 6 && !mol.isAtomFlag(i, FLAG_CENTER_ATOM))
				mol.setAtomFlag(i, FFMolecule.FLAG1, true);
		}

	}
	
	public static void flagUnflaggedWithFlagCenter(FFMolecule mol) {
		for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
			if (!mol.isAtomFlag(i, FFMolecule.FLAG1))
				mol.setAtomFlag(i, FLAG_CENTER_ATOM, true);
		}
	}
	
	public static int[][] getConnectionTable(FFMolecule mol){
		int [][] arrConnTable = new int [mol.getAllAtoms()][mol.getAllAtoms()];
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			for (int j = 0; j < mol.getAllConnAtoms(i); j++) {
				int atConn = mol.getConnAtom(i,j);
				int bondIndex = mol.getBondBetween(i, atConn);
				int bondOrder = mol.getBondOrder(bondIndex);
				arrConnTable[i][atConn] = bondOrder;
				arrConnTable[atConn][i] = bondOrder;
			}
		}
		return arrConnTable;
	}

	
	

	/**
	 * 
	 * @param mol
	 *            the atoms in this molecule will be deleteted.
	 * @param liIndices2Delete
	 *            list with Integer as indices for the atoms we will delete.
	 */
	public static final void removeAtomsIfCarbon(FFMolecule mol, List<Integer> liIndices2Delete) {
		Collections.sort(liIndices2Delete);
		Collections.reverse(liIndices2Delete);

		// Unify atom index list.
		int ii = 0;
		while (ii < liIndices2Delete.size() - 1) {
			if (liIndices2Delete.get(ii).equals(liIndices2Delete.get(ii + 1))) {
				liIndices2Delete.remove(ii + 1);
			} else {
				ii++;
			}
		}

		// System.out.println(liIndices2Del);

		// Order is correct, because we made a sort and a reverse before.
		for (ii = 0; ii < liIndices2Delete.size(); ii++) {
			int ind = liIndices2Delete.get(ii).intValue();
			if (mol.getAtomicNo(ind) == 6)
				mol.deleteAtom(ind);
		}

	}
	
	public static final FFMolecule removeAllAtomsWithoutNeighbours(FFMolecule ffMol) {
		FFMolecule ff = new FFMolecule(ffMol);
		
		HashSet<Integer> hsAt2Del = new HashSet<Integer>();
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			if(ff.getConnAtoms(i)==0)
				hsAt2Del.add(i);
		}
		
		List<Integer> liAt2Del = new ArrayList<Integer>(hsAt2Del);
		Collections.sort(liAt2Del);
		Collections.reverse(liAt2Del);
		
		for (Integer at : liAt2Del) {
			ff.deleteAtom(at);
		}
		
		return ff;
	}

	public static final void removeAtoms(FFMolecule mol, List<Integer> liIndices2Delete) {
		Collections.sort(liIndices2Delete);
		Collections.reverse(liIndices2Delete);

		// Unify atom index list.
		int ii = 0;
		while (ii < liIndices2Delete.size() - 1) {
			if (liIndices2Delete.get(ii).equals(liIndices2Delete.get(ii + 1))) {
				liIndices2Delete.remove(ii + 1);
			} else {
				ii++;
			}
		}

		// System.out.println(liIndices2Del);

		// Order is correct, because we made a sort and a reverse before.
		for (ii = 0; ii < liIndices2Delete.size(); ii++) {
			int ind = liIndices2Delete.get(ii).intValue();
			mol.deleteAtom(ind);
		}

	}

	public static final void removeFlaggedAtoms(FFMolecule mol) {
		List<Integer> liIndices2Delete = new ArrayList<Integer>();
		
		for(int i=0; i < mol.getAllAtoms(); i++){
			if(mol.isAtomFlag(i, FFMolecule.FLAG1)) {
				liIndices2Delete.add(new Integer(i));
			}
		}
		removeAtoms(mol, liIndices2Delete);
	}

	public static final void replaceAtoms(FFMolecule ffMol, String idcode2Replace, String idcodeNewSubstructure, int [][] arrMapAtoms) {
		
		
		// StereoMolecule mol = convert2StereoMolecule(ffMol);
		
		StereoMolecule mol = ffMol.toStereoMolecule();
		
		mol.ensureHelperArrays(Molecule.cHelperRings);
		
		
		StereoMolecule query = new StereoMolecule();
		
		query.setFragment(true);
		
		
		new IDCodeParser().parse(query, idcode2Replace);
		
			
		List<int []> liAtomLists = getListFromSSSearcher(mol, query);
		
		if(liAtomLists.size() == 0) {
			Exception ex = new Exception("Substructure not found.");
			ex.printStackTrace();
			return;
		}
		System.out.println("Found: " + liAtomLists.size());

		
		// Extract substructure from molecule
		for (Iterator<int []> iter = liAtomLists.iterator(); iter.hasNext();) {
			StereoMolecule subStruc = new StereoMolecule(mol);	
			
			int [] arrAtomList = iter.next();
			
			for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
				
				boolean bInList = false;
				for (int j = 0; j < arrAtomList.length; j++) {
					if(i == arrAtomList[j]){
						subStruc.setAtomCharge(i, i);
						bInList = true;
						break;
					}
				}
				if(!bInList){
					subStruc.setAtomSelection(i, true);
				}
			}
			
			subStruc.deleteSelectedAtoms();
			
			
		}
			
		mol.deleteSelectedAtoms();
		
		// return new FFMolecule(mol);
		return;
		
	}
	public static final void formatAtomDescriptionsForViewer(FFMolecule mol, boolean showIndex) {
		
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			String sDescription = mol.getAtomDescription(i).trim();
			
			if(sDescription==null){
				sDescription="";
			}
			
			if(showIndex)
				sDescription = LEADING_SPACE_DESCRIPTION + i + ";" + sDescription;
			else
				sDescription = LEADING_SPACE_DESCRIPTION + sDescription;
			mol.setAtomDescription(i,sDescription);
		}
	}

	public static final void formatAtomDescriptionsForViewer(FFMolecule mol) {
		
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			String sDescription = mol.getAtomDescription(i);
			
			if(sDescription!=null){
				sDescription = mol.getAtomDescription(i);
				sDescription = LEADING_SPACE_DESCRIPTION + sDescription;
				mol.setAtomDescription(i,sDescription);
			}
		}
	}

	/**
	 * Generates a center atom for each atom of a terminal alkyl group -CC, -C(C)C and
	 * -C(C)(C)C. The interaction type is the core atom of the substituents.
	 * All center atoms have identical coordinates.
	 * The original index of each considered atom is written to Molecule3D.INFO_ATOMGROUP.
	 * @param mol Molecule, the atoms used for calculating the center are flagged with FLAG1
	 * @return number of terminal alkyl groups (Ethyl, Propyl, Butyl).
	 */
	public final static int calcTerminalAlkylGroupsCenter(FFMolecule mol) {

		List<List<Integer>> liEndingAlkylGroups = findTerminalAlkylGroups(mol);

		List<Integer> liAtoms2Delete = new ArrayList<Integer>();

		for (Iterator<List<Integer>> iter = liEndingAlkylGroups.iterator(); iter.hasNext();) {
			List<Integer> liIndices = (ArrayList<Integer>) iter.next();

			int[] arrIndices = ArrayUtilsCalc.toIntArray(liIndices);
			Coordinates coord = getCenterGravity(mol, arrIndices);

			for (int at = 0; at < arrIndices.length; at++) {
				// The interaction type is the core atom of the substituents.
				int iInteractionType = mol.getAtomInteractionClass(arrIndices[at]);
				String sDescription = mol.getAtomDescription(arrIndices[at]);
				// Major MM2 interaction type
				int iMM2Type = mol.getAtomMM2Class(arrIndices[at]);

				int index = mol.getAllAtoms();
				
				
				mol.addAtom(6);
				mol.setAtomInteractionClass(index, iInteractionType);
				mol.setAtomMM2Class(index, iMM2Type);
				
				String sOrigIndex = Integer.toString(arrIndices[at]);
				mol.setAtomChainId(index, sOrigIndex);

				
				if(sDescription!=null)
					mol.setAtomDescription(index, sDescription);

				mol.setCoordinates(index, coord);
				
				// Set original index
				mol.setAtomChainId(index, arrIndices[at]+"");
				
				mol.setAtomFlag(index, FLAG_CENTER_ATOM, true);
			}

			liAtoms2Delete.addAll(liIndices);
		}
		
		for (int i = 0; i < liAtoms2Delete.size(); i++) {
			int index = liAtoms2Delete.get(i).intValue();
			mol.setAtomFlag(index, FFMolecule.FLAG1, true);
		}

		return liEndingAlkylGroups.size();

	}

	/**
	 * Finds terminal alkyl groups
	 * 
	 * @param mol
	 * @return List of Lists with Integer containing the atom indices. At pos 0
	 *         is the index for the core atom given.
	 */
	private final static List<List<Integer>> findTerminalAlkylGroups(FFMolecule mol) {
		List<List<Integer>> liAlkylGroups = new ArrayList<List<Integer>>();

		Hashtable<Integer, List<Integer>> ht = new Hashtable<Integer, List<Integer>>();

		int[] arrMethylGroups = findTerminalMethylAtCarb(mol);
		for (int i = 0; i < arrMethylGroups.length; i++) {
			int nConnAts = mol.getAllConnAtoms(arrMethylGroups[i]);
			for (int j = 0; j < nConnAts; j++) {
				int indNeighborAt = mol.getConnAtom(arrMethylGroups[i], j);
				int atomicNumberNeighbor = mol.getAtomicNo(indNeighborAt);
				if (atomicNumberNeighbor > 1) {
					Integer intIndNeighborAt = new Integer(indNeighborAt);
					if (ht.containsKey(intIndNeighborAt)) {
						List<Integer> li = ht.get(intIndNeighborAt);
						li.add(new Integer(arrMethylGroups[i]));
					} else {
						List<Integer> li = new ArrayList<Integer>();
						li.add(intIndNeighborAt);
						li.add(new Integer(arrMethylGroups[i]));
						ht.put(intIndNeighborAt, li);
					}
				}
			}
		}

		for (Enumeration<List<Integer>> en = ht.elements(); en.hasMoreElements();) {
			List<Integer> element = en.nextElement();
			liAlkylGroups.add(element);
		}

		return liAlkylGroups;

	}

	/**
	 * 
	 * @param mol
	 * @return indices for all terminal methyl CH3 groups which are connected to
	 *         a carbon.
	 */
	public final static int[] findTerminalMethylAtCarb(FFMolecule mol) {

		int numAts = mol.getAllAtoms();
		int[] arrIndexHeavyNeighbor = new int[numAts];
		for (int i = 0; i < arrIndexHeavyNeighbor.length; i++) {
			arrIndexHeavyNeighbor[i] = -1;
		}

		int numMethyl = 0;
		for (int i = 0; i < numAts; i++) {
			if (mol.getAtomicNo(i) == 6) {
				int nConnAts = mol.getAllConnAtoms(i);
				int nHeavyNeighAts = 0;
				int indexNeighborAt = -1;
				for (int j = 0; j < nConnAts; j++) {
					int indAt = mol.getConnAtom(i, j);
					int atomicNumber = mol.getAtomicNo(indAt);
					if (atomicNumber > 1) {
						nHeavyNeighAts++;
						indexNeighborAt = indAt;
					}
				}

				if (nHeavyNeighAts == 1
						&& mol.getAtomicNo(indexNeighborAt) == 6) {

					arrIndexHeavyNeighbor[i] = indexNeighborAt;
					numMethyl++;
				}
			}
		}

		int[] arrIndexMethylGroups = new int[numMethyl];

		int cc = 0;
		for (int i = 0; i < arrIndexHeavyNeighbor.length; i++) {
			if (arrIndexHeavyNeighbor[i] > -1
					&& mol.getAtomicNo(arrIndexHeavyNeighbor[i]) == 6) {
				arrIndexMethylGroups[cc++] = i;
			}
		}

		return arrIndexMethylGroups;
	}

	public final static Coordinates getCenterGravity(FFMolecule mol) {
		
		int n = mol.getAllAtoms();
		
		int [] indices = new int [n];
		
		for (int i = 0; i < indices.length; i++) {
			indices[i]=i;
		}

		return getCenterGravity(mol, indices);
	}

	public final static Coordinates getCenterGravity(FFMolecule mol, int[] indices) {
		
		Coordinates c = new Coordinates();
		for (int i = 0; i < indices.length; i++) {
			c.x += mol.getAtomX(indices[i]);
			c.y += mol.getAtomY(indices[i]);
			c.z += mol.getAtomZ(indices[i]);
		}
		c.x /= indices.length;
		c.y /= indices.length;
		c.z /= indices.length;

		return c;
	}
	
	public final static FFMolecule getCentered(FFMolecule ff) {
		FFMolecule ffCent = new FFMolecule(ff);
		
		final int n = ff.getAllAtoms();
		
		double meanX = 0, meanY = 0, meanZ = 0;
		
		for (int i = 0; i < n; i++) {
			Coordinates c = ff.getCoordinates(i);
			
			meanX += c.x;
			meanY += c.y;
			meanZ += c.z;
			
		}
		
		meanX /= n;
		meanY /= n;
		meanZ /= n;
		
		for (int i = 0; i < n; i++) {
			Coordinates c = ffCent.getCoordinates(i);
			
			c.x -= meanX;
			c.y -= meanY;
			c.z -= meanZ;
		}
		
		return ffCent;
	}
	
	public final static FFMolecule getCentered(FFMolecule ff, List<Integer> liAtomIndexCenter) {
		FFMolecule ffCent = new FFMolecule(ff);
		
		double meanX = 0, meanY = 0, meanZ = 0;
		
		for (int i = 0; i < liAtomIndexCenter.size(); i++) {
			Coordinates c = ffCent.getCoordinates(liAtomIndexCenter.get(i));
			
			meanX += c.x;
			meanY += c.y;
			meanZ += c.z;
			
		}
		
		meanX /= liAtomIndexCenter.size();
		meanY /= liAtomIndexCenter.size();
		meanZ /= liAtomIndexCenter.size();
		
		for (int i = 0; i < ffCent.getAllAtoms(); i++) {
			Coordinates c = ffCent.getCoordinates(i);
			
			c.x -= meanX;
			c.y -= meanY;
			c.z -= meanZ;
		}
		
		return ffCent;
	}

	/**
	 * Topological centaer atoms are the atoms with the lowest squared sum of topological distances to all atoms.
	 * @param ff
	 * @return
	 */
	public final static List<Integer> getTopologicalCenter(int [][] arrTopoDist) {
		List<Integer> liTopoCenterAtoms = new ArrayList<Integer>();

		List<IntegerDouble> li = new ArrayList<IntegerDouble>();
		for (int i = 0; i < arrTopoDist.length; i++) {
			double sum=0;
			for (int j = 0; j < arrTopoDist.length; j++) {
				sum += arrTopoDist[i][j]*arrTopoDist[i][j];
			}
			
			li.add(new IntegerDouble(i,sum));
		}
		
		Collections.sort(li, IntegerDouble.getComparatorDouble());
		
		liTopoCenterAtoms.add(li.get(0).getInt());
		
		for (int i = 1; i < li.size(); i++) {
			if(li.get(i).getDouble()==li.get(0).getDouble()){
				liTopoCenterAtoms.add(li.get(i).getInt());
			}
		}
		
		return liTopoCenterAtoms;
	}
	
	public final static List<Integer> getTopologicalCenter(int [][] arrTopoDist, ListWithIntVec ilIndexAtoms) {
		List<Integer> liTopoCenterAtoms = new ArrayList<Integer>();

		List<IntegerDouble> li = new ArrayList<IntegerDouble>();
		
		for (int i = 0; i < ilIndexAtoms.size(); i++) {
			
			int indexAt1 = ilIndexAtoms.get(i);
			
			double sum=0;
			for (int j = 0; j < ilIndexAtoms.size(); j++) {
				
				int indexAt2 = ilIndexAtoms.get(j);
				
				sum += arrTopoDist[indexAt1][indexAt2]*arrTopoDist[indexAt1][indexAt2];
			}
			
			li.add(new IntegerDouble(indexAt1,sum));
		}
		
		Collections.sort(li, IntegerDouble.getComparatorDouble());
		
		liTopoCenterAtoms.add(li.get(0).getInt());
		
		for (int i = 1; i < li.size(); i++) {
			if(li.get(i).getDouble()==li.get(0).getDouble()){
				liTopoCenterAtoms.add(li.get(i).getInt());
			}
		}
		
		return liTopoCenterAtoms;
	}

	/**
	 * Gets the points with the maximum sum of squared topological distances to all atoms.
	 * @param ff
	 * @param arrTopoDist
	 * @return
	 */
	public final static List<Integer> getPheriphericPoints(int [][] arrTopoDist) {
		
		List<Integer> liTopoCenterAtoms = new ArrayList<Integer>();

		List<IntegerDouble> li = new ArrayList<IntegerDouble>();
		for (int i = 0; i < arrTopoDist.length; i++) {
			double sum=0;
			for (int j = 0; j < arrTopoDist.length; j++) {
				sum += arrTopoDist[i][j]*arrTopoDist[i][j];
			}
			
			li.add(new IntegerDouble(i,sum));
		}
		
		Collections.sort(li, IntegerDouble.getComparatorDouble());
		
		Collections.reverse(li);
		
		liTopoCenterAtoms.add(li.get(0).getInt());
		
		for (int i = 1; i < li.size(); i++) {
			if(li.get(i).getDouble()==li.get(0).getDouble()){
				liTopoCenterAtoms.add(li.get(i).getInt());
			}
		}
		
		return liTopoCenterAtoms;
	}

	public final static int [][] getTopologicalDistanceMatrix(FFMolecule mol) {
		
		return StructureCalculator.getNumberOfBondsBetweenAtoms(mol, mol.getAllBonds(), null);
		
	}
	
	public final static List<Integer> getLongestChain(FFMolecule ff) {
		
		int [][] arrTopoDist = FFMoleculeFunctions.getTopologicalDistanceMatrix(ff);
		
		int maxDist=0;
		
		int indexAt=-1;
		for (int i = 0; i < arrTopoDist.length; i++) {
			// We will not start at Hydrogen or electron pairs.
			if(ff.getAtomicNo(i)<2)
				continue;
			
			for (int j = i+1; j < arrTopoDist.length; j++) {
				if(arrTopoDist[i][j]>maxDist){
					maxDist=arrTopoDist[i][j];
					indexAt=i;
				}
			}
		}
		
		List<Integer> liLongestChain = getLongestChain(ff, indexAt);

		return liLongestChain;
	}
	
	/**
	 * User object in DefaultMutableTreeNode is the index of the atom. Root contains indexAtomStart.
	 * @param ff
	 * @param indexAtomStart
	 * @return
	 */
	public final static DefaultMutableTreeNode getTreeFromBroadFirstSearch(FFMolecule ff, int indexAtomStart){
		
		HashSet<Integer> hsVisited = new HashSet<Integer>();
		
		hsVisited.add(indexAtomStart);
		List<DefaultMutableTreeNode> liQueue = new ArrayList<DefaultMutableTreeNode>();
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		
		root.setUserObject(indexAtomStart);
		
		liQueue.add(root);
		
		while(!liQueue.isEmpty()){
			DefaultMutableTreeNode parent = liQueue.remove(0);
			
			int indexAtom = (Integer)parent.getUserObject();
			
			int nConnceted = ff.getConnAtoms(indexAtom);
			
			for (int i = 0; i < nConnceted; i++) {
				int indexAtomConnceted = ff.getConnAtom(indexAtom, i);
				
				if(ff.getAtomicNo(indexAtomConnceted)<2)
					continue;
				
				if(!hsVisited.contains(indexAtomConnceted)) {
					
					hsVisited.add(indexAtomConnceted);
					
					DefaultMutableTreeNode child = new DefaultMutableTreeNode();
					
					child.setUserObject(indexAtomConnceted);
					
					liQueue.add(child);
					
					parent.add(child);
					
				}
			}
		}
		
		return root;
	}

	/**
	 * Get all possible paths. Only crossing is not allowed.
	 * @param ff
	 * @param indexAtomStart
	 * @return
	 */
	public final static DefaultMutableTreeNode getTreeFromComplete(FFMolecule ff, int indexAtomStart){
		
		List<DefaultMutableTreeNode> liQueue = new ArrayList<DefaultMutableTreeNode>();
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		
		root.setUserObject(indexAtomStart);
		
		liQueue.add(root);
		
		while(!liQueue.isEmpty()){
			DefaultMutableTreeNode parent = liQueue.remove(0);
			
			int indexAtom = (Integer)parent.getUserObject();
			
			int nConnceted = ff.getConnAtoms(indexAtom);
			
			for (int i = 0; i < nConnceted; i++) {
				int indexAtomConnceted = ff.getConnAtom(indexAtom, i);
				
				if(ff.getAtomicNo(indexAtomConnceted)<2)
					continue;
				
				// Check for crossing
				if(!isInPath(parent, indexAtomConnceted)) {
					
					DefaultMutableTreeNode child = new DefaultMutableTreeNode();
					
					child.setUserObject(indexAtomConnceted);
					
					liQueue.add(child);
					
					parent.add(child);
					
				}
			}
		}
		
		return root;
	}
	
	private static final boolean isInPath(DefaultMutableTreeNode parent, int indexAtom){
		boolean inPath=false;
		
		TreeNode [] arrPath = parent.getPath();
		
		for (int i = 0; i < arrPath.length; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)arrPath[i];
			
			int indexAtomNode = (Integer)node.getUserObject();
			
			if(indexAtomNode==indexAtom){
				inPath=true;
				break;
			}
			
		}
		
		return inPath;
	}
	
	
	@SuppressWarnings("unchecked")
	public final static int [] getPath(FFMolecule ff, int indexAt1, int indexAt2){
		
		int [] arrPath = null;
		
		DefaultMutableTreeNode root = getTreeFromBroadFirstSearch(ff, indexAt1);
		
		Enumeration<DefaultMutableTreeNode> en = root.breadthFirstEnumeration();
		
		for(;en.hasMoreElements();){
			DefaultMutableTreeNode node = en.nextElement();
			
			int indexAtomNode = (Integer)node.getUserObject();
			
			if(indexAt2==indexAtomNode) {
				TreeNode [] arrPathNodes = node.getPath();
				
				arrPath = new int [arrPathNodes.length];
				
				for (int i = 0; i < arrPathNodes.length; i++) {
					arrPath[i]=(Integer)((DefaultMutableTreeNode)arrPathNodes[i]).getUserObject();
				}
			}
			
		}
		
		return arrPath;
	}

	
	/**
	 * Returns the atom indices as layers of the start atom. First layer is indexAtomStart.
	 * @param ff
	 * @param indexAtomStart
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public final static List<List<Integer>> getLayersFromBroadFirstSearch(FFMolecule ff, int indexAtomStart){
		
		DefaultMutableTreeNode root = FFMoleculeFunctions.getTreeFromBroadFirstSearch(ff, indexAtomStart);
		
		Enumeration<DefaultMutableTreeNode> en = (Enumeration<DefaultMutableTreeNode>)root.breadthFirstEnumeration();
		
		List<List<Integer>> liliIndexAtomLayer = new ArrayList<List<Integer>>();
		
		List<Integer> liIndexAtomLayer = new ArrayList<Integer>();
		
		int level = 0;
		while(en.hasMoreElements()){
			DefaultMutableTreeNode node = en.nextElement();
			
			if(node.getLevel()>level){
				liliIndexAtomLayer.add(liIndexAtomLayer);
				liIndexAtomLayer = new ArrayList<Integer>();
				level++;
			}
			liIndexAtomLayer.add((Integer)node.getUserObject());
		}
		
		if(liIndexAtomLayer.size()>0){
			liliIndexAtomLayer.add(liIndexAtomLayer);
		}
		
		return liliIndexAtomLayer;
	}

	public final static List<Integer> getLongestChain(FFMolecule ff, int indexAtomStart) {
		
		HashSet<Integer> hsVisited = new HashSet<Integer>();
		
		hsVisited.add(indexAtomStart);
		
		List<DefaultMutableTreeNode> liQueue = new ArrayList<DefaultMutableTreeNode>();
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		
		root.setUserObject(indexAtomStart);
		
		liQueue.add(root);
		
		DefaultMutableTreeNode nodeDeepest =  root;
		
		while(!liQueue.isEmpty()){
			DefaultMutableTreeNode parent = liQueue.remove(0);
			
			int indexAtom = (Integer)parent.getUserObject();
			
			int nConnceted = ff.getConnAtoms(indexAtom);
			
			for (int i = 0; i < nConnceted; i++) {
				int indexAtomConnceted = ff.getConnAtom(indexAtom, i);
				
				if(ff.getAtomicNo(indexAtomConnceted)<2)
					continue;
				
				if(!hsVisited.contains(indexAtomConnceted)) {
					
					hsVisited.add(indexAtomConnceted);
					
					DefaultMutableTreeNode child = new DefaultMutableTreeNode();
					
					child.setUserObject(indexAtomConnceted);
					
					liQueue.add(child);
					
					parent.add(child);
					
					if(child.getLevel()>nodeDeepest.getLevel()){
						
						nodeDeepest = child;
					}
				}
			}
		}
		
		TreeNode [] tnPath = nodeDeepest.getPath();
		
		List<Integer> liChain = new ArrayList<Integer>();
		for (int i = 0; i < tnPath.length; i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)tnPath[i];
			
			liChain.add((Integer)node.getUserObject());
		}
		
		return liChain;
		
	}
	
	
	
	public final static String toString(FFMolecule mol){
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			sb.append(mol.getAtomicNo(i));
			sb.append(":");
			sb.append(NF.format(mol.getAtomX(i)));
			sb.append(",");
			sb.append(NF.format(mol.getAtomY(i)));
			sb.append(",");
			sb.append(NF.format(mol.getAtomZ(i)));
			sb.append("\n");
		}
		
		
		return sb.toString();
	}
	
	/**
	 * Deprotonates and decreases charge by 1.
	 * @param ff
	 * @param indexAtomReactiveCenter
	 * @return null if no hydrogen is attached to indexAtomReactiveCenter.
	 */
	public static FFMolecule getDeprotonated(FFMolecule ff, int indexAtomReactiveCenter){
		
		int indexProtonAcid = -1;
		
		int nConnected2Donor = ff.getAllConnAtoms(indexAtomReactiveCenter);
		
		for (int i = 0; i < nConnected2Donor; i++) {
			int indexAtomConn = ff.getConnAtom(indexAtomReactiveCenter, i);
			
			if(ff.getAtomicNo(indexAtomConn)==1){
				indexProtonAcid = indexAtomConn;
				break;
			}
		}
		
		if(indexProtonAcid==-1){
			return null;
		}
		
		FFMolecule ffDeprotonated = new FFMolecule(ff);
		
		ffDeprotonated.deleteAtom(indexProtonAcid);
		
		int charge = ffDeprotonated.getAtomCharge(indexAtomReactiveCenter) - 1;
		
		ffDeprotonated.setAtomCharge(indexAtomReactiveCenter, charge);
		
		return ffDeprotonated;
	}
	
	/**
	 * Protonates and increases charge by 1.
	 * @param ff
	 * @param indexAtomBase
	 * @return
	 */
	public static FFMolecule getProtonated(FFMolecule ff, int indexAtomReactiveCenter){
		
		FFMolecule ffProtonated = new FFMolecule(ff);
		
		int atomicNo = ff.getAtomicNo(indexAtomReactiveCenter);
		
		int connAtoms = ff.getConnAtoms(indexAtomReactiveCenter);
		
		int sumBondOrder = 0;
		for (int i = 0; i < connAtoms; i++) {
			sumBondOrder += ff.getConnBondOrder(indexAtomReactiveCenter, i);
		}
		
		if(atomicNo == 6){
			if(sumBondOrder == 4){ // C
				return null;
			}
		} else if(atomicNo == 7){ // N
			if(sumBondOrder == 4){
				return null;
			}
		} else if(atomicNo == 8){ // O
			if(sumBondOrder == 3){
				return null;
			}
		}
		
		int indexProton = ffProtonated.addAtom(1);
		
		int charge = ffProtonated.getAtomCharge(indexAtomReactiveCenter) + 1;
		
		ffProtonated.setAtomCharge(indexAtomReactiveCenter, charge);
		
		ffProtonated.addBond(indexAtomReactiveCenter, indexProton, 1);
		
		return ffProtonated;
	}

	public static FFMolecule randomizeAtoms(FFMolecule ff){
		
		
		FFMolecule ffPure = new FFMolecule(ff);
		
		removeElectronPairs(ffPure);
		
		List<Integer> liAtomIndex = new ArrayList<Integer>();
		
		for (int i = 0; i < ffPure.getAllAtoms(); i++) {
			liAtomIndex.add(i);
		}
		
		Collections.shuffle(liAtomIndex);
		
		int [] atomMapRND2Mol = ArrayUtilsCalc.toIntArray(liAtomIndex);
		int [] atomMapMol2RNDl = new int [ffPure.getAllAtoms()];
		
		FFMolecule ffRND = new FFMolecule(ffPure.getAllAtoms(), ffPure.getAllBonds());
		
		
		for (int i = 0; i < atomMapRND2Mol.length; i++) {
			
			int indexAtSource = atomMapRND2Mol[i];
			
			int indexAtRND = ffRND.addAtom(ffPure.getAtomicNo(indexAtSource));
			
			Coordinates c = ffPure.getCoordinates(indexAtSource);
			
			ffRND.setCoordinates(indexAtRND, c);
			
			atomMapMol2RNDl[indexAtSource]=indexAtRND;
		}
		
		int nBonds = ffPure.getAllBonds();
		
		for (int i = 0; i < nBonds; i++) {
			
			int indexAt1 = ffPure.getBondAtom(0, i);
			int indexAt2 = ffPure.getBondAtom(1, i);
			
			int indexAtRND1 = atomMapMol2RNDl[indexAt1];
			
			int indexAtRND2 = atomMapMol2RNDl[indexAt2];
			
			int order = ffPure.getBondOrder(i);
			
			ffRND.setBondAtom(0, i, indexAtRND1);
			ffRND.setBondAtom(1, i, indexAtRND2);
			
			ffRND.addBond(indexAtRND1, indexAtRND2, order);
			
		}
		
		return ffRND;
	}

	/**
	 * The x axis will become the main axis (first principal component).
	 * Don't forget to center ffIn, if necessary.
	 * @param ffIn
	 * @param liAtomIndex the principal axis will be calculated from these atoms.
	 * @return
	 */
	public static FFMolecule transferMolecule2PricipalAxis(FFMolecule ffIn, List<Integer> liAtomIndex){
		
		FFMolecule ffPrincipal = new FFMolecule(ffIn);
		
		Matrix X = new Matrix(liAtomIndex.size(), COLS);
		
		for (int i = 0; i < liAtomIndex.size(); i++) {
			
			Coordinates c = ffPrincipal.getCoordinates(liAtomIndex.get(i));
			
			X.set(i, 0, c.x);
			X.set(i, 1, c.y);
			X.set(i, 2, c.z);
			
		}
		
		PCA pca = new PCA(X, PRINCIPAL_COMPONENTS);
		
		Matrix maEV = pca.getEigenVectorsLeft();
		
		for (int i = 0; i < ffPrincipal.getAllAtoms(); i++) {
			Coordinates c = ffPrincipal.getCoordinates(i);
			
			Matrix vecAtom = new Matrix(3,1);
			
			vecAtom.set(0, 0, c.x);
			vecAtom.set(1, 0, c.y);
			vecAtom.set(2, 0, c.z);
			
			Matrix vecAtomNew = maEV.multiply(true, false, vecAtom);
			
			double x = vecAtomNew.get(0, 0);
			double y = vecAtomNew.get(1, 0);
			double z = vecAtomNew.get(2, 0);
			Coordinates cNew = new Coordinates(x, y, z);
			ffPrincipal.setCoordinates(i, cNew);
			
		}
		
		return ffPrincipal;
	}

	
	
	
}
