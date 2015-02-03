/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-10-22 14:12:46 -0500 (Sun, 22 Oct 2006) $
 * $Revision: 5999 $
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

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.SymmetryInterface;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.Quaternion;
import org.jmol.viewer.JmolConstants;

import java.io.BufferedReader;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/*
 * Notes 9/2006 Bob Hanson
 * 
 * all parsing functions now moved to org.jmol.util.Parser
 * 
 * to add symmetry capability to any reader, some or all of the following 
 * methods need to be there:
 * 
 *  setFractionalCoordinates()
 *  setSpaceGroupName()
 *  setUnitCell()
 *  setUnitCellItem()
 *  setAtomCoord()
 *  applySymmetryAndSetTrajectory()
 * 
 * At the very minimum, you need:
 * 
 *  setAtomCoord()
 *  applySymmetryAndSetTrajectory()
 * 
 * so that:
 *  (a) atom coordinates can be turned fractional by load parameters
 *  (b) symmetry can be applied once per model in the file
 *  
 *  If you know where the end of the atom+bond data are, then you can
 *  use applySymmetryAndSetTrajectory() once, just before exiting. Otherwise, use it
 *  twice -- it has a check to make sure it doesn't RUN twice -- once
 *  at the beginning and once at the end of the model.
 *  
 * htParams is used for passing information to the readers
 * and for returning information from the readers
 * 
 * It won't be null at this stage.
 * 
 * from Eval or Viewer:
 * 
 *  applySymmetryToBonds
 *  atomTypes (for Mol2Reader)
 *  bsModels
 *  filter
 *  firstLastStep
 *  firstLastSteps
 *  getHeader
 *  isTrajectory
 *  lattice
 *  manifest (for SmarterJmolAdapter)
 *  modelNumber
 *  spaceGroupIndex
 *  symmetryRange
 *  unitcell
 *  packed
 *  
 * from FileManager:
 * 
 *  fullPathName
 *  subFileList (for SmarterJmolAdapter)
 * 
 * from MdTopReader:
 *   
 *  isPeriodic
 *  templateAtomCount
 *  
 * from MdCrdReader:   
 * 
 *  trajectorySteps
 *  
 * from Resolver:
 * 
 *  filteredAtomCount
 *  ptFile
 *  readerName
 *  templateAtomCount
 *  
 *  
 * from AtomSetCollectionReader:
 *  
 *  bsFilter
 *  
 * 
 */

public abstract class AtomSetCollectionReader {

  public final static float ANGSTROMS_PER_BOHR = 0.5291772f; // used by SpartanArchive

  protected AtomSetCollection atomSetCollection;
  protected BufferedReader reader;
  protected String readerName;
  public Hashtable htParams;

  // buffer
  public String line, prevline; 
  protected int[] next = new int[1];
  private long ptLine;

  // protected/public state variables
  public int[] latticeCells;
  public boolean iHaveUnitCell;
  public boolean iHaveSymmetryOperators;
  protected boolean doApplySymmetry;
  protected boolean ignoreFileSymmetryOperators;
  protected boolean isTrajectory;
  protected boolean applySymmetryToBonds;
  protected boolean needToApplySymmetry;
  protected boolean getHeader;
  protected int templateAtomCount;
  protected int modelNumber;
  protected BitSet bsModels;
  protected BitSet bsFilter;
  protected String filter;
  protected boolean haveAtomFilter;
  protected String spaceGroup;
  protected boolean havePartialChargeFilter;
  public String calculationType = "?";

  // private state variables
  private boolean iHaveFractionalCoordinates;
  private boolean doPackUnitCell;
  private boolean doConvertToFractional;
  private boolean fileCoordinatesAreFractional;
  protected boolean ignoreFileUnitCell;
  private boolean ignoreFileSpaceGroupName;
  private float symmetryRange;  
  private float[] notionalUnitCell; //0-5 a b c alpha beta gamma; 6-21 matrix c->f
  private int[] firstLastStep;
  private int desiredModelNumber = Integer.MIN_VALUE;
  private int lastModelNumber = Integer.MAX_VALUE;
  private int desiredSpaceGroupIndex = -1;
  private SymmetryInterface symmetry;

/*  
  public void finalize() {
    System.out.println(this + " finalized");
  }
*/  
  
