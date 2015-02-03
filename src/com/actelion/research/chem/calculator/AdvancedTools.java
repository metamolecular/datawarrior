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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFConfig;
import com.actelion.research.forcefield.FFUtils;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.SuperposeTerm;
import com.actelion.research.forcefield.mm2.MM2Config;
import com.actelion.research.forcefield.mm2.MM2Parameters;
import com.actelion.research.forcefield.optimizer.AlgoLBFGS;
import com.actelion.research.forcefield.optimizer.EvaluableDockFlex;
import com.actelion.research.forcefield.optimizer.EvaluableDockRigid;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.forcefield.optimizer.PreOptimizer;
import com.actelion.research.util.Formatter;

/**
 * This class has a set of tools used to simplify the most common uses of the
 * forcefield. It is also used as a repository for experimental functions
 * 
 */
public class AdvancedTools {

	/**
	 * Estimates the cavity size in A^3
	 * 
	 * @param mol
	 * @param c
	 * @return
	 */
	public static double getCavitySize(FFMolecule mol, Coordinates c) {
		Coordinates[] bounds = StructureCalculator.getBounds(mol);
		MoleculeGrid grid = new MoleculeGrid(mol, 1.5);
		double size = 0;
		final double maxRadius = 7;
		for (double x = Math.max(bounds[0].x, c.x - maxRadius); x < Math.min(
				bounds[1].x, c.x + maxRadius); x++) {
			for (double y = Math.max(bounds[0].y, c.y - maxRadius); y < Math
					.min(bounds[1].y, c.y + maxRadius); y++) {
				for (double z = Math.max(bounds[0].z, c.z - maxRadius); z < Math
						.min(bounds[1].z, c.z + maxRadius); z++) {
					int n = grid.getNeighbours(new Coordinates(x, y, z), 1.4)
							.size();
					if (n == 0)
						size++;
				}
			}
		}

		return size;
	}

	public static int fillWaterProbes(FFMolecule mol) {
		final double dist = 2.4;
		MoleculeGrid grid = new MoleculeGrid(mol);
		Coordinates[] bounds = StructureCalculator.getLigandBounds(mol);
		if (bounds == null)
			bounds = StructureCalculator.getBounds(mol);
		else {
			bounds[0].sub(new Coordinates(6, 6, 6));
			bounds[1].add(new Coordinates(6, 6, 6));
		}
		int count = 0;
		for (double x = bounds[0].x + dist / 2; x < bounds[1].x; x += dist) {
			for (double y = bounds[0].y + dist / 2; y < bounds[1].y; y += dist) {
				for (double z = bounds[0].z + dist / 2; z < bounds[1].z; z += dist) {
					Coordinates c = new Coordinates(x, y, z);
					Set<Integer> set = grid.getNeighbours(c, 2.8, true);
					if (set.size() == 0) {
						if (grid.hasNeighbours(c, 6)) {
							int water = mol.addAtom(8);
							mol.setCoordinates(water, c);
							mol.setAtomFlag(water, FFMolecule.RIGID, true);
							count++;
						}
					}
				}
			}
		}
		return count;

	}

	public static double getHBondContribution(FFMolecule mol) {
		int[] donor = new int[mol.getAllAtoms()];
		int[] acceptor = new int[mol.getAllAtoms()];
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if (mol.getAtomicNo(i) != 8 && mol.getAtomicNo(i) != 7
					&& mol.getAtomicNo(i) != 16)
				continue;

			if (mol.getAtomicNo(i) == 8 && mol.getConnAtoms(i) < 2) {
				donor[i] = mol.getAtomMM2Class(i) == 6 ? 2
						: mol.getAtomMM2Class(i) == 7 ? 1 : 0;
			} else if (mol.getAtomicNo(i) == 7 && mol.getConnAtoms(i) < 4) {
				donor[i] = mol.getAtomMM2Class(i) == 8 ? 2
						: mol.getAtomMM2Class(i) == 9 || mol.getAtomMM2Class(i) == 37
								|| mol.getAtomMM2Class(i) == 40 ? 1 : 0;
			} else if (mol.getAtomicNo(i) == 16 && mol.getConnAtoms(i) < 2) {
				donor[i] = 1;
			}
			acceptor[i] = mol.getAtomicNo(i) == 8 ? 2
					: mol.getAtomicNo(i) == 7 ? (mol.getAtomMM2Class(i) == 8 ? 1
							: 0) : 0; // Approximative
		}

