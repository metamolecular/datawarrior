/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-21 14:42:28 +0200 (mar., 21 juil. 2009) $
 * $Revision: 11276 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
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

import java.util.BitSet;

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Point3fi;

import org.jmol.i18n.GT;
import org.jmol.modelset.MeasurementPending;

class PickingManager {

  private Viewer viewer;

  private int pickingMode = JmolConstants.PICKING_IDENT;
  private int pickingStyleSelect = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
  private int pickingStyleMeasure = JmolConstants.PICKINGSTYLE_MEASURE_OFF;

  private boolean drawHover;
  private int pickingStyle;
    
  private MeasurementPending measurementQueued;
  
  PickingManager(Viewer viewer) {
    this.viewer = viewer;
    clear();
  }

  void clear() {
    pickingMode = JmolConstants.PICKING_IDENT;
    drawHover = false;
  }
  
  void setPickingMode(int pickingMode) {
    this.pickingMode = pickingMode;
    resetMeasurement();
  }

  int getPickingMode() {
    return pickingMode;
  }
    
  private void resetMeasurement() {
    measurementQueued = new MeasurementPending(viewer.getModelSet());    
  }

  void setPickingStyle(int pickingStyle) {
    this.pickingStyle = pickingStyle;
    if (pickingStyle >= JmolConstants.PICKINGSTYLE_MEASURE_ON) {
      pickingStyleMeasure = pickingStyle;
      resetMeasurement();
    } else {
      pickingStyleSelect = pickingStyle;
    }
  }
  
  int getPickingStyle() {
    return pickingStyle;
  }

  void setDrawHover(boolean TF) {
    drawHover = TF;
  }
  
  boolean getDrawHover() {
    return drawHover;
  }

  void atomsPicked(BitSet bs, int modifiers) {
    if (BitSetUtil.firstSetBit(bs) < 0)
      return;
    pickSelected(Escape.escape(bs), modifiers, false);
  }  

