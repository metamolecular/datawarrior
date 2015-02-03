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

package com.actelion.research.table.view;

import com.actelion.research.table.CompoundRecord;


public class VisualizationPoint {
	public int screenX,screenY,width,height;
	protected int chartGroupIndex;
	protected byte colorIndex,shape,exclusionFlags,hvIndex;
	protected CompoundRecord record;

	protected VisualizationPoint(CompoundRecord r) {
		record = r;
		colorIndex = 0;
		shape = 0;
        hvIndex = 0;
		}
	}
