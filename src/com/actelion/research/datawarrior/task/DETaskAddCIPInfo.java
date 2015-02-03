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

package com.actelion.research.datawarrior.task;

import java.util.Properties;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;


public class DETaskAddCIPInfo extends DETaskAbstractAddChemProperty implements Runnable {
	public static final String TASK_NAME = "Add CIP Info";
    private static Properties sRecentConfiguration;

	public DETaskAddCIPInfo(DEFrame parent, boolean isInteractive) {
		super(parent, DESCRIPTOR_NONE, true, true, isInteractive);
		}

	@Override
	public Properties getRecentConfiguration() {
    	return sRecentConfiguration;
    	}

	@Override
	public void setRecentConfiguration(Properties configuration) {
    	sRecentConfiguration = configuration;
    	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
    protected int getNewColumnCount() {
		return 1;
		}

	@Override
    protected String getNewColumnName(int column) {
		return "CIP Info";
		}

	@Override
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
		if (mol.getAllAtoms() == 0)
			return "";

		mol.ensureHelperArrays(Molecule.cHelperParities);
		if (mol.getStereoCenterCount() == 0)
			return "";

		StringBuilder info = new StringBuilder();
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.isAtomStereoCenter(atom)) {
				if (info.length() != 0)
					info.append(" ");
				info.append(""+atom+(mol.getAtomCIPParity(atom)==Molecule.cAtomCIPParityRorM?"R":"S"));
				}
			}
		return info.toString();
		}
	}
