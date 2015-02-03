package com.actelion.research.chem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * 
 * 
 * FFMoleculeMultiple
 * Extracts molecules from one FFMolecule and delivers a map.
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Sep 20, 2011 MvK: Start implementation
 */
public class FFMoleculeExtractor {
	
	private FFMolecule ff;
	
	private List<FFMolecule> liFFMoleculeFrag;
	
	private List<int []> liMapFrag2Mol;
	
	public FFMoleculeExtractor(FFMolecule ff) {
		
		this.ff = ff;
		
		init();
	}
	
	public FFMolecule getMolecule(int index){
		return liFFMoleculeFrag.get(index);
	}
	
	public int [] getMap(int index){
		return liMapFrag2Mol.get(index);
	}
	
	public int size(){
		return liFFMoleculeFrag.size();
	}

	private void init() {
		List<Integer> liIndexAtom = new ArrayList<Integer>();
		for (int i = 0; i < ff.getAllAtoms(); i++) {
			liIndexAtom.add(i);
		}
		
		List<List<Integer>> liliIndexAtomsFrags = new ArrayList<List<Integer>>(); 
		
		while(!liIndexAtom.isEmpty()) {
			int indexAtomStart = liIndexAtom.remove(0);
			
			HashSet<Integer> hsIndexAtomFrag = extract(ff, indexAtomStart);
			
			liliIndexAtomsFrags.add(new ArrayList<Integer>(hsIndexAtomFrag));
			
			for (int i = liIndexAtom.size()-1; i >= 0; i--) {
				
				if(hsIndexAtomFrag.contains(liIndexAtom.get(i)))
					liIndexAtom.remove(i);
			}
		}
		
		extractWithMapGeneration(ff, liliIndexAtomsFrags);
	}
	
	private static HashSet<Integer> extract(FFMolecule ff, int indexAtomStart) {
		
		HashSet<Integer> hsVisisted = new HashSet<Integer>();
		
		hsVisisted.add(indexAtomStart);
		
		List<Integer> liQueue = new ArrayList<Integer>();
		
		liQueue.add(indexAtomStart);
		
		while(!liQueue.isEmpty()){
			int indexAtom = liQueue.remove(0);
			
			int nConn = ff.getAllConnAtoms(indexAtom);
			
			for (int i = 0; i < nConn; i++) {
				int indexAtomConn = ff.getConnAtom(indexAtom, i);
				
				if(!hsVisisted.contains(indexAtomConn)) {
					hsVisisted.add(indexAtomConn);
					
					liQueue.add(indexAtomConn);
				}
			}
		}
		
		return hsVisisted;
	}
	
	private void extractWithMapGeneration(FFMolecule ff, List<List<Integer>> liliIndexAtomsFrags) {
		
		List<FFMolecule> liFFMolecule = new ArrayList<FFMolecule>();
		
		List<int[]> liMap = new ArrayList<int[]>();
		
		for (int i = 0; i < liliIndexAtomsFrags.size(); i++) {
			
			List<Integer> liIndexAtomsFrag = liliIndexAtomsFrags.get(i);
			
			FFMolecule ffFrag = new FFMolecule();
			
			int [] arrMap = new int [liIndexAtomsFrag.size()];
			
			HashMap<Integer, Integer> hmIndexFF_FFFrag = new HashMap<Integer, Integer>();
			
			for (int j = 0; j < liIndexAtomsFrag.size(); j++) {
				
				int indexAtom = liIndexAtomsFrag.get(j);
				
				int indexAtomFrag = ffFrag.addAtom(ff, indexAtom);
								
				if(j!=indexAtomFrag){
					throw new RuntimeException("Error in algorithm.");
				}
				arrMap[j]=indexAtom;
				
				hmIndexFF_FFFrag.put(indexAtom, j);
				
			}
			
			for (int j = 0; j < ff.getAllBonds(); j++) {
				int indexAtom1 = ff.getBondAtom(0, j);
				
				if(hmIndexFF_FFFrag.containsKey(indexAtom1)){
					int indexAtom2 = ff.getBondAtom(1, j);
					
					if(hmIndexFF_FFFrag.containsKey(indexAtom2)){
						
						ffFrag.addBond(hmIndexFF_FFFrag.get(indexAtom1), hmIndexFF_FFFrag.get(indexAtom2), ff.getBondOrder(j));
					}
				}
			}
			
			liFFMolecule.add(ffFrag);
			
			liMap.add(arrMap);
			
			
		}
		
		liFFMoleculeFrag = Collections.unmodifiableList(liFFMolecule);
		
		liMapFrag2Mol = Collections.unmodifiableList(liMap);
		
		
	}
	
	/**
	 * 
	 * @param ff
	 * @param liIndexAtomsFrags the first atom in the index list becomes the first atom in the new molecule and so on.
	 * @return
	 */
	public static FFMolecule extract(FFMolecule ff, List<Integer> liIndexAtomsFrags) {
		
		FFMolecule ffFrag = new FFMolecule();
		
		int [] arrMap = new int [liIndexAtomsFrags.size()];
		
		HashMap<Integer, Integer> hmIndexFF_FFFrag = new HashMap<Integer, Integer>();
		
		for (int i = 0; i < liIndexAtomsFrags.size(); i++) {
			
			int indexAtom = liIndexAtomsFrags.get(i);
			
			int indexAtomFrag = ffFrag.addAtom(ff, indexAtom);
							
			if(i!=indexAtomFrag){
				throw new RuntimeException("Error in algorithm.");
			}
			arrMap[i]=indexAtom;
			
			hmIndexFF_FFFrag.put(indexAtom, i);
			
		}
		
		for (int i = 0; i < ff.getAllBonds(); i++) {
			int indexAtom1 = ff.getBondAtom(0, i);
			
			if(hmIndexFF_FFFrag.containsKey(indexAtom1)){
				int indexAtom2 = ff.getBondAtom(1, i);
				
				if(hmIndexFF_FFFrag.containsKey(indexAtom2)){
					
					ffFrag.addBond(hmIndexFF_FFFrag.get(indexAtom1), hmIndexFF_FFFrag.get(indexAtom2), ff.getBondOrder(i));
				}
			}
		}
		
		return ffFrag;
		
	}

}
