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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;

public class DETaskAddLargestFragment extends DETaskAbstractAddChemProperty {
	public static final String TASK_NAME = "Add Largest Fragment";
    private static Properties sRecentConfiguration;

    public DETaskAddLargestFragment(DEFrame parent, boolean isInteractive) {
		super(parent, DESCRIPTOR_NONE, false, true, isInteractive);
		}

	@Override
	protected int getNewColumnCount() {
		return 2;
		}

	@Override
	protected String getNewColumnName(int column) {
		// is done by setNewColumnProperties()
		return "";
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
	protected void setNewColumnProperties(int firstNewColumn) {
		String sourceColumnName = getTableModel().getColumnTitle(getStructureColumn());
		getTableModel().prepareStructureColumns(firstNewColumn, "Largest Fragment of " + sourceColumnName, true, false);
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null) {
			mol.stripSmallFragments();
			Canonizer canonizer = new Canonizer(mol);
			getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn);
			getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstNewColumn+1);
			}
		}
	}
