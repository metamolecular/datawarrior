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
 * 
 */
public class AlgoConjugateGradient extends AbstractEvaluableOptimizer {
	@Override
	public double optimize(IEvaluable eval) {
				
		long s = System.currentTimeMillis();		
		int errs = 0; 		
		int iter;
		MultiVariate initial = eval.getState();
		int N = initial.vector.length;
		MultiVariate grad = new MultiVariate(N);
		MultiVariate gradprevious = new MultiVariate(N);
		MultiVariate d = new MultiVariate(N);
		MultiVariate dprevious = new MultiVariate(N);
		double f = eval.getFGValue(grad);
		for(iter=0; iter<maxIterations && errs<3 && grad.getRMS()>minRMS && System.currentTimeMillis()-s<getMaxTime(); iter++) {
			
			System.arraycopy(grad.vector, 0, gradprevious.vector, 0, N);
			System.arraycopy(d.vector, 0, dprevious.vector, 0, N);

			d = grad.scale(-1);
			double normSq = grad.getNormSq();
			if(normSq==0) break;
			for(int i=0; i<grad.vector.length; i++) {
				double beta = grad.vector[i] * (grad.vector[i]-gradprevious.vector[i])/normSq;
				d.vector[i] += dprevious.vector[i]*beta;
			}					   				
			
			Object[] res = AlgoLineSearch.minimizeEnergyAroundDirection(eval, f, grad, d, Math.sqrt(normSq));
			if(res[2]==Boolean.FALSE) {
				errs++;
			}
			f = ((Double)res[0]).doubleValue();
			grad = (MultiVariate) res[1]; 
			
		}	
//System.out.println("CG in "+(System.currentTimeMillis()-s)+"ms "+iter+" iter    errs="+errs+" rms="+grad.getRMS()+"    "+(100*errs/(iter+1))+"%");		
		return f;
	}
	
}
