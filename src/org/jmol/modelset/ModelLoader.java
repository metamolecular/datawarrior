/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import org.jmol.shape.Shape;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;
import org.jmol.util.Quaternion;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;

import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolBioResolver;
import org.jmol.api.SymmetryInterface;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/* 
 * 
 * This subclass contains only the private methods 
 * used to load a model. Methods exclusively after 
 * file loading are included only in the superclass, ModelSet,
 * and its superclasses, ModelCollection, BondCollection, and AtomCollection.
 * 
 * Bob Hanson, 5/2007
 *  
 */

public final class ModelLoader extends ModelSet {

  //public void finalize() {
  //  System.out.println("ModelLoader " + this + " finalized");
  //}
  

  private ModelLoader mergeModelSet;
  private boolean merging;
  private String jmolData; // from a PDB remark "Jmol PDB-encoded data"

  private final int[] specialAtomIndexes = new int[JmolConstants.ATOMID_MAX];
  private String[] group3Lists;
  private int[][] group3Counts;
  
  public ModelLoader(Viewer viewer, String name) {
    if (shapes == null && !viewer.isDataOnly())
      shapes = new Shape[JmolConstants.SHAPE_MAX];    
    this.viewer = viewer;
    initializeInfo(name, 1, null, null);
    createModelSet(null, null);
    modelSetName = "zapped";
    viewer.setStringProperty("_fileType", "");
  }


  public ModelLoader(Viewer viewer, Object atomSetCollection, 
      ModelLoader mergeModelSet, String modelSetName) {
    
    if (shapes == null && !viewer.isDataOnly())
      shapes = new Shape[JmolConstants.SHAPE_MAX];
    
    JmolAdapter adapter = viewer.getModelAdapter();
    this.modelSetName = modelSetName;
    this.mergeModelSet = mergeModelSet;
    merging = (mergeModelSet != null && mergeModelSet.atomCount > 0);
    this.viewer = viewer;
    initializeInfo(adapter.getFileTypeName(atomSetCollection).toLowerCase().intern(),
        adapter.getEstimatedAtomCount(atomSetCollection), adapter
            .getAtomSetCollectionProperties(atomSetCollection), adapter
            .getAtomSetCollectionAuxiliaryInfo(atomSetCollection));
    createModelSet(adapter, atomSetCollection);
    // dumpAtomSetNameDiagnostics(adapter, atomSetCollection);
  }
/*
  private void dumpAtomSetNameDiagnostics(JmolAdapter adapter, Object atomSetCollection) {
    int frameModelCount = modelCount;
    int adapterAtomSetCount = adapter.getAtomSetCount(atomSetCollection);
    if (Logger.debugging) {
      Logger.debug(
          "----------------\n" + "debugging of AtomSetName stuff\n" +
          "\nframeModelCount=" + frameModelCount +
          "\nadapterAtomSetCount=" + adapterAtomSetCount + "\n -- \n");
      for (int i = 0; i < adapterAtomSetCount; ++i) {
        Logger.debug(
            "atomSetName[" + i + "]=" + adapter.getAtomSetName(atomSetCollection, i) +
            " atomSetNumber[" + i + "]=" + adapter.getAtomSetNumber(atomSetCollection, i));
      }
    }
  }
*/

  private boolean someModelsHaveUnitcells;
  private boolean isTrajectory;
  private String fileHeader;

  private void initializeInfo(String name, int nAtoms, Properties properties,
                       Hashtable info) {
    g3d = viewer.getGraphics3D();
    //long timeBegin = System.currentTimeMillis();
    modelSetTypeName = name;
    isXYZ = (modelSetTypeName == "xyz");
    setModelSetProperties(properties);
    setModelSetAuxiliaryInfo(info);
    //isMultiFile = getModelSetAuxiliaryInfoBoolean("isMultiFile"); -- no longer necessary
    isPDB = getModelSetAuxiliaryInfoBoolean("isPDB");
    jmolData = (String) getModelSetAuxiliaryInfo("jmolData");
    fileHeader = (String) getModelSetAuxiliaryInfo("fileHeader");
    trajectorySteps = (Vector) getModelSetAuxiliaryInfo("trajectorySteps");
    isTrajectory = (trajectorySteps != null);
    adapterTrajectoryCount = (trajectorySteps == null ? 0 : trajectorySteps.size()); 
    someModelsHaveSymmetry = getModelSetAuxiliaryInfoBoolean("someModelsHaveSymmetry");
    someModelsHaveUnitcells = getModelSetAuxiliaryInfoBoolean("someModelsHaveUnitcells");
    someModelsHaveFractionalCoordinates = getModelSetAuxiliaryInfoBoolean("someModelsHaveFractionalCoordinates");
    if (merging) {
      someModelsHaveSymmetry |= mergeModelSet.getModelSetAuxiliaryInfoBoolean("someModelsHaveSymmetry");
      someModelsHaveUnitcells |= mergeModelSet.getModelSetAuxiliaryInfoBoolean("someModelsHaveUnitcells");
      someModelsHaveFractionalCoordinates |= mergeModelSet.getModelSetAuxiliaryInfoBoolean("someModelsHaveFractionalCoordinates");
      someModelsHaveAromaticBonds |= mergeModelSet.someModelsHaveAromaticBonds;
    }
    initializeBuild(nAtoms);
  }

  private final static int ATOM_GROWTH_INCREMENT = 2000;
  private final Hashtable htAtomMap = new Hashtable();

  private void initializeBuild(int atomCountEstimate) {
    if (atomCountEstimate <= 0)
      atomCountEstimate = ATOM_GROWTH_INCREMENT;
    if (merging) {
      atoms = mergeModelSet.atoms;
      bonds = mergeModelSet.bonds;
    } else {
      atoms = new Atom[atomCountEstimate];
      bonds = new Bond[250 + atomCountEstimate]; // was "2 *" -- WAY overkill.
    }
    htAtomMap.clear();
    initializeGroupBuild();
  }

  private final static int defaultGroupCount = 32;
  private Chain[] chainOf;
  private String[] group3Of;
  private int[] seqcodes;
  private int[] firstAtomIndexes;

  private int currentModelIndex;
  private Model currentModel;
  private char currentChainID;
  private Chain currentChain;
  private int currentGroupSequenceNumber;
  private char currentGroupInsertionCode;
  private String currentGroup3;

