/*
 * Created on Jun 21, 2004
 *
 */
package com.actelion.research.util;

import javax.vecmath.Matrix3d;

import com.actelion.research.chem.Coordinates;

/**
 * 
 * @author freyssj
 */
public class MathUtils {
	public static double getMeanPolarAngle(double ang1, double ang2) {
		double ang = (ang1+ang2)/2;
		if(Math.abs(ang1-ang2)>=Math.PI) {
			if(ang>=0) ang -= Math.PI;
			else ang += Math.PI;
		}
		return ang;
	}
	
	public static double[] getMeanSphericalAngle(double[] ang1, double[] ang2) {
		Coordinates c1 = new Coordinates(
			Math.cos(ang1[0]) * Math.cos(ang1[1]),
			Math.sin(ang1[0]) * Math.cos(ang1[1]),
			Math.sin(ang1[1]));
		Coordinates c2 = new Coordinates(
			Math.cos(ang2[0]) * Math.cos(ang2[1]),
			Math.sin(ang2[0]) * Math.cos(ang2[1]),
			Math.sin(ang2[1]));
			
		Coordinates c3 = c1.addC(c2);
		if(c3.x==0) return new double[]{0,0};
		double[] res = new double[] {
			Math.atan(c3.y/c3.x),
			Math.asin(c3.z/c3.dist())
		};
		if(c3.x<0)       res[0] = res[0] + Math.PI;
		if(res[0]>Math.PI) res[0] -= 2*Math.PI;
		return res;
	}
	
	public static double[][] eulerToMatrix(double heading, double attitude, double bank) { 
		double c1 = Math.cos(heading);
		double s1 = Math.sin(heading);
	    double c2 = Math.cos(attitude);    
	    double s2 = Math.sin(attitude);    
	    double c3 = Math.cos(bank);    
	    double s3 = Math.sin(bank);
		return new double[][] {
			{c1 * c2, -s1 * c2, s2},
			{s1 * c3+(c1 * s2 * s3), (c1*c3) - (s1 * s2 * s3), -c2 * s3},
			{(s1 * s3) - (c1 * s2 * c3), (c1 * s3) + (s1 * s2 * c3), c2*c3}
		};
	}

	public static double[][] inverse(double[][] m) {
		double det = m[0][0] * (m[1][1]*m[2][2] - m[1][2]*m[2][1])  
			- m[0][1] * (m[1][0]*m[2][2] - m[1][2]*m[2][0])
			+ m[0][2] * (m[1][0]*m[2][1] - m[1][1]*m[2][0]);
		System.out.println(det);
		if(det==0) return null; //Non inversible matrix
		
		double[][] res = new double[][] {
			{  m[1][1]*m[2][2]-m[1][2]*m[2][1] , -(m[0][1]*m[2][2]-m[0][2]*m[2][1]),   m[0][1]*m[1][2]-m[0][2]*m[1][1] },
			{-(m[1][0]*m[2][2]-m[1][2]*m[2][0]),   m[0][0]*m[2][2]-m[0][2]*m[2][0] , -(m[0][0]*m[1][2]-m[0][2]*m[1][0])},
			{  m[1][0]*m[2][1]-m[1][1]*m[2][0] , -(m[0][0]*m[2][1]-m[0][1]*m[2][0]),   m[0][0]*m[1][1]-m[0][1]*m[1][0] }
		};
		return res;	 	
	}

	/**
	 * Logarithm with a given base.
	 * @param d
	 * @param base
	 * @return
	 */
	public static double logBase(double d, double base) {
	      return Math.log(d)/Math.log(base);
	}
	
