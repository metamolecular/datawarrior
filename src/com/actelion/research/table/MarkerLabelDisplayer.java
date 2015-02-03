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

package com.actelion.research.table;

public interface MarkerLabelDisplayer extends MarkerLabelConstants {
	/**
	 * Assigns a label position (or 'no position') to a column.
	 * @param column a valid compound table column
	 * @param position a valid position index or -1
	 * @param inTreeViewOnly
	 */
	public void setMarkerLabels(int[] columnAtPosition);
    public void setMarkerLabelSize(float size, boolean isAdjusting);
	public void setMarkerLabelsInTreeViewOnly(boolean inTreeViewOnly);
    public float getMarkerLabelSize();
	public int getMarkerLabelColumn(int position);
	public int getMarkerLabelTableEntryCount();
	public boolean supportsMidPositionLabels();
	public boolean supportsMarkerLabelTable();
	public boolean isTreeViewModeEnabled();
	public boolean isMarkerLabelsInTreeViewOnly();
	}
