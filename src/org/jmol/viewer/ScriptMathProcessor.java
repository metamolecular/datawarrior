/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-12 07:58:28 -0500 (Fri, 12 Jun 2009) $
 * $Revision: 11009 $
 *
 * Copyright (C) 2003-2006  Miguel, Jmol Development, www.jmol.org
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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.BoxInfo;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;
import org.jmol.viewer.ScriptEvaluator.ScriptException;

class ScriptMathProcessor {
  /**
   * Reverse Polish Notation Engine for IF, SET, and %{...} -- Bob Hanson
   * 2/16/2007 Just a (not so simple?) RPN processor that can handle boolean,
   * int, float, String, Point3f, and BitSet
   * 
   * hansonr@stolaf.edu
   * 
   */

  private boolean isSyntaxCheck;
  private boolean logMessages;
  private ScriptEvaluator eval;
  private Viewer viewer;

  private Token[] oStack = new Token[8];
  private ScriptVariable[] xStack = new ScriptVariable[8];
  private char[] ifStack = new char[8];
  private int ifPt = -1;
  private int oPt = -1;
  private int xPt = -1;
  private int parenCount;
  private int squareCount;
  private int braceCount;
  private boolean wasX;
  private int incrementX;
  private boolean isArrayItem;
  private boolean asVector;
  private int ptid = 0;
  private int ptx = Integer.MAX_VALUE;

  ScriptMathProcessor(ScriptEvaluator eval, boolean isArrayItem,
      boolean asVector) {
    this.eval = eval;
    this.viewer = eval.viewer;
    this.logMessages = eval.logMessages;
    this.isSyntaxCheck = eval.isSyntaxCheck;
    this.isArrayItem = isArrayItem;
    this.asVector = asVector || isArrayItem;
    wasX = isArrayItem;
    if (logMessages)
      Logger.info("initialize RPN");
  }

  ScriptVariable getResult(boolean allowUnderflow, String key)
      throws ScriptException {
    boolean isOK = true;
    ScriptVariable x = null;
    while (isOK && oPt >= 0)
      isOK = operate();
    if (isOK) {
      if (isArrayItem && xPt == 2 && xStack[1].tok == Token.leftsquare) {
        // stack is selector [ VALUE
        x = xStack[2];
        // asVector will be true;
      }
      if (asVector) {
        Vector result = new Vector();
        for (int i = 0; i <= xPt; i++)
          result.addElement(ScriptVariable.selectItem(xStack[i]));
        return new ScriptVariable(Token.vector, result);
      }
      if (xPt == 0) {
        if (x == null)
          x = xStack[0];
        if (x.tok == Token.bitset || x.tok == Token.list
            || x.tok == Token.string)
          x = ScriptVariable.selectItem(x);
        return x;
      }
    }
    if (!allowUnderflow && (xPt >= 0 || oPt >= 0)) {
      // iToken--;
      eval.error(ScriptEvaluator.ERROR_invalidArgument);
    }
    return null;
  }

  private void putX(ScriptVariable x) {
    // System.out.println("putX skipping : " + skipping + " " + x);
    if (skipping)
      return;
    if (++xPt == xStack.length)
      xStack = (ScriptVariable[]) ArrayUtil.doubleLength(xStack);
    if (logMessages) {
      Logger.info("\nputX: " + x);
    }
    xStack[xPt] = x;
    ptx = ++ptid;
  }

  private void putOp(Token op) {
    if (++oPt >= oStack.length)
      oStack = (Token[]) ArrayUtil.doubleLength(oStack);
    oStack[oPt] = op;
    ptid++;
  }

  private void putIf(char c) {
    if (++ifPt >= ifStack.length)
      ifStack = (char[]) ArrayUtil.doubleLength(ifStack);
    ifStack[ifPt] = c;
  }

  boolean addX(ScriptVariable x) {
    // the standard entry point
    putX(x);
    return wasX = true;
  }

  boolean addX(Object x) {
    // the standard entry point
    ScriptVariable v = ScriptVariable.getVariable(x);
    if (v == null)
      return false;
    putX(v);
    return wasX = true;
  }

  boolean addXNum(ScriptVariable x) throws ScriptException {
    // corrects for x -3 being x - 3
    // only when coming from expression() or parameterExpression()
    if (wasX)
      switch (x.tok) {
      case Token.integer:
        if (x.intValue < 0) {
          addOp(Token.tokenMinus);
          x = ScriptVariable.intVariable(-x.intValue);
        }
        break;
      case Token.decimal:
        float f = ((Float) x.value).floatValue();
        if (f < 0) {
          addOp(Token.tokenMinus);
          x = new ScriptVariable(Token.decimal, new Float(-f));
        }
        break;
      }
    putX(x);
    return wasX = true;
  }

  private boolean addX(boolean x) {
    putX(ScriptVariable.getVariable(x ? Boolean.TRUE
        : Boolean.FALSE));
    return wasX = true;
  }

  private boolean addX(int x) {
    // no check for unary minus
    putX(ScriptVariable.intVariable(x));
    return wasX = true;
  }

  private boolean addX(float x) {
    // no check for unary minus
    return addX(new Float(x));
  }

  private static boolean isOpFunc(Token op) {
    return (Token.tokAttr(op.tok, Token.mathfunc) || op.tok == Token.propselector
        && Token.tokAttr(op.intValue, Token.mathfunc));
  }

  private boolean skipping;

  /**
   * addOp The primary driver of the Reverse Polish Notation evaluation engine.
   * 
   * This method loads operators onto the oStack[] and processes them based on a
   * precedence system. Operands are added by addX() onto the xStack[].
   * 
   * We check here for syntax issues that were not caught in the compiler. I
   * suppose that should be done at compilation stage, but this is how it is for
   * now.
   * 
   * The processing of functional arguments and (___?___:___) constructs is
   * carried out by pushing markers onto the stacks that later can be used to
   * fill argument lists or turn "skipping" on or off. Note that in the case of
   * skipped sections of ( ? : ) no attempt is made to do syntax checking.
   * [That's not entirely true -- when syntaxChecking is true, that is, when the
   * user is typing at the Jmol application console, then this code is being
   * traversed with dummy variables. That could be improved, for sure.
   * 
   * Actually, there's plenty of room for improvement here. I did this based on
   * what I learned in High School in 1974 -- 35 years ago! -- when I managed to
   * build a mini FORTRAN compiler from scratch in machine code. That was fun.
   * (This was fun, too.)
   * 
   * -- Bob Hanson, hansonr@stolaf.edu 6/9/2009
   * 
   * 
   * @param op
   * @return false if an error condition arises
   * @throws ScriptException
   */
  boolean addOp(Token op) throws ScriptException {
    return addOp(op, true);
  }

