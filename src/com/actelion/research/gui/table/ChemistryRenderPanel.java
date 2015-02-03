package com.actelion.research.gui.table;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.actelion.research.chem.*;
import com.actelion.research.chem.reaction.Reaction;

public class ChemistryRenderPanel extends JPanel {
    static final long serialVersionUID = 0x20070312;

    private Object  mChemistry;
    private Color   mAlternatingBackgroundColor,mForeGround;

    public void setChemistry(Object chemistry) {
        mChemistry = chemistry;
        repaint();
        }

    public void update(Graphics g) {
        paint(g);
        }

    public void setSelected(boolean isSelected) {
        if (isSelected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
            }
        else {
            setForeground(UIManager.getColor("Table.foreground"));
            if (mAlternatingBackgroundColor != null)
                setBackground(mAlternatingBackgroundColor);
            else
                setBackground(UIManager.getColor("Table.background"));
            }
        }

    public void setFocus(boolean hasFocus) {
        if (hasFocus)
            setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        else
            setBorder(new EmptyBorder(1, 1, 1, 1));
        }

    public void setForeground(Color fg) {
		mForeGround = fg;
    	if (Color.black.equals(mForeGround))
    		mForeGround = null;
    	}

    public void setAlternatingBackground(Color bg) {
        mAlternatingBackgroundColor = bg;
        }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Rectangle r = new Rectangle(new java.awt.Point(0,0), getSize());
        r.grow(-2, -2);

        Insets insets = getInsets();
        r.x += insets.left;
        r.y += insets.top;
        r.width -= insets.left + insets.right;
        r.height -= insets.top + insets.bottom;

        if (mChemistry != null && r.width > 0 && r.height > 0) {
            if (mChemistry instanceof ExtendedMolecule) {
                Depictor2D d = new Depictor2D((StereoMolecule)mChemistry);
                if (mForeGround != null)
                	d.setOverruleColor(mForeGround, getBackground());
                d.validateView(g, new Rectangle2D.Float(r.x, r.y, r.width, r.height), AbstractDepictor.cModeInflateToMaxAVBL);
                d.paint(g);
                }
            if (mChemistry instanceof Reaction) {
            	Reaction rxn = (Reaction)mChemistry;
                ExtendedDepictor d = new ExtendedDepictor(rxn, rxn.getDrawingObjects(), !rxn.hasAbsoluteCoordinates(), true);
                if (mForeGround != null)
                	d.setOverruleColor(mForeGround, getBackground());
                d.validateView(g, new Rectangle2D.Float(r.x, r.y, r.width, r.height), AbstractDepictor.cModeInflateToMaxAVBL);
                d.paint(g);
                }
            }
        }
    }
