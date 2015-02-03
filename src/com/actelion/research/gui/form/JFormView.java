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

package com.actelion.research.gui.form;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.print.*;
import java.util.*;
import javax.swing.*;
import info.clearthought.layout.*;

public class JFormView extends JPanel implements FormObjectListener,Printable {
	private static final long serialVersionUID = 0x20061016;

	private ArrayList<AbstractFormObject> mComponentList;
	private FormModel			mFormModel;
	private double[][]			mLayoutDesc;
	private FormObjectFactory	mObjectFactory;
	private boolean				mEditable,mBlockEvents;

	public JFormView() {
		mObjectFactory = new FormObjectFactory();
		mComponentList = new ArrayList<AbstractFormObject>();
		}

	public void setModel(FormModel model) {
		mFormModel = model;
		}

	public FormModel getModel() {
		return mFormModel;
		}

	public void setEditable(boolean b) {
		if (mEditable != b) {
			mEditable = b;
			mFormModel.setEditable(b);
			for (AbstractFormObject afo:mComponentList) {
				afo.setEditable(b);
				afo.setData(mFormModel.getValue(afo.getKey()));
				}
			repaint();
			}
		}

	public void addSupportedType(String typeString, Class<?> formObjectClass) {
		mObjectFactory.addSupportedType(typeString, formObjectClass);
		}

	public AbstractFormObject addFormObject(String key, String type) {
			// works together with a final call to createDefaultLayout()
			// which then physically adds all form objects to the form
			// this function does not set set the form objects initial content
		AbstractFormObject formObject = mObjectFactory.createFormObject(key, type);
		if (formObject != null) {
			formObject.setEditable(mEditable);
			formObject.addFormObjectListener(this);
			mComponentList.add(formObject);
			}

		return formObject;
		}

	/**
	 * Adds the form object immediately and sets its initial content.
	 * @param key
	 * @param type
	 * @param constraint table layout constraint to place the form object
	 * @return constructed and added form object
	 */
	public AbstractFormObject addFormObject(String key, String type, String constraint) {
		AbstractFormObject formObject = addFormObject(key, type);
		addObject(formObject, constraint);
		formObject.setEditable(mEditable);
		formObject.addFormObjectListener(this);
		formObject.setData(mFormModel.getValue(key));
		return formObject;
		}

	/**
	 * Adds the form object immediately and sets its initial content.
	 * @param objectDescriptor is key \t type \t constraint
	 * @return constructed and added form object
	 */
	public AbstractFormObject addFormObject(String objectDescriptor) {
		int index1 = objectDescriptor.indexOf('\t');
		if (index1 != -1) {
			int index2 = objectDescriptor.indexOf('\t', index1+1);
			if (index2 != -1) {
				String key = objectDescriptor.substring(0, index1);
				String type = objectDescriptor.substring(index1+1, index2);
				AbstractFormObject formObject = addFormObject(key, type);
				addObject(formObject, objectDescriptor.substring(index2+1));
				formObject.setEditable(mEditable);
				formObject.addFormObjectListener(this);
				formObject.setData(mFormModel.getValue(key));
				return formObject;
				}
			}
		return null;
		}

	public int getFormObjectCount() {
		return mComponentList.size();
		}

	public AbstractFormObject getFormObject(int no) {
		return mComponentList.get(no);
		}

	public void removeFormObject(int no) {
		remove(mComponentList.get(no).getComponent());
		mComponentList.remove(no);
		}

	public String getFormObjectTitle(int no) {
		return mComponentList.get(no).getTitle();
		}

	public AbstractFormObject getFormObjectAt(int x, int y) {
		for (AbstractFormObject fo:mComponentList)
			if (fo.getComponent().contains(x, y))
				return fo;
		return null;
		}

