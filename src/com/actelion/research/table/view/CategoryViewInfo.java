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

import java.awt.Color;

public class CategoryViewInfo {
    int[][] pointsInCategory;
    int[][][] pointsInColorCategory;
    Color[] color;
    float[][] barValue;
    float[][] innerDistance;   // distance of two adjacent sub-bar areas in bar
    float[][] pieX;
    float[][] pieY;
    float[][] pieSize;         // pie size in pixel
    float axisMin,axisMax,barBase;
    float barWidth;
    int barAxis;
    boolean barOrPieDataAvailable;

    public CategoryViewInfo(int hvCount, int catCount, int colorCount) {
        pointsInCategory = new int[hvCount][catCount];
        pointsInColorCategory = new int[hvCount][catCount][colorCount];
        barValue = new float[hvCount][catCount];
        color = new Color[colorCount];
        }
	}
