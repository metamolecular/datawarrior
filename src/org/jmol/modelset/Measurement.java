/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-21 14:42:28 +0200 (mar., 21 juil. 2009) $
 * $Revision: 11276 $
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
package org.jmol.modelset;

import org.jmol.util.Escape;
import org.jmol.util.Point3fi;
import org.jmol.util.Measure;

import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.AxisAngle4f;

import java.util.Vector;

public class Measurement {

  Viewer viewer;

  public ModelSet modelSet;

  protected int count;
  protected int[] countPlusIndices = new int[5];
  protected Point3fi[] points;
  
  public int getCount() {
    return count;
  }
  
  public void setCount(int count) {
    this.count = countPlusIndices[0] = count;
  }

  public int[] getCountPlusIndices() {
    return countPlusIndices;
  }
  
  public Point3fi[] getPoints() {
    return points;
  }

  public int getAtomIndex(int n) {
    return (n > 0 && n <= count ? countPlusIndices[n] : -1);
  }
  
  public Point3fi getAtom(int i) {
    int pt = countPlusIndices[i];
    return (pt < -1 ? points[-2 - pt] : modelSet.getAtomAt(pt));
  }

  public int getLastIndex() {
    return (count > 0 ? countPlusIndices[count] : -1);
  }
  
  private String strMeasurement;
  
  public String getString() {
    return strMeasurement;
  }
  
  public String getStringDetail() {
    return (count == 2 ? "Distance" : count == 3 ? "Angle" : "Torsion")
        + getMeasurementScript(" - ", false) + " : " + value;  
  }
  
  private String strFormat;
  
  public String getStrFormat() {
    return strFormat;
  }
  
  protected float value;
  
  public float getValue() {
    return value;
  }
  
  private boolean isVisible = true;
  private boolean isHidden = false;
  private boolean isDynamic = false;
  private boolean isTrajectory = false;
  
  public boolean isVisible() {
    return isVisible;
  }
  public boolean isHidden() {
    return isHidden;
  }
  public boolean isDynamic() {
    return isDynamic;
  }
  
  public boolean isTrajectory() {
    return isTrajectory;
  }
  
  public void setVisible(boolean TF) {
    this.isVisible = TF;
  }
  public void setHidden(boolean TF) {
    this.isHidden = TF;
  }
  public void setDynamic(boolean TF) {
    this.isDynamic = TF;
  }
  
  private short colix;
  
  public short getColix() {
    return colix;
  }
  
  public void setColix(short colix) {
    this.colix = colix;
  }
  
  private int index;
  
  public void setIndex(int index) {
    this.index = index;
  }
  
  public int getIndex() {
    return index;
  }
  
  private AxisAngle4f aa;
  
  public AxisAngle4f getAxisAngle() {
    return aa;
  }
  
  private Point3f pointArc;
  
  public Point3f getPointArc() {
    return pointArc;
  }
  
  public Measurement(ModelSet modelSet, int[] indices,
      Point3fi[] points, float value, short colix, String strFormat, int index) {
    //value Float.isNaN ==> pending
    this.index = index;
    this.modelSet = modelSet;
    this.viewer = modelSet.viewer;
    this.colix = colix;
    this.strFormat = strFormat;
    count = (indices == null ? 0 : indices[0]);
    if (count > 0) {
      System.arraycopy(indices, 0, countPlusIndices, 0, count + 1);
      isTrajectory = modelSet.isTrajectory(countPlusIndices);
    }
    this.value = (Float.isNaN(value) || isTrajectory ? getMeasurement() : value);
    this.points = (points == null ? new Point3fi[4] : points);
    formatMeasurement();
  }   

  public Measurement(ModelSet modelSet, int[] indices, Point3fi[] points) {
    // temporary holding structure only; -- no viewer
    countPlusIndices = indices;
    count = indices[0];
    this.points = (points == null ? new Point3fi[4] : points);
    this.modelSet = modelSet;
  }

  public void refresh() {
    value = getMeasurement();
    isTrajectory = modelSet.isTrajectory(countPlusIndices);
    formatMeasurement();
  }
  
  /**
   * Used by MouseManager and Picking Manager to build the script
   * @param sep
   * @param withModelIndex 
   * @return measure (atomIndex=1) (atomIndex=2)....
   */
  public String getMeasurementScript(String sep, boolean withModelIndex) {
    String str = "";
    boolean asScript = (sep.equals(" "));
    for (int i = 1; i <= count; i++)
      str += (i > 1 ? sep : " ") + getLabel(i, asScript, withModelIndex); 
    return str;  
  }
  
  public void formatMeasurement(String strFormat, boolean useDefault) {
    if (strFormat != null && strFormat.length() == 0)
      strFormat = null;
    if (!useDefault && strFormat != null && strFormat.indexOf(countPlusIndices[0]+":")!=0)
      return;
    this.strFormat = strFormat; 
    formatMeasurement();
  }

