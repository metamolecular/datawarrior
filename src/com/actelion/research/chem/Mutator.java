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

import java.util.ArrayList;
import java.util.Random;

public class Mutator {
	public static final int MUTATION_GROW = Mutation.MUTATION_ADD_ATOM
										  | Mutation.MUTATION_INSERT_ATOM;
	public static final int MUTATION_SHRINK = Mutation.MUTATION_CUTOUT_ATOM
											| Mutation.MUTATION_DELETE_ATOM
											| Mutation.MUTATION_DELETE_SUBSTITUENT
											| Mutation.MUTATION_CUTOUT_SFRAGMENT;
	public static final int MUTATION_KEEP_SIZE = Mutation.MUTATION_CHANGE_ATOM
											   | Mutation.MUTATION_ADD_BOND
											   | Mutation.MUTATION_CHANGE_BOND
											   | Mutation.MUTATION_DELETE_BOND
											   | Mutation.MUTATION_CHANGE_RING
											   | Mutation.MUTATION_MIGRATE
											   | Mutation.MUTATION_SWAP_SUBSTITUENT;

	public static final int MUTATION_ANY = MUTATION_GROW
										 | MUTATION_SHRINK
										 | MUTATION_KEEP_SIZE;

	private static final int cDefaultMinAtoms = 4;
	private static final int cDefaultOptAtoms = 9;
	private static final int cDefaultMaxAtoms = 24;
	private static final float cRingBoost = 100.0f;
	private static final float cGrowBoost = 20.0f;
	private static final float cDefaultEductFrequency = 0.1f;

	private Random mRandom;

	private StereoMolecule	    mMolCopy 	= new StereoMolecule();
	private StereoMolecule	    mMol		= new StereoMolecule();
	private AtomTypeList		mAtomTypeList;
    private Canonizer           mCanonizer;
    private boolean[]           mIsSymmetricAtom;
	private Mutation			mMostRecentMutation;
	private int					mMinAtoms,mOptAtoms,mMaxAtoms;

	/**
	 * Creates a new Mutator that calculates propabilities of individual mutations from
	 * a given atom type file. For every potential mutation the algorithm calculates
	 * a probability considering the frequencies of broken and formed atom types during the conversion.<br>
	 * If no type list is given, all possible mutations are considered equally likely.<br>
	 * New type files can be created from an sdf or dwar file like this:<br>
	 * <i>new AtomTypeList().create("/somepath/chembl14.dwar", AtomTypeCalculator.cPropertiesForMutator);<i><br>
	 * This would cause a file <i>/somepath/chembl14.typ</i> to be created with the statistics taken from <i>chembl14.dwar</i>.
	 * @param filename null or name of a atomTypeList file ('*.typ')
	 */
	public Mutator(String filename) {
		if (filename != null) {
	        try {
	            mAtomTypeList = new AtomTypeList(filename, AtomTypeCalculator.cPropertiesForMutator);
	            }
	        catch (Exception e) {
	            e.printStackTrace();
	            }
	        }

	    mRandom = new Random();

		mMinAtoms = cDefaultMinAtoms;
		mOptAtoms = cDefaultOptAtoms;
		mMaxAtoms = cDefaultMaxAtoms;
		}

	public void setPreferredSize(int minAtoms, int preferredAtoms, int maxAtoms) {
		mMinAtoms = minAtoms;
		mOptAtoms = preferredAtoms;
		mMaxAtoms = maxAtoms;
		}

	public StereoMolecule[] getMutatedSet(StereoMolecule mol,
                                          int mutationType,
                                          boolean regulateSize,
                                          int count) {
	    ArrayList<Mutation> mutationList = generateMutationList(mol, mutationType, regulateSize);

        if (count > mutationList.size())
	        count = mutationList.size();

        StereoMolecule[] set = new StereoMolecule[count];
	    for (int i=0; i<count; i++) {
	        set[i] = new StereoMolecule(mol);
	        mutate(set[i], mutationList);
	        }
	    return set;
	    }

	/**
	 * Does an in-place mutation of the molecule
	 * allowing any kind of mutation
	 * at any of the molecules non-selected atoms
	 * aiming for 4-24 non-H atoms with an optimum of 9 atoms.
	 * @param mol
     * @return list of all not-used mutations
	 */
	public ArrayList<Mutation> mutate(StereoMolecule mol) {
		return mutate(mol, MUTATION_ANY, true);
		}

	public ArrayList<Mutation> mutate(StereoMolecule mol,
	                                  int mutationType,
	                                  boolean regulateSize) {
	    ArrayList<Mutation> ml = generateMutationList(mol, mutationType, regulateSize);

	    if (ml.size() == 0) {
	        System.out.println("no possible mutation found");
	        return ml;
	        }

	    mutate(mol, ml);
	    return ml;
	    }

	/**
     * Selects a likely mutation from the list,
     * performs the mutation and removes it from the list.
     * @param mol
     * @param mutationList
     */
    public void mutate(StereoMolecule mol, ArrayList<Mutation> mutationList) {
        mMostRecentMutation = selectLikelyMutation(mutationList);
        performMutation(mol, mMostRecentMutation);
        mutationList.remove(mMostRecentMutation);
        }

	/**
	 * Creates a list of possible mutations and their probabilities
	 * @param mol
	 * @param mutationType flag list of allowed mutations types
	 * @param regulateSize if true keeps non-H atoms between 4 and 24 with an optimum at 9.
	 */
	public ArrayList<Mutation> generateMutationList(StereoMolecule mol,
	                                                int mutationType,
	                                                boolean regulateSize) {
//for (int atom=0; atom<mol.getAtoms(); atom++)
//System.out.println("frequency("+atom+")="+getFrequency(mol,atom));
	    mMol = mol;

	    detectSymmetry();

		ArrayList<Mutation> mutationList = new ArrayList<Mutation>();
	    ArrayList<MutatorSubstituent> substituentList = createSubstituentList();

		if (!regulateSize
		 || (mRandom.nextDouble() > (float)(mMol.getAtoms() - mOptAtoms)
		 						  / (float)(mMaxAtoms - mOptAtoms))) {
			if ((mutationType & Mutation.MUTATION_ADD_ATOM) != 0)
				addPropabilitiesForAddAtom(mutationList);
			if ((mutationType & Mutation.MUTATION_INSERT_ATOM) != 0)
				addPropabilitiesForInsertAtom(mutationList);
			}

		addPropabilitiesForChangeAtom(mutationList);

		if (!regulateSize
		 || (mRandom.nextDouble() < (float)(mMol.getAtoms() - mMinAtoms)
		 						  / (float)(mOptAtoms - mMinAtoms))) {
			if ((mutationType & Mutation.MUTATION_DELETE_ATOM) != 0)
				addPropabilitiesForDeleteAtom(mutationList);
			if ((mutationType & Mutation.MUTATION_CUTOUT_ATOM) != 0)
				addPropabilitiesForCutOutAtom(mutationList);
            if ((mutationType & Mutation.MUTATION_DELETE_SUBSTITUENT) != 0)
                addPropabilitiesForDeleteSubstituent(mutationList, substituentList);
	        if ((mutationType & Mutation.MUTATION_CUTOUT_SFRAGMENT) != 0)
	            addPropabilitiesForCutOutFragment(mutationList);
			}

		if ((mutationType & Mutation.MUTATION_ADD_BOND) != 0)
			addPropabilitiesForAddBond(mutationList);
		if ((mutationType & Mutation.MUTATION_CHANGE_BOND) != 0)
			addPropabilitiesForChangeBond(mutationList);
		if ((mutationType & Mutation.MUTATION_DELETE_BOND) != 0)
			addPropabilitiesForDeleteBond(mutationList);
		if ((mutationType & Mutation.MUTATION_CHANGE_RING) != 0)
			addPropabilitiesForChangeRing(mutationList);
		if ((mutationType & Mutation.MUTATION_MIGRATE) != 0)
			addPropabilitiesForMigrate(mutationList);
        if ((mutationType & Mutation.MUTATION_SWAP_SUBSTITUENT) != 0)
            addPropabilitiesForSwapSubstituent(mutationList, substituentList);

		return mutationList;
	    }


