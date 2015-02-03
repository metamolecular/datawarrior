/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

package org.jmol.modelset;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.vecmath.Tuple3f;

import org.jmol.util.TextFormat;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;

public class LabelToken {
  
  /*
   * by Bob Hanson, 5/28/2009
   * 
   * a compiler for the atom label business.
   * 
   * Prior to this, once for every atom, twice for every bond, and 2-4 times for every
   * measurement we were scanning the format character by character. And if data were
   * involved, then calls were made for every atom to find the data set and return its
   * value. Now you can still do that, but the Jmol code doesn't. 
   * 
   * Instead, we now first compile a set of tokens -- either pure text or some
   * sort of %xxxx business. Generally we would alternate between these, so the
   * compiler is set up to initialize an array that has 2n+1 elements, where n is the
   * number of % signs in the string. This is guaranteed to be more than really necessary.
   * 
   * Because we are working with tokens, we can go beyond the limiting A-Za-z business
   * that we had before. That still works, but now we can have any standard token be
   * used in brackets:
   * 
   *   %n.m[xxxxx]
   * 
   * This complements the 
   * 
   *   %n.m{xxxxx}
   *   
   * used for data. The brackets make for a nice-looking format:
   * 
   * 
   *  print {*}.bonds.label("%6[atomName]1 - %6[atomName]2  %3ORDER  %6.2LENGTH")
   * 
   * [Note that the %ORDER and %LENGTH variables are bond labeling options, and 
   *  the 1 and 2 after %[xxx] indicate which atom in involved.
   * 
   * 
   */

  private String text; 
  private String key;
  private float[] data;
  private int tok;
  private int pt = -1;
  private char ch1;
  private int width;
  private int precision = Integer.MAX_VALUE;
  private boolean alignLeft;
  private boolean zeroPad;
  private boolean intAsFloat;

  // do not change array order without changing string order as well
  // new tokens can be added to the list at the end
  // and then also added in appendTokenValue()
  // and also in Eval, to atomProperty()
  
  final private static String labelTokenParams = "AaBbCcDEefGgIiLlMmNnoPpQqRrSsTtUuVvWXxYyZz%%%gqW";
  final private static int[] labelTokenIds = {
  /* 'A' */Token.altloc,
  /* 'a' */Token.atomName,
  /* 'B' */Token.atomType,
  /* 'b' */Token.temperature,
  /* 'C' */Token.formalCharge,
  /* 'c' */Token.chain,
  /* 'D' */Token.atomIndex,
  /* 'E' */Token.insertion,
  /* 'e' */Token.element,
  /* 'f' */Token.phi,
  /* 'G' */Token.groupindex,
  /* 'g' */'g', //getSelectedGroupIndexWithinChain()
  /* 'I' */Token.ionic,
  /* 'i' */Token.atomno,
  /* 'L' */Token.polymerLength,
  /* 'l' */Token.elemno,
  /* 'M' */Token.model,
  /* 'm' */Token.group1,
  /* 'N' */Token.molecule,
  /* 'n' */Token.group,
  /* 'o' */Token.symmetry,
  /* 'P' */Token.partialCharge,
  /* 'p' */Token.psi,
  /* 'Q' */'Q',  //occupancy 0.0 to 1.0
  /* 'q' */Token.occupancy,
  /* 'R' */Token.resno,
  /* 'r' */'r',
  /* 'S' */Token.site,
  /* 's' */Token.chain,
  /* 'T' */Token.straightness,
  /* 't' */Token.temperature,
  /* 'U' */Token.identify,
  /* 'u' */Token.surfacedistance,
  /* 'V' */Token.vanderwaals,
  /* 'v' */Token.vibXyz, 
  /* 'W' */'W',   // identifier and XYZ coord
  /* 'X' */Token.fracX, 
  /* 'x' */Token.atomX, 
  /* 'Y' */Token.fracY, 
  /* 'y' */Token.atomY, 
  /* 'Z' */Token.fracZ,
  /* 'z' */Token.atomZ, 

  // not having letter equivalents:
  
           Token.adpmax,
           Token.adpmin,
           Token.atomID,
           Token.bondcount,
           Token.color,
           Token.groupID,
           Token.covalent,
           Token.file,
           Token.format,
           Token.label,
           Token.modelindex,
           Token.property,
           Token.radius,
           Token.sequence,
           Token.spacefill,
           Token.structure,
           Token.strucno,
           Token.strucid,
           Token.symbol,
           Token.unitX,
           Token.unitY,
           Token.unitZ,
           Token.valence,
           Token.vibX,
           Token.vibY,
           Token.vibZ,
           Token.unitXyz,
           Token.fracXyz,
           Token.xyz,
           
  };

