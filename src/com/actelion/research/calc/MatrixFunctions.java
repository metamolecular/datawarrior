package com.actelion.research.calc;


import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.actelion.research.util.datamodel.ScorePoint;

/**
 * <p>Title: MatrixFunctions</p>
 * <p>Description: Matrix operations for which a direct access to the matrix
 * member variables is not necessary</p>
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * 20.11.2003 MvK: Start implementation
 * 10.06.2004 MvK read matrix.
 */

public class MatrixFunctions {

    
    public static int countFieldsBiggerThan(Matrix ma, int row, double thresh) {
    	int cc = 0;
    	for (int i = 0; i < ma.getColDim(); i++) {
			if(ma.get(row,i) > thresh) {
				cc++;
			}
		}
    	return cc;
    }
    
    public static int countFieldsBiggerThanThreshColWise(Matrix ma, int col, double thresh) {
    	int cc = 0;
    	for (int i = 0; i < ma.rows(); i++) {
			if(ma.get(i,col) > thresh) {
				cc++;
			}
		}
    	return cc;
    }
    
    public static Matrix countFieldsBiggerThanThreshColWise(Matrix ma, double thresh) {
    	Matrix maCounts = new Matrix(1, ma.cols());
    	
    	for (int i = 0; i < ma.cols(); i++) {
			int cc = countFieldsBiggerThanThreshColWise(ma, i, thresh);
			
			maCounts.set(0, i, cc);
		}
    	
    	return maCounts;
    }
    
    /**
     * Calculates the inverse Tanimoto coefficient from row wise comparison of the two input matrices.
     * 
     * @param ma1
     * @param ma2
     * @return complete distance matrix calculated between all rows from  the two input matrices.
     */
    public static Matrix getDistanceMatrixTanimotoInv(Matrix ma1, Matrix ma2) {
        Matrix maDist = new Matrix(ma1.getRowDim(), ma2.getRowDim());
        for (int i = 0; i < ma1.getRowDim(); i++) {
            for (int j = 0; j < ma2.getRowDim(); j++) {
                double dist = getDistanceTanimotoInv(ma1, i, ma2, j);
                maDist.set(i,j, dist);
            }
        }
        return maDist;
    }
    
    public static Matrix getDistanceMatrix(List<Point> li) {
        Matrix maDist = new Matrix(li.size(), li.size());
        for (int i = 0; i <li.size(); i++) {
            for (int j = 0; j < li.size(); j++) {
                double dist = li.get(i).distance(li.get(j));
                maDist.set(i,j, dist);
            }
        }
        return maDist;
    }

    public static Matrix getDistTanimotoInvReduced(Matrix ma1, Matrix ma2) {
        Matrix maDist = new Matrix(ma1.getRowDim(), ma2.getRowDim());
        for (int i = 0; i < ma1.getRowDim(); i++) {
            for (int j = 0; j < ma2.getRowDim(); j++) {
                double dist = getDistTanimotoInvReduced(ma1, i, ma2, j);
                maDist.set(i,j, dist);
            }
        }
        return maDist;
    }
    /**
     *
     * @param ma1 Matrix
     * @param row1 row
     * @param ma2 Matrix
     * @param row2 row
     * @return maximum distance = 0, minimum distance = 1
     */
    public static double getDistanceTanimotoInv(Matrix ma1, int row1, Matrix ma2, int row2) {
        double dist = 0;
        double dAtB = ma1.multiply(row1, ma2, row2);
        double dAtA = ma1.multiply(row1, ma1, row1);
        double dBtB = ma2.multiply(row2, ma2, row2);
        dist = dAtB / (dAtA + dBtB - dAtB);
        return 1-dist;
    }
    /**
     * Only fields are considered which are not nut 0 in both or in one of the
     * rows we from where the distance is calculated.
     * @param ma1 Matrix
     * @param row1 row
     * @param ma2 Matrix
     * @param row2 row
     * @return maximum distance = 0, minimum distance = 1
     */
    public static double getDistTanimotoInvReduced(Matrix ma1, int row1, Matrix ma2, int row2) {
        double dist = 0;

        List<Double> li1 = new ArrayList<Double>();
        List<Double> li2 = new ArrayList<Double>();
        for (int ii = 0; ii < ma1.getColDim(); ii++) {
            if((ma1.get(row1, ii) != 0) || (ma2.get(row2, ii) != 0)) {
                li1.add(new Double(ma1.get(row1, ii)));
                li2.add(new Double(ma2.get(row2, ii)));
            }
        }

        Matrix maRow1 = new Matrix(true, li1);
        Matrix maRow2 = new Matrix(true, li2);

        double dAtB = maRow1.multiply(0, maRow2, 0);
        double dAtA = maRow1.multiply(0, maRow1, 0);
        double dBtB = maRow2.multiply(0, maRow2, 0);
        dist = dAtB / (dAtA + dBtB - dAtB);
        return 1-dist;
    }

