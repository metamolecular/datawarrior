/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-27 01:06:21 +0200 (lun., 27 juil. 2009) $
 * $Revision: 11287 $
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
package org.jmol.viewer;

import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.StateManager.Orientation;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import org.jmol.g3d.Graphics3D;

class TransformManager11 extends TransformManager {

  private float navigationSlabOffset;
  private float zoomFactor = Float.MAX_VALUE;

  TransformManager11() {
    super();
    setNavFps(10);
  }
  
  TransformManager11(Viewer viewer) {
    super(viewer);
    setNavFps(10);
  }

  protected void setNavFps(int navFps) {
    this.navFps = navFps;
  }

  TransformManager11(Viewer viewer, int width, int height) {
    super(viewer, width, height);
    setNavFps(10);
  }

  protected void calcCameraFactors() {
    //(m) model coordinates
    //(s) screen coordinates = (m) * screenPixelsPerAngstrom
    //(p) plane coordinates = (s) / screenPixelCount

    if (Float.isNaN(cameraDepth)) {
      cameraDepth = cameraDepthSetting;
      zoomFactor = Float.MAX_VALUE;
    }

    // reference point where p=0 
    cameraDistance = cameraDepth * screenPixelCount; //(s)

    // distance from camera to midPlane of model (p=0.5)
    // the factor to apply based on screen Z
    referencePlaneOffset = cameraDistance + screenPixelCount / 2f; //(s)

    // conversion factor Angstroms --> pixels
    // so that "full window" is visualRange
    scalePixelsPerAngstrom = (scale3D && !perspectiveDepth && !isNavigationMode ? 
        72 / scale3DAngstromsPerInch : screenPixelCount / visualRange);  //(s/m)

    // model radius in pixels
    modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; //(s)

    // model center offset for zoom 100
    float offset100 = (2 * modelRadius) / visualRange * referencePlaneOffset; //(s)

    //System.out.println("sppA " + scalePixelsPerAngstrom + " pD " + perspectiveDepth 
      //  + " spC " + screenPixelCount + " vR " + visualRange 
        //+ " sDPPA " + scaleDefaultPixelsPerAngstrom);

    if (!isNavigationMode) {
      // nonNavigation mode -- to match Jmol 10.2 at midplane (caffeine.xyz)
      //flag that we have left navigation mode
      zoomFactor = Float.MAX_VALUE;
      // we place the model at the referencePlaneOffset offset and then change the scale
      modelCenterOffset = referencePlaneOffset;
      //now factor the scale by distance from camera and zoom
      if (!scale3D || perspectiveDepth)
        scalePixelsPerAngstrom *= (modelCenterOffset / offset100) * zoomPercent / 100; //(s/m)

      //System.out.println("sppA revised:" + scalePixelsPerAngstrom);
      // so that's sppa = (spc / vR) * rPO * (vR / 2)  / mR * rPO = spc/2/mR
      
      modelRadiusPixels = modelRadius * scalePixelsPerAngstrom; //(s)
      //System.out.println("transformman scalppa modelrad " + scalePixelsPerAngstrom + " " + modelRadiusPixels + " " + visualRange);
      return;
    }
    
    if (zoomFactor == Float.MAX_VALUE) {
      //entry point
      if (zoomPercent > MAXIMUM_ZOOM_PERSPECTIVE_DEPTH)
        zoomPercent = MAXIMUM_ZOOM_PERSPECTIVE_DEPTH;
      // screen offset to fixed rotation center
      modelCenterOffset = offset100 * 100 / zoomPercent;
    } else if (prevZoomSetting != zoomPercentSetting) {
      if (zoomRatio == 0) //scripted change zoom xxx
        modelCenterOffset = offset100 * 100 / zoomPercentSetting;
      else
        // fractional change by script or mouse
        modelCenterOffset += (1 - zoomRatio) * referencePlaneOffset;
      navMode = NAV_MODE_ZOOMED;
    }
    prevZoomSetting = zoomPercentSetting;
    zoomFactor = modelCenterOffset / referencePlaneOffset;
    // infinite or negative value means there is no corresponding non-navigating zoom setting
    zoomPercent = (zoomFactor == 0 ? MAXIMUM_ZOOM_PERSPECTIVE_DEPTH
        : offset100 / modelCenterOffset * 100);
  }

