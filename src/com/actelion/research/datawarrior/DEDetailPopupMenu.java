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

package com.actelion.research.datawarrior;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.SmilesCreator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.task.view.DETaskSeparateCases;
import com.actelion.research.datawarrior.task.view.DETaskSetBackgroundImage;
import com.actelion.research.datawarrior.task.view.DETaskSetConnectionLines;
import com.actelion.research.datawarrior.task.view.DETaskSetFocus;
import com.actelion.research.datawarrior.task.view.DETaskSetGeneralViewProperties;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerBackgroundColor;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerColor;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerJittering;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerShape;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerSize;
import com.actelion.research.datawarrior.task.view.DETaskSetMarkerTransparency;
import com.actelion.research.datawarrior.task.view.DETaskSetMultiValueMarker;
import com.actelion.research.datawarrior.task.view.DETaskSetPreferredChartType;
import com.actelion.research.datawarrior.task.view.DETaskSetStatisticalViewOptions;
import com.actelion.research.datawarrior.task.view.DETaskShowLabels;
import com.actelion.research.datawarrior.task.view.DETaskSplitView;
import com.actelion.research.gui.JDrawDialog;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.filter.JMultiStructureFilterPanel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.FocusableView;
import com.actelion.research.table.view.JStructureGrid;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization3D;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.table.view.VisualizationPanel3D;
import com.actelion.research.util.BrowserControl;

public class DEDetailPopupMenu extends JPopupMenu implements ActionListener,ItemListener {
	private static final long serialVersionUID = 0x20060904;

	private static final String TEXT_STRUCTURE_LABELS = "Show/Hide/Size Labels...";
	private static final String TEXT_GENERAL_OPTIONS = "Set General View Options...";
	private static final String TEXT_STATISTICAL_OPTIONS = DETaskSetStatisticalViewOptions.TASK_NAME+"...";
	private static final String TEXT_CHART_TYPE = DETaskSetPreferredChartType.TASK_NAME+"...";
	private static final String TEXT_SPLIT_VIEW = DETaskSplitView.TASK_NAME+"...";
	private static final String TEXT_MARKER_SIZE = DETaskSetMarkerSize.TASK_NAME+"...";
	private static final String TEXT_MARKER_SHAPE = DETaskSetMarkerShape.TASK_NAME+"...";
	private static final String TEXT_MARKER_COLOR = DETaskSetMarkerColor.TASK_NAME+"...";
	private static final String TEXT_MARKER_BG_COLOR = DETaskSetMarkerBackgroundColor.TASK_NAME+"...";
	private static final String TEXT_MARKER_LABELS = "Set Marker Labels...";
	private static final String TEXT_MARKER_CONNECTION = DETaskSetConnectionLines.TASK_NAME+"...";
	private static final String TEXT_MARKER_JITTERING = DETaskSetMarkerJittering.TASK_NAME+"...";
	private static final String TEXT_MARKER_TRANSPARENCY = DETaskSetMarkerTransparency.TASK_NAME+"...";
	private static final String TEXT_MULTI_VALUE_MARKER = DETaskSetMultiValueMarker.TASK_NAME+"...";
	private static final String TEXT_SEPARATE_CASES = DETaskSeparateCases.TASK_NAME+"...";
	private static final String TEXT_FOCUS = DETaskSetFocus.TASK_NAME+"...";
	private static final String TEXT_BACKGROUND_IMAGE = DETaskSetBackgroundImage.TASK_NAME+"...";

	private static final String DELIMITER = "@";
	private static final String COPY_STRUCTURE = "structure" + DELIMITER;
	private static final String COPY_IDCODE = "idcode" + DELIMITER;
	private static final String COPY_SMILES = "smiles" + DELIMITER;
	private static final String COPY_VALUE = "value" + DELIMITER;
	protected static final String EDIT_VALUE = "edit" + DELIMITER;
	private static final String ADD_TO_LIST = "add" + DELIMITER;
	private static final String REMOVE_FROM_LIST = "remove" + DELIMITER;
	private static final String LOOKUP = "lookup" + DELIMITER;
//	private static final String CONFORMERS = "Explore conformers of '";
	private static final String NEW_FILTER = "filter";
	private static final String NEW_FILTER_THIS = "filterT" + DELIMITER;
	private static final String NEW_FILTER_SELECTED = "filterS" + DELIMITER;
	private static final String NEW_FILTER_VISIBLE = "filterV" + DELIMITER;