    public static Matrix getHistogram(Matrix maHistogramBins) {
        Matrix maHistogram = new Matrix(maHistogramBins);
        maHistogram.resize(3, maHistogramBins.getColDim());
        return maHistogram;
    }
    
    /**
     * Gets the upper and lower limit of the most occupied bin.
     * @param maHistogram
     * @param radius so many bins are taken from the left and the right.
     * @return arr[0]: lower limit, arr[1]: upper limit 
     */
    public static double [] getBordersMostFreqOccBin(Matrix maHistogram, int radius) {
    	double [] arr = new double[2];
    	
    	double maxFreq = 0;
    	int index=-1;
    	for (int i = radius; i < maHistogram.cols() - radius; i++) {
    		
    		int sumFreq = 0;
    		for (int j = -radius; j < radius+1; j++) {
    			sumFreq += maHistogram.get(2, i+j);
			}
    		
			if(sumFreq>maxFreq){
				maxFreq=sumFreq;
				index=i;
			}
		}
    	
    	int indexLower = Math.max(0, index-radius);
    	
    	int indexUpper = Math.min(maHistogram.cols()-1, index+radius);
    	
    	arr[0]=maHistogram.get(0, indexLower);
    	
    	arr[1]=maHistogram.get(1, indexUpper);
    	
        return arr;
    }

    
    
    /**
    *
    * @param ma matrix values are written into the histogram
        * @param maHistogramBins matrix with two rows row 1: lower bins; row 2: upper
    * bins.
    * @return matrix with 3 rows. Row 0 lower bins, row 1: upper bins, row 2
    * frequency.
    */
    public static Matrix getHistogram(Matrix ma, Matrix maHistogramBins) {
    	
        Matrix maHistogram = new Matrix(maHistogramBins);

        maHistogram.resize(3, maHistogramBins.getColDim());

        for (int i = 0; i < ma.getRowDim(); i++) {
            for (int j = 0; j < ma.getColDim(); j++) {
                double v = ma.get(i, j);
                placeValueInHistogramBin(v, maHistogram);
            }
        }

        return maHistogram;
    }
    
    public static Matrix getHistogram(double [] arrValues, Matrix maHistogramBins) {
        Matrix maHistogram = new Matrix(maHistogramBins);

        maHistogram.resize(3, maHistogramBins.getColDim());

        for (int i = 0; i < arrValues.length; i++) {
        	placeValueInHistogramBin(arrValues[i], maHistogram);
		}

        return maHistogram;
    }
    
    private static void placeValueInHistogramBin(double v, Matrix maHist){
    	
    	if(Double.isNaN(v)) {
    		System.err.println("Warning " + v + " in placeValueInHistogramBin(...).");
    		return;
    	}
    	
    	if (v < maHist.get(0, 0) || v > maHist.get(1, maHist.cols()-1))
    		return;
    	
    	int maxloops = maHist.cols();
    		
    	boolean bEnd = false;
    	
    	int pos = maHist.cols() / 2;
    	
    	int posLow=0;
    	
    	int posUp = maHist.cols()-1;
    	
    	int cc=0;
    	while(!bEnd){
    		
    		if (v >= maHist.get(0, pos) && v < maHist.get(1, pos)) {
                maHist.increase(2, pos, 1);
                bEnd=true;
            } else if (v < maHist.get(0, pos)) {
            	posUp = pos;
            	pos = posLow + (pos-posLow)/2;
            } else if (v >= maHist.get(1, pos)) {
            	posLow = pos;
            	pos = pos + (int)(((double)posUp-pos)/2+0.5);
            }
    		
    		if(cc==maxloops){
    			throw new RuntimeException("Fitting bin for value " + v + " not found");
    		}
    		cc++;
    		
    	}
    	
    }
    
    
    public static Matrix getHistogram(float [] ma, Matrix maHistogramBins) {
        Matrix maHistogram = new Matrix(maHistogramBins);
        maHistogram.resize(3, maHistogramBins.getColDim());
        for (int ii = 0; ii < ma.length; ii++) {
            for (int kk = 0; kk < maHistogramBins.getColDim(); kk++) {
                if (ma[ii] >= maHistogram.get(0, kk) &&
                	ma[ii] < maHistogram.get(1, kk)) {
                    int iCounter = (int) maHistogram.get(2, kk);
                    iCounter++;
                    maHistogram.set(2, kk, iCounter);
                }
            }
        }

        return maHistogram;
    }

