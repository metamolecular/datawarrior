/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-24 06:08:26 +0200 (lun., 24 août 2009) $
 * $Revision: 11332 $
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
import org.jmol.i18n.GT;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

abstract public class JmolPopup {

  private final static boolean forceAwt = false;

  //list is saved in http://www.stolaf.edu/academics/chemapps/jmol/docs/misc
  private final static boolean dumpList = false;

  //public void finalize() {
  //  System.out.println("JmolPopup " + this + " finalize");
  //}

  JmolViewer viewer;
  Component display;
  MenuItemListener mil;
  CheckboxMenuItemListener cmil;
  boolean asPopup = true;

  Hashtable htMenus = new Hashtable();
  Properties menuText = new Properties();
  
  Object frankPopup;

  int aboutComputedMenuBaseCount;
  String nullModelSetName, modelSetName;
  String modelSetFileName, modelSetRoot;
  
  Hashtable modelSetInfo, modelInfo;
  Vector PDBOnly = new Vector();
  Vector UnitcellOnly = new Vector();
  Vector FramesOnly = new Vector();
  Vector VibrationOnly = new Vector();
  Vector SymmetryOnly = new Vector();
  Vector SignedOnly = new Vector();
  Vector AppletOnly = new Vector();
  Vector ChargesOnly = new Vector();
  Vector TemperatureOnly = new Vector();

  boolean isPDB;
  boolean isSymmetry;
  boolean isUnitCell;
  boolean isMultiFrame;
  boolean isMultiConfiguration;
  boolean isVibration;
  boolean isApplet;
  boolean isSigned;
  boolean isZapped;
  boolean haveCharges;
  boolean haveBFactors;
  String altlocs;

  int modelIndex, modelCount, atomCount;

  final static int MAX_ITEMS = 25;
  final static int TITLE_MAX_WIDTH = 20;

  static String menuStructure;
  
  JmolPopup(JmolViewer viewer, boolean asPopup) {
    this.viewer = viewer;
    this.asPopup = asPopup;
    display = viewer.getDisplay();
    mil = new MenuItemListener();
    cmil = new CheckboxMenuItemListener();
    //System.out.println("JmolPopup " + this + " constructor");
  }

  static public JmolPopup newJmolPopup(JmolViewer viewer, boolean doTranslate, String menu, boolean asPopup) {
    menuStructure = menu;
    GT.setDoTranslate(true);
    JmolPopup popup;
    try {
      popup = (!viewer.isJvm12orGreater() || forceAwt ? (JmolPopup) new JmolPopupAwt(
          viewer, asPopup)
          : (JmolPopup) new JmolPopupSwing(viewer, asPopup));
    } catch (Exception e) {
      Logger.error("JmolPopup not loaded");
      GT.setDoTranslate(doTranslate);
      return null;
    }
    // long runTime = System.currentTimeMillis() - beginTime;
    // Logger.debug("LoadPopupThread finished " + runTime + " ms");
    try {
      popup.updateComputedMenus();
    } catch (NullPointerException e) {
      // ignore -- the frame just wasn't ready yet;
      // updateComputedMenus() will be called again when the frame is ready; 
    }
    GT.setDoTranslate(doTranslate);
    return popup;
  }

  public abstract void installMainMenu(Object objMenuBar); 
  
  void build(Object popupMenu) {
    htMenus.put("popupMenu", popupMenu);
    boolean allowSignedFeatures = (!viewer.isApplet() || viewer.getBooleanProperty("_signedApplet"));
    addMenuItems("", "popupMenu", popupMenu, new PopupResourceBundle(menuStructure, menuText), viewer
        .isJvm12orGreater(), allowSignedFeatures);
  }

  public String getMenu(String title) {
    int pt = title.indexOf("|"); 
    if (pt >= 0) {
      String type = title.substring(pt);
      title = title.substring(0, pt);
      if (type.indexOf("current") >= 0) {
        return getMenuCurrent();
      }
    }
    return (new PopupResourceBundle(menuStructure, null)).getMenu(title);
  }
  
  abstract String getMenuCurrent();
  
  static protected void addCurrentItem(StringBuffer sb, char type, int level, String name, String label, String script, String flags) {
    sb.append(type).append(level).append('\t').append(name);
    if(label == null) {
      sb.append(".\n");
      return;
    }
    sb.append("\t").append(label)
        .append("\t").append(script == null || script.length() == 0 ? "-" : script)
        .append("\t").append(flags)
        .append("\n");
  }

  final static int UPDATE_ALL = 0;
  final static int UPDATE_CONFIG = 1;
  final static int UPDATE_SHOW = 2;
  int updateMode;

  private String getMenuText(String key) {
    String str = menuText.getProperty(key);
    return (str == null ? key : str);
  }