  boolean addOp(Token op, boolean allowMathFunc) throws ScriptException {

    if (logMessages) {

      dumpStacks("addOp entry\naddOp: " + op + " oPt=" + oPt + " ifPt = "
          + ifPt + " skipping=" + skipping + " wasX=" + wasX);
    }

    // are we skipping due to a ( ? : ) construct?
    int tok0 = (oPt >= 0 ? oStack[oPt].tok : 0);
    skipping = (ifPt >= 0 && (ifStack[ifPt] == 'F' || ifStack[ifPt] == 'X'));
    if (skipping) {
      switch (op.tok) {
      case Token.leftparen:
        putOp(op);
        return true;
      case Token.colon:
        // dumpStacks("skipping -- :");
        if (tok0 != Token.colon || ifStack[ifPt] == 'X')
          return true; // ignore if not a clean opstack or T already processed
        // no object here because we were skipping
        // set to flag end of this parens
        ifStack[ifPt] = 'T';
        wasX = false;
        // dumpStacks("(..False...? .skip.. :<--here.... )");
        skipping = false;
        return true;
      case Token.rightparen:
        if (tok0 == Token.leftparen) {
          oPt--; // clear opstack
          return true;
        }
        // dumpStacks("skipping -- )");
        if (tok0 != Token.colon) {
          putOp(op);
          return true;
        }
        wasX = true;
        // and remove markers
        ifPt--;
        oPt -= 2;
        skipping = false;
        // dumpStacks("(..True...? ... : ...skip...)<--here ");
        return true;
      default:
        return true;
      }
    }

    // Do we have the appropriate context for this operator?

    Token newOp = null;
    int tok;
    boolean isLeftOp = false;
    boolean isDotSelector = (op.tok == Token.propselector);

    if (isDotSelector && !wasX)
      return false;

    boolean isMathFunc = (allowMathFunc && isOpFunc(op));

    // the word "plane" can also appear alone, not as a function
    if (oPt >= 1 && op.tok != Token.leftparen && tok0 == Token.plane)
      tok0 = oStack[--oPt].tok;

    // math functions as arguments appear without a prefixing operator
    boolean isArgument = (oPt >= 1 && tok0 == Token.leftparen);

    switch (op.tok) {
    case Token.comma:
      if (!wasX)
        return false;
      break;
    case Token.min:
    case Token.max:
    case Token.average:
    case Token.sum2:
    case Token.stddev:
    case Token.minmaxmask:
      tok = (oPt < 0 ? Token.nada : tok0);
      if (!wasX
          || !(tok == Token.propselector || tok == Token.bonds || tok == Token.atoms))
        return false;
      oStack[oPt].intValue |= op.tok;
      return true;
    case Token.leftsquare: // {....}[n][m]
      isLeftOp = true;
      if (!wasX) {
        squareCount++;
        op = newOp = Token.tokenArray;
      }
      break;
    case Token.rightsquare:
      break;
    case Token.minusMinus:
    case Token.plusPlus:
      incrementX = (op.tok == Token.plusPlus ? 1 : -1);
      if (ptid == ptx) {
        if (isSyntaxCheck)
          return true;
        ScriptVariable x = xStack[xPt];
        xStack[xPt] = (new ScriptVariable()).set(x);
        return x.increment(incrementX);
      }
      break;
    case Token.minus:
      if (wasX)
        break;
      addX(0);
      op = new ScriptVariable(Token.unaryMinus, "-");
      break;
    case Token.rightparen: // () without argument allowed only for math funcs
      if (!wasX && oPt >= 1 && tok0 == Token.leftparen
          && !isOpFunc(oStack[oPt - 1]))
        return false;
      break;
    case Token.opNot:
    case Token.leftparen:
      isLeftOp = true;
      // fall through
    default:
      if (isMathFunc) {
        if (!isDotSelector && wasX && !isArgument)
          return false;
        newOp = op;
        isLeftOp = true;
        break;
      }
      if (wasX == isLeftOp && tok0 != Token.propselector) // for now, because
        // we have .label
        // and .label()
        return false;
      break;
    }

    // do we need to operate?

    while (oPt >= 0
        && tok0 != Token.colon
        && (!isLeftOp || tok0 == Token.propselector
            && (op.tok == Token.propselector || op.tok == Token.leftsquare))
        && Token.getPrecedence(tok0) >= Token.getPrecedence(op.tok)) {

      if (logMessages) {
        Logger.info("\noperating, oPt=" + oPt + " isLeftOp=" + isLeftOp
            + " oStack[oPt]=" + Token.nameOf(tok0) + "        prec="
            + Token.getPrecedence(tok0) + " pending op=\""
            + Token.nameOf(op.tok) + "\" prec=" + Token.getPrecedence(op.tok));
        dumpStacks("operating");
      }
      // ) and ] must wait until matching ( or [ is found
      if (op.tok == Token.rightparen && tok0 == Token.leftparen) {
        // (x[2]) finalizes the selection
        if (xPt >= 0)
          xStack[xPt] = ScriptVariable.selectItem(xStack[xPt]);
        break;
      }
      if (op.tok == Token.rightsquare && tok0 == Token.array) {
        break;
      }
      if (op.tok == Token.rightsquare && tok0 == Token.leftsquare) {
        if (xPt == 0 && isArrayItem) {
          addX(new ScriptVariable(Token.tokenArraySelector));
          break;
        }
        if (!doBitsetSelect())
          return false;
        break;
      }

      // if not, it's time to operate

      if (!operate())
        return false;
      tok0 = (oPt >= 0 ? oStack[oPt].tok : 0);
    }

    // now add a marker on the xStack if necessary

    if (newOp != null)
      addX(new ScriptVariable(Token.opEQ, newOp));

    // fix up counts and operand flag
    // right ) and ] are not added to the stack

    switch (op.tok) {
    case Token.leftparen:
      // System.out.println("----------(----------");
      parenCount++;
      wasX = false;
      break;
    case Token.opIf:
      // System.out.println("---------IF---------");
      boolean isFirst = ScriptVariable.bValue(getX());
      if (tok0 == Token.colon)
        ifPt--;
      else 
        putOp(Token.tokenColon);
      putIf(isFirst ? 'T' : 'F');
      skipping = !isFirst;
      wasX = false;
      // dumpStacks("(.." + isFirst + "...?<--here ... :...skip...) ");
      return true;
    case Token.colon:
      // System.out.println("----------:----------");
      if (tok0 != Token.colon)
        return false;
      if (ifPt < 0)
        return false;
      ifStack[ifPt] = 'X';
      wasX = false;
      skipping = true;
      // dumpStacks("(..True...? ... :<--here ...skip...) ");
      return true;
    case Token.rightparen:
      // System.out.println("----------)----------");
      wasX = true;
      if (parenCount-- <= 0)
        return false;
      if (tok0 == Token.colon) {
        // remove markers
        ifPt--;
        oPt--;
        // dumpStacks("(..False...? ...skip... : ...)<--here ");
      }
      oPt--;
      if (oPt < 0)
        return true;
      if (isOpFunc(oStack[oPt]) && !evaluateFunction())
        return false;
      skipping = (ifPt >= 0 && ifStack[ifPt] == 'X');
      return true;
    case Token.comma:
      wasX = false;
      return true;
    case Token.leftsquare:
      squareCount++;
      wasX = false;
      break;
    case Token.rightsquare:
      wasX = true;
      if (squareCount-- <= 0)
        return false;
      if (oStack[oPt].tok == Token.array)
        return evaluateFunction();
      oPt--;
      return true;
    case Token.propselector:
      wasX = (!allowMathFunc || !Token.tokAttr(op.intValue, Token.mathfunc));
      break;
    case Token.leftbrace:
      braceCount++;
      wasX = false;
      break;
    case Token.rightbrace:
      if (braceCount-- <= 0)
        return false;
    default:
      wasX = false;
    }

    // add the operator if possible

    putOp(op);

    // dumpStacks("putOp complete");
    if (op.tok == Token.propselector
        && (op.intValue & ~Token.minmaxmask) == Token.function
        && op.intValue != Token.function) {
      return evaluateFunction();
    }
    return true;
  }

  private boolean doBitsetSelect() {
    if (xPt < 0 || xPt == 0 && !isArrayItem) {
      return false;
    }
    int i = ScriptVariable.iValue(xStack[xPt--]);
    ScriptVariable var = xStack[xPt];
    switch (var.tok) {
    default:
      var = new ScriptVariable(Token.string, ScriptVariable.sValue(var));
      // fall through
    case Token.bitset:
    case Token.list:
    case Token.string:
      xStack[xPt] = ScriptVariable.selectItem(var, i);
    }
    return true;
  }

  void dumpStacks(String message) {
    Logger.info("\n\nRPN stacks: " + message + "\n");
    for (int i = 0; i <= xPt; i++)
      Logger.info("x[" + i + "]: " + xStack[i]);
    Logger.info("\n");
    for (int i = 0; i <= oPt; i++)
      Logger.info("o[" + i + "]: " + oStack[i] + " prec="
          + Token.getPrecedence(oStack[i].tok));
    Logger.info(" ifStack = " + (new String(ifStack)).substring(0, ifPt + 1));
    System.out.flush();
  }

  private ScriptVariable getX() throws ScriptException {
    if (xPt < 0)
      eval.error(ScriptEvaluator.ERROR_endOfStatementUnexpected);
    ScriptVariable v = ScriptVariable.selectItem(xStack[xPt]);
    xStack[xPt--] = null;
    return v;
  }

  private boolean evaluateFunction() throws ScriptException {
    Token op = oStack[oPt--];
    // for .xxx or .xxx() functions
    // we store the token in the intValue field of the propselector token
    int tok = (op.tok == Token.propselector ? op.intValue & ~Token.minmaxmask
        : op.tok);

    int nParamMax = Token.getMaxMathParams(tok); // note - this is NINE for
    // dot-operators
    int nParam = 0;
    int pt = xPt;
    while (pt >= 0 && xStack[pt--].value != op)
      nParam++;
    if (nParamMax > 0 && nParam > nParamMax)
      return false;
    ScriptVariable[] args = new ScriptVariable[nParam];
    for (int i = nParam; --i >= 0;)
      args[i] = getX();
    xPt--;
    // no script checking of functions because
    // we cannot know what variables are real
    // if this is a property selector, as in x.func(), then we
    // just exit; otherwise we add a new TRUE to xStack
    if (isSyntaxCheck)
      return (op.tok == Token.propselector ? true : addX(true));
    switch (tok) {
    case Token.dot:
    case Token.distance:
      if (op.tok == Token.propselector)
        return evaluateDistance(args, tok);
      // fall through
    case Token.angle:
      return evaluateMeasure(args, op.tok == Token.angle);
    case Token.function:
      return evaluateUserFunction((String) op.value, args, op.intValue,
          op.tok == Token.propselector);
    case Token.find:
      return evaluateFind(args);
    case Token.replace:
      return evaluateReplace(args);
    case Token.array:
      return evaluateArray(args);
    case Token.abs:
    case Token.acos:
    case Token.cos:
    case Token.sin:
    case Token.sqrt:
    case Token.quaternion:
    case Token.axisangle:
      return evaluateMath(args, tok);
    case Token.cross:
      return evaluateCross(args);
    case Token.random:
      return evaluateRandom(args);
    case Token.split:
    case Token.join:
    case Token.trim:
      return evaluateString(op.intValue, args);
    case Token.add:
    case Token.sub:
    case Token.mul:
    case Token.div:
      return evaluateList(op.intValue, args);
    case Token.bin:
      return evaluateBin(args);
    case Token.helix:
      return evaluateHelix(args);
    case Token.label:
    case Token.format:
        return evaluateLabel(op.intValue, args);
    case Token.data:
      return evaluateData(args);
    case Token.load:
    case Token.file:
      return evaluateLoad(args, tok);
    case Token.write:
      return evaluateWrite(args);
    case Token.script:
    case Token.javascript:
      return evaluateScript(args, tok);
    case Token.within:
      return evaluateWithin(args);
    case Token.getproperty:
      return evaluateGetProperty(args);
    case Token.point:
      return evaluatePoint(args);
    case Token.plane:
      return evaluatePlane(args);
    case Token.connected:
      return evaluateConnected(args);
    case Token.substructure:
      return evaluateSubstructure(args);
    case Token.symop:
      return evaluateSymop(args);
    }
    return false;
  }

  private boolean evaluateSymop(ScriptVariable[] args) throws ScriptException {
    if (args.length == 0)
      return false;
    ScriptVariable x1 = getX();
    if (isSyntaxCheck)
      return addX(new Point3f());
    if (x1.tok != Token.bitset)
      return false;
    String xyz = (args[0].tok == Token.string ? ScriptVariable.sValue(args[0]) : null);
    int iOp = (xyz == null ? ScriptVariable.iValue(args[0]) : 0);
    Point3f pt = (args.length > 1 ? ptValue(args[1]) : null);
    if (args.length < 3)
      return addX((Point3f) viewer.getSymmetryInfo((BitSet) x1.value, xyz, iOp, pt, null, Token.point));
    return addX((String) viewer.getSymmetryInfo((BitSet) x1.value, xyz, iOp, pt, 
          ScriptVariable.sValue(args[2]), Token.draw));
  }

