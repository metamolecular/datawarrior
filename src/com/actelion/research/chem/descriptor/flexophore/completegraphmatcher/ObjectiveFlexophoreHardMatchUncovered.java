package com.actelion.research.chem.descriptor.flexophore.completegraphmatcher;

import java.util.Arrays;

import com.actelion.research.calc.Matrix;
import com.actelion.research.calc.graph.MinimumSpanningTree;
import com.actelion.research.chem.descriptor.flexophore.DistHist;
import com.actelion.research.chem.descriptor.flexophore.IMolDistHist;
import com.actelion.research.chem.descriptor.flexophore.MolDistHistViz;
import com.actelion.research.chem.descriptor.flexophore.PPNode;
import com.actelion.research.chem.descriptor.flexophore.generator.CGMult;
import com.actelion.research.chem.descriptor.sphere.ScaleClasses;
import com.actelion.research.util.Formatter;
import com.actelion.research.util.graph.complete.IObjectiveCompleteGraph;
import com.actelion.research.util.graph.complete.SolutionCompleteGraph;

/**
 * 
 * 
 * ObjectiveCGMolDistHistViz
 * The weighting of the coverage is hard. Which means that uncovered nodes 
 * are strongly change the final similarity score.
 * look in <code>getScoreUncoveredNearestNodesBase(SolutionCompleteGraph solution)</code> 
 * and <code>getScoreUncoveredNearestNodesQuery(SolutionCompleteGraph solution)</code>.
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Oct 2, 2012 MvK: Start implementation
 */
public class ObjectiveFlexophoreHardMatchUncovered implements IObjectiveCompleteGraph<IMolDistHist>{

	public static final int MAX_NUM_NODES_FLEXOPHORE = 64;
	
	// Default 0.3 
	final static double THRESH_NODE_SIMILARITY	= 0.3;
	
	// Default 0.05
	private final static double THRESH_HISTOGRAM_SIMILARITY	= 0.75;

	private static final float INIT_VAL = -1;

	
	private IMolDistHist cgBase;
	
	private IMolDistHist cgQuery;
	
	private int nodesBase;
	
	private int nodesQuery;
	
	private byte [] arrTmpHist;
	
	private boolean queryBias;
	
	private boolean validHelpersQuery;
	
	private boolean validHelpersBase;
	
	private boolean resetSimilarityArrays;
	
	private double threshNodeMinSimilarity;
	
	private double threshHistogramSimilarity;

	private HistogramMatchCalculator histogramMatchCalculator;
	
	// private PPNodeSimilarityOptimistic nodeSimilarity;
	// private PPNodeSimilarityMedian nodeSimilarity;
	private PPNodeSimilarityMultiplicative nodeSimilarity;
	
	private ScaleClasses scaleClassesSimilarityHistogram;
	
	private ScaleClasses scaleClassesSimilarityNodes;
	
	private ScaleClasses scaleClassesFinalSimilarity;
	
	private float [][] arrSimilarityNodes;
	
	private float [][] arrSimilarityHistograms;
	
	private double [][] arrRelativeDistanceMatrixQuery;
	
	private double [][] arrRelativeDistanceMatrixBase;
	
	private Matrix maHelperAdjacencyQuery;
	
	private Matrix maHelperAdjacencyBase;
	
	private double sumDistanceMinSpanTreeQuery;
	
	private double sumDistanceMinSpanTreeBase;

	private int numInevitablePPPoints;
	
	private double avrPairwiseMappingScaled;
	
	private double coverageQuery;
	
	private double coverageBase;
	
	private double similarity;
	