  public void updateComputedMenus() {
    updateMode = UPDATE_ALL;
    getViewerData();
    //System.out.println("jmolPopup updateComputedMenus " + modelSetFileName + " " + modelSetName + " " + atomCount);
    updateSelectMenu();
    updateWriteMenu();
    updateElementsComputedMenu(viewer.getElementsPresentBitSet(modelIndex));
    updateHeteroComputedMenu(viewer.getHeteroList(modelIndex));
    updateSurfMoComputedMenu((Hashtable) modelInfo.get("moData"));
    updateFileTypeDependentMenus();
    updatePDBComputedMenus();
    updateMode = UPDATE_CONFIG;
    updateConfigurationComputedMenu();
    updateSYMMETRYComputedMenus();
    updateFRAMESbyModelComputedMenu();
    updateModelSetComputedMenu();
    updateLanguageSubmenu();
    updateAboutSubmenu();
  }

  private void updateWriteMenu() {
    Object menu = htMenus.get("SIGNEDwriteMenu");
    if (menu == null)
      return;
    String text = getMenuText("writeFileTextVARIABLE");
    menu = htMenus.get("writeFileTextVARIABLE");
    setLabel(menu, GT.translate(text, modelSetFileName, true));
    enableMenuItem(menu, !modelSetFileName.equals("zapped"));
  }

  private void getViewerData() {
    isApplet = viewer.isApplet();
    isSigned = (viewer.getBooleanProperty("_signedApplet"));
    modelSetName = viewer.getModelSetName();
    modelSetRoot = (modelSetName == null 
        || modelSetName.indexOf("<") >= 0 
        || modelSetName.indexOf("[") >= 0
        || modelSetName.indexOf(" ") >= 0 ? "Jmol"
            : modelSetName);

    modelSetFileName = viewer.getModelSetFileName();
    if ("string".equals(modelSetFileName))
        modelSetFileName = "";
                
    isZapped = ("zapped".equals(modelSetName));
    modelIndex = viewer.getDisplayModelIndex();
    modelCount = viewer.getModelCount();
    atomCount = viewer.getAtomCountInModel(modelIndex);
    modelSetInfo = viewer.getModelSetAuxiliaryInfo();
    modelInfo = viewer.getModelAuxiliaryInfo(modelIndex);
    if (modelInfo == null)
      modelInfo = new Hashtable();
    isPDB = checkBoolean(modelSetInfo, "isPDB");
    isSymmetry = checkBoolean(modelSetInfo, "someModelsHaveSymmetry");
    isUnitCell = checkBoolean(modelSetInfo, "someModelsHaveUnitcells");
    isMultiFrame = (modelCount > 1);
    altlocs = viewer.getAltLocListInModel(modelIndex);
    isMultiConfiguration = (altlocs.length() > 0);
    isVibration = (viewer.modelHasVibrationVectors(modelIndex));
    haveCharges = (viewer.havePartialCharges());
    haveBFactors = (viewer.getBooleanProperty("haveBFactors"));
  }

  private void updateForShow() {
    updateMode = UPDATE_SHOW;
    updateSelectMenu();
    updateFRAMESbyModelComputedMenu();
    updateModelSetComputedMenu();
    updateAboutSubmenu();
  }

  boolean checkBoolean(Hashtable info, String key) {
    if (info == null || !info.containsKey(key))
      return false;
    return ((Boolean) (info.get(key))).booleanValue();
  }

  void updateSelectMenu() {
    Object menu = htMenus.get("selectMenuText");
    if (menu == null)
      return;
    enableMenu(menu, atomCount != 0);
    setLabel(menu, GT.translate(getMenuText("selectMenuText"), viewer.getSelectionCount(), true));
  }