  private boolean evaluateBin(ScriptVariable[] args) throws ScriptException {
    if (args.length != 3)
      return false;
    ScriptVariable x1 = getX();
    boolean isListf = (x1.tok == Token.listf);
    if (isSyntaxCheck || !isListf && x1.tok != Token.list)
      return addX(x1);
    float f0 = ScriptVariable.fValue(args[0]);
    float f1 = ScriptVariable.fValue(args[1]);
    float df = ScriptVariable.fValue(args[2]);
    Float[] data = (isListf ? (Float[]) x1.value : null);
    String[] sdata = (isListf ? null : (String[]) x1.value);
    int nbins = (int) ((f1 - f0) / df + 0.01f);
    int[] array = new int[nbins];
    String[] sout = new String[nbins];
    int nPoints = (isListf ? data.length : sdata.length);
    for (int i = 0; i < nPoints; i++) {
      float v = (isListf ? data[i].floatValue() : Parser.parseFloat(sdata[i]));
      int bin = (int) ((v - f0) / df);
      if (bin < 0)
        bin = 0;
      else if (bin >= nbins)
        bin = nbins;
      array[bin]++;
    }
    for (int i = 0; i < nbins; i++)
      sout[i] = "" + array[i];
    return addX(sout);
  }

  private boolean evaluateHelix(ScriptVariable[] args) throws ScriptException {
    if (args.length < 1)
      return false;
    // helix({resno=3})
    // helix({resno=3},"point|axis|radius|angle|draw|measure|array")
    // helix(resno,"point|axis|radius|angle|draw|measure|array")
    // helix(pt1, pt2, dq, "point|axis|radius|angle|draw|measure|array|")
    // helix(pt1, pt2, dq, "draw","someID")
    // helix(pt1, pt2, dq)
    int pt = (args.length > 2 ? 3 : 1);
    String type = (pt >= args.length ? "array" : ScriptVariable
        .sValue(args[pt])).toLowerCase();
    Token t = Token.getTokenFromName(type);
    if (args.length > 2) {
      // helix(pt1, pt2, dq ...)
      Point3f pta = ptValue(args[0]);
      Point3f ptb = ptValue(args[1]);
      if (args[2].tok != Token.point4f)
        return false;
      Quaternion dq = new Quaternion((Point4f) args[2].value);
      switch (t == null ? Token.nada : t.tok) {
      case Token.nada:
        break;
      case Token.point:
      case Token.axis:
      case Token.radius:
      case Token.angle:
      case Token.monitor:
        return addX(Measure.computeHelicalAxis(null, t.tok, pta, ptb, dq));
      case Token.array:
        String[] data = (String[]) Measure.computeHelicalAxis(null, Token.list, pta, ptb, dq);
        if (data == null)
          return false;
        return addX(data);
      default:
        return addX(Measure.computeHelicalAxis(type, Token.draw, pta, ptb, dq));
      }
    } else {
      BitSet bs = (args[0].value instanceof BitSet ? (BitSet) args[0].value
          : eval.compareInt(Token.resno, null, Token.opEQ, ScriptVariable
              .iValue(args[0])));
      switch (t == null ? Token.nada : t.tok) {
      case Token.point:
        return addX(isSyntaxCheck ? new Point3f() : (Point3f) viewer
            .getHelixData(bs, Token.point));
      case Token.axis:
        return addX(isSyntaxCheck ? new Vector3f() : (Vector3f) viewer
            .getHelixData(bs, Token.axis));
      case Token.radius:
        return addX(isSyntaxCheck ? new Vector3f() : (Vector3f) viewer
            .getHelixData(bs, Token.radius));
      case Token.angle:
        return addX(isSyntaxCheck ? 0 : ((Float) viewer.getHelixData(bs,
            Token.angle)).floatValue());
      case Token.draw:
      case Token.monitor:
        return addX(isSyntaxCheck ? "" : (String) viewer.getHelixData(bs,
            t.tok));
      case Token.array:
        String[] data = (String[]) viewer.getHelixData(bs, Token.list);
        if (data == null)
          return false;
        return addX(data);
      }
    }
    return false;
  }

  private boolean evaluateDistance(ScriptVariable[] args, int tok)
      throws ScriptException {
    ScriptVariable x1 = getX();
    if (args.length != 1)
      return false;
    if (isSyntaxCheck)
      return addX(1f);
    ScriptVariable x2 = args[0];
    Point3f pt2 = ptValue(x2);
    Point4f plane2 = planeValue(x2);
    if (x1.tok == Token.bitset && tok != Token.dot)
      return addX(eval.getBitsetProperty(ScriptVariable.bsSelect(x1),
          Token.distance, pt2, plane2, x1.value, null, false, x1.index));
    Point3f pt1 = ptValue(x1);
    Point4f plane1 = planeValue(x1);
    if (tok == Token.dot) {
      if (plane1 != null && plane2 != null)
        // q1.dot(q2) assume quaternions
        return addX(plane1.x * plane2.x + plane1.y * plane2.y + plane1.z * plane2.z + plane1.w * plane2.w);
        // plane.dot(point) = 
      if (plane1 != null)
        pt1 = new Point3f(plane1.x, plane1.y, plane1.z);
      // point.dot(plane)
      if (plane2 != null)
        pt2 = new Point3f(plane2.x, plane2.y, plane2.z);
      return addX(pt1.x * pt2.x + pt1.y * pt2.y + pt2.z * pt2.z);
    }

    if (plane1 == null)
      return addX(plane2 == null ? pt2.distance(pt1) : Measure
          .distanceToPlane(plane2, pt1));
    return addX(Measure.distanceToPlane(plane1, pt2));
  }

  private Point3f ptValue(ScriptVariable x) throws ScriptException {
    if (isSyntaxCheck)
      return new Point3f();
    switch (x.tok) {
    case Token.point3f:
      return (Point3f) x.value;
    case Token.bitset:
      return (Point3f) eval.getBitsetProperty(ScriptVariable.bsSelect(x),
          Token.xyz, null, null, x.value, null, false, Integer.MAX_VALUE);
    case Token.string:
    case Token.list:
      Object pt = Escape.unescapePoint(ScriptVariable.sValue(x));
      if (pt instanceof Point3f)
        return (Point3f) pt;
      break;
    }
    float f = ScriptVariable.fValue(x);
    return new Point3f(f, f, f);
  }

  private Point4f planeValue(Token x) {
    if (isSyntaxCheck)
      return new Point4f();
    switch (x.tok) {
    case Token.point4f:
      return (Point4f) x.value;
    case Token.list:
    case Token.string:
      Object pt = Escape.unescapePoint(ScriptVariable.sValue(x));
      return (pt instanceof Point4f ? (Point4f) pt : null);
    case Token.bitset:
      // ooooh, wouldn't THIS be nice!
      break;
    }
    return null;
  }

  private boolean evaluateMeasure(ScriptVariable[] args, boolean isAngle)
      throws ScriptException {
    int nPoints = args.length;
    if (nPoints < (isAngle ? 3 : 2) || nPoints > (isAngle ? 4 : 2))
      return false;
    if (isSyntaxCheck)
      return addX(1f);

    Point3f[] pts = new Point3f[nPoints];
    for (int i = 0; i < nPoints; i++)
      pts[i] = ptValue(args[i]);
    switch (nPoints) {
    case 2:
      return addX(pts[0].distance(pts[1]));
    case 3:
      return addX(Measure.computeAngle(pts[0], pts[1], pts[2], true));
    case 4:
      return addX(Measure.computeTorsion(pts[0], pts[1], pts[2], pts[3], true));
    }
    return false;
  }

  private boolean evaluateUserFunction(String name, ScriptVariable[] args,
                                       int tok, boolean isSelector)
      throws ScriptException {
    ScriptVariable x1 = null;
    if (isSelector) {
      x1 = getX();
      if (x1.tok != Token.bitset)
        return false;
    }
    wasX = false;
    if (isSyntaxCheck)
      return addX((int) 1);
    Vector params = new Vector();
    for (int i = 0; i < args.length; i++)
      params.addElement(args[i]);
    if (isSelector) {
      return addX(eval.getBitsetProperty(ScriptVariable.bsSelect(x1), tok,
          null, null, x1.value, new Object[] { name, params }, false, x1.index));
    }
    ScriptVariable var = eval.getFunctionReturn(name, params, null);
    return (var == null ? false : addX(var));
  }