    public static Matrix getHistogram(float [][] ma, int col, Matrix maHistogramBins) {
        Matrix maHistogram = new Matrix(maHistogramBins);
        maHistogram.resize(3, maHistogramBins.getColDim());
        for (int ii = 0; ii < ma.length; ii++) {
            for (int kk = 0; kk < maHistogramBins.getColDim(); kk++) {
                if (ma[ii][col] >= maHistogram.get(0, kk) &&
                	ma[ii][col] < maHistogram.get(1, kk)) {
                    int iCounter = (int) maHistogram.get(2, kk);
                    iCounter++;
                    maHistogram.set(2, kk, iCounter);
                }
            }
        }

        return maHistogram;
    }
   
    public static Matrix getHistogram(Matrix ma, int numBins) {
    	double min = ma.getMin();
    	double max = ma.getMax();
    	
    	Matrix maBins = getHistogramBins(min,max, numBins);
    	
    	Matrix maHist = getHistogram(ma, maBins);
    	
    	return maHist;
    }
    /**
     * 
     * @param maHist histogram
     * @return the lower limit of the first occupied bin in the histogram.
     */
    public static double getMinOccBin(Matrix maHist) {
    	double min = 0;
    	for (int i = 0; i < maHist.getColDim(); i++) {
			if(maHist.get(2,i) > 0) {
				min = maHist.get(0,i);
				break;
			}
		}
    	return min;
    }

    /**
     * A square-shaped neighborhood that can be used to define a set of cells surrounding a given point.
     * 
     * @param p
     * @param ma
     * @return
     */
    public static List<Point> getMooreNeighborhood(Point p, Matrix ma) {
    	List<Point> li = new ArrayList<Point>();
    	
    	int startX = Math.max(0, p.x - 1);
    	int endX = Math.min(ma.cols(), p.x+2);
    	
    	int startY = Math.max(0, p.y - 1);
    	int endY = Math.min(ma.rows(), p.y+2);
    	
    	for (int i = startY; i < endY; i++) {
			for (int j = startX; j < endX; j++) {
				if(i!=p.y || j!=p.x) {
					if(ma.get(i,j)>0) {
						li.add(new Point(j,i));
					}
				}
			}
		}
    	return li;
    }
    
    public static List<Point> getMooreNeighborhood(Point p, int r, Matrix ma) {
    	List<Point> li = new ArrayList<Point>();
    	
    	int startX = Math.max(0, p.x - r);
    	int endX = Math.min(ma.cols(), p.x+r+1);
    	
    	int startY = Math.max(0, p.y - r);
    	int endY = Math.min(ma.rows(), p.y+r+1);
    	
    	for (int i = startY; i < endY; i++) {
			for (int j = startX; j < endX; j++) {
				if(i!=p.y || j!=p.x) {
					if(ma.get(i,j)>0) {
						li.add(new Point(j,i));
					}
				}
			}
		}
    	return li;
    }
    
    /**
     * 
     * @param maHist histogram
     * @return the higher limit of the last occupied bin in the histogram.
     */
    public static double getMaxOccBin(Matrix maHist) {
    	double max = 0;
    	for (int i = maHist.getColDim() - 1; i >= 0; i--) {
			if(maHist.get(2,i) > 0) {
				max = maHist.get(1,i);
				break;
			}
		}
    	return max;
    }
    