  /**
   * also from calculateStructures
   * 
   */
  private void initializeGroupBuild() {
    chainOf = new Chain[defaultGroupCount];
    group3Of = new String[defaultGroupCount];
    seqcodes = new int[defaultGroupCount];
    firstAtomIndexes = new int[defaultGroupCount];
    currentChainID = '\uFFFF';
    currentChain = null;
    currentGroupInsertionCode = '\uFFFF';
    currentGroup3 = "xxxxx";
    currentModelIndex = -1;
    currentModel = null;
  }

  Group nullGroup; // used in Atom

  private int baseModelIndex = 0;
  private int baseModelCount = 0;
  private int baseAtomIndex = 0;
  private int baseTrajectoryCount = 0;
  private boolean appendNew;
  private int adapterModelCount = 0;
  private int adapterTrajectoryCount = 0;
  
  private void createModelSet(JmolAdapter adapter, Object atomSetCollection) {
    adapterModelCount = (adapter == null ? 1 : adapter
        .getAtomSetCount(atomSetCollection));
    //cannot append a trajectory into a previous model
    appendNew = (!merging || adapter == null || adapterModelCount > 1 
        || isTrajectory || viewer.getAppendNew());
    if (merging)
      mergeModelArrays();
    initializeAtomBondModelCounts();
    if (adapter == null) {
      setModelNameNumberProperties(0, -1, "", 1, null, null, false, null);
    } else {
      if (adapterModelCount > 0) {
        Logger.info("ModelSet: haveSymmetry:" + someModelsHaveSymmetry
            + " haveUnitcells:" + someModelsHaveUnitcells
            + " haveFractionalCoord:" + someModelsHaveFractionalCoordinates);
        Logger
            .info(adapterModelCount
                + " model"
                + (modelCount == 1 ? "" : "s")
                + " in this collection. Use getProperty \"modelInfo\" or"
                + " getProperty \"auxiliaryInfo\" to inspect them.");
      }
      Quaternion q = (Quaternion) getModelSetAuxiliaryInfo("defaultOrientationQuaternion");
      if (q != null) {
        Logger.info("defaultOrientationQuaternion = " + q);
        Logger.info("Use \"set autoLoadOrientation TRUE\" before loading or \"restore orientation DEFAULT\" after loading to view this orientation.");
      }
      iterateOverAllNewModels(adapter, atomSetCollection);
      iterateOverAllNewAtoms(adapter, atomSetCollection);
      iterateOverAllNewBonds(adapter, atomSetCollection);
      iterateOverAllNewStructures(adapter, atomSetCollection);
      if (adapter != null)
        adapter.finish(atomSetCollection);
      initializeUnitCellAndSymmetry();
      initializeBonding();
    }

    finalizeGroupBuild(); // set group offsets and build monomers
    calculatePolymers(null);
    //only now can we access all of the atom's properties

    freeze();
    calcBoundBoxDimensions(null);

    finalizeShapes();
    if (mergeModelSet != null)
      mergeModelSet.releaseModelSet();    
    mergeModelSet = null;
  }

  protected void releaseModelSet() {
    group3Lists = null;
    group3Counts = null;
    groups = null;
    super.releaseModelSet();
  }

  private void mergeModelArrays() {
    baseModelCount = mergeModelSet.modelCount;
    baseTrajectoryCount = mergeModelSet.getTrajectoryCount();
    if (baseTrajectoryCount > 0) {
      if (isTrajectory) {
        for (int i = 0; i < trajectorySteps.size(); i++)
          mergeModelSet.trajectorySteps.addElement(trajectorySteps.elementAt(i));
      }
      trajectorySteps = mergeModelSet.trajectorySteps;
    }
    modelFileNumbers = mergeModelSet.modelFileNumbers;  // file * 1000000 + modelInFile (1-based)
    modelNumbersForAtomLabel = mergeModelSet.modelNumbersForAtomLabel;
    modelNames = mergeModelSet.modelNames;
    modelNumbers = mergeModelSet.modelNumbers;
    frameTitles = mergeModelSet.frameTitles;
  }
  
  private void initializeAtomBondModelCounts() {
    atomCount = 0;
    bondCount = 0;
    int trajectoryCount = adapterTrajectoryCount;
    if (merging) {
      if (appendNew) {
        baseModelIndex = baseModelCount;
        modelCount = baseModelCount + adapterModelCount;
      } else {
        baseModelIndex = viewer.getCurrentModelIndex();
        if (baseModelIndex < 0)
          baseModelIndex = baseModelCount - 1;
        modelCount = baseModelCount;
      }
      atomCount = baseAtomIndex = mergeModelSet.atomCount;
      bondCount = mergeModelSet.bondCount;
      groupCount = baseGroupIndex = mergeModelSet.groupCount;
    } else {
      modelCount = adapterModelCount;
    }
    if (trajectoryCount > 1)
      modelCount += trajectoryCount - 1;
    models = (Model[]) ArrayUtil.setLength(models, modelCount);
    modelFileNumbers =(int[])ArrayUtil.setLength(modelFileNumbers, modelCount);
    modelNumbers =(int[])ArrayUtil.setLength(modelNumbers, modelCount);
    modelNumbersForAtomLabel = (String[])ArrayUtil.setLength(modelNumbersForAtomLabel, modelCount);
    modelNames = (String[])ArrayUtil.setLength(modelNames, modelCount);
    frameTitles = (String[])ArrayUtil.setLength(frameTitles, modelCount);

  }

  private void initializeMerge() {
    merge(mergeModelSet);
    bsSymmetry = mergeModelSet.bsSymmetry;
    Hashtable info = mergeModelSet.getAuxiliaryInfo(null);
    String[] mergeGroup3Lists = (String[]) info.get("group3Lists");
    int[][] mergeGroup3Counts = (int[][]) info.get("group3Counts");
    if (mergeGroup3Lists != null) {
      for (int i = 0; i < baseModelCount; i++) {
        group3Lists[i + 1] = mergeGroup3Lists[i + 1];
        group3Counts[i + 1] = mergeGroup3Counts[i + 1];
        structuresDefinedInFile.set(i);
      }
      group3Lists[0] = mergeGroup3Lists[0];
      group3Counts[0] = mergeGroup3Counts[0];
    }
    //if merging PDB data into an already-present model, and the 
    //structure is defined, consider the current structures in that 
    //model to be undefined. Not guarantee to work.
    if (!appendNew && isPDB) 
      structuresDefinedInFile.clear(baseModelIndex);
    surfaceDistance100s = null;
  }

