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
import java.util.BitSet;

import javax.vecmath.Vector3f;
import org.jmol.util.Logger;


//import org.jmol.viewer.Viewer;


abstract class VolumeFileReader extends SurfaceFileReader {

  protected boolean endOfData;
  protected boolean negativeAtomCount;
  protected int atomCount;
  private int nSurfaces;
  protected boolean isAngstroms;
  protected boolean canDownsample;
  private int[] downsampleRemainders;
 
  VolumeFileReader(SurfaceGenerator sg, BufferedReader br) {
    super(sg, br);
  }

  boolean readVolumeParameters() {
    endOfData = false;
    nSurfaces = readVolumetricHeader();
    if (nSurfaces == 0)
      return false;
    if (nSurfaces < params.fileIndex) {
      Logger.warn("not enough surfaces in file -- resetting params.fileIndex to "
          + nSurfaces);
      params.fileIndex = nSurfaces;
    }
    return true;
  }
  
  boolean readVolumeData(boolean isMapData) {
    if (!gotoAndReadVoxelData(isMapData))
      return false;
    if (!vertexDataOnly)
      Logger.info("Read " + nPointsX + " x " + nPointsY + " x " + nPointsZ
          + " data points");
    return true;
  }

  protected int readVolumetricHeader() {
    try {
      readTitleLines();
      Logger.info(jvxlFileHeaderBuffer.toString());
      readAtomCountAndOrigin();
      if (atomCount == Integer.MIN_VALUE)
        return 0;
      Logger.info("voxel grid origin:" + volumetricOrigin);
      int downsampleFactor = params.downsampleFactor;
      boolean downsampling = (canDownsample && downsampleFactor > 0);
      for (int i = 0; i < 3; ++i)
        readVoxelVector(i);
      if (downsampling) {
        downsampleRemainders = new int[3];
        Logger.info("downsample factor = " + downsampleFactor);
        for (int i = 0; i < 3; ++i) {
          int n = voxelCounts[i];
          downsampleRemainders[i] = n % downsampleFactor;
          voxelCounts[i] /= downsampleFactor;
          volumetricVectors[i].scale(downsampleFactor);
          Logger.info("downsampling axis " + (i + 1) + " from " + n + " to "
              + voxelCounts[i]);
        }
      }
      for (int i = 0; i < 3; ++i) {
        line = voxelCounts[i] + " " + volumetricVectors[i].x + " "
            + volumetricVectors[i].y + " " + volumetricVectors[i].z;
        jvxlFileHeaderBuffer.append(line).append('\n');
        Logger.info("voxel grid count/vector:" + line);
        if (!isAngstroms)
          volumetricVectors[i].scale(ANGSTROMS_PER_BOHR);
      }
      JvxlReader.jvxlReadAtoms(br, jvxlFileHeaderBuffer, atomCount, volumeData);
      return readExtraLine();
    } catch (Exception e) {
      Logger.error(e.toString());
      return 0;
    }
  }
  
  protected void readTitleLines() throws Exception {
    //implemented in CubeReader, ApbsReader, mrcBinaryReader, and JvxlReader  
  }
  
  protected String skipComments(boolean allowBlankLines) throws Exception {
    StringBuffer sb = new StringBuffer();
    while ((line = br.readLine()) != null && 
        (allowBlankLines && line.length() == 0 || line.indexOf("#") == 0))
      sb.append(line).append('\n');
    return sb.toString();
  }
  
  protected void readAtomCountAndOrigin() throws Exception {
    //reader-specific
  }

  protected void readVoxelVector(int voxelVectorIndex) throws Exception {    
    line = br.readLine();
    Vector3f voxelVector = volumetricVectors[voxelVectorIndex];
    if ((voxelCounts[voxelVectorIndex] = parseInt(line)) == Integer.MIN_VALUE) //unreadable
      next[0] = line.indexOf(" ");
    voxelVector.set(parseFloat(), parseFloat(), parseFloat());
  }

  protected int readExtraLine() throws Exception {
    if (!negativeAtomCount)
      return 1;
    line = br.readLine();
    Logger.info("Reading extra CUBE information line: " + line);
    return parseInt(line);
  }

