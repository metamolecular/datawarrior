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
package com.actelion.research.forcefield.mm2;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;

public class PISCF {

	public static boolean ENABLED_BONDS = true;
	public static boolean ENABLED_TORSION = true;
	
	private final MM2Parameters parameters = MM2Parameters.getInstance();
	private final FFMolecule mol;
	private final MM2TermList tl;
	private final static double epsilon = 0.01;
	
	private double fock[][] = null;
	
	private int[] iorbit;
	private double[][] ed;

	protected PISCF(MM2TermList tl) {
		this.tl = tl;
		this.mol = tl.getMolecule();
	}
	
	@SuppressWarnings("null")
	protected final void updateTerms() {
		if(!ENABLED_BONDS && !ENABLED_TORSION) return;
		
		if(fock!=null && fock.length>0) return;
		
		boolean pi[] = new boolean[mol.getAllAtoms()];
		//0. Count the number of orbits
		int norbit = 0;
		for(int i=0; i<mol.getAllAtoms(); i++) {
			double[] p = parameters.getPiAtom(mol.getAtomMM2Class(i));
			if(p!=null && !mol.isAtomFlag(i, FFMolecule.RIGID)) {
				pi[i]=true;
				norbit++;			
			}
		}

		
		//1. Find Pi Systems
		//subroutine piplane (orbital.f)
		//for each pisystem atom, find a set of atoms which define
		//the p-orbital's plane based on piatom's atomic number and
		//the number and type of attached atoms		
		iorbit = new int[norbit]; //orbit to atom
		double params[][] = new double[norbit][];
		int piperp[][] = new int[norbit][3];
		norbit=0;
		for(int i=0; i<mol.getAllAtoms(); i++) {
			double[] p = parameters.getPiAtom(mol.getAtomMM2Class(i));
			if(p==null || mol.isAtomFlag(i, FFMolecule.RIGID)) continue;
			
			iorbit[norbit] = i;
			params[norbit] = p;
			if(mol.getAllConnAtoms(i)==3) {//most common case of an atom bonded to three atoms
				piperp[norbit][0] = mol.getConnAtom(i, 0);				
				piperp[norbit][1] = mol.getConnAtom(i, 1);				
				piperp[norbit][2] = mol.getConnAtom(i, 2);				
			} else if(mol.getAllConnAtoms(i)==2 && mol.getAtomicNo(i)!=6) {//any non-alkyne atom bonded to exactly two atoms
				piperp[norbit][0] = i;				
				piperp[norbit][1] = mol.getConnAtom(i, 0);				
				piperp[norbit][2] = mol.getConnAtom(i, 1);								
			} else if(mol.getAllConnAtoms(i)==4) {//any non-alkyne atom bonded to exactly two atoms
				piperp[norbit][0] = i;
				for (int j = 0, count=0; j < 4 && count < 2; j++) {
					if(mol.getAtomicNo(mol.getConnAtom(i,j))>0){						
						piperp[norbit][count+1] = mol.getConnAtom(i, j);
						count++;
					}
				}
			} else if(mol.getAllConnAtoms(i)==1 && mol.getAtomicNo(i)==8) {//Carbonyl
				int alpha = mol.getConnAtom(i, 0);
				int beta = mol.getConnAtom(alpha, 0)==i? mol.getConnAtom(alpha, 1): mol.getConnAtom(alpha, 0);
				piperp[norbit][0] = i;				
				piperp[norbit][1] = alpha;				
				piperp[norbit][2] = beta;					
			} else if(mol.getAllConnAtoms(i)==1 && mol.getAtomicNo(i)==7) {//sp N
				int alpha = mol.getConnAtom(i, 0);
				int beta = -1;
				for (int j = 0; j < mol.getAllConnAtoms(alpha); j++) {
					int trial = mol.getConnAtom(alpha, j);
					if(trial!=i && mol.getAllConnAtoms(trial)==3 && pi[trial]) {
						beta = trial; break;
					}
					
				}
				if(beta>=0) {
					int gamma = mol.getConnAtom(beta, 0)==alpha? mol.getConnAtom(beta, 1): mol.getConnAtom(beta, 0);
					piperp[norbit][0] = i;				
					piperp[norbit][1] = alpha;				
					piperp[norbit][2] = gamma;
//				} else {
//					System.err.println("Failure to Define a pi system for "+i+" use orbital.f");
				}
				
		    } else if(mol.getAllConnAtoms(i)==2 && mol.getAtomicNo(i)==6) {//sp C
				int alpha = mol.getConnAtom(i, 0);
				int beta = 0;
				if((mol.getAllConnAtoms(alpha)==2 && mol.getAtomicNo(alpha)==6) || 
						(mol.getAllConnAtoms(alpha)==1 && mol.getAtomicNo(alpha)==7)) alpha = mol.getConnAtom(i, 1);
				
				for (int j = 0; j < mol.getAllConnAtoms(i); j++) {
					int trial = mol.getConnAtom(i, j);
					if(trial!=i && trial!=alpha && mol.getAllConnAtoms(trial)==3 && pi[trial]) {
						beta = trial; break;
					}					
				}
				for (int j = 0; j < mol.getAllConnAtoms(alpha); j++) {
					int trial = mol.getConnAtom(alpha, j);
					if(trial!=i && trial!=alpha && mol.getAllConnAtoms(trial)==3 && pi[trial]) {
						beta = trial; break;
					}					
				}
				if(beta>=0) {
					int gamma = mol.getConnAtom(beta, 0);
					if(gamma==i || gamma==alpha) gamma = mol.getConnAtom(beta, 1);
					piperp[norbit][0] = i;				
					piperp[norbit][1] = alpha;				
					piperp[norbit][2] = gamma;
				} else {
//					System.err.println("Failure to Define a pi system for "+i+" use orbital.f");
				}
				
				
			} else {
//				System.err.println("Failure to Define a pi system for "+i+" use orbital.f");
			}			
			norbit++;			
		}
		
		
		//find and store the pisystem bonds
		int nbpi = 0;
		for(int i=0; i<mol.getAllBonds(); i++) {
 			if(pi[mol.getBondAtom(0, i)] && pi[mol.getBondAtom(1, i)]) nbpi++;			
		}	
		int piBonds[][] = new int[nbpi][3];
		nbpi = 0;
		for(int i=0; i<norbit-1; i++) {
			for(int j=i+1; j<norbit; j++) {
				for(int k=0; k<mol.getAllConnAtoms(iorbit[i]); k++) {
					if(mol.getConnAtom(iorbit[i], k)!=iorbit[j]) continue;
					piBonds[nbpi][0] = mol.getConnBond(iorbit[i], k); 
					piBonds[nbpi][1] = i; 
					piBonds[nbpi][2] = j; 					
					nbpi++;
				}
			}
		}
					
//		System.out.println("norbits="+norbit+" nbpi="+nbpi);
		
		double EVOLT = 27.2113834;
		double hartree = 627.5094709;
	    double ebe = 129.37;
	    double ebb = 117.58;
	    double aeth = 2.309;
	    double abnz = 2.142;
	    double ble = 1.338;
	    double blb = 1.397;		
		
		//2. Create Fock Matrix
	    // assign empirical one-center Coulomb integrals, and  first or second ionization potential depending
		double gamma[][] = new double[norbit][norbit];
		double ip[] = new double[norbit];	
		for(int i=0; i<norbit; i++) {
			double[] v = params[i];
			gamma[i][i] = v[2] / EVOLT;			
			ip[i] = v[1] / EVOLT + (1 - v[0]) * gamma[i][i];
		}
			
		//calculate two-center repulsion integrals according to Ohno's semi-empirical formula
		for(int i=0; i<norbit-1; i++) {
			for(int j=i+1; j<norbit; j++) {
				double gll = 0.5 * (gamma[i][i] + gamma[j][j]);
				double distSq = mol.getCoordinates(iorbit[i]).distSquareTo(mol.getCoordinates(iorbit[j])); 
				distSq /= 0.5291772083*0.5291772083;
				gamma[i][j] = 1.0 / Math.sqrt(distSq + 1.0 / (gll*gll));
				gamma[j][i] = gamma[i][j];  
			}
		}
		
		// the first term in the sum to find alpha is the first or second ionization potential
		double hc[][] = new double[norbit][norbit];
		for(int i=0; i<norbit; i++) {
			double hcii = ip[i];		
			for(int j=0; j<norbit; j++) {
				if(i!=j) {
					hcii = hcii - params[j][0] * gamma[i][j];
				}
			}
			hc[i][i] = hcii;
		}
		
		//get two-center repulsion integrals via Ohno's formula
		for(int k=0; k<nbpi; k++) {
			int i = piBonds[k][1];
			int j = piBonds[k][2];
			int iorb = iorbit[i];
			int jorb = iorbit[j];
			double gll = 0.5 * (gamma[i][i] + gamma[j][j]);
			double rij = Math.sqrt(mol.getCoordinates(iorb).distSquareTo(mol.getCoordinates(jorb)));
			double rijsq = rij*rij/(0.5291772083*0.5291772083);
			
			//Bond energy using Morse Potential
			double erij = aeth * (ble - rij);
			double brij = abnz * (blb - rij);
			double eeBond = (2 * Math.exp(erij) - Math.exp(2*erij))* ebe / hartree;
			double beBond = (2 * Math.exp(brij) - Math.exp(2*brij))* ebb / hartree;			
			
			//C-C resonance integral (Whitehead and Lo formula)
			double gl4 = 1 / Math.sqrt(4 * rijsq + 1.0 / (gll*gll));
			double hcij = 1.5 * (beBond - eeBond) - 0.375*gll + 5.0/12.0*gamma[i][j] - gl4/24.0;			
			
			//if either atom is non-carbon, then factor the resonance integral by overlap ratio and ionization potential ratio
			if(mol.getAtomicNo(iorb)!=6 || mol.getAtomicNo(jorb)!=6) {
	            double ovlap = overlap (mol.getAtomicNo(iorb), mol.getAtomicNo(jorb), rij);
	            double covlap = overlap (6, 6, rij);				

	            hcij *= ovlap / covlap;
	            
				double iionize = ip[i];
				double jionize = ip[j];
				if(params[i][0]!=1) {
					if(mol.getAtomicNo(iorb)==7) iionize *= 0.595;		
					else if(mol.getAtomicNo(iorb)==8) iionize *= 0.525;		
					else if(mol.getAtomicNo(iorb)==16) iionize *= 0.89;		
				}
				if(params[j][0]!=1) {
					if(mol.getAtomicNo(jorb)==7) jionize *= 0.595;		
					else if(mol.getAtomicNo(jorb)==8) jionize *= 0.525;		
					else if(mol.getAtomicNo(jorb)==16) jionize *= 0.89;		
				}
				hcij *= (iionize+jionize) / (2 * -11.16 / EVOLT); 
	            
			}
			hc[i][j] = hc[j][i] = hcij;
		}
		
		//make an initial guess at the Fock matrix if needed
		if(fock==null) {
			fock = new double[norbit][norbit];
			for(int i=0; i<norbit; i++) {
				for(int j=0; j<norbit; j++) {
					fock[j][i] = hc[j][i];
				}
			}
			for(int i=0; i<norbit; i++) {
				fock[i][i] = 0.5 * ip[i];
			}
		}
		
		//filled orbitals
		int nfill = 0;
		for(int i=0; i<norbit; i++) {
			nfill += params[i][0];
		}
		nfill /= 2;
		
		double pnpl[] = new double[nbpi];
		double pbpl[] = new double[nbpi];
		
		double[][] v = null;
		ed = new double[norbit][norbit];
		for(int mode = 0; mode<2; mode++) { //mode==0 -> planar, mode==1 -> nonpln
			
			if(mode==1) {//nonplanar
				//pitilt
				//transform coordinates of "iorb", "jorb" and their associated planes
				double[] povlap = new double[nbpi];
				for(int k = 0; k<nbpi; k++) {								
					int i = piBonds[k][1];
					int j = piBonds[k][2];
				    int iorb = iorbit[i];
				    int jorb = iorbit[j];
				    int[] list = new int[8];
				    list[0] = iorb;
				    list[1] = jorb;
				    for(int m = 0; m<3; m++) {
				    	list[m+2] = piperp[i][m];
				    	list[m+5] = piperp[j][m];
				    }
				    
				    Coordinates[] r = new Coordinates[8]; 
				    pimove (list, r);
				
				    //check for sp-hybridized carbon in current bond;
				    if((mol.getAtomicNo(iorb)==6 && mol.getAllConnAtoms(iorb)==2) ||
				    		(mol.getAtomicNo(jorb)==6 && mol.getAllConnAtoms(jorb)==2)) {
				    	povlap[k] = 1;
				    } else {
				    	//find and normalize a vector parallel to first p-orbital
				    	Coordinates r2 = r[3].subC(r[2]);
				    	Coordinates r3 = r[4].subC(r[2]);
				    	Coordinates a1 = r2.cross(r3);
				    	if(a1.distSq()==0) {
				    		a1 = r2.cross(new Coordinates(1,0,0));
					    	if(a1.distSq()==0) a1 = r2.cross(new Coordinates(0,1,0));
					    	if(a1.distSq()==0) continue;
				    	}
			            a1 = a1.unit();

			            //now find vector parallel to the second p-orbital,
				    	r2 = r[6].subC(r[5]);
				    	r3 = r[7].subC(r[5]);
				    	Coordinates a2 = r2.cross(r3);
				    	if(a2.distSq()==0) continue;
			            a2 = a2.unit();
			            a2.x = -a2.x;

			            //compute the cosine of the angle between p-orbitals;
			            double cosine = a1.dot(a2);
				        if (cosine < 0.0) a2 = new Coordinates().subC(a2);
				
				        //find overlap if the orbitals were perfectly parallel
				        Coordinates xij = mol.getCoordinates(iorb).subC(mol.getCoordinates(jorb));
				        double rij = xij.dist();
				        double ideal = overlap(mol.getAtomicNo(iorb), mol.getAtomicNo(jorb), rij);

				        //set ratio of actual to ideal overlap for current pibond				
				        povlap[k] = ideal*a1.x*a2.x + a1.y*a2.y + a1.z*a2.z;
				    }
				}
				
	            for(int k = 0; k< nbpi; k++) {
	            	int i = piBonds[k][1];
	            	int j = piBonds[k][2];
	            	hc[i][j] *= povlap[k];
	            	hc[j][i] = hc[i][j];
	            }
			}			
			
			//3. perform scf iterations until convergence is reached;
			double delta = 2 * epsilon;
			double totalOld = 0;
			double xi = 0, xj = 0, xk = 0, xg = 0; 
			for(int iter=0; iter<10 && delta>epsilon; iter++) {
				
				//Diagonalization using jacobi
				v = jacobi(fock);
				
				for(int i=0; i<norbit; i++) {
					for(int j=0; j<norbit; j++) {
						double s1 = 0, s2 = 0;
						for(int k=0; k<nfill; k++) {
							s2 -= v[i][k] * v[j][k] * gamma[i][j];
							if(i==j) {
								for(int m=0; m<norbit; m++) {
									s1 += 2 * gamma[i][m] * v[m][k] * v[m][k];
								}
							}						
						}
						fock[j][i] = fock[i][j] = s1 + s2 + hc[i][j];
					}
				}
				
				//calculate the ground state energy
				xi=xj=xk=xg=0;
				for(int i=0; i<nfill; i++) {
					for(int j=0; j<norbit; j++) {
						for(int k=0; k<norbit; k++) {
							xi += 2 * v[j][i] * v[k][i] * hc[j][k];
							for(int m=0; m<nfill; m++) {
								xj += 2 * v[j][i] * v[j][i] * v[k][m] * v[k][m] * gamma[j][k];
								xk -= v[j][i] * v[j][m] * v[k][i] * v[k][m] * gamma[j][k];						
							}
						}
					}
				}			
				for(int i=0; i<norbit-1; i++) {
					for(int j=i+1; j<norbit; j++) {
						xg += params[i][0] * params[j][0] * gamma[i][j];
					}
				}
				
				double total = xi + xj + xk + xg;
				delta = Math.abs(total - totalOld);
				totalOld = total;
			}
			//if(delta>epsilon) System.err.println("The SCF-MO Iteration has not reached Self-Consistency");
	
			
			//Electron Density
			for(int i=0; i<norbit; i++) {
				for(int j=0; j<norbit; j++) {
					ed[i][j] = 0;
					for(int k=0; k<nfill; k++) {
						ed[i][j] += 2 * v[i][k] * v[j][k]; 
					}	
				}
			}
			
			//now, get the bond orders (compute p and p*b)
			for(int k=0; k<nbpi; k++) {
				int i = piBonds[k][1];
				int j = piBonds[k][2];
				double p = 0;
				for(int m=0; m<nfill; m++) p += 2 * v[i][m] * v[j][m];
								
				if(mode==0) { //Planar
					pbpl[k] = p * hc[i][j] / -0.0757;	
				} else { //non-planar
					pnpl[k] = p;
				}				
			}
			/*
			if(false){
				//Print
				System.out.println("--------------------"+mode+"----------------------------");
				System.out.println("Coulomb Repulsion: "+xj);
				System.out.println("Exchange Repulsion: "+xk);
				System.out.println("Nuclear Repulsion: "+xg);
				System.out.print("Orbits: ");
				for (int i = 0; i < norbit; i++) {
					System.out.print(params[i][2]/EVOLT+"("+iorbit[i]+")\t");
				}
				System.out.println();
				System.out.println("Molecular Orbitals: ");
				print(v);
				System.out.println();
				System.out.println("FOCK: ");
				print(fock);
				System.out.println();
				System.out.println("Electron density: ");
				print(ed);
				System.out.println();
				System.out.println("HCore: ");
				print(hc);
				System.out.println();
				System.out.println("Gamma: ");
				print(gamma);
				
				System.out.println();
				System.out.println("Pi Bond orders: ");
				for (int i = 0; i < nbpi; i++) {
					System.out.println(piBonds[i][1]+" "+piBonds[i][2]+" -> "+pnpl[i]+" "+pbpl[i]);
				}
			}*/
		}
		/*
		if(false) 
		{
			System.out.println();
			System.out.println("Electron density: ");
			print(getElectronDensity());
		}*/

		
		
		//4. Update parameters (pialter)
		for(int nt=0; nt<tl.size(); nt++) {
			AbstractTerm term = tl.get(nt);
			if(term instanceof TorsionTerm) {
				TorsionTerm t = (TorsionTerm) term;
				if(ENABLED_TORSION) {
					for(int k=0; k<nbpi; k++) {
						int a1 = iorbit[piBonds[k][1]];
						int a2 = iorbit[piBonds[k][2]];
						if(t.getAtoms()[1]==a1 && t.getAtoms()[2]==a2) {
							if(mol.getAtomicNo(t.getAtoms()[0])>1 && mol.getAtomicNo(t.getAtoms()[3])>1) {
								if(Math.abs(pbpl[k])>0.01) t.v2 = pbpl[k] * t.initV2;
//								System.out.println("updated torsion by "+Formatter.format2(pnpl[k])+" from "+Formatter.format2(t.initV1)+","+Formatter.format2(t.initV2)+","+Formatter.format2(t.initV3)+" - "+t.getAtoms()[0]+" "+t.getAtoms()[1]+" "+t.getAtoms()[2]+" "+t.getAtoms()[3] + " - "+mol.getAtomClass(t.getAtoms()[1])+" "+mol.getAtomClass(t.getAtoms()[2]));
								break;
								
							}
						}
					}
				}								
			} else if(term instanceof BondTerm) {
				if(ENABLED_BONDS) { 
					BondTerm t = (BondTerm) term;
					for(int i=0; i<nbpi; i++) {
						int a1 = iorbit[piBonds[i][1]];
						int a2 = iorbit[piBonds[i][2]];
						if(t.getAtoms()[0]==a1 && t.getAtoms()[1]==a2) {
							double[] par = parameters.getPiBond(mol.getAtomMM2Class(a1), mol.getAtomMM2Class(a2));
							if(par==null) {
								//System.err.println("Undefined Pi Bond for "+a1+"-"+a2);
								continue;
							}
							t.Kb -= par[0]*(1-pnpl[i]);
							t.eq += par[1]*(1-pnpl[i]);
	//						System.out.println("updated bond("+pnpl[i]+") "+t.getAtoms()[0]+"-"+t.getAtoms()[1]+"   "+mol.getAtomClass(t.getAtoms()[0])+" "+mol.getAtomClass(t.getAtoms()[1])+"    "+t.Kb+" "+t.eq);
							break;
							
						}									
					}
				}
			}
		}
	}

