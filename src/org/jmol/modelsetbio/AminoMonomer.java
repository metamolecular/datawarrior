/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-12-11 13:29:38 -0600 (Mon, 11 Dec 2006) $
 * $Revision: 6442 $
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

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Bond;
import org.jmol.modelset.Chain;
import org.jmol.util.Logger;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;

public class AminoMonomer extends AlphaMonomer {

  private final static byte CA = 0;
  private final static byte O = 1;
  private final static byte N = 2;
  private final static byte C = 3;
  private final static byte OT = 4;
  //private final static byte O1 = 5;
  
  // negative values are optional
  final static byte[] interestingAminoAtomIDs = {
    JmolConstants.ATOMID_ALPHA_CARBON,      // 0 CA alpha carbon
    ~JmolConstants.ATOMID_CARBONYL_OXYGEN,   // 1 O wing man
    JmolConstants.ATOMID_AMINO_NITROGEN,    // 2 N
    JmolConstants.ATOMID_CARBONYL_CARBON,   // 3 C  point man
    ~JmolConstants.ATOMID_TERMINATING_OXT,  // 4 OXT
    ~JmolConstants.ATOMID_O1,               // 5 O1
  };

  static Monomer
    validateAndAllocate(Chain chain, String group3, int seqcode,
                        int firstAtomIndex, int lastAtomIndex,
                        int[] specialAtomIndexes, Atom[] atoms) {
    byte[] offsets = scanForOffsets(firstAtomIndex, specialAtomIndexes,
                                    interestingAminoAtomIDs);
    if (offsets == null)
      return null;
    checkOptional(offsets, O, firstAtomIndex, specialAtomIndexes[JmolConstants.ATOMID_O1]);
    if (atoms[firstAtomIndex].isHetero() && !isBondedCorrectly(firstAtomIndex, offsets, atoms)) 
      return null;
    AminoMonomer aminoMonomer =
      new AminoMonomer(chain, group3, seqcode,
                       firstAtomIndex, lastAtomIndex, offsets);
    return aminoMonomer;
  }

  private static boolean isBondedCorrectly(int offset1, int offset2,
                                   int firstAtomIndex,
                                   byte[] offsets, Atom[] atoms) {
    int atomIndex1 = firstAtomIndex + (offsets[offset1] & 0xFF);
    int atomIndex2 = firstAtomIndex + (offsets[offset2] & 0xFF);
    if (atomIndex1 >= atomIndex2)
      return false;
    return atoms[atomIndex1].isBonded(atoms[atomIndex2]);
  }

  private static boolean isBondedCorrectly(int firstAtomIndex, byte[] offsets,
                                 Atom[] atoms) {
    return (isBondedCorrectly(N, CA, firstAtomIndex, offsets, atoms)
            && isBondedCorrectly(CA, C, firstAtomIndex, offsets, atoms)
            && (offsets[O] == -1 
                || isBondedCorrectly(C, O, firstAtomIndex, offsets, atoms))
            );
  }
  
  ////////////////////////////////////////////////////////////////

  private AminoMonomer(Chain chain, String group3, int seqcode,
               int firstAtomIndex, int lastAtomIndex,
               byte[] offsets) {
    super(chain, group3, seqcode,
          firstAtomIndex, lastAtomIndex, offsets);
  }

  boolean isAminoMonomer() { return true; }

  Atom getNitrogenAtom() {
    return getAtomFromOffsetIndex(N);
  }

  Point3f getNitrogenAtomPoint() {
    return getAtomFromOffsetIndex(N);
  }

  Atom getCarbonylCarbonAtom() {
    return getAtomFromOffsetIndex(C);
  }

  Point3f getCarbonylCarbonAtomPoint() {
    return getAtomFromOffsetIndex(C);
  }

  Atom getCarbonylOxygenAtom() {
    return getWingAtom();
  }

  Point3f getCarbonylOxygenAtomPoint() {
    return getWingAtomPoint();
  }

  Atom getInitiatorAtom() {
    return getNitrogenAtom();
  }

  Atom getTerminatorAtom() {
    return getAtomFromOffsetIndex(offsets[OT] != -1 ? OT : C);
  }