	public Mutation getMostRecentMutation() {
		return mMostRecentMutation;
		}


	private void detectSymmetry() {
        mCanonizer = new Canonizer(mMol, Canonizer.CREATE_SYMMETRY_RANK);
        mIsSymmetricAtom = new boolean[mMol.getAtoms()];

        int[] rankCount = new int[1+mMol.getAtoms()];
        boolean[] unselectedAtomFound = new boolean[1+mMol.getAtoms()];
        for (int atom=0; atom<mMol.getAtoms(); atom++) {
            int rank = mCanonizer.getSymmetryRank(atom);
            rankCount[rank]++;
            if (!mMol.isSelectedAtom(atom))
                unselectedAtomFound[rank] = true;
            }

        boolean[] primaryAtomFound = new boolean[1+mMol.getAtoms()];
        for (int atom=0; atom<mMol.getAtoms(); atom++) {
            int rank = mCanonizer.getSymmetryRank(atom);
            if (rankCount[rank] > 1) {
                if (primaryAtomFound[rank]) {
                    mIsSymmetricAtom[atom] = true;
                    }
                else if (mMol.isSelectedAtom(atom)
                      && unselectedAtomFound[rank]) {
                    mIsSymmetricAtom[atom] = true;
                    }
                else {
                    primaryAtomFound[rank] = true;
                    }
                }
            }
	    }


