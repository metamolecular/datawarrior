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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.FFParameters;

/**
 * 
 */
public class MM2Parameters extends FFParameters {

	private static volatile MM2Parameters instance = null;
	
	public static MM2Parameters getInstance() {
		if(instance==null) {
			synchronized(MM2Parameters.class) {
				if(instance==null) {
					try {
						instance = new MM2Parameters();
					} catch(Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
		return instance;
	}
	
	private MM2Parameters() throws Exception {
		//System.out.println("Load MM2 Parameters");
		URL url = getClass().getResource("/resources/forcefield/MM2.parameters");
		System.out.println("Loaded MM2 Parameters");
		if(url==null) throw new Exception("Could not find MM2.parameters in the classpath");
		try {
			InputStream is = url.openStream();
			load(is);
			is.close();
		} catch(IOException e) {
			throw new Exception(e);
		}
	}
		
	
	/**
	 * Parses the given parameter file
	 * @param fileName
	 * @throws ActelionException
	 */	
	private void load(InputStream is) throws Exception {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			int state = 0;
			while((line = reader.readLine())!=null) {
				String token[] = line.split("\t");
				if(token.length==0 || token[0].length()==0) continue;
				else if(token.length==1) {
					if(line.equals("--- ATOM ---")) state = 1;
					else if(line.equals("--- BOND ---")) state = 2;
					else if(line.equals("--- ANGLE ---")) state = 3;
					else if(line.equals("--- 3-ANGLE ---")) state = 8;
					else if(line.equals("--- 4-ANGLE ---")) state = 9;
					else if(line.equals("--- TORSION ---")) state = 4;
					else if(line.equals("--- 4-TORSION ---")) state = 10;
					else if(line.equals("--- OPBEND ---")) state = 5;
					else if(line.equals("--- VDW ---")) state = 6;
					else if(line.equals("--- VDW PAIRS ---")) state = 7;
					else if(line.equals("--- STRETCH-BEND ---")) state = 11;
					else if(line.equals("--- PI-BONDS ---")) state = 12;
					else if(line.equals("--- PI-ATOMS ---")) state = 13;
					else if(line.equals("--- ELECTRONEGATIVITY ---")) state = 14;
					continue;
				}
				
				try {
					switch(state) {
						case 1: {// ---- ATOM -----
							String description = token[0].toUpperCase().intern();
							String label = token[1];
							int atomicNo = Molecule.getAtomicNoFromLabel(label);
							//double VDW = Double.parseDouble(token[2]);
							int classNo = Integer.parseInt("0"+token[3]);
							//int hybridization = 3;//Integer.parseInt(line.substring(64,65).trim());
							int doubleBonds = token.length>8?Integer.parseInt("0"+token[8]):0;
							int tripleBonds = token.length>9?Integer.parseInt("0"+token[9]):0;		
							double charge = token[4].length()>0? Double.parseDouble(token[4]): 0;
							int[] replacement = null;
							if(token.length>13) {								
								String[] s = token[13].trim().split(",");
								if(s.length>0) {
									if(s.length%2==1) throw new Exception("The replacement string is invalid on "+line);
									replacement = new int[s.length];
									for(int i=0; i<s.length; i++) replacement[i] = Integer.parseInt(s[i]);
								}
							}
							AtomClass atom = new AtomClass(classNo, atomicNo, description, charge, doubleBonds, tripleBonds, replacement);
							descriptionToAtom.put(description.toUpperCase(), atom);
							classNoToAtom.put(classNo, atom);
							break;
							
						} case 2: {// ---- BOND -----
							int[] atoms = toIntArray(token[0]); 
							double fc = Double.parseDouble(token[1]);
							double eq = Double.parseDouble(token[2]);
							double dip = Double.parseDouble(token[3]);
							bondParameters.put(getOrdered(atoms), new BondParameters(fc, eq));
							dipoleParameters.put(atoms, dip);
							break;
							
						} case 3:
						  case 8:
						  case 9: {// ---- ANGLE -----
							if(token[2].length()==0) continue;
							int ring = state==3? 0: state==8? 3: state==9? 4: -1;
							
							int[] atoms = getOrdered(toIntArray(token[0]));
							double fc = Double.parseDouble(token[1]);
							for(int nHydrogens = 0; nHydrogens<=2 && token.length>2+nHydrogens && token[2+nHydrogens].length()>0; nHydrogens++) {
								double eq = Double.parseDouble(token[2+nHydrogens]);
								angleParameters.put(new int[]{atoms[0], atoms[1], atoms[2], nHydrogens, ring}, new AngleParameters(fc, eq));
							}
							break;
							
						} case 4:
						  case 10: {// ---- TORSION -----
						  	int ring = state==4? 0: 4;
							int[] atoms = getOrdered(toIntArray(token[0]));
							double v1 = Double.parseDouble(token[1]);
							double v2 = Double.parseDouble(token[2]);
							double v3 = Double.parseDouble(token[3]);
							TorsionParameters p = new TorsionParameters(v1, v2, v3);
							torsionParameters.put(new int[]{atoms[0], atoms[1], atoms[2], atoms[3], ring}, p);
							
							break;
							
						} case 5: {// ---- OPBEND -----
							int[] atoms = getOrdered(toIntArray(token[0]));
							double opb = Double.parseDouble(token[1]);
							OutOfPlaneBendParameters p = new OutOfPlaneBendParameters(opb);
							outOfPlaneBendParameters.put(atoms, p);
							break;
							
						} case 6: {// ---- VDW -----
							int atoms = Integer.parseInt(token[0]);
							double rad = Double.parseDouble(token[1]);
							double eps = Double.parseDouble(token[2]);
							double red = Double.parseDouble(token[3]);
							SingleVDWParameters p = new SingleVDWParameters(rad, eps, red);
							singleVDWParameters.put(atoms, p);
							break;
							
						  } case 7: {// ---- VDW PAIRS-----
							  int[] atoms = getOrdered(toIntArray(token[0]));
							  double rad = Double.parseDouble(token[1]);
							  double eps = Double.parseDouble(token[2]);
							  VDWParameters p = new VDWParameters(rad, eps);
							  vdwParameters.put(atoms, p);
							  break;
							
						  } case 11: {// --- STRETCH-BEND ---
 							int atoms = Integer.parseInt(token[0]);
							  double h0 = Double.parseDouble(token[1]);
							  double h1 = Double.parseDouble(token[2]);
							  strBendParameters.put(atoms, new double[]{h0, h1});
							  break;
							  
						  } case 12: {// --- PI-BONDS ---
								String atoms = token[0];
								double t0 = Double.parseDouble(token[1]);
								double t1 = Double.parseDouble(token[2]);
								piBonds.put(atoms, new double[]{t0, t1});
								break;
						  } case 13: {// --- PI-ATOMS ---
								String atoms = token[0];
								double t0 = Double.parseDouble(token[1]);
								double t1 = Double.parseDouble(token[2]);
								double t2 = Double.parseDouble(token[3]);
								piAtoms.put(atoms, new double[]{t0, t1, t2});
						  		break;
						  } case 14: {// --- ELECTRONEGATIVITY ---
								int[] atoms = toIntArray(token[0]);
								double t0 = Double.parseDouble(token[1]);
								electronegativity.put(atoms, new Double(t0));
						  		break;
						  } default:
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					System.err.println("Error on "+line);
					throw e;
				}
							
			}
			reader.close();
		} catch(IOException e) {
			throw new Exception("Could not read the MM2 parameter file", e);
		}		
	}
	
	private static final int[] toIntArray(String s) {
		String[] split = s.split("-");
		int n = split.length;
		
		int[] res = new int[n];
		for(int i=0; i<n; i++) res[i] = Integer.parseInt(split[i]);
		return res;		
	}
	
	private static final int[] getOrdered(int[] v) {
		
		if(v[0]<v[v.length-1]) return v;
		if(v.length==4 && v[0]==v[3] && v[1]<v[2]) return v;
		
		//Inverse order
		int[] res = new int[v.length];
		for(int i=0; i<v.length; i++) {
			res[i] = v[v.length-1-i];
		}
		return res;
	}

	@Override
	public void setAtomClassesForMolecule(FFMolecule mol) {
//		long s = System.currentTimeMillis();			

		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			mol.setAtomMM2Description(i, "");
		}
	
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.getAtomicNo(i)>1) {				
				String description = getAtomDescription(mol, i);				
				mol.setAtomMM2Description(i, description==null?"???": description);
			} else if(mol.getAtomicNo(i)==0)  mol.setAtomMM2Description(i, "LP LONE PAIR".intern());
		}
		
		//HYDROGENS
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(mol.getAtomicNo(i)!=1) continue;
			String connDesc = mol.getAtomMM2Description(mol.getConnAtom(i,0)).intern();
			String description;
			
			//String comparison using the intern() representation
			if(connDesc=="O ENOL") 		description = "H ENOL";
			else if(connDesc=="O CARBONYL") 	description = "H CARBOXYL";
			else if(connDesc=="O CARBOXYL") 	description = "H CARBOXYL";
			else if(connDesc=="O PHOSPHATE") 	description = "H PHOSPHATE";
			else if(connDesc.startsWith("O "))	description = "H ALCOHOL";
			
			else if(connDesc=="N AMMONIUM") 	description = "H AMMONIUM";
			else if(connDesc=="N IMMONIUM") 	description = "H AMMONIUM";
			else if(connDesc=="N PYRIDINIUM")	description = "H AMMONIUM";
			else if(connDesc=="N AMIDE") 		description = "H AMIDE";
			else if(connDesc=="N PYRROLE") 		description = "H AMIDE";
			else if(connDesc=="N GUANIDINE") 	description = "H GUANIDINE";
			else if(connDesc.startsWith("N ")) 	description = "H AMINE";
			
			else if(connDesc=="S THIOL") 		description = "H THIOL";
			else if(connDesc=="S THIOETHER") 	description = "H THIOL";
			else description = "H";
			mol.setAtomMM2Description(i, description.intern());
		}
		
		for(int i=0; i<mol.getAllAtoms(); i++) {
			AtomClass a = descriptionToAtom.get(mol.getAtomMM2Description(i));
			if(a==null) {
				System.err.println("Invalid description for "+i+": "+mol.getAtomMM2Description(i));
				mol.setAtomMM2Class(i, 1);
			} else {
				mol.setAtomMM2Class(i, a.atomClass);				
			}
		}
//		logger.finest("MM2 parameters computed in "+(System.currentTimeMillis()-s)+"ms");
	}
	
