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

package com.actelion.research.chem.descriptor;

public class DescriptorHelper implements DescriptorConstants {
	
	public static final String TAG_SIMILARITY = "Similarity";
	
    public static int getDescriptorType(String shortName) {
        DescriptorInfo descriptorInfo = getDescriptorInfo(unifyShortName(shortName));
        return (descriptorInfo == null) ? DESCRIPTOR_TYPE_UNKNOWN
                                        : descriptorInfo.type;
    }

    public static DescriptorInfo getDescriptorInfo(String shortName) {
        for (int i=0; i<DESCRIPTOR_EXTENDED_LIST.length; i++)
            if (DESCRIPTOR_EXTENDED_LIST[i].shortName.equals(unifyShortName(shortName)))
                return DESCRIPTOR_EXTENDED_LIST[i];
        return null;
    }

    public static boolean isBinaryFingerprint(String shortName) {
        DescriptorInfo descriptorInfo = getDescriptorInfo(unifyShortName(shortName));
        return (descriptorInfo == null) ? false
                                        : descriptorInfo.isBinary;
    }

    public static boolean isDescriptorShortName(String shortName) {
        for (int i=0; i<DESCRIPTOR_EXTENDED_LIST.length; i++)
            if (DESCRIPTOR_EXTENDED_LIST[i].shortName.equals(unifyShortName(shortName)))
                return true;
        return false;
    }

    private static String unifyShortName(String shortname) {
        return "PP3DMM2".equals(shortname) ? "Flexophore" : shortname;
    }

	/**
	 * Creates a header tag name from the descriptor short name.
	 * The tag is used to store virtual screening scores. 
	 * @param dh
	 * @return
	 */
	public static String getTagDescriptorSimilarity(ISimilarityCalculator<?> dh) {
		return TAG_SIMILARITY + dh.getInfo().shortName;
		}
	
	public static String getTagDescriptorSimilarity(DescriptorInfo dh){
		return TAG_SIMILARITY + dh.shortName;
		}
	}