  private void iterateOverAllNewModels(JmolAdapter adapter, Object atomSetCollection) {

    if (modelCount > 0) {
      nullGroup = new Group(new Chain(this, getModel(baseModelIndex), ' '), "",
          0, -1, -1);
    }

    group3Lists = new String[modelCount + 1];
    group3Counts = new int[modelCount + 1][];

    structuresDefinedInFile = new BitSet();

    if (merging)
      initializeMerge();

    int iTrajectory = (isTrajectory ? baseTrajectoryCount : -1);
    int ipt = baseModelIndex;
    for (int i = 0; i < adapterModelCount; ++i, ++ipt) {
      int modelNumber = (appendNew ? adapter.getAtomSetNumber(atomSetCollection, i)
          : Integer.MAX_VALUE);
      String modelName = adapter.getAtomSetName(atomSetCollection, i);
      Properties modelProperties = adapter.getAtomSetProperties(atomSetCollection, i);
      Hashtable modelAuxiliaryInfo = adapter.getAtomSetAuxiliaryInfo(
          atomSetCollection, i);
      viewer.setStringProperty("_fileType", (String) modelAuxiliaryInfo
          .get("fileType"));
      if (modelName == null)
        modelName = (jmolData != null ? jmolData.substring(jmolData
            .indexOf(":") + 2, jmolData.indexOf(";"))
            : modelNumber == Integer.MAX_VALUE ? "" : ""
                + (modelNumber % 1000000));
      boolean isPDBModel = setModelNameNumberProperties(ipt, iTrajectory,
          modelName, modelNumber, modelProperties, modelAuxiliaryInfo, isPDB,
          jmolData);
      if (isPDBModel) {
        group3Lists[ipt + 1] = JmolConstants.group3List;
        group3Counts[ipt + 1] = new int[JmolConstants.group3Count + 10];
        if (group3Lists[0] == null) {
          group3Lists[0] = JmolConstants.group3List;
          group3Counts[0] = new int[JmolConstants.group3Count + 10];
        }
      }
      if (getModelAuxiliaryInfo(ipt, "periodicOriginXyz") != null)
        someModelsHaveSymmetry = true;
    }
    if (isTrajectory) {
      // fill in the rest of the data
      Logger.info((modelCount - ipt + 1) + " trajectory steps read");
      for (int ia = adapterModelCount, i = ipt; i < modelCount; i++) {
        models[i] = models[baseModelCount];
        modelNumbers[i] = adapter.getAtomSetNumber(atomSetCollection, ia++);
        structuresDefinedInFile.set(i);
      }
    }
    finalizeModels(baseModelCount);
  }
    
  private boolean setModelNameNumberProperties(int modelIndex, int trajectoryBaseIndex,
                                       String modelName, int modelNumber,
                                       Properties modelProperties,
                                       Hashtable modelAuxiliaryInfo,
                                       boolean isPDB, String jmolData) {
    if (modelNumber != Integer.MAX_VALUE) {
      models[modelIndex] = new Model((ModelSet) this, modelIndex, trajectoryBaseIndex, jmolData,
          modelProperties, modelAuxiliaryInfo);
      modelNumbers[modelIndex] = modelNumber;
      modelNames[modelIndex] = modelName;
    }
    String codes = (String) getModelAuxiliaryInfo(modelIndex, "altLocs");
    models[modelIndex].setNAltLocs(codes == null ? 0 : codes.length());
    codes = (String) getModelAuxiliaryInfo(modelIndex, "insertionCodes");
    models[modelIndex].setNInsertions(codes == null ? 0 : codes.length());
    return models[modelIndex].isPDB = getModelAuxiliaryInfoBoolean(modelIndex,
        "isPDB");
  }

  /**
   * Model numbers are considerably more complicated in Jmol 11.
   * 
   * int modelNumber
   *  
   *   The adapter gives us a modelNumber, but that is not necessarily
   *   what the user accesses. If a single files is loaded this is:
   *   
   *   a) single file context:
   *   
   *     1) the sequential number of the model in the file , or
   *     2) if a PDB file and "MODEL" record is present, that model number
   *     
   *   b) multifile context:
   *   
   *     always 1000000 * (fileIndex + 1) + (modelIndexInFile + 1)
   *   
   *   
   * int fileIndex
   * 
   *   The 0-based reference to the file containing this model. Used
   *   when doing   "select model=3.2" in a multifile context
   *   
   * int modelFileNumber
   * 
   *   An integer coding both the file and the model:
   *   
   *     file * 1000000 + modelInFile (1-based)
   *     
   *   Used all over the place. Note that if there is only one file,
   *   then modelFileNumber < 1000000.
   * 
   * String modelNumberDotted
   *   
   *   A number the user can use "1.3"
   *   
   * String modelNumberForAtomLabel
   * 
   *   Either the dotted number or the PDB MODEL number, if there is only one file
   *   
   * @param baseModelCount
   *    
   */
  private void finalizeModels(int baseModelCount) {
    if (modelCount == baseModelCount)
      return;
    String sNum;
    int modelnumber = 0;
    int lastfilenumber = -1;
    if (isTrajectory)
      for (int i = baseModelCount; ++i < modelCount;)
        modelNumbers[i] = modelNumbers[i - 1] + 1;
    if (baseModelCount > 0) {
      // load append
      if (modelNumbers[0] < 1000000) {
        // initially we had just one file
        for (int i = 0; i < baseModelCount; i++) {
          // create 1000000 model numbers for the original file models
          if (modelNames[i].length() == 0)
            modelNames[i] = "" + modelNumbers[i];
          modelNumbers[i] += 1000000;
          modelNumbersForAtomLabel[i] = "1." + (i + 1);
        }
      }
      // update file number
      int filenumber = modelNumbers[baseModelCount - 1];
      filenumber -= filenumber % 1000000;
      if (modelNumbers[baseModelCount] < 1000000)
        filenumber += 1000000;
      for (int i = baseModelCount; i < modelCount; i++)
        modelNumbers[i] += filenumber;
    }
    for (int i = baseModelCount; i < modelCount; ++i) {
      if (fileHeader != null)
        setModelAuxiliaryInfo(i, "fileHeader", fileHeader);
      int filenumber = modelNumbers[i] / 1000000;
      if (filenumber != lastfilenumber) {
        modelnumber = 0;
        lastfilenumber = filenumber;
      }
      modelnumber++;
      if (filenumber == 0) {
        // only one file -- take the PDB number or sequential number as given by adapter
        sNum = "" + getModelNumber(i);
        filenumber = 1;
      } else {
        //        //if only one file, just return the integer file number
        //      if (modelnumber == 1
        //        && (i + 1 == modelCount || models[i + 1].modelNumber / 1000000 != filenumber))
        //    sNum = filenumber + "";
        // else
        sNum = filenumber + "." + modelnumber;
      }
      modelNumbersForAtomLabel[i] = sNum;
      models[i].fileIndex = filenumber - 1;
      modelFileNumbers[i] = filenumber * 1000000 + modelnumber;
      if (modelNames[i] == null || modelNames[i].length() == 0)
        modelNames[i] = sNum;
   }
    
    if (merging)
      for (int i = 0; i < baseModelCount; i++)
        models[i].modelSet = this;
    
    // this won't do in the case of trajectories
    for (int i = 0; i < modelCount; i++) {
      setModelAuxiliaryInfo(i, "modelName", modelNames[i]);
      setModelAuxiliaryInfo(i, "modelNumber", new Integer(modelNumbers[i] % 1000000));
      setModelAuxiliaryInfo(i, "modelFileNumber", new Integer(modelFileNumbers[i]));
      setModelAuxiliaryInfo(i, "modelNumberDotted", getModelNumberDotted(i));
    }
  }

