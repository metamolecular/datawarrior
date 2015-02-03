/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-12 06:50:34 +0200 (dim., 12 juil. 2009) $
 * $Revision: 11203 $

 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.viewer.JmolConstants;

public class SticksRenderer extends ShapeRenderer {

  protected boolean showMultipleBonds;
  protected byte modeMultipleBond;
  //boolean showHydrogens;
  protected byte endcaps;

  protected boolean ssbondsBackbone;
  protected boolean hbondsBackbone;
  protected boolean bondsBackbone;
  protected boolean hbondsSolid;
  
  protected Atom atomA, atomB;
  protected Bond bond;
  int xA, yA, zA;
  int xB, yB, zB;
  int dx, dy;
  int mag2d;
  protected short colixA, colixB;
  protected int width;
  protected int bondOrder;
  private boolean renderWireframe;
  private boolean isAntialiased;


  protected void render() {
    endcaps = Graphics3D.ENDCAPS_SPHERICAL;
    showMultipleBonds = viewer.getShowMultipleBonds();
    modeMultipleBond = viewer.getModeMultipleBond();
    renderWireframe = viewer.getInMotion() && viewer.getWireframeRotation();
    ssbondsBackbone = viewer.getSsbondsBackbone();
    hbondsBackbone = viewer.getHbondsBackbone();
    bondsBackbone = hbondsBackbone | ssbondsBackbone;
    hbondsSolid = viewer.getHbondsSolid();
    isAntialiased = g3d.isAntialiased();
    Bond[] bonds = modelSet.getBonds();
    for (int i = modelSet.getBondCount(); --i >= 0; ) {
      bond = bonds[i];
      if ((bond.getShapeVisibilityFlags() & myVisibilityFlag) != 0) 
        renderBond();
    }
  }

