package com.actelion.research.chem.properties.complexity;

import com.actelion.research.util.BurtleHasher;
import com.actelion.research.util.BurtleHasherABC;

/**
 * BitArray128Factory
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Nov 20, 2014 MvK Start implementation
 */
public class BitArray128Factory implements IBitArrayFactory<BitArray128> {
	
	private BurtleHasherABC burtleHasherABC;
	
	/**
	 * 
	 */
	public BitArray128Factory() {
		burtleHasherABC = new BurtleHasherABC(0, 0, 0);


	}
	
	/* (non-Javadoc)
	 * @see com.actelion.research.chem.properties.complexity.IBitArrayCreator#getNew()
	 */
	@Override
	public BitArray128 getNew(int index) {
		return new BitArray128(index);
	};
	
	public void calculateHash(BitArray128 f){
		
		burtleHasherABC.a = f.l1;
		burtleHasherABC.b = f.l2;
		burtleHasherABC.c = 0;
		
		BurtleHasher.mix64(burtleHasherABC);
		
		f.setHash((int)burtleHasherABC.c);
		
	}


}
