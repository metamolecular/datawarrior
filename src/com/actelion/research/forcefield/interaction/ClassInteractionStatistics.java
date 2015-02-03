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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.mm2.MM2Parameters;

/**
 * Simplifies the retrieval of StatisticalPreference
 * @author freyssj
 */
public class ClassInteractionStatistics {
	
	/**
	 * Classes that are used for compatibility reasons, so that atom types are not changed with future versions.
	 */
	private static final String[] orderedClasses = new String[]{"12*MG", "12*MG_", "15*PPHOSPHATE", "15*PPHOSPHATE_", "16*SSULFONE", "16*SSULFONE_=C", "16*SSULFONE_=O=OCC", "16*SSULFONE_=O=OCN", "16*SSULFONE_=O=OCO", "16*SSULFONE_=O=ONN", "16*SSULFONE_=O=ONO", "16*SSULFONE_=O=OOO", "16*STHIOETHER", "16*STHIOETHER_CC", "16*STHIOETHER_CFe", "16*STHIOETHER_CS", "16*STHIOETHER_FeFe", "16*STHIOL", "16*STHIOL_H1C", "16*STHIOL_H1P", "16*STHIOPHENE", "16*STHIOPHENE_{NNS}", "16*STHIOPHENE_{NS}", "16*STHIOPHENE_{S}", "17*CL", "17*CL_", "25*MN", "25*MN_", "26*FEOCTAHEDRAL", "26*FEOCTAHEDRAL_", "30*ZNTRIGPLANAR", "30*ZNTRIGPLANAR_", "35*BR", "35*BR_", "42*MOTETRAHEDRAL", "42*MOTETRAHEDRAL_", "5*BTRIGPLANAR", "5*BTRIGPLANAR_", "53*I", "53*I_", "6*CALKANE", "6*CALKANE_CCCC", "6*CALKANE_CCCN", "6*CALKANE_CCCO", "6*CALKANE_CCCS", "6*CALKANE_CCFF", "6*CALKANE_CCNO", "6*CALKANE_CCOO", "6*CALKANE_CFFF", "6*CALKANE_CFFP", "6*CALKANE_FFFO", "6*CALKANE_H1CCC", "6*CALKANE_H1CCF", "6*CALKANE_H1CCN", "6*CALKANE_H1CCO", "6*CALKANE_H1CCS", "6*CALKANE_H1CNN", "6*CALKANE_H1CNO", "6*CALKANE_H1CNP", "6*CALKANE_H1CNS", "6*CALKANE_H1COO", "6*CALKANE_H1COS", "6*CALKANE_H1FFO", "6*CALKANE_H1NNN", "6*CALKANE_H1NNO", "6*CALKANE_H1NNS", "6*CALKANE_H2CC", "6*CALKANE_H2CCl", "6*CALKANE_H2CF", "6*CALKANE_H2CN", "6*CALKANE_H2CO", "6*CALKANE_H2CP", "6*CALKANE_H2CS", "6*CALKANE_H2NN", "6*CALKANE_H2NO", "6*CALKANE_H2OO", "6*CALKANE_H2OP", "6*CALKANE_H2PP", "6*CALKANE_H3C", "6*CALKANE_H3N", "6*CALKANE_H3O", "6*CALKANE_H3S", "6*CALKANE_H4", "6*CALKENE", "6*CALKENE_=CCC", "6*CALKENE_=CCN", "6*CALKENE_=CCO", "6*CALKENE_=CCS", "6*CALKENE_=CNN", "6*CALKENE_=CNO", "6*CALKENE_=Cl{}", "6*CALKENE_=C{NN}", "6*CALKENE_=C{N}", "6*CALKENE_=C{}", "6*CALKENE_=NCC", "6*CALKENE_=NCN", "6*CALKENE_=NCO", "6*CALKENE_=NNN", "6*CALKENE_=N{NN}", "6*CALKENE_=N{}", "6*CALKENE_Br{NN}", "6*CALKENE_Br{}", "6*CALKENE_Cl{NN}", "6*CALKENE_Cl{N}", "6*CALKENE_Cl{S}", "6*CALKENE_Cl{}", "6*CALKENE_C{NNN}", "6*CALKENE_C{NNO}", "6*CALKENE_C{NN}", "6*CALKENE_C{NO}", "6*CALKENE_C{NS}", "6*CALKENE_C{N}", "6*CALKENE_C{O}", "6*CALKENE_C{S}", "6*CALKENE_C{}", "6*CALKENE_F{NN}", "6*CALKENE_F{N}", "6*CALKENE_F{}", "6*CALKENE_H1=CC", "6*CALKENE_H1=CN", "6*CALKENE_H1=CO", "6*CALKENE_H1=NC", "6*CALKENE_H1=NN", "6*CALKENE_H1{NNN}", "6*CALKENE_H1{NN}", "6*CALKENE_H1{NO}", "6*CALKENE_H1{NS}", "6*CALKENE_H1{N}", "6*CALKENE_H1{O}", "6*CALKENE_H1{S}", "6*CALKENE_H1{}", "6*CALKENE_H2=C", "6*CALKENE_I{}", "6*CALKENE_N{NNN}", "6*CALKENE_N{NNS}", "6*CALKENE_N{NN}", "6*CALKENE_N{NO}", "6*CALKENE_N{NS}", "6*CALKENE_N{N}", "6*CALKENE_N{S}", "6*CALKENE_N{}", "6*CALKENE_O{NNN}", "6*CALKENE_O{NN}", "6*CALKENE_O{NS}", "6*CALKENE_O{N}", "6*CALKENE_O{O}", "6*CALKENE_O{S}", "6*CALKENE_O{}", "6*CALKENE_S{NNN}", "6*CALKENE_S{NNS}", "6*CALKENE_S{NN}", "6*CALKENE_S{NS}", "6*CALKENE_S{N}", "6*CALKENE_S{S}", "6*CALKENE_S{}", "6*CALKENE_{NNNNN}", "6*CALKENE_{NNNN}", "6*CALKENE_{NNN}", "6*CALKENE_{NNO}", "6*CALKENE_{NNS}", "6*CALKENE_{NN}", "6*CALKENE_{NO}", "6*CALKENE_{NS}", "6*CALKENE_{N}", "6*CALKENE_{O}", "6*CALKENE_{S}", "6*CALKENE_{}", "6*CALKYNE", "6*CALKYNE_#CC", "6*CCARBONYL", "6*CCARBONYL_=OCC", "6*CCARBONYL_=OCN", "6*CCARBONYL_=OCO", "6*CCARBONYL_=OCS", "6*CCARBONYL_=ONN", "6*CCARBONYL_=ONO", "6*CCARBONYL_=SNN", "6*CCARBONYL_H1=OC", "6*CCARBONYL_H1=ON", "6*CCYCLOPROPANE", "6*CCYCLOPROPANE_CCCC", "6*CCYCLOPROPANE_CCCN", "6*CCYCLOPROPANE_CCCO", "6*CCYCLOPROPANE_H1CCC", "6*CCYCLOPROPANE_H1CCN", "6*CCYCLOPROPANE_H1CCO", "6*CCYCLOPROPANE_H2CC", "6*CCYCLOPROPENE", "6*CISONITRILE", "6*CISONITRILE_#NC", "6*CISONITRILE_#NFe", "7*NAMIDE", "7*NAMIDE_C()C()C(=CC)", "7*NAMIDE_C()C(=CC)S", "7*NAMIDE_C()C(=CN)C(=ON)", "7*NAMIDE_C()C(=OC)C(=ON)", "7*NAMIDE_C(=C)C(C)C(CO)", "7*NAMIDE_C(=CC)C(=CN)C(C)", "7*NAMIDE_C(=CC)C(=OC)C(C)", "7*NAMIDE_C(=CC)C(C)C(C)", "7*NAMIDE_C(=CN)C(C)C(C)", "7*NAMIDE_C(=NC)C(C)C(C)", "7*NAMIDE_C(=NC)C(CO)C(N)", "7*NAMIDE_C(=OC)C(C)C(C)", "7*NAMIDE_C(=OC)C(C)C(CC)", "7*NAMIDE_C(=ON)C(C)C(CC)", "7*NAMIDE_C(=ON)C(C)C(CO)", "7*NAMIDE_H1C()C(=OC)", "7*NAMIDE_H1C(=CC)C(=CC)", "7*NAMIDE_H1C(=CC)C(=CN)", "7*NAMIDE_H1C(=CC)C(=NC)", "7*NAMIDE_H1C(=CC)C(=NN)", "7*NAMIDE_H1C(=CC)C(=NS)", "7*NAMIDE_H1C(=CC)C(=OC)", "7*NAMIDE_H1C(=CC)C(=ON)", "7*NAMIDE_H1C(=CC)C(C)", "7*NAMIDE_H1C(=CC)C(CC)", "7*NAMIDE_H1C(=CC)C(N)", "7*NAMIDE_H1C(=CC)C(NN)", "7*NAMIDE_H1C(=CC)N", "7*NAMIDE_H1C(=CC)O", "7*NAMIDE_H1C(=CC)S", "7*NAMIDE_H1C(=CN)C(=ON)", "7*NAMIDE_H1C(=CN)C(C)", "7*NAMIDE_H1C(=CN)C(CC)", "7*NAMIDE_H1C(=CN)C(CO)", "7*NAMIDE_H1C(=CN)C(N)", "7*NAMIDE_H1C(=NC)C(=ON)", "7*NAMIDE_H1C(=NC)C(C)", "7*NAMIDE_H1C(=NC)C(CC)", "7*NAMIDE_H1C(=NC)C(CO)", "7*NAMIDE_H1C(=NN)C(C)", "7*NAMIDE_H1C(=NN)C(CC)", "7*NAMIDE_H1C(=NS)C(=OC)", "7*NAMIDE_H1C(=OC)C(=ON)", "7*NAMIDE_H1C(=OC)C(C)", "7*NAMIDE_H1C(=OC)C(CC)", "7*NAMIDE_H1C(=OC)C(CO)", "7*NAMIDE_H1C(=OC)C(NO)", "7*NAMIDE_H1C(=OC)N", "7*NAMIDE_H1C(=OC)O", "7*NAMIDE_H1C(=OC)S", "7*NAMIDE_H1C(=ON)C(C)", "7*NAMIDE_H1C(=ON)C(CC)", "7*NAMIDE_H1C(=ON)C(CO)", "7*NAMIDE_H1C(=OO)C(CC)", "7*NAMIDE_H2C(=CC)", "7*NAMIDE_H2C(=CN)", "7*NAMIDE_H2C(=NC)", "7*NAMIDE_H2C(=NN)", "7*NAMIDE_H2C(=NS)", "7*NAMIDE_H2C(=OC)", "7*NAMIDE_H2C(=ON)", "7*NAMIDE_H2C(=OO)", "7*NAMINE", "7*NAMINE_C()C()C(C)", "7*NAMINE_C()C()C(CC)", "7*NAMINE_C()C(C)C(C)", "7*NAMINE_C(C)C(C)C(C)", "7*NAMINE_C(C)C(C)C(CC)", "7*NAMINE_C(C)C(C)C(CCC)", "7*NAMINE_C(C)C(C)C(CO)", "7*NAMINE_C(C)C(CC)C(CC)", "7*NAMINE_H1C()C(CC)", "7*NAMINE_H1C(C)C(C)", "7*NAMINE_H1C(C)C(CC)", "7*NAMINE_H1C(C)C(CCC)", "7*NAMINE_H1C(C)C(CO)", "7*NAMINE_H1C(C)N", "7*NAMINE_H1C(CC)C(CC)", "7*NAMINE_H1C(CC)C(CO)", "7*NAMINE_H1C(CC)C(NO)", "7*NAMINE_H1C(CC)C(NS)", "7*NAMINE_H1C(CN)C(N)", "7*NAMINE_H1C(CO)C(NO)", "7*NAMINE_H1PP", "7*NAMINE_H2C(C)", "7*NAMINE_H2C(CC)", "7*NAMINE_H2C(CCC)", "7*NAMINE_H2C(CN)", "7*NAMINE_H2C(CO)", "7*NAMINE_H2C(NN)", "7*NAMINE_H2N", "7*NAMINE_H3", "7*NAMMONIUM", "7*NAMMONIUM_C()C()C()C(C)", "7*NCONNAROMATIC", "7*NCONNAROMATIC_=C(CC)C(=CC)", "7*NCONNAROMATIC_=C(CC)C(=CN)", "7*NCONNAROMATIC_=C(CC)N", "7*NCONNAROMATIC_=NC(=CC)", "7*NCONNAROMATIC_H1=C(NN)", "7*NGUANIDINE", "7*NGUANIDINE_H1=C(NN)", "7*NGUANIDINE_H1C(=NN)C(C)", "7*NGUANIDINE_H1C(=NN)C(CC)", "7*NGUANIDINE_H2C(=NN)", "7*NIMINE", "7*NIMINE_=C(C)N", "7*NIMINE_=C(C)O", "7*NIMINE_=C(CC)C(=CC)", "7*NIMINE_=C(CC)N", "7*NIMINE_=C(CO)C(=CO)", "7*NIMINE_=C(CO)C(CC)", "7*NIMINE_=NC(=CC)", "7*NIMINE_=NC(=NC)", "7*NIMINE_=NC(=OC)", "7*NIMINE_=NC(C)", "7*NIMINE_H1=C(CN)", "7*NIMINE_H1=C(CO)", "7*NIMMONIUM", "7*NIMMONIUM_=C(CC)C()C(=N)", "7*NIMMONIUM_=C(N)C(=NC)C(CO)", "7*NIMMONIUM_C{NNN}", "7*NIMMONIUM_C{NN}", "7*NIMMONIUM_C{NS}", "7*NIMMONIUM_C{N}", "7*NIMMONIUM_O{N}", "7*NIMMONIUM_{N}", "7*NNITRILE", "7*NNITRILE_#C(C)", "7*NNITRILE_#C(Fe)", "7*NOXAZOLE", "7*NOXAZOLE_{NNNN}", "7*NOXAZOLE_{NNN}", "7*NOXAZOLE_{NNO}", "7*NOXAZOLE_{NNS}", "7*NOXAZOLE_{NN}", "7*NOXAZOLE_{NO}", "7*NOXAZOLE_{NS}", "7*NPYRIDINE", "7*NPYRIDINE_{N}", "7*NPYRIMIDINE", "7*NPYRIMIDINE_{NNN}", "7*NPYRIMIDINE_{NN}", "7*NPYRROLE", "7*NPYRROLE_C{NNN}", "7*NPYRROLE_C{NN}", "7*NPYRROLE_C{N}", "7*NPYRROLE_Fe{NN}", "7*NPYRROLE_Fe{N}", "7*NPYRROLE_H1{NNNN}", "7*NPYRROLE_H1{NNN}", "7*NPYRROLE_H1{NN}", "7*NPYRROLE_H1{N}", "7*NPYRROLE_N{N}", "7*NPYRROLE_{NNN}", "7*NPYRROLE_{NN}", "7*NPYRROLE_{N}", "7*NSULFONAMIDE", "7*NSULFONAMIDE_C(C)C(C)S", "7*NSULFONAMIDE_C(C)C(CC)S", "7*NSULFONAMIDE_H1C(=CC)S", "7*NSULFONAMIDE_H1C(C)S", "7*NSULFONAMIDE_H2S", "8*OALCOHOL", "8*OALCOHOL_H1C(=CN)", "8*OALCOHOL_H1C(=CO)", "8*OALCOHOL_H1C(=NC)", "8*OALCOHOL_H1C(=NN)", "8*OALCOHOL_H1C(C)", "8*OALCOHOL_H1C(CC)", "8*OALCOHOL_H1C(CCC)", "8*OALCOHOL_H1C(CCO)", "8*OALCOHOL_H1C(CN)", "8*OALCOHOL_H1C(CO)", "8*OALCOHOL_H1C(CPP)", "8*OALCOHOL_H1C(NN)", "8*OALCOHOL_H1N", "8*OALCOHOL_H1O", "8*OALCOHOL_H1S", "8*OAMIDE", "8*OAMIDE_=C(CN)", "8*OAMIDE_=C(N)", "8*OAMIDE_=C(NN)", "8*OAMIDE_=C(NO)", "8*OAMIDE_=S", "8*OCARBONYL", "8*OCARBONYL_=C(C)", "8*OCARBONYL_=C(CO)", "8*OCARBONYL_=C(CS)", "8*OCARBOXYL", "8*OCARBOXYL_=C(CO)", "8*OCARBOXYL_=S", "8*OCARBOXYL_C()C(=CC)", "8*OCARBOXYL_C()C(=OC)", "8*OCARBOXYL_C()C(CC)", "8*OCARBOXYL_C()C(CO)", "8*OCARBOXYL_C(=CC)C(=CC)", "8*OCARBOXYL_C(=CC)C(=NC)", "8*OCARBOXYL_C(=CC)C(=OC)", "8*OCARBOXYL_C(=CC)C(C)", "8*OCARBOXYL_C(=CC)C(CC)", "8*OCARBOXYL_C(=CC)C(CCC)", "8*OCARBOXYL_C(=CC)C(CO)", "8*OCARBOXYL_C(=CC)C(FF)", "8*OCARBOXYL_C(=CC)C(FFF)", "8*OCARBOXYL_C(=CC)C(O)", "8*OCARBOXYL_C(=NC)C(C)", "8*OCARBOXYL_C(=OC)C(C)", "8*OCARBOXYL_C(=OC)C(CC)", "8*OCARBOXYL_C(=OC)C(CCC)", "8*OCARBOXYL_C(=ON)C(C)", "8*OCARBOXYL_C(=ON)C(CC)", "8*OCARBOXYL_C(=ON)C(CCC)", "8*OCARBOXYL_C(C)C(CC)", "8*OCARBOXYL_C(C)C(CO)", "8*OCARBOXYL_C(CC)C(CC)", "8*OCARBOXYL_C(CC)C(CCO)", "8*OCARBOXYL_C(CC)C(CN)", "8*OCARBOXYL_C(CC)C(CO)", "8*OCARBOXYL_C(CC)C(CS)", "8*OCARBOXYL_C(CC)S", "8*OCARBOXYL_C(CCN)C(CN)", "8*OCARBOXYL_C(CCO)C(CO)", "8*OCARBOXYL_C(CO)C(CO)", "8*OCARBOXYL_C(CO)O", "8*OCARBOXYL_H1C(=OC)", "8*OCARBOXYL_H1S", "8*OENOL", "8*OENOL_H1C(=CC)", "8*OENOL_H1N", "8*OETHER", "8*OETHER_C()C(C)", "8*OETHER_C(=C)C(=C)", "8*OETHER_C(=C)C(C)", "8*OETHER_C(C)C(C)", "8*OETHER_C(C)C(CCO)", "8*OETHER_C(C)C(N)", "8*OETHER_C(C)N", "8*OETHER_C(C)S", "8*OETHER_OO", "8*OFURAN", "8*OFURAN_{NNO}", "8*OFURAN_{NO}", "8*OFURAN_{O}", "8*OOXO", "8*OOXO_=C(CC)", "8*OOXO_=S", "8*OPHOSPHATE", "8*OPHOSPHATE_C(=CC)P", "8*OPHOSPHATE_C(=OC)P", "8*OPHOSPHATE_C(C)P", "8*OPHOSPHATE_C(CC)P", "8*OPHOSPHATE_C(CCO)P", "8*OPHOSPHATE_C(CO)P", "8*OPHOSPHATE_OP", "8*OPHOSPHATE_P", "8*OPHOSPHATE_PP", "8*OWATER", "8*OWATER_H2", "9*F", "9*F_", "92*???", "92*???_"};
	private static final Map<Boolean,ClassInteractionStatistics> instances = new HashMap<Boolean, ClassInteractionStatistics>();

	
	