	private DEMainPane			mMainPane;
	private CompoundTableModel	mTableModel;
	private CompoundRecord		mRecord;
	private DEPruningPanel		mPruningPanel;
	private CompoundTableView	mSource;
	private DatabaseActions		mDatabaseActions;

	/**
	 * Creates a context dependent popup menu presenting options for one record.
	 * @param tableModel
	 * @param record
	 * @param pruningPanel
	 * @param source
	 * @param selectedColumn if the source view provides it, otherwise -1
	 */
	public DEDetailPopupMenu(DEMainPane mainPane, CompoundRecord record,
							 DEPruningPanel pruningPanel, CompoundTableView source,
							 DatabaseActions databaseActions, int selectedColumn) {
		super();

		mMainPane = mainPane;
		mTableModel = mainPane.getTableModel();
		mRecord = record;
		mPruningPanel = pruningPanel;
		mSource = source;
		mDatabaseActions = databaseActions;

		if (record != null) {
			ArrayList<String> idcodeColumnList = new ArrayList<String>();
			if (selectedColumn == -1) {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
					if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column)))
						idcodeColumnList.add(mTableModel.getColumnTitle(column));
				}
			else {
				if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(selectedColumn)))
					idcodeColumnList.add(mTableModel.getColumnTitle(selectedColumn));
				}

			if (selectedColumn == -1) {
				JMenu copyMenu = new JMenu("Copy");
				if (idcodeColumnList.size() > 0) {
					for (int i=0; i<idcodeColumnList.size(); i++) {
						JMenu copyStructureMenu = new JMenu(idcodeColumnList.get(i)+" as");
						addSubmenuItem(copyStructureMenu, "Chemical Structure", COPY_STRUCTURE+idcodeColumnList.get(i));
						addSubmenuItem(copyStructureMenu, "ID-Code", COPY_IDCODE+idcodeColumnList.get(i));
						addSubmenuItem(copyStructureMenu, "SMILES Code", COPY_SMILES+idcodeColumnList.get(i));
						copyMenu.add(copyStructureMenu);
						}
					}
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
					if (mTableModel.isColumnDisplayable(column)
					 && mTableModel.getColumnSpecialType(column) == null)
						addSubmenuItem(copyMenu, mTableModel.getColumnTitle(column), COPY_VALUE+mTableModel.getColumnTitle(column));
				add(copyMenu);
				}
			else {
				if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(selectedColumn))) {
					JMenu copyMenu = new JMenu("Copy as");
					addSubmenuItem(copyMenu, "Chemical Structure", COPY_STRUCTURE+mTableModel.getColumnTitle(selectedColumn));
					addSubmenuItem(copyMenu, "ID-Code", COPY_IDCODE+mTableModel.getColumnTitle(selectedColumn));
					addSubmenuItem(copyMenu, "SMILES Code", COPY_SMILES+mTableModel.getColumnTitle(selectedColumn));
					add(copyMenu);
					}
				else if (mTableModel.isColumnDisplayable(selectedColumn)
   					 && mTableModel.getColumnSpecialType(selectedColumn) == null) {
					addMenuItem("Copy", COPY_VALUE+mTableModel.getColumnTitle(selectedColumn));
					}
				}

			addSeparator();
			if (selectedColumn == -1) {
				JMenu editMenu = new JMenu("Edit");
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
					if (mTableModel.isColumnDisplayable(column)) {
						String columnType = mTableModel.getColumnSpecialType(column);
						if (columnType == null
						 || columnType.equals(CompoundTableModel.cColumnTypeIDCode)
						 || columnType.equals(CompoundTableModel.cColumnTypeRXNCode))
							addSubmenuItem(editMenu, mTableModel.getColumnTitle(column), EDIT_VALUE+mTableModel.getColumnTitleNoAlias(column));
						}
				add(editMenu);
				}
			else {
				String columnType = mTableModel.getColumnSpecialType(selectedColumn);
				if (columnType == null
				 || columnType.equals(CompoundTableModel.cColumnTypeIDCode))
					addMenuItem("Edit", EDIT_VALUE+mTableModel.getColumnTitleNoAlias(selectedColumn));
				}

			CompoundTableHitlistHandler hh = mTableModel.getHitlistHandler();
			if (hh.getHitlistCount() != 0) {
				DEScrollableMenu hitlistAddMenu = null;
				DEScrollableMenu hitlistRemoveMenu = null;
				for (int i=0; i<hh.getHitlistCount(); i++) {
					if (record.isFlagSet(hh.getHitlistFlagNo(i))) {
						if (hitlistRemoveMenu == null)
							hitlistRemoveMenu = new DEScrollableMenu("Remove Row From List");
						addSubmenuItem(hitlistRemoveMenu, hh.getHitlistName(i), REMOVE_FROM_LIST+hh.getHitlistName(i));
						}
					else {
						if (hitlistAddMenu == null)
							hitlistAddMenu = new DEScrollableMenu("Add Row To List");
						addSubmenuItem(hitlistAddMenu, hh.getHitlistName(i), ADD_TO_LIST+hh.getHitlistName(i));
						}
					}
				addSeparator();
				if (hitlistAddMenu != null)
					add(hitlistAddMenu);
				if (hitlistRemoveMenu != null)
					add(hitlistRemoveMenu);
				}

			if (idcodeColumnList.size() != 0) {
				addSeparator();
				JMenu filterMenu = new JMenu("New Structure Filter from");
				for (int i=0; i<idcodeColumnList.size(); i++) {
					String columnName = idcodeColumnList.get(i);
					addSubmenuItem(filterMenu, "this "+columnName, NEW_FILTER_THIS+columnName);
					addSubmenuItem(filterMenu, "selected "+columnName+"s", NEW_FILTER_SELECTED+columnName);
					addSubmenuItem(filterMenu, "visible "+columnName+"s", NEW_FILTER_VISIBLE+columnName);
					}
				add(filterMenu);

/*				for (String columnName : idcodeColumnList) {	 taken out because of unclear copyright situation with CCDC concerning torsion statistics
					addMenuItem(CONFORMERS+columnName+"'");
					}	*/
				}

			boolean lookupFound = false;
			try {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					String lookupCount = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupCount);
					if (lookupCount != null && Integer.parseInt(lookupCount) != 0)
						lookupFound = true;
					}
				if (lookupFound) {
					if (getComponentCount() > 0)
						addSeparator();

					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						String lookupCount = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupCount);
						if (lookupCount != null) {
							int count = Integer.parseInt(lookupCount);
							for (int i=0; i<count; i++) {
								String[] key = mTableModel.separateEntries(mTableModel.encodeData(record, column));
								if (key.length != 1 || key[0].length() != 0) {
									String name = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupName+i);
									String url = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyLookupURL+i);
									if (name != null && url != null) {	// just to make sure
										JMenu lookupMenu = new JMenu("Lookup "+name);
										for (int j=0; j<key.length; j++)
											addSubmenuItem(lookupMenu, key[j], LOOKUP+url.replace("%s", key[j]));
										add(lookupMenu);
										}
									}
								}
							}
						}
					}
				}
			catch (NumberFormatException e) {}

			if (idcodeColumnList.size() != 0 && mDatabaseActions != null) {
				if (!lookupFound)
					addSeparator();

				mDatabaseActions.addActionItems(this, mRecord, idcodeColumnList);
				}
			}

		if (source instanceof VisualizationPanel) {
			if (getComponentCount() > 0)
				addSeparator();

			int chartType = ((VisualizationPanel)source).getVisualization().getChartType();

			addMenuItem(TEXT_CHART_TYPE);
			addSeparator();

			addMenuItem(TEXT_GENERAL_OPTIONS);
			if (source instanceof VisualizationPanel2D
			 && chartType != JVisualization.cChartTypeBars
			 && chartType != JVisualization.cChartTypePies)
				addMenuItem(TEXT_STATISTICAL_OPTIONS);

			addSeparator();

			if (chartType != JVisualization.cChartTypeBars)
				addMenuItem(TEXT_MARKER_SIZE);

			if (chartType != JVisualization.cChartTypeBars
			 && chartType != JVisualization.cChartTypePies)
				addMenuItem(TEXT_MARKER_SHAPE);

			addMenuItem(TEXT_MARKER_COLOR);

			if (source instanceof VisualizationPanel2D
			 && chartType != JVisualization.cChartTypeBars)
				addMenuItem(TEXT_MARKER_BG_COLOR);

			addSeparator();

			if (chartType != JVisualization.cChartTypeBars
			 && chartType != JVisualization.cChartTypePies) {
				addMenuItem(TEXT_MARKER_LABELS);
				addMenuItem(TEXT_MARKER_CONNECTION);
				addMenuItem(TEXT_MARKER_JITTERING);
				}

			if (source instanceof VisualizationPanel2D) {
				addMenuItem(TEXT_MARKER_TRANSPARENCY);
				}

			addSeparator();
			addMenuItem(TEXT_SEPARATE_CASES);
			if (source instanceof VisualizationPanel2D) {
				addMenuItem(TEXT_SPLIT_VIEW);

				if (chartType != JVisualization.cChartTypeBars
				 && chartType != JVisualization.cChartTypePies)
					addMenuItem(TEXT_MULTI_VALUE_MARKER);

				addSeparator();
				addMenuItem(TEXT_BACKGROUND_IMAGE);
				}
			}
		else if (source instanceof JStructureGrid) {
			if (getComponentCount() > 0)
				addSeparator();

			addMenuItem(TEXT_STRUCTURE_LABELS);
			}

		if (source instanceof FocusableView) {
			if (getComponentCount() > 0)
				addSeparator();
			addMenuItem(TEXT_FOCUS);
			}

		if (source instanceof VisualizationPanel3D) {
			JVisualization3D visualization3D = (JVisualization3D)((VisualizationPanel3D)source).getVisualization();

			addSeparator();

			JCheckBoxMenuItem itemIsStereo = new JCheckBoxMenuItem("Use Stereo", visualization3D.isStereo());
			itemIsStereo.addItemListener(this);
			add(itemIsStereo);

			JMenu stereoModeMenu = new JMenu("Stereo Mode");
			JCheckBoxMenuItem item2 = new JCheckBoxMenuItem("H-Interlace Left Eye First",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST);
			item2.addItemListener(this);
			stereoModeMenu.add(item2);

			JCheckBoxMenuItem item3 = new JCheckBoxMenuItem("H-Interlace Right Eye First",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST);
			item3.addItemListener(this);
			stereoModeMenu.add(item3);

			JCheckBoxMenuItem item4 = new JCheckBoxMenuItem("Vertical Interlace",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_V_INTERLACE);
			item4.addItemListener(this);
			stereoModeMenu.add(item4);

			JCheckBoxMenuItem item5 = new JCheckBoxMenuItem("Side By Side For 3D-TV",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_3DTV_SIDE_BY_SIDE);
			item5.addItemListener(this);
			stereoModeMenu.add(item5);

			add(stereoModeMenu);
			}

		if (source instanceof JStructureGrid) {
			if (getComponentCount() > 0)
				addSeparator();
			JMenu columnMenu = new JMenu("Set No Of Columns");
			for (int i=1; i<=8; i++) {
				JMenuItem menuItem = new JMenuItem(""+i);
				menuItem.addActionListener(this);
				columnMenu.add(menuItem);
				}
			for (int i=10; i<=16; i+=2) {
				JMenuItem menuItem = new JMenuItem(""+i);
				menuItem.addActionListener(this);
				columnMenu.add(menuItem);
				}
			add(columnMenu);
			}
		}

	public void itemStateChanged(ItemEvent e) {
		if (((JCheckBoxMenuItem)e.getItem()).getText().equals("Use Stereo")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereo(((JCheckBoxMenuItem)e.getItem()).isSelected());
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("H-Interlace Left Eye First")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST);
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("H-Interlace Right Eye First")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST);
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("Vertical Interlace")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_V_INTERLACE);
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("Side By Side For 3D-TV")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_3DTV_SIDE_BY_SIDE);
			}
		}

	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if (actionCommand.startsWith(COPY_IDCODE)) {
			int idcodeColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			byte[] idcode = (byte[])mRecord.getData(idcodeColumn);
			if (idcode != null) {
				StringSelection theData = new StringSelection(new String(idcode));
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
				}
			}
		else if (actionCommand.startsWith(COPY_SMILES)) {
			String smiles = null;
			for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
				if (mTableModel.getColumnTitle(i).equalsIgnoreCase("smiles")) {
					smiles = mTableModel.getValue(mRecord, i);
					break;
					}
				}

			if (smiles == null) {
				int idcodeColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
				byte[] idcode = (byte[])mRecord.getData(idcodeColumn);
				if (idcode != null) {
					ExtendedMolecule mol = new IDCodeParser().getCompactMolecule(idcode);
					smiles = new SmilesCreator().generateSmiles(mol);
					}
				}

			if (smiles != null) {
				StringSelection theData = new StringSelection(smiles);
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
				}
			}
		else if (actionCommand.startsWith(COPY_STRUCTURE)) {
			int idcodeColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			StereoMolecule mol = mTableModel.getChemicalStructure(mRecord, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
			if (mol != null)
				new ClipboardHandler().copyMolecule(mol);
			}
		else if (actionCommand.startsWith(COPY_VALUE)) {
			int valueColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			byte[] value = (byte[])mRecord.getData(valueColumn);
			if (value != null) {
				StringSelection theData = new StringSelection(new String(value));
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
				}
			}
		else if (actionCommand.startsWith(EDIT_VALUE)) {
			int valueColumn = mTableModel.findColumn(getCommandColumn(actionCommand));
			String columnType = mTableModel.getColumnSpecialType(valueColumn);
			if (columnType == null) {
				byte[] bytes = (byte[])mRecord.getData(valueColumn);
				String oldValue = (bytes == null) ? "" : new String(bytes);

				JTextArea textArea = new JTextArea(oldValue);
				JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				scrollPane.setPreferredSize(new Dimension(320, 120));
				if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(getParentFrame(), scrollPane, "Edit '"+mTableModel.getColumnTitle(valueColumn)+"'", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
					if (mTableModel.isColumnTypeRangeCategory(valueColumn)
					 && !mTableModel.getNativeCategoryList(valueColumn).containsString(textArea.getText())) {
						JOptionPane.showMessageDialog(getParentFrame(), "For columns that contain range categories, you need to type an existing range.");
						}
					else {
						mRecord.setData(textArea.getText().getBytes(), valueColumn, true);
						mTableModel.finalizeChangeCell(mRecord, valueColumn);
						}
					}
				}
			else if (columnType.equals(CompoundTableModel.cColumnTypeIDCode)) {
				StereoMolecule oldMol = mTableModel.getChemicalStructure(mRecord, valueColumn, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, null);
				JDrawDialog dialog = new JDrawDialog(getParentFrame(), oldMol, "Edit '"+mTableModel.getColumnTitle(valueColumn)+"'");
				if (mTableModel.getChildColumn(valueColumn, CompoundTableConstants.cColumnTypeAtomColorInfo) != -1)
					dialog.getDrawArea().setAtomColorSupported(true);
				dialog.setVisible(true);
				if (!dialog.isCancelled()) {
					if (mTableModel.setChemicalStructure(mRecord, dialog.getStructure(), valueColumn))
						mTableModel.finalizeChangeCell(mRecord, valueColumn);
					}
				}
			else if (columnType.equals(CompoundTableModel.cColumnTypeRXNCode)) {
				Reaction oldRxn = mTableModel.getChemicalReaction(mRecord, valueColumn);
				JDrawDialog dialog = new JDrawDialog(getParentFrame(), oldRxn, "Edit '"+mTableModel.getColumnTitle(valueColumn)+"'");
				dialog.setVisible(true);
				if (!dialog.isCancelled()
				 && mTableModel.setChemicalReaction(mRecord, dialog.getReactionAndDrawings(), valueColumn))
					mTableModel.finalizeChangeCell(mRecord, valueColumn);
				}
			}
		else if (actionCommand.startsWith(ADD_TO_LIST)) {
			String hitlistName = getCommandColumn(actionCommand);
			CompoundTableHitlistHandler hh = mTableModel.getHitlistHandler();
			hh.addRecord(mRecord, hh.getHitlistIndex(hitlistName));
			}
		else if (actionCommand.startsWith(REMOVE_FROM_LIST)) {
			String hitlistName = getCommandColumn(actionCommand);
			CompoundTableHitlistHandler hh = mTableModel.getHitlistHandler();
			hh.removeRecord(mRecord, hh.getHitlistIndex(hitlistName));
			}
		else if (actionCommand.startsWith(NEW_FILTER)) {
			String columnName = getCommandColumn(actionCommand);
			int idcodeColumn = mTableModel.findColumn(columnName);
			if (mTableModel.hasDescriptorColumn(idcodeColumn)) {
				if (actionCommand.startsWith(NEW_FILTER_THIS)) {
					StereoMolecule mol = mTableModel.getChemicalStructure(mRecord, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
					if (mol != null) {
						try {
							mPruningPanel.addStructureFilter(mTableModel, idcodeColumn, mol);
							}
						catch (DEPruningPanel.FilterException fpe) {
							JOptionPane.showMessageDialog(getParentFrame(), fpe.getMessage());
							}
						}
					}
				else {
					boolean selected = actionCommand.startsWith(NEW_FILTER_SELECTED);
					try {
						TreeSet<String> idcodeSet = new TreeSet<String>();
						for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
							CompoundRecord record = mTableModel.getTotalRecord(row);
							if (mTableModel.isVisible(record)
							 && (!selected || mTableModel.isVisibleAndSelected(record)))
								idcodeSet.add(mTableModel.getTotalValueAt(row, idcodeColumn));
							}
						int count = idcodeSet.size();
						if (count < 100
						 || JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(getParentFrame(),
								 "Creating a filter with "+count+" structures may take some time.\nDo you want to continue?",
								 "New Structure List Filter", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
							JMultiStructureFilterPanel filter = mPruningPanel.addStructureListFilter(mTableModel, idcodeColumn, false);
							filter.getCompoundCollectionPane().getModel().setCompoundList(idcodeSet);
							}
						}
					catch (DEPruningPanel.FilterException fpe) {
						JOptionPane.showMessageDialog(getParentFrame(), fpe.getMessage());
						}
					}
				}
			else {
				JOptionPane.showMessageDialog(getParentFrame(), "Please calculate a descriptor for the column '"+columnName+"' before creating a structure filter.");
				}
			}
/*		else if (actionCommand.startsWith(CONFORMERS)) {	 taken out because of unclear copyright situation with CCDC concerning torsion statistics
			int index = actionCommand.lastIndexOf('\'');
			final int idcodeColumn = mTableModel.findColumn(actionCommand.substring(CONFORMERS.length(), index));
			StereoMolecule mol = mTableModel.getChemicalStructure(mRecord, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
			if (mol != null)
				new DEConformerDialog(getParentFrame(), mol).generateConformers();
			}*/
		else if (actionCommand.startsWith(LOOKUP)) {
			BrowserControl.displayURL(getCommandColumn(actionCommand));
			}
		else if (actionCommand.equals(TEXT_CHART_TYPE)) {
			new DETaskSetPreferredChartType(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_STRUCTURE_LABELS)
			  || actionCommand.equals(TEXT_MARKER_LABELS)) {
			new DETaskShowLabels(getParentFrame(), mMainPane, mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MARKER_SIZE)) {
			new DETaskSetMarkerSize(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MARKER_SHAPE)) {
			new DETaskSetMarkerShape(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MARKER_COLOR)) {
			new DETaskSetMarkerColor(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MARKER_BG_COLOR)) {
			new DETaskSetMarkerBackgroundColor(getParentFrame(), mMainPane, (VisualizationPanel2D)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MARKER_TRANSPARENCY)) {
			new DETaskSetMarkerTransparency(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MARKER_CONNECTION)) {
			new DETaskSetConnectionLines(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_FOCUS)) {
			new DETaskSetFocus(getParentFrame(), mMainPane, (FocusableView)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MARKER_JITTERING)) {
			new DETaskSetMarkerJittering(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_MULTI_VALUE_MARKER)) {
			new DETaskSetMultiValueMarker(getParentFrame(), mMainPane, (VisualizationPanel2D)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_SEPARATE_CASES)) {
			new DETaskSeparateCases(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();;
			}
		else if (actionCommand.equals(TEXT_SPLIT_VIEW)) {
			new DETaskSplitView(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_BACKGROUND_IMAGE)) {
			new DETaskSetBackgroundImage(getParentFrame(), mMainPane, (VisualizationPanel2D)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_GENERAL_OPTIONS)) {
			new DETaskSetGeneralViewProperties(getParentFrame(), mMainPane, (VisualizationPanel)mSource).defineAndRun();
			}
		else if (actionCommand.equals(TEXT_STATISTICAL_OPTIONS)) {
			new DETaskSetStatisticalViewOptions(getParentFrame(), mMainPane, (VisualizationPanel2D)mSource).defineAndRun();
			}
		else {
			try {
				int columnCount = Integer.parseInt(e.getActionCommand());
				((JStructureGrid)mSource).setColumnCount(columnCount);
				}
			catch (NumberFormatException nfe) {}
			}
		}

	private String getCommandColumn(String actionCommand) {
		return actionCommand.substring(actionCommand.indexOf(DELIMITER)+1);
		}

	private void addSubmenuItem(JMenu menu, String text, String actionCommand) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		if (actionCommand != null)
			item.setActionCommand(actionCommand);
		menu.add(item);
		}

	private void addMenuItem(String text) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		add(item);
		}

	private void addMenuItem(String text, String actionCommand) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		if (actionCommand != null)
			item.setActionCommand(actionCommand);
		add(item);
		}

	// TODO DEFrames are not known in applets. Update this stuff somewhere else to regain compatibility for the DataWarrior applet!
	private DEFrame getParentFrame() {
		Component c = (Component)mSource;
		while (c != null && !(c instanceof DEFrame))
			c = c.getParent();
		return (DEFrame)c;
		}
	}
