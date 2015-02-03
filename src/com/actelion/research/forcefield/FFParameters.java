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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.util.ArrayUtils;
import com.actelion.research.util.MultipleIntMap;


/**
 * 
 */
public abstract class FFParameters {

	protected static boolean DEBUG = true;
	
	/**
	 * Description of an atom's class
	 * @author freyssj
	 */
	public static class AtomClass {
		public AtomClass(int number, int atomicNo, String description, double charge, int doubleBonds, int tripleBonds, int[] replacement) {
			this.atomClass = number;
			this.atomicNo = atomicNo;
			this.description = description;
			this.doubleBonds = doubleBonds;
			this.tripleBonds = tripleBonds;
			this.charge = charge;
			this.replacement = replacement;
		}
		
		public final int atomClass;
		public final int atomicNo;
		public final String description;
		public final int doubleBonds, tripleBonds;
		public final double charge;
		/** Replacement is an array of atomClass, cost used to define
		 * which atom can be used to replace this one in case of missing parameters
		 */
		public final int[] replacement;
		
		@Override
		public String toString() {
			return description;			
		}
	}
	
	public static class BondParameters {
		public BondParameters(double fc, double eq) {
			this.fc = fc;
			this.eq = eq;
		}
		@Override
		public String toString() { return "Bond: " + fc + " " + eq;}
		public double fc;
		public double eq;
	}
	
	public static class AngleParameters {
		public AngleParameters(double fc, double eq) {
			this.fc = fc;
			this.eq = eq;
		}
		@Override
		public String toString() { return "Angle: " + fc + " " + eq;}
		public double fc;
		public double eq;
	}

	public static class TorsionParameters {
		public TorsionParameters(double v1, double v2, double v3) {
			this.v1 = v1;
			this.v2 = v2;
			this.v3 = v3;
		}
		@Override
		public String toString() { return "Torsion: " + v1 + " " + v2 + " " + v3;}
		public double v1, v2, v3;
	}
	
	public static class SingleVDWParameters {
		public SingleVDWParameters(double radius, double epsilon, double reduct) {
			this.radius = radius;
			this.epsilon = epsilon;
			this.reduct = reduct;
		}
		
		public double radius;	
		public double epsilon;	
		public double reduct;	
	}
	
	public static class OutOfPlaneBendParameters {
		public OutOfPlaneBendParameters(double fopb) {
			this.fopb = fopb;
		}
		
		public double fopb;	
	}
	
	public static class VDWParameters {
		public VDWParameters(double radius, double esp) {
			this.radius = radius;
			this.esp = esp;
		}
		@Override
		public String toString() { return "VDW: " + radius + " " + esp;}
		public double radius, esp;
	}
	
	

	protected static final Map<String, AtomClass> descriptionToAtom = new ConcurrentHashMap<String, AtomClass>();	 
	protected final Map<Integer, AtomClass> classNoToAtom = new ConcurrentHashMap<Integer, AtomClass>();	 
	protected final MultipleIntMap<BondParameters> bondParameters = new MultipleIntMap<BondParameters>(2);
	protected final MultipleIntMap<Double> dipoleParameters = new MultipleIntMap<Double>(2);
	protected final MultipleIntMap<TorsionParameters> torsionParameters = new MultipleIntMap<TorsionParameters>(5);
	protected final MultipleIntMap<AngleParameters> angleParameters = new MultipleIntMap<AngleParameters>(5);
	protected final Map<Integer, SingleVDWParameters> singleVDWParameters = new ConcurrentHashMap<Integer, SingleVDWParameters>();
	protected final MultipleIntMap<VDWParameters> vdwParameters = new MultipleIntMap<VDWParameters>(2);
	protected final MultipleIntMap<OutOfPlaneBendParameters> outOfPlaneBendParameters = new MultipleIntMap<OutOfPlaneBendParameters>(2);	
	protected final Map<Integer, double[]> strBendParameters = new ConcurrentHashMap<Integer, double[]>();	
	protected final Map<String, double[]> piAtoms = new ConcurrentHashMap<String, double[]>();	
	protected final Map<String, double[]> piBonds = new ConcurrentHashMap<String, double[]>();	
	protected final MultipleIntMap<Double> electronegativity = new MultipleIntMap<Double>(3);	