	public String getFormLayoutDescriptor() {
		LayoutManager layout = (LayoutManager)getLayout();

		if (layout instanceof TableLayout) {
			StringBuffer desc = new StringBuffer("TableLayout");
			desc.append(","+((TableLayout)layout).getNumColumn());
			for (int i=0; i<((TableLayout)layout).getNumColumn(); i++)
				desc.append(","+((TableLayout)layout).getColumn(i));
			desc.append(","+((TableLayout)layout).getNumRow());
			for (int i=0; i<((TableLayout)layout).getNumRow(); i++)
				desc.append(","+((TableLayout)layout).getRow(i));

			return desc.toString();
			}

		return null;
		}

	public String getFormObjectDescriptor(int no) {
		AbstractFormObject formObject = mComponentList.get(no);
		return formObject.getKey()+"\t"
			 + formObject.getType()+"\t"
			 + ((TableLayout)getLayout()).getConstraints(formObject.getComponent()).toString();
		}

	public TableLayoutConstraints getFormObjectConstraints(int no) {
		AbstractFormObject formObject = mComponentList.get(no);
		return ((TableLayout)getLayout()).getConstraints(formObject.getComponent());
		}

	public void setFormObjectTitle(String title, int no) {
		mComponentList.get(no).setTitle(title);
		}

	public void setFormLayoutDescriptor(String layoutDesc) {
		double[][] desc = parseLayout(layoutDesc);
		if (desc == null)
			createDefaultLayout();
		else
			setFormLayout(desc);
		}

	public void setFormLayout(double[][] layoutDesc) {
		removeAll();
		mComponentList.clear();

		setLayout(new TableLayout(layoutDesc));
		mLayoutDesc = layoutDesc;
		}

	public double[][] getFormLayout() {
		return mLayoutDesc;
		}

	public void dataChanged(AbstractFormObject fo) {
		if (!mBlockEvents && !mFormModel.setValue(fo.getKey(), fo.getData())) {
			// if we cannot modify the model after a user change, we need to revert the form object's value
			mBlockEvents = true;
			fo.setData(mFormModel.getValue(fo.getKey()));
			mBlockEvents = false;
			}
		}

	public void update() {
		for (AbstractFormObject formObject:mComponentList)
			formObject.setData(mFormModel.getValue(formObject.getKey()));
		}

	public AbstractFormObject getFormObjectOnFocus() {
		for (AbstractFormObject formObject:mComponentList)
			if (formObject.getComponent().hasFocus())
				return formObject;

		return null;
		}

	public int print(Graphics g, PageFormat f, int pageIndex) {
		if (pageIndex != 0)
			return NO_SUCH_PAGE;

		Rectangle2D.Float bounds = new Rectangle2D.Float((float)f.getImageableX(),
														 (float)f.getImageableY(),
														 (float)f.getImageableWidth(),
														 (float)f.getImageableHeight());
		print((Graphics2D)g, bounds, 1.0f, mFormModel);

		return PAGE_EXISTS;
		}

