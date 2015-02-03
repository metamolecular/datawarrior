package com.actelion.research.gui.dock;

import java.awt.*;

import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;

public class ShadowBorder extends AbstractBorder {
    private static final long serialVersionUID = 0x20070807;

    private static final Insets INSETS = new Insets(1, 1, 3, 3);
    private Insets mInsets;

    /**
     * Creates a border with a grey 1 pixel line on left and top
     * and a 3 pixel wide shadow on right and bottom.
     * Insets are 1,1,3,3, i.e. as large as needed for the drawn border.
     */
    public ShadowBorder() {
    	mInsets = INSETS;
    	}

    /**
     * Creates a border with a grey 1 pixel line on left and top
     * and a 3 pixel wide shadow on right and bottom.
     * Insets can be specified to be larger than the drawn border,
     * which allows to adjust the spacing outside the drawn border.
     */
    public ShadowBorder(int top, int left, int bottom, int right) {
    	mInsets = new Insets(top, left, bottom, right);
    	}

    public Insets getBorderInsets(Component c) {
        return mInsets;
        }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Color shadow = UIManager.getColor("controlShadow");
        if (shadow == null)
            shadow = Color.GRAY;

        Color lightShadow = new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), 170);
        Color lighterShadow = new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), 70);
        g.translate(x, y);

        int t = mInsets.top;
        int l = mInsets.left;
        h -= mInsets.bottom;
        w -= mInsets.right;
        int b = h;
        int r = w;
        h -= mInsets.top;
        w -= mInsets.left;

        g.setColor(shadow);
        g.fillRect(l-1, t-1, w+1, 1);
        g.fillRect(l-1, t-1, 1, h+1);
        g.fillRect(r, t, 1, h+1);
        g.fillRect(l, b, w+1, 1);
        // Shadow line 1
        g.setColor(lightShadow);
        g.fillRect(r, t-1, 1, 1);
        g.fillRect(l-1, b, 1, 1);
        g.fillRect(r+1, t, 1, h+1);
        g.fillRect(l, b+1, w+1, 1);
        // Shadow line2
        g.setColor(lighterShadow);
        g.fillRect(r+1, t-1, 1, 1);
        g.fillRect(l-1, b+1, 1, 1);
        g.fillRect(r+1, b+1, 1, 1);
        g.fillRect(r+2, t, 1, h+2);
        g.fillRect(l, b+2, w+2, 1);
        g.translate(-x, -y);
        }
    }
