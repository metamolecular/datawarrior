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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.conf.VDWRadii;


/**
 * Set of tools to estimate the surfaces of FFMolecule (3D Coordinates set)
 */
public class SurfaceCalculator {

	public static double STEP = .8;
	private static final double PROBE = 1.4;
	private static final double offset = 4;
	
	public static int getLigandSAS(FFMolecule mol) {
		int[] complexedState = calculateSurface(mol, true);
		int vol = complexedState[complexedState.length-1];				
		return vol;
	}

	public static int getLigandHydrophobicSAS(FFMolecule mol) {
		int[] complexedState = calculateSurface(mol, true);		
		int total = 0;
		for (int i = 0; i < complexedState.length-1; i++) {
			if(mol.isAtomFlag(i, FFMolecule.LIGAND) && mol.getAtomicNo(i)==6) {
				total += complexedState[i];
			}
		}
		return total;
	}
	
	public static double getLigandBuried(FFMolecule mol) {
		int[] complexedState = calculateSurface(mol, true);		
		int[] solvatedState = calculateSurface(mol, false);		
		int total = 0;
		for (int i = 0; i < complexedState.length-1; i++) {
			if(mol.isAtomFlag(i, FFMolecule.LIGAND)) {
				total += solvatedState[i] - complexedState[i];
			}
		}
		return total;
	}

	/**
	 * Return the estimated ligand surface 
	 * @param mol
	 * @return
	 */
	public static int getLigandSurface(FFMolecule mol) {
		int[] solvatedState = calculateSurface(mol, false);
		return solvatedState[solvatedState.length-1];
	}
	
	public static double getLigandPolarSurface(FFMolecule mol) {
		int[] solvatedState = calculateSurface(mol, false);
		int total = 0;
		for (int i = 0; i < solvatedState.length-1; i++) {
			if(Molecule.isAtomicNoElectronegative(mol.getAtomicNo(i))) {
				total += solvatedState[i];
			}
		}
		return total;
	}	
	

	/**
	 * Calculates the Occupancy matrix
	 * 
	 */	
	public static int[][][] calculateOccupancy(FFMolecule mol, boolean complexedState, double R) {
		return calculateOccupancy(mol, complexedState, R, 1);
	}
	public static int[][][] calculateOccupancy(FFMolecule mol, boolean complexedState, double R, double step) {
		int[][][] occupancy; 
		MoleculeGrid grid = new MoleculeGrid(mol, 1.8);
		Coordinates[] bounds = StructureCalculator.getLigandBounds(mol);
		if(bounds==null) return new int[][][]{};
		double sx = bounds[0].x - offset;
		double sy = bounds[0].y - offset;
		double sz = bounds[0].z - offset;
		
		//Creates an occupancy matrix
		// occupancy[x][y][z] contains the number of the closest atom (within the vdw radius)
		//int volume = 0;
		occupancy = new int[(int)((bounds[1].x-bounds[0].x+offset*2+1)/step)][(int)((bounds[1].y-bounds[0].y+offset*2+1)/step)][(int)((bounds[1].z-bounds[0].z+offset*2+1)/step)];
		for (int x = 0; x < occupancy.length; x++) {
			for (int y = 0; y < occupancy[x].length; y++) {
				Arrays.fill(occupancy[x][y], -1);
				for (int z = 0; z < occupancy[x][y].length; z++) {
					Coordinates dot = new Coordinates(x*step+sx, y*step+sy, z*step+sz);
					
					double min = 1000;
					
					
					for(int a: grid.getNeighbours(dot, 3.4)) {
						if(mol.getAtomicNo(a)<=1) continue;
						if(mol.getAllConnAtoms(a)==0) continue;
						if(!complexedState && !mol.isAtomFlag(a, FFMolecule.LIGAND)) continue;
						if(mol.getAtomicNo(a)>=VDWRadii.VDW_RADIUS.length) continue; //How can this happen?
						double radius = VDWRadii.VDW_RADIUS[mol.getAtomicNo(a)];
						radius+= R>0? R: PROBE;
						double dist = dot.distance(mol.getCoordinates(a));
						if(radius<dist) continue;						
						if(radius-dist<min) {
							occupancy[x][y][z] = a;
							min = radius-dist;
						}
						
					}
					//if(occupancy[x][y][z]>=0 && mol.isAtomFlag(occupancy[x][y][z], FFMolecule.LIGAND)) volume++;
					
				}						
			}						
		}
		return occupancy;
	}