	private double similarityScaled;
	
	
	public ObjectiveFlexophoreHardMatchUncovered() {
		
		arrTmpHist =  new byte[CGMult.BINS_HISTOGRAM];
		
		histogramMatchCalculator = new HistogramMatchCalculator();
		
		// nodeSimilarity = new PPNodeSimilarityOptimistic();
		// nodeSimilarity = new PPNodeSimilarityMedian();
		nodeSimilarity = new PPNodeSimilarityMultiplicative();
		
		/**
		 * The scaling could be scaleClassesSimilarityHistogram.add(0.0, 1.0, 0.5, 1.0) 
		 * because a minimum similarity is already given in <code>THRESH_HISTOGRAM_SIMILARITY</code>
		 */
		scaleClassesSimilarityHistogram = new ScaleClasses();
		scaleClassesSimilarityHistogram.add(0.05, 1.0, 0.2, 1.0);

		scaleClassesSimilarityNodes = new ScaleClasses();
		scaleClassesSimilarityNodes.add(0.0, 1.0, 0.0, 1.0);
		
		scaleClassesFinalSimilarity = new ScaleClasses();
		scaleClassesFinalSimilarity.add(0.0, 0.1, 0.0, 0.5);
		scaleClassesFinalSimilarity.add(0.1, 0.5, 0.5, 0.8);
		scaleClassesFinalSimilarity.add(0.5, 0.7, 0.8, 0.9);
		scaleClassesFinalSimilarity.add(0.7, 1.0, 0.9, 1.0);
		
		threshNodeMinSimilarity = THRESH_NODE_SIMILARITY;
		
		threshHistogramSimilarity = THRESH_HISTOGRAM_SIMILARITY;
		
		initSimilarityMatrices();
	}
	
	private void initSimilarityMatrices(){
		
		arrSimilarityNodes = new float [MAX_NUM_NODES_FLEXOPHORE][];
		for (int i = 0; i < MAX_NUM_NODES_FLEXOPHORE; i++) {
			arrSimilarityNodes[i] = new float [MAX_NUM_NODES_FLEXOPHORE];
			Arrays.fill(arrSimilarityNodes[i], INIT_VAL);
			
		}

		int maxNumHistograms = ((MAX_NUM_NODES_FLEXOPHORE*MAX_NUM_NODES_FLEXOPHORE)-MAX_NUM_NODES_FLEXOPHORE)/2;
		
		arrSimilarityHistograms = new float [maxNumHistograms][];
		for (int i = 0; i < maxNumHistograms; i++) {
			arrSimilarityHistograms[i] = new float [maxNumHistograms];
			Arrays.fill(arrSimilarityHistograms[i], INIT_VAL);
		}
	}
	
	private void resetSimilarityMatrices(){
		
		for (int i = 0; i < nodesQuery; i++) {
			for (int j = 0; j < nodesBase; j++) {
				arrSimilarityNodes[i][j] = INIT_VAL;	
			}
		}

		int numHistogramsQuery = ((nodesQuery*nodesQuery)-nodesQuery)/2;
		
		int numHistogramsBase = ((nodesBase*nodesBase)-nodesBase)/2;
		
		for (int i = 0; i < numHistogramsQuery; i++) {
			for (int j = 0; j < numHistogramsBase; j++) {
				arrSimilarityHistograms[i][j] = INIT_VAL;	
			}
		}
		
		resetSimilarityArrays = false;
	}
	
