 /*
 * @(#)ConformationSampler.java
 *
 * Copyright 2004 Actelion Ltd., Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Actelion Pharmaceuticals, Ltd.
 * Use is subject to license terms.
 *
 * @author Thomas Sander
 */

package com.actelion.research.chem.conf;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.actelion.research.calc.DataProcessor;
import com.actelion.research.calc.SingularValueDecomposition;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.RingCollection;
import com.actelion.research.chem.StereoMolecule;

public class ConformationSampler extends DataProcessor {

	private static final int	BREAKOUT_ROUNDS = 5;
	private static final int	CYCLE_MULTIPLIER_BREAKOUT_PHASE = 1000;
	private static final int	CYCLE_MULTIPLIER_OPTIMIZATION_PHASE = 2000;
	private static final int	CYCLE_MULTIPLIER_MINIMIZATION_PHASE = 500;
	private static final float	STANDARD_CYCLE_FACTOR = 0.2f;
	private static final float	MINIMIZATION_REDUCTION_FACTOR = 20.0f;
	private static final float	ATOM_BREAKOUT_STRAIN = 0.02f;
	private static final float	WEAK_CONSTRAINT_STRAIN_LIMIT = 0.02f;

	private final StereoMolecule	mMol;
	private final float[][][]		mDistance;
	private final boolean[][]		mDistanceConstraintIsMinMax;
	private final BondLengthSet		mBondLengthSet;
	private final BondAngleSet		mBondAngleSet;
	private final ConformationConstraint[] mConstraint;
	private volatile boolean		mIsSMT;
	private ThreadData				mThreadData;
	private ConformerData[]			mConformer;
	private AtomicInteger			mConformerSMTIndex;

	/**
	 * Generates a new ConformationSampler from the given molecule.
	 * Explicit hydrogens are removed from the molecule, which stays
	 * untouched otherwise. A set of constraints for the coordinate
	 * generation is compiled during the construction.
	 * @param mol
	 */
	public ConformationSampler(final StereoMolecule mol) {

/*// winkel zwischen zwei vektoren:
final float[] v1 = { Math.sqrt(3.0)/2.0, 0.5, 0 };
final float[] v2 = { -1, 0, 1 };
float cosa = (v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2])
			/ (Math.sqrt(v1[0]*v1[0]+v1[1]*v1[1]+v1[2]*v1[2])
			 * Math.sqrt(v2[0]*v2[0]+v2[1]*v2[1]+v2[2]*v2[2]));
float a = Math.acos(cosa);
System.out.println("angle:"+a+"  in degrees:"+(a*180/Math.PI));
*/
		mMol = mol;
		mMol.removeExplicitHydrogens();
		mMol.ensureHelperArrays(Molecule.cHelperParities);
		
		mBondLengthSet = new BondLengthSet(mol);
		mBondAngleSet = new BondAngleSet(mol, mBondLengthSet);

		mDistance = new float[mMol.getAtoms()][][];
	    mDistanceConstraintIsMinMax = new boolean[mMol.getAtoms()][];

		calculateDistanceConstraints();

		ArrayList<ConformationConstraint> constraintList = new ArrayList<ConformationConstraint>();
		calculatePlaneConstraints(constraintList);
///		calculateLineConstraints(constraintList);
//		calculateStereoConstraints(constraintList);
		mConstraint = constraintList.toArray(new ConformationConstraint[0]);
		}

	/**
	 * Adapts the distance constraints by interpreting the first conformer
	 * of the previous run.
	 */
	public void boostDistanceConstraints() {
		for (int atom1=1; atom1<mMol.getAtoms(); atom1++) {
			for (int atom2=0; atom2<atom1; atom2++) {
				if (mDistanceConstraintIsMinMax[atom1][atom2]) {
					float dx = mMol.getAtomX(atom2) - mMol.getAtomX(atom1);
					float dy = mMol.getAtomY(atom2) - mMol.getAtomY(atom1);
					float dz = mMol.getAtomZ(atom2) - mMol.getAtomZ(atom1);
					float distance = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
					if (mDistance[atom1][atom2][0] < distance)
						mDistance[atom1][atom2][0] = distance - 0.5f;	// to relax piling up strains
					}
				}
			}
		}

	/**
	 * Generates the coordinates for one conformer in the calling thread.
	 * Progress is reported through the standard DataProcessor calls.
	 * If you need one or more conformers from many molecules with maximum
	 * performance, then process the molecules in multiple threads and generate
	 * conformers one after another with this method.
     * @param randomSeed 0 or specific seed
	 */
	public void generateConformer(long randomSeed) {
		mIsSMT = false;
		mConformer = new ConformerData[1];
		mConformer[0] = new ConformerData(mMol);
		mThreadData = new ThreadData(randomSeed);
		generateConformer(mConformer[0], mThreadData);
		}

	/**
	 * Creates as many new threads as there are cores to generate the defined
	 * number of conformers. Returns after all thread are finished.
	 * Does not report any progress.
	 * If you need many conformers from one(!) molecule quickly, this is the
	 * method of choice. However, if you process many molecules, then create
	 * the threads yourself and use generateConformer() as often as needed for
	 * any molecule.
	 * @param conformerCount
	 */
	public void generateConformersSMT(final int conformerCount) {
		mIsSMT = true;
		mConformer = new ConformerData[conformerCount];
		mConformerSMTIndex = new AtomicInteger(conformerCount);
		int threadCount = Runtime.getRuntime().availableProcessors();
		Thread[] thread = new Thread[threadCount];
        for (int i=0; i<threadCount; i++) {
    		thread[i] = new Thread("Conformer Generator "+(i+1)) {
    			public void run() {
    				ThreadData theadData = new ThreadData(0);
    				int conformerIndex;
    				while ((conformerIndex = mConformerSMTIndex.decrementAndGet()) >= 0 && !threadMustDie()) {
    					mConformer[conformerIndex] = new ConformerData(mMol);
    					generateConformer(mConformer[conformerIndex], theadData);
    					}
    				}
    			};
    		thread[i].setPriority(Thread.MIN_PRIORITY);
    		thread[i].start();
    		}
        for (int i=0; i<threadCount; i++) {
        	try { thread[i].join(); } catch (InterruptedException ie) {}
        	}
		}

