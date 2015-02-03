/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-03-30 11:40:16 -0500 (Fri, 30 Mar 2007) $
 * $Revision: 7273 $
 *
 * Copyright (C) 2007 Miguel, Bob, Jmol Development
 *
 * Contact: hansonr@stolaf.edu
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
package org.jmol.jvxl.readers;

import java.io.BufferedReader;

import org.jmol.util.BinaryDocument;
import org.jmol.util.Parser;

abstract class SurfaceFileReader extends SurfaceReader {

  protected BufferedReader br;
  protected BinaryDocument binarydoc;
 
  SurfaceFileReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg);
    this.br = br; 
  }

  static String determineFileType(BufferedReader bufferedReader) {
    // JVXL should be on the FIRST line of the file, but it may be 
    // after comments or missing.
    
    // Apbs, Jvxl, or Cube, also efvet
    
    String line;
    LimitedLineReader br = new LimitedLineReader(bufferedReader, 16000);
    //sure bets, but not REQUIRED:
    if ((line = br.info()).indexOf("#JVXL+") >= 0)
      return "Jvxl+";
    if (line.indexOf("#JVXL") >= 0)
      return "Jvxl";
    if (line.indexOf("&plot") == 0)
      return "Jaguar";
    if (line.indexOf("!NTITLE") >= 0 || line.indexOf("REMARKS ") >= 0)
      return "Xplor";
    if (line.indexOf("MAP ") == 208)
      return "MRC" + line.substring(67,68);
    if (line.indexOf("<efvet ") >= 0)
      return "Efvet";
    if (line.indexOf(PmeshReader.PMESH_BINARY_MAGIC_NUMBER) == 0)
      return "Pmesh";
    line = br.readNonCommentLine();
    if (line.indexOf("object 1 class gridpositions counts") == 0)
      return "Apbs";

    // Jvxl, or Cube, maybe formatted Plt
    
    String[] tokens = Parser.getTokens(line); 
    line = br.readNonCommentLine();// second line
    if (tokens.length == 2 
        && Parser.parseInt(tokens[0]) == 3 
        && Parser.parseInt(tokens[1])!= Integer.MIN_VALUE) {
      tokens = Parser.getTokens(line);
      if (tokens.length == 3 
          && Parser.parseInt(tokens[0])!= Integer.MIN_VALUE 
          && Parser.parseInt(tokens[1])!= Integer.MIN_VALUE
          && Parser.parseInt(tokens[2])!= Integer.MIN_VALUE)
        return "PltFormatted";
    }
    line = br.readNonCommentLine(); // third line
    //next line should be the atom line
    int nAtoms = Parser.parseInt(line);
    if (nAtoms == Integer.MIN_VALUE)
      return (line.indexOf("+") == 0 ? "Jvxl+" : "UNKNOWN");
    if (nAtoms >= 0)
      return "Cube"; //Can't be a Jvxl file
    nAtoms = -nAtoms;
    for (int i = 4 + nAtoms; --i >=0;)
      if ((line = br.readNonCommentLine()) == null)
        return "UNKNOWN";
    int nSurfaces = Parser.parseInt(line);
    if (nSurfaces == Integer.MIN_VALUE)
      return "UNKNOWN";
    return (nSurfaces < 0 ?  "Jvxl" : "Cube"); //Final test looks at surface definition line
  }
  
  void discardTempData(boolean discardAll) {
    try {
      if (br != null)
        br.close();
      if (binarydoc != null)
        binarydoc.close();
    } catch (Exception e) {
    }
    super.discardTempData(discardAll);
  }
     
  String line;
  int[] next = new int[1];
  
  String[] getTokens() {
    return Parser.getTokens(line, 0);
  }

  float parseFloat() {
    return Parser.parseFloat(line, next);
  }

  float parseFloat(String s) {
    next[0] = 0;
    return Parser.parseFloat(s, next);
  }
/*
  float parseFloatNext(String s) {
    return Parser.parseFloat(s, next);
  }
*/
  int parseInt() {
    return Parser.parseInt(line, next);
  }
  
  int parseInt(String s) {
    next[0] = 0;
    return Parser.parseInt(s, next);
  }
  
  int parseIntNext(String s) {
    return Parser.parseInt(s, next);
  }
    
  protected void skipTo(String info, String what) throws Exception {
    if (info != null)
      while ((line = br.readLine()).indexOf(info) < 0) {
      }
    if (what != null)
      next[0] = line.indexOf(what) + what.length() + 2;
  }

/*  
  int parseInt(String s, int iStart) {
    next[0] = iStart;
    return Parser.parseInt(s, next);
  }
*/
  
}

class LimitedLineReader {
  //from Resolver
  private char[] buf;
  private int cchBuf;
  private int ichCurrent;
  private int iLine;

  LimitedLineReader(BufferedReader bufferedReader, int readLimit) {
    buf = new char[readLimit];
    try {
      bufferedReader.mark(readLimit);
      cchBuf = bufferedReader.read(buf);
      ichCurrent = 0;
      bufferedReader.reset();
    } catch (Exception e) {      
    }
  }

  String info() {
    return new String(buf);  
  }
  
  int iLine() {
    return iLine;
  }
  
  String readNonCommentLine() {
    while (ichCurrent < cchBuf) {
      int ichBeginningOfLine = ichCurrent;
      char ch = 0;
      while (ichCurrent < cchBuf &&
             (ch = buf[ichCurrent++]) != '\r' && ch != '\n') {
      }
      int cchLine = ichCurrent - ichBeginningOfLine;
      if (ch == '\r' && ichCurrent < cchBuf && buf[ichCurrent] == '\n')
        ++ichCurrent;
      iLine++;
      if (buf[ichBeginningOfLine] == '#') // flush comment lines;
        continue;
      StringBuffer sb = new StringBuffer(cchLine);
      sb.append(buf, ichBeginningOfLine, cchLine);
      return sb.toString();
    }
    return "";
  }
}
