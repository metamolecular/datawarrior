/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2008-09-16 22:39:58 -0500 (Tue, 16 Sep 2008) $
 * $Revision: 9905 $

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

package org.jmol.modelset;

import org.jmol.util.Logger;

public class HBond extends Bond {

  private float energy;
  private byte paletteID;
  
  HBond(Atom atom1, Atom atom2, short order, short mad, short colix, float energy) {
    super(atom1, atom2, order, mad, colix);
    if (Logger.debugging)
       Logger.debug("HBond energy = " + energy + " for #" + atom1.getAtomIndex() + " " + atom1.getInfoXYZ(false) + ", #" + atom2.getAtomIndex() + " " + atom2.getInfoXYZ(false));
    this.energy = energy;
  }
  
  public float getEnergy() {
    return energy;
  }
  
  public byte getPaletteId() {
    return paletteID;
  }
  
  public void setPaletteID(byte paletteID) {
    this.paletteID = paletteID;
  }

}
