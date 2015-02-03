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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.vecmath.Matrix3d;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.chem.calculator.TorsionCalculator;
import com.actelion.research.util.MathUtils;
import com.actelion.research.util.datamodel.IntQueue;

/**
 * Transform used to go from a cartesian referential (3 * nAtoms degrees of freedom) to a torsion referential (nRotBonds degrees of freedom)
 *  
 */
@SuppressWarnings("unchecked")
public class TorsionTransform extends AbstractTransform {

	private final FFMolecule mol;

	/** 
	 * Array of rotatable bonds defined by their 2 connected atoms.
	 * The bonds are ordered as to minimize the impact on the center of gravity	
	 */	
	private final int[] bondAtoms1;
	private final int[] bondAtoms2;
	//private final int[] weigths;
	
	/** possibleTorsions[i] contained the list of the prefered angles for the bond i */
	private List<Double>[] possibleTorsions;

	/** Array of atomNo -> Array of rotatableBond indexes */ 
	private final int[][] atomsToIndexes;
	private final List<List<Integer>> groupOfTorsions = new ArrayList<List<Integer>>(); 

	private final boolean considerHydrogens;
	private final int centerAtom;
	 
	
	
	public TorsionTransform(FFMolecule mol, boolean considerHydrogens) {
		this(mol, 0, null, considerHydrogens);
	}
	
