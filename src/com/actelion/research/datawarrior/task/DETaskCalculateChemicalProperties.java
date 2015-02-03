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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.pKaPlugin;
import chemaxon.marvin.plugin.PluginException;

import com.actelion.research.chem.AtomFunctionAnalyzer;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.MolecularFormula;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.NastyFunctionDetector;
import com.actelion.research.chem.RingCollection;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.MolecularFlexibilityCalculator;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.prediction.CLogPPredictor;
import com.actelion.research.chem.prediction.DruglikenessPredictorWithIndex;
import com.actelion.research.chem.prediction.PolarSurfaceAreaPredictor;
import com.actelion.research.chem.prediction.SolubilityPredictor;
import com.actelion.research.chem.prediction.ToxicityPredictor;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.util.DoubleFormat;

public class DETaskCalculateChemicalProperties extends ConfigurableTask {
	public static final String TASK_NAME = "Calculate Compound Properties";

    private static final String CHEMPROPERTY_LIST_SEPARATOR = "\t";
    private static final String CHEMPROPERTY_LIST_SEPARATOR_REGEX = "\\t";
    private static final String CHEMPROPERTY_OPTION_SEPARATOR = "|";
    private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
    private static final String PROPERTY_CHEMPROPERTY_LIST = "propertyList";

    private static final int PREDICTOR_COUNT			= 8;
    private static final int PREDICTOR_LOGP				= 0;
    private static final int PREDICTOR_LOGS				= 1;
    private static final int PREDICTOR_PKA				= 2;
    private static final int PREDICTOR_PSA				= 3;
    private static final int PREDICTOR_DRUGLIKENESS		= 4;
    private static final int PREDICTOR_TOXICITY			= 5;
    private static final int PREDICTOR_NASTY_FUNCTIONS	= 6;
    private static final int PREDICTOR_FLEXIBILITY		= 7;

    private static final int PREDICTOR_FLAG_LOGP			= (1 << PREDICTOR_LOGP);
    private static final int PREDICTOR_FLAG_LOGS			= (1 << PREDICTOR_LOGS);
    private static final int PREDICTOR_FLAG_PKA				= (1 << PREDICTOR_PKA);
    private static final int PREDICTOR_FLAG_PSA				= (1 << PREDICTOR_PSA);
    private static final int PREDICTOR_FLAG_DRUGLIKENESS	= (1 << PREDICTOR_DRUGLIKENESS);
    private static final int PREDICTOR_FLAG_TOXICITY		= (1 << PREDICTOR_TOXICITY);
    private static final int PREDICTOR_FLAG_NASTY_FUNCTIONS	= (1 << PREDICTOR_NASTY_FUNCTIONS);
    private static final int PREDICTOR_FLAG_FLEXIBILITY		= (1 << PREDICTOR_FLEXIBILITY);

    private static final int PROPERTY_COUNT = 40;

    private static final int TOTAL_WEIGHT = 0;
    private static final int FRAGMENT_WEIGHT = 1;
    private static final int FRAGMENT_ABS_WEIGHT = 2;
    private static final int LOGP = 3;
    private static final int LOGS = 4;
    private static final int LOGD = 5;
    private static final int ACIDIC_PKA = 6;
    private static final int BASIC_PKA = 7;
    private static final int ACCEPTORS = 8;
	private static final int DONORS = 9;
	private static final int PSA = 10;
	private static final int DRUGLIKENESS = 11;

	private static final int LE = 12;
//	private static final int SE = ;
	private static final int LLE = 13;
	private static final int LELP = 14;
	private static final int MUTAGENIC = 15;
	private static final int TUMORIGENIC = 16;
	private static final int REPRODUCTIVE_EFECTIVE = 17;
	private static final int IRRITANT = 18;
    private static final int NASTY_FUNCTIONS = 19;

	private static final int SHAPE = 20;
	private static final int FLEXIBILITY = 21;
	private static final int COMPLEXITY = 22;
	private static final int HEAVY_ATOMS = 23;
	private static final int NONCARBON_ATOMS = 24;
	private static final int METAL_ATOMS = 25;
	private static final int NEGATIVE_ATOMS = 26;
	private static final int STEREOCENTERS = 27;
	private static final int ROTATABLE_BONDS = 28;
	private static final int RINGS = 29;
	private static final int AROMATIC_RINGS = 30;
	private static final int SP3_ATOMS = 31;
	private static final int SYMMETRIC_ATOMS = 32;

	private static final int ALL_AMIDES = 33;
	private static final int ALL_AMINES = 34;
	private static final int ALKYL_AMINES = 35;
	private static final int ARYL_AMINES = 36;
	private static final int AROMATIC_NITROGEN = 37;
	private static final int BASIC_NITROGEN = 38;
	private static final int ACIDIC_OXYGEN = 39;

	private static final Color[] TOX_COLOR_LIST = { Color.RED, Color.YELLOW, Color.GREEN };

	private static final String[] PROPERTY_CODE = { "totalWeight", "fragmentWeight", "fragmentAbsWeight", "logP", "logS", "logD",
    												"acidicPKA", "basicPKA", "acceptors", "donors", "tpsa", "druglikeness",
    												"le", /*"se",*/ "lle", "lelp", "mutagenic", "tumorigenic", "reproEffective", "irritant", "nasty",
    												"shape", "flexibility", "complexity", "heavyAtoms", "nonCHAtoms", "metalAtoms", "negAtoms",
    												"stereoCenters", "rotBonds", "rings", "aromRings", "sp3Atoms", "symmetricAtoms",
    												"amides", "amines", "alkylAmines", "arylAmines", "aromN", "basicN", "acidicO" };

	private static final String[] TAB_GROUP = { "Druglikeness", "LE & Tox", "Shape & Counts", "Functional Groups" };

	private static Properties sRecentConfiguration;

	private DEFrame						mParentFrame;
	private CompoundTableModel			mTableModel;
	private DEProperty[]				mPropertyTable;
	private TreeMap<String,DEProperty>	mPropertyMap;
	private ArrayList<DEPropertyOrder>	mPropertyOrderList;
	private Object[]					mPredictor;
	private volatile int				mIDCodeColumn,mFragFpColumn,mFlexophoreColumn;
	private JComboBox					mComboBoxStructureColumn;
	private JTabbedPane					mTabbedPane;
	private DEPropertyGUI[]				mPropertyGUI;
    private AtomicInteger				mSMPRecordIndex,mSMPWorkingThreads,mSMPErrorCount;
    private boolean						mIsInteractive;

