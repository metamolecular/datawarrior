package com.actelion.research.util;

import java.util.*;

/**
 * Represents a polynomial spline function.
 * @author freyssj, jakarta
 */
public final class FastSpline {
   
	public final static class Polynome  {
		private final double coeffs[]; //x0 x1 x2 x3 
		
		public Polynome(double[] coeffs) {
			this.coeffs = coeffs;			
		}
		
		public final Polynome derivative() {
			return new Polynome(new double[] {coeffs[1], 2*coeffs[2], 3*coeffs[3], 0});
		}

		public final double value(double x) {
			return coeffs[0]+x*(coeffs[1]+x*(coeffs[2]+x*coeffs[3]));			
//			PolynomialFunction
		}
		public final double[] getCoefficients() {
			return coeffs;
		}
	}
    
	
    /** Spline segment interval delimiters (knots).   Size is n+1 for n segments. */
    private final double knots[];

    /**
     * The polynomial functions that make up the spline.  The first element
     * determines the value of the spline over the first subinterval, the
     * second over the second, etc.   Spline function values are determined by
     * evaluating these functions at <code>(x - knot[i])</code> where i is the
     * knot segment to which x belongs.
     */
    private final Polynome polynomials[];
    
    /** 
     * Number of spline segments = number of polynomials
     *  = number of partition points - 1 
     */
    private final int n;
    

    /**
     * Construct a polynomial spline function with the given segment delimiters
     * and interpolating polynomials.
     * <p>
     * The constructor copies both arrays and assigns the copies to the knots
     * and polynomials properties, respectively.
     * 
     * @param knots spline segment interval delimiters
     * @param polynomials polynomial functions that make up the spline
     * @throws NullPointerException if either of the input arrays is null
     * @throws IllegalArgumentException if knots has length less than 2,  
     * <code>polynomials.length != knots.length - 1 </code>, or the knots array
     * is not strictly increasing.
     * 
     */
    public FastSpline(double knots[], Polynome polynomials[]) {
        if (knots.length < 2) throw new IllegalArgumentException("Not enough knot values -- spline partition must have at least 2 points.");
        if (knots.length - 1 != polynomials.length) throw new IllegalArgumentException("Number of polynomial interpolants must match the number of segments.");
        if (!isStrictlyIncreasing(knots)) throw new IllegalArgumentException("Knot values must be strictly increasing.");
        
        this.n = knots.length -1;
        this.knots = new double[n + 1];
        this.polynomials = new Polynome[n];
        
        System.arraycopy(knots, 0, this.knots, 0, n + 1);
        System.arraycopy(polynomials, 0, this.polynomials, 0, n);
    }

    /**
     * Compute the value for the function.
     * <p>
     * Throws FunctionEvaluationException if v is outside of the domain of the
     * function.  The domain is [smallest knot, largest knot).
     * <p>
     * See {@link PolynomialSplineFunction} for details on the algorithm for
     * computing the value of the function.
     * 
     * @param v the point for which the function value should be computed
     * @return the value
     * @throws FunctionEvaluationException if v is outside of the domain of
     * of the spline function (less than the smallest knot point or greater
     * than or equal to the largest knot point)
     */
    public final double value(double v) {    	
    	//if (v < knots[0]) return polynomials[0].value(v - knots[0]);
        int i = Arrays.binarySearch(knots, v);
        if (i < 0) i = -i - 2;
        return polynomials[i].value(v - knots[i]);
    }
        
    /**
     * Returns the derivative of the polynomial spline function as a PolynomialSplineFunction
     * 
     * @return  the derivative function
     */
    public final FastSpline derivative() {
    	Polynome derivativePolynomials[] = new Polynome[n];
        for (int i = 0; i < n; i++) derivativePolynomials[i] = polynomials[i].derivative();
        return new FastSpline(knots, derivativePolynomials);
    }


    /**
     * Returns a copy of the interpolating polynomials array.
     * <p>
     * Returns a fresh copy of the array. Changes made to the copy will
     * not affect the polynomials property.
     * 
     * @return the interpolating polynomials
     */
    public final Polynome[] getPolynomes() {
    	Polynome p[] = new Polynome[n];
        System.arraycopy(polynomials, 0, p, 0, n);
        return p;
    }


    /**
     * Determines if the given array is ordered in a strictly increasing
     * fashion.
     * 
     * @param x the array to examine.
     * @return <code>true</code> if the elements in <code>x</code> are ordered
     * in a stricly increasing manner.  <code>false</code>, otherwise.
     */
    private final static boolean isStrictlyIncreasing(double[] x) {
        for (int i = 1; i < x.length; ++i) if (x[i - 1] >= x[i]) return false;
        return true;
    }
}
