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

import java.io.Serializable;
import java.util.Comparator;

public class ByteArrayComparator implements Comparator<byte[]>,Serializable {
    static final long serialVersionUID = 0x20120809;
 
    public int compare(byte[] b1, byte[] b2) {
        if (b1 == null)
            return (b2 == null) ? 0 : 1;
        if (b2 == null)
            return -1;
        for (int i=0; i<b1.length; i++) {
            if (b2.length == i)
                return 1;
            if (b1[i] != b2[i])
                return (b1[i] < b2[i]) ? -1 : 1;
            }
        return (b2.length > b1.length) ? -1 : 0;
		}
	}
