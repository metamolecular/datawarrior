/*
 * Created on Jun 29, 2005
 *
 */
package com.actelion.research.chem.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.SDFileParser;

/**
 * 
 * @author freyssj
 */
public class MolFileParser extends AbstractParser {

	/**
	 * @see com.actelion.research.chem.parsers.AbstractParser#load(java.io.Reader, com.actelion.research.chem.Molecule3D)
	 */
	public  List<FFMolecule> loadGroup(String fileName, Reader in, int from, int to) throws Exception {
		FFMolecule mol = new FFMolecule();
		mol.setName(fileName);
		try {
			new SDFileParser(in); 
			
			MolfileParser p = new MolfileParser();
			StereoMolecule m = new StereoMolecule();
			p.parse(m, new BufferedReader(in));
			mol.clear();
			mol.fusion(new FFMolecule(m));
			mol.setAllAtomFlag(FFMolecule.LIGAND, true);
			mol.compact();
			return Collections.singletonList(mol);
		} finally {
			try{in.close();} catch(IOException e) {}
		}		
	}
	
	/**
	 * @see com.actelion.research.chem.parsers.AbstractParser#save(com.actelion.research.chem.Molecule3D, java.io.Writer)
	 */
	public void save(FFMolecule m, Writer writer) throws Exception {
		new MolfileCreator(m.toExtendedMolecule(), false).writeMolfile(writer);
	}


	public static final ParserFileFilter FILEFILTER = new ParserFileFilter() {
		public boolean accept(java.io.File f) {
			return f.getName().toUpperCase().endsWith(".MOL")
			|| f.isDirectory();
		}

		public String getDescription() {
			return "MOL File";
		}

		public String getExtension() {
			return ".mol";
		}
	};

}
