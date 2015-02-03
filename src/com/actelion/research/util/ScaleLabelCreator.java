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

package com.actelion.research.util;

import java.util.ArrayList;

public class ScaleLabelCreator {

	/**
	 * Creates a list of scale labels with their relative positions for a linear
	 * numerical scale with given numerical value range.
	 * @param rangeLow low limit of numerical range
	 * @param rangeHigh high limit of numerical range
	 * @return list of labels with relative positions
	 */
	public static ArrayList<ScaleLabel> createLinearLabelList(float rangeLow, float rangeHigh) {
		if (rangeHigh <= rangeLow)
			return null;

		float range = rangeHigh - rangeLow;

		int exponent = 0;
		while (range >= 50.0) {
			rangeLow /= 10;
			range /= 10;
			exponent++;
			}
		while (range < 5.0) {
			rangeLow *= 10;
			range *= 10.0;
			exponent--;
			}

		int gridSpacing = (int)(range / 10);
	    if (gridSpacing < 1)
			gridSpacing = 1;
		else if (gridSpacing < 2)
			gridSpacing = 2;
		else
			gridSpacing = 5;

	    int value = (rangeLow < 0) ?
		      (int)(rangeLow - 0.0000001 - (rangeLow % gridSpacing))
			: (int)(rangeLow + 0.0000001 + gridSpacing - (rangeLow % gridSpacing));

		ArrayList<ScaleLabel> labelList = new ArrayList<ScaleLabel>();
		while ((float)value < (rangeLow + range)) {
			float position = ((float)value-rangeLow) / range;

			labelList.add(new ScaleLabel(DoubleFormat.toString(value, exponent), position, (float)value*(float)Math.pow(10, exponent)));

			value += gridSpacing;
			}

		return labelList;
		}

	/**
	 * Creates a list of scale labels with their relative positions for a
	 * logarithmical numerical scale with given numerical value range.
	 * @param rangeLow log10 of low limit of numerical range
	 * @param rangeHigh log10 of high limit of numerical range
	 * @return list of labels with relative positions
	 */
	public static ArrayList<ScaleLabel> createLogarithmicLabelList(float rangeLow, float rangeHigh) {
		if (rangeHigh <= rangeLow)
			return null;

		float range = rangeHigh - rangeLow;

        int intMin = (int)Math.floor(rangeLow);
        int intMax = (int)Math.floor(rangeHigh);
        
		ArrayList<ScaleLabel> labelList = new ArrayList<ScaleLabel>();
        if (range > 5.4) {
            int step = 1 + (int)range/10;
            for (int i=intMin; i<=intMax; i+=step)
                addLogarithmicScaleLabel(labelList, i, rangeLow, range);
            }
        else if (range > 3.6) {
            for (int i=intMin; i<=intMax; i++) {
                addLogarithmicScaleLabel(labelList, i, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.47712125472f, rangeLow, range);
                }
            }
        else if (range > 1.8) {
            for (int i=intMin; i<=intMax; i++) {
                addLogarithmicScaleLabel(labelList, i, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.301029996f, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.698970004f, rangeLow, range);
                }
            }
        else if (range > 1.0) {
            for (int i=intMin; i<=intMax; i++) {
                addLogarithmicScaleLabel(labelList, i, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.176091259f, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.301029996f, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.477121255f, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.698970004f, rangeLow, range);
                addLogarithmicScaleLabel(labelList, i + 0.84509804f, rangeLow, range);
                }
            }
        else {
            float start = (float)Math.pow(10, rangeLow);
            float length = (float)Math.pow(10, rangeLow+range) - start;

            int exponent = 0;
            while (length >= 50.0) {
                start /= 10;
                length /= 10;
                exponent++;
                }
            while (length < 5.0) {
                start *= 10;
                length *= 10.0;
                exponent--;
                }

            int gridSpacing = (int)(length / 10);
            if (gridSpacing < 1)
                gridSpacing = 1;
            else if (gridSpacing < 2)
                gridSpacing = 2;
            else
                gridSpacing = 5;

            int value = (start < 0) ?
                  (int)(start - 0.0000001 - (start % gridSpacing))
                : (int)(start + 0.0000001 + gridSpacing - (start % gridSpacing));
            while ((float)value < (start + length)) {
                float log = (float)Math.log10(value) + exponent;
                float position = (float)(log-rangeLow) / range;
                labelList.add(new ScaleLabel(DoubleFormat.toString(value, exponent), position, log));
                value += gridSpacing;
                }
            }

        return labelList;
		}

    private static void addLogarithmicScaleLabel(ArrayList<ScaleLabel> labelList, float value, float rangeLow, float range) {
        if (value >= rangeLow && value <= rangeLow+range) {
            float position = (value-rangeLow) / range;
            labelList.add(new ScaleLabel(DoubleFormat.toString(Math.pow(10, value), 3), position, value));
            }
        }
	}