	public int print(Graphics2D g2D, Rectangle2D.Float bounds, float scale, FormModel formModel) {
		// calculate TableLayout coordinates
		float[][] coord = new float[2][];
		for (int i=0; i<2; i++) {
			int columnsAssigned = 0;
			double remainingSize = (i == 0) ? bounds.width : bounds.height;
			double[] size = new double[mLayoutDesc[i].length];
			for (int j=0; j<mLayoutDesc[i].length; j++) {
				if (mLayoutDesc[i][j] >= 1.0 || mLayoutDesc[i][j] == 0.0) {
					size[j] = scale * mLayoutDesc[i][j];
					remainingSize -= size[j];
					columnsAssigned++;
					}
				}

			if (remainingSize > 0.0) {
				double relativeSize = remainingSize;
				for (int j=0; j<mLayoutDesc[i].length; j++) {
					if (mLayoutDesc[i][j] > 0.0 && mLayoutDesc[i][j] < 1.0) {
						size[j] = mLayoutDesc[i][j] * relativeSize;
						remainingSize -= size[j];
						columnsAssigned++;
						}
					}
				}

			if (remainingSize > 0.0) {
				remainingSize /= mLayoutDesc[i].length - columnsAssigned;
				for (int j=0; j<mLayoutDesc[i].length; j++)
					if (mLayoutDesc[i][j] == TableLayoutConstants.FILL)
						size[j] = remainingSize;
				}
			
			coord[i] = new float[mLayoutDesc[i].length+1];
			coord[i][0] = (i == 0) ? bounds.x : bounds.y;
			for (int j=0; j<mLayoutDesc[i].length; j++)
				coord[i][j+1] = coord[i][j] + (float)size[j];
			}

		for (int i=0; i<mComponentList.size(); i++) {
			TableLayoutConstraints constraints = getFormObjectConstraints(i);
			float x1 = coord[0][constraints.col1];
			float x2 = coord[0][constraints.col2+1];
			float y1 = coord[1][constraints.row1];
			float y2 = coord[1][constraints.row2+1];
			Rectangle2D.Float objectRect = new Rectangle2D.Float(x1, y1, x2-x1, y2-y1);
			AbstractFormObject formObject = mComponentList.get(i);
			formObject.print(g2D, objectRect, scale, formModel.getValue(formObject.getKey()));
			}

		return PAGE_EXISTS;
		}

	public void createDefaultLayout() {
		removeAll();

		int overallHeight = 0;
		for (int i=0; i<mComponentList.size(); i++)
			overallHeight += mComponentList.get(i).getRelativeHeight();

		String[] position = new String[mComponentList.size()];
		int x = 0;
		int y = 0;
		int layoutHeight = 0;
		for (int i=0; i<mComponentList.size(); i++) {
			int layoutX = x*2+1;	// considers border and spacing
			int layoutY = y*2+1;
			int height = mComponentList.get(i).getRelativeHeight();
			if (height == 1)
				position[i] = ""+layoutX+", "+layoutY;
			else
				position[i] = ""+layoutX+", "+layoutY+", "+layoutX+", "+(layoutY+2*height-2);
			y += height;
			if (2*y >= overallHeight) {
				layoutHeight = y;
				x++;
				y = 0;
				}
			}

		double[] yLayout = new double[layoutHeight*2+1];
		for (int i=0; i<layoutHeight; i++) {
			yLayout[i*2] = 8;
			yLayout[i*2+1] = TableLayout.FILL;
			}
		yLayout[layoutHeight*2] = 8;

		double[][] layoutDesc = { {8, TableLayout.FILL, 8, TableLayout.FILL, 8}, yLayout };
		setLayout(new TableLayout(layoutDesc));
		mLayoutDesc = layoutDesc;

		for (int i=0; i<mComponentList.size(); i++) {
			AbstractFormObject formObject = mComponentList.get(i);
			addObject(formObject, position[i]);
			formObject.setData(mFormModel.getValue(formObject.getKey()));
			}
		}

	private double[][] parseLayout(String desc) {
		if (desc.startsWith("TableLayout")) {
			StringTokenizer st = new StringTokenizer(desc, ",");
			st.nextToken();
			int width = new Integer(st.nextToken()).intValue();
			double[] horizontal = new double[width];
			for (int i=0; i<width; i++)
				horizontal[i] = new Double(st.nextToken()).doubleValue();
			int height = new Integer(st.nextToken()).intValue();
			double[] vertical = new double[height];
			for (int i=0; i<height; i++)
				vertical[i] = new Double(st.nextToken()).doubleValue();
			double[][] size = { horizontal, vertical };
			return size;
			}

		return null;
		}

	private void addObject(AbstractFormObject formObject, String constraint) {
		String title = mFormModel.getTitle(formObject.getKey());
		formObject.setTitle(title);   // actually creates border
		add(formObject.getComponent(), constraint);
		}
	}
