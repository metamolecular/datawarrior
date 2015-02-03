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
 * @author Christian Rufener
 */

package com.actelion.research.chem.io;

import java.io.*;

import com.actelion.research.chem.ChemistryHelper;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.reaction.Reaction;

public class RXNFileCreator
{
    private StringBuffer rxnbuffer = null;

    public RXNFileCreator(Reaction rxn)
    {
        this(rxn, null);
    }

    public RXNFileCreator(Reaction r, String programName)
    {
        Reaction rxn = new Reaction(r);
        try {
            StringWriter theWriter = new StringWriter();
            theWriter.write("$RXN\n");
            theWriter.write(programName != null ? programName : "");
            theWriter.write("\n\n\n");
            theWriter.write("  "+rxn.getReactants()+"  "+rxn.getProducts() + "\n");

            // Scale down reaction x/y coordinates to result in numbers < 1.00 since the molfilecreator will scale down
            // each molecule individually, which might result in different scaled coordinates which WILL be a problem
            // in reactions!
            scaleReaction(rxn);

            for (int i=0; i<rxn.getMolecules(); i++) {
                theWriter.write("$MOL\n");
                new MolfileCreator(rxn.getMolecule(i),false).writeMolfile(theWriter);
            }
            rxnbuffer = theWriter.getBuffer();
            theWriter.close();
        } catch (Exception e) {
            System.err.println("Error in RXNFileCreator: " + e);
        }
    }

    private double log10(double d)
    {
        return Math.log(d)/Math.log(10);
    }

    private void scaleReaction(Reaction r)
    {
        java.awt.geom.Rectangle2D rc = ChemistryHelper.getBoundingRect(r,true);
        if (rc != null) {
            double x = Math.max(Math.abs(rc.getMinX()),Math.abs(rc.getMaxX()));
            double y = Math.max(Math.abs(rc.getMinY()),Math.abs(rc.getMaxY()));
            int size = ((int)Math.max(log10(x),log10(y))+1);
            double scale = 1/(Math.pow(10,size));
            ChemistryHelper.transformReaction(r,0,0,scale);
        }
   }

    public String getRXNfile()
    {
        return rxnbuffer != null ? rxnbuffer.toString() : null;
    }


    public void writeRXNfile(Writer theWriter) throws IOException
    {
        if (rxnbuffer == null)
            throw new IOException("NULL RXNFileBuffer!");
        theWriter.write(rxnbuffer.toString());
    }
}
