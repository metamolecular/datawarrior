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
import java.awt.font.*;
import java.awt.geom.Rectangle2D;

import javax.swing.border.AbstractBorder;

import com.actelion.research.util.Platform;

public class FormObjectBorder extends AbstractBorder {
    private static final long serialVersionUID = 0x20090731;

    protected static final Font FONT = new Font("Helvetica", Font.PLAIN, 10);

    private static final Color TITLE_BACKGROUND = new Color(224, 224, 255);
    private static final Color BORDER_COLOR = new Color(128, 128, 255);
    private static final Color EDIT_TITLE_BACKGROUND = new Color(240, 240, 160);
    private static final Color EDIT_BORDER_COLOR = new Color(208, 192, 96);
    private static final int BORDER = 1;
    private static final int MIN_CONTENT_HEIGHT = 12;
    private String mTitle;
    private FontMetrics mMetrics;
    private boolean mIsEditMode;

    public FormObjectBorder(String title) {
        mTitle = title;
    	}

    public Insets getBorderInsets(Component c) {
        if (c.getGraphics() == null)
            return new Insets(0, 0, 0, 0);

        Dimension size = c.getSize();
        Insets insets = null;
        if (mTitle == null) {
            if (size.height >= MIN_CONTENT_HEIGHT + 2*BORDER)
                insets = new Insets(BORDER, BORDER, BORDER, BORDER);
            else
                insets = new Insets(0, 0, 0, 0);
        	}
        else {
            if (mMetrics == null)
                mMetrics = c.getGraphics().getFontMetrics(FONT);

            int titleWidth = (int)mMetrics.getStringBounds(" "+mTitle+": ", c.getGraphics()).getWidth();
            
            if (size.height >= MIN_CONTENT_HEIGHT + 2*BORDER + mMetrics.getHeight())
                insets = new Insets(BORDER+mMetrics.getHeight(), BORDER, BORDER, BORDER);
            else if (size.height >= MIN_CONTENT_HEIGHT + 2 * BORDER)
                insets = new Insets(BORDER, Math.min(size.width/2, BORDER+titleWidth), BORDER, BORDER);
            else
                insets = new Insets(0, Math.min(size.width/2, titleWidth), 0, 0);
        	}

        return insets;
    	}

    public Insets getBorderInsets(Component c, Insets insets) {
        return getBorderInsets(c);
    	}

    public String getTitle() {
    	return mTitle;
    	}
    
    public void setTitle(String title) {
    	mTitle = title;
    	}

    public void setEditMode(boolean b) {
        mIsEditMode = b;
        }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    	// switch off antialiasing and sub-pixel accuracy for the border, which some implementation may have used
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        Insets insets = getBorderInsets(c);
        if (mTitle != null) {
            String title = " "+mTitle+": ";
	        if (insets.top > BORDER) {	// title on top
	            g.setColor(mIsEditMode ? EDIT_TITLE_BACKGROUND : TITLE_BACKGROUND);
	            g.fillRect(x+BORDER, y+BORDER, width-2*BORDER, insets.top-BORDER);
	            g.setColor(Color.BLACK);
	            g.setFont(FONT);
	            g.drawString(title, x, y+BORDER+mMetrics.getAscent());
	        	}
	        else {	// title on left side
	            g.setColor(mIsEditMode ? EDIT_TITLE_BACKGROUND : TITLE_BACKGROUND);
	            g.fillRect(x+insets.top, y+insets.top, insets.left-insets.top, height-2*insets.top);
	            g.setColor(Color.BLACK);
	            g.setFont(FONT);
	            int titleWidth = (int)mMetrics.getStringBounds(title, g).getWidth();
	            g.drawString(title, x+insets.left-titleWidth, y+insets.top+mMetrics.getAscent());
	        	}
        	}
        if (insets.top != 0) {
            g.setColor(mIsEditMode ? EDIT_BORDER_COLOR : BORDER_COLOR);
            g.drawRect(x, y, width-1, height-1);
        	}
    	}

    public void printBorder(Graphics2D g2D, Rectangle2D.Float rect, float scale) {
    	g2D.setColor(BORDER_COLOR);
        float lineWidth = scale;
        g2D.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        shrink(rect, lineWidth/2);
        if (Platform.isMacintosh()) {
	        rect.x -= 0.5;
	        rect.y -= 0.5;
        	}
        g2D.draw(rect);
        if (Platform.isMacintosh()) {
	        rect.x += 0.5;
	        rect.y += 0.5;
        	}
        shrink(rect, lineWidth/2);

        g2D.setFont(new Font("Helvetica", Font.PLAIN, (int)(scale*9+0.5)));
        FontMetrics metrics = g2D.getFontMetrics();
 		GlyphVector title = g2D.getFont().createGlyphVector(g2D.getFontRenderContext(), " "+mTitle+": ");
        Rectangle2D.Float titleRect = (Rectangle2D.Float)rect.clone();
    	if (rect.height >= 2*metrics.getHeight()) {
    	    titleRect.height = metrics.getHeight();
    	    rect.y += metrics.getHeight();
    	    rect.height -= metrics.getHeight();
    	    g2D.setColor(TITLE_BACKGROUND);
    	    g2D.fill(titleRect);
    	    g2D.setColor(Color.black);
    		}
    	else {
            Rectangle2D titleBounds = title.getLogicalBounds();
    	    titleRect.width = Math.min(titleRect.width/2f, (float)titleBounds.getWidth());
    	    rect.x += titleRect.width;
    	    rect.width -= titleRect.width;
    	    g2D.setColor(TITLE_BACKGROUND);
    	    g2D.fill(titleRect);
    	    g2D.setColor(Color.black);
    		}
    	Shape clip = g2D.getClip();
    	g2D.setClip(titleRect);
    	g2D.drawGlyphVector(title, (float)titleRect.x, (float)(titleRect.y+metrics.getAscent()));
    	g2D.setClip(clip);
    	}

	private void shrink(Rectangle2D.Float r, float d) {
		r.x += d;
		r.y += d;
		r.width -= 2*d;
		r.height -= 2*d;
		}
	}
