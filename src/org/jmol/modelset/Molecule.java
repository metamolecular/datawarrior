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

import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;
import java.util.BitSet;
import java.util.Hashtable;

class Molecule {
  
  ModelSet modelSet;
  int moleculeIndex;
  int modelIndex;
  int indexInModel;
  int nAtoms;
  int nElements;
  int[] elementCounts = new int[JmolConstants.elementNumberMax];
  int[] altElementCounts = new int[JmolConstants.altElementMax];
  int elementNumberMax;
  int altElementMax;
  String mf;
  BitSet atomList;

  Molecule(ModelSet modelSet, int moleculeIndex, BitSet atomList, int modelIndex,
      int indexInModel) {
    this.modelSet = modelSet;
    this.atomList = atomList;
    this.moleculeIndex = moleculeIndex;
    this.modelIndex = modelIndex;
    this.indexInModel = indexInModel;
    getElementAndAtomCount(atomList);
    mf = getMolecularFormula();

    if (Logger.debugging)
      Logger.debug("new Molecule (" + mf + ") " + (indexInModel + 1) + "/"
          + (modelIndex + 1));
  }

  void getElementAndAtomCount(BitSet atomList) {
    for (int i = 0; i < modelSet.atomCount; i++)
      if (atomList.get(i)) {
        nAtoms++;
        int n = modelSet.atoms[i].getAtomicAndIsotopeNumber();
        if (n < JmolConstants.elementNumberMax) {
          elementCounts[n]++;
          if (elementCounts[n] == 1)
            nElements++;
          elementNumberMax = Math.max(elementNumberMax, n);
        } else {
          n = JmolConstants.altElementIndexFromNumber(n);
          altElementCounts[n]++;
          if (altElementCounts[n] == 1)
            nElements++;
          altElementMax = Math.max(altElementMax, n);
        }
      }
  }

  String getMolecularFormula() {
    String mf = "";
    String sep = "";
    int nX;
    for (int i = 1; i <= elementNumberMax; i++) {
      nX = elementCounts[i];
      if (nX != 0) {
        mf += sep + JmolConstants.elementSymbolFromNumber(i) + " " + nX;
        sep = " ";
      }
    }
    for (int i = 1; i <= altElementMax; i++) {
      nX = altElementCounts[i];
      if (nX != 0) {
        mf += sep
            + JmolConstants.elementSymbolFromNumber(JmolConstants
                .altElementNumberFromIndex(i)) + " " + nX;
        sep = " ";
      }
    }
    return mf;
  }

  Hashtable getInfo() {
    Hashtable info = new Hashtable();
    info.put("number", new Integer(moleculeIndex + 1)); //for now
    info.put("modelNumber", modelSet.getModelNumberDotted(modelIndex));
    info.put("numberInModel", new Integer(indexInModel + 1));
    info.put("nAtoms", new Integer(nAtoms));
    info.put("nElements", new Integer(nElements));
    info.put("mf", mf);
    return info;
  }
}  
