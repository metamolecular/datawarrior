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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.table.CompoundTableModel;

public class DETaskNewView extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "New View";

	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_NEW_VIEW = "newView";
	private static final String PROPERTY_WHERE_VIEW = "whereView";
	private static final String PROPERTY_WHERE = "where";
	private static final String PROPERTY_REF_COLUMN = "refColumn";

	private static final String[] TEXT_RELATION = { "Center",  "Top", "Left", "Bottom", "Right" };
	private static final String[] CODE_WHERE = { "center",  "top", "left", "bottom", "right" };

	private static Properties sRecentConfiguration;

	private DEMainPane	mMainPane;
	private JTextField	mTextFieldViewName;
	private JComboBox	mComboBoxType,mComboBoxView,mComboBoxRefColumn,mComboBoxWhere;
	private int			mViewType,mRefColumn;
	private String		mWhereViewName;

	public DETaskNewView(Frame parent, DEMainPane	mainPane, int viewType, String whereViewName, int refColumn) {
		super(parent, false);
		mMainPane = mainPane;
		mViewType = viewType;
		mWhereViewName = whereViewName;
		mRefColumn = refColumn;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));
/*
for (Dockable d:mMainPane.getDockables())
 System.out.println("isVisible:"+d.isVisible()+" isVisibleDockable:"+d.isVisibleDockable());
*/
		p.add(new JLabel("New View type:"), "1,1");
		mComboBoxType = new JComboBox(DEMainPane.VIEW_TYPE_ITEM);
		mComboBoxType.addActionListener(this);
		p.add(mComboBoxType, "3,1");

		p.add(new JLabel("New View name:"), "1,3");
		mTextFieldViewName = new JTextField();
		p.add(mTextFieldViewName, "3,3,5,3");

		p.add(new JLabel("Relative to view:"), "1,5");
		mComboBoxView = new JComboBox();
		for (Dockable d:mMainPane.getDockables())
			if (d.isVisibleDockable())
				mComboBoxView.addItem(d.getTitle());
		mComboBoxView.setEditable(true);
		p.add(mComboBoxView, "3,5");

		p.add(new JLabel(" at "), "4,5");
		mComboBoxWhere = new JComboBox(TEXT_RELATION);
		p.add(mComboBoxWhere, "5,5");

		p.add(new JLabel("Structure column:"), "1,7");
		mComboBoxRefColumn = new JComboBox();
		int[] column = mMainPane.getTableModel().getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (column != null)
			for (int i:column)
				mComboBoxRefColumn.addItem(mMainPane.getTableModel().getColumnTitle(i));
		mComboBoxRefColumn.setEnabled(false);
		mComboBoxRefColumn.setEditable(true);
		p.add(mComboBoxRefColumn, "3,7");

		return p;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxType) {
			mComboBoxRefColumn.setEnabled(DEMainPane.VIEW_TYPE_ITEM[DEMainPane.VIEW_TYPE_STRUCTURE].equals(
					mComboBoxType.getSelectedItem()));
			return;
			}
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mWhereViewName == null || mViewType == -1)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_NEW_VIEW, mMainPane.getDefaultViewName(mViewType, mRefColumn));
		configuration.setProperty(PROPERTY_WHERE_VIEW, mWhereViewName);
		configuration.setProperty(PROPERTY_WHERE, CODE_WHERE[0]);
		configuration.setProperty(PROPERTY_TYPE, DEMainPane.VIEW_TYPE_CODE[mViewType]);
		if (mRefColumn != -1)
			configuration.setProperty(PROPERTY_REF_COLUMN, mMainPane.getTableModel().getColumnTitleNoAlias(mRefColumn));
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_NEW_VIEW, mTextFieldViewName.getText());
		configuration.setProperty(PROPERTY_TYPE, DEMainPane.VIEW_TYPE_CODE[mComboBoxType.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_WHERE_VIEW, (String)mComboBoxView.getSelectedItem());
		configuration.setProperty(PROPERTY_WHERE, CODE_WHERE[mComboBoxWhere.getSelectedIndex()]);
		if (DEMainPane.VIEW_TYPE_ITEM[DEMainPane.VIEW_TYPE_STRUCTURE].equals(mComboBoxType.getSelectedItem())
		 && mComboBoxRefColumn.getSelectedItem() != null)
			configuration.setProperty(PROPERTY_REF_COLUMN, mMainPane.getTableModel().getColumnTitleNoAlias((String)mComboBoxRefColumn.getSelectedItem()));
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldViewName.setText(configuration.getProperty(PROPERTY_NEW_VIEW, "Untitled View"));
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), DEMainPane.VIEW_TYPE_CODE, DEMainPane.VIEW_TYPE_2D);
		mComboBoxType.setSelectedIndex(type);
		String whereViewName = configuration.getProperty(PROPERTY_WHERE_VIEW);
		mComboBoxView.setSelectedItem(whereViewName);
		int where = findListIndex(configuration.getProperty(PROPERTY_WHERE), CODE_WHERE, 0);
		mComboBoxWhere.setSelectedIndex(where);
		String refColumn = configuration.getProperty(PROPERTY_REF_COLUMN);
		if (refColumn != null)
			mComboBoxRefColumn.setSelectedItem(refColumn);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldViewName.setText("Untitled 2D-View");
		mComboBoxType.setSelectedIndex(DEMainPane.VIEW_TYPE_2D);
		mComboBoxView.setSelectedItem(mMainPane.getSelectedViewTitle());
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), DEMainPane.VIEW_TYPE_CODE, DEMainPane.VIEW_TYPE_2D);
		String viewName = configuration.getProperty(PROPERTY_WHERE_VIEW);
		if (viewName == null) {
			showErrorMessage("View name not defined.");
			return false;
			}
		if (isLive) {
			Dockable dockable = mMainPane.getDockable(viewName);
			if (dockable == null) {
				showErrorMessage("View '"+viewName+"' not found.");
				return false;
				}
	/*		if (!dockable.isVisibleDockable()) {	this should not be an issue
				showErrorMessage("View '"+viewName+"' is not visible.");
				return false;
				}*/
			if (type == DEMainPane.VIEW_TYPE_STRUCTURE) {
				if (mMainPane.getTableModel().getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode) == null) {
					showErrorMessage("No structure columns found.");
					return false;
					}
				String refColumn = configuration.getProperty(PROPERTY_REF_COLUMN);
				if (refColumn != null) {
					int column = mMainPane.getTableModel().findColumn(refColumn);
					if (column == -1) {
						showErrorMessage("Structure column '"+refColumn+"' not found.");
						return false;
						}
					if (!CompoundTableModel.cColumnTypeIDCode.equals(mMainPane.getTableModel().getColumnSpecialType(column))) {
						showErrorMessage(refColumn+" does not contain chemical structures.");
						return false;
						}
					}
				}
			}
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), DEMainPane.VIEW_TYPE_CODE, DEMainPane.VIEW_TYPE_2D);
		String viewName = configuration.getProperty(PROPERTY_NEW_VIEW);
		String whereView = configuration.getProperty(PROPERTY_WHERE_VIEW);
		int where = findListIndex(configuration.getProperty(PROPERTY_WHERE), CODE_WHERE, 0);
		int refColumn = mMainPane.getTableModel().findColumn(configuration.getProperty(PROPERTY_REF_COLUMN));
		if (type == DEMainPane.VIEW_TYPE_STRUCTURE && refColumn == -1)
			refColumn = mMainPane.getTableModel().getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode)[0];
		mMainPane.createNewView(viewName, type, whereView, CODE_WHERE[where], refColumn);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public Properties getRecentConfiguration() {
		return sRecentConfiguration;
		}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}
	}
