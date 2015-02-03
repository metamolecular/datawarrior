/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-05-30 22:20:47 +0200 (sam., 30 mai 2009) $
 * $Revision: 10920 $

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

import org.jmol.g3d.Graphics3D;

import java.util.BitSet;

import org.jmol.viewer.JmolConstants;

public class Bond {

  public static class BondSet extends BitSet {
    private int[] associatedAtoms;
    
    public int[] getAssociatedAtoms() {
      return associatedAtoms;
    }

    public BondSet(BitSet bs) {
      for (int i = bs.size(); --i >= 0;)
        if (bs.get(i))
          set(i);
    }

    public BondSet(BitSet bs, int[] atoms) {
      this(bs);
      associatedAtoms = atoms;
    }
  }

  Atom atom1;
  Atom atom2;
  short order;
  short mad;
  public short getMad() {
    return mad;
  }


  short colix;
  
  public short getColix() {
    return colix;
  }
  
  int index = -1;
  int shapeVisibilityFlags;
  
  public int getShapeVisibilityFlags() {
    return shapeVisibilityFlags;
  }

  
  final static int myVisibilityFlag = JmolConstants.getShapeVisibilityFlag(JmolConstants.SHAPE_STICKS);

  Bond(Atom atom1, Atom atom2, short order,
              short mad, short colix) {
    if (atom1 == null || atom2 == null)
      throw new NullPointerException();
    this.atom1 = atom1;
    this.atom2 = atom2;
    if (atom1.getElementNumber() == 16 && atom2.getElementNumber() == 16)
      order |= JmolConstants.BOND_SULFUR_MASK;
    if (order == JmolConstants.BOND_AROMATIC_MASK)
      order = JmolConstants.BOND_AROMATIC;
    this.order = order;
    this.colix = colix;
    setMad(mad);
  }

  public String getIdentity() {
    return (index + 1) + " "+ getOrderNumberAsString() + " " + atom1.getInfo() + " -- "
        + atom2.getInfo() + " " + atom1.distance(atom2);
  }

  public boolean isCovalent() {
    return (order & JmolConstants.BOND_COVALENT_MASK) != 0;
  }

  boolean isHydrogen() {
    return (order & JmolConstants.BOND_HYDROGEN_MASK) != 0;
  }

  boolean isStereo() {
    return (order & JmolConstants.BOND_STEREO_MASK) != 0;
  }

  boolean isPartial() {
    return (order & JmolConstants.BOND_PARTIAL_MASK) != 0;
  }

  boolean isAromatic() {
    return (order & JmolConstants.BOND_AROMATIC_MASK) != 0;
  }

  int getValence() {
    return (!isCovalent() ? 0
        : isPartial() || is(JmolConstants.BOND_AROMATIC) ? 1
        : order & 7);
  }

  void deleteAtomReferences() {
    if (atom1 != null)
      atom1.deleteBond(this);
    if (atom2 != null)
      atom2.deleteBond(this);
    atom1 = atom2 = null;
  }

  public void setMad(short mad) {
    boolean wasVisible = (this.mad != 0); 
    boolean isVisible = (mad != 0);
    if (wasVisible != isVisible) {
      atom1.addDisplayedBond(myVisibilityFlag, isVisible);
      atom2.addDisplayedBond(myVisibilityFlag, isVisible);    
    }
    this.mad = mad;
    setShapeVisibility(myVisibilityFlag, isVisible);
  }

  final void setShapeVisibility(int shapeVisibilityFlag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= shapeVisibilityFlag;        
    } else {
      shapeVisibilityFlags &=~shapeVisibilityFlag;
    }
  }
      
  public void setColix(short colix) {
    this.colix = colix;
  }

  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colix = Graphics3D.getColixTranslucent(colix, isTranslucent, translucentLevel);
  }
  
  boolean isTranslucent() {
    return Graphics3D.isColixTranslucent(colix);
    //but may show up translucent anyway!
  }

  public void setOrder(short order) {
    this.order = (short) (order | (this.order & JmolConstants.BOND_NEW));
  }

  public Atom getAtom1() {
    return atom1;
  }

  public Atom getAtom2() {
    return atom2;
  }

  public int getAtomIndex1() {
    return atom1.atomIndex;
  }
  
  public int getAtomIndex2() {
    return atom2.atomIndex;
  }
  
  float getRadius() {
    return mad / 2000f;
  }

  public short getOrder() {
    return order;
  }

  String getOrderName() {
    return JmolConstants.getBondOrderNameFromOrder(order);
  }

  String getOrderNumberAsString() {
    return JmolConstants.getBondOrderNumberFromOrder(order);
  }

  short getColix1() {
    return Graphics3D.getColixInherited(colix, atom1.colixAtom);
  }

  int getArgb1() {
    return atom1.group.chain.modelSet.viewer.getColixArgb(getColix1());
  }

  short getColix2() {
    return Graphics3D.getColixInherited(colix, atom2.colixAtom);
  }

  int getArgb2() {
    return atom1.group.chain.modelSet.viewer.getColixArgb(getColix2());
  }

  public Atom getOtherAtom(Atom thisAtom) {
    return (atom1 == thisAtom ? atom2 : atom2 == thisAtom ? atom1 : null);
  }
  
  ////////////////////////////////////////////////////////////////
  
  public void setShapeVisibilityFlags(int shapeVisibilityFlags) {
    this.shapeVisibilityFlags = shapeVisibilityFlags;
  }

  public void setIndex(int i) {
    index = i;
  }

  public boolean is(int bondType) {
    return (order & ~JmolConstants.BOND_NEW) == bondType;
  }

}