	public DETaskCalculateChemicalProperties(DEFrame parent, boolean isInteractive) {
    	super(parent, true);
    	mParentFrame = parent;
    	mTableModel = parent.getTableModel();
    	mIsInteractive = isInteractive;
    	}

	@Override
	public JPanel createDialogContent() {
		if (mPropertyMap == null)
			createPropertyMap();

		double[][] size1 = { {TableLayout.FILL, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, TableLayout.FILL},
							 {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED } };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size1));

		int[] structureColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);

		// create components
		mComboBoxStructureColumn = new JComboBox();
		if (structureColumn != null)
			for (int i=0; i<structureColumn.length; i++)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(structureColumn[i]));
		if (!mIsInteractive)
			mComboBoxStructureColumn.setEditable(true);
		content.add(new JLabel("Structure column:", JLabel.RIGHT), "1,1");
		content.add(mComboBoxStructureColumn, "3,1");

		mTabbedPane = new JTabbedPane();
        mPropertyGUI = new DEPropertyGUI[PROPERTY_COUNT];
		for (int tab=0; tab<TAB_GROUP.length; tab++) {
			JPanel cbp = new JPanel();
	        double[][] size2 = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8}, null };

	        int count = 0;
	        for (DEProperty property:mPropertyTable)
	        	if (property.tab == tab)
	        		count++;
	        size2[1] = new double[2*count];

	        count = 0;
	        for (DEProperty property:mPropertyTable) {
	        	if (property.tab == tab) {
	        		size2[1][2*count] = 2;
	        		size2[1][2*count+1] = TableLayout.PREFERRED;
	        		count++;
	        		}
				}
	        cbp.setLayout(new TableLayout(size2));
	
	        int row = 1;
	        for (DEProperty property:mPropertyTable) {
	        	if (property.tab == tab) {
	        		mPropertyGUI[property.type] = new DEPropertyGUI(property);
	    			if (property.dependentColumnFilter == null) {
	    				cbp.add(mPropertyGUI[property.type].getCheckBox(), "1,"+row+",3,"+row);
	    				}
	    			else {
	    				cbp.add(mPropertyGUI[property.type].getCheckBox(), "1,"+row);
						cbp.add(mPropertyGUI[property.type].getComboBox(), "3,"+row);
						if (mIsInteractive && mPropertyGUI[property.type].getComboBox().getItemCount() == 0)
							mPropertyGUI[property.type].getCheckBox().setEnabled(false);
						if (!mIsInteractive)
							mPropertyGUI[property.type].getComboBox().setEditable(true);
						}
		
					row += 2;
	        		}
				}

	        mTabbedPane.add(TAB_GROUP[tab], cbp);
			}

		content.add(mTabbedPane, "0,3,4,3");
		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#MolecularProperties";
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

		configuration.put(PROPERTY_STRUCTURE_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxStructureColumn.getSelectedItem()));

    	StringBuilder codeList = new StringBuilder();
    	for (int i=0; i<mPropertyGUI.length; i++) {
    		if (mPropertyGUI[i].getCheckBox().isEnabled() && mPropertyGUI[i].getCheckBox().isSelected()) {
    	    	if (codeList.length() != 0)
    	    		codeList.append(CHEMPROPERTY_LIST_SEPARATOR);

    	    	codeList.append(PROPERTY_CODE[i]);
    	    	if (mPropertyGUI[i].getComboBox() != null) {
    	    		codeList.append(CHEMPROPERTY_OPTION_SEPARATOR);
    	    		codeList.append(mTableModel.getColumnTitleNoAlias(mTableModel.findColumn((String)mPropertyGUI[i].getComboBox().getSelectedItem())));
    	    		}
    			}
    		}

    	if (codeList.length() != 0) {
    		configuration.put(PROPERTY_CHEMPROPERTY_LIST, codeList.toString());
    		}

    	return configuration;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		else if (!mIsInteractive)
			mComboBoxStructureColumn.setSelectedItem("Structure");

		for (int i=0; i<mPropertyGUI.length; i++)
			mPropertyGUI[i].getCheckBox().setSelected(false);

    	mTabbedPane.setSelectedIndex(0);
		}

    @Override
    public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");
		if (value.length() != 0) {
			int column = mTableModel.findColumn(value);
			if (column != -1)
				mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!mIsInteractive)
				mComboBoxStructureColumn.setSelectedItem(value);
			else if (mComboBoxStructureColumn.getItemCount() != 0)
				mComboBoxStructureColumn.setSelectedIndex(0);
			}
		else if (!mIsInteractive) {
			mComboBoxStructureColumn.setSelectedItem("Structure");
			}

		for (int i=0; i<mPropertyGUI.length; i++)
			mPropertyGUI[i].getCheckBox().setSelected(false);

    	value = configuration.getProperty(PROPERTY_CHEMPROPERTY_LIST);
		if (value == null)
			return;

		int lowestCheckedTab = Integer.MAX_VALUE;
		String[] codeList = value.split(CHEMPROPERTY_LIST_SEPARATOR_REGEX);
		for (String code:codeList) {
			int optionColumn = -2;
			int index = code.indexOf(CHEMPROPERTY_OPTION_SEPARATOR);
			if (index != -1) {
				String option = code.substring(index+CHEMPROPERTY_OPTION_SEPARATOR.length());
				optionColumn = mTableModel.findColumn(option);
				code = code.substring(0, index);
				}

			DEProperty property = getProperty(code);
			if (property != null && optionColumn != -1) {
				mPropertyGUI[property.type].getCheckBox().setSelected(true);
				if (lowestCheckedTab > property.tab)
					lowestCheckedTab = property.tab;
				if (optionColumn != -2)
					mPropertyGUI[property.type].getComboBox().setSelectedItem(mTableModel.getColumnTitle(optionColumn));
				}
			}

		if (lowestCheckedTab != Integer.MAX_VALUE)
			mTabbedPane.setSelectedIndex(lowestCheckedTab);
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode) == null) {
			showErrorMessage("No column with chemical structures found.");
			return false;
			}

		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int idcodeColumn = selectStructureColumn(configuration);
			if (idcodeColumn == -1) {
				showErrorMessage("Structure column not found.");
				return false;
				}
			}
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getRecentConfiguration() {
		return sRecentConfiguration;
		}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}

	private int selectStructureColumn(Properties configuration) {
		int[] idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumn.length == 1)
			return idcodeColumn[0];	// there is no choice
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN));
		for (int i=0; i<idcodeColumn.length; i++)
			if (column == idcodeColumn[i])
				return column;
		return -1;
		}

	private void addPropertyOrderIfValid(String propertyCode) {
		int index = propertyCode.indexOf(CHEMPROPERTY_OPTION_SEPARATOR);
		String propertyName = (index == -1) ? propertyCode : propertyCode.substring(0, index);
		DEProperty property = getProperty(propertyName);
		if (property == null) {
			showErrorMessage("Cannot calculate unknown property '"+propertyName+"'.");
			return;
			}

		int dependentColumn = -1;
		if (index != -1) {
			String option = propertyCode.substring(index+CHEMPROPERTY_OPTION_SEPARATOR.length());
			dependentColumn = mTableModel.findColumn(option);
			if (dependentColumn == -1) {
				showErrorMessage("Cannot calculate property '"+propertyName+"': Column '"+option+"' not found.");
				return;
				}
			if (!mTableModel.isColumnTypeDouble(dependentColumn)) {
				showErrorMessage("Cannot calculate property '"+propertyName+"': Column '"+option+"' not numerical.");
				return;
				}
			}

		mPropertyOrderList.add(new DEPropertyOrder(property, dependentColumn));
		}

	private DEProperty getProperty(String propertyName) {
		if (mPropertyMap == null)
			createPropertyMap();

		return mPropertyMap.get(propertyName);
		}

	private void ensurePredictor(int predictorFlags) {
		for (int i=0; i<PREDICTOR_COUNT; i++) {
			int flag = (1 << i);
			if ((predictorFlags & flag) != 0 && mPredictor[i] == null) {
				mPredictor[i] = (flag == PREDICTOR_FLAG_LOGP) ? new CLogPPredictor()
							  : (flag == PREDICTOR_FLAG_LOGS) ? new SolubilityPredictor()
							  : (flag == PREDICTOR_FLAG_PKA) ? new PKaPredictor()
							  : (flag == PREDICTOR_FLAG_PSA) ? new PolarSurfaceAreaPredictor()
							  : (flag == PREDICTOR_FLAG_DRUGLIKENESS) ? new DruglikenessPredictorWithIndex()
							  : (flag == PREDICTOR_FLAG_TOXICITY) ? new ToxicityPredictor()
							  : (flag == PREDICTOR_FLAG_NASTY_FUNCTIONS) ? new NastyFunctionDetector()
							  : (flag == PREDICTOR_FLAG_FLEXIBILITY) ? new MolecularFlexibilityCalculator()
//          				  : (flag == PREDICTOR_HERG) ? new RiskOf_hERGActPredictor()
							  : null;
				}
			}
		}

	private void createPropertyMap() {
		mPropertyMap = new TreeMap<String,DEProperty>();
		mPropertyTable = new DEProperty[PROPERTY_COUNT];

	   	addProperty(TOTAL_WEIGHT, 0, "Total Molweight", "Total molweight in g/mol; natural abundance");
    	addProperty(FRAGMENT_WEIGHT, 0, "Molweight", "Molweight of largest fragment in g/mol; natural abundance");
    	addProperty(FRAGMENT_ABS_WEIGHT, 0, "Absolute Weight", "Absolute weight of largest fragment in g/mol");
    	addProperty(LOGP, 0, "cLogP", "cLogP; P: conc(octanol)/conc(water)", null, null, PREDICTOR_FLAG_LOGP);
    	addProperty(LOGS, 0, "cLogS", "cLogS; S: water solubility in mol/l, pH=7.5, 25C", null, null, PREDICTOR_FLAG_LOGS);
    	addProperty(LOGD, 0, "cLogD (pH=7.4)", "cLogD at pH=7.4; via logP and ChemAxon pKa", null, null, PREDICTOR_FLAG_LOGP | PREDICTOR_FLAG_PKA);
    	addProperty(ACIDIC_PKA, 0, "acidic pKa", "lowest acidic pKa; ChemAxon method", null, null, PREDICTOR_FLAG_PKA);
    	addProperty(BASIC_PKA, 0, "basic pKa", "highest basic pKa; ChemAxon method", null, null, PREDICTOR_FLAG_PKA);
    	addProperty(ACCEPTORS, 0, "H-Acceptors", "H-Acceptors");
    	addProperty(DONORS, 0, "H-Donors", "H-Donors");
    	addProperty(PSA, 0, "Polar Surface Area", "Polar Surface Area (P. Ertl approach)", null, null, PREDICTOR_FLAG_PSA);
    	addProperty(DRUGLIKENESS, 0, "Druglikeness", "Druglikeness", null, DescriptorConstants.DESCRIPTOR_FFP512.shortName, PREDICTOR_FLAG_DRUGLIKENESS);

    	addProperty(LE, 1, "LE", "Ligand Efficiency (LE) from", "ic50", null, 0);
//    	addProperty(SE, 1, "SE", "Surface Efficiency (SE) from", "ic50", null, 0);
    	addProperty(LLE, 1, "LLE", "Lipophilic Ligand Efficiency (LLE) from", "ic50", null, PREDICTOR_FLAG_LOGP);
    	addProperty(LELP, 1, "LELP", "Ligand Efficiency Lipophilic Price (LELP) from", "ic50", null, PREDICTOR_FLAG_LOGP);
    	addProperty(MUTAGENIC, 1, "Mutagenic", "Mutagenic", null, null, PREDICTOR_FLAG_TOXICITY);
    	addProperty(TUMORIGENIC, 1, "Tumorigenic", "Tumorigenic", null, null, PREDICTOR_FLAG_TOXICITY);
    	addProperty(REPRODUCTIVE_EFECTIVE, 1, "Reproductive Effective", "Reproductive Effective", null, null, PREDICTOR_FLAG_TOXICITY);
    	addProperty(IRRITANT, 1, "Irritant", "Irritant", null, null, PREDICTOR_FLAG_TOXICITY);
    	addProperty(NASTY_FUNCTIONS, 1, "Nasty Functions", "Nasty Functions", null, DescriptorConstants.DESCRIPTOR_FFP512.shortName, PREDICTOR_FLAG_NASTY_FUNCTIONS);

    	addProperty(SHAPE, 2, "Shape Index", "Molecular Shape Index (spherical < 0.5 < linear)");
    	addProperty(FLEXIBILITY, 2, "Molecular Flexibility", "Molecular Flexibility (low < 0.5 < high)", null, null, PREDICTOR_FLAG_FLEXIBILITY);
    	addProperty(COMPLEXITY, 2, "Molecular Complexity", "Molecular Complexity (low < 0.5 < high)");
    	addProperty(HEAVY_ATOMS, 2, "Non-H Atoms", "Non-Hydrogen Atom Count");
    	addProperty(NONCARBON_ATOMS, 2, "Non-C/H Atoms", "Non-Carbon/Hydrogen Atom Count");
    	addProperty(METAL_ATOMS, 2, "Metal-Atoms", "Metal-Atom Count");
    	addProperty(NEGATIVE_ATOMS, 2, "Electronegative Atoms", "Electronegative Atom Count (N, O, P, S, F, Cl, Br, I, As, Se)");
    	addProperty(STEREOCENTERS, 2, "Stereo Centers", "Stereo Center Count");
    	addProperty(ROTATABLE_BONDS, 2, "Rotatable Bonds", "Rotatable Bond Count");
    	addProperty(RINGS, 2, "Rings", "Ring Count");
    	addProperty(AROMATIC_RINGS, 2, "Aromatic Rings", "Aromatic Ring Count");
    	addProperty(SP3_ATOMS, 2, "sp3-Atoms", "sp3-Atom Count");
    	addProperty(SYMMETRIC_ATOMS, 2, "Symmetric atoms", "Symmetric Atom Count");

    	addProperty(ALL_AMIDES, 3, "Amides", "Amide Nitrogen Count (includes imides and sulfonamides)");
    	addProperty(ALL_AMINES, 3, "Amines", "Amine Count (excludes enamines, aminales, etc.)");
    	addProperty(ALKYL_AMINES, 3, "Alkyl-Amines", "Alkyl-Amine Count (excludes Aryl-,Alkyl-Amines)");
    	addProperty(ARYL_AMINES, 3, "Aromatic Amines", "Aryl-Amine Count (includes Aryl-,Alkyl-Amines)");
    	addProperty(AROMATIC_NITROGEN, 3, "Aromatic Nitrogens", "Aromatic Nitrogen Atom Count");
    	addProperty(BASIC_NITROGEN, 3, "Basic Nitrogens", "Basic Nitrogen Atom Count (rough estimate: pKa above 7)");
    	addProperty(ACIDIC_OXYGEN, 3, "Acidic Oxygens", "Acidic Oxygen Atom Count (rough estimate: pKa below 7)");

    	addBackgroundColor(MUTAGENIC, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
    	addBackgroundColor(TUMORIGENIC, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
    	addBackgroundColor(REPRODUCTIVE_EFECTIVE, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
    	addBackgroundColor(IRRITANT, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
		}

	private void addProperty(int type, int tab, String columnTitle, String description) {
		addProperty(type, tab, columnTitle, description, null, null, 0);
		}

	private void addProperty(int type, int tab, String columnTitle, String description, String dependentColumnFilter,
							 String descriptorName, int predictorFlags) {
		DEProperty property = new DEProperty(type, tab, columnTitle, description, dependentColumnFilter,
											 descriptorName, predictorFlags);
		mPropertyTable[mPropertyMap.size()] = property;
		mPropertyMap.put(PROPERTY_CODE[type], property);
		}

	private void addBackgroundColor(int type, int colorMode, Color[] colorList) {
		mPropertyMap.get(PROPERTY_CODE[type]).backgroundColor = new DEBackgroundColor(colorMode, colorList);
		}

	@Override
	public void runTask(Properties configuration) {
		mIDCodeColumn = selectStructureColumn(configuration);

		String value = configuration.getProperty(PROPERTY_CHEMPROPERTY_LIST);
		if (value == null) {
			showErrorMessage("Property list missing.");
			return;
			}

		String[] codeList = value.split(CHEMPROPERTY_LIST_SEPARATOR_REGEX);
		mPropertyOrderList = new ArrayList<DEPropertyOrder>();
		for (String code:codeList)
			addPropertyOrderIfValid(code);

		if (mPropertyOrderList.size() == 0)
			return;

		String[] columnName = new String[mPropertyOrderList.size()];
		int column = 0;
		mPredictor = new Object[PREDICTOR_COUNT];
		for (DEPropertyOrder order:mPropertyOrderList) {
			columnName[column++] = order.getColumnTitle();
			ensurePredictor(order.property.predictorFlags);
			}

        boolean fragFpNeeded = false;
        boolean pp3DNeeded = false;
		for (DEPropertyOrder order:mPropertyOrderList) {
            if (order.property.descriptorName != null) {
                if (order.property.descriptorName.equals(DescriptorConstants.DESCRIPTOR_FFP512.shortName))
                    fragFpNeeded = true;
                if (order.property.descriptorName.equals(DescriptorConstants.DESCRIPTOR_Flexophore.shortName))
                    pp3DNeeded = true;
                }
            }

        mFragFpColumn = -1;
        if (fragFpNeeded) {
            mFragFpColumn = mTableModel.getChildColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
            if (mFragFpColumn == -1)
            	mFragFpColumn = mTableModel.createDescriptorColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);

            waitForDescriptor(mTableModel, mFragFpColumn);
    		if (threadMustDie())
    			return;
			}
        mFlexophoreColumn = -1;
        if (pp3DNeeded) {
            mFlexophoreColumn = mTableModel.getChildColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_Flexophore.shortName);
            if (mFlexophoreColumn == -1)
            	mFlexophoreColumn = mTableModel.createDescriptorColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_Flexophore.shortName);

            waitForDescriptor(mTableModel, mFlexophoreColumn);
    		if (threadMustDie())
    			return;
            }

		if (threadMustDie())
			return;

		final int firstNewColumn = mTableModel.addNewColumns(columnName);

        startProgress("Calculating properties...", 0, mTableModel.getTotalRowCount());

        if (mPredictor[PREDICTOR_PKA] == null)
        	finishTaskMultiCore(firstNewColumn);
        else
        	finishTaskSingleCore(firstNewColumn);
		}

	private void finishTaskSingleCore(final int firstNewColumn) {
		int errorCount = 0;

		StereoMolecule containerMol = new StereoMolecule();
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
	    	if ((row % 16) == 15)
	    		updateProgress(row);

	    	try {
	    		processRow(row, firstNewColumn, containerMol);
	    		}
	    	catch (Exception e) {
	    		errorCount++;
	    		}
			}

		if (!threadMustDie() && errorCount != 0)
			showErrorMessage("The task '"+TASK_NAME+"' failed on "+errorCount+" molecules.");

		finalizeTableModel(firstNewColumn);
		}

	private void finishTaskMultiCore(final int firstNewColumn) {
    	int threadCount = Runtime.getRuntime().availableProcessors();
    	mSMPRecordIndex = new AtomicInteger(mTableModel.getTotalRowCount());
    	mSMPWorkingThreads = new AtomicInteger(threadCount);
    	mSMPErrorCount = new AtomicInteger(0);

    	Thread[] t = new Thread[threadCount];
    	for (int i=0; i<threadCount; i++) {
    		t[i] = new Thread("Chemical Property Calculator "+(i+1)) {
    			public void run() {
    				StereoMolecule containerMol = new StereoMolecule();
    				int recordIndex = mSMPRecordIndex.decrementAndGet();
    				while (recordIndex >= 0 && !threadMustDie()) {
    					try {
    				    	processRow(recordIndex, firstNewColumn, containerMol);
    						}
    					catch (Exception e) {
    						mSMPErrorCount.incrementAndGet();
    						e.printStackTrace();
    						}

    					updateProgress(-1);
    					recordIndex = mSMPRecordIndex.decrementAndGet();
    					}

    				if (mSMPWorkingThreads.decrementAndGet() == 0) {
						if (!threadMustDie() && mSMPErrorCount.get() != 0)
							showErrorMessage("The task '"+TASK_NAME+"' failed on "+mSMPErrorCount.get()+" molecules.");

   			            finalizeTableModel(firstNewColumn);
    					}
    				}
    			};
    		t[i].setPriority(Thread.MIN_PRIORITY);
    		t[i].start();
    		}

    	// the controller thread must wait until all others are finished
    	// before the next task can begin or the dialog is closed
    	for (int i=0; i<threadCount; i++)
    		try { t[i].join(); } catch (InterruptedException e) {}
		}

	private void finalizeTableModel(int firstNewColumn) {
		mTableModel.finalizeNewColumns(firstNewColumn, this);

		DETableView tableView = mParentFrame.getMainFrame().getMainPane().getTableView();
		if (tableView != null) {
			int column = firstNewColumn;
			for (DEPropertyOrder order:mPropertyOrderList) {
				if (order.property.backgroundColor != null) {
					VisualizationColor vc = tableView.getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND);
					vc.setColor(column, order.property.backgroundColor.colorList, order.property.backgroundColor.colorMode);
					}
				column++;
				}
			}
		}

	private void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		StereoMolecule mol = mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), mIDCodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, containerMol);
		if (mol == null)
			return;

		chemaxon.struc.Molecule camol = null;
		double totalWeight = -1;
		if (mol.getAllAtoms() != 0) {
			totalWeight = new MolecularFormula(mol).getRelativeWeight();
			mol.stripSmallFragments();
			if (mPredictor[PREDICTOR_PKA] != null)
				camol = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).convert(mol);
			}

		int currentColumn = firstNewColumn;
		for (DEPropertyOrder order:mPropertyOrderList) {
	    	int count = 0;
			if (mol.getAllAtoms() == 0) {
				mTableModel.setTotalValueAt("", row, currentColumn);
				}
			else {
				String value = null;
				switch (order.property.type) {
				case TOTAL_WEIGHT:
					value = DoubleFormat.toString(totalWeight, 6);
					break;
				case FRAGMENT_WEIGHT:
					value = DoubleFormat.toString(new MolecularFormula(mol).getRelativeWeight(), 6);
					break;
				case FRAGMENT_ABS_WEIGHT:
					value = DoubleFormat.toString(new MolecularFormula(mol).getAbsoluteWeight(), 9);
					break;
				case LOGP:
					try {
						value = DoubleFormat.toString(((CLogPPredictor)mPredictor[PREDICTOR_LOGP]).assessCLogP(mol));
						}
					catch (Exception e) {
						value = e.toString();
						}
					break;
				case LOGS:
					value = DoubleFormat.toString(((SolubilityPredictor)mPredictor[PREDICTOR_LOGS]).assessSolubility(mol));
					break;
				case LOGD:
					if (camol == null) {
						value = "molecule conversion error";
						}
					else {
						final double LOGD_PH = 7.4f;
						try {
							double logP = (float)((CLogPPredictor)mPredictor[PREDICTOR_LOGP]).assessCLogP(mol);
							double aPKa = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostAcidicPKa(camol);
							double bPKa = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostBasicPKa(camol);
							double logD = (Double.isNaN(aPKa) && Double.isNaN(bPKa)) ? logP
									   : Double.isNaN(aPKa) ? logP - Math.log10(1.0 + Math.pow(10, bPKa-LOGD_PH))
									   : Double.isNaN(bPKa) ? logP - Math.log10(1.0 + Math.pow(10, LOGD_PH-aPKa))
									   : (LOGD_PH-aPKa > bPKa-LOGD_PH) ?
											   logP - Math.log10(1.0 + Math.pow(10, LOGD_PH-aPKa))
											 : logP - Math.log10(1.0 + Math.pow(10, bPKa-LOGD_PH));
							value = DoubleFormat.toString(logD);
							}
						catch (Exception e) {
							value = e.toString();
							}
						}
					break;
				case ACIDIC_PKA:
					if (camol == null) {
						value = "molecule conversion error";
						}
					else {
						double pKa = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostAcidicPKa(camol);
						value = Double.isNaN(pKa) ? "" : DoubleFormat.toString(pKa);
						}
					break;
				case BASIC_PKA:
					if (camol == null) {
						value = "molecule conversion error";
						}
					else {
						double pKa = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostBasicPKa(camol);
						value = Double.isNaN(pKa) ? "" : DoubleFormat.toString(pKa);
						}
					break;
				case ACCEPTORS:
					for (int atom=0; atom<mol.getAllAtoms(); atom++)
						if (mol.getAtomicNo(atom) == 7 || mol.getAtomicNo(atom) == 8)
							count++;
					value = ""+count;
					break;
				case DONORS:
					for (int atom=0; atom<mol.getAllAtoms(); atom++)
						if ((mol.getAtomicNo(atom) == 7 || mol.getAtomicNo(atom) == 8)
						 && mol.getAllHydrogens(atom) > 0)
							count++;
					value = ""+count;
					break;
				case PSA:
					value = DoubleFormat.toString(((PolarSurfaceAreaPredictor)mPredictor[PREDICTOR_PSA]).assessPSA(mol));
					break;
				case DRUGLIKENESS:
					value = DoubleFormat.toString(((DruglikenessPredictorWithIndex)mPredictor[PREDICTOR_DRUGLIKENESS]).assessDruglikeness(mol,
                            (int[])mTableModel.getTotalRecord(row).getData(mFragFpColumn), this));
					break;
				case LE:	// dG / HA
	    			// dG = -RT*ln(Kd) with R=1.986cal/(K*mol); T=300K; dG in kcal/mol
	    			// We use IC50 instead of Kd, which is acceptable according to
	    			// Andrew L. Hopkins, Colin R. Groom, Alexander Alex
	    			// Drug Discovery Today, Volume 9, Issue 10, 15 May 2004, Pages 430-431
					if (order.dependentColumn != -1) {
						double ic50 = mTableModel.getTotalOriginalDoubleAt(row, order.dependentColumn);
						if (!Double.isNaN(ic50)) {
							double le = - 1.986 * 0.300 * Math.log(0.000000001 * ic50) / mol.getAtoms();
							value = DoubleFormat.toString(le);
							}
						}
					break;
/*							case SE:	// dG / molecule surface
		    			// dG = -RT*ln(Kd) with R=1.986cal/(K*mol); T=300K; dG in kcal/mol
		    			// We use IC50 instead of Kd, which is acceptable according to
		    			// Andrew L. Hopkins, Colin R. Groom, Alexander Alex
		    			// Drug Discovery Today, Volume 9, Issue 10, 15 May 2004, Pages 430-431
						// surface = sMethane * pow(nAtoms, 2/3); surface grows less quickly than volume!
						if (property.dependentColumn != -1) {
							double ic50 = mTableModel.getTotalOriginalDoubleAt(row, property.dependentColumn);
							if (!Double.isNaN(ic50)) {
								double dG = - 1.986 * 0.300 * Math.log(0.000000001 * ic50);
								double se = dG / (4.43 * Math.pow(mol.getAtoms(), 0.6666666667));
								value = DoubleFormat.toString(se);
								}
							}
						break;	*/
				case LLE:	// pIC50 - logP
					if (order.dependentColumn != -1) {
						double ic50 = mTableModel.getTotalOriginalDoubleAt(row, order.dependentColumn);
						if (!Double.isNaN(ic50)) {
							double pic50 = - Math.log10(0.000000001 * ic50);
							try {
								value = DoubleFormat.toString(pic50 - ((CLogPPredictor)mPredictor[PREDICTOR_LOGP]).assessCLogP(mol));
								}
							catch (Exception e) {
								value = e.toString();
								}
							}
						}
					break;
				case LELP:	// logP / LE
					if (order.dependentColumn != -1) {
						double ic50 = mTableModel.getTotalOriginalDoubleAt(row, order.dependentColumn);
						if (!Double.isNaN(ic50)) {
							double le = - 1.986 * 0.300 * Math.log(0.000000001 * ic50) / mol.getAtoms();
							try {
								value = DoubleFormat.toString(((CLogPPredictor)mPredictor[PREDICTOR_LOGP]).assessCLogP(mol) / le);
								}
							catch (Exception e) {
								value = e.toString();
								}
							}
						}
					break;
				case MUTAGENIC:
				    value = ToxicityPredictor.RISK_NAME[((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeMutagenic, this)];
				    break;
				case TUMORIGENIC:
				    value = ToxicityPredictor.RISK_NAME[((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeTumorigenic, this)];
				    break;
				case REPRODUCTIVE_EFECTIVE:
				    value = ToxicityPredictor.RISK_NAME[((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeReproductiveEffective, this)];
				    break;
				case IRRITANT:
				    value = ToxicityPredictor.RISK_NAME[((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeIrritant, this)];
				    break;
/*                          case HERG_RISK:
                        value = ((RiskOf_hERGActPredictor)predictor[PREDICTOR_HERG]).assess_hERGRisk(mol, mProgressDialog);
                        break;*/
				case NASTY_FUNCTIONS:
					value = ((NastyFunctionDetector)mPredictor[PREDICTOR_NASTY_FUNCTIONS]).getNastyFunctionString(mol,
							(int[])mTableModel.getTotalRecord(row).getData(mFragFpColumn));
					break;
				case SHAPE:
					value = DoubleFormat.toString(assessMolecularShape(mol));
					break;
				case FLEXIBILITY:
					value = DoubleFormat.toString(((MolecularFlexibilityCalculator)mPredictor[PREDICTOR_FLEXIBILITY]).calculateMolecularFlexibility(mol));
					break;
				case COMPLEXITY:
					value = DoubleFormat.toString(assessMolecularComplexity(mol));
					break;
				case HEAVY_ATOMS:
					value = ""+mol.getAtoms();
					break;
				case NONCARBON_ATOMS:
					mol.ensureHelperArrays(Molecule.cHelperNeighbours);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (mol.getAtomicNo(atom) != 6)
							count++;
					value = ""+count;
					break;
				case METAL_ATOMS:
					mol.ensureHelperArrays(Molecule.cHelperNeighbours);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (mol.isMetalAtom(atom))
							count++;
					value = ""+count;
					break;
				case NEGATIVE_ATOMS:
					mol.ensureHelperArrays(Molecule.cHelperNeighbours);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (mol.isElectronegative(atom))
							count++;
					value = ""+count;
					break;
				case STEREOCENTERS:
					value = ""+mol.getStereoCenterCount();
				    break;
				case ROTATABLE_BONDS:
					value = ""+mol.getRotatableBondCount();
					break;
				case RINGS:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					value = ""+mol.getRingSet().getSize();
					break;
				case AROMATIC_RINGS:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					RingCollection rc = mol.getRingSet();
					for (int i=0; i<rc.getSize(); i++)
						if (rc.isAromatic(i))
							count++;
					value = ""+count;
					break;
				case SP3_ATOMS:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if ((mol.getAtomicNo(atom) == 6 && mol.getAtomPi(atom) == 0)
						 || (mol.getAtomicNo(atom) == 7 && !mol.isFlatNitrogen(atom))
						 || (mol.getAtomicNo(atom) == 8 && mol.getAtomPi(atom) == 0 && !mol.isAromaticAtom(atom))
						 || (mol.getAtomicNo(atom) == 15)
						 || (mol.getAtomicNo(atom) == 16 && !mol.isAromaticAtom(atom)))
							count++;
					value = ""+count;
					break;
				case SYMMETRIC_ATOMS:
					mol.ensureHelperArrays(Molecule.cHelperSymmetrySimple);
					int maxRank = 0;
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (maxRank < mol.getSymmetryRank(atom))
							maxRank = mol.getSymmetryRank(atom);
					value = ""+(mol.getAtoms()-maxRank);
					break;
				case ALL_AMIDES:
					mol.ensureHelperArrays(Molecule.cHelperNeighbours);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (AtomFunctionAnalyzer.isAmide(mol, atom))
							count++;
					value = ""+count;
					break;
				case ALL_AMINES:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (AtomFunctionAnalyzer.isAmine(mol, atom))
							count++;
					value = ""+count;
					break;
				case ALKYL_AMINES:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (AtomFunctionAnalyzer.isAlkylAmine(mol, atom))
							count++;
					value = ""+count;
					break;
				case ARYL_AMINES:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (AtomFunctionAnalyzer.isArylAmine(mol, atom))
							count++;
					value = ""+count;
					break;
				case AROMATIC_NITROGEN:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (mol.getAtomicNo(atom) == 7 && mol.isAromaticAtom(atom))
							count++;

					value = ""+count;
					break;
				case BASIC_NITROGEN:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (AtomFunctionAnalyzer.isBasicNitrogen(mol, atom))
							count++;

					value = ""+count;
					break;
				case ACIDIC_OXYGEN:
					mol.ensureHelperArrays(Molecule.cHelperRings);
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (AtomFunctionAnalyzer.isAcidicOxygen(mol, atom))
							count++;

					value = ""+count;
					break;
					}

				mTableModel.setTotalValueAt(value, row, currentColumn);
				}
			currentColumn++;
			}
		}

	/**
	 * Returns the number of bonds of the shortest path between
	 * those two atoms with the largest topological distance.
	 * @param mol
	 * @return
	 */
	private double assessMolecularShape(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperRings);
		if (mol.getAtoms() == 0)
			return -1;
		if (mol.getBonds() == 0)
			return 0;

		int maxLength = 0;
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (mol.getConnAtoms(atom) == 1 || mol.isRingAtom(atom))
				maxLength = Math.max(maxLength, findHighestAtomDistance(mol, atom));

		return (double)(maxLength+1) / (double)mol.getAtoms();
		}

    public double assessMolecularComplexity(StereoMolecule mol) {
    	final int MAX_BOND_COUNT = 7;
    	int bondCount = Math.min(mol.getBonds()/2, MAX_BOND_COUNT);

    	mol.ensureHelperArrays(Molecule.cHelperRings);
        StereoMolecule fragment = new StereoMolecule(mol.getAtoms(), mol.getBonds());
        TreeSet<String> fragmentSet = new TreeSet<String>();
        int[] atomMap = new int[mol.getAllAtoms()];

        boolean[][] bondsTouch = new boolean[mol.getBonds()][mol.getBonds()];
        for (int atom=0; atom<mol.getAtoms(); atom++) {
        	for (int i=1; i<mol.getConnAtoms(atom); i++) {
            	for (int j=0; j<i; j++) {
            		int bond1 = mol.getConnBond(atom, i);
            		int bond2 = mol.getConnBond(atom, j);
            		bondsTouch[bond1][bond2] = true;
            		bondsTouch[bond2][bond1] = true;
            		}
        		}
        	}

        boolean[] bondIsMember = new boolean[mol.getBonds()];
        int maxLevel = bondCount - 2;
        int[] levelBond = new int[maxLevel+1];
    	for (int rootBond=0; rootBond<mol.getBonds(); rootBond++) {
    		bondIsMember[rootBond] = true;
    		int level = 0;
    		levelBond[0] = rootBond;
    		while (true) {
				boolean levelBondFound = false;
    			while (!levelBondFound && levelBond[level] < mol.getBonds()-1) {
    				levelBond[level]++;
    				if (!bondIsMember[levelBond[level]]) {
	    				for (int bond=rootBond; bond<mol.getBonds(); bond++) {
	    					if (bondIsMember[bond] && bondsTouch[bond][levelBond[level]]) {
	    						levelBondFound = true;
	    						break;
	    						}
	    					}
    					}
    				}

    			if (levelBondFound) {
    				bondIsMember[levelBond[level]] = true;
    				if (level == maxLevel) {
    				    mol.copyMoleculeByBonds(fragment, bondIsMember, true, atomMap);
    				    fragmentSet.add(new Canonizer(fragment).getIDCode());
        				bondIsMember[levelBond[level]] = false;
    					}
    				else {
    					level++;
    					levelBond[level] = rootBond;
    					}
    				}
    			else {
					if (--level < 0)
						break;
					bondIsMember[levelBond[level]] = false;
    				}
    			}
    		}

        return Math.log(fragmentSet.size()) / bondCount;
        }

	/**
	 * Calculates the topological distance to the topologically most remote atom.
	 * @param mol
	 * @param startAtom
	 * @return number of bonds from startAtom to remote atom
	 */
	private int findHighestAtomDistance(StereoMolecule mol, int startAtom) {
		int[] graphLevel = new int[mol.getAtoms()];
        int[] graphAtom = new int[mol.getAtoms()];

        graphAtom[0] = startAtom;
        graphLevel[startAtom] = 1;

        int current = 0;
        int highest = 0;
        while (current <= highest /* && graphLevel[current] <= maxLength */) {
            int parent = graphAtom[current];
            for (int i=0; i<mol.getConnAtoms(parent); i++) {
                int candidate = mol.getConnAtom(parent, i);
                if (graphLevel[candidate] == 0) {
                    graphAtom[++highest] = candidate;
                    graphLevel[candidate] = graphLevel[parent]+1;
                    }
                }
            current++;
            }
        return graphLevel[graphAtom[highest]] - 1;
		}

	private class PKaPredictor {
		private pKaPlugin plugin;

		public PKaPredictor() {
			plugin = new pKaPlugin();

			// set parameters
			plugin.setMaxIons(6);
			plugin.setBasicpKaLowerLimit(0.0);
			plugin.setAcidicpKaUpperLimit(14.0);
			plugin.setpHLower(3.0); // for ms distr
			plugin.setpHUpper(6.0); // for ms distr
			plugin.setpHStep(1.0);  // for ms distr
			}

		public double getMostBasicPKa(chemaxon.struc.Molecule mol) {
			double[] basicpKa = new double[3];
			int[] basicIndexes = new int[3];

			try {
				plugin.setMolecule(mol);
				plugin.run();
				plugin.getMacropKaValues(pKaPlugin.BASIC, basicpKa, basicIndexes);
				}
			catch (PluginException pe) {
				System.out.println("PluginException:"+pe.toString());
				}
			catch (Exception e) {
				System.out.println("Unexpected Exception:"+e.toString());
				}

			return basicpKa[0];
			}

		public double getMostAcidicPKa(chemaxon.struc.Molecule mol) {
			double[] acidicpKa = new double[3];
			int[] acidicIndexes = new int[3];
			try {
				plugin.setMolecule(mol);
				plugin.run();
				plugin.getMacropKaValues(pKaPlugin.ACIDIC, acidicpKa, acidicIndexes);
				}
			catch (PluginException pe) {
				System.out.println("PluginException:"+pe.toString());
				}
			catch (Exception e) {
				System.out.println("Unexpected Exception:"+e.toString());
				}

			return acidicpKa[0];
			}

/*		private StereoMolecule convert(chemaxon.struc.Molecule chemAxonMol) {
			try {
				return new MolfileParser().getCompactMolecule(MolExporter.exportToFormat(chemAxonMol, "mol"));
				}
			catch (IOException ioe) {
				ioe.printStackTrace();
				return null;
				}
			}*/

		private chemaxon.struc.Molecule convert(StereoMolecule actelionMol) {
			String molfile = null;
			try {
				molfile = new MolfileCreator(actelionMol).getMolfile();
				return MolImporter.importMol(molfile, "mol");
				}
			catch (MolFormatException ioe) {
				ioe.printStackTrace();
				return null;
				}
			}
		}

	private class DEProperty {
		public final String columnTitle;
		public final String description;
		public final String descriptorName;
		public final String dependentColumnFilter;	// if not null, e.g. 'ic50', serves as substring to prioritize numerical columns for selection, lower case!!!
		public final int predictorFlags,tab,type;
		public DEBackgroundColor backgroundColor;
	
		/**
		 * @param columnTitle
		 * @param description
		 * @param dependentColumnFilter
		 * @param descriptorName
		 * @param predictor
		 */
		public DEProperty(int type, int tab, String columnTitle, String description, String dependentColumnFilter,
						  String descriptorName, int predictorFlags) {
			this.type = type;
			this.tab = tab;
			this.columnTitle = columnTitle;
			this.description = description;
			this.dependentColumnFilter = dependentColumnFilter;
			this.descriptorName = descriptorName;
			this.predictorFlags = predictorFlags;
			}
		}

	private class DEBackgroundColor {
		int colorMode;
		Color[] colorList;

		public DEBackgroundColor(int colorMode, Color[] colorList) {
			this.colorMode = colorMode;
			this.colorList = colorList;
			}
		}

	private class DEPropertyOrder {
		DEProperty property;
		int dependentColumn;

		public DEPropertyOrder(DEProperty property, int dependentColumn) {
			this.property = property;
			this.dependentColumn = dependentColumn;
			}

		public String getColumnTitle() {
			return (dependentColumn == -1) ? property.columnTitle
					: property.columnTitle + " from " + mTableModel.getColumnTitle(dependentColumn);
			}
		}

	private class DEPropertyGUI implements ActionListener {
		private JCheckBox mCheckBox;
		private JComboBox mComboBox;
		private DEProperty mProperty;

		/**
		 * @param columnTitle
		 * @param description
		 * @param dependentColumnFilter
		 * @param descriptorName
		 * @param predictor
		 */
		public DEPropertyGUI(DEProperty property) {
			mProperty = property;

			mCheckBox = new JCheckBox(property.description);
			mCheckBox.addActionListener(this);
			if (property.dependentColumnFilter != null) {
				mComboBox = new JComboBox();
		        for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
		            if (mTableModel.isColumnTypeDouble(i) && mTableModel.getColumnTitle(i).toLowerCase().contains(property.dependentColumnFilter))
		            	mComboBox.addItem(mTableModel.getColumnTitle(i));
		        for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
		            if (mTableModel.isColumnTypeDouble(i) && !mTableModel.getColumnTitle(i).toLowerCase().contains(property.dependentColumnFilter))
		            	mComboBox.addItem(mTableModel.getColumnTitle(i));

				if (mComboBox.getItemCount() == 0)
					mCheckBox.setEnabled(false);
				}

			// Check, whether the ChemAxon classes are available
			if (property.type == LOGD
			 || property.type == ACIDIC_PKA
			 || property.type == BASIC_PKA) {
				try {
					Class.forName("chemaxon.marvin.calculations.pKaPlugin");
					}
				catch (ClassNotFoundException cnfe) {
					mCheckBox.setEnabled(false);
					}
//			 && !mParentFrame.getApplication().isActelion()
//			 && !new File(DataWarrior.getApplicationFolder()+File.separator+"capka.jar").exists())
				}
			}

		public JCheckBox getCheckBox() {
			return mCheckBox;
			}

		public JComboBox getComboBox() {
			return mComboBox;
			}

		public void actionPerformed(ActionEvent e) {
			if (mProperty.descriptorName != null) {
				int structureColumn = mTableModel.findColumn((String)mComboBoxStructureColumn.getSelectedItem());
			    if (mTableModel.getChildColumn(structureColumn, mProperty.descriptorName) == -1) {
		            JOptionPane.showMessageDialog(getParentFrame(), "Calculating '" + mProperty.columnTitle + "' requires the '" + mProperty.descriptorName
		            										  + "' descriptor, which is not available.");
		            ((JCheckBox)e.getSource()).setSelected(false);
					}
				}
	        }
		}
	}