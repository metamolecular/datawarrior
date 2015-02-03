/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-16 17:49:44 +0200 (dim., 16 août 2009) $
 * $Revision: 11320 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

package org.jmol.adapter.smarter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.BitSet;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.SymmetryInterface;
import org.jmol.api.VolumeDataInterface;
import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;

public class AtomSetCollection {
  
  String fileTypeName;
  String collectionName;
  Properties atomSetCollectionProperties = new Properties();
  Hashtable atomSetCollectionAuxiliaryInfo = new Hashtable();

  
  final static String[] globalBooleans = {"someModelsHaveFractionalCoordinates",
    "someModelsHaveSymmetry", "someModelsHaveUnitcells", "isPDB"};

  final static int GLOBAL_FRACTCOORD = 0;
  final static int GLOBAL_SYMMETRY = 1;
  final static int GLOBAL_latticeCells = 2;
  
   final public static String[] notionalUnitcellTags =
  { "a", "b", "c", "alpha", "beta", "gamma" };

  int atomCount;
  public int getAtomCount() {
    return atomCount;
  }
  
  Atom[] atoms = new Atom[256];
  
  public Atom[] getAtoms() {
    return atoms;
  }
  
  public Atom getAtom(int i) {
    return atoms[i];
  }
  
  int bondCount;
  
  public int getBondCount() {
    return bondCount;
  }
  
  Bond[] bonds = new Bond[256];
  
  public Bond[] getBonds() {
    return bonds;
  }
  
  public Bond getBond(int i) {
    return bonds[i];
  }
  
  int structureCount;
  Structure[] structures = new Structure[16];
  int atomSetCount;
  public int getAtomSetCount() {
    return atomSetCount;
  }
  
  int currentAtomSetIndex = -1;
  public int getCurrentAtomSetIndex() {
    return currentAtomSetIndex;
  }

  int[] atomSetNumbers = new int[16];
  String[] atomSetNames = new String[16];
  int[] atomSetAtomCounts = new int[16];
  int[] atomSetBondCounts = new int[16];
  Properties[] atomSetProperties = new Properties[16];
  Hashtable[] atomSetAuxiliaryInfo = new Hashtable[16];
  int[] latticeCells;

  public String errorMessage;

  //float wavelength = Float.NaN;
  boolean coordinatesAreFractional;
  private boolean isTrajectory;
  public boolean isTrajectory() {
    return isTrajectory;
  }
  
  int trajectoryStepCount = 0;
  Point3f[] trajectoryStep;
  Vector trajectorySteps;

  float[] notionalUnitCell = new float[6]; 
  // expands to 22 for cartesianToFractional matrix as array (PDB)
  
  public AtomSetCollection(String fileTypeName) {
    this.fileTypeName = fileTypeName;
    // set the default PATH properties as defined in the SmarterJmolAdapter
    atomSetCollectionProperties.put("PATH_KEY",
                                    SmarterJmolAdapter.PATH_KEY);
    atomSetCollectionProperties.put("PATH_SEPARATOR",
                                    SmarterJmolAdapter.PATH_SEPARATOR);
  }

  
  /**
   * Creates an AtomSetCollection based on an array of AtomSetCollection
   * 
   * @param array Array of AtomSetCollection
   */
  
  public AtomSetCollection(AtomSetCollection[] array) {
    this("Array");
    setAtomSetCollectionAuxiliaryInfo("isMultiFile", Boolean.TRUE);
    for (int i = 0; i < array.length; i++) {
      appendAtomSetCollection(i, array[i]);
    }
  }

  /**
   * Creates an AtomSetCollection based on a Vector of 
   *   AtomSetCollection or Vector (from zipped zip files)
   * 
   * @param list Vector of AtomSetCollection
   */
  
  public AtomSetCollection(Vector list) {
    this("Array");
    setAtomSetCollectionAuxiliaryInfo("isMultiFile", Boolean.TRUE);
    appendAtomSetCollection(list);
  }

  private void appendAtomSetCollection(Vector list) {
    int n = list.size();
    for (int i = 0; i < n; i++) {
      Object o = list.elementAt(i);
      if (o instanceof Vector)
        appendAtomSetCollection((Vector) o);
      else
        appendAtomSetCollection(i, (AtomSetCollection) o);
    }  
  }
  
  /**
   * Just sets the overall file type after the fact.
   * @param type
   */
  public void setFileTypeName(String type) {
    fileTypeName = type;
  }
  
  public boolean setTrajectory() {
    if (!isTrajectory)
      trajectorySteps = new Vector();
    return (isTrajectory = true);
  }
  
  /**
   * Appends an AtomSetCollection
   * 
   * @param collectionIndex collection index for new model number
   * @param collection AtomSetCollection to append
   */
  protected void appendAtomSetCollection(int collectionIndex,
                                         AtomSetCollection collection) {
    // Initialisations
    int existingAtomsCount = atomCount;
    // auxiliary info
    // Clone each AtomSet
    int clonedAtoms = 0;
    for (int atomSetNum = 0; atomSetNum < collection.atomSetCount; atomSetNum++) {
      newAtomSet();
      atomSetAuxiliaryInfo[currentAtomSetIndex] = collection.atomSetAuxiliaryInfo[atomSetNum];
      setAtomSetAuxiliaryInfo("title", collection.collectionName);    
      setAtomSetName(collection.getAtomSetName(atomSetNum));
      Properties properties = collection.getAtomSetProperties(atomSetNum);
      if (properties != null) {
        Enumeration props = properties.keys();
        while ((props != null) && (props.hasMoreElements())) {
          String key = (String) props.nextElement();
          setAtomSetProperty(key, properties.getProperty(key));
        }
      }
      for (int atomNum = 0; atomNum < collection.atomSetAtomCounts[atomSetNum]; atomNum++) {
        try {
          newCloneAtom(collection.atoms[clonedAtoms]);
        } catch (Exception e) {
          errorMessage = "appendAtomCollection error: " + e;
        }
        clonedAtoms++;
      }

      //Structures: We must incorporate any global structures (modelIndex == -1) into this model
      //explicitly. Whew! This is because some cif _data structures have multiple PDB models (1skt)
      for (int i = 0; i < collection.structureCount; i++)
        if (collection.structures[i].modelIndex == atomSetNum
            || collection.structures[i].modelIndex == -1)
          addStructure(collection.structures[i]);

      // names and numbers
      atomSetNames[currentAtomSetIndex] = collection.atomSetNames[atomSetNum];
      
      atomSetNumbers[currentAtomSetIndex] = ((collectionIndex + 1) * 1000000)
          + collection.atomSetNumbers[atomSetNum];

      // Note -- this number is used for Model.modelNumber. It is a combination of
      // file number * 1000000 + PDB MODEL NUMBER, which could be anything.
      // Adding the file number here indicates that we have multiple files.
      // But this will all be adjusted in ModelLoader.finalizeModels(). BH 11/2007
      
    }
    // Clone bonds
    for (int bondNum = 0; bondNum < collection.bondCount; bondNum++) {
      Bond bond = collection.bonds[bondNum];
      addNewBond(bond.atomIndex1 + existingAtomsCount, bond.atomIndex2
          + existingAtomsCount, bond.order);
    }
    // Set globals
    for (int i = globalBooleans.length; --i >= 0;)
      if (Boolean.TRUE.equals(
          collection.getAtomSetCollectionAuxiliaryInfo(globalBooleans[i])))
        setGlobalBoolean(i);
  }

