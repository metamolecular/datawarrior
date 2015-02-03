/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-05 07:42:12 -0500 (Fri, 05 Jun 2009) $
 * $Revision: 10958 $
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

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Bond.BondSet;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;

class ScriptVariable extends Token {

  final private static ScriptVariable vT = new ScriptVariable(on, 1, "true");
  final private static ScriptVariable vF = new ScriptVariable(off, 0, "false");
  static ScriptVariable vAll = new ScriptVariable(all, "all");

  int index = Integer.MAX_VALUE;

  String name;

  private final static int FLAG_CANINCREMENT = 1;
  private final static int FLAG_LOCALVAR = 2;

  int flags = ~FLAG_CANINCREMENT & FLAG_LOCALVAR;

  ScriptVariable() {
    tok = string;
    value = "";
  }

  ScriptVariable(int tok) {
    this.tok = tok;
  }

  ScriptVariable(int tok, int intValue, Object value) {
    super(tok, intValue, value);
  }

  ScriptVariable(int tok, Object value) {
    super(tok, value);
  }

  ScriptVariable(int tok, int intValue) {
    super(tok, intValue);
  }

  ScriptVariable(BitSet bs, int index) {
    value = bs;
    this.index = index;
    tok = bitset;
  }

  ScriptVariable(Token theToken) {
    tok = theToken.tok;
    intValue = theToken.intValue;
    value = theToken.value;
  }

  /**
   * @param x
   * @return  a ScriptVariable of the input type, or if x is null, then a new ScriptVariable,
   *     or, if the type is not found, null
   */
  static ScriptVariable getVariable(Object x) {
    if (x == null)
      return new ScriptVariable();
    if (x instanceof ScriptVariable)
      return (ScriptVariable) x;
    if (x instanceof String) {
      x = unescapePointOrBitsetAsVariable((String) x);
      if (x instanceof ScriptVariable)
        return (ScriptVariable) x;
      return new ScriptVariable(string, x);
    }
    if (x instanceof Boolean)
      return getBoolean(((Boolean)x).booleanValue());
    if (x instanceof Integer)
      return new ScriptVariable(integer, ((Integer) x).intValue());
    if (x instanceof Float)
      return new ScriptVariable(decimal, x);
    if (x instanceof float[]) {
      float[] f = (float[]) x;
      String[] s = new String[f.length];
      for (int i = f.length; --i >= 0; )
        s[i] = "" + f[i];
      return new ScriptVariable(list, s);
    }
    if (x instanceof double[]) {
      double[] f = (double[]) x;
      String[] s = new String[f.length];
      for (int i = f.length; --i >= 0; )
        s[i] = "" + f[i];
      return new ScriptVariable(list, s);
    }
    if (x instanceof Float[])
      return new ScriptVariable(listf, x);
    if (x instanceof String[])
      return new ScriptVariable(list, x);
    if (x instanceof String)
      return new ScriptVariable(string, x);
    if (x instanceof Vector3f)
      return new ScriptVariable(point3f, new Point3f((Vector3f) x));
    if (x instanceof Point3f)
      return new ScriptVariable(point3f, x);
    if (x instanceof Point4f)
      return new ScriptVariable(point4f, x);
    if (x instanceof BitSet)
      return new ScriptVariable(bitset, x);
    if (x instanceof Quaternion)
      return new ScriptVariable(point4f, ((Quaternion)x).toPoint4f());
    return null;
  }

  ScriptVariable set(ScriptVariable v) {
    index = v.index;
    intValue = v.intValue;
    tok = v.tok;
    if (tok == Token.list) {
      int n = ((String[])v.value).length;
      value = new String[n];
      System.arraycopy(v.value, 0, value, 0, n);
    } else {
      value = v.value;
    }
    return this;
  }

  ScriptVariable setName(String name) {
    this.name = name;
    flags |= FLAG_CANINCREMENT;
    //System.out.println("Variable: " + name + " " + intValue + " " + value);
    return this;
  }

  ScriptVariable setGlobal() {
    flags &= ~FLAG_LOCALVAR;
    return this;
  }