	/**
	 * Calculates the Solvent Accessible Surface
	 * This returns an array of size mol.getAllAtoms()+1
	 * 	array[i] = number of SA dots for the atom i
	 * 	array[mol.getAllAtoms] = total of dots
	 * 
	 * @param mol
	 * @param complexedState - whether it has to be calculated in a complexed state 
	 * 	or in a solvated state
	 * @return
	 */
	public static int[] calculateSurface(FFMolecule mol, boolean complexedState) {
		//int N = mol.getNMovables();		
		int[][][] occupancy = calculateOccupancy(mol, complexedState, 1.4); 

		//Calculates the exposed surface of each atom
		//A dot is part of the surface if one of its neighbour is not occupied
		int total = 0;
		int[] exposed = new int[mol.getAllAtoms()+1];
		for (int x = 0; x < occupancy.length; x++) {
			for (int y = 0; y < occupancy[x].length; y++) {
				for (int z = 0; z < occupancy[x][y].length; z++) {
			
					if(occupancy[x][y][z]<0 || !mol.isAtomFlag(occupancy[x][y][z], FFMolecule.LIGAND)) continue;
					if(free(occupancy, x+1, y, z) || free(occupancy, x-1, y, z) 
						|| free(occupancy, x, y+1, z) || free(occupancy, x, y-1, z)
						|| free(occupancy, x, y, z+1) || free(occupancy, x, y, z-1)) {
						total++; exposed[occupancy[x][y][z]]++;
					}																		
				}				
			}
		}
		exposed[mol.getAllAtoms()] = total;
		
		return exposed;
		
	}
	
	private static Object[] fill(int[][][] occ, int x0, int y0, int z0, int v) {
		if(occ[x0][y0][z0]==v) return null;
		int fill = occ[x0][y0][z0];

		int tot = 0;		
		boolean reachBorder = false;
		List<int[]> q = new LinkedList<int[]>();
		q.add(new int[]{x0,y0,z0});
		while(!q.isEmpty()) {
			int[] p = q.remove(0);
			int x = p[0];
			int y = p[1];
			int z = p[2];
			
			if(x<0 || x>=occ.length || y<0 || y>=occ[x].length || z<0 || z>=occ[x][y].length) {
				reachBorder = true;
				continue;
			} 
			if(occ[x][y][z]!=fill) continue;
			
			occ[x][y][z] = v;		
			tot++;			
			q.add(new int[]{x-1, y, z});			
			q.add(new int[]{x, y-1, z});			
			q.add(new int[]{x, y, z-1});			
			q.add(new int[]{x+1, y, z});			
			q.add(new int[]{x, y+1, z});			
			q.add(new int[]{x, y, z+1});						
		}
		return new Object[]{ tot, reachBorder};
	}
	
	/**
	 * Estimates the cavitation effect (i.e. the empty space that cannot be occupied by water)
	 * We suppose that water occupies a sphere of 1.4A of radius
	 * @param mol
	 * @return
	 */
	public static double calculateCavitation(FFMolecule mol, List<Coordinates> probes) {

		int Nlig = mol.getNMovables();
		if(probes==null) probes = new ArrayList<Coordinates>();
		if(Nlig<0) return 0;		

		Coordinates[] bounds = StructureCalculator.getLigandBounds(mol);
		double sx = bounds[0].x - offset;
		double sy = bounds[0].y - offset;
		double sz = bounds[0].z - offset;

		final double vol = STEP*STEP;
		int[][][] occupancy = calculateOccupancy(mol, true, 1.4, STEP); 
		
		double total = 0;
		for (int x = 1; x < occupancy.length-1; x++) {
			for (int y = 1; y < occupancy[x].length-1; y++) {
				for (int z = 1; z < occupancy[x][y].length-1; z++) {
					if(occupancy[x][y][z]!=-1) continue;
					if( (occupancy[x-1][y][z]>=Nlig || occupancy[x-1][y][z]<0) && 
						(occupancy[x][y-1][z]>=Nlig || occupancy[x][y-1][z]<0) && 
						(occupancy[x][y][z-1]>=Nlig || occupancy[x][y][z-1]<0) &&
						(occupancy[x+1][y][z]>=Nlig || occupancy[x+1][y][z]<0) && 
						(occupancy[x][y+1][z]>=Nlig || occupancy[x][y+1][z]<0) && 
						(occupancy[x][y][z+1]>=Nlig || occupancy[x][y][z+1]<0)) continue;
					Object[] o = fill(occupancy, x, y, z, -2);
					int size = (Integer)o[0];
					boolean reachBorder = (Boolean)o[1];
					if(!reachBorder) {
						if(size>0 && size<15) total+=vol;
					} else {
						fill(occupancy, x, y, z, 0);
					}
					
					
				}
			}
		}
		for (int x = 1; x < occupancy.length-1; x++) {
			for (int y = 1; y < occupancy[x].length-1; y++) {
				for (int z = 1; z < occupancy[x][y].length-1; z++) {
					if(occupancy[x][y][z]==-2) probes.add(new Coordinates(x*STEP+sx, y*STEP+sy, z*STEP+sz));
				}
			}
		}
		
		return total;
					
	}
	
