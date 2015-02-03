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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.dnd.MoleculeDragAdapter;
import com.actelion.research.gui.dnd.MoleculeTransferable;
import com.actelion.research.table.CompoundListSelectionModel;
import com.actelion.research.table.CompoundRecord;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.HighlightListener;
import com.actelion.research.table.MarkerLabelDisplayer;

public class JStructureGrid extends JScrollPane
		implements CompoundTableColorHandler.ColorListener,CompoundTableView,FocusableView,HighlightListener,
					ListSelectionListener,MarkerLabelDisplayer,MouseListener,MouseMotionListener,Printable {
	private static final long serialVersionUID = 0x20060904;

	private static final Color cSelectionColor = UIManager.getColor("TextArea.selectionBackground");
	private static final Color cOutOfFocusBackground = new Color(224, 224, 224);
	private static final Color cOutOfFocusForeground = new Color(192, 192, 192);

	private Frame						mParentFrame;
	private CompoundTableModel			mTableModel;
	private CompoundTableColorHandler	mColorHandler;
	private CompoundListSelectionModel	mSelectionModel;
	private CompoundRecord				mActiveRow,mHighlightedRow;
	private JStructureGridContentPanel	mContentPanel;
	private GridCellSize				mCellSize;
	private float						mMarkerLabelSize;
	private int							mNoOfColumns,mTableRowCount,
										mRecordCountOnLastValidation,mIndexOfFirstImage,
										mTableRowCountOnLastValidation,mFocusHitlist,mFocusCount,
										mStructureColumn,mCoordinateColumn;
	private int[]						mLabelColumn,mFocusRow;
	private ArrayList<GridImage> 		mImageList;
	private boolean						mFocusValid,mSelectionChanged,mRepaintRequested,mShowAnyLabels;
	private DetailPopupProvider			mDetailPopupProvider;

	public JStructureGrid(Frame parent, CompoundTableModel tableModel,
						  CompoundTableColorHandler colorHandler, CompoundListSelectionModel selectionModel,
						  int structureColumn, int noOfColumns) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mColorHandler = colorHandler;
		mSelectionModel = selectionModel;
		setStructureColumn(structureColumn);
		mTableModel.addHighlightListener(this);
		mSelectionModel.addListSelectionListener(this);
		getVerticalScrollBar().setUnitIncrement(16);
		addMouseListener(this);
		addMouseMotionListener(this);
		mNoOfColumns = noOfColumns;
		mLabelColumn = new int[cPositionCode.length];
		for (int i=0; i<mLabelColumn.length; i++)
			mLabelColumn[i] = -1;
		mMarkerLabelSize = 1.0f;
		mActiveRow = mTableModel.getActiveRow();
		mHighlightedRow = mTableModel.getHighlightedRow();
		mImageList = new ArrayList<GridImage>();
		mContentPanel = new JStructureGridContentPanel();
		mContentPanel.setBackground(Color.white);
		mFocusHitlist = cHitlistUnassigned;
		mFocusValid = true;

		setBorder(BorderFactory.createEmptyBorder());
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		setViewportView(mContentPanel);

		mColorHandler.addColorListener(this);

		initializeDragAndDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		}

	public void setStructureColumn(int structureColumn) {
		mStructureColumn = structureColumn;
		mCoordinateColumn = (structureColumn == -1) ? -1
						  : mTableModel.getChildColumn(mStructureColumn, CompoundTableModel.cColumnType2DCoordinates);
		}

	public int getStructureColumn() {
		return mStructureColumn;
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	public void cleanup() {
		mTableModel.removeHighlightListener(this);
		mSelectionModel.removeListSelectionListener(this);
		mColorHandler.removeColorListener(this);
		}

	public void colorChanged(int column, int type, VisualizationColor color) {
		if (mStructureColumn == column) {
			invalidateView();
			return;
			}
		for (int i=0; i<mLabelColumn.length; i++) {
			if (mLabelColumn[i] == column) {
				invalidateView();
				return;
				}
			}
		}

	public int getTotalHeight(int totalWidth) {
		if (mTableModel.getRowCount() == 0)
			return 0;

		int cellHeight = new GridCellSize(totalWidth, 4, 1.0f, 1.0f, 7).totalHeight;
		if (cellHeight == 0)
			return 0;

		int lineCount = (mTableModel.getRowCount() + mNoOfColumns - 1) / mNoOfColumns;
		return lineCount * cellHeight;
		}

	public synchronized void paintHighResolution(Graphics g, Dimension size, float scaling, boolean transparentBG) {
		GridCellSize cellSize = new GridCellSize(size.width, (int)(scaling*2), 1.0f, 1.0f, 7);
		String footer = "";
		Rectangle2D.Float bounds = new Rectangle2D.Float(0, 0, size.width, size.height);
		paintHighResolution(g, bounds, cellSize, 0, mTableModel.getRowCount(), scaling, transparentBG, footer);
		}

	public synchronized int print(Graphics g, PageFormat f, int pageIndex) {
		final float FOOTER_HEIGHT = 12.0f;

		GridCellSize cellSize = new GridCellSize((int)f.getImageableWidth(), 4, 1.0f, 0.85f, 3);

		if ((int)f.getImageableHeight() < cellSize.totalHeight)
			return NO_SUCH_PAGE;

		if (mTableModel.getRowCount() == 0)
			return NO_SUCH_PAGE;

		int structuresOnPage = mNoOfColumns * ((int)(f.getImageableHeight()-FOOTER_HEIGHT) / cellSize.totalHeight);
		int maxPageIndex = (mTableModel.getRowCount()-1) / structuresOnPage;
		if (pageIndex > maxPageIndex)
			return NO_SUCH_PAGE;

		int offset = pageIndex * structuresOnPage;
		if (structuresOnPage > mTableModel.getRowCount() - offset)
			structuresOnPage = mTableModel.getRowCount() - offset;

		Rectangle2D.Float bounds = new Rectangle2D.Float((float)f.getImageableX(),
														  (float)f.getImageableY(),
														  (float)f.getImageableWidth(),
														  (float)f.getImageableHeight());
		String footer = "page "+(pageIndex+1)+" of "+(maxPageIndex+1);
		paintHighResolution(g, bounds, cellSize, offset, structuresOnPage, 1, false, footer);
		
		return PAGE_EXISTS;
		}

	private void paintHighResolution(Graphics g, Rectangle2D.Float bounds, GridCellSize cellSize,
			int firstRow, int rowCount, float scaling, boolean transparentBG, String footer) {
   		validateFocus();

		Graphics2D g2D = (Graphics2D)g;
		g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final float FOOTER_FONT_SIZE = 8.0f;

		Rectangle2D.Float r = new Rectangle2D.Float();

		Stroke outlineStroke = new BasicStroke(scaling);

		// print footer
		float x1 = bounds.x;
		float x2 = bounds.x+bounds.width;
		float y2 = bounds.y+bounds.height-FOOTER_FONT_SIZE/4;
		g2D.setColor(Color.black);
		g2D.setFont(new Font("Helvetica", Font.PLAIN, (int)FOOTER_FONT_SIZE));
		String docTitle = mParentFrame.getTitle();
		g2D.drawString(docTitle, (int)x1, (int)y2);
		g2D.drawString(footer, (int)x2-g2D.getFontMetrics().stringWidth(footer), (int)y2);

		Composite opaque = null;
		Composite transparent = null;
		if (transparentBG) {
			opaque = g2D.getComposite();
			transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f); 
			}

		Shape oldClip = g2D.getClip();
		StereoMolecule molContainer = new StereoMolecule();
		for (int i=0; i<rowCount; i++) {
			r.setRect(bounds.x+(i % mNoOfColumns) * cellSize.scaledWidth + cellSize.border/2,
					  bounds.y+(i / mNoOfColumns) * cellSize.scaledHeight + cellSize.border/2,
					  cellSize.scaledWidth - cellSize.border,
					  cellSize.scaledHeight - cellSize.border);

			g2D.setColor(Color.lightGray);
			g2D.setStroke(outlineStroke);
			g2D.draw(new Rectangle2D.Float(r.x, r.y, r.width, r.height));

			r.x += cellSize.border/2;
			r.y += cellSize.border/2;
			r.width -= cellSize.border;
			r.height -= cellSize.border;
			g2D.setClip(r);

			if (mShowAnyLabels) {
				g2D.setColor(Color.black);
				g2D.setFont(new Font("Helvetica", Font.PLAIN, (int)cellSize.fontSize));
				for (int position=0; position<mLabelColumn.length; position++) {
					int column = mLabelColumn[position];
					if (column != -1) {
						Rectangle2D.Float labelRect = new Rectangle2D.Float(r.x, r.y, cellSize.viewWidth / 3, cellSize.fontSize);
						switch (position) {
						case MarkerLabelDisplayer.cTopLeft:
							labelRect.height = cellSize.topHeight;
							break;
						case MarkerLabelDisplayer.cTopCenter:
							labelRect.x += labelRect.width;
							labelRect.height = cellSize.topHeight;
							break;
						case MarkerLabelDisplayer.cTopRight:
							labelRect.x += cellSize.viewWidth - labelRect.width;
							labelRect.height = cellSize.topHeight;
							break;
						case MarkerLabelDisplayer.cBottomLeft:
							labelRect.y += cellSize.structureHeight - cellSize.bottomHeight;
							labelRect.height = cellSize.bottomHeight;
							break;
						case MarkerLabelDisplayer.cBottomCenter:
							labelRect.x += labelRect.width;
							labelRect.y += cellSize.structureHeight - cellSize.bottomHeight;
							labelRect.height = cellSize.bottomHeight;
							break;
						case MarkerLabelDisplayer.cBottomRight:
							labelRect.x += cellSize.viewWidth - labelRect.width;
							labelRect.y += cellSize.structureHeight - cellSize.bottomHeight;
							labelRect.height = cellSize.bottomHeight;
							break;
						default:
							labelRect.y += cellSize.viewHeight
								- cellSize.fontSize * (mTableRowCount
								- position + MarkerLabelDisplayer.cFirstTablePosition);
							labelRect.width = cellSize.viewWidth;
							break;
							}
						
						if (mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND)) {
							if (transparentBG)
								g2D.setComposite(transparent);
							g2D.setColor(mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND).getLighterColor(getRecord(firstRow+i)));
							g2D.fill(labelRect);
							if (transparentBG)
								g2D.setComposite(opaque);
							}
	
						if (mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND))
							g2D.setColor(mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND).getDarkerColor(getRecord(firstRow+i)));
						else
							g2D.setColor(Color.black);
	
						float x = 0;
						float y = 0;
						String label = mTableModel.getValue(getRecord(firstRow+i), column).replaceAll("\n", "; ");
						switch (position) {
						case MarkerLabelDisplayer.cTopLeft:
							x = 2.0f;
							y = cellSize.fontSize;
							break;
						case MarkerLabelDisplayer.cTopCenter:
							x = (cellSize.viewWidth - g2D.getFontMetrics().stringWidth(label)) / 2;
							y = cellSize.fontSize;
							break;
						case MarkerLabelDisplayer.cTopRight:
							x = cellSize.viewWidth-2 - g2D.getFontMetrics().stringWidth(label);
							y = cellSize.fontSize;
							break;
						case MarkerLabelDisplayer.cBottomLeft:
							x = 2;
							y = cellSize.structureHeight - cellSize.fontSize/3;
							break;
						case MarkerLabelDisplayer.cBottomCenter:
							x = (cellSize.viewWidth - g2D.getFontMetrics().stringWidth(label)) / 2;
							y = cellSize.structureHeight - cellSize.fontSize/3;
							break;
						case MarkerLabelDisplayer.cBottomRight:
							x = cellSize.viewWidth-2 - g2D.getFontMetrics().stringWidth(label);
							y = cellSize.structureHeight - cellSize.fontSize/3;
							break;
						default:
							String columnName = mTableModel.getColumnTitle(column)+":";
							x = cellSize.viewWidth/2-1 - g2D.getFontMetrics().stringWidth(columnName);
							y = cellSize.viewHeight + cellSize.fontSize *7/8 - cellSize.fontSize * (mTableRowCount -
								position + MarkerLabelDisplayer.cFirstTablePosition);
							g2D.drawString(columnName, r.x+x, r.y+y);
							x = cellSize.viewWidth/2;
							break;
							}
						g2D.drawString(label, r.x+x, r.y+y);
						}
					}
				}

			if (mColorHandler.hasColorAssigned(mStructureColumn, CompoundTableColorHandler.BACKGROUND)) {
				if (transparentBG)
					g2D.setComposite(transparent);
				g2D.setColor(mColorHandler.getVisualizationColor(mStructureColumn, CompoundTableColorHandler.BACKGROUND).getLighterColor(getRecord(firstRow+i)));
				g2D.fill(new Rectangle2D.Float(r.x, r.y+cellSize.topHeight, r.width, r.height-cellSize.topHeight-cellSize.bottomHeight-cellSize.tableHeight));
				if (transparentBG)
					g2D.setComposite(opaque);
				}

			StereoMolecule mol = mTableModel.getChemicalStructure(getRecord(firstRow+i), mStructureColumn, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, molContainer);
			if (mol != null) {
				g2D.setColor(Color.black);
				Depictor2D depictor = new Depictor2D(mol);
				depictor.validateView(g2D, new Rectangle2D.Float(r.x,
											r.y+cellSize.topHeight,
											r.width,
											r.height-cellSize.topHeight-cellSize.bottomHeight-cellSize.tableHeight),
									  AbstractDepictor.cModeInflateToMaxAVBL+(int)(scaling*AbstractDepictor.cOptAvBondLen));

				if (mColorHandler.hasColorAssigned(mStructureColumn, CompoundTableColorHandler.FOREGROUND)) {
					Color foreground = mColorHandler.getVisualizationColor(mStructureColumn, CompoundTableColorHandler.FOREGROUND).getDarkerColor(getRecord(firstRow+i));
					Color background = mColorHandler.hasColorAssigned(mStructureColumn, CompoundTableColorHandler.BACKGROUND) ?
							mColorHandler.getVisualizationColor(mStructureColumn, CompoundTableColorHandler.BACKGROUND).getLighterColor(getRecord(firstRow+i)) : Color.white;
					depictor.setOverruleColor(foreground, background);
					}
				depictor.paint(g2D);
				}

			g2D.setClip(oldClip);
			}
		}

	/**
	 * Returns the record to be shown at this index position
	 * considering whether we have an active focus. In this case
	 * the records in focus are returned first.
	 * @param index
	 * @return
	 */
	private CompoundRecord getRecord(int row) {
		return (mFocusRow == null) ? mTableModel.getRecord(row) : mTableModel.getRecord(mFocusRow[row]);
		}

	public void setDetailPopupProvider(DetailPopupProvider p) {
		mDetailPopupProvider = p;
		}

	public void setColumnCount(int no) {
		if (mNoOfColumns != no) {
			mNoOfColumns = no;
			mRepaintRequested = true;	// to track and abort nested paint() calls
			mContentPanel.repaint();
			}
		}

	public int getColumnCount() {
		return mNoOfColumns;
		}

	@Override
	public int getFocusHitlist() {
		return mFocusHitlist;
		}

	@Override
	public void setFocusHitlist(int no) {
		if (mFocusHitlist != no) {
			mFocusHitlist = no;
			mFocusValid = false;
			invalidateView();
			}
		}

	private void validateFocus() {
		if (!mFocusValid) {
			if (mFocusHitlist == cHitlistUnassigned) {
				mFocusRow = null;
				}
			else {
				int focusFlag = (mFocusHitlist == cFocusOnSelection) ? CompoundRecord.cFlagSelected
							  : mTableModel.getHitlistHandler().getHitlistFlagNo(mFocusHitlist);
		
				mFocusCount = 0;
				for (int row=0; row<mTableModel.getRowCount(); row++)
					if (mTableModel.getRecord(row).isFlagSet(focusFlag))
						mFocusCount++;
		
				mFocusRow = new int[mTableModel.getRowCount()];
				int focusIndex = 0;
				int nonFocusIndex = mFocusCount;
		
				for (int row=0; row<mTableModel.getRowCount(); row++)
					if (mTableModel.getRecord(row).isFlagSet(focusFlag))
						mFocusRow[focusIndex++] = row;
					else
						mFocusRow[nonFocusIndex++] = row;
				}

			mFocusValid = true;
			}
		}

	public synchronized void compoundTableChanged(CompoundTableEvent e) {
		boolean needsUpdate = false;
		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows
		 || e.getType() == CompoundTableEvent.cChangeExcluded
		 || e.getType() == CompoundTableEvent.cChangeSortOrder) {
			if (mFocusHitlist != cHitlistUnassigned)
				mFocusValid = false;
			mHighlightedRow = mTableModel.getHighlightedRow();
			needsUpdate = true;
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			if (mStructureColumn != -1)
				mStructureColumn = columnMapping[mStructureColumn];
			if (mCoordinateColumn != -1)
				mCoordinateColumn = columnMapping[mCoordinateColumn];

			for (int i=0; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] >= 0) {
					mLabelColumn[i] = columnMapping[mLabelColumn[i]];
					if (mLabelColumn[i] == -1)
						needsUpdate = true;
					}
				}
			int position = cFirstTablePosition;
			for (int i=cFirstTablePosition; i<mLabelColumn.length; i++) {
				if (i != position && mLabelColumn[i] != -1) {
					mLabelColumn[position++] = mLabelColumn[i];
					mLabelColumn[i] = -1;
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			if (mStructureColumn == e.getSpecifier())
				needsUpdate = true;
			else {
				for (int i=0; i<mLabelColumn.length; i++) {
					if (mLabelColumn[i] == e.getSpecifier()) {
						needsUpdate = true;
						break;
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			for (int i=cFirstTablePosition; i<mLabelColumn.length; i++) {
				if (mLabelColumn[i] == e.getSpecifier()) {
					needsUpdate = true;
					break;
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			updateActiveRow(mTableModel.getActiveRow());
			}

		if (needsUpdate)
			invalidateView();
		}

	public void invalidateView() {
		mImageList.clear();
		mRepaintRequested = true;	// to track and abort nested paint() calls
		mContentPanel.repaint();
		}

	@Override
	public void hitlistChanged(CompoundTableHitlistEvent e) {
		if (e.getType() == CompoundTableHitlistEvent.cDelete) {
			if (mFocusHitlist == e.getHitlistIndex())
				setFocusHitlist(cHitlistUnassigned);
			else if (mFocusHitlist > e.getHitlistIndex())
				mFocusHitlist--;
			}
		if (e.getType() == CompoundTableHitlistEvent.cChange) {
			if (mFocusHitlist == e.getHitlistIndex()) {
				mFocusValid = false;
				invalidateView();
				}
			}
		}

	@Override
	public boolean isMarkerLabelsInTreeViewOnly() {
		return false;
		}

	@Override
	public void setMarkerLabelsInTreeViewOnly(boolean inTreeViewOnly) {
		}

	@Override
	public boolean isTreeViewModeEnabled() {
		return false;
		}

	/**
	 * Assigns a column to any one of the allowed marker positions of this view.
	 * @param columnAtPosition column index or -1 for every allowed position
	 */
	@Override
	public void setMarkerLabels(int[] columnAtPosition) {
		mLabelColumn = columnAtPosition;

		for (int i=cTopLeft; i<=cTopRight; i++)
			if (mLabelColumn[i] != -1)
				mShowAnyLabels = true;
		for (int i=cBottomLeft; i<=cBottomRight; i++)
			if (mLabelColumn[i] != -1)
				mShowAnyLabels = true;

		mTableRowCount = 0;
		int position = cInTableLine1;
		for (int i=cInTableLine1; i<mLabelColumn.length; i++) {
			if (mLabelColumn[i] != -1) {
				if (i != position) {	// in case we have holes in the table
					mLabelColumn[position] = mLabelColumn[i];
					mLabelColumn[i] = -1;
					}
				position++;
				mTableRowCount++;
				mShowAnyLabels = true;
				}
			}
		validateSize(true);
		invalidateView();
		}

	public boolean supportsMarkerLabelTable() {
		return true;
		}

	public boolean supportsMidPositionLabels() {
		return false;
		}

	public int getMarkerLabelTableEntryCount() {
		return mTableRowCount;
		}

	public int getMarkerLabelColumn(int position) {
		return mLabelColumn[position];
		}

	public float getMarkerLabelSize() {
		return mMarkerLabelSize;
		}

	public void setMarkerLabelSize(float size, boolean isAdjusting) {
		if (mMarkerLabelSize != size) {
			mMarkerLabelSize = size;
			if (mShowAnyLabels) {
				validateSize(true);
				invalidateView();
				}
			}
		}

	public synchronized void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			if (mFocusHitlist == cFocusOnSelection) {
				mFocusValid = false;
				invalidateView();
				}
			else {
				mHighlightedRow = mTableModel.getHighlightedRow();
				mSelectionChanged = true;
				mContentPanel.repaint();
				}
			}
		}

	private void updateActiveRow(CompoundRecord record) {
		if (mActiveRow != record) {
			if (mTableModel.getStructureHiliteMode(mStructureColumn) == CompoundTableModel.cStructureHiliteModeCurrentRow)
				invalidateView();

			Rectangle r = new Rectangle();
			for (int i=0; i<mImageList.size(); i++) {
				int row = mIndexOfFirstImage + i;
				if (getRecord(row) == mActiveRow
				 || getRecord(row) == record) {
					setFieldRect(r, row);
					mContentPanel.repaint(r);
					}
				}
			mActiveRow = record;
			}
		}

	@Override
	public void highlightChanged(CompoundRecord record) {
		if (mHighlightedRow != record) {
			Rectangle r = new Rectangle();
			for (int i=0; i<mImageList.size(); i++) {
				int row = mIndexOfFirstImage + i;
				if (getRecord(row) == mHighlightedRow
				 || getRecord(row) == record) {
					setFieldRect(r, row);
					mContentPanel.repaint(r);
					}
				}
			mHighlightedRow = record;
			}
		}

	public void mouseClicked(MouseEvent e) {
		int viewIndex = getIndex(e.getX(), e.getY());
		CompoundRecord record = (viewIndex == -1) ? null : getRecord(viewIndex);
		mTableModel.setActiveRow(record);
		}

	public synchronized void mousePressed(MouseEvent e) {
		if (!handlePopupTrigger(e)
		 && (e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
			int index = getIndex(e.getX(), e.getY());
			if (index != -1) {
				if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
					mSelectionModel.setLeadSelectionIndex(index);
					}
				else if ((e.getModifiers() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()) != 0) {
					if (getRecord(index).isSelected())
						mSelectionModel.removeSelectionInterval(index, index);
					else
						mSelectionModel.addSelectionInterval(index, index);
					}
				else {
					mSelectionModel.clearSelection();
					mSelectionModel.addSelectionInterval(index, index);
					}
				}
			}
		}

	public synchronized void mouseReleased(MouseEvent e) {
		handlePopupTrigger(e);
		}

	public void mouseEntered(MouseEvent e) {
		}

	public void mouseExited(MouseEvent e) {
		}

	private boolean handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger()) {
			int index = getIndex(e.getX(), e.getY());
			if (index != -1 && mDetailPopupProvider != null) {
				JPopupMenu popup = mDetailPopupProvider.createPopupMenu(getRecord(index), this, -1);
				if (popup != null)
					popup.show(this, e.getX(), e.getY());
				}
			return true;
			}
		return false;
		}

	public synchronized void mouseMoved(MouseEvent e) {
		int index = getIndex(e.getX(), e.getY());
		CompoundRecord record = (index == -1) ? null : getRecord(index);
		if (mHighlightedRow != record)
			mTableModel.setHighlightedRow(record);
		}

	public synchronized void mouseDragged(MouseEvent e) {
		}

	private void setFieldRect(Rectangle r, int index) {
		r.setRect((index % mNoOfColumns) * mCellSize.totalWidth,
				  (index / mNoOfColumns) * mCellSize.totalHeight,
				  mCellSize.totalWidth,
				  mCellSize.totalHeight);
		}

	private int getIndex(int x, int y) {
		if (mCellSize == null || mCellSize.totalWidth == 0)
			return -1;

		int column = (x-1) / mCellSize.totalWidth;
		if (column < 0 || column >= mNoOfColumns)
			return -1;

		int row = (y-1+getVerticalScrollBar().getValue()) / mCellSize.totalHeight;
		if (row < 0)
			return -1;

		int index = column + mNoOfColumns * row;
		if (index >= mTableModel.getRowCount())
			return -1;

		return index;
		}

	private void validateSize(boolean labelChanged) {
		Rectangle r = getViewportBorderBounds();
		if (mCellSize == null || mCellSize.totalWidth != r.width/mNoOfColumns
		 || mRecordCountOnLastValidation != mTableModel.getRowCount()
		 || mTableRowCountOnLastValidation != mTableRowCount
		 || labelChanged) {
			mRecordCountOnLastValidation = mTableModel.getRowCount();
			mTableRowCountOnLastValidation = mTableRowCount;

			mCellSize = new GridCellSize(r.width, 1, JVisualization.sRetinaFactor, 1.0f, 7);

			mImageList.clear();
			mContentPanel.setPreferredSize(
				new Dimension(mCellSize.totalWidth*mNoOfColumns,
							  mCellSize.totalHeight*((mTableModel.getRowCount()+mNoOfColumns-1)/mNoOfColumns)));
			mContentPanel.revalidate();
			}
		}

	private void initializeDragAndDrop(int dragAction) {
//		final JStructureGrid outer = this;

		if (dragAction != DnDConstants.ACTION_NONE) {
			new MoleculeDragAdapter(this) {
				public Transferable getTransferable(Point origin) {
					return getMoleculeTransferable(origin);
					}

/*				public void onDragEnter() {
					outer.onDragEnter();
					}

				public void onDragOver() {
					outer.onDragOver();
					}

				public void onDragExit() {
					outer.onDragExit();
					}	*/
				};
			}
		}

	protected Transferable getMoleculeTransferable(Point pt) {
		int row = getIndex(pt.x, pt.y);
		if (row == -1)
			return null;
		StereoMolecule mol = mTableModel.getChemicalStructure(getRecord(row), mStructureColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
		return new MoleculeTransferable(mol);
		}


	class JStructureGridContentPanel extends JPanel {
		private static final long serialVersionUID = 0x20060904;

		public void paintComponent(Graphics g) {
			mRepaintRequested = false;
			super.paintComponent(g);
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			validateSize(false);
			validateFocus();

			Rectangle visRect = mContentPanel.getVisibleRect();
			int firstVisible = visRect.y / mCellSize.totalHeight * mNoOfColumns;
			int firstNonVisible = (visRect.y + visRect.height) / mCellSize.totalHeight * mNoOfColumns + mNoOfColumns;
			if (firstNonVisible > mTableModel.getRowCount())
				firstNonVisible = mTableModel.getRowCount();

				// adapt imagelist to contain exactly those images of the structures currently visible
			if (firstVisible > mIndexOfFirstImage) {
				if (mIndexOfFirstImage + mImageList.size() < firstVisible)
					mImageList.clear();
				else
					for (int i=0; i<firstVisible-mIndexOfFirstImage; i++)
						mImageList.remove(0);
				mIndexOfFirstImage = firstVisible;
				}
			if (firstNonVisible < mIndexOfFirstImage + mImageList.size()) {
				if (mIndexOfFirstImage > firstNonVisible)
					mImageList.clear();
				else {
					int firstToRemove = firstNonVisible - mIndexOfFirstImage;
					while (mImageList.size() > firstToRemove)
						mImageList.remove(firstToRemove);
					}
				}

				// create and add not yet available structure images to imagelist
			if (mSelectionChanged || firstNonVisible - firstVisible > mImageList.size()) {
				StereoMolecule molContainer = new StereoMolecule();
				int indexAfterLastImage = mIndexOfFirstImage + mImageList.size();
				for (int i=firstVisible; i<firstNonVisible; i++) {
					if ((i < mIndexOfFirstImage
					  || i >= indexAfterLastImage
					  || (mImageList.get(i-firstVisible)).isSelected != getRecord(i).isSelected())
					 && mCellSize.scaledWidth > 0
					 && mCellSize.scaledHeight > 0) {
						BufferedImage image = new BufferedImage(mCellSize.scaledWidth, mCellSize.scaledHeight, BufferedImage.TYPE_INT_ARGB);
						Graphics ig = image.getGraphics();
						((Graphics2D)ig).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						((Graphics2D)ig).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
// doesn't seem to work ((Graphics2D)ig).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
						ig.setColor(Color.LIGHT_GRAY);
						for (int j=0; j<mCellSize.border; j++)
							ig.drawRect(j, j, mCellSize.scaledWidth-2*j-1, mCellSize.scaledHeight-2*j-1);

						if (mFocusHitlist != cHitlistUnassigned)
							ig.setColor(i<mFocusCount ? Color.white : cOutOfFocusBackground);
						else
							ig.setColor((getRecord(i).isSelected()) ? cSelectionColor : Color.white);
						ig.fillRect(mCellSize.border, mCellSize.border, mCellSize.viewWidth, mCellSize.viewHeight);

						boolean isOutOfFocus = (mFocusHitlist != cHitlistUnassigned && i>=mFocusCount);

						if (mShowAnyLabels) {
							ig.setFont(new Font("Helvetica", Font.PLAIN, mCellSize.fontSize));
							for (int position=0; position<mLabelColumn.length; position++) {
								int column = mLabelColumn[position];
								if (column != -1) {
									int x = mCellSize.border;
									int y = mCellSize.border;
									if (!isOutOfFocus && mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND)) {
										int width = (2 + mCellSize.viewWidth) / 3;
										int height = mCellSize.fontSize;
										switch (position) {
										case MarkerLabelDisplayer.cTopLeft:
											height = mCellSize.topHeight;
											break;
										case MarkerLabelDisplayer.cTopCenter:
											x += width;
											height = mCellSize.topHeight;
											break;
										case MarkerLabelDisplayer.cTopRight:
											x += mCellSize.viewWidth - width;
											height = mCellSize.topHeight;
											break;
										case MarkerLabelDisplayer.cBottomLeft:
											y += mCellSize.structureHeight - mCellSize.bottomHeight;
											height = mCellSize.bottomHeight;
											break;
										case MarkerLabelDisplayer.cBottomCenter:
											x += width;
											y += mCellSize.structureHeight - mCellSize.bottomHeight;
											height = mCellSize.bottomHeight;
											break;
										case MarkerLabelDisplayer.cBottomRight:
											x += mCellSize.viewWidth - width;
											y += mCellSize.structureHeight - mCellSize.bottomHeight;
											height = mCellSize.bottomHeight;
											break;
										default:
											y += mCellSize.viewHeight
												- mCellSize.fontSize * (mTableRowCount
												- position + MarkerLabelDisplayer.cFirstTablePosition);
											width = mCellSize.viewWidth;
											break;
											}
										ig.setColor(mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND).getLighterColor(getRecord(i)));
										ig.fillRect(x, y, width, height);
										}

									if (isOutOfFocus)
										ig.setColor(cOutOfFocusForeground);
									else if (mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND))
										ig.setColor(mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND).getDarkerColor(getRecord(i)));
									else
										ig.setColor(Color.black);

									x = mCellSize.border;
									y = mCellSize.border;
									String label = mTableModel.getValue(getRecord(i), column).replaceAll("\n", "; ");
									switch (position) {
									case MarkerLabelDisplayer.cTopLeft:
										x += 3;
										y += mCellSize.fontSize;
										break;
									case MarkerLabelDisplayer.cTopCenter:
										x += (mCellSize.viewWidth - ig.getFontMetrics().stringWidth(label)) / 2;
										y += mCellSize.fontSize;
										break;
									case MarkerLabelDisplayer.cTopRight:
										x += mCellSize.viewWidth - 3 - ig.getFontMetrics().stringWidth(label);
										y += mCellSize.fontSize;
										break;
									case MarkerLabelDisplayer.cBottomLeft:
										x += 3;
										y += mCellSize.structureHeight - mCellSize.fontSize / 3;
										break;
									case MarkerLabelDisplayer.cBottomCenter:
										x += (mCellSize.viewWidth - ig.getFontMetrics().stringWidth(label)) / 2;
										y += mCellSize.structureHeight - mCellSize.fontSize / 3;
										break;
									case MarkerLabelDisplayer.cBottomRight:
										x += mCellSize.viewWidth - 3 - ig.getFontMetrics().stringWidth(label);
										y += mCellSize.structureHeight - mCellSize.fontSize / 3;
										break;
									default:
										String columnName = mTableModel.getColumnTitle(column)+":";
										x += mCellSize.viewWidth / 2 - ig.getFontMetrics().stringWidth(columnName) - 2;
										y += mCellSize.viewHeight + mCellSize.fontSize *7/8
										  - mCellSize.fontSize * (mTableRowCount -
												  position + MarkerLabelDisplayer.cFirstTablePosition);
										ig.drawString(columnName, x, y);
										x = mCellSize.scaledWidth / 2;
										break;
										}

									ig.drawString(label, x, y);
									}
								}
							}

						if (!isOutOfFocus && mColorHandler.hasColorAssigned(mStructureColumn, CompoundTableColorHandler.BACKGROUND)) {
							ig.setColor(mColorHandler.getVisualizationColor(mStructureColumn, CompoundTableColorHandler.BACKGROUND).getLighterColor(getRecord(i)));
							ig.fillRect(mCellSize.border, mCellSize.border+mCellSize.topHeight, mCellSize.viewWidth, mCellSize.structureHeight-mCellSize.topHeight-mCellSize.bottomHeight);
							}

						StereoMolecule mol = mTableModel.getChemicalStructure(getRecord(i), mStructureColumn, CompoundTableModel.ATOM_COLOR_MODE_ALL, molContainer);
						if (mol != null) {
							AbstractDepictor depictor = (JVisualization.sRetinaFactor == 1f) ? new Depictor2D(mol) : new Depictor2D(mol);
							depictor.validateView(ig,
												  new Rectangle2D.Float(mCellSize.border,
														(float)mCellSize.border+mCellSize.topHeight,
														(float)(mCellSize.viewWidth),
														(float)(mCellSize.structureHeight-mCellSize.topHeight-mCellSize.bottomHeight)),
												  AbstractDepictor.cModeInflateToMaxAVBL+(int)(JVisualization.sRetinaFactor*AbstractDepictor.cOptAvBondLen));
							if (isOutOfFocus)
								depictor.setOverruleColor(cOutOfFocusForeground, cOutOfFocusBackground);
							else if (mColorHandler.hasColorAssigned(mStructureColumn, CompoundTableColorHandler.FOREGROUND)) {
								Color foreground = mColorHandler.getVisualizationColor(mStructureColumn, CompoundTableColorHandler.FOREGROUND).getDarkerColor(getRecord(i));
								Color background = mColorHandler.hasColorAssigned(mStructureColumn, CompoundTableColorHandler.BACKGROUND) ?
										mColorHandler.getVisualizationColor(mStructureColumn, CompoundTableColorHandler.BACKGROUND).getLighterColor(getRecord(i)) : Color.white;
								depictor.setOverruleColor(foreground, background);
								}

							depictor.paint(ig);
							}

						GridImage gridImage = new GridImage(image, getRecord(i).isSelected());
						if (i < mIndexOfFirstImage)
							mImageList.add(i-firstVisible, gridImage);
						else if (i >= indexAfterLastImage)
							mImageList.add(gridImage);
						else
							mImageList.set(i-firstVisible, gridImage);

						Thread.yield();
						if (mRepaintRequested)
							return;
						}
					}
				mSelectionChanged = false;
				}
			mIndexOfFirstImage = firstVisible;

			Rectangle clipRect = g.getClipBounds();
			Rectangle r = new Rectangle();
			for (int i=firstVisible; i<firstNonVisible; i++) {
				setFieldRect(r, i);
				if (clipRect.intersects(r)) {
					// mImageList may be deleted by tableChange event
					if (i-mIndexOfFirstImage >= mImageList.size())
						return;

					GridImage gridImage = null;
					try {	// in case compoundTableChanged() deletes mImageList
						gridImage = mImageList.get(i-mIndexOfFirstImage);
						}
					catch (Exception e) { return; }

					g.drawImage(gridImage.image ,r.x, r.y, mCellSize.totalWidth, mCellSize.totalHeight, null);

					CompoundRecord record = getRecord(i);
					boolean isCurrentRecord = (mActiveRow != null && mActiveRow == record);
					boolean isHighlightedRecord = (mHighlightedRow != null && mHighlightedRow == record);
					if (isHighlightedRecord || isCurrentRecord) {
						g.setColor(isCurrentRecord && isHighlightedRecord ? Color.magenta :
													  isHighlightedRecord ? Color.blue : Color.red);
						g.drawRect(r.x, r.y, r.width-1, r.height-1);
						g.drawRect(r.x+1, r.y+1, r.width-3, r.height-3);
						}
					}
				}
			}
		}

	class GridCellSize {
		protected int border;
		protected int totalWidth;
		protected int totalHeight;
		protected int scaledWidth;
		protected int scaledHeight;
		protected int viewWidth;
		protected int viewHeight;
		protected int fontSize;
		protected int topHeight;
		protected int bottomHeight;
		protected int structureHeight;
		protected int tableHeight;

		protected GridCellSize(int gridWidth, int cellBorder, float contentScale, float fontScale, int minFontSize) {
			totalWidth = gridWidth / mNoOfColumns;
			border = (int)(contentScale * cellBorder);
			scaledWidth = (int)(contentScale * totalWidth);
			viewWidth = scaledWidth - 2 * border;

			fontSize = (int)(mMarkerLabelSize * contentScale * Math.max(fontScale*viewWidth/16, minFontSize));
			topHeight = 0;
			bottomHeight = 0;
			if (mShowAnyLabels) {
				for (int position=0; position<mLabelColumn.length; position++) {
					if (mLabelColumn[position] != -1) {
						if (position == cTopLeft || position == cTopCenter || position == cTopRight)
							topHeight = fontSize *4/3;
						if (position == cBottomLeft || position == cBottomCenter || position == cBottomRight)
							bottomHeight = fontSize *4/3;
						}
					}
				}
			tableHeight = mTableRowCount*fontSize;
			viewHeight = (tableHeight+topHeight+bottomHeight > viewWidth/2) ? 
					viewWidth/2 + tableHeight + topHeight + bottomHeight
							  : viewWidth + tableHeight/2;
			structureHeight = viewHeight - tableHeight;
			scaledHeight = viewHeight + 2 * border;
			totalHeight = (int)((float)scaledHeight / contentScale);
			}
		}
	}


class GridImage {
	protected Image image;
	protected boolean isSelected;

	protected GridImage(Image image, boolean isSelected) {
		this.image = image;
		this.isSelected = isSelected;
		}
	}
