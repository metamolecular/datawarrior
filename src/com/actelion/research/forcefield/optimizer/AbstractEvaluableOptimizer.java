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

import com.actelion.research.util.Console;

/**
 * AbstractEvaluableOptimizer is used to describe any optimization done on
 * a IEvaluable class
 * 
 */
public abstract class AbstractEvaluableOptimizer {

	protected Console console = new Console();

	protected double minRMS = 0.1;
	protected int maxIterations = 2000;
	protected int maxTime = 10000;
	protected double RMS;

	/**
	 * Function used to optimize the forcefield
	 * @return the final energy
	 */
	public abstract double optimize(IEvaluable eval);


	
	/**
	 * @return
	 */
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * @return
	 */
	public double getMinRMS() {
		return minRMS;
	}

	/**
	 * @return
	 */
	public double getRMS() {
		return RMS;
	}

	/**
	 * @param d
	 */
	public void setMaxIterations(int d) {
		maxIterations = d;
	}

	/**
	 * @param d
	 */
	public void setMinRMS(double d) {
		minRMS = d;
	}

	/**
	 * @return
	 */
	public int getMaxTime() {
		return maxTime;
	}

	/**
	 * @param i
	 */
	public void setMaxTime(int i) {
		maxTime = i;
	}

}