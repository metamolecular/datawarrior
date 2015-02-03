/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Joel Freyss
 */
package com.actelion.research.forcefield.interaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.calculator.StructureCalculator;

public abstract class AbstractKeyAssigner {
	
	/**
	 * The name of the file in resources/forcefield
	 * @return
	 */
	public abstract String getParameterFile();
	
	/**
	 * the character '-' is not allowed
	 * @param mol
	 * @param a
	 * @return
	 */
	public abstract String getSuperKey(FFMolecule mol, int a);
	
	/**
	 * Gets a subkey = superkey + " " + more info
	 * the character '-' is not allowed  
	 * @param mol
	 * @param a
	 * @return
	 */
	public abstract String getSubKey(FFMolecule mol, int a);

	
	/**
	 * Return the mm2 type from the mm2parameters file
	 * @param subKey
	 * @return
	 */
	public String getMM2Type(String subKey) {
		String[] split = subKey.split("\\_");
		if(split.length<1) return null; 
		return split[0].substring(split[0].indexOf('*')+1);
	}

	/**
	 * Return the "acceptor", "donor", "carbon", "occupied" type
	 * @param subKey
	 * @return
	 */
	public String getPPPType(String subKey) {
		if(subKey.startsWith("1*")) return "hydrogen";
		if(subKey.startsWith("6*")) return "carbon";
		if(subKey.startsWith("7*") || subKey.startsWith("8*") || subKey.startsWith("16*")) return "doneptor";
		return "occupied";
	}
	
	public boolean isHydrogen(String key) {
		return key.startsWith("1*"); 
	}
	
	/**
	 * Concrete Implementation
	 * @author freyssj
	 *
	 */
	public static class ACD7 extends AbstractKeyAssigner {
		@Override
		public String getParameterFile() { return "inter_acd7.txt"; }
		
		@Override
		public String getSuperKey(FFMolecule mol, int a) {		
			int an = mol.getAtomicNo(a);
			return an+"*"+(mol.getAtomMM2Description(a)==null?"": mol.getAtomMM2Description(a).replace(" ", ""));
		}
			
		/**
		 * Gets a subkey = superkey + " " + more info
		 * the character '-' is not allowed  
		 * @param mol
		 * @param a
		 * @return
		 */
		@Override
		public String getSubKey(FFMolecule mol, int a)  {			
			String k1 = getSuperKey(mol, a);
			String k2 = getInfos(mol, a);
			
			return k1 + "_" + k2;
		}
		
		private static String getCode(FFMolecule mol, int a) {
			if(mol.getAtomicNo(a)<=1) return null;
			String s = Molecule.cAtomLabel[mol.getAtomicNo(a)];
			return s;
		}
		
		/**
		 * Creates a code for each atom based on the neighboring atoms
		 * 
		 * NON AROMATIC ATOM
		 * The code equals neighbor1(neighborsOfNeighbor1)neighbor2(neighborsOfneighbor2)neighbor3(neighborsOfNeighbor3)HnumberOfH
		 * Ex:
		 *         C
		 *       /
		 * O = C
		 *       \
		 *         OH
		 * The code of O is "=C(CO)"
		 * The code of central C is "=OCO(H)"
		 * The code of OH is "C(=OC)H"
		 *        
		 * 
		 * 
		 * 
		 * AROMATIC ATOM
		 * The code equals "{atoms in the ring if <6}neighbors"
		 * Ex:
		 * 
		 * 	H - C - C5N
		 * 
		 * the code of C is {CCCCCN}H1
		 * 
		 * 
		 * @param mol
		 * @param a
		 * @return
		 */
		private static String getInfos(FFMolecule mol, int a){
			//Compute neighboring atoms
			int an = mol.getAtomicNo(a);
			if(an!=6 && an!=7 && an!=8 && an!=16) return "";
			
			if(mol.isAromatic(a)) {			

				List<String> set = new ArrayList<String>();
				for(int ringNo: mol.getAtomToRings()[a]) {
					if(!mol.isAromaticRing(ringNo)) continue;
					for(int a2: mol.getAllRings().get(ringNo)) {
						if(mol.getAtomicNo(a2)==6) continue;
						String s = getCode(mol, a2);
						if(s!=null) set.add(s);
					}
				}
				StringBuilder atomsInAromaticRing = new StringBuilder();
				if(set.size()<=6) {
					Collections.sort(set);
					for (String s : set) {
						atomsInAromaticRing.append(s);
					}
				}

				set.clear();			
				for (int i = 0; i < mol.getConnAtoms(a); i++) {
					int a2 = mol.getConnAtom(a, i);
					if(mol.getAtomicNo(a2)<=1 || a==a2 || mol.isAromatic(a2)) continue;
					String s = (mol.getConnBondOrder(a, i)==1?"": mol.getConnBondOrder(a, i)==2?"=": "#") + getCode(mol, a2);
					set.add(s);
				}
				Collections.sort(set);

				StringBuilder sb2 = new StringBuilder();
				int nH = StructureCalculator.getImplicitHydrogens(mol, a) + StructureCalculator.getExplicitHydrogens(mol, a);
				sb2.append(nH>0? "H" + nH: "");
				for (String s : set) sb2.append(s);
				return sb2+"{"+atomsInAromaticRing.toString()+"}";

			} else {
				List<String> set1 = new ArrayList<String>();
				for (int i = 0; i < mol.getConnAtoms(a); i++) {
					int a2 = mol.getConnAtom(a, i);
					if(mol.getAtomicNo(a2)<=1) continue;
					StringBuilder sb2 = new StringBuilder();
					sb2.append((mol.getConnBondOrder(a, i)==1?"": mol.getConnBondOrder(a, i)==2?"=": "#") + getCode(mol, a2));				
					if((mol.getAtomicNo(a)==8 || mol.getAtomicNo(a)==7) && mol.getAtomicNo(a2)==6) { //Polar - C - ...
						List<String> set2 = new ArrayList<String>();
						for (int j = 0; j < mol.getConnAtoms(a2); j++) {
							int a3 = mol.getConnAtom(a2, j);
							if(a3==a || mol.getAtomicNo(a)==6) continue;
							set2.add((mol.getConnBondOrder(a2, j)==1?"": mol.getConnBondOrder(a2, j)==2?"=": "#")+getCode(mol, a3));
						}
						Collections.sort(set2);
						StringBuilder sb3 = new StringBuilder();
//						int nH = StructureCalculator.getImplicitHydrogens(mol, a2) + StructureCalculator.getExplicitHydrogens(mol, a2);
//						sb3.append(nH>0? "H" + nH: "");
						for (String s : set2) sb3.append(s);
						sb2.append( "(" + sb3 + ")");  						
					}	
					set1.add(sb2.toString());
				}
				StringBuilder sb1 = new StringBuilder();
				int nH = StructureCalculator.getImplicitHydrogens(mol, a) + StructureCalculator.getExplicitHydrogens(mol, a);
				sb1.append(nH>0? "H" + nH: "");
				Collections.sort(set1);
				for (String s : set1) sb1.append(s);
				
				return sb1.toString();
			}

		}
		
	}
}
