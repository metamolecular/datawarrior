/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-13 12:24:16 +0200 (jeu., 13 août 2009) $
 * $Revision: 11312 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.api.JmolRendererInterface;
import org.jmol.g3d.*;
import org.jmol.modelset.ModelSet;
import org.jmol.shape.Shape;
import org.jmol.shape.ShapeRenderer;
import org.jmol.util.Logger;

import java.awt.Component;
import java.awt.Rectangle;

public class RepaintManager {

  private Viewer viewer;

  RepaintManager(Viewer viewer) {
    this.viewer = viewer;
  }

  void clear() {
    clear(-1);
  }

  private int holdRepaint = 0;
  boolean repaintPending;

  void pushHoldRepaint() {
    ++holdRepaint;
  }

  void popHoldRepaint() {
    --holdRepaint;
    if (holdRepaint <= 0) {
      holdRepaint = 0;
      repaintPending = true;
      repaintDisplay();
    }
  }

  boolean refresh() {
    if (repaintPending)
      return false;
    repaintPending = true;
    if (holdRepaint == 0) {
      repaintDisplay();
    }
    return true;
  }

  synchronized void requestRepaintAndWait() {
    repaintDisplay();
    try {
      wait();
    } catch (InterruptedException e) {
    }
  }

  private void repaintDisplay() {
    Component display = viewer.getDisplay();
    if (display == null)
      return;
    display.repaint();
  }

  synchronized void repaintDone() {
    repaintPending = false;
    notify(); // to cancel any wait in requestRepaintAndWait()
  }

  void render(Graphics3D g3d, ModelSet modelSet) {// , Rectangle rectClip
    if (!viewer.getRefreshing())
      return;
    render1(g3d, modelSet); // , rectClip
    Rectangle band = viewer.getRubberBandSelection();
    if (band != null && g3d.setColix(viewer.getColixRubberband()))
      g3d.drawRect(band.x, band.y, 0, 0, band.width, band.height);
  }

  private boolean logTime;

  private void render1(Graphics3D g3d, ModelSet modelSet) { // , Rectangle rectClip

    if (modelSet == null || !viewer.mustRenderFlag())
      return;

    logTime = viewer.getTestFlag1();

    viewer.finalizeTransformParameters();

    if (logTime)
      Logger.startTimer();

    try {
      g3d.renderBackground();
      if (renderers ==  null)
        renderers = new ShapeRenderer[JmolConstants.SHAPE_MAX];
      for (int i = 0; i < JmolConstants.SHAPE_MAX && g3d.currentlyRendering(); ++i) {
        Shape shape = modelSet.getShape(i);
        if (shape == null)
          continue;
        getRenderer(i, g3d).render(g3d, modelSet, shape);
      }

    } catch (Exception e) {
      Logger
          .error("rendering error -- perhaps use \"set refreshing FALSE/TRUE\" ? ");
    }
    if (logTime)
      Logger.checkTimer("render time");
  }

  private ShapeRenderer[] renderers;
  
  void clear(int iShape) {
    if (renderers ==  null)
      return;
    if (iShape >= 0)
      renderers[iShape] = null;
    else
      for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i)
        renderers[i] = null;
  }

  private ShapeRenderer getRenderer(int shapeID, Graphics3D g3d) {
    if (renderers[shapeID] == null)
      renderers[shapeID] = allocateRenderer(shapeID, g3d);
    return renderers[shapeID];
  }

  private ShapeRenderer allocateRenderer(int shapeID, Graphics3D g3d) {
    String className = JmolConstants.getShapeClassName(shapeID) + "Renderer";
    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer) shapeClass.newInstance();
      renderer.setViewerG3dShapeID(viewer, g3d, shapeID);
      return renderer;
    } catch (Exception e) {
      Logger.error("Could not instantiate renderer:" + className, e);
    }
    return null;
  }

  String generateOutput(String type, Graphics3D g3d, ModelSet modelSet,
                        String fileName) {

    viewer.finalizeTransformParameters();

    JmolRendererInterface g3dExport = null;
    Object output = null;
    boolean isOK = false;
    try {
      if (fileName == null) {
        output = new StringBuffer();
      } else {
        if (fileName.charAt(0) == '?')
          fileName = viewer.dialogAsk("save", fileName.substring(1));
        if (fileName == null)
          return null;
        output = fileName;
      }
      Class export3Dclass = Class.forName("org.jmol.export.Export3D");
      g3dExport = (JmolRendererInterface) export3Dclass.newInstance();
      isOK = g3dExport.initializeExporter(type, viewer, g3d, output);
    } catch (Exception e) {
    }
    if (!isOK) {
      Logger.error("Cannot export " + type);
      return null;
    }
    g3dExport.renderBackground();
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = modelSet.getShape(i);
      if (shape == null)
        continue;
      ShapeRenderer generator = getGenerator(i, g3d);
      generator.setGenerator(true);
      generator.render(g3dExport, modelSet, shape);
      generator.setGenerator(false);
    }
    return g3dExport.finalizeOutput();
  }

  private ShapeRenderer getGenerator(int shapeID, Graphics3D g3d) {
    String className = "org.jmol.export."
        + JmolConstants.getShapeClassName(~shapeID) + "Generator";
    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer) shapeClass.newInstance();
      renderer.setViewerG3dShapeID(viewer, g3d, shapeID);
      return renderer;
    } catch (Exception e) {
      // no generator -- just use renderer
      return getRenderer(shapeID, g3d);
    }
  }
}
