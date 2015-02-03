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
package com.actelion.research.forcefield.transformation;

import java.util.List;

import javax.vecmath.Matrix3d;

import com.actelion.research.chem.*;
import com.actelion.research.chem.calculator.*;
import com.actelion.research.util.*;

/**
 * Rotation around G + Translation 
 * @author freyssj
 */
public class TransRotTransform extends AbstractTransform {
	private Matrix3d[] matrixes;
	private Coordinates G;
	private int groupSeed;
	private int[] a2g;
	
	private TransRotTransform() {
		this.parameters = new double[6];
	}

	public TransRotTransform(FFMolecule mol) {
		this(mol, 0, StructureCalculator.getAtomToGroups(mol));		
	}

	public TransRotTransform(FFMolecule mol, Coordinates G, int groupSeed, int[] a2g) {
		this();
		this.G = new Coordinates(G);
		this.groupSeed = groupSeed;
		this.a2g = a2g;
	}
	
	
	public TransRotTransform(FFMolecule mol, int groupSeed, int[] a2g) {
		this();
		List<Integer> backbone = StructureCalculator.getBackbone(mol, groupSeed);		
		this.G = new Coordinates(mol.getCoordinates(backbone.get(backbone.size()/2)));
		this.groupSeed = groupSeed;
		this.a2g = a2g;
	}
		

	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#random()
	 */
	@Override
	public void random() {
		
		parameters[0] = (Math.random()-.5)*10;
		parameters[1] = (Math.random()-.5)*10;
		parameters[2] = (Math.random()-.5)*10;
		
		parameters[3] = (Math.random() - .5) * (Math.PI * 2);
		parameters[4] = (Math.random() - .5) * (Math.PI * 2);
		parameters[5] = (Math.random() - .5) * (Math.PI * 2);		
	}



	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#mutate()
	 */
	@Override
	public AbstractTransform mutate() {		
		TransRotTransform res = (TransRotTransform) this.clone();
		if(Math.random()<=.5) {
			Coordinates c;
			do {c = new Coordinates(Math.random()-.5, Math.random()-.5, Math.random()-.5);} while(c.distSq()==0);
			c =  c.unit().scaleC(Math.random()*2);						
			res.parameters[0] += c.x;
			res.parameters[1] += c.y;
			res.parameters[2] += c.z;
			res.parameters[3] += (Math.random()-.5)*Math.PI/12;
			res.parameters[4] += (Math.random()-.5)*Math.PI/12;
			res.parameters[5] += (Math.random()-.5)*Math.PI/12;		
		}  else  {
			Coordinates c;
			do {c = new Coordinates(Math.random()-.5, Math.random()-.5, Math.random()-.5);} while(c.distSq()==0);
			c =  c.unit().scaleC(Math.random()*4);						
			res.parameters[0] += c.x;
			res.parameters[1] += c.y;
			res.parameters[2] += c.z;
			res.parameters[3] = (Math.random()-.5)*Math.PI*2;
			res.parameters[4] = (Math.random()-.5)*Math.PI*2;
			res.parameters[5] = (Math.random()-.5)*Math.PI*2;						
		}

		return res;
	}


	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#crossover(com.actelion.research.forcefield.transformation.AbstractTransform)
	 */
	@Override
	public AbstractTransform crossover(AbstractTransform transform) {
		TransRotTransform res = (TransRotTransform) this.clone();
		if(Math.random()<=.5) {			
			res.parameters[0] = transform.parameters[0];
			res.parameters[1] = transform.parameters[1];
			res.parameters[2] = transform.parameters[2];
		} else {
			res.parameters[3] = transform.parameters[3];
			res.parameters[4] = transform.parameters[4];
			res.parameters[5] = transform.parameters[5];			
		}
		return res;
	}