  private void iterateOverAllNewAtoms(JmolAdapter adapter, Object atomSetCollection) {
    // atom is created, but not all methods are safe, because it
    // has no group -- this is only an issue for debugging

    int size = viewer.getDefaultMadAtom();
    for (JmolAdapter.AtomIterator iterAtom = adapter
        .getAtomIterator(atomSetCollection); iterAtom.hasNext();) {
      short elementNumber = (short) iterAtom.getElementNumber();
      if (elementNumber <= 0)
        elementNumber = JmolConstants.elementNumberFromSymbol(iterAtom
            .getElementSymbol());
      char alternateLocation = iterAtom.getAlternateLocationID();
      addAtom(iterAtom.getAtomSetIndex() + baseModelIndex, iterAtom.getAtomSymmetry(), iterAtom.getAtomSite(),
          iterAtom.getUniqueID(), elementNumber, iterAtom.getAtomName(), size,
          iterAtom.getFormalCharge(), iterAtom.getPartialCharge(),
          iterAtom.getEllipsoid(), 
          iterAtom.getOccupancy(), iterAtom.getBfactor(), iterAtom.getX(),
          iterAtom.getY(), iterAtom.getZ(), iterAtom.getIsHetero(), iterAtom
              .getAtomSerial(), iterAtom.getChainID(), iterAtom.getGroup3(),
          iterAtom.getSequenceNumber(), iterAtom.getInsertionCode(), iterAtom
              .getVectorX(), iterAtom.getVectorY(), iterAtom.getVectorZ(),
          alternateLocation, iterAtom.getClientAtomReference(), iterAtom.getRadius());
    }
    
    int iLast = -1;
    for (int i = 0; i < atomCount; i++)
      if (atoms[i].modelIndex != iLast) {
        iLast = atoms[i].modelIndex;
        models[iLast].firstAtomIndex = i;
        models[iLast].bsAtoms = null;
      }
  }

  private void addAtom(int modelIndex, BitSet atomSymmetry, int atomSite,
                       Object atomUid, short atomicAndIsotopeNumber,
                       String atomName, int size, int formalCharge,
                       float partialCharge, Object[] ellipsoid,
                       int occupancy, float bfactor,
                       float x, float y, float z, boolean isHetero,
                       int atomSerial, char chainID, String group3,
                       int groupSequenceNumber, char groupInsertionCode,
                       float vectorX, float vectorY, float vectorZ,
                       char alternateLocationID, Object clientAtomReference,
                       float radius) {
    checkNewGroup(atomCount, modelIndex, chainID, group3, groupSequenceNumber,
        groupInsertionCode);
    if (atomCount == atoms.length)
      growAtomArrays(ATOM_GROWTH_INCREMENT);
    Atom atom = new Atom(viewer, currentModelIndex, atomCount, atomSymmetry,
        atomSite, atomicAndIsotopeNumber, size, formalCharge, x, y, z, isHetero,
        chainID, alternateLocationID, radius);
    atoms[atomCount] = atom;
    setBFactor(atomCount, bfactor);
    setOccupancy(atomCount, occupancy);
    setPartialCharge(atomCount, partialCharge);
    if (ellipsoid != null)
      setEllipsoid(atomCount, ellipsoid);
    atom.group = nullGroup;
    atom.colixAtom = viewer.getColixAtomPalette(atom, JmolConstants.PALETTE_CPK);
    if (atomName != null) {
      if (atomNames == null)
        atomNames = new String[atoms.length];
      int i;
      if ((i = atomName.indexOf('\0')) >= 0) {
        if (atomTypes == null)
          atomTypes = new String[atoms.length];
        atomTypes[atomCount] = atomName.substring(i + 1);
        atomName = atomName.substring(0, i);
      }
      atomNames[atomCount] = atomName.intern();
      byte specialAtomID = lookupSpecialAtomID(atomName);
      if (specialAtomID == JmolConstants.ATOMID_ALPHA_CARBON && group3 != null
          && group3.equalsIgnoreCase("CA"))
        specialAtomID = 0;
      if (specialAtomID != 0) {
        if (specialAtomIDs == null)
          specialAtomIDs = new byte[atoms.length];
        specialAtomIDs[atomCount] = specialAtomID;
      }
    }    
    if (atomSerial != Integer.MIN_VALUE) {
      if (atomSerials == null)
        atomSerials = new int[atoms.length];
      atomSerials[atomCount] = atomSerial;
    }
    if (clientAtomReference != null) {
      if (clientAtomReferences == null)
        clientAtomReferences = new Object[atoms.length];
      clientAtomReferences[atomCount] = clientAtomReference;
    }
    if (!Float.isNaN(vectorX))
      setVibrationVector(atomCount, vectorX, vectorY, vectorZ);
    htAtomMap.put(atomUid, atom);
    atomCount++;
  }

  private static Hashtable htAtom = new Hashtable();
  static {
    for (int i = JmolConstants.specialAtomNames.length; --i >= 0; ) {
      String specialAtomName = JmolConstants.specialAtomNames[i];
      if (specialAtomName != null) {
        Integer boxedI = new Integer(i);
        htAtom.put(specialAtomName, boxedI);
      }
    }
  }

