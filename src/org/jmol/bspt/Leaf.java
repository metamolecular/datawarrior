/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-03-28 16:42:36 +0100 (sam., 28 mars 2009) $
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

import javax.vecmath.Point3f;

/**
 * A leaf of Point3f objects in the bsp tree
 *
 * @author Miguel, miguel@jmol.org
 */
class Leaf extends Element {
  Point3f[] tuples;
    
  Leaf(Bspt bspt) {
    this.bspt = bspt;
    count = 0;
    tuples = new Point3f[Bspt.leafCountMax];
  }
    
  Leaf(Bspt bspt, Leaf leaf, int countToKeep) {
    this(bspt);
    for (int i = countToKeep; i < Bspt.leafCountMax; ++i) {
      tuples[count++] = leaf.tuples[i];
      leaf.tuples[i] = null;
    }
    leaf.count = countToKeep;
  }

  void sort(int dim) {
    for (int i = count; --i > 0; ) { // this is > not >=
      Point3f champion = tuples[i];
      float championValue = Node.getDimensionValue(champion, dim);
      for (int j = i; --j >= 0; ) {
        Point3f challenger = tuples[j];
        float challengerValue = Node.getDimensionValue(challenger, dim);
        if (challengerValue > championValue) {
          tuples[i] = challenger;
          tuples[j] = champion;
          champion = challenger;
          championValue = challengerValue;
        }
      }
    }
  }

  Element addTuple(int level, Point3f tuple) {
    if (count < Bspt.leafCountMax) {
      tuples[count++] = tuple;
      return this;
    }
    Node node = new Node(bspt, level, this);
    return node.addTuple(level, tuple);
  }
    
  /*
    void dump(int level) {
    for (int i = 0; i < count; ++i) {
    Point3f t = tuples[i];
    for (int j = 0; j < level; ++j)
    Logger.debug(".");
    for (int dim = 0; dim < dimMax-1; ++dim)
    Logger.debug("" + t.getDimensionValue(dim) + ",");
    Logger.debug("" + t.getDimensionValue(dimMax - 1));
    }
    }

    public String toString() {
    return "leaf:" + count + "\n";
    }
  */

}
