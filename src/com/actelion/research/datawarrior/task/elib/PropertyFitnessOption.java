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

import com.actelion.research.chem.AtomFunctionAnalyzer;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.MolecularFlexibilityCalculator;
import com.actelion.research.chem.prediction.CLogPPredictor;
import com.actelion.research.chem.prediction.PolarSurfaceAreaPredictor;
import com.actelion.research.chem.prediction.SolubilityPredictor;

public class PropertyFitnessOption extends FitnessOption {
	private int mType;
	private float mHalfFitnessWidth,mValueMin,mValueMax;

	public PropertyFitnessOption(int type, String params) {
		mType = type;
		mHalfFitnessWidth = getHalfFitnessWidth(type);
		String[] param = params.split("\\t");
		if (param.length == 3) {
			mValueMin = (param[0].length() == 0) ? Float.NaN : Float.parseFloat(param[0]);
			mValueMax = (param[1].length() == 0) ? Float.NaN : Float.parseFloat(param[1]);
			mSliderValue = Integer.parseInt(param[2]);
			}
		}

	@Override
	public String getName() {
		return FitnessPanel.OPTION_TEXT[mType];
		}

	public static String getParamError(int type, String params) {
		String[] param = params.split("\\t");
		if (param.length != 3)
			return "Wrong parameter count.";

		if (param[0].length() == 0 && param[1].length() == 0)
			return "Min and max values are empty.";

		try {
			float min = (param[0].length() == 0) ? Float.NaN : Float.parseFloat(param[0]);
			float max = (param[1].length() == 0) ? Float.NaN : Float.parseFloat(param[1]);

			if (param[0].length() != 0 && param[1].length() != 0 && min >= max)
				return "Max value of criterion must be larger than min value.";

			return null;
			}
		catch (NumberFormatException nfe) {
			return "Input value is not numerical.";
			}
		}

	private static float getHalfFitnessWidth(int type) {
		return (type == FitnessPanel.OPTION_MOLWEIGHT) ? 50f
			 : (type == FitnessPanel.OPTION_CLOGP) ? 0.5f
			 : (type == FitnessPanel.OPTION_CLOGS) ? 0.5f
			 : (type == FitnessPanel.OPTION_PSA) ? 20f
			 : (type == FitnessPanel.OPTION_ROTATABLEBONDS) ? 5f
			 : (type == FitnessPanel.OPTION_FLEXIBILITY) ? 0.05f
			 : (type == FitnessPanel.OPTION_STEREOCENTERS) ? 2f
			 : (type == FitnessPanel.OPTION_SMALLRINGCOUNT) ? 1f
			 : (type == FitnessPanel.OPTION_AROMRINGCOUNT) ? 1f
			 : (type == FitnessPanel.OPTION_BASIC_NITROGENS) ? 0.5f
			 : (type == FitnessPanel.OPTION_ACIDIC_OXYGENS) ? 0.5f
			 : Float.NaN;
		}

	@Override
	public float calculateProperty(StereoMolecule mol) {
		return (mType == FitnessPanel.OPTION_MOLWEIGHT) ? mol.getMolweight()
			 : (mType == FitnessPanel.OPTION_CLOGP) ? new CLogPPredictor().assessCLogP(mol)
			 : (mType == FitnessPanel.OPTION_CLOGS) ? new SolubilityPredictor().assessSolubility(mol)
			 : (mType == FitnessPanel.OPTION_PSA) ? new PolarSurfaceAreaPredictor().assessPSA(mol)
			 : (mType == FitnessPanel.OPTION_ROTATABLEBONDS) ? mol.getRotatableBondCount()
			 : (mType == FitnessPanel.OPTION_FLEXIBILITY) ? new MolecularFlexibilityCalculator().calculateMolecularFlexibility(mol)
			 : (mType == FitnessPanel.OPTION_STEREOCENTERS) ? mol.getStereoCenterCount()
			 : (mType == FitnessPanel.OPTION_SMALLRINGCOUNT) ? mol.getRingSet().getSize()
			 : (mType == FitnessPanel.OPTION_AROMRINGCOUNT) ? mol.getAromaticRingCount()
			 : (mType == FitnessPanel.OPTION_BASIC_NITROGENS) ? getBasicNitrogenCount(mol)
			 : (mType == FitnessPanel.OPTION_ACIDIC_OXYGENS) ? getAcidicOxygenCount(mol)
			 : Float.NaN;
		}

	private int getBasicNitrogenCount(StereoMolecule mol) {
		int count = 0;
		mol.ensureHelperArrays(Molecule.cHelperRings);
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (AtomFunctionAnalyzer.isBasicNitrogen(mol, atom))
				count++;
		return count;
		}

	private int getAcidicOxygenCount(StereoMolecule mol) {
		int count = 0;
		mol.ensureHelperArrays(Molecule.cHelperRings);
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (AtomFunctionAnalyzer.isAcidicOxygen(mol, atom))
				count++;
		return count;
		}

	@Override
	public float evaluateFitness(float propertyValue) {
		if (!Float.isNaN(mValueMin) && propertyValue < mValueMin)
			return (float)Math.exp((propertyValue-mValueMin)/mHalfFitnessWidth);
		if (!Float.isNaN(mValueMax) && propertyValue > mValueMax)
			return (float)Math.exp((mValueMax-propertyValue)/mHalfFitnessWidth);
		return 1.0f;
		}
	}
