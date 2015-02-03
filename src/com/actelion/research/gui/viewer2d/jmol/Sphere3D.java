/* $RCSfile: Sphere3D.java,v $
 * $Author: nicove $
 * $Date: 2004/11/12 09:38:02 $
 * $Revision: 1.15 $
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package com.actelion.research.gui.viewer2d.jmol;

/**
 *<p>
 * Implements high performance rendering of shaded spheres.
 *</p>
 *<p>
 * Drawing spheres quickly is critically important to Jmol.
 * These routines implement high performance rendering of
 * spheres in 3D.
 *</p>
 *<p>
 * If you can think of a faster way to implement this, please
 * let us know.
 *</p>
 *<p>
 * There is a lot of bit-twiddling going on here, which may
 * make the code difficult to understand for non-systems programmers.
 *</p>
 */
class Sphere3D {

  Graphics3D g3d;

  Sphere3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  void render(short colix, int diameter, int x, int y, int z) {
    int[] shades = g3d.getShades(colix);
    if (diameter >= maxSphereCache) {
      renderLargeSphere(shades, diameter, x, y, z);
      return;
    }
    if (diameter > maxOddSizeSphere)
      diameter &= ~1;
    int radius = (diameter + 1) >> 1;
    int minX = x - radius, maxX = x + radius;
    int minY = y - radius, maxY = y + radius;
    int minZ = z - radius;
    if (maxX < 0 || minX >= g3d.width ||
        maxY < 0 || minY >= g3d.height ||
        z < g3d.slab || minZ > g3d.depth)
      return;
    int[] ss = getSphereShape(diameter);
    if (minX < 0 || maxX >= g3d.width ||
        minY < 0 || maxY >= g3d.height ||
        minZ < g3d.slab || z > g3d.depth)
      renderShapeClipped(shades, ss, diameter, x, y, z);
    else
      renderShapeUnclipped(shades, ss, diameter, x, y, z);
  }
  
  void renderShapeUnclipped(int[] shades, int[] sphereShape,
                            int diameter, int x, int y, int z) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int offsetSphere = 0;
    int width = g3d.width;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = width * y + x;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    do {
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      do {
        packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if (zPixel < zbuf[offsetSE]) {
          zbuf[offsetSE] = (short)zPixel;
          pbuf[offsetSE] = shades[(packed >> 7) & 0x3F];
        }
        if (zPixel < zbuf[offsetSW]) {
          zbuf[offsetSW] = (short)zPixel;
          pbuf[offsetSW] = shades[(packed >> 13) & 0x3F];
        }
        if (zPixel < zbuf[offsetNE]) {
          zbuf[offsetNE] = (short)zPixel;
          pbuf[offsetNE] = shades[(packed >> 19) & 0x3F];
        }
        if (zPixel < zbuf[offsetNW]) {
          zbuf[offsetNW] = (short)zPixel;
          pbuf[offsetNW] = shades[(packed >> 25) & 0x3F];
        }
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
    } while (--nLines > 0);
  }

