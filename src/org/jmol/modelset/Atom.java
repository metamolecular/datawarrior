/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-11 23:55:37 +0200 (mar., 11 août 2009) $
 * $Revision: 11309 $

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

import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;
import org.jmol.api.SymmetryInterface;
import org.jmol.g3d.Graphics3D;
import org.jmol.util.Point3fi;
import org.jmol.util.Quaternion;

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

final public class Atom extends Point3fi {

  private final static byte VIBRATION_VECTOR_FLAG = 1;
  private final static byte IS_HETERO_FLAG = 2;
  private final static byte FLAG_MASK = 3;
  
  public static final int RADIUS_MAX = 16;
  private static final int MAD_MAX = RADIUS_MAX * 2000; // must be < 32000

  Group group;
  int atomIndex;
  BitSet atomSymmetry;
  int atomSite;
  private float userDefinedVanDerWaalRadius;
  
  public int getScreenRadius() {
    return screenDiameter / 2;
  }
  
  public short atomicAndIsotopeNumber;
  private byte formalChargeAndFlags;
  private byte valence;
  char alternateLocationID;
  short madAtom;
  public short getMadAtom() {
    return madAtom;
  }
  
  short colixAtom;
  byte paletteID = JmolConstants.PALETTE_CPK;

  Bond[] bonds;
  int nBondsDisplayed = 0;
  int nBackbonesDisplayed = 0;
  
  public int getNBackbonesDisplayed() {
    return nBackbonesDisplayed;
  }
  
  int clickabilityFlags;
  int shapeVisibilityFlags;
  boolean isSimple = false;
  public boolean isSimple() {
    return isSimple;
  }
  
  public Atom(Point3f pt) {
    //just a point -- just enough to determine a position
    isSimple = true;
    this.x = pt.x; this.y = pt.y; this.z = pt.z;
    //must be transformed later -- Polyhedra;
  }
  
  Atom(Viewer viewer, int modelIndex, int atomIndex,
       BitSet atomSymmetry, int atomSite,
       short atomicAndIsotopeNumber,
       int size, int formalCharge, 
       float x, float y, float z,
       boolean isHetero, char chainID,
       char alternateLocationID,
       float radius) {
    this.modelIndex = (short)modelIndex;
    this.atomSymmetry = atomSymmetry;
    this.atomSite = atomSite;
    this.atomIndex = atomIndex;
    this.atomicAndIsotopeNumber = atomicAndIsotopeNumber;
    if (isHetero)
      formalChargeAndFlags = IS_HETERO_FLAG;
    setFormalCharge(formalCharge);
    this.alternateLocationID = alternateLocationID;
    userDefinedVanDerWaalRadius = radius;
    setMadAtom(viewer, size, Float.NaN);
    set(x, y, z);
  }

  public final void setShapeVisibilityFlags(int flag) {
    shapeVisibilityFlags = flag;
  }

