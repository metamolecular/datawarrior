/* $RCSfile$
 * $Author: migueljmol $
 * $Date: 2006-03-25 09:27:43 -0600 (Sat, 25 Mar 2006) $
 * $Revision: 4696 $

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

package org.jmol.shape;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.viewer.JmolConstants;

import java.util.BitSet;

public class HalosRenderer extends ShapeRenderer {

  boolean isAntialiased;
  protected void render() {
    Halos halos = (Halos) shape;
    boolean selectDisplayTrue = viewer.getSelectionHaloEnabled();
    boolean showHiddenSelections = (selectDisplayTrue && viewer
        .getShowHiddenSelectionHalos());
    if (halos.mads == null && !selectDisplayTrue)
      return;
    isAntialiased = g3d.isAntialiased();
    Atom[] atoms = modelSet.atoms;
    BitSet bsSelected = (selectDisplayTrue ? viewer.getSelectionSet() : null);
    for (int i = modelSet.getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.getShapeVisibilityFlags() & JmolConstants.ATOM_IN_FRAME) == 0)
        continue;
      short mad = (halos.mads == null ? 0 : halos.mads[i]);
      short colix = (halos.colixes == null || i >= halos.colixes.length ? Graphics3D.INHERIT_ALL
          : halos.colixes[i]);
      boolean isHidden = modelSet.isAtomHidden(i);
      if (selectDisplayTrue && bsSelected.get(i)) {
        if (isHidden && !showHiddenSelections)
          continue;
        if (mad == 0)
          mad = -1; // unsized
        if (colix == Graphics3D.INHERIT_ALL)
          colix = halos.colixSelection;
        if (colix == Graphics3D.USE_PALETTE)
          colix = Graphics3D.GOLD;
        else if (colix == Graphics3D.INHERIT_ALL)
          colix = Graphics3D.getColixInherited(colix, atom.getColix());
      } else if (isHidden) {
        continue;
      } else {
        colix = Graphics3D.getColixInherited(colix, atom.getColix());
      }
      if (mad == 0)
        continue;
      render1(atom, mad, colix);
    }
  }

  void render1(Atom atom, short mad, short colix) {
    int z = atom.screenZ;
    int diameter = mad;
    if (diameter < 0) { //unsized selection
      diameter = atom.screenDiameter;
      if (diameter == 0) {
        float ellipsemax = atom.getADPMinMax(true);
        if (ellipsemax > 0)
          diameter = viewer.scaleToScreen(z, (int) (ellipsemax * 2000));
        if (diameter == 0) {
          diameter = viewer.scaleToScreen(z, 500);
        }
      }
    } else {
      diameter = viewer.scaleToScreen(z, mad);
    }
    float d = diameter;
    if (isAntialiased)
      d /= 2;
    float haloDiameter = (d / 4);
    if (haloDiameter < 4)
      haloDiameter = 4;
    if (haloDiameter > 10)
      haloDiameter = 10;
    haloDiameter = d + 2 * haloDiameter;
    if (isAntialiased)
      haloDiameter *= 2;
    int haloWidth = (int) haloDiameter;
    if (haloWidth <= 0)
      return;
    g3d.fillScreenedCircleCentered(colix, haloWidth, atom.screenX,
        atom.screenY, atom.screenZ);
  }  
}
