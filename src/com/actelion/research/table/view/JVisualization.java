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

package com.actelion.research.table.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.TTestImpl;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.table.CompoundListSelectionModel;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableHitlistListener;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.HighlightListener;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.view.graph.RadialVisualizationNode;
import com.actelion.research.table.view.graph.TreeVisualizationNode;
import com.actelion.research.table.view.graph.VisualizationNode;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.DoubleFormat;

public abstract class JVisualization extends JComponent
		implements CompoundTableListener,FocusableView,HighlightListener,MarkerLabelDisplayer,ListSelectionListener,MouseListener,
			MouseMotionListener,Printable,CompoundTableHitlistListener,VisualizationColorListener {
	private static final long serialVersionUID = 0x20100610;

	// taken from class com.actelion.research.gui.form.FormObjectBorder
	public static final Color DEFAULT_TITLE_BACKGROUND = new Color(224, 224, 255);
	protected static final Rectangle2D.Float DEPICTOR_RECT = new Rectangle2D.Float(0, 0, 32000, 32000);

	public static final int cColumnUnassigned = -1;
	public static final int cConnectionColumnConnectAll = -2;
	public static final int cConnectionColumnMeanAndMedian = -3;
	public static final int cConnectionColumnConnectCases = -4;

	public static final int cChartTypeScatterPlot = 0;
	public static final int cChartTypeWhiskerPlot = 1;
	public static final int cChartTypeBoxPlot = 2;
	public static final int cChartTypeBars = 3;
	public static final int cChartTypePies = 4;

	public static final String[] CHART_TYPE_NAME = { "Scatter Plot", "Whisker Plot", "Box Plot", "Bar Chart", "Pie Chart" };
	public static final String[] CHART_TYPE_CODE = { "scatter", "whiskers", "boxes", "bars", "pies" };

	public static final int cChartModeCount = 0;
	public static final int cChartModePercent = 1;
	public static final int cChartModeMean = 2;
	public static final int cChartModeMin = 3;
	public static final int cChartModeMax = 4;
	public static final int cChartModeSum = 5;

	public static final String[] CHART_MODE_NAME = { "Row Count", "Row Percentage", "Mean Value", "Minimum Value", "Maximum Value", "Sum of Values" };
	public static final String[] CHART_MODE_CODE = { "count", "percent", "mean", "min", "max", "sum" };

	public static final int cTreeViewModeNone = 0;
	public static final int cTreeViewModeHTree = 1;
	public static final int cTreeViewModeVTree = 2;
	public static final int cTreeViewModeRadial = 3;

	public static final String[] TREE_VIEW_MODE_NAME = { "<none>", "Horizontal Tree", "Vertical Tree", "Radial Graph" };
	public static final String[] TREE_VIEW_MODE_CODE = { "none", "hTree", "vTree", "radial" };

	public static final int cMaxChartCategoryCount = 256;			// this is for one axis
	public static final int cMaxTotalChartCategoryCount = 32768;	// this is the product of all axis
	private static final float cBarSpacingFactor = 1.08f;

	public static final int cMaxCaseSeparationCategoryCount = 64;	// this is for one axis

	protected static final String[] CHART_MODE_AXIS_TEXT = CHART_MODE_CODE;

	public static final String[] BOXPLOT_MEAN_MODE_TEXT = { "No Indicator", "Median Line", "Mean Line", "Mean & Median Lines", "Mean & Median Triangles" };
	public static final String[] BOXPLOT_MEAN_MODE_CODE = { "none", "median", "mean", "both", "triangles" };
	public static final int BOXPLOT_DEFAULT_MEAN_MODE = 3;
	protected static final int cBoxplotMeanModeMedian = 1;
	protected static final int cBoxplotMeanModeMean = 2;
	protected static final int cBoxplotMeanModeLines = 3;
	protected static final int cBoxplotMeanModeTriangles = 4;

	protected static final int AXIS_TYPE_UNASSIGNED = 0;
	protected static final int AXIS_TYPE_TEXT_CATEGORY = 1;
	protected static final int AXIS_TYPE_DOUBLE_CATEGORY = 2;
	protected static final int AXIS_TYPE_DOUBLE_VALUE = 3;

	protected static final float SCALE_LIGHT = 0.4f;
	protected static final float SCALE_MEDIUM = 0.7f;
	protected static final float SCALE_STRONG = 1.0f;

	private static final byte EXCLUSION_FLAG_ZOOM_0 = 0x01;
	private static final byte EXCLUSION_FLAG_NAN_0 = 0x08;
	private static final byte EXCLUSION_FLAGS_NAN = 7 * EXCLUSION_FLAG_NAN_0;
	private static final byte EXCLUSION_FLAG_DETAIL_GRAPH = 0x40;

	// This is an Apple only solution and needs to be adapted to support high-res displays of other vendors
	private static final Object sContentScaleFactorObject = Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor");
	public static final float sRetinaFactor = (sContentScaleFactorObject == null) ? 1f : ((Float)sContentScaleFactorObject).floatValue();

	protected CompoundTableModel	mTableModel;
	protected float[]				mAxisVisMin,mAxisVisMax;
	protected float[][]				mAxisSimilarity;
	protected int[]					mAxisIndex,mLabelColumn,mSplittingColumn,mCategoryVisibleCount;
	protected boolean[]				mIsCategoryAxis;
	protected CategoryViewInfo		mChartInfo;
	protected VisualizationPoint[]	mPoint;
	protected VisualizationPoint 	mHighlightedPoint,mActivePoint;
	protected JVisualizationLegend	mColorLegend,mShapeLegend,mSizeLegend;
	protected VisualizationColor	mMarkerColor;
	protected VisualizationSplitter	mSplitter;
	protected VisualizationPoint[]  mConnectionLinePoint;
	protected TreeMap<byte[],VisualizationPoint>mConnectionLineMap;
	protected VisualizationNode[][]	mTreeNodeList;
	protected float					mAbsoluteMarkerSize,mRelativeMarkerSize,mMarkerLabelSize,mMarkerJittering,mRelativeFontSize,
									mAbsoluteConnectionLineWidth,mRelativeConnectionLineWidth,mCaseSeparationValue,mMarkerSizeZoomAdaption;
	protected boolean				mOffImageValid,mCoordinatesValid,mMouseIsDown,mTouchFunctionActive,mLocalAffectsGlobalExclusion,
									mAddingToSelection,mMarkerSizeInversion,mSuppressScale,mSuppressGrid,mSuspendGlobalExclusion,
									mShowNaNValues,mBoxplotShowMeanAndMedianValues,mBoxplotShowPValue,mBoxplotShowFoldChange,
									mLabelsInTreeViewOnly,mTreeViewShowAll,mIsFastRendering,mMarkerSizeProportional;
	protected int					mDataPoints,mMarkerSizeColumn,mMarkerShapeColumn,mFontHeight,mBoxplotMeanMode,
									mMouseX1,mMouseY1,mMouseX2,mMouseY2,mDimensions,mConnectionColumn,mConnectionOrderColumn,
									mChartColumn,mChartMode,mChartType,mPreferredChartType,mPValueColumn,mTreeViewRadius,
									mFocusHitlist,mCaseSeparationColumn,mCaseSeparationCategoryCount,
									mTreeViewMode,mActiveExclusionFlags,mHVCount;
	protected String				mPValueRefCategory;
	protected Random				mRandom;
	protected StereoMolecule		mLabelMolecule;

	private CompoundListSelectionModel mSelectionModel;
	private int						mLocalExclusionFlagNo,mPreviousLocalExclusionFlagNo;
	private float[]					mPruningBarLow,mPruningBarHigh;
	private int[]					mCategoryMin,mCategoryMax,mCombinedCategoryCount;
	private Color					mViewBackground,mTitleBackground;
	private boolean					mLassoSelecting,mRectangleSelecting,mApplyLocalExclusionScheduled;
	private Polygon			 	mLassoRegion;
	private DetailPopupProvider	 mDetailPopupProvider;

	public JVisualization(CompoundTableModel tableModel,
						  CompoundListSelectionModel selectionModel,
						  int dimensions) {
		mTableModel = tableModel;
		mSelectionModel = selectionModel;
		mDimensions = dimensions;

		addMouseListener(this);
		addMouseMotionListener(this);

		tableModel.addHighlightListener(this);
		selectionModel.addListSelectionListener(this);

		mPruningBarLow = new float[mDimensions];
		mPruningBarHigh = new float[mDimensions];

		mAxisVisMin = new float[mDimensions];
		mAxisVisMax = new float[mDimensions];
		mAxisIndex = new int[mDimensions];
		mIsCategoryAxis = new boolean[mDimensions];
		mSplittingColumn = new int[mDimensions];
		mPreviousLocalExclusionFlagNo = -1;
		mLocalExclusionFlagNo = -1;
		mLocalAffectsGlobalExclusion = true;	// default
		mSuspendGlobalExclusion = true;
		mMarkerSizeZoomAdaption = 1.0f;

		mMarkerColor = new VisualizationColor(mTableModel, this);

		mLabelColumn = new int[MarkerLabelDisplayer.cPositionCode.length];

		mPreferredChartType = cChartTypeScatterPlot;
		mChartMode = cChartModeCount;
		mChartColumn = cColumnUnassigned;

		mCategoryMin = new int[mDimensions];
		mCategoryMax = new int[mDimensions];
		mCategoryVisibleCount = new int[mDimensions];
		mAxisSimilarity = new float[mDimensions][];

		mRandom = new Random();
		setToolTipText("");	// to switch on tool-tips
		}

	protected void initialize() {
		mRelativeFontSize = 1.0f;
		mRelativeMarkerSize = 1.0f;
		mMarkerSizeInversion = false;
		mMarkerSizeProportional = false;
		mMarkerSizeColumn = cColumnUnassigned;
		mMarkerShapeColumn = cColumnUnassigned;
		mMarkerJittering = 0.0f;
		mConnectionColumn = cColumnUnassigned;
		mConnectionOrderColumn = cColumnUnassigned;
		mRelativeConnectionLineWidth = 1.0f;
		mSplittingColumn[0] = cColumnUnassigned;
		mSplittingColumn[1] = cColumnUnassigned;
		mCaseSeparationColumn = cColumnUnassigned;
		mCaseSeparationValue = 0.5f;
		mPValueColumn = cColumnUnassigned;
		mBoxplotMeanMode = BOXPLOT_DEFAULT_MEAN_MODE;
		mSplitter = null;
		mHVCount = 1;
		mHighlightedPoint = null;
		mActivePoint = null;
		mCoordinatesValid = false;
		mColorLegend = null;
		mShapeLegend = null;
		mSizeLegend = null;
		mSuppressGrid = false;
		mSuppressScale = false;
		mFocusHitlist = cHitlistUnassigned;
		mLabelsInTreeViewOnly = false;
		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++) {
			mLabelColumn[i] = cColumnUnassigned;
			}
		mMarkerLabelSize = 1.0f;
		for (int axis=0; axis<mDimensions; axis++) {
			mAxisIndex[axis] = cColumnUnassigned;
			initializeAxis(axis);
			}
		mShowNaNValues = true;
		mTitleBackground = DEFAULT_TITLE_BACKGROUND;
		mTreeViewMode = cTreeViewModeNone;
		mTreeViewRadius = 5;
		mTreeViewShowAll = true;
		determineChartType();
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	public String getAxisTitle(int column) {
		return mTableModel.getColumnTitleExtended(column);
		}

/*	protected int getAxisType(int axis) {
		if (mAxisIndex[axis] == -1)
			return AXIS_TYPE_UNASSIGNED;
		return mIsCategoryAxis[axis] ? AXIS_TYPE_TEXT_CATEGORY : AXIS_TYPE_DOUBLE_VALUE;

		if (mAxisIndex[axis] != -1) {
			if (!mTableModel.isColumnTypeDouble(mAxisIndex[axis])
			 && !mTableModel.isDescriptorColumn(mAxisIndex[axis]))
				return AXIS_TYPE_TEXT_CATEGORY;
			if ((mChartType == cChartTypeBars
			  || mChartType == cChartTypePies
			  || ((mChartType == cChartTypeBoxPlot
				|| mChartType == cChartTypeWhiskerPlot)
			   && axis != mChartInfo.barAxis)
			  || (mChartType == cChartTypeScatterPlot
			   && mCaseSeparationColumn != cColumnUnassigned))
			  && mTableModel.isColumnTypeCategory(mAxisIndex[axis]))
				return AXIS_TYPE_DOUBLE_CATEGORY;
			else
				return AXIS_TYPE_DOUBLE_VALUE;
			}
		else if (mChartType == cChartTypeBars && mChartInfo.barAxis == axis) {
			return AXIS_TYPE_DOUBLE_VALUE;
			}
		return AXIS_TYPE_UNASSIGNED;
		}*/

	public void cleanup() {
		mTableModel.removeHighlightListener(this);
		mSelectionModel.removeListSelectionListener(this);
		if (mLocalExclusionFlagNo != -1)
			mTableModel.freeCompoundFlag(mLocalExclusionFlagNo);
		mLocalExclusionFlagNo = -1;
		}

	public int getDimensionCount() {
		return mDimensions;
		}

	public void initializeDataPoints() {
		mDataPoints = mTableModel.getTotalRowCount();

		mPoint = new VisualizationPoint[mDataPoints];
		for (int i=0; i<mDataPoints; i++) {
			CompoundRecord record = mTableModel.getTotalRecord(i);
			mPoint[record.getID()] = createVisualizationPoint(record);
			}

		updateActiveRow();
		}

	protected void drawSelectionOutline(Graphics g) {
		g.setColor(VisualizationColor.cSelectedColor);
		if (mRectangleSelecting)
			g.drawRect((mMouseX1<mMouseX2) ? mMouseX1 : mMouseX2,
					   (mMouseY1<mMouseY2) ? mMouseY1 : mMouseY2,
					   Math.abs(mMouseX2-mMouseX1),
					   Math.abs(mMouseY2-mMouseY1));

		if (mLassoSelecting)
			g.drawPolygon(mLassoRegion);
		}

	protected abstract VisualizationPoint createVisualizationPoint(CompoundRecord record);
	public abstract int getAvailableShapeCount();
	public abstract int print(Graphics g, PageFormat f, int pageIndex);
	public abstract void paintHighResolution(Graphics g, Rectangle bounds, float fontScaling, boolean transparentBG, boolean isPrinting);

	// methods needed by JVisualizationLegend
	protected abstract int getStringWidth(String s);
	protected abstract void setFontHeight(int h);
	protected abstract void setColor(Color c);
	protected abstract void drawLine(int x1, int y1, int x2, int y2);
	protected abstract void drawRect(int x, int y, int w, int h);
	protected abstract void fillRect(int x, int y, int w, int h);
	protected abstract void drawMarker(Color color, int shape, int size, int x, int y);
	protected abstract void drawString(String s, int x, int y);
	protected abstract void drawMolecule(StereoMolecule mol, Color color, Rectangle2D.Float rect, int mode, int maxAVBL);

	protected void setActivePoint(VisualizationPoint vp) {
		mActivePoint = vp;

		if (mTreeViewMode != cTreeViewModeNone)
			updateTreeViewGraph();

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] != cColumnUnassigned && mTableModel.isDescriptorColumn(mAxisIndex[axis]))
				setSimilarityValues(axis);

		repaint();
		}

	private void setHighlightedPoint(VisualizationPoint vp) {
		mHighlightedPoint = vp;
		repaint();
		}

	/**
	 * Determines whether radial chart view is selected and if conditions are such
	 * that a radial chart is shown when a current record is defined, i.e. we have
	 * connection lines defined by both a referencing and a referenced column.
	 * @return true radial chart view can be displayed
	 */
	public int getTreeViewMode() {
		if (mTreeViewMode != cTreeViewModeNone
		 && mConnectionColumn != cColumnUnassigned
		 && mConnectionColumn != cConnectionColumnConnectAll
		 && mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn)) != -1)
			return mTreeViewMode;
		return cTreeViewModeNone;
		}

	public int getTreeViewRadius() {
		return mTreeViewRadius;
		}

	public void setTreeViewMode(int mode, int radius, boolean showAll) {
		if (mTreeViewMode != mode
		 || (mTreeViewMode != cTreeViewModeNone
		  && (mTreeViewRadius != radius || mTreeViewShowAll != showAll))) {
			mTreeViewMode = mode;
			mTreeViewRadius = radius;
			mTreeViewShowAll = showAll;
			updateTreeViewGraph();
			}
		}

	public boolean isTreeViewShowAll() {
		return mTreeViewShowAll;
		}

	public boolean isTreeViewModeEnabled() {
		return getTreeViewMode() != cTreeViewModeNone;
		}

	public boolean isMarkerLabelsInTreeViewOnly() {
		return mLabelsInTreeViewOnly;
		}

	/**
	 * Determines whether currently a tree view is displayed, which includes
	 * empty tree views, if no root is chosen and not all rows are shown.
	 * @return
	 */
	protected boolean isTreeViewGraph() {
		return mTreeViewMode != cTreeViewModeNone
			&& (mActivePoint != null || !mTreeViewShowAll)
		   	&& mConnectionColumn != cColumnUnassigned
		   	&& mConnectionColumn != cConnectionColumnConnectAll
		   	&& mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn)) != -1;
		}

	protected int compareConnectionLinePoints(VisualizationPoint p1, VisualizationPoint p2) {
		if (p1.hvIndex != p2.hvIndex)
			return (p1.hvIndex < p2.hvIndex) ? -1 : 1;
		if (isCaseSeparationDone()) {
			float v1 = p1.record.getDouble(mCaseSeparationColumn);
			float v2 = p2.record.getDouble(mCaseSeparationColumn);
			if (v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		if (mConnectionColumn > cColumnUnassigned) {
			float v1 = p1.record.getDouble(mConnectionColumn);
			float v2 = p2.record.getDouble(mConnectionColumn);
			if (v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		int connectionOrderColumn = (mConnectionOrderColumn == cColumnUnassigned) ? mAxisIndex[0] : mConnectionOrderColumn;
		if (connectionOrderColumn != cColumnUnassigned) {
			float v1 = p1.record.getDouble(connectionOrderColumn);
			float v2 = p2.record.getDouble(connectionOrderColumn);
			if (v1 != v2)
				return (v1 < v2) ? -1 : 1;
			}
		return 0;
		}

	protected boolean isConnectionLinePossible(VisualizationPoint p1, VisualizationPoint p2) {
		if (p1.hvIndex != p2.hvIndex
		 || (isCaseSeparationDone()
		  && p1.record.getDouble(mCaseSeparationColumn) != p2.record.getDouble(mCaseSeparationColumn))
		 || (mConnectionColumn != JVisualization.cConnectionColumnConnectAll
		  && p1.record.getDouble(mConnectionColumn) != p2.record.getDouble(mConnectionColumn)))
		  	return false;
		return true;
		}

	protected int getNextChangedConnectionLinePointIndex(int index1) {
		int index2 = index1+1;

		while (index2<mConnectionLinePoint.length
			&& compareConnectionLinePoints(mConnectionLinePoint[index1], mConnectionLinePoint[index2]) == 0)
			index2++;

		return index2;
		}

	private void updateTreeViewGraph() {
		boolean oldWasNull = (mTreeNodeList == null);
		mTreeNodeList = null;

		if (isTreeViewGraph()) {
			int referencedColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn));
			int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
			float min = 0;
			float max = 0;
			float dif = 0;
			if (strengthColumn != -1) {
				min = mTableModel.getMinimumValue(strengthColumn);
				max = mTableModel.getMaximumValue(strengthColumn);
				if (max == min) {
					strengthColumn = -1;
					}
				else {
					min -= 0.2 * (max - min);
					dif = max - min;
					}
				}

			if (mConnectionLineMap == null)
				mConnectionLineMap = createReferenceMap(mConnectionColumn, referencedColumn);

	   		for (int i=0; i<mDataPoints; i++)
	   			mPoint[i].exclusionFlags |= EXCLUSION_FLAG_DETAIL_GRAPH;

			if (mActivePoint == null) {
				mTreeNodeList = new VisualizationNode[0][];
				}
			else {
		   		mActivePoint.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
		   		
				VisualizationNode[] rootShell = new VisualizationNode[1];
				rootShell[0] = createVisualizationNode(mActivePoint, null, 1.0f);
	
				ArrayList<VisualizationNode[]> shellList = new ArrayList<VisualizationNode[]>();
				shellList.add(rootShell);
	
				// create array lists for every shell
				for (int shell=1; shell<=mTreeViewRadius; shell++) {
					ArrayList<VisualizationNode> vpList = new ArrayList<VisualizationNode>();
					for (VisualizationNode parent:shellList.get(shell-1)) {
						byte[] data = (byte[])parent.getVisualizationPoint().record.getData(mConnectionColumn);
						if (data != null) {
							String[] entry = mTableModel.separateEntries(new String(data));
							String[] strength = null;
							if (strengthColumn != cColumnUnassigned) {
								byte[] strengthData = (byte[])parent.getVisualizationPoint().record.getData(strengthColumn);
								if (strengthData != null) {
									strength = mTableModel.separateEntries(new String(strengthData));
									if (strength.length != entry.length)
										strength = null;
									}
								}
							int firstChildIndex = vpList.size();
							for (int i=0; i<entry.length; i++) {
								String ref = entry[i];
								VisualizationPoint vp = mConnectionLineMap.get(ref.getBytes());
								if (vp != null) {
									// if we don't have connection strength information and the child is already connected to another parent
									if (strengthColumn == cColumnUnassigned && (vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0)
										continue;
	
									float strengthValue = 1.0f;
									if (strength != null) {
										try {
											float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strength[i], strengthColumn)));
											strengthValue = Float.isNaN(value) ? 0.0f : (float)((value-min) / dif);
											}
										catch (NumberFormatException nfe) {}
										}
	
									VisualizationNode childNode = null;
	
									// if we have a strength value and the child is already connected compare strength values
									if ((vp.exclusionFlags & EXCLUSION_FLAG_DETAIL_GRAPH) == 0) {
										for (int j=0; j<firstChildIndex; j++) {
											if (vpList.get(j).getVisualizationPoint() == vp) {
												if (vpList.get(j).getStrength() < strengthValue) {
													childNode = vpList.get(j);
													vpList.remove(childNode);
													firstChildIndex--;
													childNode.setParentNode(parent);
													childNode.setStrength(strengthValue);
													}
												break;
												}
											}
										if (childNode == null)
											continue;
										}
									else {
										vp.exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
										childNode = createVisualizationNode(vp, parent, strengthValue);
										}
	
									int insertIndex = firstChildIndex;
									while (insertIndex < vpList.size() && childNode.getStrength() <= vpList.get(insertIndex).getStrength())
										insertIndex++;
	
									vpList.add(insertIndex, childNode);
									}
								}
							}
						}
	
					if (vpList.size() == 0)
						break;
	
					shellList.add(vpList.toArray(new VisualizationNode[0]));
					}
	
				mTreeNodeList = shellList.toArray(new VisualizationNode[0][]);
				}
			}

		if (!oldWasNull && mTreeNodeList == null) {
	   		for (int i=0; i<mDataPoints; i++)
	   			mPoint[i].exclusionFlags &= ~EXCLUSION_FLAG_DETAIL_GRAPH;
			}

		if (!oldWasNull || mTreeNodeList != null) {
			// if we have a tree without any nodes (no chosen root) we don't want to hide invisible rows from other views
			mActiveExclusionFlags = (mTreeNodeList != null && mTreeNodeList.length == 0) ? 0 : EXCLUSION_FLAG_DETAIL_GRAPH;
			applyLocalExclusion(false);
			invalidateOffImage(true);
			}
		}

	private VisualizationNode createVisualizationNode(VisualizationPoint vp, VisualizationNode parent, float strength) {
		if (mTreeViewMode == cTreeViewModeHTree || mTreeViewMode == cTreeViewModeVTree)
			return new TreeVisualizationNode(vp, (TreeVisualizationNode)parent, strength);
		if (mTreeViewMode == cTreeViewModeRadial)
			return new RadialVisualizationNode(vp, (RadialVisualizationNode)parent, strength);
		return null;
		}

	protected void invalidateOffImage(boolean invalidateCoordinates) {
		if (invalidateCoordinates)
			mCoordinatesValid = false;
		mOffImageValid = false;
		repaint();
		}

	protected void calculateLegend(Rectangle bounds, int fontHeight) {
		if (mMarkerSizeColumn != cColumnUnassigned
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			mSizeLegend = new JVisualizationLegend(this, mTableModel,
													mMarkerSizeColumn,
													null,
													JVisualizationLegend.cLegendTypeSize);
			mSizeLegend.calculate(bounds, fontHeight);
			bounds.height -= mSizeLegend.getHeight();
			}
		else {
			mSizeLegend = null;
			}

		if (mMarkerShapeColumn != cColumnUnassigned
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			mShapeLegend = new JVisualizationLegend(this, mTableModel,
													mMarkerShapeColumn,
													null,
													JVisualizationLegend.cLegendTypeShapeCategory);
			mShapeLegend.calculate(bounds, fontHeight);
			bounds.height -= mShapeLegend.getHeight();
			}
		else {
			mShapeLegend = null;
			}

		if (mMarkerColor.getColorColumn() != cColumnUnassigned) {
			mColorLegend = new JVisualizationLegend(this, mTableModel,
													mMarkerColor.getColorColumn(),
													mMarkerColor,
													mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories ?
													  JVisualizationLegend.cLegendTypeColorCategory
													: JVisualizationLegend.cLegendTypeColorDouble);
			mColorLegend.calculate(bounds, fontHeight);
			bounds.height -= mColorLegend.getHeight();
			}
		else {
			mColorLegend = null;
			}
		}

	protected void paintLegend(Rectangle bounds) {
		if (mColorLegend != null)
			mColorLegend.paint(bounds);

		if (mSizeLegend != null)
			mSizeLegend.paint(bounds);

		if (mShapeLegend != null)
			mShapeLegend.paint(bounds);
		}

	protected void paintTouchIcon(Graphics g) {
		if (mTouchFunctionActive) {
			g.setColor(Color.red);
	//		g.setFont(Font.);
			g.drawString("touch", 10, 20);
			}
		}

	public void setDetailPopupProvider(DetailPopupProvider p) {
		mDetailPopupProvider = p;
		}

	/**
	 * This returns the <b>indended</b> case separation column that was defined with
	 * setCaseSeparation(). Whether the view really separates cases by applying a case
	 * specific shift to markers, bars or boxes, depends on other view settings.
	 * This shift is not applied, if<br>
	 * - the categories are already separated, because the same column is also assigned to an axis.<br>
	 * - none of the axes is unassigned or assigned to another category column with a low number of categories.<br>
	 * Call isCaseSeparationDone() to find out, whether the view applies a case specific shift.
	 * @return
	 */
	public int getCaseSeparationColumn() {
		return mCaseSeparationColumn;
		}

	public float getCaseSeparationValue() {
		return mCaseSeparationValue;
		}

	/**
	 * Checks, whether a case separation column was defined and the view applies
	 * a case specific shift to all markers, bars or boxes.
	 * Case separation is not done, if the case separation column is also assigned to an axis.
	 * Case separation is not possible if we have no category axis or unassigned axis.
	 * @return true, if the view applies a case specific shift to markers, bars or boxes
	 */
	public boolean isCaseSeparationDone() {
		if (mCaseSeparationColumn == cColumnUnassigned)
			return false;

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] == mCaseSeparationColumn)
				return false;	// don't separate cases, if we have them separated on one axis anyway

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] == cColumnUnassigned
			 || (mTableModel.isColumnTypeCategory(mAxisIndex[axis])
			  && mTableModel.getCategoryCount(mAxisIndex[axis]) <= cMaxCaseSeparationCategoryCount))
				return true;

		return false;
		}

	protected int getCaseSeparationAxis() {
		if (!isCaseSeparationDone())
			return -1;

		int preferredAxis = -1;
		int preferredCategoryCount = Integer.MAX_VALUE;
		for (int axis=0; axis<mDimensions; axis++) {
			if ((mChartType != cChartTypeBoxPlot
			  && mChartType != cChartTypeWhiskerPlot)
			 || ((BoxPlotViewInfo)mChartInfo).barAxis != axis) {
				int column = mAxisIndex[axis];
				if (column == cColumnUnassigned) {
					if (preferredCategoryCount > 1) {
						preferredAxis = axis;
						preferredCategoryCount = 1;
						}
					}
				else if (mTableModel.isColumnTypeCategory(column)
					  && mTableModel.getCategoryCount(column) <= cMaxCaseSeparationCategoryCount
					  && preferredCategoryCount > mTableModel.getCategoryCount(column)) {
					preferredAxis = axis;
					preferredCategoryCount = mTableModel.getCategoryCount(column);
					}
				}
			}
		return preferredAxis;
		}

	@Override
	public int getFocusHitlist() {
		return mFocusHitlist;
		}

	protected int getFocusFlag() {
		return (mFocusHitlist == cHitlistUnassigned) ? -1
			 : (mFocusHitlist == cFocusOnSelection) ? CompoundRecord.cFlagSelected
			 : mTableModel.getHitlistHandler().getHitlistFlagNo(mFocusHitlist);
		}

	public float getFontSize() {
		return mRelativeFontSize;
		}

	public int getMarkerShapeColumn() {
		return mMarkerShapeColumn;
		}

	public float getJittering() {
		return mMarkerJittering;
		}

	public float getMarkerSize() {
		return mRelativeMarkerSize;
		}

	public int getMarkerSizeColumn() {
		return mMarkerSizeColumn;
		}

	public boolean getMarkerSizeInversion() {
		return mMarkerSizeInversion;
		}

	public boolean getMarkerSizeProportional() {
		return mMarkerSizeProportional;
		}

	public boolean isMarkerSizeZoomAdapted() {
		return !Float.isNaN(mMarkerSizeZoomAdaption);
		}

	public float getMarkerLabelSize() {
		return mMarkerLabelSize;
		}

	/**
	 * Calculates the font size for drawing a marker label. Usually this depends on
	 * default font size, general font size factor, marker label font size factor.
	 * However, we have a central label instead of a marker, and if the marker size column
	 * is set, then the central label's font size is defined by the general marker label size setting.
	 * @param vp
	 * @param position cMidCenter or other
	 * @return
	 */
	protected float getLabelFontSize(VisualizationPoint vp, int position, boolean isTreeView) {
		if (mMarkerSizeColumn != cColumnUnassigned
		 && position == cMidCenter
		 && !isTreeView) {
			float fontsize = getMarkerSize(vp) / 2.0f;
			float factor = 0.5f*(mMarkerLabelSize+1.0f); // reduce effect by factor 2.0
			return Math.max(3f, mRelativeFontSize*factor*fontsize);
			}

		return mMarkerLabelSize * mFontHeight;
		}

	/**
	 * Calculates the average bond length used for molecule scaling, if a label
	 * displays the chemical structure. This method is based on getLabelFontSize()
	 * and, thus, used the same logic.
	 * @param vp
	 * @param position cMidCenter or other
	 * @return
	 */
	protected float getLabelAVBL(VisualizationPoint vp, int position, boolean isTreeView) {
		return getLabelFontSize(vp, position, isTreeView) / 2f;
		}

	/**
	 * Calculates the absolute marker size of the visualization point, which depends
	 * on a view size specific base value (mAbsoluteMarkerSize), a user changeable factor
	 * (mRelativeMarkerSize) and optionally another factor derived from a column value
	 * defined to influence the marker size.
	 * @param vp
	 * @return
	 */
	protected float getMarkerSize(VisualizationPoint vp) {
		if (mMarkerSizeColumn == cColumnUnassigned) {
			float size = Float.isNaN(mMarkerSizeZoomAdaption) ? mAbsoluteMarkerSize : mAbsoluteMarkerSize * mMarkerSizeZoomAdaption;
			return validateSizeWithConnections(size);
			}

		if (CompoundTableHitlistHandler.isHitlistColumn(mMarkerSizeColumn))
			return getMarkerSizeFromHitlistMembership(vp.record.isFlagSet(
				mTableModel.getHitlistHandler().getHitlistFlagNo(CompoundTableHitlistHandler.getHitlistFromColumn(mMarkerSizeColumn))));

		return getMarkerSizeFromValue(vp.record.getDouble(mMarkerSizeColumn));
		}

	protected float getMarkerSizeFromHitlistMembership(boolean isMember) {
		float size = (isMember ^ mMarkerSizeInversion) ?
				(int)(mAbsoluteMarkerSize * 1.2) : (int)(mAbsoluteMarkerSize * 0.6);
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			size *= mMarkerSizeZoomAdaption;
		return validateSizeWithConnections(size);
		}

	protected float getMarkerSizeFromValue(float value) {
		float factor = getMarkerSizeVPFactor(value, mMarkerSizeColumn);
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			factor *= mMarkerSizeZoomAdaption;
		float size = (int)(factor*mAbsoluteMarkerSize);
		return validateSizeWithConnections(size);
		}

	/**
	 * Calculates the marker size factor from the absolute(!) row value normalized
	 * by the maximum of all absolute values. Returned is 2*sqrt(value/max)) assuming that
	 * the factor is used on two marker dimensions to make the marker area proportional
	 * to the value.
	 * @param value
	 * @param invert
	 * @param valueColumn
	 * @return 0.0 -> 2.0
	 */
	protected float getMarkerSizeVPFactor(float value, int valueColumn) {
		if (mMarkerSizeProportional) {
			float min = mTableModel.getMinimumValue(valueColumn);
			float max = mTableModel.getMaximumValue(valueColumn);
			if (mTableModel.isLogarithmicViewMode(valueColumn)) {
				max = (float)Math.pow(10, max);
				value = (float)Math.pow(10, value);
				}
			else {
				max = Math.max(Math.abs(min), Math.abs(max));
				value = Math.abs(value);
				}
			return 2f*(float)Math.sqrt((0.01+(mMarkerSizeInversion?max-value:value)) / (1.01*max));
			}
		else {
			float min = mTableModel.getMinimumValue(valueColumn);
			float max = mTableModel.getMaximumValue(valueColumn);
			return 2f*(float)Math.sqrt((0.04+(mMarkerSizeInversion?max-value:value-min)) / (1.04*(max-min)));
			}
		}

	/**
	 * Make marker size not smaller than 1.5 unless<br>
	 * - marker sizes are not modulated by a column value<br>
	 * - and connection lines are shown that are thicker than the marker<br>
	 * This allows reduce marker size to 0, if connection lines are shown.
	 * @param updated size
	 * @return
	 */
	private float validateSizeWithConnections(float size) {
		if (size < 1.5) {
			if (mMarkerSizeColumn != cColumnUnassigned
			 || mConnectionColumn == cColumnUnassigned
			 || mAbsoluteConnectionLineWidth < size)
				size = 1.5f;
			}

		return size;
		}

	public void colorChanged(VisualizationColor source) {
		if (source == mMarkerColor) {
			updateColorIndices();
			}
		}

	protected boolean isLegendLayoutValid(JVisualizationLegend legend, VisualizationColor color) {
		if (legend == null || color.getColorColumn() == cColumnUnassigned)
			return legend == null && color.getColorColumn() == cColumnUnassigned;

		return legend.layoutIsValid(color.getColorListMode() == VisualizationColor.cColorListModeCategories, color.getColorListSizeWithoutDefaults());
		}

	public VisualizationColor getMarkerColor() {
		return mMarkerColor;
		}

	private void updateColorIndices() {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned)
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = VisualizationColor.cDefaultDataColorIndex;
		else if (CompoundTableHitlistHandler.isHitlistColumn(mMarkerColor.getColorColumn())) {
			int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mMarkerColor.getColorColumn());
			int flagNo = mTableModel.getHitlistHandler().getHitlistFlagNo(hitlistIndex);
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = (byte)(mPoint[i].record.isFlagSet(flagNo) ?
						VisualizationColor.cSpecialColorCount : VisualizationColor.cSpecialColorCount + 1);
			}
		else if (mTableModel.isDescriptorColumn(mMarkerColor.getColorColumn()))
			setSimilarityColors();
		else if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = (byte)(VisualizationColor.cSpecialColorCount
					+ mTableModel.getCategoryIndex(mMarkerColor.getColorColumn(), mPoint[i].record));
			}
		else if (mTableModel.isColumnTypeDouble(mMarkerColor.getColorColumn())) {
			float min = Float.isNaN(mMarkerColor.getColorMin()) ?
									mTableModel.getMinimumValue(mMarkerColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mMarkerColor.getColorColumn())) ?
							   (float)Math.log10(mMarkerColor.getColorMin()) : mMarkerColor.getColorMin();
			float max = Float.isNaN(mMarkerColor.getColorMax()) ?
									mTableModel.getMaximumValue(mMarkerColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mMarkerColor.getColorColumn())) ?
							   (float)Math.log10(mMarkerColor.getColorMax()) : mMarkerColor.getColorMax();

			//	1. colorMin is explicitly set; max is real max, but lower than min
			// or 2. colorMax is explicitly set; min is real min, but larger than max
			// first case is OK, second needs adaption below to be handled as indented
			if (min >= max)  
				if (!Float.isNaN(mMarkerColor.getColorMax()))
					min = Float.MIN_VALUE;

			for (int i=0; i<mDataPoints; i++) {
				float value = mPoint[i].record.getDouble(mMarkerColor.getColorColumn());
				if (Float.isNaN(value))
					mPoint[i].colorIndex = VisualizationColor.cMissingDataColorIndex;
				else if (value <= min)
					mPoint[i].colorIndex = (byte)VisualizationColor.cSpecialColorCount;
				else if (value >= max)
					mPoint[i].colorIndex = (byte)(mMarkerColor.getColorList().length-1);
				else
					mPoint[i].colorIndex = (byte)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mMarkerColor.getColorList().length-VisualizationColor.cSpecialColorCount-1)
						* (value - min) / (max - min));
				}
			}

		invalidateOffImage(true);
		}

	private void setSimilarityValues(int axis) {
		int column = mAxisIndex[axis];
		if (mActivePoint == null) {
			mAxisSimilarity[axis] = null;
			}
		else {
			mAxisSimilarity[axis] = null;
			if (DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(column))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				Object descriptor = mActivePoint.record.getData(column);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					mAxisSimilarity[axis] = createSimilarityListSMP(descriptor, column);
					}
				}
			else {
				mAxisSimilarity[axis] = new float[mDataPoints];
				for (int i=0; i<mDataPoints; i++)
					mAxisSimilarity[axis][mPoint[i].record.getID()] =
						(float)mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
				}
			}
		invalidateOffImage(true);
		}

	private void setSimilarityColors() {
		if (mActivePoint == null)
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].colorIndex = VisualizationColor.cDefaultDataColorIndex;
		else {
			float min = Float.isNaN(mMarkerColor.getColorMin()) ? 0.0f : mMarkerColor.getColorMin();
			float max = Float.isNaN(mMarkerColor.getColorMax()) ? 1.0f : mMarkerColor.getColorMax();
			if (min >= max) {
				min = 0.0f;
				max = 1.0f;
				}

			int column = mMarkerColor.getColorColumn();
			float[] flexophoreSimilarity = null;
			if (DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(column))) {
				// if we have the slow 3DPPMM2 then use a progress dialog and multi-threading
				Object descriptor = mActivePoint.record.getData(column);
				if (descriptor != null) {
					Component c = this;
					while (!(c instanceof Frame))
						c = c.getParent();

					flexophoreSimilarity = createSimilarityListSMP(descriptor, column);
					if (flexophoreSimilarity == null) {	// cancelled
						mMarkerColor.setColor(cColumnUnassigned);
						return;
						}
					}
				}

			for (int i=0; i<mDataPoints; i++) {
				float similarity = (flexophoreSimilarity != null) ? flexophoreSimilarity[i]
								  : mTableModel.getDescriptorSimilarity(mActivePoint.record, mPoint[i].record, column);
				if (Float.isNaN(similarity))
					mPoint[i].colorIndex = VisualizationColor.cMissingDataColorIndex;
				else if (similarity <= min)
					mPoint[i].colorIndex = (byte)VisualizationColor.cSpecialColorCount;
				else if (similarity >= max)
					mPoint[i].colorIndex = (byte)(mMarkerColor.getColorList().length-1);
				else
					mPoint[i].colorIndex = (byte)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mMarkerColor.getColorList().length - VisualizationColor.cSpecialColorCount - 1)
						* (similarity - min) / (max - min));
				}
			}
		}

	private float[] createSimilarityListSMP(Object descriptor, int descriptorColumn) {
		Component c = this;
		while (!(c instanceof Frame))
			c = c.getParent();

		JProgressDialog progressDialog = new JProgressDialog((Frame)c) {
			private static final long serialVersionUID = 0x20110325;

			public void stopProgress() {
				super.stopProgress();
				close();
				}
			};

	   	mTableModel.createSimilarityListSMP(null, descriptor, descriptorColumn, progressDialog);
	   	progressDialog.setVisible(true);

		return mTableModel.getSimilarityListSMP();
 		}

	private void updateSplittingIndices() {
		int count1 = (mSplittingColumn[0] == cColumnUnassigned) ? 1 : mTableModel.getCategoryCount(mSplittingColumn[0]);
		int count2 = (mSplittingColumn[1] == cColumnUnassigned) ? 1 : mTableModel.getCategoryCount(mSplittingColumn[1]);
		mHVCount = count1 * count2;

		if (mSplittingColumn[0] == cColumnUnassigned) {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].hvIndex = 0;
			}
		else if (mSplittingColumn[1] == cColumnUnassigned) {
			if (CompoundTableHitlistHandler.isHitlistColumn(mSplittingColumn[0])) {
				int flagNo = mTableModel.getHitlistHandler().getHitlistFlagNo(CompoundTableHitlistHandler.getHitlistFromColumn(mSplittingColumn[0]));
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = (byte)(mPoint[i].record.isFlagSet(flagNo) ? 0 : 1);
				}
			else {
				for (int i=0; i<mDataPoints; i++)
					mPoint[i].hvIndex = (byte)mTableModel.getCategoryIndex(mSplittingColumn[0], mPoint[i].record);
				}
			}
		else {
			int flagNo1 = -1;
			if (CompoundTableHitlistHandler.isHitlistColumn(mSplittingColumn[0]))
				flagNo1 = mTableModel.getHitlistHandler().getHitlistFlagNo(CompoundTableHitlistHandler.getHitlistFromColumn(mSplittingColumn[0]));

			int flagNo2 = -1;
			if (CompoundTableHitlistHandler.isHitlistColumn(mSplittingColumn[1]))
				flagNo2 = mTableModel.getHitlistHandler().getHitlistFlagNo(CompoundTableHitlistHandler.getHitlistFromColumn(mSplittingColumn[1]));

			for (int i=0; i<mDataPoints; i++) {
				int index1 = (flagNo1 != -1) ?
							   (mPoint[i].record.isFlagSet(flagNo1) ? 0 : 1)
							 : mTableModel.getCategoryIndex(mSplittingColumn[0], mPoint[i].record);
				int index2 = (flagNo2 != -1) ?
							   (mPoint[i].record.isFlagSet(flagNo2) ? 0 : 1)
							 : mTableModel.getCategoryIndex(mSplittingColumn[1], mPoint[i].record);
				mPoint[i].hvIndex = (byte)(index1 + index2 * count1);
				}
			}

		invalidateOffImage(true);
		}

	public Color getTitleBackground() {
		return mTitleBackground;
		}

	public void setTitleBackground(Color c) {
		if (!mTitleBackground.equals(c)) {
			mTitleBackground = c;
			invalidateOffImage(false);
			}
		}

	public Color getViewBackground() {
		return (mViewBackground == null) ? Color.WHITE : mViewBackground;
		}

	/**
	 * Defines the background color
	 * @param c (null for WHITE)
	 * @return whether there was a change
	 */
	public boolean setViewBackground(Color c) {
		if (Color.WHITE.equals(c))
			c = null;
		if ((c != null && !c.equals(mViewBackground))
		 || (c == null && mViewBackground != null)) {
			mViewBackground = c;
			invalidateOffImage(false);
			return true;
			}
		return false;
		}

	/**
	 * Generates a neutral grey with given contrast to the current background.
	 * @param contrast 0.0 (not visible) to 1.0 (full contrast)
	 * @return
	 */
	protected Color getContrastGrey(float contrast) {
		return getContrastGrey(contrast, mViewBackground);
		}

	/**
	 * Generates a neutral grey with given contrast to given color.
	 * @param contrast 0.0 (not visible) to 1.0 (full contrast)
	 * @return
	 */
	protected Color getContrastGrey(float contrast, Color color) {
		float brightness = ColorHelper.perceivedBrightness(color);
		float range = (brightness > 0.5) ? brightness : 1f-brightness;

		// enhance contrast for middle bright backgrounds
		contrast = (float)Math.pow(contrast, range);

		return (brightness > 0.5) ?
				  Color.getHSBColor(0.0f, 0.0f, brightness - range*contrast)
				: Color.getHSBColor(0.0f, 0.0f, brightness + range*contrast);
		}

	@Override
	public void setFocusHitlist(int hitlistIndex) {
		if (mFocusHitlist != hitlistIndex) {
			mFocusHitlist = hitlistIndex;
			invalidateOffImage(mChartType != cChartTypeScatterPlot);	// no of colors in mChartInfo needs to be updated
			}
		}

	/**
	 * This is the user defined relative font size factor applied to marker and scale labels.
	 * Note: Marker labels are also affected by setMarkerLabelSize().
	 * @param size
	 * @param isAdjusting
	 */
	public void setFontSize(float size, boolean isAdjusting) {
		if (mRelativeFontSize != size) {
			mRelativeFontSize = size;
			invalidateOffImage(true);
			}
		}

	public void setJittering(float jittering, boolean isAdjusting) {
		if (mMarkerJittering != jittering) {
			mMarkerJittering = jittering;
			invalidateOffImage(true);
			}
		}

	public void setCaseSeparation(int column, float value, boolean isAdjusting) {
		if (column >= 0
		 && (!mTableModel.isColumnTypeCategory(column)
		  || mTableModel.getCategoryCount(column) > cMaxCaseSeparationCategoryCount))
			column = cColumnUnassigned;

		if (mCaseSeparationColumn != column
		 || (mCaseSeparationColumn != cColumnUnassigned && mCaseSeparationValue != value)) {
			mCaseSeparationColumn = column;
			mCaseSeparationValue = value;
		   	validateExclusion(determineChartType());
			invalidateOffImage(true);
			}
		}

	public boolean isSplitView() {
		return mSplittingColumn[0] != cColumnUnassigned;
		}

	public int[] getSplittingColumns() {
		return mSplittingColumn;
		}

	public void setSplittingColumns(int column1, int column2) {
		if (column1 == cColumnUnassigned
		 && column2 != cColumnUnassigned) {
			column1 = column2;
			column2 = cColumnUnassigned;
			}

		if (mSplittingColumn[0] != column1 || mSplittingColumn[1] != column2) {
			if ((column1 == cColumnUnassigned
			  || mTableModel.isColumnTypeCategory(column1))
			 && (column2 == cColumnUnassigned
			  || mTableModel.isColumnTypeCategory(column2))) {
				mSplittingColumn[0] = column1;
				mSplittingColumn[1] = column2;
				updateSplittingIndices();
				}
			}
		}

	/** Determine current mean/median setting for box and whisker plots
	 * @return mode or BOXPLOT_DEFAULT_MEAN_MODE if not box/whisker plot
	 */
	public int getBoxplotMeanMode() {
		return (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) ? mBoxplotMeanMode : BOXPLOT_DEFAULT_MEAN_MODE;
		}

	/**
	 * Defines whether mean and/or median are have indicators in box/whisker plots.
	 * @param mode
	 */
	public void setBoxplotMeanMode(int mode) {
		if (mBoxplotMeanMode != mode) {
			mBoxplotMeanMode = mode;
			invalidateOffImage(false);
			}
		}

	/** Determines whether mean and/or median values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot. 
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowMeanAndMedianValues() {
		return (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot)
			  && mBoxplotShowMeanAndMedianValues;
		}

	/**
	 * Defines whether mean and/or median are shown in box/whisker plots.
	 * @param b
	 */
	public void setShowMeanAndMedianValues(boolean b) {
		if (mBoxplotShowMeanAndMedianValues != b) {
			mBoxplotShowMeanAndMedianValues = b;
			invalidateOffImage(false);
			}
		}

	/** Determines whether p-values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot
	 *  or if no proper p-value column is assigned.
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowPValue() {
		return (mChartType == cChartTypeBoxPlot
	   		 || mChartType == cChartTypeWhiskerPlot)
	   		&& mBoxplotShowPValue
   			&& isValidPValueColumn(mPValueColumn);
   		}

	/**
	 * Defines whether p-values are shown in box/whisker plots.
	 * For showing p-values one also needs to call setPValueColumn()
	 * @param b
	 */
	public void setShowPValue(boolean b) {
		if (mBoxplotShowPValue != b) {
			mBoxplotShowPValue = b;
			if (isValidPValueColumn(mPValueColumn))
				invalidateOffImage(true);	// to recalculate p-values
			}
		}

	/** Determines whether fold-change values are shown in a current box or whisker plot.
	 *  Returns false if the current plot is neither box nor whisker plot
	 *  or if no proper p-value column is assigned.
	 * @return false if the current box or whisker plot shows 
	 */
	public boolean isShowFoldChange() {
		return (mChartType == cChartTypeBoxPlot
	   		 || mChartType == cChartTypeWhiskerPlot)
	   		&& mBoxplotShowFoldChange
   			&& isValidPValueColumn(mPValueColumn);
   		}

	/**
	 * Defines whether fold-change values are shown in box/whisker plots.
	 * For showing fold-change values one also needs to call setPValueColumn()
	 * @param b
	 */
	public void setShowFoldChange(boolean b) {
		if (mBoxplotShowFoldChange != b) {
			mBoxplotShowFoldChange = b;
			if (isValidPValueColumn(mPValueColumn))
				invalidateOffImage(true);	// to recalculate fold change
			}
		}

	/**
	 * Returns the currently applied p-value column.
	 * @return
	 */
	public int getPValueColumn() {
		return isValidPValueColumn(mPValueColumn) ? mPValueColumn : cColumnUnassigned;
		}

	/**
	 * Returns the currently applied reference category for p-value calculation.
	 * @return
	 */
	public String getPValueRefCategory() {
		return (getPValueColumn() == cColumnUnassigned) ? null : mPValueRefCategory;
		}

	public boolean isValidPValueColumn(int column) {
		if (column == cColumnUnassigned)
			return false;

		if (mChartType == cChartTypeBoxPlot
		 || mChartType == cChartTypeWhiskerPlot) {
			if (column == getCaseSeparationColumn())
				return true;

			if (column == mSplittingColumn[0]
			 || column == mSplittingColumn[1])
				return true;

			for (int axis=0; axis<mDimensions; axis++)
				if (column == mAxisIndex[axis]
				 && mCategoryVisibleCount[axis] >= 2
				 && axis != mChartInfo.barAxis)
					return true;
			}

		return false;
		}

	public void setPValueColumn(int column, String refCategory) {
		if (column == cColumnUnassigned)
			refCategory = null;
		else if (refCategory == null)
			column = cColumnUnassigned;

		if (column != mPValueColumn
		 || (refCategory != null && !refCategory.equals(mPValueRefCategory))) {
			mPValueColumn = column;
			mPValueRefCategory = refCategory;
			invalidateOffImage(true);
			}
		}

	/**
	 * In fast rendering mode anti-aliasing is switched off
	 * @return whether we are in fast render mode
	 */
	public boolean isFastRendering() {
		return mIsFastRendering;
		}

	/**
	 * In fast rendering mode anti-aliasing is switched off
	 * @param v
	 */
	public void setFastRendering(boolean v) {
		if (mIsFastRendering != v) {
			mIsFastRendering = v;
			invalidateOffImage(false);
			}
		}

	/**
	 * Determines the type of the drawn chart and visible ranges on all axes.
	 * This depends on the preferred chart type, on the columns being assigned
	 * to the axes, the number of existing categories within these columns,
	 * and the current pruning bar settings.
	 * If any of these change, then this method needs to be called.
	 * @return local exclusion update needs (EXCLUSION_FLAG_ZOOM_0 left shifted according to axis)
	 */
	private int determineChartType() {
		boolean[] wasCatagoryAxis = new boolean[mDimensions];
		for (int axis=0; axis<mDimensions; axis++)
			wasCatagoryAxis[axis] = mIsCategoryAxis[axis];

		mChartType = cChartTypeScatterPlot;	// scatter plot is the default that is always possible
		for (int axis=0; axis<mDimensions; axis++)
			mIsCategoryAxis[axis] = (mAxisIndex[axis] != cColumnUnassigned
								  && mTableModel.isColumnTypeCategory(mAxisIndex[axis])
								  && !mTableModel.isColumnTypeDouble(mAxisIndex[axis]));

		if (mPreferredChartType == cChartTypeScatterPlot) {
			int csAxis = getCaseSeparationAxis();
			if (csAxis != -1)
				mIsCategoryAxis[csAxis] = true;
			}
		else if (mPreferredChartType == cChartTypeBoxPlot
			  || mPreferredChartType == cChartTypeWhiskerPlot) {
			int boxPlotDoubleAxis = determineBoxPlotDoubleAxis();
			if (boxPlotDoubleAxis != -1) {
				mChartType = mPreferredChartType;
				for (int axis=0; axis<mDimensions; axis++)
					mIsCategoryAxis[axis] = (axis != boxPlotDoubleAxis);
				}
			}
		else if (mPreferredChartType == cChartTypeBars
			  || mPreferredChartType == cChartTypePies) {
			boolean allQualify = true;
			for (int axis=0; axis<mDimensions; axis++)
				if (!qualifiesAsChartCategory(axis))
					allQualify = false;

			if (allQualify) {
				int categoryCount = 1;
				for (int axis=0; axis<mDimensions; axis++)
					if (mAxisIndex[axis] != cColumnUnassigned)
						categoryCount *= mTableModel.getCategoryCount(mAxisIndex[axis]);

				if (categoryCount <= cMaxTotalChartCategoryCount) {
					mChartType = mPreferredChartType;
					for (int axis=0; axis<mDimensions; axis++)
						mIsCategoryAxis[axis] = (mAxisIndex[axis] != cColumnUnassigned);
					}
				}
			}

		if (mTreeNodeList == null) {			// no tree view
			mActiveExclusionFlags = 0xFF;	// masks out NaN flags for axes that show categories
			for (int axis=0; axis<mDimensions; axis++)
				if (mIsCategoryAxis[axis])
					mActiveExclusionFlags &= ~(byte)(EXCLUSION_FLAG_NAN_0 << axis);
			}
		else if (mTreeNodeList.length == 0) {	// we have an empty tree view
			mActiveExclusionFlags = 0;
			}
		else {									// tree view with at least one node
			mActiveExclusionFlags = EXCLUSION_FLAG_DETAIL_GRAPH;
			}

		int localExclusionNeeds = 0;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mIsCategoryAxis[axis] != wasCatagoryAxis[axis])
				localExclusionNeeds |= (EXCLUSION_FLAG_ZOOM_0 << axis);

			calculateVisibleRange(axis);
			}

		return localExclusionNeeds;
		}

	/**
	 * Updates the NaN and zoom flags of local exclusion for all axes where needed
	 * and applies the local to the global exclusion, if local NaN or zoom flags were updated.
	 * @param localExclusionNeeds EXCLUSION_FLAG_NAN_0 and EXCLUSION_FLAG_ZOOM_0 left shifted according to axis
	 */
	private void validateExclusion(int localExclusionNeeds) {
		boolean found = false;
		for (int axis=0; axis<mDimensions; axis++) {
			if ((localExclusionNeeds & (EXCLUSION_FLAG_NAN_0 << axis)) != 0) {
				found = true;
				initializeLocalExclusion(axis);
				}
			if (mAxisIndex[axis] != cColumnUnassigned
			 && (localExclusionNeeds & (EXCLUSION_FLAG_ZOOM_0 << axis)) != 0) {
				found = true;
				updateLocalZoomExclusion(axis);
				}
			}
		if (found)
		   	applyLocalExclusion(false);
		}

	/**
	 * Determines based on table model information and current axis assignment,
	 * which axis is the preferred double value axis.
	 * @return 0,1,-1 for x-axis, y-axis, none
	 */
	protected int determineBoxPlotDoubleAxis() {
		for (int i=0; i<mDimensions; i++)
			if (mAxisIndex[i] != cColumnUnassigned
			 && (mTableModel.isDescriptorColumn(mAxisIndex[i]) || mTableModel.isColumnTypeDouble(mAxisIndex[i]))
			 && !mTableModel.isColumnTypeCategory(mAxisIndex[i])
			 && qualifyOtherAsChartCategory(i))
				return i;

		int minCategoryCount = Integer.MAX_VALUE;
		int minCountAxis = -1;
		for (int i=0; i<mDimensions; i++) {
			if (mAxisIndex[i] != cColumnUnassigned
			 && mTableModel.isColumnTypeDouble(mAxisIndex[i])
			 && qualifyOtherAsChartCategory(i)) {
				int categoryCount = 1;
				for (int axis=0; axis<mDimensions; axis++)
					if (axis != i && mAxisIndex[axis] != cColumnUnassigned)
						categoryCount = mTableModel.getCategoryCount(mAxisIndex[axis]);

				if (minCategoryCount > categoryCount) {
					minCategoryCount = categoryCount;
					minCountAxis = i;
					}
				}
			}

		return minCountAxis;
		}

	private boolean qualifyOtherAsChartCategory(int axis) {
		for (int i=0; i<mDimensions; i++)
			if (axis != i && !qualifiesAsChartCategory(i))
				return false;
		return true;
		}

	protected boolean qualifiesAsChartCategory(int axis) {
		return (mAxisIndex[axis] == cColumnUnassigned
			 || (mTableModel.isColumnTypeCategory(mAxisIndex[axis])
			  && mTableModel.getCategoryCount(mAxisIndex[axis]) <= cMaxChartCategoryCount));
		}

	/**
	 * Calculates the visible range of the axis based on pruning bar settings,
	 * current graph type, logarithmic view mode.
	 * @param axis
	 * @param low 0.0 >= value >= high
	 * @param high low >= value >= 1.0
	 * @return whether the visible range has been changed
	 */
	private boolean calculateVisibleRange(int axis) {
		float visMin = -1.0f;
		float visMax =  1.0f;
		int column = mAxisIndex[axis];
		if (column != cColumnUnassigned) {
			if (mIsCategoryAxis[axis]) {
				int maxCategory = mTableModel.getCategoryCount(column) - 1;
				visMin = Math.round(mPruningBarLow[axis] * maxCategory) - 0.5f;
				visMax = Math.round(mPruningBarHigh[axis] * maxCategory) + 0.5f;
				}
			else if (mTableModel.isDescriptorColumn(column)) {
				visMin = mPruningBarLow[axis];
				visMax = mPruningBarHigh[axis];
				}
			else {
				float dataMin = mTableModel.getMinimumValue(column);
				float dataMax = mTableModel.getMaximumValue(column);
				float dataRange = dataMax - dataMin;
				visMin = (mPruningBarLow[axis] == 0.0f) ? dataMin : dataMin + mPruningBarLow[axis] * dataRange;
				visMax = (mPruningBarHigh[axis] == 1.0f) ? dataMax : dataMin + mPruningBarHigh[axis] * dataRange;
				}
			}

		if (visMin != mAxisVisMin[axis]
		 || visMax != mAxisVisMax[axis]) {
			mAxisVisMin[axis] = visMin;
			mAxisVisMax[axis] = visMax;
			return true;
			}

		return false;
		}

	/**
	 * Based on the currently selected visible range, the displayed chart type and axis usage,
	 * this method calculates the pruning bar value from 0.0 to 1.0 to reflect visible range low value.
	 * @param axis
	 * @return low value of visible range normalized to span 0.0 to 1.0
	 */
	public float getPruningBarLow(int axis) {
		int column = mAxisIndex[axis];
		if (column == cColumnUnassigned)
			return 0.0f;
		if (mTableModel.isDescriptorColumn(column))
			return mAxisVisMin[axis];
		if (mIsCategoryAxis[axis])
			return (float)Math.round(mAxisVisMin[axis] + 0.5f) / (float)mTableModel.getCategoryCount(column);
		else
			return (mAxisVisMin[axis] - mTableModel.getMinimumValue(column))
				 / (mTableModel.getMaximumValue(column) - mTableModel.getMinimumValue(column));
		}

	/**
	 * Based on the currently selected visible range, the displayed chart type and axis usage,
	 * this method calculates the pruning bar value from 0.0 to 1.0 to reflect visible range high value.
	 * @param axis
	 * @return high value of visible range normalized to span 0.0 to 1.0
	 */
	public float getPruningBarHigh(int axis) {
		int column = mAxisIndex[axis];
		if (column == cColumnUnassigned)
			return 1.0f;
		if (mTableModel.isDescriptorColumn(column))
			return mAxisVisMax[axis];
		if (mIsCategoryAxis[axis])
			return (float)Math.round(mAxisVisMax[axis] + 0.5f) / (float)mTableModel.getCategoryCount(column);
		else
			return (mAxisVisMax[axis] - mTableModel.getMinimumValue(column))
				 / (mTableModel.getMaximumValue(column) - mTableModel.getMinimumValue(column));
		}

	private float calculateZoomState() {
		float zoomState = 1.0f;
		int axisCount = 0;
		for (int axis=0; axis<mDimensions; axis++) {
			if (mAxisIndex[axis] != cColumnUnassigned) {
				zoomState *= Math.max(0.001f, mPruningBarHigh[axis] - mPruningBarLow[axis]);
				axisCount++;
				}
			}
		zoomState = (float)Math.pow(zoomState, 0.75f / (float)axisCount);
		return Math.min(100f, 1f / zoomState);
		}

	// TODO remove mCategoryMin,mCategoryMax,mCategoryVisibleCount
	protected void calculateCategoryCounts(int boxPlotDoubleAxis) {
		for (int axis=0; axis<mDimensions; axis++) {
			int column = mAxisIndex[axis];
			if (column == cColumnUnassigned || axis == boxPlotDoubleAxis) {
				mCategoryMin[axis] = 0;
				mCategoryMax[axis] = 1;
				mCategoryVisibleCount[axis] = 1;
				}
			else {
				mCategoryMin[axis] = Math.round(mAxisVisMin[axis] + 0.5f);
				mCategoryMax[axis] = Math.round(mAxisVisMax[axis] + 0.5f);
				mCategoryVisibleCount[axis] = mCategoryMax[axis] - mCategoryMin[axis];
				}
			}

		if (isCaseSeparationDone())
			mCaseSeparationCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
		else
			mCaseSeparationCategoryCount = 1;

		mCombinedCategoryCount = new int[1+mDimensions];
		mCombinedCategoryCount[0] = mCaseSeparationCategoryCount;
		for (int axis=0; axis<mDimensions; axis++)
			mCombinedCategoryCount[axis+1] = mCombinedCategoryCount[axis] * mCategoryVisibleCount[axis];
		}

	/**
	 * Based on axis column assignments and on hvIndices of VisualizationPoints
	 * this method assigns all visible VisualizationPoints to bars/pies and to color categories
	 * within these bars/pies. It also calculates relative bar/pie sizes.
	 * @param hvCount
	 */
	protected void calculateBarsOrPies() {
		calculateCategoryCounts(-1);

		Color[] colorList = mMarkerColor.getColorList();
		int focusFlagNo = getFocusFlag();
		int basicColorCount = colorList.length + 1;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);

		int catCount = mCaseSeparationCategoryCount;
		for (int count:mCategoryVisibleCount)
			catCount *= count;

		mChartInfo = new CategoryViewInfo(mHVCount, catCount, colorCount);

		mChartInfo.barAxis = 0;
		for (int axis=1; axis<mDimensions; axis++)
			if (mAxisIndex[axis] == cColumnUnassigned)
				mChartInfo.barAxis = axis;
			else if (mAxisIndex[mChartInfo.barAxis] != cColumnUnassigned
				  && mCategoryVisibleCount[axis] <= mCategoryVisibleCount[mChartInfo.barAxis])
				mChartInfo.barAxis = axis;

		for (int i=0; i<colorList.length; i++)
			mChartInfo.color[i] = colorList[i];
		mChartInfo.color[colorList.length] = VisualizationColor.cSelectedColor;
		if (focusFlagNo != -1) {
			for (int i=0; i<colorList.length; i++)
				mChartInfo.color[i+basicColorCount] = VisualizationColor.grayOutColor(colorList[i]);
			mChartInfo.color[colorList.length+basicColorCount] = VisualizationColor.grayOutColor(VisualizationColor.cSelectedColor);
			}

		int visibleCount = 0;
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int colorIndex = ((mPoint[i].record.getFlags() & CompoundRecord.cFlagMaskSelected) != 0
							   && mFocusHitlist != cFocusOnSelection) ?
									   colorList.length : mPoint[i].colorIndex;
				if (focusFlagNo != -1 && !mPoint[i].record.isFlagSet(focusFlagNo))
					colorIndex += basicColorCount;

				int cat = getChartCategoryIndex(mPoint[i]);
				mChartInfo.pointsInCategory[mPoint[i].hvIndex][cat]++;
				mChartInfo.pointsInColorCategory[mPoint[i].hvIndex][cat][colorIndex]++;
				visibleCount++;
				switch (mChartMode) {
				case cChartModeCount:
				case cChartModePercent:
					mChartInfo.barValue[mPoint[i].hvIndex][cat]++;
					break;
				case cChartModeMean:
				case cChartModeSum:
					mChartInfo.barValue[mPoint[i].hvIndex][cat] += mPoint[i].record.getDouble(mChartColumn);
					break;
				case cChartModeMin:
				case cChartModeMax:
					float value = mPoint[i].record.getDouble(mChartColumn);
					if (mChartInfo.pointsInCategory[mPoint[i].hvIndex][cat] == 1)
						mChartInfo.barValue[mPoint[i].hvIndex][cat] = value;
					else if (mChartMode == cChartModeMin)
						mChartInfo.barValue[mPoint[i].hvIndex][cat] = Math.min(mChartInfo.barValue[mPoint[i].hvIndex][cat], value);
					else
						mChartInfo.barValue[mPoint[i].hvIndex][cat] = Math.max(mChartInfo.barValue[mPoint[i].hvIndex][cat], value);
					break;
					}
				}
			}
		if (mChartMode == cChartModePercent)
			for (int i=0; i<mHVCount; i++)
				for (int j=0; j<catCount; j++)
	   				mChartInfo.barValue[i][j] *= 100f / visibleCount;
		if (mChartMode == cChartModeMean)
			for (int i=0; i<mHVCount; i++)
				for (int j=0; j<catCount; j++)
					if (mChartInfo.pointsInCategory[i][j] != 0)
						mChartInfo.barValue[i][j] /= mChartInfo.pointsInCategory[i][j];

		int[][][] count = new int[mHVCount][catCount][colorCount];
		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				for (int color=1; color<colorCount; color++)
					count[hv][cat][color] = count[hv][cat][color-1]+mChartInfo.pointsInColorCategory[hv][cat][color-1];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int colorIndex = (mPoint[i].record.isSelected()
							   && mFocusHitlist != cFocusOnSelection) ?
									   colorList.length : mPoint[i].colorIndex;
				if (focusFlagNo != -1 && !mPoint[i].record.isFlagSet(focusFlagNo))
					colorIndex += basicColorCount;

				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				mPoint[i].chartGroupIndex = count[hv][cat][colorIndex];
				count[hv][cat][colorIndex]++;
				}
			}

		float dataMin = Float.POSITIVE_INFINITY;
		float dataMax = Float.NEGATIVE_INFINITY;
		for (int i=0; i<mHVCount; i++) {
			for (int j=0; j<catCount; j++) {
				if (mChartInfo.pointsInCategory[i][j] != 0) {
					if (dataMin > mChartInfo.barValue[i][j])
						dataMin = mChartInfo.barValue[i][j];
					if (dataMax < mChartInfo.barValue[i][j])
						dataMax = mChartInfo.barValue[i][j];
					}
				}
			}
		mChartInfo.barOrPieDataAvailable = (dataMin != Float.POSITIVE_INFINITY);

		if (mChartInfo.barOrPieDataAvailable) {
			switch (mChartMode) {
			case cChartModeCount:
			case cChartModePercent:
				mChartInfo.axisMin = 0.0f;
				mChartInfo.axisMax = dataMax * cBarSpacingFactor;
				mChartInfo.barBase = 0.0f;
				break;
			default:
				if (mTableModel.isLogarithmicViewMode(mChartColumn)) {
					float dataRange = dataMax - dataMin;
					mChartInfo.axisMin = dataMin - dataRange * (cBarSpacingFactor - 1.0f);
					mChartInfo.axisMax = dataMax + dataRange * (cBarSpacingFactor - 1.0f);
					mChartInfo.barBase = mChartInfo.axisMin;
					}
				else {
					if (dataMin >= 0.0) {
						mChartInfo.axisMin = 0.0f;
						mChartInfo.axisMax = dataMax * cBarSpacingFactor;
						}
					else if (dataMax <= 0.0) {
						mChartInfo.axisMin = dataMin * cBarSpacingFactor;
						mChartInfo.axisMax = 0.0f;
						}
					else {
						float dataRange = dataMax - dataMin;
						float spacing = (cBarSpacingFactor - 1.0f) * dataRange / 2.0f;
						mChartInfo.axisMax = dataMax + spacing;
						mChartInfo.axisMin = dataMin - spacing;
						}
					mChartInfo.barBase = 0.0f;
					}
				break;
				}
			}
		else {
			mChartInfo.axisMin = 0.0f;
			mChartInfo.axisMax = cBarSpacingFactor;
			mChartInfo.barBase = 0.0f;
			}
