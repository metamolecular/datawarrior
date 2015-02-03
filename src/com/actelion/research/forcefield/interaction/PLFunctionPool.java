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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;



/**
 * StatisticsResult contains the number of occurences of all pair of atomTypes
 * 
 */
public class PLFunctionPool {
	
	/** Map of atomType1-atomType2 -> int[] of occurences */
	private Set<String> allKeys = new TreeSet<String>();
	private Map<String, PLFunction> mapFunctions = new TreeMap<String, PLFunction>();
	
	public PLFunctionPool() {
	}
	
	
	public Set<String> getAllTypes() {
		return allKeys;
	}
	
	public PLFunction get(String key1Key2) {		
		return mapFunctions.get(key1Key2);
	}
	
	public void remove(String key1Key2) {		
		mapFunctions.remove(key1Key2);
	}
	public PLFunction getOrCreate(String key1Key2) {
		PLFunction f = mapFunctions.get(key1Key2);
		if(f==null) {
			String sp[] = key1Key2.split("-");
			if(sp.length!=2) {
				System.err.println("Invalid key: "+key1Key2);
			}
			allKeys.add(sp[0]);
			allKeys.add(sp[1]);
			
			f = new PLFunction(key1Key2);
			mapFunctions.put(key1Key2, f);			
		}
		return f;
	}

	
	/**
	 * Add an Interaction for the given atom pair (key) and at the given distance
	 * @param key
	 * @param dist
	 */
	public void addInteraction(String key1key2, double dist) {		
		if(dist>=PLFunctionSplineCalculator.CUTOFF_STATS) return;
		PLFunction f = getOrCreate(key1key2);
		
		int index = DiscreteFunction.distanceToIndex(dist);
		int[] occurences = f.getOccurencesArray();
		if(index<occurences.length) occurences[index]++;
	}	

	
	public int getTotalOccurences(String key1key2) {
		PLFunction occ = get(key1key2);
		return occ==null? 0: occ.getTotalOccurences();
	}
	
	public int getOccurences(String key1key2, int index) {
		PLFunction occ = get(key1key2);
		return occ==null? 0: occ.getOccurencesArray()[index];
	}
	
	public void setOccurences(String key1key2, int index, int value) {
		PLFunction f = getOrCreate(key1key2);
		int[] occurences = f.getOccurencesArray();
		if(index<occurences.length) occurences[index] = value;
		
	}
	
	public Set<String> getFunctionKeys() {
		return mapFunctions.keySet();
	}

	
	public void setPLOccurenceArray(String key1key2, int[] array) {
		PLFunction f = getOrCreate(key1key2);
		f.setOccurencesArray(array);
	}
	
	//////////////////// IO FUNCTIONS ////////////////////////////////////
	public void write(String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		//Write the distance pair interactions
		for(String key1key2: getFunctionKeys()) {
			PLFunction occ = mapFunctions.get(key1key2);
			writer.write(key1key2.replace(' ', '_'));
			for(int index=0; index<occ.getOccurencesArray().length; index++) {
				writer.write(" "+occ.getOccurencesArray()[index]);						
			}
			writer.write(System.getProperty("line.separator"));			
		}
		
		writer.write(System.getProperty("line.separator"));		
		writer.close();
	
	}


	public static PLFunctionPool read(boolean includeHydrogens) {
		try {
			URL url =  PLFunctionPool.class.getResource("/resources/forcefield/" + KeyAssigner.getParameterFile());
			if(url==null) {
				System.err.println("Could not find the interactions parameter file in the classpath: "+"/resources/forcefield/" + KeyAssigner.getParameterFile());
				return new PLFunctionPool();
			}
			return read(url.openStream(), includeHydrogens);
		} catch(Exception e) {
			e.printStackTrace();
			return new PLFunctionPool();
		}
	}
	
	public static PLFunctionPool read(InputStream is) throws IOException {
		return read(is, true);
	}
	public static PLFunctionPool read(InputStream is, boolean includeHydrogens) throws IOException {
		PLFunctionPool pool = new PLFunctionPool();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line;
		
		while((line = reader.readLine())!=null) {
			if(line.length()==0) break;
			StringTokenizer st = new StringTokenizer(line);			
			String key = st.nextToken();
			
			//Skip hydrogens?
			if(!includeHydrogens) {
				String s[] = key.split("-");
				if(KeyAssigner.isHydrogen(s[0]) || KeyAssigner.isHydrogen(s[1])) continue;
			}
			
			PLFunction f = pool.getOrCreate(key);
			for(int index=0; st.hasMoreTokens() && index<f.getOccurencesArray().length; index++) {
				int occ = Integer.parseInt(st.nextToken());
				f.getOccurencesArray()[index] = occ;
			}
		}
	
		is.close();
		PLFunctionSplineCalculator.calculateSplines(pool);
		return pool;
	}

}