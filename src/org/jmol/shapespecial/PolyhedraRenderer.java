/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-12 11:05:36 -0500 (Mon, 12 Mar 2007) $
 * $Revision: 7077 $
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.shapespecial;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.ShapeRenderer;

public class PolyhedraRenderer extends ShapeRenderer {

  private int drawEdges;

  protected void render() {
    Polyhedra polyhedra = (Polyhedra) shape;
    Polyhedra.Polyhedron[] polyhedrons = polyhedra.polyhedrons;
    drawEdges = polyhedra.drawEdges;
    for (int i = polyhedra.polyhedronCount; --i >= 0;) {
      short colix = polyhedra.colixes == null ? Graphics3D.INHERIT_ALL
          : polyhedra.colixes[polyhedrons[i].centralAtom.getAtomIndex()];
      render1(polyhedrons[i], colix);
    }
  }

  private void render1(Polyhedra.Polyhedron p, short colix) {
    if (p.visibilityFlags == 0)
      return;
    colix = Graphics3D.getColixInherited(colix, p.centralAtom.getColix());
    Atom[] vertices = p.vertices;
    byte[] planes;

    planes = p.planes;
    for (int i = vertices.length; --i >= 0;) {
      if (vertices[i].isSimple())
        vertices[i].transform(viewer);
    }

    boolean isAll = (drawEdges == Polyhedra.EDGES_ALL);
    boolean isFrontOnly = (drawEdges == Polyhedra.EDGES_FRONT);

    // no edges to new points when not collapsed
    if (g3d.setColix(colix))
      for (int i = 0, j = 0; j < planes.length;)
        fillFace(p.normixes[i++], vertices[planes[j++]], vertices[planes[j++]],
            vertices[planes[j++]]);
    if (!g3d.setColix(Graphics3D.getColixTranslucent(colix, false, 0)))
      return;
    for (int i = 0, j = 0; j < planes.length;)
      drawFace(p.normixes[i++], vertices[planes[j++]],
          vertices[planes[j++]], vertices[planes[j++]], isAll, isFrontOnly);
  }

  private void drawFace(short normix, Atom atomA, Atom atomB, Atom atomC,
                boolean isAll, boolean isFrontOnly) {
    if (isAll || isFrontOnly && g3d.isDirectedTowardsCamera(normix))
      g3d.drawCylinderTriangle(atomA.screenX, atomA.screenY, atomA.screenZ,
          atomB.screenX, atomB.screenY, atomB.screenZ, atomC.screenX,
          atomC.screenY, atomC.screenZ, 3);
  }

  private void fillFace(short normix,
                  Atom atomA, Atom atomB, Atom atomC) {
    g3d.fillTriangle(normix,
                     atomA.screenX, atomA.screenY, atomA.screenZ,
                     atomB.screenX, atomB.screenY, atomB.screenZ,
                     atomC.screenX, atomC.screenY, atomC.screenZ);
  }
}
