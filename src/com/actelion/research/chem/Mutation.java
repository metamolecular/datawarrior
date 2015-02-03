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

public class Mutation {
    public static final int MUTATION_ADD_ATOM = 1;
    public static final int MUTATION_INSERT_ATOM = 2;
    public static final int MUTATION_CHANGE_ATOM = 4;
    public static final int MUTATION_CUTOUT_ATOM = 8;
    public static final int MUTATION_DELETE_ATOM = 16;
    public static final int MUTATION_ADD_BOND = 32;
    public static final int MUTATION_CHANGE_BOND = 64;
    public static final int MUTATION_DELETE_BOND = 128;
    public static final int MUTATION_CHANGE_RING = 256;
    public static final int MUTATION_MIGRATE = 512;
    public static final int MUTATION_SWAP_SUBSTITUENT = 1024;
    public static final int MUTATION_DELETE_SUBSTITUENT = 2048;
    public static final int MUTATION_CUTOUT_SFRAGMENT = 4096;

	public static final int[][] cAllowedAtomicNo = 
		{ { 5, 6, 7, 8, 9, 15, 16, 17, 35, 53 },
		  { 6, 7, 8, 15, 16 },
		  { 6, 7 } };

	public int mMutationType;
	public int mWhere1;
    public int mWhere2;
	public int mSpecifier1;
	public int mSpecifier2;
	public double mPropability;

	public Mutation() {
		}

	public Mutation(int mutationType,
	                int where1, int where2,
	                int specifier1, int specifier2,
	                double propability) {
		mMutationType	= mutationType;
		mWhere1			= where1;
        mWhere2         = where2;
		mSpecifier1		= specifier1;
		mSpecifier2		= specifier2;
		mPropability	= propability;
		}

    public String toString() {
        switch (mMutationType) {
        case MUTATION_ADD_ATOM:
            return "Atom Addition; AtAtom:"+mWhere1
                  +" AtomicNo:"+mSpecifier1
                  +" BondType:"+mSpecifier2;
        case MUTATION_INSERT_ATOM:
            return "Atom Insertion; AtBond:"+mWhere1
                  +" AtomicNo:"+mSpecifier1;
        case MUTATION_CHANGE_ATOM:
            return "Atom Change; Atom:"+mWhere1
                  +" AtomicNo:"+mSpecifier1;
        case MUTATION_DELETE_ATOM:
            return "Atom Deletion; Atom:"+mWhere1;
        case MUTATION_CUTOUT_ATOM:
            return "Atom CutOut; Atom:"+mWhere1;
        case MUTATION_ADD_BOND:
            return "Bond Addition; FromAtom:"+mWhere1
                  +" ToAtom:"+mWhere2
                  +" BondType:"+mSpecifier1;
        case MUTATION_CHANGE_BOND:
            return "Bond Change; Bond:"+mWhere1
                  +" BondType:"+mSpecifier1;
        case MUTATION_DELETE_BOND:
            return "Bond Deletion; Bond:"+mWhere1;
        case MUTATION_CHANGE_RING:
            return "Ring Change; Ring:"+mWhere1;
        case MUTATION_MIGRATE:
            return "Group Migration; Bond:"+mWhere1
                  +" originalAtom:"+mSpecifier1
                  +" newAtom:"+mSpecifier2;
        case MUTATION_SWAP_SUBSTITUENT:
            return "Swap Substituent; coreAtom1:"+mWhere1
                  +" firstAtom1:"+mSpecifier1
                  +" coreAtom2:"+mWhere2
                  +" firstAtom2:"+mSpecifier2;
        case MUTATION_DELETE_SUBSTITUENT:
            return "Delete Substituent; coreAtom:"+mWhere1
                  +" firstAtom:"+mSpecifier1;
        case MUTATION_CUTOUT_SFRAGMENT:
            return "CutOut Fragment; rootAtom:"+mWhere1
                  +" new bond from atom1:"+mSpecifier1+" to atom2:"+mSpecifier2;
            }
        return "Unknown Mutation";
        }
    }