  private static byte lookupSpecialAtomID(String atomName) {
    if (atomName != null) {
      if (atomName.indexOf('*') >= 0)
        atomName = atomName.replace('*', '\'');
      Integer boxedAtomID = (Integer)htAtom.get(atomName);
      if (boxedAtomID != null)
        return (byte)(boxedAtomID.intValue());
    }
    return 0;
  }

  private void checkNewGroup(int atomIndex, int modelIndex, char chainID,
                             String group3, int groupSequenceNumber,
                             char groupInsertionCode) {
    String group3i = (group3 == null ? null : group3.intern());
    if (modelIndex != currentModelIndex) {
      currentModel = getModel(modelIndex);
      currentModelIndex = modelIndex;
      currentChainID = '\uFFFF';
    }
    if (chainID != currentChainID) {
      currentChainID = chainID;
      currentChain = getOrAllocateChain(currentModel, chainID);
      currentGroupInsertionCode = '\uFFFF';
      currentGroupSequenceNumber = -1;
      currentGroup3 = "xxxx";
    }
    if (groupSequenceNumber != currentGroupSequenceNumber
        || groupInsertionCode != currentGroupInsertionCode
        || group3i != currentGroup3) {
      currentGroupSequenceNumber = groupSequenceNumber;
      currentGroupInsertionCode = groupInsertionCode;
      currentGroup3 = group3i;
      while (groupCount >= group3Of.length) {
        chainOf = (Chain[]) ArrayUtil.doubleLength(chainOf);
        group3Of = ArrayUtil.doubleLength(group3Of);
        seqcodes = ArrayUtil.doubleLength(seqcodes);
        firstAtomIndexes = ArrayUtil.doubleLength(firstAtomIndexes);
      }
      firstAtomIndexes[groupCount] = atomIndex;
      chainOf[groupCount] = currentChain;
      group3Of[groupCount] = group3;
      seqcodes[groupCount] = Group.getSeqcode(groupSequenceNumber,
          groupInsertionCode);
      ++groupCount;
    }
  }

  private Chain getOrAllocateChain(Model model, char chainID) {
    //Logger.debug("chainID=" + chainID + " -> " + (chainID + 0));
    Chain chain = model.getChain(chainID);
    if (chain != null)
      return chain;
    if (model.chainCount == model.chains.length)
      model.chains = (Chain[])ArrayUtil.doubleLength(model.chains);
    return model.chains[model.chainCount++] = new Chain(this, model, chainID);
  }

  private void growAtomArrays(int byHowMuch) {
    int newLength = atomCount + byHowMuch;
    atoms = (Atom[]) ArrayUtil.setLength(atoms, newLength);
    if (clientAtomReferences != null)
      clientAtomReferences = (Object[]) ArrayUtil.setLength(
          clientAtomReferences, newLength);
    if (vibrationVectors != null)
      vibrationVectors = (Vector3f[]) ArrayUtil.setLength(vibrationVectors,
          newLength);
    if (occupancies != null)
      occupancies = ArrayUtil.setLength(occupancies, newLength);
    if (bfactor100s != null)
      bfactor100s = ArrayUtil.setLength(bfactor100s, newLength);
    if (partialCharges != null)
      partialCharges = ArrayUtil.setLength(partialCharges, newLength);
    if (ellipsoids != null) {
      Object[][] ellipsoids2 = new Object[newLength][];
      for (int i = 0; i < ellipsoids.length; i++)
        ellipsoids2[i] = ellipsoids[i];
      ellipsoids = ellipsoids2;
    }
    if (atomNames != null)
      atomNames = ArrayUtil.setLength(atomNames, newLength);
    if (atomTypes != null)
      atomTypes = ArrayUtil.setLength(atomTypes, newLength);
    if (atomSerials != null)
      atomSerials = ArrayUtil.setLength(atomSerials, newLength);
    if (specialAtomIDs != null)
      specialAtomIDs = ArrayUtil.setLength(specialAtomIDs, newLength);
  }


  private void iterateOverAllNewBonds(JmolAdapter adapter, Object atomSetCollection) {
    JmolAdapter.BondIterator iterBond = adapter.getBondIterator(atomSetCollection);
    if (iterBond == null)
      return;
    short mad = viewer.getMadBond();
    short order;
    defaultCovalentMad = (jmolData == null ? mad : 0);
    boolean haveMultipleBonds = false;
    while (iterBond.hasNext()) {
      order = (short) iterBond.getEncodedOrder();
      bondAtoms(iterBond.getAtomUniqueID1(), iterBond.getAtomUniqueID2(), order);
      if (order > 1)
        haveMultipleBonds = true; 
    }
    if (haveMultipleBonds && someModelsHaveSymmetry && !viewer.getApplySymmetryToBonds())
      Logger.info("ModelSet: use \"set appletSymmetryToBonds TRUE \" to apply the file-based multiple bonds to symmetry-generated atoms.");
    defaultCovalentMad = mad;
  }
  
  private void bondAtoms(Object atomUid1, Object atomUid2, short order) {
    Atom atom1 = (Atom) htAtomMap.get(atomUid1);
    if (atom1 == null) {
      Logger.error("bondAtoms cannot find atomUid1?:" + atomUid1);
      return;
    }
    Atom atom2 = (Atom) htAtomMap.get(atomUid2);
    if (atom2 == null) {
      Logger.error("bondAtoms cannot find atomUid2?:" + atomUid2);
      return;
    }
    // note that if the atoms are already bonded then
    // Atom.bondMutually(...) will return null
    if (atom1.isBonded(atom2))
      return;
    Bond bond = bondMutually(atom1, atom2, order, getDefaultMadFromOrder(order));
    if (bond.isAromatic())
      someModelsHaveAromaticBonds = true;
    if (bondCount == bonds.length)
      bonds = (Bond[]) ArrayUtil.setLength(bonds, bondCount + 2
          * ATOM_GROWTH_INCREMENT);
    setBond(bondCount++, bond);
    //if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
      //fileHasHbonds = true;
  }

