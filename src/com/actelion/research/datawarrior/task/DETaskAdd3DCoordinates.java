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

import info.clearthought.layout.TableLayout;

import java.util.List;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.calculator.TorsionCalculator;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.optimizer.AlgoLBFGS;
import com.actelion.research.forcefield.optimizer.EvaluableConformation;
import com.actelion.research.forcefield.optimizer.EvaluableForceField;
import com.actelion.research.table.CompoundTableModel;


public class DETaskAdd3DCoordinates extends DETaskAbstractAddChemProperty implements Runnable {
	public static final String TASK_NAME = "Calculate 3D-Coordinates";
	private static Properties sRecentConfiguration;

	private static final String PROPERTY_ALGORITHM = "algorithm";
	private static final String PROPERTY_MINIMIZE = "minimize";

	/*	 reduced options because of unclear copyright situation with CCDC concerning torsion statistics
	private static final int ADAPTIVE_RANDOM = 0;
	private static final int SELF_ORGANIZED = 1;
	private static final int ACTELION3D = 2;
	private static final int DEFAULT_ALGORITHM = ADAPTIVE_RANDOM;

	private static final String[] ALGORITHM_TEXT = { "Rule based assembly of self organized fragments", "Entirely self organized", "Actelion3D" };
	private static final String[] ALGORITHM_CODE = { "adaptiveRandom", "selfOrganized", "actelion3d" };	*/

	private static final String[] ALGORITHM_TEXT = { "Actelion3D" };
	private static final String[] ALGORITHM_CODE = { "actelion3d" };
	private static final int ACTELION3D = 0;
	private static final int DEFAULT_ALGORITHM = ACTELION3D;

	private JComboBox	mComboBoxAlgorithm;
	private JCheckBox	mCheckBoxMinimize;
	private int			mAlgorithm;
	private boolean		mMinimize;

	public DETaskAdd3DCoordinates(DEFrame parent, boolean isInteractive) {
		super(parent, DESCRIPTOR_NONE, false, true, isInteractive);
		}

