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
package com.actelion.research.chem.calculator;

import java.util.*;

import javax.vecmath.*;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.util.ArrayUtils;

/**
 * SuperposeCalculator contains an algo to find the best transformation (Matrix 4x4) 
 * between two (ordered) lists of 3D coordinates
 * 
 * The simplest way to use it is:
 * <pre>
 *   //Superimpose coords2 on top of coords1
 *   SuperposeCalculator.superpose(coords1, coords2, new Matrix4d());
 * </pre>
 * 
 * For superimposing proteins:
 * <pre>
 *   new SuperposeCalculator(SuperposeCalculator.ALGO_BACKBONE).superposeProteins(m1, m2, new Matrix4(), -1);
 * </pre>
 * 
 * For superimposing ligands:
 * <pre>
 *   new SuperposeCalculator(SuperposeCalculator.ALGO_HASH).superposeProteins(m1, m2, new Matrix4(), -1);
 * </pre>
 * 
 */
public class SuperposeCalculator {

	public static final int ALGO_AMINOGROUP = 0;
	public static final int ALGO_BACKBONE = 1;
	public static final int ALGO_AMINOTYPE = 2;
	public static final int ALGO_LIGAND_MATCHING = 3;

	private int algo;
	private int nMatch = 0;
	private boolean fitCavity = true;
	private int fitCavityRadius = 22;

	public SuperposeCalculator(int algo) {
		this.algo = algo;
	}

	/**
	 * Superpose the coordinate c2 on top of model.
	 * The model is kept unchanged, while c2 are changed to the best fit.
	 * The transformation Matrix is copied into M
	 * the function returns the rmsd
	 * 
	 * @param model
	 * @param c2
	 * @param M
	 * @return
	 */
	public static double superpose(final Coordinates[] model,	final Coordinates[] c2, final Matrix4d M) {
		return superpose(model, c2, M, null, -1);
	}

	public static double superpose(final Coordinates[] model,	final Coordinates[] c2, final Matrix4d M, final double[] w) {
		return superpose(model, c2, M, w, -1);
	}