	public abstract void setAtomClassesForMolecule(FFMolecule mol);
	public abstract boolean addLonePairs(FFMolecule mol);
	
	public Collection<AtomClass> getAtomClasses() {
		return descriptionToAtom.values();	
	}
	public AtomClass getAtomClass(int classNo) {
		return classNoToAtom.get(classNo);	 
	}
	
	public String getDescription(int classNo) {
		AtomClass a = classNoToAtom.get(classNo);
		if(a==null) return null;
		return a.description;
	}
	
	/**
	 * Get the atomType matching the description
	 * @param desc
	 * @return
	 */
	public AtomClass getAtomType(String desc) {		
		AtomClass res = descriptionToAtom.get(desc);	 
		if(res!=null) return res;
		
		for (String s : descriptionToAtom.keySet()) {
			if(s.equalsIgnoreCase(desc) || s.replace(" ", "").equalsIgnoreCase(desc)) return descriptionToAtom.get(s);
		}
		System.err.println("Invalid atomType: "+desc);
		return null;
	}
	
	public int getAtomClass(String desc) {		
		AtomClass at = descriptionToAtom.get(desc);
		return at==null?-1:at.atomClass;
	}
	
	public BondParameters getBondParameters(int n1, int n2) {
		int[] key = n1<n2? new int[]{n1, n2}:  new int[]{n2, n1};		
		 
		BondParameters p = (BondParameters) bondParameters.get(key);
		if(p==null) {
			int bestCost = 1000;
			BondParameters bestP = null;
//			int[] bestKey = null;
			for(int[] k: bondParameters.keys()) {
				int cost = Math.min(cost(n1, k[0]) + cost(n2, k[1]),  cost(n1, k[1]) + cost(n2, k[0]));
				
				if(cost<bestCost) {
					bestCost = cost;
					bestP = (BondParameters) bondParameters.get(k);
//					bestKey = k;
				}
			}
			if(bestP!=null) {
//				logger.fine("approximate Bond for " + ArrayUtils.toString(key) + " with "+ArrayUtils.toString(bestKey)+" (cost:"+bestCost+")");
				bondParameters.put(key, bestP);				
				p = bestP;
			}
			if(p==null) {
//				logger.warning("no Bond parameters between "+n1 + " and "+n2);
				p = new BondParameters(0.5, 1.5);
				if(n1>=0 && n2>=0) bondParameters.put(key, p);				
			} 
		}
		return p;
	}
	
	public OutOfPlaneBendParameters getOutOfPlaneBendParameters(int n1, int n2) {
		int[] key = n1<n2? new int[]{n1, n2}: new int[]{n2, n1};
		OutOfPlaneBendParameters p = null;
		p = (OutOfPlaneBendParameters) outOfPlaneBendParameters.get(key);
		return p;
	}
	
	public double getDipoleParameters(int n1, int n2) {
		if(n1<n2) {
			Double res = (Double) dipoleParameters.get(new int[]{n1,n2});
			return res==null? 0: res.doubleValue();
		} 
		Double res = (Double) dipoleParameters.get(new int[]{n2,n1});
		return res==null? 0:  -res.doubleValue();
	}
	
	public AngleParameters getAngleParameters(int n1, int n2, int n3, int nHydrogen, int ringSize) {
		if(ringSize>4) ringSize = 0;
		int[] key;
		if(n1<n3) key = new int[]{n1, n2, n3, nHydrogen, ringSize};
		else key = new int[]{n3, n2, n1, nHydrogen, ringSize};
		
		AngleParameters p = null;
		p = (AngleParameters) angleParameters.get(key);

		if(p==null) {
			int bestCost = 1000;
			AngleParameters bestP = null;
//			int[] bestKey = null;
			synchronized (angleParameters) {
				
				for(int[] k: angleParameters.keys()) {
					int cost = Math.min(cost(n1, k[0]) + cost(n2, k[1])*10 + cost(n3, k[2]),
						cost(n3, k[0]) + cost(n2, k[1])*10 + cost(n1, k[2])) +
						Math.abs(k[3]-nHydrogen)*2 + Math.abs(ringSize-k[4])*5;
					if(cost<bestCost) {
						bestCost = cost;
						bestP = (AngleParameters) angleParameters.get(k);
//						bestKey = k;
					}
				}
			}
			if(bestP!=null) {
//				logger.fine("approximate Angle for " + ArrayUtils.toString(key) +" with "+ArrayUtils.toString(bestKey)+" (cost:"+bestCost+")");
				angleParameters.put(key, bestP);
				p = bestP;				
			}
		}

		if(p==null) {
			if(DEBUG) System.err.println("Guessed Angle for " + ArrayUtils.toString(key));
			p = new AngleParameters(0.5, 120);
			angleParameters.put(key, p);
		} 
		return p;
	}
	
