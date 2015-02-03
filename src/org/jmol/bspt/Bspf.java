/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-08 21:59:41 +0200 (lun., 08 juin 2009) $
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
package org.jmol.bspt;

import java.util.BitSet;

import javax.vecmath.Point3f;

/**
 * A Binary Space Partitioning Forest
 *<p>
 * This is simply an array of Binary Space Partitioning Trees identified
 * by indexes
 *
 * @author Miguel, miguel@jmol.org
*/

public final class Bspf {

  int dimMax;
  Bspt bspts[];
  //SphereIterator[] sphereIterators;
  CubeIterator[] cubeIterators;
  
  public Bspf(int dimMax) {
    this.dimMax = dimMax;
    bspts = new Bspt[0];
    cubeIterators = new CubeIterator[0];
  }

  public int getBsptCount() {
    return bspts.length;
  }
  
  public void clearBspt(int bsptIndex) {
    bspts[bsptIndex] = null;
  }
  
  public boolean isInitialized(int bsptIndex) {
    return bspts.length > bsptIndex && bspts[bsptIndex] != null;
  }
  
  public void addTuple(int bsptIndex, Point3f tuple) {
    if (bsptIndex >= bspts.length) {
      Bspt[] t = new Bspt[bsptIndex + 1];
      System.arraycopy(bspts, 0, t, 0, bspts.length);
      bspts = t;
    }
    Bspt bspt = bspts[bsptIndex];
    if (bspt == null)
      bspt = bspts[bsptIndex] = new Bspt(dimMax);
    bspt.addTuple(tuple);
  }

  public void stats() {
    for (int i = 0; i < bspts.length; ++i)
      if (bspts[i] != null)
        bspts[i].stats();
  }

  /*
  public void dump() {
    for (int i = 0; i < bspts.length; ++i) {
      Logger.debug(">>>>\nDumping bspt #" + i + "\n>>>>");
      bspts[i].dump();
    }
    Logger.debug("<<<<");
  }
  */
/*
  public SphereIterator getSphereIterator(int bsptIndex) {
    if (bsptIndex >= sphereIterators.length) {
      SphereIterator[] t = new SphereIterator[bsptIndex + 1];
      System.arraycopy(sphereIterators, 0, t, 0, sphereIterators.length);
      sphereIterators = t;
    }
    if (sphereIterators[bsptIndex] == null &&
        bspts[bsptIndex] != null)
      sphereIterators[bsptIndex] = bspts[bsptIndex].allocateSphereIterator();
    return sphereIterators[bsptIndex];
  }
*/  
  public CubeIterator getCubeIterator(int bsptIndex) {
    if (bsptIndex >= cubeIterators.length) {
      CubeIterator[] t = new CubeIterator[bsptIndex + 1];
      System.arraycopy(cubeIterators, 0, t, 0, cubeIterators.length);
      cubeIterators = t;
    }
    if (cubeIterators[bsptIndex] == null &&
        bspts[bsptIndex] != null)
      cubeIterators[bsptIndex] = bspts[bsptIndex].allocateCubeIterator();
    return cubeIterators[bsptIndex];
  }

  public void initialize(int modelIndex, Point3f[] atoms, BitSet modelAtomBitSet) {
    for (int i = atoms.length; --i >= 0;)
      if (modelAtomBitSet.get(i))
        addTuple(modelIndex, atoms[i]);
  }

}
