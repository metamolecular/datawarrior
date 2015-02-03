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
package com.actelion.research.forcefield;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.mm2.MM2Config;
import com.actelion.research.forcefield.mm2.MM2Parameters;
import com.actelion.research.forcefield.mm2.MM2TermList;
import com.actelion.research.util.PriorityQueue;

/**
 * Generic Forcefield implementation, using MM2 by default, with its standard parameters 
 * 
 */
public class ForceField implements Cloneable {
	

	/** The MM2 parameters */
	private static final FFParameters parameters = MM2Parameters.getInstance();
		
	private final FFConfig config;
	private FFMolecule mol;	
	private TermList terms = null;

	public ForceField(FFMolecule mol) {		
		this(mol, new MM2Config());
	}
	public ForceField(FFMolecule mol, FFConfig config) {
		this(mol, new MM2TermList(), config);
	}
	public ForceField(FFMolecule mol, TermList terms, FFConfig config) {
		if(mol.getAllAtoms()>28000) throw new IllegalArgumentException("The molecule is too big: "+mol.getAllAtoms()+" atoms");
		this.mol = mol;
		this.terms = terms;
		this.config = config;
		terms.prepareMolecule(mol, config); 
	}
	
//	public void init() {
//		
//		FFConfig config = getConfig();
//		FFMolecule mol = getMolecule();
//
//		boolean changed = false;
//		//Add the hydrogens (or remove), without placing them
//		if(!(config instanceof FFConfig.PreoptimizeConfig)) {
//			if(config.isAddHydrogens()) {
//				changed = StructureCalculator.addHydrogens(mol, config.isUseHydrogenOnProtein()) || changed;
//			} else {
//				changed = StructureCalculator.deleteHydrogens(mol) || changed;
//			}
//		}
//		
//		//Set the MM2 atom classes
//		MM2Parameters.getInstance().setAtomClassesForMolecule(mol);
//
//		//Add the lone pairs
//		if(!(config instanceof FFConfig.PreoptimizeConfig)) {
//			if(config.isAddHydrogens() && config.isAddLonePairs()) changed = getParameters().addLonePairs(mol) || changed;
//		}
//		
//		//Set the interactions atom classes
//		if(config.isUsePLInteractions()) config.getClassStatistics().setClassIdsForMolecule(mol);
//		
//		//Preoptimize the H
//		if(changed &&  !(config instanceof FFConfig.PreoptimizeConfig)) {
//			PreOptimizer.preOptimizeHydrogens(mol);
//			
//		}
//
//		terms.clear();	
//
//	}
	
	@Override
	public String toString() {
		initTerms();
		return terms.toString();
	}
	
	public String getEnergyBreakdown() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<getTerms().size(); i++) {
			sb.append(getTerms().get(i)+System.getProperty("line.separator"));
		}
		sb.append(System.getProperty("line.separator"));
		sb.append(toString());
		return sb.toString();
	}
	
	public String getEnergy(boolean mainTermOnly) {
		
		StringBuffer sb = new StringBuffer();
		//PriorityQueue all = new PriorityQueue();
		List<AbstractTerm> all = new ArrayList<AbstractTerm>(); 
		for(int i=0; i<getTerms().size(); i++) {
			AbstractTerm t = getTerms().get(i);
			double v = t.getFGValue(null);
			if(Math.abs(v)>1 || !mainTermOnly) all.add(t/*, -Math.abs(v)*/);
		}

		for(int i=0; i<all.size(); i++) {
			AbstractTerm t = (AbstractTerm) all.get(i);
			sb.append(t+System.getProperty("line.separator"));
		}
		sb.append(System.getProperty("line.separator"));
		//sb.append(toString());
		return sb.toString();
	}
	
	public String getEnergyInterMolecularMain() {
		
		StringBuffer sb = new StringBuffer();
		
		PriorityQueue<AbstractTerm> all = new PriorityQueue<AbstractTerm>();
		for(int i=0; i<getTerms().size(); i++) {			
			AbstractTerm t = getTerms().get(i);
			if(!t.isExtraMolecular()) continue;
			double v = t.getFGValue(null);
			if(Math.abs(v)>1) all.add(t, -Math.abs(v));
		}

		for(int i=0; i<all.size(); i++) {
			AbstractTerm t = (AbstractTerm) all.get(i);
			sb.append(t+System.getProperty("line.separator"));
		}
		sb.append(System.getProperty("line.separator"));
		sb.append(toString());
		return sb.toString();
	}	
	
	public void initTerms() {
		terms.init(mol, config);
	}
	
	
	public FFMolecule getMolecule() {
		return mol;
	}

	/**
	 * @return
	 */
	public TermList getTerms() {
		if(terms.size()==0) {
			initTerms();
		} 
		return terms;
	}

	/**
	 * @return
	 */
	public FFParameters getParameters() {
		return parameters;
	}


	/**
	 * @return
	 */
	public FFConfig getConfig() {
		return config;
	}

//
//	/**
//	 * Recreates and preoptimize the hydrogens. This function will
//	 * remove all extra hydrogens and add all missing hydrogens
//	 * Warning: this function changes the atom's order
//	 * @param forcefield
//	 * @param atms
//	 */
//	public void recreateHydrogens(int[] atms) {
//		FFMolecule mol = getMolecule();
//		
//		if(atms!=null) {
//			//Delete the current hydrogens (and lone pairs) 
//			List<Integer> atomsToBeDeleted = new ArrayList<Integer>();
//			for(int k=0; k<atms.length; k++) {
//				int atm = atms[k];
//				if(atm<0 || mol.isAtomFlag(atm, FFMolecule.RIGID)) continue;
//				
//				int nH = StructureCalculator.getImplicitHydrogens(mol, atm);
//				
//				for (int i = 0; nH<0 && i<mol.getAllConnAtoms(atm) ; i++) {
//					if(mol.getAtomicNo(mol.getConnAtom(atm, i))<=1) {
//						atomsToBeDeleted.add(new Integer(mol.getConnAtom(atm, i)));
//						nH++;
//					} 
//				}
//				
//				//Add and preoptimize the new hydrogens
//				for(int i=0; i<nH; i++) {
//					int a = mol.addAtom(1);
//					mol.setAtomFlags(a, mol.getAtomFlags(atm));
//					mol.setAtomFlag(a, FFMolecule.PREOPTIMIZED, false);
//					mol.addBond(atm, a, 1);
//				}
//			}
//			Collections.sort(atomsToBeDeleted);			
//			for(int i=atomsToBeDeleted.size()-1; i>=0; i--) {
//				mol.deleteAtom(atomsToBeDeleted.get(i));
//			}
//		} else {
//			int N = Math.min(mol.getNMovables(), mol.getAllAtoms());
//			for(int i=N-1; i>=0; i--) {
//				if(mol.getAtomicNo(i)<=1) mol.deleteAtom(i);
//			}
//			StructureCalculator.addHydrogens(mol);
//		}
//		init();
//		PreOptimizer.preOptimize(this);
//	}
	
	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public ForceField clone()  {
		
		try {
			FFMolecule m = new FFMolecule(mol);
			ForceField copy = new ForceField(m, config);
			if(terms!=null) {
				copy.terms = terms.clone();
				copy.terms.setMolecule(m);
			}
			
			return copy;
		} catch (Exception e) {
			e.printStackTrace();
			return this;
		}
		/*
		try {
			return (ForceField) super.clone();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}*/
	}


}
