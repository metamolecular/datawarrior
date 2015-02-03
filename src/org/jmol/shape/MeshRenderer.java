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
package org.jmol.shape;

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import org.jmol.g3d.Graphics3D;

public abstract class MeshRenderer extends ShapeRenderer {

  protected float imageFontScaling;
  protected float scalePixelsPerMicron;
  protected Point3f[] vertices;
  protected short[] normixes;
  protected Point3i[] screens;
  protected Vector3f[] transformedVectors;
  protected int vertexCount;
  protected boolean frontOnly;
  protected boolean antialias;
  protected Mesh mesh;
  protected int diameter;
  protected float width;
  protected boolean isTranslucent;

  protected final Point3f pt1f = new Point3f();
  protected final Point3f pt2f = new Point3f();

  protected final Point3i pt1i = new Point3i();
  protected final Point3i pt2i = new Point3i();

  protected void render() {
    antialias = g3d.isAntialiased();
    MeshCollection mc = (MeshCollection) shape;
    for (int i = mc.meshCount; --i >= 0;)
      render1(mc.meshes[i]);
  }

  //draw, isosurface, molecular orbitals
  public boolean render1(Mesh mesh) {  //used by mps renderer
    this.mesh = mesh;
    if (!setVariables())
      return false;
    
    if (!g3d.setColix(colix) && !mesh.showContourLines)
      return mesh.title != null;

    transform();
    render2(isGenerator);
    viewer.freeTempScreens(screens);
    return true;
  }
  
  private boolean setVariables() {
    slabbing = viewer.getSlabEnabled();
    vertices = mesh.vertices; //because DRAW might have a text associated with it
    colix = mesh.colix;
    if (mesh == null || mesh.visibilityFlags == 0  
        || (vertexCount = mesh.vertexCount) == 0
        || mesh.polygonCount == 0)
      return false;
    normixes = mesh.normixes;
    if (normixes == null || vertices == null)
      return false;
    //this can happen when user switches windows 
    // during a surface calculation
    
    frontOnly = !slabbing && mesh.frontOnly && !mesh.isTwoSided;
    screens = viewer.allocTempScreens(vertexCount);
    transformedVectors = g3d.getTransformedVertexVectors();
    isTranslucent = Graphics3D.isColixTranslucent(mesh.colix);
    return true;
  }

  // all of the following methods are overridden in subclasses
  // DO NOT change parameters without first checking for the
  // same method in a subclass.
  
  protected void transform() {
    for (int i = vertexCount; --i >= 0;) {
      viewer.transformPoint(vertices[i], screens[i]);
/*
      Point3f ptf = new Point3f();
      viewer.transformPoint(vertices[i], ptf);
      System.out.println("meshrend " + i + " " + vertices[i] + " " + screens[i] + " " + ptf);
*/      
    }
  }
  
  protected boolean isPolygonDisplayable(int i) {
    return true;
  }

  //isosurface,meshRenderer::render1 (just about everything)
  protected void render2(boolean generateSet) {
    if (!g3d.setColix(colix))
      return;
    if (mesh.showPoints)
      renderPoints();
    if (mesh.drawTriangles)
      renderTriangles(false, false, false);
    if (mesh.fillTriangles)
      renderTriangles(true, mesh.showTriangles, generateSet);
  }
  
  protected void renderPoints() {
    for (int i = vertexCount; --i >= 0;)
      if (!frontOnly || transformedVectors[normixes[i]].z >= 0)
        g3d.fillSphereCentered(4, screens[i]);
  }

  protected BitSet bsFaces = new BitSet();
  protected void renderTriangles(boolean fill, boolean iShowTriangles, boolean generateSet) {
    int[][] polygonIndexes = mesh.polygonIndexes;
    colix = mesh.colix;
    //vertexColixes are only isosurface properties of IsosurfaceMesh, not Mesh
    g3d.setColix(colix);
    if (generateSet) {
      frontOnly = false;
      bsFaces.clear();
    }
    for (int i = mesh.polygonCount; --i >= 0;) {
      if (!isPolygonDisplayable(i))
        continue;
      int[] vertexIndexes = polygonIndexes[i];
      int iA = vertexIndexes[0];
      int iB = vertexIndexes[1];
      int iC = vertexIndexes[2];
      if (iB == iC) {
        //line or point
        drawLine(iA, iB, fill);
        continue;
      }
      switch (vertexIndexes.length) {
      case 3:
        if (frontOnly && transformedVectors[normixes[iA]].z < 0
            && transformedVectors[normixes[iB]].z < 0
            && transformedVectors[normixes[iC]].z < 0)
          continue;
        if (fill) {
          if (generateSet) {
            bsFaces.set(i);
            continue;
          }
          if (iShowTriangles) {
            g3d.fillTriangle(screens[iA], colix, normixes[iA], screens[iB],
                colix, normixes[iB], screens[iC], colix, normixes[iC], 0.1f);
            continue;
          }
          g3d.fillTriangle(screens[iA], colix, normixes[iA], screens[iB],
              colix, normixes[iB], screens[iC], colix, normixes[iC]);
          continue;
        }
        g3d.drawTriangle(screens[iA], screens[iB], screens[iC], 7);
        continue;
      case 4:
        int iD = vertexIndexes[3];
        if (frontOnly && transformedVectors[normixes[iA]].z < 0
            && transformedVectors[normixes[iB]].z < 0
            && transformedVectors[normixes[iC]].z < 0
            && transformedVectors[normixes[iD]].z < 0)
          continue;
        if (fill) {
          if (generateSet) {
            bsFaces.set(i);
            continue;
          }
          g3d.fillQuadrilateral(screens[iA], colix, normixes[iA], screens[iB],
              colix, normixes[iB], screens[iC], colix, normixes[iC],
              screens[iD], colix, normixes[iD]);
          continue;
        }
        g3d.drawQuadrilateral(colix, screens[iA], screens[iB], screens[iC],
            screens[iD]);
      }
    }
    if (generateSet)
      renderExport();
  }

  protected void drawLine(int iA, int iB, boolean fill) {
    byte endCap = (iA != iB  && !fill ? Graphics3D.ENDCAPS_NONE 
        : width < 0 || iA != iB && isTranslucent ? Graphics3D.ENDCAPS_FLAT
        : Graphics3D.ENDCAPS_SPHERICAL);
    if (diameter == 0)
      diameter = (mesh.diameter > 0 ? mesh.diameter : iA == iB ? 7 : 3);
    if (width == 0) {
      if (iA == iB)
        g3d.fillSphereCentered(diameter, screens[iA]);
      else
        g3d.fillCylinder(endCap, diameter, screens[iA], screens[iB]);
    } else {
      pt1f.set(vertices[iA]);
      pt1f.add(vertices[iB]);
      pt1f.scale(1f / 2f);
      viewer.transformPoint(pt1f, pt1i);      
      diameter = viewer.scaleToScreen(pt1i.z,
          (int) (Math.abs(width) * 1000));
      if (diameter == 0)
        diameter = 1;
      viewer.transformPoint(vertices[iA], pt1f);
      viewer.transformPoint(vertices[iB], pt2f);
      if (mesh.scale != 0 && mesh.haveXyPoints) {
        
      }

      g3d.fillCylinderBits(endCap, diameter, pt1f, pt2f);
    }    
  }

  protected void renderExport() {
      g3d.renderIsosurface(mesh.vertices, mesh.colix, null,
          mesh.getVertexNormals(), mesh.polygonIndexes, bsFaces, 
          mesh.vertexCount, 4, null, mesh.polygonCount);
  }
   
}
