package com.actelion.research.calc.principalcomponentanalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.actelion.research.calc.MatrixFunctions;
import com.actelion.research.calc.Matrix;
import com.actelion.research.calc.SingularValueDecomposition;


/**
 * 
 * 
 * PCA
 * MvK: I compared the values with the SVD output from R and it was ok. At the
 * moment it is not clear whether the data should be centered and the variances
 * should be normalised before giving the data into the SVD. </p>
 * <p>Copyright: Actelion Ltd., Inc. All Rights Reserved
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.</p>
 * @author Modest von Korff
 * @version 1.0
 * Jan 26, 2012 MvK: Start implementation
 */
public class PCA {

    private static final int DIGITS_OUT_SYSTEM = 4;

    private Matrix maCovariance;
    private Matrix maMean;
    private Matrix maStandardDeviation;

    private Matrix maEigenVectorsLeft;
    private Matrix maEigenVectorsReight;

    private Matrix maFactors;

    private Matrix maSingularValues;
    private Matrix maEigenValues;

    private Matrix maExplainedVariance;
    private Matrix maVariableContributionToEigenvectors;

    private Matrix maCommunality;
    private Matrix B;

    private Matrix maVariableCorrelations;

    public PCA() {

    }
    
    public PCA(Matrix X) {

        // The SVD results with X * Xt gives better results with the Benzo5Musc5_PFp2D.sdf
        // data set than the Eigenvalue decomposition

        // Matrix Xq = Xnorm.transpose().times(Xnorm).divide(Xnorm.getRowDimension() - 1);

        Matrix Xm = X.getCenteredMatrix();
        maMean = X.getMeanCols();
        maStandardDeviation = Xm.getStandardDeviationCols();

        // System.out.println("Xq Centered");
        // System.out.println(Xm.toString());

        Matrix Xnorm = Xm.getNormalizedMatrix();

        // System.out.println("Xnorm");
        // System.out.println(Xnorm.toString());

        // Correlation matrix
        Matrix Xq = Xnorm.multiply(true, false, Xnorm);
        Xq = Xq.devide(Xnorm.getRowDim() - 1.0);

        // System.out.println("Squared normalized X matrix:");
        // System.out.println(Xq.toString(DIGITS_OUT_SYSTEM));

        SingularValueDecomposition svd = new SingularValueDecomposition(Xq.getArray(), null, null);

        maEigenVectorsLeft = new Matrix(svd.getU());
        maEigenVectorsReight = new Matrix(svd.getV());

       

        // Principal components
        // Factors = Xnorm.multiply(EigenVectorsLeft);



        maSingularValues = new Matrix(true, svd.getSingularValues());
        maSingularValues = maSingularValues.diagonalize();

        maEigenValues = maSingularValues.getSQRT();


        Matrix OnePerEigenValues = maSingularValues.pow(-0.5);
        OnePerEigenValues = OnePerEigenValues.getDiagonal();
        OnePerEigenValues = OnePerEigenValues.diagonalize();
        B = maEigenVectorsLeft.multiply(OnePerEigenValues);
        maFactors = Xnorm.multiply(B);

        // Expl Var
        maExplainedVariance = calcExplainedVariance(maSingularValues);


        // Percentage of contributions of the X variables to each eigenvector
        maVariableContributionToEigenvectors = getContributions();

        // Variable correlationconfiguration
        // according Henrion, Henrion, pp. 24
        Matrix SingularValuesSquared = maSingularValues.getSQRT();
        maVariableCorrelations = maEigenVectorsLeft.multiply(SingularValuesSquared);

        communality();

        // System.out.println(toString());
    }

