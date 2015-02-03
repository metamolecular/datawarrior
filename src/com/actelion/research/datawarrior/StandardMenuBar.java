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
import java.awt.Event;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.action.DECorrelationDialog;
import com.actelion.research.datawarrior.action.DEFileLoader;
import com.actelion.research.datawarrior.action.DEGenericTautomerCreator;
import com.actelion.research.datawarrior.action.DEHelpFrame;
import com.actelion.research.datawarrior.action.DEInteractiveSARDialog;
import com.actelion.research.datawarrior.action.DEMarkushDialog;
import com.actelion.research.datawarrior.action.DENativeMDLReactionReader;
import com.actelion.research.datawarrior.action.DEReactionDialog;
import com.actelion.research.datawarrior.action.DESARAnalyzer;
import com.actelion.research.datawarrior.action.DESOMkNNAnalyzer;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.DETaskAdd3DCoordinates;
import com.actelion.research.datawarrior.task.DETaskAddEmptyColumns;
import com.actelion.research.datawarrior.task.DETaskAddEmptyRows;
import com.actelion.research.datawarrior.task.DETaskAddFormula;
import com.actelion.research.datawarrior.task.DETaskAddLargestFragment;
import com.actelion.research.datawarrior.task.DETaskAddRecordNumbers;
import com.actelion.research.datawarrior.task.DETaskAddSelectionToList;
import com.actelion.research.datawarrior.task.DETaskAddSmiles;
import com.actelion.research.datawarrior.task.DETaskAddStructureFromName;
import com.actelion.research.datawarrior.task.DETaskAnalyseActivityCliffs;
import com.actelion.research.datawarrior.task.DETaskAnalyseScaffolds;
import com.actelion.research.datawarrior.task.DETaskAutomaticSAR;
import com.actelion.research.datawarrior.task.DETaskCalculateChemicalProperties;
import com.actelion.research.datawarrior.task.DETaskCalculateColumn;
import com.actelion.research.datawarrior.task.DETaskCalculateDescriptor;
import com.actelion.research.datawarrior.task.DETaskCalculateSOM;
import com.actelion.research.datawarrior.task.DETaskClassifyReactions;
import com.actelion.research.datawarrior.task.DETaskClusterCompounds;
import com.actelion.research.datawarrior.task.DETaskCopy;
import com.actelion.research.datawarrior.task.DETaskCreateBins;
import com.actelion.research.datawarrior.task.DETaskDeleteColumns;
import com.actelion.research.datawarrior.task.DETaskDeleteInvisibleRows;
import com.actelion.research.datawarrior.task.DETaskDeleteRedundantRows;
import com.actelion.research.datawarrior.task.DETaskDeleteSelectedRows;
import com.actelion.research.datawarrior.task.DETaskDeselectRowsFromList;
import com.actelion.research.datawarrior.task.DETaskExportHitlist;
import com.actelion.research.datawarrior.task.DETaskFindSimilarCompoundsInFile;
import com.actelion.research.datawarrior.task.DETaskImportHitlist;
import com.actelion.research.datawarrior.task.DETaskInvertSelection;
import com.actelion.research.datawarrior.task.DETaskMergeColumns;
import com.actelion.research.datawarrior.task.DETaskNewColumnWithListNames;
import com.actelion.research.datawarrior.task.DETaskNewRowList;
import com.actelion.research.datawarrior.task.DETaskNewRowListFromLogicalOperation;
import com.actelion.research.datawarrior.task.DETaskPCA;
import com.actelion.research.datawarrior.task.DETaskPaste;
import com.actelion.research.datawarrior.task.DETaskRemoveSelectionFromList;
import com.actelion.research.datawarrior.task.DETaskRunMacro;
import com.actelion.research.datawarrior.task.DETaskSearchAndReplace;
import com.actelion.research.datawarrior.task.DETaskSelectAll;
import com.actelion.research.datawarrior.task.DETaskSelectDiverse;
import com.actelion.research.datawarrior.task.DETaskSelectRowsFromList;
import com.actelion.research.datawarrior.task.DETaskSetValueRange;
import com.actelion.research.datawarrior.task.DETestCompareDescriptorSimilarityDistribution;
import com.actelion.research.datawarrior.task.DETestExtractPairwiseCompoundSimilarities;
import com.actelion.research.datawarrior.task.DETestExtractPairwiseStuff;
import com.actelion.research.datawarrior.task.elib.DETaskCreateEvolutionaryLibrary;
import com.actelion.research.datawarrior.task.file.DETaskAbstractOpenFile;
import com.actelion.research.datawarrior.task.file.DETaskApplyTemplateFromFile;
import com.actelion.research.datawarrior.task.file.DETaskCloseWindow;
import com.actelion.research.datawarrior.task.file.DETaskExportMacro;
import com.actelion.research.datawarrior.task.file.DETaskImportMacro;
import com.actelion.research.datawarrior.task.file.DETaskMergeFile;
import com.actelion.research.datawarrior.task.file.DETaskNewFile;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromPivoting;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromReversePivoting;
import com.actelion.research.datawarrior.task.file.DETaskNewFileFromSelection;
import com.actelion.research.datawarrior.task.file.DETaskOpenFile;
import com.actelion.research.datawarrior.task.file.DETaskRunMacroFromFile;
import com.actelion.research.datawarrior.task.file.DETaskSaveFile;
import com.actelion.research.datawarrior.task.file.DETaskSaveFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveSDFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveTemplateFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveTextFileAs;
import com.actelion.research.datawarrior.task.file.DETaskSaveVisibleRowsAs;
import com.actelion.research.datawarrior.task.filter.DETaskAddNewFilter;
import com.actelion.research.datawarrior.task.filter.DETaskResetAllFilters;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableHitlistListener;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.CompoundTableSOMLoader;
import com.actelion.research.table.view.JCompoundTableForm;