	/**
	 * Estimates the cavitation effect (i.e. the empty space that cannot be occupied by water)
	 * We suppose that water occupies a sphere of 1.4A of radius
	 * @param mol
	 * @return
	 */
	public static double calculateHydrophobic(FFMolecule mol, List<Coordinates> probes) {

		int Nlig = mol.getNMovables();
		if(Nlig<0) return 0;		

		Coordinates[] bounds = StructureCalculator.getLigandBounds(mol);
		double sx = bounds[0].x - offset;
		double sy = bounds[0].y - offset;
		double sz = bounds[0].z - offset;
		final double vol = STEP*STEP;
		int[][][] occupancy = calculateOccupancy(mol, true, 1.4, STEP); 
		
		double total = 0;
		for (int x = 1; x < occupancy.length-1; x++) {
			for (int y = 1; y < occupancy[x].length-1; y++) {
				for (int z = 1; z < occupancy[x][y].length-1; z++) {
					if(occupancy[x][y][z]!=-1) continue;
					
					
					if( (occupancy[x-1][y][z]>=0 && occupancy[x-1][y][z]<Nlig && isHydrophobic(mol, occupancy[x-1][y][z])) ||
							(occupancy[x+1][y][z]>=0 && occupancy[x+1][y][z]<Nlig && isHydrophobic(mol, occupancy[x+1][y][z])) ||
							(occupancy[x][y-1][z]>=0 && occupancy[x][y-1][z]<Nlig && isHydrophobic(mol, occupancy[x][y-1][z])) ||
							(occupancy[x][y+1][z]>=0 && occupancy[x][y+1][z]<Nlig && isHydrophobic(mol, occupancy[x][y+1][z])) ||
							(occupancy[x][y][z-1]>=0 && occupancy[x][y][z-1]<Nlig && isHydrophobic(mol, occupancy[x][y][z-1])) ||
							(occupancy[x][y][z+1]>=0 && occupancy[x][y][z+1]<Nlig && isHydrophobic(mol, occupancy[x][y][z+1]))) {
						
						if(probes!=null) probes.add(new Coordinates(x*STEP+sx, y*STEP+sy, z*STEP+sz));
						total+=vol;
					}
					
				}
			}
		}
		
		return total;
					
	}
	
	private static boolean isHydrophobic(FFMolecule mol, int a) {
		return mol.getAtomicNo(a)==6 && !mol.isAromatic(a);
	}

	/**
	 * Estimates the cavitation effect (i.e. the empty space that cannot be occupied by water)
	 * We suppose that water occupies a sphere of 1.4A of radius
	 * @param mol
	 * @return
	 */
	public static double calculateSAS(FFMolecule mol, List<Coordinates> probes) {

		int Nlig = mol.getNMovables();
		if(Nlig<=0) return 0;		

		Coordinates[] bounds = StructureCalculator.getLigandBounds(mol);
		if(bounds==null) return 0;
		double sx = bounds[0].x - offset;
		double sy = bounds[0].y - offset;
		double sz = bounds[0].z - offset;
		final double vol = STEP*STEP;
		int[][][] occupancy = calculateOccupancy(mol, true, 1.4, STEP); 
		
		double total = 0;
		for (int x = 1; x < occupancy.length-1; x++) {
			for (int y = 1; y < occupancy[x].length-1; y++) {
				for (int z = 1; z < occupancy[x][y].length-1; z++) {
					if(occupancy[x][y][z]!=-1) continue;
					
					
					if( (occupancy[x-1][y][z]>=0 && occupancy[x-1][y][z]<Nlig ) ||
							(occupancy[x+1][y][z]>=0 && occupancy[x+1][y][z]<Nlig ) ||
							(occupancy[x][y-1][z]>=0 && occupancy[x][y-1][z]<Nlig ) ||
							(occupancy[x][y+1][z]>=0 && occupancy[x][y+1][z]<Nlig ) ||
							(occupancy[x][y][z-1]>=0 && occupancy[x][y][z-1]<Nlig ) ||
							(occupancy[x][y][z+1]>=0 && occupancy[x][y][z+1]<Nlig )) {
						
						if(probes!=null) probes.add(new Coordinates(x*STEP+sx, y*STEP+sy, z*STEP+sz));
						total+=vol;
					}
					
				}
			}
		}
		
		return total;					
	}

