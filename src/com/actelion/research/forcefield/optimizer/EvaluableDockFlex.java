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
package com.actelion.research.forcefield.optimizer;


import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.*;
import com.actelion.research.forcefield.transformation.*;


/**
 * Transformation used to optimize the docking of the ligand inside a protein:
 * trans/rot/torsions (degrees of freedoms = 6 + nRotBonds)
 * 
 * @author freyssj
 */
public class EvaluableDockFlex extends EvaluableTransformation {
	
	private EvaluableDockFlex(EvaluableDockFlex e) {
		super(e.forcefield, (ChainOfTransformations) e.chain.clone(), e.initial);		
	}
	
	public EvaluableDockFlex(ForceField forcefield) {
		this(forcefield, false);
	}
	public EvaluableDockFlex(ForceField forcefield, boolean considerHydrogens) {
		super(forcefield);
		forcefield.getMolecule().reorderAtoms();

		
		List<Integer> seeds = new ArrayList<Integer>();
		int[] a2g = StructureCalculator.getAtomToGroups(forcefield.getMolecule(), seeds);
		List<AbstractTransform> comb = new ArrayList<AbstractTransform>();
		for (int seed : seeds) {
			if(!forcefield.getMolecule().isAtomFlag(seed, FFMolecule.LIGAND)) break;
			if(seed>=forcefield.getMolecule().getNMovables()) break;
			TorsionTransform r = new TorsionTransform(forcefield.getMolecule(), seed, a2g, considerHydrogens);
			TransRotTransform t = new TransRotTransform(forcefield.getMolecule(), forcefield.getMolecule().getCoordinates(r.getCenterAtom()), seed, a2g);
			if(seed==0) comb.add(t);
			comb.add(r);
		}
		ChainOfTransformations c = new ChainOfTransformations(comb.toArray(new AbstractTransform[]{}), forcefield.getMolecule());
			
		//TorsionTransform r = new TorsionTransform(forcefield.getMolecule(), 0, considerHydrogens);
		//TransRotTransform t = new TransRotTransform(forcefield.getMolecule(), forcefield.getMolecule().getCoordinates(r.getAtomCenter()));
		//ChainOfTransformations c = new ChainOfTransformations(new AbstractTransform[]{t, r}, forcefield.getMolecule());
		setChain(c); 
	}
	
	
	/**
	 * @see com.actelion.research.forcefield.optimizer.IEvaluable#clone()
	 */
	@Override
	public EvaluableDockFlex clone() {
		return new EvaluableDockFlex(this);
	}

	
	
}