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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.interaction.KeyAssigner;
import com.actelion.research.forcefield.interaction.ProteinLigandTerm;
import com.actelion.research.forcefield.mm2.MM2TermList;
import com.actelion.research.util.ArrayUtils;

/**
 * The TermList contains and manage the terms of a forcefield
 * 
 */
public abstract class TermList implements Cloneable {
	
	protected int nTerms = 0;
	protected int nProteinLigandTerms = 0;
	protected AbstractTerm[] terms = new AbstractTerm[500];
	protected ProteinLigandTerm[] proteinLigandTerms = new ProteinLigandTerm[500];	
	protected FFMolecule mol;

	
	public void setMolecule(FFMolecule mol) {
		this.mol = mol;
	}
	
	public final FFMolecule getMolecule() {
		return mol;
	}


	/**
	 * Called at the forcefield initialization (once) to set the atom classes
	 * @param config
	 */
	public abstract void prepareMolecule(FFMolecule mol, FFConfig config);
	
	/**
	 * Called before any energy calculation to recreate the terms
	 * @param config
	 */
	public abstract void init(FFMolecule mol, FFConfig config);
	
	
	/**
	 * Used by the preoptimizer, returns the expected bond distance between mol.getAtom(a1) and mol.getAtom(a2)
	 * @param a1
	 * @param a2
	 * @return
	 */
	public abstract double getBondDistance(int a1, int a2);
	
	/**
	 * Called before running Genetic Algorithm, to optimize the terms
	 */
	/**
	 * Aggregate terms to speed up the calculation process.
	 * The potential is calculated 
	 * 
	 */
	public final void aggregateTerms() {
		//Calculate the bounds
		final Coordinates[] bounds = StructureCalculator.getBounds(mol);		
		//System.out.println("Used before: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024+" / "+Runtime.getRuntime().maxMemory()/1024);
		
		MM2TermList[] tl = new MM2TermList[mol.getNMovables()];
		for (int i = 0; i < tl.length; i++) tl[i] = new MM2TermList(this);

		loop: for (int j = 0; j < size(); j++) {
			AbstractTerm t = get(j);
			if(!(t instanceof ProteinLigandTerm)) continue;
			
			//Check if this term can be aggregated i.e. only one movable term 'a'
			int a = -1;
			for (int k = 0; k < t.getAtoms().length; k++) {
				if(t.getAtoms()[k]<mol.getNMovables()) {
					if(a<0) a = t.getAtoms()[k];
					else continue loop;					
				}
			}
			
			if(a>=0 /*&& (t instanceof SuperposeTerm || mol.getAtomicNo(a)==6)*/) {
				tl[a].add(remove(j--));
			}				
		}

		//Create a Map of atomtype -> list of movable atoms
		final Map<Integer, List<Integer>> classIdToAtom = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < tl.length; i++) {
			if(tl[i].size()>0) {
				List<Integer> l = classIdToAtom.get(mol.getAtomInteractionClass(i));
				if(l==null) {
					l = new ArrayList<Integer>();
					classIdToAtom.put(mol.getAtomInteractionClass(i), l);
				}
				l.add(i);
			}
		}
		
		//Sort this list by size to process first the most frequent atomtypes
		final List<List<Integer>> values = new ArrayList<List<Integer>>(classIdToAtom.values());
		Collections.sort(values, new Comparator<List<Integer>>() {
			@Override
			public int compare(List<Integer> l0, List<Integer> l1) {
				return l1.size()-l0.size();
			}		
		});		
		final List<Integer> orderOfProcessing = new ArrayList<Integer>();
		for(List<Integer> l: values) orderOfProcessing.addAll(l);
		
