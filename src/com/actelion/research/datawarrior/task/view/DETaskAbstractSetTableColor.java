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

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationColor;

public abstract class DETaskAbstractSetTableColor extends DETaskAbstractSetColor {
	private static final String PROPERTY_COLUMN = "column";

    private static Properties sRecentConfiguration;

    private int			mDefaultColumn,mColorType;
    private JComboBox	mComboBoxColumn;

	public DETaskAbstractSetTableColor(Frame owner,
									   DEMainPane mainPane,
									   DETableView view,
									   int defaultColumn,
									   int colorType) {
		super(owner, mainPane, view,
				(colorType == CompoundTableColorHandler.FOREGROUND) ? "Set Text/Structure Color" : "Set Table Cell Background Color");
		mDefaultColumn = defaultColumn;
		mColorType = colorType;

		super.initialize();	// this is a hack to run initialize() after setting mVisualizationColor.
		}

	@Override
	protected void initialize() {}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view) {
		return ((DETableView)view).getColorHandler().getVisualizationColor(mDefaultColumn, mColorType);
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof DETableView) ? null : "Text color and background can only be set on table views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel p = (JPanel)super.createInnerDialogContent();

		int selected = -1;
		mComboBoxColumn = new JComboBox();
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (getTableModel().isColumnDisplayable(column)) {
				if (column == mDefaultColumn)
					selected = mComboBoxColumn.getItemCount();
				mComboBoxColumn.addItem(getTableModel().getColumnTitleExtended(column));
				}
			}
		if (selected != -1)
			mComboBoxColumn.setSelectedIndex(selected);
		else if (mComboBoxColumn.getItemCount() != 0)
			mComboBoxColumn.setSelectedIndex(0);
		mComboBoxColumn.setEditable(mDefaultColumn == -1);
		mComboBoxColumn.setEnabled(mDefaultColumn == -1);
		
		mComboBoxColumn.addItemListener(this);
		p.add(new JLabel("Table column:"), "1,3");
		p.add(mComboBoxColumn, "3,3");
		return p;
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		super.setDialogToConfiguration(configuration);
		if (!hasInteractiveView()) {
			int column = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN));
			if (column == -1)
				mComboBoxColumn.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN));
			else
				mComboBoxColumn.setSelectedItem(getTableModel().getColumnTitle(column));
			}
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		super.addDialogConfiguration(configuration);
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		super.addViewConfiguration(configuration);
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias(mDefaultColumn));
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			int column = view.getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN));
			if (column == -1) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' not found.");
				return false;
				}
			}
		return super.isViewConfigurationValid(view, configuration);
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view, Properties configuration) {
		int column = view.getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN));
		return ((DETableView)view).getColorHandler().getVisualizationColor(column, mColorType);
		}

	@Override
	public Properties getRecentConfigurationLocal() {
		return sRecentConfiguration;
		}
	
	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}
	}
