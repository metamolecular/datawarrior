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
package com.actelion.research.chem;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * Class to encapsulate 3D coordinates
 */
public final class Coordinates implements Serializable, Comparable<Coordinates> {
	
	private static final long serialVersionUID = 3112010;
	
	public double x,y,z;
	
	public Coordinates() {}
	
	public Coordinates(Coordinates c) {
		this(c.x, c.y, c.z);
	}

	public Coordinates(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}	
	
	public final double dist() {
		return Math.sqrt(distSq());
	}
	public final double distSq() {
		return  x*x + y*y + z*z;
	}
	
	public final double distanceSquared(Coordinates c) {
		return (c.x-x)*(c.x-x) + (c.y-y)*(c.y-y) + (c.z-z)*(c.z-z);
	}
	public final double distSquareTo(Coordinates c) {
		return distanceSquared(c);
	}
	public final double distance(Coordinates c) {
		return Math.sqrt(distanceSquared(c));
	}
	
	public final double dot(Coordinates c) {
		return x*c.x + y*c.y + z*c.z;
	}

	public final Coordinates cross(Coordinates c) {
		return new Coordinates(y*c.z-z*c.y, -(x*c.z-z*c.x), x*c.y-y*c.x);
	}
	
	/**
	 * Gets the angle formed between the 2 vectors ([0,PI])
	 * @param c
	 * @return angle in radian
	 */
	public final double getAngle(Coordinates c) {
		double d1 = distSq();
		double d2 = c.x*c.x+c.y*c.y+c.z*c.z;
		if(d1==0 || d2==0) return 0;
		double d = dot(c) / Math.sqrt(d1*d2);
		if(d>=1) return 0;
		if(d<=-1) return Math.PI;
		return Math.acos(d);
	}
	
	
	public final double getDihedral(Coordinates c2, Coordinates c3, Coordinates c4) {
		return getDihedral(this, c2, c3, c4);
	}

	public final Coordinates subC(Coordinates c) {
		return new Coordinates(x-c.x, y-c.y, z-c.z);
	}

	public final Coordinates addC(Coordinates c) {
		return new Coordinates(x+c.x, y+c.y, z+c.z);
	}

	public final Coordinates scaleC(double scale) {
		return new Coordinates(x*scale, y*scale, z*scale);
	}

	public final void sub(Coordinates c) {
		x-=c.x;
		y-=c.y;
		z-=c.z;
	}
	public final void add(Coordinates c) {
		x+=c.x;
		y+=c.y;
		z+=c.z;
	}
	public final void scale(double scale) {
		x*=scale;
		y*=scale;
		z*=scale;
	}
	
	public final Coordinates rotate(Coordinates normal, double theta) {
		if(Math.abs(normal.x*normal.x+normal.y*normal.y+normal.z*normal.z-1)>1E-6) throw new IllegalArgumentException("normal needs to a unit vector: "+normal);
		double x = normal.x;
		double y = normal.y;
		double z = normal.z;
		double c = Math.cos(theta);
		double s = Math.sin(theta);
		double t = 1-c;
		Coordinates opp = new Coordinates(
			(t*x*x+c)*this.x 	+ (t*x*y+s*z)*this.y + (t*x*z-s*y)*this.z,
			(t*x*y-s*z)*this.x	+ (t*y*y+c)*this.y 	+ (t*y*z+s*x)*this.z,
			(t*x*z+s*y)*this.x 	+ (t*z*y-s*x)*this.y + (t*z*z+c)*this.z
		);		
		return opp;
	}
	
	
	public final Coordinates unit() {
		double d = dist();
		if(d==0) {
			System.err.println("Cannot call unit() on a null vector");
			return new Coordinates(1,0,0);
		}
		return new Coordinates(x/d, y/d, z/d);		
	}
	
	public final boolean insideBounds(Coordinates[] bounds) {
		return bounds!=null && bounds[0].x<=x && x<=bounds[1].x && bounds[0].y<=y && y<=bounds[1].y && bounds[0].z<=z && z<=bounds[1].z;
	}
	
	@Override
	public final String toString() {
		DecimalFormat df = new DecimalFormat("0.00");
		return "[" + df.format(x) + ", " + df.format(y) + ", " + df.format(z) + "]";
	}
	
	public final String toStringSpaceDelimited() {
		DecimalFormat df = new DecimalFormat("0.00");
		return df.format(x) + " " + df.format(y) + " " + df.format(z);
	}
	
	@Override
	public final boolean equals(Object o) {
		if(o==null || !(o instanceof Coordinates)) return false;
		Coordinates c = (Coordinates) o;
		return Math.abs(c.x-x) + Math.abs(c.y-y) + Math.abs(c.z-z) < 1E-6;
	}
	
	public final boolean isNaN() {
		return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
	}
	
	public final Coordinates min(Coordinates c) {
		return new Coordinates(Math.min(x, c.x), Math.min(y, c.y), Math.min(z, c.z));
	}
	public final Coordinates max(Coordinates c) {
		return new Coordinates(Math.max(x, c.x), Math.max(y, c.y), Math.max(z, c.z));
	}

	
	/////////////////// UTILITITIES ///////////////////////////////////////
	public static final double getNorm(Coordinates[] c) {
		double sum = 0;
		for(int i=0; i<c.length; i++) {
			sum += c[i].distSq(); 
		}
		return Math.sqrt(sum);
	}
	
