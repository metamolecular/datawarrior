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
 * @author Thomas Sander, Christian Rufener
 */

package com.actelion.research.chem.io;

import java.io.*;

import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;


public class RXNFileParser
{
    private static final String RXN_V3_COUNTS_LINE = "M  V30 COUNTS";
    private static final String V30_BEGIN_REACTANT = "M  V30 BEGIN REACTANT";
    private static final String V30_BEGIN_PRODUCT = "M  V30 BEGIN PRODUCT";
    private static final String RXN_MAGIC = "$RXN";
    private static final String RXN_V3_MAGIC = "$RXN V3000";
    private static final String END_CTAB = "M  V30 END CTAB";
    private static final String END_MOL_TAG = "M  END";
    private static final String MOL_MAGIC = "$MOL";
    private static final String AUX_MOLFILE_HEADER = "\nActelion Java MolfileCreator 2.0\n\n  0  0  0  0  0  0              0 V3000\n";
    private static final int DEFAULT_CAPACITY = 32768;

    public RXNFileParser()
	{
	}

	public Reaction getReaction(String buffer) throws Exception
	{
		Reaction theReaction = new Reaction();
		BufferedReader theReader = new BufferedReader(new StringReader(buffer));
		parse(theReaction, theReader);

		return theReaction;
	}

	public Reaction getReaction(File file) throws Exception
	{
		Reaction theReaction = new Reaction();
		BufferedReader theReader = new BufferedReader(new FileReader(file));
		parse(theReaction, theReader);

		return theReaction;
	}

	public boolean parse(Reaction theReaction, String buffer)
		throws Exception
	{
		BufferedReader theReader = new BufferedReader(new StringReader(buffer));

		return parse(theReaction, theReader);
	}

	public boolean parse(Reaction theReaction, File file)
		throws Exception
	{
		BufferedReader theReader = new BufferedReader(new FileReader(file));

		return parse(theReaction, theReader);
	}

    private boolean parse(Reaction theReaction, BufferedReader theReader) throws Exception
    {
        String theLine = theReader.readLine();
        boolean ok = false;
        if ((theLine == null) || !theLine.startsWith(RXN_MAGIC)) {
            throw new Exception("'$RXN' tag not found");
        }
        if (theLine.equals(RXN_V3_MAGIC)) {
            ok = parseV3(theReaction,theReader);
        } else {
            ok = parseV2(theReaction,theReader);
        }
        return ok;
    }

    // First line is already parsed
    private boolean parseV3(Reaction theReaction, BufferedReader theReader) throws Exception
    {
        boolean ok = false;

        String theLine = "";
        for (int i = 0; i < 4; i++) {
            theLine = theReader.readLine();
        }
        MolfileParser molParser = new MolfileParser();
        if (theLine != null && theLine.startsWith(RXN_V3_COUNTS_LINE)) {
            String t = theLine.substring(13).trim();
            String p[] = t.split(" ");
            int reactantCount = Integer.parseInt(p[0]);
            int productCount = Integer.parseInt(p[1]);
            if (reactantCount > 0) {
                theLine = theReader.readLine();
                if (V30_BEGIN_REACTANT.equals(theLine)) {
                    for (int i = 0; i < reactantCount; i++) {
                        StereoMolecule molecule = new StereoMolecule();
                        StringBuffer molfile = new StringBuffer(DEFAULT_CAPACITY);
                        molfile.append(AUX_MOLFILE_HEADER);
                        do {
                            theLine = theReader.readLine();
                            molfile.append(theLine);
                            molfile.append("\n");
                        } while ((theLine != null) && !theLine.startsWith(END_CTAB));
                        molParser.parse(molecule,molfile);
                        theReaction.addReactant(molecule);
                    }
                }
                
                theLine = theReader.readLine(); //  end reactant line
            }
            if (productCount > 0) {
                theLine = theReader.readLine();
                if (V30_BEGIN_PRODUCT.equals(theLine)) {
                    for (int i = 0; i < productCount; i++) {
                        StereoMolecule molecule = new StereoMolecule();
                        StringBuffer molfile = new StringBuffer(DEFAULT_CAPACITY);
                        molfile.append(AUX_MOLFILE_HEADER);
                        do {
                            theLine = theReader.readLine();
                            molfile.append(theLine);
                            molfile.append("\n");
                        } while ((theLine != null) && !theLine.startsWith(END_CTAB));
                        molParser.parse(molecule,molfile);
                        theReaction.addProduct(molecule);
                    }
                    theLine = theReader.readLine(); //  end product line
                }
            }
            ok = true;
        }
        return ok;
    }