  public final void setShapeVisibility(int shapeVisibilityFlag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= shapeVisibilityFlag;        
    } else {
      shapeVisibilityFlags &=~shapeVisibilityFlag;
    }
  }
  
  public boolean isBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
  }

  public Bond getBond(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(atomOther) != null)
          return bonds[i];
    return null;
  }

  void addDisplayedBond(int stickVisibilityFlag, boolean isVisible){
    nBondsDisplayed+=(isVisible ? 1 : -1);
    setShapeVisibility(stickVisibilityFlag, isVisible);
  } 
  
  public void addDisplayedBackbone(int backboneVisibilityFlag, boolean isVisible){
    nBackbonesDisplayed+=(isVisible ? 1 : -1);
    setShapeVisibility(backboneVisibilityFlag, isVisible);
  }
  
  void deleteBond(Bond bond) {
    //this one is used -- from Bond.deleteAtomReferences
    for (int i = bonds.length; --i >= 0; )
      if (bonds[i] == bond) {
        deleteBond(i);
        return;
      }
  }

  private void deleteBond(int i) {
    int newLength = bonds.length - 1;
    if (newLength == 0) {
      bonds = null;
      return;
    }
    Bond[] bondsNew = new Bond[newLength];
    int j = 0;
    for ( ; j < i; ++j)
      bondsNew[j] = bonds[j];
    for ( ; j < newLength; ++j)
      bondsNew[j] = bonds[j + 1];
    bonds = bondsNew;
  }

  void clearBonds() {
    bonds = null;
  }

  int getBondedAtomIndex(int bondIndex) {
    return bonds[bondIndex].getOtherAtom(this).atomIndex;
  }

  /*
   * What is a MAR?
   *  - just a term that Miguel made up
   *  - an abbreviation for Milli Angstrom Radius
   * that is:
   *  - a *radius* of either a bond or an atom
   *  - in *millis*, or thousandths of an *angstrom*
   *  - stored as a short
   *
   * However! In the case of an atom radius, if the parameter
   * gets passed in as a negative number, then that number
   * represents a percentage of the vdw radius of that atom.
   * This is converted to a normal MAR as soon as possible
   *
   * (I know almost everyone hates bytes & shorts, but I like them ...
   *  gives me some tiny level of type-checking ...
   *  a rudimentary form of enumerations/user-defined primitive types)
   */

  public void setMadAtom(Viewer viewer, int size, float fsize) {
    madAtom = convertEncodedMad(viewer, size, fsize);
  }

  public short convertEncodedMad(Viewer viewer, int size, float fsize) {
    short mad;
    if (Float.isNaN(fsize)) {
      switch (size) {
      case 0:
        return 0;
      case -1000: // temperature
        int diameter = getBfactor100() * 10 * 2;
        if (diameter > 4000)
          diameter = 4000;
        size = diameter;
        break;
      case -1001: // ionic
        size = (getBondingMar() * 2);
        break;
      case -100: // simple van der waals
        size = getVanderwaalsMad(viewer);
      default:
        if (size <= Short.MIN_VALUE) { // ADPMIN
          float d = 2000 * getADPMinMax(false);
          if (size < Short.MIN_VALUE)
            size = (int) (d * (Short.MIN_VALUE - size) / 100f);
          else
            size = (int) d;
          break;
        } else if (size < -2000) {
          // percent of custom size, to diameter
          // -2000 = Jmol, -3000 = Babel, -4000 = RasMol, -5000 = User
          int iMode = (-size / 1000) - 2;
          size = (-size) % 1000;
          size = (int) (size / 50f * viewer.getVanderwaalsMar(
              atomicAndIsotopeNumber % 128, iMode));
        } else if (size < 0) {
          // percent
          // we are going from a radius to a diameter
          size = -size;
          if (size > 200)
            size = 200;
          size = (int) (size / 100f * getVanderwaalsMad(viewer));
        } else if (size >= Short.MAX_VALUE) { // ADPMAX
          float d = 2000 * getADPMinMax(true);
          if (size > Short.MAX_VALUE)
            size = (int) (d * (size - Short.MAX_VALUE) / 100f);
          else
            size = (int) d;
          break;
        } else if (size >= 10000) {
          // radiusAngstroms = vdw + x, where size = (x*2)*1000 + 10000
          // max is SHORT.MAX_VALUE - 1 = 32766
          // so max x is about 11 -- should be plenty!
          // and vdwMar = vdw * 1000
          // we want mad = diameterAngstroms * 1000 = (radiusAngstroms *2)*1000
          // = (vdw * 2 * 1000) + x * 2 * 1000
          // = vdwMar * 2 + (size - 10000)
          size = size - 10000 + getVanderwaalsMad(viewer);
        }
      }
    } else {
      // "size" is a flag == 1 for "VDW + fsize"
      // max is SHORT.MAX_VALUE - 1 = 32766
      // so max storable radius is 16.383. Just call that 16
        size = (int) (fsize * 2000) + (size == 1 ? getVanderwaalsMad(viewer) : 0);
        if (size > MAD_MAX)
          size = MAD_MAX;
    }
    mad = (short) size;
    if (mad < 0)
      mad = 0;
    return mad; 
  }

  public float getADPMinMax(boolean isMax) {
    Object[] ellipsoid = getEllipsoid();
    if (ellipsoid == null)
      return 0;
    return ((float[])ellipsoid[1])[isMax ? 5 : 3];
  }

  public int getRasMolRadius() {
    return Math.abs(madAtom / 8); //  1000r = 1000d / 2; rr = (1000r / 4);
  }

  public int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; )
      if ((bonds[i].order & JmolConstants.BOND_COVALENT_MASK) != 0)
        ++n;
    return n;
  }

  int getCovalentHydrogenCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; )
      if ((bonds[i].order & JmolConstants.BOND_COVALENT_MASK) != 0
          && (bonds[i].getOtherAtom(this).getElementNumber()) == 1)
        ++n;
    return n;
  }

  public Bond[] getBonds() {
    return bonds;
  }

  public void setColixAtom(short colixAtom) {
    this.colixAtom = colixAtom;
  }

  public void setPaletteID(byte paletteID) {
    this.paletteID = paletteID;
  }

  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colixAtom = Graphics3D.getColixTranslucent(colixAtom, isTranslucent, translucentLevel);    
  }

  public boolean isTranslucent() {
    return Graphics3D.isColixTranslucent(colixAtom);
  }

  public short getElementNumber() {
    return (short) (atomicAndIsotopeNumber % 128);
  }
  
  public short getIsotopeNumber() {
    return (short) (atomicAndIsotopeNumber >> 7);
  }
  
  public short getAtomicAndIsotopeNumber() {
    return atomicAndIsotopeNumber;
  }

  public void setAtomicAndIsotopeNumber(int n) {
    if (n < 0 || (n % 128) >= JmolConstants.elementNumberMax || n > Short.MAX_VALUE)
      n = 0;
    atomicAndIsotopeNumber = (short) n;
  }

  public String getElementSymbol(boolean withIsotope) {
    return JmolConstants.elementSymbolFromNumber(withIsotope ? atomicAndIsotopeNumber : atomicAndIsotopeNumber % 128);    
  }
  
  public String getElementSymbol() {
    return getElementSymbol(true);
  }

  public char getAlternateLocationID() {
    return alternateLocationID;
  }
  
  boolean isAlternateLocationMatch(String strPattern) {
    if (strPattern == null)
      return (alternateLocationID == '\0');
    if (strPattern.length() != 1)
      return false;
    char ch = strPattern.charAt(0);
    return (ch == '*' 
        || ch == '?' && alternateLocationID != '\0' 
        || alternateLocationID == ch);
  }

  public boolean isHetero() {
    return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
  }

  void setFormalCharge(int charge) {
    formalChargeAndFlags = (byte)((formalChargeAndFlags & FLAG_MASK) 
        | ((charge == Integer.MIN_VALUE ? 0 : charge > 7 ? 7 : charge < -3 ? -3 : charge) << 2));
  }
  
  void setVibrationVector() {
    formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
  }
  
  public int getFormalCharge() {
    return formalChargeAndFlags >> 2;
  }

  // a percentage value in the range 0-100
  public int getOccupancy100() {
    byte[] occupancies = group.chain.modelSet.occupancies;
    return occupancies == null ? 100 : occupancies[atomIndex];
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  public int getBfactor100() {
    short[] bfactor100s = group.chain.modelSet.bfactor100s;
    if (bfactor100s == null)
      return 0;
    return bfactor100s[atomIndex];
  }

  public boolean setRadius(float radius) {
    return !Float.isNaN(userDefinedVanDerWaalRadius = (radius > 0 ? radius : Float.NaN));  
  }
  
  public void setValence(int nBonds) {
    valence = (byte) (nBonds < 0 ? 0 : nBonds < 0xEF ? nBonds : 0xEF);
  }

  public int getValence() {
    int n = valence;
    if (n == 0 && bonds != null)
      for (int i = bonds.length; --i >= 0;)
        n += bonds[i].getValence();
    return n;
  }

  public float getDimensionValue(int dimension) {
    return (dimension == 0 ? x : (dimension == 1 ? y : z));
  }

  private int getVanderwaalsMad(Viewer viewer) {
    return (Float.isNaN(userDefinedVanDerWaalRadius) 
        ? viewer.getVanderwaalsMar(atomicAndIsotopeNumber % 128) * 2
        : (int)(userDefinedVanDerWaalRadius * 2000f));
  }

  short getBondingMar() {
    return JmolConstants.getBondingMar(atomicAndIsotopeNumber % 128,
        getFormalCharge());
  }

  public float getVanderwaalsRadiusFloat() {
    return (Float.isNaN(userDefinedVanDerWaalRadius) 
        ? group.chain.modelSet.getVanderwaalsMar(atomicAndIsotopeNumber % 128) / 1000f
        : userDefinedVanDerWaalRadius);
  }

  public float getCovalentRadiusFloat() {
    return JmolConstants.getBondingMar(atomicAndIsotopeNumber % 128, 0) / 1000f;
  }

  public float getBondingRadiusFloat() {
    return getBondingMar() / 1000f;
  }

  int getCurrentBondCount() {
    return bonds == null ? 0 : bonds.length;
  }

  public short getColix() {
    return colixAtom;
  }

  public byte getPaletteID() {
    return paletteID;
  }

  public float getRadius() {
    return Math.abs(madAtom / (1000f * 2));
  }

  public int getAtomIndex() {
    return atomIndex;
  }

  public int getAtomSite() {
    return atomSite;
  }

  public BitSet getAtomSymmetry() {
    return atomSymmetry;
  }

   void setGroup(Group group) {
     this.group = group;
   }

   public Group getGroup() {
     return group;
   }
   
   public void transform(Viewer viewer) {
     Point3i screen;
     Vector3f[] vibrationVectors;
     if ((formalChargeAndFlags & VIBRATION_VECTOR_FLAG) == 0 ||
         (vibrationVectors = group.chain.modelSet.vibrationVectors) == null)
       screen = viewer.transformPoint(this);
     else 
       screen = viewer.transformPoint(this, vibrationVectors[atomIndex]);
     screenX = screen.x;
     screenY = screen.y;
     screenZ = screen.z;
     screenDiameter = viewer.scaleToScreen(screenZ, Math.abs(madAtom));
   }

   // note: atomName cannot be null
   // note: atomNames cannot be null
   
   public String getAtomName() {
     return group.chain.modelSet.atomNames[atomIndex];
   }
   
   public String getAtomType() {
    String[] atomTypes = group.chain.modelSet.atomTypes;
    String type = (atomTypes == null ? null : atomTypes[atomIndex]);
    return (type == null ? group.chain.modelSet.atomNames[atomIndex] : type);
  }
   
   public int getAtomNumber() {
     int[] atomSerials = group.chain.modelSet.atomSerials;
     return (atomSerials != null ? atomSerials[atomIndex] : atomIndex);
//        : group.chain.modelSet.isZeroBased ? atomIndex : atomIndex);
   }

   public boolean isInFrame() {
     return ((shapeVisibilityFlags & JmolConstants.ATOM_IN_FRAME) != 0);
   }

   public int getShapeVisibilityFlags() {
     return shapeVisibilityFlags;
   }
   
   public boolean isShapeVisible(int shapeVisibilityFlag) {
     return ((shapeVisibilityFlags & shapeVisibilityFlag) != 0);
   }

   public float getPartialCharge() {
     float[] partialCharges = group.chain.modelSet.partialCharges;
     return partialCharges == null ? 0 : partialCharges[atomIndex];
   }

   public float getStraightness() {
     return group.getStraightness();
   }

   public Object[] getEllipsoid() {
     return group.chain.modelSet.getEllipsoid(atomIndex);
   }

   /**
    * Given a symmetry operation number, the set of cells in the model, and the
    * number of operations, this method returns either 0 or the cell number (555, 666)
    * of the translated symmetry operation corresponding to this atom.
    * 
    * atomSymmetry is a bitset that is created in adapter.smarter.AtomSetCollection
    * 
    * It is arranged as follows:
    * 
    * |--overall--|---cell1---|---cell2---|---cell3---|...
    * 
    * |012..nOps-1|012..nOps-1|012..nOp-1s|012..nOps-1|...
    * 
    * If a bit is set, it means that the atom was created using that operator
    * operating on the base file set and translated for that cell.
    * 
    * If any bit is set in any of the cell blocks, then the same
    * bit will also be set in the overall block. This allows for
    * rapid determination of special positions and also of
    * atom membership in any operation set.
    * 
    *  Note that it is not necessarily true that an atom is IN the designated
    *  cell, because one can load {nnn mmm 0}, and then, for example, the {-x,-y,-z}
    *  operator sends atoms from 555 to 444. Still, those atoms would be marked as
    *  cell 555 here, because no translation was carried out. 
    *  
    *  That is, the numbers 444 in symop=3444 do not refer to a cell, per se. 
    *  What they refer to is the file-designated operator plus a translation of
    *  {-1 -1 -1/1}. 
    * 
    * @param symop        = 0, 1, 2, 3, ....
    * @param cellRange    = {444, 445, 446, 454, 455, 456, .... }
    * @param nOps         = 2 for x,y,z;-x,-y,-z, for example
    * @return cell number such as 565
    */
   public int getSymmetryTranslation(int symop, int[] cellRange, int nOps) {
     int pt = symop;
     for (int i = 0; i < cellRange.length; i++)
       if (atomSymmetry.get(pt += nOps))
         return cellRange[i];
     return 0;
   }
   
   /**
    * Looks for a match in the cellRange list for this atom within the specified translation set
    * select symop=0NNN for this
    * 
    * @param cellNNN
    * @param cellRange
    * @param nOps
    * @return     matching cell number, if applicable
    */
   public int getCellTranslation(int cellNNN, int[] cellRange, int nOps) {
     int pt = nOps;
     for (int i = 0; i < cellRange.length; i++)
       for (int j = 0; j < nOps;j++, pt++)
       if (atomSymmetry.get(pt) && cellRange[i] == cellNNN)
         return cellRange[i];
     return 0;
   }
   
   String getSymmetryOperatorList() {
    String str = "";
    ModelSet f = group.chain.modelSet;
    if (atomSymmetry == null || f.unitCells == null
        || f.unitCells[modelIndex] == null)
      return "";
    int[] cellRange = f.getModelCellRange(modelIndex);
    if (cellRange == null)
      return "";
    int nOps = f.getModelSymmetryCount(modelIndex);
    int pt = nOps;
    for (int i = 0; i < cellRange.length; i++)
      for (int j = 0; j < nOps; j++)
        if (atomSymmetry.get(pt++))
          str += "," + (j + 1) + "" + cellRange[i];
    return str.substring(1);
  }
   
   public int getModelIndex() {
     return modelIndex;
   }
   
   public int getMoleculeNumber() {
     return (group.chain.modelSet.getMoleculeIndex(atomIndex) + 1);
   }
   
   String getClientAtomStringProperty(String propertyName) {
     Object[] clientAtomReferences = group.chain.modelSet.clientAtomReferences;
     return
       ((clientAtomReferences==null || clientAtomReferences.length<=atomIndex)
        ? null : (group.chain.modelSet.viewer.
           getClientAtomStringProperty(clientAtomReferences[atomIndex],
                                       propertyName)));
   }

   public byte getSpecialAtomID() {
     byte[] specialAtomIDs = group.chain.modelSet.specialAtomIDs;
     return specialAtomIDs == null ? 0 : specialAtomIDs[atomIndex];
   }
   
  public float getFractionalCoord(char ch) {
    Point3f pt = getFractionalCoord();
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }
    
  public float getFractionalUnitCoord(char ch) {
    Point3f pt = getFractionalUnitCoord(false);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }

  public Point3f getFractionalCoord() {
    SymmetryInterface[] c = group.chain.modelSet.unitCells;
    Point3f pt = new Point3f(this);
    if (c != null)
      c[modelIndex].toFractional(pt);
    return pt;
  }
  
  public Point3f getFractionalUnitCoord(boolean asCartesian) {
    SymmetryInterface[] c = group.chain.modelSet.unitCells;
    Point3f pt = new Point3f(this);
    if (c != null) {
      c[modelIndex].toUnitCell(pt, null);
      if (!asCartesian)
        c[modelIndex].toFractional(pt);
    }
    return pt;
  }
  
  public float getFractionalUnitDistance(Point3f pt, Point3f ptTemp1, Point3f ptTemp2) {
    SymmetryInterface[] c = group.chain.modelSet.unitCells;
    if (c == null) 
      return distance(pt);
    ptTemp1.set(this);
    c[modelIndex].toUnitCell(ptTemp1, null);
    ptTemp2.set(pt);
    c[modelIndex].toUnitCell(ptTemp2, null);
    return ptTemp1.distance(ptTemp2);
  }
  
  void setFractionalCoord(int tok, float fValue) {
    SymmetryInterface[] c = group.chain.modelSet.unitCells;
    if (c != null)
      c[modelIndex].toFractional(this);
    switch (tok) {
    case Token.fracX:
      x = fValue;
      break;
    case Token.fracY:
      y = fValue;
      break;
    case Token.fracZ:
      z = fValue;
      break;
    }
    if (c != null)
      c[modelIndex].toCartesian(this);
  }
  
  void setFractionalCoord(Point3f ptNew) {
    set(ptNew);
    SymmetryInterface[] c = group.chain.modelSet.unitCells;
    if (c != null)
      c[modelIndex].toCartesian(this);
  }
  
  boolean isCursorOnTopOf(int xCursor, int yCursor,
                        int minRadius, Atom competitor) {
    int r = screenDiameter / 2;
    if (r < minRadius)
      r = minRadius;
    int r2 = r * r;
    int dx = screenX - xCursor;
    int dx2 = dx * dx;
    if (dx2 > r2)
      return false;
    int dy = screenY - yCursor;
    int dy2 = dy * dy;
    int dz2 = r2 - (dx2 + dy2);
    if (dz2 < 0)
      return false;
    if (competitor == null)
      return true;
    int z = screenZ;
    int zCompetitor = competitor.screenZ;
    int rCompetitor = competitor.screenDiameter / 2;
    if (z < zCompetitor - rCompetitor)
      return true;
    int dxCompetitor = competitor.screenX - xCursor;
    int dx2Competitor = dxCompetitor * dxCompetitor;
    int dyCompetitor = competitor.screenY - yCursor;
    int dy2Competitor = dyCompetitor * dyCompetitor;
    int r2Competitor = rCompetitor * rCompetitor;
    int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
    return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
  }

  /*
   *  DEVELOPER NOTE (BH):
   *  
   *  The following methods may not return 
   *  correct values until after modelSet.finalizeGroupBuild()
   *  
   */
   
  public String getInfo() {
    return getIdentity(true);
  } 

  String getInfoXYZ(boolean useChimeFormat) {
    if (useChimeFormat) {
      String group3 = getGroup3(true);
      char chainID = getChainID();
      Point3f pt = (group.chain.modelSet.unitCells == null ? null : getFractionalCoord());
      return "Atom: " + (group3 == null ? getElementSymbol() : getAtomName()) + " " + getAtomNumber() 
          + (group3 != null && group3.length() > 0 ? 
              (isHetero() ? " Hetero: " : " Group: ") + group3 + " " + getResno() 
              + (chainID != 0 && chainID != ' ' ? " Chain: " + chainID : "")              
              : "")
          + " Model: " + getModelNumber()
          + " Coordinates: " + x + " " + y + " " + z
          + (pt == null ? "" : " Fractional: "  + pt.x + " " + pt.y + " " + pt.z); 
    }
    return getIdentity(true) + " " + x + " " + y + " " + z;
  }

  String getIdentityXYZ() {
    return getIdentity(false) + " " + x + " " + y + " " + z;
  }
  
  String getIdentity(boolean allInfo) {
    StringBuffer info = new StringBuffer();
    String group3 = getGroup3(true);
    String seqcodeString = getSeqcodeString();
    char chainID = getChainID();
    if (group3 != null && group3.length() > 0) {
      info.append("[");
      info.append(group3);
      info.append("]");
    }
    if (seqcodeString != null)
      info.append(seqcodeString);
    if (chainID != 0 && chainID != ' ') {
      info.append(":");
      info.append(chainID);
    }
    if (!allInfo)
      return info.toString();
    if (info.length() > 0)
      info.append(".");
    info.append(getAtomName());
    if (info.length() == 0) {
      // since atomName cannot be null, this is unreachable
      info.append(getElementSymbol(false));
      info.append(" ");
      info.append(getAtomNumber());
    }
    if (alternateLocationID != 0) {
      info.append("%");
      info.append(alternateLocationID);
    }
    if (group.chain.modelSet.getModelCount() > 1) {
      info.append("/");
      info.append(getModelNumberForLabel());
    }
    info.append(" #");
    info.append(getAtomNumber());
    return info.toString();
  }

  public int getGroupIndex() {
    return group.getGroupIndex();
  }
  
  public String getGroup3(boolean allowNull) {
    String group3 = group.getGroup3();
    return (allowNull || group3 != null || group3.length() > 0 ? group3 : "UNK");
  }

  public String getGroup1(char c0) {
    char c = group.getGroup1();
    return (c != '\0' ? "" + c : c0 != '\0' ? "" + c0 : "");
  }

  boolean isGroup3(String group3) {
    return group.isGroup3(group3);
  }

  boolean isProtein() {
    return group.isProtein();
  }

  boolean isCarbohydrate() {
    return group.isCarbohydrate();
  }

  boolean isNucleic() {
    return group.isNucleic();
  }

  boolean isDna() {
    return group.isDna();
  }
  
  boolean isRna() {
    return group.isRna();
  }

  boolean isPurine() {
    return group.isPurine();
  }

  boolean isPyrimidine() {
    return group.isPyrimidine();
  }

  int getSeqcode() {
    return group.getSeqcode();
  }

  public int getResno() {
    return group.getResno();   
  }

  public boolean isClickable() {
    // certainly if it is not visible, then it can't be clickable
    if (!isVisible(0))
      return false;
    int flags = shapeVisibilityFlags | group.shapeVisibilityFlags;
    return ((flags & clickabilityFlags) != 0);
  }

  public int getClickabilityFlags() {
    return clickabilityFlags;
  }
  
  public void setClickable(int flag) {
    if (flag == 0)
      clickabilityFlags = 0;
    else
      clickabilityFlags |= flag;
  }
  
  /**
   * determine if an atom or its PDB group is visible
   * @param flags TODO
   * @return true if the atom is in the "select visible" set
   */
  public boolean isVisible(int flags) {
    // Is the atom's model visible? Is the atom NOT hidden?
    if (!isInFrame() || group.chain.modelSet.isAtomHidden(atomIndex))
      return false;
    // Is any shape associated with this atom visible? 
    if (flags != 0)
      return (isShapeVisible(flags));  
    flags = shapeVisibilityFlags;
    // Is its PDB group visible in any way (cartoon, e.g.)?
    //  An atom is considered visible if its PDB group is visible, even
    //  if it does not show up itself as part of the structure
    //  (this will be a difference in terms of *clickability*).
    flags |= group.shapeVisibilityFlags;
    // We know that (flags & AIM), so now we must remove that flag
    // and check to see if any others are remaining.
    // Only then is the atom considered visible.
    return ((flags & ~JmolConstants.ATOM_IN_FRAME) != 0);
  }

  public float getGroupPhi() {
    return group.phi;
  }

  public float getGroupPsi() {
    return group.psi;
  }

  public char getChainID() {
    return group.chain.chainID;
  }

  public int getSurfaceDistance100() {
    return group.chain.modelSet.getSurfaceDistance100(atomIndex);
  }

  public Vector3f getVibrationVector() {
    return group.chain.modelSet.getVibrationVector(atomIndex, false);
  }

  public float getVibrationCoord(char ch) {
    Point3f pt = getFractionalUnitCoord(false);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }


  public int getPolymerLength() {
    return group.getBioPolymerLength();
  }

  public Quaternion getQuaternion(char qtype) {
    return group.getQuaternion(qtype);
  }
  
  int getPolymerIndex() {
    return group.getBioPolymerIndex();
  }

  public int getSelectedGroupCountWithinChain() {
    return group.chain.getSelectedGroupCount();
  }

  public int getSelectedGroupIndexWithinChain() {
    return group.getSelectedGroupIndex();
  }

  public int getSelectedMonomerCountWithinPolymer() {
    return group.getSelectedMonomerCount();
  }

  public int getSelectedMonomerIndexWithinPolymer() {
    return group.getSelectedMonomerIndex();
  }

  Chain getChain() {
    return group.chain;
  }

  String getModelNumberForLabel() {
    return group.chain.modelSet.getModelNumberForAtomLabel(modelIndex);
  }
  
  public int getModelNumber() {
    return group.chain.modelSet.getModelNumber(modelIndex) % 1000000;
  }
  
  public int getModelFileIndex() {
    return group.chain.model.fileIndex;
  }
  
  public int getModelFileNumber() {
    return group.chain.modelSet.getModelFileNumber(modelIndex);
  }
  
  public byte getProteinStructureType() {
    return group.getProteinStructureType();
  }
  
  public int getStrucNo() {
    return group.getStrucNo();
  }

  public String getStructureId() {
    return group.getStructureId();
  }

  public String getProteinStructureTag() {
    return group.getProteinStructureTag();
  }

  public short getGroupID() {
    return group.groupID;
  }

  String getSeqcodeString() {
    return group.getSeqcodeString();
  }

  int getSeqNumber() {
    return group.getSeqNumber();
  }

  public char getInsertionCode() {
    return group.getInsertionCode();
  }
  
  public boolean equals(Object obj) {
    return (this == obj);
  }

  public int hashCode() {
    //this overrides the Point3fi hashcode, which would
    //give a different hashcode for an atom depending upon
    //its screen location! Bug fix for 11.1.43 Bob Hanson
    return atomIndex;
  }
  
  public Atom findAromaticNeighbor(BitSet notAtoms) {
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      Atom a = bondT.getOtherAtom(this);
      if (bondT.isAromatic() && (notAtoms == null || !notAtoms.get(a.atomIndex)))
        return a;
    }
    return null;
  }

  public Atom findAromaticNeighbor(int notAtomIndex) {
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      Atom a = bondT.getOtherAtom(this);
      if (bondT.isAromatic() && a.atomIndex != notAtomIndex)
        return a;
    }
    return null;
  }

  /**
   * called by isosurface and int comparator via atomProperty()
   * and also by getBitsetProperty() 
   * 
   * @param atom
   * @param tokWhat
   * @return         int value or Integer.MIN_VALUE
   */
  public static int atomPropertyInt(Atom atom, int tokWhat) {
    switch (tokWhat) {
    case Token.atomno:
      return atom.getAtomNumber();
    case Token.atomID:
      return atom.getSpecialAtomID();
    case Token.atomIndex:
      return atom.getAtomIndex();
    case Token.bondcount:
      return atom.getCovalentBondCount();
    case Token.color:
      return atom.group.chain.modelSet.viewer.getColixArgb(atom.getColix());
    case Token.element:
    case Token.elemno:
      return atom.getElementNumber();
    case Token.file:
      return atom.getModelFileIndex() + 1;
    case Token.formalCharge:
      return atom.getFormalCharge();
    case Token.groupID:
      return atom.getGroupID(); //-1 if no group
    case Token.groupindex:
      return atom.getGroupIndex(); 
    case Token.model:
      //integer model number -- could be PDB/sequential adapter number
      //or it could be a sequential model in file number when multiple files
      return atom.getModelNumber();
    case -Token.model:
      //float is handled differently
      return atom.getModelFileNumber();
    case Token.modelindex:
      return atom.modelIndex;
    case Token.molecule:
      return atom.getMoleculeNumber();
    case Token.occupancy:
      return atom.getOccupancy100();
    case Token.polymerLength:
      return atom.getPolymerLength();
    case Token.radius:
      // the comparitor uses rasmol radius, unfortunately, for integers
      return atom.getRasMolRadius();        
    case Token.resno:
      return atom.getResno();
    case Token.site:
      return atom.getAtomSite();
    case Token.structure:
      return atom.getProteinStructureType();
    case Token.strucno:
      return atom.getStrucNo();
    case Token.valence:
      return atom.getValence();
    }
    return 0;      
  }

  /**
   * called by isosurface and int comparator via atomProperty()
   * and also by getBitsetProperty() 
   * 
   * @param atom
   * @param tokWhat
   * @return       float value or value*100 (asInt=true) or throw an error if not found
   * 
   */  
  public static float atomPropertyFloat(Atom atom, int tokWhat) {

    switch (tokWhat) {
    case Token.radius:
      return atom.getRadius();
      
    case Token.surfacedistance:
      atom.group.chain.modelSet.getSurfaceDistanceMax();
      return atom.getSurfaceDistance100() / 100f;
    case Token.temperature: // 0 - 9999
      return atom.getBfactor100() / 100f;

    // these next have to be multiplied by 100 if being compared
    // note that spacefill here is slightly different than radius -- no integer option
      
    case Token.adpmax:
      return atom.getADPMinMax(true);
    case Token.adpmin:
      return atom.getADPMinMax(false);
    case Token.atomX:
      return atom.x;
    case Token.atomY:
      return atom.y;
    case Token.atomZ:
      return atom.z;
    case Token.covalent:
      return atom.getCovalentRadiusFloat();
    case Token.fracX:
      return atom.getFractionalCoord('X');
    case Token.fracY:
      return atom.getFractionalCoord('Y');
    case Token.fracZ:
      return atom.getFractionalCoord('Z');
    case Token.ionic:
      return atom.getBondingRadiusFloat();
    case Token.occupancy:
      return atom.getOccupancy100() / 100f;
    case Token.partialCharge:
      return atom.getPartialCharge();
    case Token.phi:
      return atom.getGroupPhi();
    case Token.psi:
      return atom.getGroupPsi();
    case Token.spacefill:
      return atom.getRadius();
    case Token.straightness:
      return atom.getStraightness();
    case Token.unitX:
      return atom.getFractionalUnitCoord('X');
    case Token.unitY:
      return atom.getFractionalUnitCoord('Y');
    case Token.unitZ:
      return atom.getFractionalUnitCoord('Z');
    case Token.vanderwaals:
      return atom.getVanderwaalsRadiusFloat();
    case Token.vibX:
      return atom.getVibrationCoord('X');
    case Token.vibY:
      return atom.getVibrationCoord('Y');
    case Token.vibZ:
      return atom.getVibrationCoord('Z');
    }
    return atomPropertyInt(atom, tokWhat);
  }

  public static String atomPropertyString(Atom atom, int tokWhat) {
    char ch;
    switch (tokWhat) {
    case Token.altloc:
      ch = atom.getAlternateLocationID();
      return (ch == '\0' ? "" : "" + ch);
    case Token.atomName:
      return atom.getAtomName();
    case Token.atomType:
      return atom.getAtomType();
    case Token.chain:
      ch = atom.getChainID();
      return (ch == '\0' ? "" : "" + ch);
    case Token.sequence:
      return atom.getGroup1('?');
    case Token.group1:
      return atom.getGroup1('\0');
    case Token.group:
      return atom.getGroup3(false);
    case Token.element:
      return atom.getElementSymbol(true);
    case Token.identify:
      return atom.getIdentity(true);
    case Token.insertion:
      ch = atom.getInsertionCode();
      return (ch == '\0' ? "" : "" + ch);
    case Token.label:
    case Token.format:
      String s = atom.group.chain.modelSet.getAtomLabel(atom.getAtomIndex());
      if (s == null)
        s = "";
      return s;
    case Token.structure:
      return JmolConstants.getProteinStructureName(atom.getProteinStructureType());
    case Token.strucid:
      return atom.getStructureId();
    case Token.symbol:
      return atom.getElementSymbol(false);
    case Token.symmetry:
      return atom.getSymmetryOperatorList();
    }
    return ""; 
  }

  public static Tuple3f atomPropertyTuple(Atom atom, int tok) {
    switch (tok) {
    case Token.fracXyz:
      return atom.getFractionalCoord();
    case Token.unitXyz:
      return atom.getFractionalUnitCoord(false);
    case Token.vibXyz:
      Vector3f v = atom.getVibrationVector();
      if (v == null)
        v = new Vector3f();
      return v;
    case Token.xyz:
      return atom;
    case Token.color:
      return Graphics3D.colorPointFromInt2(
          atom.group.chain.modelSet.viewer.getColixArgb(atom.getColix())
          );
    }
    return null;
  }

  boolean isWithinStructure(byte type) {
    return group.isWithinStructure(type);
  }

  /* DEVELOPER NOTE -- ATOM/MODEL DELETION --
   * 
   * The challenge of atom deletion:
   * 
   * Many data structures involve reference to Atom, atomIndex, Model, or modelIndex
   * A first-pass list includes:

org.jmol.modelset
-----------------

Atom.atomIndex
Atom.modelIndex
Bond.atom1
Bond.atom2
Chain.model
Group.firstAtomIndex
Group.lastAtomIndex
Model.modelIndex
Model.fileIndex
Model.firstAtomIndex
Model.firstMolecule
Model.chains
Model.bioPolymers
Model.auxiliaryInfo

AtomCollection.atoms
AtomCollection.atomCount
AtomCollection.atomNames
AtomCollection.atomSerials
AtomCollection.bfactor100s
AtomCollection.bspf
AtomCollection.bsHidden
AtomCollection.bsSurface
AtomCollection.nSurfaceAtoms
AtomCollection.clientAtomReferences
AtomCollection.hasBfactorRange   -- set false
AtomCollection.occupancies
AtomCollection.partialCharges
AtomCollection.specialAtomIDs
AtomCollection.surfaceDistance100s -- set null
AtomCollection.tainted
AtomCollection.vibrationVectors

BondCollection.bonds
BondCollection.bondCount

ModelCollection.averageAtomPoint
ModelCollection.bboxModels
ModelCollection.bboxAtoms
ModelCollection.boxInfo

ModelCollection.modelNumbers
ModelCollection.models
ModelCollection.modelSetAuxiliaryInfo["group3Lists", "group3Counts, "models"]
ModelCollection.molecules -- just set null
ModelCollection.moleculeCount
ModelCollection.stateScripts ?????
ModelCollection.thisStateModel  -- just set -1
ModelCollection.structures
ModelCollection.structureCount

ModelSet.shapes  (many of these hold references that would need adjusting)

CellInfo.modelIndex
Measurement.countPlusIndices
MeasurementPending.countPlusIndices
Polymer.leadAtomIndices -- can be set null in BioPolymer.recalculateLeadMidpointsAndWingVectors()

org.jmol.modelsetbio
--------------------

org.jmol.popup
--------------
 [ would need updating ]

org.jmol.shape
--------------

AtomShape.mads
AtomShape.colixes
AtomShape.paletteIDs
AtomShape.bsSizeSet
AtomShape.bsColixSet
AtomShape.atomCount
AtomShape.atoms

Dots?

Labels.strings
Labels.formats
Labels.bgcolixes
Labels.fids
Labels.offsets
Measures.measurements

Mesh.title (sometimes model-based?)
Mesh.atomIndex
Mesh.modelIndex
Mesh.modelFlags
MeshCollection.meshes
MeshCollection.modelCount
MeshCollection.title ?

Sticks.bsOrderSet
Sticks.bsSizeSet
Sticks.bsColixSet
Sticks.selectedBonds

TextShape.modelIndex

org.jmol.shapebio
-----------------

BioShape.modelIndex
BioShape.leadAtomIndices
BioShapeCollection.atoms
BioShapeRenderer -- all need to be set null

org.jmol.shapespecial
---------------------

Dipole.modelIndex
Dipole.atoms
DrawMesh.drawTypes
DrawMesh.ptCenters
DrawMesh.drawVertexCount
DrawMesh.drawVertexCounts
Draw.modelCount
MolecularOrbital.htModels
Polyhedra.Polyhedrons
Polyhedra.Polyhedron.centralAtom

org.jmol.viewer
---------------

SelectionManager.bsHidden
SelectionManager.bsSelection
SelectionManager.bsSubset
Eval.bsSubset

org.openscience.jmol.app
------------------------

AtomSetChooser ??


   * 
   */
}
