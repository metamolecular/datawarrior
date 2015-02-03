/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
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


/*
 
 * The JVXL file format
 * --------------------
 * 
 * as of 3/29/07 this code is COMPLETELY untested. It was hacked out of the
 * Jmol code, so there is probably more here than is needed.
 * 
 * 
 * 
 * see http://www.stolaf.edu/academics/chemapps/jmol/docs/misc/JVXL-format.pdf
 *
 * The JVXL (Jmol VoXeL) format is a file format specifically designed
 * to encode an isosurface or planar slice through a set of 3D scalar values
 * in lieu of a that set. A JVXL file can contain coordinates, and in fact
 * it must contain at least one coordinate, but additional coordinates are
 * optional. The file can contain any finite number of encoded surfaces. 
 * However, the compression of 300-500:1 is based on the reduction of the 
 * data to a SINGLE surface. 
 * 
 * 
 * The original Marching Cubes code was written by Miguel Howard in 2005.
 * The classes Parser, ArrayUtil, and TextFormat are condensed versions
 * of the classes found in org.jmol.util.
 * 
 * All code relating to JVXL format is copyrighted 2006/2007 and invented by 
 * Robert M. Hanson, 
 * Professor of Chemistry, 
 * St. Olaf College, 
 * 1520 St. Olaf Ave.
 * Northfield, MN. 55057.
 * 
 * Implementations of the JVXL format should reference 
 * "Robert M. Hanson, St. Olaf College" and the opensource Jmol project.
 * 
 * 
 * implementing marching squares; see 
 * http://www.secam.ex.ac.uk/teaching/ug/studyres/COM3404/COM3404-2006-Lecture15.pdf
 * 
 * lines through coordinates are identical to CUBE files
 * after that, we have a line that starts with a negative number to indicate this
 * is a JVXL file:
 * 
 * line1:  (int)-nSurfaces  (int)edgeFractionBase (int)edgeFractionRange  
 * (nSurface lines): (float)cutoff (int)nBytesData (int)nBytesFractions
 * 
 * definition1
 * edgedata1
 * fractions1
 * colordata1
 * ....
 * definition2
 * edgedata2
 * fractions2
 * colordata2
 * ....
 * 
 * definitions: a line with detail about what sort of compression follows
 * 
 * edgedata: a list of the count of vertices ouside and inside the cutoff, whatever
 * that may be, ordered by nested for loops for(x){for(y){for(z)}}}.
 * 
 * nOutside nInside nOutside nInside...
 * 
 * fractions: an ascii list of characters represting the fraction of distance each
 * encountered surface point is along each voxel cube edge found to straddle the 
 * surface. The order written is dictated by the reader algorithm and is not trivial
 * to describe. Each ascii character is constructed by taking a base character and 
 * adding onto it the fraction times a range. This gives a character that can be
 * quoted EXCEPT for backslash, which MAY be substituted for by '!'. Jmol uses the 
 * range # - | (35 - 124), reserving ! and } for special meanings.
 * 
 * colordata: same deal here, but with possibility of "double precision" using two bytes.
 * 
 * 
 * 
 * THIS READER
 * -----------
 * 
 * This is a first attempt at a generic JVXL file reader and writer class.
 * It is an extraction of Jmol org.jmol.viewer.Isosurface.Java and related pieces.
 * 
 * The goal of the reader is to be able to read CUBE-like data and 
 * convert that data to JVXL file data.
 * 
 * 
 */

package org.jmol.jvxl.data;

import java.util.BitSet;

import javax.vecmath.Point3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.util.*;

public class MeshData {
  private final static int SEED_COUNT = 25;
  
  public final static int MODE_GET_VERTICES = 1;
  public final static int MODE_GET_COLOR_INDEXES = 2;
  public final static int MODE_PUT_SETS = 3;
  public final static int MODE_PUT_VERTICES = 4;

  public int polygonCount;
  public Point3f[] vertices;
  public short[] vertexColixes;
  public int vertexCount;
  public float[] vertexValues;
  public int[][] polygonIndexes;
  public short[] polygonColixes;

  public BitSet[] surfaceSet;
  public int[] vertexSets;
  public int nSets = 0;
  
  public Point3f[] dots;
  
  private boolean setsSuccessful;
  public int vertexIncrement = 1;


  public void clear(String meshType) {
    vertexCount = polygonCount = 0;
    vertices = null;
    polygonIndexes = null;
    surfaceSet = null;
  }
  
  public int addVertexCopy(Point3f vertex, float value, int assocVertex) {
    if (assocVertex < 0)
      vertexIncrement = -assocVertex;  //3 in some cases
    if (vertexCount == 0)
      vertexValues = new float[SEED_COUNT];
    else if (vertexCount >= vertexValues.length)
      vertexValues = (float[]) ArrayUtil.doubleLength(vertexValues);
    vertexValues[vertexCount] = value;
    return addVertexCopy(vertex);
  }

  private int addVertexCopy(Point3f vertex) {
    if (vertexCount == 0)
      vertices = new Point3f[SEED_COUNT];
    else if (vertexCount == vertices.length)
      vertices = (Point3f[]) ArrayUtil.doubleLength(vertices);
    vertices[vertexCount] = new Point3f(vertex);
    //Logger.debug("mesh.addVertexCopy " + vertexCount + vertex +vertices[vertexCount]);
    return vertexCount++;
  }

  private int lastColor;
  private short lastColix;

