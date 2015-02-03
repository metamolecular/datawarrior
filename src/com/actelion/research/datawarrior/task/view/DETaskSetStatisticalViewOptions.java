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

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;


public class DETaskSetStatisticalViewOptions extends DETaskAbstractSetViewOptions {

	public static final String TASK_NAME = "Set Statistical View Options";

    private static final String PROPERTY_CORRELATION_TYPE = "correlationType";
    private static final String PROPERTY_MEAN_LINE_TYPE = "meanLineType";
    private static final String PROPERTY_SHOW_STDDEV = "showStdDev";
    private static final String PROPERTY_SPLIT_BY_CATEGORY = "splitCurvesByCategory";
    private static final String PROPERTY_BOXPLOT_SHOW_PVALUE = "showPValues";
    private static final String PROPERTY_BOXPLOT_SHOW_FOLDCHANGE = "showFoldChange";
    private static final String PROPERTY_PVALUE_COLUMN = "pValueColumn";
    private static final String PROPERTY_PVALUE_REF_CATEGORY = "pValueRefCategory";
    private static final String PROPERTY_BOXPLOT_MEAN_MODE = "boxPlotMeanMode";
    private static final String PROPERTY_BOXPLOT_MEAN_VALUES = "boxPlotMeanValues";

    private static Properties sRecentConfiguration;

	private JComboBox			mComboBoxCorrelationType,mComboBoxLineMode,mComboBoxBoxplotMeanMode;
	private JCheckBox			mCheckBoxStdDev,mCheckBoxMultipleCurves,mCheckBoxShowMeanValues,mCheckBoxShowPValues,mCheckBoxShowFoldChange;
	private JComponent			mSelectorPValueColumn,mSelectorPValueRefCategory;

	/**
	 * @param owner
	 * @param visualization null or view that is interactively updated
	 */
	public DETaskSetStatisticalViewOptions(Frame owner, DEMainPane mainPane, VisualizationPanel2D view) {
		super(owner, mainPane, view);
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Statistical view options can only be defined for 2D- and 3D-Views.";
		}

	@Override
	public JComponent createInnerDialogContent() {
		JTabbedPane tabbedPane = new JTabbedPane();

		double[][] scatterPlotSize = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
									   {8, TableLayout.PREFERRED, 24, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };
		JPanel scatterPlotPanel = new JPanel();
		scatterPlotPanel.setLayout(new TableLayout(scatterPlotSize));

		scatterPlotPanel.add(new JLabel("Show Correlation Coefficient:"), "1,1");

		mComboBoxCorrelationType = new JComboBox();
		mComboBoxCorrelationType.addItem("<none>");
		for (int i=0; i<CorrelationCalculator.TYPE_NAME.length; i++)
	        mComboBoxCorrelationType.addItem(CorrelationCalculator.TYPE_NAME[i]);
		mComboBoxCorrelationType.addActionListener(this);
		scatterPlotPanel.add(mComboBoxCorrelationType, "3,1");

		scatterPlotPanel.add(new JLabel("Show Fitted Curve:"), "1,3");
		mComboBoxLineMode = new JComboBox(JVisualization2D.CURVE_MODE_TEXT);
		mComboBoxLineMode.addActionListener(this);
		scatterPlotPanel.add(mComboBoxLineMode, "3,3");

		JPanel innerCheckBoxPanel = new JPanel();
		innerCheckBoxPanel.setLayout(new GridLayout(2, 1));
		mCheckBoxStdDev = new JCheckBox("Display Standard Deviation");
        mCheckBoxStdDev.setEnabled(false);
		mCheckBoxStdDev.addActionListener(this);
		innerCheckBoxPanel.add(mCheckBoxStdDev);

		mCheckBoxMultipleCurves = new JCheckBox("Split By Color-Category");
		mCheckBoxMultipleCurves.setEnabled(false);
		mCheckBoxMultipleCurves.addActionListener(this);
		innerCheckBoxPanel.add(mCheckBoxMultipleCurves);

		JPanel checkBoxPanel = new JPanel();
		checkBoxPanel.setLayout(new BorderLayout());
		checkBoxPanel.add(innerCheckBoxPanel, BorderLayout.EAST);
		scatterPlotPanel.add(checkBoxPanel, "1,5,3,5");

		tabbedPane.add(scatterPlotPanel, "Scatter Plot");


		double[][] boxPlotSize = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
								   {4, TableLayout.PREFERRED, TableLayout.PREFERRED,
									4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
								   24, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4} };
		JPanel boxPlotPanel = new JPanel();
		boxPlotPanel.setLayout(new TableLayout(boxPlotSize));

