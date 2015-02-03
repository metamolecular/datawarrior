package com.actelion.research.gui.table;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class JTableWithRowNumbers extends JTable implements TableModelListener {
    private static final long serialVersionUID = 0x20060906;

    private RowNumberTable	mRowNumberTable = null;
	private JScrollPane		mScrollPane = null;
	private Cursor			mResizeCursor,mDefaultCursor;
	private boolean			mIsResizing;
	private int				mResizingRowY,mResizingRowHeight;

	public JTableWithRowNumbers() {
		super();
		initialize();
		}

    public JTableWithRowNumbers(int numRows, int numColumns) {
		super(numRows, numColumns);
		initialize();
		}

    public JTableWithRowNumbers(final Object[][] rowData, final Object[] columnNames) {
		super(rowData, columnNames);
		initialize();
		}

    public JTableWithRowNumbers(TableModel dm) {
		super(dm);
		initialize();
		}

    public JTableWithRowNumbers(TableModel dm, TableColumnModel cm) {
		super(dm, cm);
		initialize();
		}

    public JTableWithRowNumbers(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
		super(dm, cm, sm);
		initialize();
		mRowNumberTable.setSelectionModel(sm);
		}

    @SuppressWarnings("unchecked")
	public JTableWithRowNumbers(final Vector rowData, final Vector columnNames) {
		super(rowData, columnNames);
		initialize();
		}

	private void initialize() {
		mRowNumberTable = new RowNumberTable();
		setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		mDefaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		mResizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
		}

	public void paint(Graphics g) {
		super.paint(g);

		if (mIsResizing) {
			int tableWidth = getWidth();
			g.setColor(Color.black);
			g.drawLine(0, mResizingRowY, tableWidth, mResizingRowY);
			g.drawLine(0, mResizingRowY+mResizingRowHeight, tableWidth, mResizingRowY+mResizingRowHeight);
			}
		}

	public void setRowHeight(int height) {
		super.setRowHeight(height);
		if (mRowNumberTable != null)
			mRowNumberTable.setRowHeight(height);
		}

	public void setRowHeight(int row, int height) {
		super.setRowHeight(row, height);
		if (mRowNumberTable != null)
			mRowNumberTable.setRowHeight(row, height);
		}

	public void addNotify() {
		super.addNotify();

		if (getParent().getParent() instanceof JScrollPane) {
			mScrollPane = (JScrollPane) getParent().getParent();
			mScrollPane.setRowHeaderView(mRowNumberTable);

			optimizeColumnWidth();

	// make the background of the scrollpane match that of the table.
//			pane.getViewport().setBackground(getBackground());
//			pane.getColumnHeader().setBackground(getBackground());
//			pane.getRowHeader().setBackground(getBackground());
			}
		}

	private void optimizeColumnWidth() {
		if (mScrollPane != null) {
			JViewport viewport = mScrollPane.getRowHeader();
			Dimension size = viewport.getPreferredSize();
			size.width = 8 + 7 * Integer.toString(getRowCount()).length();
			viewport.setPreferredSize(size);
			}
		}

	public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);

		if (mRowNumberTable != null) {
			optimizeColumnWidth();
			mRowNumberTable.invalidate();
			mRowNumberTable.repaint();
			}
		}

	public void rowNumberClicked(int row) {	// overwrite this if you want specific effects
		}

	public void setSelectionModel(ListSelectionModel selectionModel) {
		super.setSelectionModel(selectionModel);
		if (mRowNumberTable != null)
			mRowNumberTable.setSelectionModel(selectionModel);
		}

	// Inner class used to display the row numbers on the left side of the table. Order
	// is considered important enough on this screen to re-enforce it with a visual.

	private class RowNumberTable extends JTable implements ListSelectionListener,MouseListener,MouseMotionListener {
        private static final long serialVersionUID = 0x20060906;

        private static final int cResizeTolerance = 2;

		private boolean mFullRowSelection;
		private int mDragStartY,mDragStartRow,mDragStartRowHeight;

		public RowNumberTable() {
			super();

			setAutoCreateColumnsFromModel(false);
			setModel(new RowNumberTableModel());
			JTableWithRowNumbers.this.setRowHeight(JTableWithRowNumbers.this.getRowHeight());

			setColumnSelectionAllowed(false);
			setRowSelectionAllowed(true);

			TableColumn column = new TableColumn();
			column.setResizable(false);
			column.setCellRenderer(new RowNumberRenderer());
			addColumn(column);

			setBackground(Color.lightGray);
			addMouseListener(this);
			addMouseMotionListener(this);
			}

//		public boolean isFocusTraversable() {
//			return false;
//			}

		public void mouseClicked(MouseEvent e) {
			if (getCursor() != mResizeCursor)
				rowNumberClicked(rowAtPoint(e.getPoint()));
			}

		public void mousePressed(MouseEvent e) {
			mIsResizing = (getCursor() == mResizeCursor);
			mFullRowSelection = !mIsResizing;
			if (mIsResizing) {
				Point p = e.getPoint();
				p.y -= cResizeTolerance;
				int row = rowAtPoint(p);
				mResizingRowY = getCellRect(row, 0, false).y-1;
				mDragStartY = e.getY();
				mDragStartRow = row;
				mDragStartRowHeight = mResizingRowHeight = getRowHeight(row);
				}
			}

		public void mouseReleased(MouseEvent e) {
			if (mIsResizing) {
				mIsResizing = false;
				if (mDragStartRowHeight != mResizingRowHeight)
					JTableWithRowNumbers.this.setRowHeight(mResizingRowHeight);
				}
			}

		public void mouseEntered(MouseEvent e) {
			}

		public void mouseExited(MouseEvent e) {
			mFullRowSelection = false;
			if (!mIsResizing)
				setCursor(mDefaultCursor);
			}

		public void mouseMoved(MouseEvent e) {
			int row = rowAtPoint(e.getPoint());
			Rectangle cellRect = getCellRect(row, 0, false);
			int y = e.getY() - cellRect.y;
			if ((y < cResizeTolerance && row != 0) || (y > cellRect.height-cResizeTolerance) && row != this.getRowCount()-1) {
				setCursor(mResizeCursor);
				this.setSelectionModel(new DefaultListSelectionModel());
				}
			else {
				setCursor(mDefaultCursor);
				this.setSelectionModel(JTableWithRowNumbers.this.getSelectionModel());
				}
			}

		public void mouseDragged(MouseEvent e) {
			if (mIsResizing) {
				int rowHeight = Math.max(16, mDragStartRowHeight + e.getY() - mDragStartY);
				if (mResizingRowHeight != rowHeight) {
					mResizingRowHeight = rowHeight;
					JTableWithRowNumbers.this.setRowHeight(mDragStartRow, mResizingRowHeight);
					}
				}
			}

		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				int[] selectedRow = getSelectedRows();

				if (selectedRow.length > 0) {
					JTableWithRowNumbers theTable = JTableWithRowNumbers.this;

					if (mFullRowSelection && theTable.getColumnCount() != 0)
						theTable.setColumnSelectionInterval(0, theTable.getColumnCount()-1);
					}
				}

            repaint();
			}
		}

 	private class RowNumberTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 0x20060906;

        public int getColumnCount() {
			return 1;
			}

		public int getRowCount() {
			if (getModel() != null)
				return getModel().getRowCount();

			return 0;
			}

		public Object getValueAt(int r, int c) {
			return ""+(r+1);
			}
		}

	private class RowNumberRenderer extends JPanel implements TableCellRenderer {
        private static final long serialVersionUID = 0x20060906;

        private String mRowHeader;

		public RowNumberRenderer() {
			super();
			}

		public void paint(Graphics g) {
			Dimension size = getSize();
			g.setColor(Color.lightGray.brighter());
			g.drawLine(0, 0, size.width, 0);
			g.drawLine(0, 0, 0, size.height);
			g.setColor(Color.black);
			g.drawString(mRowHeader, 4, size.height/2+5);
			}

		public Component getTableCellRendererComponent(JTable table, Object value,
								boolean isSelected, boolean hasFocus, int row, int col) {
			mRowHeader = ""+(row+1);
			return this;
			}
		}
	}