//System.out.println("calculateBarsOrPies() dataMin:"+mChartInfo.dataMin+" dataMax:"+mChartInfo.dataMax+" axisMin:"+mChartInfo.axisMin+" axisMax:"+mChartInfo.axisMax+" barBase:"+mChartInfo.barBase);
		}

	/**
	 * Based on axis column assignments and on hvIndices of VisualizationPoints
	 * this method assigns all visible VisualizationPoints to boxes and to color categories
	 * within these boxes. It also calculates statistical parameters of all boxes.
	 * @param hvCount
	 */
	protected void calculateBoxPlot() {
		int doubleAxis = determineBoxPlotDoubleAxis();
		calculateCategoryCounts(doubleAxis);

		Color[] colorList = mMarkerColor.getColorList();
		int focusFlagNo = getFocusFlag();
		int basicColorCount = colorList.length + 1;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);

		int catCount = mCaseSeparationCategoryCount;
		for (int axis=0; axis<mDimensions; axis++)
			if (axis != doubleAxis)
				catCount *= mCategoryVisibleCount[axis]; 

		BoxPlotViewInfo boxPlotInfo = new BoxPlotViewInfo(mHVCount, catCount, colorCount);
		mChartInfo = boxPlotInfo;
		boxPlotInfo.barAxis = doubleAxis;

		// create array with all visible values separated by hv and cat
		int[][] vCount = new int[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = getChartCategoryIndex(mPoint[i]);
				int hv = mPoint[i].hvIndex;
				vCount[hv][cat]++;
				}
			}
		double[][][] value = new double[mHVCount][catCount][];
		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (vCount[hv][cat] != 0)
					value[hv][cat] = new double[vCount[hv][cat]];

		// fill in values
		vCount = new int[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				float d = getValue(mPoint[i].record, boxPlotInfo.barAxis);
				boxPlotInfo.barValue[hv][cat] += d;
				value[hv][cat][vCount[hv][cat]] = d;
				vCount[hv][cat]++;
				}
			}

		boxPlotInfo.boxQ1 = new float[mHVCount][catCount];
		boxPlotInfo.median = new float[mHVCount][catCount];
		boxPlotInfo.boxQ3 = new float[mHVCount][catCount];
		boxPlotInfo.boxLAV = new float[mHVCount][catCount];
		boxPlotInfo.boxUAV = new float[mHVCount][catCount];

		// calculate statistical parameters from sorted values
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (vCount[hv][cat] != 0) {
					Arrays.sort(value[hv][cat]);
					boxPlotInfo.boxQ1[hv][cat] = getPercentile(value[hv][cat], 0.25f);
					boxPlotInfo.median[hv][cat] = getPercentile(value[hv][cat], 0.50f);
					boxPlotInfo.boxQ3[hv][cat] = getPercentile(value[hv][cat], 0.75f);
					boxPlotInfo.barValue[hv][cat] /= vCount[hv][cat];
	
					// set lower and upper adjacent values
					float iqr = boxPlotInfo.boxQ3[hv][cat] - boxPlotInfo.boxQ1[hv][cat];
					float lowerLimit = boxPlotInfo.boxQ1[hv][cat] - 1.5f * iqr;
					float upperLimit = boxPlotInfo.boxQ3[hv][cat] + 1.5f * iqr;
					int i = 0;
					while (value[hv][cat][i] < lowerLimit)
						i++;
					boxPlotInfo.boxLAV[hv][cat] = (float)value[hv][cat][i];
					i = value[hv][cat].length - 1;
					while (value[hv][cat][i] > upperLimit)
						i--;
					boxPlotInfo.boxUAV[hv][cat] = (float)value[hv][cat][i];

					if (mChartType == cChartTypeWhiskerPlot)
						boxPlotInfo.pointsInCategory[hv][cat] = vCount[hv][cat];
					}
				}
			}

		int pValueColumn = getPValueColumn();
		if (pValueColumn != cColumnUnassigned) {
			int categoryIndex = getCategoryIndex(pValueColumn, mPValueRefCategory);
			if (categoryIndex != -1) {
				if (mBoxplotShowFoldChange)
					boxPlotInfo.foldChange = new float[mHVCount][catCount];
				if (mBoxplotShowPValue)
					boxPlotInfo.pValue = new float[mHVCount][catCount];
				int[] individualIndex = new int[1+mDimensions];
				for (int hv=0; hv<mHVCount; hv++) {
					for (int cat=0; cat<catCount; cat++) {
						if (vCount[hv][cat] != 0) {
							int refHV = getReferenceHV(hv, pValueColumn, categoryIndex);
							int refCat = getReferenceCat(cat, pValueColumn, categoryIndex, individualIndex);
							if ((refHV != hv || refCat != cat) && vCount[refHV][refCat] != 0) {
								if (mBoxplotShowFoldChange) {
									if (mTableModel.isLogarithmicViewMode(mAxisIndex[boxPlotInfo.barAxis]))
										boxPlotInfo.foldChange[hv][cat] = 3.321928094887363f * (boxPlotInfo.barValue[hv][cat] - boxPlotInfo.barValue[refHV][refCat]);	// this is the log2(fc)
									else
										boxPlotInfo.foldChange[hv][cat] = boxPlotInfo.barValue[hv][cat] / boxPlotInfo.barValue[refHV][refCat];
									}
								if (mBoxplotShowPValue) {
									try {
										boxPlotInfo.pValue[hv][cat] = (float) new TTestImpl().tTest(value[hv][cat], value[refHV][refCat]);
										}
									catch (IllegalArgumentException e) {
										boxPlotInfo.pValue[hv][cat] = Float.NaN;
										}
									catch (MathException e) {
										boxPlotInfo.pValue[hv][cat] = Float.NaN;
										}
									}
								}
							else {
								if (mBoxplotShowFoldChange)
									boxPlotInfo.foldChange[hv][cat] = Float.NaN;
								if (mBoxplotShowPValue)
									boxPlotInfo.pValue[hv][cat] = Float.NaN;
								}
							}
						}
					}
				}
			}

		// for this chart type we need the statistical parameters, but no colored bar
		if (mChartType == cChartTypeWhiskerPlot)
			return;

		// create color list
		for (int i=0; i<colorList.length; i++)
			boxPlotInfo.color[i] = colorList[i];
		boxPlotInfo.color[colorList.length] = VisualizationColor.cSelectedColor;
		if (focusFlagNo != -1) {
			for (int i=0; i<colorList.length; i++)
				boxPlotInfo.color[i+basicColorCount] = VisualizationColor.grayOutColor(colorList[i]);
			boxPlotInfo.color[colorList.length+basicColorCount] = VisualizationColor.grayOutColor(VisualizationColor.cSelectedColor);
			}

		boxPlotInfo.outlierCount = new int[mHVCount][catCount];

		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = getChartCategoryIndex(mPoint[i]);
				int hv = mPoint[i].hvIndex;
				if (boxPlotInfo.isOutsideValue(hv, cat, getValue(mPoint[i].record, boxPlotInfo.barAxis))) {
					boxPlotInfo.outlierCount[hv][cat]++;
					}
				else {
					int colorIndex = ((mPoint[i].record. getFlags() & CompoundRecord.cFlagMaskSelected) != 0
								   && mFocusHitlist != cFocusOnSelection) ?
										   colorList.length : mPoint[i].colorIndex;
					if (focusFlagNo != -1 && !mPoint[i].record.isFlagSet(focusFlagNo))
						colorIndex += basicColorCount;
	
					boxPlotInfo.pointsInCategory[hv][cat]++;
					boxPlotInfo.pointsInColorCategory[hv][cat][colorIndex]++;
					}
				}
			}

		int[][][] count = new int[mHVCount][catCount][colorCount];
		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				for (int color=1; color<colorCount; color++)
					count[hv][cat][color] = count[hv][cat][color-1]+boxPlotInfo.pointsInColorCategory[hv][cat][color-1];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisible(mPoint[i])) {
				float v = getValue(mPoint[i].record, boxPlotInfo.barAxis);
				if (Float.isNaN(v)) {
					mPoint[i].chartGroupIndex = -1;
					}
				else {
					int hv = mPoint[i].hvIndex;
					int cat = getChartCategoryIndex(mPoint[i]);
					if (boxPlotInfo.isOutsideValue(hv, cat, v)) {
						mPoint[i].chartGroupIndex = -1;
						}
					else {
						CompoundRecord record = mPoint[i].record;
						int colorIndex = (record.isSelected()
									   && mFocusHitlist != cFocusOnSelection) ?
											   colorList.length : mPoint[i].colorIndex;
						if (focusFlagNo != -1 && !record.isFlagSet(focusFlagNo))
							colorIndex += basicColorCount;
		
						mPoint[i].chartGroupIndex = count[hv][cat][colorIndex];
						count[hv][cat][colorIndex]++;
						}
					}
				}
			}
		}

	public String getStatisticalValues() {
		if (mChartType == cChartTypeScatterPlot)
			return "Incompatible chart type.";

		String[][] categoryList = new String[6][];
		int[] categoryColumn = new int[6];
		int categoryColumnCount = 0;
		
		for (int i=0; i<2; i++) {
			if (mSplittingColumn[i] != cColumnUnassigned) {
				categoryColumn[categoryColumnCount] = mSplittingColumn[i];
				categoryList[categoryColumnCount] = mTableModel.getCategoryList(mSplittingColumn[i]);
				categoryColumnCount++;
				}
			}
		for (int axis=0; axis<mDimensions; axis++) {
			if (mIsCategoryAxis[axis]) {
				categoryColumn[categoryColumnCount] = mAxisIndex[axis];
				categoryList[categoryColumnCount] = new String[mCategoryVisibleCount[axis]];
				if (mAxisIndex[axis] == cColumnUnassigned) { // box/whisker plot with unassigned category axis
					categoryList[categoryColumnCount][0] = "<All Rows>";
					}
				else {
					String[] list = mTableModel.getCategoryList(categoryColumn[categoryColumnCount]);
					for (int j=0; j<mCategoryVisibleCount[axis]; j++)
						categoryList[categoryColumnCount][j] = list[mCategoryMin[axis]+j];
					}
				categoryColumnCount++;
				}
			}
		if (isCaseSeparationDone()) {
			categoryColumn[categoryColumnCount] = mCaseSeparationColumn;
			categoryList[categoryColumnCount] = mTableModel.getCategoryList(mCaseSeparationColumn);
			categoryColumnCount++;
			}

		boolean includePValue = false;
		boolean includeFoldChange = false;
		int pValueColumn = getPValueColumn();
		int referenceCategoryIndex = -1;
		if (pValueColumn != cColumnUnassigned) {
			referenceCategoryIndex = getCategoryIndex(pValueColumn, mPValueRefCategory);
			if (referenceCategoryIndex != -1) {
				includePValue = mBoxplotShowPValue;
				includeFoldChange = mBoxplotShowFoldChange;
				}
			}

		StringWriter stringWriter = new StringWriter(1024);
		BufferedWriter writer = new BufferedWriter(stringWriter);

		try {
			// construct the title line
			for (int i=0; i<categoryColumnCount; i++) {
				String columnTitle = (categoryColumn[i] == -1) ? "Category" : mTableModel.getColumnTitle(categoryColumn[i]);
				writer.append(columnTitle+"\t");
				}

			if (mChartType != cChartTypeBoxPlot)
				writer.append("Rows in Category");

			if ((mChartType == cChartTypeBars
			  || mChartType == cChartTypePies)
			 && mChartMode != cChartModeCount) {
				String name = mTableModel.getColumnTitle(mChartColumn);
				writer.append((mChartMode == cChartModePercent) ? "\tPercent of Rows"
							: (mChartMode == cChartModeMean) ? "\tMean of "+name
							: (mChartMode == cChartModeMean) ? "\tSum of "+name
							: (mChartMode == cChartModeMin) ? "\tMinimum of "+name
							: (mChartMode == cChartModeMax) ? "\tMaximum of "+name : "");
				}

			if (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) {
				if (mChartType == cChartTypeBoxPlot) {
					writer.append("Total Count");
					writer.append("\tOutlier Count");
					}
				writer.append("\tMean Value");
				writer.append("\t1st Quartile");
				writer.append("\tMedian");
				writer.append("\t3rd Quartile");
				writer.append("\tLower Adjacent Limit");
				writer.append("\tUpper Adjacent Limit");
/*				if (includeFoldChange || includePValue)		don't use additional column
					writer.append("\tIs Reference Group");	*/
				if (includeFoldChange)
					writer.append(mTableModel.isLogarithmicViewMode(mAxisIndex[((BoxPlotViewInfo)mChartInfo).barAxis]) ? "\tlog2(Fold Change)" : "\tFold Change");
				if (includePValue)
					writer.append("\tp-Value");
				}
			writer.newLine();
		
			int[] categoryIndex = new int[6];
			while (categoryIndex[0] < categoryList[0].length) {
				int columnIndex = 0;
				int hv = 0;
				if (mSplittingColumn[0] != cColumnUnassigned)
					hv += categoryIndex[columnIndex++];
				if (mSplittingColumn[1] != cColumnUnassigned)
					hv += categoryIndex[columnIndex++] * categoryList[0].length;

				int cat = 0;
				for (int axis=0; axis<mDimensions; axis++)
					if (mIsCategoryAxis[axis])
						cat += categoryIndex[columnIndex++] * mCombinedCategoryCount[axis];

				if (isCaseSeparationDone())
					cat += categoryIndex[columnIndex++];

				if (mChartInfo.pointsInCategory[hv][cat] != 0) {
					for (int i=0; i<categoryColumnCount; i++) {
						writer.append(categoryList[i][categoryIndex[i]]);
						if ((includeFoldChange || includePValue)
						 && pValueColumn==categoryColumn[i]
						 && mPValueRefCategory.equals(categoryList[i][categoryIndex[i]]))
							writer.append(" (ref)");
						writer.append("\t");
						}
			
					if (mChartType != cChartTypeBoxPlot)
						writer.append(""+mChartInfo.pointsInCategory[hv][cat]);

					if ((mChartType == cChartTypeBars
					  || mChartType == cChartTypePies)
					 && mChartMode != cChartModeCount)
						writer.append("\t"+formatValue(mChartInfo.barValue[hv][cat], mChartColumn));

					if (mChartType == cChartTypeBoxPlot
					 || mChartType == cChartTypeWhiskerPlot) {
						BoxPlotViewInfo vi = (BoxPlotViewInfo)mChartInfo;
						if (mChartType == cChartTypeBoxPlot) {
							writer.append(""+(mChartInfo.pointsInCategory[hv][cat]+vi.outlierCount[hv][cat]));
							writer.append("\t"+vi.outlierCount[hv][cat]);
							}
						int column = mAxisIndex[((BoxPlotViewInfo)mChartInfo).barAxis];
						writer.append("\t"+formatValue(vi.barValue[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxQ1[hv][cat], column));
						writer.append("\t"+formatValue(vi.median[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxQ3[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxLAV[hv][cat], column));
						writer.append("\t"+formatValue(vi.boxUAV[hv][cat], column));
/*						if (includeFoldChange || includePValue) {		don't use additional column
							int refHV = getReferenceHV(hv, pValueColumn, referenceCategoryIndex);
							int refCat = getReferenceCat(cat, pValueColumn, referenceCategoryIndex, new int[1+mDimensions]);
							writer.append("\t"+((hv==refHV && cat==refCat) ? "yes" : "no"));
							}	*/
						if (includeFoldChange) {
							writer.append("\t");
							if (!Float.isNaN(vi.foldChange[hv][cat]))
								writer.append(new DecimalFormat("#.#####").format(vi.foldChange[hv][cat]));
							}
						if (includePValue) {
							writer.append("\t");
							if (!Float.isNaN(vi.pValue[hv][cat]))
								writer.append(new DecimalFormat("#.#####").format(vi.pValue[hv][cat]));
							}
						}
					writer.newLine();
					}
	
				// update category indices for next row
				for (int i=categoryColumnCount-1; i>=0; i--) {
					if (++categoryIndex[i] < categoryList[i].length || i == 0)
						break;

					categoryIndex[i] = 0;
					}
				}
			writer.close();
			}
		catch (IOException ioe) {}

		return stringWriter.toString();
		}

	/**
	 * Formats a numerical value for displaying it.
	 * This includes proper rounding and potential de-logarithmization of the original value.
	 * @param value is the logarithm, if the column is in logarithmic view mode
	 * @param column the column this value refers to
	 * @return
	 */
	protected String formatValue(float value, int column) {
		if (mTableModel.isLogarithmicViewMode(column) && !Float.isNaN(value) && !Float.isInfinite(value))
			value = (float)Math.pow(10, value);
		return DoubleFormat.toString(value);
		}

	/**
	 * Returns the correct value to apply, when positioning a VisualizationPoint
	 * on an axis. This method resolves whether we have a dynamic value (e.g. from
	 * a descriptor similarity calculation) or a static value from the CompoundRecord.
	 * With ambiguous column types (category and double) it also considers, whether
	 * to use the category index or the double value.
	 * @param vp
	 * @param axis
	 * @return
	 */
	protected float getValue(CompoundRecord record, int axis) {
		int column = mAxisIndex[axis];
		return mTableModel.isDescriptorColumn(column) ?
				(mAxisSimilarity[axis] == null ? 0.5f : mAxisSimilarity[axis][record.getID()])
			  : (mIsCategoryAxis[axis]) ? mTableModel.getCategoryIndex(column, record) : record.getDouble(column);
		}

	protected TreeMap<byte[],VisualizationPoint> createReferenceMap(int referencingColumn, int referencedColumn) {
		// create list of referencing keys
		TreeSet<byte[]> set = new TreeSet<byte[]>(new ByteArrayComparator());
		for (VisualizationPoint vp:mPoint) {
			byte[] data = (byte[])vp.record.getData(referencingColumn);
			if (data != null)
				for (String ref:mTableModel.separateEntries(new String(data)))
					set.add(ref.getBytes());
			}

		// create map of existing and referenced VisualizationPoints
		TreeMap<byte[],VisualizationPoint> map = new TreeMap<byte[],VisualizationPoint>(new ByteArrayComparator());
		for (VisualizationPoint vp:mPoint) {
			byte[] key = (byte[])vp.record.getData(referencedColumn);
			if (set.contains(key))
				map.put(key, vp);
			}

		return map;
		}

	public boolean getShowNaNValues() {
		return mShowNaNValues;
		}

	public void setShowNaNValues(boolean b) {
		if (mShowNaNValues != b) {
			mShowNaNValues = b;
			applyLocalExclusion(false);
			invalidateOffImage(true);
			}
		}

	public boolean isGridSuppressed() {
		return mSuppressGrid;
		}

	public boolean isScaleSuppressed() {
		return mSuppressScale;
		}

	public void setSuppressScale(boolean hideScale, boolean hideGrid) {
		if (mSuppressScale != hideScale
		 || mSuppressGrid != hideGrid) {
			boolean scaleChanged = (mSuppressScale != hideScale);
			mSuppressScale = hideScale;
			mSuppressGrid = hideGrid;
			invalidateOffImage(scaleChanged);
			}
		}

	public int getConnectionColumn() {
		// filter out connection types being incompatible with chart type
		if (mChartType == cChartTypeBoxPlot
		 || mChartType == cChartTypeWhiskerPlot) {
			if (mConnectionColumn == cConnectionColumnConnectCases
			 || (mCaseSeparationCategoryCount != 1 && mConnectionColumn == mCaseSeparationColumn))
				return mConnectionColumn;
			for (int i=0; i<mDimensions; i++)
				if (mConnectionColumn == mAxisIndex[i])
					return mConnectionColumn;
			return cColumnUnassigned;
			}
		else {
			return mConnectionColumn != cConnectionColumnConnectCases ? mConnectionColumn : cColumnUnassigned;
			}
		}

	public int getConnectionOrderColumn() {
		return (mConnectionColumn == cColumnUnassigned
			 || mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) ?
					 cColumnUnassigned : mConnectionOrderColumn;
		}

	public void setConnectionColumns(int column, int orderColumn) {
		if (column != cColumnUnassigned
		 && column != cConnectionColumnConnectAll
		 && column != cConnectionColumnConnectCases
		 && !mTableModel.isColumnTypeCategory(column)
		 && mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn) == null)
			column = cColumnUnassigned;

		if (column == cColumnUnassigned
		 || column == cConnectionColumnConnectCases
		 ||	(column >= 0 && mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn) != null))
			orderColumn = cColumnUnassigned;

		if (mConnectionColumn != column || mConnectionOrderColumn != orderColumn) {
			invalidateConnectionLines();
			mConnectionColumn = column;
			mConnectionOrderColumn = orderColumn;
			invalidateOffImage(false);
			updateTreeViewGraph();
			}
		}

	public void setConnectionLineWidth(float width, boolean isAdjusting) {
		width = Math.min(4f, width);
		if (mRelativeConnectionLineWidth != width) {
			mRelativeConnectionLineWidth = width;
			invalidateOffImage(false);
			}
		}

	public float getConnectionLineWidth() {
		return mRelativeConnectionLineWidth;
		}

	private void invalidateConnectionLines() {
		mConnectionLinePoint = null;
		mConnectionLineMap = null;
		}

	public void setMarkerSize(float size, boolean isAdjusting) {
		if (mRelativeMarkerSize != size) {
			mRelativeMarkerSize = size;

			// if no connection lines are drawn then keep line width synchronized with marker size for potential use
			if (mConnectionColumn == cColumnUnassigned)
				mRelativeConnectionLineWidth = mRelativeMarkerSize;

			invalidateOffImage(true);
			}
		}

	public void setMarkerSizeColumn(int column) {
		if (mMarkerSizeColumn != column) {
			mMarkerSizeColumn = column;
			invalidateOffImage(true);
			}
		}

	public void setMarkerSizeInversion(boolean inversion) {
		if (mMarkerSizeInversion != inversion) {
			mMarkerSizeInversion = inversion;
			invalidateOffImage(false);
			}
		}

	public void setMarkerSizeProportional(boolean proportional) {
		if (mMarkerSizeProportional != proportional) {
			mMarkerSizeProportional = proportional;
			invalidateOffImage(false);
			}
		}

	public void setMarkerSizeZoomAdaption(boolean adapt) {
		if (Float.isNaN(mMarkerSizeZoomAdaption) == adapt) {
			if (adapt)
				mMarkerSizeZoomAdaption = calculateZoomState();
			else
				mMarkerSizeZoomAdaption = Float.NaN;
			invalidateOffImage(false);
			}
		}

	public void setMarkerLabelSize(float size, boolean isAdjusting) {
		if (mMarkerLabelSize != size) {
			mMarkerLabelSize = size;
			if (showAnyLabels()) {
				invalidateOffImage(false);
				}
			}
		}

	public void setMarkerShapeColumn(int column) {
		if (column >= 0
		 && (!mTableModel.isColumnTypeCategory(column)
		  || mTableModel.getCategoryCount(column) > getAvailableShapeCount()))
			column = cColumnUnassigned;

		if (mMarkerShapeColumn != column) {
			mMarkerShapeColumn = column;
			updateShapeIndices();
			}
		}
	
	private void updateShapeIndices() {
		if (mMarkerShapeColumn == cColumnUnassigned)
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = 0;
		else if (CompoundTableHitlistHandler.isHitlistColumn(mMarkerShapeColumn)) {
			int flagNo = mTableModel.getHitlistHandler().getHitlistFlagNo(CompoundTableHitlistHandler.getHitlistFromColumn(mMarkerShapeColumn));
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = (byte)(mPoint[i].record.isFlagSet(flagNo) ? 0 : 1);
			}
		else {
			for (int i=0; i<mDataPoints; i++)
				mPoint[i].shape = (byte)mTableModel.getCategoryIndex(mMarkerShapeColumn, mPoint[i].record);
			}

		invalidateOffImage(true);
		}

	public void setMarkerLabelsInTreeViewOnly(boolean inTreeViewOnly) {
		mLabelsInTreeViewOnly = inTreeViewOnly;
		invalidateOffImage(false);
		}

	public void setMarkerLabels(int[] columnAtPosition) {
		mLabelColumn = columnAtPosition;
		invalidateOffImage(false);
		}

	protected boolean showAnyLabels() {
		if (!mLabelsInTreeViewOnly || isTreeViewGraph())
			for (int i=0; i<mLabelColumn.length; i++)
				if (mLabelColumn[i] != cColumnUnassigned)
					return true;

		return false;
		}

	public int getMarkerLabelColumn(int position) {
		return mLabelColumn[position];
		}

	public int getColumnIndex(int axis) {
		return mAxisIndex[axis];
		}

	/**
	 * Assigns the axis to the specified column or cColumnUnassigned.
	 * The chart type is updated and the visible range set to the maximum.
	 * Local record hiding of this axis is initialized and applied to the global
	 * exclusion.
	 * @param axis
	 * @param index
	 */
	public void setColumnIndex(int axis, int index) {
		if (mAxisIndex[axis] != index) {
			mAxisIndex[axis] = index;
			initializeAxis(axis);
			int exclusionNeeeds = (EXCLUSION_FLAG_NAN_0 << axis) | determineChartType();
			validateExclusion(exclusionNeeeds);
			}
		}

	public abstract int[] getSupportedChartTypes();

	public int getChartType() {
		return mChartType;
		}

	public int getPreferredChartType() {
		return mPreferredChartType;
		}

	public int getPreferredChartMode() {
		return mChartMode;
		}

	public int getPreferredChartColumn() {
		return mChartColumn;
		}

	public void setPreferredChartType(int type, int mode, int column) {
		if (mode == -1)
			mode = cChartModeCount;
		if (mode != cChartModeCount && mode != cChartModePercent && column == cColumnUnassigned)
			mode = cChartModeCount;
		if (mPreferredChartType != type
		 || mChartColumn != column
		 || mChartMode != mode) {
			mChartColumn = column;
			mPreferredChartType = type;
			mChartMode = mode;
			int exclusionNeeeds = determineChartType();
			validateExclusion(exclusionNeeeds);
			updateTreeViewGraph();
			invalidateOffImage(true);
			}
		}

	private int getReferenceHV(int hv, int categoryColumn, int categoryIndex) {
		if (mSplittingColumn[0] == cColumnUnassigned)
			return 0;

		if (mSplittingColumn[1] == cColumnUnassigned)
			return (mSplittingColumn[0] == categoryColumn) ? categoryIndex : hv;

		int categoryCount = mTableModel.getCategoryCount(mSplittingColumn[0]);
		if (mSplittingColumn[0] == categoryColumn) {
			int index2 = hv / categoryCount;
			return categoryIndex + index2 * categoryCount;
			}
		else {
			int index1 = hv % categoryCount;
			return index1 + categoryIndex * categoryCount;
			}
		}

	private int getReferenceCat(int cat, int categoryColumn, int categoryIndex, int[] individualIndex) {
		for (int i=mDimensions; i>0; i--) {
			individualIndex[i] = cat / mCombinedCategoryCount[i-1];
			cat -= individualIndex[i] * mCombinedCategoryCount[i-1];
			}
		individualIndex[0] = cat;
		
		if (mCaseSeparationCategoryCount != 1 && categoryColumn == mCaseSeparationColumn) {
			individualIndex[0] = categoryIndex;
			}
		else {
			for (int axis=0; axis<mDimensions; axis++) {
				if (categoryColumn == mAxisIndex[axis]) {
					individualIndex[axis+1] = categoryIndex;
					break;
					}
				}
			}

		int index = individualIndex[0];
		for (int axis=0; axis<mDimensions; axis++)
			index += individualIndex[axis+1] * mCombinedCategoryCount[axis];

		return index;
		}

	/**
	 * Returns the category index on the axis, i.e. the visible(!) category list
	 * index of the visualization point. If the row belong to a category being
	 * zoomed out of the view, then -1 is returned.
	 * @param axis
	 * @param vp
	 * @return visible category index or -1
	 */
	protected int getCategoryIndex(int axis, VisualizationPoint vp) {
		if (mAxisIndex[axis] == cColumnUnassigned)
			return 0;

		int index = mTableModel.getCategoryIndex(mAxisIndex[axis], vp.record);

		return (index >= mCategoryMin[axis] && index < mCategoryMax[axis]) ?
			index - mCategoryMin[axis] : -1;
		}

	/**
	 * Returns the index of value in the column's visible(!!!) category list.
	 * If the column is shown on an axis and if this axis is zoomed in, then
	 * this category index differs from the one of the CompoundTableModel.
	 * @param column
	 * @param value
	 * @return category index or -1 if the category is scrolled out of view
	 */
	private int getCategoryIndex(int column, String value) {
		if (column == mCaseSeparationColumn
		 || column == mSplittingColumn[0]
		 || column == mSplittingColumn[1])
			return mTableModel.getCategoryIndex(column, value);

		int axis = -1;
		for (int i=0; i<mDimensions; i++) {
			if (mAxisIndex[i] == column) {
				axis = i;
				break;
				}
			}

		int index = mTableModel.getCategoryIndex(column, value);

		return (index >= mCategoryMin[axis] && index < mCategoryMax[axis]) ?
			index - mCategoryMin[axis] : -1;
		}

	/**
	 * This method requires a valid chart type, which may be achieved by calling determineChartBasics()
	 * @param axis
	 * @return whether the data of the column assigned to this axis is considered category data
	 */
	public boolean isCategoryAxis(int axis) {
		return mIsCategoryAxis[axis];
		}

	/**
	 * Calculates one combined category index for VisualizationPoint
	 * that includes individual categories from case separation and axis
	 * categories. If the vp is zoomed out of the view, then -1 is returned.
	 * @param vp
	 * @return
	 */
	protected int getChartCategoryIndex(VisualizationPoint vp) {
		int index = (mCombinedCategoryCount[0] == 1) ?
				0 : mTableModel.getCategoryIndex(mCaseSeparationColumn, vp.record);

		for (int axis=0; axis<mDimensions; axis++) {
			if ((mChartType != cChartTypeBoxPlot
   			  && mChartType != cChartTypeWhiskerPlot)
		   	 || axis != mChartInfo.barAxis) {
				int axisIndex = getCategoryIndex(axis, vp);
				if (axisIndex == -1)
					return -1;

				index += axisIndex * mCombinedCategoryCount[axis];
				}
			}

		return index;
		}

	protected float getPercentile(double[] value, double cutoff) {
		int index = (int)(cutoff * ((float)value.length - 0.999999));
		float percentile = (float)value[index];
		if (0.0001 + index < cutoff * (value.length - 1))
			percentile += cutoff * (value[index+1] - value[index]);
		return percentile;
		}

	public void mouseClicked(MouseEvent e) {
		if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			VisualizationPoint marker = findMarker(e.getX(), e.getY());
			if (mActivePoint != marker) {
				// don't allow root de-selection if we are in a dedicated tree view
				boolean isPureTreeView = isTreeViewGraph() && !mTreeViewShowAll;
				if (marker != null || !isPureTreeView)
					mTableModel.setActiveRow(marker==null? null : marker.record);
				}
			}
		}

	/**
	 * This is the default implementation of locating a marker from screen coordinates.
	 * It assumes a rectangular marker shape and relies on the getDistanceToMarker().
	 * For complex marker shapes overwrite this method or getDistanceToMarker().
	 * @param x
	 * @param y
	 * @return
	 */
	public VisualizationPoint findMarker(int x, int y) {
		// inverted order to prefer markers that are in the front
		VisualizationPoint p = null;
		int minDistance = Integer.MAX_VALUE;
		for (int i=mDataPoints-1; i>=0; i--) {
			if (isVisible(mPoint[i])) {
				int dvp = getDistanceToMarker(mPoint[i], x, y);
				if (dvp == 0)
					return mPoint[i];
				if (dvp < 4 && dvp < minDistance) {
					p = mPoint[i];
					dvp = minDistance;
					}
				}
			}

		return p;
		}

	/**
	 * This method assumes a rectangular marker shape and uses the
	 * VisualizationPount's width and height values.
	 * May be overwritten to support complex marker shapes.
	 * @param vp
	 * @param x
	 * @param y
	 * @return
	 */
	public int getDistanceToMarker(VisualizationPoint vp, int x, int y) {
		int dx = Math.abs(vp.screenX - x) - Math.round(vp.width / 2.0f);
		int dy = Math.abs(vp.screenY - y) - Math.round(vp.height / 2.0f);
		return Math.max(0, Math.max(dx, dy));
		}

	public void mousePressed(MouseEvent e) {
		mMouseX1 = mMouseX2 = e.getX();
		mMouseY1 = mMouseY2 = e.getY();
		mMouseIsDown = true;

		if (System.getProperty("touch") != null) {
			new Thread() {
				public void run() {
					try {
						Thread.sleep(1000);
						}
					catch (InterruptedException ie) {}

					if (Math.abs(mMouseX2 - mMouseX1) < 5
					 && Math.abs(mMouseY2 - mMouseY1) < 5
					 && mMouseIsDown) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								activateTouchFunction();
								}
							});
						}
					}
				}.start();
			}

		mRectangleSelecting = false;
		mLassoSelecting = false;
		if (!handlePopupTrigger(e)
		 && (e.getModifiers() & InputEvent.BUTTON3_MASK) == 0) {
			mAddingToSelection = e.isShiftDown();
			if (e.isAltDown())
				mRectangleSelecting = true;
			else {
				mLassoSelecting = true;
				mLassoRegion = new Polygon();
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				}
			}
		}

	public void mouseReleased(MouseEvent e) {
		mMouseIsDown = false;
		if (!handlePopupTrigger(e)) {
			if (mRectangleSelecting) {
				int mouseX1,mouseX2,mouseY1,mouseY2;

				if (mMouseX1 < mMouseX2) {
					mouseX1 = mMouseX1;
					mouseX2 = mMouseX2;
					}
				else {
					mouseX1 = mMouseX2;
					mouseX2 = mMouseX1;
					}

				if (mMouseY1 < mMouseY2) {
					mouseY1 = mMouseY1;
					mouseY2 = mMouseY2;
					}
				else {
					mouseY1 = mMouseY2;
					mouseY2 = mMouseY1;
					}

				for (int i=0; i<mDataPoints; i++) {
					if (mPoint[i].screenX >= mouseX1
					 && mPoint[i].screenX <= mouseX2
					 && mPoint[i].screenY >= mouseY1
					 && mPoint[i].screenY <= mouseY2
					 && isVisible(mPoint[i]))
						mPoint[i].record.setSelection(true);
					else if (!mAddingToSelection)
						mPoint[i].record.setSelection(false);
					}

				mRectangleSelecting = false;
				mSelectionModel.invalidate();
				}
			else if (mLassoSelecting) {
				for (int i=0; i<mDataPoints; i++) {
					if (mLassoRegion.contains(mPoint[i].screenX, mPoint[i].screenY)
					 && isVisible(mPoint[i]))
						mPoint[i].record.setSelection(true);
					else if (!mAddingToSelection)
						mPoint[i].record.setSelection(false);
					}

				mLassoSelecting = false;
				mSelectionModel.invalidate();
				}
			}
		if (mTouchFunctionActive) {
			mTouchFunctionActive = false;
			repaint();
			}
		}

	private void activateTouchFunction() {
		if (!showPopupMenu()) {
			mTouchFunctionActive = true;
			repaint();
			}
		}

	protected boolean isTouchFunctionActive() {
		return mTouchFunctionActive;
		}

	private boolean handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger())
			showPopupMenu();

		return false;
		}

	private boolean showPopupMenu() {
		if (mDetailPopupProvider != null && allowPopupMenu()) {
			CompoundRecord record = (mHighlightedPoint == null) ? null : mHighlightedPoint.record;
			JPopupMenu popup = mDetailPopupProvider.createPopupMenu(record, (VisualizationPanel)getParent(), -1);
			if (popup != null) {
				popup.show(this, mMouseX1, mMouseY1);
				return true;
				}
			}

		return false;
		}

	/**
	 * May be overridden to allow popup menus depending on current state,
	 * e.g. on mCurrentHighlighted being null
	 * @return true if popup menu shall be shown
	 */
	public boolean allowPopupMenu() {
		return true;
		}

	public void mouseEntered(MouseEvent e) {
		mMouseIsDown = false;
		}

	public void mouseExited(MouseEvent e) {
		mMouseIsDown = false;
		}

	public void mouseMoved(MouseEvent e) {
		VisualizationPoint marker = findMarker(e.getX(), e.getY());
		if (mHighlightedPoint != marker)
			mTableModel.setHighlightedRow((marker == null) ? null : marker.record);
		}

	public void mouseDragged(MouseEvent e) {
		mMouseX2 = e.getX();
		mMouseY2 = e.getY();

		if (mRectangleSelecting) {
			repaint();
			}
		else if (mLassoSelecting) {
			if ((Math.abs(mMouseX2 - mLassoRegion.xpoints[mLassoRegion.npoints-1]) > 3)
			 || (Math.abs(mMouseY2 - mLassoRegion.ypoints[mLassoRegion.npoints-1]) > 3)) {
				mLassoRegion.npoints--;
				mLassoRegion.addPoint(mMouseX2, mMouseY2);
				mLassoRegion.addPoint(mMouseX1, mMouseY1);
				}

			repaint();
			}
		}

	@Override
	public Point getToolTipLocation(MouseEvent e) {
		VisualizationPoint vp = findMarker(e.getX(), e.getY());
		return (vp != null) ? new Point(vp.screenX, vp.screenY) : null;
		}

	@Override
	public String getToolTipText(MouseEvent e) {
		VisualizationPoint vp = findMarker(e.getX(), e.getY());
		if (vp == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (int axis=0; axis<mDimensions; axis++)
			addTooltipRow(vp.record, mAxisIndex[axis], mAxisSimilarity[axis], sb);

		addMarkerTooltips(vp, sb);

		sb.append("</html>");
		return sb.toString();
		}

	protected void addMarkerTooltips(VisualizationPoint vp, StringBuilder sb) {
		addTooltipRow(vp.record, mMarkerColor.getColorColumn(), null, sb);
		addTooltipRow(vp.record, mMarkerSizeColumn, null, sb);
		addTooltipRow(vp.record, mMarkerShapeColumn, null, sb);
		}

	protected void addTooltipRow(CompoundRecord record, int column, float[] similarity, StringBuilder sb) {
		if (column != cColumnUnassigned) {
			sb.append((sb.length() == 0) ? "<html>" : "<br>");
			if (CompoundTableHitlistHandler.isHitlistColumn(column)) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(column);
				int flagNo = mTableModel.getHitlistHandler().getHitlistFlagNo(hitlistIndex);
				sb.append(record.isFlagSet(flagNo) ? "M" : "Not m");
				sb.append("ember of '" + mTableModel.getHitlistHandler().getHitlistName(hitlistIndex) + "'");
				}
			else {
				sb.append(getAxisTitle(column)+": ");
				if (mTableModel.isDescriptorColumn(column)) {
					if (similarity != null)
						sb.append(DoubleFormat.toString(similarity[record.getID()]));
					else
						sb.append(DoubleFormat.toString((mActivePoint == null) ? Double.NaN
								: mTableModel.getDescriptorSimilarity(mActivePoint.record, record, column)));
					}
			   	else {
			   		sb.append(mTableModel.encodeData(record, column));
			   		}
		   		}
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		boolean needsUpdate = false;
		int exclusionNeeds = 0;

		if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int column = e.getSpecifier();
			for (int axis=0; axis<mDimensions; axis++) {
				if (column == mAxisIndex[axis]) {
				   	if (mTableModel.isDescriptorColumn(column))
				   		setSimilarityValues(axis);	// TODO keep old values and calculate changes only

					exclusionNeeds = ((EXCLUSION_FLAG_NAN_0 | EXCLUSION_FLAG_ZOOM_0) << axis);
					needsUpdate = true;
					}
				}
			if (mMarkerSizeColumn == column)
				needsUpdate = true;
			if (mMarkerShapeColumn == column) {
				if (!mTableModel.isColumnTypeCategory(column)
				 || mTableModel.getCategoryCount(column) > getAvailableShapeCount())
				 	mMarkerShapeColumn = cColumnUnassigned;
	   			updateShapeIndices();
		   		needsUpdate = true;
				}
			if (mCaseSeparationColumn == column) {
				if (!mTableModel.isColumnTypeCategory(column)
				 || mTableModel.getCategoryCount(column) > cMaxCaseSeparationCategoryCount)
					mCaseSeparationColumn = cColumnUnassigned;
				needsUpdate = true;
				}
			if (mSplittingColumn[0] == column
			 || mSplittingColumn[1] == column) {
				if (!mTableModel.isColumnTypeCategory(column)) {
					if (mSplittingColumn[0] == column) {
						mSplittingColumn[0] = mSplittingColumn[1];
						mSplittingColumn[1] = cColumnUnassigned;
						}
					else {
						mSplittingColumn[1] = cColumnUnassigned;
						}
					}
				
				updateSplittingIndices();
				}
			for (int i=0; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] == column)
					needsUpdate = true;
				}
			if (mConnectionColumn == column || mConnectionOrderColumn == column) {
				invalidateConnectionLines();
				needsUpdate = true;
				}
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			initializeDataPoints();
			for (int axis=0; axis<mDimensions; axis++) {
				int column = mAxisIndex[axis];
				if (column != cColumnUnassigned) {
				   	if (mTableModel.isDescriptorColumn(column))
				   		setSimilarityValues(axis);	// TODO keep old values and calculate changes only 

				   	exclusionNeeds |= ((EXCLUSION_FLAG_NAN_0 | EXCLUSION_FLAG_ZOOM_0) << axis);
					}
				}

		   	if (mMarkerShapeColumn >= 0) {	// if not is unassigned or hitlist
				if (!mTableModel.isColumnTypeCategory(mMarkerShapeColumn))
					mMarkerShapeColumn = cColumnUnassigned;
				updateShapeIndices();
				}
			if (mCaseSeparationColumn >= 0) {
				if (!mTableModel.isColumnTypeCategory(mCaseSeparationColumn)
				 || mTableModel.getCategoryCount(mCaseSeparationColumn) > cMaxCaseSeparationCategoryCount)
					mCaseSeparationColumn = cColumnUnassigned;
				needsUpdate = true;
				}
			if (mSplittingColumn[0] >= 0
			 || mSplittingColumn[1] >= 0) {  // if not is unassigned or hitlist
				if (mSplittingColumn[0] >= 0 && !mTableModel.isColumnTypeCategory(mSplittingColumn[0])) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				if (mSplittingColumn[1] >= 0 && !mTableModel.isColumnTypeCategory(mSplittingColumn[1])) {
					mSplittingColumn[1] = cColumnUnassigned;
					}
				updateSplittingIndices();
				}
			invalidateConnectionLines();
			needsUpdate = true;
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			if (mMarkerSizeColumn >= 0) {
				mMarkerSizeColumn = columnMapping[mMarkerSizeColumn];
				if (mMarkerSizeColumn == cColumnUnassigned) {
					mSizeLegend = null;
					needsUpdate = true;
					}
				}
			if (mMarkerShapeColumn >= 0) {
				mMarkerShapeColumn = columnMapping[mMarkerShapeColumn];
				if (mMarkerShapeColumn == cColumnUnassigned) {
					mShapeLegend = null;
					updateShapeIndices();
					}
				}
			if (mCaseSeparationColumn >= 0) {
				mCaseSeparationColumn = columnMapping[mCaseSeparationColumn];
				if (mCaseSeparationColumn == cColumnUnassigned) {
					needsUpdate = true;
					}
				}
			if (mSplittingColumn[0] >= 0) {
				mSplittingColumn[0] = columnMapping[mSplittingColumn[0]];
				boolean updateSplitting = (mSplittingColumn[0] == cColumnUnassigned);
				if (mSplittingColumn[1] >= 0) {
					mSplittingColumn[1] = columnMapping[mSplittingColumn[1]];
					if (mSplittingColumn[1] == cColumnUnassigned)
						updateSplitting = true;
					}
				if (mSplittingColumn[0] == cColumnUnassigned && mSplittingColumn[1] != -1) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				if (updateSplitting) {
					updateSplittingIndices();
					needsUpdate = true;
					}
				}
			if (mConnectionColumn >= 0) {
				mConnectionColumn = columnMapping[mConnectionColumn];
				if (mConnectionColumn == cColumnUnassigned) {
					invalidateConnectionLines();
					needsUpdate = true;
					}
				}
			if (mConnectionOrderColumn != cColumnUnassigned) {
				mConnectionOrderColumn = columnMapping[mConnectionOrderColumn];
				if (mConnectionOrderColumn == cColumnUnassigned) {
					needsUpdate = true;
					}
				}
			for (int i=0; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] >= 0) {
					mLabelColumn[i] = columnMapping[mLabelColumn[i]];
					if (mLabelColumn[i] == cColumnUnassigned)
						needsUpdate = true;
					}
				}
			for (int axis=0; axis<mDimensions; axis++) {
				if (mAxisIndex[axis] != cColumnUnassigned) {
					mAxisIndex[axis] = columnMapping[mAxisIndex[axis]];
					if (mAxisIndex[axis] == cColumnUnassigned) {
						mAxisIndex[axis] = cColumnUnassigned;
						needsUpdate = true;
						initializeAxis(axis);
						exclusionNeeds = (EXCLUSION_FLAG_NAN_0 << axis);
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			updateActiveRow();
			needsUpdate = true;
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			invalidateOffImage(false);
			}

		if (mColorLegend != null)
			mColorLegend.compoundTableChanged(e);
		if (mSizeLegend != null)
			mSizeLegend.compoundTableChanged(e);
		if (mShapeLegend != null)
			mShapeLegend.compoundTableChanged(e);

		mMarkerColor.compoundTableChanged(e);

	   	validateExclusion(exclusionNeeds | determineChartType());

		if (needsUpdate)
			invalidateOffImage(true);
		}

	public void hitlistChanged(CompoundTableHitlistEvent e) {
		if (e.getType() == CompoundTableHitlistEvent.cDelete) {
			if (mFocusHitlist != cHitlistUnassigned) {
				if (mFocusHitlist == e.getHitlistIndex())
					setFocusHitlist(cHitlistUnassigned);
				else if (mFocusHitlist > e.getHitlistIndex())
					mFocusHitlist--;
				}
			if (CompoundTableHitlistHandler.isHitlistColumn(mMarkerSizeColumn)) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mMarkerSizeColumn);
				if (e.getHitlistIndex() == hitlistIndex) {
					mMarkerSizeColumn = cColumnUnassigned;
					invalidateOffImage(false);
					}
				else if (hitlistIndex > e.getHitlistIndex()) {
					mMarkerSizeColumn = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex-1);
					}
				}
			if (CompoundTableHitlistHandler.isHitlistColumn(mMarkerShapeColumn)) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mMarkerShapeColumn);
				if (e.getHitlistIndex() == hitlistIndex) {
					mMarkerShapeColumn = cColumnUnassigned;
					for (int i=0; i<mDataPoints; i++)
						mPoint[i].shape = 0;
					invalidateOffImage(true);
					}
				else if (hitlistIndex > e.getHitlistIndex()) {
					mMarkerShapeColumn = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex-1);
					}
				}
			if (CompoundTableHitlistHandler.isHitlistColumn(mCaseSeparationColumn)) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mCaseSeparationColumn);
				if (e.getHitlistIndex() == hitlistIndex) {
					mCaseSeparationColumn = cColumnUnassigned;
					invalidateOffImage(true);
					}
				else if (hitlistIndex > e.getHitlistIndex()) {
					mCaseSeparationColumn = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex-1);
					}
				}
			boolean splittingChanged = false;
			if (CompoundTableHitlistHandler.isHitlistColumn(mSplittingColumn[0])) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mSplittingColumn[0]);
				if (e.getHitlistIndex() == hitlistIndex) {
					mSplittingColumn[0] = cColumnUnassigned;
					splittingChanged = true;
					}
				else if (hitlistIndex > e.getHitlistIndex()) {
					mSplittingColumn[0] = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex-1);
					}
				}
			if (CompoundTableHitlistHandler.isHitlistColumn(mSplittingColumn[1])) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mSplittingColumn[1]);
				if (e.getHitlistIndex() == hitlistIndex) {
					mSplittingColumn[1] = cColumnUnassigned;
					splittingChanged = true;
					}
				else if (hitlistIndex > e.getHitlistIndex()) {
					mSplittingColumn[1] = CompoundTableHitlistHandler.getColumnFromHitlist(hitlistIndex-1);
					}
				}
			if (splittingChanged) {
				if (mSplittingColumn[0] == cColumnUnassigned
				 || mSplittingColumn[1] != cColumnUnassigned) {
					mSplittingColumn[0] = mSplittingColumn[1];
					mSplittingColumn[1] = cColumnUnassigned;
					}
				updateSplittingIndices();
				}
			}
		else if (e.getType() == CompoundTableHitlistEvent.cChange) {
			if (mFocusHitlist == e.getHitlistIndex()) {
				invalidateOffImage(false);
				}
			if (CompoundTableHitlistHandler.isHitlistColumn(mMarkerSizeColumn)) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mMarkerSizeColumn);
				if (e.getHitlistIndex() == hitlistIndex) {
					invalidateOffImage(false);
					}
				}
			if (CompoundTableHitlistHandler.isHitlistColumn(mMarkerShapeColumn)) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mMarkerShapeColumn);
				if (e.getHitlistIndex() == hitlistIndex) {
					int flagNo = mTableModel.getHitlistHandler().getHitlistFlagNo(hitlistIndex);
					for (int i=0; i<mDataPoints; i++)
						mPoint[i].shape = (byte)(mPoint[i].record.isFlagSet(flagNo) ? 0 : 1);
					invalidateOffImage(false);
					}
				}
			if (CompoundTableHitlistHandler.isHitlistColumn(mCaseSeparationColumn)) {
				int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mCaseSeparationColumn);
				if (e.getHitlistIndex() == hitlistIndex)
					invalidateOffImage(true);
				}
			if ((CompoundTableHitlistHandler.isHitlistColumn(mSplittingColumn[0])
			  && e.getHitlistIndex() == CompoundTableHitlistHandler.getHitlistFromColumn(mSplittingColumn[0]))
			 || (CompoundTableHitlistHandler.isHitlistColumn(mSplittingColumn[1])
			  && e.getHitlistIndex() == CompoundTableHitlistHandler.getHitlistFromColumn(mSplittingColumn[1]))) {
				updateSplittingIndices();
				}
			}

		mMarkerColor.hitlistChanged(e);
		}

	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			invalidateOffImage(mChartType == cChartTypeBoxPlot
							|| mChartType == cChartTypeBars
							|| mChartType == cChartTypePies);
			}
		}

	@Override
	public void highlightChanged(CompoundRecord record) {
		if (record != null
		 && (mHighlightedPoint == null || mHighlightedPoint.record != record)) {
			for (int i=0; i<mPoint.length; i++) {
				if (mPoint[i].record == record) {
					setHighlightedPoint(mPoint[i]);
					break;
					}
				}
			}
		else if (record == null && mHighlightedPoint != null) {
			setHighlightedPoint(null);
			}
		}

	public void updateVisibleRange(int axis, float low, float high, boolean isAdjusting) {
		if (axis < mDimensions && mAxisIndex[axis] != cColumnUnassigned) {
			mPruningBarLow[axis] = low;
			mPruningBarHigh[axis] = high;
			if (calculateVisibleRange(axis)) {
				updateLocalZoomExclusion(axis);
				applyLocalExclusion(isAdjusting);
				if (!Float.isNaN(mMarkerSizeZoomAdaption))
					mMarkerSizeZoomAdaption = calculateZoomState();
				invalidateOffImage(true);
				}
			}
		}

	/**
	 * This is used by find marker, assuming that a marker covers a
	 * rectangular area. If the marker's area is not rectangular,
	 * then findMarker() should be overridden for a smooth handling.
	 * @param p
	 * @return
	 */
	protected float getMarkerWidth(VisualizationPoint p) {
		return mAbsoluteMarkerSize;
		}

	/**
	 * This is used by find marker, assuming that a marker covers a
	 * rectangular area. If the marker's area is not rectangular,
	 * then findMarker() should be overridden for a smooth handling.
	 * @param p
	 * @return
	 */
	protected float getMarkerHeight(VisualizationPoint p) {
		return mAbsoluteMarkerSize;
		}

	/**
	 * Resets the visible range of the axis to the tablemodel's
	 * min and max values and repaints.
	 * @param axis
	 */
	public void initializeAxis(int axis) {
		mPruningBarLow[axis] = 0.0f;
		mPruningBarHigh[axis] = 1.0f;

		mAxisSimilarity[axis] = null;

		int column = mAxisIndex[axis];

		if (column != cColumnUnassigned
		 && mTableModel.isDescriptorColumn(column))
			setSimilarityValues(axis);

		invalidateOffImage(true);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered visible, if the showNaNValue option is on.
	 * @param point
	 * @return
	 */
	protected boolean isVisible(VisualizationPoint point) {
		return (point.exclusionFlags & (mShowNaNValues ? ~EXCLUSION_FLAGS_NAN : mActiveExclusionFlags)) == 0
			&& mTableModel.isVisible(point.record);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered invisible, even if the showNaNValue option is on.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleExcludeNaN(VisualizationPoint point) {
		return (point.exclusionFlags & mActiveExclusionFlags) == 0
			&& mTableModel.isVisible(point.record);
		}

	/**
	 * Checks, whether this visualization point is visible in this view,
	 * i.e. whether it is not excluded by filters, foreign views or local view
	 * settings. Visualization points with a NaN value in one of the axis columns
	 * are considered visible, even if the showNaNValue option is off.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleIncludeNaN(VisualizationPoint point) {
		return (point.exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0
			&& mTableModel.isVisible(point.record);
		}

	/**
	 * Checks, whether this visualization point is not zoomed out of this view
	 * and not invisible because of another view, but contains a NaN value
	 * on at least one axis that shows floating point values.
	 * @param point
	 * @return
	 */
	protected boolean isVisibleAndNaN(VisualizationPoint point) {
		return (point.exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0
			&& (point.exclusionFlags & EXCLUSION_FLAGS_NAN & mActiveExclusionFlags) != 0
			&& mTableModel.isVisible(point.record);
		}

	/**
	 * Checks, whether there is at least one axis showing floating point values
	 * (not as categories) where this point has a NaN value in the associated column.
	 * @param point
	 * @return
	 */
	protected boolean isNaN(VisualizationPoint point) {
		return (point.exclusionFlags & EXCLUSION_FLAGS_NAN & mActiveExclusionFlags) != 0;
		}

	/**
	 * Sets axis related zoom flags to false and sets NaN flags depending on
	 * whether the value is NaN regardless whether the axis is showing floats or categories.
	 * @param axis
	 */
	private void initializeLocalExclusion(int axis) {
			// flags 0-2: set if invisible due to view zooming
			// flags 3-5: set if invisible because of empty data (applies only for non-category )
			// flags 6  : set if invisible because point is not part of currently shown detail graph
		int column = mAxisIndex[axis];
		byte nanFlag = (byte)(EXCLUSION_FLAG_NAN_0 << axis);
		byte bothFlags = (byte)((EXCLUSION_FLAG_ZOOM_0 | EXCLUSION_FLAG_NAN_0) << axis);

			// reset all flags and then
			// flag all records with empty data
		for (int i=0; i<mDataPoints; i++) {
			mPoint[i].exclusionFlags &= ~bothFlags;
			if (column != -1
			 && !mTableModel.isDescriptorColumn(column)
			 && Float.isNaN(mPoint[i].record.getDouble(column)))
				mPoint[i].exclusionFlags |= nanFlag;
			}
		}

	/**
	 * Updates the local exclusion flags of non-NAN row values to
	 * reflect whether the value lies between the visible range of the axis.
	 * Needs to be called after determineChartType().
	 * @param axis
	 */
	private void updateLocalZoomExclusion(int axis) {
		byte zoomFlag = (byte)(EXCLUSION_FLAG_ZOOM_0 << axis);
		byte nanFlag = (byte)(EXCLUSION_FLAG_NAN_0 << axis);
		for (int i=0; i<mDataPoints; i++) {
			if (mIsCategoryAxis[axis] || (mPoint[i].exclusionFlags & nanFlag) == 0) {
				float theDouble = getValue(mPoint[i].record, axis);
				if (theDouble < mAxisVisMin[axis]
				 || theDouble > mAxisVisMax[axis])
					mPoint[i].exclusionFlags |= zoomFlag;
				else
					mPoint[i].exclusionFlags &= ~zoomFlag;
				}
			else {
				mPoint[i].exclusionFlags &= ~zoomFlag;
				}
			}
		}

	private void updateGlobalExclusion() {
		if (mLocalAffectsGlobalExclusion && !mSuspendGlobalExclusion) {
			if (mLocalExclusionFlagNo == -1)
				mLocalExclusionFlagNo = mTableModel.getUnusedCompoundFlag(true);
			}
		else {
			mLocalExclusionFlagNo = -1;
			}
		applyLocalExclusion(false);
		}

	/**
	 * Returns whether rows zoomed out of view or invisible rows because of NaN values
	 * are also excluded from other views. The default is true.
	 * @return whether this view's local exclusion also affects the global exclusion
	 */
	public boolean getAffectGlobalExclusion() {
		return mLocalAffectsGlobalExclusion;
		}

	/**
	 * Defines whether local exclusion is affecting global exclusion,
	 * i.e. whether rows zoomed out of view or invisible rows because of NaN values
	 * are also excluded from other views. The default is true.
	 * @param v
	 */
	public void setAffectGlobalExclusion(boolean v) {
		if (mLocalAffectsGlobalExclusion != v) {
	   		mLocalAffectsGlobalExclusion = v;
			updateGlobalExclusion();
			}
		}

	/**
	 * Used to temporarily suspend the global record exclusion from the local one.
	 * This is called when the view gets hidden or shown again.
	 * @param suspend
	 */
	public void setSuspendGlobalExclusion(boolean suspend) {
		if (mSuspendGlobalExclusion != suspend) {
			mSuspendGlobalExclusion = suspend;
			updateGlobalExclusion();
			}
		}

	/**
	 * Set table model row exclusion flags according to local zooming/NAN-values
	 * and trigger a TableModelEvent in case the global record visibility changes.
	 * @param isAdjusting
	 */
	private void applyLocalExclusion(final boolean isAdjusting) {
			// set "current view exclusion flags" in CompoundTableModel
		if (!mApplyLocalExclusionScheduled) {
			mApplyLocalExclusionScheduled = true;

			// in case applyLocalExclusion() is called in the cascade caused by a compoundTableChanged()
			// (e.g. with delete columns), then we must wait until all views have updated accordingly,
			// before interfering by spawning another compoundTableChanged() cascade...
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					mApplyLocalExclusionScheduled = false;

					if (mLocalExclusionFlagNo != -1) {
						boolean excludedRecordsFound = false;
						long mask = mTableModel.convertCompoundFlagToMask(mLocalExclusionFlagNo);
						for (int i=0; i<mDataPoints; i++) {
							if ((mPoint[i].exclusionFlags & mActiveExclusionFlags) == 0
							 || (mShowNaNValues
							  && (mPoint[i].exclusionFlags & ~EXCLUSION_FLAGS_NAN) == 0)) {
								mPoint[i].record.clearFlags(mask);
								}
							else {
								mPoint[i].record.setFlags(mask);
								excludedRecordsFound = true;
								}
							}
	
						mTableModel.updateLocalExclusion(mLocalExclusionFlagNo, isAdjusting, excludedRecordsFound);
						}
					else if (mPreviousLocalExclusionFlagNo != -1) {
						mTableModel.freeCompoundFlag(mPreviousLocalExclusionFlagNo);
						}

					mPreviousLocalExclusionFlagNo = mLocalExclusionFlagNo;
					}
				});
			}
		}

	protected String createDateLabel(int theMarker, int exponent) {
		long time = theMarker;
		while (exponent < 0) {
			if (time % 10 != 0)
				return null;
			time /= 10;
			exponent++;
			}
		while (exponent > 0) {
			time *= 10;
			exponent--;
			}
		return DateFormat.getDateInstance().format(new Date(86400000*time+43200000));
		}

	protected void updateActiveRow() {
		CompoundRecord newActiveRow = mTableModel.getActiveRow();
		if (newActiveRow != null
		 && (mActivePoint == null || mActivePoint.record != newActiveRow)) {
			for (int i=0; i<mPoint.length; i++) {
				if (mPoint[i].record == newActiveRow) {
					setActivePoint(mPoint[i]);
					break;
					}
				}
			}
		else if (newActiveRow == null && mActivePoint != null) {
			setActivePoint(null);
			}
		}

	public boolean supportsMarkerLabelTable() {
		return false;
		}

	public boolean supportsMidPositionLabels() {
		return true;
		}

	public int getMarkerLabelTableEntryCount() {
		return 0;
		}

	public class FloatDimension {
		float width,height;
		}
	}