	public TorsionTransform(FFMolecule mol, int groupSeed, int[] a2g, boolean considerHydrogens) {
		this(mol, groupSeed, a2g, -1, considerHydrogens, true);
		
	}
	public TorsionTransform(FFMolecule mol, int groupSeed, int[] a2g, int atmCenter, boolean considerHydrogens, boolean rotateSmallestGroup) {
		this.mol = mol;
		this.considerHydrogens = considerHydrogens;
		if(mol.getAllAtoms()==0) throw new IllegalArgumentException("No molecule");
		if(!mol.isMoleculeInOrder()) throw new IllegalArgumentException("Molecule not in order");
		
		//0. Extract ligand
		//FFMolecule lig = StructureCalculator.extractLigand(mol);
		
		//1. Calculates the rotatables bonds		
		int[] rotatables = StructureCalculator.getRotatableBonds(mol, groupSeed, considerHydrogens);
		
		bondAtoms1 = new int[rotatables.length];
		bondAtoms2 = new int[rotatables.length];
		//weigths = new int[rotatables.length];
		parameters = new double[rotatables.length];
		atomsToIndexes = new int[mol.getNMovables()][];

		int[][] dists = StructureCalculator.getNumberOfBondsBetweenAtoms(mol, mol.getNMovables(), new int[mol.getNMovables()][mol.getNMovables()]);
		//2. The Center Atom is defined as the atom with the biggest no of rotatables bonds and the closest to the center		
		if(atmCenter>=0) {
			centerAtom = atmCenter; 
		} else if(mol.isAtomFlag(groupSeed, FFMolecule.LIGAND) && rotateSmallestGroup) {
			centerAtom = StructureCalculator.getStructureCenter(mol, groupSeed, rotatables, dists);
		} else {
			centerAtom = groupSeed;
		}
		
		//3. the atom1 should be the closest to the center atom
		Set<Integer> setOrientedBonds = new TreeSet<Integer>();
		Set<Integer> setBonds = new TreeSet<Integer>();
		for (int i = 0; i < rotatables.length; i++) {
			int a1 = mol.getBondAtom(0, rotatables[i]);
			int a2 = mol.getBondAtom(1, rotatables[i]);

			if(rotateSmallestGroup) {
				if(centerAtom<mol.getNMovables() && a1<mol.getNMovables() && a2<mol.getNMovables() && dists[centerAtom][a2]<dists[centerAtom][a1]) {
					bondAtoms1[i] = a2;
					bondAtoms2[i] = a1;				
				} else {
					bondAtoms1[i] = a1;
					bondAtoms2[i] = a2;
				}
				setOrientedBonds.add(bondAtoms2[i]*1000+bondAtoms1[i]);
			} else {				
				if(a1<mol.getNMovables() && a2<mol.getNMovables() && a2<mol.getNMovables() && dists[groupSeed][a2]<dists[groupSeed][a1] && a2!=centerAtom) {
					bondAtoms1[i] = a2;
					bondAtoms2[i] = a1;				
				} else {
					bondAtoms1[i] = a1;
					bondAtoms2[i] = a2;
				}				
				setOrientedBonds.add(bondAtoms2[i]*1000+bondAtoms1[i]);
			}
			/*if(!rotateSmallestGroup) {
				int a = bondAtoms1[i];
				bondAtoms1[i] = bondAtoms2[i];
				bondAtoms2[i] = a;
			}*/
			
			setBonds.add(rotatables[i]);
		}
		
		//4. Create independant groups of rotation
		for (int i = 0; i < mol.getAllConnAtoms(centerAtom); i++) {
			int a = mol.getConnAtom(centerAtom, i);
			boolean[] seen = new boolean[mol.getNMovables()];
			seen[centerAtom] = true;
			StructureCalculator.dfs(mol, a, seen);
			
			List<Integer> group = new ArrayList<Integer>();
			for (int j = 0; j < seen.length; j++) {
				if(j!=centerAtom && seen[j]) { 
					for (int k = 0; k < bondAtoms1.length; k++) {
						if((j==bondAtoms1[k] || j==bondAtoms2[k]) && !group.contains(k)) {group.add(k); break;}							
					}
				}
			}
			if(group.size()>0) {
				groupOfTorsions.add(group); 
			}
		}
		
		//5. For each atom check which bonds have to be rotated
		//Arrays.fill(weigths, 1);
		List<Integer>[] lists = new List[atomsToIndexes.length];
		for (int atom = 0; atom < mol.getNMovables(); atom++) {
			if(a2g!=null && a2g[atom]!=a2g[groupSeed]) continue;
			lists[atom] = new ArrayList<Integer>();
			IntQueue q = new IntQueue();
			q.push(atom);
			while(!q.isEmpty()) {
				int a1 = q.pop();
				for(int j=0; j<mol.getAllConnAtoms(a1); j++) {
					int b = mol.getConnBond(a1, j);
					int a2 = mol.getConnAtom(a1, j);
					if(!setBonds.contains(b)) {
						if(q.indexOf(a2)<0) q.push(a2);
					} else {
						if(setOrientedBonds.contains(a1*1000+a2)) {
							//we go through a rotatable bond, add it to our list
							int k=0; while(rotatables[k]!=b) k++; 
							if(atom!=bondAtoms2[k]) {
								lists[atom].add(k);
							} 
							if(q.indexOf(a2)<0) q.push(a2);
						}
					}					
				}
			}
		}
		for (int i = 0; i < mol.getNMovables(); i++) {
			if(lists[i]==null) {
				atomsToIndexes[i] = new int[]{};
			} else {
				atomsToIndexes[i] = new int[lists[i].size()];
				for (int j = lists[i].size()-1; j>=0; j--) {
					atomsToIndexes[i][j] = lists[i].get(j);
				}
			}
		}
	}	
	
	public void initPossibleTorsions() {
		//Extract possible torsions
		possibleTorsions = TorsionCalculator.computePossibleTorsions(mol, considerHydrogens);
	}
	
	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#random()
	 */
	@Override
	public void random() {
		if(possibleTorsions==null) {
			initPossibleTorsions();
		}
		if(parameters.length!=possibleTorsions.length) {
			System.err.println("WARNING:  SIZE DIFFERS: "+parameters.length+" <>"+possibleTorsions.length+": how is possible?");
		} else {
			
			for (int i = 0; i < possibleTorsions.length; i++) {
				if(possibleTorsions[i].size()==0) {
					System.err.println("WARNING:  BOND "+i+" does not have possible torsions"); 
				} else {
					parameters[i] = possibleTorsions[i].get((int)(Math.random()*possibleTorsions[i].size()));
				}
			}
		}
	}