		CompoundTableModel tableModel = getTableModel();

		mCheckBoxShowPValues = new JCheckBox("Show p-values");
		mCheckBoxShowPValues.addActionListener(this);
		boxPlotPanel.add(mCheckBoxShowPValues, "1,1,3,1");

		mCheckBoxShowFoldChange = new JCheckBox("Show fold-change");
		mCheckBoxShowFoldChange.addActionListener(this);
		boxPlotPanel.add(mCheckBoxShowFoldChange, "1,2,3,2");

		boxPlotPanel.add(new JLabel("Compare values on:"), "1,4");
		if (hasInteractiveView()) {
			JComboBox cb = new JComboBox();
			for (int column=0; column<tableModel.getTotalColumnCount(); column++)
				if (getVisualization().isValidPValueColumn(column))
					cb.addItem(tableModel.getColumnTitle(column));
			cb.addActionListener(this);
			mSelectorPValueColumn = cb;
			}
		else {
			mSelectorPValueColumn = new JTextField();
			}
		boxPlotPanel.add(mSelectorPValueColumn, "3,4");

		boxPlotPanel.add(new JLabel("Reference category:"), "1,6");
		if (hasInteractiveView()) {
			JComboBox cb = new JComboBox();
			int column = getTableModel().findColumn((String)((JComboBox)mSelectorPValueColumn).getSelectedItem());
			if (column != -1) {
	    		String[] categories = getTableModel().getCategoryList(column);
	    		for (String category:categories)
	    			cb.addItem(category);
				}
			cb.addActionListener(this);
			mSelectorPValueRefCategory = cb;
			}
		else {
			mSelectorPValueRefCategory = new JTextField();
			}
		boxPlotPanel.add(mSelectorPValueRefCategory, "3,6");

		if (hasInteractiveView()
		 && ((JComboBox)mSelectorPValueColumn).getItemCount() == 0) {
			mCheckBoxShowPValues.setEnabled(false);
			mCheckBoxShowFoldChange.setEnabled(false);
			}

		boxPlotPanel.add(new JLabel("Mean/median mode:"), "1,8");
		mComboBoxBoxplotMeanMode = new JComboBox(JVisualization2D.BOXPLOT_MEAN_MODE_TEXT);
		mComboBoxBoxplotMeanMode.addActionListener(this);
		boxPlotPanel.add(mComboBoxBoxplotMeanMode, "3,8");

		mCheckBoxShowMeanValues = new JCheckBox("Show mean/median values");
		mCheckBoxShowMeanValues.setEnabled(false);
		mCheckBoxShowMeanValues.addActionListener(this);
		boxPlotPanel.add(mCheckBoxShowMeanValues, "1,10,3,10");

		tabbedPane.add(boxPlotPanel, "Box Plot");

		if (hasInteractiveView() && getVisualization().getChartType() == JVisualization.cChartTypeBoxPlot)
			tabbedPane.setSelectedComponent(boxPlotPanel);

		return tabbedPane;
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		if (mComboBoxCorrelationType.getSelectedIndex() != 0)
			configuration.setProperty(PROPERTY_CORRELATION_TYPE, CorrelationCalculator.TYPE_NAME[mComboBoxCorrelationType.getSelectedIndex()-1]);