  boolean hasOAtom() {
    return offsets[O] != -1;
  }
  
  ////////////////////////////////////////////////////////////////

  boolean isConnectedAfter(Monomer possiblyPreviousMonomer) {
    if (possiblyPreviousMonomer == null)
      return true;
    if (! (possiblyPreviousMonomer instanceof AminoMonomer))
      return false;
    AminoMonomer other = (AminoMonomer)possiblyPreviousMonomer;
    return other.getCarbonylCarbonAtom().isBonded(getNitrogenAtom());
  }

  ////////////////////////////////////////////////////////////////

  void findNearestAtomIndex(int x, int y, Atom[] closest,
                            short madBegin, short madEnd) {
    
    Atom competitor = closest[0];
    Atom nitrogen = getNitrogenAtom();
    short marBegin = (short) (madBegin / 2);
    if (marBegin < 1200)
      marBegin = 1200;
    if (nitrogen.screenZ == 0)
      return;
    int radiusBegin = scaleToScreen(nitrogen.screenZ, marBegin);
    if (radiusBegin < 4)
      radiusBegin = 4;
    Atom ccarbon = getCarbonylCarbonAtom();
    short marEnd = (short) (madEnd / 2);
    if (marEnd < 1200)
      marEnd = 1200;
    int radiusEnd = scaleToScreen(nitrogen.screenZ, marEnd);
    if (radiusEnd < 4)
      radiusEnd = 4;
    Atom alpha = getLeadAtom();
    if (isCursorOnTopOf(alpha, x, y, (radiusBegin + radiusEnd) / 2,
        competitor)
        || isCursorOnTopOf(nitrogen, x, y, radiusBegin, competitor)
        || isCursorOnTopOf(ccarbon, x, y, radiusEnd, competitor))
      closest[0] = alpha;
  }

  boolean nhChecked = false;

  public void resetHydrogenPoint() {
    nhChecked = false;
    nitrogenHydrogenPoint = null;
  }

  Point3f getNitrogenHydrogenPoint() {
    if (nitrogenHydrogenPoint == null && !nhChecked) {
      nhChecked = true;
      Atom nitrogen = getNitrogenAtom();
      Atom h = null;
      Bond[] bonds = nitrogen.getBonds();
      for (int i = 0; i < bonds.length; i++)
        if ((h = bonds[i].getOtherAtom(nitrogen)).getElementNumber() == 1)
          return (nitrogenHydrogenPoint = h);
    }
    return nitrogenHydrogenPoint;
  }
  
  public boolean getNHPoint(Point3f aminoHydrogenPoint, Vector3f vNH) {
    if (monomerIndex == 0 || getGroupID() == JmolConstants.GROUPID_PROLINE) 
      return false;      
    Point3f nitrogenPoint = getNitrogenAtomPoint();
    Point3f nhPoint = getNitrogenHydrogenPoint();
    if (nhPoint != null) {
      vNH.sub(nhPoint, nitrogenPoint);
      aminoHydrogenPoint.set(nhPoint);
      return true;
    }
    vNH.sub(nitrogenPoint, getLeadAtomPoint());
    vNH.add(nitrogenPoint);
    vNH.sub(((AminoMonomer)bioPolymer.monomers[monomerIndex - 1]).getCarbonylCarbonAtomPoint());
    vNH.normalize();
    aminoHydrogenPoint.add(nitrogenPoint, vNH);
    this.nitrogenHydrogenPoint = new Point3f(aminoHydrogenPoint);
    if (Logger.debugging)
      Logger.info("draw pta" + monomerIndex + " {" + aminoHydrogenPoint.x + " " + aminoHydrogenPoint.y + " " + aminoHydrogenPoint.z + "} color red#aminoPolymer.calchbonds");
    return true;
  }