  protected float getPerspectiveFactor(float z) {
    //System.out.println (z + " getPerspectiveFactor " + referencePlaneOffset + " " + (z <= 0 ? referencePlaneOffset : referencePlaneOffset / z));
    return (z <= 0 ? referencePlaneOffset : referencePlaneOffset / z);
  }

  protected void adjustTemporaryScreenPoint() {

    //fixedRotation point is at the origin initially

    float z = point3fScreenTemp.z;

    //this could easily go negative -- behind the screen --
    //but we don't care. In fact, that just makes it easier, 
    //because it means we won't render it.
    //we should probably assign z = 0 as "unrenderable"

    if (Float.isNaN(z)) {
      if (!haveNotifiedNaN)
        Logger.debug("NaN seen in TransformPoint");
      haveNotifiedNaN = true;
      z = 1;
    } else if (z <= 0) {
      //just don't let z go past 1  BH 11/15/06
      z = 1;
    }
    point3fScreenTemp.z = z;

    // x and y are moved inward (generally) relative to 0, which
    // is either the fixed rotation center or the navigation center

    // at this point coordinates are centered on rotation center

    if (perspectiveDepth) {
      if (isNavigationMode) {
        // move nav center to 0; refOffset = Nav - Rot
        point3fScreenTemp.x -= navigationShiftXY.x;
        point3fScreenTemp.y -= navigationShiftXY.y;
      }
      // apply perspective factor
      float factor = getPerspectiveFactor(z);
      point3fScreenTemp.x *= factor;
      point3fScreenTemp.y *= factor;
    }

    //now move the center point to where it needs to be
    if (isNavigationMode) {
      point3fScreenTemp.x += navigationOffset.x;
      point3fScreenTemp.y += navigationOffset.y;
    } else {
      point3fScreenTemp.x += fixedRotationOffset.x;
      point3fScreenTemp.y += fixedRotationOffset.y;
    }

    if (Float.isNaN(point3fScreenTemp.x) && !haveNotifiedNaN) {
      Logger.debug("NaN found in transformPoint ");
      haveNotifiedNaN = true;
    }

    point3iScreenTemp.set((int) point3fScreenTemp.x, (int) point3fScreenTemp.y,
        (int) point3fScreenTemp.z);
  }

  /* ***************************************************************
   * Navigation support
   ****************************************************************/

  final private static int NAV_MODE_IGNORE = -2;
  final private static int NAV_MODE_ZOOMED = -1;
  final private static int NAV_MODE_NONE = 0;
  final private static int NAV_MODE_RESET = 1;
  final private static int NAV_MODE_NEWXY = 2;
  final private static int NAV_MODE_NEWXYZ = 3;
  final private static int NAV_MODE_NEWZ = 4;

  private int navMode = NAV_MODE_RESET;


  void setScreenParameters(int screenWidth, int screenHeight,
                           boolean useZoomLarge, boolean antialias,
                           boolean resetSlab, boolean resetZoom) {
    Point3f pt = (isNavigationMode ? new Point3f(navigationCenter) : null);
    Point3f ptoff = new Point3f(navigationOffset);
    ptoff.x = ptoff.x / width;
    ptoff.y = ptoff.y / height;
    super.setScreenParameters(screenWidth, screenHeight, useZoomLarge, 
        antialias, resetSlab, resetZoom);
    if (pt != null) {
      navigationCenter.set(pt);
      navTranslatePercent(-1, ptoff.x * width, ptoff.y * height);
      navigate(0, pt);
    }
  }

  float navigationDepth;