  private void pickSelected(String spec, int modifiers, boolean isDoubleClick) {
    boolean shiftKey = ((modifiers & MouseManager.SHIFT) != 0);
    boolean alternateKey = ((modifiers & MouseManager.ALT) != 0);
    switch (pickingMode) {
    case JmolConstants.PICKING_LABEL:
      viewer.script("set labeltoggle " + spec);
      return;
    case JmolConstants.PICKING_IDENT:
    case JmolConstants.PICKING_SELECT_ATOM:
      applyMouseStyle(spec, shiftKey, alternateKey, isDoubleClick);
      viewer.clearClickCount();
      return;
    case JmolConstants.PICKING_SELECT_GROUP:
      applyMouseStyle("within(group, " + spec +")", shiftKey, alternateKey, isDoubleClick);
      viewer.clearClickCount();
      return;
    case JmolConstants.PICKING_SELECT_CHAIN:
      applyMouseStyle("within(chain, " + spec +")", shiftKey, alternateKey, isDoubleClick);
      viewer.clearClickCount();
      return;
    case JmolConstants.PICKING_SELECT_MOLECULE:
      applyMouseStyle("visible and within(molecule, " + spec +")", shiftKey, alternateKey, isDoubleClick);
      viewer.clearClickCount();
      return;
    case JmolConstants.PICKING_SELECT_SITE:
      applyMouseStyle("visible and within(site, " + spec +")", shiftKey, alternateKey, isDoubleClick);
      viewer.clearClickCount();
      return;
    case JmolConstants.PICKING_SELECT_MODEL:
      applyMouseStyle("within(model, " + spec +")", shiftKey, alternateKey, isDoubleClick);
      viewer.clearClickCount();
      return;
    case JmolConstants.PICKING_SELECT_ELEMENT:
      applyMouseStyle("visible and within(element, " + spec +")", shiftKey, alternateKey, isDoubleClick);
      viewer.clearClickCount();
      return;
    }
  }
  void atomPicked(int atomIndex, Point3fi ptClicked, int modifiers, boolean isDoubleClick) {
    // atomIndex < 0 is possible here.
    boolean shiftKey = ((modifiers & MouseManager.SHIFT) != 0);
    boolean alternateKey = ((modifiers & MouseManager.ALT) != 0);
    if (atomIndex < 0) {
      if (isDoubleClick)
        return;
      if (pickingStyleSelect == JmolConstants.PICKINGSTYLE_SELECT_PFAAT 
          && !shiftKey && !alternateKey)
        viewer.script("select none");
      resetMeasurement();
      if (pickingMode != JmolConstants.PICKING_SPIN)
        return;
    }
    int n = 2;
    switch (pickingMode) {
    case JmolConstants.PICKING_OFF:
      return;
    case JmolConstants.PICKING_MEASURE_TORSION:
      n++;
      //fall through
    case JmolConstants.PICKING_MEASURE_ANGLE:
      n++;
      //fall through
    case JmolConstants.PICKING_MEASURE:
    case JmolConstants.PICKING_MEASURE_DISTANCE:
      if (isDoubleClick)
        return;
      if (measurementQueued == null || measurementQueued.getCount() >= n)
        resetMeasurement();
      if (queueAtom(atomIndex, ptClicked) < n)
        return;
      viewer.setStatusMeasuring("measurePicked", n, measurementQueued.getStringDetail());
      if (pickingMode == JmolConstants.PICKING_MEASURE
          || pickingStyleMeasure == JmolConstants.PICKINGSTYLE_MEASURE_ON) {
        viewer.script("measure " + measurementQueued.getMeasurementScript(" ", true));
      }
      return;
    case JmolConstants.PICKING_CENTER:
      if (isDoubleClick)
        return;
      if (ptClicked == null)
        viewer.script("zoomTo (atomindex=" + atomIndex+")");
      else
        viewer.script("zoomTo " + Escape.escape(ptClicked));
      return;
    case JmolConstants.PICKING_SPIN:
      if (isDoubleClick)
        return;
      if (viewer.getSpinOn() || viewer.getNavOn() || viewer.getPendingMeasurement() != null) {
        resetMeasurement();
        viewer.script("spin off");
        return;
      }
      if (measurementQueued.getCount() >= 2)
        resetMeasurement();
      int queuedAtomCount = measurementQueued.getCount(); 
      if (queuedAtomCount == 1) {
        if (ptClicked == null) {
          if (measurementQueued.getAtomIndex(1) == atomIndex)
            return;
        } else {
          if (measurementQueued.getAtom(1).distance(ptClicked) == 0)
            return;
        }
      }
      if (atomIndex >= 0 || ptClicked != null)
        queuedAtomCount = queueAtom(atomIndex, ptClicked);
      if (queuedAtomCount < 2) {
        viewer.scriptStatus(queuedAtomCount == 1 ?
            GT.translate("pick one more atom in order to spin the model around an axis") :
            GT.translate("pick two atoms in order to spin the model around an axis"));
        return;
      }
      viewer.script("spin" + measurementQueued.getMeasurementScript(" ", false) + " " + viewer.getPickingSpinRate());
    }
    if (ptClicked != null)
      return;
    switch (pickingMode) {
    case JmolConstants.PICKING_IDENT:
      if (isDoubleClick)
        return;
      viewer.setStatusAtomPicked(atomIndex, null);
      return;
    case JmolConstants.PICKING_LABEL:
      if (isDoubleClick)
        return;
      viewer.script("set labeltoggle {atomindex="+atomIndex+"}");
      return;
    }
    pickSelected("atomindex=" + atomIndex, modifiers, isDoubleClick);
  }

  private int queueAtom(int atomIndex, Point3fi ptClicked) {
    int n = measurementQueued.addPoint(atomIndex, ptClicked, true);
    if (atomIndex >= 0)
      viewer.setStatusAtomPicked(atomIndex, "Atom #" + n + ":"
          + viewer.getAtomInfo(atomIndex));
    return n;
  }

  private void applyMouseStyle(String item, boolean shiftKey,
                               boolean alternateKey, boolean isDoubleClick) {
    item = "(" + item + ")";
    if (isDoubleClick) {
      viewer.script("select " + item);
    } else if (pickingStyleSelect == JmolConstants.PICKINGSTYLE_SELECT_PFAAT
        || pickingStyleSelect == JmolConstants.PICKINGSTYLE_SELECT_DRAG) {
      if (shiftKey && alternateKey)
        viewer.script("select selected and not " + item);
      else if (shiftKey)
        viewer.script("select selected tog " + item); //toggle
      else if (alternateKey)
        viewer.script("select selected or " + item);
      else
        viewer.script("select " + item);
    } else {
      if (shiftKey
          || pickingStyleSelect == JmolConstants.PICKINGSTYLE_SELECT_JMOL)
        viewer.script("select selected tog " + item); //toggle
      else
        viewer.script("select " + item);
    }
  }

}
