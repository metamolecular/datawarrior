/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-01 02:02:02 +0200 (mer., 01 juil. 2009) $
 * $Revision: 11160 $
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

class MouseManager10 extends MouseManager {

  MouseManager10(Component display, Viewer viewer) {
    super(display, viewer);
    //Logger.debug("MouseManager10 implemented");
  }

  private int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & MIDDLE_RIGHT) == 0)  ? (modifiers | LEFT) : modifiers;
  }

  int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

  boolean handleOldJvm10Event(Event e) {
    int x = e.x, y = e.y, modifiers = e.modifiers;
    long time = e.when;
    modifiers = applyLeftMouse(modifiers);
    switch (e.id) {
    case Event.MOUSE_DOWN:
      xWhenPressed = x; yWhenPressed = y; modifiersWhenPressed10 = modifiers;
      mousePressed(time, x, y, modifiers, false);
      break;
    case Event.MOUSE_DRAG:
      mouseDragged(time, x, y, modifiers);
      break;
    case Event.MOUSE_ENTER:
      mouseEntered(time, x, y);
      break;
    case Event.MOUSE_EXIT:
      mouseExited(time, x, y);
      break;
    case Event.MOUSE_MOVE:
      mouseMoved(time, x, y, modifiers);
      break;
    case Event.MOUSE_UP:
      mouseReleased(time, x, y, modifiers);
      // simulate a mouseClicked event for us
      if (x == xWhenPressed && y == yWhenPressed &&
          modifiers == modifiersWhenPressed10) {
        // the underlying code will turn this into dbl clicks for us
        mouseClicked(time, x, y, modifiers, 1);
      }
      break;
    default:
      return false;
    }
    return true;
  }
}