  public Object readData(String filename, Hashtable htParams, 
                         BufferedReader reader) throws Exception {
    initialize(htParams);
    readAtomSetCollection(reader);
    reader.close();
    return finalize(htParams, filename);
  }  
  
  protected Object readData(String filename, Hashtable htParams, Object DOMNode) {
    initialize(htParams);
    readAtomSetCollectionFromDOM(DOMNode);
    return finalize(htParams, filename);
  }

  public abstract void readAtomSetCollection(BufferedReader reader);

  public void readAtomSetCollectionFromDOM(Object DOMNode) {
  }

  private Object finalize(Hashtable htParams, String filename) {
    if (!htParams.containsKey("templateAtomCount"))
      htParams.put("templateAtomCount", new Integer(atomSetCollection
          .getAtomCount()));
    if (htParams.containsKey("bsFilter"))
      htParams.put("filteredAtomCount", new Integer(BitSetUtil
          .cardinalityOf((BitSet) htParams.get("bsFilter"))));
    if (!calculationType.equals("?"))
      atomSetCollection.setAtomSetCollectionAuxiliaryInfo("calculationType",
          calculationType);

    String fileType = atomSetCollection.fileTypeName;
    if (fileType.indexOf("(") >= 0)
      fileType = fileType.substring(0, fileType.indexOf("("));
    for (int i = atomSetCollection.getAtomSetCount(); --i >= 0;) {
      atomSetCollection.setAtomSetAuxiliaryInfo("fileName", filename, i);
      atomSetCollection.setAtomSetAuxiliaryInfo("fileType", fileType, i);
    }
    atomSetCollection.freeze();
    if (atomSetCollection.errorMessage != null)
      return atomSetCollection.errorMessage + "\nfor file " + filename
          + "\ntype " + atomSetCollection.fileTypeName;
    if (atomSetCollection.atomCount == 0)
      return "No atoms found\nfor file " + filename + "\ntype "
          + atomSetCollection.fileTypeName;
    return atomSetCollection;
  }

  protected void setError(Exception e) {
    e.printStackTrace();
    if (line == null)
      atomSetCollection.errorMessage = "Unexpected end of file after line "
          + --ptLine + ":\n" + prevline;
    else
      atomSetCollection.errorMessage = "Error reading file at line " + ptLine
          + ":\n" + line + "\n" + e.getMessage();
  }
  
