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
 * @author Thomas Sander
 */

package com.actelion.research.chem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.TreeMap;

import com.actelion.research.chem.io.CompoundFileParser;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.io.SDFileParser;


public class AtomTypeList {
    private static final String VERSION_STRING = "AtomTypeList v1.1";

    private TreeMap<Long,Integer> mList;
	private float[]	              mRingSizeAdjust;
	private int                   mAtomTypeMode;

    public AtomTypeList() {
        mRingSizeAdjust = new float[8];
        mList = new TreeMap<Long,Integer>();
        }

    public AtomTypeList(String filename, int mode) throws Exception {
        this();

        if (filename.endsWith(".typ")) {
	        BufferedReader theReader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(filename)));
	        String version =theReader.readLine();
	        if (!VERSION_STRING.equals(version)) {
	            throw new Exception("Outdated atom type list file.");
	            }

	        mAtomTypeMode = Integer.parseInt(theReader.readLine());
	        if (mAtomTypeMode != mode) {
	            throw new Exception("Incompatible atom type mode.");
	            }

	        for (int i=0; i<8; i++)
	            mRingSizeAdjust[i] = Float.parseFloat(theReader.readLine());

	        while (true) {
	            String theLine = theReader.readLine();
	            if (theLine == null)
	                break;

	            int tab = theLine.indexOf('\t');
	            mList.put(new Long(Long.parseLong(theLine.substring(0, tab))),
	                      new Integer(Integer.parseInt(theLine.substring(tab+1))));
	            }
	        theReader.close();
	        }
		}


	/**
	 * Creates an AtomTypeList by parsing all atom types of the given substance file
	 * @param filename a valid xxx.dwar or xxx.sdf file
	 */
	public void create(String filename) {
	    create(filename, AtomTypeCalculator.cPropertiesAll);
		}


	public void create(String filename, int mode) {
		mAtomTypeMode = mode;
		int index = filename.lastIndexOf('.');
		if (index != -1) {
		    String typeFileName = filename.substring(0, index)+".typ";
    		if (filename.endsWith(".sdf"))
    		    createTypeFile(new SDFileParser(filename), typeFileName);
    		else if (filename.endsWith(".dwar") || filename.endsWith(".ode"))
    		    createTypeFile(new DWARFileParser(filename), typeFileName);
		    }
		}


	public void createTypeFile(CompoundFileParser parser, String typeFileName) {
        for (int i=0; i<8; i++)
            mRingSizeAdjust[i] = 0.0f;

        while (parser.next())
		    processMolecule(parser);

		float ringSum = 0.0f;
		for (int i=0; i<8; i++)
			ringSum += mRingSizeAdjust[i];
		for (int i=0; i<8; i++)
			mRingSizeAdjust[i] /= ringSum;

		try {
		    BufferedWriter writer = new BufferedWriter(new FileWriter(typeFileName));
		    writer.write(VERSION_STRING);
		    writer.newLine();

	        writer.write(""+mAtomTypeMode);
            writer.newLine();

            for (int i=0; i<8; i++) {
	            writer.write(""+mRingSizeAdjust[i]);
	            writer.newLine();
	            }

            for (Long type:mList.keySet()) {
		        writer.write(type.toString()+"\t"+mList.get(type).toString());
                writer.newLine();
		        }
		    writer.close();
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}


	public TreeMap<Long,Integer> getAtomTypeList() {
	    return mList;
	    }


	private void processMolecule(CompoundFileParser parser) {
		try {
		    StereoMolecule mol = parser.getMolecule();
		    if (mol != null) {
				mol.stripIsotopInfo();
				mol.ensureHelperArrays(Molecule.cHelperNeighbours);
	
				for(int atom=0;atom<mol.getAtoms();atom++)
					addAtomType(AtomTypeCalculator.getAtomType(mol, atom, mAtomTypeMode));
	
				RingCollection ringSet = mol.getRingSet();
				for (int ring=0; ring<ringSet.getSize(); ring++)
					mRingSizeAdjust[ringSet.getRingSize(ring)]++;
		    	}
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}


	private void addAtomType(Long atomType) {
	    Integer count = mList.get(atomType);
	    if (count == null)
	        mList.put(atomType, new Integer(1));
	    else
            mList.put(atomType, new Integer(count.intValue()+1));
		}


	public int getTotalFromType(long type){
	    return mList.get(new Long(type)).intValue();
		}


	public float getRingSizeAdjust(int ringSize) {
		return mRingSizeAdjust[ringSize];
		}
	}
