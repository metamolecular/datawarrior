/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-27 01:06:21 +0200 (lun., 27 juil. 2009) $
 * $Revision: 11287 $
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

package org.jmol.shape;

import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Measurement;
import org.jmol.modelset.MeasurementPending;
import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Point3fi;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;

import java.util.BitSet;
import java.util.Vector;
import java.util.Hashtable;

public class Measures extends Shape {

  private final static int measurementGrowthIncrement = 16;

  private BitSet bsColixSet;
  private BitSet bsSelected;
  private String strFormat;
  private boolean isAllConnected = false;
  private float[] rangeMinMax = {Float.MAX_VALUE, Float.MAX_VALUE};

  private Atom[] atoms;
  private int atomCount;

  int measurementCount = 0;
  Measurement[] measurements = new Measurement[measurementGrowthIncrement];
  MeasurementPending measurementPending;
  
  short mad = (short)-1;
  short colix; // default to none in order to contrast with background
  
  Font3D font3d;
  
  protected void initModelSet() {
    for (int i = 0; i < measurements.length; i++)
      if (measurements[i] != null)
        measurements[i].modelSet = modelSet;
    atoms = modelSet.atoms;
    atomCount = modelSet.getAtomCount();
  }
  
  public void initShape() {
    super.initShape();
    font3d = g3d.getFont3D(JmolConstants.MEASURE_DEFAULT_FONTSIZE);
  }

  public void setSize(int size, BitSet bsSelected) {
    mad = (short)size;
  }

  public void setProperty(String propertyName, Object value, BitSet bsIgnored) {
    // the following can be used with "select measures ({bitset})"
    if ("select".equals(propertyName)) {
      BitSet bs = (BitSet) value;
      if (bs == null || BitSetUtil.cardinalityOf(bs) == 0) {
        bsSelected = null;
      } else {
        bsSelected = new BitSet();
        bsSelected.or(bs);
      }
      return;
    }
    
    if ("color".equals(propertyName)) {
      if (bsColixSet == null)
        bsColixSet = new BitSet();
        short colix = (value == null ? Graphics3D.INHERIT_ALL : Graphics3D.getColix(value));
        if (bsSelected == null)
          this.colix = colix;
      for (int i = 0; i < measurements.length; i++)
        if (measurements[i] != null
            && (bsSelected != null && bsSelected.get(i) || bsSelected == null
                && (colix == Graphics3D.INHERIT_ALL || measurements[i].getColix() == Graphics3D.INHERIT_ALL))) {
          measurements[i].setColix(colix);
          bsColixSet.set(i);
        }
      return;
    } 

    if ("refresh".equals(propertyName)) {
      for (int i = 0; i < measurements.length; i++)
        if (measurements[i] != null)
          measurements[i].refresh();
      return;
    } 

    if ("refreshTrajectories".equals(propertyName)) {
      for (int i = 0; i < measurements.length; i++)
        if (measurements[i] != null && measurements[i].isTrajectory())
          measurements[i].refresh();
      return;
    } 

    if ("hideAll".equals(propertyName)) {
      showHide(((Boolean) value).booleanValue());
      return;
    }
    
    if ("setFormats".equals(propertyName)) {
      setFormats((String) value);
      return;
    }

    if ("delete".equals(propertyName)) {
      delete(value);
      setIndices();
      return;
    } 
    
    if ("clearModelIndex".equals(propertyName)) {
      for (int i = 0; i < measurementCount; i++)
        measurements[i].setModelIndex((short) 0);
      return;
    }
    
    //any one of the following clears the "select measures" business
    
    bsSelected = null;
    if ("pending".equals(propertyName)) {
      pending((MeasurementPending) value);
    } else if ("font".equals(propertyName)) {
      font3d = (Font3D) value;
    } else if ("clear".equals(propertyName)) {
      clear();
    } else if ("reformatDistances".equals(propertyName)) {
      reformatDistances();
    } else if ("setRange".equals(propertyName)) {
      setRange((float[]) value);
    } else if ("setFormat".equals(propertyName)) {
      strFormat = (String) value;
    } else if ("setConnected".equals(propertyName)) {
      setConnected(((Boolean) value).booleanValue());
    } else if ("hide".equals(propertyName)) {
      showHide(new Measurement(modelSet, (int[]) value, null), true);
    } else if ("show".equals(propertyName)) {
      showHide(new Measurement(modelSet, (int[]) value, null), false);
    } else if ("toggle".equals(propertyName)) {
      toggle(new Measurement(modelSet, (int[]) value, null));
    } else if ("toggleOn".equals(propertyName)) {
      toggleOn((int[]) value);
    } else if ("hideVector".equals(propertyName)) {
      showHide(setSingleItem((Vector) value), true);
    } else if ("showVector".equals(propertyName)) {
      showHide(setSingleItem((Vector) value), false);
    } else if ("defineVector".equals(propertyName)) {
      toggle(setSingleItem((Vector) value));
    } else if ("deleteVector".equals(propertyName)) {
      define(setSingleItem((Vector) value), true, false, false);
      setIndices();
    } else if ("defineVector_All".equals(propertyName)) {
      define((Vector) value, false, false, false);
    } else if ("deleteVector_All".equals(propertyName)) {
      define((Vector) value, true, false, false);
      setIndices();
    } else if ("hideVector_All".equals(propertyName)) {
      define((Vector) value, false, false, true);
    } else if ("showVector_All".equals(propertyName)) {
      define((Vector) value, false, true, false);
    } else if ("deleteModelAtoms".equals(propertyName)) {
      atoms = (Atom[])((Object[])value)[1];
      atomCount = modelSet.getAtomCount();
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
      int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
      int atomMax = firstAtomDeleted + nAtomsDeleted;
      for (int i = measurementCount; --i >= 0;) {
        Measurement m = measurements[i];
        int[] indices = m.getCountPlusIndices();
        for (int j = 1; j <= indices[0]; j++) {
          int iAtom = indices[j];
          if (iAtom >= firstAtomDeleted) {
            if (iAtom < atomMax) {
              deleteMeasurement(i);
              break;
            }
            indices[j] -= nAtomsDeleted;
          } else if (iAtom < 0) {
            Point3fi pt = m.getAtom(j);
            if (pt.modelIndex > modelIndex) {
              pt.modelIndex--;
            } else if (pt.modelIndex == modelIndex) {
              deleteMeasurement(i);
              break;
            }
          }
        }
      }
    }
  }