  private boolean noAutoBond;
  void setNoAutoBond() {
    noAutoBond = true;
  }
  
  void finish() {
    for (int i = 0; i < atomSetCount; i++) {
      if (noAutoBond)
        setAtomSetAuxiliaryInfo("noAutoBond", Boolean.TRUE, i);
      setAtomSetAuxiliaryInfo("initialAtomCount", new Integer(atomSetAtomCounts[i]), i);
      setAtomSetAuxiliaryInfo("initialBondCount", new Integer(atomSetBondCounts[i]), i);
    }
    atoms = null;
    atomSetAtomCounts = new int[16];
    atomSetAuxiliaryInfo = new Hashtable[16];
    atomSetCollectionProperties = new Properties();
    atomSetCollectionAuxiliaryInfo = new Hashtable();
    atomSetCount = 0;
    atomSetNumbers = new int[16];
    atomSetNames = new String[16];
    atomSetProperties = new Properties[16];
    atomSymbolicMap = new Hashtable();
    bonds = null;
    cartesians = null;
    connectLast = null;
    currentAtomSetIndex = -1;
    latticeCells = null;
    notionalUnitCell = null;
    symmetry = null;
    structures = new Structure[16];
    structureCount = 0;
    trajectoryStep = null;
    trajectorySteps = null;
    vConnect = null;
    vd = null;
  }


  void freeze() {
    //Logger.debug("AtomSetCollection.freeze; atomCount = " + atomCount);
    if (isTrajectory)
      finalizeTrajectory(true);
    getAltLocLists();
    getInsertionLists();
  }

  public void discardPreviousAtoms() {
    for (int i = atomCount; --i >= 0; )
      atoms[i] = null;
    atomCount = 0;
    clearSymbolicMap();
    atomSetCount = 0;
    currentAtomSetIndex = -1;
    for (int i = atomSetNames.length; --i >= 0; ) {
      atomSetAtomCounts[i] = 0;
      atomSetNames[i] = null;
    }
  }

  public void removeAtomSet() {
    if (currentAtomSetIndex < 0)
      return;
    currentAtomSetIndex--;
    atomSetCount--;
  }
  
  Atom newCloneAtom(Atom atom) throws Exception {
    //Logger.debug("newCloneAtom()");
    Atom clone = atom.cloneAtom();
    addAtom(clone);
    return clone;
  }

  // FIX ME This should really also clone the other things pertaining
  // to an atomSet, like the bonds (which probably should be remade...)
  // but also the atomSetProperties and atomSetName...
  public void cloneFirstAtomSet() throws Exception {
    newAtomSet();
    for (int i = 0, firstCount = atomSetAtomCounts[0]; i < firstCount; ++i)
      newCloneAtom(atoms[i]);
  }

  public void cloneFirstAtomSetWithBonds(int nBonds) throws Exception {
    cloneFirstAtomSet();
    int firstCount = atomSetAtomCounts[0];
    for (int bondNum = 0; bondNum < nBonds; bondNum++) {
      Bond bond = bonds[bondCount - nBonds];
      addNewBond(bond.atomIndex1 + firstCount, bond.atomIndex2 + firstCount,
          bond.order);
    }

  }

  public void cloneLastAtomSet() throws Exception {
    //Logger.debug("cloneLastAtomSet");
    //Logger.debug("b4 atomCount=" + atomCount);
    //Logger.debug("atomSetCount=" + atomSetCount);
    //Logger.debug("atomSetAtomCount=" +
    //                       atomSetAtomCounts[currentAtomSetIndex]);
    int count = getLastAtomSetAtomCount();
    int atomIndex = getLastAtomSetAtomIndex();
    newAtomSet();
    for ( ; --count >= 0; ++atomIndex)
      newCloneAtom(atoms[atomIndex]);
    //Logger.debug("after atomCount=" + atomCount);
  }
  
  public int getFirstAtomSetAtomCount() {
    return atomSetAtomCounts[0];
  }

  public int getLastAtomSetAtomCount() {
    return atomSetAtomCounts[currentAtomSetIndex];
  }

  public int getLastAtomSetAtomIndex() {
    //Logger.debug("atomSetCount=" + atomSetCount);
    return atomCount - atomSetAtomCounts[currentAtomSetIndex];
  }

  public Atom addNewAtom() {
    Atom atom = new Atom();
    addAtom(atom);
    return atom;
  }

  public void addAtom(Atom atom) {
    if (atomCount == atoms.length)
      atoms = (Atom[])ArrayUtil.doubleLength(atoms);
    atom.atomIndex = atomCount;
    atoms[atomCount++] = atom;
    if (atomSetCount == 0)
      newAtomSet();
    atom.atomSetIndex = currentAtomSetIndex;
    atom.atomSite = atomSetAtomCounts[currentAtomSetIndex]++;
  }

  public void addAtomWithMappedName(Atom atom) {
    addAtom(atom);
    mapMostRecentAtomName();
  }

  public void addAtomWithMappedSerialNumber(Atom atom) {
    addAtom(atom);
    mapMostRecentAtomSerialNumber();
  }

  public Bond addNewBond(int atomIndex1, int atomIndex2) {
    return addNewBond(atomIndex1, atomIndex2, 1);
  }

  Bond addNewBond(String atomName1, String atomName2) {
    return addNewBond(atomName1, atomName2, 1);
  }

