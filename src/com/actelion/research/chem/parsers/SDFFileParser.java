package com.actelion.research.chem.parsers;

import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.MolfileV3Creator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.calculator.AdvancedTools;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.util.IOUtils;

/**
 * SDFFileParser is a proxy to the SDFFileParser inheriting from AbstractParser
 * The coordinates are read exactly as they appear in the file (opposed to SDFileParser)
 * @author freyssj
 */
public class SDFFileParser extends AbstractParser {
	
	public boolean optimize3D = true;
	
	public SDFFileParser() {}	

	@Override
	public List<FFMolecule> loadGroup(String fileName, Reader reader, int from, int to) throws Exception {
		
		List<FFMolecule> res = new ArrayList<FFMolecule>();
		int size = 4000000;
		PushbackReader in = new PushbackReader(reader, size+1);
		
		String content = IOUtils.readerToString(in, size);
		
		CharArrayReader header = new CharArrayReader(content.toCharArray());
		SDFileParser parser = new SDFileParser(header);
		int nameIndex = -1;
		String[] fieldNames = parser.getFieldNames();
		if(fieldNames!=null && fieldNames.length>0) {
			for (int i = 0; i < fieldNames.length; i++) {
				if(fieldNames[i].equalsIgnoreCase("IDNUMBER")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("SUBSTANCE")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("Supplier-ID")) nameIndex = i;
				if(fieldNames[i].equalsIgnoreCase("ACTELION NO")) {nameIndex = i;break;}
				if(fieldNames[i].equalsIgnoreCase("code")) nameIndex = i;
				
				
				System.out.println("SDF > fieldNames["+i+"]="+fieldNames[i]);
			}
			in.unread(content.toCharArray());
			header.close();
			parser = new SDFileParser(in, fieldNames);
		} else {
			in.unread(content.toCharArray());
			header.close();
			parser = new SDFileParser(in);
		}
				
		StereoMolecule mol = new StereoMolecule();
		MolfileParser p = new MolfileParser();
		int count = -1;
		MolfileParser.debug = true;
		while( parser.next()) {
			String s = parser.getNextMolFile();

			count++;
			if(count%5000==0 && count>0) {System.out.println("Read "+count+" files");}
			if(from>count) continue;
			if(to>=0 && to<count) break;
			
			mol = p.getCompactMolecule(s);
			if(mol==null) {
				System.err.println("Null mol returned??");
				continue;
			}
			//Name
			String name = nameIndex>=0? parser.getFieldData(nameIndex): null;
			if(name==null) name = "SDF"+(count+1);
			
			//3D optimization?
			List<FFMolecule> isomers;
			if(optimize3D && !is3D(mol)) {
				isomers = AdvancedTools.convertMolecule2DTo3D(mol);
			} else {
				isomers = Collections.singletonList(new FFMolecule(mol));
			}
			
			//Set Names, Auxiliary Infos
			for (int iso = 0; iso < isomers.size(); iso++) {
				FFMolecule m = isomers.get(iso);
				m.setName(name + (isomers.size()>1?" Iso"+(iso+1):""));	

				//Update 
				for (int i = 0; i < fieldNames.length; i++) {
					String val = parser.getFieldData(i);
					if(val==null)  continue;
					m.getAuxiliaryInfos().put(fieldNames[i], val);
				}
				
				if(m.getAllAtoms()<120) m.setAllAtomFlag(FFMolecule.LIGAND, true);

				m.compact();
				res.add(m);
			}
			
			
		}		
		
		

		
		AbstractParser.convertDataToPrimitiveTypes(res);
		return res;
	}
	
	public void save(List<FFMolecule> mols, Writer w) throws Exception {
		boolean v3 = false;
		try {
			for(FFMolecule m : mols) {
				if(m.getAllAtoms()>=1000) v3 = true;
			}
		
			for (int i = 0; i < mols.size(); i++) {
				FFMolecule m = mols.get(i);			
				if(m==null) throw new IOException("Empty Molecule");
				try {
					if(v3) {
						new MolfileV3Creator(m.toStereoMolecule(), false).writeMolfile(w);
					} else {
						new MolfileCreator(m.toStereoMolecule(), false).writeMolfile(w);
					}
					for (String key : m.getAuxiliaryInfos().keySet()) {
						if(m.getAuxiliaryInfo(key).getClass().isArray()) continue;
						w.write("> <" + key + ">"+System.getProperty("line.separator"));
						w.write(m.getAuxiliaryInfo(key)+System.getProperty("line.separator"));
						w.write(System.getProperty("line.separator"));				
					} 
					w.write("$$$$");
					w.write(System.getProperty("line.separator"));
				} catch (Exception e) {
					throw new Exception("Cannot save file #" + (i+1) + ": "+m.getName()+": "+e);
				}
				
			}
		} finally {
			w.close();
		}
	}
	
	
	public static final ParserFileFilter FILEFILTER = new ParserFileFilter() {
		public boolean accept(File f) {
			return f.getName().toUpperCase().endsWith(".SDF")
			|| f.isDirectory();
		}

		public String getDescription() {
			return "MDL SDFile";
		}

		public String getExtension() {
			return ".sdf";
		}
	};

	public void setOptimize3D(boolean optimize3d) {
		optimize3D = optimize3d;
	}
	
	public boolean isOptimize3D() {
		return optimize3D;
	}

}