  private Measurement setSingleItem(Vector vector) {
    Point3fi[] points = new Point3fi[4];
    int[] indices = new int[5];
    indices[0] = vector.size();
    for (int i = vector.size(); --i >= 0; ) {
      Object value = vector.get(i);
      if (value instanceof BitSet) {
        int atomIndex = BitSetUtil.firstSetBit((BitSet) value);
        if (atomIndex < 0)
          return null;
        indices[i + 1] = atomIndex;
      } else {
        points[i] = (Point3fi) value;
        indices[i + 1] = -2 - i;
      }
    }
    return new Measurement(modelSet, indices, points);
  }

  public Object getProperty(String property, int index) {
    if ("pending".equals(property))
      return measurementPending;
    if ("count".equals(property))
      return new Integer(measurementCount);
    if ("countPlusIndices".equals(property))
      return (index < measurementCount ? 
          measurements[index].getCountPlusIndices() : null);
    if ("stringValue".equals(property))
      return (index < measurementCount ? measurements[index].getString() : null);
    if ("pointInfo".equals(property))
      return measurements[index / 10].getLabel(index % 10, false, false);
    if ("info".equals(property))
      return getAllInfo();
    if ("infostring".equals(property))
      return getAllInfoAsString();
    return null;
  }

  private void clear() {
    if (measurementCount == 0)
      return;
    int countT = measurementCount;
    measurementCount = 0;
    for (int i = countT; --i >= 0; )
      measurements[i] = null;
    viewer.setStatusMeasuring("measureDeleted", -1, "all");
  }

  private int findMeasurement(int[] indices, Point3fi[] points) {
    for (int i = measurementCount; --i >= 0; )
      if (measurements[i].sameAs(indices, points))
        return i;
    return -1;
  }
  
  private int findMeasurement(Measurement m) {
    return findMeasurement(m.getCountPlusIndices(), m.getPoints());
  }

  private void showHide(Measurement m, boolean isHide) {
    int i = findMeasurement(m);
    if (i < 0)
      return;
    measurements[i].setHidden(isHide);
  }
  