  /**
   * Pull in all spans of helix, etc. in the file(s)
   * 
   * We do turn first, because sometimes a group is defined
   * twice, and this way it gets marked as helix or sheet
   * if it is both one of those and turn.
   * 
   * @param adapter
   * @param atomSetCollection
   */
  private void iterateOverAllNewStructures(JmolAdapter adapter,
                                           Object atomSetCollection) {
    JmolAdapter.StructureIterator iterStructure = adapter
        .getStructureIterator(atomSetCollection);
    if (iterStructure != null)
      while (iterStructure.hasNext()) {
        //System.out.println(iterStructure.getStructureType() + iterStructure
          //  .getStartSequenceNumber()+" "+iterStructure.getEndSequenceNumber());
        if (!iterStructure.getStructureType().equals("turn")) {
          defineStructure(iterStructure.getModelIndex(),
              iterStructure.getStructureType(),
              iterStructure.getStructureID(), 
              iterStructure.getSerialID(),
              iterStructure.getStrandCount(),
              iterStructure.getStartChainID(), iterStructure
                  .getStartSequenceNumber(), iterStructure
                  .getStartInsertionCode(), iterStructure.getEndChainID(),
              iterStructure.getEndSequenceNumber(), iterStructure
                  .getEndInsertionCode());
        }
      }

    // define turns LAST. (pulled by the iterator first)
    // so that if they overlap they get overwritten:

    iterStructure = adapter.getStructureIterator(atomSetCollection);
    if (iterStructure != null)
      while (iterStructure.hasNext()) {
        if (iterStructure.getStructureType().equals("turn"))
          defineStructure(iterStructure.getModelIndex(),
              iterStructure.getStructureType(),
              iterStructure.getStructureID(), 1, 1,
              iterStructure.getStartChainID(), iterStructure
                  .getStartSequenceNumber(), iterStructure
                  .getStartInsertionCode(), iterStructure.getEndChainID(),
              iterStructure.getEndSequenceNumber(), iterStructure
                  .getEndInsertionCode());
      }
  }
  
  protected void defineStructure(int modelIndex, String structureType,
                                 String structureID, int serialID, int strandCount, 
                                 char startChainID, int startSequenceNumber,
                                 char startInsertionCode, char endChainID,
                                 int endSequenceNumber, char endInsertionCode) {
    if (modelIndex >= 0 || isTrajectory) { //from PDB file
      if (isTrajectory)
        modelIndex = 0;
      modelIndex += baseModelIndex;
      structuresDefinedInFile.set(modelIndex);
      super.defineStructure(modelIndex, structureType, 
          structureID, serialID, 
          strandCount, startChainID,
          startSequenceNumber, startInsertionCode, endChainID,
          endSequenceNumber, endInsertionCode);
      return;
    }
    for (int i = baseModelIndex; i < modelCount; i++) {
      structuresDefinedInFile.set(i);
      super.defineStructure(i, structureType, 
          structureID, serialID, 
          strandCount, startChainID,
          startSequenceNumber, startInsertionCode, endChainID,
          endSequenceNumber, endInsertionCode);
    }
  }

  ////// symmetry ///////
  
  private void initializeUnitCellAndSymmetry() {
    /*
     * really THREE issues here:
     * 1) does a model have an associated unit cell that could be displayed?
     * 2) are the coordinates fractional and so need to be transformed?
     * 3) does the model have symmetry operations that were applied?
     * 
     * This must be done for each model individually.
     * 
     */

    if (someModelsHaveUnitcells) {
      unitCells = new SymmetryInterface[modelCount];
      boolean haveMergeCells = (mergeModelSet != null && mergeModelSet.unitCells != null);
      for (int i = 0; i < modelCount; i++) {
        if (haveMergeCells && i < baseModelCount) {
          unitCells[i] = mergeModelSet.unitCells[i];
        } else {
          unitCells[i] = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
          unitCells[i].setSymmetryInfo(i, getModelAuxiliaryInfo(i));
        }
      }
    }
    if (someModelsHaveSymmetry) {
      getAtomBits(Token.symmetry, null);
      for (int iAtom = baseAtomIndex, iModel = -1, i0 = 0; iAtom < atomCount; iAtom++) {
        if (atoms[iAtom].modelIndex != iModel) {
          iModel = atoms[iAtom].modelIndex;
          i0 = baseAtomIndex
              + getModelAuxiliaryInfoInt(iModel, "presymmetryAtomIndex")
              + getModelAuxiliaryInfoInt(iModel, "presymmetryAtomCount");
        }
        if (iAtom >= i0)
          bsSymmetry.set(iAtom);
      }
    }
    if (someModelsHaveFractionalCoordinates) {
      for (int i = baseAtomIndex; i < atomCount; i++) {
        int modelIndex = atoms[i].modelIndex;
        if (!unitCells[modelIndex].getCoordinatesAreFractional())
          continue;
        unitCells[modelIndex].toCartesian(atoms[i]);
        if (Logger.debugging)
          Logger.debug("atom " + i + ": " + (Point3f) atoms[i]);
      }
    }
  }

  private void initializeBonding() {
    // perform bonding if necessary
    
    // 1. apply CONECT records and set bsExclude to omit them
    
    BitSet bsExclude = (getModelSetAuxiliaryInfo("someModelsHaveCONECT") == null ? null : new BitSet());
    if (bsExclude != null)
      setPdbConectBonding(baseAtomIndex, baseModelIndex, bsExclude);
    
    // 2. for each model in the collection, 
    int atomIndex = baseAtomIndex;
    int modelAtomCount = 0;
    boolean symmetryAlreadyAppliedToBonds = viewer.getApplySymmetryToBonds();
    boolean doAutoBond = viewer.getAutoBond();
    boolean forceAutoBond = viewer.getForceAutoBond();
    BitSet bs = null;
    boolean autoBonding = false;
    for (int i = baseModelIndex; i < modelCount; 
        atomIndex += modelAtomCount, i++) {
      modelAtomCount = getModelAuxiliaryInfoInt(i, "initialAtomCount");
      if (modelAtomCount < 0)
        modelAtomCount = atomCount; // old style -- one structure
      if (getModelAuxiliaryInfoBoolean(i, "noautobond")) 
        continue;
      int modelBondCount = getModelAuxiliaryInfoInt(i, "initialBondCount");
      if (modelBondCount < 0)
        modelBondCount = bondCount;
      boolean modelIsPDB = models[i].isPDB;
      boolean modelHasSymmetry = getModelAuxiliaryInfoBoolean(i, "hasSymmetry");
      //check for PDB file with fewer than one bond per every two atoms
      //this is in case the PDB format is being usurped for non-RCSB uses
      //In other words, say someone uses the PDB format to indicate atoms and
      //connectivity. We do NOT want to mess up that connectivity here. 
      //It would be OK if people used HETATM for every atom, but I think people
      //use ATOM, so that's a problem. Those atoms would not be excluded from the
      //automatic bonding, and additional bonds might be made.
      boolean doBond = (forceAutoBond || doAutoBond && (
                  modelBondCount == 0
               || modelIsPDB && jmolData == null && modelBondCount < modelAtomCount / 2  
               || modelHasSymmetry && !symmetryAlreadyAppliedToBonds
          ));
      if (!doBond) 
        continue;
      autoBonding = true;
      if (merging || modelCount > 1) {
        if (bs == null)
          bs = new BitSet(atomCount);
        //System.out.println(atomIndex + " " + modelAtomCount);
        for (int j = atomIndex + modelAtomCount; --j >= atomIndex;)
          bs.set(j);
      }
    }
    if (autoBonding) {
      autoBond(bs, bs, bsExclude, null);
      //Logger.info("ModelSet: autobonding; use  autobond=false  to not generate bonds automatically");
    } else {
      //Logger.info("ModelSet: not autobonding; use  forceAutobond=true  to force automatic bond creation");        
    }
  }