  void renderShapeClipped(int[] shades, int[] sphereShape,
                          int diameter, int x, int y, int z) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int offsetSphere = 0;
    int width = g3d.width, height = g3d.height;
    int slab = g3d.slab, depth = g3d.depth;
    int evenSizeCorrection = 1 - (diameter & 1);
    int offsetSouthCenter = width * y + x;
    int offsetNorthCenter = offsetSouthCenter - evenSizeCorrection * width;
    int nLines = (diameter + 1) / 2;
    int ySouth = y;
    int yNorth = y - evenSizeCorrection;
    do {
      boolean tSouthVisible = ySouth >= 0 && ySouth < height;
      boolean tNorthVisible = yNorth >= 0 && yNorth < height;
      int offsetSE = offsetSouthCenter;
      int offsetSW = offsetSouthCenter - evenSizeCorrection;
      int offsetNE = offsetNorthCenter;
      int offsetNW = offsetNorthCenter - evenSizeCorrection;
      int packed;
      int xEast = x;
      int xWest = x - evenSizeCorrection;
      do {
        boolean tWestVisible = xWest >= 0 && xWest < width;
        boolean tEastVisible = xEast >= 0 && xEast < width;
        packed = sphereShape[offsetSphere++];
        int zPixel = z - (packed & 0x7F);
        if (zPixel >= slab && zPixel <= depth) {
          if (tSouthVisible) {
            if (tEastVisible && zPixel < zbuf[offsetSE]) {
              zbuf[offsetSE] = (short)zPixel;
              pbuf[offsetSE] = shades[(packed >> 7) & 0x3F];
            }
            if (tWestVisible && zPixel < zbuf[offsetSW]) {
              zbuf[offsetSW] = (short)zPixel;
              pbuf[offsetSW] = shades[(packed >> 13) & 0x3F];
            }
          }
          if (tNorthVisible) {
            if (tEastVisible && zPixel < zbuf[offsetNE]) {
              zbuf[offsetNE] = (short)zPixel;
              pbuf[offsetNE] = shades[(packed >> 19) & 0x3F];
            }
            if (tWestVisible && zPixel < zbuf[offsetNW]) {
              zbuf[offsetNW] = (short)zPixel;
              pbuf[offsetNW] = shades[(packed >> 25) & 0x3F];
            }
          }
        }
        ++offsetSE;
        --offsetSW;
        ++offsetNE;
        --offsetNW;
        ++xEast;
        --xWest;
      } while (packed >= 0);
      offsetSouthCenter += width;
      offsetNorthCenter -= width;
      ++ySouth;
      --yNorth;
    } while (--nLines > 0);
  }

  final static int maxSphereCache = 128;
  final static int maxOddSizeSphere = 49;
  static int[][] sphereShapeCache = new int[maxSphereCache][];

  int[] getSphereShape(int diameter) {
    int[] ss;
    if (diameter > maxSphereCache)
      diameter = maxSphereCache;
    ss = sphereShapeCache[diameter - 1];
    if (ss != null)
      return ss;
    ss = sphereShapeCache[diameter - 1] = createSphereShape(diameter);
    return ss;
  }

  static void flushImageCache() {
    sphereShapeCache = new int[maxSphereCache][];
  }

  int[] createSphereShape(int diameter) {
    int countSE = 0;
    boolean oddDiameter = (diameter & 1) != 0;
    float radiusF = diameter / 2.0f;
    float radiusF2 = radiusF * radiusF;
    int radius = (diameter + 1) / 2;

    float y = oddDiameter ? 0 : 0.5f;
    for (int i = 0; i < radius; ++i, ++y) {
      float y2 = y * y;
      float x = oddDiameter ? 0 : 0.5f;
      for (int j = 0; j < radius; ++j, ++x) {
        float x2 = x * x;
        float z2 = radiusF2 - y2 - x2;
        if (z2 >= 0)
          ++countSE;
      }
    }
    
    int[] sphereShape = new int[countSE];
    int offset = 0;

    y = oddDiameter ? 0 : 0.5f;
    for (int i = 0; i < radius; ++i, ++y) {
      float y2 = y * y;
      float x = oddDiameter ? 0 : 0.5f;
      for (int j = 0; j < radius; ++j, ++x) {
        float x2 = x * x;
        float z2 = radiusF2 - y2 - x2;
        if (z2 >= 0) {
          float z = (float)Math.sqrt(z2);
          int height = (int)z;
          int intensitySE = Shade3D.calcDitheredNoisyIntensity( x,  y, z, radiusF);
          int intensitySW = Shade3D.calcDitheredNoisyIntensity(-x,  y, z, radiusF);
          int intensityNE = Shade3D.calcDitheredNoisyIntensity( x, -y, z, radiusF);
          int intensityNW = Shade3D.calcDitheredNoisyIntensity(-x, -y, z, radiusF);
          int packed = (height |
                        (intensitySE << 7) |
                        (intensitySW << 13) |
                        (intensityNE << 19) |
                        (intensityNW << 25));
          sphereShape[offset++] = packed;
        }
      }
      sphereShape[offset - 1] |= 0x80000000;
    }
    return sphereShape;
  }

  ////////////////////////////////////////////////////////////////
  // Sphere shading cache for Large spheres
  ////////////////////////////////////////////////////////////////

  static boolean sphereShadingCalculated = false;
  final static byte[] sphereIntensities = new byte[256 * 256];

  void calcSphereShading() {
    if (! sphereShadingCalculated) {
      float xF = -127.5f;
      for (int i = 0; i < 256; ++xF, ++i) {
        float yF = -127.5f;
        for (int j = 0; j < 256; ++yF, ++j) {
          byte intensity = 0;
          float z2 = 130*130 - xF*xF - yF*yF;
          if (z2 > 0) {
            float z = (float)Math.sqrt(z2);
            intensity = Shade3D.calcDitheredNoisyIntensity(xF, yF, z, 130);
          }
          sphereIntensities[(j << 8) + i] = intensity;
        }
      }
      sphereShadingCalculated = true;
    }
  }

  static byte calcSphereIntensity(int x, int y, int r) {
    int d = 2*r + 1;
    x += r;
    if (x < 0)
      x = 0;
    int x8 = (x << 8) / d;
    if (x8 > 0xFF)
      x8 = 0xFF;
    y += r;
    if (y < 0)
      y = 0;
    int y8 = (y << 8) / d;
    if (y8 > 0xFF)
      y8 = 0xFF;
    return sphereIntensities[(y8 << 8) + x8];
  }

  void renderLargeSphere(int[] shades, int diameter, int x, int y, int z) {
    if (! sphereShadingCalculated)
      calcSphereShading();
    renderQuadrant(shades, diameter, x, y, z, -1, -1);
    renderQuadrant(shades, diameter, x, y, z, -1,  1);
    renderQuadrant(shades, diameter, x, y, z,  1, -1);
    renderQuadrant(shades, diameter, x, y, z,  1,  1);
  }


  void renderQuadrant(int[] shades, int diameter,
                      int x, int y, int z,
                      int xSign, int ySign) {
    int xStatus = (x < 0) ? -1 : (x < g3d.width) ? 0 : 1;
    int yStatus = (y < 0) ? -1 : (y < g3d.height) ? 0 : 1;
    int zStatus = (z < g3d.depth) ? 0 : 1;
    int r = diameter / 2;
    int x2 = x + r * xSign;
    int x2Status = (x2 < 0) ? -1 : (x2 < g3d.width) ? 0 : 1;
    int y2 = y + r * ySign;
    int y2Status = (y2 < 0) ? -1 : (y2 < g3d.height) ? 0 : 1;
    int z2Status = (z < g3d.slab) ? -1 : 0;


    if (xStatus < 0 && x2Status < 0 || xStatus > 0 && x2Status > 0 ||
        yStatus < 0 && y2Status < 0 || yStatus > 0 && y2Status > 0)
      return;
    if (xStatus == 0 && x2Status == 0 &&
        yStatus == 0 && y2Status == 0 &&
        zStatus == 0 && z2Status == 0)
      renderQuadrantUnclipped(shades, diameter, x, y, z, xSign, ySign);
    else
      renderQuadrantClipped(shades, diameter, x, y, z, xSign, ySign);
  }

  void renderQuadrantUnclipped(int[] shades, int diameter,
                               int x, int y, int z,
                               int xSign, int ySign) {
    int r = diameter / 2;
    int r2 = r * r;
    int dDivisor = r * 2 + 1;

    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int offsetPbufBeginLine = width * y + x;
    if (ySign < 0)
      width = -width;
    offsetPbufBeginLine -= width;
    for (int i = 0, i2 = 0; i2 <= r2; i2 += i + i + 1, ++i) {
      int offsetPbuf = (offsetPbufBeginLine += width) - xSign;
      int s2 = r2 - i2;
      int z0 = z - r;
      int y8 = ((i * ySign + r) << 8) / dDivisor;
      for (int j = 0, j2 = 0; j2 <= s2;
           j2 += j + j + 1, ++j) {
        if (zbuf[offsetPbuf += xSign] <= z0)
          continue;
        int k = (int)Math.sqrt(s2 - j2);
        z0 = z - k;
        if (zbuf[offsetPbuf] <= z0)
          continue;
        int x8 = ((j * xSign + r) << 8) / dDivisor;
        pbuf[offsetPbuf] = shades[sphereIntensities[(y8 << 8) + x8]];
        zbuf[offsetPbuf] = (short) z0;
      }
    }
  }

  void renderQuadrantClipped(int[] shades, int diameter,
                             int x, int y, int z,
                             int xSign, int ySign) {
    int r = diameter / 2;
    int r2 = r * r;
    int dDivisor = r * 2 + 1;

    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int slab = g3d.slab;
    int depth = g3d.depth;
    int height = g3d.height;
    int width = g3d.width;
    int offsetPbufBeginLine = width * y + x;
    int lineIncrement = width;
    if (ySign < 0)
      lineIncrement = -width;
    int yCurrent = y - ySign;
    for (int i = 0, i2 = 0;
         i2 <= r2;
         i2 += i + i + 1, ++i, offsetPbufBeginLine += lineIncrement) {
      yCurrent += ySign;
      if (yCurrent < 0) {
        if (ySign < 0)
          return;
        continue;
      }
      if (yCurrent >= height) {
        if (ySign > 0)
          return;
        continue;
      }
      int offsetPbuf = offsetPbufBeginLine;
      int s2 = r2 - i2;
      int z0 = z - r;
      int xCurrent = x - xSign;
      int y8 = ((i * ySign + r) << 8) / dDivisor;
      for (int j = 0, j2 = 0; j2 <= s2;
           j2 += j + j + 1, ++j, offsetPbuf += xSign) {
        xCurrent += xSign;
        if (xCurrent < 0) {
          if (xSign < 0)
            break;
          continue;
        }
        if (xCurrent >= width) {
          if (xSign > 0)
            break;
          continue;
        }
        if (zbuf[offsetPbuf] <= z0)
          continue;
        int k = (int)Math.sqrt(s2 - j2);
        z0 = z - k;
        if (z0 < slab || z0 > depth || zbuf[offsetPbuf] <= z0)
          continue;
        int x8 = ((j * xSign + r) << 8) / dDivisor;
        pbuf[offsetPbuf] = shades[sphereIntensities[(y8 << 8) + x8]];
        zbuf[offsetPbuf] = (short) z0;
      }
    }
  }
}
