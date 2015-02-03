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

import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

import org.jmol.api.VolumeDataInterface;

public class VolumeData implements VolumeDataInterface {

  public final Point3f volumetricOrigin = new Point3f();
  public final float[] origin = new float[3];
  public final Vector3f[] volumetricVectors = new Vector3f[3];
  public final int[] voxelCounts = new int[3];
  public int nPoints;
  public float[][][] voxelData;  
  public final float[] volumetricVectorLengths = new float[3];
  public final Vector3f[] unitVolumetricVectors = new Vector3f[3];
  private final Matrix3f volumetricMatrix = new Matrix3f();
  private Point4f thePlane;
  private float thePlaneNormalMag;
  private final Point3f ptXyzTemp = new Point3f();
  private final Vector3f pointVector = new Vector3f();
  
  public VolumeData() {
    volumetricVectors[0] = new Vector3f();
    volumetricVectors[1] = new Vector3f();
    volumetricVectors[2] = new Vector3f();
    unitVolumetricVectors[0] = new Vector3f();
    unitVolumetricVectors[1] = new Vector3f();
    unitVolumetricVectors[2] = new Vector3f();
  }

  public void setVolumetricOrigin(float x, float y, float z) {
    volumetricOrigin.set(x, y, z);
  }

  public float[] getOriginFloat() {
    return origin;
  }

  public float[] getVolumetricVectorLengths() {
    return volumetricVectorLengths;
  }
  
  public void setVolumetricVector(int i, float x, float y, float z) {
    volumetricVectors[i].x = x;
    volumetricVectors[i].y = y;
    volumetricVectors[i].z = z;
  }

  public int[] getVoxelCounts() {
    return voxelCounts;
  }
  
  public int setVoxelCounts(int nPointsX, int nPointsY, int nPointsZ) {
    voxelCounts[0] = nPointsX;
    voxelCounts[1] = nPointsY;
    voxelCounts[2] = nPointsZ;
    return nPoints = nPointsX * nPointsY * nPointsZ;
  }

  public float[][][] getVoxelData() {
    return voxelData;
  }
  
  public void setVoxelData(float[][][] voxelData) {
    this.voxelData = voxelData;
  }

  public void setMatrix() {
    for (int i = 0; i < 3; i++)
      volumetricMatrix.setColumn(i, volumetricVectors[i]);
  }

  public void transform(Vector3f v1, Vector3f v2) {
    volumetricMatrix.transform(v1, v2);
  }

  public void setPlaneParameters(Point4f plane) {
    thePlane = plane;
    thePlaneNormalMag = (new Vector3f(plane.x, plane.y, plane.z)).length();
  }

  public float calcVoxelPlaneDistance(int x, int y, int z) {
    voxelPtToXYZ(x, y, z, ptXyzTemp);
    return (thePlane.x * ptXyzTemp.x + thePlane.y * ptXyzTemp.y + thePlane.z
        * ptXyzTemp.z + thePlane.w)
        / thePlaneNormalMag;
  }

  public float distancePointToPlane(Point3f pt) {
    return (thePlane.x * pt.x + thePlane.y * pt.y + thePlane.z * pt.z + thePlane.w)
        / thePlaneNormalMag;
  }

  public void voxelPtToXYZ(int x, int y, int z, Point3f pt) {
    pt.scaleAdd(x, volumetricVectors[0], volumetricOrigin);
    pt.scaleAdd(y, volumetricVectors[1], pt);
    pt.scaleAdd(z, volumetricVectors[2], pt);
  }

  public void setUnitVectors() {
    for (int i = 0; i < 3; i++) {
      volumetricVectorLengths[i] = volumetricVectors[i].length();
      unitVolumetricVectors[i].normalize(volumetricVectors[i]);
    }
    origin[0] = volumetricOrigin.x;
    origin[1] = volumetricOrigin.y;
    origin[2] = volumetricOrigin.z;
  }

  private float scaleByVoxelVector(Vector3f vector, int voxelVectorIndex) {
    // ORTHOGONAL ONLY!!! -- required for creating planes
    return (vector.dot(unitVolumetricVectors[voxelVectorIndex]) / volumetricVectorLengths[voxelVectorIndex]);
  }

  public void xyzToVoxelPt(float x, float y, float z, Point3i pt3i) {
    pointVector.set(x, y, z);
    setVoxelPoint();
    pt3i.set((int) ptXyzTemp.x, (int) ptXyzTemp.y, (int) ptXyzTemp.z);
  }

  private void setVoxelPoint() {
    pointVector.sub(volumetricOrigin);
    ptXyzTemp.x = scaleByVoxelVector(pointVector, 0);
    ptXyzTemp.y = scaleByVoxelVector(pointVector, 1);
    ptXyzTemp.z = scaleByVoxelVector(pointVector, 2);
  }
  
