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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.awt.print.PageFormat;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.imageio.ImageIO;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.calc.INumericalDataColumn;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.DepictorTransformation;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.table.CompoundListSelectionModel;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.view.graph.RadialGraphOptimizer;
import com.actelion.research.table.view.graph.TreeGraphOptimizer;
import com.actelion.research.table.view.graph.VisualizationNode;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.DoubleFormat;

public class JVisualization2D extends JVisualization {
	private static final long serialVersionUID = 0x00000001;

	public static final int cAvailableShapeCount = 7;
	public static final int cVisibleRecords = -1;

	private static final float cMarkerSize = 0.028f;
	private static final float cConnectionLineWidth = 0.005f;
	private static final float cMaxPieSize = 1.0f;

	private static final int cPrintScaling = 16;

	private static final float MARKER_OUTLINE = 0.7f;
	private static final float NAN_WIDTH = 2.0f;
	private static final float NAN_SPACING = 0.5f;

	public static final String[] CURVE_MODE_TEXT = { "<none>", "Vertical Line", "Horizontal Line", "Fitted Line" };
	public static final String[] CURVE_MODE_CODE = { "none", "abscissa", "ordinate", "fitted" };
	private static final int cCurveModeNone = 0;
	private static final int cCurveModeMask = 7;
	private static final int cCurveModeAbscissa = 1;
	private static final int cCurveModeOrdinate = 2;
	private static final int cCurveModeBothAxes = 3;
	private static final int cCurveStandardDeviation = 8;
	private static final int cCurveSplitByCategory = 16;

	private static final int[] SUPPORTED_CHART_TYPE = { cChartTypeScatterPlot, cChartTypeWhiskerPlot, cChartTypeBoxPlot, cChartTypeBars, cChartTypePies };

	private static final int cScaleTextNormal = 1;
	private static final int cScaleTextAlternating = 2;
	private static final int cScaleTextInclined = 3;

	public static final int cMultiValueMarkerModeNone = 0;
	private static final int cMultiValueMarkerModePies = 1;
	private static final int cMultiValueMarkerModeBars = 2;
	public static final String[] MULTI_VALUE_MARKER_MODE_TEXT = { "<none>", "Pie Pieces", "Bars" };
	public static final String[] MULTI_VALUE_MARKER_MODE_CODE = { "none", "pies", "bars" };

	private static int[]	mX,mY;

	private Graphics		mG;
	private float[]			mCorrelationCoefficient;
	private float			mFontScaling,mMarkerTransparency;
	private int				mBorder,mCurveInfo,mBackgroundHCount,mBackgroundVCount,
							mBackgroundColorRadius,mBackgroundColorFading,mBackgroundColorConsidered,
							mConnectionFromIndex1,mConnectionFromIndex2,mShownCorrelationType,mMultiValueMarkerMode;
	private boolean			mBackgroundValid,mIsHighResolution;
	private int[]			mScaleSize,mScaleTextMode,mScaleDepictorOffset,mSplittingMolIndex,mNaNSize,mMultiValueMarkerColumns;
	private JVisualizationLegend	mBackgroundLegend,mMultiValueLegend;
	protected VisualizationColor	mBackgroundColor;
	private Color[]			mMultiValueMarkerColor;
	private Color[][][]		mBackground;
	private Depictor2D[][]	mScaleDepictor,mSplittingDepictor;
	private VolatileImage	mOffImage;
	private BufferedImage   mBackgroundImage;
	private byte[]			mBackgroundImageData;
	private Graphics		mOffG;
	private ArrayList<ScaleLine>[]	mScaleLineList;

	@SuppressWarnings("unchecked")
	public JVisualization2D(CompoundTableModel tableModel,
							CompoundListSelectionModel selectionModel) {
		super(tableModel, selectionModel, 2);
		mX = new int[4];
		mY = new int[4];
		mPoint = new VisualizationPoint2D[0];
		mNaNSize = new int[2];
		mScaleSize = new int[2];
		mScaleTextMode = new int[2];
		mScaleDepictor = new Depictor2D[2][];
		mScaleDepictorOffset = new int[2];
		mScaleLineList = new ArrayList[2];
		mScaleLineList[0] = new ArrayList<ScaleLine>();
		mScaleLineList[1] = new ArrayList<ScaleLine>();
		mSplittingDepictor = new Depictor2D[2][];
		mSplittingMolIndex = new int[2];
		mSplittingMolIndex[0] = cColumnUnassigned;
		mSplittingMolIndex[1] = cColumnUnassigned;

		mBackgroundColor = new VisualizationColor(mTableModel, this);

		initialize();
		}

	protected void initialize() {
		super.initialize();
		mBackgroundColorConsidered = cVisibleRecords;
		mMarkerShapeColumn = cColumnUnassigned;
		mChartColumn = cColumnUnassigned;
		mChartMode = cChartModeCount;
		mBackgroundColorRadius = 10;
		mBackgroundColorFading = 10;
		mShapeLegend = null;
		mMultiValueLegend = null;
		mBackgroundLegend = null;
		mBackgroundImage = null;
		mBackgroundImageData = null;
		mMultiValueMarkerColumns = null;
		mMultiValueMarkerMode = cMultiValueMarkerModePies;
		mPreferredChartType = cChartTypeBars;
		mShownCorrelationType = CorrelationCalculator.TYPE_NONE;
		}

	@Override
 	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!mIsFastRendering)
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		mIsHighResolution = false;

		int width = getWidth();
		int height = getHeight();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

		if (mOffImage == null
		 || mOffImage.getWidth(null) != (int)(width*sRetinaFactor)
		 || mOffImage.getHeight(null) != (int)(height*sRetinaFactor)) {
			mOffImage = gc.createCompatibleVolatileImage((int)(width*sRetinaFactor), (int)(height*sRetinaFactor), Transparency.OPAQUE);
			mCoordinatesValid = false;
			}

		if (!mCoordinatesValid)
			mOffImageValid = false;

		if (!mOffImageValid) {
			do  {
				int valid = mOffImage.validate(gc);
				 
				if (valid == VolatileImage.IMAGE_INCOMPATIBLE)
					mOffImage = gc.createCompatibleVolatileImage(width, height, Transparency.OPAQUE);

				mOffG = null;
				try {
					mOffG = mOffImage.createGraphics();
					if (sRetinaFactor != 1f)
						((Graphics2D)mOffG).scale(sRetinaFactor, sRetinaFactor);
					if (!mIsFastRendering)
						((Graphics2D)mOffG).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		//			((Graphics2D)mOffG).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	no sub-pixel accuracy looks cleaner
		
					mOffG.setColor(getViewBackground());
					mOffG.fillRect(0, 0, width, height);
					Insets insets = getInsets();
					Rectangle bounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);
		
					mCorrelationCoefficient = null;

					// font size limitations used to cause different view layouts when resizing a view, TLS 20-Dez-2013
					mFontHeight = (int)(mRelativeFontSize * Math.sqrt(bounds.width*bounds.height) / 60f);
//					mFontHeight = (int)(mRelativeFontSize * Math.max(Math.min(bounds.width/60, 14), 7));
		
					mG = mOffG;
//long millis = System.currentTimeMillis();
					paintContent(bounds);
					paintLegend(bounds);
//System.out.println("used:"+(System.currentTimeMillis()-millis));
					}
				finally {
				// It's always best to dispose of your Graphics objects.
					mOffG.dispose();
					}
				} while (mOffImage.contentsLost());

			mOffImageValid = true;
			}

		g.drawImage(mOffImage, 0, 0, width, height, this);
		if (mActivePoint != null
		 && isVisible(mActivePoint))
			markReference(g);

		if (mHighlightedPoint != null)
			markHighlighted(g);

		drawSelectionOutline(g);
		}