	@Override
	public boolean addLonePairs(FFMolecule mol) {
		return addLonePairs(mol, false);
	}
	public boolean addLonePairs(FFMolecule mol, boolean alsoRigid) {
		boolean modified = false;
		//Add Lone Pairs
		for(int i=0; i<mol.getAllAtoms(); i++) {
			if(!alsoRigid && mol.isAtomFlag(i, FFMolecule.RIGID)) continue;
			
			int nLonePairs = getNLonePairs(mol, i);
			for(int j=0; j<mol.getAllConnAtoms(i); j++) {
				if(mol.getAtomicNo(mol.getConnAtom(i, j))==0) {
					if(nLonePairs>0) nLonePairs--;
					else {
						mol.deleteAtom(mol.getConnAtom(i, j));
						j--;
						modified = true;
					}					
				} 
			}
			for(int j=0; j<nLonePairs; j++) {
				int lp = mol.addAtom(0);
				mol.setAtomMM2Description(lp, "LP LONE PAIR".intern());
				mol.setAtomMM2Class(lp, 20);
				mol.setAtomFlags(lp, mol.getAtomFlags(i) & ~FFMolecule.PREOPTIMIZED & ~FFMolecule.RIGID);
				mol.addBond(i, lp, 1);
				mol.setCoordinates(lp, mol.getCoordinates(i));
				modified = true;
			}
		}
		return modified;			
	}
	public int getNLonePairs(FFMolecule mol, int i) {
		int nLonePairs = 0;
		if(mol.getAtomMM2Class(i)==6 && mol.getAllConnAtoms(i)>1) nLonePairs = 2;
		else if(mol.getAtomMM2Class(i)==37) nLonePairs = 1;
		else if(mol.getAtomMM2Class(i)==8) nLonePairs = 1;
		else if(mol.getAtomMM2Class(i)==41) nLonePairs = 1;
		else if(mol.getAtomMM2Class(i)==49) nLonePairs = 2;
		else if(mol.getAtomMM2Class(i)==82) nLonePairs = 1;
		else if(mol.getAtomMM2Class(i)==83) nLonePairs = 1;

		return nLonePairs;
	}
	
