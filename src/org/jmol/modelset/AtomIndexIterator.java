package org.jmol.modelset;

public interface AtomIndexIterator {
  boolean hasNext();
  int next();
  void release();
  float foundDistance2();
}
