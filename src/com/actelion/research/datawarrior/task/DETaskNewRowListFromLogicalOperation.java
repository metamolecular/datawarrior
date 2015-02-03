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

package com.actelion.research.datawarrior.task;

import info.clearthought.layout.TableLayout;

import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableHitlistHandler;


public class DETaskNewRowListFromLogicalOperation extends ConfigurableTask {
	public static final String TASK_NAME = "New Row List From Logical Operation";

	private static final String PROPERTY_OPERATION = "operation";
	private static final String PROPERTY_NEW_HITLIST = "newList";
	private static final String PROPERTY_HITLIST_1 = "list1";
	private static final String PROPERTY_HITLIST_2 = "list2";

	private static final String DEFAULT_LIST_NAME = "Unnamed List";

    private static Properties sRecentConfiguration;

	private CompoundTableHitlistHandler	mHitlistHandler;
	private JComboBox					mComboBoxHitlist1,mComboBoxHitlist2,mComboBoxOperation;
	private JTextField					mTextFieldName;

    public DETaskNewRowListFromLogicalOperation(DEFrame parent) {
		super(parent, true);
		mHitlistHandler = parent.getTableModel().getHitlistHandler();
	    }

	@Override
	public boolean isConfigurable() {
		String[] names = mHitlistHandler.getHitlistNames();
		if (names == null || names.length < 2) {
			showErrorMessage("Less than two row lists found.");
			return false;
			}
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel mp = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8} };
		mp.setLayout(new TableLayout(size));

		String[] hitlistNames = mHitlistHandler.getHitlistNames();

		mTextFieldName = new JTextField();
		mp.add(new JLabel("New list name:"), "1, 1");
		mp.add(mTextFieldName, "3, 1");

		mComboBoxHitlist1 = new JComboBox(hitlistNames);
		mp.add(new JLabel("1st list:"), "1, 3");
		mp.add(mComboBoxHitlist1, "3, 3");

		mComboBoxHitlist2 = new JComboBox(hitlistNames);
		mp.add(new JLabel("2nd list:"), "1, 5");
		mp.add(mComboBoxHitlist2, "3, 5");

		mComboBoxOperation = new JComboBox(HitlistOptionRenderer.OPERATION_TEXT);
		mComboBoxOperation.setRenderer(new HitlistOptionRenderer());
		mp.add(new JLabel("Operation:"), "1, 7");
		mp.add(mComboBoxOperation, "3, 7");

		return mp;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_NEW_HITLIST, mTextFieldName.getText());
		configuration.setProperty(PROPERTY_HITLIST_1, (String)mComboBoxHitlist1.getSelectedItem());
		configuration.setProperty(PROPERTY_HITLIST_2, (String)mComboBoxHitlist2.getSelectedItem());
		configuration.setProperty(PROPERTY_OPERATION, HitlistOptionRenderer.OPERATION_CODE[mComboBoxOperation.getSelectedIndex()]);
        return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldName.setText(configuration.getProperty(PROPERTY_NEW_HITLIST, ""));
		int hitlist1 = mHitlistHandler.getHitlistIndex(configuration.getProperty(PROPERTY_HITLIST_1, ""));
		mComboBoxHitlist1.setSelectedIndex(hitlist1 == -1 ? 0 : hitlist1);
		int hitlist2 = mHitlistHandler.getHitlistIndex(configuration.getProperty(PROPERTY_HITLIST_2, ""));
		mComboBoxHitlist2.setSelectedIndex(hitlist2 == -1 ? 0 : hitlist2);
		mComboBoxOperation.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_OPERATION), HitlistOptionRenderer.OPERATION_CODE, 0));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldName.setText(DEFAULT_LIST_NAME);
		mComboBoxHitlist1.setSelectedIndex(0);
		mComboBoxHitlist2.setSelectedIndex(1);
		mComboBoxOperation.setSelectedIndex(0);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (findListIndex(configuration.getProperty(PROPERTY_OPERATION), HitlistOptionRenderer.OPERATION_CODE, -1) == -1) {
			showErrorMessage("Logical operation not found.");
			return false;
			}
		if (configuration.getProperty(PROPERTY_NEW_HITLIST, "").length() == 0) {
			showErrorMessage("No list name specified.");
			return false;
			}

		if (isLive) {
			String hitlistName1 = configuration.getProperty(PROPERTY_HITLIST_1, "");
			int hitlist1 = mHitlistHandler.getHitlistIndex(hitlistName1);
			if (hitlist1 == -1) {
				showErrorMessage("Row list '"+hitlistName1+"' not found.");
				return false;
				}
			String hitlistName2 = configuration.getProperty(PROPERTY_HITLIST_2, "");
			int hitlist2 = mHitlistHandler.getHitlistIndex(hitlistName2);
			if (hitlist2 == -1) {
				showErrorMessage("Row list '"+hitlistName2+"' not found.");
				return false;
				}
			if (hitlist1 == hitlist2) {
				showErrorMessage("One list is selected twice.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int hitlist1 = mHitlistHandler.getHitlistIndex(configuration.getProperty(PROPERTY_HITLIST_1));
		int hitlist2 = mHitlistHandler.getHitlistIndex(configuration.getProperty(PROPERTY_HITLIST_2));
		int operation = findListIndex(configuration.getProperty(PROPERTY_OPERATION), HitlistOptionRenderer.OPERATION_CODE, 0);

		if (mHitlistHandler.createHitlist(mTextFieldName.getText(), hitlist1, hitlist2, operation) == null)
			showErrorMessage("The maximum number of filters/lists is reached.");
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
