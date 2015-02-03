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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.view.DETaskSetCategoryCustomOrder;
import com.actelion.research.datawarrior.task.view.DETaskSetTextBackgroundColor;
import com.actelion.research.datawarrior.task.view.DETaskSetTextColor;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.filter.JCategoryFilterPanel;

public class DETablePopupMenu extends JPopupMenu implements ActionListener {
    private static final long serialVersionUID = 0x20060904;

    private static final String[] ROUND_OPTION = {"<not rounded>", "1", "2", "3", "4", "5", "6", "7"};

    private CompoundTableModel	mTableModel;
	private int					mColumn;
	private Frame    			mParentFrame;
	private DEMainPane			mMainPane;
	private DETableView         mTableView;

	public DETablePopupMenu(Frame parent, DEMainPane mainPane, DETableView tableView, int column) {
		super();

        mParentFrame = parent;
        mMainPane = mainPane;
        mTableView = tableView;
		mTableModel = tableView.getTableModel();
		mColumn = column;

		addItem("Set Column Alias...");
		addItem("Set Column Description...");

		if (DETaskSetCategoryCustomOrder.columnQualifies(mTableModel, column)) {
	        addSeparator();
			addItem("Set Category Custom Order...");
			}

        addSeparator();
        String specialType = mTableModel.getColumnSpecialType(column);
        if (specialType != null) {
            if (specialType.equals(CompoundTableModel.cColumnTypeIDCode)) {
    			JMenu filterMenu = new JMenu("New Structure Filter");
                add(filterMenu);
                addItem(filterMenu, "Single Structure", "New Structure Filter");
                addItem(filterMenu, "Substructure List", "New SSS-List Filter");
                addItem(filterMenu, "Similar Structure List", "New Sim-List Filter");
            	}
            if (specialType.equals(CompoundTableModel.cColumnTypeRXNCode)) {
                addItem("New Reaction Filter");
            	}
            }
        else {
            if (mTableModel.isColumnTypeString(column) && specialType == null)
                addItem("New Text Filter");

            if (mTableModel.isColumnTypeDouble(column)
             && mTableModel.hasNumericalVariance(column))
                addItem("New Slider Filter");

            if (mTableModel.isColumnTypeCategory(column)
       		 && mTableModel.getCategoryCount(column) < JCategoryFilterPanel.cMaxCheckboxCount)
                addItem("New Category Filter");

            if (mTableModel.isColumnTypeRangeCategory(column))
                addItem("New Slider Filter");
            }

		if (mTableModel.isColumnTypeDouble(column)) {
			addSeparator();
			JMenu summaryModeMenu = new JMenu("Show Multiple Values As");
            add(summaryModeMenu);
            for (int i=0; i<CompoundTableConstants.cSummaryModeText.length; i++) {
                JRadioButtonMenuItem summaryItem = new JRadioButtonMenuItem(CompoundTableConstants.cSummaryModeText[i],
                        mTableModel.getColumnSummaryMode(mColumn) == i);
                summaryItem.addActionListener(this);
                summaryModeMenu.add(summaryItem);
                }
            summaryModeMenu.addSeparator();
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Hide Value Count");
            menuItem.setState(mTableModel.isColumnSummaryCountHidden(column));
            menuItem.addActionListener(this);
            summaryModeMenu.add(menuItem);

            addItem("Show Rounded Values...");

            if (mTableModel.isColumnWithModifiers(column)) {
                addCheckBoxMenuItem("Exclude Values With Modifiers",
                                    mTableModel.getColumnModifierExclusion(column));
                }
		    }

		if (mTableModel.getColumnSpecialType(column) == null) {
            addSeparator();
            addCheckBoxMenuItem("Wrap Text", mTableView.getTextWrapping(column));
		    }

		addSeparator();
        if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
			JMenu hiliteModeMenu = new JMenu("Highlight Structure By");
            add(hiliteModeMenu);
            for (int i=0; i<CompoundTableConstants.cStructureHiliteModeText.length; i++) {
                JRadioButtonMenuItem hiliteItem = new JRadioButtonMenuItem(CompoundTableConstants.cStructureHiliteModeText[i],
                        mTableModel.getStructureHiliteMode(mColumn) == i);
                hiliteItem.addActionListener(this);
                hiliteModeMenu.add(hiliteItem);
                }
        	addItem("Set Structure Color...");
        	}
        else {
        	addItem("Set Text Color...");
        	}
		addItem("Set Background Color...");