  protected void formatMeasurement() {
    strMeasurement = null;
    if (Float.isNaN(value) || count == 0)
      return;
    switch (count) {
    case 2:
      strMeasurement = formatDistance(value);
      return;
    case 3:
      if (value == 180) {
        aa = null;
        pointArc = null;
      } else {
        Vector3f vectorBA = new Vector3f();
        Vector3f vectorBC = new Vector3f();        
        float radians = Measure.computeAngle(getAtom(1), getAtom(2), getAtom(3), vectorBA, vectorBC, false);
        Vector3f vectorAxis = new Vector3f();
        vectorAxis.cross(vectorBA, vectorBC);
        aa = new AxisAngle4f(vectorAxis.x, vectorAxis.y, vectorAxis.z, radians);

        vectorBA.normalize();
        vectorBA.scale(0.5f);
        pointArc = new Point3f(vectorBA);
      }
      // fall through
    case 4:
      strMeasurement = formatAngle(value);
      return;
    }
  }
  
  public void reformatDistanceIfSelected() {
    if (count != 2)
      return;
    if (viewer.isSelected(countPlusIndices[1]) &&
        viewer.isSelected(countPlusIndices[2]))
      formatMeasurement();
  }

  private String formatDistance(float dist) {
    int nDist = (int)(dist * 100 + 0.5f);
    float value;
    String units = viewer.getMeasureDistanceUnits();
    if (units == "nanometers") {
      units = "nm";
      value = nDist / 1000f;
    } else if (units == "picometers") {
      units = "pm";
      value = (int)((dist * 1000 + 0.5)) / 10f;
    } else if (units == "au") {
      value = (int) (dist / JmolConstants.ANGSTROMS_PER_BOHR * 1000 + 0.5f) / 1000f;
    } else {
      units = "\u00C5"; // angstroms
      value = nDist / 100f;
    }
    return formatString(value, units);
  }

  private String formatAngle(float angle) {
    angle = (int)(angle * 10 + (angle >= 0 ? 0.5f : -0.5f));
    angle /= 10;
    return formatString(angle, "\u00B0");
  }

  private String formatString(float value, String units) {
    String s = countPlusIndices[0]+":" + "";
    String label = (strFormat != null && strFormat.indexOf(s)==0? strFormat : viewer
        .getDefaultMeasurementLabel(countPlusIndices[0]));
    if (label.indexOf(s)==0)
      label = label.substring(2);
    return LabelToken.labelFormat(this, label, value, units);
  }

  public boolean sameAs(int[] indices, Point3fi[] points) {
    if (count != indices[0]) 
      return false;
    boolean isSame = true;
    for (int i = 1; i <= count && isSame; i++)
      isSame = (countPlusIndices[i] == indices[i]);
    if (isSame)
      for (int i = 0; i < count && isSame; i++) {
        if (points[i] != null)
          isSame = (this.points[i].distance(points[i]) < 0.01); 
      }
    if (isSame)
      return true;
    switch (count) {
    default:
      return true;
    case 2:
      return sameAs(indices, points, 1, 2) 
          && sameAs(indices, points, 2, 1);
    case 3:
      return sameAs(indices, points, 1, 3)
          && sameAs(indices, points, 2, 2)
          && sameAs(indices, points, 3, 1);
    case 4:  
      return  sameAs(indices, points, 1, 4)
          && sameAs(indices, points, 2, 3) 
          && sameAs(indices, points, 3, 2)
          && sameAs(indices, points, 4, 1);
    } 
  }

  private boolean sameAs(int[] atoms, Point3fi[] points, int i, int j) {
    int ipt = countPlusIndices[i];
    int jpt = atoms[j];
    if (jpt < 0 && points[-2-jpt] == null)
      System.out.println("measurement -- ohoh");
    return (ipt >= 0 || jpt >= 0 ? ipt == jpt 
        : this.points[-2 - ipt].distance(points[-2 - jpt]) < 0.01);
  }

  public boolean sameAs(int i, int j) {
    return sameAs(countPlusIndices, points, i, j);
  }

  public Vector toVector() {
    Vector V = new Vector();
    for (int i = 1; i <= count; i++ )
      V.addElement(getLabel(i, false, false));
    V.addElement(strMeasurement);
    return V;  
  }
  
  public float getMeasurement() {
    if (countPlusIndices == null)
      return Float.NaN;
    if (count < 2)
      return Float.NaN;
    for (int i = count; --i >= 0;)
      if (countPlusIndices[i + 1] == -1) {
        return Float.NaN;
      }
    Point3fi ptA = getAtom(1);
    Point3fi ptB = getAtom(2);
    Point3fi ptC, ptD;
    switch (count) {
    case 2:
      return ptA.distance(ptB);
    case 3:
      ptC = getAtom(3);
      return Measure.computeAngle(ptA, ptB, ptC, true);
    case 4:
      ptC = getAtom(3);
      ptD = getAtom(4);
      return Measure.computeTorsion(ptA, ptB, ptC, ptD, true);
    default:
      return Float.NaN;
    }
  }

  public String getLabel(int i, boolean asBitSet, boolean withModelIndex) {
    int atomIndex = countPlusIndices[i];
    return (atomIndex < 0 
        ? (withModelIndex ? "modelIndex " + getAtom(i).modelIndex + " " : "")
            + Escape.escape(getAtom(i))
        : asBitSet ? "({" + atomIndex + "})"
        : viewer.getAtomInfo(atomIndex));
  }

  public void setModelIndex(short modelIndex) {
    if (points == null)
      return;
    for (int i = 0; i < count; i++) {
      if (points[i] != null)
        points[i].modelIndex = modelIndex;
    }
  }

}


