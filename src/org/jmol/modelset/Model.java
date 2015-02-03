/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-11 23:55:37 +0200 (mar., 11 août 2009) $
 * $Revision: 11309 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.modelset;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;

import org.jmol.util.BitSetUtil;
import org.jmol.viewer.StateManager.Orientation;

public final class Model {

  /*
   * In Jmol all atoms and bonds are kept as a set of arrays in 
   * the AtomCollection and BondCollection objects. 
   * Thus, "Model" is not atoms and bonds. 
   * It is a description of all the:
   * 
   * chains (as defined in the file)
   *   and their associated file-associated groups,  
   * polymers (same, I think, but in terms of secondary structure)
   *   and their associated monomers
   * molecules (as defined by connectivity)
   *  
   * Note that "monomer" extends group. A group only becomes a 
   * monomer if it can be identified as one of the following 
   * PDB/mmCIF types:
   * 
   *   amino  -- has an N, a C, and a CA
   *   alpha  -- has just a CA
   *   nucleic -- has C1',C2',C3',C4',C5',O3', and O5'
   *   phosphorus -- has P
   *   
   * The term "conformation" is a bit loose. It means "what you get
   * when you go with one or another set of alternative locations.
   *
   * A Model then is just a small set of fields, a few arrays pointing
   * to other objects, and a couple of hash tables for information storage
   * 
   * Additional information here includes
   * how many atoms there were before symmetry was applied
   * as well as a bit about insertions and alternative locations.
   * 
   * 
   * one model = one animation "frame", but we don't use the "f" word
   * here because that would confuse the issue.
   * 
   * If multiple files are loaded, then they will appear here in 
   * at least as many Model objects. Each vibration will be a complete
   * set of atoms as well.
   * 
   * Jmol 11.3.58 developed the trajectory idea -- where
   * multiple models may share the same structures, bonds, etc., but
   * just differ in atom positions, saved in the Trajectories Vector
   * in ModelCollection.
   *  
   */
  
  ModelSet modelSet;
 
  /**
   * BE CAREFUL: FAILURE TO NULL REFERENCES TO modelSet WILL PREVENT FINALIZATION
   * AND CREATE A MEMORY LEAK.
   * 
   * @return associated ModelSet
   */
  public ModelSet getModelSet() {
    return modelSet;
  }

  int modelIndex;   // our 0-based reference
  int fileIndex;   // 0-based file reference

  boolean isPDB;
  boolean isTrajectory;
  int trajectoryBaseIndex;
  int selectedTrajectory = -1;
  boolean hasCalculatedHBonds;
  
  Hashtable dataFrames;
  int dataSourceFrame = -1;
  String jmolData; // from a PDB remark "Jmol PDB-encoded data"
  String jmolFrameType;

  int firstAtomIndex;
  int atomCount = -1;
  BitSet bsAtoms;
  
  int bondCount = -1;

  int firstMolecule;
  int moleculeCount;
  
  int nAltLocs;
  int nInsertions;
  
  int groupCount = -1;

  int chainCount = 0;
  Chain[] chains = new Chain[8];

  int bioPolymerCount = 0;
  Polymer[] bioPolymers = new Polymer[8];

  int biosymmetryCount;

  Hashtable auxiliaryInfo;
  Properties properties;
  float defaultRotationRadius;

  Orientation orientation;

  Model(ModelSet modelSet, int modelIndex, int trajectoryBaseIndex, 
      String jmolData, Properties properties, Hashtable auxiliaryInfo) {
    this.modelSet = modelSet;
    dataSourceFrame = this.modelIndex = modelIndex;
    this.jmolData = jmolData;
    isTrajectory = (trajectoryBaseIndex >= 0);
    this.trajectoryBaseIndex = (isTrajectory ? trajectoryBaseIndex : modelIndex);
    if (auxiliaryInfo == null)
      auxiliaryInfo = new Hashtable();
    this.auxiliaryInfo = auxiliaryInfo;
    if (auxiliaryInfo.containsKey("biosymmetryCount"))
      biosymmetryCount = ((Integer)auxiliaryInfo.get("biosymmetryCount")).intValue();
    this.properties = properties;
    if (jmolData == null) {
      jmolFrameType = "modelSet";
    } else {
      auxiliaryInfo.put("jmolData", jmolData);
      auxiliaryInfo.put("title", jmolData);
      jmolFrameType = (jmolData.indexOf("ramachandran") >= 0 ? "ramachandran"
          : jmolData.indexOf("quaternion") >= 0 ? "quaternion" 
          : "data");
    }
  }

  void setNAltLocs(int nAltLocs) {
    this.nAltLocs = nAltLocs;  
  }
  
  void setNInsertions(int nInsertions) {
    this.nInsertions = nInsertions;  
  }
  
