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
import java.io.IOException;

import javax.vecmath.Point3f;

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.util.Logger;
import org.jmol.viewer.Viewer;

class XplorReader extends VolumeFileReader {

  /*
   * 
   * VERY preliminary Xplor electron density map reader
   * something like this -- untested
   * 
   * http://cci.lbl.gov/~rwgk/shortcuts/htdocs/current/python/iotbx.xplor.map.html
   * 
   * Example format for Xplor Maps:
 
       2 !NTITLE
 REMARKS FILENAME=""
 REMARKS scitbx.flex.double to Xplor map format
      24       0      24     120       0     120      54       0      54
 3.20420E+01 1.75362E+02 7.96630E+01 9.00000E+01 9.00000E+01 9.00000E+01
ZYX
       0
-2.84546E-01-1.67775E-01-5.66095E-01-1.18305E+00-1.49559E+00-1.31942E+00
-1.01611E+00-1.00873E+00-1.18992E+00-1.02460E+00-2.72099E-01 5.94242E-01
<deleted>
   -9999
  0.0000E+00  1.0000E+00
That is:
...a blank line
...an integer giving the number of title lines, with mandatory !NTITLE
...title lines in %-264s format
...X, Y, and Z sections giving:
       sections per unit cell, in the given direction
       ordinal of first section in file
       ordinal of last section in file
...unit cell dimensions
...slow, medium, fast section order, always ZYX
...for each slow section, the section number
...sectional data in special fortran format shown
...-9999
...map average and standard deviation
   */
  XplorReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
    isAngstroms = false;
    jvxlData.wasCubic = true;
  }

  protected int readVolumetricHeader() {
    try {
        readTitleLines();
        Logger.info(jvxlFileHeaderBuffer.toString());
        readAtomCountAndOrigin();
        Logger.info("voxel grid origin:" + volumetricOrigin);
        readVoxelVectors();
        for (int i = 0; i < 3; ++i)
          Logger.info("voxel grid vector:" + volumetricVectors[i]);
        JvxlReader.jvxlCreateHeaderWithoutTitleOrAtoms(volumeData, jvxlFileHeaderBuffer);
      return readExtraLine();
    } catch (Exception e) {
      Logger.error(e.toString());
      throw new NullPointerException();
    }
  }
  
 
  protected void readTitleLines() throws Exception {
    jvxlFileHeaderBuffer = new StringBuffer();
    int nLines = parseInt(getLine());
    for (int i = nLines; --i >= 0; ) {
      line = br.readLine().trim();
      jvxlFileHeaderBuffer.append("# ").append(line).append('\n');
    }
    jvxlFileHeaderBuffer.append("Xplor data\nJmol " + Viewer.getJmolVersion() + '\n');
  }

  int nBlock;
  protected void readVoxelVectors() throws Exception {
    
    //not yet treating min/max
    int nA = parseInt(getLine());
    int minA = parseInt();
    int maxA = parseInt();
    int nB = parseInt();
    int minB = parseInt();
    int maxB = parseInt();
    int nC = parseInt();
    int minC = parseInt();
    int maxC = parseInt();
    
    voxelCounts[0] = maxC - minC + 1;
    voxelCounts[1] = maxB - minB + 1;
    voxelCounts[2] = maxA - minA + 1;

    nBlock = voxelCounts[2] * voxelCounts[1];
    
    float a = parseFloat(getLine());
    float b = parseFloat();
    float c = parseFloat();
    float alpha = parseFloat();
    float beta = parseFloat();
    float gamma = parseFloat();

    SymmetryInterface symmetry = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
    symmetry.setUnitCell(new float[] {a, b, c, alpha, beta, gamma});
    Point3f pt;
    //these vectors need not be perpendicular
    pt = new Point3f(0, 0, 1f/nC);
    symmetry.toCartesian(pt);
    volumetricVectors[0].set(pt);
    pt = new Point3f(0, 1f/nB, 0);
    symmetry.toCartesian(pt);
    volumetricVectors[1].set(pt);
    pt = new Point3f(1f/nA, 0, 0);
    symmetry.toCartesian(pt);
    volumetricVectors[2].set(pt);
 
    //ZYX
    
    getLine();
    
  }

  protected void readAtomCountAndOrigin() throws Exception {
    atomCount = 0;
    negativeAtomCount = false;    
    volumetricOrigin.set(0, 0, 0);
  }
  
  private String getLine() throws IOException {
    line = br.readLine();
    while (line != null && (line.length() == 0 || line.indexOf("REMARKS") >= 0 || line.indexOf("XPLOR:") >= 0))
      line = br.readLine();
    return line;
  }
  

  int linePt = Integer.MAX_VALUE;
  int nRead;
  protected float nextVoxel() throws Exception {
    if (linePt >= line.length()) {
      line = br.readLine();
      //System.out.println(nRead + " " + line);
      linePt = 0;
      if ((nRead % nBlock) == 0) {
        //System.out.println("block " + line);
        line = br.readLine();
      }
    }
    if (line == null)
      return 0;
    float val = parseFloat(line.substring(linePt, linePt+12));
    linePt += 12;
    nRead++;
    //System.out.println("val " + val);
    return val;
  }

//  for(int i = 1; i < 1000; i=i+1)
  //  System.out.println( (int)(((9999.0 + (i/100000000.)+(i/1000.)+0.000000001)-9999.0) * 100000000.));

}


