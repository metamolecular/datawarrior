/*
 * Created on Mar 10, 2005
 *
 */
package com.actelion.research.chem.parsers;

import java.io.*;
import java.text.*;
import java.util.*;

import com.actelion.research.chem.*;
import com.actelion.research.chem.calculator.BondsCalculator;

/**
 * 
 * @author freyssj
 */
public class XYZFileParser extends AbstractParser {

	/**
	 * @see com.actelion.research.chem.parsers.AbstractParser#load(java.io.Reader, com.actelion.research.chem.Molecule3D)
	 */
	public List<FFMolecule> loadGroup(String fileName, Reader in, int from, int to) throws Exception {
		FFMolecule mol = new FFMolecule();
		mol.setName(fileName);
		String line;
		BufferedReader reader = new BufferedReader(in);
		Map<Integer, Integer> xyzIdtoAtomIds = new HashMap<Integer, Integer>();
		reader.readLine(); //Skip the first line _(number of atoms)
		List<int[]> bonds = new ArrayList<int[]>();
		int count = 0;
		while ((line = reader.readLine()) != null && line.length()>7) {
			StringTokenizer st = new StringTokenizer(line);
			if(st.countTokens()<4) continue;
			String tok1 = st.nextToken();
			int xyzId;
			String label;
			try { 
				xyzId = Integer.parseInt(tok1);
				label = st.nextToken();
			} catch (Exception e) {
				xyzId = count++;
				label = tok1;
			}
			int atomicNo = Molecule.getAtomicNoFromLabel(label);
			int atomId = mol.addAtom(atomicNo);
			xyzIdtoAtomIds.put(xyzId, atomId);
	
			mol.setAtomX(atomId, Double.parseDouble(st.nextToken()));				 
			mol.setAtomY(atomId, Double.parseDouble(st.nextToken()));				 
			mol.setAtomZ(atomId, Double.parseDouble(st.nextToken()));
	
			if(st.hasMoreTokens()) st.nextToken(); //parse class
			while(st.hasMoreTokens()) {
				bonds.add(new int[]{xyzId, Integer.parseInt(st.nextToken())});					
			}					
		}
		//Add the bonds
		Iterator<int[]> iter = bonds.iterator();
		while(iter.hasNext()) {
			int[] bond = iter.next();
			int a1 = xyzIdtoAtomIds.get(bond[0]);
			int a2 = xyzIdtoAtomIds.get(bond[1]);
			mol.addBond(a1, a2, 1); 
							
		}	
		
		BondsCalculator.calculateBondOrders(mol, true);
				
		reader.close();		
		return Collections.singletonList(mol);
	}
	
	/**
	 * @see com.actelion.research.chem.parsers.AbstractParser#save(com.actelion.research.chem.Molecule3D, java.io.Writer)
	 */
	public void save(FFMolecule mol, Writer f) throws Exception {
		DecimalFormat df = new DecimalFormat("0.000000");
		f.write(format("" + mol.getAllAtoms(),6));
		f.write(System.getProperty("line.separator"));									
		for(int i=0; i<mol.getAllAtoms(); i++) {
			f.write(format("" + (i+1), 6));
			f.write(format(mol.getAtomicNo(i)>0?Molecule.cAtomLabel[mol.getAtomicNo(i)]:"Lp", 3));				
			f.write(format("" + df.format(mol.getAtomX(i)), 14));				
			f.write(format("" + df.format(mol.getAtomY(i)), 12));				
			f.write(format("" + df.format(mol.getAtomZ(i)), 12));
			f.write(format("" + mol.getAtomMM2Class(i), 6)); 
			for(int j=0; j<mol.getAllConnAtoms(i); j++) {
				int a = mol.getConnAtom(i, j);
				f.write(format("" + (a+1), 6));
			}
			f.write(System.getProperty("line.separator"));									
		}
		f.flush();
		f.close();
	}

	private static String format(String s, int len) {
		while(s.length()<len) s = " " + s;
		return s;
	}

	public static final ParserFileFilter FILEFILTER = new ParserFileFilter() {
		public boolean accept(java.io.File f) {
			return f.getName().toUpperCase().endsWith(".XYZ")
			|| f.isDirectory();
		}

		public String getDescription() {
			return "XYZ File";
		}

		public String getExtension() {
			return ".xyz";
		}
	};

}
