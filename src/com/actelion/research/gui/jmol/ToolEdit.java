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
package com.actelion.research.gui.jmol;


public abstract class ToolEdit extends AbstractTool {

	public ToolEdit(String name) {
		super(name);
	}
	
	protected void optimize() {
		optimize(false);
	}
	protected void optimize(boolean hydrogenOnly) {
		viewer.optimize(hydrogenOnly);
	}
	protected void optimizeAndCenter(boolean hydrogenOnly) {
		viewer.optimize(hydrogenOnly);
		viewer.script("select hetero; center selected; ");		
	}
	
	
}
