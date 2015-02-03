/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Bond.BondSet;

/**
 * For defining the state, mostly
 * 
 */
public class Escape {

  public static String escape(Object x) {
    if (x instanceof String)
      return escape("" + x);
    if (x instanceof String[])
      return escape((String[]) x);
    return x.toString();
  }

  public static String escapeColor(int argb) {
    return "[x" + Graphics3D.getHexColorFromRGB(argb) + "]";
  }

  public static String escape(Point4f xyzw) {
    return "{" + xyzw.x + " " + xyzw.y + " " + xyzw.z + " " + xyzw.w + "}";
  }

  public static String escape(Tuple3f xyz) {
    return "{" + xyz.x + " " + xyz.y + " " + xyz.z + "}";
  }

  public static String escape(float[] f, boolean asArray) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < f.length; i++) {
      if (i > 0)
        sb.append('\n');
      sb.append(f[i]);
    }
    return sb.toString();
  }

  public static String escape(float[][] f, boolean addSemi) {
    StringBuffer sb = new StringBuffer();
    String eol = (addSemi ? ";\n" : "\n");
    for (int i = 0; i < f.length; i++)
      if (f[i] != null) {
        if (i > 0)
          sb.append(eol);
        for (int j = 0; j < f[i].length; j++)
          sb.append(f[i][j]).append('\t');
      }
    return sb.toString();
  }

  public static String escape(int[] f) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < f.length; i++) {
      if (i > 0)
        sb.append('\n');
      sb.append("" + f[i]);
    }
    return sb.toString();
  }

  private final static String escapable = "\\\\\tt\rr\nn\"\""; 
  public static String escape(String str) {
    if (str == null)
      return "\"\"";
    boolean haveEscape = false;
    int i = 0;
    for (; i < escapable.length(); i += 2)
      if (str.indexOf(escapable.charAt(i)) >= 0) {
        haveEscape = true;
        break;
      }
    if (haveEscape)
      while (i < escapable.length()) {
        int pt = -1;
        char ch = escapable.charAt(i++);
        char ch2 = escapable.charAt(i++);
        StringBuffer sb = new StringBuffer();
        int pt0 = 0;
        while ((pt = str.indexOf(ch, pt + 1)) >= 0) {
          sb.append(str.substring(pt0, pt)).append('\\').append(ch2);
          pt0 = pt + 1;
        }
        sb.append(str.substring(pt0, str.length()));
        str = sb.toString();
      }
    for (i = str.length(); --i >= 0;)
      if (str.charAt(i) > 0x7F)
        str = str.substring(0, i) + unicode(str.charAt(i))
            + str.substring(i + 1);
    return chop("\"" + str + "\"");
  }

  private static String chop(String s) {
    int len = s.length();
    if (len < 512)
      return s;
    StringBuffer sb = new StringBuffer();
    String sep = "\"\\\n    + \"";
    int pt = 0;
    for (int i = 72; i < len; pt = i, i += 72) {
      while (s.charAt(i - 1) == '\\')
        i++;
      sb.append((pt == 0 ? "" : sep)).append(s.substring(pt, i));
    }
    sb.append(sep).append(s.substring(pt, len));
    return sb.toString();
  }

  static String ESCAPE_SET = " ,./;:_+-~=><?'!@#$%^&*";
  static int nEscape = ESCAPE_SET.length();

  /**
   * Serialize a simple string-based array as a single string followed by a
   * .split(x) where x is some character not in the string. A bit kludgy, but it
   * works.
   * 
   * 
   * @param list
   *          list of strings to serialize
   * @return serialized array
   */
  public static String escape(String[] list) {
    if (list == null)
      return escape("");
    StringBuffer s = new StringBuffer();
    s.append("[");
    for (int i = 0; i < list.length; i++) {
      if (i > 0)
        s.append(", ");
      s.append(escapeNice(list[i]));
    }
    s.append("]");
    return s.toString();
  }

  public static String escapeDoubleArray(Object x) {
    // from isosurface area or volume calc
    if (x == null)
      return escape("");
    if (x instanceof Float)
      return "" + x;
    StringBuffer s = new StringBuffer();
    double[] list = (double[]) x;
    s.append("[");
    for (int i = 0; i < list.length; i++) {
      if (i > 0)
        s.append(", ");
      s.append(list[i]);
    }
    s.append("]");
    return s.toString();
  }

  private static String escapeNice(String s) {
    float f = Parser.parseFloatStrict(s);
    return (Float.isNaN(f) ? escape(s) : s);
  }

  private static String unicode(char c) {
    String s = "0000" + Integer.toHexString(c);
    return "\\u" + s.substring(s.length() - 4);
  }

  public static Object unescapePoint(String strPoint) {
    if (strPoint == null || strPoint.length() == 0)
      return strPoint;
    String str = TextFormat.simpleReplace(strPoint, "\n", " ").trim();
    if (str.charAt(0) != '{' || str.charAt(str.length() - 1) != '}')
      return strPoint;
    float[] points = new float[5];
    int nPoints = 0;
    str = str.substring(1, str.length() - 1);
    int[] next = new int[1];
    for (; nPoints < 5; nPoints++) {
      points[nPoints] = Parser.parseFloat(str, next);
      if (Float.isNaN(points[nPoints])) {
        if (next[0] >= str.length() || str.charAt(next[0]) != ',')
          break;
        next[0]++;
        nPoints--;
      }
    }
    if (nPoints == 3)
      return new Point3f(points[0], points[1], points[2]);
    if (nPoints == 4)
      return new Point4f(points[0], points[1], points[2], points[3]);
    return strPoint;
  }

  public static BitSet unescapeBitset(String strBitset) {
    if (strBitset == "{null}")
      return null;
    BitSet bs = new BitSet();
    int len = strBitset.length();
    int iPrev = -1;
    int iThis = -2;
    char ch;
    if (len < 3)
      return bs;
    for (int i = 0; i < len; i++) {
      switch (ch = strBitset.charAt(i)) {
      case '}':
      case '{':
      case ' ':
        if (iThis < 0)
          break;
        if (iPrev < 0)
          iPrev = iThis;
        for (int j = iPrev; j <= iThis; j++)
          bs.set(j);
        iPrev = -1;
        iThis = -2;
        break;
      case ':':
        iPrev = iThis;
        iThis = -2;
        break;
      default:
        if (Character.isDigit(ch)) {
          if (iThis < 0)
            iThis = 0;
          iThis = (iThis << 3) + (iThis << 1) + (ch - '0');
        }
      }
    }
    return bs;
  }

  /**
   * accepts [xRRGGBB] or [0xRRGGBB] or [0xFFRRGGBB] 
   * or #RRGGBB or [red,green,blue]
   * or a valid JavaScript color
   * 
   * @param strColor
   * @return 0 if invalid or integer color 
   */
  public static int unescapeColor(String strColor) {
    return Graphics3D.getArgbFromString(strColor);
  }
  
  public static String escape(BitSet bs, boolean isAtoms) {
    char chOpen = (isAtoms ? '(' : '[');
    char chClose = (isAtoms ? ')' : ']');
    if (bs == null)
      return chOpen + "{}" + chClose;
    StringBuffer s = new StringBuffer(chOpen + "{");
    int imax = bs.size();
    int iLast = -1;
    int iFirst = -2;
    int i = -1;
    while (++i <= imax) {
      boolean isSet = bs.get(i);
      if (i == imax || iLast >= 0 && !isSet) {
        if (iLast >= 0 && iFirst != iLast)
          s.append((iFirst == iLast - 1 ? " " : ":") + iLast);
        if (i == imax)
          break;
        iLast = -1;
      }
      if (bs.get(i)) {
        if (iLast < 0) {
          s.append((iFirst == -2 ? "" : " ") + i);
          iFirst = i;
        }
        iLast = i;
      }
    }
    s.append("}").append(chClose);
    return s.toString();
  }

  public static String escape(BitSet bs) {
    return escape(bs, true);
  }

  private static String packageJSON(String infoType, StringBuffer sb) {
    return packageJSON(infoType, sb.toString());
  }

  private static String packageJSON(String infoType, String info) {
    if (infoType == null)
      return info;
    return "\"" + infoType + "\": " + info;
  }

  private static String packageReadable(String infoName, String infoType,
                                        StringBuffer sb) {
    return packageReadable(infoName, infoType, sb.toString());
  }
  
  private static String packageReadable(String infoName, String infoType,
                                        String info) {
    String s = (infoType == null ? "" : infoType + "\t");
    if (infoName == null)
      return s + info;
    return "\n" + infoName + "\t" + info;
  }

  private static String fixString(String s) {
    if (s == null || s.indexOf("{\"") == 0) //don't doubly fix JSON strings when retrieving status
      return s;
    s = TextFormat.simpleReplace(s, "\"", "''");
    s = TextFormat.simpleReplace(s, "\n", " | ");
    return "\"" + s + "\"";
  }

  public static String toJSON(String infoType, Object info) {

    //Logger.debug(infoType+" -- "+info);

    StringBuffer sb = new StringBuffer();
    String sep = "";
    if (info == null)
      return packageJSON(infoType, (String) null);
    if (info instanceof Integer || info instanceof Float)
      return packageJSON(infoType, info.toString());
    if (info instanceof String)
      return packageJSON(infoType, fixString((String) info));
    if (info instanceof String[]) {
      sb.append("[");
      int imax = ((String[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(fixString(((String[]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof int[]) {
      sb.append("[");
      int imax = ((int[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(((int[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof float[]) {
      sb.append("[");
      int imax = ((float[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(((float[]) info)[i]);
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof int[][]) {
      sb.append("[");
      int imax = ((int[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((int[][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof float[][]) {
      sb.append("[");
      int imax = ((float[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((float[][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof Vector) {
      sb.append("[ ");
      int imax = ((Vector) info).size();
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toJSON(null, ((Vector) info).get(i)));
        sep = ",";
      }
      sb.append(" ]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof Matrix3f) {
      sb.append("[[").append(((Matrix3f) info).m00).append(",")
        .append(((Matrix3f) info).m01).append(",")
        .append(((Matrix3f) info).m02).append("]")
        .append(",[").append(((Matrix3f) info).m10).append(",")
        .append(((Matrix3f) info).m11).append(",")
        .append(((Matrix3f) info).m12).append("]")
        .append(",[").append(((Matrix3f) info).m20).append(",")
        .append(((Matrix3f) info).m21).append(",")
        .append(((Matrix3f) info).m22).append("]]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof Tuple3f) {
      sb.append("[").append(((Tuple3f) info).x).append(",")
        .append(((Tuple3f) info).y).append(",")
        .append(((Tuple3f) info).z).append("]");
      return packageJSON(infoType, sb);
    }
    if (info instanceof Hashtable) {
      sb.append("{ ");
      Enumeration e = ((Hashtable) info).keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        sb.append(sep)
            .append(packageJSON(key, toJSON(null, ((Hashtable) info).get(key))));
        sep = ",";
      }
      sb.append(" }");
      return packageJSON(infoType, sb);
    }
    return packageJSON(infoType, fixString(info.toString()));
  }

  public static String toReadable(Object info) {
    return toReadable(null, info);
  }
  
  public static String toReadable(String infoType, Object info) {
    StringBuffer sb =new StringBuffer();
    String sep = "";
    if (info == null)
      return "null";
    if (info instanceof String)
      return packageReadable(infoType, null, escape((String) info));
    if (info instanceof String[]) {
      sb.append("array(");
      int imax = ((String[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(escape(((String[]) info)[i]));
        sep = ",";
      }
      sb.append(")");
      return packageReadable(infoType, "String[" + imax + "]", sb);
    }
    if (info instanceof int[]) {
      sb.append("array(");
      int imax = ((int[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(((int[]) info)[i]);
        sep = ",";
      }
      sb.append(")");
      return packageReadable(infoType, "int[" + imax + "]", sb);
    }
    if (info instanceof float[]) {
      sb.append("array(");
      int imax = ((float[]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(((float[]) info)[i]);
        sep = ",";
      }
      sb.append(")");
      return packageReadable(infoType, "float[" + imax + "]", sb);
    }
    if (info instanceof int[][]) {
      sb.append("[");
      int imax = ((int[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toReadable(null, ((int[][]) info)[i]));
        sep = ",";
      }
      sb.append("]");
      return packageReadable(infoType, "int[" + imax + "][]", sb);
    }
    if (info instanceof float[][]) {
      sb.append("[\n");
      int imax = ((float[][]) info).length;
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toReadable(null, ((float[][]) info)[i]));
        sep = ",\n";
      }
      sb.append("]");
      return packageReadable(infoType, "float[][]", sb);
    }
    if (info instanceof Vector) {
      sb.append("");
      int imax = ((Vector) info).size();
      for (int i = 0; i < imax; i++) {
        sb.append(sep).append(toReadable(null, ((Vector) info).get(i)));
        sep = ",";
      }
      return packageReadable(infoType, "Vector[" + imax + "]", sb);
    }
    if (info instanceof Matrix3f) {
      sb.append("[[").append(((Matrix3f) info).m00).append(",")
      .append(((Matrix3f) info).m01).append(",")
      .append(((Matrix3f) info).m02).append("]")
      .append(",[").append(((Matrix3f) info).m10).append(",")
      .append(((Matrix3f) info).m11).append(",")
      .append(((Matrix3f) info).m12).append("]")
      .append(",[").append(((Matrix3f) info).m20).append(",")
      .append(((Matrix3f) info).m21).append(",")
      .append(((Matrix3f) info).m22).append("]]");
      return packageReadable(infoType, null, sb);
    }
    if (info instanceof Tuple3f) {
      sb.append(escape((Tuple3f) info));
      return packageReadable(infoType, null, sb);
    }
    if (info instanceof Hashtable) {
      Enumeration e = ((Hashtable) info).keys();
      while (e.hasMoreElements()) {
        String key = (String) e.nextElement();
        sb.append(sep).append(packageReadable(key, null, toReadable(null, ((Hashtable) info)
                .get(key))));
        sep = "";
      }
      sb.append("\n");
      return packageReadable(infoType, null, sb);
    }
    return packageReadable(infoType, null, info.toString());
  }

  public static String escapeModelFileNumber(int iv) {
    return "" + (iv / 1000000) + "." + (iv % 1000000);
  }

  public static Object encapsulateData(String name, Object data) {
    return "  DATA \"" + name + "\"\n" + 
        (data instanceof float[][] ?
          escape((float[][]) data, true) + ";\n"
        : data) + "    END \"" + name + "\";\n";
  }

  public static Object unescapePointOrBitset(String s) {
    Object v = s;
    if (s.charAt(0) == '{')
      v = unescapePoint(s);
    else if (s.indexOf("({") == 0 && s.indexOf("({") == s.lastIndexOf("({"))
      v = unescapeBitset(s);
    else if (s.indexOf("[{") == 0)
      v = new BondSet(unescapeBitset(s));
    return v;
  }
}