  public Bond addNewBond(int atomIndex1, int atomIndex2, int order) {
    if (atomIndex1 < 0 || atomIndex1 >= atomCount ||
        atomIndex2 < 0 || atomIndex2 >= atomCount)
      return null;
    Bond bond = new Bond(atomIndex1, atomIndex2, order);
    addBond(bond);
    return bond;
  }
  
  public Bond addNewBond(String atomName1, String atomName2, int order) {
    return addNewBond(getAtomNameIndex(atomName1),
                      getAtomNameIndex(atomName2),
                      order);
  }

  public Bond addNewBondWithMappedSerialNumbers(int atomSerial1, int atomSerial2,
                                         int order) {
    return addNewBond(getAtomSerialNumberIndex(atomSerial1),
                      getAtomSerialNumberIndex(atomSerial2),
                      order);
  }


  Vector vConnect;
  int connectNextAtomIndex = 0;
  int connectNextAtomSet = 0;
  int[] connectLast;
  
  public void addConnection(int[] is) {
    if (vConnect == null) {
      connectLast = null;
      vConnect = new Vector();
    }
    if (connectLast != null) {
      if (is[0] == connectLast[0] 
          && is[1] == connectLast[1] 
          && is[2] != JmolAdapter.ORDER_HBOND) {
        connectLast[2]++;
        return;
      }
    }
    vConnect.addElement(connectLast = is);
  }

  public void connectAll(int maxSerial) {
    if (vConnect == null)
      return;
    int firstAtom = connectNextAtomIndex;
    for (int i = connectNextAtomSet; i < atomSetCount; i++) {
      setAtomSetCollectionAuxiliaryInfo("someModelsHaveCONECT", Boolean.TRUE);
      setAtomSetAuxiliaryInfo("PDB_CONECT_firstAtom_count_max", new int[] {firstAtom, atomSetAtomCounts[i], maxSerial}, i);
      setAtomSetAuxiliaryInfo("PDB_CONECT_bonds", vConnect, i);
      firstAtom += atomSetAtomCounts[i];
    }
    vConnect = null;
    connectNextAtomSet = currentAtomSetIndex + 1;
    connectNextAtomIndex = firstAtom;
  }

  public void addBond(Bond bond) {
    if (trajectoryStepCount > 0)
      return;
    if (bond.atomIndex1 < 0 ||
        bond.atomIndex2 < 0 ||
        bond.order < 0 ||
        //do not allow bonds between models
        atoms[bond.atomIndex1].atomSetIndex != atoms[bond.atomIndex2].atomSetIndex) {
      if (Logger.debugging) {
        Logger.debug(
            ">>>>>>BAD BOND:" + bond.atomIndex1 + "-" +
            bond.atomIndex2 + " order=" + bond.order);
      }
      return;
    }
    if (bondCount == bonds.length)
      bonds = (Bond[])ArrayUtil.setLength(bonds, bondCount + 1024);
    bonds[bondCount++] = bond;
    atomSetBondCounts[currentAtomSetIndex]++;
  }

  public void addStructure(Structure structure) {
    if (structureCount == structures.length)
      structures = (Structure[])ArrayUtil.setLength(structures,
                                                      structureCount + 32);
    structure.modelIndex = currentAtomSetIndex;
    structures[structureCount++] = structure;
    if (structure.strandCount == 1) {
      int i = structureCount;
      for (i = structureCount; --i >= 0 
        && structures[i].modelIndex == currentAtomSetIndex
        && structures[i].structureID.equals(structure.structureID); ) {
      }
      int n = structureCount - i;
      for (; i < structureCount; i++) 
        structures[i].strandCount = n;
    }
  }

  void setAtomSetSpaceGroupName(String spaceGroupName) {
    setAtomSetAuxiliaryInfo("spaceGroup", spaceGroupName+"");
  }
    
  void setCoordinatesAreFractional(boolean coordinatesAreFractional) {
    this.coordinatesAreFractional = coordinatesAreFractional;
    setAtomSetAuxiliaryInfo(
        "coordinatesAreFractional",
        Boolean.valueOf(coordinatesAreFractional));
    if (coordinatesAreFractional)
      setGlobalBoolean(GLOBAL_FRACTCOORD);    
  }
  
  float symmetryRange;
  void setSymmetryRange(float factor) {
    symmetryRange = factor;
    setAtomSetCollectionAuxiliaryInfo("symmetryRange", new Float(factor));
  }
  
  void setLatticeCells(int[] latticeCells, boolean applySymmetryToBonds, 
                       boolean doPackUnitCell) {
    //set when unit cell is determined
    // x <= 555 and y >= 555 indicate a range of cells to load
    // AROUND the central cell 555 and that
    // we should normalize (z = 1) or pack unit cells (z = -1) or not (z = 0)
    // in addition (Jmol 11.7.36) z = -2 does a full 3x3x3 around the designated cells
    // but then only delivers the atoms that are within the designated cells. 
    // Normalization is the moving of the center of mass into the unit cell.
    
    this.latticeCells = latticeCells;
    isLatticeRange = (latticeCells[2] == 0 || latticeCells[2] == 1 
        || latticeCells[2] == -1) && (latticeCells[0] <= 555  && latticeCells[1] >= 555);
    doNormalize = (!isLatticeRange || latticeCells[2] == 1);
    this.doPackUnitCell = doPackUnitCell;
    this.applySymmetryToBonds = applySymmetryToBonds;
  }
  
  SymmetryInterface symmetry;
  private SymmetryInterface getSymmetry() {
    if (symmetry == null)
      symmetry = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
    return symmetry;
  }
  
  boolean setNotionalUnitCell(float[] info) {
    notionalUnitCell = new float[info.length];
    for (int i = 0; i < info.length; i++)
      notionalUnitCell[i] = info[i];
    setAtomSetAuxiliaryInfo("notionalUnitcell", notionalUnitCell);
    setGlobalBoolean(GLOBAL_latticeCells);
    getSymmetry().setUnitCell(notionalUnitCell);
    return true;
  }

  void setGlobalBoolean(int globalIndex) {
    setAtomSetCollectionAuxiliaryInfo(globalBooleans[globalIndex], Boolean.TRUE);
  }
  
  boolean addSpaceGroupOperation(String xyz) {
    getSymmetry().setSpaceGroup(doNormalize);
    return (symmetry.addSpaceGroupOperation(xyz) >= 0);
  }
  
