/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

import java.awt.Rectangle;
import java.util.BitSet;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.atomdata.AtomData;
import org.jmol.bspt.Bspf;
import org.jmol.g3d.Graphics3D;
import org.jmol.geodesic.EnvelopeCalculation;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;

abstract public class AtomCollection {

  protected void releaseModelSet() {
    atoms = null;
    viewer = null;
    g3d = null;
    bspf = null;
    surfaceDistance100s = null;
    bsSurface = null;
    tainted = null;

    atomNames = null;
    atomTypes = null;
    atomSerials = null;
    clientAtomReferences = null;
    vibrationVectors = null;
    occupancies = null;
    bfactor100s = null;
    partialCharges = null;
    specialAtomIDs = null;
    ellipsoids = null;

  }

  protected void merge(AtomCollection mergeModelSet) {
    tainted = mergeModelSet.tainted;
    atomNames = mergeModelSet.atomNames;
    atomTypes = mergeModelSet.atomTypes;
    atomSerials = mergeModelSet.atomSerials;
    clientAtomReferences = mergeModelSet.clientAtomReferences;
    vibrationVectors = mergeModelSet.vibrationVectors;
    occupancies = mergeModelSet.occupancies;
    bfactor100s = mergeModelSet.bfactor100s;
    partialCharges = mergeModelSet.partialCharges;
    ellipsoids = mergeModelSet.ellipsoids;
    specialAtomIDs = mergeModelSet.specialAtomIDs;
    setHaveStraightness(false);
  }
  
  public void setHaveStraightness(boolean TF) {
    haveStraightness = TF;
  }
  
  protected boolean getHaveStraightness() {
    return haveStraightness;
  }
  
  public Viewer viewer;
  protected Graphics3D g3d;

  public Atom[] atoms;
  int atomCount;


  public Atom[] getAtoms() {
    return atoms;
  }

  public Atom getAtomAt(int atomIndex) {
    return atoms[atomIndex];
  }

  public int getAtomCount() {
    // not established until AFTER model loading
    return atomCount;
  }
  
  ////////////////////////////////////////////////////////////////
  // these may or may not be allocated
  // depending upon the AtomSetCollection characteristics
  //
  // used by Atom:
  //
  String[] atomNames;
  String[] atomTypes;
  int[] atomSerials;
  byte[] specialAtomIDs;
  Object[] clientAtomReferences;
  Vector3f[] vibrationVectors;
  byte[] occupancies;
  short[] bfactor100s;
  float[] partialCharges;
  protected Object[][] ellipsoids;
  protected int[] surfaceDistance100s;

  protected boolean haveStraightness;

  public boolean modelSetHasVibrationVectors(){
    return (vibrationVectors != null);
  }
  
  public String[] getAtomNames() {
    return atomNames;
  }

  public String[] getAtomTypes() {
    return atomTypes;
  }

  
  public float[] getPartialCharges() {
    return partialCharges;
  }

  public short[] getBFactors() {
    return bfactor100s;
  }

  private BitSet bsHidden = new BitSet();

  public void setBsHidden(BitSet bs) { //from selection manager
    bsHidden = bs;
  }

  public boolean isAtomHidden(int iAtom) {
    return bsHidden.get(iAtom);
  }
  
  //////////// atoms //////////////
  
  public String getAtomInfo(int i, String format) {
    return (format == null ? atoms[i].getInfo() : LabelToken.formatLabel(atoms[i],format));
  }

  public String getAtomInfoXYZ(int i, boolean useChimeFormat) {
    return atoms[i].getInfoXYZ(useChimeFormat);
  }

  public String getElementSymbol(int i) {
    return atoms[i].getElementSymbol();
  }

  public int getElementNumber(int i) {
    return atoms[i].getElementNumber();
  }

  String getElementName(int i) {
      return JmolConstants.elementNameFromNumber(atoms[i]
          .getAtomicAndIsotopeNumber());
  }

  public String getAtomName(int i) {
    return atoms[i].getAtomName();
  }

  public int getAtomNumber(int i) {
    return atoms[i].getAtomNumber();
  }

  public float getAtomX(int i) {
    return atoms[i].x;
  }

  public float getAtomY(int i) {
    return atoms[i].y;
  }

  public float getAtomZ(int i) {
    return atoms[i].z;
  }

  public Point3f getAtomPoint3f(int i) {
    return atoms[i];
  }

  public float getAtomRadius(int i) {
    return atoms[i].getRadius();
  }

  public float getAtomVdwRadius(int i) {
    return atoms[i].getVanderwaalsRadiusFloat();
  }

  public short getAtomColix(int i) {
    return atoms[i].getColix();
  }

  public String getAtomChain(int i) {
    return "" + atoms[i].getChainID();
  }

  public String getAtomSequenceCode(int i) {
    return atoms[i].getSeqcodeString();
  }

  public int getAtomModelIndex(int i) {
    return atoms[i].getModelIndex();
  }
  
  public Object[] getEllipsoid(int i) {
    return (i < 0 || ellipsoids == null || i >= ellipsoids.length ? null
        : ellipsoids[i]);
  }

  public Quaternion getQuaternion(int i, char qtype) {
    return atoms[i].group.getQuaternion(qtype);
  } 

  public Object getHelixData(BitSet bs, int tokType) {
    int iatom = BitSetUtil.firstSetBit(bs);
    return (iatom < 0 ? "null" : atoms[iatom].group.getHelixData(tokType, 
        viewer.getQuaternionFrame(), viewer.getHelixStep()));
  }
  
