package com.actelion.research.forcefield.mm2;

import java.text.DecimalFormat;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFConfig;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.forcefield.TermList;
import com.actelion.research.forcefield.interaction.ProteinLigandTerm;
import com.actelion.research.forcefield.optimizer.PreOptimizer;
import com.actelion.research.util.MultipleIntMap;

/**
 * The TermList contains and manage the terms of a forcefield
 * 
 * @author freyssj
 */
public class MM2TermList extends TermList implements Cloneable {

	private final static FFParameters parameters = MM2Parameters.getInstance();
	private MM2Config config;
	private PISCF scf;

	private MultipleIntMap<Double> bondDistance = new MultipleIntMap<Double>(2);

	public MM2TermList(TermList tl) {
		setMolecule(tl.getMolecule());
	}
	
	public MM2TermList() {
	}
	
	@Override
	public void setMolecule(FFMolecule mol) {
		super.setMolecule(mol);
	}

	
	public final BondTerm addBondTerm(int a1, int a2) {
		BondTerm term = BondTerm.create(this, a1, a2);
		if(term!=null) add(term);
		return term;
	}	
	public final AngleTerm addAngleTerm(int atm1, int atm2, int atm3, boolean isInPlaneAngleEnabled) {
		AngleTerm term = AngleTerm.create(this, atm1, atm2, atm3, isInPlaneAngleEnabled);
		if(term!=null) add(term);
		return term;
	}		
	public final OutOfPlaneAngleTerm addOutOfPlaneBendTerm(int a1, int a2, int a3, int a4) {
		OutOfPlaneAngleTerm term = OutOfPlaneAngleTerm.create(this, a1, a2, a3, a4);
		if(term!=null) add(term);
		return term;
	}
	public final InPlaneAngleTerm addInPlaneAngleTerm(int a1, int a2, int a3, int a4) {
		InPlaneAngleTerm term = InPlaneAngleTerm.create(this, a1, a2, a3, a4);
		if(term!=null) add(term);
		return term;
	}
	public final StretchBendTerm addStretchBendTerm(int a1, int a2, int a3, BondTerm t1, BondTerm t2) {
		StretchBendTerm term = StretchBendTerm.create(this, a1, a2, a3, t1, t2);
		if(term!=null) add(term);
		return term;
	}
	public final DipoleTerm addDipoleTerm(int a1, int a2, int a3, int a4) {
		DipoleTerm term = DipoleTerm.create(this, a1, a2, a3, a4);
		if(term!=null) add(term);
		return term;
	}
	public final ChargeDipoleTerm addChargeDipoleTerm(int a1, int a2, int a3) {
		ChargeDipoleTerm term = ChargeDipoleTerm.create(this, a1, a2, a3);
		if(term!=null) add(term);
		return term;
	}
	public final TorsionTerm addTorsionTerm(int a1, int a2, int a3, int a4) {
		TorsionTerm term = TorsionTerm.create(this, a1, a2, a3, a4);
		if(term!=null) add(term);
		return term;
	}	
	public final AbstractTerm addVDWTerm(int a1, int a2) {
		AbstractTerm term = VDWLNTerm.create(this, a1, a2);
		if(term!=null) add(term);
		return term;
	}	
	public final AbstractTerm addVDW48Term(int a1, int a2) {
		AbstractTerm term = VDWLN48Term.create(this, a1, a2);
		if(term!=null) add(term);
		return term;
	}	
	public final AbstractTerm addVDW48NoHTerm(int a1, int a2) {
		AbstractTerm term = VDWLN48TermNoH.create(this, a1, a2);
		if(term!=null) add(term);
		return term;
	}	
	public final ChargeTerm addChargeTerm(int a1, int a2) {
		ChargeTerm term = ChargeTerm.create(this, a1, a2);
		if(term!=null) add(term);
		return term;
	}	
	
	public final ProteinLigandTerm addProteinLigandTerm(int proteinAtm, int ligandAtm) {
		ProteinLigandTerm term = ProteinLigandTerm.create(getConfig().getClassStatistics(), getMolecule(), proteinAtm, ligandAtm);		
		if(term!=null) add(term);
		return term;
	}
		