  /**
   * All the magic happens here.
   *
   */
  protected void calcNavigationPoint() {
    // called by finalize
    calcNavigationDepthPercent();
    if (!navigating && navMode != NAV_MODE_RESET) {
      // rotations are different from zoom changes
      if (navigationDepth < 100 && navigationDepth > 0
          && !Float.isNaN(previousX)
          && previousX == fixedTranslation.x 
             && previousY == fixedTranslation.y 
             && navMode != NAV_MODE_ZOOMED)
        navMode = NAV_MODE_NEWXYZ;
      else
        navMode = NAV_MODE_NONE;
    }
    switch (navMode) {
    case NAV_MODE_RESET:
      //simply place the navigation center front and center and recalculate modelCenterOffset
      navigationOffset.set(width / 2f, getNavPtHeight(), referencePlaneOffset);
      zoomFactor = Float.MAX_VALUE;
      calcCameraFactors();
      calcTransformMatrix();
      newNavigationCenter();
      break;
    case NAV_MODE_NONE:
    case NAV_MODE_ZOOMED:
      //update fixed rotation offset and find the new 3D navigation center
      fixedRotationOffset.set(fixedTranslation);
      newNavigationCenter();
      break;
    case NAV_MODE_NEWXY:
      // redefine the navigation center based on its old screen position
      newNavigationCenter();
      break;
    case NAV_MODE_IGNORE:
    case NAV_MODE_NEWXYZ:
      // must just be (not so!) simple navigation
      // navigation center will initially move
      // but we center it by moving the rotation center instead
      matrixTransform.transform(navigationCenter, pointT);
      float z = pointT.z;
      matrixTransform.transform(fixedRotationCenter, pointT);
      modelCenterOffset = referencePlaneOffset + (pointT.z - z);
      calcCameraFactors();
      calcTransformMatrix();
      break;
    case NAV_MODE_NEWZ:
      //just untransform the offset to get the new 3D navigation center
      navigationOffset.z = referencePlaneOffset;
      //System.out.println("nav_mode_newz " + navigationOffset);
      unTransformPoint(navigationOffset, navigationCenter);
      break;
    }
    matrixTransform.transform(navigationCenter, navigationShiftXY);
    if (viewer.getNavigationPeriodic()) {
      //TODO
      // but if periodic, then the navigationCenter may have to be moved back a notch
      viewer.toUnitCell(navigationCenter, null);
      if (pointT.distance(navigationCenter) > 0.01) {
        matrixTransform.transform(navigationCenter, pointT);
        float dz = navigationShiftXY.z - pointT.z;
        //the new navigation center determines the navigationZOffset
        modelCenterOffset += dz;
        calcCameraFactors();
        calcTransformMatrix();
        matrixTransform.transform(navigationCenter, navigationShiftXY);
      }
    }
    transformPoint(fixedRotationCenter, fixedTranslation);
    fixedRotationOffset.set(fixedTranslation);
    previousX = fixedTranslation.x;
    previousY = fixedTranslation.y;
    transformPoint(navigationCenter, navigationOffset);
    navigationOffset.z = referencePlaneOffset;
    navMode = NAV_MODE_NONE;
    calcNavigationSlabAndDepth();
  }

  private float getNavPtHeight() {
    boolean navigateSurface = viewer.getNavigateSurface();
    return height / (navigateSurface ? 1f : 2f);
  }

  protected void calcNavigationSlabAndDepth() {
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
    if (!slabEnabled)
      return;
    slabValue = (isNavigationMode ? 10 : (int) (referencePlaneOffset - navigationSlabOffset));
    depthValue = zValueFromPercent(depthPercentSetting);

    viewer.getGlobalSettings().setParameterValue("navigationDepth",getNavigationDepthPercent());
    viewer.getGlobalSettings().setParameterValue("navigationSlab",getNavigationSlabOffsetPercent());

    if (Logger.debugging)
      Logger.debug("\n" + "\nperspectiveScale: " + referencePlaneOffset
          + " screenPixelCount: " + screenPixelCount 
          + "\nmodelTrailingEdge: "
          + (modelCenterOffset + modelRadiusPixels) + " depthValue: "
          + depthValue + "\nmodelCenterOffset: " + modelCenterOffset
          + " modelRadiusPixels: " + modelRadiusPixels + "\nmodelLeadingEdge: "
          + (modelCenterOffset - modelRadiusPixels) + " slabValue: "
          + slabValue + "\nzoom: " + zoomPercent + " navDepth: "
          + ((int) (100 * getNavigationDepthPercent()) / 100f) + " visualRange: " + visualRange
          + "\nnavX/Y/Z/modelCenterOffset: " + navigationOffset.x + "/"
          + navigationOffset.y + "/" + navigationOffset.z + "/"
          + modelCenterOffset + " navCenter:" + navigationCenter);
  }

