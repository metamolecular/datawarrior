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
 * Adapted from the Tinker code 
 */
public class AlgoLineSearch {

//	protected static Logger logger = Logger.getLogger(AlgoLineSearch.class.getName());
	
	/**
	 * Minimize the function according to the line search algorithm. 
	 * This function expects the initial FGValue, the direction of search and the initial move 
	 * @param f0
	 * @param function
	 * @param grad
	 * @param search
	 * @param fMove
	 * @return
	 */
	public static final Object[] minimizeEnergyAroundDirection(final IEvaluable function, double f0, final MultiVariate grad, final MultiVariate search, final double fMove) {
		final double CAPPA = 0.9;
		final double STPMIN = 1E-5;
		final double STPMAX = 1;
		final int INTMAX = 3;
		double fA=0, fB=0, fC=0;
		double sgA=0, sgB=0, sgC=0;
		double cube;
		int reSearch = 1;
		int intPln = 0;
		
		//Compute length of Gradient
		final double len = grad.vector.length;
		final double sNorm = search.getNorm();
				
		//Normalize the search vector and find the projected gradient
		double sg0 = 0;
		for(int i=0; i<len; i++) {
			search.vector[i] /= sNorm;
			sg0 += search.vector[i]*grad.vector[i];
		}
			
		double step = Math.min(2.0*Math.abs(fMove/sg0), sNorm);
		if(step>STPMAX) step = STPMAX;
		else if(step<STPMIN) step = STPMIN;
		
		double lambda = 0;
		MultiVariate initial = function.getState();
		try {

			loop10: while(true) {
				boolean restart = true;
				intPln = 0;
				fB = f0;
				sgB = sg0;
	
				//Begin the quadratic procedure
				int counter = 0;
				while(counter++<10) {	
					fA = fB;
					sgA = sgB;
					
					lambda += step;
					move(function, initial, search, lambda);
					
					//get new function
					fB = function.getFGValue(grad);
					sgB = 0; for(int i=0; i<len; i++) sgB += search.vector[i]*grad.vector[i];
					
					//scale stepsize if gradient change is too large					
					if(restart && Math.abs(sgB/sgA)>10000 && step>STPMIN) {								
						lambda = 0;
						step = step / 8;
						continue loop10;					
					} else if(Math.abs(sgB/sg0)<=CAPPA && fB<=fA) {
						//success
						return new Object[]{new Double(fB), grad, Boolean.TRUE};
					}				
					restart = false;
					
					//interpolate if gradient changes sign or function increases
					if(fB>=fA || sgB*sgA<0) break;
					
					
//					step *= 4; //Needed?
//					step *= 2; 
					
					if(sgB>sgA) {
						double parab = (fA - fB) / (sgB - sgA);
						if(parab>2*step) step = 2 * step;
						else if(parab<2*step) step = step / 2;
						else step = parab;
					}
					if(step>STPMAX) step = STPMAX;
				} // end-while

				//Begin the cubic procedure (http://www.mathworks.com/access/helpdesk_r13/help/toolbox/optim/tutori5b.html)
				double cubStep;
				boolean continueLoop = true;
				do {			//Cubic interpolation
					intPln++;
					double sss = 3*(fB-fA)/step - sgA - sgB;
		
					double ttt = sss*sss - sgA*sgB;
					if(ttt<0) {
						//Interpolation error, stop here
//						logger.fine("Interpolation error 1");
						return new Object[]{new Double(fB), grad, Boolean.FALSE};
					}
					ttt = Math.sqrt(ttt);
					cube = step * (sgB + ttt + sss) / (sgB - sgA + 2 * ttt);
					if(cube<0 || cube>step) {
						//Interpolation error, stop here
//						logger.fine("Interpolation error 2: " +cube + "<0 or >" + step);
						return new Object[]{new Double(fB), grad, Boolean.FALSE};
					}					
					lambda -= cube;	
					
					//Get new function and gradient
					move(function, initial, search, lambda);
					fC = function.getFGValue(grad);
					sgC = 0; for(int i=0; i<len; i++) sgC += search.vector[i]*grad.vector[i];
	
	
					if(Math.abs(sgC/sg0)<CAPPA) {
						//Success
						return new Object[]{new Double(fC), grad, Boolean.TRUE};
					}
					cubStep = Math.min(Math.abs(cube), Math.abs(step-cube));
					continueLoop = (fC<=fA || fC<=fB) && cubStep>=STPMIN && intPln<INTMAX;
					if(continueLoop) {
						if( (sgA*sgB<0 && sgA*sgC<0) || (sgA*sgB>=0 && (sgA*sgC<0 || fA<fC)) ) {
							//C becomes the right limit -> B
							fB = fC;
							sgB = sgC;
							step -= cube;
						} else {
							//C becomes the left limit -> A
							fA = fC;
							sgA = sgC;
							step = cube;
							lambda += cube;					
						}
					}
				} while(continueLoop); 
				
				//Cubic Interpolation has failed, reset to best current point
				double fL, sgL;
				if(fA<fB && fA<fC) {
					fL = fA;
					sgL = sgA;
					lambda += cube-step;					
				} else if(fB<fA && fB<fC) {
					fL = fB;
					sgL = sgB;
					lambda += cube;					
				} else /*fC<=fA && fC<=fB*/ {
					fL = fC;
					sgL = sgC;
				}

				if(reSearch==0) {
					//Already restarted, return best current point					
					move(function, initial, search, lambda);
					f0 = function.getFGValue(grad);
					return new Object[]{new Double(f0), grad, Boolean.FALSE};
				}
	
			
				//try to restart from best point with smaller stepsize
				if(fL>f0) {
					move(function, initial, search, lambda);
					f0 = function.getFGValue(grad);
					return new Object[]{new Double(f0), grad, Boolean.FALSE};
				}
				f0 = fL;
				sg0 = sgL;
				if(sgL>0) {
					lambda = 0;
					for(int i=0; i<search.vector.length; i++) search.vector[i] = -search.vector[i];
					sg0 -= -sg0;
				} 
				step = Math.max( STPMIN, Math.max(cube, step-cube) / 16);			
				reSearch--;
			}
		} catch(Exception e) {
			e.printStackTrace();
			function.setState(initial);
			f0 = function.getFGValue(grad);
			return new Object[]{new Double(f0), grad, Boolean.FALSE};				
		}
	}	

	private final static void move(IEvaluable eval, MultiVariate initial, MultiVariate search, double lambda) {
		MultiVariate v = eval.getState();
		for (int i = 0; i < v.vector.length; i++) {
			v.vector[i] = initial.vector[i] + lambda * search.vector[i]; 
		}
		eval.setState(v);
	}
	
}
