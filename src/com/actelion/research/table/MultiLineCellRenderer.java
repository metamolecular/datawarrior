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

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;

public class MultiLineCellRenderer extends JTextArea implements ColorizedCellRenderer,TableCellRenderer {
    static final long serialVersionUID = 0x20070312;

    private Color mAlternatingRowBackground;
    private VisualizationColor mForegroundColor,mBackgroundColor;

    public MultiLineCellRenderer() {
		setLineWrap(true);
		setWrapStyleWord(true);
		setOpaque(false);
		}

    public void setAlternatingRowBackground(Color bg) {
        mAlternatingRowBackground = bg;
        }

	public void setColorHandler(VisualizationColor vc, int type) {
		switch (type) {
		case CompoundTableColorHandler.FOREGROUND:
	    	mForegroundColor = vc;
	    	break;
		case CompoundTableColorHandler.BACKGROUND:
	    	mBackgroundColor = vc;
	    	break;
			}
		}

    public Component getTableCellRendererComponent(JTable table, Object value,
							boolean isSelected, boolean hasFocus, int row, int column) {
		if (isSelected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
			}
		else {
            if (mForegroundColor != null && mForegroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	setForeground(mForegroundColor.getDarkerColor(record));
            	}
            else
            	setForeground(UIManager.getColor("Table.foreground"));

            boolean isQuaQuaLaF = UIManager.getLookAndFeel().getName().startsWith("Quaqua");

            // Quaqua does not use the defined background color if CellRenderer is translucent
        	if (isQuaQuaLaF)
        		setOpaque(mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned);

            if (mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	setBackground(mBackgroundColor.getLighterColor(record));
            	}
            else {
            	if (!isQuaQuaLaF) {	// simulate the quaqua table style "striped"
	            	if (mAlternatingRowBackground != null && (row & 1) == 1)
	            		setBackground(mAlternatingRowBackground);
	            	else
	            		setBackground(UIManager.getColor("Table.background"));
            		}
            	}
			}

		setFont(table.getFont());
		if (hasFocus) {
			setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
			if (table.isCellEditable(row, column)) {
				setForeground( UIManager.getColor("Table.focusCellForeground") );
				setBackground( UIManager.getColor("Table.focusCellBackground") );
				}
			}
		else {
			setBorder(new EmptyBorder(1, 2, 1, 2));
			}

		setText((value == null) ? "" : value.toString());
		return this;
		}
	}
