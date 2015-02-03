/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-02-26 22:47:27 -0600 (Mon, 26 Feb 2007) $
 * $Revision: 6957 $

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

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.util.Quaternion;
import org.jmol.viewer.JmolConstants;

public class NucleicMonomer extends PhosphorusMonomer {

  final static byte C6 = 1;
  private final static byte O2Pr = 2;
  private final static byte C5 = 3;
  private final static byte N1 = 4;
  private final static byte C2 = 5;
  private final static byte N3 = 6;
  private final static byte C4 = 7;
  private final static byte O2 = 8;
  private final static byte N7 = 9;
  private final static byte C8 = 10;
  private final static byte N9 = 11;  
  private final static byte O4 = 12;
  private final static byte O6 = 13;
  private final static byte N4 = 14;
  private final static byte NP = 15;
  private final static byte N6 = 16;
  private final static byte N2 = 17;
  private final static byte H5T = 18;
  private final static byte O5Pr = 19;
  private final static byte H3T = 20;
  private final static byte O3Pr = 21; 
  private final static byte C3Pr = 22;
  private final static byte O1P = 23; 
  private final static byte O2P = 24;
  //private final static byte S4 = 25;
  //private final static byte O5T = 26;
  //private final static byte OP1 = 27;
  //private final static byte OP2 = 28;
  //private final static byte HO3Pr = 29;
  //private final static byte HO5Pr = 30;
   
  // negative values are optional
  final static byte[] interestingNucleicAtomIDs = {
    ~JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS,    //  the lead, POSSIBLY P, maybe H5T or O5T 
    JmolConstants.ATOMID_C6,   // 1 the wing man, c6

    ~JmolConstants.ATOMID_O2_PRIME, // 2  O2' for RNA

    JmolConstants.ATOMID_C5,   //  3 C5
    JmolConstants.ATOMID_N1,   //  4 N1
    JmolConstants.ATOMID_C2,   //  5 C2
    JmolConstants.ATOMID_N3,   //  6 N3
    JmolConstants.ATOMID_C4,   //  7 C4

    ~JmolConstants.ATOMID_O2,  //  8 O2

    ~JmolConstants.ATOMID_N7,  // 9 N7
    ~JmolConstants.ATOMID_C8,  // 10 C8
    ~JmolConstants.ATOMID_N9,  // 11 C9

    ~JmolConstants.ATOMID_O4,  // 12 O4   U (& ! C5M)
    ~JmolConstants.ATOMID_O6,  // 13 O6   I (& ! N2)
    ~JmolConstants.ATOMID_N4,  // 14 N4   C
    ~JmolConstants.ATOMID_NUCLEIC_PHOSPHORUS, // 15 
    ~JmolConstants.ATOMID_N6,  // 16 N6   A
    ~JmolConstants.ATOMID_N2,  // 17 N2   G

    ~JmolConstants.ATOMID_H5T_TERMINUS, // 18 H5T terminus
    ~JmolConstants.ATOMID_O5_PRIME,     // 19 O5' terminus

    ~JmolConstants.ATOMID_H3T_TERMINUS, // 20 H3T terminus
    JmolConstants.ATOMID_O3_PRIME,      // 21 O3' terminus
    JmolConstants.ATOMID_C3_PRIME,      // 22 C3'
    
    ~JmolConstants.ATOMID_O1P,  // 23 Phosphorus O1
    ~JmolConstants.ATOMID_O2P,  // 24 Phosphorus O2
    
    // unused:

    //~JmolConstants.ATOMID_S4,  // 15 S4   tU
    //~JmolConstants.ATOMID_O5T_TERMINUS, // 26 O5T terminus

    // alternative designations:
    
    //~JmolConstants.ATOMID_OP1,  // 27 Phosphorus O1 (new)
    //~JmolConstants.ATOMID_OP2,  // 28 Phosphorus O2 (new)

    //~JmolConstants.ATOMID_HO3_PRIME, // 29 HO3' terminus (new)
    //~JmolConstants.ATOMID_HO5_PRIME, // 29 HO3' terminus (new)
    
  };

