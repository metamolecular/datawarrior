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

package com.actelion.research.datawarrior.action;

import java.awt.Frame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableModel;

public class DESOMkNNAnalyzer implements Runnable {
    private Frame               mParentFrame;
	private DataWarrior		mApplication;
	private DEFrame				mTargetFrame;
	private CompoundTableModel	mSourceTableModel;
    private int                 mDescriptorColumn,mSomXColumn,mSomYColumn;
	private JProgressDialog		mProgressDialog;

    public DESOMkNNAnalyzer(DEFrame owner, DataWarrior application) {
		mParentFrame = owner;
		mApplication = application;
		mSourceTableModel = owner.getTableModel();
	    }

    public void analyze() {
        mSomXColumn = mSourceTableModel.findColumn("SOM_X");
        mSomYColumn = mSourceTableModel.findColumn("SOM_Y");

        if (mSomXColumn == -1 || mSomYColumn == -1) {
            JOptionPane.showMessageDialog(mParentFrame, "SOM_X and/or SOM_Y columns not found.");
            return;
            }

        selectDescriptor();

        if (mDescriptorColumn != -1) {
			mProgressDialog = new JProgressDialog(mParentFrame);

			Thread t = new Thread(this, "DESimilarityMatrixDialog");
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
			}
		}

    private void selectDescriptor() {
        mDescriptorColumn = -1;

        int[] descriptorColumn = new int[mSourceTableModel.getTotalColumnCount()];
        int descriptorCount = 0;
        for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
            if (mSourceTableModel.isDescriptorColumn(column))
                descriptorColumn[descriptorCount++] = column;

        if (descriptorCount == 0) {
            JOptionPane.showMessageDialog(mParentFrame, "No descriptor column available.");
            return;
            }

        if (descriptorCount == 1) {
            mDescriptorColumn = descriptorColumn[0];
            return;
            }

        String[] descriptorName = new String[descriptorCount];
        for (int i=0; i<descriptorCount; i++)
            descriptorName[i] = mSourceTableModel.getColumnTitle(mSourceTableModel.getParentColumn(descriptorColumn[i]))
                              + " ["+mSourceTableModel.getDescriptorHandler(descriptorColumn[i]).getInfo().shortName+"]";
        String name = (String)JOptionPane.showInputDialog(mParentFrame,
                "Please select one of these descriptor columns!",
                "Select Descriptor",
                JOptionPane.QUESTION_MESSAGE,
                null,
                descriptorName,
                descriptorName[0]);
        for (int i=0; i<descriptorCount; i++) {
            if (descriptorName[i].equals(name)) {
                mDescriptorColumn = descriptorColumn[i];
                break;
                }
            }
        }
    