  public void addTriangleCheck(int vertexA, int vertexB, int vertexC, int check, int color) {
  if (vertexValues != null && (Float.isNaN(vertexValues[vertexA])||Float.isNaN(vertexValues[vertexB])||Float.isNaN(vertexValues[vertexC])))
    return;
  if (Float.isNaN(vertices[vertexA].x)||Float.isNaN(vertices[vertexB].x)||Float.isNaN(vertices[vertexC].x))
    return;
  if (polygonCount == 0)
    polygonIndexes = new int[SEED_COUNT][];
  else if (polygonCount == polygonIndexes.length)
    polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
  if (color != 0) {
    if (polygonColixes == null) {
      polygonColixes = new short[SEED_COUNT];
      lastColor = 0;
    }
    else if (polygonCount == polygonColixes.length) {
      polygonColixes = (short[]) ArrayUtil.doubleLength(polygonColixes);
    }
    polygonColixes[polygonCount] = (color == lastColor ? lastColix : (lastColix = Graphics3D.getColix(lastColor = color)));
  }    
  polygonIndexes[polygonCount++] = new int[] {vertexA, vertexB, vertexC, check};
 }
  
  public BitSet[] getSurfaceSet() {
    return (surfaceSet == null ? getSurfaceSet(0) : surfaceSet);
  }
  
  public BitSet[] getSurfaceSet(int level) {
    if (level == 0) {
      surfaceSet = new BitSet[100];
      nSets = 0;
    }
    setsSuccessful = true;
    for (int i = 0; i < polygonCount; i++)
      if (polygonIndexes[i] != null) {
        int[] p = polygonIndexes[i];
        int pt0 = findSet(p[0]);
        int pt1 = findSet(p[1]);
        int pt2 = findSet(p[2]);
        if (pt0 < 0 && pt1 < 0 && pt2 < 0) {
          createSet(p[0], p[1], p[2]);
          continue;
        }
        if (pt0 == pt1 && pt1 == pt2)
          continue;
        if (pt0 >= 0) {
          surfaceSet[pt0].set(p[1]);
          surfaceSet[pt0].set(p[2]);
          if (pt1 >= 0 && pt1 != pt0)
            mergeSets(pt0, pt1);
          if (pt2 >= 0 && pt2 != pt0 && pt2 != pt1)
            mergeSets(pt0, pt2);
          continue;
        }
        if (pt1 >= 0) {
          surfaceSet[pt1].set(p[0]);
          surfaceSet[pt1].set(p[2]);
          if (pt2 >= 0 && pt2 != pt1)
            mergeSets(pt1, pt2);
          continue;
        }
        surfaceSet[pt2].set(p[0]);
        surfaceSet[pt2].set(p[1]);
      }
    int n = 0;
    for (int i = 0; i < nSets; i++)
      if (surfaceSet[i] != null)
        n++;
    BitSet[] temp = new BitSet[100];
    n = 0;
    for (int i = 0; i < nSets; i++)
      if (surfaceSet[i] != null)
        temp[n++] = surfaceSet[i];
    nSets = n;
    surfaceSet = temp;
    if (!setsSuccessful && level < 2)
      getSurfaceSet(level + 1);
    if (level == 0) {
      vertexSets = new int[vertexCount];
      for (int i = 0; i < nSets; i++)
        for (int j = 0; j < vertexCount; j++)
          if (surfaceSet[i].get(j))
            vertexSets[j] = i;
    }
    return surfaceSet;
  }

  private int findSet(int vertex) {
    for (int i = 0; i < nSets; i++)
      if (surfaceSet[i] != null && surfaceSet[i].get(vertex))
        return i;
    return -1;
  }

  private void createSet(int v1, int v2, int v3) {
    int i;
    for (i = 0; i < nSets; i++)
      if (surfaceSet[i] == null)
        break;
    if (i >= 100) {
      setsSuccessful = false;
      return;
    }
    surfaceSet[i] = new BitSet();
    surfaceSet[i].set(v1);
    surfaceSet[i].set(v2);
    surfaceSet[i].set(v3);
    if (i == nSets)
      nSets++;
  }

  private void mergeSets(int a, int b) {
    surfaceSet[a].or(surfaceSet[b]);
    surfaceSet[b] = null;
  }
  
  public void invalidateSurfaceSet(int i) {
    for (int j = surfaceSet[i].length(); --j >= 0;)
      if (surfaceSet[i].get(j))
        vertexValues[j] = Float.NaN;
    surfaceSet[i] = null;
  }
  
  public static boolean checkCutoff(int iA, int iB, int iC, float[] vertexValues) {
    // never cross a +/- junction with a triangle in the case of orbitals, 
    // where we are using |psi| instead of psi for the surface generation.
    // note that for bicolor maps, where the values are all positive, we 
    // check this later in the meshRenderer
    if (iA < 0 || iB < 0 || iC < 0)
      return false;

    float val1 = vertexValues[iA];
    float val2 = vertexValues[iB];
    float val3 = vertexValues[iC];
    return (val1 >= 0 && val2 >= 0 && val3 >= 0 
        || val1 <= 0 && val2 <= 0 && val3 <= 0);
  }
  
  private boolean setABC(int i) {
    int[] vertexIndexes = polygonIndexes[i];
    return vertexIndexes != null
          && !(Float.isNaN(vertexValues[vertexIndexes[0]])
            || Float.isNaN(vertexValues[vertexIndexes[1]]) 
            || Float.isNaN(vertexValues[vertexIndexes[2]]));
  }
  

  public void invalidateTriangles() {
    for (int i = polygonCount; --i >= 0;)
      if (!setABC(i))
        polygonIndexes[i] = null;
  }
}

