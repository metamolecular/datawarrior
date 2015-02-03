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
package com.actelion.research.gui.jmol;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolFileReaderInterface;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.chem.conf.VDWRadii;

/**
 * Wrapper to Jmol, using our FFMolecule implementation as model
 */
@SuppressWarnings("rawtypes")
public class ActelionJmolAdapter extends org.jmol.api.JmolAdapter {

	public ActelionJmolAdapter() {
		super("Actelion");		
	}

	
	@Override
	public AtomIterator getAtomIterator(Object clientFile) {
		return new AtomIterator((FFMolecule) clientFile);
	}

	@Override
	public int getEstimatedAtomCount(Object clientFile) {
		return ((FFMolecule)clientFile).getAllAtoms();
	}

	@Override
	public boolean coordinatesAreFractional(Object clientFile) {
		return false;
	}

	@Override
	public void finish(Object clientFile) {
	}

	@Override
	public String getAdapterName() {
		return "ActelionJmolAdapter";
	}

	@Override
	public Hashtable getAtomSetAuxiliaryInfo(Object clientFile, int atomSetIndex) {
		return new Hashtable();
	}

	@Override
	public Hashtable getAtomSetCollectionAuxiliaryInfo(Object clientFile) {
		return ((FFMolecule) clientFile).getAuxiliaryInfos();

	}

	@Override
	public String getAtomSetCollectionName(Object clientFile) {
		return ((FFMolecule) clientFile).getName();
	}

	@Override
	public Properties getAtomSetCollectionProperties(Object clientFile) {
		return new Properties();
	}

	@Override
	public int getAtomSetCount(Object clientFile) {
		return 1;
	}

	@Override
	public String getAtomSetName(Object clientFile, int atomSetIndex) {
		return "";
	}

	@Override
	public int getAtomSetNumber(Object clientFile, int atomSetIndex) {
		return 1;
	}

	@Override
	public Properties getAtomSetProperties(Object clientFile, int atomSetIndex) {
		return new Properties();
	}

	@Override
	public BondIterator getBondIterator(Object clientFile) {
		((FFMolecule) clientFile).reorderAtoms();
		return new BondIterator((FFMolecule) clientFile);
	}

	@Override
	public String getFileTypeName(Object clientFile) {
		if (clientFile == null) {
			return null;
		}
		if (clientFile instanceof BufferedReader) {
			String type = Resolver.getFileType((BufferedReader) clientFile);
			return type;
		}
		return "NOTYPE";

	}


	//@Override
	public Object openBufferedReader(String name, String type, BufferedReader bufferedReader, Hashtable htParams) {
		try {
			FFMolecule m = Resolver.resolve(name, type, bufferedReader, htParams);
			m.reorderAtoms();
			return m;
		} catch (Exception e) {
			e.printStackTrace();
			bufferedReader = null;
			return "" + e;
		}
	}

	
	@Override
	public String[] specialLoad(String name, String type) {
		return null;
	}

	
	class AtomIterator extends JmolAdapter.AtomIterator {
		private FFMolecule mol;
		private int iatom;
		private int atomCount;
		private int[] a2g;

		AtomIterator(FFMolecule mol) {
			this.mol = mol;
			this.atomCount = mol.getAllAtoms();
			iatom = -1;
			a2g = StructureCalculator.getAtomToGroups(mol);
		}
		
		@Override
		public boolean hasNext() {
			iatom++;
			if (iatom >= atomCount) return false;
			return true;
		}

		@Override
		public Object getUniqueID() {
			return iatom + 1;
		}

		@Override
		public float getX() {
			return (float) mol.getAtomX(iatom);
		}

		@Override
		public float getY() {
			return (float) mol.getAtomY(iatom);
		}

		@Override
		public float getZ() {
			return (float) mol.getAtomZ(iatom);
		}
		
		@Override
		public String getElementSymbol() {
			return Molecule.cAtomLabel[mol.getAtomicNo(iatom)];
		}
		
		@Override
		public int getElementNumber() {
			return mol.getAtomicNo(iatom)>0?mol.getAtomicNo(iatom):99;
		}

		@Override
		public boolean getIsHetero() {
			return iatom<mol.getNMovables();
		}
		
