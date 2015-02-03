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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.FFConfig;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.mm2.MM2Config;
import com.actelion.research.forcefield.optimizer.AlgoLBFGS;
import com.actelion.research.forcefield.optimizer.EvaluableConformation;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.forcefield.optimizer.MultiVariate;
import com.actelion.research.forcefield.transformation.TorsionTransform;

/**
 * 
 * Utility functions to compute torsions
 */
public class TorsionCalculator {
	
	public static void randomizeTorsions(FFMolecule mol) {
		TorsionTransform tt = new TorsionTransform(mol, false);
		tt.random();
		Coordinates[] coords = StructureCalculator.getLigandCoordinates(mol);
		Coordinates[] newCoords = tt.getTransformation(coords);
		System.arraycopy(newCoords, 0, mol.getCoordinates(), 0, newCoords.length);
	}
	
	public static void randomizeTorsions(FFMolecule mol, double maxAngleInRad) {
		TorsionTransform tt = new TorsionTransform(mol, true);
		double[] params = tt.getParameters();
		
		for (int i = 0; i < params.length; i++) {
			params[i] += (Math.random()-.5) * maxAngleInRad * 2;
		}		
		Coordinates[] coords = StructureCalculator.getLigandCoordinates(mol);
		Coordinates[] newCoords = tt.getTransformation(coords);
		System.arraycopy(newCoords, 0, mol.getCoordinates(), 0, newCoords.length);
		
	}
	
	
	/**
	 * For each rotatable bond, computes the possible torsion angles
	 * @param mol
	 * @return a List[] of Double 
	 */
	@SuppressWarnings("unchecked")
	public static List<Double>[] computePossibleTorsions(FFMolecule mol, boolean considerHydrogens) {
		FFMolecule lig = StructureCalculator.extractLigand(mol);
		FFConfig config = new MM2Config.MM2Basic();
		EvaluableConformation eval = new EvaluableConformation(new ForceField(lig, config), considerHydrogens, -1);
		AlgoLBFGS algo = new AlgoLBFGS();		
		algo.setMaxIterations(10); //Low number for speed
		algo.setMinRMS(1); 
		
		MultiVariate v = eval.getState();
		List<Double>[] res = new ArrayList[v.vector.length];

		//Foreach rotatable bond, find the acceptable torsions (without considering vdw, dipoles forces)
		for (int i = 0; i < v.vector.length; i++) {
			res[i] = new ArrayList<Double>();
			List<Double> energies = new ArrayList<Double>();
			double minE = Double.MAX_VALUE;	
			angle: for (double angle = 0; angle < 2*Math.PI; angle+=Math.PI/3) {
				//compute an optimized torsion starting at angle
				v.vector[i] = angle;
				eval.setState(v);
				double e = algo.optimize(eval);
				double torsion = eval.getState().vector[i];
				//make sure torsion is unique
				for(double t: res[i]) {
					double diff = Math.abs(torsion-t) % (2*Math.PI);
					if(diff>Math.PI) diff = 2*Math.PI - diff;
					if(diff<Math.PI/6) continue angle;					
				}
				
				//add this torsion
				res[i].add(torsion);
				energies.add(e);	
				minE = Math.min(minE, e);						
			}
			
			//Remove energies that are too big
			for (Iterator<Double> iter = res[i].iterator(), iter2 = energies.iterator(); iter.hasNext();) {
				iter.next().doubleValue();					
				double e = iter2.next();					
				if(!Double.isNaN(e) && e> minE+50) {
					iter.remove();
					iter2.remove();
				} 
			}
			
		}
		return res; 
	}


