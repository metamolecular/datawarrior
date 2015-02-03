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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;

public abstract class CompoundFileHelper {
	public static final int cFileTypeMask = 0x0000FFFF;
	public static final int cFileTypeDataWarrior = 0x00000001;
	public static final int cFileTypeDataWarriorTemplate = 0x00000002;
	public static final int cFileTypeDataWarriorQuery = 0x00000004;
	public static final int cFileTypeDataWarriorMacro = 0x00000008;
	public static final int cFileTypeTextTabDelimited = 0x00000010;
    public static final int cFileTypeTextCommaSeparated = 0x00000020;
    public static final int cFileTypeText = cFileTypeTextTabDelimited | cFileTypeTextCommaSeparated;
	public static final int cFileTypeSDV3 = 0x00000040;
    public static final int cFileTypeSDV2 = 0x00000080;
    public static final int cFileTypeSD = cFileTypeSDV3 | cFileTypeSDV2;
	public static final int cFileTypeDataWarriorCompatibleData = cFileTypeDataWarrior | cFileTypeText | cFileTypeSD;
	public static final int cFileTypeDataWarriorTemplateContaining = cFileTypeDataWarrior | cFileTypeDataWarriorQuery | cFileTypeDataWarriorTemplate;
	public static final int cFileTypeRXN = 0x00000100;
	public static final int cFileTypeSOM = 0x00000200;
	public static final int cFileTypeJPG = 0x00000400;
	public static final int cFileTypePNG = 0x00000800;
	public static final int cFileTypeSVG = 0x00001000;
	public static final int cFileTypePictureFile = cFileTypeJPG | cFileTypePNG | cFileTypeSVG;
    public static final int cFileTypeRDV3 = 0x00002000;
    public static final int cFileTypeRDV2 = 0x00004000;
    public static final int cFileTypeRD = cFileTypeRDV3 | cFileTypeRDV2;
    public static final int cFileTypeUnknown = -1;

	private static File sCurrentDirectory;
	private int mRecordCount,mErrorCount;

	public abstract String selectOption(String message, String title, String[] option);
	public abstract File selectFileToOpen(String title, int filetypes);
	public abstract String selectFileToSave(String title, int filetype, String newFileName);
	public abstract void showMessage(String message);

	public static File getCurrentDirectory() {
		return sCurrentDirectory;
		}

	public static void setCurrentDirectory(File d) {
		sCurrentDirectory = d;
		}

	public ArrayList<StereoMolecule> readStructuresFromFile(boolean readIdentifier) {
        File file = selectFileToOpen("Please select substance file",
                               CompoundFileHelper.cFileTypeSD
                             | CompoundFileHelper.cFileTypeDataWarrior);

        return readStructuresFromFile(file, readIdentifier);
	    }

	public ArrayList<String> readIDCodesFromFile() {
        File file = selectFileToOpen("Please select substance file",
        					   CompoundFileHelper.cFileTypeSD
                             | CompoundFileHelper.cFileTypeDataWarrior);

        return readIDCodesFromFile(file);
	    }

	public ArrayList<StereoMolecule> readStructuresFromFile(File file, boolean readIdentifier) {
        if (file == null)
            return null;

        ArrayList<StereoMolecule> moleculeList = new ArrayList<StereoMolecule>();
        readChemObjectsFromFile(file, moleculeList, null, readIdentifier);

        return moleculeList;
	    }

	public ArrayList<String> readIDCodesFromFile(File file) {
        if (file == null)
            return null;

        ArrayList<String> idcodeList = new ArrayList<String>();
        readChemObjectsFromFile(file, null, idcodeList, false);

        return idcodeList;
	    }

	private void readChemObjectsFromFile(File file,
	                                            ArrayList<StereoMolecule> moleculeList,
	                                            ArrayList<String> idcodeList,
	                                            boolean readIdentifier) {
	    mRecordCount = 0;
	    mErrorCount = 0;
	    String filename = file.getName();
	    int index = filename.indexOf('.');
	    String extention = (index == -1) ? "" : filename.substring(index).toLowerCase();
	    CompoundFileParser parser = (extention.equals(".sdf")) ?
	                                           new SDFileParser(file)
	                              : (extention.equals(".dwar")) ?
	                                           new DWARFileParser(file)
	                              : (extention.equals(".ode")) ?
	                                           new ODEFileParser(file) : null;

	    // If we create molecules,
	    // then we might set the name field with the proper identifier
	    int indexOfID = -1;
	    if (readIdentifier && moleculeList != null) {
	        String[] fieldNames = parser.getFieldNames();
	        if (fieldNames != null && fieldNames.length != 0) {
	            String id = selectOption("Select compound name or identifier", filename, fieldNames);
	            if (id != null)
	            	for (int i=0; i<fieldNames.length; i++)
	            		if (fieldNames[i].equals(id))
	            			{ indexOfID = i; break; }
	            }
	        if (parser instanceof SDFileParser)
	            parser = new SDFileParser(file, fieldNames);
	        }

	    while (parser.next()) {
	        mRecordCount++;
	        boolean isError = false;
	        if (moleculeList != null) {
	            StereoMolecule mol = parser.getMolecule();
	            if (mol != null) {
	                if (indexOfID != -1)
	                    mol.setName(parser.getFieldData(indexOfID));
                    moleculeList.add(mol);
                    }
	            else {
	                isError = true;
	                }
	            }

	        if (idcodeList != null) {
	            String idcode = parser.getIDCode();
	            if (idcode != null)
	                idcodeList.add(idcode);
	            else
                    isError = true;
    	        }

	        if (isError)
	            mErrorCount++;
    	    }
    	}

