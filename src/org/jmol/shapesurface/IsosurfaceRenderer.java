/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-25 09:53:35 -0500 (Wed, 25 Apr 2007) $
 * $Revision: 7491 $
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
package org.jmol.shapesurface;

import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.readers.Parameters;
import org.jmol.shape.MeshRenderer;
import org.jmol.util.Logger;

public class IsosurfaceRenderer extends MeshRenderer {

  private boolean iShowNormals;
  protected boolean iHideBackground;
  protected boolean isBicolorMap;
  protected short backgroundColix;
  protected int nError = 0;
  protected float[] vertexValues;
  protected IsosurfaceMesh imesh;

  protected void render() {
    iShowNormals = viewer.getTestFlag4();
    Isosurface isosurface = (Isosurface) shape;
    int slabValue = (viewer.getNavigationMode() ? g3d.getSlab() : Integer.MAX_VALUE);
    for (int i = isosurface.meshCount; --i >= 0;) {
      imesh = (IsosurfaceMesh) isosurface.meshes[i];
      if (slabValue != Integer.MAX_VALUE && imesh.isSolvent) {
        g3d.setSlab((int) viewer.getNavigationOffset().z);
        render1(imesh);
        g3d.setSlab(slabValue);
      } else {
        render1(imesh);
      }
    }
  }

  protected void transform() {
    vertexValues = imesh.vertexValues;
    for (int i = vertexCount; --i >= 0;) {
      if (Float.isNaN(vertices[i].x))
        continue;
      if (vertexValues == null || !Float.isNaN(vertexValues[i])
          || imesh.hasGridPoints) {
        viewer.transformPoint(vertices[i], screens[i]);
      }
    }
  }
  
  protected void render2(boolean isGenerator) {
    switch (imesh.dataType) {
    case Parameters.SURFACE_LONEPAIR:
      renderLonePair(false);
      return;
    case Parameters.SURFACE_RADICAL:
      renderLonePair(true);
      return;
    }
    isBicolorMap = imesh.jvxlData.isBicolorMap;
    super.render2(isGenerator);
    if (!g3d.setColix(Graphics3D.BLACK)) // must be 1st pass
      return;
    if (imesh.showContourLines)
      renderContourLines();
  }
  
  private void renderLonePair(boolean isRadical) {
    pt2f.set(imesh.vertices[1]);
    viewer.transformPoint(pt2f, pt2f);
    int r = viewer.scaleToScreen((int)pt2f.z, 100);
    if (r < 1)
      r = 1;
    if (!isRadical) {
      Vector3f v1 = new Vector3f();
      Vector3f v2 = new Vector3f();
      pt1f.set(imesh.vertices[0]);
      viewer.transformPoint(pt1f, pt1f);
      v1.sub(pt2f, pt1f);
      v2.set(v1.x, v1.y, v1.z + 1);
      v2.cross(v2,v1);
      v2.normalize();
      float f = viewer.scaleToScreen((int)pt1f.z, 100);
      v2.scale(f);
      pt1f.set(pt2f);
      pt1f.add(v2);
      pt2f.sub(v2);
      screens[0].set((int)pt1f.x,(int)pt1f.y,(int)pt1f.z);
      g3d.fillSphereCentered(r, screens[0]);
    }
    screens[1].set((int)pt2f.x,(int)pt2f.y,(int)pt2f.z);
    g3d.fillSphereCentered(r, screens[1]);
  }
  
  private void renderContourLines() {
    Vector[] vContours = imesh.getContours();
    if (vContours == null)
      return;
    for (int i = vContours.length; --i >= 0;) {
      Vector v = vContours[i];
      if (v.size() < IsosurfaceMesh.CONTOUR_POINTS)
        continue;
      if (!g3d.setColix(mesh.fillTriangles ? Graphics3D.BLACK : Graphics3D
          .getColix(((int[]) v.get(IsosurfaceMesh.CONTOUR_COLOR))[0])))
        return;
      int n = v.size() - 1;
      for (int j = IsosurfaceMesh.CONTOUR_POINTS; j < n; j++) {
        Point3f pt1 = (Point3f) v.get(j);
        Point3f pt2 = (Point3f) v.get(++j);
        viewer.transformPoint(pt1, pt1i);
        viewer.transformPoint(pt2, pt2i);
        if (Float.isNaN(pt1.x) || Float.isNaN(pt2.x))
          break;
        g3d.drawLine(pt1i, pt2i);
      }
    }
  }
  
  private final Point3f ptTemp = new Point3f();
  private final Point3i ptTempi = new Point3i();