	private PLFunctionPool pool;
	private Map<String, Integer> keyToClassId = new HashMap<String, Integer>(); 
	private int classIdParents[]; 
	private Map<Integer, PLFunction> functions = new HashMap<Integer, PLFunction>();	 

	
	
	public static ClassInteractionStatistics getInstance () {
		return getInstance(false);
	}
	@SuppressWarnings("null")
	public static ClassInteractionStatistics getInstance (boolean includeHydrogen) {
		ClassInteractionStatistics instance = instances.get(includeHydrogen);
		if(instance==null) {
			synchronized (instances) {
				if(instance==null) { 
					try {				
						instance = new ClassInteractionStatistics(includeHydrogen);
						instances.put(includeHydrogen, instance);
					} catch(Exception e) {
						e.printStackTrace();
					}			
				}
			}
		}		
		return instance;
	}
	
	public int getNClasses() {
		return classIdParents.length;
	}
	
	private ClassInteractionStatistics(boolean includeHydrogens) {
		StringBuilder sb = new StringBuilder();
		pool = PLFunctionPool.read(includeHydrogens);
		
		//0.Retrieves memorized classes
		int clas = 0;
		for (String key: orderedClasses) {
			sb.append((sb.length()>0?",":"")+"\""+key+"\"");						
			keyToClassId.put(key, clas++);
		}
		
		//1. Gets all the classes
		Set<String> allKeys = new TreeSet<String>();
		for(String key: pool.getFunctionKeys()) {
			StringTokenizer st = new StringTokenizer(key, "-");
			if(st.countTokens()!=2) {System.err.println("Invalid key: "+key);continue;}
			String key1 = st.nextToken();
			String key2 = st.nextToken();
			allKeys.add(key1);
			allKeys.add(key2);		
		}
		
		//2. Creates a Map of key to classId
		for(String key: allKeys) {
			if(keyToClassId.get(key)!=null) continue; 
			sb.append((sb.length()>0?",":"")+"\""+key+"\"");						
			keyToClassId.put(key, clas++);
		}
		int N = clas;		
		classIdParents = new int[N];
		
		//3. Creates a Map of parents	
		for (String key : allKeys) {
			int claz = keyToClassId.get(key);
			String parentKey = key.indexOf('_')<0? null: key.substring(0, key.indexOf('_'));
			if(parentKey!=null) {
				classIdParents[claz] = getClassId(parentKey);
			} else {
				classIdParents[claz] = claz;
			}
			if(classIdParents[claz]<0) {
				classIdParents[claz] = claz;
			} 
		}
		
		//4. Populates the functions array
//		functions = new PLFunction[N][N];
		for(String key: pool.getFunctionKeys()) {
			if(pool.get(key).getSpline()==null) continue;			
			StringTokenizer st = new StringTokenizer(key, "-");
			if(st.countTokens()!=2) continue;
			String key1 = st.nextToken();
			String key2 = st.nextToken();
//			functions[getClassId(key1)][getClassId(key2)] = pool.get(key);
			if(functions.get((getClassId(key1)<<16) + getClassId(key2))!=null) {
				throw new IllegalArgumentException("The same key is used twice");
			}
			functions.put((getClassId(key1)<<16) + getClassId(key2), pool.get(key));
		}
	}	
	
