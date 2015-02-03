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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.MarkerLabelConstants;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;


public class DETaskShowLabels extends DETaskAbstractSetViewOptions implements MarkerLabelConstants {
	public static final String TASK_NAME = "Show Labels";

	private static final String PROPERTY_IN_DETAIL_GRAPH_ONLY = "inDetailGraphOnly";
	private static final String PROPERTY_LABEL_SIZE = "labelSize";

	private static final String TEXT_NO_LABEL = "<no label>";

	private static Properties sRecentConfiguration;

	private JComboBox[]			mComboBoxPosition;
	private JSlider				mSlider;
	private JCheckBox			mCheckBoxDetailGraphOnly;

	public DETaskShowLabels(Frame owner, DEMainPane mainPane, CompoundTableView view) {
		super(owner, mainPane, view);
		}

	private MarkerLabelDisplayer getLabelDisplayer(CompoundTableView view) {
		return (view == null) ? null
			 : (view instanceof VisualizationPanel) ? ((VisualizationPanel)view).getVisualization()
			 : (MarkerLabelDisplayer)view;
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
		return (view instanceof MarkerLabelDisplayer
			 || view instanceof VisualizationPanel) ? null : "Labels can only be shown in 2D-, 3D- and structure-views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		ArrayList<String> columnNameList = new ArrayList<String>();
		columnNameList.add(TEXT_NO_LABEL);
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (columnQualifies(column))
				columnNameList.add(getTableModel().getColumnTitle(column));
		String[] columnName = columnNameList.toArray(new String[0]);

		MarkerLabelDisplayer mld = getLabelDisplayer(getInteractiveView());

		mComboBoxPosition = new JComboBox[cPositionCode.length];
		for (int i=0; i<cFirstMidPosition; i++)
			mComboBoxPosition[i] = new JComboBox(columnName);
		if (mld == null || mld.supportsMidPositionLabels())
			for (int i=cFirstMidPosition; i<MarkerLabelDisplayer.cFirstBottomPosition; i++)
				mComboBoxPosition[i] = new JComboBox(columnName);
		for (int i=cFirstBottomPosition; i<cFirstTablePosition; i++)
			mComboBoxPosition[i] = new JComboBox(columnName);
		int tableLines = (mld == null || mld.supportsMarkerLabelTable()) ? cPositionCode.length - cFirstTablePosition : 0;
		for (int i=0; i<tableLines; i++)
				mComboBoxPosition[cFirstTablePosition+i] = new JComboBox(columnName);

		for (JComboBox cb:mComboBoxPosition)
			if (cb != null)
				cb.addActionListener(this);

		double[] sizeY1 = {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16 };
		double[] sizeY = new double[sizeY1.length+2*tableLines+6];
		int index = 0;
		for (double s:sizeY1)
			sizeY[index++] = s;
		for (int i=0; i<tableLines; i++) {
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 4;
			}
		sizeY[index-1] = 16;	// correct the last vertical spacer
		for (int i=0; i<3; i++) {
			sizeY[index++] = TableLayout.PREFERRED;
			sizeY[index++] = 8;
			}

		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8 }, sizeY };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		cp.add(new JLabel("Top Left"), "1,1");
		cp.add(new JLabel("Top Right", JLabel.RIGHT), "5,1");
		cp.add(new JLabel("Bottom Left"), "1,9");
		cp.add(new JLabel("Bottom Right", JLabel.RIGHT), "5,9");
		for (int i=0; i<cPositionCode.length; i++) {
			if (mComboBoxPosition[i] != null) {
				mComboBoxPosition[i].addItemListener(this);
				mComboBoxPosition[i].setEditable(!hasInteractiveView());
				if (i < cFirstTablePosition) {
					cp.add(mComboBoxPosition[i], ""+(1+2*(i%3))+","+(3+2*(i/3)));
					}
				else {
					int y = 11+2*(i-cFirstTablePosition);
					cp.add(new JLabel("Table line "+(1+i-cFirstTablePosition)+":", JLabel.RIGHT), "1,"+y);
					cp.add(mComboBoxPosition[i], "3,"+y);
					}
				}
			}

		mCheckBoxDetailGraphOnly = new JCheckBox("Show labels in detail graph only");
		mCheckBoxDetailGraphOnly.setEnabled(mld == null || mld.isTreeViewModeEnabled());
		mCheckBoxDetailGraphOnly.setHorizontalAlignment(SwingConstants.CENTER);
		mCheckBoxDetailGraphOnly.addActionListener(this);
		cp.add(mCheckBoxDetailGraphOnly, "1,"+(sizeY.length-6)+",5,"+(sizeY.length-6));

		JPanel sliderpanel = new JPanel();
		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 150, 50);
		mSlider.setPreferredSize(new Dimension(150, 20));
		mSlider.addChangeListener(this);
		sliderpanel.add(new JLabel("Label Size:"));
		sliderpanel.add(mSlider);
		cp.add(sliderpanel, "1,"+(sizeY.length-4)+",5,"+(sizeY.length-4));

		JPanel bp = new JPanel();
		bp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bdefault = new JButton("Add Default");
		bdefault.addActionListener(this);
		bp.add(bdefault);
		JButton bnone = new JButton("Remove All");
		bnone.addActionListener(this);
		bp.add(bnone);
		cp.add(bp, "1,"+(sizeY.length-2)+",5,"+(sizeY.length-2));

		return cp;
		}

	private boolean columnQualifies(int column) {
		return getTableModel().getColumnSpecialType(column) == null
			|| (CompoundTableModel.cColumnTypeIDCode.equals(getTableModel().getColumnSpecialType(column))
			 && (!hasInteractiveView() || getInteractiveView() instanceof VisualizationPanel));
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Remove All")
			removeAllLabels();

		if (e.getActionCommand() == "Add Default")
			addDefaultLabels();
	
		if (!isIgnoreEvents()) {
			for (JComboBox cb:mComboBoxPosition) {
				if (cb != null && e.getSource() == cb && !TEXT_NO_LABEL.equals(cb.getSelectedItem())) {
					setIgnoreEvents(true);
					for (JComboBox cbi:mComboBoxPosition)
						if (cbi != null && cbi != cb && cb.getSelectedItem().equals(cbi.getSelectedItem()))
							cbi.setSelectedItem(TEXT_NO_LABEL);
					setIgnoreEvents(false);
					}
				}
			
			super.actionPerformed(e);	// causes a view update
			}
		}

	private void removeAllLabels() {
		setIgnoreEvents(true);
		for (int i=0; i<mComboBoxPosition.length; i++)
			if (mComboBoxPosition[i] != null)
				mComboBoxPosition[i].setSelectedItem(TEXT_NO_LABEL);
		setIgnoreEvents(false);
		}

	private void addDefaultLabels() {
		removeAllLabels();

		setIgnoreEvents(true);
		int idCount = 0;
		int numCount = 0;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (columnQualifies(column) && getTableModel().getColumnSpecialType(column) == null) {
				if (getTableModel().isColumnTypeDouble(column)) {
					if ((!hasInteractiveView() || getLabelDisplayer(getInteractiveView()).supportsMarkerLabelTable()) && numCount < 6)
						mComboBoxPosition[cFirstTablePosition+numCount++].setSelectedItem(getTableModel().getColumnTitle(column));
					}
				else {
					if (idCount == 0) {
						mComboBoxPosition[0].setSelectedItem(getTableModel().getColumnTitle(column));
						idCount++;
						}
					else if (idCount == 1) {
						mComboBoxPosition[2].setSelectedItem(getTableModel().getColumnTitle(column));
						idCount++;
						}
					}
				}
			}
		setIgnoreEvents(false);
   		}
	
	@Override
	public void setDialogToDefault() {
		for (JComboBox cb:mComboBoxPosition)
			cb.setSelectedItem(TEXT_NO_LABEL);
		mSlider.setValue(50);
		mCheckBoxDetailGraphOnly.setSelected(false);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		for (int i=0; i<cPositionCode.length; i++) {
			if (mComboBoxPosition[i] != null) {
				String columnName = configuration.getProperty(cPositionCode[i]);
				if (columnName == null)
					mComboBoxPosition[i].setSelectedItem(TEXT_NO_LABEL);
				else {
					int column = getTableModel().findColumn(columnName);
					mComboBoxPosition[i].setSelectedItem(column == -1 ? columnName : getTableModel().getColumnTitle(column));
					}
				}
			}

		float size = 1.0f;
		try {
			size = Float.parseFloat(configuration.getProperty(PROPERTY_LABEL_SIZE, "1.0"));
			}
		catch (NumberFormatException nfe) {}
		mSlider.setValue(50+(int)(50.0*Math.log(size)));

		mCheckBoxDetailGraphOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY)));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		for (int i=0; i<cFirstTablePosition; i++)
			if (mComboBoxPosition[i] != null && !mComboBoxPosition[i].getSelectedItem().equals(TEXT_NO_LABEL))
				configuration.setProperty(cPositionCode[i], ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxPosition[i].getSelectedItem()));
		int tableIndex = cFirstTablePosition;
		for (int i=cFirstTablePosition; i<cPositionCode.length; i++)
			if (mComboBoxPosition[i] != null && !mComboBoxPosition[i].getSelectedItem().equals(TEXT_NO_LABEL))
				configuration.setProperty(cPositionCode[tableIndex++], ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxPosition[i].getSelectedItem()));
		float size = (float)Math.exp((double)(mSlider.getValue()-50)/50.0);
		configuration.setProperty(PROPERTY_LABEL_SIZE, ""+size);
		configuration.setProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY, mCheckBoxDetailGraphOnly.isSelected() ? "true" : "false");
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		MarkerLabelDisplayer mld = getLabelDisplayer(getInteractiveView());
		for (int i=0; i<cPositionCode.length; i++) {
			int column = mld.getMarkerLabelColumn(i);
			if (column != JVisualization.cColumnUnassigned)
				configuration.setProperty(cPositionCode[i], ""+getTableModel().getColumnTitleNoAlias(column));
			}
		configuration.setProperty(PROPERTY_LABEL_SIZE, ""+mld.getMarkerLabelSize());
		configuration.setProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY, mld.isMarkerLabelsInTreeViewOnly() ? "true" : "false");
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			for (int i=0; i<cPositionCode.length; i++) {
				String columnName = configuration.getProperty(cPositionCode[i]);
				if (columnName != null) {
					int column = getTableModel().findColumn(columnName);
					if (column == -1) {
						showErrorMessage("Column '"+columnName+"' not found.");
						return false;
						}
					if (!columnQualifies(column)) {
						showErrorMessage("Column '"+columnName+"' cannot be used for displaying labels.");
						return false;
						}
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
		int[] columnAtPosition = new int[cPositionCode.length];

		MarkerLabelDisplayer mld = getLabelDisplayer(view);
		for (int i=0; i<cPositionCode.length; i++) {
			String columnName = configuration.getProperty(cPositionCode[i]);
			if (columnName == null)
				columnAtPosition[i] = -1;
			else
				columnAtPosition[i] = getTableModel().findColumn(columnName);
			}
		mld.setMarkerLabels(columnAtPosition);

		try {
			mld.setMarkerLabelSize(Float.parseFloat(configuration.getProperty(PROPERTY_LABEL_SIZE, "1.0")), isAdjusting);
			}
		catch (NumberFormatException nfe) {}

		mld.setMarkerLabelsInTreeViewOnly("true".equals(configuration.getProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY)));
		}
	}