	public static List<FFMolecule> createBlobOfConformations(FFMolecule ligand, double withinRMSD, int nConformations) {
		List<FFMolecule> conformers = TorsionCalculator.createAllConformations(ligand);
		AdvancedTools.removeClosestPositions(conformers, nConformations);
		List<FFMolecule> res = new ArrayList<FFMolecule>();
		List<List<FFMolecule>> models = new ArrayList<List<FFMolecule>>();
				
		for (int i = 0; i < conformers.size(); i++) {
			int closestModel = -1;
			double closestRMSD = 100;
			for (int modelNo = 0; modelNo < models.size(); modelNo++) {
				double rmsd = 0;
				for (FFMolecule m : models.get(modelNo)) {
					rmsd = Math.max(rmsd, AdvancedTools.superposeLigands(m, conformers.get(i)));					
				}
				
				if(rmsd<closestRMSD && rmsd<withinRMSD) {
					closestModel = modelNo;
					closestRMSD = rmsd;
				}
			}
			FFMolecule fusion;
			if(closestModel < 0) {
				fusion = new FFMolecule();
				res.add(fusion);
				List<FFMolecule> model = new ArrayList<FFMolecule>();				
				model.add(conformers.get(i));
				models.add(model);
			} else {
				fusion = res.get(closestModel);
			}
			fusion.fusion(conformers.get(i));				
		}
							
		return res;
	}


	public static List<FFMolecule> createBlobOfConformations(FFMolecule ligand, double withinRMSD) {
		return createBlobOfConformations(ligand, withinRMSD, 30);
	}


	/**
	 * Generate a list of diverse conformations by updating the rings, the amines and the torsions
	 * @param mol
	 * @param N the maximum number of most diverse conformations to return, 
	 * @return
	 */
	public static List<FFMolecule> createConformations(FFMolecule mol, int N) {
		final List<FFMolecule> ringConformers = AdvancedTools.generateRingsAmineConformers(mol);
		final List<FFMolecule> conformers = new ArrayList<FFMolecule>();
		for (FFMolecule m : ringConformers) {
			conformers.addAll(TorsionCalculator.createConformations(m, N, ringConformers.size()>1? N: 3 * N, new ArrayList<Double>(), new ArrayList<TorsionTransform>(), 0.7, false, -1));
		}
		
		FFMolecule model = conformers.get(0);
		for (int i = 1; i < conformers.size(); i++) {
			FFMolecule m = conformers.get(i);
			AdvancedTools.superposeLigands(model, m);
		}
		
		AdvancedTools.removeClosestPositions(conformers, N);
		return conformers;
	}


	private static void doAllConfRec(int index, FFMolecule mol, TorsionTransform t, List<Double>[] possibleTorsions, ForceField f, List<FFMolecule> recs) {
		
		if(recs.size()>1800) return;//max
		
		if (index < t.getParameters().length) {
			for (int i = 0; i < possibleTorsions[index].size(); i++) {
				t.getParameters()[index] = possibleTorsions[index].get(i);
				doAllConfRec(index + 1, mol, t, possibleTorsions, f, recs);
			}
		} else {
			Coordinates[] c = t.getTransformation(mol.getCoordinates());
			System.arraycopy(c, 0, f.getMolecule().getCoordinates(), 0, mol.getAllAtoms());

			double e = f.getTerms().getFGValue(null);
			if (e < 1000) {
				FFMolecule r = new FFMolecule(f.getMolecule());
				r.setAuxiliaryInfo("SE", e);
				recs.add(r);
			}
		}
	}