	public int getClassId(FFMolecule mol, int i) {
		int classId = getClassId(KeyAssigner.getSubKey(mol, i));
		if(classId<0) {			
			classId = getClassId(KeyAssigner.getSuperKey(mol, i));
		}
		return classId;
	}

	public int getClassId(String description) {
		Integer i = keyToClassId.get(description);
		return i==null?-1: i.intValue();
	}
	
	public String getDescription(int claz) {		
		for (String key : keyToClassId.keySet()) {
			if(keyToClassId.get(key)==claz) {
				return key;
			}
		}
		return null;
	}

	public PLFunction getFunction(int key1, int key2) {
//		if(key1<0 || key2<0 || key1>=functions.length || key2>= functions[key1].length) return null;
//		
//		PLFunction function = functions[key1][key2];
//		if(function!=null && function.getSpline()!=null) return function;
//
//		int keyParent1 = classIdParents[key1];
//		int keyParent2 = classIdParents[key2];		
//
//		PLFunction function1 = functions[keyParent1][key2];
//		if(function1!=null && function1.getSpline()!=null) return function1;
//		
//		PLFunction function2 = functions[key1][keyParent2];
//		if(function2!=null && function2.getSpline()!=null) return function1;
//		
//		function = functions[keyParent1][keyParent2];
//
//		return function;
		
		
		if(key1<0 || key2<0) return null;
		PLFunction function = functions.get((key1<<16)+key2);
		if(function!=null && function.getSpline()!=null) return function;

		int keyParent1 = classIdParents[key1];
		int keyParent2 = classIdParents[key2];		

		PLFunction function1 = functions.get((keyParent1<<16)+key2);
		if(function1!=null && function1.getSpline()!=null) return function1;
		
		PLFunction function2 = functions.get((key1<<16)+keyParent2);
		if(function2!=null && function2.getSpline()!=null) return function1;
		
		function = functions.get((keyParent1<<16)+keyParent2);

		return function;

	}
	
