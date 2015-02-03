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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;


public class DETaskSetMarkerSize extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Marker Size";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_SIZE = "size";
	private static final String PROPERTY_INVERSE = "inverse";
	private static final String PROPERTY_ADAPTIVE = "adaptive";
	private static final String PROPERTY_PROPORTIONAL = "proportional";

    private static Properties sRecentConfiguration;

	private JSlider         mSlider;
    private JComboBox		mComboBox;
    private JCheckBox		mCheckBoxProportional,mCheckBoxInverse,mCheckBoxAdaptive;

    public DETaskSetMarkerSize(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
		return (view instanceof VisualizationPanel) ? null : "Marker sizes can only be applied to 2D- or 3D-Views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		double[][] size = { {8, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, 8},
							{8, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED,
								TableLayout.PREFERRED, TableLayout.PREFERRED, 8} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		JPanel sp = new JPanel();
		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
//		mSlider.setPreferredSize(new Dimension(100, 20));
//		mSlider.setMinorTickSpacing(10);
//		mSlider.setMajorTickSpacing(100);
		mSlider.addChangeListener(this);
		sp.add(new JLabel("small"));
		sp.add(mSlider);
		sp.add(new JLabel("large"));
		p.add(sp, "1,1,3,1");

		JPanel cp = new JPanel();
		mComboBox = new JComboBox();
		mComboBox.addItem(getTableModel().getColumnTitleExtended(-1));
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (getTableModel().isColumnTypeDouble(i))
				mComboBox.addItem(getTableModel().getColumnTitleExtended(i));
		for (int i=0; i<getTableModel().getHitlistHandler().getHitlistCount(); i++)
			mComboBox.addItem(getTableModel().getColumnTitleExtended(CompoundTableHitlistHandler.getColumnFromHitlist(i)));
        mComboBox.setEditable(!hasInteractiveView());
		mComboBox.addItemListener(this);
		if (hasInteractiveView()) {
			JVisualization visualization = ((VisualizationPanel)getInteractiveView()).getVisualization();
			if (visualization.getChartType() == JVisualization.cChartTypeBars
			 || visualization.getChartType() == JVisualization.cChartTypePies)
				mComboBox.setEnabled(false);
			}
		cp.add(new JLabel("Size by: "));
		cp.add(mComboBox);
		p.add(cp, "1,2,3,2");

		mCheckBoxProportional = new JCheckBox("Strictly proportional");
		mCheckBoxProportional.addActionListener(this);
		p.add(mCheckBoxProportional, "2,3");

		mCheckBoxInverse = new JCheckBox("Invert sizes");
		mCheckBoxInverse.addActionListener(this);
		p.add(mCheckBoxInverse, "2,4");

		mCheckBoxAdaptive = new JCheckBox("Adapt size to zoom state");
		mCheckBoxAdaptive.addActionListener(this);
		p.add(mCheckBoxAdaptive, "2,5");

		return p;
	    }

	@Override
	public void setDialogToDefault() {
        mComboBox.setSelectedIndex(0);
		mSlider.setValue(50);
		mCheckBoxProportional.setSelected(false);
		mCheckBoxInverse.setSelected(false);
		mCheckBoxAdaptive.setSelected(true);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode);
		int column = getTableModel().findColumn(columnName);
		mComboBox.setSelectedItem(!hasInteractiveView() && column == -1 ? columnName : getTableModel().getColumnTitleExtended(column));

		float size = 0.5f;
		try {
			size = Float.parseFloat(configuration.getProperty(PROPERTY_SIZE, "0.5"));
			}
		catch (NumberFormatException nfe) {}
		mSlider.setValue((int)(50.0*Math.sqrt(size)));

		mCheckBoxProportional.setSelected("true".equals(configuration.getProperty(PROPERTY_PROPORTIONAL, "true")));
		mCheckBoxInverse.setSelected("true".equals(configuration.getProperty(PROPERTY_INVERSE)));
		mCheckBoxAdaptive.setSelected("true".equals(configuration.getProperty(PROPERTY_ADAPTIVE)));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBox.getSelectedItem()));
		configuration.setProperty(PROPERTY_SIZE, ""+(mSlider.getValue()*mSlider.getValue()/2500f));
		configuration.setProperty(PROPERTY_PROPORTIONAL, mCheckBoxProportional.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_INVERSE, mCheckBoxInverse.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_ADAPTIVE, mCheckBoxAdaptive.isSelected() ? "true" : "false");
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias(getVisualization().getMarkerSizeColumn()));
		configuration.setProperty(PROPERTY_SIZE, ""+getVisualization().getMarkerSize());
		configuration.setProperty(PROPERTY_PROPORTIONAL, getVisualization().getMarkerSizeProportional() ? "true" : "false");
		configuration.setProperty(PROPERTY_INVERSE, getVisualization().getMarkerSizeInversion() ? "true" : "false");
		configuration.setProperty(PROPERTY_ADAPTIVE, getVisualization().isMarkerSizeZoomAdapted() ? "true" : "false");
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
				if (!getTableModel().isColumnTypeDouble(column)) {
					showErrorMessage("Column '"+columnName+"' does not contain numerical values.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
		mCheckBoxProportional.setEnabled(mComboBox.getSelectedIndex() != 0);
		mCheckBoxInverse.setEnabled(mComboBox.getSelectedIndex() != 0);
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
		float size = 1.0f;
		try {
			size = Float.parseFloat(configuration.getProperty(PROPERTY_SIZE, "1.0"));
			}
		catch (NumberFormatException nfe) {}

		int column = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode));

		((VisualizationPanel)view).getVisualization().setMarkerSizeColumn(column);
		((VisualizationPanel)view).getVisualization().setMarkerSize(size, isAdjusting);
		((VisualizationPanel)view).getVisualization().setMarkerSizeProportional("true".equals(configuration.getProperty(PROPERTY_PROPORTIONAL, "true")));
		((VisualizationPanel)view).getVisualization().setMarkerSizeInversion("true".equals(configuration.getProperty(PROPERTY_INVERSE)));
		((VisualizationPanel)view).getVisualization().setMarkerSizeZoomAdaption("true".equals(configuration.getProperty(PROPERTY_ADAPTIVE)));
		}
	}