  protected int getAtomCountInModel(int modelIndex) {
    int n = 0;
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].modelIndex == modelIndex)
        n++;
    return n;
  }
  
  public int getAtomIndexFromAtomNumber(int atomNumber, BitSet bsVisibleFrames) {
    //definitely want FIRST (model) not last here
    for (int i = 0; i < atomCount; i++) {
      Atom atom = atoms[i];
      if (atom.getAtomNumber() == atomNumber && bsVisibleFrames.get(atom.modelIndex))
        return i;
    }
    return -1;
  }

  public void setFormalCharges(BitSet bs, int formalCharge) {
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        atoms[i].setFormalCharge(formalCharge);
        taint(i, TAINT_FORMALCHARGE);
      }
  }
  
  public float[] getAtomicCharges() {
    float[] charges = new float[atomCount];
    for (int i = atomCount; --i >= 0; )
      charges[i] = atoms[i].getElementNumber();
    return charges;
  }

  protected float getRadiusVdwJmol(Atom atom) {
    return JmolConstants.getVanderwaalsMar(atom.getElementNumber(),
        JmolConstants.VDW_JMOL) / 1000f;
  }
  
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  protected float maxBondingRadius = Float.MIN_VALUE;
  private float maxVanderwaalsRadius = Float.MIN_VALUE;

  public float getMaxVanderwaalsRadius() {
    //Dots
    if (maxVanderwaalsRadius == Float.MIN_VALUE)
      findMaxRadii();
    return maxVanderwaalsRadius;
  }

  protected void findMaxRadii() {
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float bondingRadius = atom.getBondingRadiusFloat();
      if (bondingRadius > maxBondingRadius)
        maxBondingRadius = bondingRadius;
      float vdwRadius = atom.getVanderwaalsRadiusFloat();
      if (vdwRadius > maxVanderwaalsRadius)
        maxVanderwaalsRadius = vdwRadius;
    }
  }

  private boolean hasBfactorRange;
  private int bfactor100Lo;
  private int bfactor100Hi;

  public void clearBfactorRange() {
    hasBfactorRange = false;
  }

  private void calcBfactorRange(BitSet bs) {
    if (!hasBfactorRange) {
      bfactor100Lo = Integer.MAX_VALUE;
      bfactor100Hi = Integer.MIN_VALUE;
      for (int i = atomCount; --i > 0;)
        if (bs == null || bs.get(i)) {
          int bf = atoms[i].getBfactor100();
          if (bf < bfactor100Lo)
            bfactor100Lo = bf;
          else if (bf > bfactor100Hi)
            bfactor100Hi = bf;
        }
      hasBfactorRange = true;
    }
  }

  public int getBfactor100Lo() {
    //ColorManager
    if (!hasBfactorRange) {
      if (viewer.isRangeSelected()) {
        calcBfactorRange(viewer.getSelectionSet());
      } else {
        calcBfactorRange(null);
      }
    }
    return bfactor100Lo;
  }

  public int getBfactor100Hi() {
    //ColorManager
    getBfactor100Lo();
    return bfactor100Hi;
  }

  private int surfaceDistanceMax;

  public int getSurfaceDistanceMax() {
    //ColorManager, Eval
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistanceMax;
  }

  private BitSet bsSurface;
  private int nSurfaceAtoms;

  int getSurfaceDistance100(int atomIndex) {
    //atom
    if (nSurfaceAtoms == 0)
      return -1;
    if (surfaceDistance100s == null)
      calcSurfaceDistances();
    return surfaceDistance100s[atomIndex];
  }

  private void calcSurfaceDistances() {
    calculateSurface(null, -1);
  }
  
  public Point3f[] calculateSurface(BitSet bsSelected, float envelopeRadius) {
    if (envelopeRadius < 0)
      envelopeRadius = EnvelopeCalculation.SURFACE_DISTANCE_FOR_CALCULATION;
    EnvelopeCalculation ec = new EnvelopeCalculation(viewer, atomCount, null);
    ec.calculate(Float.MAX_VALUE, envelopeRadius, 1, Float.MAX_VALUE, 
        bsSelected, BitSetUtil.copyInvert(bsSelected, atomCount), 
        true, false, false, false, true);
    Point3f[] points = ec.getPoints();
    surfaceDistanceMax = 0;
    bsSurface = ec.getBsSurfaceClone();
    surfaceDistance100s = new int[atomCount];
    nSurfaceAtoms = BitSetUtil.cardinalityOf(bsSurface);
    if (nSurfaceAtoms == 0 || points == null || points.length == 0)
      return points;
    //for (int i = 0; i < points.length; i++) {
    //  System.out.println("draw pt"+i+" " + Escape.escape(points[i]));
    //}
    float radiusAdjust = (envelopeRadius == Float.MAX_VALUE ? 0 : envelopeRadius);
    for (int i = 0; i < atomCount; i++) {
      //surfaceDistance100s[i] = Integer.MIN_VALUE;
      if (bsSurface.get(i)) {
        surfaceDistance100s[i] = 0;
      } else {
        float dMin = Float.MAX_VALUE;
        Atom atom = atoms[i];
        for (int j = points.length; --j >= 0;) {
          float d = Math.abs(points[j].distance(atom) - radiusAdjust);
          if (d < 0 && Logger.debugging)
            Logger.debug("draw d" + j + " " + Escape.escape(points[j])
                + " \"" + d + " ? " + atom.getInfo() + "\"");
          dMin = Math.min(d, dMin);
        }
        int d = surfaceDistance100s[i] = (int) (dMin * 100);
        surfaceDistanceMax = Math.max(surfaceDistanceMax, d);
      }
    }
    return points;
  }

  public void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    Point3f xyz = null;
    Point3f[] values = null;
    if (xyzValues instanceof Point3f)
      xyz = (Point3f) xyzValues;
    else
      values = (Point3f[]) xyzValues;
    if (xyz == null && (values == null || values.length == 0))
      return;
    int n = 0;
    for (int i = 0; i < atomCount; i++) {
      if (!bs.get(i))
        continue;
      if (values != null) { 
        if (n >= values.length)
          return;
        xyz = values[n++];
      }
      switch (tokType) {
      case Token.xyz:
        setAtomCoord(i, xyz.x, xyz.y, xyz.z);
        break;
      case Token.fracXyz:
        atoms[i].setFractionalCoord(xyz);
        taint(i, TAINT_COORD);
        break;
      case Token.vibXyz:
        setAtomVibrationVector(i, xyz.x, xyz.y, xyz.z);
        break;
      }
    }
  }

  private void setAtomVibrationVector(int atomIndex, float x, float y, float z) {
    setVibrationVector(atomIndex, x, y, z);  
    taint(atomIndex, TAINT_VIBRATION);
  }
  
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x = x;
    atoms[atomIndex].y = y;
    atoms[atomIndex].z = z;
    taint(atomIndex, TAINT_COORD);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    if (atomIndex < 0 || atomIndex >= atomCount)
      return;
    bspf = null;
    atoms[atomIndex].x += x;
    atoms[atomIndex].y += y;
    atoms[atomIndex].z += z;
    taint(atomIndex, TAINT_COORD);
  }

  protected void setAtomCoordRelative(BitSet atomSet, float x, float y, float z) {
    bspf = null;
    for (int i = atomCount; --i >= 0;)
      if (atomSet.get(i))
        setAtomCoordRelative(i, x, y, z);
  }

  public void setAtomProperty(BitSet bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    int n = 0;
    if (values != null && values.length == 0)
      return;
    for (int i = 0; i < atomCount; i++) {
      if (!bs.get(i))
        continue;
      if (values != null) {
        if (n >= values.length)
          return;
        fValue = values[n++];
        iValue = (int) fValue;
      } else if (list != null) {
        if (n >= list.length)
          return;
        sValue = list[n++];
      }
      Atom atom = atoms[i];
      switch (tok) {
      case Token.atomName:
        taint(i, TAINT_ATOMNAME);
        setAtomName(i, sValue);
        break;
      case Token.atomType:
        taint(i, TAINT_ATOMTYPE);
        setAtomType(i, sValue);
        break;
      case Token.atomX:
        setAtomCoord(i, fValue, atom.y, atom.z);
        break;
      case Token.atomY:
        setAtomCoord(i, atom.x, fValue, atom.z);
        break;
      case Token.atomZ:
        setAtomCoord(i, atom.x, atom.y, fValue);
        break;
      case Token.vibX:
      case Token.vibY:
      case Token.vibZ:
        setVibrationVector(i, tok, fValue);
        break;
      case Token.fracX:
      case Token.fracY:
      case Token.fracZ:
        atom.setFractionalCoord(tok, fValue);
        taint(i, TAINT_COORD);
        break;
      case Token.elemno:
      case Token.element:
        taint(i, TAINT_ELEMENT);
        atom.setAtomicAndIsotopeNumber(iValue);
        atom.setPaletteID(JmolConstants.PALETTE_CPK);
        atom.setColixAtom(viewer.getColixAtomPalette(atom,
            JmolConstants.PALETTE_CPK));
        break;
      case Token.formalCharge:
        atom.setFormalCharge(iValue);
        taint(i, TAINT_FORMALCHARGE);
        break;
      case Token.label:
      case Token.format:
        ((ModelSet)this).setAtomLabel(sValue, i);
        break;
      case Token.occupancy:
        if (iValue < 2)
          iValue = (int)(100 * fValue);
        if (setOccupancy(i, iValue))
          taint(i, TAINT_OCCUPANCY);
        break;
      case Token.partialCharge:
        if (setPartialCharge(i, fValue))
          taint(i, TAINT_PARTIALCHARGE);
        break;
      case Token.radius:
      case Token.spacefill:
        if (fValue < 0)
          fValue = 0;
        else if (fValue > Atom.RADIUS_MAX)
          fValue = Atom.RADIUS_MAX;
        atom.setMadAtom(viewer, 0, fValue);
        break;
      case Token.temperature:
        if (setBFactor(i, fValue))
          taint(i, TAINT_TEMPERATURE);
        break;
      case Token.valence:
        atom.setValence(iValue);
        taint(i, TAINT_VALENCE);
        break;
      case Token.vanderwaals:
        if (atom.setRadius(fValue))
          taint(i, TAINT_VANDERWAALS);
        else
          untaint(i, TAINT_VANDERWAALS);
        break;
      default:
        Logger.error("unsettable atom property: " + Token.nameOf(tok));
        break;
      }
    }
  }

  public float getVibrationCoord(int atomIndex, char c) {
    if (vibrationVectors == null || vibrationVectors[atomIndex] == null)
      return 0;
    switch (c) {
    case 'x':
      return vibrationVectors[atomIndex].x;
    case 'y':
      return vibrationVectors[atomIndex].y;
    default:
      return vibrationVectors[atomIndex].z;
    }
  }

  public Vector3f getVibrationVector(int atomIndex, boolean forceNew) {
    Vector3f v = (vibrationVectors == null ? null : vibrationVectors[atomIndex]);
    return (v == null && forceNew ? new Vector3f() : v);
  }

  protected void setVibrationVector(int atomIndex, float x, float y, float z) {
    if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z))
      return;
    if (vibrationVectors == null)
      vibrationVectors = new Vector3f[atoms.length];
    vibrationVectors[atomIndex] = new Vector3f(x, y, z);
    atoms[atomIndex].setVibrationVector();
  }

  private void setVibrationVector(int atomIndex, int tok, float fValue) {
    Vector3f v = getVibrationVector(atomIndex, true);
    if (v == null)
      v = new Vector3f();
    switch(tok) {
    case Token.vibX:
      v.x = fValue;
      break;
    case Token.vibY:
      v.y = fValue;
      break;
    case Token.vibZ:
      v.z = fValue;
      break;
    }
    setAtomVibrationVector(atomIndex, v.x, v.y, v.z);
  }

  protected void setAtomName(int atomIndex, String name) {
    if (atomNames == null)
      atomNames = new String[atoms.length];
    atomNames[atomIndex] = name;
  }

  protected void setAtomType(int atomIndex, String type) {
      if (atomTypes == null)
        atomTypes = new String[atoms.length];
      atomTypes[atomIndex] = type;
  }
  
  protected boolean setOccupancy(int atomIndex, int occupancy) {
    if (occupancies == null) {
      if (occupancy == 100)
        return false; // 100 is the default;
      occupancies = new byte[atoms.length];
    }
    occupancies[atomIndex] = (byte) (occupancy > 255 ? 255 : occupancy < 0 ? 0 : occupancy);
    return true;
  }
  
  protected boolean setPartialCharge(int atomIndex, float partialCharge) {
    if (Float.isNaN(partialCharge))
      return false;
    if (partialCharges == null) {
      if (partialCharge == 0)
        return false; // no need to store a 0.
      partialCharges = new float[atoms.length];
    }
    partialCharges[atomIndex] = partialCharge;
    return true;
  }

  protected boolean setBFactor(int atomIndex, float bfactor) {
    if (Float.isNaN(bfactor))
      return false;
    if (bfactor100s == null) {
      if (bfactor == 0 && bfactor100s == null) // there's no need to store a 0.
        return false;
      bfactor100s = new short[atoms.length];
    }
    bfactor100s[atomIndex] = (short) ((bfactor < -327.68f ? -327.68f
        : bfactor > 327.67 ? 327.67 : bfactor) * 100);
    return true;
  }

  protected void setEllipsoid(int atomIndex, Object[] ellipsoid) {
    if (ellipsoid == null)
      return;
    if (ellipsoids == null)
      ellipsoids = new Object[atoms.length][];
    ellipsoids[atomIndex] = ellipsoid;
  }

  // loading data
  
  public void setAtomData(int type, String name, String dataString) {
    float[] fData = null;
    BitSet bs = null;
    switch (type) {
    case TAINT_COORD:
      loadCoordinates(dataString, false);
      return;
    case TAINT_VIBRATION:
      loadCoordinates(dataString, true);
      return;
    case TAINT_MAX:
      fData = new float[atomCount];
      bs = new BitSet(atomCount);
      break;
    }
    int[] lines = Parser.markLines(dataString, ';');
    int n = 0;
    try {
      int nData = Parser.parseInt(dataString.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(dataString.substring(
            lines[i], lines[i + 1] - 1)));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        if (atomIndex < 0 || atomIndex >= atomCount)
          continue;
        Atom atom = atoms[atomIndex];
        n++;
        int pt = tokens.length - 1;
        float x = Parser.parseFloat(tokens[pt]);
        switch (type) {
        case TAINT_MAX:
          fData[atomIndex] = x;
          bs.set(atomIndex);
          continue;
        case TAINT_ATOMNAME:
          setAtomName(atomIndex, tokens[pt]);
          break;
        case TAINT_ATOMTYPE:
          setAtomType(atomIndex, tokens[pt]);
          break;
        case TAINT_ELEMENT:
          atom.setAtomicAndIsotopeNumber((int)x);
          atom.setPaletteID(JmolConstants.PALETTE_CPK);
          atom.setColixAtom(viewer.getColixAtomPalette(atom, JmolConstants.PALETTE_CPK));
          break;
        case TAINT_FORMALCHARGE:
          atom.setFormalCharge((int)x);          
          break;
        case TAINT_PARTIALCHARGE:
          setPartialCharge(atomIndex, x);          
          break;
        case TAINT_TEMPERATURE:
          setBFactor(atomIndex, x);
          break;
        case TAINT_VALENCE:
          atom.setValence((int)x);          
          break;
        case TAINT_VANDERWAALS:
          atom.setRadius(x);          
          break;
        }
        taint(atomIndex, (byte) type);
      }
      if (type == TAINT_MAX && n > 0)
        viewer.setData(name, new Object[] {name, fData, bs}, 0, 0, 0, 0, 0);
        
    } catch (Exception e) {
      Logger.error("AtomCollection.loadData error: " + e);
    }    
  }
  
  private void loadCoordinates(String data, boolean isVibrationVectors) {
    if (!isVibrationVectors)
      bspf = null;
    int[] lines = Parser.markLines(data, ';');
    try {
      int nData = Parser.parseInt(data.substring(0, lines[0] - 1));
      for (int i = 1; i <= nData; i++) {
        String[] tokens = Parser.getTokens(Parser.parseTrimmed(data.substring(
            lines[i], lines[i + 1])));
        int atomIndex = Parser.parseInt(tokens[0]) - 1;
        float x = Parser.parseFloat(tokens[3]);
        float y = Parser.parseFloat(tokens[4]);
        float z = Parser.parseFloat(tokens[5]);
        if (isVibrationVectors) {
          setAtomVibrationVector(atomIndex, x, y, z);
        } else {
          setAtomCoord(atomIndex, x, y, z);
        }
      }
    } catch (Exception e) {
      Logger.error("Frame.loadCoordinate error: " + e);
    }
  }


  // Binary Space Partitioning Forest
  
  protected Bspf bspf;

  // state tainting
  
  ////  atom coordinate and property changing  //////////
  
  // be sure to add the name to the list below as well!
  final public static byte TAINT_ATOMNAME = 0;
  final public static byte TAINT_ATOMTYPE = 1;
  final public static byte TAINT_COORD = 2;
  final private static byte TAINT_ELEMENT = 3;
  final private static byte TAINT_FORMALCHARGE = 4;
  final private static byte TAINT_OCCUPANCY = 5;
  final private static byte TAINT_PARTIALCHARGE = 6;
  final private static byte TAINT_TEMPERATURE = 7;
  final private static byte TAINT_VALENCE = 8;
  final private static byte TAINT_VANDERWAALS = 9;
  final private static byte TAINT_VIBRATION = 10;
  final public static byte TAINT_MAX = 11; // 1 more than last number, above
  
  final private static String[] userSettableValues = {
    "atomName",
    "atomType",
    "coord",
    "element",
    "formalCharge",
    "occupany",
    "partialCharge",
    "temperature",
    "valence",
    "vanderWaals",
    "vibrationVector"
  };
  
  static {
   if (userSettableValues.length != TAINT_MAX)
     Logger.error("AtomCollection.java userSettableValues is not length TAINT_MAX!");
  }
  
  protected BitSet[] tainted;  // not final -- can be set to null

  public static int getUserSettableType(String dataType) {
    boolean isExplicit = (dataType.indexOf("property_") == 0);
    String check = (isExplicit ? dataType.substring(9) : dataType);
    for (int i = 0; i < TAINT_MAX; i++)
      if (userSettableValues[i].equalsIgnoreCase(check))
        return i;
    return (isExplicit ? TAINT_MAX : -1);
  }

  public BitSet getTaintedAtoms(byte type) {
    return tainted == null ? null : tainted[type];
  }
  
  protected void taint(int atomIndex, byte type) {
    if (tainted == null)
      tainted = new BitSet[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = new BitSet(atomCount);
    tainted[type].set(atomIndex);
  }

  private void untaint(int i, byte type) {
    if (tainted == null || tainted[type] == null)
      return;
    tainted[type].clear(i);
  }

  public void setTaintedAtoms(BitSet bs, byte type) {
    if (bs == null) {
      if (tainted == null)
        return;
      tainted[type] = null;
      return;
    }
    if (tainted == null)
      tainted = new BitSet[TAINT_MAX];
    if (tainted[type] == null)
      tainted[type] = new BitSet(atomCount);
    BitSetUtil.copy(bs, tainted[type]);
  }

  public String getAtomicPropertyState(int taintWhat, BitSet bsSelected) {
    BitSet bs;
    StringBuffer commands = new StringBuffer();
    for (byte i = 0; i < TAINT_MAX; i++)
      if (taintWhat < 0 || i == taintWhat)
      if((bs = (bsSelected != null ? bsSelected : getTaintedAtoms(i))) != null)
        getAtomicPropertyState(commands, atoms, atomCount, i, bs, null, null);
    return commands.toString();
  }
  
  public static void getAtomicPropertyState(StringBuffer commands,
                                            Atom[] atoms, int atomCount,
                                            byte type, BitSet bs, String label,
                                            float[] fData) {
    //see setAtomData()
    StringBuffer s = new StringBuffer();
    String dataLabel = (label == null ? userSettableValues[type] : label)
        + " set";
    int n = 0;
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        s.append(i + 1).append(" ").append(atoms[i].getElementSymbol()).append(
            " ").append(atoms[i].getInfo().replace(' ', '_')).append(" ");
        switch (type) {
        case TAINT_MAX:
          if (i < fData.length) // when data are appended, the array may not extend that far
            s.append(fData[i]);
          break;
        case TAINT_ATOMNAME:
          s.append(atoms[i].getAtomName());
          break;
        case TAINT_ATOMTYPE:
          s.append(atoms[i].getAtomType());
          break;
        case TAINT_COORD:
          s.append(atoms[i].x).append(" ").append(atoms[i].y).append(" ")
              .append(atoms[i].z);
          break;
        case TAINT_VIBRATION:
          Vector3f v = atoms[i].getVibrationVector();
          if (v == null)
            v = new Vector3f();
          s.append(v.x).append(" ").append(v.y).append(" ").append(v.z);
        case TAINT_ELEMENT:
          s.append(atoms[i].getAtomicAndIsotopeNumber());
          break;
        case TAINT_FORMALCHARGE:
          s.append(atoms[i].getFormalCharge());
          break;
        case TAINT_OCCUPANCY:
          s.append(atoms[i].getOccupancy100());
          break;
        case TAINT_PARTIALCHARGE:
          s.append(atoms[i].getPartialCharge());
          break;
        case TAINT_TEMPERATURE:
          s.append(atoms[i].getBfactor100() / 100f);
          break;
        case TAINT_VALENCE:
          s.append(atoms[i].getValence());
          break;
        case TAINT_VANDERWAALS:
          s.append(atoms[i].getVanderwaalsRadiusFloat());
          break;
        }
        s.append(" ;\n");
        ++n;
      }
    if (n == 0)
      return;
    commands.append("\n  DATA \"" + dataLabel + "\"\n").append(n).append(
        " ;\nJmol Property Data Format 1 -- Jmol ").append(
        Viewer.getJmolVersion()).append(";\n");
    commands.append(s);
    commands.append("  end \"" + dataLabel + "\";\n");
  }

