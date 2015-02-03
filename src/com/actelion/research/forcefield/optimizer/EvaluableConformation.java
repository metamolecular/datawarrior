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


import com.actelion.research.forcefield.*;
import com.actelion.research.forcefield.transformation.*;


/**
 * Transformation used to optimize the torsions of the ligand (degrees of freedom = nRotBonds) 
 * 
 */
public class EvaluableConformation extends EvaluableTransformation {
	
	private EvaluableConformation(EvaluableConformation e) {
		super(e.forcefield, (ChainOfTransformations) e.chain.clone(), e.initial);		
	}
	
	public EvaluableConformation(ForceField forcefield) {
		this(forcefield, true, -1);
	}
	public EvaluableConformation(ForceField forcefield, boolean considerHydrogens, int centerAtom) {
		super(forcefield, new ChainOfTransformations(new AbstractTransform[]{new TorsionTransform(forcefield.getMolecule(), 0, null, centerAtom, considerHydrogens, true)}, forcefield.getMolecule()));
	}
	

	
	/**
	 * Clone the multivariate of this function. The forcefield and the initial position
	 * are shared by the original 
	 * @see com.actelion.research.forcefield.optimizer.IEvaluable#clone()
	 */
	@Override
	public EvaluableConformation clone() {
		return new EvaluableConformation(this);
	}

	
	
}