    /**
     * The matrix is centered but not normalized. 
     * @param X
     * @param numPCs
     */
    public PCA(Matrix X, int numPCs) {

        // The SVD results with X * Xt gives better results with the Benzo5Musc5_PFp2D.sdf
        // data set than the Eigenvalue decomposition

        // Matrix Xq = Xnorm.transpose().times(Xnorm).divide(Xnorm.getRowDimension() - 1);

        Matrix XCentered = X.getCenteredMatrix();
        maMean = X.getMeanCols();
        maStandardDeviation = XCentered.getStandardDeviationCols();

        // System.out.println("Xq Centered");
        // System.out.println(Xm.toString());

        // MatrixK Xnorm = Xm.getNormalizedMatrix();

        // System.out.println("Xnorm");
        // System.out.println(Xnorm.toString());

        // Correlation matrix
        Matrix Xq = XCentered.multiply(true, false, XCentered);
        Xq = Xq.devide(XCentered.getRowDim() - 1.0);

        // System.out.println("Squared normalized X matrix:");
        // System.out.println(Xq.toString(DIGITS_OUT_SYSTEM));

        // SingularValueDecompositionMatrixK svd = new SingularValueDecompositionMatrixK(Xq);
        SingularValueDecomposition svd = new SingularValueDecomposition(Xq.getArray(), null, null);

        maEigenVectorsLeft = new Matrix(svd.getU());
        maEigenVectorsReight = new Matrix(svd.getV());

        // Principal components
        // Factors = Xnorm.multiply(EigenVectorsLeft);

        maSingularValues = new Matrix(true, svd.getSingularValues());
        for (int ii = numPCs; ii < maSingularValues.getColDim(); ii++) {
          maSingularValues.set(0,ii,0.0);
        }
        maSingularValues = maSingularValues.diagonalize();

        maEigenValues = maSingularValues.getSQRT();

        // According NIST
        // http://www.itl.nist.gov/div898/handbook/pmc/section5/pmc552.htm
        Matrix maReciprocalEigenValues = maSingularValues.pow(-0.5);
        maReciprocalEigenValues = maReciprocalEigenValues.getDiagonal().getTranspose();
        for (int ii = 0; ii < maReciprocalEigenValues.getColDim(); ii++) {
          if(Double.isInfinite(maReciprocalEigenValues.get(0,ii)))
              maReciprocalEigenValues.set(0,ii,0.0);
        }
        maReciprocalEigenValues = maReciprocalEigenValues.diagonalize();
        B = maEigenVectorsLeft.multiply(maReciprocalEigenValues);
        maFactors = XCentered.multiply(B);

        // System.out.println("EigenVectorsLeft\n" + EigenVectorsLeft);
        // System.out.println("EigenValues\n" + EigenValues);
        // System.out.println("ReciprocalEigenValues\n" + ReciprocalEigenValues);
        // System.out.println("B\n" + B);


        // Calculation of the explained Variance in the Singular values
        maExplainedVariance = calcExplainedVariance(maSingularValues);

        // Percentage of contributions of the X variables to each eigenvector
        maVariableContributionToEigenvectors = getContributions();

        // Variable correlation configuration
        // according Henrion, Henrion, pp. 24
        Matrix maSingularValuesSquared = maSingularValues.getSQRT();
        maVariableCorrelations = maEigenVectorsLeft.multiply(maSingularValuesSquared);

        communality();

        // System.out.println(toString());
    }

    /**
     * Communality
     * 18.07.2003 MvK
     * Calculates the communality according the NIST example. The communality is
     * the percentage of variance of one variable that is explained with the
     * principal component under consideration. The result is written into the
     * matrix Communality. A row correponds to one principle component. The columns
     * corresponds to the variables. Variable 0,1 is than the percentile of the
     * explained variance in the first principal component for the second
     * variable.
     */
    public void communality() {

        Matrix FactorStructure = maEigenVectorsLeft.multiply(maEigenValues);
        List<Integer> vecIndices = new ArrayList<Integer>();
        vecIndices.add(new Integer(0));
        Matrix maFactorStructureReduced = FactorStructure.getColumns(vecIndices);
        Matrix maCommunalityRow = maFactorStructureReduced.multiply(false, true, maFactorStructureReduced);
        maCommunalityRow = maCommunalityRow.getDiagonal().getTranspose();

        maCommunality = new Matrix(FactorStructure.getColDim(), maCommunalityRow.getColDim());
        maCommunality.assignRow(0, maCommunalityRow.getRow(0));


        for (int ii = 1; ii < FactorStructure.getColDim(); ii++) {
            vecIndices = new ArrayList<Integer>();
            vecIndices.add(new Integer(ii));
            // vecIndices.add(new Integer(1));
            maFactorStructureReduced = FactorStructure.getColumns(vecIndices);

            maCommunalityRow = maFactorStructureReduced.multiply(false, true, maFactorStructureReduced);
            maCommunalityRow = maCommunalityRow.getDiagonal().getTranspose();
            for (int jj = 0; jj < maCommunalityRow.getColDim(); jj++) {
                double val = maCommunalityRow.get(0,jj) + maCommunality.get(ii-1,jj);
                maCommunalityRow.set(0,jj, val);
            }
            maCommunality.assignRow(ii, maCommunalityRow.getRow(0));
        }

        // System.out.println("Communality\n" + Communality);

        // A row correponds to one singular value
        // A column corresponds to the variables
        // Variable 0,1 is than the percentile of the explained variance in the
        // first principal component for the second variable.
        // System.out.println("maCommunality");
        // System.out.println(MatrixFunctions.toString(Communality));
        // The sum for each row should be 1
        // System.out.println("Sum");
        // System.out.println(MatrixFunctions.toString(Communality.sum()));

    }
    /**
     * The coefficient matrix, B, is formed using the reciprocals of the
     * diagonals of L1/2 (L1/2 Eigenvalues)
     * @return
     */
    public Matrix getB() {
        return B;
    }