		double res = 0;
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if (!mol.isAtomFlag(i, FFMolecule.LIGAND))
				continue;
			for (int j = 0; j < mol.getAllAtoms(); j++) {
				if (mol.isAtomFlag(j, FFMolecule.LIGAND))
					continue;

				double dr = Math.abs(mol.getCoordinates(i).distance(
						mol.getCoordinates(j))
						- (1.85 + .97));
				if (dr > .65)
					continue;

				double gr = dr < .25 ? 1 : 1 - (dr - .25) / 4;
				double hb = 0;

				for (int k = 0; k < 2; k++) {
					int tmp = i;
					i = j;
					j = tmp;

					int f = donor[i] * acceptor[j];
					if (f == 0)
						continue;

					double ga = 0;
					for (int m = 0; m < mol.getAllConnAtoms(i); m++) {
						int a = mol.getConnAtom(i, m);
						if (mol.getAtomicNo(a) <= 1)
							continue;

						double da = Math.abs(mol.getCoordinates(j).subC(
								mol.getCoordinates(i)).getAngle(
								mol.getCoordinates(a).subC(
										mol.getCoordinates(i)))
								- 108 * Math.PI / 180);
						ga = Math
								.max(
										ga,
										da < 30 * Math.PI / 180 ? 1
												: da < 80 * Math.PI / 180 ? 1 - (da - 30 * Math.PI / 180) / 50
														: -(da - 80 * Math.PI / 180) / 30);
					}
					// hb = Math.max(hb, f*gr*ga);
					hb = Math.max(hb, gr * ga);
				}
				res += hb;

			}
		}
		return res;
	}

	/**
	 * Creates different protein models by giving random torsions to the
	 * protein's side chains. Results not promising
	 * 
	 * @param protein
	 * @param center
	 * @param radius
	 * @param N
	 * @return
	 */
	public static List<FFMolecule> createModels(FFMolecule protein,
			Coordinates center, int radius, int N) {
		List<FFMolecule> res = new ArrayList<FFMolecule>();
		protein = StructureCalculator.crop(protein, center, radius + 8);
		StructureCalculator.makeProteinFlexible(protein, center, radius, true);
		protein.reorderAtoms();
		int[] rot = StructureCalculator.getRotatableBonds(protein, false);
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMinRMS(1);
		algo.setMaxIterations(2000);
		MM2Config config = new MM2Config();
		config.setUseOrbitals(false);
		config.setMaxDistance(7);
		config.setUsePLInteractions(false);
		ForceField f = new ForceField(protein, config);
		double e = algo.optimize(new EvaluableForceField(f));
		protein.setAuxiliaryInfo("e", e);
		res.add(protein);
		boolean[] seen = new boolean[protein.getAllAtoms()];
		for (int i = 0; i < N - 1; i++) {
			FFMolecule m = new FFMolecule(protein);
			res.add(m);
		}
		// Rotate the side chains
		for (int j = 0; j < rot.length; j++) {
			int a1 = protein.getBondAtom(0, rot[j]);
			int a2 = protein.getBondAtom(1, rot[j]);

			Arrays.fill(seen, false);
			seen[a2] = true;
			int n = StructureCalculator.dfs(protein, a1, seen);
			for (FFMolecule m : res) {
				if (n < 12)
					StructureCalculator.rotateBond(m, a2, a1, Math.random() * 2
							* Math.PI);
				else
					StructureCalculator.rotateBond(m, a1, a2, Math.random() * 2
							* Math.PI);
			}
		}

		for (FFMolecule m : res) {
			StructureCalculator.makeProteinFlexible(m, center, radius + 4,
					false);

			f = new ForceField(m, config);

			e = algo.optimize(new EvaluableForceField(f));

			StructureCalculator.makeProteinRigid(m);
			m.setAuxiliaryInfo("e", e);
		}

		for (int j = 0; j < res.size(); j++) {
			for (int k = j + 1; k < res.size(); k++) {
				res.get(j).setAllAtomFlag(FFMolecule.LIGAND, true);
				res.get(k).setAllAtomFlag(FFMolecule.LIGAND, true);
				double rmsd = StructureCalculator.getLigandRMSD(res.get(j), res
						.get(k));
				System.out.println(j + "-" + k + " -> rmsd: " + rmsd);
				res.get(j).setAllAtomFlag(FFMolecule.LIGAND, false);
				res.get(k).setAllAtomFlag(FFMolecule.LIGAND, false);

			}
		}

		// Select the best N
		Collections.sort(res, new Comparator<FFMolecule>() {
			@Override
			public int compare(FFMolecule o1, FFMolecule o2) {
				return ((Double) o1.getAuxiliaryInfo("e")).compareTo((Double) o2
						.getAuxiliaryInfo("e"));
			}
		});
		res = res.subList(0, N);

		return res;
	}

	/**
	 * Generates all isomers for the given molecule
	 * @param em
	 * @return
	 */
	public static List<FFMolecule> convertMolecule2DTo3D(StereoMolecule em) {
		List<FFMolecule> mols = convertMolecule2DTo3D(em, false, null);
		
		return mols;
	}
	/**
	 * Converts the molecule to 3d and optimize it (including rings)
	 * 
	 * @param em
	 * @param generateAll (true to generate all isomers)
	 * @param errors a list of errors returned (can be null) 
	 * @return
	 */
	public static List<FFMolecule> convertMolecule2DTo3D(StereoMolecule em, boolean generateAll, List<String> errors) {
		new CoordinateInventor().invent(em);
		if(em.getName()==null) em.setName("Molecule");
		List<FFMolecule> mols = AdvancedTools.orientMoleculeForParity(em, generateAll, errors);
		for (FFMolecule mol : mols) {
			new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(mol)));
			optimizeRings(mol);
			optimizeByVibrating(mol);
		} 
		return mols;
	}

	/**
	 * Remove the positions that are closer than maxRMSD 
	 * @param positions
	 * @param maxRmsd
	 * @return
	 */
	public static int removeSimilarPositions(List<FFMolecule> positions, double maxRmsd) {
		int similars = 0;
		for (int i = 0; i < positions.size(); i++) {
			FFMolecule m1 = positions.get(i);
			for (int j = i + 1; j < positions.size(); j++) {
				FFMolecule m2 = positions.get(j);

				double rmsd = 0;
				for (int h = 0; h < m1.getAllAtoms(); h++)
					rmsd += m1.getCoordinates(h).distSquareTo(
							m2.getCoordinates(h));
				rmsd = Math.sqrt(rmsd / m1.getAllAtoms());
				if (rmsd < maxRmsd) {
					positions.remove(j);
					similars++;
					j--;
				}
			}
		}
		return similars;
	}

	/**
	 * Little routine used to keep a diverse set of positions.
	 * 
	 * @param positions
	 * @param nToKeep
	 * @return the rmsd used as a minimal distance between 2 positions 
	 */
	public static double removeClosestPositions(List<FFMolecule> positions, int nToKeep) {
		if (nToKeep >= positions.size())
			return -1;
		List<Double> distances = new ArrayList<Double>();

		for (int i = 0; i < positions.size(); i++) {
			FFMolecule m1 = positions.get(i);
			double dist = Double.MAX_VALUE;

			for (int j = 0; j < i; j++) {
				FFMolecule m2 = positions.get(j);
				double rmsd = 0;
				for (int h = 0; h < m1.getAllAtoms(); h++)
					rmsd += m1.getCoordinates(h).distSquareTo(
							m2.getCoordinates(h));
				rmsd = Math.sqrt(rmsd / m1.getAllAtoms());
				dist = Math.min(dist, rmsd);
			}
			distances.add(new Double(dist));
		}

		List<Double> distancesClone = new ArrayList<Double>(distances);
		Collections.sort(distancesClone);
		double treshold = distancesClone.get(positions.size() - nToKeep);

		for (int i = 0; i < positions.size(); i++) {
			double dist = distances.get(i);
			if (dist < treshold || i >= nToKeep) {
				distances.remove(i);
				positions.remove(i);
				i--;
			}
		}

		return treshold;
	}

	/**
	 * Gets the Free Energy (<0)
	 * 
	 * @param mol
	 * @return
	 * 
	 * public static double getBestConformationEnergy(FFMolecule mol) {
	 * AlgoLBFGS algo = new AlgoLBFGS(); algo.setMinRMS(.1);
	 * algo.setMaxIterations(1200); try { //Optimize locally the ligand outside
	 * the cavity FFMolecule lig = (FFMolecule)
	 * StructureCalculator.extractLigand(mol, new FFMolecule()); ForceField f =
	 * new ForceField(lig, new FFConfig.MM2Config());
	 * 
	 * //Optimize globally the Ligand EvaluableTransformation eval = new
	 * EvaluableConformation(f, true); GADock dock = new GADock(eval);
	 * dock.setInitialPopulation(500); GAPosition best = dock.optimize();
	 * 
	 * return best.getScore();
	 *  } catch (Throwable e) { e.printStackTrace(); return 0; }
	 *  }
	 */
	public static void expandCavity(FFMolecule mol) {
		FFMolecule lig = StructureCalculator.extractLigand(mol);
		Coordinates center = StructureCalculator.getLigandCenter(lig);
		if (center == null)
			throw new IllegalArgumentException("No ligand in expandCavity");
		StructureCalculator.replaceLigand(mol, null);
		Random random = new Random();
		MoleculeGrid grid = new MoleculeGrid(mol, .75);
		loop: for (int j = 0; j < 200; j++) {
			Coordinates coords = new Coordinates((random.nextDouble() - .5),
					(random.nextDouble() - .5), (random.nextDouble() - .5))
					.scaleC(20);
			coords = coords.addC(center);
			Set<Integer> neighbours = grid.getNeighbours(coords, 1.4);
			for (Iterator<Integer> iter = neighbours.iterator(); iter.hasNext();) {
				int at = iter.next().intValue();
				if (mol.getCoordinates(at).distSquareTo(coords) < 1.4 * 1.4) {
					continue loop;
				}
			}
			int a1 = mol.addAtom(8);
			// int a2 = mol.addAtom(8);
			mol.setCoordinates(a1, coords);
			// mol.setCoordinates(a2, coords);
			mol.setAtomFlag(a1, FFMolecule.LIGAND | FFMolecule.PREOPTIMIZED,
					true);
			// mol.setAtomFlag(a2, FFMolecule.LIGAND | FFMolecule.PREOPTIMIZED,
			// true);
			// mol.addBond(a1, a2, 2);
			grid = new MoleculeGrid(mol, .75);

		}
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMaxTime(80000);
		algo.setMinRMS(.5);
		mol.reorderAtoms();
		MM2Config config = new MM2Config();
		config.setMaxDistance(6);
		config.setMaxPLDistance(6);

		StructureCalculator.makeProteinFlexible(mol, null, 5, false);
		algo.optimize(new EvaluableForceField(new ForceField(mol, config)));
		StructureCalculator.makeProteinRigid(mol);

		StructureCalculator.replaceLigand(mol, lig);
	}

	/**
	 * 
	 * @param mol
	 * @return
	 */
	public static void optimizeHydrogens(FFMolecule mol) {
		int N = mol.getNMovables();

		// Set the non-H as rigid
		for (int i = 0; i < N; i++) {
			mol.setAtomFlag(i, FFMolecule.PREOPTIMIZED, mol.getAtomicNo(i) > 1);
		}

		// optimize the H
		PreOptimizer.preOptimizeHydrogens(mol);
	}

	public static boolean fixHydrogens(FFMolecule mol) {
		boolean changed1 = StructureCalculator.addHydrogens(mol);
		MM2Parameters.getInstance().setAtomClassesForMolecule(mol);
		boolean changed2 = MM2Parameters.getInstance().addLonePairs(mol, false);
		return changed1 || changed2;
	}

	/**
	 * Superpose the 2 ligands (fusion consists of 2 ligands) using the default suggested superposition 
	 * @param fusion
	 */
	public static double superposeLigands(FFMolecule fusion) {
		return superposeLigands(fusion, suggestLigandSuperposition(fusion), true, true, true);
	}
	
	/**
	 * Superpose 2 identical ligands
	 * @param model
	 * @param mol2
	 * @return
	 */
	public static double superposeLigands(FFMolecule model, FFMolecule mol2) {
		if(model.getAllAtoms()!=mol2.getAllAtoms()) throw new IllegalArgumentException("the new molecules must have the same number of atoms");
		int n = model.getAllAtoms();
		Coordinates[] c1 = new Coordinates[n];
		Coordinates[] c2 = new Coordinates[n];
		double[] w = new double[n]; 

		for (int i = 0; i < n; i++) {
			c1[i] = model.getCoordinates(i);
			c2[i] = new Coordinates(mol2.getCoordinates(i));
			w[i] = mol2.getConnAtoms(i)*mol2.getConnAtoms(i);
		}
		
		Matrix4d M = new Matrix4d();
		M.setIdentity();
		double rmsd = SuperposeCalculator.superpose(c1, c2, M, w);
		for (int i = 0; i < model.getAllAtoms(); i++) {
			Coordinates c = mol2.getCoordinates(i);
			Point3d p = new Point3d(c.x, c.y, c.z);
			M.transform(p);
			//System.out.println(c.x+">"+p.x);
			c.x = p.x;
			c.y = p.y;
			c.z = p.z;
		}
		
		return rmsd;
		
	}
	
	public static double superposeLigands(FFMolecule mol1, FFMolecule mol2, List<Integer> sel) {
		Coordinates[] c1 = new Coordinates[sel.size()/2];
		Coordinates[] c2 = new Coordinates[sel.size()/2];
		
		for (int j = 0; j < c1.length; j++) {
			int a1 = sel.get(2*j);
			int a2 = sel.get(2*j+1);
			
//			System.out.println("Match " + a1 +"/"+mol1.getAllAtoms() +  " - " + a2 +  "/"+mol2.getAllAtoms());

			
			c1[j] = mol1.getCoordinates(a1);
			c2[j] = new Coordinates(mol2.getCoordinates(a2));
		}
		Matrix4d M = new Matrix4d(); 
		M.setIdentity();
		double fit = SuperposeCalculator.superpose(c1, c2, M);
		
//		System.out.println("Got fit of " +fit);
		
		for (int i = 0; i < mol2.getAllAtoms(); i++) {
			Coordinates c = mol2.getCoordinates(i);
			Point3d p = new Point3d(c.x, c.y, c.z);
			M.transform(p);
			c.x = p.x;
			c.y = p.y;
			c.z = p.z;
		}
		return fit;
	}

	
	/**
	 * Superpose 2 flexibles molecules using the matching pattern provided. 
	 * @see suggestLigandSuperposition
	 * @param fusion
	 * @param match
	 */
	public static double superposeLigands(FFMolecule fusion, List<Integer> match, boolean finer, boolean flexible1, boolean flexible2) {
		final AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMaxIterations(1000);
		algo.setMinRMS(.1);
		fusion.reorderAtoms();
		MM2Config c = new MM2Config.DockConfig();
		c.setMaxDistance(100);
		final ForceField f = new ForceField(fusion, c);

		final int[] a2g = StructureCalculator.getAtomToGroups(fusion);

		// add superposition terms
		if(!finer) {
			for (int i = 0; i < match.size(); i += 2) {
				int a1 = match.get(i);
				int a2 = match.get(i + 1);
				f.getTerms().add(new SuperposeTerm(f.getMolecule(), 0, new int[] { a1, a2 }));
			}
		}
		for (int i = 0; i < fusion.getAllAtoms(); i++) {
			if(a2g[i]==0) {
				fusion.setAtomFlag(i, FFMolecule.RIGID, !flexible1);
			}
			if(a2g[i]==0) {
				fusion.setAtomFlag(i, FFMolecule.RIGID, !flexible2);
			}
			if (a2g[i] != 1 || fusion.getAtomicNo(i) <= 1) continue;
			for (int j = 0; j < fusion.getAllAtoms(); j++) {
				if (a2g[i] == a2g[j] || fusion.getAtomicNo(j) <= 1) continue;
				if (fusion.getAtomInteractionClass(i) == fusion.getAtomInteractionClass(j))
					f.getTerms().add( new SuperposeTerm(f.getMolecule(), finer?-.001: .5, new int[] { i, j }));
			}
		}

		// use basic superposition first
		Coordinates[] c1 = new Coordinates[match.size() / 2];
		Coordinates[] c2 = new Coordinates[match.size() / 2]; 
		for (int i = 0; i < match.size(); i += 2) {
			c1[i / 2] = new Coordinates(fusion.getCoordinates(match.get(i)));
			c2[i / 2] = new Coordinates(fusion.getCoordinates(match.get(i + 1)));
		}
		Matrix4d M = new Matrix4d();
		M.setIdentity();
		SuperposeCalculator.superpose(c1, c2, M);
		for (int i = 0; i < fusion.getAllAtoms(); i++) {
			if (a2g[i] != 1) {
				Point3d p = new Point3d(fusion.getCoordinates(i).x, fusion.getCoordinates(i).y, fusion.getCoordinates(i).z);
				M.transform(p);
				fusion.getCoordinates(i).x = p.x;
				fusion.getCoordinates(i).y = p.y;
				fusion.getCoordinates(i).z = p.z;
			}
		}

		if(!flexible1 && !flexible2) {
			return algo.optimize(new EvaluableDockRigid(f));
		} else {
			algo.optimize(new EvaluableForceField(f));
			algo.optimize(new EvaluableDockFlex(f));

			// Then remove all superpose terms and optimize again
			if(finer) return 0;
 
			for (int i = 0; i < f.getTerms().size(); i++) {
				AbstractTerm t = f.getTerms().get(i);
				if (t instanceof SuperposeTerm)
					f.getTerms().remove(i--);
			}
			return algo.optimize(new EvaluableForceField(f));
		}
	}
	
	
	

	/**
	 * Takes a molecule consisting of 2 groups and suggest a superposition between the 2.
	 * The result is a List<Integer> formatted as follow: 
	 * - atom1 of 1st molecule
	 * - matching atom1 of 2nd molecule 
	 * - atom2 of 1st molecule
	 * - matching atom2 of 2nd molecule
	 * - ... 
	 * @param fusion 
	 * @return 
	 */
	public static List<Integer> suggestLigandSuperposition(FFMolecule fusion) {

		//Shortcut-> test if same molecules?
		List<FFMolecule> frags = StructureCalculator.extractFragments(fusion);
		if(frags.size()==2 && frags.get(0).getAtoms()==frags.get(1).getAtoms()) {
			boolean ok = true;
			for (int i = 0; i < frags.get(0).getAtoms(); i++) {
				if(frags.get(0).getAtomicNo(i)!=frags.get(1).getAtomicNo(i)) {
					ok = false;
				}
			}
			if(ok) {
				List<Integer> res = new ArrayList<Integer>();
				for (int i = 0; i < frags.get(0).getAllAtoms(); i++) {
					if(frags.get(0).getAtomicNo(i)<=1) continue;
					res.add(i);
					res.add(frags.get(0).getAllAtoms()+i);
				}
				return res;
			}
			
		}
		
		List<int[]> matches = new ArrayList<int[]>(); // atom1, atom2, size

		// Start with n x m potential matches of depth 1 and try to increase the
		// depth until we find only a few points
		int[] a2g = StructureCalculator.getAtomToGroups(fusion);
		for (int i = 0; i < fusion.getAllAtoms(); i++) {
			if (a2g[i] != 1 || fusion.getAtomicNo(i) <= 1) continue;
			for (int j = 0; j < fusion.getAllAtoms(); j++) {
				if (a2g[j] != 2 || fusion.getAtomicNo(j) <= 1) continue;
				if (fusion.getAtomicNo(i) != fusion.getAtomicNo(j)) continue;
				matches.add(new int[] { i, j, 1 });
			}
		}

		if(matches.size()==0) {
			System.err.println("No possible match found");
			return new ArrayList<Integer>();
		}
		
		// Now we increase the depth until we have a limited number of matches
		int depth = 1;
		List<int[]> toBeProcessed = matches;
		List<int[]> all = new ArrayList<int[]>();
		while (toBeProcessed.size() > 10 && depth < 6) {
			matches = new ArrayList<int[]>();
			
			for (int[] p : toBeProcessed) {
				long[] h1 = getHash(fusion, p[0], depth, new boolean[fusion.getAllAtoms()]);
				long[] h2 = getHash(fusion, p[1], depth, new boolean[fusion.getAllAtoms()]);

				if (h1[0] == h2[0] && h1[1] == h2[1]) {
					matches.add(new int[] { p[0], p[1], (int) h1[1] });
				}
			}
			depth++;

			for (int[] is : matches) {
				int index = 0;
				for (; index < all.size(); index++) {
					if (all.get(index)[0] == is[0] && all.get(index)[1] == is[1]) break;
				}
				
				if (index >= all.size()) {
					all.add(is);
				} else {
					all.get(index)[2] = Math.max(all.get(index)[2], is[2]);
				}
			}

			if (matches.size() == 0) break;

			toBeProcessed = matches;
		}

		Collections.sort(all, new Comparator<int[]>() {
			@Override
			public int compare(int[] o1, int[] o2) {
				return o2[2] - o1[2];
			}
		});

//		int cut = -1;
//		Map<Integer, int[]> map = new TreeMap<Integer, int[]>();
//		for (int i = 0; i < all.size(); i++) {
//			if (all.get(i)[2] < 3) {
//				cut = i;
//				break;
//			}
//			map.put(all.get(i)[0], new int[] { all.get(i)[1], all.get(i)[2] });
//		}
//		if (cut > 3) all = all.subList(0, cut);

		List<Integer> sel = new ArrayList<Integer>();

		// Find the best alyphatic atom first
		for (int i = 0; i < all.size() && sel.size() < 1; i++) {
			if (fusion.getAtomicNo(all.get(i)[0]) == 6) continue;
			sel.add(all.get(i)[0]);
			sel.add(all.get(i)[1]);
		}

		int[][] dist = StructureCalculator.getNumberOfBondsBetweenAtoms(fusion, fusion.getAllBonds(), null);
		loop: for (int i = 0; i < all.size() && sel.size() < 16; i++) {
			if (sel.contains(all.get(i)[0]) || sel.contains(all.get(i)[1])) continue;
			if (fusion.getAtomMM2Class(all.get(i)[0]) == 1 || fusion.getAtomMM2Class(all.get(i)[0]) == 2 || fusion.getAtomicNo(all.get(i)[0]) > 16) continue;
			for (int j = 0; j < sel.size(); j += 2) {
				int d1 = dist[sel.get(j)][all.get(i)[0]];
				int d2 = dist[sel.get(j + 1)][all.get(i)[1]];
				if (Math.abs(d1 - d2) > 2) continue loop;
			}
			sel.add(all.get(i)[0]);
			sel.add(all.get(i)[1]);
		}

		// Find others that matches all distances
		for (int i = 0; i < all.size() && sel.size() < 10; i++) {
			if (all.get(i)[2] < 3) break;
			if (sel.contains(all.get(i)[0]) || sel.contains(all.get(i)[1])) continue;
			int sum = 0;
			for (int j = 0; j < sel.size(); j += 2) {
				int d1 = dist[sel.get(j)][all.get(i)[0]];
				int d2 = dist[sel.get(j + 1)][all.get(i)[1]];
				sum += Math.abs(d1 - d2);
			}
			if (sum > 0) continue;
			sel.add(all.get(i)[0]);
			sel.add(all.get(i)[1]);
		}

		return sel;
	}

	private static long[] getHash(FFMolecule mol, int a, int depth, boolean[] visited) {
		if(visited[a]) return new long[]{0, 0};
		visited[a] = true;
		if(depth<=0) return new long[]{mol.getAtomicNo(a), 1};
		
		long hash = 0;
		long size = 1;
		for (int i = 0; i < mol.getAllConnAtoms(a); i++) {
			int a2 = mol.getConnAtom(a, i);
			if(mol.getAtomicNo(a2)<=1 || visited[a2]) continue;
			long[] h2 = getHash(mol, a2, depth-1, visited);			
			hash = (hash + h2[0] * (2<<(depth*4)) ) % (Long.MAX_VALUE / (2<<16));
			size += h2[1];
		}	
		return new long[]{hash, size};
	}

	/**
	 * Converts the ExtendedMolecule by orienting the up/down bonds and
	 * rescaling the molecule, no optimization done at this point 
	 * @param mol
	 * @return a list of isomers
	 */
	public static List<FFMolecule> orientMoleculeForParity(StereoMolecule stereo, boolean generateAll, List<String> errors) {
		if(errors==null) errors = new ArrayList<String>();

		StereoMolecule mol = new StereoMolecule(stereo);
		mol.ensureHelperArrays(Molecule.cHelperParities);
		boolean[] done = new boolean[mol.getAllAtoms()];
		
		//Rescale the molecule if needed
		double avgBondLen = 0;
		for (int i = 0; i < mol.getAllBonds(); i++) {
			int a1 = mol.getBondAtom(0, i);
			int a2 = mol.getBondAtom(1, i);
			double dist = (mol.getAtomX(a1) - mol.getAtomX(a2)) * (mol.getAtomX(a1) - mol.getAtomX(a2)) +
				(mol.getAtomY(a1) - mol.getAtomY(a2)) * (mol.getAtomY(a1) - mol.getAtomY(a2)) +
				(mol.getAtomZ(a1) - mol.getAtomZ(a2)) * (mol.getAtomZ(a1) - mol.getAtomZ(a2));
			avgBondLen += Math.sqrt(dist);
		}
		avgBondLen /= mol.getAllBonds();		
		if(avgBondLen<0.1) {
			new CoordinateInventor().invent(mol);
			return orientMoleculeForParity(mol, generateAll, errors);
		}
		
		if(avgBondLen<1.1 || avgBondLen>1.8) {
			//rescale needed
			double scale = 1.5 / avgBondLen;
			for (int i = 0; i < mol.getAllAtoms(); i++) {
				mol.setAtomX(i, mol.getAtomX(i)*scale);
				mol.setAtomY(i, mol.getAtomY(i)*scale);
				mol.setAtomZ(i, mol.getAtomZ(i)*scale);
			}
		}	
		
		
	
		List<int[]> allRings = new ArrayList<int[]>();
		
		//Handle Lewis form
		for (int j = 0; j < mol.getAtoms(); j++) {
			if(mol.getAtomicNo(j)!=16 && mol.getConnAtoms(j)==4  && (mol.getAtomParity(j)<=0  || mol.getAtomParity(j)>=3)) {
				double factor = mol.getAtomParity(j)!=0?.5: .1;
				int[] biggestAngle = new int[2];
				for (int i = 0; i < 4; i++) {
					for (int k = i+1; k < 4; k++) {
						if(GeometryCalculator.getAngle(mol, mol.getConnAtom(j, i), j, mol.getConnAtom(j, k))>GeometryCalculator.getAngle(mol, biggestAngle[0], j, biggestAngle[1])) {
							biggestAngle[0] = mol.getConnAtom(j, i);
							biggestAngle[1] = mol.getConnAtom(j, k);
						}
					}
				}
				mol.setAtomZ(biggestAngle[0], mol.getAtomZ(biggestAngle[0])+factor);
				mol.setAtomZ(biggestAngle[1], mol.getAtomZ(biggestAngle[1])+factor);
				for (int i = 0; i < 4; i++) {
					int a = mol.getConnAtom(j, i);
					if(a!=biggestAngle[0] && a!=biggestAngle[1]) mol.setAtomZ(a, mol.getAtomZ(a)-.8);
				}
			}
		}

		
		

		//Handle Cage, an atom is in a cage if it non aromatic, 
		//it belongs to 2 rings and 2 neigbours belong to the same 2 rings
		List<Integer>[] atomToRings = StructureCalculator.getRings(new FFMolecule(mol), allRings);
		
		List<FFMolecule> res = new ArrayList<FFMolecule>();
		
		
		for(FFMolecule m: res) {
			//Handle SO2 case
			so2: for (int j = 0; j < mol.getAtoms(); j++) {
				if(m.getAtomicNo(j)==16 && m.getConnAtoms(j)>2) {
					int a2 = -1, a3 = -1;
					for (int i = 0; i < m.getConnAtoms(j); i++) {
						int a = m.getConnAtom(j, i);
						if(m.getAtomicNo(a)==8) {
							if(a2<0) a2 = a;
							else if(a3<0) a3 = a;
							else continue so2;
						}
					}
					if(a3>=0) {//we have SO2, look at the O=S=O angle
						double angle = GeometryCalculator.getAngle(mol, a2, j, a3);
						if(angle<Math.PI*2/3) {
							m.setAtomZ(a2, m.getAtomZ(a2)-.3);
							m.setAtomZ(a3, m.getAtomZ(a3)+.3);
						} else {
							m.setAtomZ(a2, m.getAtomZ(a2)+.3);
							m.setAtomZ(a3, m.getAtomZ(a3)+.3);
							for (int i = 0; i < mol.getConnAtoms(j); i++) {
								int a = m.getConnAtom(j, i);
								if(a!=a2 && a!=a3) {
									m.setAtomZ(a, mol.getAtomZ(a)-.3);
								}
							}
						}
						
					}
				}
			}

		}
		
		for (int j = 0; j < mol.getAtoms(); j++) {
			if(mol.isAromaticAtom(j)) continue;
			List<Integer> rings = atomToRings[j];
			if(rings.size()<2) continue;

			List<Integer> connectors = new ArrayList<Integer>();
			for (int conn = 0; conn < mol.getConnAtoms(j); conn++) {
				int a = mol.getConnAtom(j, conn);
				if(atomToRings[a].size()>=2) connectors.add(a);
			}			 
			if(connectors.size()>=2) { //Cage
				done[j] = true;
			}
		}
	
		
		int errorCode = AdvancedTools.makeIsomers(mol, res, generateAll, new TreeSet<Integer>(), done);
		if(errorCode==1) errors.add("The molecule " +mol.getName()+" has several stereoisomers. Only one is displayed");
		else if(errorCode==2) errors.add("The parity of " +mol.getName()+" could not be checked correctly");
	
		//Fix Amides angles
		for (FFMolecule m : res) {
			for (int a1 = 0; a1 < m.getAllAtoms(); a1++) {
				if(m.getAtomicNo(a1)==8 && m.getAllConnAtoms(a1)==1 && m.getConnBondOrder(a1,0)==2) { //O
					int a2 = m.getConnAtom(a1, 0);
					if(m.getAtomicNo(a2)==6 && m.getAllConnAtoms(a2)==3 && m.getRingSize(a2)<0) { //O=C
						for (int i = 0; i < 3; i++) {
							int a3 = m.getConnAtom(a2, i); 
							if(m.getAtomicNo(a3)==7) { //O=CN
								int a4 = -1;
								int nH = 0;
								if(m.getAllConnAtoms(a3)==3) {//Explicit H?
									for (int j = 0; j < 3; j++) {
										if(m.getAtomicNo(m.getConnAtom(a3, j))==1) {
											nH++;
										} else if(m.getConnAtom(a3, j)!=a2) a4 = m.getConnAtom(a3, j);
									}
								} else if(m.getAllConnAtoms(a3)==2) {//Implicit H?
									if(m.getConnBondOrder(a3, 0)==1 && m.getConnBondOrder(a3, 1)==1) {		
										nH=1;
										if(m.getConnAtom(a3,0)!=a2) a4 = m.getConnAtom(a3,0);
										else a4 = m.getConnAtom(a3,1);										
									}
								}
								if(nH==1 && a4>=0) { 
									//We have an amide O=CN?
									//The angle (OC,N?) must be PI/3
									double angle = m.getCoordinates(a1).getDihedral(m.getCoordinates(a2), m.getCoordinates(a3), m.getCoordinates(a4));									
									if(Math.abs(angle) > Math.PI/2) StructureCalculator.rotateBond(m, a2, a3, -angle);
								}								
							}
						}
					}
//				} else if(m.getAtomicNo(a1)==6 && m.getAllConnAtoms(a1)==4) {
//					//check each atom goes in a different direction
//					Coordinates v1 = m.getCoordinates(m.getConnAtom(a1, 0)).subC(m.getCoordinates(a1));
//					Coordinates v2 = new Coordinates();
//					for (int i = 1; i < 4; i++) {
//						v2.addC(m.getCoordinates(m.getConnAtom(a1, i)).subC(m.getCoordinates(a1)));
//					}
//					if(v2.distSq()<0.1) continue;
//					v2 = v2.unit();
//					
//					if(v1.dot(v2)>0) { //same direction, inverse the direction of the last atom
//						int a3 = m.getConnAtom(a1, 3);
//						m.setCoordinates(a3, m.getCoordinates(a1).subC(v2.scaleC(1.3)));
//					}
					
				}
			}
			
		}		
		
		
		//Center at barycenter
		int count = 0;
		for (FFMolecule m : res) {
			Coordinates center = StructureCalculator.getLigandCenter(m);
			if(res.size()>1) {
				m.setName((m.getName()==null?"":m.getName())+ "-iso"+(++count));
			}
			for (int i = m.getAllAtoms()-1; i >=0; i--) {
				m.setAtomX(i, m.getAtomX(i)-center.x);
				m.setAtomY(i, m.getAtomY(i)-center.y);
				m.setAtomZ(i, m.getAtomZ(i)-center.z);
			}
		}
		
		return res;
	}

	private static int makeIsomers(StereoMolecule mol, List<FFMolecule> res, boolean generateAll, Set<Integer> visitedGroups, boolean[] olddone) {
		FFMolecule m = new FFMolecule(mol);		
		int errorCode = 0;
		boolean[] done = new boolean[mol.getAllAtoms()];
		if(olddone!=null) {
			System.arraycopy(olddone, 0, done, 0, done.length);
		}
		for (int a = 0; a < mol.getAllAtoms(); a++) {
			if(mol.getAtomicNo(a)!=m.getAtomicNo(a)) {
				throw new IllegalArgumentException("Invalid atomicNo!! Copy Incorrect??");
			}
			int parity = mol.getAtomParity(a);
			if(parity==0 || parity==3) continue;

			if(parity==StereoMolecule.cAtomParityUnknown) {
				if(generateAll) {
					//Unknown parity, generate all stereo isomers with a parity of 1 and 2				
					mol.setAtomParity(a, StereoMolecule.cAtomParity1, false);
					errorCode = Math.max(errorCode, makeIsomers(mol, res, generateAll, visitedGroups, done));
					mol.setAtomParity(a, StereoMolecule.cAtomParity2, false);
					errorCode = Math.max(errorCode, makeIsomers(mol, res, generateAll, visitedGroups, done));					
					mol.setAtomParity(a, StereoMolecule.cAtomParityUnknown, false);
					return errorCode;
				} else {
					errorCode = 1;
					m.setAuxiliaryInfo("error", "1");
				}
			}
			
		
			int esrGroup = mol.getAtomESRGroup(a);
			
			if(!visitedGroups.contains(esrGroup)) {
				visitedGroups.add(esrGroup);
				if(mol.getAtomESRType(a)==StereoMolecule.cESRTypeAbs) {
					//Nothing, process the parity as written
				} else if(!generateAll) {
					errorCode = 1;
					m.setAuxiliaryInfo("error", "1");
					//ok but several isomers, returned the isometry of the 2d molecule
				} else {
					//ESRor or ESRand
					//Find all atoms in this esr group
					List<Integer> atomsInGroup = new ArrayList<Integer>();
					for (int a2 = 0; a2 < mol.getAllAtoms(); a2++) {
						if(mol.getAtomESRGroup(a2)==esrGroup) {
							atomsInGroup.add(a2);
						}
					}
					
					//ESRor or ESRand only make sense if there are more than 2 atoms in the group
					if(atomsInGroup.size()>0) {
						//Create the isomers for the inverse parity
//						for (int a2 : atomsInGroup) {
//							mol.setAtomParity(a2, mol.getAtomParity(a2)==1? 2: mol.getAtomParity(a2)==2? 1: mol.getAtomParity(a2), false);							
//						}			
						errorCode = Math.max(errorCode, makeIsomers(mol, res, generateAll, visitedGroups, done));
						
						//Create the isomers for the inverse parity 
						for (int a2 : atomsInGroup) {
							mol.setAtomParity(a2, mol.getAtomParity(a2)==1? 2: mol.getAtomParity(a2)==2? 1: mol.getAtomParity(a2), false);								
						}
						errorCode = Math.max(errorCode, makeIsomers(mol, res, generateAll, visitedGroups, done));
						return errorCode;
					}
					
				}
			}
			
			if(mol.getAllConnAtoms(a)<3) {
				System.err.println(mol.getName() + " - Atom "+a +" has a parity but "+mol.getAllConnAtoms(a)+" connected atoms");
				continue;
			}
			List<Integer> conns = new ArrayList<Integer>();
			for (int i = 0; i < mol.getAllConnAtoms(a); i++) {
				conns.add(mol.getConnAtom(a, i));
			}	
			Collections.sort(conns);
			
			//define normal to the plane
//			System.out.println("atom=" +a +" parity=" + mol.getAtomParity(a)+" cip="+mol.getAtomCIPParity(a));
//			System.out.println("conn = " + conns.get(0)+"("+mol.getAtomicNo(conns.get(0)) + "), " +conns.get(1)+"("+mol.getAtomicNo(conns.get(1))+ "), " +conns.get(2)+"("+ mol.getAtomicNo(conns.get(2))+")");
			
			Coordinates v = m.getCoordinates(conns.get(1)).subC(m.getCoordinates(conns.get(0)));
			Coordinates w = m.getCoordinates(conns.get(2)).subC(m.getCoordinates(conns.get(1)));	
			Coordinates n = v.cross(w);
			Coordinates bary = Coordinates.createBarycenter(new Coordinates[] {m.getCoordinates(conns.get(0)), m.getCoordinates(conns.get(1)), m.getCoordinates(conns.get(2))});
			if(n.distSq()<.01) {
				System.err.println("Plane could not be defined |n|="+n.dist());
				continue;
			}
			n = n.unit();
	
			//move a
			int direction = (parity==1? 1: -1);
			n = n.scaleC(direction);
			m.setCoordinates(a, bary.addC(n.scaleC(.4)));
			if(!done[conns.get(0)] && mol.getAtomParity(conns.get(0))==0) m.setCoordinates(conns.get(0), m.getCoordinates(conns.get(0)).addC(n.scaleC(-.4)));
			if(!done[conns.get(1)] && mol.getAtomParity(conns.get(1))==0) m.setCoordinates(conns.get(1), m.getCoordinates(conns.get(1)).addC(n.scaleC(-.4)));
			if(!done[conns.get(2)] && mol.getAtomParity(conns.get(2))==0) m.setCoordinates(conns.get(2), m.getCoordinates(conns.get(2)).addC(n.scaleC(-.4)));
			done[a] = true;

			if(mol.getConnAtoms(a)>=4) {
				int a3 = mol.getConnAtom(a, 3);
				
				Coordinates t = m.getCoordinates(a3).subC(m.getCoordinates(a));
				Coordinates n2 = n.cross(t);
				if(n2.distSq()<.01) {
					System.err.println("Plane could not be defined |n|="+n.dist());
					continue;
				}
				n2 = n2.unit();

				
				Coordinates nc = m.getCoordinates(a).addC(n.unit().scaleC(1.4));
				double theta = t.getAngle(nc.subC(m.getCoordinates(a)));
				boolean[] seen = new boolean[m.getAllAtoms()];
				seen[a] = true;
				StructureCalculator.dfs(m, a3, seen);
				for (int i = 0; i < seen.length; i++) {
					if(seen[i]) {
						Coordinates nc2 = m.getCoordinates(i).subC(m.getCoordinates(a)).rotate(n2, theta).addC(m.getCoordinates(a));
						m.setCoordinates(i, nc2);
					}					
				}
			}
		}		
		res.add(m);
		
		return errorCode;
	}

	public static void changeRingConformationByAtom(FFMolecule mol, int atomSeed, int confType) throws Exception {
		List<Integer> rings = mol.getAtomToRings()[atomSeed];
		if(rings.size()==0) throw new Exception("No rings");

		int ringNo = -1;
		for (int r : rings) {
			if(mol.getAllRings().get(r).length==6) ringNo = r;
		}
		if(ringNo<0) throw new Exception("No 6-Rings");			
		
		changeRingConformation(mol, ringNo, confType, 1);
	}
	/**
	 * 
	 * @param mol
	 * @param ringNo
	 * @param confType (0==chair-0, 1==chair-1, 2=boat-0, 3=boat-1)
	 * @throws Exception
	 */
	private static double changeRingConformation(FFMolecule mol, int ringNo, int confType, double minRmsd) {
		if(confType<-1 || confType>3) throw new IllegalArgumentException("Invalid conformation type: " + confType);
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMinRMS(minRmsd);
		
		ForceField f = new ForceField(mol);
		double resE = algo.optimize(new EvaluableForceField(f));
		mol.setAuxiliaryInfo("SE", resE);

		if(ringNo<0 || confType<0) return resE;
		
		int[] ring = mol.getAllRings().get(ringNo);
		for(int step=0; step<5; step++) {
			
			//Find Ring plane
			double smallestD = 10;
			int startI = -1;
			Coordinates norm = new Coordinates();
			Coordinates cent = new Coordinates();
			for (int i = 0; i <= 1; i++) {
				int a1 = ring[(i+1)%ring.length];
				int a2 = ring[(i+2)%ring.length];
				int a4 = ring[(i+4)%ring.length];
				int a5 = ring[(i+5)%ring.length];
				
				//Create a normal vector
				//
				//          /3
				//    /5--4/ |
				//   | /1--2/
				//   0/
				//
				Coordinates center = Coordinates.createBarycenter(mol.getCoordinates(a1), mol.getCoordinates(a2), mol.getCoordinates(a4), mol.getCoordinates(a5));
				Coordinates normal = mol.getCoordinates(a4).subC(mol.getCoordinates(a1)).cross(mol.getCoordinates(a5).subC(mol.getCoordinates(a2)));
				if(normal.dist()==0) normal = new Coordinates(Math.random()-.5, Math.random()-.5, Math.random()-.5);//random
				normal = normal.unit();
				
				//Project a4 on the normal vector
				double d = Math.abs(mol.getCoordinates(a4).subC(mol.getCoordinates(a1)).dot(normal));
				if(d<smallestD) {
					startI = i;
					smallestD = d;
					norm = normal;
					cent = center;
				}
			}
			
			int conf;
			if(smallestD>.5) {
				conf = 3;				
			} else {
				int a0 = ring[(startI+0)%ring.length];
				int a1 = ring[(startI+1)%ring.length];
				int a3 = ring[(startI+3)%ring.length];
				
				//Check if a3, a6 are opposite the ring or not
				double dot1 = mol.getCoordinates(a3).subC(cent).dot(norm);
				double dot2 = mol.getCoordinates(a0).subC(cent).dot(norm);
				if(dot1*dot2>0) {
					conf = norm.dot(mol.getCoordinates(a1).subC(mol.getCoordinates(a0)))>0? 2: 3;
				} else {
					conf = norm.dot(mol.getCoordinates(a1).subC(mol.getCoordinates(a0)))>0? 0: 1;
				}				
			}
			
						
			if(confType==conf) {
//				System.out.println("returned conf after "+step+" steps");
				return resE;
			}
			
			int moveAtomNumber;
			boolean vibrate;
			if((confType==0 && conf==1) || (confType==1 && conf==0)) {
				//We want the other chair conformation, move 2 atoms
				moveAtomNumber = 2;
				vibrate = false;
			} else {
				moveAtomNumber = 1;
				if(step%2==1) startI+=3;
				vibrate = true;
			}
//			System.out.println("   "+step+". go from "+conf+" to "+confType);
			//Move a0 or a3 to the the other side of the ring (use the one with the smallest no of connected
			//and rotate all connecting atoms
			for (int moveAtomIndex = 0; moveAtomIndex < moveAtomNumber; moveAtomIndex++) {
				if(moveAtomIndex>0) startI+=3;
				
				int a = ring[(startI+0)%ring.length];
				int ap = ring[(startI+3)%ring.length];
				Coordinates rotationCenter = Coordinates.createBarycenter(mol.getCoordinates(ring[(startI+1)%ring.length]), mol.getCoordinates(ring[(startI+5)%ring.length]));
				
				Coordinates v = mol.getCoordinates(a).subC(rotationCenter);
				Coordinates v2 = v.cross(norm);
				if(v2.distSq()==0) v2 = new Coordinates(Math.random()-.5, Math.random()-.5, Math.random()-.5);
				Coordinates rotateVector = v2.unit();
						
				double angle;
	
				Coordinates ca1 = mol.getCoordinates(a).subC(rotationCenter).rotate(rotateVector, 2*Math.PI/3).addC(rotationCenter);						
				Coordinates ca2 = mol.getCoordinates(a).subC(rotationCenter).rotate(rotateVector, -2*Math.PI/3).addC(rotationCenter);
				if(ca1.distanceSquared(mol.getCoordinates(ap))>ca2.distanceSquared(mol.getCoordinates(ap))) {
					angle = 2*Math.PI/3;
				} else {
					angle = -2*Math.PI/3;						
				}
	
				//update all atoms to the other side of the rings
				boolean[] seen = new boolean[mol.getAllAtoms()];
				for (int at : ring) seen[at] = true;
				seen[a] = false;			
				StructureCalculator.dfs(mol, a, seen);
				for (int at : ring) seen[at] = false;
				seen[a] = true;	
				for (int at=0; at<mol.getAllAtoms(); at++) {		
					if(seen[at]) {
						Coordinates c2 = mol.getCoordinates(at).subC(rotationCenter).rotate(rotateVector, angle).addC(rotationCenter);						
						mol.setCoordinates(at, c2);
					}
				}
				if(vibrate) {
					double scale = step*.05;
					for(int a2: ring) {
						v2 = new Coordinates(Math.random()-.5, Math.random()-.5, Math.random()-.5);
						if(v2.distSq()==0) continue;
						mol.setCoordinates(a2, mol.getCoordinates(a).addC(v2.unit().scaleC(scale)));
					}				
				}
				resE = algo.optimize(new EvaluableForceField(f));
				mol.setAuxiliaryInfo("SE", resE);
	
			}
		}
		System.err.println("Could not return conformation of type "+confType);
		return resE;
	}
	
	public static double changeAmineConformation(FFMolecule mol, int atomNo, double minRmsd) {
		if(mol.getAtomicNo(atomNo)!=7) throw new IllegalArgumentException("Not a N");
		
		int lp = StructureCalculator.connected(mol, atomNo, 0, 1);
		if(lp<0) {
			System.err.println("No Lp connected");
			mol.setAuxiliaryInfo("SE", 99);
			return 99;
		}
		
		//define symmetry plan
		int a1=-1, a2=-1, a3=-1;
		for (int i = 0; i < mol.getAllConnAtoms(atomNo); i++) {
			int a = mol.getConnAtom(atomNo, i);
			if(mol.getAtomicNo(a)>0) {
				if(a1<0) a1 = a;
				else if(a2<0) a2 = a;
				else if(a3<0) a3 = a;
				else {
					System.err.println("N with more than 5 connected???");
					mol.setAuxiliaryInfo("SE", 99);
					return 99;
				}
			}
		}
		if(a3<0) {
//			System.err.println("N amine with less than 3 non-LP connected??? atom="+atomNo+" , connected="+mol.getAllConnAtoms(atomNo));
			mol.setAuxiliaryInfo("SE", 99);
			return 99;
		}
		
		//create symmetry of N and Lp / plan  
		Coordinates cN = Coordinates.getMirror(mol.getCoordinates(atomNo), mol.getCoordinates(a1), mol.getCoordinates(a2), mol.getCoordinates(a3));
		Coordinates cLp = Coordinates.getMirror(mol.getCoordinates(lp), mol.getCoordinates(a1), mol.getCoordinates(a2), mol.getCoordinates(a3));
		
		mol.setCoordinates(atomNo, cN);
		mol.setCoordinates(lp, cLp);
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMinRMS(minRmsd);
		
		double e = algo.optimize(new EvaluableForceField(new ForceField(mol)));
		mol.setAuxiliaryInfo("SE", e);
		return e;
	}

	/**
	 * Generate a list of conformers by setting the rings (5 and 6 members) to any chair, the amines to up or down
	 * 
	 * @param mol
	 * @return a list of molecules sorted by SE (Structure Energy).
	 */
	public static List<FFMolecule> generateRingsAmineConformers(FFMolecule mol) {
		//Be sure to have an energy for the initial structure
		double e = new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(mol)));
		mol.setAuxiliaryInfo("SE", e);
		
		//Then generate rings, and for each rings generate the amines
		List<FFMolecule> all = new ArrayList<FFMolecule>();
		List<FFMolecule> rings = generateRings(mol, 0, .5);
		for(FFMolecule m: rings) {
			List<FFMolecule> amines = generateAmines(m, 0, .5);
			all.addAll(amines);
		}
		
		Collections.sort(all, FFUtils.SE_COMPARATOR);
		return all;
	}
	
	private static List<FFMolecule> generateAmines(FFMolecule mol, int startingAtom, double minRmsd) {
		List<FFMolecule> res = new ArrayList<FFMolecule>();
		for (int i = startingAtom; res.size()<4 && i < mol.getNMovables(); i++) {
			if(mol.getAtomicNo(i)==7 && StructureCalculator.connected(mol, i, 0, 1)>=0) {
				
				FFMolecule copy = new FFMolecule(mol);
				double v = changeAmineConformation(copy, i, minRmsd);
				if(v<99) {
					res.addAll(generateAmines(copy, i+1, minRmsd));
				}
			}
		}
		res.add(mol);
		return res;		
	}
	
	private static List<FFMolecule> generateRings(FFMolecule mol, int startingRing, double minRmsd) {
		List<FFMolecule> res = new ArrayList<FFMolecule>();

		
		List<int[]> rings = mol.getAllRings();
		for (int i = startingRing; res.size()<8 && i < rings.size(); i++) {
			if(rings.get(i).length!=6) continue;
			if(mol.isAtomFlag(rings.get(i)[0], FFMolecule.RIGID)) continue;
			int doubleBonds = 0;
			for (int j = 0; j < 6; j++) {
				int a = StructureCalculator.connected(mol, rings.get(i)[j], -1, 2);
				if(a>=0 && a!=rings.get(i)[(j+1)%6]) doubleBonds++;
			}
			
			if(doubleBonds>1) continue;
			
			for(int conf = 0; conf<=1; conf++) {
				FFMolecule copy = new FFMolecule(mol);
				changeRingConformation(copy, i, conf, minRmsd);
				res.addAll(generateRings(copy, i+1, minRmsd));				
			}
			return res; //stop there because the function before already returned all possibilities
		}
		res.add(mol);
		return res;		
	}

	
	public static void optimizeRings(FFMolecule mol) {
		
		new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(mol)));

		double energy = new ForceField(mol).getTerms().getStructureEnergy();
		
		if(energy>1000) {
			System.err.println("The input molecule is strained (e="+Formatter.format2(energy)+"), reinvent coordinates");
			StructureCalculator.deleteHydrogens(mol);
			final StereoMolecule m = mol.toStereoMolecule();
			m.ensureHelperArrays(Molecule.cHelperParities);
			
			CoordinateInventor c = new CoordinateInventor();
			c.invent(m);
			
			FFMolecule r = AdvancedTools.convertMolecule2DTo3D(new StereoMolecule(m), false, null).get(0);
			mol.clear();
			mol.fusion(r);
		}
		
		
		//OPtimize rings
		List<FFMolecule> mols = generateRingsAmineConformers(mol);
		FFMolecule copy = mols.get(0);
		StructureCalculator.setLigandCoordinates(mol, copy.getCoordinates());		
	}
	
	public static double optimizeByVibrating(FFMolecule mol) {
		return optimizeByVibrating(mol, false, 100);
	}
	
	public static double optimizeByVibrating(FFMolecule mol, boolean hydrogensOnly, int maxIter) {
		FFConfig config = new MM2Config.MM2Basic();
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMinRMS(0.1);
		algo.setMaxIterations(maxIter);
		

		mol.reorderHydrogens();
		if(!hydrogensOnly) algo.optimize(new EvaluableForceField(new ForceField(mol, config)));

		FFMolecule copy = new FFMolecule(mol); 
		
		if(hydrogensOnly) {
			for (int i = 0; i < copy.getNMovables(); i++) {
				if(copy.getAtomicNo(i)>1) copy.setAtomFlag(i, FFMolecule.RIGID, true);
			}			
		}
		ForceField f = new ForceField(copy, config);		
		double bestE = algo.optimize(new EvaluableForceField(f));
		
		Coordinates[] best = new Coordinates[copy.getNMovables()];
		System.arraycopy(copy.getCoordinates(), 0, best, 0, best.length); 
		
		//Vibrate tensed atoms
		boolean tensed = true;
		for (int step = 0; tensed && step < 30; step++) {
			if(step<3 || Math.random()<.3) System.arraycopy(best, 0, copy.getCoordinates(), 0, best.length);
			tensed = false;
			for (int i = 0; i < copy.getNMovables(); i++) {
				if(hydrogensOnly && copy.getAtomicNo(i)>1) continue;

				//If this atom has energy, move it
				if(step<2 && mol.isAromatic(i)) {
					double scale = .4;
					Coordinates v = new Coordinates((Math.random()-.5)*scale, (Math.random()-.5)*scale, (Math.random()-.5)*scale);
					copy.setCoordinates(i, copy.getCoordinates(i).addC(v));
					tensed = true;
				} else if(copy.getConnAtoms(i)==1) {				
					double e = f.getTerms().getEnergy(i, true, false);

					if(e>5) {				
						tensed = true;					
						Coordinates vector = new Coordinates((Math.random()-.5), (Math.random()-.5), (Math.random()-.5));
						double dist = Math.random()*step/30.0+.1;
						if(vector.dist()>0) vector.scale(dist / vector.dist());
						copy.getCoordinates(i).add(vector);									
//						System.out.println(i+"("+mol.getAtomicNo(i)+") > "+e+" > "+copy.getCoordinates(i));
					}
				}
				
			}
			if(tensed) {
				f = new ForceField(copy, config);
				double e = algo.optimize(new EvaluableForceField(f));
//				System.out.println("Vibrate tensed molecule e = "+e);
				if(e<bestE/1.05) {
					System.arraycopy(copy.getCoordinates(), 0, best, 0, best.length); 
					bestE = e;
				}
			}
		}

		System.arraycopy(best, 0, copy.getCoordinates(), 0, best.length); 
		if(hydrogensOnly) {
			for (int i = 0; i < copy.getNMovables(); i++) {
				if(copy.getAtomicNo(i)>1) copy.setAtomFlag(i, FFMolecule.RIGID, false);
			}			
		}
		mol.clear();
		mol.fusion(copy);
		
		return bestE;
	}
	
	/**
	 * Vibrate the molecule a little to avoid being stucked in a planar conformation
	 * @param mol
	 * @param maxIter
	 * @return
	 */
	public static double optimizeByVibratingOld(FFMolecule mol, boolean hydrogensOnly, int maxIter) {
		mol.reorderHydrogens();
		
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMinRMS(0.2);
		algo.setMaxIterations(maxIter);
		

		FFConfig config = new MM2Config.MM2Basic();
		if(!hydrogensOnly) algo.optimize(new EvaluableForceField(new ForceField(mol, config)));
		FFMolecule copy = new FFMolecule(mol);
		if(hydrogensOnly) {
			for (int i = 0; i < copy.getNMovables(); i++) {
				if(copy.getAtomicNo(i)>1) copy.setAtomFlag(i, FFMolecule.RIGID, true);
			}			
		}
		double bestE = Double.MAX_VALUE;
		Coordinates[] best = new Coordinates[copy.getAllAtoms()];
		for (int step = 0; step < 7; step++) {
			if(step>0) {
				System.arraycopy(best, 0, copy.getCoordinates(), 0, best.length);

				for(int i = 0; i<mol.getAllAtoms(); i++) {
					if(((!hydrogensOnly && mol.getAtomicNo(i)>=7) || mol.getAtomicNo(i)<=1 ) && mol.getConnAtoms(i)==1) {
						double scale = .3+step*.15 + (hydrogensOnly?1:0);
						Coordinates v = new Coordinates((Math.random()-.5)*scale, (Math.random()-.5)*scale, (Math.random()-.5)*scale);
						if(copy.getCoordinates(i)==null) copy.setCoordinates(i, new Coordinates());
						copy.setCoordinates(i, copy.getCoordinates(i).addC(v));
					}
				}	
				if(!hydrogensOnly) {
					for(int i = 0; i<mol.getAllAtoms(); i++) {
						if(mol.isAromatic(i)) {
							double scale = .4;
							Coordinates v = new Coordinates((Math.random()-.5)*scale, (Math.random()-.5)*scale, (Math.random()-.5)*scale);
							copy.setCoordinates(i, copy.getCoordinates(i).addC(v));
						}
					}
				}
			}
			
			//optimize
			ForceField f = new ForceField(copy, config);
			double e = algo.optimize(new EvaluableForceField(f));
			if(e<bestE/1.2) {
				System.arraycopy(copy.getCoordinates(), 0, best, 0, best.length); 
				bestE = e;
			}
		}
		System.arraycopy(best, 0, mol.getCoordinates(), 0, best.length); 
		return bestE;
	}
	
	
	public static double optimize(FFMolecule mol) {
		return optimize(mol, 150);
	}
	public static double optimize(FFMolecule mol, int maxIter) {
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMaxIterations(maxIter);
		return algo.optimize(new EvaluableForceField(new ForceField(mol)));
	}
	
}
