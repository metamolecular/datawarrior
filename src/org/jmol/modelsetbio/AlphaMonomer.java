/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-15 11:45:59 -0600 (Thu, 15 Feb 2007) $
 * $Revision: 6834 $
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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.util.Quaternion;
import org.jmol.viewer.JmolConstants;

public class AlphaMonomer extends Monomer {

  final static byte[] alphaOffsets = { 0 };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstIndex, int lastIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    if (firstIndex != lastIndex ||
        specialAtomIndexes[JmolConstants.ATOMID_ALPHA_CARBON] != firstIndex)
      return null;
    return new AlphaMonomer(chain, group3, seqcode,
                            firstIndex, lastIndex, alphaOffsets);
  }
  
  ////////////////////////////////////////////////////////////////

  AlphaMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isAlphaMonomer() { return true; }

  protected ProteinStructure proteinStructure;
  protected Point3f nitrogenHydrogenPoint;
  
  public ProteinStructure getProteinStructure() { return proteinStructure; }

  public Object getStructure() { return getProteinStructure(); }

  void setStructure(ProteinStructure proteinStructure) {
    this.proteinStructure = proteinStructure;
    if (proteinStructure == null)
      nitrogenHydrogenPoint = null;
  }
  
  public void setProteinStructureId(int id) {
    if (proteinStructure != null)
      proteinStructure.uniqueID = id;
  }

  public byte getProteinStructureType() {
    return proteinStructure == null ? JmolConstants.PROTEIN_STRUCTURE_NONE
        : proteinStructure.type;
  }

  public int getStrucNo() {
    return proteinStructure != null ? proteinStructure.uniqueID : 0;
  }

  public boolean isHelix() {
    return proteinStructure != null &&
      proteinStructure.type == JmolConstants.PROTEIN_STRUCTURE_HELIX;
  }

  public boolean isSheet() {
    return proteinStructure != null &&
      proteinStructure.type == JmolConstants.PROTEIN_STRUCTURE_SHEET;
  }

  /**
   * 
   * @param iType
   * @param monomerIndexCurrent   a pointer to the current ProteinStructure
   * @return                      a pointer to this ProteinStructure
   */
  public int setProteinStructureType(byte iType, int monomerIndexCurrent) {
    if (monomerIndexCurrent < 0 
        || monomerIndexCurrent > 0 && monomerIndex == 0) {
      if (proteinStructure != null) {
        int nAbandoned = proteinStructure.removeMonomer(monomerIndex);
        if (nAbandoned > 0)
          getBioPolymer().removeProteinStructure(monomerIndex + 1, nAbandoned);
      }
      switch (iType) {
      case JmolConstants.PROTEIN_STRUCTURE_HELIX:
        setStructure(new Helix((AlphaPolymer) bioPolymer, monomerIndex, 1, 0));
        break;
      case JmolConstants.PROTEIN_STRUCTURE_SHEET:
        setStructure(new Sheet((AlphaPolymer) bioPolymer, monomerIndex, 1, 0));
        break;
      case JmolConstants.PROTEIN_STRUCTURE_TURN:
        setStructure(new Turn((AlphaPolymer) bioPolymer, monomerIndex, 1, 0));
        break;
      case JmolConstants.PROTEIN_STRUCTURE_NONE:
        setStructure(null);
      }
    } else {
      setStructure(getBioPolymer().getProteinStructure(monomerIndexCurrent));
      if (proteinStructure != null)
        proteinStructure.addMonomer(monomerIndex);
    }
    return monomerIndex;
  }
  
  final public Atom getAtom(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_ALPHA_CARBON
            ? getLeadAtom()
            : null);
  }

  final public Point3f getAtomPoint(byte specialAtomID) {
    return (specialAtomID == JmolConstants.ATOMID_ALPHA_CARBON
            ? getLeadAtomPoint()
            : null);
  }

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof AlphaMonomer))
      return false;
    Atom atom1 = getLeadAtom();
    Atom atom2 = possiblyPreviousMonomer.getLeadAtom();
    return atom1.isBonded(atom2) || atom1.distance(atom2) <= 4.2f;
    // jan reichert in email to miguel on 10 May 2004 said 4.2 looked good
  }
  
  Atom getQuaternionFrameCenter(char qType) {
    switch (qType) {
    default:
    case 'a':
    case 'c':
    case 'C':
      return getLeadAtom();
    case 'q':
    case 'p':
    case 'P':
    case 'n':
      return null;
    }
  }

  public Object getHelixData(int tokType, char qType, int mStep) {
    return getHelixData2(tokType, qType, mStep);
  }
  
  public Quaternion getQuaternion(char qType) {
    /*
     * also NucleicMonomer, AminoMonomer
     * 
     * This definition is only for alpha-only chains
     *   
     */
    
    Vector3f vA = new Vector3f();
    Vector3f vB = new Vector3f();
    Vector3f vC = null;

    switch (qType) {
    case 'a':
      //vA = ptCa(i+1) - ptCa
      //vB = ptCa(i-1) - ptCa
      if (monomerIndex == 0 
          || monomerIndex == bioPolymer.monomerCount - 1)
        return null;
      Point3f ptCa = getLeadAtomPoint();
      Point3f ptCaNext = bioPolymer.getLeadPoint(monomerIndex + 1);
      Point3f ptCaPrev = bioPolymer.getLeadPoint(monomerIndex - 1);
      vA.sub(ptCaNext, ptCa);
      vB.sub(ptCaPrev, ptCa);
      break;
    default:
    case 'c':
    case 'n':
    case 'p':
      return null;
    }
    return Quaternion.getQuaternionFrame(vA, vB, vC);
  }
  

}
