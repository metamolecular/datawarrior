/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-26 17:54:47 +0200 (mer., 26 août 2009) $
 * $Revision: 11371 $
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

import java.awt.Image;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.api.MinimizerInterface;
import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.ModelCollection;
import org.jmol.modelset.ModelSet;
import org.jmol.modelset.Bond.BondSet;
import org.jmol.modelset.ModelCollection.StateScript;
import org.jmol.shape.Object2d;
import org.jmol.util.BitSetUtil;
import org.jmol.util.ColorEncoder;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;

class ScriptEvaluator {

  /*
   * The ScriptEvaluator class, the Viewer, the xxxxManagers, the
   * Graphics3D rendeing engine, the Shape classes, and the Adapter file
   * reader classes form the core of the Jmol molecular visualization framework.
   * 
   * The ScriptEvaluator has just a few entry points, which you will find
   * immediately following this comment. They include:
   * 
   *  public boolean compileScriptString(String script, boolean tQuiet)
   *  
   *  public boolean compileScriptFile(String filename, boolean tQuiet)
   *
   *  public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option,
   *                  boolean isCmdLine_C_Option, boolean historyDisabled,
   *                  boolean listCommands)
   *                  
   * Essentially ANYTHING can be done using these three methods. A variety
   * of other methods are available via Viewer, which is the the true portal
   * to Jmol (via the JmolViewer interface) for application developers who
   * want faster, more direct processing. 
   * 
   * A little Jmol history:
   * 
   * General history notes can be found at our ConfChem paper, which can be found at 
   * http://chemapps.stolaf.edu/jmol/presentations/confchem2006/jmol-confchem.htm
   * 
   * This ScriptEvaluator class was initially written by Michael (Miguel) Howard
   * as Eval.java as an efficient means of reproducing the RasMol scripting
   * language for Jmol. Key additions there included:
   * 
   *   - tokenization of commands via the Compiler class (now ScriptCompiler and 
   *     ScriptCompilationTokenParser)
   *   - ScriptException error handling 
   *   - a flexible yet structured command parameter syntax
   *   - implementations of RasMol secondary structure visualizations
   *   - isosurfaces, dots, labels, polyhedra, draw, stars, pmesh, more
   *   
   * Other Miguel contributions include:
   * 
   *   - the structural bases of the Adapter, ModelSet, and ModelSetBio classes 
   *   - creation of Manager classes
   *   - absolutely amazing raw pixel bitmap rendering code (org.jmol.g3d)
   *   - popup context menu
   *   - inline model loading
   * 
   * Bob Hanson (St. Olaf College) found out about Jmol during the spring of
   * 2004. After spending over a year working on developing online interactive
   * documentation, he started actively writing code early in 2006. During the
   * period 2006-2009 Bob completely reworked the script processor (and much of
   * the rest of Jmol) to handle a much broader range of functionality. Notable
   * improvements include:
   * 
   *   - display/hide commands
   *   - dipole, ellipsoid, geosurface, lcaoCartoon visualizations
   *   - quaternion and ramachandran commands
   *   - much expanded isosurface / draw commands
   *   - configuration, disorder, and biomolecule support
   *   - broadly 2D- and 3D-positionable echos
   *   - translateSelected and rotateSelected commands
   *   - getProperty command, providing access to more file information 
   *   - data and write commands
   *   - writing of high-resolution JPG, PNG, and movie-sequence JPG 
   *   - generalized export to Maya and PovRay formats
   *   
   *   - multiple file loading, including trajectories
   *   - minimization using the Universal Force Field (UFF)
   *   - atom/model deletion and addition
   *   - direct loading of properties such as partial charge or coordinates
   *   - several new file readers, including manifested zip file reading
   *   - default directory, CD command, and pop-up file open/save dialogs
   *   
   *   - "internal" molecular coordinate-based rotations
   *   - full support for crystallographic formats, including
   *     space groups, symmetry, unit cells, and fractional coordinates
   *   - support for point groups and molecular symmetry
   *   - navigation mode
   *   - antialiasing of display and imaging 
   *   - save/restore/write exact Jmol state
   *   - JVXL file format for compressed rapid generation of isosurfaces
   *   
   *   - user-defined variables
   *   - addition of a Reverse Polish Notation (RPN) expression processor
   *   - extension of the RPN processor to user variables
   *   - user-defined functions
   *   - flow control commands if/else/endif, for, and while
   *   - JavaScript/Java-like brace syntax
   *   - key stroke-by-key stroke command syntax checking
   *   - integrated help command
   *   - user-definable popup menu
   *   - language switching
   *   
   *   - fully functional signed applet
   *   - applet-applet synchronization, including two-applet geoWall stereo rendering
   *   - JSON format for property delivery to JavaScript
   *   - jmolScriptWait, dual-threaded queued JavaScript scripting interface
   *   - extensive callback development
   *   - script editor panel (work in progress, June 2009)
   *
   * Several other people have contributed. Perhaps they will not be too shy
   * to add their claim to victory here. Please add your contributions.
   * 
   *   - Jmol application (Egon Willighagen)
   *   - smiles support (Nico Vervelle)
   *   - readers (Rene Kanter, Egon, several others)
   *   - initial VRML export work (Nico Vervelle)
   *   - WebExport (Jonathan Gutow)
   *   - internationalization (Nico, Egon, Angel Herriez)
   *   - Jmol Wiki and user guide book (Angel Herriez)
   *   
   * While this isn't necessarily the best place for such discussion,
   * open source principles require proper credit given to those who have
   * contributed. This core class seems to me a place to acknowledge this
   * core work of the Jmol team.  
   *   
   *   Bob Hanson, 6/2009
   *   hansonr@stolaf.edu
   */
  
  ScriptEvaluator(Viewer viewer) {
    this.viewer = viewer;
    compiler = viewer.compiler;
    definedAtomSets = viewer.definedAtomSets;
  }

  ////////////////// primary interfacing methods //////////////////
  
  /*
   * see Viewer.evalStringWaitStatus for how these are implemented
   * 
   */
  public boolean compileScriptString(String script, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = "[script]";
    return compileScript(null, script, debugScript);
  }

  public boolean compileScriptFile(String filename, boolean tQuiet) {
    clearState(tQuiet);
    contextPath = filename;
    return compileScriptFileInternal(filename);
  }

  public void evaluateCompiledScript(boolean isCmdLine_c_or_C_Option,
                      boolean isCmdLine_C_Option, boolean historyDisabled,
                      boolean listCommands) {
    boolean tempOpen = this.isCmdLine_C_Option;
    this.isCmdLine_C_Option = isCmdLine_C_Option;
    viewer.pushHoldRepaint("runEval");
    try {
	    interruptExecution = executionPaused = false;
	    executionStepping = false;
	    isExecuting = true;
	    currentThread = Thread.currentThread();
	    isSyntaxCheck = this.isCmdLine_c_or_C_Option = isCmdLine_c_or_C_Option;
	    timeBeginExecution = System.currentTimeMillis();
	    this.historyDisabled = historyDisabled;
	    setErrorMessage(null);
	    
	    try {
	      try {
	        setScriptExtensions();
	        instructionDispatchLoop(listCommands);
	        String script = viewer.getInterruptScript();
	        if (script != "")
	          runScript(script, null);
	      } catch (Error er) {
	        viewer.handleError(er, false);
	        setErrorMessage("" + er + " " + viewer.getShapeErrorState());
	        errorMessageUntranslated = "" + er;
	        scriptStatusOrBuffer(errorMessage);
	      }
	    } catch (ScriptException e) {
	      setErrorMessage(e.toString());
	      errorMessageUntranslated = e.getErrorMessageUntranslated();
	      scriptStatusOrBuffer(errorMessage);
	      viewer.notifyError((errorMessage != null
	          && errorMessage.indexOf("java.lang.OutOfMemoryError") >= 0 ? "Error"
	          : "ScriptException"), errorMessage, errorMessageUntranslated);
	    }
	    timeEndExecution = System.currentTimeMillis();
	    this.isCmdLine_C_Option = tempOpen;
	    if (errorMessage == null && interruptExecution)
	      setErrorMessage("execution interrupted");
	    else if (!tQuiet && !isSyntaxCheck)
	      viewer.scriptStatus(ScriptManager.SCRIPT_COMPLETED);
	    isExecuting = isSyntaxCheck = isCmdLine_c_or_C_Option = historyDisabled = false;
	    viewer.setTainted(true);
    } finally {
    	viewer.popHoldRepaint("runEval");
    }
  }

  /**
   * runs a script and sends selected output to a provided StringBuffer
   * 
   * @param script
   * @param outputBuffer
   * @throws ScriptException
   */
  public void runScript(String script, StringBuffer outputBuffer)
      throws ScriptException {
    // a = script("xxxx")
    pushContext(null);
    contextPath += " >> script() ";

    this.outputBuffer = outputBuffer;
    if (compileScript(null, script + JmolConstants.SCRIPT_EDITOR_IGNORE, false))
      instructionDispatchLoop(false);
    popContext();
  }

  /**
   * a method for just checking a script
   * 
   * @param script
   * @return       a ScriptContext that indicates errors and provides a
   *               tokenized version of the script that has passed 
   *               all syntax checking, both in the compiler and 
   *               the evaluator
   *               
   */
  public ScriptContext checkScriptSilent(String script) {
    ScriptContext sc = compiler.compile(null, script, false, true, false, true);
    if (sc.errorType != null)
      return sc;
    getScriptContext(sc, false);
    isSyntaxCheck = true;
    isCmdLine_c_or_C_Option = isCmdLine_C_Option = false;
    pc = 0;
    try {
      instructionDispatchLoop(false);
    } catch (ScriptException e) {
      setErrorMessage(e.toString());
      sc = getScriptContext();
    }
    isSyntaxCheck = false;
    return sc;
  }

  ////////////////////////// script execution /////////////////////
  
  private boolean tQuiet;
  protected boolean isSyntaxCheck;
  private boolean isCmdLine_C_Option;
  protected boolean isCmdLine_c_or_C_Option;
  private boolean historyDisabled;
  protected boolean logMessages;
  private boolean debugScript;
  
  void setDebugging() {
    debugScript = viewer.getDebugScript();
    logMessages = (debugScript && Logger.debugging);
  }

  private boolean interruptExecution;
  private boolean executionPaused;
  private boolean executionStepping;
  private boolean isExecuting;

  private long timeBeginExecution;
  private long timeEndExecution;

  int getExecutionWalltime() {
    return (int) (timeEndExecution - timeBeginExecution);
  }

  void haltExecution() {
    resumePausedExecution();
    interruptExecution = true;
  }

  void pauseExecution() {
    if (isSyntaxCheck)
      return;
    delay(-100);
    viewer.popHoldRepaint("pauseExecution");
    executionStepping = false;
    executionPaused = true;
  }

  void stepPausedExecution() {
    executionStepping = true;
    executionPaused = false;
    //releases a paused thread but
    //sets it to pause for the next command.
  }

  void resumePausedExecution() {
    executionPaused = false;
    executionStepping = false;
  }

  boolean isScriptExecuting() {
    return isExecuting && !interruptExecution;
  }

  boolean isExecutionPaused() {
    return executionPaused;
  }

  boolean isExecutionStepping() {
    return executionStepping;
  }

  /**
   * when paused, indicates what statement will be next
   * 
   * @return  a string indicating the statement
   */
  String getNextStatement() {
    return (pc < aatoken.length ? 
        setErrorLineMessage(functionName, filename,
            getLinenumber(null), pc, statementAsString(aatoken[pc], -9999)) : "");  
  }
  
  /** 
   * used for recall of commands in the application console
   * 
   * @param pc
   * @param allThisLine
   * @param addSemi
   * @return               a string representation of the command
   */
  private String getCommand(int pc, boolean allThisLine, boolean addSemi) {
    if (pc >= lineIndices.length)
      return "";
    if (allThisLine) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < lineNumbers.length; i++)
        if (lineNumbers[i] == lineNumbers[pc])
          sb.append(getCommand(i, false, false));
        else if (lineNumbers[i] == 0 || lineNumbers[i] > lineNumbers[pc]) {
          break;
        }
      return sb.toString();
    }
    int ichBegin = lineIndices[pc][0];
    int ichEnd = lineIndices[pc][1];
    //(pc + 1 == lineIndices.length || lineIndices[pc + 1][0] == 0 ? script
      //  .length()
        //: lineIndices[pc + 1]);
    String s = "";
    if (ichBegin < 0 || ichEnd <= ichBegin || ichEnd > script.length())
      return "";
    try {
      s = script.substring(ichBegin, ichEnd);
      if (s.indexOf("\\\n") >= 0)
        s = TextFormat.simpleReplace(s, "\\\n", "  ");
      if (s.indexOf("\\\r") >= 0)
        s = TextFormat.simpleReplace(s, "\\\r", "  ");
      //int i;
      //for (i =  s.length(); --i >= 0 && !ScriptCompiler.eol(s.charAt(i), 0); ){
      //}      
      //s = s.substring(0, i + 1);
      if (s.length() > 0 && !s.endsWith(";") && !s.endsWith("{")
          && !s.endsWith("}"))
        s += ";";
    } catch (Exception e) {
      Logger.error("darn problem in Eval getCommand: ichBegin=" + ichBegin
          + " ichEnd=" + ichEnd + " len = " + script.length() + "\n" + e);
    }
    return s;
  }

  private void logDebugScript(int ifLevel) {
    if (logMessages) {
      if (statement.length > 0)
        Logger.debug(statement[0].toString());
      for (int i = 1; i < statementLength; ++i)
        Logger.debug(statement[i].toString());
    }
    iToken = -9999;
    if (logMessages) {
      StringBuffer strbufLog = new StringBuffer(80);
      String s = (ifLevel > 0 ? "                          ".substring(0,
          ifLevel * 2) : "");
      strbufLog.append(s).append(statementAsString(statement, iToken));
      viewer.scriptStatus(strbufLog.toString());
    } else {
      String cmd = getCommand(pc, false, false);
      viewer.scriptStatus(cmd);
    }

  }

  ///////////////// string-based evaluation support /////////////////////
  
  private final static String EXPRESSION_KEY = "e_x_p_r_e_s_s_i_o_n";

  /**
   * a general-use method to evaluate a "SET" type expression.
   * 
   * @param viewer
   * @param expr
   * @return an object of one of the following types: Boolean, Integer, Float,
   *         String, Point3f, BitSet
   */

  static Object evaluateExpression(Viewer viewer, Object expr) {
    // Text.formatText for MESSAGE and ECHO
    ScriptEvaluator e = new ScriptEvaluator(viewer);
    try {
      if (expr instanceof String) {
        if (e.compileScript(null, EXPRESSION_KEY + " = " + expr, false)) {
          e.contextVariables = viewer.eval.contextVariables;
          e.setStatement(0);
          return e.parameterExpression(2, 0, "", false);
        }
      } else if (expr instanceof Token[]) {
        e.contextVariables = viewer.eval.contextVariables;
        return e.expression((Token[]) expr, 0, 0, true, false, true, false);
      }
    } catch (Exception ex) {
      Logger.error("Error evaluating: " + expr + "\n" + ex);
    }
    return "ERROR";
  }

  /**
   *  a general method to evaluate a string representing an atom set.
   *  
   * @param e
   * @param atomExpression
   * @return                is a bitset indicating the selected atoms
   * 
   */
  static BitSet getAtomBitSet(ScriptEvaluator e, Object atomExpression) {
    if (atomExpression instanceof BitSet)
      return (BitSet) atomExpression;
    BitSet bs = new BitSet();
    try {
      e.pushContext(null);
      String scr = "select (" + atomExpression + ")";
      scr = TextFormat.replaceAllCharacters(scr, "\n\r", "),(");
      scr = TextFormat.simpleReplace(scr, "()", "(none)");
      if (e.compileScript(null, scr, false)) {
        e.statement = e.aatoken[0];
        bs = e.expression(e.statement, 1, 0, false, false, true, true);
      }
      e.popContext();
    } catch (Exception ex) {
      Logger.error("getAtomBitSet " + atomExpression + "\n" + ex);
    }
    return bs;
  }

  /**
   * just provides a vector list of atoms in a string-based expression
   * 
   * @param e
   * @param atomCount
   * @param atomExpression
   * @return                vector list of selected atoms
   */
  static Vector getAtomBitSetVector(ScriptEvaluator e, int atomCount,
                                    Object atomExpression) {
    Vector V = new Vector();
    BitSet bs = getAtomBitSet(e, atomExpression);
    for (int i = 0; i < atomCount; i++)
      if (bs.get(i))
        V.addElement(new Integer(i));
    return V;
  }

  private Object parameterExpression(int pt, int ptMax, String key,
                                     boolean asVector) throws ScriptException {
    return parameterExpression(pt, ptMax, key, asVector, -1, false, null, null);
  }

  /**
   * This is the primary driver of the RPN (reverse Polish notation) expression
   * processor. It handles all math outside of a "traditional" Jmol
   * SELECT/RESTRICT context. [Object expression() takes care of that, and also
   * uses the RPN class.]
   * 
   * @param pt
   *          token index in statement start of expression
   * @param ptMax
   *          token index in statement end of expression
   * @param key
   *          variable name for debugging reference only -- null indicates
   *          return Boolean -- "" indicates return String
   * @param asVector
   *          a flag passed on to RPN;
   * @param ptAtom
   *          this is a for() or select() function with a specific atom selected
   * @param isArrayItem
   *          we are storing A[x] = ... so we need to deliver "x" as well
   * @param localVars
   *          see below -- lists all nested for(x, {exp}, select(y, {ex},...))
   *          variables
   * @param localVar
   *          x or y in above for(), select() examples
   * @return either a vector or a value, caller's choice.
   * @throws ScriptException
   *           errors are thrown directly to the Eval error system.
   */
  private Object parameterExpression(int pt, int ptMax, String key,
                                     boolean asVector, int ptAtom,
                                     boolean isArrayItem, Hashtable localVars,
                                     String localVar) throws ScriptException {

    /*
     * localVar is a variable designated at the beginning of the select(x,...)
     * or for(x,...) construct that will be implicitly used for properties. So,
     * for example, "atomno" will become "x.atomno". That's all it is for.
     * localVars provides a localized context variable set for a given nested
     * set of for/select.
     * 
     * Note that localVars has nothing to do standard if/for/while flow
     * contexts, just these specialized functions. Any variable defined in for
     * or while is simply added to the context for a given script or function.
     * These assignments are made by the compiler when seeing a VAR keyword.
     */
    Object v, res;
    boolean isImplicitAtomProperty = (localVar != null);
    boolean isOneExpressionOnly = (pt < 0);
    boolean returnBoolean = (key == null);
    boolean returnString = (key != null && key.length() == 0);
    if (isOneExpressionOnly)
      pt = -pt;
    int nParen = 0;
    ScriptMathProcessor rpn = new ScriptMathProcessor(this, isArrayItem, asVector);
    if (pt == 0 && ptMax == 0) // set command with v[...] = ....
      pt = 2;
    if (ptMax < pt)
      ptMax = statementLength;
    out: for (int i = pt; i < ptMax; i++) {
      v = null;
      int tok = getToken(i).tok;
      if (isImplicitAtomProperty && tokAt(i + 1) != Token.period) {
        ScriptVariable token = (localVars != null
            && localVars.containsKey(theToken.value) ? null
            : getBitsetPropertySelector(i, false));
        if (token != null) {
          rpn.addX((ScriptVariable) localVars.get(localVar));
          if (!rpn.addOp(token)) {
            error(ERROR_invalidArgument);
          }
          if (token.intValue == Token.function
              && tokAt(iToken + 1) != Token.leftparen) {
            rpn.addOp(Token.tokenLeftParen);
            rpn.addOp(Token.tokenRightParen);
          }
          i = iToken;
          continue;
        }
      }
      switch (tok) {
      case Token.ifcmd:
        if (getToken(++i).tok != Token.leftparen)
          error(ERROR_invalidArgument);
        if (localVars == null)
          localVars = new Hashtable();
        res = parameterExpression(++i, -1, null, false, -1, false, localVars,
            localVar);
        boolean TF = ((Boolean) res).booleanValue();
        int iT = iToken;
        if (getToken(iT++).tok != Token.semicolon)
          error(ERROR_invalidArgument);
        parameterExpression(iT, -1, null, false);
        int iF = iToken;
        if (tokAt(iF++) != Token.semicolon)
          error(ERROR_invalidArgument);
        parameterExpression(-iF, -1, null, false, 1, false, localVars, localVar);
        int iEnd = iToken;
        if (tokAt(iEnd) != Token.rightparen)
          error(ERROR_invalidArgument);
        v = parameterExpression(TF ? iT : iF, TF ? iF : iEnd, "XXX", false, 1,
            false, localVars, localVar);
        i = iEnd;
        break;
      case Token.forcmd:
      case Token.select:
        boolean isFunctionOfX = (pt > 0);
        boolean isFor = (isFunctionOfX && tok == Token.forcmd);
        // it is important to distinguish between the select command:
        // select {atomExpression} (mathExpression)
        // and the select(dummy;{atomExpression};mathExpression) function:
        // select {*.ca} (phi < select(y; {*.ca}; y.resno = _x.resno + 1).phi)
        String dummy;
        // for(dummy;...
        // select(dummy;...
        if (isFunctionOfX) {
          if (getToken(++i).tok != Token.leftparen
              || getToken(++i).tok != Token.identifier)
            error(ERROR_invalidArgument);
          dummy = parameterAsString(i);
          if (getToken(++i).tok != Token.semicolon)
            error(ERROR_invalidArgument);
        } else {
          dummy = "_x";
        }
        // for(dummy;{atom expr};...
        // select(dummy;{atom expr};...
        v = tokenSetting(-(++i)).value;
        if (!(v instanceof BitSet))
          error(ERROR_invalidArgument);
        BitSet bsAtoms = (BitSet) v;
        i = iToken;
        if (isFunctionOfX && getToken(i++).tok != Token.semicolon)
          error(ERROR_invalidArgument);
        // for(dummy;{atom expr};math expr)
        // select(dummy;{atom expr};math expr)
        // bsX is necessary because there are a few operations that still
        // are there for now that require it; could go, though.
        BitSet bsSelect = new BitSet();
        BitSet bsX = new BitSet();
        String[] sout = (isFor ? new String[BitSetUtil.cardinalityOf(bsAtoms)]
            : null);
        ScriptVariable t = null;
        int atomCount = (isSyntaxCheck ? 0 : viewer.getAtomCount());
        if (localVars == null)
          localVars = new Hashtable();
        bsX.set(0);
        localVars.put(dummy, t = ScriptVariable.getVariableSelected(0, bsX)
            .setName(dummy));
        // one test just to check for errors and get iToken
        int pt2 = -1;
        if (isFunctionOfX) {
          pt2 = i - 1;
          int np = 0;
          int tok2;
          while (np >= 0 && ++pt2 < ptMax) {
            if ((tok2 = tokAt(pt2)) == Token.rightparen)
              np--;
            else if (tok2 == Token.leftparen)
              np++;
          }
        }
        int p = 0;
        int jlast = 0;
        if (BitSetUtil.firstSetBit(bsAtoms) < 0) {
          iToken = pt2 - 1;
        } else {
          for (int j = 0; j < atomCount; j++)
            if (bsAtoms.get(j)) {
              if (jlast >= 0)
                bsX.clear(jlast);
              jlast = j;
              bsX.set(j);
              t.index = j;
              res = parameterExpression(i, pt2, (isFor ? "XXX" : null), isFor, j,
                  false, localVars, isFunctionOfX ? null : dummy);
              if (isFor) {
                if (res == null || ((Vector) res).size() == 0)
                  error(ERROR_invalidArgument);
                sout[p++] = ScriptVariable.sValue((ScriptVariable) ((Vector) res)
                    .elementAt(0));
              } else if (((Boolean) res).booleanValue()) {
                bsSelect.set(j);
              }
            }
        }
        if (isFor) {
          v = sout;
        } else if (isFunctionOfX) {
          v = bsSelect;
        } else {
          return bitsetVariableVector(bsSelect);
        }
        i = iToken + 1;
        break;
      case Token.semicolon: // for (i = 1; i < 3; i=i+1)
        break out;
      case Token.spec_seqcode:
      case Token.integer:
        rpn.addXNum(ScriptVariable.intVariable(theToken.intValue));
        break;
        // these next are for the within() command
      case Token.plane:
        if (tokAt(iToken + 1) == Token.leftparen) {
          if (!rpn.addOp(theToken, true))
            error(ERROR_invalidArgument);
          break;
        }
        rpn.addX(new ScriptVariable(theToken));
        break;
      case Token.atomName:
      case Token.atomType:
      case Token.branch:
      case Token.boundbox:
      case Token.chain:
      case Token.coord:
      case Token.element:
      case Token.group:
      case Token.hkl:
      case Token.model:
      case Token.molecule:
      case Token.site:
      case Token.structure:
        //
      case Token.on:
      case Token.off:
      case Token.string:
      case Token.decimal:
      case Token.point3f:
      case Token.point4f:
      case Token.bitset:
        rpn.addX(new ScriptVariable(theToken));
        break;
      case Token.dollarsign:
        rpn.addX(new ScriptVariable(Token.point3f, centerParameter(i)));
        i = iToken;
        break;
      case Token.leftbrace:
        v = getPointOrPlane(i, false, true, true, false, 3, 4);
        i = iToken;
        break;
      case Token.expressionBegin:
        if (tokAt(i + 1) == Token.all && tokAt(i + 2) == Token.expressionEnd) {
          tok = Token.all;
          iToken += 2;
        }
        // fall through
      case Token.all:
        if (tok == Token.all)
          v = viewer.getModelAtomBitSet(-1, true);
        else
          v = expression(statement, i, 0, true, true, true, true);
        i = iToken;
        if (nParen == 0 && isOneExpressionOnly) {
          iToken++;
          return bitsetVariableVector(v);
        }
        break;
      case Token.expressionEnd:
        i++;
        break out;
      case Token.rightbrace:
        error(ERROR_invalidArgument);
        break;
      case Token.comma: // ignore commas
        if (!rpn.addOp(theToken))
          error(ERROR_invalidArgument);
        break;
      case Token.period:
        ScriptVariable token = getBitsetPropertySelector(i + 1, false);
        if (token == null)
          error(ERROR_invalidArgument);
        // check for added min/max modifier
        boolean isUserFunction = (token.intValue == Token.function);
        boolean allowMathFunc = true;
        int tok2 = tokAt(iToken + 2);
        if (tokAt(iToken + 1) == Token.period) {
          switch (tok2) {
          case Token.all:
            tok2 = Token.minmaxmask;
            if (tokAt(iToken + 3) == Token.period && tokAt(iToken + 4) == Token.bin)
              tok2 = Token.allfloat;
            // fall through
          case Token.min:
          case Token.max:
          case Token.stddev:
          case Token.sum2:
          case Token.average:
            allowMathFunc = (isUserFunction 
                || tok2 == Token.minmaxmask || tok2 == Token.allfloat);
            token.intValue |= tok2;
            getToken(iToken + 2);
          }
        }
        allowMathFunc &= (tokAt(iToken + 1) == Token.leftparen || isUserFunction);
        if (!rpn.addOp(token, allowMathFunc))
          error(ERROR_invalidArgument);
        i = iToken;
        if (token.intValue == Token.function && tokAt(i + 1) != Token.leftparen) {
          rpn.addOp(Token.tokenLeftParen);
          rpn.addOp(Token.tokenRightParen);
        }
        break;
      default:
        if (theTok == Token.identifier
            && viewer.isFunction((String) theToken.value)) {
          if (!rpn.addOp(new ScriptVariable(Token.function, theToken.value))) {
            // iToken--;
            error(ERROR_invalidArgument);
          }
          if (tokAt(i + 1) != Token.leftparen) {
            rpn.addOp(Token.tokenLeftParen);
            rpn.addOp(Token.tokenRightParen);
          }
        } else if (Token.tokAttr(theTok, Token.mathop)
            || Token.tokAttr(theTok, Token.mathfunc)) {
          if (!rpn.addOp(theToken)) {
            if (ptAtom >= 0) {
              // this is expected -- the right parenthesis
              break out;
            }
            error(ERROR_invalidArgument);
          }
          if (theTok == Token.leftparen)
            nParen++;
          else if (theTok == Token.rightparen) {
            if (--nParen == 0 && isOneExpressionOnly) {
              iToken++;
              break out;
            }
          }
        } else {
          String name = parameterAsString(i).toLowerCase(); // necessary?
          if (isSyntaxCheck)
            v = name;
          else if ((localVars == null || (v = localVars.get(name)) == null)
              && (v = getContextVariableAsVariable(name)) == null)
            rpn.addX(viewer.getOrSetNewVariable(name, false));
          break;
        }
      }
      if (v != null)
        rpn.addX(v);
    }
    ScriptVariable result = rpn.getResult(false, key);
    if (result == null) {
      if (!isSyntaxCheck)
        rpn.dumpStacks("null result");
      error(ERROR_endOfStatementUnexpected);
    }
    if (result.tok == Token.vector)
      return result.value;
    if (returnBoolean)
      return Boolean.valueOf(ScriptVariable.bValue(result));
    if (returnString) {
      if (result.tok == Token.string)
        result.intValue = Integer.MAX_VALUE;
      return ScriptVariable.sValue(result);
    }
    switch (result.tok) {
    case Token.on:
    case Token.off:
      return Boolean.valueOf(result.intValue == 1);
    case Token.integer:
      return new Integer(result.intValue);
    case Token.bitset:
    case Token.decimal:
    case Token.string:
    case Token.point3f:
    default:
      return result.value;
    }
  }

  Object bitsetVariableVector(Object v) {
    Vector resx = new Vector();
    if (v instanceof BitSet)
      resx.addElement(new ScriptVariable(Token.bitset, v));
    return resx;
  }

  Object getBitsetIdent(BitSet bs, String label, Object tokenValue,
                        boolean useAtomMap, int index, boolean isExplicitlyAll) {
    boolean isAtoms = !(tokenValue instanceof BondSet);
    if (isAtoms) {
      if (label == null)
        label = viewer.getStandardLabelFormat();
      else if (label.length() == 0)
        label = "%[label]";
    }
    int pt = (label == null ? -1 : label.indexOf("%"));
    boolean haveIndex = (index != Integer.MAX_VALUE);
    if (bs == null || isSyntaxCheck || isAtoms && pt < 0) {
      if (label == null)
        label = "";
      return isExplicitlyAll ? new String[] { label } : (Object) label;
    }
    int len = (haveIndex ? index + 1 : bs.size());
    int nmax = (haveIndex ? 1 : BitSetUtil.cardinalityOf(bs));
    String[] sout = new String[nmax];
    ModelSet modelSet = viewer.getModelSet();
    int n = 0;
    int[] indices = (isAtoms || !useAtomMap ? null : ((BondSet) tokenValue)
        .getAssociatedAtoms());
    if (indices == null && label != null && label.indexOf("%D") > 0)
      indices = viewer.getAtomIndices(bs);
    boolean asIdentity = (label == null || label.length() == 0);
    Hashtable htValues = (isAtoms || asIdentity ? null : LabelToken
        .getBondLabelValues());
    LabelToken[] tokens = (asIdentity ? null : isAtoms ? LabelToken.compile(
        viewer, label, '\0', null) : LabelToken.compile(viewer, label, '\1',
        htValues));
    for (int j = (haveIndex ? index : 0); j < len; j++)
      if (index == j || bs.get(j)) {
        String str;
        if (isAtoms) {
          if (asIdentity)
            str = modelSet.getAtomAt(j).getInfo();
          else
            str = LabelToken.formatLabel(modelSet.getAtomAt(j), null, tokens,
                '\0', indices);
        } else {
          Bond bond = modelSet.getBondAt(j);
          if (asIdentity)
            str = bond.getIdentity();
          else
            str = LabelToken.formatLabel(bond, tokens, htValues, indices);
        }
        str = TextFormat.formatString(str, "#", (n + 1));
        sout[n++] = str;
        if (haveIndex)
          break;
      }
    return nmax == 1 && !isExplicitlyAll ? sout[0] : (Object) sout;
  }

  private ScriptVariable getBitsetPropertySelector(int i, boolean mustBeSettable)
      throws ScriptException {
    int tok = getToken(i).tok;
    String s = null;
    switch (tok) {
    default:
      if (Token.tokAttrOr(tok, Token.atomproperty, Token.mathproperty))
        break;
      return null;
    case Token.min:
    case Token.max:
    case Token.average:
    case Token.stddev:
    case Token.sum2:
    case Token.property:
      break;
    case Token.identifier:
      String name = parameterAsString(i);
      switch (tok = Token.getSettableTokFromString(name)) {
      case Token.atomX:
      case Token.atomY:
      case Token.atomZ:
      case Token.qw:
        break;
      default:
        if (!mustBeSettable && viewer.isFunction(name)) {
          tok = Token.function;
          break;
        }
        return null;
      }
      break;
    }
    if (mustBeSettable && !Token.tokAttr(tok, Token.settable))
      return null;
    if (s == null)
      s = parameterAsString(i).toLowerCase();
    return new ScriptVariable(Token.propselector, tok, s);
  }

  protected Object getBitsetProperty(BitSet bs, int tok, Point3f ptRef,
                                     Point4f planeRef, Object tokenValue,
                                     Object opValue, boolean useAtomMap,
                                     int index) throws ScriptException {
    
    // index is a special argument set in parameterExpression that 
    // indicates we are looking at only one atom within a for(...) loop
    // the bitset cannot be a BondSet in that case
    
    boolean haveIndex = (index != Integer.MAX_VALUE);
    
    boolean isAtoms = haveIndex || !(tokenValue instanceof BondSet);
    // check minmax flags:
    
    int minmaxtype = tok & Token.minmaxmask;
    boolean allFloat = (minmaxtype == Token.allfloat);
    boolean isExplicitlyAll = (minmaxtype == Token.minmaxmask || allFloat);
    tok &= ~Token.minmaxmask;
    if (tok == Token.nada)
      tok = (isAtoms ? Token.atoms : Token.bonds);
    
    // determine property type:
    
    boolean isPt = false;
    boolean isInt = false;
    boolean isString = false;
    switch (tok) {
    case Token.xyz:
    case Token.vibXyz:
    case Token.fracXyz:
    case Token.unitXyz:
    case Token.color:
      isPt = true;
      break;
    case Token.function:
    case Token.distance:
      break;
    default:
      isInt = Token.tokAttr(tok, Token.intproperty)
          && !Token.tokAttr(tok, Token.floatproperty);
      // occupancy and radius considered floats here
      isString = !isInt && Token.tokAttr(tok, Token.strproperty);
      // structure considered int; for the name, use .label("%[structure]")
    }

    // preliminarty checks we only want to do once:
    
    Point3f pt = (isPt || !isAtoms ? new Point3f() : null);
    if (isString || isExplicitlyAll)
      minmaxtype = Token.all;
    Vector vout =  (minmaxtype == Token.all ? new Vector() : null);
    
    BitSet bsNew = null;
    String userFunction = null;
    Vector params = null;
    BitSet bsAtom = null;
    ScriptVariable tokenAtom = null;
    Point3f ptT = null;
    float[] data = null;

    switch (tok) {
    case Token.atoms:
    case Token.bonds:
      if (isSyntaxCheck)
        return bs;
      bsNew = (tok == Token.atoms ? (isAtoms ? bs : viewer.getAtomBits(
          Token.bonds, bs)) : (isAtoms ? new BondSet(viewer.getBondsForSelectedAtoms(bs))
          : bs));
      int i;
      switch (minmaxtype) {
      case Token.min:
        i = BitSetUtil.firstSetBit(bsNew);
        break;
      case Token.max:
        i = BitSetUtil.length(bsNew) - 1;
        break;
      case Token.stddev:
      case Token.sum2:
        return new Float(Float.NaN);
      default:
        return bsNew;        
      }
      bsNew.clear();
      if (i >= 0)
        bsNew.set(i);
      return bsNew;
    case Token.identify:
      switch (minmaxtype) {
      case 0:
      case Token.all:
        return getBitsetIdent(bs, null, tokenValue, useAtomMap, index,
            isExplicitlyAll);
      }
      return "";
    case Token.function:
      userFunction = (String) ((Object[]) opValue)[0];
      params = (Vector) ((Object[]) opValue)[1];
      bsAtom = new BitSet();
      tokenAtom = new ScriptVariable(Token.bitset, bsAtom);
      break;
    case Token.straightness:
    case Token.surfacedistance:
      viewer.autoCalculate(tok);
      break;
    case Token.distance:
      if (ptRef == null && planeRef == null)
        return new Point3f();
     break;
    case Token.color:
      ptT = new Point3f();
      break;
    case Token.property:
      data = viewer.getDataFloat((String) opValue);
      break;
    }

    int n = 0;
    int ivvMinMax = 0;
    int ivMinMax = 0;
    float fvMinMax = 0;
    double sum = 0;
    double sum2 = 0;
    switch (minmaxtype) {
    case Token.min:
      ivMinMax = Integer.MAX_VALUE;
      fvMinMax = Float.MAX_VALUE;
      break;
    case Token.max:
      ivMinMax = Integer.MIN_VALUE;
      fvMinMax = -Float.MAX_VALUE;
      break;
    }
    ModelSet modelSet = viewer.getModelSet();
 
    int count = 0;
    int mode = (isPt ? 3 : isString ? 2 : isInt ? 1 : 0);
    if (isAtoms) {
      int iModel = -1;
      int nOps = 0;
      count = (isSyntaxCheck ? 0 : viewer.getAtomCount());
      for (int i = (haveIndex ? index : 0); i < count; i++) {
        if (!haveIndex && bs != null && !bs.get(i))
          continue;
        n++;
        Atom atom = modelSet.getAtomAt(i);
        switch (mode) {
        case 0: // float
          float fv = Float.MAX_VALUE;
          switch (tok) {
          case Token.function:
            bsAtom.set(i);
            fv = ScriptVariable.fValue((Token) getFunctionReturn(userFunction,
                params, tokenAtom));
            bsAtom.clear(i);
            break;
          case Token.property:
            fv = (data == null ? 0 : data[i]);
            break;
          case Token.distance:
            if (planeRef != null)
              fv = Measure.distanceToPlane(planeRef, atom);
            else
              fv = atom.distance(ptRef);
            break;
          default:
            fv = Atom.atomPropertyFloat(atom, tok);
          }
          if (fv == Float.MAX_VALUE || Float.isNaN(fv)
              && minmaxtype != Token.all) {
            n--; // don't count this one
            continue;
          }
          switch (minmaxtype) {
          case Token.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case Token.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case Token.all:
            vout.add(new Float(fv));
            break;
          case Token.sum2:
          case Token.stddev:
            sum2 += ((double) fv) * fv;
            // fall through
          default:
            sum += fv;
          }
          break;
        case 1: // isInt
          int iv = 0;
          switch (tok) {
          case Token.symop:
            // a little weird:
            // First we determine how many operations we have in this model.
            // Then we get the symmetry bitset, which shows the assignments
            // of symmetry for this atom.
            if (atom.getModelIndex() != iModel) {
              iModel = atom.getModelIndex();
              nOps = modelSet.getModelSymmetryCount(iModel);
            }
            BitSet bsSym = atom.getAtomSymmetry();
            int len = nOps;
            int p = 0;
            switch (minmaxtype) {
            case Token.min:
              ivvMinMax = Integer.MAX_VALUE;
              break;
            case Token.max:
              ivvMinMax = Integer.MIN_VALUE;
              break;
            }
            for (int k = 0; k < len; k++)
              if (bsSym.get(k)) {
                iv += k + 1;
                switch (minmaxtype) {
                case Token.min:
                  ivvMinMax = Math.min(ivvMinMax, k + 1);
                  break;
                case Token.max:
                  ivvMinMax = Math.max(ivvMinMax, k + 1);
                  break;
                }
                p++;
              }
            switch (minmaxtype) {
            case Token.min:
            case Token.max:
              iv = ivvMinMax;
            }
            n += p - 1;
            break;
          case Token.cell:
            error(ERROR_unrecognizedAtomProperty, Token.nameOf(tok));
          default:
            iv = Atom.atomPropertyInt(atom, tok);
          }
          switch (minmaxtype) {
          case Token.min:
            if (iv < ivMinMax)
              ivMinMax = iv;
            break;
          case Token.max:
            if (iv > ivMinMax)
              ivMinMax = iv;
            break;
          case Token.all:
            vout.add(new Integer(iv));
            break;
          case Token.sum2:
          case Token.stddev:
            sum2 += ((double) iv) * iv;
            // fall through
          default:
            sum += iv;
          }
          break;
        case 2: // isString
          vout.add(Atom.atomPropertyString(atom, tok));
          break;
        case 3: // isPt
          Tuple3f t = Atom.atomPropertyTuple(atom, tok);
          if (t == null)
            error(ERROR_unrecognizedAtomProperty, Token.nameOf(tok));
          pt.add(t);
          if (minmaxtype == Token.all) {
            vout.add(new Point3f(pt));
            pt.set(0, 0, 0);
          }
          break;
        }
        if (haveIndex)
          break;
      }
    } else { // bonds
      count = viewer.getBondCount();
      for (int i = 0; i < count; i++) {
        if (bs != null && !bs.get(i))
          continue;
        n++;
        Bond bond = modelSet.getBondAt(i);
        switch (tok) {
        case Token.length:
          float fv = bond.getAtom1().distance(bond.getAtom2());
          switch (minmaxtype) {
          case Token.min:
            if (fv < fvMinMax)
              fvMinMax = fv;
            break;
          case Token.max:
            if (fv > fvMinMax)
              fvMinMax = fv;
            break;
          case Token.all:
            vout.add(new Float(fv));
            break;
          case Token.sum2:
          case Token.stddev:
            sum2 += (double) fv * fv;
            // fall through
          default:
            sum += fv;
          }
          break;
        case Token.xyz:
          switch (minmaxtype) {
          case Token.all:
            pt.set(bond.getAtom1());
            pt.add(bond.getAtom2());
            pt.scale(0.5f);
            vout.add(new Point3f(pt));
            break;
          default:
            pt.add(bond.getAtom1());
            pt.add(bond.getAtom2());
            n++;
          }
          break;
        case Token.color:
          Graphics3D.colorPointFromInt(viewer.getColixArgb(bond.getColix()),
              ptT);
          switch (minmaxtype) {
          case Token.all:
            vout.add(new Point3f(ptT));
            break;
          default:
            pt.add(ptT);
          }
          break;
        default:
          error(ERROR_unrecognizedBondProperty, Token.nameOf(tok));
        }
      }
    }
    if (minmaxtype == Token.all) {
      int len = vout.size();
      if (isString && !isExplicitlyAll && len == 1)
        return vout.get(0);
      if (tok == Token.sequence) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < len; i++)
          sb.append((String) vout.get(i));
        return sb.toString();
      }
      if (allFloat) {
        Float[] fout = new Float[len];
        Point3f zero = (len > 0 && isPt ? new Point3f() : null);
        for (int i = len; --i >= 0;) {
          Object v = vout.get(i);
          switch (mode) {
          case 0: 
            fout[i] = (Float) v;
            break;
          case 1:
            fout[i] = new Float(((Integer) v).floatValue());
            break;
          case 2:
            fout[i] = new Float(Parser.parseFloat((String)v));
            break;
          case 3:
            fout[i] = new Float(((Point3f) v).distance(zero));
            break;
          }
        }
        return fout;
      }
      String[] sout = new String[len];
      for (int i = len; --i >= 0;) {
        Object v = vout.get(i);
        if (v instanceof Point3f)
          sout[i] = Escape.escape((Point3f) v);
        else
          sout[i] = "" + vout.get(i);
      }
      return sout;
    }
    if (isPt)
      return (n == 0 ? pt : new Point3f(pt.x / n, pt.y / n, pt.z / n));
    if (n == 0 || n == 1 && minmaxtype == Token.stddev)
      return new Float(Float.NaN);
    if (isInt) {
      switch (minmaxtype) {
      case Token.min:
      case Token.max:
        return new Integer(ivMinMax);
      }
    }
    switch (minmaxtype) {
    case Token.min:
    case Token.max:
      sum = fvMinMax;
      break;
    case Token.sum2:
      sum = sum2;
      break;
    case Token.stddev:
      // because SUM (x_i - X_av)^2 = SUM(x_i^2) - 2X_av SUM(x_i) + SUM(X_av^2)
      // = SUM(x_i^2) - 2nX_av^2 + nX_av^2
      // = SUM(x_i^2) - nX_av^2
      // = SUM(x_i^2) - [SUM(x_i)]^2 / n
      sum = Math.sqrt((sum2 - sum * sum / n) / (n - 1));
      break;
    default:
      sum /= n;
      break;
    }
    return new Float(sum);
  }

  private void setBitsetProperty(BitSet bs, int tok, int iValue, float fValue,
                                 Token tokenValue) throws ScriptException {
    if (isSyntaxCheck || BitSetUtil.cardinalityOf(bs) == 0)
      return;
    String[] list = null;
    String sValue = null;
    float[] fvalues = null;
    int nValues;
    switch (tok) {
    case Token.xyz:
    case Token.fracXyz:
    case Token.vibXyz:
      if (tokenValue.tok == Token.point3f) {
        viewer.setAtomCoord(bs, tok, tokenValue.value);
      } else if (tokenValue.tok == Token.list) {
        list = (String[]) tokenValue.value;
        if ((nValues = list.length) == 0)
          return;
        Point3f[] values = new Point3f[nValues];
        for (int i = nValues; --i >= 0;) {
          Object o = Escape.unescapePoint(list[i]);
          if (!(o instanceof Point3f))
            error(ERROR_unrecognizedParameter, "ARRAY", list[i]);
          values[i] = (Point3f) o;
        }
        viewer.setAtomCoord(bs, tok, values);
      }
      return;
    case Token.color:
      if (tokenValue.tok == Token.point3f)
        iValue = colorPtToInt((Point3f) tokenValue.value);
      else if (tokenValue.tok == Token.list) {
        list = (String[]) tokenValue.value;
        if ((nValues = list.length) == 0)
          return;
        int[] values = new int[nValues];
        for (int i = nValues; --i >= 0;) {
          Object pt = Escape.unescapePoint(list[i]);
          if (pt instanceof Point3f)
            values[i] = colorPtToInt((Point3f) pt);
          else
            values[i] = Graphics3D.getArgbFromString(list[i]);
          if (values[i] == 0
              && (values[i] = Parser.parseInt(list[i])) == Integer.MIN_VALUE)
            error(ERROR_unrecognizedParameter, "ARRAY", list[i]);
        }
        viewer.setShapeProperty(JmolConstants.SHAPE_BALLS, "colorValues",
            values, bs);
        return;
      }
      viewer.setShapeProperty(JmolConstants.SHAPE_BALLS, "color",
          tokenValue.tok == Token.string ? tokenValue.value : new Integer(
              iValue), bs);
      return;
    case Token.label:
    case Token.format:
      if (tokenValue.tok == Token.list)
        list = (String[]) tokenValue.value;
      else
        sValue = ScriptVariable.sValue(tokenValue);
      viewer.setAtomProperty(bs, tok, iValue, fValue, sValue, fvalues, list);
      return;
    case Token.element:
    case Token.elemno:
      clearDefinedVariableAtomSets();
      break;
    }
    if (tokenValue.tok == Token.list || tokenValue.tok == Token.string) {
      list = (tokenValue.tok == Token.list ? (String[]) tokenValue.value
          : Parser.getTokens(ScriptVariable.sValue(tokenValue)));
      if ((nValues = list.length) == 0)
        return;
      fvalues = new float[nValues];
      for (int i = nValues; --i >= 0;)
        fvalues[i] = (tok == Token.element ? JmolConstants
            .elementNumberFromSymbol(list[i]) : Parser.parseFloat(list[i]));
      if (tokenValue.tok == Token.string && nValues == 1) {
        fValue = fvalues[0];
        iValue = (int) fValue;
        sValue = list[0];
        list = null;
        fvalues = null;
      }
    }
    viewer.setAtomProperty(bs, tok, iValue, fValue, sValue, fvalues, list);
  }

  /////////////////////// general fields //////////////////////
  
  private final static int scriptLevelMax = 10;

  private Thread currentThread;
  protected Viewer viewer;
  protected ScriptCompiler compiler;
  private Hashtable definedAtomSets;
  private StringBuffer outputBuffer;
  private ScriptContext[] stack = new ScriptContext[scriptLevelMax];

  private String contextPath = "";
  private String filename;
  private String functionName;
  private boolean isStateScript;
  private int scriptLevel;
  private int scriptReportingLevel = 0;
  private int commandHistoryLevelMax = 0;

  // created by Compiler:
  private Token[][] aatoken;
  private short[] lineNumbers;
  private int[][] lineIndices;
  private Hashtable contextVariables;
  private String script;

  String getScript() {
    return script;
  }

  // specific to current statement
  protected int pc; // program counter
  private String thisCommand;
  private String fullCommand;
  private Token[] statement;
  private int statementLength;
  private int iToken;
  private int lineEnd;
  private int pcEnd;
  private String scriptExtensions;

  String getState() {
    return getFunctionCalls("");
  }

  //////////////////////// supporting methods for compilation and loading //////////

  private boolean compileScript(String filename, String strScript,
                                boolean debugCompiler) {
    this.filename = filename;
    getScriptContext(compiler.compile(filename, strScript, false, false,
        debugCompiler, false), false);
    isStateScript = (script.indexOf(Viewer.STATE_VERSION_STAMP) >= 0);
    String s = script;
    pc = setScriptExtensions();
    if (!isSyntaxCheck && viewer.isScriptEditorVisible()
        && strScript.indexOf(JmolConstants.SCRIPT_EDITOR_IGNORE) < 0)
      viewer.scriptStatus("");
    script = s;
    return !error;
  }

  private int setScriptExtensions() {
    String extensions = scriptExtensions;
    if (extensions == null)
      return 0;
    int pt = extensions.indexOf("##SCRIPT_STEP");
    if (pt >= 0) {
      executionStepping = true;
    }
    pt = extensions.indexOf("##SCRIPT_START=");
    if (pt < 0)
      return 0;
    pt = Parser.parseInt(extensions.substring(pt + 15));
    if (pt == Integer.MIN_VALUE)
      return 0;
    for (pc = 0; pc < lineIndices.length; pc++) {
      if (lineIndices[pc][0] > pt || lineIndices[pc][1] >= pt)
        break;
    }
    if (pc > 0 && pc < lineIndices.length && lineIndices[pc][0] > pt)
      --pc;
    return pc;
  }

  private void runScript(String script) throws ScriptException {
    runScript(script, outputBuffer);
  }

  private boolean compileScriptFileInternal(String filename) {
    // from "script" command, with push/pop surrounding or viewer
    if (filename.toLowerCase().indexOf("javascript:") == 0)
      return compileScript(filename, viewer.jsEval(filename.substring(11)),
          debugScript);
    String[] data = new String[2];
    data[0] = filename;
    if (!viewer.getFileAsString(data, Integer.MAX_VALUE, false)) {
      setErrorMessage("io error reading " + data[0] + ": " + data[1]);
      return false;
    }
    this.filename = filename;
    return compileScript(filename, data[1], debugScript);
  }


  /////////////// Jmol parameter / user variable / function support ///////////////
  
  private Object getParameter(String key, boolean asToken) {
    Object v = getContextVariableAsVariable(key);
    if (v == null)
      v = viewer.getParameter(key);
    if (asToken)
      return (v instanceof ScriptVariable ? (ScriptVariable) v : ScriptVariable
          .getVariable(v));
    return (v instanceof ScriptVariable ? ScriptVariable
        .oValue((ScriptVariable) v) : v);
  }

  private String getParameterEscaped(String var) {
    ScriptVariable v = getContextVariableAsVariable(var);
    return (v == null ? "" + viewer.getParameterEscaped(var) : Escape
        .escape(v.value));
  }

  private String getStringParameter(String var, boolean orReturnName) {
    ScriptVariable v = getContextVariableAsVariable(var);
    if (v != null)
      return ScriptVariable.sValue(v);
    String val = "" + viewer.getParameter(var);
    return (val.length() == 0 && orReturnName ? var : val);
  }

  private Object getNumericParameter(String var) {
    if (var.equalsIgnoreCase("_modelNumber")) {
      int modelIndex = viewer.getCurrentModelIndex();
      return new Integer(modelIndex < 0 ? 0 : viewer
          .getModelFileNumber(modelIndex));
    }
    ScriptVariable v = getContextVariableAsVariable(var);
    if (v == null) {
      Object val = viewer.getParameter(var);
      if (!(val instanceof String))
        return val;
      v = new ScriptVariable(Token.string, val);
    }
    return ScriptVariable.nValue(v);
  }

  private ScriptVariable getContextVariableAsVariable(String var) {
    if (var.equals("expressionBegin"))
      return null;
    var = var.toLowerCase();
    if (contextVariables != null && contextVariables.containsKey(var))
      return (ScriptVariable) contextVariables.get(var);
    for (int i = scriptLevel; --i >= 0;)
      if (stack[i].contextVariables != null
          && stack[i].contextVariables.containsKey(var))
        return (ScriptVariable) stack[i].contextVariables.get(var);
    return null;
  }

  private Object getStringObjectAsVariable(String s, String key) {
    if (s == null || s.length() == 0)
      return s;
    Object v = ScriptVariable.unescapePointOrBitsetAsVariable(s);
    if (v instanceof String && key != null)
      v = viewer.setUserVariable(key, new ScriptVariable(Token.string, (String) v));
    return v;
  }

  private boolean loadFunction(String name, Vector params) {
    ScriptFunction function = viewer.getFunction(name);
    if (function == null)
      return false;
    aatoken = function.aatoken;
    lineNumbers = function.lineNumbers;
    lineIndices = function.lineIndices;
    script = function.script;
    pc = 0;
    if (function.names != null) {
      contextVariables = new Hashtable();
      function.setVariables(contextVariables, params);
    }
    functionName = name;
    return true;
  }

  protected ScriptVariable getFunctionReturn(String name, Vector params,
                                   ScriptVariable tokenAtom)
      throws ScriptException {
    pushContext(null);
    contextPath += " >> function " + name;
    loadFunction(name, params);
    if (tokenAtom != null)
      contextVariables.put("_x", tokenAtom);
    instructionDispatchLoop(false);
    ScriptVariable v = getContextVariableAsVariable("_retval");
    popContext();
    return v;
  }

  private void clearDefinedVariableAtomSets() {
    definedAtomSets.remove("# variable");
  }

  /**
   *  support for @xxx or define xxx commands
   * 
   */
  private void defineSets() {
    if (!definedAtomSets.containsKey("# static")) {
      for (int i = 0; i < JmolConstants.predefinedStatic.length; i++)
        defineAtomSet(JmolConstants.predefinedStatic[i]);
      defineAtomSet("# static");
    }
    if (definedAtomSets.containsKey("# variable"))
      return;
    for (int i = 0; i < JmolConstants.predefinedVariable.length; i++)
      defineAtomSet(JmolConstants.predefinedVariable[i]);
    // Now, define all the elements as predefined sets
    // hydrogen is handled specially, so don't define it

    int firstIsotope = JmolConstants.firstIsotope;
    // name ==> e_=n for all standard elements
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      String definition = "@" + JmolConstants.elementNameFromNumber(i) + " _e="
          + i;
      defineAtomSet(definition);
    }
    // _Xx ==> name for of all elements, isotope-blind
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      String definition = "@_" + JmolConstants.elementSymbolFromNumber(i) + " "
          + JmolConstants.elementNameFromNumber(i);
      defineAtomSet(definition);
    }
    // name ==> _e=nn for each alternative element
    for (int i = firstIsotope; --i >= 0;) {
      String definition = "@" + JmolConstants.altElementNameFromIndex(i)
          + " _e=" + JmolConstants.altElementNumberFromIndex(i);
      defineAtomSet(definition);
    }
    // these variables _e, _x can't be more than two characters
    // name ==> _isotope=iinn for each isotope
    // _T ==> _isotope=iinn for each isotope
    // _3H ==> _isotope=iinn for each isotope
    for (int i = JmolConstants.altElementMax; --i >= firstIsotope;) {
      String def = " element=" + JmolConstants.altElementNumberFromIndex(i);
      String definition = "@_" + JmolConstants.altElementSymbolFromIndex(i);
      defineAtomSet(definition + def);
      definition = "@_" + JmolConstants.altIsotopeSymbolFromIndex(i);
      defineAtomSet(definition + def);
      definition = "@" + JmolConstants.altElementNameFromIndex(i);
      if (definition.length() > 1)
        defineAtomSet(definition + def);
    }
    defineAtomSet("# variable");
  }

  private void defineAtomSet(String script) {
    if (script.indexOf("#") == 0) {
      definedAtomSets.put(script, Boolean.TRUE);
      return;
    }
    ScriptContext sc = compiler.compile("#predefine", script, true, false, false, false);
    if (sc.errorType != null) {
        viewer
           .scriptStatus("JmolConstants.java ERROR: predefined set compile error:"
              + script
              + "\ncompile error:"
              + sc.errorMessageUntranslated);
      return;
    }

    if (sc.aatoken.length != 1) {
      viewer
          .scriptStatus("JmolConstants.java ERROR: predefinition does not have exactly 1 command:"
              + script);
      return;
    }
    Token[] statement = sc.aatoken[0];
    if (statement.length <= 2) {
      viewer.scriptStatus("JmolConstants.java ERROR: bad predefinition length:"
          + script);
      return;
    }
    int tok = statement[iToken = 1].tok;
    if (tok != Token.identifier && !Token.tokAttr(tok, Token.predefinedset)) {
      viewer.scriptStatus("JmolConstants.java ERROR: invalid variable name:"
          + script);
      return;
    }
    definedAtomSets.put(statement[1].value, statement);
  }

  private BitSet lookupIdentifierValue(String identifier)
      throws ScriptException {
    // all variables and possible residue names for PDB
    // or atom names for non-pdb atoms are processed here.

    // priority is given to a defined variable.

    BitSet bs = lookupValue(identifier, false);
    if (bs != null)
      return BitSetUtil.copy(bs);

    // next we look for names of groups (PDB) or atoms (non-PDB)
    bs = getAtomBits(Token.identifier, identifier);
    return (bs == null ? new BitSet() : bs);
  }

  private BitSet lookupValue(String setName, boolean plurals)
      throws ScriptException {
    if (isSyntaxCheck) {
      return new BitSet();
    }
    defineSets();
    Object value = definedAtomSets.get(setName);
    boolean isDynamic = false;
    if (value == null) {
      value = definedAtomSets.get("!" + setName);
      isDynamic = (value != null);
    }
    if (value instanceof BitSet)
      return (BitSet) value;
    if (value instanceof Token[]) {
      pushContext(null);
      BitSet bs = expression((Token[]) value, -2, 0, true, false, true, true);
      popContext();
      if (!isDynamic)
        definedAtomSets.put(setName, bs);
      return bs;
    }
    if (plurals)
      return null;
    int len = setName.length();
    if (len < 5) // iron is the shortest
      return null;
    if (setName.charAt(len - 1) != 's')
      return null;
    if (setName.endsWith("ies"))
      setName = setName.substring(0, len - 3) + 'y';
    else
      setName = setName.substring(0, len - 1);
    return lookupValue(setName, true);
  }

  void deleteAtomsInVariables(BitSet bsDeleted) {
    Enumeration e = definedAtomSets.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      Object value = definedAtomSets.get(key);
      if (value instanceof BitSet)
        BitSetUtil.deleteBits((BitSet) value, bsDeleted);
    }
  }

  /**
   * provides support for @x and @{....} in statements.
   * The compiler passes on these, because they must be integrated
   * with the statement dynamically.
   * 
   * @param pc
   * @return     a fixed token set -- with possible overrun of unused null tokens
   * 
   * @throws ScriptException
   */
  private boolean setStatement(int pc) throws ScriptException {
    statement = aatoken[pc];
    statementLength = statement.length;
    if (statementLength == 0)
      return true;
    Token[] fixed;
    int i;
    int tok;
    for (i = 1; i < statementLength; i++)
      if (statement[i].tok == Token.define)
        break;
    if (i == statementLength)// || isScriptCheck)
      return i == statementLength;
    fixed = new Token[statementLength];
    fixed[0] = statement[0];
    boolean isExpression = false;
    int j = 1;
    for (i = 1; i < statementLength; i++) {
      switch (tok = statement[i].tok) {
      case Token.define:
        Object v;
        // Object var_set;
        String s;
        String var = parameterAsString(++i);
        boolean isClauseDefine = (tokAt(i) == Token.expressionBegin);
        if (isClauseDefine) {
          Vector val = (Vector) parameterExpression(++i, 0, "_var", true);
          if (val == null || val.size() == 0)
            error(ERROR_invalidArgument);
          i = iToken;
          ScriptVariable vt = (ScriptVariable) val.elementAt(0);
          v = (vt.tok == Token.list ? vt : ScriptVariable.oValue(vt));
        } else {
          v = getParameter(var, false);
        }
        tok = tokAt(0);
        boolean forceString = (Token.tokAttr(tok, Token.implicitStringCommand) 
            || tok == Token.load || tok == Token.script); // for the file names
        if (v instanceof ScriptVariable) {
          fixed[j] = (Token) v;
          if (isExpression && fixed[j].tok == Token.list)
            fixed[j] = new ScriptVariable(Token.bitset, getAtomBitSet(this,
                ScriptVariable.sValue((ScriptVariable) fixed[j])));
        } else if (v instanceof Boolean) {
          fixed[j] = (((Boolean) v).booleanValue() ? Token.tokenOn
              : Token.tokenOff);
        } else if (v instanceof Integer) {
          // if (isExpression && !isClauseDefine
          // && (var_set = getParameter(var + "_set", false)) != null)
          // fixed[j] = new Token(Token.define, "" + var_set);
          // else
          fixed[j] = new Token(Token.integer, ((Integer) v).intValue(), v);

        } else if (v instanceof Float) {
          fixed[j] = new Token(Token.decimal, JmolConstants.modelValue("" + v),
              v);
        } else if (v instanceof String) {
          v = getStringObjectAsVariable((String) v, null);
          if (v instanceof ScriptVariable) {
            fixed[j] = (Token) v;
          } else {
            s = (String) v;
            if (isExpression) {
              fixed[j] = new Token(Token.bitset, getAtomBitSet(this, s));
            } else {
              // bit of a hack here....
              // identifiers cannot have periods; file names can, though
              // TODO: this is still a hack
              // what we really need to know is what the compiler
              // expects here -- a string or an identifier, because
              // they will be processed differently.
              // a filename with only letters and numbers will be 
              // read incorrectly here as an identifier.
              tok = (isClauseDefine || forceString 
                  || s.indexOf(".") >= 0
                  || s.indexOf(" ") >= 0 || s.indexOf("=") >= 0
                  || s.indexOf(";") >= 0 || s.indexOf("[") >= 0
                  || s.indexOf("{") >= 0 ? Token.string : Token.identifier);
              fixed[j] = new Token(tok, v);
            }
          }
        } else if (v instanceof BitSet) {
          fixed[j] = new Token(Token.bitset, v);
        } else if (v instanceof Point3f) {
          fixed[j] = new Token(Token.point3f, v);
        } else if (v instanceof Point4f) {
          fixed[j] = new Token(Token.point4f, v);
        } else if (v instanceof String[]) {
          fixed[j] = new Token(Token.string, Escape.escape((String[])v));
        } else {
          Point3f center = getObjectCenter(var, Integer.MIN_VALUE);
          if (center == null) 
            error(ERROR_invalidArgument);
          fixed[j] = new Token(Token.point3f, center);
        }
        if (j == 1 && statement[0].tok == Token.set
            && fixed[j].tok != Token.identifier)
          error(ERROR_invalidArgument);
        break;
      case Token.expressionBegin:
      case Token.expressionEnd:
        // @ in expression will be taken as SELECT
        isExpression = (tok == Token.expressionBegin);
        fixed[j] = statement[i];
        break;
      default:
        fixed[j] = statement[i];
      }

      j++;
    }
    statement = fixed;
    for (i = j; i < statement.length; i++)
      statement[i] = null;
    statementLength = j;
    return true;
  }

  /////////////////// Script context support //////////////////////

  private void clearState(boolean tQuiet) {
    for (int i = scriptLevelMax; --i >= 0;)
      stack[i] = null;
    scriptLevel = 0;
    setErrorMessage(null);
    contextPath = "";
    this.tQuiet = tQuiet;
  }

  private void pushContext(ScriptFunction function) throws ScriptException {
    if (scriptLevel == scriptLevelMax)
      error(ERROR_tooManyScriptLevels);
    ScriptContext context = getScriptContext();
    stack[scriptLevel++] = context;
    if (isCmdLine_c_or_C_Option)
      Logger.info("-->>-------------".substring(0, scriptLevel + 5) + filename);
  }

  ScriptContext getScriptContext() {
    ScriptContext context = new ScriptContext();
    context.contextPath = contextPath;
    context.filename = filename;
    context.functionName = functionName;
    context.script = script;
    context.lineNumbers = lineNumbers;
    context.lineIndices = lineIndices;
    context.aatoken = aatoken;    
    context.statement = statement;
    context.statementLength = statementLength;
    context.pc = pc;
    context.lineEnd = lineEnd;
    context.pcEnd = pcEnd;
    context.iToken = iToken;
    context.outputBuffer = outputBuffer;
    context.contextVariables = contextVariables;
    context.isStateScript = isStateScript;
    
    context.errorMessage = errorMessage;
    context.errorType = errorType;
    context.iCommandError = iCommandError;

    context.stack = stack;
    context.scriptLevel = scriptLevel;
    context.isSyntaxCheck = isSyntaxCheck;
    context.executionStepping = executionStepping;
    context.executionPaused = executionPaused;
    context.scriptExtensions = scriptExtensions;
    return context;
  }

  private void getScriptContext(ScriptContext context, boolean isFull) { 

    // just from the compiler:
    
    script = context.script;
    lineNumbers = context.lineNumbers;
    lineIndices = context.lineIndices;
    aatoken = context.aatoken;
    contextVariables = context.contextVariables;
    scriptExtensions = context.scriptExtensions;
    if (!isFull) {
      error = (context.errorType != null);
      errorMessage = context.errorMessage;
      errorMessageUntranslated = context.errorMessageUntranslated;
      iCommandError = context.iCommandError;
      errorType = context.errorType;
      return;
    }
    
    contextPath = context.contextPath;
    filename = context.filename;
    functionName = context.functionName;
    statement = context.statement;
    statementLength = context.statementLength;
    pc = context.pc;
    lineEnd = context.lineEnd;
    pcEnd = context.pcEnd;
    iToken = context.iToken;
    outputBuffer = context.outputBuffer;
    isStateScript = context.isStateScript;
  }
  
  private void popContext() {
    if (isCmdLine_c_or_C_Option)
      Logger.info("--<<-------------".substring(0, scriptLevel + 5) + filename);
    if (scriptLevel == 0)
      return;
    ScriptContext context = stack[--scriptLevel];
    stack[scriptLevel] = null;
    getScriptContext(context, true);
  }

  private String getContext(boolean withVariables) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < scriptLevel; i++) {
      if (withVariables) {
        if (stack[i].contextVariables != null) {
          sb.append(getScriptID(stack[i]));
          sb.append(StateManager.getVariableList(stack[i].contextVariables, 80));
        }
      } else {
        sb.append(setErrorLineMessage(stack[i].functionName, stack[i].filename,
            getLinenumber(stack[i]), stack[i].pc,
            statementAsString(stack[i].statement, -9999)));
      }
    }
    if (withVariables) {
      if (contextVariables != null) {
        sb.append(getScriptID(null));
        sb.append(StateManager.getVariableList(contextVariables, 80));
      }
    } else {
      sb.append(setErrorLineMessage(functionName, filename,
      getLinenumber(null), pc, statementAsString(statement, -9999)));
    }

    return sb.toString();
  }

  private int getLinenumber(ScriptContext c) {
    return (c == null ? lineNumbers[pc] : c.lineNumbers[c.pc]);
  }

  private String getScriptID(ScriptContext context) {
    String fuName = (context == null ? functionName : "function " + context.functionName);
    String fiName = (context == null ? filename : context.filename);    
    return "\n# " + fuName + " (file " + fiName + ")\n";
  }

  ///////////////// error message support /////////////////
  
  private boolean error;
  private String errorMessage;
  protected String errorMessageUntranslated;
  protected String errorType;
  protected int iCommandError;
  


  String getErrorMessage() {
    return errorMessage;
  }

  String getErrorMessageUntranslated() {
    return errorMessageUntranslated == null ? errorMessage
        : errorMessageUntranslated;
  }

  private void setErrorMessage(String err) {
    errorMessageUntranslated = null;
    if (err == null) {
      error = false;
      errorType = null;
      errorMessage = null;
      iCommandError = -1;
      return;
    }
    error = true;
    if (errorMessage == null) // there could be a compiler error from a script
                              // command
      errorMessage = GT.translate("script ERROR: ");
    errorMessage += err;
  }

  private boolean ignoreError;

  private void planeExpected() throws ScriptException {
    error(ERROR_planeExpected, "{a b c d}",
        "\"xy\" \"xz\" \"yz\" \"x=...\" \"y=...\" \"z=...\"", "$xxxxx");
  }

  private void integerOutOfRange(int min, int max) throws ScriptException {
    error(ERROR_integerOutOfRange, "" + min, "" + max);
  }

  private void numberOutOfRange(float min, float max) throws ScriptException {
    error(ERROR_numberOutOfRange, "" + min, "" + max);
  }

  void error(int iError) throws ScriptException {
    error(iError, null, null, null, false);
  }

  void error(int iError, String value) throws ScriptException {
    error(iError, value, null, null, false);
  }

  void error(int iError, String value, String more) throws ScriptException {
    error(iError, value, more, null, false);
  }

  void error(int iError, String value, String more, String more2)
      throws ScriptException {
    error(iError, value, more, more2, false);
  }

  private void warning(int iError, String value, String more)
      throws ScriptException {
    error(iError, value, more, null, true);
  }

  void error(int iError, String value, String more, String more2,
             boolean warningOnly) throws ScriptException {
    String strError = ignoreError ? null : errorString(iError, value, more,
        more2, true);
    String strUntranslated = (!ignoreError && GT.getDoTranslate() ? errorString(
        iError, value, more, more2, false)
        : null);
    if (!warningOnly)
      evalError(strError, strUntranslated);
    showString(strError);
  }

  void evalError(String message, String strUntranslated) throws ScriptException {
    if (ignoreError)
      throw new NullPointerException();
    if (!isSyntaxCheck) {
      // String s = viewer.getSetHistory(1);
      // viewer.addCommand(s + CommandHistory.ERROR_FLAG);
      viewer.setCursor(Viewer.CURSOR_DEFAULT);
      viewer.setRefreshing(true);
    }
    throw new ScriptException(message, strUntranslated);
  }

  final static int ERROR_axisExpected = 0;
  final static int ERROR_backgroundModelError = 1;
  final static int ERROR_badArgumentCount = 2;
  final static int ERROR_badMillerIndices = 3;
  final static int ERROR_badRGBColor = 4;
  final static int ERROR_booleanExpected = 5;
  final static int ERROR_booleanOrNumberExpected = 6;
  final static int ERROR_booleanOrWhateverExpected = 7;
  final static int ERROR_colorExpected = 8;
  final static int ERROR_colorOrPaletteRequired = 9;
  final static int ERROR_commandExpected = 10;
  final static int ERROR_coordinateOrNameOrExpressionRequired = 11;
  final static int ERROR_drawObjectNotDefined = 12;
  final static int ERROR_endOfStatementUnexpected = 13;
  final static int ERROR_expressionExpected = 14;
  final static int ERROR_expressionOrIntegerExpected = 15;
  final static int ERROR_filenameExpected = 16;
  final static int ERROR_fileNotFoundException = 17;
  final static int ERROR_incompatibleArguments = 18;
  final static int ERROR_insufficientArguments = 19;
  final static int ERROR_integerExpected = 20;
  final static int ERROR_integerOutOfRange = 21;
  final static int ERROR_invalidArgument = 22;
  final static int ERROR_invalidParameterOrder = 23;
  final static int ERROR_keywordExpected = 24;
  final static int ERROR_moCoefficients = 25;
  final static int ERROR_moIndex = 26;
  final static int ERROR_moModelError = 27;
  final static int ERROR_moOccupancy = 28;
  final static int ERROR_moOnlyOne = 29;
  final static int ERROR_multipleModelsNotOK = 30;
  final static int ERROR_noData = 31;
  final static int ERROR_noPartialCharges = 32;
  final static int ERROR_noUnitCell = 33;
  final static int ERROR_numberExpected = 34;
  final static int ERROR_numberMustBe = 35;
  final static int ERROR_numberOutOfRange = 36;
  final static int ERROR_objectNameExpected = 37;
  final static int ERROR_planeExpected = 38;
  final static int ERROR_propertyNameExpected = 39;
  final static int ERROR_spaceGroupNotFound = 40;
  final static int ERROR_stringExpected = 41;
  final static int ERROR_stringOrIdentifierExpected = 42;
  final static int ERROR_tooManyPoints = 43;
  final static int ERROR_tooManyScriptLevels = 44;
  final static int ERROR_unrecognizedAtomProperty = 45;
  final static int ERROR_unrecognizedBondProperty = 46;
  final static int ERROR_unrecognizedCommand = 47;
  final static int ERROR_unrecognizedExpression = 48;
  final static int ERROR_unrecognizedObject = 49;
  final static int ERROR_unrecognizedParameter = 50;
  final static int ERROR_unrecognizedParameterWarning = 51;
  final static int ERROR_unrecognizedShowParameter = 52;
  final static int ERROR_what = 53;
  final static int ERROR_writeWhat = 54;

  static String errorString(int iError, String value, String more,
                            String more2, boolean translated) {
    boolean doTranslate = false;
    if (!translated && (doTranslate = GT.getDoTranslate()) == true)
      GT.setDoTranslate(false);
    String msg;
    switch (iError) {
    default:
      msg = "Unknown error message number: " + iError;
      break;
    case ERROR_axisExpected:
      msg = GT.translate("x y z axis expected");
      break;
    case ERROR_backgroundModelError:
      msg = GT.translate("{0} not allowed with background model displayed");
      break;
    case ERROR_badArgumentCount:
      msg = GT.translate("bad argument count");
      break;
    case ERROR_badMillerIndices:
      msg = GT.translate("Miller indices cannot all be zero.");
      break;
    case ERROR_badRGBColor:
      msg = GT.translate("bad [R,G,B] color");
      break;
    case ERROR_booleanExpected:
      msg = GT.translate("boolean expected");
      break;
    case ERROR_booleanOrNumberExpected:
      msg = GT.translate("boolean or number expected");
      break;
    case ERROR_booleanOrWhateverExpected:
      msg = GT.translate("boolean, number, or {0} expected");
      break;
    case ERROR_colorExpected:
      msg = GT.translate("color expected");
      break;
    case ERROR_colorOrPaletteRequired:
      msg = GT.translate("a color or palette name (Jmol, Rasmol) is required");
      break;
    case ERROR_commandExpected:
      msg = GT.translate("command expected");
      break;
    case ERROR_coordinateOrNameOrExpressionRequired:
      msg = GT.translate("{x y z} or $name or (atom expression) required");
      break;
    case ERROR_drawObjectNotDefined:
      msg = GT.translate("draw object not defined");
      break;
    case ERROR_endOfStatementUnexpected:
      msg = GT.translate("unexpected end of script command");
      break;
    case ERROR_expressionExpected:
      msg = GT.translate("valid (atom expression) expected");
      break;
    case ERROR_expressionOrIntegerExpected:
      msg = GT.translate("(atom expression) or integer expected");
      break;
    case ERROR_filenameExpected:
      msg = GT.translate("filename expected");
      break;
    case ERROR_fileNotFoundException:
      msg = GT.translate("file not found");
      break;
    case ERROR_incompatibleArguments:
      msg = GT.translate("incompatible arguments");
      break;
    case ERROR_insufficientArguments:
      msg = GT.translate("insufficient arguments");
      break;
    case ERROR_integerExpected:
      msg = GT.translate("integer expected");
      break;
    case ERROR_integerOutOfRange:
      msg = GT.translate("integer out of range ({0} - {1})");
      break;
    case ERROR_invalidArgument:
      msg = GT.translate("invalid argument");
      break;
    case ERROR_invalidParameterOrder:
      msg = GT.translate("invalid parameter order");
      break;
    case ERROR_keywordExpected:
      msg = GT.translate("keyword expected");
      break;
    case ERROR_moCoefficients:
      msg = GT.translate("no MO coefficient data available");
      break;
    case ERROR_moIndex:
      msg = GT.translate("An MO index from 1 to {0} is required");
      break;
    case ERROR_moModelError:
      msg = GT.translate("no MO basis/coefficient data available for this frame");
      break;
    case ERROR_moOccupancy:
      msg = GT.translate("no MO occupancy data available");
      break;
    case ERROR_moOnlyOne:
      msg = GT.translate("Only one molecular orbital is available in this file");
      break;
    case ERROR_multipleModelsNotOK:
      msg = GT.translate("{0} require that only one model be displayed");
      break;
    case ERROR_noData:
      msg = GT.translate("No data available");
      break;
    case ERROR_noPartialCharges:
      msg = GT
          .translate("No partial charges were read from the file; Jmol needs these to render the MEP data.");
      break;
    case ERROR_noUnitCell:
      msg = GT.translate("No unit cell");
      break;
    case ERROR_numberExpected:
      msg = GT.translate("number expected");
      break;
    case ERROR_numberMustBe:
      msg = GT.translate("number must be ({0} or {1})");
      break;
    case ERROR_numberOutOfRange:
      msg = GT.translate("decimal number out of range ({0} - {1})");
      break;
    case ERROR_objectNameExpected:
      msg = GT.translate("object name expected after '$'");
      break;
    case ERROR_planeExpected:
      msg = GT
          .translate("plane expected -- either three points or atom expressions or {0} or {1} or {2}");
      break;
    case ERROR_propertyNameExpected:
      msg = GT.translate("property name expected");
      break;
    case ERROR_spaceGroupNotFound:
      msg = GT.translate("space group {0} was not found.");
      break;
    case ERROR_stringExpected:
      msg = GT.translate("quoted string expected");
      break;
    case ERROR_stringOrIdentifierExpected:
      msg = GT.translate("quoted string or identifier expected");
      break;
    case ERROR_tooManyPoints:
      msg = GT.translate("too many rotation points were specified");
      break;
    case ERROR_tooManyScriptLevels:
      msg = GT.translate("too many script levels");
      break;
    case ERROR_unrecognizedAtomProperty:
      msg = GT.translate("unrecognized atom property");
      break;
    case ERROR_unrecognizedBondProperty:
      msg = GT.translate("unrecognized bond property");
      break;
    case ERROR_unrecognizedCommand:
      msg = GT.translate("unrecognized command");
      break;
    case ERROR_unrecognizedExpression:
      msg = GT.translate("runtime unrecognized expression");
      break;
    case ERROR_unrecognizedObject:
      msg = GT.translate("unrecognized object");
      break;
    case ERROR_unrecognizedParameter:
      msg = GT.translate("unrecognized {0} parameter");
      break;
    case ERROR_unrecognizedParameterWarning:
      msg = GT
          .translate("unrecognized {0} parameter in Jmol state script (set anyway)");
      break;
    case ERROR_unrecognizedShowParameter:
      msg = GT.translate("unrecognized SHOW parameter --  use {0}");
      break;
    case ERROR_what:
      msg = "{0}";
      break;
    case ERROR_writeWhat:
      msg = GT.translate("write what? {0} or {1} \"filename\"");
      break;
    }
    if (msg.indexOf("{0}") < 0) {
      if (value != null)
        msg += ": " + value;
    } else {
      msg = TextFormat.simpleReplace(msg, "{0}", value);
      if (msg.indexOf("{1}") >= 0)
        msg = TextFormat.simpleReplace(msg, "{1}", more);
      else if (more != null)
        msg += ": " + more;
      if (msg.indexOf("{2}") >= 0)
        msg = TextFormat.simpleReplace(msg, "{2}", more);
    }
    if (doTranslate)
      GT.setDoTranslate(true);
    return msg;
  }

  String contextTrace() {
    StringBuffer sb = new StringBuffer();
    for (;;) {
      sb.append(setErrorLineMessage(functionName, filename, getLinenumber(null),
          pc, statementAsString(statement, iToken)));
      if (scriptLevel > 0)
        popContext();
      else
        break;
    }
    return sb.toString();
  }

  static String setErrorLineMessage(String functionName, String filename,
                                    int lineCurrent, int pcCurrent,
                                    String lineInfo) {
    String err = "\n----";
    if (filename != null || functionName != null)
      err += "line " + lineCurrent + " command " + (pcCurrent + 1) + " of "
          + (functionName == null ? filename : "function " + functionName ) + ":";
    err += "\n         " + lineInfo;
    return err;
  }

  class ScriptException extends Exception {

    private String message;
    private String untranslated;

    ScriptException(String msg, String untranslated) {
      errorType = message = msg;
      iCommandError = pc;
      this.untranslated = (untranslated == null ? msg : untranslated);
      if (message == null) {
        message = "";
        return;
      }
      
      String s = contextTrace();
      message += s;
      this.untranslated += s;
      if (isSyntaxCheck
          || msg.indexOf("file recognized as a script file:") >= 0)
        return;
      Logger.error("eval ERROR: " + toString());
      if (viewer.autoExit)
        viewer.exitJmol();      
    }

    protected String getErrorMessageUntranslated() {
      return untranslated;
    }

    public String toString() {
      return message;
    }
  }

  public String toString() {
    StringBuffer str = new StringBuffer();
    str.append("Eval\n pc:");
    str.append(pc);
    str.append("\n");
    str.append(aatoken.length);
    str.append(" statements\n");
    for (int i = 0; i < aatoken.length; ++i) {
      str.append("----\n");
      Token[] atoken = aatoken[i];
      for (int j = 0; j < atoken.length; ++j) {
        str.append(atoken[j]);
        str.append('\n');
      }
      str.append('\n');
    }
    str.append("END\n");
    return str.toString();
  }

  private String statementAsString(Token[] statement, int iTok) {
    if (statement.length == 0)
      return "";
    StringBuffer sb = new StringBuffer();
    int tok = statement[0].tok;
    switch (tok) {
    case Token.nada:
      String s = (String) statement[0].value;
      return (s.startsWith("/") ? "/" : "#") + s;
    case Token.end:
      if (statement.length == 2 && statement[1].tok == Token.function)
        return ((ScriptFunction) (statement[1].value)).toString();
    }
    boolean useBraces = true;// (!Token.tokAttr(tok,
    // Token.atomExpressionCommand));
    boolean inBrace = false;
    boolean inClauseDefine = false;
    boolean setEquals = (tok == Token.set
        && ((String) statement[0].value) == "" && statement[0].intValue == '=' && tokAt(1) != Token.expressionBegin);
    int len = statement.length;
    for (int i = 0; i < len; ++i) {
      Token token = statement[i];
      if (token == null) {
        len = i;
        break;
      }
      if (iTok == i - 1)
        sb.append(" <<");
      if (i != 0)
        sb.append(' ');
      if (i == 2 && setEquals) {
        setEquals = false;
        if (token.tok != Token.opEQ)
          sb.append("= ");
      }
      if (iTok == i && token.tok != Token.expressionEnd)
        sb.append(">> ");
      switch (token.tok) {
      case Token.expressionBegin:
        if (useBraces)
          sb.append("{");
        continue;
      case Token.expressionEnd:
        if (inClauseDefine && i == statementLength - 1)
          useBraces = false;
        if (useBraces)
          sb.append("}");
        continue;
      case Token.leftsquare:
      case Token.rightsquare:
        break;
      case Token.leftbrace:
      case Token.rightbrace:
        inBrace = (token.tok == Token.leftbrace);
        break;
      case Token.define:
        if (i > 0 && ((String) token.value).equals("define")) {
          sb.append("@");
          if (tokAt(i + 1) == Token.expressionBegin) {
            if (!useBraces)
              inClauseDefine = true;
            useBraces = true;
          }
          continue;
        }
        break;
      case Token.on:
        sb.append("true");
        continue;
      case Token.off:
        sb.append("false");
        continue;
      case Token.select:
        break;
      case Token.integer:
        sb.append(token.intValue);
        continue;
      case Token.point3f:
      case Token.point4f:
      case Token.bitset:
        sb.append(ScriptVariable.sValue(token));
        continue;
      case Token.seqcode:
        sb.append('^');
        continue;
      case Token.spec_seqcode_range:
        if (token.intValue != Integer.MAX_VALUE)
          sb.append(token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        token = statement[++i];
        sb.append(' ');
        // if (token.intValue == Integer.MAX_VALUE)
        sb.append(inBrace ? "-" : "- ");
        // fall through
      case Token.spec_seqcode:
        if (token.intValue != Integer.MAX_VALUE)
          sb.append(token.intValue);
        else
          sb.append(Group.getSeqcodeString(getSeqCode(token)));
        continue;
      case Token.spec_chain:
        sb.append("*:");
        sb.append((char) token.intValue);
        continue;
      case Token.spec_alternate:
        sb.append("*%");
        if (token.value != null)
          sb.append(token.value.toString());
        continue;
      case Token.spec_model:
        sb.append("*/");
        // fall through
      case Token.spec_model2:
      case Token.decimal:
        if (token.intValue < Integer.MAX_VALUE) {
          sb.append(Escape.escapeModelFileNumber(token.intValue));
        } else {
          sb.append("" + token.value);
        }
        continue;
      case Token.spec_resid:
        sb.append('[');
        sb.append(Group.getGroup3((short) token.intValue));
        sb.append(']');
        continue;
      case Token.spec_name_pattern:
        sb.append('[');
        sb.append(token.value);
        sb.append(']');
        continue;
      case Token.spec_atom:
        sb.append("*.");
        break;
      case Token.cell:
        if (token.value instanceof Point3f) {
          Point3f pt = (Point3f) token.value;
          sb.append("cell={").append(pt.x).append(" ").append(pt.y).append(" ")
              .append(pt.z).append("}");
          continue;
        }
        break;
      case Token.string:
        sb.append("\"").append(token.value).append("\"");
        continue;
      case Token.opEQ:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opLT:
      case Token.opNE:
        // not quite right -- for "inmath"
        if (token.intValue == Token.property) {
          sb.append((String) statement[++i].value).append(" ");
        } else if (token.intValue != Integer.MAX_VALUE)
          sb.append(Token.nameOf(token.intValue)).append(" ");
        break;
      case Token.identifier:
        break;
      default:
        if (!logMessages)
          break;
        sb.append('\n').append(token.toString()).append('\n');
        continue;
      }
      if (token.value != null)
        // value SHOULD NEVER BE NULL, BUT JUST IN CASE...
        sb.append(token.value.toString());
    }
    if (iTok >= len - 1)
      sb.append(" <<");
    return sb.toString();
  }

  
  ////////////// outgoing methods for setting properties
  
  private void setShapeProperty(int shapeType, String propertyName,
                                Object propertyValue) {
    if (!isSyntaxCheck)
      viewer.setShapeProperty(shapeType, propertyName, propertyValue);
  }

  private void setShapeSize(int shapeType, int size) {
    setShapeSize(shapeType, size, Float.NaN);
  }

  private void setShapeSize(int shapeType, int size, float fsize) {
    // stars, halos, balls only
    if (!isSyntaxCheck)
      viewer.setShapeSize(shapeType, size, fsize);
  }

  private void setBooleanProperty(String key, boolean value) {
    if (!isSyntaxCheck)
      viewer.setBooleanProperty(key, value);
  }

  private boolean setIntProperty(String key, int value) {
    if (!isSyntaxCheck)
      viewer.setIntProperty(key, value);
    return true;
  }

  private boolean setFloatProperty(String key, float value) {
    if (!isSyntaxCheck)
      viewer.setFloatProperty(key, value);
    return true;
  }

  private void setStringProperty(String key, String value) {
    if (!isSyntaxCheck) // ??? || key.equalsIgnoreCase("defaultdirectory"))
      viewer.setStringProperty(key, value);
  }

  private void showString(String str) {
    if (isSyntaxCheck)
      return;
    if (outputBuffer != null)
      outputBuffer.append(str).append('\n');
    else
      viewer.showString(str, false);
  }

  private void scriptStatusOrBuffer(String s) {
    if (outputBuffer != null) {
      outputBuffer.append(s).append('\n');
      return;
    }
    viewer.scriptStatus(s);
  }

  /*
   * ******************************************************
   * ============= expression processing ==================
   */

  private Token[] tempStatement;
  private boolean isBondSet;
  private Object expressionResult;

  private BitSet expression(int index) throws ScriptException {
    if (!checkToken(index))
      error(ERROR_badArgumentCount);
    return expression(statement, index, 0, true, false, true, true);
  }

  private BitSet expression(Token[] code, int pcStart, int pcStop,
                            boolean allowRefresh, boolean allowUnderflow,
                            boolean mustBeBitSet, boolean andNotDeleted)
      throws ScriptException {
    // note that this is general -- NOT just statement[]
    // errors reported would improperly access statement/line context
    // there should be no errors anyway, because this is for
    // predefined variables, but it is conceivable that one could
    // have a problem.

    isBondSet = false;
    if (code != statement) {
      tempStatement = statement;
      statement = code;
    }
    ScriptMathProcessor rpn = new ScriptMathProcessor(this, false, false);
    Object val;
    int comparisonValue = Integer.MAX_VALUE;
    boolean refreshed = false;
    iToken = 1000;
    boolean ignoreSubset = (pcStart < 0);
    boolean isInMath = false;
    int nExpress = 0;
    int atomCount = viewer.getAtomCount();
    if (ignoreSubset)
      pcStart = -pcStart;
    ignoreSubset |= isSyntaxCheck;
    if (pcStop == 0 && code.length > pcStart)
      pcStop = pcStart + 1;
    // if (logMessages)
    // viewer.scriptStatus("start to evaluate expression");
    expression_loop: for (int pc = pcStart; pc < pcStop; ++pc) {
      iToken = pc;
      Token instruction = code[pc];
      if (instruction == null)
        break;
      Object value = instruction.value;
      // if (logMessages)
      // viewer.scriptStatus("instruction=" + instruction);
      switch (instruction.tok) {
      case Token.expressionBegin:
        pcStart = pc;
        pcStop = code.length;
        nExpress++;
        break;
      case Token.expressionEnd:
        nExpress--;
        if (nExpress > 0)
          continue;
        break expression_loop;
      case Token.leftbrace:
        if (isPoint3f(pc)) {
          Point3f pt = getPoint3f(pc, true);
          if (pt != null) {
            rpn.addX(pt);
            pc = iToken;
            break;
          }
        }
        break; // ignore otherwise
      case Token.rightbrace:
        break;
      case Token.leftsquare:
        isInMath = true;
        rpn.addOp(instruction);
        break;
      case Token.rightsquare:
        isInMath = false;
        rpn.addOp(instruction);
        break;
      case Token.identifier:
        val = getParameter((String) value, false);
        if (val instanceof String)
          val = getStringObjectAsVariable((String) val, null);
        if (val instanceof String)
          val = lookupIdentifierValue((String) value);
        rpn.addX(val);
        break;
      case Token.define:
        rpn.addX(getAtomBitSet(this, (String) value));
        break;
      case Token.plane:
        rpn.addX(new ScriptVariable(instruction));
        rpn.addX(new ScriptVariable(Token.point4f, planeParameter(pc + 2)));
        pc = iToken;
        break;
      case Token.coord:
        rpn.addX(new ScriptVariable(instruction));
        rpn.addX(getPoint3f(pc + 2, true));
        pc = iToken;
        break;
      case Token.string:
        rpn.addX(new ScriptVariable(instruction));
        //note that the compiler has changed all within() types to strings.
        if (((String) value).equals("hkl")) {
          rpn.addX(new ScriptVariable(Token.point4f, hklParameter(pc + 2)));
          pc = iToken;
        }
        break;
      case Token.within:
      case Token.substructure:
      case Token.connected:
      case Token.comma:
        rpn.addOp(instruction);
        break;
      case Token.all:
        rpn.addX(viewer.getModelAtomBitSet(-1, true));
        break;
      case Token.none:
        rpn.addX(new BitSet());
        break;
      case Token.on:
      case Token.off:
        rpn.addX(new ScriptVariable(instruction));
        break;
      case Token.selected:
        rpn.addX(BitSetUtil.copy(viewer.getSelectionSet()));
        break;
      case Token.subset:
        BitSet bsSubset = viewer.getSelectionSubset();
        rpn.addX(bsSubset == null ? viewer.getModelAtomBitSet(-1, true)
            : BitSetUtil.copy(bsSubset));
        break;
      case Token.hidden:
        rpn.addX(BitSetUtil.copy(viewer.getHiddenSet()));
        break;
      case Token.displayed:
        rpn.addX(BitSetUtil.copyInvert(viewer.getHiddenSet(), atomCount));
        break;
      case Token.visible:
        if (!isSyntaxCheck && !refreshed)
          viewer.setModelVisibility();
        refreshed = true;
        rpn.addX(viewer.getVisibleSet());
        break;
      case Token.clickable:
        // a bit different, because it requires knowing what got slabbed
        if (!isSyntaxCheck && allowRefresh)
          refresh();
        rpn.addX(viewer.getClickableSet());
        break;
      case Token.carbohydrate:
      case Token.dna:
      case Token.hetero:
      case Token.isaromatic:
      case Token.nucleic:
      case Token.protein:
      case Token.purine:
      case Token.pyrimidine:
      case Token.rna:
      case Token.spec_atom:
      case Token.spec_name_pattern:
      case Token.spec_alternate:
      case Token.specialposition:
      case Token.symmetry:
      case Token.unitcell:
        rpn.addX(getAtomBits(instruction.tok, (String) value));
        break;
      case Token.spec_model:
        // from select */1002 or */1000002 or */1.2
        // */1002 is equivalent to 1.2 when more than one file is present
      case Token.spec_model2:
        // from just using the number 1.2
        int iModel = instruction.intValue;
        if (iModel == Integer.MAX_VALUE && value instanceof Integer) {
          // from select */n
          iModel = ((Integer) value).intValue();
          if (!viewer.haveFileSet()) {
            rpn.addX(getAtomBits(Token.spec_model, new Integer(iModel)));
            break;
          }
          if (iModel < 1000)
            iModel = iModel * 1000000;
          else
            iModel = (iModel / 1000) * 1000000 + iModel % 1000;
        }
        rpn.addX(bitSetForModelFileNumber(iModel));
        break;
      case Token.spec_resid:
      case Token.spec_chain:
        rpn
            .addX(getAtomBits(instruction.tok,
                new Integer(instruction.intValue)));
        break;
      case Token.spec_seqcode:
        if (isInMath)
          rpn.addXNum(ScriptVariable.intVariable(instruction.intValue));
        else
          rpn.addX(getAtomBits(Token.spec_seqcode, new Integer(
              getSeqCode(instruction))));
        break;
      case Token.spec_seqcode_range:
        if (isInMath) {
          rpn.addXNum(ScriptVariable.intVariable(instruction.intValue));
          rpn.addX(Token.tokenMinus);
          rpn.addXNum(ScriptVariable.intVariable(code[++pc].intValue));
          break;
        }
        int chainID = (pc + 3 < code.length && code[pc + 2].tok == Token.opAnd
            && code[pc + 3].tok == Token.spec_chain ? code[pc + 3].intValue
            : '\t');
        rpn.addX(getAtomBits(Token.spec_seqcode_range, new int[] {
            getSeqCode(instruction), getSeqCode(code[++pc]), chainID }));
        if (chainID != '\t')
          pc += 2;
        break;
      case Token.cell:
        Point3f pt = (Point3f) value;
        rpn.addX(getAtomBits(Token.cell, new int[] { (int) (pt.x * 1000),
            (int) (pt.y * 1000), (int) (pt.z * 1000) }));
        break;
      case Token.thismodel:
        rpn
            .addX(viewer
                .getModelAtomBitSet(viewer.getCurrentModelIndex(), true));
        break;
      case Token.hydrogen:
      case Token.amino:
      case Token.backbone:
      case Token.solvent:
      case Token.helix:
      case Token.sidechain:
      case Token.surface:
        rpn.addX(lookupIdentifierValue((String) value));
        break;
      case Token.opLT:
      case Token.opLE:
      case Token.opGE:
      case Token.opGT:
      case Token.opEQ:
      case Token.opNE:
        val = code[++pc].value;
        int tokOperator = instruction.tok;
        int tokWhat = instruction.intValue;
        String property = (tokWhat == Token.property ? (String) val : null);
        if (property != null)
          val = code[++pc].value;
        if (isSyntaxCheck) {
          rpn.addX(new BitSet());
          break;
        }
        boolean isModel = (tokWhat == Token.model);
        boolean isIntProperty = Token.tokAttr(tokWhat, Token.intproperty);
        boolean isFloatProperty = Token.tokAttr(tokWhat, Token.floatproperty);
        boolean isIntOrFloat = isIntProperty && isFloatProperty;
        boolean isStringProperty = !isIntProperty
            && Token.tokAttr(tokWhat, Token.strproperty);
        if (tokWhat == Token.element)
          isIntProperty = !(isStringProperty = false);
        int tokValue = code[pc].tok;
        comparisonValue = code[pc].intValue;
        float comparisonFloat = Float.NaN;
        if (val instanceof String) {
          if (tokWhat == Token.color) {
            comparisonValue = Graphics3D.getArgbFromString((String) val);
            if (comparisonValue == 0 && tokValue == Token.identifier) {
              val = getStringParameter((String) val, true);
              comparisonValue = Graphics3D.getArgbFromString((String) val);
            }
            tokValue = Token.integer;
            isIntProperty = true;
          } else if (isStringProperty) {
            if (tokValue == Token.identifier)
              val = getStringParameter((String) val, true);
          } else {
            if (tokValue == Token.identifier)
              val = getNumericParameter((String) val);
            if (val instanceof String) {
              if (tokWhat == Token.structure || tokWhat == Token.element)
                isStringProperty = !(isIntProperty = (comparisonValue != Integer.MAX_VALUE));
              else
                val = ScriptVariable.nValue(code[pc]);
            }
            if (val instanceof Integer)
              comparisonFloat = comparisonValue = ((Integer) val).intValue();
            else if (val instanceof Float && isModel)
              comparisonValue = ModelCollection
                  .modelFileNumberFromFloat(((Float) val).floatValue());
          }
        }
        if (isStringProperty && !(val instanceof String)) {
          val = "" + val;
        }
        if (val instanceof Integer || tokValue == Token.integer) {
          if (isModel) {
            if (comparisonValue >= 1000000)
              tokWhat = -Token.model;
          } else if (isIntOrFloat) {
            isFloatProperty = false;
          } else if (isFloatProperty) {
            comparisonFloat = comparisonValue;
          }
        } else if (val instanceof Float) {
          if (isModel) {
            tokWhat = -Token.model;
          } else {
            comparisonFloat = ((Float) val).floatValue();
            if (isIntOrFloat) {
              isIntProperty = false;
            } else if (isIntProperty) {
              comparisonValue = (int) comparisonFloat;
            }
          }
        } else if (!isStringProperty) {
          iToken++;
          error(ERROR_invalidArgument);
        }
        if (isModel && comparisonValue >= 1000000
            && comparisonValue % 1000000 == 0) {
          comparisonValue /= 1000000;
          tokWhat = Token.file;
          isModel = false;
        }
        if (tokWhat == -Token.model && tokOperator == Token.opEQ) {
          rpn.addX(bitSetForModelFileNumber(comparisonValue));
          break;
        }
        if (value != null && ((String) value).indexOf("-") >= 0) {
          if (isIntProperty)
            comparisonValue = -comparisonValue;
          else if (!Float.isNaN(comparisonFloat))
            comparisonFloat = -comparisonFloat;
        }
        float[] data = (tokWhat == Token.property ? viewer
            .getDataFloat(property) : null);
        rpn.addX(isIntProperty ? compareInt(tokWhat, data, tokOperator,
            comparisonValue) : isStringProperty ? compareString(tokWhat,
            tokOperator, (String) val) : compareFloat(tokWhat, data,
            tokOperator, comparisonFloat));
        break;
      case Token.bitset:
      case Token.point3f:
      case Token.point4f:
        rpn.addX(value);
        break;
      case Token.decimal:
      case Token.integer:
        rpn.addXNum(new ScriptVariable(instruction));
        break;
      default:
        if (Token.tokAttr(instruction.tok, Token.mathop))
          rpn.addOp(instruction);
        else
          error(ERROR_unrecognizedExpression);
      }
    }
    expressionResult = rpn.getResult(allowUnderflow, null);
    if (expressionResult == null) {
      if (allowUnderflow)
        return null;
      if (!isSyntaxCheck)
        rpn.dumpStacks("after getResult");
      error(ERROR_endOfStatementUnexpected);
    }
    expressionResult = ((ScriptVariable) expressionResult).value;
    if (expressionResult instanceof String
        && (mustBeBitSet || ((String) expressionResult).startsWith("({"))) {
      // allow for select @{x} where x is a string that can evaluate to a bitset
      expressionResult = (isSyntaxCheck ? new BitSet() : getAtomBitSet(this,
          (String) expressionResult));
    }
    if (!mustBeBitSet && !(expressionResult instanceof BitSet))
      return null; // because result is in expressionResult in that case
    BitSet bs = (expressionResult instanceof BitSet ? (BitSet) expressionResult
        : new BitSet());
    isBondSet = (expressionResult instanceof BondSet);
    BitSet bsDeleted = viewer.getDeletedAtoms();
    if (!isBondSet && bsDeleted != null)
      BitSetUtil.andNot(bs, bsDeleted);
    BitSet bsSubset = viewer.getSelectionSubset();
    if (!ignoreSubset && bsSubset != null && !isBondSet)
      bs.and(bsSubset);
    if (tempStatement != null) {
      statement = tempStatement;
      tempStatement = null;
    }
    return bs;
  }

  private BitSet compareFloat(int tokWhat, float[] data, int tokOperator,
                              float comparisonFloat) {
    BitSet bs = new BitSet();
    int atomCount = viewer.getAtomCount();
    ModelSet modelSet = viewer.getModelSet();
    Atom[] atoms = modelSet.atoms;
    float propertyFloat = 0;
    viewer.autoCalculate(tokWhat);
    for (int i = 0; i < atomCount; ++i) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        propertyFloat = Atom.atomPropertyFloat(atom, tokWhat);
        break;
      case Token.property:
        if (data == null || data.length <= i)
          continue;
        propertyFloat = data[i];
      }
      match = compareFloat(tokOperator, propertyFloat, comparisonFloat);
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private BitSet compareString(int tokWhat, int tokOperator,
                               String comparisonString) throws ScriptException {
    BitSet bs = new BitSet();
    Atom[] atoms = viewer.getModelSet().atoms;
    int atomCount = viewer.getAtomCount();
    boolean isCaseSensitive = (tokWhat == Token.chain && viewer
        .getChainCaseSensitive());
    if (!isCaseSensitive)
      comparisonString = comparisonString.toLowerCase();
    for (int i = 0; i < atomCount; ++i) {
      String propertyString = Atom.atomPropertyString(atoms[i], tokWhat);
      if (!isCaseSensitive)
        propertyString = propertyString.toLowerCase();
      if (compareString(tokOperator, propertyString, comparisonString))
        bs.set(i);
    }
    return bs;
  }

  protected BitSet compareInt(int tokWhat, float[] data, int tokOperator,
                            int comparisonValue) {
    BitSet bs = new BitSet();
    int propertyValue = Integer.MAX_VALUE;
    BitSet propertyBitSet = null;
    int bitsetComparator = tokOperator;
    int bitsetBaseValue = comparisonValue;
    int atomCount = viewer.getAtomCount();
    ModelSet modelSet = viewer.getModelSet();
    Atom[] atoms = modelSet.atoms;
    int imax = -1;
    int imin = 0;
    int iModel = -1;
    int[] cellRange = null;
    int nOps = 0;
    for (int i = 0; i < atomCount; ++i) {
      boolean match = false;
      Atom atom = atoms[i];
      switch (tokWhat) {
      default:
        propertyValue = Atom.atomPropertyInt(atom, tokWhat);
        break;
      case Token.symop:
        propertyBitSet = atom.getAtomSymmetry();
        if (atom.getModelIndex() != iModel) {
          iModel = atom.getModelIndex();
          cellRange = modelSet.getModelCellRange(iModel);
          nOps = modelSet.getModelSymmetryCount(iModel);
          imax = nOps;
        }
        if (bitsetBaseValue >= 200) {
          if (cellRange == null)
            continue;
          /*
           * symop>=1000 indicates symop*1000 + lattice_translation(555) for
           * this the comparision is only with the translational component; the
           * symop itself must match thus: select symop!=1655 selects all
           * symop=1 and translation !=655 select symop>=2555 selects all
           * symop=2 and translation >555 symop >=200 indicates any symop in the
           * specified translation (a few space groups have > 100 operations)
           * 
           * Note that when normalization is not done, symop=1555 may not be in
           * the base unit cell. Everything is relative to wherever the base
           * atoms ended up, usually in 555, but not necessarily.
           * 
           * The reason this is tied together an atom may have one translation
           * for one symop and another for a different one.
           * 
           * Bob Hanson - 10/2006
           */
          comparisonValue = bitsetBaseValue % 1000;
          int symop = bitsetBaseValue / 1000 - 1;
          if (symop < 0) {
            match = true;
          } else if (nOps == 0 || symop >= 0
              && !(match = propertyBitSet.get(symop))) {
            continue;
          }
          bitsetComparator = Token.none;
          if (symop < 0)
            propertyValue = atom.getCellTranslation(comparisonValue, cellRange,
                nOps);
          else
            propertyValue = atom.getSymmetryTranslation(symop, cellRange, nOps);
        } else if (nOps > 0) {
          if (comparisonValue > nOps) {
            if (bitsetComparator != Token.opLT
                && bitsetComparator != Token.opLE)
              continue;
          }
          if (bitsetComparator == Token.opNE) {
            if (comparisonValue > 0 && comparisonValue <= nOps
                && !propertyBitSet.get(comparisonValue)) {
              bs.set(i);
            }
            continue;
          }
        }
        switch (bitsetComparator) {
        case Token.opLT:
          imax = comparisonValue - 1;
          imin = 0;
          break;
        case Token.opLE:
          imax = comparisonValue;
          imin = 0;
          break;
        case Token.opGE:
          if (imax < 0)
            imax = propertyBitSet.size();
          imin = comparisonValue - 1;
          break;
        case Token.opGT:
          if (imax < 0)
            imax = propertyBitSet.size();
          imin = comparisonValue;
          break;
        case Token.opEQ:
          imax = comparisonValue;
          imin = comparisonValue - 1;
          break;
        case Token.opNE:
          match = !propertyBitSet.get(comparisonValue);
          break;
        }
        if (imin < 0)
          imin = 0;
        if (imax > propertyBitSet.size())
          imax = propertyBitSet.size();
        for (int iBit = imin; iBit < imax; iBit++) {
          if (propertyBitSet.get(iBit)) {
            match = true;
            break;
          }
        }
        // note that a symop property can be both LE and GT !
        if (!match || propertyValue == Integer.MAX_VALUE)
          tokOperator = Token.none;
      }
      if (tokOperator != Token.none)
        match = compareInt(tokOperator, propertyValue, comparisonValue);
      if (match)
        bs.set(i);
    }
    return bs;
  }

  private boolean compareString(int tokOperator, String propertyValue,
                                String comparisonValue) throws ScriptException {
    switch (tokOperator) {
    case Token.opEQ:
    case Token.opNE:
      return (TextFormat.isMatch(propertyValue, comparisonValue, true, true) == (tokOperator == Token.opEQ));
    default:
      error(ERROR_invalidArgument);
    }
    return false;
  }

  private static boolean compareInt(int tokOperator, int propertyValue,
                                    int comparisonValue) {
    switch (tokOperator) {
    case Token.opLT:
      return propertyValue < comparisonValue;
    case Token.opLE:
      return propertyValue <= comparisonValue;
    case Token.opGE:
      return propertyValue >= comparisonValue;
    case Token.opGT:
      return propertyValue > comparisonValue;
    case Token.opEQ:
      return propertyValue == comparisonValue;
    case Token.opNE:
      return propertyValue != comparisonValue;
    }
    return false;
  }

  private static boolean compareFloat(int tokOperator, float propertyFloat,
                                      float comparisonFloat) {
    switch (tokOperator) {
    case Token.opLT:
      return propertyFloat < comparisonFloat;
    case Token.opLE:
      return propertyFloat <= comparisonFloat;
    case Token.opGE:
      return propertyFloat >= comparisonFloat;
    case Token.opGT:
      return propertyFloat > comparisonFloat;
    case Token.opEQ:
      return propertyFloat == comparisonFloat;
    case Token.opNE:
      return propertyFloat != comparisonFloat;
    }
    return false;
  }

  private BitSet getAtomBits(int tokType, Object specInfo) {
    return (isSyntaxCheck ? new BitSet() : viewer
        .getAtomBits(tokType, specInfo));
  }

  private static int getSeqCode(Token instruction) {
    return (instruction.intValue != Integer.MAX_VALUE ? Group.getSeqcode(
        instruction.intValue, ' ') : ((Integer) instruction.value).intValue());
  }

  /*
   * ****************************************************************************
   * ============================================================== checks and
   * parameter retrieval
   * ==============================================================
   */

  private void checkLength(int length) throws ScriptException {
    if (length >= 0) {
      checkLength(length, 0);
      return;
    }
    // max
    if (statementLength <= -length)
      return;
    iToken = -length;
    error(ERROR_badArgumentCount);
  }

  private void checkLength(int length, int errorPt) throws ScriptException {
    if (statementLength == length)
      return;
    iToken = errorPt > 0 ? errorPt : statementLength;
    error(errorPt > 0 ? ERROR_invalidArgument : ERROR_badArgumentCount);
  }

  private int checkLength23() throws ScriptException {
    iToken = statementLength;
    if (statementLength < 2 || statementLength > 3)
      error(ERROR_badArgumentCount);
    return statementLength;
  }

  private void checkLength34() throws ScriptException {
    iToken = statementLength;
    if (statementLength < 3 || statementLength > 4)
      error(ERROR_badArgumentCount);
  }

  private int theTok;
  private Token theToken;

  private Token getToken(int i) throws ScriptException {
    if (!checkToken(i))
      error(ERROR_endOfStatementUnexpected);
    theToken = statement[i];
    theTok = theToken.tok;
    return theToken;
  }

  private int tokAt(int i) {
    return (i < statementLength ? statement[i].tok : Token.nada);
  }

  private int tokAt(int i, Token[] args) {
    return (i < args.length ? args[i].tok : Token.nada);
  }

  private Token tokenAt(int i, Token[] args) {
    return (i < args.length ? args[i] : null);
  }

  private boolean checkToken(int i) {
    return (iToken = i) < statementLength;
  }

  private int modelNumberParameter(int index) throws ScriptException {
    int iFrame = 0;
    boolean useModelNumber = false;
    switch (tokAt(index)) {
    case Token.integer:
      useModelNumber = true;
      // fall through
    case Token.decimal:
      iFrame = getToken(index).intValue; // decimal Token intValue is
                                         // model/frame number encoded
      break;
    default:
      error(ERROR_invalidArgument);
    }
    return viewer.getModelNumberIndex(iFrame, useModelNumber, true);
  }

  private String optParameterAsString(int i) throws ScriptException {
    if (i >= statementLength)
      return "";
    return parameterAsString(i);
  }

  private String parameterAsString(int i) throws ScriptException {
    getToken(i);
    if (theToken == null)
      error(ERROR_endOfStatementUnexpected);
    return (theTok == Token.integer ? "" + theToken.intValue : ""
        + theToken.value);
  }

  private int intParameter(int index) throws ScriptException {
    if (checkToken(index))
      if (getToken(index).tok == Token.integer)
        return theToken.intValue;
    error(ERROR_integerExpected);
    return 0;
  }

  private int intParameter(int i, int min, int max) throws ScriptException {
    int val = intParameter(i);
    if (val < min || val > max)
      integerOutOfRange(min, max);
    return val;
  }

  private boolean isFloatParameter(int index) {
    switch (tokAt(index)) {
    case Token.integer:
    case Token.decimal:
      return true;
    }
    return false;
  }

  private float floatParameter(int i, float min, float max)
      throws ScriptException {
    float val = floatParameter(i);
    if (val < min || val > max)
      numberOutOfRange(min, max);
    return val;
  }

  private float floatParameter(int index) throws ScriptException {
    if (checkToken(index)) {
      getToken(index);
      switch (theTok) {
      case Token.spec_seqcode:
      case Token.integer:
        return theToken.intValue;
      case Token.spec_model2:
      case Token.decimal:
        return ((Float) theToken.value).floatValue();
      }
    }
    error(ERROR_numberExpected);
    return 0;
  }

  private int floatParameterSet(int i, float[] fparams) throws ScriptException {
    if (tokAt(i) == Token.leftbrace)
      i++;
    for (int j = 0; j < fparams.length; j++)
      fparams[j] = floatParameter(i++);
    if (tokAt(i) == Token.rightbrace)
      i++;
    return i;
  }

  private String stringParameter(int index) throws ScriptException {
    if (!checkToken(index) || getToken(index).tok != Token.string)
      error(ERROR_stringExpected);
    return (String) theToken.value;
  }

  private String objectNameParameter(int index) throws ScriptException {
    if (!checkToken(index))
      error(ERROR_objectNameExpected);
    return parameterAsString(index);
  }

  /**
   * Based on the form of the parameters, returns and encoded radius as follows:
   * 
   * script meaning range encoded
   * 
   * +1.2 offset [0 - 10] x -1.2 offset 0) x 1.2 absolute (0 - 10] x + 10 -30%
   * 70% (-100 - 0) x + 200 +30% 130% (0 x + 200 80% percent (0 x + 100
   * 
   * in each case, numbers can be integer or float
   * 
   * @param index
   * @param defaultValue
   *          a default value or Float.NaN
   * @return one of the above possibilities
   * @throws ScriptException
   */
  private float radiusParameter(int index, float defaultValue)
      throws ScriptException {
    if (!checkToken(index)) {
      if (Float.isNaN(defaultValue))
        error(ERROR_numberExpected);
      return defaultValue;
    }
    getToken(index);
    float v = Float.NaN;
    boolean isOffset = (theTok == Token.plus);
    if (isOffset)
      index++;
    boolean isPercent = (tokAt(index + 1) == Token.percent);
    switch (tokAt(index)) {
    case Token.integer:
      v = intParameter(index);
    case Token.decimal:
      if (Float.isNaN(v))
        v = floatParameter(index);
      if (v < 0)
        isOffset = true;
      break;
    default:
      v = defaultValue;
      index--;
    }
    iToken = index + (isPercent ? 1 : 0);
    if (Float.isNaN(v))
      error(ERROR_numberExpected);
    if (v == 0)
      return 0;
    if (isPercent) {
      if (v <= -100)
        error(ERROR_invalidArgument);
      v += (isOffset ? 200 : 100);
    } else if (isOffset) {
    } else {
      if (v < 0 || v > 10)
        numberOutOfRange(0f, 10f);
      v += 10;
    }
    return v;
  }

  private boolean booleanParameter(int i) throws ScriptException {
    if (statementLength == i)
      return true;
    checkLength(i + 1);
    switch (getToken(i).tok) {
    case Token.on:
      return true;
    case Token.off:
      return false;
    default:
      error(ERROR_booleanExpected);
    }
    return false;
  }

  private Point3f atomCenterOrCoordinateParameter(int i) throws ScriptException {
    switch (getToken(i).tok) {
    case Token.bitset:
    case Token.expressionBegin:
      BitSet bs = expression(statement, i, 0, true, false, false, true);
      if (bs != null)
        return viewer.getAtomSetCenter(bs);
      if (expressionResult instanceof Point3f)
        return (Point3f) expressionResult;
      error(ERROR_invalidArgument);
      break;
    case Token.leftbrace:
    case Token.point3f:
      return getPoint3f(i, true);
    }
    error(ERROR_invalidArgument);
    // impossible return
    return null;
  }

  private boolean isCenterParameter(int i) {
    int tok = tokAt(i);
    return (tok == Token.dollarsign || tok == Token.leftbrace
        || tok == Token.expressionBegin || tok == Token.point3f || tok == Token.bitset);
  }

  private Point3f centerParameter(int i) throws ScriptException {
    Point3f center = null;
    expressionResult = null;
    if (checkToken(i)) {
      switch (getToken(i).tok) {
      case Token.dollarsign:
        int index = Integer.MIN_VALUE;
        String id = objectNameParameter(++i);
        // allow for $pt2.3 -- specific vertex
        if (tokAt(i + 1) == Token.leftsquare) {
          index = intParameter(i + 2);
          if (getToken(i + 3).tok != Token.rightsquare)
            error(ERROR_invalidArgument);
        }
        if (isSyntaxCheck)
          return new Point3f();
        if ((center = getObjectCenter(id, index)) == null)
          error(ERROR_drawObjectNotDefined, id);
        break;
      case Token.bitset:
      case Token.expressionBegin:
      case Token.leftbrace:
      case Token.point3f:
        center = atomCenterOrCoordinateParameter(i);
        break;
      }
    }
    if (center == null)
      error(ERROR_coordinateOrNameOrExpressionRequired);
    return center;
  }

  private Point4f planeParameter(int i) throws ScriptException {
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    if (i < statementLength)
      switch (getToken(i).tok) {
      case Token.point4f:
        return (Point4f) theToken.value;
      case Token.dollarsign:
        String id = objectNameParameter(++i);
        if (isSyntaxCheck)
          return new Point4f();
        int shapeType = viewer.getShapeIdFromObjectName(id);
        switch (shapeType) {
        case JmolConstants.SHAPE_DRAW:
          setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", id);
          Point3f[] points = (Point3f[]) viewer.getShapeProperty(
              JmolConstants.SHAPE_DRAW, "vertices");
          if (points == null || points.length < 3)
            break;
          return Measure.getPlaneThroughPoints(points[0], points[1],
              points[2], new Vector3f(), vAB, vAC);
        case JmolConstants.SHAPE_ISOSURFACE:
          setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "thisID", id);
          Point4f plane = (Point4f) viewer.getShapeProperty(
              JmolConstants.SHAPE_ISOSURFACE, "plane");
          if (plane != null)
            return plane;
        }
        break;
      case Token.identifier:
      case Token.string:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("xy"))
          return new Point4f(0, 0, 1, 0);
        if (str.equalsIgnoreCase("xz"))
          return new Point4f(0, 1, 0, 0);
        if (str.equalsIgnoreCase("yz"))
          return new Point4f(1, 0, 0, 0);
        iToken += 2;
        if (str.equalsIgnoreCase("x")) {
          if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
            evalError("x=?", null);
          return new Point4f(1, 0, 0, -floatParameter(i));
        }

        if (str.equalsIgnoreCase("y")) {
          if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
            evalError("y=?", null);
          return new Point4f(0, 1, 0, -floatParameter(i));
        }
        if (str.equalsIgnoreCase("z")) {
          if (!checkToken(++i) || getToken(i++).tok != Token.opEQ)
            evalError("z=?", null);
          return new Point4f(0, 0, 1, -floatParameter(i));
        }
        break;
      case Token.leftbrace:
        if (!isPoint3f(i))
          return getPoint4f(i);
        // fall through
      case Token.bitset:
      case Token.expressionBegin:
        Point3f pt1 = atomCenterOrCoordinateParameter(i);
        if (getToken(++iToken).tok == Token.comma)
          ++iToken;
        Point3f pt2 = atomCenterOrCoordinateParameter(iToken);
        if (getToken(++iToken).tok == Token.comma)
          ++iToken;
        Point3f pt3 = atomCenterOrCoordinateParameter(iToken);
        i = iToken;
        Vector3f plane = new Vector3f();
        float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, plane, vAB,
            vAC);
        Point4f p = new Point4f(plane.x, plane.y, plane.z, w);
        if (!isSyntaxCheck && Logger.debugging) {
          Logger.debug("points: " + pt1 + pt2 + pt3 + " defined plane: " + p);
        }
        return p;
      }
    planeExpected();
    // impossible return
    return null;
  }

  private Point4f hklParameter(int i) throws ScriptException {
    if (!isSyntaxCheck && viewer.getCurrentUnitCell() == null)
      error(ERROR_noUnitCell);
    Vector3f vAB = new Vector3f();
    Vector3f vAC = new Vector3f();
    Point3f pt = (Point3f) getPointOrPlane(i, false, true, false, true, 3, 3);
    Point3f pt1 = new Point3f(pt.x == 0 ? 1 : 1 / pt.x, 0, 0);
    Point3f pt2 = new Point3f(0, pt.y == 0 ? 1 : 1 / pt.y, 0);
    Point3f pt3 = new Point3f(0, 0, pt.z == 0 ? 1 : 1 / pt.z);
    // trick for 001 010 100 is to define the other points on other edges

    if (pt.x == 0 && pt.y == 0 && pt.z == 0) {
      error(ERROR_badMillerIndices);
    } else if (pt.x == 0 && pt.y == 0) {
      pt1.set(1, 0, pt3.z);
      pt2.set(0, 1, pt3.z);
    } else if (pt.y == 0 && pt.z == 0) {
      pt2.set(pt1.x, 0, 1);
      pt3.set(pt1.x, 1, 0);
    } else if (pt.z == 0 && pt.x == 0) {
      pt3.set(0, pt2.y, 1);
      pt1.set(1, pt2.y, 0);
    } else if (pt.x == 0) {
      pt1.set(1, pt2.y, 0);
    } else if (pt.y == 0) {
      pt2.set(0, 1, pt3.z);
    } else if (pt.z == 0) {
      pt3.set(pt1.x, 0, 1);
    }
    viewer.toCartesian(pt1);
    viewer.toCartesian(pt2);
    viewer.toCartesian(pt3);
    Vector3f plane = new Vector3f();
    float w = Measure.getNormalThroughPoints(pt1, pt2, pt3, plane, vAB, vAC);
    Point4f p = new Point4f(plane.x, plane.y, plane.z, w);
    if (!isSyntaxCheck && Logger.debugging)
      Logger.info("defined plane: " + p);
    return p;
  }

  private int getMadParameter() throws ScriptException {
    // wireframe, ssbond, hbond
    int mad = 1;
    switch (getToken(1).tok) {
    case Token.only:
      restrictSelected(false);
      break;
    case Token.on:
      break;
    case Token.off:
      mad = 0;
      break;
    case Token.integer:
      int radiusRasMol = intParameter(1, 0, 750);
      mad = radiusRasMol * 4 * 2;
      break;
    case Token.decimal:
      mad = (int) (floatParameter(1, 0, 3) * 1000 * 2);
      break;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    return mad;
  }

  private int getSetAxesTypeMad(int index) throws ScriptException {
    if (index == statementLength)
      return 1;
    checkLength(index + 1);
    switch (getToken(index).tok) {
    case Token.on:
      return 1;
    case Token.off:
      return 0;
    case Token.dotted:
      return -1;
    case Token.integer:
      return intParameter(index, -1, 19);
    case Token.decimal:
      float angstroms = floatParameter(index, 0, 2);
      return (int) (angstroms * 1000 * 2);
    }
    error(ERROR_booleanOrWhateverExpected, "\"DOTTED\"");
    return 0;
  }

  private boolean isColorParam(int i) {
    int tok = tokAt(i);
    return (tok == Token.leftsquare || tok == Token.point3f || isPoint3f(i) || (tok == Token.string || tok == Token.identifier)
        && Graphics3D.getArgbFromString((String) statement[i].value) != 0);
  }

  private int getArgbParam(int index) throws ScriptException {
    return getArgbParam(index, false);
  }

  private int getArgbParamLast(int index, boolean allowNone)
      throws ScriptException {
    int icolor = getArgbParam(index, allowNone);
    checkLength(iToken + 1);
    return icolor;
  }

  private int getArgbParam(int index, boolean allowNone) throws ScriptException {
    Point3f pt = null;
    if (checkToken(index)) {
      switch (getToken(index).tok) {
      case Token.identifier:
      case Token.string:
        return Graphics3D.getArgbFromString(parameterAsString(index));
      case Token.leftsquare:
        return getColorTriad(++index);
      case Token.point3f:
        pt = (Point3f) theToken.value;
        break;
      case Token.leftbrace:
        pt = getPoint3f(index, false);
        break;
      case Token.none:
        if (allowNone)
          return 0;
      }
    }
    if (pt == null)
      error(ERROR_colorExpected);
    return colorPtToInt(pt);
  }

  static int colorPtToInt(Point3f pt) {
    return 0xFF000000 | (((int) pt.x) & 0xFF) << 16
        | (((int) pt.y) & 0xFF) << 8 | (((int) pt.z) & 0xFF);
  }

  private int getColorTriad(int i) throws ScriptException {
    int[] colors = new int[3];
    int n = 0;
    String hex = "";
    getToken(i);
    Point3f pt = null;
    out: switch (theTok) {
    case Token.integer:
    case Token.spec_seqcode:
      for (; i < statementLength; i++) {
        getToken(i);
        switch (theTok) {
        case Token.comma:
          continue;
        case Token.identifier:
          if (n != 1 || colors[0] != 0)
            error(ERROR_badRGBColor);
          hex = "0" + parameterAsString(i);
          break out;
        case Token.integer:
          if (n > 2)
            error(ERROR_badRGBColor);
          colors[n++] = theToken.intValue;
          continue;
        case Token.spec_seqcode:
          if (n > 2)
            error(ERROR_badRGBColor);
          colors[n++] = ((Integer) theToken.value).intValue() % 256;
          continue;
        case Token.rightsquare:
          if (n == 3)
            return colorPtToInt(new Point3f(colors[0], colors[1], colors[2]));
        default:
          error(ERROR_badRGBColor);
        }
      }
      error(ERROR_badRGBColor);
      break;
    case Token.point3f:
      pt = (Point3f) theToken.value;
      break;
    case Token.identifier:
      hex = parameterAsString(i);
      break;
    default:
      error(ERROR_badRGBColor);
    }
    if (getToken(++i).tok != Token.rightsquare)
      error(ERROR_badRGBColor);
    if (pt != null)
      return colorPtToInt(pt);
    if ((n = Graphics3D.getArgbFromString("[" + hex + "]")) == 0)
      error(ERROR_badRGBColor);
    return n;
  }

  private boolean coordinatesAreFractional;

  private boolean isPoint3f(int i) {
    // first check for simple possibilities:
    boolean isOK;
    if ((isOK = (tokAt(i) == Token.point3f)) || tokAt(i) == Token.point4f
        || isFloatParameter(i + 1) && isFloatParameter(i + 2)
        && isFloatParameter(i + 3) && isFloatParameter(i + 4))
      return isOK;
    ignoreError = true;
    int t = iToken;
    isOK = true;
    try {
      getPoint3f(i, true);
    } catch (Exception e) {
      isOK = false;
    }
    ignoreError = false;
    iToken = t;
    return isOK;
  }

  private Point3f getPoint3f(int i, boolean allowFractional)
      throws ScriptException {
    return (Point3f) getPointOrPlane(i, false, allowFractional, true, false, 3,
        3);
  }

  private Point4f getPoint4f(int i) throws ScriptException {
    return (Point4f) getPointOrPlane(i, false, false, false, false, 4, 4);
  }

  private Object getPointOrPlane(int index, boolean integerOnly,
                                 boolean allowFractional, boolean doConvert,
                                 boolean implicitFractional, int minDim,
                                 int maxDim) throws ScriptException {
    // { x y z } or {a/b c/d e/f} are encoded now as seqcodes and model numbers
    // so we decode them here. It's a bit of a pain, but it isn't too bad.
    float[] coord = new float[6];
    int n = 0;
    coordinatesAreFractional = implicitFractional;
    if (tokAt(index) == Token.point3f) {
      if (minDim <= 3 && maxDim >= 3)
        return (Point3f) getToken(index).value;
      error(ERROR_invalidArgument);
    }
    if (tokAt(index) == Token.point4f) {
      if (minDim <= 4 && maxDim >= 4)
        return (Point4f) getToken(index).value;
      error(ERROR_invalidArgument);
    }
    int multiplier = 1;
    out: for (int i = index; i < statement.length; i++) {
      switch (getToken(i).tok) {
      case Token.leftbrace:
      case Token.comma:
        // case Token.opOr:
      case Token.opAnd:
        break;
      case Token.rightbrace:
        break out;
      case Token.minus:
        multiplier = -1;
        break;
      case Token.spec_seqcode_range:
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = theToken.intValue;
        multiplier = -1;
        break;
      case Token.integer:
      case Token.spec_seqcode:
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = theToken.intValue * multiplier;
        multiplier = 1;
        break;
      case Token.divide:
        getToken(++i);
      case Token.spec_model: // after a slash
        n--;
        if (n < 0 || integerOnly)
          error(ERROR_invalidArgument);
        if (theToken.value instanceof Integer || theTok == Token.integer) {
          coord[n++] /= (theToken.intValue == Integer.MAX_VALUE ? ((Integer) theToken.value)
              .intValue()
              : theToken.intValue);
        } else if (theToken.value instanceof Float) {
          coord[n++] /= ((Float) theToken.value).floatValue();
        }
        coordinatesAreFractional = true;
        break;
      case Token.decimal:
      case Token.spec_model2:
        if (integerOnly)
          error(ERROR_invalidArgument);
        if (n == 6)
          error(ERROR_invalidArgument);
        coord[n++] = ((Float) theToken.value).floatValue();
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (n < minDim || n > maxDim)
      error(ERROR_invalidArgument);
    if (n == 3) {
      Point3f pt = new Point3f(coord[0], coord[1], coord[2]);
      if (coordinatesAreFractional && doConvert && !isSyntaxCheck)
        viewer.toCartesian(pt);
      return pt;
    }
    if (n == 4) {
      if (coordinatesAreFractional) // no fractional coordinates for planes (how
                                    // to convert?)
        error(ERROR_invalidArgument);
      Point4f plane = new Point4f(coord[0], coord[1], coord[2], coord[3]);
      return plane;
    }
    return coord;
  }

  private Point3f xypParameter(int index) throws ScriptException {
    // [x y] or [x,y] refers to an xy point on the screen
    // just a Point3f with z = Float.MAX_VALUE
    // [x y %] or [x,y %] refers to an xy point on the screen
    // as a percent
    // just a Point3f with z = -Float.MAX_VALUE

    if (tokAt(index) != Token.leftsquare || !isFloatParameter(++index))
      return null;
    Point3f pt = new Point3f();
    pt.x = floatParameter(index);
    if (tokAt(++index) == Token.comma)
      index++;
    if (!isFloatParameter(index))
      return null;
    pt.y = floatParameter(index);
    boolean isPercent = (tokAt(++index) == Token.percent);
    if (isPercent)
      ++index;
    if (tokAt(index) != Token.rightsquare)
      return null;
    iToken = index;
    pt.z = (isPercent ? -1 : 1) * Float.MAX_VALUE;
    return pt;
  }

  private int intSetting(int pt, int val, int min, int max)
      throws ScriptException {
    if (val == Integer.MAX_VALUE)
      val = intSetting(pt);
    if (val < min || val > max)
      integerOutOfRange(min, max);
    return val;
  }

  private int intSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return ScriptVariable.iValue((ScriptVariable) v.elementAt(0));
  }

  private float floatSetting(int pt, float min, float max)
      throws ScriptException {
    float val = floatSetting(pt);
    if (val < min || val > max)
      numberOutOfRange(min, max);
    return val;
  }

  private float floatSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return ScriptVariable.fValue((ScriptVariable) v.elementAt(0));
  }

  private String stringSetting(int pt, boolean isJmolSet)
      throws ScriptException {
    if (isJmolSet && statementLength == pt + 1)
      return parameterAsString(pt);
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return ScriptVariable.sValue((ScriptVariable) v.elementAt(0));
  }

  private ScriptVariable tokenSetting(int pt) throws ScriptException {
    Vector v = (Vector) parameterExpression(pt, -1, "XXX", true);
    if (v == null || v.size() == 0)
      error(ERROR_invalidArgument);
    return (ScriptVariable) v.elementAt(0);
  }


  /*
   * ****************************************************************
   * =============== command dispatch ===============================
   */

  /**
   * provides support for the script editor
   * 
   * @param i
   * @return  true if displayable
   */
  private boolean isCommandDisplayable(int i) {
    if (i >= aatoken.length || i >= pcEnd || aatoken[i] == null)
      return false;
    return (lineIndices[i][1] > lineIndices[i][0]);
  }

  /**
   * checks to see if there is a pause condition, during which
   * commands can still be issued, but with the ! first. 
   * 
   * @return  false if there was a problem
   */
  private boolean checkContinue() {
    if (interruptExecution)
      return false;

    if (executionStepping && isCommandDisplayable(pc)) {
      viewer.scriptStatus("Next: " + getNextStatement(), "stepping -- type RESUME to continue", 0, null);
      executionPaused = true;
    } else if (!executionPaused) {
      return true;
    }
  
    if (true || Logger.debugging) {
      Logger.info("script execution paused at command " + (pc + 1) + " level " + scriptLevel + ": " + thisCommand);
    }
      
    try {
      while (executionPaused) {
        viewer.popHoldRepaint("pause");
        try {
	        Thread.sleep(100);
	        refresh();
	        String script = viewer.getInterruptScript();
	        if (script != "") {
	          resumePausedExecution();
	          setErrorMessage(null);
	          pc--; // in case there is an error, we point to the PAUSE command
	          try {
	            runScript(script);
	          } catch (Exception e) {
	            setErrorMessage("" + e);
	          } catch (Error er) {
	            setErrorMessage("" + er);
	          }
	          if (error) {
	            popContext();
	            scriptStatusOrBuffer(errorMessage);
	            setErrorMessage(null);
	          }
	          pc++;
	          pauseExecution();
	        }
        } catch (Exception e) {
			//Nothing
		}
        viewer.pushHoldRepaint("pause");
      }
      if (!isSyntaxCheck && !interruptExecution && !executionStepping) {
        viewer.scriptStatus("script execution " + (error || interruptExecution ? "interrupted" : "resumed"));
      }
    } catch (Exception e) {
      viewer.pushHoldRepaint("pause");
    }
    Logger.debug("script execution resumed");
    // once more to trap quit during pause
    return !error && !interruptExecution;
  }

  /**
   * here we go -- everything else in this class is called by this method
   * or one of its subsidiary methods.
   * 
   * 
   * @param doList
   * @throws ScriptException
   */
  private void instructionDispatchLoop(boolean doList) throws ScriptException {
    long timeBegin = 0;
    boolean isForCheck = false;  // indicates the stage of the for command loop

    debugScript = logMessages = false;
    if (!isSyntaxCheck)
      setDebugging();
    if (logMessages) {
      timeBegin = System.currentTimeMillis();
      viewer.scriptStatus("Eval.instructionDispatchLoop():" + timeBegin);
      viewer.scriptStatus(script);
    }
    if (pcEnd == 0)
      pcEnd = Integer.MAX_VALUE;
    if (lineEnd == 0)
      lineEnd = Integer.MAX_VALUE;
    String lastCommand = "";
    if (aatoken == null)
      return;
    for (; pc < aatoken.length && pc < pcEnd; pc++) {
      if (!isSyntaxCheck && !checkContinue())
        break;
      if (lineNumbers[pc] > lineEnd)
        break;
      Token token = (aatoken[pc].length == 0 ? null : aatoken[pc][0]);
      // when checking scripts, we can't check statments
      // containing @{...}
      if (!historyDisabled && !isSyntaxCheck
          && scriptLevel <= commandHistoryLevelMax && !tQuiet) {
        thisCommand = getCommand(pc, true, true);
        if (token != null && !thisCommand.equals(lastCommand)
            && (token.tok == Token.function || !Token.tokAttr(token.tok, Token.flowCommand))
            && thisCommand.length() > 0)
          viewer.addCommand(lastCommand = thisCommand);
      }
      if (!setStatement(pc)) {
        Logger.info(getCommand(pc, true, false)
            + " -- STATEMENT CONTAINING @{} SKIPPED");
        continue;
      }
      thisCommand = getCommand(pc, false, true);
      fullCommand = thisCommand + getNextComment();
      iToken = 0;
      String script = viewer.getInterruptScript();
      if (script != "")
        runScript(script);
      if (doList || !isSyntaxCheck) {
        int milliSecDelay = viewer.getScriptDelay();
        if (doList || milliSecDelay > 0 && scriptLevel > 0) {
          if (milliSecDelay > 0)
            delay(-(long) milliSecDelay);
          viewer.scriptEcho("$[" + scriptLevel + "." + lineNumbers[pc] + "."
              + (pc + 1) + "] " + thisCommand);
        }
      }
      if (isSyntaxCheck) {
        if (isCmdLine_c_or_C_Option)
          Logger.info(thisCommand);
        if (statementLength == 1 && statement[0].tok != Token.function)
          // && !Token.tokAttr(token.tok, Token.unimplemented))
          continue;
      } else {
        if (debugScript)
          logDebugScript(0);
        if (logMessages && token != null)
          Logger.debug(token.toString());
      }
      if (token == null)
        continue;
      
      switch (token.tok) {
      case Token.nada:
        break;
      case Token.elseif:
      case Token.ifcmd:
      case Token.whilecmd:
      case Token.forcmd:
      case Token.endifcmd:
      case Token.elsecmd:
      case Token.end:
      case Token.breakcmd:
      case Token.continuecmd:
        isForCheck = flowControl(token.tok, isForCheck);
        break;
      case Token.backbone:
        proteinShape(JmolConstants.SHAPE_BACKBONE);
        break;
      case Token.background:
        background(1);
        break;
      case Token.center:
        center(1);
        break;
      case Token.color:
        color();
        break;
      case Token.cd:
        cd();
        break;
      case Token.data:
        data();
        break;
      case Token.define:
        define();
        break;
      case Token.echo:
        echo(1, false);
        break;
      case Token.message:
        message();
        break;
      case Token.exit: // flush the queue and...
        if (!isSyntaxCheck && pc > 0)
          viewer.clearScriptQueue();
      case Token.quit: // quit this only if it isn't the first command
        if (!isSyntaxCheck)
          interruptExecution = (pc > 0 || !viewer.usingScriptQueue());
        break;
      case Token.label:
        label(1);
        break;
      case Token.hover:
        hover();
        break;
      case Token.load:
        load();
        break;
      case Token.monitor:
        monitor();
        break;
      case Token.refresh:
        refresh();
        break;
      case Token.initialize:
        viewer.initialize();
        break;
      case Token.reset:
        reset();
        break;
      case Token.rotate:
        rotate(false, false);
        break;
      case Token.javascript:
      case Token.script:
        script(token.tok);
        break;
      case Token.function:
        function();
        break;
      case Token.sync:
        sync();
        break;
      case Token.history:
        history(1);
        break;
      case Token.delete:
        delete();
        break;
      case Token.minimize:
        minimize();
        break;
      case Token.select:
        select();
        break;
      case Token.translate:
        translate();
        break;
      case Token.invertSelected:
        invertSelected();
        break;
      case Token.rotateSelected:
        rotate(false, true);
        break;
      case Token.translateSelected:
        translateSelected();
        break;
      case Token.zap:
        zap(true);
        break;
      case Token.zoom:
        zoom(false);
        break;
      case Token.zoomTo:
        zoom(true);
        break;
      case Token.delay:
        delay();
        break;
      case Token.loop:
        delay();
        if (!isSyntaxCheck)
          pc = -1;
        break;
      case Token.gotocmd:
        gotocmd();
        break;
      case Token.move:
        move();
        break;
      case Token.display:
        display(true);
        break;
      case Token.hide:
        display(false);
        break;
      case Token.restrict:
        restrict();
        break;
      case Token.subset:
        subset();
        break;
      case Token.selectionHalo:
        selectionHalo(1);
        break;
      case Token.set:
        set();
        break;
      case Token.slab:
        slab(false);
        break;
      case Token.depth:
        slab(true);
        break;
      case Token.ellipsoid:
        ellipsoid();
        break;
      case Token.star:
        setAtomShapeSize(JmolConstants.SHAPE_STARS, -100);
        break;
      case Token.halo:
        setAtomShapeSize(JmolConstants.SHAPE_HALOS, -20);
        break;
      case Token.spacefill: // aka cpk
        setAtomShapeSize(JmolConstants.SHAPE_BALLS, -100);
        break;
      case Token.structure:
        structure();
        break;
      case Token.wireframe:
        wireframe();
        break;
      case Token.vector:
        vector();
        break;
      case Token.dipole:
        dipole();
        break;
      case Token.animation:
        animation();
        break;
      case Token.vibration:
        vibration();
        break;
      case Token.calculate:
        calculate();
        break;
      case Token.dots:
        dots(JmolConstants.SHAPE_DOTS);
        break;
      case Token.strands:
        proteinShape(JmolConstants.SHAPE_STRANDS);
        break;
      case Token.meshRibbon:
        proteinShape(JmolConstants.SHAPE_MESHRIBBON);
        break;
      case Token.ribbon:
        proteinShape(JmolConstants.SHAPE_RIBBONS);
        break;
      case Token.trace:
        proteinShape(JmolConstants.SHAPE_TRACE);
        break;
      case Token.cartoon:
        proteinShape(JmolConstants.SHAPE_CARTOON);
        break;
      case Token.rocket:
        proteinShape(JmolConstants.SHAPE_ROCKETS);
        break;
      case Token.spin:
        rotate(true, false);
        break;
      case Token.ssbond:
        ssbond();
        break;
      case Token.hbond:
        hbond(true);
        break;
      case Token.show:
        show();
        break;
      case Token.file:
        file();
        break;
      case Token.frame:
      case Token.model:
        frame(1);
        break;
      case Token.font:
        font(-1, 0);
        break;
      case Token.moveto:
        moveto();
        break;
      case Token.navigate:
        navigate();
        break;
      case Token.bondorder:
        bondorder();
        break;
      case Token.console:
        console();
        break;
      case Token.pmesh:
        isosurface(JmolConstants.SHAPE_PMESH);
        break;
      case Token.draw:
        draw();
        break;
      case Token.polyhedra:
        polyhedra();
        break;
      case Token.geosurface:
        dots(JmolConstants.SHAPE_GEOSURFACE);
        break;
      case Token.centerAt:
        centerAt();
        break;
      case Token.isosurface:
        isosurface(JmolConstants.SHAPE_ISOSURFACE);
        break;
      case Token.lcaocartoon:
        lcaoCartoon();
        break;
      case Token.mo:
        mo(false);
        break;
      case Token.stereo:
        stereo();
        break;
      case Token.connect:
        connect(1);
        break;
      case Token.getproperty:
        getProperty();
        break;
      case Token.configuration:
        configuration();
        break;
      case Token.axes:
        axes(1);
        break;
      case Token.boundbox:
        boundbox(1);
        break;
      case Token.unitcell:
        unitcell(1);
        break;
      case Token.frank:
        frank(1);
        break;
      case Token.help:
        help();
        break;
      case Token.save:
        save();
        break;
      case Token.restore:
        restore();
        break;
      case Token.ramachandran:
        dataFrame(JmolConstants.JMOL_DATA_RAMACHANDRAN);
        break;
      case Token.quaternion:
        dataFrame(JmolConstants.JMOL_DATA_QUATERNION);
        break;
      case Token.write:
        write(null);
        break;
      case Token.print:
        print();
        break;
      case Token.returncmd:
        returnCmd();
        break;
      case Token.pause: // resume is done differently
        pause();
        break;
      case Token.step:
        if (pause())
          stepPausedExecution();
        break;
      case Token.resume:
        if (!isSyntaxCheck)
          resumePausedExecution();
        break;
      default:
        error(ERROR_unrecognizedCommand);
      }
      if (!isSyntaxCheck)
        viewer.setCursor(Viewer.CURSOR_DEFAULT);
      // at end because we could use continue to avoid it
      if (executionStepping) {
        executionPaused = (isCommandDisplayable(pc + 1));
      }
    }
  }

  private boolean flowControl(int tok, boolean isForCheck) throws ScriptException {
    int pt = statement[0].intValue;
    boolean isDone = (pt < 0 && !isSyntaxCheck);
    boolean isOK = true;
    int ptNext = 0;
    switch (tok) {
    case Token.ifcmd:
    case Token.elseif:
      isOK = (!isDone && ifCmd());
      if (isSyntaxCheck)
        break;
      ptNext = Math.abs(aatoken[Math.abs(pt)][0].intValue);
      ptNext = (isDone || isOK ? -ptNext : ptNext);
      aatoken[Math.abs(pt)][0].intValue = ptNext;
      break;
    case Token.elsecmd:
      checkLength(1);
      if (pt < 0 && !isSyntaxCheck)
        pc = -pt - 1;
      break;
    case Token.endifcmd:
      checkLength(1);
      break;
    case Token.end: // function, if, for, while
      checkLength(2);
      if (getToken(1).tok == Token.function) {
        viewer.addFunction((ScriptFunction) theToken.value);
        return isForCheck;
      }
      isForCheck = (theTok == Token.forcmd);
      isOK = (theTok == Token.ifcmd);
      break;
    case Token.whilecmd:
      isForCheck = false;
      if (!ifCmd() && !isSyntaxCheck)
        pc = pt;
      break;
    case Token.breakcmd:
      if (!isSyntaxCheck)
        pc = aatoken[pt][0].intValue;
      if (statementLength > 1) {
        checkLength(2);
        intParameter(1);
      }
      break;
    case Token.continuecmd:
      isForCheck = true;
      if (!isSyntaxCheck)
        pc = pt - 1;
      if (statementLength > 1) {
        checkLength(2);
        intParameter(1);
      }
      break;
    case Token.forcmd:
      // for (i = 1; i < 3; i = i + 1);
      // for (var i = 1; i < 3; i = i + 1);
      // for (;;;);
      int[] pts = new int[2];
      int j = 0;
      for (int i = 1, nSkip = 0; i < statementLength && j < 2; i++) {
        switch (tokAt(i)) {
        case Token.semicolon:
          if (nSkip > 0)
            nSkip--;
          else
            pts[j++] = i;
          break;
        case Token.select:
          nSkip += 2;
          break;
        }

      }
      if (isForCheck) {
        j = pts[1] + 1;
        isForCheck = false;
      } else {
        j = 2;
        if (tokAt(j) == Token.var)
          j++;
      }
      String key = parameterAsString(j);
      if (tokAt(j) == Token.identifier || getContextVariableAsVariable(key) != null) {
        if (getToken(++j).tok != Token.opEQ)
          error(ERROR_invalidArgument);
        setVariable(++j, statementLength - 1, key, false, 0);
      }
      isOK = ((Boolean) parameterExpression(pts[0] + 1, pts[1], null, false))
          .booleanValue();
      pt++;
      break;
    }
    if (!isOK && !isSyntaxCheck)
      pc = Math.abs(pt) - 1;
    return isForCheck;
  }

  private boolean ifCmd() throws ScriptException {
    return ((Boolean) parameterExpression(1, 0, null, false)).booleanValue();
  }

  private void returnCmd() throws ScriptException {
    ScriptVariable t = getContextVariableAsVariable("_retval");
    if (t == null) {
      if (!isSyntaxCheck)
        interruptExecution = true;
      return;
    }
    Vector v = (statementLength == 1 ? null : (Vector) parameterExpression(1,
        0, null, true));
    if (isSyntaxCheck)
      return;
    ScriptVariable tv = (v == null || v.size() == 0 ? ScriptVariable
        .intVariable(0) : (ScriptVariable) v.get(0));
    t.value = tv.value;
    t.intValue = tv.intValue;
    t.tok = tv.tok;
    pcEnd = pc;
  }

  private void help() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String what = (statementLength == 1 ? "" : parameterAsString(1));
    Token t = Token.getTokenFromName(what);
    if (t != null && (t.tok & Token.command) != 0)
      what = "?command=" + what;
    viewer.getHelp(what);
  }

  private void move() throws ScriptException {
    if (statementLength > 11)
      error(ERROR_badArgumentCount);
    // rotx roty rotz zoom transx transy transz slab seconds fps
    Vector3f dRot = new Vector3f(floatParameter(1), floatParameter(2),
        floatParameter(3));
    float dZoom = floatParameter(4);
    Vector3f dTrans = new Vector3f(intParameter(5), intParameter(6),
        intParameter(7));
    float dSlab = floatParameter(8);
    float floatSecondsTotal = floatParameter(9);
    int fps = (statementLength == 11 ? intParameter(10) : 30);
    if (isSyntaxCheck)
      return;
    refresh();
    viewer.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
  }

  private void moveto() throws ScriptException {
    // moveto time
    // moveto [time] { x y z deg} zoom xTrans yTrans (rotCenter) rotationRadius
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} 0 xTrans yTrans (rotCenter) [zoom factor]
    // (navCenter) xNav yNav navDepth
    // moveto [time] { x y z deg} (rotCenter) [zoom factor] (navCenter) xNav
    // yNav navDepth
    // where zoom factor is z [[+|-|*|/] n] including 0
    // moveto [time] front|back|left|right|top|bottom
    if (statementLength == 2 && isFloatParameter(1)) {
      float f = floatParameter(1);
      if (isSyntaxCheck)
        return;
      if (f > 0)
        refresh();
      viewer.moveTo(f, null, JmolConstants.axisZ, 0, 100, 0, 0, 0, null,
          Float.NaN, Float.NaN, Float.NaN);
      return;
    }
    Vector3f axis = new Vector3f();
    Point3f center = null;
    int i = 1;
    float floatSecondsTotal = (isFloatParameter(i) ? floatParameter(i++) : 2.0f);
    float degrees = 90;
    BitSet bsCenter = null;
    switch (getToken(i).tok) {
    case Token.quaternion:
      i++;
      Quaternion q;
      if(tokAt(i) == Token.bitset || tokAt(i) == Token.expressionBegin) {
        center = centerParameter(i);
        if (!(expressionResult instanceof BitSet))
          error(ERROR_invalidArgument);  
        bsCenter = (BitSet) expressionResult;
        q = (isSyntaxCheck ? new Quaternion() : getAtomQuaternion(viewer, bsCenter));
      } else {
        q = new Quaternion(getPoint4f(i));
      }
      i = iToken + 1;      
      if (q == null)
        error(ERROR_invalidArgument);
      AxisAngle4f aa = q.toAxisAngle4f();
      axis.set(aa.x, aa.y, aa.z);
      /*
       * The quaternion angle represents the angle by which the reference
       * frame must be rotated to match the frame defined for the residue.
       * However, to "moveTo" this frame as the REFERENCE frame, what
       * we have to do is take that quaternion frame and rotate it
       * BACKWARD by that many degrees. Then it will match the reference
       * frame, which is ultimately our window frame.
       * 
       */
      degrees = -(float)(aa.angle * 180 / Math.PI);
      break;
    case Token.point3f:
    case Token.leftbrace:
      // {X, Y, Z} deg or {x y z deg}
      if (isPoint3f(i)) {
        axis.set(getPoint3f(i, true));
        i = iToken + 1;
        degrees = floatParameter(i++);
      } else {
        Point4f pt4 = getPoint4f(i);
        i = iToken + 1;
        axis.set(pt4.x, pt4.y, pt4.z);
        degrees = pt4.w;
      }
      break;
    case Token.front:
      axis.set(1, 0, 0);
      degrees = 0f;
      i++;
      break;
    case Token.back:
      axis.set(0, 1, 0);
      degrees = 180f;
      i++;
      break;
    case Token.left:
      axis.set(0, 1, 0);
      i++;
      break;
    case Token.right:
      axis.set(0, -1, 0);
      i++;
      checkLength(i);
      break;
    case Token.top:
      axis.set(1, 0, 0);
      i++;
      checkLength(i);
      break;
    case Token.bottom:
      axis.set(-1, 0, 0);
      i++;
      checkLength(i);
      break;
    default:
      // X Y Z deg
      axis = new Vector3f(floatParameter(i++), floatParameter(i++),
          floatParameter(i++));
      degrees = floatParameter(i++);
    }

    boolean isChange = !viewer.isInPosition(axis, degrees);
    // optional zoom 
    float zoom = (isFloatParameter(i) ? floatParameter(i++) : Float.NaN);
    // optional xTrans yTrans
    float xTrans = 0;
    float yTrans = 0;
    if (isFloatParameter(i) && !isCenterParameter(i)) {
      xTrans = floatParameter(i++);
      yTrans = floatParameter(i++);
      if (!isChange && Math.abs(xTrans - viewer.getTranslationXPercent()) >= 1)
        isChange = true;
      if (!isChange && Math.abs(yTrans - viewer.getTranslationYPercent()) >= 1)
        isChange = true;
    }
    if (bsCenter == null && i != statementLength) {
      // if any more, required (center)
      center = centerParameter(i);
      if (expressionResult instanceof BitSet)
        bsCenter = (BitSet) expressionResult;
      i = iToken + 1;
    }
    float rotationRadius = Float.NaN;
    float zoom0 = viewer.getZoomSetting();
    if (center != null) {
      if (!isChange && center.distance(viewer.getRotationCenter()) >= 0.1)
        isChange = true;
      // optional {center} rotationRadius
      if (isFloatParameter(i))
        rotationRadius = floatParameter(i++);
      if (!isCenterParameter(i)) {
        if ((rotationRadius == 0 || Float.isNaN(rotationRadius))
            && (zoom == 0 || Float.isNaN(zoom))) {
          // alternative (atom expression) 0 zoomFactor
          float newZoom = Math.abs(getZoom(i, bsCenter, (zoom == 0 ? 0 : zoom0)));
          i = iToken + 1;
          zoom = newZoom;
        } else {
          if (!isChange
              && Math.abs(rotationRadius - viewer.getRotationRadius()) >= 0.1)
            isChange = true;
        }
      }
    }
    if (zoom == 0 || Float.isNaN(zoom))
      zoom = 100;
    if (Float.isNaN(rotationRadius))
      rotationRadius = 0;

    if (!isChange && Math.abs(zoom - zoom0) >= 1)
      isChange = true;
    // (navCenter) xNav yNav navDepth

    Point3f navCenter = null;
    float xNav = Float.NaN;
    float yNav = Float.NaN;
    float navDepth = Float.NaN;

    if (i != statementLength) {
      navCenter = centerParameter(i);
      i = iToken + 1;
      if (i != statementLength) {
        xNav = floatParameter(i++);
        yNav = floatParameter(i++);
      }
      if (i != statementLength)
        navDepth = floatParameter(i++);
    }

    if (i != statementLength)
      error(ERROR_badArgumentCount);

    if (isSyntaxCheck)
      return;
    if (!isChange)
      floatSecondsTotal = 0;
    if (floatSecondsTotal > 0)
      refresh();
    viewer.moveTo(floatSecondsTotal, center, axis, degrees, zoom, xTrans, yTrans,
        rotationRadius, navCenter, xNav, yNav, navDepth);
  }

  private void navigate() throws ScriptException {
    /*
     * navigation on/off navigation depth p # would be as a depth value, like
     * slab, in percent, but could be negative navigation nSec translate X Y #
     * could be percentages navigation nSec translate $object # could be a draw
     * object navigation nSec translate (atom selection) #average of values
     * navigation nSec center {x y z} navigation nSec center $object navigation
     * nSec center (atom selection) navigation nSec path $object navigation nSec
     * path {x y z theta} {x y z theta}{x y z theta}{x y z theta}... navigation
     * nSec trace (atom selection)
     */
    if (statementLength == 1) {
      setBooleanProperty("navigationMode", true);
      return;
    }
    Vector3f rotAxis = new Vector3f(0, 1, 0);
    Point3f pt;
    if (statementLength == 2) {
      switch (getToken(1).tok) {
      case Token.on:
      case Token.off:
        if (isSyntaxCheck)
          return;
        viewer.setObjectMad(JmolConstants.SHAPE_AXES, "axes", 1);
        setShapeProperty(JmolConstants.SHAPE_AXES, "position", new Point3f(50, 50, Float.MAX_VALUE));
        setBooleanProperty("navigationMode", true);
        viewer.setNavOn(theTok == Token.on);
        return;
      case Token.string:
      case Token.identifier:
        String cmd = parameterAsString(1);
        if (cmd.equalsIgnoreCase("stop")) {
          if (!isSyntaxCheck)
            viewer.setNavXYZ(0, 0, 0);
          return;
        }
        break;
      case Token.point3f:
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (!viewer.getNavigationMode())
      setBooleanProperty("navigationMode", true);
    for (int i = 1; i < statementLength; i++) {
      float timeSec = (isFloatParameter(i) ? floatParameter(i++) : 2f);
      if (timeSec < 0)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck && timeSec > 0)
        refresh();
      switch (getToken(i).tok) {
      case Token.point3f:
      case Token.leftbrace:
        // navigate {x y z}
        pt = getPoint3f(i, true);
        iToken++;
        if (iToken != statementLength)
          error(ERROR_invalidArgument);
        if (isSyntaxCheck)
          return;
        viewer.setNavXYZ(pt.x, pt.y, pt.z);
        return;
      case Token.depth:
        float depth = floatParameter(++i);
        if (!isSyntaxCheck)
          viewer.setNavigationDepthPercent(timeSec, depth);
        continue;
      case Token.center:
        pt = centerParameter(++i);
        i = iToken;
        if (!isSyntaxCheck)
          viewer.navigate(timeSec, pt);
        continue;
      case Token.rotate:
        switch (getToken(++i).tok) {
        case Token.identifier:
          String str = parameterAsString(i++);
          if (str.equalsIgnoreCase("x")) {
            rotAxis.set(1, 0, 0);
            break;
          }
          if (str.equalsIgnoreCase("y")) {
            rotAxis.set(0, 1, 0);
            break;
          }
          if (str.equalsIgnoreCase("z")) {
            rotAxis.set(0, 0, 1);
            break;
          }
          error(ERROR_invalidArgument); // for now
          break;
        case Token.point3f:
        case Token.leftbrace:
          rotAxis.set(getPoint3f(i, true));
          i = iToken + 1;
          break;
        }
        float degrees = floatParameter(i);
        if (!isSyntaxCheck)
          viewer.navigate(timeSec, rotAxis, degrees);
        continue;
      case Token.translate:
        float x = Float.NaN;
        float y = Float.NaN;
        if (isFloatParameter(++i)) {
          x = floatParameter(i);
          y = floatParameter(++i);
        } else if (getToken(i).tok == Token.identifier) {
          String str = parameterAsString(i);
          if (str.equalsIgnoreCase("x"))
            x = floatParameter(++i);
          else if (str.equalsIgnoreCase("y"))
            y = floatParameter(++i);
          else
            error(ERROR_invalidArgument);
        } else {
          pt = centerParameter(i);
          i = iToken;
          if (!isSyntaxCheck)
            viewer.navTranslate(timeSec, pt);
          continue;
        }
        if (!isSyntaxCheck)
          viewer.navTranslatePercent(timeSec, x, y);
        continue;
      case Token.divide:
        continue;
      case Token.trace:
        Point3f[][] pathGuide;
        Vector vp = new Vector();
        BitSet bs = expression(++i);
        i = iToken;
        if (isSyntaxCheck)
          return;
        viewer.getPolymerPointsAndVectors(bs, vp);
        int n;
        if ((n = vp.size()) > 0) {
          pathGuide = new Point3f[n][];
          for (int j = 0; j < n; j++) {
            pathGuide[j] = (Point3f[]) vp.get(j);
          }
          viewer.navigate(timeSec, pathGuide);
          continue;
        }
        break;
      case Token.surface:
        if (i != 1)
          error(ERROR_invalidArgument);
        if (isSyntaxCheck)
          return;
        viewer.navigateSurface(timeSec, optParameterAsString(2));
        continue;
      case Token.identifier:
        Point3f[] path;
        float[] theta = null; // orientation; null for now
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("path")) {
          if (getToken(i + 1).tok == Token.dollarsign) {
            i++;
            // navigate timeSeconds path $id indexStart indexEnd
            String pathID = objectNameParameter(++i);
            if (isSyntaxCheck)
              return;
            setShapeProperty(JmolConstants.SHAPE_DRAW, "thisID", pathID);
            path = (Point3f[]) viewer.getShapeProperty(
                JmolConstants.SHAPE_DRAW, "vertices");
            refresh();
            if (path == null)
              error(ERROR_invalidArgument);
            int indexStart = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
                : 0);
            int indexEnd = (int) (isFloatParameter(i + 1) ? floatParameter(++i)
                : Integer.MAX_VALUE);
            if (!isSyntaxCheck)
              viewer.navigate(timeSec, path, theta, indexStart, indexEnd);
            continue;
          }
          Vector v = new Vector();
          while (isCenterParameter(i + 1)) {
            v.addElement(centerParameter(++i));
            i = iToken;
          }
          if (v.size() > 0) {
            path = new Point3f[v.size()];
            for (int j = 0; j < v.size(); j++) {
              path[j] = (Point3f) v.get(j);
            }
            if (!isSyntaxCheck)
              viewer.navigate(timeSec, path, theta, 0, Integer.MAX_VALUE);
            continue;
          }
          // possibility here of multiple coord4s?
        }
        // fall through;
      default:
        error(ERROR_invalidArgument);
      }
    }
  }

  private void bondorder() throws ScriptException {
    checkLength(-3);
    short order = 0;
    switch (getToken(1).tok) {
    case Token.integer:
    case Token.decimal:
      if ((order = JmolConstants.getBondOrderFromFloat(floatParameter(1))) == JmolConstants.BOND_ORDER_NULL)
        error(ERROR_invalidArgument);
      break;
    default:
      if ((order = JmolConstants.getBondOrderFromString(parameterAsString(1))) == JmolConstants.BOND_ORDER_NULL)
        error(ERROR_invalidArgument);
      // generic partial can be indicated by "partial n.m"
      if (order == JmolConstants.BOND_PARTIAL01 && tokAt(2) == Token.decimal) {
        order = JmolConstants
            .getPartialBondOrderFromInteger(statement[2].intValue);
      }
    }
    setShapeProperty(JmolConstants.SHAPE_STICKS, "bondOrder", new Short(order));
  }

  private void console() throws ScriptException {
    switch (getToken(1).tok) {
    case Token.off:
      if (!isSyntaxCheck)
        viewer.showConsole(false);
      break;
    case Token.on:
      if (isSyntaxCheck)
        break;
      viewer.showConsole(true);
      viewer.clearConsole();
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void centerAt() throws ScriptException {
    String relativeTo = null;
    switch (getToken(1).tok) {
    case Token.absolute:
      relativeTo = "absolute";
      break;
    case Token.average:
      relativeTo = "average";
      break;
    case Token.boundbox:
      relativeTo = "boundbox";
      break;
    default:
      error(ERROR_invalidArgument);
    }
    Point3f pt = new Point3f(0, 0, 0);
    if (statementLength == 5) {
      // centerAt xxx x y z
      pt.x = floatParameter(2);
      pt.y = floatParameter(3);
      pt.z = floatParameter(4);
    } else if (isCenterParameter(2)) {
      pt = centerParameter(2);
      checkLength(iToken + 1);
    } else {
      checkLength(2);
    }
    if (!isSyntaxCheck)
      viewer.setCenterAt(relativeTo, pt);
  }

  private void stereo() throws ScriptException {
    int stereoMode = JmolConstants.STEREO_DOUBLE;
    // see www.usm.maine.edu/~rhodes/0Help/StereoViewing.html
    // stereo on/off
    // stereo color1 color2 6
    // stereo redgreen 5

    float degrees = TransformManager.DEFAULT_STEREO_DEGREES;
    boolean degreesSeen = false;
    int[] colors = null;
    int colorpt = 0;
    for (int i = 1; i < statementLength; ++i) {
      if (isColorParam(i)) {
        if (colorpt > 1)
          error(ERROR_badArgumentCount);
        if (colorpt == 0)
          colors = new int[2];
        if (!degreesSeen)
          degrees = 3;
        colors[colorpt] = getArgbParam(i);
        if (colorpt++ == 0)
          colors[1] = ~colors[0];
        i = iToken;
        continue;
      }
      switch (getToken(i).tok) {
      case Token.on:
        checkLength(2);
        iToken = 1;
        break;
      case Token.off:
        checkLength(2);
        iToken = 1;
        stereoMode = JmolConstants.STEREO_NONE;
        break;
      case Token.integer:
      case Token.decimal:
        degrees = floatParameter(i);
        degreesSeen = true;
        break;
      case Token.identifier:
        if (!degreesSeen)
          degrees = 3;
        stereoMode = JmolConstants.getStereoMode(parameterAsString(i));
        if (stereoMode != JmolConstants.STEREO_UNKNOWN)
          break;
        // fall into
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (isSyntaxCheck)
      return;
    viewer.setStereoMode(colors, stereoMode, degrees);
  }

  private void connect(int index) throws ScriptException {

    final float[] distances = new float[2];
    BitSet[] atomSets = new BitSet[2];
    atomSets[0] = atomSets[1] = viewer.getSelectionSet();
    float radius = Float.NaN;
    int color = Integer.MIN_VALUE;
    int distanceCount = 0;
    short bondOrder = JmolConstants.BOND_ORDER_NULL;
    short bo;
    int operation = JmolConstants.CONNECT_MODIFY_OR_CREATE;
    boolean isDelete = false;
    boolean haveType = false;
    boolean haveOperation = false;
    String translucency = null;
    float translucentLevel = Float.MAX_VALUE;
    boolean isColorOrRadius = false;
    int nAtomSets = 0;
    int nDistances = 0;
    BitSet bsBonds = new BitSet();
    boolean isBonds = false;
    int expression2 = 0;
    /*
     * connect [<=2 distance parameters] [<=2 atom sets] [<=1 bond type] [<=1
     * operation]
     */

    if (statementLength == 1) {
      viewer.rebond();
      return;
    }

    for (int i = index; i < statementLength; ++i) {
      if (isColorParam(i)) {
        color = getArgbParam(i);
        i = iToken;
        isColorOrRadius = true;
        continue;
      }
      switch (getToken(i).tok) {
      case Token.on:
      case Token.off:
        checkLength(2);
        if (!isSyntaxCheck)
          viewer.rebond();
        return;
      case Token.integer:
      case Token.decimal:
        if (nAtomSets > 0) {
          if (haveType || isColorOrRadius)
            error(ERROR_invalidParameterOrder);
          bo = JmolConstants.getBondOrderFromFloat(floatParameter(i));
          if (bo == JmolConstants.BOND_ORDER_NULL)
            error(ERROR_invalidArgument);
          bondOrder = bo;
          haveType = true;
          break;
        }
        if (++nDistances > 2)
          error(ERROR_badArgumentCount);
        distances[distanceCount++] = floatParameter(i);
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (nAtomSets > 2 || isBonds && nAtomSets > 0)
          error(ERROR_badArgumentCount);
        if (haveType || isColorOrRadius)
          error(ERROR_invalidParameterOrder);
        atomSets[nAtomSets++] = expression(i);
        isBonds = isBondSet;
        if (nAtomSets == 2) {
          int pt = iToken;
          for (int j = i; j < pt; j++)
            if (tokAt(j) == Token.identifier
                && parameterAsString(j).equals("_1")) {
              expression2 = i;
              break;
            }
          iToken = pt;
        }
        i = iToken;
        break;
      case Token.identifier:
      case Token.hbond:
        String cmd = parameterAsString(i);
        if (cmd.equalsIgnoreCase("pdb")) {
          boolean isAuto = (optParameterAsString(2).equalsIgnoreCase("auto"));
          if (isAuto)
            checkLength(3);
          else
            checkLength(2);
          if (isSyntaxCheck)
            return;
          viewer.setPdbConectBonding(isAuto);
          return;
        }
        if ((bo = JmolConstants.getBondOrderFromString(cmd)) == JmolConstants.BOND_ORDER_NULL) {
          // must be an operation and must be last argument
          haveOperation = true;
          if (++i != statementLength)
            error(ERROR_invalidParameterOrder);
          if ((operation = JmolConstants.connectOperationFromString(cmd)) < 0)
            error(ERROR_invalidArgument);
          if (operation == JmolConstants.CONNECT_AUTO_BOND
              && !(bondOrder == JmolConstants.BOND_ORDER_NULL
                  || bondOrder == JmolConstants.BOND_H_REGULAR || bondOrder == JmolConstants.BOND_AROMATIC))
            error(ERROR_invalidArgument);
          break;
        }
        // must be bond type
        if (haveType)
          error(ERROR_incompatibleArguments);
        haveType = true;
        if (bo == JmolConstants.BOND_PARTIAL01) {
          switch (tokAt(i + 1)) {
          case Token.decimal:
            bo = JmolConstants
                .getPartialBondOrderFromInteger(statement[++i].intValue);
            break;
          case Token.integer:
            bo = (short) intParameter(++i);
            break;
          }
        }
        bondOrder = bo;
        break;
      case Token.translucent:
      case Token.opaque:
        if (translucency != null)
          error(ERROR_invalidArgument);
        isColorOrRadius = true;
        translucency = parameterAsString(i);
        if (theTok == Token.translucent && isFloatParameter(i + 1))
          translucentLevel = getTranslucentLevel(++i);
        break;
      case Token.radius:
        radius = floatParameter(++i);
        isColorOrRadius = true;
        break;
      case Token.none:
      case Token.delete:
        if (++i != statementLength)
          error(ERROR_invalidParameterOrder);
        operation = JmolConstants.CONNECT_DELETE_BONDS;
        if (isColorOrRadius)
          error(ERROR_invalidArgument);
        isDelete = true;
        break;
      default:
        error(ERROR_invalidArgument);
      }
    }
    if (isSyntaxCheck)
      return;
    if (distanceCount < 2) {
      if (distanceCount == 0)
        distances[0] = JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE;
      distances[1] = distances[0];
      distances[0] = JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE;
    }
    if (translucency != null || !Float.isNaN(radius)
        || color != Integer.MIN_VALUE) {
      if (!haveType)
        bondOrder = JmolConstants.BOND_ORDER_ANY;
      if (!haveOperation)
        operation = JmolConstants.CONNECT_MODIFY_ONLY;
    }
    int nNew = 0;
    int nModified = 0;
    int[] result;
    if (expression2 > 0) {
      BitSet bs = new BitSet();
      definedAtomSets.put("_1", bs);
      for (int atom1 = atomSets[0].size(); atom1 >= 0; atom1--)
        if (atomSets[0].get(atom1)) {
          bs.set(atom1);
          result = viewer.makeConnections(distances[0], distances[1],
              bondOrder, operation, bs, expression(expression2), bsBonds,
              isBonds);
          nNew += result[0];
          nModified += result[1];
          bs.clear(atom1);
        }
    } else {
      result = viewer.makeConnections(distances[0], distances[1], bondOrder,
          operation, atomSets[0], atomSets[1], bsBonds, isBonds);
      nNew += result[0];
      nModified += result[1];
    }
    if (isDelete) {
      if (!(tQuiet || scriptLevel > scriptReportingLevel))
        scriptStatusOrBuffer(GT.translate("{0} connections deleted", nModified));
      return;
    }
    if (isColorOrRadius) {
      viewer.selectBonds(bsBonds);
      if (!Float.isNaN(radius))
        viewer.setShapeSize(JmolConstants.SHAPE_STICKS, (int) (radius * 2000),
            Float.NaN, bsBonds);
      if (color != Integer.MIN_VALUE)
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "color",
            new Integer(color), bsBonds);
      if (translucency != null) {
        if (translucentLevel == Float.MAX_VALUE)
          translucentLevel = viewer.getDefaultTranslucent();
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "translucentLevel",
            new Float(translucentLevel));
        viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "translucency",
            translucency, bsBonds);
      }
    }
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      scriptStatusOrBuffer(GT.translate("{0} new bonds; {1} modified", new Object[] {
          new Integer(nNew), new Integer(nModified) }));
  }

  private float getTranslucentLevel(int i) throws ScriptException {
    float f = floatParameter(i);
    return (theTok == Token.integer && f > 0 && f < 9 ? f + 1 : f);
  }

  private void getProperty() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String retValue = "";
    String property = optParameterAsString(1);
    String param = optParameterAsString(2);
    int tok = tokAt(2);
    BitSet bs = (tok == Token.expressionBegin || tok == Token.bitset ? expression(2)
        : null);
    int propertyID = PropertyManager.getPropertyNumber(property);
    if (property.length() > 0 && propertyID < 0) {
      property = ""; // produces a list from Property Manager
      param = "";
    } else if (propertyID >= 0 && statementLength < 3) {
      param = PropertyManager.getDefaultParam(propertyID);
      if (param.equals("(visible)")) {
        viewer.setModelVisibility();
        bs = viewer.getVisibleSet();
      }
    } else if (propertyID == PropertyManager.PROP_FILECONTENTS_PATH) {
      for (int i = 3; i < statementLength; i++)
        param += parameterAsString(i);
    }
    retValue = (String) viewer.getProperty("readable", property,
        (bs == null ? (Object) param : (Object) bs));
    showString(retValue);
  }

  private void background(int i) throws ScriptException {
    getToken(i);
    int argb;
    if (theTok == Token.image) {
      // background IMAGE "xxxx.jpg"
      checkLength(3);
      if (isSyntaxCheck)
        return;
      Hashtable htParams = new Hashtable();
      String file = parameterAsString(++i);
      Object image = (Image) null;
      if (!file.equalsIgnoreCase("none") && file.length() > 0)
        image = viewer.getFileAsImage(file, htParams);
      if (image instanceof String)
        evalError((String) image, null);
      viewer.setBackgroundImage((String) htParams.get("fullPathName"),
          (Image) image);
      return;
    }
    if (isColorParam(i) || theTok == Token.none) {
      argb = getArgbParamLast(i, true);
      if (!isSyntaxCheck)
        viewer.setObjectArgb("background", argb);
      return;
    }
    int iShape = getShapeType(theTok);
    colorShape(iShape, i + 1, true);
  }

  private void center(int i) throws ScriptException {
    // from center (atom) or from zoomTo under conditions of not
    // windowCentered()
    if (statementLength == 1) {
      viewer.setNewRotationCenter((Point3f) null);
      return;
    }
    Point3f center = centerParameter(i);
    if (center == null)
      error(ERROR_invalidArgument);
    if (!isSyntaxCheck)
      viewer.setNewRotationCenter(center);
  }

  private String setObjectProperty() throws ScriptException {
    String s = "";
    String id = getShapeNameParameter(2);
    if (isSyntaxCheck)
      return "";
    int iTok = iToken;
    int tokCommand = tokAt(0);
    boolean isWild = TextFormat.isWild(id);
    for (int iShape = JmolConstants.SHAPE_DIPOLES;;) {
      if (iShape != JmolConstants.SHAPE_MO
          && viewer.getShapeProperty(iShape, "checkID:" + id) != null) {
        setShapeProperty(iShape, "thisID", id);
        switch (tokCommand) {
        case Token.delete:
          setShapeProperty(iShape, "delete", null);
          break;
        case Token.hide:
        case Token.display:
          setShapeProperty(iShape, "hidden",
              tokCommand == Token.display ? Boolean.FALSE : Boolean.TRUE);
          break;
        case Token.show:
          if (iShape == JmolConstants.SHAPE_ISOSURFACE && !isWild)
            return getIsosurfaceJvxl();
          s += (String) viewer.getShapeProperty(iShape, "command") + "\n";
        case Token.color:
          colorShape(iShape, iTok + 1, false);
          break;
        }
        if (!isWild)
          break;
      }
      if (iShape == JmolConstants.SHAPE_DIPOLES)
        iShape = JmolConstants.SHAPE_MAX_HAS_ID;
      if (--iShape < JmolConstants.SHAPE_MIN_HAS_ID)
        break;
    }
    return s;
  }

  private void color() throws ScriptException {
    int argb = 0;
    if (isColorParam(1)) {
      colorObject(Token.atoms, 1);
      return;
    }
    switch (getToken(1).tok) {
    case Token.dollarsign:
      setObjectProperty();
      return;
    case Token.none:
    case Token.spacefill:
    case Token.amino:
    case Token.chain:
    case Token.group:
    case Token.shapely:
    case Token.structure:
    case Token.temperature:
    case Token.fixedtemp:
    case Token.formalCharge:
    case Token.partialCharge:
    case Token.straightness:
    case Token.surfacedistance:
    case Token.vanderwaals:
    case Token.monomer:
    case Token.molecule:
    case Token.altloc:
    case Token.insertion:
    case Token.translucent:
    case Token.opaque:
    case Token.jmol:
    case Token.rasmol:
    case Token.symop:
    case Token.user:
    case Token.property:
      colorObject(Token.atoms, 1);
      return;
    case Token.string:
      String strColor = stringParameter(1);
      setStringProperty("propertyColorSchemeOverLoad", strColor);
      if (tokAt(2) == Token.range || tokAt(2) == Token.absolute) {
        float min = floatParameter(3);
        float max = floatParameter(4);
        if (!isSyntaxCheck)
          viewer.setCurrentColorRange(min, max);
      }
      return;
    case Token.range:
    case Token.absolute:
      checkLength(4);
      float min = floatParameter(2);
      float max = floatParameter(3);
      if (!isSyntaxCheck)
        viewer.setCurrentColorRange(min, max);
      return;
    case Token.background:
      argb = getArgbParamLast(2, true);
      if (!isSyntaxCheck)
        viewer.setObjectArgb("background", argb);
      return;
    case Token.bitset:
    case Token.expressionBegin:
      colorObject(Token.atoms, -1);
      return;
    case Token.rubberband:
      argb = getArgbParamLast(2, false);
      if (!isSyntaxCheck)
        viewer.setRubberbandArgb(argb);
      return;
    case Token.selectionHalo:
      int i = 2;
      if (tokAt(2) == Token.opaque)
        i++;
      argb = getArgbParamLast(i, true);
      if (isSyntaxCheck)
        return;
      viewer.loadShape(JmolConstants.SHAPE_HALOS);
      setShapeProperty(JmolConstants.SHAPE_HALOS, "argbSelection", new Integer(
          argb));
      return;
    case Token.axes:
    case Token.boundbox:
    case Token.unitcell:
    case Token.identifier:
    case Token.hydrogen:
      // color element
      String str = parameterAsString(1);
      if (checkToken(2)) {
        switch (getToken(2).tok) {
        case Token.rasmol:
          argb = Token.rasmol;
          break;
        case Token.none:
        case Token.jmol:
          argb = Token.jmol;
          break;
        default:
          argb = getArgbParam(2);
        }
      }
      if (argb == 0)
        error(ERROR_colorOrPaletteRequired);
      checkLength(iToken + 1);
      if (str.equalsIgnoreCase("axes")) {
        setStringProperty("axesColor", Escape.escapeColor(argb));
        return;
      } else if (StateManager.getObjectIdFromName(str) >= 0) {
        if (!isSyntaxCheck)
          viewer.setObjectArgb(str, argb);
        return;
      }
      if (changeElementColor(str, argb))
        return;
      error(ERROR_invalidArgument);
      break;
    case Token.isosurface:
      setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "thisID",
          JmolConstants.PREVIOUS_MESH_ID);
      // fall through
    default:
      colorObject(theTok, 2);
    }
  }

  private boolean changeElementColor(String str, int argb) {
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase(JmolConstants.elementNameFromNumber(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = JmolConstants.altElementMax; --i >= 0;) {
      if (str.equalsIgnoreCase(JmolConstants.altElementNameFromIndex(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
              argb);
        return true;
      }
    }
    if (str.charAt(0) != '_')
      return false;
    for (int i = JmolConstants.elementNumberMax; --i >= 0;) {
      if (str.equalsIgnoreCase("_" + JmolConstants.elementSymbolFromNumber(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(i, argb);
        return true;
      }
    }
    for (int i = JmolConstants.altElementMax; --i >= JmolConstants.firstIsotope;) {
      if (str
          .equalsIgnoreCase("_" + JmolConstants.altElementSymbolFromIndex(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
              argb);
        return true;
      }
      if (str
          .equalsIgnoreCase("_" + JmolConstants.altIsotopeSymbolFromIndex(i))) {
        if (!isSyntaxCheck)
          viewer.setElementArgb(JmolConstants.altElementNumberFromIndex(i),
              argb);
        return true;
      }
    }
    return false;
  }

  private void colorObject(int tokObject, int index) throws ScriptException {
    colorShape(getShapeType(tokObject), index, false);
  }

  private void colorShape(int shapeType, int index, boolean isBackground)
      throws ScriptException {
    String translucency = null;
    Object colorvalue = null;
    BitSet bs = null;
    String prefix = "";
    boolean isColor = false;
    int typeMask = 0;
    float translucentLevel = Float.MAX_VALUE;
    if (index < 0) {
      bs = expression(-index);
      index = iToken + 1;
      if (isBondSet)
        shapeType = JmolConstants.SHAPE_STICKS;
    }
    if (isBackground)
      getToken(index);
    else if ((isBackground = (getToken(index).tok == Token.background)) == true)
      getToken(++index);
    if (isBackground)
      prefix = "bg";
    if (!isSyntaxCheck && shapeType == JmolConstants.SHAPE_MO && !mo(true))
      return;
    if (theTok == Token.translucent || theTok == Token.opaque) {
      translucency = parameterAsString(index++);
      if (theTok == Token.translucent && isFloatParameter(index))
        translucentLevel = getTranslucentLevel(index++);
    }
    int tok = 0;
    if (index < statementLength && tokAt(index) != Token.on
        && tokAt(index) != Token.off) {
      isColor = true;
      tok = getToken(index).tok;
      if (isColorParam(index)) {
        int argb = getArgbParam(index, false);
        colorvalue = (argb == 0 ? null : new Integer(argb));
        if (translucency == null && tokAt(index = iToken + 1) != Token.nada) {
          getToken(index);
          if (translucency == null
              && (theTok == Token.translucent || theTok == Token.opaque)) {
            translucency = parameterAsString(index);
            if (theTok == Token.translucent && isFloatParameter(index + 1))
              translucentLevel = getTranslucentLevel(++index);
          }
          // checkLength(index + 1);
          // iToken = index;
        }
      } else if (shapeType == JmolConstants.SHAPE_LCAOCARTOON) {
        iToken--; // back up one
      } else {
        // must not be a color, but rather a color SCHEME
        // this could be a problem for properties, which can't be
        // checked later -- they must be turned into a color NOW.

        // "cpk" value would be "spacefill"
        String name = parameterAsString(index).toLowerCase();
        boolean isByElement = (name.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0);
        boolean isColorIndex = (isByElement || name
            .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
        byte pid = (isColorIndex || shapeType == JmolConstants.SHAPE_ISOSURFACE ? JmolConstants.PALETTE_PROPERTY
            : tok == Token.spacefill ? JmolConstants.PALETTE_CPK
                : JmolConstants.getPaletteID(name));
        // color atoms "cpkScheme"
        if (pid == JmolConstants.PALETTE_UNKNOWN
            || (pid == JmolConstants.PALETTE_TYPE || pid == JmolConstants.PALETTE_ENERGY)
            && shapeType != JmolConstants.SHAPE_HSTICKS)
          error(ERROR_invalidArgument);
        Object data = null;
        if (pid == JmolConstants.PALETTE_PROPERTY) {
          if (isColorIndex) {
            if (!isSyntaxCheck) {
              data = getBitsetProperty(null, (isByElement ? Token.elemno
                  : Token.groupID)
                  | Token.minmaxmask, null, null, null, null, false,
                  Integer.MAX_VALUE);
            }
          } else {
            if (!isColorIndex && shapeType != JmolConstants.SHAPE_ISOSURFACE)
              index++;
            if (name.equals("property")
                && Token.tokAttr((tok = getToken(index).tok),
                    Token.atomproperty)
                && !Token.tokAttr(tok, Token.strproperty)) {
              if (!isSyntaxCheck) {
                data = getBitsetProperty(null, getToken(index++).tok
                    | Token.minmaxmask, null, null, null, null, false,
                    Integer.MAX_VALUE);
              }
            }
          }
          if (data != null && !(data instanceof float[])) {
            if (data instanceof String[]) {
              float[] fdata = new float[((String[]) data).length];
              Parser.parseFloatArray((String[]) data, null, fdata);
              data = fdata;
            } else {
              error(ERROR_invalidArgument);
            }
          }
        } else if (pid == JmolConstants.PALETTE_VARIABLE) {
          index++;
          name = parameterAsString(index++);
          data = new float[viewer.getAtomCount()];
          Parser.parseFloatArray("" + getParameter(name, false), null,
              (float[]) data);
          pid = JmolConstants.PALETTE_PROPERTY;
        }
        if (pid == JmolConstants.PALETTE_PROPERTY) {
          String scheme = (tokAt(index) == Token.string ? parameterAsString(
              index++).toLowerCase() : null);
          if (scheme != null) {
            setStringProperty("propertyColorScheme", scheme);
            isColorIndex = (scheme.indexOf(ColorEncoder.BYELEMENT_PREFIX) == 0 || scheme
                .indexOf(ColorEncoder.BYRESIDUE_PREFIX) == 0);
          }
          float min = 0;
          float max = Float.MAX_VALUE;
          if (!isColorIndex
              && (tokAt(index) == Token.absolute || tokAt(index) == Token.range)) {
            min = floatParameter(index + 1);
            max = floatParameter(index + 2);
            index += 3;
            if (min == max && shapeType == JmolConstants.SHAPE_ISOSURFACE) {
              float[] range = (float[]) viewer.getShapeProperty(shapeType,
                  "dataRange");
              if (range != null) {
                min = range[0];
                max = range[1];
              }
            } else if (min == max)
              max = Float.MAX_VALUE;
          }
          if (!isSyntaxCheck) {
            if (shapeType != JmolConstants.SHAPE_ISOSURFACE
                && max != -Float.MAX_VALUE) {
              if (data == null)
                viewer.setCurrentColorRange(name);
              else
                viewer.setCurrentColorRange((float[]) data, null);
            }
            if (max != Float.MAX_VALUE)
              viewer.setCurrentColorRange(min, max);
          }
          if (shapeType == JmolConstants.SHAPE_ISOSURFACE)
            prefix = "remap";
        } else {
          index++;
        }
        colorvalue = new Byte((byte) pid);
        checkLength(index);
      }
    }
    if (isSyntaxCheck || shapeType < 0)
      return;
    typeMask = (shapeType == JmolConstants.SHAPE_HSTICKS ? JmolConstants.BOND_HYDROGEN_MASK
        : shapeType == JmolConstants.SHAPE_SSSTICKS ? JmolConstants.BOND_SULFUR_MASK
            : shapeType == JmolConstants.SHAPE_STICKS ? JmolConstants.BOND_COVALENT_MASK
                : 0);
    if (typeMask == 0) {
      viewer.loadShape(shapeType);
      if (shapeType == JmolConstants.SHAPE_LABELS)
        setShapeProperty(JmolConstants.SHAPE_LABELS, "setDefaults", viewer
            .getNoneSelected());
    } else {
      if (bs != null) {
        viewer.selectBonds(bs);
        bs = null;
      }
      shapeType = JmolConstants.SHAPE_STICKS;
      setShapeProperty(shapeType, "type", new Integer(typeMask));
    }
    if (isColor) {
      // ok, the following five options require precalculation.
      // the state must not save them as paletteIDs, only as pure
      // color values.
      switch (tok) {
      case Token.surfacedistance:
      case Token.straightness:
        viewer.autoCalculate(tok);
        break;
      case Token.temperature:
        if (viewer.isRangeSelected())
          viewer.clearBfactorRange();
        break;
      case Token.group:
        viewer.calcSelectedGroupsCount();
        break;
      case Token.monomer:
        viewer.calcSelectedMonomersCount();
        break;
      case Token.molecule:
        viewer.calcSelectedMoleculesCount();
        break;
      }
      if (bs == null)
        viewer.setShapeProperty(shapeType, prefix + "color", colorvalue);
      else
        viewer.setShapeProperty(shapeType, prefix + "color", colorvalue, bs);
    }
    if (translucency != null)
      setShapeTranslucency(shapeType, prefix, translucency, translucentLevel,
          bs);
    if (typeMask != 0)
      viewer.setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
          JmolConstants.BOND_COVALENT_MASK));
  }

  private void setShapeTranslucency(int shapeType, String prefix,
                                    String translucency,
                                    float translucentLevel, BitSet bs) {
    if (translucentLevel == Float.MAX_VALUE)
      translucentLevel = viewer.getDefaultTranslucent();
    setShapeProperty(shapeType, "translucentLevel", new Float(translucentLevel));
    if (prefix == null)
      return;
    if (bs == null)
      setShapeProperty(shapeType, prefix + "translucency", translucency);
    else if (!isSyntaxCheck)
      viewer.setShapeProperty(shapeType, prefix + "translucency", translucency,
          bs);
  }

  private void cd() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String dir = (statementLength == 1 ? null : parameterAsString(1));
    showString(viewer.cd(dir));
  }

  private Object[] data;

  private void data() throws ScriptException {
    String dataString = null;
    String dataLabel = null;
    boolean isOneValue = false;
    int i;
    switch (iToken = statementLength) {
    case 5:
      // parameters 3 and 4 are just for the ride: [end] and ["key"]
      dataString = parameterAsString(2);
      // fall through
    case 4:
    case 2:
      dataLabel = parameterAsString(1);
      if (dataLabel.equalsIgnoreCase("clear")) {
        if (!isSyntaxCheck)
          viewer.setData(null, null, 0, 0, 0, 0, 0);
        return;
      }
      if ((i = dataLabel.indexOf("@")) >= 0) {
        dataString = "" + getParameter(dataLabel.substring(i + 1), false);
        dataLabel = dataLabel.substring(0, i).trim();
      } else if (dataString == null && (i = dataLabel.indexOf(" ")) >= 0) {
        dataString = dataLabel.substring(i + 1).trim();
        dataLabel = dataLabel.substring(0, i).trim();
        isOneValue = true;
      }
      break;
    default:
      error(ERROR_badArgumentCount);
    }
    dataLabel = dataLabel.toLowerCase();
    String dataType = dataLabel + " ";
    dataType = dataType.substring(0, dataType.indexOf(" "));
    boolean isModel = dataType.equals("model");
    boolean isAppend = dataType.equals("append");
    boolean processModel = ((isModel || isAppend) && (!isSyntaxCheck || isCmdLine_C_Option));
    if ((isModel || isAppend) && dataString == null)
      error(ERROR_invalidArgument);
    int userType = -1;
    if (processModel) {
      // only if first character is "|" do we consider "|" to be new line
      char newLine = viewer.getInlineChar();
      if (dataString.length() > 0 && dataString.charAt(0) != newLine)
        newLine = '\0';
      int modelCount = viewer.getModelCount()
          - (viewer.getFileName().equals("zapped") ? 1 : 0);
      boolean appendNew = viewer.getAppendNew();
      viewer.loadInline(dataString, newLine, isAppend);
      if (isAppend && appendNew) {
        viewer.setAnimationRange(-1, -1);
        viewer.setCurrentModelIndex(modelCount);
      }
    }
    if (isSyntaxCheck && !processModel)
      return;
    data = new Object[3];
    if (dataType.equals("element_vdw")) {
      // vdw for now
      data[0] = dataType;
      data[1] = dataString.replace(';', '\n');
      int n = JmolConstants.elementNumberMax;
      int[] eArray = new int[n + 1];
      for (int ie = 1; ie <= n; ie++)
        eArray[ie] = ie;
      data[2] = eArray;
      viewer.setData("element_vdw", data, n, 0, 0, 0, 0);
      return;
    }
    if (dataType.indexOf("data2d_") == 0) {
      // data2d someName
      data[0] = dataLabel;
      data[1] = Parser.parseFloatArray2d(dataString);
      viewer.setData(dataLabel, data, 0, 0, 0, 0, 0);
      return;
    }
    String[] tokens = Parser.getTokens(dataLabel);
    if (dataType.indexOf("property_") == 0
        && !(tokens.length == 2 && tokens[1].equals("set"))) {
      BitSet bs = viewer.getSelectionSet();
      data[0] = dataType;
      int atomNumberField = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyAtomNumberField")).intValue());
      int atomNumberFieldColumnCount = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyAtomNumberColumnCount")).intValue());
      int propertyField = (isOneValue ? Integer.MIN_VALUE : ((Integer) viewer
          .getParameter("propertyDataField")).intValue());
      int propertyFieldColumnCount = (isOneValue ? 0 : ((Integer) viewer
          .getParameter("propertyDataColumnCount")).intValue());
      if (!isOneValue && dataLabel.indexOf(" ") >= 0) {
        if (tokens.length == 3) {
          // DATA "property_whatever [atomField] [propertyField]"
          dataLabel = tokens[0];
          atomNumberField = Parser.parseInt(tokens[1]);
          propertyField = Parser.parseInt(tokens[2]);
        }
        if (tokens.length == 5) {
          // DATA
          // "property_whatever [atomField] [atomFieldColumnCount] [propertyField] [propertyDataColumnCount]"
          dataLabel = tokens[0];
          atomNumberField = Parser.parseInt(tokens[1]);
          atomNumberFieldColumnCount = Parser.parseInt(tokens[2]);
          propertyField = Parser.parseInt(tokens[3]);
          propertyFieldColumnCount = Parser.parseInt(tokens[4]);
        }
      }
      if (atomNumberField < 0)
        atomNumberField = 0;
      if (propertyField < 0)
        propertyField = 0;
      int atomCount = viewer.getAtomCount();
      int[] atomMap = null;
      BitSet bsAtoms = new BitSet(atomCount);
      if (atomNumberField > 0) {
        atomMap = new int[atomCount + 2];
        for (int j = 0; j <= atomCount; j++)
          atomMap[j] = -1;
        for (int j = 0; j < atomCount; j++) {
          if (!bs.get(j))
            continue;
          int atomNo = viewer.getAtomNumber(j);
          if (atomNo > atomCount + 1 || atomNo < 0 || bsAtoms.get(atomNo))
            continue;
          bsAtoms.set(atomNo);
          atomMap[atomNo] = j;
        }
        data[2] = atomMap;
      } else {
        data[2] = BitSetUtil.copy(bs);
      }
      data[1] = dataString;
      viewer.setData(dataType, data, atomCount, atomNumberField,
          atomNumberFieldColumnCount, propertyField, propertyFieldColumnCount);
      return;
    }
    userType = AtomCollection.getUserSettableType(dataType);
    if (userType >= 0) {
      // this is a known settable type or "property_xxxx"
      viewer.setAtomData(userType, dataType, dataString);
      return;
    }
    // this is just information to be stored.
    data[0] = dataLabel;
    data[1] = dataString;
    viewer.setData(dataType, data, 0, 0, 0, 0, 0);
  }

  private void define() throws ScriptException {
    // note that the standard definition depends upon the
    // current state. Once defined, a setName is the set
    // of atoms that matches the definition at that time.
    // adding DYMAMIC_ to the beginning of the definition
    // allows one to create definitions that are recalculated
    // whenever they are used. When used, "DYNAMIC_" is dropped
    // so, for example:
    // define DYNAMIC_what selected and visible
    // and then
    // select what
    // will return different things at different times depending
    // upon what is selected and what is visible
    // but
    // define what selected and visible
    // will evaluate the moment it is defined and then represent
    // that set of atoms forever.

    String setName = (String) getToken(1).value;
    BitSet bs = expression(2);
    if (isSyntaxCheck)
      return;
    boolean isDynamic = (setName.indexOf("dynamic_") == 0);
    if (isDynamic) {
      Token[] code = new Token[statementLength];
      for (int i = statementLength; --i >= 0;)
        code[i] = statement[i];
      definedAtomSets.put("!" + setName.substring(8), code);
      viewer.addStateScript(thisCommand, false, true);
    } else {
      definedAtomSets.put(setName, bs);
      setStringProperty("@" + setName, Escape.escape(bs));
    }
  }

  private void echo(int index, boolean isImage) throws ScriptException {
    if (isSyntaxCheck)
      return;
    String text = optParameterAsString(index);
    if (viewer.getEchoStateActive()) {
      if (isImage) {
        Hashtable htParams = new Hashtable();
        Object image = viewer.getFileAsImage(text, htParams);
        if (image instanceof String) {
          text = (String) image;
        } else {
          setShapeProperty(JmolConstants.SHAPE_ECHO, "text", htParams
              .get("fullPathName"));
          setShapeProperty(JmolConstants.SHAPE_ECHO, "image", image);
          text = null;
        }
      } else if (text.startsWith("\0")) {
        // no reporting, just screen echo
        text = text.substring(1);
        isImage = true;
      }
      if (text != null)
        setShapeProperty(JmolConstants.SHAPE_ECHO, "text", text);
    }
    if (!isImage && viewer.getRefreshing())
      showString(viewer.formatText(text));
  }

  private void message() throws ScriptException {
    checkLength(2);
    String text = parameterAsString(1);
    if (isSyntaxCheck)
      return;
    String s = viewer.formatText(text);
    if (outputBuffer == null)
      viewer.showMessage(s);
    if (!s.startsWith("_"))
      scriptStatusOrBuffer(s);
  }

  private void print() throws ScriptException {
    if (statementLength == 1)
      error(ERROR_badArgumentCount);
    String s = (String) parameterExpression(1, 0, "", false);
    if (isSyntaxCheck)
      return;
    if (outputBuffer != null)
      outputBuffer.append(s).append('\n');
    else
      viewer.showString(s, true);
  }

  private boolean pause() throws ScriptException {
    if (isSyntaxCheck)
      return false;
    String msg = optParameterAsString(1);
    if (!viewer.getBooleanProperty("_useCommandThread")) {
      //showString("Cannot pause thread when _useCommandThread = FALSE: " + msg);
      //return;
    }
    if (viewer.autoExit || !viewer.haveDisplay)
      return false;
    if (scriptLevel == 0 && pc == aatoken.length - 1) {
      viewer.scriptStatus("nothing to pause: " + msg); 
      return false;
    }
    msg = (msg.length() == 0 ? ": RESUME to continue." 
        : ": " + viewer.formatText(msg));
    pauseExecution();
    viewer.scriptStatus("script execution paused" + msg, "script paused for RESUME");
    return true;
  }

  private void label(int index) throws ScriptException {
    if (isSyntaxCheck)
      return;
    String strLabel = parameterAsString(index++);
    if (strLabel.equalsIgnoreCase("on")) {
      strLabel = viewer.getStandardLabelFormat();
    } else if (strLabel.equalsIgnoreCase("off")) {
      strLabel = null;
    }
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    viewer.setLabel(strLabel);
  }

  private void hover() throws ScriptException {
    if (isSyntaxCheck)
      return;
    String strLabel = parameterAsString(1);
    if (strLabel.equalsIgnoreCase("on"))
      strLabel = "%U";
    else if (strLabel.equalsIgnoreCase("off"))
      strLabel = null;
    viewer.loadShape(JmolConstants.SHAPE_HOVER);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "label", strLabel);
  }

  private void load() throws ScriptException {
    boolean isAppend = false;
    Vector firstLastSteps = null;
    int modelCount = viewer.getModelCount()
        - (viewer.getFileName().equals("zapped") ? 1 : 0);
    boolean appendNew = viewer.getAppendNew();
    boolean atomDataOnly = false;
    StringBuffer loadScript = new StringBuffer("load");
    int nFiles = 1;
    Hashtable htParams = new Hashtable();
    int i = 1;
    // ignore optional file format
    // String filename = "";
    String modelName = null;
    String filename;
    int tokType = 0;
    boolean needToLoad = true;
    String sOptions = "";
    if (statementLength == 1) {
      i = 0;
    } else {
      modelName = parameterAsString(1);
      if (tokAt(1) == Token.identifier || modelName.equals("fileset")) {
        // 
        if (modelName.equals("menu")) {
          checkLength(3);
          if (!isSyntaxCheck)
            viewer.setMenu(parameterAsString(2), true);
          return;
        }
        i = 2;
        loadScript.append(" " + modelName);
        isAppend = (modelName.equalsIgnoreCase("append"));
        atomDataOnly = Parser.isOneOf(modelName.toLowerCase(),
            JmolConstants.LOAD_ATOM_DATA_TYPES);
        if (atomDataOnly) {
          htParams.put("atomDataOnly", Boolean.TRUE);
          htParams.put("modelNumber", new Integer(1));
          isAppend = true;
          tokType = Token.getTokenFromName(modelName.toLowerCase()).tok;
          if (tokType == Token.vibration)
            tokType = Token.vibXyz;
        }
        if (isAppend
            && ((filename = optParameterAsString(2))
                .equalsIgnoreCase("trajectory") || filename
                .equalsIgnoreCase("models"))) {
          modelName = filename;
          loadScript.append(" " + modelName);
          i++;
        }
        if (modelName.equalsIgnoreCase("trajectory")
            || modelName.equalsIgnoreCase("models")) {
          if (modelName.equalsIgnoreCase("trajectory"))
            htParams.put("isTrajectory", Boolean.TRUE);
          if (isPoint3f(i)) {
            Point3f pt = getPoint3f(i, false);
            i = iToken + 1;
            // first last stride
            htParams.put("firstLastStep", new int[] { (int) pt.x, (int) pt.y,
                (int) pt.z });
            loadScript.append(" " + Escape.escape(pt));
          } else if (tokAt(i) == Token.bitset) {
            htParams.put("bsModels", (BitSet) getToken(i++).value);
          } else {
            htParams.put("firstLastStep", new int[] { 0, -1, 1 });
          }
        }
      } else {
        modelName = "fileset";
      }
      if (getToken(i).tok != Token.string)
        error(ERROR_filenameExpected);
    }
    // long timeBegin = System.currentTimeMillis();
    if (statementLength == i + 1) {
      if (i == 0 || (filename = parameterAsString(i)).length() == 0)
        filename = viewer.getFullPathName();
      if (filename == null) {
        zap(false);
        return;
      }
      if (filename.equals("string[]"))
        return;
    } else if (getToken(i + 1).tok == Token.leftbrace
        || theTok == Token.point3f || theTok == Token.integer
        || theTok == Token.identifier && tokAt(i + 3) != Token.coord) {
      if ((filename = parameterAsString(i++)).length() == 0)
        filename = viewer.getFullPathName();
      if (filename == null) {
        zap(false);
        return;
      }
      if (filename.equals("string[]"))
        return;
      int tok;
      if ((tok = tokAt(i)) == Token.identifier
          && parameterAsString(i).equalsIgnoreCase("manifest")) {
        String manifest = stringParameter(++i);
        htParams.put("manifest", manifest);
        sOptions += " MANIFEST " + Escape.escape(manifest);
        tok = tokAt(++i);
      }
      if (tok == Token.integer) {
        int modelNumber = intParameter(i);
        sOptions += " " + modelNumber;
        htParams.put("modelNumber", new Integer(modelNumber));
        tok = tokAt(++i);
      }
      Point3f lattice = null;
      if (tok == Token.leftbrace || tok == Token.point3f) {
        lattice = getPoint3f(i, false);
        i = iToken + 1;
        tok = tokAt(i);
      }
      boolean isPacked = false;
      if (tok == Token.identifier
          && parameterAsString(i).equalsIgnoreCase("packed")) {
        if (lattice == null)
          lattice = new Point3f(555, 555, -1);
        isPacked = true;
      }
      if (lattice != null) {
        i = iToken + 1;
        htParams.put("lattice", lattice);
        sOptions += " {" + (int) lattice.x + " " + (int) lattice.y + " "
            + (int) lattice.z + "}";
        if (isPacked) {
          htParams.put("packed", Boolean.TRUE);
          sOptions += " PACKED";
        }
        int iGroup = -1;
        float distance = 0;
        /*
         * # Jmol 11.3.9 introduces the capability of visualizing the close
         * contacts around a crystalline protein (or any other cyrstal
         * structure) that are to atoms that are in proteins in adjacent unit
         * cells or adjacent to the protein itself. The option RANGE x, where x
         * is a distance in angstroms, placed right after the braces containing
         * the set of unit cells to load does this. The distance, if a positive
         * number, is the maximum distance away from the closest atom in the {1
         * 1 1} set. If the distance x is a negative number, then -x is the
         * maximum distance from the {not symmetry} set. The difference is that
         * in the first case the primary unit cell (555) is first filled as
         * usual, using symmetry operators, and close contacts to this set are
         * found. In the second case, only the file-based atoms ( Jones-Faithful
         * operator x,y,z) are initially included, then close contacts to that
         * set are found. Depending upon the application, one or the other of
         * these options may be desirable.
         */
        if (tokAt(i) == Token.range) {
          i++;
          distance = floatParameter(i++);
          sOptions += " range " + distance;
        }
        htParams.put("symmetryRange", new Float(distance));
        if (tokAt(i) == Token.spacegroup) {
          ++i;
          String spacegroup = TextFormat.simpleReplace(parameterAsString(i++),
              "''", "\"");
          sOptions += " spacegroup " + Escape.escape(spacegroup);
          if (spacegroup.equalsIgnoreCase("ignoreOperators")) {
            iGroup = -999;
          } else {
            if (spacegroup.indexOf(",") >= 0) // Jones Faithful
              if ((lattice.x < 9 && lattice.y < 9 && lattice.z == 0))
                spacegroup += "#doNormalize=0";
            iGroup = viewer.getSymmetry().determineSpaceGroupIndex(spacegroup);
            if (iGroup == -1)
              iGroup = -2;
            htParams.put("spaceGroupName", spacegroup);
          }
          htParams.put("spaceGroupIndex", new Integer(iGroup));
        }
        if (tokAt(i) == Token.unitcell) {
          ++i;
          htParams.put("spaceGroupIndex", new Integer(iGroup));
          float[] fparams = new float[6];
          i = floatParameterSet(i, fparams);
          sOptions += " unitcell {";
          for (int j = 0; j < 6; j++)
            sOptions += (j == 0 ? "" : " ") + fparams[j];
          sOptions += "}";
          htParams.put("unitcell", fparams);
        }
      }
      if (tokAt(i) == Token.identifier
          && parameterAsString(i).equalsIgnoreCase("filter")) {
        String filter = stringParameter(++i);
        htParams.put("filter", filter);
        sOptions += " FILTER " + Escape.escape(filter);
      }
    } else {
      if (i != 2) {
        modelName = parameterAsString(i++);
        loadScript.append(" ").append(Escape.escape(modelName));
      }
      Point3f pt = null;
      BitSet bs = null;
      Vector fNames = new Vector();
      while (i < statementLength) {
        switch (tokAt(i)) {
        case Token.identifier:
          String s = parameterAsString(i);
          if (s.equalsIgnoreCase("filter")) {
            String filter = stringParameter(++i);
            htParams.put("filter", filter);
            loadScript.append(" FILTER ").append(Escape.escape(filter));
            ++i;
            continue;
          }
          error(ERROR_invalidArgument);
          break;
        case Token.coord:
          htParams.remove("isTrajectory");
          if (firstLastSteps == null) {
            firstLastSteps = new Vector();
            pt = new Point3f(0, -1, 1);
          }
          if (isPoint3f(++i)) {
            pt = getPoint3f(i, false);
            i = iToken + 1;
          } else if (tokAt(i) == Token.bitset) {
            bs = (BitSet) getToken(i).value;
            pt = null;
            i = iToken + 1;
          }
        }
        fNames.add(filename = parameterAsString(i++));
        if (pt != null) {
          firstLastSteps.addElement(new int[] { (int) pt.x, (int) pt.y,
              (int) pt.z });
          loadScript.append(" COORD " + Escape.escape(pt));
        } else if (bs != null) {
          firstLastSteps.addElement(bs);
          loadScript.append(" COORD " + Escape.escape(bs));
        }
        loadScript.append(" /*file*/").append(Escape.escape(filename));
      }
      if (firstLastSteps != null)
        htParams.put("firstLastSteps", firstLastSteps);
      nFiles = fNames.size();
      filename = "";
      String[] filenames = new String[nFiles];
      for (int j = 0; j < nFiles; j++) {
        filenames[j] = (String) fNames.get(j);
        filename += (j == 0 ? "" : "; ") + filenames[j];
      }
      if (!isSyntaxCheck || isCmdLine_C_Option) {
        viewer.openFiles(modelName, filenames, null, isAppend, htParams);
      }
      needToLoad = false;
    }
    if (needToLoad && (!isSyntaxCheck || isCmdLine_C_Option)) {
      if (filename.startsWith("@") && filename.length() > 1) {
        htParams.put("fileData", getStringParameter(filename.substring(1),
            false));
        filename = "string";
      }
      viewer.openFile(filename, htParams, null, isAppend);
      loadScript.append(" ");
      if (!filename.equals("string") && !filename.equals("string[]"))
        loadScript.append("/*file*/");
      loadScript.append(Escape.escape(modelName = (String) htParams
          .get("fullPathName")));
      loadScript.append(sOptions);
    }
    if (isSyntaxCheck && !isCmdLine_C_Option) {
      viewer.deallocateReaderThreads();
      return;
    }
    String errMsg = null;
    if (atomDataOnly) {
      errMsg = viewer.loadAtomDataAndReturnError(tokType);
    } else {
      viewer.addLoadScript(loadScript.toString());
      errMsg = viewer.createModelSetAndReturnError(isAppend);
      // int millis = (int)(System.currentTimeMillis() - timeBegin);
      // Logger.debug("!!!!!!!!! took " + millis + " ms");
    }
    if (errMsg != null && !isCmdLine_c_or_C_Option) {
      if (errMsg.indexOf("file recognized as a script file:") >= 0) {
        viewer.addLoadScript("-");
        script(Token.script);
        return;
      }
      evalError(errMsg, null);
    }

    if (isAppend && (appendNew || nFiles > 1)) {
      viewer.setAnimationRange(-1, -1);
      viewer.setCurrentModelIndex(modelCount);
    }
    if (logMessages)
      scriptStatusOrBuffer("Successfully loaded:" + modelName);
    String defaultScript = viewer.getDefaultLoadScript();
    String msg = "";
    if (defaultScript.length() > 0)
      msg += "\nUsing defaultLoadScript: " + defaultScript;
    String script = (String) viewer.getModelSetAuxiliaryInfo("jmolscript");
    if (script != null && viewer.getAllowEmbeddedScripts()) {
      msg += "\nAdding embedded #jmolscript: " + script;
      defaultScript += ";" + script;
      defaultScript = "allowEmbeddedScripts = false;" + defaultScript
          + ";allowEmbeddedScripts = true;";
    }
    if (msg.length() > 0)
      Logger.info(msg);
    if (defaultScript.length() > 0 && !isCmdLine_c_or_C_Option) // NOT checking
                                                           // embedded
      // scripts here
      runScript(defaultScript);
  }

  private String getFullPathName() throws ScriptException {
    String filename = (!isSyntaxCheck || isCmdLine_C_Option ? viewer
        .getFullPathName()
        : "test.xyz");
    if (filename == null)
      error(ERROR_invalidArgument);
    return filename;
  }

  private void dataFrame(int datatype) throws ScriptException {
    boolean isQuaternion = false;
    boolean isDraw = (tokAt(0) == Token.draw);
    int pt0 = (isDraw ? 1 : 0);
    boolean isDerivative = false;
    boolean isSecondDerivative = false;
    boolean isRamachandranRelative = false;
    int pt = statementLength - 1;
    String type = optParameterAsString(pt).toLowerCase();
    switch (datatype) {
    case JmolConstants.JMOL_DATA_RAMACHANDRAN:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = optParameterAsString(--pt).toLowerCase();
      }
      isRamachandranRelative = (pt > pt0 && type.startsWith("r"));
      type = "ramachandran" + (isRamachandranRelative ? " r" : "")
          + (isDraw ? " draw" : "");
      break;
    case JmolConstants.JMOL_DATA_QUATERNION:
      isQuaternion = true;
      // working backward this time:
      if (type.equalsIgnoreCase("draw")) {
        isDraw = true;
        type = optParameterAsString(--pt).toLowerCase();
      } 
      isDerivative = (type.startsWith("deriv") || type.startsWith("diff"));
      isSecondDerivative = (isDerivative && type.indexOf("2") > 0);
      if (isDerivative)
        pt--;
      if (type.equalsIgnoreCase("helix") || type.equalsIgnoreCase("axis")) {
        isDraw = true;
        isDerivative = true;
        pt = -1;
      }
      type = ((pt <= pt0 ? "" : optParameterAsString(pt)) + "w")
          .substring(0, 1);
      if (type.equals("a") || type.equals("r"))
        isDerivative = true;
      if (!Parser.isOneOf(type, "w;x;y;z;r;a")) // a absolute; r relative
        evalError("QUATERNION [w,x,y,z,a,r] [difference][2]", null);
      type = "quaternion " + type + (isDerivative ? " difference" : "")
          + (isSecondDerivative ? "2" : "") + (isDraw ? " draw" : "");
      break;
    }
    if (isSyntaxCheck) // just in case we later add parameter options to this
      return;
    // for now, just one frame visible
    int modelIndex = viewer.getCurrentModelIndex();
    if (modelIndex < 0)
      error(ERROR_multipleModelsNotOK, type);
    modelIndex = viewer.getJmolDataSourceFrame(modelIndex);
    if (isDraw) {
      runScript(viewer.getPdbData(modelIndex, type));
      return;
    }
    int ptDataFrame = viewer.getJmolDataFrameIndex(modelIndex, type);
    if (ptDataFrame > 0) {
      // data frame can't be 0.
      viewer.setCurrentModelIndex(ptDataFrame, true);
      // BitSet bs2 = viewer.getModelAtomBitSet(ptDataFrame);
      // bs2.and(bs);
      // need to be able to set data directly as well.
      // viewer.display(BitSetUtil.setAll(viewer.getAtomCount()), bs2, tQuiet);
      return;
    }
    String[] savedFileInfo = viewer.getFileInfo();
    boolean oldAppendNew = viewer.getAppendNew();
    viewer.setAppendNew(true);
    String data = viewer.getPdbData(modelIndex, type);
    boolean isOK = (data != null && viewer.loadInline(data, '\n', true) == null);
    viewer.setAppendNew(oldAppendNew);
    viewer.setFileInfo(savedFileInfo);
    if (!isOK)
      return;
    StateScript ss = viewer.addStateScript(type, true, false);
    int modelCount = viewer.getModelCount();
    viewer.setJmolDataFrame(type, modelIndex, modelCount - 1);
    String script;
    switch (datatype) {
    case JmolConstants.JMOL_DATA_RAMACHANDRAN:
    default:
      viewer.setFrameTitle(modelCount - 1, type + " plot for model "
          + viewer.getModelNumberDotted(modelIndex));
      script = "frame 0.0; frame last; reset;"
          + "select visible; color structure; spacefill 3.0; wireframe 0;"
          + "draw ramaAxisX" + modelCount + " {200 0 0} {-200 0 0} \"phi\";"
          + "draw ramaAxisY" + modelCount + " {0 200 0} {0 -200 0} \"psi\";"
      // + "draw ramaAxisZ" + modelCount + " {0 0 400} {0 0 0} \"" +
      // (isRamachandranRelative ? "theta" : "omega") +"\";"
      ;
      break;
    case JmolConstants.JMOL_DATA_QUATERNION:
      viewer.setFrameTitle(modelCount - 1, type + " for model "
          + viewer.getModelNumberDotted(modelIndex));
      String color = (Escape.escapeColor(viewer.getColixArgb(viewer.getColixBackgroundContrast())));
      script = "frame 0.0; frame last; reset;"
          + "select visible; wireframe 0; " + "isosurface quatSphere"
          + modelCount
          + " resolution 1.0 color " + color + " sphere 10.0 mesh nofill frontonly translucent 0.8;"
          + "draw quatAxis" + modelCount
          + "X {10 0 0} {-10 0 0} color red \"x\";" + "draw quatAxis"
          + modelCount + "Y {0 10 0} {0 -10 0} color green \"y\";"
          + "draw quatAxis" + modelCount
          + "Z {0 0 10} {0 0 -10} color blue \"z\";" + "color structure;"
          + "draw quatCenter" + modelCount + "{0 0 0} scale 0.02";
      break;
    }
    runScript(script);
    ss.setModelIndex(viewer.getCurrentModelIndex());
    viewer.setRotationRadius(isQuaternion ? 12.5f : 260f, true);
    viewer.loadShape(JmolConstants.SHAPE_ECHO);
    showString("frame " + viewer.getModelNumberDotted(modelCount - 1)
        + " created: " + type);
  }

  // measure() see monitor()

  private void monitor() throws ScriptException {
    if (statementLength == 1) {
      viewer.hideMeasurements(false);
      return;
    }
    switch (statementLength) {
    case 2:
      switch (getToken(1).tok) {
      case Token.on:
        if (!isSyntaxCheck)
          viewer.hideMeasurements(false);
        return;
      case Token.off:
        if (!isSyntaxCheck)
          viewer.hideMeasurements(true);
        return;
      case Token.delete:
        if (!isSyntaxCheck)
          viewer.clearAllMeasurements();
        return;
      case Token.string:
        if (!isSyntaxCheck)
          viewer.setMeasurementFormats(stringParameter(1));
        return;
      }
      error(ERROR_keywordExpected, "ON, OFF, DELETE");
      break;
    case 3: // measure delete N
      if (getToken(1).tok == Token.delete) {
        if (getToken(2).tok == Token.all) {
          if (!isSyntaxCheck)
            viewer.clearAllMeasurements();
        } else {
          int i = intParameter(2) - 1;
          if (!isSyntaxCheck)
            viewer.deleteMeasurement(i);
        }
        return;
      }
    }

    int nAtoms = 0;
    int expressionCount = 0;
    int modelIndex = -1;
    int atomIndex = -1;
    int ptFloat = -1;
    int[] countPlusIndexes = new int[5];
    float[] rangeMinMax = new float[] { Float.MAX_VALUE, Float.MAX_VALUE };
    boolean isAll = false;
    boolean isAllConnected = false;
    boolean isDelete = false;
    boolean isRange = true;
    boolean isON = false;
    boolean isOFF = false;
    String strFormat = null;
    Vector monitorExpressions = new Vector();
    BitSet bs = new BitSet();
    Object value = null;
    for (int i = 1; i < statementLength; ++i) {
      switch (getToken(i).tok) {
      default:
        error(ERROR_expressionOrIntegerExpected);
      case Token.on:
        if (isON || isOFF || isDelete)
          error(ERROR_invalidArgument);
        isON = true;
        continue;
      case Token.off:
        if (isON || isOFF || isDelete)
          error(ERROR_invalidArgument);
        isOFF = true;
        continue;
      case Token.delete:
        if (isON || isOFF || isDelete)
          error(ERROR_invalidArgument);
        isDelete = true;
        continue;
      case Token.range:
        isAll = true;
        isRange = true; // unnecessary
        atomIndex = -1;
        continue;
      case Token.modelindex:
        modelIndex = intParameter(++i);
        continue;
      case Token.identifier:
        if (!parameterAsString(i).equalsIgnoreCase("ALLCONNECTED"))
          error(ERROR_keywordExpected, "ALL, ALLCONNECTED, DELETE");
        isAllConnected = true;
        // fall through
      case Token.all:
        atomIndex = -1;
        isAll = true;
        continue;
      case Token.string:
        // measures "%a1 %a2 %v %u"
        strFormat = stringParameter(i);
        continue;
      case Token.decimal:
        isAll = true;
        isRange = true;
        ptFloat = (ptFloat + 1) % 2;
        rangeMinMax[ptFloat] = floatParameter(i);
        continue;
      case Token.integer:
        int iParam = intParameter(i);
        if (isAll) {
          isRange = true; // irrelevant if just four integers
          ptFloat = (ptFloat + 1) % 2;
          rangeMinMax[ptFloat] = iParam;
        } else {
          atomIndex = viewer.getAtomIndexFromAtomNumber(iParam);
          if (!isSyntaxCheck && atomIndex < 0)
            return;
          if (value != null)
            error(ERROR_invalidArgument);
          if ((countPlusIndexes[0] = ++nAtoms) > 4)
            error(ERROR_badArgumentCount);
          countPlusIndexes[nAtoms] = atomIndex;
        }
        continue;
      case Token.bitset:
      case Token.expressionBegin:
      case Token.leftbrace:
      case Token.point3f:
      case Token.dollarsign:
        if (atomIndex >= 0)
          error(ERROR_invalidArgument);
        expressionResult = Boolean.FALSE;
        value = centerParameter(i);
        if (expressionResult instanceof BitSet) {
          value = bs = (BitSet) expressionResult;
          if (!isSyntaxCheck && BitSetUtil.firstSetBit(bs) < 0)
            return;
        }
        if (value instanceof Point3f) {
          Point3fi v = new Point3fi();
          v.set((Point3f)value);
          v.modelIndex = (short) modelIndex;
          value = v;
        }
        if ((nAtoms = ++expressionCount) > 4)
          error(ERROR_badArgumentCount);
        monitorExpressions.addElement(value);
        i = iToken;
        continue;
      }
    }
    if (nAtoms < 2)
      error(ERROR_badArgumentCount);
    if (strFormat != null && strFormat.indexOf(nAtoms + ":") != 0)
      strFormat = nAtoms + ":" + strFormat;
    if (isRange && rangeMinMax[1] < rangeMinMax[0]) {
      rangeMinMax[1] = rangeMinMax[0];
      rangeMinMax[0] = (rangeMinMax[1] == Float.MAX_VALUE ? Float.MAX_VALUE
          : -200F);
    }
    if (isSyntaxCheck)
      return;
    if (value != null) {
      viewer.defineMeasurement(monitorExpressions, rangeMinMax, isDelete,
          isAll, isAllConnected, isON, isOFF, strFormat);
      return;
    }
    if (isDelete)
      viewer.deleteMeasurement(countPlusIndexes);
    else if (isON)
      viewer.showMeasurement(countPlusIndexes, true);
    else if (isOFF)
      viewer.showMeasurement(countPlusIndexes, false);
    else
      viewer.toggleMeasurement(countPlusIndexes, strFormat);
  }

  private void refresh() {
    if (isSyntaxCheck)
      return;
    viewer.setTainted(true);
    viewer.requestRepaintAndWait();
  }

  private void reset() throws ScriptException {
    checkLength(-2);
    if (isSyntaxCheck)
      return;
    if (statementLength == 1) {
      viewer.reset();
      return;
    }
    // possibly "all"
    if (tokAt(1) == Token.function) {
      viewer.clearFunctions();
      return;
    }
    if (tokAt(1) == Token.vanderwaals) {
      viewer.setData("element_vdw", new Object[] { null, "" }, 0, 0, 0, 0, 0);
      return;
    }
    String var = parameterAsString(1);
    if (var.charAt(0) == '_')
      error(ERROR_invalidArgument);
    if (var.equalsIgnoreCase("aromatic")) {
      viewer.resetAromatic();
    } else {
      viewer.unsetProperty(var);
    }
  }

  private void restrict() throws ScriptException {
    select();
    if (isSyntaxCheck)
      return;
    restrictSelected(true);
  }

  private void restrictSelected(boolean doInvert) {
    BitSet bsSelected = BitSetUtil.copy(viewer.getSelectionSet());
    if (doInvert)
      viewer.invertSelection();
    BitSet bsSubset = viewer.getSelectionSubset();
    if (doInvert && bsSubset != null) {
      bsSelected = BitSetUtil.copy(viewer.getSelectionSet());
      bsSelected.and(bsSubset);
      viewer.setSelectionSet(bsSelected);
      BitSetUtil.invertInPlace(bsSelected, viewer.getAtomCount());
      bsSelected.and(bsSubset);
    }
    BitSetUtil.andNot(bsSelected, viewer.getDeletedAtoms());
    boolean bondmode = viewer.getBondSelectionModeOr();
    setBooleanProperty("bondModeOr", true);
    setShapeSize(JmolConstants.SHAPE_STICKS, 0);

    // also need to turn off backbones, ribbons, strands, cartoons
    for (int shapeType = JmolConstants.SHAPE_MAX_SIZE_ZERO_ON_RESTRICT; --shapeType >= 0;)
      if (shapeType != JmolConstants.SHAPE_MEASURES)
        setShapeSize(shapeType, 0);
    setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "delete", null);
    viewer.setLabel(null);

    setBooleanProperty("bondModeOr", bondmode);
    viewer.setSelectionSet(bsSelected);
  }

  private void rotate(boolean isSpin, boolean isSelected)
      throws ScriptException {

    // rotate is a full replacement for spin
    // spin is DEPRECATED

    /*
     * The Chime spin method:
     * 
     * set spin x 10;set spin y 30; set spin z 10; spin | spin ON spin OFF
     * 
     * Jmol does these "first x, then y, then z" I don't know what Chime does.
     * 
     * spin and rotate are now consolidated here.
     * 
     * far simpler is
     * 
     * spin x 10 spin y 10
     * 
     * these are pure x or y spins or
     * 
     * spin axisangle {1 1 0} 10
     * 
     * this is the same as the old "spin x 10; spin y 10" -- or is it? anyway,
     * it's better!
     * 
     * note that there are many defaults
     * 
     * spin # defaults to spin y 10 spin 10 # defaults to spin y 10 spin x #
     * defaults to spin x 10
     * 
     * and several new options
     * 
     * spin -x spin axisangle {1 1 0} 10 spin 10 (atomno=1)(atomno=2) spin 20 {0
     * 0 0} {1 1 1}
     * 
     * spin MOLECULAR {0 0 0} 20
     * 
     * The MOLECULAR keyword indicates that spins or rotations are to be carried
     * out in the internal molecular coordinate frame, not the fixed room frame.
     * 
     * In the case of rotateSelected, all rotations are molecular and the
     * absense of the MOLECULAR keyword indicates to rotate about the geometric
     * center of the molecule, not {0 0 0}
     * 
     * Fractional coordinates may be indicated:
     * 
     * spin 20 {0 0 0/} {1 1 1/}
     * 
     * In association with this, TransformManager and associated functions are
     * TOTALLY REWRITTEN and consolideated. It is VERY clean now - just two
     * methods here -- one fixed and one molecular, two in Viewer, and two in
     * TransformManager. All the centering stuff has been carefully inspected
     * are reorganized as well.
     * 
     * Bob Hanson 5/21/06
     */

    if (statementLength == 2)
      switch (getToken(1).tok) {
      case Token.on:
        if (!isSyntaxCheck)
          viewer.setSpinOn(true);
        return;
      case Token.off:
        if (!isSyntaxCheck)
          viewer.setSpinOn(false);
        return;
      }

    BitSet bsAtoms = null;
    float degrees = Float.MIN_VALUE;
    int nPoints = 0;
    float endDegrees = Float.MAX_VALUE;
    boolean isMolecular = false;
    Point3f[] points = new Point3f[2];
    Vector3f rotAxis = new Vector3f(0, 1, 0);
    int direction = 1;
    int tok;
    boolean axesOrientationRasmol = viewer.getAxesOrientationRasmol();

    for (int i = 1; i < statementLength; ++i) {
      switch (tok = getToken(i).tok) {
      case Token.spin:
        isSpin = true;
        continue;
      case Token.minus:
        direction = -1;
        continue;
      case Token.quaternion:
        i++;
        // fall through
      case Token.point4f:
        Quaternion q = new Quaternion(getPoint4f(i));
        rotAxis.set(q.getNormal());
        degrees = q.getTheta();
        break;
      case Token.axisangle:
        if (isPoint3f(++i)) {
          rotAxis.set(centerParameter(i));
          break;
        }
        Point4f p4 = getPoint4f(i);
        rotAxis.set(p4.x, p4.y, p4.z);
        degrees = p4.w;
        break;
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("x")) {
          rotAxis.set(direction, 0, 0);
          continue;
        }
        if (str.equalsIgnoreCase("y")) {
          rotAxis.set(0, (axesOrientationRasmol && !isMolecular ? -direction
              : direction), 0);
          continue;
        }
        if (str.equalsIgnoreCase("z")) {
          rotAxis.set(0, 0, direction);
          continue;
        }
        if (str.equalsIgnoreCase("internal")
            || str.equalsIgnoreCase("molecular")) {
          isMolecular = true;
          continue;
        }
        error(ERROR_invalidArgument);
      case Token.branch:
        int iAtom1 = BitSetUtil.firstSetBit(expression(++i));
        int iAtom2 = BitSetUtil.firstSetBit(expression(++iToken));
        if (iAtom1 < 0 || iAtom2 < 0)
          return;
        bsAtoms = viewer.getBranchBitSet(iAtom2, iAtom1);
        isMolecular = true;
        points[0] = viewer.getAtomPoint3f(iAtom1);
        points[1] = viewer.getAtomPoint3f(iAtom2);
        nPoints = 2;
        i = iToken;
        break;
      case Token.bitset:
      case Token.expressionBegin:
      case Token.leftbrace:
      case Token.point3f:
      case Token.dollarsign:
        if (nPoints == 2) // only 2 allowed for rotation -- for now
          error(ERROR_tooManyPoints);
        // {X, Y, Z}
        // $drawObject[n]
        Point3f pt1 = centerParameter(i);
        if (!isSyntaxCheck && tok == Token.dollarsign
            && tokAt(i + 2) != Token.leftsquare)
          rotAxis = getDrawObjectAxis(objectNameParameter(++i));
        points[nPoints++] = pt1;
        break;
      case Token.comma:
        continue;
      case Token.integer:
      case Token.decimal:
        // end degrees followed by degrees per second
        if (degrees == Float.MIN_VALUE)
          degrees = floatParameter(i);
        else {
          endDegrees = degrees;
          degrees = floatParameter(i);
          if (endDegrees * degrees < 0) {
            // degrees per second here
            // but expresses as seconds now
            degrees = -endDegrees / degrees;
          }
          isSpin = true;
        }
        continue;
      default:
        error(ERROR_invalidArgument);
      }
      i = iToken;
    }
    if (isSyntaxCheck)
      return;
    if (degrees == Float.MIN_VALUE)
      degrees = 10;
    if (isSelected && bsAtoms == null)
      bsAtoms = viewer.getSelectionSet();
    if (nPoints < 2) {
      if (!isMolecular) {
        // fixed-frame rotation
        // rotate x 10 # Chime-like
        // rotate axisangle {0 1 0} 10
        // rotate x 10 (atoms) # point-centered
        // rotate x 10 $object # point-centered
        viewer.rotateAxisAngleAtCenter(points[0], rotAxis, degrees, endDegrees,
            isSpin, bsAtoms);
        return;
      }
      if (nPoints == 0)
        points[0] = new Point3f();
      // rotate MOLECULAR
      // rotate MOLECULAR (atom1)
      // rotate MOLECULAR x 10 (atom1)
      // rotate axisangle MOLECULAR (atom1)
      points[1] = new Point3f(points[0]);
      points[1].add(rotAxis);
    }
    if (points[0].distance(points[1]) == 0) {
      points[1] = new Point3f(points[0]);
      points[1].y += 1.0;
    }
    viewer.rotateAboutPointsInternal(points[0], points[1], degrees, endDegrees,
        isSpin, bsAtoms);
  }

  private Point3f getObjectCenter(String axisID, int index) {
    Point3f pt = (Point3f) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
        "getCenter:" + axisID, index);
    if (pt == null)
      pt = (Point3f) viewer.getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
          "getCenter:" + axisID, index);
    return pt;
  }

  private Vector3f getDrawObjectAxis(String axisID) {
    return (Vector3f) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
        "getSpinAxis:" + axisID);
  }

  private void script(int tok) throws ScriptException {
    boolean loadCheck = true;
    boolean isCheck = false;
    boolean doStep = false;
    int lineNumber = 0;
    int pc = 0;
    int lineEnd = 0;
    int pcEnd = 0;
    int i = 2;
    String filename = null;
    String theScript = parameterAsString(1);
    if (tok == Token.javascript) {
      checkLength(2);
      if (!isSyntaxCheck)
        viewer.jsEval(theScript);
      return;
    }
    if (theScript.equalsIgnoreCase("applet")) {
      // script APPLET x "....."
      String appID = parameterAsString(2);
      theScript = parameterExpression(3, 0, "_script", false).toString();
      checkLength(iToken + 1);
      if (isSyntaxCheck)
        return;
      if (appID.length() == 0 || appID.equals("all"))
        appID = "*";
      if (!appID.equals(".")) {
        viewer.jsEval(appID + "\1" + theScript);
        if (!appID.equals("*"))
          return;
      }
    } else {
      if (getToken(1).tok != Token.string)
        error(ERROR_filenameExpected);
      filename = theScript;
      theScript = null;
      String option = optParameterAsString(statementLength - 1);
      doStep = option.equalsIgnoreCase("step");
      if (filename.equalsIgnoreCase("inline")) {
        theScript = parameterExpression(2, (doStep ? statementLength - 1 : 0), "_script", false).toString();
        i = iToken + 1;
      }
      option = optParameterAsString(i);
      if (option.equalsIgnoreCase("check")) {
        isCheck = true;
        option = optParameterAsString(++i);
      }
      if (option.equalsIgnoreCase("noload")) {
        loadCheck = false;
        option = optParameterAsString(++i);
      }
      if (option.equalsIgnoreCase("line") || option.equalsIgnoreCase("lines")) {
        i++;
        lineEnd = lineNumber = Math.max(intParameter(i++), 0);
        if (checkToken(i))
          if (getToken(i++).tok == Token.minus)
            lineEnd = (checkToken(i) ? intParameter(i++) : 0);
          else
            error(ERROR_invalidArgument);
      } else if (option.equalsIgnoreCase("command")
          || option.equalsIgnoreCase("commands")) {
        i++;
        pc = Math.max(intParameter(i++) - 1, 0);
        pcEnd = pc + 1;
        if (checkToken(i))
          if (getToken(i++).tok == Token.minus)
            pcEnd = (checkToken(i) ? intParameter(i++) : 0);
          else
            error(ERROR_invalidArgument);
      }
      checkLength(doStep ? i + 1 : i);
    }
    if (isSyntaxCheck && !isCmdLine_c_or_C_Option)
      return;
    if (isCmdLine_c_or_C_Option)
      isCheck = true;
    boolean wasSyntaxCheck = isSyntaxCheck;
    boolean wasScriptCheck = isCmdLine_c_or_C_Option;
    if (isCheck)
      isSyntaxCheck = isCmdLine_c_or_C_Option = true;
    pushContext(null);
    contextPath += " >> " + filename;
    if (theScript == null ? compileScriptFileInternal(filename) : compileScript(null,
        theScript, false)) {
      this.pcEnd = pcEnd;
      this.lineEnd = lineEnd;
      while (pc < lineNumbers.length && lineNumbers[pc] < lineNumber)
        pc++;
      this.pc = pc;
      boolean saveLoadCheck = isCmdLine_C_Option;
      isCmdLine_C_Option &= loadCheck;
      executionStepping |= doStep;
      instructionDispatchLoop(isCheck);
      if (debugScript && viewer.getMessageStyleChime())
        viewer.scriptStatus("script <exiting>");
      isCmdLine_C_Option = saveLoadCheck;
      popContext();
    } else {
      Logger.error(GT.translate("script ERROR: ") + errorMessage);
      popContext();
      if (wasScriptCheck) {
        setErrorMessage(null);
      } else {
        evalError(null, null);
      }
    }

    isSyntaxCheck = wasSyntaxCheck;
    isCmdLine_c_or_C_Option = wasScriptCheck;
  }

  private void function() throws ScriptException {
    if (isSyntaxCheck && !isCmdLine_c_or_C_Option)
      return;
    String name = (String) getToken(0).value;
    if (!viewer.isFunction(name)) {
      if (name.equalsIgnoreCase("exitjmol")) {
        if (isSyntaxCheck || viewer.isApplet())
          return;
        viewer.exitJmol();
      }
      error(ERROR_commandExpected);
    }
    Vector params = (statementLength == 1 || statementLength == 3
        && tokAt(1) == Token.leftparen && tokAt(2) == Token.rightparen ? null
        : (Vector) parameterExpression(1, 0, null, true));
    if (isSyntaxCheck)
      return;
    pushContext(null);
    contextPath += " >> function " + name;
    loadFunction(name, params);
    instructionDispatchLoop(false);
    popContext();
  }

  private void sync() throws ScriptException {
    // new 11.3.9
    checkLength(-3);
    String text = "";
    String applet = "";
    switch (statementLength) {
    case 1:
      applet = "*";
      text = "ON";
      break;
    case 2:
      applet = parameterAsString(1);
      if (applet.indexOf("jmolApplet") == 0 || Parser.isOneOf(applet, "*;.;^")) {
        text = "ON";
        if (!isSyntaxCheck)
          viewer.syncScript(text, applet);
        applet = ".";
        break;
      }
      text = applet;
      applet = "*";
      break;
    case 3:
      applet = parameterAsString(1);
      text = (tokAt(2) == Token.stereo ? Viewer.SYNC_GRAPHICS_MESSAGE
          : parameterAsString(2));
      break;
    }
    if (isSyntaxCheck)
      return;
    viewer.syncScript(text, applet);
  }

  private void history(int pt) throws ScriptException {
    // history or set history
    if (statementLength == 1) {
      // show it
      showString(viewer.getSetHistory(Integer.MAX_VALUE));
      return;
    }
    if (pt == 2) {
      // set history n; n' = -2 - n; if n=0, then set history OFF
      checkLength(3);
      int n = intParameter(2);
      if (n < 0)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        viewer.getSetHistory(n == 0 ? 0 : -2 - n);
      return;
    }
    checkLength(2);
    switch (getToken(1).tok) {
    // pt = 1 history ON/OFF/CLEAR
    case Token.on:
    case Token.clear:
      if (!isSyntaxCheck)
        viewer.getSetHistory(Integer.MIN_VALUE);
      return;
    case Token.off:
      if (!isSyntaxCheck)
        viewer.getSetHistory(0);
      break;
    default:
      error(ERROR_keywordExpected, "ON, OFF, CLEAR");
    }
  }

  private void display(boolean isDisplay) throws ScriptException {
    if (tokAt(1) == Token.dollarsign) {
      setObjectProperty();
      return;
    }
    BitSet bs = (statementLength == 1 ? null : expression(1));
    if (isSyntaxCheck)
      return;
    if (isDisplay)
      viewer.display(bs, tQuiet);
    else
      viewer.hide(bs, tQuiet);
  }

  private void delete() throws ScriptException {
    if (statementLength == 1) {
      zap(true);
      return;
    }
    if (tokAt(1) == Token.dollarsign) {
      setObjectProperty();
      return;
    }
    BitSet bs = expression(statement, 1, 0, true, false, true, false);
    if (isSyntaxCheck)
      return;
    int nDeleted = viewer.deleteAtoms(bs, false);
    if (!(tQuiet || scriptLevel > scriptReportingLevel))
      scriptStatusOrBuffer(GT.translate("{0} atoms deleted", nDeleted));
  }

  private void minimize() throws ScriptException {
    BitSet bsSelected = null;
    int steps = Integer.MAX_VALUE;
    float crit = 0;
    MinimizerInterface minimizer = viewer.getMinimizer(false);
    // may be null
    for (int i = 1; i < statementLength; i++)
      switch (tokAt(i)) {
      case Token.clear:
        checkLength(2);
        if (isSyntaxCheck || minimizer == null)
          return;
        minimizer.setProperty("clear", null);
        return;
      case Token.constraint:
        if (i != 1)
          error(ERROR_invalidArgument);
        int n = 0;
        i++;
        float targetValue = 0;
        int[] aList = new int[5];
        if (tokAt(i) == Token.clear) {
          checkLength(2);
        } else {
          while (n < 4 && !isFloatParameter(i)) {
            aList[++n] = BitSetUtil.firstSetBit(expression(i));
            i = iToken + 1;
          }
          aList[0] = n;
          targetValue = floatParameter(i++);
          checkLength(i);
        }
        if (!isSyntaxCheck)
          viewer.getMinimizer(true).setProperty("constraint",
              new Object[] { aList, new int[n], new Float(targetValue) });
        return;
      case Token.string:
      case Token.identifier:
        String cmd = parameterAsString(i).toLowerCase();
        if (cmd.equals("stop") || cmd.equals("cancel")) {
          checkLength(2);
          if (isSyntaxCheck || minimizer == null)
            return;
          minimizer.setProperty(cmd, null);
          return;
        }
        if (cmd.equals("fix")) {
          if (i != 1)
            error(ERROR_invalidArgument);
          BitSet bsFixed = expression(++i);
          if (BitSetUtil.firstSetBit(bsFixed) < 0)
            bsFixed = null;
          checkLength(iToken + 1, 1);
          if (!isSyntaxCheck)
            viewer.getMinimizer(true).setProperty("fixed", bsFixed);
          return;
        }
        if (cmd.equals("energy")) {
          steps = 0;
          continue;
        }
        if (cmd.equals("criterion")) {
          crit = floatParameter(++i);
          continue;
        }
        if (cmd.equals("steps")) {
          steps = intParameter(++i);
          continue;
        }
        error(ERROR_invalidArgument);
        break;
      case Token.select:
        bsSelected = expression(++i);
        i = iToken;
        continue;
      }
    if (isSyntaxCheck)
      return;
    if (bsSelected == null) {
      int i = BitSetUtil.firstSetBit(viewer.getVisibleFramesBitSet());
      bsSelected = viewer.getModelAtomBitSet(i, false);
    }
    try {
      viewer.getMinimizer(true).minimize(steps, crit, bsSelected);
    } catch (Exception e) {
      evalError(e.getMessage(), null);
    }
  }

  private void select() throws ScriptException {
    // NOTE this is called by restrict()
    if (statementLength == 1) {
      viewer.select(null, tQuiet || scriptLevel > scriptReportingLevel);
      return;
    }
    if (statementLength == 2 && tokAt(1) == Token.only)
      return; // coming from "cartoon only"
    // select beginexpr none endexpr
    viewer.setNoneSelected(statementLength == 4 && tokAt(2) == Token.none);
    // select beginexpr bonds ( {...} ) endexpr
    if (tokAt(2) == Token.bitset && getToken(2).value instanceof BondSet
        || getToken(2).tok == Token.bonds && getToken(3).tok == Token.bitset) {
      if (statementLength == iToken + 2) {
        if (!isSyntaxCheck)
          viewer.selectBonds((BitSet) theToken.value);
        return;
      }
      error(ERROR_invalidArgument);
    }
    if (getToken(2).tok == Token.monitor) {
      if (statementLength == 5 && getToken(3).tok == Token.bitset) {
        if (!isSyntaxCheck)
          setShapeProperty(JmolConstants.SHAPE_MEASURES, "select",
              theToken.value);
        return;
      }
      error(ERROR_invalidArgument);
    }
    BitSet bs = null;
    if (getToken(1).intValue == 0) {
      Object v = tokenSetting(0).value;
      if (!(v instanceof BitSet))
        error(ERROR_invalidArgument);
      checkLength(++iToken);
      bs = (BitSet) v;
    } else {
      bs = expression(1);
    }
    if (isSyntaxCheck)
      return;
    if (isBondSet) {
      viewer.selectBonds(bs);
    } else {
      viewer.select(bs, tQuiet || scriptLevel > scriptReportingLevel);
    }
  }

  private void subset() throws ScriptException {
    BitSet bs = (statementLength == 1 ? null : expression(-1));
    if (isSyntaxCheck)
      return;
    // There might have been a reason to have bsSubset being set BEFORE
    // checking syntax checking, but I can't remember why.
    // will leave it this way for now. Might cause some problems with script
    // checking.
    viewer.setSelectionSubset(bs);
    // I guess we do not want to select, because that could
    // throw off picking in a strange way
    // viewer.select(bsSubset, false);
  }

  private void invertSelected() throws ScriptException {
    // invertSelected POINT
    // invertSelected PLANE
    // invertSelected HKL
    Point3f pt = null;
    Point4f plane = null;
    if (statementLength == 1) {
      if (isSyntaxCheck)
        return;
      BitSet bs = viewer.getSelectionSet();
      pt = viewer.getAtomSetCenter(bs);
      viewer.invertSelected(pt, bs);
      return;
    }
    String type = parameterAsString(1);

    if (type.equalsIgnoreCase("point")) {
      pt = centerParameter(2);
    } else if (type.equalsIgnoreCase("plane")) {
      plane = planeParameter(2);
    } else if (type.equalsIgnoreCase("hkl")) {
      plane = hklParameter(2);
    }
    checkLength(iToken + 1, 1);
    if (plane == null && pt == null)
      error(ERROR_invalidArgument);
    if (isSyntaxCheck)
      return;
    viewer.invertSelected(pt, plane);
  }

  private void translateSelected() throws ScriptException {
    // translateSelected {x y z}
    Point3f pt = getPoint3f(1, true);
    if (!isSyntaxCheck)
      viewer.setAtomCoordRelative(pt);
  }

  private void translate() throws ScriptException {
    char type = (optParameterAsString(3).toLowerCase() + '\0').charAt(0);
    checkLength(type == '\0' ? 3 : 4);
    float percent = floatParameter(2);
    if (getToken(1).tok == Token.identifier) {
      char xyz = parameterAsString(1).toLowerCase().charAt(0);
      switch (xyz) {
      case 'x':
      case 'y':
      case 'z':
        if (isSyntaxCheck)
          return;
        viewer.translate(xyz, percent, type);
        return;
      }
    }
    error(ERROR_axisExpected);
  }

  private void zap(boolean isZapCommand) throws ScriptException {
    if (statementLength == 1 || !isZapCommand) {
      viewer.zap(true, isZapCommand && !isStateScript);
      refresh();
      return;
    }
    BitSet bs = expression(1);
    if (isSyntaxCheck)
      return;
    int nDeleted = viewer.deleteAtoms(bs, true);
    boolean isQuiet = (tQuiet || scriptLevel > scriptReportingLevel);
    if (!isQuiet)
      scriptStatusOrBuffer(GT.translate("{0} atoms deleted", nDeleted));
    viewer.select(null, isQuiet);
  }

  private void zoom(boolean isZoomTo) throws ScriptException {
    if (!isZoomTo) {
      // zoom
      // zoom on|off
      int tok = (statementLength > 1 ? getToken(1).tok : Token.on);
      switch (tok) {
      case Token.on:
      case Token.off:
        if (statementLength > 2)
          error(ERROR_badArgumentCount);
        if (!isSyntaxCheck)
          setBooleanProperty("zoomEnabled", tok == Token.on);
        return;
      }
    }
    Point3f center = null;
    Point3f currentCenter = viewer.getRotationCenter();
    int i = 1;
    // zoomTo time-sec
    float time = (isZoomTo ? (isFloatParameter(i) ? floatParameter(i++) : 2f)
        : 0f);
    if (time < 0) {
      //zoom -10
      i--;
      time = 0;
    }
    // zoom {x y z} or (atomno=3)
    int ptCenter = 0;
    BitSet bsCenter = null;
    if (isCenterParameter(i)) {
      ptCenter = i;
      center = centerParameter(i);
      if (expressionResult instanceof BitSet)
        bsCenter = (BitSet) expressionResult;
      i = iToken + 1;
    }

    // disabled sameAtom stuff -- just too weird
    boolean isSameAtom = false && (center != null && currentCenter
        .distance(center) < 0.1);
    // zoom/zoomTo percent|-factor|+factor|*factor|/factor | 0
    float zoom = viewer.getZoomSetting();
    float newZoom = getZoom(i, bsCenter, zoom);
    if (iToken + 1 != statementLength)
      error(ERROR_invalidArgument);
    if (newZoom < 0) {
      newZoom = -newZoom; // currentFactor
      if (isZoomTo) {
        // no factor -- check for no center (zoom out) or same center (zoom in)
        if (statementLength == 1 || isSameAtom)
          newZoom *= 2;
        else if (center == null)
          newZoom /= 2;
      }
    }
    float xTrans = 0;
    float yTrans = 0;
    float max = viewer.getMaxZoomPercent();
    if (newZoom < 5 || newZoom > max)
      numberOutOfRange(5, max);
    if (!viewer.isWindowCentered()) {
      // do a smooth zoom only if not windowCentered
      if (center != null) {
        BitSet bs = expression(ptCenter);
        if (!isSyntaxCheck)
          viewer.setCenterBitSet(bs, false);
      }
      center = viewer.getRotationCenter();
      xTrans = viewer.getTranslationXPercent();
      yTrans = viewer.getTranslationYPercent();
    }
    if (isSyntaxCheck)
      return;
    if (isSameAtom && Math.abs(zoom - newZoom) < 1)
      time = 0;
    viewer.moveTo(time, center, JmolConstants.center, Float.NaN, newZoom,
        xTrans, yTrans, Float.NaN, null, Float.NaN, Float.NaN, Float.NaN);
  }

  private float getZoom(int i, BitSet bs, float currentZoom)
      throws ScriptException {
    // moveTo/zoom/zoomTo [optional {center}] percent|-factor|+factor|*factor|/factor
    // moveTo/zoom/zoomTo {center} 0 [optional -factor|+factor|*factor|/factor]

    float zoom = (isFloatParameter(i) ? floatParameter(i++) : Float.NaN);
    if (zoom == 0 || currentZoom == 0) {
      // moveTo/zoom/zoomTo {center} 0
      if (bs == null)
        error(ERROR_invalidArgument);
      float r = viewer.calcRotationRadius(bs);
      currentZoom = viewer.getRotationRadius() / r * 100;
      zoom = Float.NaN;
    }
    if (zoom < 0) {
      // moveTo/zoom/zoomTo -factor
      zoom += currentZoom;
    } else if (Float.isNaN(zoom)) {
      // moveTo/zoom/zoomTo [optional {center}] percent|+factor|*factor|/factor
      // moveTo/zoom/zoomTo {center} 0 [optional -factor|+factor|*factor|/factor]
      int tok = tokAt(i);
      switch (tok) {
      case Token.divide:
      case Token.times:
      case Token.plus:
        float value = floatParameter(++i);
        i++;
        switch (tok) {
        case Token.divide:
          zoom = currentZoom / value;
          break;
        case Token.times:
          zoom = currentZoom * value;
          break;
        case Token.plus:
          zoom = currentZoom + value;
          break;
        }
        break;
      default:
        // indicate no factor indicated
        zoom = (bs == null ? -currentZoom : currentZoom);
      }
    }
    iToken = i - 1;
    return zoom;
  }

  private void gotocmd() throws ScriptException {
    checkLength(2);
    String strTo = null;
    strTo = parameterAsString(1);
    int pcTo = -1;
    for (int i = 0; i < aatoken.length; i++) {
      Token[] tokens = aatoken[i];
      if (tokens[0].tok == Token.message || tokens[0].tok == Token.nada)
        if (tokens[tokens.length - 1].value.toString().equalsIgnoreCase(strTo)) {
          pcTo = i;
          break;
        }
    }
    if (pcTo < 0)
      error(ERROR_invalidArgument);
    if (!isSyntaxCheck)
      pc = pcTo - 1; // ... resetting the program counter
  }

  private void delay() throws ScriptException {
    long millis = 0;
    switch (getToken(1).tok) {
    case Token.on: // this is auto-provided as a default
      millis = 1;
      break;
    case Token.integer:
      millis = intParameter(1) * 1000;
      break;
    case Token.decimal:
      millis = (long) (floatParameter(1) * 1000);
      break;
    default:
      error(ERROR_numberExpected);
    }
    if (!isSyntaxCheck)
      delay(millis);
  }

  private void delay(long millis) {
    long timeBegin = System.currentTimeMillis();
    refresh();
    int delayMax;
    if (millis < 0)
      millis = -millis;
    else if ((delayMax = viewer.getDelayMaximum()) > 0 && millis > delayMax)
      millis = delayMax;
    millis -= System.currentTimeMillis() - timeBegin;
    int seconds = (int) millis / 1000;
    millis -= seconds * 1000;
    if (millis <= 0)
      millis = 1;
    while (seconds >= 0 && millis > 0 && !interruptExecution
        && currentThread == Thread.currentThread()) {
      viewer.popHoldRepaint("delay");
      try {
        Thread.sleep((seconds--) > 0 ? 1000 : millis);
      } catch (InterruptedException e) {
      }
      viewer.pushHoldRepaint("delay");
    }
  }

  private void slab(boolean isDepth) throws ScriptException {
    boolean TF = false;
    Point4f plane = null;
    String str;
    if (isCenterParameter(1) || tokAt(1) == Token.point4f)
      plane = planeParameter(1);
    else
      switch (getToken(1).tok) {
      case Token.integer:
        checkLength(2);
        int percent = intParameter(1);
        if (!isSyntaxCheck)
          if (isDepth)
            viewer.depthToPercent(percent);
          else
            viewer.slabToPercent(percent);
        return;
      case Token.on:
        checkLength(2);
        TF = true;
        // fall through
      case Token.off:
        checkLength(2);
        setBooleanProperty("slabEnabled", TF);
        return;
      case Token.reset:
        checkLength(2);
        if (isSyntaxCheck)
          return;
        viewer.slabReset();
        setBooleanProperty("slabEnabled", true);
        return;
      case Token.set:
        checkLength(2);
        if (isSyntaxCheck)
          return;
        viewer.setSlabDepthInternal(isDepth);
        setBooleanProperty("slabEnabled", true);
        return;
      case Token.minus:
        str = parameterAsString(2);
        if (str.equalsIgnoreCase("hkl"))
          plane = hklParameter(3);
        else if (str.equalsIgnoreCase("plane"))
          plane = planeParameter(3);
        if (plane == null)
          error(ERROR_invalidArgument);
        plane.scale(-1);
        break;
      case Token.plane:
        switch (getToken(2).tok) {
        case Token.none:
          break;
        default:
          plane = planeParameter(2);
        }
        break;
      case Token.hkl:
        plane = (getToken(2).tok == Token.none ? null : hklParameter(2));
        break;
      case Token.identifier:
        str = parameterAsString(1);
        if (str.equalsIgnoreCase("reference")) {
          // only in 11.2; deprecated
          return;
        }
      default:
        error(ERROR_invalidArgument);
      }
    if (!isSyntaxCheck)
      viewer.slabInternal(plane, isDepth);
  }

  private void ellipsoid() throws ScriptException {
    int mad = 0;
    int i = 1;
    switch (getToken(1).tok) {
    case Token.on:
      mad = 50;
      break;
    case Token.off:
      break;
    case Token.integer:
      mad = intParameter(1);
      break;
    case Token.times:
    case Token.identifier:
      viewer.loadShape(JmolConstants.SHAPE_ELLIPSOIDS);
      if (parameterAsString(i).equalsIgnoreCase("ID"))
        i++;
      setShapeId(JmolConstants.SHAPE_ELLIPSOIDS, i, false);
      i = iToken;
      for (++i; i < statementLength; i++) {
        String key = parameterAsString(i);
        Object value = null;
        if (key.equalsIgnoreCase("modelIndex")) {
          value = new Integer(intParameter(++i));
          key = "modelindex";
        } else if (key.equalsIgnoreCase("axes")) {
          Vector3f[] axes = new Vector3f[3];
          for (int j = 0; j < 3; j++) {
            axes[j] = new Vector3f();
            axes[j].set(centerParameter(++i));
            i = iToken;
          }
          value = axes;
        } else if (key.equalsIgnoreCase("on")) {
          value = Boolean.TRUE;
        } else if (key.equalsIgnoreCase("off")) {
          key = "on";
          value = Boolean.FALSE;
        } else if (key.equalsIgnoreCase("delete")) {
          value = Boolean.TRUE;
          checkLength(3);
        } else if (key.equalsIgnoreCase("center")) {
          value = centerParameter(++i);
          i = iToken;
        } else if (key.equalsIgnoreCase("scale")) {
          value = new Float(floatParameter(++i));
        } else if (key.equalsIgnoreCase("color")) {
          float translucentLevel = Float.NaN;
          i++;
          if ((theTok = tokAt(i)) == Token.translucent) {
            value = "translucent";
            if (isFloatParameter(++i))
              translucentLevel = getTranslucentLevel(i++);
            else
              translucentLevel = viewer.getDefaultTranslucent();
          } else if (theTok == Token.opaque) {
            value = "opaque";
            i++;
          }
          if (isColorParam(i)) {
            setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS, "color",
                new Integer(getArgbParam(i)));
            i = iToken;
          }
          if (value == null)
            continue;
          if (!Float.isNaN(translucentLevel))
            setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS,
                "translucentLevel", new Float(translucentLevel));
          key = "translucency";
        }
        if (value == null)
          error(ERROR_invalidArgument);
        setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS, key.toLowerCase(),
            value);
      }
      setShapeProperty(JmolConstants.SHAPE_ELLIPSOIDS, "thisID", null);
      return;
    default:
      error(ERROR_invalidArgument);
    }
    setShapeSize(JmolConstants.SHAPE_ELLIPSOIDS, mad);
  }

  private String getShapeNameParameter(int i) throws ScriptException {
    String id = parameterAsString(i);
    boolean isWild = id.equals("*");
    if (id.length() == 0)
      error(ERROR_invalidArgument);
    if (isWild) {
      switch (tokAt(i + 1)) {
      case Token.nada:
      case Token.on:
      case Token.off:
      case Token.displayed:
      case Token.hidden:
      case Token.color:
      case Token.delete:
        break;
      default:
        id += optParameterAsString(++i);
      }
    }
    if (tokAt(i + 1) == Token.times)
      id += parameterAsString(++i);
    iToken = i;
    return id;
  }

  private String setShapeId(int iShape, int i, boolean idSeen)
      throws ScriptException {
    if (idSeen)
      error(ERROR_invalidArgument);
    String name = getShapeNameParameter(i).toLowerCase();
    setShapeProperty(iShape, "thisID", name);
    return name;
  }

  private void setAtomShapeSize(int shape, int defOn) throws ScriptException {
    // halo star spacefill
    int code = 0;
    float fsize = Float.NaN;
    int tok = tokAt(1);
    switch (tok) {
    case Token.only:
      restrictSelected(false);
      code = defOn;
      break;
    case Token.on:
      code = defOn;
      break;
    case Token.vanderwaals:
      code = -100;
      break;
    case Token.off:
      break;
    case Token.plus:
    case Token.decimal:
      int i = (tok == Token.plus ? 2 : 1);
      code = (i == 2 ? 1 : -1);
      fsize = floatParameter(i, 0, Atom.RADIUS_MAX);
      break;
    case Token.integer:
      int intVal = intParameter(1);
      if (tokAt(2) == Token.percent) {
        if (intVal < 0 || intVal > 200)
          integerOutOfRange(0, 200);
        int iMode = JmolConstants.getVdwType(optParameterAsString(3));
        if (iMode >= 0)
          code = (-(iMode + 1) * 2000 - intVal);
        else
          code = (-intVal);
        break;
      }
      // rasmol 250-scale if positive or percent (again), if negative
      // (deprecated)
      if (intVal > 749 || intVal < -200)
        integerOutOfRange(-200, 749);
      code = (intVal <= 0 ? intVal : intVal * 8);
      break;
    case Token.temperature:
      code = -1000;
      break;
    case Token.ionic:
      code = -1001;
      break;
    case Token.adpmax:
      code = Short.MAX_VALUE;
      if (tokAt(2) == Token.integer)
        code += intParameter(2);
      break;
    case Token.adpmin:
      code = Short.MIN_VALUE;
      if (tokAt(2) == Token.integer)
        code -= intParameter(2);
      break;
    default:
      error(ERROR_invalidArgument);
    }
    setShapeSize(shape, code, fsize);
  }

  private void structure() throws ScriptException {
    String type = parameterAsString(1).toLowerCase();
    byte iType = 0;
    BitSet bs = null;
    if (type.equals("helix"))
      iType = JmolConstants.PROTEIN_STRUCTURE_HELIX;
    else if (type.equals("sheet"))
      iType = JmolConstants.PROTEIN_STRUCTURE_SHEET;
    else if (type.equals("turn"))
      iType = JmolConstants.PROTEIN_STRUCTURE_TURN;
    else if (type.equals("none"))
      iType = JmolConstants.PROTEIN_STRUCTURE_NONE;
    else
      error(ERROR_invalidArgument);
    switch (tokAt(2)) {
    case Token.bitset:
    case Token.expressionBegin:
      bs = expression(2);
      checkLength(iToken + 1);
      break;
    default:
      checkLength(2);
    }
    if (isSyntaxCheck)
      return;
    clearDefinedVariableAtomSets();
    viewer.setProteinType(iType, bs);
  }

  private void wireframe() throws ScriptException {
    int mad = getMadParameter();
    if (isSyntaxCheck)
      return;
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, mad);
  }

  private void ssbond() throws ScriptException {
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_SULFUR_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, getMadParameter());
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
  }

  private void hbond(boolean isCommand) throws ScriptException {
    if (statementLength == 2 && getToken(1).tok == Token.calculate) {
      if (isSyntaxCheck)
        return;
      int n = viewer.autoHbond(null);
      scriptStatusOrBuffer(GT.translate("{0} hydrogen bonds", n));
      return;
    }
    if (statementLength == 2 && getToken(1).tok == Token.delete) {
      if (isSyntaxCheck)
        return;
      connect(0);
      return;
    }
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_HYDROGEN_MASK));
    setShapeSize(JmolConstants.SHAPE_STICKS, getMadParameter());
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_COVALENT_MASK));
  }

  private void configuration() throws ScriptException {
    if (!isSyntaxCheck && viewer.getDisplayModelIndex() <= -2)
      error(ERROR_backgroundModelError, "\"CONFIGURATION\"");
    BitSet bsConfigurations;
    if (statementLength == 1) {
      bsConfigurations = viewer.setConformation();
      viewer.addStateScript("select", null, viewer.getSelectionSet(), null,
          "configuration", true, false);
    } else {
      checkLength(2);
      if (isSyntaxCheck)
        return;
      int n = intParameter(1);
      bsConfigurations = viewer.setConformation(n - 1);
      viewer.addStateScript("configuration " + n + ";", true, false);
    }
    if (isSyntaxCheck)
      return;
    boolean addHbonds = viewer.hasCalculatedHBonds(bsConfigurations);
    setShapeProperty(JmolConstants.SHAPE_STICKS, "type", new Integer(
        JmolConstants.BOND_HYDROGEN_MASK));
    viewer.setShapeSize(JmolConstants.SHAPE_STICKS, 0, Float.NaN,
        bsConfigurations);
    if (addHbonds)
      viewer.autoHbond(bsConfigurations, bsConfigurations, null, 0, 0);
    viewer.select(bsConfigurations, tQuiet);
  }

  private void vector() throws ScriptException {
    int code = 1;
    float fsize = Float.NaN;
    checkLength(-3);
    switch (iToken = statementLength) {
    case 1:
      break;
    case 2:
      switch (getToken(1).tok) {
      case Token.on:
        break;
      case Token.off:
        code = 0;
        break;
      case Token.integer:
        // diameter Pixels
        code = intParameter(1, 0, 19);
        break;
      case Token.decimal:
        // radius angstroms
        code = -1;
        fsize = floatParameter(1, 0, 3);
        break;
      default:
        error(ERROR_booleanOrNumberExpected);
      }
      break;
    case 3:
      if (tokAt(1) == Token.scale) {
        setFloatProperty("vectorScale", floatParameter(2, -10, 10));
        return;
      }
    }
    setShapeSize(JmolConstants.SHAPE_VECTORS, code, fsize);
  }

  private void dipole() throws ScriptException {
    // dipole intWidth floatMagnitude OFFSET floatOffset {atom1} {atom2}
    String propertyName = null;
    Object propertyValue = null;
    boolean iHaveAtoms = false;
    boolean iHaveCoord = false;
    boolean idSeen = false;

    viewer.loadShape(JmolConstants.SHAPE_DIPOLES);
    if (tokAt(1) == Token.list && listIsosurface(JmolConstants.SHAPE_DIPOLES))
      return;
    setShapeProperty(JmolConstants.SHAPE_DIPOLES, "init", null);
    if (statementLength == 1) {
      setShapeProperty(JmolConstants.SHAPE_DIPOLES, "thisID", null);
      return;
    }
    for (int i = 1; i < statementLength; ++i) {
      propertyName = null;
      propertyValue = null;
      switch (getToken(i).tok) {
      case Token.on:
        propertyName = "on";
        break;
      case Token.off:
        propertyName = "off";
        break;
      case Token.delete:
        propertyName = "delete";
        break;
      case Token.integer:
      case Token.decimal:
        propertyName = "value";
        propertyValue = new Float(floatParameter(i));
        break;
      case Token.bitset:
        propertyName = "atomBitset";
        // fall through
      case Token.expressionBegin:
        if (propertyName == null)
          propertyName = (iHaveAtoms || iHaveCoord ? "endSet" : "startSet");
        propertyValue = expression(i);
        i = iToken;
        iHaveAtoms = true;
        break;
      case Token.leftbrace:
      case Token.point3f:
        // {X, Y, Z}
        Point3f pt = getPoint3f(i, true);
        i = iToken;
        propertyName = (iHaveAtoms || iHaveCoord ? "endCoord" : "startCoord");
        propertyValue = pt;
        iHaveCoord = true;
        break;
      case Token.bonds:
        propertyName = "bonds";
        break;
      case Token.calculate:
        propertyName = "calculate";
        break;
      case Token.times:
      case Token.identifier:
        String cmd = parameterAsString(i);
        if (cmd.equalsIgnoreCase("id")) {
          setShapeId(JmolConstants.SHAPE_DIPOLES, ++i, idSeen);
          i = iToken;
          break;
        }
        if (cmd.equalsIgnoreCase("cross")) {
          propertyName = "cross";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (cmd.equalsIgnoreCase("noCross")) {
          propertyName = "cross";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (cmd.equalsIgnoreCase("offset")) {
          float v = floatParameter(++i);
          if (theTok == Token.integer) {
            propertyName = "offsetPercent";
            propertyValue = new Integer((int) v);
          } else {
            propertyName = "offset";
            propertyValue = new Float(v);
          }
          break;
        }
        if (cmd.equalsIgnoreCase("value")) {
          propertyName = "value";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (cmd.equalsIgnoreCase("offsetSide")) {
          propertyName = "offsetSide";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (cmd.equalsIgnoreCase("width")) {
          propertyName = "width";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        setShapeId(JmolConstants.SHAPE_DIPOLES, i, idSeen);
        i = iToken;
        break;
      default:
        error(ERROR_invalidArgument);
      }
      idSeen = (theTok != Token.delete && theTok != Token.calculate);
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_DIPOLES, propertyName,
            propertyValue);
    }
    if (iHaveCoord || iHaveAtoms)
      setShapeProperty(JmolConstants.SHAPE_DIPOLES, "set", null);
  }

  private void animationMode() throws ScriptException {
    float startDelay = 1, endDelay = 1;
    if (statementLength > 5)
      error(ERROR_badArgumentCount);
    int animationMode = AnimationManager.ANIMATION_ONCE;
    switch (getToken(2).tok) {
    case Token.loop:
      animationMode = AnimationManager.ANIMATION_LOOP;
      break;
    case Token.identifier:
      String cmd = parameterAsString(2);
      if (cmd.equalsIgnoreCase("once")) {
        startDelay = endDelay = 0;
        break;
      }
      if (cmd.equalsIgnoreCase("palindrome")) {
        animationMode = AnimationManager.ANIMATION_PALINDROME;
        break;
      }
      error(ERROR_invalidArgument);
    }
    if (statementLength >= 4) {
      startDelay = endDelay = floatParameter(3);
      if (statementLength == 5)
        endDelay = floatParameter(4);
    }
    if (!isSyntaxCheck)
      viewer.setAnimationReplayMode(animationMode, startDelay, endDelay);
  }

  private void vibration() throws ScriptException {
    checkLength(-3);
    float period = 0;
    switch (getToken(1).tok) {
    case Token.on:
      checkLength(2);
      period = viewer.getVibrationPeriod();
      break;
    case Token.off:
      checkLength(2);
      period = 0;
      break;
    case Token.integer:
    case Token.decimal:
      checkLength(2);
      period = floatParameter(1);
      break;
    case Token.scale:
      setFloatProperty("vibrationScale", floatParameter(2, -10, 10));
      return;
    case Token.identifier:
      String cmd = optParameterAsString(1);
      if (cmd.equalsIgnoreCase("period")) {
        setFloatProperty("vibrationPeriod", floatParameter(2));
        return;
      }
      error(ERROR_invalidArgument);
    default:
      period = -1;
    }
    if (period < 0)
      error(ERROR_invalidArgument);
    if (isSyntaxCheck)
      return;
    if (period == 0) {
      viewer.setVibrationOff();
      return;
    }
    viewer.setVibrationPeriod(-period);
  }

  private void animationDirection() throws ScriptException {
    checkLength(4);
    boolean negative = false;
    getToken(2);
    if (theTok == Token.minus)
      negative = true;
    else if (theTok != Token.plus)
      error(ERROR_invalidArgument);
    int direction = intParameter(3);
    if (direction != 1)
      error(ERROR_numberMustBe, "-1", "1");
    if (negative)
      direction = -direction;
    if (!isSyntaxCheck)
      viewer.setAnimationDirection(direction);
  }

  private void calculate() throws ScriptException {
    boolean isSurface = false;
    BitSet bs;
    if ((iToken = statementLength) >= 2) {
      clearDefinedVariableAtomSets();
      switch (getToken(1).tok) {
      case Token.straightness:
        if (!isSyntaxCheck) {
          viewer.calculateStraightness();
          viewer.addStateScript(thisCommand, false, true);
        }
        return;
      case Token.pointgroup:
        pointGroup();
        return;
      case Token.surface:
        isSurface = true;
        // deprecated
        // fall through
      case Token.surfacedistance:
        /*
         * preferred:
         * 
         * calculate surfaceDistance FROM {...} calculate surfaceDistance WITHIN
         * {...}
         */
        String type = optParameterAsString(2);
        boolean isFrom = false;
        if (type.equalsIgnoreCase("within")) {
        } else if (type.equalsIgnoreCase("from")) {
          isFrom = true;
        } else if (type.length() > 0) {
          isFrom = true;
          iToken--;
        } else if (!isSurface) {
          isFrom = true;
        }
        bs = (iToken + 1 < statementLength ? expression(++iToken) : viewer
            .getSelectionSet());
        checkLength(++iToken);
        if (isSyntaxCheck)
          return;
        viewer.calculateSurface(bs, (isFrom ? Float.MAX_VALUE : -1));
        return;
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("AROMATIC")) {
          checkLength(2);
          if (!isSyntaxCheck)
            viewer.assignAromaticBonds();
          return;
        }
        break;
      case Token.hbond:
        if (statementLength == 2) {
          if (!isSyntaxCheck)
            viewer.autoHbond(null);
          return;
        }
        BitSet bs1 = expression(2);
        BitSet bs2 = expression(iToken + 1);
        if (!isSyntaxCheck) {
          int nBonds = viewer.autoHbond(bs1, bs2, null, -1, -1);
          showString(nBonds + " hydrogen bonds created");
        }
        return;
      case Token.structure:
        bs = (statementLength == 2 ? null : expression(2));
        if (isSyntaxCheck)
          return;
        if (bs == null)
          bs = viewer.getAtomBitSet(null);
        viewer.calculateStructures(bs);
        viewer.addStateScript(thisCommand, false, true);
      }
    }
    error(
        ERROR_what,
        "CALCULATE",
        "aromatic? hbonds? polymers? straightness? structure? surfaceDistance FROM? surfaceDistance WITHIN?");
  }

  private void pointGroup() throws ScriptException {
    switch (tokAt(0)) {
    case Token.calculate:
      if (!isSyntaxCheck)
        showString(viewer.calculatePointGroup());
      return;
    case Token.show:
      if (!isSyntaxCheck)
        showString(viewer.getPointGroupAsString(false, null, 0, 0));
      return;
    }
    // draw pointgroup [C2|C3|Cs|Ci|etc.] [n] [scale x]
    int pt = 2;
    String type = (tokAt(pt) == Token.scale ? "" : optParameterAsString(pt));
    float scale = 1;
    int index = 0;
    if (type.length() > 0) {
      if (isFloatParameter(++pt))
        index = intParameter(pt++);
    }
    if (tokAt(pt) == Token.scale)
      scale = floatParameter(++pt);
    if (!isSyntaxCheck)
      runScript(viewer.getPointGroupAsString(true, type, index, scale));
  }

  private void dots(int iShape) throws ScriptException {
    if (!isSyntaxCheck)
      viewer.loadShape(iShape);
    setShapeProperty(iShape, "init", null);
    int code = 0;
    float fsize = Float.NaN;
    int ipt = 1;
    switch (getToken(1).tok) {
    case Token.only:
      restrictSelected(false);
      code = 1;
      break;
    case Token.on:
    case Token.vanderwaals:
      code = 1;
      break;
    case Token.ionic:
      code = -1;
      break;
    case Token.off:
      break;
    case Token.plus:
      fsize = floatParameter(++ipt, 0, Atom.RADIUS_MAX); // ambiguity here
      code = 1;
      break;
    case Token.decimal:
      fsize = floatParameter(ipt, 0, Atom.RADIUS_MAX);
      code = -1;
      break;
    case Token.integer:
      int dotsParam = intParameter(ipt++);
      if (statementLength > ipt && statement[ipt].tok == Token.radius) {
        setShapeProperty(iShape, "atom", new Integer(dotsParam));
        setShapeProperty(iShape, "radius", new Float(floatParameter(++ipt)));
        if (statementLength > ipt + 1 && statement[++ipt].tok == Token.color)
          setShapeProperty(iShape, "colorRGB", new Integer(getArgbParam(++ipt)));
        if (getToken(ipt).tok != Token.bitset)
          error(ERROR_invalidArgument);
        setShapeProperty(iShape, "dots", statement[ipt].value);
        return;
      }
      if (dotsParam < 0 || dotsParam > 1000)
        integerOutOfRange(0, 1000);
      code = (dotsParam == 0 ? 0 : dotsParam + 1);
      break;
    case Token.adpmax:
      code = Short.MAX_VALUE;
      if (tokAt(2) == Token.integer)
        code += intParameter(2);
      break;
    case Token.adpmin:
      code = Short.MIN_VALUE;
      if (tokAt(2) == Token.integer)
        code -= intParameter(2);
      break;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    setShapeSize(iShape, code, fsize);
  }

  private void proteinShape(int shapeType) throws ScriptException {
    int mad = 0;
    // token has ondefault1
    switch (getToken(1).tok) {
    case Token.only:
      if (isSyntaxCheck)
        return;
      restrictSelected(false);
      mad = -1;
      break;
    case Token.on:
      mad = -1; // means take default
      break;
    case Token.off:
      break;
    case Token.structure:
      mad = -2;
      break;
    case Token.temperature:
    case Token.displacement:
      mad = -4;
      break;
    case Token.integer:
      mad = (intParameter(1, 0, 499) * 8);
      break;
    case Token.decimal:
      mad = (int) (floatParameter(1, 0, 4) * 2000);
      break;
    case Token.bitset:
      if (!isSyntaxCheck)
        viewer.loadShape(shapeType);
      setShapeProperty(shapeType, "bitset", theToken.value);
      return;
    default:
      error(ERROR_booleanOrNumberExpected);
    }
    setShapeSize(shapeType, mad);
  }

  private void animation() throws ScriptException {
    boolean animate = false;
    switch (getToken(1).tok) {
    case Token.on:
      animate = true;
      // fall through
    case Token.off:
      if (!isSyntaxCheck)
        viewer.setAnimationOn(animate);
      break;
    case Token.frame:
      frame(2);
      break;
    case Token.mode:
      animationMode();
      break;
    case Token.direction:
      animationDirection();
      break;
    case Token.identifier:
      String str = parameterAsString(1);
      if (str.equalsIgnoreCase("fps")) {
        checkLength(3);
        setIntProperty("animationFps", intParameter(2));
        break;
      }
    default:
      frameControl(1, true);
    }
  }

  private void file() throws ScriptException {
    checkLength(2);
    int file = intParameter(1);
    if (isSyntaxCheck)
      return;
    int modelIndex = viewer.getModelNumberIndex(file * 1000000 + 1, false,
        false);
    int modelIndex2 = -1;
    if (modelIndex >= 0) {
      modelIndex2 = viewer.getModelNumberIndex((file + 1) * 1000000 + 1, false,
          false);
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelCount();
      modelIndex2--;
    }
    viewer.setAnimationOn(false);
    viewer.setAnimationDirection(1);
    viewer.setAnimationRange(modelIndex, modelIndex2);
    viewer.setCurrentModelIndex(-1);
  }

  private void frame(int offset) throws ScriptException {
    boolean useModelNumber = true;
    // for now -- as before -- remove to implement
    // frame/model difference
    if (statementLength == 1 && offset == 1) {
      int modelIndex = viewer.getCurrentModelIndex();
      int m;
      if (!isSyntaxCheck && modelIndex >= 0
          && (m = viewer.getJmolDataSourceFrame(modelIndex)) >= 0)
        viewer.setCurrentModelIndex(m == modelIndex ? Integer.MIN_VALUE : m);
      return;
    }
    String p1 = optParameterAsString(1);
    if (statementLength == 3 && p1.equalsIgnoreCase("Title")) {
      if (!isSyntaxCheck)
        viewer.setFrameTitle(parameterAsString(2));
      return;
    } else if (p1.equalsIgnoreCase("ALIGN")) {
      BitSet bs = (statementLength == 2 || tokAt(2) == Token.none ? null
          : expression(2));
      if (!isSyntaxCheck)
        viewer.setFrameOffsets(bs);
      return;
    }
    if (getToken(offset).tok == Token.minus) {
      ++offset;
      checkLength(offset + 1);
      if (getToken(offset).tok != Token.integer || intParameter(offset) != 1)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        viewer.setAnimationPrevious();
      return;
    }
    boolean isPlay = false;
    boolean isRange = false;
    boolean isAll = false;
    boolean isHyphen = false;
    int[] frameList = new int[] { -1, -1 };
    int nFrames = 0;

    for (int i = offset; i < statementLength; i++) {
      switch (getToken(i).tok) {
      case Token.all:
      case Token.times:
        checkLength(offset + (isRange ? 2 : 1));
        isAll = true;
        break;
      case Token.minus: // ignore
        if (nFrames != 1)
          error(ERROR_invalidArgument);
        isHyphen = true;
        break;
      case Token.none:
        checkLength(offset + 1);
        break;
      case Token.decimal:
        useModelNumber = false;
        if (floatParameter(i) < 0)
          isHyphen = true;
        // fall through
      case Token.integer:
        if (nFrames == 2)
          error(ERROR_invalidArgument);
        int iFrame = statement[i].intValue;
        if (iFrame >= 1000 && iFrame < 1000000 && viewer.haveFileSet())
          iFrame = (iFrame / 1000) * 1000000 + (iFrame % 1000); // initial way
        if (!useModelNumber && iFrame == 0)
          isAll = true; // 0.0 means ALL; 0 means "all in this range
        if (iFrame >= 1000000)
          useModelNumber = false;
        frameList[nFrames++] = iFrame;
        break;
      case Token.play:
        isPlay = true;
        break;
      case Token.range:
        isRange = true;
        break;
      default:
        checkLength(offset + 1);
        frameControl(i, false);
        return;
      }
    }
    boolean haveFileSet = viewer.haveFileSet();
    if (isRange && nFrames == 0)
      isAll = true;
    if (isSyntaxCheck)
      return;
    if (isAll) {
      viewer.setAnimationOn(false);
      viewer.setAnimationRange(-1, -1);
      if (!isRange) {
        viewer.setCurrentModelIndex(-1);
      }
      return;
    }
    if (nFrames == 2 && !isRange)
      isHyphen = true;
    if (haveFileSet)
      useModelNumber = false;
    else if (useModelNumber)
      for (int i = 0; i < nFrames; i++)
        if (frameList[i] >= 0)
          frameList[i] %= 1000000;
    int modelIndex = viewer.getModelNumberIndex(frameList[0], useModelNumber,
        false);
    int modelIndex2 = -1;
    if (haveFileSet && nFrames == 1 && modelIndex < 0 && frameList[0] != 0) {
      // may have frame 2.0 or frame 2 meaning the range of models in file 2
      if (frameList[0] < 1000000)
        frameList[0] *= 1000000;
      if (frameList[0] % 1000000 == 0) {
        frameList[0]++;
        modelIndex = viewer.getModelNumberIndex(frameList[0], false, false);
        if (modelIndex >= 0) {
          modelIndex2 = viewer.getModelNumberIndex(frameList[0] + 1000000,
              false, false);
          if (modelIndex2 < 0)
            modelIndex2 = viewer.getModelCount();
          modelIndex2--;
          if (isRange)
            nFrames = 2;
          else if (!isHyphen && modelIndex2 != modelIndex)
            isHyphen = true;
          isRange = isRange || modelIndex == modelIndex2;// (isRange ||
                                                         // !isHyphen &&
                                                         // modelIndex2 !=
                                                         // modelIndex);
        }
      } else {
        // must have been a bad frame number. Just return.
        return;
      }
    }

    if (!isPlay && !isRange || modelIndex >= 0)
      viewer.setCurrentModelIndex(modelIndex, false);
    if (isPlay && nFrames == 2 || isRange || isHyphen) {
      if (modelIndex2 < 0)
        modelIndex2 = viewer.getModelNumberIndex(frameList[1], useModelNumber,
            false);
      viewer.setAnimationOn(false);
      viewer.setAnimationDirection(1);
      viewer.setAnimationRange(modelIndex, modelIndex2);
      viewer.setCurrentModelIndex(isHyphen && !isRange ? -1
          : modelIndex >= 0 ? modelIndex : 0, false);
    }
    if (isPlay)
      viewer.resumeAnimation();
  }

  BitSet bitSetForModelFileNumber(int m) {
    // where */1.0 or */1.1 or just 1.1 is processed
    BitSet bs = new BitSet();
    if (isSyntaxCheck)
      return bs;
    int modelCount = viewer.getModelCount();
    boolean haveFileSet = viewer.haveFileSet();
    if (m < 1000000 && haveFileSet)
      m *= 1000000;
    int pt = m % 1000000;
    if (pt == 0) {
      int model1 = viewer.getModelNumberIndex(m + 1, false, false);
      if (model1 < 0)
        return bs;
      int model2 = (m == 0 ? modelCount : viewer.getModelNumberIndex(
          m + 1000001, false, false));
      if (model1 < 0)
        model1 = 0;
      if (model2 < 0)
        model2 = modelCount;
      if (viewer.isTrajectory(model1))
        model2 = model1 + 1;
      for (int j = model1; j < model2; j++)
        bs.or(viewer.getModelAtomBitSet(j, false));
    } else {
      int modelIndex = viewer.getModelNumberIndex(m, false, true);
      if (modelIndex >= 0)
        bs.or(viewer.getModelAtomBitSet(modelIndex, false));
    }
    return bs;
  }

  private void frameControl(int i, boolean isSubCmd) throws ScriptException {
    checkLength(i + 1);
    int tok = getToken(i).tok;
    if (isSyntaxCheck)
      switch (tok) {
      case Token.playrev:
      case Token.play:
      case Token.resume:
      case Token.pause:
      case Token.next:
      case Token.prev:
      case Token.rewind:
      case Token.last:
        return;
      }
    else
      switch (tok) {
      case Token.playrev:
        viewer.reverseAnimation();
        // fall through
      case Token.play:
      case Token.resume:
        viewer.resumeAnimation();
        return;
      case Token.pause:
        viewer.pauseAnimation();
        return;
      case Token.next:
        viewer.setAnimationNext();
        return;
      case Token.prev:
        viewer.setAnimationPrevious();
        return;
      case Token.rewind:
        viewer.rewindAnimation();
        return;
      case Token.last:
        viewer.setAnimationLast();
        return;
      }
    error(ERROR_invalidArgument);
  }

  private int getShapeType(int tok) throws ScriptException {
    int iShape = JmolConstants.shapeTokenIndex(tok);
    if (iShape < 0)
      error(ERROR_unrecognizedObject);
    return iShape;
  }

  private void font(int shapeType, float fontsize) throws ScriptException {
    String fontface = "SansSerif";
    String fontstyle = "Plain";
    int sizeAdjust = 0;
    float scaleAngstromsPerPixel = -1;
    switch (iToken = statementLength) {
    case 6:
      scaleAngstromsPerPixel = floatParameter(5);
      if (scaleAngstromsPerPixel >= 5) // actually a zoom value
        scaleAngstromsPerPixel = viewer.getZoomSetting()
            / scaleAngstromsPerPixel / viewer.getScalePixelsPerAngstrom(false);
      // fall through
    case 5:
      if (getToken(4).tok != Token.identifier)
        error(ERROR_invalidArgument);
      fontstyle = parameterAsString(4);
      // fall through
    case 4:
      if (getToken(3).tok != Token.identifier)
        error(ERROR_invalidArgument);
      fontface = parameterAsString(3);
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      fontsize = floatParameter(2);
      shapeType = getShapeType(getToken(1).tok);
      break;
    case 3:
      if (!isFloatParameter(2))
        error(ERROR_numberExpected);
      if (shapeType == -1) {
        shapeType = getShapeType(getToken(1).tok);
        fontsize = floatParameter(2);
      } else {// labels --- old set fontsize N
        if (fontsize >= 1)
          fontsize += (sizeAdjust = 5);
      }
      break;
    case 2:
    default:
      if (shapeType == JmolConstants.SHAPE_LABELS) {
        // set fontsize
        fontsize = JmolConstants.LABEL_DEFAULT_FONTSIZE;
        break;
      }
      error(ERROR_badArgumentCount);
    }
    if (shapeType == JmolConstants.SHAPE_LABELS) {
      if (fontsize < 0
          || fontsize >= 1
          && (fontsize < JmolConstants.LABEL_MINIMUM_FONTSIZE || fontsize > JmolConstants.LABEL_MAXIMUM_FONTSIZE))
        integerOutOfRange(JmolConstants.LABEL_MINIMUM_FONTSIZE - sizeAdjust,
            JmolConstants.LABEL_MAXIMUM_FONTSIZE - sizeAdjust);
      setShapeProperty(JmolConstants.SHAPE_LABELS, "setDefaults", viewer
          .getNoneSelected());
    }
    if (isSyntaxCheck)
      return;
    Font3D font3d = viewer.getFont3D(fontface, fontstyle, fontsize);
    viewer.loadShape(shapeType);
    setShapeProperty(shapeType, "font", font3d);
    if (scaleAngstromsPerPixel >= 0)
      setShapeProperty(shapeType, "scalereference", new Float(
          scaleAngstromsPerPixel));
  }

  /*
   * ****************************************************************************
   * ============================================================== SET
   * implementations
   * ==============================================================
   */

  private void set() throws ScriptException {
    String key;
    if (statementLength == 1) {
      showString(viewer.getAllSettings(null));
      return;
    }
    boolean isJmolSet = (parameterAsString(0).equals("set"));
    if (isJmolSet && statementLength == 2
        && (key = parameterAsString(1)).indexOf("?") >= 0) {
      showString(viewer.getAllSettings(key.substring(0, key.indexOf("?"))));
      return;
    }
    boolean showing = (!isSyntaxCheck && !tQuiet
        && scriptLevel <= scriptReportingLevel && !((String) statement[0].value)
        .equals("var"));
    int val = Integer.MAX_VALUE;
    int n = 0;
    switch (getToken(1).tok) {

    // THESE ARE DEPRECATED AND HAVE THEIR OWN COMMAND

    case Token.axes:
      axes(2);
      return;
    case Token.background:
      background(2);
      return;
    case Token.boundbox:
      boundbox(2);
      return;
    case Token.frank:
      frank(2);
      return;
    case Token.history:
      history(2);
      return;
    case Token.label:
      label(2);
      return;
    case Token.unitcell:
      unitcell(2);
      return;
    case Token.display:// deprecated
    case Token.selectionHalo:
      selectionHalo(2);
      return;

      // THESE HAVE MULTIPLE CONTEXTS AND
      // SO DO NOT ALLOW CALCULATIONS xxx = a + b...

    case Token.bondmode:
      setBondmode();
      return;
    case Token.echo:
      setEcho();
      return;
    case Token.fontsize:
      checkLength23();
      font(JmolConstants.SHAPE_LABELS, statementLength == 2 ? 0
          : floatParameter(2));
      return;
    case Token.hbond:
      setHbond();
      return;
    case Token.monitor:
      setMonitor();
      return;
    case Token.property: // considered reserved
      key = parameterAsString(1).toLowerCase();
      if (key.startsWith("property_")) {
      } else {
        setProperty();
        return;
      }
      break;
    case Token.picking:
      setPicking();
      return;
    case Token.pickingStyle:
      setPickingStyle();
      return;

      // deprecated to other parameters
    case Token.spin:
      checkLength(4);
      setSpin(parameterAsString(2), (int) floatParameter(3));
      return;
    case Token.navigate:
      checkLength(4);
      setNav(parameterAsString(2), (int) floatParameter(3));
      return;
    case Token.ssbond: // ssBondsBackbone
      setSsbond();
      return;

      // THESE NEXT DO ALLOW CALCULATIONS xxx = a + b...

    case Token.scale3d:
      setFloatProperty("scaleAngstromsPerInch", floatSetting(2));
      return;
    case Token.formalCharge:
      n = intSetting(2);
      if (!isSyntaxCheck)
        viewer.setFormalCharges(n);
      return;
    case Token.specular:
      if (statementLength == 2 || statement[2].tok != Token.integer) {
        key = "specular";
        break;
      }
      // fall through
    case Token.specpercent:
      key = "specularPercent";
      break;
    case Token.ambient:
      key = "ambientPercent";
      break;
    case Token.diffuse:
      key = "diffusePercent";
      break;
    case Token.specpower:
      val = intSetting(2);
      if (val >= 0) {
        key = "specularPower";
        break;
      }
      if (val < -10 || val > -1)
        integerOutOfRange(-10, -1);
      val = -val;
      key = "specularExponent";
      break;
    case Token.specexponent:
      key = "specularExponent";
      break;
    case Token.bonds:
      key = "showMultipleBonds";
      break;
    case Token.strands:
      key = "strandCount";
      break;
    case Token.hetero:
      key = "selectHetero";
      break;
    case Token.hydrogen:
      key = "selectHydrogen";
      break;
    case Token.radius:
      key = "solventProbeRadius";
      break;
    case Token.solvent:
      key = "solventProbe";
      break;
    case Token.color:
    case Token.defaultColors:
      key = "defaultColorScheme";
      break;
    default:
      key = parameterAsString(1);
      if (key.charAt(0) == '_') // these cannot be set by user
        error(ERROR_invalidArgument);

      // these next are not reported and do not allow calculation xxxx = a + b

      if (key.equalsIgnoreCase("toggleLabel")) {
        if (setLabel("toggle"))
          return;
      }
      if (key.toLowerCase().indexOf("label") == 0
          && Parser
              .isOneOf(key.substring(5).toLowerCase(),
                  "front;group;atom;offset;pointer;alignment;toggle;scalereference")) {
        if (setLabel(key.substring(5)))
          return;
      }
      if (key.equalsIgnoreCase("userColorScheme")) {
        setUserColors();
        return;
      }
      if (key.equalsIgnoreCase("defaultLattice")) {
        Point3f pt;
        Vector v = (Vector) parameterExpression(2, 0, "XXX", true);
        if (v == null || v.size() == 0)
          error(ERROR_invalidArgument);
        Token token = (ScriptVariable) v.elementAt(0);
        if (token.tok == Token.point3f)
          pt = (Point3f) token.value;
        else {
          int ijk = ScriptVariable.iValue(token);
          if (ijk < 555)
            pt = new Point3f();
          else
            pt = viewer.getSymmetry().ijkToPoint3f(ijk + 111);
        }
        if (!isSyntaxCheck)
          viewer.setDefaultLattice(pt);
        return;
      }

      // THESE CAN BE PART OF CALCULATIONS

      if (key.equalsIgnoreCase("defaultDrawArrowScale")) {
        setFloatProperty(key, floatSetting(2));
        return;
      }
      if (key.equalsIgnoreCase("logLevel")) {
        // set logLevel n
        // we have 5 levels 0 - 4 debug -- error
        // n = 0 -- no messages -- turn all off
        // n = 1 add level 4, error
        // n = 2 add level 3, warn
        // etc.
        int ilevel = intSetting(2);
        if (isSyntaxCheck)
          return;
        setIntProperty("logLevel", ilevel);
        return;
      }
      if (key.equalsIgnoreCase("backgroundModel")) {
        String modelDotted = stringSetting(2, false);
        int modelNumber;
        boolean useModelNumber = false;
        if (modelDotted.indexOf(".") < 0) {
          modelNumber = Parser.parseInt(modelDotted);
          useModelNumber = true;
        } else {
          modelNumber = JmolConstants.modelValue(modelDotted);
        }
        if (isSyntaxCheck)
          return;
        int modelIndex = viewer.getModelNumberIndex(modelNumber,
            useModelNumber, true);
        viewer.setBackgroundModelIndex(modelIndex);
        return;
      }
      if (key.equalsIgnoreCase("debug")) {
        if (isSyntaxCheck)
          return;
        int iLevel = (tokAt(2) == Token.off 
            || tokAt(2) == Token.integer && intParameter(2) == 0 ? 4 : 5);
        Logger.setLogLevel(iLevel);
        setIntProperty("logLevel", iLevel);
        if (iLevel == 4) {
          viewer.setDebugScript(false);
          if (showing) 
            viewer.showParameter("debugScript", true, 80);
        }
        setDebugging();
        if (showing) 
          viewer.showParameter("logLevel", true, 80);
        return;
      }


      if (key.equalsIgnoreCase("language")) {
        // language can be used without quotes in a SET context
        // set language en
        String lang = stringSetting(2, isJmolSet);
        setStringProperty(key, lang);
        return;
      }
      if (key.equalsIgnoreCase("trajectory")
          || key.equalsIgnoreCase("trajectories")) {
        Token token = tokenSetting(2); // if an expression, we are done
        if (isSyntaxCheck)
          return;
        if (token.tok == Token.decimal) // if a number, we just set its
                                        // trajectory
          viewer.getModelNumberIndex(token.intValue, false, true);
        return;
      }
      // deprecated:

      if (key.equalsIgnoreCase("showSelections")) {
        key = "selectionHalos";
        break;
      }
      if (key.equalsIgnoreCase("measurementNumbers")) {
        key = "measurementLabels";
        break;
      }
    }

    if (getContextVariableAsVariable(key) != null
        || !setParameter(key, val, isJmolSet, showing)) {
      int tok2 = (tokAt(1) == Token.expressionBegin ? 0 : tokAt(2));
      int setType = statement[0].intValue;
      // recasted by compiler:
      // var c.xxx =
      // c.xxx =
      // {...}[n].xxx =
      // not supported:
      // a[...][...].xxx =
      // var a[...][...].xxx =

      int pt = (tok2 == Token.opEQ ? 3
      // set x = ...
          : setType == '=' && !key.equals("return") && tok2 != Token.opEQ ? 0
          // {c}.xxx =
              // {...}.xxx =
              // {{...}[n]}.xxx =
              : 2
      // var a[...].xxx =
      // a[...].xxx =
      // var c = ...
      // var c = [
      // c = [
      // c = ...
      // set x ...
      // a[...] =
      );
      setVariable(pt, 0, key, showing, setType);
      if (!isJmolSet)
        return;
    }
    if (showing)
      viewer.showParameter(key, true, 80);
  }

  private void setVariable(int pt, int ptMax, String key, boolean showing,
                           int setType) throws ScriptException {

    // from SET, we are only here if a Jmol parameter has not been identified
    // from FOR or WHILE, no such check is made

    // if both pt and ptMax are 0, then it indicates that

    BitSet bs = null;
    String propertyName = "";
    int tokProperty = Token.nada;
    boolean isArrayItem = (statement[0].intValue == '[');
    boolean settingProperty = false;
    boolean isExpression = false;
    boolean settingData = (key.startsWith("property_"));
    ScriptVariable t = (settingData ? null : getContextVariableAsVariable(key));
    boolean isUserVariable = (t != null);

    if (pt > 0 && tokAt(pt - 1) == Token.expressionBegin) {
      bs = expression(pt - 1);
      pt = iToken + 1;
      isExpression = true;
    }
    if (tokAt(pt) == Token.period) {
      settingProperty = true;
      ScriptVariable token = getBitsetPropertySelector(++pt, true);
      if (token == null)
        error(ERROR_invalidArgument);
      if (tokAt(++pt) != Token.opEQ)
        error(ERROR_invalidArgument);
      pt++;
      tokProperty = token.intValue;
      propertyName = (String) token.value;
    }
    if (isExpression && !settingProperty)
      error(ERROR_invalidArgument);

    // get value

    Object v = parameterExpression(pt, ptMax, key, true, -1, isArrayItem, null,
        null);
    if (v == null)
      return;
    int nv = ((Vector) v).size();
    if (nv == 0 || !isArrayItem && nv > 1 || isArrayItem && nv != 3)
      error(ERROR_invalidArgument);
    if (isSyntaxCheck)
      return;
    ScriptVariable tv = (ScriptVariable) ((Vector) v).get(isArrayItem ? 2 : 0);

    // create user variable if needed for list now, so we can do the copying

    boolean needVariable = (!isUserVariable && !isExpression && !settingData 
        && (isArrayItem || settingProperty || !(tv.value instanceof String
        || tv.tok == Token.integer || tv.value instanceof Integer 
        || tv.value instanceof Float || tv.value instanceof Boolean)));

    if (needVariable) {
      t = viewer.getOrSetNewVariable(key, true);
      if (t == null) { // can't set a variable _xxxx
        error(ERROR_invalidArgument);
      }
      isUserVariable = true;
    }

    if (isArrayItem) {

      // stack is selector [ VALUE

      int index = ScriptVariable.iValue((ScriptVariable) ((Vector) v).get(0));
      t.setSelectedValue(index, tv);
      return;
    }
    if (settingProperty) {
      if (!isExpression) {
        if (!(t.value instanceof BitSet))
          error(ERROR_invalidArgument);
        bs = (BitSet) t.value;
      }
      if (propertyName.startsWith("property_")) {
        viewer.setData(propertyName, new Object[] { propertyName,
            ScriptVariable.sValue(tv), BitSetUtil.copy(bs) }, viewer.getAtomCount(), 0, 0,
            tv.tok == Token.list ? Integer.MAX_VALUE : Integer.MIN_VALUE, 0);
        return;
      }
      setBitsetProperty(bs, tokProperty, ScriptVariable.iValue(tv),
          ScriptVariable.fValue(tv), tv);
      return;
    }

    if (isUserVariable) {
      t.set(tv);
      return;
    }

    v = ScriptVariable.oValue(tv);

    if (key.startsWith("property_")) {
      int n = viewer.getAtomCount();
      if (v instanceof String[])
        v = TextFormat.join((String[]) v, '\n', 0);
      viewer.setData(key,
          new Object[] { key, "" + v, BitSetUtil.copy(viewer.getSelectionSet()) }, n, 0, 0,
          Integer.MIN_VALUE, 0);
      return;
    }
    String str;
    if (v instanceof Boolean) {
      setBooleanProperty(key, ((Boolean) v).booleanValue());
    } else if (v instanceof Integer) {
      setIntProperty(key, ((Integer) v).intValue());
    } else if (v instanceof Float) {
      setFloatProperty(key, ((Float) v).floatValue());
    } else if (v instanceof String) {
      setStringProperty(key, (String) v);
    } else if (v instanceof BondSet) {
      setStringProperty(key, Escape.escape((BitSet) v, false));
    } else if (v instanceof BitSet) {
      setStringProperty(key, Escape.escape((BitSet) v));
    } else if (v instanceof Point3f) {
      str = Escape.escape((Point3f) v);
      setStringProperty(key, str);
    } else if (v instanceof Point4f) {
      str = Escape.escape((Point4f) v);
      setStringProperty(key, str);
    } else {
      System.out.println("ERROR -- return from propertyExpression was " + v);
    }
  }

  private boolean setParameter(String key, int intVal, boolean isJmolSet,
                               boolean showing) throws ScriptException {
    String lcKey = key.toLowerCase();
    if (key.equalsIgnoreCase("scriptReportingLevel")) { // 11.1.13
      intVal = intSetting(2);
      if (!isSyntaxCheck) {
        scriptReportingLevel = intVal;
        setIntProperty(key, intVal);
      }
      return true;
    }
    if (key.equalsIgnoreCase("historyLevel")) {
      intVal = intSetting(2);
      if (!isSyntaxCheck) {
        commandHistoryLevelMax = intVal;
        setIntProperty(key, intVal);
      }
      return true;
    }
    if (key.equalsIgnoreCase("dipoleScale"))
      return setFloatProperty("dipoleScale", floatSetting(2, -10, 10));
    if (key.equalsIgnoreCase("axesScale"))
      return setFloatProperty("axesScale", floatSetting(2, -100, 100));
    if (key.equalsIgnoreCase("measurementUnits"))
      return setMeasurementUnits(stringSetting(2, isJmolSet));
    if (key.equalsIgnoreCase("defaultVDW")) {
      String val = (statementLength == 3
          && JmolConstants.getVdwType(parameterAsString(2)) >= 0 ? parameterAsString(2)
          : stringSetting(2, false));
      if (JmolConstants.getVdwType(val) < 0)
        error(ERROR_invalidArgument);
      setStringProperty(key, val);
      return true;
    }
    if (Parser.isOneOf(lcKey, "defaults;defaultcolorscheme")) {
      String val;
      if ((theTok = tokAt(2)) == Token.jmol || theTok == Token.rasmol) {
        val = parameterAsString(2).toLowerCase();
        checkLength(3);
      } else {
        val = stringSetting(2, false).toLowerCase();
      }
      if (!val.equals("jmol") && !val.equals("rasmol"))
        error(ERROR_invalidArgument);
      setStringProperty((key.equalsIgnoreCase("defaults") ? key
          : "defaultColorScheme"), val);
      return true;
    }
    if (Parser.isOneOf(lcKey,
        "strandcount;strandcountformeshribbon;strandcountforstrands"))
      return setIntProperty(key, intSetting(2, Integer.MAX_VALUE, 0, 20));
    if (Parser.isOneOf(lcKey,
        "specularpercent;ambientpercent;diffusepercent;specularPower"))
      return setIntProperty(key, intSetting(2, intVal, 0, 100));
    if (key.equalsIgnoreCase("specularExponent"))
      return setIntProperty(key, intSetting(2, intVal, 1, 10));
    boolean isJmolParameter = viewer.isJmolVariable(key);
    if (isJmolSet && !isJmolParameter) {
      iToken = 1;
      if (!isStateScript)
        error(ERROR_unrecognizedParameter, "SET", key);
      warning(ERROR_unrecognizedParameterWarning, "SET", key);
    }
    switch (statementLength) {
    case 2:
      setBooleanProperty(key, true);
      return true;
    case 3:
      if (intVal != Integer.MAX_VALUE) {
        setIntProperty(key, intVal);
        return true;
      }
      getToken(2);
      if (theTok == Token.none) {
        if (!isSyntaxCheck)
          viewer.removeUserVariable(key);
      } else if (isJmolSet && theTok == Token.identifier) {
        // setStringProperty(key, (String) theToken.value);
      } else {
        return false;
      }
      return true;
    default:
      // if (isJmolSet)
      // error(ERROR_invalidArgument);
    }
    return false;
  }

  private void axes(int index) throws ScriptException {
    // axes or set axes
    String type = optParameterAsString(index).toLowerCase();
    if (statementLength == index + 1
        && Parser.isOneOf(type, "window;unitcell;molecular")) {
      setBooleanProperty("axes" + type, true);
      return;
    }
    // axes scale x.xxx
    if (statementLength == index + 2 && type.equals("scale")) {
      setFloatProperty("axesScale", floatParameter(++index));
      return;
    }
    // axes position [x y %]
    if (type.equals("position")) {
      Point3f xyp;
      if (tokAt(++index) == Token.off) {
        xyp = new Point3f();
      } else {
        xyp = xypParameter(index);
        if (xyp == null)
          error(ERROR_invalidArgument);
        index = iToken;
      }
      setShapeProperty(JmolConstants.SHAPE_AXES, "position", xyp);
      return;
    }
    int mad = getSetAxesTypeMad(index);
    if (!isSyntaxCheck)
      viewer.setObjectMad(JmolConstants.SHAPE_AXES, "axes", mad);
  }

  private void boundbox(int index) throws ScriptException {
    boolean byCorner = false;
    if (tokAt(index) == Token.identifier)
      byCorner = (parameterAsString(index).equalsIgnoreCase("corners"));
    if (byCorner)
      index++;
    if (isCenterParameter(index)) {
      expressionResult = null;
      Point3f pt1 = centerParameter(index);
      index = iToken + 1;
      if (byCorner || isCenterParameter(index)) {
        // boundbox CORNERS {expressionOrPoint1} {expressionOrPoint2}
        // boundbox {expressionOrPoint1} {vector}
        Point3f pt2 = (byCorner ? centerParameter(index) : getPoint3f(index,
            true));
        index = iToken + 1;
        if (!isSyntaxCheck)
          viewer.setBoundBox(pt1, pt2, byCorner);
      } else if (expressionResult != null && expressionResult instanceof BitSet) {
        // boundbox {expression}
        if (!isSyntaxCheck)
          viewer.calcBoundBoxDimensions((BitSet) expressionResult);
      } else {
        error(ERROR_invalidArgument);
      }
      if (index == statementLength)
        return;
    }
    int mad = getSetAxesTypeMad(index);
    if (!isSyntaxCheck)
      viewer.setObjectMad(JmolConstants.SHAPE_BBCAGE, "boundbox", mad);
  }

  private void unitcell(int index) throws ScriptException {
    if (statementLength == index + 1) {
      if (getToken(index).tok == Token.integer && intParameter(index) >= 111) {
        if (!isSyntaxCheck)
          viewer.setCurrentUnitCellOffset(intParameter(index));
      } else {
        int mad = getSetAxesTypeMad(index);
        if (!isSyntaxCheck)
          viewer.setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitCell", mad);
      }
      return;
    }
    // .xyz here?
    Point3f pt = (Point3f) getPointOrPlane(2, false, true, false, true, 3, 3);
    if (!isSyntaxCheck)
      viewer.setCurrentUnitCellOffset(pt);
  }

  private void frank(int index) throws ScriptException {
    setBooleanProperty("frank", booleanParameter(index));
  }

  private void setUserColors() throws ScriptException {
    Vector v = new Vector();
    for (int i = 2; i < statementLength; i++) {
      int argb = getArgbParam(i);
      v.addElement(new Integer(argb));
      i = iToken;
    }
    if (isSyntaxCheck)
      return;
    int n = v.size();
    int[] scale = new int[n];
    for (int i = n; --i >= 0;)
      scale[i] = ((Integer) v.elementAt(i)).intValue();
    Viewer.setUserScale(scale);
  }

  private void setBondmode() throws ScriptException {
    checkLength(3);
    boolean bondmodeOr = false;
    switch (getToken(2).tok) {
    case Token.opAnd:
      break;
    case Token.opOr:
      bondmodeOr = true;
      break;
    default:
      error(ERROR_invalidArgument);
    }
    setBooleanProperty("bondModeOr", bondmodeOr);
  }

  private void selectionHalo(int pt) throws ScriptException {
    boolean showHalo = false;
    switch (pt == statementLength ? Token.on : getToken(pt).tok) {
    case Token.on:
    case Token.selected:
      showHalo = true;
    case Token.off:
    case Token.none:
    case Token.normal:
      setBooleanProperty("selectionHalos", showHalo);
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void setEcho() throws ScriptException {
    String propertyName = "target";
    Object propertyValue = null;
    boolean echoShapeActive = true;
    // set echo xxx
    int len = 3;
    switch (getToken(2).tok) {
    case Token.off:
      checkLength(3);
      echoShapeActive = false;
      propertyName = "allOff";
      break;
    case Token.hide:
    case Token.hidden:
      propertyName = "hidden";
      propertyValue = Boolean.TRUE;
      break;
    case Token.on:
    case Token.display:
    case Token.displayed:
      propertyName = "hidden";
      propertyValue = Boolean.FALSE;
      break;
    case Token.none:
      echoShapeActive = false;
      // fall through
    case Token.all:
      checkLength(3);
      // fall through
    case Token.left:
    case Token.right:
    case Token.top:
    case Token.bottom:
    case Token.center:
    case Token.identifier:
      propertyValue = parameterAsString(2);
      break;
    case Token.model:
      int modelIndex = modelNumberParameter(3);
      if (isSyntaxCheck)
        return;
      if (modelIndex >= viewer.getModelCount())
        error(ERROR_invalidArgument);
      propertyName = "model";
      propertyValue = new Integer(modelIndex);
      len = 4;
      break;
    case Token.image:
      // set echo image "..."
      echo(3, true);
      return;
    case Token.depth:
      // set echo depth zzz
      propertyName = "%zpos";
      propertyValue = new Integer((int) floatParameter(3));
      len = 4;
      break;
    case Token.string:
      echo(2, false);
      return;
    default:
      error(ERROR_invalidArgument);
    }
    if (!isSyntaxCheck) {
      viewer.setEchoStateActive(echoShapeActive);
      viewer.loadShape(JmolConstants.SHAPE_ECHO);
      setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
    }
    if (statementLength == len)
      return;
    propertyName = "align";
    propertyValue = null;
    // set echo name xxx
    if (statementLength == 4) {
      if (isCenterParameter(3)) {
        setShapeProperty(JmolConstants.SHAPE_ECHO, "xyz", centerParameter(3));
        return;
      }
      switch (getToken(3).tok) {
      case Token.off:
        propertyName = "off";
        break;
      case Token.hidden:
        propertyName = "hidden";
        propertyValue = Boolean.TRUE;
        break;
      case Token.displayed:
      case Token.on:
        propertyName = "hidden";
        propertyValue = Boolean.FALSE;
        break;
      case Token.model:
        int modelIndex = modelNumberParameter(4);
        if (isSyntaxCheck)
          return;
        if (modelIndex >= viewer.getModelCount())
          error(ERROR_invalidArgument);
        propertyName = "model";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.left:
      case Token.right:
      case Token.top:
      case Token.bottom:
      case Token.center:
      case Token.identifier: // middle
        propertyValue = parameterAsString(3);
        break;
      default:
        error(ERROR_invalidArgument);
      }
      setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
      return;
    }
    // set echo name script "some script"
    // set echo name model x.y
    // set echo name depth nnnn
    // set echo name image "myimage.jpg"
    if (statementLength == 5) {
      switch (tokAt(3)) {
      case Token.script:
        propertyName = "script";
        propertyValue = parameterAsString(4);
        break;
      case Token.model:
        int modelIndex = modelNumberParameter(4);
        if (!isSyntaxCheck && modelIndex >= viewer.getModelCount())
          error(ERROR_invalidArgument);
        propertyName = "model";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.image:
        // set echo name image "xxx"
        echo(4, true);
        return;
      case Token.depth:
        propertyName = "%zpos";
        propertyValue = new Integer((int) floatParameter(4));
        break;
      }
      if (propertyValue != null) {
        setShapeProperty(JmolConstants.SHAPE_ECHO, propertyName, propertyValue);
        return;
      }
    }
    // set echo name [x y] or set echo name [x y %]
    // set echo name x-pos y-pos

    getToken(4);
    int i = 3;
    // set echo name {x y z}
    if (isCenterParameter(i)) {
      if (!isSyntaxCheck)
        setShapeProperty(JmolConstants.SHAPE_ECHO, "xyz", centerParameter(i));
      return;
    }
    String type = "xypos";
    if ((propertyValue = xypParameter(i)) == null) {
      int pos = intParameter(i++);
      propertyValue = new Integer(pos);
      if (tokAt(i) == Token.percent) {
        type = "%xpos";
        i++;
      } else {
        type = "xpos";
      }
      setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
      pos = intParameter(i++);
      propertyValue = new Integer(pos);
      if (tokAt(i) == Token.percent) {
        type = "%ypos";
        i++;
      } else {
        type = "ypos";
      }
    }
    setShapeProperty(JmolConstants.SHAPE_ECHO, type, propertyValue);
  }

  private boolean setLabel(String str) throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_LABELS);
    Object propertyValue = null;
    setShapeProperty(JmolConstants.SHAPE_LABELS, "setDefaults", viewer
        .getNoneSelected());
    while (true) {
      if (str.equals("scalereference")) {
        float scaleAngstromsPerPixel = floatParameter(2);
        if (scaleAngstromsPerPixel >= 5) // actually a zoom value
          scaleAngstromsPerPixel = viewer.getZoomSetting()
              / scaleAngstromsPerPixel
              / viewer.getScalePixelsPerAngstrom(false);
        propertyValue = new Float(scaleAngstromsPerPixel);
        break;
      }
      if (str.equals("offset")) {
        int xOffset = intParameter(2, -100, 100);
        int yOffset = intParameter(3, -100, 100);
        propertyValue = new Integer(Object2d.getOffset(xOffset, yOffset));
        break;
      }
      if (str.equals("alignment")) {
        switch (getToken(2).tok) {
        case Token.left:
        case Token.right:
        case Token.center:
          str = "align";
          propertyValue = theToken.value;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        break;
      }
      if (str.equals("pointer")) {
        int flags = Object2d.POINTER_NONE;
        switch (getToken(2).tok) {
        case Token.off:
        case Token.none:
          break;
        case Token.background:
          flags |= Object2d.POINTER_BACKGROUND;
        case Token.on:
          flags |= Object2d.POINTER_ON;
          break;
        default:
          error(ERROR_invalidArgument);
        }
        propertyValue = new Integer(flags);
        break;
      }
      if (str.equals("toggle")) {
        iToken = 1;
        BitSet bs = (statementLength == 2 ? null : expression(2));
        checkLength(iToken + 1);
        if (!isSyntaxCheck)
          viewer.togglePickingLabel(bs);
        return true;
      }
      iToken = 1;
      boolean TF = (statementLength == 2 || getToken(2).tok == Token.on);
      if (str.equals("front") || str.equals("group")) {
        if (!TF && tokAt(2) != Token.off)
          error(ERROR_invalidArgument);
        if (!TF)
          str = "front";
        propertyValue = (TF ? Boolean.TRUE : Boolean.FALSE);
        break;
      }
      if (str.equals("atom")) {
        if (!TF && tokAt(2) != Token.off)
          error(ERROR_invalidArgument);
        str = "front";
        propertyValue = (TF ? Boolean.FALSE : Boolean.TRUE);
        break;
      }
      return false;
    }
    BitSet bs = (iToken + 1 < statementLength ? expression(++iToken) : null);
    checkLength(iToken + 1);
    if (isSyntaxCheck)
      return true;
    if (bs == null)
      setShapeProperty(JmolConstants.SHAPE_LABELS, str, propertyValue);
    else
      viewer.setShapeProperty(JmolConstants.SHAPE_LABELS, str, propertyValue,
          bs);
    return true;
  }

  private void setMonitor() throws ScriptException {
    // on off here incompatible with "monitor on/off" so this is just a SET
    // option.
    boolean showMeasurementNumbers = false;
    checkLength(3);
    switch (tokAt(2)) {
    case Token.on:
      showMeasurementNumbers = true;
    case Token.off:
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "showMeasurementNumbers",
          showMeasurementNumbers ? Boolean.TRUE : Boolean.FALSE);
      return;
    case Token.identifier:
      setMeasurementUnits(parameterAsString(2));
      return;
    }
    setShapeSize(JmolConstants.SHAPE_MEASURES, getSetAxesTypeMad(2));
  }

  private boolean setMeasurementUnits(String units) throws ScriptException {
    if (!StateManager.isMeasurementUnit(units))
      error(ERROR_unrecognizedParameter, "set measurementUnits ", units);
    if (!isSyntaxCheck)
      viewer.setMeasureDistanceUnits(units);
    return true;
  }

  private void setProperty() throws ScriptException {
    // what possible good is this?
    // set property foo bar is identical to
    // set foo bar
    checkLength(4);
    if (getToken(2).tok != Token.identifier)
      error(ERROR_propertyNameExpected);
    String propertyName = parameterAsString(2);
    switch (getToken(3).tok) {
    case Token.on:
      setBooleanProperty(propertyName, true);
      break;
    case Token.off:
      setBooleanProperty(propertyName, false);
      break;
    case Token.integer:
      setIntProperty(propertyName, intParameter(3));
      break;
    case Token.decimal:
      setFloatProperty(propertyName, floatParameter(3));
      break;
    case Token.string:
      setStringProperty(propertyName, stringParameter(3));
      break;
    default:
      error(ERROR_unrecognizedParameter, "SET " + propertyName.toUpperCase(),
          parameterAsString(3));
    }
  }

  private void setSpin(String key, int value) throws ScriptException {
    key = key.toLowerCase();
    if (Parser.isOneOf(key, "x;y;z;fps")) {
      if (!isSyntaxCheck)
        viewer.setSpin(key, value);
      return;
    }
    error(ERROR_unrecognizedParameter, "set SPIN ", parameterAsString(2));
  }

  private void setNav(String key, int value) throws ScriptException {
    key = key.toUpperCase();
    if (Parser.isOneOf(key, "X;Y;Z;FPS")) {
      if (!isSyntaxCheck)
        viewer.setSpin(key, value);
      return;
    }
    error(ERROR_unrecognizedParameter, "set NAV ", parameterAsString(2));
  }

  private void setSsbond() throws ScriptException {
    checkLength(3);
    boolean ssbondsBackbone = false;
    // viewer.loadShape(JmolConstants.SHAPE_SSSTICKS);
    switch (tokAt(2)) {
    case Token.backbone:
      ssbondsBackbone = true;
      break;
    case Token.sidechain:
      break;
    default:
      error(ERROR_invalidArgument);
    }
    setBooleanProperty("ssbondsBackbone", ssbondsBackbone);
  }

  private void setHbond() throws ScriptException {
    checkLength(3);
    boolean bool = false;
    switch (tokAt(2)) {
    case Token.backbone:
      bool = true;
      // fall into
    case Token.sidechain:
      setBooleanProperty("hbondsBackbone", bool);
      break;
    case Token.solid:
      bool = true;
      // falll into
    case Token.dotted:
      setBooleanProperty("hbondsSolid", bool);
      break;
    default:
      error(ERROR_invalidArgument);
    }
  }

  private void setPicking() throws ScriptException {
    // set picking
    if (statementLength == 2) {
      setStringProperty("picking", "identify");
      return;
    }
    // set picking @{"xxx"} or some large length, ignored
    if (statementLength > 4 || tokAt(2) == Token.string) {
      setStringProperty("picking", stringSetting(2, false));
      return;
    }
    int i = 2;
    // set picking select ATOM|CHAIN|GROUP|MOLECULE|MODEL|SITE
    // set picking measure ANGLE|DISTANCE|TORSION
    // set picking spin fps
    String type = "SELECT";
    switch (getToken(2).tok) {
    case Token.select:
    case Token.monitor:
    case Token.spin:
      checkLength34();
      if (statementLength == 4) {
        type = parameterAsString(2).toUpperCase();
        if (type.equals("SPIN"))
          setIntProperty("pickingSpinRate", intParameter(3));
        else
          i = 3;
      }
      break;
    default:
      checkLength(3);
    }

    // set picking on
    // set picking normal
    // set picking identify
    // set picking off
    // set picking select
    // set picking bonds
    // set picking dragselected

    String str = parameterAsString(i);
    switch (getToken(i).tok) {
    case Token.on:
    case Token.normal:
      str = "identify";
      break;
    case Token.none:
      str = "off";
      break;
    case Token.select:
      str = "atom";
      break;
    case Token.label:
      str = "label";
      break;
    case Token.bonds: // not implemented
      str = "bond";
      break;
    }
    int mode = JmolConstants.getPickingMode(str);
    if (mode < 0)
      error(ERROR_unrecognizedParameter, "SET PICKING " + type, str);
    setStringProperty("picking", str);
  }

  private void setPickingStyle() throws ScriptException {
    if (statementLength > 4 || tokAt(2) == Token.string) {
      setStringProperty("pickingStyle", stringSetting(2, false));
      return;
    }
    int i = 2;
    boolean isMeasure = false;
    String type = "SELECT";
    switch (getToken(2).tok) {
    case Token.monitor:
      isMeasure = true;
      type = "MEASURE";
      // fall through
    case Token.select:
      checkLength34();
      if (statementLength == 4)
        i = 3;
      break;
    default:
      checkLength(3);
    }
    String str = parameterAsString(i);
    switch (getToken(i).tok) {
    case Token.none:
    case Token.off:
      str = (isMeasure ? "measureoff" : "toggle");
      break;
    case Token.on:
      if (isMeasure)
        str = "measure";
      break;
    }
    if (JmolConstants.getPickingStyle(str) < 0)
      error(ERROR_unrecognizedParameter, "SET PICKINGSTYLE " + type, str);
    setStringProperty("pickingStyle", str);
  }

  private void save() throws ScriptException {
    if (statementLength > 1) {
      String saveName = optParameterAsString(2);
      switch (tokAt(1)) {
      case Token.rotation:
        if (!isSyntaxCheck)
          viewer.saveOrientation(saveName);
        return;
      case Token.orientation:
        if (!isSyntaxCheck)
          viewer.saveOrientation(saveName);
        return;
      case Token.bonds:
        if (!isSyntaxCheck)
          viewer.saveBonds(saveName);
        return;
      case Token.state:
        if (!isSyntaxCheck)
          viewer.saveState(saveName);
        return;
      case Token.structure:
        if (!isSyntaxCheck)
          viewer.saveStructure(saveName);
        return;
      case Token.coord:
        if (!isSyntaxCheck)
          viewer.saveCoordinates(saveName, viewer.getSelectionSet());
        return;
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("selection")) {
          if (!isSyntaxCheck)
            viewer.saveSelection(saveName);
          return;
        }
      }
    }
    error(ERROR_what, "SAVE",
        "bonds? coordinates? orientation? selection? state? structure?");
  }

  private void restore() throws ScriptException {
    // restore orientation name time
    if (statementLength > 1) {
      String saveName = optParameterAsString(2);
      if (getToken(1).tok != Token.orientation)
        checkLength23();
      float timeSeconds;
      switch (getToken(1).tok) {
      case Token.rotation:
        timeSeconds = (statementLength > 3 ? floatParameter(3) : 0);
        if (timeSeconds < 0)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck)
          viewer.restoreRotation(saveName, timeSeconds);
        return;
      case Token.orientation:
        timeSeconds = (statementLength > 3 ? floatParameter(3) : 0);
        if (timeSeconds < 0)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck)
          viewer.restoreOrientation(saveName, timeSeconds);
        return;
      case Token.bonds:
        if (!isSyntaxCheck)
          viewer.restoreBonds(saveName);
        return;
      case Token.coord:
        if (isSyntaxCheck)
          return;
        String script = viewer.getSavedCoordinates(saveName);
        if (script == null)
          error(ERROR_invalidArgument);
        runScript(script);
        return;
      case Token.state:
        if (isSyntaxCheck)
          return;
        String state = viewer.getSavedState(saveName);
        if (state == null)
          error(ERROR_invalidArgument);
        runScript(state);
        return;
      case Token.structure:
        if (isSyntaxCheck)
          return;
        String shape = viewer.getSavedStructure(saveName);
        if (shape == null)
          error(ERROR_invalidArgument);
        runScript(shape);
        return;
      case Token.identifier:
        if (parameterAsString(1).equalsIgnoreCase("selection")) {
          if (!isSyntaxCheck)
            viewer.restoreSelection(saveName);
          return;
        }
      }
    }
    error(ERROR_what, "RESTORE",
        "bonds? coords? orientation? selection? state? structure?");
  }

  String write(Token[] args) throws ScriptException {
    int pt = 0;
    boolean isApplet = viewer.isApplet();
    boolean isCommand = false;
    String driverList = viewer.getExportDriverList();
    if (args == null) {
      args = statement;
      isCommand = true;
      pt++;
    }
    int argCount = (isCommand ? statementLength : args.length);
    int tok = (isCommand && args.length == 1 ? Token.clipboard
        : tokAt(pt, args));
    int len = 0;
    int width = -1;
    int height = -1;
    String type = "SPT";
    String data = "";
    String type2 = "";
    String fileName = null;
    boolean isCoord = false;
    boolean isShow = false;
    boolean isExport = false;
    BitSet bsFrames = null;
    int quality = Integer.MIN_VALUE;
    if (tok == Token.string) {
      Token t = Token.getTokenFromName(ScriptVariable.sValue(args[pt]));
      if (t != null)
        tok = t.tok;
    }
    switch (tok) {
    case Token.pointgroup:
      type = "PGRP";
      pt++;
      type2 = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (type2.equals("draw"))
        pt++;
      break;
    case Token.quaternion:
      pt++;
      type2 = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (Parser.isOneOf(type2, "w;x;y;z;a;r"))
        pt++;
      else
        type2 = "w";
      type = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      boolean isDerivative = (type.indexOf("deriv") == 0 || type
          .indexOf("diff") == 0);
      if (isDerivative || type2.equals("a") || type2.equals("r")) {
        type2 += " difference" + (type.indexOf("2") >= 0 ? "2" : "");
        if (isDerivative)
          type = ScriptVariable.sValue(tokenAt(++pt, args)).toLowerCase();
      }
      if (type.equals("draw")) {
        type2 += " draw";
        pt++;
      }
      type2 = "quaternion " + type2;
      type = "QUAT";
      break;
    case Token.ramachandran:
      pt++;
      type2 = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (Parser.isOneOf(type2, "r;c;p"))
        pt++;
      else
        type2 = "";
      type = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (type.equals("draw")) {
        type2 += " draw";
        pt++;
      }
      type2 = "ramachandran " + type2;
      type = "RAMA";
      break;
    case Token.function:
      type = "FUNCS";
      pt++;
      break;
    case Token.coord:
    case Token.data:
      type = ScriptVariable.sValue(tokenAt(++pt, args)).toLowerCase();
      type = "data";
      isCoord = true;
      break;
    case Token.state:
    case Token.script:
      pt++;
      break;
    case Token.mo:
      type = "MO";
      pt++;
      break;
    case Token.isosurface:
      type = "ISO";
      pt++;
      break;
    case Token.history:
      type = "HIS";
      pt++;
      break;
    case Token.var:
      pt += 2;
      type = "VAR";
      break;
    case Token.file:
      type = "FILE";
      pt++;
      break;
    case Token.image:
    case Token.identifier:
    case Token.string:
    case Token.frame:
      type = ScriptVariable.sValue(tokenAt(pt, args)).toLowerCase();
      if (tok == Token.image) {
        pt++;
      } else if (tok == Token.frame) {
        if (args[++pt].tok == Token.expressionBegin
            || args[pt].tok == Token.bitset) {
          bsFrames = expression(args, pt, 0, true, false, true, true);
          pt = iToken + 1;
        } else {
          bsFrames = viewer.getModelAtomBitSet(-1, false);
        }
        if (!isSyntaxCheck)
          bsFrames = viewer.getModelBitSet(bsFrames, true);
      } else if (Parser.isOneOf(type, driverList.toLowerCase())) {
        // povray, maya, vrml, idtf
        pt++;
        type = type.substring(0, 1).toUpperCase() + type.substring(1);
        isExport = true;
        fileName = "Jmol." + type;
      } else if (type.equals("menu")) {
        pt++;
        type = "MENU";
      } else {
        type = "(image)";
      }
      if (tokAt(pt, args) == Token.integer) {
        width = ScriptVariable.iValue(tokenAt(pt++, args));
        height = ScriptVariable.iValue(tokenAt(pt++, args));
      }
      break;
    }
    String val = ScriptVariable.sValue(tokenAt(pt, args));
    if (val.equalsIgnoreCase("clipboard")) {
      if (isSyntaxCheck)
        return "";
      // if (isApplet)
      // evalError(GT._("The {0} command is not available for the applet.",
      // "WRITE CLIPBOARD"));
    } else if (Parser.isOneOf(val.toLowerCase(), "png;jpg;jpeg;jpg64;jpeg64")
        && tokAt(pt + 1, args) == Token.integer) {
      quality = ScriptVariable.iValue(tokenAt(++pt, args));
    } else if (Parser.isOneOf(val.toLowerCase(), "xyz;mol;pdb")) {
      type = val.toUpperCase();
      if (pt + 1 == argCount)
        pt++;
    }

    // write [image|history|state] clipboard

    // write [optional image|history|state] [JPG quality|JPEG quality|JPG64
    // quality|PNG|PPM|SPT] "filename"
    // write script "filename"
    // write isosurface t.jvxl

    if (type.equals("(image)")
        && Parser.isOneOf(val.toUpperCase(),
            "GIF;JPG;JPG64;JPEG;JPEG64;PNG;PPM")) {
      type = val.toUpperCase();
      pt++;
    }

    if (pt + 2 == argCount) {
      data = ScriptVariable.sValue(tokenAt(++pt, args));
      if (data.charAt(0) != '.')
        type = val.toUpperCase();
    }
    switch (tokAt(pt, args)) {
    case Token.nada:
      isShow = true;
      break;
    case Token.identifier:
    case Token.string:
      fileName = ScriptVariable.sValue(tokenAt(pt, args));
      if (pt == argCount - 3 && tokAt(pt + 1, args) == Token.period) {
        // write filename.xxx gets separated as filename .spt
        // write isosurface filename.xxx also
        fileName += "." + ScriptVariable.sValue(tokenAt(pt + 2, args));
      }
      if (type != "VAR" && pt == 1)
        type = "image";
      else if (fileName.length() > 0 && fileName.charAt(0) == '.'
          && (pt == 2 || pt == 3)) {
        fileName = ScriptVariable.sValue(tokenAt(pt - 1, args)) + fileName;
        if (type != "VAR" && pt == 2)
          type = "image";
      }
      if (fileName.equalsIgnoreCase("clipboard"))
        fileName = null;
      break;
    case Token.clipboard:
      break;
    default:
      error(ERROR_invalidArgument);
    }
    if (type.equals("image") || type.equals("frame")) {
      if (fileName != null && fileName.indexOf(".") >= 0)
        type = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
      else
        type = "JPG";
      if (type.equals("MNU"))
        type = "MENU";
      else if (type.equals("WRL") || type.equals("VRML")) {
        type = "Vrml";
        isExport = true;
      } else if (type.equals("X3D")) {
        type = "X3d";
        isExport = true;
      } else if (type.equals("IDTF")) {
        type = "Idtf";
        isExport = true;
      } else if (type.equals("MA")) {
        type = "Maya";
        isExport = true;
      }
    }
    if (type.equals("data")) {
      if (fileName != null && fileName.indexOf(".") >= 0)
        type = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
      else
        type = "XYZ";
    }
    boolean isImage = Parser.isOneOf(type, "GIF;JPEG64;JPEG;JPG64;JPG;PPM;PNG");
    if (isImage && (isApplet && !viewer.isSignedApplet() || isShow))
      type = "JPG64";
    if (!isImage
        && !isExport
        && !Parser.isOneOf(type,
            "SPT;HIS;MO;ISO;VAR;FILE;XYZ;MENU;MOL;PDB;PGRP;QUAT;RAMA;FUNCS;"))
      error(
          ERROR_writeWhat,
          "COORDS|FILE|FUNCTIONS|HISTORY|IMAGE|ISOSURFACE|MENU|MO|POINTGROUP|QUATERNION [w,x,y,z] [derivative]"
              + "|RAMACHANDRAN|STATE|VAR x  CLIPBOARD",
          "JPG|JPG64|PNG|GIF|PPM|SPT|JVXL|XYZ|MOL|PDB|"
              + driverList.toUpperCase().replace(';', '|'));
    if (isSyntaxCheck)
      return "";
    data = type.intern();
    Object bytes = null;
    if (isExport) {
      // POV-Ray uses a BufferedWriter instead of a StringBuffer.
      boolean isPovRay = type.equals("Povray");
      data = viewer.generateOutput(data, isPovRay ? fileName : null, width,
          height);
      if (data == null)
        return "";
      if (isPovRay) {
        if (!isCommand)
          return data;
        fileName = data.substring(data.indexOf("File created: ") + 14);
        fileName = fileName.substring(0, fileName.indexOf("\n"));
        fileName = fileName.substring(0, fileName.lastIndexOf(" ("));
        String msg = viewer.createImage(fileName + ".ini", "ini", data,
            Integer.MIN_VALUE, 0, 0, null);
        if (msg != null) {
          if (!msg.startsWith("OK"))
            evalError(msg, null);
          scriptStatusOrBuffer("Created " + fileName + ".ini:\n\n" + data);
        }
        return "";
      }
    } else if (data == "MENU") {
      data = viewer.getMenu("");
    } else if (data == "PGRP") {
      data = viewer.getPointGroupAsString(type2.equals("draw"), null, 0, 1.0f);
    } else if (data == "PDB") {
      data = viewer.getPdbData(null);
    } else if (data == "XYZ" || data == "MOL") {
      data = viewer.getData("selected", data);
    } else if (data == "QUAT" || data == "RAMA") {
      int modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        error(ERROR_multipleModelsNotOK, "write " + type2);
      data = viewer.getPdbData(modelIndex, type2);
      type = "PDB";
    } else if (data == "FUNCS") {
      data = getFunctionCalls("");
      type = "TXT";
    } else if (data == "FILE") {
      if (isShow)
        data = viewer.getCurrentFileAsString();
      else
        bytes = viewer.getCurrentFileAsBytes();
      if ("?".equals(fileName))
        fileName = "?Jmol." + viewer.getParameter("_fileType");
      quality = Integer.MIN_VALUE;
    } else if (data == "VAR") {
      data = ""
          + getParameter(ScriptVariable
              .sValue(tokenAt(isCommand ? 2 : 1, args)), false);
      type = "TXT";
    } else if (data == "SPT") {
      if (isCoord) {
        BitSet tainted = viewer.getTaintedAtoms(AtomCollection.TAINT_COORD);
        viewer.setAtomCoordRelative(new Point3f(0, 0, 0));
        data = (String) viewer.getProperty("string", "stateInfo", null);
        viewer.setTaintedAtoms(tainted, AtomCollection.TAINT_COORD);
      } else {
        data = (String) viewer.getProperty("string", "stateInfo", null);
      }
    } else if (data == "HIS") {
      data = viewer.getSetHistory(Integer.MAX_VALUE);
      type = "SPT";
    } else if (data == "MO") {
      data = getMoJvxl(Integer.MAX_VALUE);
      type = "JVXL";
    } else if (data == "ISO") {
      if ((data = getIsosurfaceJvxl()) == null)
        error(ERROR_noData);
      if (!isShow)
        showString((String) viewer.getShapeProperty(
            JmolConstants.SHAPE_ISOSURFACE, "jvxlFileInfo"));
      type = "JVXL";
    } else {
      // image
      len = -1;
      if (quality < 0)
        quality = -1;
    }
    if (data == null)
      data = "";
    if (len == 0)
      len = (bytes == null ? data.length()
          : bytes instanceof String ? ((String) bytes).length()
              : ((byte[]) bytes).length);
    if (isImage) {
      refresh();
      if (width < 0)
        width = viewer.getScreenWidth();
      if (height < 0)
        height = viewer.getScreenHeight();
    }
    if (!isCommand)
      return data;
    if (isShow) {
      showString(data);
    } else if (bytes != null && bytes instanceof String) {
      // load error here
      scriptStatusOrBuffer((String) bytes);
    } else {
      if (bytes == null && (!isImage || fileName != null))
        bytes = data;
      String msg = viewer.createImage(fileName, type, bytes, quality, width,
          height, bsFrames);
      if (msg != null) {
        if (!msg.startsWith("OK"))
          evalError(msg, null);
        scriptStatusOrBuffer(msg
            + (isImage ? "; width=" + width + "; height=" + height : ""));
      }
    }
    return "";
  }

  private void show() throws ScriptException {
    String value = null;
    String str = parameterAsString(1);
    String msg = null;
    checkLength(-3);
    int len = 2;
    if (statementLength == 2 && str.indexOf("?") >= 0) {
      showString(viewer.getAllSettings(str.substring(0, str.indexOf("?"))));
      return;
    }
    int tok;
    switch (tok = (getToken(1) instanceof ScriptVariable ? Token.nada : getToken(1).tok)) {
    case Token.nada:
      msg = Escape.escape(((ScriptVariable)theToken).value);
      break;
    case Token.vanderwaals:
      if (statementLength == 2) {
        if (!isSyntaxCheck)
          showString(viewer.getDefaultVdw(-1));
        return;
      }
      int iMode = JmolConstants.getVdwType(parameterAsString(2));
      if (iMode < 0)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck)
        showString(viewer.getDefaultVdw(iMode));
      return;
    case Token.function:
      checkLength23();
      if (!isSyntaxCheck)
        showString(getFunctionCalls(optParameterAsString(2)));
      return;
    case Token.set:
      checkLength(2);
      if (!isSyntaxCheck)
        showString(viewer.getAllSettings(null));
      return;
    case Token.url:
      // in a new window
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          viewer.showUrl(getFullPathName());
        return;
      }
      String fileName = parameterAsString(2);
      if (!isSyntaxCheck)
        viewer.showUrl(fileName);
      return;
    case Token.color:
    case Token.defaultColors:
      str = "defaultColorScheme";
      break;
    case Token.scale3d:
      str = "scaleAngstromsPerInch";
      break;
    case Token.quaternion:
    case Token.ramachandran:
      if (isSyntaxCheck)
        return;
      int modelIndex = viewer.getCurrentModelIndex();
      if (modelIndex < 0)
        error(ERROR_multipleModelsNotOK, "show " + theToken.value);
      msg = viewer.getPdbData(modelIndex,
          theTok == Token.quaternion ? "quaternion w" : "ramachandran");
      break;
    case Token.trace:
      if (!isSyntaxCheck)
        msg = getContext(false);
      break;
    case Token.identifier:
      if (str.equalsIgnoreCase("variables")) {
        if (!isSyntaxCheck)
          msg = viewer.getVariableList() + getContext(true);
      } else if (str.equalsIgnoreCase("historyLevel")) {
        value = "" + commandHistoryLevelMax;
      } else if (str.equalsIgnoreCase("defaultLattice")) {
        value = Escape.escape(viewer.getDefaultLattice());
      } else if (str.equalsIgnoreCase("logLevel")) {
        value = "" + Viewer.getLogLevel();
      } else if (str.equalsIgnoreCase("fileHeader")) {
        if (!isSyntaxCheck)
          msg = viewer.getPDBHeader();
      } else if (str.equalsIgnoreCase("debugScript")) {
        value = "" + viewer.getDebugScript();
      } else if (str.equalsIgnoreCase("colorScheme")) {
        String name = optParameterAsString(2);
        if (name.length() > 0)
          len = 3;
        if (!isSyntaxCheck)
          value = viewer.getColorSchemeList(name, true);
      } else if (str.equalsIgnoreCase("menu")) {
        if (!isSyntaxCheck)
          value = viewer.getMenu("");
      } else if (str.equalsIgnoreCase("strandCount")) {
        msg = "set strandCountForStrands "
            + viewer.getStrandCount(JmolConstants.SHAPE_STRANDS)
            + "; set strandCountForMeshRibbon "
            + viewer.getStrandCount(JmolConstants.SHAPE_MESHRIBBON);
      } else if (str.equalsIgnoreCase("trajectory")
          || str.equalsIgnoreCase("trajectories")) {
        msg = viewer.getTrajectoryInfo();
      }
      break;
    case Token.minimize:
      msg = viewer.getMinimizationInfo();
      break;
    case Token.axes:
      switch (viewer.getAxesMode()) {
      case JmolConstants.AXES_MODE_UNITCELL:
        msg = "set axesUnitcell";
        break;
      case JmolConstants.AXES_MODE_BOUNDBOX:
        msg = "set axesWindow";
        break;
      default:
        msg = "set axesMolecular";
      }
      break;
    case Token.bondmode:
      msg = "set bondMode " + (viewer.getBondSelectionModeOr() ? "OR" : "AND");
      break;
    case Token.strands:
      msg = "set strandCountForStrands "
          + viewer.getStrandCount(JmolConstants.SHAPE_STRANDS)
          + "; set strandCountForMeshRibbon "
          + viewer.getStrandCount(JmolConstants.SHAPE_MESHRIBBON);
      break;
    case Token.hbond:
      msg = "set hbondsBackbone " + viewer.getHbondsBackbone()
          + ";set hbondsSolid " + viewer.getHbondsSolid();
      break;
    case Token.spin:
      msg = viewer.getSpinState();
      break;
    case Token.ssbond:
      msg = "set ssbondsBackbone " + viewer.getSsbondsBackbone();
      break;
    case Token.display:// deprecated
    case Token.selectionHalo:
      msg = "selectionHalos "
          + (viewer.getSelectionHaloEnabled() ? "ON" : "OFF");
      break;
    case Token.hetero:
      msg = "set selectHetero " + viewer.getRasmolHeteroSetting();
      break;
    case Token.hydrogen:
      msg = "set selectHydrogens " + viewer.getRasmolHydrogenSetting();
      break;
    case Token.ambient:
    case Token.diffuse:
    case Token.specular:
    case Token.specpower:
    case Token.specexponent:
      msg = viewer.getSpecularState();
      break;
    case Token.save:
      if (!isSyntaxCheck)
        msg = viewer.listSavedStates();
      break;
    case Token.unitcell:
      if (!isSyntaxCheck)
        msg = viewer.getUnitCellInfoText();
      break;
    case Token.coord:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getCoordinateState(viewer.getSelectionSet());
        break;
      }
      String nameC = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSavedCoordinates(nameC);
      break;
    case Token.state:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getStateInfo();
        break;
      }
      String name = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSavedState(name);
      break;
    case Token.structure:
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getProteinStructureState();
        break;
      }
      String shape = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getSavedStructure(shape);
      break;
    case Token.data:
      String type = ((len = statementLength) == 3 ? parameterAsString(2) : null);
      if (!isSyntaxCheck) {
        Object[] data = (type == null ? this.data : viewer.getData(type));
        msg = (data == null ? "no data" : "data \""
            + data[0]
            + "\"\n"
            + (data[1] instanceof float[] ? Escape.escape((float[]) data[1],
                true) : data[1] instanceof float[][] ? Escape.escape(
                (float[][]) data[1], false) : "" + data[1]))
            + "\nend \"" + data[0] + "\";";
      }
      break;
    case Token.spacegroup:
      Hashtable info = null;
      if ((len = statementLength) == 2) {
        if (!isSyntaxCheck) {
          info = viewer.getSpaceGroupInfo(null);
        }
      } else {
        String sg = parameterAsString(2);
        if (!isSyntaxCheck)
          info = viewer.getSpaceGroupInfo(TextFormat.simpleReplace(sg, "''",
            "\""));
      }
      if (info != null)
        msg = "" + info.get("spaceGroupInfo") + info.get("symmetryInfo");
      break;
    case Token.dollarsign:
      len = 3;
      msg = setObjectProperty();
      break;
    case Token.boundbox:
      if (!isSyntaxCheck) {
        msg = viewer.getBoundBoxCommand(true);
      }
      break;
    case Token.center:
      if (!isSyntaxCheck)
        msg = "center " + Escape.escape(viewer.getRotationCenter());
      break;
    case Token.draw:
      if (!isSyntaxCheck)
        msg = (String) viewer.getShapeProperty(JmolConstants.SHAPE_DRAW,
            "command");
      break;
    case Token.file:
      // as as string
      if (statementLength == 2) {
        if (!isSyntaxCheck)
          msg = viewer.getCurrentFileAsString();
        break;
      }
      len = 3;
      value = parameterAsString(2);
      if (!isSyntaxCheck)
        msg = viewer.getFileAsString(value);
      break;
    case Token.frame:
      if (tokAt(2) == Token.all && (len = 3) > 0)
        msg = viewer.getModelFileInfoAll();
      else
        msg = viewer.getModelFileInfo();
      break;
    case Token.history:
      int n = ((len = statementLength) == 2 ? Integer.MAX_VALUE
          : intParameter(2));
      if (n < 1)
        error(ERROR_invalidArgument);
      if (!isSyntaxCheck) {
        viewer.removeCommand();
        msg = viewer.getSetHistory(n);
      }
      break;
    case Token.isosurface:
      if (!isSyntaxCheck)
        msg = (String) viewer.getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
            "jvxlFileData");
      break;
    case Token.mo:
      if (optParameterAsString(2).equalsIgnoreCase("list")) {
        msg = viewer.getMoInfo(-1);
        len = 3;
      } else {
        int ptMO = ((len = statementLength) == 2 ? Integer.MIN_VALUE
            : intParameter(2));
        if (!isSyntaxCheck)
          msg = getMoJvxl(ptMO);
      }
      break;
    case Token.model:
      if (!isSyntaxCheck)
        msg = viewer.getModelInfoAsString();
      break;
    case Token.monitor:
      if (!isSyntaxCheck)
        msg = viewer.getMeasurementInfoAsString();
      break;
    case Token.translation:
    case Token.rotation:
    case Token.moveto:
      if (!isSyntaxCheck)
        msg = viewer.getOrientationText(tok);
      break;
    case Token.orientation:
      if (!isSyntaxCheck)
        msg = viewer.getOrientationText(tokAt(2));
      len = (statementLength == 3 ? 3 : 2);
      break;
    case Token.pdbheader:
      if (!isSyntaxCheck)
        msg = viewer.getPDBHeader();
      break;
    case Token.pointgroup:
      pointGroup();
      return;
    case Token.symmetry:
      if (!isSyntaxCheck)
        msg = viewer.getSymmetryInfoAsString();
      break;
    case Token.transform:
      if (!isSyntaxCheck)
        msg = "transform:\n" + viewer.getTransformText();
      break;
    case Token.zoom:
      msg = "zoom "
          + (viewer.getZoomEnabled() ? ("" + viewer.getZoomSetting()) : "off");
      break;
    case Token.frank:
      msg = (viewer.getShowFrank() ? "frank ON" : "frank OFF");
      break;
    case Token.radius:
      str = "solventProbeRadius";
      break;
    // Chime related
    case Token.chain:
    case Token.sequence:
    case Token.residue:
    case Token.selected:
    case Token.group:
    case Token.atoms:
    case Token.info:
    case Token.bonds:
      msg = viewer.getChimeInfo(tok);
      break;
    // not implemented
    case Token.echo:
    case Token.fontsize:
    case Token.property: // huh? why?
    case Token.help:
    case Token.solvent:
      value = "?";
      break;
    }
    checkLength(len);
    if (isSyntaxCheck)
      return;
    if (msg != null)
      showString(msg);
    else if (value != null)
      showString(str + " = " + value);
    else if (str != null) {
      if (str.indexOf(" ") >= 0)
        showString(str);
      else
        showString(str + " = " + getParameterEscaped(str));
    }
  }

  private String getFunctionCalls(String selectedFunction) {
    StringBuffer s = new StringBuffer();
    int pt = selectedFunction.indexOf("*");
    boolean isGeneric = (pt >= 0);
    boolean isLocal = (selectedFunction.indexOf("_") != 0);
    boolean namesOnly = (selectedFunction.equalsIgnoreCase("names") || selectedFunction.equalsIgnoreCase("_names"));
    if (namesOnly)
      selectedFunction = "";
    if (isGeneric)
      selectedFunction = selectedFunction.substring(0, pt);
    selectedFunction = selectedFunction.toLowerCase();
    Hashtable ht = viewer.getFunctions(isLocal);
    String[] names = new String[ht.size()];
    Enumeration e = ht.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      if (selectedFunction.length() == 0
          || name.equalsIgnoreCase(selectedFunction) || isGeneric
          && name.toLowerCase().indexOf(selectedFunction) == 0)
        names[n++] = name;
    }
    Arrays.sort(names, 0, n);
    for (int i = 0; i < n; i++) {
      ScriptFunction f = (ScriptFunction) ht.get(names[i]);
      s.append(namesOnly ? f.getSignature() : f.toString());
      s.append('\n');
    }
    return s.toString();
  }

  private String getIsosurfaceJvxl() {
    if (isSyntaxCheck)
      return "";
    return (String) viewer.getShapeProperty(JmolConstants.SHAPE_ISOSURFACE,
        "jvxlFileData");
  }

  private String getMoJvxl(int ptMO) throws ScriptException {
    // 0: all; Integer.MAX_VALUE: current;
    viewer.loadShape(JmolConstants.SHAPE_MO);
    int modelIndex = viewer.getDisplayModelIndex();
    if (modelIndex < 0)
      error(ERROR_multipleModelsNotOK, "MO isosurfaces");
    Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,
        "moData");
    if (moData == null)
      error(ERROR_moModelError);
    setShapeProperty(JmolConstants.SHAPE_MO, "init", new Integer(modelIndex));
    setShapeProperty(JmolConstants.SHAPE_MO, "moData", moData);
    return (String) viewer.getShapeProperty(JmolConstants.SHAPE_MO, "showMO",
        ptMO);
  }

  private String extractCommandOption(String name) {
    int i = fullCommand.indexOf(name + "=");
    return (i < 0 ? null : Parser.getNextQuotedString(fullCommand, i));
  }

  private void draw() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_DRAW);
    switch (tokAt(1)) {
    case Token.list:
      if (listIsosurface(JmolConstants.SHAPE_DRAW))
        return;
      break;
    case Token.pointgroup:
      pointGroup();
      return;
    case Token.quaternion:
      dataFrame(JmolConstants.JMOL_DATA_QUATERNION);
      return;
    case Token.helix:
      dataFrame(JmolConstants.JMOL_DATA_QUATERNION);
      return;
    case Token.ramachandran:
      dataFrame(JmolConstants.JMOL_DATA_RAMACHANDRAN);
      return;
    }
    boolean havePoints = false;
    boolean isInitialized = false;
    boolean isSavedState = false;
    boolean isTranslucent = false;
    boolean isFrame = false;
    float translucentLevel = Float.MAX_VALUE;
    int colorArgb = Integer.MIN_VALUE;
    int intScale = 0;
    String swidth = "";
    int iptDisplayProperty = 0;
    Point3f center = null;
    String thisId = initIsosurface(JmolConstants.SHAPE_DRAW);
    boolean idSeen = (thisId != null);
    boolean isWild = (idSeen && viewer.getShapeProperty(
        JmolConstants.SHAPE_DRAW, "ID") == null);
    for (int i = iToken; i < statementLength; ++i) {
      String propertyName = null;
      Object propertyValue = null;
      int tok = getToken(i).tok;
      switch (tok) {
      case Token.symop:
        String xyz = null;
        int iSym = 0;
        if (tokAt(++i) == Token.string) {
          xyz = stringParameter(i);
        } else {
          iSym = intParameter(i);
        }
        center = (i + 1 == statementLength ? null : centerParameter(++i));
        // draw ID xxx symop [n or "x,-y,-z"] [optional {center}]
        i = iToken + 1;
        checkLength(iToken + 1);
        if (!isSyntaxCheck)
          runScript((String) viewer.getSymmetryInfo(null, xyz, iSym, center,
              thisId, Token.draw));
        return;
      case Token.frame:
        isFrame = true;
        // draw ID xxx frame {center} {q1 q2 q3 q4}
        continue;
      case Token.leftbrace:
      case Token.point4f:
      case Token.point3f:
        // {X, Y, Z}
        if (tok == Token.point4f || !isPoint3f(i)) {
          propertyValue = getPoint4f(i);
          if (isFrame) {
            checkLength(iToken + 1);
            if (!isSyntaxCheck)
              runScript((new Quaternion((Point4f) propertyValue)).draw(
                  (thisId == null ? "frame" : thisId), " " + swidth,
                  (center == null ? new Point3f() : center), intScale / 100f));
            return;
          }
          propertyName = "planedef";
        } else {
          propertyValue = center = getPoint3f(i, true);
          propertyName = "coord";
        }
        i = iToken;
        havePoints = true;
        break;
      case Token.plane:
        if (havePoints) {
          propertyValue = planeParameter(++i);
          i = iToken;
          propertyName = "planedef";
        } else {
          propertyName = "plane";
        }
        break;
      case Token.bitset:
      case Token.expressionBegin:
        propertyName = "atomSet";
        propertyValue = expression(i);
        if (isFrame)
          center = centerParameter(i);
        i = iToken;
        havePoints = true;
        break;
      case Token.list:
        propertyName = "modelBasedPoints";
        propertyValue = theToken.value;
        havePoints = true;
        break;
      case Token.comma: // ignore -- necessary between { } and [x y]
        break;
      case Token.leftsquare:
        // [x y] or [x y %]
        propertyValue = xypParameter(i);
        if (propertyValue != null) {
          i = iToken;
          propertyName = "coord";
          havePoints = true;
          break;
        }
        if (isSavedState)
          error(ERROR_invalidArgument);
        isSavedState = !isSavedState;
        break;
      case Token.rightsquare:
        if (!isSavedState)
          error(ERROR_invalidArgument);
        isSavedState = !isSavedState;
        break;
      case Token.reverse:
        propertyName = "reverse";
        break;
      case Token.string:
        propertyValue = stringParameter(i);
        propertyName = "title";
        break;
      case Token.vector:
        propertyName = "vector";
        break;
      case Token.length:
        propertyValue = new Float(floatParameter(++i));
        propertyName = "length";
        break;
      case Token.decimal:
        // $drawObject
        propertyValue = new Float(floatParameter(i));
        propertyName = "length";
        break;
      case Token.integer:
        if (isSavedState) {
          propertyName = "modelIndex";
          propertyValue = new Integer(intParameter(i));
        } else {
          intScale = intParameter(i);
        }
        break;
      case Token.scale:
        if (++i >= statementLength)
          error(ERROR_numberExpected);
        switch (getToken(i).tok) {
        case Token.integer:
          intScale = intParameter(i);
          continue;
        case Token.decimal:
          intScale = (int) (floatParameter(i) * 100);
          continue;
        }
        error(ERROR_numberExpected);
        break;
      case Token.times:
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("id")) {
          thisId = setShapeId(JmolConstants.SHAPE_DRAW, ++i, idSeen);
          isWild = (viewer.getShapeProperty(JmolConstants.SHAPE_DRAW, "ID") == null);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("LINE")) {
          propertyName = "line";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("FIXED")) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("MODELBASED")) {
          propertyName = "fixed";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (str.equalsIgnoreCase("CROSSED")) {
          propertyName = "crossed";
          break;
        }
        if (str.equalsIgnoreCase("CURVE")) {
          propertyName = "curve";
          break;
        }
        if (str.equalsIgnoreCase("ARROW")) {
          propertyName = "arrow";
          break;
        }
        if (str.equalsIgnoreCase("ARC")) {
          propertyName = "arc";
          break;
        }
        if (str.equalsIgnoreCase("CIRCLE")) {
          propertyName = "circle";
          break;
        }
        if (str.equalsIgnoreCase("CYLINDER")) {
          propertyName = "cylinder";
          break;
        }
        if (str.equalsIgnoreCase("VERTICES")) {
          propertyName = "vertices";
          break;
        }
        if (str.equalsIgnoreCase("NOHEAD")) {
          propertyName = "nohead";
          break;
        }
        if (str.equalsIgnoreCase("ROTATE45")) {
          propertyName = "rotate45";
          break;
        }
        if (str.equalsIgnoreCase("PERP")
            || str.equalsIgnoreCase("PERPENDICULAR")) {
          propertyName = "perp";
          break;
        }
        if (str.equalsIgnoreCase("OFFSET")) {
          Point3f pt = getPoint3f(++i, true);
          i = iToken;
          propertyName = "offset";
          propertyValue = pt;
          break;
        }
        if (str.equalsIgnoreCase("DIAMETER")) { // pixels
          float f = floatParameter(++i);
          propertyValue = new Float(f);
          propertyName = (tokAt(i) == Token.decimal ? "width" : "diameter");
          swidth = (String) propertyName
              + (tokAt(i) == Token.decimal ? " " + f : " " + ((int) f));
          break;
        }
        if (str.equalsIgnoreCase("WIDTH")) { // angstroms
          propertyValue = new Float(floatParameter(++i));
          propertyName = "width";
          swidth = (String) propertyName + " " + propertyValue;
          break;
        }
        setShapeId(JmolConstants.SHAPE_DRAW, i, idSeen);
        i = iToken;
        break;
      case Token.dollarsign:
        // $drawObject[m]
        if (tokAt(i + 2) == Token.leftsquare || isFrame) {
          Point3f pt = center = centerParameter(i);
          i = iToken;
          propertyName = "coord";
          propertyValue = pt;
          havePoints = true;
          break;
        }
        // $drawObject
        propertyValue = objectNameParameter(++i);
        propertyName = "identifier";
        havePoints = true;
        break;
      case Token.color:
        i++;
        // fall through
      case Token.translucent:
      case Token.opaque:
        isTranslucent = false;
        boolean isColor = false;
        if (tokAt(i) == Token.translucent) {
          isTranslucent = true;
          if (isFloatParameter(++i))
            translucentLevel = getTranslucentLevel(i++);
          isColor = true;
        } else if (tokAt(i) == Token.opaque) {
          ++i;
          isColor = true;
        }
        if (isColorParam(i)) {
          colorArgb = getArgbParam(i);
          i = iToken;
          isColor = true;
        }
        if (!isColor)
          error(ERROR_invalidArgument);
        idSeen = true;
        continue;
      default:
        if (iptDisplayProperty == 0)
          iptDisplayProperty = i;
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, 0, theTok))
          error(ERROR_invalidArgument);
        continue;
      }
      idSeen = (theTok != Token.delete);
      if (havePoints && !isInitialized && !isFrame) {
        setShapeProperty(JmolConstants.SHAPE_DRAW, "points", new Integer(
            intScale));
        isInitialized = true;
        intScale = 0;
      }
      if (havePoints && isWild)
        error(ERROR_invalidArgument);
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_DRAW, propertyName, propertyValue);
    }
    if (havePoints) {
      setShapeProperty(JmolConstants.SHAPE_DRAW, "set", null);
    }
    if (colorArgb != Integer.MIN_VALUE)
      setShapeProperty(JmolConstants.SHAPE_DRAW, "color",
          new Integer(colorArgb));
    if (isTranslucent)
      setShapeTranslucency(JmolConstants.SHAPE_DRAW, "", "translucent",
          translucentLevel, null);
    if (intScale != 0) {
      setShapeProperty(JmolConstants.SHAPE_DRAW, "scale", new Integer(intScale));
    }
    if (iptDisplayProperty > 0) {
      if (!setMeshDisplayProperty(JmolConstants.SHAPE_DRAW, iptDisplayProperty,
          getToken(iptDisplayProperty).tok))
        error(ERROR_invalidArgument);
    }
  }

  private void polyhedra() throws ScriptException {
    /*
     * needsGenerating:
     * 
     * polyhedra [number of vertices and/or basis] [at most two selection sets]
     * [optional type and/or edge] [optional design parameters]
     * 
     * OR else:
     * 
     * polyhedra [at most one selection set] [type-and/or-edge or on/off/delete]
     */
    boolean needsGenerating = false;
    boolean onOffDelete = false;
    boolean typeSeen = false;
    boolean edgeParameterSeen = false;
    boolean isDesignParameter = false;
    int nAtomSets = 0;
    viewer.loadShape(JmolConstants.SHAPE_POLYHEDRA);
    setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "init", null);
    String setPropertyName = "centers";
    String decimalPropertyName = "radius_";
    boolean isTranslucent = false;
    float translucentLevel = Float.MAX_VALUE;
    int color = Integer.MIN_VALUE;
    for (int i = 1; i < statementLength; ++i) {
      if (isColorParam(i)) {
        color = getArgbParam(i);
        i = iToken;
        continue;
      }
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.opEQ:
      case Token.comma:
        continue;
      case Token.bonds:
        if (nAtomSets > 0)
          error(ERROR_invalidParameterOrder);
        needsGenerating = true;
        propertyName = "bonds";
        break;
      case Token.radius:
        decimalPropertyName = "radius";
        continue;

      case Token.color:
        i++;
        // fall through
      case Token.translucent:
      case Token.opaque:
        isTranslucent = false;
        boolean isColor = false;
        if (tokAt(i) == Token.translucent) {
          isTranslucent = true;
          if (isFloatParameter(++i))
            translucentLevel = getTranslucentLevel(i++);
          isColor = true;
        } else if (tokAt(i) == Token.opaque) {
          ++i;
          isColor = true;
        }
        if (isColorParam(i)) {
          color = getArgbParam(i);
          i = iToken;
          isColor = true;
        }
        if (!isColor)
          error(ERROR_invalidArgument);
        continue;
      case Token.identifier:
        String str = parameterAsString(i);
        if ("collapsed".equalsIgnoreCase(str)) {
          propertyName = "collapsed";
          propertyValue = Boolean.TRUE;
          if (typeSeen)
            error(ERROR_incompatibleArguments);
          typeSeen = true;
          break;
        }
        if ("flat".equalsIgnoreCase(str)) {
          propertyName = "collapsed";
          propertyValue = Boolean.FALSE;
          if (typeSeen)
            error(ERROR_incompatibleArguments);
          typeSeen = true;
          break;
        }
        if ("edges".equalsIgnoreCase(str) || "noedges".equalsIgnoreCase(str)
            || "frontedges".equalsIgnoreCase(str)) {
          if (edgeParameterSeen)
            error(ERROR_incompatibleArguments);
          propertyName = str;
          edgeParameterSeen = true;
          break;
        }
        if (!needsGenerating)
          error(ERROR_insufficientArguments);
        if ("to".equalsIgnoreCase(str)) {
          if (nAtomSets > 1)
            error(ERROR_invalidParameterOrder);
          if (getToken(i + 1).tok == Token.bitset) {
            propertyName = "toBitSet";
            propertyValue = getToken(++i).value;
            needsGenerating = true;
            break;
          }
          setPropertyName = "to";
          continue;
        }
        if ("faceCenterOffset".equalsIgnoreCase(str)) {
          decimalPropertyName = "faceCenterOffset";
          isDesignParameter = true;
          continue;
        }
        if ("distanceFactor".equalsIgnoreCase(str)) {
          decimalPropertyName = "distanceFactor";
          isDesignParameter = true;
          continue;
        }
        error(ERROR_invalidArgument);
      case Token.integer:
        if (nAtomSets > 0 && !isDesignParameter)
          error(ERROR_invalidParameterOrder);
        // no reason not to allow integers when explicit
        if (decimalPropertyName == "radius_") {
          propertyName = "nVertices";
          propertyValue = new Integer(intParameter(i));
          needsGenerating = true;
          break;
        }
      case Token.decimal:
        if (nAtomSets > 0 && !isDesignParameter)
          error(ERROR_invalidParameterOrder);
        propertyName = (decimalPropertyName == "radius_" ? "radius"
            : decimalPropertyName);
        propertyValue = new Float(floatParameter(i));
        decimalPropertyName = "radius_";
        isDesignParameter = false;
        needsGenerating = true;
        break;
      case Token.delete:
      case Token.on:
      case Token.off:
        if (i + 1 != statementLength || needsGenerating || nAtomSets > 1
            || nAtomSets == 0 && setPropertyName == "to")
          error(ERROR_incompatibleArguments);
        propertyName = parameterAsString(i);
        onOffDelete = true;
        break;
      case Token.bitset:
      case Token.expressionBegin:
        if (typeSeen)
          error(ERROR_invalidParameterOrder);
        if (++nAtomSets > 2)
          error(ERROR_badArgumentCount);
        if (setPropertyName == "to")
          needsGenerating = true;
        propertyName = setPropertyName;
        setPropertyName = "to";
        propertyValue = expression(i);
        i = iToken;
        break;
      default:
        error(ERROR_invalidArgument);
      }
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, propertyName,
          propertyValue);
      if (onOffDelete)
        return;
    }
    if (!needsGenerating && !typeSeen && !edgeParameterSeen)
      error(ERROR_insufficientArguments);
    if (needsGenerating)
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "generate", null);
    if (color != Integer.MIN_VALUE)
      setShapeProperty(JmolConstants.SHAPE_POLYHEDRA, "colorThis", new Integer(
          color));
    if (isTranslucent)
      setShapeTranslucency(JmolConstants.SHAPE_POLYHEDRA, "", "translucent",
          translucentLevel, null);
  }

  private void lcaoCartoon() throws ScriptException {
    viewer.loadShape(JmolConstants.SHAPE_LCAOCARTOON);
    if (tokAt(1) == Token.list
        && listIsosurface(JmolConstants.SHAPE_LCAOCARTOON))
      return;
    setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "init", null);
    if (statementLength == 1) {
      setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "lcaoID", null);
      return;
    }
    boolean idSeen = false;
    String translucency = null;
    for (int i = 1; i < statementLength; i++) {
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(i).tok) {
      case Token.center:
        // serialized lcaoCartoon in isosurface format
        isosurface(JmolConstants.SHAPE_LCAOCARTOON);
        return;
      case Token.rotate:
        Vector3f rotAxis = new Vector3f();
        switch (getToken(++i).tok) {
        case Token.identifier:
          String str = parameterAsString(i);
          float radians = floatParameter(++i)
              * TransformManager.radiansPerDegree;
          if (str.equalsIgnoreCase("x")) {
            rotAxis.set(radians, 0, 0);
            break;
          }
          if (str.equalsIgnoreCase("y")) {
            rotAxis.set(0, radians, 0);
            break;
          }
          if (str.equalsIgnoreCase("z")) {
            rotAxis.set(0, 0, radians);
            break;
          }
          error(ERROR_invalidArgument);
        default:
          error(ERROR_invalidArgument);
        }
        propertyName = "rotationAxis";
        propertyValue = rotAxis;
        break;
      case Token.on:
      case Token.display:
      case Token.displayed:
        propertyName = "on";
        break;
      case Token.off:
      case Token.hide:
      case Token.hidden:
        propertyName = "off";
        break;
      case Token.delete:
        propertyName = "delete";
        break;
      case Token.integer:
      case Token.decimal:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.bitset:
      case Token.expressionBegin:
        propertyName = "select";
        propertyValue = expression(i);
        i = iToken;
        break;
      case Token.color:
        translucency = setColorOptions(i + 1, JmolConstants.SHAPE_LCAOCARTOON,
            -2);
        if (translucency != null)
          setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "settranslucency",
              translucency);
        i = iToken;
        idSeen = true;
        continue;
      case Token.translucent:
      case Token.opaque:
        setMeshDisplayProperty(JmolConstants.SHAPE_LCAOCARTOON, i, theTok);
        i = iToken;
        idSeen = true;
        continue;
      case Token.string:
        propertyValue = stringParameter(i);
        propertyName = "create";
        if (optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
          i++;
          propertyName = "molecular";
        }
        break;
      case Token.select:
        if (tokAt(i + 1) == Token.bitset
            || tokAt(i + 1) == Token.expressionBegin) {
          propertyName = "select";
          propertyValue = expression(i + 1);
          i = iToken;
        } else {
          propertyName = "selectType";
          propertyValue = parameterAsString(++i);
        }
        break;
      case Token.scale:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.identifier:
        String str = parameterAsString(i);
        if (str.equalsIgnoreCase("ID")) {
          str = getShapeNameParameter(++i);
          i = iToken;
        } else if (str.equalsIgnoreCase("MOLECULAR")) {
          propertyName = "molecular";
          break;
        } else if (str.equalsIgnoreCase("LONEPAIR")) {
          propertyName = "lonePair";
          break;
        } else if (str.equalsIgnoreCase("RADICAL")) {
          propertyName = "radical";
          break;
        } else if (str.equalsIgnoreCase("CREATE")) {
          propertyValue = parameterAsString(++i);
          propertyName = "create";
          if (optParameterAsString(i + 1).equalsIgnoreCase("molecular")) {
            i++;
            propertyName = "molecular";
          }
          break;
        }
        propertyValue = str;
        // fall through for identifiers
      case Token.all:
        if (idSeen)
          error(ERROR_invalidArgument);
        propertyName = "lcaoID";
        break;
      }
      if (theTok != Token.delete)
        idSeen = true;
      if (propertyName == null)
        error(ERROR_invalidArgument);
      setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, propertyName,
          propertyValue);
    }
    setShapeProperty(JmolConstants.SHAPE_LCAOCARTOON, "clear", null);
  }

  private boolean mo(boolean isInitOnly) throws ScriptException {
    int offset = Integer.MAX_VALUE;
    BitSet bsModels = viewer.getVisibleFramesBitSet();
    int modelCount = viewer.getModelCount();
    for (int modelIndex = 0; modelIndex < modelCount; modelIndex++) {
      if (!bsModels.get(modelIndex))
        continue;
      viewer.loadShape(JmolConstants.SHAPE_MO);
      if (tokAt(1) == Token.list && listIsosurface(JmolConstants.SHAPE_MO))
        return true;
      setShapeProperty(JmolConstants.SHAPE_MO, "init", new Integer(modelIndex));
      String title = null;
      int moNumber = ((Integer) viewer.getShapeProperty(JmolConstants.SHAPE_MO,
          "moNumber")).intValue();
      if (isInitOnly)
        return true;// (moNumber != 0);
      if (moNumber == 0)
        moNumber = Integer.MAX_VALUE;
      String str;
      String propertyName = null;
      Object propertyValue = null;
      switch (getToken(1).tok) {
      case Token.integer:
        moNumber = intParameter(1);
        break;
      case Token.next:
        moNumber = Token.next;
        break;
      case Token.prev:
        moNumber = Token.prev;
        break;
      case Token.color:
        setColorOptions(2, JmolConstants.SHAPE_MO, 2);
        break;
      case Token.plane:
        // plane {X, Y, Z, W}
        propertyName = "plane";
        propertyValue = planeParameter(2);
        break;
      case Token.scale:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(2));
        break;
      case Token.identifier:
        str = parameterAsString(1);
        if ((offset = moOffset(1)) != Integer.MAX_VALUE) {
          moNumber = 0;
          break;
        }
        if (str.equalsIgnoreCase("CUTOFF")) {
          if (tokAt(2) == Token.plus) {
            propertyName = "cutoffPositive";
            propertyValue = new Float(floatParameter(3));
          } else {
            propertyName = "cutoff";
            propertyValue = new Float(floatParameter(2));
          }
          break;
        }
        if (str.equalsIgnoreCase("RESOLUTION")
            || str.equalsIgnoreCase("POINTSPERANGSTROM")) {
          propertyName = "resolution";
          propertyValue = new Float(floatParameter(2));
          break;
        }
        if (str.equalsIgnoreCase("SQUARED")) {
          propertyName = "squareData";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("TITLEFORMAT")) {
          if (2 < statementLength && tokAt(2) == Token.string) {
            propertyName = "titleFormat";
            propertyValue = parameterAsString(2);
          }
          break;
        }
        if (str.equalsIgnoreCase("DEBUG")) {
          propertyName = "debug";
          break;
        }
        if (str.equalsIgnoreCase("noplane")) {
          propertyName = "plane";
          break;
        }
        error(ERROR_invalidArgument);
      default:
        if (!setMeshDisplayProperty(JmolConstants.SHAPE_MO, 1, theTok))
          error(ERROR_invalidArgument);
        return true;
      }
      if (propertyName != null)
        setShapeProperty(JmolConstants.SHAPE_MO, propertyName, propertyValue);
      if (moNumber != Integer.MAX_VALUE) {
        if (tokAt(2) == Token.string)
          title = parameterAsString(2);
        if (!isSyntaxCheck)
          viewer.setCursor(Viewer.CURSOR_WAIT);
        setMoData(JmolConstants.SHAPE_MO, moNumber, offset, modelIndex, title);
        setShapeProperty(JmolConstants.SHAPE_MO, "finalize", null);
      }

    }
    return true;
  }

  private String setColorOptions(int index, int iShape, int nAllowed)
      throws ScriptException {
    getToken(index);
    String translucency = "opaque";
    if (theTok == Token.translucent) {
      translucency = "translucent";
      if (nAllowed < 0) {
        float value = (isFloatParameter(index + 1) ? floatParameter(++index)
            : Float.MAX_VALUE);
        setShapeTranslucency(iShape, null, "translucent", value, null);
      } else {
        setMeshDisplayProperty(iShape, index, theTok);
      }
    } else if (theTok == Token.opaque) {
      if (nAllowed >= 0)
        setMeshDisplayProperty(iShape, index, theTok);
    } else {
      iToken--;
    }
    nAllowed = Math.abs(nAllowed);
    for (int i = 0; i < nAllowed; i++) {
      if (isColorParam(iToken + 1)) {
        setShapeProperty(iShape, "colorRGB",
            new Integer(getArgbParam(++iToken)));
      } else if (iToken < index) {
        error(ERROR_invalidArgument);
      } else {
        break;
      }
    }
    return translucency;
  }

  private int moOffset(int index) throws ScriptException {
    String str = parameterAsString(index++);
    boolean isHomo = false;
    int offset = Integer.MAX_VALUE;
    if ((isHomo = str.equalsIgnoreCase("HOMO")) || str.equalsIgnoreCase("LUMO")) {
      offset = (isHomo ? 0 : 1);
      if (tokAt(index) == Token.integer && intParameter(index) < 0)
        offset += intParameter(index);
      else if (tokAt(index) == Token.plus)
        offset += intParameter(index + 1);
      else if (tokAt(index) == Token.minus)
        offset -= intParameter(index + 1);
    }
    return offset;
  }

  private void setMoData(int shape, int moNumber, int offset, int modelIndex,
                         String title) throws ScriptException {
    if (isSyntaxCheck)
      return;
    if (modelIndex < 0) {
      modelIndex = viewer.getDisplayModelIndex();
      if (modelIndex < 0)
        error(ERROR_multipleModelsNotOK, "MO isosurfaces");
    }
    Hashtable moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex,
        "jmolSurfaceInfo");
    int firstMoNumber = moNumber;
    if (moData != null && ((String) moData.get("surfaceDataType")).equals("mo")) {
      // viewer.loadShape(shape);
      // setShapeProperty(shape, "init", new Integer(modelIndex));
    } else {
      moData = (Hashtable) viewer.getModelAuxiliaryInfo(modelIndex, "moData");
      if (moData == null)
        error(ERROR_moModelError);
      int lastMoNumber = (moData.containsKey("lastMoNumber") ? ((Integer) moData
          .get("lastMoNumber")).intValue()
          : 0);
      if (moNumber == Token.prev)
        moNumber = lastMoNumber - 1;
      else if (moNumber == Token.next)
        moNumber = lastMoNumber + 1;
      Vector mos = (Vector) (moData.get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0)
        error(ERROR_moCoefficients);
      if (nOrb == 1 && moNumber > 1)
        error(ERROR_moOnlyOne);
      if (offset != Integer.MAX_VALUE) {
        // 0: HOMO;
        if (moData.containsKey("HOMO")) {
          moNumber = ((Integer) moData.get("HOMO")).intValue() + offset;
        } else {
          for (int i = 0; i < nOrb; i++) {
            Hashtable mo = (Hashtable) mos.get(i);
            if (!mo.containsKey("occupancy"))
              error(ERROR_moOccupancy);
            if (((Float) mo.get("occupancy")).floatValue() == 0) {
              moNumber = i + offset;
              break;
            }
          }
        }
        Logger.info("MO " + moNumber);
      }
      if (moNumber < 1 || moNumber > nOrb)
        error(ERROR_moIndex, "" + nOrb);
    }
    moData.put("lastMoNumber", new Integer(moNumber));
    setShapeProperty(shape, "moData", moData);
    if (title != null)
      setShapeProperty(shape, "title", title);
    if (firstMoNumber < 0)
      setShapeProperty(shape, "charges", viewer.getAtomicCharges());
    setShapeProperty(shape, "molecularOrbital", new Integer(
        firstMoNumber < 0 ? -moNumber : moNumber));
    setShapeProperty(shape, "clear", null);
  }

  private String initIsosurface(int iShape) throws ScriptException {

    // handle isosurface/mo/pmesh delete and id delete here

    setShapeProperty(iShape, "init", fullCommand);
    iToken = 0;
    if (tokAt(1) == Token.delete || tokAt(2) == Token.delete
        && tokAt(++iToken) == Token.all) {
      setShapeProperty(iShape, "delete", null);
      iToken += 2;
      if (statementLength > iToken) {
        setShapeProperty(iShape, "init", fullCommand);
        setShapeProperty(iShape, "thisID", JmolConstants.PREVIOUS_MESH_ID);
      }
      return null;
    }
    iToken = 1;
    if (!setMeshDisplayProperty(iShape, 0, tokAt(1))) {
      setShapeProperty(iShape, "thisID", JmolConstants.PREVIOUS_MESH_ID);
      if (iShape != JmolConstants.SHAPE_DRAW)
        setShapeProperty(iShape, "title", new String[] { thisCommand });
      if (tokAt(2) == Token.times
          && !parameterAsString(1).equalsIgnoreCase("id")) {
        String id = setShapeId(iShape, 1, false);
        iToken++;
        return id;
      }
    }
    return null;
  }

  private String getNextComment() {
    String nextCommand = getCommand(pc + 1, false, true);
    return (nextCommand.startsWith("#") ? nextCommand : "");
  }

  private boolean listIsosurface(int iShape) throws ScriptException {
    if (getToken(1).value instanceof String[]) // not just the word "list"
      return false;
    checkLength(2);
    if (!isSyntaxCheck)
      showString((String) viewer.getShapeProperty(iShape, "list"));
    return true;
  }

  private void isosurface(int iShape) throws ScriptException {
    viewer.loadShape(iShape);
    if (tokAt(1) == Token.list && listIsosurface(iShape))
      return;
    int colorRangeStage = 0;
    int signPt = 0;
    boolean isIsosurface = (iShape == JmolConstants.SHAPE_ISOSURFACE);
    boolean isPmesh = (iShape == JmolConstants.SHAPE_PMESH);
    boolean surfaceObjectSeen = false;
    boolean planeSeen = false;
    boolean doCalcArea = false;
    boolean doCalcVolume = false;
    boolean isCavity = false;
    boolean isFxy = false;
    float[] nlmZ = new float[5];
    float[] data = null;
    int thisSetNumber = 0;
    int nFiles = 0;
    BitSet bs;
    String str = null;
    int modelIndex = (isSyntaxCheck ? 0 : viewer.getDisplayModelIndex());
    if (!isSyntaxCheck)
      viewer.setCursor(Viewer.CURSOR_WAIT);
    boolean idSeen = (initIsosurface(iShape) != null);
    boolean isWild = (idSeen && viewer.getShapeProperty(iShape, "ID") == null);
    String translucency = null;
    String colorScheme = null;
    if (isPmesh)
      setShapeProperty(iShape, "fileType", "Pmesh");
    for (int i = iToken; i < statementLength; ++i) {
      if (isColorParam(i)) {
        if (i != signPt)
          error(ERROR_invalidParameterOrder);
        setShapeProperty(iShape, "colorRGB", new Integer(getArgbParam(i)));
        i = iToken;
        signPt = i + 1;
        idSeen = true;
        continue;
      }
      String propertyName = null;
      Object propertyValue = null;
      int tok = getToken(i).tok;
      if (tok == Token.identifier
          && (str = parameterAsString(i)).equalsIgnoreCase("inline"))
        tok = Token.string;
      switch (tok) {
      case Token.pmesh:
        setShapeProperty(iShape, "fileType", "Pmesh");
        continue;
      case Token.within:
        float distance = floatParameter(++i);
        propertyValue = centerParameter(++i);
        i = iToken;
        propertyName = "withinPoint";
        setShapeProperty(iShape, "withinDistance", new Float(distance));
        break;
      case Token.property:
        setShapeProperty(iShape, "propertySmoothing", viewer
            .getIsosurfacePropertySmoothing() ? Boolean.TRUE : Boolean.FALSE);
        str = parameterAsString(i);
        propertyName = "property";
        if (!isCavity && str.toLowerCase().indexOf("property_") == 0) {
          data = new float[viewer.getAtomCount()];
          if (isSyntaxCheck)
            continue;
          data = viewer.getDataFloat(str);
          if (data == null)
            error(ERROR_invalidArgument);
          propertyValue = data;
          break;
        }
        int tokProperty = getToken(++i).tok;
        int atomCount = viewer.getAtomCount();
        data = (isCavity ? new float[0] : new float[atomCount]);
        if (isCavity)// not implemented: && tokProperty !=
          // Token.surfacedistance)
          error(ERROR_invalidArgument);
        if (!isSyntaxCheck && !isCavity) {
          Atom[] atoms = viewer.getModelSet().atoms;
          viewer.autoCalculate(tokProperty);
          for (int iAtom = atomCount; --iAtom >= 0;) {
            data[iAtom] = Atom.atomPropertyFloat(atoms[iAtom], tokProperty);
          }
        }
        if (tokProperty == Token.color)
          colorScheme = "colorRGB";
        propertyValue = data;
        break;
      case Token.model:
        if (surfaceObjectSeen)
          error(ERROR_invalidArgument);
        modelIndex = modelNumberParameter(++i);
        if (modelIndex < 0) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        propertyName = "modelIndex";
        propertyValue = new Integer(modelIndex);
        break;
      case Token.select:
        propertyName = "select";
        propertyValue = expression(++i);
        i = iToken;
        break;
      case Token.set:
        thisSetNumber = intParameter(++i);
        break;
      case Token.center:
        propertyName = "center";
        propertyValue = centerParameter(++i);
        i = iToken;
        break;
      case Token.color:
        /*
         * "color" now is just used as an equivalent to "sign" and as an
         * introduction to "absolute" any other use is superfluous; it has been
         * replaced with MAP for indicating "use the current surface" because
         * the term COLOR is too general.
         */
        colorRangeStage = 0;
        if (getToken(i + 1).tok == Token.string)
          colorScheme = parameterAsString(++i);
        if ((theTok = tokAt(i + 1)) == Token.translucent
            || tokAt(i + 1) == Token.opaque) {
          translucency = setColorOptions(i + 1, JmolConstants.SHAPE_ISOSURFACE,
              -2);
          i = iToken;
        }
        switch (tokAt(i + 1)) {
        case Token.absolute:
        case Token.range:
          getToken(++i);
          colorRangeStage = 1;
          propertyName = "rangeAll";
          if (tokAt(i + 1) == Token.all)
            getToken(++i);
          break;
        default:
          signPt = i + 1;
          continue;
        }
        break;
      case Token.file:
        continue;
      case Token.plus:
        if (colorRangeStage == 0) {
          propertyName = "cutoffPositive";
          propertyValue = new Float(floatParameter(++i));
        }
        break;
      case Token.decimal:
      case Token.integer:
        // default is "cutoff"
        propertyName = (colorRangeStage == 1 ? "red"
            : colorRangeStage == 2 ? "blue" : "cutoff");
        propertyValue = new Float(floatParameter(i));
        if (colorRangeStage > 0)
          ++colorRangeStage;
        break;
      case Token.ionic:
        propertyName = "ionicRadius";
        propertyValue = new Float(radiusParameter(++i, 0));
        i = iToken;
        break;
      case Token.vanderwaals:
        propertyName = "vdwRadius";
        propertyValue = new Float(radiusParameter(++i, 0));
        i = iToken;
        break;
      case Token.plane:
        // plane {X, Y, Z, W}
        planeSeen = true;
        propertyName = "plane";
        propertyValue = planeParameter(++i);
        i = iToken;
        break;
      case Token.scale:
        propertyName = "scale";
        propertyValue = new Float(floatParameter(++i));
        break;
      case Token.all:
        if (idSeen)
          error(ERROR_invalidArgument);
        propertyName = "thisID";
        break;
      case Token.ellipsoid:
        // ellipsoid {xc yc zc f} where a = b and f = a/c
        // OR ellipsoid {u11 u22 u33 u12 u13 u23}
        surfaceObjectSeen = true;
        ++i;
        try {
          propertyValue = getPoint4f(i);
          propertyName = "ellipsoid";
          i = iToken;
          break;
        } catch (ScriptException e) {
        }
        try {
          float[] fparams = new float[6];
          i = floatParameterSet(i, fparams);
          propertyValue = fparams;
          propertyName = "ellipsoid";
          break;
        } catch (ScriptException e) {
        }
        bs = expression(i);
        int iAtom = BitSetUtil.firstSetBit(bs);
        Atom[] atoms = viewer.getModelSet().atoms;
        if (iAtom >= 0)
          propertyValue = atoms[iAtom].getEllipsoid();
        if (propertyValue == null)
          return;
        i = iToken;
        propertyName = "ellipsoid";
        if (!isSyntaxCheck)
          setShapeProperty(iShape, "center", viewer.getAtomPoint3f(iAtom));
        break;
      case Token.hkl:
        // miller indices hkl
        planeSeen = true;
        propertyName = "plane";
        propertyValue = hklParameter(++i);
        i = iToken;
        break;
      case Token.lcaocartoon:
        surfaceObjectSeen = true;
        String lcaoType = parameterAsString(++i);
        setShapeProperty(iShape, "lcaoType", lcaoType);
        switch (getToken(++i).tok) {
        case Token.bitset:
        case Token.expressionBegin:
          propertyName = "lcaoCartoon";
          bs = expression(i);
          i = iToken;
          int atomIndex = BitSetUtil.firstSetBit(bs);
          modelIndex = 0;
          Point3f pt;
          if (atomIndex < 0) {
            if (!isSyntaxCheck)
              error(ERROR_expressionExpected);
            pt = new Point3f();
          } else {
            modelIndex = viewer.getAtomModelIndex(atomIndex);
            pt = viewer.getAtomPoint3f(atomIndex);
          }
          setShapeProperty(iShape, "modelIndex", new Integer(modelIndex));
          Vector3f[] axes = { new Vector3f(), new Vector3f(), new Vector3f(pt),
              new Vector3f() };
          if (!isSyntaxCheck)
            viewer.getHybridizationAndAxes(atomIndex, axes[0], axes[1],
                lcaoType, false);
          propertyValue = axes;
          break;
        default:
          error(ERROR_expressionExpected);
        }
        break;
      case Token.mo:
        // mo 1-based-index
        if (++i == statementLength)
          error(ERROR_badArgumentCount);
        int moNumber = Integer.MAX_VALUE;
        int offset = Integer.MAX_VALUE;
        if (tokAt(i) == Token.integer) {
          moNumber = intParameter(i);
        } else if ((offset = moOffset(i)) != Integer.MAX_VALUE) {
          moNumber = 0;
          i = iToken;
        }
        setMoData(iShape, moNumber, offset, modelIndex, null);
        surfaceObjectSeen = true;
        continue;
      case Token.mep:
        float[] partialCharges = null;
        try {
          partialCharges = viewer.getPartialCharges();
        } catch (Exception e) {
        }
        if (!isSyntaxCheck && partialCharges == null)
          error(ERROR_noPartialCharges);
        surfaceObjectSeen = true;
        propertyName = "mep";
        propertyValue = partialCharges;
        break;
      case Token.sasurface:
      case Token.solvent:
        surfaceObjectSeen = true;
        setShapeProperty(iShape, "bsSolvent", lookupIdentifierValue("solvent"));
        propertyName = (theTok == Token.sasurface ? "sasurface" : "solvent");
        float radius = (isFloatParameter(i + 1) ? floatParameter(++i) : viewer
            .getSolventProbeRadius());
        propertyValue = new Float(radius);
        break;
      case Token.identifier:
        if (str.equalsIgnoreCase("ADDHYDROGENS")) {
          propertyName = "addHydrogens";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("ANGSTROMS")) {
          propertyName = "angstroms";
          break;
        }
        if (str.equalsIgnoreCase("ANISOTROPY")) {
          propertyName = "anisotropy";
          propertyValue = getPoint3f(++i, false);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("AREA")) {
          doCalcArea = !isSyntaxCheck;
          break;
        }
        if (str.equalsIgnoreCase("VOLUME")) {
          doCalcVolume = !isSyntaxCheck;
          break;
        }
        if (str.equalsIgnoreCase("ATOMICORBITAL")
            || str.equalsIgnoreCase("ORBITAL")) {
          surfaceObjectSeen = true;
          nlmZ[0] = intParameter(++i);
          nlmZ[1] = intParameter(++i);
          nlmZ[2] = intParameter(++i);
          nlmZ[3] = (isFloatParameter(i + 1) ? floatParameter(++i) : 6f);
          propertyName = "hydrogenOrbital";
          propertyValue = nlmZ;
          break;
        }
        if (str.equalsIgnoreCase("BINARY")) {
          // if (!isPmesh)
          // error(ERROR_invalidArgument);
          // for PMESH, specifically
          // ignore for now
          continue;
        }
        if (str.equalsIgnoreCase("BLOCKDATA")) {
          propertyName = "blockData";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("CAP")) {
          propertyName = "cappingPlane";
          propertyValue = planeParameter(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("CAVITY")) {
          if (!isIsosurface)
            error(ERROR_invalidArgument);
          isCavity = true;
          if (isSyntaxCheck)
            continue;
          float cavityRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
              : 1.2f);
          float envelopeRadius = (isFloatParameter(i + 1) ? floatParameter(++i)
              : 10f);
          if (envelopeRadius > 10f)
            integerOutOfRange(0, 10);
          setShapeProperty(iShape, "envelopeRadius", new Float(envelopeRadius));
          setShapeProperty(iShape, "cavityRadius", new Float(cavityRadius));
          propertyName = "cavity";
          break;
        }
        if (str.equalsIgnoreCase("COLORSCHEME")) {
          colorScheme = parameterAsString(++i);
          break;
        }
        if (str.equalsIgnoreCase("CONTOUR")) {
          propertyName = "contour";
          propertyValue = new Integer(
              tokAt(i + 1) == Token.integer ? intParameter(++i) : 0);
          break;
        }
        if (str.equalsIgnoreCase("CUTOFF")) {
          if (++i < statementLength && getToken(i).tok == Token.plus) {
            propertyName = "cutoffPositive";
            propertyValue = new Float(floatParameter(++i));
          } else {
            propertyName = "cutoff";
            propertyValue = new Float(floatParameter(i));
          }
          break;
        }
        if (str.equalsIgnoreCase("DOWNSAMPLE")) {
          propertyName = "downsample";
          propertyValue = new Integer(intParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("ECCENTRICITY")) {
          propertyName = "eccentricity";
          propertyValue = getPoint4f(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("ED")) {
          setMoData(iShape, -1, 0, modelIndex, null);
          surfaceObjectSeen = true;
          continue;
        }
        if (str.equalsIgnoreCase("DEBUG") || str.equalsIgnoreCase("NODEBUG")) {
          propertyName = "debug";
          propertyValue = (str.equalsIgnoreCase("DEBUG") ? Boolean.TRUE
              : Boolean.FALSE);
          break;
        }
        if (str.equalsIgnoreCase("FIXED")) {
          propertyName = "fixed";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("FUNCTIONXY")) {
          // isosurface functionXY "functionName"|"data2d_xxxxx"
          // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
          Vector v = new Vector();
          if (getToken(++i).tok != Token.string)
            error(ERROR_what,
                "functionXY must be followed by a function name in quotes.");
          String fName = parameterAsString(i++);
          // override of function or data name when saved as a state
          String dataName = extractCommandOption("# DATA" + (isFxy ? "2" : ""));
          if (dataName != null)
            fName = dataName;
          boolean isXYZ = (fName.indexOf("data2d_xyz") == 0);
          v.addElement(fName); // (0) = name
          v.addElement(getPoint3f(i, false)); // (1) = {origin}
          Point4f pt;
          int nX, nY;
          int ptX = ++iToken;
          v.addElement(pt = getPoint4f(ptX)); // (2) = {ni ix iy iz}
          nX = (int) pt.x;
          int ptY = ++iToken;
          v.addElement(pt = getPoint4f(ptY)); // (3) = {nj jx jy jz}
          nY = (int) pt.x;
          v.addElement(getPoint4f(++iToken)); // (4) = {nk kx ky kz}
          if (nX == 0 || nY == 0)
            error(ERROR_invalidArgument);
          if (!isSyntaxCheck) {
            float[][] fdata = (isXYZ ? viewer.getDataFloat2D(fName) : viewer
                .functionXY(fName, nX, nY));
            if (isXYZ) {
              nX = (fdata == null ? 0 : fdata.length);
              nY = 3;
            } else {
              nX = Math.abs(nX);
              nY = Math.abs(nY);
            }
            if (fdata == null) {
              iToken = ptX;
              error(ERROR_what, "fdata is null.");
            }
            if (fdata.length != nX && !isXYZ) {
              iToken = ptX;
              error(ERROR_what, "fdata length is not correct: " + fdata.length
                  + " " + nX + ".");
            }
            for (int j = 0; j < nX; j++) {
              if (fdata[j] == null) {
                iToken = ptY;
                error(ERROR_what, "fdata[" + j + "] is null.");
              }
              if (fdata[j].length != nY) {
                iToken = ptY;
                error(ERROR_what, "fdata[" + j + "] is not the right length: "
                    + fdata[j].length + " " + nY + ".");
              }
            }
            v.addElement(fdata); // (5) = float[][] data
          }
          i = iToken;
          propertyName = "functionXY";
          propertyValue = v;
          isFxy = surfaceObjectSeen = true;
          break;
        }
        if (str.equalsIgnoreCase("FUNCTIONXYZ")) {
          // isosurface functionXYZ "functionName"
          // {origin} {ni ix iy iz} {nj jx jy jz} {nk kx ky kz}
          Vector v = new Vector();
          if (getToken(++i).tok != Token.string)
            error(ERROR_what,
                "functionXYZ must be followed by a function name in quotes.");
          String fName = parameterAsString(i++);
          // override of function or data name when saved as a state
          String dataName = extractCommandOption("# DATA");
          if (dataName != null)
            fName = dataName;
          v.addElement(fName); // (0) = name
          v.addElement(getPoint3f(i, false)); // (1) = {origin}
          Point4f pt;
          int nX, nY, nZ;
          int ptX = ++iToken;
          v.addElement(pt = getPoint4f(ptX)); // (2) = {ni ix iy iz}
          nX = (int) pt.x;
          int ptY = ++iToken;
          v.addElement(pt = getPoint4f(ptY)); // (3) = {nj jx jy jz}
          nY = (int) pt.x;
          v.addElement(pt = getPoint4f(++iToken)); // (4) = {nk kx ky kz}
          nZ = (int) pt.x;
          if (nX == 0 || nY == 0)
            error(ERROR_invalidArgument);
          if (!isSyntaxCheck) {
            float[][][] xyzdata = viewer.functionXYZ(fName, nX, nY, nZ);
            nX = Math.abs(nX);
            nY = Math.abs(nY);
            if (xyzdata == null) {
              iToken = ptX;
              error(ERROR_what, "xyzdata is null.");
            }
            v.addElement(xyzdata); // (5) = float[][][] data
          }
          i = iToken;
          propertyName = "functionXYZ";
          propertyValue = v;
          isFxy = surfaceObjectSeen = true;
          break;
        }
        if (str.equalsIgnoreCase("GRIDPOINTS")) {
          propertyName = "gridPoints";
          break;
        }
        if (str.equalsIgnoreCase("ID")) {
          setShapeId(iShape, ++i, idSeen);
          isWild = (viewer.getShapeProperty(iShape, "ID") == null);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("IGNORE")) {
          propertyName = "ignore";
          propertyValue = expression(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("INSIDEOUT")) {
          propertyName = "insideOut";
          break;
        }
        if (str.equalsIgnoreCase("INTERIOR")) {
          propertyName = "pocket";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (str.equalsIgnoreCase("LINK")) { // for state of lcaoCartoon
          propertyName = "link";
          break;
        }
        if (str.equalsIgnoreCase("LOBE") 
            || str.equalsIgnoreCase("LP") 
            || str.equalsIgnoreCase("RAD")) {
          // lobe {eccentricity}
          surfaceObjectSeen = true;
          propertyName = str.toLowerCase();
          propertyValue = getPoint4f(++i);
          i = iToken;
          break;
        }
        if (str.equalsIgnoreCase("MAP")) { // "use current"
          surfaceObjectSeen = !isCavity;
          propertyName = "map";
          break;
        }
        if (str.equalsIgnoreCase("MAXSET")) {
          propertyName = "maxset";
          propertyValue = new Integer(intParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("MINSET")) {
          propertyName = "minset";
          propertyValue = new Integer(intParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("MODELBASED")) {
          propertyName = "fixed";
          propertyValue = Boolean.FALSE;
          break;
        }
        if (str.equalsIgnoreCase("MOLECULAR")) {
          surfaceObjectSeen = true;
          propertyName = "molecular";
          propertyValue = new Float(1.4);
          break;
        }
        if (str.equalsIgnoreCase("OBJ")) {
          setShapeProperty(iShape, "fileType", "Obj");
          continue;
        }
        if (str.equalsIgnoreCase("PHASE")) {
          if (surfaceObjectSeen)
            error(ERROR_invalidArgument);
          propertyName = "phase";
          propertyValue = (tokAt(i + 1) == Token.string ? stringParameter(++i)
              : "_orb");
          break;
        }
        if (str.equalsIgnoreCase("POCKET")) {
          propertyName = "pocket";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("REMAPPABLE")) { // testing only
          propertyName = "remappable";
          break;
        }
        if (str.equalsIgnoreCase("RESOLUTION")
            || str.equalsIgnoreCase("POINTSPERANGSTROM")) {
          propertyName = "resolution";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("REVERSECOLOR")) {
          propertyName = "reverseColor";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("SIGN")) {
          signPt = i + 1;
          propertyName = "sign";
          propertyValue = Boolean.TRUE;
          colorRangeStage = 1;
          break;
        }
        if (str.equalsIgnoreCase("SPHERE")) {
          // sphere [radius]
          surfaceObjectSeen = true;
          propertyName = "sphere";
          propertyValue = new Float(floatParameter(++i));
          break;
        }
        if (str.equalsIgnoreCase("SQUARED")) {
          propertyName = "squareData";
          propertyValue = Boolean.TRUE;
          break;
        }
        if (str.equalsIgnoreCase("VARIABLE")) {
          propertyName = "property";
          data = new float[viewer.getAtomCount()];
          if (!isSyntaxCheck) {
            Parser.parseFloatArray(""
                + getParameter(parameterAsString(++i), false), null, data);
          }
          propertyValue = data;
          break;
        }
        setShapeId(iShape, i, idSeen);
        i = iToken;
        break;
      case Token.string:
        propertyName = surfaceObjectSeen || planeSeen ? "mapColor" : "readFile";
        /*
         * a file name, optionally followed by an integer file index. OR empty.
         * In that case, if the model auxiliary info has the data stored in it,
         * we use that. There are two possible structures:
         * 
         * jmolSurfaceInfo jmolMappedDataInfo
         * 
         * Both can be present, but if jmolMappedDataInfo is missing, then
         * jmolSurfaceInfo is used by default.
         */
        String filename = parameterAsString(i);
        if (filename.equals("TESTDATA") && Viewer.testData != null) {
          propertyValue = Viewer.testData;
          break;
        }
        if (filename.equals("TESTDATA2") && Viewer.testData2 != null) {
          propertyValue = Viewer.testData2;
          break;
        }
        if (filename.length() == 0) {
          if (surfaceObjectSeen || planeSeen)
            propertyValue = viewer.getModelAuxiliaryInfo(modelIndex,
                "jmolMappedDataInfo");
          if (propertyValue == null)
            propertyValue = viewer.getModelAuxiliaryInfo(modelIndex,
                "jmolSurfaceInfo");
          surfaceObjectSeen = true;
          if (propertyValue != null)
            break;
          filename = getFullPathName();
        }
        surfaceObjectSeen = true;
        if (tokAt(i + 1) == Token.integer)
          setShapeProperty(iShape, "fileIndex", new Integer(intParameter(++i)));
        String[] fullPathNameReturn = new String[1];
        Object t;
        if (filename.equalsIgnoreCase("INLINE")) {
          // inline PMESH data
          if (tokAt(i + 1) != Token.string)
            error(ERROR_stringExpected);
          setShapeProperty(iShape, "fileType", "Pmesh");
          String sdata = parameterAsString(++i);
          sdata = TextFormat.replaceAllCharacters(sdata, "{,}|", ' ');
          if (logMessages)
            Logger.debug("pmesh inline data:\n" + sdata);
          t = (isSyntaxCheck ? null : FileManager
              .getBufferedReaderForString(sdata));
        } else {
          if (thisCommand.indexOf("# FILE" + nFiles + "=") >= 0)
            filename = extractCommandOption("# FILE" + nFiles);
          t = (isSyntaxCheck ? null : viewer
              .getBufferedReaderOrErrorMessageFromName(filename,
                  fullPathNameReturn, false));
          if (t instanceof String)
            error(ERROR_fileNotFoundException, filename + ":" + t);
          if (!isSyntaxCheck)
            Logger
                .info("reading isosurface data from " + fullPathNameReturn[0]);
          setShapeProperty(iShape, "commandOption", "FILE" + (nFiles++) + "="
              + Escape.escape(fullPathNameReturn[0]));
          setShapeProperty(iShape, "fileName", fullPathNameReturn[0]);
        }
        propertyValue = t;
        break;
      default:
        if (planeSeen && !surfaceObjectSeen) {
          setShapeProperty(iShape, "nomap", new Float(0));
          surfaceObjectSeen = true;
        }
        if (!setMeshDisplayProperty(iShape, i, theTok))
          error(ERROR_invalidArgument);
        i = iToken;
      }
      idSeen = (theTok != Token.delete);
      if (propertyName == "property" && !surfaceObjectSeen) {
        surfaceObjectSeen = true;
        setShapeProperty(iShape, "bsSolvent", lookupIdentifierValue("solvent"));
        propertyName = "sasurface";
        propertyValue = new Float(0);
      }
      if (isWild && surfaceObjectSeen)
        error(ERROR_invalidArgument);
      if (propertyName != null)
        setShapeProperty(iShape, propertyName, propertyValue);
    }
    if (isCavity && !surfaceObjectSeen) {
      surfaceObjectSeen = true;
      setShapeProperty(iShape, "bsSolvent", lookupIdentifierValue("solvent"));
      setShapeProperty(iShape, "sasurface", new Float(0));
    }

    if (planeSeen && !surfaceObjectSeen) {
      setShapeProperty(iShape, "nomap", new Float(0));
      surfaceObjectSeen = true;
    }
    if (thisSetNumber > 0)
      setShapeProperty(iShape, "getSurfaceSets", new Integer(thisSetNumber - 1));
    if (colorScheme != null)
      setShapeProperty(iShape, "setColorScheme", colorScheme);
    Object area = null;
    Object volume = null;
    if (doCalcArea) {
      area = viewer.getShapeProperty(iShape, "area");
      if (area instanceof Float)
        viewer.setFloatProperty("isosurfaceArea", ((Float) area).floatValue());
      else
        viewer.setUserVariable("isosurfaceArea", ScriptVariable.getVariable(area));
    }
    if (doCalcVolume) {
      volume = (doCalcVolume ? viewer.getShapeProperty(iShape, "volume") : null);
      if (volume instanceof Float)
        viewer.setFloatProperty("isosurfaceVolume", ((Float) volume)
            .floatValue());
      else
        viewer.setUserVariable("isosurfaceVolume", ScriptVariable.getVariable(volume));
    }
    if (surfaceObjectSeen && isIsosurface && !isSyntaxCheck) {
      setShapeProperty(iShape, "finalize", null);
      Integer n = (Integer) viewer.getShapeProperty(iShape, "count");
      float[] dataRange = (float[]) viewer
          .getShapeProperty(iShape, "dataRange");
      String s = (String) viewer.getShapeProperty(iShape, "ID");
      if (s != null) {
        s += " created with cutoff = "
            + viewer.getShapeProperty(iShape, "cutoff")
            + " ; number of isosurfaces = " + n;
        if (dataRange != null && dataRange[0] != dataRange[1])
          s += "\ncolor range " + dataRange[2] + " " + dataRange[3]
              + "; mapped data range " + dataRange[0] + " to " + dataRange[1];
        if (doCalcArea)
          s += "\nisosurfaceArea = " + Escape.escapeDoubleArray(area);
        if (doCalcVolume)
          s += "\nisosurfaceVolume = " + Escape.escapeDoubleArray(volume);
        showString(s);
      }
    } else if (doCalcArea || doCalcVolume) {
      if (doCalcArea)
        showString("isosurfaceArea = " + Escape.escapeDoubleArray(area));
      if (doCalcVolume)
        showString("isosurfaceVolume = " + Escape.escapeDoubleArray(volume));
    }
    if (translucency != null)
      setShapeProperty(iShape, "translucency", translucency);
    setShapeProperty(iShape, "clear", null);
  }

  private boolean setMeshDisplayProperty(int shape, int i, int tok)
      throws ScriptException {
    String propertyName = null;
    Object propertyValue = null;
    boolean checkOnly = (i == 0);
    // these properties are all processed in MeshCollection.java
    switch (tok) {
    case Token.opaque:
    case Token.translucent:
      if (checkOnly)
        return true;
      colorShape(shape, iToken, false);
      return true;
    case Token.nada:
    case Token.delete:
    case Token.on:
    case Token.off:
    case Token.hide:
    case Token.hidden:
    case Token.display:
    case Token.displayed:
      if (iToken == 1)
        setShapeProperty(shape, "thisID", (String) null);
      if (tok == Token.nada)
        return (iToken == 1);
      if (checkOnly)
        return true;
      if (tok == Token.delete) {
        setShapeProperty(shape, "delete", null);
        return true;
      }
      if (tok == Token.hidden || tok == Token.hide)
        tok = Token.off;
      else if (tok == Token.displayed || tok == Token.display)
        tok = Token.on;
      // fall through for on/off
    case Token.frontlit:
    case Token.backlit:
    case Token.fullylit:
    case Token.contourlines:
    case Token.nocontourlines:
    case Token.dots:
    case Token.nodots:
    case Token.mesh:
    case Token.nomesh:
    case Token.fill:
    case Token.nofill:
    case Token.triangles:
    case Token.notriangles:
    case Token.frontonly:
    case Token.notfrontonly:
      propertyName = "token";
      propertyValue = new Integer(tok);
      break;
    }
    if (propertyName == null)
      return false;
    if (checkOnly)
      return true;
    setShapeProperty(shape, propertyName, propertyValue);
    if ((tok = tokAt(iToken + 1)) != Token.nada) {
      if (!setMeshDisplayProperty(shape, ++iToken, tok))
        --iToken;
    }
    return true;
  }

  static Quaternion getAtomQuaternion(Viewer viewer, BitSet bs) {
    int i = BitSetUtil.firstSetBit(bs);
    if (i < 0)
      return null;
    return (i < 0 ? null : viewer.getModelSet().getAtomAt(i).getQuaternion(
            viewer.getQuaternionFrame()));
  }
  
  // OK, that's all there is to it... Simple enough! ;)

}