	public double getElectronegativity(int n1, int n2, int n3) {
		Double res = (Double) electronegativity.get(new int[]{n1, n2, n3});
		return res!=null? res.doubleValue(): 0;
	}

	public TorsionParameters getTorsionParameters(int n1, int n2, int n3, int n4, int ringSize) {
		int[] key;
		if(ringSize!=4) ringSize = 0;
		if(n1<n4 || (n1==n4 && n2<=n3)) key = new int[]{n1, n2, n3, n4, ringSize};		
		else key = new int[]{n4, n3, n2, n1, ringSize};	

		TorsionParameters p = (TorsionParameters) torsionParameters.get(key);
		
		if(p==null) {
			int bestCost = 1000;
			TorsionParameters bestP = null;
//			int[] bestKey = null;
			for(int[] k: torsionParameters.keys()) {
				int cost = Math.min(cost(n1, k[0])*10 + cost(n2, k[1]) + cost(n3, k[2]) + cost(n4, k[3])*10,
						cost(n1, k[3])*10 + cost(n2, k[2]) + cost(n3, k[1]) + cost(n4, k[0])*10);
				if(cost<bestCost) {
					bestCost = cost;
					bestP = (TorsionParameters) torsionParameters.get(k);
//					bestKey = k;
				}
			}
			if(bestP!=null) {
//				logger.fine("approximate torsion for " + ArrayUtils.toString(key) +" with "+ArrayUtils.toString(bestKey)+" (cost:"+bestCost+")");
				torsionParameters.put(key, bestP);			
				p = bestP;	
			}
		}
		
		if(p==null) {
				
			if(DEBUG) System.err.println("no Torsion parameters for " + ArrayUtils.toString(key));
			p = new TorsionParameters(0, 0, 0);
			torsionParameters.put(key, p);			
		} 
		return p;
	}
	
	/**
	 * Returns the costs of replacing class n1 by class n2
	 * @return the cost or 1000 if none is found
	 */
	private int cost(int n1, int n2) {
		if(n1==n2) return 0;
		AtomClass claz = classNoToAtom.get(n1);
		if(claz==null) {System.err.println("invalid class "+n1); return 1000;}  
		if(claz.replacement==null) return 1000;
		for(int i=0; i<claz.replacement.length; i+=2) {
			if(claz.replacement[i]==n2) return claz.replacement[i+1]*3+(i/2);
		}
		return 1000;
	}
	
	public SingleVDWParameters getSingleVDWParameters(int n1) {
		SingleVDWParameters p = singleVDWParameters.get(n1);
		if(p==null && n1>=0) {
			if(DEBUG) System.err.println("no Single VDW parameters for " + n1);
		}
		return p;
	}


	public VDWParameters getVDWParameters(int n1, int n2) {
		int[] key = n1<n2? new int[]{n1,n2}: new int[]{n2, n1};		
		VDWParameters p = (VDWParameters) vdwParameters.get(key);
		return p;
	}
	
	public double[] getPiAtom(int n1) {		
		return piAtoms.get(""+n1);
	}
	
	public double[] getPiBond(int n1, int n2) {
		String key = n1<n2? n1+"-"+n2: n2+"-"+n1;		
		return piBonds.get(key);
	}
	
	public double getStretchBendParameter(int n1, int nHydro) {
		double[] res = strBendParameters.get(n1);
		if(nHydro==2) nHydro = 1;
		return res==null? 0: res[nHydro];
	}
	

	
}
