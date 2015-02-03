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

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.UIManager;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.table.ChemistryCellRenderer;
import com.actelion.research.gui.table.ChemistryRenderPanel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;

public class CompoundTableChemistryCellRenderer extends ChemistryCellRenderer implements ColorizedCellRenderer {
    private VisualizationColor mForegroundColor,mBackgroundColor;

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row, int col) {
        if (value != null && value instanceof String) {
            String s = (String)value;
            if (s.length() != 0 && s.indexOf(' ') == -1 && s.indexOf('\n') == -1) {
                CompoundTableModel tableModel = (CompoundTableModel)table.getModel();
                int idcodeColumn = tableModel.convertFromDisplayableColumnIndex(table.convertColumnIndexToModel(col));
                int coordsColumn = tableModel.getChildColumn(idcodeColumn, CompoundTableModel.cColumnType2DCoordinates);
                CompoundRecord record = tableModel.getRecord(row);
                byte[] idcode = (byte[])record.getData(idcodeColumn);
                byte[] coords = (coordsColumn == -1) ? null : (byte[])record.getData(coordsColumn);
                StereoMolecule mol = new StereoMolecule();
                new IDCodeParser(true).parse(mol, idcode, coords);
                tableModel.colorizeAtoms(record, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_ALL, mol);
                value = mol;
                }
            }
        ChemistryRenderPanel renderPanel = (ChemistryRenderPanel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

		if (!isSelected) {
            if (mForegroundColor != null && mForegroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	renderPanel.setForeground(mForegroundColor.getDarkerColor(record));
            	}

            // Quaqua does not use the defined background color if CellRenderer is translucent
        	if (UIManager.getLookAndFeel().getName().startsWith("Quaqua"))
        		renderPanel.setOpaque(mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned);

            if (mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	renderPanel.setBackground(mBackgroundColor.getLighterColor(record));
            	}
			}

        return renderPanel;
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
	}