  final private Point3f ptTemp = new Point3f();
  final private static float beta = (float) (17 * Math.PI/180);
  
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
      return getCarbonylCarbonAtom();
    case 'n':
      return getNitrogenAtom();
    }
  }

  public Quaternion getQuaternion(char qType) {
    /*
     * also NucleicMonomer
     *  
     * see:
     * 
     *  Hanson and Thakur: http://www.cs.indiana.edu/~hanson/  http://www.cs.indiana.edu/~sithakur/
     *  
     *  Albrecht, Hart, Shaw, Dunker: 
     *  
     *   Contact Ribbons: a New Tool for Visualizing Intra- and Intermolecular Interactions in Proteins
     *   Electronic Proceedings for the 1996 Pacific Symposium on Biocomputing
     *   http://psb.stanford.edu/psb-online/proceedings/psb96/albrecht.pdfx
     *   
     *  Kneller and Calligari:
     *  
     *   Efficient characterization of protein secondary structure in terms of screw motion
     *   Acta Cryst. (2006). D62, 302-311    [ doi:10.1107/S0907444905042654 ]
     *   http://scripts.iucr.org/cgi-bin/paper?ol5289
     * 
     *  Wang and Zang:
     *   
     *   Protein secondary structure prediction with Bayesian learning method
     *   http://cat.inist.fr/?aModele=afficheN&cpsidt=15618506
     *
     *  Geetha:
     *  
     *   Distortions in protein helices
     *   International Journal of Biological Macromolecules, Volume 19, Number 2, August 1996 , pp. 81-89(9)
     *   http://www.ingentaconnect.com/content/els/01418130/1996/00000019/00000002/art01106
     *   DOI: 10.1016/0141-8130(96)01106-3
     *    
     *  Kavraki:
     *  
     *   Representing Proteins in Silico and Protein Forward Kinematics
     *   http://cnx.org/content/m11621/latest
     *   
     */
    
    Point3f ptC = getCarbonylCarbonAtomPoint();
    Point3f ptCa = getLeadAtomPoint();
    Vector3f vA = new Vector3f();
    Vector3f vB = new Vector3f();
    Vector3f vC = null;
    
    switch (qType) {
    case 'a':
      return super.getQuaternion('a');
    case 'c':
      //vA = ptC - ptCa
      //vB = ptN - ptCa
      vA.sub(ptC, ptCa);
      vB.sub(getNitrogenAtomPoint(), ptCa);
      break;
    default:
    case 'p':
      //Bob's idea for a peptide plane frame
      //vA = ptCa - ptC
      //vB = ptN' - ptC
      vA.sub(ptCa, ptC);
      if (monomerIndex == bioPolymer.monomerCount - 1)
        return null;
      vB.sub(((AminoMonomer) bioPolymer.getMonomers()[monomerIndex + 1]).getNitrogenAtomPoint(), ptC);
      break;
    case 'n':
      // amino nitrogen chemical shift tensor frame      
      // vA = ptH - ptN rotated beta (17 degrees) clockwise (-) around Y (perp to plane)
      // vB = ptCa - ptN
      if (monomerIndex == 0 || getGroupID() == JmolConstants.GROUPID_PROLINE)
        return null;
      vC = new Vector3f();
      getNHPoint(ptTemp, vC);
      vB.sub(ptCa, getNitrogenAtomPoint());
      vB.cross(vC, vB);
      Matrix3f mat = new Matrix3f();
      mat.set(new AxisAngle4f(vB, -beta));
      mat.transform(vC);
      vA.cross(vB, vC);
      break;
    }
    return Quaternion.getQuaternionFrame(vA, vB, vC);
  }
  
  public boolean isWithinStructure(byte type) {
    ProteinStructure s = (ProteinStructure) getStructure();
    return (s != null && s.isWithin(monomerIndex) && s.type == type);
  }
  
  public String getStructureId() {
    if (proteinStructure == null || proteinStructure.structureID == null)
      return "";
    return proteinStructure.structureID;
  }
  
  public String getProteinStructureTag() {
    if (proteinStructure == null || proteinStructure.structureID == null)
      return null;
    String tag = "%3N %2ID";
    tag = TextFormat.formatString(tag, "N", proteinStructure.serialID);
    tag = TextFormat.formatString(tag, "ID", proteinStructure.structureID);
    if (proteinStructure.type == JmolConstants.PROTEIN_STRUCTURE_SHEET)
      tag += " " + proteinStructure.strandCount;
    return tag;
  }
  
}
