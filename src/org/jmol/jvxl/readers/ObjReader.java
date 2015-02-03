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

import org.jmol.g3d.Graphics3D;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

/*
 * 
 * See at http://www.eg-models.de/formats/Format_Obj.html

# 6825 vertices 12620 faces 9 groups

mtllib g_visible.mtl

v 111.025230 65.735298 2.483954
v 111.221596 65.772804 2.270540
v 111.046539 65.643066 2.200877
...
g k000000
usemtl k000000
f 1 2 3
f 1 3 4
f 1 4 5

g k0066FF
usemtl k0066FF
f 6 7 8
f 8 7 9
...
f 6825 6805 6824
f 6821 6808 6825
f 6808 6825 6804
f 6824 6807 6822
f 6807 6822 6815
f 6805 6824 6806
f 6806 6824 6807

 * just looking for v, g, and f. 
 * Groups are designated as any multi-isosurface file using an
 * integer after the file name. In this case, no integer
 * means "read all the data"
 *
 */


class ObjReader extends PolygonFileReader {

  private int nPolygons;
  String pmeshError;

  ObjReader(SurfaceGenerator sg, String fileName, BufferedReader br) {
    super(sg, br);
    jvxlFileHeaderBuffer = new StringBuffer();
    jvxlFileHeaderBuffer
        .append("pmesh file format\nvertices and triangles only\n");
    JvxlReader.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData,
        jvxlFileHeaderBuffer);
  }

  void getSurfaceData() throws Exception {
    if (readPmesh())
      Logger.info("obj file contains "
          + nVertices + " vertices and " + nPolygons + " polygons for "
          + nTriangles + " triangles");
    else
      Logger.error(params.fileName + ": " 
          + (pmeshError == null ? "Error reading obj data "
              : pmeshError));
  }

  private boolean readPmesh() {
    try {
      if (readVertices() && readPolygons())
        return true;
    } catch (Exception e) {
      if (pmeshError == null)
        pmeshError = "pmesh ERROR: " + e;
    }
    return false;
  }

  Point3f pt = new Point3f();
  private boolean readVertices() throws Exception {
    pmeshError = "pmesh ERROR: invalid vertex list";
    Point3f pt = new Point3f();
    while ((line = br.readLine()) != null) {
      if (line.length() == 0 || nVertices == 0 && line.indexOf("v ") != 0)
        continue;
      if (line.indexOf("v ") != 0)
        break;
      next[0] = 2;
      pt.set(Parser.parseFloat(line, next), Parser.parseFloat(line, next), Parser.parseFloat(line, next));
      addVertexCopy(pt, 0, ++nVertices);
    }
    pmeshError = null;
    return true;
  }

  private boolean readPolygons() {
    nPolygons = 0;
    int color = 0;
    try {
      if (!params.readAllData) {
        for (int i = 0; i < params.fileIndex; i++) {
          while (line != null && line.indexOf("g ") != 0)
            line = br.readLine();
          if (line == null)
            break;
          color = Graphics3D.getArgbFromString("[x" + line.substring(3) + "]");
          //System.out.println("[x" + line.substring(3) + "]" + " " + color);
          line = br.readLine();
        }
      }

      while (line != null) {
        if (line.indexOf("f ") == 0) {
          nPolygons++;
          next[0] = 2;
          int ia = Parser.parseInt(line, next);
          int ib = Parser.parseInt(line, next);
          int ic = Parser.parseInt(line, next);
          int id = Parser.parseInt(line, next);
          int vertexCount = (id == Integer.MIN_VALUE ? 3 : 4);
          if (vertexCount == 4) {
            nTriangles += 2;
            addTriangleCheck(ia - 1, ib - 1, ic - 1, 5, false, color);
            addTriangleCheck(ib - 1, ic - 1, id - 1, 3, false, color);
          } else {
            nTriangles++;
            addTriangleCheck(ia - 1, ib - 1, ic - 1, 7, false, color);
          }
        } else if (line.indexOf("g ") == 0) {
          if (!params.readAllData)
            break;
          color = Graphics3D.getArgbFromString("[x" + line.substring(3) + "]");
        }
        line = br.readLine();
      }
    } catch (Exception e) {
      if (line != null)
        pmeshError = "problem reading OBJ file at: " + line;
      // normal;
    }
    return true;
  }
}