public class StandardMenuBar extends JMenuBar implements ActionListener,
			CompoundTableListener,CompoundTableHitlistListener,ItemListener {
	static final long serialVersionUID = 0x20060728;

	public static final boolean SUPPRESS_CHEMISTRY = false;

	private static final String OPEN_FILE = "open_";
	private static final String SET_RANGE = "range_";
	private static final String LIST_ADD = "add_";
	private static final String LIST_REMOVE = "remove_";
	private static final String LIST_SELECT = "select_";
	private static final String LIST_DESELECT = "deselect_";
	private static final String EXPORT_MACRO = "export_";
	private static final String RUN_GLOBAL_MACRO = "runGlobal_";
	private static final String RUN_INTERNAL_MACRO = "runInternal_";

	final static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private DataWarrior			mApplication;
	private DEFrame				mParentFrame;
	private DEParentPane		mParentPane;
	private DEMainPane			mMainPane;
	private CompoundTableModel	mTableModel;
	private PageFormat			mPageFormat;

	private JMenu jMenuFileOpenSpecial,jMenuFileSaveSpecial,jMenuDataRemoveRows,jMenuDataSelfOrganizingMap,jMenuDataSetRange,
				  jMenuDataViewLogarithmic,jMenuChemAddDescriptor,jMenuListCreate,jMenuMacroExport,jMenuMacroRun;

	private JMenuItem jMenuFileNew,jMenuFileNewFromSelection,jMenuFileNewFromPivoting,jMenuFileNewFromReversePivoting,
					  jMenuFileOpen,jMenuFileOpenMacro,jMenuFileOpenTemplate,jMenuFileOpenMDLReactions,jMenuFileMerge,
					  jMenuFileAppend,jMenuFileClose,jMenuFileCloseAll,jMenuFileSave,jMenuFileSaveAs,jMenuFileSaveText,
					  jMenuFileSaveSDF,jMenuFileSaveTemplate,jMenuFileSaveVisibleAs,jMenuFilePageFormat,
					  jMenuFilePreview,jMenuFilePrint,jMenuFileExit,jMenuEditCut,jMenuEditCopy,jMenuEditPaste,jMenuEditDelete,
					  jMenuEditSelectAll,jMenuEditInvertSelection,jMenuEditSearchAndReplace,jMenuEditResetFilters,
					  jMenuEditNewFilter,jMenuDataRemoveColumns,jMenuDataRemoveSelected,jMenuDataRemoveInvisible,
					  jMenuDataRemoveRedundant,jMenuDataRemoveUnique,jMenuDataMergeColumns,jMenuDataMergeRedundant,
					  jMenuDataAddEmptyColumns,jMenuDataAddEmptyRows,jMenuDataAddRowNumbers,jMenuDataAddCalculatedValues,
					  jMenuDataAddBinnedColumn,jMenuDataAddPrincipalComponents,jMenuDataSOMCreate,jMenuDataSOMApply,
					  jMenuDataSOMAnalyse,jMenuDataSOMSimMatrix,jMenuDataCorrelationMatrix,jMenuChemCCLibrary,
					  jMenuChemEALibrary,jMenuChemEnumerateMarkush,jMenuChemAddProperties,jMenuChemAddFormula,
					  jMenuChemAddSmiles,jMenuChemAdd3DCoords,jMenuChemAddLargestFragment,jMenuChemAddStructureFromName,
					  jMenuChemAutomaticSARTable,jMenuChemCoreBasedSARTable,jMenuChemInteractiveSARTable,
					  jMenuChemAnalyzeScaffolds,jMenuChemAnalyzeCliffs,jMenuChemMatchFile,jMenuChemSelectDiverse,
					  jMenuChemCluster,jMenuChemExtractPKATree,jMenuChemUndocumented,jMenuChemPredictPKa,
					  jMenuChemCreateGenericTautomers,jMenuChemCompareDescriptorSimilarityDistribution,
					  jMenuChemExtractPairwiseCompoundSimilarities,jMenuChemExtractPairwiseStuff,jMenuChemClassifyReactions,
					  jMenuListCreateSelected,jMenuListCreateVisible,jMenuListCreateHidden,jMenuListCreateClipboard,
					  jMenuListCreateMerge,jMenuListRemove,jMenuListRemoveAll,jMenuListNewColumn,jMenuListImport,
					  jMenuListExport,jMenuMacroImport,jMenuMacroStartRecording,jMenuMacroContinueRecording,
					  jMenuMacroStopRecording,jMenuHelpHelp,jMenuHelpAbout,jMenuHelpCheckForUpdate;

	private DEScrollableMenu jMenuListAddSelectedTo,jMenuListRemoveSelectedFrom,jMenuListSelectFrom,jMenuListDeselectFrom;
	private JCheckBoxMenuItem jMenuHelpAutomaticUpdateCheck;

	public StandardMenuBar(DEFrame parentFrame) {
		mApplication = parentFrame.getApplication();
		mParentFrame = parentFrame;
		mParentPane = parentFrame.getMainFrame();
		mMainPane = parentFrame.getMainFrame().getMainPane();
		mTableModel = (CompoundTableModel)mParentPane.getTableModel();
		mTableModel.addCompoundTableListener(this);
		mTableModel.getHitlistHandler().addCompoundTableHitlistListener(this);
		buildMenu();
		}

	public DEFrame getParentFrame() {
		return mParentFrame;
		}

	protected void buildMenu() {
		add(buildFileMenu());
		add(buildEditMenu());
		add(buildDataMenu());
		if (!SUPPRESS_CHEMISTRY)
			add(buildChemistryMenu());
		add(buildListMenu());
		add(buildMacroMenu());
		add(buildHelpMenu());
		}

	protected JMenu buildFileMenu() {
		jMenuFileNew = new JMenuItem();
		jMenuFileNewFromSelection = new JMenuItem();
		jMenuFileNewFromPivoting = new JMenuItem();
		jMenuFileNewFromReversePivoting = new JMenuItem();
		jMenuFileOpen = new JMenuItem();
		jMenuFileOpenSpecial = new JMenu();
		jMenuFileOpenMacro = new JMenuItem();
		jMenuFileOpenTemplate = new JMenuItem();
		jMenuFileOpenMDLReactions = new JMenuItem();
		jMenuFileMerge = new JMenuItem();
		jMenuFileAppend = new JMenuItem();
		jMenuFileClose = new JMenuItem();
		jMenuFileCloseAll = new JMenuItem();
		jMenuFileSave = new JMenuItem();
		jMenuFileSaveAs = new JMenuItem();
		jMenuFileSaveSpecial = new JMenu();
		jMenuFileSaveText = new JMenuItem();
		jMenuFileSaveSDF = new JMenuItem();
		jMenuFileSaveTemplate = new JMenuItem();
		jMenuFileSaveVisibleAs = new JMenuItem();
		jMenuFilePageFormat = new JMenuItem();
		jMenuFilePreview = new JMenuItem();
		jMenuFilePrint = new JMenuItem();
		jMenuFileExit = new JMenuItem();

		jMenuFileNew.setText("New...");
		jMenuFileNew.setAccelerator(KeyStroke.getKeyStroke('N', MENU_MASK));
		jMenuFileNew.addActionListener(this);
		jMenuFileNewFromSelection.setText("New From Selection");
		jMenuFileNewFromSelection.setAccelerator(KeyStroke.getKeyStroke('N', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileNewFromSelection.addActionListener(this);
		jMenuFileNewFromPivoting.setText("New From Pivoting...");
		jMenuFileNewFromPivoting.addActionListener(this);
		jMenuFileNewFromReversePivoting.setText("New From Reverse Pivoting...");
		jMenuFileNewFromReversePivoting.addActionListener(this);
		jMenuFileOpen.setText("Open...");
		jMenuFileOpen.setAccelerator(KeyStroke.getKeyStroke('O', MENU_MASK));
		jMenuFileOpen.addActionListener(this);
		jMenuFileOpenSpecial.setText("Open Special");
		jMenuFileOpenMacro.setText("Run Macro...");
		jMenuFileOpenMacro.addActionListener(this);
		jMenuFileOpenTemplate.setText("Apply Template...");
		jMenuFileOpenTemplate.addActionListener(this);
		jMenuFileOpenMDLReactions.setText("IsisBase Reactions...");
		jMenuFileOpenMDLReactions.addActionListener(this);
		jMenuFileMerge.setText("Merge File...");
		jMenuFileMerge.addActionListener(this);
		jMenuFileAppend.setText("Append File...");
		jMenuFileAppend.addActionListener(this);
		jMenuFileClose.setText("Close");
		jMenuFileClose.setAccelerator(KeyStroke.getKeyStroke('W', MENU_MASK));
		jMenuFileClose.addActionListener(this);
		jMenuFileCloseAll.setText("Close All");
		jMenuFileCloseAll.setAccelerator(KeyStroke.getKeyStroke('W', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileCloseAll.addActionListener(this);
		jMenuFileSave.setText("Save");
		jMenuFileSave.setAccelerator(KeyStroke.getKeyStroke('S', MENU_MASK));
		jMenuFileSave.addActionListener(this);
		jMenuFileSaveAs.setText("Save As...");
		jMenuFileSaveAs.setAccelerator(KeyStroke.getKeyStroke('S', Event.SHIFT_MASK | MENU_MASK));
		jMenuFileSaveAs.addActionListener(this);
		jMenuFileSaveSpecial.setText("Save Special");
		jMenuFileSaveText.setText("Textfile...");
		jMenuFileSaveText.addActionListener(this);
		jMenuFileSaveSDF.setText("SD-File...");
		jMenuFileSaveSDF.addActionListener(this);
		jMenuFileSaveTemplate.setText("Template...");
		jMenuFileSaveTemplate.addActionListener(this);
		jMenuFileSaveVisibleAs.setText("Save Visible As...");
		jMenuFileSaveVisibleAs.addActionListener(this);
		jMenuFilePageFormat.setText("Page Format...");
		jMenuFilePageFormat.addActionListener(this);
		jMenuFilePreview.setText("Print Preview");
		jMenuFilePreview.addActionListener(this);
		jMenuFilePrint.setText("Print...");
		jMenuFilePrint.addActionListener(this);
		if (!mApplication.isMacintosh()) {
			jMenuFileExit.setText("Exit");
			jMenuFileExit.setAccelerator(KeyStroke.getKeyStroke('X', MENU_MASK));
			jMenuFileExit.addActionListener(this);
			}

		JMenu jMenuFile = new JMenu();
		jMenuFile.setText("File");
		jMenuFile.add(jMenuFileNew);
		jMenuFile.add(jMenuFileNewFromSelection);
		jMenuFile.add(jMenuFileNewFromPivoting);
		jMenuFile.add(jMenuFileNewFromReversePivoting);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileOpen);
		jMenuFileOpenSpecial.add(jMenuFileOpenTemplate);
		jMenuFileOpenSpecial.add(jMenuFileOpenMacro);
		addActelionOpenFileMenuOptions(jMenuFileOpenSpecial);
		jMenuFileOpenSpecial.add(jMenuFileOpenMDLReactions);
		jMenuFile.add(jMenuFileOpenSpecial);
		addResourceFileMenus(jMenuFile);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileMerge);
		jMenuFile.add(jMenuFileAppend);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileClose);
		jMenuFile.add(jMenuFileCloseAll);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileSave);
		jMenuFile.add(jMenuFileSaveAs);
		jMenuFileSaveSpecial.add(jMenuFileSaveText);
		jMenuFileSaveSpecial.add(jMenuFileSaveSDF);
		addActelionSaveFileMenuOptions(jMenuFileSaveSpecial);
		jMenuFileSaveSpecial.add(jMenuFileSaveTemplate);
		jMenuFile.add(jMenuFileSaveSpecial);
 		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileSaveVisibleAs);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFilePageFormat);
