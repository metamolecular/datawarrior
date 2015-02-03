package org.jmol.api;

import java.util.BitSet;

public interface MinimizerInterface {

  public abstract boolean minimize(int steps, double crit, BitSet bsSelected)
      throws Exception;
  public abstract void setProperty(String propertyName, Object propertyValue);
  public abstract Object getProperty(String propertyName, int param);
}