    public Matrix getEigenValues() {
        return maEigenValues;
    }

    public Matrix getEigenVectorsLeft() {
        return maEigenVectorsLeft;
    }

    public static Matrix calcExplainedVariance(Matrix SingularValues) {
        // Calculation of the explained Variance in the Singular values
        double dSumSValues = SingularValues.getSum();

        Matrix ExplainedVariance = new Matrix(SingularValues.getColDim(), 2);
        double dSum = 0;
        for (int ii = 0; ii < SingularValues.getColDim(); ii++) {
            double val = (SingularValues.get(ii, ii) / dSumSValues) * 100;
            ExplainedVariance.set(ii, 0, val);
            dSum += val;
            ExplainedVariance.set(ii, 1, dSum);
        }

        return ExplainedVariance;
    }

    public Matrix getSingularValues() {
        return maSingularValues;
    }

    public Matrix getFactors() {
        return maFactors;
    }

    public Matrix getMean() {
        return maMean;
    }

    public Matrix getStandardDeviation() {
        return maStandardDeviation;
    }

    public Matrix getVariableCorrelations() {

        return maVariableCorrelations;
    }

    /**
     * Returns the number of values needed to explain percent of variance.
     * @param percent percentage
     * @return min number eigenvalues
     */
    public int getNumEigenValExplVariance(double percent) {

        int num = 0;
        for (int ii = 0; ii < maExplainedVariance.getRowDim(); ii++) {
            if(maExplainedVariance.get(ii,1) >= percent) {
                num = ii;
                break;
            }
        }
        return num;
    }
    
	public Matrix getCommunality() {
		return maCommunality;
	}


    /**
     * Percentage of contributions of the X variables to each eigenvector.
     * The sum of all  Eigen vector (column) results in 100 percent.
     * @return matrix with percentage of contribution
     */
    protected Matrix getContributions() {

        // Percentage of contributions of the X variables to each eigenvector
        // The sum of all  Eigen vector (column) results in 100 percent.
        // Matrix maContribution = EigenVectorsLeft.times(SingularValues.ebeSqrt()).abs();

        Matrix maContribution = maEigenVectorsLeft.multiply(maSingularValues).
            getAbs();

        double dSum = maContribution.getSum();
        for (int ii = 0; ii < maContribution.getRowDim(); ii++) {
            for (int jj = 0; jj < maContribution.getColDim(); jj++) {
                double dVal = 0;
                dVal = maContribution.get(ii, jj) / dSum * 100;
                maContribution.set(ii, jj, dVal);
            }
        }

        return maContribution;
    }
    
	public Matrix getExplainedVariance() {
		return maExplainedVariance;
	}


    public String toString() {
        String str = "";

        str += "Start PCA output.\n\n";

        str += "Explained variance in singular values:\n";
        str += maExplainedVariance.toStringRowNumber(DIGITS_OUT_SYSTEM, "\t") + "\n\n";

        str += "Communality:\n";
        str += "A row correponds to one singular value.\n";
        str += "A column corresponds to the variables.\n";
        str += "Variable 0,1 is than the percentile of the explained variance in the first principal component for the second variable.\n";
        str += maCommunality.toString(DIGITS_OUT_SYSTEM) + "\n\n";
        // str += "The sum for each row should be 1.\nSum:\n";
        // str += Communality.getSumCols().toString() + "\n\n";

        str +=
            "With standard deviation weighted variable contribution to Eigen vectors.\n";
        str += "Each column represents a Eigenvector multiplied with the corresponding singular value.\n";
        str += "Each row represents a variable.\n";
        str += maVariableContributionToEigenvectors.toString(DIGITS_OUT_SYSTEM) + "\n\n";

        str += "Eigen vectors left:\n";
        str += maEigenVectorsLeft.toString() + "\n\n";

        str += "Singular values:\n";
        str += maSingularValues.toString() + "\n\n";

        str += "Eigen vectors right:\n";
        str += maEigenVectorsReight.toString() + "\n\n";

        str += "Mean Vector:\n";
        str += maMean.toString() + "\n\n";

        str += "Standard Deviation Vector:\n";
        str += maStandardDeviation.toString() + "\n\n";

        str += "End PCA output.\n\n";

        return str;
    }

