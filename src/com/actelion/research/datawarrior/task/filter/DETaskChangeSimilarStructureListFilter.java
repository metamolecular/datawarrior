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

package com.actelion.research.datawarrior.task.filter;

import java.awt.Frame;
import java.util.Properties;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.filter.JMultiStructureFilterPanel;

public class DETaskChangeSimilarStructureListFilter extends DEAbstractFilterTask {
	public static final String TASK_NAME = "Change Similar Structure List Filter";

	private static Properties sRecentConfiguration;

	public DETaskChangeSimilarStructureListFilter(Frame parent, DEPruningPanel pruningPanel, JFilterPanel filter) {
		super(parent, pruningPanel, filter);
		}

	@Override
	public boolean supportsColumnSelection() {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
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
	public JFilterPanel createFilterUI() {
		return new JMultiStructureFilterPanel(getParentFrame(), getTableModel(), false);
		}

	@Override
	protected String getColumnQualificationError(int column) {
		return CompoundTableConstants.cColumnTypeIDCode.equals(getTableModel().getColumnSpecialType(column)) ? null
				: "Structure filters can be applied to structure columns only.";
		}

	@Override
	public Class<? extends JFilterPanel> getFilterClass() {
		return JMultiStructureFilterPanel.class;
		}
	}