  boolean canIncrement() {
    return tokAttr(flags, FLAG_CANINCREMENT);
  }

  boolean increment(int n) {
    if (!canIncrement())
      return false;
    switch (tok) {
    case integer:
      intValue += n;
      break;
    case decimal:
      value = new Float(((Float) value).floatValue() + n);
      break;
    default:
      value = nValue(this);
      if (value instanceof Integer) {
        tok = integer;
        intValue = ((Integer) value).intValue();
      } else {
        tok = decimal;
      }
    }
    return true;
  }

  static ScriptVariable getVariableSelected(int index, Object value) {
    ScriptVariable v = new ScriptVariable(bitset, value);
    v.index = index;
    return v;
  }

  boolean asBoolean() {
    return bValue(this);
  }

  int asInt() {
    return iValue(this);
  }

  float asFloat() {
    return fValue(this);
  }

  String asString() {
    return sValue(this);
  }

  Object getValAsObj() {
    return (tok == integer ? new Integer(intValue) : value);
  }

  // math-related Token static methods

  final static Point3f pt0 = new Point3f();

  final static ScriptVariable intVariable(int intValue) {
    return new ScriptVariable(integer, intValue);
  }

  static Object oValue(ScriptVariable x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return Boolean.TRUE;
    case nada:
    case off:
      return Boolean.FALSE;
    case integer:
      return new Integer(x.intValue);
    case string:
      //return tValue((String) x.value).value;
    default:
      return x.value;
    }
  }

  static Object nValue(Token x) {
    int iValue = 0;
    switch (x == null ? nada : x.tok) {
    case integer:
      iValue = x.intValue;
      break;
    case decimal:
      return x.value;
    case string:
      if (((String) x.value).indexOf(".") >= 0)
        return new Float(toFloat((String) x.value));
      iValue = (int) toFloat((String) x.value);
    }
    return new Integer(iValue);
  }

  static boolean bValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return true;
    case off:
      return false;
    case integer:
      return x.intValue != 0;
    case decimal:
    case string:
    case list:
      return fValue(x) != 0;
    case bitset:
      return iValue(x) != 0;
    case point3f:
    case point4f:
      return Math.abs(fValue(x)) > 0.0001f;
    default:
      return false;
    }
  }

  static int iValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
    case list:
    case string:
    case point3f:
    case point4f:
      return (int) fValue(x);
    case bitset:
      return BitSetUtil.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static float fValue(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
      return 1;
    case off:
      return 0;
    case integer:
      return x.intValue;
    case decimal:
      return ((Float) x.value).floatValue();
    case list:
      int i = x.intValue;
      String[] list = (String[]) x.value;
      if (i == Integer.MAX_VALUE)
        return list.length;
    case string:
      return toFloat(sValue(x));
    case bitset:
      return iValue(x);
    case point3f:
      return ((Point3f) x.value).distance(pt0);
    case point4f:
      return Measure.distanceToPlane((Point4f) x.value, pt0);
    default:
      return 0;
    }
  }

  static float toFloat(String s) {
    if (s.equalsIgnoreCase("true"))
      return 1;
    if (s.equalsIgnoreCase("false") || s.length() == 0)
      return 0;
    return Parser.parseFloatStrict(s);
  }

  static String sValue(Token x) {
    if (x == null)
      return "";
    int i;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case point3f:
      return Escape.escape((Point3f) x.value);
    case point4f:
      return Escape.escape((Point4f) x.value);
    case bitset:
      return Escape.escape(bsSelect(x), !(x.value instanceof BondSet));
    case list:
      String[] list = (String[]) x.value;
      i = x.intValue;
      if (i <= 0)
        i = list.length - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > list.length ? "" : list[i - 1]);
      StringBuffer sb = new StringBuffer();
      for (i = 0; i < list.length; i++)
        sb.append(list[i]).append("\n");
      return sb.toString();
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i - 1);
    case decimal:
    default:
      return "" + x.value;
    }
  }

  static String sValue(ScriptVariable x) {
    if (x == null)
      return "";
    int i;
    switch (x.tok) {
    case on:
      return "true";
    case off:
      return "false";
    case integer:
      return "" + x.intValue;
    case point3f:
      return Escape.escape((Point3f) x.value);
    case point4f:
      return Escape.escape((Point4f) x.value);
    case bitset:
      return Escape.escape(bsSelect(x), !(x.value instanceof BondSet));
    case list:
      String[] list = (String[]) x.value;
      i = x.intValue;
      if (i <= 0)
        i = list.length - i;
      if (i != Integer.MAX_VALUE)
        return (i < 1 || i > list.length ? "" : list[i - 1]);
      StringBuffer sb = new StringBuffer();
      for (i = 0; i < list.length; i++)
        sb.append(list[i]).append("\n");
      return sb.toString();
    case string:
      String s = (String) x.value;
      i = x.intValue;
      if (i <= 0)
        i = s.length() - i;
      if (i == Integer.MAX_VALUE)
        return s;
      if (i < 1 || i > s.length())
        return "";
      return "" + s.charAt(i - 1);
    case decimal:
    default:
      return "" + x.value;
    }
  }

  static int sizeOf(Token x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case off:
      return -1;
    case integer:
      return -2;
    case decimal:
      return -4;
    case point3f:
      return -8;
    case point4f:
      return -16;
    case string:
      return ((String) x.value).length();
    case list:
      return x.intValue == Integer.MAX_VALUE ? ((String[]) x.value).length
          : sizeOf(selectItem(x));
    case bitset:
      return BitSetUtil.cardinalityOf(bsSelect(x));
    default:
      return 0;
    }
  }

  static String typeOf(ScriptVariable x) {
    switch (x == null ? nada : x.tok) {
    case on:
    case off:
      return "boolean";
    case integer:
      return "integer";
    case decimal:
      return "decimal";
    case point3f:
      return "point";
    case point4f:
      return "plane";
    case string:
      return "string";
    case list:
      return "array";
    case bitset:
      return "bitset";
    default:
      return "?";
    }
  }

  static String[] concatList(ScriptVariable x1, ScriptVariable x2) {
    String[] list1 = (x1.tok == list ? (String[]) x1.value : TextFormat.split(
        sValue(x1), "\n"));
    String[] list2 = (x2.tok == list ? (String[]) x2.value : TextFormat.split(
        sValue(x2), "\n"));
    String[] list = new String[list1.length + list2.length];
    int pt = 0;
    for (int i = 0; i < list1.length; i++)
      list[pt++] = list1[i];
    for (int i = 0; i < list2.length; i++)
      list[pt++] = list2[i];
    return list;
  }

  static BitSet bsSelect(Token token) {
    token = selectItem(token, Integer.MIN_VALUE);
    return (BitSet) token.value;
  }

  static BitSet bsSelect(ScriptVariable var) {
    if (var.index == Integer.MAX_VALUE)
      var = selectItem(var);
    return (BitSet) var.value;
  }

  static BitSet bsSelect(Token token, int n) {
    token = selectItem(token);
    token = selectItem(token, 1);
    token = selectItem(token, n);
    return (BitSet) token.value;
  }

  static ScriptVariable selectItem(ScriptVariable var) {
    // pass bitsets created by the select() or for() commands
    // and all arrays by reference
    if (var.index != Integer.MAX_VALUE || 
        var.tok == list && var.intValue == Integer.MAX_VALUE)
      return var;
    return (ScriptVariable) selectItem(var, Integer.MIN_VALUE);
  }

  static Token selectItem(Token var) {
    return selectItem(var, Integer.MIN_VALUE);
  }

  static ScriptVariable selectItem(ScriptVariable var, int i2) {
    return (ScriptVariable) selectItem((Token) var, i2);
  }

  static Token selectItem(Token tokenIn, int i2) {
    if (tokenIn.tok != bitset && tokenIn.tok != list && tokenIn.tok != string)
      return tokenIn;

    // negative number is a count from the end

    BitSet bs = null;
    String[] st = null;
    String s = null;

    int i1 = tokenIn.intValue;
    if (i1 == Integer.MAX_VALUE) {
      // no selections have been made yet --
      // we just create a new token with the
      // same bitset and now indicate either
      // the selected value or "ALL" (max_value)
      if (i2 == Integer.MIN_VALUE)
        i2 = i1;
      return new ScriptVariable(tokenIn.tok, i2, tokenIn.value);
    }
    int len = 0;
    boolean isInputSelected = (tokenIn instanceof ScriptVariable && ((ScriptVariable) tokenIn).index != Integer.MAX_VALUE);
    ScriptVariable tokenOut = new ScriptVariable(tokenIn.tok, Integer.MAX_VALUE);

    switch (tokenIn.tok) {
    case bitset:
      if (tokenIn.value instanceof BondSet) {
        tokenOut.value = new BondSet((BitSet) tokenIn.value,
            ((BondSet) tokenIn.value).getAssociatedAtoms());
        bs = (BitSet) tokenOut.value;
        len = BitSetUtil.cardinalityOf(bs);
        break;
      }
      bs = BitSetUtil.copy((BitSet) tokenIn.value);
      len = (isInputSelected ? 1 : BitSetUtil.cardinalityOf(bs));
      tokenOut.value = bs;
      break;
    case list:
      st = (String[]) tokenIn.value;
      len = st.length;
      break;
    case string:
      s = (String) tokenIn.value;
      len = s.length();
    }

    // "testing"[0] gives "g"
    // "testing"[-1] gives "n"
    // "testing"[3][0] gives "sting"
    // "testing"[-1][0] gives "ng"
    // "testing"[0][-2] gives just "g" as well
    if (i1 <= 0)
      i1 = len + i1;
    if (i1 < 1)
      i1 = 1;
    if (i2 == 0)
      i2 = len;
    else if (i2 < 0)
      i2 = len + i2;

    if (i2 > len)
      i2 = len;
    else if (i2 < i1)
      i2 = i1;

    switch (tokenIn.tok) {
    case bitset:
      if (isInputSelected) {
        if (i1 > 1)
          bs.clear();
        break;
      }
      len = BitSetUtil.length(bs);
      int n = 0;
      for (int j = 0; j < len; j++)
        if (bs.get(j) && (++n < i1 || n > i2))
          bs.clear(j);
      break;
    case string:
      if (i1 < 1 || i1 > len)
        tokenOut.value = "";
      else
        tokenOut.value = s.substring(i1 - 1, i2);
      break;
    case list:
      if (i1 < 1 || i1 > len || i2 > len)
        return new ScriptVariable(string, "");
      if (i2 == i1)
        return tValue(st[i1 - 1]);
      String[] list = new String[i2 - i1 + 1];
      for (int i = 0; i < list.length; i++)
        list[i] = st[i + i1 - 1];
      tokenOut.value = list;
      break;
    }
    return tokenOut;
  }

  static ScriptVariable tValue(String str) {
    Object v = unescapePointOrBitsetAsVariable(str);
    if (!(v instanceof String))
      return (ScriptVariable) v;
    String s = (String) v;
    if (s.toLowerCase() == "true")
      return getBoolean(true);
    if (s.toLowerCase() == "false")
      return getBoolean(false);
    float f = Parser.parseFloatStrict(s);
    return (Float.isNaN(f) ? new ScriptVariable(string, v) 
        : s.indexOf(".") < 0 ? new ScriptVariable(integer, (int) f)
        : new ScriptVariable(decimal, new Float(f)));
  }

  boolean setSelectedValue(int selector, ScriptVariable var) {
    if (selector == Integer.MAX_VALUE || tok != string && tok != list)
      return false;
    String s = sValue(var);
    switch (tok) {
    case list:
      String[] array = (String[]) value;
      if (selector <= 0)
        selector = array.length + selector;
      if (--selector < 0)
        selector = 0;
      String[] arrayNew = array;
      if (arrayNew.length <= selector) {
        value = arrayNew = ArrayUtil.ensureLength(array, selector + 1);
        for (int i = array.length; i <= selector; i++)
          arrayNew[i] = "";
      }
      arrayNew[selector] = s;
      break;
    case string:
      String str = (String) value;
      int pt = str.length();
      if (selector <= 0)
        selector = pt + selector;
      if (--selector < 0)
        selector = 0;
      while (selector >= str.length())
        str += " ";
      value = str.substring(0, selector) + s + str.substring(selector + 1);
      break;
    }
    return true;
  }

  String escape() {
    switch (tok) {
    case Token.on:
      return "true";
    case Token.off:
      return "false";
    case Token.integer:
      return "" + intValue;
    case Token.bitset:
      return Escape.escape((BitSet)value);
    case Token.list:
      return Escape.escape((String[])value);
    case Token.point3f:
      return Escape.escape((Point3f)value);
    case Token.point4f:
      return Escape.escape((Point4f)value);
    default:
      return Escape.escape(value);
    }
  }

  static Object unescapePointOrBitsetAsVariable(String s) {
    if (s == null || s.length() == 0)
      return s;
    Object v = Escape.unescapePointOrBitset(s);
    if (v instanceof Point3f)
      return (new ScriptVariable(Token.point3f, v));
    if (v instanceof Point4f)
      return new ScriptVariable(Token.point4f, v);
    if (v instanceof BitSet)
      return new ScriptVariable(Token.bitset, v);
    return s;
  }

  static ScriptVariable getBoolean(boolean value) {
    return new ScriptVariable(value ? vT : vF);
  }
  
  static Object sprintf(String strFormat, ScriptVariable var) {
    if (var == null)
      return strFormat;
    int[] vd = (strFormat.indexOf("d") >= 0 || strFormat.indexOf("i") >= 0 ? new int[1]
        : null);
    float[] vf = (strFormat.indexOf("f") >= 0 ? new float[1] : null);
    double[] ve = (strFormat.indexOf("e") >= 0 ? new double[1] : null);
    boolean getS = (strFormat.indexOf("s") >= 0);
    boolean getP = (strFormat.indexOf("p") >= 0 && var.tok == Token.point3f
        || strFormat.indexOf("q") >= 0 && var.tok == Token.point4f);
    Object[] of = new Object[] { vd, vf, ve, null, null};
    if (var.tok != Token.list)
      return sprintf(strFormat, var, of, vd, vf, ve, getS, getP);
    String[] list = (String[]) var.value;
    String[] list2 = new String[list.length];
    for (int i = 0; i < list.length; i++) {
      String s = strFormat;
      list2[i] = sprintf(s, tValue(list[i]), of, vd, vf, ve, getS, getP);
    }
    return list2;
  }

  private static String sprintf(String strFormat, ScriptVariable var, Object[] of, 
                                int[] vd, float[] vf, double[] ve, boolean getS, boolean getP) {
    if (vd != null)
      vd[0] = iValue(var);
    if (vf != null)
      vf[0] = fValue(var);
    if (ve != null)
      ve[0] = fValue(var);
    if (getS)
      of[3] = sValue(var);
    if (getP)
      of[4]= var.value;
    return TextFormat.sprintf(strFormat, of );
  }

  /**
   * sprintf       accepts arguments from the format() function
   *               First argument is a format string.
   * @param args
   * @return       formatted string
   */
  static String sprintf(ScriptVariable[] args) {
    switch(args.length){
    case 0:
      return "";
    case 1:
      return sValue(args[0]);
    }
    String[] format = TextFormat.split(TextFormat.simpleReplace(sValue(args[0]), "%%","\1"), '%');
    StringBuffer sb = new StringBuffer();
    sb.append(format[0]);
    for (int i = 1; i < format.length; i++)
      sb.append(sprintf(TextFormat.formatCheck("%" + format[i]), (i < args.length ? args[i] : null)));
    return sb.toString();
  }
  
  public String toString() {
    return super.toString() + "[" + name + "] index =" + index + " hashcode=" + hashCode();
  }
}