  private void finalizeGroupBuild() {
    // run this loop in increasing order so that the
    // groups get defined going up
    groups = new Group[groupCount];
    if (merging) {
      for (int i = 0; i < baseGroupIndex; i++) {
        groups[i] = mergeModelSet.groups[i];
        groups[i].setModelSet(this);
      }
    }
    for (int i = baseGroupIndex; i < groupCount; ++i) {
      distinguishAndPropagateGroup(i, chainOf[i], group3Of[i], seqcodes[i],
          firstAtomIndexes[i], (i == groupCount - 1 ? atomCount
              : firstAtomIndexes[i + 1]));
      chainOf[i] = null;
      group3Of[i] = null;
    }
    chainOf = null;
    group3Of = null;

    if (group3Lists != null) {
      Hashtable info = getModelSetAuxiliaryInfo();
      if (info != null) {
        info.put("group3Lists", group3Lists);
        info.put("group3Counts", group3Counts);
      }
    }

    group3Counts = null;
    group3Lists = null;

  }

  private void distinguishAndPropagateGroup(int groupIndex, Chain chain, String group3,
                                    int seqcode, int firstAtomIndex,
                                    int maxAtomIndex) {
    /*
     * called by finalizeGroupBuild()
     * 
     * first: build array of special atom names, 
     * for example "CA" for the alpha carbon is assigned #2
     * see JmolConstants.specialAtomNames[]
     * the special atoms all have IDs based on Atom.lookupSpecialAtomID(atomName)
     * these will be the same for each conformation
     * 
     * second: creates the monomers themselves based on this information
     * thus building the byte offsets[] array for each monomer, indicating which
     * position relative to the first atom in the group is which atom.
     * Each monomer.offsets[i] then points to the specific atom of that type
     * these will NOT be the same for each conformation  
     * 
     */
    int lastAtomIndex = maxAtomIndex - 1;

    if (lastAtomIndex < firstAtomIndex)
      throw new NullPointerException();
    int modelIndex = atoms[firstAtomIndex].modelIndex;

    Group group = null;
    if (group3 != null && specialAtomIDs != null && haveBioClasses) {
      if (jbr == null && haveBioClasses) {
        try {
          Class shapeClass = Class.forName("org.jmol.modelsetbio.Resolver");
          jbr = (JmolBioResolver) shapeClass.newInstance();
          haveBioClasses = true;
        } catch (Exception e) {
          Logger.error("developer error: org.jmol.modelsetbio.Resolver could not be found");
          haveBioClasses = false;
        }
      }
      if (haveBioClasses) {
        group = jbr.distinguishAndPropagateGroup(chain, group3, seqcode,
            firstAtomIndex, maxAtomIndex, modelIndex,
            specialAtomIndexes, specialAtomIDs, atoms);
      }
    }
    String key;
    if (group == null) {
      group = new Group(chain, group3, seqcode, firstAtomIndex, lastAtomIndex);
      key = "o>";
    } else { 
      key = (group.isProtein() ? "p>" : group.isNucleic() ? "n>"
          : group.isCarbohydrate() ? "c>" : "o>");
    }
    if (group3 != null)
      countGroup(modelIndex, key, group3);
    addGroup(chain, group);
    groups[groupIndex] = group;
    group.setGroupIndex(groupIndex);

    for (int i = maxAtomIndex; --i >= firstAtomIndex;)
      atoms[i].setGroup(group);

  }

  private void addGroup(Chain chain, Group group) {
    if (chain.groupCount == chain.groups.length)
      chain.groups = (Group[])ArrayUtil.doubleLength(chain.groups);
    chain.groups[chain.groupCount++] = group;
  }

  private void countGroup(int modelIndex, String code, String group3) {
    int ptm = modelIndex + 1;
    if (group3Lists == null || group3Lists[ptm] == null)
      return;
    String g3code = (group3 + "   ").substring(0, 3);
    int pt = group3Lists[ptm].indexOf(g3code);
    if (pt < 0) {
      group3Lists[ptm] += ",[" + g3code + "]";
      pt = group3Lists[ptm].indexOf(g3code);
      group3Counts[ptm] = (int[]) ArrayUtil.setLength(
          group3Counts[ptm], group3Counts[ptm].length + 10);
    }
    group3Counts[ptm][pt / 6]++;
    pt = group3Lists[ptm].indexOf(",[" + g3code);
    if (pt >= 0)
      group3Lists[ptm] = group3Lists[ptm].substring(0, pt) + code
          + group3Lists[ptm].substring(pt + 2);
    //becomes x> instead of ,[ 
    //these will be used for setting up the popup menu
    if (modelIndex >= 0)
      countGroup(-1, code, group3);
  }

  private void freeze() {
    htAtomMap.clear();
    // resize arrays
    if (atomCount < atoms.length)
      growAtomArrays(0);
    if (bondCount < bonds.length)
      bonds = (Bond[]) ArrayUtil.setLength(bonds, bondCount);

    // free bonds cache 
    
    for (int i = MAX_BONDS_LENGTH_TO_CACHE; --i > 0;) { // .GT. 0
      numCached[i] = 0;
      Bond[][] bondsCache = freeBonds[i];
      for (int j = bondsCache.length; --j >= 0;)
        bondsCache[j] = null;
    }

    setAtomNamesAndNumbers();

    // find elements for the popup menus
    
    findElementsPresent();

    // finalize all group business
    if (isPDB)
      calculateStructuresAllExcept(structuresDefinedInFile, true);

    molecules = null;
    moleculeCount = 0;
    currentModel = null;
    currentChain = null;    
    setStructureIds();

  }

