/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-08-17 01:22:42 +0200 (dim., 17 août 2008) $
 * $Revision: 9744 $
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

package org.jmol.shape;

import org.jmol.g3d.*;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementPending;
import org.jmol.util.Point3fi;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

public class MeasuresRenderer extends FontLineShapeRenderer {

  private short measurementMad;
  private Font3D font3d;
  private Measurement measurement;
  private boolean doJustify;
  protected void render() {
    if (!g3d.checkTranslucent(false))
      return;
    imageFontScaling = viewer.getImageFontScaling();
    Measures measures = (Measures) shape;
    doJustify = viewer.getJustifyMeasurements();
    measurementMad = measures.mad;
    font3d = g3d.getFont3DScaled(measures.font3d, imageFontScaling);
    renderPendingMeasurement(measures.measurementPending);
    if (!viewer.getShowMeasurements())
      return;
    boolean showMeasurementLabels = viewer.getShowMeasurementLabels();
    boolean dynamicMeasurements = viewer.getDynamicMeasurements();
    measures.setVisibilityInfo();
    for (int i = measures.measurementCount; --i >= 0;) {
      Measurement m = measures.measurements[i];
      if (dynamicMeasurements || m.isDynamic())
        m.refresh();
      if (!m.isVisible())
        continue;
      colix = m.getColix();
      if (colix == 0)
        colix = measures.colix;
      if (colix == 0)
        colix = viewer.getColixBackgroundContrast();
      g3d.setColix(colix);
      renderMeasurement(m.getCount(), m, showMeasurementLabels);
    }
  }

  Point3fi atomA, atomB, atomC, atomD;
  
  private Point3fi getAtom(int i) {
    Point3fi a = measurement.getAtom(i);
    if (a.screenDiameter < 0) {
      viewer.transformPoint(a, ptA);
      a.screenX = ptA.x;
      a.screenY = ptA.y;
      a.screenZ = ptA.z;
    }
    return a;
  }
  
  private void renderMeasurement(int count, Measurement measurement, boolean renderLabel) {
    this.measurement = measurement;
    switch(count) {
    case 2:
      atomA = getAtom(1);
      atomB = getAtom(2);
      renderDistance(renderLabel);
      break;
    case 3:
      atomA = getAtom(1);
      atomB = getAtom(2);
      atomC = getAtom(3);
      renderAngle(renderLabel);
      break;
    case 4:
      atomA = getAtom(1);
      atomB = getAtom(2);
      atomC = getAtom(3);
      atomD = getAtom(4);
      renderTorsion(renderLabel);
      break;
    }
    atomA = atomB = atomC = atomD = null;
  }

  private Point3i ptA = new Point3i();
  private Point3i ptB = new Point3i();

  private int drawSegment(int x1, int y1, int z1, int x2, int y2, int z2) {
    ptA.set(x1, y1, z1);
    ptB.set(x2, y2, z2);
    if (measurementMad < 0) {
      g3d.drawDashedLine(4, 2, ptA, ptB);
      return 1;
    }
    int widthPixels = measurementMad;
    if (measurementMad >= 20)
      widthPixels = viewer.scaleToScreen((z1 + z2) / 2, measurementMad);
    g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, widthPixels, ptA, ptB);