	private void generateConformer(ConformerData conformer, ThreadData threadData) {

/*
System.out.println("---------------------------------------------------------------------");
for (int i=0; i<mMol.getAtoms(); i++) {
	System.out.print(""+i+" "+Molecule.cAtomLabel[mMol.getAtomicNo(i)]);
	for (int j=0; j<mMol.getConnAtoms(i); j++) {
		int connBond = mMol.getConnBond(i, j);
		if (mMol.isAromaticBond(connBond))
			System.out.print(" .");
		else if (mMol.getBondOrder(connBond) == 1)
			System.out.print(" -");
		else if (mMol.getBondOrder(connBond) == 2)
			System.out.print(" =");
		System.out.print(""+mMol.getConnAtom(i, j));
		}
	System.out.println();
	}

System.out.println("Distance Constraints:");
for (int i=1; i<mMol.getAtoms(); i++) {
for (int j=0; j<i; j++) {
if (mDistanceConstraintIsMinMax[i][j])
System.out.println("("+i+","+j+") min:"+mDistance[i][j][0]+" max:"+mDistance[i][j][1]);
else {
System.out.print("("+i+","+j+"):");
for (int k=0; k<mDistance[i][j].length; k++)
System.out.print(" "+mDistance[i][j][k]);
System.out.println();
}
}
}
System.out.println("Angles:");
for (int i=0; i<mMol.getAtoms(); i++) {
for (int j=1; j<mMol.getConnAtoms(i); j++) {
for (int k=0; k<j; k++) {
System.out.print(mBondAngle[i][j][k]+" ");
}}
System.out.println();
}
*/

		jumbleAtoms(conformer, threadData, false);

		optimize(conformer, threadData, mMol.getAtoms() * CYCLE_MULTIPLIER_BREAKOUT_PHASE, 2*STANDARD_CYCLE_FACTOR, STANDARD_CYCLE_FACTOR);

		for (int i=0; i<BREAKOUT_ROUNDS; i++) {
//System.out.println("optimization round "+i);
			if (jumbleAtoms(conformer, threadData, true) == 0)
				break;

			optimize(conformer, threadData, mMol.getAtoms() * CYCLE_MULTIPLIER_BREAKOUT_PHASE, STANDARD_CYCLE_FACTOR, STANDARD_CYCLE_FACTOR);
			}

		disableConflictingWeakConstraints(conformer, threadData);

		optimize(conformer, threadData, mMol.getAtoms() * CYCLE_MULTIPLIER_OPTIMIZATION_PHASE, STANDARD_CYCLE_FACTOR, STANDARD_CYCLE_FACTOR);
		optimize(conformer, threadData, mMol.getAtoms() * CYCLE_MULTIPLIER_MINIMIZATION_PHASE, STANDARD_CYCLE_FACTOR, STANDARD_CYCLE_FACTOR/MINIMIZATION_REDUCTION_FACTOR);
		}

	/**
	 * If you don't need a StereoMolecule with conformer coordinates,
	 * then use this function to access the coords.
	 * @param n index of the conformer
	 * @param atom
	 * @return returns x-coordinate of conformer n
	 */
	public float getAtomX(int n, int atom) {
		return mConformer[n].x[atom];
		}

	/**
	 * If you don't need a StereoMolecule with conformer coordinates,
	 * then use this function to access the coords.
	 * @param n index of the conformer
	 * @param atom
	 * @return returns x-coordinate of conformer n
	 */
	public float getAtomY(int n, int atom) {
		return mConformer[n].y[atom];
		}

	/**
	 * If you don't need a StereoMolecule with conformer coordinates,
	 * then use this function to access the coords.
	 * @param n index of the conformer
	 * @param atom
	 * @return returns x-coordinate of conformer n
	 */
	public float getAtomZ(int n, int atom) {
		return mConformer[n].z[atom];
		}

