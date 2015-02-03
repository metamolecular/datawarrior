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
package com.actelion.research.chem;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.util.ArrayUtils;

/**
 * FFMolecule extends Molecule3D and contains some more information relative to
 * the Forcefield:
 *  - atomClasses: the MM2 atom types
 *  - classIds:  the interaction atom types (derived from the MM2 atomTypes)
 * 
 * The atomClasses can be set using MM2Parameters.getInstance().setAtomClasses(mol)
 * The classIds can be set ClassStatistics.getInstance().setClassIds(mol)
 * 
 */
public class FFMolecule implements java.io.Serializable, Comparable<FFMolecule>  {

	private static final long serialVersionUID = 1L;
	
	public static final int RIGID = 1<<0;
	public static final int LIGAND = 1<<1;
	public static final int BACKBONE = 1<<2;
	public static final int FLAG1 = 1<<3; //Flag used for different purpose
	public static final int IMPORTANT = 1<<4;
	public static final int PREOPTIMIZED = 1<<6;		
	
	
	public static final int INFO_DESCRIPTION = 0;
	public static final int INFO_ATOMSEQUENCE = 1;
	public static final int INFO_MM2ATOMDESCRIPTION = 2;
	public static final int INFO_ATOMNAME = 3;
	public static final int INFO_AMINO = 4;
	public static final int INFO_PPP = 5;
	public static final int INFO_CHAINID = 6;
	
	private static final int MAX_INFOS = 7;

	//Molecule information
	private String name;
	private int nAtoms;
	private final Hashtable<String, Object> auxiliaryInfos = new Hashtable<String, Object>();
	
	//Atom information
	private Coordinates[] coords;
	private int[] atomicNos; 
	private int[] atomCharges; 
	private int[] atomFlags;
	private Object[][] infos;
	private double[] partialCharges;

	//Bond information
	private int nBonds;
	private int[][] bonds;   // [bondNo][0->1st atom, 1->2nd atom, 2->order]	
	private int[] nConn; 	 // <8
	private int[] connAtoms; // [atom<<3+connNo]  -> atmNo   
	private int[] connBonds; // [atom<<3+connNo]  -> bondNo  
	
	private int[] atomClasses;
	private int[] classIds;

	//Molecule properties (calculated)
	private List<Integer>[] atomToRings = null; 
	private List<int[]> allRings = null;
	
	private boolean[] aromatic;
	private boolean[] aromaticRing;
	private boolean aromaticComputed;

	private int nMovables = -1;
	
	public FFMolecule(Molecule mol) {
		this(mol.getAllAtoms(), mol.getAllBonds());
		setName(mol.getName());
		
		this.atomicNos = new int[mol.getAllAtoms()];
		this.atomCharges = new int[mol.getAllAtoms()];
		this.coords = new Coordinates[mol.getAllAtoms()];
		this.partialCharges = new double[mol.getAllAtoms()];
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			atomicNos[nAtoms] = mol.getAtomicNo(i);
			atomCharges[nAtoms] = mol.getAtomCharge(i);
			partialCharges[nAtoms] = 0;
			coords[nAtoms] = new Coordinates(mol.getAtomX(i), -mol.getAtomY(i), -mol.getAtomZ(i));
			nAtoms++;
		}
		
