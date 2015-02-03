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

package com.actelion.research.chem.io;

public interface CompoundTableConstants {
    public static final String cColumnUnassignedItemText = "<Unassigned>";
    public static final String cColumnUnassignedCode = "<none>";
    public static final String cColumnNameRowList = "List '";

	// visible special columns
    public static final String cColumnTypeIDCode = "idcode";
    public static final String cColumnTypeRXNCode = "rxncode";

    public static final String[] cParentSpecialColumnTypes = {
                                    cColumnTypeIDCode,
                                    cColumnTypeRXNCode };

        // non-parent special columns cannot be displayed
    public static final String cColumnType2DCoordinates = "idcoordinates2D";
    public static final String cColumnType3DCoordinates = "idcoordinates3D";
    public static final String cColumnTypeAtomColorInfo = "atomColorInfo";
        // in addition to these all DescriptorHandler.SHORT_NAMEs are valid column types

    public static final int cStringExclusionTypeContains = 1;
    public static final int cStringExclusionTypeStartsWith = 2;
    public static final int cStringExclusionTypeEquals = 3;
    public static final int cStringExclusionTypeRegEx = 4;

    public static final int cMaxTextCategoryCount = 65536;
    public static final int cMaxDateOrDoubleCategoryCount = 256;

    // summary mode for displaying values.
    public static final int cSummaryModeNormal = 0;
    public static final int cSummaryModeMean = 1;
    public static final int cSummaryModeMedian = 2;
    public static final int cSummaryModeMinimum = 3;
    public static final int cSummaryModeMaximum = 4;
    public static final int cSummaryModeSum = 5;
    public static final String[] cSummaryModeText = { "All Values", "Mean Value", "Median Value", "Lowest Value", "Highest Value", "Sum" };
    public static final String[] cSummaryModeOption = { "displayNormal","displayMean","displayMedian","displayMin","displayMax","displaySum" };

    // highlight mode for part-of-structure highlighting depending on current record similarity
    public static final int cStructureHiliteModeFilter = 0;
    public static final int cStructureHiliteModeCurrentRow = 1;
    public static final int cStructureHiliteModeNone = 2;
    public static final String[] cStructureHiliteModeText = { "Recent Filter", "Current Row Similarity", "No Highlighting" };
    public static final String[] cHiliteModeOption = { "hiliteFilter", "hiliteCurrent", "hiliteNone" };

	public static final String NEWLINE_STRING = "<NL>";	// used in .dwar, .txt and .cvs files to indicated next line within a cell
    public static final String cEntrySeparator = "; ";
    public static final byte[] cEntrySeparatorBytes = { ';', ' '};
    public static final String cLineSeparator = "\n";
    public static final byte cLineSeparatorByte = '\n'; // this must be equal to cLineSeparator
    public static final String cRangeSeparation = " <= x < ";
    public static final String cRangeNotAvailable = "<none>";
    public static final String cUnknownNumericalValue = "?";
    public static final String cDefaultDetailSeparator = "|#|";
    public static final String cDetailIndexSeparator = ":";
    public static final String cTextMultipleCategories = "<multiple categories>";

    public static final String cColumnPropertyUseThumbNail = "useThumbNail";
    public static final String cColumnPropertyImagePath = "imagePath";
    public static final String cColumnPropertySpecialType = "specialType";
    public static final String cColumnPropertyParentColumn = "parent";
    public static final String cColumnPropertyIdentifierColumn = "idColumn";
    public static final String cColumnPropertyIsClusterNo = "isClusterNo";
    public static final String cColumnPropertyDataMin = "dataMin";
    public static final String cColumnPropertyDataMax = "dataMax";
    public static final String cColumnPropertyCyclicDataMax = "cyclicDataMax";
    public static final String cColumnPropertyDetailCount = "detailCount";
    public static final String cColumnPropertyDetailName = "detailName";
    public static final String cColumnPropertyDetailType = "detailType";
    public static final String cColumnPropertyDetailSource = "detailSource";
    public static final String cColumnPropertyDetailSeparator = "detailSeparator";
    public static final String cColumnPropertyDescriptorVersion = "version";
    public static final String cColumnPropertyIsDisplayable = "isDisplayable";
    public static final String cColumnPropertyBinBase = "binBase";
    public static final String cColumnPropertyBinSize = "binSize";
    public static final String cColumnPropertyBinIsLog = "binIsLog";
    public static final String cColumnPropertyLookupCount = "lookupCount";
    public static final String cColumnPropertyLookupName = "lookupName";
    public static final String cColumnPropertyLookupURL = "lookupURL";
    public static final String cColumnPropertyReferencedColumn = "refColumn";
    public static final String cColumnPropertyReferenceStrengthColumn = "refStrengthColumn";
    public static final String cColumnPropertyReferenceType = "refType";
    public static final String cColumnPropertyReferenceTypeRedundant = "redundant";	// a connection is always referenced on both records
    public static final String cColumnPropertyReferenceTypeTopDown = "topdown";	// a connection is only referenced from top record

    public static final String cNativeFileHeaderStart = "<datawarrior-fileinfo>";
    public static final String cNativeFileHeaderEnd = "</datawarrior-fileinfo>";
    public static final String cNativeFileVersion = "version";
    public static final String cNativeFileRowCount = "rowcount";
    public static final String cFileExplanationStart = "<fileexplanation format=html>";
    public static final String cFileExplanationEnd = "</fileexplanation>";

    public static final String cColumnPropertyStart = "<column properties>";
    public static final String cColumnPropertyEnd = "</column properties>";
    public static final String cColumnName = "columnName";
    public static final String cColumnProperty = "columnProperty";

    public static final String cHitlistDataStart = "<hitlist data>";
    public static final String cHitlistDataEnd = "</hitlist data>";
    public static final String cHitlistName = "hitlistName";
    public static final String cHitlistData = "hitlistData";

    public static final String cDetailDataStart = "<detail data>";
    public static final String cDetailDataEnd = "</detail data>";
    public static final String cDetailID = "detailID";

    public static final String cPropertiesStart = "<datawarrior properties>";
    public static final String cPropertiesEnd = "</datawarrior properties>";

    public static final String cExtensionNameFileExplanation = "explanation";
    public static final String cExtensionNameMacroList = "macroList";
	}
