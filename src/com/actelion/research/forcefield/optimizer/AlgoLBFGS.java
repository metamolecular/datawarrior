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

import java.util.*;

/**
 * LFGS Algo adapted from the Tinker code
 */
public class AlgoLBFGS extends AbstractEvaluableOptimizer {

	private int timeBetweenIterationsMs = 0;
	

	/**
	 * Optimization routine using the limited Broyden-Fletcher-Goldfarb-Shanno algorithm
	 */	
	@Override
	public synchronized double optimize(IEvaluable eval) {
		long start = System.currentTimeMillis();
		MultiVariate initial = eval.getState();
		int N = initial.vector.length;

		final int MSAV = Math.max(1, Math.min(8, N)); 
		double[] alpha = new double[MSAV];
		double[] rho = new double[MSAV];
		int iter = 0;
		double gamma = 1;
		int m = 0;
		int nErrors = 0;

		// evaluate the function and get the initial gradient
		MultiVariate grad = new MultiVariate(N);
		double f = eval.getFGValue(grad);

		double fOld = f; 
		double f0 = f; 
		double gNorm;			
		double fMove = 0;

		if(N==0) return f0;


		MultiVariate oldX = initial; 
		MultiVariate oldGradient = new MultiVariate(N);
		double[][] s = new double[MSAV][N]; 
		double[][] y = new double[MSAV][N]; 
		double[] h0 = new double[N];
		double[] q = new double[N];
		MultiVariate r = new MultiVariate(N);
	
//		for (int i = 0; i < s.length; i++) {
//			s[i] = new MultiVariate(N);
//			y[i] = new MultiVariate(N);
//		}

		boolean restart = true;
		int mUse = 0;
		while(iter<maxIterations) {
			if(timeBetweenIterationsMs>0) {
				try {Thread.sleep(timeBetweenIterationsMs);} catch (Exception e) {}
			}
			iter++;
			if(restart) {
				mUse = 0;
				f = eval.getFGValue(grad);
				gamma = 1; 
				fMove = .25 * grad.getNorm();
				restart = false;
			}
			
			gNorm = grad.getNorm();
			RMS = gNorm / Math.sqrt(N);

			if(RMS<minRMS) {
				break;
			} else if(System.currentTimeMillis()-start>maxTime) {
				break;
			} else if(nErrors>2) {
				break;
			}
			
			//Estimate Hessian diagonal
			m = (m+1) % MSAV;
			Arrays.fill(h0, gamma);
			System.arraycopy(grad.vector, 0, q, 0, N);

			int k = m;
			for(int j=0; j<mUse; j++) {
				k = k==0? MSAV-1: k-1;
				alpha[k] = 0;
				for(int i=0; i<N; i++) alpha[k] += s[k][i] * q[i];
				alpha[k] *= rho[k];
				for(int i=0; i<N; i++) q[i] -= y[k][i] * alpha[k];
			}
			for(int i=0; i<N; i++) r.vector[i] = h0[i] * q[i];
			for(int j=0; j<mUse; j++) {
				double beta = 0;
				for(int i=0; i<N; i++) beta += y[k][i] * r.vector[i];
				beta *= rho[k];
				for(int i=0; i<N; i++) r.vector[i] += s[k][i] *(alpha[k]-beta);
				k = (k + 1) % MSAV;
			}
			
			//set search direction and store current point and gradient
			for(int i = 0; i<N; i++) {
				r.vector[i] = -r.vector[i];
			}
			
			oldX = eval.getState();
			System.arraycopy(grad.vector, 0, oldGradient.vector, 0, N);
			
			//perform line search along the new conjugate direction
			Object[] res = AlgoLineSearch.minimizeEnergyAroundDirection(eval, f, grad, r, fMove);
			f = ((Double)res[0]).doubleValue();
			grad = (MultiVariate) res[1]; 
			if(res[2]==Boolean.FALSE) {
				nErrors++; 
				restart=true;
			}


			//Update variables
			double ys = 0, yy = 0;
			MultiVariate newState = eval.getState();
			for(int i=0; i<N; i++) {
				s[m][i] = newState.vector[i] - oldX.vector[i];
				y[m][i] = grad.vector[i] - oldGradient.vector[i];
				ys += y[m][i]*s[m][i];
				yy += y[m][i]*y[m][i];
			}
			gamma = Math.abs(ys/yy);
			if(ys==0) {restart = true; continue;}
			 
			rho[m] = 1.0 / ys;			
			fMove = fOld - f;
			fOld = f;			
			mUse = Math.min(mUse+1, MSAV);

		}

		
		if(f>f0) {
			eval.setState(initial);
			f = f0;
		}
		
				
		return f;
	}
	
	/**
	 * @return
	 */
	@Override
	public int getMaxTime() {
		return maxTime;
	}

	/**
	 * Sets the maximum time in ms
	 * @param i
	 */
	@Override
	public void setMaxTime(int i) {
		maxTime = i;
	}
	

	public int getTimeBetweenIterationsMs() {
		return timeBetweenIterationsMs;
	}

	public void setTimeBetweenIterationsMs(int timeBetweenIterationsMs) {
		this.timeBetweenIterationsMs = timeBetweenIterationsMs;
	}

}
