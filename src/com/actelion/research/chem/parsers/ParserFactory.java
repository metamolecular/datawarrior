/*
 * Created on Dec 20, 2004
 *
 */
package com.actelion.research.chem.parsers;

import java.util.List;

import com.actelion.research.chem.*;

/**
 * 
 * @author freyssj
 */
public class ParserFactory {

	public static FFMolecule parse(String fileName) throws Exception {
		AbstractParser parser = getParser(fileName);
		if(parser==null) throw new Exception("Unknown file extension");
		return parser.load(fileName);
	}

	public static List<FFMolecule> parseGroup(String fileName) throws Exception {
		AbstractParser parser = getParser(fileName);
		if(parser==null) throw new Exception("Unknown file extension");
		return parser.loadGroup(fileName);
	}
		
	public static AbstractParser getParser(String fileName) {
		AbstractParser parser = null;
		if(fileName==null) return null;
		String f = fileName.toLowerCase();
		if(f.endsWith(".gz")) f = f.substring(0, f.length()-3);
		if(f.endsWith(".ent")) {
			parser = new PDBFileParser(); 
		} else if(f.endsWith(".pdb")) {
			parser = new PDBFileParser(); 
		} else if(f.endsWith(".pqr")) {
			parser = new PDBFileParser(); 
		} else if(f.endsWith(".mol2")) {
			parser = new Mol2FileParser(); 
		} else if(f.endsWith(".mol")) {
			parser = new MolFileParser(); 
		} else if(f.endsWith(".xyz")) {
			parser = new XYZFileParser(); 
		} else if(f.endsWith(".sdf")) {
			parser = new SDFFileParser(); 
		} else if(f.endsWith(".ode")) {
			parser = new ODEFileParserWrapper(); 
		} else if(f.endsWith(".dwar")) {
			parser = new DWARFileParserWrapper(); 
		}		
		return parser;
	}
	
}
