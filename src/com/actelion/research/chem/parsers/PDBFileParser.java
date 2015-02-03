package com.actelion.research.chem.parsers;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.actelion.research.chem.*;
import com.actelion.research.chem.calculator.*;

/**
 * http://www.daylight.com/meetings/mug01/Sayle/m4xbondage.html
 * 
 * @author freyssj
 */
public class PDBFileParser extends AbstractParser {

	private boolean isCreateConnections = true;

	private boolean isCreateBondOrders = true;

	private boolean isLoadHeteroAtoms = true;

	private boolean isLoadHydrogen = false;

	private boolean isLoadWater = true;

	private boolean isLoadSalts = true;

	private boolean isLoadHalfOccupancy = false;

	private boolean isLenient = true;

	private boolean isKeepMainStructureOnly = false;

	private String crystal;

	private Date depositionDate;
	
	private String proteinName;

	private double resolution;
	
	private String classification;
	
	private int loadModel = -1;
	
	private String loadChain = null;
	
	private boolean pqr = false;
	
	public PDBFileParser() {}

	/**
	 * 
	 * @param res
	 * @param mol
	 * @param fileName
	 * @param modelNo
	 * @param atomToGroup
	 * @param markedLigs
	 * @throws Exception
	 */
	private void addMol(List<FFMolecule> res, FFMolecule mol, String fileName, int modelNo, Map<Integer, String> atomToGroup, List<Integer> markedLigs) throws Exception{
		if(mol==null || mol.getAllAtoms()==0) return;
		mol.setAuxiliaryInfo("isPDB", true);		
		
		if(classification!=null) mol.setAuxiliaryInfo("CLASSIFICATION", classification);
		if(proteinName!=null && proteinName.length()<30) {
			mol.setName(proteinName);
		} else {
			mol.setName(fileName);
		}
		if(modelNo>0) mol.setName(mol.getName()+" Model "+ modelNo);

		
		//Clean chains that are alternate positions
		boolean conflictFound = true;
		atomConflictLoop: while(conflictFound) {
			MoleculeGrid grid = new MoleculeGrid(mol, 1);
			conflictFound = false;			
			for (int a=0; a<mol.getAllAtoms(); a++) {
				if(mol.getAtomicNo(a)<=1) continue;
				Set<Integer> atoms = grid.getNeighbours(mol.getCoordinates(a), 1, true);
				if(atoms.size()>1) {
					//We have a conflict, we have to chose one chain
					Set<String> chains = new HashSet<String>();
					boolean canBeDeleted = false;
					for (int at : atoms) {
						if(mol.getAtomAmino(at).endsWith("A")) {
							canBeDeleted = true;
						} else {
							chains.add(mol.getAtomAmino(at));
						}
					}
					if(canBeDeleted && chains.size()>0) {
						List<Integer> toDelete = new ArrayList<Integer>();
						for (String chainId : chains) {
							System.out.println("REMOVE Conflicting chain "+chainId);
							for (int at=0; at<mol.getAllAtoms(); at++) {
								if(mol.getAtomAmino(at).equals(chainId)) {
									toDelete.add(at);
								}
							}
						}
						if(toDelete.size()>0) {
							mol.deleteAtoms(toDelete);
							conflictFound = true;
							continue atomConflictLoop;
						}
					}					
				}			
			}
		}
			
		
		
		
		//Create Connections
		if (isCreateConnections) {
			BondsCalculator.createBonds(mol, isLenient, atomToGroup);
			if (isCreateBondOrders) BondsCalculator.calculateBondOrders(mol, isLenient);

			//Delete Salts or small groups
			List<List<Integer>> groups = StructureCalculator.getConnexComponents(mol);
			//If a group consists of mixed proteins and ligands atoms (ex 1apt)
			for (int i = 0; i < groups.size(); i++) {
				List<Integer> group = groups.get(i);
				int nLigs = 0;
				int nProts = 0;
				for (int a : group) {
					if(mol.isAtomFlag(a, FFMolecule.LIGAND)) {
						nLigs++;
					} else {
						nProts++;
					}					
				}
				if (nLigs>0 && nProts>0) {
					for (int a : group) mol.setAtomFlag(a, FFMolecule.LIGAND, nLigs>nProts);
				}
			}
			

			if (!isLoadSalts()) {
				List<Integer> atomToDelete = new ArrayList<Integer>();
				for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
					if (mol.getAllConnAtoms(i) == 0 && (mol.getAtomicNo(i) != 8 || !(/*isLoadStableWater ||*/ isLoadWater) )) {
						atomToDelete.add(i);
					}
				}

				for (int i = 0; i < groups.size(); i++) {
					List<Integer> group = groups.get(i);
					if (group.size() > 1 && group.size() <= 5) {
						atomToDelete.addAll(group);
					}
				}
				Collections.sort(atomToDelete);
				for (int i = atomToDelete.size() - 1; i >= 0; i--) {
					mol.deleteAtom(atomToDelete.get(i));
				}
			}
			
		}
		