    /**
	 * 
	 * @param ma
	 *            Matrix with ma.rows and 1 col. Containing the min value from
	 *            each row.
	 * @return
	 */
    public static Matrix getRowMinUnique(Matrix ma) {
    	
    	Matrix maTmp = new Matrix(ma);
    	
    	Matrix maMin = new Matrix(maTmp.getRowDim(), 1);
    	
    	for (int i = 0; i < maTmp.getRowDim(); i++) {
        	ScorePoint td = maTmp.getMinPos();
        	maMin.set(td.y,0,td.getScore());
        	maTmp.setRow(td.y, Double.MAX_VALUE);
        	maTmp.setCol(td.x, Double.MAX_VALUE);
		}
    	return maMin;
    }
    
    /**
     * 
     * @param ma
     * @return list with indices Point(row,col) for values>0.
     */
	public static List<Point> getPoints(Matrix ma){
		List<Point> li = new ArrayList<Point>();
		
		for (int i = 0; i < ma.rows(); i++) {
			for (int j = 0; j < ma.cols(); j++) {
				if(ma.get(i, j)>0){
					li.add(new Point(j,i));
				}
			}
		}
		
		return li;
	}
	
	public static List<Point> getIndicesUniqueMaxRowWise(Matrix maIn){
		List<Point> li = new ArrayList<Point>();
		
		Matrix ma = new Matrix(maIn);
		
		for (int i = 0; i < ma.rows(); i++) {
			
			Point p = ma.getMaxIndex();
			
			li.add(p);
			
			int row = p.y;
			
			for (int j = 0; j < ma.cols(); j++) {
				ma.set(row, j, -Double.MAX_VALUE);
			}
		}
		
		return li;
	}
	
	public static List<Point> getIndicesUniqueMaxColumnWise(Matrix maIn){
		List<Point> li = new ArrayList<Point>();
		
		Matrix ma = new Matrix(maIn);
		
		for (int col = 0; col < ma.cols(); col++) {
			
			Point p = ma.getMaxIndex();
			
			li.add(p);
			
			int colMax = p.x;
			
			for (int row = 0; row < ma.rows(); row++) {
				ma.set(row, colMax, -Double.MAX_VALUE);
			}
		}
		
		return li;
	}

