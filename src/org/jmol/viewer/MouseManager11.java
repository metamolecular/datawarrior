/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-01 05:39:15 +0200 (mer., 01 juil. 2009) $
 * $Revision: 11162 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;


import java.awt.Component;
import java.awt.Event;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

class MouseManager11 extends MouseManager
  implements MouseListener, MouseMotionListener {

  MouseManager11(Component display, Viewer viewer) {
    super(display, viewer);
    if (display == null)
      return;
    //Logger.debug("MouseManager11 implemented");
    display.addMouseListener(this);
    display.addMouseMotionListener(this);
  }

  void removeMouseListeners11() {
    viewer.getDisplay().removeMouseListener(this);
    viewer.getDisplay().removeMouseMotionListener(this);
  }

  boolean handleOldJvm10Event(Event e) {
    return false;
  }

  public void mouseClicked(MouseEvent e) {
    mouseClicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(),
                 e.getClickCount());
  }

  public void mouseEntered(MouseEvent e) {
    mouseEntered(e.getWhen(), e.getX(), e.getY());
  }
  
  public void mouseExited(MouseEvent e) {
    mouseExited(e.getWhen(), e.getX(), e.getY());
  }
  
  public void mousePressed(MouseEvent e) {
    mousePressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers(),
                 e.isPopupTrigger());
  }
  
  public void mouseReleased(MouseEvent e) {
    mouseReleased(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseDragged(MouseEvent e) {
    int modifiers = e.getModifiers();
    /****************************************************************
     * Netscape 4.* Win32 has a problem with mouseDragged
     * if you left-drag then none of the modifiers are selected
     * we will try to fix that here
     ****************************************************************/
    if ((modifiers & LEFT_MIDDLE_RIGHT) == 0)
      modifiers |= LEFT;
    /****************************************************************/      
    mouseDragged(e.getWhen(), e.getX(), e.getY(), modifiers);
  }

  public void mouseMoved(MouseEvent e) {
    mouseMoved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }
}
