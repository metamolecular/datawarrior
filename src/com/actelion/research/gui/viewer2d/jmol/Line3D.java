/* $RCSfile: Line3D.java,v $
 * $Author: nicove $
 * $Date: 2004/11/07 10:51:22 $
 * $Revision: 1.5 $
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
 * Implements 3D line drawing routines.
 *</p>
 *<p>
 * A number of line drawing routines, most of which are used to
 * implement higher-level shapes. Triangles and cylinders are drawn
 * as a series of lines
 *</p>
 */
final class Line3D {

  Graphics3D g3d;

  Line3D(Graphics3D g3d) {
    this.g3d = g3d;
  }

  void drawLine(int argbA, int argbB,
                int xA, int yA, int zA, int xB, int yB, int zB) {
    int dxBA = xB - xA, dyBA = yB - yA, dzBA = zB - zA;
    switch (visibilityCheck(xA, yA, zA, xB, yB, zB)) {
    case VISIBILITY_UNCLIPPED:
      plotLineDeltaUnclipped(argbA, argbB, xA, yA, zA, dxBA, dyBA, dzBA);
      break;
    case VISIBILITY_CLIPPED:
      plotLineDeltaClipped(argbA, argbB,
                           xA, yA, zA, dxBA, dyBA, dzBA);
    }
  }

  void drawDashedLine(int argbA, int argbB, int run, int rise,
                      int xA, int yA, int zA,
                      int xB, int yB, int zB) {
    int dxBA = xB - xA, dyBA = yB - yA, dzBA = zB - zA;
    switch (visibilityCheck(xA, yA, zA, xB, yB, zB)) {
    case VISIBILITY_UNCLIPPED:
      plotDashedLineDeltaUnclipped(argbA, argbB, run, rise,
                                   xA, yA, zA, dxBA, dyBA, dzBA);
      break;
    case VISIBILITY_CLIPPED:
      plotDashedLineDeltaClipped(argbA, argbB, run, rise,
                                 xA, yA, zA, dxBA, dyBA, dzBA);
    }
  }

  private final static int VISIBILITY_UNCLIPPED = 0;
  private final static int VISIBILITY_CLIPPED = 1;
  private final static int VISIBILITY_OFFSCREEN = 2;

  /**
   *<p>
   * Cohen-Sutherland line clipping used to check visibility.
   *</p>
   *<p>
   * Note that this routine is only used for visibility checking. To avoid
   * integer rounding errors which cause cracking to occur in 'solid'
   * surfaces, the lines are actually drawn from their original end-points.
   *</p>
   *
   * @param x1
   * @param y1
   * @param z1
   * @param x2
   * @param y2
   * @param z2
   * @return Visibility (see VISIBILITY_... constants);
   */
int visibilityCheck(int x1, int y1, int z1, int x2, int y2, int z2) {
    int cc1 = clipCode(x1, y1, z1);
    int cc2 = clipCode(x2, y2, z2);
    if ((cc1 | cc2) == 0)
      return VISIBILITY_UNCLIPPED;
    
    int xLast = g3d.xLast;
    int yLast = g3d.yLast;
    int slab = g3d.slab;
    int depth = g3d.depth;
    do {
      if ((cc1 & cc2) != 0)
        return VISIBILITY_OFFSCREEN;
      int dx = x2 - x1;
      int dy = y2 - y1;
      int dz = z2 - z1;

      if (cc1 != 0) { //cohen-sutherland line clipping
        if      ((cc1 & xLT) != 0)
          { y1 +=      (-x1 * dy)/dx; z1 +=      (-x1 * dz)/dx; x1 = 0; }
        else if ((cc1 & xGT) != 0)
          { y1 += ((xLast-x1)*dy)/dx; z1 += ((xLast-x1)*dz)/dx; x1 = xLast; }
        else if ((cc1 & yLT) != 0)
          { x1 +=      (-y1 * dx)/dy; z1 +=      (-y1 * dz)/dy; y1 = 0; }
        else if ((cc1 & yGT) != 0)
          { x1 += ((yLast-y1)*dx)/dy; z1 += ((yLast-y1)*dz)/dy; y1 = yLast; }
        else if ((cc1 & zLT) != 0)
          { x1 +=  ((slab-z1)*dx)/dz; y1 +=  ((slab-z1)*dy)/dz; z1 = slab; }
        else // must be zGT
          { x1 += ((depth-z1)*dx)/dz; y1 += ((depth-z1)*dy)/dz; z1 = depth; }

        cc1 = clipCode(x1, y1, z1);
      } else {
        if      ((cc2 & xLT) != 0)
          { y2 +=      (-x2 * dy)/dx; z2 +=      (-x2 * dz)/dx; x2 = 0; }
        else if ((cc2 & xGT) != 0)
          { y2 += ((xLast-x2)*dy)/dx; z2 += ((xLast-x2)*dz)/dx; x2 = xLast; }
        else if ((cc2 & yLT) != 0)
          { x2 +=      (-y2 * dx)/dy; z2 +=      (-y2 * dz)/dy; y2 = 0; }
        else if ((cc2 & yGT) != 0)
          { x2 += ((yLast-y2)*dx)/dy; z2 += ((yLast-y2)*dz)/dy; y2 = yLast; }
        else if ((cc2 & zLT) != 0)
          { x2 +=  ((slab-z2)*dx)/dz; y2 +=  ((slab-z2)*dy)/dz; z2 = slab; }
        else // must be zGT
          { x2 += ((depth-z2)*dx)/dz; y2 +=  ((depth-2)*dy)/dz; z2 = depth; }
        cc2 = clipCode(x2, y2, z2);
      }
    } while ((cc1 | cc2) != 0);
    return VISIBILITY_CLIPPED;
  }

