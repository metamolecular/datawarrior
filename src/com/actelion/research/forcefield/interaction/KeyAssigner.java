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

import com.actelion.research.chem.FFMolecule;

/**
 * Strategy Pattern used to defined how keys are assigned to a specific atom
 */
public class KeyAssigner {
	
	private static AbstractKeyAssigner impl = new AbstractKeyAssigner.ACD7();
	
	public static void setImplementation(AbstractKeyAssigner _impl) {
		impl = _impl;
	}
	

	public static String getParameterFile()  {
		return impl.getParameterFile();
	}
	
	public static String getSuperKey(FFMolecule mol, int a) {
		return impl.getSuperKey(mol, a);
	}
	
	public static String getSubKey(FFMolecule mol, int a)  {
		return impl.getSubKey(mol, a);
	}
	
	public static String getMM2Type(String subKey) {
		return impl.getMM2Type(subKey);
	}

	public static String getPPPType(String subKey) {
		return impl.getPPPType(subKey);
	}
	
	public static boolean isHydrogen(String key) {
		return impl.isHydrogen(key);
	}


	/*
	private static String getInfos(FFMolecule mol, int a, int visited){
		//Compute neighboring atoms
		List<String> set = new ArrayList<String>();
		int an = mol.getAtomicNo(a);
		if(an!=6 && an!=7 && an!=8 && an!=16) return ""; //Follow C, N, O, S only
		
		
		if(mol.isAromatic(a) && visited<0) {			
			for(int ringNo: mol.getAtomToRings()[a]) {
				if(!mol.isAromaticRing(ringNo)) continue;
				for(int a2: mol.getAllRings().get(ringNo)) {
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
			StringBuilder sb2 = new StringBuilder();
			Collections.sort(set);
			for (String s : set) {
				sb2.append(s);
			}


			return "{"+atomsInAromaticRing.toString()+"}"+sb2;

		} else {
			for (int i = 0; i < mol.getConnAtoms(a); i++) {
				int a2 = mol.getConnAtom(a, i);
				if(mol.getAtomicNo(a2)<=1 || visited==a2) continue;
				String s = (mol.getConnBondOrder(a, i)==1?"": mol.getConnBondOrder(a, i)==2?"=": "#") + getCode(mol, a2);				
				if((mol.getAtomicNo(a)==8 || mol.getAtomicNo(a)==7) && mol.getAtomicNo(a2)==6 && visited<0) {
					s+= "("+getInfos(mol, a2, a)+")"; //Go to 2nd next neighbour only for N-C-?, O-C-?
				}				
				set.add(s);
			}
			StringBuilder sb = new StringBuilder();
			Collections.sort(set);
			for (String s : set) {
				sb.append(s);
			}
			return sb.toString();
		}

	}
*/	
	
}