	private static String getAtomDescription(FFMolecule mol, int a) {
		String description = null;
		
		int atomicNo = mol.getAtomicNo(a);
		int connected = 0;
		int valence = 0;
		
		int ringSize = mol.getRingSize(a);
		int doubleBonds = 0;
		int tripleBonds = 0;		
//		int nonH = 0;
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int order = mol.getConnBondOrder(a, i);
			if(order==2) doubleBonds++;
			else if(order==3) tripleBonds++;
			
			if(mol.getAtomicNo(mol.getConnAtom(a, i))!=0) {
				connected++;
				valence+=order;
//				nonH++;
			}
		}
		int nH = Math.max(0, StructureCalculator.getImplicitHydrogens(mol, a));
		connected += nH;
		valence += nH;
		nH += StructureCalculator.getExplicitHydrogens(mol, a);
//		boolean aromatic = mol.isAromatic(a);

		sw: switch(atomicNo) {
			case 0: { // Lp
				description = "LP LONE PAIR";
				break;				
				
			} case 5: {
				if(mol.getAtomCharge(a)<0) description = "B TETRAHEDRAL";
				else description = "B TRIG PLANAR";
				break;
			} case 6: {
				if(ringSize==3) {
					if(doubleBonds==1) description = "C CYCLOPROPENE";
					else description = "C CYCLOPROPANE";
				} else if(doubleBonds==1) {
					if(connected(mol, a, 8, 2)>=0)  description = "C CARBONYL";			
					else if(connected(mol, a, 16, 2)>=0) description = "C CARBONYL";
					else description = "C ALKENE";						
				} else if(doubleBonds == 2)  {
					description = "C CUMULENE";
				} else if(tripleBonds == 1)  {
					if(StructureCalculator.connected(mol, a, 7, 3)>=0) description = "C ISONITRILE";
					else description = "C ALKYNE";
				} else {
					description = "C ALKANE"; 
				}
				// --> C METAL CO, C CYCLOPENTADIENYL (-0.2e)
				// --> C EPOXY , C CARBOCATION
				break;		
								
			} case 7: { //N
								
				if(valence>3) {
					if(tripleBonds>0) description = "N ISONITRILE";
					else if(doubleBonds>0) description = "N IMMONIUM";
					else description = "N AMMONIUM"; 
					break;
				}
				
				//Analyze the rings
				List<Integer> ringNos = mol.getAtomToRings()[a];
				for (Integer ringNo : ringNos) {
					if(!mol.isAromaticRing(ringNo)) continue;
					int[] ringAtoms = mol.getAllRings().get(ringNo);
					int nN = 0;
					int nSO = 0;
					for (int i = 0; i < ringAtoms.length; i++) {
						if(mol.getAtomicNo(ringAtoms[i])==7) {
							nN++;
						} else if(mol.getAtomicNo(ringAtoms[i])==8 || mol.getAtomicNo(ringAtoms[i])==16 ) {
							nSO++;
						}
					}
					if(ringAtoms.length==6) {
						//(C1=CC=CN=C1)
						if(nN>=2) {description = "N PYRIMIDINE"; break sw;}
						//(C1=CC=NN=C1)
						else {description = "N PYRIDINE"; break sw;}
					} else if(ringAtoms.length==5) {
						if(doubleBonds==0) {
							//(C1=CNC1)
							description = "N PYRROLE";  break sw; 
						} else {
							//(N1=COC1)
							if(nSO>0) {description = "N OXAZOLE"; break sw;}
							//(N1=CNC1)
							else if(nN>0) {description = "N OXAZOLE"; break sw;}
							//??
							else { description = "N IMINE";  break sw;}
						}
					}
					
				}
				
				
				/*
				 *     N
				 * N - C(sp2) - N
				 */
				guanidine:for(int i=0; i<mol.getAllConnAtoms(a); i++) {
					int a2 = mol.getConnAtom(a, i);
					if(mol.getAtomicNo(a2)==6 && mol.getConnAtoms(a2)==3 && mol.getRingSize(a2)<0 && connected(mol, a2, -1, 2)>=0) {
						for(int j=0; j<mol.getAllConnAtoms(a2); j++) {
							if(mol.getAtomicNo(mol.getConnAtom(a2, j))!=7) continue guanidine;
						}
						description = "N GUANIDINE"; break sw;
					}
				}
				
				
				if(tripleBonds>0) {description = "N NITRILE"; break sw;}
				
				for(int i=0; i<mol.getAllConnAtoms(a); i++) {
					int a2 = mol.getConnAtom(a, i);
					if(mol.getAtomicNo(a2)==6 && doubleBonds==0 && connected(mol, a2, -1, 2)>=0) {
						description = "N AMIDE"; break sw;
					} else if(mol.getAtomicNo(a2)==7 && doubleBonds==0 && connected(mol, a2, -1, 2)>=0) {
						description = "N AMIDE"; break sw;
					} else if(mol.getAtomicNo(a2)==16 && doubleBonds==0 && connected(mol, a2, -1, 2)>=0) {
						description = "N SULFONAMIDE"; break sw;
					} else if(!mol.isAromatic(a) && mol.isAromatic(a2)) {
						description = "N CONNAROMATIC"; break sw; 
					}  
				}
				
				if(doubleBonds>0) {description = "N IMINE"; break sw;}				
				description = "N AMINE"; 
				break;
			} case 8: { // O
				
				if(mol.getConnAtoms(a)==0) {description = "O WATER"; break sw;}
				
				if(mol.isAromatic(a)) { description = "O FURAN"; break sw;}
				if(connected(mol, a, 15, -1)>=0) {description = "O PHOSPHATE"; break sw;}
				
				if(mol.getConnAtoms(a)==1 && connected(mol, mol.getConnAtom(a, 0), -1, 2)>=0) {
					//     R1   R2
					//        C(sp2)
					//        OH
					int a2 = mol.getConnAtom(a, 0);
					int r1 = mol.getConnAtom(a2, 0);
					int r2 = mol.getConnAtom(a2, 1);
					int r3 = mol.getConnAtom(a2, 2);
				
					if(r1==a) {r1 = r3; }
					else if(r2==a) {r2 = r3; }
					
					//O=(C)C -> Oxo
					if(doubleBonds>0 && mol.getAtomicNo(r1)==6 && mol.getAtomicNo(r2)==6) {description = "O OXO"; break sw;} 
					//O=CN -> amide
					if(doubleBonds>0 && (mol.getAtomicNo(r1)==7 || mol.getAtomicNo(r2)==7)) {description = "O AMIDE"; break sw;}
					//C(=O)OH -> Carboxyl
					if((mol.getAtomicNo(r1)==8 && mol.getConnAtoms(r1)==1) || (mol.getAtomicNo(r2)==8 && mol.getConnAtoms(r2)==1)) {description = "O CARBOXYL"; break sw;}					
					//HO(C)C -> enol
					if(doubleBonds==0 && mol.getAtomicNo(r1)==6 && mol.getAtomicNo(r2)==6) {description = "O ENOL"; break sw;}

					if(doubleBonds>0) {description = "O CARBONYL"; break sw;}
				}

				if(mol.getConnAtoms(a)==2) {
					if(mol.getConnAtoms(mol.getConnAtom(a, 0))==3 || mol.getConnAtoms(mol.getConnAtom(a, 1))==3) {
						//COC(=C)C
						description = "O CARBOXYL";
					} else {
						//COC
						description = "O ETHER";					
					}
				} else { 
					if(doubleBonds>0) {
						//O=...
						description = "O OXO";
					} else {
						//HO-
						description = "O ALCOHOL";
					}
				} 
				break;
				
			} case 9: { // F
				description = "F";
				break;
				
			} case 11: { // Na
				description = "NA";
				break;
				
			} case 12: { // Mg
				description = "MG";
				break;
				
			} case 13: { // Al
				description = "AL TRIG PLANAR";
				break;
				
			} case 15: { // P
				description = "P PHOSPHATE";
				break;
				
			} case 16: { // S
				//if(mol.getAtomCharge(a)>0) description = "S SULFONIUM";
				if(ringSize==5 && mol.isAromatic(a)) description = "S THIOPHENE";
				else if(doubleBonds==0 && nH>0) description = "S THIOL";
				else if(doubleBonds==0) description = "S THIOETHER";
				else if(connected(mol, a, 8, -1)>=0) description = "S SULFONE";
				//else if(connected(mol, a, 8, -1)>=0) description = "S SULFOXIDE";
				//else if(connected(mol, a, 6, 2)>=0) description = "S THIOCARBONYL"; 
				//else if(doubleBonds==1 ) description = "S THIO";
				else description = "S SULFONE";
				break;
				
			} case 17: { // Cl
				description = "CL";
				break;
				
			} case 19: { // K
				description = "K";
				break;
				
			} case 20: { // Ca
				description = "CA";
				break;
				
			} case 25: { // Mn
				description = "MN";
				break;
				
			} case 26: { // Fe
				description = "FE OCTAHEDRAL";
				break;
				
			} case 30: { // Zn
				description = "ZN TRIG PLANAR";
				break;
				
			} case 35: { // Br
				description = "BR";
				break;
				
			} case 45: { // Rh
				description = "RH TETRAHEDRAL";
				break;
								
			} case 53: { // I
				description = "I";
				break;
				
			} case 78: { // Pt
				description = "PT SQUARE PLANAR";
				break;
				
			} case 14: { // Pt
				description = "SI SILANE";
				break;
				
			} default: {
				if(MM2Parameters.descriptionToAtom!=null) {
					//Find a class with the same atomicNo
					for(String key : MM2Parameters.descriptionToAtom.keySet()) {
						AtomClass atom = MM2Parameters.descriptionToAtom.get(key);
						if(atom.atomicNo==atomicNo /*&& atom.charge==mol.getAtomCharge(a)*/) {
							if(DEBUG) System.err.println("Warning: used "+atom.description+" for atomicNo "+atomicNo);
							description = atom.description;
							break;
						}
					}
				}
			}
		}
		
		if(description!=null) { 
			return description;
		} else {
			System.err.println("Could not find atom class for " +atomicNo + " " +description + " Val=" + connected + " dbl=" + doubleBonds + " tri="+ tripleBonds + " rsize="+ringSize);
			return null;			
		}
	}
	
	public static int connected(FFMolecule mol, int a, int atomicNo, int bondOrder) {
		for(int i=0; i<mol.getAllConnAtoms(a); i++) {
			int atm = mol.getConnAtom(a, i);
			if(atomicNo>0 && mol.getAtomicNo(atm)!=atomicNo) continue;
			if(bondOrder>0 && mol.getConnBondOrder(a, i)!=bondOrder) continue;
			return atm;
		}
		return -1;
	}



}
