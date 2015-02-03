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

/**
 * IEvaluable is the interface used to describe a function in the system.
 * It has a state (defined by a multivariate) and can return its value and gradient
 * 
 * @author freyssj
 */
public interface IEvaluable extends Cloneable {
	
	/**
	 * Sets the current state
	 * @param var
	 */
	public void setState(MultiVariate var);
	
	/**
	 * Gets the current state
	 * @return
	 */
	public MultiVariate getState();
	
	/**
	 * Returns the (energy) value at the current state and its gradient
	 * @param grad
	 * @return
	 */
	public double getFGValue(MultiVariate grad);
	
	public Object clone();
	
}
