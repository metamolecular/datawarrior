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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;


public class DETaskSetConnectionLines extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Connection Lines";

	private static final String PROPERTY_CONNECTION = "column";
    private static final String PROPERTY_CONNECTION_CODE_NONE = "<none>";
    private static final String PROPERTY_CONNECTION_CODE_ALL = "<all>";
    private static final String PROPERTY_CONNECTION_CODE_CASES = "<cases>";
    private static final String ITEM_CONNECTION_NONE = "<No connection lines>";
    private static final String ITEM_CONNECTION_ALL = "<Don't group, connect all>";
    private static final String ITEM_CONNECTION_CASES = "<Main cases>";

    private static final String PROPERTY_ORDER = "order";
    private static final String PROPERTY_ORDER_CODE_X_AXIS = "xAxis";
    private static final String ITEM_ORDER_X_AXIS = "<X-axis>";

    private static final String PROPERTY_RADIUS = "radius";
    private static final int DEFAULT_RADIUS = 5;
    private static final String PROPERTY_LINE_WIDTH = "lineWidth";
    private static final float DEFAULT_LINE_WIDTH = 1.0f;

    private static final String PROPERTY_TREE_VIEW = "treeView";
    private static final String PROPERTY_SHOW_ALL = "showAll";

    private static Properties sRecentConfiguration;

	JSlider             mSliderLineWidth,mSliderRadius;
	JComboBox			mComboBox1,mComboBox2,mComboBox3;
	JCheckBox			mCheckBox;
	JLabel				mLabelRadius;

	public DETaskSetConnectionLines(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
	public JComponent createInnerDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8 },
    						{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8 } };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		mComboBox1 = new JComboBox();
		mComboBox1.addItem(ITEM_CONNECTION_NONE);
		if (getVisualization() == null
		 || getVisualization().getChartType() == JVisualization.cChartTypeBoxPlot
		 || getVisualization().getChartType() == JVisualization.cChartTypeWhiskerPlot) {
			mComboBox1.addItem(ITEM_CONNECTION_CASES);
			}
		if (getVisualization() == null
		 || (getVisualization().getChartType() != JVisualization.cChartTypeBoxPlot
		  && getVisualization().getChartType() != JVisualization.cChartTypeWhiskerPlot)) {
			mComboBox1.addItem(ITEM_CONNECTION_ALL);
			}
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (getTableModel().isColumnTypeCategory(i)
			 || getTableModel().getColumnProperty(i, CompoundTableConstants.cColumnPropertyReferencedColumn) != null)
				mComboBox1.addItem(getTableModel().getColumnTitle(i));
		mComboBox1.setEditable(!hasInteractiveView());
		mComboBox1.addItemListener(this);
		cp.add(new JLabel("Group & connect by: "), "1,1");
		cp.add(mComboBox1, "3,1,5,1");

		mComboBox2 = new JComboBox();
		mComboBox2.addItem(ITEM_ORDER_X_AXIS);
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++) {
		    if (getTableModel().isColumnTypeCategory(i)
		     || getTableModel().isColumnTypeDouble(i)
		     || getTableModel().isColumnTypeDate(i)) {
				mComboBox2.addItem(getTableModel().getColumnTitle(i));
				}
			}
		mComboBox2.setEditable(!hasInteractiveView());
		mComboBox2.addItemListener(this);
		cp.add(new JLabel("Connection order by: "), "1,3");
		cp.add(mComboBox2, "3,3,5,3");

		mSliderLineWidth = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
//		mSliderLineWidth.setMinorTickSpacing(10);
//		mSliderLineWidth.setMajorTickSpacing(100);
		mSliderLineWidth.setPreferredSize(new Dimension(100, mSliderLineWidth.getPreferredSize().height));
		mSliderLineWidth.addChangeListener(this);
		cp.add(new JLabel("Relative line width:"), "1,5");
		cp.add(mSliderLineWidth, "3,5");

		mComboBox3 = new JComboBox(JVisualization.TREE_VIEW_MODE_NAME);
    	mComboBox3.addItemListener(this);
		cp.add(new JLabel("Detail graph mode: "), "1,7");
		cp.add(mComboBox3, "3,7,5,7");

		mCheckBox = new JCheckBox("Show all markers if no tree root (current row) is chosen");
		mCheckBox.addActionListener(this);
		cp.add(mCheckBox, "1,9,5,9");

		mSliderRadius = new JSlider(JSlider.HORIZONTAL, 0, 20, DEFAULT_RADIUS);