	public static List<FFMolecule> createAllConformations(FFMolecule mol) {
		mol.compact();
		FFMolecule copy = new FFMolecule(mol);
		new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(copy, new MM2Config.MM2Basic())));
		FFConfig config = new MM2Config.DockConfig();
		EvaluableConformation evalTorsions = new EvaluableConformation(new ForceField(copy, config), true, -1);
		TorsionTransform t = (TorsionTransform) evalTorsions.getChain().getElements()[0];
		List<Double>[] possibleTorsions = computePossibleTorsions(copy, true);

		List<FFMolecule> recs = new ArrayList<FFMolecule>();
		FFMolecule m = new FFMolecule(copy);
		ForceField f = new ForceField(m, config);
		

		doAllConfRec(0, copy, t, possibleTorsions, f, recs);

		Collections.sort(recs, new Comparator<FFMolecule>() {
			@Override
			public int compare(FFMolecule o1, FFMolecule o2) {
				return (Double) o1.getAuxiliaryInfo("SE") < (Double) o2.getAuxiliaryInfo("SE") ? -1 : 1;
			}
		});

		AdvancedTools.removeSimilarPositions(recs, .5);
		return recs;

	}


	/**
	 * Generates M conformation and select the N most different ones
	 * 
	 * @param mol
	 * @param N - the number of conformations to return
	 * @param M - the number of conformations to sample
	 * @param energies - a list returning the energy of each conformation
	 * @param transforms  - a list returning the transformation of each conformation
	 * @return
	 */
	public static List<FFMolecule> createConformations(FFMolecule mol, int N,
			int M, List<Double> energies, List<TorsionTransform> transforms, double minTreshold, boolean removeHighEnergy, int centerAtom) {
		
		//long s = System.currentTimeMillis();
		if(transforms==null) transforms = new ArrayList<TorsionTransform>();
		if(energies==null) energies = new ArrayList<Double>();
		
		MM2Config config = new MM2Config();
		config.setMaxDistance(0);
		config.setMaxPLDistance(0);
	
		AlgoLBFGS algo = new AlgoLBFGS();
		algo.setMaxIterations(2000);
		algo.setMinRMS(.5);		
		algo.optimize(new EvaluableForceField(new ForceField(mol, config)));
		
		FFMolecule copy = new FFMolecule(mol);
	
		config.setMaxDistance(30);
		EvaluableConformation evalTorsions = new EvaluableConformation(new ForceField(copy), true, centerAtom);
	
		// Randomize M conformations and optimize them
		energies.clear();
	
		transforms.clear();
		TorsionTransform t = (TorsionTransform) evalTorsions.getChain().getElements()[0];
		t.initPossibleTorsions();
	
		Coordinates[] coords = StructureCalculator.getLigandCoordinates(copy);
		Coordinates[] coords2 = new Coordinates[coords.length];
		List<FFMolecule> res = new ArrayList<FFMolecule>();
		for (int i = 0; i < M; i++) {
			TorsionTransform tt = (TorsionTransform) t.clone();
			tt.random();
			
			System.arraycopy(tt.getTransformation(coords), 0, copy.getCoordinates(), 0, coords.length);
			double e = algo.optimize(new EvaluableConformation(new ForceField(copy), true, -1));
			FFMolecule conformer = new FFMolecule(copy);
	
	
			System.arraycopy(conformer.getCoordinates(), 0, coords2, 0, copy.getAllAtoms());
			System.arraycopy(coords2, 0, conformer.getCoordinates(), 0, copy.getAllAtoms());
			
			res.add(conformer);
			energies.add(new Double(e));
			transforms.add(tt);
		}
		
		// Sort according to the energies
		for (int i = 0; i < res.size(); i++) {
			for (int j = i + 1; j < res.size(); j++) {
				if (energies.get(i).compareTo(energies.get(j)) > 0) {
					res.add(i, res.remove(j));
					energies.add(i, energies.remove(j));
					transforms.add(i, transforms.remove(j));
				}
			}
		}
	
		if(removeHighEnergy) {
			int size = (2*N+M)/3;
			if(size<res.size()) {
				res = res.subList(0, size);
				transforms = transforms.subList(0, size);
			}
		}
			
		// Calculate the distances between those positions
		List<Double> distances = new ArrayList<Double>();
		for (int i = 0; i < res.size(); i++) {
			FFMolecule m1 = res.get(i);
			Coordinates[] c1 = m1.getCoordinates();
			double dist = Double.MAX_VALUE;
	
			for (int j = 0; j < i; j++) {
				FFMolecule m2 = res.get(j);
				Coordinates[] c2 = m2.getCoordinates();
	
				double rmsd = 0;
				for (int h = 0; h < c1.length; h++) {
					rmsd += c1[h].distSquareTo(c2[h]);
				}
				rmsd = Math.sqrt(rmsd / c1.length);
				dist = Math.min(dist, rmsd);
			}
			distances.add(dist);
		}
	
		List<Double> distancesClone = new ArrayList<Double>(distances);
		Collections.sort(distancesClone);
		
		double treshold = Math.max(minTreshold, distancesClone.get(distancesClone.size() - N));
		for (int i = 0; i < res.size(); i++) {
			double dist = distances.get(i);
			if (dist < treshold || i >= N) {
				distances.remove(i);
				res.remove(i);
				transforms.remove(i);
				i--;
			}
		}
	
		return res;
	}


	
		  
}