		@Override
		public int getFormalCharge() {
			return mol.getAtomCharge(iatom);
		}
		@Override
		public String getGroup3() {
			return mol.getAtomAmino(iatom);
		}
		
		@Override
		public int getSequenceNumber() {
			return mol.getAtomSequence(iatom);
		}
		
		@Override
		public char getChainID() {
			String chain = mol.getAtomChainId(iatom); 
			return chain!=null && chain.length()>0? chain.charAt(0) : (char)('Z' + 1 - a2g[iatom]);
		}
		
		@Override
		public int getAtomSerial() {
			return iatom+1;
		}
		
		@Override
		public float getPartialCharge() {
			return (float) mol.getPartialCharge(iatom);
		}
		
		@Override
		public int getAtomSite() {
			return iatom;
		}
		
		@Override
		public float getRadius() {			
			return VDWRadii.VDW_RADIUS[getElementNumber()];
		}
		
		
		@Override
		public String getAtomName() {	
			return mol.getAtomName(iatom);
		}
			
		
	}
	
	class BondIterator extends JmolAdapter.BondIterator {
		private FFMolecule mol;
		int ibond;
		int bondCount;
		
		public BondIterator(FFMolecule mol) {
			this.mol = mol;
			ibond = -1;
			bondCount = mol.getAllBonds();
		}
		@Override
		public Object getAtomUniqueID1() {
			return mol.getBondAtom(0, ibond)+1;
		}

		@Override
		public Object getAtomUniqueID2() {
			return mol.getBondAtom(1, ibond)+1;
		}

		@Override
		public int getEncodedOrder() {
			return mol.getBondOrder(ibond);
		}
		
		@Override		
		public boolean hasNext() {
			ibond++;
			if(ibond>=bondCount) return false;
			return true;
		}
		
	}
	
	/*
	public class StructureIterator extends JmolAdapter.StructureIterator {
	    int structureCount;
	    Structure[] structures;
	    Structure structure;
	    int istructure;
	    
	    StructureIterator(AtomSetCollection atomSetCollection) {
	      structureCount = atomSetCollection.structureCount;
	      structures = atomSetCollection.structures;
	      istructure = 0;
	    }

	    public boolean hasNext() {
	      if (istructure == structureCount)
	        return false;
	      structure = structures[istructure++];
	      return true;
	    }

	    public int getModelIndex() {
	      return structure.modelIndex;
	    }

	    public String getStructureType() {
	      return structure.structureType;
	    }

	    public String getStructureID() {
	      return structure.structureID;
	    }

	    public int getSerialID() {
	      return structure.serialID;
	    }

	    public char getStartChainID() {
	      return canonizeChainID(structure.startChainID);
	    }
	    
	    public int getStartSequenceNumber() {
	      return structure.startSequenceNumber;
	    }
	    
	    public char getStartInsertionCode() {
	      return canonizeInsertionCode(structure.startInsertionCode);
	    }
	    
	    public char getEndChainID() {
	      return canonizeChainID(structure.endChainID);
	    }
	    
	    public int getEndSequenceNumber() {
	      return structure.endSequenceNumber;
	    }
	      
	    public char getEndInsertionCode() {
	      return structure.endInsertionCode;
	    }

	    public int getStrandCount() {
	      return structure.strandCount;
	    }
	  }
*/
	@Override
	public Object getAtomSetCollectionFromDOM(Object DOMNode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAtomSetCollectionFromReader(String name, String type, BufferedReader bufferedReader, Hashtable htParams) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAtomSetCollectionFromReaders(JmolFileReaderInterface fileReader, String[] names, String[] types, Hashtable[] htParams) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAtomSetCollectionOrBufferedReaderFromZip(InputStream is, String fileName, String[] zipDirectory, Hashtable htParams, boolean asBufferedReader) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClientAtomStringProperty(Object clientAtom, String propertyName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float[] getNotionalUnitcell(Object atomSetCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float[] getPdbScaleMatrix(Object atomSetCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float[] getPdbScaleTranslate(Object atomSetCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StructureIterator getStructureIterator(Object atomSetCollection) {
		return null;
//	    return ((AtomSetCollection)atomSetCollection).structureCount == 0 ? 
//	        null : new StructureIterator((AtomSetCollection)atomSetCollection);
	}
	
	
}