	public static final Coordinates min(Coordinates[] c) {
		Coordinates min = new Coordinates(c[0]);
		for (int i = 1; i < c.length; i++) {
			min.x = Math.min(c[i].x, min.x);
			min.y = Math.min(c[i].y, min.y);
			min.z = Math.min(c[i].z, min.z);
		}
		return min;
	}
	
	public static final Coordinates max(Coordinates[] c) {
		Coordinates max = new Coordinates(c[0]);
		for (int i = 1; i < c.length; i++) {
			max.x = Math.max(c[i].x, max.x);
			max.y = Math.max(c[i].y, max.y);
			max.z = Math.max(c[i].z, max.z);
		}
		return max;
	}

	public static final Coordinates createBarycenter(Coordinates... coords) {
		if(coords==null) throw new IllegalArgumentException("The coordinates are null");
		Coordinates res = new Coordinates();
		for(int i=0; i<coords.length; i++) {
			res.x += coords[i].x;
			res.y += coords[i].y;
			res.z += coords[i].z;
		}
		res.x /= coords.length;
		res.y /= coords.length;
		res.z /= coords.length;
		return res;
	}
	
	public static Coordinates createBarycenter(Coordinates[] coords, double[] coeffs, boolean divideBySumOfCoeffs) {
		if(coords==null) throw new IllegalArgumentException("The coordinates are null");
		if(coeffs!=null && coords.length != coeffs.length) throw new IllegalArgumentException("The coordinates and coefficients length don't match");
		Coordinates res = new Coordinates();
		double sumCoeffs = 0;
		for(int i=0; i<coords.length; i++) {
			double c = coeffs==null? 1: coeffs[i];
			res.x += c * coords[i].x;
			res.y += c * coords[i].y;
			res.z += c * coords[i].z;
			sumCoeffs += c;
		}
		if(divideBySumOfCoeffs) {
			if(sumCoeffs == 0) throw new IllegalArgumentException("The sum of the coefficients cannot be equal to zero");
			res.x /= sumCoeffs;
			res.y /= sumCoeffs;
			res.z /= sumCoeffs;
		}
		return res;
	}

	/**
	 * Get the mirror image of p through the plane defined by c1, c2, c3
	 * @param p
	 * @param c1
	 * @param c2
	 * @param c3
	 * @return
	 */
	public static final Coordinates getMirror(Coordinates p, Coordinates c1, Coordinates c2, Coordinates c3) {
		//define a unit normal vector to the plane
		Coordinates r31 = new Coordinates(c3);
		r31.sub(c1);
		Coordinates r21 = new Coordinates(c2);
		r21.sub(c1);
		Coordinates c = r31.cross(r21);
		if(c.distSq()<0.05) return new Coordinates(p);
		Coordinates n = c.unit();
		
		Coordinates pc1 = new Coordinates(c1);
		pc1.sub(p);
		double l = pc1.dot(n);
		n.scale(2*l);
		Coordinates pp = new Coordinates(p);
		pp.add(n);
		return pp; 
	}
	
	public static final double getDihedral(Coordinates c1, Coordinates c2, Coordinates c3, Coordinates c4) {
		//Coordinates c1 = this;
		// Calculate the vectors between atoms
		Coordinates r12 = new Coordinates(c1);
		r12.sub(c2);
		Coordinates r23 = new Coordinates(c2);
		r23.sub(c3);
		Coordinates r34 = new Coordinates(c3);
		r34.sub(c4);

		//  Calculate the cross products
		Coordinates A = r12.cross(r23);
		Coordinates B = r23.cross(r34);
		Coordinates C = r23.cross(A);			
					
		//  Calculate the distances
		double rA = A.dist();
		double rB = B.dist();
		double rC = C.dist();
	
		//  Calculate the sin and cos
		//  cos = A*B/(rA*rB)
		//  sin = C*B/(rC*rB)
		double cos_phi = A.dot(B) / (rA * rB);
		double sin_phi = C.dot(B) / (rC * rB);
	
		//  Get phi, assign the sign based on the sine value
		return -Math.atan2(sin_phi, cos_phi);
	}
	
	@Override
	public int compareTo(Coordinates o) {
		if(x!=o.x) return x<o.x?-1:1;
		if(y!=o.y) return y<o.y?-1:1;
		if(z!=o.z) return z<o.z?-1:1;
		return 0;
	}
	
	public static final Coordinates random() {
		Random random = new Random();
		return new Coordinates(random.nextDouble()*2-1, random.nextDouble()*2-1, random.nextDouble()*2-1);
	}
	public static double getRmsd(Coordinates[] c1, Coordinates[] c2) {
		return getRmsd(c1, c2,Math.min(c1.length, c2.length));
	}
	public static double getRmsd(Coordinates[] c1, Coordinates[] c2, int l) {
		double sum = 0;
		for (int i = 0; i < l; i++) {
			sum+= c1[i].distanceSquared(c2[i]);
		}
		return l>0? Math.sqrt(sum/l): 0;
	}
			
}