  public void setLatticeParameter(int latt) {
    getSymmetry().setSpaceGroup(doNormalize);
    symmetry.setLattice(latt);
  }
  
  void applySymmetry() throws Exception {
     //parameters are counts of unit cells as [a b c]
     applySymmetry(latticeCells[0], latticeCells[1], Math.abs(latticeCells[2]));
   }

   void applySymmetry(SymmetryInterface symmetry) throws Exception {
     getSymmetry().setSpaceGroup(symmetry);
     //parameters are counts of unit cells as [a b c]
     applySymmetry(latticeCells[0], latticeCells[1], Math.abs(latticeCells[2]));
   }

   boolean doNormalize = true;
   boolean doPackUnitCell = false;
   boolean isLatticeRange = false;
   
   void applySymmetry(int maxX, int maxY, int maxZ) throws Exception {
    if (coordinatesAreFractional && getSymmetry().haveSpaceGroup())
      applyAllSymmetry(maxX, maxY, maxZ);
   }

   private float rminx, rminy, rminz, rmaxx, rmaxy, rmaxz;
   
   private void setSymmetryMinMax(Point3f c) {
     if (rminx > c.x)
       rminx = c.x;
     if (rminy > c.y)
       rminy = c.y;
     if (rminz > c.z)
       rminz = c.z;
     
     if (rmaxx < c.x)
       rmaxx = c.x;
     if (rmaxy < c.y)
       rmaxy = c.y;
     if (rmaxz < c.z)
       rmaxz = c.z;
   }
   
   private boolean isInSymmetryRange(Point3f c) {
     return (c.x >= rminx && c.y >= rminy && c.z >= rminz 
         && c.x <= rmaxx && c.y <= rmaxy && c.z <= rmaxz);
   }

   private final Point3f ptOffset = new Point3f();
   
   private int minX, maxX, minY, maxY, minZ, maxZ;
   
   private static boolean isWithinCell(Point3f pt, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
     float slop = 0.02f;
     return (pt.x > minX - slop && pt.x < maxX + slop 
         && pt.y > minY - slop && pt.y < maxY + slop 
         && pt.z > minZ - slop && pt.z < maxZ + slop);
   }