	//private static final double ANGLES[] = {-2*Math.PI/3, -Math.PI/3, Math.PI/3, 2*Math.PI/3};   
 
	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#mutate()
	 */
	@Override
	public AbstractTransform mutate() {
		if(possibleTorsions==null) {
			initPossibleTorsions();
		}
		
		TorsionTransform res = (TorsionTransform) this.clone();
		if(parameters.length>0) {
	
			int type = (int)(Math.random()*3);
			List<Integer> group = groupOfTorsions.get((int)(Math.random()*groupOfTorsions.size()));
			if(type<=0) {
				for(int i=0; i<res.parameters.length; i++) res.parameters[i] += (Math.random()-.5) * Math.PI/6;	
			} else if(type==1) {
				for(int i: group) res.parameters[i] = possibleTorsions[i].get((int)(Math.random()*possibleTorsions[i].size()));
			} else {
				for(int i=0; i<res.parameters.length;i++) res.parameters[i] = possibleTorsions[i].get((int)(Math.random()*possibleTorsions[i].size()));
			}
		}
		
		return res;
	}
	

	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#crossover(com.actelion.research.forcefield.transformation.AbstractTransform)
	 */
	@Override
	public AbstractTransform crossover(AbstractTransform transform) {

		TorsionTransform res = (TorsionTransform) this.clone();

		if(parameters.length>0) {
			TorsionTransform t = (TorsionTransform) transform;
			
			int type = (int)(Math.random()*3);
			if(type==0 && groupOfTorsions.size()>1) {
				for (int i = 0; i < parameters.length; i++) {				
					res.parameters[i] = parameters[i];
				}				
				List<Integer> group = groupOfTorsions.get((int)(Math.random()*groupOfTorsions.size()));
				for(int i:group) res.parameters[i] = t.parameters[i];
			} else {				
				for (int i = 0; i < parameters.length; i++) {				
					res.parameters[i] = Math.random()<.5?parameters[i]: t.parameters[i];
				}				
			}
		}
		
		return res;
	}

	/*
	public static void main(String[] args) throws Exception{
		ExtendedMolecule  m = new ExtendedMolecule();
		new SmilesParser().parse(m, "CCCC");
		new SmilesParser().parse(m, "C1CC(CC)CC1OCC");
		new SmilesParser().parse(m, "CCC1CCC(CC1)CCCCCCC4CCCC4CCC2CCCC2");
		m.ensureHelperArrays(Molecule.cHelperNeighbours);
		FFMolecule mol = new FFMolecule(m);		
		StructureCalculator.addHydrogens(mol);
		
		ForceField forceField = new ForceField(mol);
		PreOptimizer.preOptimize(mol); 		
		
		new AlgoLBFGS().optimize(new EvaluableForceField(forceField));

		StructureCalculator.translateLigand(mol, new Coordinates().sub(StructureCalculator.getLigandCenter(mol)));
		
//		ChainOfTransformations chain = new ChainOfTransformations(new AbstractTransform[]{new TorsionTransform(mol)}, ForceFieldMoleculeCalculator.getLigandCoordinates(mol));
		ChainOfTransformations chain = new ChainOfTransformations(new AbstractTransform[]{new TransRotTransform(mol), new TorsionTransform(mol, false)}, StructureCalculator.getLigandCoordinates(mol));
//		ChainOfTransformations chain = new ChainOfTransformations(new AbstractTransform[]{new TransRotTransform(mol)}, ForceFieldMoleculeCalculator.getLigandCoordinates(mol));

		FFViewer viewer = FFViewer.viewMolecule(mol);
		DefaultCallBack c = new DefaultCallBack(viewer, forceField);

		while(true) {
			MultiVariate mv = chain.getMultivariate();
			for(int i=3; i<mv.vector.length; i++) mv.vector[i] = (mv.vector[i] + .1 /(1+mv.vector.length-i)) % (Math.PI*2);
			chain.setMultivariate(mv);
			try{Thread.sleep(100);}catch(Exception e){}
			
			chain.transform(mol);
			//forceField.initTerms();
			//new AlgoLBFGS().optimize(new EvaluableForceField(forceField));
			
			viewer.setMolecule(mol, false);			
			viewer.repaint();
		}
		
	}*/
	
	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#getTransformation(int, com.actelion.research.chem.Coordinates)
	 */
	@Override
	public Coordinates[] getTransformation(Coordinates X[]) {
		// Xp = F1(F2(F3(X)))
		//	with Fi = Mci(X-Ci)+Ci
		Coordinates[] res = new Coordinates[X.length];
		
		//Precompute matrixes of rotation
		Matrix3d[] M = new Matrix3d[bondAtoms1.length]; 
		for(int i=0; i<bondAtoms1.length; i++) M[i] = getMatrix(i, X);
		
		for(int atom=0; atom<atomsToIndexes.length; atom++) {			
			Coordinates c = X[atom];
			for (int j = 0; j < atomsToIndexes[atom].length; j++) {
				//compute Fj
				int v = atomsToIndexes[atom][j];				
				
				Matrix3d val = M[v];				
				Coordinates C = X[bondAtoms2[v]];
				c = c.subC(C);
				c = new Coordinates(
					val.m00*c.x + val.m01*c.y + val.m02*c.z + C.x,
					val.m10*c.x + val.m11*c.y + val.m12*c.z + C.y,
					val.m20*c.x + val.m21*c.y + val.m22*c.z + C.z);
			}
			res[atom] = c;
		}
		return res;
	}

