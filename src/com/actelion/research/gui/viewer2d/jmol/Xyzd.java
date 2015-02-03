/* $RCSfile: Xyzd.java,v $
 * $Author: egonw $
 * $Date: 2004/10/26 11:11:13 $
 * $Revision: 1.2 $
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

public class Xyzd {
  public final static int GB = 0x8000; // guard bit
  public final static int OV = 0x4000; // overflow bit
  public final static int ZO = 2000;   // zero offset
  public final static int MIN = -ZO;
  public final static int MAX = 0x3FFF - ZO;
  public final static int MASK = 0x3FFF;
  public final static int GBZO = GB + ZO; // guard bit + zero offset

  public final static long LGB = GB;
  public final static long LOV = OV;
  public final static long LMASK = MASK;
  public final static long LZO = ZO;
  public final static long ZO4 = (LZO << 48) | (LZO << 32) | (LZO << 16) | LZO;
  public final static long GB4 = (LGB << 48) | (LGB << 32) | (LGB << 16) | LGB;
  public final static long GBZO4 = GB4 | ZO4;
  public final static long OV4 = (LOV << 48) | (LOV << 32) | (LOV << 16) | LOV;
  public final static long MASK4 = (LMASK<<48)|(LMASK<<32)|(LMASK<<16)|LMASK;

  public final static long ZO3 = (LZO << 32) | (LZO << 16) | LZO;
  public final static long OV3 = (LOV << 32) | (LOV << 16) | LOV;
  public final static long MASK3 = (LMASK<<32) | (LMASK<<16) | LMASK;

  public final static long NaN = Long.MIN_VALUE;

  public final static void main(String[] argv) {
    System.out.println("Hello world!");

    long xyzdZero = getXyzd(0, 0, 0, 0);
    System.out.print("xyzdZero:");
    printXyzd(xyzdZero);
    System.out.println("isAnyNegative(xyzdZero)=" + isAnyNegative(xyzdZero));

    long xyzdNeg1 = getXyzd(-1, -1, -1, 0);
    System.out.print("xyzdNeg1:");
    printXyzd(xyzdNeg1);
    System.out.println("isAnyNegative(xyzdNeg1)=" + isAnyNegative(xyzdNeg1));
    
    long xyzd1000 = getXyzd(1000, 1000, 1000, 0);
    System.out.print("xyzd1000:");
    printXyzd(xyzd1000);
    System.out.println("isAnyNegative(xyzd1000)=" + isAnyNegative(xyzd1000));

    System.out.println("isAnyLess(xyzdNeg1, xyzdZero)=" +
                       isAnyLess(xyzdNeg1, xyzdZero));

    System.out.println("isAnyGreater(xyzdNeg1, xyzdZero)=" +
                       isAnyGreater(xyzdNeg1, xyzdZero));

    System.out.println("isAnyLess(xyzdZero, xyzdNeg1)=" +
                       isAnyLess(xyzdZero, xyzdNeg1));

    System.out.println("isAnyGreater(xyzdZero, xyzdNeg1)=" +
                       isAnyGreater(xyzdZero, xyzdNeg1));

  }

  public final static long getXyzd(int x, int y, int z, int d) {
    return
      (((long)((((d < MIN ? MIN : (d > MAX ? MAX : d)) + ZO) << 16) |
               ((z < MIN ? MIN : (z > MAX ? MAX : z)) + ZO)) << 32) |
       (((y < MIN ? MIN : (y > MAX ? MAX : y)) + ZO) << 16) |
       (((x < MIN ? MIN : (x > MAX ? MAX : x))) + ZO));
  }
  
  public final static int getX(long xyzd) { return (((int)xyzd) & MASK) - ZO; }
  public final static int getY(long xyzd) { return (((int)xyzd>>16)&MASK)-ZO; }
  public final static int getZ(long xyzd) { return ((int)(xyzd>>32)&MASK)-ZO; }
  public final static int getD(long xyzd) { return ((int)(xyzd>>48)&MASK)-ZO; }

  public final static boolean isAnyNegative(long xyzd) {
    return ((xyzd - ZO3) & OV3) != 0;
  }

  public final static boolean isAnyLess(long xyzd, long xyzdMin) {
    return ((xyzd - xyzdMin) & OV3) != 0;
  }

  public final static boolean isAnyGreater(long xyzd, long xyzdMax) {
    return ((xyzdMax - xyzd) & OV3) != 0;
  }

  public final static void printXyzd(long xyzd) {
    System.out.println("xyzd=" + xyzd +
                       " x=" + getX(xyzd) +
                       " y=" + getY(xyzd) +
                       " z=" + getZ(xyzd) +
                       " d=" + getD(xyzd));
  }

}