   private void applyAllSymmetry(int maxX, int maxY, int maxZ) throws Exception {
    int noSymmetryCount = getLastAtomSetAtomCount();
    int iAtomFirst = getLastAtomSetAtomIndex();
    for (int i = iAtomFirst; i < atomCount; i++) {
      atoms[i].ellipsoid = symmetry.getEllipsoid(atoms[i].anisoBorU);
    }
    bondCount0 = bondCount;

    symmetry.setFinalOperations(atoms, iAtomFirst, noSymmetryCount, doNormalize);
    int operationCount = symmetry.getSpaceGroupOperationCount();
    minX = 0;
    minY = 0;
    minZ = 0;
    if (isLatticeRange) {
      //alternative format for indicating a range of cells:
      //{111 666}
      //555 --> {0 0 0}
      minX = (maxX / 100) - 5;
      minY = (maxX % 100) / 10 - 5;
      minZ = (maxX % 10) - 5;
      //555 --> {1 1 1}
      maxX = (maxY / 100) - 4;
      maxZ = (maxY % 10) - 4;
      maxY = (maxY % 100) / 10 - 4;
    }
    if (doPackUnitCell) {
      minX--;
      maxX++;
      minY--;
      maxY++;
      minZ--;
      maxZ++;
    }
    this.maxX = maxX;
    this.maxY = maxY;
    this.maxZ = maxZ;
    int nCells = (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    int cartesianCount = (checkSpecial ? noSymmetryCount * operationCount
        * nCells : symmetryRange > 0 ? noSymmetryCount * operationCount // checking against {1 1 1} 
    : symmetryRange < 0 ? 1 // checking against symop=1555 set; just a box
        : 1 // not checking
    );
    cartesians = new Point3f[cartesianCount];
    for (int i = 0; i < noSymmetryCount; i++)
      atoms[i + iAtomFirst].bsSymmetry = new BitSet(operationCount
          * (nCells + 1));
    int pt = 0;
    int[] unitCells = new int[nCells];
    int iCell = 0;
    int cell555Count = 0;
    float absRange = Math.abs(symmetryRange);
    boolean checkSymmetryRange = (symmetryRange != 0);
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    if (checkSymmetryRange) {
      rminx = Float.MAX_VALUE;
      rminy = Float.MAX_VALUE;
      rminz = Float.MAX_VALUE;
      rmaxx = -Float.MAX_VALUE;
      rmaxy = -Float.MAX_VALUE;
      rmaxz = -Float.MAX_VALUE;
    }
    //always do the 555 cell first
    Matrix4f op = symmetry.getSpaceGroupOperation(0);
    if (doPackUnitCell)
      ptOffset.set(0, 0, 0);
    for (int tx = minX; tx < maxX; tx++)
      for (int ty = minY; ty < maxY; ty++)
        for (int tz = minZ; tz < maxZ; tz++) {
          unitCells[iCell++] = 555 + tx * 100 + ty * 10 + tz;
          if (tx != 0 || ty != 0 || tz != 0 || cartesians.length == 0)
            continue;
          for (pt = 0; pt < noSymmetryCount; pt++) {
            Atom atom = atoms[iAtomFirst + pt];
            Point3f c = new Point3f(atom);
            op.transform(c);
            symmetry.toCartesian(c);
            if (doPackUnitCell) {
              symmetry.toUnitCell(c, ptOffset);
              atom.set(c);
              symmetry.toFractional(atom);
            }
            atom.bsSymmetry.set(iCell * operationCount);
            atom.bsSymmetry.set(0);
            if (checkSymmetryRange)
              setSymmetryMinMax(c);
            if (pt < cartesianCount)
              cartesians[pt] = c;
          }
          if (checkRangeNoSymmetry) {
            rminx -= absRange;
            rminy -= absRange;
            rminz -= absRange;
            rmaxx += absRange;
            rmaxy += absRange;
            rmaxz += absRange;
          }
          cell555Count = pt = symmetryAddAtoms(iAtomFirst, noSymmetryCount, 0,
              0, 0, 0, pt, iCell * operationCount);
        }
    if (checkRange111) {
      rminx -= absRange;
      rminy -= absRange;
      rminz -= absRange;
      rmaxx += absRange;
      rmaxy += absRange;
      rmaxz += absRange;
    }
    iCell = 0;
    for (int tx = minX; tx < maxX; tx++)
      for (int ty = minY; ty < maxY; ty++)
        for (int tz = minZ; tz < maxZ; tz++) {
          iCell++;
          if (tx != 0 || ty != 0 || tz != 0)
            pt = symmetryAddAtoms(iAtomFirst, noSymmetryCount, tx, ty, tz,
                cell555Count, pt, iCell * operationCount);
        }
    if (operationCount > 0) {
      String[] symmetryList = new String[operationCount];
      for (int i = 0; i < operationCount; i++)
        symmetryList[i] = "" + symmetry.getSpaceGroupXyz(i, doNormalize);
      setAtomSetAuxiliaryInfo("symmetryOperations", symmetryList);
    }
    setAtomSetAuxiliaryInfo("presymmetryAtomIndex", new Integer(iAtomFirst));
    setAtomSetAuxiliaryInfo("presymmetryAtomCount",
        new Integer(noSymmetryCount));
    setAtomSetAuxiliaryInfo("symmetryCount", new Integer(operationCount));
    setAtomSetAuxiliaryInfo("latticeDesignation", symmetry
        .getLatticeDesignation());
    setAtomSetAuxiliaryInfo("unitCellRange", unitCells);
    symmetry.setSpaceGroup(null);
    notionalUnitCell = new float[6];
    coordinatesAreFractional = false;
    //turn off global fractional conversion -- this will be model by model
    setAtomSetAuxiliaryInfo("hasSymmetry", Boolean.TRUE);
    setGlobalBoolean(GLOBAL_SYMMETRY);
  }
  
  Point3f[] cartesians;
  int bondCount0;
  int bondIndex0;
  boolean applySymmetryToBonds = false;
  boolean checkSpecial = true;

  public void setCheckSpecial(boolean TF) {
    checkSpecial = TF;
  }
  
  private final Point3f ptTemp = new Point3f();
  private final Point3f ptTemp1 = new Point3f();
  private final Point3f ptTemp2 = new Point3f();
  
  private int symmetryAddAtoms(int iAtomFirst, int noSymmetryCount, int transX,
                               int transY, int transZ, int baseCount, int pt,
                               int iCellOpPt) throws Exception {
    boolean isBaseCell = (baseCount == 0);
    boolean addBonds = (bondCount0 > bondIndex0 && applySymmetryToBonds);
    int[] atomMap = (addBonds ? new int[noSymmetryCount] : null);
    if (doPackUnitCell)
      ptOffset.set(transX, transY, transZ);

    //symmetryRange < 0 : just check symop=1 set
    //symmetryRange > 0 : check against {1 1 1}

    // if we are not checking special atoms, then this is a PDB file
    // and we return all atoms within a cubical volume around the 
    // target set. The user can later use select within() to narrow that down
    // This saves immensely on time.

    float range2 = symmetryRange * symmetryRange;
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    boolean checkSymmetryMinMax = (isBaseCell && checkRange111);
    checkRange111 &= !isBaseCell;
    boolean checkSymmetryRange = (checkRangeNoSymmetry || checkRange111);
    boolean checkDistances = (checkSpecial || checkSymmetryRange);
    boolean addCartesian = (checkSpecial || checkSymmetryMinMax);
    if (checkRangeNoSymmetry)
      baseCount = noSymmetryCount;
    int nOperations = symmetry.getSpaceGroupOperationCount();
    int atomMax = iAtomFirst + noSymmetryCount;
    Point3f ptAtom = new Point3f();
    for (int iSym = 0; iSym < nOperations; iSym++) {
      if (isBaseCell && symmetry.getSpaceGroupXyz(iSym, true).equals("x,y,z"))
        continue;

      /* pt0 sets the range of points cross-checked. 
       * If we are checking special positions, then we have to check
       *   all previous atoms. 
       * If we are doing a symmetry range check relative to {1 1 1}, then
       *   we have to check only the base set. (checkRange111 true)
       * If we are doing a symmetry range check on symop=1555 (checkRangeNoSymmetry true), 
       *   then we don't check any atoms and just use the box.
       *    
       */

      int pt0 = (checkSpecial ? pt : checkRange111 ? baseCount : 0);
      for (int i = iAtomFirst; i < atomMax; i++) {
        symmetry.newSpaceGroupPoint(iSym, atoms[i], ptAtom, transX, transY, transZ);
        Atom special = null;
        Point3f cartesian = new Point3f(ptAtom);
        symmetry.toCartesian(cartesian);
        if (doPackUnitCell) {
          symmetry.toUnitCell(cartesian, ptOffset);
          ptAtom.set(cartesian);
          symmetry.toFractional(ptAtom);
          if (!isWithinCell(ptAtom, minX + 1, maxX - 1, minY + 1, maxY - 1, minZ + 1, maxZ - 1))
            continue;
        }
        if (checkSymmetryMinMax)
          setSymmetryMinMax(cartesian);
        if (checkDistances) {

          /* checkSpecial indicates that we are looking for atoms with (nearly) the
           * same cartesian position.  
           */
          float minDist2 = Float.MAX_VALUE;
          if (checkSymmetryRange && !isInSymmetryRange(cartesian))
            continue;
          for (int j = pt0; --j >= 0;) {
            float d2 = cartesian.distanceSquared(cartesians[j]);
            if (checkSpecial && d2 < 0.0001) {
              special = atoms[iAtomFirst + j];
              break;
            }
            if (checkRange111 && j < baseCount && d2 < minDist2)
              minDist2 = d2;
          }
          if (checkRange111 && minDist2 > range2)
            continue;
        }
        int atomSite = atoms[i].atomSite;
        if (special != null) {
          if (addBonds)
            atomMap[atomSite] = special.atomIndex;
          special.bsSymmetry.set(iCellOpPt + iSym);
          special.bsSymmetry.set(iSym);
        } else {
          if (addBonds)
            atomMap[atomSite] = atomCount;
          Atom atom1 = newCloneAtom(atoms[i]);
          atom1.set(ptAtom);
          atom1.atomSite = atomSite;
          atom1.bsSymmetry = new BitSet(1);
          atom1.bsSymmetry.set(iCellOpPt + iSym);
          atom1.bsSymmetry.set(iSym);
          if (addCartesian)
            cartesians[pt++] = cartesian;
          if (atoms[i].ellipsoid != null) {
            Object axes = atoms[i].ellipsoid[0];
            Object lengths = atoms[i].ellipsoid[1];
            if (axes != null) {
              // note -- PDB reader specifically turns off cartesians
              if (addCartesian) {
                ptTemp.set(cartesians[i - iAtomFirst]);
              } else {
                ptTemp.set(atoms[i]);
                symmetry.toCartesian(ptTemp);
              }
              axes = symmetry.rotateEllipsoid(iSym, ptTemp, (Vector3f[]) axes, ptTemp1, ptTemp2);
            }
            atom1.ellipsoid = new Object[] { axes, lengths };
          }
        }
      }
      if (addBonds) {
        // Clone bonds
        for (int bondNum = bondIndex0; bondNum < bondCount0; bondNum++) {
          Bond bond = bonds[bondNum];
          int iAtom1 = atomMap[atoms[bond.atomIndex1].atomSite];
          int iAtom2 = atomMap[atoms[bond.atomIndex2].atomSite];
          if (iAtom1 >= atomMax || iAtom2 >= atomMax)
            addNewBond(iAtom1, iAtom2, bond.order);
        }
      }
    }
    return pt;
  }
  
  public void applySymmetry(Vector biomts, boolean applySymmetryToBonds, String filter) {
    int len = biomts.size();
    this.applySymmetryToBonds = applySymmetryToBonds;
    bondCount0 = bondCount;
    boolean addBonds = (bondCount0 > bondIndex0 && applySymmetryToBonds);
    int[] atomMap = (addBonds ? new int[atomCount] : null);
    int iAtomFirst = getLastAtomSetAtomIndex();
    int atomMax = atomCount;
    for (int iAtom = iAtomFirst; iAtom < atomMax; iAtom++) {
      atoms[iAtom].bsSymmetry = new BitSet(1);
      atoms[iAtom].bsSymmetry.set(0);
    }
    for (int i = 1; i < len; i++) { //skip 1, it's the identity
      if (filter.indexOf("!#") >= 0) {
        if (filter.toUpperCase().indexOf("!#" + (i + 1) + ";") >= 0)
          continue;
      } else if (filter.indexOf("#") >= 0
          && filter.toUpperCase().indexOf("#" + (i + 1) + ";") < 0) {
        continue;
      }
      Matrix4f mat = new Matrix4f();
      mat.set((float[]) biomts.get(i));
      for (int iAtom = iAtomFirst; iAtom < atomMax; iAtom++) {
        try {
          int atomSite = atoms[i].atomSite;
          if (addBonds)
            atomMap[atomSite] = atomCount;
          Atom atom1 = newCloneAtom(atoms[iAtom]);
          atom1.atomSite = atomSite;
          mat.transform(atom1);
          atom1.bsSymmetry = new BitSet(1);
          atom1.bsSymmetry.set(i);
          if (addBonds) {
            // Clone bonds
            for (int bondNum = bondIndex0; bondNum < bondCount0; bondNum++) {
              Bond bond = bonds[bondNum];
              int iAtom1 = atomMap[atoms[bond.atomIndex1].atomSite];
              int iAtom2 = atomMap[atoms[bond.atomIndex2].atomSite];
              if (iAtom1 >= atomMax || iAtom2 >= atomMax)
                addNewBond(iAtom1, iAtom2, bond.order);
            }
          }
        } catch (Exception e) {
          errorMessage = "appendAtomCollection error: " + e;
        }
      }
      setAtomSetAuxiliaryInfo("presymmetryAtomIndex", new Integer(iAtomFirst));
      setAtomSetAuxiliaryInfo("presymmetryAtomCount",
          new Integer(atomMax - iAtomFirst));
      setAtomSetAuxiliaryInfo("biosymmetryCount", new Integer(len));
      symmetry = null;
      notionalUnitCell = new float[6];
      coordinatesAreFractional = false; 
      setAtomSetAuxiliaryInfo("hasSymmetry", Boolean.TRUE);
      setGlobalBoolean(GLOBAL_SYMMETRY);
    }
    //need to clone bonds

  }
  
  public void setCollectionName(String collectionName) {
    if (collectionName != null) {
      collectionName = collectionName.trim();
      if (collectionName.length() == 0)
        return;
        this.collectionName = collectionName;
    }
  }

  Hashtable atomSymbolicMap = new Hashtable();

  void mapMostRecentAtomName() {
    //from ?? 
    if (atomCount > 0) {
      int index = atomCount - 1;
      String atomName = atoms[index].atomName;
      if (atomName != null)
        atomSymbolicMap.put(atomName, new Integer(index));
    }
  }

  public void clearSymbolicMap() {
    atomSymbolicMap.clear();
    haveMappedSerials = false;
  }

  boolean haveMappedSerials;
  void mapMostRecentAtomSerialNumber() {
    // from ?? 
    if (atomCount == 0) 
      return;
      int index = atomCount - 1;
      int atomSerial = atoms[index].atomSerial;
      if (atomSerial != Integer.MIN_VALUE)
        atomSymbolicMap.put(new Integer(atomSerial), new Integer(index));
      haveMappedSerials = true;
  }

  public void createAtomSerialMap() {
    if (haveMappedSerials || currentAtomSetIndex < 0)
      return;
    for (int i = getLastAtomSetAtomCount(); i < atomCount; i++) {
      int atomSerial = atoms[i].atomSerial;
      if (atomSerial != Integer.MIN_VALUE)
        atomSymbolicMap.put(new Integer(atomSerial), new Integer(i));
    }
    haveMappedSerials = true;
  }

  void mapAtomName(String atomName, int atomIndex) {
    atomSymbolicMap.put(atomName, new Integer(atomIndex));
  }

  public int getAtomNameIndex(String atomName) {
    //for new Bond -- inconsistent with mmCIF altLoc
    int index = -1;
    Object value = atomSymbolicMap.get(atomName);
    if (value != null)
      index = ((Integer)value).intValue();
    return index;
  }

  public int getAtomSerialNumberIndex(int serialNumber) {
    int index = -1;
    Object value = atomSymbolicMap.get(new Integer(serialNumber));
    if (value != null)
      index = ((Integer)value).intValue();
    return index;
  }
  
  /**
   * Sets a property for the AtomSetCollection
   * @param key The poperty key.
   * @param value The property value.
   */
  public void setAtomSetCollectionProperty(String key, String value) {
    atomSetCollectionProperties.put(key, value);
  }
  
  String getAtomSetCollectionProperty(String key) {
    return (String) atomSetCollectionProperties.get(key);
  }
  
  public void setAtomSetCollectionAuxiliaryInfo(String key, Object value) {
    atomSetCollectionAuxiliaryInfo.put(key, value);
  }
  
  /**
   * Sets the partial atomic charges based on atomSetCollection auxiliary info
   *
   * @param auxKey The auxiliary key name that contains the charges
   * @return true if the data exist; false if not  
   */

  public boolean setAtomSetCollectionPartialCharges(String auxKey) {
    if (! atomSetCollectionAuxiliaryInfo.containsKey(auxKey))
      return false;
    Vector atomData = (Vector) atomSetCollectionAuxiliaryInfo.get(auxKey);
    for (int i = atomData.size(); --i >= 0;) 
      atoms[i].partialCharge = ((Float)atomData.get(i)).floatValue();
    return true;
  }
  
  public void mapPartialCharge(String atomName, float charge) {
    atoms[getAtomNameIndex(atomName)].partialCharge = charge;  
  }
  
  public Object getAtomSetCollectionAuxiliaryInfo(String key) {
    return atomSetCollectionAuxiliaryInfo.get(key);
  }
  
  ////////////////////////////////////////////////////////////////
  // atomSet stuff
  ////////////////////////////////////////////////////////////////
  
  private void addTrajectoryStep() {
    if (trajectoryStep.length == 0 || trajectoryStep.length < atomCount) {
      trajectoryStep = new Point3f[atomCount];      
    }
    for (int i = 0; i < atomCount; i++)
      trajectoryStep[i] = new Point3f(atoms[i]);
    trajectorySteps.addElement(trajectoryStep);
    trajectoryStepCount++;
  }
  
  void finalizeTrajectory(Vector trajectorySteps) {
    this.trajectorySteps = trajectorySteps;
    trajectoryStepCount = trajectorySteps.size();
    finalizeTrajectory(false);
  }

  private void finalizeTrajectory(boolean addStep) {
    if (trajectoryStepCount == 0)
      return;
    if (addStep) {
      if (trajectoryStep == null || trajectoryStep.length == 0)
        return;
      addTrajectoryStep();
    }
    //reset atom positions to original trajectory
    Point3f[] trajectory = (Point3f[])trajectorySteps.get(0);
    for (int i = 0; i < atomCount; i++)
      atoms[i].set(trajectory[i]);
    setAtomSetCollectionAuxiliaryInfo("trajectorySteps", trajectorySteps);
  }
 
  public void newAtomSet() {
    bondIndex0 = bondCount;
    if (isTrajectory) {
      if (trajectoryStep == null && atomCount > 0)
        trajectoryStep = new Point3f[0];
      if (trajectoryStep != null) { // not BEFORE first atom set
        addTrajectoryStep();
      }
      trajectoryStep = new Point3f[atomCount];
      discardPreviousAtoms();
    }
    currentAtomSetIndex = atomSetCount++;
    if (atomSetCount > atomSetNumbers.length) {
      atomSetNames = ArrayUtil.doubleLength(atomSetNames);
      atomSetAtomCounts = ArrayUtil.doubleLength(atomSetAtomCounts);
      atomSetBondCounts = ArrayUtil.doubleLength(atomSetBondCounts);
      atomSetProperties = (Properties[]) ArrayUtil
          .doubleLength(atomSetProperties);
      atomSetAuxiliaryInfo = (Hashtable[]) ArrayUtil
          .doubleLength(atomSetAuxiliaryInfo);
    }
    if (atomSetCount + trajectoryStepCount > atomSetNumbers.length) {
      atomSetNumbers = ArrayUtil.doubleLength(atomSetNumbers);
    }
    if (isTrajectory) {
      atomSetNumbers[currentAtomSetIndex + trajectoryStepCount] = atomSetCount + trajectoryStepCount;
    } else {
      atomSetNumbers[currentAtomSetIndex] = atomSetCount;
    }
    atomSymbolicMap.clear();
    setAtomSetAuxiliaryInfo("title", collectionName);    
  }

  /**
  * Sets the name for the current AtomSet
  *
  * @param atomSetName The name to be associated with the current AtomSet
  */
  public void setAtomSetName(String atomSetName) {
    setAtomSetName(atomSetName, currentAtomSetIndex);
  }
  
  /**
  * Sets the name for an AtomSet
  *
  * @param atomSetName The number to be associated with the AtomSet
  * @param atomSetIndex The index of the AtomSet that needs the association
  */
  public void setAtomSetName(String atomSetName, int atomSetIndex) {
    atomSetNames[atomSetIndex] = atomSetName;
  }
  
  /**
   * Sets the atom set names of the last n atomSets
   * @param atomSetName The name
   * @param n The number of last AtomSets that need these set
   */
  public void setAtomSetNames(String atomSetName, int n) {
    for (int idx = currentAtomSetIndex; --n >= 0; --idx)
      setAtomSetName( atomSetName, idx);
  }

  /**
  * Sets the number for the current AtomSet
  *
  * @param atomSetNumber The number for the current AtomSet.
  */
  public void setAtomSetNumber(int atomSetNumber) {
    if (isTrajectory)
      atomSetNumbers[currentAtomSetIndex + trajectoryStepCount] = atomSetNumber;
    else
      atomSetNumbers[currentAtomSetIndex] = atomSetNumber;
  }
  
  /**
  * Sets a property for the AtomSet
  *
  * @param key The key for the property
  * @param value The value to be associated with the key
  */
  public void setAtomSetProperty(String key, String value) {
    setAtomSetProperty(key, value, currentAtomSetIndex);
  }

  /**
  * Sets auxiliary information for the AtomSet
  *
  * @param key The key for the property
  * @param value The value to be associated with the key
  */
  public void setAtomSetAuxiliaryInfo(String key, Object value) {
    setAtomSetAuxiliaryInfo(key, value, currentAtomSetIndex);
  }

  /**
   * Sets the partial atomic charges based on atomSet auxiliary info
   *
   * @param auxKey The auxiliary key name that contains the charges
   * @return true if the data exist; false if not  
   */

  boolean setAtomSetPartialCharges(String auxKey) {
    if (!atomSetAuxiliaryInfo[currentAtomSetIndex].containsKey(auxKey))
      return false;
    Vector atomData = (Vector) getAtomSetAuxiliaryInfo(currentAtomSetIndex, auxKey);
    for (int i = atomData.size(); --i >= 0;)
      atoms[i].partialCharge = ((Float) atomData.get(i)).floatValue();
    return true;
  }
  
  Object getAtomSetAuxiliaryInfo(int index, String key) {
    return  atomSetAuxiliaryInfo[index].get(key);
  }
  
  /**
  * Sets the a property for the an AtomSet
  *
  * @param key The key for the property
  * @param value The value for the property
  * @param atomSetIndex The index of the AtomSet to get the property
  */
  public void setAtomSetProperty(String key, String value, int atomSetIndex) {
    // lazy instantiation of the Properties object
    if (atomSetProperties[atomSetIndex] == null)
      atomSetProperties[atomSetIndex] = new Properties();
    atomSetProperties[atomSetIndex].put(key, value);
  }

  /**
   * Sets auxiliary information for the an AtomSet
   *
   * @param key The key for the property
   * @param value The value for the property
   * @param atomSetIndex The index of the AtomSet to get the property
   */
  void setAtomSetAuxiliaryInfo(String key, Object value, int atomSetIndex) {
    if (atomSetIndex < 0)
      return;
    if (atomSetAuxiliaryInfo[atomSetIndex] == null)
      atomSetAuxiliaryInfo[atomSetIndex] = new Hashtable();
    //Logger.debug(atomSetIndex + " key="+ key + " value="+ value);
    if (value == null)
      return;
    atomSetAuxiliaryInfo[atomSetIndex].put(key, value);
  }

  /**
   * Sets the same properties for the last n atomSets.
   * @param key The key for the property
   * @param value The value of the property
   * @param n The number of last AtomSets that need these set
   */
  public void setAtomSetProperties(String key, String value, int n) {
    for (int idx=currentAtomSetIndex; --n >= 0; --idx) {
      setAtomSetProperty(key, value, idx);
    }    
  }
  

  /**
   * Clones the properties of the last atom set and associates it
   * with the current atom set. 
   */
  public void cloneLastAtomSetProperties() {
    cloneAtomSetProperties(currentAtomSetIndex-1);
  }

  /**
   * Clones the properties of an atom set and associated it with the
   * current atom set.
   * @param index The index of the atom set whose properties are to be cloned.
   */
  void cloneAtomSetProperties(int index) {
    atomSetProperties[currentAtomSetIndex] = 
      (Properties) atomSetProperties[index].clone();
  }
/*
  // currently not needed because we take the atomSetCount directly
  int getAtomSetCount() {
    return atomSetCount;
  }
*/

  int getAtomSetNumber(int atomSetIndex) {
    return atomSetNumbers[atomSetIndex >= atomSetCount ? 0 : atomSetIndex];
  }

  String getAtomSetName(int atomSetIndex) {
    if (atomSetIndex >= atomSetCount)
      atomSetIndex = atomSetCount - 1;
    return atomSetNames[atomSetIndex];
  }
  
  Properties getAtomSetProperties(int atomSetIndex) {
    if (atomSetIndex >= atomSetCount)
      atomSetIndex = atomSetCount - 1;
    return atomSetProperties[atomSetIndex];
  }

  Hashtable getAtomSetAuxiliaryInfo(int atomSetIndex) {
    if (atomSetIndex >= atomSetCount)
      atomSetIndex = atomSetCount - 1;
    return atomSetAuxiliaryInfo[atomSetIndex];
  }

  ////////////////////////////////////////////////////////////////
  // special support for alternate locations and insertion codes
  ////////////////////////////////////////////////////////////////

  boolean hasAlternateLocations() {
    for (int i = atomCount; --i >= 0; )
      if (atoms[i].alternateLocationID != '\0')
        return true;
    return false;
  }

  void getAltLocLists() {
    if (!hasAlternateLocations())
      return;
    String[] lists = new String[atomSetCount];
    for (int i = 0; i < atomSetCount; i++)
      lists[i] = "";
    for (int i = 0; i < atomCount; i++) {
      char id = atoms[i].alternateLocationID;
      if (id == '\0' || lists[atoms[i].atomSetIndex].indexOf(id) >= 0)
        continue;
      lists[atoms[i].atomSetIndex] += id;
    }
    for (int i = 0; i < atomSetCount; i++)
      if (lists[i].length() > 0)
        setAtomSetAuxiliaryInfo("altLocs", lists[i], i);
  }
  
  boolean hasInsertions() {
    for (int i = atomCount; --i >= 0; )
      if (atoms[i].insertionCode != '\0')
        return true;
    return false;
  }

  void getInsertionLists() {
    if (!hasInsertions())
      return;
    String[] lists = new String[atomSetCount];
    for (int i = 0; i < atomSetCount; i++)
      lists[i] = "";
    for (int i = 0; i < atomCount; i++) {
      char id = atoms[i].insertionCode;
      if (id == '\0' || lists[atoms[i].atomSetIndex].indexOf(id) >= 0)
        continue;
      lists[atoms[i].atomSetIndex] += id;
    }
    for (int i = 0; i < atomSetCount; i++)
      if (lists[i].length() > 0)
        setAtomSetAuxiliaryInfo("insertionCodes", lists[i], i);
  }

  //// for XmlChem3dReader, but could be for CUBE
  
  VolumeDataInterface vd;
  
  public void newVolumeData() {
    vd = (VolumeDataInterface) Interface.getOptionInterface("jvxl.data.VolumeData");
  }

  public void setVoxelCounts(int nPointsX, int nPointsY, int nPointsZ) {
    vd.setVoxelCounts(nPointsX, nPointsY, nPointsZ);
  }

  public void setVolumetricVector(int i, float x, float y, float z) {
    vd.setVolumetricVector(i, x, y, z);
  }

  public void setVolumetricOrigin(float x, float y, float z) {
    vd.setVolumetricOrigin(x, y, z);
  }

  public void setVoxelData(float[][][] voxelData) {
    vd.setVoxelData(voxelData);    
  }

  public Object getVolumeData() {
    VolumeDataInterface v = vd;
    vd = null; //delete adapter reference
    return v;
  }

  public Properties setAtomNames(Properties atomIdNames) {
    // for CML reader "a3" --> "N3"
    if (atomIdNames == null)
      return null;
    String s;
    for (int i = 0; i < atomCount; i++)
      if ((s = atomIdNames.getProperty(atoms[i].atomName)) != null)
        atoms[i].atomName = s;
    return null;
  }

}