  private boolean evaluateFind(ScriptVariable[] args) throws ScriptException {
    if (args.length != 1 && args.length != 2)
      return false;
    if (isSyntaxCheck)
      return addX((int) 1);
    ScriptVariable x1 = getX();
    String sFind = ScriptVariable.sValue(args[0]);
    boolean isPattern = (args.length == 2);
    String flags = (isPattern ? ScriptVariable.sValue(args[1]) : "");
    boolean isReverse = (flags.indexOf("v") >= 0);
    boolean isCaseInsensitive = (flags.indexOf("i") >= 0);
    boolean asMatch = (flags.indexOf("m") >= 0);
    boolean isList = (x1.tok == Token.list);
    if (isList || isPattern) {
      Pattern pattern = null;
      try {
        pattern = Pattern.compile(sFind,
            isCaseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
      } catch (Exception e) {
        eval.evalError(e.getMessage(), null);
      }
      String[] list = (isList ? (String[]) x1.value
          : new String[] { ScriptVariable.sValue(x1) });
      if (Logger.debugging)
        Logger.debug("finding " + sFind);
      BitSet bs = new BitSet();
      int ipt = 0;
      int n = 0;
      Matcher matcher = null;
      Vector v = (asMatch ? new Vector() : null);
      for (int i = 0; i < list.length; i++) {
        String what = list[i];
        matcher = pattern.matcher(what);
        boolean isMatch = matcher.find();
        if (asMatch && isMatch || !asMatch && isMatch == !isReverse) {
          n++;
          ipt = i;
          bs.set(i);
          if (asMatch)
            v.add(isReverse ? what.substring(0, matcher.start())
                + what.substring(matcher.end()) : matcher.group());
        }
      }
      if (!isList) {
        return (asMatch ? addX(v.size() == 1 ? (String) v.get(0) : "")
            : isReverse ? addX(n == 1) : asMatch ? addX(n == 0 ? "" : matcher
                .group()) : addX(n == 0 ? 0 : matcher.start() + 1));
      }
      if (n == 1)
        return addX(asMatch ? (String) v.get(0) : list[ipt]);
      String[] listNew = new String[n];
      if (n > 0)
        for (int i = list.length; --i >= 0;)
          if (bs.get(i)) {
            --n;
            listNew[n] = (asMatch ? (String) v.get(n) : list[i]);
          }
      return addX(listNew);
    }
    return addX(ScriptVariable.sValue(x1).indexOf(sFind) + 1);
  }

  private boolean evaluateGetProperty(ScriptVariable[] args) {
    if (isSyntaxCheck)
      return addX("");
    int pt = 0;
    String propertyName = (args.length > pt ? ScriptVariable.sValue(args[pt++])
        .toLowerCase() : "");
    Object propertyValue;
    if (propertyName.equalsIgnoreCase("fileContents") && args.length > 2) {
      String s = ScriptVariable.sValue(args[1]);
      for (int i = 2; i < args.length; i++)
        s += "|" + ScriptVariable.sValue(args[i]);
      propertyValue = s;
      pt = args.length;
    } else {
      propertyValue = (args.length > pt && args[pt].tok == Token.bitset ? (Object) ScriptVariable
          .bsSelect(args[pt++])
          : args.length > pt && args[pt].tok == Token.string
              && PropertyManager.acceptsStringParameter(propertyName) ? args[pt++].value
              : (Object) "");
    }
    if (args.length == pt && propertyName.indexOf(".") >= 0
        || propertyName.indexOf("[") >= 0) {
      propertyName = propertyName.replace(']', ' ').replace('[', ' ').replace(
          '.', ' ');
      propertyName = TextFormat.simpleReplace(propertyName, "  ", " ");
      String[] names = TextFormat
          .split(TextFormat.trim(propertyName, " "), " ");
      if (names.length > 0) {
        args = new ScriptVariable[names.length];
        propertyName = names[0];
        int n;
        for (int i = 1; i < names.length; i++) {
          if ((n = Parser.parseInt(names[i])) != Integer.MIN_VALUE)
            args[i] = new ScriptVariable(Token.integer, n);
          else
            args[i] = new ScriptVariable(Token.string, names[i]);
        }
        pt = 1;
      }
    }
    Object property = viewer.getProperty(null, propertyName, propertyValue);
    if (pt < args.length)
      property = PropertyManager.extractProperty(property, args, pt);
    if (property instanceof String)
      return addX(property);
    if (property instanceof Integer)
      return addX(property);
    if (property instanceof Float)
      return addX(property);
    if (property instanceof Point3f)
      return addX(property);
    if (property instanceof Vector3f)
      return addX(new Point3f((Vector3f) property));
    if (property instanceof Vector) {
      Vector v = (Vector) property;
      int len = v.size();
      String[] list = new String[len];
      for (int i = 0; i < len; i++) {
        Object o = v.elementAt(i);
        if (o instanceof String)
          list[i] = (String) o;
        else
          list[i] = Escape.toReadable(o);
      }
      return addX(list);
    }
    return addX(Escape.toReadable(property));
  }

  private boolean evaluatePoint(ScriptVariable[] args) {
    if (args.length != 1 && args.length != 3 && args.length != 4)
      return false;
    if (isSyntaxCheck) {
      return addX(args.length == 4 ? (Object) new Point4f()
          : (Object) new Point3f());
    }

    switch (args.length) {
    case 1:
      Object pt = Escape.unescapePoint(ScriptVariable.sValue(args[0]));
      if (pt instanceof Point3f)
        return addX((Point3f) pt);
      return addX("" + pt);
    case 3:
      return addX(new Point3f(ScriptVariable.fValue(args[0]), ScriptVariable
          .fValue(args[1]), ScriptVariable.fValue(args[2])));
    case 4:
      return addX(new Point4f(ScriptVariable.fValue(args[0]), ScriptVariable
          .fValue(args[1]), ScriptVariable.fValue(args[2]), ScriptVariable
          .fValue(args[3])));
    }
    return false;
  }

  private boolean evaluatePlane(ScriptVariable[] args) throws ScriptException {
    if (args.length != 1 && args.length != 3 && args.length != 4)
      return false;
    if (isSyntaxCheck)
      return addX(new Point4f(0, 0, 1, 0));

    switch (args.length) {
    case 1:
      Object pt = Escape.unescapePoint(ScriptVariable.sValue(args[0]));
      if (pt instanceof Point4f)
        return addX((Point4f) pt);
      return addX("" + pt);
    case 3:
    case 4:
      switch (args[0].tok) {
      case Token.bitset:
      case Token.point3f:
        Point3f pt1 = ptValue(args[0]);
        Point3f pt2 = ptValue(args[1]);
        Point3f pt3 = ptValue(args[2]);
        Vector3f vAB = new Vector3f();
        Vector3f vAC = new Vector3f();
        Vector3f norm = new Vector3f();
        float nd = Measure.getDirectedNormalThroughPoints(pt1, pt2, pt3,
            (args.length == 4 ? ptValue(args[3]) : null), norm, vAB, vAC);
        return addX(new Point4f(norm.x, norm.y, norm.z, nd));
      default:
        if (args.length != 4)
          return false;
        float x = ScriptVariable.fValue(args[0]);
        float y = ScriptVariable.fValue(args[1]);
        float z = ScriptVariable.fValue(args[2]);
        float w = ScriptVariable.fValue(args[3]);
        return addX(new Point4f(x, y, z, w));
      }
    }
    return false;
  }

  private boolean evaluateReplace(ScriptVariable[] args) throws ScriptException {
    if (args.length != 2)
      return false;
    ScriptVariable x = getX();
    if (isSyntaxCheck)
      return addX("");
    String sFind = ScriptVariable.sValue(args[0]);
    String sReplace = ScriptVariable.sValue(args[1]);
    String s = (x.tok == Token.list ? null : ScriptVariable.sValue(x));
    if (s != null)
      return addX(TextFormat.simpleReplace(s, sFind, sReplace));
    String[] list = (String[]) x.value;
    for (int i = list.length; --i >= 0;)
      list[i] = TextFormat.simpleReplace(list[i], sFind, sReplace);
    return addX(list);
  }

  private boolean evaluateString(int tok, ScriptVariable[] args)
      throws ScriptException {
    if (args.length > 1)
      return false;
    ScriptVariable x = getX();
    if (isSyntaxCheck)
      return addX(ScriptVariable.sValue(x));
    String s = (tok == Token.split && x.tok == Token.bitset
        || tok == Token.trim && x.tok == Token.list ? null : ScriptVariable
        .sValue(x));
    String sArg = (args.length == 1 ? ScriptVariable.sValue(args[0])
        : tok == Token.trim ? "" : "\n");
    switch (tok) {
    case Token.split:
      if (x.tok == Token.bitset) {
        BitSet bsSelected = ScriptVariable.bsSelect(x);
        sArg = "\n";
        int modelCount = viewer.getModelCount();
        s = "";
        for (int i = 0; i < modelCount; i++) {
          s += (i == 0 ? "" : "\n");
          BitSet bs = viewer.getModelAtomBitSet(i, true);
          bs.and(bsSelected);
          s += Escape.escape(bs);
        }
      }
      return addX(TextFormat.split(s, sArg));
    case Token.join:
      if (s.length() > 0 && s.charAt(s.length() - 1) == '\n')
        s = s.substring(0, s.length() - 1);
      return addX(TextFormat.simpleReplace(s, "\n", sArg));
    case Token.trim:
      if (s != null)
        return addX(TextFormat.trim(s, sArg));
      String[] list = (String[]) x.value;
      for (int i = list.length; --i >= 0;)
        list[i] = TextFormat.trim(list[i], sArg);
      return addX(list);
    }
    return addX("");
  }

  private boolean evaluateList(int tok, ScriptVariable[] args)
      throws ScriptException {
    if (args.length != 1
        && !(tok == Token.add && (args.length == 0 || args.length == 2)))
      return false;
    ScriptVariable x1 = getX();
    ScriptVariable x2;
    int len;
    String[] sList1, sList2, sList3;

    if (args.length == 2) {
      // [xxxx].add("\t", [...])
      int itab = (args[0].tok == Token.string ? 0 : 1);
      String tab = ScriptVariable.sValue(args[itab]);
      sList1 = (x1.tok == Token.list ? (String[]) x1.value : TextFormat.split(
          ScriptVariable.sValue(x1), '\n'));
      x2 = args[1 - itab];
      sList2 = (x2.tok == Token.list ? (String[]) x2.value : TextFormat.split(
          ScriptVariable.sValue(x2), '\n'));
      sList3 = new String[len = Math.max(sList1.length, sList2.length)];
      for (int i = 0; i < len; i++)
        sList3[i] = (i >= sList1.length ? "" : sList1[i]) + tab
            + (i >= sList2.length ? "" : sList2[i]);
      return addX(sList3);
    }
    x2 = (args.length == 0 ? ScriptVariable.vAll : args[0]);
    boolean isAll = (x2.tok == Token.all);
    if (x1.tok != Token.list && x1.tok != Token.string) {
      wasX = false;
      addOp(Token.tokenLeftParen);
      addX(x1);
      switch (tok) {
      case Token.add:
        addOp(Token.tokenPlus);
        break;
      case Token.sub:
        addOp(Token.tokenMinus);
        break;
      case Token.mul:
        addOp(Token.tokenTimes);
        break;
      case Token.div:
        addOp(Token.tokenDivide);
        break;
      }
      addX(x2);
      return addOp(Token.tokenRightParen);
    }
    if (isSyntaxCheck)
      return addX("");

    boolean isScalar = (x2.tok != Token.list && ScriptVariable.sValue(x2)
        .indexOf("\n") < 0);

    String sValue = (isScalar ? ScriptVariable.sValue(x2) : "");

    float factor = (sValue.indexOf("{") >= 0 ? Float.NaN
        : isScalar ? ScriptVariable.fValue(x2) : 0);

    sList1 = (x1.value instanceof String ? TextFormat.split((String) x1.value,
        "\n") : (String[]) x1.value);

    sList2 = (isScalar ? null : x2.value instanceof String ? TextFormat.split(
        (String) x2.value, "\n") : (String[]) x2.value);

    len = (isScalar ? sList1.length : Math.min(sList1.length, sList2.length));

    float[] list1 = new float[sList1.length];
    Parser.parseFloatArray(sList1, list1);

    if (isAll) {
      float sum = 0f;
      for (int i = len; --i >= 0;)
        sum += list1[i];
      return addX(sum);
    }

    sList3 = new String[len];

    float[] list2 = new float[(isScalar ? sList1.length : sList2.length)];
    if (isScalar)
      for (int i = len; --i >= 0;)
        list2[i] = factor;
    else
      Parser.parseFloatArray(sList2, list2);

    Token token = null;
    switch (tok) {
    case Token.add:
      token = Token.tokenPlus;
      break;
    case Token.sub:
      token = Token.tokenMinus;
      break;
    case Token.mul:
      token = Token.tokenTimes;
      break;
    case Token.div:
      token = Token.tokenDivide;
      break;
    }

    for (int i = 0; i < len; i++) {
      if (Float.isNaN(list1[i]))
        addX(ScriptVariable.unescapePointOrBitsetAsVariable(sList1[i]));
      else
        addX(list1[i]);
      if (!Float.isNaN(list2[i]))
        addX(list2[i]);
      else if (isScalar)
        addX(ScriptVariable.unescapePointOrBitsetAsVariable(sValue));
      else
        addX(ScriptVariable.unescapePointOrBitsetAsVariable(sList2[i]));
      if (!addOp(token) || !operate())
        return false;
      sList3[i] = ScriptVariable.sValue(xStack[xPt--]);
    }
    return addX(sList3);
  }

  private boolean evaluateArray(ScriptVariable[] args) {
    if (isSyntaxCheck)
      return addX("");
    int len = args.length;
    String[] array = new String[len];
    for (int i = 0; i < args.length; i++)
      array[i] = ScriptVariable.sValue(args[i]);
    return addX(array);
  }

  private boolean evaluateMath(ScriptVariable[] args, int tok) {
    if (tok == Token.quaternion || tok == Token.axisangle) {
      // quaternion(vector, theta)
      // quaternion(q0, q1, q2, q3)
      // quaternion("{x, y, z, w"})
      // quaternion(center, X, XY)
      // quaternion(mcol1, mcol2)
      // quaternion(q, "id", center) // draw code
      // axisangle(vector, theta)
      // axisangle(x, y, z, theta)
      // axisangle("{x, y, z, theta"})
      switch (args.length) {
      case 1:
      case 4:
        break;
      case 2:
        if (args[0].tok != Token.point3f || tok != Token.quaternion
            && args[1].tok == Token.point3f)
          return false;
        break;
      case 3:
        if (tok != Token.quaternion)
          return false;
        if (args[0].tok == Token.point4f) {
          if (args[2].tok != Token.point3f && args[2].tok != Token.bitset)
            return false;
          break;
        }
        for (int i = 0; i < 3; i++)
          if (args[i].tok != Token.point3f && args[i].tok != Token.bitset)
            return false;
        break;
      default:
        return false;
      }
      if (isSyntaxCheck)
        return addX(new Point4f(0, 0, 0, 1));
      Quaternion q = null;
      Point4f p4 = null;
      switch (args.length) {
      case 2:
        if (args[1].tok == Token.point3f)
          q = Quaternion.getQuaternionFrame(new Point3f(0, 0, 0),
              (Point3f) args[0].value, (Point3f) args[1].value);
        else
          q = new Quaternion((Point3f) args[0].value, ScriptVariable
              .fValue(args[1]));
        break;
      case 3:
        if (args[0].tok == Token.point4f) {
          Point3f pt = (args[2].tok == Token.point3f ? (Point3f) args[2].value
              : viewer.getAtomSetCenter((BitSet) args[2].value));
          return addX((new Quaternion((Point4f) args[0].value)).draw("q",
              ScriptVariable.sValue(args[1]), pt, 1f));
        }
        Point3f[] pts = new Point3f[3];
        for (int i = 0; i < 3; i++)
          pts[i] = (args[i].tok == Token.point3f ? (Point3f) args[i].value
              : viewer.getAtomSetCenter((BitSet) args[i].value));
        q = Quaternion.getQuaternionFrame(pts[0], pts[1], pts[2]);
        break;
      case 4:
        if (tok == Token.quaternion)
          p4 = new Point4f(ScriptVariable.fValue(args[1]), ScriptVariable
              .fValue(args[2]), ScriptVariable.fValue(args[3]), ScriptVariable
              .fValue(args[0]));
        else
          q = new Quaternion(new Point3f(ScriptVariable.fValue(args[0]),
              ScriptVariable.fValue(args[1]), ScriptVariable.fValue(args[2])),
              ScriptVariable.fValue(args[3]));
        break;
      default:
        if (args[0].tok == Token.point4f) {
          p4 = (Point4f) args[0].value;
        } else if (args[0].tok == Token.bitset && tok == Token.quaternion) {
          q = ScriptEvaluator.getAtomQuaternion(viewer, (BitSet) args[0].value);
          if (q == null)
            return addX((int) 0);
        } else {
          Object v = Escape.unescapePoint(ScriptVariable.sValue(args[0]));
          if (!(v instanceof Point4f))
            return false;
          p4 = (Point4f) v;
        }
        if (tok == Token.axisangle)
          q = new Quaternion(new Point3f(p4.x, p4.y, p4.z), p4.w);
      }
      if (q == null)
        q = new Quaternion(p4);
      return addX(q.toPoint4f());
    }
    if (args.length != 1)
      return false;
    if (isSyntaxCheck)
      return addX(1);
    if (tok == Token.abs) {
      if (args[0].tok == Token.integer) 
        return addX(Math.abs(ScriptVariable.iValue(args[0])));
      return addX(Math.abs(ScriptVariable.fValue(args[0])));
    }
    double x = ScriptVariable.fValue(args[0]);
    switch (tok) {
    case Token.acos:
      return addX((float) (Math.acos(x) * 180 / Math.PI));
    case Token.cos:
      return addX((float) Math.cos(x * Math.PI / 180));
    case Token.sin:
      return addX((float) Math.sin(x * Math.PI / 180));
    case Token.sqrt:
      return addX((float) Math.sqrt(x));
    }
    return false;
  }

  private boolean evaluateRandom(ScriptVariable[] args) {
    if (args.length > 2)
      return false;
    if (isSyntaxCheck)
      return addX(1);
    float lower = (args.length < 2 ? 0 : ScriptVariable.fValue(args[0]));
    float range = (args.length == 0 ? 1 : ScriptVariable
        .fValue(args[args.length - 1]));
    range -= lower;
    return addX((float) (Math.random() * range) + lower);
  }

  private boolean evaluateCross(ScriptVariable[] args) {
    if (args.length != 2)
      return false;
    ScriptVariable x1 = args[0];
    ScriptVariable x2 = args[1];
    if (x1.tok != Token.point3f || x2.tok != Token.point3f)
      return false;
    if (isSyntaxCheck)
      return addX(new Point3f());
    Vector3f a = new Vector3f((Point3f) x1.value);
    Vector3f b = new Vector3f((Point3f) x2.value);
    a.cross(a, b);
    return addX(new Point3f(a));
  }

  private boolean evaluateLoad(ScriptVariable[] args, int tok) {
    if (args.length > 2 || args.length < 1)
      return false;
    if (isSyntaxCheck)
      return addX("");
    String file = ScriptVariable.sValue(args[0]);
    int nBytesMax = (args.length == 2 ? ScriptVariable.iValue(args[1]) : Integer.MAX_VALUE);
    return addX(tok == Token.load ? viewer.getFileAsString(file, nBytesMax, false) : viewer
        .getFullPath(file));
  }

  private boolean evaluateWrite(ScriptVariable[] args) throws ScriptException {
    if (args.length == 0)
      return false;
    if (isSyntaxCheck)
      return addX("");
    return addX(eval.write(args));
  }

  private boolean evaluateScript(ScriptVariable[] args, int tok)
      throws ScriptException {
    if (tok == Token.javascript && args.length != 1 || args.length == 0
        || args.length > 2)
      return false;
    if (isSyntaxCheck)
      return addX("");
    String s = ScriptVariable.sValue(args[0]);
    StringBuffer sb = new StringBuffer();
    switch (tok) {
    case Token.script:
      String appID = (args.length == 2 ? ScriptVariable.sValue(args[1]) : ".");
      // options include * > . or an appletID with or without "jmolApplet"
      if (!appID.equals("."))
        sb.append(viewer.jsEval(appID + "\1" + s));
      if (appID.equals(".") || appID.equals("*"))
        eval.runScript(s, sb);
      break;
    case Token.javascript:
      sb.append(viewer.jsEval(s));
      break;
    }
    s = sb.toString();
    float f;
    return (Float.isNaN(f = Parser.parseFloatStrict(s)) ? addX(s) : s
        .indexOf(".") >= 0 ? addX(f) : addX(Parser.parseInt(s)));
  }

  private boolean evaluateData(ScriptVariable[] args) {

    // x = data("somedataname") # the data
    // x = data("data2d_xxxx") # 2D data (x,y paired values)
    // x = data("data2d_xxxx", iSelected) # selected row of 2D data, with <=0
    // meaning "relative to the last row"
    // x = data("property_x", "property_y") # array addition of two property
    // sets
    // x = data({atomno < 10},"xyz") # (or "pdb" or "mol") coordinate data in
    // xyz, pdb, or mol format
    // x = data(someData,ptrFieldOrColumn,nBytes,firstLine) # extraction of a
    // column of data based on a field (nBytes = 0) or column range (nBytes >
    // 0)
    if (args.length != 1 && args.length != 2 && args.length != 4)
      return false;
    if (isSyntaxCheck)
      return addX("");
    String selected = ScriptVariable.sValue(args[0]);
    String type = (args.length == 2 ? ScriptVariable.sValue(args[1]) : "");

    if (args.length == 4) {
      int iField = ScriptVariable.iValue(args[1]);
      int nBytes = ScriptVariable.iValue(args[2]);
      int firstLine = ScriptVariable.iValue(args[3]);
      float[] f = Parser.extractData(selected, iField, nBytes, firstLine);
      return addX(Escape.escape(f, false));
    }

    if (selected.indexOf("data2d_") == 0) {
      // tab, newline separated data
      float[][] f1 = viewer.getDataFloat2D(selected);
      if (f1 == null)
        return addX("");
      if (args.length == 2 && args[1].tok == Token.integer) {
        int pt = args[1].intValue;
        if (pt < 0)
          pt += f1.length;
        if (pt >= 0 && pt < f1.length)
          return addX(Escape.escape(f1[pt], false));
        return addX("");
      }
      return addX(Escape.escape(f1, false));
    }

    // parallel addition of float property data sets

    if (selected.indexOf("property_") == 0) {
      float[] f1 = viewer.getDataFloat(selected);
      if (f1 == null)
        return addX("");
      float[] f2 = (type.indexOf("property_") == 0 ? viewer.getDataFloat(type)
          : null);
      if (f2 != null) {
        f1 = (float[]) f1.clone();
        for (int i = Math.min(f1.length, f2.length); --i >= 0;)
          f1[i] += f2[i];
      }
      return addX(Escape.escape(f1, false));
    }

    // some other data type -- just return it

    if (args.length == 1) {
      Object[] data = viewer.getData(selected);
      return addX(data == null ? "" : "" + data[1]);
    }
    // {selected atoms} XYZ, MOL, PDB file format
    return addX(viewer.getData(selected, type));
  }

  private boolean evaluateLabel(int intValue, ScriptVariable[] args)
      throws ScriptException {
    // NOT {xxx}.label
    // {xxx}.label("....")
    // {xxx}.yyy.format("...")
    // (value).format("...")
    // format("....",a,b,c...)
    
    ScriptVariable x1 = (args.length < 2 ? getX() : null);
    if (isSyntaxCheck)
      return addX("");
    String format = (args.length == 0 ? "%U" : ScriptVariable.sValue(args[0]));
    boolean asArray = Token.tokAttr(intValue, Token.minmaxmask);
    if (x1 == null) 
      return addX(ScriptVariable.sprintf(args));
    if (x1.tok == Token.bitset)
      return addX(eval.getBitsetIdent(ScriptVariable.bsSelect(x1), format,
          x1.value, true, x1.index, asArray));
    return addX(ScriptVariable.sprintf(TextFormat.formatCheck(format), x1));
  }

  private boolean evaluateWithin(ScriptVariable[] args) {
    if (args.length < 1)
      return false;
    int i = args.length;
    Object withinSpec = args[0].value;
    int tok = args[0].tok;
    String withinStr = "" + withinSpec;
    BitSet bs = new BitSet();
    float distance = 0;
    boolean isSequence = false;
    boolean isWithinModelSet = false;
    boolean isDistance = (tok == Token.decimal || tok == Token.integer);
    if (withinStr.equals("branch")) {
      if (i != 3 || !(args[1].value instanceof BitSet)
          || !(args[2].value instanceof BitSet))
        return false;
      return addX(viewer.getBranchBitSet(BitSetUtil
          .firstSetBit((BitSet) args[2].value), BitSetUtil
          .firstSetBit((BitSet) args[1].value)));
    }
    if (withinSpec instanceof String) {
      isSequence = !Parser
          .isOneOf(
              withinStr.toLowerCase(),
              "helix;sheet;atomname;atomtype;element;site;group;chain;structure;molecule;model;boundbox");
    } else if (isDistance) {
      distance = ScriptVariable.fValue(args[0]);
      if (i < 2)
        return false;
      if (args[1].tok == Token.on || args[1].tok == Token.off) {
        isWithinModelSet = ScriptVariable.bValue(args[1]);
        i = 0;
      }
    } else {
      return false;
    }
    switch (i) {
    case 1:
      // within (boundbox)
      boolean isHelix = withinStr.equalsIgnoreCase("helix"); 
      if (isHelix || withinStr.equalsIgnoreCase("sheet"))
        return addX(isSyntaxCheck ? bs : viewer.getAtomBits(isHelix ? Token.helix : Token.sheet, null));
      return (!withinStr.equalsIgnoreCase("boundbox") ? false
          : addX(isSyntaxCheck ? bs : viewer.getAtomBits(Token.boundbox, null)));
    case 2:
      // within (atomName, "XX,YY,ZZZ")
      if (withinStr.equalsIgnoreCase("atomName"))
        return addX(isSyntaxCheck ? bs : viewer.getAtomBits(Token.atomName,
            ScriptVariable.sValue(args[1])));
      // within (atomType, "XX,YY,ZZZ")
      if (withinStr.equalsIgnoreCase("atomType"))
        return addX(isSyntaxCheck ? bs : viewer.getAtomBits(Token.atomType,
            ScriptVariable.sValue(args[1])));
      break;
    case 3:
      withinStr = ScriptVariable.sValue(args[1]);
      if (!Parser.isOneOf(withinStr.toLowerCase(), "on;off;plane;hkl;coord"))
        return false;
      // within (distance, true|false, {atom collection})
      // within (distance, plane|hkl, [plane definition] )
      // within (distance, coord, [point or atom center] )
      break;
    }
    Point3f pt = null;
    Point4f plane = null;
    i = args.length - 1;
    if (args[i].value instanceof Point4f)
      plane = (Point4f) args[i].value;
    else if (args[i].value instanceof Point3f)
      pt = (Point3f) args[i].value;

    if (i > 0 && plane == null && pt == null
        && !(args[i].value instanceof BitSet))
      return false;
    if (isSyntaxCheck)
      return addX(bs);
    if (plane != null)
      return addX(viewer.getAtomsWithin(distance, plane));
    if (pt != null)
      return addX(viewer.getAtomsWithin(distance, pt));
    bs = ScriptVariable.bsSelect(args[i]);
    if (isDistance)
      return addX(viewer.getAtomsWithin(distance, bs, isWithinModelSet));
    if (isSequence)
      return addX(viewer.getSequenceBits(withinStr, bs));
    return addX(viewer.getAtomBits(Token.getTokenFromName(withinStr).tok, bs));
  }

  private boolean evaluateConnected(ScriptVariable[] args) {
    /*
     * Two options here:
     * 
     * connected(1, 3, "single", {carbon})
     * 
     * connected(1, 3, "partial 3.1", {carbon})
     * 
     * means "atoms connected to carbon by from 1 to 3 single bonds"
     * 
     * connected(1.0, 1.5, "single", {carbon}, {oxygen})
     * 
     * means "single bonds from 1.0 to 1.5 Angstroms between carbon and oxygen"
     * 
     * the first returns an atom bitset; the second returns a bond bitset.
     */
    float min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
    float fmin = 0, fmax = Float.MAX_VALUE;

    short order = JmolConstants.BOND_ORDER_ANY;
    BitSet atoms1 = null;
    BitSet atoms2 = null;
    boolean haveDecimal = false;
    boolean isBonds = false;
    for (int i = 0; i < args.length; i++) {
      ScriptVariable var = args[i];
      switch (var.tok) {
      case Token.bitset:
        isBonds = (var.value instanceof BondSet);
        if (isBonds && atoms1 != null)
          return false;
        if (atoms1 == null)
          atoms1 = ScriptVariable.bsSelect(var);
        else if (atoms2 == null)
          atoms2 = ScriptVariable.bsSelect(var);
        else
          return false;
        break;
      case Token.string:
        String type = ScriptVariable.sValue(var);
        if (type.equalsIgnoreCase("hbond"))
          order = JmolConstants.BOND_HYDROGEN_MASK;
        else
          order = JmolConstants.getBondOrderFromString(type);
        if (order == JmolConstants.BOND_ORDER_NULL)
          return false;
        break;
      case Token.decimal:
        haveDecimal = true;
        // fall through
      default:
        int n = ScriptVariable.iValue(var);
        float f = ScriptVariable.fValue(var);
        if (max != Integer.MAX_VALUE)
          return false;

        if (min == Integer.MIN_VALUE) {
          min = Math.max(n, 1);
          fmin = f;
        } else {
          max = n;
          fmax = f;
        }
      }
    }
    if (min == Integer.MIN_VALUE) {
      min = 1;
      max = 100;
      fmin = JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE;
      fmax = JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE;
    } else if (max == Integer.MAX_VALUE) {
      max = min;
      fmax = fmin;
      fmin = JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (atoms1 == null)
      atoms1 = viewer.getModelAtomBitSet(-1, true);
    if (haveDecimal && atoms2 == null)
      atoms2 = atoms1;
    if (atoms2 != null) {
      BitSet bsBonds = new BitSet();
      if (isSyntaxCheck)
        return addX(new ScriptVariable(Token.bitset, new BondSet(bsBonds)));
      viewer
          .makeConnections(fmin, fmax, order,
              JmolConstants.CONNECT_IDENTIFY_ONLY, atoms1, atoms2, bsBonds,
              isBonds);
      return addX(new ScriptVariable(Token.bitset, new BondSet(bsBonds, viewer
          .getAtomIndices(viewer.getAtomBits(Token.bonds, bsBonds)))));
    }
    if (isSyntaxCheck)
      return addX(atoms1);
    return addX(viewer.getAtomsConnected(min, max, order, atoms1));
  }

  private boolean evaluateSubstructure(ScriptVariable[] args)
      throws ScriptException {
    if (args.length != 1)
      return false;
    BitSet bs = new BitSet();
    String smiles = (isSyntaxCheck ? "" : ScriptVariable.sValue(args[0]));
    if (smiles.length() > 0)
      try {
        bs = viewer.getSmilesMatcher().getSubstructureSet(smiles);
      } catch (Exception e) {
        eval.evalError(e.getMessage(), null);
      }
    return addX(bs);
  }

  private boolean operate() throws ScriptException {

    Token op = oStack[oPt--];

    if (logMessages) {
      dumpStacks("operate: " + op);
    }

    if (oPt < 0 && op.tok == Token.opEQ && isArrayItem) {
      return (xPt == 2);
    }

    ScriptVariable x2 = getX();
    if (x2 == Token.tokenArraySelector)
      return false;

    // unary:

    if (x2.tok == Token.list)
      x2 = ScriptVariable.selectItem(x2);

    if (op.tok == Token.minusMinus || op.tok == Token.plusPlus) {
      if (!isSyntaxCheck && !x2.increment(incrementX))
        return false;
      wasX = true;
      putX(x2); // reverse getX()
      return true;
    }

    if (op.tok == Token.opNot)
      return (isSyntaxCheck ? addX(true) : x2.tok == Token.point4f ? // quaternion
      addX((new Quaternion((Point4f) x2.value)).inv().toPoint4f())
          : x2.tok == Token.bitset ? addX(BitSetUtil.copyInvert(ScriptVariable
              .bsSelect(x2), (x2.value instanceof BondSet ? viewer
              .getBondCount() : viewer.getAtomCount()))) : addX(!ScriptVariable
              .bValue(x2)));
    int iv = op.intValue & ~Token.minmaxmask;
    if (op.tok == Token.propselector) {
      switch (iv) {
      case Token.length:
        if (!(x2.value instanceof BondSet))
          return addX(ScriptVariable.sizeOf(x2));
        break;
      case Token.size:
        return addX(ScriptVariable.sizeOf(x2));
      case Token.type:
        return addX(ScriptVariable.typeOf(x2));
      case Token.lines:
        String s = (x2.tok == Token.string ? (String) x2.value : ScriptVariable
            .sValue(x2));
        s = TextFormat.simpleReplace(s, "\n\r", "\n").replace('\r', '\n');
        return addX(TextFormat.split(s, '\n'));
      case Token.color:
        switch (x2.tok) {
        case Token.string:
        case Token.list:
          Point3f pt = new Point3f();
          return addX(Graphics3D.colorPointFromString(
              ScriptVariable.sValue(x2), pt));
        case Token.integer:
        case Token.decimal:
          return addX(viewer.getColorPointForPropertyValue(ScriptVariable
              .fValue(x2)));
        case Token.point3f:
          return addX(Escape.escapeColor(ScriptEvaluator
              .colorPtToInt((Point3f) x2.value)));
        default:
          // handle bitset later
        }
        break;
      case Token.boundbox:
        return (isSyntaxCheck ? addX("x") : evaluateBoundBox(x2));
      }
      if (isSyntaxCheck)
        return addX(ScriptVariable.sValue(x2));
      if (x2.tok == Token.string) {
        Object v = ScriptVariable
            .unescapePointOrBitsetAsVariable(ScriptVariable.sValue(x2));
        if (!(v instanceof ScriptVariable))
          return false;
        x2 = (ScriptVariable) v;
      }
      if (op.tok == x2.tok)
        x2 = getX();
      return evaluatePointOrBitsetOperation(op, x2);
    }

    // binary:
    String s;
    ScriptVariable x1 = getX();
    if (isSyntaxCheck)
      return addX(new ScriptVariable(x1));
    switch (op.tok) {
    case Token.opAnd:
      if (x1.tok == Token.bitset && x2.tok == Token.bitset) {
        BitSet bs = BitSetUtil.copy(ScriptVariable.bsSelect(x1));
        bs.and(ScriptVariable.bsSelect(x2));
        return addX(bs);
      }
      return addX(ScriptVariable.bValue(x1) && ScriptVariable.bValue(x2));
    case Token.opOr:
      if (x1.tok == Token.bitset && x2.tok == Token.bitset) {
        BitSet bs = BitSetUtil.copy(ScriptVariable.bsSelect(x1));
        bs.or(ScriptVariable.bsSelect(x2));
        return addX(bs);
      }
      return addX(ScriptVariable.bValue(x1) || ScriptVariable.bValue(x2));
    case Token.opXor:
      if (x1.tok == Token.bitset && x2.tok == Token.bitset) {
        BitSet bs = BitSetUtil.copy(ScriptVariable.bsSelect(x1));
        bs.xor(ScriptVariable.bsSelect(x2));
        return addX(bs);
      }
      boolean a = ScriptVariable.bValue(x1);
      boolean b = ScriptVariable.bValue(x2);
      return addX(a && !b || b && !a);
    case Token.opToggle:
      if (x1.tok != Token.bitset || x2.tok != Token.bitset)
        return false;
      return addX(BitSetUtil.toggleInPlace(BitSetUtil.copy(ScriptVariable
          .bsSelect(x1)), ScriptVariable.bsSelect(x2), viewer.getAtomCount()));
    case Token.opLE:
      return addX(ScriptVariable.fValue(x1) <= ScriptVariable.fValue(x2));
    case Token.opGE:
      return addX(ScriptVariable.fValue(x1) >= ScriptVariable.fValue(x2));
    case Token.opGT:
      return addX(ScriptVariable.fValue(x1) > ScriptVariable.fValue(x2));
    case Token.opLT:
      return addX(ScriptVariable.fValue(x1) < ScriptVariable.fValue(x2));
    case Token.opEQ:
      if (x1.tok == Token.string && x2.tok == Token.string)
        return addX(ScriptVariable.sValue(x1).equalsIgnoreCase(
            ScriptVariable.sValue(x2)));
      if (x1.tok == Token.point3f && x2.tok == Token.point3f)
        return addX(((Point3f) x1.value).distance((Point3f) x2.value) < 0.000001);
      if (x1.tok == Token.point4f && x2.tok == Token.point4f)
        return addX(((Point4f) x1.value).distance((Point4f) x2.value) < 0.000001);
      return addX(Math.abs(ScriptVariable.fValue(x1)
          - ScriptVariable.fValue(x2)) < 0.000001);
    case Token.opNE:
      if (x1.tok == Token.string && x2.tok == Token.string)
        return addX(!(ScriptVariable.sValue(x1).equalsIgnoreCase(ScriptVariable
            .sValue(x2))));
      if (x1.tok == Token.point3f && x2.tok == Token.point3f)
        return addX(((Point3f) x1.value).distance((Point3f) x2.value) >= 0.000001);
      if (x1.tok == Token.point4f && x2.tok == Token.point4f)
        return addX(((Point4f) x1.value).distance((Point4f) x2.value) >= 0.000001);
      {
        float f1 = ScriptVariable.fValue(x1);
        float f2 = ScriptVariable.fValue(x2);
        return addX(Float.isNaN(f1) || Float.isNaN(f2)
            || Math.abs(f1 - f2) >= 0.000001);
      }
    case Token.plus:
      if (x1.tok == Token.list || x2.tok == Token.list)
        return addX(ScriptVariable.concatList(x1, x2));
      if (x1.tok == Token.integer) {
        if (x2.tok == Token.string) {
          if ((s = (ScriptVariable.sValue(x2)).trim()).indexOf(".") < 0
              && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
            return addX(x1.intValue + ScriptVariable.iValue(x2));
        } else if (x2.tok != Token.decimal)
          return addX(x1.intValue + ScriptVariable.iValue(x2));
      }
      switch (x1.tok) {
      default:
        return addX(ScriptVariable.fValue(x1) + ScriptVariable.fValue(x2));
      case Token.string:
        return addX(new ScriptVariable(Token.string, ScriptVariable.sValue(x1)
            + ScriptVariable.sValue(x2)));
      case Token.point4f:
        Quaternion q1 = new Quaternion((Point4f) x1.value);
        switch (x2.tok) {
        default:
          return addX(q1.add(ScriptVariable.fValue(x2)).toPoint4f());
        case Token.point4f:
          return addX(q1.mul(new Quaternion((Point4f) x2.value)).toPoint4f());
        }
      case Token.point3f:
        Point3f pt = new Point3f((Point3f) x1.value);
        switch (x2.tok) {
        case Token.point3f:
          pt.add((Point3f) x2.value);
          return addX(pt);
        case Token.point4f:
          // extract {xyz}
          Point4f pt4 = (Point4f) x2.value;
          pt.add(new Point3f(pt4.x, pt4.y, pt4.z));
          return addX(pt);
        default:
          float f = ScriptVariable.fValue(x2);
          return addX(new Point3f(pt.x + f, pt.y + f, pt.z + f));
        }
      }
    case Token.minus:
      if (x1.tok == Token.integer) {
        if (x2.tok == Token.string) {
          if ((s = (ScriptVariable.sValue(x2)).trim()).indexOf(".") < 0
              && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
            return addX(x1.intValue - ScriptVariable.iValue(x2));
        } else if (x2.tok != Token.decimal)
          return addX(x1.intValue - ScriptVariable.iValue(x2));
      }
      if (x1.tok == Token.string && x2.tok == Token.integer) {
        if ((s = (ScriptVariable.sValue(x1)).trim()).indexOf(".") < 0
            && s.indexOf("+") <= 0 && s.lastIndexOf("-") <= 0)
          return addX(ScriptVariable.iValue(x1) - x2.intValue);
      }
      switch (x1.tok) {
      default:
        return addX(ScriptVariable.fValue(x1) - ScriptVariable.fValue(x2));
      case Token.point3f:
        Point3f pt = new Point3f((Point3f) x1.value);
        switch (x2.tok) {
        default:
          float f = ScriptVariable.fValue(x2);
          return addX(new Point3f(pt.x - f, pt.y - f, pt.z - f));
        case Token.point3f:
          pt.sub((Point3f) x2.value);
          return addX(pt);
        case Token.point4f:
          // extract {xyz}
          Point4f pt4 = (Point4f) x2.value;
          pt.sub(new Point3f(pt4.x, pt4.y, pt4.z));
          return addX(pt);
        }
      case Token.point4f:
        Quaternion q1 = new Quaternion((Point4f) x1.value);
        switch (x2.tok) {
        default:
          return addX(q1.add(-ScriptVariable.fValue(x2)).toPoint4f());
        case Token.point4f:
          Quaternion q2 = new Quaternion((Point4f) x2.value);
          return addX(q2.mul(q1.inv()).toPoint4f());
        }
      }
    case Token.unaryMinus:
      switch (x2.tok) {
      default:
        return addX(-ScriptVariable.fValue(x2));
      case Token.integer:
        return addX(-ScriptVariable.iValue(x2));
      case Token.point3f:
        Point3f pt = new Point3f((Point3f) x2.value);
        pt.scale(-1f);
        return addX(pt);
      case Token.point4f:
        Point4f plane = new Point4f((Point4f) x2.value);
        plane.scale(-1f);
        return addX(plane);
      case Token.bitset:
        return addX(BitSetUtil.copyInvert(ScriptVariable.bsSelect(x2),
            (x2.value instanceof BondSet ? viewer.getBondCount() : viewer
                .getAtomCount())));
      }
    case Token.times:
      if (x1.tok == Token.integer && x2.tok != Token.decimal)
        return addX(x1.intValue * ScriptVariable.iValue(x2));
      switch (x1.tok) {
      default:
        return addX(ScriptVariable.fValue(x1) * ScriptVariable.fValue(x2));
      case Token.point3f:
        Point3f pt = new Point3f((Point3f) x1.value);
        switch (x2.tok) {
        case Token.point3f:
          Point3f pt2 = ((Point3f) x2.value);
          return addX(pt.x * pt2.x + pt.y * pt2.y + pt.z * pt2.z);
        default:
          float f = ScriptVariable.fValue(x2);
          return addX(new Point3f(pt.x * f, pt.y * f, pt.z * f));
        }
      case Token.point4f:
        if (x2.tok == Token.point4f) {
          // quaternion multiplication
          // note that Point4f is {x,y,z,w} so we use that for
          // quaternion notation as well here.
          Quaternion q1 = new Quaternion((Point4f) x1.value);
          Quaternion q = new Quaternion((Point4f) x2.value);
          q = q1.mul(q);
          return addX(new Point4f(q.q1, q.q2, q.q3, q.q0));
        }
        return addX(new Quaternion((Point4f) x1.value).mul(
            ScriptVariable.fValue(x2)).toPoint4f());
      }
    case Token.percent:
      // more than just modulus

      // float % n round to n digits; n = 0 does "nice" rounding
      // String % -n trim to width n; left justify
      // String % n trim to width n; right justify
      // Point3f % n ah... sets to multiple of unit cell!
      // bitset % n
      // Point3f * Point3f does dot product
      // Point3f / Point3f divides by magnitude
      // float * Point3f gets magnitude
      // Point4f % n returns q0, q1, q2, q3, or theta
      // Point4f % Point4f
      s = null;
      int n = ScriptVariable.iValue(x2);
      switch (x1.tok) {
      case Token.on:
      case Token.off:
      case Token.integer:
      default:
        if (n == 0)
          return addX((int) 0);
        return addX(ScriptVariable.iValue(x1) % n);
      case Token.decimal:
        float f = ScriptVariable.fValue(x1);
        // neg is scientific notation
        if (n == 0)
          return addX((int) (f + 0.5f * (f < 0 ? -1 : 1)));
        s = TextFormat.formatDecimal(f, n);
        return addX(s);
      case Token.string:
        s = (String) x1.value;
        if (n == 0)
          return addX(TextFormat.trim(s, "\n\t "));
        else if (n > 0)
          return addX(TextFormat.format(s, n, n, false, false));
        return addX(TextFormat.format(s, -n, n, true, false));
      case Token.list:
        String[] list = (String[]) x1.value;
        String[] listout = new String[list.length];
        for (int i = 0; i < list.length; i++) {
          if (n == 0)
            listout[i] = list[i].trim();
          else if (n > 0)
            listout[i] = TextFormat.format(list[i], n, n, true, false);
          else
            listout[i] = TextFormat.format(s, -n, n, false, false);
        }
        return addX(listout);
      case Token.point3f:
        Point3f pt = new Point3f((Point3f) x1.value);
        viewer.toUnitCell(pt, new Point3f(n, n, n));
        return addX(pt);
      case Token.point4f:
        Point4f q = (Point4f) x1.value;
        if (x2.tok == Token.point3f)
          return addX((new Quaternion(q)).transform((Point3f) x2.value));
        if (x2.tok == Token.point4f) {
          Point4f v4 = new Point4f((Point4f) x2.value);
          (new Quaternion(q)).getThetaDirected(v4);
          return addX(v4);
        }
        switch (n) {
        // q%0 w
        // q%1 x
        // q%2 y
        // q%3 z
        // q%4 normal
        // q%-1 vector(1)
        // q%-2 theta
        // q%-3 Matrix column 0
        // q%-4 Matrix column 1
        // q%-5 Matrix column 2
        // q%-6 AxisAngle format
        case 0:
          return addX(q.w);
        case 1:
          return addX(q.x);
        case 2:
          return addX(q.y);
        case 3:
          return addX(q.z);
        case 4:
          return addX((new Quaternion(q)).getNormal());
        case -1:
          return addX(new Quaternion(q).getVector(-1));
        case -2:
          return addX((new Quaternion(q)).getTheta());
        case -3:
          return addX((new Quaternion(q)).getVector(0));
        case -4:
          return addX((new Quaternion(q)).getVector(1));
        case -5:
          return addX((new Quaternion(q)).getVector(2));
        case -6:
          AxisAngle4f ax = (new Quaternion(q)).toAxisAngle4f();
          return addX(new Point4f(ax.x, ax.y, ax.z, (float) (ax.angle * 180 / Math.PI)));
        default:
          return addX(q);
        }
      case Token.bitset:
        return addX(ScriptVariable.bsSelect(x1, n));
      }
    case Token.divide:
      if (x1.tok == Token.integer && x2.tok == Token.integer
          && x2.intValue != 0)
        return addX(x1.intValue / x2.intValue);
      float f2 = ScriptVariable.fValue(x2);
      switch (x1.tok) {
      default:
        float f1 = ScriptVariable.fValue(x1);
        if (f2 == 0)
          return addX(f1 == 0 ? 0f : f1 < 0 ? Float.POSITIVE_INFINITY
              : Float.POSITIVE_INFINITY);
        return addX(f1 / f2);
      case Token.point3f:
        Point3f pt = new Point3f((Point3f) x1.value);
        if (f2 == 0)
          return addX(new Point3f(Float.NaN, Float.NaN, Float.NaN));
        return addX(new Point3f(pt.x / f2, pt.y / f2, pt.z / f2));
      case Token.point4f:
        if (x2.tok == Token.point4f)
          return addX(new Quaternion((Point4f) x1.value).div(
              new Quaternion((Point4f) x2.value)).toPoint4f());
        if (f2 == 0)
          return addX(new Point4f(Float.NaN, Float.NaN, Float.NaN, Float.NaN));
        return addX(new Quaternion((Point4f) x1.value).mul(1 / f2).toPoint4f());
      }
    case Token.leftdivide:
      float f = ScriptVariable.fValue(x2);
      switch (x1.tok) {
      default:
        return addX(f == 0 ? 0
            : (int) (ScriptVariable.fValue(x1) / ScriptVariable.fValue(x2)));
      case Token.point4f:
        if (f == 0)
          return addX(new Point4f(Float.NaN, Float.NaN, Float.NaN, Float.NaN));
        if (x2.tok == Token.point4f)
          return addX(new Quaternion((Point4f) x1.value).divLeft(
              new Quaternion((Point4f) x2.value)).toPoint4f());
        return addX(new Quaternion((Point4f) x1.value).mul(1 / f).toPoint4f());
      }
    }
    return true;
  }

  private boolean evaluateBoundBox(ScriptVariable x2) {
    if (x2.tok != Token.bitset)
      return false;
    if (isSyntaxCheck)
      return addX("");
    BoxInfo b = viewer.getBoxInfo(ScriptVariable.bsSelect(x2));
    Point3f[] pts = b.getBoundBoxPoints();
    return addX(new String[] { Escape.escape(pts[0]), Escape.escape(pts[1]),
        Escape.escape(pts[2]), Escape.escape(pts[3]) });
  }

  private boolean evaluatePointOrBitsetOperation(Token op, ScriptVariable x2)
      throws ScriptException {
    switch (x2.tok) {
    case Token.list:
      String[] list = (String[]) x2.value;
      if (op.intValue == Token.min || op.intValue == Token.max
          || op.intValue == Token.average || op.intValue == Token.stddev
          || op.intValue == Token.sum2) {
        return addX(ArrayUtil.getMinMax(list, op.intValue));
      }
      if (op.intValue == Token.sort || op.intValue == Token.reverse)
        return addX(ArrayUtil.sortOrReverse(x2.value, op.intValue, true));
      String[] list2 = new String[list.length];
      for (int i = 0; i < list.length; i++) {
        Object v = ScriptVariable.unescapePointOrBitsetAsVariable(list[i]);
        if (!(v instanceof ScriptVariable)
            || !evaluatePointOrBitsetOperation(op, (ScriptVariable) v))
          return false;
        list2[i] = ScriptVariable.sValue(xStack[xPt--]);
      }
      return addX(list2);
    case Token.point3f:
      switch (op.intValue) {
      case Token.atomX:
        return addX(((Point3f) x2.value).x);
      case Token.atomY:
        return addX(((Point3f) x2.value).y);
      case Token.atomZ:
        return addX(((Point3f) x2.value).z);
      case Token.xyz:
        Point3f pt = new Point3f((Point3f) x2.value);
        // assumes a fractional coordinate
        viewer.toCartesian(pt);
        return addX(pt);
      case Token.fracX:
      case Token.fracY:
      case Token.fracZ:
      case Token.fracXyz:
        Point3f ptf = new Point3f((Point3f) x2.value);
        viewer.toFractional(ptf);
        return (op.intValue == Token.fracXyz ? addX(ptf)
            : addX(op.intValue == Token.fracX ? ptf.x
                : op.intValue == Token.fracY ? ptf.y : ptf.z));
      case Token.unitX:
      case Token.unitY:
      case Token.unitZ:
      case Token.unitXyz:
        Point3f ptu = new Point3f((Point3f) x2.value);
        viewer.toUnitCell(ptu, null);
        viewer.toFractional(ptu);
        return (op.intValue == Token.unitXyz ? addX(ptu)
            : addX(op.intValue == Token.fracX ? ptu.x
                : op.intValue == Token.fracY ? ptu.y : ptu.z));
      }
      break;
    case Token.point4f:
      switch (op.intValue) {
      case Token.atomX:
        return addX(((Point4f) x2.value).x);
      case Token.atomY:
        return addX(((Point4f) x2.value).y);
      case Token.atomZ:
        return addX(((Point4f) x2.value).z);
      case Token.qw:
        return addX(((Point4f) x2.value).w);
      }
      break;
    case Token.bitset:
      if (op.intValue == Token.bonds && x2.value instanceof BondSet)
        return addX(x2);
      BitSet bs = ScriptVariable.bsSelect(x2);
      Object val = eval.getBitsetProperty(bs, op.intValue, null, null,
          x2.value, op.value, false, x2.index);
      if (op.intValue == Token.bonds)
        val = new ScriptVariable(Token.bitset, new BondSet((BitSet) val, viewer
            .getAtomIndices(bs)));
      return addX(val);
    }
    return false;
  }

}
