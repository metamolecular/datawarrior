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

package com.actelion.research.table;

import javax.swing.*;
import javax.swing.event.*;

public class JDetailTable extends JTable implements TableModelListener {
    private static final long serialVersionUID = 0x20061009;

    private static final int cTextRowHeight = 16;
	private static final int cMultiLineRowHeight = 32;
	private static final int cSpecialRowHeight = 48;

	private DetailTableModel	mDetailModel;

	public JDetailTable(DetailTableModel detailTableModel) {
		super(detailTableModel);

		mDetailModel = detailTableModel;

		getColumnModel().getColumn(1).setCellRenderer(
					new DetailTableCellRenderer(detailTableModel.getParentModel()));

		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(false);

		new TableRowHeightAdjuster(this);
		}

    public void setColorHandler(CompoundTableColorHandler colorHandler) {
    	((DetailTableCellRenderer)getColumnModel().getColumn(1).getCellRenderer()).setColorHandler(colorHandler);
    	}

	public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);

		if (mDetailModel != null) {
			CompoundTableModel tableModel = mDetailModel.getParentModel();
			for (int row=0; row<tableModel.getColumnCount(); row++) {
                int column = tableModel.convertFromDisplayableColumnIndex(row);
                String specialType = tableModel.getColumnSpecialType(column);
                if (specialType != null) {
					if (getRowHeight(row) == cTextRowHeight
					 || getRowHeight(row) == cMultiLineRowHeight)
						setRowHeight(row, cSpecialRowHeight);
					}
				else if (tableModel.isMultiLineColumn(row)) {
					if (getRowHeight(row) == cTextRowHeight
					 || getRowHeight(row) == cSpecialRowHeight)
						setRowHeight(row, cMultiLineRowHeight);
					}
				else {
					if (getRowHeight(row) == cSpecialRowHeight
					 || getRowHeight(row) == cMultiLineRowHeight)
						setRowHeight(row, cTextRowHeight);
					}
				}
			}
		}

	}
