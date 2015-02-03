package com.actelion.research.gui.table;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import com.actelion.research.chem.*;
import com.actelion.research.chem.reaction.ReactionEncoder;

public class ChemistryCellRenderer implements ListCellRenderer,TableCellRenderer {
    private ChemistryRenderPanel    mRenderPanel;
    private Color                   mAlternatingRowBackground;
    private boolean					mIsEnabled;

    public ChemistryCellRenderer() {
        mRenderPanel = new ChemistryRenderPanel();
        mRenderPanel.setOpaque(false);
        }

    public void setAlternatingRowBackground(Color bg) {
        mAlternatingRowBackground = bg;
        }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
    	mIsEnabled = list.isEnabled();
    	return getCellRendererComponent(value, isSelected, hasFocus, index);
    	}

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row, int col) {
    	mIsEnabled = table.isEnabled();
    	return getCellRendererComponent(value, isSelected, hasFocus, row);
        }

    private Component getCellRendererComponent(Object value, boolean isSelected, boolean hasFocus, int row) {
        if (value == null) {
            mRenderPanel.setChemistry(null);
            }
        else if (value instanceof String) {
            String s = (String)value;
            if (s.length() == 0) {
                mRenderPanel.setChemistry(null);
                }
            else if (s.indexOf(ReactionEncoder.PRODUCT_IDENTIFIER) != -1) {
                mRenderPanel.setChemistry(ReactionEncoder.decode((String)value, true /*, mRenderPanel.getGraphics() */ ));
                }
            else {
                int index = s.indexOf('\n');
                if (index == -1) {
                    index = s.indexOf(' ');
                    if (index == -1)
                        mRenderPanel.setChemistry(new IDCodeParser(true).getCompactMolecule(s));
                    else
                        mRenderPanel.setChemistry(new IDCodeParser(true).getCompactMolecule(
                                                     s.substring(0, index),
                                                     s.substring(index+1)));
                    }
                else {
                	StereoMolecule mol = new StereoMolecule();
                    new IDCodeParser(true).parse(mol, s.substring(0, index));
                    do {
                        s = s.substring(index+1);
                        index = s.indexOf('\n');
                        mol.addMolecule(new IDCodeParser(true).getCompactMolecule(index == -1 ? s : s.substring(0, index)));
                        } while (index != -1);
                    new CoordinateInventor().invent(mol);
                    mRenderPanel.setChemistry(mol);
                    }
                }
            }
        else {
            mRenderPanel.setChemistry(value);
            }

        if (mAlternatingRowBackground != null)
            mRenderPanel.setAlternatingBackground((row & 1) == 1 ? mAlternatingRowBackground : null);
        mRenderPanel.setSelected(isSelected);
        mRenderPanel.setFocus(hasFocus);
        mRenderPanel.setForeground(mIsEnabled ? null : Color.GRAY);
        return mRenderPanel;
        }
	}