	/**
	 * Jacobi matrix diagonalization  (jacobi.f)
	 * @param a, a hemitian matrix
	 * @return the eigenvalues of a
	 */
	private static double[][] jacobi(double[][] a) {
		int n = a.length;
		double v[][] = new double[n][n];
		double b[] = new double[n];
		double d[] = new double[n];
		double z[] = new double[n];
		
		//setup and initialization (v = Id)
		for(int ip=0; ip<n; ip++) {
			v[ip][ip] = 1;
			b[ip] = a[ip][ip];
			d[ip] = b[ip];						         
		}
		
		//Perform the Jacobi rotations
		for(int i=0; i<100; i++) {
			/*System.out.println("--------"+i+"-------------");
			System.out.print("D=");
			for (int j = 0; j < d.length; j++) System.out.print(d[j]+" ");
			System.out.println();
			System.out.println("P=");
			print(v);
			System.out.println("A=");
			print(a);
			System.out.println();*/
			double sm = 0;
			for(int ip=0; ip<n-1; ip++) {
				for(int iq=ip+1; iq<n; iq++) {
					sm += Math.abs(a[ip][iq]);
				}				
			}
			if(sm==0) break;
			
			double tresh = i<4? 0.2 * sm / (n * n): 0;
//			int nRot = 0;
			for(int ip=0; ip<n-1; ip++) {
				for(int iq=ip+1; iq<n; iq++) {
					double g = 100 * Math.abs(a[ip][iq]);
					if(i>4 && Math.abs(d[ip]) + g == Math.abs(d[ip]) && Math.abs(d[iq]) + g == Math.abs(d[iq])) {
						a[ip][iq] = 0;
					} else if(Math.abs(a[ip][iq])>tresh) {
						double h = d[iq] - d[ip];
						double t;
						if(Math.abs(h)+g==Math.abs(h)) {//more obfuscated code in Tinker (g=0)
							t = Math.abs(a[ip][iq])/h; 
						} else {
							double theta = 0.5 * h / a[ip][iq];
							t = 1 / ( Math.abs(theta) + Math.sqrt(1 + theta * theta));
							if(theta<0) t = - t;
						}
						double c = 1 / Math.sqrt(1 + t * t);
						double s = t * c;
						double tau = s / (1 + c);
						h = t * a[ip][iq];
						z[ip] -= h;
						z[iq] += h;
						d[ip] -= h;
						d[iq] += h;
						a[ip][iq] = 0;
						//System.out.println(c+" "+s);
						for(int j=0; j<=ip-1; j++) {
							g = a[j][ip];
							h = a[j][iq];
							a[j][ip] = g - s * (h + g*tau);
							a[j][iq] = h + s * (g - h*tau);
						}
						for(int j=ip+1; j<=iq-1; j++) {
							g = a[ip][j];
							h = a[j][iq];
							a[ip][j] = g - s * (h + g * tau);
		                    a[j][iq] = h + s * (g - h * tau);
						}
						for(int j=iq+1; j<n; j++) {
							g = a[ip][j];
							h = a[iq][j];
							a[ip][j] = g - s * (h + g*tau);
							a[iq][j] = h + s * (g - h*tau);
						}						
						for(int j=0; j<n; j++) {
							g = v[j][ip];
							h = v[j][iq];
							v[j][ip] = g - s * (h + g * tau); 
							v[j][iq] = h + s * (g - h * tau);
						}						
//						nRot++;
					} //end-if					
				} //end-for
			} //end-for
	
			for(int ip=0; ip<n; ip++) {
				b[ip] += z[ip];
				d[ip] = b[ip];
				z[ip] = 0;
			}
		}
		
		//sort the eigenvalues and vectors
		for(int i=0; i<n-1; i++) {
			int k = i;
			double p = d[i];
			for(int j=i+1; j<n; j++) {
				if(d[j]<p) {
					k = j;
					p = d[j];
				}
			}
			if(k!=i) {
				d[k] = d[i];
				d[i] = p;
				for(int j=0; j<n; j++) {
					p = v[j][i];
					v[j][i] = v[j][k];
					v[j][k] = p;
				}
			}
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				a[i][j]=0;
			}			
			a[i][i]= d[i];
		}
	
