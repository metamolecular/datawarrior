/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-06-05 09:23:48 +0200 (jeu., 05 juin 2008) $
 * $Revision: 9464 $
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

import org.jmol.g3d.Font3D;

import java.awt.FontMetrics;

public class Frank extends FontLineShape {

  // Axes, Bbcage, Frank, Uccage

  final static String defaultFontName = "SansSerif";
  final static String defaultFontStyle = "Bold";
  final static int defaultFontSize = 16;
  final static int frankMargin = 4;

  String frankString = "Jmol";
  Font3D currentMetricsFont3d;
  Font3D baseFont3d;
  int frankWidth;
  int frankAscent;
  int frankDescent;

  public void initShape() {
    super.initShape();
    myType = "frank";
    baseFont3d = font3d = g3d.getFont3D(defaultFontName, defaultFontStyle, defaultFontSize);
    calcMetrics();
  }

  public boolean wasClicked(int x, int y) {
    int width = viewer.getScreenWidth();
    int height = viewer.getScreenHeight();
    return (width > 0 && height > 0 
        && x > width - frankWidth - frankMargin
        && y > height - frankAscent - frankMargin);
  }

  void calcMetrics() {
    if (viewer.isSignedApplet())
      frankString = "Jmol_S";
    if (font3d == currentMetricsFont3d) 
      return;
    currentMetricsFont3d = font3d;
    FontMetrics fm = font3d.fontMetrics;
    frankWidth = fm.stringWidth(frankString);
    frankDescent = fm.getDescent();
    frankAscent = fm.getAscent();
  }

  void getFont(float imageFontScaling) {
    font3d = g3d.getFont3DScaled(baseFont3d, imageFontScaling);
    calcMetrics();
  }
}
