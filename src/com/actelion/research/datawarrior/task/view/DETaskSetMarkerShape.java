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
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.JVisualization3D;
import com.actelion.research.table.view.VisualizationPanel;


public class DETaskSetMarkerShape extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Marker Shape";

	private static final String PROPERTY_COLUMN = "column";
	
	private static Properties sRecentConfiguration;
	
	private JComboBox		mComboBox;
	
	public DETaskSetMarkerShape(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
		super(owner, mainPane, view);
		}
	
	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Marker shapes can only be applied to 2D- or 3D-Views.";
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public JComponent createInnerDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		mComboBox = new JComboBox();
		mComboBox.addItem(getTableModel().getColumnTitleExtended(-1));
		int maxShapeCount = (hasInteractiveView()) ? getVisualization().getAvailableShapeCount()
				: Math.max(JVisualization2D.cAvailableShapeCount, JVisualization3D.cAvailableShapeCount);
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (getTableModel().isColumnTypeCategory(i) && getTableModel().getCategoryCount(i) <= maxShapeCount)
				mComboBox.addItem(getTableModel().getColumnTitle(i));
		for (int i=0; i<getTableModel().getHitlistHandler().getHitlistCount(); i++)
			mComboBox.addItem(getTableModel().getColumnTitleExtended(CompoundTableHitlistHandler.getColumnFromHitlist(i)));
		mComboBox.setEditable(!hasInteractiveView());
		mComboBox.addItemListener(this);
		cp.add(new JLabel("Shape by:"), "1,1");
		cp.add(mComboBox, "3,1");

		return cp;
	    }

	@Override
	public void setDialogToDefault() {
        mComboBox.setSelectedIndex(0);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode);
		int column = getTableModel().findColumn(columnName);
		mComboBox.setSelectedItem(!hasInteractiveView() && column == -1 ? columnName : getTableModel().getColumnTitleExtended(column));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBox.getSelectedItem()));
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias(getVisualization().getMarkerShapeColumn()));
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String columnName = configuration.getProperty(PROPERTY_COLUMN);
			if (!CompoundTableModel.cColumnUnassignedCode.equals(columnName)) {
				int column = getTableModel().findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				int maxShapeCount = ((VisualizationPanel)view).getVisualization().getAvailableShapeCount();
				if (!getTableModel().isColumnTypeCategory(column) || !(getTableModel().getCategoryCount(column) <= maxShapeCount)) {
					showErrorMessage("Column '"+columnName+"' does not contain categories or contains to many categories.");
					return false;
					}
				}
			}

		return true;
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
		int column = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode));
		((VisualizationPanel)view).getVisualization().setMarkerShapeColumn(column);
		}
	}