	/**
	 * Derivate according to each coordinate
	 */
	@Override
	public Coordinates[] getPartialTransformation(Coordinates[] X) {

		Coordinates[] res = new Coordinates[X.length];
		
		//Precompute matrixes of rotation
		Matrix3d[] M = new Matrix3d[bondAtoms1.length]; 
		for(int i=0; i<bondAtoms1.length; i++) M[i] = getMatrix(i, mol.getCoordinates());
		
		for(int atom=0; atom<atomsToIndexes.length; atom++) {			
			Coordinates c = X[atom];
			for (int j = 0; j < atomsToIndexes[atom].length; j++) {
				//compute Fj
				int v = atomsToIndexes[atom][j];				
				Matrix3d val = M[v];			
				if(val==null) {
for (int i = 0; i < X.length; i++) {
	System.out.println(i+" "+X[i]);
}
System.exit(1);
					res[atom] = c;
					break;
				}
				c = new Coordinates(
					val.m00*c.x + val.m01*c.y + val.m02*c.z,// + C.x,
					val.m10*c.x + val.m11*c.y + val.m12*c.z,// + C.y,
					val.m20*c.x + val.m21*c.y + val.m22*c.z);// + C.z);
				if(Double.isNaN(c.x)) {System.err.println("C="+c+" "+val+" "+Arrays.toString(X));System.exit(1);}
			}
			res[atom] = c;
		}
		return res;
	}
		
	
	/**
	 * Derivate according to one parameter
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#getDTransformation(int, int)
	 */
	@SuppressWarnings("null")
	@Override
	public Coordinates[] getDTransformation(int var, Coordinates X[]) {
		
		// X' = F1(F2(F3(X)))
		//	with Fi = Mci(X-Ci)+Ci
		// => dX'/di = Fi'�Fi+1�...�Fn(X) * (F1�...�Fi-1)'�Fi�...�Fn(X)
		//			 = Fi'�Fi+1�...�Fn(X) * Fi-1'�Fi�...�Fn(X) * ... * F0'�F1�...�Fn(X) 

		Coordinates[] res = new Coordinates[X.length];

		//Precompute matrixes
		Matrix3d[][] M = new Matrix3d[bondAtoms1.length][]; 
		for(int i=0; i<bondAtoms1.length; i++) {
			M[i] = getMatrixes(i, X);			
		}
		
		for(int atom=0; atom<X.length; atom++) {
			
			boolean dependsOfVar = false;
			for (int j = 0; j < atomsToIndexes[atom].length; j++) {
				if(atomsToIndexes[atom][j]==var) {dependsOfVar=true; break;}
			}
			
			if(dependsOfVar) {

				Coordinates product = null; 
				Coordinates c = X[atom];
				boolean afterDerivate = false;
				for (int j = 0; j < atomsToIndexes[atom].length; j++) {
					//compute Fj
					int v = atomsToIndexes[atom][j];				
			
						
					if(afterDerivate) {
						//Coordinates C = X[bondAtoms2[v]];
						Matrix3d val = M[v][0];
						//product = product.subC(C);
						product = new Coordinates(
							val.m00*product.x + val.m01*product.y + val.m02*product.z,
							val.m10*product.x + val.m11*product.y + val.m12*product.z,
							val.m20*product.x + val.m21*product.y + val.m22*product.z);						
					} else if(v==var) {
						Matrix3d dval = M[v][1];
						c = c.subC(X[bondAtoms2[v]]);
						product = new Coordinates(
							dval.m00*c.x + dval.m01*c.y + dval.m02*c.z,
							dval.m10*c.x + dval.m11*c.y + dval.m12*c.z,
							dval.m20*c.x + dval.m21*c.y + dval.m22*c.z);
						afterDerivate=true;
					} else {
						Coordinates C = X[bondAtoms2[v]];
						Matrix3d val = M[v][0];				
						c = c.subC(C);
						c = new Coordinates(
							val.m00*c.x + val.m01*c.y + val.m02*c.z + C.x,
							val.m10*c.x + val.m11*c.y + val.m12*c.z + C.y,
							val.m20*c.x + val.m21*c.y + val.m22*c.z + C.z);						
					}
				}
				res[atom] = product; 
				
			} else {
				res[atom] = new Coordinates();
			}			
		}
		
		return res;
	}

