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
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.border.Border;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.ExtendedDepictor;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.gui.JDrawDialog;
import com.actelion.research.gui.table.ChemistryRenderPanel;

public class JReactionFormObject extends AbstractFormObject {
    private Object mChemistry;
    private boolean mIsEditable;

    public JReactionFormObject(String key, String type) {
		super(key, type);
		mComponent = new ChemistryRenderPanel() {
		    private static final long serialVersionUID = 0x20070509;
            public void setBorder(Border border) {
                if (border instanceof FormObjectBorder)
                    super.setBorder(border);
                }
            };
		mComponent.setBackground(Color.white);
		mComponent.addMouseListener(new MouseAdapter() {
			@Override
		    public void mouseClicked(MouseEvent e) {
		        if (e.getClickCount() == 2 && mIsEditable) {
		            Component c = mComponent;
		            while (!(c instanceof Frame))
		                c = c.getParent();
					JDrawDialog dialog = new JDrawDialog((Frame)c, (Reaction)mChemistry);
					dialog.setVisible(true);
					if (!dialog.isCancelled()) {
						mChemistry = dialog.getReactionAndDrawings();
						fireDataChanged();
						}
		        	}
				}
			});
//		((JChemistryView)mComponent).setClipboardHandler(new ClipboardHandler());
		}

	public void setEditable(boolean b) {
	    super.setEditable(b);
	    mIsEditable = b;
	    }

	public Object getData() {
		return mChemistry;
		}

	public void setData(Object data) {
        mChemistry = null;
		if (data != null) {
		    if (data instanceof Reaction)
		        mChemistry = (Reaction)data;
		    else if (data instanceof String)
		        mChemistry = ReactionEncoder.decode((String)data, true	/*, mComponent.getGraphics()*/ );
            }

        ((ChemistryRenderPanel)mComponent).setChemistry(mChemistry);
	    }

	public int getRelativeHeight() {
		return 4;
		}

	public void printContent(Graphics2D g2D, Rectangle2D.Float r, float scale, Object data) {
        if (data != null) {
	        Reaction rxn = null;
			if (data instanceof Reaction)
				rxn = (Reaction)data;
			else if (data instanceof String)
				rxn = ReactionEncoder.decode((String)data, true /* , g2D */);
	
		    if (rxn != null) {
				ExtendedDepictor d = new ExtendedDepictor(rxn, null, !rxn.hasAbsoluteCoordinates(), true);
				d.validateView(g2D, r, AbstractDepictor.cModeInflateToMaxAVBL);
				d.paint(g2D);
				}
        	}
		}
    }