    public int getRecordCount() {
        return mRecordCount;
        }

	public int getErrorCount() {
	    return mErrorCount;
	    }
	
	public static CompoundFileFilter createFileFilter(int filetypes, boolean isSaving) {
		CompoundFileFilter filter = new CompoundFileFilter();
		if ((filetypes & cFileTypeDataWarrior) != 0) {
            filter.addExtension("dwar");
            if (!isSaving)
                filter.addExtension("ode");  // old extention
			filter.addDescription("DataWarrior data files");
			}
		if ((filetypes & cFileTypeDataWarriorTemplate) != 0) {
            filter.addExtension("dwat");
            if (!isSaving)
                filter.addExtension("odt");  // old extention
			filter.addDescription("DataWarrior template files");
			}
		if ((filetypes & cFileTypeDataWarriorQuery) != 0) {
            filter.addExtension("dwaq");
            if (!isSaving)
                filter.addExtension("odq");  // old extention
			filter.addDescription("DataWarrior query files");
			}
		if ((filetypes & cFileTypeDataWarriorMacro) != 0) {
            filter.addExtension("dwam");
			filter.addDescription("DataWarrior macro files");
			}
		if ((filetypes & cFileTypeTextTabDelimited) != 0) {
			filter.addExtension("txt");
			filter.addDescription("TAB delimited text files");
			}
        if ((filetypes & cFileTypeTextCommaSeparated) != 0) {
            filter.addExtension("csv");
            filter.addDescription("Comma separated text files");
            }
		if ((filetypes & cFileTypeRXN) != 0) {
			filter.addExtension("rxn");
			filter.addDescription("MDL reaction files");
			}
		if ((filetypes & cFileTypeSD) != 0) {
			filter.addExtension("sdf");
			filter.addDescription("MDL SD-files");
			}
		if ((filetypes & cFileTypeRD) != 0) {
			filter.addExtension("rdf");
			filter.addDescription("MDL RD-files");
			}
		if ((filetypes & cFileTypeSOM) != 0) {
            filter.addExtension("dwas");
            if (!isSaving)
                filter.addExtension("som");  // old extention
			filter.addDescription("DataWarrior self organized map");
			}
		if ((filetypes & cFileTypeJPG) != 0) {
			filter.addExtension("jpg");
			filter.addExtension("jpeg");
			filter.addDescription("JPEG image files");
			}
		if ((filetypes & cFileTypePNG) != 0) {
			filter.addExtension("png");
			filter.addDescription("PNG image files");
			}
		if ((filetypes & cFileTypeSVG) != 0) {
			filter.addExtension("svg");
			filter.addDescription("scalable vector graphics files");
			}

        if (filetypes == cFileTypeDataWarriorCompatibleData) {
            filter.setDescription("DataWarrior compatible data files");
            }
        if (filetypes == cFileTypeDataWarriorTemplateContaining) {
            filter.setDescription("Files containing a DataWarrior template");
            }
        if (filetypes == cFileTypePictureFile) {
            filter.setDescription("Image files");
            }

		return filter;
		}

	/**
	 * Provided that fileName has a leading file path, then path and separator are removed.
	 * Provided that fileName has a recognized extension, then the extension is removed.
	 * @param filePath file name with or without complete path and with or without extension
	 * @return naked file name without leading path and extension
	 */
	public static String removePathAndExtension(String filePath) {
		int i1 = filePath.lastIndexOf(File.separatorChar);
		int i2 = (getFileType(filePath) != cFileTypeUnknown) ?
				filePath.lastIndexOf('.') : -1;
		if (i1 == -1)
			return (i2 == -1) ? filePath : filePath.substring(0, i2);
		else
			return (i2 == -1) ? filePath.substring(i1+1) : filePath.substring(i1+1, i2);
		}

