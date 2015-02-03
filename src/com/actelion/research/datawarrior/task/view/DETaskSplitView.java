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

package com.actelion.research.datawarrior.task.view;

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;


public class DETaskSplitView extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Split View By Categories";

	private static final String PROPERTY_COLUMN1 = "column1";
	private static final String PROPERTY_COLUMN2 = "column2";
	
	private static Properties sRecentConfiguration;
	
	private JComboBox	mComboBoxColumn1,mComboBoxColumn2;
	
	public DETaskSplitView(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
		super(owner, mainPane, view);
		}
	
	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel2D) ? null : "Only 2D-Views can be split by categories.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		mComboBoxColumn1 = new JComboBox();
        mComboBoxColumn2 = new JComboBox();
        mComboBoxColumn1.addItem(getTableModel().getColumnTitleExtended(-1));
        mComboBoxColumn2.addItem(getTableModel().getColumnTitleExtended(-1));
        for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
            if (columnQualifies(column)) {
                mComboBoxColumn1.addItem(getTableModel().getColumnTitle(column));
                mComboBoxColumn2.addItem(getTableModel().getColumnTitle(column));
                }
            }
		for (int i=0; i<getTableModel().getHitlistHandler().getHitlistCount(); i++) {
			mComboBoxColumn1.addItem(getTableModel().getColumnTitleExtended(CompoundTableHitlistHandler.getColumnFromHitlist(i)));
			mComboBoxColumn2.addItem(getTableModel().getColumnTitleExtended(CompoundTableHitlistHandler.getColumnFromHitlist(i)));
			}
		if (!hasInteractiveView()) {
	        mComboBoxColumn1.setEditable(true);
	        mComboBoxColumn2.setEditable(true);
			}
        mComboBoxColumn1.addItemListener(this);
        mComboBoxColumn2.addItemListener(this);

        p.add(new JLabel("1st Column:"), "1,1");
        p.add(mComboBoxColumn1, "3,1");
        p.add(new JLabel("2nd Column:"), "1,3");
        p.add(mComboBoxColumn2, "3,3");

        return p;
		}

	private boolean columnQualifies(int column) {
        return getTableModel().isColumnTypeCategory(column)
            && getTableModel().getCategoryCount(column) <= 120;
		}

	@Override
	public void setDialogToDefault() {
		mComboBoxColumn1.setSelectedIndex(0);
		mComboBoxColumn2.setSelectedIndex(0);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName1 = configuration.getProperty(PROPERTY_COLUMN1, CompoundTableModel.cColumnUnassignedCode);
		String columnName2 = configuration.getProperty(PROPERTY_COLUMN2, CompoundTableModel.cColumnUnassignedCode);
		int column1 = getTableModel().findColumn(columnName1);
		int column2 = getTableModel().findColumn(columnName2);
		mComboBoxColumn1.setSelectedItem(!hasInteractiveView() && column1 == -1 ? columnName1 : getTableModel().getColumnTitleExtended(column1));
		mComboBoxColumn2.setSelectedItem(!hasInteractiveView() && column2 == -1 ? columnName2 : getTableModel().getColumnTitleExtended(column2));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN1, ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn1.getSelectedItem()));
		configuration.setProperty(PROPERTY_COLUMN2, ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn2.getSelectedItem()));
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		int[] column = getVisualization().getSplittingColumns();
		configuration.setProperty(PROPERTY_COLUMN1, ""+getTableModel().getColumnTitleNoAlias(column[0]));
		configuration.setProperty(PROPERTY_COLUMN2, ""+getTableModel().getColumnTitleNoAlias(column[1]));
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		int multiplicity = getMultiplicity(configuration.getProperty(PROPERTY_COLUMN1))
						 * getMultiplicity(configuration.getProperty(PROPERTY_COLUMN2));
		if (multiplicity == 0)
			return false;
		if (multiplicity > 120) {
			showErrorMessage("Selected splitting column(s) would exceed the limit of 120 individual views.");
			return false;
			}

		return true;
		}

	private int getMultiplicity(String columnName) {
		if (!CompoundTableModel.cColumnUnassignedCode.equals(columnName)) {
			int column = getTableModel().findColumn(columnName);
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' not found.");
				return 0;
				}
			if (!columnQualifies(column)) {
				showErrorMessage("Column '"+columnName+"' does not contain categories or contains to many categories.");
				return 0;
				}
			return CompoundTableHitlistHandler.isHitlistColumn(column) ? 2 : getTableModel().getCategoryCount(column);
			}
		return 1;
		}

	@Override
	public void enableItems() {
		}

	@Override
	public Properties getRecentConfigurationLocal() {
		return sRecentConfiguration;
		}
	
	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		int column1 = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN1, CompoundTableModel.cColumnUnassignedCode));
		int column2 = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN2, CompoundTableModel.cColumnUnassignedCode));
        getVisualization().setSplittingColumns(column1, column2);
		}
	}