	private final Matrix3d getMatrix(int b, Coordinates X[]) {
		Coordinates normal = X[bondAtoms2[b]].subC(X[bondAtoms1[b]]);
		if(normal.dist()==0) return null;
		Matrix3d M = MathUtils.createRotationMatrix(new double[]{normal.x, normal.y, normal.z}, parameters[b]);
		return M;
	}

	private final Matrix3d[] getMatrixes(int b, Coordinates X[]) {
		Coordinates normal = X[bondAtoms2[b]].subC(X[bondAtoms1[b]]);					
		Matrix3d[] M = MathUtils.rotationMatrixAndDerivate(new double[]{normal.x, normal.y, normal.z}, parameters[b]);
		/*
		//test
		double h = 1E-7;
		Coordinates c = new Coordinates(0,0,10);//.sub(C);
		double[] val = M[0].getValues();				
		double[] dval = M[1].getValues();
		System.out.println("derivate="+ new Coordinates(
			dval[0]*c.x + dval[1]*c.y + dval[2]*c.z,
			dval[3]*c.x + dval[4]*c.y + dval[5]*c.z,
			dval[6]*c.x + dval[7]*c.y + dval[8]*c.z));			
		
		Coordinates v = new Coordinates(
			val[0]*c.x + val[1]*c.y + val[2]*c.z ,
			val[3]*c.x + val[4]*c.y + val[5]*c.z ,
			val[6]*c.x + val[7]*c.y + val[8]*c.z );
		val = Matrix3D.rotationMatrixAndDerivate(new double[]{normal.x, normal.y, normal.z}, parameters[b]+h)[0].getValues();
		Coordinates vph = new Coordinates(
			val[0]*c.x + val[1]*c.y + val[2]*c.z ,
			val[3]*c.x + val[4]*c.y + val[5]*c.z ,
			val[6]*c.x + val[7]*c.y + val[8]*c.z );
			
		System.out.println("explicit="+ vph.sub(v).scale(1.0/h));
		System.out.println();				
		*/
		
		return M;
	}



	/**
	 * @see com.actelion.research.forcefield.transformation.AbstractTransform#createGroups(int[], int)
	 */
	@Override
	protected int createGroups(int[] groups, int n) {
		//2 atoms are in the same group if they depends of the same rotatable bonds
		for (int i = 0; i < atomsToIndexes.length; i++) {
			boolean newGroup = false;
			for (int j = i+1; j < atomsToIndexes.length; j++) {
				if((atomsToIndexes[i].length==0 && atomsToIndexes[j].length==0) ||
					(atomsToIndexes[i].length>0 && atomsToIndexes[j].length>0 && atomsToIndexes[i][0]==atomsToIndexes[j][0])) {
					//i and j are in the same group						
				} else if(groups[i]==groups[j]){
					//i and j should be in different groups
					if(!newGroup) {n++; newGroup = true;} 
					groups[j] = n;						
				}
			}							
		}		
		return n;
	}

	public int[] getBondAtoms1() {
		return bondAtoms1;
	}

	public int[] getBondAtoms2() {
		return bondAtoms2;
	}

	public int getCenterAtom() {
		return centerAtom;
	}


	@Override
	public TorsionTransform clone() {
		TorsionTransform t = (TorsionTransform) super.clone(); 
		t.possibleTorsions = this.possibleTorsions;
		return t;
	}
	
}