  private void showHide(boolean isHide) {
    for (int i = measurementCount; --i >= 0; )
      if (bsSelected == null || bsSelected.get(i))
        measurements[i].setHidden(isHide);
  }

  private void toggle(Measurement m) {
    rangeMinMax[0] = Float.MAX_VALUE;
    //toggling one that is hidden should be interpreted as DEFINE
    int i = findMeasurement(m);
    if (i >= 0 && !measurements[i].isHidden()) // delete it and all like it
      define(measurements[i], true, false, false);
    else // define OR turn on if measureAllModels
      define(m, false, true, false);
    setIndices();
  }

  private void toggleOn(int[] indices) {
    rangeMinMax[0] = Float.MAX_VALUE;
    //toggling one that is hidden should be interpreted as DEFINE
    bsSelected = new BitSet();
    define(new Measurement(modelSet, indices, null), false, true, true);
    setIndices();
    reformatDistances();
  }

  private void delete(Object value) {
    if (value instanceof int[]) {
      define(new Measurement(modelSet, (int[])value, null), true, false, false);
      return;
    }
    if ((value instanceof Integer))
      deleteMeasurement(((Integer)value).intValue());   
  }

  private void define(Vector monitorExpressions, boolean isDelete, boolean isShow, boolean isHide) {
    /*
     * sets up measures based on an array of atom selection expressions -RMH 3/06
     * 
     *(1) run through first expression, choosing model
     *(2) for each item of next bs, iterate over next bitset, etc.
     *(3) for each last bitset, trigger toggle(int[])
     *
     *simple!
     *
     */
    int nPoints = monitorExpressions.size();
    if (nPoints < 2)
      return;
    int modelIndex = -1;
    Point3fi[] points = new Point3fi[4];
    int[] indices = new int[5];
    Measurement m = new Measurement(modelSet, indices, points);
    m.setCount(nPoints);
    int ptLastAtom = -1;
    BitSet bs;
    for (int i = 0; i < nPoints; i++) {
      Object obj = monitorExpressions.get(i);
      if (obj instanceof BitSet) {
        if (BitSetUtil.cardinalityOf((bs = (BitSet) obj)) > 1)
          modelIndex = 0;
        ptLastAtom = i;
        indices[i + 1] = BitSetUtil.firstSetBit(bs);
      } else {
        if (points == null)
          points = new Point3fi[4];
        points[i] = (Point3fi)obj;
        indices[i + 1] = -2 - i; 
      }
    }
    nextMeasure(0, ptLastAtom, monitorExpressions, m, modelIndex, isDelete, isShow, isHide);
  }

  private void setIndices() {
    for (int i = 0; i < measurementCount; i++)
      measurements[i].setIndex(i);
  }
  
  private void define(Measurement m, boolean isDelete, boolean isShow, boolean doSelect) {
    if (viewer.getMeasureAllModelsFlag()) {
      if (isShow) { // make sure all like this are deleted, not just hidden
        define(m, true, false, false); // self-reference
        if (isDelete)
          return;
      }
      Vector measureList = new Vector();
      int nPoints = m.getCount();
      for (int i = 1; i <= nPoints; i++) {
        int atomIndex = m.getAtomIndex(i);
        measureList.addElement(
            atomIndex >= 0 ? (Object) viewer.getAtomBits(Token.atomno, new Integer(atoms[atomIndex].getAtomNumber()))
                : (Object) m.getAtom(i));
      }
      define(measureList, isDelete, false, false);
      return;
    }    
    define(m, isDelete, doSelect);
  }

  private void define(Measurement m, boolean isDelete, boolean doSelect) {
    int i = findMeasurement(m);
    // nothing to delete and no A-A, A-B-A, A-B-C-B
    int count = m.getCount();
    if (i < 0 && isDelete || m.sameAs(1,2)
        || count > 2 && m.sameAs(1,3)
        || count == 4 && m.sameAs(2,4))
      return;
    float value = (isDelete ? rangeMinMax[0] : m.getMeasurement());
    if (rangeMinMax[0] != Float.MAX_VALUE
        && (value < rangeMinMax[0] || value > rangeMinMax[1]))
      return;
    if (i >= 0) {
      if (isDelete) {
        deleteMeasurement(i);
      } else {
        measurements[i].setHidden(false);
        if (doSelect)
          bsSelected.set(i);
      }
      return;
    }
    Measurement measureNew = new Measurement(modelSet, m.getCountPlusIndices(),
        m.getPoints(), value, colix, strFormat, measurementCount);
    if (measurementCount == measurements.length) {
      measurements = (Measurement[]) ArrayUtil.setLength(measurements,
          measurementCount + measurementGrowthIncrement);
    }
    int n = measurementCount;
    measurements[measurementCount++] = measureNew;
    viewer.setStatusMeasuring("measureCompleted",
        n, measureNew.toVector().toString());
  }

