/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-08-22 03:13:40 -0500 (Tue, 22 Aug 2006) $
 * $Revision: 5412 $

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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.shape;

import java.util.BitSet;
import java.util.Hashtable;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.viewer.JmolConstants;

public abstract class AtomShape extends Shape {

  // Balls, Dots, Ellipsoids, Halos, Labels, Polyhedra, Stars, Vectors

  public short[] mads;
  public short[] colixes;
  public byte[] paletteIDs;
  protected BitSet bsSizeSet;
  protected BitSet bsColixSet;
  public int atomCount;
  public Atom[] atoms;
  public boolean isActive;

  protected void initModelSet() {
    atoms = modelSet.atoms;
    atomCount = modelSet.getAtomCount();
    // in case this is due to "load append"
    if (mads != null)
      mads = ArrayUtil.setLength(mads, atomCount);
    if (colixes != null)
      colixes = ArrayUtil.setLength(colixes, atomCount);
    if (paletteIDs != null)
      paletteIDs = ArrayUtil.setLength(paletteIDs, atomCount);
  }

  public void setSize(int size, BitSet bsSelected) {
    setSize(size, Float.NaN, bsSelected);
  }

  public void setSize(int size, float fsize, BitSet bsSelected) {
    // Halos Stars Vectors only
    isActive = true;
    if (bsSizeSet == null)
      bsSizeSet = new BitSet();
    boolean isVisible = (size != 0);
    for (int i = atomCount; --i >= 0;)
      if (bsSelected == null || bsSelected.get(i)) {
        if (mads == null)
          mads = new short[atomCount];
        Atom atom = atoms[i];
        mads[i] = atom.convertEncodedMad(viewer, size, fsize);
        bsSizeSet.set(i, isVisible);
        atom.setShapeVisibility(myVisibilityFlag, isVisible);
      }
  }

  public void setProperty(String propertyName, Object value, BitSet bs) {
    if ("color" == propertyName) {
      isActive = true;
      short colix = Graphics3D.getColix(value);
      byte pid = JmolConstants.pidOf(value);
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i))
          setColixAndPalette(colix, pid, i);
      return;
    }
    if ("translucency" == propertyName) {
      isActive = true;
      boolean isTranslucent = (value.equals("translucent"));
      if (bsColixSet == null)
        bsColixSet = new BitSet();
      for (int i = atomCount; --i >= 0;)
        if (bs.get(i)) {
          if (colixes == null) {
            colixes = new short[atomCount];
            paletteIDs = new byte[atomCount];
          }
          colixes[i] = Graphics3D.getColixTranslucent(colixes[i],
              isTranslucent, translucentLevel);
          if (isTranslucent)
            bsColixSet.set(i);
        }
      return;
    }
    if (propertyName == "deleteModelAtoms") {
      atoms = (Atom[]) ((Object[]) value)[1];
      atomCount = modelSet.getAtomCount();
      int firstAtomDeleted = ((int[]) ((Object[]) value)[2])[1];
      int nAtomsDeleted = ((int[]) ((Object[]) value)[2])[2];
      mads = (short[]) ArrayUtil.deleteElements(mads, firstAtomDeleted,
          nAtomsDeleted);
      colixes = (short[]) ArrayUtil.deleteElements(colixes, firstAtomDeleted,
          nAtomsDeleted);
      paletteIDs = (byte[]) ArrayUtil.deleteElements(paletteIDs,
          firstAtomDeleted, nAtomsDeleted);
      BitSetUtil.deleteBits(bsSizeSet, bs);
      BitSetUtil.deleteBits(bsColixSet, bs);
      return;
    }
    super.setProperty(propertyName, value, bs);
  }

  void setColixAndPalette(short colix, byte paletteID, int atomIndex) {
    if (colixes == null || atomIndex >= colixes.length) {
      if (colix == Graphics3D.INHERIT_ALL)
        return;
      colixes = ArrayUtil.ensureLength(colixes, atomIndex + 1);
      paletteIDs = ArrayUtil.ensureLength(paletteIDs, atomIndex + 1);
    }
    if (bsColixSet == null)
      bsColixSet = new BitSet();
    colixes[atomIndex] = colix = setColix(colix, paletteID, atomIndex);
    bsColixSet.set(atomIndex, colix != Graphics3D.INHERIT_ALL);
    paletteIDs[atomIndex] = paletteID;
  }

  public void setModelClickability() {
    if (!isActive)
      return;
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.getShapeVisibilityFlags() & myVisibilityFlag) == 0
          || modelSet.isAtomHidden(i))
        continue;
      atom.setClickable(myVisibilityFlag);
    }
  }

  public String getShapeState() {
    if (!isActive)
      return "";
    Hashtable temp = new Hashtable();
    Hashtable temp2 = new Hashtable();
    String type = JmolConstants.shapeClassBases[shapeID];
    for (int i = atomCount; --i >= 0;) {
      if (bsSizeSet != null && bsSizeSet.get(i))
        setStateInfo(temp, i, type + " " + (mads[i] / 2000f));
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp2, i, getColorCommand(type, paletteIDs[i], colixes[i]));
    }
    return getShapeCommands(temp, temp2, atomCount);
  }

}