//		mSliderRadius.setMinorTickSpacing(1);
//		mSliderRadius.setMajorTickSpacing(5);
		mSliderRadius.setPreferredSize(new Dimension(100, mSliderRadius.getPreferredSize().height));
		mSliderRadius.addChangeListener(this);
		cp.add(new JLabel("Detail graph levels:"), "1,11");
		cp.add(mSliderRadius, "3,11");
		mLabelRadius = new JLabel(""+DEFAULT_RADIUS);
		mLabelRadius.setPreferredSize(new Dimension(32, mLabelRadius.getPreferredSize().height));
		cp.add(mLabelRadius, "5,11");

		return cp;
	    }

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Connection lines can only be shown in 2D- and 3D-Views.";
		}

	@Override
	public void setDialogToDefault() {
		mComboBox1.setSelectedItem(ITEM_CONNECTION_NONE);
		mComboBox2.setSelectedItem(ITEM_ORDER_X_AXIS);
		mComboBox3.setSelectedItem(JVisualization.cTreeViewModeNone);
		mSliderRadius.setValue(DEFAULT_RADIUS);
		mSliderLineWidth.setValue((int)(50.0*Math.sqrt(DEFAULT_LINE_WIDTH)));
		mCheckBox.setSelected(true);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String connection = configuration.getProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		if (connection.equals(PROPERTY_CONNECTION_CODE_NONE))
			mComboBox1.setSelectedItem(ITEM_CONNECTION_NONE);
		else if (connection.equals(PROPERTY_CONNECTION_CODE_ALL))
			mComboBox1.setSelectedItem(ITEM_CONNECTION_ALL);
		else if (connection.equals(PROPERTY_CONNECTION_CODE_CASES))
			mComboBox1.setSelectedItem(ITEM_CONNECTION_CASES);
		else
			mComboBox1.setSelectedItem(connection);

		String order = configuration.getProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		if (order.equals(PROPERTY_ORDER_CODE_X_AXIS))
			mComboBox2.setSelectedItem(ITEM_ORDER_X_AXIS);
		else
			mComboBox2.setSelectedItem(order);

		mComboBox3.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_TREE_VIEW),
					JVisualization.TREE_VIEW_MODE_CODE, JVisualization.cTreeViewModeNone));

		mCheckBox.setSelected(configuration.getProperty(PROPERTY_SHOW_ALL, "true").equals("true"));

		int radius = DEFAULT_RADIUS;
		try {
			radius = Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, ""+DEFAULT_RADIUS));
			}
		catch (NumberFormatException nfe) {}
		mSliderRadius.setValue(radius);

		float lineWidth = DEFAULT_LINE_WIDTH;
		try {
			lineWidth = Float.parseFloat(configuration.getProperty(PROPERTY_LINE_WIDTH, ""+DEFAULT_LINE_WIDTH));
			}
		catch (NumberFormatException nfe) {}
		mSliderLineWidth.setValue((int)(50.0*Math.sqrt(lineWidth)));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		String connection = (String)mComboBox1.getSelectedItem();
		if (connection.equals(ITEM_CONNECTION_NONE))
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		else if (connection.equals(ITEM_CONNECTION_ALL))
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_ALL);
		else if (connection.equals(ITEM_CONNECTION_CASES))
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_CASES);
		else
			configuration.setProperty(PROPERTY_CONNECTION, getTableModel().getColumnTitleNoAlias(connection));

		String order = (String)mComboBox2.getSelectedItem();
		if (order.equals(ITEM_ORDER_X_AXIS))
			configuration.setProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		else
			configuration.setProperty(PROPERTY_ORDER, getTableModel().getColumnTitleNoAlias(order));

		configuration.setProperty(PROPERTY_TREE_VIEW, JVisualization.TREE_VIEW_MODE_CODE[mComboBox3.getSelectedIndex()]);

		configuration.setProperty(PROPERTY_SHOW_ALL, mCheckBox.isSelected() ? "true" : "false");

		configuration.setProperty(PROPERTY_RADIUS, ""+mSliderRadius.getValue());
		configuration.setProperty(PROPERTY_LINE_WIDTH, ""+((float)(mSliderLineWidth.getValue()*mSliderLineWidth.getValue())/2500.0f));
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		int selectedConnectionColumn = getVisualization().getConnectionColumn();
		if (selectedConnectionColumn == JVisualization.cColumnUnassigned)
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		else if (selectedConnectionColumn == JVisualization.cConnectionColumnConnectAll)
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_ALL);
		else if (selectedConnectionColumn == JVisualization.cConnectionColumnConnectCases)
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_CASES);
		else
			configuration.setProperty(PROPERTY_CONNECTION, getTableModel().getColumnTitleNoAlias(selectedConnectionColumn));

		int selectedOrderColumn = getVisualization().getConnectionOrderColumn();
		if (selectedOrderColumn == JVisualization.cColumnUnassigned)
			configuration.setProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		else
			configuration.setProperty(PROPERTY_ORDER, getTableModel().getColumnTitleNoAlias(selectedOrderColumn));

		configuration.setProperty(PROPERTY_TREE_VIEW, JVisualization.TREE_VIEW_MODE_CODE[getVisualization().getTreeViewMode()]);

		configuration.setProperty(PROPERTY_SHOW_ALL, getVisualization().isTreeViewShowAll() ? "true" : "false");

		configuration.setProperty(PROPERTY_RADIUS, ""+getVisualization().getTreeViewRadius());
		configuration.setProperty(PROPERTY_LINE_WIDTH, ""+getVisualization().getConnectionLineWidth());
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String connection = configuration.getProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
			if (!connection.equals(PROPERTY_CONNECTION_CODE_NONE)
			 && !connection.equals(PROPERTY_CONNECTION_CODE_ALL)
			 && !connection.equals(PROPERTY_CONNECTION_CODE_CASES)
			 && getTableModel().findColumn(connection) == -1) {
				showErrorMessage("Column '"+connection+"' not found.");
				return false;
				}
	
			String order = configuration.getProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
			if (!order.equals(PROPERTY_ORDER_CODE_X_AXIS)
			 && getTableModel().findColumn(order) == -1) {
				showErrorMessage("Column '"+order+"' not found.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		String connection = configuration.getProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		int column = connection.equals(PROPERTY_CONNECTION_CODE_NONE) ? JVisualization.cColumnUnassigned
				   : connection.equals(PROPERTY_CONNECTION_CODE_ALL) ? JVisualization.cConnectionColumnConnectAll
				   : connection.equals(PROPERTY_CONNECTION_CODE_CASES) ? JVisualization.cConnectionColumnConnectCases
				   : getTableModel().findColumn(connection);
		String orderString = configuration.getProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		int orderColumn = orderString.equals(PROPERTY_ORDER_CODE_X_AXIS) ? JVisualization.cColumnUnassigned
				   : getTableModel().findColumn(orderString);

		int mode = findListIndex(configuration.getProperty(PROPERTY_TREE_VIEW),
					JVisualization.TREE_VIEW_MODE_CODE, JVisualization.cTreeViewModeNone);

		int radius = DEFAULT_RADIUS;
		try {
			radius = Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, ""+DEFAULT_RADIUS));
			}
		catch (NumberFormatException nfe) {}

		float lineWidth = DEFAULT_LINE_WIDTH;
		try {
			lineWidth = Float.parseFloat(configuration.getProperty(PROPERTY_LINE_WIDTH, ""+DEFAULT_LINE_WIDTH));
			}
		catch (NumberFormatException nfe) {}

		boolean showAll = configuration.getProperty(PROPERTY_SHOW_ALL, "true").equals("true");

		visualization.setConnectionColumns(column, orderColumn);
		visualization.setTreeViewMode(mode, radius, showAll);
		visualization.setConnectionLineWidth(lineWidth, isAdjusting);
		}

	@Override
	public void enableItems() {
		String item = (String)mComboBox1.getSelectedItem();
		boolean isReferencedConnection = false;
		if (!item.equals(ITEM_CONNECTION_NONE)
		 && !item.equals(ITEM_CONNECTION_ALL)
		 && !item.equals(ITEM_CONNECTION_CASES)) {
			int column = getTableModel().findColumn(item);
			if (column != -1
			 && getTableModel().getColumnProperty(column, CompoundTableModel.cColumnPropertyReferencedColumn) != null)
				isReferencedConnection = true;
			}
		mComboBox2.setEnabled(!item.equals(ITEM_CONNECTION_NONE) && !item.equals(ITEM_CONNECTION_CASES) && !isReferencedConnection);
		mSliderLineWidth.setEnabled(!item.equals(ITEM_CONNECTION_NONE));
		mComboBox3.setEnabled(!hasInteractiveView() || isReferencedConnection);

		boolean showDetailGraph = ((!hasInteractiveView() || isReferencedConnection)
								&& mComboBox3.getSelectedIndex() != JVisualization.cTreeViewModeNone);
		mCheckBox.setEnabled(showDetailGraph);
		mSliderRadius.setEnabled(showDetailGraph);
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == mSliderRadius)
			mLabelRadius.setText(""+mSliderRadius.getValue());

		super.stateChanged(e);
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
