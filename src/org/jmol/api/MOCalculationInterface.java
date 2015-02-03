package org.jmol.api;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;


public interface MOCalculationInterface {

  public abstract void calculate(VolumeDataInterface volumeData, BitSet bsSelected,
                                 String calculationType,
                                 Point3f[] atomCoordAngstroms,
                                 int firstAtomOffset, Vector shells,
                                 float[][] gaussians, Hashtable aoOrdersDF,
                                 int[][] slaterInfo, float[][] slaterData,
                                 float[] moCoefficients, float[] nuclearCharges);
  
  public abstract void calculateElectronDensity(float[] nuclearCharges);
}
