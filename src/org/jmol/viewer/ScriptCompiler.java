/* $Author: hansonr $
 * $Date: 2009-08-23 07:29:58 +0200 (dim., 23 août 2009) $
 * $Revision: 11331 $
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

package org.jmol.viewer;

import org.jmol.util.Logger;
import org.jmol.util.CommandHistory;
import org.jmol.util.Parser;
import org.jmol.i18n.GT;
import org.jmol.modelset.Group;
import org.jmol.modelset.Bond.BondSet;

import java.util.Hashtable;
import java.util.Vector;
import java.util.BitSet;

class ScriptCompiler extends ScriptCompilationTokenParser {

  /**
   * The Compiler clas is really two parts -- 
   * 
   * Compiler.class          going from characters to tokens
   * CompilationTokenParser  further syntax checking and modifications
   * 
   * The data structures follow the following sequences:
   * 
   * String script ==> Vector lltoken[][] --> Token[][] aatokenCompiled[][]
   * 
   * A given command goes through the sequence:
   * 
   * String characters --> Token token --> Vector ltoken[] --> Token[][] aatokenCompiled[][]
   * 
   */
  private static final String LOAD_TYPES = "append;files;menu;trajectory;models;" + JmolConstants.LOAD_ATOM_DATA_TYPES;
  
  ScriptCompiler(Viewer viewer) {
    this.viewer = viewer;
  }
  
  private Viewer viewer;
  private String filename;
  private boolean isSilent;

  // returns:
  
  private Hashtable contextVariables;
  private Token[][] aatokenCompiled;
  private short[] lineNumbers;
  private int[][] lineIndices;
  
  private int lnLength = 8;
  private boolean preDefining;
  private boolean isShowScriptOutput;
  private boolean isCheckOnly;
  private boolean haveComments;

  String scriptExtensions;
  
  private ScriptFunction thisFunction;
  
 
  /**
   * return a structure that is only the first part of the process - identifying lines and commands
   * for the scriptEditor
   * 
   * @param script
   * @return        ScriptContext
   */
  
  ScriptContext parseScriptForTokens(String script) {
    this.script = script;
    filename = null;
    isCheckOnly = true;
    isSilent = true;
    logMessages = false;
    preDefining = false;
    return parseScript(false);
  }
 
  private ScriptContext parseScript(boolean doFull) {
    if (!compile0(doFull))
      handleError();
    ScriptContext sc = new ScriptContext();
    sc.script = script;
    sc.scriptExtensions = scriptExtensions;
    sc.errorType = errorType;
    if (errorType != null) {
      sc.iCommandError = iCommand;
      setAaTokenCompiled();
    }
    sc.aatoken = aatokenCompiled;
    sc.errorMessage = errorMessage;
    sc.errorMessageUntranslated = (errorMessageUntranslated == null 
        ? errorMessage : errorMessageUntranslated);
    sc.lineIndices = lineIndices;
    sc.lineNumbers = lineNumbers;
    sc.contextVariables = contextVariables;
    return sc;
  }

  ScriptContext compile(String filename, String script, boolean isPredefining,
                  boolean isSilent, boolean debugScript, boolean isCheckOnly) {
    this.isCheckOnly = isCheckOnly;
    this.filename = filename;
    this.isSilent = isSilent;
    this.script = script;
    logMessages = (!isSilent && !isPredefining && debugScript);
    preDefining = (filename == "#predefine");
    return parseScript(true);
  }

  private void addContextVariable(String ident) {
    if (thisFunction == null) {
      if (contextVariables == null)
        contextVariables = new Hashtable();
      contextVariables.put(ident, (new ScriptVariable(Token.string, "")).setName(ident));
    } else {
      thisFunction.addVariable(ident, false);
    }
  }
  
  private boolean isContextVariable(String ident) {
    return (thisFunction != null ? thisFunction.isVariable(ident)
      : contextVariables != null && contextVariables.containsKey(ident));
  }
  
  /**
   * allows for three kinds of comments.
   * NOTE: closing involves asterisks and slash together, but that can't be shown here. 
   * 
   * 1) /** .... ** /  super-comment
   * 2) /* ..... * /   may be INSIDE /**....** /).
   * 3)  \n//.....\n   single-line comments -- like #, but removed entirely 
   * The reason is that /* ... * / will appear as standard in MOVETO command
   * but we still might want to escape it, so around that you can have /** .... ** /
   * 
   * The terminator is not necessary -- so you can quickly escape anything in a file 
   * after /** or /*
   * 
   * In addition, we can have [/*|/**] .... **** Jmol Embedded Script ****  [script commands] [** /|* /]
   * Then ONLY that script is taken. This is a powerful and simple way then to include Jmol scripting
   * in any file -- including, for example, HTML as an HTML comment. Just send the whole file to 
   * Jmol, and it will find its script!
   * 
   * @param script
   * @return cleaned script
   */
  private String cleanScriptComments(String script) {
    int pt = (script.indexOf("\0##"));
    if (pt >= 0) {
      // these are for jmolConsole and scriptEditor
      scriptExtensions = script.substring(pt + 1);
      script = script.substring(0, pt);
    }
    haveComments = (script.indexOf("#") >= 0); // speeds processing
    pt = script.indexOf(JmolConstants.EMBEDDED_SCRIPT_TAG);
    if (pt < 0)
      return script;
    int pt1 = script.lastIndexOf("/*", pt);
    int pt2 = script.indexOf((script.charAt(pt1 + 2) == '*' ? "*" : "") + "*/", pt);
    return (pt1 < 0 || pt2 < pt ? script 
        : script.substring(pt + JmolConstants.EMBEDDED_SCRIPT_TAG.length(), pt2));
  }
  
  private ScriptFlowContext flowContext;
  private Vector ltoken;
  private Vector lltoken;
  private Vector vBraces;


  private int ichBrace;
  private int cchToken;
  private int cchScript;

  private int nSemiSkip;
  private int parenCount;
  private int braceCount;
  private int setBraceCount;
  private int bracketCount;
  private int ptSemi;
  private int forPoint3;
  private int setEqualPt;
  private int iBrace;

  private boolean iHaveQuotedString;
  private boolean isEndOfCommand;
  private boolean needRightParen;
  private boolean endOfLine;

  private String comment;

  private void addTokenToPrefix(Token token) {
    if (logMessages)
      Logger.debug("addTokenToPrefix" + token);
    ltoken.addElement(token);
    lastToken = token;
  }

  private final static int OK = 0;
  private final static int OK2 = 1;
  private final static int CONTINUE = 2;
  private final static int EOL = 3;
  private final static int ERROR = 4;

  private int tokLastMath;
  
  private boolean compile0(boolean isFull) {
    
    script = cleanScriptComments(script);
    cchScript = this.script.length();

    // these four will be returned:
    contextVariables = null;
    lineNumbers = null;
    lineIndices = null;
    aatokenCompiled = null;
    
    thisFunction = null;
    flowContext = null;
    errorType = null;
    errorMessage = null;
    errorMessageUntranslated = null;
    errorLine = null;

    nSemiSkip = 0;
    ichToken = 0;
    ichCurrentCommand = 0;
    ichComment = 0;
    ichBrace = 0;
    lineCurrent = 1;
    iCommand = 0;
    tokLastMath = 0;
    lastToken = Token.tokenOff;
    vBraces = new Vector();
    iBrace = 0;
    braceCount = 0;
    parenCount = 0;
    ptSemi = -10;
    cchToken = 0;
    lnLength = 8;
    lineNumbers = new short[lnLength];
    lineIndices = new int[lnLength][2];
    isNewSet = isSetBrace = false;
    ptNewSetModifier = 1;
    isShowScriptOutput = false;    
    iHaveQuotedString = false;
    lltoken = new Vector();
    ltoken = new Vector();
    tokCommand = Token.nada;
    lastFlowCommand = null;
    tokenAndEquals = null;
    setBraceCount = 0;
    bracketCount = 0;
    forPoint3 = -1;
    setEqualPt = Integer.MAX_VALUE;
    endOfLine = false;
    comment = null;
    isEndOfCommand = false;
    needRightParen = false;
    theTok = Token.nada;
    short iLine = 1;

    for (; true; ichToken += cchToken) {
      if ((nTokens = ltoken.size()) == 0) { 
        if (thisFunction != null && thisFunction.chpt0 == 0)
          thisFunction.chpt0 = ichToken;
        ichCurrentCommand = ichToken;
        iLine = lineCurrent;
      }
      if (lookingAtLeadingWhitespace())
        continue;
      endOfLine = false;
      if (!isEndOfCommand) {
        endOfLine = lookingAtEndOfLine();
        switch (endOfLine ? OK : lookingAtComment()) {
        case CONTINUE: //  short /*...*/ or comment to completely ignore 
          continue;
        case EOL: // /* .... \n ... */ -- flag as end of line but ignore
          isEndOfCommand = true;
          continue;
        case OK2: // really just line-ending comment -- mark it for later inclusion
          isEndOfCommand = true;
          // start-of line comment -- include as Token.nada 
          comment = script.substring(ichToken, ichToken + cchToken).trim();
          break;
        }
        isEndOfCommand = isEndOfCommand || endOfLine || lookingAtEndOfStatement();
      }
      
      if (isEndOfCommand) {
        isEndOfCommand = false;
        switch (processTokenList(iLine, isFull)) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
        if (ichToken < cchScript)
          continue;
        setAaTokenCompiled();
        return (flowContext == null 
            || error(ERROR_missingEnd, Token.nameOf(flowContext.token.tok)));
      }
      
      if (nTokens > 0) {
        switch (checkSpecialParameterSyntax()) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
      }
      if (lookingAtLookupToken(ichToken)) {
        String ident = getPrefixToken();
        switch (parseKnownToken(ident)) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
        switch (parseCommandParameter(ident)) {
        case CONTINUE:
          continue;
        case ERROR:
          return false;
        }
        addTokenToPrefix(theToken);
        continue;
      }
      if (nTokens == 0 || (isNewSet || isSetBrace)
          && nTokens == ptNewSetModifier) {
        if (nTokens == 0 && lookingAtImpliedString())
          ichEnd = ichToken + cchToken;
        return commandExpected();
      }
      return error(ERROR_unrecognizedToken, script.substring(ichToken,
          ichToken + 1));
    }
  }
  
  private void setAaTokenCompiled() {
    aatokenCompiled = new Token[lltoken.size()][];
    lltoken.copyInto(aatokenCompiled);
  }

  private boolean lookingAtLeadingWhitespace() {
    int ichT = ichToken;
    while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
      ++ichT;
    if (isLineContinuation(ichT, true))
      ichT += 1 + nCharNewLine(ichT + 1);
    cchToken = ichT - ichToken;
    return cchToken > 0;
  }

  private boolean isLineContinuation(int ichT, boolean checkMathop) {
    boolean isEscaped = (ichT + 2 < cchScript && script.charAt(ichT) == '\\' && nCharNewLine(ichT + 1) > 0 
        || checkMathop && lookingAtMathContinuation(ichT));   
    if (isEscaped)
      lineCurrent++;
    return isEscaped;
  }

  private boolean lookingAtMathContinuation(int ichT) {
    int n;
    if (ichT >= cchScript || (n = nCharNewLine(ichT)) == 0 || lastToken.tok == Token.leftbrace)
      return false;
    if (parenCount > 0 || bracketCount > 0)
      return true;
    if (tokCommand != Token.set && tokCommand != Token.print)
        return false;
    if (lastToken.tok == tokLastMath)
      return true;
    ichT += n;
    while (ichT < cchScript && isSpaceOrTab(script.charAt(ichT)))
      ++ichT;
    return (lookingAtLookupToken(ichT) 
        && tokLastMath != 0);
  }

  private boolean lookingAtEndOfLine() {
    int ichT = ichEnd = ichToken;
    if (ichToken >= cchScript) {
      ichEnd = cchScript;
      return true;
    }
    int n = nCharNewLine(ichT);
    if (n == 0)
      return false;
    ichEnd = ichToken;
    cchToken = n;
    return true;    
  }
  
  private int nCharNewLine(int ichT) {
    char ch = script.charAt(ichT); 
    return (ch != '\r' ? (ch == '\n' ? 1 : 0) 
        : ++ichT < cchScript && script.charAt(ichT) == '\n' ? 2 : 1);
  }

  private boolean lookingAtEndOfStatement() {
    boolean isSemi = (script.charAt(ichToken) == ';');
    if (isSemi && nTokens > 0)
      ptSemi = nTokens;
    if (!isSemi || nSemiSkip-- > 0)
      return false;
    cchToken = 1;
    return true;
  }

  private boolean isShowCommand;
  
  private int lookingAtComment() {
    char ch = script.charAt(ichToken);
    int ichT = ichToken;
    int ichFirstSharp = -1;

    // return CONTINUE: totally ignore
    // return EOL: treat as line end, even though it isn't
    // return OK: no comment here

    /*
     * New in Jmol 11.1.9: we allow for output from the set showScript command
     * to be used as input. These lines start with $ and have a [...] phrase
     * after them. Their presence switches us to this new mode where we use
     * those statements as our commands and any line WITHOUT those as comments.
     */
    if (ichToken == ichCurrentCommand && ch == '$') {
      isShowScriptOutput = true;
      isShowCommand = true;
      while (ch != ']' && ichT < cchScript && !eol(ch = script.charAt(ichT)))
        ++ichT;
      cchToken = ichT - ichToken;
      return CONTINUE;
    } else if (isShowScriptOutput && !isShowCommand) {
      ichFirstSharp = ichT;
    }
    if (ch == '/' && ichT + 1 < cchScript)
      switch (script.charAt(++ichT)) {
      case '/':
        ichFirstSharp = ichToken;
        ichEnd = ichT - 1;
        break;
      case '*':
        ichEnd = ichT - 1;
        String terminator = (++ichT < cchScript && (ch = script.charAt(ichT)) == '*' 
            ? "**/" : "*/");
        ichT = script.indexOf(terminator, ichToken + 2);
        if (ichT < 0) {
          ichToken = cchScript;
          return EOL;
        }
        // ichT points at char after /*, whatever that is. So even /***/ will be caught
        incrementLineCount(script.substring(ichToken, ichT));
        cchToken = ichT + (ch == '*' ? 3 : 2) - ichToken;
        return CONTINUE;
      default:
        return OK;
      }

    boolean isSharp = (ichFirstSharp < 0);
    if (isSharp && !haveComments)
      return OK;

    // old way:
    // first, find the end of the statement and scan for # (sharp) signs

    if (ichComment > ichT)
      ichT = ichComment;
    for (; ichT < cchScript; ichT++) {
      if (eol(ch = script.charAt(ichT))) {
        ichEnd = ichT;
        if (isLineContinuation(ichT - 1, false)) {
          ichT += nCharNewLine(ichT);
          continue;
        }
        if (!isSharp && ch == ';')
          continue;
        break;
      }
      if (ichFirstSharp > 0)
        continue;
      if (ch == '#')
        ichFirstSharp = ichT;
    }
    if (ichFirstSharp < 0) // there were no sharps found
      return OK;
    ichComment = ichFirstSharp;
    /****************************************************************
     * check for #jc comment if it occurs anywhere in the statement, then the
     * statement is not executed. This allows statements which are executed in
     * RasMol but are comments in Jmol
     ****************************************************************/

    if (isSharp && nTokens == 0 && cchScript - ichFirstSharp >= 3
        && script.charAt(ichFirstSharp + 1) == 'j'
        && script.charAt(ichFirstSharp + 2) == 'c') {
      // statement contains a #jc before then end ... strip it all
      cchToken = ichT - ichToken;
      return CONTINUE;
    }

    // if the sharp was not the first character then it isn't a comment
    if (ichFirstSharp != ichToken)
      return OK;

    /****************************************************************
     * check for leading #jx <space> or <tab> if you see it, then only strip
     * those 4 characters. if they put in #jx <newline> then they are not going
     * to execute anything, and the regular code will take care of it
     ****************************************************************/
    if (isSharp && cchScript > ichToken + 3 && script.charAt(ichToken + 1) == 'j'
        && script.charAt(ichToken + 2) == 'x'
        && isSpaceOrTab(script.charAt(ichToken + 3))) {
      cchToken = 4; // #jx[\s\t]
      return CONTINUE;
    }
    
    if (ichT == ichToken)
      return OK;

    // first character was a sharp, but was not #jx ... strip it all
    cchToken = ichT - ichToken;
    return (nTokens == 0 ? OK2 : CONTINUE);
  }

  private int processTokenList(short iLine, boolean doCompile) {
    if (nTokens > 0 || comment != null) {
      if (nTokens == 0) {
        // just a comment
        ichCurrentCommand = ichToken;
        if (comment != null)
          addTokenToPrefix(new Token(Token.nada,
              (comment.length() == 1 ? comment : comment.substring(1))));
      }
      // end of command or comment
      iCommand = lltoken.size();
      if (thisFunction != null && thisFunction.cmdpt0 < 0) {
        thisFunction.cmdpt0 = iCommand;
      }
      if (nTokens == 1 && tokenCommand.value.equals("{")
          && lastFlowCommand != null) {
        parenCount = setBraceCount = 0;
        tokCommand = lastFlowCommand.tok;
        tokenCommand = lastFlowCommand;
        ltoken.removeElementAt(0);
      }
      if (bracketCount > 0 || setBraceCount > 0 || parenCount > 0 
          || braceCount == 1 && !checkFlowStartBrace(true)) {
        error(nTokens == 1 ? ERROR_commandExpected : ERROR_endOfCommandUnexpected);
        return ERROR;
      }
      if (needRightParen) {
        addTokenToPrefix(Token.tokenRightParen);
        needRightParen = false;
      }

      if (ltoken.size() > 0) {
        if (doCompile && !compileCommand())
          return ERROR;
        if (logMessages) {
          Logger.debug("-------------------------------------");
        }
        if (!Token.tokAttr(tokCommand, Token.noeval)
            || atokenInfix.length > 0 && atokenInfix[0].intValue <= 0) {
          if (iCommand == lnLength) {
            short[] lnT = new short[lnLength * 2];
            System.arraycopy(lineNumbers, 0, lnT, 0, lnLength);
            lineNumbers = lnT;
            int[][] lnI = new int[lnLength * 2][2];
            System.arraycopy(lineIndices, 0, lnI, 0, lnLength);
            lineIndices = lnI;
            lnLength *= 2;
          }
          lineNumbers[iCommand] = iLine;
          lineIndices[iCommand][0] = ichCurrentCommand;
          lineIndices[iCommand][1] =  Math.max(ichCurrentCommand, 
              Math.min(cchScript, ichEnd == ichCurrentCommand ? ichToken : ichEnd));
          lltoken.addElement(atokenInfix);
          iCommand = lltoken.size();
        }
        if (tokCommand == Token.set)
          lastFlowCommand = null;
      }
      tokenCommand = null;
      tokenAndEquals = null;
      comment = null;
      tokCommand = Token.nada;
      iHaveQuotedString = isNewSet = isSetBrace = needRightParen = false;
      ptNewSetModifier = 1;
      ltoken.setSize(0);
      nTokens = nSemiSkip = 0;
      ptSemi = -10;
      forPoint3 = -1;
      setEqualPt = Integer.MAX_VALUE;

      if (endOfLine) {
        if (flowContext != null
            && flowContext.checkForceEndIf(1)) {
          forceFlowEnd(flowContext.token);
          isEndOfCommand = true;
          cchToken = 0;
          ichCurrentCommand = ichToken;
          lineCurrent--;
          return CONTINUE;
        }
      }
    }
    if (endOfLine) {
      isShowCommand = false;
      ++lineCurrent;
    }
    if (ichToken >= cchScript) {
      // check for end of all brace work
      tokenCommand = Token.tokenAll;
      tokCommand = 1;
      switch (checkFlowEndBrace()) {
      case ERROR:
        return ERROR;
      case CONTINUE:
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      ichToken = cchScript;
      return OK; //main loop exit
    }
    return OK;
  }

  private boolean compileCommand() {
    if (ltoken.size() == 0) {
      // comment
      atokenInfix = new Token[0];
      ltoken.copyInto(atokenInfix);
      return true;
    }
    tokenCommand = (Token) ltoken.firstElement();
    tokCommand = tokenCommand.tok;

    isImplicitExpression = Token.tokAttr(tokCommand,
        Token.mathExpressionCommand);
    isSetOrDefine = (tokCommand == Token.set || tokCommand == Token.define);
    isCommaAsOrAllowed = Token.tokAttr(tokCommand, Token.atomExpressionCommand);
    int size = ltoken.size();
    int tok;
    int pt = size - 1;
    if (size == 1 && Token.tokAttr(tokCommand, Token.defaultON)) {
      addTokenToPrefix(Token.tokenOn);
    } else if (tokCommand == Token.set && size > 2) {
      if ((tok = ((Token) ltoken.get(pt)).tok) == Token.plusPlus 
          || tok == Token.minusMinus
          || (tok = ((Token) ltoken.get(pt = 1)).tok) == Token.plusPlus 
          || tok == Token.minusMinus) {
        ltoken.removeElementAt(pt);
        addTokenToPrefix(Token.tokenEquals);
        for (int i = 1; i < size - 1; i++)
          addTokenToPrefix((Token) ltoken.elementAt(i));
        addTokenToPrefix(tok == Token.minusMinus ? Token.tokenMinus
            : Token.tokenPlus);
        addTokenToPrefix(Token.intToken(1));
        if (((Token) ltoken.get(2)).tok == Token.leftsquare)
          ltoken.setElementAt(Token.tokenSetArray, 0);
      }
    }
    if (tokenAndEquals != null) {
      int j;
      int i = 0;
      for (i = 1; i < size; i++) {
        if ((j = ((Token) ltoken.elementAt(i)).tok) == Token.andequals)
          break;
      }
      size = i;
      i++;
      if (ltoken.size() < i) {
        System.out.println("COMPILER ERROR! - andEquals ");
      } else {
        for (j = 1; j < size; j++, i++)
          ltoken.insertElementAt((Token) ltoken.elementAt(j), i);
        ltoken.setElementAt(Token.tokenEquals, size);
        ltoken.insertElementAt(tokenAndEquals, i);
        ltoken.insertElementAt(Token.tokenLeftParen, ++i);
        addTokenToPrefix(Token.tokenRightParen);
      }
    }

    atokenInfix = new Token[size = ltoken.size()];
    ltoken.copyInto(atokenInfix);
    if (logMessages) {
      Logger.debug("token list:");
      for (int i = 0; i < atokenInfix.length; i++)
        Logger.debug(i + ": " + atokenInfix[i]);
      Logger.debug("vBraces list:");
      for (int i = 0; i < vBraces.size(); i++)
        Logger.debug(i + ": " + vBraces.get(i));
      Logger.debug("-------------------------------------");
    }
    
    // compile expressions  (CompilationTokenParser.java)

    return compileExpressions();
    
  }

  
  private String getPrefixToken() {
    String ident = script.substring(ichToken, ichToken + cchToken);
    // hack to support case sensitive alternate locations and chains
    // if an identifier is a single character long, then
    // allocate a new Token with the original character preserved
    if (ident.length() == 1) {
      if ((theToken = Token.getTokenFromName(ident)) == null
          && (theToken = Token.getTokenFromName(ident.toLowerCase())) != null)
        theToken = new Token(theToken.tok, theToken.intValue, ident);
    } else {
      ident = ident.toLowerCase();
      theToken = Token.getTokenFromName(ident);
    }
    if (theToken == null) {
      if (ident.indexOf("property_") == 0)
        theToken = new Token(Token.property, ident.toLowerCase());
      else
        theToken = new Token(Token.identifier, ident);
    }    
    theTok = theToken.tok;
    return ident;
  }

  private int checkSpecialParameterSyntax() {
    char ch;
    if (nTokens == ptNewSetModifier) {
      if (tokCommand == Token.set || Token.tokAttr(tokCommand, Token.setparam)) {
        ch = script.charAt(ichToken);
        if (ch == '=')
          setEqualPt = ichToken;

        // axes, background, define, display, echo, frank, hbond, history,
        // set, var
        // can all appear with or without "set" in front of them. These
        // are then
        // both commands and parameters for the SET command, but only if
        // they are
        // the FIRST parameter of the set command.
        boolean isAndEquals = ("+-\\*/&|=".indexOf(ch) >= 0);
        if (Token.tokAttr(tokCommand, Token.setparam) && ch == '='
            || (isNewSet || isSetBrace) && (isAndEquals || ch == '.' || ch == '[')) {
          tokenCommand = (isAndEquals ? Token.tokenSet
              : ch == '[' && !isSetBrace ? Token.tokenSetArray : Token.tokenSetProperty);
          tokCommand = Token.set;
          ltoken.insertElementAt(tokenCommand, 0);
          cchToken = 1;
          switch (ch) {
          case '[':
            addTokenToPrefix(new Token(Token.leftsquare, "["));
            bracketCount++;
            return CONTINUE;
          case '.':
            addTokenToPrefix(new Token(Token.period, "."));
            return CONTINUE;
          case '-':
          case '+':
          case '*':
          case '/':
          case '\\':
          case '&':
          case '|':
            if (ichToken + 1 >= cchScript)
              return ERROR(ERROR_endOfCommandUnexpected);
            if (script.charAt(ichToken + 1) != ch) {
              if (script.charAt(ichToken + 1) != '=')
                return ERROR(ERROR_badContext, "" + ch);
            }
            break;
          default:
            lastToken = Token.tokenMinus; // just to allow for {(....)}
            return CONTINUE;
          }
        }
      }
    }
    
    // cd, echo, gotocmd, help, hover, javascript, label, message, and pause
    // all are implicitly strings. You CAN use "..." but you don't have to,
    // and you cannot use '...'. This way the introduction of single quotes 
    // as an equivalent of double quotes cannot break existing scripts. -- BH 06/2009
    
    if (lookingAtString(!Token.tokAttr(tokCommand, Token.implicitStringCommand))) {
      if (cchToken < 0)
        return ERROR(ERROR_endOfCommandUnexpected);
      String str = ((tokCommand == Token.load || tokCommand == Token.background || tokCommand == Token.script)
          && !iHaveQuotedString ? script.substring(ichToken + 1, ichToken
          + cchToken - 1) : getUnescapedStringLiteral());
      addTokenToPrefix(new Token(Token.string, str));
      iHaveQuotedString = true;
      if (tokCommand == Token.data && str.indexOf("@") < 0 && !getData(str))
        return ERROR(ERROR_missingEnd, "data");
      return CONTINUE;
    }
    if (tokCommand == Token.sync && nTokens == 1 && charToken()) {
      String ident = script.substring(ichToken, ichToken + cchToken);
      addTokenToPrefix(new Token(Token.identifier, ident));
      return CONTINUE;
    } else if (tokCommand == Token.load) {
      if (script.charAt(ichToken) == '@') {
        iHaveQuotedString = true;
        return OK;
      }
      if (nTokens == 1 && lookingAtLoadFormat()) {
        String strFormat = script.substring(ichToken, ichToken + cchToken);
        strFormat = strFormat.toLowerCase();
        if (Parser.isOneOf(strFormat, LOAD_TYPES))
          addTokenToPrefix(new Token(Token.identifier, strFormat));
        else if (strFormat.indexOf("=") == 0) {
          addTokenToPrefix(new Token(Token.string, strFormat));
        }
        return CONTINUE;
      }
      BitSet bs;
      if (script.charAt(ichToken) == '{' || parenCount > 0) {
      } else if ((bs = lookingAtBitset()) != null) {
        addTokenToPrefix(new Token(Token.bitset, bs));
        return CONTINUE;
      } else if (!iHaveQuotedString && lookingAtImpliedString()) {
        String str = script.substring(ichToken, ichToken + cchToken);
        int pt = str.indexOf(" ");
        if (pt > 0) {
          cchToken = pt;
          str = str.substring(0, pt);
        }
        addTokenToPrefix(new Token(Token.string, str));
        iHaveQuotedString = true;
        return CONTINUE;
      }
    } else if (tokCommand == Token.script) {
      if (!iHaveQuotedString && lookingAtImpliedString()) {
        String str = script.substring(ichToken, ichToken + cchToken);
        int pt = str.indexOf(" ");
        if (pt > 0) {
          cchToken = pt;
          str = str.substring(0, pt);
        }
        addTokenToPrefix(new Token(Token.string, str));
        iHaveQuotedString = true;
        return CONTINUE;
      }
    } else if (tokCommand == Token.write) {
      int pt = cchToken;
      // write image 300 300 filename
      // write script filename
      // write spt filename
      // write jpg filename
      // write filename
      if (nTokens == 2 && lastToken.tok == Token.frame)
        iHaveQuotedString = true;
      if (!iHaveQuotedString && lookingAtImpliedString()) {
        String str = script.substring(ichToken, ichToken + cchToken);
        if (str.startsWith("@{")) {
          iHaveQuotedString = true;
        } else if (str.indexOf(" ") < 0) {
          addTokenToPrefix(new Token(Token.string, str));
          iHaveQuotedString = true;
          return CONTINUE;
        }
        cchToken = pt;
      }
    }
    if (Token.tokAttr(tokCommand, Token.implicitStringCommand)
        && !(tokCommand == Token.script && iHaveQuotedString)
        && lookingAtImpliedString()) {
      String str = script.substring(ichToken, ichToken + cchToken);
      addTokenToPrefix(new Token(Token.string, str));
      return CONTINUE;
    }
    float value;
    if (!Float.isNaN(value = lookingAtExponential())) {
      addTokenToPrefix(new Token(Token.decimal, new Float(value)));
      return CONTINUE;
    }
    if (lookingAtObjectID(nTokens == 1)) {
      addTokenToPrefix(Token.getTokenFromName("$"));
      addTokenToPrefix(new Token(Token.identifier, script.substring(ichToken,
          ichToken + cchToken)));
      return CONTINUE;
    }
    if (lookingAtDecimal()) {
      value =
      // can't use parseFloat with jvm 1.1
      // Float.parseFloat(script.substring(ichToken, ichToken +
      // cchToken));
      Float.valueOf(script.substring(ichToken, ichToken + cchToken))
          .floatValue();
      int intValue = (JmolConstants.modelValue(script.substring(ichToken, ichToken + cchToken)));
      addTokenToPrefix(new Token(Token.decimal, intValue, new Float(value)));
      return CONTINUE;
    }
    if (lookingAtSeqcode()) {
      ch = script.charAt(ichToken);
      try {
        int seqNum = (ch == '*' || ch == '^' ? Integer.MAX_VALUE : Integer
            .parseInt(script.substring(ichToken, ichToken + cchToken - 2)));
        char insertionCode = script.charAt(ichToken + cchToken - 1);
        if (insertionCode == '^')
          insertionCode = ' ';
        if (seqNum < 0) {
          seqNum = -seqNum;
          addTokenToPrefix(Token.tokenMinus);
        }
        int seqcode = Group.getSeqcode(seqNum, insertionCode);
        addTokenToPrefix(new Token(Token.seqcode, seqcode, "seqcode"));
        return CONTINUE;
      } catch (NumberFormatException nfe) {
        return ERROR(ERROR_invalidExpressionToken, "" + ch);
      }
    }
    if (lookingAtInteger()) {
      String intString = script.substring(ichToken, ichToken + cchToken);
      int val = Integer.parseInt(intString);
      if (tokCommand == Token.breakcmd || tokCommand == Token.continuecmd) {
        if (nTokens != 1)
          return ERROR(ERROR_badArgumentCount);
        ScriptFlowContext f = (flowContext == null ? null : flowContext
            .getBreakableContext(val = Math.abs(val)));
        if (f == null)
          return ERROR(ERROR_badContext, (String) tokenCommand.value);
        ((Token) ltoken.get(0)).intValue = f.getPt0(); // copy
      }
      addTokenToPrefix(new Token(Token.integer, val, intString));
      return CONTINUE;
    }
    if (tokCommand == Token.structure && nTokens == 2
        || tokCommand == Token.frame && nTokens == 2
        || tokCommand == Token.polyhedra
        || lastToken.tok == Token.select
        || lastToken.tok == Token.within
        || !(lastToken.tok == Token.identifier || tokenAttr(lastToken,
            Token.mathfunc))) {
      // here if:
      // select ({...})
      // within({...})
      // structure helix ({...})
      // polyhedra n TO ({...})
      // NOT myfunc({...})
      // NOT mathFunc({...})
      // if you want to use a bitset there, you must use
      // bitsets properly: x.distance( ({1 2 3}) )
      boolean isBond = (script.charAt(ichToken) == '[');
      BitSet bs = lookingAtBitset();
      if (bs != null) {
        if (isBond)
          addTokenToPrefix(new Token(Token.bitset, new BondSet(bs)));
        // occasionally BondSet appears unknown in Eclipse even though it
        // is defined
        // in Eval.java -- doesn't seem to matter.
        else
          addTokenToPrefix(new Token(Token.bitset, bs));
        return CONTINUE;
      }
    }
    return OK;
  }

  private int parseKnownToken(String ident) {

    // specific token-based issues depend upon where we are in the command
    
    Token token;

    if (tokLastMath != 0)
      tokLastMath = theTok;
    switch (theTok) {
    case Token.andequals:
      if (theTok == Token.andequals) {
        if (nSemiSkip == forPoint3 && nTokens == ptSemi + 2) {
          token = lastToken;
          addTokenToPrefix(Token.tokenEquals);
          addTokenToPrefix(token);
          token = Token.getTokenFromName(ident.substring(0, 1));
          addTokenToPrefix(token);
          addTokenToPrefix(Token.tokenLeftParen);
          needRightParen = true;
          return CONTINUE;
        }
        if (tokCommand == Token.set) {
          tokenAndEquals = Token.getTokenFromName(ident.substring(0, 1));
          setEqualPt = ichToken;
          return OK;
        }
        // otherwise ignore
        return CONTINUE;
      }
      break;
    case Token.end:
    case Token.endifcmd:
      if (flowContext != null)
       flowContext.forceEndIf = false;
       // fall through
    case Token.elsecmd:
      if (nTokens > 0) {
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      break;
    case Token.forcmd:
      if (bracketCount > 0)  // ignore [FOR], as in 1C4D 
        break;
      // fall through
    case Token.elseif:
    case Token.whilecmd:
    case Token.ifcmd:
      if (nTokens > 1 && tokCommand != Token.set) {
        isEndOfCommand = true;
        if (flowContext != null)
          flowContext.forceEndIf = true;
        cchToken = 0;
        return CONTINUE;            
      }
      break;
    case Token.minusMinus:
    case Token.plusPlus:
      if (isNewSet && parenCount == 0 && bracketCount == 0 && ichToken <= setEqualPt) {
        nTokens = ltoken.size();
        addTokenToPrefix(Token.tokenEquals);
        setEqualPt = 0;
        for (int i = 1; i < nTokens; i++)
          addTokenToPrefix((Token)ltoken.elementAt(i));
        addTokenToPrefix(theTok == Token.minusMinus ? Token.tokenMinus : Token.tokenPlus);
        addTokenToPrefix(Token.intToken(1));
        return CONTINUE;  
      } else if (nSemiSkip == forPoint3 && nTokens == ptSemi + 2) {            
        token = lastToken;
        addTokenToPrefix(Token.tokenEquals);
        addTokenToPrefix(token);
        addTokenToPrefix(theTok == Token.minusMinus ? Token.tokenMinus : Token.tokenPlus);
        addTokenToPrefix(Token.intToken(1));
        return CONTINUE;
      }
      break;
    case Token.opEQ:
      if (parenCount == 0 && bracketCount == 0)
        setEqualPt = ichToken;
      break;
    case Token.period:
      if (tokCommand == Token.set && parenCount == 0 && bracketCount == 0 && ichToken < setEqualPt) {
        ltoken.insertElementAt(Token.tokenExpressionBegin, 1);
        addTokenToPrefix(Token.tokenExpressionEnd);
        ltoken.setElementAt(Token.tokenSetProperty, 0);
        setEqualPt = 0;
      }            
      break;
    case Token.leftbrace:
      braceCount++;
      if (braceCount == 1 && parenCount == 0 && checkFlowStartBrace(false)) {
        isEndOfCommand = true;
        if (flowContext != null)
          flowContext.forceEndIf = false;
        return CONTINUE;
      }

      // fall through
    case Token.leftparen:
      parenCount++;
      // the select() function uses dual semicolon notation
      // but we must differentiate from isosurface select(...) and set
      // picking select
      if (nTokens > 1
          && (lastToken.tok == Token.select
              || lastToken.tok == Token.forcmd || lastToken.tok == Token.ifcmd))
        nSemiSkip += 2;
      break;
    case Token.rightbrace:
      if (iBrace > 0 && parenCount == 0 && braceCount == 0) {
        ichBrace = ichToken;
        if (nTokens == 0) {
          braceCount = parenCount = 1;
        } else {
          braceCount = parenCount = nSemiSkip = 0;
          vBraces.add(theToken);
          iBrace++;
          isEndOfCommand = true;
          return CONTINUE;
        }
      }
      braceCount--;
      // fall through
    case Token.rightparen:
      parenCount--;
      if (parenCount < 0)
        return ERROR(ERROR_tokenUnexpected, ident);
      // we need to remove the semiskip if parentheses or braces have been
      // closed. 11.5.46
      if (parenCount == 0)
        nSemiSkip = 0;
      if (needRightParen) {
        addTokenToPrefix(Token.tokenRightParen);
        needRightParen = false;
      }
      break;
    case Token.leftsquare:
      bracketCount++;
      break;
    case Token.rightsquare:
      bracketCount--;
      if (bracketCount < 0)
        return ERROR(ERROR_tokenUnexpected, "]");
    }
    return OK;
  }

  private int parseCommandParameter(String ident) {
    // PART II:
    //
    // checking tokens based on the current command
    // all command starts are handled by case Token.nada

    nTokens = ltoken.size();
    switch (tokCommand) {
    case Token.nada:
      // first token in command
      lastToken = Token.tokenOff;
      ichCurrentCommand = ichEnd = ichToken;
      tokenCommand = theToken;
      tokCommand = theTok;
      // checking first here for a flow command because
      // if (......) {.....} ELSE
      if (Token.tokAttr(tokCommand, Token.flowCommand)) {
        lastFlowCommand = tokenCommand;
        if (iBrace > 0
            && (tokCommand == Token.elsecmd || tokCommand == Token.elseif)) {
          if (((Token) vBraces.get(iBrace - 1)).tok == Token.rightbrace) {
            vBraces.remove(--iBrace);
            vBraces.remove(--iBrace);
          }
        }
      }
      // before processing this command, check to see if we have completed
      // a right-brace.
      int ret = checkFlowEndBrace();
      if (ret == ERROR)
        return ERROR;
      else if (ret == CONTINUE) {
        // yes, so re-read this one
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }

      if (Token.tokAttr(tokCommand, Token.flowCommand)) {
        if (!checkFlowCommand((String) tokenCommand.value))
          return ERROR;
        theToken = tokenCommand;
        break;
      }

      if (theTok == Token.rightbrace) {
        // if }, just push onto vBrace, but NOT onto ltoken
        vBraces.add(tokenCommand);
        iBrace++;
        tokCommand = Token.nada;
        return CONTINUE;
      }
      if (theTok != Token.leftbrace)
        lastFlowCommand = null;

      if (Token.tokAttr(tokCommand, Token.command))
        break;

      // not the standard command
      // isSetBrace: {xxx}.yyy =  or {xxx}[xx].
      // isNewSet:   xxx =
      // but not xxx = where xxx is a known "set xxx" variant
      // such as "set hetero" or "set hydrogen" or "set solvent"
      isSetBrace = (theTok == Token.leftbrace);
      if (isSetBrace && !lookingAtBraceSyntax()) {
        isEndOfCommand = true;
        if (flowContext != null)
          flowContext.forceEndIf = false;
      }
      if (!isSetBrace && theTok != Token.plusPlus && theTok != Token.minusMinus
          && !Token.tokAttr(theTok, Token.identifier)
          && !Token.tokAttr(theTok, Token.setparam)
          && !isContextVariable(ident)) {
        commandExpected();
        return ERROR;
      }
      tokCommand = Token.set;
      isNewSet = !isSetBrace;
      setBraceCount = (isSetBrace ? 1 : 0);
      bracketCount = 0;
      setEqualPt = Integer.MAX_VALUE;
      ptNewSetModifier = (isNewSet ? 1 : Integer.MAX_VALUE);
      break;
    case Token.function:
      if (tokenCommand.intValue == 0) {
        if (nTokens != 1)
          break; // anything after name is ok
        // user has given macro command
        tokenCommand.value = ident;
        return CONTINUE; // don't store name in stack
      }
      if (nTokens == 1) {
        flowContext.setFunction(thisFunction = new ScriptFunction(ident));
        break; // function f
      }
      if (nTokens == 2) {
        if (theTok != Token.leftparen)
          return ERROR(ERROR_tokenExpected, "(");
        break; // function f (
      }
      if (nTokens == 3 && theTok == Token.rightparen)
        break; // function f ( )
      if (nTokens % 2 == 0) {
        // function f ( x , y )
        if (theTok != Token.comma && theTok != Token.rightparen)
          return ERROR(ERROR_tokenExpected, ", )");
        break;
      }
      thisFunction.addVariable(ident, true);
      break;
    case Token.elsecmd:
      if (nTokens == 1 && theTok != Token.ifcmd) {
        isEndOfCommand = true;
        cchToken = 0;
        return CONTINUE;
      }
      if (nTokens != 1 || theTok != Token.ifcmd && theTok != Token.leftbrace)
        return ERROR(ERROR_badArgumentCount);
      ltoken.removeElementAt(0);
      ltoken.addElement(flowContext.token = new Token(Token.elseif, "elseif"));
      tokCommand = Token.elseif;
      return CONTINUE;
    case Token.var:
      if (nTokens != 1)
        break;
      addContextVariable(ident);
      ltoken.removeElementAt(0);
      ltoken.addElement(Token.tokenSetVar);
      tokCommand = Token.set;
      break;
    case Token.end:
      if (nTokens != 1)
        return ERROR(ERROR_badArgumentCount);
      if (!checkFlowEnd(theTok, ident, ichCurrentCommand))
        return ERROR;
      if (theTok == Token.function)
        return CONTINUE;
      break;
    case Token.whilecmd:
      if (nTokens > 2 && braceCount == 0 && parenCount == 0) {
        isEndOfCommand = true;
        flowContext.setLine();
      }
      break;
    case Token.elseif:
    case Token.ifcmd:
      if (nTokens > 2 && braceCount == 0 && parenCount == 0) {
        isEndOfCommand = true;
        flowContext.setLine();
      }
      break;
    case Token.forcmd:
      if (nTokens == 1) {
        if (theTok != Token.leftparen)
          return ERROR(ERROR_unrecognizedToken, ident);
        forPoint3 = nSemiSkip = 0;
        nSemiSkip += 2;
      } else if (nTokens == 3 && ((Token) ltoken.get(2)).tok == Token.var) {
        addContextVariable(ident);
      } else if (braceCount == 0 && parenCount == 0) {
        isEndOfCommand = true;
        flowContext.setLine();
      }
      break;
    case Token.set:
      if (theTok ==  Token.leftbrace)
        setBraceCount++;
      else if (theTok == Token.rightbrace) {
        setBraceCount--;
        if (isSetBrace && setBraceCount == 0
            && ptNewSetModifier == Integer.MAX_VALUE)
          ptNewSetModifier = nTokens + 1;
      }
      if (nTokens == ptNewSetModifier) { // 1 when { is not present
        boolean isSetArray = false;
        if (theTok == Token.leftparen) {
          // mysub(xxx,xxx,xxx)
          Token token = (Token) ltoken.get(0);
          ltoken.setElementAt(tokenCommand = new Token(Token.function, 0, token.value), 0);
          tokCommand = Token.function;
          break;
        }
        if (theTok != Token.identifier && theTok != Token.andequals
            && (!Token.tokAttr(theTok, Token.setparam))) {
          if (isNewSet)
            commandExpected();
          else
            error(ERROR_unrecognizedParameter, "SET", ": " + ident);
          return ERROR;
        }
        if (isSetArray) {
          addTokenToPrefix(theToken);
          //token = Token.tokenArraySelector;
          //theTok = Token.leftsquare;
        } else if (nTokens == 1 
              && (lastToken.tok == Token.plusPlus || lastToken.tok == Token.minusMinus)) {
          ltoken.removeElementAt(0);
          tokenCommand = Token.tokenSet; 
          tokCommand = Token.set;
          ltoken.insertElementAt(tokenCommand, 0);
          addTokenToPrefix(lastToken);
          break;
        }
      }
      break;
    case Token.display:
    case Token.hide:
    case Token.restrict:
    case Token.select:
    case Token.delete:
    case Token.define:
      if (tokCommand == Token.define) {
        if (nTokens == 1) {
          // we are looking at the variable name
          if (theTok != Token.identifier) {
            if (preDefining) {
              if (!Token.tokAttr(theTok, Token.predefinedset)) {
                error(
                    "ERROR IN Token.java or JmolConstants.java -- the following term was used in JmolConstants.java but not listed as predefinedset in Token.java: "
                        + ident, null);
                return ERROR;
              }
            } else if (Token.tokAttr(theTok, Token.predefinedset)) {
              Logger
                  .warn("WARNING: predefined term '"
                      + ident
                      + "' has been redefined by the user until the next file load.");
            } else if (!isCheckOnly && ident.length() > 1) {
              Logger
                  .warn("WARNING: redefining "
                      + ident
                      + "; was "
                      + theToken
                      + "not all commands may continue to be functional for the life of the applet!");
              theTok = theToken.tok = Token.identifier;
              Token.addToken(ident, theToken);
            }
          }
          addTokenToPrefix(theToken);
          lastToken = Token.tokenComma;
          return CONTINUE;
        }
        if (nTokens == 2) {
          if (theTok == Token.opEQ) {
            // we are looking at @x =.... just insert a SET command
            // and ignore the =. It's the same as set @x ...
            ltoken.insertElementAt(Token.tokenSet, 0);
            return CONTINUE;
          }
        }
      }
      if (bracketCount == 0 && theTok != Token.identifier
          && !Token.tokAttr(theTok, Token.expression) 
          && (theTok & Token.minmaxmask) != theTok) 
        return ERROR(ERROR_invalidExpressionToken, ident);
      break;
    case Token.center:
      if (theTok != Token.identifier && theTok != Token.dollarsign
          && !Token.tokAttr(theTok, Token.expression))
        return ERROR(ERROR_invalidExpressionToken, ident);
      break;
    }
    return OK;
  }

  private boolean checkFlowStartBrace(boolean atEnd) {
    if (!Token.tokAttr(tokCommand, Token.flowCommand)
        || tokCommand == Token.breakcmd || tokCommand == Token.continuecmd)
      return false;
    if (atEnd) {
      //ltoken.remove(--nTokens);
      vBraces.add(tokenCommand);
      iBrace++;
      parenCount = braceCount = 0;
    }
    return true;
  }

  private int checkFlowEndBrace() {
    if (iBrace <= 0
        || ((Token) vBraces.get(iBrace - 1)).tok != Token.rightbrace)
      return OK;
    // time to execute end
    vBraces.remove(--iBrace);
    Token token = (Token) vBraces.remove(--iBrace);
    return forceFlowEnd(token);
  }

  private int forceFlowEnd(Token token) {    
    Token t0 = tokenCommand;    
    tokenCommand = new Token(Token.end, "end");
    tokCommand = tokenCommand.tok;
    if (!checkFlowCommand("end"))
      return Token.nada;
    addTokenToPrefix(tokenCommand);
    switch (token.tok) {
    case Token.ifcmd:
    case Token.elsecmd:
    case Token.elseif:
      token = Token.tokenIf;
      break;
    default:
      token = Token.getTokenFromName((String)token.value);
      break;
    }
    if (!checkFlowEnd(token.tok, (String)token.value, ichBrace))
      return ERROR;
    if (token.tok != Token.function)
      addTokenToPrefix(token);
    tokenCommand = t0;
    if (tokenCommand != null)
      tokCommand = tokenCommand.tok;
    return CONTINUE;
  }

  private boolean checkFlowCommand(String ident) {
    int pt = lltoken.size();
    boolean isEnd = false;
    boolean isNew = true;
    switch (tokCommand) {
    case Token.end:
      if (flowContext == null)
        return error(ERROR_badContext, ident);
      isEnd = true;
      if (flowContext.token.tok != Token.function)
        tokenCommand = new Token(tokCommand, -flowContext.getPt0(), ident); //copy
      break;
    case Token.ifcmd:
    case Token.forcmd:
    case Token.whilecmd:
      break;
    case Token.endifcmd:
      isEnd = true;
      if (flowContext == null || flowContext.token.tok != Token.ifcmd
          && flowContext.token.tok != Token.elsecmd
          && flowContext.token.tok != Token.elseif)
        return error(ERROR_badContext, ident);
      break;
    case Token.elsecmd:
      if (flowContext == null || flowContext.token.tok != Token.ifcmd
          && flowContext.token.tok != Token.elseif)
        return error(ERROR_badContext, ident);
      flowContext.token.intValue = flowContext.setPt0(pt);
      break;
    case Token.breakcmd:
    case Token.continuecmd:
      isNew = false;
      ScriptFlowContext f = (flowContext == null ? null : flowContext.getBreakableContext(0));
      if (f == null)
        return error(ERROR_badContext, ident);
      tokenCommand = new Token(tokCommand, f.getPt0(), ident); //copy
      break;
    case Token.elseif:
      if (flowContext == null || flowContext.token.tok != Token.ifcmd
          && flowContext.token.tok != Token.elseif
          && flowContext.token.tok != Token.elsecmd)
        return error(ERROR_badContext, "elseif");
      flowContext.token.intValue = flowContext.setPt0(pt);
      break;
    case Token.function:
      if (flowContext != null)
        return error(ERROR_badContext, "function");
      break;
    }
    if (isEnd) {
      flowContext.token.intValue = pt;
      if (tokCommand == Token.endifcmd)
        flowContext = flowContext.getParent();
    } else if (isNew) {
      tokenCommand = new Token(tokCommand, tokenCommand.value); //copy
      if (tokCommand == Token.elsecmd || tokCommand == Token.elseif) {
        flowContext.token = tokenCommand;
      } else {
        flowContext = new ScriptFlowContext(this, tokenCommand, pt, flowContext);        
      }
    }
    tokCommand = tokenCommand.tok;
    return true;
  }

  private boolean checkFlowEnd(int tok, String ident, int pt1) {
    if (flowContext == null || flowContext.token.tok != tok)
      if (tok != Token.ifcmd || flowContext.token.tok != Token.elsecmd
          && flowContext.token.tok != Token.elseif)
        return error(ERROR_badContext, "end " + ident);
    switch (tok) {
    case Token.ifcmd:
    case Token.forcmd:
    case Token.whilecmd:
      break;
    case Token.function:
      if (!isCheckOnly) {
        addTokenToPrefix(new Token(Token.function, thisFunction));
        ScriptFunction.setFunction(thisFunction, script, pt1, lltoken.size(),
            lineNumbers, lineIndices, lltoken);
      }
      thisFunction = null;
      tokenCommand.intValue = 0;
      flowContext = flowContext.getParent();
      return true;
    default:
      return error(ERROR_unrecognizedToken, "end " + ident);
    }
    flowContext = flowContext.getParent();
    return true;
  }

  private boolean getData(String key) {
    ichToken += key.length() + 2;
    if (script.length() > ichToken && script.charAt(ichToken) == '\r') {
      lineCurrent++;ichToken++;
    }
    if (script.length() > ichToken && script.charAt(ichToken) == '\n') {
      lineCurrent++;ichToken++;
    }
    int i = script.indexOf(chFirst + key + chFirst, ichToken) - 4;
    if (i < 0 || !script.substring(i, i + 4).equalsIgnoreCase("END "))
      return false;
    String str = script.substring(ichToken, i);
    incrementLineCount(str);
    addTokenToPrefix(new Token(Token.data, str));
    addTokenToPrefix(new Token(Token.identifier, "end"));
    addTokenToPrefix(new Token(Token.string, key));
    cchToken = i - ichToken + key.length() + 6;
    return true;
  }

  private int incrementLineCount(String str) {
    char ch;
    int pt = str.indexOf('\r');
    int pt2 = str.indexOf('\n');
    if (pt < 0 && pt2 < 0)
      return 0;
    int n = lineCurrent;
    if (pt < 0 || pt2 < pt)
      pt = pt2;
    for (int i = str.length(); --i >= pt;) {
      if ((ch = str.charAt(i)) == '\n' || ch == '\r')
        lineCurrent++;
    }
    return lineCurrent - n;
  }
  
  private static boolean isSpaceOrTab(char ch) {
    return ch == ' ' || ch == '\t';
  }

  private boolean eol(char ch) {
    return eol(ch, nSemiSkip);  
  }
  
  static boolean eol(char ch, int nSkip) {
    return (ch == '\r' || ch == '\n' || ch == ';' && nSkip <= 0);  
  }
  
  private boolean lookingAtBraceSyntax() {
    // isSetBrace: {xxx}.yyy =  or {xxx}[xx].
    int ichT = ichToken;
    int nParen = 1;
    while (++ichT < cchScript && nParen > 0) {
      switch (script.charAt(ichT)) {
      case '{':
        nParen++;
        break;
      case '}':
        nParen--;
      break;
      }
    }
    if (ichT < cchScript && script.charAt(ichT) == '[' && ++nParen == 1)
      while (++ichT < cchScript && nParen > 0) {
        switch (script.charAt(ichT)) {
        case '[':
          nParen++;
          break;
        case ']':
          nParen--;
        break;
        }
      }
    if (ichT < cchScript && script.charAt(ichT) == '.' && nParen == 0) {
      return true;
    }
    
    return false;
  }

  char chFirst;
  private boolean lookingAtString(boolean allowPrime) {
    if (ichToken == cchScript)
      return false;
    chFirst = script.charAt(ichToken);
    if (chFirst != '"' && (!allowPrime || chFirst != '\''))
      return false;
    int ichT = ichToken;
    char ch;
    boolean previousCharBackslash = false;
    while (++ichT < cchScript) {
      ch = script.charAt(ichT);
      if (ch == chFirst && !previousCharBackslash)
        break;
      previousCharBackslash = (ch == '\\' ? !previousCharBackslash : false);
    }
    if (ichT == cchScript)
      cchToken = -1;
    else
      cchToken = ++ichT - ichToken;
    return true;
  }

  String getUnescapedStringLiteral() {
    if (cchToken < 2)
      return "";
    StringBuffer sb = new StringBuffer(cchToken - 2);
    int ichMax = ichToken + cchToken - 1;
    int ich = ichToken + 1;
    while (ich < ichMax) {
      char ch = script.charAt(ich++);
      if (ch == '\\' && ich < ichMax) {
        ch = script.charAt(ich++);
        switch (ch) {
        case 'b':
          ch = '\b';
          break;
        case 'n':
          ch = '\n';
          break;
        case 't':
          ch = '\t';
          break;
        case 'r':
          ch = '\r';
        // fall into
        case '"':
        case '\\':
        case '\'':
          break;
        case 'x':
        case 'u':
          int digitCount = ch == 'x' ? 2 : 4;
          if (ich < ichMax) {
            int unicode = 0;
            for (int k = digitCount; --k >= 0 && ich < ichMax;) {
              char chT = script.charAt(ich);
              int hexit = getHexitValue(chT);
              if (hexit < 0)
                break;
              unicode <<= 4;
              unicode += hexit;
              ++ich;
            }
            ch = (char) unicode;
          }
        }
      }
      sb.append(ch);
    }
    return sb.toString();
  }

  static int getHexitValue(char ch) {
    if (ch >= '0' && ch <= '9')
      return ch - '0';
    else if (ch >= 'a' && ch <= 'f')
      return 10 + ch - 'a';
    else if (ch >= 'A' && ch <= 'F')
      return 10 + ch - 'A';
    else
      return -1;
  }

  //  static String[] loadFormats = { "append", "files", "trajectory", "menu", "models",
  //  /*ancient:*/ "alchemy", "mol2", "mopac", "nmrpdb", "charmm", "xyz", "mdl", "pdb" };

  private boolean lookingAtLoadFormat() {
    // just allow a simple word or =xxxx
    int ichT = ichToken;
    char ch = '\0';
    while (ichT < cchScript
        && ((ch = script.charAt(ichT)) == '=' && ichT == ichToken 
            || Character.isLetterOrDigit(ch)))
      ++ichT;
    if (ichT == ichToken || !eol(ch) && !isSpaceOrTab(ch))
      return false;
    cchToken = ichT - ichToken;
    return true;
  }

  /**
   * An "implied string" is a parameter that is not quoted
   * but because of its position in a command is implied to
   * be a string. First we must exclude @xxxx. Then we consume
   * the entire math syntax @{......} or any set of characters
   * not involving white space.
   * 
   * @return true or false
   */
  private boolean lookingAtImpliedString() {
    int ichT = ichToken;
    char ch;
    // look ahead to \n, \r, terminal ;, or }
    while (ichT < cchScript && !eol(ch = script.charAt(ichT)) && ch != '}')
      ++ichT;
    boolean isMath = false;
    // if we have @{ then this is not an implied string look ahead to \n, \r, terminal ;, or }
    if (ichT > ichToken && script.charAt(ichToken) == '@'
        && (ichT <= ichToken + 1 || !(isMath = script.charAt(ichToken + 1) == '{')))
      return false;
    if (isMath) {
      ichT = ichMathTerminator(script, ichToken + 1, cchScript);
      if (ichT == cchScript)
        return false;
      return ((cchToken = ichT  + 1 - ichToken) > 0);
    }
    while (--ichT > ichToken && Character.isWhitespace(script.charAt(ichT))) {
    }
    return (cchToken = ++ichT - ichToken) > 0;
  }

  /**
   * For @{....}
   * 
   * @param script
   * @param ichT
   * @param len
   * @return     position of "}"
   */
  static int ichMathTerminator(String script, int ichT, int len) {
    int nP = 1;
    char chFirst = '\0';
    char chLast = '\0';
    while (nP > 0 && ++ichT < len) {
      char ch = script.charAt(ichT);
      if (chFirst != '\0') {
        if (chLast == '\\') {
          ch = '\0';
        } else if (ch == chFirst) {
          chFirst = '\0';
        }
        chLast = ch;
        continue;
      }
      switch(ch) {
      case '\'':
      case '"':
        chFirst = ch;
        break;
      case '{':
        nP++;
        break;
      case '}':
        nP--;
        break;
      }
    }
    return ichT;
  }

  private float lookingAtExponential() {
    if (ichToken == cchScript)
      return Float.NaN; //end
    int ichT = ichToken;
    boolean isNegative = (script.charAt(ichT) == '-');
    if (isNegative)
      ++ichT;
    int pt0 = ichT;
    boolean digitSeen = false;
    char ch = 'X';
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    if (ichT < cchScript && ch == '.')
      ++ichT;
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    if (ichT == cchScript || !digitSeen)
      return Float.NaN; //integer
    int ptE = ichT;
    int factor = 1;
    int exp = 0;
    boolean isExponential = (ch == 'E' || ch == 'e');
    if (!isExponential || ++ichT == cchScript)
      return Float.NaN;
    ch = script.charAt(ichT);
    // I THOUGHT we only should allow "E+" or "E-" here, not "2E1" because
    // "2E1" might be a PDB het group by that name. BUT it turns out that
    // any HET group starting with a number is unacceptable and must
    // be given as [nXm], in brackets.

    if (ch == '-' || ch == '+') {
      ichT++;
      factor = (ch == '-' ? -1 : 1);
    }
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT))) {
      ichT++;
      exp = (exp * 10 + ch - '0');
    }
    if (exp == 0)
      return Float.NaN;
    cchToken = ichT - ichToken;
    double value = Float.valueOf(script.substring(pt0, ptE)).doubleValue();
    value *= (isNegative ? -1 : 1) * Math.pow(10, factor * exp);
    return (float) value;
  }

  private boolean lookingAtDecimal() {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    if (script.charAt(ichT) == '-')
      ++ichT;
    boolean digitSeen = false;
    char ch = 'X';
    while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT++)))
      digitSeen = true;
    if (ch != '.')
      return false;
    // only here if  "dddd."

    // to support 1.ca, let's check the character after the dot
    // to determine if it is an alpha
    char ch1;
    if (ichT < cchScript && !eol(ch1 = script.charAt(ichT))) {
      if (Character.isLetter(ch1) || ch1 == '?')
        return false;
      //well, guess what? we also have to look for 86.1Na, so...
      //watch out for moveto..... 56.;refresh...
      if (ichT + 1 < cchScript
          && (Character.isLetter(ch1 = script.charAt(ichT + 1)) || ch1 == '?'))
        return false;
    }
    while (ichT < cchScript && Character.isDigit(script.charAt(ichT))) {
      ++ichT;
      digitSeen = true;
    }
    cchToken = ichT - ichToken;
    return digitSeen;
  }

  private boolean lookingAtSeqcode() {
    int ichT = ichToken;
    char ch = ' ';
    if (ichT + 1 < cchScript && script.charAt(ichT) == '*'
        && script.charAt(ichT + 1) == '^') {
      ch = '^';
      ++ichT;
    } else {
      if (script.charAt(ichT) == '-')
        ++ichT;
      while (ichT < cchScript && Character.isDigit(ch = script.charAt(ichT)))
        ++ichT;
    }
    if (ch != '^')
      return false;
    ichT++;
    if (ichT == cchScript)
      ch = ' ';
    else
      ch = script.charAt(ichT++);
    if (ch != ' ' && ch != '*' && ch != '?' && !Character.isLetter(ch))
      return false;
    cchToken = ichT - ichToken;
    return true;
  }

  private boolean lookingAtInteger() {
    if (ichToken == cchScript)
      return false;
    int ichT = ichToken;
    if (script.charAt(ichToken) == '-')
      ++ichT;
    int ichBeginDigits = ichT;
    while (ichT < cchScript && Character.isDigit(script.charAt(ichT)))
      ++ichT;
    if (ichBeginDigits == ichT)
      return false;
    cchToken = ichT - ichToken;
    return true;
  }

  BitSet lookingAtBitset() {
    // ({n n:m n}) or ({null})
    // [{n:m}] is a BOND bitset
    // EXCEPT if the previous token was a function:
    // {carbon}.distance({3 3 3})
    // Yes, I wish I had used {{...}}, but this will work. 
    // WITHIN ({....}) unfortunately has two contexts
    
    if (script.indexOf("({null})", ichToken) == ichToken) {
      cchToken = 8;
      return new BitSet();
    }
    if (ichToken + 4 > cchScript 
        || script.charAt(ichToken + 1) != '{'
      ||(script.charAt(ichToken) != '(' 
        && script.charAt(ichToken) != '['))
      return null;
    int ichT = ichToken + 2;
    char chEnd = (script.charAt(ichToken) == '(' ? ')' : ']');
    char ch = ' ';
    while (ichT < cchScript && (ch = script.charAt(ichT)) != '}'
        && (Character.isDigit(ch) || isSpaceOrTab(ch) || ch == ':'))
      ichT++;
    if (ch != '}' || ichT + 1 == cchScript
        || script.charAt(ichT + 1) != chEnd)
      return null;
    int iprev = -1;
    int ipt = 0;
    BitSet bs = new BitSet();
    for (int ich = ichToken+ 2; ich < ichT;ich = ipt) {
      while (isSpaceOrTab(ch = script.charAt(ich)))
        ich++;
      ipt = ich;
      while (Character.isDigit(ch = script.charAt(ipt)))
        ipt++;
      if (ipt == ich) // possibly :m instead of n:m
        return null;
      int val = Integer.parseInt(script.substring(ich, ipt));
      if (ch == ':') {
        iprev = val;
        ipt++;
      } else {
        if (iprev >= 0) {
          if (iprev > val)
            return null;
          for (int i = iprev; i <= val; i++)
            bs.set(i);
        } else {
          bs.set(val);
        }
        iprev = -1;
      }
    }
    if (iprev >= 0)
      return null;
    cchToken = ichT + 2 - ichToken;
    return bs;
  }
  
  private boolean lookingAtObjectID(boolean allowWildID) {
    int ichT = ichToken;
    if (ichT == cchScript || script.charAt(ichT) != '$')
      return false;
    if (++ichT != cchScript && script.charAt(ichT) == '"')
      return false;
    while (ichT < cchScript) {
      char ch;
      if (Character.isWhitespace(ch = script.charAt(ichT))) {
        if (ichT == ichToken + 1)
          return false;
        break;
      }
      if (!Character.isLetterOrDigit(ch)) {
        switch (ch) {
        default:
          return false;
        case '*':
          if (!allowWildID)
            return false;
        case '~':
        case '_':
          break;
        }
      }
      ichT++;
    }
    cchToken = ichT - (++ichToken);
    return true;
  }

  private boolean lookingAtLookupToken(int ichT) {
    if (ichT == cchScript)
      return false;
    int ichT0 = ichT;
    tokLastMath = 0;
    char ch;
    switch (ch = script.charAt(ichT++)) {
    case '-':
    case '+':
    case '&':
    case '|':
      if (ichT < cchScript) {
        if (script.charAt(ichT) == ch) {
          ++ichT;
          if (ch == '-' || ch == '+')
            break;
        } else if (script.charAt(ichT) == '=') {
          ++ichT;
        }
      }
      tokLastMath++;
      break;
    case '/':
      if (ichT < cchScript && script.charAt(ichT) == '/')
        break;
    case '\\':  // leftdivide
    case '*':
    case '!':
      if (ichT < cchScript && script.charAt(ichT) == '=')
        ++ichT;
      tokLastMath++;
      break;
    case ')':
    case ']':
    case '}':
    case '.':
      break;
    case '(':
    case ',':
    case '{':
    case '$':
    case ':':
    case ';':
    case '@':
    case '%':
    case '[':
      tokLastMath = 1;
      break;
    case '<':
  case '=':
    case '>':
      if (ichT < cchScript
          && ((ch = script.charAt(ichT)) == '<' || ch == '=' || ch == '>'))
        ++ichT;
      tokLastMath = 1;
      break;
    default:
      if (!Character.isLetter(ch))
        return false;
    //fall through
    case '~':
    case '_':
    case '\'':
    case '?': // include question marks in identifier for atom expressions
      if (ch == '?')
        tokLastMath = 1;
      while (ichT < cchScript
          && (Character.isLetterOrDigit(ch = script.charAt(ichT)) 
              || ch == '_' || ch == '?' || ch == '~' || ch == '\'')
          ||
          // hack for insertion codes embedded in an atom expression :-(
          // select c3^a
          (ch == '^' && ichT > ichT0 && Character.isDigit(script
              .charAt(ichT - 1)))
          || ch == '\\' && ichT + 1 < cchScript && script.charAt(ichT + 1) == '?')
        ++ichT;
      break;
    }
    cchToken = ichT - ichT0;
    return true;
  }

  private boolean charToken() {
    char ch;
    if (ichToken == cchScript || (ch = script.charAt(ichToken)) == '"' || ch == '@')
      return false;
    int ichT = ichToken;
    while (ichT < cchScript && !isSpaceOrTab(ch = script.charAt(ichT)) 
        && ch != '#' && ch != '}' && !eol(ch))
        ++ichT;
    cchToken = ichT - ichToken;
    return true;
  }
 
  
  private int ERROR(int error) {
    error(error, null, null);
    return ERROR;
  }
  
  private int ERROR(int error, String value) {
    error(error, value);
    return ERROR;
  }
  
  private boolean handleError() {
    errorType = errorMessage;
    errorLine = script.substring(ichCurrentCommand, ichEnd <= ichCurrentCommand ? ichToken : ichEnd);
    String lineInfo = (ichToken < ichEnd 
        ? errorLine.substring(0, ichToken - ichCurrentCommand)
              + " >>>> " + errorLine.substring(ichToken - ichCurrentCommand) 
        : errorLine)
        + " <<<<";
    errorMessage = GT.translate("script compiler ERROR: ") + errorMessage
         + ScriptEvaluator.setErrorLineMessage(null, filename, lineCurrent, iCommand, lineInfo);
    if (!isSilent) {
      viewer.addCommand(errorLine + CommandHistory.ERROR_FLAG);
      Logger.error(errorMessage);
    }
    return false;
  }

}