  protected void renderPoints() {
    int incr = imesh.vertexIncrement;
    int diam = 4;
    boolean showNumbers = viewer.getTestFlag2();
    if (showNumbers)
      g3d.setFont(g3d.getFontFid("Monospaced", 10));
    for (int i = (!imesh.hasGridPoints || imesh.firstRealVertex < 0 ? 0
        : imesh.firstRealVertex); i < vertexCount; i += incr) {
      if (vertexValues != null && Float.isNaN(vertexValues[i]) || frontOnly
          && transformedVectors[normixes[i]].z < 0)
        continue;
      if (imesh.vertexColixes != null)
        g3d.setColix(imesh.vertexColixes[i]);
      g3d.fillSphereCentered(diam, screens[i]);
      if (showNumbers && screens[i].z > 10)
        g3d.drawStringNoSlab(i
            + (imesh.isColorSolid ? "" : " " + imesh.vertexValues[i]), null,
            screens[i].x, screens[i].y, screens[i].z);
    }
    if (incr != 3)
      return;
    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.GRAY, true, 0.5f) : Graphics3D.GRAY);
    for (int i = 1; i < vertexCount; i += 3)
      g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, 1, screens[i],
          screens[i + 1]);
    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.YELLOW, true, 0.5f) : Graphics3D.YELLOW);
    for (int i = 1; i < vertexCount; i += 3)
      g3d.fillSphereCentered(4, screens[i]);
    g3d.setColix(isTranslucent ? Graphics3D.getColixTranslucent(
        Graphics3D.BLUE, true, 0.5f) : Graphics3D.BLUE);
    for (int i = 2; i < vertexCount; i += 3) {
      g3d.fillSphereCentered(4, screens[i]);
    }
  }

  protected void renderTriangles(boolean fill, boolean iShowTriangles, boolean isGenerator) {
    int[][] polygonIndexes = imesh.polygonIndexes;
    colix = imesh.colix;
    short[] vertexColixes = imesh.vertexColixes;
    g3d.setColix(imesh.colix);
    boolean generateSet = isGenerator;
    if (generateSet) {
      frontOnly = false;
      bsFaces.clear();
    }
    boolean colorSolid = (vertexColixes == null || imesh.isColorSolid);
    short colix = this.colix;
    if (!colorSolid && !fill && imesh.fillTriangles
        && imesh.jvxlData.jvxlPlane != null) {
      colorSolid = true;
      colix = Graphics3D.BLACK;
    }
    boolean colorArrayed = (fill && colorSolid && imesh.polygonColixes != null);

    // two-sided means like a plane, with no front/back distinction
    for (int i = imesh.polygonCount; --i >= 0;) {
      int[] vertexIndexes = polygonIndexes[i];
      if (vertexIndexes == null)
        continue;
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      if (imesh.thisSet >= 0 && imesh.vertexSets[iA] != imesh.thisSet)
        continue;
      short nA = normixes[iA];
      short nB = normixes[iB];
      short nC = normixes[iC];
      if (frontOnly && transformedVectors[nA].z < 0
          && transformedVectors[nB].z < 0 && transformedVectors[nC].z < 0)
        continue;
      short colixA, colixB, colixC;
      if (colorSolid) {
        if (colorArrayed && i < imesh.polygonColixes.length) {
          short c = imesh.polygonColixes[i];
          if (c != 0)
            colix = c;
        }
        colixA = colixB = colixC = colix;
      } else {
        colixA = vertexColixes[iA];
        colixB = vertexColixes[iB];
        colixC = vertexColixes[iC];
        if (isBicolorMap && (colixA != colixB || colixB != colixC))
          continue;
      }
      /*      System.out.println(iA + " " + iB + " " + iC + " " + colixA + " " + colixB + " " + colixC 
       + " " + Integer.toHexString(Graphics3D.getColorArgb(colixA))
       + " " + Integer.toHexString(Graphics3D.getColorArgb(colixB))
       + " " + Integer.toHexString(Graphics3D.getColorArgb(colixC))
       );
       */
      if (fill) {
        if (generateSet) {
          bsFaces.set(i);
          continue;
        }
        if (iShowTriangles) {
          g3d.fillTriangle(screens[iA], colixA, nA, screens[iB], colixB, nB,
              screens[iC], colixC, nC, 0.1f);
        } else {
          //System.out.println(iA + " " + screens[iA] + " " + screens[iB] + " " + screens[iC]);
          try {
            g3d.fillTriangle(screens[iA], colixA, nA, screens[iB], colixB, nB,
                screens[iC], colixC, nC);
          } catch (Exception e) {
            if (nError++ < 1) {
              Logger.warn("IsosurfaceRenderer -- competing thread bug?\n", e);
            }
          }
        }
        if (iShowNormals)
          renderNormals();
      } else {
        int check = vertexIndexes[3];
        if (check == 0)
          continue;
        // check: 1 (ab) | 2(bc) | 4(ac)
        if (vertexColixes == null)
          g3d.drawTriangle(screens[iA], screens[iB], screens[iC], check);
        else
          g3d.drawTriangle(screens[iA], colixA, screens[iB], colixB,
              screens[iC], colixC, check);
      }
    }
    if (generateSet)
      renderExport();
  }

  private void renderNormals() {
    //Logger.debug("mesh renderPoints: " + vertexCount);
    if (!g3d.setColix(Graphics3D.WHITE))
      return;
    for (int i = vertexCount; --i >= 0;) {
      if (vertexValues != null && !Float.isNaN(vertexValues[i])) {
        //if ((i % 3) == 0) { //investigate vertex normixes
          ptTemp.set(mesh.vertices[i]);
          short n = mesh.normixes[i];
          // -n is an intensity2sided and does not correspond to a true normal index
          if (n >= 0) {
            //System.out.println(i + " " + n);
            ptTemp.add(g3d.getNormixVector(n));
            viewer.transformPoint(ptTemp, ptTempi);
            g3d.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, 1,
                screens[i], ptTempi);
          }
      }
    }
  }

}
