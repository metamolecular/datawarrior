/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-11-15 22:16:49 +0100 (sam., 15 nov. 2008) $
 * $Revision: 10330 $
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

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.api.JmolRendererInterface;
import org.jmol.modelset.ModelSet;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

public abstract class ShapeRenderer {

  //public void finalize() {
  //  System.out.println("ShapeRenderer " + shapeID + " " + this + " finalized");
  //}
  
  protected Viewer viewer;
  protected JmolRendererInterface g3d;
  protected ModelSet modelSet;
  protected Shape shape;

  protected int myVisibilityFlag;
  protected int shapeID;
  
  //working values, for export generators
  protected short colix;
  protected short mad;
  protected short madBeg;
  protected short madMid;
  protected short madEnd;
  protected boolean isGenerator;
  protected boolean slabbing;

  public void setGenerator(boolean isGenerator) {
    this.isGenerator = isGenerator;
  }

  public short getMad(int which) {
    switch (which) {
    case 1:
      return madBeg;
    case 2:
      return madMid;
    case 3:
      return madEnd;
    }
    return mad;
  }

  public final void setViewerG3dShapeID(Viewer viewer, JmolRendererInterface g3d, int shapeID) {
    this.viewer = viewer;
    this.g3d = g3d;
    this.shapeID = shapeID;
    myVisibilityFlag = JmolConstants.getShapeVisibilityFlag(shapeID);
    initRenderer();
  }

  protected void initRenderer() {
  }

  public void render(JmolRendererInterface g3d, ModelSet modelSet, Shape shape) {
    this.g3d = g3d;
    this.modelSet = modelSet;
    this.shape = shape;
    render();
  }

  abstract protected void render();
  
  protected void renderLine(Point3f p0, Point3f p1, int widthPixels, byte endcap, 
                            Point3i pt0, Point3i pt1) {
    pt0.set((int) p0.x, (int) p0.y, (int) p0.z );
    pt1.set((int) p1.x, (int) p1.y, (int) p1.z );
    if (widthPixels < 0)
      g3d.drawDottedLine(pt0, pt1);
    else
      g3d.fillCylinder(endcap, widthPixels, pt0, pt1);
  }    

}

