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

package com.actelion.research.table.filter;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.JStructureView;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableListener;
import com.actelion.research.table.CompoundTableModel;

public class JCategoryFilterPanel extends JFilterPanel
				implements ActionListener,CompoundTableListener,MouseListener {
	private static final long serialVersionUID = 0x20060821;

	public static final int cPreferredCheckboxCount = 16;
	public static final int cMaxCheckboxCount = 128;

	private String[]	mCategoryList;
	private JCheckBox[]	mCheckBox;
	private JTextArea	mTextArea;
	private JPanel		mCategoryOptionPanel;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
	public JCategoryFilterPanel(CompoundTableModel tableModel) {
		this(tableModel, 0, 0);
		}

	public JCategoryFilterPanel(CompoundTableModel tableModel, int columnIndex, int exclusionFlag) {
		super(tableModel, columnIndex, exclusionFlag, false);

		mCategoryOptionPanel = new JPanel();
		mCategoryOptionPanel.setOpaque(false);
		if (isActive()) {
			addCheckBoxes();
			addMouseListener(this);
			}
		else {
			addTextField();
			}
		add(mCategoryOptionPanel, BorderLayout.CENTER);

		mIsUserChange = true;
		}

	private void addTextField() {
		double[][] size = { {4, TableLayout.PREFERRED, 4},
							{TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4} };
		mCategoryOptionPanel.setLayout(new TableLayout(size));
		mTextArea = new JTextArea();
		mTextArea.setPreferredSize(new Dimension(300, 128));
		JScrollPane scrollPane = new JScrollPane(mTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		mCategoryOptionPanel.add(new JLabel("Excluded categories:"), "1,0");
		mCategoryOptionPanel.add(scrollPane, "1,2");
		}

	private void addCheckBoxes() {
		boolean isIDCode = CompoundTableModel.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(mColumnIndex));
		mCategoryList = mTableModel.getCategoryList(mColumnIndex);
		mCheckBox = new JCheckBox[mCategoryList.length];

		double[] sizeV = new double[mCategoryList.length+1];
		for (int i=0; i<mCategoryList.length; i++)
			sizeV[i] = (isIDCode && !CompoundTableConstants.cTextMultipleCategories.equals(mCategoryList[i])) ?
					Math.max(36, Math.min(100, (int)Math.sqrt(200*mCategoryList[i].length()))) : 18;
		sizeV[mCategoryList.length] = 4;
		double[] sizeH = isIDCode ? new double[2] : new double[1];
		sizeH[0] = isIDCode ? 24 : TableLayout.PREFERRED;
		if (isIDCode)
			sizeH[1] = TableLayout.PREFERRED;
		double[][] size = { sizeH, sizeV };
		mCategoryOptionPanel.setLayout(new TableLayout(size));

		for (int i=0; i<mCategoryList.length; i++) {
			String categoryName = isIDCode ? "" : mCategoryList[i];
			if (categoryName.length() > 32)
				categoryName = categoryName.substring(0, 30) + " ...";
			mCheckBox[i] = new JCheckBox(categoryName, true);
			mCheckBox[i].addMouseListener(this);
			mCheckBox[i].setActionCommand("cat"+i);
			mCheckBox[i].addActionListener(this);
			mCategoryOptionPanel.add(mCheckBox[i], "0,"+i);
			if (isIDCode) {
				String idcode = mCategoryList[i];
				if (idcode != null && idcode.length() != 0) {
					if (idcode.equals(CompoundTableConstants.cTextMultipleCategories)) {
						mCategoryOptionPanel.add(new JLabel("multiple ring systems"), "1,"+i);
						}
					else {
						int index = idcode.indexOf(' ');
						JStructureView view = new JStructureView(DnDConstants.ACTION_COPY_OR_MOVE, 0);
						view.setClipboardHandler(new ClipboardHandler());
						if (index == -1)
							view.setIDCode(idcode);
						else
							view.setIDCode(idcode.substring(0, index), idcode.substring(index+1));
						view.setPreferredSize(new Dimension(120, 48));
						mCategoryOptionPanel.add(view, "1, "+i);
						}
					}
				}
			}
		}

	public boolean isFilterEnabled() {
		return true;
		}

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		handlePopupTrigger(e);
		}

	public void mouseReleased(MouseEvent e) {
		handlePopupTrigger(e);
		}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.startsWith("Select All")) {
			boolean update = false;
			for (int i=0; i<mCheckBox.length; i++) {
				if (!mCheckBox[i].isSelected()) {
					mCheckBox[i].setSelected(true);
					update = true;
					}
				}
			if (update)
				updateExclusion();
			return;
			}
		if (command.startsWith("Deselect All")) {
			boolean update = false;
			for (int i=0; i<mCheckBox.length; i++) {
				if (mCheckBox[i].isSelected()) {
					mCheckBox[i].setSelected(false);
					update = true;
					}
				}
			if (update)
				updateExclusion();
			return;
			}
		if (command.startsWith("cat")) {
			updateExclusion();
			return;
			}

		super.actionPerformed(e);
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);

		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows
		 || (e.getType() == CompoundTableEvent.cChangeColumnData && e.getSpecifier() == mColumnIndex)) {
			if (!mTableModel.isColumnTypeCategory(mColumnIndex)) {
				removePanel();
				return;
				}
			if (updateCheckboxes() && e.getType() != CompoundTableEvent.cDeleteRows)
				updateExclusion();
			}

		mIsUserChange = true;
		}

	private void handlePopupTrigger(MouseEvent e) {
		if (mCheckBox.length != 0 && e.isPopupTrigger()) {
			JPopupMenu popup = new JPopupMenu();
			JMenuItem item = new JMenuItem("Select All");
			item.addActionListener(this);
			popup.add(item);
			item = new JMenuItem("Deselect All");
			item.addActionListener(this);
			int x = e.getX();
			int y = e.getY();
			if (e.getSource() instanceof JCheckBox) {
				x += ((JCheckBox)e.getSource()).getX();
				y += ((JCheckBox)e.getSource()).getY();
				}
			popup.add(item);
			popup.show(this, x, y);
			}
		}

	private boolean updateCheckboxes() {
		String[] newCategoryList = mTableModel.getCategoryList(mColumnIndex);
		if (newCategoryList.length <= 1
		 || newCategoryList.length > cMaxCheckboxCount) {
			removePanel();
			return false;
			}

		String[] oldCategoryList = mCategoryList;
		boolean categoriesChanged = (newCategoryList.length != oldCategoryList.length);
		if (!categoriesChanged) {
			for (int i=0; i<newCategoryList.length; i++) {
				if (!newCategoryList[i].equals(oldCategoryList[i])) {
					categoriesChanged = true;
					break;
					}
				}
			}
		
		if (categoriesChanged) {
			JCheckBox[] oldCheckBox = mCheckBox;
			mCategoryOptionPanel.removeAll();
			addCheckBoxes();
			for (int i=0; i<mCategoryList.length; i++) {
				int oldIndex = -1;
				for (int j=0; j<oldCategoryList.length; j++) {
					if (newCategoryList[i].equals(oldCategoryList[j])) {
						oldIndex = j;
						break;
						}
					}
				if (oldIndex != -1 && !oldCheckBox[oldIndex].isSelected())
					mCheckBox[i].setSelected(false);
				}
			}
		getParent().getParent().validate();
		return true;
		}

	@Override
	public String getInnerSettings() {
		String settings = null;
		if (isActive()) {
			for (int i=0; i<mCheckBox.length; i++)
				if (!mCheckBox[i].isSelected())
					settings = (settings == null) ?
							mCheckBox[i].getText() : settings+"\t"+mCheckBox[i].getText();
			}
		else {
			settings = mTextArea.getText().replace('\n', '\t');
			int index = settings.length();	// remove trailing TABs
			while (index>0 && settings.charAt(index-1) == '\t')
				index--;
			if (index < settings.length())
				settings = settings.substring(0, index);
			}
		return settings;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (isActive()) {
			int index = 0;
			while (index != -1) {
				int index2 = settings.indexOf('\t', index);
				if (index2 == -1) {
					exclude(settings.substring(index));
					index = -1;
					}
				else {
					exclude(settings.substring(index, index2));
					index = index2+1;
					}
				}
			updateExclusion();
			}
		else {
			mTextArea.setText(settings.replace('\t', '\n'));
			}
		}

	@Override
	public void innerReset() {
		if (isActive()) {
			boolean found = false;
			for (int i=0; i<mCheckBox.length; i++) {
				if (!mCheckBox[i].isSelected()) {
					mCheckBox[i].setSelected(true);
					found = true;
					}
				}
			if (found)
				mTableModel.clearCompoundFlag(mExclusionFlag);
			}
		else {
			mTextArea.setText("");
			}
		}

	private void updateExclusion() {
		boolean[] selected = new boolean[mCheckBox.length];
		for (int j=0; j<mCheckBox.length; j++)
			selected[j] = mCheckBox[j].isSelected();

		if (isActive()) {
			mTableModel.setCategoryExclusion(mExclusionFlag, mColumnIndex, selected, isInverse());
			fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			}
		}

	private void exclude(String category) {
		for (int i=0; i<mCheckBox.length; i++) {
			if (mCheckBox[i].getText().equals(category)) {
				mCheckBox[i].setSelected(false);
				break;
				}
			}
		}
	}
