/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.chem;

import java.awt.Color;

import javax.vecmath.Point3i;

import org.jmol.g3d.Graphics3D;

public class Depictor3D extends AbstractDepictor {
    private int			mZ;
    private short		mColix;
	private int			mpTextSize;

	public Depictor3D(StereoMolecule mol) {
		super(mol);
		}


	public Depictor3D(StereoMolecule mol, int displayMode) {
		super(mol, displayMode);
		}

	
	public void setZ(int z) {
	    mZ = z;
		}


	protected void drawBlackLine(DepictorLine theLine) {
        ((Graphics3D)mG).drawLine((int)theLine.x1, (int)theLine.y1, mZ,
                				  (int)theLine.x2, (int)theLine.y2, mZ);
		}


    protected void drawDottedLine(DepictorLine theLine) {
        ((Graphics3D)mG).drawDottedLine(new Point3i((int)theLine.x1, (int)theLine.y1, mZ),
        								new Point3i((int)theLine.x2, (int)theLine.y2, mZ));
        }


    protected void drawString(String s, float x, float y) {
	    if (mpTextSize > 1) {
			double strWidth = getStringWidth(s);
			((Graphics3D)mG).drawStringNoSlab(s, null, (int)(x-strWidth/2.0), (int)(y+(double)mpTextSize/3.0), 0);
	    	}
		}


	protected void drawPolygon(float[] x, float[] y, int count) {
	    		// only polygons with 3 or 4 corners used
	    if (count >= 3)
	        ((Graphics3D)mG).fillTriangle(mColix,
	                					  (int)x[0], (int)y[0], mZ,
	                					  (int)x[1], (int)y[1], mZ,
	                					  (int)x[2], (int)y[2], mZ);
	    if (count == 4)
	        ((Graphics3D)mG).fillTriangle(mColix,
					  (int)x[2], (int)y[2], mZ,
					  (int)x[3], (int)y[3], mZ,
					  (int)x[0], (int)y[0], mZ);
		}


	protected void fillCircle(float x, float y, float r) {
		((Graphics3D)mG).drawCircleCentered(mColix, (int)(2*r), (int)x, (int)y, mZ, true);
		}


	protected float getStringWidth(String s) {
		return ((Graphics3D)mG).getFont3DCurrent().fontMetrics.stringWidth(s);
		}


	protected void setTextSize(int h) {
		mpTextSize = h;
		((Graphics3D)mG).setFont(((Graphics3D)mG).getFont3D(h));
		}


    public int getTextSize() {
        return mpTextSize;
        }


	protected float getLineWidth() {
		return 1.0f;
		}


	protected void setLineWidth(float lineWidth) {
		}


	protected void setColor(Color c) {
		((Graphics3D)mG).setColix(Graphics3D.getColix(c.getRGB()));
		}
	}
