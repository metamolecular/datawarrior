/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
package org.jmol.util;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;

final public class Measure {

  public final static float radiansPerDegree = (float) (2 * Math.PI / 360);

  
  
  public static float computeAngle(Tuple3f pointA, Tuple3f pointB, Tuple3f pointC, Vector3f vectorBA, Vector3f vectorBC, boolean asDegrees) {
    vectorBA.sub(pointA, pointB);
    vectorBC.sub(pointC, pointB);
    float angle = vectorBA.angle(vectorBC);
    return (asDegrees ? angle / radiansPerDegree : angle);
  }

  public static float computeAngle(Tuple3f pointA, Tuple3f pointB, Tuple3f pointC, boolean asDegrees) {
    Vector3f vectorBA = new Vector3f();
    Vector3f vectorBC = new Vector3f();        
    return Measure.computeAngle(pointA, pointB, pointC, vectorBA, vectorBC, asDegrees);
  }

  public static float computeTorsion(Tuple3f p1, Tuple3f p2, Tuple3f p3, Tuple3f p4, boolean asDegrees) {
  
    float ijx = p1.x - p2.x;
    float ijy = p1.y - p2.y;
    float ijz = p1.z - p2.z;
  
    float kjx = p3.x - p2.x;
    float kjy = p3.y - p2.y;
    float kjz = p3.z - p2.z;
  
    float klx = p3.x - p4.x;
    float kly = p3.y - p4.y;
    float klz = p3.z - p4.z;
  
    float ax = ijy * kjz - ijz * kjy;
    float ay = ijz * kjx - ijx * kjz;
    float az = ijx * kjy - ijy * kjx;
    float cx = kjy * klz - kjz * kly;
    float cy = kjz * klx - kjx * klz;
    float cz = kjx * kly - kjy * klx;
  
    float ai2 = 1f / (ax * ax + ay * ay + az * az);
    float ci2 = 1f / (cx * cx + cy * cy + cz * cz);
  
    float ai = (float) Math.sqrt(ai2);
    float ci = (float) Math.sqrt(ci2);
    float denom = ai * ci;
    float cross = ax * cx + ay * cy + az * cz;
    float cosang = cross * denom;
    if (cosang > 1) {
      cosang = 1;
    }
    if (cosang < -1) {
      cosang = -1;
    }
  
    float torsion = (float) Math.acos(cosang);
    float dot = ijx * cx + ijy * cy + ijz * cz;
    float absDot = Math.abs(dot);
    torsion = (dot / absDot > 0) ? torsion : -torsion;
    return (asDegrees ? torsion / radiansPerDegree : torsion);
  }