	// This is the old approach, RXN specs were missing so we invented one
    private boolean parseOld(Reaction theReaction, BufferedReader theReader)
		throws Exception
	{
		String theLine = theReader.readLine();

		if ((theLine == null) || !theLine.startsWith(RXN_MAGIC)) {
			throw new Exception("'$RXN' tag not found");
		}

		for (int i = 0; i < 4; i++) {
			theLine = theReader.readLine();
		}
		int reactantCount = Integer.parseInt(theLine.substring(0, 3).trim());
		int productCount = Integer.parseInt(theLine.substring(3, 6).trim());

		MolfileParser molParser = new MolfileParser();

		for (int i = 0; i < reactantCount; i++) {
			theLine = theReader.readLine();

			if ((theLine == null) || !theLine.startsWith(MOL_MAGIC)) {
				throw new Exception("'$MOL' tag not found");
			}

            StereoMolecule reactant = new StereoMolecule();
			StringBuffer molfile = new StringBuffer(DEFAULT_CAPACITY);

			do {
				theLine = theReader.readLine();
				molfile.append(theLine);
				molfile.append("\n");
			} while ((theLine != null) && !theLine.startsWith(END_MOL_TAG));

			if (theLine == null) {
				throw new Exception("'M  END' not found");
			}

			molParser.parse(reactant, molfile);

			theReaction.addReactant(reactant);
		}

		for (int i = 0; i < productCount; i++) {
			theLine = theReader.readLine();

			if ((theLine == null) || !theLine.startsWith(MOL_MAGIC)) {
				throw new Exception("'$MOL' tag not found");
			}

            StereoMolecule product = new StereoMolecule();
			StringBuffer molfile = new StringBuffer(DEFAULT_CAPACITY);

			do {
				theLine = theReader.readLine();
				molfile.append(theLine);
				molfile.append("\n");
			} while ((theLine != null) && !theLine.startsWith(END_MOL_TAG));

			if (theLine == null) {
				throw new Exception("'M  END' not found");
			}

			molParser.parse(product, molfile);

			theReaction.addProduct(product);
		}

		theReader.close();

		return true;

		//return theReaction;
	}

    /**
     * @param theReaction
     * @param theReader
     * @return
     * @throws Exception
     */
    // First line is already parsed
    private boolean parseV2(Reaction theReaction, BufferedReader theReader) throws Exception
    {
        String theLine = "";
        for (int i = 0; i < 4; i++) {
            theLine = theReader.readLine();
        }
        int reactantCount = Integer.parseInt(theLine.substring(0, 3).trim());
        int productCount = Integer.parseInt(theLine.substring(3, 6).trim());

        MolfileParser molParser = new MolfileParser();

        for (int i = 0; i < reactantCount; i++) {
            theLine = theReader.readLine();

            if ((theLine == null) || !theLine.startsWith(MOL_MAGIC)) {
                throw new Exception("'$MOL' tag not found");
            }

            StereoMolecule reactant = new StereoMolecule();
            StringBuffer molfile = new StringBuffer(DEFAULT_CAPACITY);

            do {
                theLine = theReader.readLine();
                molfile.append(theLine);
                molfile.append("\n");
            } while ((theLine != null) && !theLine.startsWith(END_MOL_TAG));

            if (theLine == null) {
                throw new Exception("'M  END' not found");
            }

            molParser.parse(reactant, molfile);

            theReaction.addReactant(reactant);
        }

        for (int i = 0; i < productCount; i++) {
            theLine = theReader.readLine();

            if ((theLine == null) || !theLine.startsWith(MOL_MAGIC)) {
                throw new Exception("'$MOL' tag not found");
            }

            StereoMolecule product = new StereoMolecule();
            StringBuffer molfile = new StringBuffer(DEFAULT_CAPACITY);

            do {
                theLine = theReader.readLine();
                molfile.append(theLine);
                molfile.append("\n");
            } while ((theLine != null) && !theLine.startsWith(END_MOL_TAG));

            if (theLine == null) {
                throw new Exception("'M  END' not found");
            }

            molParser.parse(product, molfile);

            theReaction.addProduct(product);
        }

        theReader.close();

        return true;

        //return theReaction;
    }

}
