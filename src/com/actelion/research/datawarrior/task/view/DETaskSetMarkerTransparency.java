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

import java.awt.Dimension;
import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;


public class DETaskSetMarkerTransparency extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Marker Transparency";

	private static final String PROPERTY_TRANSPARENCY = "transparency";

    private static Properties sRecentConfiguration;

	private JSlider         mSlider;

    public DETaskSetMarkerTransparency(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
		return (view instanceof VisualizationPanel2D) ? null : "Marker transparencies can only be applied to 2D-Views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel sp = new JPanel();
		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		mSlider.setPreferredSize(new Dimension(100, 20));
		mSlider.setMinorTickSpacing(10);
		mSlider.setMajorTickSpacing(100);
		mSlider.addChangeListener(this);
		sp.add(new JLabel("Transparency:  "));
		sp.add(mSlider);

		return sp;
	    }

	@Override
	public void setDialogToDefault() {
		mSlider.setValue(0);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		float transparency = 0f;
		try {
			transparency = Float.parseFloat(configuration.getProperty(PROPERTY_TRANSPARENCY, "0"));
			}
		catch (NumberFormatException nfe) {}
		mSlider.setValue((int)(100f*transparency));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_TRANSPARENCY, ""+(0.01f*mSlider.getValue()));
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_TRANSPARENCY, ""+((JVisualization2D)getVisualization()).getMarkerTransparency());
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
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel)view).getVisualization();
		float transparency = 0f;
		try {
			transparency = Float.parseFloat(configuration.getProperty(PROPERTY_TRANSPARENCY, "0"));
			visualization.setMarkerTransparency(transparency);
			}
		catch (NumberFormatException nfe) {}
		}
	}