	/**
	 * Provided that fileName has a leading file path, then path and separator are removed.
	 * @param filePath file name with or without complete path and with or without extension
	 * @return file name or path without extension
	 */
	public static String removeExtension(String filePath) {
		int i = (getFileType(filePath) != cFileTypeUnknown) ?
				filePath.lastIndexOf('.') : -1;
		return (i == -1) ? filePath : filePath.substring(0, i);
		}

	public static int getFileType(String filename) {
        int index = filename.lastIndexOf('.');

        if (index == -1)
            return cFileTypeUnknown;

        String extension = filename.substring(index).toLowerCase();
        if (extension.equals(".dwar") || extension.equals(".ode"))
            return cFileTypeDataWarrior;
        if (extension.equals(".dwat") || extension.equals(".odt"))
            return cFileTypeDataWarriorTemplate;
        if (extension.equals(".dwaq") || extension.equals(".odq"))
            return cFileTypeDataWarriorQuery;
        if (extension.equals(".dwas") || extension.equals(".som"))
            return cFileTypeSOM;
        if (extension.equals(".dwam"))
            return cFileTypeDataWarriorMacro;
        if (extension.equals(".txt"))
            return cFileTypeTextTabDelimited;
        if (extension.equals(".csv"))
            return cFileTypeTextCommaSeparated;
        if (extension.equals(".sdf"))
            return cFileTypeSD;
        if (extension.equals(".rdf"))
            return cFileTypeRD;
        if (extension.equals(".rxn"))
            return cFileTypeRXN;
        if (extension.equals(".jpg") || extension.equals(".jpeg"))
            return cFileTypeJPG;
        if (extension.equals(".png"))
            return cFileTypePNG;
        if (extension.equals(".svg"))
            return cFileTypeSVG;

        return cFileTypeUnknown;
        }

    /**
     * @param fileTypes
     * @return list of all extensions that are covered by fileTypes
     */
    public ArrayList<String> getExtensionList(int fileTypes) {
    	ArrayList<String> list = new ArrayList<String>();
    	int type = 0x00000001;
    	while ((type & cFileTypeMask) != 0) {
    		if ((type & fileTypes) != 0) {
    			String extension = getExtension(type);
    			if (extension.length() != 0 && !list.contains(extension))
    				list.add(extension);
    			}
    		type <<= 1;
    		}
    	return list;
    	}

    public static String getExtension(int filetype) {
		String extension = "";
		switch (filetype) {
		case cFileTypeDataWarrior:
			extension = ".dwar";
			break;
		case cFileTypeDataWarriorQuery:
			extension = ".dwaq";
			break;
		case cFileTypeDataWarriorTemplate:
			extension = ".dwat";
			break;
		case cFileTypeDataWarriorMacro:
			extension = ".dwam";
			break;
		case cFileTypeTextTabDelimited:
			extension = ".txt";
			break;
        case cFileTypeTextCommaSeparated:
            extension = ".csv";
            break;
		case cFileTypeSD:
        case cFileTypeSDV2:
        case cFileTypeSDV3:
			extension = ".sdf";
			break;
		case cFileTypeRD:
        case cFileTypeRDV2:
        case cFileTypeRDV3:
			extension = ".rdf";
			break;
		case cFileTypeRXN:
			extension = ".rxn";
			break;
		case cFileTypeSOM:
			extension = ".dwas";
			break;
		case cFileTypeJPG:
			extension = ".jpeg";
			break;
		case cFileTypePNG:
			extension = ".png";
			break;
		case cFileTypeSVG:
			extension = ".svg";
			break;
			}
		return extension;
		}

	public void saveRXNFile(Reaction rxn) {
		String fileName = selectFileToSave("Select reaction file", cFileTypeRXN, "Untitled Reaction");
		if (fileName != null) {
			String extension = ".rxn";
			int dotIndex = fileName.lastIndexOf('.');
			int slashIndex = fileName.lastIndexOf(File.separator);
			if (dotIndex == -1
			 || dotIndex < slashIndex)
				fileName = fileName.concat(extension);
		    else if (!fileName.substring(dotIndex).equalsIgnoreCase(extension)) {
				showMessage("uncompatible file name extension.");
			    return;
				}

			try {
				BufferedWriter theWriter = new BufferedWriter(new FileWriter(new File(fileName)));
				theWriter.write("$RXN");
				theWriter.newLine();
				theWriter.newLine();
				theWriter.newLine();
				theWriter.newLine();
				theWriter.write("  "+rxn.getReactants()+"  "+rxn.getProducts());
				theWriter.newLine();

				for (int i=0; i<rxn.getMolecules(); i++) {
					theWriter.write("$MOL");
					theWriter.newLine();
					new MolfileCreator(rxn.getMolecule(i)).writeMolfile(theWriter);
					}
				theWriter.close();
				}
			catch (IOException e) {
				showMessage("IOException: "+e);
				}
			}
		}
	}