	/**
	 * If a single histogram is not matching the solution is invalid.
	 * 
	 * If at least one node is not matching the solution is invalid.
	 */
	public boolean isValidSolution(SolutionCompleteGraph solution) {
		
		boolean mapping = true;
		
		if(!validHelpersQuery){
			calculateHelpersQuery();
		}
		
		if(!validHelpersBase){
			calculateHelpersBase();
		}
		
		
		if(resetSimilarityArrays){
			resetSimilarityMatrices();
		}
		
		// 
		// Should contain at least one pppoint with a hetero atom.
		// 
		
		int heap = solution.getSizeHeap();
		
		
		//
		// Check for inevitable pharmacophore points.
		//
		if(numInevitablePPPoints > 0) {
			
			int ccInevitablePPPointsInSolution = 0;
			
			for (int i = 0; i < heap; i++) {
				int indexNodeQuery = solution.getIndexQueryFromHeap(i);
				
				if(cgQuery.isInevitablePharmacophorePoint(indexNodeQuery)){
					ccInevitablePPPointsInSolution++;
				}
			}
			
			int neededMinInevitablePPPoints = Math.min(heap, numInevitablePPPoints);
			
			if(ccInevitablePPPointsInSolution < neededMinInevitablePPPoints){
				mapping = false;
			}
			
		}

		//
		// Check for one hetero atom in solution
		//
		if(mapping){
			
			boolean heteroNodeQuery = false;
			
			boolean heteroNodeBase = false;

			for (int i = 0; i < heap; i++) {
				
				int indexNodeQuery = solution.getIndexQueryFromHeap(i);
				PPNode nodeQuery = cgQuery.getNode(indexNodeQuery);
				if(nodeQuery.hasHeteroAtom()){
					heteroNodeQuery = true;
				}
				
				int indexNodeBase = solution.getIndexCorrespondingBaseNode(indexNodeQuery);
				
				PPNode nodeBase = cgBase.getNode(indexNodeBase);
				if(nodeBase.hasHeteroAtom()){
					heteroNodeBase = true;
				}
			}
			
			if(!heteroNodeQuery || !heteroNodeBase) {
				mapping = false;
			}
		}
		
		
		//
		// Check for matching nodes.
		//
		if(mapping){
			for (int i = 0; i < heap; i++) {
				
				int indexNodeQuery = solution.getIndexQueryFromHeap(i);
				
				int indexNodeBase = solution.getIndexCorrespondingBaseNode(indexNodeQuery);
				
				if(!areNodesMapping(indexNodeQuery, indexNodeBase)) {
					mapping = false;
					break;
				}
			}
		}
		
		//
		// Check for matching histograms.
		//
		if(mapping){
			outer:
			for (int i = 0; i < heap; i++) {
				
				int indexNode1Query = solution.getIndexQueryFromHeap(i);
				
				int indexNode1Base = solution.getIndexCorrespondingBaseNode(indexNode1Query);
				
				for (int j = i+1; j < heap; j++) {
					int indexNode2Query = solution.getIndexQueryFromHeap(j);
					
					int indexNode2Base = solution.getIndexCorrespondingBaseNode(indexNode2Query);
					
					if(!areHistogramsMapping(indexNode1Query, indexNode2Query, indexNode1Base, indexNode2Base)){
						mapping = false;
						break outer;
					}
	
				}
			}
		}
		
		return mapping;
		
	}

	public boolean areNodesMapping(int indexNodeQuery, int indexNodeBase) {
		
		if(!validHelpersQuery){
			calculateHelpersQuery();
		}
		
		if(!validHelpersBase){
			calculateHelpersBase();
		}
		
		if(resetSimilarityArrays){
			resetSimilarityMatrices();
		}
		
		
		boolean match = true;

		double simNodes = getSimilarityNodes(indexNodeQuery, indexNodeBase);
		
		if(simNodes < threshNodeMinSimilarity){
			match=false;
		}
		
		return match;
	}
	
	private boolean areHistogramsMapping(int indexNode1Query, int indexNode2Query, int indexNode1Base, int indexNode2Base) {
		
		boolean match = true;

		double simHistograms = getSimilarityHistogram(indexNode1Query, indexNode2Query, indexNode1Base, indexNode2Base);
		
		if(simHistograms < threshHistogramSimilarity){
			match=false;
		}
		
		return match;
	}

