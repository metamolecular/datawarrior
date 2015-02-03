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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point4f;
import java.io.BufferedReader;
import java.util.BitSet;
import java.util.Vector;

import org.jmol.shapesurface.IsosurfaceMesh;
import org.jmol.util.*;
import org.jmol.jvxl.api.MeshDataServer;
import org.jmol.jvxl.data.JvxlData;
import org.jmol.jvxl.data.MeshData;
import org.jmol.jvxl.data.VolumeData;

public class JvxlReader extends VolumeFileReader {

  private final static String JVXL_VERSION = "2.0";
  
  // 1.4 adds -nContours to indicate contourFromZero for MEP data mapped onto planes
  // 2.0 adds vertex/triangle compression when no grid is present 
  // Jmol 11.7.25 -- recoded so that we do not create voxelData[nx][ny][nz] and instead
  //                 simply create a BitSet of length nx * ny * nz. This saves memory hugely.

  // NEVER change the numbers for these next defaults
  
  final public static int defaultEdgeFractionBase = 35; //#$%.......
  final public static int defaultEdgeFractionRange = 90;
  final public static int defaultColorFractionBase = 35;
  final public static int defaultColorFractionRange = 90;

  JvxlReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    jvxlData.wasJvxl = isJvxl = true;
    isXLowToHigh = false;
  }

  protected static void jvxlUpdateInfo(JvxlData jvxlData, String[] title, int nBytes) {
    jvxlData.title = title;
    jvxlData.nBytes = nBytes;
    jvxlUpdateInfoLines(jvxlData);
  }

  public static void jvxlUpdateInfoLines(JvxlData jvxlData) {
    jvxlData.jvxlDefinitionLine = jvxlGetDefinitionLine(jvxlData, false);
    jvxlData.jvxlInfoLine = jvxlGetDefinitionLine(jvxlData, true);
  }
  //// methods used for reading any file format, but creating a JVXL file

  /////////////reading the format///////////

  private int surfaceDataCount;
  private int edgeDataCount;
  private int colorDataCount;
  private boolean haveContourData;

  protected boolean readVolumeData(boolean isMapData) {
    if (!super.readVolumeData(isMapData))
      return false;
    strFractionTemp = jvxlEdgeDataRead;
    fractionPtr = 0;
    return true;
  }

  protected boolean gotoAndReadVoxelData(boolean isMapData) {
    initializeVolumetricData();
    if (nPointsX < 0 || nPointsY < 0 || nPointsZ < 0) 
      return true;
    try {
      gotoData(params.fileIndex - 1, nPointsX * nPointsY * nPointsZ);
      if (vertexDataOnly)
        return true;
      readSurfaceData(isMapData);
      if (edgeDataCount > 0)
        jvxlEdgeDataRead = jvxlReadData("edge", edgeDataCount);
      if (colorDataCount > 0)
        jvxlColorDataRead = jvxlReadData("color", colorDataCount);
      if (haveContourData)
        jvxlDecodeContourData(getXmlData("jvxlContourData", null, false));
    } catch (Exception e) {
      Logger.error(e.toString());
      try {
      br.close();
      } catch (Exception e2) {
        // ignore
      }
      return false;
    }
    return true;
  }
  
  private int nThisValue;
  private boolean thisInside;
  
  protected void initializeVoxelData() {
    thisInside = !params.isContoured;
    nThisValue = 0;
  }
  
  protected void readSurfaceData(boolean isMapDataIgnored) throws Exception {
    initializeVoxelData();
    //calls VolumeFileReader.readVoxelData; no mapping allowed
    if (vertexDataOnly) {
      getEncodedVertexData();
      return;
    }
    if (params.thePlane == null) {
      super.readSurfaceData(false);
      return;
    }
    volumeData.setDataDistanceToPlane(params.thePlane);
    setVolumeData(volumeData);
    params.cutoff = 0f;
    setSurfaceInfo(jvxlData, params.thePlane, 0, new StringBuffer());
  }

  // #comments (optional)
  // info line1
  // info line2
  // -na originx originy originz   [ANGSTROMS/BOHR] optional; BOHR assumed
  // n1 x y z
  // n2 x y z
  // n3 x y z
  // a1 a1.0 x y z
  // a2 a2.0 x y z
  // a3 a3.0 x y z
  // a4 a4.0 x y z 
  // etc. -- na atoms
  // -ns 35 90 35 90 Jmol voxel format version 1.0
  // # more comments
  // cutoff +/-nEdges +/-nVertices [more here]
  // integer inside/outside edge data
  // ascii-encoded fractional edge data
  // ascii-encoded fractional color data
  // # optional comments

  protected void readTitleLines() throws Exception {
    jvxlFileHeaderBuffer = new StringBuffer(skipComments(false));
    if (line == null || line.length() == 0)
      line = "Line 1";
    jvxlFileHeaderBuffer.append(line).append('\n');
    if ((line = br.readLine()) == null || line.length() == 0)
      line = "Line 2";
    jvxlFileHeaderBuffer.append(line).append('\n');
  }

  
  /**
   * checks an atom line for "ANGSTROMS", possibly overriding the data's 
   * natural units, BOHR (similar to Gaussian CUBE files).
   * 
   * @param isXLowToHigh
   * @param isAngstroms
   * @param strAtomCount
   * @param atomLine
   * @param bs
   * @return  isAngstroms
   */
  protected static boolean jvxlCheckAtomLine(boolean isXLowToHigh, boolean isAngstroms,
                                   String strAtomCount, String atomLine,
                                   StringBuffer bs) {
    if (strAtomCount != null) {
      int atomCount = Parser.parseInt(strAtomCount);
      if (atomCount == Integer.MIN_VALUE) {
        atomCount = 0;
        atomLine = " " + atomLine.substring(atomLine.indexOf(" ") + 1);
      } else {
        String s = "" + atomCount;
        atomLine = atomLine.substring(atomLine.indexOf(s) + s.length());
      }
      bs.append((isXLowToHigh ? "+" : "-") + Math.abs(atomCount));
    }
    int i = atomLine.indexOf("ANGSTROM");
    if (isAngstroms && i < 0)
      atomLine += " ANGSTROMS";
    else if (atomLine.indexOf("ANGSTROMS") >= 0)
      isAngstroms = true;
    i = atomLine.indexOf("BOHR");
    if (!isAngstroms && i < 0)
      atomLine += " BOHR";
    bs.append(atomLine).append('\n');
    return isAngstroms;
  }
  
  protected void readAtomCountAndOrigin() throws Exception {
      jvxlFileHeaderBuffer.append(skipComments(false));
      String atomLine = line;
      String[] tokens = Parser.getTokens(atomLine, 0);
      isXLowToHigh = false;
      negativeAtomCount = true;
      atomCount = 0;
      if (tokens[0] == "-0") {
      } else if (tokens[0].charAt(0) == '+'){
        isXLowToHigh = true;
        atomCount = parseInt(tokens[0].substring(1));
      } else {
        atomCount = -parseInt(tokens[0]);
      }
      if (atomCount == Integer.MIN_VALUE)
        return;
      volumetricOrigin.set(parseFloat(tokens[1]), parseFloat(tokens[2]), parseFloat(tokens[3]));
      isAngstroms = jvxlCheckAtomLine(isXLowToHigh, isAngstroms, null, atomLine, jvxlFileHeaderBuffer);
      if (!isAngstroms)
        volumetricOrigin.scale(ANGSTROMS_PER_BOHR);
  }

  protected static void jvxlReadAtoms(BufferedReader br, StringBuffer bs, int atomCount,
                            VolumeData v) throws Exception {
    //mostly ignored
    for (int i = 0; i < atomCount; ++i)
      bs.append(br.readLine() + "\n");
    //if (atomCount == 0)
      //jvxlAddDummyAtomList(v, bs);
  }

  protected int readExtraLine() throws Exception {
    skipComments(true);
    Logger.info("Reading extra JVXL information line: " + line);
    int nSurfaces = parseInt(line);
    if (!(isJvxl = (nSurfaces < 0)))
      return nSurfaces;
    nSurfaces = -nSurfaces;
    Logger.info("jvxl file surfaces: " + nSurfaces);
    int ich;
    if ((ich = parseInt()) == Integer.MIN_VALUE) {
      Logger.info("using default edge fraction base and range");
    } else {
      edgeFractionBase = ich;
      edgeFractionRange = parseInt();
    }
    if ((ich = parseInt()) == Integer.MIN_VALUE) {
      Logger.info("using default color fraction base and range");
    } else {
      colorFractionBase = ich;
      colorFractionRange = parseInt();
    }
    cJvxlEdgeNaN = (char)(edgeFractionBase + edgeFractionRange);
    return nSurfaces;
  }

  private void jvxlReadDefinitionLine(boolean showMsg) throws Exception {
    String comment = skipComments(true);
    if (showMsg)
      Logger.info("reading jvxl data set: " + comment + line);
    haveContourData = (comment.indexOf("+contourlines") >= 0);
    jvxlCutoff = parseFloat(line);
    Logger.info("JVXL read: cutoff " + jvxlCutoff);

    //  optional comment line for compatibility with earlier Jmol versions:
    //  #+contourlines
    //  cutoff       nInts     (+/-)bytesEdgeData (+/-)bytesColorData
    //               param1              param2         param3    
    //                 |                   |              |
    //   when          |                   |        >  0 ==> jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &&   == -2 ==> contourable plane
    //   when        < -1*     &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    // * nInts saved as -1 - nInts
    
    // it's possible that a plane will not be contoured (-1 -1) when it is a solid color.
    // why you would want to save this as JVXL is another question.
    // instead, we just set "contour 1" to indicate just one contour to demo that.
    // In addition, now we consider contouring functionXY, so in that case we would
    // have surface data, edge data, and color data

    int param1 = parseInt();
    int param2 = parseInt();
    int param3 = parseInt();
    if (param3 == Integer.MIN_VALUE || param3 == -1)
      param3 = 0;

    if (param1 == -1) {
      // a plane is defined
      try {
        params.thePlane = new Point4f(parseFloat(), parseFloat(), parseFloat(),
            parseFloat());
      } catch (Exception e) {
        Logger
            .error("Error reading 4 floats for PLANE definition -- setting to 0 0 1 0  (z=0)");
        params.thePlane = new Point4f(0, 0, 1, 0);
      }
      Logger.info("JVXL read: {" + params.thePlane.x + " " + params.thePlane.y
          + " " + params.thePlane.z + " " + params.thePlane.w + "}");
      if (param2 == -1 && param3 < 0)
        param3 = -param3;
      //error in some versions of Jmol. (fixed in 11.3.54)
    } else {
      params.thePlane = null;
    }
    if (param1 < 0 && param2 != -1) {
      // contours are defined (possibly overridden -- this is just a display option
      // could be plane or functionXY
      params.isContoured = (param3 != 0);
      int nContoursRead = parseInt();
      if (nContoursRead != Integer.MIN_VALUE) {
        if (nContoursRead < 0) {
          nContoursRead = -1 - nContoursRead;
          params.contourFromZero = false; //MEP data to complete the plane
        }
        if (nContoursRead != 0 && params.nContours == 0) {
          params.nContours = nContoursRead;
          Logger.info("JVXL read: contours " + params.nContours);
        }
      }
    } else {
      params.isContoured = false;
    }

    jvxlDataIsPrecisionColor = (param1 == -1 && param2 == -2 
        || param3 < 0);
    params.isBicolorMap = (param1 > 0 && param2 < 0);
    jvxlDataIsColorMapped = (param3 != 0);
    jvxlDataIs2dContour = (jvxlDataIsColorMapped && params.isContoured);

    if (params.isBicolorMap || params.colorBySign)
      jvxlCutoff = 0;
    surfaceDataCount = (param1 < -1 ? -1 - param1 : param1 > 0 ? param1 : 0);
    //prior to JVXL 1.1 (4/2007), this number counts the bytes of integer data.
    //after that, the number of integers, for the progressive reader
    
    if (param1 == -1)
      edgeDataCount = 0; //plane
    else
      edgeDataCount = (param2 < -1 ? -param2 : param2 > 0 ? param2 : 0);
    colorDataCount = (params.isBicolorMap ? -param2 : param3 < -1 ? -param3
        : param3 > 0 ? param3 : 0);
    if (params.colorBySign)
      params.isBicolorMap = true;
    if (jvxlDataIsColorMapped) {
      float dataMin = parseFloat();
      float dataMax = parseFloat();
      float red = parseFloat();
      float blue = parseFloat();
      if (!Float.isNaN(dataMin) && !Float.isNaN(dataMax)) {
        if (dataMax == 0 && dataMin == 0) {
          //set standard -1/1; bit of a hack
          dataMin = -1;
          dataMax = 1;
        }
        params.mappedDataMin = dataMin;
        params.mappedDataMax = dataMax;
        Logger.info("JVXL read: data min/max: " + params.mappedDataMin + "/"
            + params.mappedDataMax);
      }
      if (!params.rangeDefined)
        if (!Float.isNaN(red) && !Float.isNaN(blue)) {
          if (red == 0 && blue == 0) {
            //set standard -1/1; bit of a hack
            red = -1;
            blue = 1;
          }
          params.valueMappedToRed = red;
          params.valueMappedToBlue = blue;
          params.rangeDefined = true;
        } else {
          params.valueMappedToRed = 0f;
          params.valueMappedToBlue = 1f;
          params.rangeDefined = true;
        }
      Logger.info("JVXL read: color red/blue: " + params.valueMappedToRed + "/"
          + params.valueMappedToBlue);
    }
    jvxlData.insideOut = (line.indexOf("insideOut") >= 0);
    if (params.insideOut)
      jvxlData.insideOut = !jvxlData.insideOut;
    params.insideOut = jvxlData.insideOut;
    jvxlData.valueMappedToRed = params.valueMappedToRed;
    jvxlData.valueMappedToBlue = params.valueMappedToBlue;
    jvxlData.mappedDataMin = params.mappedDataMin;
    jvxlData.mappedDataMax = params.mappedDataMax;
  }

  private String jvxlReadData(String type, int nPoints) {
    String str = "";
    try {
      while (str.length() < nPoints) {
        line = br.readLine();
        str += jvxlUncompressString(line);
      }
    } catch (Exception e) {
      Logger.error("Error reading " + type + " data " + e);
      throw new NullPointerException();
    }
    return str;
  }

  public static String jvxlCompressString(String data) {
    /* just a simple compression, but allows 2000-6000:1 CUBE:JVXL for planes!
     * 
     *   "X~nnn " means "nnn copies of character X" 
     *   
     *   ########## becomes "#~10 " 
     *   ~ becomes "~~" 
     *
     */
    StringBuffer dataOut = new StringBuffer();
    char chLast = '\0';
    data += '\0';
    int nLast = 0;
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == '\n' || ch == '\r')
        continue;
      if (ch == chLast) {
        ++nLast;
        if (ch != '~')
          ch = '\0';
      } else if (nLast > 0) {
        if (nLast < 4 || chLast == '~' || chLast == ' '
            || chLast == '\t')
          while (--nLast >= 0)
            dataOut.append(chLast);
        else 
          dataOut.append("~" + nLast + " ");
        nLast = 0;
      }
      if (ch != '\0') {
        dataOut.append(ch);
        chLast = ch;
      }
    }
    return dataOut.toString();
  }

  private static String jvxlUncompressString(String data) {
    if (data.indexOf("~") < 0)
      return data;
    StringBuffer dataOut = new StringBuffer();
    char chLast = '\0';
    int[] next = new int[1];
    for (int i = 0; i < data.length(); i++) {
      char ch = data.charAt(i);
      if (ch == '~') {
        next[0] = ++i;
        int nChar = Parser.parseInt(data, next);
        if (nChar == Integer.MIN_VALUE) {
          if (chLast == '~') {
            dataOut.append('~');
            while ((ch = data.charAt(++i)) == '~')
              dataOut.append('~');
          } else {
            Logger.error("Error uncompressing string " + data.substring(0, i)
                + "?");
          }
        } else {
          for (int c = 0; c < nChar; c++)
            dataOut.append(chLast);
          i = next[0];
        }
      } else {
        dataOut.append(ch);
        chLast = ch;
      }
    }
    return dataOut.toString();
  }

  protected BitSet getVoxelBitSet(int nPoints) throws Exception {
    BitSet bs = new BitSet();
    int bsVoxelPtr = 0;
    if (surfaceDataCount <= 0)
      return bs; //unnecessary -- probably a plane
    int nThisValue = 0;
    while (bsVoxelPtr < nPoints) {
      nThisValue = parseInt();
      if (nThisValue == Integer.MIN_VALUE) {
        line = br.readLine();
        // note -- does not allow for empty lines;
        // must be a continuous block of numbers.
        if (line == null || (nThisValue = parseInt(line)) == Integer.MIN_VALUE) {
          if (!endOfData)
            Logger.error("end of file in JvxlReader?" + " line=" + line);
          endOfData = true;
          nThisValue = 10000;
          //throw new NullPointerException();
        }
      } 
      thisInside = !thisInside;
      ++jvxlNSurfaceInts;
      if (thisInside)
        bs.set(bsVoxelPtr, bsVoxelPtr + nThisValue);
      bsVoxelPtr += nThisValue;
    }
    return bs;
  }
  
  protected float getNextVoxelValue(StringBuffer sb) throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (surfaceDataCount <= 0)
      return 0f; //unnecessary -- probably a plane
    while (nThisValue == 0) {
      nThisValue = parseInt();
      if (nThisValue == Integer.MIN_VALUE) {
        line = br.readLine();
        if (line == null || (nThisValue = parseInt(line)) == Integer.MIN_VALUE) {
          if (!endOfData)
            Logger.error("end of file in JvxlReader?" + " line=" + line);
          endOfData = true;
          nThisValue = 10000;
          //throw new NullPointerException();
        } else if (sb != null) {
          sb.append(line).append('\n');
        }
      } 
      thisInside = !thisInside;
      ++jvxlNSurfaceInts;
    }
    --nThisValue;
    return (thisInside ? 1f : 0f);
  }

  public static void setSurfaceInfoFromBitSet(JvxlData jvxlData, BitSet bs,
                                              Point4f thePlane) {
    StringBuffer sb = new StringBuffer();
    int nPoints = jvxlData.nPointsX * jvxlData.nPointsY * jvxlData.nPointsZ;
    int nSurfaceInts = jvxlEncodeBitSet(bs, nPoints, sb);
    setSurfaceInfo(jvxlData, thePlane, nSurfaceInts, sb);
  }
  
  private static int jvxlEncodeBitSet(BitSet bs, int nPoints, StringBuffer sb) {
    // nunset nset nunset ...
    int dataCount = 0;
    int n = 0;
    boolean isset = false;
    for (int i = 0; i < nPoints; ++i) {
      if (isset == bs.get(i)) {
        dataCount++;
      } else {
        sb.append(' ').append(dataCount);
        n++;
        dataCount = 1;
        isset = !isset;
      }
    }
    sb.append(' ').append(dataCount).append('\n');
    return n;
  }

  private static BitSet jvxlDecodeBitSet(String data) {
    // nunset nset nunset ...
    BitSet bs = new BitSet();
    int dataCount = 0;
    int ptr = 0;
    boolean isset = false;
    int[] next = new int[1];
    while ((dataCount = Parser.parseInt(data, next)) != Integer.MIN_VALUE) {
      if (isset)
        bs.set(ptr, ptr + dataCount);
      ptr += dataCount;
      isset = !isset;
    }
    return bs;
  }

  protected static void setSurfaceInfo(JvxlData jvxlData, Point4f thePlane, int nSurfaceInts, StringBuffer surfaceData) {
    jvxlData.jvxlSurfaceData = surfaceData.toString();
    if (jvxlData.jvxlSurfaceData.indexOf("--") == 0)
      jvxlData.jvxlSurfaceData = jvxlData.jvxlSurfaceData.substring(2);
    jvxlData.jvxlPlane = thePlane;
    jvxlData.nSurfaceInts = nSurfaceInts;
  }
  
  protected float getSurfacePointAndFraction(float cutoff, boolean isCutoffAbsolute, float valueA,
                         float valueB, Point3f pointA, Vector3f edgeVector, 
                         float[] fReturn, Point3f ptReturn) {
    if (edgeDataCount <= 0)
      return super.getSurfacePointAndFraction(cutoff, isCutoffAbsolute, valueA, valueB,
          pointA, edgeVector, fReturn, ptReturn);
    ptReturn.scaleAdd(fReturn[0] = jvxlGetNextFraction(edgeFractionBase, edgeFractionRange, 0.5f), 
        edgeVector, pointA);
    return fReturn[0];
  }

  private int fractionPtr;
  private String strFractionTemp = "";

  private float jvxlGetNextFraction(int base, int range, float fracOffset) {
    if (fractionPtr >= strFractionTemp.length()) {
      if (!endOfData)
        Logger.error("end of file reading compressed fraction data");
      endOfData = true;
      strFractionTemp = "" + (char) base;
      fractionPtr = 0;
    }
    return jvxlFractionFromCharacter(strFractionTemp.charAt(fractionPtr++),
        base, range, fracOffset);
  }

  protected String readColorData() {
    // overloads SurfaceReader
    // standard jvxl file read for color 

    fractionPtr = 0;
    int vertexCount = jvxlData.vertexCount = meshData.vertexCount;
    short[] colixes = meshData.vertexColixes;
    float[] vertexValues = meshData.vertexValues;
    strFractionTemp = (isJvxl ? jvxlColorDataRead : "");
    if (isJvxl && strFractionTemp.length() == 0) {
      Logger
          .error("You cannot use JVXL data to map onto OTHER data, because it only contains the data for one surface. Use ISOSURFACE \"file.jvxl\" not ISOSURFACE .... MAP \"file.jvxl\".");
      return "";
    }
    fractionPtr = 0;
    Logger.info("JVXL reading color data mapped min/max: " + params.mappedDataMin
        + "/" + params.mappedDataMax + " for " + vertexCount + " vertices."
        + " using encoding keys " + colorFractionBase + " "
        + colorFractionRange);
    Logger.info("mapping red-->blue for " + params.valueMappedToRed + " to "
        + params.valueMappedToBlue + " colorPrecision:"
        + jvxlDataIsPrecisionColor);

    float min = (params.mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMin
        : params.mappedDataMin);
    float range = (params.mappedDataMin == Float.MAX_VALUE ? defaultMappedDataMax
        : params.mappedDataMax)
        - min;
    float colorRange = params.valueMappedToBlue - params.valueMappedToRed;
    float contourPlaneMinimumValue = Float.MAX_VALUE;
    float contourPlaneMaximumValue = -Float.MAX_VALUE;
    if (colixes == null || colixes.length < vertexCount)
      meshData.vertexColixes = colixes = new short[vertexCount];
    String data = jvxlColorDataRead;
    //hasColorData = true;
    int cpt = 0;
    short colixNeg = 0, colixPos = 0;
    if (params.colorBySign) {
      colixPos = ColorEncoder
          .getColorIndex(params.isColorReversed ? params.colorNeg
              : params.colorPos);
      colixNeg = ColorEncoder
          .getColorIndex(params.isColorReversed ? params.colorPos
              : params.colorNeg);
    }
    int vertexIncrement = meshData.vertexIncrement;
    
    for (int i = 0; i < vertexCount; i+= vertexIncrement) {
      float fraction, value;
      if (jvxlDataIsPrecisionColor) {
        // this COULD be an option for mapped surfaces; 
        // necessary for planes; used for vertex/triangle 2.0 style
        // precision is used for FULL-data range encoding, allowing full
        // treatment of JVXL files as though they were CUBE files.
        // the two parts of the "double-character-precision" value
        // are in separate lines, separated by n characters.
        fraction = jvxlFractionFromCharacter2(data.charAt(cpt), data.charAt(cpt
            + vertexCount), colorFractionBase, colorFractionRange);
        value = min + fraction * range;
      } else {
        // my original encoding scheme
        // low precision only allows for mapping relative to the defined color range
        fraction = jvxlFractionFromCharacter(data.charAt(cpt),
            colorFractionBase, colorFractionRange, 0.5f);
        value = params.valueMappedToRed + fraction * colorRange;
      }
      vertexValues[i] = value;
      ++cpt;
      if (value < contourPlaneMinimumValue)
        contourPlaneMinimumValue = value;
      if (value > contourPlaneMaximumValue)
        contourPlaneMaximumValue = value;
      
      //note: these are just default colorings
      //orbital color had a bug through 11.2.6/11.3.6
      if (params.isContoured) {
        marchingSquares.setContourData(i, value);
      } else if (params.colorBySign) {
        colixes[i] = ((params.isColorReversed ? value > 0 : value <= 0) ? colixNeg
            : colixPos);
      } else {
        colixes[i] = getColorIndexFromPalette(value);
      }
    }
    if (params.mappedDataMin == Float.MAX_VALUE) {
      params.mappedDataMin = contourPlaneMinimumValue;
      params.mappedDataMax = contourPlaneMaximumValue;
    }
    return data + "\n";
  }

  protected void gotoData(int n, int nPoints) throws Exception {

    //called by VolumeFileReader.readVoxelData

    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    vertexDataOnly = jvxlData.vertexDataOnly = (nPoints == 0);
    for (int i = 0; i < n; i++) {
      jvxlReadDefinitionLine(true);
      Logger.info("JVXL skipping: jvxlSurfaceDataCount=" + surfaceDataCount
          + " jvxlEdgeDataCount=" + edgeDataCount
          + " jvxlDataIsColorMapped=" + jvxlDataIsColorMapped);
      jvxlSkipData(nPoints, true);
    }
    jvxlReadDefinitionLine(true);
  }

  private void jvxlSkipData(int nPoints, boolean doSkipColorData)
      throws Exception {
    // surfaceDataCount is quantitatively unreliable in pre-4/2007 versions (Jvxl 1.0)
    // so we just add them all up -- they must sum to nX * nY * nZ points 
    if (surfaceDataCount > 0) // unreliable in pre-4/2007 versions (Jvxl 1.0)
      jvxlSkipDataBlock(nPoints, true);
    if (edgeDataCount > 0)
      jvxlSkipDataBlock(edgeDataCount, false);
    if (jvxlDataIsColorMapped && doSkipColorData)
      jvxlSkipDataBlock(colorDataCount, false);
  }

  private void jvxlSkipDataBlock(int nPoints, boolean isInt) throws Exception {
    int n = 0;
    while (n < nPoints) {
      line = br.readLine();
      n += (isInt ? countData(line) : jvxlUncompressString(line).length());
    }
  }

  private int countData(String str) {
    int count = 0;
    int n = parseInt(str);
    while (n != Integer.MIN_VALUE) {
      count += n;
      n = parseIntNext(str);
    }
    return count;
  }

  //// methods for creating the JVXL code  

  protected static void jvxlCreateHeaderWithoutTitleOrAtoms(VolumeData v, StringBuffer bs) {
    jvxlCreateHeader(v, Integer.MAX_VALUE, null, null, bs);
  }

  protected static void jvxlCreateHeader(VolumeData v, int nAtoms, 
                                         Point3f[] atomXyz, int[] atomNo,
                                         StringBuffer sb) {
    // if the StringBuffer comes in non-empty, it should have two lines
    // that do not start with # already present.
    if (sb.length() == 0)
      sb.append("Line 1\nLine 2\n");
    sb.append(nAtoms == Integer.MAX_VALUE ? -2 : -nAtoms).append(' ')
      .append(v.volumetricOrigin.x).append(' ')
      .append(v.volumetricOrigin.y).append(' ')
      .append(v.volumetricOrigin.z).append(" ANGSTROMS\n");
    for (int i = 0; i < 3; i++)
      sb.append(v.voxelCounts[i]).append(' ')
        .append(v.volumetricVectors[i].x).append(' ')
        .append(v.volumetricVectors[i].y).append(' ')
        .append(v.volumetricVectors[i].z).append('\n');
    if (nAtoms == Integer.MAX_VALUE) {
      jvxlAddDummyAtomList(v, sb);
      return;
    }
    nAtoms = Math.abs(nAtoms);
      for (int i = 0, n = 0; i < nAtoms; i++)
        sb.append((n = Math.abs(atomNo[i])) + " " + n + ".0 "
            + atomXyz[i].x + " " + atomXyz[i].y + " " + atomXyz[i].z + "\n");
  }
  
  private static void jvxlAddDummyAtomList(VolumeData v, StringBuffer bs) {
    Point3f pt = new Point3f(v.volumetricOrigin);
    bs.append("1 1.0 ").append(pt.x).append(' ').append(pt.y).append(' ')
        .append(pt.z).append(" //BOGUS H ATOM ADDED FOR JVXL FORMAT\n");
    for (int i = 0; i < 3; i++)
      pt.scaleAdd(v.voxelCounts[i] - 1, v.volumetricVectors[i], pt);
    bs.append("2 2.0 ").append(pt.x).append(' ').append(pt.y).append(' ')
        .append(pt.z).append(" //BOGUS He ATOM ADDED FOR JVXL FORMAT\n");
  }

  public static String jvxlGetDefinitionLine(JvxlData jvxlData, boolean isInfo) {
    String definitionLine = (jvxlData.vContours == null ? "" : "#+contourlines\n")
        + jvxlData.cutoff + " ";

    //  optional comment line for compatibility with earlier Jmol versions:
    //  #+contourlines
    //  cutoff       nInts     (+/-)bytesEdgeData (+/-)bytesColorData
    //               param1              param2         param3    
    //                 |                   |              |
    //   when          |                   |        >  0 ==> jvxlDataIsColorMapped
    //   when          |                   |       == -1 ==> not color mapped
    //   when          |                   |        < -1 ==> jvxlDataIsPrecisionColor    
    //   when        == -1     &&   == -1 ==> noncontoured plane
    //   when        == -1     &&   == -2 ==> contourable plane
    //   when        < -1*     &&    >  0 ==> contourable functionXY
    //   when        > 0       &&    <  0 ==> jvxlDataisBicolorMap

    // * nInts saved as -1 - nInts

    if (jvxlData.jvxlSurfaceData == null)
      return "";
    StringBuffer info = new StringBuffer();
    int nSurfaceInts = jvxlData.nSurfaceInts;//jvxlData.jvxlSurfaceData.length();
    int bytesUncompressedEdgeData = (jvxlData.vertexDataOnly ? 0
        : jvxlData.jvxlEdgeData.length() - 1);
    int nColorData = (jvxlData.jvxlColorData.length() - 1);
    if (isInfo && !jvxlData.vertexDataOnly) {
      info.append("\n  cutoff=\"" + jvxlData.cutoff + "\"");
      info.append("\n  pointsPerAngstrom=\"" + jvxlData.pointsPerAngstrom
          + "\"");
      info.append("\n  nSurfaceInts=\"" + nSurfaceInts + "\"");
      info
          .append("\n  nBytesData=\""
              + (jvxlData.jvxlSurfaceData.length() + bytesUncompressedEdgeData + jvxlData.jvxlColorData
                  .length()) + "\"");
    }
    if (jvxlData.jvxlPlane == null) {
      if (jvxlData.isContoured) {
        if (isInfo)
          info.append("\n  contoured=\"true\"");
        else
          definitionLine += (-1 - nSurfaceInts) + " "
              + bytesUncompressedEdgeData;
      } else if (jvxlData.isBicolorMap) {
        if (isInfo)
          info.append("\n  bicolorMap=\"true\"");
        else
          definitionLine += (nSurfaceInts) + " " + (-bytesUncompressedEdgeData);

      } else {
        if (!isInfo)
          definitionLine += nSurfaceInts + " " + bytesUncompressedEdgeData;
        else if (nColorData > 0)
          info.append("\n  colorMapped=\"true\"");
      }
      if (!isInfo)
        definitionLine += " "
            + (jvxlData.isJvxlPrecisionColor && nColorData != -1 ? -nColorData
                : nColorData);
    } else {

      String s = " " + jvxlData.jvxlPlane.x + " " + jvxlData.jvxlPlane.y + " "
          + jvxlData.jvxlPlane.z + " " + jvxlData.jvxlPlane.w;
      if (!isInfo)
        definitionLine += (jvxlData.isContoured ? "-1 -2 " + (-nColorData)
            : "-1 -1 " + nColorData)
            + s;
      else if (nColorData > 0)
        info.append("\n  colorMapped=\"true\"");
      if (isInfo)
        info.append("\n  plane=\"{ " + s + " }\"");
    }
    if (jvxlData.isContoured) {
      if (isInfo)
        info.append("\n  nContours=\"" + Math.abs(jvxlData.nContours) + "\"");
      else
        definitionLine += " " + jvxlData.nContours;
    }
    // ...  mappedDataMin  mappedDataMax  valueMappedToRed  valueMappedToBlue ...
    float min = (jvxlData.mappedDataMin == Float.MAX_VALUE ? 0f
        : jvxlData.mappedDataMin);
    if (!isInfo)
      definitionLine += " " + min + " " + jvxlData.mappedDataMax + " "
          + jvxlData.valueMappedToRed + " " + jvxlData.valueMappedToBlue;

    if (isInfo && jvxlData.jvxlColorData.length() > 0 && !jvxlData.isBicolorMap) {
      info.append("\n  dataMinimum=\"" + min + "\"");
      info.append("\n  dataMaximum=\"" + jvxlData.mappedDataMax + "\"");
      info.append("\n  valueMappedToRed=\"" + jvxlData.valueMappedToRed + "\"");
      info.append("\n  valueMappedToBlue=\"" + jvxlData.valueMappedToBlue
          + "\"");
    }
    if (isInfo && jvxlData.jvxlCompressionRatio > 0)
      info.append("\n  approximateCompressionRatio=\""
          + jvxlData.jvxlCompressionRatio + ":1\"");
    if (isInfo && jvxlData.isXLowToHigh)
      info
          .append("\n  note=\"progressive JVXL+ -- X values read from low(0) to high("
              + (jvxlData.nPointsX - 1) + ")\"");
    if (jvxlData.insideOut) {
      if (isInfo)
        info.append("\n  insideOut=\"true\"");
      else
        definitionLine += " insideOut";
    }
    if (!isInfo)
      return definitionLine;
    info.append("\n  precisionColor=\"" + jvxlData.isJvxlPrecisionColor + "\"");
    info.append("\n  nColorData=\"" + nColorData + "\"");
    info.append("\n  version=\"" + jvxlData.version + "\"");
    return "<jvxlSurfaceInfo>" + info.toString() + "\n</jvxlSurfaceInfo>";
  }

  protected static String jvxlExtraLine(JvxlData jvxlData, int n) {
    return (-n) + " " + jvxlData.edgeFractionBase + " "
        + jvxlData.edgeFractionRange + " " + jvxlData.colorFractionBase + " "
        + jvxlData.colorFractionRange + " Jmol voxel format version " +  JVXL_VERSION + "\n";
    //0.9e adds color contours for planes and min/max range, contour settings
  }

  public static String jvxlGetFile(MeshDataServer meshDataServer,
                                   JvxlData jvxlData, MeshData meshData,
                                   String[] title, String msg,
                                   boolean includeHeader, int nSurfaces,
                                   String state, String comment) {
    StringBuffer data = new StringBuffer();
    if (includeHeader) {
      String s = jvxlData.jvxlFileHeader
          + (nSurfaces > 0 ? (-nSurfaces) + jvxlData.jvxlExtraLine.substring(2)
              : jvxlData.jvxlExtraLine);
      if (s.indexOf("#JVXL") != 0)
        data.append("#JVXL").append(jvxlData.isXLowToHigh ? "+" : "").append(
            " VERSION ").append(JVXL_VERSION).append("\n");
      data.append(s);
    }
    if ("HEADERONLY".equals(msg))
      return data.toString();
    data.append("# ").append(msg).append('\n');
    if (title != null)
      for (int i = 0; i < title.length; i++)
        data.append("# ").append(title[i]).append('\n');
    data.append(jvxlData.jvxlDefinitionLine + " rendering:" + state).append(
        '\n');

    StringBuffer sb = new StringBuffer();
    if (jvxlData.vertexDataOnly && meshData != null) {
      int[] vertexIdNew = new int[meshData.vertexCount];
      sb.append("<jvxlSurfaceData>\n");
      sb.append(jvxlEncodeTriangleData(meshData.polygonIndexes,
          meshData.polygonCount, vertexIdNew));
      sb.append(jvxlEncodeVertexData(meshDataServer, jvxlData, vertexIdNew,
          meshData.vertices, meshData.vertexValues, meshData.vertexCount,
          meshData.polygonColixes, meshData.polygonCount,
          jvxlData.jvxlColorData.length() > 0));
      sb.append("</jvxlSurfaceData>\n");
    } else if (jvxlData.jvxlPlane == null) {
      //no real point in compressing this unless it's a sign-based coloring
      sb.append(jvxlData.jvxlSurfaceData);
      sb.append(jvxlCompressString(jvxlData.jvxlEdgeData)).append('\n').append(
          jvxlCompressString(jvxlData.jvxlColorData)).append('\n');
    } else {
      sb.append(jvxlCompressString(jvxlData.jvxlColorData)).append('\n');
    }
    int len = sb.length();
    if (len > 0) {
      if (jvxlData.wasCubic && jvxlData.nBytes > 0)
        jvxlData.jvxlCompressionRatio = (int) (((float) jvxlData.nBytes) / len);
      else
        jvxlData.jvxlCompressionRatio = (int) (((float) (jvxlData.nPointsX
            * jvxlData.nPointsY * jvxlData.nPointsZ * 13)) / len);
    }

    data.append(sb);
    if (includeHeader) {
      if (msg != null && !jvxlData.vertexDataOnly)
        data.append("#-------end of jvxl file data-------\n");
      data.append(jvxlData.jvxlInfoLine).append('\n');
      if (jvxlData.vContours != null) {
        data.append("<jvxlContourData>\n");
        jvxlEncodeContourData(jvxlData.vContours, data);
        data.append("</jvxlContourData>\n");
      }
      if (comment != null)
        data.append("<jvxlSurfaceCommand>\n  ").append(comment).append(
            "\n</jvxlSurfaceCommand>\n");
      if (state != null)
        data.append("<jvxlSurfaceState>\n  ").append(state).append(
            "\n</jvxlSurfaceState>\n");
      if (includeHeader)
        data.append("<jvxlFileTitle>\n").append(jvxlData.jvxlFileTitle).append(
            "</jvxlFileTitle>\n");
    }
    return data.toString();
  }

  private static void jvxlEncodeContourData(Vector[] contours, StringBuffer sb) {
    for (int i = 0; i < contours.length; i++) {
      if (contours[i].size() < IsosurfaceMesh.CONTOUR_POINTS)
        continue;
      int nPolygons = ((Integer) contours[i]
          .get(IsosurfaceMesh.CONTOUR_NPOLYGONS)).intValue();
      sb.append("<jvxlContour i=\"" + i + "\"");
      sb.append(" value=\"" + contours[i].get(IsosurfaceMesh.CONTOUR_VALUE)
          + "\"");
      sb.append(" color=\""
          + Escape.escapeColor(((int[]) contours[i]
              .get(IsosurfaceMesh.CONTOUR_COLOR))[0]) + "\"");
      sb.append(" npolygons=\"" + nPolygons + "\"");
      StringBuffer sb1 = new StringBuffer();
      jvxlEncodeBitSet((BitSet) contours[i].get(IsosurfaceMesh.CONTOUR_BITSET),
          nPolygons, sb1);
      sb.append(" data=\"" + contours[i].get(IsosurfaceMesh.CONTOUR_FDATA)
          + "\">\n");
      sb.append(sb1);
      sb.append("</jvxlContour>\n");
    }
  }

  //  to/from ascii-encoded data

  protected static float jvxlFractionFromCharacter(int ich, int base, int range,
                                         float fracOffset) {
    if (ich == base + range)
      return Float.NaN;
    if (ich < base)
      ich = 92; // ! --> \
    float fraction = (ich - base + fracOffset) / range;
    if (fraction < 0f)
      return 0f;
    if (fraction > 1f)
      return 0.999999f;
    //System.out.println("ffc: " + fraction + " <-- " + ich + " " + (char) ich);
    return fraction;
  }

  /* unused here
  float jvxlValueFromCharacter(int ich, float min, float max, int base,
  int range, float fracOffset) {
  float fraction = jvxlFractionFromCharacter(ich, base, range, fracOffset);
  return (max == min ? fraction : min + fraction * (max - min));
  }
  */

  protected static float jvxlValueFromCharacter2(int ich, int ich2, float min, float max,
                                       int base, int range) {
    float fraction = jvxlFractionFromCharacter2(ich, ich2, base, range);
    return (max == min ? fraction : min + fraction * (max - min));
  }

  protected static float jvxlFractionFromCharacter2(int ich1, int ich2, int base,
                                          int range) {
    float fraction = jvxlFractionFromCharacter(ich1, base, range, 0);
    float remains = jvxlFractionFromCharacter(ich2, base, range, 0.5f);
    return fraction + remains / range;
  }

  protected static char jvxlValueAsCharacter(float value, float min, float max, int base,
                                   int range) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    return jvxlFractionAsCharacter(fraction, base, range);
  }

  public static char jvxlFractionAsCharacter(float fraction) {
    return jvxlFractionAsCharacter(fraction, defaultEdgeFractionBase, defaultEdgeFractionRange);  
  }
  
  protected static char jvxlFractionAsCharacter(float fraction, int base, int range) {
    if (fraction > 0.9999f)
      fraction = 0.9999f;
    else if (Float.isNaN(fraction))
      fraction = 1.0001f;
    int ich = (int) (fraction * range + base);
    if (ich < base)
      return (char) base;
    if (ich == 92)
      return 33; // \ --> !
    //if (logCompression)
    //Logger.info("fac: " + fraction + " --> " + ich + " " + (char) ich);
    return (char) ich;
  }

  private static void jvxlAppendCharacter2(float value, float min, float max,
                                           int base, int range,
                                           StringBuffer list1,
                                           StringBuffer list2) {
    float fraction = (min == max ? value : (value - min) / (max - min));
    char ch1 = jvxlFractionAsCharacter(fraction, base, range);
    list1.append(ch1);
    fraction -= jvxlFractionFromCharacter(ch1, base, range, 0);
    list2.append(jvxlFractionAsCharacter(fraction * range, base, range));
  }

  public static void jvxlUpdateSurfaceData(JvxlData jvxlData, float[] vertexValues, int vertexCount, int vertexIncrement, char isNaN) { 
    char[] chars = jvxlData.jvxlEdgeData.toCharArray();
    for (int i = 0, ipt = 0; i < vertexCount; i+= vertexIncrement, ipt++)
      if (Float.isNaN(vertexValues[i]))
          chars[ipt] = isNaN;
    jvxlData.jvxlEdgeData = String.copyValueOf(chars);
  }
  
  public static void jvxlCreateColorData(JvxlData jvxlData, float[] vertexValues) {
    if (vertexValues == null) {
      jvxlData.jvxlColorData = "";
      return;
    }
    boolean writePrecisionColor = jvxlData.isJvxlPrecisionColor;
    boolean doTruncate = jvxlData.isTruncated;
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    float valueBlue = jvxlData.valueMappedToBlue;
    float valueRed = jvxlData.valueMappedToRed;
    int vertexCount = jvxlData.vertexCount;
    float min = jvxlData.mappedDataMin;
    float max = jvxlData.mappedDataMax;
    StringBuffer list1 = new StringBuffer();
    StringBuffer list2 = new StringBuffer();
    for (int i = 0; i < vertexCount; i++) {
      float value = vertexValues[i];
      if (doTruncate)
        value = (value > 0 ? 0.999f : -0.999f);
      if (writePrecisionColor)
        jvxlAppendCharacter2(value, min, max, colorFractionBase,
            colorFractionRange, list1, list2);
      else
        list1.append(jvxlValueAsCharacter(value, valueRed, valueBlue,
            colorFractionBase, colorFractionRange));
    }
    jvxlData.jvxlColorData = list1.append(list2).append('\n').toString();
    jvxlUpdateInfoLines(jvxlData);
  }

  /* ******************************************************************
   * 
   * JVXL 2.0 encoding of vertices, triangles, and vertex values:
   * 
   * <jvxlSurfaceData>
   *   <jvxlTriangleData len="576766" count="89296">
   *     !]][[_]]Y`WbVa^]]] ... cZ_T^ZdUdTc!^[Dv][Bx-43,+44,]-55,f+43,_W`Z^X`Z ...
   *   </jvxlTriangleData>
   *   <jvxlVertexData len="267876" count="44646" min="(15.218472, -28.304049, 34.71112)" max="(97.8228, 54.011948, 109.95208)">
   *     0HY0HZ0HZ0HY0H[0GZ0IZ0IZ0H[0FZ...
   *   </jvxlVertexData>
   *   <jvxlColorData len="37312" count="44646" compressed="1">
   *     015.86++1@<?<D~4 CD2BDDCD*D?BCB?~6 @@.=??CAAC@?~4 A@A?CCD@?@?ABA?>B=<=~4 <====???>>>???,0<<<0/5;:;=><;=<<;,8:;:;=><BAA=?<;+,)*0+/<=<<<<==<~7 =<=<=<====<??<<>>>?=>>??>~5 ?>=>>>===<<<<;<<;;<<<=<<<===><====<==<=~4 <=<9::<<;;;::;;:;;<:;>~4 ;;==<=;=~4 <====>??>==>>>==<=<==<==>=~4 >>>>=>>==?==><~7 ;;;<;<<<;;;;/..0/<;316268<<<;;22:~18 ;:;:<<<=<~9 ;;<;~4 <~6 =<;;<;~4 <<<;<<;<<<;~5 <~16 ;;;;<;:;;:~4 ;:;;;<;;;:;<<<;::;;:;:99;;;:?@>@<<<;==<;::<<;=;<>;:<==>;<;@@>===<<;=AAAA;=~4 ;:=<::><::9::::9:;;;;::::;~4 :;;:;~7 <<;;;<:;::;~5 <;<;~5 ::;9~7 :999;:::;:9~4 ;:;~6 :;;:~4 ;;;:::9;~4 <;<<::999:999:~7 ;;;:::;:<<<:<;<::<;<:::;:~5 9:::99:999::999<<<=<===>~4 ?@?A@A?BABA===>>>==<A>><><>???B>>>BBB?~6 >>BBBB>@AA>~4 ?AB~4 <~5 9~16 :~4 9:~5 9:::9~7 8999:::9:::999:99::9::9~11 ;::::;;99:9::99::9999:9~6 :~5 ;;;988889~5 8889889998:989989~4 8~5 9==>==;<;=>;<====<;>>>=<<<<;;?A?AB<~4 ;=<;ABB<;<~6 ==<=<~4 ;;;;9~7 8989~4 88898~4 989~11 8999989889:::998888::7788789999:8989888898988988988989~31 ;:;<:9888999:;;;;<~6 ;::<<;<:~4 9;;::8887777556878887666787867757775~6 6~4 8789889:99:998999:999899::99:9~23 8898~4 99998~4 989998989767788876776696668766667899777889989999898~4 987778~4 9998989~5 898~4 78668777788767555655688786668998666967888668898~11 66687778~8 78~16 7766686655667688786~14 7766778898899:9:7899995554454~5 5544655456655568~9 9998889899998~4 99888777887~4 8889998766999896859988986666558865886695~4 8888998~5 9887776787~4 878~4 788445444554436666776~6 76~4 7778867788778666886667663~10 433223334444338~14 998~6 99899988898~16 66545888776787865777883333445456343~10 8~10 98~4 989~7 88993~11 878~5 9867766676776~10 333343~7 443~9 76776776633357776554755888878~6 787788877676666765999988878877766668866557563335564554555656577657755756655665664554434564465544467778878~4 7777898798989998889~5 5~5 6~13 776445565756675446766565566446~5 7~5 56665556666565644765588775767686676878~5 78787777886667~5 6~8 78~4 7777887678887869996~7 76~5 7667~6 8~5 78~5 98778887787887~7 ::886828::88988:9::9:88:98888:::88777769768987978889:96777788:::;::;:87789~13 8999777766665~7 6666566555599888898998988:::7787:::99897:889787~4 88887778887998898999-)**,+)*)*()*(+++(***7878889788989998999898777677776777876~4 7665652231~5 4/24656657677<<;<<<<5665656555665~5 4456565~11 6665~4 6655656556665657~17 6655777677554~6 33;4444;<;;4555;;5<;<<;;449;;;<7~7 67776777767~5 6656677657<<<;~4 <<<=<;;<;<~6 =<;5~14 =~16 <===8889999899998~4 99997887877878~7 998888987888798989999;;;9:~6 ;:::989;;;988887788665589886577887778877767~4 6655565789899998787878788788998999:9:~4 988766676~10 565<<<<=~7 <===<<=<====>>=>=:;~4 <<;;;:<::78878777677767~4 8877888787787~5 8~7 7788878878887~5 8777787~4 66788776767667~15 877887988988778978876~6 565~4 66656656;<;;;<<:;:~4 ;;;;<:~5 9:99:9~6 7~10 8~6 7~6 887~10 88878~4 7878866678898~6 6787768668778887777888788878778~21 6~4 5676~4 56878878~8 99888999::9:::9~7 :4~8 54~7 54446458~7 9888898889~14 8998989898~6 9889~4 88999898779889889969899687869969867866556989~7 89888898889~8 8894556568~19 98989~9 4~5 333434~5 55455454445~5 88889988899898988989~7 8~5 99998~8 98~4 988878656787888645~4 675455546778~8 988998884~4 34~17 9~10 889899989~5 89~5 8999989:9::9~7 :9~8 787899989~5 88799989889~4 :9~10 :9:9~4 :9:5598898769~4 688899:566645477767774644;;;;:::;;;;9:::6~4 777::::6~12 ::;:::4456~4 447~5 66664557776669~7 :::9:9~5 :;;:;;:~16 99::9::98:7:999988778988::::9:::99::999:~8 ;777:::8989:~4 98:7:8:;:8;~7 :7778<;<<<;<;~7 <<:<<::;<;;;;<;;<<;~4 <;;<~4 78;;:;:;;9;;:;<;~6 :;:;;:;~11 :::;:;:;~8 :~7 ;;::::9998~4 :::899888:;;::978;:;9999:9;;:<<;::;::::;<:::9<<<:;<<;<;;<;;;;:;;;;:9;;:;;;::;~5 :;;::;~5 ::9;::9:~5 ;::::;;:::;;9:~5 ;:~4 ;~20 :;;::;~6 :::;:;;::;~16 :;;;9::::;:;;;;:;~4 :;;;;:~4 ;~4 :;~6 :~8 ;;;;8988998889:~4 99:9:~7 99:9997789~5 889898899879878899889:;;;:~7 ;:~15 889999:9:9:89779~5 8~4 ::9:::9::9::9:~19 9:999989~5 ::;9~5 ::9::;:9::99:998888;~6 88;;;:9:9:::98;;::9:;9:9999::;;:::;;:~5 ;:99:~4 ;<;;<<:;:<;;<;::::<<<<::<;:::<~4 =:::9<;;<<<;<<<;:;<:;<;~6 ::999989:9:99::9998898:9~17 8887778767668~4 98:~8 ;<~4 ;<<<;<;:;:;;:;;:::99::::98:~7 ;:~5 ;:9;;:;9::9;:;8:9:9::98988899::9888878:~12 9999:~8 7677787676669~24 54~8 5656559:9:9:9:9:::9::;99::9;;;::9999::::;:;~5 :;:::9;::9~8 897798~5 999886667766678::9999898~5 7~4 88:;::99:99:;:5566456654~8 5449~9 ::::;:::;~6 ::;;;888989~7 88899988989989~12 89~7 667767755888776487888855666675644545555664~4 5~5 45~5 ;::;;;;<<<<;<~5 ==<~4 99::;;<<:;;:77998989989898979~4 :~5 9:9:;;::;;;;:<;<66699796999:767668888978998454566655;;;:::;;;;99:9::;;9;;;;8899899998<<<<;;;=<=:;;<<5556555=~8 <=<<====<666656776~6 55664446688998~10 98::9~5 :9:::99:988::;;;;:98889986666788889988669688677789978~5 776778~15 9889~8 ::::88666766766:::;:;;:;65~5 665556~4 56~9 8~18 98988998998899998~8 9999889888988878~5 6666777888688877778~15 998989998889955566555:~5 9999::;;:;:~4 ;;;;<<;;5~4 666565~4 7~8 88877878~15 7~5 87888778~10 7~6 87787766777677788677665567766667755675557~9 87787777566665~8 9::;::::;888899888989878~10 78787878778887778~6 7~10 8~6 7~4 8887888777876667768787666555665~9 787877887787788787~6 875~4 4457888778887~4 878778~4 5~8 4554559~11 8988898888798898~4 9877788889988877789876786::9:~5 7::::9889~4 8:788766999855775~4 9~5 889998998887978889988878~14 756667676577877776677767756666778776785667~4 577675759~13 :~9 99:99::9:9~6 ::::99::999898998889988997998897899889987888879~5 8979899879889~5 8899988989899998988899889~7 78~5 778978~4 76~4 56~8 999897~6 877889:::99978879:99:9999899:99987998889889~5 ::99998:::99:99:~20 7~8 6768787777676776554554664677677766989::::8899::999::99888:::9:~15 9999:99989998~6 :898~4 97887878988898888::9:::99:~16 8~4 :::99998:99:778566786998666:::9:~5 9:99:::98897996678;;;:;::;;:::56545644333343~4 4534:9:9::999:~11 ;;;:;899898999;<;99:99<<<;;:;;<877;<::99:::;:~8 =~4 <<=>====:=~9 <~4 66766559:;<<6;=46789:3334333435633=>~5 ==><;=>~4 ==>>>?=:<;:<;=:;54~12 55455534~12 9:9:;;::9;:::;;<;;<~4 ;;;;>==>==<<:;><~8 :=<<<<===>~7 7~4 87884555764577658799::;9:;988999::9::::668987=><89;;<;::95555657779986887955<==<<89;<;::;97778~5 9?>>???>>?>=>=56556666>>?@A<???@?>???<==<>;~6 <<>>;;;<;;=;?>?>@<;<<;666656~12 566668888:99:999;;:98~4 ;<::9::999::=>=<;;;:8~6 777677878~6 77878776677776777788;:99:;:~6 9:99:8>>>=;<=?>;>~5 ;<;?>?>7~8 666?~9 >?~5 >;;;;<;;?<?<<><<;~4 ?>;;;??@????;;<;;<9~4 889998~6 997878987786:999:999::9998889898889998898:999:9::99::9::99:~5 9:::888:99:::9::88889998899989~7 :9:~4 9:9~7 ::::9:~5 ;;:;;:<;8~5 ::::>><><==?;;:;::::;;<<;7978878889~20 89~6 89999899:99:9:::999:9::99988998889~4 ::999888878888779~5 :99:9~4 ::::9:9998:9:~17 9:::9::;9::;~4 ::88989898~8 9:~4 9:9:~9 99:999::9::::9:;;;:;::;:::;::::;::<;<~4 ;<;<;;;9::99988;;::98<~4 ;;<~4 ::9;9<;;:::99<<<;<<<;;;:;:<<<<;:899;;<<<;:;::<<;;<;;;;::<:::<<;;<<;<;<;;<<;<<<;:99<;:;;;:9:;;;;<;;:;;;;9<9;==>==>~5 =<<=~7 9999===:::<<<;;;9::=<;~4 <::>==>??>?~8 >?>>>>=>?>>?>~6 ==>~6 =>88788878799888>>>>====>>>====>?>>==?:586649568;88967878~8 78889:7:6786777766577:898:=~9 <<=<;;<::::;:<<<<;:;:9;7979::984655796:97877<~4 ====::<<=<<;=<<<<;:::989897778887788867;;:;998:;=~5 ;<~4 ====<<===<~16 =<>~4 ?>>=>?>=<;;;:;<;<=~5 ><>~6 ==>>==>=;<<;:;<<:;=:::?~4 >~6 ?>????>~5 ?>~6 =>~8 =<=<<;=;=><<>~6 =<>><<=>?=<?~5 >>?>?>>>>A@@@?@???>?>@==@>>?@@?>>>>??=<=<<>>====>==><?<=?>>>?<;;<;<<;<<<==<=<==<=<<<=~5 <====<>=~5 <~4 ;<~5 ;;<;~5 :;;;<<=<~5 ;;<~12 ==;<>====>=>=>~8 ??=>>>=~7 ???>>==>==>==????>>>>?====>=>=>>>=~19 <<=<=<<<====<~8 =<==<<<<====>==>=>===<<==>=~4 <=~4 <<=<~4 =<~4 ;<;~7 ::;;:;;<;<;<~5 ===<>=>=><<<<==>>>=>==>~9 ==>====>>=>~5 =>=~14 <=~9 <=~22 >~11 <<<<==>=<<====>====>~7 <~4 ;<<=~4 >>==<<<<=~4 <~5 ==<~12 =<~4 =~4 <<==<=<~12 =~5 ><>===<=<~8 =;;;<<;<;<~16 ===<<<;~8 =~5 ;==;;;<=<<<<=~6 >>>=>===>>=<=<====>~6 ?>>><====<>=~10 <===<~9 =<<<==<=>~12 ?>~4 ===<~5 =~5 <====>>=>=>~4 ;~6 <;;<<<<;;;<<;;<<<DFEDEFFFFGFFE~8 FFE~5 FFFEEFE~6 DDD=~50 <~11 =<=<=~8 <;;;;<<;<;<;~4 <~5 ;;<;;;;<<<;;;==<=<<=<===<=<===<~36 ;<<<===<====<=>>=<<<;<<=>=>>>?>>>>:~7 ;::;:::;<::;::A?@AA@?>A?@@@?@>???@A@?@?C???>>=>=;;:?<<>9=<7:>>>====<===<~14 =DBAC<<B===<:;<<;:?<<?=>8:8<;;<=<9<=<<=<>==<;>=>@@@<<=<~9 ?<>??<?A@?<;>@B9<=?=@>:@B@<<<=<===<~6 ;;<;<;<<<<;9:999::;;<:9;;:=<<<<=>>==>>?<==<=>@>==<=<=<<<====@AA@B@@AAAB@AAAA@<~6 ??>??>?~4 ===<=<<===<~5 ==>>==<<===>>@=<====>?===>==@=>=@>@~4 ?>>?@?@@@?<;<<;;<<=<<<;;<;;=>><<<;<??@>==:<:;;=~4 <=<====>=>===>>>=>=~4 >=>??====??>>?>>===>89<===>~4 :899<>=9989;9;;:>>?A???ABBABB@???>?~11 @A?>?>???>??>~5 ???>~9 ??>?@@????>~7 =>@>~9 ??==>=>=~6 ???@~4 ?AAAA@@AAB@BB@A???====>~5 ?>=>>??@@====@A@???AA??@@@?~4 >???>~4 =>>>=>~4 =~5 <=<==>=<<==>==<<<<==>>???>???>>>>????@~4 ?@@@?@@@AABAB@AA@@?BB@~5 AA@@@?@???AA@~7 A@~6 A@@?@~5 A@?~4 @~6 ?@@@?@@@?@~5 >?~4 @~5 ??@?@??@??>>>===>====>=~12 >=~8 <=<<===>==>==<<>><<=>><>>>==>>>==>>>=>?==?>~8 ?~8 >@???@@=>===>===?@>@@?=>>>>??=>??>==<<=?<~5 ==>????@?>==>???>>>>???>?=>>>@@???@@?>>;9?>>>=:9<>>>>?>>>??>>?><>9::?>?=?=>>>:<<<><>?;7999:8=?<;<?>66;==<==?~5 >A@@A?>@@>@?@@?@@@@???>~4 ?<>>?@CA?@??@@??@@?@@@@?@@>?>>>=>>>>====<<==<<<;<<<;;<;;;<<<;<<;~4 <~5 ;;;::;:9::9~4 ;:>>====<===<<=<<<<?@>?>?>>?@>=?=~6 ?>>?>?===???@?>==?<=~4 <~7 :;:;999:99887789~4 8777899976669988<;<<<:::;;<9998898798768776~4 7:;:;:9888989889~6 8~5 9889888898899988898~8 787~4 66687776776665889:9~5 8999;;::;:~5 9999::;~7 :~4 ;:;:~7 ;<<::::<<<8~5 98~5 98?>?>~6 ====?===?>>>>=>::;;;:9:99:8<<==;:>96=<=~7 <<=~4 >>=>=~4 <==>>><=<~8 =<~8 ==<=<<<=<~4 ==<~4 ==>~5 =~5 <~5 =~15 >=>~5 =>====>==>>>====>~7 =~5 >>>=~4 >>>====;~4 <;;====:;;:;;==<=<<<<;<=<;<=~18 @@@?@?@@?@?<=<<=?===>==>>?=?>>=~4 >=>=>?<?=;<?<>=>==<==>>?>?>??>>>====>=>==>>=~6 >=~7 <<;;;>;;<=<<;=;;===<<=<=~7 >=<=;:7>@?@?<<;<<;<~10 =>==?==?>>?>>>???=99;998~10 :8998;::9;;98<;:=9=:<;8;664687787574325289:<66FFFGFGGGFGF~5 EFFFFEFEF~4 EFCCCEBDGEEFCC@DDBF=~5 <=<<<=<~7 =<~7 ;<<;~6 :;;::<~8 ;;;:9:899;<<:;:<;;;;<;<<<;;<<<;:<<:::;~4 =?>887;<=>;:99::::;:;~10 :;::;~4 79777878E~6 DEE8EEEED;=;9;<<;;<;;67;~5 <<6;<EDDDEDEDEEEED~7 ;;::;::;<;~4 <~7 ;<:;;:;~9 <;~48 <~7 ===<=<~12 =<<<<===;<====<==<<<=<~20 >~8 =>?>=><==>=><<<<=<<<=~4 <<=<>=~4 <~10 >>><<<<==>===><<==<=~5 <<<=<==<~7 =~9 >>>>=>~4 ==>>=>>>=<<===<~4 ===<=<==<=~11 <~5 ===>??>==>>=~10 >=>===<<<===<~6 =<=<<=<<<<>>>=~14 >=~9 <====<====>>?>~4 ?>>??>?>?>>=>>>>=>>>>==>~4 ?>====>>=>~24 ?>???>???>>?>~5 ?>=~4 >=<=<~8 =<=<=<====<<=<<<=<~4 ;=>=>==<==<<=<<<<===<;?=<>=<;;;<=<<==>>=>=>>>><C?C@C@=BABCD@?@@??@???@??@@@@??@???>===><>;<@@@??=>>?~4 =>????<>=??@@?><<<<==<=<==><;:;;<>=~5 >====^__^`_]]]^^]]_^__^^]]]^^^_^~4 babadffcgddfffcdeb^^^```????@?~10 >?~8 !!![!^_^][!]!^!![![[???>?~5 >@??=?=?~4 ><<<>>><;<=<=>>=>=<=>~4 ?>=>~6 kkjkkhhhg!^!_^Y^!Yhfd_`!]gbbeeY[![[!ZYZ!Z[[_!`a`^Y_dfZWWX[kkjjjkhjgjjdjjedkkkk_[a[aXXWWWX[cdicXVXVYYVXrrrsposlkkkkjkkkikilkmknlh_^^]lgmhb`eaoonqlYZ[XVYY[Z[Y[[[[Z]^][[]!^!]YYYXXXYXYYYWXXVWXWXZXSSVRSRRRSRSRRVSVZXV<~7 ;~7 <;<;;<<;<~4 ;<~14 99::9::<<;;<;;<:~6 <<<<;<;~4 <<:9:9:9:;;;:::98899<~22 ;;<;;<<<8887~4 877877;:9999:<<<<;<<<;<;>~5 ?>????9889????:=:>>899788<?>????>?;<<<<>>>>=<<==<<=>>?>>>>=<=<>>;<<;==<=;<====;==ECEC???BB;=<HIJ@EE88:9878:3443333654545444455432553:845545~4 :89955588798554544456<<B?89=56=?=<<<<:<<<<;<~4 ;<~6 ;~5 <;<4636.333387867767741354655623665564044547~6 8777865=>>>??>?=;:;;<<=<=;;;<;EGFHGHELLKKAGENK7679HDI86NONDC567::;::::;::<9:?~8 <GHIGGHIIHGGGHHIEIGH204D364=HIIJI3<H./KFGFFFIGGG--//0;:;:;;;;:::;:~4 ^_^a]cd`alllkleeflkihli;::99:aagg9f``b^]]]][^!::9999fffc]a_a_`zyyzrr{twwzy{|zzkklkklkjkikwqppvtli^^iWWVVX`Wj[[]Z[[[[Z[f`WXvxWWfbZYY![]]]!]!]![~5 Z[[[ZZ]^~9 _[[^^^__^^ZWYXXYYZXYWYWXXYX[:;;;:;;;:;;9:;~4 :;::;;:;;::::=>=:;::<==>;:;>=<=<=;====<>>==>>>??>=??>A~5 @AAAA@~4 AAAA@@A@AAAA@@AA@@A????>?>>>=>>>>?>>>A@@A@@??A===>>A@?@A@@?A?~4 @?>?@???@@??A~5 @@@A@?A@??@@@A~5 @A@@@@???>>>?~6 ;<<;<~5 ;<<>=<=<;>?@??@?@@?@?@@??@@??>??@???>~6 ?>?>>?=~4 <<===>>???>>=>>=?~5 =>===<=<<<>=>=>=<<=>>>==<<=~4 >>=~8 >>==>>>>?>??@@????@?>===<~13 ;<<<;~8 <<<=<=<<<===<<<===<=<=<~16 =<=<=<~5 =~4 <=>=>?>;;<~4 =<<;;;;<<<<;;;<~4 =;~4 <<<;;;;<~9 ==<=<<;=>=?==>>=<<==<==<====>>>=>>=>~4 ??>>??>~7 ====?>>>>??>>>>??@@?~4 @????>=~4 <<=<;~6 <;<<:;;;;<<>;=9<<<?=<<<<;=~14 <=<<=~11 <===<=;===>>>==<=~5 <=>=>====<<<<==<<=<<<===<<<=~6 <=~6 <=>=<<<=;===<=~7 <~12 =<====<==>=~6 <=~5 DA@DC<=?FDCEDHJJH=~11 <==CEEBGIBH<~19 =<=<=~4 <=>BC=~4 ><<===<E<CJKIGHBJ====<=<==<<<===<<<==;;<~11 =<~9 ;~7 <;~12 <;<<<=>====;<>>>?=::9:;;;;:::9<<<9=;;;<~6 =<=<<<<;;<;;<;<;~7 <;~6 <<;~5 <<;;;<;~53 :;~10 <;;;<~6 ;;<;;<;;;;:::<:~4 <~4 ;<<;~5 <<<;~8 :~5 ;<;;;<<<;;;;<~17 =<<;;9::;;:;;;<;;;::9998;:;99889:98888;;:::9<;;;;GGIFJHLLBCLFHI<CA==<===<<<<===<=><=<>>=<=~12 <====<<<=~7 <;<<==;<;:=:::;~4 <<:;:~4 8:9988977===<;<;hggjhrnotvnsiqvfhffeefjyxyhghxyxtyzxy|xtmpxxxefcccdecbcbccddgffbabccabadcccd|zz{~7 zz{{zz{wz|zzz{{zyyyzwywwwsttrrnzxwssssnllmmvqpR~13 QRRQRQRQRRPRQQQRQQrwnvxxruuolsjihivgebaa```gb```cdl```bbb`bi`^_`elfnahae`p^_^^_^skoressnajbga`b_e?>>>??>~5 ?>>=IMIK:;:~7 ;:~4 ;;;9~5 :999:9:~9 ;<~5 ;;;<;;:9:~13 9999888899998899889~4 ;~7 <;;<~4 ;;<<;~10 :~5 ;<<;;;:::;:::;~10 <<<<;;<;<<<<;;:;:9<=<;<~4 =<<<===9:9:889::;;<;<99=;98:<<===<<;<;;;;:<=:<~8 ;;;9~5 :99:;;;;:;99<~5 ;<;<;;<9988977778788865767::8998:89==<=<<=<==<==<==<=<~8 ;;<<<<:;:;;;;<<<;;;<;:::=~11 <===<~12 =<=<<<<=<=<~14 =<=<;<;<;~7 =~6 <<=;;=<<=<==;<<=;=~4 >=>><<===<<=~10 >=>>=~6 >=>>=;<<<=<<<====>>==>??@?>>>??>>?@?>><~5 >==>??>=>>>>?>?>::;~6 <;~10 <~8 ;<;~5 <<;<<<<;;<;~5 :::;::;~13 <;;<;;<==;::;<;::<~6 ;99998899889;;:;:;;<:;:;99;:;;9:;:;:;;<;<<;;;;<<;<;~4 88898899;;<<;~4 <<<<;<;<~5 =<<=<;<<<==<=<<==@>??;==<??<<<<>==>>>;;>>=<=<;<=><9;;:9:;;;;<~4 ;~5 ::::;;:::9::9898::::999;898878989;9:9988::87887878778888::::;:89::::9:<==<<<=<9::999:99::;::=<>>=>>><;>>><~4 :::;;<<;::::;<;;;:~5 ;::<;<=>==<<;<;:==<<;;<<<=~4 ;;<<;<<<;;:<=<==<;>=>>;;;==<==<>~5 ====>==<=~4 <=~4 <<<=<~5 ;=~10 <<=~6 <===>>>==>=~15 >~5 =?>===>==>>>=>>=>===>>>=~10 >:;~9 :;~4 ::;~5 <<<<=<;<<<<;<;<==;;<<<<;;>>>>?>>>><<<>=<=<<<;>>>=<>>:<>>=9::;;::;9=;<<;::;==9:::<;;::=::::>=>~4 8998999899:88787889999:9999:999:~12 ;~5 ::::;;::;:~5 ;<==<<=~5 <;~5 =<<<=~11 <;<<;;;]aa]_a_ba^eeakljlihkllkkkgffeed<<>>>=;<<;~4 <::Y!Z!Z_]ZYX^`db^!!ff[cccbbababcbcb_`bc[[Wdb^[]Z[]`dXW[eaYXVYehgdextsdccccnm{xystuw{wsf[XeXW_Wjk_XZmk7~4 :;::::88::9:999acb_`_`^^a_accba__]bca````a``ab``<=<@@@```_``_^_`]^^==<==5657989<6>:>:=443444756><<=D?>??>>>>???>>>??>~4 ==>=>=>>>===BBADCG====>=>?>>====>~18 EEDFEGGE~19 DDEDEDE>~8 5453552256;5986A:<;<<77767;::;99665569797;::7656<<;<:;=>====:9;9:66:967577888989766555656687677768978<;99;;<:;9::;~9 :787;;99;77;:977;;;<~8 ===<<==;;==>=>=>=>====<=<;==>=>=;;;<~5 ;<<;;<<<;;;<;~4 <<<;;;<;~13 <;;<<;:;9;9:::;<74;;22487;<;<<;<<;<;<~5 ;:;;::;~5 :;;9:::9~7 ::;999:9:;;998;;:99:9999778788789889::99898~10 778777688758877887675656777<~6 ===<=<~10 ===<~5 ;~4 :;::;:;;:9===<<<=~5 <~6 =<<=~7 <=<~5 ;<;;<~4 ==<=<====<<<::<<<;;<=<~6 :<;:9~6 <<<;;;<<;<<<;:;;::;;;;:::99;99888;;::9:<<;~8 <<;~9 :;;9~5 8887~4 677667678898:;;;:;~13 :<:?;:~5 ;9::;;;;<;;;999;9:::877887898978:~5 9::98:966677887777668777A@;<BF8~7 56679887~4 889877998998:::89~4 77;;;9:;97878~5 ;;;;99:999:999987::;>989:;::::9>?@@A9~17 <<<::;<::;<:9~6 =<<<;<<<;::;:;9<;;:9;:;:;;<<>==>><<<>===><QQQQRQQRRQPQQQP~8 ==>>==>=~7 @@@A@A@A@@@AA>>>>?@@?~4 @?>~8 ?=>>>=>?>???@@@?@???>?@DDDCDDDD?@?@CCCADDD@@???AA?@AAACABDDEEEDECAAA@@????C???GGGEFHHHFHEFGFFFGFHFHGHHHGG?~4 @@>~4 ?>??BBDB>??>~8 =>~4 ?===>>>>==>~5 AA??@>>@ABBA@@B?@?@??>A@?A?>>@@???A~5 >@?~4 @?>>@??@@@@?@@??@~5 A??@~4 >>?>~9 ==>@@@@>???>?@>?>>>====>=>=<=<~5 ===>>=>>==>>><<>;>=><<<;;==<=<<===<~5 ====;;;:::;;;::;<<;:=<;::;<;=~4 >=~8 ??@@===>>>@=@@@==?==A~4 BA][]XXZZVVXYVUUWUVVUZ[Z![]^_^^_^_^^_^^ZVTSRSTSSTSSSR<~8 ;;RROOQOKHHLJFRQOQOPOQFGGGFFGGHGFHFFFFHFHLKL8~13 77877878~6 <<;<;<;;;;8~4 <<;;<899::;;:::;:98888998;;<;:998:;?@@@>?>>??>>?>?>>?888?>>>8~6 =>>===9:;8:8~6 ;=:89;98~4 ?==<9;<<;98?@???>><>~4 =~4 <<;<>>>===<8888;;<;88989:::9<9:99:~4 ;:;:<:::;<:8:9::::8~6 >~6 =>>>887888877779899878778:7;99:::9998~4 77676~5 :;::;:::>~8 =~4 <<===<<<<;;<:~4 ;:;;99::9<=;:89889ONNPNQQPOPPQPQPOOIHHJJIPOPPLLLMNLKOHGHLFHEFL:~7 ;~6 :~5 9::9;<~6 ??@?~8 >?~32 >?~10 @@@?==<>;<;;<<<;>=;;;<=>=?E~11 D~4 ==<=<;::<;::;;<=;;;;:;;;<;<~9 =<<<<==<<<;<<<<;~4 <<;<~21 =<<=<;;<;<;;<;<<<<;<~9 ===<====<==<<;;<<;<====<~21 ;;;;<;<<<;~4 <;;<;;;===<<====<<<<==;<<;==<=<~13 ;~19 <~5 ;;;;=;;<~4 ==<<=;===<~8 ===<~4 =<~11 ;;<=;<~4 ====<<====<==<~6 =<<===>=<<=~5 >=~4 >>=>?>==>>??>??=>>>==>==?>@?>@>~11 =>==<<==<????@??>????@@@@>>????@???><~5 ;;<~5 ;;;;<;<<;<;~6 :~7 <<<<;:~10 ;:;<<;:~10 9~5 8~4 9989988:;<;:::HHIHIIIGFHIIIHIGFHGIFFF>?JI?>?JJJGG?>FFFH?>=OPOOOOKMMKJLNONKPPOPILIJ???>>?>????@>=~5 >@==@<A>=<~4 ===>>=>=?>=>><<=<>>>?>>A@A@CC?~4 @@@?@?ABC@>>?A@BACDEAAB~5 ADQQPRRPPOPMKKMNORPRRRIIJIHPQPPONMMLHKHHHHA@@A~4 ?AA=>>>=>@@@B@@@?@?BA@???@@>@@@>@>>===>>>=>>=~10 <<<<=~5 >=>??>@@@=>=>>?>>>?>>@@@@????@>=>??=>???@?~5 @=?==<<<@?>?>?@@<@@>>>??>???>?~7 >A@@@?~4 @??@~8 ?@@@??@?@?@@??>?~6 >?>@~9 ?@?@@?@?~5 >?~7 >~6 ?>?>????>>??>~4 =>>====>??>=~5 ????>~5 ?>>?=>>?~8 >>>??>AAA@?A@AAA@=@@>=><=~4 >?====<===>=>>;<;;<;=<<<=;;;=~14 ;;;<<;<;~6 :~5 ??>?>=@@@>==>=@===?@>>>=>=<<====>~5 @~4 ??>>@@>@>?~7 >>>?>??@@??@????@BB?@?>?>@B@~4 A@??>??@=~6 ;<~5 >>>>=<=<=;;<===<===<:;;;<;9;9;=<;=89:9:9;:;7776667755668568778888=<<;<;::999:9:;9:999;88788;<<;:~4 ;::;:98:~6 7877878:::;;;:::99:885799<:<<<;:;:===<=<=;;<;<<<<;~4 :~4 898~4 ::9:99:::89:9999899<===<<<;;<~5 ;;:<:9:<::9998998~5 98878778877<<<;<;99::;;;;::98~5 96~6 99777766887798978787688878~4 7878998888989:9:;88:9898877768778~6 78876677677767666868~4 9~6 ::99:9;9;;:::;;99:~5 ;:9::::99:;;;@~9 ?@?@A~8 @B~4 ABB?@@ABBAA>A@AAAA=~5 >>A=>AA===@@==><=~5 <<>>?>>=~4 ?@??>====>>?>>>@A????@A?><@??@AAA<BAB@?@AA@?;?<=B<BBBB<<<BABAAA@AAAAB@@>?=><<<<==>?@<<<<@?@<<<<>><~5 ;<<;~4 <~5 =<=;==<;=~4 <;::;:<~13 ?@~4 ?AA@;:;::<<<::<<;:;:~6 99@AA@~6 AA@~5 A@@@@A@@@B~4 ABDCAAABABBCBA@>>AAA@>@AAA<=<@99::999:::9:@~4 A@~5 AA>?>?CBBCABB@A@AABB:;;:::<<<;;:~4 ;;;<;=;<=~8 <>>>=>>===>>>=>>==>>>=>>????>>>>????>=>?<;<<<=;==;<;;<<<<;;;;=;;;:;:~4 ?>?<<<?~7 =>=<=>??<?==>>><>~4 A~7 BAAAA<<;;<<=;=BABA==AAA==BB=~4 AAA@A==BCBB:;;BBBCBB:::;BBBB;<;AA=<B<;=AA@@A@@???>=>A>>>>?>>?=<===>=?B@@@?AA>??@??@???>~7 ====@~5 ??>==@@=>@>>>=~10 >>>=>>=>=~37 >>=>??>~5 ?>?>>??>?>>=~4 >==>>>==>=~6 A~5 @A~6 @????@@?>?>>?=@@A??@@A=~5 >>?===>=>>===>~5 =>>=>~11 ==>==>~5 =>>=~7 >=>>=~15 <=~4 <=<===<<==<==<<<=~7 <=<=~15 <===<~5 =<<<=<<==<<=~8 <=<<<<=<<<<====<==>>=~6 <~8 =<<>>>=~4 <<<<=~5 ;<<;;;<;;;<~15 =<<<<;<;;=~6 <>>===<=~6 >~19 ==>>;~11 <~34 >~7 <=~4 <==<<<>>=<<====><<;==<=<;<=~29 >=~5 <<====>=<=<>=>=<~4 ;;<;<=~21 <=<<<<===<<<=<=<<<=~5 >===>>=~12 >=~7 <<>~22 =~17 <<=~11 <=<<==<====<===<<<<=<<=<====<=<<<<==LMLLLLKIJLLLJK=>=~6 >=~5 >>===IHKJGJHKLLLLM===>~7 KFJKJJHFJJ>=~10 <=<<=<==EFFFFHFHFIHFHE?DCA<===?<<=CDCC<=~6 FGGBD;;;;DGFE;<;;=H;=>>CH<=<~4 FHHIEIG=<==<~4 ==<=<>=~4 <<<<==>~8 ====>==<=~6 <=EE>===E@;<~5 ;~4 <<==<;<<<;;<<<==<=<==<=<==<<=<=<~6 ====<<=<====A?@@@C??=>?>?>=?<<==<=<===>>=~4 <=~4 >>?>>??<<<<=~4 <<=???>>???===<=<EEDEEDDDDEDEDDCD;~7 <<<<;;<;;;;::;;:;;::;;;::;~4 <;=FBFBHHHG=>=>>=<<<=IKID;<=GJJJ?A@CB?>>@?C@?><;<;<<<<====>;=<==>;;;>;~7 ====<=<=~6 <<<=<<<===<<>=~10 >=<====>=<<><~5 ;=<=<=~4 <<<;;;;<;~5 <<;9999:::9::;<<<=<<<;=<<<===<~5 >>=>~4 :::<:;:;:9~11 ;<<=:<;====:<=~9 <<=~6 >>=~5 >>>=::8:999;;;999;:;;<~7 =;;:::;;:<<:~4 ;~4 <~4 :<<<:::;::;<==>==<<>=>;=>>>==<<<<=<=<=<<<<=<~4 =<<<==<=~4 >DFFDEHEKJGKIIJIGILKKKMCKKKLJKKKII?AA><<?B=<=<ACD==<=<=KLKLKMJJ=~11 >=>~4 =>>=>~6 ==>==>==>>==>>>=~4 >~4 =~4 <==<=<=<=~4 <<<<==<=<<=;<;;=~18 >==>=~10 >>=~10 >=~4 <=<==<====<<<;;;;<;=;9~10 ::::;;9999::9;~8 :9::;;:<=999899:;:::;::;<<;=;<<<;:~8 ;~4 ::;9:999:98889:;;;;::;;:;8787788997~4 99::9877888968867776788898899989997798887;<;~7 <;;<<<===<<==<<===<<=<<=~4 <===<<<==<<<===<===>=~8 :<9<;<:<<<<==<;;;;::899:;9998~4 JGGLD>F9;=>?@46<@E>>=F>===>=~4 ::<<;<;<<=>>>>=>>>=<==;=>>=CC;6@8=6DA7:98798;98?~5 @CAJDG@CLD?8?::?55<6@A6675@95553434558C9C;74755566665~7 DQ77:9NNOFM::9:99<?~9 >~4 =~4 ????>???>>=>====HLKJ=~5 AB<<<=<<<=<<====<===<====<==;;<;=:;;;:<<=<==<<=<;<<GEEEGDDED<=<<==>==<=>>==<~5 =<<=~5 <==CF>FCF<=<<=<~8 =<<<>>=<==<<?<<<<=<<<@?B@??@??@?@~4 =~4 <<<==<~6 =<~6 ===<~6 :;;;<~5 ;;<;;<==::::<~4 ;;<~5 ;;<<;~4 <;;<;~7 <<;~10 :;:;;;;:~4 ;:;<;;<;~9 :;~4 A>?;;;;@?;~7 ?>??@<;<;~4 <;~8 <;;;<;;<~4 ;<;<;~10 <;;<;~26 <;~5 <~4 ;~4 <~5 ==<==<<<<=<=<<<==;<~4 =<<<<;;<<;;=~4 <~4 =<~20 ===<====<=~7 <<<;;<<;;==;~4 <<<<===<~4 =;~10 <<<;<~10 =<<<=<==;<;<;;<<;;<;~11 <;:::;<<;;<<<==<;<=:;=<:<;<~4 ;;=<~4 ==<<<=>>=<<=<=<~6 ====<<<<===;;:;<<;;;:;:<:::<;:=>=~8 <=<=~7 <===<==<<<=<<==<>==<<<>>>=~8 >=<<>=>==>>=>=>>>>=~10 >>;<<;<;;==>=~7 >=<<;<=<;~6 >~13 ====<<====<=~12 <=~26 >>?>?>===<===>??>=>>>=~5 >>>>=>==EEEDEDDDC~5 DDCCCDCCDDDDCDCD~7 EBAABBABA~4 BA~12 @?@@A@AA?@AAAA@AA@?@A?@>?>>???@@?A@?@??>?>>?>>>??>~6 b~26 acaabcbbaaabbbabbbbaA~7 @A@A~6 WWWWVWXVWYXXWWXWWTUWUXWXWY@A@@@@AA@AABAAA?A??@A~5 @@@AXXXYXWZYhhgih[]!]eefcdeefcdca`dde`bcT[T^^XZUWZXZ[Y[Xcdd^]_ZYWWZmlkffmmhjeWVWW!!]bb`UUTUWZTTVV[^!]ZZ!Zcc`ccdddffihi[!jllkikkjkic`b_Zj9==:?<<@<?:<::;::<::?>~6 =>~5 <=<<=>=>><<==>>>=<???>==<>~5 ???>>>>???9:99=<====;;<==<:999==<99:;8<=<=;::;=>;:>>>???>=<<><=>?<<=?>;>~10 =>~10 =>====<<=>==<<<=<<<<;<;~6 >====;:;====<<<;~5 <<;:<;===>>=>====>~4 =~6 >====>?~5 >>=???==?~10 =>~5 ?>~10 ??>A??A?>>?>?~5 ==>=>~5 =>>>=;~6 <<;<~5 =>?~4 A@@@?@????@@?@A~4 BA@SSU?BBWSVZPPP?BEKXQPNNONNPQP>ONNNJOMMMJKQQPN~4 B>@@>>>=~4 >C==?PQ>=F>====HDLC?>=>>>>???@B@=@<<<==<<=<<==<===>>====<?>>><~8 >>>>@==??<==???>==<<<=<<====<>>>=????=<<<?>>=~11 <~4 ;<=;===<<;:::;~9 ::;:;<=<=<<<;<>>>>=<==>;<=;<=>?:~6 ;~11 <;<~4 =;~5 =<=<;<;;;<~6 ;:;<~9 ;<~8 ;<~9 :;::;:;;::::;:<<;;:~5 ;;9:;<<===<<<==<<<;;;:::;<;9;:;::=~5 <=~6 :::====>>====chbh_``bcmnmnhlnl]]]]b]^!]]bklhklkglll=~6 >>>==?>???>==bb_fg[^[T]mmgpmlm]mngbebdcfebaaVTVTSYTUX!!Y!W]VV!`[YYTXa^_aVU_ZZZgfefeebRUQURUWWUT^baWUUU]bdcTTY]WVVVbbbaddecdeecdcd@=<A?=<=<=<=>><<<;;:;;<<<;;;:;;;<=<~5 ===<<===<===>>>=~8 ;<~5 =<=<~4 =<~5 =<==<<<<=>=>=<=<==<~6 ;<~16 JJLJHKLLLLKLMMLLKLLKHKNNNPOLI>?>????;<<>>?===;~7 <~14 ===<~16 ;?>??>~5 ???>>>><~4 =~8 <>~4 ?>><==>===<=<<<==>>>?==<=>>=<<?>???<~4 ???>?>?>>A@A@???@~4 ;=<<<==>>?=;>=<?;<;;=;;BA@>=AABBAA<<<A87666989::;:8;<;:;:9:KGIIEFGLGHKNKJ>3HL1BE<6CO47657999:;;5699986897:660116575224<;;:~6 ;;<;~5 :::;:;<~6 ;;;<<;;;<<<;<;;<;;;<<<<;~4 :;::<;~5 <;;;<<;;=<=<<===:;=;:;:::;::=;::>><===>>>==>>>?>??A?>?AAA>?>A>>>?>?AAA;;:;HFEG[!YWZ!XWUYUWVWUYXWWVWUY!XWWWWRRRSSQSRSS^~8 99:~17 ;::;::9:9786658666;596857996<8<7989~4 :97899778876997=~13 :~10 ;:;~13 :;;::99;:::778;8~4 788:;8989~9 89:78~4 586885:5698;6677888766588>>>><<<;==<;><;<=><;=~4 <;<;<=;;<=>>===<<<<=<<<<;<~20 ===<~28 ;~8 <;;;9:9898999::999:~5 9~4 :9:::;<;<<;<<<<;;;<<;;<;<<;<<=<=<<=~5 <<<<=<===<==<<<<=<~8 ;;;:;::;:;:;::;:;;;<<;:;~5 <;<<;<<=<<;;;<~15 ;<<<;;<;<;<;=<<<;;<<<:<:;:<<;<<nopllkjkhikjlmlprrpppqnqoorppmkknnmghjhhjfffgh~4 ileefddeh~4 ei:~9 srsrptmrptqqpowwtejflllhmunlffdhfjsvvrlrkknmnvvlehfdckgfmmdljfjmllkmqpqlmkmussutturihjffigfgffikleffefjkefedccdddcghflpelmkjkngkbbccbcbcbbccbbbbcifdcjffe77888878:9:;97:<;858768949~7 59999859795=~4 <<==<=~8 E~16 DDEDE~11 99:98868:7;;<;<<;~9 <;<=<<;;;;<888799989;~4 ::;~8 :::;:;;;;:;:~4 <;~4 ::;;:;::;;:;:<<<;<<;;<<<;<<<;<<<<=<<==<<<;~9 <<;<;;;<;;=:::9999<;;9;<<<=<<;<<::;9;;;999<==<;;;;=;<;;;<;;<~6 =<<<=<~4 ==<;=;;:;;::9;~4 :<9:=;<;;:;;9;;<<<<====<<<;;=;;===;~10 <<<;;;=<~11 ;<<==<~6 =~7 <~6 ;<<;;<~16 ;;<<<;<<<:;<:<<===<;;;<~4 ;<;~9 <<<=<<<==;<==;<=<=<<=<<==<==<==<=~9 <=~7 <====>><=<<=<=~8 <=>>>=~4 >=~4 >====>===????=<<=>>=><===>=><==<====;==;<==>~5 ?>~9 ?>;<~4 ;;;;<;;;<;;;<<====<=<<<=<~11 =<<<<=<<<;;;;<;;<;;;:;~7 <~6 ;<<==;<<<>==::;;;:==;;;;>>=;=<==<<=;:::;~7 :;~7 <:<;;<;;;<~5 ===<<;;;<~5 ;;;<;<>???@@@?@@@@?AA??>@<~5 >>>>=<<=><<<<=>?=>=><~5 :::;;;::<;~7 <<<<;;;:;;;;<~6 ;::;<;<;;<<<;;;;<<<<;~4 <<<<==<<<;:<;=<<<>>?@>??A@AA@?>??==<<?>??=???<=<=~4 <=<;<~4 =><<<=<<<<====>?>>=~34 >>>>=>>>=>>===>>>==>>>==>=~4 <<==<;==<=~7 ;~4 >~5 ====>>==>=>=>~10 ===>==>>=>~11 ==>~5 =>~7 =>>==>~4 =>==>=>=~8 >~4 =A?A@@@???>?=@A@@?@@>>>?@?>=>>?==>?><>=~4 >=~5 >===<<<=~8 <<===>~8 =>==>~4 ?>>?>~6 =~4 >>>=>??A@@AABAB@@@?>>CABBA@B?BB??B~4 =<==<<<===<=<=<~8 ;<;;;<;;<<<=>=>>>==<><=<<=~7 <=~12 <=~6 A@AAB?B^h]gfBf^_fbXXZ!YXZYYBBkkjj!C?@????efhnnkmkmlnlkllk??>^d]VZ[WVX_eXZ[kjkUYVTTTRQcddddecccddddcd`cacc^^VWaaVTWacca!ccbbU!VVTTUTYYXUWWXbXWecRc`TUVUafj_XVkiffdblXnmoUTTUh`befmXompoek]k<=>=ebcbceecdbbbfabhhhghfhgddA~4 DDFEEDDCCDDDDCCDDDEDE~4 DDhfececfcbdbbchgfd<<<<==<<<==<E~4 FE~11 DEEEDDDE~6 ::;:~4 99989989:979::9:988;<<>;B?;>>:<:;>>=>>>>=>>=~12 ><=>>====<==<;;====<~6 ;;<;<;;;<<<;;;<;~5 @<AAEEADDAAA<C::99:8998:<999889~4 :9;:;;==:9:8:9<<<<=<<:::<<<=:~5 ;:;<;::;;;:99;;<;;9999<:9;:::9:~5 9~4 ;:;<<<:;:;;<;99:999::9:9999:~5 999:;~10 9:9;;;=<<==<~8 =<=<;<<<<;<~4 ;<<<;<<:;;:;::<;~6 <<;;<<;~10 <<;;;;<<;~21 =><;?;~6 >;?;;<<<;<::<<<<;<<<;;;<<;:<~9 ;;;<;;<;~4 <;;;;898;:;;<<<<;:;;9898:::;9;9;;;::;99::;;;::;;9:;<<;;;;:~5 ;:~4 ;::;:::;::;;:::9:~6 99:;:~9 ;;;;<~12 ===<<<=:;9:9::;::;:9;;;999;<;<~8 ===;;;<<<<=<~5 ==<<=<~5 =<~9 =<;<;<<::;<;~5 :;<<<<:;;:9999;::9:;;:~9 ;::;;:~5 ;:~4 9:::;;<;;;::;~7 :;;;:;;:~6 <<<<;~4 <;;<;;;;<~4 ;;;:9;~4 :::99;<<;<<;<;:~5 ;::::>>;;?;:~7 <;~5 :;;;;:;~9 :~9 ;;;;:::;::;:::;~4 ::;;<<<:;~4 :;~15 <;;;;<<<<;<;<;~4 :;:~4 9:9999F<?:;=;<9999:::C=99::BECDB:;~5 <<<;;;::;~5 <<;:<;;;9:::;;;;<~4 ==;<>><<=<;;<~5 ==>====<<=<=<=~5 <<<::;:;;>??@C=~5 ;~4 :;:;;:;;;==<;<;<;~5 <<<<;;<;;<~4 ;;<<;;<~4 D~8 CDEDDEDDDC~4 DCCDDDDCDD67657776676669~13 4633300-..10/31.769955566715157599555342:0812.:;;-,12,;;;;<;;-6,;;9;<=>=>::>><=>;:<>><<<>~6 ==>.0-9~10 :9::9999::99:~5 ;;:::17/73:9~5 <<:<<99;:;;9::9<<<;;;::<~10 =<=>>=>=<==<==<;<;::;<;:<::;:<<9:98;99<98<9;<9<9:9~4 :99:9:9~4 :<<<<=<<====<=~8 <<<=~4 <<=<==<=<<<;;<;;;;<<<;:;:<<;;;<;<::<~4 ;==<<=9::98999:999::;9=~8 99::;:;998:999;<===;:<<;=;::<<===<=~4 <<<<::;:99:99:::99;;;:::98899;:;::9999:;<998~4 9999877988778799977:98979:;;;::::;;;;<:;;=<<;;<<<<=<<==<=<=<=~5 <=<>==;<;;<;<<999::::<:::;:;<<<9999:;:;;::<:<99<:<>?~4 >?=~7 >>>>?=~5 >?A~13 @AA@A@~6 AA@~5 A~5 @AAA@A~7 @AAA?>?>><=?===>><<<>>>====<=<==<~6 ===<;<<;<=>=><>>><<=<<;=<><<>;:;:;:;;;:;=~8 <<==<<=<<<=<==<<<<=<<<==<~12 =~12 <~13 =<<<<=<<====<=~4 <<<<=<<=<<<<==<<<=<=<~10 =<<==>=>===>=>>=<?<=?~4 =~4 <==<~9 =<=~8 ><=>><<==>>=>=>>>?>>==>?>>>====>>===>>=>>=~11 <<<=<<==<<====>>>=>==>~9 ===<===<==>=~10 >==>???>>@?>?>@A@~4 AA@@AAA??@?>>>A>~6 =<>=>>>>==>>=>A~10 BA@A@AABBACCABCC?~5 >~4 ?>>>?>>?>=~6 <===<===>===<<=?>~6 ?>=~5 >=>=~9 >=>~4 =>~4 =>>=~9 <<<<;;;<:77788788:;889889;:9789777=><;<>>=>==>===>><<<;<;<777;:7~6 12:;;85+..98889:87988872/*0..7~5 17~9 337789899978,.))(672655===34<<<9<22<4=2-==>===<6===>=>>>>=~5 ;<<;<<;<<;;<;;:;::;;;:<<=~4 <=99:9<9:;8~5 9999:8;:;;;<:::;<;:676~8 7~12 879888978777787778777789<<<;;<=<;?>?AAB>>?~4 BD>>>?>?==>?=>>==>=?==<<>;<;:;~4 :;~5 :~6 9:9:;;:~14 ;;;:::;;9<;<=<<;<;;;;<;;<;<;;<=<==<=;~7 <~4 =<;;;<;<<;;;<=9~7 88898:987:88988877788798899464767~4 676654778777755644445664556~7 44546~9 767~4 6~8 766676666877787888778787888878787~5 8~4 999899888998:~11 9989:::9:::9:9:9:~6 9:~5 ;;:;;:;::;;:;~6 :::<;<<<;:~9 ;;:;;;:~4 9778~4 :::88::9899:9::9::9:9777789899989766678877877588887547777556676645778~4 77778~9 98888989~5 7787777886566756678668545556666455676776889899998~5 798~5 99::::97~4 8889~4 ::88998888778779898999:;~6 ::;9::99;~7 ::;;;:;:::;~7 A@A~4 @~5 AAA@AA@A~6 @@AA@ABBA@A@@@A@~4 AA@A@@@>~18 BBBCCBBBCBBAAABBBADDDEDB@~4 BB@BAA@BBBB@@@AA@@CC@@@?~6 @~7 ??>~4 =>=>~8 @>~18 ????>>?>~20 ?>????>@~6 >>>?=>==@???>?>=~4 >>=~4 >>===>>==>=~17 @ABBBA@B~32 ?BA@??AB>B~4 >===>>?>?=?~4 >??>??B~9 @?@??@A~4 BA?A@B@???@@ABBBB????@@BBBA?~9 >?>?>~4 ????>==>=<=<=~4 >><~4 ===>~8 ==>>>?>?>>=>===>??>>===>==<=~6 <=?~8 >?AB?@@AA???A????@@?@?@@?@?@@A@@A@@@@????@BB@B???@@??@@?~11 @@?~4 @@?@???@?~11 >@=?~8 @@@@?~13 @@?@@>?~5 >?>?<<<<>===<<<>=>=<=>>>>==<=>~8 ??>??>?@@?~4 >???@????@??@@@@A@~5 ?~4 @????@??@???@????@?>?>?@??>>????>>?~7 >>>=>=>~16 ?>~9 ?>~9 =>>===>~7 =>~7 @?@?~4 >>>?==<<=<<<>==<<<???>>>===<=>>>=><<<<;==<;=<=>;?~6 >~5 ?>~12 =>=>>>>==>>>==>=~4 <~11 >~15 <==><==<~15 =<~19 >>>=<=>>>=~4 <=~5 >>>>=>>=>====<<<====<~14 =<<<==<~5 ===<=<=>~7 =<<==>>=>>==>====<===<~8 ;<<;~7 ?~12 <~14 =<==>>??<<<<>=><~4 =~4 <?>~4 =>>>===>><~9 ;=<;;;<<;;;<=<=<<;?~9 >=~5 >=>>><<<>=<<<==<<=>???>???>?~11 ===;<;<<=<~5 ;<<;;===<~5 ;;;<~7 ;<<<=;;;<<<;;<;<;<=;;;;<;<;;;TQRRSSRRSPS==<<==<<==KNONNOOOQOPSS=~5 >>>==???>?>????>>?>SPRRQRSQNNQR===<>=>=KKLKLKKLKLLMOLQMMKFJGPNQONOHEHKMOLFEEEIEI>=>==>><==<<<==<<<<==<<<=<<;<<<;LKLJLMLNPPNPKPPNOQNIJNKPKMMJMKJO??>~5 ????>>?~5 @???@?@@>=>===>>====POPPNOOOOPOONMLNMMMNM?M?~6 >~5 ?>?>==?=>>>=>===>>=>??>??>=~14 >===>=<<===<=<=<<<<=~6 <=<<<===<~5 =<<=~5 <=<==<<<<=<~5 ;<~4 ;<<==<=<;~4 ONMNOQOOOFGIHJ>=HJ>~9 =~17 >=>=>=>>===>=@??@AGDEHC?@?@>>>>DH>>>FA?NFNGILMMMMKJIIL?>IHLL?MO>KO?>===>=~5 <=~5 <==<===<<<<===<====<==<~9 =<<<<;<~6 >?>??@@??=>?>=@@@?<<<=<~5 ;<<;<<;;:::;<<;;<::<<:9<;<<===;;;<>;:<<;;;;::;::<==;<<=~6 ?~5 :~8 ;;:~16 ;~4 :===>;=~4 >>>??@?@@??@@A@A;=~4 <~6 =<>?<?<<<?=?<=<=<<<=>AB<<B=<<?~6 >BBBB<B@>>BB<<<>><~5 ;@AAAA<<<<@A>>A<<@>@>ABBBAA<~4 =;<=<?@@@<@~7 A~10 @@@A;~10 :;~5 :;=>==DFCF=>=;<<<<;==<<=<===>A;<>;<=A====<;::999;;::;;::;<;<;<=<?LNDQO??FO>PVI>PJG@K@===?>==IPQFUSVVTYJDVVU>~6 @>>?>??@>==AB???=?>>??@?<<;;::;@@><?::::<;:::@>??>?@?@@@@?@@A@ZXQ[QQOMMKJJQMNLMMLKJKHIIJILIMNJMMONOPPPPNKOPMMNLNMNOMMKKHLMNMPPOOMLKJONPONMNMNLNONNMNOPPPPOJMMPOOONGDFMN=<<===GHF>JFE>>=<<<=<<=OOOQONOPPO=<<=<=<<<<=<<<=~5 >=>>===>~6 ==>>>=>>>==>=>>==>>=>>>>==>=<~8 ===<<<<=<<<<;<<;<===<<>==<<<<=<=~5 <=<=<<<====<<;;;;=~5 <<<;=;<>=>=<<=<<;;;;<~10 ;;<~8 ;~5 <~6 ;<;<<<;::9;;;9:9:;;:9;;;<<;;9<<<;<~14 ;~6 :;;;;:::<<<;;;;<:;<;~8 <;<;~6 <~10 ;~7 :;~6 :;~4 <;~4 <~5 ;<<<==<=~5 <==<;;;<<;<~4 ==>>=;<???;~16 ECCFGGF@@@?FFEFG~6 BC@@?@DA>?>=MMMKLORTSRRRRQRSRSPONMPMPOLMLNQPOOQOTORSNNNOSQORQQQRQSPOONOPOPQNOMNMLMLOMKMKLKKK>~4 ?>>>?>>=~5 >=>=>=>=~7 :;:~6 ;;:::>~4 ===<===<;=<<==<====>>>=~23 >~4 <<=<==<===@>?>=@A=<=>??CACACCC=~4 >=>=<=~7 <=<~5 ;<;;<<<:;;<<<;<;=C??A===<D@BD<====>=~4 >>?=<=;<=>==;;<;:;OMQQNPPPQQONNNOOONMQPQOLJKKNM?@?>>>@?===>=???>=?==?~4 >>>>==>==>~5 ==>=<====><=?@=@<<>><<?;;;:;;;;::PNPOPPNOMKKLONRRQNRSRMLLMMLLMNNP???@@????@~4 ??PPQPNOPOMP@???>=>=?@?>>=><=~6 ><~7 ;~7 <<<<;;<<;;@?>??@>@=?@=?>@@@@<~8 ;<<;<;:;:9~5 :;;88:;:;<<<;;;;<;:;~4 :;:;;<<<;<;~15 <<;;;<<;<;;;;<;;;:~7 9:~9 99989:9~4 :::99::::8:::;:99:8<;;;<:;:::;;;;<;;;<<<;;::;;::;::;;;;:;~5 ::;;<;<<;~6 :;~5 <;;<<<;<<<<;;;;<<;~7 <;:;~7 :9;;;:;;;;<=<<<==<=~9 <<=<===<<<<==;<==<~6 ;<;;=<;;=~6 ;;<~18 =<=<<<=~5 ;====<=~4 <;;;<<<<==<;;<<=;;<;<;<~5 =~5 <=<~4 ==<=~6 <~11 ;<~16 ><<<=~4 <=~4 :::=@?<<::@?@>@?@=<:>::@::<;:@<;<;<<;:<=~5 <=<<=<<;<:;<<===<=~5 <;===<~6 ==<<<==<>?==>=<<<=<=~12 <<<;<<;;<<;;;;<~4 ==;;:::=<<<<=~11 <=~9 <=~5 >=~4 >??<=<@?=?=>?=~4 <<=;;=~7 <===;<;====;;;===<:;;;;<<=;==<;;;<<::;:?;;@=~6 >=:~4 <:;===<~5 =<<<<=<<;=<<?>??@@@B?>?==>>=<<><<><~6 :;:<==<~6 =<~7 ====>><<@>????>=<<<==<=<?>><>===><===<<<<><?:9::999;:;98989~7 898998999BBBAAAABABBBA~4 B~6 AAABA@B?@@ABBAABAA@@?~4 BBA?>??>===>~7 @??>>>?@@????>==>!~5 [![!~11 [!!!B~4 ABAB~5 AB~16 A~8 B~11 AAAAB~5 !_]_[^[^[X][Xhiihiihhhg~4 hhfeehfegdehmnfiX^Yab[]XWXXttr!ZXY[YZ[Z!Yxvvxzvyqsuvnhp![[[!![[]Z!_!!c_chkXWWZZX[jo!ZX^`_`^iij[^[e~4 aedfhfhfjlkjkikadf?>>?=>>>>??>?=>~7 ?~10 >?>=>====>>====?=~4 >~11 @~9 ?~10 ====>?>=>><<>>>=<<?>??>>===><<=<?>>?>===>>?~9 >?>??>>>??>>>>?>>?~4 @@???>=>==>>????>???>?>~7 ??>>>>?~9 >>?==?==>=>==>>===>>>=~18 <==<~4 ==?>~4 =>>>>=?>>>>=~14 >=>????>>>>?>=~5 >=~5 <<<<=<=~11 <=<<=<=?==>~6 ==>===>>=>~4 =>=~4 >>=>?>~6 ===>=>~7 =~4 <=~6 <<=~9 >>>=~4 >>=>>==>===>>>=~24 <=~24 >>==>==>=>==>~13 =>>><===<<====>>===<<=<<=~4 >~4 =~5 >??>=>>>=>>==<=<===<===<=>>>>===>=>====<~16 =<=<=~7 <<=<<<<=<=<=<~6 =<<<?~4 >~4 =>><<<<=~4 >><=djbioojc`rmru`^~6 aa_!!ecdffcuuwxwyxwxvvr]lr?=>>=>~4 ?>???==>~4 choio]^!Y`cb^ruvwwwxwyxXZ[][!WWYWXZYY[!`[bedhi!lnlmlllhhYWWY!!XXpgegglkZXabkeaeedffceYXa!]YY>~4 ?>?>?~4 =~7 >>>=>>>>=~6 >===>>>=~7 >>>=~4 >====<===>>>?>??>??>~7 ?>>><;<:89>>>=>=>::9:9;9<<=<=~4 <~6 ====<<====68576:8687787;;85:ED~4 @@BBB@AEECCABD>?A>@A@AA@~5 A@~8 AAA@~5 A@@A~6 @A:;;;:::;:;;:<~10 >===?~7 >=<<;<;;:~9 ;<<<<@???<<=>=<;;<;<===>===<>=~4 ?@?@>>>=~4 >=?@@????AA@===@<===?@@@?~5 @?~5 @~7 >???@??@?>>>??>>>>=>=@>>>>??>?<~11 ;<;<<>=~8 >=>=<====>=>====>>><<<>>=~8 <~4 =~6 >==?>@?===>=<=>@==@@@<<>=>><<>===?<=?=<=<=99:9~18 :9999:~24 ;:::;:::<<=<;;<;;;:;;<:9~4 ;::;:~8 ;;;::::;~7 99::=~4 <;<<=<<<=<<>==<<<;<=;<==:::;<;=<<===;<:<~5 ;<;<<<<==>???=>>=>>>====>====>~7 ====>>><=<>>?>>???>~4 ===>==>~7 99:999:::9<<:<<;<;:::<~12 ::;::;;;<;<~5 ::;;==<==<<<===<<==<>=~6 <~6 .10,54+.--+48731/0::;,0---*-/-+-//./0/0-0/0.32561-,,,,-165?=@@?9=BA,,,,<<=~5 >==@??CAACCAC===>>>D?=?>?@>=>)+*,++*'*'(&('&&#$#*+:~7 ;:~6 9~4 79<;::9;887777;~4 77867:9~4 888879~6 :::9999::9999:::9:~6 999:~12 ;~6 ====<<<=<*))&%-,++'(($$%#&%*##&#,-.---,,===;<<=<=<<<<;;:;<==<<<;=~5 ;;;:;;=<<=~5 >==<=~12 <====;<====<====<=>===>===;:<::::;;<;<<==;=~4 9:8=<<<:;<<;===<;<<=<~5 77778898698;;:;;::9:::9:;;<:<;~5 98999::::<;;::;;:;<;;<;<;<;;;;<;~5 =~13 <<=~5 <===<<<<;===<=~9 >==>=~10 8987889899887*+*777,,+---.-0,-.-,,-,((*'(&*-#(%(+(0178736546678777#1449~4 :;:;;:::30./2460/664567567665676464-455;~6 9999:::999:9~6 :~6 8:::7887788779997878::9~6 ::;;:;~5 ::;::;;9;;;;:;::;~4 ::8:9:98987878:;~5 8778<::=~4 <=<<8779;>B?B<8878896787>;=;9>=?;95Arpnnmpsqrrrnmmfghfdgfihlfkihkijfldea~4 bb`^_``a`a``_a`a`agggebbdbab9899998897~8 rrpstrtwwuuwuoonooppqporquuvvsurssvotswtklhoolggpmmmheeflkoqvwwmhhkfhirsieipddhe_`]]_]_]bc]!!]!``^_]]!]!]!!d]^]c`]^]^]]]!!]eqqgghdfahfa__gjhfaplmmii_]^ghlhfg_afdeh]^^]]!]!]]h]^]!~7 97978887::8;?><<.(,//1-'.CAA??@===::A7@7<;;;==<;<<;<~10 >>>><><=>=<=<==>==>~4 =>~6 =;==;=;=<>::;;<<;:;<;;;<;<;;::;;<<;;<;<<;<<<<::::9:;<:;;;====;;>=>~7 =<<<=;<==<<===<?=<=<<=;<<;=<;;<;;;;<=<;:::;;:;;:;<;:;:;;:~4 >=<<<>>>?>>@?~7 <=?>>=~6 <===A~5 >=>=?@<<<<?=>?><~4 >>>>?>>>>@AA?A@??>>=>~4 ===>===>>=~5 >~8 ?>?>>?>>?>??>?@?@??@??>=>>>===>>?>>>>?>?==>>==>AAA@@=~4 <===>=~9 >=~5 ;<;<;;<;<;<=<===;;;=>>>>?@@@==??>>@>=>=>=~6 >>A@@ABB?>?>?@A>===>>>=>?>;=<~4 ;<==;<;;<<;<;<<=<=<==>=>>>???>?A==>>===>~4 ==>~7 ?>??>?>~4 ??>>>>=>=>>>>?>?>>>>=>>=>==><=<>>>>=>====>==>=>>>==>?==>==>~10 <~4 ===<<;<;~13 ==<;<;<;<~8 =<:;;:::;;<;<<<<;<;~10 <<<<;<<<<:~8 ;;:;;;:;;<:;<88889988987888:~7 9:~6 88:;:;9;<<:9:;;:878::78::87~5 @??@A???AAAA?A;;;<=<====;;@@@A;@@@==>~4 @==<<=;?A>>@?@;<=;>=>=::;~8 <<;<<==;;<=:::;::9:::88::9::;:;::;988:~8 777899978:::9888787789~4 8988?@@A@AA=~7 @ABAAAA@AAA??@BB==>====>=~13 >?>@>??AAA>>==>?~4 BB?B====;:;;:::9:::;:=~12 <~8 ====<~10 ===<<<<>=~7 ;=<<<<=;==<<;==>===;=<<====<><==>><>>=>~4 ==<<>=>><==>=><<>~6 =>>>====>==>>===>>>>==>=><~10 ==<<===<<<====>===>>>=~6 >==>>??=>=?===>==>=><=<==<<==<<<<=<=<==<>>@?@@?@?@@@???>~5 =>=>~13 =>=>>>=>~5 CBB@@>@?@>>?>~11 =~4 ::;9:::===:<<<:;;>=~6 <=<<===;<=<=<====<<====<<==AAAA@caadc_ae]^^`outuqtwpv!]!!``![!ilcllc>~4 yxwxuyyywyywy>>a`XYZ[Y^_]_]a^c[[Ydbdca```acbb^`__Y[W!]fffed]!]!!XZfe][^[![Z[!!!![[[!aZcXXXZffeec`hcYXce`eednqmmfeYYkienoilkYWWYW!!bdqrmlZfj777a_^`_^]]]]^!]![!!!]_!]^``]]]^A~4 BBA]!]~4 ::99;;:89;<;<;<;9::]!!]]!!!!?D?@??:;;::;<9;9:;~5 :;;:>>=>=~7 >>>====>===<===???>>===A>>>>=>=~13 >~7 ==>=<=?><<<=<<<==>??<?==<<?@@=??<=<<==<<=~7 <~4 ;<<<==<<==<<=<<====<~12 =<~10 =<<;<~6 >=<==>===<~11 =<~11 =<~4 ==<=<~4 ===<~5 =<~5 =<=~7 <=<<=<~6 =~6 <===<~5 ;~5 <<;~5 <;;:::;;;::;:;::;:;::;~4 :;:~5 ;;<;<~4 =~8 <===<>=>==>=>><<;<===<=<==<=????>AA@AAA@A??>>??=>>?A:A:A:~8 <=;<;;;:;;A;::B:BBA::::???=<<>?>===<<<<==;<=<====>>>;;:<;;>==<=<;~5 ?@A???@@@ABA??=>=>@@B?>>>>==>?>==>==>>=><<>=>;;<=::;;:~4 ;~9 <<=<;~9 <;<<<;~4 <~4 ;<<;<;~12 ::;~9 <~8 ===::;;;<:<~4 ;<~4 ;;:;:::9;:~4 9::9998:9988;~10 9;;;:;;;;::;;;;:;;;:;;:9::;::8=~5 <~4 =<=<<<===<~5 ===<~6 ;<====<====<<<<=~9 >=<~4 ==<<=~5 <====;<<==<;<~5 ==<==<?@?>>===??==@~5 ?@?;=<~5 ==<<==;<<<==>=>=~4 >===>===>>>=>?=?>~4 ===>>>>====<~4 ===<<;<>>?=~5 ??==?=<<;;;;::899:889:~4 ;<;<;<;:;;;;RRSRSTSQQSSTSSPQQQRQPPRTRBCDBDBDDFCDBDBBD??@DCCC==-1<=CD1484:0<?A?C-BD;===:../0=<~14 =<>==>>=<====<=<==<~6 ==>><@>?@DD<?@B?DBD~4 EEGEGDDED?=DHGFDEEFEGHHHGGGFHHHEH<3<<<72>~4 =>>>>====>~4 ====>~5 ====>><=>~5 <<====1052==>=~7 >~6 @A@@@?@@?A??>A@AAA@>>>>?>>@~4 ??>@???@@@??@~5 ??@>~13 =>>===>~5 ?>>?>==>>>>?>~6 ?>>=~4 >=>=~8 ??>>>==???==>~9 ?>=~4 >>>??>><<=<<<>=>>=~4 <~8 ====>==<====><>>>>==>>=>=><=<~8 ?>?>>=>=>=>~16 ?~9 ===>>>>=>====<<=<>===<<<=<=????>~16 ?>>>D~4 CD>~9 D~8 B@A@A??CD?>?>>>?>>DDCD??A???>??>?~4 >>>>?>???>~11 =>~14 ?~12 >>=>?=?==>>==>>?>>>EDEDDD>~6 ===>>=>>>>====<<<<==<<<<==DCDFEDGFGDFGCEBAC?@@CB@??@??@?>>>???A??<=<===<<===<~5 =<~5 =<<>~7 A~10 B~11 AAB~9 AAA@AAA@?@@@?BA~5 BBBCCBB?>>@?>~10 ??><<==<<;:;::;<;;:;;;:;;:~5 ;=:;<~9 ====<=;<<<<=<<=<~7 ;~20 <~9 ;<~16 =<<<==<~11 ;<~19 ===<<=<<<<=<==<<=<=<<=<=<<==<<<=<~10 =~18 <=<=~4 <===<=<<<==<~11 =>>=;:;==>==>>=~6 >>?>>>???>~4 ?>?>?>=?~5 =>???>>??<<<=>><=<>=><~4 =<=~6 <~4 =<<=;;==>>>>=~4 >>????>?>=>~6 <<=~8 >==>>==?>@~4 A>??>@?>?@BB>>??AA@AB@@A@??>?@@===>=~4 >~4 =~6 <<<<=~6 BBCCBCCB~4 A~6 @ABAA@AAA@A@@@@>~5 ?>>????>????>?>>>??>=><=>?;;;<<=<=>>??;<==<<<<=<~14 ;<;<<<;;<<;;;;<;;<~4 ;~4 ?>><<=>=G~15 C~4 DECBDDBEEDDCDCCCBBBBCBBDCC@@@BB@@?>?>@??=>=?>?>@?@=?<?>?<<??<;<?>>?=:>=>>====>===87;A9;>>;<~6 >>==<===:~5 @:~7 @@A::::<=~25 >>;<=;:~4 ??>??=>=:<??=::=@=;=<;<<;;<~8 ::<=~6 <@?>==<>>>?@?>=~4 ;=;<<<;<;;;<<<??@~8 A@?@~19 =~5 <><==>=@>>><=>~11 =<=>>AAA@><<<<A@ABB;;<<;<?@=@@@<<???=??>=??>??>??@@=>~4 ?>BBB<=;;;;B;@>=>~4 ==>>=~12 ;=<====<:;<;<::<<==>~5 :~5 <;<;;;;<>>>I>><;<==>;;<;;<<=>==><<<:~4 ;99:99:<;<;<;~11 <;<<<;<;;:;;;<<<::<???<<==><<<==><@???@>=<~6 @@<<<<@>>==<>=>==<~25 >~10 <<=<<>=>>>=~5 <=>===<==>~5 ====<<<<=~4 <~5 =<=<===<=<=<=<;~9 =>>==><<=;;;=>;<<??=>=>?=><<<<;=887788878~4 7~4 88777999:99889988989899998999::99:~6 ;;::;::;<<<;~7 <;::::9998878898:9:87778787778<<<<;<;<;;:;;;:;;:;;;:~4 99::::8::99::999::899897899:788:9:9<~7 ;<<<;<:;<~6 ;;;:::9999;:;8:9<<==<===<<<=<<=~4 <==<<<<=<=<<<;;<<===<<<<===;<;=<;;:;;;;:;<::;89::8:::8:::8787:::79:9:99989:9:;:;;;:;;<;;;;<~7 ;<99;;:
   *   </jvxlColorData>
   * </jvxlSurfaceData>
   * 
   * 
   **********************************************************/

  /**
   * encode triangle data -- [ia ib ic]  [ia ib ic]  [ia ib ic] ...
   * algorithm written by Bob Hanson, 11/2008. The principle is that
   * not all vertices may be represented -- we only need the 
   * used vertices here. Capitalizing on the fact that triangle sets
   * tend to have common edges and similar numbers for sequential triangles.
   * 
   * a) Renumbering vertices as they appear in the triangle set
   * 
   *    [2456 2457 2458] [2456 2459 2458]
   *    
   *   becomes
   *    
   *    [   1    2    3] [   1    4    3]
   * 
   * b) This allows efficient encoding of differences, not absolute numbers.
   * 
   *        0    1    2     -2    3   -1
   *        
   * c) Which can then be represented often using a single ASCII character. 
   *    I chose \ to be 0, and replace that with !.
   *    
   *    ASCII:
   *    -30       -20       -10         0       +10       +20       +30      
   *    <=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\]^_`abcdefghijklmnopqrstuvwxyz{|
   *    
   *    So the above sequence would simply be:
   *    
   *      !]^Z_[
   *
   *    When the range falls outside of +/-32, we simply use a number.
   *    When a positive number follows another number, we add a "+" to it.
   *    
   *      !]^Z_[-33+250]230-210]]
   *      
   *    Preliminary trials indicated that on average a triangle
   *    can be encoded in about 7 bytes, or roughly half the 12 bytes
   *    necessary for standard binary encoding of integers. The advantage
   *    here is that we have an ASCII-readable file and no little-/big-endian issue.
   * 
   * @param triangles
   * @param nData
   * @param vertexIdNew
   * @return            encoded data string
   */
  public static String jvxlEncodeTriangleData(int[][] triangles, int nData,
                                              int[] vertexIdNew) {
    StringBuffer list = new StringBuffer();
    StringBuffer list1 = new StringBuffer();
    int ilast = 1;
    int p = 0;
    int inew = 0;
    boolean addPlus = false;
    for (int i = 0; i < nData;) {
      int idata = triangles[i][p];
      if (vertexIdNew[idata] > 0) {
        idata = vertexIdNew[idata];
      } else {
        idata = vertexIdNew[idata] = ++inew;
      }

      if (++p % 3 == 0) {
        i++;
        p = 0;
      }
      int diff = idata - ilast;
      ilast = idata;
      if (diff == 0) {
        list1.append('!');
        addPlus = false;
      } else if (diff > 32) {
        if (addPlus)
          list1.append('+');
        list1.append(diff);
        addPlus = true;
      } else if (diff < -32) {
        list1.append(diff);
        addPlus = true;
      } else {
        list1.append((char) ('\\' + diff));
        addPlus = false;
      }
    }
    return list.append(
        "  <jvxlTriangleData len=\"" + list1.length() + "\" count=\"" + nData
            + "\">\n    ").append(list1).append("\n  </jvxlTriangleData>\n")
        .toString();
  }

  /**
   * encode the vertex data. This must be done AFTER encoding the triangles,
   * because the triangles redefine the order of vertices.
   * 
   * Bob Hanson 11/2008
   * 
   * If another program has created the triangles, we probably do not
   * know the grid that was used for Marching Cubes, or quite possibly
   * no grid was used. In that case, we just save the vertex/triangle/value
   * data in a compact form.  
   * 
   * For the  we use an extension of the way edge points are encoded. 
   * We simply identify the minimum and maximum x, y, and z coordinates and
   * then express the point as a fraction along each of those directions. 
   * Thus, the x, y, and z coordinate are within the interval [0,1]. 
   *  
   * We opt for the two-byte double-precision JVXL character compression. 
   * This allows a 1 part in 8100 resolution, which is plenty for these 
   * purposes. 
   * 
   * The tag will indicate the minimum and maximum values:
   * 
   *   <jvxlVertexData len="267876" count="44646" 
   *      min="(15.218472, -28.304049, 34.71112)" 
   *      max="(97.8228, 54.011948, 109.95208)">
   * 
   * The resultant string is really two strings of length nData
   * where the first string lists the "high" part of the positions,
   * and the second string lists the "low" part of the positions.
   * @param meshDataServer 
   * 
   * @param jvxlData
   * @param vertexIdNew
   * @param vertices
   * @param vertexValues
   * @param vertexCount
   * @param polygonColixes 
   * @param polygonCount 
   * @param addColorData
   * @return              string of encoded data
   */
  public static String jvxlEncodeVertexData(MeshDataServer meshDataServer,
                                            JvxlData jvxlData,
                                            int[] vertexIdNew,
                                            Point3f[] vertices,
                                            float[] vertexValues,
                                            int vertexCount,
                                            short[] polygonColixes, 
                                            int polygonCount,
                                            boolean addColorData) {
    Point3f min = new Point3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
    Point3f max = new Point3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    Point3f p;
    for (int i = 0; i < vertexCount; i++) {
      p = vertices[i];
      if (p.x < min.x)
        min.x = p.x;
      if (p.y < min.y)
        min.y = p.y;
      if (p.z < min.z)
        min.z = p.z;
      if (p.x > max.x)
        max.x = p.x;
      if (p.y > max.y)
        max.y = p.y;
      if (p.z > max.z)
        max.z = p.z;
    }
    StringBuffer list = new StringBuffer();
    StringBuffer list1 = new StringBuffer();
    StringBuffer list2 = new StringBuffer();
    int[] vertexIdOld = new int[vertexCount];
    for (int i = 0; i < vertexCount; i++)
      if (vertexIdNew[i] > 0) // not all vertices may be in triangle -- that's OK
        vertexIdOld[vertexIdNew[i] - 1] = i;
    for (int i = 0; i < vertexCount; i++) {
      p = vertices[vertexIdOld[i]];
      jvxlAppendCharacter2(p.x, min.x, max.x, colorFractionBase,
          colorFractionRange, list1, list2);
      jvxlAppendCharacter2(p.y, min.y, max.y, colorFractionBase,
          colorFractionRange, list1, list2);
      jvxlAppendCharacter2(p.z, min.z, max.z, colorFractionBase,
          colorFractionRange, list1, list2);
    }
    list1.append(list2);
    list.append("  <jvxlVertexData len=\"" + list1.length() + "\" count=\""
        + vertexCount + "\" min=\"" + min + "\" max=\"" + max + "\">\n    ");
    list.append(list1).append("\n  </jvxlVertexData>\n");
    if (polygonColixes != null) {
      list1 = new StringBuffer();
      int count = 0;
      short colix = 0;
      boolean done = false;
      for (int i = 0; i < polygonCount || (done = true) == true; i++) {
        if (done || polygonColixes[i] != colix) {
          if (count != 0)
            list1.append(" ").append(count).append(" ").append(
                (colix == 0 ? 0 : meshDataServer.getColixArgb(colix)));
          if (done)
            break;
          colix = polygonColixes[i];
          count = 1;
        } else {
          count++;
        }
      }
      list.append("  <jvxlPolygonColorData len=\"" + list1.length() + "\" count=\""
          + polygonCount+ "\">\n    ").append(list1).append("\n  </jvxlPolygonColorData>\n");
    }
    if (!addColorData)
      return list.toString();

    // now add the color data, again as a double-precision value.

    list1 = new StringBuffer();
    list2 = new StringBuffer();
    for (int i = 0; i < vertexCount; i++) {
      float value = vertexValues[vertexIdOld[i]];
      jvxlAppendCharacter2(value, jvxlData.mappedDataMin,
          jvxlData.mappedDataMax, colorFractionBase, colorFractionRange, list1,
          list2);
    }
    String s = jvxlCompressString(list1.append(list2).toString());
    return list.append(
        "  <jvxlColorData len=\"" + s.length() + "\" count=\"" + vertexCount
            + "\" compressed=\"1\" precision=\"true\">\n    ").append(s)
        .append("\n  </jvxlColorData>\n").toString();
  }

  /**
   * retrieve Jvxl 2.0 format vertex/triangle/color data found
   * within <jvxlSurfaceData> element 
   * 
   * @throws Exception
   */
  private void getEncodedVertexData() throws Exception {
    String data = getXmlData("jvxlSurfaceData", null, true);
    jvxlDecodeVertexData(getXmlData("jvxlVertexData", data, true), false);
    String polygonColorData = getXmlData("jvxlPolygonColorData", data, false);
    jvxlDecodeTriangleData(getXmlData("jvxlTriangleData", data, true), polygonColorData, false);
    Logger.info("Checking for vertex values");
    jvxlColorDataRead = jvxlUncompressString(getXmlData("jvxlColorData", data, false));
    jvxlDataIsColorMapped = (jvxlColorDataRead.length() > 0);
    jvxlDataIsPrecisionColor = (data.indexOf("precision=\"true\"") >= 0);
    jvxlDecodeContourData(getXmlData("jvxlContourData", null, false));
    Logger.info("Done");
  }

  private void jvxlDecodeContourData(String data) throws Exception {
    Vector vs = new Vector();
    int pt = -1;
    vContours = null;
    if (data == null)
      return;
    while ((pt = data.indexOf("<jvxlContour", pt + 1)) >= 0) {
      Vector v = new Vector();
      String s = getXmlData("jvxlContour", data.substring(pt), true);
      int n = parseInt(getXmlAttrib(s, "npolygons"));
      float value = parseFloat(getXmlAttrib(s, "value"));
      int color = Escape.unescapeColor(getXmlAttrib(s, "color"));
      String fData = getXmlAttrib(s, "data");
      BitSet bs = jvxlDecodeBitSet(s.substring(s.lastIndexOf("\">") + 2));
      IsosurfaceMesh.setContourVector(v, n, bs, value, color, new StringBuffer(fData));
      //if (s.indexOf("i=\"5\"")  >= 0)
      vs.add(v);
    }

    vContours = new Vector[vs.size()];
    for (int i = 0; i < vs.size(); i++)
      vContours[i] = (Vector) vs.get(i);
  }

  public static void set3dContourVector(Vector v, int[][] polygonIndexes, Point3f[] vertices) {
    // we must add points only after the MarchingCubes process has completed.
    if (v.size() < IsosurfaceMesh.CONTOUR_POINTS)
      return;
    String fData = ((StringBuffer) v.get(IsosurfaceMesh.CONTOUR_FDATA)).toString();
    BitSet bs = (BitSet) v.get(IsosurfaceMesh.CONTOUR_BITSET);
    int nPolygons = ((Integer)v.get(IsosurfaceMesh.CONTOUR_NPOLYGONS)).intValue();
    int pt = 0;
    for (int i = 0; i < nPolygons; i++) {
      if (!bs.get(i))
        continue;
      int[] vertexIndexes = polygonIndexes[i];
      int type = ((int) fData.charAt(pt++)) - 48;
      char c1 = fData.charAt(pt++);
      char c2 = fData.charAt(pt++);
      float f1 = jvxlFractionFromCharacter(c1, defaultEdgeFractionBase, defaultEdgeFractionRange, 0);
      float f2 = jvxlFractionFromCharacter(c2, defaultEdgeFractionBase, defaultEdgeFractionRange, 0);
      int i1, i2, i3, i4;
      /*
       *     char type ('3', '6', '5') indicating which two edges
       *       of the triangle are connected: 
       *         '3' 0x011 AB-BC
       *         '5' 0x101 AB-CA
       *         '6' 0x110 BC-CA
       */
      if ((type & 1) == 0) { //BC-CA
        i1 = vertexIndexes[1];
        i2 = i3 = vertexIndexes[2];
        i4 = vertexIndexes[0];
      } else { //AB-BC or //AB-CA
        i1 = vertexIndexes[0];
        i2 = vertexIndexes[1];
        if ((type & 2) != 0) {
          i3 = i2;
          i4 = vertexIndexes[2];
        } else {
          i3 = vertexIndexes[2];
          i4 = i1;          
        }
      }
      Point3f pa = IsosurfaceMesh.getContourPoint(vertices, i1, i2, f1);
      Point3f pb = IsosurfaceMesh.getContourPoint(vertices, i3, i4, f2);
      v.add(pa);
      v.add(pb);
    }
    v.add(new Point3f(Float.NaN,Float.NaN,Float.NaN));
  }


  /**
   * a relatively simple XML reader for this specific application.
   * 
   * @param name
   * @param data
   * @param withTag
   * @return            trimmed contents or tag + contents, never closing tag 
   * @throws Exception
   */
  private String getXmlData(String name, String data, boolean withTag)
      throws Exception {
    //crude
    String closer = "</" + name + ">";
    String tag = "<" + name;
    if (data == null) {
      StringBuffer sb = new StringBuffer();
      try {
        while (line.indexOf(tag) < 0) {
          line = br.readLine();
        }
      } catch (Exception e) {
        return null;
      }
      sb.append(line);
      while (line.indexOf(closer) < 0)
        sb.append(line = br.readLine());
      data = sb.toString();
    }
    int pt1 = data.indexOf(tag);
    int pt2 = data.indexOf(closer);
    if (pt1 >= 0 && !withTag) {
      pt1 = data.indexOf(">", pt1) + 1;
      while (Character.isWhitespace(data.charAt(pt1)))
        pt1++;
    }
    if (pt1 < 0 || pt1 > pt2)
      return "";
    return data.substring(pt1, pt2);
  }

  /**
   * decode vertex data found within <jvxlVertexData> element
   * as created by jvxlEncodeVertexData (see above)
   * 
   * @param data      tag and contents 
   * @param asArray   or just addVertexCopy    
   * @return          Point3f[] if desired 
   *    
   */
  public Point3f[] jvxlDecodeVertexData(String data, boolean asArray) {
    int[] next = new int[1];
    setNext(data, "count", next, 2);
    int vertexCount = Parser.parseInt(data, next);
    if (!asArray)
      Logger.info("Reading " + vertexCount + " vertices");
    next[0]++;
    Point3f min = new Point3f();
    Point3f range = new Point3f();
    setNext(data, "min", next, 3);
    min.x = Parser.parseFloat(data, next);
    next[0]++;
    min.y = Parser.parseFloat(data, next);
    next[0]++;
    min.z = Parser.parseFloat(data, next);
    setNext(data, "max", next, 3);
    range.x = Parser.parseFloat(data, next) - min.x;
    next[0]++;
    range.y = Parser.parseFloat(data, next) - min.y;
    next[0]++;
    range.z = Parser.parseFloat(data, next) - min.z;
    int colorFractionBase = jvxlData.colorFractionBase;
    int colorFractionRange = jvxlData.colorFractionRange;
    int ptCount = vertexCount * 3;
    Point3f[] vertices = (asArray ? new Point3f[vertexCount] : null);
    Point3f p = (asArray ? null : new Point3f());
    float fraction;
    setNext(data, ">", next, 0);
    int pt = next[0];
    while (Character.isWhitespace(data.charAt(pt)))
      pt++;
    pt--;
    for (int i = 0; i < vertexCount; i++) {
      if (asArray)
        p = vertices[i] = new Point3f();
      fraction = jvxlFractionFromCharacter2(data.charAt(++pt), data.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.x = min.x + fraction * range.x;
      fraction = jvxlFractionFromCharacter2(data.charAt(++pt), data.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.y = min.y + fraction * range.y;
      fraction = jvxlFractionFromCharacter2(data.charAt(++pt), data.charAt(pt
          + ptCount), colorFractionBase, colorFractionRange);
      p.z = min.z + fraction * range.z;
      if (!asArray)
        addVertexCopy(p, 0, i);
    }
    return vertices;
  }
  /**
   * decode triangle data found within <jvxlTriangleData> element
   * as created with jvxlEncodeTriangleData (see above)
   * 
   * @param data      tag and contents 
   * @param colorData 
   * @param asArray   or just addTriangleCheck    
   * @return          int[][] if desired 
   */
  int[][] jvxlDecodeTriangleData(String data, String colorData, boolean asArray) {
    int[] next = new int[1];
    int[] nextc = new int[1];
    int nColors = (colorData == null ? -1 : 0);
    int color = 0;
    setNext(data, "count", next, 2);
    int nData = Parser.parseInt(data, next);
    if (!asArray)
      Logger.info("Reading " + nData + " triangles");
    int[][] triangles = (asArray ? new int[nData][3] : null);
    int[] triangle = (asArray ? triangles[0] : new int[3]);
    int ilast = 0;
    int p = 0;
    int b0 = (int)'\\';
    setNext(data, ">", next, -1);
    int pt = next[0];
    for (int i = 0; i < nData;) {
      char ch = data.charAt(++pt);
      int idiff;
      switch(ch) {
      case '!':
        idiff = 0;
        break;
      case '+':
      case '.':
      case ' ':
      case '\n':
      case '\r':
      case '\t':
      case ',':
        continue;
      case '-':
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        next[0] = pt;
        idiff = Parser.parseInt(data, next);
        pt = next[0] - 1;
        break;
      default:
        idiff = (int)ch - b0; 
      }
      ilast += idiff;
      if (asArray)
        triangles[i][p] = ilast;
      else
        triangle[p] = ilast;
      if (++p % 3 == 0) {
        i++;
        p = 0;
        if (!asArray) {
          if (nColors >= 0) {
            if (nColors == 0) {
              nColors = Parser.parseInt(colorData, nextc);
              color = Parser.parseInt(colorData, nextc);
              if (color == Integer.MIN_VALUE)
                color = nColors = 0;
            } 
            nColors--;
          }
          addTriangleCheck(triangle[0], triangle[1], triangle[2], 7, false, color);
        }
      }
    }
    return triangles;
  }

  private static String getXmlAttrib(String data, String what) {
    // presumes what="xxxx" exactly like that, no whitespace around =
    int[] next = new int[1];
    int pt = setNext(data, what, next, 2);
    if (pt < 2)
      return "";
    int pt1 = setNext(data, "\"", next, -1);
    return (pt1 <= 0 ? "" : data.substring(pt, pt1));
  }
  /**
   * shift pointer to a new tag or field contents
   * 
   * @param data   string of data
   * @param what   tag or field name
   * @param next   current pointer into data
   * @param offset offset past end of "what" for pointer
   * @return pointer to data
   */
  private static int setNext(String data, String what, int[] next, int offset) {
    return next[0] = data.indexOf(what, next[0]) + what.length() + offset;
  }
}