	public final double getInteractionEnergy(double maxDist) {
		double sum = 0;		
		for(int i=0; i<size(); i++) {
			AbstractTerm term = get(i);
			if(term.isExtraMolecular() ) {
				double d = mol.getCoordinates(term.getAtoms()[0]).distance(mol.getCoordinates(term.getAtoms()[1]));
				if(d>maxDist) continue;
				double e = term.getFGValue(null); 
				sum += e;
			}
		}
		return sum;
	}
	public final double getInteractionEnergyH() {
		double sum = 0;
		
		for(int i=0; i<size(); i++) {
			AbstractTerm term = get(i);
			if(term.isExtraMolecular() && (mol.getAtomicNo(term.getAtoms()[0])==1 || mol.getAtomicNo(term.getAtoms()[1])==1)) {
				double e = term.getFGValue(null); 
				sum += e;
			}
		}

		return sum;
	}
	/*
	public final double getInteractionEnergyVDW() {
		double sum = 0;
		for(int a1=0; a1<mol.getNMovables(); a1++) {
			for(int a2=a1+1; a2<mol.getAllAtoms(); a2++) {
				
				if(mol.isAtomFlag(a1, FFMolecule.RIGID) && mol.isAtomFlag(a2, FFMolecule.RIGID)) continue;
				
				boolean interMolecular = (mol.isAtomFlag(a1, FFMolecule.LIGAND) && !mol.isAtomFlag(a2, FFMolecule.LIGAND)) 
						|| (mol.isAtomFlag(a2, FFMolecule.LIGAND) && !mol.isAtomFlag(a1, FFMolecule.LIGAND));
				
				if(interMolecular && mol.getAtomicNo(a1)>1 && mol.getAtomicNo(a2)>1){							
					VDWLNTerm t = VDWLNTerm.create(this, a1, a2);
					sum += t.getFGValue(null);
				}
				
			}
		}

		return sum;
	}	*/
	
	public final double getStructureEnergyNoH() {
		double sum = 0;
		loop: for(int i=0; i<size(); i++) {
			AbstractTerm term = get(i);
			if(term.isExtraMolecular()) continue;
			for (int j = 0; j < term.getAtoms().length; j++) {
				if(mol.getAtomicNo(term.getAtoms()[j])<=1) continue loop;
			}
			sum += term.getFGValue(null);
		}
		return sum;
	}	
	public final double getStructureEnergySmooth() {
		double sum = 0;
		for(int i=0; i<size(); i++) {
			AbstractTerm term = get(i);
			if(term.isExtraMolecular()) continue;
			if((term instanceof VDWLNTerm) || (term instanceof VDWLN48Term) || (term instanceof DipoleTerm) || (term instanceof TorsionTerm)  || (term instanceof OutOfPlaneAngleTerm) ) {
				sum += term.getFGValue(null);
			}
		}
		return sum;
	}	
	public final double getProteinEnergy() {
		double sum = 0;
		terms: for (int i = 0; i < size(); i++) {
			AbstractTerm term = get(i);
			int[] a = term.getAtoms();
			for (int j = 0; j < a.length; j++) if(mol.isAtomFlag(a[j], FFMolecule.LIGAND)) continue terms;
			sum += term.getFGValue(null);			
		}
		return sum;
	} 
		
	 
	public final double getVDW() {
		double energy = 0; 
		for(int k=0; k<size(); k++) {
			AbstractTerm term = get(k);
			if(term instanceof VDWLNTerm || term instanceof VDWLN48Term) {
				double e = term.getFGValue(null);
				energy += e;				
			}
		}
		return energy;
		
	}
	