/*	public void paintComponent(Graphics g) {	// this is the old BufferedImage based approach
		super.paintComponent(g);
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		mIsHighResolution = false;

		int width = getWidth();
		int height = getHeight();

		if (mOffImage == null
		 || mOffImage.getWidth(null) != (int)(width*sRetinaFactor)
		 || mOffImage.getHeight(null) != (int)(height*sRetinaFactor)) {
//			mOffImage = createImage((int)(width*sRetinaFactor), (int)(height*sRetinaFactor));
			mOffImage = new BufferedImage((int)(width*sRetinaFactor), (int)(height*sRetinaFactor), BufferedImage.TYPE_INT_ARGB);
			mOffG = mOffImage.getGraphics();
			if (sRetinaFactor != 1f)
				((Graphics2D)mOffG).scale(sRetinaFactor, sRetinaFactor);
			((Graphics2D)mOffG).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//			((Graphics2D)mOffG).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	no sub-pixel accuracy looks cleaner
			mCoordinatesValid = false;
			}

		if (!mCoordinatesValid)
			mOffImageValid = false;

		if (!mOffImageValid) {
			mOffG.setColor(getViewBackground());
			mOffG.fillRect(0, 0, width, height);
			Insets insets = getInsets();
			Rectangle bounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);

			mFontHeight = (int)(mRelativeFontSize * Math.max(Math.min(bounds.width/60, 14), 7));

			mG = mOffG;
long millis = System.currentTimeMillis();
			paintContent(bounds);
			paintLegend(bounds);
System.out.println("used:"+(System.currentTimeMillis()-millis));

			mOffImageValid = true;
			}

		g.drawImage(mOffImage, 0, 0, width, height, this);
		if (mActivePoint != null
		 && isVisible(mActivePoint))
			markReference(g);

		if (mHighlightedPoint != null)
			markHighlighted(g);

		drawSelectionOutline(g);
		}*/

	public int print(Graphics g, PageFormat f, int pageIndex) {
		if (pageIndex != 0)
			return NO_SUCH_PAGE;

		Rectangle bounds = new Rectangle((int)(cPrintScaling * f.getImageableX()),
										 (int)(cPrintScaling * f.getImageableY()),
										 (int)(cPrintScaling * f.getImageableWidth()),
										 (int)(cPrintScaling * f.getImageableHeight()));

		paintHighResolution(g, bounds, cPrintScaling, false, true);

		return PAGE_EXISTS;
		}

	@Override
	public void paintHighResolution(Graphics g, Rectangle bounds, float fontScaling, boolean transparentBG, boolean isPrinting) {
			// font sizes are optimized for screen resolution are need to be scaled by fontScaling
		mIsHighResolution = true;
		mFontScaling = fontScaling;

		mCoordinatesValid = false;
		mBackgroundValid = false;

		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	no sub-pixel accuracy looks cleaner

					// font size limitations used to cause different view layouts when resizing a view, TLS 20-Dez-2013
//		mFontHeight = (int)(mRelativeFontSize * Math.max(Math.min(bounds.width/60, 14*fontScaling), 7*fontScaling));
		mFontHeight = (int)(mRelativeFontSize * Math.sqrt(bounds.width*bounds.height) / 60f);

		mG = g;

		if (isPrinting)
				// fontScaling was also used to inflate bounds to gain resolution
				// and has to be compensated by inverted scaling of the g2D
			((Graphics2D)g).scale(1.0/fontScaling, 1.0/fontScaling);

		if (!transparentBG) {
			setColor(getViewBackground());
			fillRect(0, 0, bounds.width, bounds.height);
			}

		if (bounds.width > 0 && bounds.height > 0)
			paintContent(bounds);
		paintLegend(bounds);

		mCoordinatesValid = false;
		mBackgroundValid = false;
		}

	private void paintContent(final Rectangle bounds) {
		mChartInfo = null;
		switch (mChartType) {
		case cChartTypeBoxPlot:
		case cChartTypeWhiskerPlot:
			calculateBoxPlot();
			break;
		case cChartTypeBars:
		case cChartTypePies:
			calculateBarsOrPies();
			break;
			}

		calculateMarkerSize(bounds);
		calculateLegend(bounds);

		if (mSplittingColumn[0] != cColumnUnassigned) {
			int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
			if (mColorLegend != null || mSizeLegend != null || mShapeLegend != null || mMultiValueLegend != null || mBackgroundLegend != null)
				bounds.height -= scaledFontHeight / 2;
			compileSplittingHeaderMolecules();
			int count1 = mTableModel.getCategoryCount(mSplittingColumn[0]);
			int count2 = (mSplittingColumn[1] == cColumnUnassigned) ? -1
					   : mTableModel.getCategoryCount(mSplittingColumn[1]);
			boolean largeHeader = (mSplittingDepictor[0] != null
								|| mSplittingDepictor[1] != null);
			mSplitter = new VisualizationSplitter(bounds, count1, count2, scaledFontHeight, largeHeader);
			float titleBrightness = ColorHelper.perceivedBrightness(getTitleBackground());
			float backgroundBrightness = ColorHelper.perceivedBrightness(getViewBackground());
			Color borderColor = (backgroundBrightness > titleBrightness) ? getTitleBackground().darker().darker()
																		 : getTitleBackground().brighter().brighter();
			mSplitter.paintGrid(mG, borderColor, getTitleBackground());
			for (int hv=0; hv<mHVCount; hv++)
				paintGraph(mSplitter.getGraphBounds(hv), hv);

			mG.setColor(getContrastGrey(SCALE_STRONG, getTitleBackground()));
			mG.setFont(new Font("Helvetica", Font.BOLD, scaledFontHeight));
			for (int hv=0; hv<mHVCount; hv++) {
				Rectangle titleArea = mSplitter.getTitleBounds(hv);
				mG.setClip(titleArea);

				int molWidth = Math.min(titleArea.width*2/5, titleArea.height*3/2);
				int cat1Index = (mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv);
				String shortTitle1 = mTableModel.getCategoryList(mSplittingColumn[0])[cat1Index];
				String title1 = mTableModel.getColumnTitle(mSplittingColumn[0])+" = "+shortTitle1;
				int title1Width = mSplittingDepictor[0] == null ?
								  mG.getFontMetrics().stringWidth(title1)
								: molWidth;
				String shortTitle2 = null;
				String title2 = null;
				int title2Width = 0;
				int totalWidth = title1Width;
				if (mSplittingColumn[1] != cColumnUnassigned) {
					shortTitle2 = mTableModel.getCategoryList(mSplittingColumn[1])[mSplitter.getVIndex(hv)];
					title2 = mTableModel.getColumnTitle(mSplittingColumn[1])+" = "+shortTitle2;
					title2Width = mSplittingDepictor[1] == null ?
								  mG.getFontMetrics().stringWidth(title2)
								: molWidth;
					totalWidth += title2Width + mG.getFontMetrics().stringWidth("|");
					}

				int textY = titleArea.y+(1+titleArea.height-scaledFontHeight)/2+mG.getFontMetrics().getAscent();

				if (totalWidth > titleArea.width) {
					title1 = shortTitle1;
					title1Width = mSplittingDepictor[0] == null ?
								  mG.getFontMetrics().stringWidth(shortTitle1)
								: molWidth;
					totalWidth = title1Width;
					if (mSplittingColumn[1] != cColumnUnassigned) {
						title2 = shortTitle2;
						title2Width = mSplittingDepictor[1] == null ?
									  mG.getFontMetrics().stringWidth(title2)
									: molWidth;
						totalWidth += title2Width + mG.getFontMetrics().stringWidth("|");
						}
					}

				int x1 = titleArea.x+(titleArea.width-totalWidth)/2;
				if (mSplittingDepictor[0] == null)
					mG.drawString(title1, x1, textY);
				else if (mSplittingDepictor[0][cat1Index] != null) {
					Rectangle.Float r = new Rectangle.Float(x1, titleArea.y, molWidth, titleArea.height);
					int maxAVBL = Depictor2D.cOptAvBondLen;
					if (mIsHighResolution)
						maxAVBL *= mFontScaling;
					mSplittingDepictor[0][cat1Index].validateView(mG, r, Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
					mSplittingDepictor[0][cat1Index].paint(mG);
					}

				if (mSplittingColumn[1] != cColumnUnassigned) {
					mG.drawString("|", titleArea.x+(titleArea.width-totalWidth)/2+title1Width,
								  textY);

					int x2 = titleArea.x+(totalWidth+titleArea.width)/2-title2Width;
					if (mSplittingDepictor[1] == null)
						mG.drawString(title2, x2, textY);
					else if (mSplittingDepictor[1][mSplitter.getVIndex(hv)] != null) {
						Rectangle.Float r = new Rectangle.Float(x2, titleArea.y, molWidth, titleArea.height);
						int maxAVBL = Depictor2D.cOptAvBondLen;
						if (mIsHighResolution)
							maxAVBL *= mFontScaling;
						mSplittingDepictor[1][mSplitter.getVIndex(hv)].validateView(mG, r, Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
						mSplittingDepictor[1][mSplitter.getVIndex(hv)].paint(mG);
						}
					}
				}
			setFontHeightAndScaleToSplitView(mFontHeight);	// set font back to plain
			mG.setClip(null);
			}
		else {
			mSplitter = null;
			paintGraph(bounds, 0);
			}

		Rectangle baseGraphRect = getGraphBounds(mSplittingColumn[0] == cColumnUnassigned ?
											bounds : mSplitter.getGraphBounds(0));

		if (baseGraphRect.width <= 0 || baseGraphRect.height <= 0)
			return;

		switch (mChartType) {
		case cChartTypeBars:
			paintBarChart(mG, baseGraphRect);
			break;
		case cChartTypePies:
			paintPieChart(mG, baseGraphRect);
			break;
		case cChartTypeScatterPlot:
			paintMarkers(baseGraphRect);
			break;
		case cChartTypeBoxPlot:
		case cChartTypeWhiskerPlot:
			paintMarkers(baseGraphRect);
			paintBoxOrWhiskerPlot(mG, baseGraphRect);
			break;
			}
		}

	/**
	 * Returns the bounds of the graph area, provided that the given point
	 * is part of it. If we have split views, then the graph area of that view
	 * is returned, which contains the the given point.
	 * If point(x,y) is outside of the/an graph area, then null is returned.
	 * Scale, legend and border area is not part of the graph bounds.
	 * @param screenX
	 * @param screenY
	 * @return graph bounds or null
	 */
	public Rectangle getGraphBounds(int screenX, int screenY) {
		int width = getWidth();
		int height = getHeight();
		Insets insets = getInsets();
		Rectangle allBounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);
		if (mSplittingColumn[0] == cColumnUnassigned) {
			if (mColorLegend != null)
				allBounds.height -= mColorLegend.getHeight(); 
			if (mSizeLegend != null)
				allBounds.height -= mSizeLegend.getHeight(); 
			if (mShapeLegend != null)
				allBounds.height -= mShapeLegend.getHeight(); 
			if (mMultiValueLegend != null)
				allBounds.height -= mMultiValueLegend.getHeight();
			if (mBackgroundLegend != null)
				allBounds.height -= mBackgroundLegend.getHeight();
			Rectangle bounds = getGraphBounds(allBounds);
			return bounds.contains(screenX, screenY) ? bounds : null;
			}
		else {
			for (int hv=0; hv<mHVCount; hv++) {
				Rectangle bounds = getGraphBounds(mSplitter.getGraphBounds(hv));
				if (bounds.contains(screenX, screenY))
					return bounds;
				}
			return null;
			}
		}

	private Rectangle getGraphBounds(Rectangle bounds) {
		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		return (mSuppressScale || mTreeNodeList != null) ?
				new Rectangle(bounds.x + mBorder + mNaNSize[0],
						bounds.y + mBorder,
						bounds.width - mNaNSize[0] - 2 * mBorder,
						bounds.height - mNaNSize[1] - 2 * mBorder)
			  : new Rectangle(bounds.x + mBorder + mScaleSize[1] + mNaNSize[0],
					  	bounds.y + mBorder + scaledFontHeight,
					  	bounds.width - mScaleSize[1] - mNaNSize[0] - 2 * mBorder,
					  	bounds.height - mScaleSize[0] - mNaNSize[1] - 2 * mBorder - 2 * scaledFontHeight);
		}

	private void paintGraph(Rectangle bounds, int hvIndex) {
		setFontHeightAndScaleToSplitView(mFontHeight);  // calculateCoordinates() requires a proper getStringWidth()
		if (!mCoordinatesValid)
			calculateCoordinates(mG, bounds);

		Rectangle graphRect = getGraphBounds(bounds);

		if (hasColorBackground()) {
			if (mSplitter != null
			 && (mSplitter.getHCount() != mBackgroundHCount
			  || mSplitter.getVCount() != mBackgroundVCount))
				mBackgroundValid = false;

			if (!mBackgroundValid)
				calculateBackground(graphRect);
			}

		if (mShowNaNValues)
			drawNaNArea(mG, graphRect);

		if (mBackgroundImage != null
		 || hasColorBackground())
			drawBackground(mG, graphRect, hvIndex);

		if (mTreeNodeList == null) {
			if (!(mSuppressGrid && mSuppressScale))
				drawGrid(mG, graphRect);
			if (!mSuppressScale)
				drawAxes(mG, graphRect);
			}
		}

	private boolean hasColorBackground() {
		return mChartType != cChartTypeBars
	   		&& mChartType != cChartTypeBoxPlot
	   		&& mBackgroundColor.getColorColumn() != cColumnUnassigned;
		}

	private void paintMarkers(Rectangle baseGraphRect) {
		if (mTreeNodeList != null && mTreeNodeList.length == 0)
			return;	// We have a detail graph view without root node (no active row chosen)

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = ((Graphics2D)mG).getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			((Graphics2D)mG).setComposite(composite);
			}

		boolean showAnyLabels = showAnyLabels();
		boolean drawConnectionLinesInFocus = (mChartType == cChartTypeScatterPlot
										   || mTreeNodeList != null) ? drawConnectionLines() : false;

		if (mConnectionColumn != cColumnUnassigned
		 && mRelativeMarkerSize == 0.0
		 && !showAnyLabels) {
			// don't draw markers if we have connection lines and marker size is zero
			if (drawConnectionLinesInFocus)
				drawConnectionLines(true, true);
			}
		else {
			int focusFlagNo = getFocusFlag();
			int firstFocusIndex = 0;
			if (focusFlagNo != -1) {
				int index2 = mDataPoints-1;
				while (firstFocusIndex<index2) {
					if (mPoint[firstFocusIndex].record.isFlagSet(focusFlagNo)) {
						while (mPoint[index2].record.isFlagSet(focusFlagNo)
							&& index2 > firstFocusIndex)
							index2--;
						if (index2 == firstFocusIndex)
							break;
						VisualizationPoint temp = mPoint[firstFocusIndex];
						mPoint[firstFocusIndex] = mPoint[index2];
						mPoint[index2] = temp;
						}
					firstFocusIndex++;
					}
				}

			boolean isTreeView = isTreeViewGraph();
			boolean isDarkBackground = (ColorHelper.perceivedBrightness(getViewBackground()) <= 0.5);
			MultiValueBars mvbi = (mMultiValueMarkerMode == cMultiValueMarkerModeBars && mMultiValueMarkerColumns != null) ?
					new MultiValueBars() : null;


			for (int i=0; i<mDataPoints; i++) {
				if (drawConnectionLinesInFocus && i == firstFocusIndex)
					drawConnectionLines(true, true);

				if (isVisible(mPoint[i])
				 && (mChartType == cChartTypeScatterPlot
				  || mChartType == cChartTypeWhiskerPlot
				  || mPoint[i].chartGroupIndex == -1
				  || mTreeNodeList != null)) {
					VisualizationPoint vp = mPoint[i];
					vp.width = vp.height = (int)getMarkerSize(vp);
					boolean inFocus = (focusFlagNo == -1 || vp.record.isFlagSet(focusFlagNo));

					Color color = (vp.record.isSelected() && mFocusHitlist != cFocusOnSelection) ?
									VisualizationColor.cSelectedColor : mMarkerColor.getColorList()[vp.colorIndex];

					if (vp.width != 0
					 && (mLabelColumn[MarkerLabelDisplayer.cMidCenter] == cColumnUnassigned
					  || (mLabelsInTreeViewOnly && !isTreeView))) {
						if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null) {
							if (mMultiValueMarkerMode == cMultiValueMarkerModeBars)
								drawMultiValueBars(color, inFocus, isDarkBackground, vp.width, mvbi, vp);
							else
								drawMultiValuePies(color, inFocus, isDarkBackground, vp.width, vp);
							}
						else {
							Color markerColor = inFocus ? color : VisualizationColor.lowContrastColor(color, getViewBackground());
							Color outlineColor = isDarkBackground ? markerColor.brighter() : markerColor.darker();
							drawMarker(markerColor, outlineColor, vp.shape, vp.width, vp.screenX, vp.screenY);
							}
						}

					if (inFocus && showAnyLabels)
						drawMarkerLabels(vp, color, isTreeView);
					}
				}
			}

		if (original != null)
			((Graphics2D)mG).setComposite(original);

		if (mChartType == cChartTypeScatterPlot && mTreeNodeList == null) {
			if (mCurveInfo != cCurveModeNone)
				drawCurves(baseGraphRect);

			if (mShownCorrelationType != CorrelationCalculator.TYPE_NONE)
				drawCorrelationCoefficient(baseGraphRect);
			}
		}

	private void drawMarkerLabels(VisualizationPoint vp, Color color, boolean isTreeView) {
		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mMarkerLabelSize * mFontHeight);

		boolean isLightBackground = (ColorHelper.perceivedBrightness(getViewBackground()) > 0.5);
		if (isLightBackground)
			color = color.darker();

		if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != cColumnUnassigned
		 && (!mLabelsInTreeViewOnly || isTreeView))
			drawMarkerLabel(vp, MarkerLabelDisplayer.cMidCenter, color, isTreeView);

		for (int i=0; i<MarkerLabelDisplayer.cPositionCode.length; i++)
			if (i != MarkerLabelDisplayer.cMidCenter
			 && mLabelColumn[i] != cColumnUnassigned
			 && (!mLabelsInTreeViewOnly || isTreeView))
				drawMarkerLabel(vp, i, color, isTreeView);

		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mFontHeight);
		}

	/**
	 * Draws the marker label at the specified position considering vp.width and vp.height
	 * for exact label location. If position is midCenter and therefore replaces
	 * the original marker and, thus, redefines marker dimensions as being mid-center label size,
	 * then vp.width and vp.height are updated in place to hold the label size instead of marker size.
	 * @param point
	 * @param position
	 * @param color
	 */
	private void drawMarkerLabel(VisualizationPoint vp, int position, Color color, boolean isTreeView) {
		int column = mLabelColumn[position];
		boolean isMolecule = CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column));
		int x = vp.screenX;
		int y = vp.screenY;
		int w = 0;
		int h = 0;
		String label = null;
		Depictor2D depictor = null;
		Rectangle2D.Float molRect = null;

		// in case we have an empty label replacing the marker
		if (position == MarkerLabelDisplayer.cMidCenter) {
			vp.width = 0;
			vp.height = 0;
			}

		if (isMolecule) {
			if (mLabelMolecule == null)
				mLabelMolecule = new StereoMolecule();
			StereoMolecule mol = mTableModel.getChemicalStructure(vp.record, column, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, mLabelMolecule);
			if (mol == null)
				return;

			depictor = new Depictor2D(mol);
			depictor.validateView(mG, DEPICTOR_RECT,
								  Depictor2D.cModeInflateToHighResAVBL+Math.max(1, (int)(256*scaleIfSplitView(getLabelAVBL(vp, position, isTreeView)))));
			molRect = depictor.getBoundingRect();
			w = (int)molRect.width;
			h = (int)molRect.height;
			}
		else {
			label = mTableModel.getValue(vp.record, column);
			if (label.length() == 0)
				return;

			setFontHeightAndScaleToSplitView(getLabelFontSize(vp, position, isTreeView));
			w = mG.getFontMetrics().stringWidth(label);
			h = mG.getFontMetrics().getHeight();
			}

		switch (position) {
		case MarkerLabelDisplayer.cTopLeft:
			x -= vp.width/2 + w;
			y -= vp.height/2 + h;
			break;
		case MarkerLabelDisplayer.cTopCenter:
			x -= w/2;
			y -= vp.height/2 + h;
			break;
		case MarkerLabelDisplayer.cTopRight:
			x += vp.width/2;
			y -= vp.height/2 + h;
			break;
		case MarkerLabelDisplayer.cMidLeft:
			x -= vp.width*2/3 + w;
			y -= h/2;
			break;
		case MarkerLabelDisplayer.cMidCenter:
			vp.width = w;
			vp.height = h;
			x -= w/2;
			y -= h/2;
			break;
		case MarkerLabelDisplayer.cMidRight:
			x += vp.width*2/3;
			y -= h/2;
			break;
		case MarkerLabelDisplayer.cBottomLeft:
			x -= vp.width/2 + w;
			y += vp.height/2;
			break;
		case MarkerLabelDisplayer.cBottomCenter:
			x -= w/2;
			y += vp.height/2;
			break;
		case MarkerLabelDisplayer.cBottomRight:
			x += vp.width/2;
			y += vp.height/2;
			break;
			}

		if (isMolecule) {
			depictor.applyTransformation(new DepictorTransformation(1.0f, x - molRect.x, y - molRect.y));
			depictor.setOverruleColor(color, getViewBackground());
			depictor.paint(mG);
			}
		else {
			mG.setColor(color);
			mG.drawString(label, x, y + mG.getFontMetrics().getAscent());
			}
		}

	protected void drawMarker(Color color, int shape, int size, int x, int y) {
		drawMarker(color, getContrastGrey(MARKER_OUTLINE), shape, size, x, y);
		}

	private void drawMarker(Color color, Color outlineColor,
							int shape, int size, int x, int y) {
		int halfSize = size/2;
		int sx,sy;
		Polygon p;

		mG.setColor(color);
		switch (shape) {
		case 0:
			mG.fillRect(x-halfSize, y-halfSize, size, size);
			if (size > 4) {
				mG.setColor(outlineColor);
				mG.drawRect(x-halfSize, y-halfSize, size, size);
				}
			break;
		case 1:
			mG.fillOval(x-halfSize, y-halfSize, size, size);
			if (size > 4) {
				mG.setColor(outlineColor);
				mG.drawOval(x-halfSize, y-halfSize, size, size);
				}
			break;
		case 2:
			mX[0] = x-halfSize;
			mX[1] = x+halfSize;
			mX[2] = x;
			mY[0] = mY[1] = y+size/3;
			mY[2] = mY[0]-size;
			p = new Polygon(mX, mY, 3);
			mG.fillPolygon(p);
			if (size > 4) {
				mG.setColor(outlineColor);
				mG.drawPolygon(p);
				}
			break;
		case 3:
			mX[0] = x-halfSize;
			mX[1] = mX[3] = x;
			mX[2] = x+halfSize;
			mY[0] = mY[2] = y;
			mY[1] = y+halfSize;
			mY[3] = y-halfSize;
			p = new Polygon(mX, mY, 4);
			mG.fillPolygon(p);
			if (size > 4) {
				mG.setColor(outlineColor);
				mG.drawPolygon(p);
				}
			break;
		case 4:
			mX[0] = x-halfSize;
			mX[1] = x+halfSize;
			mX[2] = x;
			mY[0] = mY[1] = y-size/3;
			mY[2] = mY[0]+size;
			p = new Polygon(mX, mY, 3);
			mG.fillPolygon(p);
			if (size > 4) {
				mG.setColor(outlineColor);
				mG.drawPolygon(p);
				}
			break;
		case 5:
			sx = size/4;
			sy = sx+halfSize;
			mG.fillRect(x-sx, y-sy, 2*sx, 2*sy);
			if (size > 4) {
				mG.setColor(outlineColor);
				mG.drawRect(x-sx, y-sy, 2*sx, 2*sy);
				}
			break;
		case 6:
			sy = size/4;
			sx = sy+halfSize;
			mG.fillRect(x-sx, y-sy, 2*sx, 2*sy);
			if (size > 4) {
				mG.setColor(outlineColor);
				mG.drawRect(x-sx, y-sy, 2*sx, 2*sy);
				}
			break;
			}
		}

	private void drawMultiValueBars(Color color, boolean inFocus, boolean isDarkBackground, float size, MultiValueBars info, VisualizationPoint vp) {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned && color != VisualizationColor.cSelectedColor) {
			if (mMultiValueMarkerColor == null || mMultiValueMarkerColor.length != mMultiValueMarkerColumns.length)
				mMultiValueMarkerColor = VisualizationColor.createDiverseColorList(mMultiValueMarkerColumns.length);
			color = null;
			}

		info.calculate(size, vp);
		int x = info.firstBarX;
		mG.setColor(getContrastGrey(1f));
		mG.drawLine(x-info.barWidth/2, info.zeroY, x+mMultiValueMarkerColumns.length*info.barWidth+info.barWidth/2, info.zeroY);
		for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
			if (!Float.isNaN(info.relValue[i])) {
				Color barColor = (color != null) ? color : mMultiValueMarkerColor[i];
				Color fillColor = inFocus ? barColor : VisualizationColor.lowContrastColor(barColor, getViewBackground());
				mG.setColor(fillColor);
				mG.fillRect(x, info.barY[i], info.barWidth, info.barHeight[i]);
				}
			x += info.barWidth;
			}
		}

	private void drawMultiValuePies(Color color, boolean inFocus, boolean isDarkBackground, float size, VisualizationPoint vp) {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned && color != VisualizationColor.cSelectedColor) {
			if (mMultiValueMarkerColor == null || mMultiValueMarkerColor.length != mMultiValueMarkerColumns.length)
				mMultiValueMarkerColor = VisualizationColor.createDiverseColorList(mMultiValueMarkerColumns.length);
			color = null;
			}

		size *= 0.5f  * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
			// one sqrt because of area, 2nd sqrt to grow under-proportional with number of pie pieces

		float angleIncrement = 360f / mMultiValueMarkerColumns.length;
		float angle = 90f - angleIncrement;
		for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
			int r = (int)(size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]));
			Color piePieceColor = (color != null) ? color : mMultiValueMarkerColor[i];
			Color fillColor = inFocus ? piePieceColor : VisualizationColor.lowContrastColor(piePieceColor, getViewBackground());
			mG.setColor(fillColor);
			mG.fillArc(vp.screenX-r, vp.screenY-r, 2*r-1, 2*r-1, Math.round(angle), Math.round(angleIncrement));
			Color lineColor = isDarkBackground ? fillColor.brighter() : fillColor.darker();
			mG.setColor(lineColor);
			mG.drawArc(vp.screenX-r, vp.screenY-r, 2*r-1, 2*r-1, Math.round(angle), Math.round(angleIncrement));
			angle -= angleIncrement;
			}
		}

	/**
	 * If no connection lines need to be drawn, then this method does nothing and returns false.
	 * Otherwise, if no focus is set, then this method draws all connection lines and returns false.
	 * Otherwise, this method draws those lines connecting markers, which are not in focus and
	 * returns true to indicate that connection line drawing is not completed yet. In this case
	 * drawConnectionLines(true, true) needs to be called after drawing those markers that are
	 * not in focus.
	 * @return true if drawConnectionLines(true, true) needs to be called later
	 */
	private boolean drawConnectionLines() {
		if (mPoint.length == 0)
			return false;

		if (mConnectionColumn == cColumnUnassigned
		 || mConnectionColumn == cConnectionColumnConnectCases)
			return false;

		if (!mIsHighResolution && mAbsoluteConnectionLineWidth < 0.5f)
			return false;

		String value = (mConnectionColumn < 0) ?
				null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);
		if (value == null)
			return drawCategoryConnectionLines();

		int referencedColumn = mTableModel.findColumn(value);
		if (referencedColumn != -1)
			return drawReferenceConnectionLines(referencedColumn);

		return false;
		}

	/**
	 * 
	 * @param considerFocus
	 * @param inFocus
	 */
	private void drawConnectionLines(boolean considerFocus, boolean inFocus) {
		String value = (mConnectionColumn < 0) ?
				null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);

		if (value == null)
			drawCategoryConnectionLines(considerFocus, inFocus);
		else
			drawReferenceConnectionLines(considerFocus, inFocus);
		}

	private boolean drawCategoryConnectionLines() {
		int connectionOrderColumn = (mConnectionOrderColumn == cColumnUnassigned) ?
										mAxisIndex[0] : mConnectionOrderColumn;
		if (connectionOrderColumn == cColumnUnassigned)
			return false;

		if (mConnectionLinePoint == null || mConnectionLinePoint.length != mPoint.length)
			mConnectionLinePoint = new VisualizationPoint[mPoint.length];
		for (int i=0; i<mPoint.length; i++)
			mConnectionLinePoint[i] = mPoint[i];

		Arrays.sort(mConnectionLinePoint, new Comparator<VisualizationPoint>() {
			public int compare(VisualizationPoint p1, VisualizationPoint p2) {
				return compareConnectionLinePoints(p1, p2);
				}
			} );

		mConnectionFromIndex1 = 0;
		while (mConnectionFromIndex1<mConnectionLinePoint.length
			&& !isVisibleExcludeNaN(mConnectionLinePoint[mConnectionFromIndex1]))
			mConnectionFromIndex1++;

		if (mConnectionFromIndex1 == mConnectionLinePoint.length)
			return false;

		mConnectionFromIndex2 = getNextChangedConnectionLinePointIndex(mConnectionFromIndex1);
		if (mConnectionFromIndex2 == mConnectionLinePoint.length)
			return false;

		drawCategoryConnectionLines(mFocusHitlist != cHitlistUnassigned, false);
		return (mFocusHitlist != cHitlistUnassigned);
		}

	private void drawCategoryConnectionLines(boolean considerFocus, boolean inFocus) {
		long focusMask = (mFocusHitlist == cHitlistUnassigned) ? 0
					   : (mFocusHitlist == cFocusOnSelection) ? CompoundRecord.cFlagMaskSelected
					   : mTableModel.getHitlistHandler().getHitlistMask(mFocusHitlist);

		int fromIndex1 = mConnectionFromIndex1;
		int fromIndex2 = mConnectionFromIndex2;

		Stroke oldStroke = ((Graphics2D)mG).getStroke();
		((Graphics2D)mG).setStroke(new BasicStroke(mAbsoluteConnectionLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		while (true) {
			int toIndex1 = fromIndex2;

			while (toIndex1<mConnectionLinePoint.length
				&& !isVisibleExcludeNaN(mConnectionLinePoint[toIndex1]))
				toIndex1++;

			if (toIndex1 == mConnectionLinePoint.length) {
				((Graphics2D)mG).setStroke(oldStroke);
				return;
				}

			int toIndex2 = getNextChangedConnectionLinePointIndex(toIndex1);

			if (isConnectionLinePossible(mConnectionLinePoint[fromIndex1], mConnectionLinePoint[toIndex1]))
				for (int i=fromIndex1; i<fromIndex2; i++)
					if (isVisibleExcludeNaN(mConnectionLinePoint[i]))
						for (int j=toIndex1; j<toIndex2; j++)
							if (isVisibleExcludeNaN(mConnectionLinePoint[j])
							 && (!considerFocus
							  || (inFocus
								^ (mConnectionLinePoint[j].record.getFlags() & focusMask) == 0)))
								drawConnectionLine(mConnectionLinePoint[i], mConnectionLinePoint[j], considerFocus && !inFocus, 0.0f);

			fromIndex1 = toIndex1;
			fromIndex2 = toIndex2;
			}
		}

	private boolean drawReferenceConnectionLines(int referencedColumn) {
		if (mConnectionLineMap == null)
			mConnectionLineMap = createReferenceMap(mConnectionColumn, referencedColumn);

		drawReferenceConnectionLines(mFocusHitlist != cHitlistUnassigned, false);			

		return (mFocusHitlist != cHitlistUnassigned);
		}

	private void drawReferenceConnectionLines(boolean considerFocus, boolean inFocus) {
		long focusMask = (mFocusHitlist == cHitlistUnassigned) ? 0
					   : (mFocusHitlist == cFocusOnSelection) ? CompoundRecord.cFlagMaskSelected
					   : mTableModel.getHitlistHandler().getHitlistMask(mFocusHitlist);
		int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn,
				CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
		boolean isRedundant = CompoundTableConstants.cColumnPropertyReferenceTypeRedundant.equals(
				mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType));

		Stroke oldStroke = ((Graphics2D)mG).getStroke();
		((Graphics2D)mG).setStroke(new BasicStroke(mAbsoluteConnectionLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		Composite original = (strengthColumn == -1) ? null : ((Graphics2D)mG).getComposite();

		if (mTreeNodeList != null) {
			for (int layer=1; layer<mTreeNodeList.length; layer++) {
				for (VisualizationNode node:mTreeNodeList[layer]) {
					VisualizationPoint vp1 = node.getVisualizationPoint();
					VisualizationPoint vp2 = node.getParentNode().getVisualizationPoint();
					float strength = node.getStrength();
					if (isVisible(vp1)
					 && isVisible(vp2)
					 && (!considerFocus
					  || (inFocus
						^ (vp1.record.getFlags() & vp2.record.getFlags() & focusMask) == 0))) {
						drawConnectionLine(vp1, vp2, considerFocus && !inFocus, strength);
						}
					}
				}
			}
		else {
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

			for (VisualizationPoint vp1:mPoint) {
				if (isVisible(vp1)) {
					byte[] data = (byte[])vp1.record.getData(mConnectionColumn);
					if (data != null) {
						String[] entry = mTableModel.separateEntries(new String(data));
	
						String[] strength = null;
						if (strengthColumn != -1) {
							byte[] strengthData = (byte[])vp1.record.getData(strengthColumn);
							if (strengthData != null) {
								strength = mTableModel.separateEntries(new String(strengthData));
								if (strength.length != entry.length)
									strength = null;
								}
							}
	
						int index = 0;
						for (String ref:entry) {
							VisualizationPoint vp2 = mConnectionLineMap.get(ref.getBytes());
							if (vp2 != null && isVisible(vp2)
							 && (!isRedundant || (vp1.record.getID() < vp2.record.getID()))
							 && (!considerFocus
							  || (inFocus
							   ^ (vp1.record.getFlags() & vp2.record.getFlags() & focusMask) == 0))) {
								float transparency = 0.0f;
								if (strength != null) {
									try {
										float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strength[index++], strengthColumn)));
										transparency = Float.isNaN(value) ? 1.0f : (float)((max-value) / dif);
										}
									catch (NumberFormatException nfe) {}
									}
								if (transparency != 1.0f) {
									drawConnectionLine(vp1, vp2, considerFocus && !inFocus, transparency);
									}
								}
							}
						}
					}
				}
			}

		if (original != null)
			((Graphics2D)mG).setComposite(original);

		((Graphics2D)mG).setStroke(oldStroke);
		}

	/**
	 * Draws a connection line between the given points. 
	 * If transparency is different from 0.0, then this method sets a Composite on mG. In this case the calling
	 * method needs to save and restore the old Composite before/after calling this method.
	 * If fast render mode is active, trasparency is simulated by adapting the line color to the current background.
	 * @param p1
	 * @param p2
	 * @param outOfFocus
	 * @param transparency if 0.0 then the current composite is not touched and the line drawn with the current g2d transparency
	 */
	private void drawConnectionLine(VisualizationPoint p1, VisualizationPoint p2, boolean outOfFocus, float transparency) {
		Color color = ColorHelper.intermediateColor(mMarkerColor.getColorList()[p1.colorIndex],
													mMarkerColor.getColorList()[p2.colorIndex], 0.5f);
		if (transparency != 0.0f) {
			if (!mIsFastRendering || mIsHighResolution)
				((Graphics2D)mG).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
			else
				color = ColorHelper.intermediateColor(color, getViewBackground(), transparency);
			}
		if (outOfFocus)
			color = VisualizationColor.lowContrastColor(color, getViewBackground());
		mG.setColor(color);
		if (mIsHighResolution)
			((Graphics2D)mG).draw(new Line2D.Float(p1.screenX, p1.screenY, p2.screenX, p2.screenY));
		else
			mG.drawLine(p1.screenX, p1.screenY, p2.screenX, p2.screenY);
		}

	private void paintBarChart(Graphics g, Rectangle baseRect) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		float axisSize = mChartInfo.axisMax - mChartInfo.axisMin;

		float cellWidth = (mChartInfo.barAxis == 1) ?
				(float)baseRect.width / (float)mCategoryVisibleCount[0]
			  : (float)baseRect.height / (float)mCategoryVisibleCount[1];
		float cellHeight = (mChartInfo.barAxis == 1) ?
				(float)baseRect.height / (float)mCategoryVisibleCount[1]
			  : (float)baseRect.width / (float)mCategoryVisibleCount[0];

		mChartInfo.barWidth = Math.min(0.2f * cellHeight, 0.5f * cellWidth / mCaseSeparationCategoryCount);

		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 1;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int catCount = mCategoryVisibleCount[0]*mCategoryVisibleCount[1]*mCaseSeparationCategoryCount; 

		float barBaseOffset = (mChartInfo.barBase - mChartInfo.axisMin) * cellHeight / axisSize;

		mChartInfo.innerDistance = new float[mHVCount][catCount];
		float[][] barPosition = new float[mHVCount][catCount];
		float[][][] barColorEdge = new float[mHVCount][catCount][colorCount+1];
		float csWidth = (mChartInfo.barAxis == 1 ? cellWidth : -cellWidth)
					   * mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (mSplittingColumn[0] != cColumnUnassigned) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i=0; i<mCategoryVisibleCount[0]; i++) {
				for (int j=0; j<mCategoryVisibleCount[1]; j++) {
					for (int k=0; k<mCaseSeparationCategoryCount; k++) {
						int cat = (i+j*mCategoryVisibleCount[0])*mCaseSeparationCategoryCount+k;
						if (mChartInfo.pointsInCategory[hv][cat] > 0) {
							mChartInfo.innerDistance[hv][cat] = cellHeight * Math.abs(mChartInfo.barValue[hv][cat] - mChartInfo.barBase)
													   / (axisSize * (float)mChartInfo.pointsInCategory[hv][cat]);
							barPosition[hv][cat] = (mChartInfo.barAxis == 1) ?
									  baseRect.x + hOffset + i*cellWidth + cellWidth/2
									: baseRect.y + vOffset + baseRect.height - j*cellWidth - cellWidth/2;
	
							if (mCaseSeparationCategoryCount != 1)
								barPosition[hv][cat] += csOffset + k*csWidth;
	
							float barOffset = (mChartInfo.barValue[hv][cat] >= 0.0) ? 0.0f
									: cellHeight * (mChartInfo.barValue[hv][cat] - mChartInfo.barBase) / axisSize;
							barColorEdge[hv][cat][0] = (mChartInfo.barAxis == 1) ?
									baseRect.y + vOffset - barBaseOffset - barOffset + baseRect.height - cellHeight * j
								  : baseRect.x + hOffset + barBaseOffset + barOffset + cellHeight * i;
		
							for (int l=0; l<colorCount; l++)
								barColorEdge[hv][cat][l+1] = (mChartInfo.barAxis == 1) ?
										  barColorEdge[hv][cat][l] - mChartInfo.innerDistance[hv][cat]
										  * (float)mChartInfo.pointsInColorCategory[hv][cat][l]
										: barColorEdge[hv][cat][l] + mChartInfo.innerDistance[hv][cat]
										  * (float)mChartInfo.pointsInColorCategory[hv][cat][l];
							}
						}
					}
				}
			}

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = ((Graphics2D)mG).getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			((Graphics2D)mG).setComposite(composite);
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				for (int k=0; k<colorCount; k++) {
					if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
						g.setColor(mChartInfo.color[k]);
						if (mChartInfo.barAxis == 1)
							g.fillRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
		  							   Math.round(barColorEdge[hv][cat][k+1]),
									   Math.round(mChartInfo.barWidth),
									   Math.round(barColorEdge[hv][cat][k])-Math.round(barColorEdge[hv][cat][k+1]));
						else
							g.fillRect(Math.round(barColorEdge[hv][cat][k]),
		  							   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
									   Math.round(barColorEdge[hv][cat][k+1]-Math.round(barColorEdge[hv][cat][k])),
									   Math.round(mChartInfo.barWidth));
						}
					}
				if (mChartInfo.pointsInCategory[hv][cat] > 0) {
					g.setColor(getContrastGrey(MARKER_OUTLINE));
					if (mChartInfo.barAxis == 1)
						g.drawRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
  								   Math.round(barColorEdge[hv][cat][colorCount]),
								   Math.round(mChartInfo.barWidth),
								   Math.round(barColorEdge[hv][cat][0])-Math.round(barColorEdge[hv][cat][colorCount]));
					else
						g.drawRect(Math.round(barColorEdge[hv][cat][0]),
  								   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
								   Math.round(barColorEdge[hv][cat][colorCount])-Math.round(barColorEdge[hv][cat][0]),
								   Math.round(mChartInfo.barWidth));
					}
				}
			}

		if (original != null)
			((Graphics2D)mG).setComposite(original);

		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				if (mChartInfo.barAxis == 1) {
					mPoint[i].screenX = Math.round(barPosition[hv][cat]);
					mPoint[i].screenY = Math.round(barColorEdge[hv][cat][0]
									  - mChartInfo.innerDistance[hv][cat]*(0.5f+(float)mPoint[i].chartGroupIndex));
					mPoint[i].width = Math.round(mChartInfo.barWidth);
					mPoint[i].height = Math.round(mChartInfo.innerDistance[hv][cat]);
					}
				else {
					mPoint[i].screenX = Math.round(barColorEdge[hv][cat][0]
									  + mChartInfo.innerDistance[hv][cat]*(0.5f+(float)mPoint[i].chartGroupIndex));
					mPoint[i].screenY = Math.round(barPosition[hv][cat]);
					mPoint[i].width = Math.round(mChartInfo.innerDistance[hv][cat]);
					mPoint[i].height = Math.round(mChartInfo.barWidth);
					}
				}
			}
		}

	private void paintPieChart(Graphics g, Rectangle baseRect) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		float cellWidth = (float)baseRect.width / (float)mCategoryVisibleCount[0];
		float cellHeight = (float)baseRect.height / (float)mCategoryVisibleCount[1];
		float cellSize = Math.min(cellWidth, cellHeight);

		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 1;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int catCount = mCaseSeparationCategoryCount*mCategoryVisibleCount[0]*mCategoryVisibleCount[1]; 

		mChartInfo.pieSize = new float[mHVCount][catCount];
		mChartInfo.pieX = new float[mHVCount][catCount];
		mChartInfo.pieY = new float[mHVCount][catCount];
		float[][][] pieColorEdge = new float[mHVCount][catCount][colorCount+1];
		int preferredCSAxis = (cellWidth > cellHeight) ? 0 : 1;
		float csWidth = (preferredCSAxis == 0 ? cellWidth : -cellHeight)
						* mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (mSplittingColumn[0] != cColumnUnassigned) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i=0; i<mCategoryVisibleCount[0]; i++) {
				for (int j=0; j<mCategoryVisibleCount[1]; j++) {
					for (int k=0; k<mCaseSeparationCategoryCount; k++) {
						int cat = (i+j*mCategoryVisibleCount[0])*mCaseSeparationCategoryCount+k;
						if (mChartInfo.pointsInCategory[hv][cat] > 0) {
							float relSize = Math.abs(mChartInfo.barValue[hv][cat] - mChartInfo.barBase)
											 / (mChartInfo.axisMax - mChartInfo.axisMin);
							mChartInfo.pieSize[hv][cat] = cMaxPieSize * cellSize * mRelativeMarkerSize
													  * (float)Math.sqrt(relSize);
							mChartInfo.pieX[hv][cat] = baseRect.x + hOffset + i*cellWidth + cellWidth/2;
							mChartInfo.pieY[hv][cat] = baseRect.y + vOffset + baseRect.height - j*cellHeight - cellHeight/2;
	
							if (mCaseSeparationCategoryCount != 1) {
								if (preferredCSAxis == 0)
									mChartInfo.pieX[hv][cat] += csOffset + k*csWidth;
								else
									mChartInfo.pieY[hv][cat] += csOffset + k*csWidth;
								}
	
							for (int l=0; l<colorCount; l++)
								pieColorEdge[hv][cat][l+1] = pieColorEdge[hv][cat][l] + 360.0f
										  * (float)mChartInfo.pointsInColorCategory[hv][cat][l]
										  / (float)mChartInfo.pointsInCategory[hv][cat];
							}
						}
					}
				}
			}

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = ((Graphics2D)mG).getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			((Graphics2D)mG).setComposite(composite);
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (mChartInfo.pointsInCategory[hv][cat] > 0) {
					int r = Math.round(mChartInfo.pieSize[hv][cat]/2);
					int x = Math.round(mChartInfo.pieX[hv][cat]);
					int y = Math.round(mChartInfo.pieY[hv][cat]);
					if (mChartInfo.pointsInCategory[hv][cat] == 1) {
						for (int k=0; k<colorCount; k++) {
							if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mChartInfo.color[k]);
								break;
								}
							}
						g.fillOval(x-r, y-r, 2*r, 2*r);
						}
					else {
						for (int k=0; k<colorCount; k++) {
							if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mChartInfo.color[k]);
								g.fillArc(x-r, y-r, 2*r, 2*r,
										  Math.round(pieColorEdge[hv][cat][k]),
										  Math.round(pieColorEdge[hv][cat][k+1])-Math.round(pieColorEdge[hv][cat][k]));
								}
							}
						}
					g.setColor(getContrastGrey(MARKER_OUTLINE));
					g.drawOval(x-r, y-r, 2*r, 2*r);
					}
				}
			}

		if (original != null)
			((Graphics2D)mG).setComposite(original);

			// calculate coordinates for selection
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int hv = mPoint[i].hvIndex;
				int cat = getChartCategoryIndex(mPoint[i]);
				float angle = (0.5f +(float)mPoint[i].chartGroupIndex)
							 * 2.0f * (float)Math.PI / (float)mChartInfo.pointsInCategory[hv][cat];
				mPoint[i].screenX = Math.round(mChartInfo.pieX[hv][cat]+mChartInfo.pieSize[hv][cat]/2.0f*(float)Math.cos(angle));
				mPoint[i].screenY = Math.round(mChartInfo.pieY[hv][cat]-mChartInfo.pieSize[hv][cat]/2.0f*(float)Math.sin(angle));
				}
			}
		}

	private void paintBoxOrWhiskerPlot(Graphics g, Rectangle baseRect) {
		BoxPlotViewInfo boxPlotInfo = (BoxPlotViewInfo)mChartInfo;

		float cellWidth = (boxPlotInfo.barAxis == 1) ?
				(float)baseRect.width / (float)mCategoryVisibleCount[0]
			  : (float)baseRect.height / (float)mCategoryVisibleCount[1];
		float cellHeight = (boxPlotInfo.barAxis == 1) ?
				(float)baseRect.height
			  : (float)baseRect.width;
		float valueRange = (boxPlotInfo.barAxis == 1) ?
				mAxisVisMax[1]-mAxisVisMin[1]
			  : mAxisVisMax[0]-mAxisVisMin[0];

		mChartInfo.barWidth = Math.min(0.2f * cellHeight, 0.5f * cellWidth / mCaseSeparationCategoryCount);

		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 1;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int axisCatCount = mCategoryVisibleCount[boxPlotInfo.barAxis == 1 ? 0 : 1];
		int catCount = axisCatCount * mCaseSeparationCategoryCount;

		boxPlotInfo.innerDistance = new float[mHVCount][catCount];
		float[][] barPosition = new float[mHVCount][catCount];
		float[][][] barColorEdge = new float[mHVCount][catCount][colorCount+1];
		float[][] boxLAV = new float[mHVCount][catCount];
		float[][] boxUAV = new float[mHVCount][catCount];
		float[][] mean = new float[mHVCount][catCount];
		float[][] median = new float[mHVCount][catCount];
		float csWidth = (mChartInfo.barAxis == 1 ? cellWidth : -cellWidth)
					   * mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (mSplittingColumn[0] != cColumnUnassigned) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i=0; i<axisCatCount; i++) {
				for (int j=0; j<mCaseSeparationCategoryCount; j++) {
					int cat = i*mCaseSeparationCategoryCount + j;
					if (boxPlotInfo.pointsInCategory[hv][cat] != 0) {
						boxPlotInfo.innerDistance[hv][cat] = (boxPlotInfo.boxQ3[hv][cat] - boxPlotInfo.boxQ1[hv][cat])
														  * cellHeight / valueRange / (float)boxPlotInfo.pointsInCategory[hv][cat];
	
						int offset = 0;
						float visMin = 0;
						float factor = 0;
						float innerDistance = boxPlotInfo.innerDistance[hv][cat];
						if (boxPlotInfo.barAxis == 1) {
							barPosition[hv][cat] = baseRect.x + hOffset + i*cellWidth + cellWidth/2;
	
							offset = baseRect.y + vOffset + baseRect.height;
							visMin = mAxisVisMin[1];
							factor =  - (float)baseRect.height / valueRange;
							innerDistance = -innerDistance;
							}
						else {
							barPosition[hv][cat] = baseRect.y + vOffset + baseRect.height - i*cellWidth - cellWidth/2;
	
							offset = baseRect.x + hOffset;
							visMin = mAxisVisMin[0];
							factor =  (float)baseRect.width / valueRange;
							}

						if (mCaseSeparationCategoryCount != 1)
							barPosition[hv][cat] += csOffset + j*csWidth;

						barColorEdge[hv][cat][0] = offset + factor * (boxPlotInfo.boxQ1[hv][cat] - visMin);
	
						for (int k=0; k<colorCount; k++)
							barColorEdge[hv][cat][k+1] = barColorEdge[hv][cat][k] + innerDistance
													   * (float)boxPlotInfo.pointsInColorCategory[hv][cat][k];

	
						boxLAV[hv][cat] = offset + factor * (boxPlotInfo.boxLAV[hv][cat] - visMin);
						boxUAV[hv][cat] = offset + factor * (boxPlotInfo.boxUAV[hv][cat] - visMin);
						mean[hv][cat] = offset + factor * (boxPlotInfo.barValue[hv][cat] - visMin);
						median[hv][cat] = offset + factor * (boxPlotInfo.median[hv][cat] - visMin);
						}
					}
				}
			}

		// draw connection lines
		if (mConnectionColumn == cConnectionColumnConnectCases
		 || mConnectionColumn == mAxisIndex[1-boxPlotInfo.barAxis]) {
			Stroke oldStroke = ((Graphics2D)mG).getStroke();
			((Graphics2D)g).setStroke(new BasicStroke((float)mAbsoluteConnectionLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(mBoxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : getContrastGrey(SCALE_STRONG));
			for (int hv=0; hv<mHVCount; hv++) {
				for (int j=0; j<mCaseSeparationCategoryCount; j++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (mCaseSeparationCategoryCount != 1
					 && mMarkerColor.getColorColumn() == mCaseSeparationColumn) {
						if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(mMarkerColor.getColor(j));
							}
						else {
							for (int k=0; k<colorCount; k++) {
								if (boxPlotInfo.pointsInColorCategory[hv][j][k] != 0) {
									g.setColor(boxPlotInfo.color[k]);
									break;
									}
								}
							}
						}

					for (int i=0; i<axisCatCount; i++) {
						int cat = i*mCaseSeparationCategoryCount + j;
				
						if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
							int value = Math.round(mBoxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(boxPlotInfo.barAxis == 1 ? barPosition[hv][cat] : value);
							int newY = Math.round(boxPlotInfo.barAxis == 1 ? value : barPosition[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
								}
							oldX = newX;
							oldY = newY;
							}
						}
					}
				}
			((Graphics2D)g).setStroke(oldStroke);
			}
		else if (mCaseSeparationCategoryCount != 1 && mConnectionColumn == mCaseSeparationColumn) {
			Stroke oldStroke = ((Graphics2D)mG).getStroke();
			((Graphics2D)g).setStroke(new BasicStroke((float)mAbsoluteConnectionLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(mBoxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : getContrastGrey(SCALE_STRONG));
			for (int hv=0; hv<mHVCount; hv++) {
				for (int i=0; i<axisCatCount; i++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (mMarkerColor.getColorColumn() == mAxisIndex[1-boxPlotInfo.barAxis]) {
						if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(mMarkerColor.getColor(i));
							}
						else {
							for (int k=0; k<colorCount; k++) {
								if (boxPlotInfo.pointsInColorCategory[hv][i*mCaseSeparationCategoryCount][k] != 0) {
									g.setColor(boxPlotInfo.color[k]);
									break;
									}
								}
							}
						}

					for (int j=0; j<mCaseSeparationCategoryCount; j++) {
						int cat = i*mCaseSeparationCategoryCount + j;
				
						if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
							int value = Math.round(mBoxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(boxPlotInfo.barAxis == 1 ? barPosition[hv][cat] : value);
							int newY = Math.round(boxPlotInfo.barAxis == 1 ? value : barPosition[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
								}
							oldX = newX;
							oldY = newY;
							}
						}
					}
				}
			((Graphics2D)g).setStroke(oldStroke);
			}

		Composite original = null;
		Composite composite = null;
		if (mChartType == cChartTypeBoxPlot && mMarkerTransparency != 0.0) {
			original = ((Graphics2D)mG).getComposite();
			composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			}

		float lineLengthAV = mChartInfo.barWidth / 3;

		Stroke oldStroke = ((Graphics2D)mG).getStroke();
		Stroke lineStroke = null;
		Stroke dashedStroke = null;
		if (mChartInfo.barWidth >= 16) {
			float lineWidth = Math.min(6, (float)mChartInfo.barWidth/12);
			lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			dashedStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
										   lineWidth, new float[] {3*lineWidth}, 0f);
			}
		else {
			lineStroke = oldStroke;
			dashedStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
					   					   1.0f, new float[] {(float)mChartInfo.barWidth/5}, 0f);
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
					if (mChartType == cChartTypeBoxPlot) {
						if (composite != null)
							((Graphics2D)mG).setComposite(composite);
	
						for (int k=0; k<colorCount; k++) {
							if (boxPlotInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(boxPlotInfo.color[k]);
								if (boxPlotInfo.barAxis == 1)
									g.fillRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
				  							   Math.round(barColorEdge[hv][cat][k+1]),
											   Math.round(mChartInfo.barWidth),
											   Math.round(barColorEdge[hv][cat][k])-Math.round(barColorEdge[hv][cat][k+1]));
								else
									g.fillRect(Math.round(barColorEdge[hv][cat][k]),
				  							   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
											   Math.round(barColorEdge[hv][cat][k+1]-Math.round(barColorEdge[hv][cat][k])),
											   Math.round(mChartInfo.barWidth));
								}
							}
						if (original != null)
							((Graphics2D)mG).setComposite(original);
						}

					// If we show no markers in a whisker plot, and if every whisker belongs to one category
					// of that column that is assigned for marker coloring, then we draw the whisker itself
					// in the color assigned to that category.
					if (mChartType == cChartTypeWhiskerPlot
				   	 && mRelativeMarkerSize == 0.0) {
						if (mMarkerColor.getColorColumn() == mAxisIndex[boxPlotInfo.barAxis == 1 ? 0 : 1])
							g.setColor(mMarkerColor.getColor(cat / mCaseSeparationCategoryCount));
						else if (mCaseSeparationCategoryCount != 1
							  && mMarkerColor.getColorColumn() == mCaseSeparationColumn)
							g.setColor(mMarkerColor.getColor(cat % mCaseSeparationCategoryCount));
						else
							g.setColor(getContrastGrey(SCALE_STRONG));
						}
					else {
						g.setColor(getContrastGrey(SCALE_STRONG));
						}

					if (boxPlotInfo.barAxis == 1) {
						((Graphics2D)mG).setStroke(lineStroke);
						if (mChartType == cChartTypeBoxPlot) {
							g.drawRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
	  								   Math.round(barColorEdge[hv][cat][colorCount]),
									   Math.round(mChartInfo.barWidth),
									   Math.round(barColorEdge[hv][cat][0])-Math.round(barColorEdge[hv][cat][colorCount]));
							}
						g.drawLine(Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxLAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV),
								   Math.round(boxLAV[hv][cat]));
						g.drawLine(Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxUAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV),
								   Math.round(boxUAV[hv][cat]));

						((Graphics2D)mG).setStroke(dashedStroke);
						if (mChartType == cChartTypeWhiskerPlot) {
							g.drawLine(Math.round(barPosition[hv][cat]),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(boxLAV[hv][cat]));
							}
						else {
							g.drawLine(Math.round(barPosition[hv][cat]),
									   Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][0]));
							g.drawLine(Math.round(barPosition[hv][cat]),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][colorCount]));
							}
						}
					else {
						((Graphics2D)mG).setStroke(lineStroke);
						if (mChartType == cChartTypeBoxPlot) {
							g.drawRect(Math.round(barColorEdge[hv][cat][0]),
	  								   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
									   Math.round(barColorEdge[hv][cat][colorCount])-Math.round(barColorEdge[hv][cat][0]),
									   Math.round(mChartInfo.barWidth));
							}
						((Graphics2D)mG).setStroke(lineStroke);
						g.drawLine(Math.round(boxLAV[hv][cat]),
								   Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxLAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV));
						g.drawLine(Math.round(boxUAV[hv][cat]),
								   Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxUAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV));

						((Graphics2D)mG).setStroke(dashedStroke);
						if (mChartType == cChartTypeWhiskerPlot) {
							g.drawLine(Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]));
							}
						else {
							((Graphics2D)mG).setStroke(dashedStroke);
							g.drawLine(Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][0]),
									   Math.round(barPosition[hv][cat]));
							g.drawLine(Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][colorCount]),
									   Math.round(barPosition[hv][cat]));
							}
						}

					((Graphics2D)mG).setStroke(lineStroke);
					drawBoxMeanIndicators(g, median[hv][cat], mean[hv][cat], barPosition[hv][cat]);
					}
				}
			}

		if (mBoxplotShowMeanAndMedianValues
		 || boxPlotInfo.foldChange != null
		 || boxPlotInfo.pValue != null) {
			String[] lineText = new String[4];
			g.setColor(getContrastGrey(SCALE_STRONG));
			int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
			boolean isLogarithmic = mTableModel.isLogarithmicViewMode(mAxisIndex[boxPlotInfo.barAxis]);
			for (int hv=0; hv<mHVCount; hv++) {
				int hOffset = 0;
				int vOffset = 0;
				if (mSplittingColumn[0] != cColumnUnassigned) {
					hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					}
				for (int cat=0; cat<catCount; cat++) {
					if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
						int lineCount = 0;
						if (mBoxplotShowMeanAndMedianValues) {
							float meanValue = isLogarithmic ? (float)Math.pow(10, boxPlotInfo.barValue[hv][cat]) : boxPlotInfo.barValue[hv][cat];
							float medianValue = isLogarithmic ? (float)Math.pow(10, boxPlotInfo.median[hv][cat]) : boxPlotInfo.median[hv][cat];
							switch (mBoxplotMeanMode) {
							case cBoxplotMeanModeMedian:
								lineText[lineCount++] = "median="+DoubleFormat.toString(medianValue);
								break;
							case cBoxplotMeanModeMean:
								lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue);
								break;
							case cBoxplotMeanModeLines:
							case cBoxplotMeanModeTriangles:
								lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue);
								lineText[lineCount++] = "median="+DoubleFormat.toString(medianValue);
								break;
								}
							}
						if (boxPlotInfo.foldChange != null && !Float.isNaN(boxPlotInfo.foldChange[hv][cat])) {
							String label = mTableModel.isLogarithmicViewMode(mAxisIndex[boxPlotInfo.barAxis]) ? "l2fc=" : "fc=";
							lineText[lineCount++] = label+new DecimalFormat("#.###").format(boxPlotInfo.foldChange[hv][cat]);
							}
						if (boxPlotInfo.pValue != null && !Float.isNaN(boxPlotInfo.pValue[hv][cat])) {
							lineText[lineCount++] = "p="+new DecimalFormat("#.####").format(boxPlotInfo.pValue[hv][cat]);
							}
	
	/*					if (boxPlotInfo.barAxis == 1) {	this was the single lined stuff with p-value only
							int x = Math.round(barPosition[hv][cat] - textWidth/2);
							int y = Math.round(Math.min(boxLAV[hv][cat]+scaledFontHeight*3/2, baseRect.y+baseRect.height+vOffset-scaledFontHeight/2));
							}
						else {
							int x = Math.round(Math.min(boxUAV[hv][cat]+scaledFontHeight/2, baseRect.x+baseRect.width+hOffset-textWidth));
							int y = Math.round(barPosition[hv][cat]+mG.getFontMetrics().getAscent()/2);
							}*/
	
						for (int line=0; line<lineCount; line++) {
							int textWidth = mG.getFontMetrics().stringWidth(lineText[line]);
							float x,y;
							if (boxPlotInfo.barAxis == 1) {
								x = barPosition[hv][cat] - textWidth/2;
								y = Math.min(boxLAV[hv][cat]+scaledFontHeight*3/2, baseRect.y+baseRect.height+vOffset+scaledFontHeight/2-lineCount*scaledFontHeight)+line*scaledFontHeight;
								}
							else {
								x = Math.min(boxUAV[hv][cat]+scaledFontHeight/2, baseRect.x+baseRect.width+hOffset-textWidth);
								y = barPosition[hv][cat]+mG.getFontMetrics().getAscent()/2-((lineCount-1)*scaledFontHeight)/2+line*scaledFontHeight;
								}
							mG.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
					}
				}
			}

		((Graphics2D)mG).setStroke(oldStroke);

		if (original != null)
			((Graphics2D)mG).setComposite(original);

		// in case of box-plot calculate screen positions of all non-outliers
		if (mChartType != cChartTypeWhiskerPlot) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int chartGroupIndex = mPoint[i].chartGroupIndex;
					if (chartGroupIndex != -1) {
						int hv = mPoint[i].hvIndex;
						int cat = getChartCategoryIndex(mPoint[i]);
						if (boxPlotInfo.barAxis == 1) {
							mPoint[i].screenX = Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2)+Math.round(mChartInfo.barWidth/2);
							mPoint[i].screenY = Math.round(barColorEdge[hv][cat][0]-boxPlotInfo.innerDistance[hv][cat]*(1+chartGroupIndex))
											  + Math.round(boxPlotInfo.innerDistance[hv][cat]/2);
							mPoint[i].width = Math.round(boxPlotInfo.barWidth);
							mPoint[i].height = Math.round(boxPlotInfo.innerDistance[hv][cat]);
							}
						else {
							mPoint[i].screenX = Math.round(barColorEdge[hv][cat][0]+boxPlotInfo.innerDistance[hv][cat]*chartGroupIndex)
											  + Math.round(boxPlotInfo.innerDistance[hv][cat]/2);
							mPoint[i].screenY = Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2)+Math.round(mChartInfo.barWidth/2);
							mPoint[i].width = Math.round(boxPlotInfo.innerDistance[hv][cat]);
							mPoint[i].height = Math.round(boxPlotInfo.barWidth);
							}
						}
					}
				}
			}
		}

	private void drawBoxMeanIndicators(Graphics g, float median, float mean, float bar) {
		float lineWidth = Math.max(2, (float)mChartInfo.barWidth/10);
		switch (mBoxplotMeanMode) {
		case cBoxplotMeanModeMedian:
			drawIndicatorLine(g, median, bar, lineWidth, getContrastGrey(SCALE_STRONG));
			break;
		case cBoxplotMeanModeMean:
			drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
			break;
		case cBoxplotMeanModeLines:
			drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
			drawIndicatorLine(g, median, bar, lineWidth, getContrastGrey(SCALE_STRONG));
			break;
		case cBoxplotMeanModeTriangles:
			float width = mChartInfo.barWidth / 4;
			float space = width / 3;
			float tip = space + 1.5f * width;

			if (mChartInfo.barAxis == 1) {
				drawIndicatorTriangle(g, bar+tip, median, bar+space, median-width, bar+space, median+width, Color.BLACK);
				drawIndicatorTriangle(g, bar-tip, mean, bar-space, mean-width, bar-space, mean+width, Color.RED);
				}
			else {
				drawIndicatorTriangle(g, median, bar+tip, median-width, bar+space, median+width, bar+space, Color.BLACK);
				drawIndicatorTriangle(g, mean, bar-tip, mean-width, bar-space, mean+width, bar-space, Color.RED);
				}
			break;
			}
		}

	private void drawIndicatorLine(Graphics g, float position, float bar, float lineWidth, Color color) {
		g.setColor(color);
		if (mChartInfo.barAxis == 1) {
			g.fillRect(Math.round(bar-mChartInfo.barWidth/2),
					   Math.round(position-lineWidth/2),
					   Math.round(mChartInfo.barWidth),
					   Math.round(lineWidth));
			}
		else {
			g.fillRect(Math.round(position-lineWidth/2),
					   Math.round(bar-mChartInfo.barWidth/2),
					   Math.round(lineWidth),
					   Math.round(mChartInfo.barWidth));
			}
		}

	private void drawIndicatorTriangle(Graphics g, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
		GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);

		polygon.moveTo(Math.round(x1), Math.round(y1));
		polygon.lineTo(Math.round(x2), Math.round(y2));
		polygon.lineTo(Math.round(x3), Math.round(y3));
		polygon.closePath();

		g.setColor(color);
		((Graphics2D)g).fill(polygon);
		g.setColor(getContrastGrey(SCALE_STRONG));
		((Graphics2D)g).draw(polygon);
		}

	private void markHighlighted(Graphics g) {
		if (isVisible(mHighlightedPoint)) {
			g.setColor(getContrastGrey(SCALE_STRONG));
			markMarker(g, (VisualizationPoint2D)mHighlightedPoint, false);
			}
		}

	private void markReference(Graphics g) {
		g.setColor(Color.red);
		markMarker(g, (VisualizationPoint2D)mActivePoint, true);
		}

	private void drawCurves(Rectangle baseGraphRect) {
		mG.setColor(getContrastGrey(SCALE_STRONG));
		switch (mCurveInfo & cCurveModeMask) {
		case cCurveModeAbscissa:
			drawVerticalMeanLine(baseGraphRect);
			break;
		case cCurveModeOrdinate:
			drawHorizontalMeanLine(baseGraphRect);
			break;
		case cCurveModeBothAxes:
			drawFittedMeanLine(baseGraphRect);
			break;
			}
		}

	@Override
	public String getStatisticalValues() {
		if (mChartType != cChartTypeScatterPlot)
			return super.getStatisticalValues();

		StringWriter stringWriter = new StringWriter(1024);
		BufferedWriter writer = new BufferedWriter(stringWriter);

		try {
			if ((mCurveInfo & cCurveModeMask) == cCurveModeAbscissa)
				getMeanLineStatistics(writer, 0);
			if ((mCurveInfo & cCurveModeMask) == cCurveModeOrdinate)
				getMeanLineStatistics(writer, 1);
			if ((mCurveInfo & cCurveModeMask) == cCurveModeBothAxes)
				getFittedLineStatistics(writer);

			if (mShownCorrelationType != CorrelationCalculator.TYPE_NONE && mCorrelationCoefficient != null) {
				writer.write("Correlation coefficient"+ " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+"):");
				if (mCorrelationCoefficient.length == 1) {
					writer.write(DoubleFormat.toString(mCorrelationCoefficient[0], 3));
					writer.newLine();
					}
				else {
					String[] splittingCategory0 = (mSplittingColumn[0] == cColumnUnassigned) ? null : mTableModel.getCategoryList(mSplittingColumn[0]);
					String[] splittingCategory1 = (mSplittingColumn[1] == cColumnUnassigned) ? null : mTableModel.getCategoryList(mSplittingColumn[1]);
					if (mSplittingColumn[0] != cColumnUnassigned)
						writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
					if (mSplittingColumn[1] != cColumnUnassigned)
						writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
					writer.write("r");
					for (int hv=0; hv<mHVCount; hv++) {
						if (mSplittingColumn[0] != cColumnUnassigned)
							writer.write(splittingCategory0[(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
						if (mSplittingColumn[1] != cColumnUnassigned)
							writer.write(splittingCategory1[mSplitter.getVIndex(hv)]+"\t");
						writer.write(DoubleFormat.toString(mCorrelationCoefficient[hv], 3));
						writer.newLine();
						}
					}
				}

			writer.close();
			}
		catch (IOException ioe) {}

		return stringWriter.toString();
		}

	private void getMeanLineStatistics(BufferedWriter writer, int axis) throws IOException {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				xmean[mPoint[i].hvIndex][cat] += getValue(mPoint[i].record, axis);
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					xmean[hv][cat] /= count[hv][cat];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (getValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat])
											   		* (getValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		String[] splittingCategory0 = (mSplittingColumn[0] == cColumnUnassigned) ? null : mTableModel.getCategoryList(mSplittingColumn[0]);
		String[] splittingCategory1 = (mSplittingColumn[1] == cColumnUnassigned) ? null : mTableModel.getCategoryList(mSplittingColumn[1]);
		String[] colorCategory = (catCount == 1) ? null : mTableModel.getCategoryList(mMarkerColor.getColorColumn());

		writer.write((axis == 0) ? "Vertical Mean Line:" : "Horizontal Mean Line:");
		writer.newLine();
		if (mSplittingColumn[0] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (catCount != 1)
			writer.write(mTableModel.getColumnTitle(mMarkerColor.getColorColumn())+"\t");
		writer.write("Value Count\tMean Value");
		if ((mCurveInfo & cCurveStandardDeviation) != 0)
			writer.write("\tStandard Deviation");
		writer.newLine();

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (mSplittingColumn[0] != cColumnUnassigned)
					writer.write(splittingCategory0[(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
				if (mSplittingColumn[1] != cColumnUnassigned)
					writer.write(splittingCategory1[mSplitter.getVIndex(hv)]+"\t");
				if (catCount != 1)
					writer.write(colorCategory[cat]+"\t");
				writer.write(count[hv][cat]+"\t");
				writer.write(count[hv][cat] == 0 ? "" : formatValue(xmean[hv][cat], mAxisIndex[axis]));
				if ((mCurveInfo & cCurveStandardDeviation) != 0) {
					stdDev[hv][cat] /= (count[hv][cat]-1);
					stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
					writer.write("\t"+formatValue(stdDev[hv][cat], mAxisIndex[axis]));
					}
				writer.newLine();
				}
			}
		writer.newLine();
		}

	private void getFittedLineStatistics(BufferedWriter writer) throws IOException {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				sx[mPoint[i].hvIndex][cat] += getValue(mPoint[i].record, 0);
				sy[mPoint[i].hvIndex][cat] += getValue(mPoint[i].record, 1);
				sx2[mPoint[i].hvIndex][cat] += getValue(mPoint[i].record, 0) * getValue(mPoint[i].record, 0);
				sxy[mPoint[i].hvIndex][cat] += getValue(mPoint[i].record, 0) * getValue(mPoint[i].record, 1);
				count[mPoint[i].hvIndex][cat]++;
				}
			}
		float[][] m = null;
		float[][] b = null;
		m = new float[mHVCount][catCount];
		b = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				m[hv][cat] = (count[hv][cat]*sxy[hv][cat]-sx[hv][cat]*sy[hv][cat])/(count[hv][cat]*sx2[hv][cat]-sx[hv][cat]*sx[hv][cat]);
				b[hv][cat] = sy[hv][cat]/count[hv][cat]-m[hv][cat]*sx[hv][cat]/count[hv][cat];
				}
			}

		float[][] stdDev = null;
		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			stdDev = new float[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					float b2 = getValue(mPoint[i].record, 1) + getValue(mPoint[i].record, 0)/m[mPoint[i].hvIndex][cat];
					float xs = (b2-b[mPoint[i].hvIndex][cat])/(m[mPoint[i].hvIndex][cat]+1.0f/m[mPoint[i].hvIndex][cat]);
					float ys = -xs/m[mPoint[i].hvIndex][cat] + b2;
					stdDev[mPoint[i].hvIndex][cat] += (getValue(mPoint[i].record, 0)-xs)*(getValue(mPoint[i].record, 0)-xs);
					stdDev[mPoint[i].hvIndex][cat] += (getValue(mPoint[i].record, 1)-ys)*(getValue(mPoint[i].record, 1)-ys);
					}
				}
			}

		String[] splittingCategory0 = (mSplittingColumn[0] == cColumnUnassigned) ? null : mTableModel.getCategoryList(mSplittingColumn[0]);
		String[] splittingCategory1 = (mSplittingColumn[1] == cColumnUnassigned) ? null : mTableModel.getCategoryList(mSplittingColumn[1]);
		String[] colorCategory = (catCount == 1) ? null : mTableModel.getCategoryList(mMarkerColor.getColorColumn());

		writer.write("Fitted Straight Line:");
		writer.newLine();
		if (mTableModel.isLogarithmicViewMode(mAxisIndex[0]) || mTableModel.isLogarithmicViewMode(mAxisIndex[1])) {
			writer.write("Gradient m and standard deviation are based on logarithmic values.");
			writer.newLine();
			}
		if (mSplittingColumn[0] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (catCount != 1)
			writer.write(mTableModel.getColumnTitle(mMarkerColor.getColorColumn())+"\t");
		writer.write("Value Count\tGradient m\tIntercept b");
		if ((mCurveInfo & cCurveStandardDeviation) != 0)
			writer.write("\tStandard Deviation");
		writer.newLine();

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (mSplittingColumn[0] != cColumnUnassigned)
					writer.write(splittingCategory0[(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
				if (mSplittingColumn[1] != cColumnUnassigned)
					writer.write(splittingCategory1[mSplitter.getVIndex(hv)]+"\t");
				if (catCount != 1)
					writer.write(colorCategory[cat]+"\t");
				writer.write(count[hv][cat]+"\t");
				if (count[hv][cat] < 2) {
					writer.write("\t");
					if ((mCurveInfo & cCurveStandardDeviation) != 0)
						writer.write("\t");
					}
				else {
					if (count[hv][cat]*sx2[hv][cat] == sx[hv][cat]*sx[hv][cat])
						writer.write("Infinity\t-Infinity");
					else if (count[hv][cat]*sxy[hv][cat] == sx[hv][cat]*sy[hv][cat])
						writer.write("0.0\t"+formatValue(sy[hv][cat] / count[hv][cat], mAxisIndex[1]));
					else
						writer.write(DoubleFormat.toString(m[hv][cat])+"\t"+formatValue(b[hv][cat], mAxisIndex[1]));
					if ((mCurveInfo & cCurveStandardDeviation) != 0) {
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
						writer.write("\t"+DoubleFormat.toString(stdDev[hv][cat]));
						}
					}
				writer.newLine();
				}
			}
		writer.newLine();
		}

	private void drawVerticalMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				xmean[mPoint[i].hvIndex][cat] += mPoint[i].screenX;
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					xmean[hv][cat] /= count[hv][cat];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat])
											   		* (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int hOffset = 0;
					int vOffset = 0;
					if (mSplittingColumn[0] != cColumnUnassigned) {
						hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
						vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
						}
					int ymin = baseGraphRect.y + vOffset;
					int ymax = ymin + baseGraphRect.height;
	
					if (catCount != 1)
						mG.setColor(mMarkerColor.getColor(cat));
					drawVerticalLine(xmean[hv][cat], ymin, ymax, false);
	
					if ((mCurveInfo & cCurveStandardDeviation) != 0) {
						int xmin = baseGraphRect.x + hOffset;
						int xmax = xmin + baseGraphRect.width;
	
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
	
		   				if (xmean[hv][cat]-stdDev[hv][cat] > xmin)
							drawVerticalLine(xmean[hv][cat]-stdDev[hv][cat], ymin, ymax, true);
						if (xmean[hv][cat]+stdDev[hv][cat] < xmax)
							drawVerticalLine(xmean[hv][cat]+stdDev[hv][cat], ymin, ymax, true);
						}
					}
				}
			}
		}

	private void drawVerticalLine(float x, float ymin, float ymax, boolean isStdDevLine) {
		if (mIsHighResolution) {
			float stroke = isStdDevLine? 2 : 3;
			((Graphics2D)mG).setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			((Graphics2D)mG).draw(new Line2D.Float(x, ymin, x, ymax));
			}
		else {
			if (isStdDevLine) {
				mG.drawLine((int)(x-0.5), (int)ymin, (int)(x-0.5), (int)ymax);
				mG.drawLine((int)(x+0.5), (int)ymin, (int)(x+0.5), (int)ymax);
				}
			else {
				mG.drawLine((int)(x-1.0), (int)ymin, (int)(x-1.0), (int)ymax);
				mG.drawLine((int)x, (int)ymin, (int)x, (int)ymax);
				mG.drawLine((int)(x+1.0), (int)ymin, (int)(x+1.0), (int)ymax);
				}
			}
		}

	private void drawHorizontalMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] ymean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				ymean[mPoint[i].hvIndex][cat] += mPoint[i].screenY;
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					ymean[hv][cat] /= count[hv][cat];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat])
													* (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat]);
					}
				}
			}
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int hOffset = 0;
					int vOffset = 0;
					if (mSplittingColumn[0] != cColumnUnassigned) {
						hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
						vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
						}
					int xmin = baseGraphRect.x + hOffset;
					int xmax = xmin + baseGraphRect.width;
	
					if (catCount != 1)
						mG.setColor(mMarkerColor.getColor(cat));
					drawHorizontalLine(xmin, xmax, ymean[hv][cat], false);
	
					if ((mCurveInfo & cCurveStandardDeviation) != 0) {
						int ymin = baseGraphRect.y + vOffset;
						int ymax = ymin + baseGraphRect.height;
	
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
	
						if (ymean[hv][cat]-stdDev[hv][cat] > ymin)
							drawHorizontalLine(xmin, xmax, ymean[hv][cat]-stdDev[hv][cat], true);
						if (ymean[hv][cat]+stdDev[hv][cat] < ymax)
							drawHorizontalLine(xmin, xmax, ymean[hv][cat]+stdDev[hv][cat], true);
						}
					}
				}
			}
		}

	private void drawHorizontalLine(float xmin, float xmax, float y, boolean isStdDevLine) {
		if (mIsHighResolution) {
			float stroke = isStdDevLine? 2 : 3;
			((Graphics2D)mG).setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			((Graphics2D)mG).draw(new Line2D.Float(xmin, y, xmax, y));
			}
		else {
			if (isStdDevLine) {
				mG.drawLine((int)xmin, (int)(y-0.5), (int)xmax, (int)(y-0.5));
				mG.drawLine((int)xmin, (int)(y+0.5), (int)xmax, (int)(y+0.5));
				}
			else {
				mG.drawLine((int)xmin, (int)(y-1.0), (int)xmax, (int)(y-1.0));
				mG.drawLine((int)xmin, (int)y, (int)xmax, (int)y);
				mG.drawLine((int)xmin, (int)(y+1.0), (int)xmax, (int)(y+1.0));
				}
			}
		}

	private void drawFittedMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				sx[mPoint[i].hvIndex][cat] += mPoint[i].screenX;
				sy[mPoint[i].hvIndex][cat] += mPoint[i].screenY;
				sx2[mPoint[i].hvIndex][cat] += mPoint[i].screenX * mPoint[i].screenX;
				sxy[mPoint[i].hvIndex][cat] += mPoint[i].screenX * mPoint[i].screenY;
				count[mPoint[i].hvIndex][cat]++;
				}
			}
		float[][] m = null;
		float[][] b = null;
		m = new float[mHVCount][catCount];
		b = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				m[hv][cat] = (count[hv][cat]*sxy[hv][cat]-sx[hv][cat]*sy[hv][cat])/(count[hv][cat]*sx2[hv][cat]-sx[hv][cat]*sx[hv][cat]);
				b[hv][cat] = sy[hv][cat]/count[hv][cat]-m[hv][cat]*sx[hv][cat]/count[hv][cat];
				}
			}

		float[][] stdDev = null;
		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			stdDev = new float[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					float b2 = mPoint[i].screenY + mPoint[i].screenX/m[mPoint[i].hvIndex][cat];
					float xs = (b2-b[mPoint[i].hvIndex][cat])/(m[mPoint[i].hvIndex][cat]+1.0f/m[mPoint[i].hvIndex][cat]);
					float ys = -xs/m[mPoint[i].hvIndex][cat] + b2;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenX-xs)*(mPoint[i].screenX-xs);
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenY-ys)*(mPoint[i].screenY-ys);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] < 2) {
					continue;
					}
				if (count[hv][cat]*sx2[hv][cat] == sx[hv][cat]*sx[hv][cat]) {
					float x = sx[hv][cat] / count[hv][cat];
					float ymin = baseGraphRect.y;
					if (mSplittingColumn[0] != cColumnUnassigned)
						ymin += mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					drawVerticalLine(x, ymin, ymin+baseGraphRect.height, false);
					continue;
					}
				if (count[hv][cat]*sxy[hv][cat] == sx[hv][cat]*sy[hv][cat]) {
					float y = sy[hv][cat] / count[hv][cat];
					float xmin = baseGraphRect.x;
					if (mSplittingColumn[0] != cColumnUnassigned)
						xmin += mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					drawHorizontalLine(xmin, xmin+baseGraphRect.width, y, false);
					continue;
					}

				if (catCount != 1)
					mG.setColor(mMarkerColor.getColor(cat));
				drawInclinedLine(baseGraphRect, hv, m[hv][cat], b[hv][cat], false);
		
				if ((mCurveInfo & cCurveStandardDeviation) != 0) {
					stdDev[hv][cat] /= (count[hv][cat]-1);
					stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
					float db = (float)Math.sqrt(stdDev[hv][cat]*stdDev[hv][cat]*(1+m[hv][cat]*m[hv][cat]));
					drawInclinedLine(baseGraphRect, hv, m[hv][cat], b[hv][cat]+db, true);
					drawInclinedLine(baseGraphRect, hv, m[hv][cat], b[hv][cat]-db, true);
					}
				}
			}
		}

	private void drawInclinedLine(Rectangle baseGraphRect, int hv, float m, float b, boolean isStdDevLine) {
		int hOffset = 0;
		int vOffset = 0;
		if (mSplittingColumn[0] != cColumnUnassigned) {
			hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
			vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
			}

		int xmin = baseGraphRect.x+hOffset;
		int xmax = xmin+baseGraphRect.width;
		int ymin = baseGraphRect.y+vOffset;
		int ymax = ymin+baseGraphRect.height;

		float sxtop = (ymin-b)/m;
		float sxbottom = (ymax-b)/m;
		float syleft = m*xmin+b;
		float syright = m*(xmax)+b;
		float[] x = new float[2];
		float[] y = new float[2];
		if (syleft >= ymin && syleft <= ymax) {
			x[0] = xmin;
			y[0] = syleft;
			}
		else if (m < 0) {
			if (sxbottom < xmin || sxbottom > xmax)
				return;
			x[0] = sxbottom;
			y[0] = ymax;
			}
		else {
			if (sxtop < xmin || sxtop > xmax)
				return;
			x[0] = sxtop;
			y[0] = ymin;
			}
		if (syright >= ymin && syright <= ymax) {
			x[1] = xmax;
			y[1] = syright;
			}
		else if (m < 0) {
			if (sxtop < xmin || sxtop > xmax)
				return;
			x[1] = sxtop;
			y[1] = ymin;
			}
		else {
			if (sxbottom < xmin || sxbottom > xmax)
				return;
			x[1] = sxbottom;
			y[1] = ymax;
			}
		float stroke = isStdDevLine? 2 : 3;
		((Graphics2D)mG).setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		((Graphics2D)mG).draw(new Line2D.Float(x[0], y[0], x[1], y[1]));
		((Graphics2D)mG).setStroke(new BasicStroke((float)1.0, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		}

	private void drawCorrelationCoefficient(Rectangle baseGraphRect) {
		if (mAxisIndex[0] == cColumnUnassigned || mAxisIndex[1] == cColumnUnassigned)
			return;

		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		setFontHeight(scaledFontHeight);
		mG.setColor(getContrastGrey(SCALE_STRONG));

		mCorrelationCoefficient = new float[mHVCount];
		if (mHVCount == 1) {
			float r = (float)CorrelationCalculator.calculateCorrelation(
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isVisibleExcludeNaN(mPoint[row]) ? getValue(mPoint[row].record, 0) : Float.NaN;
							}
						},
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isVisibleExcludeNaN(mPoint[row]) ? getValue(mPoint[row].record, 1) : Float.NaN;
							}
						},
					mShownCorrelationType);
			String s = "r="+DoubleFormat.toString(r, 3)
					 + " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+")";
			mG.drawString(s, baseGraphRect.x+baseGraphRect.width-mG.getFontMetrics().stringWidth(s),
							 baseGraphRect.y+baseGraphRect.height-scaledFontHeight/2);
			mCorrelationCoefficient[0] = r;
			}
		else {
			int[] count = new int[mHVCount];
			for (int i=0; i<mDataPoints; i++)
				if (isVisibleExcludeNaN(mPoint[i]))
					count[mPoint[i].hvIndex]++;
			float[][][] value = new float[mHVCount][2][];
			for (int hv=0; hv<mHVCount; hv++) {
				value[hv][0] = new float[count[hv]];
				value[hv][1] = new float[count[hv]];
				}
			count = new int[mHVCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					value[mPoint[i].hvIndex][0][count[mPoint[i].hvIndex]] = getValue(mPoint[i].record, 0);
					value[mPoint[i].hvIndex][1][count[mPoint[i].hvIndex]] = getValue(mPoint[i].record, 1);
					count[mPoint[i].hvIndex]++;
					}
				}

			for (int hv=0; hv<mHVCount; hv++) {
				if (count[hv] >= 2) {
					final float[][] _value = value[hv];
					float r = (float)CorrelationCalculator.calculateCorrelation(
							new INumericalDataColumn() {
								public int getValueCount() {
									return _value[0].length;
									}
								public double getValueAt(int row) {
									return _value[0][row];
									}
								},
							new INumericalDataColumn() {
								public int getValueCount() {
									return _value[1].length;
									}
								public double getValueAt(int row) {
									return _value[1][row];
									}
								},
							mShownCorrelationType);

					int hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					int vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					String s = "r="+DoubleFormat.toString(r, 3)
							 + " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+")";
					mG.drawString(s, hOffset+baseGraphRect.x+baseGraphRect.width-mG.getFontMetrics().stringWidth(s),
									 vOffset+baseGraphRect.y+baseGraphRect.height-scaledFontHeight/2);
					mCorrelationCoefficient[hv] = r;
					}
				}
			}
		}

	@Override
	protected void setActivePoint(VisualizationPoint newReference) {
		super.setActivePoint(newReference);

		if (mBackgroundColor.getColorColumn() != cColumnUnassigned) {
			if (mTableModel.isDescriptorColumn(mBackgroundColor.getColorColumn())) {
				setBackgroundSimilarityColors();
				mBackgroundValid = false;
				mOffImageValid = false;
				}
			}
		}

	private void markMarker(Graphics g, VisualizationPoint2D marker, boolean boldLine) {
		int sizeX,sizeY,hSizeX,hSizeY;
		if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != -1
		 && (!mLabelsInTreeViewOnly || isTreeViewGraph())) {
			hSizeX = Math.round(marker.width/2) + 2;
			hSizeY = Math.round(marker.height/2) + 2;
			sizeX = Math.round(marker.width) + 4;
			sizeY = Math.round(marker.height) + 4;
			g.drawRect(marker.screenX-hSizeX-2, marker.screenY-hSizeY-2, sizeX+4, sizeY+4);
			if (boldLine)
				g.drawRect(marker.screenX-hSizeX-3, marker.screenY-hSizeY-3, sizeX+6, sizeY+6);
			}
		else if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && marker.chartGroupIndex != -1)) {
			int hv = marker.hvIndex;
			int cat = getChartCategoryIndex(marker);
			if (cat != -1) {
				if (mChartInfo.barAxis == 1) {
					hSizeX = Math.round(mChartInfo.barWidth/2) + 2;
					hSizeY = Math.round(mChartInfo.innerDistance[hv][cat]/2) + 2;
					sizeX = Math.round(mChartInfo.barWidth) + 4;
					sizeY = Math.round(mChartInfo.innerDistance[hv][cat]) + 4;
					}
				else {
					hSizeX = Math.round(mChartInfo.innerDistance[hv][cat]/2) + 2;
					hSizeY = Math.round(mChartInfo.barWidth/2) + 2;
					sizeX = Math.round(mChartInfo.innerDistance[hv][cat]) + 4;
					sizeY = Math.round(mChartInfo.barWidth) + 4;
					}
	
				g.drawRect(marker.screenX-hSizeX,
						   marker.screenY-hSizeY, sizeX, sizeY);
				if (boldLine)
					g.drawRect(marker.screenX-hSizeX-1,
							   marker.screenY-hSizeY-1, sizeX+2, sizeY+2);
				}
			}
		else if (mChartType == cChartTypePies) {
			if (!mChartInfo.barOrPieDataAvailable)
				return;

			int hv = marker.hvIndex;
			int cat = getChartCategoryIndex(marker);
			if (cat != -1) {
				int x = Math.round(mChartInfo.pieX[hv][cat]);
				int y = Math.round(mChartInfo.pieY[hv][cat]);
				int r = Math.round(mChartInfo.pieSize[hv][cat]/2);
				float dif = 2.0f * (float)Math.PI / (float)mChartInfo.pointsInCategory[hv][cat];
				float angle = dif * marker.chartGroupIndex;
				int intDif = Math.round(180.0f * dif / (float)Math.PI);
				int intAngle = Math.round(180.0f * angle / (float)Math.PI);
				Stroke oldStroke = null;
				if (boldLine) {
					oldStroke = ((Graphics2D)g).getStroke();
					((Graphics2D)g).setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					}
				if (mChartInfo.pointsInCategory[hv][cat] == 1) {
					g.drawOval(x-r-2, y-r-2, 2*r+4, 2*r+4);
					}
				else {
					g.drawArc(x-r-2, y-r-2, 2*r+4, 2*r+4, intAngle, intDif);
					g.drawLine(x, y, x+(int)(Math.cos(angle)*((float)r+2)), y-(int)(Math.sin(angle)*((float)r+2)));
					g.drawLine(x, y, x+(int)(Math.cos(angle+dif)*((float)r+2)), y-(int)(Math.sin(angle+dif)*((float)r+2)));
					}
				if (boldLine) {
					((Graphics2D)g).setStroke(oldStroke);
					}
				}
			}
		else if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null) {
			if (mMultiValueMarkerMode == cMultiValueMarkerModeBars) {
				MultiValueBars mvbi = new MultiValueBars();
				mvbi.calculate(marker.width, marker);
				final int z = mMultiValueMarkerColumns.length-1;
				for (int d=2; d<(boldLine?4:3); d++) {
					int x1 = mvbi.firstBarX-d;
					int xn = mvbi.firstBarX+mMultiValueMarkerColumns.length*mvbi.barWidth+d-1;
					g.drawLine(x1, mvbi.barY[0]-d, x1, mvbi.barY[0]+mvbi.barHeight[0]+d-1);
					g.drawLine(xn, mvbi.barY[z]-d, xn, mvbi.barY[z]+mvbi.barHeight[z]+d-1);
					int x2 = x1;
					int y1 = mvbi.barY[0]-d;
					int y2 = mvbi.barY[0]+mvbi.barHeight[0]+d-1;
					for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
						int x3 = mvbi.firstBarX+(i+1)*mvbi.barWidth-d;
						int x4 = x3;
						if (i == z || mvbi.barY[i]<mvbi.barY[i+1])
							x3 += 2*d-1;
						if (i == z || mvbi.barY[i]+mvbi.barHeight[i]>mvbi.barY[i+1]+mvbi.barHeight[i+1])
							x4 += 2*d-1;
						g.drawLine(x1, y1, x3, y1);
						g.drawLine(x2, y2, x4, y2);
						if (i != z) {
							int y3 = mvbi.barY[i+1]-d;
							int y4 = mvbi.barY[i+1]+mvbi.barHeight[i+1]+d-1;
							g.drawLine(x3, y1, x3, y3);
							g.drawLine(x4, y2, x4, y4);
							y1 = y3;
							y2 = y4;
							}
						x1 = x3;
						x2 = x4;
						}
					}
				}
			else {
				int x = marker.screenX;
				int y = marker.screenY;
				float size = 0.5f  * marker.width * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
				int[] r = new int[mMultiValueMarkerColumns.length];
				for (int i=0; i<mMultiValueMarkerColumns.length; i++)
					r[i] = (int)(size * getMarkerSizeVPFactor(marker.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]));
				Stroke oldStroke = null;
				if (boldLine) {
					oldStroke = ((Graphics2D)g).getStroke();
					((Graphics2D)g).setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					}
				float angleIncrement = 360f / mMultiValueMarkerColumns.length;
				float angle = 90f - angleIncrement;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					g.drawArc(x-r[i], y-r[i], 2*r[i]-1, 2*r[i]-1, Math.round(angle), Math.round(angleIncrement));
	
					float floatAngle = (float)Math.PI * angle / 180;
					int h = (i == mMultiValueMarkerColumns.length-1) ? 0 : i+1;
					g.drawLine(x+(int)(Math.cos(floatAngle)*((float)r[h]))-1, y-(int)(Math.sin(floatAngle)*((float)r[h]))-1,
							   x+(int)(Math.cos(floatAngle)*((float)r[i]))-1, y-(int)(Math.sin(floatAngle)*((float)r[i]))-1);
	
					angle -= angleIncrement;
					}
				if (boldLine) {
					((Graphics2D)g).setStroke(oldStroke);
					}
				}
			}
		else {
			int size = (int)getMarkerSize(marker);
			int halfSize = size / 2;
			int sx,sy;
			int[] x,y;
			Polygon p;

			switch (marker.shape) {
			case 0:
				g.drawRect(marker.screenX-halfSize-2,
						   marker.screenY-halfSize-2, size+4, size+4);
				if (boldLine)
					g.drawRect(marker.screenX-halfSize-3,
							   marker.screenY-halfSize-3, size+6, size+6);
				break;
			case 1:
				g.drawOval(marker.screenX-halfSize-2,
						   marker.screenY-halfSize-2, size+4, size+4);
				if (boldLine)
					g.drawOval(marker.screenX-halfSize-3,
							   marker.screenY-halfSize-3, size+6, size+6);
				break;
			case 2:
				x = new int[3];
				y = new int[3];
				x[0] = marker.screenX-halfSize-3;
				x[1] = marker.screenX+halfSize+3;
				x[2] = marker.screenX;
				y[0] = y[1] = marker.screenY+size/3+2;
				y[2] = y[0]-size-5;
				p = new Polygon(x, y, 3);
				g.drawPolygon(p);
				if (boldLine) {
					x[0]--;
					x[1]++;
					y[0]++;
					y[1]++;
					y[2]--;
					p = new Polygon(x, y, 3);
					g.drawPolygon(p);
					}
				break;
			case 3:
				x = new int[4];
				y = new int[4];
				x[0] = marker.screenX-halfSize-3;
				x[1] = x[3] = marker.screenX;
				x[2] = marker.screenX+halfSize+3;
				y[0] = y[2] = marker.screenY;
				y[1] = marker.screenY+halfSize+3;
				y[3] = marker.screenY-halfSize-3;
				p = new Polygon(x, y, 4);
				g.drawPolygon(p);
				if (boldLine) {
					x[0]--;
					x[2]++;
					y[1]++;
					y[3]--;
					p = new Polygon(x, y, 4);
					g.drawPolygon(p);
					}
				break;
			case 4:
				x = new int[3];
				y = new int[3];
				x[0] = marker.screenX-halfSize-3;
				x[1] = marker.screenX+halfSize+3;
				x[2] = marker.screenX;
				y[0] = y[1] = marker.screenY-size/3-2;
				y[2] = y[0]+size+5;
				p = new Polygon(x, y, 3);
				g.drawPolygon(p);
				if (boldLine) {
					x[0]--;
					x[1]++;
					y[0]--;
					y[1]--;
					y[2]++;
					p = new Polygon(x, y, 3);
					g.drawPolygon(p);
					}
				break;
			case 5:
				sx = size/4+2;
				sy = sx+halfSize;
				g.drawRect(marker.screenX-sx,
						   marker.screenY-sy, 2*sx, 2*sy);
				if (boldLine)
					g.drawRect(marker.screenX-sx-1,
							   marker.screenY-sy-1, 2*sx+2, 2*sy+2);
				break;
			case 6:
				sy = size/4+2;
				sx = sy+halfSize;
				g.drawRect(marker.screenX-sx,
						   marker.screenY-sy, 2*sx, 2*sy);
				if (boldLine)
					g.drawRect(marker.screenX-sx-1,
							   marker.screenY-sy-1, 2*sx+2, 2*sy+2);
				break;
				}
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);

		if (e.getType() == CompoundTableEvent.cChangeExcluded) {
			if (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) {
				invalidateOffImage(true);
				}
			if (mBackgroundColorConsidered == cVisibleRecords)
				mBackgroundValid = false;
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			for (int axis=0; axis<2; axis++)
				mScaleDepictor[axis] = null;
			for (int i=0; i<2; i++)
				mSplittingDepictor[i] = null;
			invalidateOffImage(true);
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			if (mMultiValueMarkerColumns != null) {
				int count = 0;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					mMultiValueMarkerColumns[i] = columnMapping[mMultiValueMarkerColumns[i]];
					if (mMultiValueMarkerColumns[i] == cColumnUnassigned)
						count++;
					}
				if (count != 0) {
					if (count == mMultiValueMarkerColumns.length) {
						mMultiValueMarkerColumns = null;
						}
					else {
						int[] newColumns = new int[mMultiValueMarkerColumns.length-count];
						int index = 0;
						for (int i=0; i<mMultiValueMarkerColumns.length; i++)
							if (mMultiValueMarkerColumns[i] != cColumnUnassigned)
								newColumns[index++] = mMultiValueMarkerColumns[i];
						mMultiValueMarkerColumns = newColumns;
						}
					invalidateOffImage(false);
					}
				}

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mChartColumn != cColumnUnassigned) {
				mChartColumn = columnMapping[mChartColumn];
				if (mChartColumn == cColumnUnassigned) {
					mChartMode = cChartModeCount;
					invalidateOffImage(true);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int column = e.getSpecifier();
			for (int axis=0; axis<2; axis++)
				if (column == mAxisIndex[axis])
					mScaleDepictor[axis] = null;
			for (int i=0; i<2; i++)
				if (column == mSplittingColumn[i])
					mSplittingDepictor[i] = null;
			if (mMultiValueMarkerColumns != null)
				for (int i=0; i<mMultiValueMarkerColumns.length; i++)
					if (column == mMultiValueMarkerColumns[i])
						invalidateOffImage(false);

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mChartColumn == column) {
				invalidateOffImage(true);
				}
			}

		if (mMultiValueLegend != null)
			mMultiValueLegend.compoundTableChanged(e);
		if (mBackgroundLegend != null)
			mBackgroundLegend.compoundTableChanged(e);

		mBackgroundColor.compoundTableChanged(e);
		}

	public void hitlistChanged(CompoundTableHitlistEvent e) {
		super.hitlistChanged(e);

		if (e.getType() == CompoundTableHitlistEvent.cDelete) {
			if (mBackgroundColorConsidered != cVisibleRecords) {
				if (e.getHitlistIndex() == mBackgroundColorConsidered) {
					mBackgroundColorConsidered = cVisibleRecords;
					mBackgroundValid = false;
					invalidateOffImage(false);
					}
				else if (mBackgroundColorConsidered > e.getHitlistIndex()) {
					mBackgroundColorConsidered--;
					}
				}
			}
		else if (e.getType() == CompoundTableHitlistEvent.cChange) {
			if (mBackgroundColorConsidered != cVisibleRecords) {
				if (e.getHitlistIndex() == mBackgroundColorConsidered) {
					mBackgroundValid = false;
					invalidateOffImage(false);
					}
				}
			}

		mBackgroundColor.hitlistChanged(e);
		}

	@Override
	public void colorChanged(VisualizationColor source) {
		if (source == mBackgroundColor) {
			updateBackgroundColorIndices();
			return;
			}

		super.colorChanged(source);
		}

	public VisualizationColor getBackgroundColor() {
		return mBackgroundColor;
		}

	public int getBackgroundColorConsidered() {
		return mBackgroundColorConsidered;
		}

	public int getBackgroundColorRadius() {
		return mBackgroundColorRadius;
		}

	public int getBackgroundColorFading() {
		return mBackgroundColorFading;
		}

	public void setBackgroundColorConsidered(int considered) {
		if (mBackgroundColorConsidered != considered) {
			mBackgroundColorConsidered = considered;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorRadius(int radius) {
		if (mBackgroundColorRadius != radius) {
			mBackgroundColorRadius = radius;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorFading(int fading) {
		if (mBackgroundColorFading != fading) {
			mBackgroundColorFading = fading;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	@Override
	protected void addMarkerTooltips(VisualizationPoint vp, StringBuilder sb) {
		if (mMultiValueMarkerColumns != null) {
			for (int i=0; i<mMultiValueMarkerColumns.length; i++)
		        addTooltipRow(vp.record, mMultiValueMarkerColumns[i], null, sb);
	        addTooltipRow(vp.record, mMarkerColor.getColorColumn(), null, sb);
	        addTooltipRow(vp.record, mMarkerSizeColumn, null, sb);
			}
		else {
			super.addMarkerTooltips(vp, sb);
			}
		addTooltipRow(vp.record, mBackgroundColor.getColorColumn(), null, sb);
		}

	@Override
	public boolean setViewBackground(Color c) {
		if (super.setViewBackground(c)) {
			mBackgroundValid = false;
			return true;
			}
		return false;
		}

	public float getMarkerTransparency() {
		return mMarkerTransparency;
		}

	/**
	 * Changes the marker transparency for non-histogram views
	 * @param transparency value from 0.0 to 1.0
	 */
	public void setMarkerTransparency(float transparency) {
		if (mMarkerTransparency != transparency) {
			mMarkerTransparency = transparency;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	@Override
	public void setMarkerSize(float size, boolean isAdjusting) {
		float oldSize = mRelativeMarkerSize;
		super.setMarkerSize(size, isAdjusting);
		if (oldSize != mRelativeMarkerSize)
			invalidateOffImage(true);
		}

	private void updateBackgroundColorIndices() {
		if (mBackgroundColor.getColorColumn() == cColumnUnassigned)
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cDefaultDataColorIndex;
		else if (CompoundTableHitlistHandler.isHitlistColumn(mBackgroundColor.getColorColumn())) {
			int hitlistIndex = CompoundTableHitlistHandler.getHitlistFromColumn(mBackgroundColor.getColorColumn());
			int flagNo = mTableModel.getHitlistHandler().getHitlistFlagNo(hitlistIndex);
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = mPoint[i].record.isFlagSet(flagNo) ?
						VisualizationColor.cSpecialColorCount : VisualizationColor.cSpecialColorCount + 1;
			}
		else if (mTableModel.isDescriptorColumn(mBackgroundColor.getColorColumn()))
			setBackgroundSimilarityColors();
		else if (mBackgroundColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cSpecialColorCount
						+ mTableModel.getCategoryIndex(mBackgroundColor.getColorColumn(), mPoint[i].record);
			}
		else if (mTableModel.isColumnTypeDouble(mBackgroundColor.getColorColumn())) {
			float min = Float.isNaN(mBackgroundColor.getColorMin()) ?
					mTableModel.getMinimumValue(mBackgroundColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mBackgroundColor.getColorColumn())) ?
							   (float)Math.log10(mBackgroundColor.getColorMin()) : mBackgroundColor.getColorMin();
			float max = Float.isNaN(mBackgroundColor.getColorMax()) ?
					mTableModel.getMaximumValue(mBackgroundColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mBackgroundColor.getColorColumn())) ?
							   (float)Math.log10(mBackgroundColor.getColorMax()) : mBackgroundColor.getColorMax();

			//	1. colorMin is explicitly set; max is real max, but lower than min
			// or 2. colorMax is explicitly set; min is real min, but larger than max
			// first case is OK, second needs adaption below to be handled as indented
			if (min >= max)
				if (!Float.isNaN(mBackgroundColor.getColorMax()))
					min = Float.MIN_VALUE;

			for (int i=0; i<mDataPoints; i++) {
				float value = mPoint[i].record.getDouble(mBackgroundColor.getColorColumn());
				if (Float.isNaN(value))
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cMissingDataColorIndex;
				else if (value <= min)
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (byte)VisualizationColor.cSpecialColorCount;
				else if (value >= max)
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (byte)(mBackgroundColor.getColorList().length-1);
				else
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (byte)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mBackgroundColor.getColorList().length-VisualizationColor.cSpecialColorCount-1)
						* (value - min) / (max - min));
				}
			}

		mBackgroundValid = false;
		invalidateOffImage(true);
		}

	private void setBackgroundSimilarityColors() {
		if (mActivePoint == null)
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cDefaultDataColorIndex;
		else {
			for (int i=0; i<mDataPoints; i++) {
				float similarity = mTableModel.getDescriptorSimilarity(
										mActivePoint.record, mPoint[i].record, mBackgroundColor.getColorColumn());
				if (Float.isNaN(similarity))
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cMissingDataColorIndex;
				else
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (int)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mBackgroundColor.getColorList().length - VisualizationColor.cSpecialColorCount - 1)
						* similarity);
				}
			}
		}

	public byte[] getBackgroundImageData() {
		return mBackgroundImageData;
		}

	public BufferedImage getBackgroundImage() {
		return mBackgroundImage;
		}

	public void setBackgroundImageData(byte[] imageData) {
		if (imageData == null) {
			if (mBackgroundImageData == null)
				return;

			mBackgroundImage = null;
			mBackgroundImageData = null;
			mSuppressScale = false;
			}
		else {
			try {
				mBackgroundImage = ImageIO.read(new ByteArrayInputStream(imageData));
				mBackgroundImageData = imageData;
				}
			catch (IOException e) {}
			}

		invalidateOffImage(false);
		}

   public void setBackgroundImage(BufferedImage image) {
		if (image == null) {
			if (mBackgroundImage == null)
				return;

			mBackgroundImage = null;
			mBackgroundImageData = null;
			}
		else {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(image, "png", baos);
				mBackgroundImageData = baos.toByteArray();
				mBackgroundImage = image;
				}
			catch (IOException e) {}
			}

		invalidateOffImage(false);
		}

	public VisualizationPoint findMarker(int x, int y) {
		if (mChartType == cChartTypePies) {
			if (mChartInfo != null && mChartInfo.barOrPieDataAvailable) {
				int catCount = mCategoryVisibleCount[0]*mCategoryVisibleCount[1]*mCaseSeparationCategoryCount; 
				for (int hv=mHVCount-1; hv>=0; hv--) {
					for (int cat=catCount-1; cat>=0; cat--) {
						float dx = x - mChartInfo.pieX[hv][cat];
						float dy = mChartInfo.pieY[hv][cat] - y;
						float radius = Math.round(mChartInfo.pieSize[hv][cat]/2);
						if (Math.sqrt(dx*dx+dy*dy) < radius) {
							float angle = (dx==0) ? ((dy>0) ? (float)Math.PI/2 : -(float)Math.PI/2)
										 : (dx<0) ? (float)Math.PI + (float)Math.atan(dy/dx)
										 : (dy<0) ? 2*(float)Math.PI + (float)Math.atan(dy/dx) : (float)Math.atan(dy/dx);
							int index = (int)(mChartInfo.pointsInCategory[hv][cat] * angle/(2*Math.PI));
							if (index>=0 && index<mChartInfo.pointsInCategory[hv][cat]) {
								for (int i=mDataPoints-1; i>=0; i--) {
									if (mPoint[i].hvIndex == hv
									 && getChartCategoryIndex(mPoint[i]) == cat
									 && mPoint[i].chartGroupIndex == index
									 && isVisibleExcludeNaN(mPoint[i]))
										return mPoint[i];
									}
								return null;	// should never reach this
								}
							}
						}
					}
				}

			return null;
			}

		return super.findMarker(x, y);
		}

	@Override
    public int getDistanceToMarker(VisualizationPoint vp, int x, int y) {
		if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null
		 && (mChartType == cChartTypeScatterPlot
		  || mChartType == cChartTypeWhiskerPlot
		  || (mChartType == cChartTypeBoxPlot && vp.chartGroupIndex == -1))) {
			if (mMultiValueMarkerMode == cMultiValueMarkerModePies) {
				float dx = x - vp.screenX;
				float dy = y - vp.screenY;
				float a = (float)(Math.atan2(dy, dx) + Math.PI/2);	// 0 degrees is not in EAST, but in NORTH
				if (a < 0f)
					a += 2*Math.PI;
				int i = Math.min((int)(a * mMultiValueMarkerColumns.length / (2*Math.PI)), mMultiValueMarkerColumns.length-1);
				int distance = (int)Math.sqrt(dx*dx + dy*dy);
				float size = 0.5f  * vp.width * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
				int r = (int)(size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]));
				return Math.max(0, distance-r);
				}
			else {
				int minDistance = Integer.MAX_VALUE;
				int maxdx = (mMultiValueMarkerColumns.length*Math.max(2, Math.round(vp.width/(2f*(float)Math.sqrt(mMultiValueMarkerColumns.length))))+8)/2;
				int maxdy = Math.round(vp.height*2f)+4;
				if (Math.abs(x-vp.screenX) < maxdx && Math.abs(y-vp.screenY) < maxdy) {
					MultiValueBars mvbi = new MultiValueBars();
					mvbi.calculate(vp.width, vp);
					for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
						int barX = mvbi.firstBarX+i*mvbi.barWidth;
						int dx = Math.max(0, (x < barX) ? barX-x : x-(barX+mvbi.barWidth));
						int dy = Math.max(0, (y < mvbi.barY[i]) ? mvbi.barY[i]-y : y-(mvbi.barY[i]+mvbi.barHeight[i]));
						int d = Math.max(dx, dy);
						if (minDistance > d)
							minDistance = d;
						}
					}
				return minDistance;
				}
			}

		return super.getDistanceToMarker(vp, x, y);
		}

	@Override
	protected float getMarkerWidth(VisualizationPoint p) {
		// Pie charts don't use this function because marker location is handled
		// by overwriting the findMarker() method.
		if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1)) {
			if (mChartInfo == null) {
				return 0;
				}
			else if (mChartInfo.barAxis == 1) {
				return mChartInfo.barWidth;
				}
			else {
				int cat = getChartCategoryIndex(p);
				return (cat == -1) ? 0 : mChartInfo.innerDistance[p.hvIndex][cat];
				}
			}
		else {
			return getMarkerSize(p);
			}
		}

	@Override
	protected float getMarkerHeight(VisualizationPoint p) {
		// Pie charts don't use this function because marker location is handled
		// by overwriting the findMarker() method.
		if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1)) {
			if (mChartInfo == null) {
				return 0;
				}
			else if (mChartInfo.barAxis == 1) {
				int cat = getChartCategoryIndex(p);
				return (cat == -1) ? 0 : mChartInfo.innerDistance[p.hvIndex][cat];
				}
			else {
				return mChartInfo.barWidth;
				}
			}
		else {
			return getMarkerSize(p);
			}
		}

	public void initializeAxis(int axis) {
		super.initializeAxis(axis);

		mBackgroundValid = false;
		mScaleDepictor[axis] = null;
		}

	private void calculateNaNArea(int width, int height) {
		int size = Math.min((int)((NAN_WIDTH+NAN_SPACING) * mAbsoluteMarkerSize), Math.min(width, height) / 5);
		for (int axis=0; axis<mDimensions; axis++)
			mNaNSize[axis] = (!mShowNaNValues
							|| mAxisIndex[axis] == cColumnUnassigned
							|| mIsCategoryAxis[axis]
							|| mTableModel.isColumnDataComplete(mAxisIndex[axis])) ? 0 : size;
		}

	private void calculateScaleDimensions(Graphics g, int width, int height) {
		if (mSuppressScale || mTreeNodeList != null) {
			mScaleSize[0] = 0;
			mScaleSize[1] = 0;
			return;
			}

		int[] minScaleSize = new int[2];	// minimum space needed near graph root to not truncate labels of other axis

		int scaledFontSize = (int)scaleIfSplitView(mFontHeight);
		for (int axis=0; axis<2; axis++) {
			compileScaleLabels(axis);
			if (mScaleLineList[axis].isEmpty()) {	   // empty scale
				mScaleSize[axis] = 0;
				}
			else if (mScaleDepictor[axis] != null) {	// molecules on scale
				if (axis == 0) {
					mScaleSize[0] = scaledFontSize+Math.min(width*5/6/mScaleLineList[0].size()*4/5, height/10);
					}
				else {
					mScaleSize[1] = scaledFontSize+Math.min(height*5/6/mScaleLineList[1].size()*5/4, width/8);
					}
				}
			else {
				int maxSize = 0;
				int allowedMax = (axis == 0) ? height/2 : width/3;
				for (int i=0; i<mScaleLineList[axis].size(); i++) {
					String label = (String)mScaleLineList[axis].get(i).label;
					int size = getStringWidth(label);
					if (maxSize < size)
						maxSize = Math.min(size, allowedMax);
					}

				if (axis == 0) {
					// assume vertical scale to take 1/6 of total width
					int gridSize = width*5/6 / mScaleLineList[0].size();
					if (maxSize > gridSize*2) {
						mScaleSize[0] = (int)(0.71*(scaledFontSize+maxSize));
						mScaleTextMode[axis] = cScaleTextInclined;
						minScaleSize[1] = mScaleSize[0]*4/5;
						}
					else if (maxSize > gridSize) {
						mScaleSize[0] = 2*scaledFontSize;
						mScaleTextMode[axis] = cScaleTextAlternating;
						minScaleSize[1] = scaledFontSize;
						}
					else {
						mScaleSize[0] = scaledFontSize;
						mScaleTextMode[axis] = cScaleTextNormal;
						minScaleSize[1] = scaledFontSize;
						}
					}
				else {
					mScaleSize[1] = Math.max(scaledFontSize, maxSize);
					mScaleTextMode[1] = cScaleTextNormal;
					minScaleSize[0] = scaledFontSize / 2;
					}
				}
			}

		mScaleSize[0] = Math.max(minScaleSize[0]-mNaNSize[1], mScaleSize[0]);
		mScaleSize[1] = Math.max(minScaleSize[1]-mNaNSize[0], mScaleSize[1]);
		}

	private void compileSplittingHeaderMolecules() {
		for (int i=0; i<2; i++) {
			if (mSplittingMolIndex[i] != mSplittingColumn[i])
				mSplittingDepictor[i] = null;

			mSplittingDepictor[i] = null;
			if (mSplittingColumn[i] >= 0) {
				if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(mSplittingColumn[i]))
				 && mSplittingDepictor[i] == null) {
					String[] idcodeList = mTableModel.getCategoryList(mSplittingColumn[i]);
			
					mSplittingDepictor[i] = new Depictor2D[idcodeList.length];
					mSplittingMolIndex[i] = mSplittingColumn[i];

					for (int j=0; j<idcodeList.length; j++) {
						String idcode = idcodeList[j];
						if (idcode.length() != 0) {
							int index = idcode.indexOf(' ');
							StereoMolecule mol = (index == -1) ?
										new IDCodeParser(true).getCompactMolecule(idcode)
									  : new IDCodeParser(true).getCompactMolecule(
																	idcode.substring(0, index),
																	idcode.substring(index+1));
							mSplittingDepictor[i][j] = new Depictor2D(mol);
							}
						}
					}
				}
			}
		}   

	private void updateScaleMolecules(int axis, int i1, int i2) {
		String[] idcodeList = mTableModel.getCategoryList(mAxisIndex[axis]);
		if (mScaleDepictor[axis] == null) {
			mScaleDepictor[axis] = new Depictor2D[Math.min(maxDisplayedCategoryLabels(axis), idcodeList.length)];
			mScaleDepictorOffset[axis] = Math.min(i1, idcodeList.length-mScaleDepictor[axis].length);
			createScaleMolecules(axis, mScaleDepictorOffset[axis], mScaleDepictorOffset[axis]+mScaleDepictor[axis].length);
			}
		else if (i1 < mScaleDepictorOffset[axis]) {
			int shift = mScaleDepictorOffset[axis]-i1;
			for (int i=mScaleDepictor[axis].length-1; i>=shift; i--)
				mScaleDepictor[axis][i] = mScaleDepictor[axis][i-shift];
			mScaleDepictorOffset[axis] = i1;
			createScaleMolecules(axis, i1, i1+Math.min(shift, mScaleDepictor[axis].length));
			}
		else if (i2 > mScaleDepictorOffset[axis]+mScaleDepictor[axis].length) {
			int shift = i2-mScaleDepictorOffset[axis]-mScaleDepictor[axis].length;
			for (int i=0; i<mScaleDepictor[axis].length-shift; i++)
				mScaleDepictor[axis][i] = mScaleDepictor[axis][i+shift];
			mScaleDepictorOffset[axis] = i2-mScaleDepictor[axis].length;
			createScaleMolecules(axis, i2-Math.min(shift, mScaleDepictor[axis].length), i2);
			}
		}

	private void createScaleMolecules(int axis, int i1, int i2) {
		String[] idcodeList = mTableModel.getCategoryList(mAxisIndex[axis]);
		for (int i=i1; i<i2; i++) {
			String idcode = idcodeList[i];
			if (idcode.length() != 0) {
				int index = idcode.indexOf(' ');
				StereoMolecule mol = (index == -1) ?
							new IDCodeParser(true).getCompactMolecule(idcode)
						  : new IDCodeParser(true).getCompactMolecule(
														idcode.substring(0, index),
														idcode.substring(index+1));
				mScaleDepictor[axis][i-mScaleDepictorOffset[axis]] = new Depictor2D(mol);
				}
			}
		}

	protected boolean isTextCategoryAxis(int axis) {
		return mAxisIndex[axis] != cColumnUnassigned
			&& !mTableModel.isColumnTypeDouble(mAxisIndex[axis])
			&& !mTableModel.isDescriptorColumn(mAxisIndex[axis]);
		}

	private void compileScaleLabels(int axis) {
		mScaleLineList[axis].clear();
		if (mAxisIndex[axis] == cColumnUnassigned) {
			if (mChartType == cChartTypeBars && mChartInfo.barAxis == axis)
				compileDoubleScaleLabels(axis);
			}
		else {
			if (mIsCategoryAxis[axis])
				compileCategoryScaleLabels(axis);
			else
				compileDoubleScaleLabels(axis);
			}
		}

	private int maxDisplayedCategoryLabels(int axis) {
		int splitCount = (mSplitter == null) ? 1
				: (axis == 0) ? mSplitter.getHCount()
				:			   mSplitter.getVCount();

		return 64 / splitCount;
		}

	private void compileCategoryScaleLabels(int axis) {
		if ((int)(mAxisVisMax[axis]-mAxisVisMin[axis]) > maxDisplayedCategoryLabels(axis))
			return;

		String[] categoryList = mTableModel.getCategoryList(mAxisIndex[axis]);
		int entireCategoryCount = categoryList.length;
		if (entireCategoryCount == 0)
			return;

		int min = Math.round(mAxisVisMin[axis] + 0.5f);
		int max = Math.round(mAxisVisMax[axis] - 0.5f);
		if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(mAxisIndex[axis])))
			updateScaleMolecules(axis, min, max+1);

		for (int i=min; i<=max; i++) {
			float scalePosition = (mChartType == cChartTypeBars && axis == mChartInfo.barAxis) ?
				(mChartInfo.barBase - mChartInfo.axisMin) / (mChartInfo.axisMax - mChartInfo.axisMin) - 0.5f + i : i;
			float position = (scalePosition - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]);
			if (mScaleDepictor[axis] == null)
				mScaleLineList[axis].add(new ScaleLine(position, categoryList[i]));
			else
				mScaleLineList[axis].add(new ScaleLine(position, mScaleDepictor[axis][i-mScaleDepictorOffset[axis]]));
			}
		}

	private void compileDoubleScaleLabels(int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mTableModel.isLogarithmicViewMode(mChartColumn)) {
				compileLogarithmicScaleLabels(axis);
				return;
				}

			axisStart = mChartInfo.axisMin;
			axisLength = mChartInfo.axisMax - mChartInfo.axisMin;
			totalRange = axisLength;
			}
		else if (mTableModel.isDescriptorColumn(mAxisIndex[axis])) {
			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
			totalRange = 1.0f;
			}
		else {
			if (mTableModel.isLogarithmicViewMode(mAxisIndex[axis])) {
				compileLogarithmicScaleLabels(axis);
				return;
				}

			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
			totalRange = mTableModel.getMaximumValue(mAxisIndex[axis])
						- mTableModel.getMinimumValue(mAxisIndex[axis]);
			}

		if (axisLength == 0.0
		 || axisLength < totalRange/100000)
			return;

		int exponent = 0;
		while (axisLength >= 50.0) {
			axisStart /= 10;
			axisLength /= 10;
			exponent++;
			}
		while (axisLength < 5.0) {
			axisStart *= 10;
			axisLength *= 10.0;
			exponent--;
			}

		int gridSpacing = (int)(axisLength / 10);
		if (gridSpacing < 1)
			gridSpacing = 1;
		else if (gridSpacing < 2)
			gridSpacing = 2;
		else
			gridSpacing = 5;

		int theMarker = (axisStart < 0) ?
			  (int)(axisStart - 0.0000001 - (axisStart % gridSpacing))
			: (int)(axisStart + 0.0000001 + gridSpacing - (axisStart % gridSpacing));
		while ((float)theMarker < (axisStart + axisLength)) {
			float position = (float)(theMarker-axisStart) / axisLength;

			if (mAxisIndex[axis] != -1 && mTableModel.isColumnTypeDate(mAxisIndex[axis])) {
				String label = createDateLabel(theMarker, exponent);
				if (label != null)
					mScaleLineList[axis].add(new ScaleLine(position, label));
				}
			else
				mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toString(theMarker, exponent)));

			theMarker += gridSpacing;
			}
		}

	private void compileLogarithmicScaleLabels(int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			axisStart = mChartInfo.axisMin;
			axisLength = mChartInfo.axisMax - mChartInfo.axisMin;
			totalRange = axisLength;
			}
		else {
			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - axisStart;
			totalRange = mTableModel.getMaximumValue(mAxisIndex[axis])
						 - mTableModel.getMinimumValue(mAxisIndex[axis]);
			}

		if (axisLength == 0.0
		 || axisLength < totalRange/100000)
			return;

		int intMin = (int)Math.floor(axisStart);
		int intMax = (int)Math.floor(axisStart+axisLength);
		
		if (axisLength > 5.4) {
			int step = 1 + (int)axisLength/10;
			for (int i=intMin; i<=intMax; i+=step)
				addLogarithmicScaleLabel(axis, i);
			}
		else if (axisLength > 3.6) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.47712125472f);
				}
			}
		else if (axisLength > 1.8) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.301029996f);
				addLogarithmicScaleLabel(axis, i + 0.698970004f);
				}
			}
		else if (axisLength > 1.0) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.176091259f);
				addLogarithmicScaleLabel(axis, i + 0.301029996f);
				addLogarithmicScaleLabel(axis, i + 0.477121255f);
				addLogarithmicScaleLabel(axis, i + 0.698970004f);
				addLogarithmicScaleLabel(axis, i + 0.84509804f);
				}
			}
		else {
			float start = (float)Math.pow(10, axisStart);
			float length = (float)Math.pow(10, axisStart+axisLength) - start;

			int exponent = 0;
			while (length >= 50.0) {
				start /= 10;
				length /= 10;
				exponent++;
				}
			while (length < 5.0) {
				start *= 10;
				length *= 10.0;
				exponent--;
				}

			int gridSpacing = (int)(length / 10);
			if (gridSpacing < 1)
				gridSpacing = 1;
			else if (gridSpacing < 2)
				gridSpacing = 2;
			else
				gridSpacing = 5;

			int theMarker = (start < 0) ?
				  (int)(start - 0.0000001 - (start % gridSpacing))
				: (int)(start + 0.0000001 + gridSpacing - (start % gridSpacing));
			while ((float)theMarker < (start + length)) {
				float log = (float)Math.log10(theMarker) + exponent;
				float position = (float)(log-axisStart) / axisLength;
				mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toString(theMarker, exponent)));
				theMarker += gridSpacing;
				}
			}
		}

	private void addLogarithmicScaleLabel(int axis, float value) {
		float min = (mAxisIndex[axis] == -1) ? mChartInfo.axisMin : mAxisVisMin[axis];
		float max = (mAxisIndex[axis] == -1) ? mChartInfo.axisMax : mAxisVisMax[axis];
		if (value >= min && value <= max) {
			float position = (value-min) / (max - min);
			mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toString(Math.pow(10, value), 3)));
			}
		}

	/**
	 * Needs to be called before validateLegend(), because the size legend depends on it.
	 */
	private void calculateMarkerSize(Rectangle bounds) {
		if (mChartType != cChartTypeBars && mChartType != cChartTypePies) {
			if (mChartType == cChartTypeBoxPlot || mChartType == cChartTypeWhiskerPlot) {
				float cellWidth = (mChartInfo.barAxis == 1) ?
						Math.min((float)bounds.width / (float)mCategoryVisibleCount[0], (float)bounds.height / 3.0f)
					  : Math.min((float)bounds.height / (float)mCategoryVisibleCount[1], (float)bounds.width / 3.0f);
				mAbsoluteMarkerSize = mRelativeMarkerSize * cellWidth / (2.0f * (float)Math.sqrt(mCaseSeparationCategoryCount * mHVCount));
				}
			else {
				mAbsoluteMarkerSize = mRelativeMarkerSize * cMarkerSize * (float)Math.sqrt(bounds.width * bounds.height / mHVCount);
				}

			mAbsoluteConnectionLineWidth = mRelativeConnectionLineWidth * cConnectionLineWidth
		 								 * (float)Math.sqrt(bounds.width * bounds.height / mHVCount);
			if (!Float.isNaN(mMarkerSizeZoomAdaption))
				mAbsoluteConnectionLineWidth *= mMarkerSizeZoomAdaption;
			}
		}

	private void calculateCoordinates(Graphics g, Rectangle bounds) {
		int size = Math.min(bounds.width, bounds.height);
		mBorder = size/32;

		calculateNaNArea(bounds.width, bounds.height);
		calculateScaleDimensions(g, bounds.width, bounds.height);

		if (mChartType == cChartTypeScatterPlot
		 || mChartType == cChartTypeBoxPlot
		 || mChartType == cChartTypeWhiskerPlot
		 || mTreeNodeList != null) {
			Rectangle graphRect = getGraphBounds(bounds);

			float jitterMaxX = mMarkerJittering * graphRect.width
					/ (mIsCategoryAxis[0] ? mAxisVisMax[0] - mAxisVisMin[0] : 5);
			float jitterMaxY = mMarkerJittering * graphRect.height
					/ (mIsCategoryAxis[1] ? mAxisVisMax[1] - mAxisVisMin[1] : 5);

			if (mTreeNodeList != null) {
				if (mTreeNodeList.length != 0)
					calculateTreeCoordinates(graphRect);
				mBackgroundValid = false;

/*				if (mMarkerJittering > 0.0) {	// don't jitter trees
					for (int i=0; i<mDataPoints; i++) {
						mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
						mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;
						}
					}*/
				}
			else {
				float csCategoryWidth = 0;
				float csOffset = 0;
				int csAxis = getCaseSeparationAxis();
				if (csAxis != -1) {
					float width = csAxis == 0 ? graphRect.width : graphRect.height;
					float categoryWidth = (mAxisIndex[csAxis] == cColumnUnassigned) ? width
										 : width / (mAxisVisMax[csAxis]-mAxisVisMin[csAxis]);	// mCategoryCount[csAxis]; 	mCategoryCount is undefined for scatter plots
					categoryWidth *= mCaseSeparationValue;
					float csCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
					csCategoryWidth = categoryWidth / csCategoryCount;
					csOffset = (csCategoryWidth - categoryWidth) / 2;
					}

				int xNaN = Math.round(graphRect.x - mNaNSize[0] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				int yNaN = Math.round(graphRect.y + graphRect.height + mNaNSize[1] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				if (mChartType == cChartTypeScatterPlot) {
					for (int i=0; i<mDataPoints; i++) {
						// calculating coordinates for invisible records also allows to skip coordinate recalculation
						// when the visibility changes (JVisualization3D uses the inverse approach)
						float doubleX = (mAxisIndex[0] == cColumnUnassigned) ? 0.0f : getValue(mPoint[i].record, 0);
						float doubleY = (mAxisIndex[1] == cColumnUnassigned) ? 0.0f : getValue(mPoint[i].record, 1);
						mPoint[i].screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + Math.round((doubleX-mAxisVisMin[0])*graphRect.width
															/ (mAxisVisMax[0]-mAxisVisMin[0]));
						mPoint[i].screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + Math.round((mAxisVisMin[1]-doubleY)*graphRect.height
															/ (mAxisVisMax[1]-mAxisVisMin[1]));
						if (mMarkerJittering > 0.0) {
							mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
							mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;
							}
						if (csAxis != -1) {
							float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, mPoint[i].record);
							if (csAxis == 0)
								mPoint[i].screenX += csShift;
							else
								mPoint[i].screenY -= csShift;
							}
						}
					}
				else {	// mChartType == cChartTypeBoxPlot or cChartTypeWhiskerPlot
					boolean xIsDoubleCategory = mChartInfo.barAxis == 1
											 && mAxisIndex[0] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[0]);
					boolean yIsDoubleCategory = mChartInfo.barAxis == 0
											 && mAxisIndex[1] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[1]);
					for (int i=0; i<mDataPoints; i++) {
						if (mChartType == cChartTypeWhiskerPlot
						 || mPoint[i].chartGroupIndex == -1) {
							if (mAxisIndex[0] == cColumnUnassigned)
								mPoint[i].screenX = graphRect.x + Math.round(graphRect.width * 0.5f);
							else if (xIsDoubleCategory)
								mPoint[i].screenX = graphRect.x + Math.round(graphRect.width
											* (0.5f + getCategoryIndex(0, mPoint[i])) / mCategoryVisibleCount[0]);
							else {
								float doubleX = getValue(mPoint[i].record, 0);
								mPoint[i].screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + Math.round((doubleX-mAxisVisMin[0])*graphRect.width
															/ (mAxisVisMax[0]-mAxisVisMin[0]));							}
							if (mAxisIndex[1] == cColumnUnassigned)
								mPoint[i].screenY = graphRect.y + Math.round(graphRect.height * 0.5f);
							else if (yIsDoubleCategory)
								mPoint[i].screenY = graphRect.y + graphRect.height - Math.round(graphRect.height
											* (0.5f + getCategoryIndex(1, mPoint[i])) / mCategoryVisibleCount[1]);
							else {
								float doubleY = getValue(mPoint[i].record, 1);
								mPoint[i].screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + Math.round((mAxisVisMin[1]-doubleY)*graphRect.height
															/ (mAxisVisMax[1]-mAxisVisMin[1]));
								}
							if (mMarkerJittering > 0.0) {
								mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
								mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;
								}
	
							if (csAxis != -1) {
								float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, mPoint[i].record);
								if (csAxis == 0)
									mPoint[i].screenX += csShift;
								else
									mPoint[i].screenY -= csShift;
								}
							}
						}
					}
				}

			addSplittingOffset();
			}

	   	mCoordinatesValid = true;
		}

	private void calculateTreeCoordinates(Rectangle graphRect) {
		if (mTreeViewMode == cTreeViewModeRadial) {
			float zoomFactor = (!mTreeViewShowAll || Float.isNaN(mMarkerSizeZoomAdaption)) ? 1f : mMarkerSizeZoomAdaption;
			float preferredMarkerDistance = 4*mAbsoluteMarkerSize*zoomFactor;
			RadialGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, preferredMarkerDistance);
			return;
			}
		if (mTreeViewMode == cTreeViewModeHTree) {
			int maxLayerDistance = graphRect.height / 4;
			int maxNeighborDistance = graphRect.width / 8;
			TreeGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, false, maxLayerDistance, maxNeighborDistance);
			}
		if (mTreeViewMode == cTreeViewModeVTree) {
			int maxLayerDistance = graphRect.width / 4;
			int maxNeighborDistance = graphRect.height / 8;
			TreeGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, true, maxLayerDistance, maxNeighborDistance);
			}
		}

	private void addSplittingOffset() {
		if (mSplittingColumn[0] != cColumnUnassigned) {
			int gridWidth = mSplitter.getGridWidth();
			int gridHeight = mSplitter.getGridHeight();
			for (int i=0; i<mDataPoints; i++) {
				if (mChartType == cChartTypeScatterPlot
				 || mChartType == cChartTypeWhiskerPlot
				 || mPoint[i].chartGroupIndex == -1) {
					int hIndex = mSplitter.getHIndex((int)mPoint[i].hvIndex);
					int vIndex = mSplitter.getVIndex((int)mPoint[i].hvIndex);
					mPoint[i].screenX += hIndex * gridWidth;
					mPoint[i].screenY += vIndex * gridHeight;
					}
				}
			}
		}

	/**
	 * Calculates the background color array for all split views.
	 * @param graphBounds used in case of tree view only
	 */
	private void calculateBackground(Rectangle graphBounds) {
		int backgroundSize = (int)(240.0 - 60.0 * Math.log(mBackgroundColorRadius));
		int backgroundColorRadius = mBackgroundColorRadius;
		if (mIsHighResolution) {
			backgroundSize *= 2;
			backgroundColorRadius *= 2;
			}

		if (mSplitter == null) {
			mBackgroundHCount = 1;
			mBackgroundVCount = 1;
			}
		else {
			backgroundSize *= 2;
			backgroundColorRadius *= 2;
			mBackgroundHCount = mSplitter.getHCount();
			mBackgroundVCount = mSplitter.getVCount();
			}
		int backgroundWidth = backgroundSize / mBackgroundHCount;
		int backgroundHeight = backgroundSize / mBackgroundVCount;

			// add all points' RGB color components to respective grid cells
			// consider all points that are less than backgroundColorRadius away from visible area
		float[][][] backgroundR = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundG = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundB = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundC = new float[mHVCount][backgroundWidth][backgroundHeight];

		float xMin,xMax,yMin,yMax;
		if (mTreeNodeList != null) {
			xMin = graphBounds.x;
			yMin = graphBounds.y + graphBounds.height;
			xMax = graphBounds.x + graphBounds.width;
			yMax = graphBounds.y;
			}
		else {
			if (mAxisIndex[0] == cColumnUnassigned) {
				xMin = mAxisVisMin[0];
				xMax = mAxisVisMax[0];
				}
			else if (mIsCategoryAxis[0]) {
				xMin = -0.5f;
				xMax = -0.5f + mTableModel.getCategoryCount(mAxisIndex[0]);
				}
			else {
				xMin = mTableModel.getMinimumValue(mAxisIndex[0]);
				xMax = mTableModel.getMaximumValue(mAxisIndex[0]);
				}
	
			if (mAxisIndex[1] == cColumnUnassigned) {
				yMin = mAxisVisMin[1];
				yMax = mAxisVisMax[1];
				}
			else if (mIsCategoryAxis[1]) {
				yMin = -0.5f;
				yMax = -0.5f + mTableModel.getCategoryCount(mAxisIndex[1]);
				}
			else {
				yMin = mTableModel.getMinimumValue(mAxisIndex[1]);
				yMax = mTableModel.getMaximumValue(mAxisIndex[1]);
				}
			}

		Color neutralColor = getViewBackground();
		int neutralR = neutralColor.getRed();
		int neutralG = neutralColor.getGreen();
		int neutralB = neutralColor.getBlue();

		float rangeX = xMax - xMin;
		float rangeY = yMax - yMin;
		boolean considerVisibleRecords = (mBackgroundColorConsidered == cVisibleRecords) || (mTreeNodeList != null);
		int hitlistFlagNo = (considerVisibleRecords) ? -1
						: mTableModel.getHitlistHandler().getHitlistFlagNo(mBackgroundColorConsidered);
		for (int i=0; i<mDataPoints; i++) {
			if ((considerVisibleRecords
			  && isVisibleExcludeNaN(mPoint[i]))
			 || (!considerVisibleRecords && mPoint[i].record.isFlagSet(hitlistFlagNo)))	{
				float valueX;
				float valueY;
				if (mTreeNodeList != null) {
					valueX = mPoint[i].screenX;
					valueY = mPoint[i].screenY;
					}
				else {
					valueX = (mAxisIndex[0] == cColumnUnassigned) ? (xMin + xMax) / 2 : getValue(mPoint[i].record, 0);
					valueY = (mAxisIndex[1] == cColumnUnassigned) ? (yMin + yMax) / 2 : getValue(mPoint[i].record, 1);
					}
							  
				if (Float.isNaN(valueX) || Float.isNaN(valueY))
					continue;

				int x = Math.min(backgroundWidth-1, (int)(backgroundWidth * (valueX - xMin) / rangeX));
				int y = Math.min(backgroundHeight-1, (int)(backgroundHeight * (valueY - yMin) / rangeY));

				Color c = mBackgroundColor.getColorList()[((VisualizationPoint2D)mPoint[i]).backgroundColorIndex];
				backgroundR[mPoint[i].hvIndex][x][y] += c.getRed() - neutralR;
				backgroundG[mPoint[i].hvIndex][x][y] += c.getGreen() - neutralG;
				backgroundB[mPoint[i].hvIndex][x][y] += c.getBlue() - neutralB;
				backgroundC[mPoint[i].hvIndex][x][y] += 1.0;	// simply counts individual colors added
				}
			}

			// propagate colors to grid neighbourhood via cosine function
		float[][] influence = new float[backgroundColorRadius][backgroundColorRadius];
		for (int x=0; x<backgroundColorRadius; x++) {
			for (int y=0; y<backgroundColorRadius; y++) {
				float distance = (float)Math.sqrt(x*x + y*y);
				if (distance < backgroundColorRadius)
					influence[x][y] = (float)(0.5 + Math.cos(Math.PI*distance/(float)backgroundColorRadius) / 2.0);
				}
			}
		float[][][] smoothR = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothG = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothB = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothC = new float[mHVCount][backgroundWidth][backgroundHeight];
		boolean xIsCyclic = (mAxisIndex[0] == cColumnUnassigned) ? false
									: (mTableModel.getColumnProperty(mAxisIndex[0],
										CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
		boolean yIsCyclic = (mAxisIndex[1] == cColumnUnassigned) ? false
									: (mTableModel.getColumnProperty(mAxisIndex[1],
										CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
		for (int x=0; x<backgroundWidth; x++) {
			int xmin = x-backgroundColorRadius+1;
			if (xmin < 0 && !xIsCyclic)
				xmin = 0;
			int xmax = x+backgroundColorRadius-1;
			if (xmax >= backgroundWidth && !xIsCyclic)
				xmax = backgroundWidth-1;

			for (int y=0; y<backgroundHeight; y++) {
				int ymin = y-backgroundColorRadius+1;
				if (ymin < 0 && !yIsCyclic)
					ymin = 0;
				int ymax = y+backgroundColorRadius-1;
				if (ymax >= backgroundHeight && !yIsCyclic)
					ymax = backgroundHeight-1;
	
				for (int hv=0; hv<mHVCount; hv++) {
					if (backgroundC[hv][x][y] > (float)0.0) {
						for (int ix=xmin; ix<=xmax; ix++) {
							int dx = Math.abs(x-ix);
	
							int destX = ix;
							if (destX < 0)
								destX += backgroundWidth;
							else if (destX >= backgroundWidth)
								destX -= backgroundWidth;
	
							for (int iy=ymin; iy<=ymax; iy++) {
								int dy = Math.abs(y-iy);
	
								int destY = iy;
								if (destY < 0)
									destY += backgroundHeight;
								else if (destY >= backgroundHeight)
									destY -= backgroundHeight;
	
								if (influence[dx][dy] > (float)0.0) {
									smoothR[hv][destX][destY] += influence[dx][dy] * backgroundR[hv][x][y];
									smoothG[hv][destX][destY] += influence[dx][dy] * backgroundG[hv][x][y];
									smoothB[hv][destX][destY] += influence[dx][dy] * backgroundB[hv][x][y];
									smoothC[hv][destX][destY] += influence[dx][dy] * backgroundC[hv][x][y];
									}
								}
							}
						}
					}
				}
			}

			// find highest sum of RGB components
		float max = (float)0.0;
		for (int hv=0; hv<mHVCount; hv++)
			for (int x=0; x<backgroundWidth; x++)
				for (int y=0; y<backgroundHeight; y++)
					if (max < smoothC[hv][x][y])
						max = smoothC[hv][x][y];

		float fading = (float)Math.exp(Math.log(1.0)-(float)mBackgroundColorFading/20*(Math.log(1.0)-Math.log(0.1)));

		mBackground = new Color[mHVCount][backgroundWidth][backgroundHeight];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int x=0; x<backgroundWidth; x++) {
				for (int y=0; y<backgroundHeight; y++) {
					if (smoothC[hv][x][y] == 0) {
						mBackground[hv][x][y] = neutralColor;
						}
					else {
						float f = (float)Math.exp(fading*Math.log(smoothC[hv][x][y] / max)) / smoothC[hv][x][y];
						mBackground[hv][x][y] = new Color(neutralR+(int)(f*smoothR[hv][x][y]),
														  neutralG+(int)(f*smoothG[hv][x][y]),
														  neutralB+(int)(f*smoothB[hv][x][y]));
						}
					}
				}
			}

		mBackgroundValid = true;
		}

	private void drawBackground(Graphics g, Rectangle graphRect, int hvIndex) {
		ViewPort port = new ViewPort();

		if (hasColorBackground()) {
			int backgroundWidth = mBackground[0].length;
			int backgroundHeight = mBackground[0][0].length;
	
			int[] x = new int[backgroundWidth+1];
			int[] y = new int[backgroundHeight+1];
	
			float factorX = (float)graphRect.width/port.getVisRangle(0);
			float factorY = (float)graphRect.height/port.getVisRangle(1);
	
			int minxi = 0;
			int maxxi = backgroundWidth;
			int minyi = 0;
			int maxyi = backgroundHeight;
			for (int i=0; i<=backgroundWidth; i++) {
				float axisX = port.min[0]+i*port.getRange(0)/backgroundWidth;
				x[i] = graphRect.x + (int)(factorX*(axisX-port.visMin[0]));
				if (x[i] <= graphRect.x) {
					x[i] = graphRect.x;
					minxi = i;
					}
				if (x[i] >= graphRect.x+graphRect.width) {
					x[i] = graphRect.x+graphRect.width;
					maxxi = i;
					break;
					}
				}
			for (int i=0; i<=backgroundHeight; i++) {
	 			float axisY = port.min[1]+i*port.getRange(1)/backgroundHeight;
				y[i] = graphRect.y+graphRect.height - (int)(factorY*(axisY-port.visMin[1]));
				if (y[i] >= graphRect.y+graphRect.height) {
					y[i] = graphRect.y+graphRect.height;
					minyi = i;
					}
				if (y[i] <= graphRect.y) {
					y[i] = graphRect.y;
					maxyi = i;
					break;
					}
				}
			for (int xi=minxi; xi<maxxi; xi++) {
				for (int yi=minyi; yi<maxyi; yi++) {
					g.setColor(mBackground[hvIndex][xi][yi]);
					g.fillRect(x[xi], y[yi+1], x[xi+1]-x[xi], y[yi]-y[yi+1]);
					}
				}
			}

		if (mBackgroundImage != null) {
			int sx1 = Math.round((float)mBackgroundImage.getWidth()*(port.visMin[0]-port.min[0])/port.getRange(0));
			int sx2 = Math.round((float)mBackgroundImage.getWidth()*(port.visMax[0]-port.min[0])/port.getRange(0));
			int sy1 = Math.round((float)mBackgroundImage.getHeight()*(port.max[1]-port.visMax[1])/port.getRange(1));
			int sy2 = Math.round((float)mBackgroundImage.getHeight()*(port.max[1]-port.visMin[1])/port.getRange(1));
			if (sx1 < sx2 && sy1 < sy2)
				g.drawImage(mBackgroundImage, graphRect.x, graphRect.y,
											  graphRect.x+graphRect.width, graphRect.y+graphRect.height,
											  sx1, sy1, sx2, sy2, null);
			}
		}

	private void drawNaNArea(Graphics g, Rectangle graphRect) {
		mG.setColor(getContrastGrey(0.1f));
		int xNaNSpace = Math.round(mNaNSize[0] * NAN_SPACING / (NAN_WIDTH + NAN_SPACING));
		int yNaNSpace = Math.round(mNaNSize[1] * NAN_SPACING / (NAN_WIDTH + NAN_SPACING));
		if (mNaNSize[0] != 0)
			mG.fillRect(graphRect.x - mNaNSize[0], graphRect.y, mNaNSize[0] - xNaNSpace, graphRect.height + mNaNSize[1]);
		if (mNaNSize[1] != 0)
			mG.fillRect(graphRect.x - mNaNSize[0], graphRect.y + graphRect.height + yNaNSpace, graphRect.width + mNaNSize[0], mNaNSize[1] - yNaNSpace);
		}

	private void drawAxes(Graphics g, Rectangle graphRect) {
		g.setColor(getContrastGrey(SCALE_STRONG));

		int xmin = graphRect.x;
		int xmax = graphRect.x+graphRect.width;
		int ymin = graphRect.y;
		int ymax = graphRect.y+graphRect.height;

		int arrowSize = (int)Math.min(0.8*scaleIfSplitView(mFontHeight), mBorder);
		int[] px = new int[3];
		int[] py = new int[3];
		if (mAxisIndex[0] != cColumnUnassigned
		 || (mChartType == cChartTypeBars && mChartInfo.barAxis == 0)) {
			g.drawLine(xmin, ymax, xmax, ymax);
			px[0] = xmax;
			py[0] = ymax - arrowSize/3;
			px[1] = xmax;
			py[1] = ymax + arrowSize/3;
			px[2] = xmax + arrowSize;
			py[2] = ymax;
			g.fillPolygon(px, py, 3);

			String label = (mAxisIndex[0] != cColumnUnassigned) ? getAxisTitle(mAxisIndex[0])
					: mChartMode == cChartModeCount ? "Count"
					: mChartMode == cChartModePercent ? "Percent"
					: CHART_MODE_AXIS_TEXT[mChartMode]+"("+mTableModel.getColumnTitle(mChartColumn)+")";
			g.drawString(label,
					xmax-g.getFontMetrics().stringWidth(label),
					ymax+mScaleSize[0]+mNaNSize[1]+g.getFontMetrics().getAscent());
			}

		if (mAxisIndex[1] != cColumnUnassigned
		 || (mChartType == cChartTypeBars && mChartInfo.barAxis == 1)) {
			g.drawLine(xmin, ymax, xmin, ymin);
			px[0] = xmin - arrowSize/3;
			py[0] = ymin;
			px[1] = xmin + arrowSize/3;
			py[1] = ymin;
			px[2] = xmin;
			py[2] = ymin - arrowSize;
			g.fillPolygon(px, py, 3);

			String label = (mAxisIndex[1] != cColumnUnassigned) ? getAxisTitle(mAxisIndex[1])
					: mChartMode == cChartModeCount ? "Count"
					: mChartMode == cChartModePercent ? "Percent"
					: CHART_MODE_AXIS_TEXT[mChartMode]+"("+mTableModel.getColumnTitle(mChartColumn)+")";
			int labelX = xmin-mBorder-g.getFontMetrics().stringWidth(label);
			if (labelX < xmin-mBorder-mScaleSize[1]-mNaNSize[0])
				labelX = xmin+mBorder;
			g.drawString(label, labelX, ymin-g.getFontMetrics().getDescent());
			}
		}

	private void drawGrid(Graphics g, Rectangle graphRect) {
		for (int axis=0; axis<2; axis++) {
			for (int i=0; i<mScaleLineList[axis].size(); i++)
				drawScaleLine(g, graphRect, axis, i);
			}
		}

	private void drawScaleLine(Graphics g, Rectangle graphRect, int axis, int index) {
		ScaleLine scaleLine = mScaleLineList[axis].get(index);
		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		if (axis == 0) {	// X-axis
			int axisPosition = graphRect.x + Math.round(graphRect.width*scaleLine.position);

			if (!mSuppressGrid) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(axisPosition, graphRect.y, axisPosition, graphRect.y+graphRect.height+mNaNSize[1]);
				}

			if (scaleLine.label != null && !mSuppressScale) {
				if (scaleLine.label instanceof String) {
					g.setColor(getContrastGrey(SCALE_MEDIUM));
					String label = (String)scaleLine.label;
					if (mScaleTextMode[axis] == cScaleTextInclined) {
						int labelWidth = g.getFontMetrics().stringWidth(label);
						int textX = axisPosition-(int)(0.71*labelWidth);
						int textY = graphRect.y+graphRect.height+mNaNSize[1]+(int)(0.71*(scaledFontHeight+labelWidth));
						((Graphics2D)g).rotate(-Math.PI/4, textX, textY);
						g.drawString(label, textX, textY);
						((Graphics2D)g).rotate(Math.PI/4, textX, textY);
						}
					else {
						int yShift = ((mScaleTextMode[axis] == cScaleTextAlternating && (index & 1)==1)) ?
								scaledFontHeight : 0;
						g.drawString(label,
									 axisPosition-g.getFontMetrics().stringWidth(label)/2,
									 graphRect.y+graphRect.height+mNaNSize[1]+scaledFontHeight+yShift);
						}
					}
				else {
					Depictor2D depictor = (Depictor2D)scaleLine.label;
					depictor.setOverruleColor(getContrastGrey(SCALE_MEDIUM), null);
					drawScaleMolecule(g, graphRect, axis, scaleLine.position, depictor);
					}
				}
			}
		else {  // Y-axis
			int axisPosition = graphRect.y+graphRect.height + Math.round(-graphRect.height*scaleLine.position);

			if (!mSuppressGrid) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(graphRect.x-mNaNSize[0], axisPosition, graphRect.x+graphRect.width+mNaNSize[0], axisPosition);
				}

			if (scaleLine.label != null && !mSuppressScale) {
				if (scaleLine.label instanceof String) {
					g.setColor(getContrastGrey(SCALE_MEDIUM));
					String label = (String)scaleLine.label;
					g.drawString(label,
								 graphRect.x-mNaNSize[0]-4-g.getFontMetrics().stringWidth(label),
								 axisPosition+scaledFontHeight/2);
					}
				else {
					Depictor2D depictor = (Depictor2D)scaleLine.label;
					depictor.setOverruleColor(getContrastGrey(SCALE_MEDIUM), null);
					drawScaleMolecule(g, graphRect, axis, scaleLine.position, depictor);
					}
				}
			}
		}

	private void drawScaleMolecule(Graphics g, Rectangle graphRect, int axis, float position, Depictor2D depictor) {
		int x,y,w,h;

		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		if (axis == 0) {	// X-axis
			h = mScaleSize[axis]-scaledFontHeight;
			w = h*5/4;
			x = graphRect.x + (int)((float)graphRect.width * position) - w/2;
			y = graphRect.y + graphRect.height + mNaNSize[1] + scaledFontHeight/2;
			}
		else {  // Y-axis
			w = mScaleSize[axis]-scaledFontHeight;
			h = w*4/5;
			x = graphRect.x - mNaNSize[0] - w -scaledFontHeight/2;
			y = graphRect.y + graphRect.height - (int)((float)graphRect.height * position) - h/2;
			}

		int maxAVBL = Depictor2D.cOptAvBondLen;
		if (mIsHighResolution)
			maxAVBL *= mFontScaling;
		depictor.validateView(g, new Rectangle2D.Float(x, y, w, h), Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
		depictor.paint(g);
		}

	public int getShownCorrelationType() {
		return mShownCorrelationType;
		}

	public void setShownCorrelationType(int type) {
		if (mShownCorrelationType != type) {
			mShownCorrelationType = type;
			invalidateOffImage(false);
			}
		}
	
	public int getCurveMode() {
		return mCurveInfo & cCurveModeMask;
		}

	public boolean isShowStandardDeviation() {
		return (mCurveInfo & cCurveStandardDeviation) != 0;
		}

	public boolean isCurveSplitByCategory() {
		return (mCurveInfo & cCurveSplitByCategory) != 0;
		}

	public void setCurveMode(int mode, boolean drawStdDevRange, boolean splitByCategory) {
		int newInfo = mode
					+ (drawStdDevRange ? cCurveStandardDeviation : 0)
					+ (splitByCategory ? cCurveSplitByCategory : 0);
		if (mCurveInfo != newInfo) {
			mCurveInfo = newInfo;
			invalidateOffImage(false);
			}
		}

	public int[] getMultiValueMarkerColumns() {
		return mMultiValueMarkerColumns;
		}

	public int getMultiValueMarkerMode() {
		return mMultiValueMarkerMode;
		}

	public void setMultiValueMarkerColumns(int[] columns, int mode) {
		if (columns == null)
			mode = cMultiValueMarkerModeNone;
		if (mode == cMultiValueMarkerModeNone)
			columns = null;

		boolean isChange = (mMultiValueMarkerMode != mode);
		if (!isChange) {
			isChange = (columns != mMultiValueMarkerColumns);
			if (columns != null && mMultiValueMarkerColumns != null) {
				isChange = true;
				if (columns.length == mMultiValueMarkerColumns.length) {
					isChange = false;
					for (int i=0; i<columns.length; i++) {
						if (columns[i] != mMultiValueMarkerColumns[i]) {
							isChange = true;
							break;
							}
						}
					}
				}
			}
		if (isChange) {
			mMultiValueMarkerColumns = columns;
			mMultiValueMarkerMode = mode;
			invalidateOffImage(true);
			}
		}

	protected Color getMultiValueMarkerColor(int i) {
		return mMultiValueMarkerColor[i];
		}

	@Override
	protected VisualizationPoint createVisualizationPoint(CompoundRecord record) {
		return new VisualizationPoint2D(record);
		}

	protected int getStringWidth(String s) {
		// used by JVisualizationLegend
		return (int)mG.getFontMetrics().getStringBounds(s, mG).getWidth();
		}

	protected void setFontHeightAndScaleToSplitView(float h) {
		mG.setFont(new Font("Helvetica", Font.PLAIN, (int)scaleIfSplitView(h)));
		}

	protected void setFontHeight(int h) {
		mG.setFont(new Font("Helvetica", Font.PLAIN, h));
		}

	/**
	 * If the view is split, then all text drawing is reduced in size
	 * depending on the number of split views. If we don't have view splitting
	 * then no scaling is done.
	 * @return value scaled down properly to be used in split view
	 */
	private float scaleIfSplitView(float value) {
		return (mHVCount == 1) ? value : (float)(value / Math.pow(mHVCount, 0.3));
		}

	protected void setColor(Color c) {
		mG.setColor(c);
		}

	protected void drawLine(int x1, int y1, int x2, int y2) {
		mG.drawLine(x1, y1, x2, y2);
		}

	protected void drawRect(int x, int y, int w, int h) {
		mG.drawRect(x, y, w-1, h-1);
		}

	protected void fillRect(int x, int y, int w, int h) {
		mG.fillRect(x, y, w, h);
		}

	protected void drawString(String s, int x, int y) {
		mG.drawString(s, x, y);
		}

	protected void drawMolecule(StereoMolecule mol, Color color, Rectangle2D.Float rect, int mode, int maxAVBL) {
		Depictor2D d = new Depictor2D(mol);
		d.validateView(mG, rect, mode+maxAVBL);
		d.setOverruleColor(color, null);
		d.paint(mG);
		}

	protected void paintLegend(Rectangle bounds) {
		super.paintLegend(bounds);

		if (mMultiValueLegend != null)
			mMultiValueLegend.paint(bounds);
		if (mBackgroundLegend != null)
			mBackgroundLegend.paint(bounds);
		}

	private void calculateLegend(Rectangle bounds) {
		int scaledFontHeight = (int)scaleIfSplitView(mFontHeight);
		super.calculateLegend(bounds, scaledFontHeight);

		if (mMultiValueMarkerMode != cMultiValueMarkerModeNone
		 && mMultiValueMarkerColumns != null
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			mMultiValueLegend = new JVisualizationLegend(this, mTableModel, cColumnUnassigned, null,
														 JVisualizationLegend.cLegendTypeMultiValueMarker);
			mMultiValueLegend.calculate(bounds, scaledFontHeight);
			bounds.height -= mMultiValueLegend.getHeight();
			}
		else {
			mMultiValueLegend = null;
			}

		if (mBackgroundColor.getColorColumn() != cColumnUnassigned
		 && mChartType != cChartTypeBars) {
			mBackgroundLegend = new JVisualizationLegend(this, mTableModel,
													mBackgroundColor.getColorColumn(),
													mBackgroundColor,
													mBackgroundColor.getColorListMode() == VisualizationColor.cColorListModeCategories ?
													  JVisualizationLegend.cLegendTypeBackgroundColorCategory
													: JVisualizationLegend.cLegendTypeBackgroundColorDouble);
			mBackgroundLegend.calculate(bounds, scaledFontHeight);
			bounds.height -= mBackgroundLegend.getHeight();
			}
		else {
			mBackgroundLegend = null;
			}
		}

	public int getAvailableShapeCount() {
		return cAvailableShapeCount;
		}

	public int[] getSupportedChartTypes() {
		return SUPPORTED_CHART_TYPE;
		}

	class ScaleLine {
		float position;
		Object label;

		ScaleLine(float position, Object label) {
			this.position = position;
			this.label = label;
			}
		}

	class MultiValueBars {
		private float top,bottom;	// relative usage of area above and below zero line (0...1); is the same for all markers in a view
		private float[] relValue;	// relative value of a specific marker compared to max/min (-1...1)
		int barWidth,firstBarX,zeroY;
		int[] barY,barHeight;

		MultiValueBars() {
			barY = new int[mMultiValueMarkerColumns.length];
			barHeight = new int[mMultiValueMarkerColumns.length];
			relValue = new float[mMultiValueMarkerColumns.length];
			calculateExtends();
			}

		/**
		 * The relative area above and below of the zero line, when showing
		 * multiple bars representing multiple column values of one row.
		 * @return float[2] with top and bottom area between 0.0 and 1.0 each
		 */
		private void calculateExtends() {
			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				float min = mTableModel.getMinimumValue(mMultiValueMarkerColumns[i]);
				float max = mTableModel.getMaximumValue(mMultiValueMarkerColumns[i]);
				if (min >= 0f) {
					top = 1f;
					continue;
					}
				if (max <= 0f) {
					bottom = 1f;
					continue;
					}
				float topPart = max / (max - min);
				if (top < topPart)
					top = topPart;
				if (bottom < 1f - topPart)
					bottom = 1f - topPart;
				}
			}

		private void calculate(float size, VisualizationPoint vp) {
			float factor = 0;
			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				float min = mTableModel.getMinimumValue(mMultiValueMarkerColumns[i]);
				float max = mTableModel.getMaximumValue(mMultiValueMarkerColumns[i]);
				float value = vp.record.getDouble(mMultiValueMarkerColumns[i]);
				relValue[i] = Float.isNaN(value) ? Float.NaN
							: (min >= 0f) ? value/max
							: (max <= 0f) ? value/-min
							: (max/top > -min/bottom) ? value*top/max : value*bottom/-min;
				if (!Float.isNaN(value))
					factor = Math.max(factor, Math.abs(relValue[i]));
				}

			float height = size*2f;	// value range of one bar in pixel; the histogram height is <= 2*height
			barWidth = Math.max(2, Math.round(size/(2f*(float)Math.sqrt(mMultiValueMarkerColumns.length))));

			// if we have not used all height, then we reduce barWidth and give it to height
			float widthReduction = 1f;
			int newBarWidth = Math.max(1, (int)((barWidth+1) * Math.sqrt(factor)));
			if (newBarWidth < barWidth) {
				widthReduction = (float)barWidth / (float)newBarWidth;
				barWidth = newBarWidth;
				}

			firstBarX = vp.screenX - mMultiValueMarkerColumns.length * barWidth / 2;
			zeroY = vp.screenY + Math.round(height*factor*widthReduction*0.5f*(top-bottom));

			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				if (Float.isNaN(relValue[i])) {
					barHeight[i] = -1;
					barY[i] = zeroY+1;
					}
				else if (relValue[i] > 0) {
					barHeight[i] = Math.round(height*widthReduction*relValue[i]);
					barY[i] = zeroY-barHeight[i];
					}
				else {
					barHeight[i] = -Math.round(height*widthReduction*relValue[i]);
					barY[i] = zeroY+1;
					}
				}
			}
		}

	class ViewPort {
		float[] min,max,visMin,visMax;

		ViewPort() {
			min = new float[2];
			max = new float[2];
			visMin = new float[2];
			visMax = new float[2];
			for (int i=0; i<2; i++) {
				int column = mAxisIndex[i];
				if (column == cColumnUnassigned || mTreeNodeList != null) {
					min[i] = mAxisVisMin[i];
					max[i] = mAxisVisMax[i];
					}
				else if (mIsCategoryAxis[i]) {
					min[i] = -0.5f;
					max[i] = -0.5f + mTableModel.getCategoryCount(column);
					}
				else {
					min[i] = mTableModel.getMinimumValue(column);
					max[i] = mTableModel.getMaximumValue(column);
					}
				visMin[i] = mAxisVisMin[i];
				visMax[i] = mAxisVisMax[i];
				}
			}

		float getRange(int dimension) {
			return max[dimension] - min[dimension];
			}

		float getVisRangle(int dimension) {
			return visMax[dimension] - visMin[dimension];
			}
		}
	}
