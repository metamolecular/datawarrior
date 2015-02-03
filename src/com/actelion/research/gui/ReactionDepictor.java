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
 * @author Christian Rufener
 */

package com.actelion.research.gui;

import com.actelion.research.chem.*;
import com.actelion.research.chem.reaction.Reaction;

import java.awt.*;

public class ReactionDepictor
{
    private Reaction rxn_ = null;
    public static final int ARROWWIDTH = 40;
    public static final int ARROWHEIGHT = 20;
    public static final int PLUSSIZE = 20;
    private static final String PLUSSTRING = "+";
    private static final Font PLUSFONT = new Font("Helvetica", Font.BOLD, 12);
    private static Rectangle dim_ = new Rectangle(0,0,600,400);
    private DrawingObjectList drwobj_ = null;
    private int displaymode = 0;
    private Color _color = null;
    
    public ReactionDepictor()
    {
    }


    public ReactionDepictor(Reaction r, DrawingObjectList drwobj)
    {
        rxn_ = r;
        drwobj_ = drwobj;
    }

    public ReactionDepictor(Reaction r)
    {
        this(r,null);
    }

    public void setReaction(Reaction r)
    {
        rxn_ = r;
    }

    public void setDisplayMode(int mode)
    {
        displaymode = mode;
    }
    
    public void setDrawingObjects(DrawingObjectList drwobj)
    {
        drwobj_ = drwobj;
    }

//    private boolean useExtendedDepictor = true;

    public DepictorTransformation paint(Graphics g)
    {
        if (rxn_ != null)
            drawReaction(g,rxn_,drwobj_);
        return null;
    }
/*
    private ReactionArrow getReactionArrow()
    {
        java.awt.geom.Rectangle2D ar = ChemistryHelper.getArrowBoundingRect(rxn_);
        if (ar != null) {
            ReactionArrow arrow = new ReactionArrow();
            arrow.setCoordinates(ar.getX(),ar.getY(),ar.getX()+ar.getWidth(),ar.getY()+ ar.getHeight());
            return arrow;
        }
        return null;
    }


    private Dimension getSize()
    {
        return new Dimension(dim_.width,dim_.height);
    }
*/

