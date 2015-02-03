package com.actelion.research.chem.descriptor.flexophore.generator;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.calc.MatrixFunctions;
import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.descriptor.flexophore.MolDistHist;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistViz;
import com.actelion.research.chem.descriptor.flexophore.PPNodeViz;

/**
 * 
 * CGMult
 * Object containing pharmacophore nodes and a list of distance tables. 
 * The distnace tables represent the distances from the different conformations.
 * 
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2004 MvK: Start implementation
 */
public class CGMult {
	
	public static final String SEPERATOR_NO_ATOMS = ";";
	private static final DecimalFormat FORMAT_DISTANES = new DecimalFormat("0.00000"); 
	
	public static final int BINS_HISTOGRAM = 40;
	
	/**
	 * Range histogram in Angstrom.
	 */
	public static final int RANGE_HISTOGRAM = 20;

	
	private List<PPNodeViz> liPPNode;
	
	private List<double[][]> liDistTable;   
	
	private FFMolecule ffMol;

	/**
	 * 
	 * @param cg 
	 * @param ff FFMolecule for visualization.
	 */
	public CGMult(CompleteGraph cg, FFMolecule ff) {
		init();
		
		for (int i = 0; i < cg.getAllNodes(); i++) {
			liPPNode.add(new PPNodeViz(cg.getPPNode(i)));	
		}
		
		liDistTable  = new ArrayList<double[][]>();
		liDistTable.add(cg.getEdges());
		
		ffMol = new FFMolecule(ff);
	}

	/**
	 * Checks for the same number of nodes and for the same atom types in each node.
	 * Only the distances (edges) are added to the CGMult instance. 
	 * @param cg complete graph with nodes in the same order as the nodes in CGMult.
	 * @throws Exception
	 */
	public void add(CompleteGraph cg) throws Exception {
		
		if(getAllNodes() != cg.getAllNodes()) {
			throw new Exception("Number of atoms differs: " + getAllNodes() + " and " + cg.getAllNodes() + ".");
		}
		
		for (int i = 0; i < getAllNodes(); i++) {
			if(!getPPNode(i).equalAtoms(cg.getPPNode(i))) {
				throw new Exception("Node type differs.");
			} 
			
		}
		
		liDistTable.add(cg.getEdges());
		
	}
	
	/**
	 * Generates the MolDistHist (Flexophore) descriptor from the conformations.
	 * The Alkane clusters are summarized.
	 * The index tables are not created!
	 * @return
	 */
	public MolDistHist getMolDistHist() {
		
		MolDistHistViz mdhv = getMolDistHistViz();
		
		return mdhv.getMolDistHist();
	}
	
	/**
	 * the only difference to getMolDistHist() is the usage of PPNodeViz for the MolDistHist object.
	 * In contrary to getMolDistHistVizFineGranulated() alkene clusters are summarised.
	 * @return
	 */
	public MolDistHistViz getMolDistHistViz() {
		
		MolDistHistViz mdhv = getMolDistHistVizFineGranulated();
		
		mdhv = MolDistHistViz.summarizeAlkaneCluster(mdhv, MolDistHist.MAX_DIST_CLUSTER);
		
		if(!mdhv.check()){
			return null;
		}
		
		return mdhv;
	}
	
	public MolDistHistViz getMolDistHistVizFineGranulated() {
		
		int numNodes = getAllNodes();
		MolDistHistViz mdhv = new MolDistHistViz(numNodes, ffMol);
		
		double min =0;
		double max = RANGE_HISTOGRAM; 
		int bins = BINS_HISTOGRAM;
		
		for (int i = 0; i < numNodes; i++) {
			mdhv.addNode(getPPNode(i));
		}
		
		for (int indexNode1 = 0; indexNode1 < numNodes; indexNode1++) {
			for (int indexNode2 = indexNode1+1; indexNode2 < numNodes; indexNode2++) {
				double [] arrDists = new double [liDistTable.size()];
				int cc = 0;
				
				for (int i = 0; i < liDistTable.size(); i++) {
					double[][] arrDistTbl = liDistTable.get(i);
					arrDists[cc++]=arrDistTbl[indexNode1][indexNode2];
				}
				
				Matrix maDist = new Matrix(true, arrDists);
				Matrix maBins = MatrixFunctions.getHistogramBins(min,max, bins);
				Matrix maHist =  MatrixFunctions.getHistogram(maDist, maBins);
				
				byte [] arrHist = new byte [maHist.getColDim()];
				for (int k = 0; k < arrHist.length; k++) {
					arrHist[k]= (byte)maHist.get(2,k);
				}
				mdhv.setDistHist(indexNode1,indexNode2,arrHist);
			}
		}
		
		mdhv.setDistanceTables(liDistTable);
		
		// mdhv.blurrSingleBinHistograms();
		
		mdhv.realize();
		
		if(!mdhv.check()){
			return null;
		}
		
		return mdhv;
	}

	
	
	public int getNumConformations(){
		return liDistTable.size();
	}
	
	public final PPNodeViz getPPNode(int i) {
		return liPPNode.get(i);
	}
	public int getAllNodes(){
		return liPPNode.size();
	}
	
	
	private void init(){
		liPPNode = new ArrayList<PPNodeViz>();
		liDistTable = new ArrayList<double[][]>();
	}
	
	public String toString() {
		StringBuffer str = new StringBuffer();

		str.append(getAllNodes() + SEPERATOR_NO_ATOMS);
		
		for (int i = 0; i < getAllNodes(); i++) 
			str.append(getPPNode(i).toString() + " "); 
		
		
		for (int i = 0; i < liDistTable.size(); i++)
			str.append(toStringDistances(i));
		
		return str.toString();
	}
	
	public String toStringDistances(int iConformation) {
		StringBuffer str = new StringBuffer();
		int nNodes = getAllNodes();
		double [][] edges = (double[][])liDistTable.get(iConformation);
		
		for (int i = 0; i < nNodes; i++) {
			for (int j = i + 1; j < nNodes; j++) {
				str.append(FORMAT_DISTANES.format(edges[i][j]) + " "); 
			}
		}
		return str.toString();
	}

	
}
