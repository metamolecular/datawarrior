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
 * @author Joel Freyss
 */
package com.actelion.research.gui.jmol;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.JPanel;

import org.jmol.viewer.Viewer;

/**
 * Adapted from the Jmol code to run the Jmol viewer into Actelion editor
 *
 */
public class DisplayPanel extends JPanel implements ComponentListener, Printable {

	Viewer viewer;

	private Dimension startupDimension;
	private boolean haveDisplay;

	public DisplayPanel(boolean haveDisplay, int startupWidth, int startupHeight) {
		startupDimension = new Dimension(startupWidth, startupHeight);
		this.haveDisplay = haveDisplay;
		setFocusable(true);
		setDoubleBuffered(false);
		addComponentListener(this);
		setBackground(Color.RED);
	}

	public void setViewer(Viewer viewer) {
		this.viewer = viewer;
		viewer.setScreenDimension(haveDisplay ? getSize(dimSize) : startupDimension);
	}

	// current dimensions of the display screen
	final Dimension dimSize = new Dimension();
	private final Rectangle rectClip = new Rectangle();

	void setRotateMode() {
		viewer.setSelectionHalos(false);
	}

	@Override
	public void componentHidden(java.awt.event.ComponentEvent e) {
	}
	@Override
	public void componentMoved(java.awt.event.ComponentEvent e) {
	}
	@Override
	public void componentResized(java.awt.event.ComponentEvent e) {
		updateSize();
	}
	@Override
	public void componentShown(java.awt.event.ComponentEvent e) {
		updateSize();
	}

	private void updateSize() {
		viewer.setScreenDimension(haveDisplay ? getSize(dimSize) : startupDimension);
		setRotateMode();
		viewer.refresh(3, "updateSize");
		repaint();
	}
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		// if (showPaintTime) startPaintClock();
		g.getClipBounds(rectClip);
		if (dimSize.width == 0)
			return;
		try {
			
			viewer.renderScreenImage(g, dimSize, rectClip);
		} catch (Exception e) {
			// Ignore
			System.err.println("DisplayPanel.paint() error: "+e);
		}
	}
	@Override
	public int print(Graphics g, PageFormat pf, int pageIndex) {
		Graphics2D g2 = (Graphics2D) g;
		if (pageIndex > 0)
			return Printable.NO_SUCH_PAGE;
		rectClip.x = rectClip.y = 0;
		int screenWidth = rectClip.width = viewer.getScreenWidth();
		int screenHeight = rectClip.height = viewer.getScreenHeight();
		Image image = viewer.getScreenImage();
		int pageX = (int) pf.getImageableX();
		int pageY = (int) pf.getImageableY();
		int pageWidth = (int) pf.getImageableWidth();
		int pageHeight = (int) pf.getImageableHeight();
		float scaleWidth = pageWidth / (float) screenWidth;
		float scaleHeight = pageHeight / (float) screenHeight;
		float scale = (scaleWidth < scaleHeight ? scaleWidth : scaleHeight);
		if (scale < 1) {
			int width = (int) (screenWidth * scale);
			int height = (int) (screenHeight * scale);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2.drawImage(image, pageX, pageY, width, height, null);
		} else {
			g2.drawImage(image, pageX, pageY, null);
		}
		viewer.releaseScreenImage();
		return Printable.PAGE_EXISTS;
	}

}
