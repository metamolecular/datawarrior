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

package com.actelion.research.datawarrior.task;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;

import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.base.CMLException;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.cml.element.CMLMolecule;
import org.xmlcml.cml.element.CMLScalar;
import org.xmlcml.cml.legacy.molecule.MDLConverter;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.opsin.NameToStructureConfig;
import uk.ac.cam.ch.wwmm.opsin.OpsinResult;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableModel;

public class DETaskAddStructureFromName extends DETaskWithEmptyConfiguration {
	public static final String TASK_NAME = "Add Structures From Name";

	private static final String[] cSourceColumnName = { "substance name", "compound name", "iupac name" };

	private CompoundTableModel	mTableModel;

	public DETaskAddStructureFromName(DEFrame parentFrame) {
    	super(parentFrame, true);
    	mTableModel = parentFrame.getTableModel();
    	}

    @Override
	public boolean isConfigurable() {
    	for (String name:cSourceColumnName)
    		if (mTableModel.findColumn(name) != -1)
    			return true;

    	showErrorMessage("'Substance Name' column not found.");
    	return false;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
    	int sourceColumn = -1;
		for (String name:cSourceColumnName) {
    		sourceColumn = mTableModel.findColumn(name);
    		if (sourceColumn != -1)
    			break;
			}

        int firstNewColumn = mTableModel.addNewColumns(3);
        int idcodeColumn = firstNewColumn;
        int coordsColumn = firstNewColumn+1;
        mTableModel.prepareStructureColumns(idcodeColumn, "Structure", true, true);

		NameToStructureConfig opsinConfig = new NameToStructureConfig();
		NameToStructure opsinN2S = NameToStructure.getInstance();
		StereoMolecule mol = new StereoMolecule();

		startProgress("Generating Structures...", 0, mTableModel.getTotalRowCount());
		for (int row=0; row<mTableModel.getTotalRowCount() && !threadMustDie(); row++) {
			updateProgress(row);

			String name = mTableModel.getTotalValueAt(row, sourceColumn);
			if (name.length() != 0) {
				try {
					OpsinResult result = opsinN2S.parseChemicalName(name, opsinConfig);

					Element el = result.getCml();
					if (el == null)
						continue;
					
					String xmlCML = el.toXML();
					if (xmlCML == null)
						continue;

					String sdf = convertCML2SDF(xmlCML);
					if (sdf == null)
						continue;

					mol.deleteMolecule();
					new MolfileParser().parse(mol, sdf);
					if (mol.getAllAtoms() != 0) {
						mol.setFragment(false);
						new CoordinateInventor().invent(mol);
						Canonizer canonizer = new Canonizer(mol);
						mTableModel.setTotalValueAt(canonizer.getIDCode(), row, idcodeColumn);
						mTableModel.setTotalValueAt(canonizer.getEncodedCoordinates(), row, coordsColumn);
						}
					}
				catch (Exception e) {
					e.printStackTrace();
					}
				}
			}

		mTableModel.finalizeNewColumns(firstNewColumn, this);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	private String convertCML2SDF(String cml) throws CMLException, IOException{
		InputStream is = new ByteArrayInputStream(cml.getBytes());
  		Document document = null;
  		try {
  			document = new CMLBuilder().build(is);
  			}
  		catch (Exception e) {
  			return null;
  			}

  		CMLMolecule molecule = (CMLMolecule) CMLUtil.getQueryNodes(document,
  				"//" + CMLMolecule.NS, CMLConstants.CML_XPATH).get(0);
  		List<Node> scalars = CMLUtil.getQueryNodes(molecule, "//"
  				+ CMLScalar.NS, CMLConstants.CML_XPATH);

  		for (Node node:scalars)
  			node.detach();
  		
  		MDLConverter mdlConverter = new MDLConverter();
  		StringWriter writer = new StringWriter();
  		mdlConverter.writeMOL(writer, molecule);
  		writer.flush();
  		return writer.toString();
		}
	}
