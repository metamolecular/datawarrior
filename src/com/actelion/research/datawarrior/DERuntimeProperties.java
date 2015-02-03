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

import javax.swing.SwingUtilities;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.gui.JMultiPanelView;
import com.actelion.research.gui.JPruningBar;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.RuntimeProperties;
import com.actelion.research.table.filter.JCategoryBrowser;
import com.actelion.research.table.filter.JCategoryFilterPanel;
import com.actelion.research.table.filter.JDoubleFilterPanel;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.filter.JHitlistFilterPanel;
import com.actelion.research.table.filter.JMultiStructureFilterPanel;
import com.actelion.research.table.filter.JReactionFilterPanel;
import com.actelion.research.table.filter.JSingleStructureFilterPanel;
import com.actelion.research.table.filter.JStringFilterPanel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.ExplanationView;
import com.actelion.research.table.view.FocusableView;
import com.actelion.research.table.view.JCompoundTableForm;
import com.actelion.research.table.view.JStructureGrid;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.JVisualization3D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.table.view.VisualizationPanel3D;
import com.actelion.research.util.DoubleFormat;

public class DERuntimeProperties extends RuntimeProperties {
	private static final long serialVersionUID = 0x20061101;

	private static final String cViewTypeTable = "tableView";
	private static final String cViewType2D = "2Dview";
	private static final String cViewType3D = "3Dview";
	private static final String cViewTypeForm = "formView";
	private static final String cViewTypeStructureGrid = "structureView";
	private static final String cViewTypeExplanation = "explanationView";
	private static final String cViewTypeMacroEditor = "macroEditor";

	private static final String cMainSplitting = "mainSplitting";
	private static final String cRightSplitting = "rightSplitting";
	private static final String cSelectedMainView = "mainView";
	private static final String cMainViewCount = "mainViewCount";
	private static final String cMainViewName = "mainViewName";
	private static final String cMainViewType = "mainViewType";
	private static final String cMainViewDockInfo = "mainViewDockInfo";
	private static final String cDetailView = "detailView";
	private static final String cTableRowHeight = "rowHeight";
	private static final String cTableColumnWidth = "columnWidth";
	private static final String cTableColumnWrapping = "columnWrapping";
	private static final String cTableColumnVisibility = "columnVisibility";
	private static final String cTableColumnOrder = "columnOrder";
	private static final String cTableText = "Text_";				// suffix for cColor???? keys in case of table view column text color
	private static final String cTableBackground = "Background_";	// suffix for cColor???? keys in case of table view column background color
	private static final String cFastRendering = "fastRendering";
	private static final String cViewBackground = "background";
	private static final String cTitleBackground = "titleBackground";
	private static final String cFaceColor3D = "faceColor3D";
	private static final String cAxisColumn = "axisColumn";
	private static final String cAxisLow = "axisLow";
	private static final String cAxisHigh = "axisHigh";
	private static final String cJittering = "jittering";
	private static final String cFocusHitlist = "focusHitlist";
	private static final String cFocusOnSelection = "selection";
	private static final String cMarkerSize = "markersize";
	private static final String cSizeColumn = "sizeColumn";
	private static final String cSizeInversion = "sizeInversion";
	private static final String cSizeProportional = "sizeProportional";
	private static final String cSizeAdaption = "sizeAdaption";
	private static final String cLabelSize = "labelSize";
	private static final String cLabelColumn = "labelColumn";
	private static final String cLabelMode = "labelMode";
	private static final String cColorColumn = "colorColumn";
	private static final String cColorCount = "colorCount";
	private static final String cColor = "color";
	private static final String cDefaultColor = "defaultColor";
	private static final String cMissingColor = "missingColor";
	private static final String cColorMin = "colorMin";
	private static final String cColorMax = "colorMax";
	private static final String cColorListMode = "colorListMode";
	private static final String cBackgroundColorColumn = "backgroundColorColumn";
	private static final String cBackgroundColorCount = "backgroundColorCount";
	private static final String cBackgroundColor = "backgroundColor";
	private static final String cBackgroundColorListMode = "backgroundColorListMode";
	private static final String cBackgroundColorRadius = "backgroundColorRadius";
	private static final String cBackgroundColorFading = "backgroundColorFading";
	private static final String cBackgroundColorRecords = "backgroundColorRecords";
	private static final String cBackgroundColorMin = "backgroundColorMin";
	private static final String cBackgroundColorMax = "backgroundColorMax";
	private static final String cBackgroundImage = "backgroundImage";
	private static final String cSuppressGrid = "suppressGrid";
	private static final String cSuppressScale = "suppressScale";
	private static final String cViewFontSize = "fontSize";
	private static final String cShapeColumn = "shapeColumn";
	private static final String cMarkerTransparency = "markertransparency";
	private static final String cMultiValueMarkerMode = "multiValueMarkerMode";
	private static final String cMultiValueMarkerColumns = "multiValueMarkerColumns";
	private static final String cConnectionColumn1 = "connectionColumn";
	private static final String cConnectionColumn2 = "connectionOrderColumn";
	private static final String cConnectionColumnConnectAll = "<connectAll>";
	private static final String cConnectionColumnConnectCases = "<connectCases>";
	private static final String cConnectionLineWidth = "connectionLineWidth";
	private static final String cTreeViewMode = "treeViewMode";
	private static final String cTreeViewRadius = "treeViewRadius";
	private static final String cTreeViewShowAll = "treeViewShowAll";
	private static final String cSplitViewColumn1 = "splitViewColumn1";
	private static final String cSplitViewColumn2 = "splitViewColumn2";
	private static final String cCaseSeparationColumn = "caseSeparationColumn";
	private static final String cCaseSeparationValue = "caseSeparationValue";
	private static final String cChartType = "chartType";
	private static final String cChartMode = "chartMode";
	private static final String cChartColumn = "chartColumn";
	private static final String cBoxplotMeanMode = "boxplotMeanMode";
	private static final String cBoxplotShowMeanValues = "boxPlotShowMeanValues";
	private static final String cCurveMode = "meanLineMode";
	private static final String cCurveStdDev = "meanLineStdDev";
	private static final String cCurveSplitByCategory = "splitCurveByCategory";
	private static final String cShowPValue = "showPValue";
	private static final String cShowFoldChange = "showFoldChange";
	private static final String cPValueColumn = "pValueColumn";
	private static final String cPValueRefCategory = "pValueRefCategory";
	private static final String cCorrelationCoefficient = "corrCoefficient";
	private static final String cAffectGlobalExclusion = "affectGlobalExclusion";
	private static final String cShowNaNValues = "showNaNValues";
	private static final String cRotation = "rotationMatrix";
	private static final String cMasterView = "masterView";
	private static final String cStructureGridColumn = "structureGridColumn";
	private static final String cStructureGridColumns = "structureGridColumns";
	private static final String cFilter = "filter";
	private static final String cFilterTypeCategoryBrowser = "#browser#";
	private static final String cFilterTypeDouble = "#double#";
	private static final String cFilterTypeCategory = "#category#";
	private static final String cFilterTypeString = "#string#";
	private static final String cFilterTypeHitlist = "#hitlist#";
	private static final String cFilterTypeStructure = "#structure#";
	private static final String cFilterTypeSSSList = "#sssList#";
	private static final String cFilterTypeSIMList = "#simList#";
	private static final String cFilterTypeReaction = "#reaction#";
	private static final String cFilterAnimation = "filterAnimation";
	private static final String cFormLayout = "formLayout";
	private static final String cFormObjectCount = "formObjectCount";
	private static final String cFormObjectInfo = "formObjectInfo";

	private DEParentPane	mParentPane;
	private DEMainPane		mMainPane;
	private DEDetailPane	mDetailPane;
	private DEPruningPanel	mPruningPanel;