		long free = Runtime.getRuntime().maxMemory()-(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());		
//		int n2 = 0;
		Map<Integer, GridTerm> map = new HashMap<Integer, GridTerm>();
		for (int i: orderOfProcessing) {
			//Add the new aggregated term if needed
			if(tl[i].size()>0) {
				
				final double gridSize =  (tl[i].get(0) instanceof SuperposeTerm)? .5: mol.getAtomicNo(i)==6? .75:.4;
				
				
				GridTerm gt = map.get(mol.getAtomInteractionClass(i));
				if(gt==null) {
					final int size = (int)((bounds[1].x-bounds[0].x)/gridSize *(bounds[1].y-bounds[0].y)/gridSize *(bounds[1].z-bounds[0].z)/gridSize);  
					final int mem = (16*4)*size;
					if(free>4*1024*1024+mem && classIdToAtom.get(mol.getAtomInteractionClass(i)).size()>0) {
						free-=mem;
						gt = new GridTerm(getMolecule(), i, tl[i], bounds, gridSize);
//						n1+=tl[i].size();
//						n2++;
						map.put(mol.getAtomInteractionClass(i), gt);
						//System.out.println("aggregate " + StatisticalPreference.keyAssigner.getSubKey(mol, i)+" "+classIdToAtom.get(mol.getAtomClassId(i)).size()+" TermList");
					} else {
						if(classIdToAtom.get(mol.getAtomInteractionClass(i)).size()>1) System.out.println("Memory low!!! Cannot aggregate "+KeyAssigner.getSubKey(mol, i)+" "+classIdToAtom.get(mol.getAtomInteractionClass(i)).size()+" TermList: "+free/(1024*1024)+"M free");
						for (int j = 0; j < tl[i].size(); j++) add(tl[i].get(j));
						continue;
					}
				} else {
					gt = new GridTerm(getMolecule(), i, gt);
//					n1+=tl[i].size();
				}					
				add(gt);
				
			}
		}
		//System.out.println("#TERMS="+size()+" terms with "+n2+" gridTerms" );
		//System.out.println("Used after: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024+" / "+Runtime.getRuntime().maxMemory()/1024);
	
	}	
	public void clear() {
		nTerms = 0;
		nProteinLigandTerms = 0;
	}
	
	public final int size() {
		return nTerms + nProteinLigandTerms;
	}
	
	public final AbstractTerm get(int i) {
		return i<nTerms? terms[i]: proteinLigandTerms[i-nTerms];
	}

	public final void add(AbstractTerm t) {
		if(t instanceof ProteinLigandTerm ) {
			if(proteinLigandTerms.length<=nProteinLigandTerms) proteinLigandTerms = (ProteinLigandTerm[]) ArrayUtils.resize(proteinLigandTerms, nProteinLigandTerms*3+100);
			proteinLigandTerms[nProteinLigandTerms++] = (ProteinLigandTerm) t;			
		} else {
			if(terms.length<=nTerms) terms = (AbstractTerm[]) ArrayUtils.resize(terms, nTerms*2+100);
			terms[nTerms++] = t;
		}
	} 

	public final void addAll(Collection<? extends AbstractTerm> terms) {
		for (AbstractTerm t : terms) {
			add(t);			
		}
	}

	public final AbstractTerm remove(AbstractTerm t) {		
		if(t instanceof ProteinLigandTerm) {			
			for (int i = 0; i < proteinLigandTerms.length; i++) {
				if(proteinLigandTerms[i]==t) {
					return remove(i+nTerms);
				}
			}
		} else {
			for (int i = 0; i < terms.length; i++) {
				if(terms[i]==t) {
					return remove(i);
				}
			}
			
		}
		return null;
	}
	
	public final AbstractTerm remove(int i) {
		AbstractTerm res = null;
		if(i<nTerms) {
			res = terms[i];
			nTerms--;
			terms[i] = terms[nTerms];
		} else if(i<nTerms + nProteinLigandTerms) {
			i-=nTerms;
			res = proteinLigandTerms[i];
			nProteinLigandTerms--;
			proteinLigandTerms[i] = proteinLigandTerms[nProteinLigandTerms];
			//for (int j = i; j < nProteinLigandTerms; j++) proteinLigandTerms[j] = proteinLigandTerms[j+1];									
		} else {
			throw new IllegalArgumentException("Invalid index: "+i+" / "+nTerms+", "+(nTerms+nProteinLigandTerms));
		}
		return res;
	} 	
	
	/**
	 * Compute the forces and the gradient in the cartesian referential 
	 * @param d
	 * @return
	 */
	public final double getFGValue(final Coordinates[] d) {
		double energy = 0; 
		
		//Reset derivates
		if(d!=null) for(int i=0; i<d.length; i++) {d[i] = new Coordinates();}
		 
		//Compute Derivates		
		for(int k=0; k<size(); k++) {
			AbstractTerm term = get(k);
			double e = term.getFGValue(d);
			energy += e;
		}
		return energy;
	}
	
	public final double getInteractionEnergy() {
		double sum = 0;		
		for(int i=0; i<size(); i++) {
			AbstractTerm term = get(i);
			if(term.isExtraMolecular() ) {
				double e = term.getFGValue(null); 
				sum += e;
			}
		}
		return sum;
	}

	public final double getStructureEnergy() {
		double sum = 0;
		for(int i=0; i<size(); i++) {
			AbstractTerm term = get(i);
			if(term.isExtraMolecular() || !mol.isAtomFlag(term.getAtoms()[0], FFMolecule.LIGAND)) continue;
			sum += term.getFGValue(null);
		}
		return sum;
	}
	
	/**
	 * Gets the energy relative to the atoms given in parameters
	 * @param which
	 * @return
	 */
	public final double getEnergy(boolean[] which) {
		double sum = 0;
		loop: for(int k=0; k<size(); k++) {
			AbstractTerm term = get(k);
			int[] atoms = term.getAtoms();
			for(int i=0; i<atoms.length; i++) {
				if(which!=null && !which[atoms[i]]) continue loop;
			}				 
			sum += term.getFGValue(null);
		}
		return sum;
	}

	/**
	 * Gets the energy attributed to the atom given in parameter
	 * @param which
	 * @return
	 */
	public final double getEnergy(int which, boolean intraMolecular, boolean extraMolecular) {
		double sum = 0;
		for(int k=0; k<size(); k++) {
			AbstractTerm term = get(k);
			if(!intraMolecular && !term.isExtraMolecular()) continue;
			if(!extraMolecular && term.isExtraMolecular()) continue;
			int[] atoms = term.getAtoms();
			boolean ok = false;
			int n = 0;
			for(int i=0; i<atoms.length; i++) {
				if(atoms[i]==which) ok = true;
				if(mol.isAtomFlag(atoms[i], FFMolecule.LIGAND) && mol.getAtomicNo(atoms[i])>1) n++;
			}				 
			if(ok && n>0) {
				sum += term.getFGValue(null) / n;
			}
		}
		return sum;
	}	
	
	@Override
	public TermList clone() {
		try {
			return (TermList) super.clone();
		} catch(CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public String toString() {		
		return "["+getClass()+"]";
	}

}