		for(int i=0; i<mol.getAllBonds(); i++) {
			addBond(mol.getBondAtom(0, i), mol.getBondAtom(1, i), mol.getBondOrder(i));
		}				
		setAllAtomFlag(FFMolecule.LIGAND, true);	
	}

	public FFMolecule(String name) {
		this();
		setName(name);
	}

	public FFMolecule(FFMolecule mol) {
		this(mol.getAllAtoms(), mol.getAllBonds());
		this.name = mol.name;
		this.auxiliaryInfos.putAll(mol.auxiliaryInfos);
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			atomicNos[nAtoms] = mol.getAtomicNo(i);
			atomCharges[nAtoms] = mol.getAtomCharge(i);
			partialCharges[nAtoms] = mol.getPartialCharge(i);
			infos[nAtoms] = mol.infos[i];
			coords[nAtoms] = new Coordinates(mol.getAtomX(i), mol.getAtomY(i), mol.getAtomZ(i));
			nAtoms++;
		}
		for(int i=0; i<mol.getAllBonds(); i++) {
			addBond(mol.getBondAtom(0, i), mol.getBondAtom(1, i), mol.getBondOrder(i));
		}
		System.arraycopy(mol.atomFlags, 0, atomFlags, 0, mol.getAllAtoms());		
		System.arraycopy(mol.infos, 0, infos, 0, mol.getAllAtoms());		
		System.arraycopy(mol.atomClasses, 0, atomClasses, 0, mol.getAllAtoms());
		System.arraycopy(mol.classIds, 0, classIds, 0, mol.getAllAtoms());
		System.arraycopy(mol.partialCharges, 0, partialCharges, 0, mol.getAllAtoms());
	}	

	public FFMolecule() {
		this(5, 5);
	}

	public FFMolecule(int a, int b) {
		if(a<5) a = 5;
		if(b<5) b = 5;

		name = "Molecule";
		nAtoms = 0;				
		coords = new Coordinates[a];
		atomicNos = new int[a];
		atomCharges = new int[a];
		atomFlags   = new int[a];
		partialCharges = new double[a];
		//atomDescriptions = new String[a];
		infos = new Object[a][MAX_INFOS];

		nConn = new int[a];
		connAtoms = new int[a<<3];
		connBonds = new int[a<<3];

		nBonds = 0;				
		bonds = new int[b][3];		
		
		atomClasses = new int[a];
		//atomTypes = new String[a];
		classIds = new int[a];
		aromatic = new boolean[a];
	}	

	@Override
	public String toString() {
 		return (name!=null? name : "");
	}

	public String getDataAsString() {
		StringBuilder sb = new StringBuilder();
		DecimalFormat df2 = new DecimalFormat("0.00");
		for (String key: getAuxiliaryInfos().keySet()) {
			if(key.equals("idcode")) continue;
			if(key.equals("idcoordinates")) continue;
			if(key.startsWith("group3")) continue;
			Object value = getAuxiliaryInfo(key);
			if(value==null) continue;
			String s = value instanceof Double? df2.format(value): ""+value;
			sb.append(key+"="+s+" ");
		}
 		return sb.toString();
	}

	public void clear() {
		nBonds = 0;
		nAtoms = 0;
	}

	public final void setAtomFlag(int atm, int flag, boolean value) {
		if(value) atomFlags[atm] |= flag;
		else atomFlags[atm] &= ~flag;
	}
	
	public final boolean isAtomFlag(int atm, int flag) {
		return (atomFlags[atm]&flag)>0;		
	}
	public final int getAtomFlags(int atm) {
		return atomFlags[atm];		
	}
	public final void setAtomFlags(int atm, int flags) {
		atomFlags[atm] = flags;		
	}
	public final void setAllAtomFlag(int flag, boolean value) {
		for (int i = 0; i < getAllAtoms(); i++) setAtomFlag(i, flag, value);	
	}
	
	public final Coordinates getCoordinates(int atm) {
		return coords[atm];
	}
	public final Coordinates[] getCoordinates() {
		return coords;
	}
	
	public final void setCoordinates(int atm, Coordinates c) {
		coords[atm] = c;
	}
	public final void setCoordinates(Coordinates[] c) {
		if(c.length!=coords.length) throw new IllegalArgumentException("Length "+c.length+" <> "+coords.length);
		coords = c;
	}

	public final double getAtomX(int atm) {
		return getCoordinates(atm).x;
	}

	public final double getAtomY(int atm) {
		return getCoordinates(atm).y;
	}

	public final double getAtomZ(int atm) {
		return getCoordinates(atm).z;
	}

	public final void setAtomX(int atm, double x) {
		getCoordinates(atm).x = x;
	}

	public final void setAtomY(int atm, double y) {
		getCoordinates(atm).y = y;
	}

	public final void setAtomZ(int atm, double z) {
		getCoordinates(atm).z = z;
	}

	/**
	 * This method will append a Molecule3D to the end.  
	 * @param m2
	 * @return the index dividing the 2 molecules
	 */
	public int fusion(FFMolecule m2) {
		if(m2==this) throw new IllegalArgumentException("Cannot fusion a molecule with itself");
		int index = getAllAtoms();
		int[] oldToNew = new int[m2.getAllAtoms()];
		for(int i=0; i<m2.getAllAtoms(); i++) {			
			oldToNew[i] = addAtom(m2, i); 
		}
		for(int i=0; i<m2.getAllBonds(); i++) {
			addBond(oldToNew[m2.getBondAtom(0, i)], oldToNew[m2.getBondAtom(1, i)], m2.getBondOrder(i));
		}
		return index;
	}
	public int fusionKeepCoordinatesObjects(FFMolecule m2) {
		int i = getAllAtoms();
		int res = fusion(m2);
		System.arraycopy(m2.getCoordinates(), 0, getCoordinates(), i, m2.getAllAtoms());
		return res;
		
	}
	
	public int fusion(FFMolecule m2, int flags) {
		if(m2==this) throw new IllegalArgumentException("Cannot fusion a molecule with itself");
		int index = getAllAtoms();
		int[] oldToNew = new int[m2.getAllAtoms()];
		Arrays.fill(oldToNew, -1);
		for(int i=0; i<m2.getAllAtoms(); i++) {
			if(flags==0 || m2.isAtomFlag(i, flags)) oldToNew[i] = addAtom(m2, i); 
		}
		for(int i=0; i<m2.getAllBonds(); i++) {
			int a1 = oldToNew[m2.getBondAtom(0, i)];
			int a2 = oldToNew[m2.getBondAtom(1, i)];
			if(a1>=0 && a2>=0) addBond(a1, a2, m2.getBondOrder(i));
		}
		return index;
	}
	


	public final int getAllAtoms() {
		return nAtoms;
	}

	public int getAtoms() {
		int res = 0;
		for (int i = 0; i < getAllAtoms(); i++) {
			if(getAtomicNo(i)>1) res++;
		}
		return res;
	}


	public final void setAtomicNo(int atm, int atomicNo) {
		atomicNos[atm] = atomicNo;		
	}

	public final int getAtomicNo(int atm) {
		return atomicNos[atm];
	}

	public final int getAllConnAtoms(int atm) {
		return nConn[atm];
	}

	public final int getConnAtoms(int atm) {
		int count = 0;
		for (int i = 0; i < getAllConnAtoms(atm); i++) {
			if(getAtomicNo(getConnAtom(atm,i))>1) count++;
		}
		return count;
	}
	public final int getValence(int atm) {
		int count = 0;
		for (int i = 0; i < getAllConnAtoms(atm); i++) {
			if(getAtomicNo(getConnAtom(atm,i))>1) count += getConnBondOrder(atm, i);
		}
		return count;
	}

	public final int getConnAtom(int atm, int i) {
		return connAtoms[(atm<<3)+i];
	}

	public int getConnBond(int atm, int i) {
		return connBonds[(atm<<3)+i];
	}

	public final int getConnBondOrder(int atm, int i) {
		return bonds[connBonds[(atm<<3)+i]][2];
	}

	public final int getAllBonds() {
		return nBonds;
	}

	public final int getBondAtom(int i, int bond) {
		return bonds[bond][i];
	}

	public final void setAtomCharge(int atm, int charge) {
		atomCharges[atm] = charge;		
	}

	public final int getAtomCharge(int atm) {
		return atomCharges[atm];
	}
	
	public final Object getInfo(int atm, int n) {
		return infos[atm][n];
	}
	
	
	
	public final void setAtomDescription(int atm, String s) {
		infos[atm][INFO_DESCRIPTION] = s;
	}

	public final String getAtomDescription(int atm) {
		return (String) infos[atm][INFO_DESCRIPTION];
	}
	
	public final void setPPP(int atm, int[] a) {
		infos[atm][INFO_PPP] = a;
	}

	public final int[] getPPP(int atm) {
		return (int[]) infos[atm][INFO_PPP];
	}
	public final void setAtomSequence(int atm, int a) {
		infos[atm][INFO_ATOMSEQUENCE] = a;
	}

	public final int getAtomSequence(int atm) {
		return infos[atm][INFO_ATOMSEQUENCE]==null?-1 : (Integer) infos[atm][INFO_ATOMSEQUENCE];
	}
	
	public final void setAtomChainId(int atm, String a) {
		infos[atm][INFO_CHAINID] = a;
	}

	public final String getAtomChainId(int atm) {
		return (String) infos[atm][INFO_CHAINID];
	}
	
	public final void setAtomName(int atm, String a) {
		infos[atm][INFO_ATOMNAME] = a;
	}

	public final String getAtomName(int atm) {
		return (String) infos[atm][INFO_ATOMNAME];
	}
	
	public final void setAtomAmino(int atm, String a) {
		infos[atm][INFO_AMINO] = a;
	}

	public final String getAtomAmino(int atm) {
		return (String) infos[atm][INFO_AMINO];
	}
	
	public final int getBondBetween(int a1, int a2) {
		int connBond;
		for(connBond=0; connBond<getAllConnAtoms(a1); connBond++) {
			if(getConnAtom(a1, connBond)==a2) return getConnBond(a1, connBond);
		}
		return -1;
				
	}
	


	

	public final boolean setBondOrder(int bond, int order) {
		bonds[bond][2] = order;
		return true;	
	}

	public final int getBondOrder(int bond) {
		return bonds[bond][2];
	}

	public final void setBondAtom(int i, int bond, int atm) {
		bonds[bond][i] = atm;
	}


	
	
	

	
	
	////////////////////////////// UTILITIES ////////////////////////////////////////
	
	public final void deleteAtoms(List<Integer> atomsToBeDeleted) {
		Collections.sort(atomsToBeDeleted);
		for (int i = atomsToBeDeleted.size()-1; i>=0; i--) {
			deleteAtom(atomsToBeDeleted.get(i));
		}		
	}

	public String getName() {
		return name==null?"":name;
	}

	public String getShortName() {
		String name = getName();
		if(name.indexOf(' ')>0) name = name.substring(0, name.indexOf(' '));
		if(name.length()>12) name = name.substring(0, 12);
		return name;
	}

	private static transient int n = 0;
	public void setName(String name) {
		this.name = name;
		if(name==null || name.length()==0) {
			name = "Mol-" + (++n);
		}
	}

	public Hashtable<String, Object> getAuxiliaryInfos() {
		return auxiliaryInfos;
	}	
	public Object getAuxiliaryInfo(String name) {
		return auxiliaryInfos.get(name);
	}	
	public void setAuxiliaryInfo(String name, Object value) {
		if(value==null) {
			System.err.println("Attempt to set "+name+" to null");
			auxiliaryInfos.remove(name);
		} else {
			auxiliaryInfos.put(name, value);
		}
	}	

	@Override
	public boolean equals(Object obj) {
		return obj==this;
	}
	

	
	public final void setAtomMM2Class(int atm, int claz) {
		atomClasses[atm] = claz;		
	}
	

	public final void setAtomMM2Description(int atm, String desc) {
		infos[atm][INFO_MM2ATOMDESCRIPTION] = desc;
	}

	/**
	 * Returns the Interaction Atom Type
	 * @param atm
	 * @param id
	 */
	public final void setAtomInteractionClass(int atm, int id) {
		classIds[atm] = id;
	}

	/**
	 * Returns the MM2 Atom Type
	 * @param atm
	 * @return
	 */
	public final int getAtomMM2Class(int atm) {
		return atomClasses[atm];
	}
	
	public final String getAtomMM2Description(int atm) {
		return (String) getInfo(atm, INFO_MM2ATOMDESCRIPTION);
	}
	
	public final int getAtomInteractionClass(int atm) {
		return classIds[atm];
	}
	
	/**
	 * Adds a bond between 2 atoms
	 * @see com.actelion.research.chem.IMolecule#addBond(int, int, int)
	 */
	public final int addBond(int atm1, int atm2, int order) {
		atomToRings = null;
		aromaticComputed = false;

		//check for existing connections
		for(int i=0; i<getAllConnAtoms(atm1); i++)  {
			if(getConnAtom(atm1, i)==atm2) {
				int bnd = getConnBond(atm1, i);
				setBondOrder(bnd, order);
				return bnd;
			}
		}
		
		//Add the bond
		if(nConn[atm1]>7 || nConn[atm2]>7) throw new IllegalArgumentException("Maximum 8 neighbours");
		if(atm2<atm1) {int tmp = atm2; atm2 = atm1; atm1 = tmp;}
		connAtoms[(atm1<<3)+nConn[atm1]] = atm2;
		connBonds[(atm1<<3)+nConn[atm1]++] = nBonds;
		connAtoms[(atm2<<3)+nConn[atm2]] = atm1;
		connBonds[(atm2<<3)+nConn[atm2]++] = nBonds;
		
		if(bonds.length<=nBonds) {
			bonds = (int[][]) ArrayUtils.resize(bonds, 5+bonds.length*2);
		} 
		int n = nBonds;
		bonds[nBonds++] = new int[]{atm1, atm2, order};
		return n;
	}
	

	/**
	 * Delete an atom
	 * @see com.actelion.research.chem.IMolecule#deleteAtom(int)
	 */
	public void deleteAtom(int atm) {
		atomToRings = null;
		nMovables = -1;
		aromaticComputed = false;
		//Delete bonds going to atm
		for(int i=nConn[atm]-1; i>=0; i--) {
			deleteBond(connBonds[(atm<<3)+i]);
		}		
		//if(getAllConnAtoms(atm)!=0 )throw new IllegalArgumentException();

		//Delete the atom
		--nAtoms;
		if(atm==nAtoms) return;	
		copyAtom(atm, nAtoms);		
	}
	
	/**
	 * Deletes a bond
	 * @see com.actelion.research.chem.IMolecule#deleteBond(int)
	 */
	public final void deleteBond(int bond) {
		atomToRings = null;
		aromaticComputed = false;

		//Delete the connected atoms
		int i;
		int a1 = bonds[bond][0];
		int a2 = bonds[bond][1];
		for(i=0; connBonds[(a1<<3)+i]!=bond; i++) {}
		--nConn[a1];
		connBonds[(a1<<3)+i] = connBonds[(a1<<3)+ nConn[a1]];
		connAtoms[(a1<<3)+i] = connAtoms[(a1<<3)+ nConn[a1]];

		for(i=0; connBonds[(a2<<3)+i]!=bond; i++) {}
		--nConn[a2];
		connBonds[(a2<<3)+i] = connBonds[(a2<<3)+ nConn[a2]];
		connAtoms[(a2<<3)+i] = connAtoms[(a2<<3)+ nConn[a2]];
		
		//Move the last bond to the new position
		--nBonds;
		if(nBonds<=0 || bond==nBonds) return;		
		bonds[bond] = bonds[nBonds];
		
		//update references to this bond
		a1 = bonds[nBonds][0];
		a2 = bonds[nBonds][1];
		for(i=0; connBonds[(a1<<3)+i]!=nBonds; i++) {}
		connBonds[(a1<<3)+i] = bond;
		for(i=0; connBonds[(a2<<3)+i]!=nBonds; i++) {}
		connBonds[(a2<<3)+i] = bond;				
	}

	public List<Integer>[] getAtomToRings() {
		if(atomToRings==null) {
			allRings = new ArrayList<int[]>();
			atomToRings = StructureCalculator.getRingsFast(this, allRings);
		}
		return atomToRings;
	}

	public List<int[]> getAllRings() {
		if(atomToRings==null) getAtomToRings();
		return allRings;
	}

	public int getRingSize(int atom) {
		if(getAtomToRings()[atom].size()==0) return -1;
		int ringNo = getAtomToRings()[atom].get(0);
		return allRings.get(ringNo).length;
	}
	
	/**
	 * Get the size of the ring that is shared by the given atoms 
	 * @param atoms
	 * @return
	 */
	public final int getRingSize(int[] atoms) {
		Set<Integer> set = new TreeSet<Integer>();
		set.addAll(getAtomToRings()[atoms[0]]);
		for(int i=1; i<atoms.length; i++) {
			set.retainAll(getAtomToRings()[atoms[i]]);			
		}
		if(set.size()==0) return 0;
		int ringNo = set.iterator().next();
		int ringSize = getAllRings().get(ringNo).length;
		if(ringSize>=3 && ringSize<=6) return ringSize;
		return 0;
	}	
	
	public void compact() {
		extendAtomsSize(getAllAtoms());
		bonds = (int[][]) ArrayUtils.resize(bonds, getAllBonds());
	}
	
	protected void extendAtomsSize(int newSize) {
		coords = (Coordinates[]) ArrayUtils.resize(coords, newSize);
		atomFlags  = (int[]) ArrayUtils.resize(atomFlags, newSize);
		atomicNos  = (int[]) ArrayUtils.resize(atomicNos, newSize);
		atomCharges  = (int[]) ArrayUtils.resize(atomCharges, newSize);
		//atomDescriptions = (String[]) ArrayUtils.resize(atomDescriptions, newSize);
		infos = (Object[][]) ArrayUtils.resize(infos, newSize);
		partialCharges = (double[]) ArrayUtils.resize(partialCharges, newSize);
		
		connAtoms  = (int[]) ArrayUtils.resize(connAtoms, newSize<<3);
		connBonds  = (int[]) ArrayUtils.resize(connBonds, newSize<<3);
		nConn  = (int[]) ArrayUtils.resize(nConn, newSize);
		atomClasses  = (int[]) ArrayUtils.resize(atomClasses, newSize);
		classIds = (int[]) ArrayUtils.resize(classIds, newSize);
		aromatic = (boolean[]) ArrayUtils.resize(aromatic, newSize);
	}
	

	/**
	 * Copy an atom from src to dest. 
	 * src can be freely deleted after that.
	 * This has to be overriden by subclasses
	 * @param dest
	 * @param src
	 */
	protected void copyAtom(int dest, int src) {
		if(src>=0) {
			atomFlags[dest] = atomFlags[src];
			atomicNos[dest] = atomicNos[src];
			atomCharges[dest] = atomCharges[src];
			
			partialCharges[dest] = partialCharges[src];
			infos[dest] = infos[src].clone();			
			coords[dest] = coords[src];
			nConn[dest] = nConn[src];
			//nConn[src] = 0;
			for(int i=0; i<nConn[dest]; i++) {				
				connAtoms[(dest<<3)+i] = connAtoms[(src<<3)+i];
				connBonds[(dest<<3)+i] = connBonds[(src<<3)+i];
			}	
			
			//Update the references to atm (replace src by dest everywhere)
			for(int i=0; i<connAtoms.length; i++) if(connAtoms[i]==src) connAtoms[i]=dest;
			
			for(int i=0; i<nBonds; i++) {
				if(bonds[i][0]==src) bonds[i][0]=dest;
				else if(bonds[i][1]==src) bonds[i][1]=dest;
			} 

			atomClasses[dest] = atomClasses[src];
			classIds[dest] = classIds[src];
		} else {
			atomFlags[dest] = 0;
			atomicNos[dest] = 0;
			atomCharges[dest] = 0;

			partialCharges[dest] = 0;
			infos[dest] = new Object[MAX_INFOS];
			coords[dest] = new Coordinates();
			nConn[dest] = 0;		
			atomClasses[dest] = -1;
			classIds[dest] = -1;
		}
	}
	
	/**
	 * Add an atom with the given atomicNo
	 * @see com.actelion.research.chem.IMolecule#addAtom(int)
	 */
	public int addAtom(int atomicNo) {
		if(atomicNos.length<=nAtoms) {
			extendAtomsSize(10+atomicNos.length*2);
		}
		int n = nAtoms;
		copyAtom(n, -1);
		atomicNos[n] = atomicNo;
		nAtoms++;
		nMovables = -1;	
		return n;
	}		
	
	/**
	 * Add an atom by copying its properties from the given Molecule3D
	 * This has to be overriden by subclasses
	 * @param m
	 * @param i
	 * @return
	 */
	public int addAtom(FFMolecule m, int i) {		
		int a = addAtom(m.getAtomicNo(i));			
		atomFlags[a] = m.getAtomFlags(i);
		infos[a] = m.infos[i].clone();
		coords[a] = new Coordinates(m.getCoordinates(i));
		atomCharges[a] = m.getAtomCharge(i);
		atomClasses[a] = m.getAtomMM2Class(i);
		classIds[a] = m.getAtomInteractionClass(i);
		partialCharges[a] = m.getPartialCharge(i);
		nMovables = -1;
		return a;
	}
	
	///////////////////// UTILITY FUNCTIONS ////////////////////////////////////
	/**
	 * Move the Moveable atom to the first indexes
	 * [slow function]
	 * @return the number of moveable atoms
	 */
	public boolean reorderAtoms() {
		boolean changed = false;
		int N = getAllAtoms();
		int i = 0; 	 //index of the first moveable atom
		int j = N-1; //index of the last non-moveable atom
		
		if(N==0) {
			nMovables = 0;
			return false;
		}

		//Increase the array size if needed (to have up to N+1 atoms)
		if(atomClasses.length==N) extendAtomsSize(N*2+10);
		
		while(i<j) {
			//Move i to the first non-moveable atom
			while(i<j && !isAtomFlag(i, RIGID)) i++;

			//Move j to the last moveable atom
			while(i<j && isAtomFlag(j, RIGID)) j--;
			if(isAtomFlag(i, RIGID) && !isAtomFlag(j, RIGID)) {
				//Switch the 2 atoms
				copyAtom(N, i);
				copyAtom(i, j);
				copyAtom(j, N);
				changed = true;
			}
		}
		nMovables = isAtomFlag(i, RIGID)? i: i+1;		

		if(changed) {
			atomToRings = null;
			aromaticComputed = false;
		}
		return changed;
	}
	
	public void reorderHydrogens() {
		int N = getAllAtoms();
		int i = 0; 	 //index of the first hydrogen
		int j = N-1; //index of the last hydrogen
		
		//Increase the array size if needed (to have up to N+1 atoms)
		if(atomClasses.length==N) extendAtomsSize(N*2+10);
		
		while(i<j) {
			//Move i to the first non-moveable atom
			while(i<j && getAtomicNo(i)<=1) i++;

			//Move j to the last moveable atom
			while(i<j && !(getAtomicNo(j)<=1)) j--;
			if(i<j && !(getAtomicNo(i)<=1) && (getAtomicNo(j)<=1)) {
				//Switch the 2 atoms
				copyAtom(N, i);
				copyAtom(i, j);
				copyAtom(j, N);
				atomToRings = null;
				aromaticComputed = false;
				
			}
		}
	}		

	/**
	 * @return the number of movable atoms (after reorderatoms has been called)
	 */
	public int getNMovables() {
		if(nMovables<0 || (getAllAtoms()>0 && !isAtomFlag(0, LIGAND))) reorderAtoms();
		return nMovables;
	}
	public boolean isMoleculeInOrder() {
		return reorderAtoms()==false;
	}
	
	/**
	 * Huckel's rule for aromaticity prediction
	 * 
	 */
	public boolean isAromatic(int a) {
		if(!aromaticComputed) computeAromaticity();
		return aromatic[a];
	}
	public boolean isAromaticRing(int a) {
		if(!aromaticComputed) computeAromaticity();
		return aromaticRing[a];
	}

	private void computeAromaticity() {
		aromaticComputed = true;

		List<int[]> allRings = getAllRings();
		Arrays.fill(aromatic, false);
		aromaticRing = new boolean[allRings.size()];
		
		loopRing: for(int ringNo=0; ringNo<getAllRings().size(); ringNo++) { 
			int[] ring = getAllRings().get(ringNo);
			int n = ring.length;
			if(n!=5 && n!=6) continue;
			int nPi = 0;
			for(int i=0; i<ring.length; i++) {
				int a1 = ring[(i)%ring.length];				
				int a2 = ring[(i+1)%ring.length];				
				int a0 = ring[(i-1+ring.length)%ring.length];				
				int bnd1 = getBondBetween(a1, a2);
				int bnd2 = getBondBetween(a1, a0);
				if(bnd1<0 || bnd2<0) {
					System.err.println("Inconsistent Molecule: rings with <0 bonds??");
					atomToRings = null;
					allRings = null;
					return;
				} else if(getAtomicNo(a1)==6) {
					boolean ok = false;
					for (int j = 0; j < getAllConnAtoms(a1); j++) {
						if(getConnBondOrder(a1, j)==2) {ok=true; break;}
					}				
					if(ok) nPi++;
					else continue loopRing;
				} else if(getAtomicNo(a1)==7) {
					if(getBondOrder(bnd1) + getBondOrder(bnd2)==2) {nPi+=2;} 
					else nPi++;
				} else if((getAtomicNo(a1)==16 || getAtomicNo(a1)==8) && getAllConnAtoms(a1)==2) {
					if(getBondOrder(bnd1) + getBondOrder(bnd2)==2) {nPi+=2;} 
					else continue loopRing;
				} else {
					continue loopRing;
				} 
				
			}
			if(nPi==6 || nPi==10) {
				aromaticRing[ringNo]=true;
				for(int atom: ring) {
					aromatic[atom]=true;
				}
			}			
		}
		/*
		boolean seen[] = new boolean[getAllAtoms()];
		loopAtoms: for (int a = 0; a < seen.length; a++) {
			if(seen[a]) continue;
			seen[a] = true;
			
			loop: for (int ringNo: getAtomToRings()[a]) {
				int[] ring = allRings.get(ringNo);
				int n = ring.length;
				if(n!=5 && n!=6) continue;
				int nPi = 0;
				for(int i=0; i<ring.length; i++) {
					int a1 = ring[(i)%ring.length];				
					int a2 = ring[(i+1)%ring.length];				
					int a0 = ring[(i-1+ring.length)%ring.length];				
					int bnd1 = getBondBetween(a1, a2);
					int bnd2 = getBondBetween(a1, a0);
					
					if(getAtomicNo(a1)==6) {
						boolean ok = false;
						for (int j = 0; j < getAllConnAtoms(a1); j++) {
							if(getConnBondOrder(a1, j)==2) {ok=true; break;}
						}				
						if(ok) nPi++;
						else continue loop;
					} else if(getAtomicNo(a1)==7) {
						if(getBondOrder(bnd1) + getBondOrder(bnd2)==2) {nPi+=2;} 
						else nPi++;
					} else {
						continue loop;
					} 
					
				}
				if(nPi==6 || nPi==10) {
					for(int atom: ring) {
						aromatic[atom]=true;
						seen[atom] = true;
					}
					continue loopAtoms;
				}
			}
			
		}*/
		
	}

	public void checkIntegrity() {
		for (int i = 0; i < getAllAtoms(); i++) {
			if(Double.isNaN(getAtomX(i)) || Double.isNaN(getAtomY(i)) || Double.isNaN(getAtomZ(i))) {
				throw new IllegalArgumentException("Invalid coordinates, atom "+i+" -> "+getCoordinates(i));
			}
		}
	}
	
	public double getPartialCharge(int a) {
		return partialCharges[a];
	}
	public void setPartialCharge(int a, double v) {
		partialCharges[a] = v;
	}
	
	/**
	 * Factory Constructor to a StereoMolecule 
	 * @param mol
	 */
	public StereoMolecule toStereoMolecule() {
		StereoMolecule m = new StereoMolecule(getAllAtoms(), getAllBonds());
		populate(m);
		return m;
	}
	public ExtendedMolecule toExtendedMolecule() {
		ExtendedMolecule m = new ExtendedMolecule(getAllAtoms(), getAllBonds());
		populate(m);
		return m;
	}

	private void populate(ExtendedMolecule m) {
		m.setName(getName());
		int[] ff2em = new int[getAllAtoms()];
		for(int i=0; i<getAllAtoms(); i++) {
			int at = getAtomicNo(i);
			//if(at==0) continue;
			int a = m.addAtom(at);
			ff2em[i] = a+1;
			m.setAtomX(a, getAtomX(i));
			m.setAtomY(a, -getAtomY(i));
			m.setAtomZ(a, -getAtomZ(i));
			m.setAtomCharge(a, getAtomCharge(i));
		}
		for(int i=0; i<getAllBonds(); i++) {
			int order = getBondOrder(i);
			int a1 = ff2em[getBondAtom(0, i)]-1;
			int a2 = ff2em[getBondAtom(1, i)]-1;
			if(a1>=0 && a2>=0 && order==1) {
				m.addBond(a1, a2, ExtendedMolecule.cBondTypeSingle);
			} else if(a1>=0 && a2>=0 && order==2) {
				m.addBond(a1, a2, ExtendedMolecule.cBondTypeDouble);
			} else if(a1>=0 && a2>=0 && order==3) {
				m.addBond(a1, a2, ExtendedMolecule.cBondTypeTriple);
			} else {
				throw new IllegalArgumentException("Invalid bond : "+a1+"-"+a2+"=" +order+ "  was "+getBondAtom(0, i)+"-"+getBondAtom(1, i)+" atoms="+getAllAtoms());
			}
		}		
	}

	@Override
	public int compareTo(FFMolecule o) {
		return o==null? 1: getName().compareTo(o.getName());
	}

	public void copyCoordinates(Coordinates[] coords) {		
		System.arraycopy(coords, 0, this.coords, 0, Math.min(coords.length, this.coords.length));
	}
	
	@Override
	public int hashCode() {
		return name==null? 0 : name.hashCode();
	}
		
}