	@Override
	public Properties getRecentConfiguration() {
		return sRecentConfiguration;
		}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		double[][] size = { {TableLayout.PREFERRED, 8, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Algorithm:"), "0,0");
		mComboBoxAlgorithm = new JComboBox(ALGORITHM_TEXT);
		ep.add(mComboBoxAlgorithm, "2,0");
		mCheckBoxMinimize = new JCheckBox("Forcefield energy minimization");
		ep.add(mCheckBoxMinimize, "2,2");
		return ep;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_ALGORITHM, ALGORITHM_CODE[mComboBoxAlgorithm.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_MINIMIZE, mCheckBoxMinimize.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxAlgorithm.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_ALGORITHM), ALGORITHM_CODE, DEFAULT_ALGORITHM));
		mCheckBoxMinimize.setSelected(configuration.getProperty(PROPERTY_MINIMIZE).equals("true"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxAlgorithm.setSelectedIndex(DEFAULT_ALGORITHM);
		mCheckBoxMinimize.setSelected(true);
		}

	@Override
	protected int getNewColumnCount() {
		return 1;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		getTableModel().setColumnProperty(firstNewColumn,
				CompoundTableModel.cColumnPropertySpecialType,
				CompoundTableModel.cColumnType3DCoordinates);
		getTableModel().setColumnProperty(firstNewColumn,
				CompoundTableModel.cColumnPropertyParentColumn,
				getTableModel().getColumnTitleNoAlias(getStructureColumn()));
		}

	@Override
	protected String getNewColumnName(int column) {
		String title = "3D-"+getTableModel().getColumnTitle(getStructureColumn());
		switch (mAlgorithm) {
		/*	 taken out because of unclear copyright situation with CCDC concerning torsion statistics
		case ADAPTIVE_RANDOM:
			return title + (mMinimize ? " (adaptive torsions - minimized)" : " (adaptive torsions)");
		case SELF_ORGANIZED:
			return title + (mMinimize ? " (self-organized - minimized)" : " (self-organized)");	*/
		case ACTELION3D:
			return title + (mMinimize ? " (Actelion3D - minimized)" : " (Actelion3D)");
		default:	// should not happen
			return CompoundTableModel.cColumnType3DCoordinates;
			}
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mAlgorithm = findListIndex(configuration.getProperty(PROPERTY_ALGORITHM), ALGORITHM_CODE, DEFAULT_ALGORITHM);
		mMinimize = configuration.getProperty(PROPERTY_MINIMIZE).equals("true");
		return true;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule mol) throws Exception {
		byte[] idcode = (byte[])getTableModel().getTotalRecord(row).getData(getStructureColumn());
		if (idcode != null) {
			if (mol != null)
				mol.deleteMolecule();

			boolean isOneStereoIsomer = false;	// must be overridden
			FFMolecule ffmol = null;

			switch (mAlgorithm) {
/*	 taken out because of unclear copyright situation with CCDC concerning torsion statistics
			case ADAPTIVE_RANDOM:
				// ConformationGenerator based: adaptive random
				// (since we only create one conformer, the strategy is not very important)
				new IDCodeParser(true).parse(mol, idcode);
				isOneStereoIsomer = !hasMultipleStereoIsomers(mol);
				if (mol.getAllAtoms() != 0)
					mol = new ConformerGenerator(12345L).getOneConformer(mol);
			break;
			case SELF_ORGANIZED:
				//from here ConformationSampler based
				new IDCodeParser(true).parse(mol, idcode);
				isOneStereoIsomer = !hasMultipleStereoIsomers(mol);
				ConformationSelfOrganizer sampler = new ConformationSelfOrganizer(mol, false);
				sampler.generateOneConformerInPlace(0);
				break;	*/
			case ACTELION3D:
				// from here AdvancedTools based
				mol = getChemicalStructure(row, mol);
				isOneStereoIsomer = !hasMultipleStereoIsomers(mol);
				if (mol != null && mol.getAllAtoms() != 0) {
					try {
						List<FFMolecule> isomerList = TorsionCalculator.createAllConformations(new FFMolecule(mol));
						if (isomerList.size() != 0)
							ffmol = isomerList.get(0);
						}
					catch (Exception e) {
						e.printStackTrace();
						}
					}
				break;
				}

			if (mol != null && mol.getAllAtoms() != 0 && mMinimize) {
				if (ffmol == null)
					ffmol = new FFMolecule(mol);	 

				ForceField f = new ForceField(ffmol);
				new AlgoLBFGS().optimize(new EvaluableConformation(f));	//optimize torsions -> 6+nRot degrees of freedom, no change of angles and bond distances
				new AlgoLBFGS().optimize(new EvaluableForceField(f));	//optimize cartesians -> 3n degrees of freedem

				// EvaluableForcefield -> optimize everything in a cartesian referential
				// EvaluableConformation -> optimize the torsions in the torsion referential
				// EvaluableDockFlex -> optimize the torsions + translation/rotation in the torsion referential
				// EvaluableDockRigid -> optimize the translation/rotation in the cartesian referential

				// AlgoLBFGS -> faster algo
				// AlgoConjugateGradient -> very slow, not used anymore
				// AlgoMD -> test of molecular dynamic, not a optimization
				}

			if (ffmol != null) {
				for (int atom=0; atom<mol.getAtoms(); atom++) {
					mol.setAtomX(atom, ffmol.getAtomX(atom));
					mol.setAtomY(atom, ffmol.getAtomY(atom));
					mol.setAtomZ(atom, ffmol.getAtomZ(atom));
					}
				}

			if (mol != null && mol.getAllAtoms() != 0) {
				Canonizer canonizer = new Canonizer(mol);
				if (isOneStereoIsomer	// a final conformer is one stereo isomer
				 && !canonizer.getIDCode().equals(getTableModel().getTotalValueAt(row, getStructureColumn()))) {
					System.out.println("WARNING: idcodes after 3D-coordinate generation differ!!!");
					System.out.println("old: "+getTableModel().getTotalValueAt(row, getStructureColumn()));
					System.out.println("new: "+canonizer.getIDCode());
					}
				getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(true), row, firstNewColumn);
				}
			else {
				getTableModel().setTotalValueAt(null, row, firstNewColumn);
				}
			}
		}

	private boolean hasMultipleStereoIsomers(StereoMolecule mol) {
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (mol.getAtomParity(atom) == Molecule.cAtomParityUnknown
			 || (mol.isAtomStereoCenter(atom) && mol.getAtomESRType(atom) != Molecule.cESRTypeAbs))
				return true;
		for (int bond=0; bond<mol.getBonds(); bond++)
			if (mol.getBondParity(bond) == Molecule.cBondParityUnknown)
				return true;

		return false;
		}
	}