  private void deleteMeasurement(int i) {
    String msg = measurements[i].toVector().toString();
    System.arraycopy(measurements, i + 1, measurements, i, measurementCount
        - i - 1);
    measurements[--measurementCount] = null;
    viewer.setStatusMeasuring("measureDeleted", i, msg);
  }

  private void nextMeasure(int thispt, int ptLastAtom,
                           Vector monitorExpressions, Measurement m,
                           int thisModel, boolean isDelete, boolean isShow,
                           boolean isHide) {
    if (thispt > ptLastAtom) {
      // all atom bitsets have been iterated
      if (isAllConnected && !isConnected(m, thispt))
        return;
      int iThis = findMeasurement(m);
      if (iThis >= 0) {
        if (isDelete)
          define(m, true, false);
        else if (strFormat != null)
          measurements[iThis].formatMeasurement(strFormat, true);
        else
          measurements[iThis].setHidden(isHide);
      } else if (!isDelete && !isHide && !isShow) {
        define(m, false, true);
      }
      return;
    }
    BitSet bs = (BitSet) monitorExpressions.get(thispt);
    int[] indices = m.getCountPlusIndices();
    int thisAtomIndex = indices[thispt];
    if (thisAtomIndex < 0) {
      nextMeasure(thispt + 1, ptLastAtom, monitorExpressions, m, thisModel,
          isDelete, isShow, isHide);
      return;
    }
    boolean haveNext = false;
    for (int i = 0; i < atomCount; i++) {
      if (!bs.get(i) || i == thisAtomIndex)
        continue;
      int modelIndex = atoms[i].getModelIndex();
      if (thisModel >= 0) {
        if (thispt == 0)
          thisModel = modelIndex;
        else if (thisModel != modelIndex)
          continue;
      }
      indices[thispt + 1] = i;
      haveNext = true;
      nextMeasure(thispt + 1, ptLastAtom, monitorExpressions, m, thisModel,
          isDelete, isShow, isHide);
    }
    if (!haveNext)
      nextMeasure(thispt + 1, ptLastAtom, monitorExpressions, m, thisModel,
          isDelete, isShow, isHide);
  }
    
  private boolean isConnected(Measurement m, int ptLastAtom) {
    int atomIndexLast = -1;
    for (int i = 1; i <= ptLastAtom; i++) {
      int atomIndex = m.getAtomIndex(i);
      if (atomIndex < 0)
        continue;
      if (atomIndexLast >= 0
          && !atoms[atomIndex].isBonded(atoms[atomIndexLast]))
        return false;
      atomIndexLast = atomIndex;
    }
    return true;
  }
  
  private void setRange(float[] rangeMinMax) {
    this.rangeMinMax[0] = rangeMinMax[0];
    this.rangeMinMax[1] = rangeMinMax[1];
  }
  
  private void setConnected(boolean isAllConnected) {
    this.isAllConnected = isAllConnected;  
  }
  
  private void pending(MeasurementPending measurementPending) {
    this.measurementPending = measurementPending;
    if (measurementPending == null)
      return;
    if (measurementPending.getCount() > 1)
      viewer.setStatusMeasuring("measurePending",
          measurementPending.getCount(), measurementPending.toVector().toString());
  }

  private void reformatDistances() {
    for (int i = measurementCount; --i >= 0; )
      measurements[i].reformatDistanceIfSelected();    
  }
  
  private void setFormats(String format) {
    if (format != null && format.length() == 0)
      format = null;
    for (int i = measurementCount; --i >= 0;)
      if (bsSelected == null || bsSelected.get(i))
        measurements[i].formatMeasurement(format, false);
  }
  
  private Vector getAllInfo() {
    Vector info = new Vector();
    for (int i = 0; i< measurementCount; i++) {
      info.addElement(getInfo(i));
    }
    return info;
  }
  
