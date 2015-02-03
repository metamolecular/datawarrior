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

import java.awt.Dimension;
import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;

public class DETaskSetMarkerBackgroundColor extends DETaskAbstractSetColor {
	public static final String TASK_NAME = "Set Marker Background Color";

	private static final String PROPERTY_CONSIDER = "consider";
	private static final String PROPERTY_CONSIDER_VISIBLE = "visible";
	private static final String PROPERTY_RADIUS = "radius";
	private static final String PROPERTY_FADING = "fading";

	private static final String ITEM_VISIBLE_ROWS = "Visible Rows";

	private static Properties sRecentConfiguration;

    private JSlider		mSliderRadius,mSliderFading;
    private JComboBox	mComboBoxConsider;

	public DETaskSetMarkerBackgroundColor(Frame owner,
									DEMainPane mainPane,
									VisualizationPanel2D view) {
		super(owner, mainPane, view, "Set Marker Background Color");
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view) {
		return ((JVisualization2D)((VisualizationPanel)view).getVisualization()).getBackgroundColor();
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view, Properties configuration) {
		return getVisualizationColor(view);
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel2D) ? null : "Marker background colors can only be assigned to 2D-Views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel p = (JPanel)super.createInnerDialogContent();

		mComboBoxConsider = new JComboBox();
        mComboBoxConsider.addItem(ITEM_VISIBLE_ROWS);
		for (int i=0; i<getTableModel().getHitlistHandler().getHitlistCount(); i++) {
			int pseudoColumn = CompoundTableHitlistHandler.getColumnFromHitlist(i);
			mComboBoxConsider.addItem(getTableModel().getColumnTitleExtended(pseudoColumn));
			}
		mComboBoxConsider.setEditable(!hasInteractiveView());
        mComboBoxConsider.addItemListener(this);
		p.add(new JLabel("Consider:"), "1,3");
		p.add(mComboBoxConsider, "3,3");

		double size[][] = { {4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4},
				 			{4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4} };
		JPanel sliderpanel = new JPanel();
		sliderpanel.setLayout(new TableLayout(size));
		mSliderRadius = new JSlider(0, 20, 10);
		mSliderRadius.addChangeListener(this);
		mSliderRadius.setMinorTickSpacing(1);
		mSliderRadius.setMajorTickSpacing(5);
		mSliderRadius.setPaintTicks(true);
		mSliderRadius.setPaintLabels(true);
		mSliderRadius.setPreferredSize(new Dimension(160, 42));
		sliderpanel.add(new JLabel("Radius:"), "1,1");
		sliderpanel.add(mSliderRadius, "1,3");

		mSliderFading = new JSlider(0, 20, 10);
		mSliderFading.addChangeListener(this);
		mSliderFading.setMinorTickSpacing(1);
		mSliderFading.setMajorTickSpacing(5);
		mSliderFading.setPaintTicks(true);
		mSliderFading.setPaintLabels(true);
		mSliderFading.setPreferredSize(new Dimension(160, 42));
		sliderpanel.add(new JLabel("Fading:"), "3,1");
		sliderpanel.add(mSliderFading, "3,3");

		p.add(sliderpanel, "1,11,3,11");
		return p;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void setDialogToDefault() {
		super.setDialogToDefault();
		mComboBoxConsider.setSelectedIndex(0);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		super.setDialogToConfiguration(configuration);
		String consider = configuration.getProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		if (consider.equals(PROPERTY_CONSIDER_VISIBLE)) {
			mComboBoxConsider.setSelectedIndex(0);
			}
		else {
			int pseudoColumn = getTableModel().findColumn(consider);
			mComboBoxConsider.setSelectedItem(!hasInteractiveView() && pseudoColumn == -1 ? consider : getTableModel().getColumnTitleExtended(pseudoColumn));
			}

		int radius = 10;
		try {
			radius = Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, "10"));
			}
		catch (NumberFormatException nfe) {}
		mSliderRadius.setValue(radius);

		int fading = 10;
		try {
			fading = Integer.parseInt(configuration.getProperty(PROPERTY_FADING, "10"));
			}
		catch (NumberFormatException nfe) {}
		mSliderFading.setValue(20-fading);
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		super.addDialogConfiguration(configuration);
		if (mComboBoxConsider.getSelectedIndex() == 0)
			configuration.setProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		else
			configuration.setProperty(PROPERTY_CONSIDER, getTableModel().getColumnTitleNoAlias((String)mComboBoxConsider.getSelectedItem()));
		configuration.setProperty(PROPERTY_RADIUS, ""+(mSliderRadius.getValue()));
		configuration.setProperty(PROPERTY_FADING, ""+(20-mSliderFading.getValue()));
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		super.addViewConfiguration(configuration);
		int hitlist = ((JVisualization2D)getVisualization()).getBackgroundColorConsidered();
		if (hitlist == -1)
			configuration.setProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		else
			configuration.setProperty(PROPERTY_CONSIDER, getTableModel().getColumnTitleNoAlias(CompoundTableHitlistHandler.getColumnFromHitlist(hitlist)));
		configuration.setProperty(PROPERTY_RADIUS, ""+((JVisualization2D)getVisualization()).getBackgroundColorRadius());
		configuration.setProperty(PROPERTY_FADING, ""+((JVisualization2D)getVisualization()).getBackgroundColorFading());
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String consider = configuration.getProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
			if (!consider.equals(PROPERTY_CONSIDER_VISIBLE)) {
				int pseudoColumn = getTableModel().findColumn(consider);
				if (pseudoColumn == -1) {
					showErrorMessage("Column '"+consider+"' not found.");
					return false;
					}
				}
			}
		return super.isViewConfigurationValid(view, configuration);
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		super.applyConfiguration(view, configuration, isAdjusting);

		JVisualization2D v2D = (JVisualization2D)((VisualizationPanel)view).getVisualization();

		String consider = configuration.getProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		if (consider.equals(PROPERTY_CONSIDER_VISIBLE))
			v2D.setBackgroundColorConsidered(-1);
		else
			v2D.setBackgroundColorConsidered(CompoundTableHitlistHandler.getHitlistFromColumn(getTableModel().findColumn(consider)));

		try {
			v2D.setBackgroundColorRadius(Math.max(1, Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, "10"))));
			}
		catch (NumberFormatException nfe) {}

		try {
			v2D.setBackgroundColorFading(Math.max(1, 20-Integer.parseInt(configuration.getProperty(PROPERTY_FADING, "10"))));
			}
		catch (NumberFormatException nfe) {}
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
