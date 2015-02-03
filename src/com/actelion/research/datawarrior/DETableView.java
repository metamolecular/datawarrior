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
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.table.TableCellRenderer;

import com.actelion.research.datawarrior.task.DETaskSortRows;
import com.actelion.research.table.ColorizedCellRenderer;
import com.actelion.research.table.CompoundListSelectionModel;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableHitlistListener;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.MultiLineCellRenderer;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationColor;

public class DETableView extends JScrollPane
		implements CompoundTableView,CompoundTableHitlistListener,MouseListener,MouseMotionListener,Printable,CompoundTableColorHandler.ColorListener {
	private static final long serialVersionUID = 0x20060904;

	private Frame				mParentFrame;
	private DEParentPane		mParentPane;
	private DETable				mTable;
	private DECompoundTableModel	mTableModel;
	private DetailPopupProvider	mDetailPopupProvider;
	private int					mMouseX;
	private CompoundTableColorHandler	mColorHandler;

	public DETableView(Frame parentFrame, DEParentPane parentPane, DECompoundTableModel tableModel,
					   CompoundTableColorHandler colorHandler, CompoundListSelectionModel selectionModel) {
		mParentFrame = parentFrame;
		mParentPane = parentPane;
		mTableModel = tableModel;
		mColorHandler = colorHandler;

		mTable = new DETable(tableModel, null, selectionModel);
		mTable.getTableHeader().addMouseListener(this);
		mTable.addMouseListener(this);
		mTable.addMouseMotionListener(this);
		mTable.setColumnSelectionAllowed(true);

		mColorHandler.addColorListener(this);

		setBorder(BorderFactory.createEmptyBorder());
		getViewport().add(mTable, null);
		}

	public void cleanup() {
		mTable.cleanup();
		mColorHandler.removeColorListener(this);
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		}

	public void hitlistChanged(CompoundTableHitlistEvent e) {}

	public DEParentPane getParentPane() {
		return mParentPane;
		}

	public DECompoundTableModel getTableModel() {
		return mTableModel;
		}

	public boolean getTextWrapping(int column) {
		int modelColumn = mTable.convertTotalColumnIndexToView(column);
		if (modelColumn == -1)
			return false;
		TableCellRenderer renderer = mTable.getColumnModel().getColumn(modelColumn).getCellRenderer();
		return renderer instanceof MultiLineCellRenderer
			&& ((MultiLineCellRenderer)renderer).getLineWrap();
		}

	public void setTextWrapping(int column, boolean wrap) {
		int viewColumn = mTable.convertTotalColumnIndexToView(column);
		if (viewColumn != -1) {
			TableCellRenderer renderer = mTable.getColumnModel().getColumn(viewColumn).getCellRenderer();
			if (renderer instanceof MultiLineCellRenderer) {
				((MultiLineCellRenderer)renderer).setLineWrap(wrap);
				mTable.repaint();
				}
			}
		}

	public boolean isColumnVisible(int column) {
		return (mTable.convertTotalColumnIndexToView(column) != -1);
		}

	public void setColumnVisibility(int column, boolean b) {
		mTable.setColumnVisibility(column, b);
		}

	public void colorChanged(int column, int type, VisualizationColor color) {
		int viewColumn = mTable.convertTotalColumnIndexToView(column);
		if (viewColumn != -1) {
			TableCellRenderer renderer = mTable.getColumnModel().getColumn(viewColumn).getCellRenderer();
			if (renderer instanceof ColorizedCellRenderer) {
				((ColorizedCellRenderer)renderer).setColorHandler(color, type);
				mTable.repaint();
				}
			}
		}

	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == mTable.getTableHeader()
		 && e.getX() == mMouseX
		 && (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
			int column = mTable.getTableHeader().columnAtPoint(e.getPoint());
			boolean isShiftDown = ((e.getModifiers() & MouseEvent.SHIFT_MASK) != 0);
			if (column != -1)
				new DETaskSortRows(mParentFrame, mTableModel, mTable.convertTotalColumnIndexFromView(column), isShiftDown).defineAndRun();
			return;
			}

		if (e.getClickCount() == 2
		 && e.getSource() == mTable) {
			int column = mTable.convertTotalColumnIndexFromView(mTable.columnAtPoint(e.getPoint()));
			if (column != -1) {
				int row = mTable.rowAtPoint(e.getPoint());
				new DEDetailPopupMenu(mParentPane.getMainPane(), mTableModel.getRecord(row), null, this, null, column).actionPerformed(
						new ActionEvent(this, ActionEvent.ACTION_PERFORMED, DEDetailPopupMenu.EDIT_VALUE+mTableModel.getColumnTitleNoAlias(column)));
				}
			}
		}

	public void mousePressed(MouseEvent e) {
		if (handlePopupTrigger(e))
			return;

		mMouseX = e.getX();
		}

	public void mouseReleased(MouseEvent e) {
		if (handlePopupTrigger(e))
			return;
		}

	public void mouseEntered(MouseEvent e) {
		}

	public void mouseExited(MouseEvent e) {
		}

	private boolean handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (e.getSource() == mTable.getTableHeader()) {
				int column = mTable.convertTotalColumnIndexFromView(mTable.getTableHeader().columnAtPoint(e.getPoint()));
				if (column != -1) {
					JPopupMenu popup = new DETablePopupMenu(mParentFrame, mParentPane.getMainPane(), this, column);
					if (popup.getComponentCount() != 0)
						popup.show(mTable.getTableHeader(), e.getX(), e.getY());
					}
				}
			else if (e.getSource() == mTable) {
				int theRow = mTable.rowAtPoint(e.getPoint());
				if (theRow != -1) {
					int theColumn = mTable.convertTotalColumnIndexFromView(mTable.columnAtPoint(e.getPoint()));
					JPopupMenu popup = mDetailPopupProvider.createPopupMenu(mTableModel.getRecord(theRow), this, theColumn);
					if (popup != null)
						popup.show(mTable, e.getX(), e.getY());
					}
				}
			return true;
			}
		return false;
		}

	public void mouseDragged(MouseEvent e) {
		}

	public void mouseMoved(MouseEvent e) {
		if (e.getSource() == mTable)
			mTableModel.setHighlightedRow(mTable.rowAtPoint(e.getPoint()));
			}

	public void setDetailPopupProvider(DetailPopupProvider detailPopupProvider) {
		mDetailPopupProvider = detailPopupProvider;
		}

	public DETable getTable() {
		return mTable;
		}

	public CompoundTableColorHandler getColorHandler() {
		return mColorHandler;
		}

	public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
		Graphics2D g2 = (Graphics2D)g;
	 	g2.setColor(Color.black);
	 	int fontHeight = g2.getFontMetrics().getHeight();
	 	int fontDesent = g2.getFontMetrics().getDescent();

	 	//leave room for page number
	 	double pageHeight = pageFormat.getImageableHeight() - fontHeight;
	 	double pageWidth = pageFormat.getImageableWidth();
	 	double tableWidth = (double)mTable.getColumnModel().getTotalColumnWidth();
	 	double scale = 1;
	 	if (tableWidth >= pageWidth)
			scale = pageWidth / tableWidth;

	 	double headerHeightOnPage= mTable.getTableHeader().getHeight()*scale;
	 	double tableWidthOnPage = tableWidth*scale;

	 	double oneRowHeight = (mTable.getRowHeight() /* + mTable.getRowMargin()*/)*scale;
	 	int numRowsOnAPage = (int)((pageHeight-headerHeightOnPage) / oneRowHeight);
	 	double pageHeightForTable = oneRowHeight * numRowsOnAPage;
	 	int totalNumPages = (int)Math.ceil(((double)mTable.getRowCount()) / numRowsOnAPage);

	 	if(pageIndex >= totalNumPages)
			return NO_SUCH_PAGE;

	 	g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
	 	g2.drawString("Page: "+(pageIndex+1),
					  (int)pageWidth / 2-35,
					  (int)(pageHeight + fontHeight - fontDesent));

	 	g2.translate(0f,headerHeightOnPage);
	 	g2.translate(0f,-pageIndex*pageHeightForTable);

	 	//If this piece of the table is smaller
	 	//than the size available,
	 	//clip to the appropriate bounds.
	 	if (pageIndex + 1 == totalNumPages) {
			int lastRowPrinted = numRowsOnAPage * pageIndex;
			int numRowsLeft = mTable.getRowCount() - lastRowPrinted;
			g2.setClip(0, (int)(pageHeightForTable * pageIndex),
						  (int)Math.ceil(tableWidthOnPage),
						  (int)Math.ceil(oneRowHeight * numRowsLeft));
			}
	 	//else clip to the entire area available.
	 	else {
			g2.setClip(0, (int)(pageHeightForTable*pageIndex),
						  (int) Math.ceil(tableWidthOnPage),
						  (int) Math.ceil(pageHeightForTable));
			}

		g2.scale(scale,scale);
		mTable.paint(g2);
		g2.scale(1/scale,1/scale);
		g2.translate(0f,pageIndex*pageHeightForTable);
		g2.translate(0f, -headerHeightOnPage);
		g2.setClip(0, 0,
				   (int) Math.ceil(tableWidthOnPage),
				   (int)Math.ceil(headerHeightOnPage));
	 	g2.scale(scale, scale);
		mTable.getTableHeader().paint(g2);

	 	return Printable.PAGE_EXISTS;
		}
	}