  private void setAtomNamesAndNumbers() {
    // first, validate that all atomSerials are NaN
    if (atomSerials == null)
      atomSerials = new int[atomCount];
    // now, we'll assign 1-based atom numbers within each model
    boolean isZeroBased = isXYZ && viewer.getZeroBasedXyzRasmol();
    int lastModelIndex = Integer.MAX_VALUE;
    int atomNo = 1;
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = atoms[i];
      if (atom.modelIndex != lastModelIndex) {
        lastModelIndex = atom.modelIndex;
        atomNo = (isZeroBased ? 0 : 1);
      }
      // 1) do not change numbers assigned by adapter
      // 2) do not change the number already assigned when merging
      // 3) restart numbering with new atoms, not a continuation of old
      
      if (atomSerials[i] == 0)
        atomSerials[i] = (i < baseAtomIndex ? mergeModelSet.atomSerials[i]
            : atomNo++);
    }
    if (atomNames == null)
      atomNames = new String[atomCount];
    for (int i = 0; i < atomCount; ++i)
      if (atomNames[i] == null) {
        Atom atom = atoms[i];
        atomNames[i] = atom.getElementSymbol() + atom.getAtomNumber();
      }
  }

  private void findElementsPresent() {
    elementsPresent = new BitSet[modelCount];
    for (int i = 0; i < modelCount; i++)
      elementsPresent[i] = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      int n = atoms[i].getAtomicAndIsotopeNumber();
      if (n >= JmolConstants.elementNumberMax)
        n = JmolConstants.elementNumberMax
            + JmolConstants.altElementIndexFromNumber(n);
      elementsPresent[atoms[i].modelIndex].set(n);
    }
  }

  ///////////////  shapes  ///////////////
  
  private void finalizeShapes() {
    if (someModelsHaveAromaticBonds && viewer.getSmartAromatic())
      assignAromaticBonds(false);
    if (shapes == null)
      return;
    if (merging) {
      for (int i = 0; i < JmolConstants.SHAPE_MAX; i++)
        if ((shapes[i] = mergeModelSet.shapes[i]) != null)
          shapes[i].setModelSet(this);
      if (baseModelCount == 1 && shapes[JmolConstants.SHAPE_MEASURES] != null)
        setShapeProperty(JmolConstants.SHAPE_MEASURES, "clearModelIndex", null, null);
      viewer.clearShapes();
      merging = false;
      return;
    }
    loadShape(JmolConstants.SHAPE_BALLS);
    loadShape(JmolConstants.SHAPE_STICKS);
    loadShape(JmolConstants.SHAPE_MEASURES);
    loadShape(JmolConstants.SHAPE_BBCAGE);
    loadShape(JmolConstants.SHAPE_UCCAGE);
  }
  
  public void createAtomDataSet(int tokType, Object atomSetCollection, BitSet bsSelected) {
    if (atomSetCollection == null) 
        return; 
    // must be one of JmolConstants.LOAD_ATOM_DATA_TYPES
    JmolAdapter adapter = viewer.getModelAdapter();
    Point3f pt = new Point3f();
    Point3f v = new Point3f();
    float tolerance = ((Float) viewer.getParameter("loadAtomDataTolerance")).floatValue();
    int i = -1;
    int n = 0;
    int imax = (tokType == Token.xyz ? BitSetUtil.length(bsSelected) : 0);
    for (JmolAdapter.AtomIterator iterAtom = adapter
        .getAtomIterator(atomSetCollection); iterAtom.hasNext();) {
      float x = iterAtom.getX();
      float y = iterAtom.getY();
      float z = iterAtom.getZ();
      if (Float.isNaN(x + y + z))
        continue;
      
      if (tokType == Token.xyz) {
        while (++i < imax && !bsSelected.get(i)) {
          // continue
        }
        if ( i == imax)
          break;
        n++;
        if (Logger.debugging)
          Logger.debug("atomIndex = " + i+ ": " + (Point3f)atoms[i] + " --> (" + x + "," + y + "," + z);
        setAtomCoord(i, x, y, z);
        continue;
      }
      pt.set(x, y, z);
      BitSet bs = new BitSet();
      getAtomsWithin(-tolerance, pt, bs, -1);
      getAtomsWithin(tolerance, pt, bs, -1);
      bs.and(bsSelected);
      if (BitSetUtil.cardinalityOf(bsSelected) == viewer.getAtomCount()) {
        n = BitSetUtil.cardinalityOf(bs);
        if (n == 0) {
          Logger.warn("createAtomDataSet: no atom found at position " + pt);
          continue;
        } else if (n > 1 && Logger.debugging) {
          Logger.debug("createAtomDataSet: " + n + " atoms found at position " + pt);
        }
      }
      switch (tokType) {
      case Token.vibXyz:
        float vx = iterAtom.getVectorX();
        float vy = iterAtom.getVectorY();
        float vz = iterAtom.getVectorZ();
        if (Float.isNaN(vx + vy + vz))
          continue;
          v.set(vx, vy, vz);
          if (Logger.debugging)
            Logger.info("xyz: " + pt + " vib: " + v);
          setAtomCoord(bs,Token.vibXyz, v);
        break;
      case Token.occupancy:
        // [0 to 100], default 100
        setAtomProperty(bs, tokType, iterAtom.getOccupancy(), 0, null, null, null);
        break;
      case Token.partialCharge:
        // anything but NaN, default NaN
        setAtomProperty(bs, tokType, 0, iterAtom.getPartialCharge(), null, null, null);
        break;
      case Token.temperature:
        // anything but NaN but rounded to 0.01 precision and stored as a short (-32000 - 32000), default NaN
        setAtomProperty(bs, tokType, 0, iterAtom.getBfactor(), null, null, null);
        break;
      }
    }
    //finally:
    switch (tokType) {
    case Token.vibXyz:
      String vibName = adapter.getAtomSetName(atomSetCollection, 0);
      Logger.info("_vibrationName = " + vibName);
      viewer.setStringProperty("_vibrationName", vibName);
      break;
    case Token.xyz:
      Logger.info(n + " atom positions read");
      recalculateLeadMidpointsAndWingVectors(-1);
      break;
    }
    
  }
}