	public static int[] calculateSurfaceByAtom(FFMolecule mol, boolean complexedState) {
		int N = mol.getAllAtoms();
		if(N<0) return new int[]{0,0};		

		int[][][] occupancy = calculateOccupancy(mol, complexedState, 1.4, STEP); 
		
		int[] res = new int[N];
		for (int x = 1; x < occupancy.length-1; x++) {
			for (int y = 1; y < occupancy[x].length-1; y++) {
				for (int z = 1; z < occupancy[x][y].length-1; z++) {
					if(occupancy[x][y][z]!=-1) continue;
					
					
					if(occupancy[x-1][y][z]>=0 && occupancy[x-1][y][z]<N && mol.isAtomFlag(occupancy[x-1][y][z], FFMolecule.LIGAND)) res[occupancy[x-1][y][z]]++;
					else if(occupancy[x+1][y][z]>=0 && occupancy[x+1][y][z]<N && mol.isAtomFlag(occupancy[x+1][y][z], FFMolecule.LIGAND) ) res[occupancy[x+1][y][z]]++;
					else if(occupancy[x][y-1][z]>=0 && occupancy[x][y-1][z]<N && mol.isAtomFlag(occupancy[x][y-1][z], FFMolecule.LIGAND) ) res[occupancy[x][y-1][z]]++;
					else if(occupancy[x][y+1][z]>=0 && occupancy[x][y+1][z]<N && mol.isAtomFlag(occupancy[x][y+1][z], FFMolecule.LIGAND) ) res[occupancy[x][y+1][z]]++;
					else if(occupancy[x][y][z-1]>=0 && occupancy[x][y][z-1]<N && mol.isAtomFlag(occupancy[x][y][z-1], FFMolecule.LIGAND) ) res[occupancy[x][y][z-1]]++;
					else if(occupancy[x][y][z+1]>=0 && occupancy[x][y][z+1]<N && mol.isAtomFlag(occupancy[x][y][z+1], FFMolecule.LIGAND) ) res[occupancy[x][y][z+1]]++;
				}
			}
		}
		
		return res;
		
	}


	public static double getVolume(FFMolecule mol) {		
		Coordinates[] bounds = StructureCalculator.getLigandBounds(mol);
		if(bounds==null) return 0;
		MoleculeGrid grid = new MoleculeGrid(mol);
		double vol = 0;
		for (double x = bounds[0].x; x <= bounds[1].x; x+=.5) {
			for (double y = bounds[0].y; y <= bounds[1].y; y+=.5) {
				loop: for (double z = bounds[0].z; z <= bounds[1].z; z+=.5) {
					Coordinates c = new Coordinates(x,y,z);
					Set<Integer> neighbours = grid.getNeighbours(c, 1.4);
	 				for (Iterator<Integer> iter = neighbours.iterator(); iter.hasNext();) {
						int a = iter.next().intValue();
						if(mol.getAtomicNo(a)>=VDWRadii.VDW_RADIUS.length) continue;
						double R = VDWRadii.VDW_RADIUS[mol.getAtomicNo(a)] + 0.9;
						if(mol.isAtomFlag(a, FFMolecule.LIGAND) && mol.getCoordinates(a).distSquareTo(c)<R*R) {
							vol += .5*.5*.5;
							continue loop;
						} 						
					}
				}
			}
		}
		return vol;
		
	}
	
	
	private final static boolean free(int[][][] occ, int x, int y, int z) {
		return x>=0 && y>=0 && z>=0 && x<occ.length && y<occ[x].length  && z<occ[x][y].length && occ[x][y][z]<0;
	}
	
}