    public static Matrix getScaled(Matrix ma, double fac) {
    	int cols = (int)(ma.cols()*fac+0.5);
    	int rows = (int)(ma.rows()*fac+0.5);
    	
    	Matrix maSc = new Matrix(rows, cols);
    	
    	for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				double v = ma.get((int)(i/fac), (int)(j/fac));
				maSc.set(i,j,v);
			}
		}
    	
    	return maSc;
    	
    }
    
    
    /**
     *
     * @param dMin smallest value to put into the histogram
     * @param dMax maximum value to be considered.
     * @param iNumBins number of bins, between min and max.
     * @return matrix with two rows, the lower and upper bins.
     * The last bin is a little bit bigger than the other bins. So the highest
     * value fits into it.
     */
    public static Matrix getHistogramBins(double dMin, double dMax, int iNumBins) {
        Matrix maHistogramBins = new Matrix(2, iNumBins);

        double dDelta = dMax - dMin;
        double dBinWidth = dDelta / iNumBins;

        double dIncrementLast = dBinWidth * 0.0001;

        double dLow = dMin;

        int iCols = maHistogramBins.getColDim();
        for (int ii = 0; ii < iCols; ii++) {
            maHistogramBins.set(0, ii, dLow);
            double dUp = dLow + dBinWidth;

            if (ii == (iCols - 1))
                dUp += dIncrementLast;

            maHistogramBins.set(1, ii, dUp);
            dLow = dUp;
        }

        return maHistogramBins;
    }

    /**
     * 03.10.04 MvK
     * @param ma matrix with objects in rows
     * @param k number of desired cluster
     * @return Matrix of cluster centers, k rows and cols equal ma.cols.
     */
    public static Matrix getKMeanClusters(Matrix ma, int k) {

        Matrix maCenters = new Matrix(k,ma.getColDim());

        int iMaxIterations = 100;
        // Array with indices
        // The index specified the corresponding mean cluster
        int[] arrIndex = new int[ma.getRowDim()];

        // Generate the first mean clusters by random
        Random rnd = new Random();
        for (int ii = 0; ii < k; ii++) {
            int rndIndex = rnd.nextInt(ma.getRowDim());
            maCenters.assignRow(ii, ma.getRow(rndIndex));
        }
        // For test
        // maCenters.assignRow(0, ma.getRow(0));
        // maCenters.assignRow(1, ma.getRow(6));
        // maCenters.assignRow(2, ma.getRow(14));

        maCenters = maCenters.getSorted();
        Matrix maCenters2 = new Matrix(maCenters);
        int counter = 0;
        do{
            maCenters = new Matrix(maCenters2);

            for (int ii = 0; ii < arrIndex.length; ii++)
                arrIndex[ii] = -1;

            // Find the next mean cluster for each object in the matrix.
            // Each col represents a mean cluster, each row represents an
            // object.
            Matrix maDist = getDistTanimotoInvReduced(ma, maCenters);
            for (int ii = 0; ii < maDist.getRowDim(); ii++) {
                int index = maDist.getMinRowIndexRND(ii);
                arrIndex[ii] = index;
            }

            // System.out.println("maDist\n" + maDist);

            // Calculate the new mean clusters.
            double[] arrNumObjects = new double[k];
            maCenters2.set(0.0);
            for (int ii = 0; ii < ma.getRowDim(); ii++) {
                int index = arrIndex[ii];
                maCenters2.add2Row(index, ma, ii);
                arrNumObjects[index]++;
            }
            // boolean bEmptyCenter = false;
            for (int ii = 0; ii < maCenters2.getRowDim(); ii++) {
                if(arrNumObjects[ii] > 0)
                    maCenters2.devideRow(ii, arrNumObjects[ii]);
                else {
                    // maCenters2.setRow(ii, -1);
                    maCenters2.assignRow(ii, maCenters.getRow(ii));
                    // bEmptyCenter = true;
                }
            }
/*
            if(bEmptyCenter) {
                String str = "Break because of empty center.\n";
                System.err.print(str);
                break;
            }
*/
            maCenters2 = maCenters2.getSorted();

            // System.out.println("maCenters2\n" + maCenters2);
            counter++;
            if(counter > iMaxIterations) {
                System.err.print("Max num iterations reached.\n");
                // (new Exception()).printStackTrace();
                break;
            }
        } while(!maCenters.equal(maCenters2));

        System.out.println("Number iterations: " + counter);

        return maCenters;
    }
    
    public static final double getCorrPearson(Matrix A, Matrix B) {
        
        final double [] a = A.toArray();
        
        final double [] aCent = ArrayUtilsCalc.getCentered(a);
        
        final double [] aCentNorm = ArrayUtilsCalc.getNormalized(aCent);
        
        final double [] b = B.toArray();
        
        final double [] bCent = ArrayUtilsCalc.getCentered(b);
        
        final double [] bCentNorm = ArrayUtilsCalc.getNormalized(bCent);
        
        final double val = ArrayUtilsCalc.getCorrPearsonStandardized(aCentNorm,bCentNorm);
        
        return val;
    }
    
