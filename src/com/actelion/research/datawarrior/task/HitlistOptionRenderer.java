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

import java.awt.*;
import java.io.*;
import javax.swing.*;

public class HitlistOptionRenderer extends JPanel implements ListCellRenderer {
	private static final long serialVersionUID = 0x20130227;

	public static final String[] OPERATION_TEXT = { "logical AND",
													"logical OR",
													"logical XOR",
													"logical NOT"};
	public static final String[] OPERATION_CODE = { "and", "or", "xor", "not" };

	private static final Color cSelectionColor = UIManager.getColor("TextArea.selectionBackground");
	private static final int cItemWidth = 120;
	private static final int cItemHeight = 19;
	private static final int cImageWidth = 28;
	private static final int cImageHeight = 17;
	private static final String IMAGE_PATH = "/images/booleanOperations.gif";

	private static Image	sImage;

	private Object			mParameterValue;
	private int				mParameterIndex;
	private boolean			mIsSelected,mIsActiveItem;

	public HitlistOptionRenderer() {
		if (sImage == null) {
			Toolkit tk = Toolkit.getDefaultToolkit();
			try {
				BufferedInputStream in=new BufferedInputStream(getClass().getResourceAsStream(IMAGE_PATH));
				byte[] imageData= new byte[2000]; // make it as big as necessary
				in.read(imageData);
				sImage = tk.createImage(imageData);
				}
			catch (Exception e) {
				System.out.println("Error loading image: "+e);
				}
			}

		setPreferredSize(new Dimension(cItemWidth, cItemHeight));
		}

	public void paintComponent(Graphics g) {
		if (mParameterIndex != -1) {
			Dimension theSize = getSize();
			if (mIsSelected) {
				g.setColor(cSelectionColor);
				g.fillRect(0, 0, theSize.width, theSize.height);
				}
			g.setColor(Color.black);
			g.drawString((String)mParameterValue, cImageWidth+8, cItemHeight-4);

			int verticalBorder = (cItemHeight - cImageHeight) / 2;
			g.setClip(4, verticalBorder, cImageWidth, cImageHeight);
			g.drawImage(sImage, 4 - mParameterIndex * cImageWidth, verticalBorder, null);
			}
		}

	public Component getListCellRendererComponent(JList list,
												  Object value,
												  int index,
												  boolean isSelected,
												  boolean cellHasFocus) {
		mIsActiveItem = false;
		if (index == -1) {
			index = list.getSelectedIndex();
			mIsActiveItem = (index != -1);
			}

		mParameterValue = value;
		mParameterIndex = index;
		mIsSelected = isSelected;
		return this;
		}
	}