  void addSecondaryStructure(byte type, 
                             String structureID, int serialID, int strandCount,
                             char startChainID, int startSeqcode,
                             char endChainID, int endSeqcode) {
    for (int i = bioPolymerCount; --i >= 0; ) {
      Polymer polymer = bioPolymers[i];
      polymer.addSecondaryStructure(type, structureID, serialID, strandCount, startChainID, startSeqcode,
                                    endChainID, endSeqcode);
    }
  }

  boolean structureTainted;
  void calculateStructures() {
    structureTainted = modelSet.proteinStructureTainted = true;
    for (int i = bioPolymerCount; --i >= 0; ) {
      bioPolymers[i].clearStructures();
      bioPolymers[i].calculateStructures();
    }
  }

  public boolean isStructureTainted() {
    return structureTainted;
  }
  
  void setConformation(BitSet bsConformation) {
    for (int i = bioPolymerCount; --i >= 0; )
      bioPolymers[i].setConformation(bsConformation, nAltLocs);
  }

  public Chain[] getChains() {
    return chains;
  }

  
  public int getChainCount(boolean countWater) {
    if (chainCount > 1 && !countWater)
      for (int i = 0; i < chainCount; i++)
        if (chains[i].chainID == '\0')
          return chainCount - 1;
    return chainCount;
  }

  public int getGroupCount(boolean isHetero) {
    int n = 0;
    for (int i = chainCount; --i >= 0;)
      for (int j = chains[i].groupCount; --j >= 0;)
        if (chains[i].groups[j].isHetero() == isHetero)
          n++;
    return n;
  }
  
  public int getBioPolymerCount() {
    return bioPolymerCount;
  }

  void calcSelectedGroupsCount(BitSet bsSelected) {
    for (int i = chainCount; --i >= 0; )
      chains[i].calcSelectedGroupsCount(bsSelected);
  }

  void calcSelectedMonomersCount(BitSet bsSelected) {
    for (int i = bioPolymerCount; --i >= 0; )
      bioPolymers[i].calcSelectedMonomersCount(bsSelected);
  }

  void selectSeqcodeRange(int seqcodeA, int seqcodeB, char chainID, BitSet bs,
                          boolean caseSensitive) {
    for (int i = chainCount; --i >= 0;)
      if (chainID == '\t' || chainID == chains[i].chainID || !caseSensitive
          && chainID == Character.toUpperCase(chains[i].chainID))
        for (int index = 0; index >= 0;)
          index = chains[i].selectSeqcodeRange(index, seqcodeA, seqcodeB, bs);
  }

  int getGroupCount() {
    if (groupCount < 0) {
      groupCount = 0;
      for (int i = chainCount; --i >= 0;)
        groupCount += chains[i].getGroupCount();
    }
    return groupCount;
  }

  Chain getChain(char chainID) {
    for (int i = chainCount; --i >= 0; ) {
      Chain chain = chains[i];
      if (chain.getChainID() == chainID)
        return chain;
    }
    return null;
  }

  Chain getChain(int i) {
    return (i < chainCount ? chains[i] : null);
  }

  public Polymer getBioPolymer(int polymerIndex) {
    return bioPolymers[polymerIndex];
  }

  void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    for (int i = bioPolymerCount; --i >= 0;) {
      int type = bioPolymers[i].getType();
      if (type != Polymer.TYPE_AMINO && type != Polymer.TYPE_NUCLEIC)
        continue;
      if (type == Polymer.TYPE_AMINO)
        bioPolymers[i].calcHydrogenBonds(null, bsA, bsB);
      for (int j = bioPolymerCount; --j >= 0;)
        if (i != j && bioPolymers[i] != null
            && type == bioPolymers[j].getType())
          bioPolymers[j].calcHydrogenBonds(bioPolymers[i], bsA, bsB);
    }
  }
  
  public boolean isAtomHidden(int index) {
    return modelSet.isAtomHidden(index);
  }
  
  public void addHydrogenBond(Atom atom1, Atom atom2, short order, BitSet bsA, BitSet bsB, float energy) {
    hasCalculatedHBonds = true;
    modelSet.addHydrogenBond(atom1, atom2, order, bsA, bsB, energy);
    //System.out.println("addHB " + atom1.getInfo() + " "+ atom2.getInfo() + " " + atom1.distance(atom2));
  }

  public int getModelIndex() {
    return modelIndex;
  }

  void fixIndices(int modelIndex, int nAtomsDeleted, BitSet bsDeleted) {
    if (dataSourceFrame > modelIndex)
      dataSourceFrame--;
    if (trajectoryBaseIndex > modelIndex)
      trajectoryBaseIndex--;
    firstAtomIndex -= nAtomsDeleted;
    for (int i = 0; i < chainCount; i++)
      chains[i].fixIndices(nAtomsDeleted);
    for (int i = 0; i < bioPolymerCount; i++)
      bioPolymers[i].recalculateLeadMidpointsAndWingVectors();
    BitSetUtil.deleteBits(bsAtoms, bsDeleted);
  }
}
