package com.actelion.research.chem.descriptor.flexophore.generator;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.descriptor.flexophore.PPNodeViz;

/**
 * 
 * CompleteGraph
 * Contains pharmacophore nodes and a distance table.
 * The distance may be in Angstrom.
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2004 MvK: Start implementation
 */
public class CompleteGraph {
	
	private static final NumberFormat NF = new DecimalFormat("0.0000000");

	// atom type 1, atom type 2, distance
	public static final int ENTRIES_IN_MATRIX = 3;
    public static final String NAME_COL_NO_ATOMS = "NoAtoms";

	private List<PPNodeViz> liNodes;
	
	// Symmetric matrix of distances
	private double [][] edges;   
	
	/**
	 * Constructor
	 */
	public CompleteGraph() {
		init();
	}

	private void init(){
		liNodes = new ArrayList<PPNodeViz>();
	}
	
	/**
	 * @param mol
	 */
	public CompleteGraph(CompleteGraph mol) {
		init();
		for(int i=0; i<mol.getAllNodes(); i++) {
			liNodes.add(new PPNodeViz(mol.getPPNode(i)));
		}
		
		edges = new double [mol.getAllNodes()][mol.getAllNodes()];

		for(int i=0; i<mol.getAllBonds(); i++) {
			for(int j=0; j<mol.getAllBonds(); j++) {
				edges[i][j] = mol.edges[i][j];
			}
		}
	}
	
	/**
	 * 
	 * @param node
	 * @return index of the node.
	 */
	public int addNode(PPNodeViz node) {
		int index = -1;
		
		boolean bNewNode = true;
		for (int i = 0; i < size(); i++) {
			if(node.hasSamePosition(getPPNode(i))) {
				getPPNode(i).addAtoms(node);
				
				bNewNode = false;
				index = i;
				break;
			}
		}
		
		if(bNewNode){
			index = liNodes.size();
			
			node.setIndex(index);
			
			liNodes.add(node);
		}
		
		return index;
	}

	/**
	 * The distances are written to a symmetric matrix to accelerate the access.
	 *
	 */
	public void calculateDistances() {
		edges = new double [getAllNodes()][getAllNodes()];
		
		int nNodes = getAllNodes();
		for (int i = 0; i < nNodes; i++) {
			PPNodeViz ppNode1 = getPPNode(i);
			double x1 = ppNode1.getX();
			double y1 = ppNode1.getY();
			double z1 = ppNode1.getZ();
			for (int j = i; j < nNodes; j++) {
				PPNodeViz ppNode2 = getPPNode(j);
				double x2 = ppNode2.getX();
				double y2 = ppNode2.getY();
				double z2 = ppNode2.getZ();
				double dist = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2)
						* (y1 - y2) + (z1 - z2) * (z1 - z2));
				edges[i][j] = dist;
				edges[j][i] = dist;
			}
		}
		// Matrix m = new Matrix(edges);
		// System.out.println(m.toString(3));
	}

	public final double [][] getEdges() {
		return edges;
	}

	public final void setEdge(int at1, int at2, double dist) {
		edges[at1][at2] = dist;
		edges[at2][at1] = dist;
	}

	public final PPNodeViz getPPNode(int i) {
		return liNodes.get(i);
	}
	public final int getAllNodes() {
		return liNodes.size();
	}
	public final int size() {
		return liNodes.size();
	}

	public final double [] getAllConnBonds(int atm) {
		return edges[atm];
	}

	public final double getConnBond(int atm, int i) {
		return edges[atm][i];
	}

	public final int getAllBonds() {
		return edges.length;
	}
	
	public final double getBondBetween(int a1, int a2) {
		return edges[a1][a2];
	}
	

	/**
	 * Adds a bond between 2 atoms
	 * @see com.actelion.research.chem.IMolecule#addBond(int, int, int)
	 */
	public void addBond(int atm1, int atm2, double length) {
		edges[atm1][atm2] = length;
	}

	public void realize(){
		for (PPNodeViz node : liNodes) {
			node.realize();
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("["+getAllNodes()+" atoms, "+ getAllBonds()+" bonds]");
		sb.append("\n");
		for (int i = 0; i < liNodes.size(); i++) {
			sb.append(liNodes.get(i).toString());
			sb.append(":");
			sb.append(NF.format(liNodes.get(i).getX()));
			sb.append(",");
			sb.append(NF.format(liNodes.get(i).getY()));
			sb.append(",");
			sb.append(NF.format(liNodes.get(i).getZ()));
			sb.append("\n");
		}
		
 		return sb.toString();
 		
	}
	
	public String toStringDistances() {
		String str = "";
		int nNodes = getAllNodes();
		DecimalFormat df = new DecimalFormat("0.000"); 
		
		for (int i = 0; i < nNodes; i++) {
			for (int j = 0; j < nNodes; j++) {
				str += df.format(edges[i][j]) + " "; 
			}
			str += "\n";
		}
		return str;
	}
}