    private void drawReaction(Graphics g, Reaction r,DrawingObjectList dobs)
    {

        java.awt.geom.Rectangle2D reactionBoundingRect = ChemistryHelper.getBoundingRect(r,true);
        int numProd = r.getProducts();
        int numReact = r.getReactants();
        int mols = r.getMolecules();
        if (reactionBoundingRect != null) {

            double offsetx = -reactionBoundingRect.getX();
            double offsety = -reactionBoundingRect.getY();
            double scaler = reactionBoundingRect.getHeight() / reactionBoundingRect.getWidth();
            Rectangle d = dim_;
            double insetx = dim_.x + 2;
            double insety = dim_.y + 2;
            double insetwidth = dim_.width - 4;
            double insetheight = dim_.height - 4;

            double scalec = (double)d.height / (double)d.width ;
            double scale = 1;

            if(scalec > scaler) {                   // scale on x-dimension
                scale = insetwidth / (float)reactionBoundingRect.getWidth();
                double t = (insetheight/scale - (float)reactionBoundingRect.getHeight() ) / 2;
                offsety += t;
            } else {                                // scale on y-dimension
                scale = insetheight / (float)reactionBoundingRect.getHeight();
                double t = (insetwidth/scale - (float)reactionBoundingRect.getWidth()) / 2;
                offsetx += t;
            }

            System.out.println("Transforming reaction " + reactionBoundingRect + " " + offsety + " " + scale);
            ChemistryHelper.transformReaction(r,offsetx,offsety,scale);
            java.awt.geom.Rectangle2D rn = ChemistryHelper.getBoundingRect(r,false);
            System.out.println("Transformed reaction " + rn);
//            r.setReactionLayoutRequired(true);
//            ExtendedDepictor dep = new ExtendedDepictor(r,null,true);
//            dep.validateView(g,new Rectangle2D.Float((float)dim_.x,(float)dim_.y,(float)dim_.width,(float)dim_.height),AbstractDepictor.cModeInflateToMaxAVBL);
//            dep.paint(g);

            for (int i = 0; i< mols; i++) {
                Depictor2D depict = new Depictor2D(r.getMolecule(i), displaymode);
                if (_color != null)
                    depict.setOverruleColor(_color, null);
                java.awt.geom.Rectangle2D o = ChemistryHelper.getBoundingRect(r.getMolecule(i));
                if (o != null) {
                    DepictorTransformation tm =  depict.validateView(g,
                            new java.awt.geom.Rectangle2D.Float((float)(o.getX()+insetx), (float)(o.getY() + insety),
                            		(float)(o.getWidth()),(float)o.getHeight()), AbstractDepictor.cModeInflateToMaxAVBL);
                    depict.paint(g);
                }
            }
            java.awt.geom.Rectangle2D ar = ChemistryHelper.getArrowBoundingRect(r);
            if (ar != null) {
                Arrow a = null;
                if (ar.getWidth() == 0) {
                    a = new Arrow((int)insetx + (int)ar.getX()-ARROWWIDTH, (int)insety + (int)ar.getY(), ARROWWIDTH, ARROWHEIGHT);
                } else if (ar.getWidth() < ARROWWIDTH) {
                    a = new Arrow((int)insetx + (int)ar.getX(), (int)insety + (int)ar.getY(), (int)ar.getWidth(), ARROWHEIGHT);
                } else {
                    a = new Arrow((int)insetx + (int)ar.getX()+ (int)(ar.getWidth()-ARROWWIDTH)/2, (int)insety + (int)ar.getY(), ARROWWIDTH, ARROWHEIGHT);
                }
                a.paint(g);
            }
            FontMetrics fm = g.getFontMetrics(PLUSFONT);
            java.awt.font.LineMetrics lm = fm.getLineMetrics(PLUSSTRING, g);

            for (int i = 1; i< numReact; i++) {
                java.awt.geom.Rectangle2D r1 = ChemistryHelper.getBoundingRect(r.getMolecule(i-1));
                java.awt.geom.Rectangle2D r2 = ChemistryHelper.getBoundingRect(r.getMolecule(i));
                java.awt.geom.Rectangle2D rp = null;
                if (r1.intersects(r2.getX(),r2.getY(),r2.getWidth(),r2.getHeight())) {
                    rp = r1.createIntersection(r2);
                } else
                    rp = ChemistryHelper.getDiffRect(r1,r2);
                int x = (int)rp.getCenterX();
                int y = (int)rp.getCenterY();
                g.setFont(PLUSFONT);
                g.drawString(PLUSSTRING, (int)insetx + x ,(int)insety + y + fm.getHeight()/2);
            }

            for (int i = 1; i< numProd; i++) {
                java.awt.geom.Rectangle2D r1 = ChemistryHelper.getBoundingRect(r.getMolecule(numReact+i-1));
                java.awt.geom.Rectangle2D r2 = ChemistryHelper.getBoundingRect(r.getMolecule(numReact+i));
                java.awt.geom.Rectangle2D rp = null;
                if (r1.intersects(r2.getX(),r2.getY(),r2.getWidth(),r2.getHeight())) {
                    rp = r1.createIntersection(r2);
                } else
                    rp = ChemistryHelper.getDiffRect(r1,r2);
                int x = (int)rp.getCenterX();
                int y = (int)rp.getCenterY();
                g.setFont(PLUSFONT);
                g.drawString(PLUSSTRING, (int)insetx + x ,(int)(insety + y + fm.getHeight()/2));
            }

    /*
            if (dobs != null) {
                int size = dobs.size();
                for (int i = 0; i < size; i ++) {
                    AbstractDrawingObject o = (AbstractDrawingObject)dobs.get(i);
                    java.awt.geom.Rectangle2D r1 = o.getBoundingRect();
                    System.out.println("Drawing Object " + o + "\nBounds: " + r1 + "\n Offsets (" + offsetx +","+offsety + ")");
                    DepictorTransformation tm = new DepictorTransformation();
         //           tm.move(offsetx*scale,offsety*scale);
          //          tm.setScaling(scale);
                    o.draw(g,tm);
                }
            }
     */
        }
    }
    
    public boolean updateCoords(Graphics g,double x1,double y1,double width,double height, int mode)
    {
//        ExtendedDepictor dep = new ExtendedDepictor(rxn_,null,false);
//        dep.updateCoords(g,new java.awt.geom.Rectangle2D.Float((float)x1, (float)y1, (float)width, (float)height), AbstractDepictor.cModeInflateToMaxAVBL);

        dim_ = new Rectangle((int)x1,(int)y1,(int)width,(int)height);
        return true;
    }

//    private DrawingObjectList getPlusOjects(Reaction rxn)
//    {
//        DrawingObjectList res = new DrawingObjectList();
//        if (rxn != null) {
//            int mols = rxn.getMolecules();
//            int rn = rxn.getReactants();
//            int pn = rxn.getProducts();
//
//            for (int i = 1; i < mols; i++) {
//                if (i != rn) {
//                    ExtendedMolecule m = rxn.getMolecule(i-1);
//                    java.awt.geom.Rectangle2D rc1 = ChemistryHelper.getBoundingRect(m);
//                    m = rxn.getMolecule(i);
//                    java.awt.geom.Rectangle2D rc2 = ChemistryHelper.getBoundingRect(m);
//                    java.awt.geom.Rectangle2D.Double d = ChemistryHelper.getDiffRect(rc1,rc2);
//                    if (d != null) {
//                        TextDrawingObject o = new TextDrawingObject();
//                        o.setCoordinates((float)d.getCenterX(),(float)d.getCenterY());
//                        o.setValues("+",10, TextDrawingObject.DEFAULT_STYLE);
//                        res.add(o);
//                    }
//                }
//            }
//        }
//        return res;
//    }
//
    public void setColor(Color c)
    {
        _color = c	;
    }
}