	private void addPropabilitiesForAddAtom(ArrayList<Mutation> mutationList){
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (mIsSymmetricAtom[atom] || mMol.isSelectedAtom(atom))
				continue;

			int freeValence = mMol.getFreeValence(atom);
			int maxBondOrder = (freeValence > 3) ? 3 : freeValence;
			if( maxBondOrder > 0) {
				mMol.copyMolecule(mMolCopy);
				int newAtom = mMolCopy.addAtom(6);
				int newBond = mMolCopy.addBond(atom, newAtom, Molecule.cBondTypeSingle);

				//loop for single float or triple bond to add 
				for (int bondOrder=1; bondOrder<=maxBondOrder; bondOrder++) {
					//check if the atom to add has enough bonds
					boolean allowed = false;
					for (int i=0; i<Mutation.cAllowedAtomicNo[bondOrder-1].length; i++) {
						if (Mutation.cAllowedAtomicNo[bondOrder-1][i] == mMol.getAtomicNo(atom)) {
							allowed = true;
							break;
							}
						}
					if (!allowed)
						break;

					for (int i=0; i<Mutation.cAllowedAtomicNo[bondOrder-1].length;i++) {
						float f_Edukt = getFrequency(mMol, atom);
						if (f_Edukt == 0.0f)
							f_Edukt = cDefaultEductFrequency;

						int atomicNo	= Mutation.cAllowedAtomicNo[bondOrder-1][i];
						int bondType	= getBondTypeFromOrder(bondOrder);
						mMolCopy.setAtomicNo(newAtom, atomicNo);
						mMolCopy.setBondType(newBond, bondType);
						mMolCopy.ensureHelperArrays(Molecule.cHelperRings);
						float f_Produkt = getFrequency(mMolCopy, atom);
						float p = cGrowBoost * f_Produkt / f_Edukt;
						if (p > 0.0 && isValidStructure(mMolCopy))
						    mutationList.add(new Mutation(Mutation.MUTATION_ADD_ATOM,
						                                  atom,
						                                  -1,
						                                  atomicNo,
						                                  bondType,
						                                  p));
						}
					}
				}
			}
		}

	private void addPropabilitiesForInsertAtom(ArrayList<Mutation> mutationList) {
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (mMol.getBondType(bond) == Molecule.cBondTypeSingle
			 && !mMol.isAromaticBond(bond)) {
				int atom1 = mMol.getBondAtom(0, bond);
				int atom2 = mMol.getBondAtom(1, bond);

				if (mMol.isSelectedAtom(atom1) || mMol.isSelectedAtom(atom2))
					continue;

				if ((mIsSymmetricAtom[atom1] ^ mIsSymmetricAtom[atom2])
				 && mCanonizer.getSymmetryRank(atom1) != mCanonizer.getSymmetryRank(atom2))
				    continue;
                if (mIsSymmetricAtom[atom1]
                 && mIsSymmetricAtom[atom2])
                    continue;

				float f_Edukt1 = getFrequency(mMol, atom1);
				float f_Edukt2 = getFrequency(mMol, atom2);
				if (f_Edukt1 == 0)
					f_Edukt1 = cDefaultEductFrequency;
				if (f_Edukt2 == 0)
					f_Edukt2 = cDefaultEductFrequency;

				mMol.copyMolecule(mMolCopy);
				mMolCopy.deleteBond(bond);
				int newAtom = mMolCopy.addAtom(6);
				mMolCopy.addBond(atom1, newAtom, Molecule.cBondTypeSingle);
				mMolCopy.addBond(atom2, newAtom, Molecule.cBondTypeSingle);

				for (int i=0; i<Mutation.cAllowedAtomicNo[1].length;i++) {
					int atomicNo = Mutation.cAllowedAtomicNo[1][i];
					mMolCopy.setAtomicNo(newAtom, atomicNo);

					float f_Produkt1 = getFrequency(mMolCopy, atom1);
					float f_Produkt2 = getFrequency(mMolCopy, atom2);

					float p = (float)Math.sqrt(f_Produkt1 * f_Produkt2 / f_Edukt1 / f_Edukt2);
                    if (p > 0.0 && isValidStructure(mMolCopy))
                        mutationList.add(new Mutation(Mutation.MUTATION_INSERT_ATOM,
													  bond,
													  -1,
													  atomicNo,
													  -1,
													  p));
					}
				}
			}
		}


	private void addPropabilitiesForChangeAtom(ArrayList<Mutation> mutationList) {
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (mMol.isSelectedAtom(atom) || mIsSymmetricAtom[atom])
				continue;

			int maxBondOrder = 1;
			for (int i=0; i<mMol.getConnAtoms(atom); i++)
				if (maxBondOrder < mMol.getConnBondOrder(atom, i))
					maxBondOrder = mMol.getConnBondOrder(atom, i);

			for (int i=0; i<Mutation.cAllowedAtomicNo[maxBondOrder-1].length; i++) {
				int proposedAtomicNo = Mutation.cAllowedAtomicNo[maxBondOrder-1][i];
				int valences = mMol.getConnAtoms(atom) + mMol.getAtomPi(atom);

				if (mMol.getAtomicNo(atom) == proposedAtomicNo)
					continue;

				if (valences > 1
				 && (proposedAtomicNo == 9
				  || proposedAtomicNo == 17
				  || proposedAtomicNo == 35
				  || proposedAtomicNo == 53))
					continue;

				if (valences > 2
				 && proposedAtomicNo == 8)
					continue;

				if (valences > 3
				 && proposedAtomicNo == 5)
					continue;

				if (valences > 4
				 && (proposedAtomicNo == 6
				  || proposedAtomicNo == 7))
					continue;

				if (valences > 5
				 && proposedAtomicNo == 15)
					continue;

				mMol.copyMolecule(mMolCopy);
				mMolCopy.setAtomicNo(atom, proposedAtomicNo);
				mMolCopy.ensureHelperArrays(Molecule.cHelperRings);

				float currentFrequency = getFrequency(mMol,atom);
				if (currentFrequency == 0.0)
					currentFrequency = cDefaultEductFrequency;

				float mutatedFrequency = getFrequency(mMolCopy,atom);

				for (int j=0; j<mMol.getConnAtoms(atom); j++) {
					int connAtom = mMol.getConnAtom(atom, j);
					float frequency = getFrequency(mMol, connAtom);
					if (frequency != 0.0)
						currentFrequency *= frequency;

					mutatedFrequency *= getFrequency(mMolCopy,connAtom);
					}

				float p = (float)Math.pow(mutatedFrequency / currentFrequency,
						 			1.0f / (float)(1f + mMol.getConnAtoms(atom)));
                if (p > 0.0 && isValidStructure(mMolCopy))
                    mutationList.add(new Mutation(Mutation.MUTATION_CHANGE_ATOM,
                                                  atom,
                                                  -1,
                                                  proposedAtomicNo,
                                                  -1,
                                                  p));
				}
			}		
		}


	private void addPropabilitiesForDeleteAtom(ArrayList<Mutation> mutationList) {
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (mMol.isSelectedAtom(atom) || mIsSymmetricAtom[atom])
				continue;

			if (mMol.getConnAtoms(atom)==1
			 || (mMol.isRingAtom(atom)
			  && mMol.getConnAtoms(atom)==2)) {
				float p = 1.0f;

				mMol.copyMolecule(mMolCopy);
				for(int j=0;j<mMol.getConnAtoms(atom);j++)
					mMolCopy.deleteBond(mMol.getConnBond(atom,j));

				mMolCopy.ensureHelperArrays(Molecule.cHelperRings);
				for(int j=0;j<mMol.getConnAtoms(atom);j++) {
					int connAtom = mMol.getConnAtom(atom,j);
					float f_Edukt = getFrequency(mMol, connAtom);
					if (f_Edukt == 0)
						f_Edukt = cDefaultEductFrequency;
					float f_Produkt = getFrequency(mMolCopy, connAtom);
					p *= (f_Produkt / f_Edukt);
					}

				p = (float)Math.pow(p, 1.0f / (float)mMol.getConnAtoms(atom));
                if (p > 0.0 && isValidStructure(mMolCopy))
                    mutationList.add(new Mutation(Mutation.MUTATION_DELETE_ATOM,
												  atom,
                                                  -1,
												  -1,
												  -1,
												  p));
				}
			}
		}


	private void addPropabilitiesForCutOutAtom(ArrayList<Mutation> mutationList){
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (mMol.isSelectedAtom(atom) || mIsSymmetricAtom[atom])
				continue;

			if (!mMol.isAromaticAtom(atom)
			 && mMol.getConnAtoms(atom)==2) {
				int atom1 = mMol.getConnAtom(atom, 0);
				int atom2 = mMol.getConnAtom(atom, 1);

				float f_Edukt1 = getFrequency(mMol, atom1);
				float f_Edukt2 = getFrequency(mMol, atom2);

				mMol.copyMolecule(mMolCopy);
				mMolCopy.addBond(atom1, atom2, Molecule.cBondTypeSingle);
				mMolCopy.deleteAtom(atom);

				int newBond = mMolCopy.getAllBonds() - 1;
				atom1 = mMolCopy.getBondAtom(0, newBond);
				atom2 = mMolCopy.getBondAtom(1, newBond);

				float f_Produkt1 = getFrequency(mMolCopy, atom1);
				float f_Produkt2 = getFrequency(mMolCopy, atom2);

				if (f_Edukt1 == 0)
					f_Edukt1 = cDefaultEductFrequency;
				if (f_Edukt2 == 0)
					f_Edukt2 = cDefaultEductFrequency;

				float p = (float)Math.sqrt(f_Produkt1 * f_Produkt2 / (f_Edukt1 * f_Edukt2));

                if (p > 0.0 && isValidStructure(mMolCopy))
                    mutationList.add(new Mutation(Mutation.MUTATION_CUTOUT_ATOM,
												  atom,
                                                  -1,
												  -1,
												  -1,
												  p));
				}
			}
		}//end_deleting


	private void addPropabilitiesForAddBond(ArrayList<Mutation> mutationList) {
		for (int atom1=0; atom1<mMol.getAtoms(); atom1++) {
			if (mMol.isSelectedAtom(atom1) || mIsSymmetricAtom[atom1])
				continue;

			int graphAtom[] = new int[mMol.getAtoms()];
			int graphBond[] = new int[mMol.getAtoms()];
			int graphParent[] = new int[mMol.getAtoms()];
			int graphLevel[] = new int[mMol.getAtoms()];
			graphAtom[0] = atom1;
			graphLevel[atom1] = 1;
			int current = 0;
			int highest = 0;
		 	while (current <= highest) {
				if (graphLevel[graphAtom[current]] > 6)
					break;

				for (int i=0; i<mMol.getConnAtoms(graphAtom[current]); i++) {
					int candidate = mMol.getConnAtom(graphAtom[current],i);
					if (graphLevel[candidate] == 0) {
						graphParent[candidate] = graphAtom[current];
						graphLevel[candidate] = graphLevel[graphAtom[current]] + 1;
						graphBond[candidate] = mMol.getConnBond(graphAtom[current],i);
						graphAtom[++highest] = candidate;
						}
					}
				current++;
				}

			for (int atom2=atom1+1; atom2<mMol.getAtoms(); atom2++) {
				if (mMol.isSelectedAtom(atom2))
					continue;
				if (mIsSymmetricAtom[atom2]
				 && mCanonizer.getSymmetryRank(atom1) != mCanonizer.getSymmetryRank(atom2))
				    continue;

				if (graphLevel[atom2] >= 3 && graphLevel[atom2] <= 7) {
					boolean isClosureBond = false;
					for (int i=0; i<mMol.getConnAtoms(atom1); i++) {
						if (atom2 == mMol.getConnAtom(atom1, i)) {
							isClosureBond = true;
							}
						}
					if (isClosureBond)
						continue;

					if (!qualifiesForRing(graphBond, graphLevel, graphParent, atom1, atom2))
						continue;

					for (int order=1; order<=2; order++) {
						if (mMol.getFreeValence(atom1) < order
						 || mMol.getFreeValence(atom2) < order)
							break;

						mMol.copyMolecule(mMolCopy);
						mMolCopy.addBond(atom1, atom2, getBondTypeFromOrder(order));

						float f_Edukt1 = getFrequency(mMol, atom1);
						float f_Edukt2 = getFrequency(mMol, atom2);
						float f_Produkt1 = getFrequency(mMolCopy, atom1);
						float f_Produkt2 = getFrequency(mMolCopy, atom2);

						if (f_Edukt1 == 0.0)
							f_Edukt1 = cDefaultEductFrequency;
						if (f_Edukt2 == 0.0)
							f_Edukt2 = cDefaultEductFrequency;

						float p = (mAtomTypeList == null) ? 1.0f : mAtomTypeList.getRingSizeAdjust(graphLevel[atom2]);
						p *= cRingBoost * Math.sqrt(f_Produkt1 * f_Produkt2 / f_Edukt1 / f_Edukt2);

                        if (p > 0.0 && isValidStructure(mMolCopy))
                            mutationList.add(new Mutation(Mutation.MUTATION_ADD_BOND,
                                                          atom1,
                                                          atom2,
                                                          getBondTypeFromOrder(order),
                                                          -1,
                                                          p));
						}
					}
				}
			}
		}


	private void addPropabilitiesForChangeBond(ArrayList<Mutation> mutationList) {
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			int atom1 = mMol.getBondAtom(0, bond);
			int atom2 = mMol.getBondAtom(1, bond);

			if (mMol.isSelectedAtom(atom1) || mMol.isSelectedAtom(atom2))
				continue;

            if ((mIsSymmetricAtom[atom1] ^ mIsSymmetricAtom[atom2])
             && mCanonizer.getSymmetryRank(atom1) != mCanonizer.getSymmetryRank(atom2))
                continue;
            if (mIsSymmetricAtom[atom1]
             && mIsSymmetricAtom[atom2])
                continue;

			int minFreeValence = mMol.getFreeValence(atom1);
			if (minFreeValence > mMol.getFreeValence(atom2))
				minFreeValence = mMol.getFreeValence(atom2);
			int maxBondOrder = mMol.getBondOrder(bond) + minFreeValence;

			if (maxBondOrder > 3)
				maxBondOrder = 3;

			if (mMol.isAromaticBond(bond)) {
			    if (mMol.getBondOrder(bond) == 1)
			        continue;
				maxBondOrder = 2;
			    }

			if (mMol.isSmallRingBond(bond)) {
				if (mMol.getBondOrder(bond) == 1
				 && mMol.getAtomPi(atom1)+mMol.getAtomPi(atom2) != 0)
					maxBondOrder = 1;
				else if (maxBondOrder > 2)
					maxBondOrder = 2;
				}

			if (maxBondOrder == 2
			 && (mMol.getAtomicNo(atom1) < 5
			  || (mMol.getAtomicNo(atom1) > 8
			   && mMol.getAtomicNo(atom1) != 15
			   && mMol.getAtomicNo(atom1) != 16)
			  || mMol.getAtomicNo(atom2) < 5
			  || (mMol.getAtomicNo(atom2) > 8
			   && mMol.getAtomicNo(atom2) != 15
			   && mMol.getAtomicNo(atom2) != 16)))
				maxBondOrder = 1;

			for (int bondOrder=1; bondOrder<=maxBondOrder; bondOrder++) {
				if (bondOrder == mMol.getBondOrder(bond))
					continue;

				mMol.copyMolecule(mMolCopy);
				mMolCopy.setBondType(bond, getBondTypeFromOrder(bondOrder));

				float f_Edukt1 = getFrequency(mMol, atom1);
				float f_Edukt2 = getFrequency(mMol, atom2);
				float f_Produkt1 = getFrequency(mMolCopy, atom1);
				float f_Produkt2 = getFrequency(mMolCopy, atom2);
				if (f_Edukt1 == 0.0)
					f_Edukt1 = cDefaultEductFrequency;
				if (f_Edukt2 == 0.0)
					f_Edukt2 = cDefaultEductFrequency;

				float p = (float)Math.sqrt(f_Produkt1 * f_Produkt2 / (f_Edukt1 * f_Edukt2));

                if (p > 0.0 && isValidStructure(mMolCopy))
                    mutationList.add(new Mutation(Mutation.MUTATION_CHANGE_BOND,
												  bond,
												  -1,
												  getBondTypeFromOrder(bondOrder),
												  -1,
												  p));
				}
			}
		}


	private void addPropabilitiesForDeleteBond(ArrayList<Mutation> mutationList) {
		for (int bond=0; bond<mMol.getBonds(); bond++) {
			if (mMol.isRingBond(bond)) {
				int atom1 = mMol.getBondAtom(0, bond);
				int atom2 = mMol.getBondAtom(1, bond);

				if (mMol.isSelectedAtom(atom1) || mMol.isSelectedAtom(atom2))
					continue;

	            if ((mIsSymmetricAtom[atom1] ^ mIsSymmetricAtom[atom2])
                 && mCanonizer.getSymmetryRank(atom1) != mCanonizer.getSymmetryRank(atom2))
	                continue;
	            if (mIsSymmetricAtom[atom1]
	             && mIsSymmetricAtom[atom2])
	                continue;

				mMol.copyMolecule(mMolCopy);
				mMolCopy.deleteBond(bond);

				float f_Edukt1 = getFrequency(mMol, atom1);
				float f_Edukt2 = getFrequency(mMol, atom2);
				float f_Produkt1 = getFrequency(mMolCopy, atom1);
				float f_Produkt2 = getFrequency(mMolCopy, atom2);

				if (f_Edukt1 == 0.0)
					f_Edukt1 = cDefaultEductFrequency;
				if (f_Edukt2 == 0.0)
					f_Edukt2 = cDefaultEductFrequency;

				float p = (float)Math.sqrt(f_Produkt1 * f_Produkt2 / f_Edukt1 / f_Edukt2);

                if (p > 0.0 && isValidStructure(mMolCopy))
                    mutationList.add(new Mutation(Mutation.MUTATION_DELETE_BOND,
												  bond,
												  -1,
                                                  -1,
												  -1,
												  p));
				}
			}
		}


	private void addPropabilitiesForChangeRing(ArrayList<Mutation> mutationList) {
		mMol.ensureHelperArrays(Molecule.cHelperRings);
		RingCollection ringSet = mMol.getRingSet();
		for (int ring=0; ring<ringSet.getSize(); ring++) {
			int ringSize = ringSet.getRingSize(ring);
			if (ringSize == 5 || ringSize == 6) {
				int[] ringAtom = ringSet.getRingAtoms(ring);

				int symmetricMemberCount = 0;
				boolean fixedAtomFound = false;
				for (int i=0; i<ringSize; i++) {
					if (mMol.isSelectedAtom(ringAtom[i]))
						fixedAtomFound = true;
					if (mIsSymmetricAtom[ringAtom[i]])
					    symmetricMemberCount++;
					}
				if (fixedAtomFound)
					continue;
				if (symmetricMemberCount >= ringSize/2)  // reasonable approximation
				    continue;

				int[] ringBond = ringSet.getRingBonds(ring);
				if (hasExocyclicPiBond(ringAtom, ringBond))
					continue;

				for (int heteroPosition=0; heteroPosition<ringSize; heteroPosition++) {
					if (heteroPosition > 0
					 && (ringSize == 6
					  || ringSet.isAromatic(ring)))
						break;

					if (ringSize == 5
					 && mMol.getAtomicNo(ringAtom[heteroPosition]) != 7
					 && mMol.getAtomicNo(ringAtom[heteroPosition]) != 8
					 && mMol.getAtomicNo(ringAtom[heteroPosition]) != 16)
						continue;

					mMol.copyMolecule(mMolCopy);
					mMolCopy.ensureHelperArrays(Molecule.cHelperRings);
					if (!changeAromaticity(mMolCopy, ring, heteroPosition))
						continue;

					float p = 1.0f;
					for (int atom=0; atom<ringSize; atom++) {
						float f_Edukt = getFrequency(mMol, atom);
						float f_Produkt = getFrequency(mMolCopy, atom);
						if (f_Edukt == 0.0)
							f_Edukt = cDefaultEductFrequency;
						p *= f_Produkt / f_Edukt;
						}
					p = (float)Math.pow(p, 1.0 / (float)ringSize);
                    if (p > 0.0 && isValidStructure(mMolCopy))
                        mutationList.add(new Mutation(Mutation.MUTATION_CHANGE_RING,
                                                      ring,
                                                      -1,
                                                      heteroPosition,
                                                      -1,
                                                      p));
					}
				}
			}
		}


	private void addPropabilitiesForMigrate(ArrayList<Mutation> mutationList) {
		for (int atom=0; atom<mMol.getAtoms(); atom++) {
			if (!mIsSymmetricAtom[atom] && !mMol.isSelectedAtom(atom) && mMol.getConnAtoms(atom) > 2) {
				for (int i=0; i<mMol.getConnAtoms(atom); i++) {
					int sourceAtom = mMol.getConnAtom(atom, i);
					if (mIsSymmetricAtom[sourceAtom] || mMol.isSelectedAtom(sourceAtom))
						continue;

					int migratingBond = mMol.getConnBond(atom, i);
					if (!mMol.isRingBond(migratingBond)
					 && mMol.getBondOrder(migratingBond) == 1) {
						for (int j=0; j<mMol.getConnAtoms(atom); j++) {
						    if (i == j)
						        continue;

						    int destinationAtom = mMol.getConnAtom(atom, j);
						    if (!mIsSymmetricAtom[destinationAtom]
						     && !mMol.isSelectedAtom(destinationAtom)
							 && mMol.getFreeValence(destinationAtom) > 0) {
								mMol.copyMolecule(mMolCopy);
								for (int k=0; k<2; k++)
									if (mMolCopy.getBondAtom(k, migratingBond) == atom)
										mMolCopy.setBondAtom(k, migratingBond, destinationAtom);

								float f_Edukt = getFrequency(mMol, atom)
											   * getFrequency(mMol, sourceAtom)
											   * getFrequency(mMol, destinationAtom);
								float f_Produkt = getFrequency(mMolCopy, atom)
												 * getFrequency(mMolCopy, sourceAtom)
												 * getFrequency(mMolCopy, destinationAtom);

								if (f_Edukt == 0.0)
									f_Edukt = cDefaultEductFrequency;

								float p = (float)Math.pow(f_Produkt / f_Edukt, 1.0 / 3.0);

		                        if (p > 0.0 && isValidStructure(mMolCopy))
		                            mutationList.add(new Mutation(Mutation.MUTATION_MIGRATE,
																  migratingBond,
				                                                  -1,
																  atom,
																  destinationAtom,
																  p));
								}
							}
						}
					}
				}
			}
		}


    private void addPropabilitiesForSwapSubstituent(ArrayList<Mutation> mutationList,
                                                    ArrayList<MutatorSubstituent> substituentList) {
        for (MutatorSubstituent s1:substituentList) {
            for (MutatorSubstituent s2:substituentList) {
                if (!mIsSymmetricAtom[s1.firstAtom]
                 && s1.coreAtom != s2.coreAtom
                 && s1.bond != s2.bond) {
                    mMol.copyMolecule(mMolCopy);
                    for (int k=0; k<2; k++) {
                        if (mMolCopy.getBondAtom(k, s1.bond) == s1.firstAtom)
                            mMolCopy.setBondAtom(k, s1.bond, s2.firstAtom);
                        if (mMolCopy.getBondAtom(k, s2.bond) == s2.firstAtom)
                            mMolCopy.setBondAtom(k, s2.bond, s1.firstAtom);
                        }

                    float f_Edukt = getFrequency(mMol, s1.coreAtom)
                                   * getFrequency(mMol, s1.firstAtom)
                                   * getFrequency(mMol, s2.coreAtom)
                                   * getFrequency(mMol, s2.firstAtom);
                    float f_Produkt = getFrequency(mMolCopy, s1.coreAtom)
                                     * getFrequency(mMolCopy, s1.firstAtom)
                                     * getFrequency(mMolCopy, s2.coreAtom)
                                     * getFrequency(mMolCopy, s2.firstAtom);

                    if (f_Edukt == 0.0)
                        f_Edukt = cDefaultEductFrequency;

                    float p = (float)Math.pow(f_Produkt / f_Edukt, 1.0 / 4.0);

                    if (p > 0.0 && isValidStructure(mMolCopy))
                        mutationList.add(new Mutation(Mutation.MUTATION_SWAP_SUBSTITUENT,
                                                      s1.coreAtom,
                                                      s2.coreAtom,
                                                      s1.firstAtom,
                                                      s2.firstAtom,
                                                      p));
                    }
                }
            }
        }


    private void addPropabilitiesForDeleteSubstituent(ArrayList<Mutation> mutationList,
                                                      ArrayList<MutatorSubstituent> substituentList) {
        for (MutatorSubstituent s:substituentList) {
            if (!mIsSymmetricAtom[s.firstAtom]) {
                mMol.copyMolecule(mMolCopy);
                mMolCopy.deleteBond(s.bond);
    
                float f_Edukt = getFrequency(mMol, s.coreAtom);
                float f_Produkt = getFrequency(mMolCopy, s.coreAtom);
    
                if (f_Edukt == 0.0)
                    f_Edukt = cDefaultEductFrequency;
    
                float p = f_Produkt / f_Edukt;
    
                if (p > 0.0 && isValidStructure(mMolCopy))
                    mutationList.add(new Mutation(Mutation.MUTATION_DELETE_SUBSTITUENT,
                                                  s.coreAtom,
                                                  -1,
                                                  s.firstAtom,
                                                  -1,
                                                  p));
                }
            }
        }


    private void addPropabilitiesForCutOutFragment(ArrayList<Mutation> mutationList) {
        for (int atom=0; atom<mMol.getAtoms(); atom++) {
            if (!mIsSymmetricAtom[atom]
             && !mMol.isSelectedAtom(atom)
             && mMol.getConnAtoms(atom) > 2) {
                int ringBondCount = 0;
                for (int i=0; i<mMol.getConnAtoms(atom); i++)
                    if (mMol.isRingBond(mMol.getConnBond(atom, i)))
                        ringBondCount++;
                if (ringBondCount <= 2) {
                    for (int i=1; i<mMol.getConnAtoms(atom); i++) {
                        int atom1 = mMol.getConnAtom(atom, i);
                        int bond1 = mMol.getConnBond(atom, i);
                        if (mMol.getBondOrder(bond1) != 1)
                            continue;
                        for (int j=0; j<i; j++) {
                            int atom2 = mMol.getConnAtom(atom, j);
                            int bond2 = mMol.getConnBond(atom, j);
                            if (mMol.getBondOrder(bond2) != 1)
                                continue;
                            int coveredRingBondCount = (mMol.isRingBond(bond1) ? 1 : 0)
                                                     + (mMol.isRingBond(bond2) ? 1 : 0);
                            if (coveredRingBondCount == ringBondCount) {
                                mMol.copyMolecule(mMolCopy);
                                mMolCopy.setBondAtom(0, bond1, atom1);
                                mMolCopy.setBondAtom(1, bond1, atom2);
                                mMolCopy.deleteBond(bond2);
                                int[] atomMap = mMolCopy.deleteAtoms(mMolCopy.getFragmentAtoms(atom));

                                float f_Edukt = getFrequency(mMol, atom1)
                                               * getFrequency(mMol, atom2);
                                float f_Produkt = getFrequency(mMolCopy, atomMap[atom1])
                                                 * getFrequency(mMolCopy, atomMap[atom2]);

                                if (f_Edukt == 0.0)
                                    f_Edukt = cDefaultEductFrequency;

                                float p = (float)Math.pow(f_Produkt / f_Edukt, 1.0 / 2.0);

                                if (p > 0.0 && isValidStructure(mMolCopy))
                                    mutationList.add(new Mutation(Mutation.MUTATION_SWAP_SUBSTITUENT,
                                                                  atom,
                                                                  -1,
                                                                  atom1,
                                                                  atom2,
                                                                  p));
                                }
                            }
                        }
                    }
                }
            }
        }


    private ArrayList<MutatorSubstituent> createSubstituentList() {
        ArrayList<MutatorSubstituent> substituentList = new ArrayList<MutatorSubstituent>();
        boolean[] atomMask = new boolean[mMol.getAtoms()];
        for (int atom=0; atom<mMol.getAtoms(); atom++) {
            if (mMol.getConnAtoms(atom) > 2) {
                for (int i=0; i<mMol.getConnAtoms(atom); i++) {
                    int connAtom = mMol.getConnAtom(atom, i);
                    int connBond = mMol.getConnBond(atom, i);
                    if (mMol.isSelectedAtom(connAtom)
                     || mMol.isRingBond(connBond)
                     || mMol.getBondOrder(connBond) != 1)
                        continue;

                    boolean isSelectedSubstituent = false;
                    mMol.getSubstituent(atom, connAtom, atomMask, null, null);
                    int atomCount = 0;
                    for (int j=0; j<atomMask.length; j++) {
                        if (atomMask[j]) {
                            atomCount++;
                            if (mMol.isSelectedAtom(j)) {
                                isSelectedSubstituent = true;
                                break;
                                }
                            }
                        }
                    if (isSelectedSubstituent || atomCount<atomMask.length/2)
                        continue;

                    substituentList.add(new MutatorSubstituent(atom, connAtom, connBond));
                    }
                }
            }
        return substituentList;
	    }


	private float getFrequency(StereoMolecule mol, int atom) {
		try {
		    return mAtomTypeList == null ? 1.0f
		         : mAtomTypeList.getTotalFromType(AtomTypeCalculator.getAtomType(mol, atom, AtomTypeCalculator.cPropertiesForMutator));
			}
		catch (Exception e) {
			return 0.0f;
			}
		}


    private Mutation selectLikelyMutation(ArrayList<Mutation> mutationList) {
		float propabilitySum = 0.0f;
		for (Mutation m:mutationList)
			propabilitySum += m.mPropability;

		float selector = mRandom.nextFloat() * propabilitySum;

		propabilitySum = 0.0f;
        for (Mutation m:mutationList) {
			propabilitySum += m.mPropability;
			if (selector < propabilitySum) {
			    mMostRecentMutation = m;
			    return m;
			    }
			}
		return null;
		}


    /**
     * Performs the given mutation on the molecule, updates atom coordinates,
     * and updates stereo bonds to reflect lost or new stereo centers.
     * @param mol
     * @param mutation
     */
	public void performMutation(StereoMolecule mol, Mutation mutation) {
        mol.ensureHelperArrays(Molecule.cHelperParities);
		switch (mutation.mMutationType) {
		case Mutation.MUTATION_ADD_ATOM:
			int newAtom = mol.addAtom(mutation.mSpecifier1);
			mol.addBond(mutation.mWhere1, newAtom, mutation.mSpecifier2);
			break;
		case Mutation.MUTATION_INSERT_ATOM:
			int atom1 = mol.getBondAtom(0, mutation.mWhere1);
			int atom2 = mol.getBondAtom(1, mutation.mWhere1);
			mol.deleteBond(mutation.mWhere1);
			newAtom = mol.addAtom(mutation.mSpecifier1);
			mol.addBond(atom1, newAtom, Molecule.cBondTypeSingle);
			mol.addBond(atom2, newAtom, Molecule.cBondTypeSingle);
			break;
		case Mutation.MUTATION_CHANGE_ATOM:
			mol.setAtomicNo(mutation.mWhere1, mutation.mSpecifier1);
			break;
		case Mutation.MUTATION_DELETE_ATOM:
			mol.deleteAtom(mutation.mWhere1);	
			break;
		case Mutation.MUTATION_CUTOUT_ATOM:
			atom1 = mol.getConnAtom(mutation.mWhere1, 0);
			atom2 = mol.getConnAtom(mutation.mWhere1, 1);
			mol.addBond(atom1, atom2, Molecule.cBondTypeSingle);
			mol.deleteAtom(mutation.mWhere1);
			break;
		case Mutation.MUTATION_ADD_BOND:
			mol.addBond(mutation.mWhere1, mutation.mWhere2, mutation.mSpecifier1);
			break;
		case Mutation.MUTATION_CHANGE_BOND:
			mol.setBondType(mutation.mWhere1, mutation.mSpecifier1);
			break;
		case Mutation.MUTATION_DELETE_BOND:
			mol.deleteBond(mutation.mWhere1);
			break;
		case Mutation.MUTATION_CHANGE_RING:
			changeAromaticity(mol, mutation.mWhere1, mutation.mSpecifier1);
			break;
		case Mutation.MUTATION_MIGRATE:
			for (int i=0; i<2; i++)
				if (mol.getBondAtom(i, mutation.mWhere1) == mutation.mSpecifier1)
					mol.setBondAtom(i, mutation.mWhere1, mutation.mSpecifier2);
			break;
		case Mutation.MUTATION_DELETE_SUBSTITUENT:
		    boolean[] atomMask = new boolean[mol.getAtoms()];
		    mol.getSubstituent(mutation.mWhere1, mutation.mSpecifier1, atomMask, null, null);
		    mol.deleteAtoms(atomMask);
		    break;
        case Mutation.MUTATION_CUTOUT_SFRAGMENT:
            int rootAtom = mutation.mWhere1;
            atom1 = mutation.mSpecifier1;
            atom2 = mutation.mSpecifier2;
            int bond1 = -1;
            int bond2 = -1;
            for (int i=0; i<mol.getConnAtoms(rootAtom); i++) {
                if (mol.getConnAtom(rootAtom, i) == atom1)
                    bond1 = mol.getConnBond(rootAtom, i);
                else if (mol.getConnAtom(rootAtom, i) == atom2)
                    bond2 = mol.getConnBond(rootAtom, i);
                }
            if (bond1 != -1 && bond2 != -1) {
                mol.deleteBond(bond1);
                mol.deleteBond(bond2);
                int[] atomMap = mol.deleteAtoms(mol.getFragmentAtoms(rootAtom));
                mol.addBond(atomMap[atom1], atomMap[atom2], Molecule.cBondTypeSingle);
                }
            break;
			}

		repairCharges(mol);

		new CoordinateInventor().invent(mol);
		mol.setStereoBondsFromParity();	// rescue old parity information, where it is still correct

		mol.ensureHelperArrays(Molecule.cHelperParities);	// detect over/under-specified stereo information
		repairStereoChemistry(mol);	// assign random parities to new stereo centers, and change up/down accordingly
	    }


	private int getBondTypeFromOrder(int bondOrder) {
		switch (bondOrder) {
		case 1:
			return Molecule.cBondTypeSingle;
		case 2:
			return Molecule.cBondTypeDouble;
		case 3:
			return Molecule.cBondTypeTriple;
			}
		return 0;
		}


	private boolean qualifiesForRing(int[] graphBond, int[] graphLevel,
									 int[] graphParent, int atom1, int atom2) {
		int ringSize = graphLevel[atom2];
		int firstConsecutiveRingAtom = 0;
		int firstMaxConsecutiveRingAtom = 0;
		int lastMaxConsecutiveRingAtom = 0;
		int consecutiveRingBonds = 0;
		int maxConsecutiveRingBonds = 0;
		int currentAtom = atom2;
		while (currentAtom != atom1) {
			if (mMol.getAtomPi(currentAtom) == 2) {
				if (mMol.getConnBondOrder(currentAtom, 0) == 2) {	// allene
					if (ringSize < 9)
						return false;
					}
				else {	// alkyne
					if (ringSize < 10)
						return false;
					}
				}

			if (mMol.isRingBond(graphBond[currentAtom])) {
				if (consecutiveRingBonds == 0)
					firstConsecutiveRingAtom = currentAtom;
				consecutiveRingBonds++;
				}
			else {
				if (maxConsecutiveRingBonds < consecutiveRingBonds) {
					maxConsecutiveRingBonds = consecutiveRingBonds;
					firstMaxConsecutiveRingAtom = firstConsecutiveRingAtom;
					lastMaxConsecutiveRingAtom = currentAtom;
					}
				consecutiveRingBonds = 0;
				}

			currentAtom = graphParent[currentAtom];
			}

		if (maxConsecutiveRingBonds < consecutiveRingBonds) {
			maxConsecutiveRingBonds = consecutiveRingBonds;
			firstMaxConsecutiveRingAtom = firstConsecutiveRingAtom;
			lastMaxConsecutiveRingAtom = currentAtom;
			}

		int penalty = maxConsecutiveRingBonds;
		if (maxConsecutiveRingBonds > 0
		 && ((mMol.isAromaticAtom(firstMaxConsecutiveRingAtom)
		   || mMol.isAromaticAtom(lastMaxConsecutiveRingAtom))
		  || (mMol.getAtomPi(firstMaxConsecutiveRingAtom) > 0
		   && mMol.getAtomPi(lastMaxConsecutiveRingAtom) > 0)))
			penalty++;

		if (ringSize - penalty < 3)
			return false;
		
		return true;
		}


	/**
	 * Runs a rule based check for bicyclic systems with unacceptable strain.
	 * Checks any bridge between two different atoms of one ring
	 * connected to different atoms of one ring.
	 * @param mol
	 * @return
	 */
	private boolean isValidStructure(StereoMolecule mol) {
	    // The largest bridge length (bonds) between two direct ring neighbours
	    // that would still cause an unacceptable ring strain.
	    // (this is the smallest allowed chain (bond count) from ring atom to ring atom minus 3!!!)
	    // First index is ring size, 2nd index is number of bonds between attachment points
	    // We distinguish 3 types of rings:
	    // - aromatic or both attachment points sp2
	    // - one attachment point sp2
	    // - both attachment points sp3
	    final int[][] MAX_FORBIDDEN_BRIDGE_LENGTH_AROM = { null, null, null,
	            { -1, 2 },
	            { -1, 2, 6 },
	            { -1, 1, 5 },
	            { -1, 1, 4, 6 },
	            { -1, 1, 4, 6 } };
        final int[][] MAX_FORBIDDEN_BRIDGE_LENGTH_PI = { null, null, null,
                { -1, 1 },
                { -1, 1, 5 },
                { -1, 1, 4 },
                { -1, 0, 3, 4 },
                { -1, 0, 2, 3 } };
        final int[][] MAX_FORBIDDEN_BRIDGE_LENGTH_ALIPH = { null, null, null,
                { -1, 0 },
                { -1, -1, 3 },
                { -1, -1, 0 },
                { -1, -1, 0, -1 },
                { -1, -1, 0, -1 } };

	    mol.ensureHelperArrays(Molecule.cHelperRings);

	    RingCollection ringSet = mol.getRingSet();
	    boolean[] neglectAtom = new boolean[mol.getAtoms()];
	    for (int ring=0; ring<ringSet.getSize() && (ringSet.getRingSize(ring)<8); ring++) {
	        int[] ringAtom = ringSet.getRingAtoms(ring);
	        for (int atom:ringAtom)
	            neglectAtom[atom] = true;

	        int ringSize = ringSet.getRingSize(ring);
	        for (int i=1; i<ringSize; i++) {
	            if (mol.getConnAtoms(ringAtom[i]) > 2) {
	                for (int ci=0; ci<mol.getConnAtoms(ringAtom[i]); ci++) {
	                    int atom1 = mol.getConnAtom(ringAtom[i], ci);
	                    if (mol.isRingAtom(atom1) && !ringSet.isAtomMember(ring, atom1)) {
            	            for (int j=0; j<i; j++) {
            	                if (mol.getConnAtoms(ringAtom[j]) > 2) {
            	                    for (int cj=0; cj<mol.getConnAtoms(ringAtom[j]); cj++) {
            	                        int atom2 = mol.getConnAtom(ringAtom[j], cj);
            	                        if (mol.isRingAtom(atom2) && !ringSet.isAtomMember(ring, atom2)) {
            	                            int ringDif = Math.min(i-j, ringSize-i+j);
            	                            int pi1 = mol.getAtomPi(ringAtom[i]);
                                            int pi2 = mol.getAtomPi(ringAtom[j]);
            	                            int maxForbiddenLength =
            	                                (ringSet.isAromatic(ring) || (pi1!=0 && pi2!=0)) ?
            	                                    MAX_FORBIDDEN_BRIDGE_LENGTH_AROM[ringSize][ringDif]
                                              : (pi1!=0 || pi2!=0) ?
                                                    MAX_FORBIDDEN_BRIDGE_LENGTH_PI[ringSize][ringDif]
                                                  : MAX_FORBIDDEN_BRIDGE_LENGTH_ALIPH[ringSize][ringDif];
            	                            if (maxForbiddenLength != -1 && mol.getPathLength(atom1, atom2, maxForbiddenLength, neglectAtom) != -1)
            	                                return false;
            	                            }
            	                        }
            	                    }
            	                }
	                        }
	                    }
	                }
	            }

	        for (int atom:ringAtom)
                neglectAtom[atom] = false;
	        }
	    
	    return true;
	    }


	private boolean hasExocyclicPiBond(int[] ringAtom, int[] ringBond) {
		for (int i=0; i<ringAtom.length; i++) {
			for (int j=0; j<mMol.getConnAtoms(ringAtom[i]); j++) {
				int connBond = mMol.getConnBond(ringAtom[i], j);
				boolean bondIsInRing = false;
				for (int k=0; k<ringBond.length; k++) {
					if (connBond == ringBond[k]) {
						bondIsInRing = true;
						break;
						}
					}
				if (!bondIsInRing
				 && (mMol.isAromaticBond(connBond)
				  || mMol.getBondOrder(connBond) > 1))
					return true;
				}
			}
		return false;
		}


	private boolean changeAromaticity(StereoMolecule mol, int ring, int heteroPosition) {
		mol.ensureHelperArrays(Molecule.cHelperRings);
		RingCollection ringSet = mol.getRingSet();
		int ringSize = ringSet.getRingSize(ring);
		int ringAtom[] = ringSet.getRingAtoms(ring);
		int ringBond[] = ringSet.getRingBonds(ring);
		if (ringSet.isAromatic(ring)) {
			for (int i=0; i<ringSize; i++)
				mol.setBondType(ringBond[i], Molecule.cBondTypeSingle);
			return true;
			}
		else {
			for (int i=0; i<ringSize; i++)
				mol.setBondType(ringBond[i], Molecule.cBondTypeSingle);

			if (ringSize == 5) {
				for (int i=0; i<ringSize; i++) {
					if (heteroPosition != i) {
						if (mol.getFreeValence(ringAtom[i]) < 1)
							return false;
						if (mol.getAtomicNo(ringAtom[i]) != 6
						 && mol.getAtomicNo(ringAtom[i]) != 7)
							return false;
						}
					}
				int doubleBondPosition1 = heteroPosition + 1;
				int doubleBondPosition2 = heteroPosition + 3;
				if (doubleBondPosition1 > 4)
					doubleBondPosition1 -= 5;
				if (doubleBondPosition2 > 4)
					doubleBondPosition2 -= 5;
				mol.setBondType(doubleBondPosition1, Molecule.cBondTypeDouble);
				mol.setBondType(doubleBondPosition2, Molecule.cBondTypeDouble);

				return true;
				}

			if (ringSize == 6) {
				for (int i=0; i<ringSize; i++) {
					if (mol.getFreeValence(ringAtom[i]) < 1)
						return false;
					if (mol.getAtomicNo(ringAtom[i]) != 6
					 && mol.getAtomicNo(ringAtom[i]) != 7)
						return false;
					}

				for (int i=0; i<ringSize; i+=2)
					mol.setBondType(ringBond[i], Molecule.cBondTypeDouble);

				return true;
				}
			}
		return false;
		}

	private void repairCharges(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperRings);
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			// make sure that quarternary nitrogen is charged
			if (mol.getAtomicNo(atom) == 7
			 && (mol.getConnAtoms(atom) + mol.getAtomPi(atom)) == 4) {
				mol.setAtomCharge(atom, 1);
				for (int i=0; i<mol.getConnAtoms(atom); i++) {
					int connAtom = mol.getConnAtom(atom, i);
					if ((mol.getAtomicNo(connAtom) == 7 && (mol.getConnAtoms(connAtom) + mol.getAtomPi(connAtom)) < 3)
					 || (mol.getAtomicNo(connAtom) == 8 && (mol.getConnAtoms(connAtom) + mol.getAtomPi(connAtom)) < 2)) {
						mol.setAtomCharge(connAtom, -1);
						}
					}
				}
			// discharge carbon atoms
			if (mol.getAtomicNo(atom) == 6
			 && mol.getAtomCharge(atom) != 0) {
				mol.setAtomCharge(atom, 0);
				}
			}
		// remove isolated charges where possible
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.getAtomCharge(atom) != 0) {
				boolean chargedNeighborFound = false;
				for (int i=0; i<mol.getConnAtoms(atom); i++) {
					if (mol.getAtomCharge(mol.getConnAtom(atom, i)) != 0) {
						chargedNeighborFound = true;
						break;
						}
					}
				if (!chargedNeighborFound) {
					int valence = mol.getConnAtoms(atom) + mol.getAtomPi(atom);
					int maxValence = mol.getMaxValenceUncharged(atom);
					if (valence <= maxValence)
						mol.setAtomCharge(atom, 0);
					}
				}
			}
		}

	private void repairStereoChemistry(StereoMolecule mol) {
        for (int atom=0; atom<mol.getAtoms(); atom++) {
            switch (mol.getAtomParity(atom)) {
            case Molecule.cAtomParityUnknown:
                int parity = (mRandom.nextDouble() < 0.5) ? Molecule.cAtomParity1 : Molecule.cAtomParity2;
                boolean isPseudo = mol.isAtomParityPseudo(atom);
                mol.setAtomParity(atom, parity, isPseudo);
            case Molecule.cAtomParity1:
            case Molecule.cAtomParity2:
                mol.setStereoBondFromAtomParity(atom);
                break;
            case Molecule.cAtomParityNone:
                mol.convertStereoBondsToSingleBonds(atom);
                break;
                }
            }
        for (int bond=0; bond<mol.getBonds(); bond++) {
        	if (mol.isBINAPChiralityBond(bond)) {
	            switch (mol.getBondParity(bond)) {
	            case Molecule.cBondParityUnknown:
	                int parity = (mRandom.nextDouble() < 0.5) ? Molecule.cBondParityEor1 : Molecule.cBondParityZor2;
	                boolean isPseudo = mol.isBondParityPseudo(bond);
	                mol.setBondParity(bond, parity, isPseudo);
	            case Molecule.cBondParityEor1:
	            case Molecule.cBondParityZor2:
	                mol.setStereoBondFromBondParity(bond);
	                break;
	            case Molecule.cBondParityNone:
	                mol.convertStereoBondsToSingleBonds(mol.getBondAtom(0, bond));
	                mol.convertStereoBondsToSingleBonds(mol.getBondAtom(1, bond));
	                break;
	                }
        		}
            }
        }

    class MutatorSubstituent {
        public int coreAtom;
        public int firstAtom;
        public int bond;
//        public int size;

        public MutatorSubstituent(int coreAtom, int firstAtom, int bond) {
            this.coreAtom = coreAtom;
            this.firstAtom = firstAtom;
            this.bond = bond;
//            this.size = mMol.getSubstituentSize(coreAtom, firstAtom);
// in a first approach size is not considered
            }
        }
    }
