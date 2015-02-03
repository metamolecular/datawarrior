package org.jmol.api;

import java.util.BitSet;

import org.jmol.modelset.ModelSet;

public interface SmilesMatcherInterface {

  public abstract void setModelSet(ModelSet modelSet);
  public abstract BitSet getSubstructureSet(String smiles)
      throws Exception;
}
