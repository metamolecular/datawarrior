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

package com.actelion.research.gui.form;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.viewer2d.MoleculeCanvas;
import com.actelion.research.gui.viewer2d.MoleculeViewer;

public class JStructure3DFormObject extends AbstractFormObject {
	public static final String FORM_OBJECT_TYPE = "structure3D";
	public JStructure3DFormObject(String key, String type) {
		super(key, type);
		mComponent = new MoleculeViewer();
		}

    @Override
	public Object getData() {
		return ((MoleculeViewer)mComponent).getMolecule();
		}

    @Override
	public void setData(Object data) {
		if (data == null)
			((MoleculeViewer)mComponent).setMolecule((FFMolecule)null);
		else if (data instanceof StereoMolecule) {
			((MoleculeViewer)mComponent).setMolecule((StereoMolecule)data);
			((MoleculeViewer)mComponent).resetView();
			((MoleculeViewer)mComponent).repaint();
			}
		else if (data instanceof String) {
			String idcode = (String)data;
			int index = idcode.indexOf('\t');
			String coords = (index == -1) ? null : idcode.substring(index+1);
			StereoMolecule mol = new IDCodeParser().getCompactMolecule(idcode, coords);
			((MoleculeViewer)mComponent).setMolecule(mol);
			((MoleculeViewer)mComponent).resetView();
			((MoleculeViewer)mComponent).repaint();
			}
		}

    @Override
	public int getRelativeHeight() {
		return 4;
		}

    @Override
	public void printContent(Graphics2D g2D, Rectangle2D.Float r, float scale, Object data) {
	    if (data != null && r.width > 1 && r.height > 1) {
		    ExtendedMolecule mol = null;
		    if (data instanceof ExtendedMolecule) {
		        mol = (ExtendedMolecule)data;
		    	}
		    else if (data instanceof String) {
				String idcode = (String)data;
				int index = idcode.indexOf('\t');
				String coords = (index == -1) ? null : idcode.substring(index+1);
				mol = new IDCodeParser().getCompactMolecule(idcode, coords);
		    	}
		    
		    if (mol != null) {
		        AffineTransform originalTransform = g2D.getTransform();
		        g2D.translate(r.x, r.y);
		        g2D.scale(0.25, 0.25);
		        MoleculeCanvas moleculeCanvas = new MoleculeCanvas();
		        moleculeCanvas.setBackground(Color.WHITE);
		        moleculeCanvas.setMolecule(mol);
		        moleculeCanvas.setSize((int)(4*r.width), (int)(4*r.height));
		        moleculeCanvas.resetView();
		        moleculeCanvas.paint(g2D, (int)(4*r.width), (int)(4*r.height));
		        g2D.setTransform(originalTransform);
		    	}
	    	}
		}
	}