//		jMenuFile.add(jMenuFilePreview);
		jMenuFile.add(jMenuFilePrint);
		if (!mApplication.isMacintosh()) {
			jMenuFile.addSeparator();
			jMenuFile.add(jMenuFileExit);
			}
		return jMenuFile;
		}

	protected void addActelionOpenFileMenuOptions(JMenu jMenuFileOpenSpecial) {}	// override to add Actelion specific items
	protected void addActelionSaveFileMenuOptions(JMenu jMenuFileOpenSpecial) {}	// override to add Actelion specific items

	protected JMenu buildEditMenu() {
		jMenuEditCut = new JMenuItem();
		jMenuEditCopy = new JMenuItem();
		jMenuEditPaste = new JMenuItem();
		jMenuEditDelete = new JMenuItem();
		jMenuEditSelectAll = new JMenuItem();
		jMenuEditInvertSelection = new JMenuItem();
		jMenuEditSearchAndReplace = new JMenuItem();
		jMenuEditResetFilters = new JMenuItem();
		jMenuEditNewFilter = new JMenuItem();

		jMenuEditCut.setText("Cut");
		jMenuEditCut.setAccelerator(KeyStroke.getKeyStroke('X', MENU_MASK));
		jMenuEditCut.setEnabled(false);
		jMenuEditCopy.setText("Copy");
		jMenuEditCopy.setAccelerator(KeyStroke.getKeyStroke('C', MENU_MASK));
		jMenuEditCopy.addActionListener(this);
		jMenuEditPaste.setText("Paste");
		jMenuEditPaste.setAccelerator(KeyStroke.getKeyStroke('V', MENU_MASK));
		jMenuEditPaste.addActionListener(this);
		jMenuEditDelete.setText("Delete");
		jMenuEditDelete.setEnabled(false);
		jMenuEditSelectAll.setText("Select All");
		jMenuEditSelectAll.setAccelerator(KeyStroke.getKeyStroke('A', MENU_MASK));
		jMenuEditSelectAll.addActionListener(this);
		jMenuEditInvertSelection.setText("Invert Selection");
		jMenuEditInvertSelection.addActionListener(this);
		jMenuEditSearchAndReplace.setText("Search And Replace...");
		jMenuEditSearchAndReplace.addActionListener(this);
		jMenuEditNewFilter.setText("New Filter...");
		jMenuEditNewFilter.addActionListener(this);
		jMenuEditResetFilters.setText("Reset All Filters");
		jMenuEditResetFilters.addActionListener(this);

		JMenu jMenuEdit = new JMenu();
		jMenuEdit.setText("Edit");
		jMenuEdit.add(jMenuEditCut);
		jMenuEdit.add(jMenuEditCopy);
		jMenuEdit.add(jMenuEditPaste);
		jMenuEdit.add(jMenuEditDelete);
 		jMenuEdit.addSeparator();
		jMenuEdit.add(jMenuEditSelectAll);
		jMenuEdit.add(jMenuEditInvertSelection);
 		jMenuEdit.addSeparator();
 		jMenuEdit.add(jMenuEditSearchAndReplace);
 		jMenuEdit.addSeparator();
 		jMenuEdit.add(jMenuEditNewFilter);
 		jMenuEdit.add(jMenuEditResetFilters);
 		return jMenuEdit;
		}

	protected JMenu buildDataMenu() {
		jMenuDataRemoveColumns = new JMenuItem();
		jMenuDataRemoveRows = new JMenu();
		jMenuDataRemoveSelected = new JMenuItem();
		jMenuDataRemoveInvisible = new JMenuItem();
		jMenuDataRemoveRedundant = new JMenuItem();
		jMenuDataRemoveUnique = new JMenuItem();
		jMenuDataMergeColumns = new JMenuItem();
		jMenuDataMergeRedundant = new JMenuItem();
		jMenuDataAddEmptyColumns = new JMenuItem();
		jMenuDataAddEmptyRows = new JMenuItem();
		jMenuDataAddRowNumbers = new JMenuItem();
		jMenuDataAddCalculatedValues = new JMenuItem();
		jMenuDataAddBinnedColumn = new JMenuItem();
		jMenuDataAddPrincipalComponents = new JMenuItem();
		jMenuDataSelfOrganizingMap = new JMenu();
		jMenuDataSOMCreate = new JMenuItem();
		jMenuDataSOMApply = new JMenuItem();
		jMenuDataSOMAnalyse = new JMenuItem();
		jMenuDataSOMSimMatrix = new JMenuItem();
		jMenuDataSetRange = new JMenu();
		jMenuDataViewLogarithmic = new JMenu();
		jMenuDataCorrelationMatrix = new JMenuItem();

		jMenuDataRemoveColumns.setText("Delete Columns...");
		jMenuDataRemoveColumns.addActionListener(this);
		jMenuDataRemoveRows.setText("Delete Rows");
		jMenuDataRemoveSelected.setText("Selected Rows");
		jMenuDataRemoveSelected.addActionListener(this);
		jMenuDataRemoveInvisible.setText("Invisible Rows");
		jMenuDataRemoveInvisible.addActionListener(this);
		jMenuDataRemoveRedundant.setText("Redundant Rows...");
		jMenuDataRemoveRedundant.addActionListener(this);
		jMenuDataRemoveUnique.setText("Unique Rows...");
		jMenuDataRemoveUnique.addActionListener(this);
		jMenuDataMergeColumns.setText("Merge Colums...");
		jMenuDataMergeColumns.addActionListener(this);
		jMenuDataMergeRedundant.setText("Merge Rows...");
		jMenuDataMergeRedundant.addActionListener(this);
		jMenuDataAddEmptyColumns.setText("Add Empty Columns...");
		jMenuDataAddEmptyColumns.addActionListener(this);
		jMenuDataAddEmptyRows.setText("Add Empty Rows...");
		jMenuDataAddEmptyRows.addActionListener(this);
		jMenuDataAddRowNumbers.setText("Add Row Numbers...");
		jMenuDataAddRowNumbers.addActionListener(this);
		jMenuDataAddCalculatedValues.setText("Add Calculated Values...");
		jMenuDataAddCalculatedValues.addActionListener(this);
		jMenuDataAddBinnedColumn.setText("Add Binned Column...");
		jMenuDataAddBinnedColumn.addActionListener(this);
		jMenuDataAddPrincipalComponents.setText("Add Principal Components...");
		jMenuDataAddPrincipalComponents.addActionListener(this);
		jMenuDataSelfOrganizingMap.setText("Self Organizing Map");
		jMenuDataSOMCreate.setText("Create...");
		jMenuDataSOMCreate.addActionListener(this);
		jMenuDataSOMApply.setText("Open And Apply...");
		jMenuDataSOMApply.addActionListener(this);
		jMenuDataSOMAnalyse.setText("Analyse...");
		jMenuDataSOMAnalyse.addActionListener(this);
		jMenuDataSOMSimMatrix.setText("Create Similarity Matrix...");
		jMenuDataSOMSimMatrix.addActionListener(this);
		jMenuDataViewLogarithmic.setText("Treat Logarithmically");
		jMenuDataSetRange.setText("Set Value Range");
		jMenuDataCorrelationMatrix.setText("Show Correlation Matrix...");
		jMenuDataCorrelationMatrix.addActionListener(this);

		JMenu jMenuData = new JMenu();
		jMenuData.setText("Data");
		jMenuData.add(jMenuDataRemoveColumns);
		jMenuData.add(jMenuDataRemoveRows);
		jMenuDataRemoveRows.add(jMenuDataRemoveSelected);
		jMenuDataRemoveRows.add(jMenuDataRemoveInvisible);
		jMenuDataRemoveRows.add(jMenuDataRemoveRedundant);
		jMenuDataRemoveRows.add(jMenuDataRemoveUnique);
 		jMenuData.addSeparator();
 		jMenuData.add(jMenuDataMergeColumns);
		jMenuData.add(jMenuDataMergeRedundant);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataAddEmptyColumns);
		jMenuData.add(jMenuDataAddEmptyRows);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataAddRowNumbers);
		jMenuData.add(jMenuDataAddCalculatedValues);
		jMenuData.add(jMenuDataAddBinnedColumn);
		jMenuData.add(jMenuDataAddPrincipalComponents);
		jMenuData.add(jMenuDataSelfOrganizingMap);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMCreate);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMApply);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMAnalyse);
		jMenuDataSelfOrganizingMap.add(jMenuDataSOMSimMatrix);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataSetRange);
		jMenuData.add(jMenuDataViewLogarithmic);
		jMenuData.addSeparator();
		jMenuData.add(jMenuDataCorrelationMatrix);
		return jMenuData;
		}

	protected JMenu buildChemistryMenu() {
		jMenuChemCCLibrary = new JMenuItem();
		jMenuChemEALibrary = new JMenuItem();
		jMenuChemEnumerateMarkush = new JMenuItem();
		jMenuChemAddProperties = new JMenuItem();
		jMenuChemAddDescriptor = new JMenu();
		jMenuChemAddFormula = new JMenuItem();
		jMenuChemAddSmiles = new JMenuItem();
		jMenuChemAdd3DCoords = new JMenuItem();
		jMenuChemAddLargestFragment = new JMenuItem();
		jMenuChemAddStructureFromName = new JMenuItem();
		jMenuChemAutomaticSARTable = new JMenuItem();
		jMenuChemCoreBasedSARTable = new JMenuItem();
		jMenuChemInteractiveSARTable = new JMenuItem();
		jMenuChemAnalyzeScaffolds = new JMenuItem();
		jMenuChemAnalyzeCliffs = new JMenuItem();
		jMenuChemMatchFile = new JMenuItem();
		jMenuChemSelectDiverse = new JMenuItem();
		jMenuChemCluster = new JMenuItem();
		jMenuChemExtractPKATree = new JMenuItem();
		jMenuChemUndocumented = new JMenuItem();
		jMenuChemPredictPKa = new JMenuItem();
		jMenuChemCreateGenericTautomers = new JMenuItem();
		jMenuChemCompareDescriptorSimilarityDistribution = new JMenuItem();
		jMenuChemExtractPairwiseCompoundSimilarities = new JMenuItem();
		jMenuChemExtractPairwiseStuff = new JMenuItem();
		jMenuChemClassifyReactions = new JMenuItem();

		jMenuChemCCLibrary.setText("Create Combinatorial Library...");
		jMenuChemCCLibrary.addActionListener(this);
		jMenuChemEALibrary.setText("Create Evolutionary Library...");
		jMenuChemEALibrary.addActionListener(this);
		jMenuChemEnumerateMarkush.setText("Enumerate Markush Structure...");
		jMenuChemEnumerateMarkush.addActionListener(this);
		jMenuChemAddProperties.setText("Add Compound Properties...");
		jMenuChemAddProperties.addActionListener(this);
		jMenuChemAddDescriptor.setText("Add Descriptor");
		jMenuChemAdd3DCoords.setText("Add 3D-Coordinates...");
		jMenuChemAdd3DCoords.addActionListener(this);
		jMenuChemAddFormula.setText("Add Molecular Formula");
		jMenuChemAddFormula.addActionListener(this);
		jMenuChemAddSmiles.setText("Add Smiles");
		jMenuChemAddSmiles.addActionListener(this);
		jMenuChemAddLargestFragment.setText("Add Largest Fragment");
		jMenuChemAddLargestFragment.addActionListener(this);
		jMenuChemAddStructureFromName.setText("Add Structures From Name");
		jMenuChemAddStructureFromName.addActionListener(this);
		jMenuChemAutomaticSARTable.setText("Automatic SAR Analysis...");
		jMenuChemAutomaticSARTable.addActionListener(this);
		jMenuChemCoreBasedSARTable.setText("Core based SAR Analysis...");
		jMenuChemCoreBasedSARTable.addActionListener(this);
		jMenuChemInteractiveSARTable.setText("Interactive SAR Analysis...");
		jMenuChemInteractiveSARTable.addActionListener(this);
		jMenuChemAnalyzeScaffolds.setText("Analyse Scaffolds...");
		jMenuChemAnalyzeScaffolds.addActionListener(this);
		jMenuChemAnalyzeCliffs.setText("Analyse Similarity/Activity Cliffs...");
		jMenuChemAnalyzeCliffs.addActionListener(this);
		jMenuChemMatchFile.setText("Find Similar Compounds In File...");
		jMenuChemMatchFile.addActionListener(this);
		jMenuChemSelectDiverse.setText("Select Diverse Set...");
		jMenuChemSelectDiverse.addActionListener(this);
		jMenuChemCluster.setText("Cluster Compounds...");
		jMenuChemCluster.addActionListener(this);
		jMenuChemExtractPKATree.setText("Extract pKa-Tree");
		jMenuChemExtractPKATree.addActionListener(this);
		jMenuChemUndocumented.setText("Do Undocumented Stuff");
		jMenuChemUndocumented.addActionListener(this);
		jMenuChemPredictPKa.setText("Predict pKa");
		jMenuChemPredictPKa.addActionListener(this);
		jMenuChemCreateGenericTautomers.setText("Create Generic Tautomers");
		jMenuChemCreateGenericTautomers.addActionListener(this);
		jMenuChemCompareDescriptorSimilarityDistribution.setText("Compare Descriptor Similarity Distribution");
		jMenuChemCompareDescriptorSimilarityDistribution.addActionListener(this);
		jMenuChemExtractPairwiseCompoundSimilarities.setText("Extract Pairwise Compound Similarities");
		jMenuChemExtractPairwiseCompoundSimilarities.addActionListener(this);
		jMenuChemExtractPairwiseStuff.setText("Extract Pairwise Similarities And Distances");
		jMenuChemExtractPairwiseStuff.addActionListener(this);
		jMenuChemClassifyReactions.setText("Classify Reactions");
		jMenuChemClassifyReactions.addActionListener(this);

		JMenu jMenuChem = new JMenu();
		jMenuChem.setText("Chemistry");
		jMenuChem.add(jMenuChemCCLibrary);
		jMenuChem.add(jMenuChemEALibrary);
//		jMenuChem.add(jMenuChemEnumerateMarkush);
 		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAddProperties);
		jMenuChem.add(jMenuChemAddDescriptor);
		for (int i=0; i<DescriptorConstants.DESCRIPTOR_LIST.length; i++) {
			JMenuItem item = new JMenuItem();
			item.setText(DescriptorConstants.DESCRIPTOR_LIST[i].shortName);
			item.addActionListener(this);
			jMenuChemAddDescriptor.add(item);
			}
		jMenuChem.add(jMenuChemAddFormula);
		jMenuChem.add(jMenuChemAddSmiles);
		jMenuChem.add(jMenuChemAdd3DCoords);
		jMenuChem.add(jMenuChemAddLargestFragment);
		jMenuChem.add(jMenuChemAddStructureFromName);
		addActelionChemistryMenuOptions(jMenuChem);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAutomaticSARTable);
		jMenuChem.add(jMenuChemCoreBasedSARTable);
		if (mApplication.isActelion() || System.getProperty("development") != null)
			jMenuChem.add(jMenuChemInteractiveSARTable);
		jMenuChem.add(jMenuChemAnalyzeScaffolds);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemAnalyzeCliffs);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemMatchFile);
		jMenuChem.addSeparator();
		jMenuChem.add(jMenuChemSelectDiverse);
		jMenuChem.add(jMenuChemCluster);
		if (System.getProperty("development") != null) {
			jMenuChem.addSeparator();
//			jMenuChem.add(jMenuChemExtractPKATree);
			jMenuChem.add(jMenuChemCompareDescriptorSimilarityDistribution);
			jMenuChem.add(jMenuChemExtractPairwiseCompoundSimilarities);
			jMenuChem.add(jMenuChemExtractPairwiseStuff);
			jMenuChem.addSeparator();
			jMenuChem.add(jMenuChemClassifyReactions);
//		jMenuChem.add(jMenuChemPredictPKa);
//		jMenuChem.add(jMenuChemCreateGenericTautomers);
			}
		
		return jMenuChem;
		}

	protected void addActelionChemistryMenuOptions(JMenu jMenuChem) {}	// override to add Actelion specific items

	protected JMenu buildListMenu() {
		jMenuListCreate = new JMenu();
		jMenuListCreateSelected = new JMenuItem();
		jMenuListCreateVisible = new JMenuItem();
		jMenuListCreateHidden = new JMenuItem();
		jMenuListCreateClipboard = new JMenuItem();
		jMenuListCreateMerge = new JMenuItem();
		jMenuListAddSelectedTo = new DEScrollableMenu();
		jMenuListRemoveSelectedFrom = new DEScrollableMenu();
		jMenuListSelectFrom = new DEScrollableMenu();
		jMenuListDeselectFrom = new DEScrollableMenu();
		jMenuListRemove = new JMenuItem();
		jMenuListRemoveAll = new JMenuItem();
		jMenuListNewColumn = new JMenuItem();
		jMenuListImport = new JMenuItem();
		jMenuListExport = new JMenuItem();

		jMenuListCreate.setText("Create Row List From");
		jMenuListCreateSelected.setText("Selected Rows...");
		jMenuListCreateSelected.addActionListener(this);
		jMenuListCreateVisible.setText("Visible Rows...");
		jMenuListCreateVisible.addActionListener(this);
		jMenuListCreateHidden.setText("Hidden Rows...");
		jMenuListCreateHidden.addActionListener(this);
		jMenuListCreateClipboard.setText("Clipboard...");
		jMenuListCreateClipboard.addActionListener(this);
		jMenuListCreateMerge.setText("Existing Row Lists...");
		jMenuListCreateMerge.addActionListener(this);
		jMenuListAddSelectedTo.setText("Add Selected To");
		jMenuListRemoveSelectedFrom.setText("Remove Selected From");
		jMenuListSelectFrom.setText("Select Rows From");
		jMenuListDeselectFrom.setText("Deselect Rows From");
		jMenuListRemove.setText("Delete Row List...");
		jMenuListRemove.addActionListener(this);
		jMenuListRemoveAll.setText("Delete All Row Lists");
		jMenuListRemoveAll.addActionListener(this);
		jMenuListNewColumn.setText("Add Column From Row Lists...");
		jMenuListNewColumn.addActionListener(this);
		jMenuListExport.setText("Export Row List...");
		jMenuListExport.addActionListener(this);
		jMenuListImport.setText("Import Row List...");
		jMenuListImport.addActionListener(this);
		jMenuListCreate.add(jMenuListCreateSelected);
		jMenuListCreate.add(jMenuListCreateVisible);
		jMenuListCreate.add(jMenuListCreateHidden);
 		jMenuListCreate.addSeparator();
		jMenuListCreate.add(jMenuListCreateClipboard);
 		jMenuListCreate.addSeparator();
		jMenuListCreate.add(jMenuListCreateMerge);
		JMenu jMenuList = new JMenu();
		jMenuList.setText("List");
		jMenuList.add(jMenuListCreate);
 		jMenuList.addSeparator();
 		jMenuList.add(jMenuListAddSelectedTo);
 		jMenuList.add(jMenuListRemoveSelectedFrom);
 		jMenuList.addSeparator();
 		jMenuList.add(jMenuListSelectFrom);
 		jMenuList.add(jMenuListDeselectFrom);
 		jMenuList.addSeparator();
		jMenuList.add(jMenuListRemove);
		jMenuList.add(jMenuListRemoveAll);
		jMenuList.addSeparator();
		jMenuList.add(jMenuListNewColumn);
		jMenuList.addSeparator();
		jMenuList.add(jMenuListImport);
		jMenuList.add(jMenuListExport);
		return jMenuList;
		}

	protected JMenu buildMacroMenu() {
		jMenuMacroImport = new JMenuItem();
		jMenuMacroExport = new JMenu();
		jMenuMacroRun = new JMenu();
		jMenuMacroStartRecording = new JMenuItem();
		jMenuMacroContinueRecording = new JMenuItem();
		jMenuMacroStopRecording = new JMenuItem();

		jMenuMacroImport.setText("Import Macro...");
		jMenuMacroImport.addActionListener(this);
		jMenuMacroExport.setText("Export Macro");
		jMenuMacroStartRecording.setText("Start Recording...");
		jMenuMacroStartRecording.addActionListener(this);
		jMenuMacroContinueRecording.setText("Continue Recording");
		jMenuMacroContinueRecording.setEnabled(false);
		jMenuMacroContinueRecording.addActionListener(this);
		jMenuMacroStopRecording.setText("Stop Recording");
		jMenuMacroStopRecording.setEnabled(false);
		jMenuMacroStopRecording.addActionListener(this);
		jMenuMacroRun.setText("Run Macro");
		addMenuItem(jMenuMacroExport, "<no macros defined>", null);
		addMacroFileItems(jMenuMacroRun);
		JMenu jMenuMacro = new JMenu();
		jMenuMacro.setText("Macro");
		jMenuMacro.add(jMenuMacroImport);
		jMenuMacro.add(jMenuMacroExport);
 		jMenuMacro.addSeparator();
		jMenuMacro.add(jMenuMacroStartRecording);
		jMenuMacro.add(jMenuMacroContinueRecording);
		jMenuMacro.add(jMenuMacroStopRecording);
 		jMenuMacro.addSeparator();
		jMenuMacro.add(jMenuMacroRun);
		return jMenuMacro;
		}

	protected JMenu buildHelpMenu() {
		jMenuHelpHelp = new JMenuItem();
		jMenuHelpAbout = new JMenuItem();
		jMenuHelpAutomaticUpdateCheck = new JCheckBoxMenuItem();
		jMenuHelpCheckForUpdate = new JMenuItem();

		jMenuHelpHelp.setText("Help...");
		jMenuHelpHelp.addActionListener(this);
		if (!mApplication.isMacintosh()) {
			jMenuHelpAbout.setText("About...");
			jMenuHelpAbout.addActionListener(this);
			}
		JMenu jMenuHelp = new JMenu();
		jMenuHelp.setText("Help");
		jMenuHelp.add(jMenuHelpHelp);
		if (!mApplication.isMacintosh()) {
			jMenuHelp.addSeparator();
			jMenuHelp.add(jMenuHelpAbout);
			}

		if (!mApplication.isActelion()) {
			Preferences prefs = Preferences.userRoot().node(DataWarrior.PREFERENCES_ROOT);
			boolean check = prefs.getBoolean(DataWarrior.PREFERENCES_KEY_AUTO_UPDATE_CHECK, true);

			jMenuHelpAutomaticUpdateCheck.setText("Automatically Check For Updates");
			jMenuHelpAutomaticUpdateCheck.setSelected(check);
			jMenuHelpAutomaticUpdateCheck.addActionListener(this);
			jMenuHelpCheckForUpdate.setText("Check For Update Now...");
			jMenuHelpCheckForUpdate.addActionListener(this);

			jMenuHelp.addSeparator();
			jMenuHelp.add(jMenuHelpAutomaticUpdateCheck);
			jMenuHelp.add(jMenuHelpCheckForUpdate);
			}

		return jMenuHelp;
		}

	private void ensurePageFormat(PrinterJob job) {
		if (mPageFormat == null) {
			mPageFormat = job.defaultPage();
			Paper paper = mPageFormat.getPaper();
			paper.setImageableArea(60, 30, paper.getWidth() - 90, paper.getHeight() - 60);
			mPageFormat.setPaper(paper);
			}
		}

	private void menuFilePageFormat() {
		PrinterJob job = PrinterJob.getPrinterJob();
		ensurePageFormat(job);
		mPageFormat = job.pageDialog(mPageFormat);
		}

	private void menuFilePreview() {
		
		}

	public void menuFilePrint() {
		if (mMainPane.getSelectedDockable() == null) {
			JOptionPane.showMessageDialog(mParentFrame, "Sorry, an empty view cannot be printed");
			return;
			}

		PrinterJob job = PrinterJob.getPrinterJob();
		ensurePageFormat(job);

		try {
			Component component = mMainPane.getSelectedDockable().getContent();
			if (component instanceof DEFormView) {
				JCompoundTableForm form = ((DEFormView)component).getCompoundTableForm();
				if (!new DEPrintFormDialog(mParentFrame, mTableModel, form).isOK())
					return;
				form.setPageFormat(mPageFormat);
				job.setPageable(form);
				}
			else { // assume Printable
				job.setPrintable((Printable)component, mPageFormat);
				}

			job.setJobName("DataWarrior:"+mParentFrame.getTitle());
			if (job.printDialog()) {
				try {
					job.print();
					}
				catch (PrinterException e) {
					JOptionPane.showMessageDialog(mParentFrame, e);
					}
				}
			}
		catch (ClassCastException e) {
			JOptionPane.showMessageDialog(mParentFrame, "Sorry, the current view cannot be printed");
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			jMenuListSelectFrom.removeAll();
			jMenuListDeselectFrom.removeAll();
			jMenuListAddSelectedTo.removeAll();
			jMenuListRemoveSelectedFrom.removeAll();
			}
		if (e.getType() == CompoundTableEvent.cNewTable
		 || e.getType() == CompoundTableEvent.cAddColumns
		 || e.getType() == CompoundTableEvent.cRemoveColumns
		 || e.getType() == CompoundTableEvent.cChangeColumnName
		 || e.getType() == CompoundTableEvent.cChangeColumnData
		 || e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows) {
			jMenuDataViewLogarithmic.removeAll();
			jMenuDataSetRange.removeAll();
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isLogarithmicViewMode(column)
				 || (mTableModel.isColumnTypeDouble(column)
				  && !mTableModel.isColumnTypeDate(column)
				  && mTableModel.getMinimumValue(column) > 0)) {
					JCheckBoxMenuItem item = new JCheckBoxMenuItem(mTableModel.getColumnTitle(column),
																   mTableModel.isLogarithmicViewMode(column));
					item.addItemListener(this);
					jMenuDataViewLogarithmic.add(item);
					}
				if (mTableModel.isColumnTypeDouble(column)
				 && !mTableModel.isColumnTypeDate(column)) {
					addMenuItem(jMenuDataSetRange, mTableModel.getColumnTitle(column)+"...", SET_RANGE+mTableModel.getColumnTitle(column));
					}
				}
			}
		if (e.getType() == CompoundTableEvent.cNewTable
		 || (e.getType() == CompoundTableEvent.cChangeExtensionData
		  && e.getSpecifier() == DECompoundTableExtensionHandler.ID_MACRO)) {
			jMenuMacroExport.removeAll();
			jMenuMacroRun.removeAll();
			@SuppressWarnings("unchecked")
			ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
			if (macroList == null || macroList.size() == 0) {
				addMenuItem(jMenuMacroExport, "<no macros defined>", null);
				}
			else {
				for (DEMacro macro:macroList) {
					addMenuItem(jMenuMacroExport, macro.getName()+"...", EXPORT_MACRO+macro.getName());
					addMenuItem(jMenuMacroRun, macro.getName(), RUN_INTERNAL_MACRO+macro.getName());
					}
				}
			addMacroFileItems(jMenuMacroRun);
			}
		}

	public void hitlistChanged(CompoundTableHitlistEvent e) {
		jMenuListSelectFrom.removeAll();
		jMenuListDeselectFrom.removeAll();
		jMenuListAddSelectedTo.removeAll();
		jMenuListRemoveSelectedFrom.removeAll();
		CompoundTableHitlistHandler hitlistHandler = mTableModel.getHitlistHandler();

		for (int hitlist=0; hitlist<hitlistHandler.getHitlistCount(); hitlist++) {
			addMenuItem(jMenuListAddSelectedTo, hitlistHandler.getHitlistName(hitlist), LIST_ADD+hitlist);
			addMenuItem(jMenuListRemoveSelectedFrom, hitlistHandler.getHitlistName(hitlist), LIST_REMOVE+hitlist);
			addMenuItem(jMenuListSelectFrom, hitlistHandler.getHitlistName(hitlist), LIST_SELECT+hitlist);
			addMenuItem(jMenuListDeselectFrom, hitlistHandler.getHitlistName(hitlist), LIST_DESELECT+hitlist);
			}
		}

	/**
	 * @param menu
	 * @param text
	 * @param actionCommand if null, then show menu item as disabled
	 */
	private void addMenuItem(JMenu menu, String text, String actionCommand) {
		JMenuItem item = new JMenuItem(text);
		if (actionCommand != null) {
			item.setActionCommand(actionCommand);
			item.addActionListener(this);
			}
		menu.add(item);
		}

	public void actionPerformed(ActionEvent e) {
		try {
			Object source = e.getSource();
			String actionCommand = e.getActionCommand();
			if (source == jMenuFilePageFormat)
				menuFilePageFormat();
			else if (source == jMenuFilePreview)
				menuFilePreview();
			else if (source == jMenuFilePrint)
				menuFilePrint();
			else if (source == jMenuFileExit)
				mApplication.closeApplication();
			else if (source == jMenuHelpAbout)
				new DEAboutDialog(mParentFrame);
			else if (source == jMenuHelpHelp)
				new DEHelpFrame(mParentFrame);
			else if (source == jMenuFileNew)
				new DETaskNewFile(mApplication).defineAndRun();
			else if (source == jMenuFileNewFromSelection)
				new DETaskNewFileFromSelection(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuFileNewFromPivoting)
				new DETaskNewFileFromPivoting(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuFileNewFromReversePivoting)
				new DETaskNewFileFromReversePivoting(mParentFrame, mApplication).defineAndRun();
			else if (actionCommand.startsWith(OPEN_FILE))	// these are the reference,sample,etc-files
				new DETaskOpenFile(mApplication, actionCommand.substring(OPEN_FILE.length())).defineAndRun();
			else if (source == jMenuFileOpen)
				new DETaskOpenFile(mApplication, true).defineAndRun();
			else if (source == jMenuFileOpenTemplate)
				new DETaskApplyTemplateFromFile(mApplication, true).defineAndRun();
			else if (source == jMenuFileOpenMacro)
				new DETaskRunMacroFromFile(mApplication, true).defineAndRun();
			else if (source == jMenuFileOpenMDLReactions)
				new DENativeMDLReactionReader(mParentFrame, mApplication).read();
			else if (source == jMenuFileMerge)
				new DETaskMergeFile(mParentFrame, true).defineAndRun();
			else if (source == jMenuFileAppend) {
				if (mParentFrame.getTableModel().getTotalRowCount() == 0)
					JOptionPane.showMessageDialog(mParentFrame, "You cannot append a file to an empty table. Use 'Open File...' instead.");
				else {
					File file = FileHelper.getFile(mParentFrame, "Append DataWarrior-, SD- or Text-File", FileHelper.cFileTypeDataWarriorCompatibleData);
					if (file != null)
						new DEFileLoader(mParentFrame, null).appendFile(file);
					}
				}
			else if (source == jMenuFileClose)
				new DETaskCloseWindow(mParentFrame, mApplication, mParentFrame).defineAndRun();
			else if (source == jMenuFileCloseAll)
				mApplication.closeAllFrames();
			else if (source == jMenuFileSave) {
				if (mTableModel.getFile() == null)
					new DETaskSaveFileAs(mParentFrame, true).defineAndRun();
				else
					new DETaskSaveFile(mParentFrame, true).defineAndRun();
				}
			else if (source == jMenuFileSaveAs)
				new DETaskSaveFileAs(mParentFrame, true).defineAndRun();
			else if (source == jMenuFileSaveText)
				new DETaskSaveTextFileAs(mParentFrame, true).defineAndRun();
			else if (source == jMenuFileSaveSDF)
				new DETaskSaveSDFileAs(mParentFrame, true).defineAndRun();
			else if (source == jMenuFileSaveTemplate)
				new DETaskSaveTemplateFileAs(mParentFrame, true).defineAndRun();
			else if (source == jMenuFileSaveVisibleAs)
				new DETaskSaveVisibleRowsAs(mParentFrame, true).defineAndRun();
			else if (source == jMenuEditCopy)
				new DETaskCopy(mParentFrame).defineAndRun();
			else if (source == jMenuEditPaste)
				new DETaskPaste(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuEditSelectAll)
				new DETaskSelectAll(mParentFrame).defineAndRun();
			else if (source == jMenuEditInvertSelection)
				new DETaskInvertSelection(mParentFrame).defineAndRun();
			else if (source == jMenuEditSearchAndReplace)
				new DETaskSearchAndReplace(mParentFrame, true).defineAndRun();
			else if (source == jMenuEditNewFilter)
				new DETaskAddNewFilter(mParentFrame, true).defineAndRun();
			else if (source == jMenuEditResetFilters)
				new DETaskResetAllFilters(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveColumns)
				new DETaskDeleteColumns(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataRemoveSelected)
				new DETaskDeleteSelectedRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveInvisible)
				new DETaskDeleteInvisibleRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataRemoveRedundant)
				new DETaskDeleteRedundantRows(mParentFrame, DETaskDeleteRedundantRows.MODE_REMOVE_REDUNDANT, true).defineAndRun();
			else if (source == jMenuDataRemoveUnique)
				new DETaskDeleteRedundantRows(mParentFrame, DETaskDeleteRedundantRows.MODE_REMOVE_UNIQUE, true).defineAndRun();
			else if (source == jMenuDataMergeColumns)
				new DETaskMergeColumns(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataMergeRedundant)
				new DETaskDeleteRedundantRows(mParentFrame, DETaskDeleteRedundantRows.MODE_MERGE_REDUNDANT, true).defineAndRun();
			else if (source == jMenuDataAddRowNumbers)
				new DETaskAddRecordNumbers(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataAddEmptyColumns)
				new DETaskAddEmptyColumns(mApplication).defineAndRun();
			else if (source == jMenuDataAddEmptyRows)
				new DETaskAddEmptyRows(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddCalculatedValues)
				new DETaskCalculateColumn(mParentFrame).defineAndRun();
			else if (source == jMenuDataAddBinnedColumn)
				new DETaskCreateBins(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataAddPrincipalComponents)
				new DETaskPCA(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataSOMCreate)
				new DETaskCalculateSOM(mParentFrame, true).defineAndRun();
			else if (source == jMenuDataSOMApply) {
				File file = FileHelper.getFile(mParentFrame, "Select DataWarrior SOM File", FileHelper.cFileTypeSOM);
				if (file != null)
					new CompoundTableSOMLoader(mParentFrame).apply(file, mTableModel);
				}
			else if (source == jMenuDataSOMAnalyse) {
				File file = FileHelper.getFile(mParentFrame, "Select DataWarrior SOM File", FileHelper.cFileTypeSOM);
				if (file != null) {
					DEFrame targetFrame = mApplication.getEmptyFrame(null);
					new CompoundTableSOMLoader(targetFrame).analyse(file, targetFrame.getTableModel());
					}
				}
			else if (source == jMenuDataSOMSimMatrix)
				new DESOMkNNAnalyzer(mParentFrame, mApplication).analyze();
			else if (source == jMenuDataCorrelationMatrix) {
				new DECorrelationDialog(mParentFrame, mTableModel);
				}
			else if (source == jMenuChemCCLibrary)
				new DEReactionDialog(mParentFrame, mApplication);
			else if (source == jMenuChemEALibrary)
				new DETaskCreateEvolutionaryLibrary(mParentFrame, mApplication, true).defineAndRun();
			else if (source == jMenuChemEnumerateMarkush)
				new DEMarkushDialog(mParentFrame, mApplication);
			else if (source == jMenuChemAddProperties)
				new DETaskCalculateChemicalProperties(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemAdd3DCoords)
				new DETaskAdd3DCoordinates(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemAddFormula)
				new DETaskAddFormula(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemAddSmiles)
				new DETaskAddSmiles(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemAddLargestFragment)
				new DETaskAddLargestFragment(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemAddStructureFromName)
				new DETaskAddStructureFromName(mParentFrame).defineAndRun();
			else if (source == jMenuChemAutomaticSARTable)
				new DETaskAutomaticSAR(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemCoreBasedSARTable) {
				int idcodeColumn = getStructureColumn(true);
				if (idcodeColumn != -1)
					new DESARAnalyzer(mParentFrame, mTableModel).analyze(idcodeColumn);
				}
			else if (source == jMenuChemInteractiveSARTable) {
JOptionPane.showMessageDialog(mParentFrame, "This functionality is not final yet.\nSuggestions and sample data are welcome.");
				int idcodeColumn = getStructureColumn(true);
				if (idcodeColumn != -1)
					new DEInteractiveSARDialog(mParentFrame, mTableModel, idcodeColumn);
				}
			else if (source == jMenuChemAnalyzeScaffolds)
				new DETaskAnalyseScaffolds(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemAnalyzeCliffs)
				new DETaskAnalyseActivityCliffs(mParentFrame, mApplication, true).defineAndRun();
			else if (source == jMenuChemMatchFile)
				new DETaskFindSimilarCompoundsInFile(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemSelectDiverse)
				new DETaskSelectDiverse(mParentFrame, true).defineAndRun();
			else if (source == jMenuChemCluster)
				new DETaskClusterCompounds(mParentFrame, true).defineAndRun();