        addSeparator();
		addItem("Delete '"+mTableModel.getColumnTitle(column)+"'");

		addSeparator();
		addItem("Hide '"+mTableModel.getColumnTitle(column)+"'");

        for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
            if (mTableModel.isColumnDisplayable(i)
             && !mTableView.isColumnVisible(i)) {
                addItem("Show '"+mTableModel.getColumnTitle(i)+"'");
                }
            }
        }

	private void addItem(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(this);
        add(menuItem);
	    }

	private void addItem(JMenuItem menu, String text, String actionCommand) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(this);
        menu.add(menuItem);
	    }

	private void addCheckBoxMenuItem(String text, boolean state) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text);
        menuItem.setState(state);
        menuItem.addActionListener(this);
        add(menuItem);
	    }

	public void actionPerformed(ActionEvent e) {
	    for (int summaryMode=0; summaryMode<CompoundTableConstants.cSummaryModeText.length; summaryMode++) {
	        if (e.getActionCommand().equals(CompoundTableConstants.cSummaryModeText[summaryMode])) {
                mTableModel.setColumnSummaryMode(mColumn, summaryMode);
	            return;
	            }
	        }
	    for (int hiliteMode=0; hiliteMode<CompoundTableConstants.cStructureHiliteModeText.length; hiliteMode++) {
	        if (e.getActionCommand().equals(CompoundTableConstants.cStructureHiliteModeText[hiliteMode])) {
                mTableModel.setStructureHiliteMode(mColumn, hiliteMode);
	            return;
	            }
	        }
		if (e.getActionCommand().equals("Set Column Alias...")) {
			String alias = (String)JOptionPane.showInputDialog(
					mParentFrame,
					"Column Alias to be used for '"+mTableModel.getColumnTitleNoAlias(mColumn)+"'",
					"Set Column Alias",
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					mTableModel.getColumnTitle(mColumn));
			if (alias != null)	// if not canceled
			    mTableModel.setColumnAlias((alias.length() == 0
			                             || alias.equalsIgnoreCase(mTableModel.getColumnTitleNoAlias(mColumn))) ?
			                    null : alias, mColumn);
			}
		if (e.getActionCommand().equals("Set Column Description...")) {
			String description = (String)JOptionPane.showInputDialog(
					mParentFrame,
					"Column Description for '"+mTableModel.getColumnTitle(mColumn)+"'",
					"Set Column Description",
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					mTableModel.getColumnDescription(mColumn));
			if (description != null)	// if not canceled
			    mTableModel.setColumnDescription(description, mColumn);
			}
		else if (e.getActionCommand().equals("Set Category Custom Order...")) {
			new DETaskSetCategoryCustomOrder(mParentFrame, mTableModel, mColumn).defineAndRun();
			}
        else if (e.getActionCommand().startsWith("New ") && e.getActionCommand().endsWith(" Filter")) {
            try {
                DEPruningPanel pruningPanel = mTableView.getParentPane().getPruningPanel();
                if (e.getActionCommand().equals("New Structure Filter")) {
                    addDefaultDescriptor(mColumn);
                    pruningPanel.addStructureFilter(mTableModel, mColumn, null);
                    }
                else if (e.getActionCommand().equals("New SSS-List Filter")) {
                    addDefaultDescriptor(mColumn);
                    pruningPanel.addStructureListFilter(mTableModel, mColumn, true);
                    }
                else if (e.getActionCommand().equals("New Sim-List Filter")) {
                    addDefaultDescriptor(mColumn);
                    pruningPanel.addStructureListFilter(mTableModel, mColumn, false);
                    }
                else if (e.getActionCommand().equals("New Reaction Filter")) {
                    addDefaultDescriptor(mColumn);
                    pruningPanel.addReactionFilter(mTableModel, mColumn, null);
                    }
                else if (e.getActionCommand().equals("New Text Filter")) {
                    pruningPanel.addStringFilter(mTableModel, mColumn);
                    }
                else if (e.getActionCommand().equals("New Slider Filter")) {
                    pruningPanel.addDoubleFilter(mTableModel, mColumn);
                    }
                else if (e.getActionCommand().equals("New Category Filter")) {
                    pruningPanel.addCategoryFilter(mTableModel, mColumn);
                    }
                }
            catch (DEPruningPanel.FilterException fpe) {
                JOptionPane.showMessageDialog(mParentFrame, fpe.getMessage());
                }
            }
        else if (e.getActionCommand().equals("Hide Value Count")) {
            mTableModel.setColumnSummaryCountHidden(mColumn, !mTableModel.isColumnSummaryCountHidden(mColumn));
        	}
        else if (e.getActionCommand().equals("Show Rounded Values...")) {
            int oldDigits = mTableModel.getColumnSignificantDigits(mColumn);
            String selection = (String)JOptionPane.showInputDialog(
                    mParentFrame,
                    "Number of significant digits:",
                    "Display Rounded Value",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    ROUND_OPTION,
                    "" + ROUND_OPTION[oldDigits]);
            if (selection != null) {// if not cancelled
                int newDigits = 0;
                while (!selection.equals(ROUND_OPTION[newDigits]))
                    newDigits++;

                if (newDigits != oldDigits)  // if changed
                    mTableModel.setColumnSignificantDigits(mColumn, newDigits);
                }
            }
        else if (e.getActionCommand().equals("Exclude Values With Modifiers")) {
            mTableModel.setColumnModifierExclusion(mColumn, !mTableModel.getColumnModifierExclusion(mColumn));
            }
        else if (e.getActionCommand().equals("Set Text Color...")
       		  || e.getActionCommand().equals("Set Structure Color...")) {
        	new DETaskSetTextColor(mParentFrame, mMainPane, mTableView, mColumn).defineAndRun();
        	}
        else if (e.getActionCommand().equals("Set Background Color...")) {
        	new DETaskSetTextBackgroundColor(mParentFrame, mMainPane, mTableView, mColumn).defineAndRun();
        	}
        else if (e.getActionCommand().equals("Wrap Text")) {
            mTableView.setTextWrapping(mColumn, !mTableView.getTextWrapping(mColumn));
            }
        else if (e.getActionCommand().startsWith("Hide '")) {
            mTableView.setColumnVisibility(mColumn, false);
            }
        else if (e.getActionCommand().startsWith("Show '")) {
            mTableView.setColumnVisibility(mTableModel.findColumn(e.getActionCommand().substring(6, e.getActionCommand().length()-1)), true);
            }
		else if (e.getActionCommand().startsWith("Delete '")) {
	        int doDelete = JOptionPane.showConfirmDialog(this,
                    "Do you really want to delete the column '"+mTableModel.getColumnTitle(mColumn)+"'?",
                    "Delete Column?",
                    JOptionPane.OK_CANCEL_OPTION);
	        if (doDelete == JOptionPane.OK_OPTION) {
	        	boolean[] removeColumn = new boolean[mTableModel.getTotalColumnCount()];
	        	removeColumn[mColumn] = true;
	        	mTableModel.removeColumns(removeColumn, 1);
	        	}
			}
		}

    private void addDefaultDescriptor(int column) {
        for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
            if (mTableModel.getParentColumn(i) == column
             && mTableModel.isDescriptorColumn(i))
                return;

        if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column)))
            mTableModel.createDescriptorColumn(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
        else if (CompoundTableModel.cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column)))
            mTableModel.createDescriptorColumn(column, DescriptorConstants.DESCRIPTOR_ReactionIndex.shortName);
        }
    }
