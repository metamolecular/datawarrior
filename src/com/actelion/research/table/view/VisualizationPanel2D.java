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

package com.actelion.research.table.view;

import java.awt.Frame;
import java.awt.Rectangle;

import com.actelion.research.table.CompoundListSelectionModel;
import com.actelion.research.table.CompoundTableModel;

public class VisualizationPanel2D extends VisualizationPanel {
    private static final long serialVersionUID = 0x20060904;

    public VisualizationPanel2D(Frame parent, CompoundTableModel tableModel,
							    CompoundListSelectionModel selectionModel) {
        super(parent, tableModel);
		mVisualization = new JVisualization2D(tableModel, selectionModel);
		mDimensions = 2;
		initialize();
		}

    @Override
    public void zoom(int sx, int sy, int steps) {
    	final float MIN_ZOOM = 0.0001f;
    	Rectangle bounds = ((JVisualization2D)mVisualization).getGraphBounds(sx, sy);
    	if (bounds != null && bounds.contains(sx, sy)) {
    		boolean zoom = false;
    		float[] low = new float[2];
    		float[] high = new float[2];
    		low[0] = (float)getActingPruningBar(0).getLowValue();
    		high[0] = (float)getActingPruningBar(0).getHighValue();
    		float f = (float)Math.exp(steps / 20.0);
    		if ((steps < 0 && high[0]-low[0] > MIN_ZOOM) || (steps > 0 && high[0]-low[0] < 1.0)) {
	    		float x = low[0] + (float)(sx - bounds.x) * (high[0] - low[0]) / bounds.width;
	    		low[0] = Math.max(0, x-f*(x-low[0]));
	    		high[0] = Math.min(1.0f, x+f*(high[0]-x));
	    		zoom = true;
    			}
    		low[1] = (float)getActingPruningBar(1).getLowValue();
    		high[1] = (float)getActingPruningBar(1).getHighValue();
    		if ((steps < 0 && high[1]-low[1] > MIN_ZOOM) || (steps > 0 && high[1]-low[1] < 1.0)) {
	    		float y = low[1] + (float)(bounds.y + bounds.height - sy) * (high[1] - low[1]) / bounds.height;
	    		low[1] = Math.max(0, y-f*(y-low[1]));
	    		high[1] = Math.min(1.0f, y+f*(high[1]-y));
	    		zoom = true;
    			}
    		if (zoom)
    			setZoom(low, high, false);
    		}
    	}
	}
