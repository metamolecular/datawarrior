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

package com.actelion.research.datawarrior.task.filter;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.filter.JCategoryBrowser;
import com.actelion.research.table.filter.JCategoryFilterPanel;
import com.actelion.research.table.filter.JDoubleFilterPanel;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.filter.JHitlistFilterPanel;
import com.actelion.research.table.filter.JMultiStructureFilterPanel;
import com.actelion.research.table.filter.JReactionFilterPanel;
import com.actelion.research.table.filter.JSingleStructureFilterPanel;
import com.actelion.research.table.filter.JStringFilterPanel;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2013
 * Company:
 * @author
 * @version 1.0
 */

public class DETaskAddNewFilter extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Create Filter";

    private static final String[] FILTER_NAME = {
    	"[Text]",
    	"[Slider]",
    	"[Category]",
    	"[Structure]",
    	"[Structure List,SSS]",
    	"[Structure List,Similarity]",
    	"[Reaction]",
    	"[Row List]",
    	"[Category Browser]"
    	};

    private static final String[] FILTER_CODE = {
    	"text",
    	"slider",
    	"category",
    	"structure",
    	"sssList",
    	"simList",
    	"reaction",
    	"list",
    	"browser"
    	};

    private static final boolean[] FILTER_NEEDS_COLUMN = {
    	true,
    	true,
    	true,
    	true,
    	true,
    	true,
    	true,
    	false,
    	false
    	};

    private static final String PROPERTY_SHOW_DUPLICATES = "showDuplicates";
    private static final String PROPERTY_FILTER_COUNT = "filterCount";
    private static final String PROPERTY_FILTER = "filter";
    
    private static final int TYPE_STRING = 0;
	private static final int TYPE_DOUBLE = 1;
	private static final int TYPE_CATEGORY = 2;
	private static final int TYPE_STRUCTURE = 3;
	private static final int TYPE_SSS_LIST = 4;
	private static final int TYPE_SIM_LIST = 5;
    private static final int TYPE_REACTION = 6;
    private static final int TYPE_ROWLIST = 7;
    private static final int TYPE_CATEGORY_BROWSER = 8;

    private static Properties sRecentConfiguration;

	private CompoundTableModel  mTableModel;
	private DEPruningPanel      mPruningPanel;
	private JList				mFilterList;
	private JCheckBox			mCheckBox;
	private JTextArea			mTextArea;
	private boolean				mIsInteractive;

    public DETaskAddNewFilter(DEFrame parent, boolean isInteractive) {
		super(parent, false);

		mTableModel = parent.getTableModel();
		mPruningPanel = parent.getMainFrame().getPruningPanel();
		mIsInteractive = isInteractive;
    	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBox) {
			populateFilterList(mCheckBox.isSelected());
			return;
			}
		}

	private void populateFilterList(boolean allowDuplicates) {
        ArrayList<String> itemList = new ArrayList<String>();

        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            if (mTableModel.isColumnTypeCategory(column)) {
                addItem(itemList, -1, TYPE_CATEGORY_BROWSER, allowDuplicates);
                break;
                }
            }

        for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
            String specialType = mTableModel.getColumnSpecialType(i);
            if (specialType != null) {
                if (specialType.equals(CompoundTableModel.cColumnTypeIDCode)) {
                    addItem(itemList, i, TYPE_STRUCTURE, allowDuplicates);
                    addItem(itemList, i, TYPE_SSS_LIST, allowDuplicates);
                    addItem(itemList, i, TYPE_SIM_LIST, allowDuplicates);
                	}
                if (specialType.equals(CompoundTableModel.cColumnTypeRXNCode))
                    addItem(itemList, i, TYPE_REACTION, allowDuplicates);
                }
            else {
   				addItem(itemList, i, TYPE_STRING, allowDuplicates);
    
    		    if (mTableModel.isColumnTypeDouble(i)
                 && mTableModel.hasNumericalVariance(i))
    				addItem(itemList, i, TYPE_DOUBLE, allowDuplicates);
    
    		    if (mTableModel.isColumnTypeCategory(i)
    		     && mTableModel.getCategoryCount(i) < JCategoryFilterPanel.cMaxCheckboxCount)
    				addItem(itemList, i, TYPE_CATEGORY, allowDuplicates);
    
    		    if (mTableModel.isColumnTypeRangeCategory(i))
    				addItem(itemList, i, TYPE_DOUBLE, allowDuplicates);
                }
			}

		if (mTableModel.getHitlistHandler().getHitlistCount() > 0)
			addItem(itemList, -1, TYPE_ROWLIST, allowDuplicates);

		Collections.sort(itemList, String.CASE_INSENSITIVE_ORDER);
        mFilterList.setListData(itemList.toArray());
		}

	private void addItem(ArrayList<String> itemList, int column, int type, boolean allowDuplicates) {
		if (!allowDuplicates) {
			for (int i=0; i<mPruningPanel.getFilterCount(); i++) {
				JFilterPanel filter = mPruningPanel.getFilter(i);
				if (type == TYPE_ROWLIST) {
					if (filter instanceof JHitlistFilterPanel)
						return;
					}
                else if (type == TYPE_CATEGORY_BROWSER) {
                    if (filter instanceof JCategoryBrowser)
                        return;
                    }
				else if (filter.getColumnIndex() == column) {
					switch (type) {
                        case TYPE_STRUCTURE:
                            if (filter instanceof JSingleStructureFilterPanel)
                                return;
                            break;
                        case TYPE_SSS_LIST:
                            if (filter instanceof JMultiStructureFilterPanel
                       		 && ((JMultiStructureFilterPanel)filter).supportsSSS())
                                return;
                            break;
                        case TYPE_SIM_LIST:
                            if (filter instanceof JMultiStructureFilterPanel
                      		 && ((JMultiStructureFilterPanel)filter).supportsSim())
                                return;
                            break;
                        case TYPE_REACTION:
                            if (filter instanceof JReactionFilterPanel)
                                return;
                            break;
                        case TYPE_STRING:
							if (filter instanceof JStringFilterPanel)
								return;
							break;
						case TYPE_DOUBLE:
							if (filter instanceof JDoubleFilterPanel)
								return;
							break;
						case TYPE_CATEGORY:
							if (filter instanceof JCategoryFilterPanel)
								return;
							break;
						}
					}
				}
			}

		itemList.add(getFilterDisplayName(type, column));
		}

	private String getFilterDisplayName(int type, int column) {
		if (!FILTER_NEEDS_COLUMN[type])
			return FILTER_NAME[type];

		String columnName = (column < 0) ? null : mTableModel.getColumnTitleExtended(column);

		if (type == TYPE_DOUBLE) {
			String text = columnName+" "+FILTER_NAME[type];
            return mTableModel.isColumnDataComplete(column) ? text : text+" ";
			}

		return columnName+" "+FILTER_NAME[type];
		}

	private void addDefaultDescriptor(int column) {
        for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
            if (mTableModel.getParentColumn(i) == column
             && mTableModel.isDescriptorColumn(i))
                return;

        if (CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column)))
            mTableModel.createDescriptorColumn(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
        else if (CompoundTableModel.cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column)))
            mTableModel.createDescriptorColumn(column, DescriptorConstants.DESCRIPTOR_ReactionIndex.shortName);
        }

	@Override
	public boolean isConfigurable() {
		return mTableModel.getTotalColumnCount() != 0;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JScrollPane scrollPane = null;

		if (mIsInteractive) {
	        mFilterList = new JList();
	        mFilterList.setCellRenderer(new DefaultListCellRenderer() {
	            private static final long serialVersionUID = 0x20110526;
	
	            public Component getListCellRendererComponent(JList list,
	            	      Object value, int index, boolean isSelected, boolean cellHasFocus) {
	            	Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	
	            	if (((String)value).endsWith(" "))
	            		renderer.setForeground(Color.RED);
	            	return renderer;
	            	}
	        	});
	        scrollPane = new JScrollPane(mFilterList);
	        scrollPane.setPreferredSize(new Dimension(320,240));
	
			mCheckBox = new JCheckBox("Show duplicate filters");
			mCheckBox.addActionListener(this);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			scrollPane.setPreferredSize(new Dimension(320, 128));
			}

		JPanel content = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		content.setLayout(new TableLayout(size));
		String syntax = mIsInteractive ? "Column Name [Type]" : "type:column name";
		content.add(new JLabel("New Filters (as '"+syntax+"'):"), "1,1");
		content.add(scrollPane, "1, 3");

		if (mIsInteractive)
			content.add(mCheckBox, "1, 5");

		return content;
		}

	@Override
	public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	if (mIsInteractive) {
        	if (mCheckBox.isSelected())
        	    configuration.setProperty(PROPERTY_SHOW_DUPLICATES, "true");

        	Object[] selectedFilters = mFilterList.getSelectedValues();
	
		    configuration.setProperty(PROPERTY_FILTER_COUNT, ""+selectedFilters.length);
	
			for (int filter=0; filter<selectedFilters.length; filter++) {
				String selected = ((String)selectedFilters[filter]).trim();	// get rid of color indication
	
				int type = getFilterTypeFromName(selected);
	
				String columnName = null;
				if (FILTER_NEEDS_COLUMN[type])
					columnName = mTableModel.getColumnTitleNoAlias(selected.substring(0, selected.length()-FILTER_NAME[type].length()-1));

				configuration.setProperty(PROPERTY_FILTER+filter, (columnName == null) ? FILTER_CODE[type] : FILTER_CODE[type]+":"+columnName);
				}
    		}
    	else {
    		String[] filterDefList = mTextArea.getText().split("\\n");
		    configuration.setProperty(PROPERTY_FILTER_COUNT, ""+filterDefList.length);
			for (int filter=0; filter<filterDefList.length; filter++)
				configuration.setProperty(PROPERTY_FILTER+filter, filterDefList[filter]);
    		}

		return configuration;
		}

	private int getFilterTypeFromName(String filterName) {
		for (int i=0; i<FILTER_NAME.length; i++)
			if (filterName.endsWith(FILTER_NAME[i]))
				return i;
		return -1;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int filterCount = Integer.parseInt(configuration.getProperty(PROPERTY_FILTER_COUNT, "0"));

		if (mIsInteractive) {
			mCheckBox.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_DUPLICATES)));

			populateFilterList(mCheckBox.isSelected());
	
			mFilterList.clearSelection();
			for (int filter=0; filter<filterCount; filter++) {
				String filterDef = configuration.getProperty(PROPERTY_FILTER+filter);
				int type = getFilterTypeFromCode(filterDef);
				if (type != -1) {
					int column = getFilterColumn(type, filterDef);
					String displayName = getFilterDisplayName(type, column);
					for (int i=0; i<mFilterList.getModel().getSize(); i++) {
						if (mFilterList.getModel().getElementAt(i).equals(displayName)) {
							mFilterList.addSelectionInterval(i, i);
							break;
							}
						}
					}
				}
			}
		else {
			StringBuilder sb = new StringBuilder();
			for (int filter=0; filter<filterCount; filter++) {
				String filterDef = configuration.getProperty(PROPERTY_FILTER+filter);
				if (filterDef != null)
					sb.append(filterDef).append('\n');
				}
			mTextArea.setText(sb.toString());
			}
		}

	private int getFilterTypeFromCode(String def) {
		if (def != null)
			for (int i=0; i<FILTER_CODE.length; i++)
				if (def.equals(FILTER_CODE[i]) || def.startsWith(FILTER_CODE[i]+":"))
					return i;
		return -1;
		}

	/**
	 * Checks whether a column name is given, whether the column can be found
	 * and whether a column is needed with this filter type. Return values are:<br>
	 * >=0: column needed, name given and column found<br>
	 * -1: column needed, name given, but not found<br>
	 * -2: column needed, no name given<br>
	 * -3: column not needed, but name given<br>
	 * -4: column not needed and no name given<br>
	 * @param type
	 * @param def
	 * @return valid column or negative result code
	 */
	private int getFilterColumn(int type, String def) {
		if (FILTER_NEEDS_COLUMN[type]) {
			if (def.length() < FILTER_CODE[type].length()+2)
				return -2;
			return mTableModel.findColumn(def.substring(FILTER_CODE[type].length()+1));
			}
		return (def.length() < FILTER_CODE[type].length()+2) ? -4 : -3;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mIsInteractive) {
			mCheckBox.setSelected(false);
			populateFilterList(mCheckBox.isSelected());
			}
		else {
			mTextArea.setText("");
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		int filterCount = Integer.parseInt(configuration.getProperty(PROPERTY_FILTER_COUNT, "0"));
		if (filterCount == 0) {
			showErrorMessage("No filters defined.");
			return false;
			}
		for (int filter=0; filter<filterCount; filter++) {
			String def = configuration.getProperty(PROPERTY_FILTER+filter, "");
			int type = getFilterTypeFromCode(def);
			if (type == -1) {
				StringBuilder sb = new StringBuilder(FILTER_CODE[0]);
				for (int i=1; i<FILTER_CODE.length; i++)
					sb.append(", ").append(FILTER_CODE[i]);
				showErrorMessage("No valid filter type found in '"+def+"'.\nValid filter types are: "+sb.toString()+".");
				return false;
				}
			int column = getFilterColumn(type, def);
			if (column == -2) {
				showErrorMessage("Column name missing. "+FILTER_NAME[type]+" filters must be defined as '"+FILTER_CODE[type]+":<column name>'.");
				return false;
				}
			if (column == -3) {
				showErrorMessage("Superflous column name specified. "+FILTER_NAME[type]+" filters must be specified as '"+FILTER_CODE[type]+"'.");
				return false;
				}
			if (isLive) {
				if (column == -1) {
					showErrorMessage("Column '"+def.substring(FILTER_CODE[type].length()+1)+"' not found.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int filterCount = Integer.parseInt(configuration.getProperty(PROPERTY_FILTER_COUNT, "0"));
		int foundCount = 0;
		for (int filter=0; filter<filterCount; filter++) {
			String filterDef = configuration.getProperty(PROPERTY_FILTER+filter);
			int type = getFilterTypeFromCode(filterDef);
			if (type != -1) {
				int column = getFilterColumn(type, filterDef);
				if (!FILTER_NEEDS_COLUMN[type] || column >= 0) {
					try {
						switch (type) {
						case TYPE_STRING:
							mPruningPanel.addStringFilter(mTableModel, column);
							break;
						case TYPE_DOUBLE:
							mPruningPanel.addDoubleFilter(mTableModel, column);
							break;
						case TYPE_CATEGORY:
							mPruningPanel.addCategoryFilter(mTableModel, column);
							break;
						case TYPE_STRUCTURE:
		                    addDefaultDescriptor(column);
		                    mPruningPanel.addStructureFilter(mTableModel, column, null);
							break;
						case TYPE_SSS_LIST:
		                    addDefaultDescriptor(column);
		                    mPruningPanel.addStructureListFilter(mTableModel, column, true);
							break;
						case TYPE_SIM_LIST:
		                    addDefaultDescriptor(column);
		                    mPruningPanel.addStructureListFilter(mTableModel, column, false);
							break;
						case TYPE_REACTION:
		                    addDefaultDescriptor(column);
		                    mPruningPanel.addReactionFilter(mTableModel, column, null);
							break;
						case TYPE_ROWLIST:
							mPruningPanel.addHitlistFilter(mTableModel);
							break;
						case TYPE_CATEGORY_BROWSER:
		                    mPruningPanel.addCategoryBrowser(mTableModel);
		                	break;
							}
		
						foundCount++;
						}
					catch (DEPruningPanel.FilterException fpe) {
						showErrorMessage(fpe.getMessage());
						}
					}
				}
			}

		if (foundCount != 0) {
			mPruningPanel.getParentPane().fireRuntimePropertyChanged(
					new RuntimePropertyEvent(mPruningPanel, RuntimePropertyEvent.TYPE_ADD_FILTER, -1));
			}
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