	/**
	 * Superpose c2 onto c1
	 * http://bioserv.rpbs.jussieu.fr/software/QBestFit.c
	 * @param model
	 * @param c2
	 */
	public static double superpose(Coordinates[] model, Coordinates[] c2, final Matrix4d M, final double[] w, double treshHold) {
		int len = model.length;
		if (model.length != c2.length) throw new IllegalArgumentException("superpose: c1 and c2 do not have the same length");
		if (w!=null && model.length != w.length) throw new IllegalArgumentException("superpose: c1 and w do not have the same length");
		
		//Apply transformation for the weight
		if(w!=null) {
			for (int i = 0; i < w.length; i++) {
				if(w[i]<=0) throw new IllegalArgumentException("The weights have to be strictly positive");
				model[i].scale(Math.sqrt(w[i]));
				c2[i].scale(Math.sqrt(w[i]));
			}
		}

		Coordinates[] copy = c2.clone();

		if (len == 0)
			return 0;
		Matrix3d C = XYCov(model, c2);
		Coordinates bc1 = Coordinates.createBarycenter(model);
		Coordinates bc2 = Coordinates.createBarycenter(c2);

		Matrix4d P = new Matrix4d();
		P.m00 = -C.m00 + C.m11 - C.m22;
		P.m01 = P.m10 = -C.m01 - C.m10;
		P.m02 = P.m20 = -C.m12 - C.m21;
		P.m03 = P.m30 = C.m02 - C.m20;

		P.m11 = C.m00 - C.m11 - C.m22;
		P.m12 = P.m21 = C.m02 + C.m20;
		P.m13 = P.m31 = C.m12 - C.m21;

		P.m22 = -C.m00 - C.m11 + C.m22;
		P.m23 = P.m32 = C.m01 - C.m10;

		P.m33 = C.m00 + C.m11 + C.m22;

		Vector4d V = new Vector4d();
		largestEV4(P, V);

		Matrix4d RM = new Matrix4d();
		RM.m00 = -V.x * V.x + V.y * V.y - V.z * V.z + V.w * V.w;
		RM.m10 = 2 * (V.z * V.w - V.x * V.y);
		RM.m20 = 2 * (V.y * V.z + V.x * V.w);
		RM.m30 = 0.;

		RM.m01 = -2 * (V.x * V.y + V.z * V.w);
		RM.m11 = V.x * V.x - V.y * V.y - V.z * V.z + V.w * V.w;
		RM.m21 = 2 * (V.y * V.w - V.x * V.z);
		RM.m31 = 0.;

		RM.m02 = 2 * (V.y * V.z - V.x * V.w);
		RM.m12 = -2 * (V.x * V.z + V.y * V.w);
		RM.m22 = -V.x * V.x - V.y * V.y + V.z * V.z + V.w * V.w;
		RM.m32 = 0.;

		RM.m03 = 0.;
		RM.m13 = 0.;
		RM.m23 = 0.;
		RM.m33 = 1.;

		Matrix4d TY = new Matrix4d();
		TY.setIdentity();
		TY.m30 = -bc2.x;
		TY.m31 = -bc2.y;
		TY.m32 = -bc2.z;

		Matrix4d TX = new Matrix4d();
		TX.setIdentity();
		TX.m30 = bc1.x;
		TX.m31 = bc1.y;
		TX.m32 = bc1.z;

		Matrix4d TMP = new Matrix4d();
		TMP.mul(TY, RM);

		M.mul(TMP, TX);

		M.transpose();

		/* Now superpose the coordinates */
		for (int aDot = 0; aDot < len; aDot++) {
			Point3d p = new Point3d(c2[aDot].x, c2[aDot].y, c2[aDot].z);
			M.transform(p);
			c2[aDot].x = p.x;
			c2[aDot].y = p.y;
			c2[aDot].z = p.z;
		}

		
		if(w!=null) {
			for (int i = 0; i < w.length; i++) {
				model[i].scale(1/Math.sqrt(w[i]));
				c2[i].scale(1/Math.sqrt(w[i]));
			}
		}
		
		
		// Compute squared RMSd
		double squared_rms = 0;
		for (int i = 0; i < len; i++) {
			squared_rms += (w==null?1: w[i]) * model[i].distSquareTo(c2[i]);
		}
		squared_rms = Math.sqrt(squared_rms / model.length);
		
		// Remove points where the distance is above treshold
		if (treshHold > 0) {
			int nArtifacts = 0;
			for (int i = 0; i < model.length; i++) {
				if ((w==null?1: w[i]) * model[i].distSquareTo(c2[i]) > treshHold * treshHold) {
					model[i] = model[model.length - 1];
					copy[i] = copy[model.length - 1];
					model = (Coordinates[]) ArrayUtils.resize(model, model.length - 1);
					copy = (Coordinates[]) ArrayUtils.resize(copy,
							copy.length - 1);
					i--;
					nArtifacts++;
				}
			}
			if (nArtifacts > 0 && nArtifacts < model.length / 4) {
				// System.out.println(nArtifacts+" artifacts");
				// Matrix4d M2 = new Matrix4d();
				squared_rms = superpose(model, copy, M, w, -1);
				System.arraycopy(copy, 0, c2, 0, copy.length);
				// M2.mul(M);
				// M.mul(M2);
			}			
		}

		return squared_rms;

	}

	private static Matrix3d XYCov(Coordinates[] X, Coordinates[] Y) {

		/* Average */
		Coordinates xMean = Coordinates.createBarycenter(X);
		Coordinates yMean = Coordinates.createBarycenter(Y);

		/* Covariance matrix */
		Matrix3d pM = new Matrix3d();

		for (int i = 0; i < X.length; i++) {
			Coordinates Xv = X[i].subC(xMean);
			Coordinates Yv = Y[i].subC(yMean);

			pM.m00 += Xv.x * Yv.x;
			pM.m01 += Xv.x * Yv.y;
			pM.m02 += Xv.x * Yv.z;

			pM.m10 += Xv.y * Yv.x;
			pM.m11 += Xv.y * Yv.y;
			pM.m12 += Xv.y * Yv.z;

			pM.m20 += Xv.z * Yv.x;
			pM.m21 += Xv.z * Yv.y;
			pM.m22 += Xv.z * Yv.z;
		}

		return pM;
	}