  /**
   * We do not want the fixed navigation offset to change,
   * but we need a new model-based equivalent position.
   * The fixed rotation center is at a fixed offset as well.
   * This means that the navigationCenter must be recalculated
   * based on its former offset in the new context. We have two points, 
   * N(navigation) and R(rotation). We know where they ARE: 
   * fixedNavigationOffset and fixedRotationOffset.
   * From these we must derive navigationCenter.

   */
  private void newNavigationCenter() {
    
    //Point3f fixedRotationCenter, Point3f navigationOffset,
 
      //                      Point3f navigationCenter) {
    
    //fixedRotationCenter, navigationOffset, navigationCenter
    isNavigationMode = false;
    //get the rotation center's Z offset and move X and Y to 0,0
    transformPoint(fixedRotationCenter, pointT);
    pointT.x -= navigationOffset.x;
    pointT.y -= navigationOffset.y;
    //unapply the perspective as if IT were the navigation center
    float f = -getPerspectiveFactor(pointT.z);
    pointT.x /= f;
    pointT.y /= f;
    pointT.z = referencePlaneOffset;
    //now untransform that point to give the center that would
    //deliver this fixedModel position
    matrixUnTransform(pointT, navigationCenter);
    isNavigationMode = true;
  }

  boolean canNavigate() {
    return true;
  }

  private int nHits;
  private int multiplier = 1;

  
  protected void resetNavigationPoint(boolean doResetSlab) {

    //no release from navigation mode if too far zoomed in!

    if (zoomPercent < 5 && !isNavigationMode) {
      isNavigationMode = perspectiveDepth = true;
      return;
    }
    if (isNavigationMode) {
      navMode = NAV_MODE_RESET;
      slabPercentSetting = 0;
      perspectiveDepth = true;
    } else if (doResetSlab) {
      slabPercentSetting = 100;
    }
    if (doResetSlab)
      slabEnabled = isNavigationMode;
    
    zoomFactor = Float.MAX_VALUE;
    zoomPercentSetting = zoomPercent;
  }

  protected void setNavigationOffsetRelative(boolean navigatingSurface) {
    if (navigatingSurface) {
      navigateSurface(Integer.MAX_VALUE);
      return;
    }
    if (navigationDepth < 0 && navZ > 0 
        || navigationDepth > 100 && navZ < 0) {
      navZ = 0;
    }
    rotateXRadians(radiansPerDegree * -.02f * navY, null);
    rotateYRadians(radiansPerDegree * .02f * navX, null);    
    Point3f pt = getNavigationCenter();
    Point3f pts = new Point3f();
    transformPoint(pt, pts);
    pts.z += navZ;
    unTransformPoint(pts, pt);
    navigate(0, pt);
   }

