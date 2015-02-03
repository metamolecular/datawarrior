package com.actelion.research.chem.descriptor.flexophore;

import java.util.StringTokenizer;

import com.actelion.research.chem.descriptor.DescriptorEncoder;
import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.util.datamodel.IntVec;

/**
 * MolDistHistEncoder
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Apr 19, 2013 MvK Start implementation
 */
public class MolDistHistEncoder {
	
	private static MolDistHistEncoder INSTANCE;

	private DistHistEncoder distHistEncoder;
	
	public MolDistHistEncoder() {
		
		distHistEncoder = new DistHistEncoder();
		
	}
	
	public String encode(MolDistHist mdh){
		
		if(!mdh.isFinalized())
			mdh.realize();
				
		String strNodes = encodeByteVec(mdh.getArrNode());
				
		String strDistHist = distHistEncoder.encodeHistograms(mdh);
		
		return strNodes + " " + strDistHist;

	}
	
	public MolDistHist decode(byte [] arr){
		return decode(new String(arr));
	}
	
	public MolDistHist decode(String s){

		if(s.equals(DescriptorHandlerFlexophore.FAILED_STRING)){
			return DescriptorHandlerFlexophore.FAILED_OBJECT;
		}
		
		StringTokenizer st = new StringTokenizer(s);
		
		String sNodes = st.nextToken();
		
		String sHistograms = "";
		if(st.hasMoreTokens()){
			sHistograms = st.nextToken();
		}
		
		byte [] arrNodes = decodeNodes(sNodes);

		int nNodes=0;
		int pos=0;
		while(arrNodes[pos] > 0){
			pos += arrNodes[pos] * PPNode.NUM_BYTES_INTERACTION_TYPE + 1;
			
			nNodes++;
			
			if(pos >= arrNodes.length){
				break;
			}
		}
		
		byte [] arrNodesTrunc = new byte [pos];
		
		System.arraycopy(arrNodes, 0, arrNodesTrunc, 0, arrNodesTrunc.length);
				
		MolDistHist mdh = new MolDistHist(nNodes);
				
		mdh.setArrNode(arrNodesTrunc);
		
		if(sHistograms.length()>0)
			distHistEncoder.decodeHistograms(sHistograms, mdh);
				
		return mdh;

	}
	
	/**
	 * 
	 * @param s
	 * @return
	 */
	public static byte [] decodeNodes(String s){
		
		IntVec iv = new IntVec(new DescriptorEncoder().decode(s));
		
		byte [] arr = new byte [iv.sizeBytes()];
		
		int sum=0;
		for (int i = 0; i < arr.length; i++) {
			arr[i] = (byte)(iv.getByte(i) & 0xFF);
			sum += Math.abs(arr[i]);	
		}
		
		if(sum==0){
			throw new RuntimeException("Node vector contains only 0's!");
		}
		
		return arr;
	}

	public static MolDistHistEncoder getInstance(){
		
		if(INSTANCE == null){
			INSTANCE = new MolDistHistEncoder();
		}
		
		return INSTANCE;
	}

	private static String encodeByteVec(byte [] arr){
		
		double ratio = Integer.SIZE / Byte.SIZE;
		int sizeIntVec = (int)((arr.length / ratio) + ((ratio-1) / ratio));
		IntVec iv = new IntVec(sizeIntVec);
		for (int i = 0; i < arr.length; i++) {
			iv.setByte(i, (arr[i] & 0xFF));
		}
		String s= new String (new DescriptorEncoder().encode(iv.get()));
		
		return s;
	}
	


}
