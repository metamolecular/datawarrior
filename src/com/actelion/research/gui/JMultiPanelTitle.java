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

package com.actelion.research.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JMultiPanelTitle extends JComponent implements MouseListener,MouseMotionListener {
    private static final long serialVersionUID = 0x20100813;

    public static final int HEIGHT = 10;
	public static final int TEXT_HEIGHT = 9;

	/*	don't use Metal L&F anymore
	private static final Color cThumbColor          = new Color(153, 153, 204);
	private static final Color cThumbShadowColor    = new Color(102, 102, 153);
	private static final Color cThumbHighlightColor = new Color(204, 204, 255);
	private static final Color cTextColor			= new Color( 70,  71, 110);
	*/

	private MultiPanelDragListener	mDragListener;
	private String					mTitle;
	private boolean					mDragEnabled;

	public JMultiPanelTitle(MultiPanelDragListener parent, String title) {
		mDragListener = parent;
		mTitle = title;
		mDragEnabled = true;
		addMouseListener(this);
		addMouseMotionListener(this);
		}

	public void setDragEnabled(boolean b) {
		mDragEnabled = b;
		}

	public void paintComponent(Graphics g) {
		Dimension size = getSize();

        Graphics2D g2 = (Graphics2D) g;
        Paint storedPaint = g2.getPaint();

        g2.setPaint(HeaderPaintHelper.getHeaderPaint(true, size.height));
        g2.fillRect(0, 0, size.width, size.height);

        g2.setPaint(storedPaint);

		g.setColor(Color.BLACK);
		g.setFont(new Font("Helvetica", Font.PLAIN, TEXT_HEIGHT));
		int stringWidth = (int)g.getFontMetrics().getStringBounds(mTitle, g).getWidth();
		g.drawString(mTitle, (size.width-stringWidth)/2, HEIGHT-2);

		/*	don't use Metal L&F anymore
		g.setColor(cThumbHighlightColor);
		g.drawLine(0, 0, size.width-1, 0);
		g.drawLine(0, 0, 0, size.height-1);

		g.setColor(cThumbShadowColor);
		g.drawLine(size.width-1, 1, size.width-1, size.height-1);
		g.drawLine(1, size.height-1, size.width-1, size.height-1);

		g.setColor(cThumbColor);
		g.fillRect(1, 1, size.width-2, size.height-2);

		g.setColor(cTextColor);
		g.setFont(new Font("Helvetica", Font.PLAIN, TEXT_HEIGHT));
		int stringWidth = (int)g.getFontMetrics().getStringBounds(mTitle, g).getWidth();
		g.drawString(mTitle, (size.width-stringWidth)/2, HEIGHT-2);

		drawPattern(g, size.width, stringWidth);
		*/
		}

	/*	don't use Metal L&F anymore
	private void drawPattern(Graphics g, int totalWidth, int stringWidth) {
		int cycles = (0xFFFE & ((totalWidth - stringWidth - 16) / 4)) - 1;

		g.setColor(cThumbHighlightColor);
		drawDotSet(g, 3, 2, cycles);
		drawDotSet(g, totalWidth-2*cycles-3, 2, cycles);

		g.setColor(cThumbShadowColor);
		drawDotSet(g, 4, 3, cycles);
		drawDotSet(g, totalWidth-2*cycles-2, 3, cycles);
		}

	private void drawDotSet(Graphics g, int x, int y, int cycles) {
		for (int i=0; i<cycles; i++)
			for (int j=0; j<3; j++)
				if (((i + j) & 1) == 0)
					g.drawLine(x+i*2, y+j*2, x+i*2, y+j*2);
		}
	*/

	public void mouseClicked(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (mDragEnabled)
			mDragListener.dragStarted(e.getY(), this);
		}

	public void mouseReleased(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {
		if (mDragEnabled)
			setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
		}

	public void mouseExited(MouseEvent e) {
		setCursor(Cursor.getDefaultCursor());
		}

	public void mouseDragged(MouseEvent e) {
		if (mDragEnabled)
			mDragListener.dragContinued(e.getY());
		}

	public void mouseMoved(MouseEvent e) {}
	}