  void updateElementsComputedMenu(BitSet elementsPresentBitSet) {
    Object menu = htMenus.get("elementsComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);
    if (elementsPresentBitSet == null)
      return;
    for (int i = 0; i < JmolConstants.elementNumberMax; ++i) {
      if (elementsPresentBitSet.get(i)) {
        String elementName = JmolConstants.elementNameFromNumber(i);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(i);
        String entryName = elementSymbol + " - " + elementName;
          addMenuItem(menu, entryName, "SELECT " + elementName, null);
      }
    }
    for (int i = JmolConstants.firstIsotope; i < JmolConstants.altElementMax; ++i) {
      int n = JmolConstants.elementNumberMax + i;
      if (elementsPresentBitSet.get(n)) {
        n = JmolConstants.altElementNumberFromIndex(i);
        String elementName = JmolConstants.elementNameFromNumber(n);
        String elementSymbol = JmolConstants.elementSymbolFromNumber(n);
        String entryName = elementSymbol + " - " + elementName;
        addMenuItem(menu, entryName, "SELECT " + elementName, null);
      }
    }
    enableMenu(menu, true);
  }

  void updateHeteroComputedMenu(Hashtable htHetero) {
    Object menu = htMenus.get("PDBheteroComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);
    if (htHetero == null)
      return;
    Enumeration e = htHetero.keys();
    int n = 0;
    while (e.hasMoreElements()) {
      String heteroCode = (String) e.nextElement();
      String heteroName = (String) htHetero.get(heteroCode);
      if (heteroName.length() > 20)
        heteroName = heteroName.substring(0, 20) + "...";
      String entryName = heteroCode + " - " + heteroName;
      addMenuItem(menu, entryName, "SELECT [" + heteroCode + "]", null);
      n++;
    }
    enableMenu(menu, (n > 0));
  }

  void updateSurfMoComputedMenu(Hashtable moData) {
    Object menu = htMenus.get("surfMoComputedMenu");
    if (menu == null)
      return;
    enableMenu(menu, false);
    removeAll(menu);
    if (moData == null)
      return;
    Vector mos = (Vector) (moData.get("mos"));
    int nOrb = (mos == null ? 0 : mos.size());
    if (nOrb == 0)
      return;
    enableMenu(menu, true);
    Object subMenu = menu;
    int nmod = (nOrb % MAX_ITEMS);
    if (nmod == 0)
      nmod = MAX_ITEMS;
    int pt = (nOrb > MAX_ITEMS ? 0 : Integer.MIN_VALUE);
    for (int i = nOrb; --i >= 0;) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        if (pt == nmod + 1)
          nmod = MAX_ITEMS;
        String id = "mo" + pt + "Menu";
        subMenu = newMenu(Math.max(i + 2 - nmod, 1) + "..." + (i + 1),
            getId(menu) + "." + id);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      Hashtable mo = (Hashtable) mos.get(i);
      String entryName = "#" + (i + 1) + " " 
          + (mo.containsKey("energy") ? mo.get("energy") : "") + " " 
          + (mo.containsKey("type") ? (String)mo.get("type") : "");
      String script = "mo " + (i + 1);
      addMenuItem(subMenu, entryName, script, null);
    }
  }

  void updatePDBComputedMenus() {

    Object menu = htMenus.get("PDBaaResiduesComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);

    Object menu1 = htMenus.get("PDBnucleicResiduesComputedMenu");
    if (menu1 == null)
      return;
    removeAll(menu1);
    enableMenu(menu1, false);

    Object menu2 = htMenus.get("PDBcarboResiduesComputedMenu");
    if (menu2 == null)
      return;
    removeAll(menu2);
    enableMenu(menu2, false);
    if (modelSetInfo == null)
      return;
    int n = (modelIndex < 0 ? 0 : modelIndex + 1);
    String[] lists = ((String[]) modelSetInfo.get("group3Lists"));
    group3List = (lists == null ? null : lists[n]);
    group3Counts = (lists == null ? null : ((int[][]) modelSetInfo
        .get("group3Counts"))[n]);

    if (group3List == null)
      return;
    //next is correct as "<=" because it includes "UNK"
    int nItems = 0;
    for (int i = 1; i < JmolConstants.GROUPID_AMINO_MAX; ++i)
      nItems += updateGroup3List(menu, JmolConstants.predefinedGroup3Names[i]);
    nItems += augmentGroup3List(menu, "p>", true);
    enableMenu(menu, (nItems > 0));
    enableMenu(htMenus.get("PDBproteinMenu"), (nItems > 0));

    nItems = augmentGroup3List(menu1, "n>", false);
    enableMenu(menu1, nItems > 0);
    enableMenu(htMenus.get("PDBnucleicMenu"), (nItems > 0));

    nItems = augmentGroup3List(menu2, "c>", false);
    enableMenu(menu2, nItems > 0);
    enableMenu(htMenus.get("PDBcarboMenu"), (nItems > 0));
  }

  String group3List;
  int[] group3Counts;

  int updateGroup3List(Object menu, String name) {
    int nItems = 0;
    int n = group3Counts[group3List.indexOf(name) / 6];
    String script = null;
    if (n > 0) {
      script ="SELECT " + name;
      name += "  (" + n + ")";
      nItems++;
    } else {
      script = null;
    }
    Object item = addMenuItem(menu, name, script, getId(menu) + "." + name);
    if (n == 0)
      enableMenuItem(item, false);
    return nItems;
  }

  int augmentGroup3List(Object menu, String type, boolean addSeparator) {
    int pt = JmolConstants.GROUPID_AMINO_MAX * 6 - 6;
    // ...... p>AFN]o>ODH]n>+T ]
    int nItems = 0;
    while (true) {
      pt = group3List.indexOf(type, pt);
      if (pt < 0)
        break;
      if (nItems++ == 0 && addSeparator)
        addMenuSeparator(menu);
      int n = group3Counts[pt / 6];
      String heteroCode = group3List.substring(pt + 2, pt + 5);
      String name = heteroCode + "  (" + n + ")";
      addMenuItem(menu, name, "SELECT [" + heteroCode + "]", getId(menu) + "." + name);
      pt++;
    }
    return nItems;
  }

  void updateSYMMETRYComputedMenus() {
    updateSYMMETRYSelectComputedMenu();
    updateSYMMETRYShowComputedMenu();
  }

  private void updateSYMMETRYShowComputedMenu() {
    Object menu = htMenus.get("SYMMETRYShowComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);
    if (!isSymmetry || modelIndex < 0)
      return;
    Hashtable info = (Hashtable) viewer.getProperty("DATA_API", "spaceGroupInfo", null);
    if (info == null)
      return;
    Object[][] infolist = (Object[][]) info.get("operations");
    if (infolist == null)
      return;
    String name = (String) info.get("spaceGroupName");
    setLabel(menu, name == null ? GT.translate("Space Group") : name);
    Object subMenu = menu;
    int nmod = MAX_ITEMS;
    int pt = (infolist.length > MAX_ITEMS ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < infolist.length; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "drawsymop" + pt + "Menu";
        subMenu = newMenu((i + 1) + "..."
            + Math.min(i + MAX_ITEMS, infolist.length), getId(menu) + "." + id);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String entryName = (i + 1) + " " + infolist[i][2] + " (" + infolist[i][0] + ")";
      enableMenuItem(addMenuItem(subMenu, entryName, "draw SYMOP " + (i + 1), null), true);
    }
    enableMenu(menu, true);
  }

  private void updateSYMMETRYSelectComputedMenu() {
    Object menu = htMenus.get("SYMMETRYSelectComputedMenu");
    if (menu == null)
      return;
    removeAll(menu);
    enableMenu(menu, false);
    if (!isSymmetry || modelIndex < 0)
      return;
    String[] list = (String[]) modelInfo.get("symmetryOperations");
    if (list == null)
      return;
    int[] cellRange = (int[]) modelInfo.get("unitCellRange");
    boolean haveUnitCellRange = (cellRange != null);
    Object subMenu = menu;
    int nmod = MAX_ITEMS;
    int pt = (list.length > MAX_ITEMS ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < list.length; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "symop" + pt + "Menu";
        subMenu = newMenu((i + 1) + "..."
            + Math.min(i + MAX_ITEMS, list.length), getId(menu) + "." + id);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String entryName = "symop=" + (i + 1) + " # " + list[i];
      enableMenuItem(addMenuItem(subMenu, entryName, "SELECT symop=" + (i + 1), null),
          haveUnitCellRange);
    }
    enableMenu(menu, true);
  }

  void updateFRAMESbyModelComputedMenu() {
    //allowing this in case we move it later
    Object menu = htMenus.get("FRAMESbyModelComputedMenu");
    if (menu == null)
      return;
    enableMenu(menu, (modelCount > 1));
    setLabel(menu, (modelIndex < 0 ? GT.translate(getMenuText("allModelsText"), modelCount, true)
        : getModelLabel()));
    removeAll(menu);
    if (modelCount < 2)
      return;
    addCheckboxMenuItem(menu, GT.translate("All", true), "frame 0 ##", null,
        (modelIndex < 0));

    Object subMenu = menu;
    int nmod = MAX_ITEMS;
    int pt = (modelCount > MAX_ITEMS ? 0 : Integer.MIN_VALUE);
    for (int i = 0; i < modelCount; i++) {
      if (pt >= 0 && (pt++ % nmod) == 0) {
        String id = "model" + pt + "Menu";
        subMenu = newMenu(
            (i + 1) + "..." + Math.min(i + MAX_ITEMS, modelCount), getId(menu)
                + "." + id);
        addMenuSubMenu(menu, subMenu);
        htMenus.put(id, subMenu);
        pt = 1;
      }
      String script = "" + viewer.getModelNumberDotted(i);
      String entryName = viewer.getModelName(i);
      if (!entryName.equals(script))
        entryName = script + ": " + entryName;
      if (entryName.length() > 50)
        entryName = entryName.substring(0, 45) + "...";
      addCheckboxMenuItem(subMenu, entryName, "model " + script + " ##", null,
          (modelIndex == i));
    }
  }

  String configurationSelected = "";

  void updateConfigurationComputedMenu() {
    Object menu = htMenus.get("configurationComputedMenu");
    if (menu == null)
      return;
    enableMenu(menu, isMultiConfiguration);
    if (!isMultiConfiguration)
      return;
    int nAltLocs = altlocs.length();
    setLabel(menu, GT.translate(getMenuText("configurationMenuText"), nAltLocs, true));
    removeAll(menu);
    String script = "hide none ##CONFIG";
    addCheckboxMenuItem(menu, GT.translate("All", true), script, null,
        (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)));
    for (int i = 0; i < nAltLocs; i++) {
      script = "configuration " + (i + 1) + "; hide thisModel and not selected ##CONFIG";
      String entryName = "" + (i + 1) + " -- \"" + altlocs.charAt(i) + "\"";
      addCheckboxMenuItem(menu, entryName, script, null,
          (updateMode == UPDATE_CONFIG && configurationSelected.equals(script)));
    }
  }

  void updateModelSetComputedMenu() {
    Object menu = htMenus.get("modelSetMenu");
    if (menu == null)
      return;
    removeAll(menu);
    renameMenu(menu, nullModelSetName);
    enableMenu(menu, false);
    enableMenu(htMenus.get("surfaceMenu"), !isZapped);
    enableMenu(htMenus.get("measureMenu"), !isZapped);
    enableMenu(htMenus.get("pickingMenu"), !isZapped);
    if (modelSetName == null || isZapped)
      return;
    if (isMultiFrame) {
      modelSetName = GT.translate(getMenuText("modelSetCollectionText"), modelCount);
      if (modelSetName.length() > TITLE_MAX_WIDTH)
        modelSetName = modelSetName.substring(0, TITLE_MAX_WIDTH) + "...";
    } else if (viewer.getBooleanProperty("hideNameInPopup")) {
      modelSetName = getMenuText("hiddenModelSetText");
    } else if (modelSetName.length() > TITLE_MAX_WIDTH) {
      modelSetName = modelSetName.substring(0, TITLE_MAX_WIDTH) + "...";
    }
    renameMenu(menu, modelSetName);
    enableMenu(menu, true);
    // 100 here is totally arbitrary. You can do a minimization on any number of atoms
    enableMenuItem(addMenuItem(menu, GT.translate("Minimize"), "minimize", null), atomCount <= 100);
    addMenuSeparator(menu);
    addMenuItem(menu, GT.translate(getMenuText("atomsText"), atomCount, true));
    addMenuItem(menu, GT.translate(getMenuText("bondsText"), viewer
        .getBondCountInModel(modelIndex), true));
    if (isPDB) {
      addMenuSeparator(menu);
      addMenuItem(menu, GT.translate(getMenuText("groupsText"), viewer
          .getGroupCountInModel(modelIndex), true));
      addMenuItem(menu, GT.translate(getMenuText("chainsText"), viewer
          .getChainCountInModel(modelIndex), true));
      addMenuItem(menu, GT.translate(getMenuText("polymersText"), viewer
          .getPolymerCountInModel(modelIndex), true));
      Object submenu = htMenus.get("BiomoleculesMenu");
      if (submenu == null) {
        submenu = newMenu(GT.translate(getMenuText("biomoleculesMenuText")),
            getId(menu) + ".biomolecules");
        addMenuSubMenu(menu, submenu);
      }
      removeAll(submenu);
      enableMenu(submenu, false);
      Vector biomolecules;
      if (modelIndex >= 0
          && (biomolecules = (Vector) viewer.getModelAuxiliaryInfo(modelIndex,
              "biomolecules")) != null) {
        enableMenu(submenu, true);
        int nBiomolecules = biomolecules.size();
        for (int i = 0; i < nBiomolecules; i++) {
          String script = (isMultiFrame ? ""
              : "save orientation;load \"\" FILTER \"biomolecule " + (i + 1) + "\";restore orientation;");
          int nAtoms = ((Integer) ((Hashtable) biomolecules.elementAt(i)).get("atomCount")).intValue();
          String entryName = GT.translate(getMenuText(isMultiFrame ? "biomoleculeText"
              : "loadBiomoleculeText"), new Object[] { new Integer(i + 1),
              new Integer(nAtoms) });
          addMenuItem(submenu, entryName, script, null);
        }
      }
    }
    if (isApplet && viewer.showModelSetDownload()
        && !viewer.getBooleanProperty("hideNameInPopup")) {
      addMenuSeparator(menu);
      addMenuItem(menu, GT.translate(getMenuText("viewMenuText"), 
          modelSetFileName, true), "show url", null);
    }
  }

  void updateFileTypeDependentMenus() {
    for (int i = 0; i < PDBOnly.size(); i++)
      enableMenu(PDBOnly.get(i), isPDB);
    for (int i = 0; i < UnitcellOnly.size(); i++)
      enableMenu(UnitcellOnly.get(i), isUnitCell);
    for (int i = 0; i < FramesOnly.size(); i++)
      enableMenu(FramesOnly.get(i), isMultiFrame);
    for (int i = 0; i < VibrationOnly.size(); i++)
      enableMenu(VibrationOnly.get(i), isVibration);
    for (int i = 0; i < SymmetryOnly.size(); i++)
      enableMenu(SymmetryOnly.get(i), isSymmetry && isUnitCell);
    for (int i = 0; i < SignedOnly.size(); i++)
      enableMenu(SignedOnly.get(i), isSigned || !isApplet);
    for (int i = 0; i < AppletOnly.size(); i++)
      enableMenu(AppletOnly.get(i), isApplet);
    for (int i = 0; i < ChargesOnly.size(); i++)
      enableMenu(ChargesOnly.get(i), haveCharges);
    for (int i = 0; i < TemperatureOnly.size(); i++)
      enableMenu(TemperatureOnly.get(i), haveBFactors);
  }

  String getModelLabel() {
    return GT.translate(getMenuText("modelMenuText"), (modelIndex + 1) + "/" + modelCount, true);
  }

  private void updateAboutSubmenu() {
    Object menu = htMenus.get("aboutComputedMenu");
    if (menu == null)
      return;
    for (int i = getMenuItemCount(menu); --i >= aboutComputedMenuBaseCount;)
      removeMenuItem(menu, i);
    addMenuSeparator(menu);
    addMenuItem(menu, "Jmol " + JmolConstants.version + (isSigned ? " (signed)" : ""));
    addMenuItem(menu, JmolConstants.date);
    addMenuItem(menu, viewer.getOperatingSystemName());
    addMenuItem(menu, viewer.getJavaVendor());
    addMenuItem(menu, viewer.getJavaVersion());
    addMenuSeparator(menu);
    addMenuItem(menu, GT.translate("Java memory usage", true));
    Runtime runtime = Runtime.getRuntime();
    //runtime.gc();
    long mbTotal = convertToMegabytes(runtime.totalMemory());
    long mbFree = convertToMegabytes(runtime.freeMemory());
    long mbMax = convertToMegabytes(maxMemoryForNewerJvm());
    addMenuItem(menu, GT.translate("{0} MB total", new Object[] { new Long(mbTotal) },
        true));
    addMenuItem(menu, GT.translate("{0} MB free", new Object[] { new Long(mbFree) },
        true));
    if (mbMax > 0)
      addMenuItem(menu, GT.translate("{0} MB maximum",
          new Object[] { new Long(mbMax) }, true));
    else
      addMenuItem(menu, GT.translate("unknown maximum", true));
    int availableProcessors = availableProcessorsForNewerJvm();
    if (availableProcessors > 0)
      addMenuItem(menu, (availableProcessors == 1) ? GT.translate("1 processor", true)
          : GT.translate("{0} processors", availableProcessors, true));
    else
      addMenuItem(menu, GT.translate("unknown processor count", true));
  }

  private void updateLanguageSubmenu() {
    Object menu = htMenus.get("languageComputedMenu");
    if (menu == null)
      return;
    for (int i = getMenuItemCount(menu); --i >= 0;)
      removeMenuItem(menu, i);
    String language = GT.getLanguage();
    String id = getId(menu);
    GT.Language[] languages = GT.getLanguageList();
    for (int i = 0; i < languages.length; i++) {
      if (languages[i].display) {
        String code = languages[i].code;
        String name = languages[i].language;
        addCheckboxMenuItem(
            menu,
            GT.translate(name, true) + " (" + code + ")",
            "language = \"" + code + "\" ##" + name,
            id + "." + code,
            language.equals(code));
      }
    }
  }

  private long convertToMegabytes(long num) {
    if (num <= Long.MAX_VALUE - 512 * 1024)
      num += 512 * 1024;
    return num / (1024 * 1024);
  }

  private void addMenuItems(String parentId, String key, Object menu,
                            PopupResourceBundle popupResourceBundle,
                            boolean isJVM12orGreater, boolean allowSignedFeatures) {
    String id = parentId + "." + key;
    String value = popupResourceBundle.getStructure(key);
    //Logger.debug(id + " --- " + value);
    if (value == null) {
      addMenuItem(menu, "#" + key, "", "");
      return;
    }
    // process predefined @terms
    StringTokenizer st = new StringTokenizer(value);
    String item;
    while (value.indexOf("@") >= 0) {
      String s = "";
      while (st.hasMoreTokens())
        s += " " + ((item = st.nextToken()).startsWith("@") 
            ? popupResourceBundle.getStructure(item) : item);
      value = s.substring(1);
      st = new StringTokenizer(value);
    }
    while (st.hasMoreTokens()) {
      Object newMenu = null;
      String script = "";
      item = st.nextToken();
      boolean isDisabled = (!isJVM12orGreater && item.indexOf("JVM12") >= 0);
      String word = popupResourceBundle.getWord(item);
      if (item.indexOf("Menu") >= 0) {
        if (!allowSignedFeatures && item.startsWith("SIGNED"))
          continue;
        Object subMenu = newMenu(word, id + "." + item);        
        addMenuSubMenu(menu, subMenu);
        htMenus.put(item, subMenu);
        if (item.indexOf("Computed") < 0)
          addMenuItems(id, item, subMenu, popupResourceBundle, isJVM12orGreater, allowSignedFeatures);
        // these will need tweaking:
        if ("aboutComputedMenu".equals(item)) {
          aboutComputedMenuBaseCount = getMenuItemCount(subMenu);
        } else if ("modelSetMenu".equals(item)) {
          nullModelSetName = word;
          enableMenu(subMenu, false);
        }
        newMenu = subMenu;
        if (isDisabled)
          enableMenu(newMenu, false);
      } else if ("-".equals(item)) {
        addMenuSeparator(menu);
      } else if (item.endsWith("Checkbox")) {
        script = popupResourceBundle.getStructure(item);
        String basename = item.substring(0, item.length() - 8);
        if (script == null || script.length() == 0)
          script = "set " + basename + " T/F";
        newMenu = addCheckboxMenuItem(menu, word, basename 
            + ":" + script, id + "." + item);
      } else {
        script = popupResourceBundle.getStructure(item);
        if (script == null)
          script = item;
        newMenu = addMenuItem(menu, word, script, id + "." + item);
        if (isDisabled)
          enableMenuItem(newMenu, false);
      }

      if (item.indexOf("VARIABLE") >= 0)
        htMenus.put(item, newMenu);
      // menus or menu items:
      if (item.indexOf("PDB") >= 0) {
        PDBOnly.add(newMenu);
      } else if (item.indexOf("URL") >= 0) {
        AppletOnly.add(newMenu);
      } else if (item.indexOf("CHARGE") >= 0) {
        ChargesOnly.add(newMenu);
      } else if (item.indexOf("BFACTORS") >= 0) {
        TemperatureOnly.add(newMenu);
      } else if (item.indexOf("UNITCELL") >= 0) {
        UnitcellOnly.add(newMenu);
      } else if (item.indexOf("FRAMES") >= 0) {
        FramesOnly.add(newMenu);
      } else if (item.indexOf("VIBRATION") >= 0) {
        VibrationOnly.add(newMenu);
      } else if (item.indexOf("SYMMETRY") >= 0) {
        SymmetryOnly.add(newMenu);
      }
      if (item.startsWith("SIGNED"))
        SignedOnly.add(newMenu);

      if (dumpList) {
        String str = item.endsWith("Menu") ? "----" : id + "." + item + "\t"
            + word + "\t" + fixScript(id + "." + item, script);
        str = "addMenuItem('\t" + str + "\t')";
        Logger.info(str);
      }
    }
  }

  Hashtable htCheckbox = new Hashtable();

  void rememberCheckbox(String key, Object checkboxMenuItem) {
    htCheckbox.put(key, checkboxMenuItem);
  }

  /**
   * (1) setOption --> set setOption true or set setOption false
   *  
   * @param what option to set
   * @param TF   true or false
   */
  void setCheckBoxValue(String what, boolean TF) {
    int pt;
    if (what.indexOf("##") < 0) {
      // not a special computed checkbox
      String basename = what.substring(0, (pt = what.indexOf(":")));
      if (viewer.getBooleanProperty(basename) == TF)
        return;
      what = what.substring(pt + 1);
      if ((pt = what.indexOf("|")) >= 0)
        what = (TF ? what.substring(0, pt) : what.substring(pt + 1)).trim();
      what = TextFormat.simpleReplace(what, "T/F", (TF ? " TRUE" : " FALSE"));
    }
    viewer.evalStringQuiet(what);
    if (what.indexOf("#CONFIG") >= 0) {
      configurationSelected = what;
      updateConfigurationComputedMenu();
      updateModelSetComputedMenu();
    }
  }

  String currentMenuItemId = null;

  class MenuItemListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      restorePopupMenu();
      String script = e.getActionCommand();
      if (script == null || script.length() == 0)
        return;
      if (script.equals("MAIN")) {
        show(thisx, thisy);
        return;
      }
      String id = getId(e.getSource());
      if (id != null) {
        script = fixScript(id, script);
        currentMenuItemId = id;
      }
      viewer.evalStringQuiet(script);
    }
  }