	public void evaluateMolecule(FFMolecule mol) {	
		MM2Parameters.getInstance().setAtomClassesForMolecule(mol);
		int[] lvl = new int[5]; 		
		for (int i = 0; i < mol.getNMovables(); i++) {
			for (int j = mol.getNMovables(); j < mol.getAllAtoms(); j++) {
								
				if(mol.getAtomicNo(i)<=1) continue;
				
				int key1 = getClassId(mol, j); 
				int key2 = getClassId(mol, i);
				
				if(key1<0) {
					System.out.println("no class for "+KeyAssigner.getSuperKey(mol, j));
					lvl[0]++; continue;
				}
				if(key2<0) {
					System.out.println("no class for "+KeyAssigner.getSuperKey(mol, i));
					lvl[0]++; continue;
				}

				PLFunction function = functions.get((key1<<16)+key2);
				if(function!=null && function.getSpline()!=null) {lvl[4]++; continue;}

				int keyParent1 = classIdParents[key1];
				int keyParent2 = classIdParents[key2];		
				function = functions.get((keyParent1<<16)+key2);
				if(function!=null && function.getSpline()!=null) {lvl[3]++; continue;}
				function = functions.get((key1<<16)+keyParent2);
				if(function!=null && function.getSpline()!=null) {lvl[3]++; continue;}
				function = functions.get((keyParent1<<16)+keyParent2);
				if(function!=null && function.getSpline()!=null) {lvl[2]++; continue;}
				
				{lvl[1]++; continue;}				 
				
			}			
		}

		System.out.println("Function evaluation: ");
		System.out.println(" Lvl_0 (no key):  "+lvl[0]);
		System.out.println(" Lvl_1 (nothing): "+lvl[1]);
		System.out.println(" Lvl_2 (parent):  "+lvl[2]);
		System.out.println(" Lvl_3 (mix):     "+lvl[3]);
		System.out.println(" Lvl_4 (sub):     "+lvl[4]);
		System.out.println();
		
	}
	
	
	public int getParent(int claz) {
		return classIdParents[claz];
	}

	/**
	 * Assigns the atomClassId for the molecule
	 * (assigns also the atom types if it has not been set up)
	 * @param mol
	 */
	public void setClassIdsForMolecule(FFMolecule mol) {
		if(mol.getAllAtoms()==0) return;
		if(mol.getAtomMM2Description(0)==null) MM2Parameters.getInstance().setAtomClassesForMolecule(mol);
		for(int i=0; i<mol.getAllAtoms(); i++) {
			mol.setAtomInteractionClass(i, getClassId(mol, i));
		}
	}
	

	/**
	 * Print the current order of classes (to make backward compatibilities)
	 * @param args
	 */
	public static void main(String[] args) {
		ClassInteractionStatistics s = new ClassInteractionStatistics(false);
		System.out.println("N classes="+s.getNClasses());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.getNClasses(); i++) {
			if(i>0) sb.append(", ");
			sb.append("\"" + s.getDescription(i) + "\"");			
		}
		System.out.println("orderedClasses = new String[]{"+sb+"}");
	}
}
