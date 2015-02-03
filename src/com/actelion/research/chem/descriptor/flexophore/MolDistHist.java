package com.actelion.research.chem.descriptor.flexophore;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.calc.ArrayUtilsCalc;
import com.actelion.research.chem.descriptor.flexophore.generator.CGMult;
import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;
import com.actelion.research.util.StringFunctions;

/**
 * 
 * MolDistHist
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2005 MvK: Start implementation
 * 23 Oct 2012 MvK: Variable names updated.
 * 12 Dec 2012 MvK: Bug fix, adding more than three interaction types failed.
 */
public class MolDistHist extends DistHist implements Serializable, IMolDistHist  {
	
	private static final long serialVersionUID = 17042013;

	// Minimum number of nodes for an comparison.
	public final static int MINIMUM_NUM_NODES = 3;
	
	public static final String TAG_FLEXOPHORE_OBJECT =  "FlexDecoded";
	
	public static final String TAG_FLEXOPHORE_POINTS =  "Flexophore points";
	
	public static final int MAX_DIST_CLUSTER = 2;
	
	public static final boolean VERBOSE = false;
	
	private static final int SIZE_BUFFER = 16;
	
	// The pharmacophore nodes containing 
	// The first entry is the number of atom types in the node, followed by the atom types for the node.
	// [nAtomsNode1, attype11, attype12, nAtomsNode2, attype21, nAtomsNode3...]
	
	private byte [] arrNode;
	
	private int posNode;
	
	private boolean finalized;

	public MolDistHist () {
		initHistogramArray(SIZE_BUFFER);
	}
	
	public MolDistHist (int size) {
		initHistogramArray(size);
	}
	
	public MolDistHist (MolDistHist mdh) {
		initHistogramArray(SIZE_BUFFER);
		mdh.copy(this);
	}
	
	public MolDistHist copy(){
		MolDistHist copy = new MolDistHist();
		copy(copy);
		return copy;
	}
	
	
	public boolean check(){
		boolean bOK = true;
		
		int nodes = getNumPPNodes();
		for (int i = 0; i < nodes; i++) {
			PPNode node = getNode(i);
			int ats = node.getInteractionTypeCount();
			for (int j = 0; j < ats; j++) {
				int inttype = node.getInteractionId(j);
				if(inttype > (ClassInteractionStatistics.getInstance().getNClasses()-1) ) {
					bOK = false;
					if(VERBOSE)
						System.err.println("Node " + i + " atom " + j + " Interaction type " + inttype + ".");
				}
			}
		}
		return bOK;
	}
	/**
	 * 
	 * @param copy: This object is written into copy.
	 */
	public void copy(MolDistHist copy){
		super.copy(copy);
		
		copy.arrNode = new byte [arrNode.length];
		
		System.arraycopy(arrNode, 0, copy.arrNode, 0, arrNode.length);
		
		copy.posNode = posNode;
		
		copy.realize();
		
	}
	
	public boolean equals(Object o) {
		boolean bEQ=true;
		
		MolDistHist mdh=null;
		try {
			mdh = (MolDistHist)o;
		} catch (RuntimeException e) {
			return false;
		}
		
		
		if(getNumPPNodes() != mdh.getNumPPNodes())
			return false;
		
		
		for (int i = 0; i < getNumPPNodes(); i++) {
			PPNode n1 = getNode(i);
			PPNode n2 = mdh.getNode(i);
			if(!n1.equals(n2)){
				bEQ = false;
				break;
			}
		}
		
		for (int i = 0; i < getNumPPNodes(); i++) {
			for (int j = i+1; j < getNumPPNodes(); j++) {
				byte [] a1 = getDistHist(i,j);
				byte [] a2 = mdh.getDistHist(i,j);
				for (int k = 0; k < a2.length; k++) {
					if(a1[k]!=a2[k]){
						bEQ = false;
						break;
					}
				}
			}
		}
		
		return bEQ;
	}
	
