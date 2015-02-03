/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-05-28 21:23:41 +0200 (jeu., 28 mai 2009) $
 * $Revision: 10912 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
package org.jmol.viewer;

import org.jmol.util.ArrayUtil;
import org.jmol.util.Logger;
import java.util.BitSet;
import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.HBond;
import org.jmol.modelset.ModelSet;
import org.jmol.util.ColorEncoder;

class ColorManager {

  private Viewer viewer;
  private Graphics3D g3d;
  private int[] argbsCpk;
  private int[] altArgbsCpk;
  private float colorHi, colorLo;
  private float[] colorData;  
  private int palette = 0;
  

  ColorManager(Viewer viewer, Graphics3D g3d) {
    this.viewer = viewer;
    this.g3d = g3d;
    argbsCpk = JmolConstants.argbsCpk;
    altArgbsCpk = ArrayUtil.arrayCopy(JmolConstants.altArgbsCpk, 0, -1, false);
  }

  void clear() {
    //causes problems? flushCaches();
  }
  
  private boolean isDefaultColorRasmol;
  boolean getDefaultColorRasmol() {
    return isDefaultColorRasmol;
  }

  void resetElementColors() {
    setDefaultColors("Jmol");
  }
  
  void setDefaultColors(String colorScheme) {
    if (colorScheme.equalsIgnoreCase("Jmol")) {
      isDefaultColorRasmol = false;
      argbsCpk = JmolConstants.argbsCpk;
    } else if (colorScheme.equalsIgnoreCase("RasMol")) {
      isDefaultColorRasmol = true;
      argbsCpk = ColorEncoder.getRasmolScale(true);
    } else {
      Logger.error("unrecognized color scheme");
      return;
    }
    altArgbsCpk = ArrayUtil.arrayCopy(JmolConstants.altArgbsCpk, 0, -1, false);
    ColorEncoder.makeColorScheme(colorScheme, null, true);
    for (int i = JmolConstants.argbsCpk.length; --i >= 0; )
      g3d.changeColixArgb((short)i, argbsCpk[i]);
    for (int i = JmolConstants.altArgbsCpk.length; --i >= 0; )
      g3d.changeColixArgb((short)(JmolConstants.elementNumberMax + i), altArgbsCpk[i]);
  }

  short colixRubberband = Graphics3D.HOTPINK;
  void setRubberbandArgb(int argb) {
    colixRubberband = (argb == 0 ? 0 : Graphics3D.getColix(argb));
  }

  /*
   * black or white, whichever contrasts more with the current background
   *
   *
   * @return black or white colix value
   */
  short colixBackgroundContrast;
  void setColixBackgroundContrast(int argb) {
    colixBackgroundContrast =
      ((Graphics3D.calcGreyscaleRgbFromRgb(argb) & 0xFF) < 128
       ? Graphics3D.WHITE : Graphics3D.BLACK);
  }

  short getColixBondPalette(Bond bond, byte pid) {
    int argb = 0;
    switch (pid) {
    case JmolConstants.PALETTE_ENERGY:
      return ColorEncoder.getColorIndexFromPalette(((HBond)bond).getEnergy(), 
          0.5f, 4.5f, ColorEncoder.BWR);
    }
    return (argb == 0 ? Graphics3D.RED : Graphics3D.getColix(argb));
  }
  