	/**
	 * Returns original StereoMolecule with its atom coordinates adapted
	 * to reflect the n-th conformer.
	 * @return 
	 */
	public StereoMolecule getConformerInPlace(int n) {
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			mMol.setAtomX(atom, mConformer[n].x[atom]);
			mMol.setAtomY(atom, mConformer[n].y[atom]);
			mMol.setAtomZ(atom, mConformer[n].z[atom]);
			}
		return mMol;
		}

	/**
	 * Creates a compact copy of the original StereoMolecule with the
	 * coordinates updated to reflect the first or only conformer.
	 * If no conformers were generated yet, the molecule copy contains
	 * the original coordinates.
	 * @return 
	 */
	public StereoMolecule getConformer() {
		return (mConformer == null) ? mMol.getCompactCopy() : getConformer(0);
		}

	/**
	 * Creates a compact copy of the original StereoMolecule with the
	 * coordinates updated to reflect the n-th conformer.
	 * @return 
	 */
	public StereoMolecule getConformer(int n) {
		StereoMolecule mol = mMol.getCompactCopy();
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			mol.setAtomX(atom, mConformer[n].x[atom]);
			mol.setAtomY(atom, mConformer[n].y[atom]);
			mol.setAtomZ(atom, mConformer[n].z[atom]);
			}
		return mol;
		}

	public void optimize(int cycles, float startFactor, float endFactor) {
		optimize(mConformer[0], mThreadData, cycles, startFactor, endFactor);
		}

	private void optimize(ConformerData conformer, ThreadData threadData, int cycles, float startFactor, float endFactor) {
		float otherConstraintLikelyhood = 0.05f * (float)mConstraint.length
												/ (float)mMol.getAtoms();

		float k = (float)Math.log(startFactor/endFactor)/(float)cycles;

		if (!mIsSMT)
			startProgress((startFactor==endFactor)?"optimizing...":"finetuning...", 0, cycles);

		for (int cycle=0; cycle<cycles; cycle++) {
			float cycleFactor = (float)Math.exp(-k*cycle);
			if (mConstraint.length != 0 && threadData.random.nextFloat() < otherConstraintLikelyhood) {
				int index = (int)(threadData.random.nextFloat() * mConstraint.length);
				if (!threadData.isConstraintDisabled[index]) {
					ConformationConstraint constraint =  mConstraint[index];
					switch (constraint.type) {
					case ConformationConstraint.PLANE_CONSTRAINT:
					case ConformationConstraint.PLANE_CONSTRAINT_WEAK:
						handlePlaneConstraint(conformer, constraint.atomList, 0.5f * cycleFactor);
						break;
					case ConformationConstraint.LINE_CONSTRAINT:
						handleLineConstraint(conformer, constraint.atomList, cycleFactor);
						break;
					case ConformationConstraint.STEREO_CONSTRAINT:
						handleStereoConstraint(conformer, constraint.atomList);
						break;
						}
					}
				}
			else {
				handleDistanceConstraint(conformer, threadData, cycleFactor);
				}

			if (!mIsSMT && (cycle % 500 == 499))
				updateProgress(cycle);
			}

		if (!mIsSMT)
			stopProgress("done...");

		threadData.atomStrain = null;
		}

	/**
	 * Jumble atom positions in one thread.
	 * @param randomSeed 0 or specific seed
	 */
	public void jumbleAtoms(long randomSeed) {
		mIsSMT = false;
		mConformer = new ConformerData[1];
		mConformer[0] = new ConformerData(mMol);
		mThreadData = new ThreadData(randomSeed);

		jumbleAtoms(mConformer[0], mThreadData, false);

		stopProgress("finished jumbling.");
		}

	private int jumbleAtoms(ConformerData conformer, ThreadData threadData, boolean highStrainAtomsOnly) {
		if (highStrainAtomsOnly)
			calculateAtomStrains(conformer, threadData);

		int atomCount = 0;
		float boxSize = 1.0f + 3.0f * (float)Math.sqrt(mMol.getAtoms());
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (!highStrainAtomsOnly || threadData.atomStrain[atom] > ATOM_BREAKOUT_STRAIN) {
				conformer.x[atom] = boxSize * threadData.random.nextFloat() - boxSize / 2;
				conformer.y[atom] = boxSize * threadData.random.nextFloat() - boxSize / 2;
				conformer.z[atom] = boxSize * threadData.random.nextFloat() - boxSize / 2;
				atomCount++;
				}
			}

		if (atomCount != 0)
			threadData.atomStrain = null;

		return atomCount;
		}

	private void disableConflictingWeakConstraints(ConformerData conformer, ThreadData threadData) {
		calculateAtomStrains(conformer, threadData);

		for (int i=0; i<mConstraint.length; i++) {
			if (mConstraint[i].type == ConformationConstraint.PLANE_CONSTRAINT_WEAK) {
				for (int j=0; j<mConstraint[i].atomList.length; j++) {
					int atom = mConstraint[i].atomList[j];
					if (threadData.atomStrain[atom] > WEAK_CONSTRAINT_STRAIN_LIMIT) {
						threadData.isConstraintDisabled[i] = true;
							// to escape from planarity already achieved by constraint
						addRandomDistortion(conformer, threadData, mConstraint[i].atomList, 0.5f);
						break;
						}
					}
				}
			}
		}

	private void addRandomDistortion(ConformerData conformer, ThreadData threadData, int[] atomList, float maximum) {
		float range = 2.0f * maximum;
		for (int i=0; i<atomList.length; i++) {
			int atom = atomList[i];
			conformer.x[atom] = conformer.x[atom]+range*threadData.random.nextFloat()-maximum;
			conformer.y[atom] = conformer.y[atom]+range*threadData.random.nextFloat()-maximum;
			conformer.z[atom] = conformer.z[atom]+range*threadData.random.nextFloat()-maximum;
			}
		}

	private void handleDistanceConstraint(ConformerData conformer, ThreadData threadData, float cycleFactor) {
		int atom1 = (int)(threadData.random.nextFloat() * mMol.getAtoms());
		int atom2 = (int)(threadData.random.nextFloat() * mMol.getAtoms());
		while (atom2 == atom1)
			atom2 = (int)(threadData.random.nextFloat() * mMol.getAtoms());

		if (atom1 < atom2) {
			int temp = atom1;
			atom1 = atom2;
			atom2 = temp;
			}

		float dx = conformer.x[atom2] - conformer.x[atom1];
		float dy = conformer.y[atom2] - conformer.y[atom1];
		float dz = conformer.z[atom2] - conformer.z[atom1];
		float distance = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);

		float distanceFactor = 0.0f;
		if (mDistanceConstraintIsMinMax[atom1][atom2]) {
			if (distance < mDistance[atom1][atom2][0]) {
				distanceFactor = (distance-mDistance[atom1][atom2][0])/(2*mDistance[atom1][atom2][0]);
				if (cycleFactor > 1.0f)
					cycleFactor = 1.0f;
				}
			else if (distance > mDistance[atom1][atom2][1]) {
				distanceFactor = (distance-mDistance[atom1][atom2][1])/(2*distance);
				}
			}
		else {
		    float minDistanceDif = Math.abs(mDistance[atom1][atom2][0]-distance);
		    int minDistanceDifIndex = 0;
		    for (int i=1; i<mDistance[atom1][atom2].length; i++) {
		        float distanceDif = Math.abs(mDistance[atom1][atom2][i]-distance);
		        if (minDistanceDif > distanceDif) {
		            minDistanceDif = distanceDif;
		            minDistanceDifIndex = i;
			        }
			    }
			if (distance < mDistance[atom1][atom2][minDistanceDifIndex]) {
				distanceFactor = (distance-mDistance[atom1][atom2][minDistanceDifIndex])
							   / (2*mDistance[atom1][atom2][minDistanceDifIndex]);
				if (cycleFactor > 1.0f)
					cycleFactor = 1.0f;
				}
			else if (distance > mDistance[atom1][atom2][minDistanceDifIndex]) {
				distanceFactor = (distance-mDistance[atom1][atom2][minDistanceDifIndex])/(2*distance);
				}
			}

		if (distanceFactor != 0.0) {
			float factor = cycleFactor * distanceFactor;
			conformer.x[atom1] += dx*factor;
			conformer.x[atom2] -= dx*factor;
			conformer.y[atom1] += dy*factor;
			conformer.y[atom2] -= dy*factor;
			conformer.z[atom1] += dz*factor;
			conformer.z[atom2] -= dz*factor;
			}
		}

	private void handlePlaneConstraint(ConformerData conformer, int[] atomList, float cycleFactor) {
		float[][] A = new float[atomList.length][3];
		for (int i=0; i<atomList.length; i++) {
			A[i][0] = conformer.x[atomList[i]];
			A[i][1] = conformer.y[atomList[i]];
			A[i][2] = conformer.z[atomList[i]];
			}

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				for (int k=0; k<3; k++)
					squareMatrix[j][k] += A[i][j] * A[i][k];

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[] S = svd.getSingularValues();
		int minIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] < S[minIndex])
				minIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted plane
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][minIndex];

		float[] cog = new float[3];	// center of gravity
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				cog[j] += A[i][j];
		for (int j=0; j<3; j++)
			cog[j] /= atomList.length;

		double d = n[0]*cog[0] + n[1]*cog[1] + n[2]*cog[2];	// distance of Hesse equation of plane

		for (int i=0; i<atomList.length; i++) {
			float distance = (float)(d - n[0]*A[i][0] - n[1]*A[i][1] - n[2]*A[i][2]);
			conformer.x[atomList[i]] = A[i][0] + distance*cycleFactor*n[0];
			conformer.y[atomList[i]] = A[i][1] + distance*cycleFactor*n[1];
			conformer.z[atomList[i]] = A[i][2] + distance*cycleFactor*n[2];
			}
		}

	private void handleLineConstraint(ConformerData conformer, int[] atomList, float cycleFactor) {
		float[][] A = new float[atomList.length][3];
		for (int i=0; i<atomList.length; i++) {
			A[i][0] = conformer.x[atomList[i]];
			A[i][1] = conformer.y[atomList[i]];
			A[i][2] = conformer.z[atomList[i]];
			}

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				for (int k=0; k<3; k++)
					squareMatrix[j][k] += A[i][j] * A[i][k];

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[] S = svd.getSingularValues();
		int maxIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] > S[maxIndex])
				maxIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted line
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][maxIndex];

		float[] cog = new float[3];	// center of gravity
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				cog[j] += A[i][j];
		for (int j=0; j<3; j++)
			cog[j] /= atomList.length;

			// for a point P on the fitted line is: P = COG + lamda * N
		for (int i=0; i<atomList.length; i++) {
				// calculate lamda that gives the closest point to current atom location
			float lamda = n[0]*(A[i][0]-cog[0])+n[1]*(A[i][1]-cog[1])+n[2]*(A[i][2]-cog[2]);
			conformer.x[atomList[i]] = A[i][0] + cycleFactor*(cog[0]+lamda*n[0]-A[i][0]);
			conformer.y[atomList[i]] = A[i][1] + cycleFactor*(cog[1]+lamda*n[1]-A[i][1]);
			conformer.z[atomList[i]] = A[i][2] + cycleFactor*(cog[2]+lamda*n[2]-A[i][2]);
			}
		}

	private void handleStereoConstraint(ConformerData conformer, int[] atomList) {
		float[] n = new float[3];
		if (getStereoAngleCosine(conformer, atomList, n) > 0.0) {
			// invert stereocenter by moving atomList[3] through plane of other atoms

			float lenN = (float)Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
			n[0] /= lenN;
			n[1] /= lenN;
			n[2] /= lenN;

			float d = n[0]*conformer.x[atomList[0]]
					+ n[1]*conformer.y[atomList[0]]
					+ n[2]*conformer.z[atomList[0]];	// distance of Hesse equation of plane

			float distance = n[0]*conformer.x[atomList[3]]
						   + n[1]*conformer.y[atomList[3]]
						   + n[2]*conformer.z[atomList[3]] - d;

			float neededDistance = (atomList[4] == -1) ?
						// stereo center with 3 substituents (atomList[3] is central atom)
									-(float)Math.cos(109.47 * Math.PI/180.0) / 3.0f
								  * (getMinDistance(atomList[0], atomList[3])
								   + getMinDistance(atomList[1], atomList[3])
								   + getMinDistance(atomList[2], atomList[3]))
						// stereo center with 4 substituents (atomList[4] is central atom)
								  :	-(float)Math.cos(109.47 * Math.PI/180.0) / 3.0f
								  * (getMinDistance(atomList[0], atomList[4])
								   + getMinDistance(atomList[1], atomList[4])
								   + getMinDistance(atomList[2], atomList[4]))
								  + getMinDistance(atomList[3], atomList[4]);

			float neededMovement = (distance < 0.0) ? distance - neededDistance
													 : distance + neededDistance;

			for (int i=0; i<4; i++) {
				int atom = atomList[i];
				float factor = (i == 3) ? -0.75f : 0.25f;
				conformer.x[atom] += factor*neededMovement*n[0];
				conformer.y[atom] += factor*neededMovement*n[1];
				conformer.z[atom] += factor*neededMovement*n[2];
				}
			}
		}

	public void testStereoConstraint() {
		ConformerData conformer = mConformer[0];
		for (ConformationConstraint constraint:mConstraint) {
			if (constraint.type == ConformationConstraint.STEREO_CONSTRAINT) {
				int[] atomList = constraint.atomList;
				float[] n = new float[3];
				if (getStereoAngleCosine(conformer, atomList, n) > 0.0) {
					// invert stereocenter by moving atomList[3] through plane of other atoms

					float lenN = (float)Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]);
					n[0] /= lenN;
					n[1] /= lenN;
					n[2] /= lenN;

					float d = n[0]*conformer.x[atomList[0]]
							+ n[1]*conformer.y[atomList[0]]
							+ n[2]*conformer.z[atomList[0]];	// distance of Hesse equation of plane

					float distance = n[0]*conformer.x[atomList[3]]
								   + n[1]*conformer.y[atomList[3]]
								   + n[2]*conformer.z[atomList[3]] - d;

					for (int j=0; j<4; j++) {
						int atom = atomList[j];
						float factor = (j == 3) ? -1.5f : 0.5f;
						int newAtom = mMol.addAtom(conformer.x[atom] + (float)(factor*distance*n[0]),
												   conformer.y[atom] + (float)(factor*distance*n[1]),
												   conformer.z[atom] + (float)(factor*distance*n[2]));
						mMol.setAtomicNo(newAtom, 16);
						mMol.addBond(atom, newAtom, 1);
						}
					}
				}
			}
		}

	/**
	 * Gets the sum of atom strains after generating one(!) conformer.
	 * @return
	 */
	public float getStrain() {
		calculateAtomStrains(mConformer[0], mThreadData);
		float strain = 0.0f;
		for (int atom=0; atom<mMol.getAtoms(); atom++)
			strain += mThreadData.atomStrain[atom];
		return strain;
		}

	private void calculateAtomStrains(ConformerData conformer, ThreadData threadData) {
		if (threadData.atomStrain != null)
			return;

		threadData.atomStrain = new float[mMol.getAtoms()];
		for (int atom1=1; atom1<mMol.getAtoms(); atom1++) {
			for (int atom2=0; atom2<atom1; atom2++) {
				float dx = conformer.x[atom2] - conformer.x[atom1];
				float dy = conformer.y[atom2] - conformer.y[atom1];
				float dz = conformer.z[atom2] - conformer.z[atom1];
				float distance = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
				if (mDistanceConstraintIsMinMax[atom1][atom2]) {
					if (distance < mDistance[atom1][atom2][0]) {
						float strain = (mDistance[atom1][atom2][0] - distance) / 2.0f;
						threadData.atomStrain[atom1] += (strain*strain);
						threadData.atomStrain[atom2] += (strain*strain);
						}
					else if (distance > mDistance[atom1][atom2][1]) {
						float strain = (distance - mDistance[atom1][atom2][1]) / 2.0f;
						threadData.atomStrain[atom1] += (strain*strain);
						threadData.atomStrain[atom2] += (strain*strain);
						}
					}
				else {
				    float minDistanceDif = Math.abs(mDistance[atom1][atom2][0]-distance);
				    int minDistanceDifIndex = 0;
				    for (int i=1; i<mDistance[atom1][atom2].length; i++) {
				        float distanceDif = Math.abs(mDistance[atom1][atom2][i]-distance);
				        if (minDistanceDif > distanceDif) {
				            minDistanceDif = distanceDif;
				            minDistanceDifIndex = i;
					        }
					    }
					float strain = (distance - mDistance[atom1][atom2][minDistanceDifIndex]) / 2.0f;
					threadData.atomStrain[atom1] += (strain*strain);
					threadData.atomStrain[atom2] += (strain*strain);
					}
				}
			}

		for (int i=0; i<mConstraint.length; i++) {
			if (!threadData.isConstraintDisabled[i]) {
				switch (mConstraint[i].type) {
				case ConformationConstraint.PLANE_CONSTRAINT:
				case ConformationConstraint.PLANE_CONSTRAINT_WEAK:
					addPlaneStrain(conformer, threadData, mConstraint[i].atomList);
					break;
				case ConformationConstraint.LINE_CONSTRAINT:
					addLineStrain(conformer, threadData, mConstraint[i].atomList);
					break;
				case ConformationConstraint.STEREO_CONSTRAINT:
					addStereoStrain(conformer, threadData, mConstraint[i].atomList);
					break;
					}
				}
			}
/*
System.out.print("atomStrain:");
for (int i=0; i<mAtomStrain.length; i++)
System.out.print(" "+((float)((int)(100000*mAtomStrain[i]))/100000));
System.out.println();
*/
		}

	private void addPlaneStrain(ConformerData conformer, ThreadData threadData, int[] atomList) {
		float[][] A = new float[atomList.length][3];
		for (int i=0; i<atomList.length; i++) {
			A[i][0] = conformer.x[atomList[i]];
			A[i][1] = conformer.y[atomList[i]];
			A[i][2] = conformer.z[atomList[i]];
			}

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				for (int k=0; k<3; k++)
					squareMatrix[j][k] += A[i][j] * A[i][k];

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[] S = svd.getSingularValues();
		int minIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] < S[minIndex])
				minIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted plane
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][minIndex];

		float[] cog = new float[3];	// center of gravity
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				cog[j] += A[i][j];
		for (int j=0; j<3; j++)
			cog[j] /= atomList.length;

		float d = n[0]*cog[0] + n[1]*cog[1] + n[2]*cog[2];	// distance of Hesse equation of plane

		for (int i=0; i<atomList.length; i++) {
			float strain = Math.abs(d - n[0]*A[i][0] - n[1]*A[i][1] - n[2]*A[i][2]);
			threadData.atomStrain[atomList[i]] += (strain*strain);
			}
		}

	private void addLineStrain(ConformerData conformer, ThreadData threadData, int[] atomList) {
		float[][] A = new float[atomList.length][3];
		for (int i=0; i<atomList.length; i++) {
			A[i][0] = conformer.x[atomList[i]];
			A[i][1] = conformer.y[atomList[i]];
			A[i][2] = conformer.z[atomList[i]];
			}

		double[][] squareMatrix = new double[3][3];
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				for (int k=0; k<3; k++)
					squareMatrix[j][k] += A[i][j] * A[i][k];

		SingularValueDecomposition svd = new SingularValueDecomposition(squareMatrix, null, null);
		double[] S = svd.getSingularValues();
		int maxIndex = 0;
		for (int i=1; i<3; i++)
			if (S[i] > S[maxIndex])
				maxIndex = i;

		double[][] U = svd.getU();
		float[] n = new float[3];	// normal vector of fitted line
		for (int i=0; i<3; i++)
			n[i] = (float)U[i][maxIndex];

		float[] cog = new float[3];	// center of gravity
		for (int i=0; i<atomList.length; i++)
			for (int j=0; j<3; j++)
				cog[j] += A[i][j];
		for (int j=0; j<3; j++)
			cog[j] /= atomList.length;

		for (int i=0; i<atomList.length; i++) {
				// calculate lamda that gives the closest point to current atom location
			float lamda = n[0]*(A[i][0]-cog[0])+n[1]*(A[i][1]-cog[1])+n[2]*(A[i][2]-cog[2]);
			float dx = cog[0]+lamda*n[0]-A[i][0];
			float dy = cog[1]+lamda*n[1]-A[i][1];
			float dz = cog[2]+lamda*n[2]-A[i][2];
			threadData.atomStrain[atomList[i]] += dx*dx+dy*dy+dz*dz;
			}
		}

	private void addStereoStrain(ConformerData conformer, ThreadData threadData, int[] atomList) {
		if (getStereoAngleCosine(conformer, atomList) > 0.0) {
			for (int i=0; i<atomList.length; i++)
				if (atomList[i] != -1)
					threadData.atomStrain[atomList[i]] += 0.25; // arbitrary value 0.5 * 0.5
			}
		}

	private float getStereoAngleCosine(ConformerData conformer, int[] atomList) {
		// returns the angle between normal vector of plane (a0->a1, a0->a2) and vector (a0->a3)
		float[] n = new float[3];
		return getStereoAngleCosine(conformer, atomList, n);
		}

	private float getStereoAngleCosine(ConformerData conformer, int[] atomList, float[] n) {
		// returns the angle between normal vector of plane (a0->a1, a0->a2) and vector (a0->a3)

		// calculate the three vectors leading from atom[0] to the other three atoms
		float[][] coords = new float[3][3];
		for (int i=0; i<3; i++) {
			coords[i][0] = conformer.x[atomList[i+1]] - conformer.x[atomList[0]];
			coords[i][1] = conformer.y[atomList[i+1]] - conformer.y[atomList[0]];
			coords[i][2] = conformer.z[atomList[i+1]] - conformer.z[atomList[0]];
			}

		// calculate the normal vector (vector product of coords[0] and coords[1])
		n[0] = coords[0][1]*coords[1][2]-coords[0][2]*coords[1][1];
		n[1] = coords[0][2]*coords[1][0]-coords[0][0]*coords[1][2];
		n[2] = coords[0][0]*coords[1][1]-coords[0][1]*coords[1][0];

		// calculate cos(angle) of coords[2] to normal vector
		return (coords[2][0]*n[0]+coords[2][1]*n[1]+coords[2][2]*n[2])
			/ (float)(Math.sqrt(coords[2][0]*coords[2][0]+coords[2][1]*coords[2][1]+coords[2][2]*coords[2][2])
			 * Math.sqrt(n[0]*n[0]+n[1]*n[1]+n[2]*n[2]));
		}

	private int[] calculateBondRingSizes() {
		int[] bondRingSize = new int[mMol.getBonds()];
		RingCollection ringSet = mMol.getRingSet();
		for (int ring=0; ring<ringSet.getSize(); ring++) {
			int[] ringBond = ringSet.getRingBonds(ring);
			for (int i=0; i<ringBond.length; i++) {
				if (bondRingSize[ringBond[i]] == 0
				 || bondRingSize[ringBond[i]] > ringBond.length) {
					bondRingSize[ringBond[i]] = ringBond.length;
					}
				}
			}
		return bondRingSize;
		}

	private void calculateDistanceConstraints() {
		for (int atom=1; atom<mMol.getAtoms(); atom++) {
			mDistance[atom] = new float[atom][];
			mDistanceConstraintIsMinMax[atom] = new boolean[atom];
			}

					// distances with 1 bond between both atoms
		for (int bond=0; bond<mMol.getBonds(); bond++)
			setFixedDistance(mMol.getBondAtom(0, bond),
							 mMol.getBondAtom(1, bond),
							 mBondLengthSet.getLength(bond));

		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			for (int i=1; i<mMol.getConnAtoms(atom); i++) {
				int connAtom1 = mMol.getConnAtom(atom, i);
				int connBond1 = mMol.getConnBond(atom, i);
				float bondLength1 = mBondLengthSet.getLength(connBond1);

					// distances with 2 bonds between both atoms
				for (int j=0; j<i; j++) {
					int connAtom2 = mMol.getConnAtom(atom, j);
					int connBond2 = mMol.getConnBond(atom, j);

					float angle = mBondAngleSet.getConnAngle(atom, i, j);
	
					float bondLength2 = mBondLengthSet.getLength(connBond2);
					float distance = (float)Math.sqrt(bondLength1*bondLength1+bondLength2*bondLength2
												-2*bondLength1*bondLength2*Math.cos(angle));
					setFixedDistance(connAtom1, connAtom2, distance);
					}
				}
			}

					// distances with 3 bonds between both atoms (special cases only)
		int[] bondRingSize = calculateBondRingSizes();
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			int[] atom = new int[2];
			for (int i=0; i<2; i++)
				atom[i] = mMol.getBondAtom(i, bond);

			if (mMol.getConnAtoms(atom[0]) > 1
			 && mMol.getConnAtoms(atom[1]) > 1) {

					// triple bonds
				if (mMol.getBondOrder(bond) == 3) {
					float distance = mBondLengthSet.getLength(bond);
					int[] connAtom = new int[2];
					for (int i=0; i<2; i++) {
						for (int j=0; j<mMol.getConnAtoms(atom[i]); j++) {
							int connBond = mMol.getConnBond(atom[i], j);
							if (connBond != bond) {
								distance += mBondLengthSet.getLength(connBond);
								connAtom[i] = mMol.getConnAtom(atom[i], j);
								break;
								}
							}
						}
					setFixedDistance(connAtom[0], connAtom[1], distance);
					}

					// strainless float bond with stereo information
					// (including symmetrical ones with parityNone)
				else if (mMol.getBondOrder(bond) == 2
				      && !mMol.isAromaticBond(bond)
					  && mMol.getBondParity(bond) != Molecule.cBondParityUnknown
					  && (bondRingSize[bond] == 0 || bondRingSize[bond] > 5)) {
//					  && (!mMol.isRingAtom(atom[0]) || mMol.getAtomRingSize(atom[0]) > 5)
//					  && (!mMol.isRingAtom(atom[1]) || mMol.getAtomRingSize(atom[1]) > 5)) {
					int[][] connAtom = new int[2][];
					int[][] connBond = new int[2][];
					float[][] connAngle = new float[2][];
					for (int i=0; i<2; i++) {
						connAtom[i] = new int[mMol.getConnAtoms(atom[i])-1];
						connBond[i] = new int[mMol.getConnAtoms(atom[i])-1];
						connAngle[i] = new float[mMol.getConnAtoms(atom[i])-1];

						int floatBondOpponentIndex = -1;
						for (int j=0; j<mMol.getConnAtoms(atom[i]); j++) {
							if (mMol.getConnAtom(atom[i], j) == atom[1-i]) {
							    floatBondOpponentIndex = j;
								break;
								}
							}

						int connIndex = 0;
					    for (int j=0; j<mMol.getConnAtoms(atom[i]); j++) {
							if (j != floatBondOpponentIndex) {
								connAtom[i][connIndex] = mMol.getConnAtom(atom[i], j);
								connBond[i][connIndex] = mMol.getConnBond(atom[i], j);
								connAngle[i][connIndex] = mBondAngleSet.getConnAngle(atom[i], floatBondOpponentIndex, j);
	        					connIndex++;
								}
							}
						}

					for (int i=0; i<connAtom[0].length; i++) {
						for (int j=0; j<connAtom[1].length; j++) {
							boolean isE = (mMol.getBondParity(bond) == Molecule.cBondParityEor1);
							if (connAtom[0].length == 2 && connAtom[0][i] > connAtom[0][1-i])
								isE = !isE;
							if (connAtom[1].length == 2 && connAtom[1][j] > connAtom[1][1-j])
								isE = !isE;
							setStereoBondDistance(connAtom[0][i], connAtom[1][j],
												  connBond[0][i], connBond[1][j],
												  connAngle[0][i], connAngle[1][j],
												  bond, isE);
							}
						}
					}

					// rotatable single bonds with one non-aromatic bond at each side
					// lookup dihedral angles and calculate associated distances
				else if (mMol.getBondOrder(bond) == 1) {
					int bondConnIndex1 = -1;
					for (int i=0; i<mMol.getConnAtoms(atom[0]); i++) {
					    if (mMol.getConnAtom(atom[0], i) == atom[1]) {
					        bondConnIndex1 = i;
					        break;
					    	}
						}
					for (int i=0; i<mMol.getConnAtoms(atom[0]); i++) {
					    int conn1 = mMol.getConnAtom(atom[0], i);
					    if (conn1 == atom[1])
					        continue;

						int bondConnIndex2 = -1;
						for (int j=0; j<mMol.getConnAtoms(atom[1]); j++) {
						    if (mMol.getConnAtom(atom[1], j) == atom[0]) {
						        bondConnIndex2 = j;
						        break;
						    	}
							}
					    for (int j=0; j<mMol.getConnAtoms(atom[1]); j++) {
						    int conn2 = mMol.getConnAtom(atom[1], j);
						    if (conn2 == atom[0])
						        continue;

							int[] dihedral = DihedralAngleKnowledgeBase.getKnowledgeBase().getDihedralAngles(mMol, bond, i, j);
							if (dihedral == null)
							    continue;

							int bond1 = mMol.getConnBond(atom[0], i);
							int bond2 = mMol.getConnBond(atom[1], j);

							float angle1 = mBondAngleSet.getConnAngle(atom[0], i, bondConnIndex1);
							float angle2 = mBondAngleSet.getConnAngle(atom[1], j, bondConnIndex2);

							float dx = mBondLengthSet.getLength(bond)
						              - mBondLengthSet.getLength(bond1)*(float)Math.cos(angle1)
						              - mBondLengthSet.getLength(bond2)*(float)Math.cos(angle2);
							float[] distance = new float[dihedral.length];
							for (int k=0; k<dihedral.length; k++) {
							    float dha = (float)Math.PI*(0.5f+(float)dihedral[k])/180.0f;
							    float dy = mBondLengthSet.getLength(bond2)*(float)Math.sin(angle2)*(float)Math.cos(dha)
										  - mBondLengthSet.getLength(bond1)*(float)Math.sin(angle1);
							    float dz = mBondLengthSet.getLength(bond2)*(float)Math.sin(angle2)*(float)Math.sin(dha);
							    distance[k] = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
								}
							if (conn1 < conn2)
							    mDistance[conn2][conn1] = distance;
							else
							    mDistance[conn1][conn2] = distance;
					    	}
					    }
					}
				}
			}

			// distances over 4 bonds in allenes
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
		    if (mMol.getAtomPi(atom) == 2
		     && mMol.getConnAtoms(atom) == 2
		     && mMol.getConnBondOrder(atom, 0) == 2
		     && mMol.getConnBondOrder(atom, 1) == 2) {
		        int atom1 = mMol.getConnAtom(atom, 0);
		        int atom2= mMol.getConnAtom(atom, 1);
		        for (int i=0; i<mMol.getConnAtoms(atom1); i++) {
		            int conn1 = mMol.getConnAtom(atom1, i);
		            if (conn1 != atom) {
				        for (int j=0; j<mMol.getConnAtoms(atom2); j++) {
				            int conn2 = mMol.getConnAtom(atom2, j);
				            if (conn2 != atom) {
								float angle1 = mBondAngleSet.getAngle(atom1, atom, conn1);
								float angle2 = mBondAngleSet.getAngle(atom2, atom, conn2);
								float bondLength1 = mBondLengthSet.getLength(mMol.getConnBond(atom1, i));
								float bondLength2 = mBondLengthSet.getLength(mMol.getConnBond(atom2, j));
							    float dx = mBondLengthSet.getLength(mMol.getConnBond(atom, 0))
							              + mBondLengthSet.getLength(mMol.getConnBond(atom, 1))
							              - bondLength1*(float)Math.cos(angle1)
										  - bondLength2*(float)Math.cos(angle2);
							    float dy = bondLength1*(float)Math.sin(angle1);
								float dz = bondLength2*(float)Math.sin(angle2);
								setFixedDistance(conn1, conn2, (float)Math.sqrt(dx*dx+dy*dy+dz*dz));
				            	}
					        }
		            	}
			        }
			    }
		    }

		for (int atom=0; atom<mMol.getAtoms(); atom++)
			calculateLongDistanceConstraints(atom);

		calculateDisconnectedDistanceConstraints();
		}

	private void calculateLongDistanceConstraints(int rootAtom) {
		int[] bondCount = new int[mMol.getAtoms()];
		int[] graphAtom = new int[mMol.getAtoms()];
		float[] distanceToRoot = new float[mMol.getAtoms()];

		graphAtom[0] = rootAtom;
		int current = 0;
		int highest = 0;
	 	while (current <= highest) {
			int parent = graphAtom[current];
			for (int i=0; i<mMol.getConnAtoms(parent); i++) {
				int candidate = mMol.getConnAtom(parent, i);
				if (bondCount[candidate] == 0 && candidate != rootAtom) {
					graphAtom[++highest] = candidate;
					bondCount[candidate] = bondCount[parent] + 1;

					if (bondCount[candidate] == 2)
						distanceToRoot[candidate] = (candidate < rootAtom) ?
													mDistance[rootAtom][candidate][0]
												  : mDistance[candidate][rootAtom][0];
						// distances with 3 or more bonds in between
					else if (bondCount[candidate] > 2) {
						distanceToRoot[candidate] = distanceToRoot[parent]
												  + mBondLengthSet.getLength(mMol.getConnBond(parent, i));

						if (candidate < rootAtom && mDistance[rootAtom][candidate] == null) {
						    mDistanceConstraintIsMinMax[rootAtom][candidate] = true;
						    mDistance[rootAtom][candidate] = new float[2];
							mDistance[rootAtom][candidate][0] = getVDWRadius(rootAtom) + getVDWRadius(candidate);
							mDistance[rootAtom][candidate][1] = distanceToRoot[candidate];

							if (bondCount[candidate] == 3/* && atomsShareSameRing(rootAtom, candidate)*/)
								mDistance[rootAtom][candidate][0] *= 0.75;	// take out internal strain of ring systems etc...
							}
						}
					}
				}
			current++;
			}
		}

	private void calculateDisconnectedDistanceConstraints() {
		for (int atom1=1; atom1<mMol.getAtoms(); atom1++) {
			for (int atom2=0; atom2<atom1; atom2++) {
				if (mDistance[atom1][atom2] == null) {
				    mDistanceConstraintIsMinMax[atom1][atom2] = true;
				    mDistance[atom1][atom2] = new float[2];
					mDistance[atom1][atom2][0] = getVDWRadius(atom1) + getVDWRadius(atom2);
					mDistance[atom1][atom2][1] = Float.MAX_VALUE;
					}
				}
			}
		}

    private float getVDWRadius(int atom) {
        int atomicNo = mMol.getAtomicNo(atom);
        return (atomicNo < VDWRadii.VDW_RADIUS.length) ? VDWRadii.VDW_RADIUS[atomicNo] : 2.0f;
        }

    private void calculatePlaneConstraints(ArrayList<ConformationConstraint> constraintList) {
		boolean[] isFlatBond = new boolean[mMol.getBonds()];
		int[] atomicNo = new int[2];
		for (int bond=0; bond<mMol.getBonds(); bond++) {
		    atomicNo[0] = mMol.getAtomicNo(mMol.getBondAtom(0, bond));
		    atomicNo[1] = mMol.getAtomicNo(mMol.getBondAtom(1, bond));
			isFlatBond[bond] = (mMol.isAromaticBond(bond)
							|| (mMol.getBondOrder(bond) > 1
							 && atomicNo[0] <= 8 && atomicNo[1] <= 8));
			if (!isFlatBond[bond]) {
					// check if bond is an amide or ester bond
				if (mMol.getBondOrder(bond) == 1) {
					for (int i=0; i<2; i++) {
						if ((atomicNo[i] == 7 || atomicNo[i] == 8) && atomicNo[1-i] == 6) {
							int carbon = mMol.getBondAtom(1-i, bond);
							for (int j=0; j<mMol.getConnAtoms(carbon); j++) {
								if (mMol.getConnBondOrder(carbon, j) == 2
								 && mMol.getAtomicNo(mMol.getConnAtom(carbon, j)) == 8) {
									isFlatBond[bond] = true;
									break;
									}
								}
							}
						}
					}
				}
			}

		int[] fragmentAtom = new int[mMol.getAtoms()];
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (isFlatBond[bond]) {
				fragmentAtom[0] = mMol.getBondAtom(0, bond);
				int count = getFlatFragmentAtoms(fragmentAtom, isFlatBond);
				int[] atomList = new int[count];
				for (int i=0; i<count; i++)
					atomList[i] = fragmentAtom[i];
				constraintList.add(new ConformationConstraint(ConformationConstraint.PLANE_CONSTRAINT, atomList));
				}
			}

			// the following are only flat if not colliding with other constraints
		boolean[] tryFlatBond = new boolean[mMol.getBonds()];
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (!mMol.isAromaticBond(bond)
			 && mMol.getBondOrder(bond) == 1) {
				for (int i=0; i<2; i++) {
					if (mMol.isAromaticAtom(mMol.getBondAtom(i, bond))
					 && (mMol.getAtomicNo(mMol.getBondAtom(1-i, bond)) == 7
					  || mMol.getAtomicNo(mMol.getBondAtom(1-i, bond)) == 8)) {
						tryFlatBond[bond] = true;
						}
					}
				}
			}

		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (tryFlatBond[bond]) {
				fragmentAtom[0] = mMol.getBondAtom(0, bond);
				int count = getFlatFragmentAtoms(fragmentAtom, tryFlatBond);
				int[] atomList = new int[count];
				for (int i=0; i<count; i++)
					atomList[i] = fragmentAtom[i];
				constraintList.add(new ConformationConstraint(ConformationConstraint.PLANE_CONSTRAINT_WEAK, atomList));
				}
			}
		}

	private void calculateStereoConstraints(ArrayList<ConformationConstraint> constraintList) {
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			int parity = mMol.getAtomParity(atom);
			if (parity == Molecule.cAtomParity1 || parity == Molecule.cAtomParity2) {
				int[] atomList = new int[5];
				for (int i=0; i<mMol.getConnAtoms(atom); i++) {
					int connAtom = mMol.getConnAtom(atom, i);
					int index = 0;
					while (index < i && connAtom > atomList[index])
						index++;
					for (int j=i-1; j>=index; j--)
						atomList[j+1] = atomList[j];
					atomList[index] = connAtom;
					}
				if (mMol.getConnAtoms(atom) == 3) {
					atomList[3] = atom;
					atomList[4] = -1;
					}
				else {
					atomList[4] = atom;
					}

				if (parity == Molecule.cAtomParity1) {
					int temp = atomList[0];
					atomList[0] = atomList[1];
					atomList[1] = temp;
					}

				constraintList.add(new ConformationConstraint(ConformationConstraint.STEREO_CONSTRAINT, atomList));
				}
			}
		}

	private void setStereoBondDistance(int atom1, int atom2,
									   int bond1, int bond2,
									   float angle1, float angle2,
									   int floatBond, boolean isE) {
	    float s1 = mBondLengthSet.getLength(floatBond)
	              - mBondLengthSet.getLength(bond1) * (float)Math.cos(angle1)
	              - mBondLengthSet.getLength(bond2) * (float)Math.cos(angle2);
	    float s2 = mBondLengthSet.getLength(bond1) * (float)Math.sin(angle1);
	    if (isE)
	        s2 += mBondLengthSet.getLength(bond2) * Math.sin(angle2);
	    else
	        s2 -= mBondLengthSet.getLength(bond2) * Math.sin(angle2);
		setFixedDistance(atom1, atom2, (float)Math.sqrt(s1*s1+s2*s2));
	    
	    
/*      float s1 = mBondLength[floatBond] + (mBondLength[bond1] + mBondLength[bond2]) / 2.0;
		float s2 = (isE) ? (mBondLength[bond1] + mBondLength[bond2]) * 0.86602540378
						  : (mBondLength[bond1] - mBondLength[bond2]) * 0.86602540378;
		setFixedDistance(atom1, atom2, Math.sqrt(s1*s1+s2*s2));*/
		}

	private void setFixedDistance(int atom1, int atom2, float distance) {
	    if (atom1 < atom2) {
	        int temp = atom1;
	        atom1 = atom2;
	        atom2 = temp;
	    	}

	    if (mDistance[atom1][atom2] == null) {
	        mDistance[atom1][atom2] = new float[1];
	        mDistance[atom1][atom2][0] = distance;
	    	}
		}

	private int getFlatFragmentAtoms(int[] fragmentAtom, boolean[] isFlatBond) {
			// locate all atoms connected directly via flat bonds
		boolean[] isFragmentMember = new boolean[mMol.getAtoms()];
		isFragmentMember[fragmentAtom[0]] = true;
		int current = 0;
		int highest = 0;
	 	while (current <= highest && mMol.getAtomPi(fragmentAtom[current]) < 2) {
			for (int i=0; i<mMol.getConnAtoms(fragmentAtom[current]); i++) {
				int candidateAtom = mMol.getConnAtom(fragmentAtom[current], i);
				int candidateBond = mMol.getConnBond(fragmentAtom[current], i);
				if (isFlatBond[candidateBond]) {
					if (!isFragmentMember[candidateAtom]) {
						fragmentAtom[++highest] = candidateAtom;
						isFragmentMember[candidateAtom] = true;
						}
					isFlatBond[candidateBond] = false;
					}
				}
			current++;
			}

			// attach first sphere of atoms connected via non-flat bonds
		for (int i=highest; i>=0; i--) {
			for (int j=0; j<mMol.getConnAtoms(fragmentAtom[i]); j++) {
				int connAtom = mMol.getConnAtom(fragmentAtom[i], j);
				if (!isFragmentMember[connAtom]) {
					fragmentAtom[++highest] = connAtom;
					isFragmentMember[connAtom] = true;
					}
				}	
			}

		return highest+1;
		}

	private void calculateLineConstraints(ArrayList<ConformationConstraint> constraintList) {
	    boolean[] atomHandled = new boolean[mMol.getAtoms()];
		int[] fragmentAtom = new int[mMol.getAtoms()];
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (!atomHandled[atom] && mMol.getAtomPi(atom) == 2 && mMol.getAtomicNo(atom) <= 8) {
				fragmentAtom[0] = atom;
				int count = getLineFragmentAtoms(fragmentAtom);
				int[] atomList = new int[count];
				for (int i=0; i<count; i++) {
					atomList[i] = fragmentAtom[i];
					atomHandled[fragmentAtom[i]] = true;
					}
				constraintList.add(new ConformationConstraint(ConformationConstraint.LINE_CONSTRAINT, atomList));
				}
			}
		}

	private int getLineFragmentAtoms(int[] fragmentAtom) {
			// locate all atoms connected directly via flat bonds
		boolean[] isFragmentMember = new boolean[mMol.getAtoms()];
		isFragmentMember[fragmentAtom[0]] = true;
		int current = 0;
		int highest = 0;
	 	while (current <= highest) {
			for (int i=0; i<mMol.getConnAtoms(fragmentAtom[current]); i++) {
				int candidate = mMol.getConnAtom(fragmentAtom[current], i);
				if (mMol.getAtomPi(candidate) == 2
				 && mMol.getAtomicNo(candidate) <= 8
				 && !isFragmentMember[candidate]) {
					fragmentAtom[++highest] = candidate;
					isFragmentMember[candidate] = true;
					}
				}
			current++;
			}

			// attach first sphere of atoms connected via non-flat bonds
		for (int i=highest; i>=0; i--) {
			for (int j=0; j<mMol.getConnAtoms(fragmentAtom[i]); j++) {
				int connAtom = mMol.getConnAtom(fragmentAtom[i], j);
				if (!isFragmentMember[connAtom]) {
					fragmentAtom[++highest] = connAtom;
					isFragmentMember[connAtom] = true;
					}
				}	
			}

		return highest+1;
		}


	private float getMinDistance(int atom1, int atom2) {
		return (atom1 < atom2) ? mDistance[atom2][atom1][0] : mDistance[atom1][atom2][0];
		}

	private class ThreadData {
		public float[] atomStrain;
		public boolean[] isConstraintDisabled;
		public Random random;

		ThreadData(long randomSeed) {
			isConstraintDisabled = new boolean[mConstraint.length];
			random = (randomSeed == 0) ? new Random() : new Random(randomSeed);
			}
		}

	public class ConformerData {
		public float[] x,y,z;

		ConformerData(StereoMolecule mol) {
			x = new float[mol.getAtoms()];
			y = new float[mol.getAtoms()];
			z = new float[mol.getAtoms()];
			}
		}

	private class ConformationConstraint {
		public static final int PLANE_CONSTRAINT = 1;
		public static final int PLANE_CONSTRAINT_WEAK = 2;
		public static final int LINE_CONSTRAINT = 3;
		public static final int STEREO_CONSTRAINT = 4;
	
		public int[]	atomList;
		public int		type;
	
		ConformationConstraint(int type, int[] atomList) {
			this.type = type;
			this.atomList = atomList;
			}
		}
	}