    public static void main(String[] arg) {

        // runTestNIST01();
        // System.exit(0);
        int rows = 30000;
        int cols = 512;
        int k = 1000;
        Matrix X = MatrixFunctions.getRandomMatrix(rows, cols);
        Matrix maCenters = MatrixFunctions.getKMeanClusters(X, k);
        System.exit(0);
        // System.out.println("input matrix X:");
        // System.out.println(X.toString());

        PCA pca = new PCA(X);
        System.out.println("PCA finished");

        // System.out.println("input matrix X:");
        // System.out.println(X.toString());
        // System.out.println(pca.toString());

        Matrix U = pca.maEigenVectorsLeft;
        Matrix S = pca.maSingularValues;
        Matrix V = pca.maEigenVectorsReight;

        // Generate the quadratic matrix from the U, S and V
        X = U.multiply(S).multiply(false, true, V);
        System.out.println("X = U S V' ");
        // System.out.println(X.toString(DIGITS_OUT_SYSTEM));

        System.out.println("End");

    }


    public static void runTestNIST01() {

        // http://www.hume.com/la/NIST.html
        Matrix X = testNIST01();

        int iRows = 10;
        int iCols = 20;

        int nPCs = 1;

        // X = RandomMatrix.beta(iRows,iCols,0.5,0.5);

        System.out.println("input matrix X\n");
        System.out.println(X.toString(DIGITS_OUT_SYSTEM));

        // pca(X);
        // X = X.mergeColumns(X);
        PCA pca = new PCA(X,nPCs);
        // pca.communality();

        System.out.println("input matrix X:");
        System.out.println(X.toString());
        System.out.println(pca.toString());

        Matrix U = pca.maEigenVectorsLeft;
        Matrix S = pca.maSingularValues;
        Matrix V = pca.maEigenVectorsReight;

        // Generate the quadratic matrix from the U, S and V
        Matrix Xq = U.multiply(S).multiply(false, true, V);
        System.out.println("Xq = U S V' ");
        System.out.println(Xq.toString(DIGITS_OUT_SYSTEM));

        Matrix PCs = X.getCenteredMatrix();
        PCs = PCs.getNormalizedMatrix();
        PCs = PCs.multiply(U);
        System.out.println("PCs\n" + PCs.toString(DIGITS_OUT_SYSTEM));
        System.out.println("Factors\n" + pca.getFactors());



        // Generation of the original values with the Eigenvectors
        Matrix r1 = new Matrix(true, PCs.getRow(0));
        for (int ii = nPCs; ii < r1.getColDim(); ii++) {
            r1.set(0,ii,0);
        }


        Matrix r1Pred = r1.multiply(false, true, U);
        r1Pred = r1Pred.multCols(pca.getStandardDeviation());
        r1Pred = r1Pred.add2CompleteCol(pca.getMean());


        System.out.println("r1\n" + r1.toString(3));
        System.out.println("r1Pred\n" + r1Pred.toString(3));


        // Generate the quadratic matrix from the U, S and V
        // Matrix Xpred = U.multiply(S).multiply(false, true, V);

        // System.exit(0);

    }

    public static Matrix testNIST01() {

        double[][] dArr = {
            {
            7, 4, 3}
            , {
            4, 1, 8}
            , {
            6, 3, 5}
            , {
            8, 6, 1}
            , {
            8, 5, 7}
            , {
            7, 2, 9}
            , {
            5, 3, 3}
            , {
            9, 5, 8}
            , {
            7, 4, 5}
            , {
            8, 2, 2}
        };

        Matrix ma = new Matrix(dArr);

        // System.out.println(MatrixFunctions.toString(ma));

        return ma;
    }

    public static Matrix testBinary01() {

        int iRows = 3;
        int iCols = 60;

        double dThreshold = 0.5;

        long lSeed = 1122334455;
        Random rnd = new Random(lSeed);

        double[][] dArr = new double[iRows][iCols];
        for (int ii = 0; ii < dArr.length; ii++) {
            for (int jj = 0; jj < dArr[ii].length; jj++) {
                double dVal = rnd.nextDouble();
                int iValBool = 0;
                if (dVal > dThreshold) {
                    iValBool = 1;
                }
                dArr[ii][jj] = iValBool;
            }
        }

        Vector vecIndices = new Vector();
        for (int ii = 0; ii < dArr.length; ii++) {
            Vector vecIndicesRow = new Vector();

            for (int jj = 0; jj < dArr[ii].length; jj++) {
                if (dArr[ii][jj] == 1) {
                    vecIndicesRow.add(new Integer(jj));
                }
            }
            vecIndices.add(vecIndicesRow);
        }

        Matrix ma = new Matrix(dArr);

        for (int ii = 0; ii < vecIndices.size(); ii++) {
            System.out.println(vecIndices.get(ii));
        }

        return ma;
    }

}