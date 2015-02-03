package com.actelion.research.chem.descriptor.flexophore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.actelion.research.calc.ArrayUtilsCalc;
import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;

/**
 * <p>Title: PPNode </p>
 * <p>A PPnode can contain several atoms and atom types. </p>
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 2005 MvK: Start implementation
 * 15. Apr. 2013 MvK: changed size interaction types into two bytes.
 * 11. Oct. 2013 MvK: toStringLong() small changes.
 */
public class PPNode implements Comparable<PPNode> {

	protected static final int NUM_BYTES_INTERACTION_TYPE = 2;
	
	public static final int DUMMY_INTERACT_ID = 37;
		
	public static final String SEPARATOR_ATOMS = ","; 
	
	public static final int INFO_DEFAULT = -1;
	
	private static final int MAX_VALUE = 30000;
	
	private static final int MASK1 = 0x000000FF;
	
	// May be be a multiple of 2.
	private static final int BUFFER_SIZE=6;

	
	private byte [] arrInteractionType;
	
	private byte size;
	
	public PPNode(){
		init();
	}

	public PPNode(PPNode node){
		copy(node);
	}

	/**
	 * 
	 * @param interactionID
	 */
	public PPNode(int interactionID){
		init();
		
		add(interactionID);
		
	}

	/**
	 * Caution! After finishing all adding's realize() has to be called!
	 * @param interactiontype
	 */
	public void add(int interactiontype){
		
		if(interactiontype > MAX_VALUE)
			throw new RuntimeException("Interaction type " + interactiontype + " larger than " + MAX_VALUE + ".");
				
		int index = size * NUM_BYTES_INTERACTION_TYPE;
		
		if(index == arrInteractionType.length){
			index = arrInteractionType.length;
			
			arrInteractionType = ArrayUtilsCalc.resize(arrInteractionType, arrInteractionType.length+BUFFER_SIZE);
			
		}
		
		setPPPoint(size, interactiontype);
		
		size++;
	}
	
	/**
	 * Only atoms are added that are not yet in the list,
	 * check PPAtom.equals for comparison.
	 * @param node
	 */
	public void addAtoms(PPNode node){
		
		for (int i = 0; i < node.getInteractionTypeCount(); i++) {
			
			if(!containsInteractionID(node.getInteractionId(i))){
				add(node.getInteractionId(i));
			}
		}
		
	}
		
	public int compareTo(PPNode o) {
		int cmp=0;
		
		
		int size1 = getInteractionTypeCount();
		
		int size2 = o.getInteractionTypeCount();
		
		int max1 = getMaximumInteractionType(this);
		
		int max2 = getMaximumInteractionType(o);
		
		if(max1 > max2)
			cmp=1;
		else if(max1 < max2)
			cmp=-1;
		else {
			if(size1 > size2)
				cmp=1;
			else if(size1 < size2)
				cmp=-1;
			else {
				
				for (int i = 0; i < size1; i++) {
					
					int id1 = getInteractionId(i);
					
					int id2 = o.getInteractionId(i);
					
					if(id1 > id2){
						cmp=1;
						
					} else if(id1 < id2){
						
						cmp=-1;
					}
				}
			}
		}
			
		return cmp;
	}
	
	public boolean containsInteractionID(int interactid){
		boolean contains=false;
		
		if(arrInteractionType==null)
			return contains;
		
		int size = getInteractionTypeCount();

		for (int i = 0; i < size; i++) {
			if(getInteractionId(i) == interactid){
				contains=true;
				break;
			}
		}
		
		return contains;
		
	}
	
	/**
	 * Copy of node into this.
	 * @param node
	 */
	public void copy(PPNode node){
		
		arrInteractionType = new byte[node.arrInteractionType.length];
		
		System.arraycopy(node.arrInteractionType, 0, arrInteractionType, 0, node.arrInteractionType.length);
		
		size = node.size;
	}
	
	/**
	 * 
	 * @param node deep copy.
	 */
	public PPNode getCopy(){
		return new PPNode(this);  
	}
	
	public boolean equals(Object o) {
		PPNode n = (PPNode)o;
		return equalAtoms(n);
	}
	
	/**
	 * May be called after finishing adding new interaction types. 
	 */
	public void realize(){
		
		int sizeBytes = size*2;
		
		arrInteractionType = ArrayUtilsCalc.resize(arrInteractionType, sizeBytes);
		
		sortInteractionTypes();
	}
	
	/**
	 * realize() may be called first. 
	 * @param node
	 * @return
	 */
	public boolean equalAtoms(PPNode node) {
		boolean b = true;
		
		int size1 = getInteractionTypeCount();
		
		int size2 = node.getInteractionTypeCount();
		
		if(size1 != size2)
			b = false;
		else {
			
			for (int i = 0; i < size1; i++) {
				
				int id1 = getInteractionId(i);
				
				int id2 = node.getInteractionId(i);
				
				if(id1 != id2){
					b = false;
					break;
				}
			}
		}
			
		return b;
	}
	