  void initialize(Hashtable htParams) {

    initializeSymmetry();
    this.htParams = htParams;
    getHeader = htParams.containsKey("getHeader");
    readerName = (String) htParams.get("readerName");
    if (htParams.containsKey("modelNumber"))
      desiredModelNumber = ((Integer) htParams.get("modelNumber")).intValue();
    applySymmetryToBonds = htParams.containsKey("applySymmetryToBonds");
    filter = (String) htParams.get("filter");
    // filter is used for 
    // atoms: can contain [XXX] or ![XXX], .XXX; or !.XXX;, :X, !:X
    // GAMESS partial charge type: charge=LOW (MULLIKEN is default)
    // MO type: phrase or !phrase in MO line
    // bsFilter is usually null, but it gets set to indicate
    // which atoms were selected by the filter. This then
    // gets used by COORD files to load just those coordinates
    haveAtomFilter = (filter != null && (filter.indexOf(".") >= 0 
        || filter.indexOf("[") >= 0 || filter.indexOf(".") >= 0));
    havePartialChargeFilter = (filter != null 
        && filter.toLowerCase().indexOf("charge=") >= 0);
    bsFilter = (BitSet) htParams.get("bsFilter");
    if (bsFilter == null && filter != null) {
      bsFilter = new BitSet();
      htParams.put("bsFilter", bsFilter);
      filter = (";" + filter + ";").replace(',', ';');
      Logger.info("filtering with " + filter);
    }
    // ptFile < 0 indicates just one file being read
    // ptFile >= 0 indicates multiple files are being loaded
    // if the file is not the first read in the LOAD command, then
    // we look to see if it was loaded using LOAD ... "..." COORD ....
    int ptFile = (htParams.containsKey("ptFile") ? ((Integer) htParams
        .get("ptFile")).intValue() : -1);
    if (ptFile > 0 && htParams.containsKey("firstLastSteps")) {
      Object val = ((Vector) htParams.get("firstLastSteps"))
          .elementAt(ptFile - 1);
      if (val instanceof BitSet) {
        bsModels = (BitSet) val;
      } else {
        firstLastStep = (int[]) val;
      }
      templateAtomCount = ((Integer) htParams.get("templateAtomCount"))
          .intValue();
    } else if (htParams.containsKey("firstLastStep")) {
      isTrajectory = htParams.containsKey("isTrajectory");
      firstLastStep = (int[]) htParams.get("firstLastStep");
    } else if (htParams.containsKey("bsModels")) {
      isTrajectory = htParams.containsKey("isTrajectory");
      bsModels = (BitSet) htParams.get("bsModels");
    }
    if (bsModels != null || firstLastStep != null)
      desiredModelNumber = Integer.MIN_VALUE;
    if (bsModels == null && firstLastStep != null) {
      if (firstLastStep[0] < 0)
        firstLastStep[0] = 0;
      if (firstLastStep[2] == 0 || firstLastStep[1] < firstLastStep[0])
        firstLastStep[1] = -1;
      if (firstLastStep[2] < 1)
        firstLastStep[2] = 1;
      bsModels = new BitSet();
      bsModels.set(firstLastStep[0]);
      if (firstLastStep[1] > firstLastStep[0]) {
        for (int i = firstLastStep[0]; i <= firstLastStep[1]; i += firstLastStep[2])
          bsModels.set(i);
      }
    }
    if (bsModels != null && (firstLastStep == null || firstLastStep[1] != -1))
      lastModelNumber = BitSetUtil.length(bsModels);
    symmetryRange = (htParams.containsKey("symmetryRange") ? ((Float) htParams
        .get("symmetryRange")).floatValue() : 0);

    latticeCells = new int[3];
    if (htParams.containsKey("lattice")) {
      Point3f pt = ((Point3f) htParams.get("lattice"));
      latticeCells[0] = (int) pt.x;
      latticeCells[1] = (int) pt.y;
      latticeCells[2] = (int) pt.z;
      doPackUnitCell = (htParams.containsKey("packed") || latticeCells[2] < 0);
    }
    doApplySymmetry = (latticeCells[0] > 0 && latticeCells[1] > 0);
    //allows for {1 1 1} or {1 1 -1} or {555 555 0|1|-1} (-1  being "packed")
    if (!doApplySymmetry) {
      latticeCells[0] = 0;
      latticeCells[1] = 0;
      latticeCells[2] = 0;
    }

    //this flag FORCES symmetry -- generally if coordinates are not fractional,
    //we may note the unit cell, but we do not apply symmetry
    //with this flag, we convert any nonfractional coordinates to fractional
    //if a unit cell is available.

    if (htParams.containsKey("spaceGroupIndex")) {
      // three options include:
      // = -1: normal -- use operators if present or name if not
      // = -2: user is supplying operators or name
      // >=0: spacegroup fully determined
      // = -999: ignore -- just the operators

      desiredSpaceGroupIndex = ((Integer) htParams.get("spaceGroupIndex"))
          .intValue();
      if (desiredSpaceGroupIndex == -2)
        spaceGroup = (String) htParams.get("spaceGroupName");
      ignoreFileSpaceGroupName = (desiredSpaceGroupIndex == -2 || desiredSpaceGroupIndex >= 0);
      ignoreFileSymmetryOperators = (desiredSpaceGroupIndex != -1);
    }
    if (htParams.containsKey("unitcell")) {
      float[] fParams = (float[]) htParams.get("unitcell");
      setUnitCell(fParams[0], fParams[1], fParams[2], fParams[3], fParams[4],
          fParams[5]);
      ignoreFileUnitCell = iHaveUnitCell;
    }
  }

  private boolean haveModel = false;
  protected boolean doGetModel(int modelNumber) {
    // modelNumber is 1-based, but firstLastStep is 0-based
  boolean isOK = (bsModels == null ? desiredModelNumber == Integer.MIN_VALUE || modelNumber == desiredModelNumber
        : modelNumber > lastModelNumber ? false 
            : modelNumber > 0 && bsModels.get(modelNumber - 1)
            || haveModel && firstLastStep != null && firstLastStep[1] < 0
            && (firstLastStep[2] < 2 || (modelNumber - 1 - firstLastStep[0]) % firstLastStep[2] == 0));
  haveModel |= isOK;
  return isOK;
  }
  
