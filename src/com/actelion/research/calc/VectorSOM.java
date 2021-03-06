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
 * @author Thomas Sander
 */

package com.actelion.research.calc;

import java.io.*;
import java.util.Random;
import com.actelion.research.util.DoubleFormat;

public class VectorSOM extends SelfOrganizedMap {
	private double[]	mMeanParameter,mStandardDeviation;
	private int			mParameterCount;

	public VectorSOM() {}
			// constructor to be used if SOM interna are read from a SOM file with read()

	public VectorSOM(int nx, int ny, int mode) {
		super(nx, ny, mode);
		}
	
	public void setParameterCount(int parameterCount) {
	    	// needs to be called before organize()
		mParameterCount = parameterCount;
		}

	protected void initializeNormalization() {
			// must be overridden if input vectors aren't double arrays
		startProgress("Calculating mean parameters...", 0, mController.getInputVectorCount());

			// don't normalize input vectors if they are random vectors
		if (mController.getInputVectorCount() == -1) {
			mMeanParameter = new double[mParameterCount];
			mStandardDeviation = new double[mParameterCount];
			for (int i=0; i<mParameterCount; i++) {
				mMeanParameter[i] = 0.0;
				mStandardDeviation[i] = 1.0;
				}
			return;
			}

		mMeanParameter = (double[])mController.getInputVector(0);

		for (int row=0; row<mController.getInputVectorCount(); row++) {
			if (threadMustDie())
				break;
			updateProgress(row);

			double[] vector = (double[])mController.getInputVector(row);

			for (int i=0; i<mParameterCount; i++)
				mMeanParameter[i] += vector[i];
			}
		if (!threadMustDie())
			for (int i=0; i<mParameterCount; i++)
				mMeanParameter[i] /= (double)mController.getInputVectorCount();

		startProgress("Calculating variance...", 0, mController.getInputVectorCount());
		mStandardDeviation = new double[mParameterCount];
		for (int row=0; row<mController.getInputVectorCount(); row++) {
			if (threadMustDie())
				break;
			updateProgress(row);

			double[] vector = (double[])mController.getInputVector(row);

			for (int i=0; i<mParameterCount; i++) {
				double dif = vector[i] - mMeanParameter[i];
				mStandardDeviation[i] += dif * dif;
				}
			}
		if (!threadMustDie())
			for (int i=0; i<mParameterCount; i++)
			    mStandardDeviation[i] = Math.sqrt(mStandardDeviation[i]/(double)mController.getInputVectorCount());
		}

	public void write(BufferedWriter writer) throws IOException {
		super.write(writer);

		writer.write("<meanParameter=\""+doubleArrayToString(mMeanParameter)+"\">");
		writer.newLine();

		writer.write("<standardDeviation=\""+doubleArrayToString(mStandardDeviation)+"\">");
		writer.newLine();
		}

	public void read(BufferedReader reader) throws Exception {
		super.read(reader);

		String theLine = reader.readLine();
		boolean error = !theLine.startsWith("<meanParameter=");
		if (!error) {
			mMeanParameter = stringToDoubleArray(extractValue(theLine));
			mParameterCount = mMeanParameter.length;
			theLine = reader.readLine();
			if (theLine.startsWith("<variance=")) {	// old SOM files contains the variance
			    mStandardDeviation = stringToDoubleArray(extractValue(theLine));
			    for (int i=0; i<mStandardDeviation.length; i++)
			        mStandardDeviation[i] = Math.sqrt(mStandardDeviation[i]);
				}
			else if (theLine.startsWith("<standardDeviation=")) {
			    mStandardDeviation = stringToDoubleArray(extractValue(theLine));
				}
			else {
			    error = true;
				}
			}

		if (error)
			throw new IOException("Invalid SOM file format");
		}

	protected String referenceVectorToString(int x, int y) {
		return doubleArrayToString((double[])mReferenceVector[x][y]);
		}

	public static String doubleArrayToString(double[] d) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<d.length; i++) {
			if (i != 0)
				buf.append('\t');
			buf.append(DoubleFormat.toString(d[i]));
			}
		return buf.toString();
		}

	protected void setReferenceVector(int x, int y, String ref) throws Exception {
			// must be overridden if input vectors aren't double arrays
		mReferenceVector[x][y] = stringToDoubleArray(ref);
		}

	public static double[] stringToDoubleArray(String s) {
		int size = 1;
		for (int i=0; i<s.length(); i++)
			if (s.charAt(i) == '\t')
				size++;

		int startPosition = 0;
		int tabPosition = s.indexOf('\t');
		double[] d = new double[size];
		for (int i=0; i<size-1; i++) {
			d[i] = Double.parseDouble(s.substring(startPosition, tabPosition));
			startPosition = tabPosition + 1;
			tabPosition = s.indexOf('\t', startPosition);
			}
		d[size-1] = Double.parseDouble(s.substring(startPosition));
		return d;
		}

	public double getDissimilarity(Object vector1, Object vector2) {
			// must be overridden if input vectors aren't double arrays

/*	    	// this would be Tanimoto which requires positive vector values
	    	// and therefore doesn't work with normalized, centered vectors
	    	// which are used by the VectorSOM
	    double[] v1 = (double[])vector1;
		double[] v2 = (double[])vector2;

        double dAtB = 0.0;
        double dAtA = 0.0;
        double dBtB = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dAtA += v1[i] * v1[i];
            dAtB += v1[i] * v2[i];
            dBtB += v2[i] * v2[i];
        	}

        return 1.0 - dAtB / (dAtA + dBtB - dAtB);	*/

	    double[] v1 = (double[])vector1;
		double[] v2 = (double[])vector2;

		double sum = 0.0;
		for (int i=0; i<v1.length; i++) {
			double dif = v1[i] - v2[i];
			sum += dif * dif;
			}

		// euclidian dissimilarity normalized by SQRT(dimensionCount)
		return Math.sqrt(sum)/Math.sqrt(v1.length);
		}

	protected void updateReference(Object inputVector, Object referenceVector, double influence) {
			// must be overridden if input vectors aren't double arrays
		double[] input = (double[])inputVector;
		double[] reference = (double[])referenceVector;
		for (int i=0; i<reference.length; i++)
			reference[i] += influence * (input[i] - reference[i]);
		}

	protected Object getRandomVector() {
	    Random random = new Random();
	    double[] vector = new double[mParameterCount];
		for (int i=0; i<mParameterCount; i++)
		    vector[i] = random.nextGaussian();
		return vector;
		}

	protected Object getMeanVector(Object vector1, Object vector2) {
			// must be overridden if input vectors aren't double arrays
		double[] v1 = (double[])vector1;
		double[] v2 = (double[])vector2;
		double[] mv = new double[v1.length];

		for (int i=0; i<v1.length; i++)
			mv[i] = 0.5 * (v1[i] + v2[i]);

		return mv;
		}

	public Object normalizeVector(Object vector) {
		double[] v = (double[])vector;
		for (int i=0; i<v.length; i++)
			v[i] = (v[i] - mMeanParameter[i]) / mStandardDeviation[i];
		return v;
		}
	}