		if(markedLigs.size()>0) {
			mol.setAllAtomFlag(FFMolecule.LIGAND, false);
			for (int atom : markedLigs) mol.setAtomFlag(atom, FFMolecule.LIGAND, true);
			
			boolean connectedToProtein = false;
			for (int atom : markedLigs) {
				for (int i = 0; i < mol.getAllConnAtoms(atom); i++) {
					if(!mol.isAtomFlag(mol.getConnAtom(atom, i), FFMolecule.LIGAND)) {
						System.out.println("Ligand conencted to protein?? Reset");
						connectedToProtein = true; break;
					}
				}
			}
			if(connectedToProtein) for (int atom : markedLigs) mol.setAtomFlag(atom, FFMolecule.LIGAND, false);
		}
		

		
		if (isKeepMainStructureOnly && mol.getAllBonds() > 4000) {
			BondsCalculator.extractMainStructure(new FFMolecule(mol), mol);
		}

		// Mark Backbone
		StructureCalculator.flagBackbone(mol);
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			String n = mol.getAtomName(i);
			if (n != null && (n.equals("CA") || n.equals("C") || n.equals("N"))) {
				mol.setAtomFlag(i, FFMolecule.BACKBONE, true);
			}
		}

		// Load stable water
		//if (/*isLoadStableWater &&*/ isLoadWater) {
			List<int[]> interactions = StructureCalculator.getInterMolecularInteractions(mol);
			int[] cc = new int[mol.getAllAtoms()];
			for (int[] pair: interactions) {
				if (mol.getAtomicNo(pair[0]) == 8
						&& mol.getConnAtoms(pair[0]) == 0
						&& (mol.getAtomicNo(pair[1]) == 7 || mol.getAtomicNo(pair[1]) == 8)
						&& mol.getConnAtoms(pair[1]) > 0)
					cc[pair[0]]++;
				if (mol.getAtomicNo(pair[1]) == 8
						&& mol.getConnAtoms(pair[1]) == 0
						&& (mol.getAtomicNo(pair[0]) == 7 || mol.getAtomicNo(pair[0]) == 8)
						&& mol.getConnAtoms(pair[0]) > 0)
					cc[pair[1]]++;
			}
			List<Integer> keep = new ArrayList<Integer>();
			for (int i = 0; i < cc.length; i++) {
				if (cc[i] >= 2) {
					keep.add(i);
				}
			}
			for (int i = mol.getAllAtoms() - 1; i >= 0; i--) {
				if (mol.getAtomicNo(i) == 8 && mol.getAllConnAtoms(i) == 0 ) {
					if(keep.contains(i)) {					
						mol.setAtomFlag(i, FFMolecule.IMPORTANT, true);
					} else {
						mol.setAtomFlag(i, FFMolecule.IMPORTANT, false);
						if(!isLoadWater()) mol.deleteAtom(i);
					}
				}
			}
		//}

		mol.compact();
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			
			if(mol.getConnAtoms(i)==0) {
				mol.setAtomFlag(i, FFMolecule.LIGAND, false);
			}
			
			mol.setAtomFlag(i, FFMolecule.RIGID, !mol.isAtomFlag(i,FFMolecule.LIGAND));
			mol.setAtomFlag(i, FFMolecule.PREOPTIMIZED, true);
		}
		
		
		res.add(mol);
	}
	
	@Override
	public List<FFMolecule> loadGroup(String fileName, Reader in, int from, int to) throws Exception {
		
		String line;
		resolution = -1;
		int structure = 0;
		LineNumberReader reader = null;
		String dubiousOcc = null;
		
		reader = new LineNumberReader(in);
		Map<String, Integer> allAtomIds = new HashMap<String, Integer>();
		Map<Integer, String> atomToGroup = new HashMap<Integer, String>();
		List<Integer> markedLigs = new ArrayList<Integer>();
		proteinName = "";

		int model = 0;

		FFMolecule mol = new FFMolecule();
		List<FFMolecule> res = new ArrayList<FFMolecule>();

		pqr = false;
		while ((line = reader.readLine()) != null) {
			
			if (line.length() < 7) continue;
			String type = line.substring(0, 7);
			boolean isTypeAtom = type.startsWith("ATOM ");
			boolean isTypeHet = type.startsWith("HETATM");
			if (type.startsWith("MODEL ")) {
				String n = new StringTokenizer(line.substring(6)).nextToken().trim();
				structure++;
				try {
					addMol(res, mol, fileName, model, atomToGroup, markedLigs);
					model = Integer.parseInt(n);					
					mol = new FFMolecule();
					markedLigs.clear();
				} catch (Exception e) {
					System.err.println(e);
				}
			} 
			if(loadModel>=0) {
				if(model!=loadModel) continue;
			}
			
			
			if (type.startsWith("TER ")) {
				structure++;
			} else if (isTypeAtom || (isLoadHeteroAtoms && isTypeHet)) {				
				//New atom to add
				if (line.length() <= 60) {
					if (isLenient) continue;
					throw new IOException("invalid line: " + line);
				}

				String description = line.substring(7, 30).trim();
				String id = line.substring(7, 12).trim();
				String elt2Prefix = line.substring(12, 13).trim().toUpperCase(); // A or N or ''
				String atomName = line.substring(13, 17).trim().toUpperCase(); // Element
				String amino = line.substring(17, 20).trim();
				String chainId = line.substring(21, 22).trim();
				String sequence = line.substring(22, 30).trim(); // Name
				String elt1 = line.length() > 79 ? line.substring(75, 80).trim() : ""; // Element
				String xPosition = line.substring(30, 38).trim(); // X
				String yPosition = line.substring(38, 46).trim(); // Y
				String zPosition = line.substring(46, 54).trim(); // Z
				String occupancy = line.substring(54, 62).trim();
				String alt2 = line.substring(16, 17).trim();
				String elt;

				if(loadChain!=null && !loadChain.equals(chainId) && !isTypeHet && !amino.equals("ACT")) continue;
				
				if(atomName.length()==0) continue;
				if (!isLoadHalfOccupancy && alt2.equals("B")) continue; // Alternate position
				if (!isLoadHalfOccupancy && alt2.equals("2")) continue; // Alternate position
				if (!isLoadHalfOccupancy && alt2.equals("3")) continue; // Alternate position
				
				if (elt1.length() > 0 && elt1.length() < 3) {
					elt = elt1;
				} else {
					if (Character.isDigit(atomName.charAt(0))) {
						elt = atomName.substring(1);
					} else {
						elt = atomName;
					}
					if (elt2Prefix.length() > 0
							&& !Character.isDigit(elt2Prefix.charAt(0))
							&& elt2Prefix.charAt(0) != 'N'
							&& elt2Prefix.charAt(0) != 'A') {
						elt = elt2Prefix + elt;
						elt2Prefix = "";
					}
				}
				
				if (elt.equals("D") || elt.startsWith("Q") || elt.startsWith("DUM")) continue; // Dummy atom
			
				if (/*!isLoadStableWater &&*/ !isLoadWater && amino.equalsIgnoreCase("HOH")) continue;

				if(!pqr) {
					double occ = 1;
				
					try { occ = Double.parseDouble(occupancy);} catch (Exception e) {}
					if (occ>0) {
//						if (sequence.equals(previousResidue)) count++;
//						previousResidue = sequence;	
		
						// Treat occupancy <=.5
						if(!isLoadHalfOccupancy) {
							if((amino.endsWith("B") || amino.startsWith("2")  || amino.startsWith("3")) && occ<=.5) continue;
							if (occ == 0.5) {							
								if (description.equalsIgnoreCase(dubiousOcc)) continue;
								dubiousOcc = description;
							} else {
								if (occ <= 0.3) continue;
								dubiousOcc = null;
							}
						}
					}	
				}
				 
//				read++;

				int atomNo = 0;
				if (elt.startsWith("'")) elt = elt.substring(1);
				else if (elt.startsWith("R")) elt = elt.substring(1);
				else if (elt.startsWith("X")) elt = elt.substring(1);
				else if (elt.startsWith("*")) elt = elt.substring(1);
				
				elt = wipeDigits(elt);
				
				if (elt.equals("AD1")) atomNo = 8;
				else if (elt.equals("AE1")) atomNo = 8;
				else if (elt.equals("AD2")) atomNo = 7;
				else if (elt.equals("AE2")) atomNo = 7;
				else if (elt.startsWith("H")) atomNo = 1;
				else if (elt.startsWith("FE")) atomNo = Molecule.getAtomicNoFromLabel("Fe");
				else if (elt.startsWith("CL")) atomNo = Molecule.getAtomicNoFromLabel("Cl");
				else if (elt.startsWith("BR")) atomNo = Molecule.getAtomicNoFromLabel("Br");
				else if (elt.startsWith("HG")) atomNo = Molecule.getAtomicNoFromLabel("Hg");
				else if (elt.startsWith("SI")) atomNo = Molecule.getAtomicNoFromLabel("Si");
				else if(elt.length()>0) atomNo = Molecule.getAtomicNoFromLabel(wipeDigits(elt.substring(0, 1)));
				
				if ((atomNo >= 100 || atomNo == 0) && elt.length() >= 2) atomNo = Molecule.getAtomicNoFromLabel(wipeDigits(elt.substring(0, 2)));
				if ((atomNo >= 100 || atomNo == 0) && elt.length() >= 2) atomNo = Molecule.getAtomicNoFromLabel(wipeDigits(elt.substring(1)));
				if ((atomNo >= 100 || atomNo == 0) && (elt2Prefix + elt).length() >= 2) atomNo = Molecule.getAtomicNoFromLabel((elt2Prefix + elt) .substring(0, 2));

				if (atomNo >= 100 || atomNo == 0) {
					if (isLenient) {System.err.println("Unknown atom " + elt);continue;}
					throw new IOException("The file has an element '" + elt + "' with an atomic number of " + atomNo + " at line " + reader.getLineNumber());
				}
				if (atomNo >= 2 || (isLoadHydrogen && atomNo >= 1)) {
					double x = Double.parseDouble(xPosition);
					double y = Double.parseDouble(yPosition);
					double z = Double.parseDouble(zPosition);

					if (x >= 9999) {
						System.err.println("Invalid coordinates for " + elt);
						continue; // Dummy atom
					}

					String name = "";
					for (int i = 0; i < atomName.length() && name.length() < 2; i++) {
						if (Character.isLetter(atomName.charAt(i))) name += atomName.charAt(i);
					}
					if (name == "") name = Molecule.cAtomLabel[mol.getAtomicNo(atomNo)];

					// Add the atom		
					int atom = mol.addAtom(atomNo);
					mol.setAtomX(atom, x);
					mol.setAtomY(atom, y);
					mol.setAtomZ(atom, z);
					mol.setAtomDescription(atom, description);
					mol.setAtomName(atom, atomName);
					mol.setAtomAmino(atom, amino);
					if(pqr) {
						try {
							mol.setPartialCharge(atom, Double.parseDouble(occupancy));
						} catch (Exception e) {
						}
					}
					try {
						mol.setAtomSequence(atom, Integer.parseInt(sequence));
					} catch (Exception e) {
					}
					mol.setAtomChainId(atom, chainId);
					
					if ("LIG".equals(amino) || "ACT".equals(amino)) markedLigs.add(atom);
					
					allAtomIds.put(id, atom);
					
					String groupId  = isTypeAtom? (""+structure) : (sequence);
					atomToGroup.put(atom, groupId);
					if(isTypeHet)  mol.setAtomFlag(atom, FFMolecule.LIGAND, true);
				}
			} else if(type.startsWith("BOND ") ) { // Bond information (ACT specific)
				String id1 = line.substring(7, 13).trim();
				String id2 = line.substring(13, 18).trim();
				String order = line.substring(18, 23).trim();
				Integer atom1 = allAtomIds.get(id1);
				Integer atom2 = allAtomIds.get(id2);
				mol.addBond(atom1, atom2, Integer.parseInt(order));
				
			} else if (line.startsWith("CRYST1")) {
				crystal = line.substring(7);
				if (crystal.length() > 63)
					crystal = crystal.substring(0, 63);
				crystal = crystal.trim();
			} else if (line.startsWith("ORIGX")) {

			} else if (line.startsWith("REMARK")) {
				if (line.indexOf(" PQR ") > 0) {
					pqr = true;
				}
				if (line.indexOf(" RESOLUTION. ") > 0) {
					try {
						String s = line
								.substring(line.indexOf(" RESOLUTION. ") + 2);
						StringTokenizer st = new StringTokenizer(s);
						st.nextToken();
						if (st.hasMoreTokens()) {
							String resString = st.nextToken();
							resolution = Double.parseDouble(resString);
							if(resolution>100) resolution/=100;
						}
					} catch (Exception e) {
						// Nothing
					}
				} else if (line.indexOf(" RESOLUTION RANGE HIGH ") > 0
						&& resolution < 0) {
					try {
						int end = line.indexOf("      ");
						if (end > 0)
							line = line.substring(0, end);
						StringTokenizer st = new StringTokenizer(line);
						String last = "";
						while (st.hasMoreTokens())
							last = st.nextToken();
						resolution = Double.parseDouble(last);
					} catch (Exception e) {
						// Nothing
					}
				}
			} else if (line.startsWith("TITLE")) {				
				proteinName += (proteinName.length()>0?" ":"")+line.substring(10).trim();
			} else if (line.startsWith("HEADER")) {
				try {
					classification = line.substring(10,50).trim();
					depositionDate = new SimpleDateFormat("dd-MMM-yy").parse(line.substring(50,60).trim());
				} catch (Exception e) {					
				}
			}
		}
		
		addMol(res, mol, fileName, model, atomToGroup, markedLigs);
		
		in.close();

		return res;

	}

	/**
	 * @return
	 */
	public boolean isCreateConnections() {
		return isCreateConnections;
	}

	/**
	 * @return
	 */
	public boolean isLoadHeteroAtoms() {
		return isLoadHeteroAtoms;
	}

	/**
	 * @param b
	 */
	public void setCreateConnections(boolean b) {
		isCreateConnections = b;
	}

	/**
	 * @param b
	 */
	public void setLoadHeteroAtoms(boolean b) {
		isLoadHeteroAtoms = b;
	}

	/**
	 * @return
	 */
	public boolean isLoadHydrogen() {
		return isLoadHydrogen;
	}

	/**
	 * @param b
	 */
	public void setLoadHydrogen(boolean b) {
		isLoadHydrogen = b;
	}

	/**
	 * @return
	 */
	public boolean isLoadHalfOccupancy() {
		return isLoadHalfOccupancy;
	}

	/**
	 * @param b
	 */
	public void setLoadHalfOccupancy(boolean b) {
		isLoadHalfOccupancy = b;
	}

	/**
	 * @return
	 */
	public boolean isCreateBondOrders() {
		return isCreateBondOrders;
	}

	/**
	 * @param b
	 */
	public void setCreateBondOrders(boolean b) {
		isCreateBondOrders = b;
	}

	/**
	 * @return
	 */
	public double getResolution() {
		return resolution;
	}

	/**
	 * @return
	 */
	public boolean isLoadWater() {
		return isLoadWater;
	}

	/**
	 * @param b
	 */
	public void setLoadWater(boolean b) {
		isLoadWater = b;
	}

	/**
	 * @return
	 */
	public boolean isLenient() {
		return isLenient;
	}

	/**
	 * @param b
	 */
	public void setLenient(boolean b) {
		isLenient = b;
	}

	/**
	 * @return
	 */
	public String getProteinName() {
		return proteinName;
	}

	/**
	 * @return
	 */
	public boolean isLoadSalts() {
		return isLoadSalts;
	}

	/**
	 * @param b
	 */
	public void setLoadSalts(boolean b) {
		isLoadSalts = b;
	}

	/**
	 * @return
	 */
	public String getCrystal() {
		return crystal;
	}

	public static final ParserFileFilter FILEFILTER = new ParserFileFilter() {
		public boolean accept(File f) {
			return f.getName().toUpperCase().endsWith(".PDB")
					|| f.getName().toUpperCase().endsWith(".ENT")
					|| f.getName().toUpperCase().endsWith(".PQR")
					|| f.isDirectory();
		}

		public String getDescription() {
			return "PDB File";
		}

		public String getExtension() {
			return ".pdb";
		}
	};

	/**
	 * @return
	 */
	public boolean isLoadMainStructureOnly() {
		return isKeepMainStructureOnly;
	}

	/**
	 * @param b
	 */
	public void setLoadMainStructureOnly(boolean b) {
		isKeepMainStructureOnly = b;
	}

	@Override
	public void save(FFMolecule mol, Writer writer) throws Exception {
		save(Collections.singletonList(mol), writer);
	}
	
	@Override
	public void save(List<FFMolecule> mols, Writer writer) throws Exception {
		DecimalFormat df = new DecimalFormat("0.000");
		
		// Write the atoms
		writer.write("HEADER    " + NEWLINE);
		writer.write("REMARK   1 AUTH WRITTEN BY ACT3D" + NEWLINE);
		int model = 0;
		for (FFMolecule mol : mols) {
			if(mols.size()>0) {
				writer.write("MODEL " + (model++));
				writer.write(NEWLINE);
			}
			int count = 0;
			for (int t = 0; t < 2; t++) {
				for (int i = 0; i < mol.getAllAtoms(); i++) {
	
					if (mol.getAtomicNo(i) <= 0) continue; // Lp
					if (t == 0 && mol.isAtomFlag(i, FFMolecule.LIGAND)) continue;
					if (t == 1 && !mol.isAtomFlag(i, FFMolecule.LIGAND)) continue;
						
					String name = mol.getAtomName(i);
					if(name==null) name = Molecule.cAtomLabel[mol.getAtomicNo(i)];
					
					String chain = mol.getAtomChainId(i);
					if(chain==null) chain = t == 0 ? "A" : "L";
					
					String amino = mol.getAtomAmino(i);
					if(amino==null) amino = t == 0 ? "PRO" : "LIG";
	
					int seq = mol.getAtomSequence(i);
	
					if (t == 0) writeL(writer, "ATOM  ", 6);
					else if (t == 1) writeL(writer, "HETATM", 6);
					
					writeR(writer, "" + (++count), 5);
					writeR(writer, "  ", 2);
					writeL(writer, name, 4);
					writeR(writer, amino, 3);
					writeR(writer, chain, 2);
					writeR(writer, "" + seq, 4);
				
					writeR(writer, df.format(mol.getAtomX(i)), 12);
					writeR(writer, df.format(mol.getAtomY(i)), 8);
					writeR(writer, df.format(mol.getAtomZ(i)), 8);
					writer.write("  1.00  0.00");
					writeR(writer, ExtendedMolecule.cAtomLabel[mol.getAtomicNo(i)],
							12);
					writer.write(NEWLINE);
				}
			}
		}
	}

	
	private static String wipeDigits(String s) {
		String res = "";
		for (int i = 0; i < s.length(); i++) {
			if(Character.isLetter(s.charAt(i))) res+=s.charAt(i);
		}
		return res;
	}

	public Date getDepositionDate() {
		return depositionDate;
	}
	
	public String getClassification() {
		return classification;
	}

	public int getLoadModel() {
		return loadModel;
	}

	public void setLoadModel(int loadModel) {
		this.loadModel = loadModel;
	}

	public String getLoadChain() {
		return loadChain;
	}

	public void setLoadChain(String loadChain) {
		this.loadChain = loadChain;
	}
	

	public static void main(String[] args) throws Exception {
		List<FFMolecule> res = new PDBFileParser().loadGroup("D:/dev/Java/Actelion3D/DUDtest/kinHSP90/1uy6.pdb");
		new PDBFileParser().save(res.get(0), "c:/1uy6.pdb");
		new PDBFileParser().loadGroup("c:/1uy6.pdb");
		System.out.println("models=" +res.size());
	}
}
