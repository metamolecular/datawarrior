package org.jmol.jvxl.api;

import javax.vecmath.Point3f;

import org.jmol.jvxl.data.MeshData;
import java.util.BitSet;

public interface MeshDataServer extends VertexDataServer {
  
  /*
   * An interface for interacting with 
   * the MarchingCubes and MarchingSquares classes 
   * as well as the SurfaceGenerator/SurfaceReader classes
   * during and after surface generation
   * 
   * Isosurface is an example.
   * 
   * SurfaceReader accepts vertexes from MarchingCubes/MarchingSquares
   * and then either consumes them or passes them on to Isosurface.
   * 
   * In addition, MeshData information is passed back and forth
   * via this mechanism.
   * 
   * This is crude. I would like to do it better.
   * 
   * Bob Hanson 20 Apr 2007
   * 
   */
  
  public abstract void invalidateTriangles();
  public abstract void fillMeshData(MeshData meshData, int mode);
  public abstract void notifySurfaceGenerationCompleted();
  public abstract void notifySurfaceMappingCompleted();
  public abstract Point3f[] calculateGeodesicSurface(BitSet bsSelected, float envelopeRadius);  
  public abstract int getColixArgb(short colix);
}