  short getColixAtomPalette(Atom atom, byte pid) {
    int argb = 0;
    int index;
    short id;
    ModelSet modelSet;
    int modelIndex;
    float lo, hi;
    switch (pid) {
    case JmolConstants.PALETTE_PROPERTY:
      return getPropertyColix(atom.getAtomIndex());
    case JmolConstants.PALETTE_NONE:
    case JmolConstants.PALETTE_CPK:
      // Note that CPK colors can be changed based upon user preference
      // therefore, a changable colix is allocated in this case
      id = atom.getAtomicAndIsotopeNumber();
      if (id < JmolConstants.elementNumberMax)
        return g3d.getChangeableColix(id, argbsCpk[id]);
      id = (short) JmolConstants.altElementIndexFromNumber(id);
      return g3d.getChangeableColix(
          (short) (JmolConstants.elementNumberMax + id), altArgbsCpk[id]);
    case JmolConstants.PALETTE_PARTIAL_CHARGE:
      // This code assumes that the range of partial charges is [-1, 1].
      index = ColorEncoder.quantize(atom.getPartialCharge(), 
          -1, 1, JmolConstants.PARTIAL_CHARGE_RANGE_SIZE);
      return g3d.getChangeableColix(
          (short) (JmolConstants.PARTIAL_CHARGE_COLIX_RED + index),
          JmolConstants.argbsRwbScale[index]);
    case JmolConstants.PALETTE_FORMAL_CHARGE:
      index = atom.getFormalCharge() - JmolConstants.FORMAL_CHARGE_MIN;
      return g3d.getChangeableColix(
          (short) (JmolConstants.FORMAL_CHARGE_COLIX_RED + index),
          JmolConstants.argbsFormalCharge[index]);
    case JmolConstants.PALETTE_TEMP:
    case JmolConstants.PALETTE_FIXEDTEMP:
      if (pid == JmolConstants.PALETTE_TEMP) {
        modelSet = viewer.getModelSet();
        lo = modelSet.getBfactor100Lo();
        hi = modelSet.getBfactor100Hi();
      } else {
        lo = 0;
        hi = 100 * 100; // scaled by 100
      }
      return ColorEncoder.getColorIndexFromPalette(atom.getBfactor100(), 
          lo, hi, ColorEncoder.BWR);
    case JmolConstants.PALETTE_STRAIGHTNESS:
      return ColorEncoder.getColorIndexFromPalette(atom.getStraightness(), 
          -1, 1, ColorEncoder.BWR);
    case JmolConstants.PALETTE_SURFACE:
      hi = viewer.getSurfaceDistanceMax();
      return ColorEncoder.getColorIndexFromPalette(atom.getSurfaceDistance100(), 
          0, hi, ColorEncoder.BWR);
    case JmolConstants.PALETTE_AMINO:
      return ColorEncoder.getColorIndexFromPalette(atom
          .getGroupID(), 0, 0, ColorEncoder.AMINO);
    case JmolConstants.PALETTE_SHAPELY:
      return ColorEncoder.getColorIndexFromPalette(atom
          .getGroupID(), 0, 0, ColorEncoder.SHAPELY);
    case JmolConstants.PALETTE_GROUP:
      // viewer.calcSelectedGroupsCount() must be called first ...
      // before we call getSelectedGroupCountWithinChain()
      // or getSelectedGropuIndexWithinChain
      // however, do not call it here because it will get recalculated
      // for each atom
      // therefore, we call it in Eval.colorObject();
      return ColorEncoder.getColorIndexFromPalette(
          atom.getSelectedGroupIndexWithinChain(), 0,
          atom.getSelectedGroupCountWithinChain() - 1,
          ColorEncoder.BGYOR);
    case JmolConstants.PALETTE_MONOMER:
      // viewer.calcSelectedMonomersCount() must be called first ...
      return ColorEncoder.getColorIndexFromPalette(
          atom.getSelectedMonomerIndexWithinPolymer(), 
          0, atom.getSelectedMonomerCountWithinPolymer() - 1,
          ColorEncoder.BGYOR);
    case JmolConstants.PALETTE_MOLECULE:
      modelSet = viewer.getModelSet();
      return ColorEncoder.getColorIndexFromPalette(
          modelSet.getMoleculeIndex(atom.getAtomIndex()), 
          0, modelSet.getMoleculeCountInModel(atom.getModelIndex()) - 1, 
          ColorEncoder.ROYGB);
    case JmolConstants.PALETTE_ALTLOC:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      return ColorEncoder.getColorIndexFromPalette(
          modelSet.getAltLocIndexInModel(modelIndex,
          atom.getAlternateLocationID()), 
          0, modelSet.getAltLocCountInModel(modelIndex),
          ColorEncoder.ROYGB);
    case JmolConstants.PALETTE_INSERTION:
      modelSet = viewer.getModelSet();
      //very inefficient!
      modelIndex = atom.getModelIndex();
      return ColorEncoder.getColorIndexFromPalette(
          modelSet.getInsertionCodeIndexInModel(
          modelIndex, atom.getInsertionCode()), 
          0, modelSet.getInsertionCountInModel(modelIndex),
          ColorEncoder.ROYGB);
    case JmolConstants.PALETTE_JMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, Token.jmol);
      break;
    case JmolConstants.PALETTE_RASMOL:
      id = atom.getAtomicAndIsotopeNumber();
      argb = getJmolOrRasmolArgb(id, Token.rasmol);
      break;
    case JmolConstants.PALETTE_STRUCTURE:
      argb = JmolConstants.argbsStructure[atom.getProteinStructureType()];
      break;
    case JmolConstants.PALETTE_CHAIN:
      int chain = atom.getChainID() & 0x1F;
      if (chain < 0)
        chain = 0;
      if (chain >= JmolConstants.argbsChainAtom.length)
        chain = chain % JmolConstants.argbsChainAtom.length;
      argb = (atom.isHetero() ? JmolConstants.argbsChainHetero
          : JmolConstants.argbsChainAtom)[chain];
      break;
    }
    return (argb == 0 ? Graphics3D.HOTPINK : Graphics3D.getColix(argb));
  }

  private short getPropertyColix(int iAtom) {
    if (colorData == null || iAtom >= colorData.length)
      return Graphics3D.GRAY;
    return getColixForPropertyValue(colorData[iAtom]);    
  }

  private static int getJmolOrRasmolArgb(int id, int argb) {
    switch (argb) {
    case Token.jmol:
      if (id >= JmolConstants.elementNumberMax)
        break;
      return ColorEncoder.getArgbFromPalette(id, 0, 0, ColorEncoder.JMOL);
    case Token.rasmol:
      if (id >= JmolConstants.elementNumberMax)
        break;
      return ColorEncoder.getArgbFromPalette(id, 0, 0, ColorEncoder.RASMOL);
    default:
      return argb;
    }
    return JmolConstants.altArgbsCpk[JmolConstants
        .altElementIndexFromNumber(id)];
  }

  void setElementArgb(int id, int argb) {
    if (argb == Token.jmol && argbsCpk == JmolConstants.argbsCpk)
      return;
    argb = getJmolOrRasmolArgb(id, argb);
    if (argbsCpk == JmolConstants.argbsCpk) {
      argbsCpk = ArrayUtil.arrayCopy(JmolConstants.argbsCpk, 0, -1, false);
      altArgbsCpk = ArrayUtil.arrayCopy(JmolConstants.altArgbsCpk, 0, -1, false);
    }
    if (id < JmolConstants.elementNumberMax) {
      argbsCpk[id] = argb;
      g3d.changeColixArgb((short)id, argb);
      return;
    }
    id = JmolConstants.altElementIndexFromNumber(id);
    altArgbsCpk[id] = argb;
    g3d.changeColixArgb((short) (JmolConstants.elementNumberMax + id), argb);
  }

  int setColorScheme(String colorScheme, boolean isOverloaded) {
    palette = ColorEncoder.getColorScheme(colorScheme, isOverloaded);
    Logger.info("ColorManager: color scheme now \"" + ColorEncoder.getColorSchemeName(palette) + "\" color value range: " + colorLo + " to " + colorHi);
    return palette;
  }

  float[] getCurrentColorRange() {
    return new float[] {colorLo, colorHi};
  }

  void setCurrentColorRange(float[] data, BitSet bs, String colorScheme) {
    colorData = data;
    palette = ColorEncoder.getColorScheme(colorScheme, false);
    colorHi = Float.MIN_VALUE;
    colorLo = Float.MAX_VALUE;
    if (data == null)
      return;
    for (int i = data.length; --i >= 0;)
      if (bs == null || bs.get(i)) {
        float d = data[i];
        if (Float.isNaN(d))
          continue;
        colorHi = Math.max(colorHi, d);
        colorLo = Math.min(colorLo, d);
      }
    setCurrentColorRange(colorLo, colorHi);
  }  

  void setCurrentColorRange(float min, float max) {
    colorHi = max;
    colorLo = min;
    Logger.info("color \"" + ColorEncoder.getColorSchemeName(palette) + "\" range " + colorLo + " " + colorHi);
  }

  static String getState(StringBuffer sfunc) {
    return ColorEncoder.getState(sfunc);
  }
  
  static void setUserScale(int[] scale) {
    ColorEncoder.setUserScale(scale);
  }
  
  int[] getColorSchemeArray(String colorScheme) {
    return ColorEncoder.getColorSchemeArray(colorScheme == null || colorScheme.length() == 0 ? palette : ColorEncoder.getColorScheme(colorScheme, false));  
  }
  
  String getColorSchemeList(String colorScheme, boolean ifDefault) {
    if (!ifDefault && ColorEncoder.getColorScheme(colorScheme, false) >= 0)
      return "";
    return ColorEncoder.getColorSchemeList(getColorSchemeArray(colorScheme));
  }
  
  short getColixForPropertyValue(float val) {
    return (colorLo < colorHi ? 
        ColorEncoder.getColorIndexFromPalette(val, colorLo, colorHi, palette)
        :ColorEncoder.getColorIndexFromPalette(-val, -colorLo, -colorHi, palette));    
  }

  /*
  void flushCachedColors() {
  }
  */
  
  private static void flushCaches() {
    Graphics3D.flushShadesAndSphereCaches();
  }

  static void setSpecular(boolean specular) {
    if (Graphics3D.getSpecular() == specular)
      return;
    Graphics3D.setSpecular(specular);
    flushCaches();
  }

  static boolean getSpecular() {
    return Graphics3D.getSpecular();
  }

  static void setSpecularPercent(int specularPercent) {
    if (Graphics3D.getSpecularPercent() == specularPercent)
      return;
    Graphics3D.setSpecularPercent(specularPercent);
    flushCaches();
  }
  
  static int getSpecularPercent() {
    return Graphics3D.getSpecularPercent();
  }

  static void setSpecularPower(int specularPower) {
    if (specularPower < 0) {
      if (Graphics3D.getSpecularExponent() == -specularPower)
        return;
      Graphics3D.setSpecularExponent(-specularPower);
    } else {
      if (Graphics3D.getSpecularPower() == specularPower)
        return;
      Graphics3D.setSpecularPower(specularPower);
    }
    flushCaches();
  }
  
  static void setDiffusePercent(int diffusePercent) {
    if (Graphics3D.getDiffusePercent() == diffusePercent)
      return;
    Graphics3D.setDiffusePercent(diffusePercent);
    flushCaches();
  }

  static int getDiffusePercent() {
    return Graphics3D.getDiffusePercent();
  }

  static void setAmbientPercent(int ambientPercent) {
    if (Graphics3D.getAmbientPercent() == ambientPercent)
      return;
    Graphics3D.setAmbientPercent(ambientPercent);
    flushCaches();
  }

  static int getAmbientPercent() {
    return Graphics3D.getAmbientPercent();
  }

  /*
   void setLightsourceZ(float dist) {
   Graphics3D.setLightsourceZ(dist);
   flushCaches();
   }
   */

}