  public float lookupInterpolatedVoxelValue(Point3f point) {
    //ARGH!!! ONLY FOR ORTHOGONAL AXES!!!!!
    //the dot product presumes axes are PERPENDICULAR.
    pointVector.set(point);
    setVoxelPoint();
    return getInterpolatedVoxelValue(ptXyzTemp);
  }

  private float getInterpolatedVoxelValue(Point3f pt) {
    int iMax;
    int xDown = indexDown(pt.x, iMax = voxelCounts[0] - 1);
    int xUp = xDown + (pt.x < 0 || xDown == iMax ? 0 : 1);
    int yDown = indexDown(pt.y, iMax = voxelCounts[1] - 1);
    int yUp = yDown + (pt.y < 0 || yDown == iMax ? 0 : 1);
    int zDown = indexDown(pt.z, iMax = voxelCounts[2] - 1);
    int zUp = zDown + (pt.z < 0 || zDown == iMax ? 0 : 1);
    float v1 = getFractional2DValue(pt.x - xDown, pt.y - yDown,
        voxelData[xDown][yDown][zDown], voxelData[xUp][yDown][zDown],
        voxelData[xDown][yUp][zDown], voxelData[xUp][yUp][zDown]);
    float v2 = getFractional2DValue(pt.x - xDown, pt.y - yDown,
        voxelData[xDown][yDown][zUp], voxelData[xUp][yDown][zUp],
        voxelData[xDown][yUp][zUp], voxelData[xUp][yUp][zUp]);
    return v1 + (pt.z - zDown) * (v2 - v1);
  }

  public static float getFractional2DValue(float fx, float fy, float x11,
                                           float x12, float x21, float x22) {
    float v1 = x11 + fx * (x12 - x11);
    float v2 = x21 + fx * (x22 - x21);
    return v1 + fy * (v2 - v1);
  }

  private static int indexDown(float value, int iMax) {
    if (value < 0)
      return 0;
    int floor = (int) value;
    return (floor > iMax ? iMax : floor);
  }

  void offsetCenter(Point3f center) {
    Point3f pt = new Point3f();
    pt.scaleAdd((voxelCounts[0] - 1) / 2f, volumetricVectors[0], pt);
    pt.scaleAdd((voxelCounts[1] - 1) / 2f, volumetricVectors[1], pt);
    pt.scaleAdd((voxelCounts[2] - 1) / 2f, volumetricVectors[2], pt);
    volumetricOrigin.sub(center, pt);
  }

  public void setDataDistanceToPlane(Point4f plane) {
    setPlaneParameters(plane);
    int nx = voxelCounts[0];
    int ny = voxelCounts[1];
    int nz = voxelCounts[2];
    voxelData = new float[nx][ny][nz];
    for (int x = 0; x < nx; x++)
      for (int y = 0; y < ny; y++)
        for (int z = 0; z < nz; z++)
          voxelData[x][y][z] = calcVoxelPlaneDistance(x, y, z);
  }

  public void filterData(boolean isSquared, float invertCutoff) {
    boolean doInvert = (!Float.isNaN(invertCutoff));
    int nx = voxelCounts[0];
    int ny = voxelCounts[1];
    int nz = voxelCounts[2];
    if (isSquared) 
      for (int x = 0; x < nx; x++)
        for (int y = 0; y < ny; y++)
          for (int z = 0; z < nz; z++)
            voxelData[x][y][z] = voxelData[x][y][z] * voxelData[x][y][z];
    if (doInvert) 
      for (int x = 0; x < nx; x++)
        for (int y = 0; y < ny; y++)
          for (int z = 0; z < nz; z++)
            voxelData[x][y][z] = invertCutoff - voxelData[x][y][z];
  }

  public void capData(Point4f plane, float cutoff) {
    int nx = voxelCounts[0];
    int ny = voxelCounts[1];
    int nz = voxelCounts[2];
    Vector3f normal = new Vector3f(plane.x, plane.y, plane.z);
    normal.normalize();
    float f = 1f;
    for (int x = 0; x < nx; x++)
      for (int y = 0; y < ny; y++)
        for (int z = 0; z < nz; z++) {
          float value = voxelData[x][y][z] - cutoff;
          voxelPtToXYZ(x, y, z, ptXyzTemp);
          float d = (ptXyzTemp.x * normal.x + ptXyzTemp.y * normal.y + ptXyzTemp.z * normal.z + plane.w - cutoff) / f;
          if (d >= 0 || d > value)
            voxelData[x][y][z] = d;
        }
  }

}
