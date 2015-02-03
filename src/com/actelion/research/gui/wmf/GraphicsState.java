/*
 * @(#)GraphicsState.java	1.00 10/10/2003
 *
 * Copyright 2000-2003 Actelion Ltd. All Rights Reserved.
 * 
 * This software is the proprietary information of Actelion Ltd.  
 * Use is subject to license terms.
 * 
 */

package com.actelion.research.gui.wmf;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * 
 * <p>Title: Actelion Library</p>
 * <p>Description: Actelion Java Library </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: Actelion Ltd</p>
 * @author Christian Rufener
 * @version 1.0
 */
class GraphicsState
{

    GraphicsState()
    {
        origin = new Point(0, 0);
    }

    void decreaseCount()
    {
        count--;
    }

    int getBrush()
    {
        return brushhandle;
    }

    Rectangle getClip()
    {
        return clip;
    }

    int getCount()
    {
        return count;
    }

    int getFont()
    {
        return fonthandle;
    }

    Point getOrigin()
    {
        return origin;
    }

    int getPen()
    {
        return penhandle;
    }

    void increaseCount()
    {
        count++;
    }

    void setBrush(int i)
    {
        brushhandle = i;
    }

    void setClip(Rectangle rectangle)
    {
        clip = rectangle;
    }

    void setFont(int i)
    {
        fonthandle = i;
    }

    void setOrigin(Point point)
    {
        origin.move(point.x, point.y);
    }

    void setPen(int i)
    {
        penhandle = i;
    }

    private int count;
    private int penhandle;
    private int brushhandle;
    private int fonthandle;
    private Rectangle clip;
    private Point origin;
}
