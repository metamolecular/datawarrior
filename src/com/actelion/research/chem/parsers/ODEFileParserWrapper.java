package com.actelion.research.chem.parsers;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.ODEFileParser;

/**
 * SDFFileParser is a proxy to the SDFFileParser inheriting from AbstractParser
 * 
 * @author freyssj
 */
public class ODEFileParserWrapper extends AbstractParser {
	
	public ODEFileParserWrapper() {}	

	public List<FFMolecule> loadGroup(String fileName, Reader in, int from, int to) throws Exception {
		
		List<FFMolecule> res = new ArrayList<FFMolecule>();

		ODEFileParser parser = new ODEFileParser(in);
		String[] fieldNames = parser.getFieldNames();
		
		int nameIndex = 0;		
		if(fieldNames!=null && fieldNames.length>0) {
			for (int i = 0; i < fieldNames.length; i++) { 
				if(fieldNames[i].equalsIgnoreCase("IDNUMBER")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("SUBSTANCE")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("Supplier-ID")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("ACTELION NO")) {nameIndex = i;break;}
				if(fieldNames[i].equalsIgnoreCase("code")) nameIndex = i;
			}
		}
		
		StereoMolecule mol = new StereoMolecule();
		while(parser.moreRecordsAvailable()) {
			String name = parser.getFieldData(nameIndex);
			String idcode = parser.getIDCode();
			String coords = parser.getCoordinates();						
			try {
				new IDCodeParser(true).parse(mol, idcode, coords);					
			} catch (Exception e) {
				continue;
			}			

			FFMolecule m = new FFMolecule();			
			m.fusion(new FFMolecule(mol));
			m.setName(name);
			m.setAuxiliaryInfo("Structure", idcode);
			m.setAuxiliaryInfo("idcoordinates2D", coords);
			
			if(nameIndex>=0) m.setName(parser.getFieldData(nameIndex));			
			for (int i = 0; i < fieldNames.length; i++) {
				if(i==nameIndex)  continue;
				String val = parser.getFieldData(i);
				m.getAuxiliaryInfos().put(fieldNames[i], val);
			}
			
			m.compact();
			m.setAllAtomFlag(FFMolecule.LIGAND, true);
			
			res.add(m);
		}		

		AbstractParser.convertDataToPrimitiveTypes(res);		
		
		return res;
	}
	
	public void save(List<FFMolecule> records, Writer w) throws Exception {
		FFMolecule rec = records.get(0);
		boolean showMol = rec.getAllAtoms()>0; 
		String[] header = rec.getAuxiliaryInfos().keySet().toArray(new String[] {});
		for (int i = 0; i < header.length; i++) {
			if(header[i].equals("idcode") || header[i].equals("idccoords")) continue;
			w.write(header[i]+"\t");			
		}
		if(showMol) {
			w.write("idcode\tidcoords");
		}
		w.write(System.getProperty("line.separator"));
		
		for (FFMolecule r: records) {
			for (int i = 0; i < header.length; i++) {
				w.write(r.getAuxiliaryInfo(header[i])+"\t");			
			}
			if(showMol) {
				Canonizer canon = new Canonizer(r.toExtendedMolecule());
				w.write(canon.getIDCode()+"\t"+canon.getEncodedCoordinates(true));
			}
			w.write(System.getProperty("line.separator"));			
		}	
	} 
	
	public static final ParserFileFilter FILEFILTER = new ParserFileFilter() {
		public boolean accept(File f) {
			return f.getName().toUpperCase().endsWith(".ODE")
			|| f.isDirectory();
		}

		public String getDescription() {
			return "ODE File";
		}

		public String getExtension() {
			return ".ode";
		}
	};
	

}
