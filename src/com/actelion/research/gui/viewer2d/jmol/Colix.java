/* $RCSfile: Colix.java,v $
 * $Author: egonw $
 * $Date: 2004/10/26 11:11:12 $
 * $Revision: 1.3 $
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

import java.awt.Color;


/**
 *<p>
 * Implements a color index model using a colix as a
 * <strong>COLor IndeX</strong>.
 *</p>
 *<p>
 * A colix is a color index represented as a short int.
 *</p>
 *<p>
 * The value 0 is considered a null value ... for no color. In Jmol this
 * generally means that the value is inherited from some other object.
 *</p>
 */
public class Colix {


  private static short colixMax = 1;
  private static int[] argbs = new int[128];
  private static Color[] colors = new Color[128];
  private static int[][] ashades = new int[128][];

  public static short getColix(int argb) {
    if (argb == 0)
      return 0;
    argb |= 0xFF000000;
    for (int i = colixMax; --i >= 0; )
      if (argb == argbs[i])
        return (short)i;
    if (colixMax == argbs.length) {
      int oldSize = argbs.length;
      int[] t0 = new int[oldSize * 2];
      System.arraycopy(argbs, 0, t0, 0, oldSize);
      argbs = t0;

      Color[] t1 = new Color[oldSize * 2];
      System.arraycopy(colors, 0, t1, 0, oldSize);
      colors = t1;

      int[][] t2 = new int[oldSize * 2][];
      System.arraycopy(ashades, 0, t2, 0, oldSize);
      ashades = t2;
    }
    argbs[colixMax] = argb;
    return colixMax++;
  }

  public static short getColix(Color color) {
    if (color == null)
      return 0;
    int argb = color.getRGB();
    short colix = getColix(argb);
    if (colors[colix] == null && (argb & 0xFF000000) == 0xFF000000)
      colors[colix] = color;
    return colix;
  }

  public static Color getColor(short colix) {
    if (colix == 0)
      return null;
    Color color = colors[colix];
    if (color == null)
      color = colors[colix] = new Color(argbs[colix]);
    return colors[colix];
  }

  public static int getArgb(short colix) {
    return argbs[colix];
  }

  public static int[] getShades(short colix) {
    int[] shades = ashades[colix];
    if (shades == null)
      shades = ashades[colix] = Shade3D.getShades(argbs[colix]);
    return shades;
  }

  public static void flushShades() {
    for (int i = colixMax; --i >= 0; )
      ashades[i] = null;
  }
}
