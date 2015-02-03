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
package com.actelion.research.chem.calculator;

/**
 * 
 */
public class ProteinTools {

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static char getAminoLetter(String s) {
		if(s==null) return '?';
		s = s.toUpperCase();
		if(s.equals("GLY")) return 'G';
		if(s.equals("ALA")) return 'A';
		if(s.equals("VAL")) return 'V';
		if(s.equals("LEU")) return 'L';
		if(s.equals("ILE")) return 'I';
		if(s.equals("MET")) return 'M';
		if(s.equals("PRO")) return 'P';
		if(s.equals("PHE")) return 'F';
		if(s.equals("TRP")) return 'W';
		if(s.equals("SER")) return 'S';
		if(s.equals("THR")) return 'T';
		if(s.equals("ASN")) return 'N';
		if(s.equals("GLN")) return 'Q';
		if(s.equals("CYS")) return 'C';
		if(s.equals("TYR")) return 'Y';
		if(s.equals("LYS")) return 'K';
		if(s.equals("ARG")) return 'R';
		if(s.equals("HIS")) return 'H';
		if(s.equals("ASP")) return 'D';
		if(s.equals("GLU")) return 'E';
		if(s.equals("MSE")) return 'm';
		if(s.equals("STA")) return 's'; //non human?
		if(s.equals("LTA")) return 't'; //non human?
		if(s.equals("IVA")) return 'i'; //non human?
		return '?';	
	}
	
	/**
	 * Return a letter describing the property of the amino
	 *  - s for small hydrophobic
	 *  - l for large hydrophobic
	 *  - p for polar
	 *  - c for charged
	 * @param a
	 * @return
	 */
	public static char getAminoClass(char a) {
		switch(a) {
			case 'G': case 'A': return 's';
			case 'V': case 'L': case 'I': case 'M': case 'P': case 'F': case 'W': return 'l';
			case 'S': case 'T': case 'N': case 'Q': case 'C': case 'Y': return 'p';
			case 'K': case 'R': case 'H': case 'D': case 'E': return 'c';
			default: return '?';
		}
	}
}