  public static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {

    byte[] offsets = scanForOffsets(firstAtomIndex,
                                    specialAtomIndexes,
                                    interestingNucleicAtomIDs);

    if (offsets == null)
      return null;
    if (!checkOptional(offsets, O5Pr, firstAtomIndex, 
        specialAtomIndexes[JmolConstants.ATOMID_O5T_TERMINUS]))
      return null;
    checkOptional(offsets, H3T, firstAtomIndex, 
        specialAtomIndexes[JmolConstants.ATOMID_HO3_PRIME]);
    checkOptional(offsets, H5T, firstAtomIndex, 
        specialAtomIndexes[JmolConstants.ATOMID_HO5_PRIME]);
    checkOptional(offsets, O1P, firstAtomIndex, 
        specialAtomIndexes[JmolConstants.ATOMID_OP1]);
    checkOptional(offsets, O2P, firstAtomIndex, 
        specialAtomIndexes[JmolConstants.ATOMID_OP2]);

    NucleicMonomer nucleicMonomer =
      new NucleicMonomer(chain, group3, seqcode,
                         firstAtomIndex, lastAtomIndex, offsets);
    return nucleicMonomer;
  }

  ////////////////////////////////////////////////////////////////

  private boolean hasRnaO2Prime;

  NucleicMonomer(Chain chain, String group3, int seqcode,
                 int firstAtomIndex, int lastAtomIndex,
                 byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
    if (offsets[NP] == -1 && (offsets[0] = offsets[H5T]) == -1)
        offsets[0] = offsets[O5Pr];
    this.hasRnaO2Prime = offsets[O2Pr] != -1;
    this.isPyrimidine = offsets[O2] != -1;
    this.isPurine =
      offsets[N7] != -1 && offsets[C8] != -1 && offsets[N9] != -1;
  }

  public boolean isNucleicMonomer() { return true; }

  public boolean isDna() { return !hasRnaO2Prime; }

  public boolean isRna() { return hasRnaO2Prime; }

  public boolean isPurine() { return isPurine; }

  public boolean isPyrimidine() { return isPyrimidine; }

  public boolean isGuanine() { return offsets[N2] != -1; }

  public byte getProteinStructureType() {
    return (hasRnaO2Prime
            ? JmolConstants.PROTEIN_STRUCTURE_RNA
            : JmolConstants.PROTEIN_STRUCTURE_DNA);
  }

  ////////////////////////////////////////////////////////////////


  Atom getN1() {
    return getAtomFromOffsetIndex(N1);
  }

  Atom getN3() {
    return getAtomFromOffsetIndex(N3);
  }

  Atom getN2() {
    return getAtomFromOffsetIndex(N2);
  }

  Atom getO2() {
    return getAtomFromOffsetIndex(O2);
  }

  Atom getO6() {
    return getAtomFromOffsetIndex(O6);
  }

  Atom getN4() {
    return getAtomFromOffsetIndex(N4);
  }

  Atom getN6() {
    return getAtomFromOffsetIndex(N6);
  }

  Atom getO4() {
    return getAtomFromOffsetIndex(O4);
  }

  Atom getTerminatorAtom() {
    return getAtomFromOffsetIndex(offsets[H3T] != -1 ? H3T : O3Pr);
  }

  private final static byte[] ring6OffsetIndexes = {C5, C6, N1, C2, N3, C4};

  public void getBaseRing6Points(Point3f[] ring6Points) {
    for (int i = 6; --i >= 0; )
      ring6Points[i] = getAtomFromOffsetIndex(ring6OffsetIndexes[i]);
  }
  
  private final static byte[] ring5OffsetIndexes = {C5, N7, C8, N9, C4};

  public boolean maybeGetBaseRing5Points(Point3f[] ring5Points) {
    if (isPurine)
      for (int i = 5; --i >= 0; )
        ring5Points[i] = getAtomFromOffsetIndex(ring5OffsetIndexes[i]);
    return isPurine;
  }