  public static Object computeHelicalAxis(String id, int tokType, Point3f a, Point3f b,
                                    Quaternion dq) {
    /*
                b
           |   /|
           |  / |
           | /  |
           |/   c
         b'+   / \
           |  /   \      Vcb = Vab . n
         n | /     \d    Vda = (Vcb - Vab) / 2
           |/theta  \
         a'+---------a
                r 
    */
    
    Vector3f vab = new Vector3f();
    vab.sub(b, a);
    /*
     * testing here to see if directing the normal makes any difference -- oddly
     * enough, it does not. When n = -n and theta = -theta vab.n is reversed,
     * and that magnitude is multiplied by n in generating the A'-B' vector.
     * 
     * a negative angle implies a left-handed axis (sheets)
     */
    Point4f aa = new Point4f();
    aa.x = vab.x;
    aa.y = vab.y;
    aa.z = vab.z;
    float theta = dq.getTheta();
    Vector3f n = dq.getNormal();
    aa.x = vab.x;
    aa.y = vab.y;
    aa.z = vab.z;
    float v_dot_n = vab.dot(n);
    if (Math.abs(v_dot_n) < 0.0001f)
      v_dot_n = 0;
    if (tokType == Token.axis) {
      if (v_dot_n != 0)
        n.scale(v_dot_n);
      return n;
    }
    Vector3f va_prime_d = new Vector3f();
    va_prime_d.cross(vab, n);
    if (va_prime_d.dot(va_prime_d) != 0)
      va_prime_d.normalize();
    Vector3f vda = new Vector3f();
    Vector3f vcb = new Vector3f(n);
    if (v_dot_n == 0)
      v_dot_n = Float.MIN_VALUE; // allow for perpendicular axis to vab
    vcb.scale(v_dot_n);
    vda.sub(vcb, vab);
    vda.scale(0.5f);
    va_prime_d.scale(theta == 0 ? 0 : (float) (vda.length() / Math
        .tan(theta / 2 / 180 * Math.PI)));
    Vector3f r = new Vector3f(va_prime_d);
    if (theta != 0)
      r.add(vda);
    if (tokType == Token.radius)
      return r;
    Point3f pt_a_prime = new Point3f(a);
    pt_a_prime.sub(r);
    if (tokType == Token.point) {
      return pt_a_prime;
    }
    if (v_dot_n != Float.MIN_VALUE)
      n.scale(v_dot_n);
    // must calculate directed angle:
    Point3f pt_b_prime = new Point3f(pt_a_prime);
    pt_b_prime.add(n);
    theta = computeTorsion(a, pt_a_prime, pt_b_prime, b, true);
    if (Float.isNaN(theta) || r.length() < 0.0001f) {
      aa.set(n.x, n.y, n.z, 0);
      dq.getThetaDirected(aa); // allow for r = 0
      theta = aa.w;
    }
    if (tokType == Token.angle)
      return new Float(theta);
    if (tokType == Token.draw)
      return "draw ID " + id + " VECTOR " + Escape.escape(pt_a_prime)
          + " " + Escape.escape(n) + " color "
          + (theta < 0 ? "{255.0 200.0 0.0}" : "{255.0 0.0 128.0}");
    if (tokType == Token.monitor)
      return "measure " + Escape.escape(a) + Escape.escape(pt_a_prime) + Escape.escape(pt_b_prime) + Escape.escape(b);
    // for now... array:
    float residuesPerTurn = Math.abs(theta == 0 ? 0 : 360f / theta);
    float pitch = Math.abs(v_dot_n == Float.MIN_VALUE ? 0 : n.length() * (theta == 0 ? 1 : 360f / theta));
    switch (tokType) {
    case Token.array:
      return new Object[] {pt_a_prime, n, r,  new Point3f(theta, pitch, residuesPerTurn)};
    case Token.list:
      return new String[] { 
          Escape.escape(pt_a_prime), // a' 
          Escape.escape(n), // n
          Escape.escape(r), // r
          Escape.escape(new Point3f(theta /*(degrees)*/,pitch, residuesPerTurn))
          };
    default:
      return null;
    }
  }

  public static Point4f getPlaneThroughPoints(Point3f pointA,
                                              Point3f pointB,
                                              Point3f pointC, Vector3f vNorm,
                                              Vector3f vAB, Vector3f vAC) {
    float w = getNormalThroughPoints(pointA, pointB, pointC, vNorm, vAB, vAC);
    return new Point4f(vNorm.x, vNorm.y, vNorm.z, w);
  }
  
  public static float distanceToPlane(Point4f plane, Point3f pt) {
    return (plane == null ? Float.NaN 
        : (plane.x * pt.x + plane.y * pt.y + plane.z * pt.z + plane.w)
        / (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z
            * plane.z));
  }

  public static float distanceToPlane(Point4f plane, float d, Point3f pt) {
    return (plane == null ? Float.NaN : (plane.x * pt.x + plane.y
        * pt.y + plane.z * pt.z + plane.w) / d);
  }

  public static float distanceToPlane(Vector3f norm, float w, Point3f pt) {
    return (norm == null ? Float.NaN 
        : (norm.x * pt.x + norm.y * pt.y + norm.z * pt.z + w)
        / (float) Math.sqrt(norm.x * norm.x + norm.y * norm.y + norm.z
            * norm.z));
  }