  protected boolean isLastModel(int modelNumber) {
    return (desiredModelNumber != Integer.MIN_VALUE || modelNumber >= lastModelNumber);
  }

  private void initializeSymmetry() {
    iHaveUnitCell = ignoreFileUnitCell;
    if (!ignoreFileUnitCell) {
      notionalUnitCell = new float[22];
      //0-5 a b c alpha beta gamma; 6-21 m00 m01... m33 cartesian-->fractional
      for (int i = 22; --i >= 0;)
        notionalUnitCell[i] = Float.NaN;
      symmetry = null;
    }
    if (!ignoreFileSpaceGroupName)
      spaceGroup = "unspecified *";

    needToApplySymmetry = false;
  }

  protected void newAtomSet(String name) {
    if (atomSetCollection.currentAtomSetIndex >= 0) {
      atomSetCollection.newAtomSet();
      atomSetCollection.setCollectionName("<collection of "
          + (atomSetCollection.currentAtomSetIndex + 1) + " models>");
    } else {
      atomSetCollection.setCollectionName(name);
    }
    Logger.debug(name);
  }

  public void setSpaceGroupName(String name) {
    if (ignoreFileSpaceGroupName)
      return;
    spaceGroup = name.trim();
  }

  public void setSymmetryOperator(String xyz) {
    if (ignoreFileSymmetryOperators)
      return;
    atomSetCollection.setLatticeCells(latticeCells, applySymmetryToBonds, doPackUnitCell);
    if (!atomSetCollection.addSpaceGroupOperation(xyz))
      Logger.warn("Skipping symmetry operation " + xyz);
    iHaveSymmetryOperators = true;
  }

  private int nMatrixElements = 0;
  private void initializeCartesianToFractional() {
    for (int i = 0; i < 16; i++)
      if (!Float.isNaN(notionalUnitCell[6 + i]))
        return; //just do this once
    for (int i = 0; i < 16; i++)
      notionalUnitCell[6 + i] = ((i % 5 == 0 ? 1 : 0));
    nMatrixElements = 0;
  }

  public void clearLatticeParameters() {
    if (ignoreFileUnitCell)
      return;
    for (int i = 6; i < notionalUnitCell.length; i++)
      notionalUnitCell[i] = Float.NaN;
    checkUnitCell(6);    
  }
  
  public void setUnitCellItem(int i, float x) {
    if (ignoreFileUnitCell)
      return;
    if (!Float.isNaN(x) && i >= 6 && Float.isNaN(notionalUnitCell[6]))
      initializeCartesianToFractional();
    notionalUnitCell[i] = x;
    if (Logger.debugging) {
      Logger.debug("setunitcellitem " + i + " " + x);
    }
    //System.out.println("atomSetCollection unitcell item " + i + " = " + x);
    if (i < 6 || Float.isNaN(x))
      iHaveUnitCell = checkUnitCell(6);
    else if(++nMatrixElements == 12)
      checkUnitCell(22);
  }

  protected void setUnitCell(float a, float b, float c, float alpha, float beta,
                   float gamma) {
    if (ignoreFileUnitCell)
      return;
    notionalUnitCell[JmolConstants.INFO_A] = a;
    notionalUnitCell[JmolConstants.INFO_B] = b;
    notionalUnitCell[JmolConstants.INFO_C] = c;
    notionalUnitCell[JmolConstants.INFO_ALPHA] = alpha;
    notionalUnitCell[JmolConstants.INFO_BETA] = beta;
    notionalUnitCell[JmolConstants.INFO_GAMMA] = gamma;
    iHaveUnitCell = checkUnitCell(6);
  }
  
  public void addPrimitiveLatticeVector(int i, float[] xyz) {
    i = 6 + i * 3;
    notionalUnitCell[i++] = xyz[0];
    notionalUnitCell[i++] = xyz[1];
    notionalUnitCell[i++] = xyz[2];
    checkUnitCell(15);
  }