	public float getSimilarity(SolutionCompleteGraph solution) {

		if(!validHelpersQuery){
			calculateHelpersQuery();
		}
		
		if(!validHelpersBase){
			calculateHelpersBase();
		}

		if(resetSimilarityArrays){
			resetSimilarityMatrices();
		}

		
		int heap = solution.getSizeHeap();

		double sumPairwiseMapping = 0;
		
		for (int i = 0; i < heap; i++) {
			
			int indexNode1Query = solution.getIndexQueryFromHeap(i);
			
			int indexNode1Base = solution.getIndexCorrespondingBaseNode(indexNode1Query);
			
			for (int j = i+1; j < heap; j++) {
				int indexNode2Query = solution.getIndexQueryFromHeap(j);
				
				int indexNode2Base = solution.getIndexCorrespondingBaseNode(indexNode2Query);
				
				sumPairwiseMapping += getScorePairwiseMapping(indexNode1Query, indexNode2Query, indexNode1Base, indexNode2Base);

			}
		}
		
		double mappings = ((heap * heap)-heap) / 2.0;
	
		avrPairwiseMappingScaled = sumPairwiseMapping/mappings;
				
		coverageQuery = getRatioMinimumSpanningTreeQuery(solution);
		
		coverageBase = getRatioMinimumSpanningTreeBase(solution);
		
		double coverage = coverageQuery * coverageBase;
				
		double nodesQuerySq = nodesQuery * nodesQuery;
		
		double nodesBaseSq = nodesBase * nodesBase;
		
		double ratioNodes = Math.min(nodesQuerySq, nodesBaseSq) / Math.max(nodesQuerySq, nodesBaseSq);

		
		if(queryBias) {
			
			coverage = coverageQuery;
			
			ratioNodes = 1;
						
			if(nodesQuery > nodesBase){
				
				ratioNodes = nodesBaseSq / nodesQuerySq;
				
			}
			
		}
					
		similarity = avrPairwiseMappingScaled * coverage * ratioNodes;
				
		return (float)similarity;
	}



	/**
	 * Sets the color information for the visualization of the Flexophore PPPoints.
	 * Call before visualization. Method sets identical info values for corresponding nodes.
	 * @param solution
	 */
	public void setMatchingInfoInQueryAndBase(SolutionCompleteGraph solution){
		
		MolDistHistViz mdhvQuery = null; 
		MolDistHistViz mdhvBase = null; 
		
		if(cgQuery instanceof MolDistHistViz) {
			mdhvQuery = (MolDistHistViz)cgQuery;
		} else {
			throw new RuntimeException("Query has to be of type MolDistHistViz for visualization.");
		}
		
		if(cgBase instanceof MolDistHistViz) {
			mdhvBase = (MolDistHistViz)cgBase;
		} else {
			throw new RuntimeException("Base has to be of type MolDistHistViz for visualization.");
		}
		
		mdhvQuery.resetInfoColor();
		
		mdhvBase.resetInfoColor();
		
		int heap = solution.getSizeHeap();
		
		for (int i = 0; i < heap; i++) {
			
			int indexNodeQuery = solution.getIndexQueryFromHeap(i);
			
			int indexNodeBase = solution.getIndexCorrespondingBaseNode(indexNodeQuery);
			
			
			double similarityMappingNodes = getSimilarityNodes(indexNodeQuery, indexNodeBase);
//			
//			System.out.println(i + " Query " + indexNodeQuery + " " + cgQuery.getNode(indexNodeQuery).toStringLong() + "; base " + indexNodeBase + " "+ cgBase.getNode(indexNodeBase).toStringLong() + " similarity " + Formatter.format3(simNode));
			
			
			mdhvQuery.setSimilarityMappingNodes(indexNodeQuery, (float)similarityMappingNodes);
						
			mdhvQuery.setMappingIndex(indexNodeQuery, i);
			
			
			mdhvBase.setMappingIndex(indexNodeBase, i);
			
			mdhvBase.setSimilarityMappingNodes(indexNodeBase, (float)similarityMappingNodes);
		}

	}
	
	
	public IMolDistHist getBase() {
		return cgBase;
	}