  public static void calcNormalizedNormal(Point3f pointA, Point3f pointB,
         Point3f pointC, Vector3f vNormNorm, Vector3f vAB, Vector3f vAC) {
    vAB.sub(pointB, pointA);
    vAC.sub(pointC, pointA);
    vNormNorm.cross(vAB, vAC);
    vNormNorm.normalize();
  }

  public static float getDirectedNormalThroughPoints(Point3f pointA, 
         Point3f pointB, Point3f pointC, Point3f ptRef, Vector3f vNorm, 
         Vector3f vAB, Vector3f vAC) {
    // for x = plane({atomno=1}, {atomno=2}, {atomno=3}, {atomno=4})
    float nd = getNormalThroughPoints(pointA, pointB, pointC, vNorm, vAB, vAC);
    if (ptRef != null) {
      Point3f pt0 = new Point3f(pointA);
      pt0.add(vNorm);
      float d = pt0.distance(ptRef);
      pt0.set(pointA);
      pt0.sub(vNorm);
      if (d > pt0.distance(ptRef)) {
        vNorm.scale(-1);
        nd = -nd;
      }
    }
    return nd;
  }
  
  public static float getNormalThroughPoints(Point3f pointA, Point3f pointB,
                                   Point3f pointC, Vector3f vNorm, Vector3f vAB, Vector3f vAC) {
    // for Polyhedra
    calcNormalizedNormal(pointA, pointB, pointC, vNorm, vAB, vAC);
    // ax + by + cz + d = 0
    // so if a point is in the plane, then N dot X = -d
    vAB.set(pointA);
    float d = -vAB.dot(vNorm);
    return d;
  }

  public static void getNormalFromCenter(Point3f ptCenter, Point3f ptA, Point3f ptB,
                            Point3f ptC, boolean isOutward, Vector3f normal) {
    // for Polyhedra
    Point3f ptT = new Point3f();
    Point3f ptT2 = new Point3f();
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    calcNormalizedNormal(ptA, ptB, ptC, normal, vAB, vAC);
    //but which way is it? add N to A and see who is closer to Center, A or N. 
    ptT.set(ptA);
    ptT.add(ptB);
    ptT.add(ptC);
    ptT.scale(1/3f);
    ptT2.set(normal);
    ptT2.scale(0.1f);
    ptT2.add(ptT);
    //              A      C         Bob Hanson 2006
    //                \   /
    //                 \ / 
    //                  x pT is center of ABC; ptT2 is offset a bit from that
    //                  |    either closer to x (ok if not opaque) or further
    //                  |    from x (ok if opaque)
    //                  B
    // in the case of facet ABx, the "center" is really the OTHER point, C.
    boolean doReverse = (isOutward && ptCenter.distance(ptT2) < ptCenter.distance(ptT)
        || !isOutward && ptCenter.distance(ptT) < ptCenter.distance(ptT2));
    if (doReverse)
      normal.scale(-1f);
  }

  public static void calcXYNormalToLine(Point3f pointA, Point3f pointB,
                                   Vector3f vNormNorm) {
    // vector in xy plane perpendicular to a line between two points RMH
    Vector3f axis = new Vector3f(pointA);
    axis.sub(pointB);
    vNormNorm.cross(axis, JmolConstants.axisY);
    vNormNorm.normalize();
    if (Float.isNaN(vNormNorm.x))
      vNormNorm.set(1, 0, 0);
  }
  
  public static void projectOntoAxis (Point3f point, Point3f axisA, Vector3f axisUnitVector, Vector3f vectorProjection) {
    vectorProjection.sub(point, axisA);
    float projectedLength = vectorProjection.dot(axisUnitVector);
    point.set(axisUnitVector);
    point.scaleAdd(projectedLength, axisA);
    vectorProjection.sub(point, axisA);
  }
  