  final static int zLT = 32;
  final static int zGT = 16;
  final static int xLT = 8;
  final static int xGT = 4;
  final static int yLT = 2;
  final static int yGT = 1;

  private final int clipCode(int x, int y, int z) {
    int code = 0;
    if (x < 0)
      code |= xLT;
    else if (x >= g3d.width)
      code |= xGT;

    if (y < 0)
      code |= yLT;
    else if (y >= g3d.height)
      code |= yGT;

    if (z < g3d.slab)
      code |= zLT;
    else if (z > g3d.depth) // note that this is .GT., not .GE.
      code |= zGT;

    return code;
  }

  void plotDashedLineDeltaUnclipped(int argb1, int argb2, int run, int rise, 
                                    int x, int y, int z,
                                    int dx, int dy, int dz) {
    if (rise >= run) {
      plotLineDeltaUnclipped(argb1, argb2, x, y, z, dx, dy, dz);
      return;
    }
    int runIndex = 0;
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x;
    // int yCurrent = y;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      for (int n = dx - 1, nMid = n / 2; --n >= 0; ) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (runIndex < rise  && zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
        ++runIndex;
        if (runIndex == run)
          runIndex = 0;
      }
    } else {
      int roundingFactor = dy - 1;
      if (dy < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      for (int n = dy - 1, nMid = n / 2; --n >= 0; ) {
        // yCurrent += yIncrement;
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          // xCurrent += xIncrement;
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (runIndex < rise && zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
        ++runIndex;
        if (runIndex == run)
          runIndex = 0;
      }
    }
  }

  void plotDashedLineDeltaClipped(int argb1, int argb2, int run, int rise,
                                  int x, int y, int z,
                                  int dx, int dy, int dz) {
    if (rise >= run) {
      plotLineDeltaClipped(argb1, argb2, x, y, z, dx, dy, dz);
      return;
    }
    int runIndex = 0;
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width, height = g3d.height;
    int slab = g3d.slab, depth = g3d.depth;
    int offset = y * width + x;
    if (x >= 0 && x < width &&
        y >= 0 && y < height &&
        z >= slab && z <= depth &&
        z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    int xCurrent = x;
    int yCurrent = y;
    int xIncrement = 1;
    int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      for (int n = dx - 1, nMid = n / 2; --n >= 0; ) {
        xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        if (runIndex < rise &&
            xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent >= slab &&
              zCurrent <= depth &&
              zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
        ++runIndex;
        if (runIndex == run)
          runIndex = 0;
      }
    } else {
      int roundingFactor = dy - 1;
      if (dy < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      for (int n = dy - 1, nMid = n / 2; --n > 0; ) {
        yCurrent += yIncrement;
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          xCurrent += xIncrement;
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        if (runIndex < rise &&
            xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent >= slab &&
              zCurrent <= depth &&
              zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
        ++runIndex;
        if (runIndex == run)
          runIndex = 0;
      }
    }
  }

  /*
  void plotLineDeltaUnclippedGradient(int argb1, int argb2,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;

    int r1 = (argb1 >> 16) & 0xFF;
    int g1 = (argb1 >> 8) & 0xFF;
    int b1 = argb1 & 0xFF;
    int r2 = (argb2 >> 16) & 0xFF;
    int g2 = (argb2 >> 8) & 0xFF;
    int b2 = argb2 & 0xFF;
    int dr = r2 - r1;
    int dg = g2 - g1;
    int db = b2 - b1;
    int rScaled = r1 << 10;
    int gScaled = g1 << 10;
    int bScaled = b1 << 10;
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      int n = dx;
      int nTransition = n >> 2;
      int nColor2 = (n - nTransition) / 2;
      int nColor1 = n - nColor2;
      if (nTransition <= 0)
        nTransition = 1;
      int drScaled = (dr << 10) / nTransition;
      int dgScaled = (dg << 10) / nTransition;
      int dbScaled = (db << 10) / nTransition;
      do {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] =
            (n > nColor1) ? argb1 :
            (n <= nColor2) ? argb2 :
            0xFF000000 | 
            ((rScaled << 6) & 0x00FF0000) |
            ((gScaled >> 2) & 0x0000FF00) |
            (bScaled >> 10);
        }
        if (n <= nColor1) {
          rScaled += drScaled;
          gScaled += dgScaled;
          bScaled += dbScaled;
        }
      } while (--n > 0);
      return;
    }
    int roundingFactor = dy - 1;
    if (dz < 0) roundingFactor = -roundingFactor;
    int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
    int twoDyAccumulatedXError = 0;
    int n = dy;
    int nTransition = n >> 2;
    int nColor2 = (n - nTransition) / 2;
    int nColor1 = n - nColor2;
    if (nTransition <= 0)
      nTransition = 1;
    int drScaled = (dr << 10) / nTransition;
    int dgScaled = (dg << 10) / nTransition;
    int dbScaled = (db << 10) / nTransition;
    do {
      // yCurrent += yIncrement;
      offset += yOffsetIncrement;
      zCurrentScaled += zIncrementScaled;
      twoDyAccumulatedXError += twoDx;
      if (twoDyAccumulatedXError > dy) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        twoDyAccumulatedXError -= twoDy;
      }
      int zCurrent = zCurrentScaled >> 10;
      if (zCurrent < zbuf[offset]) {
        zbuf[offset] = (short)zCurrent;
        pbuf[offset] =
          (n > nColor1) ? argb1 :
          (n <= nColor2) ? argb2 :
          0xFF000000 | 
          ((rScaled << 6) & 0x00FF0000) |
          ((gScaled >> 2) & 0x0000FF00) |
          (bScaled >> 10);
      }
      if (n <= nColor1) {
        rScaled += drScaled;
        gScaled += dgScaled;
        bScaled += dbScaled;
      }
    } while (--n > 0);
  }
  */

  void plotLineDeltaUnclipped(int[] shades1, int[] shades2, int fp8Intensity,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int offset = y1 * width + x1;
    int intensity = fp8Intensity >> 8;
    int intensityUp =
      (intensity < Shade3D.shadeLast ? intensity + 1 : intensity);
    int intensityDn =
      (intensity > 0 ? intensity - 1 : intensity);
    //int fp8Fraction = fp8Intensity & 0xFF;
    int argb1 = shades1[intensity];
    int argb1Up = shades1[intensityUp];
    int argb1Dn = shades1[intensityDn];
    int argb2 = shades2[intensity];
    int argb2Up = shades2[intensityUp];
    int argb2Dn = shades2[intensityDn];
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      for (int n = dx - 1, nMid = n / 2; --n >= 0; ) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          int rand8 = Shade3D.nextRandom8Bit();
          pbuf[offset] =
            (n > nMid
             ? (rand8 < 85 ? argb1Dn : (rand8 > 170 ? argb1Up : argb1))
             : (rand8 < 85 ? argb2Dn : (rand8 > 170 ? argb2Up : argb2)));
        }
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      for (int n = dy - 1, nMid = n / 2; --n >= 0; ) {
        // yCurrent += yIncrement;
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          // xCurrent += xIncrement;
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          int rand8 = Shade3D.nextRandom8Bit();
          pbuf[offset] =
            (n > nMid
             ? (rand8 < 85 ? argb1Dn : (rand8 > 170 ? argb1Up : argb1))
             : (rand8 < 85 ? argb2Dn : (rand8 > 170 ? argb2Up : argb2)));
        }
      }
    }
  }


  void plotLineDeltaUnclipped(int argb1, int argb2,
                              int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width;
    int offset = y1 * width + x1;
    if (z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    // int xCurrent = x1;
    // int yCurrent = y1;
    int xIncrement = 1;
    // int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      // yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      for (int n = dx - 1, nMid = n / 2; --n >= 0; ) {
        // xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          // yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      for (int n = dy - 1, nMid = n / 2; --n >= 0; ) {
        // yCurrent += yIncrement;
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          // xCurrent += xIncrement;
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        int zCurrent = zCurrentScaled >> 10;
        if (zCurrent < zbuf[offset]) {
          zbuf[offset] = (short)zCurrent;
          pbuf[offset] = n > nMid ? argb1 : argb2;
        }
      }
    }
  }

  void plotLineDeltaClipped(int argb1, int argb2, 
                            int x1, int y1, int z1, int dx, int dy, int dz) {
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width, height = g3d.height;
    int slab = g3d.slab, depth = g3d.depth;
    int offset = y1 * width + x1;
    if (x1 >= 0 && x1 < width &&
        y1 >= 0 && y1 < height &&
        z1 >= slab && z1 <= depth &&
        z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    int xCurrent = x1;
    int yCurrent = y1;
    int xIncrement = 1;
    int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      for (int n = dx - 1, nMid = n / 2; --n >= 0; ) {
        xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        if (xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent >= slab &&
              zCurrent <= depth &&
              zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      for (int n = dy - 1, nMid = n / 2; --n >= 0; ) {
        yCurrent += yIncrement;
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          xCurrent += xIncrement;
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        if (xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent >= slab &&
              zCurrent <= depth &&
              zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
      }
    }
  }

  void plotLineDeltaClipped(int[] shades1, int[] shades2, int fp8Intensity,
                            int x1, int y1, int z1, int dx, int dy, int dz) {
    int intensity = fp8Intensity >> 8;
    int argb1 = shades1[intensity];
    int argb2 = shades2[intensity];
    int[] pbuf = g3d.pbuf;
    short[] zbuf = g3d.zbuf;
    int width = g3d.width, height = g3d.height;
    int slab = g3d.slab, depth = g3d.depth;
    int offset = y1 * width + x1;
    if (x1 >= 0 && x1 < width &&
        y1 >= 0 && y1 < height &&
        z1 >= slab && z1 <= depth &&
        z1 < zbuf[offset]) {
      zbuf[offset] = (short)z1;
      pbuf[offset] = argb1;
    }
    if (dx == 0 && dy == 0)
      return;

    int xCurrent = x1;
    int yCurrent = y1;
    int xIncrement = 1;
    int yIncrement = 1;
    int yOffsetIncrement = width;

    if (dx < 0) {
      dx = -dx;
      xIncrement = -1;
    }
    if (dy < 0) {
      dy = -dy;
      yIncrement = -1;
      yOffsetIncrement = -width;
    }
    int twoDx = dx + dx, twoDy = dy + dy;

    // the z dimension and the z increment are stored with a fractional
    // component in the bottom 10 bits.
    int zCurrentScaled = z1 << 10;
    if (dy <= dx) {
      int roundingFactor = dx - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dx;
      int twoDxAccumulatedYError = 0;
      for (int n = dx - 1, nMid = n / 2; --n >= 0; ) {
        xCurrent += xIncrement;
        offset += xIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDxAccumulatedYError += twoDy;
        if (twoDxAccumulatedYError > dx) {
          yCurrent += yIncrement;
          offset += yOffsetIncrement;
          twoDxAccumulatedYError -= twoDx;
        }
        if (xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent >= slab &&
              zCurrent <= depth &&
              zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
      }
    } else {
      int roundingFactor = dy - 1;
      if (dz < 0) roundingFactor = -roundingFactor;
      int zIncrementScaled = ((dz << 10) + roundingFactor) / dy;
      int twoDyAccumulatedXError = 0;
      for (int n = dy - 1, nMid = n / 2; --n >= 0; ) {
        yCurrent += yIncrement;
        offset += yOffsetIncrement;
        zCurrentScaled += zIncrementScaled;
        twoDyAccumulatedXError += twoDx;
        if (twoDyAccumulatedXError > dy) {
          xCurrent += xIncrement;
          offset += xIncrement;
          twoDyAccumulatedXError -= twoDy;
        }
        if (xCurrent >= 0 && xCurrent < width &&
            yCurrent >= 0 && yCurrent < height) {
          int zCurrent = zCurrentScaled >> 10;
          if (zCurrent >= slab &&
              zCurrent <= depth &&
              zCurrent < zbuf[offset]) {
            zbuf[offset] = (short)zCurrent;
            pbuf[offset] = n > nMid ? argb1 : argb2;
          }
        }
      }
    }
  }
}
