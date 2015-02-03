  /* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-04-24 08:15:07 -0500 (Tue, 24 Apr 2007) $
 * $Revision: 7479 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
import org.jmol.modelset.Group;
import org.jmol.modelset.LabelToken;
import org.jmol.modelset.Model;
import org.jmol.modelset.Polymer;
import org.jmol.util.BitSetUtil; //import org.jmol.util.Escape;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;
import org.jmol.viewer.Token;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

public abstract class BioPolymer extends Polymer {

  Monomer[] monomers;

  public Monomer[] getMonomers() {
    return monomers;
  }

  int monomerCount;

  public int getMonomerCount() {
    return monomerCount;
  }

  protected Model model;

  BioPolymer(Monomer[] monomers) {
    this.monomers = monomers;
    monomerCount = monomers.length;
    for (int i = monomerCount; --i >= 0;)
      monomers[i].setBioPolymer(this, i);
    model = monomers[0].getModel();
  }

  static BioPolymer allocateBioPolymer(Group[] groups, int firstGroupIndex,
                                       boolean checkConnections) {
    Monomer previous = null;
    int count = 0;
    for (int i = firstGroupIndex; i < groups.length; ++i) {
      Group group = groups[i];
      Monomer current;
      if (!(group instanceof Monomer)
          || (current = (Monomer) group).bioPolymer != null || previous != null
          && previous.getClass() != current.getClass() || checkConnections
          && !current.isConnectedAfter(previous))
        break;
      previous = current;
      count++;
    }
    if (count == 0)
      return null;
    Monomer[] monomers = new Monomer[count];
    for (int j = 0; j < count; ++j)
      monomers[j] = (Monomer) groups[firstGroupIndex + j];
    if (previous instanceof AminoMonomer)
      return new AminoPolymer(monomers);
    if (previous instanceof AlphaMonomer)
      return new AlphaPolymer(monomers);
    if (previous instanceof NucleicMonomer)
      return new NucleicPolymer(monomers);
    if (previous instanceof PhosphorusMonomer)
      return new PhosphorusPolymer(monomers);
    if (previous instanceof CarbohydrateMonomer)
      return new CarbohydratePolymer(monomers);
    Logger
        .error("Polymer.allocatePolymer() ... no matching polymer for monomor "
            + previous);
    throw new NullPointerException();
  }

  public void clearStructures() {
    for (int i = 0; i < monomerCount; i++)
      monomers[i].setStructure(null);
  }

  void removeProteinStructure(int monomerIndex, int count) {
    // System.out.println("biopolymer removeProteinStructure mIndex " +
    // monomerIndex + " count " + count);
    for (int i = 0, pt = monomerIndex; i < count && pt < monomerCount; i++, pt++)
      monomers[pt].setStructure(null);
  }

  public int[] getLeadAtomIndices() {
    if (leadAtomIndices == null) {
      leadAtomIndices = new int[monomerCount];
      for (int i = monomerCount; --i >= 0;)
        leadAtomIndices[i] = monomers[i].getLeadAtomIndex();
    }
    return leadAtomIndices;
  }

  int getIndex(char chainID, int seqcode) {
    int i;
    for (i = monomerCount; --i >= 0;)
      if (monomers[i].getChainID() == chainID) {
        // System.out.println("BioPolymer getIndex seqcode monomers[i].seqcode "
        // + chainID + " "
        // + Group.getSeqcodeString(seqcode) + " "
        // + Group.getSeqcodeString(monomers[i].getSeqcode()));
        if (monomers[i].getSeqcode() == seqcode)
          break;
      }
    return i;
  }

  final Point3f getLeadPoint(int monomerIndex) {
    return monomers[monomerIndex].getLeadAtomPoint();
  }

  final Point3f getInitiatorPoint() {
    return monomers[0].getInitiatorAtom();
  }

  final Point3f getTerminatorPoint() {
    return monomers[monomerCount - 1].getTerminatorAtom();
  }

  /*
   * public final Atom getLeadAtom(int monomerIndex) { return
   * monomers[monomerIndex].getLeadAtom(); }
   */
  void getLeadMidPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == monomerCount) {
      --groupIndex;
    } else if (groupIndex > 0) {
      midPoint.set(getLeadPoint(groupIndex));
      midPoint.add(getLeadPoint(groupIndex - 1));
      midPoint.scale(0.5f);
      return;
    }
    midPoint.set(getLeadPoint(groupIndex));
  }

  void getLeadPoint(int groupIndex, Point3f midPoint) {
    if (groupIndex == monomerCount)
      --groupIndex;
    midPoint.set(getLeadPoint(groupIndex));
  }

  boolean hasWingPoints() {
    return false;
  }

  // this might change in the future ... if we calculate a wing point
  // without an atom for an AlphaPolymer
  final Point3f getWingPoint(int polymerIndex) {
    return monomers[polymerIndex].getWingAtomPoint();
  }

  final Point3f getPointPoint(int polymerIndex) {
    return monomers[polymerIndex].getPointAtomPoint();
  }

  public void setConformation(BitSet bsSelected, int nAltLocsInModel) {
    for (int i = monomerCount; --i >= 0;)
      monomers[i].updateOffsetsForAlternativeLocations(bsSelected,
          nAltLocsInModel);
    recalculateLeadMidpointsAndWingVectors();
    // calculateStructures();
  }

  public void recalculateLeadMidpointsAndWingVectors() {
    leadAtomIndices = null;
    sheetPoints = null;
    getLeadAtomIndices();
    ProteinStructure ps;
    ProteinStructure psLast = null;
    for (int i = 0; i < monomerCount; i++) {
      if ((ps = getProteinStructure(i)) != null && ps != psLast)
        (psLast = ps).resetAxes();
      monomers[i].resetHydrogenPoint();
    }
    calcLeadMidpointsAndWingVectors(false);
  }

  public Point3f[] getLeadMidpoints() {
    if (leadMidpoints == null)
      calcLeadMidpointsAndWingVectors(true);
    return leadMidpoints;
  }

  Point3f[] getLeadPoints() {
    if (leadPoints == null)
      calcLeadMidpointsAndWingVectors(true);
    return leadPoints;
  }

  public Point3f[] getControlPoints(boolean isTraceAlpha, float sheetSmoothing,
                                    boolean invalidate) {
    if (invalidate)
      sheetPoints = null;
    if (!isTraceAlpha)
      return leadMidpoints;
    else if (sheetSmoothing == 0)
      return leadPoints;
    return getSheetPoints(sheetSmoothing);
  }

  private float sheetSmoothing;

  private Point3f[] getSheetPoints(float sheetSmoothing) {
    if (sheetPoints != null && sheetSmoothing == this.sheetSmoothing)
      return sheetPoints;
    sheetPoints = new Point3f[monomerCount + 1];
    getLeadPoints();
    for (int i = 0; i < monomerCount; i++)
      sheetPoints[i] = new Point3f();
    Vector3f v = new Vector3f();
    for (int i = 0; i < monomerCount; i++) {
      if (monomers[i].isSheet()) {
        v.sub(leadMidpoints[i], leadPoints[i]);
        v.scale(sheetSmoothing);
        sheetPoints[i].add(leadPoints[i], v);
      } else {
        sheetPoints[i] = leadPoints[i];
      }
    }
    sheetPoints[monomerCount] = sheetPoints[monomerCount - 1];
    this.sheetSmoothing = sheetSmoothing;
    return sheetPoints;
  }

  public final Vector3f[] getWingVectors() {
    if (leadMidpoints == null) // this is correct ... test on leadMidpoints
      calcLeadMidpointsAndWingVectors(true);
    return wingVectors; // wingVectors might be null ... before autocalc
  }

  private final void calcLeadMidpointsAndWingVectors(boolean getNewPoints) {
    int count = monomerCount;
    if (leadMidpoints == null || getNewPoints) {
      leadMidpoints = new Point3f[count + 1];
      leadPoints = new Point3f[count + 1];
      wingVectors = new Vector3f[count + 1];
      sheetSmoothing = Float.MIN_VALUE;
    }

    boolean hasWingPoints = hasWingPoints();
    // if (model.getModelSet().viewer.getTestFlag1()) hasWingPoints = false;

    Vector3f vectorA = new Vector3f();
    Vector3f vectorB = new Vector3f();
    Vector3f vectorC = new Vector3f();
    Vector3f vectorD = new Vector3f();

    Point3f leadPointPrev, leadPoint;
    leadMidpoints[0] = getInitiatorPoint();
    leadPoints[0] = leadPoint = getLeadPoint(0);
    Vector3f previousVectorD = null;
    // proteins:
    // C O (wing)
    // \ |
    // CA--N--C O (wing)
    // (lead) \ |
    // CA--N--C
    // (lead) \
    // CA--N
    // (lead)
    // mon# 2 1 0
    for (int i = 1; i < count; ++i) {
      leadPointPrev = leadPoint;
      leadPoints[i] = leadPoint = getLeadPoint(i);
      Point3f midpoint = new Point3f(leadPoint);
      midpoint.add(leadPointPrev);
      midpoint.scale(0.5f);
      leadMidpoints[i] = midpoint;
      if (hasWingPoints) {
        vectorA.sub(leadPoint, leadPointPrev);
        vectorB.sub(leadPointPrev, getWingPoint(i - 1));
        vectorC.cross(vectorA, vectorB);
        vectorD.cross(vectorA, vectorC);
        vectorD.normalize();
        if (previousVectorD != null
            && previousVectorD.angle(vectorD) > Math.PI / 2)
          vectorD.scale(-1);
        previousVectorD = wingVectors[i] = new Vector3f(vectorD);
      }
    }
    leadPoints[count] = leadMidpoints[count] = getTerminatorPoint();
    if (!hasWingPoints) {
      if (count < 3) {
        wingVectors[1] = unitVectorX;
      } else {
        // auto-calculate wing vectors based upon lead atom positions only
        Vector3f previousVectorC = null;
        for (int i = 1; i < count; ++i) {
          // perfect for traceAlpha on; reasonably OK for traceAlpha OFF
          vectorA.sub(leadMidpoints[i], leadPoints[i]);
          vectorB.sub(leadPoints[i], leadMidpoints[i + 1]);
          vectorC.cross(vectorA, vectorB);
          vectorC.normalize();
          if (previousVectorC != null
              && previousVectorC.angle(vectorC) > Math.PI / 2)
            vectorC.scale(-1);
          previousVectorC = wingVectors[i] = new Vector3f(vectorC);
        }
      }
    }
    wingVectors[0] = wingVectors[1];
    wingVectors[count] = wingVectors[count - 1];
    /*
     * Point3f pt = leadPoints[11]; vectorC.set(wingVectors[11]);
     * vectorC.add(pt); //order of points is mid11 lead11 mid12 lead12
     * System.out.println("draw pt" + 11 + "b " +
     * Escape.escape(leadMidpoints[11]) + " color yellow");
     * System.out.println("draw pt" + 11 + " " + Escape.escape(leadPoints[11]) +
     * " color red"); System.out.println("draw pt" + 12 + "b " +
     * Escape.escape(leadMidpoints[12]) + " color blue");
     * System.out.println("draw pt" + 12 + " " + Escape.escape(leadPoints[12]) +
     * " color green"); System.out.println("draw v" + 11 + " arrow " +
     * Escape.escape(pt) + " " + Escape.escape(vectorC));
     * System.out.println("draw plane" + 11 + " " +
     * Escape.escape(leadPoints[11]) + " " + Escape.escape(leadMidpoints[11]) +
     * " "+ Escape.escape(leadMidpoints[12]));
     * 
     * pt = leadMidpoints[11]; vectorC.set(wingVectors[11]); vectorC.add(pt);
     * System.out.println("draw v" + 11 + "b arrow " + Escape.escape(pt) + " " +
     * Escape.escape(vectorC));
     */

  }

  private final Vector3f unitVectorX = new Vector3f(1, 0, 0);

  public void findNearestAtomIndex(int xMouse, int yMouse, Atom[] closest,
                                   short[] mads, int myVisibilityFlag) {
    for (int i = monomerCount; --i >= 0;) {
      if ((monomers[i].shapeVisibilityFlags & myVisibilityFlag) == 0
          || !monomers[i].getLeadAtom().isVisible(0))
        continue;
      if (mads[i] > 0 || mads[i + 1] > 0)
        monomers[i].findNearestAtomIndex(xMouse, yMouse, closest, mads[i],
            mads[i + 1]);
    }
  }

  private int selectedMonomerCount;

  int getSelectedMonomerCount() {
    return selectedMonomerCount;
  }

  BitSet bsSelectedMonomers;

  public void calcSelectedMonomersCount(BitSet bsSelected) {
    selectedMonomerCount = 0;
    if (bsSelectedMonomers == null)
      bsSelectedMonomers = new BitSet();
    BitSetUtil.clear(bsSelectedMonomers);
    for (int i = 0; i < monomerCount; i++) {
      if (monomers[i].isSelected(bsSelected)) {
        ++selectedMonomerCount;
        bsSelectedMonomers.set(i);
      }
    }
  }

  boolean isMonomerSelected(int i) {
    return (i >= 0 && bsSelectedMonomers.get(i));
  }

  public int getPolymerPointsAndVectors(int last, BitSet bs, Vector vList,
                                        boolean isTraceAlpha,
                                        float sheetSmoothing) {
    Point3f[] points = getControlPoints(isTraceAlpha, sheetSmoothing, false);
    Vector3f[] vectors = getWingVectors();
    int count = monomerCount;
    for (int j = 0; j < count; j++)
      if (bs.get(monomers[j].getLeadAtomIndex())) {
        vList.addElement(new Point3f[] { points[j], new Point3f(vectors[j]) });
        last = j;
      } else if (last != Integer.MAX_VALUE - 1) {
        vList.addElement(new Point3f[] { points[j], new Point3f(vectors[j]) });
        last = Integer.MAX_VALUE - 1;
      }
    if (last + 1 < count)
      vList.addElement(new Point3f[] { points[last + 1],
          new Point3f(vectors[last + 1]) });
    return last;
  }

  public String getSequence() {
    char[] buf = new char[monomerCount];
    for (int i = 0; i < monomerCount; i++)
      buf[i] = monomers[i].getGroup1();
    return String.valueOf(buf);
  }

  public Hashtable getPolymerInfo(BitSet bs) {
    Hashtable returnInfo = new Hashtable();
    Vector info = new Vector();
    Vector structureInfo = null;
    ProteinStructure ps;
    ProteinStructure psLast = null;
    int n = 0;
    for (int i = 0; i < monomerCount; i++) {
      if (bs.get(monomers[i].getLeadAtomIndex())) {
        Hashtable monomerInfo = monomers[i].getMyInfo();
        monomerInfo.put("monomerIndex", new Integer(i));
        info.addElement(monomerInfo);
        if ((ps = getProteinStructure(i)) != null && ps != psLast) {
          Hashtable psInfo = new Hashtable();
          (psLast = ps).getInfo(psInfo);
          if (structureInfo == null)
            structureInfo = new Vector();
          psInfo.put("index", new Integer(n++));
          structureInfo.addElement(psInfo);
        }
      }
    }
    if (info.size() > 0) {
      returnInfo.put("sequence", getSequence());
      returnInfo.put("monomers", info);
      if (structureInfo != null)
        returnInfo.put("structures", structureInfo);
    }
    return returnInfo;
  }

  public void getPolymerSequenceAtoms(int iModel, int iPolymer, int group1,
                                      int nGroups, BitSet bsInclude,
                                      BitSet bsResult) {
    int max = group1 + nGroups;
    for (int i = group1; i < monomerCount && i < max; i++)
      monomers[i].getMonomerSequenceAtoms(bsInclude, bsResult);
  }

  public ProteinStructure getProteinStructure(int monomerIndex) {
    return monomers[monomerIndex].getProteinStructure();
  }

  protected boolean calcPhiPsiAngles() {
    return false;
  }

  final private static String[] qColor = { "yellow", "orange", "purple" };

  final public static void getPdbData(BioPolymer p, char ctype, char qtype,
                                      int mStep, int derivType, boolean isDraw,
                                      BitSet bsAtoms, StringBuffer pdbATOM,
                                      StringBuffer pdbCONECT,
                                      BitSet bsSelected, boolean addHeader,
                                      BitSet bsWritten) {
    boolean calcRamachandranStraightness = (qtype == 'C' || qtype == 'P');
    boolean isRamachandran = (ctype == 'R' || ctype == 'S' && 
        calcRamachandranStraightness);
    if (isRamachandran && !p.calcPhiPsiAngles())
      return;
    /*
     * A quaternion visualization involves assigning a frame to each amino acid
     * residue or nucleic acid base. This frame is an orthonormal x-y-z axis
     * system, which can be defined any number of ways.
     * 
     * 'c' C-alpha, as defined by Andy Hanson, U. of Indiana (unpublished
     * results)
     * 
     * X: CA->C (carbonyl carbon) Z: X x (CA->N) Y: Z x X
     * 
     * 'p' Peptide plane as defined by Bob Hanson, St. Olaf College (unpublished
     * results)
     * 
     * X: C->CA Z: X x (C->N') Y: Z x X
     * 
     * 'n' NMR frame using Beta = 17 degrees (Quine, Cross, et al.)
     * 
     * Y: (N->H) x (N->CA) X: R[Y,-17](N->H) Z: X x Y
     * 
     * quaternion types:
     * 
     * w, x, y, z : which of the q-terms to expunge in order to display the
     * other three.
     * 
     * 
     * a : absolute (standard) derivative r : relative (commuted) derivative s :
     * same as w but for calculating straightness
     */

    boolean isAmino = (p instanceof AminoPolymer);
    boolean isRelativeAlias = (ctype == 'r');
    boolean quaternionStraightness = (!isRamachandran && ctype == 'S');
    if (derivType == 2 && isRelativeAlias)
      ctype = 'w';
    if (quaternionStraightness)
      derivType = 2;
    boolean useQuaternionStraightness = (ctype == 'S');
    boolean writeRamachandranStraightness = ("rcpCP".indexOf(qtype) >= 0);
    if (Logger.debugging && (quaternionStraightness  || calcRamachandranStraightness)) {
      Logger.debug("For straightness calculation: useQuaternionStraightness = "
          + useQuaternionStraightness + " and quaternionFrame = " + qtype);
    }
    if (addHeader && !isDraw) {
      pdbATOM.append("REMARK   6    AT GRP CH RESNO  ");
      switch (ctype) {
      default:
      case 'w':
        pdbATOM.append("x*10___ y*10___ z*10___      w*10__       ");
        break;
      case 'x':
        pdbATOM.append("y*10___ z*10___ w*10___      x*10__       ");
        break;
      case 'y':
        pdbATOM.append("z*10___ w*10___ x*10___      y*10__       ");
        break;
      case 'z':
        pdbATOM.append("w*10___ x*10___ y*10___      z*10__       ");
        break;
      case 'R':
        if (writeRamachandranStraightness)
          pdbATOM.append("phi____ psi____ theta         Straightness");
        else
          pdbATOM.append("phi____ psi____ omega-180    PartialCharge");
        break;
      }
      pdbATOM.append("    Sym   q0_______ q1_______ q2_______ q3_______");
      pdbATOM.append("  theta_  aaX_______ aaY_______ aaZ_______");
      if (ctype != 'R')
        pdbATOM.append("  centerX___ centerY___ centerZ___");
      if (qtype == 'n')
        pdbATOM.append("  NHX_______ NHY_______ NHZ_______");
      pdbATOM.append("\n\n");
    }
    int iMax = (mStep < 1 ? 1 : mStep);
    for (int i = 0; i < iMax; i++)
      getData(i, mStep, p, ctype, qtype, derivType, 
          bsAtoms, bsSelected, bsWritten, 
          isDraw, isRamachandran,
          calcRamachandranStraightness,
          useQuaternionStraightness,
          writeRamachandranStraightness,
          quaternionStraightness,
          isAmino, isRelativeAlias,
          pdbATOM, pdbCONECT);
  }

  private static void getData(int m0, int mStep, BioPolymer p, char ctype,
                              char qtype, int derivType,
                              BitSet bsAtoms, BitSet bsSelected, BitSet bsWritten,
                              boolean isDraw, boolean isRamachandran,
                              boolean calcRamachandranStraightness,
                              boolean useQuaternionStraightness,
                              boolean writeRamachandranStraightness,
                              boolean quaternionStraightness, boolean isAmino,
                              boolean isRelativeAlias, StringBuffer pdbATOM,
                              StringBuffer pdbCONECT) {
    String prefix = (derivType > 0 ? "dq" + (derivType == 2 ? "2" : "") : "q");
    Quaternion q;
    Atom aprev = null;
    Quaternion qprev = null;
    Quaternion dq = null;
    Quaternion dqprev = null;
    Quaternion qref = null;
    int atomno = Integer.MIN_VALUE;
    float factor = (ctype == 'R' ? 1f : 10f);
    float x = 0, y = 0, z = 0, w = 0;
    String strExtra = "";
    float val1 = Float.NaN;
    float val2 = Float.NaN;
    int dm = (mStep <= 1 ? 1 : mStep);
    for (int m = m0; m < p.monomerCount; m += dm) {
      Monomer monomer = p.monomers[m];
      if (bsAtoms == null || bsAtoms.get(monomer.getLeadAtomIndex())) {
        Atom a = monomer.getLeadAtom();
        String id = monomer.getUniqueID();
        if (isRamachandran) {
          if (ctype == 'S')
            monomer.setStraightness(Float.NaN);
          x = monomer.getPhi();
          y = monomer.getPsi();
          z = monomer.getOmega();
          if (z < -90)
            z += 360;
          z -= 180; // center on 0
          if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) {
            if (bsAtoms != null)
              bsAtoms.clear(a.getAtomIndex());
            continue;
          }
          float angledeg = (writeRamachandranStraightness ? p.calculateRamachandranHelixAngle(m, qtype) : 0);
          float straightness = (calcRamachandranStraightness ? getStraightness((float) Math.cos(angledeg / 2 / 180 * Math.PI)): 
            writeRamachandranStraightness ? monomer.getStraightness() : 0);
          if (ctype == 'S') {
            monomer.setStraightness(straightness);
            continue;
          }
          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getAtomIndex()))
              continue;
            // draw arrow arc {3.N} {3.ca} {3.C} {131 -131 0.5} "phi -131"
            // draw arrow arc {3.CA} {3.C} {3.N} {0 133 0.5} "psi 133"
            // as looked DOWN the bond, with {pt1} in the back, using
            // standard dihedral/Jmol definitions for anticlockwise positive
            // angles
            AminoMonomer aa = (AminoMonomer) monomer;
            pdbATOM.append("draw phi" + id + " ARROW ARC ").append(
                Escape.escape(aa.getNitrogenAtomPoint())).append(
                Escape.escape((Point3f) a)).append(
                Escape.escape(aa.getCarbonylCarbonAtomPoint())).append(
                "{" + (-x) + " " + x + " 0.5} \"phi = " + (int) x + "\"")
                .append(" color ").append(qColor[2]).append('\n');
            pdbATOM.append("draw psi" + id + " ARROW ARC ").append(
                Escape.escape((Point3f) a)).append(
                Escape.escape(aa.getCarbonylCarbonAtomPoint())).append(
                Escape.escape(aa.getNitrogenAtomPoint())).append(
                "{0 " + y + " 0.5} \"psi = " + (int) y + "\"")
                .append(" color ").append(qColor[1]).append('\n');
            pdbATOM.append("draw planeNCC" + id + " ").append(
                Escape.escape(aa.getNitrogenAtomPoint())).append(
                Escape.escape(a)).append(
                Escape.escape(aa.getCarbonylCarbonAtomPoint())).append(
                " color ").append(qColor[0]).append('\n');
            pdbATOM.append("draw planeCNC" + id + " ").append(
                Escape.escape(((AminoMonomer) p.monomers[m - 1])
                    .getCarbonylCarbonAtomPoint())).append(
                Escape.escape(aa.getNitrogenAtomPoint())).append(
                Escape.escape(a)).append(" color ").append(qColor[1]).append(
                '\n');
            pdbATOM.append("draw planeCCN" + id + " ").append(Escape.escape(a))
                .append(Escape.escape(aa.getCarbonylCarbonAtomPoint())).append(
                    Escape.escape(((AminoMonomer) p.monomers[m + 1])
                        .getNitrogenAtomPoint())).append(" color ").append(
                    qColor[2]).append('\n');
            continue;
          }
          if (Float.isNaN(angledeg)) {
            strExtra = "";
            if (writeRamachandranStraightness)
              continue;
          } else {
            q = new Quaternion(new Point3f(1, 0, 0), angledeg);
            strExtra = q.getInfo();
            if (writeRamachandranStraightness) {
              z = angledeg;
              w = getStraightness((float)(angledeg/180 * Math.PI));
            } else {
              w = a.getPartialCharge();
            }
            
          }
        } else {
          // quaternion
          q = monomer.getQuaternion(qtype);
          if (q != null) {
            q.setRef(qref);
            qref = new Quaternion(q);
          }
          if (derivType == 2)
            monomer.setStraightness(Float.NaN);
          if (q == null) {
            qprev = null;
            qref = null;
          } else if (derivType > 0) {
            Atom anext = a;
            Quaternion qnext = q;
            if (qprev == null) {
              q = null;
              dqprev = null;
            } else {
              
              // we now have two quaternionions, q and qprev
              // the revised procedure assigns dq for these to group a, not aprev
              // a/anext: q
              // aprev: qprev
              
              // back up to previous frame pointer - no longer
              //a = aprev;
              //q = qprev;
              //monomer = (Monomer) a.getGroup();
              // get dq or dq* for PREVIOUS atom
              if (isRelativeAlias) {
                // ctype = 'r';
                // dq*[i] = q[i-1] \ q[i]
                // R(v) = q[i-1] \ q(i) * (0, v) * q[i] \ q[i-1]
                // used for aligning all standard amino acids along X axis
                // in the second derivative and in an ellipse in the first
                // derivative
                //PRE 11.7.47:
                // dq*[i] = q[i] \ q[i+1]
                // R(v) = q[i] \ q(i+1) * (0, v) * q[i+1] \ q[i]
                // used for aligning all standard amino acids along X axis
                // in the second derivative and in an ellipse in the first
                // derivative
                dq = qprev.leftDifference(q);// qprev.inv().mul(q) = qprev \ q
              } else {
                // ctype = 'a' or 'w' or 's'
                
                // OLD:
                // the standard "absolute" difference dq
                // dq[i] = q[i+1] / q[i]
                // R(v) = q[i+1] / q[i] * (0, v) * q[i] / q[i+1]
                // used for definition of the local helical axis

                // NEW:
                // the standard "absolute" difference dq
                // dq[i] = q[i] / q[i-1]
                // R(v) = q[i] / q[i-1] * (0, v) * q[i-1] / q[i]
                // used for definition of the local helical axis

                dq = q.rightDifference(qprev);// q.mul(qprev.inv());
              }
              if (derivType == 1) {
                // first deriv:
                q = dq;
              } else if (dqprev == null) {
                q = null;
              } else {
                /*
                 * standard second deriv.
                 * 
                 * OLD:
                 * 
                 * ddq[i] =defined= (q[i+1] \/ q[i]) / (q[i] \/ q[i-1])
                 * 
                 * Relative to the previous atom as "i" (which is now "a"), we
                 * have:
                 * 
                 * dqprev = q[i] \/ q[i-1] dq = q[i+1] \/ q[i]
                 * 
                 * and so
                 * 
                 * ddq[i] = dq / dqprev
                 * 

                 * NEW: dq = q[i] \/ q[i-1]    dqprev = q[i-1] \/ q[i-2]
                 * 
                 * and so
                 * 
                 * ddq[iprev] = dq / dqprev
                 * 
                 * 
                 * 
                 * Looks odd, perhaps, because it is written "dq[i] / dq[i-1]"
                 * but this is correct; we are assigning ddq to the correct
                 * atom.
                 * 
                 * 5.8.2009 -- bh -- changing quaternion straightness to be
                 * assigned to PREVIOUS aa
                 * 
                 */
                q = dq.rightDifference(dqprev); // q = dq.mul(dqprev.inv());
                val1 = getQuaternionStraightness(id, dqprev, dq);
                val2 = get3DStraightness(id, dqprev, dq);
                aprev.getGroup().setStraightness(useQuaternionStraightness ? val1 : val2);
              }
              dqprev = dq;
            }
            aprev = anext; //(a)
            qprev = qnext; //(q)
          }
          if (q == null) {
            atomno = Integer.MIN_VALUE;
            continue;
          }
          switch (ctype) {
          default:
            x = q.q1;
            y = q.q2;
            z = q.q3;
            w = q.q0;
            break;
          case 'x':
            x = q.q0;
            y = q.q1;
            z = q.q2;
            w = q.q3;
            break;
          case 'y':
            x = q.q3;
            y = q.q0;
            z = q.q1;
            w = q.q2;
            break;
          case 'z':
            x = q.q2;
            y = q.q3;
            z = q.q0;
            w = q.q1;
            break;
          }
          Point3f ptCenter = monomer.getQuaternionFrameCenter(qtype);
          if (ptCenter == null)
            ptCenter = new Point3f();
          if (isDraw) {
            if (bsSelected != null && !bsSelected.get(a.getAtomIndex()))
              continue;
            int deg = (int) (Math.acos(w) * 360 / Math.PI);
            if (derivType == 0) {
              pdbATOM.append(q.draw(prefix, id, ptCenter, 1f));
              if (qtype == 'n' && isAmino) {
                Point3f ptH = ((AminoMonomer) monomer)
                    .getNitrogenHydrogenPoint();
                if (ptH != null)
                  pdbATOM.append("draw " + prefix + "nh" + id + " width 0.1 "
                      + Escape.escape(ptH) + "\n");
              }
            }
            if (derivType == 1) {
              pdbATOM.append(monomer.getHelixData(Token.draw, qtype, mStep)).append('\n');
              continue;
            }
            pdbATOM.append(
                "draw " + prefix + "a" + id + " VECTOR "
                    + Escape.escape(ptCenter) + " {" + (x * 2) + "," + (y * 2)
                    + "," + (z * 2) + "}" + " \">" + deg + "\"").append(
                " color ").append(qColor[derivType]).append('\n');
            continue;
          }
          strExtra = q.getInfo()
              + TextFormat.sprintf("  %10.5p %10.5p %10.5p",
                  new Object[] { ptCenter });
          if (qtype == 'n' && isAmino) {
            strExtra += TextFormat.sprintf("  %10.5p %10.5p %10.5p",
                new Object[] { ((AminoMonomer) monomer).getNitrogenHydrogenPoint() });
          } else if (derivType == 2 && !Float.isNaN(val1)) {
            strExtra += TextFormat.sprintf(" %10.5f %10.5f",
                new Object[] { new float[] { val1, val2 } });
          }
        }
        if (pdbATOM == null)
          continue;
        pdbATOM.append(LabelToken.formatLabel(a, "ATOM  %5i %4a%1A%3n %1c%4R%1E   "));
        pdbATOM.append(TextFormat.sprintf(
            "%8.2f%8.2f%8.2f      %6.3f          %2s    %s\n", new Object[] {
                a.getElementSymbol(false).toUpperCase(), strExtra, new float[] {
                x * factor, y * factor, z * factor, w * factor }}));
        if (atomno != Integer.MIN_VALUE) {
          pdbCONECT.append("CONECT");
          pdbCONECT.append(TextFormat.formatString("%5i", "i", atomno));
          pdbCONECT.append(TextFormat.formatString("%5i", "i", a
              .getAtomNumber()));
          pdbCONECT.append('\n');
          bsWritten.set(((Monomer) a.getGroup()).getLeadAtomIndex());
        }
        atomno = a.getAtomNumber();
      }
    }
  }

  protected float calculateRamachandranHelixAngle(int m, char qtype) {
    return Float.NaN;
  }

  // starting with Jmol 11.7.47, dq is defined so as to LEAD TO
  // the target atom, not LEAD FROM it. 
  private static float get3DStraightness(String id, Quaternion dq,
                                       Quaternion dqnext) {
    // 
    // Normal-only simple dot-product straightness = dq1.normal.DOT.dq2.normal
    //
    return dq.getNormal().dot(dqnext.getNormal());
  }

  private static float getQuaternionStraightness(String id, Quaternion dq,
                                                 Quaternion dqnext) {
    // 
    // Dan Kohler's quaternion straightness = 1 - acos(|dq1.dq2|)/(PI/2)
    //
    // alignment = near 0 or near 180 --> same - just different rotations.
    // It's a 90-degree change in direction that corresponds to 0.
    //
    return getStraightness(dq.dot(dqnext));
  }
  private static float getStraightness(float halfCosTheta) {
    return (float) (1 - 2 * Math.acos(Math.abs(halfCosTheta))/Math.PI);   
  }


}