	private static int largestEV4(Matrix4d R, Vector4d V) {
		int rs = inversePower(R, 10000, 1E-8, V);
		if (rs == 0) {
			// Temporary solution to avoid singular matrixes
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					R.setElement(i, j, R.getElement(i, j) + Math.random()
							/ 10000.0);
				}
			}
			rs = inversePower(R, 10000, 1E-8, V);
			// rs = shiftPower(M2, 10000, 1.e-8, V);
		}
		return rs;
	}

	/*
	 * private static int shiftPower(Matrix4d a, int maxiter, double eps,
	 * Vector4d v) { double sh; int niter; int i,j;
	 * 
	 * Matrix4d tmp = new Matrix4d(a); sh=best_shift(tmp);
	 * 
	 * niter = power(tmp, maxiter, eps, v); v.x=v.x-sh; return niter; }
	 * 
	 */
	private static int lu_c(Matrix4d a) {

		int err = 1;
		int k = 0;
		while (err == 1 && k < 4) {
			double pivot = a.getElement(k, k);
			if (Math.abs(pivot) >= 1E-8) {
				for (int i = k + 1; i < 4; i++) {
					double coef = a.getElement(i, k) / pivot;
					for (int j = k; j < 4; j++) {
						a.setElement(i, j, a.getElement(i, j) - coef
								* a.getElement(k, j));
					}
					a.setElement(i, k, coef);
				}
			} else
				err = 0;
			k++;
		}
		if (a.getElement(3, 3) == 0)
			err = 0;
		return err;
	}

	private static int inversePower(Matrix4d a, int maxiter, double eps,
			Vector4d w) {

		Vector4d y = new Vector4d(Math.random(), Math.random(), Math.random(),
				Math.random());

		double r = lmaxEstim(a);
		for (int i = 0; i < 4; i++)
			a.setElement(i, i, a.getElement(i, i) - r);

		if (lu_c(a) == 0) {
			return 0;
		}

		int niter = 0;
		double d, l;
		do {
			double normy = y.length();
			w.x = y.x / normy;
			w.y = y.y / normy;
			w.z = y.z / normy;
			w.w = y.w / normy;
			y = new Vector4d(w);

			resolLu(a, y);

			l = w.dot(y);
			niter++;

			double sum = (y.x - l * w.x) * (y.x - l * w.x) + (y.y - l * w.y)
					* (y.y - l * w.y) + (y.z - l * w.z) * (y.z - l * w.z)
					+ (y.w - l * w.w) * (y.w - l * w.w);
			d = Math.sqrt(sum);

		} while (d > eps * Math.abs(l) && niter < maxiter);
		// V.x = r + 1.0 / l;
		return niter;
	}

	private static double lmaxEstim(Matrix4d a) {
		double t = a.m00;
		for (int i = 0; i < 4; i++) {
			double sum = 0;
			for (int j = 0; j < 4; j++) {
				if (j != i)
					sum += Math.abs(a.getElement(i, j));
			}
			t = Math.max(t, a.getElement(i, i) + sum);
		}
		return t;
	}

	private static void resolLu(Matrix4d a, Vector4d b) {
		Vector4d y = new Vector4d();
		y.x = b.x;
		y.y = b.y - a.getElement(1, 0) * y.x;
		y.z = b.z - a.getElement(2, 0) * y.x - a.getElement(2, 1) * y.y;
		y.w = b.w - a.getElement(3, 0) * y.x - a.getElement(3, 1) * y.y
				- a.getElement(3, 2) * y.z;

		b.w = y.w / a.m33;
		b.z = (y.z - a.getElement(2, 3) * b.w) / a.getElement(2, 2);
		b.y = (y.y - a.getElement(1, 2) * b.z - a.getElement(1, 3) * b.w)
				/ a.getElement(1, 1);
		b.x = (y.x - a.getElement(0, 1) * b.y - a.getElement(0, 2) * b.z - a
				.getElement(0, 3)
				* b.w)
				/ a.getElement(0, 0);

	}


	private double superposeAminos(FFMolecule m1, FFMolecule m2, Matrix4d M,
			double treshold) {
		List<List<Integer>> cc1 = StructureCalculator.getConnexComponents(m1);
		List<List<Integer>> cc2 = StructureCalculator.getConnexComponents(m2);

		// Get the backbones
		List<List<Integer>> bb1s = new ArrayList<List<Integer>>();
		List<List<Integer>> bb2s = new ArrayList<List<Integer>>();
		for (int i = 0; i < cc1.size(); i++) {
			// if(lc1<0 || ((List)cc1.get(i)).size()>((List)cc1.get(lc1)).size()
			// ) lc1 = i;

			List<Integer> bb = StructureCalculator.getLongestChain(m1, (cc1.get(i)).get(0).intValue());
			bb = StructureCalculator.getLongestChain(m1, bb.get(0));
			bb1s.add(bb);
		}

		for (int i = 0; i < cc2.size(); i++) {
			// if(lc2<0 || ((List)cc2.get(i)).size()>((List)cc2.get(lc2)).size()
			// ) lc2 = i;

			List<Integer> bb = StructureCalculator.getLongestChain(m2, (cc2.get(i)).get(0).intValue());
			bb = StructureCalculator.getLongestChain(m2, bb.get(0));
			bb2s.add(bb);
		}

		// find the backbone with the best matching pattern
		int lc1 = 0, lc2 = 0;
		int bestMatch = 0;
		for (int i = 0; i < bb1s.size(); i++) {
			for (int j = 0; j < bb2s.size(); j++) {
				String a1 = getAminoProperties(getAminoChain(m1, bb1s.get(i)));
				String a2 = getAminoProperties(getAminoChain(m2, bb2s.get(j)));
				//System.out.println("." + i + ">" + a1);
				//System.out.println("-" + j + ">" + a2);
				int match = Math.max(lcstring(a1, a2).length(), lcstring(a1,
						reverse(a2)).length());
				if (match > bestMatch) {
					lc1 = i;
					lc2 = j;
					bestMatch = match;
				}

			}
		}

		//System.out.println("Superpose match " + bestMatch + " aminos " + bb1s.size() + " " + bb2s.size());
		if (bestMatch == 0)
			return -1;

		// Get the 2 backbones that need to be matched
		List<Integer> bb1 = bb1s.get(lc1);
		List<Integer> bb2 = bb2s.get(lc2);

		// Try to match c2 onto c1
		String aminos1 = getAminoProperties(getAminoChain(m1, bb1));
		String aminos2 = getAminoProperties(getAminoChain(m2, bb2));

		List<Coordinates> coordinates1 = new ArrayList<Coordinates>();
		for (int i = 0; i < bb1.size(); i++) {
			int a = bb1.get(i);

			String atomType = m1.getAtomName(a);
			if (!atomType.equals("CA"))
				continue;

			coordinates1.add(new Coordinates(m1.getCoordinates(a)));
		}

		List<Coordinates> coordinates2 = new ArrayList<Coordinates>();
		for (int i = 0; i < bb2.size(); i++) {
			int a = ((Integer) bb2.get(i)).intValue();

			String atomType = m2.getAtomName(a);
			if (!atomType.equals("CA"))
				continue;

			coordinates2.add(new Coordinates(m2.getCoordinates(a)));
		}

		// Common substring algo
		String lcs1 = lcstring(aminos1, aminos2);
		String lcs2 = lcstring(aminos1, reverse(aminos2));

		if (lcs2.length() > lcs1.length()) {
			aminos2 = reverse(aminos2);
			Collections.reverse(coordinates2);
			lcs1 = lcs2;
		}
		//System.out.println("M1>" + lcstring(aminos1, aminos2));
		//System.out.println("M2>" + lcs(aminos1, aminos2));

		Coordinates[] c1s = new Coordinates[lcs1.length()];
		Coordinates[] c2s = new Coordinates[lcs1.length()];

		int[] match1 = new int[] { aminos1.indexOf(lcs1),
				aminos1.indexOf(lcs1) + lcs1.length() };
		int[] match2 = new int[] { aminos2.indexOf(lcs1),
				aminos2.indexOf(lcs1) + lcs1.length() };
		nMatch = lcs1.length();
		for (int i = match1[0], j = 0; i < match1[1] && j < c1s.length; i++, j++) {
			c1s[j] = coordinates1.get(i);
		}
		for (int i = match2[0], j = 0; i < match2[1] && j < c2s.length; i++, j++) {
			c2s[j] = coordinates2.get(i);
		}

		// Gets the transformation matrix
		return superpose(c1s, c2s, M, null, treshold);
	}

	/*
	 * private double superposeAlgo2(FFMolecule m1, FFMolecule m2, Matrix4d M,
	 * double treshold) {
	 * 
	 * List coordinates1 = new ArrayList(); for (int a = 0; a <
	 * m1.getAllAtoms(); a++) { String atomType = m1.getInfo(a,
	 * FFMolecule.INFO_ATOMNAME); if(!atomType.equals("CA")) continue;
	 * coordinates1.add(new Coordinates(m1.getCoordinates(a))); }
	 * 
	 * List coordinates2 = new ArrayList(); for (int a = 0; a <
	 * m2.getAllAtoms(); a++) { String atomType = m2.getInfo(a,
	 * FFMolecule.INFO_ATOMNAME); if(!atomType.equals("CA")) continue;
	 * coordinates2.add(new Coordinates(m2.getCoordinates(a))); }
	 * 
	 * 
	 * System.out.println(coordinates1.size()+" "+coordinates2.size());
	 * 
	 * Coordinates[] c1s = new
	 * Coordinates[coordinates1.size()*coordinates2.size()]; Coordinates[] c2s =
	 * new Coordinates[coordinates1.size()*coordinates2.size()]; int count = 0;
	 * for (int i = 0; i < coordinates1.size(); i++) { for (int j = 0; j <
	 * coordinates2.size(); j++) { c1s[count] = (Coordinates)
	 * coordinates1.get(i); c2s[count] = (Coordinates) coordinates2.get(j);
	 * count++; } } System.out.println(count+" "); return superpose(c1s, c2s, M,
	 * treshold); }
	 * 
	 */
	private double superposeBackbone(FFMolecule m1, FFMolecule m2, Matrix4d M,
			double treshold) {
		List<List<Integer>> cc1 = StructureCalculator.getConnexComponents(m1);
		List<List<Integer>> cc2 = StructureCalculator.getConnexComponents(m2);

		// Get the backbones
		List<List<Integer>> bb1s = new ArrayList<List<Integer>>();
		List<List<Integer>> bb2s = new ArrayList<List<Integer>>();
		for (int i = 0; i < cc1.size(); i++) {
			List<Integer> bb = StructureCalculator.getLongestChain(m1, (cc1.get(i)).get(0).intValue());
			bb = StructureCalculator.getLongestChain(m1, bb.get(0));
			bb1s.add(bb);
		}

		for (int i = 0; i < cc2.size(); i++) {
			List<Integer> bb = StructureCalculator.getLongestChain(m2, (cc2.get(i)).get(0).intValue());
			bb = StructureCalculator.getLongestChain(m2, bb.get(0));
			bb2s.add(bb);
		}

		// find the longest backbones
		int lc1 = 0, lc2 = 0;
		for (int i = 0; i < bb1s.size(); i++) {
			if(bb1s.get(i).size() > bb1s.get(lc1).size())
				lc1 = i;
		}
		for (int i = 0; i < bb2s.size(); i++) {
			if(bb2s.get(i).size() > bb2s.get(lc2).size())
				lc2 = i;
		}
		List<Integer> bb1 = bb1s.get(lc1);
		List<Integer> bb2 = bb2s.get(lc2);

		int besti = 0, bestj = 0;
		double bestSim = 1000;
		int bestSize = Math.min(bb1.size(), bb2.size());
		for (int size = Math.min(bb1.size(), bb2.size()); bestSim > 3 && size > 10; size = (int)(size*.7-1)) {
			Coordinates[] c1s = new Coordinates[size];
			Coordinates[] c2s = new Coordinates[size];
			for (int i = 0; i + size < bb1.size(); i += size) {
				for (int k = 0; k < c1s.length; k++)
					c1s[k] = new Coordinates(m1.getCoordinates(bb1.get(i + k)));
				
				for (int j = 0; j + size < bb2.size(); j++) {
					for (int k = 0; k < c2s.length; k++)
						c2s[k] = new Coordinates(m2.getCoordinates(bb2.get(j + k)));
					
					double rmsd = superpose(c1s, c2s, M, null, -1);
					if (rmsd < bestSim) {
						bestSim = rmsd;
						//System.out.println("squared_rms = " + bestSim + " size = " + size + " (" + i + "," + j + ")");
						besti = i;
						bestj = j;
						bestSize = size;

					}
				}
			}
		}
		
		List<Coordinates> coords1 = new ArrayList<Coordinates>();
		List<Coordinates> coords2 = new ArrayList<Coordinates>();

		nMatch = bestSize;
		Coordinates center1 = StructureCalculator.getLigandCenter(m1);
		Coordinates center2 = StructureCalculator.getLigandCenter(m2);
		for (int k = 0; k < bestSize; k++) {
			int a1 = bb1.get(besti + k);
			int a2 = bb2.get(bestj + k);
			if(isFitCavity()) {
				if(center1!=null && m1.getCoordinates(a1).distanceSquared(center1)>fitCavityRadius*fitCavityRadius) continue;
				if(center2!=null && m2.getCoordinates(a2).distanceSquared(center2)>fitCavityRadius*fitCavityRadius) continue;
			}
			coords1.add(m1.getCoordinates(a1));
			coords2.add(new Coordinates(m2.getCoordinates(a2)));
			
		}
		
		/*
		//Create the weight matrix
		double[] w = new double[c1s.length];
		Coordinates center1 = StructureCalculator.getLigandCenter(m1);
		Coordinates center2 = StructureCalculator.getLigandCenter(m2);
		if(isFitCavity()) {
			for (int i = 0; i < w.length; i++) {
				w[i] = 10; //((center1!=null? 1.0/c1s[i].distance(center1): 1.0) * (center2!=null? 1.0/c2s[i].distance(center2): 1.0));
			}
		} else {
			Arrays.fill(w, 1);			
		}
		*/
		
		return superpose(coords1.toArray(new Coordinates[]{}), coords2.toArray(new Coordinates[]{}), M, null, treshold);
	}

	private double superposeAminoGroup(FFMolecule m1, FFMolecule m2, Matrix4d M, double treshold) {
		TreeMap<Integer, Integer> ag2Carbon1 = new TreeMap<Integer, Integer>();
		TreeMap<Integer, Integer> ag2Carbon2 = new TreeMap<Integer, Integer>();
		
		for (int i = 0; i < m1.getAllAtoms(); i++) {
			if("CA".equals(m1.getAtomName(i))) {
				try {
					String[] sp = m1.getAtomDescription(i).split(" ");
					ag2Carbon1.put(Integer.parseInt(sp[sp.length-1]), i);
				} catch (Exception e) {}
			}
		}
		for (int i = 0; i < m2.getAllAtoms(); i++) {
			if("CA".equals(m2.getAtomName(i))) {
				try {
					String[] sp = m2.getAtomDescription(i).split(" ");
					ag2Carbon2.put(Integer.parseInt(sp[sp.length-1]), i);
				} catch (Exception e) {}
			}
		}
		
		List<Coordinates> coords1 = new ArrayList<Coordinates>();
		List<Coordinates> coords2 = new ArrayList<Coordinates>();
		 
		Coordinates center1 = StructureCalculator.getLigandCenter(m1);
		Coordinates center2 = StructureCalculator.getLigandCenter(m2);
		for(int missed = 0, count = 0; missed<1000; missed++, count++) {
			Integer a1 = ag2Carbon1.get(count);
			Integer a2 = ag2Carbon2.get(count);
			if(a1==null && a2==null) {missed++; continue;}
			if(a1==null || a2==null) continue;
			if(isFitCavity()) {
				if(center1!=null && m1.getCoordinates(a1).distanceSquared(center1)>fitCavityRadius*fitCavityRadius) continue;
				if(center2!=null && m2.getCoordinates(a2).distanceSquared(center2)>fitCavityRadius*fitCavityRadius) continue;
			}
			
			coords1.add(m1.getCoordinates(a1));
			coords2.add(new Coordinates(m2.getCoordinates(a2)));
			
		}
		//System.out.println("match "+ coords1.size());
		nMatch = coords1.size();
				
		return superpose(coords1.toArray(new Coordinates[]{}), coords2.toArray(new Coordinates[]{}), M, null, treshold);
	}
	
	
	
	private double superposeHash(FFMolecule m1, FFMolecule m2, Matrix4d M, double treshold) {
		List<Coordinates> c1 = new ArrayList<Coordinates>(); 
		List<Coordinates> c2 = new ArrayList<Coordinates>(); 
		
		for (int i = 0; i < m1.getAllAtoms(); i++) {
			if(m1.getAtomicNo(i)<=1 || !m1.isAtomFlag(i, FFMolecule.LIGAND)) continue;
			for (int j = 0; j < m2.getAllAtoms(); j++) {
				if(m2.getAtomicNo(j)<=1 || !m2.isAtomFlag(j, FFMolecule.LIGAND)) continue;

				if(getAtomHashkey(m1,i)!=getAtomHashkey(m2,j)) continue;
				
				c1.add(new Coordinates(m1.getCoordinates(i))); 
				c2.add(new Coordinates(m2.getCoordinates(j))); 
			}			
		}
		
		
		
		return superpose(c1.toArray(new Coordinates[0]), c2.toArray(new Coordinates[0]), M, null, treshold);
	}
	
	
	private static final long getAtomHashkey(FFMolecule mol, int a) {
		return StructureCalculator.getAtomHashkey(mol, a);
	}

	
	 /* Superimpose 2 molecules by assigning a hashkey to each atom and find the
	 * maximum number of atoms that can be superimposed
	 * 
	 * @param m1
	 * @param m2
	 * @param M
	 * @param treshold
	 * @return
	 *
	@SuppressWarnings("unchecked")
	private double superposeHash(FFMolecule m1, FFMolecule m2, Matrix4d M, @SuppressWarnings("unused") double treshold) {

		int[] hash1 = new int[m1.getAllAtoms()];
		int[] hash2 = new int[m2.getAllAtoms()];

		for (int i = 0; i < hash1.length; i++)
			if (m1.getAtomicNo(i) > 1) {
				hash1[i] = getAtomHashkey(m1, i);
			}
		for (int i = 0; i < hash2.length; i++)
			if (m2.getAtomicNo(i) > 1) {
				hash2[i] = getAtomHashkey(m2, i);
			}

		// find the max number of matching atoms

		int[] map = new int[hash1.length];
		Arrays.fill(map, -1);
		boolean[] mapped = new boolean[hash2.length];

		List<Pos> list = new ArrayList<Pos>();
		list.add(new Pos(Double.MAX_VALUE, 0, map, mapped, M));
		Pos best = null;
		for (int match = 2;; match++) {
			List<Pos> previous = list;
			list.clear();

			// Do all combinations
			for (Iterator iter = previous.iterator(); iter.hasNext();) {
				Pos pos = (Pos) iter.next();
				doSuperposeHash(pos, 0, match, m1, m2, hash1, hash2, M, list);
			}

			Collections.sort(list);
			if (list.size() == 0) break;
			best = list.get(0);
			System.out.println(match + ">" + best.score + " size="
					+ list.size());
			if (list.size() > 50)
				list = list.subList(0, 50);

		}

		M.mul(0);
		M.add(best.M);

		return 0;

	}

	private class Pos implements Comparable {
		public Pos(double score, int matched, int[] map, boolean[] mapped,
				Matrix4d M) {
			this.score = score;
			this.map = map;
			this.mapped = mapped;
			this.M = M;
			this.matched = matched;
		}

		double score;

		int matched;

		int[] map;

		boolean[] mapped;

		Matrix4d M;

		public int compareTo(Object o) {
			return score > ((Pos) o).score ? 1 : -1;
		}

		public boolean equals(Object obj) {
			Pos p2 = (Pos) obj;
			for (int i = 0; i < map.length; i++)
				if (map[i] != p2.map[i])
					return false;
			return true;
		}
	}

	private void doSuperposeHash(Pos pos, int index, int match, FFMolecule m1,
			FFMolecule m2, int[] hash1, int[] hash2, Matrix4d M, List<Pos> list) {
		int[] map = pos.map;
		boolean[] mapped = pos.mapped;
		if (list.size() > 500)
			return;
		if (index >= hash1.length) {
			// Not a valid combination

		} else if (pos.matched == match) {
			// Test this combination
			if (match <= 3) {
				// check the distances (no need to superimpose)
				int a = -1, b = -1, c = -1, d = -1, e = -1, f = -1;
				for (int i = 0; i < pos.map.length; i++) {
					if (pos.map[i] >= 0) {
						if (a < 0) {
							a = i;
							c = pos.map[i];
						} else if (b < 0) {
							b = i;
							d = pos.map[i];
						} else {
							e = i;
							f = pos.map[i];
						}
					}
				}
				double d1 = m1.getCoordinates(a).distance(m1.getCoordinates(b));
				double d2 = m2.getCoordinates(c).distance(m2.getCoordinates(d));
				if (Math.abs(d2 - d1) > .5)
					return;

				if (e >= 0) {
					d1 = m1.getCoordinates(a).distance(m1.getCoordinates(e));
					d2 = m2.getCoordinates(c).distance(m2.getCoordinates(f));
					if (Math.abs(d2 - d1) > .5)
						return;
					d1 = m1.getCoordinates(b).distance(m1.getCoordinates(e));
					d2 = m2.getCoordinates(d).distance(m2.getCoordinates(f));
					if (Math.abs(d2 - d1) > .5)
						return;
				}

				Pos p = new Pos(pos.score, pos.matched, map.clone(),
						mapped.clone(), new Matrix4d(M));
				if (!list.contains(p))
					list.add(p);

			} else {
				Coordinates[] coords1 = new Coordinates[match];
				Coordinates[] coords2 = new Coordinates[match];

				for (int i = 0, j = 0; j < coords1.length; i++) {
					if (map[i] >= 0) {
						coords1[j] = m1.getCoordinates(i);
						coords2[j] = new Coordinates(m2.getCoordinates(map[i]));
						j++;
					}
				}
				M = new Matrix4d();
				double score = superpose(coords1, coords2, M, -1);
				// System.out.println(match+">"+score);
				if (score < .5) {
					Pos p = new Pos(score, pos.matched, map.clone(),
							mapped.clone(), new Matrix4d(M));
					if (!list.contains(p))
						list.add(p);
				}

			}

		} else {
			doSuperposeHash(pos, index + 1, match, m1, m2, hash1, hash2, M,
					list);
			if (hash1[index] > 0 && map[index] < 0) {
				for (int i = 0; i < hash2.length; i++) {
					if (mapped[i] || hash1[index] != hash2[i])
						continue;
					map[index] = i;
					mapped[i] = true;
					pos.matched++;
					doSuperposeHash(pos, index + 1, match, m1, m2, hash1,
							hash2, M, list);
					pos.matched--;
					map[index] = -1;
					mapped[i] = false;
				}
			}
		}

	}
*/
	/**
	 * Superpose 2 proteins
	 * 
	 * @param m1
	 *            the model (coordinates not changed)
	 * @param m2
	 *            the molecule to be superposed (coordinates changed)
	 * @return
	 */
	public double superposeProteins(FFMolecule m1, FFMolecule m2, Matrix4d M,
			double treshold) {

		// Gets the transformation matrix

		double rmsd;
		switch (algo) {
		case ALGO_AMINOTYPE:
			rmsd = superposeAminos(m1, m2, M, treshold);
			break;
		case ALGO_BACKBONE:
			rmsd = superposeBackbone(m1, m2, M, treshold);
			break;
		case ALGO_LIGAND_MATCHING:
			rmsd = superposeHash(m1, m2, M, treshold);
			break;
		case ALGO_AMINOGROUP:
			rmsd = superposeAminoGroup(m1, m2, M, treshold);
			break;
		default:
			throw new IllegalArgumentException("Invalid algo");

		}

		if(rmsd>2) return -1;
		
		// Transform the coordinates
		for (int aDot = 0; aDot < m2.getAllAtoms(); aDot++) {
			Coordinates c = m2.getCoordinates(aDot);
			Point3d p = new Point3d(c.x, c.y, c.z);
			M.transform(p);
			c.x = p.x;
			c.y = p.y;
			c.z = p.z;
		}

		return rmsd;
	}

	private static String getAminoProperties(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			sb.append(ProteinTools.getAminoClass(s.charAt(i)));
		}
		return sb.toString();
	}

	private static String getAminoChain(FFMolecule m, List<Integer> bb) {
		StringBuilder aminos = new StringBuilder();
		for (int i = 0; i < bb.size(); i++) {
			int a = bb.get(i);
			String atomType = m.getAtomName(a);

			if (atomType==null || !atomType.equals("CA")) continue;
			if(m.getAtomAmino(a)==null) aminos.append("");
			aminos.append(ProteinTools.getAminoLetter(m.getAtomAmino(a)));
		}
		return aminos.toString();
	}

	private final static String reverse(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = s.length() - 1; i >= 0; i--) {
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

//	/**
//	 * Longest Common Substring Algo
//	 * 
//	 * @param s1
//	 * @param s2
//	 * @return
//	 */
//	private static String lcs(String s1, String s2) {
//		int l[][] = new int[s1.length() + 1][s2.length() + 1];
//		for (int i = s1.length(); i >= 0; i--) {
//			for (int j = s2.length(); j >= 0; j--) {
//				if (i == s1.length() || j == s2.length()) {
//					l[i][j] = 0;
//				} else if (s1.charAt(i) == s2.charAt(j)) {
//					l[i][j] = 1 + l[i + 1][j + 1];
//				} else {
//					l[i][j] = Math.max(l[i + 1][j], l[i][j + 1]);
//				}
//			}
//		}
//
//		String res = "";
//		for (int i = 0, j = 0; i < s1.length() && j < s2.length();) {
//			if (s1.charAt(i) == s2.charAt(j)) {
//				res += s1.charAt(i);
//				i++;
//				j++;
//			} else if (l[i + 1][j] >= l[i][j + 1]) {
//				i++;
//			} else {
//				j++;
//			}
//		}
//		return res;
//	}
	
	/**
	 * Longest Common String Algo
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	private static String lcstring(String s1, String s2) {
		for (int i = s2.length(); i > 0; i--) {
			for (int j = 0; j <= s2.length() - i; j++) {
				String s = s2.substring(j, j + i);
				if (s1.indexOf(s) >= 0)
					return s;
			}
		}
		return "";

	}
/*
	public static void main(String[] args) throws Exception {
		
		FFMolecule[] m = new FFMolecule[] {
				ParserFactory.parse("D:\\1ei1.pdb"),
				ParserFactory.parse("D:\\1kij.pdb"),
		};
		for (int i = 0; i < m.length; i++) {
			StructureCalculator.markLigand(m[i]);
		}

		FFMolecule f = new FFMolecule(m[0]);
		SuperposeCalculator sp = new SuperposeCalculator(ALGO_BACKBONE);
		//sp.setFitCavity(false);
		double[] dists = new double[m.length];
		for (int i = 1; i < dists.length; i++) {
			dists[i] = sp.superposeProteins(m[0], m[i], new Matrix4d(), 3.5);
			System.out
					.println(i + " > " + dists[i] + "  " + m[i].getAllAtoms());
		}
		for (int i = 0; i < dists.length; i++) {
			for (int j = i + 1; j < dists.length; j++) {
				if (dists[i] > dists[j]) {
					double tmp = dists[i];
					dists[i] = dists[j];
					dists[j] = tmp;
					FFMolecule tm = m[i];
					m[i] = m[j];
					m[j] = tm;
				}
			}
		}

		for (int i = 1; i < dists.length; i++) {
			f.fusion(m[i]);
		}

		MoleculeViewer.viewMolecule(f);
	}
*/
	public int getNMatch() {
		return nMatch;
	}

	public boolean isFitCavity() {
		return fitCavity;
	}

	public void setFitCavity(boolean fitCavity) {
		this.fitCavity = fitCavity;
	}

}