  class CheckboxMenuItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      restorePopupMenu();
      setCheckBoxValue(e.getSource());
      String id = getId(e.getSource());
      if (id != null) {
        currentMenuItemId = id;
      }
      //Logger.debug("CheckboxMenuItemListener() " + e.getSource());
    }
  }

  String fixScript(String id, String script) {
    int pt;
    if (script == "" || id.endsWith("Checkbox"))
      return script;

    if (script.indexOf("SELECT") == 0) {
      return "select thisModel and (" + script.substring(6) + ")";
    }

    if ((pt = id.lastIndexOf("[")) >= 0) {
      // setSpin
      id = id.substring(pt + 1);
      if ((pt = id.indexOf("]")) >= 0)
        id = id.substring(0, pt);
      id = id.replace('_', ' ');
      if (script.indexOf("[]") < 0)
        script = "[] " + script;
      return TextFormat.simpleReplace(script, "[]", id); 
    } else if (script.indexOf("?FILEROOT?") >= 0) {
      script = TextFormat.simpleReplace(script, "FILEROOT?", modelSetRoot);
    } else if (script.indexOf("?FILE?") >= 0) {
      script = TextFormat.simpleReplace(script, "FILE?", modelSetFileName);
    } else if (script.indexOf("?PdbId?") >= 0) {
      script = TextFormat.simpleReplace(script, "PdbId?", 
          "=" + (modelSetRoot.length() == 4 && modelSetRoot.indexOf(".") < 0 ? modelSetRoot : "1crn"));
    }
    return script;
  }

  Object addMenuItem(Object menuItem, String entry) {
    return addMenuItem(menuItem, entry, "", null);
  }

  Object addCheckboxMenuItem(Object menu, String entry, String basename,
                             String id) {
    Object item = addCheckboxMenuItem(menu, entry, basename, id, false);
    rememberCheckbox(basename, item);
    return item;
  }

  int thisx, thisy;

  public void show(int x, int y) {
    thisx = x;
    thisy = y;
    String id = currentMenuItemId;
    updateForShow();
    for (Enumeration keys = htCheckbox.keys(); keys.hasMoreElements();) {
      String key = (String) keys.nextElement();
      Object item = htCheckbox.get(key);
      String basename = key.substring(0, key.indexOf(":"));
      boolean b = viewer.getBooleanProperty(basename);
      setCheckBoxState(item, b);
    }
    if (x < 0) {
      setFrankMenu(id);
      thisx = -x - 50;
      if (nFrankList > 1) {
        thisy = y - nFrankList * getMenuItemHeight();
        showFrankMenu(thisx, thisy);
        return;
      }
    }
    restorePopupMenu();
    if (asPopup)
      showPopupMenu(thisx, thisy);
  }

  Object[][] frankList = new Object[10][]; //enough to cover menu drilling
  int nFrankList = 0;
  String currentFrankId = null;

  void setFrankMenu(String id) {
    if (currentFrankId != null && currentFrankId == id && nFrankList > 0)
      return;
    if (frankPopup == null)
      createFrankPopup();
    resetFrankMenu();
    if (id == null)
      return;
    currentFrankId = id;
    nFrankList = 0;
    frankList[nFrankList++] = new Object[] { null, null, null };
    addMenuItem(frankPopup, GT.translate(getMenuText("mainMenuText")), "MAIN", "");
    for (int i = id.indexOf(".", 2) + 1;;) {
      int iNew = id.indexOf(".", i);
      if (iNew < 0)
        break;
      String strMenu = id.substring(i, iNew);
      Object menu = htMenus.get(strMenu);
      frankList[nFrankList++] = new Object[] { getParent(menu), menu,
          new Integer(getPosition(menu)) };
      addMenuSubMenu(frankPopup, menu);
      i = iNew + 1;
    }
  }

  void restorePopupMenu() {
    if (nFrankList < 2)
      return;
    // first entry is just the main item
    for (int i = nFrankList; --i > 0;) {
      insertMenuSubMenu(frankList[i][0], frankList[i][1],
          ((Integer) frankList[i][2]).intValue());
    }
    nFrankList = 1;
  }

  ////////////////////////////////////////////////////////////////

  abstract void resetFrankMenu();

  abstract Object getParent(Object menu);

  abstract void insertMenuSubMenu(Object menu, Object subMenu, int index);

  abstract int getPosition(Object menu);

  abstract void showPopupMenu(int x, int y);

  abstract void showFrankMenu(int x, int y);

  abstract void setCheckBoxState(Object item, boolean state);

  abstract void addMenuSeparator(Object menu);

  abstract Object addMenuItem(Object menu, String entry, String script,
                              String id);

  abstract void setLabel(Object menu, String entry);

  abstract void updateMenuItem(Object menuItem, String entry, String script);

  abstract Object addCheckboxMenuItem(Object menu, String entry,
                                      String basename, String id, boolean state);

  abstract void addMenuSubMenu(Object menu, Object subMenu);

  abstract Object newMenu(String menuName, String id);

  abstract void enableMenu(Object menu, boolean enable);

  abstract void enableMenuItem(Object item, boolean enable);

  abstract void renameMenu(Object menu, String menuName);

  abstract void removeAll(Object menu);

  abstract int getMenuItemCount(Object menu);

  abstract void removeMenuItem(Object menu, int index);

  abstract String getId(Object menuItem);

  abstract void setCheckBoxValue(Object source);

  abstract void createFrankPopup();

  abstract int getMenuItemHeight();

  long maxMemoryForNewerJvm() {
    // this method is overridden in JmolPopupSwing for newer Javas
    // JmolPopupAwt does not implement this
    return 0;
  }

  int availableProcessorsForNewerJvm() {
    // this method is overridden in JmolPopupSwing for newer Javas
    // JmolPopupAwt does not implement this
    return 0;
  }

}