  ////////////////////////////////////////////////////////////////

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    Atom myPhosphorusAtom = getAtomFromOffsetIndex(NP);
    if (myPhosphorusAtom == null)
      return false;
    if (! (possiblyPreviousMonomer instanceof NucleicMonomer))
      return false;
    NucleicMonomer other = (NucleicMonomer)possiblyPreviousMonomer;
    if (other.getAtomFromOffsetIndex(O3Pr).isBonded(myPhosphorusAtom))
      return true;
    return super.isConnectedAfter(possiblyPreviousMonomer);
  }

  ////////////////////////////////////////////////////////////////

  public void findNearestAtomIndex(int x, int y, Atom[] closest,
                            short madBegin, short madEnd) {
    Atom competitor = closest[0];
    Atom lead = getLeadAtom();
    Atom o5prime = getAtomFromOffsetIndex(O5Pr);
    Atom c3prime = getAtomFromOffsetIndex(C3Pr);
    short mar = (short)(madBegin / 2);
    if (mar < 1900)
      mar = 1900;
    int radius = scaleToScreen(lead.screenZ, mar);
    if (radius < 4)
      radius = 4;
    if (isCursorOnTopOf(lead, x, y, radius, competitor)
        || isCursorOnTopOf(o5prime, x, y, radius, competitor)
        || isCursorOnTopOf(c3prime, x, y, radius, competitor))
      closest[0] = lead;
  }
  
 public void setModelClickability() {
    Atom atom;
    if (isAtomHidden(getLeadAtomIndex()))
      return;
    for (int i = 6; --i >= 0;) {
      atom = getAtomFromOffsetIndex(ring6OffsetIndexes[i]);
      atom.setClickable(JmolConstants.CARTOON_VISIBILITY_FLAG);
    }
    if (isPurine)
      for (int i = 5; --i >= 0;) {
        atom = getAtomFromOffsetIndex(ring5OffsetIndexes[i]);
        atom.setClickable(JmolConstants.CARTOON_VISIBILITY_FLAG);
      }
  }
 
 Atom getQuaternionFrameCenter(char qType) {
   return (getAtomFromOffsetIndex(isPurine ? N9 : N1));
 }
 
 public Object getHelixData(int tokType, char qType, int mStep) {
   return getHelixData2(tokType, qType, mStep);
 }
 

 public Quaternion getQuaternion(char qType) {
   /*
    * also AminoMonomer
    *   
    */
    
   /*
   Point3f ptP = getP(); 
   Point3f ptO1P = getO1P();
   Point3f ptO2P = getO2P();
   if(ptP == null || ptO1P == null || ptO2P == null)
     return null;
   //vA = ptO1P - ptP
   Vector3f vA = new Vector3f(ptO1P);
   vA.sub(ptP);
   
   //vB = ptO2P - ptP
   Vector3f vB = new Vector3f(ptO2P);
   vB.sub(ptP);
   return Quaternion.getQuaternionFrame(vA, vB);   
   
   */
   
   //if (m.getLeadAtom().getElementSymbol() != "P")
     //return null;
   Point3f ptA, ptB;
   Point3f ptN = getQuaternionFrameCenter(qType);
   if (isPurine) {
     // vA = N9--C4
     // vB = N9--C8
     ptA = getAtomFromOffsetIndex(C4);
     ptB = getAtomFromOffsetIndex(C8);
   } else {
     // vA = N1--C2
     // vB = N1--C6
     ptA = getAtomFromOffsetIndex(C2);
     ptB = getAtomFromOffsetIndex(C6);
   }
   if(ptN == null || ptA == null || ptB == null)
     return null;

   Vector3f vA = new Vector3f(ptA);
   vA.sub(ptN);
   
   Vector3f vB = new Vector3f(ptB);
   vB.sub(ptN);
   return Quaternion.getQuaternionFrame(vA, vB, null);
 }

}