    public void run() {
        try {
            run_kNN_analysis();
            }
        catch (OutOfMemoryError e) {
            final String message = "Out of memory. Launch DataWarrior with Java option -Xms???m or -Xmx???m.";
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(mParentFrame, message);
                    }
                } );
            }

        mProgressDialog.close(mTargetFrame);
        }

    private void run_kNN_analysis() {
        int compounds = mSourceTableModel.getTotalRowCount();
        int compoundIncrement = 1;
        if (compounds > 1000) {
            compounds = 1000;
            compoundIncrement = mSourceTableModel.getTotalRowCount() / 1000;
            }

        boolean continuous = (mSourceTableModel.getColumnProperty(mSomXColumn, CompoundTableModel.cColumnPropertyCyclicDataMax) != null
                           && mSourceTableModel.getColumnProperty(mSomYColumn, CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
        double maxX = mSourceTableModel.getMaximumValue(mSomXColumn);
        double maxY = mSourceTableModel.getMaximumValue(mSomYColumn);
        double diagonal = Math.sqrt(maxX*maxX+maxY*maxY);
        if (continuous)
            diagonal /= 2.0;
        int rankCount = Math.min(100, mSourceTableModel.getTotalRowCount()-1);

        final String[] columnName = {"somSim", "somSimRank", "descSim", "descSimRank"};
        double[][] data = new double[compounds*rankCount][2];
        int[][] rows = new int[compounds*rankCount][2];

        mProgressDialog.startProgress("Running kNN anaylsis...", 0, compounds);
        for (int compound=0; compound<compounds; compound++) {
            int row = compound * compoundIncrement;
            mProgressDialog.updateProgress(compound);

            long[] somSimilarity = new long[rankCount];
            long[] chemSimilarity = new long[rankCount];
            for (int i=0; i<mSourceTableModel.getTotalRowCount(); i++) {
                if (mProgressDialog.threadMustDie())
                    return;

                if (i != row) {
                    double dx = Math.abs(mSourceTableModel.getDoubleAt(row, mSomXColumn)
                                       - mSourceTableModel.getDoubleAt(i, mSomXColumn));
                    if (dx > maxX/2 && continuous)
                        dx = maxX - dx;
                    double dy = Math.abs(mSourceTableModel.getDoubleAt(row, mSomYColumn)
                                       - mSourceTableModel.getDoubleAt(i, mSomYColumn));
                    if (dy > maxY/2 && continuous)
                        dy = maxY - dy;
                    double somSim = 1.0-Math.sqrt(dx*dx + dy*dy)/diagonal;
                    long somSimWithIndex = ((long)(somSim * 0x7FFFFFFF) << 32) + i;
                    if (somSimWithIndex > somSimilarity[rankCount-1]) {
                        int simIndex = -1;
                        for (int j=0; j<rankCount; j++) {
                            if (somSimWithIndex > somSimilarity[j]) {
                                simIndex = j;
                                break;
                                }
                            }
                        for (int j=rankCount-2; j>=simIndex; j--) {
                            somSimilarity[j+1] = somSimilarity[j];
                            }
                        somSimilarity[simIndex] = somSimWithIndex;
                        }

                    double chemSim = mSourceTableModel.getDescriptorSimilarity(mSourceTableModel.getTotalRecord(row),
                                                                         mSourceTableModel.getTotalRecord(i),
                                                                         mDescriptorColumn);

                    long chemSimWithIndex = ((long)(chemSim * 0x7FFFFFFF) << 32) + i;
                    if (chemSimWithIndex > chemSimilarity[rankCount-1]) {
                        int simIndex = -1;
                        for (int j=0; j<rankCount; j++) {
                            if (chemSimWithIndex > chemSimilarity[j]) {
                                simIndex = j;
                                break;
                                }
                            }
                        for (int j=rankCount-2; j>=simIndex; j--) {
                            chemSimilarity[j+1] = chemSimilarity[j];
                            }
                        chemSimilarity[simIndex] = chemSimWithIndex;
                        }
                    }
                }

            for (int i=0; i<rankCount; i++) {
                int rowIndex = compound*rankCount+i;
                data[rowIndex][0] = (double)(somSimilarity[i] >>> 32) / 0x7FFFFFFF;
                rows[rowIndex][0] = (int)(somSimilarity[i] & 0x7FFFFFFF);

                data[rowIndex][1] = (double)(chemSimilarity[i] >>> 32) / 0x7FFFFFFF;
                rows[rowIndex][1] = (int)(chemSimilarity[i] & 0x7FFFFFFF);
                }
            }

        if (!mProgressDialog.threadMustDie()) {
            final int MAX_RANK = 20;
            int[] somFinding = new int[MAX_RANK];
            int[] somCount = new int[MAX_RANK];
            int[] chemFinding = new int[MAX_RANK];
            int[] chemCount = new int[MAX_RANK];
            for (int compound=0; compound<compounds; compound++) {
                int base = compound*rankCount;
                for (int rank=0; rank<MAX_RANK; rank++) {
                    int somRow = rows[base+rank][0];
                    boolean found = false;
                    for (int r=0; r<2*(rank+1); r++) {
                        if (rows[base+r][1] == somRow) {
                            found = true;
                            break;
                            }
                        }
                    for (int r=rank; r<MAX_RANK; r++) {
                        somCount[r]++;
                        if (found)
                            somFinding[r]++;
                        }

                
                    int chemRow = rows[base+rank][1];
                    found = false;
                    for (int r=0; r<2*(rank+1); r++) {
                        if (rows[base+r][0] == chemRow) {
                            found = true;
                            break;
                            }
                        }
                    for (int r=rank; r<MAX_RANK; r++) {
                        chemCount[r]++;
                        if (found)
                            chemFinding[r]++;
                        }
                    }
                }

            String filename = mSourceTableModel.getFile().getName();
            int index1 = filename.indexOf("_");
            int index2 = filename.indexOf(".ode");
            String file = (index1 != -1 && index2 != -1) ? filename.substring(index1+1, index2) : "unknown";
            for (int i=0; i<MAX_RANK; i++)
                System.out.println(file+"\tsom->chem\t"+(i+1)+"\t"+(double)((int)(10000*(double)somFinding[i]/(double)somCount[i]))/100);
            for (int i=0; i<MAX_RANK; i++)
                System.out.println(file+"\tchem->som\t"+(i+1)+"\t"+(double)((int)(10000*(double)chemFinding[i]/(double)chemCount[i]))/100);
            }

        mTargetFrame = mApplication.getEmptyFrame("SOM kNN-Analysis");
        CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
        targetTableModel.initializeTable(data.length, 4);
        for (int column=0; column<4; column++)
        	targetTableModel.setColumnName(columnName[column], column);
        for (int row=0; row<data.length; row++) {
            for (int i=0; i<2; i++) {
            	targetTableModel.setTotalValueAt(""+data[row][i], row, i*2);
            	targetTableModel.setTotalValueAt(""+(1+row%rankCount), row, i*2+1);
                }
            }

        targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultRuntimeProperties, mProgressDialog);
        }
    }