  private static boolean isLabelPropertyTok(int tok) {
    for (int i = labelTokenIds.length; --i >= 0;)
      if (labelTokenIds[i] == tok)
        return true;
    return false;
  }

  private final static String twoCharLabelTokenParams = "fuv";

  private final static int[] twoCharLabelTokenIds = { Token.fracX, Token.fracY,
      Token.fracZ, Token.unitX, Token.unitY, Token.unitZ, Token.vibX,
      Token.vibY, Token.vibZ, };

  public static final String STANDARD_LABEL = "%[identify]";

  private LabelToken(String text) {
    this.text = text;
  }

  private LabelToken(int pt) {
    this.pt = pt;
  }

  public static LabelToken[] compile(Viewer viewer, String strFormat, char chAtom, Hashtable htValues) {
    if (strFormat.indexOf("%") < 0)
      return new LabelToken[] { new LabelToken(strFormat) };
    int n = 0;
    int ich = -1;
    int cch = strFormat.length();
    while (++ich < cch && (ich = strFormat.indexOf('%', ich)) >= 0)
      n++;
    LabelToken[] tokens = new LabelToken[n * 2 + 1];
    int ichPercent;
    int i = 0;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) >= 0;) {
      if (ich != ichPercent)
        tokens[i++] = new LabelToken(strFormat.substring(ich, ichPercent));
      LabelToken lt = tokens[i++] = new LabelToken(ichPercent);
      viewer.autoCalculate(lt.tok);
      ich = setToken(viewer, strFormat, lt, cch, chAtom, htValues);
    }
    if (ich < cch)
      tokens[i++] = new LabelToken(strFormat.substring(ich));
    return tokens;
  }

  private static int setToken(Viewer viewer, String strFormat, LabelToken lt, int cch, int chAtom, Hashtable htValues) {
    int ich = lt.pt + 1;
    char ch;
    if (strFormat.charAt(ich) == '-') {
      lt.alignLeft = true;
      ++ich;
    }
    if (strFormat.charAt(ich) == '0') {
      lt.zeroPad = true;
      ++ich;
    }
    while (Character.isDigit(ch = strFormat.charAt(ich))) {
      lt.width = (10 * lt.width) + (ch - '0');
      ++ich;
    }
    lt.precision = Integer.MAX_VALUE;
    boolean isNegative = false;
    if (strFormat.charAt(ich) == '.') {
      ++ich;
      lt.intAsFloat = true;
      if ((ch = strFormat.charAt(ich)) == '-') {
        isNegative = true;
        ++ich;
      }
      if (Character.isDigit(ch = strFormat.charAt(ich))) {
        lt.precision = ch - '0';
        if (isNegative)
          lt.precision = -1 - lt.precision;
        ++ich;
      }
    }
    if (htValues != null) {
      Enumeration keys = htValues.keys();
      while (keys.hasMoreElements()) {
        String key = (String) keys.nextElement();
        if (strFormat.indexOf(key) == ich) {
          lt.key = key;
          return ich + key.length();
        }
      }
    }
    switch (ch = strFormat.charAt(ich++)) {
    case '%':
      lt.text = "%";
      return ich;
    case '[':
      int ichClose = strFormat.indexOf(']', ich);
      if (ichClose < ich) {
        ich = cch;
        break;
      }
      String propertyName = strFormat.substring(ich, ichClose).toLowerCase();
      if (propertyName.startsWith("property_")) {
        lt.text = propertyName;
        lt.tok = Token.data;
        lt.data = viewer.getDataFloat(lt.text);        
      } else {
        Token token = Token.getTokenFromName(propertyName);
        if (token != null && isLabelPropertyTok(token.tok))
          lt.tok = token.tok;
      }
      ich = ichClose + 1;
      break;
    case '{': // client property name
      int ichCloseBracket = strFormat.indexOf('}', ich);
      if (ichCloseBracket < ich) {
        ich = cch;
        break;
      }
      lt.text = strFormat.substring(ich, ichCloseBracket);
      lt.tok = Token.data;
      lt.data = viewer.getDataFloat(lt.text);
      ich = ichCloseBracket + 1;
      break;
    default:
      int i, i1;
      if (ich < cch 
          && (i = twoCharLabelTokenParams.indexOf(ch)) >= 0
          && (i1 = "xyz".indexOf(strFormat.charAt(ich))) >= 0) {
        lt.tok = twoCharLabelTokenIds[i * 3 + i1];
        ich++;
      } else if ((i = labelTokenParams.indexOf(ch)) >= 0) {
        lt.tok = labelTokenIds[i];
      }
    }
    lt.text = strFormat.substring(lt.pt, ich);
    if (chAtom != '\0' && ich < cch && Character.isDigit(ch = strFormat.charAt(ich))) {
      ich++;
      lt.ch1 = ch;
      if (ch != chAtom && chAtom != '\1')
        lt.tok = 0;
    }
    return ich;
  }

  //////////// label formatting for atoms, bonds, and measurements ///////////

  public static String formatLabel(Atom atom, String strFormat) {
    return formatLabel(atom, strFormat, null, '\0', null);
  }

  public static String formatLabel(Atom atom, String strFormat, LabelToken[] tokens, char chAtom, int[]indices) {
    if (atom == null || tokens == null && (strFormat == null || strFormat.length() == 0))
        return null;
    StringBuffer strLabel = (chAtom > '0' ? null : new StringBuffer());
    if (tokens == null)
      tokens = compile(atom.group.chain.modelSet.viewer, strFormat, chAtom, null);
    for (int i = 0; i < tokens.length; i++) {
      LabelToken t = tokens[i];
      if (t == null)
        break;
      if (chAtom > '0' && t.ch1 != chAtom)
        continue;
      if (t.tok <= 0 || t.key != null) {
        if (strLabel !=  null) {
          strLabel.append(t.text);
          if (t.ch1 != '\0')
            strLabel.append(t.ch1);
        }
      } else {
        appendAtomTokenValue(atom, t, strLabel, indices);
      }
    }
    return (strLabel == null ? null : strLabel.toString().intern());
  }
  
  private static void appendAtomTokenValue(Atom atom, LabelToken t,
                                           StringBuffer strLabel, int[] indices) {
    String strT = null;
    float floatT = Float.NaN;
    Tuple3f ptT = null;
    try {
      switch (t.tok) {
      
      // special cases only for labels 
      
      case Token.atomIndex:
        strT = ""
            + (indices == null ? atom.atomIndex : indices[atom.atomIndex]);
        break;
      case Token.color:
        ptT = Atom.atomPropertyTuple(atom, t.tok);
        break;
      case Token.data:
        floatT = (t.data != null ? t.data[atom.atomIndex] : Float.NaN);
        if (Float.isNaN(floatT))
          strT = atom.getClientAtomStringProperty(t.text);
        break;
      case Token.formalCharge:
        int formalCharge = atom.getFormalCharge();
        if (formalCharge > 0)
          strT = "" + formalCharge + "+";
        else if (formalCharge < 0)
          strT = "" + -formalCharge + "-";
        else
          strT = "0";
        break;
      case 'g':
        strT = "" + atom.getSelectedGroupIndexWithinChain();
        break;
      case Token.model:
        strT = atom.getModelNumberForLabel();
        break;
      case Token.occupancy:
        strT = "" + Atom.atomPropertyInt(atom, t.tok);
        break;
      case 'Q':
        floatT = atom.getOccupancy100() / 100f;
        break;
      case Token.radius:
        floatT = Atom.atomPropertyFloat(atom, t.tok);
        break;
      case 'r':
        strT = atom.getSeqcodeString();
        break;
      case Token.strucid:
        strT = atom.getStructureId();
        break;
      case Token.strucno:
        int id = atom.getStrucNo();
        strT = (id <= 0 ? "" : "" + id);
        break;
      case Token.straightness:
        floatT = atom.getStraightness();
        if (Float.isNaN(floatT))
          strT = "null";
        break;
      case Token.structure:
        strT = Atom.atomPropertyString(atom, t.tok);
        break;
      case 'W':
        strT = atom.getIdentityXYZ();
        break;
        
      // standard 
        
      default:
        switch (t.tok & Token.PROPERTYFLAGS) {
        case Token.intproperty:
          if (t.intAsFloat)
            floatT = Atom.atomPropertyInt(atom, t.tok);
          else
            strT = "" + Atom.atomPropertyInt(atom, t.tok);
          break;
        case Token.floatproperty:
          floatT = Atom.atomPropertyFloat(atom, t.tok);
          break;
        case Token.strproperty:
          strT = Atom.atomPropertyString(atom, t.tok);
          break;
        case Token.atomproperty:
          ptT = Atom.atomPropertyTuple(atom, t.tok);
        default:
          // any dual case would be here -- must handle specially
        }
      }
    } catch (IndexOutOfBoundsException ioobe) {
      floatT = Float.NaN;
      strT = null;
      ptT = null;
    }
    strT = t.format(floatT, strT, ptT);
    if (strLabel == null)
      t.text = strT;
    else
      strLabel.append(strT);
  }

  
  public static Hashtable getBondLabelValues() {
    Hashtable htValues = new Hashtable();
    htValues.put("#", "");
    htValues.put("ORDER", "");
    htValues.put("TYPE", "");
    htValues.put("LENGTH", new Float(0));
    return htValues;
  }

  public static String formatLabel(Bond bond, LabelToken[] tokens, Hashtable values, int[] indices) {
    values.put("#", "" + (bond.index + 1));
    values.put("ORDER", "" + bond.getOrderNumberAsString());
    values.put("TYPE", bond.getOrderName());
    values.put("LENGTH", new Float(bond.atom1.distance(bond.atom2)));
    setValues(tokens, values);
    formatLabel(bond.atom1, null, tokens, '1', indices);
    formatLabel(bond.atom2, null, tokens, '2', indices);
    return getLabel(tokens);
  }

  public static String labelFormat(Measurement measurement, String label, float value, String units) {
    Hashtable htValues = new Hashtable();
    htValues.put("#", "" + (measurement.getIndex() + 1));
    htValues.put("VALUE", new Float(value));
    htValues.put("UNITS", units);
    LabelToken[] tokens = compile(measurement.viewer, label, '\1', htValues);
    setValues(tokens, htValues);
    Atom[] atoms = measurement.modelSet.atoms;
    int[] indices = measurement.getCountPlusIndices();
    for (int i = indices[0]; i >= 1;--i)
      if (indices[i] >= 0)
        formatLabel(atoms[indices[i]], null, tokens, (char)('0' + i), null);
    label = getLabel(tokens);
    return (label == null ? "" : label);
  }

  public String format(float floatT, String strT, Tuple3f ptT) {
    if (!Float.isNaN(floatT)) {
      return TextFormat.format(floatT, width, precision, alignLeft, zeroPad);
    } else if (strT != null) {
      return TextFormat.format(strT, width, precision, alignLeft, zeroPad);
    } else if (ptT != null) {
      if (width == 0 && precision == Integer.MAX_VALUE) {
        width = 6;
        precision = 2;
      }
      return TextFormat.format(ptT.x, width, precision, false, false)
      + TextFormat.format(ptT.y, width, precision, false, false)
      + TextFormat.format(ptT.z, width, precision, false, false);
    } else {
      return text;
    }
  }

  public static void setValues(LabelToken[] tokens, Hashtable values) {
    for (int i = 0; i < tokens.length; i++) {
      LabelToken lt = tokens[i];
      if (lt == null)
        break;
      if (lt.key == null)
        continue;
      Object value = values.get(lt.key);
        lt.text = (value instanceof Float ? 
            lt.format(((Float)value).floatValue(), null, null)
            : lt.format(Float.NaN, (String) value, null));
    }    
  }

  public static String getLabel(LabelToken[] tokens) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tokens.length; i++) {
      LabelToken lt = tokens[i];
      if (lt == null)
        break;
      sb.append(lt.text);
    }
    return sb.toString();
  }

}
