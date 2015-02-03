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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.view.DETaskAssignOrZoomAxes;
import com.actelion.research.datawarrior.task.view.DETaskCloseView;
import com.actelion.research.datawarrior.task.view.DETaskCopyStatisticalValues;
import com.actelion.research.datawarrior.task.view.DETaskCopyView;
import com.actelion.research.datawarrior.task.view.DETaskDuplicateView;
import com.actelion.research.datawarrior.task.view.DETaskNewView;
import com.actelion.research.datawarrior.task.view.DETaskRelocateView;
import com.actelion.research.datawarrior.task.view.DETaskRenameView;
import com.actelion.research.datawarrior.task.view.DETaskSelectView;
import com.actelion.research.datawarrior.task.view.DETaskSynchronizeView;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.dock.JDockingPanel;
import com.actelion.research.gui.dock.PopupProvider;
import com.actelion.research.table.CompoundListSelectionModel;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableHitlistListener;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.ExplanationView;
import com.actelion.research.table.view.JStructureGrid;
import com.actelion.research.table.view.VisualizationEvent;
import com.actelion.research.table.view.VisualizationListener;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.table.view.VisualizationPanel3D;
import com.hexidec.ekit.Ekit;

public class DEMainPane extends JDockingPanel
		implements CompoundTableListener,CompoundTableHitlistListener,PopupProvider,ListSelectionListener,VisualizationListener {
	private static final long serialVersionUID = 0x20060904;

	public static final String[] VIEW_TYPE_ITEM = {"2D-View", "3D-View", "Structure View", "Form View", "Text View", "Macro Editor"};
	public static final String[] VIEW_TYPE_CODE = {"2D", "3D", "structure", "form", "text", "macro"};
	public static final int VIEW_TYPE_2D = 0;
	public static final int VIEW_TYPE_3D = 1;
	public static final int VIEW_TYPE_STRUCTURE = 2;
	public static final int VIEW_TYPE_FORM = 3;
	public static final int VIEW_TYPE_TEXT = 4;
	public static final int VIEW_TYPE_MACRO_EDITOR = 5;
	public static final Dimension MINIMUM_SIZE = new Dimension(128, 128);
	public static final Dimension MINIMUM_VIEW_SIZE = new Dimension(64, 64);

	private static final String COMMAND_DUPLICATE = "dup_";
	private static final String COMMAND_RENAME = "rename_";
	private static final String COMMAND_COPY_VIEW = "copyView_";
	private static final String COMMAND_COPY_STATISTICS = "copyStat_";
	private static final String ITEM_NEW_MACRO_EDITOR = "New Macro Editor";
	private static final String ITEM_NEW_EXPLANATION_VIEW = "New Explanation View";

	private Frame						mParentFrame;
	private ApplicationViewFactory		mAppViewFactory;	// creates views that are not supported by the DataWarriorApplet
	private DECompoundTableModel		mTableModel;
	private CompoundListSelectionModel	mListSelectionModel;
	private DEParentPane				mParentPane;
	private DEDetailPane				mDetailPane;
	private DEStatusPanel				mStatusPanel;
	private CompoundTableColorHandler	mColorHandler;

	public DEMainPane(Frame parent,
					  DECompoundTableModel tableModel,
					  DEDetailPane detailPane,
					  DEStatusPanel statusPanel,
					  DEParentPane mainFrame) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mTableModel.addCompoundTableListener(this);
		mTableModel.getHitlistHandler().addCompoundTableHitlistListener(this);
		mDetailPane = detailPane;
		mStatusPanel = statusPanel;
		mParentPane = mainFrame;

		mColorHandler = new CompoundTableColorHandler(mTableModel);
		mDetailPane.setColorHandler(mColorHandler);

		setMinimumSize(MINIMUM_SIZE);
		setPreferredSize(MINIMUM_SIZE);
		setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 0));
		setToolTipText("");

		addPopupListener();

		mListSelectionModel = new CompoundListSelectionModel(mTableModel);
		mListSelectionModel.addListSelectionListener(this);
		mListSelectionModel.addListSelectionListener(statusPanel);
		}

	public void setApplicationViewFactory(ApplicationViewFactory factory) {
		mAppViewFactory = factory;
		}

	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			CompoundListSelectionModel selectionModel = (CompoundListSelectionModel)e.getSource();
			if (selectionModel.getSelectionCount() == 1)
				mDetailPane.highlightChanged(mTableModel.getRecord(selectionModel.getMinSelectionIndex()));
			}
		}

	public DECompoundTableModel getTableModel() {
		return mTableModel;
		}

	public DETableView getTableView() {
		Dockable selectedDockable = getSelectedDockable();
		if (selectedDockable != null && selectedDockable.getContent() instanceof DETableView)
			return (DETableView)selectedDockable.getContent();

		for (Dockable dockable:getDockables())
			if (dockable.getContent() instanceof DETableView)
				return (DETableView)dockable.getContent();

		return null;
		}

	private void addPopupListener() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				handlePopupTrigger(e);
				}

			@Override
			public void mouseReleased(MouseEvent e) {
				handlePopupTrigger(e);
				}
			} );
		}

	private void handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger() && getDockableCount() == 0) {
			JPopupMenu popup = new JPopupMenu();
			
			JMenuItem item1 = new JMenuItem(ITEM_NEW_EXPLANATION_VIEW);
	        item1.addActionListener(this);
	        popup.add(item1);
			JMenuItem item2 = new JMenuItem(ITEM_NEW_MACRO_EDITOR);
	        item2.addActionListener(this);
	        popup.add(item2);

	        popup.show(this, e.getX(), e.getY());
			}
		}

	private boolean hasMacroEditorView() {
		for (Dockable dockable:getDockables())
			if (dockable.getContent() instanceof DEMacroEditor)
				return true;

		return false;
		}

	public DETable getTable() {
		DETableView tv = getTableView();
		return (tv == null) ? null : tv.getTable();
		}

	public void resetAllFilters() {
		for (Dockable dockable:getDockables())
			if (dockable.getContent() instanceof VisualizationPanel)
				((VisualizationPanel)dockable.getContent()).resetAllFilters();
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cAddColumns) {
			for (int column=e.getSpecifier(); column<mTableModel.getTotalColumnCount(); column++) {
				if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))
				 && mTableModel.getColumnTitleNoAlias(column).equals("Structure")) {
					addStructureView("Structures", getSelectedViewTitle()+"\tcenter", column);
					}
				}
			}
		if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMap = e.getMapping();
			ArrayList<Dockable> removalList = new ArrayList<Dockable>();
			for (Dockable dockable:getDockables()) {
				CompoundTableView view = (CompoundTableView)dockable.getContent();
				if (view instanceof JStructureGrid
				 && columnMap[((JStructureGrid)view).getStructureColumn()] == -1)
					removalList.add(dockable);
				}
			for (Dockable dockable:removalList)
				removeView(dockable);
			}
		if (e.getType() == CompoundTableEvent.cNewTable) {
			removeAllViews();
			addTableView("Table", "root");
			if (e.getSpecifier() == CompoundTableEvent.cSpecifierDefaultRuntimeProperties) {
				add2DView("2D View", "Table\tbottom").setDefaultColumns();
				add3DView("3D View", "2D View\tright").setDefaultColumns();
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))) {
						String title = mTableModel.getColumnTitleNoAlias(column).equals("Structure") ?
								"Structures" : "Structures of "+mTableModel.getColumnTitle(column);
						addStructureView(title, "Table\tright", column);
						break;
						}
					}
				}
			}

		for (Dockable dockable:getDockables())
			((CompoundTableView)dockable.getContent()).compoundTableChanged(e);

		mColorHandler.compoundTableChanged(e);

		updateStatusPanel();
		}

	public void hitlistChanged(CompoundTableHitlistEvent e) {
		for (Dockable dockable:getDockables())
			((CompoundTableView)dockable.getContent()).hitlistChanged(e);

		mColorHandler.hitlistChanged(e);
		}

	@Override
	public void visualizationChanged(VisualizationEvent e) {
		if (DEMacroRecorder.getInstance().isRecording()) {
			DETaskAssignOrZoomAxes task = new DETaskAssignOrZoomAxes(mParentFrame, this, e.getSource());
			DEMacroRecorder.record(task, task.getRecentConfiguration() /* this is the view configuration */);
			}
		}

	public DEProgressPanel getMacroProgressPanel() {
		return mStatusPanel.getMacroProgressPanel();
		}

	private void updateStatusPanel() {
		mStatusPanel.setNoOfRecords(mTableModel.getTotalRowCount());
		mStatusPanel.setNoOfVisible(mTableModel.getRowCount());
		}

	public JPopupMenu createPopupMenu(String title) {
		JPopupMenu popup = new JPopupMenu();

		// Currently there is always exactly one TableView.
		//	  addPopupItem(popup, "New Table-View", "newTable_"+title);

		addPopupItem(popup, "New 2D-View", "new2D");
		addPopupItem(popup, "New 3D-View", "new3D");
		addPopupItem(popup, "New Form View", "newForm");
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))) {
				addPopupItem(popup, "New Structure View", "newSGrid");
				break;
				}
			}
	   	addPopupItem(popup, "New Explanation View", "newExplanation");