	public IMolDistHist getQuery() {
		return cgQuery;
	}

	public void setBase(IMolDistHist cgBase) {
		
		this.cgBase = cgBase;
		
		nodesBase = cgBase.getNumPPNodes();
		
		validHelpersBase = false;
		
		resetSimilarityArrays = true;
	}
	
	/**
	 * @param queryBias the queryBias to set
	 */
	public void setQueryBias(boolean queryBias) {
		this.queryBias = queryBias;
	}
	
	public void setQuery(IMolDistHist cgQuery) {
		
		this.cgQuery = cgQuery;
		
		nodesQuery = cgQuery.getNumPPNodes();
		
		numInevitablePPPoints = cgQuery.getNumInevitablePharmacophorePoints();
		
		validHelpersQuery = false;
		
		resetSimilarityArrays = true;
	}
	
	
	private void calculateHelpersQuery(){
		
		arrRelativeDistanceMatrixQuery = calculateRelativeDistanceMatrix(cgQuery);
		
		maHelperAdjacencyQuery = new Matrix(arrRelativeDistanceMatrixQuery);
		
		MinimumSpanningTree mst = new MinimumSpanningTree(maHelperAdjacencyQuery);
		
		Matrix maMST = mst.getMST();
		
		sumDistanceMinSpanTreeQuery = maMST.getSumUpperTriangle();
		
		validHelpersQuery = true;
	}


	private void calculateHelpersBase(){
		
		arrRelativeDistanceMatrixBase = calculateRelativeDistanceMatrix(cgBase);

		maHelperAdjacencyBase = new Matrix(arrRelativeDistanceMatrixBase);
		
		
		MinimumSpanningTree mst = new MinimumSpanningTree(maHelperAdjacencyBase);
		
		Matrix maMST = mst.getMST();
		
		sumDistanceMinSpanTreeBase = maMST.getSumUpperTriangle();
		
		validHelpersBase = true;
	}
	
	private double getRatioMinimumSpanningTreeQuery(SolutionCompleteGraph solution) {
		
		double ratioCovered2Total = 0;
		
		int heap = solution.getSizeHeap();

		maHelperAdjacencyQuery.set(Double.NaN);
		
		for (int i = 0; i < heap; i++) {
			
			int indexNode1 = solution.getIndexQueryFromHeap(i);

			for (int j = i+1; j < heap; j++) {
				
				int indexNode2 = solution.getIndexQueryFromHeap(j);
				
				maHelperAdjacencyQuery.set(indexNode1, indexNode2, arrRelativeDistanceMatrixQuery[indexNode1][indexNode2]);
				maHelperAdjacencyQuery.set(indexNode2, indexNode1, arrRelativeDistanceMatrixQuery[indexNode1][indexNode2]);
				
			}
		}
		
		MinimumSpanningTree mstSolution = new MinimumSpanningTree(maHelperAdjacencyQuery);
		
		Matrix maMST = mstSolution.getMST();
		
		double sum = maMST.getSumUpperTriangle();
				
		// ratioCovered2Total = (sum*sum)/(sumDistanceMinSpanTreeQuery*sumDistanceMinSpanTreeQuery);
		
		double sumMSTSquared = sum*sum;
		
		double sumMSTSQuerySquared = sumDistanceMinSpanTreeQuery*sumDistanceMinSpanTreeQuery;
		
		ratioCovered2Total = Math.min(sumMSTSquared, sumMSTSQuerySquared)/Math.max(sumMSTSquared, sumMSTSQuerySquared);
	
		return ratioCovered2Total;
	}
	