	@Override
	public void prepareMolecule(FFMolecule mol, FFConfig conf) {
		MM2Config config = (MM2Config) conf;
		
		boolean changed = false;
		// Add the hydrogens (or remove), without placing them
		if (!(config instanceof MM2Config.PreoptimizeConfig)) {
			if (config.isAddHydrogens()) {
				changed = StructureCalculator.addHydrogens(mol, config.isUseHydrogenOnProtein()) || changed;
			} else {
				changed = StructureCalculator.deleteHydrogens(mol) || changed;
			}
		}

		//Set the MM2 atom classes
		MM2Parameters.getInstance().setAtomClassesForMolecule(mol);

		// Add the lone pairs
		if (!(config instanceof MM2Config.PreoptimizeConfig)) {
			if (config.isAddHydrogens() && config.isAddLonePairs())
				changed = parameters.addLonePairs(mol) || changed;
		}

		// Set the interactions atom classes
		if (config.isUsePLInteractions())
			config.getClassStatistics().setClassIdsForMolecule(mol);

		// Preoptimize the H
		if (changed && !(config instanceof MM2Config.PreoptimizeConfig)) {
			PreOptimizer.preOptimizeHydrogens(mol);
		}

	}
	
	@Override
	public void init(FFMolecule mol, FFConfig conf) {
		MM2Config config = (MM2Config) conf;
		
		this.mol = mol;
		this.config = config;
		this.scf = new PISCF(this);

		
		clear();
		
		createBondDistanceTable();
		int dist[][] = StructureCalculator.getNumberOfBondsBetweenAtoms(mol, 3, new int[mol.getNMovables()][mol.getNMovables()]);
		int[] a2g = StructureCalculator.getAtomToGroups(mol);
		
		//Bond Terms			
		MultipleIntMap<BondTerm> map = new MultipleIntMap<BondTerm>(2);
		for(int i=0; i<mol.getAllBonds(); i++) {
			int a1 = mol.getBondAtom(0, i);	
			int a2 = mol.getBondAtom(1, i);
			if(mol.isAtomFlag(a1, FFMolecule.RIGID) && mol.isAtomFlag(a2, FFMolecule.RIGID)) continue;
			BondTerm term = addBondTerm(a1, a2);
			map.put(new int[]{a1,a2}, term);
			map.put(new int[]{a2,a1}, term);
		}

		//Angle and StretchBend Terms
		for(int a2=0; a2<mol.getNMovables(); a2++) {
			for(int i=0; i<mol.getAllConnAtoms(a2); i++) {
				int a1 = mol.getConnAtom(a2, i);
				for(int j=0; j<mol.getAllConnAtoms(a1); j++) {
					int a3 = mol.getConnAtom(a1, j);
					if(a3<mol.getNMovables() &&  a3<=a2) continue;

					
					if(mol.isAtomFlag(a2, FFMolecule.RIGID) && 
						(mol.isAtomFlag(a1, FFMolecule.RIGID) && mol.isAtomFlag(a3, FFMolecule.RIGID))) continue;				
					addAngleTerm(a2, a1, a3, config.isUseInPlaneAngle());
					if(config.isUseStretchBend() && !mol.isAtomFlag(a1, FFMolecule.RIGID)) {
						BondTerm t1 = (BondTerm) map.get(new int[]{a1, a2});
						BondTerm t2 = (BondTerm) map.get(new int[]{a1, a3});
						addStretchBendTerm(a2, a1, a3, t1, t2);					
					}
				}			
			}			
		}
		
		//OutOfPlaneAngle Terms
		if(config.isUseOutOfPlaneAngle()) {
			for(int i=0; i<mol.getAllAtoms(); i++) {
				if(mol.getAllConnAtoms(i)==3) {
					int a0 = mol.getConnAtom(i, 0);
					int a1 = mol.getConnAtom(i, 1);
					int a2 = mol.getConnAtom(i, 2);
					if(mol.isAtomFlag(a0, FFMolecule.RIGID) 
						&& mol.isAtomFlag(a1, FFMolecule.RIGID)
						&& mol.isAtomFlag(a2, FFMolecule.RIGID)
						&& mol.isAtomFlag(i, FFMolecule.RIGID)) continue;				
					addOutOfPlaneBendTerm(a0, i, a1, a2); 
					addOutOfPlaneBendTerm(a1, i, a0, a2); 
					addOutOfPlaneBendTerm(a2, i, a0, a1);
						
					if(config.isUseInPlaneAngle()) {
						addInPlaneAngleTerm(a0, i, a1, a2);
						addInPlaneAngleTerm(a0, i, a2, a1);
						addInPlaneAngleTerm(a1, i, a2, a0);
					}
				}
			}			
		} 

		//Torsion Terms
		for(int b=0; b<mol.getAllBonds(); b++) {
			int a2 = mol.getBondAtom(0, b);
			int a3 = mol.getBondAtom(1, b);
			for(int i=0; i<mol.getAllConnAtoms(a2); i++) {
				int a1 = mol.getConnAtom(a2, i);
				if(a1==a3) continue;
				for(int j=0; j<mol.getAllConnAtoms(a3); j++) {
					int a4 = mol.getConnAtom(a3, j);
					if(a4==a2 || a4==a1) continue;
					if(mol.isAtomFlag(a1, FFMolecule.RIGID) 
						&& mol.isAtomFlag(a2, FFMolecule.RIGID)
						&& mol.isAtomFlag(a3, FFMolecule.RIGID)
						&& mol.isAtomFlag(a4, FFMolecule.RIGID)) continue;
					if(mol.getAllConnAtoms(a2)<=2 || mol.getAllConnAtoms(a3)<=2) continue;
					addTorsionTerm(a1, a2, a3, a4);						
				}					
			}
		}			

		//VDW and Charge Terms
		if(config.getMaxDistance()>0) {
			if(config.getVdwType()>=0 || config.isUseCharge()) { 
				for(int a1=0; a1<mol.getNMovables(); a1++) {
					for(int a2=0; a2<mol.getNMovables(); a2++) {
						if(mol.getCoordinates(a2).distance(mol.getCoordinates(a1))>config.getMaxDistance()) continue;
						if(a2g[a1]!=a2g[a2]) continue;
						
						if(a2<mol.getNMovables() && dist[a1][a2]>=0 && dist[a1][a2]<=2) continue;
						
						if(mol.isAtomFlag(a2, FFMolecule.RIGID)) continue;
						
						if(config.isUseCharge()) addChargeTerm(a1, a2);						
	
						if(config.getVdwType()==0) addVDWTerm(a1, a2);
						else if(config.getVdwType()==1) addVDW48Term(a1, a2);
						else if(config.getVdwType()==2) addVDW48NoHTerm(a1, a2);
						else if(config.getVdwType()==3) addProteinLigandTerm(a1, a2);
					}			
				}		
			}
	
			//Dipole Terms 
			if(config.isUseDipole() || config.isUsePLDipole()) {
				for(int i=0; i<mol.getAllBonds(); i++) {
					for(int j=i+1; j<mol.getAllBonds(); j++) {										
						int a0 = mol.getBondAtom(0,i);
						int a1 = mol.getBondAtom(1,i);
						int a2 = mol.getBondAtom(0,j);
						int a3 = mol.getBondAtom(1,j);
						if(a0==a2 || a0==a3 || a1==a2 || a1==a3) continue;
	
						boolean interMolecular = a2g[a0]!=a2g[a2];
						
							//((mol.isAtomFlag(a0, FFMolecule.LIGAND) && !mol.isAtomFlag(a2, FFMolecule.LIGAND)) || 
							//(mol.isAtomFlag(a2, FFMolecule.LIGAND) && !mol.isAtomFlag(a0, FFMolecule.LIGAND)));
						if(interMolecular && !mol.isAtomFlag(a0, FFMolecule.LIGAND)) continue;
						if(interMolecular && !config.isUsePLDipole()) continue;
						if(!interMolecular && !config.isUseDipole()) continue;
						
						if(interMolecular && (mol.getAtomicNo(a0)<=1 || mol.getAtomicNo(a1)<=1 || mol.getAtomicNo(a2)<=1 || mol.getAtomicNo(a3)<=1) ) continue;
	
						if(mol.isAtomFlag(a0, FFMolecule.RIGID)
							&& mol.isAtomFlag(a1, FFMolecule.RIGID)
							&& mol.isAtomFlag(a2, FFMolecule.RIGID)					 
							&& mol.isAtomFlag(a3, FFMolecule.RIGID)) continue;
	
						double maxDist2 = interMolecular? config.getMaxPLDistance()*config.getMaxPLDistance(): config.getMaxDistance()*config.getMaxDistance();
						if(mol.getCoordinates(a0).addC(mol.getCoordinates(a1)).scaleC(.5).distSquareTo(mol.getCoordinates(a2).addC(mol.getCoordinates(a3)).scaleC(.5))>maxDist2) continue;
								
						addDipoleTerm(a0, a1, a2, a3);
					}			
				}					
			} 
			
			//Charge-Dipole Terms
			if(config.isUseChargeDipole() || config.isUsePLDipole()) {
				for(int a1=0; a1<mol.getNMovables(); a1++) {
					for(int i=0; i<mol.getAllBonds(); i++) {
						int a2 = mol.getBondAtom(0, i); 
						int a3 = mol.getBondAtom(1, i); 
						//if(a2==a1 || a3==a1) continue;
						if(a1<dist.length && a2<dist[a1].length  && dist[a1][a2]>=0 && dist[a1][a2]<=2) continue;
						if(a1<dist.length && a3<dist[a1].length && dist[a1][a3]>=0 && dist[a1][a3]<=2) continue;					
						if(mol.isAtomFlag(a1, FFMolecule.RIGID)&& mol.isAtomFlag(a2, FFMolecule.RIGID) && mol.isAtomFlag(a3, FFMolecule.RIGID)) continue;
						
						boolean interMolecular = ((mol.isAtomFlag(a1, FFMolecule.LIGAND) && !mol.isAtomFlag(a2, FFMolecule.LIGAND)) || 
							(mol.isAtomFlag(a2, FFMolecule.LIGAND) && !mol.isAtomFlag(a1, FFMolecule.LIGAND)));
						
						if(interMolecular /*&& !config.isUsePLDipole()*/) continue;
						if(!interMolecular && !config.isUseChargeDipole()) continue;
						addChargeDipoleTerm(a1, a2, a3);
					}
				}
			}
		}
		
		//Protein-Ligand Terms
		nProteinLigandTerms = 0;
		if(config.isUsePLInteractions()) {
			
			if(config.getMaxPLDistance()==0) return;
		
			Set<Integer> proteinAtoms = new TreeSet<Integer>();
			Set<Integer> ligandAtoms = new TreeSet<Integer>();	 			 
			for (int a = 0; a < mol.getAllAtoms(); a++) {			
				
				if(mol.isAtomFlag(a, FFMolecule.LIGAND)) {
					ligandAtoms.add(a);
				} else {
					proteinAtoms.add(a);
				}
			}

			//Add the terms
			for(int a1: ligandAtoms) {
				for(int a2: proteinAtoms) {
					if(mol.isAtomFlag(a1, FFMolecule.RIGID) && mol.isAtomFlag(a2, FFMolecule.RIGID )) continue;
					if(mol.getCoordinates(a1).distSquareTo(mol.getCoordinates(a2))>config.getMaxPLDistance()*config.getMaxPLDistance()) continue;						
					if(!config.isUseHydrogenOnProtein() && (mol.getAtomicNo(a1)<=1 || mol.getAtomicNo(a2)<=1)) continue;
					
					if(config.getMaxPLDistance()>0) {
						addProteinLigandTerm(a2, a1);
					} else {
						addVDW48Term(a2, a1);						
					}
					
				}			
			}
			
			//Add the Hbonds terms
		/*	if(config.isUseHBonds()) {
				for(int a1: ligandAtoms) {
					for(int a2: proteinAtoms) {
						if(mol.getAtomicNo(a1)>1 && mol.getAtomicNo(a2)>0) continue;
						HBondTerm term = HBondTerm.create(this, a2, a1);
						if(term!=null) add(term);
					}
				}
				/*
				//Ligand = Donor
				for(int a1: ligandAtoms) {
					if(mol.getAtomicNo(a1)!=1 ||  mol.getAllConnAtoms(a1)!=1) continue;
					int a3 = mol.getConnAtom(a1, 0);
					if(mol.getAtomicNo(a3)!=8 && mol.getAtomicNo(a3)!=7) continue;
					for(int a2: proteinAtoms) {
						if(mol.getAtomicNo(a2)!=8 && mol.getAtomicNo(a2)!=7 && mol.getAtomicNo(a2)!=15) continue;
						if(grid.getNeighbours(mol.getCoordinates(a2), 6, true, FFMolecule.LIGAND).size()==0) continue;
						HBondTerm term = HBondTerm.create(this, a2, a1);
						//System.out.println("creat hbond "+a2+" "+a1+" "+term);
						if(term!=null) add(term);
					}
				}
				
				//Ligand = Acceptor
				for(int a1: ligandAtoms) {
					if(mol.getAtomicNo(a1)!=8 && mol.getAtomicNo(a1)!=7 && mol.getAtomicNo(a1)!=15) continue;
					for(int a2: proteinAtoms) {
						if(mol.getAtomicNo(a2)!=1 ||  mol.getConnAtoms(a2)!=1) continue;
						if(grid.getNeighbours(mol.getCoordinates(a2), 6, true, FFMolecule.LIGAND).size()==0) continue;
						int a3 = mol.getConnAtom(a2, 0);
						if(mol.getAtomicNo(a3)!=8 && mol.getAtomicNo(a3)!=7) continue;
						HBondTerm term = HBondTerm.create(this, a2, a1);
						if(term!=null) add(term);
					}
				}
			}
*/			
		}

		if(config.isUseOrbitals()) {
			try {
				scf.updateTerms();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//System.out.println("FF created with "+nTerms+" "+nProteinLigandTerms+" "+mol.getNMovables());
		
	}

	
	
	private void createBondDistanceTable() {
		bondDistance.clear();
		int N = mol.getNMovables();
		for(int i=0; i<mol.getAllBonds(); i++) {
			int a1 = mol.getBondAtom(0, i);
			int a2 = mol.getBondAtom(1, i);
			
			if(a1>=N && a2>=N) continue;
			if(mol.getAtomicNo(a1)<=1 && mol.getAtomicNo(a2)<=1) continue;
			
			int n1 = mol.getAtomMM2Class(a1);
			int n2 = mol.getAtomMM2Class(a2);
			FFParameters.BondParameters params = parameters.getBondParameters(n1, n2);
			
			//Rectify the distance based on the electronegativity
			double electronegativity = 0;
			for(int j=0; j<mol.getAllConnAtoms(a1); j++) {
				int a3 = mol.getConnAtom(a1, j);
				if(a3!=a2) electronegativity += parameters.getElectronegativity(n2, n1, mol.getAtomMM2Class(a3));
			}
			for(int j=0; j<mol.getAllConnAtoms(a2); j++) {
				int a3 = mol.getConnAtom(a2, j);
				if(a3!=a1) electronegativity += parameters.getElectronegativity(n1, n2, mol.getAtomMM2Class(a3));
			}
			double eq = params.eq + electronegativity;
			
			int[] key = a1<a2? new int[]{a1,a2}: new int[]{a2,a1};
			bondDistance.put(key, eq);			
		}
	}
	
	@Override	
	public final double getBondDistance(int a1, int a2) {
		int[] key = a1<a2? new int[]{a1,a2}: new int[]{a2,a1};
		Double d = bondDistance.get(key);
		if(d==null) {
			return 1.5;
		}
		return d.doubleValue();
	}

	
	
	
	/**
	 * Deep Copy
	 * 
	 */
	@Override
	public MM2TermList clone() {
		try {
			MM2TermList tl = (MM2TermList) super.clone();
			tl.terms = new AbstractTerm[nTerms];
			tl.proteinLigandTerms = new ProteinLigandTerm[nProteinLigandTerms];
			
			
			for (int i = 0; i < tl.terms.length; i++) {
				tl.terms[i] = terms[i].clone();
//				tl.terms[i].setTermList(tl);
			}
			for (int i = 0; i < tl.proteinLigandTerms.length; i++) {
				tl.proteinLigandTerms[i] = (ProteinLigandTerm) proteinLigandTerms[i].clone();
//				tl.proteinLigandTerms[i].setTermList(tl);
			}
			
			return tl;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		
	}

	public MM2Config getConfig() {
		return config;
	}
	
	/*
	public void removeLongerPLTerms() {
		//first sort the PL terms according to the distances
		Arrays.sort(proteinLigandTerms, new Comparator<AbstractTerm>() {
			public int compare(AbstractTerm o1, AbstractTerm o2) {
				if(!(o1 instanceof ProteinLigandTerm)) return 1;
				if(!(o2 instanceof ProteinLigandTerm)) return -1;
				ProteinLigandTerm p1 = (ProteinLigandTerm) o1;
				ProteinLigandTerm p2 = (ProteinLigandTerm) o2;
				double d1 = mol.getCoordinates(p1.getAtoms()[0]).distSquareTo(mol.getCoordinates(p1.getAtoms()[1]));
				double d2 = mol.getCoordinates(p2.getAtoms()[0]).distSquareTo(mol.getCoordinates(p2.getAtoms()[1]));
				return d2>d1?-1:1;
			}
		});
		
		
		//Then keep the shortest distances for each pair
		for (int i = 0; i < nProteinLigandTerms; i++) {
			ProteinLigandTerm p1 = (ProteinLigandTerm) proteinLigandTerms[i];
			double d2 = mol.getCoordinates(p1.getAtoms()[0]).distSquareTo(mol.getCoordinates(p1.getAtoms()[1]));
			//System.out.println(i+"->"+d2);
			if(d2>4.5*4.5) {nProteinLigandTerms=i;  break;}
			for (int j = i+1; j < nProteinLigandTerms; j++) {
				ProteinLigandTerm p2 = (ProteinLigandTerm) proteinLigandTerms[j];
				if(p2.getAtoms()[0]==p1.getAtoms()[0] || p2.getAtoms()[1]==p1.getAtoms()[1]) {
					nProteinLigandTerms--;
					for (int k = j; k < nProteinLigandTerms; k++) {
						proteinLigandTerms[k] = proteinLigandTerms[k+1]; 
					}
				}
			}			
		}
		
		
	}
	/*
	public void removeLessImportantPLTerms() {
		//first sort the PL terms according to the values
		Arrays.sort(proteinLigandTerms, new Comparator<AbstractTerm>() {
			public int compare(AbstractTerm o1, AbstractTerm o2) {
				if(!(o1 instanceof ProteinLigandTerm)) return 1;
				if(!(o2 instanceof ProteinLigandTerm)) return -1;
				ProteinLigandTerm p1 = (ProteinLigandTerm) o1;
				ProteinLigandTerm p2 = (ProteinLigandTerm) o2;
				double d1 = p1.getFGValue(null);
				double d2 = p2.getFGValue(null);
				return Math.abs(d2)>Math.abs(d1)?1:-1;
			}
		});
		
		
		//Then keep the shortest distances for each pair
		for (int i = 0; i < nProteinLigandTerms; i++) {
			ProteinLigandTerm p1 = (ProteinLigandTerm) proteinLigandTerms[i];
			double d2 = mol.getCoordinates(p1.getAtoms()[0]).distSquareTo(mol.getCoordinates(p1.getAtoms()[1]));
			//System.out.println(i+"->"+d2);
			if(d2>4.5*4.5) {nProteinLigandTerms=i;  break;}
			for (int j = i+1; j < nProteinLigandTerms; j++) {
				ProteinLigandTerm p2 = (ProteinLigandTerm) proteinLigandTerms[j];
				if(p2.getAtoms()[0]==p1.getAtoms()[0] || p2.getAtoms()[1]==p1.getAtoms()[1]) {
					nProteinLigandTerms--;
					for (int k = j; k < nProteinLigandTerms; k++) {
						proteinLigandTerms[k] = proteinLigandTerms[k+1]; 
					}
				}
			}			
		}
		
		
	}
	*/
	
	
	@Override
	public String toString() {
		double sum = 0;
		double sum1 = 0, sum2 = 0, sum3 = 0, sum4 = 0, sum5 = 0, sum6 = 0, sum7 = 0, sum8 = 0, sum10 = 0, sum11 = 0, sum12 = 0;
		int n1 = 0, n2 = 0, n3 = 0, n4 = 0, n5 = 0, n6 = 0, n7 = 0, n8 = 0, n10 = 0, n11 = 0, n12 = 0;
				
		double others = 0;
		int n=0;
		
		
		
		MM2TermList tl = this;
		for(int k=0; k<tl.size(); k++) {
			AbstractTerm term = tl.get(k);
			double e = term.getFGValue(null);
			sum += e;
			
			if(term instanceof BondTerm) {sum1 += e; n1++;} 
			else if(term instanceof AngleTerm) {sum2 += e; n2++;}
			else if(term instanceof TorsionTerm) {sum3 += e; n3++;}
			else if(term instanceof VDWLNTerm || term instanceof VDWLN48TermNoH || term instanceof VDWLN48Term) {sum4 += e; n4++;}
			else if(term instanceof DipoleTerm) {sum5 += e; n5++;}
			else if(term instanceof OutOfPlaneAngleTerm) {sum6 += e; n6++;}
			else if(term instanceof InPlaneAngleTerm) {sum8 += e; n8++;}
			else if(term instanceof StretchBendTerm) {sum10 += e; n10++;}
			else if(term instanceof ProteinLigandTerm) {sum7 += e; n7++;}
			else if(term instanceof ChargeTerm) {sum11 += e; n11++;}
			else if(term instanceof ChargeDipoleTerm) {sum12 += e; n12++;}
//			else if(term instanceof HBondTerm) {sum13 += e; n13++;}
			else {others += e; n++;} 
			
		}				
		DecimalFormat df = new DecimalFormat("0.00");
		StringBuffer sb = new StringBuffer();
		sb.append("Total Potential Energy: " + df.format(sum) + " Kcal/mole   (flexible atoms: " + mol.getNMovables()+")"+System.getProperty("line.separator"));
		if(n1>0) sb.append(" Bond Stretching:    " + df.format(sum1) + "   (" + n1 + ")" + System.getProperty("line.separator"));
		if(n2>0) sb.append(" Angle Bending:      " + df.format(sum2) + "   (" + n2 + ")" + System.getProperty("line.separator"));
		if(n10>0) sb.append(" Stretch-Bend:       " + df.format(sum10) + "   (" + n10 + ")" + System.getProperty("line.separator"));
		if(n8>0) sb.append(" In-Plane Angle:     " + df.format(sum8) + "   (" + n8 + ")" + System.getProperty("line.separator"));
		if(n6>0) sb.append(" Out-of-Plane Angle: " + df.format(sum6) + "   (" + n6 + ")" + System.getProperty("line.separator"));
		if(n3>0) sb.append(" Torsional Angle:    " + df.format(sum3) + "   (" + n3 + ")" + System.getProperty("line.separator"));
		if(n4>0) sb.append(" Van der Waals:      " + df.format(sum4) + "   (" + n4 + ")" + System.getProperty("line.separator"));
		if(n5>0) sb.append(" Dipole-Dipole:      " + df.format(sum5) + "   (" + n5 + ")" + System.getProperty("line.separator"));
		if(n11>0) sb.append(" Charge-Charge:      " + df.format(sum11) + "   (" + n11 + ")" + System.getProperty("line.separator"));
		if(n12>0) sb.append(" Charge-Dipole:      " + df.format(sum12) + "   (" + n12 + ")" + System.getProperty("line.separator"));
		if(n7>0) sb.append(" Protein-Ligand:     " + df.format(sum7) + "   (" + n7 + ")" + System.getProperty("line.separator"));
//		if(n13>0) sb.append(" HBonds:             " + df.format(sum13) + "   (" + n13 + ")" + System.getProperty("line.separator"));
		if(n>0)  sb.append(" Others:             " + df.format(others) + "   (" + n + ")" + System.getProperty("line.separator"));
		return sb.toString();
	}
}