  protected void readSurfaceData(boolean isMapData) throws Exception {
    /*
     * possibilities:
     * 
     * cube file data only -- monochrome surface (single pass)
     * cube file with plane (color, two pass)
     * cube file data + cube file color data (two pass)
     * jvxl file no color data (single pass)
     * jvxl file with color data (single pass)
     * jvxl file with plane (single pass)
     * 
     * cube file with multiple MO data will be interspersed 
     * 
     * 
     */
    /* 
     * This routine is used twice in the case of color mapping. 
     * First (isMapData = false) to read the surface values, which
     * might be a plane, then (isMapData = true) to color them based 
     * on a second data set.
     * 
     * Planes are compatible with data sets that return actual 
     * numbers at all grid points -- cube files, orbitals, functionXY,
     * and solvent/molecular surface calculations.
     *  
     * It is possible to map a QM orbital onto a plane. In the first pass we defined
     * the plane; in the second pass we just calculate the new voxel values and return.
     * 
     * Starting with Jmol 11.7.25, JVXL files do not create voxelData[][][]
     * and instead just fill a bitset, thus saving nx*ny*nz*8 - (nx*ny*nz/32) bytes in memory
     * 
     */

    next[0] = 0;
    int downsampleFactor = params.downsampleFactor;
    boolean isDownsampled = canDownsample && (downsampleFactor > 0);
    if (params.thePlane != null) {
      params.cutoff = 0f;
    } else if (isJvxl) {
      params.cutoff = (params.isBicolorMap || params.colorBySign ? 0.01f : 0.5f);
    }
    nDataPoints = 0;
    line = "";
    jvxlNSurfaceInts = 0;
    if (isJvxl) {
      nDataPoints = volumeData.setVoxelCounts(nPointsX, nPointsY, nPointsZ);
      jvxlVoxelBitSet = getVoxelBitSet(nDataPoints);
      voxelData = null;
    } else {
      voxelData = new float[nPointsX][][];
      int nSkipX = 0;
      int nSkipY = 0;
      int nSkipZ = 0;
      if (isDownsampled) {
        nSkipX = downsampleFactor - 1;
        nSkipY = downsampleRemainders[2]
            + (downsampleFactor - 1)
            * (nSkipZ = (nPointsZ * downsampleFactor + downsampleRemainders[2]));
        nSkipZ = downsampleRemainders[1] * nSkipZ + (downsampleFactor - 1)
            * nSkipZ * (nPointsY * downsampleFactor + downsampleRemainders[1]);
        //System.out.println(nSkipX + " " + nSkipY + " " + nSkipZ);
      }

      //Note downsampling not allowed for JVXL files

      for (int x = 0; x < nPointsX; ++x) {
        float[][] plane = new float[nPointsY][];
        voxelData[x] = plane;
        for (int y = 0; y < nPointsY; ++y) {
          float[] strip = new float[nPointsZ];
          plane[y] = strip;
          for (int z = 0; z < nPointsZ; ++z) {
            strip[z] = getNextVoxelValue();
            if (isDownsampled)
              skipVoxels(nSkipX);
          }
          if (isDownsampled)
            skipVoxels(nSkipY);
        }
        if (isDownsampled)
          skipVoxels(nSkipZ);
      }
      //Jvxl getNextVoxelValue records the data read on its own.
    }
    volumeData.setVoxelData(voxelData);
  }

  private void skipVoxels(int n) throws Exception {
    // not allowed for JVXL data
    for (int i = n; --i >= 0;)
      getNextVoxelValue();
  }
  
  protected BitSet getVoxelBitSet(int nPoints) throws Exception {
    // jvxlReader will use this to read the surface voxel data
    return null;  
  }
  
  protected float getNextVoxelValue() throws Exception {
    //overloaded in JvxlReader, where sb is appended to
    float voxelValue = 0;
    if (nSurfaces > 1 && !params.blockCubeData) {
      for (int i = 1; i < params.fileIndex; i++)
        nextVoxel();
      voxelValue = nextVoxel();
      for (int i = params.fileIndex; i < nSurfaces; i++)
        nextVoxel();
    } else {
      voxelValue = nextVoxel();
    }
    return voxelValue;
  }

  protected float nextVoxel() throws Exception {
    float voxelValue = parseFloat();
    if (Float.isNaN(voxelValue)) {
      while ((line = br.readLine()) != null
          && Float.isNaN(voxelValue = parseFloat(line))) {
      }
      if (line == null) {
        if (!endOfData)
          Logger.warn("end of file reading cube voxel data? nBytes=" + nBytes
              + " nDataPoints=" + nDataPoints + " (line):" + line);
        endOfData = true;
        line = "0 0 0 0 0 0 0 0 0 0";
      }
      nBytes += line.length() + 1;
    }
    return voxelValue;
  }

  protected void gotoData(int n, int nPoints) throws Exception {
    if (!params.blockCubeData)
      return;
    if (n > 0)
      Logger.info("skipping " + n + " data sets, " + nPoints + " points each");
    for (int i = 0; i < n; i++)
      skipData(nPoints);
  }

  protected void skipData(int nPoints) throws Exception {
    int iV = 0;
    while (iV < nPoints) {
      line = br.readLine();
      iV += countData(line);
    }
  }

  private int countData(String str) {
    int count = 0;
    int ich = 0;
    int ichMax = str.length();
    char ch;
    while (ich < ichMax) {
      while (ich < ichMax && ((ch = str.charAt(ich)) == ' ' || ch == '\t'))
        ++ich;
      if (ich < ichMax)
        ++count;
      while (ich < ichMax && ((ch = str.charAt(ich)) != ' ' && ch != '\t'))
        ++ich;
    }
    return count;
  }
  
}

