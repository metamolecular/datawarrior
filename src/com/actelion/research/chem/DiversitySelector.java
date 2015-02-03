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

package com.actelion.research.chem;

import java.util.Arrays;
import java.util.Comparator;
import com.actelion.research.calc.DataProcessor;

public class DiversitySelector extends DataProcessor {
	private int			mNoOfFeatures,mExistingSetCount;
	private int[][] 	mFeatureList;
	private boolean		mAddingToExistingSet;
	private double[]	mCentroidVector;

	public DiversitySelector() {
		super();
		}

	public void initializeExistingSet(int noOfKeys) {
		mFeatureList = new int[1][];
		mCentroidVector = new double[noOfKeys];
		mExistingSetCount = 0;
	    }

	public void addToExistingSet(int[] featureList) {
		mFeatureList[0] = featureList;
		DiversitySelectorRecord record = new DiversitySelectorRecord(0);
		record.addToCentroidVector();
		mAddingToExistingSet = true;
		mExistingSetCount++;
    	}


	public void setExistingSet(int[][] featureList) {
		// create centroid vector based on already selected set of compounds
		mCentroidVector = new double[32*featureList[0].length];
		mFeatureList = featureList;
		for (int compound=0; compound<featureList.length; compound++) {
			DiversitySelectorRecord record = new DiversitySelectorRecord(compound);
			record.addToCentroidVector();
			}

		mAddingToExistingSet = true;
		mExistingSetCount = featureList.length;
		}


	public int[] select(int[][] featureList, int compoundsToSelect) {
		int compoundsAvailable = featureList.length;
		mNoOfFeatures = 32*featureList[0].length;
		mFeatureList = featureList;

		if (compoundsToSelect > compoundsAvailable)
			compoundsToSelect = compoundsAvailable;

		startProgress("Creating Key Lists...", 0, compoundsAvailable);

		DiversitySelectorRecord[] recordList = new DiversitySelectorRecord[compoundsAvailable];
		for (int compound=0; compound<compoundsAvailable; compound++) {
			recordList[compound] = new DiversitySelectorRecord(compound);
			if ((compound & 63) == 63) {
				if (threadMustDie()) {
				    stopProgress("Selection cancelled");
					return null;
					}
				updateProgress(compound);
				}
			}

	    startProgress("Locating Starting Compound...", 0, compoundsAvailable);

		if (!mAddingToExistingSet) {
			// find most similar compound as starting point
			mCentroidVector = new double[mNoOfFeatures];
			for (int compound=0; compound<compoundsAvailable; compound++) {
				recordList[compound].addToCentroidVector();
				if ((compound & 127) == 127) {
					if (threadMustDie()) {
					    stopProgress("Selection cancelled");
						return null;
						}
					updateProgress(compound/2);
					}
				}
			double maxDotProduct = 0.0;
			int maxCompoundIndex = 0;
			for (int compound=0; compound<compoundsAvailable; compound++) {
				// dot product must be based on the centroid vector of the complete set minus the compound
				// under investigation.
				double dotProduct = 0.0;
				for (int keyIndex=0; keyIndex<recordList[compound].mKeyList.length; keyIndex++) {
					int key = recordList[compound].mKeyList[keyIndex];
					dotProduct += (mCentroidVector[key] - recordList[compound].mWeight) * recordList[compound].mWeight;
					}
				if (maxDotProduct < dotProduct) {
					maxDotProduct = dotProduct;
					maxCompoundIndex = compound;
					}
				if ((compound & 127) == 127) {
					if (threadMustDie()) {
					    stopProgress("Selection cancelled");
						return null;
						}
					updateProgress((compoundsAvailable+compound)/2);
					}
				}
			DiversitySelectorRecord startCompound = recordList[maxCompoundIndex];
			recordList[maxCompoundIndex] = recordList[0];
			recordList[0] = startCompound;

			mCentroidVector = new double[mNoOfFeatures];
			startCompound.addToCentroidVector();
			}

/*
try {
BufferedWriter writer = new BufferedWriter(new FileWriter("d:\\test.txt"));
writer.write("run\tposition\tindex\tdotProduct\n");
*/

	    startProgress("Selecting Compounds...", 0, compoundsToSelect);

		int selectionCycle = 0;
		for (int compound=(mAddingToExistingSet)?0:1; compound<compoundsToSelect; compound++) {
			// set noOfCompoundsToSort larger than typical maximum of sort position change considering
			// no of compounds already in centroid vector and size of remaining compound set
			int noOfCompoundsToSort = (int)(10.0*(mExistingSetCount+compoundsAvailable)/(mExistingSetCount+compound));

			// increase every forth sorting noOfCompoundsToSort by factor of 4
			// increase every 16th sorting noOfCompoundsToSort by factor of 16
			// increase every 64th sorting noOfCompoundsToSort by factor of 64 and so on
			int mask = 0x00000003;
			while (noOfCompoundsToSort < compoundsAvailable - compound) {
				if ((selectionCycle & mask) != 0)
					break;
				mask = (mask << 2) | 0x00000003;
				noOfCompoundsToSort *= 4;
				}

			int lastCompoundToConsider = compound + noOfCompoundsToSort;
			if (lastCompoundToConsider > compoundsAvailable)
				lastCompoundToConsider = compoundsAvailable;

			for (int i=compound; i<lastCompoundToConsider; i++)
				recordList[i].calculateDotProduct();

			Arrays.sort(recordList, compound, lastCompoundToConsider,
                        new DiversitySelectorComparator<DiversitySelectorRecord>());
			recordList[compound].addToCentroidVector();
			selectionCycle++;
/*
for (int i=compound; i<compoundsAvailable; i++)
if (recordList[i].mCompoundIndex < 20)
writer.write(compound+"\t"+i+"\t"+(1+recordList[i].mCompoundIndex)+"\t"+recordList[i].mDotProduct+"\n");
*/
			if (threadMustDie()) {
			    stopProgress("Selection cancelled");
				return null;
				}
			updateProgress(compound);
			}

/*
writer.close();
} catch (Exception e) {
e.printStackTrace();
}
*/
		int[] selected = new int[compoundsToSelect];
		for (int compound=0; compound<compoundsToSelect; compound++)
			selected[compound] = recordList[compound].mCompoundIndex;

	    stopProgress("Compound Selection Done");

			// the selected array contains compound indices of selected compounds only
			// sorted by the selection order (first selected is first in array)
		return selected;
		}