  private String getAllInfoAsString() {
    String info = "Measurement Information";
    for (int i = 0; i< measurementCount; i++) {
      info += "\n" + getInfoAsString(i);
    }
    return info;
  }
  
  private Hashtable getInfo(int index) {
    Measurement m = measurements[index];
    int count = m.getCount();
    Hashtable info = new Hashtable();
    info.put("index", new Integer(index));
    info.put("type", (count == 2 ? "distance" : count == 3 ? "angle"
        : "dihedral"));
    info.put("strMeasurement", m.getString());
    info.put("count", new Integer(count));
    info.put("value", new Float(m.getValue()));
    Vector atomsInfo = new Vector();
    for (int i = 1; i <= count; i++) {
      Hashtable atomInfo = new Hashtable();
      int atomIndex = m.getAtomIndex(i);
      atomInfo.put("_ipt", new Integer(atomIndex));
      atomInfo.put("coord", Escape.escape(m.getAtom(i)));
      atomInfo.put("atomno", new Integer(atomIndex < 0 ? -1 : atoms[atomIndex].getAtomNumber()));
      atomInfo.put("info", (atomIndex < 0 ? "<point>" : atoms[atomIndex].getInfo()));
      atomsInfo.addElement(atomInfo);
    }
    info.put("atoms", atomsInfo);
    return info;
  }

  private String getInfoAsString(int index) {
    Measurement m = measurements[index];
    int count = m.getCount();
    StringBuffer sb = new StringBuffer();
    sb.append(count == 2 ? "distance" : count == 3 ? "angle" : "dihedral");
    sb.append(" \t").append(m.getValue()).append(" \t").append(m.getString());
    for (int i = 1; i <= count; i++)
      sb.append(" \t").append(m.getLabel(i, false, false));
    return sb.toString();
  }
  
  void setVisibilityInfo() {
    BitSet bsModels = viewer.getVisibleFramesBitSet();
    out:
    for (int i = measurementCount; --i >= 0; ) {
      Measurement m = measurements[i];
      m.setVisible(false);
      if(mad == 0 || m.isHidden())
        continue;
      for (int iAtom = m.getCount(); iAtom > 0; iAtom--) {
        int atomIndex = m.getAtomIndex(iAtom);
        if (atomIndex >= 0) {
          if (!modelSet.getAtomAt(atomIndex).isClickable())
            continue out;
        } else {
          int modelIndex = m.getAtom(iAtom).modelIndex;
          if (modelIndex >= 0 && !bsModels.get(modelIndex))
            continue out;
        }
      }
      measurements[i].setVisible(true);
    }
  }
  
 public String getShapeState() {
    StringBuffer commands = new StringBuffer("");
    appendCmd(commands, "measures delete");
    for (int i = 0; i < measurementCount; i++)
      appendCmd(commands, getState(i));
    appendCmd(commands, "select *; set measures " + viewer.getMeasureDistanceUnits());
    appendCmd(commands, getFontCommand("measures", font3d));
    int n = 0;
    Hashtable temp = new Hashtable();
    BitSet bs = new BitSet(measurementCount);
    for (int i = 0; i < measurementCount; i++) {
      if (measurements[i].isHidden()) {
        n++;
        bs.set(i);
      }
      if (bsColixSet != null && bsColixSet.get(i))
        setStateInfo(temp, i, getColorCommand("measure", measurements[i].getColix()));
      if (measurements[i].getStrFormat() != null)
        setStateInfo(temp, i, "measure "
            + Escape.escape(measurements[i].getStrFormat()));
    }
    if (n > 0)
      if (n == measurementCount)
        appendCmd(commands, "measures off; # lines and numbers off");
      else
        for (int i = 0; i < measurementCount; i++)
          if (measurements[i].isHidden())
            setStateInfo(temp, i, "measure off");
    String s = getShapeCommands(temp, null, -1, "select measures");
    if (s != null) {
      commands.append(s);
      appendCmd(commands, "select measures ({null})");
    }
    return commands.toString();
  }
  
  private String getState(int index) {
    Measurement m = measurements[index];
    int count = m.getCount();
    StringBuffer sb = new StringBuffer("measure");
    for (int i = 1; i <= count; i++)
      sb.append(" ").append(m.getLabel(i, true, true));
    sb.append("; # " + getInfoAsString(index));
    return sb.toString();
  }
}
