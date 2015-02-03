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


public class CorrelationCalculator {
    public static final String[] TYPE_NAME = { "Bravais-Pearson",
                                               "Spearman" };
    public static final int TYPE_NONE = -1;
    public static final int TYPE_BRAVAIS_PEARSON = 0;
    public static final int TYPE_SPEARMAN = 1;

    /**
     * Calculates the correlation coefficient between two columns of data.
     * Use the TYPE_BRAVAIS_PEARSON for normal distributed data and the
     * more robust TYPE_SPEARMAN if the data is not normal distributed.
     * If type==TYPE_BRAVAIS_PEARSON and one of a row's values is Double.NaN,
     * then the row is skipped.
     * If less than two valid rows are found or if both columns have a
     * different number of values, than Double.NaN is returned.
     * @param column1
     * @param column2
     * @param correlationType
     * @return
     */
    public static double calculateCorrelation(INumericalDataColumn column1,
                                              INumericalDataColumn column2,
                                              int correlationType) {
        int valueCount = column1.getValueCount();
        if (valueCount != column2.getValueCount())
            return Double.NaN;

        double r = Double.NaN;

        if (correlationType == TYPE_BRAVAIS_PEARSON) {
            // http://de.wikibooks.org/wiki/Mathematik:_Statistik:_Korrelationsanalyse
            int realValueCount = 0;
            double xMean = 0;
            double yMean = 0;
            for (int i=0; i<valueCount; i++) {
                double x = column1.getValueAt(i);
                double y = column2.getValueAt(i);
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
                    xMean += x;
                    yMean += y;
                    realValueCount++;
                    }
                }

            if (realValueCount < 2)
                return Double.NaN;

            xMean /= realValueCount;
            yMean /= realValueCount;

            double sumdxdx = 0;
            double sumdxdy = 0;
            double sumdydy = 0;
            for (int i=0; i<valueCount; i++) {
                double x = column1.getValueAt(i);
                double y = column2.getValueAt(i);
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
                    double dx = x - xMean;
                    double dy = y - yMean;
                    sumdxdx += dx*dx;
                    sumdxdy += dx*dy;
                    sumdydy += dy*dy;
                    }
                }
            r = sumdxdy / Math.sqrt(sumdxdx * sumdydy);
            }
        else if (correlationType == TYPE_SPEARMAN) {
            if (valueCount < 2)
                return Double.NaN;

            double[] xValue = new double[valueCount];
            double[] yValue = new double[valueCount];
            for (int i=0; i<valueCount; i++) {
                xValue[i] = column1.getValueAt(i);
                yValue[i] = column2.getValueAt(i);
                }
            java.util.Arrays.sort(xValue);
            java.util.Arrays.sort(yValue);

            int realCount = valueCount;
            while (realCount > 0
            	&& (Double.isNaN(xValue[realCount-1]) || Double.isNaN(yValue[realCount-1])))
            	realCount--;

            double sumdxdx = 0;
            double sumdxdy = 0;
            double sumdydy = 0;
            double mean = ((double)realCount) / 2;
            for (int i=0; i<valueCount; i++) {
            	if (!Double.isNaN(column1.getValueAt(i)) && !Double.isNaN(column2.getValueAt(i))) {
	                double xPosition = getPosition(xValue, column1.getValueAt(i));
	                double yPosition = getPosition(yValue, column2.getValueAt(i));
	                double dx = xPosition - mean;
	                double dy = yPosition - mean;
	                sumdxdx += dx*dx;
	                sumdxdy += dx*dy;
	                sumdydy += dy*dy;
            		}
                }
            r = sumdxdy / Math.sqrt(sumdxdx * sumdydy);
            }

        return r;
        }

    /**
     * Calculates a half correlation matrix of all passed numerical columns
     * @param numericalColumn
     * @param type
     * @return half matrix with matrix.length=numericalColumn.length and matrix[i].length=i
     */
    public static double[][] calculateMatrix(final INumericalDataColumn[] numericalColumn, int type) {
        double[][] matrix = new double[numericalColumn.length][];
        for (int i=1; i<numericalColumn.length; i++) {
            matrix[i] = new double[i];
            for (int j=0; j<i; j++)
                matrix[i][j] = CorrelationCalculator.calculateCorrelation(numericalColumn[i], numericalColumn[j], type);
            }
        return matrix;
        }

    private static double getPosition(double[] array, double value) {
        int position = java.util.Arrays.binarySearch(array, value);
        int position1 = position;
        while (position1 > 0 && array[position1-1] == value)
            position1--;
        int position2 = position;
        while (position2 < array.length-1 && array[position2+1] == value)
            position2++;
        return ((double)(position1+position2))/2;
        }
    }