//		if (System.getProperty("development") != null || System.getProperty("user.name").toLowerCase().equals("giraudi")) {
		   	if (mAppViewFactory != null && !hasMacroEditorView())
		   		addPopupItem(popup, "New Macro Editor", "newMacro");
//			}

		popup.addSeparator();

		if (isDuplicatableView((CompoundTableView)getDockable(title).getContent()))
			addPopupItem(popup, "Duplicate View", COMMAND_DUPLICATE+title);

		addPopupItem(popup, "Rename View...", COMMAND_RENAME+title);

		Dockable thisDockable = getDockable(title);
		if (thisDockable.getContent() instanceof VisualizationPanel) {
			popup.addSeparator();
			addPopupItem(popup, "Copy View Image...", COMMAND_COPY_VIEW+title);
			addPopupItem(popup, "Copy Statistical Values", COMMAND_COPY_STATISTICS+title);

			VisualizationPanel thisVP = (VisualizationPanel)thisDockable.getContent();
			JMenu menuSynchronize = null;
			ButtonGroup groupSynchronize = null;
			for (Dockable dockable:getDockables()) {
				if (dockable != thisDockable && dockable.getContent() instanceof VisualizationPanel) {
					VisualizationPanel otherVP = (VisualizationPanel)dockable.getContent();
					if (otherVP.getSynchronizationMaster() == null && thisVP.getDimensionCount() <= otherVP.getDimensionCount()) {
						if (menuSynchronize == null) {
							menuSynchronize = new JMenu("Synchronize View To ");
							groupSynchronize = new ButtonGroup();
							addRadioButtonItem(menuSynchronize, groupSynchronize, "<none>",
											   "synchronizeToNone",
											   thisVP.getSynchronizationMaster() == null);
							}
	
						addRadioButtonItem(menuSynchronize, groupSynchronize, dockable.getTitle(),
										   "synchronizeTo_"+dockable.getTitle(),
										   thisVP.getSynchronizationMaster() == otherVP);
						}
					}
				}
			if (menuSynchronize != null) {
				popup.addSeparator();
				popup.add(menuSynchronize);
				}
			}

		if (thisDockable.getContent() instanceof JStructureGrid) {
			popup.addSeparator();
			addPopupItem(popup, "Copy View Image...", COMMAND_COPY_VIEW+title);
			}

		if (getDockable(title).getContent() instanceof DEFormView) {
			popup.addSeparator();
			addPopupItem(popup, "Copy View Image...", COMMAND_COPY_VIEW+title);

			popup.addSeparator();

			boolean isDesignMode = ((DEFormView)getDockable(title).getContent()).isDesignMode();
			JCheckBoxMenuItem item1 = new JCheckBoxMenuItem("Design Mode", isDesignMode);
			item1.addActionListener(this);
			popup.add(item1);

			boolean isEditMode = ((DEFormView)getDockable(title).getContent()).isEditMode();
			JCheckBoxMenuItem item2 = new JCheckBoxMenuItem("Edit Mode", isEditMode);
			item2.addActionListener(this);
			popup.add(item2);
			}

		if (getDockable(title).getContent() instanceof ExplanationView) {
			popup.addSeparator();

			addPopupItem(popup, "Edit...", "edit");
			}

		return popup;
		}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		String viewName = (command.indexOf("_") == -1) ? null : command.substring(command.indexOf("_")+1);
		CompoundTableView view = getView(viewName);
		if (command.equals(ITEM_NEW_EXPLANATION_VIEW)) {
			addExplanationView(getDefaultViewName(VIEW_TYPE_TEXT, -1), "root");
			}
		if (command.equals(ITEM_NEW_MACRO_EDITOR)) {
			addApplicationView(VIEW_TYPE_MACRO_EDITOR, getDefaultViewName(VIEW_TYPE_MACRO_EDITOR, -1), "root");
			}
		else if (command.startsWith("popup_")) {
			VisualizationPanel panel = (VisualizationPanel)view;
			panel.showControls();
			}
		else if (command.startsWith("close_")) {
			new DETaskCloseView(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith("selected_")) {
			new DETaskSelectView(mParentFrame, this, view).defineAndRun();
			}
		else if (e.getActionCommand().startsWith("max_")) {
			if (!isMaximized()) {	// we are maximizing rather than de-maximizing
				Dockable maximizingDockable = getDockable(e.getActionCommand().substring(4));
				for (Dockable d:getDockables())
					if (d != maximizingDockable && d.getContent() instanceof VisualizationPanel)
					 	((VisualizationPanel)d.getContent()).hideControls();
				}
			super.actionPerformed(e);
			}
		else if (command.equals("Design Mode")) {
			DEFormView formView = (DEFormView)getSelectedDockable().getContent();
			boolean mode = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			if (formView.isEditMode())
				formView.setEditMode(false);
			formView.setDesignMode(mode);
			}
		else if (command.equals("Edit Mode")) {
			DEFormView formView = (DEFormView)getSelectedDockable().getContent();
			boolean mode = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			if (formView.isDesignMode())
				formView.setDesignMode(false);
			formView.setEditMode(mode);
			}
		else if (command.equals("edit")) {
			final ExplanationView explanationView = (ExplanationView)getSelectedDockable().getContent();
			Ekit ekit = new Ekit(mParentFrame) {
				private static final long serialVersionUID = 20131128L;

				@Override
				public void windowClosing(WindowEvent we) {
					String text = getEkitCore().getDocumentText();
					mTableModel.setExtensionData(CompoundTableModel.cExtensionNameFileExplanation, text);
//					explanationView.setText();
					setVisible(false);
					dispose();
					}
				};
			ekit.getEkitCore().setDocumentText(explanationView.getText());
			}
		else if (command.startsWith(COMMAND_DUPLICATE)) {
			new DETaskDuplicateView(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_RENAME)) {
			new DETaskRenameView(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_COPY_VIEW)) {
			new DETaskCopyView(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_COPY_STATISTICS)) {
			new DETaskCopyStatisticalValues(mParentFrame, this, view).defineAndRun();
			}
		else if (command.equals("synchronizeToNone")) {
			new DETaskSynchronizeView(mParentFrame, this, getSelectedDockable().getTitle(), null).defineAndRun();
			}
		else if (command.startsWith("synchronizeTo_")) {
			String slave = getSelectedDockable().getTitle();
			new DETaskSynchronizeView(mParentFrame, this, slave, viewName).defineAndRun();
			}
		else if (command.equals("new2D")) {
			new DETaskNewView(mParentFrame, this, VIEW_TYPE_2D, getSelectedDockable().getTitle(), -1).defineAndRun();
			}
		else if (command.equals("new3D")) {
			new DETaskNewView(mParentFrame, this, VIEW_TYPE_3D, getSelectedDockable().getTitle(), -1).defineAndRun();
			}
		else if (command.equals("newSGrid")) {
			int column = selectStructureColumn();
			if (column != -1)
				new DETaskNewView(mParentFrame, this, VIEW_TYPE_STRUCTURE, getSelectedDockable().getTitle(), column).defineAndRun();
			}
		else if (command.equals("newForm")) {
			new DETaskNewView(mParentFrame, this, VIEW_TYPE_FORM, getSelectedDockable().getTitle(), -1).defineAndRun();
			}
		else if (command.equals("newExplanation")) {
			new DETaskNewView(mParentFrame, this, VIEW_TYPE_TEXT, getSelectedDockable().getTitle(), -1).defineAndRun();
			}
		else if (command.equals("newMacro")) {
			new DETaskNewView(mParentFrame, this, VIEW_TYPE_MACRO_EDITOR, getSelectedDockable().getTitle(), -1).defineAndRun();
			}
		}

	public void renameView(String oldTitle, String newTitle) {
		if (newTitle != null && newTitle.length() != 0 && !newTitle.equals(oldTitle)) {
			changeTitle(oldTitle, newTitle);
			mParentPane.fireRuntimePropertyChanged(
					new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_RENAME_VIEW, -1));
			}
		}

	public void synchronizeView(VisualizationPanel vp, VisualizationPanel master) {
		if (master == null) {
			vp.setSynchronizationMaster(null);
			}
		else {
			// make master the new master of all panels currently controlled by vp including vp itself
			ArrayList<VisualizationPanel> childList = new ArrayList<VisualizationPanel>(vp.getSynchronizationChildList());
			childList.add(vp);
			for (VisualizationPanel child:childList)
				child.setSynchronizationMaster(master);
			}
		mParentPane.fireRuntimePropertyChanged(
				new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_SYNCHRONIZE_VIEW, -1));
		}

	/**
	 * Adds a new view with default name on top of another view
	 * @param type
	 * @param whereViewTitle
	 * @param structureColumn
	 */
	public void createNewView(int type, String whereViewTitle, int structureColumn) {
		createNewView(null, type, whereViewTitle, "center", structureColumn);
		}

	/**
	 * Adds a new view relative to another view optionally defining the divider position
	 * @param viewName null for default name
	 * @param type
	 * @param whereViewTitle
	 * @param whereLocation relation[\tdividerposition]
	 * @param structureColumn
	 */
	public void createNewView(String viewName, int type, String whereViewTitle, String whereLocation, int structureColumn) {
		String dockInfo = whereViewTitle + "\t" + whereLocation;
		if (viewName == null)
			viewName = getDefaultViewName(type, structureColumn);
		switch (type) {
		case VIEW_TYPE_2D:
			add2DView(viewName, dockInfo).setDefaultColumns();
			break;
		case VIEW_TYPE_3D:
			add3DView(viewName, dockInfo).setDefaultColumns();
			break;
		case VIEW_TYPE_STRUCTURE:
			addStructureView(viewName, dockInfo, structureColumn);
			break;
		case VIEW_TYPE_FORM:
			addFormView(viewName, dockInfo, true);
			break;
		case VIEW_TYPE_TEXT:
			addExplanationView(viewName, dockInfo);
			break;
		default:
			addApplicationView(type, viewName, dockInfo);
			break;
			}
		mParentPane.fireRuntimePropertyChanged(
					new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_ADD_VIEW, -1));
		}

	public String getDefaultViewName(int viewType, int structureColumn) {
		switch (viewType) {
		case VIEW_TYPE_2D:
			return "2D View";
		case VIEW_TYPE_3D:
			return "3D View";
		case VIEW_TYPE_STRUCTURE:
			return (structureColumn == -1 || mTableModel.getColumnTitle(structureColumn).equals("Structure")) ? 
						"Structures" : mTableModel.getColumnTitle(structureColumn);
		case VIEW_TYPE_FORM:
			return "Form View";
		case VIEW_TYPE_TEXT:
			return "Explanation";
		case VIEW_TYPE_MACRO_EDITOR:
			return "Macro Editor";
		default:
			return "Unknown View";
			}
		}

	public boolean hasDuplicatableView() {
		for (Dockable d:getDockables())
			if (isDuplicatableView((CompoundTableView)d.getContent()))
				return true;

		return false;
		}

	public boolean isDuplicatableView(CompoundTableView view) {
		return view instanceof DEFormView
			|| view instanceof VisualizationPanel2D
			|| view instanceof VisualizationPanel3D
			|| view instanceof JStructureGrid;
		}

	/**
	 * @param title the title of an existing view
	 */
	public void closeView(String title) {
		((CompoundTableView)getDockable(title).getContent()).cleanup();
		undock(title, false);

		mParentPane.fireRuntimePropertyChanged(
				new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_REMOVE_VIEW, -1));
		}

	/**
	 * Duplicates a view with all properties and docks it on top of the copied view.
	 * @param title of the existing view to be copied
	 */
	public void duplicateView(String title) {
		duplicateView(title, "Copy of "+title, title+"\tcenter");
		}

	/**
	 * Duplicates a view with all properties and docks it into the tree of views.
	 * @param title of the existing view to be copied
	 * @param newTitle title of the new view
	 * @param position e.g. "someViewTitle\tright\t0.25" or "otherViewTitle\tcenter"
	 */
	public void duplicateView(String title, String newTitle, String position) {
			// change view's name temporarily and learn view's properties
		DERuntimeProperties properties = new DERuntimeProperties(mParentPane);
		properties.learn();
	
		Component view = getSelectedDockable().getContent();
		CompoundTableView newView = null;
		if (view instanceof DEFormView)
			newView = addFormView(newTitle, title+"\tcenter", true);
		else if (view instanceof VisualizationPanel2D)
			newView = add2DView(newTitle, title+"\tcenter");
		else if (view instanceof VisualizationPanel3D)
			newView = add3DView(newTitle, title+"\tcenter");
		else if (view instanceof JStructureGrid)
			newView = addStructureView(newTitle, title+"\tcenter", ((JStructureGrid)view).getStructureColumn());
		properties.applyViewProperties(newView, "_"+title);
	
		mParentPane.fireRuntimePropertyChanged(
				new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_ADD_VIEW, -1));
		}

	private void addPopupItem(JPopupMenu popup, String itemText, String command) {
		JMenuItem item = new JMenuItem(itemText);
		item.setActionCommand(command);
		item.addActionListener(this);
		popup.add(item);
		}

	private void addRadioButtonItem(JMenu menu, ButtonGroup group, String text, String command, boolean isSelected) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(text);
		item.setSelected(isSelected);
		item.setActionCommand(command);
		item.addActionListener(this);
		group.add(item);
		menu.add(item);
		}

	public DETableView addTableView(String title, String dockInfo) {
		DETableView tableView = new DETableView(mParentFrame, mParentPane, mTableModel, mColorHandler, mListSelectionModel);
		tableView.setDetailPopupProvider(mParentPane);
		Dockable dockable = new Dockable(tableView, validateTitle(title), this, false);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dockable.setPopupProvider(this);
		dock(dockable, dockInfo);
		return tableView;
		}

	public JStructureGrid addStructureView(String title, String dockInfo, int column) {
		JStructureGrid structureGrid = new JStructureGrid(mParentFrame, mTableModel, mColorHandler, mListSelectionModel, column, 6);
		structureGrid.setDetailPopupProvider(mParentPane);
		Dockable dockable = new Dockable(structureGrid, validateTitle(title), this, true);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dockable.setPopupProvider(this);
		dock(dockable, dockInfo);
		return structureGrid;
		}

	/**
	 * Adds a view of a type that is not known to DEMainPane, e.g. if it is not supported in applets.
	 * @param type e.g. VIEW_TYPE_MACRO_EDITOR
	 * @param title
	 * @param dockInfo
	 * @return requested view or minimum view showing error message
	 */
	public CompoundTableView addApplicationView(int type, String title, String dockInfo) {
		CompoundTableView view = (mAppViewFactory == null) ? null : mAppViewFactory.createApplicationView(type, mParentFrame);
		if (view == null)
			view = new ErrorView("View type not supported!");

		Dockable dockable = new Dockable((Component)view, validateTitle(title), this, true);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dockable.setPopupProvider(this);
		dock(dockable, dockInfo);
		return view;
		}

	public ExplanationView addExplanationView(String title, String dockInfo) {
		ExplanationView view = new ExplanationView(mTableModel);
		Dockable dockable = new Dockable(view, validateTitle(title), this, true);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dockable.setPopupProvider(this);
		dock(dockable, dockInfo);
		return view;
		}

	public VisualizationPanel2D add2DView(String title, String dockInfo) {
		try {
			VisualizationPanel2D panel2D = new VisualizationPanel2D(mParentFrame, mTableModel, mListSelectionModel);
			panel2D.getVisualization().setDetailPopupProvider(mParentPane);
			panel2D.addVisualizationListener(this);
			Dockable dockable = new Dockable(panel2D, validateTitle(title), this, true, true);
			dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
			dockable.setPopupProvider(this);
			dock(dockable, dockInfo);
			return panel2D;
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParentFrame, e.toString());
			e.printStackTrace();
			}
		return null;
		}

	public VisualizationPanel3D add3DView(String title, String dockInfo) {
		VisualizationPanel3D panel3D = new VisualizationPanel3D(mParentFrame, mTableModel, mListSelectionModel);
		panel3D.getVisualization().setDetailPopupProvider(mParentPane);
		panel3D.addVisualizationListener(this);
		Dockable dockable = new Dockable(panel3D, validateTitle(title), this, true, true);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dockable.setPopupProvider(this);
		dock(dockable, dockInfo);
		return panel3D;
		}

	public DEFormView addFormView(String title, String dockInfo, boolean createDefaultLayout) {
		DEFormView form = new DEFormView(mParentFrame, mTableModel, mColorHandler);
		form.setDetailPopupProvider(mParentPane);
		if (createDefaultLayout)
			form.createDefaultLayout();
		Dockable dockable = new Dockable(form, validateTitle(title), this, true);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dockable.setPopupProvider(this);
		dock(dockable, dockInfo);
		return form;
		}

	public void removeView(String title) {
		removeView(getDockable(title));
		}

	public void removeView(Dockable dockable) {
		((CompoundTableView)dockable.getContent()).cleanup();;
		undock(dockable.getTitle());
		}

	public void removeAllViews() {
		for (Dockable dockable:getDockables())
			((CompoundTableView)dockable.getContent()).cleanup();
		undockAll();
		}

	public CompoundTableView getView(String name) {
		if (name == null)
			return null;
		Dockable d = getDockable(name);
		return (d == null) ? null : (CompoundTableView)d.getContent();
		}

	public String getViewTitle(CompoundTableView view) {
		if (view == null)
			return null;
		for (Dockable dockable:getDockables())
			if ((CompoundTableView)dockable.getContent() == view)
				return getTitle(dockable);
		return null;
		}

	public String getSelectedViewTitle() {
		return getSelectedDockable().getTitle();
		}

	public CompoundTableView getSelectedView() {
		return (CompoundTableView)getSelectedDockable().getContent();
		}

	public void setSelectedView(String uniqueID) {
		selectDockable(getDockable(uniqueID));
		}

	@Override
	public void relocateView(String movedDockableName, String targetDockableName, int targetPosition) {
		new DETaskRelocateView(mParentFrame, this, getView(movedDockableName), targetDockableName, targetPosition).defineAndRun();
		}

	public void doRelocateView(String movedDockableName, String targetDockableName, int targetPosition) {
		super.relocateView(movedDockableName, targetDockableName, targetPosition);
		}

	public void visibilityChanged(Dockable dockable, boolean isVisible) {
		super.visibilityChanged(dockable, isVisible);

		Component view = dockable.getContent();
		if (view instanceof VisualizationPanel) {
			VisualizationPanel vp = (VisualizationPanel)view;
	   		vp.getVisualization().setSuspendGlobalExclusion(!isVisible);
	   		if (!isVisible)
	   			vp.hideControls();
			}
		}

	public String validateTitle(String name) {
		Set<String> titleSet = getDockableTitles();
		if (titleSet.contains(name)) {
			int suffix=2;
			while (true) {
				String newName = name+"_"+(suffix++);
				if (!titleSet.contains(newName))
					return newName;
				}
			}
		return name;
		}

	private int selectStructureColumn() {
		int column = -1;
		ArrayList<String> structureColumnList = null;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
			if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(i))) {
				if (column == -1)
					column = i;
				else if (structureColumnList == null) {
					structureColumnList = new ArrayList<String>();
					structureColumnList.add(mTableModel.getColumnTitle(column));
					structureColumnList.add(mTableModel.getColumnTitle(i));
					}
				else {
					structureColumnList.add(mTableModel.getColumnTitle(i));
					}
				}
			}
		if (structureColumnList != null) {
			String option = (String)JOptionPane.showInputDialog(mParentFrame,
					"Please select a column with chemical structures!",
					"Select Structure Column",
					JOptionPane.QUESTION_MESSAGE,
					null,
					structureColumnList.toArray(),
					structureColumnList.get(0));
			column = mTableModel.findColumn(option);
			}
		return column;
		}
	}

class ErrorView extends JPanel implements CompoundTableView {
	private static final long serialVersionUID = 20131211L;

	String mMessage;

	public ErrorView(String message) {
		mMessage = message;
		}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Dimension size = getSize();
		FontMetrics metrics = g.getFontMetrics();
		g.setColor(Color.RED);
		g.drawString(mMessage, (size.width-metrics.stringWidth(mMessage)) / 2, (size.height+g.getFont().getSize())/2);
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		}

	@Override
	public void hitlistChanged(CompoundTableHitlistEvent e) {
		}

	@Override
	public void cleanup() {
		}

	@Override
	public CompoundTableModel getTableModel() {
		return null;
		}
	}