		//System.out.println("JACOBI");
		//System.out.println("P=");
		//print(v);
		//System.out.println("D=");
		//print(a);
		
		
		return v;
	}
/*
	private static void print(double[][] res) {
		for (int i = 0; i < res.length; i++) {
			System.out.print(i+".\t");
			double sum = 0;
			for (int j = 0; j < res[i].length; j++) {
				System.out.print(new DecimalFormat("0.000").format(res[i][j])+"\t");
				sum += res[i][j];
			}
			System.out.println("SUM="+new DecimalFormat("0.000").format(sum));
			System.out.println();
		}
	}*/

	private void pimove(int[] list, Coordinates[] xr) {
		//translate "list" atoms to place atom 1 at origin
		Coordinates xt = mol.getCoordinates(list[0]);
		for (int i = 0; i < 8; i++) {
			xr[i] = mol.getCoordinates(list[i]).subC(xt);
		}
	
		
		//rotate "list" atoms to place atom 2 on the x-axis
	    double denom = Math.sqrt(xr[1].x*xr[1].x +  xr[1].y*xr[1].y);
	    if (denom != 0.0) {
	        double sine = xr[1].y / denom;
	        double cosine = xr[1].x / denom;            
	        for (int i = 0; i < 8; i++) {
	        	double xold = xr[i].x;
	            xr[i].x = xr[i].x*cosine + xr[i].y*sine;
	            xr[i].y = xr[i].y*cosine - xold*sine;
	        }
	    }
	    denom = Math.sqrt(xr[1].x*xr[1].x +  xr[1].z*xr[1].z);
	    if (denom != 0.0) {
	        double sine = xr[1].z / denom;
	        double cosine = xr[1].x / denom;            
	        for (int i = 0; i < 8; i++) {
	        	double xold = xr[i].x;
	            xr[i].x = xr[i].x*cosine + xr[i].z*sine;
	            xr[i].z = xr[i].z*cosine - xold*sine;
	        }
	    }
	
	    //normalize the coordinates of atoms defining the plane
	    for(int i=2; i<5; i++) {
	    	if (list[i] != list[0]) {
	    		if(xr[i].distSq()==0) return;
	    		xr[i] = xr[i].unit();
	    		
	    	}
	    }
	    
	    //normalization of plane defining atoms for atom 2; for the
	    for(int i=5; i<8; i++) {
	         if (list[i] != list[1]) {
	            denom = Math.sqrt((xr[i].x-xr[1].x)*(xr[i].x-xr[1].x) + xr[i].y*xr[i].y + xr[i].z*xr[i].z);
	            xr[i].x = (xr[i].x-xr[1].x)/denom + xr[1].x;
	            xr[i].y = xr[i].y / denom;
	            xr[i].z = xr[i].z / denom;
	         }
	    }
	}

	private static double overlap(int atmnum1, int atmnum2, double rang) {
		final double bohr = 0.5291772083;
		final double[] zeta =  new double[] {1.000, 1.700, 0.650, 0.975, 1.300, 1.625,
			     1.950, 2.275, 2.600, 2.925, 0.733, 0.950,
			     1.167, 1.383, 1.600, 1.817, 2.033, 2.250};
	
		
		final int[] icosa = {0, 1};
		final int[] icosb = {0, 1};
		final double[] cosa  = {1.0, 1.0};
		final double[] cosb  = {-1.0, 1.0};
//		final int[] idsga = {0, 1, 2, 2, 0};
//		final int[] idsgb = {0, 1, 2, 0, 2};
//		final double[] dsiga = {3.0, 4.0, 3.0, -1.0, -1.0};
//		final double[] dsigb = {3.0,-4.0, 3.0, -1.0, -1.0};
		final int[] isina = {0, 2, 0, 2};
		final int[] isinb = {0, 0, 2, 2};
		final double[] sinab = {-1.0, 1.0, 1.0, -1.0};
		final double[] theta = {0.7071068, 1.2247450, 0.8660254,
	                  0.7905694, 1.9364916, 0.9682458};
				
		
		//principal quantum number from atomic number
		final int na = atmnum1>10? 3: 2;
		final int nb = atmnum2>10? 3: 2;
		
		//azimuthal quantum number for p-orbitals
		final int la = 1;
		final int lb = 1;
		
		//orbital exponent from stored ideal values
		final double za = zeta[atmnum1];
		final double zb = zeta[atmnum2];
		
		//convert interatomic distance to bohrs
		final double r = rang / bohr;
		
		//get pi-overlap via generic overlap integral routine
		final double[] s = new double[3];
		final double ana = Math.pow(2.0*za, 2*na+1) / fact(2*na);
		final double anb = Math.pow(2.0*zb, 2*nb+1) / fact(2*nb);
		
		//orbitals are on the same atomic center
	    if (r < 0.000001) {
	        s[1] = fact(na + nb) / Math.pow(za+zb, na + nb + 1);
	        for (int novi = 0; novi < 3; novi++) {
	           s[novi] = s[novi] * Math.sqrt(ana*anb) * 1.0;
	        }
	        return s[1];
	    }
	    
	    //compute overlap integrals for general case	    
	    final double[] a = aset(.5*r * (za+zb), na+nb);
	    final double[] b = bset(.5*r * (za-zb), na+nb);
	    final int max = na - la + nb - lb + 1;
	    final int[] ia = new int[200]; 
	    final int[] ib = new int[200]; 
	    final double[] cbase = new double[200]; 
	    final double[] c = new double[200]; 
	    for (int j = 0; j < max; j++) {
			ia[j] = j;
			ib[j] = max - j - 1;
			cbase[j] = cjkm(j,  na - la, nb - lb);
			c[j] = cbase[j];
		}
	    
	    int maxx = max;
//	    if (la == 1) 
	    maxx = polyp (c,ia,ib,maxx,cosa,icosa,icosb);
//	    else if (la == 2) maxx = polyp (c,ia,ib,maxx,dsiga,idsga,idsgb);	    
//	    if (lb == 1) 
	    maxx = polyp (c,ia,ib,maxx,cosb,icosa,icosb);
//	    else if (lb == 2) maxx = polyp (c,ia,ib,maxx,dsigb,idsga,idsgb);
	
	    int novi = 0;
	    while (true) {
	         for(int j = 0; j<maxx; j++) {
	            double coef = c[j];
	            if (Math.abs(coef) > epsilon) {
	            	s[novi] += coef*a[ia[j]]*b[ib[j]];
	            }
	         }
	         int ja = la*(la+1)/2 + novi;
	         int jb = lb*(lb+1)/2 + novi;
	         s[novi] = s[novi] * theta[ja] * theta[jb];
	
	         if (novi==0 && la!=0 && lb!=0) {
	            maxx = max;
	            for(int j = 0; j<maxx; j++) {
	            	c[j] = cbase[j];
	            }
	            maxx = polyp (c,ia,ib,maxx,sinab,isina,isinb);
//	            if (la == 2) maxx = polyp (c,ia,ib,maxx,cosa,icosa,icosb);
//	            if (lb == 2) maxx = polyp (c,ia,ib,maxx,cosb,icosa,icosb);
	
	            novi = 1;
//	         } else if (novi==1 && la==2 && lb==2) {
//	            maxx = max;
//	            for(int j = 1; j<=maxx; j++) {
//	            	c[j] = cbase[j];
//	        	}
//	            maxx = polyp (c,ia,ib,maxx,sinab,isina,isinb);
//	            maxx = polyp (c,ia,ib,maxx,sinab,isina,isinb);
//	            novi = 2;
	         } else {
	            double anr = Math.pow(0.5*r, na+nb+1);
	            double an = Math.sqrt(ana*anb);
	            for(int novi2 = 0; novi2<3; novi2++) {
	               s[novi2] *= an * anr;
	            }
	            break;
	         }
		}
		
		return s[1];
	}

	private static int polyp (double[] c, int[] ia, int[] ib, int max, double[] d, int[] iaa, int[] ibb) {
		for (int j = 0; j < max; j++) {
			for (int i = iaa.length-1; i >=0; i--) {
		        int m = i*max + j;
		        c[m] = c[j] * d[i];
		        ia[m] = ia[j] + iaa[i];
		        ib[m] = ib[j] + ibb[i];
			}
		}
	    return iaa.length * max;
	}

	private static double cjkm(int j, int k, int m) {
		int min = j>m? j - m + 1: 1;
		int max = k < j? k + 1: j + 1;
	    double sum = 0;
	    for (int ip1 = min; ip1 <=max; ip1++) {			
	         int i = ip1 - 1;
	         double b1 = (double)fact(k) / (fact(i)*fact(k - i));
	         double b2 = j < i? 1.0: (double)fact(m) / (fact(j - i)*fact(m - (j-i)));
	         sum = sum + b1*b2 * Math.pow(-1, i);
	    }
		return sum * Math.pow(-1, m-j);
	}

	private static double[] aset(double alpha, int n) {
		double[] a = new double[n+1];
		double alp = 1.0 / alpha;
		a[0] = Math.exp(-alpha) * alp;
		for (int i = 0; i < n; i++) {
			a[i+1] = a[0] + i * a[i]*alp;			
		}
		return a;
	}

	private static double[] bset(double beta, int n) {
		double[] b = new double[n+1];
		
		if(Math.abs(beta)<0.000001) {
			for (int i = 0; i <= n; i++) {
				b[i] = 2.0 / (i+1);
				if( (i+1) %2==0) b[i] = 0;
			}
		} if (Math.abs(beta) > n/2.3) {
	        double d1 = Math.exp(beta);
	        double d2 = 1.0 / d1;
	        double betam = 1.0 / beta;
	        b[0] = (d1-d2) * betam;
			for (int i = 0; i < n; i++) {
	           d1 = -d1;
	           b[i+1] = (d1-d2+(i+1)*b[i]) * betam;
			}
		} else {
	        b[n] = bmax(beta, n);
	        double d1 = Math.exp(beta);
	        double d2 = 1.0 / d1;
	        if ( (n+1)%2 == 0)  d1 = -d1;
			for (int j = n-1; j >=0; j--) {
	           d1 = -d1;
	           b[j] = (d1+d2+beta*b[j+1]) / (j+1);
			}
		}
				
		return b;
	}

	private static double bmax(double beta, int n) {
		double b = beta * beta;
		double top = n + 1.0;
	    double sum = 1.0 / top;
	    double fi = 2.0;
	    double sign = 2.0;
	    if ( (n+1)%2==0) {
	    	top = top + 1.0;
	        sum = beta / top;
	        fi = fi + 1.0;
	        sign = -2.0;
	    }
	      
	    double term = sum;
	    while (true) {
	    	double bot = top + 2.0;
	        term = term * b * top / (fi*(fi-1)*bot);
	        sum = sum + term;
	        if (Math.abs(term) < 0.0000001) break;
	        fi = fi + 2.0;
	        top = bot;
		}
		
		return sign * sum;
	}

	private static int fact(int n) {
		int res = 1;
		for (int i = 2; i <= n; i++) res*=i;
		return res;
	}

	
	/**
	 * Return a atom to atom density matrix
	 * @return
	 */
	public double[][] getElectronDensity() {
		if(ed==null) updateTerms();
		double[][] res = new double[mol.getAllAtoms()][mol.getAllAtoms()];
		for (int i = 0; i < ed.length; i++) {
			for (int j = 0; j < ed.length; j++) {
				res[iorbit[i]][iorbit[j]] = ed[i][j];
			}
		}
		
		
		return res;
	}
	/*
	public static void main(String[] args) throws Exception {
		double[][] mat = {
				{1,0,-1,3},
				{0,1,0,4},
				{0,0,4,-1},
				{0,0,0,4}};
		
		
		
		double[][] res = jacobi(mat);
		Matrix p = new Matrix(res);
		Matrix d = new Matrix(mat);
		Matrix r = p.multiply(d).multiply(p.transpose());
		
		System.out.println(p);
		System.out.println(d);
		System.out.println("initial="+r);
		
		
		ExtendedMolecule m = new ExtendedMolecule();
		//new SmilesParser().parse(m, "C1=CC=C1");
		FFMolecule mol = new FFMolecule(m);
		//MM2Parameters.getInstance().setAtomClassesForMolecule(mol);
		PreOptimizer.preOptimize(mol);
		new AlgoLBFGS().optimize(new EvaluableForceField(new ForceField(mol)));
		System.out.println(new ForceField(mol));
		//new PISCF(mol);
	}*/
	
}
