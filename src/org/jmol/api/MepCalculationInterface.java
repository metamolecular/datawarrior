package org.jmol.api;

import java.util.BitSet;

import javax.vecmath.Point3f;


public interface MepCalculationInterface {

  public abstract void calculate(VolumeDataInterface volumeData, BitSet bsSelected,
                                 Point3f[] atomCoordAngstroms, float[] charges);

}