	public DERuntimeProperties(DEParentPane parentPane) {
		super(parentPane.getTableModel());
		mParentPane = parentPane;
		mMainPane = parentPane.getMainPane();
		mDetailPane = parentPane.getDetailPane();
		mPruningPanel = parentPane.getPruningPanel();
		}

	public void setParentPane(DEParentPane parentPane) {
		mTableModel = parentPane.getTableModel();
		mParentPane = parentPane;
		mMainPane = parentPane.getMainPane();
		mDetailPane = parentPane.getDetailPane();
		mPruningPanel = parentPane.getPruningPanel();
		}

	public void apply() {
		if (size() == 0)
			return;

		if (SwingUtilities.isEventDispatchThread()) {
			doApply();
			}
		else {
				// if we are not in the event dispatcher thread we need to use invokeAndWait
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						doApply();
						}
					} );
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			}
		}

	protected void doApply() {
		if (size() == 0)
			return;

		mMainPane.removeAllViews();
		mPruningPanel.removeAllFilters();

		super.apply();

		String mainSplitting = getProperty(cMainSplitting);
		if (mainSplitting != null) {
			try {
				mParentPane.setMainSplitting(Double.parseDouble(mainSplitting));
				}
			catch (NumberFormatException e) {}
			}
		String rightSplitting = getProperty(cRightSplitting);
		if (rightSplitting != null) {
			try {
				mParentPane.setRightSplitting(Double.parseDouble(rightSplitting));
				}
			catch (NumberFormatException e) {}
			}

		String viewCountString = getProperty(cMainViewCount);
		if (viewCountString == null) {	// old file with standard views only
			applyViewProperties(mMainPane.addTableView("Table", "root"), "Table");
			applyViewProperties(mMainPane.add2DView("2D View", "Table\tcenter"), "2D");
			applyViewProperties(mMainPane.add3DView("3D View", "Table\tcenter"), "3D");
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))) {
					applyViewProperties(mMainPane.addStructureView("Structures", "Table\tcenter", column), "StructureView");
					break;
					}
				}
			}
		else {
			int viewCount = Integer.parseInt(viewCountString);
			for (int i=0; i<viewCount; i++) {
				String viewType = getProperty(cMainViewType+i);
				String tabName = getProperty(cMainViewName+i);
				String dockInfo = getProperty(cMainViewDockInfo+i);

				if (i == 0) {
					// cMainViewName, cMainViewType and cMainViewDockInfo were not defined
					// for view=0 before V3.0
					if (tabName == null && viewType == null) {
						viewType = cViewTypeTable;
						tabName = "Table";
						}
					if (dockInfo == null) {
						dockInfo = "root";
						}
					}
				else if (dockInfo == null) {
					// cMainViewDockInfo was not defined before V3.1
					String refTitle = getProperty(cMainViewName+0);
					if (refTitle == null)
						refTitle = "Table";
					dockInfo = refTitle + "\tcenter";
					}

				CompoundTableView view = viewType.equals(cViewTypeTable) ? mMainPane.addTableView(tabName, dockInfo)
							   : viewType.equals(cViewType2D) ? mMainPane.add2DView(tabName, dockInfo)
							   : viewType.equals(cViewType3D) ? mMainPane.add3DView(tabName, dockInfo)
							   : viewType.equals(cViewTypeForm) ? mMainPane.addFormView(tabName, dockInfo, false)
							   : viewType.equals(cViewTypeStructureGrid) ? mMainPane.addStructureView(tabName, dockInfo, -1)
							   : viewType.equals(cViewTypeExplanation) ? mMainPane.addExplanationView(tabName, dockInfo)
							   : viewType.equals(cViewTypeMacroEditor) ? mMainPane.addApplicationView(DEMainPane.VIEW_TYPE_MACRO_EDITOR, tabName, dockInfo)
							   : null;
				if (view != null)
					applyViewProperties(view, "_" + tabName);
				}

			for (int i=0; i<viewCount; i++) {
				String viewType = getProperty(cMainViewType+i);
				if (viewType != null	// was not defined for view=0 before V3.0
				 && (viewType.equals(cViewType2D) || viewType.equals(cViewType3D))) {
					String tabName = getProperty(cMainViewName+i);
					String masterView = getProperty(cMasterView+"_"+tabName);
					if (masterView != null)
						((VisualizationPanel)mMainPane.getView(tabName)).setSynchronizationMaster(
								(VisualizationPanel)mMainPane.getView(masterView));
					}
				}
			}

		String property = null;
		for (int i=0; (property=getProperty(cFilter+i))!=null; i++) {

			// Formats prior V2.7.0 didn't store column names for structure filters.
			if (property.equals(cFilterTypeStructure))
				property = cFilterTypeStructure + "\tStructure";

			int index1 = property.indexOf('\t');

			// Formats prior V2.7.0 didn't store column names for structure filters.
			if (property.startsWith(cFilterTypeStructure+"\t#substructure#")
			 || property.startsWith(cFilterTypeStructure+"\t#similarity#")
			 || property.startsWith(cFilterTypeStructure+"\t#inverse#\t#substructure#")
			 || property.startsWith(cFilterTypeStructure+"\t#inverse#\t#similarity#"))
				property = cFilterTypeStructure+"\tStructure"+property.substring(index1);

			JFilterPanel filter = null;
			if (property.startsWith(cFilterTypeCategoryBrowser)) {
				try {
					filter = mPruningPanel.addCategoryBrowser(mTableModel);
					if (index1 != -1)
						filter.applySettings(property.substring(index1+1));
					}
				catch (DEPruningPanel.FilterException fpe) {}
				}
			else if (property.startsWith(cFilterTypeHitlist)) {
				if (mTableModel.getHitlistHandler().getHitlistCount() > 0) {
					try {
						filter = mPruningPanel.addHitlistFilter(mTableModel);
						if (index1 != -1)
							filter.applySettings(property.substring(index1+1));
						}
					catch (DEPruningPanel.FilterException fpe) {}
					}
				}
			else if (index1 != -1) {
				int index2 = property.indexOf('\t', index1+1);
				String columnName = (index2 == -1) ? property.substring(index1+1)
												   : property.substring(index1+1, index2);

				int column = mTableModel.findColumn(columnName);
				if (column != -1) {
					try {
						if (property.startsWith(cFilterTypeDouble))
							filter = mPruningPanel.addDoubleFilter(mTableModel, column);
						else if (property.startsWith(cFilterTypeCategory))
							filter = mPruningPanel.addCategoryFilter(mTableModel, column);
						else if (property.startsWith(cFilterTypeString))
							filter = mPruningPanel.addStringFilter(mTableModel, column);
						else if (property.startsWith(cFilterTypeStructure))
							filter = mPruningPanel.addStructureFilter(mTableModel, column, null);
						else if (property.startsWith(cFilterTypeSSSList))
							filter = mPruningPanel.addStructureListFilter(mTableModel, column, true);
						else if (property.startsWith(cFilterTypeSIMList))
							filter = mPruningPanel.addStructureListFilter(mTableModel, column, false);
						else if (property.startsWith(cFilterTypeReaction))
							filter = mPruningPanel.addReactionFilter(mTableModel, column, null);
						if (filter != null
						 && index2 != -1)
							filter.applySettings(property.substring(index2+1));
						}
					catch (DEPruningPanel.FilterException fpe) {}
					}
				}

			if (filter != null) {
				String settings = getProperty(cFilterAnimation+i);
				if (settings != null)
					filter.applyAnimationSettings(settings);
				}
			}

		String name = getProperty(cSelectedMainView);
		if (name != null)
			mMainPane.setSelectedView(name);

		String detail = getProperty(cDetailView);
		if (detail != null) {
			if (detail.startsWith(JMultiPanelView.VIEW_HEIGHT))
				mDetailPane.setProperties(detail);
			else {		// to be compatible with old JTabbedPane based DEDetailPane
				if (detail.equals("Detail"))
					mDetailPane.setProperties("height[Structure]=0.5;height[Data]=0.5");
				else
					mDetailPane.setProperties("height["+detail+"]=1.0");
				}
			}
		}

	private void applyMarkerLabelDisplayerProperties(String viewName, MarkerLabelDisplayer displayer) {
		int[] columnAtPosition = new int[MarkerLabelDisplayer.cPositionCode.length];
		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++) {
			String columnKey = cLabelColumn+viewName+"_"+MarkerLabelDisplayer.cPositionCode[i];
			String columnName = getProperty(columnKey);

			// for compatibility to older encoding before Sep2013
			if (columnName == null)
				columnName = getProperty(cLabelColumn+viewName+"_"+MarkerLabelDisplayer.cPositionOption[i]);

			columnAtPosition[i] = (columnName == null) ? -1 : mTableModel.findColumn(columnName);
			}

		displayer.setMarkerLabels(columnAtPosition);

		String mode = getProperty(cLabelMode+viewName);
		displayer.setMarkerLabelsInTreeViewOnly("inDetailGraphOnly".equals(mode));

		String size = getProperty(cLabelSize+viewName);
		if (size != null)
			displayer.setMarkerLabelSize(Float.parseFloat(size), false);
		}

	public void applyViewProperties(CompoundTableView view, String viewName) {
		if (view instanceof DETableView) {
			DETable table = ((DETableView)view).getTable();
			String value = getProperty(cTableRowHeight+viewName);
			table.setRowHeight((value == null) ? 16 : Integer.parseInt(value));
			for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) {
				int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
				value = getProperty(cTableColumnWidth+viewName+"_"+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					table.getColumnModel().getColumn(table.convertColumnIndexToView(modelColumn)).setPreferredWidth(Integer.parseInt(value));
				value = getProperty(cTableColumnWrapping+viewName+"_"+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					((DETableView)view).setTextWrapping(column, value.equals("true"));
				value = getProperty(cTableColumnVisibility+viewName+"_"+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					((DETableView)view).setColumnVisibility(column, value.equals("true"));
				value = getProperty(cColorColumn+cTableText+viewName+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					applyViewColorProperties(cTableText+viewName+mTableModel.getColumnTitleNoAlias(column),
							((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND));
				value = getProperty(cColorColumn+cTableBackground+viewName+mTableModel.getColumnTitleNoAlias(column));
				if (value != null)
					applyViewColorProperties(cTableBackground+viewName+mTableModel.getColumnTitleNoAlias(column),
							((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND));
				}
			value = getProperty(cTableColumnOrder+viewName);
			if (value != null)
				table.setColumnOrder((String)value);
			}
		else if (view instanceof VisualizationPanel) {
			VisualizationPanel vpanel = (VisualizationPanel)view;
			JVisualization visualization = vpanel.getVisualization();

			int chartType = JVisualization2D.cChartTypeBars;
			int chartMode = JVisualization2D.cChartModeCount;
			int chartColumn = JVisualization.cColumnUnassigned;

			// for compatibility up to version 3.4.2
			String value = getProperty("preferHistogram"+viewName);

			// for compatibility with version with fixed main views
			if (value == null && viewName.equals("2D"))
				value = getProperty("preferHistogram");

			if (value != null && value.equals("false")) {
				chartType = JVisualization2D.cChartTypeScatterPlot;
				}
			else {	// this is the handling after version 3.5.0
				chartType = decodeProperty(cChartType+viewName, JVisualization.CHART_TYPE_CODE);
				value = getProperty(cChartMode+viewName);
				for (int i=0; i<JVisualization.CHART_MODE_CODE.length; i++) {
					if (JVisualization.CHART_MODE_CODE[i].equals(value)) {
						chartMode = i;
						break;
						}
					}
				if (chartMode != JVisualization.cChartModeCount && chartMode != JVisualization.cChartModePercent) {
					String columnName = getProperty(cChartColumn+viewName);
					if (columnName != null)
						chartColumn = mTableModel.findColumn(columnName);
					if (chartColumn == JVisualization.cColumnUnassigned)
						chartMode = JVisualization.cChartModeCount;
					}
				}
			visualization.setPreferredChartType(chartType, chartMode, chartColumn);

			int dimensions = vpanel.getDimensionCount();
			for (int j=0; j<dimensions; j++) {
						// popups assigning column to axis
				String key = cAxisColumn + viewName + "_" + j;
				value = getProperty(key);
				if (value != null
				 && vpanel.setAxisColumnName(j, value)) {

						// setting of pruning bars
					key = cAxisLow + viewName + "_" + j;
					value = getProperty(key);
					if (value != null)
						vpanel.getPruningBar(j).setLowValue(Float.parseFloat(value));

					key = cAxisHigh + viewName + "_" + j;
					value = getProperty(key);
					if (value != null)
						vpanel.getPruningBar(j).setHighValue(Float.parseFloat(value));
					}
				}

			value = getProperty(cViewFontSize + viewName);
			if (value != null)
				visualization.setFontSize(Float.parseFloat(value), false);

			value = getProperty(cViewBackground + viewName);
			if (value != null)
				visualization.setViewBackground(Color.decode(value));

			value = getProperty(cTitleBackground + viewName);
			if (value != null)
				visualization.setTitleBackground(Color.decode(value));

			value = getProperty(cJittering + viewName);
			if (value != null)
				visualization.setJittering(Float.parseFloat(value), false);

			value = getProperty(cFastRendering + viewName);
			if (value != null)
				visualization.setFastRendering("true".equals(value));

			value = getProperty(cMarkerSize + viewName);
			if (value != null)
				visualization.setMarkerSize(Float.parseFloat(value), false);

			value = getProperty(cSizeInversion + viewName);
			visualization.setMarkerSizeInversion(value != null && value.equals("true"));

			value = getProperty(cSizeProportional + viewName);
			visualization.setMarkerSizeProportional(value != null && value.equals("true"));

			value = getProperty(cSizeAdaption + viewName);
			visualization.setMarkerSizeZoomAdaption(value == null || value.equals("true"));

			value = getProperty(cSizeColumn + viewName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
				if (value.startsWith("sizeByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
				if (column != JVisualization.cColumnUnassigned)
					visualization.setMarkerSizeColumn(column);
				}

			applyViewColorProperties(viewName, visualization.getMarkerColor());

			value = getProperty(cShapeColumn+viewName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
				if (value.startsWith("shapeByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
				if (column != JVisualization.cColumnUnassigned)
					visualization.setMarkerShapeColumn(column);
				}

			value = getProperty(cCaseSeparationColumn+viewName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
				if (value.startsWith("splitByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
				if (column != JVisualization.cColumnUnassigned) {
					value = getProperty(cCaseSeparationValue+viewName);
					visualization.setCaseSeparation(column, Float.parseFloat(value), false);
					}
				}

			value = getProperty(cSplitViewColumn1+viewName);
			if (value != null) {
				int column1 = JVisualization.cColumnUnassigned;
				if (value.startsWith("splitByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
					if (hitlistIndex != -1)
						column1 = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex);
					}
				else {
					column1 = mTableModel.findColumn(value);
					}

				int column2 = JVisualization.cColumnUnassigned;
				value = getProperty(cSplitViewColumn2+viewName);
				if (value != null) {
					column2 = JVisualization.cColumnUnassigned;
					if (value.startsWith("splitByHitlist")) {
						String hitlistName = value.substring(value.indexOf('\t')+1);
						int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
						if (hitlistIndex != -1)
							column2 = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex);
						}
					else {
						column2 = mTableModel.findColumn(value);
						}
					}

				if (column1 != JVisualization.cColumnUnassigned
				 || column2 != JVisualization.cColumnUnassigned)
					visualization.setSplittingColumns(column1, column2);
				}

			applyMarkerLabelDisplayerProperties(viewName, visualization);

			value = getProperty(cConnectionColumn1+viewName);
			if (value != null) {
				int column1 = value.equals(cConnectionColumnConnectAll) ? JVisualization.cConnectionColumnConnectAll
							: value.equals(cConnectionColumnConnectCases) ? JVisualization.cConnectionColumnConnectCases : mTableModel.findColumn(value);
				if (column1 != JVisualization.cColumnUnassigned) {
					int column2 = JVisualization.cColumnUnassigned;
					value = getProperty(cConnectionColumn2+viewName);
					if (value != null)
						column2 = mTableModel.findColumn(value);
					visualization.setConnectionColumns(column1, column2);
					value = getProperty(cConnectionLineWidth+viewName);
					float lineWidth = (value != null) ? Float.parseFloat(value)
									 : visualization.getMarkerSize();
					visualization.setConnectionLineWidth(lineWidth, false);

					int treeViewMode = decodeProperty(cTreeViewMode+viewName, JVisualization.TREE_VIEW_MODE_CODE);
					if (treeViewMode != -1 && treeViewMode != JVisualization.cTreeViewModeNone) {
						int radius = 5;
						value = getProperty(cTreeViewRadius+viewName);
						if (value != null)
							try { radius = Integer.parseInt(value); } catch (NumberFormatException nfe) {}
						value = getProperty(cTreeViewShowAll+viewName);
						boolean showAll = (value == null || value.equals("true"));
						visualization.setTreeViewMode(treeViewMode, radius, showAll);
						}
					}
				}

			value = getProperty(cShowNaNValues+viewName);
			visualization.setShowNaNValues(value != null && value.equals("true"));

			value = getProperty(cAffectGlobalExclusion+viewName);
			visualization.setAffectGlobalExclusion(value == null || value.equals("true"));

			value = getProperty(cSuppressScale+viewName);
			boolean hideScale = "true".equals(value);
			value = getProperty(cSuppressGrid+viewName);
			boolean hideGrid = (value == null) ? hideScale : "true".equals(value);
			visualization.setSuppressScale(hideScale, hideGrid);

			value = getProperty(cShowFoldChange+viewName);
			visualization.setShowFoldChange(value != null && value.equals("true"));
			
			value = getProperty(cShowPValue+viewName);
			visualization.setShowPValue(value != null && value.equals("true"));

			value = getProperty(cPValueColumn+viewName);
			if (value != null) {
				int column = mTableModel.findColumn(value);
				if (column != JVisualization.cColumnUnassigned)
					visualization.setPValueColumn(column, getProperty(cPValueRefCategory+viewName));
				}

			int boxplotMeanMode = decodeProperty(cBoxplotMeanMode+viewName, JVisualization.BOXPLOT_MEAN_MODE_CODE);
			if (boxplotMeanMode != -1)
				visualization.setBoxplotMeanMode(boxplotMeanMode);

			value = getProperty(cBoxplotShowMeanValues+viewName);
			boolean showMeanValues = (value != null && value.equals("true"));
			visualization.setShowMeanAndMedianValues(showMeanValues);

			if (view instanceof VisualizationPanel2D) {
				value = getProperty(cMarkerTransparency+viewName);
				if (value != null) {
					try {
						float transparency = Float.parseFloat(value);
						((JVisualization2D)visualization).setMarkerTransparency(transparency);
						}
					catch (NumberFormatException nfe) {}
					}

				value = getProperty(cMultiValueMarkerColumns+viewName);
				if (value != null) {
					String[] columnName = value.split("\\t");
					int[] column = new int[columnName.length];
					int foundCount = 0;
					for (int i=0; i<column.length; i++) {
						column[i] = mTableModel.findColumn(columnName[i]);
						if (column[i] != -1)
							foundCount++;
						}
					if (foundCount > 1) {
						if (foundCount < column.length) {
							int[] newColumn = new int[foundCount];
							int index = 0;
							for (int i=0; i<column.length; i++)
								if (column[i] != -1)
									newColumn[index++] = column[i];
							column = newColumn;
							}
						int mode = decodeProperty(cMultiValueMarkerMode+viewName, JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE);
						if (mode != -1)
							((JVisualization2D)visualization).setMultiValueMarkerColumns(column, mode);
						}
					}

				value = getProperty(cBackgroundColorColumn+viewName);
				if (value != null) {
					try {
						int column = JVisualization.cColumnUnassigned;

							// to be compatible with format prior V2.7.0
						if (value.equals("colorBySimilarity"))
							column = mTableModel.findColumn(DescriptorConstants.DESCRIPTOR_FFP512.shortName);

						else if (value.startsWith("colorByHitlist")) {
							String hitlistName = value.substring(value.indexOf('\t')+1);
							int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
							if (hitlistIndex != -1)
								column = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex);
							}
						else {
							column = mTableModel.findColumn(value);
							}

						if (column != JVisualization.cColumnUnassigned) {
							Color[] colorList = null;
							value = getProperty(cBackgroundColorCount+viewName);
							if (value != null) {
								int colorCount = Integer.parseInt(value);
								colorList = new Color[colorCount];
								for (int j=0; j<colorCount; j++) {
									value = getProperty(cBackgroundColor+viewName + "_" + j);
									colorList[j] = Color.decode(value);
									}
								}

							value = getProperty(cBackgroundColorListMode+viewName);
							int colorListMode = VisualizationColor.cColorListModeStraight;	// default
							if (value != null) {
								if (value.equals("Categories"))
									colorListMode = VisualizationColor.cColorListModeCategories;
								else if (value.equals("HSBShort"))
									colorListMode = VisualizationColor.cColorListModeHSBShort;
								else if (value.equals("HSBLong"))
									colorListMode = VisualizationColor.cColorListModeHSBLong;
								else if (value.equals("straight"))
									colorListMode = VisualizationColor.cColorListModeStraight;
								}

							if (colorList == null) {	// cColorCount is only available if mode is cColorListModeCategory
								Color color1 = Color.decode(getProperty(cBackgroundColor + viewName + "_0"));
								Color color2 = Color.decode(getProperty(cBackgroundColor + viewName + "_1"));
								colorList = VisualizationColor.createColorWedge(color1, color2, colorListMode, null);
								}

							((JVisualization2D)visualization).getBackgroundColor().setColor(column, colorList, colorListMode);

							value = getProperty(cBackgroundColorRecords);
							if (value != null) {
								if (value.equals("visibleRecords"))
									((JVisualization2D)visualization).setBackgroundColorConsidered(
													JVisualization2D.cVisibleRecords);
								else if (value.startsWith("fromHitlist")) {
									String hitlistName = value.substring(value.indexOf('\t')+1);
									int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
									if (hitlistIndex != -1)
										((JVisualization2D)visualization).setBackgroundColorConsidered(hitlistIndex);
									}
								}

							value = getProperty(cBackgroundColorRadius+viewName);
							if (value != null) {
								int radius = Integer.parseInt(value);
								((JVisualization2D)visualization).setBackgroundColorRadius(radius);
								}

							value = getProperty(cBackgroundColorFading+viewName);
							if (value != null) {
								int fading = Integer.parseInt(value);
								((JVisualization2D)visualization).setBackgroundColorFading(fading);
								}

							value = getProperty(cBackgroundColorMin + viewName);
							float min = (value == null) ? Float.NaN : Float.parseFloat(value);
							value = getProperty(cBackgroundColorMax + viewName);
							float max = (value == null) ? Float.NaN : Float.parseFloat(value);
							if (!Double.isNaN(min) || !Double.isNaN(max))
								((JVisualization2D)visualization).getBackgroundColor().setColorRange(min, max);
							}
						}
					catch (Exception e) {
//						JOptionPane.showMessageDialog(mParentFrame, "Invalid color settings");
						}
					}

				byte[] backgroundImageData = getBinary(cBackgroundImage+viewName);
				if (backgroundImageData != null)
					((JVisualization2D)visualization).setBackgroundImageData(backgroundImageData);

				int mode = decodeProperty(cCurveMode+viewName, JVisualization2D.CURVE_MODE_CODE);
				if (mode != -1) {
					value = getProperty(cCurveStdDev+viewName);
					boolean stdDev = (value != null && value.equals("true"));
					value = getProperty(cCurveSplitByCategory+viewName);
					boolean split = (value != null && value.equals("true"));
					((JVisualization2D)visualization).setCurveMode(mode, stdDev, split);
					}

				int type = decodeProperty(cCorrelationCoefficient+viewName, CorrelationCalculator.TYPE_NAME);
				if (type != -1)
					((JVisualization2D)visualization).setShownCorrelationType(type);
				}

			if (view instanceof VisualizationPanel3D) {
				value = getProperty(cFaceColor3D + viewName);
				if (value != null)
					((JVisualization3D)visualization).setGraphFaceColor(Color.decode(value));

				float[][] rotation = new float[3][3];
				int count = 0;
				for (int j=0; j<3; j++) {
					for (int k=0; k<3; k++) {
						value = getProperty(cRotation+viewName+j+k);
						if (value != null) {
							rotation[j][k] = Float.parseFloat(value);
							count++;
							}
						}
					}
				if (count == 9)
					((JVisualization3D)visualization).setRotationMatrix(rotation);
				}
			}
		else if (view instanceof DEFormView) {
			JCompoundTableForm form = ((DEFormView)view).getCompoundTableForm();

			form.setFormLayoutDescriptor(getProperty(cFormLayout+viewName));

			try {
				String value = getProperty(cFormObjectCount+viewName);
				int objectCount = Integer.parseInt(value);
				for (int j=0; j<objectCount; j++) {
					String description = getProperty(cFormObjectInfo+viewName+"_"+j);

						// The "idcode" column of versions before 2.7 is renamed to "Structure"
						// Thus, all references have to be adapted
					if (description.startsWith("idcode\tstructure")
					 && mTableModel.findColumn("idcode") == -1)
						description = "Structure" + description.substring(6);

					form.addFormObject(updateFormObjectDescription(description));
					}
				form.updateColors();
				}
			catch (NumberFormatException e) {}
			}
		else if (view instanceof JStructureGrid) {
			applyMarkerLabelDisplayerProperties(viewName, (JStructureGrid)view);

			if (viewName.equals("StructureView"))
				viewName = "";		// to provide read compatibility to version with static views

			String value = getProperty(cStructureGridColumns+viewName);
			try {
				((JStructureGrid)view).setColumnCount((value == null) ? 6 : Integer.parseInt(value));
				}
			catch (NumberFormatException e) {}

			value = getProperty(cStructureGridColumn+viewName);
			if (value == null)  // to be compatible with prior V2.7.0 format
				value = "Structure";
			((JStructureGrid)view).setStructureColumn(mTableModel.findColumn(value));
			}
		
		if (view instanceof FocusableView) {
			String value = getProperty(cFocusHitlist + viewName);
			if (value != null) {
				if (value.equals(cFocusOnSelection)) {
					((FocusableView)view).setFocusHitlist(JVisualization.cFocusOnSelection);
					}
				else {
					int hitlist = mTableModel.getHitlistHandler().getHitlistIndex(value);
					if (hitlist != -1)
						((FocusableView)view).setFocusHitlist(hitlist);
					}
				}
			}
		}

	private void applyViewColorProperties(String vColorName, VisualizationColor vColor) {
		try {
			String value = getProperty(cDefaultColor+vColorName);
			if (value != null)
				vColor.setDefaultDataColor(Color.decode(value));

			value = getProperty(cMissingColor+vColorName);
			if (value != null)
				vColor.setMissingDataColor(Color.decode(value));

			value = getProperty(cColorColumn + vColorName);
			if (value != null) {
				int column = JVisualization.cColumnUnassigned;
	
					// to be compatible with format prior V2.7.0
				if (value.equals("colorBySimilarity"))
					column = mTableModel.findColumn(DescriptorConstants.DESCRIPTOR_FFP512.shortName);
	
				else if (value.startsWith("colorByHitlist")) {
					String hitlistName = value.substring(value.indexOf('\t')+1);
					int hitlistIndex = mTableModel.getHitlistHandler().getHitlistIndex(hitlistName);
					if (hitlistIndex != -1)
						column = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex);
					}
				else {
					column = mTableModel.findColumn(value);
					}
	
				if (column == JVisualization.cColumnUnassigned) {
					vColor.setColor(JVisualization.cColumnUnassigned);
					}
				else {
					Color[] colorList = null;
					value = getProperty(cColorCount + vColorName);
					if (value != null) {
						int colorCount = Integer.parseInt(value);
						colorList = new Color[colorCount];
						for (int j=0; j<colorCount; j++) {
							value = getProperty(cColor + vColorName + "_" + j);
							colorList[j] = Color.decode(value);
							}
						}
	
					value = getProperty(cColorListMode + vColorName);
					int colorListMode = VisualizationColor.cColorListModeStraight;	// default
					if (value != null) {
						if (value.equals("Categories"))
							colorListMode = VisualizationColor.cColorListModeCategories;
						else if (value.equals("HSBShort"))
							colorListMode = VisualizationColor.cColorListModeHSBShort;
						else if (value.equals("HSBLong"))
							colorListMode = VisualizationColor.cColorListModeHSBLong;
						else if (value.equals("straight"))
							colorListMode = VisualizationColor.cColorListModeStraight;
						}
	
					if (colorList == null) {	// cColorCount is only available if mode is cColorListModeCategory
						Color color1 = Color.decode(getProperty(cColor + vColorName + "_0"));
						Color color2 = Color.decode(getProperty(cColor + vColorName + "_1"));
						colorList = VisualizationColor.createColorWedge(color1, color2, colorListMode, null);
						}
	
					vColor.setColor(column, colorList, colorListMode);
	
					value = getProperty(cColorMin + vColorName);
					float min = (value == null) ? Float.NaN : Float.parseFloat(value);
					value = getProperty(cColorMax + vColorName);
					float max = (value == null) ? Float.NaN : Float.parseFloat(value);
					if (!Double.isNaN(min) || !Double.isNaN(max))
						vColor.setColorRange(min, max);
					}
				}
			}
		catch (Exception e) {
//				JOptionPane.showMessageDialog(mParentFrame, "Invalid color settings");
			}
		}

	private String updateFormObjectDescription(String description) {
			// convert form object keys referencing structure fields prior V2.7.0
		int index = description.indexOf("#structure#");
		if (index != -1)
			return description.substring(0, index)+"Structure"+description.substring(index+11);

		index = description.indexOf("#3Dstructure#");
		if (index != -1)
			return description.substring(0, index)+CompoundTableModel.cColumnType3DCoordinates+description.substring(index+11);

		if (description.startsWith("Chem Lab Journal")
		 && mTableModel.findColumn("Chem Lab Journal") == -1)
			description = "ELN/ExtRef" + description.substring(16);

		return description;
		}

	public void learn() {
		super.learn();
		setProperty(cMainSplitting, DoubleFormat.toString(mParentPane.getMainSplitting()));
		setProperty(cRightSplitting, DoubleFormat.toString(mParentPane.getRightSplitting()));
		setProperty(cSelectedMainView, mMainPane.getSelectedViewTitle());
		setProperty(cDetailView, mDetailPane.getProperties());
		String[] dockInfo = mMainPane.getDockInfoSequence();
		setProperty(cMainViewCount, ""+dockInfo.length);
		for (int i=0; i<dockInfo.length; i++) {
			int dockInfoIndex = dockInfo[i].indexOf('\t');
			String title = dockInfo[i].substring(0, dockInfoIndex);
			String state = dockInfo[i].substring(dockInfoIndex+1);
			CompoundTableView view = mMainPane.getView(title);
			setProperty(cMainViewName+i, title);
			setProperty(cMainViewType+i, (view instanceof DETableView) ? cViewTypeTable
									   : (view instanceof VisualizationPanel2D) ? cViewType2D
									   : (view instanceof VisualizationPanel3D) ? cViewType3D
									   : (view instanceof DEFormView) ? cViewTypeForm
									   : (view instanceof JStructureGrid) ? cViewTypeStructureGrid
									   : (view instanceof ExplanationView) ? cViewTypeExplanation
									   : (view instanceof DEMacroEditor) ? cViewTypeMacroEditor
									   : "UNKNOWN_VIEW");
			setProperty(cMainViewDockInfo+i, state);

			String viewName = "_"+title;
			if (view instanceof DETableView) {
				DETable table = ((DETableView)view).getTable();
				setProperty(cTableRowHeight+viewName, ""+table.getRowHeight());

				// store column width for all visible columns
				for (int modelColumn=0; modelColumn<mTableModel.getColumnCount(); modelColumn++) {
					int column = mTableModel.convertFromDisplayableColumnIndex(modelColumn);
					if (((DETableView)view).isColumnVisible(column)) {
						int viewColumn = table.convertColumnIndexToView(modelColumn);
						setProperty(cTableColumnWidth+viewName+"_"+mTableModel.getColumnTitleNoAlias(column), ""+table.getColumnModel().getColumn(viewColumn).getPreferredWidth());
						if (((DETableView)view).getTextWrapping(column))
							setProperty(cTableColumnWrapping+viewName+"_"+mTableModel.getColumnTitleNoAlias(column), "true");
						}
					else {
						setProperty(cTableColumnVisibility+viewName+"_"+mTableModel.getColumnTitleNoAlias(column), "false");
						}
					if (((DETableView)view).getColorHandler().hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND))
						learnViewColorProperties(cTableText+viewName+mTableModel.getColumnTitleNoAlias(column),
								((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND));
					if (((DETableView)view).getColorHandler().hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND))
						learnViewColorProperties(cTableBackground+viewName+mTableModel.getColumnTitleNoAlias(column),
								((DETableView)view).getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND));
					}

				String order = table.getColumnOrder();
				if (order != null)
					setProperty(cTableColumnOrder+viewName, order);
				}
			else if (view instanceof VisualizationPanel) {
				VisualizationPanel vpanel = (VisualizationPanel)view;
				JVisualization visualization = vpanel.getVisualization();

				VisualizationPanel master = vpanel.getSynchronizationMaster();
				if (master != null) {
					setProperty(cMasterView+viewName, mMainPane.getViewTitle(master));
					}
				else {
					int dimensions = vpanel.getDimensionCount();
					for (int j=0; j<dimensions; j++) {
								// popups assigning column to axis
						String key = cAxisColumn + viewName + "_" + j;
						setProperty(key, vpanel.getAxisColumnName(j));
	
								// setting of pruning bars
						JPruningBar pbar = vpanel.getPruningBar(j);
						if (pbar.getLowValue() != pbar.getMinimumValue()) {
							key = cAxisLow + viewName + "_" + j;
							setProperty(key, ""+pbar.getLowValue());
							}
						if (pbar.getHighValue() != pbar.getMaximumValue()) {
							key = cAxisHigh + viewName + "_" + j;
							setProperty(key, ""+pbar.getHighValue());
							}
						}

					if (view instanceof VisualizationPanel3D) {
						if (!visualization.getViewBackground().equals(Color.WHITE))
							setProperty(cFaceColor3D+viewName, ""+((JVisualization3D)visualization).getGraphFaceColor().getRGB());

						float[][] rotation = ((JVisualization3D)visualization).getRotationMatrix();
						for (int j=0; j<3; j++)
							for (int k=0; k<3; k++)
								setProperty(cRotation+viewName+j+k, DoubleFormat.toString(rotation[j][k]));
						}
					}

				if (visualization.getFontSize() != 1.0)
					setProperty(cViewFontSize+viewName, ""+visualization.getFontSize());

				if (!visualization.getViewBackground().equals(Color.WHITE))
					setProperty(cViewBackground+viewName, ""+visualization.getViewBackground().getRGB());

				if (visualization.isSplitView())
					setProperty(cTitleBackground+viewName, ""+((JVisualization)visualization).getTitleBackground().getRGB());

				if (visualization.getJittering() != 0.0)
					setProperty(cJittering+viewName, ""+visualization.getJittering());

				if (visualization.isFastRendering())
					setProperty(cFastRendering+viewName, "true");

				if (visualization.getMarkerSize() != 1.0)
					setProperty(cMarkerSize+viewName, ""+visualization.getMarkerSize());

				if (visualization.getMarkerSizeInversion())
					setProperty(cSizeInversion+viewName, "true");

				if (visualization.getMarkerSizeProportional())
					setProperty(cSizeProportional+viewName, "true");

				if (!visualization.isMarkerSizeZoomAdapted())
					setProperty(cSizeAdaption+viewName, "false");

				int column = visualization.getMarkerSizeColumn();
				if (column != JVisualization.cColumnUnassigned) {
					String key = cSizeColumn+viewName;
					if (CompoundTableHitlistHandler.isHitlistColumn(column))
						setProperty(key, "sizeByHitlist\t"
								+ mTableModel.getHitlistHandler().getHitlistName(
										CompoundTableHitlistHandler.getHitlistFromColumn(column)));
					else {
						setProperty(key, mTableModel.getColumnTitleNoAlias(column));
						}
					}

				learnViewColorProperties(viewName, visualization.getMarkerColor());

				column = visualization.getMarkerShapeColumn();
				if (column != JVisualization.cColumnUnassigned) {
					if (CompoundTableHitlistHandler.isHitlistColumn(column))
						setProperty(cShapeColumn+viewName, "shapeByHitlist\t"
								+ mTableModel.getHitlistHandler().getHitlistName(
										CompoundTableHitlistHandler.getHitlistFromColumn(column)));
					else
						setProperty(cShapeColumn+viewName, mTableModel.getColumnTitleNoAlias(column));
					}

				if (visualization.isCaseSeparationDone()) {
					int csColumn = visualization.getCaseSeparationColumn();
					if (csColumn != JVisualization.cColumnUnassigned) {
						if (CompoundTableHitlistHandler.isHitlistColumn(csColumn))
							setProperty(cCaseSeparationColumn+viewName, "splitByHitlist\t"
									+ mTableModel.getHitlistHandler().getHitlistName(
											CompoundTableHitlistHandler.getHitlistFromColumn(csColumn)));
						else
							setProperty(cCaseSeparationColumn+viewName, mTableModel.getColumnTitleNoAlias(csColumn));
						setProperty(cCaseSeparationValue+viewName, ""+visualization.getCaseSeparationValue());
						}
					}

				int[] sc = visualization.getSplittingColumns();
				if (sc[0] != JVisualization.cColumnUnassigned) {
					if (CompoundTableHitlistHandler.isHitlistColumn(sc[0]))
						setProperty(cSplitViewColumn1+viewName, "splitByHitlist\t"
								+ mTableModel.getHitlistHandler().getHitlistName(
										CompoundTableHitlistHandler.getHitlistFromColumn(sc[0])));
					else
						setProperty(cSplitViewColumn1+viewName, mTableModel.getColumnTitleNoAlias(sc[0]));
					}
				if (sc[1] != JVisualization.cColumnUnassigned) {
					if (CompoundTableHitlistHandler.isHitlistColumn(sc[1]))
						setProperty(cSplitViewColumn2+viewName, "splitByHitlist\t"
								+ mTableModel.getHitlistHandler().getHitlistName(
										CompoundTableHitlistHandler.getHitlistFromColumn(sc[1])));
					else
						setProperty(cSplitViewColumn2+viewName, mTableModel.getColumnTitleNoAlias(sc[1]));
					}

				learnMarkerLabelDisplayerProperties(viewName, visualization);

				int type = visualization.getChartType();
				setProperty(cChartType+viewName, JVisualization.CHART_TYPE_CODE[type]);
				if (type == JVisualization.cChartTypeBars || type == JVisualization.cChartTypePies) {
					int mode = visualization.getPreferredChartMode();
					setProperty(cChartMode+viewName, JVisualization.CHART_MODE_CODE[mode]);
					if (mode != JVisualization.cChartModeCount && mode != JVisualization.cChartModePercent) {
						column = visualization.getPreferredChartColumn();
						setProperty(cChartColumn+viewName, mTableModel.getColumnTitleNoAlias(column));
						}
					}

				column = visualization.getConnectionColumn();
				if (column != JVisualization.cColumnUnassigned) {
					setProperty(cConnectionColumn1+viewName,
							(column == JVisualization.cConnectionColumnConnectAll) ? cConnectionColumnConnectAll
						  : (column == JVisualization.cConnectionColumnConnectCases) ? cConnectionColumnConnectCases
						  : mTableModel.getColumnTitleNoAlias(column));
					column = visualization.getConnectionOrderColumn();
					if (column != JVisualization.cColumnUnassigned)
						setProperty(cConnectionColumn2+viewName, mTableModel.getColumnTitleNoAlias(column));
					double lineWidth = visualization.getConnectionLineWidth();
					if (lineWidth != 1.0)
						setProperty(cConnectionLineWidth+viewName, ""+lineWidth);

					if (visualization.getTreeViewMode() != JVisualization.cTreeViewModeNone) {
						setProperty(cTreeViewMode+viewName, JVisualization.TREE_VIEW_MODE_CODE[visualization.getTreeViewMode()]);
						setProperty(cTreeViewRadius+viewName, ""+visualization.getTreeViewRadius());
						setProperty(cTreeViewShowAll+viewName, visualization.isTreeViewShowAll() ? "true" : "false");
						}
					}

				if (visualization.getShowNaNValues())
					setProperty(cShowNaNValues+viewName, "true");

				if (!visualization.getAffectGlobalExclusion())
					setProperty(cAffectGlobalExclusion+viewName, "false");

				if (visualization.isGridSuppressed() != visualization.isScaleSuppressed())
					setProperty(cSuppressGrid+viewName, visualization.isGridSuppressed() ? "true" : "false");

				if (visualization.isScaleSuppressed())
					setProperty(cSuppressScale+viewName, "true");

				if (visualization.isShowFoldChange())
					setProperty(cShowFoldChange+viewName, "true");

				if (visualization.isShowPValue())
					setProperty(cShowPValue+viewName, "true");

				column = visualization.getPValueColumn();
				if (column != JVisualization.cColumnUnassigned) {
					setProperty(cPValueColumn+viewName, mTableModel.getColumnTitleNoAlias(column));
					setProperty(cPValueRefCategory+viewName, visualization.getPValueRefCategory());
					}
	
				int boxplotMeanMode = visualization.getBoxplotMeanMode();
				if (boxplotMeanMode != JVisualization.BOXPLOT_DEFAULT_MEAN_MODE)
					setProperty(cBoxplotMeanMode+viewName, JVisualization.BOXPLOT_MEAN_MODE_CODE[boxplotMeanMode]);

				if (visualization.isShowMeanAndMedianValues())
					setProperty(cBoxplotShowMeanValues+viewName, "true");

				if (view instanceof VisualizationPanel2D) {
					double transparency = ((JVisualization2D)visualization).getMarkerTransparency();
					if (transparency != 0.0) {
						setProperty(cMarkerTransparency+viewName, ""+transparency);
						}

					int[] multiValueMarkerColumn = ((JVisualization2D)visualization).getMultiValueMarkerColumns();
					if (multiValueMarkerColumn != null) {
						int multiValueMarkerMode = ((JVisualization2D)visualization).getMultiValueMarkerMode();
						setProperty(cMultiValueMarkerMode+viewName, JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE[multiValueMarkerMode]);
						StringBuilder columnNames = new StringBuilder(mTableModel.getColumnTitleNoAlias(multiValueMarkerColumn[0]));
						for (int j=1; j<multiValueMarkerColumn.length; j++)
							columnNames.append('\t').append(mTableModel.getColumnTitleNoAlias(multiValueMarkerColumn[j]));
						setProperty(cMultiValueMarkerColumns+viewName, ""+columnNames.toString());
						}

					column = ((JVisualization2D)visualization).getBackgroundColor().getColorColumn();
					if (column != JVisualization.cColumnUnassigned) {
						String key = cBackgroundColorColumn+viewName;
						if (CompoundTableHitlistHandler.isHitlistColumn(column))
							setProperty(key, "colorByHitlist\t"
									+ mTableModel.getHitlistHandler().getHitlistName(
											CompoundTableHitlistHandler.getHitlistFromColumn(column)));
						else {
							setProperty(key, mTableModel.getColumnTitleNoAlias(column));
							}

						int mode = ((JVisualization2D)visualization).getBackgroundColor().getColorListMode();
						key = cBackgroundColorListMode+viewName;
						if (mode == VisualizationColor.cColorListModeCategories)
							setProperty(key, "Categories");
						else if (mode == VisualizationColor.cColorListModeHSBShort)
							setProperty(key, "HSBShort");
						else if (mode == VisualizationColor.cColorListModeHSBLong)
							setProperty(key, "HSBLong");
						else if (mode == VisualizationColor.cColorListModeStraight)
							setProperty(key, "straight");

						Color[] colorList = ((JVisualization2D)visualization).getBackgroundColor().getColorListWithoutDefaults();
						if (mode == VisualizationColor.cColorListModeCategories) {
							setProperty(cBackgroundColorCount+viewName, ""+colorList.length);
							for (int j=0; j<colorList.length; j++)
								setProperty(cBackgroundColor+viewName+"_"+j, ""+colorList[j].getRGB());
							}
						else {
							setProperty(cBackgroundColor+viewName+"_0", ""+colorList[0].getRGB());
							setProperty(cBackgroundColor+viewName+"_1", ""+colorList[colorList.length-1].getRGB());
							}

						int hitlist = ((JVisualization2D)visualization).getBackgroundColorConsidered();
						String value = (hitlist == JVisualization2D.cVisibleRecords) ? "visibleRecords"
								: "fromHitlist\t" + mTableModel.getHitlistHandler().getHitlistName(hitlist);
						setProperty(cBackgroundColorRecords, value);
						setProperty(cBackgroundColorRadius+viewName, ""+((JVisualization2D)visualization).getBackgroundColorRadius());
						setProperty(cBackgroundColorFading+viewName, ""+((JVisualization2D)visualization).getBackgroundColorFading());

						if (!Double.isNaN(((JVisualization2D)visualization).getBackgroundColor().getColorMin()))
							setProperty(cBackgroundColorMin+viewName, ""+((JVisualization2D)visualization).getBackgroundColor().getColorMin());
						if (!Double.isNaN(((JVisualization2D)visualization).getBackgroundColor().getColorMax()))
							setProperty(cBackgroundColorMax+viewName, ""+((JVisualization2D)visualization).getBackgroundColor().getColorMax());
						}

					byte[] backgroundImageData = ((JVisualization2D)visualization).getBackgroundImageData();
					if (backgroundImageData != null)
						setBinary(cBackgroundImage+viewName, backgroundImageData);

					int curveMode = ((JVisualization2D)visualization).getCurveMode();
					if (curveMode != 0) {
						setProperty(cCurveMode+viewName, JVisualization2D.CURVE_MODE_CODE[curveMode]);
						if (((JVisualization2D)visualization).isShowStandardDeviation())
							setProperty(cCurveStdDev+viewName, "true");
						if (((JVisualization2D)visualization).isCurveSplitByCategory())
							setProperty(cCurveSplitByCategory+viewName, "true");
						}

					int correlationType = ((JVisualization2D)visualization).getShownCorrelationType();
					if (correlationType != CorrelationCalculator.TYPE_NONE) {
						setProperty(cCorrelationCoefficient+viewName, CorrelationCalculator.TYPE_NAME[correlationType]);
						}
					}
				}
			else if (view instanceof DEFormView) {
				JCompoundTableForm form = ((DEFormView)view).getCompoundTableForm();

				setProperty(cFormLayout+viewName, form.getFormLayoutDescriptor());

				setProperty(cFormObjectCount+viewName, ""+form.getFormObjectCount());
				for (int j=0; j<form.getFormObjectCount(); j++)
					setProperty(cFormObjectInfo+viewName+"_"+j, ""+form.getFormObjectDescriptor(j));
				}
			else if (view instanceof JStructureGrid) {
				learnMarkerLabelDisplayerProperties(viewName, (JStructureGrid)view);

				int structureGridColumns = ((JStructureGrid)view).getColumnCount();
				setProperty(cStructureGridColumns+viewName, ""+structureGridColumns);

				String structureGridColumn = mTableModel.getColumnTitleNoAlias(((JStructureGrid)view).getStructureColumn());
				setProperty(cStructureGridColumn+viewName, ""+structureGridColumn);
				}

			if (view instanceof FocusableView) {
				if (((FocusableView)view).getFocusHitlist() == JVisualization.cFocusOnSelection)
					setProperty(cFocusHitlist+viewName, cFocusOnSelection);
				else if (((FocusableView)view).getFocusHitlist() != JVisualization.cHitlistUnassigned)
					setProperty(cFocusHitlist+viewName, mTableModel.getHitlistHandler().getHitlistNames()[((FocusableView)view).getFocusHitlist()]);
				}
			}

		for (int i=0; i<mPruningPanel.getFilterCount(); i++) {
			JFilterPanel filter = mPruningPanel.getFilter(i);

			int column = filter.getColumnIndex();
			String columnName = (column < 0) ? null : mTableModel.getColumnTitleNoAlias(column);

			String property = null;
			if (filter instanceof JCategoryBrowser)
				property = cFilterTypeCategoryBrowser;
			else if (filter instanceof JDoubleFilterPanel)
				property = cFilterTypeDouble + "\t" + columnName;
			else if (filter instanceof JCategoryFilterPanel)
				property = cFilterTypeCategory + "\t" + columnName;
			else if (filter instanceof JStringFilterPanel)
				property = cFilterTypeString + "\t" + columnName;
			else if (filter instanceof JHitlistFilterPanel)
				property = cFilterTypeHitlist;
			else if (filter instanceof JSingleStructureFilterPanel)
				property = cFilterTypeStructure + "\t" + columnName;
			else if (filter instanceof JMultiStructureFilterPanel && ((JMultiStructureFilterPanel)filter).supportsSSS())
				property = cFilterTypeSSSList + "\t" + columnName;
			else if (filter instanceof JMultiStructureFilterPanel && ((JMultiStructureFilterPanel)filter).supportsSim())
				property = cFilterTypeSIMList + "\t" + columnName;
			else if (filter instanceof JReactionFilterPanel)
				property = cFilterTypeReaction + "\t" + columnName;

			String settings = filter.getSettings();
			if (settings != null)
				property = property.concat("\t" + settings);

			setProperty(cFilter+i, property);
			}

		for (int i=0; i<mPruningPanel.getFilterCount(); i++) {
			JFilterPanel filter = mPruningPanel.getFilter(i);

			String property = filter.getAnimationSettings();
			if (property != null)
				setProperty(cFilterAnimation+i, property);
			}
		}

	private void learnViewColorProperties(String vColorName, VisualizationColor vColor) {
		if (!vColor.isDefaultDefaultDataColor())
			setProperty(cDefaultColor+vColorName, ""+vColor.getDefaultDataColor().getRGB());

		if (!vColor.isDefaultMissingDataColor())
			setProperty(cMissingColor+vColorName, ""+vColor.getMissingDataColor().getRGB());

		int column = vColor.getColorColumn();
		if (column != JVisualization.cColumnUnassigned) {
			String key = cColorColumn+vColorName;
			if (CompoundTableHitlistHandler.isHitlistColumn(column))
				setProperty(key, "colorByHitlist\t"
						+ mTableModel.getHitlistHandler().getHitlistName(
								CompoundTableHitlistHandler.getHitlistFromColumn(column)));
			else {
				setProperty(key, mTableModel.getColumnTitleNoAlias(column));
				}
	
			int mode = vColor.getColorListMode();
			key = cColorListMode+vColorName;
			if (mode == VisualizationColor.cColorListModeCategories)
				setProperty(key, "Categories");
			else if (mode == VisualizationColor.cColorListModeHSBShort)
				setProperty(key, "HSBShort");
			else if (mode == VisualizationColor.cColorListModeHSBLong)
				setProperty(key, "HSBLong");
			else if (mode == VisualizationColor.cColorListModeStraight)
				setProperty(key, "straight");
	
			Color[] colorList = vColor.getColorListWithoutDefaults();
			if (mode == VisualizationColor.cColorListModeCategories) {
				setProperty(cColorCount+vColorName, ""+colorList.length);
				for (int j=0; j<colorList.length; j++)
					setProperty(cColor+vColorName+"_"+j, ""+colorList[j].getRGB());
				}
			else {
				setProperty(cColor+vColorName+"_0", ""+colorList[0].getRGB());
				setProperty(cColor+vColorName+"_1", ""+colorList[colorList.length-1].getRGB());
				}
	
			if (!Double.isNaN(vColor.getColorMin()))
				setProperty(cColorMin+vColorName, ""+vColor.getColorMin());
			if (!Double.isNaN(vColor.getColorMax()))
				setProperty(cColorMax+vColorName, ""+vColor.getColorMax());
			}
		}

	private void learnMarkerLabelDisplayerProperties(String viewName, MarkerLabelDisplayer displayer) {
		boolean labelsUsed = false;
		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++) {
			int column = displayer.getMarkerLabelColumn(i);
			if (column != -1) {
				String columnKey = cLabelColumn+viewName+"_"+MarkerLabelDisplayer.cPositionCode[i];
				setProperty(columnKey, mTableModel.getColumnTitleNoAlias(column));
				labelsUsed = true;
				}
			}
		if (labelsUsed) {
			if (displayer.isMarkerLabelsInTreeViewOnly())
				setProperty(cLabelMode+viewName, "inDetailGraphOnly");

			double size = displayer.getMarkerLabelSize();
			if (size != 1.0)
				setProperty(cLabelSize+viewName, ""+size);
			}
		}

	public int decodeProperty(String key, String[] option) {
		String value = getProperty(key);
		if (value != null)
			for (int i=0; i<option.length; i++)
				if (value.equals(option[i]))
					return i;
		return -1;
		}
	}