  private boolean checkUnitCell(int n) {
    for (int i = 0; i < n; i++)
      if (Float.isNaN(notionalUnitCell[i]))
        return false;
    newSymmetry().setUnitCell(notionalUnitCell);
    if (doApplySymmetry)
      doConvertToFractional = !fileCoordinatesAreFractional;
    //if (but not only if) applying symmetry do we force conversion
    return true;
  }

  private SymmetryInterface newSymmetry() {
    symmetry = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
    return symmetry;
  }

  public void setFractionalCoordinates(boolean TF) {
    iHaveFractionalCoordinates = fileCoordinatesAreFractional = TF;
  }

  protected boolean filterAtom(Atom atom) {
    //cif, pdb readers
    return (!haveAtomFilter || filterAtom(atom, atomSetCollection.atomCount));
  }

  /**
   * 
   * filter can contain [XXX] or ![XXX], .XXX; or !.XXX;, :X, !:X
   * @param atom
   * @param iAtom
   * @return        true if we want this atom
   */
  protected boolean filterAtom(Atom atom, int iAtom) {
    //mdtop, cif, pdb
    String code;
    boolean isOK = false;
    while (true) {
      if (atom.group3 != null) {
        code = "[" + atom.group3.toUpperCase() + "]";
        if (filter.indexOf("![") >= 0) {
          if (filter.toUpperCase().indexOf(code) >= 0)
            break;
        } else if (filter.indexOf("[") >= 0
            && filter.toUpperCase().indexOf(code) < 0) {
          break;
        }
      }
      if (atom.atomName != null) {
        code = "." + atom.atomName.toUpperCase() + ";";
        if (filter.indexOf("!.") >= 0) {
          if (filter.toUpperCase().indexOf(code) >= 0)
            break;
        } else if (filter.indexOf("*.") >= 0
            && filter.toUpperCase().indexOf(code) < 0) {
          break;
        }
      }
      if (filter.indexOf("!:") >= 0) {
        if (filter.indexOf(":" + atom.chainID) >= 0)
          break;
      } else if (filter.indexOf(":") >= 0
          && filter.indexOf(":" + atom.chainID) < 0) {
        break;
      }
      isOK = true;
      break;
    }
    bsFilter.set(iAtom, isOK);
    return isOK;
  }
  
  public void setAtomCoord(Atom atom, float x, float y, float z) {
    atom.set(x, y, z);
    setAtomCoord(atom);
  }

  public void setAtomCoord(Atom atom) {
    if (doConvertToFractional && !fileCoordinatesAreFractional
        && symmetry != null) {
      symmetry.toFractional(atom);
      iHaveFractionalCoordinates = true;
    }
    //if (Logger.debugging)
    //Logger.debug(" atom "+atom.atomName + " " + atom.x + " " + atom.y+" "+atom.z);
    needToApplySymmetry = true;
  }