	protected void initHistogramArray(int size) {
		super.initHistogramArray(size);
		
		arrNode = new byte[size*3];
		
		finalized = false;
	}
	
	public void addNode(PPNode node) {
		byte [] arr = node.get();
		
		int newlen = posNode + arr.length + 1;
		
		if(arrNode.length <= newlen){
			resize(arrNode.length+SIZE_BUFFER);
		}
		
		arrNode[posNode++] = (byte)node.getInteractionTypeCount();
		for (int i = 0; i < arr.length; i++) {
			arrNode[posNode++]= arr[i];
		}
		
		incrementNumPPNodes();
		
		finalized = false;
	}

	/**
	 * @return the arrNode
	 */
	protected byte[] getArrNode() {
		return arrNode;
	}

	/**
	 * @param arrNode the arrNode to set
	 */
	protected void setArrNode(byte[] arrNode) {
		
		this.arrNode = arrNode;
		
		int pos=0;
		while(arrNode[pos] > 0){
			pos += arrNode[pos] * PPNode.NUM_BYTES_INTERACTION_TYPE + 1;
			incrementNumPPNodes();
			if(pos>=arrNode.length)
				break;
		}
		
	}

	public void realize(){
		super.realize();
		
		int size = getNumPPNodes();
		
		if(size==0){
			throw new RuntimeException("No pharmacophore points in Flexophore.");
		}
		
		int pos = getPositionNode(size-1);
		
		int len = pos + arrNode[pos] * PPNode.NUM_BYTES_INTERACTION_TYPE + 1;
		
		resize(len);
		
		if(getNumPPNodes()==0)
			return;

		finalized = true;
	}
	
	private void resize(int newsize){
		
		byte [] arr = new byte [newsize];
		
		int len = Math.min(newsize, arrNode.length);
		
		System.arraycopy(arrNode, 0, arr, 0, len);
		
		arrNode = arr;
		
		finalized = false;
	}
	
	
	public int getConnAtom(int at, int index) {
		if(index >= at)
			index++;
		
		return index;
	}

	@Override
	public int hashCode() {
		String s = toString();
		s = s.replace(" ", "");
		return s.hashCode();
	}
	
	public String toString(){
		
		if(!finalized)
			realize();
		
		StringBuffer b = new StringBuffer();
		
		b.append("[");
		for (int i = 0; i < getNumPPNodes(); i++) {
			b.append(getNode(i).toString());
			if(i<getNumPPNodes()-1){
				b.append(" ");
			} else {
				b.append("]");
			}
		}
		
		for (int i = 0; i < getNumPPNodes(); i++) {
			for (int j = i+1; j < getNumPPNodes(); j++) {
				byte [] arrHist = getDistHist(i,j);
				
				if(arrHist!=null)
					b.append(ArrayUtilsCalc.toString(arrHist));
			}
		}
		
		return b.toString();
	}
	
	public String toStringHists(){
		
		if(!finalized)
			realize();
		
		StringBuffer b = new StringBuffer();
		
		for (int i = 0; i < getNumPPNodes(); i++) {
			for (int j = i+1; j < getNumPPNodes(); j++) {
				byte [] arrHist = getDistHist(i,j);
				
				if(arrHist!=null)
					b.append(ArrayUtilsCalc.toString(arrHist));
			}
		}
		
		return b.toString();
	}

	
	protected boolean isFinalized() {
		return finalized;
	}
	
	
	
	/**
	 * The position of the size of the node. The node index starts with 0 and runs up to size-1 inclusive.
	 * @param index
	 * @return
	 */
	private int getPositionNode(int index){
		int pos = 0;
		for (int i = 0; i < index; i++) {
			pos += arrNode[pos]  * PPNode.NUM_BYTES_INTERACTION_TYPE + 1;
		}
		return pos;
	}
	