  public static void calcBestAxisThroughPoints(Point3f[] points, Point3f axisA,
                                               Vector3f axisUnitVector,
                                               Vector3f vectorProjection,
                                               int nTriesMax) {
    // just a crude starting point.

    int nPoints = points.length;
    axisA.set(points[0]);
    axisUnitVector.sub(points[nPoints - 1], axisA);
    axisUnitVector.normalize();

    /*
     * We now calculate the least-squares 3D axis
     * through the helix alpha carbons starting with Vo
     * as a first approximation.
     * 
     * This uses the simple 0-centered least squares fit:
     * 
     * Y = M cross Xi
     * 
     * minimizing R^2 = SUM(|Y - Yi|^2) 
     * 
     * where Yi is the vector PERPENDICULAR of the point onto axis Vo
     * and Xi is the vector PROJECTION of the point onto axis Vo
     * and M is a vector adjustment 
     * 
     * M = SUM_(Xi cross Yi) / sum(|Xi|^2)
     * 
     * from which we arrive at:
     * 
     * V = Vo + (M cross Vo)
     * 
     * Basically, this is just a 3D version of a 
     * standard 2D least squares fit to a line, where we would say:
     * 
     * y = m xi + b
     * 
     * D = n (sum xi^2) - (sum xi)^2
     * 
     * m = [(n sum xiyi) - (sum xi)(sum yi)] / D
     * b = [(sum yi) (sum xi^2) - (sum xi)(sum xiyi)] / D
     * 
     * but here we demand that the line go through the center, so we
     * require (sum xi) = (sum yi) = 0, so b = 0 and
     * 
     * m = (sum xiyi) / (sum xi^2)
     * 
     * In 3D we do the same but 
     * instead of x we have Vo,
     * instead of multiplication we use cross products
     * 
     * A bit of iteration is necessary.
     * 
     * Bob Hanson 11/2006
     * 
     */

    Measure.calcAveragePointN(points, nPoints, axisA);

    int nTries = 0;
    while (nTries++ < nTriesMax
        && findAxis(points, nPoints, axisA, axisUnitVector, vectorProjection) > 0.001) {
    }

    /*
     * Iteration here gets the job done.
     * We now find the projections of the endpoints onto the axis
     * 
     */

    Point3f tempA = new Point3f(points[0]);
    projectOntoAxis(tempA, axisA, axisUnitVector, vectorProjection);
    axisA.set(tempA);
  }

  public static float findAxis(Point3f[] points, int nPoints, Point3f axisA,
                        Vector3f axisUnitVector, Vector3f vectorProjection) {
    Vector3f sumXiYi = new Vector3f();
    Vector3f vTemp = new Vector3f();
    Point3f pt = new Point3f();
    Point3f ptProj = new Point3f();
    Vector3f a = new Vector3f(axisUnitVector);

    float sum_Xi2 = 0;
    float sum_Yi2 = 0;
    for (int i = nPoints; --i >= 0;) {
      pt.set(points[i]);
      ptProj.set(pt);
      projectOntoAxis(ptProj, axisA, axisUnitVector,
          vectorProjection);
      vTemp.sub(pt, ptProj);
      sum_Yi2 += vTemp.lengthSquared();
      vTemp.cross(vectorProjection, vTemp);
      sumXiYi.add(vTemp);
      sum_Xi2 += vectorProjection.lengthSquared();
    }
    Vector3f m = new Vector3f(sumXiYi);
    m.scale(1 / sum_Xi2);
    vTemp.cross(m, axisUnitVector);
    axisUnitVector.add(vTemp);
    axisUnitVector.normalize();
    //check for change in direction by measuring vector difference length
    vTemp.set(axisUnitVector);
    vTemp.sub(a);
    return vTemp.length();
  }
  
  
  public static void calcAveragePoint(Point3f pointA, Point3f pointB,
                                      Point3f pointC) {
    pointC.set((pointA.x + pointB.x) / 2, (pointA.y + pointB.y) / 2,
        (pointA.z + pointB.z) / 2);
  }
  
  public static void calcAveragePointN(Point3f[] points, int nPoints,
                                Point3f averagePoint) {
    averagePoint.set(points[0]);
    for (int i = 1; i < nPoints; i++)
      averagePoint.add(points[i]);
    averagePoint.scale(1f / nPoints);
  }

}
