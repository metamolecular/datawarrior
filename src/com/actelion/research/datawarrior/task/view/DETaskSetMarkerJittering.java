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
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;


public class DETaskSetMarkerJittering extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Marker Jittering";

	private static final String PROPERTY_JITTER = "jitter";

    private static Properties sRecentConfiguration;

	private JSlider         mSlider;

    public DETaskSetMarkerJittering(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
		return (view instanceof VisualizationPanel) ? null : "Marker jittering can only be applied to 2D- or 3D-Views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel sp = new JPanel();
		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		mSlider.setPreferredSize(new Dimension(100, 20));
		mSlider.setMinorTickSpacing(10);
		mSlider.setMajorTickSpacing(100);
		mSlider.addChangeListener(this);
		sp.add(new JLabel("Jittering:  "));
		sp.add(mSlider);

		return sp;
	    }

	@Override
	public void setDialogToDefault() {
		mSlider.setValue(0);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		float jitter = 0f;
		try {
			jitter = Float.parseFloat(configuration.getProperty(PROPERTY_JITTER, "0"));
			}
		catch (NumberFormatException nfe) {}
		mSlider.setValue((int)(100f*jitter));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_JITTER, ""+(0.01f*mSlider.getValue()));
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_JITTER, ""+getVisualization().getJittering());
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
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		float jitter = 0f;
		try {
			jitter = Float.parseFloat(configuration.getProperty(PROPERTY_JITTER, "0"));
			visualization.setJittering(jitter, isAdjusting);
			}
		catch (NumberFormatException nfe) {}
		}
	}
