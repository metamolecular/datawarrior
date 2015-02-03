/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
package org.jmol.util;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.jmol.viewer.Token;

final public class ArrayUtil {

  public static Object ensureLength(Object array, int minimumLength) {
    if (array != null && Array.getLength(array) >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static String[] ensureLength(String[] array, int minimumLength) {
    if (array != null && array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static float[] ensureLength(float[] array, int minimumLength) {
    if (array != null && array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static int[] ensureLength(int[] array, int minimumLength) {
    if (array != null && array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static short[] ensureLength(short[] array, int minimumLength) {
    if (array != null && array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static byte[] ensureLength(byte[] array, int minimumLength) {
    if (array != null && array.length >= minimumLength)
      return array;
    return setLength(array, minimumLength);
  }

  public static Object doubleLength(Object array) {
    return setLength(array, (array == null ? 16 : 2 * Array.getLength(array)));
  }

  public static String[] doubleLength(String[] array) {
    return setLength(array, (array == null ? 16 : 2 * array.length));
  }

  public static float[] doubleLength(float[] array) {
    return setLength(array, (array == null ? 16 : 2 * array.length));
  }

  public static int[] doubleLength(int[] array) {
    return setLength(array, (array == null ? 16 : 2 * array.length));
  }

  public static short[] doubleLength(short[] array) {
    return setLength(array, (array == null ? 16 : 2 * array.length));
  }

  public static byte[] doubleLength(byte[] array) {
    return setLength(array, (array == null ? 16 : 2 * array.length));
  }

  public static boolean[] doubleLength(boolean[] array) {
    return setLength(array, (array == null ? 16 : 2 * array.length));
  }

  public static Object setLength(Object array, int newLength) {
    if (array == null) {
      return null; // We can't allocate since we don't know the type of array
    }
    Object t = Array
        .newInstance(array.getClass().getComponentType(), newLength);
    int oldLength = Array.getLength(array);
    System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength
        : newLength);
    return t;
  }

  public static Object deleteElements(Object array, int firstElement,
                                     int nElements) {
    if (nElements == 0 || array == null)
      return array;
    int oldLength = Array.getLength(array);
    if (oldLength - nElements <= 0)
      return array;
    Object t = Array.newInstance(array.getClass().getComponentType(), oldLength
        - nElements);
    if (firstElement > 0)
      System.arraycopy(array, 0, t, 0, firstElement);
    int n = oldLength - firstElement - nElements;
    if (n > 0)
      System.arraycopy(array, firstElement + nElements, t, firstElement, n);
    return t;
  }

  public static String[] setLength(String[] array, int newLength) {
    String[] t = new String[newLength];
    if (array != null) {
      int oldLength = array.length;
      System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength
          : newLength);
    }
    return t;
  }

  public static float[] setLength(float[] array, int newLength) {
    float[] t = new float[newLength];
    if (array != null) {
      int oldLength = array.length;
      System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength
          : newLength);
    }
    return t;
  }

  public static int[] setLength(int[] array, int newLength) {
    int[] t = new int[newLength];
    if (array != null) {
      int oldLength = array.length;
      System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength
          : newLength);
    }
    return t;
  }

  public static int[] arrayCopy(int[] array, int i0, int n, boolean isReverse) {
    if (array == null)
      return null;
    int oldLength = array.length;
    if (n == -1) n = oldLength;
    if (n == -2) n = oldLength / 2;
    n = n - i0;
    int[] t = new int[n];
    System.arraycopy(array, i0, t, 0, n);
    if (isReverse)
      for (int i = n / 2; --i >= 0;)
        swap(t, i, n - 1 - i);
    return t;
  }

  public static short[] setLength(short[] array, int newLength) {
    short[] t = new short[newLength];
    if (array != null) {
      int oldLength = array.length;
      System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength
          : newLength);
    }
    return t;
  }

  public static byte[] setLength(byte[] array, int newLength) {
    byte[] t = new byte[newLength];
    if (array != null) {
      int oldLength = array.length;
      System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength
          : newLength);
    }
    return t;
  }

  public static boolean[] setLength(boolean[] array, int newLength) {
    boolean[] t = new boolean[newLength];
    if (array != null) {
      int oldLength = array.length;
      System.arraycopy(array, 0, t, 0, oldLength < newLength ? oldLength
          : newLength);
    }
    return t;
  }

  public static void swap(short[] array, int indexA, int indexB) {
    short t = array[indexA];
    array[indexA] = array[indexB];
    array[indexB] = t;
  }

  public static void swap(int[] array, int indexA, int indexB) {
    int t = array[indexA];
    array[indexA] = array[indexB];
    array[indexB] = t;
  }

  public static void swap(float[] array, int indexA, int indexB) {
    float t = array[indexA];
    array[indexA] = array[indexB];
    array[indexB] = t;
  }
  
  public static String dumpArray(String msg, float[][] A, int x1, int x2, int y1, int y2) {
    String s = "dumpArray: " + msg + "\n";
    for (int x = x1; x <= x2; x++)
      s += "\t*" + x + "*";
    for (int y = y2; y >= y1; y--) {
      s += "\n*" + y + "*";
      for (int x = x1; x <= x2; x++)
        s += "\t" + (x < A.length && y < A[x].length ? A[x][y] : Float.NaN);
    }
    return s;
  }

  public static String dumpIntArray(int[] A, int n) {
    String str = "";
    for (int i = 0; i < n; i++)
      str += " " + A[i];
    return str;
  }

  public static Object getMinMax(Object floatOrStringArray, int tok) {
    float[] data;
    if (floatOrStringArray instanceof String[]) {
      data = new float[((String[])floatOrStringArray).length];
      Parser.parseFloatArray((String[])floatOrStringArray, data);
    } else {
      data = (float[]) floatOrStringArray;
    }
    double sum;
    switch (tok) {
    case Token.min:
      sum = Float.MAX_VALUE;
      break;
    case Token.max:
      sum = -Float.MAX_VALUE;
      break;
    default:
      sum = 0;
    }
    double sum2 = 0;
    int n = 0;
    for (int i = data.length; --i >= 0; ) {
      float v;
      if (Float.isNaN(v = data[i]))
        continue;
      n++;
      switch(tok){
      case Token.sum2:
      case Token.stddev:
        sum2 += ((double) v) * v;
        //fall through
      case Token.average:
        sum += v;
        break;
      case Token.min:
        if (v < sum)
          sum = v;
        break;
      case Token.max:
        if (v > sum)
          sum = v;
        break;
      }
    }
    if (n == 0)
      return "NaN";
    switch (tok) {
    case Token.average:
      sum /= n;
      break;
    case Token.stddev:
      if (n == 1)
        return "NaN";
      sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
      break;
    case Token.sum2:
      sum = sum2;
      break;
    }
    return new Float(sum);
  }

  public static Object sortOrReverse(Object list, int tok, boolean checkFloat) {
    float[] f = null;
    if (list instanceof String[]) {
      String[] s = (String[]) list;
      if (s.length < 2)
        return list;
      if (checkFloat && !Float.isNaN(Parser.parseFloat(s[0]))) {
        f = new float[s.length];
        Parser.parseFloatArray(s, f);
      } else {
        String[] s2 = new String[s.length];
        System.arraycopy(s, 0, s2, 0, s.length);
        switch (tok) {
        case Token.sort:
          Arrays.sort(s2);
          return s2;
        case Token.reverse:
          for (int left = 0, right = s2.length - 1; left < right; left++, right--) {
            String temp = s2[left];
            s2[left] = s2[right];
            s2[right] = temp;
          }
          return s2;
        }
      }
    } else if (list instanceof float[]) {
      f = new float[((float[]) list).length];
      System.arraycopy(list, 0, f, 0, f.length);
      if (f.length < 2)
        return list;
    } else {
      return list;
    }
    switch (tok) {
    case Token.sort:
      Arrays.sort(f);
      break;
    case Token.reverse:
      for (int left = 0, right = f.length - 1; left < right; left++, right--) {
        float ftemp = f[left];
        f[left] = f[right];
        f[right] = ftemp;
      }
    }
    return f;
  }
}
