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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import com.actelion.research.gui.table.ChemistryCellRenderer;
import com.actelion.research.gui.table.ChemistryRenderPanel;

public class DetailTableCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 0x20061009;
    public static final Color TOGGLE_ROW_BACKGROUND = new Color(230, 235, 240); 

    private CompoundTableModel          mTableModel;
    private ChemistryCellRenderer       mChemistryRenderer;
	private MultiLineCellRenderer       mMultiLineRenderer;
	private CompoundTableColorHandler	mColorHandler;

    public DetailTableCellRenderer(CompoundTableModel tableModel) {
		super();
		mTableModel = tableModel;
    	}

    public void setColorHandler(CompoundTableColorHandler colorHandler) {
    	mColorHandler = colorHandler;
    	}

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row, int col) {
        if (mTableModel.getColumnCount() == 0)
            return null;

        int column = mTableModel.convertFromDisplayableColumnIndex(row);
	    if (mTableModel.getColumnSpecialType(column) != null) {
            if (mChemistryRenderer == null) {
                mChemistryRenderer = new ChemistryCellRenderer();
                mChemistryRenderer.setAlternatingRowBackground(TOGGLE_ROW_BACKGROUND);
                }

            return colorize((ChemistryRenderPanel)mChemistryRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col), column);
			}
        else {
            if (mMultiLineRenderer == null) {
                mMultiLineRenderer = new MultiLineCellRenderer();
                mMultiLineRenderer.setAlternatingRowBackground(TOGGLE_ROW_BACKGROUND);
                }

            return colorize((JTextArea)mMultiLineRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col), column);
    		}
        }

	private JComponent colorize(JComponent c, int column) {
		if (mColorHandler != null) {
	    	CompoundRecord record = mTableModel.getHighlightedRow();
	    	if (record != null) {
	    		if (mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.FOREGROUND)) {
	    			c.setForeground(mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.FOREGROUND).getDarkerColor(record));
	    			}

	    		if (mColorHandler.hasColorAssigned(column, CompoundTableColorHandler.BACKGROUND)) {
		    		// Quaqua does not use the defined background color if CellRenderer is translucent
		    		if (UIManager.getLookAndFeel().getName().startsWith("Quaqua"))
		    			c.setOpaque(true);
	
	    			c.setBackground(mColorHandler.getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND).getLighterColor(record));
	    			}
	    		}
			}
		return c;
		}
	}