  protected void addSites(Hashtable htSites) {
    atomSetCollection.setAtomSetAuxiliaryInfo("pdbSites", htSites);
    Enumeration e = htSites.keys();
    String sites = "";
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      Hashtable htSite = (Hashtable) htSites.get(name);
      char ch;
      for (int i = name.length(); --i >= 0; )
        if (!Character.isLetterOrDigit(ch = name.charAt(i)) && ch != '\'')
          name = name.substring(0, i) + "_" + name.substring(i + 1);
      String seqNum = (String) htSite.get("seqNum");
      String groups = (String) htSite.get("groups");
      if (groups.length() == 0)
        continue;
      addJmolScript("@site_" + name + " " + groups);
      addJmolScript("@" + seqNum + " " + groups);
      addJmolScript("site_" + name + " = \"" + groups + "\".split(\",\")");
      sites += (sites == "" ? "" : ",") + "site_" + name;
    }
    addJmolScript("site_list = \"" + sites + "\".split(\",\")");
  }

  public void applySymmetryAndSetTrajectory() throws Exception {
    if (isTrajectory)
      atomSetCollection.setTrajectory();
    if (!needToApplySymmetry || !iHaveUnitCell) {
      initializeSymmetry();
      return;
    }
    atomSetCollection.setCoordinatesAreFractional(iHaveFractionalCoordinates);
    atomSetCollection.setNotionalUnitCell(notionalUnitCell);
    atomSetCollection.setAtomSetSpaceGroupName(spaceGroup);
    atomSetCollection.setSymmetryRange(symmetryRange);
    if (doConvertToFractional || fileCoordinatesAreFractional) {
      atomSetCollection.setLatticeCells(latticeCells, applySymmetryToBonds, doPackUnitCell);
      if (ignoreFileSpaceGroupName || !iHaveSymmetryOperators) {
        SymmetryInterface symmetry = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
        if (symmetry.createSpaceGroup(desiredSpaceGroupIndex, (spaceGroup
            .indexOf("*") >= 0 ? "P1" : spaceGroup), notionalUnitCell,
            atomSetCollection.doNormalize)) {
          atomSetCollection
              .setAtomSetSpaceGroupName(symmetry.getSpaceGroupName());
          atomSetCollection.applySymmetry(symmetry);
        }
      } else {
        atomSetCollection.applySymmetry();
      }
    }
    initializeSymmetry();
  }

  public void setMOData(Hashtable moData) {
    atomSetCollection.setAtomSetAuxiliaryInfo("moData", moData);
    Vector orbitals = (Vector) moData.get("mos");
    if (orbitals != null)
      Logger.info(orbitals.size() + " molecular orbitals read in model " + modelNumber);
  }

  private Matrix3f matrixRotate;
  public void setTransform(float x1, float y1, float z1, float x2, float y2,
                              float z2, float x3, float y3, float z3) {
    if (matrixRotate != null)
      return;
    matrixRotate = new Matrix3f();
    Vector3f v = new Vector3f();
    // rows in Sygress/CAChe and Spartan become columns here
    v.set(x1, y1, z1);
    v.normalize();
    matrixRotate.setColumn(0, v);
    v.set(x2, y2, z2);
    v.normalize();
    matrixRotate.setColumn(1, v);
    v.set(x3, y3, z3);
    v.normalize();
    matrixRotate.setColumn(2, v);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("defaultOrientationMatrix",
        new Matrix3f(matrixRotate));
    // first two matrix column vectors define quaternion X and XY plane
    Quaternion q = new Quaternion(matrixRotate);
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("defaultOrientationQuaternion", q);
    Logger.info("defaultOrientationMatrix = " + matrixRotate);    
    
  }
 
  public static String getElementSymbol(int elementNumber) {
    return JmolAdapter.getElementSymbol(elementNumber);
  }
  
  protected static String deducePdbElementSymbol(boolean isHetero, String XX,
                                           String group3) {
    // short of having an entire table,
    int i = XX.indexOf('\0');
    String atomType = null;
    if (i >= 0) {
      atomType = XX.substring(i + 1);
      XX = XX.substring(0, i);
      if (atomType != null && atomType.length() == 1)
        return atomType;
    }
    if (XX.equalsIgnoreCase(group3))
      return XX; // Cd Mg etc.
    int len = XX.length();
    char ch1 = ' ';
    i = 0;
    while (i < len && (ch1 = XX.charAt(i++)) <= '9') {
      // find first nonnumeric letter
    }

    char ch2 = (i < len ? XX.charAt(i) : ' ');
    String full = group3 + "." + ch1 + ch2;
    // Cd Nd Ne are not in complex hetero; Ca is in these:
    if (("OEC.CA ICA.CA OC1.CA OC2.CA OC4.CA").indexOf(full) >= 0)
      return "Ca";
    if (XX.indexOf("'") > 0 || XX.indexOf("*") >= 0 || "HCNO".indexOf(ch1) >= 0
        && ch2 <= 'H' || XX.startsWith("CM"))
      return "" + ch1;
    if (isHetero && Atom.isValidElementSymbolNoCaseSecondChar(ch1, ch2))
      return ("" + ch1 + ch2).trim();
    if (Atom.isValidElementSymbol(ch1))
      return "" + ch1;
    if (Atom.isValidElementSymbol(ch2))
      return "" + ch2;
    return "Xx";
  }

  protected void fillDataBlock(String[][] data) throws Exception {
    int nLines = data.length;
    for (int i = 0; i < nLines; i++)
      data[i] = getTokens(discardLinesUntilNonBlank());
  }

  protected void discardLines(int nLines) throws Exception {
    for (int i = nLines; --i >= 0;)
      readLine();
  }

  protected String discardLinesUntilStartsWith(String startsWith) throws Exception {
    while (readLine() != null && !line.startsWith(startsWith)) {
    }
    return line;
  }

  protected String discardLinesUntilContains(String containsMatch) throws Exception {
    while (readLine() != null && line.indexOf(containsMatch) < 0) {
    }
    return line;
  }

  protected void discardLinesUntilBlank() throws Exception {
    while (readLine() != null && line.trim().length() != 0) {
    }
  }

  protected String discardLinesUntilNonBlank() throws Exception {
    while (readLine() != null && line.trim().length() == 0) {
    }
    return line;
  }

  protected void checkLineForScript(String line) {
    this.line = line;
    checkLineForScript();
  }
  
  public void checkLineForScript() {
    if (line.indexOf("Jmol PDB-encoded data") >= 0) 
       atomSetCollection.setAtomSetCollectionAuxiliaryInfo("jmolData", "" + line);
    if (line.endsWith("#noautobond")) {
      line = line.substring(0, line.lastIndexOf('#')).trim();
      atomSetCollection.setNoAutoBond();
    }
    int pt = line.indexOf("jmolscript:");
    if (pt >= 0) {
      String script = line.substring(pt + 11, line.length());
      if (script.indexOf("#") >= 0) {
        script = script.substring(0, script.indexOf("#"));
      }
      addJmolScript(script);
      line = line.substring(0, pt).trim();
    }
  }

  private String previousScript;  
  protected void addJmolScript(String script) {
    Logger.info("#jmolScript: " + script);
    if (previousScript == null)
      previousScript = "";
    else if (!previousScript.endsWith(";"))
      previousScript += ";";
    previousScript += script;
    atomSetCollection.setAtomSetCollectionAuxiliaryInfo("jmolscript", 
        previousScript);
  }

  public String readLine() throws Exception {
    prevline = line;
    line = reader.readLine();
    ptLine++;
    if (Logger.debugging)
      Logger.debug(line);
    //System.out.println("readLine " + ptLine + " " + line);
    return line;
  }

  protected String readLineTrimmed() throws Exception {
    readLine();
    if (line == null)
      line = "";
    return line = line.trim();
  }
  
  final static protected String[] getStrings(String sinfo, int nFields, int width) {
    String[] fields = new String[nFields];
    for (int i = 0, pt = 0; i < nFields; i++, pt += width)
      fields[i] = sinfo.substring(pt, pt + width);
    return fields;
  }

  // parser functions are static, so they need notstatic counterparts
  
  protected String[] getTokens() {
    return Parser.getTokens(line);  
  }
  
  protected static void getTokensFloat(String s, float[] f, int n) {
    Parser.parseFloatArray(getTokens(s), f, n);
  }
  
  public static String[] getTokens(String s) {
    return Parser.getTokens(s);  
  }
  
  protected static String[] getTokens(String s, int iStart) {
    return Parser.getTokens(s, iStart);  
  }
  
  protected float parseFloat() {
    return Parser.parseFloat(line, next);
  }

  public float parseFloat(String s) {
    next[0] = 0;
    return Parser.parseFloat(s, next);
  }

  protected float parseFloat(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return Parser.parseFloat(s, iEnd, next);
  }
  
  protected int parseInt() {
    return Parser.parseInt(line, next);
  }
  
  public int parseInt(String s) {
    next[0] = 0;
    return Parser.parseInt(s, next);
  }
  
  protected int parseInt(String s, int iStart) {
    next[0] = iStart;
    return Parser.parseInt(s, next);
  }
  
  protected int parseInt(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return Parser.parseInt(s, iEnd, next);
  }

  protected String parseToken() {
    return Parser.parseToken(line, next);
  }
  
  protected String parseToken(String s) {
    next[0] = 0;
    return Parser.parseToken(s, next);
  }
  
  protected String parseTokenNext(String s) {
    return Parser.parseToken(s, next);
  }

  protected String parseToken(String s, int iStart, int iEnd) {
    next[0] = iStart;
    return Parser.parseToken(s, iEnd, next);
  }
  
  protected static String parseTrimmed(String s, int iStart) {
    return Parser.parseTrimmed(s, iStart);
  }
  
  protected static String parseTrimmed(String s, int iStart, int iEnd) {
    return Parser.parseTrimmed(s, iStart, iEnd);
  }

}
