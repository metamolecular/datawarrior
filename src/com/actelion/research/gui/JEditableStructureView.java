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

package com.actelion.research.gui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import com.actelion.research.chem.*;

public class JEditableStructureView extends JStructureView {
    static final long serialVersionUID = 0x20090727;

    private static final String EDIT_MESSAGE = "<double click to edit>";
    private boolean mIsEditable;

    public JEditableStructureView() {
        super(null);
        mIsEditable = true;
		}

	public JEditableStructureView(StereoMolecule mol) {
        super(mol);
        mIsEditable = true;
	    }

	public JEditableStructureView(int dragAction, int dropAction) {
        super(null, dragAction, dropAction);
        mIsEditable = true;
	    }

	public JEditableStructureView(StereoMolecule mol, int dragAction, int dropAction) {
        super(mol, dragAction, dropAction);
        mIsEditable = true;
	    }

	public synchronized void paintComponent(Graphics g) {
	    super.paintComponent(g);

        Dimension theSize = getSize();
        Insets insets = getInsets();
        theSize.width -= insets.left + insets.right;
        theSize.height -= insets.top + insets.bottom;

	    if (mIsEditable && getMolecule().getAllAtoms() == 0) {
	        g.setColor(isEnabled() ? Color.BLACK : Color.GRAY);
	        g.setFont(new Font("Helvetica", Font.PLAIN, 10));
	        FontMetrics metrics = g.getFontMetrics();
	        Rectangle2D bounds = metrics.getStringBounds(EDIT_MESSAGE, g);
	        g.drawString(EDIT_MESSAGE, (int)(insets.left+theSize.width-bounds.getWidth())/2,
	                                   (insets.top+theSize.height-metrics.getHeight())/2+metrics.getAscent());
	        }
	    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && isEnabled() && mIsEditable) {
            Component c = this;
            while (!(c instanceof Frame))
                c = c.getParent();
            JDrawDialog theDialog = new JDrawDialog((Frame)c, getMolecule());
            theDialog.addStructureListener(this);
            theDialog.setVisible(true);
            }
        }

	public void setEditable(boolean b) {
		if (mIsEditable != b) {
		    mIsEditable = b;
			}
		}

    public boolean canDrop() {
        return mIsEditable && super.canDrop();
        }
    }