///////////////////////////////////////////
  
  private final static int minimumPixelSelectionRadius = 6;

  /*
   * generalized; not just balls
   * 
   * This algorithm assumes that atoms are circles at the z-depth
   * of their center point. Therefore, it probably has some flaws
   * around the edges when dealing with intersecting spheres that
   * are at approximately the same z-depth.
   * But it is much easier to deal with than trying to actually
   * calculate which atom was clicked
   *
   * A more general algorithm of recording which object drew
   * which pixel would be very expensive and not worth the trouble
   */
  protected void findNearestAtomIndex(int x, int y, Atom[] closest) {
    Atom champion = null;
    //int championIndex = -1;
    for (int i = atomCount; --i >= 0;) {
      Atom contender = atoms[i];
      if (contender.isClickable()
          && isCursorOnTopOf(contender, x, y, minimumPixelSelectionRadius,
              champion))
        champion = contender;
    }
    closest[0] = champion;
  }

  /**
   * used by Frame and AminoMonomer and NucleicMonomer -- does NOT check for clickability
   * @param contender
   * @param x
   * @param y
   * @param radius
   * @param champion
   * @return true if user is pointing to this atom
   */
  boolean isCursorOnTopOf(Atom contender, int x, int y, int radius,
                          Atom champion) {
    return contender.screenZ > 1 && !g3d.isClippedZ(contender.screenZ)
        && g3d.isInDisplayRange(contender.screenX, contender.screenY)
        && contender.isCursorOnTopOf(x, y, radius, champion);
  }

  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  private final BitSet bsEmpty = new BitSet();
  private final BitSet bsFoundRectangle = new BitSet();

  public BitSet findAtomsInRectangle(Rectangle rect, BitSet bsModels) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if (bsModels.get(atom.modelIndex) && atom.isVisible(0) 
          && rect.contains(atom.screenX, atom.screenY))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  protected void fillAtomData(AtomData atomData, int mode) {
    atomData.atomXyz = atoms;
    atomData.atomCount = atomCount;
    atomData.atomicNumber = new int[atomCount];
    boolean includeRadii = (mode == AtomData.MODE_FILL_COORDS_AND_RADII);
    if (includeRadii)
      atomData.atomRadius = new float[atomCount];
    for (int i = 0; i < atomCount; i++) {
      if (atomData.modelIndex >= 0
          && atoms[i].modelIndex != atomData.firstModelIndex) {
        if (atomData.bsIgnored == null)
          atomData.bsIgnored = new BitSet();
        atomData.bsIgnored.set(i);
        continue;
      }
      atomData.atomicNumber[i] = atoms[i].getElementNumber();
      atomData.lastModelIndex = atoms[i].modelIndex;
      if (includeRadii)
        atomData.atomRadius[i] = (atomData.adpMode == 1 ? atoms[i]
            .getADPMinMax(true) : atomData.adpMode == -1 ? atoms[i]
            .getADPMinMax(false) : atomData.useIonic ? atoms[i]
            .getBondingRadiusFloat() : atoms[i].getVanderwaalsRadiusFloat());
    }
  }
  
  protected Point3f[][] getAdditionalHydrogens(BitSet atomSet, int[] nTotal) {
    Vector3f z = new Vector3f();
    Vector3f x = new Vector3f();
    Point3f[][] hAtoms = new Point3f[atomCount][];
    Point3f pt;
    int nH = 0;
    // just not doing aldehydes here -- all A-X-B bent == sp3 for now
    for (int i = 0; i < atomCount; i++) {
      if (atomSet.get(i) && atoms[i].getElementNumber() == 6) {

        int n = 0;
        Atom atom = atoms[i];
        int nBonds = (atom.getCovalentHydrogenCount() > 0 ? 0 : atom
            .getCovalentBondCount());
        if (nBonds == 3 || nBonds == 2) { //could be XA3 sp2 or XA2 sp
          String hybridization = getHybridizationAndAxes(i, z, x, "sp3", true);
          if (hybridization == null || hybridization.equals("sp"))
            nBonds = 0;
        }
        if (nBonds > 0 && nBonds <= 4)
          n += 4 - nBonds;
        hAtoms[i] = new Point3f[n];
        nH += n;
        n = 0;
        switch (nBonds) {
        case 1:
          getHybridizationAndAxes(i, z, x, "sp3a", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          getHybridizationAndAxes(i, z, x, "sp3b", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          getHybridizationAndAxes(i, z, x, "sp3c", false);
          pt = new Point3f(z);
          pt.scaleAdd(1.1f, atom);
          hAtoms[i][n++] = pt;
          break;
        case 2:
          String hybridization = getHybridizationAndAxes(i, z, x, "sp3", true);
          if (hybridization != null && !hybridization.equals("sp")) {
            getHybridizationAndAxes(i, z, x, "lpa", false);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
            getHybridizationAndAxes(i, z, x, "lpb", false);
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
          }
          break;
        case 3:
          if (getHybridizationAndAxes(i, z, x, "sp3", true) != null) {
            pt = new Point3f(z);
            pt.scaleAdd(1.1f, atom);
            hAtoms[i][n++] = pt;
          }
        default:
        }

      }
    }
    nTotal[0] = nH;
    return hAtoms;
  }

  ////// special method for lcaoCartoons
  
  public String getHybridizationAndAxes(int atomIndex, Vector3f z, Vector3f x,
                           String lcaoTypeRaw, boolean hybridizationCompatible) {
    String lcaoType = (lcaoTypeRaw.length() > 0 && lcaoTypeRaw.charAt(0) == '-' ? lcaoTypeRaw
        .substring(1)
        : lcaoTypeRaw);
    Atom atom = atoms[atomIndex];
    String hybridization = "";
    z.set(0, 0, 0);
    x.set(0, 0, 0);
    Atom atom1 = atom;
    Atom atom2 = atom;
    int nBonds = 0;
    float _180 = (float) Math.PI * 0.95f;
    Vector3f n = new Vector3f();
    Vector3f x2 = new Vector3f();
    Vector3f x3 = new Vector3f(3.14159f, 2.71828f, 1.41421f);
    Vector3f x4 = new Vector3f();
    Vector3f y1 = new Vector3f();
    Vector3f y2 = new Vector3f();
    if (atom.bonds != null)
      for (int i = atom.bonds.length; --i >= 0;)
        if (atom.bonds[i].isCovalent()) {
          ++nBonds;
          atom1 = atom.bonds[i].getOtherAtom(atom);
          n.sub(atom, atom1);
          n.normalize();
          z.add(n);
          switch (nBonds) {
          case 1:
            x.set(n);
            atom2 = atom1;
            break;
          case 2:
            x2.set(n);
            break;
          case 3:
            x3.set(n);
            x4.set(-z.x, -z.y, -z.z);
            break;
          case 4:
            x4.set(n);
            break;
          default:
            i = -1;
          }
        }
    switch (nBonds) {
    case 0:
      z.set(0, 0, 1);
      x.set(1, 0, 0);
      break;
    case 1:
      if (lcaoType.indexOf("sp3") == 0) { // align z as sp3 orbital
        hybridization = "sp3";
        x.cross(x3, z);
        y1.cross(z, x);
        x.normalize();
        y1.normalize();
        y2.set(x);
        z.normalize();
        x.scaleAdd(2.828f, x, z); // 2*sqrt(2)
        if (!lcaoType.equals("sp3a") && !lcaoType.equals("sp3")) {
          x.normalize();
          AxisAngle4f a = new AxisAngle4f(z.x, z.y, z.z, (lcaoType
              .equals("sp3b") ? 1 : -1) * 2.09439507f); // PI*2/3
          Matrix3f m = new Matrix3f();
          m.setIdentity();
          m.set(a);
          m.transform(x);
        }
        z.set(x);
        x.cross(y1, z);
        break;
      }
      hybridization = "sp";
      if (atom1.getCovalentBondCount() == 3) {
        //special case, for example R2C=O oxygen
        getHybridizationAndAxes(atom1.atomIndex, z, x3, lcaoType, false);
        x3.set(x);
        if (lcaoType.indexOf("sp2") == 0) { // align z as sp2 orbital
          hybridization = "sp2";
          z.scale(-1);
        }
      }
      x.cross(x3, z);
      break;
    case 2:
      if (z.length() < 0.1) {
        // linear A--X--B
        hybridization = "sp";
        if (!lcaoType.equals("pz")) {
          if (atom1.getCovalentBondCount() != 3)
            atom1 = atom2;
          if (atom1.getCovalentBondCount() == 3) {
            //special case, for example R2C=C=CR2 central carbon
            getHybridizationAndAxes(atom1.atomIndex, x, z, "pz", false);
            if (lcaoType.equals("px"))
              x.scale(-1);
            z.set(x2);
            break;
          }
        }
        z.set(x);
        x.cross(x3, z);
        break;
      }
      // bent A--X--B
      hybridization = (lcaoType.indexOf("sp3") == 0 ? "sp3" : "sp2");
      x3.cross(z, x);
      if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
        if (lcaoType.equals("sp2a") || lcaoType.equals("sp2b")) {
          z.set(lcaoType.indexOf("b") >= 0 ? x2 : x);
          z.scale(-1);
        }
        x.cross(z, x3);
        break;
      }
      if (lcaoType.indexOf("lp") == 0) { // align z as lone pair
        hybridization = "lp"; //any is OK
        x3.normalize();
        z.normalize();
        y1.scaleAdd(1.2f, x3, z);
        y2.scaleAdd(-1.2f, x3, z);
        if (!lcaoType.equals("lp"))
          z.set(lcaoType.indexOf("b") >= 0 ? y2 : y1);
        x.cross(z, x3);
        break;
      }
      hybridization = lcaoType;
      // align z as p orbital
      x.cross(z, x3);
      z.set(x3);
      if (z.z < 0) {
        z.set(-z.x, -z.y, -z.z);
        x.set(-x.x, -x.y, -x.z);
      }
      break;
    default:
      //3 or 4 bonds
      if (x.angle(x2) < _180)
        y1.cross(x, x2);
      else
        y1.cross(x, x3);
      y1.normalize();
      if (x2.angle(x3) < _180)
        y2.cross(x2, x3);
      else
        y2.cross(x, x3);
      y2.normalize();
      if (Math.abs(y2.dot(y1)) < 0.95f) {
        hybridization = "sp3";
        if (lcaoType.indexOf("sp") == 0) { // align z as sp3 orbital
          z.set(lcaoType.equalsIgnoreCase("sp3")
                  || lcaoType.indexOf("d") >= 0 ? x4
                  : lcaoType.indexOf("c") >= 0 ? x3
                      : lcaoType.indexOf("b") >= 0 ? x2 : x);
          z.scale(-1);
          x.set(y1);
        } else { //needs testing here
          if (lcaoType.indexOf("lp") == 0 && nBonds == 3) { // align z as lone pair            
            hybridization = "lp"; //any is OK
          }
          x.cross(z, x);
        }
        break;
      }
      hybridization = "sp2";
      if (lcaoType.indexOf("sp") == 0) { // align z as sp2 orbital
        z.set(lcaoType.equalsIgnoreCase("sp3") || lcaoType.indexOf("d") >= 0 ? x4
                : lcaoType.indexOf("c") >= 0 ? x3
                    : lcaoType.indexOf("b") >= 0 ? x2 : x);
        z.scale(-1);
        x.set(y1);
        break;
      }
      // align z as p orbital
      z.set(y1);
      if (z.z < 0) {
        z.set(-z.x, -z.y, -z.z);
        x.set(-x.x, -x.y, -x.z);
      }
    }

    x.normalize();
    z.normalize();

    if (Logger.debugging) {
      Logger.debug(atom.getInfo() + " nBonds=" + nBonds + " " + hybridization);
    }
    if (hybridizationCompatible) {
      if (hybridization == "")
        return null;
      if (lcaoType.indexOf("p") == 0) {
        if (hybridization == "sp3")
          return null;
      } else {
        if (lcaoType.indexOf(hybridization) < 0)
          return null;
      }
    }
    return hybridization;
  }
  
  protected String getChimeInfo(int tok, BitSet bs) {
    StringBuffer info = new StringBuffer("\n");
    char id;
    String s = "";
    Chain clast = null;
    Group glast = null;
    int modelLast = -1;
    int n = 0;
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i)) {
        id = atoms[i].getChainID();
        s = (id == '\0' ? " " : "" + id);
        switch (tok) {
        case Token.chain:
          break;
        case Token.selected:
          s = atoms[i].getInfo();
          break;
        case Token.atoms:
          s = "" + atoms[i].getAtomNumber();
          break;
        case Token.group:
          s = atoms[i].getGroup3(false);
          break;
        case Token.residue:
          s = "[" + atoms[i].getGroup3(false) + "]" + atoms[i].getSeqcodeString()
              + ":" + s;
          break;
        case Token.sequence:
          if (atoms[i].getModelIndex() != modelLast) {
            info.append('\n');
            n = 0;
            modelLast = atoms[i].getModelIndex();
            info.append("Model " + atoms[i].getModelNumber());
            glast = null;
            clast = null;
          }
          if (atoms[i].getChain() != clast) {
            info.append('\n');
            n = 0;
            clast = atoms[i].getChain();
            info.append("Chain " + s + ":\n");
            glast = null;
          }
          Group g = atoms[i].getGroup();
          if (g != glast) {
            if ((n++) % 5 == 0 && n > 1)
              info.append('\n');
            TextFormat.lFill(info, "          ", "[" + atoms[i].getGroup3(false)
                + "]" + atoms[i].getResno() + " ");
            glast = g;
          }
          continue;
        default:
          return "";
        }
        if (info.indexOf("\n" + s + "\n") < 0)
          info.append(s).append('\n');
      }
    if (tok == Token.sequence)
      info.append('\n');
    return info.toString().substring(1);
  }

  /* ******************************************************
   * 
   * These next methods are used by Eval to select for 
   * specific atom sets. They all return a BitSet
   * 
   ********************************************************/

  /**
   * general unqualified lookup of atom set type
   * @param tokType
   * @param specInfo
   * @return BitSet; or null if we mess up the type
   */
  protected BitSet getAtomBits(int tokType, Object specInfo) {
    BitSet bs = new BitSet();
    BitSet bsInfo, bsTemp;
    int iSpec;
    switch (tokType) {
    case Token.atomno:
      iSpec = ((Integer) specInfo).intValue();
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].getAtomNumber() == iSpec)
          bs.set(i);
      return bs;
    case Token.atomName:
      String names = "," + specInfo + ",";
      for (int i = atomCount; --i >= 0;) {
        String name = atoms[i].getAtomName();
        if (names.indexOf(name) >= 0)
          if (names.indexOf("," + name + ",") >= 0)
            bs.set(i);
      }
      return bs;
    case Token.atomType:
      String types = "," + specInfo + ",";
      for (int i = atomCount; --i >= 0;) {
        String type = atoms[i].getAtomType();
        if (types.indexOf(type) >= 0)
          if (types.indexOf("," + type + ",") >= 0)
            bs.set(i);
      }
      return bs;
    case Token.spec_resid:
      iSpec = ((Integer) specInfo).intValue();
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].getGroupID() == iSpec)
          bs.set(i);
      return bs;
    case Token.spec_chain:
      return getChainBits((char) ((Integer) specInfo).intValue());
    case Token.spec_seqcode:
      return getSeqcodeBits(((Integer) specInfo).intValue(), true);
    case Token.hetero:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isHetero())
          bs.set(i);
      return bs;
    case Token.hydrogen:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].getElementNumber() == 1)
          bs.set(i);
      return bs;
    case Token.protein:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isProtein())
          bs.set(i);
      return bs;
    case Token.carbohydrate:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isCarbohydrate())
          bs.set(i);
      return bs;
    case Token.helix: //WITHIN -- not ends
    case Token.sheet: //WITHIN -- not ends
      byte type = (tokType == Token.helix ? 
          JmolConstants.PROTEIN_STRUCTURE_HELIX : JmolConstants.PROTEIN_STRUCTURE_SHEET);
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isWithinStructure(type))
          bs.set(i);
      return bs;
    case Token.nucleic:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isNucleic())
          bs.set(i);
      return bs;
    case Token.dna:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isDna())
          bs.set(i);
      return bs;
    case Token.rna:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isRna())
          bs.set(i);
      return bs;
    case Token.purine:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isPurine())
          bs.set(i);
      return bs;
    case Token.pyrimidine:
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isPyrimidine())
          bs.set(i);
      return bs;
    case Token.cell:
      int[] info = (int[]) specInfo;
      Point3f cell = new Point3f(info[0] / 1000f, info[1] / 1000f,
          info[2] / 1000f);
      for (int i = atomCount; --i >= 0;)
        if (isInLatticeCell(i, cell))
          bs.set(i);
      return bs;
    case Token.group:
      bsInfo = (BitSet) specInfo;
      Group groupLast = null;
      for (int i = atomCount; --i >= 0;) {
        if (!bsInfo.get(i))
          continue;
        Atom atom = atoms[i];
        Group group = atom.getGroup();
        if (group != groupLast) {
          group.selectAtoms(bs);
          groupLast = group;
        }
      }
      return bs;
    case Token.chain:
      bsInfo = (BitSet) specInfo;
      Chain chainLast = null;
      for (int i = atomCount; --i >= 0;) {
        if (!bsInfo.get(i))
          continue;
        Chain chain = atoms[i].getChain();
        if (chain != chainLast) {
          for (int j = atomCount; --j >= 0;)
            if (atoms[j].getChain() == chain)
              bs.set(j);
          chainLast = chain;
        }
      }
      return bs;
    case Token.structure:
      bsInfo = (BitSet) specInfo;
      Object structureLast = null;
      for (int i = atomCount; --i >= 0;) {
        if (!bsInfo.get(i))
          continue;
        Object structure = atoms[i].getGroup().getStructure();
        if (structure != null && structure != structureLast) {
          for (int j = atomCount; --j >= 0;)
            if (atoms[j].getGroup().getStructure() == structure)
              bs.set(j);
          structureLast = structure;
        }
      }
      return bs;
    case Token.model:
      bsInfo = (BitSet) specInfo;
      bsTemp = new BitSet();
      for (int i = atomCount; --i >= 0;)
        if (bsInfo.get(i))
          bsTemp.set(atoms[i].modelIndex);
      for (int i = atomCount; --i >= 0;)
        if (bsTemp.get(atoms[i].modelIndex))
          bs.set(i);
      return bs;
    case Token.element:
      bsInfo = (BitSet) specInfo;
      bsTemp = new BitSet();
      for (int i = atomCount; --i >= 0;)
        if (bsInfo.get(i))
          bsTemp.set(getElementNumber(i));
      for (int i = atomCount; --i >= 0;)
        if (bsTemp.get(getElementNumber(i)))
          bs.set(i);
      return bs;
    case Token.site:
      bsInfo = (BitSet) specInfo;
      bsTemp = new BitSet();
      for (int i = atomCount; --i >= 0;)
        if (bsInfo.get(i))
          bsTemp.set(atoms[i].atomSite);
      for (int i = atomCount; --i >= 0;)
        if (bsTemp.get(atoms[i].atomSite))
          bs.set(i);
      return bs;
    case Token.identifier:
      return getIdentifierOrNull((String) specInfo);
    case Token.spec_atom:
      String atomSpec = ((String) specInfo).toUpperCase();
      if (atomSpec.indexOf("\\?") >= 0)
        atomSpec = TextFormat.simpleReplace(atomSpec, "\\?","\1");
      /// here xx*yy is NOT changed to "xx??????????yy"
      for (int i = atomCount; --i >= 0;)
        if (isAtomNameMatch(atoms[i], atomSpec, false))
          bs.set(i);
      return bs;
    case Token.spec_alternate:
      String spec = (String) specInfo;
      for (int i = atomCount; --i >= 0;)
        if (atoms[i].isAlternateLocationMatch(spec))
          bs.set(i);
      return bs;
    case Token.spec_name_pattern:
      return getSpecName((String) specInfo);
    }
    Logger.error("MISSING getAtomBits entry for " + Token.nameOf(tokType));
    return null;
  }

  protected boolean isInLatticeCell(int i, Point3f cell) {
    Point3f pt = atoms[i].getFractionalCoord();
    float slop = 0.02f;
    // {1 1 1} here is the original cell
    if (pt.x < cell.x - 1f - slop || pt.x > cell.x + slop)
      return false;
    if (pt.y < cell.y - 1f - slop || pt.y > cell.y + slop)
      return false;
    if (pt.z < cell.z - 1f - slop || pt.z > cell.z + slop)
      return false;
    return true;
  }

   /**
   * overhauled by RMH Nov 1, 2006.
   * 
   * @param identifier
   * @return null or bs
   */
  private BitSet getIdentifierOrNull(String identifier) {
    //a primitive lookup scheme when [ ] are not used
    //nam
    //na?
    //nam45
    //nam45C
    //nam45^
    //nam45^A
    //nam45^AC -- note, no colon here -- if present, handled separately
    //nam4? does NOT match anything for PDB files, but might for others
    //atom specifiers:
    //H?
    //H32
    //H3?

    //in the case of a ?, we take the whole thing
    // * can be used here, but not with ?
    //first check with * option OFF
    BitSet bs = getSpecNameOrNull(identifier, false);
    
    if (identifier.indexOf("\\?") >= 0)
      identifier = TextFormat.simpleReplace(identifier, "\\?","\1");
    if (bs != null || identifier.indexOf("?") > 0)
      return bs;
    // now check with * option ON
    if (identifier.indexOf("*") > 0) 
      return getSpecNameOrNull(identifier, true);
    
    int len = identifier.length();
    int pt = 0;
    while (pt < len && Character.isLetter(identifier.charAt(pt)))
      ++pt;
    bs = getSpecNameOrNull(identifier.substring(0, pt), false);
    if (pt == len)
      return bs;
    if (bs == null)
      bs = new BitSet();
    //
    // look for a sequence number or sequence number ^ insertion code
    //
    int pt0 = pt;
    while (pt < len && Character.isDigit(identifier.charAt(pt)))
      ++pt;
    int seqNumber = 0;
    try {
      seqNumber = Integer.parseInt(identifier.substring(pt0, pt));
    } catch (NumberFormatException nfe) {
      return null;
    }
    char insertionCode = ' ';
    if (pt < len && identifier.charAt(pt) == '^')
      if (++pt < len)
        insertionCode = identifier.charAt(pt);
    int seqcode = Group.getSeqcode(seqNumber, insertionCode);
    BitSet bsInsert = getSeqcodeBits(seqcode, false);
    if (bsInsert == null) {
      if (insertionCode != ' ')
        bsInsert = getSeqcodeBits(Character.toUpperCase(identifier.charAt(pt)),
            false);
      if (bsInsert == null)
        return null;
      pt++;
    }
    bs.and(bsInsert);
    if (pt >= len)
      return bs;
    //
    // look for a chain spec -- no colon
    //
    char chainID = identifier.charAt(pt++);
    bs.and(getChainBits(chainID));
    if (pt == len)
      return bs;
    //
    // not applicable
    //
    return null;
  }

  private BitSet getSpecName(String name) {
    // * can be used here with ?
    BitSet bs = getSpecNameOrNull(name, false);
    if (bs != null)
      return bs;
    if (name.indexOf("*") > 0)     
      bs = getSpecNameOrNull(name, true);
    return (bs == null ? new BitSet() : bs);
  }

  private BitSet getSpecNameOrNull(String name, boolean checkStar) {
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    BitSet bs = null;
    name = name.toUpperCase();
    if (name.indexOf("\\?") >= 0)
      name = TextFormat.simpleReplace(name, "\\?","\1");
    for (int i = atomCount; --i >= 0;) {
      String g3 = atoms[i].getGroup3(true);
      if (g3 != null && g3.length() > 0) {
        if (TextFormat.isMatch(g3, name, checkStar, true)) {
          if (bs == null)
            bs = new BitSet(i + 1);
          bs.set(i);
          while (--i >= 0 && atoms[i].getGroup3(true).equals(g3))
            bs.set(i);
          i++;
        }
      } else if (isAtomNameMatch(atoms[i], name, checkStar)) {
        if (bs == null)
          bs = new BitSet(i + 1);
        bs.set(i);
      }
    }
    return bs;
  }

  private boolean isAtomNameMatch(Atom atom, String strPattern, boolean checkStar) {
    /// here xx*yy is changed to "xx??????????yy" when coming from getSpecName
    /// but not necessarily when coming from getIdentifierOrNull
    /// and NOT when coming from getAtomBits with Token.spec_atom
    /// because it is presumed that some names can include "*"
    return TextFormat.isMatch(atom.getAtomName().toUpperCase(), strPattern,
        checkStar, false);
  }
  
  protected BitSet getSeqcodeBits(int seqcode, boolean returnEmpty) {
    BitSet bs = new BitSet();
    int seqNum = Group.getSequenceNumber(seqcode);
    boolean haveSeqNumber = (seqNum != Integer.MAX_VALUE);
    boolean isEmpty = true;
    char insCode = Group.getInsertionCode(seqcode);
    switch (insCode) {
    case '?':
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (!haveSeqNumber 
            || seqNum == Group.getSequenceNumber(atomSeqcode)
            && Group.getInsertionCodeValue(atomSeqcode) != 0) {
          bs.set(i);
          isEmpty = false;
        }
      }
      break;
    default:
      for (int i = atomCount; --i >= 0;) {
        int atomSeqcode = atoms[i].getSeqcode();
        if (seqcode == atomSeqcode || 
            !haveSeqNumber && seqcode == Group.getInsertionCodeValue(atomSeqcode) 
            || insCode == '*' && seqNum == Group.getSequenceNumber(atomSeqcode)) {
          bs.set(i);
          isEmpty = false;
        }
      }
    }
    return (!isEmpty || returnEmpty ? bs : null);
  }

  protected BitSet getChainBits(char chain) {
    boolean caseSensitive = viewer.getChainCaseSensitive();
    if (!caseSensitive)
      chain = Character.toUpperCase(chain);
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      char ch = atoms[i].getChainID();
      if (!caseSensitive)
        ch = Character.toUpperCase(ch);
      if (chain == ch)
        bs.set(i);
    }
    return bs;
  }

  public int[] getAtomIndices(BitSet bs) {
    int len = bs.size();
    int n = 0;
    int[] indices = new int[atomCount];
    for (int j = 0; j < len; j++)
      if (bs.get(j))
        indices[j] = ++n;
    return indices;
  }

  public BitSet getAtomsWithin(float distance, Point4f plane) {
    BitSet bsResult = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      float d = Measure.distanceToPlane(plane, atom);
      if (distance > 0 && d >= -0.1 && d <= distance || distance < 0
          && d <= 0.1 && d >= distance || distance == 0 && Math.abs(d) < 0.01)
        bsResult.set(atom.atomIndex);
    }
    return bsResult;
  }
  
  public BitSet getVisibleSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isVisible(0))
        bs.set(i);
    return bs;
  }

  public BitSet getClickableSet() {
    BitSet bs = new BitSet();
    for (int i = atomCount; --i >= 0;)
      if (atoms[i].isClickable())
        bs.set(i);
    return bs;
  }

  protected void deleteModelAtoms(int firstAtomIndex, int nAtoms, BitSet bs) {
    // all atoms in the model are being deleted here
    BitSetUtil.deleteBits(bsHidden, bs);
    BitSetUtil.deleteBits(viewer.getSelectionSet(), bs);
    BitSetUtil.deleteBits(viewer.getSelectionSubset(), bs);
    BitSetUtil.deleteBits(viewer.getFrameOffsets(), bs);
    viewer.setFrameOffsets(viewer.getFrameOffsets());
    atoms = (Atom[]) ArrayUtil.deleteElements(atoms, firstAtomIndex, nAtoms);
    atomCount = atoms.length;
    for (int j = firstAtomIndex; j < atomCount; j++) {
      atoms[j].atomIndex = j;
      atoms[j].modelIndex--;
    }
    //System.out.println("atomcollection deleteAtoms atomslen=" + atoms.length);

    atomNames = (String[]) ArrayUtil.deleteElements(atomNames, firstAtomIndex,
        nAtoms);
    atomTypes = (String[]) ArrayUtil.deleteElements(atomTypes, firstAtomIndex,
        nAtoms);
    atomSerials = (int[]) ArrayUtil.deleteElements(atomSerials, firstAtomIndex,
        nAtoms);
    bfactor100s = (short[]) ArrayUtil.deleteElements(bfactor100s,
        firstAtomIndex, nAtoms);
    hasBfactorRange = false;
    occupancies = (byte[]) ArrayUtil.deleteElements(occupancies,
        firstAtomIndex, nAtoms);
    partialCharges = (float[]) ArrayUtil.deleteElements(partialCharges,
        firstAtomIndex, nAtoms);
    //maybe will not work?
    ellipsoids = (Object[][]) ArrayUtil.deleteElements(ellipsoids,
        firstAtomIndex, nAtoms);
    specialAtomIDs = (byte[]) ArrayUtil.deleteElements(specialAtomIDs,
        firstAtomIndex, nAtoms);
    vibrationVectors = (Vector3f[]) ArrayUtil.deleteElements(vibrationVectors,
        firstAtomIndex, nAtoms);
    clientAtomReferences = (Object[]) ArrayUtil.deleteElements((Object) clientAtomReferences,
        firstAtomIndex, nAtoms);
    nSurfaceAtoms = 0;
    bsSurface = null;
    surfaceDistance100s = null;
    if (tainted != null)
      for (int i = 0; i < TAINT_MAX; i++)
        BitSetUtil.deleteBits(tainted[i], bs);
    // what about data?
  }
}

