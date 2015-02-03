/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-07-14 16:04:16 -0500 (Fri, 14 Jul 2006) $
 * $Revision: 5304 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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

public class CarbohydrateMonomer extends Monomer {

  final static byte[] alphaOffsets = { 0 };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    return new CarbohydrateMonomer(chain, group3, seqcode,
                            firstIndex, lastIndex, alphaOffsets);
  }
  
  ////////////////////////////////////////////////////////////////

  private CarbohydrateMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  public boolean isCarbohydrate() { return true; }

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    return true;
  }

}