	private double getRatioMinimumSpanningTreeBase(SolutionCompleteGraph solution) {
		
		double ratioCovered2Total = 0;
		
		int heap = solution.getSizeHeap();

		maHelperAdjacencyBase.set(Double.NaN);
		
		for (int i = 0; i < heap; i++) {
			
			int indexNode1 = solution.getIndexBaseFromHeap(i);

			for (int j = i+1; j < heap; j++) {
				
				int indexNode2 = solution.getIndexBaseFromHeap(j);
				
				maHelperAdjacencyBase.set(indexNode1, indexNode2, arrRelativeDistanceMatrixBase[indexNode1][indexNode2]);
				maHelperAdjacencyBase.set(indexNode2, indexNode1, arrRelativeDistanceMatrixBase[indexNode1][indexNode2]);
				
			}
		}
		
		MinimumSpanningTree mstSolution = new MinimumSpanningTree(maHelperAdjacencyBase);
		
		Matrix maMST = mstSolution.getMST();
		
		double sum = maMST.getSumUpperTriangle();
		
		double sumMSTSquared = sum*sum;
		
		double sumMSTSBaseSquared = sumDistanceMinSpanTreeBase*sumDistanceMinSpanTreeBase;
		
		ratioCovered2Total = Math.min(sumMSTSquared, sumMSTSBaseSquared)/Math.max(sumMSTSquared, sumMSTSBaseSquared);
		// ratioCovered2Total = (sum)/(sumDistanceMinSpanTreeBase);
		
		return ratioCovered2Total;
	}
	
	/**
	 * Calculate a distance matrix from the center of gravity distance bins. 
	 * The values are standardized by dividing them by the highest center of gravity value.  
	 * @param mdh
	 * @return
	 */
	private double [][] calculateRelativeDistanceMatrix(IMolDistHist mdh){
		
		int nodes = mdh.getNumPPNodes();
		
		double [][] arrDist = new double [nodes][nodes];
		
		int maxMedianDistanceBin = 0; 
		
		for (int i = 0; i < arrDist.length; i++) {
			
			for (int j = i+1; j < arrDist.length; j++) {
				
				int medianDistanceBin = getCenterOfGravityDistanceBin(mdh, i, j);
				
				arrDist[i][j] = medianDistanceBin;
				arrDist[j][i] = medianDistanceBin;
				
				if(medianDistanceBin > maxMedianDistanceBin){
					maxMedianDistanceBin = medianDistanceBin;
				}
				
			}
		}
		
		for (int i = 0; i < arrDist.length; i++) {
			
			for (int j = i+1; j < arrDist.length; j++) {
				
				arrDist[i][j] = arrDist[i][j] / maxMedianDistanceBin;
				
				arrDist[j][i] = arrDist[j][i] / maxMedianDistanceBin;
				
			}
		}
		
		return arrDist;
	}

	/**
	 * 
	 * @param mdh
	 * @param indexNode1
	 * @param indexNode2
	 * @return the index of the distance bin with the center of gravity for the histogram values.
	 */
	private int getCenterOfGravityDistanceBin(IMolDistHist mdh, int indexNode1, int indexNode2) {
		
		byte [] arr = mdh.getDistHist(indexNode1, indexNode2, arrTmpHist);

		double sum=0;
		
		for (int i = 0; i < arr.length; i++) {
				sum += arr[i]; 
		}
		
		double center = sum / 2.0;
		
		sum=0;
		
		int bin = -1;
		for (int i = arr.length-1; i >= 0; i--) {
			
			sum += arr[i]; 
			
			if(sum >= center) {
				bin=i;
				break;
			}
			
		}
		
		
		return bin;
	}

