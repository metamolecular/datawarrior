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

import java.util.ArrayList;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Mutation;
import com.actelion.research.chem.StereoMolecule;

public class EvolutionResult implements Comparable<EvolutionResult> {
	private StereoMolecule mMol;
	private String mIDCode;
	private ArrayList<Mutation> mMutationList;
	private int mGeneration,mParentGeneration,mID,mParentID,mChildIndex;
	private float[] mProperty,mFitness;
	private float mOverallFitness;
	private boolean mCoordinatesValid;
	private FitnessOption[] mFitnessOptionList;

	/**
	 * Creates a new EvolutionStep and calculates its fitness.
	 * One of mol and idcode may be null.
	 * @param mol
	 * @param idcode
	 * @param generation
	 */
	public EvolutionResult(StereoMolecule mol, String idcode, EvolutionResult parent, int generation,
						   FitnessOption[] fitnessOptionList, int id) {
		mMol = mol;
		mIDCode = idcode;
		mID = id;
		mGeneration = generation;
		mFitnessOptionList = fitnessOptionList;
		if (parent != null) {
			mParentID = parent.mID;
			mParentGeneration = parent.mGeneration;
			}
		else {
			mParentID = -1;
			mParentGeneration = generation - 1;
			}

		if (mMol == null)
			mMol = new IDCodeParser(true).getCompactMolecule(idcode);
		else if (mIDCode == null)
			mIDCode = new Canonizer(mMol).getIDCode();
		
		calculateIndividualFitness(mMol);
		summarizeFitness();
		}

	public int getChildIndex() {
		return mChildIndex;
		}

	public float getFitness(int i) {
		return mFitness[i];
		}

	public int getGeneration() {
		return mGeneration;
		}

	public int getID() {
		return mID;
		}

	public String getIDCode() {
		return mIDCode;
		}

	public StereoMolecule getMolecule() {
		return mMol;
		}

	public ArrayList<Mutation> getMutationList() {
		return mMutationList;
		}

	public float getOverallFitness() {
		return mOverallFitness;
		}

	public int getParentID() {
		return mParentID;
		}

	public int getParentGeneration() {
		return mParentGeneration;
		}

	public float getProperty(int i) {
		return mProperty[i];
		}

	public void ensureCoordinates() {
		if (!mCoordinatesValid) {
			mCoordinatesValid = true;
			new CoordinateInventor().invent(mMol);
			}
		}

	public void setChildIndex(int i) {
		mChildIndex = i;
		}

	public void setMutationList(ArrayList<Mutation> ml) {
		mMutationList = ml;
		}

	private void calculateIndividualFitness(StereoMolecule mol) {
		mProperty = new float[mFitnessOptionList.length];
		mFitness = new float[mFitnessOptionList.length];

		int index = 0;
		for (FitnessOption fo:mFitnessOptionList) {
			mProperty[index] = fo.calculateProperty(mol);
			mFitness[index] = fo.evaluateFitness(mProperty[index]);
			index++;
			}
		}

	private void summarizeFitness() {
		mOverallFitness = 1.0f;
		float weightSum = 0.0f;
		int index = 0;
		for (FitnessOption fo:mFitnessOptionList) {
			mOverallFitness *= Math.pow(mFitness[index++], fo.getWeight());
			weightSum += fo.getWeight();
			}
		mOverallFitness = (float)Math.pow(mOverallFitness, 1.0f / weightSum);
		}

	/**
	 * Considers EvolutionResults with lower fitness as larger
	 * that TreeSets contain high fitness results first.
	 */
	public int compareTo(EvolutionResult o) {
		return (mOverallFitness < o.mOverallFitness) ? 1
			 : (mOverallFitness > o.mOverallFitness) ? -1
			 : mIDCode.compareTo(o.mIDCode);
		}
	}