	/**
	 * !Slow method, it has to iterate through a loop to find the node in the array!
	 * @param index
	 * @return deep copy of the node.
	 */
	public PPNode getNode(int index){
		PPNode node = new PPNode();
		
		int pos = getPositionNode(index);
		
		int nPPPoints = arrNode[pos] * PPNode.NUM_BYTES_INTERACTION_TYPE;
		for (int i = 0; i < nPPPoints; i += PPNode.NUM_BYTES_INTERACTION_TYPE) {
			
			int interactionType = PPNode.getInteractionId(arrNode[pos+1+i], arrNode[pos+2+i]);
			
			node.add(interactionType);
		}
		
		node.realize();
		
		return node;
	}
	
	/**
	 * 
	 * @param index
	 * @return number of pharmacophore points at the specified index
	 * slow method because it calls getPositionNode(index).
	 */
	public int getPPPoints(int index){
		
		int pos = getPositionNode(index);
		
		int nPPPoints = arrNode[pos];
		
		return nPPPoints;
	}
	
	public int getSizeBytes(){
		int s = super.getSizeBytes();
		
		s += arrNode.length;	
		
		// bFinalized
		s += (Integer.SIZE / 8)*(1);
		
		return s;
	}

	/**
	 * Only for interface compliance needed.
	 */
	@Override
	public int getNumInevitablePharmacophorePoints() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isInevitablePharmacophorePoint(int indexNode) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * reads a MolDistHist from the toString() method.
	 * @param strMolDistHist
	 * @return
	 */
	public static MolDistHist read(String strMolDistHist){
		
		String pattern = "[0-9]+";
		
		int start = strMolDistHist.indexOf('(');
		
		boolean nodesProcessed = false;
		
		List<PPNode> liPPNode = new ArrayList<PPNode>();
		while(!nodesProcessed){
			
			int end = StringFunctions.nextClosing(strMolDistHist, start, '(', ')');
			
			String sub = strMolDistHist.substring(start+1, end);
		
			List<Point> li = StringFunctions.match(sub, pattern);
			
			PPNode n = new PPNode();
			
			for (Point p : li) {
				
				String strInteractionType = sub.substring(p.x, p.y);
				
				n.add(Integer.parseInt(strInteractionType));
			}
			
			n.realize();
			
			liPPNode.add(n);
			
			start = strMolDistHist.indexOf('(', end);
			
			if(start==-1){
				nodesProcessed = true;
			}
		}
		
		int size = liPPNode.size();
		
		MolDistHist mdh = new MolDistHist(size);
		
		for (PPNode ppNode : liPPNode) {
			mdh.addNode(ppNode);
		}
		
		boolean histsProcessed = false;
		
		List<byte []> liHist = new ArrayList<byte []>();
		
		int startHist = strMolDistHist.indexOf("][");
		
		int nHistograms = ((size*size)-size)/2;
		
		while(!histsProcessed){
			
			int endHist = StringFunctions.nextClosing(strMolDistHist, startHist, '[', ']');
			
			String sub = strMolDistHist.substring(startHist, endHist);
		
			List<Point> li = StringFunctions.match(sub, pattern);
			
			if(li.size() != CGMult.BINS_HISTOGRAM){
				throw new RuntimeException("Error in histogram.");
			}
			
			byte [] arr = new byte [CGMult.BINS_HISTOGRAM];
			
			int cc=0;
			for (Point p : li) {
				
				String strCount = sub.substring(p.x, p.y);
				
				arr[cc++] = (byte)(Integer.parseInt(strCount) & 0xFF);
				
			}
			
			liHist.add(arr);
			
			startHist = strMolDistHist.indexOf('[', endHist);
			
			if(liHist.size()==nHistograms){
				histsProcessed=true;
			}
		}
		
		int cc=0;
		for (int i = 0; i < size; i++) {
			for (int j = i+1; j < size; j++) {
				mdh.setDistHist(i,j, liHist.get(cc++));
			}
		}

		
		return mdh;
	}
	
}