/*		  else if (source == jMenuChemExtractPKATree)
				new PKATreeExtractor(mParentFrame, new PKADataWarriorAdapter(mTableModel)).extract();	*/
			else if (source == jMenuChemCompareDescriptorSimilarityDistribution)
				new DETestCompareDescriptorSimilarityDistribution(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemExtractPairwiseCompoundSimilarities)
				new DETestExtractPairwiseCompoundSimilarities(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemExtractPairwiseStuff)
				new DETestExtractPairwiseStuff(mParentFrame, mApplication).defineAndRun();
			else if (source == jMenuChemClassifyReactions)
				new DETaskClassifyReactions(mParentFrame, true).defineAndRun();

//			  int idcodeColumn = getStructureColumn(true);
//			  if (idcodeColumn != -1)
//					new UndocumentedStuff(mParentFrame, mMainPane, mTableModel, idcodeColumn).doStuff();

/*			else if (source == jMenuChemPredictPKa) {
				int idcodeColumn = getStructureColumn(true);
				if (idcodeColumn != -1)
					new UndocumentedStuff(mParentFrame, mMainPane, mTableModel, idcodeColumn).predictPKaValues();
				}*/
			else if (source == jMenuChemCreateGenericTautomers) {
				int idcodeColumn = getStructureColumn(false);
				if (idcodeColumn != -1)
					new DEGenericTautomerCreator(mParentFrame, mTableModel).create(idcodeColumn);
				}
			else if (source == jMenuListCreateSelected) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_SELECTED))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_SELECTED).defineAndRun();
				}
			else if (source == jMenuListCreateVisible) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_VISIBLE))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_VISIBLE).defineAndRun();
				}
			else if (source == jMenuListCreateHidden) {
				if (checkAndAllowEmptyList(DETaskNewRowList.MODE_HIDDEN))
					new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_HIDDEN).defineAndRun();
				}
			else if (source == jMenuListCreateClipboard)
				new DETaskNewRowList(mParentFrame, DETaskNewRowList.MODE_CLIPBOARD).defineAndRun();
			else if (source == jMenuListCreateMerge)
				new DETaskNewRowListFromLogicalOperation(mParentFrame).defineAndRun();
			else if (actionCommand.startsWith(LIST_ADD))
				new DETaskAddSelectionToList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_ADD.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_REMOVE))
				new DETaskRemoveSelectionFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_REMOVE.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_SELECT))
				new DETaskSelectRowsFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_SELECT.length()))).defineAndRun();
			else if (actionCommand.startsWith(LIST_DESELECT))
				new DETaskDeselectRowsFromList(mParentFrame, Integer.parseInt(actionCommand.substring(LIST_DESELECT.length()))).defineAndRun();
			else if (source == jMenuListRemove) {
				String[] names = mTableModel.getHitlistHandler().getHitlistNames();
				if (names == null) {
					JOptionPane.showMessageDialog(mParentFrame, "There are no row lists to be removed.");
					}
				else {
					String name = (String)JOptionPane.showInputDialog(mParentFrame,
															  "Row list name?",
															  "Delete Row List",
															  JOptionPane.QUESTION_MESSAGE,
															  (Icon)null,
															  names,
															  names[0]);
					if (name != null)
						mTableModel.getHitlistHandler().deleteHitlist(name);
					}
				}
			else if (source == jMenuListRemoveAll) {
				String[] names = mTableModel.getHitlistHandler().getHitlistNames();
				if (names == null) {
					JOptionPane.showMessageDialog(mParentFrame, "There are no row lists to be removed.");
					}
				else {
					int doDelete = JOptionPane.showConfirmDialog(this,
									"Do you really want to delete all row lists?",
									"Delete All Row Lists?",
									JOptionPane.OK_CANCEL_OPTION);
					if (doDelete == JOptionPane.OK_OPTION)
						for (int i=0; i<names.length; i++)
							mTableModel.getHitlistHandler().deleteHitlist(names[i]);
					}
				}
			else if (source == jMenuListNewColumn)
				new DETaskNewColumnWithListNames(mParentFrame).defineAndRun();
			else if (source == jMenuListImport)
				new DETaskImportHitlist(mParentFrame, true).defineAndRun();
			else if (source == jMenuListExport)
				new DETaskExportHitlist(mParentFrame, true).defineAndRun();
			else if (source == jMenuMacroImport) {
				new DETaskImportMacro(mParentFrame, true).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(EXPORT_MACRO)) {
				String macroName = e.getActionCommand().substring(EXPORT_MACRO.length());
				new DETaskExportMacro(mParentFrame, macroName).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(RUN_GLOBAL_MACRO)) {
				new DETaskRunMacroFromFile(mApplication, e.getActionCommand().substring(RUN_GLOBAL_MACRO.length())).defineAndRun();
				}
			else if (e.getActionCommand().startsWith(RUN_INTERNAL_MACRO)) {
				new DETaskRunMacro(mParentFrame, e.getActionCommand().substring(RUN_INTERNAL_MACRO.length())).defineAndRun();
				}
			else if (source == jMenuMacroStartRecording) {
				if (DEMacroRecorder.getInstance().isRecording()) {
					JOptionPane.showMessageDialog(mParentFrame, "You are already recording a macro in another window.");
					}
				else {
					boolean frameIsEmpty = mParentFrame.getTableModel().isEmpty()
										&& mParentFrame.getMainFrame().getMainPane().getDockableCount() == 0;
					DEMacro macro = DEMacroEditor.addNewMacro(mParentFrame, null);
					if (macro != null) {
						if (frameIsEmpty)
							mMainPane.addApplicationView(DEMainPane.VIEW_TYPE_MACRO_EDITOR, "Macro Editor", "root");
	
						DEMacroRecorder.getInstance().startRecording(macro, mParentFrame);
						jMenuMacroStartRecording.setEnabled(false);
						jMenuMacroStopRecording.setEnabled(true);
						jMenuMacroContinueRecording.setEnabled(false);
						}
					}
				}
			else if (source == jMenuMacroContinueRecording) {
				if (DEMacroRecorder.getInstance().isRecording()) {
					JOptionPane.showMessageDialog(mParentFrame, "You are already recording a macro in another window.");
					}
				else {
					DEMacroRecorder.getInstance().continueRecording();
					jMenuMacroStartRecording.setEnabled(false);
					jMenuMacroStopRecording.setEnabled(true);
					jMenuMacroContinueRecording.setEnabled(false);
					}
				}
			else if (source == jMenuMacroStopRecording) {
				if (!DEMacroRecorder.getInstance().isRecording()) {
					JOptionPane.showMessageDialog(mParentFrame, "You are not recording any macro in this window.");
					}
				else {
					DEMacroRecorder.getInstance().stopRecording();
					jMenuMacroStartRecording.setEnabled(true);
					jMenuMacroStopRecording.setEnabled(false);
					jMenuMacroContinueRecording.setEnabled(true);
					}
				}
			else if (source == jMenuHelpAutomaticUpdateCheck) {
				Preferences prefs = Preferences.userRoot().node(DataWarrior.PREFERENCES_ROOT);
				prefs.putBoolean(DataWarrior.PREFERENCES_KEY_AUTO_UPDATE_CHECK, jMenuHelpAutomaticUpdateCheck.isSelected());
				}
			else if (source == jMenuHelpCheckForUpdate) {
				mApplication.checkVersion(true);
				}
			else if (actionCommand.startsWith(SET_RANGE)) {
				int column = mTableModel.findColumn(actionCommand.substring(SET_RANGE.length()));
				new DETaskSetValueRange(mParentFrame, column).defineAndRun();
				}
			else if (DescriptorHelper.isDescriptorShortName(actionCommand)) {
				new DETaskCalculateDescriptor(mParentFrame, actionCommand).defineAndRun();
				}
			else
				JOptionPane.showMessageDialog(mParentFrame, "This option is not supported yet.");
			}
		catch (OutOfMemoryError ex) {
			JOptionPane.showMessageDialog(mParentFrame, ex);
			}
		}

	private boolean checkAndAllowEmptyList(int listMode) {
		boolean isEmpty = (listMode == DETaskNewRowList.MODE_SELECTED) ?
				mMainPane.getTable().getSelectionModel().isSelectionEmpty()
						: (listMode == DETaskNewRowList.MODE_VISIBLE) ?
				(mTableModel.getRowCount() == 0)
						: (listMode == DETaskNewRowList.MODE_HIDDEN) ?
				(mTableModel.getRowCount() == mTableModel.getTotalRowCount())
						: false;	// should not happen

		if (!isEmpty)
			return true;

		String message = (listMode == DETaskNewRowList.MODE_SELECTED) ?
				"The selection is empty."
						: (listMode == DETaskNewRowList.MODE_VISIBLE) ?
				"There are no visible rows."
						:
				"There are now hidden rows.";

		int doDelete = JOptionPane.showConfirmDialog(mParentFrame,
						message+"\nDo you really want to create an empty list?",
						"Create Empty List?",
						JOptionPane.OK_CANCEL_OPTION);
		return (doDelete == JOptionPane.OK_OPTION);
		}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JCheckBoxMenuItem) {
			JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem)e.getSource();
			int column = mTableModel.findColumn(menuItem.getText());
			mTableModel.setLogarithmicViewMode(column, menuItem.isSelected());
			}
		}

	private int getStructureColumn(boolean requireFingerprint) {
		int idcodeColumn = -1;

		int[] idcodeColumnList = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumnList == null) {
			JOptionPane.showMessageDialog(mParentFrame, "None of your columns contains chemical structures.");
			}
		else if (idcodeColumnList.length == 1) {
			idcodeColumn = idcodeColumnList[0];
			}
		else {
			String[] columnNameList = new String[idcodeColumnList.length];
			for (int i=0; i<idcodeColumnList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(idcodeColumnList[i]);

			String columnName = (String)JOptionPane.showInputDialog(mParentFrame,
								"Please select a column with chemical structures!",
								"Select Structure Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			idcodeColumn = mTableModel.findColumn(columnName);
			}

		if (idcodeColumn != -1 && requireFingerprint) {
			int fingerprintColumn = mTableModel.getChildColumn(idcodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			if (fingerprintColumn == -1) {
				JOptionPane.showMessageDialog(mParentFrame, "Please create first a chemical fingerprint for the selected structure column.");
				idcodeColumn = -1;
				}
			if (!mTableModel.isDescriptorAvailable(fingerprintColumn)) {
				JOptionPane.showMessageDialog(mParentFrame, "Please wait until the chemical fingerprint creation is completed.");
				idcodeColumn = -1;
				}
			}
		
		return idcodeColumn;
		}

	private int getReactionColumn() {
		int reactionColumn = -1;

		int[] reactionColumnList = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeRXNCode);
		if (reactionColumnList == null) {
			JOptionPane.showMessageDialog(mParentFrame, "None of your columns contains chemical reaction.");
			}
		else if (reactionColumnList.length == 1) {
			reactionColumn = reactionColumnList[0];
			}
		else {
			String[] columnNameList = new String[reactionColumnList.length];
			for (int i=0; i<reactionColumnList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(reactionColumnList[i]);

			String columnName = (String)JOptionPane.showInputDialog(mParentFrame,
								"Please select a column with chemical reactions!",
								"Select Reaction Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			reactionColumn = mTableModel.findColumn(columnName);
			}

		return reactionColumn;
		}

	private void addResourceFileMenus(JMenu parentMenu) {
		// alternative to get location of datawarrior.jar:
		//   getClass().getProtectionDomain().getCodeSource().getLocation();

		for (String resDir:DETaskAbstractOpenFile.RESOURCE_DIR) {
			File directory = DETaskAbstractOpenFile.resolveResourcePath(resDir);
			if (directory.exists())
				addResourceFileMenu(parentMenu, "Open "+resDir+" File", DETaskAbstractOpenFile.makePathVariable(resDir), directory);
			}

		String dirlist = System.getProperty("datapath");
		while (dirlist != null) {
			int index = dirlist.indexOf(File.pathSeparatorChar);
			String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
			dirlist = (index == -1) ? null : dirlist.substring(index+1);
			File directory = new File(DataWarrior.resolveVariables(dirname));
			if (directory.exists())
				addResourceFileMenu(parentMenu, "Open User File <"+directory.getName()+">", dirname, directory);
			}
		}

	/**
	 * @param parentMenu
	 * @param itemString
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param directory
	 */
	private void addResourceFileMenu(JMenu parentMenu, String itemString, String dirPath, File directory) {
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory())
					return false;
				return (file.getName().toLowerCase().endsWith(".dwar"));
				}
			};
		File[] file = directory.listFiles(filter);
		if (file != null && file.length != 0) {
			JMenu menu = new JMenu(itemString);
			Arrays.sort(file);
			for (int i=0; i<file.length; i++) {
				addMenuItem(menu, file[i].getName(), OPEN_FILE+dirPath+File.separator+file[i].getName());
				}
			parentMenu.add(menu);
			}
		}

	private void addMacroFileItems(JMenu parentMenu) {
		File directory = DETaskAbstractOpenFile.resolveResourcePath(DETaskAbstractOpenFile.MACRO_DIR);
		if (directory.exists())
			addMacroFileItems(parentMenu, DETaskAbstractOpenFile.makePathVariable(DETaskAbstractOpenFile.MACRO_DIR), directory);

		String dirlist = System.getProperty("macropath");
		while (dirlist != null) {
			int index = dirlist.indexOf(File.pathSeparatorChar);
			String dirname = (index == -1) ? dirlist : dirlist.substring(0, index);
			dirlist = (index == -1) ? null : dirlist.substring(index+1);
			directory = new File(DataWarrior.resolveVariables(dirname));
			if (directory.exists())
				addMacroFileItems(parentMenu, dirname, directory);
			}

		if (parentMenu.getItemCount() == 0) {
			JMenuItem item = new JMenuItem("<no macros defined>");
			item.setEnabled(false);
			parentMenu.add(item);
			}
		}

	/**
	 * @param parentMenu
	 * @param dirPath should be based on a path variable if it refers to a standard resource file
	 * @param directory
	 */
	private void addMacroFileItems(JMenu parentMenu, String dirPath, File directory) {
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				if (file.isDirectory())
					return false;
				return (file.getName().toLowerCase().endsWith(".dwam"));
				}
			};
		File[] file = directory.listFiles(filter);
		if (file != null && file.length != 0) {
			if (parentMenu.getItemCount() != 0)
				parentMenu.addSeparator();
			Arrays.sort(file);
			for (int i=0; i<file.length; i++) {
				addMenuItem(parentMenu, file[i].getName(), RUN_GLOBAL_MACRO+dirPath+File.separator+file[i].getName());
				}
			}
		}
	}