    return (widthPixels + 1) / 2;
  }

  void renderDistance(boolean renderLabel) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA, atomB
        .screenX, atomB.screenY, zB);
    if (!renderLabel)
      return;
    int z = (zA + zB) / 2;
    if (z < 1)
      z = 1;
    int x = (atomA.screenX + atomB.screenX) / 2;
    int y = (atomA.screenY + atomB.screenY) / 2;
    paintMeasurementString(x, y, z, radius, (x - atomA.screenX)*(y-atomA.screenY) > 0, 0);
  }
                           

  private AxisAngle4f aaT = new AxisAngle4f();
  private Matrix3f matrixT = new Matrix3f();
  private Point3f pointT = new Point3f();

  private void renderAngle(boolean renderLabel) {
    int zOffset = atomB.screenDiameter + 10;
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - zOffset;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA,
                             atomB.screenX, atomB.screenY, zB);
    radius += drawSegment(atomB.screenX, atomB.screenY, zB,
                          atomC.screenX, atomC.screenY, zC);    
    if (!renderLabel)
      return;
    radius = (radius + 1) / 2;

    AxisAngle4f aa = measurement.getAxisAngle();
    if (aa == null) { // 180 degrees
      int offset = (int) (5 * imageFontScaling);
      paintMeasurementString(atomB.screenX + offset, atomB.screenY - offset,
                             zB, radius, false, 0);
      return;
    }
    int dotCount = (int)((aa.angle / (2 * Math.PI)) * 64);
    float stepAngle = aa.angle / dotCount;
    aaT.set(aa);
    int iMid = dotCount / 2;
    Point3f ptArc = measurement.getPointArc();
    for (int i = dotCount; --i >= 0; ) {
      aaT.angle = i * stepAngle;
      matrixT.set(aaT);
      pointT.set(ptArc);
      matrixT.transform(pointT);
      pointT.add(atomB);
      // NOTE! Point3i screen is just a pointer 
      //  to viewer.transformManager.point3iScreenTemp
      Point3i point3iScreenTemp = viewer.transformPoint(pointT);
      int zArc = point3iScreenTemp.z - zOffset;
      if (zArc < 0) zArc = 0;
      g3d.drawPixel(point3iScreenTemp.x, point3iScreenTemp.y, zArc);
      if (i == iMid) {
        pointT.set(ptArc);
        pointT.scale(1.1f);
        // next line modifies Point3i point3iScreenTemp
        matrixT.transform(pointT);
        pointT.add(atomB);
        viewer.transformPoint(pointT);
        int zLabel = point3iScreenTemp.z - zOffset;
        paintMeasurementString(point3iScreenTemp.x, 
            point3iScreenTemp.y, zLabel, radius, 
            point3iScreenTemp.x < atomB.screenX, atomB.screenY);
      }
    }
  }

  private void renderTorsion(boolean renderLabel) {
    int zA = atomA.screenZ - atomA.screenDiameter - 10;
    int zB = atomB.screenZ - atomB.screenDiameter - 10;
    int zC = atomC.screenZ - atomC.screenDiameter - 10;
    int zD = atomD.screenZ - atomD.screenDiameter - 10;
    int radius = drawSegment(atomA.screenX, atomA.screenY, zA, atomB.screenX, atomB.screenY, zB);
    radius += drawSegment(atomB.screenX, atomB.screenY, zB, atomC.screenX, atomC.screenY, zC);
    radius += drawSegment(atomC.screenX, atomC.screenY, zC, atomD.screenX, atomD.screenY, zD);
    if (!renderLabel)
      return;
    radius /= 3;
    paintMeasurementString((atomA.screenX + atomB.screenX + atomC.screenX + atomD.screenX) / 4,
                           (atomA.screenY + atomB.screenY + atomC.screenY + atomD.screenY) / 4,
                           (zA + zB + zC + zD) / 4, radius, false, 0);
  }

  private void paintMeasurementString(int x, int y, int z, int radius,
                              boolean rightJustify, int yRef) {
    if (!doJustify) {
      rightJustify = false;
      yRef = y;
    }
    String strMeasurement = measurement.getString();
    if (strMeasurement == null)
      return;
    int width = font3d.fontMetrics.stringWidth(strMeasurement);
    int height = font3d.fontMetrics.getAscent();
    int xT = x;
    if (rightJustify)
      xT -= radius / 2 + 2 + width;
    else
      xT += radius / 2 + 2;
    int yT = y + (yRef == 0 || yRef < y ? height : -radius / 2);
    int zT = z - radius - 2;
    if (zT < 1)
      zT = 1;
    g3d.drawString(strMeasurement, font3d, xT, yT, zT, zT);
  }

  private void renderPendingMeasurement(MeasurementPending measurementPending) {
    if (isGenerator || measurementPending == null)
      return;
    int count = measurementPending.getCount();
    if (count == 0)
      return;
    g3d.setColix(viewer.getColixRubberband());
    measurementPending.refresh();
    if (measurementPending.haveTarget())
      renderMeasurement(count, measurementPending, true);
    else
      renderPendingWithCursor(count, measurementPending);
  }
  
  private void renderPendingWithCursor(int count, MeasurementPending measurementPending) {
    if (count > 1)
      renderMeasurement(count, measurementPending, false);
    measurement = measurementPending;
    Point3fi atomLast = getAtom(count);
    int lastZ = atomLast.screenZ - atomLast.screenDiameter - 10;
    int x = viewer.getCursorX();
    int y = viewer.getCursorY();
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    drawSegment(atomLast.screenX, atomLast.screenY, lastZ, x, y, 0);
  }  
}