	public byte [] get(){
		return arrInteractionType;
	}

	/**
	 * realize() may be called first.
	 * @return
	 */
	public int getInteractionTypeCount(){
		return size;
	}
	
	public int getInteractionId(int i){
		
		int index = i * NUM_BYTES_INTERACTION_TYPE;
				
		return getInteractionId(arrInteractionType[index], arrInteractionType[index+1]);
	}
	
	protected static int getInteractionId(byte low, byte high){
		
		// & 0xFF to prevent conservation of -1.
		int v = (high & 0xFF) << 8;
		
		v = v | (low & 0xFF);
		
		return v;
	}
	
	public int getAtomicNo(int i){
		return Interaction2AtomicNo.getInstance().getAtomicNumber(getInteractionId(i));
	}
	
	public boolean isAromatic(int i){
		return Interaction2AtomicNo.getInstance().isAromatic(getInteractionId(i));
	}
	
	public static int getAtomicNoFromInteractionType(int interactionType){
		return Interaction2AtomicNo.getInstance().getAtomicNumber(interactionType);
	}

	public static PPNode getDummy(){
		PPNode node = new PPNode();
		node.add(DUMMY_INTERACT_ID);
		return node;
	}
	
	public boolean hasHeteroAtom(){
		boolean bY = false;
		
		int size = getInteractionTypeCount();
		
		for (int i = 0; i < size; i++) {
			if(getAtomicNo(i) !=6) {
				bY=true;
				break;
			}
		}
		return bY;
	}
	public boolean isCarbonExclusiveNode(){
		
		boolean bY = true;
		
		int size = getInteractionTypeCount();
		
		for (int i = 0; i < size; i++) {
			if(getAtomicNo(i) !=6) {
				bY=false;
				break;
			}
		}
		
		return bY;
	}
	
	
	private void init(){
		arrInteractionType = new byte [BUFFER_SIZE];
	}
	
	/**
	 * Flat copy from node into this.
	 * @param node
	 */
	public void set(PPNode node){
		
		this.arrInteractionType = node.arrInteractionType;
		
		this.size = node.size;
	}
	
	public void setPPPoint(int i, int interactiontype){
		
		int index = i * NUM_BYTES_INTERACTION_TYPE;

		byte low = (byte)(interactiontype & MASK1);
		
		byte high = (byte)(interactiontype >> 8);
				
		arrInteractionType[index] = low;
		
		arrInteractionType[index+1] = high;
		
	}
	
	public void sortInteractionTypes(){
		
		int size = getInteractionTypeCount();
		
		int [] arr = new int [size];
		
		for (int i = 0; i < size; i++) {
			
			arr[i] = getInteractionId(i);
			
		}
		
		Arrays.sort(arr);
		
		for (int i = 0; i < size; i++) {
			setPPPoint(i, arr[i]);
		}

	}
		
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");
		
		int size = getInteractionTypeCount();
		
		for (int i = 0; i < size;i++) {
			
			if(i>0){
				sb.append(SEPARATOR_ATOMS);
			}
			
			sb.append(getInteractionId(i));
		}
		
		sb.append(")");
		return sb.toString();
	}
	
	public String toStringLong(){
		
		HashSet<String> hs = new HashSet<String>();
		
		int size = getInteractionTypeCount();
		
		for (int i = 0; i < size; i++) {
			
			String s = ClassInteractionStatistics.getInstance().getDescription(getInteractionId(i));
			
			s = formatInteractionDescription(s);
				
			
			hs.add(s);
		}
		
		
		StringBuilder sb = new StringBuilder();
		
		List<String> li = new ArrayList<String>(hs);
		
		for (int i = 0; i < li.size(); i++) {
			
			String descr = li.get(i);
			
			sb.append(descr);
			
			if(i < li.size()-1){
				sb.append(SEPARATOR_ATOMS + " ");
			}
		}
		
		return sb.toString();
	}
	
	public static String toStringInteractionType(int interactionType){
		
		String s = ClassInteractionStatistics.getInstance().getDescription(interactionType);
		
		return s;
	}
	
	private static String formatInteractionDescription(String s){
		
		String regex = ".\\*";
		
		Pattern pa = Pattern.compile(regex);
    	
    	Matcher ma = pa.matcher(s);
    	
    	StringBuilder sb = new StringBuilder();
    	
    	if(ma.find()){
    		MatchResult mr = ma.toMatchResult();

    		int end = mr.end();
    		
     		sb.append(s.substring(end));
    		
    	}
		
    	String str = sb.toString();
    	
    	str = str.replaceAll("\\{\\}", "");
    	
    	str = str.replaceAll("_", " ");
    	
    	return str;
		
	}
	
	private static int getMaximumInteractionType(PPNode n){
		
		int max = 0;
		
		int size = n.getInteractionTypeCount();
		
		for (int i = 0; i < size ; i++) {
			
			int id = n.getInteractionId(i);
			
			if(id < max){
				max=id;
			}
			
		}

		return max;
	}
	
	
}