  protected void renderBond() {
    mad = bond.getMad();
    atomA = bond.getAtom1();
    atomB = bond.getAtom2();
    if (!atomA.isInFrame() || !atomB.isInFrame()
        || !g3d.isInDisplayRange(atomA.screenX, atomA.screenY)
        || !g3d.isInDisplayRange(atomB.screenX, atomB.screenY)
        || modelSet.isAtomHidden(atomA.getAtomIndex())
        || modelSet.isAtomHidden(atomB.getAtomIndex()))
      return;

    colixA = atomA.getColix();
    colixB = atomB.getColix();
    if (((colix = bond.getColix()) & Graphics3D.OPAQUE_MASK) == Graphics3D.USE_PALETTE) {
      colix = (short) (colix & ~Graphics3D.OPAQUE_MASK);
      colixA = Graphics3D.getColixInherited((short) (colix | viewer
          .getColixAtomPalette(atomA, JmolConstants.PALETTE_CPK)), colixA);
      colixB = Graphics3D.getColixInherited((short) (colix | viewer
          .getColixAtomPalette(atomB, JmolConstants.PALETTE_CPK)), colixB);
    } else {
      colixA = Graphics3D.getColixInherited(colix, colixA);
      colixB = Graphics3D.getColixInherited(colix, colixB);
    }
    int order = bond.getOrder() & ~JmolConstants.BOND_NEW;
    if (bondsBackbone) {
      if (ssbondsBackbone && (order & JmolConstants.BOND_SULFUR_MASK) != 0) {
        // for ssbonds, always render the sidechain,
        // then render the backbone version
        /*
         mth 2004 04 26
         No, we are not going to do this any more
         render(bond, atomA, atomB);
         */

        atomA = atomA.getGroup().getLeadAtom(atomA);
        atomB = atomB.getGroup().getLeadAtom(atomB);
      } else if (hbondsBackbone
          && (order & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
        atomA = atomA.getGroup().getLeadAtom(atomA);
        atomB = atomB.getGroup().getLeadAtom(atomB);
      }
    }
    xA = atomA.screenX;
    yA = atomA.screenY;
    zA = atomA.screenZ;
    xB = atomB.screenX;
    yB = atomB.screenY;
    zB = atomB.screenZ;
    if (zA == 1 || zB == 1)
      return;
    dx = xB - xA;
    dy = yB - yA;
    width = viewer.scaleToScreen((zA + zB) / 2, mad);
    if (renderWireframe && width > 0)
      width = 1;
    bondOrder = getRenderBondOrder(order);
    switch (bondOrder) {
    case 1:
    case 2:
    case 3:
    case 4:
      renderBond(0);
      break;
    case JmolConstants.BOND_ORDER_UNSPECIFIED:
    case JmolConstants.BOND_AROMATIC_SINGLE:
      bondOrder = 1;
      renderBond(order == JmolConstants.BOND_AROMATIC_SINGLE ? 0 : 1);
      break;
    case JmolConstants.BOND_AROMATIC:
    case JmolConstants.BOND_AROMATIC_DOUBLE:
      bondOrder = 2;
      renderBond(order == JmolConstants.BOND_AROMATIC ? getAromaticDottedBondMask()
          : 0);
      break;
    case JmolConstants.BOND_STEREO_NEAR:
    case JmolConstants.BOND_STEREO_FAR:
      renderTriangle(bond);
      break;
    default:
      if ((bondOrder & JmolConstants.BOND_PARTIAL_MASK) != 0) {
        bondOrder = JmolConstants.getPartialBondOrder(order);
        renderBond(JmolConstants.getPartialBondDotted(order));
      } else if ((bondOrder & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
        if (hbondsSolid) {
          bondOrder = 1;
          renderBond(0);
        } else {
          renderHbondDashed();
        }
        break;
      }
    }
  }
    
  int getRenderBondOrder(int order) {
    order &= ~JmolConstants.BOND_NEW; 
    if ((order & JmolConstants.BOND_PARTIAL_MASK) != 0)
      return order;
    if ((order & JmolConstants.BOND_SULFUR_MASK) != 0)
      order &= ~JmolConstants.BOND_SULFUR_MASK;
    if ((order & JmolConstants.BOND_COVALENT_MASK) != 0) {
      if (order == 1 ||
          !showMultipleBonds ||
          modeMultipleBond == JmolConstants.MULTIBOND_NEVER ||
          (modeMultipleBond == JmolConstants.MULTIBOND_NOTSMALL &&
           mad > JmolConstants.madMultipleBondSmallMaximum)) {
        return 1;
      }
    }
    return order;
  }

  protected boolean lineBond;
  protected void renderBond(int dottedMask) {
    lineBond = (width <= 1);
    if (lineBond && (isAntialiased || isGenerator)) {
      width = 3;
      lineBond = false;
    }
      
    if (dx == 0 && dy == 0) {
      // end-on view
      if (! lineBond) {
        int space = width / 8 + 3;
        int step = width + space;
        int y = yA - (bondOrder - 1) * step / 2;
        do {
          fillCylinder(colixA, colixA, endcaps,
                           width, xA, y, zA, xA, y, zA);
          y += step;
        } while (--bondOrder > 0);
      }
      return;
    }
    if (bondOrder == 1) {
      if ((dottedMask & 1) != 0)
        drawDashed(xA, yA, zA, xB, yB, zB);
      else
        fillCylinder(colixA, colixB, endcaps,
                           width, xA, yA, zA, xB, yB, zB);
      return;
    }
    int dxB = dx * dx;
    int dyB = dy * dy;
    int mag2d2 = dxB + dyB;
    //if (bondOrder == 4)
      //mag2d2 *= 4;
    mag2d = (int)(Math.sqrt(mag2d2) + 0.5);
    resetAxisCoordinates();
    while (true) {
      if ((dottedMask & 1) != 0)
        drawDashed(xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
      else
        fillCylinder(colixA, colixB, endcaps, width,
                           xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
      dottedMask >>= 1;
      if (--bondOrder <= 0)
        break;
      stepAxisCoordinates();
    }
  }

  int xAxis1, yAxis1, xAxis2, yAxis2, dxStep, dyStep;

  void resetAxisCoordinates() {
    int space = mag2d >> 3;
    int step = width + space;
    dxStep = step * dy / mag2d; dyStep = step * -dx / mag2d;

    xAxis1 = xA; yAxis1 = yA; 
    xAxis2 = xB; yAxis2 = yB; 

    if (bondOrder > 1) {
      int f = (bondOrder - 1);
      xAxis1 -= dxStep * f / 2; yAxis1 -= dyStep * f / 2;
      xAxis2 -= dxStep * f / 2; yAxis2 -= dyStep * f / 2;
    }
  }


  void stepAxisCoordinates() {
    xAxis1 += dxStep; yAxis1 += dyStep;
    xAxis2 += dxStep; yAxis2 += dyStep;
  }

  /*private boolean isClipVisible(int xA, int yA, int xB, int yB) {
    // this is not actually correct, but quick & dirty
    int xMin, width, yMin, height;
    if (xA < xB) {
      xMin = xA;
      width = xB - xA;
    } else if (xB < xA) {
      xMin = xB;
      width = xA - xB;
    } else {
      xMin = xA;
      width = 1;
    }
    if (yA < yB) {
      yMin = yA;
      height = yB - yA;
    } else if (yB < yA) {
      yMin = yB;
      height = yA - yB;
    } else {
      yMin = yA;
      height = 1;
    }
    // there are some problems with this quick&dirty implementation
    // so I am going to throw in some slop
    xMin -= 5;
    yMin -= 5;
    width += 10;
    height += 10;
    rectTemp.x = xMin;
    rectTemp.y = yMin;
    rectTemp.width = width;
    rectTemp.height = height;
    boolean visible = rectClip.intersects(rectTemp);
    return visible;
  }*/

  /*private void renderDotted() {
    if (dx == 0 && dy == 0)
      return;
    g3d.drawDashedLine(colixA, colixB, 8, 4, xA, yA, zA, xB, yB, zB);
  }*/

  private static int wideWidthMilliAngstroms = 400;

  private void renderTriangle(Bond bond) {
    // for now, always solid, always opaque
    if (!g3d.checkTranslucent(false))
      return;
    int mag2d = (int)Math.sqrt(dx*dx + dy*dy);
    int wideWidthPixels = viewer.scaleToScreen(zB, wideWidthMilliAngstroms);
    int dxWide, dyWide;
    if (mag2d == 0) {
      dxWide = 0;
      dyWide = wideWidthPixels;
    } else {
      dxWide = wideWidthPixels * -dy / mag2d;
      dyWide = wideWidthPixels * dx / mag2d;
    }
    int xWideUp = xB + dxWide/2;
    int xWideDn = xWideUp - dxWide;
    int yWideUp = yB + dyWide/2;
    int yWideDn = yWideUp - dyWide;
    g3d.setColix(colixA);
    if (colixA == colixB) {
      g3d.drawfillTriangle(xA, yA, zA,
                           xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
    } else {
      int xMidUp = (xA + xWideUp) / 2;
      int yMidUp = (yA + yWideUp) / 2;
      int zMid = (zA + zB) / 2;
      int xMidDn = (xA + xWideDn) / 2;
      int yMidDn = (yA + yWideDn) / 2;
      g3d.drawfillTriangle(xA, yA, zA,
                           xMidUp, yMidUp, zMid, xMidDn, yMidDn, zMid);
      g3d.setColix(colixB);
      g3d.drawfillTriangle(xMidUp, yMidUp, zMid,
                           xMidDn, yMidDn, zMid, xWideDn, yWideDn, zB);
      g3d.drawfillTriangle(xMidUp, yMidUp, zMid,
                           xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
    }
  }

  
  private int getAromaticDottedBondMask() {
    Atom atomC = atomB.findAromaticNeighbor(atomA.getAtomIndex());
    if (atomC == null)
      return 1;
    //System.out.println("SticksRenderer atomA " + atomA.getInfo() + " atomB " + atomB.getInfo() + " atomC " + atomC.getInfo());
    int dxAC = atomC.screenX - xA;
    int dyAC = atomC.screenY - yA;
    return ((dx * dyAC - dy * dxAC) < 0 ? 2 : 1);
  }

  void drawDashed(int xA, int yA, int zA, int xB, int yB, int zB) {
    int dx = xB - xA;
    int dy = yB - yA;
    int dz = zB - zA;
    int i = 2;
    while (i <= 9) {
      int xS = xA + (dx * i) / 12;
      int yS = yA + (dy * i) / 12;
      int zS = zA + (dz * i) / 12;
      i += 3;
      int xE = xA + (dx * i) / 12;
      int yE = yA + (dy * i) / 12;
      int zE = zA + (dz * i) / 12;
      i += 2;
      fillCylinder(colixA, colixB, Graphics3D.ENDCAPS_FLAT, width,
                         xS, yS, zS, xE, yE, zE);
    }
  }

  void renderHbondDashed() {
   int dx = xB - xA;
    int dy = yB - yA;
    int dz = zB - zA;
    int i = 1;
    while (i < 10) {
      int xS = xA + (dx * i) / 10;
      int yS = yA + (dy * i) / 10;
      int zS = zA + (dz * i) / 10;
      short colixS = i < 5 ? colixA : colixB;
      i += 2;
      int xE = xA + (dx * i) / 10;
      int yE = yA + (dy * i) / 10;
      int zE = zA + (dz * i) / 10;
      short colixE = i < 5 ? colixA : colixB;
      ++i;
      fillCylinder(colixS, colixE, Graphics3D.ENDCAPS_FLAT, width,
                         xS, yS, zS, xE, yE, zE);
    }
  }
  
  ////////////////////////
  
  protected void fillCylinder(short colixA, short colixB, byte endcaps,
                              int diameter, int xA, int yA, int zA, int xB,
                              int yB, int zB) {
    if (lineBond)
      g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
    else
      g3d.fillCylinder(colixA, colixB, endcaps, diameter, xA, yA, zA, xB, yB,
          zB);
  }

}