	private final void computeMatrixes() {
		if(matrixes==null) {
			matrixes = MathUtils.anglesToMatrixAndDerivates(new double[]{parameters[3], parameters[4], parameters[5]});
		}
	}


	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#getDTransformation(int, int)
	 */
	@Override
	public Coordinates[] getDTransformation(int var, Coordinates[] X) {

		Coordinates[] res = new Coordinates[X.length];
		switch(var) {
		case 0: 
			for (int i = 0; i < res.length; i++) {
				if(a2g==null || a2g[i] == a2g[groupSeed]) res[i] = new Coordinates(1,0,0);
				else res[i] = new Coordinates();
			}
			break;
		case 1: 
			for (int i = 0; i < res.length; i++) {
				if(a2g==null || a2g[i] == a2g[groupSeed]) res[i] = new Coordinates(0,1,0);
				else res[i] = new Coordinates();
			}
			break;
		case 2: 
			for (int i = 0; i < res.length; i++) {
				if(a2g==null || a2g[i] == a2g[groupSeed]) res[i] = new Coordinates(0,0,1);
				else res[i] = new Coordinates();
			}
			break;
		case 3: 
		case 4: 
		case 5: 
			computeMatrixes();
			Matrix3d val = matrixes[var-2];
			//Coordinates G = new Coordinates();//Coordinates.createBarycenter(X);
			for (int i = 0; i < res.length; i++) {
				Coordinates c = X[i].subC(G);
				if(a2g==null || a2g[i] == a2g[groupSeed]) 
					res[i] = new Coordinates(
					val.m00*c.x + val.m01*c.y + val.m02*c.z, 
					val.m10*c.x + val.m11*c.y + val.m12*c.z, 
					val.m20*c.x + val.m21*c.y + val.m22*c.z);
				else res[i] = new Coordinates();
			}	
			break;
		default: 
			throw new RuntimeException("Invalid variable "+var);
		}
		return res;
	}


	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#getTransformation(int, com.actelion.research.chem.Coordinates)
	 */
	@Override
	public Coordinates[] getTransformation(Coordinates[] X) {
		matrixes = null;
		computeMatrixes();
		Matrix3d val = matrixes[0];

		Coordinates[] res = new Coordinates[X.length];
		for (int i = 0; i < res.length; i++) {
			Coordinates c = X[i].subC(G);
			if(a2g==null || a2g[i] == a2g[groupSeed]) 
				res[i] = new Coordinates(
				val.m00*c.x + val.m01*c.y + val.m02*c.z + parameters[0] + G.x, 
				val.m10*c.x + val.m11*c.y + val.m12*c.z + parameters[1] + G.y, 
				val.m20*c.x + val.m21*c.y + val.m22*c.z + parameters[2] + G.z);
			else 
				res[i] = new Coordinates(X[i]);
		}
		return res;		
	}
	
	@Override
	public Coordinates[] getPartialTransformation(Coordinates[] X) {
		computeMatrixes();
		Matrix3d val = matrixes[0];

		Coordinates[] res = new Coordinates[X.length];
		for (int i = 0; i < res.length; i++) {
			Coordinates c = X[i];//.sub(G);
			if(a2g==null || a2g[i] == a2g[groupSeed]) 
				res[i] = new Coordinates(
				val.m00*c.x + val.m01*c.y + val.m02*c.z,// + G.x, 
				val.m10*c.x + val.m11*c.y + val.m12*c.z,// + G.y, 
				val.m20*c.x + val.m21*c.y + val.m22*c.z);
			else
				res[i] = new Coordinates();
		}
		return res;		
		
	}

	/**
	 * Create groups of atoms, each group contains atoms that are rigid compared to each other
	 * Returns the latest created group
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#createGroups(int[], int)
	 */
	@Override
	protected int createGroups(int[] groups, int n) {
		//Nothing, all the atoms are here in the same group
		if(a2g==null) {
			return n;
		} else {
			int maxN = 0;
			for (int i = 0; i<groups.length && i < a2g.length; i++) {
				if(a2g[i]>1) {
					maxN = Math.max(a2g[i], maxN);
					groups[i] = n + a2g[i]-1;
				}
			}
			return n + maxN;
		}
	}

}
