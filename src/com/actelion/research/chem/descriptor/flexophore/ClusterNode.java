package com.actelion.research.chem.descriptor.flexophore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * ClusterNode
 * Cluster, containing node indices
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 22 Dec 2008 MvK: Start implementation
 * 
 */
public class ClusterNode implements Comparable<ClusterNode> {
	
	private int indexCenter;
	
	private List<Integer> liIndexMember;
	
	private int rmsd;
	
	public ClusterNode(int indexCenter) {
		this.indexCenter = indexCenter;
		
		liIndexMember = new ArrayList<Integer>();
		
		rmsd = 0;
	}
	
	public void add(int index){
		liIndexMember.add(index);
	}
	
	public int compareTo(ClusterNode cl) {
		return Double.compare(rmsd, cl.rmsd);
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean bEqual = true;
		
		ClusterNode cl = (ClusterNode)obj;
		
		if(liIndexMember.size()!=cl.liIndexMember.size()){
			return false;
		}
		
		List<Integer> l1 = new ArrayList<Integer>(liIndexMember);
		l1.add(indexCenter);
		
		List<Integer> l2 = new ArrayList<Integer>(cl.liIndexMember);
		l2.add(cl.indexCenter);
		
		Collections.sort(l1);
		
		Collections.sort(l2);
		
		for (int i = 0; i < l1.size(); i++) {
			int v1 = l1.get(i);
			int v2 = l2.get(i);
			
			if(v1!=v2){
				bEqual=false;
				break;
			}
		}
		
		return bEqual;
	}

	public int getIndexCenter() {
		return indexCenter;
	}

	public int getRMSD() {
		return rmsd;
	}
	
	/**
	 * Sum of squared distances of the center to the other cluster members
	 * @param rmsd
	 */ 
	public void setRMSD(int rmsd) {
		this.rmsd = rmsd;
	}
	
	public static int getRMSD(ClusterNode cluster, DistHist mdh){
		int rmsd=0;
		
		List<Integer> liIndex = cluster.getClusterMember();
		
		for (int i = 0; i < liIndex.size(); i++) {
			int dist = mdh.getMinDist(cluster.getIndexCenter(), liIndex.get(i));
			rmsd+=dist*dist;
		}
		
		return rmsd;
	}

	public List<Integer> getClusterMember() {
		return liIndexMember;
	}
	
	public boolean isCluster(){
		if(liIndexMember.size()>0)
			return true;
		else
			return false;
	}

}