	public static double[][] product(double[][] m1, double[][] m2) {
		double[][] prod = new double[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 3; k++) {
					prod[i][j] = m1[i][k] * m2[k][j];
				}									
			}
		}			
		return prod;
	}
	
	public static double[][] sum(double[][] m1, double[][] m2) {
		double[][] sum = new double[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				sum[i][j] = m1[i][j] + m2[i][j];
			}
		}			
		return sum;
	}
	
	public static double[] matrixToEuler(double[][] m) {
		return new double[] {
			m[0][0]==0?0: -Math.atan(m[0][1]/m[0][0]),
			-Math.asin(-m[0][2]),
			m[2][2]==0?0: -Math.atan(m[1][2]/m[2][2])			
		};
	} 

	public static double getRelativeCorrel(double[] X, double[] Y) {
		int N = X.length;
		double sum = 0;
		int n = 0;
		for (int i = 0; i < N; i++) {
			for (int j = i+1; j < N; j++) {
				if(X[i]==X[j]) continue;
				if(Y[i]==Y[j]) continue;
				if((X[i]-X[j])*(Y[i]-Y[j])>=0) sum++;
				//else sum--;
				n++;
			}
		}
		return sum / n;
	}
	
	protected final static double pow(double d, int n) {
		double res = 1;
		for(int i=0; i<n; i++) res *= d;
		return res;
	}
	
	/**
	 * The taper function is used to smoothen the curve between the CUTOFF and the OFFSET
	 * taper(CUTOFF)=1, dtaper(CUTOFF) = 0 
	 * taper(OFFSET)=0, dtaper(OFFSET) = 0
	 */
	public final static double[] getTaperCoeffs(double CUTOFF, double OFFSET) {
		double DENOM = pow(OFFSET - CUTOFF, 5);
		double C0 = pow(OFFSET,3) * (pow(OFFSET, 2) - 5.0*OFFSET*CUTOFF + 10.0*pow(CUTOFF,2)) / DENOM;
		double C1 = -30.0 * pow(OFFSET, 2) * pow(CUTOFF, 2) / DENOM;
		double C2 = 30.0 * (pow(OFFSET, 2)*CUTOFF + OFFSET*pow(CUTOFF,2)) / DENOM;
		double C3 = -10.0 * (pow(OFFSET, 2) + 4.0*OFFSET*CUTOFF + pow(CUTOFF, 2)) / DENOM;
		double C4 = 15.0 * (OFFSET + CUTOFF) / DENOM;
		double C5 = -6.0 / DENOM;
		return new double[]{1-C0, -C1, -C2, -C3, -C4, -C5};
	}

	public final static double evaluateTaper(double[] coeffs, double x) {
		double res = 0;
		double pow = 1;
		for(int i=0; i<coeffs.length; i++){
			res += coeffs[i] * pow;
			pow *= x;
		}
		return res;
	}
	public final static double evaluateDTaper(double[] coeffs, double x) {
		double res = 0;
		double pow = 1;
		for(int i=1; i<coeffs.length; i++){
			res += i * coeffs[i] * pow;
			pow *= x;
		}
		return res;
	}

	
	
	/**
	 * Creates the Rotation matrix equivalent to a rotation of:
	 *  - angle[0] around the x axis 
	 *  - angle[1] around the y axis 
	 *  - angle[2] around the z axis
	 *  
	 */
	public final static Matrix3d anglesToMatrix(double[] angles) {
		double c1 = Math.cos(angles[0]);
		double c2 = Math.cos(angles[1]);
		double c3 = Math.cos(angles[2]);
		double s1 = Math.sin(angles[0]);
		double s2 = Math.sin(angles[1]);
		double s3 = Math.sin(angles[2]);
		
		return new Matrix3d(new double[]{
			c2*c3,				c2*s3,			-s2,
			-c1*s3+s1*s2*c3,	c1*c3+s1*s2*s3,	s1*c2,
			c1*s2*c3+s1*s3,	c1*s2*s3-s1*c3,	c1*c2
		});		
	}
	
	
	/**
	 * Return an array of [M, dM/d1, dM/d2, dM/d3] where M is the transformation Matrix and dM/di its derivate  
	 * @param angles
	 * @return
	 */
	public final static Matrix3d[] anglesToMatrixAndDerivates(double[] angles) {
		double c1 = Math.cos(angles[0]);
		double c2 = Math.cos(angles[1]);
		double c3 = Math.cos(angles[2]);

		double s1 = Math.sin(angles[0]);
		double s2 = Math.sin(angles[1]);
		double s3 = Math.sin(angles[2]);
		
		return new Matrix3d[]{
			new Matrix3d(new double[]{
				c2*c3,				c2*s3,			-s2,	
				-c1*s3+s1*s2*c3,	c1*c3+s1*s2*s3,	s1*c2,	
				c1*s2*c3+s1*s3,		c1*s2*s3-s1*c3,	c1*c2}),			
			new Matrix3d(new double[]{
				0,					0,					0,			
				(c1*s2*c3+s1*s3),	(c1*s2*s3-s1*c3),	(c1*c2),	
				(c1*s3-s1*s2*c3),	-(c1*c3+s1*s2*s3),	- (s1*c2)}),
			new Matrix3d(new double[]{
				(-s2*c3),	- (s2*s3),	- (c2),		
				(c2*s1*c3),(c2*s1*s3),	- (s2*s1),	
				(c2*c1*c3),(c2*c1*s3),	- (s2*c1)}),
			new Matrix3d(new double[]{
				(-s3*c2),			(c3*c2),			0,	
				(-c3*c1-s3*s1*s2),	(c3*s1*s2-s3*c1),	0,	
				(c3*s1-s3*c1*s2),	(c3*c1*s2+s3*s1),	0})};	
	}			


	public static Matrix3d[] rotationMatrixAndDerivate(double[] vector, double angle) {
		double dist = Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2]);		
		double x = vector[0]/dist;
		double y = vector[1]/dist;
		double z = vector[2]/dist;
		double c = Math.cos(angle);
		double s = -Math.sin(angle);
		double t = 1-c;
		
		double dc = s;
		double ds = -c;
		double dt = -dc;
		
		return new Matrix3d[] {
			new Matrix3d(new double[]{
				t*x*x+c, 		t*x*y-s*z,		t*x*z+s*y,
				t*x*y+s*z,		t*y*y+c,		t*y*z-s*x,
				t*x*z-s*y,		t*z*y+s*x,		t*z*z+c}),
			new Matrix3d(new double[]{ //1st derivate according to angle
				dt*x*x+dc, 		dt*x*y-ds*z,	dt*x*z+ds*y,
				dt*x*y+ds*z,	dt*y*y+dc,		dt*y*z-ds*x,
				dt*x*z-ds*y,	dt*z*y+ds*x,	dt*z*z+dc})};				
	}	
		
	public static final double[] toAngles(Matrix3d val) {
		double s2 = -val.m02;
		double c2 = Math.sqrt(val.m00*val.m00 + val.m01*val.m01);
		
		double c3 = val.m00 / c2;
		double s3 = val.m01 / c2;
		
		double c1 = val.m22 / c2;
		double s1 = val.m12 / c2;
		
		double a1 = Math.atan2(s1, c1);
		double a2 = Math.atan2(s2, c2);
		double a3 = Math.atan2(s3, c3);
		
		return new double[]{a1, a2, a3};
	}


	public static Matrix3d createRotationMatrix(double[] vector, double angle) {
		double dist = Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2]);		
		double x = vector[0]/dist;
		double y = vector[1]/dist;
		double z = vector[2]/dist;
		double c = Math.cos(angle);
		double s = -Math.sin(angle);
		double t = 1-c;
		double[] v = new double[] {
			t*x*x+c, 	t*x*y-s*z,	t*x*z+s*y,
			t*x*y+s*z,	t*y*y+c,	t*y*z-s*x,
			t*x*z-s*y,	t*z*y+s*x,	t*z*z+c
		};	
		return new Matrix3d(v);
	}	

	/**
	 * BoxCox transformation
	 * MvK 27.03.2007
	 * http://en.wikipedia.org/wiki/Box-Cox_transformation
	 * @param val
	 * @param p
	 * @return
	 */
	public static final double boxcoxTransformation(double val, double p) {
		double t = 0;
		
		t = 1.0+(((Math.pow(val,p)-1)/p)*p);
		
		return t;
	}
	

	
	public static void main(String[] args) {
/*
		double r[] = getMeanSphericalAngle(
				new double[]{Math.PI/4, Math.PI},
				new double[]{0, Math.PI/2});
		System.out.println(r[0]+" "+r[1]);
		{
			double[][] t1 = eulerToMatrix(1,1,0);
			double[][] t2 = eulerToMatrix(.5,0,0);
			
			double[][] prod = product(t1, t2);
					
			double[] e = matrixToEuler(t1);
			System.out.println(e[0]+" " +e[1]+" "+e[2]);			
			e = matrixToEuler(t2);
			System.out.println(e[0]+" " +e[1]+" "+e[2]);			
			e = matrixToEuler(prod);
			System.out.println(e[0]+" " +e[1]+" "+e[2]);
		}

		//Matrix3D m = Matrix3D.anglesToMatrix(new double[]{Math.PI/2, Math.PI/2, Math.PI/2});
		//System.out.println(m+" "+m.det());
		*/
		double X[]  = new double[1000];
		double Y[]  = new double[1000];
		for (int i = 0; i < Y.length; i++) {
			X[i] = Math.random();
			Y[i] = i;
		}
		System.out.println(getRelativeCorrel(X, Y));
	}
	
	public static double mean(double[] values) {
		if(values==null || values.length==0) return 0;
		double sum = 0;
		for (double d : values) {
			sum+=d;
		}
		return sum/values.length;
	}
	public static double variance(double[] values) {
		if(values==null || values.length==0) return 0;
		double mean = mean(values);
		
		double sum = 0;
		for (double d : values) {
			sum+= (d-mean)*(d-mean);
		}
		return sum/values.length;
	}
}
