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
package com.actelion.research.forcefield;

import java.io.FileOutputStream;
import java.io.PrintStream;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;

/**
 * Aggregations of terms. The values of those terms are interpolated on a grid.
 * Terms that can be aggregated are those whose exactly one term is not rigid.
 * 
 * 
 * For reference check: http://astronomy.swin.edu.au/~pbourke/other/trilinear/
 * 
 *
 */
public class GridTerm extends AbstractTerm {
	
	private static class FGValue {
		FGValue(double x, double gx, double gy, double gz) {
			this.x = x;
			this.gx = gx;
			this.gy = gy;
			this.gz = gz;			
		}
		final double x;
		final double gx;
		final double gy;
		final double gz;
	}
	
	protected final double gridSize;
	protected final Coordinates[] bounds;
	protected final FGValue[][][] grid; 

	protected final TermList termsToAggregate;
	private final Coordinates[] g;
	protected final int evalAtom;

	/**
	 * Term that shares the property of an other term
	 * @param tl
	 * @param atom
	 * @param evalAtom
	 * @param termsToAggregate
	 * @param grid
	 * @param grad
	 * @param bounds
	 */
	public GridTerm(FFMolecule mol, int atom, GridTerm gt) {//int evalAtom, TermList termsToAggregate, FGValue[][][] grid, Coordinates[] bounds) {
		super(mol, new int[] {atom});
		
		this.bounds = gt.bounds;		
		this.evalAtom = gt.evalAtom;
		this.grid = gt.grid;		
		this.gridSize = gt.gridSize;		
		
		this.termsToAggregate = gt.termsToAggregate;		
		g = new Coordinates[evalAtom+1];
		g[evalAtom] = new Coordinates();
		
	
	}
	public GridTerm(FFMolecule mol, int atom, TermList termsToAggregate, Coordinates[] bounds, double gridSize) {
		super(mol, new int[] {atom});
		
		this.bounds = bounds;		
		this.gridSize = gridSize;		
		this.evalAtom = atom;
		this.grid = new FGValue[(int)((bounds[1].x-bounds[0].x)/gridSize+1)][(int)((bounds[1].y-bounds[0].y)/gridSize+1)][(int)((bounds[1].z-bounds[0].z)/gridSize+1)];

		this.termsToAggregate = termsToAggregate;		
		g = new Coordinates[evalAtom+1];
		g[evalAtom] = new Coordinates();
				
	}

	@Override
	public double getFGValue(final Coordinates[] gradient) {
		final int atom = atoms[0];
		final Coordinates c = getMolecule().getCoordinates(atom);
		final Coordinates delta = c.subC(bounds[0]);
		
		final int X = (int) (delta.x / gridSize); 
		final int Y = (int) (delta.y / gridSize); 
		final int Z = (int) (delta.z / gridSize);
		final double x = delta.x/gridSize - X; 
		final double y = delta.y/gridSize - Y; 
		final double z = delta.z/gridSize - Z; 
		
		//Check if we are out of bounds
		if(X<0 || X+1>=grid.length || Y<0 || Y+1>=grid[X].length || Z<0 || Z+1>=grid[X][Y].length) return 0;
		
		precompute(X,Y,Z);
		precompute(X+1,Y,Z);
		precompute(X,Y+1,Z);
		precompute(X,Y,Z+1);
		precompute(X+1,Y,Z+1);
		precompute(X,Y+1,Z+1);
		precompute(X+1,Y+1,Z);
		precompute(X+1,Y+1,Z+1);
		
		final double mx = 1 - x;
		final double my = 1 - y;
		final double mz = 1 - z;
		
		final double t0 = mx * my * mz;
		final double t1 =  x * my * mz;
		final double t2 = mx *  y * mz;
		final double t3 = mx * my *  z;
		final double t4 =  x * my *  z;
		final double t5 = mx *  y *  z;
		final double t6 =  x *  y * mz;
		final double t7 =  x *  y *  z;
			
		FGValue v0 = grid[X][Y][Z];
		FGValue v1 = grid[X+1][Y][Z];
		FGValue v2 = grid[X][Y+1][Z];
		FGValue v3 = grid[X][Y][Z+1];
		FGValue v4 = grid[X+1][Y][Z+1];
		FGValue v5 = grid[X][Y+1][Z+1];
		FGValue v6 = grid[X+1][Y+1][Z];
		FGValue v7 = grid[X+1][Y+1][Z+1];
		
		
		final double v = 	
			v0.x * t0 +
			v1.x * t1 +
			v2.x * t2 +
			v3.x * t3 +
			v4.x * t4 +
			v5.x * t5 +
			v6.x * t6 +
			v7.x * t7;
		
		if(gradient!=null && gradient.length>atom) {
			
			gradient[atom].add(new Coordinates(
					v0.gx * t0 +
					v1.gx * t1 +
					v2.gx * t2 +
					v3.gx * t3 +
					v4.gx * t4 +
					v5.gx * t5 +
					v6.gx * t6 +
					v7.gx * t7,
					
					v0.gy * t0 +
					v1.gy * t1 +
					v2.gy * t2 +
					v3.gy * t3 +
					v4.gy * t4 +
					v5.gy * t5 +
					v6.gy * t6 +
					v7.gy * t7,
					
					v0.gz * t0 +
					v1.gz * t1 +
					v2.gz * t2 +
					v3.gz * t3 +
					v4.gz * t4 +
					v5.gz * t5 +
					v6.gz * t6 +
					v7.gz * t7						
			));
			
		}
		
		return v;
	}
	
	private final void precompute(int X, int Y, int Z) {
		if(grid[X][Y][Z]!=null) return;		
		final Coordinates initial = getMolecule().getCoordinates(evalAtom);
		final Coordinates C = new Coordinates(
				bounds[0].x + X*gridSize,
				bounds[0].y + Y*gridSize,
				bounds[0].z + Z*gridSize);

		getMolecule().setCoordinates(evalAtom, C);	
		double v = termsToAggregate.getFGValue(g);
		getMolecule().setCoordinates(evalAtom, initial);
		
		grid[X][Y][Z] = new FGValue(v, g[evalAtom].x, g[evalAtom].y, g[evalAtom].z);
	}
	
	/**
	 * Debug instruction to visualize potential
	 */
	public void printValues(String name) {
		try {
			PrintStream os = new PrintStream(new FileOutputStream("c:/"+name.replace('*', ' ')+".txt"));
			os.println("X\tY\tZ\tval");
			int offset = (int)(6/gridSize);
			for (int X = offset; X < grid.length-offset; X++) {
				for (int Y = offset; Y < grid[X].length-offset; Y++) {				
					for (int Z = offset; Z < grid[X][Y].length-offset; Z++) {
						precompute(X, Y, Z);
						
						os.println(X+"\t"+Y+"\t"+Z+"\t"+grid[X][Y][Z].x);
					}	
				}	
			}
			os.close();		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
