/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-15 11:45:59 -0600 (Thu, 15 Feb 2007) $
 * $Revision: 6834 $
 *
 * Copyright (C) 2004-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.modelsetbio;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.viewer.JmolConstants;

public class PhosphorusMonomer extends Monomer {

  private final static byte[] phosphorusOffsets = { 0 };

  private static float MAX_ADJACENT_PHOSPHORUS_DISTANCE = 8.0f;
 
  protected boolean isPurine;
  protected boolean isPyrimidine;

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    //Logger.debug("PhosphorusMonomer.validateAndAllocate");
    if (firstIndex != lastIndex ||
        specialAtomIndexes[JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS]
        != firstIndex)
      return null;
    return new PhosphorusMonomer(chain, group3, seqcode,
                            firstIndex, lastIndex, phosphorusOffsets);
  }
  
  ////////////////////////////////////////////////////////////////

  protected PhosphorusMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
    if (group3.indexOf('T') >= 0)
      chain.setIsDna(true);
    if (group3.indexOf('U') + group3.indexOf('I') > -2)
        chain.setIsRna(true);
    isPurine = (group3.indexOf('A') + group3.indexOf('G') + group3.indexOf('I') > -3);
    isPyrimidine = (group3.indexOf('T') + group3.indexOf('C') + group3.indexOf('U') > -3);
  }

  boolean isPhosphorusMonomer() { return true; }

  public boolean isDna() { return chain.isDna(); }

  public boolean isRna() { return chain.isRna(); }

  public boolean isPurine() { return isPurine; }
  public boolean isPyrimidine() { return isPyrimidine; }

  public Object getStructure() { return chain; }

  public byte getProteinStructureType() {
    return JmolConstants.PROTEIN_STRUCTURE_NONE;
  }
/*
  public Atom getAtom(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS
            ? getLeadAtom()
            : null);
  }

  public Point3f getAtomPoint(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS
            ? getLeadAtomPoint()
            : null);
  }
*/
  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof PhosphorusMonomer))
      return false;
    // 1PN8 73:d and 74:d are 7.001 angstroms apart
    // but some P atoms are up to 7.4 angstroms apart
    float distance =
      getLeadAtomPoint().distance(possiblyPreviousMonomer.getLeadAtomPoint());
    return distance <= MAX_ADJACENT_PHOSPHORUS_DISTANCE;
  }
}
