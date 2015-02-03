/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-01 05:39:15 +0200 (mer., 01 juil. 2009) $
 * $Revision: 11162 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
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
package org.jmol.popup;

import org.jmol.api.*;

import java.awt.PopupMenu;
import java.awt.MenuComponent;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.CheckboxMenuItem;

// mth 2003 05 27
// This class is built with awt instead of swing so that it will
// operate as an applet with old JVMs

public class JmolPopupAwt extends JmolPopup {

  PopupMenu awtPopup;
  Menu mainMenu;
  Menu elementComputedMenu;
  static int MENUITEM_HEIGHT = 20; 

  public JmolPopupAwt(JmolViewer viewer, boolean asPopup) {
    super(viewer, asPopup);
    if (asPopup) {
      mainMenu = awtPopup = new PopupMenu("Jmol");
      display.add(awtPopup);
    } else {
      mainMenu = new Menu("Jmol");
    }
    build(mainMenu);
  }

  void showPopupMenu(int x, int y) {
    awtPopup.show(display, x, y);
  }

  public void installMainMenu(Object objMenuBar) {
    // not needed for AWT system.
  }

  Object getParent(Object menu) {
    return ((Menu)menu).getParent();  
  }

  int getMenuItemHeight() {
    return MENUITEM_HEIGHT;
  }

  int getPosition(Object menu) {
    Object p = getParent(menu);
    if (p instanceof PopupMenu) {
      for (int i = ((PopupMenu) p).getItemCount(); --i >= 0;)
        if (((PopupMenu) p).getItem(i) == menu)
          return i;
    } else {
      for (int i = ((Menu)p).getItemCount(); --i >= 0;)
        if (((Menu)p).getItem(i) == menu)
          return i;
    }
    return -1;
  }

  void insertMenuSubMenu(Object menu, Object subMenu, int index) {
    if (menu instanceof PopupMenu)
      ((PopupMenu)menu).insert((Menu)subMenu, index);
    else
      ((Menu)menu).insert((Menu)subMenu, index);
  }
  
  void createFrankPopup() {
    frankPopup = new PopupMenu("Frank");
    display.add((PopupMenu)frankPopup);
  }
  
  void showFrankMenu(int x, int y) {
    ((PopupMenu)frankPopup).show(display, x, y);
  }

  void resetFrankMenu() {
    PopupMenu menu = (PopupMenu) frankPopup;
    menu.removeAll();
  }

  void addToMenu(Object menu, MenuItem item) {
    ((Menu)menu).add(item);
  }

  ////////////////////////////////////////////////////////////////

  void addMenuSeparator(Object menu) {
    ((Menu)menu).addSeparator();
  }

  Object addMenuItem(Object menu, String entry, String script, String id) {
    MenuItem mi = new MenuItem(entry);
    updateMenuItem(mi, entry, script);
    mi.addActionListener(mil);
    mi.setName(id == null ? ((MenuComponent)menu).getName() + ".": id);
    addToMenu(menu, mi);
    return mi;
  }

  void setLabel(Object menu, String entry) {
    if (menu instanceof MenuItem)
      ((MenuItem) menu).setLabel(entry);
    else
      ((Menu) menu).setLabel(entry);
  }
  
  String getId(Object menu) {
    return ((MenuComponent) menu).getName();
  }
  
  void setCheckBoxValue(Object source) {
    CheckboxMenuItem cmi = (CheckboxMenuItem)source;
    setCheckBoxValue(cmi.getActionCommand(), cmi.getState());
  }

  void setCheckBoxState(Object item, boolean state) {
    ((CheckboxMenuItem) item).setState(state);
  }
 
  void updateMenuItem(Object menuItem, String entry, String script) {
    MenuItem mi = (MenuItem)menuItem;
    mi.setLabel(entry);
    mi.setActionCommand(script);
    // miguel 2004 12 03
    // greyed out menu entries are too hard to read
    //    mi.setEnabled(script != null);
  }

  Object addCheckboxMenuItem(Object menu, String entry, String basename,
                             String id, boolean state) {
    CheckboxMenuItem cmi = new CheckboxMenuItem(entry);
    cmi.setState(state);
    cmi.addItemListener(cmil);
    cmi.setActionCommand(basename);
    cmi.setName(id == null ? ((MenuComponent) menu).getName() + "." : id);
    addToMenu(menu, cmi);
    return cmi;
  }

  Object cloneMenu(Object menu) {
    return null;  
  }
  
  void addMenuSubMenu(Object menu, Object subMenu) {
    addToMenu(menu, (Menu)subMenu);
  }

  Object newMenu(String menuName, String id) {
    Menu m = new Menu(menuName);
    m.setName(id);
    return m;
  }

  void setAutoscrolls(Object menu) {
  }
  
  void renameMenu(Object menu, String newMenuName) {
    ((Menu)menu).setLabel(newMenuName);
  }

  Object newComputedMenu(String key, String word) {
    if ("elementComputedMenu".equals(key)) {
      elementComputedMenu = new Menu(word);
      return elementComputedMenu;
    }
    return new Menu("unrecognized ComputedMenu:" + key);
  }

  int getMenuItemCount(Object menu) {
    return ((Menu)menu).getItemCount();
  }

  void removeMenuItem(Object menu, int index) {
    ((Menu)menu).remove(index);
  }

  void removeAll(Object menu) {
    ((Menu)menu).removeAll();
  }

  void enableMenu(Object menu, boolean enable) {
    if (menu instanceof MenuItem) {
      enableMenuItem(menu, enable);
      return;
    }
   ((Menu)menu).setEnabled(enable);
  }
  
  void enableMenuItem(Object item, boolean enable) {
    ((MenuItem)item).setEnabled(enable);
  }
  
  String getMenuCurrent() {
    StringBuffer sb = new StringBuffer();
    PopupMenu main = (PopupMenu) htMenus.get("popupMenu");
    getMenuCurrent(sb, 0, main, "PopupMenu");
    return sb.toString();
  }

  private void getMenuCurrent(StringBuffer sb, int level, Menu menu, String menuName) {
    String name = menuName;
    String flags;
    int itemCount = menu.getItemCount();
    for (int i = 0; i < itemCount; i++) {
      MenuItem mi = menu.getItem(i);
      if (mi instanceof Menu) {
        Menu m = (Menu) mi;
        name = m.getName();
        flags = "enabled:" + m.isEnabled();
        addCurrentItem(sb, 'M', level, name, m.getLabel(), null, flags);
        getMenuCurrent(sb, level + 1, m, name);
      } else {
        String script = fixScript(mi.getName(), mi.getActionCommand());
        String label = mi.getLabel();
        flags = "enabled:" + mi.isEnabled();
        if (mi instanceof CheckboxMenuItem) 
          flags += ";checked:" + ((CheckboxMenuItem)mi).getState();
        if (label == "-")
          addCurrentItem(sb, 'S', level, name, null, null, null);
        else          
          addCurrentItem(sb, 'I', level, mi.getName(), label, script, flags);
      }
    }
  }
}