  synchronized void navigate(int keyCode, int modifiers) {
    if (!isNavigationMode)
      return;
    if (keyCode == 0) {
      nHits = 0;
      multiplier = 1;
      if (!navigating)
        return;
      navigating = false;
      return;
    }
    nHits++;
    if (nHits % 10 == 0)
      multiplier *= (multiplier == 4 ? 1 : 2);
    boolean navigateSurface = viewer.getNavigateSurface();
    boolean isShiftKey = ((modifiers & InputEvent.SHIFT_MASK) > 0);
    boolean isAltKey = ((modifiers & InputEvent.ALT_MASK) > 0);
    boolean isCtrlKey = ((modifiers & InputEvent.CTRL_MASK) > 0);
    float speed = viewer.getNavigationSpeed() * (isCtrlKey ? 10 : 1);
    switch (keyCode) {
    case KeyEvent.VK_PERIOD:
      navX = navY = navZ = 0;
      setNavOn(false);
      homePosition();
      return;
    case KeyEvent.VK_SPACE:
      if (!navOn)
        return;
      navX = navY = navZ = 0;
      return;
    case KeyEvent.VK_UP:
      if (navOn) {
        if (isAltKey) {
          navY += multiplier;
        } else {
          navZ += multiplier;
        }
        break;
      }
      if (navigateSurface) {
        navigateSurface(Integer.MAX_VALUE);
        break;
      }
      if (isShiftKey) {
        navigationOffset.y -= 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        rotateXRadians(radiansPerDegree * -.2f * multiplier, null);
        navMode = NAV_MODE_NEWXYZ;
        break;
      }
      modelCenterOffset -= speed * (viewer.getNavigationPeriodic() ? 1 : multiplier);
      navMode = NAV_MODE_NEWZ;
      break;
    case KeyEvent.VK_DOWN:
      if (navOn) {
        if (isAltKey) {
          navY -= multiplier;
        } else {
          navZ -= multiplier;
        }
        break;
      }
      if (navigateSurface) {
        navigateSurface(-2 * multiplier);
        break;
      }
      if (isShiftKey) {
        navigationOffset.y += 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      if (isAltKey) {
        rotateXRadians(radiansPerDegree * .2f * multiplier, null);
        navMode = NAV_MODE_NEWXYZ;
        break;
      }
      modelCenterOffset += speed * (viewer.getNavigationPeriodic() ? 1 : multiplier);
      navMode = NAV_MODE_NEWZ;
      break;
    case KeyEvent.VK_LEFT:
      if (navOn) {
        navX -= multiplier;
        break;
      }
      if (navigateSurface) {
        break;
      }
      if (isShiftKey) {
        navigationOffset.x -= 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      rotateYRadians(radiansPerDegree * 3 * -.2f * multiplier, null);
      navMode = NAV_MODE_NEWXYZ;
      break;
    case KeyEvent.VK_RIGHT:
      if (navOn) {
        navX += multiplier;
        break;
      }
      if (navigateSurface) {
        break;
      }
      if (isShiftKey) {
        navigationOffset.x += 2 * multiplier;
        navMode = NAV_MODE_NEWXY;
        break;
      }
      rotateYRadians(radiansPerDegree * 3 * .2f * multiplier, null);
      navMode = NAV_MODE_NEWXYZ;
      break;
    default:
      navigating = false;
      navMode = NAV_MODE_NONE;
      return;
    }
    navigating = true;
    finalizeTransformParameters();
  }

  private void navigateSurface(int dz) {
    if (viewer.isRepaintPending())
      return;
    viewer.setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "navigate", new Integer(dz == Integer.MAX_VALUE ? 2 * multiplier : dz));
    viewer.requestRepaintAndWait();
  }

  void navigate(float seconds, Point3f pt) {
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, pt, Float.NaN, Float.NaN, Float.NaN);
      return;
    }
    navigationCenter.set(pt);
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  void navigate(float seconds, Vector3f rotAxis, float degrees) {
    if (degrees == 0)
      return;
    if (seconds > 0) {
      navigateTo(seconds, rotAxis, degrees, null, Float.NaN, Float.NaN,
          Float.NaN);
      return;
    }
    rotateAxisAngle(rotAxis, degrees / degreesPerRadian);
    navMode = NAV_MODE_NEWXYZ;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  void setNavigationDepthPercent(float timeSec, float percent) {
    if (timeSec > 0) {
      navigateTo(timeSec, null, Float.NaN, null, percent, Float.NaN, Float.NaN);
      return;
    }    
    setNavigationDepthPercent(percent);
  }

  void navTranslate(float seconds, Point3f pt) {
    transformPoint(pt, pointT);
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, null, Float.NaN, pointT.x, pointT.y);
      return;
    }
    navTranslatePercent(-1, pointT.x, pointT.y);
  }

  void navTranslatePercent(float seconds, float x, float y) {
    // if either is Float.NaN, then the other is RELATIVE to current
    transformPoint(navigationCenter, navigationOffset);
    if (seconds >= 0) {
      if (!Float.isNaN(x))
        x = width * x / 100f
            + (Float.isNaN(y) ? navigationOffset.x : (width / 2f));
      if (!Float.isNaN(y))
        y = height * y / 100f
            + (Float.isNaN(x) ? navigationOffset.y : getNavPtHeight());
    }
    if (seconds > 0) {
      navigateTo(seconds, null, Float.NaN, null, Float.NaN, x, y);
      return;
    }
    if (!Float.isNaN(x))
      navigationOffset.x = x;
    if (!Float.isNaN(y))
      navigationOffset.y = y;
    navMode = NAV_MODE_NEWXY;
    navigating = true;
    finalizeTransformParameters();
    navigating = false;
  }

  private void navigateTo(float floatSecondsTotal, Vector3f axis,
                          float degrees, Point3f center, float depthPercent,
                          float xTrans, float yTrans) {

    Orientation o = viewer.getOrientation();
    if (!Float.isNaN(degrees) && degrees != 0)
      navigate(0, axis, degrees);
    if (center != null) {
      navigate(0, center);
    }
    if (!Float.isNaN(xTrans) || !Float.isNaN(yTrans))
      navTranslatePercent(-1, xTrans, yTrans);
    if (!Float.isNaN(depthPercent))
      setNavigationDepthPercent(depthPercent);
    Orientation o1 = viewer.getOrientation();
    o.restore(0, true);
    o1.restore(floatSecondsTotal, true);
  }

  void navigate(float seconds, Point3f[][] pathGuide) {
    navigate(seconds, pathGuide, null, null, 0, Integer.MAX_VALUE);
  }

  void navigate(float seconds, Point3f[] path, float[] theta, int indexStart,
                int indexEnd) {
    navigate(seconds, null, path, theta, indexStart, indexEnd);
  }

  private void navigate(float seconds, Point3f[][] pathGuide, Point3f[] path,
                float[] theta, int indexStart, int indexEnd) {
    if (seconds <= 0) // PER station
      seconds = 2;
    boolean isPathGuide = (pathGuide != null);
    int nSegments = Math.min(
        (isPathGuide ? pathGuide.length : path.length) - 1, indexEnd);
    if (!isPathGuide)
      while (nSegments > 0 && path[nSegments] == null)
        nSegments--;
    nSegments -= indexStart;
    if (nSegments < 1)
      return;
    int nPer = (int) (10 * seconds); //?
    int nSteps = nSegments * nPer + 1;
    Point3f[] points = new Point3f[nSteps + 2];
    Point3f[] pointGuides = new Point3f[isPathGuide ? nSteps + 2 : 0];
    int iPrev, iNext, iNext2, iNext3, pt;
    for (int i = 0; i < nSegments; i++) {
      iPrev = Math.max(i - 1, 0) + indexStart;
      pt = i + indexStart;
      iNext = Math.min(i + 1, nSegments) + indexStart;
      iNext2 = Math.min(i + 2, nSegments) + indexStart;
      iNext3 = Math.min(i + 3, nSegments) + indexStart;
      if (isPathGuide) {
        Graphics3D.getHermiteList(7, pathGuide[iPrev][0], pathGuide[pt][0],
            pathGuide[iNext][0], pathGuide[iNext2][0], pathGuide[iNext3][0],
            points, i * nPer, nPer + 1);
        Graphics3D.getHermiteList(7, pathGuide[iPrev][1], pathGuide[pt][1],
            pathGuide[iNext][1], pathGuide[iNext2][1], pathGuide[iNext3][1],
            pointGuides, i * nPer, nPer + 1);
      } else {
        Graphics3D.getHermiteList(7, path[iPrev], path[pt], path[iNext],
            path[iNext2], path[iNext3], points, i * nPer, nPer + 1);
      }
    }
    int totalSteps = nSteps;
    viewer.setInMotion(true);
    int frameTimeMillis = (int) (1000 / navFps);
    long targetTime = System.currentTimeMillis();
    for (int iStep = 0; iStep < totalSteps; ++iStep) {
      navigate(0, points[iStep]);
      if (isPathGuide) {
        alignZX(points[iStep], points[iStep + 1], pointGuides[iStep]);
      }
      targetTime += frameTimeMillis;
      if (System.currentTimeMillis() < targetTime) {
        viewer.requestRepaintAndWait();
        if (!viewer.isScriptExecuting())
          break;
        int sleepTime = (int) (targetTime - System.currentTimeMillis());
        if (sleepTime > 0) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
          }
        }
      }
    }
  }
  
  void navigateSurface(float timeSeconds, String name) {
  }


  /**
   * brings pt0-pt1 vector to [0 0 -1], then rotates
   * about [0 0 1] until ptVectorWing is in xz plane
   * @param pt0
   * @param pt1
   * @param ptVectorWing
   */
  void alignZX(Point3f pt0, Point3f pt1, Point3f ptVectorWing) {
    Point3f pt0s = new Point3f();
    Point3f pt1s = new Point3f();
    matrixRotate.transform(pt0, pt0s);
    matrixRotate.transform(pt1, pt1s);
    Vector3f vPath = new Vector3f(pt0s);
    vPath.sub(pt1s);
    Vector3f v = new Vector3f(0, 0, 1);
    float angle = vPath.angle(v);
    v.cross(vPath, v);
    if (angle != 0)
      navigate(0, v, angle * degreesPerRadian);
    matrixRotate.transform(pt0, pt0s);
    Point3f pt2 = new Point3f(ptVectorWing);
    pt2.add(pt0);
    Point3f pt2s = new Point3f();
    matrixRotate.transform(pt2, pt2s);
    vPath.set(pt2s);
    vPath.sub(pt0s);
    vPath.z = 0; // just use projection
    v.set(-1, 0, 0); // puts alpha helix sidechain above
    angle = vPath.angle(v);
    if (vPath.y < 0)
      angle = -angle;
    v.set(0, 0, 1);
    if (angle != 0)
      navigate(0, v, angle * degreesPerRadian);
    if (viewer.getNavigateSurface()) {
      // set downward viewpoint 20 degrees to horizon
      v.set(1, 0, 0);
      navigate(0, v, 20);
    }
    matrixRotate.transform(pt0, pt0s);
    matrixRotate.transform(pt1, pt1s);
    matrixRotate.transform(ptVectorWing, pt2s);
  }

  Point3f getNavigationCenter() {
    return navigationCenter;
  }

  float getNavigationDepthPercent() {
    return navigationDepth;
  }
  
  void setNavigationSlabOffsetPercent(float percent) {
    viewer.getGlobalSettings().setParameterValue("navigationSlab", percent);
    calcCameraFactors(); //current
    navigationSlabOffset = percent / 50 * modelRadiusPixels;
  }

  private float getNavigationSlabOffsetPercent() {
    calcCameraFactors(); //current
    return 50 * navigationSlabOffset / modelRadiusPixels;
  }

  Point3f getNavigationOffset() {
    transformPoint(navigationCenter, navigationOffset);
    return navigationOffset;
  }

  private void setNavigationDepthPercent(float percent) {
    // navigation depth 0 # place user at rear plane of the model
    // navigation depth 100 # place user at front plane of the model

    viewer.getGlobalSettings().setParameterValue("navigationDepth", percent);
    calcCameraFactors(); //current
    modelCenterOffset = referencePlaneOffset - (1 - percent / 50) * modelRadiusPixels;
    calcCameraFactors(); //updated
    navMode = NAV_MODE_ZOOMED;
  }

  private void calcNavigationDepthPercent() {
    calcCameraFactors(); //current
    navigationDepth = (modelRadiusPixels == 0 ? 50 : 
      50 * (1 + (modelCenterOffset - referencePlaneOffset) / modelRadiusPixels));
  }

  float getNavigationOffsetPercent(char XorY) {
    getNavigationOffset();
    if (width == 0 || height == 0)
      return 0;
    return (XorY == 'X' ? (navigationOffset.x - width / 2f) * 100f / width
        : (navigationOffset.y - getNavPtHeight()) * 100f / height);
  }

  protected String getNavigationText(boolean addComments) {
    getNavigationOffset();
    return (addComments ? " /* navigation center, translation, depth */ " : " ")
        + Escape.escape(navigationCenter) + " "
        + getNavigationOffsetPercent('X') + " "
        + getNavigationOffsetPercent('Y') + " " + getNavigationDepthPercent();
  }

  protected String getNavigationState() {
    if (!isNavigationMode)
      return "";
    return "# navigation state;\nnavigate 0 center "
        + Escape.escape(getNavigationCenter())
        + ";\nnavigate 0 translate " + getNavigationOffsetPercent('X') + " "
        + getNavigationOffsetPercent('Y') + ";\nset navigationDepth "
        + getNavigationDepthPercent() + ";\nset navigationSlab "
        + getNavigationSlabOffsetPercent() + ";\n\n";
  }
  
}
