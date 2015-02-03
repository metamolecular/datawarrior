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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.jvxl.readers;

import java.io.BufferedReader;

import javax.vecmath.Point3f;

import org.jmol.util.BinaryDocument;
import org.jmol.util.Logger;

/*
 * 
 * ASCII format:

 100
 3.0000 3.0000 1.0000
 2.3333 3.0000 1.0000
 ...(98 more like this)
 81
 5
 0
 10
 11
 1
 0
 ...(80 more sets like this)

 * The first line defines the number of grid points 
 *   defining the surface (integer, n)
 * The next n lines define the Cartesian coordinates 
 *   of each of the grid points (n lines of x, y, z floating point data points)
 * The next line specifies the number of polygons, m, to be drawn (81 in this case).
 * The next m sets of numbers, one number per line, 
 *   define the polygons. In each set, the first number, p, specifies 
 *   the number of points in each set. Currently this number must be either 
 *   4 (for triangles) or 5 (for quadrilaterals). The next p numbers specify 
 *   indexes into the list of data points (starting with 0). 
 *   The first and last of these numbers must be identical in order to 
 *   "close" the polygon.
 * 
 * Jmol does not care about lines. 
 * 
 * Binary format: 
 * 
 * note that there is NO redundant extra vertex in this format 
 *
 *  4 bytes: P M \1 \0 
 *  4 bytes: (int) 1 -- first byte used to determine big(==0) or little(!=0) endian
 *  4 bytes: (int) nVertices
 *  4 bytes: (int) nPolygons
 * 64 bytes: reserved
 *  ------------------------------
 *  float[nVertices*3]vertices {x,y,z}
 *  [nPolygons] polygons 
 *  --each polygon--
 *    4 bytes: (int)nVertices (1,2,3, or 4)
 *    [4 bytes * nVertices] int[nVertices]
 *    
 *
 */


class PmeshReader extends PolygonFileReader {

  private int nPolygons;
  private boolean isBinary;
  final static String PMESH_BINARY_MAGIC_NUMBER = "PM" + '\1' + '\0';
  String pmeshError;

  PmeshReader(SurfaceGenerator sg, String fileName, BufferedReader br) {
    super(sg, br);
    jvxlFileHeaderBuffer = new StringBuffer();
    jvxlFileHeaderBuffer
        .append("pmesh file format\nvertices and triangles only\n");
    JvxlReader.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData,
        jvxlFileHeaderBuffer);
    isBinary = checkBinary(fileName);
  }

  private boolean checkBinary(String fileName) {
    try {
      br.mark(4);
      char[] buf = new char[5];
      br.read(buf);
      if ((new String(buf)).startsWith(PMESH_BINARY_MAGIC_NUMBER)) {
        br.close();
        binarydoc = new BinaryDocument();
        binarydoc.setStream(sg.getAtomDataServer().getBufferedInputStream(
            fileName), (buf[4] == '\0'));
        return true;
      }
      br.reset();
    } catch (Exception e) {
    }
    return false;
  }

  void getSurfaceData() throws Exception {
    if (readPmesh())
      Logger.info((isBinary ? "binary " : "") + "pmesh file contains "
          + nVertices + " vertices and " + nPolygons + " polygons for "
          + nTriangles + " triangles");
    else
      Logger.error(params.fileName + ": " 
          + (pmeshError == null ? "Error reading pmesh data "
              : pmeshError));
  }

  private boolean readPmesh() {
    try {
      if (isBinary && !readBinaryHeader())
        return false;
      if (readVertices() && readPolygons())
        return true;
    } catch (Exception e) {
      if (pmeshError == null)
        pmeshError = "pmesh ERROR: " + e;
    }
    return false;
  }

  boolean readBinaryHeader() {
    pmeshError = "could not read binary Pmesh file header";
    try {
      byte[] ignored = new byte[64];
      binarydoc.readByteArray(ignored, 0, 8);
      nVertices = binarydoc.readInt();
      nPolygons = binarydoc.readInt();
      binarydoc.readByteArray(ignored, 0, 64);
    } catch (Exception e) {
      pmeshError += " " + e.getMessage();
      binarydoc.close();
      return false;
    }
    pmeshError = null;
    return true;
  }

  private boolean readVertices() throws Exception {
    pmeshError = "pmesh ERROR: vertex count must be positive";
    if (!isBinary)
      nVertices = getInt();
    if (nVertices <= 0) {
      pmeshError += " (" + nVertices + ")";
      return false;
    }
    pmeshError = "pmesh ERROR: invalid vertex list";
    Point3f pt = new Point3f();
    for (int i = 0; i < nVertices; i++) {
      pt.set(getFloat(), getFloat(), getFloat());
      addVertexCopy(pt, 0, i);
    }
    pmeshError = null;
    return true;
  }

  private boolean readPolygons() throws Exception {
    pmeshError = "pmesh ERROR: polygon count must be zero or positive";
    if (!isBinary)
      nPolygons = getInt();
    if (nPolygons < 0) {
      pmeshError += " (" + nPolygons + ")";
      return false;
    }
    int[] vertices = new int[5];
    for (int iPoly = 0; iPoly < nPolygons; iPoly++) {
      int intCount = getInt();
      int vertexCount = intCount - (isBinary ? 0 : 1);
      // (we will ignore the redundant extra vertex when not binary)
      if (vertexCount < 1 || vertexCount > 4) {
        pmeshError = "pmesh ERROR: bad polygon (must have 1-4 vertices) at #"
            + (iPoly + 1);
        return false;
      }
      for (int i = 0; i < intCount; ++i)
        if ((vertices[i] = getInt()) < 0 || vertices[i] >= nVertices) {
          pmeshError = "pmesh ERROR: invalid vertex index: " + vertices[i];
          return false;
        }
      // allow for point or line definition here
      if (vertexCount < 3)
        for (int i = vertexCount; i < 3; ++i)
          vertices[i] = vertices[i - 1];
      // check: 1 (ab) | 2(bc) | 4(ac)
      //    1
      //  a---b
      // 4 \ / 2
      //    c
      //
      //    1
      //  a---b      b
      // 4 \     +    \ 1 
      //    d      d---c
      //             2
      if (vertexCount == 4) {
        nTriangles += 2;
        addTriangleCheck(vertices[0], vertices[1], vertices[3], 5, false, 0);
        addTriangleCheck(vertices[1], vertices[2], vertices[3], 3, false, 0);
      } else {
        nTriangles++;
        addTriangleCheck(vertices[0], vertices[1], vertices[2], 7, false, 0);
      }
    }
    return true;
  }

  //////////// file reading

  private String[] tokens = new String[0];
  private int iToken = 0;

  private String nextToken() throws Exception {
    while (iToken >= tokens.length) { 
      iToken = 0;
      line = br.readLine();
      tokens = getTokens();
    }
    return tokens[iToken++];
  }

  private int getInt() throws Exception {
    return (isBinary ? binarydoc.readInt() : parseInt(nextToken()));
  }

  private float getFloat() throws Exception {
    return (isBinary ? binarydoc.readFloat() : parseFloat(nextToken()));
  }
}