	/**
	 * Compares nodes and histograms
	 * @param indexNode1Query
	 * @param indexNode2Query
	 * @param indexNode1Base
	 * @param indexNode2Base
	 * @return
	 */
	private double getScorePairwiseMapping(int indexNode1Query, int indexNode2Query, int indexNode1Base, int indexNode2Base) {
		double score = 0;

		double simNodePair1 = getSimilarityNodes(indexNode1Query, indexNode1Base);
		
		double simNodePair1Scaled = scaleClassesSimilarityNodes.scale(simNodePair1);
		
		double simNodePair2 = getSimilarityNodes(indexNode2Query, indexNode2Base);
		
		double simNodePair2Scaled = scaleClassesSimilarityNodes.scale(simNodePair2);
		
		double simHists = getSimilarityHistogram(indexNode1Query, indexNode2Query, indexNode1Base, indexNode2Base);
		
		if(simHists==0){
			System.out.println("Sim hists = 0");
		}
		
		double simHistsScaled = scaleClassesSimilarityHistogram.scale(simHists);
		
		// score = simNodePair1Scaled * simNodePair2Scaled * simHistsScaled;
		
		score = simNodePair1Scaled * simNodePair1Scaled * simNodePair2Scaled * simNodePair2Scaled * simHistsScaled * simHistsScaled;
		
		
		return score;
	}

	private float getSimilarityNodes(int indexNodeQuery, int indexNodeBase) {
		
		if(arrSimilarityNodes[indexNodeQuery][indexNodeBase] < 0){
			
			float median = (float)nodeSimilarity.getSimilarity(cgQuery.getNode(indexNodeQuery), cgBase.getNode(indexNodeBase));
			
			arrSimilarityNodes[indexNodeQuery][indexNodeBase]=median;
			
		} 
		
		return arrSimilarityNodes[indexNodeQuery][indexNodeBase];
	}
	
	private float getSimilarityHistogram(int indexNode1Query, int indexNode2Query, int indexNode1Base, int indexNode2Base) {
		
		int indexHistogramQuery = DistHist.getIndex(indexNode1Query, indexNode2Query, nodesQuery);
		
		int indexHistogramBase = DistHist.getIndex(indexNode1Base, indexNode2Base, nodesBase);
		
		if(arrSimilarityHistograms[indexHistogramQuery][indexHistogramBase] < 0){
			
			float similarityHistogram = (float)histogramMatchCalculator.getSimilarity(cgQuery, indexNode1Query, indexNode2Query, cgBase, indexNode1Base, indexNode2Base);
			
			arrSimilarityHistograms[indexHistogramQuery][indexHistogramBase]=similarityHistogram;
		} 
			
		
		return arrSimilarityHistograms[indexHistogramQuery][indexHistogramBase];
	}
	
	public double getSimilarityNodes(PPNode query, PPNode base) {
		return nodeSimilarity.getSimilarity(query, base);
	}

	
	/**
	 	private double avrPairwiseMappingScaled;
	private double coverageQuery;
	private double coverageQueryScaled;
	private double coverageBase;
	private double coverageBaseScaled;
	private double similarity;
	private double similarityScaled;
 
	 * @return
	 */
	public String toStringRecentSimilarityResults() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("avr pairwise mapping " + Formatter.format3(avrPairwiseMappingScaled) + "\n");
		sb.append("coverage query " + Formatter.format3(coverageQuery) + "\n");
		sb.append("coverage base " + Formatter.format3(coverageBase) + "\n");
		sb.append("similarity " + Formatter.format3(similarity) + " scaled " + Formatter.format3(similarityScaled) + "\n");
		
		
		return sb.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		Matrix maSimNodes = new Matrix(arrSimilarityNodes);
		
		int rowEnd = -1;
		for (int i = 0; i < maSimNodes.rows(); i++) {
			if(maSimNodes.get(i, 0)<0){
				rowEnd = i;
				break;
			}
		}
		
		int colEnd = -1;
		for (int i = 0; i < maSimNodes.cols(); i++) {
			if(maSimNodes.get(0, i)<0){
				colEnd = i;
				break;
			}
		}
		
		for (int i = 0; i < rowEnd; i++) {
			for (int j = 0; j < colEnd; j++) {
				sb.append(Formatter.format2(maSimNodes.get(i, j)));
				sb.append("  ");
			}
			sb.append("\n");
		}
		
		
		
		return sb.toString();
	}
	
}