	protected class DiversitySelectorRecord {
		private int mCompoundIndex;
		private int[] mKeyList;
		private double mWeight;
		public double mDotProduct;

		private DiversitySelectorRecord(int index) {
			int count = 0;
			for (int feature=0; feature<mNoOfFeatures; feature++)
				if ((mFeatureList[index][feature/32] & (1 << (31-feature%32))) != 0)
					count++;
			mKeyList = new int[count];
			count = 0;
			for (int feature=0; feature<mNoOfFeatures; feature++)
				if ((mFeatureList[index][feature/32] & (1 << (31-feature%32))) != 0)
					mKeyList[count++] = feature;

			mWeight = 1.0 / Math.sqrt((double)count);
			mCompoundIndex = index;
			}

		private void addToCentroidVector() {
			for (int i=0; i<mKeyList.length; i++)
				mCentroidVector[mKeyList[i]] += mWeight;
			}

		private void calculateDotProduct() {
			mDotProduct = 0.0;
			for (int i=0; i<mKeyList.length; i++)
				mDotProduct += mWeight * mCentroidVector[mKeyList[i]];
			}
		}

    class DiversitySelectorComparator<T> implements Comparator<T> {
        public int compare(T o1, T o2) {
            double d1 = ((DiversitySelectorRecord)o1).mDotProduct;
            double d2 = ((DiversitySelectorRecord)o2).mDotProduct;
            return (d1 < d2) ? -1 : (d1 == d2) ? 0 : 1;
            }
        }
    }