		if (mComboBoxLineMode.getSelectedIndex() != 0) {
			configuration.setProperty(PROPERTY_MEAN_LINE_TYPE, JVisualization2D.CURVE_MODE_CODE[mComboBoxLineMode.getSelectedIndex()]);
			configuration.setProperty(PROPERTY_SHOW_STDDEV, mCheckBoxStdDev.isSelected() ? "true" : "false");
			configuration.setProperty(PROPERTY_SPLIT_BY_CATEGORY, mCheckBoxMultipleCurves.isSelected() ? "true" : "false");
			}

		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_PVALUE, mCheckBoxShowPValues.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE, mCheckBoxShowFoldChange.isSelected() ? "true" : "false");

		if (mSelectorPValueColumn instanceof JComboBox) {
			if (((JComboBox)mSelectorPValueColumn).getItemCount() != 0
			 && ((JComboBox)mSelectorPValueRefCategory).getItemCount() >= 2) {
				String pValueColumn = (String)((JComboBox)mSelectorPValueColumn).getSelectedItem();
				configuration.setProperty(PROPERTY_PVALUE_COLUMN, getTableModel().getColumnTitleNoAlias(getTableModel().findColumn(pValueColumn)));
				String pValueRefCategory = (String)((JComboBox)mSelectorPValueRefCategory).getSelectedItem();
				configuration.setProperty(PROPERTY_PVALUE_REF_CATEGORY, pValueRefCategory);
				}
			}
		else {
			String pValueColumn = ((JTextField)mSelectorPValueColumn).getText();
			if (pValueColumn.length() != 0) {
				configuration.setProperty(PROPERTY_PVALUE_COLUMN, pValueColumn);
				configuration.setProperty(PROPERTY_PVALUE_REF_CATEGORY, ((JTextField)mSelectorPValueRefCategory).getText());
				}
			}

		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_MODE, JVisualization.BOXPLOT_MEAN_MODE_CODE[mComboBoxBoxplotMeanMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_VALUES, mCheckBoxShowMeanValues.isSelected() ? "true" : "false");
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		mComboBoxCorrelationType.setSelectedIndex(1+findListIndex(configuration.getProperty(PROPERTY_CORRELATION_TYPE),
																  CorrelationCalculator.TYPE_NAME, -1));
		mComboBoxLineMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MEAN_LINE_TYPE),
														 JVisualization2D.CURVE_MODE_CODE, 0));
		mCheckBoxStdDev.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_STDDEV)));
		mCheckBoxMultipleCurves.setSelected("true".equals(configuration.getProperty(PROPERTY_SPLIT_BY_CATEGORY)));

		mCheckBoxShowPValues.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_PVALUE)));
		mCheckBoxShowFoldChange.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE)));

		String pValueColumn = configuration.getProperty(PROPERTY_PVALUE_COLUMN);
		if (mSelectorPValueColumn instanceof JComboBox) {
			int column = getTableModel().findColumn(pValueColumn);
			if (getVisualization().isValidPValueColumn(column)) {
				((JComboBox)mSelectorPValueColumn).setSelectedItem(getTableModel().getColumnTitle(column));
				((JComboBox)mSelectorPValueRefCategory).setSelectedItem(configuration.getProperty(PROPERTY_PVALUE_REF_CATEGORY));
				}
			}
		else if (pValueColumn != null) {
			((JTextField)mSelectorPValueColumn).setText(pValueColumn);
			((JTextField)mSelectorPValueRefCategory).setText(configuration.getProperty(PROPERTY_PVALUE_REF_CATEGORY));
			}

		mComboBoxBoxplotMeanMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_MODE),
				 												JVisualization.BOXPLOT_MEAN_MODE_CODE, 3));
		mCheckBoxShowMeanValues.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_VALUES)));

		enableItems();
		}

	@Override
	public void setDialogToDefault() {
		mComboBoxCorrelationType.setSelectedIndex(0);
		mComboBoxLineMode.setSelectedIndex(0);
		mCheckBoxShowPValues.setSelected(false);
		mCheckBoxShowFoldChange.setSelected(false);
		((JTextField)mSelectorPValueColumn).setText("");
		((JTextField)mSelectorPValueRefCategory).setText("");
		mComboBoxBoxplotMeanMode.setSelectedIndex(JVisualization.BOXPLOT_DEFAULT_MEAN_MODE);
		}

	@Override
	public String getDialogTitle() {
		return "Set Statistical View Options";
		}

	@Override
	public Properties getRecentConfigurationLocal() {
		return sRecentConfiguration;
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		JVisualization2D v2d = (JVisualization2D)getVisualization();
		int correlationType = v2d.getShownCorrelationType();
		if (correlationType != -1)
			configuration.setProperty(PROPERTY_CORRELATION_TYPE, CorrelationCalculator.TYPE_NAME[correlationType]);

		configuration.setProperty(PROPERTY_MEAN_LINE_TYPE, JVisualization2D.CURVE_MODE_CODE[v2d.getCurveMode()]);
		configuration.setProperty(PROPERTY_SHOW_STDDEV, v2d.isShowStandardDeviation() ? "true" : "false");
		configuration.setProperty(PROPERTY_SPLIT_BY_CATEGORY, v2d.isCurveSplitByCategory() ? "true" : "false");

		int pValueColumn = v2d.getPValueColumn();
		if (pValueColumn != JVisualization.cColumnUnassigned) {
			configuration.setProperty(PROPERTY_PVALUE_COLUMN, getTableModel().getColumnTitleNoAlias(pValueColumn));
			configuration.setProperty(PROPERTY_PVALUE_REF_CATEGORY, v2d.getPValueRefCategory());
			configuration.setProperty(PROPERTY_BOXPLOT_SHOW_PVALUE, v2d.isShowPValue() ? "true" : "false");
			configuration.setProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE, v2d.isShowFoldChange() ? "true" : "false");
			}

		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_MODE, JVisualization.BOXPLOT_MEAN_MODE_CODE[v2d.getBoxplotMeanMode()]);
		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_VALUES, v2d.isShowMeanAndMedianValues() ? "true" : "false");
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel2D)view).getVisualization();
		visualization.setShownCorrelationType(findListIndex(configuration.getProperty(PROPERTY_CORRELATION_TYPE),
															 CorrelationCalculator.TYPE_NAME, -1));
		visualization.setCurveMode(findListIndex(configuration.getProperty(PROPERTY_MEAN_LINE_TYPE), JVisualization2D.CURVE_MODE_CODE, 0),
									"true".equals(configuration.getProperty(PROPERTY_SHOW_STDDEV)),
									"true".equals(configuration.getProperty(PROPERTY_SPLIT_BY_CATEGORY)));
		visualization.setPValueColumn(getTableModel().findColumn(configuration.getProperty(PROPERTY_PVALUE_COLUMN)), configuration.getProperty(PROPERTY_PVALUE_REF_CATEGORY));
		visualization.setShowPValue("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_PVALUE)));
		visualization.setShowFoldChange("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE)));
		visualization.setBoxplotMeanMode(findListIndex(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_MODE), JVisualization.BOXPLOT_MEAN_MODE_CODE, 0));
		visualization.setShowMeanAndMedianValues("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_VALUES)));
		}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}

	@Override
	public void enableItems() {
    	mCheckBoxStdDev.setEnabled(mComboBoxLineMode.getSelectedIndex() != 0);
    	mCheckBoxMultipleCurves.setEnabled(mComboBoxLineMode.getSelectedIndex() != 0);
    	mCheckBoxShowMeanValues.setEnabled(mComboBoxBoxplotMeanMode.getSelectedIndex() != 0);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		// update dynamic menus
	    if (e.getSource() == mSelectorPValueColumn) {
	    	((JComboBox)mSelectorPValueRefCategory).removeActionListener(this);
	    	String pValueColumn = (String)((JComboBox)mSelectorPValueColumn).getSelectedItem();
	    	((JComboBox)mSelectorPValueRefCategory).removeAllItems();
    		int column = getTableModel().findColumn(pValueColumn);
    		String[] categories = getTableModel().getCategoryList(column);
    		for (String category:categories)
    			((JComboBox)mSelectorPValueRefCategory).addItem(category);
	    	((JComboBox)mSelectorPValueRefCategory).addActionListener(this);
	        }

	    // super does handling of OK, Cancel, view updating, item enabling
	    super.actionPerformed(e);
		}
	}
