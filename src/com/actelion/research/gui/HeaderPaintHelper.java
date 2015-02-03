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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.security.AccessControlException;

public class HeaderPaintHelper {
	public static Paint getHeaderPaint(boolean isSelected, int headerHeight) {
        if (!isSelected)
            return new GradientPaint(0, -1, new Color(0xe1e1e1), 0, headerHeight, new Color(0xcfcfcf));
        else {
            boolean isDevelopment = false;
            try {
                isDevelopment = (System.getProperty("development") != null);
                }
            catch (AccessControlException ace) {}
            if (isDevelopment)
                return new GradientPaint(0, -1, new Color(0xC4C4C4), 0, headerHeight, new Color(0xfbeb00));
            else
                return new GradientPaint(0, -1, new Color(0xC4C4C4), 0, headerHeight, new Color(0x328ef5));
            }
		}
	}
