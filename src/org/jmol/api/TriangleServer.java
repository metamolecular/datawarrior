package org.jmol.api;

import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;

public interface TriangleServer {

  /**
   * a generic cell - plane intersector -- used for finding the plane through a
   * unit cell
   * 
   * @param plane
   * @param vertices
   * @param flags     1 -- edges only   2 -- triangles only   3 -- both
   * @return Vector of Point3f[3] triangles and Point3f[2] edge lines
   */
  
  public Point3i[] getCubeVertexOffsets();
  
  public Vector intersectPlane(Point4f plane, Point3f[] vertices, int flags);

}
