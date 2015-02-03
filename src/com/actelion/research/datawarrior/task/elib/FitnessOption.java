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
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.elib;

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.task.AbstractTask;

public abstract class FitnessOption {
	protected int mSliderValue;

	protected static FitnessOption createFitnessOption(String params, ProgressListener pl) {
		int index = (params == null) ? -1 : params.indexOf('\t');
		int type = (index == -1) ? -1 : AbstractTask.findListIndex(params.substring(0, index), FitnessPanel.OPTION_CODE, -1);
		return (type == -1) ? null : (type == FitnessPanel.OPTION_STRUCTURE) ?
				new StructureFitnessOption(params.substring(index+1), pl)
			  : new PropertyFitnessOption(type, params.substring(index+1));
		}

	/**
	 * @return 0.25 ... 1.0 ... 4.0
	 */
	public float getWeight() {
		return (float)Math.pow(4.0f, (float)mSliderValue / 100.0f);
		}

	public static String getParamError(String params) {
		int index = (params == null) ? -1 : params.indexOf('\t');
		int type = (index == -1) ? -1 : AbstractTask.findListIndex(params.substring(0, index), FitnessPanel.OPTION_CODE, -1);
		return (type == -1) ? "Fitness option error."
			 : (type == FitnessPanel.OPTION_STRUCTURE) ? StructureFitnessOption.getParamError(params.substring(index+1))
					 								   : PropertyFitnessOption.getParamError(type, params.substring(index+1));
		}

	public abstract float calculateProperty(StereoMolecule mol);
	public abstract float evaluateFitness(float propertyValue);
	public abstract String getName();
	}
