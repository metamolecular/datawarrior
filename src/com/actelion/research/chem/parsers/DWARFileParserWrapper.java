package com.actelion.research.chem.parsers;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.calculator.AdvancedTools;
import com.actelion.research.chem.io.DWARFileParser;


/**
 * SDFFileParser is a proxy to the SDFFileParser inheriting from AbstractParser
 * 
 * @author freyssj
 */
public class DWARFileParserWrapper extends AbstractParser {
	
	private int MAX_RECORDS = 200000;
	private boolean loadAllIsomers = true;
	
	public DWARFileParserWrapper() {}	

	@Override
	public List<FFMolecule> loadGroup(String fileName, Reader in, int from, int to) throws Exception {
		this.errors.clear();
		List<FFMolecule> res = new ArrayList<FFMolecule>();

		DWARFileParser parser = new DWARFileParser(in);
		int nameIndex = -1;		
		
		String[] fieldNames = parser.getFieldNames();		
		if(fieldNames!=null ) {
			for (int i = 0; i < fieldNames.length; i++) { 
				if(fieldNames[i].equalsIgnoreCase("IDNUMBER")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("SUBSTANCE")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("Supplier-ID")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("code")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("Name")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("Actelion No")) {nameIndex = i;break;}
				if(fieldNames[i].equalsIgnoreCase("Actelion-No")) {nameIndex = i;break;}
			}
		}
		
		int nameSpecial = -1;		
		for( String key : parser.getSpecialFieldMap().keySet()) {
			if(key.equalsIgnoreCase("IDNUMBER")) nameSpecial = parser.getSpecialFieldMap().get(key).fieldIndex;
			if(key.equalsIgnoreCase("SUBSTANCE")) nameSpecial = parser.getSpecialFieldMap().get(key).fieldIndex;
			if(key.equalsIgnoreCase("Supplier-ID")) nameSpecial = parser.getSpecialFieldMap().get(key).fieldIndex;
			if(key.equalsIgnoreCase("code")) nameSpecial = parser.getSpecialFieldMap().get(key).fieldIndex;
			if(key.equalsIgnoreCase("Name")) nameSpecial = parser.getSpecialFieldMap().get(key).fieldIndex;
			if(key.equalsIgnoreCase("Actelion No")) {nameSpecial = parser.getSpecialFieldMap().get(key).fieldIndex;break;}
			if(key.equalsIgnoreCase("Actelion-No")) {nameSpecial = parser.getSpecialFieldMap().get(key).fieldIndex;break;}
		}
		StereoMolecule mol = new StereoMolecule();
		
//		System.out.println("name index= "+nameIndex+" or "+nameSpecial);
		int count = -1;
		List<String> errors = new ArrayList<String>();
		while(parser.next()) {
			count++;
			if(count%5000==0 && count>0) {System.out.println("Read "+count+" files");}
			if(from>count) continue;
			if(to>=0 && to<count) break;
			//if(count%2500==0 &&count>0) System.out.println("loaded "+count+" records");
			if(count>MAX_RECORDS) {
				System.err.println("Too many records in file");
				break;
			}
			String idcode = parser.getIDCode();
			String coords = parser.getCoordinates();
			
			try {
				new IDCodeParser(true).parse(mol, idcode, coords);			
			} catch (Exception e) {
				continue;
			}			
			mol.ensureHelperArrays(Molecule.cHelperCIP);

			mol.setName(parser.getMoleculeName());
			if(nameIndex>=0) mol.setName(parser.getFieldData(nameIndex));
			if(nameSpecial>=0) mol.setName(parser.getSpecialFieldData(nameSpecial));			
			
			if(mol.getName()==null) mol.setName("#"+(count+1));
			
			if(is3D(mol)) {
				FFMolecule m = new FFMolecule(mol);
				m.compact();
				m.setAllAtomFlag(FFMolecule.LIGAND, true);				
				res.add(m);
			} else {
				List<FFMolecule> stereoIsomers = AdvancedTools.orientMoleculeForParity(mol, loadAllIsomers, errors);
				for(FFMolecule m: stereoIsomers) {				
					m.setName(mol.getName());
					if(idcode!=null) m.setAuxiliaryInfo("Structure", idcode);
					if(coords!=null) m.setAuxiliaryInfo("idcoordinates2D", coords);
					
					for (int i = 0; i < fieldNames.length; i++) {
						if(i==nameIndex)  continue;
						String val = parser.getFieldData(i);		
						if(val!=null && val.length()>0) m.getAuxiliaryInfos().put(fieldNames[i], val);
					}
					
					m.compact();
					m.setAllAtomFlag(FFMolecule.LIGAND, true);
					
					res.add(m);
				}
			}
		}		

		this.errors = errors;
		AbstractParser.convertDataToPrimitiveTypes(res);		
		return res;
	}
	
	
	@Override
	public void save(List<FFMolecule> records, Writer w) throws Exception {
		if(records.size()==0) {
			System.err.println("Nothing to save");
			return;
		}
		
		Set<String> infoKeys = new TreeSet<String>();
		boolean showMol = false; 
		for (FFMolecule record : records) {
			infoKeys.addAll(record.getAuxiliaryInfos().keySet());
			if(record.getAllAtoms()>0) showMol = true;
		}
		List<String> headers = new ArrayList<String>(infoKeys);
		
		final String NEWLINE = System.getProperty("line.separator");
		if(showMol) {
			w.write("<datawarrior-fileinfo>"+NEWLINE);
			w.write("<version=\"3.0\">"+NEWLINE);
			w.write("<rowcount=\"226\">"+NEWLINE);
			w.write("</datawarrior-fileinfo>"+NEWLINE);
			w.write("<column properties>"+NEWLINE);
			w.write("<columnName=\"idcoordinates2D\">"+NEWLINE);
			w.write("<columnProperty=\"specialType	idcoordinates2D\">"+NEWLINE);
			w.write("<columnProperty=\"parent	Structure\">"+NEWLINE);
			w.write("<columnName=\"Structure\">"+NEWLINE);
			w.write("<columnProperty=\"specialType	idcode\">"+NEWLINE);
			w.write("</column properties>"+NEWLINE);
			w.write("Name\tStructure\tidcoordinates2D\t");
		}
		for (int i = 0; i < headers.size(); i++) {
			if(headers.get(i).equals("Structure") || headers.get(i).equals("idcoordinates2D")) continue;
			w.write(headers.get(i));
			if(i<headers.size()-1) w.write("\t");
		}
		w.write(System.getProperty("line.separator"));
		
		for (FFMolecule r: records) {
			if(showMol) {
				Canonizer canon = new Canonizer(r.toExtendedMolecule());
				w.write(r.getName()!=null? r.getName():"");
				w.write("\t");
				w.write(r.getAuxiliaryInfo("Structure")!=null? (String) r.getAuxiliaryInfo("Structure"): canon.getIDCode());
				w.write("\t");
				w.write(r.getAuxiliaryInfo("idcoordinates2D")!=null? (String) r.getAuxiliaryInfo("idcoordinates2D"): canon.getEncodedCoordinates(true));
				w.write("\t");
			}
			for (int i = 0; i < headers.size(); i++) {
				if(r.getAuxiliaryInfo(headers.get(i))!=null) w.write(""+r.getAuxiliaryInfo(headers.get(i)));			
				if(i<headers.size()-1) w.write("\t");
			}
			w.write(System.getProperty("line.separator"));			
		}	
	} 
		
	/**
	 * @param loadAllIsomers the loadAllIsomers to set
	 */
	public void setLoadAllIsomers(boolean loadAllIsomers) {
		this.loadAllIsomers = loadAllIsomers;
	}

	/**
	 * @return the loadAllIsomers
	 */
	public boolean isLoadAllIsomers() {
		return loadAllIsomers;
	}

	public static final ParserFileFilter FILEFILTER = new ParserFileFilter() {
		@Override
		public boolean accept(File f) {
			return f.getName().toUpperCase().endsWith(".DWAR")
			|| f.isDirectory();
		}
		@Override
		public String getDescription() {
			return "DWAR File";
		}
		@Override
		public String getExtension() {
			return ".dwar";
		}
	};

}