//  public static double getCorrPearson(MatrixK A, MatrixK B) {
//  double val = 0;
//  MatrixK Anorm = A.getLinedCol();
//  Anorm = Anorm.getCenteredMatrix();
//  Anorm = Anorm.getNormalizedMatrix();
//
//  MatrixK Bnorm = B.getLinedCol();
//  Bnorm = Bnorm.getCenteredMatrix();
//  Bnorm = Bnorm.getNormalizedMatrix();
//  val = MatrixFunctions.getCorrPearsonStandardized(Anorm,Bnorm);
//  return val;
//}

    public static double getCorrPearson(Matrix A, int col1, int col2) {
        double val = 0;
        Matrix Anorm = A.getCol(col1);
        Anorm = Anorm.getCenteredMatrix();
        Anorm = Anorm.getNormalizedMatrix();

        Matrix Bnorm = A.getCol(col2);
        Bnorm = Bnorm.getCenteredMatrix();
        Bnorm = Bnorm.getNormalizedMatrix();
        val = MatrixFunctions.getCorrPearsonStandardized(Anorm,Bnorm);
        return val;
    }


    private static double getCorrPearsonStandardized(Matrix A, Matrix B) {
        double val = 0;

        double covXY = getCovarianceCentered(A,B);
        double varA = A.getVarianceCentered();
        double varB = B.getVarianceCentered();
        val = covXY / (varA * varB);
        return val;
    }

    public static double getCovariance(Matrix A, Matrix B) {
        double covXY = 0;
        double Amean = A.getMean();
        double Bmean = B.getMean();
        double sum = 0;
        for (int ii = 0; ii < A.getRowDim(); ii++) {
            for (int jj = 0; jj < A.getColDim(); jj++) {
                sum += (A.get(ii,jj) - Amean) * (B.get(ii,jj) - Bmean);
            }
        }
        covXY = sum / (A.getNumElements() - 1);

        return covXY;
    }

    public static double getCovarianceCentered(Matrix A, Matrix B) {
    	
        double covXY = 0;
        
        double sum = 0;
        
    	final int cols = A.cols();
    	
    	final int rows = A.rows();
        
        for (int i = 0; i < rows; i++) {
        	
        	final double [] a = A.getRow(i);
        	
        	final double [] b = B.getRow(i);
        	
            for (int j = 0; j < cols; j++) {
            	
                sum += (a[j] * b[j]);
            }
        }
        
        covXY = sum / (A.getNumElements() - 1);

        return covXY;
    }
    /**
     * generates a matrix with double values between 0 (inclusive) and 1
     * (exclusive).
     * @param iRows rows
     * @param iCols columns
     * @return matrix
     */
    public static Matrix getRandomMatrix(int iRows, int iCols) {
        Matrix ma = new Matrix(iRows, iCols);

        Random rnd = new Random();

        for (int ii = 0; ii < ma.getRowDim(); ii++) {
            for (int jj = 0; jj < ma.getColDim(); jj++) {
                ma.set(ii,jj, rnd.nextDouble());
            }
        }

        return ma;
    }


    /**
     * Converts a vector of vectors into doubles, each vector results in a row in
     * the matrix. All vectors have to be of equal length or a runtime  exception
     * is thrown.
     * @param vecvec vector on vectors, has to be converted into doubles
     * @param ma resulting matrix
     */
    public static void vecvec2Matrix(Vector<Vector<Double>> vecvec, Matrix ma) {

      Iterator<Vector<Double>> it = vecvec.iterator();
      int iLenCol0 = ((Vector<Double>) it.next()).size();

      for( ; it.hasNext(); ) {
        int iLen = ((Vector<Double>) it.next()).size();
        if(iLen != iLenCol0) {
          throw new RuntimeException("All vectors must have the same length.");
        }
      }
      int iRows = vecvec.size();

      ma.resize(iRows, iLenCol0);

      it = vecvec.iterator();
      int iRow = 0;
      for( ; it.hasNext(); ) {
        Vector<Double> vec = new Vector<Double>(((Vector<Double>) it.next()));
        for(int ii = 0; ii < vec.size(); ii++) {
          ma.set(iRow, ii, ((Double) vec.get(ii)).doubleValue());
        }
        iRow++;
      }
    }

    public static void writeHistogram(String sFile, Matrix hist, boolean bApppend, int digits, int totalWidth) throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
            sFile), bApppend));
        DecimalFormat dfBins = Matrix.format(digits);
        String sVal = "";
        for (int ii = 0; ii < 2; ii++) {
            sVal = "";
            for (int jj = 0; jj < hist.getColDim(); jj++) {
                sVal += Matrix.format(hist.get(ii,jj), dfBins, totalWidth) + Matrix.OUT_SEPARATOR_COL;

            }
            sVal += Matrix.OUT_SEPARATOR_ROW;
            writer.write(sVal);
        }

        DecimalFormat dfFreq = new DecimalFormat();
        sVal = "";
        for (int jj = 0; jj < hist.getColDim(); jj++) {
            sVal += Matrix.format(hist.get(2, jj), dfFreq, totalWidth) + Matrix.OUT_SEPARATOR_COL;

        }
        writer.write(sVal + "\n");

        writer.flush();
        writer.close();

    }
    
    public static String histogram2String(Matrix hist, int digits, int totalWidth) {
    	
    	StringBuilder sb = new StringBuilder();
    	
        
        DecimalFormat dfBins = Matrix.format(digits);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < hist.getColDim(); j++) {
            	sb.append(Matrix.format(hist.get(i,j), dfBins, totalWidth) + Matrix.OUT_SEPARATOR_COL);

            }
            sb.append(Matrix.OUT_SEPARATOR_ROW);
        }

        DecimalFormat dfFreq = new DecimalFormat();
        for (int i = 0; i < hist.getColDim(); i++) {
        	sb.append(Matrix.format(hist.get(2, i), dfFreq, totalWidth) + Matrix.OUT_SEPARATOR_COL);

        }
        sb.append("\n");

        return sb.toString();
    }
    

}