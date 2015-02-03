/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-30 00:35:55 +0200 (dim., 30 août 2009) $
 * $Revision: 11391 $
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.Interface;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolImageCreatorInterface;
import org.jmol.api.JmolScriptEditorInterface;
import org.jmol.api.JmolSelectionListener;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.api.MinimizerInterface;
import org.jmol.api.SmilesMatcherInterface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.AtomDataServer;
import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.i18n.GT;
import org.jmol.modelset.Atom;
import org.jmol.modelset.AtomCollection;
import org.jmol.modelset.AtomIndexIterator;
import org.jmol.modelset.Bond;
import org.jmol.modelset.BoxInfo;
import org.jmol.modelset.MeasurementPending;
import org.jmol.modelset.ModelCollection.StateScript;
import org.jmol.modelset.ModelLoader;
import org.jmol.modelset.ModelSet;
import org.jmol.popup.JmolPopup;
import org.jmol.shape.Shape;
import org.jmol.util.Base64;
import org.jmol.util.BitSetUtil;
import org.jmol.util.CifDataReader;
import org.jmol.util.CommandHistory;
import org.jmol.util.Escape;
import org.jmol.util.JpegEncoder;
import org.jmol.util.Logger;
import org.jmol.util.Measure;
import org.jmol.util.Parser;
import org.jmol.util.Point3fi;
import org.jmol.util.Quaternion;
import org.jmol.util.TempArray;
import org.jmol.util.TextFormat;
import org.jmol.viewer.StateManager.Orientation;
//import java.util.Date;

/*
 * 
 * ****************************************************************
 * The JmolViewer can be used to render client molecules. Clients implement the
 * JmolAdapter. JmolViewer uses this interface to extract information from the
 * client data structures and render the molecule to the supplied
 * java.awt.Component
 * 
 * The JmolViewer runs on Java 1.4 virtual machines. The 3d graphics rendering
 * package is a software implementation of a z-buffer. It does not use Java3D
 * and does not use Graphics2D from Java 1.2. Therefore, it is well suited to
 * building web browser applets that will run on a wide variety of system
 * configurations.
 * 
 * public here is a test for applet-applet and JS-applet communication the idea
 * being that applet.getProperty("jmolViewer") returns this Viewer object,
 * allowing direct inter-process access to public methods.
 * 
 * e.g.
 * 
 * applet.getProperty("jmolApplet").getFullPathName()
 * 
 * 
 * This viewer can also be used with JmolData.jar, which is a 
 * frameless version of Jmol that can be used to batch-process
 * scripts from the command line. No shapes, no labels, no export
 * to JPG -- just raw data checking and output. 
 * 
 * 
 * 
 * ****************************************************************
 */

public class Viewer extends JmolViewer implements AtomDataServer {

  protected void finalize() throws Throwable {
    Logger.debug("viewer finalize " + this);
    super.finalize();
  }

  // these are all private now so we are certain they are not
  // being accesed by any other classes

  private Component display;
  private Graphics3D g3d;
  private JmolAdapter modelAdapter;

  public JmolAdapter getModelAdapter() {
    if (modelAdapter == null)
      modelAdapter = new SmarterJmolAdapter();
    return modelAdapter;
  }

  private CommandHistory commandHistory = new CommandHistory();
  private ColorManager colorManager;

  ScriptCompiler compiler;
  Hashtable definedAtomSets;

  private MinimizerInterface minimizer;

  MinimizerInterface getMinimizer(boolean createNew) {
    if (minimizer == null && createNew) {
      minimizer = (MinimizerInterface) Interface
          .getOptionInterface("minimize.Minimizer");
      minimizer.setProperty("viewer", this);
    }
    return minimizer;
  }

  private SmilesMatcherInterface smilesMatcher;

  SmilesMatcherInterface getSmilesMatcher() {
    if (smilesMatcher == null) {
      smilesMatcher = (SmilesMatcherInterface) Interface
          .getOptionInterface("smiles.PatternMatcher");
    }
    smilesMatcher.setModelSet(modelSet);
    return smilesMatcher;
  }

  private SymmetryInterface symmetry;

  public SymmetryInterface getSymmetry() {
    if (symmetry == null)
      symmetry = (SymmetryInterface) Interface
          .getOptionInterface("symmetry.Symmetry");
    return symmetry;
  }

  public Object getSymmetryInfo(BitSet bsAtoms, String xyz, int op, Point3f pt,
                                String id, int type) {
    return modelSet.getSymmetryInfo(bsAtoms, xyz, op, pt, id, type);
  }
  
  private void clearModelDependentObjects() {
    setFrameOffsets(null);
    if (minimizer != null) {
      minimizer.setProperty("clear", null);
      minimizer = null;
    }
    if (smilesMatcher != null) {
      smilesMatcher.setModelSet(null);
      smilesMatcher = null;
    }
    if (symmetry != null) {
      symmetry = null;
    }
  }

  ScriptEvaluator eval;
  private AnimationManager animationManager;
  private DataManager dataManager;
  private FileManager fileManager;
  public ModelManager modelManager;
  public ModelSet modelSet;
  public MouseManager mouseManager;
  private PickingManager pickingManager;
  public RepaintManager repaintManager;
  public ScriptManager scriptManager;
  private SelectionManager selectionManager;
  private StateManager stateManager;
  public StateManager.GlobalSettings global;

  StateManager.GlobalSettings getGlobalSettings() {
    return global;
  }

  private StatusManager statusManager;
  private TempArray tempManager;
  public TransformManager transformManager;

  private String strJavaVendor;
  private String strJavaVersion;
  private String strOSName;
  private String htmlName = "";

  private String fullName = "";
  private String syncId = "";
  private String appletDocumentBase = "";
  private String appletCodeBase = "";

  private boolean jvm11orGreater = false;
  private boolean jvm12orGreater = false;
  private boolean jvm14orGreater = false;

  public Viewer(Component display, JmolAdapter modelAdapter) {
    if (Logger.debugging) {
      Logger.debug("Viewer constructor " + this);
    }
    this.display = display;
    this.modelAdapter = modelAdapter;
    strJavaVendor = System.getProperty("java.vendor");
    strOSName = System.getProperty("os.name");
    strJavaVersion = System.getProperty("java.version");
    // Netscape on MacOS does not implement 1.1 event model
    jvm11orGreater = (strJavaVersion.compareTo("1.1") >= 0 && !(strJavaVendor
        .startsWith("Netscape")
        && strJavaVersion.compareTo("1.1.5") <= 0 && "Mac OS".equals(strOSName)));
    jvm12orGreater = (strJavaVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJavaVersion.compareTo("1.4") >= 0);
    stateManager = new StateManager(this);
    g3d = new Graphics3D(display);
    colorManager = new ColorManager(this, g3d);
    statusManager = new StatusManager(this);
    scriptManager = new ScriptManager(this);
    transformManager = new TransformManager11(this);
    selectionManager = new SelectionManager(this);
    pickingManager = new PickingManager(this);
    if (display != null) {
      if (jvm14orGreater)
        mouseManager = MouseWrapper14.alloc(display, this);
      else if (jvm11orGreater)
        mouseManager = MouseWrapper11.alloc(display, this);
      else
        mouseManager = new MouseManager10(display, this);
    }
    modelManager = new ModelManager(this);
    tempManager = new TempArray();
    dataManager = new DataManager();
    animationManager = new AnimationManager(this);
    repaintManager = new RepaintManager(this);
    initialize();
    fileManager = new FileManager(this);
    compiler = new ScriptCompiler(this);
    definedAtomSets = new Hashtable();
    eval = new ScriptEvaluator(this);
  }

  /**
   * NOTE: for APPLICATION AND APPLET call
   * 
   * setModeMouse(JmolConstants.MOUSE_NONE);
   * 
   * before setting viewer=null
   * 
   * in order to remove references to display window in listeners and
   * hoverWatcher
   * 
   * This is the main access point for creating an application or applet viewer.
   * 
   * @param display
   *          either DisplayPanel or WrappedApplet
   * @param modelAdapter
   *          the model reader
   * @param fullName
   *          or null
   * @param documentBase
   *          or null
   * @param codeBase
   *          or null
   * @param commandOptions
   *          or null
   * @param statusListener
   *          or null
   * @return a viewer instance
   */

  public static JmolViewer allocateViewer(Component display,
                                          JmolAdapter modelAdapter,
                                          String fullName, URL documentBase,
                                          URL codeBase, String commandOptions,
                                          JmolStatusListener statusListener) {
    JmolViewer viewer = new Viewer(display, modelAdapter);
    viewer.setAppletContext(fullName, documentBase, codeBase, commandOptions);
    viewer.setJmolStatusListener(statusListener);
    return viewer;
  }

  /**
   * deprecated because setAppletContext needs to be invoked as well.
   * 
   * @deprecated
   * @param display
   * @param modelAdapter
   * @return a viewer instance
   */
  public static JmolViewer allocateViewer(Component display,
                                          JmolAdapter modelAdapter) {
    return new Viewer(display, modelAdapter);
  }

  private boolean isSilent = false;
  private boolean isApplet = false;

  public boolean isApplet() {
    return isApplet;
  }

  public boolean isPreviewOnly = false;

  boolean isPreviewOnly() {
    return isPreviewOnly;
  }

  boolean autoExit = false;
  boolean haveDisplay = true;
  private boolean isPrintOnly = false;
  private boolean mustRender = true;
  private boolean isCmdLine_c_or_C_Option = false;
  private boolean listCommands = false;
  private boolean isCmdLine_C_Option = true;
  private boolean useCommandThread = false;
  private boolean isSignedApplet = false;
  private boolean isDataOnly;

  public void setAppletContext(String fullName, URL documentBase, URL codeBase,
                               String commandOptions) {
    this.fullName = fullName = (fullName == null ? "" : fullName);
    appletDocumentBase = (documentBase == null ? "" : documentBase.toString());
    appletCodeBase = (codeBase == null ? "" : codeBase.toString());
    int i = fullName.lastIndexOf("[");
    htmlName = (i < 0 ? fullName : fullName.substring(0, i));
    syncId = (i < 0 ? "" : fullName.substring(i + 1, fullName.length() - 1));
    if (commandOptions == null)
      commandOptions = "";
    String str = "" + commandOptions;
    isPrintOnly = (commandOptions.indexOf("-p") >= 0);
    isApplet = (commandOptions.indexOf("-applet") >= 0);
    if (isApplet) {
      Logger.info("applet context: " + commandOptions);
      String appletProxy = null;
      // -appletProxy must be the last flag added
      if ((i = str.indexOf("-appletProxy ")) >= 0) {
        appletProxy = str.substring(i + 13);
        str = str.substring(0, i);
      }
      fileManager.setAppletContext(documentBase, codeBase, appletProxy);
      isSignedApplet = (str.indexOf("-signed") >= 0);
      if ((i = str.indexOf("-maximumSize ")) >= 0)
        setMaximumSize(Parser.parseInt(str.substring(i + 13)));
      useCommandThread = (str.indexOf("-threaded") >= 0);
      if (useCommandThread)
        scriptManager.startCommandWatcher(true);
    } else {
      // not an applet -- used to pass along command line options
      g3d.setBackgroundTransparent(str.indexOf("-b") >= 0);
      isSilent = (str.indexOf("-i") >= 0);
      if (isSilent)
        Logger.setLogLevel(Logger.LEVEL_WARN); // no info, but warnings and
                                               // errors
      isCmdLine_c_or_C_Option = (str.toLowerCase().indexOf("-c") >= 0);
      isCmdLine_C_Option = (str.indexOf("-C") >= 0);
      listCommands = (str.indexOf("-l") >= 0);
      autoExit = (str.indexOf("-x") >= 0);
      isDataOnly = (display == null);
      haveDisplay = (display != null && str.indexOf("-n") < 0);
      if (!haveDisplay)
        display = null;
      mustRender = haveDisplay;
      cd(".");
    }
    isPreviewOnly = (str.indexOf("#previewOnly") >= 0);
    setBooleanProperty("_applet", isApplet);
    setBooleanProperty("_signedApplet", isSignedApplet);
    setBooleanProperty("_useCommandThread", useCommandThread);

    /*
     * Logger.info("jvm11orGreater=" + jvm11orGreater + "\njvm12orGreater=" +
     * jvm12orGreater + "\njvm14orGreater=" + jvm14orGreater);
     */
    if (!isSilent) {
      Logger.info(JmolConstants.copyright
          + "\nJmol Version "
          + getJmolVersion()
          + "\njava.vendor:"
          + strJavaVendor
          + "\njava.version:"
          + strJavaVersion
          + "\nos.name:"
          + strOSName
          + "\nmemory:"
          + getParameter("_memory")
          + "\nuseCommandThread: "
          + useCommandThread
          + (!isApplet ? "" : "\nappletId:" + htmlName
              + (isSignedApplet ? " (signed)" : "")));
    }

    zap(false, false); // here to allow echos
    global.setParameterValue("language", GT.getLanguage());
  }

  public boolean isDataOnly() {
    return isDataOnly;
  }
  
  public static String getJmolVersion() {
    return JmolConstants.version + "  " + JmolConstants.date;
  }

  public String getExportDriverList() {
    return (String) global.getParameter("exportDrivers");
  }

  private static int getJmolVersionInt() {
    // 11.9.999 --> 1109999
    String s = JmolConstants.version;
    int version = -1;

    try {
      // Major number
      int i = s.indexOf(".");
      if (i < 0) {
        version = 100000 * Integer.parseInt(s);
        return version;
      }
      version = 100000 * Integer.parseInt(s.substring(0, i));

      // Minor number
      s = s.substring(i + 1);
      i = s.indexOf(".");
      if (i < 0) {
        version += 1000 * Integer.parseInt(s);
        return version;
      }
      version += 1000 * Integer.parseInt(s.substring(0, i));

      // Revision number
      s = s.substring(i + 1);
      i = s.indexOf("_");
      if (i >= 0)
        s = s.substring(0, i);
      i = s.indexOf(" ");
      if (i >= 0)
        s = s.substring(0, i);
      version += Integer.parseInt(s);
    } catch (NumberFormatException e) {
      // We simply keep the version currently found
    }

    return version;
  }

  String getHtmlName() {
    return htmlName;
  }

  boolean mustRenderFlag() {
    return mustRender && refreshing;
  }

  static int getLogLevel() {
    for (int i = 0; i < Logger.LEVEL_MAX; i++)
      if (Logger.isActiveLevel(i))
        return Logger.LEVEL_MAX - i;
    return 0;
  }

  public Component getDisplay() {
    return display;
  }

  public boolean handleOldJvm10Event(Event e) {
    return mouseManager.handleOldJvm10Event(e);
  }

  void reset() {
    // Eval.reset()
    // initializeModel
    modelSet.calcBoundBoxDimensions(null);
    axesAreTainted = true;
    transformManager.homePosition();
    if (modelSet.setCrystallographicDefaults())
      stateManager.setCrystallographicDefaults();
    else
      setAxesModeMolecular(false);
    prevFrame = Integer.MIN_VALUE;
    refresh(1, "Viewer:homePosition()");
  }

  public void homePosition() {
    evalString("reset");
  }

  /*
   * final Hashtable imageCache = new Hashtable();
   * 
   * void flushCachedImages() { imageCache.clear();
   * colorManager.flushCachedColors(); }
   */

  Hashtable getAppletInfo() {
    Hashtable info = new Hashtable();
    info.put("htmlName", htmlName);
    info.put("syncId", syncId);
    info.put("fullName", fullName);
    if (isApplet) {
      info.put("documentBase", appletDocumentBase);
      info.put("codeBase", appletCodeBase);
      info.put("registry", statusManager.getRegistryInfo());
    }
    info.put("version", JmolConstants.version);
    info.put("date", JmolConstants.date);
    info.put("javaVendor", strJavaVendor);
    info.put("javaVersion", strJavaVersion);
    info.put("operatingSystem", strOSName);
    return info;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to StateManager
  // ///////////////////////////////////////////////////////////////

  void initialize() {
    global = stateManager.getGlobalSettings(global);
    setIntProperty("_version", getJmolVersionInt(), true);
    setBooleanProperty("_applet", isApplet);
    setBooleanProperty("_signedApplet", isSignedApplet);
    setBooleanProperty("_useCommandThread", useCommandThread);
    colorManager.resetElementColors();
    setObjectColor("background", "black");
    setObjectColor("axis1", "red");
    setObjectColor("axis2", "green");
    setObjectColor("axis3", "blue");

    // transfer default global settings to managers and g3d

    setAmbientPercent(global.ambientPercent);
    setDiffusePercent(global.diffusePercent);
    setSpecular(global.specular);
    setSpecularPercent(global.specularPercent);
    setSpecularExponent(global.specularExponent);
    setSpecularPower(global.specularPower);

    animationManager.setAnimationFps(global.animationFps);

    statusManager.setAllowStatusReporting(global.statusReporting);

    setTransformManagerDefaults();

  }

  String listSavedStates() {
    return stateManager.listSavedStates();
  }

  public void saveOrientation(String saveName) {
    // from Eval
    stateManager.saveOrientation(saveName);
  }

  public boolean restoreOrientation(String saveName, float timeSeconds) {
    // from Eval
    return stateManager.restoreOrientation(saveName, timeSeconds, true);
  }

  public void restoreRotation(String saveName, float timeSeconds) {
    stateManager.restoreOrientation(saveName, timeSeconds, false);
  }

  void saveModelOrientation() {
    modelSet.saveModelOrientation(animationManager.currentModelIndex,
        stateManager.getOrientation());
  }

  public Orientation getOrientation() {
    return stateManager.getOrientation();
  }
  
  void restoreModelOrientation(int modelIndex) {
    StateManager.Orientation o = modelSet.getModelOrientation(modelIndex);
    if (o != null)
      o.restore(-1, true);
  }

  void restoreModelRotation(int modelIndex) {
    StateManager.Orientation o = modelSet.getModelOrientation(modelIndex);
    if (o != null)
      o.restore(-1, false);
  }

  void saveBonds(String saveName) {
    // from Eval
    stateManager.saveBonds(saveName);
  }

  boolean restoreBonds(String saveName) {
    // from Eval
    clearModelDependentObjects();
    return stateManager.restoreBonds(saveName);
  }

  void saveState(String saveName) {
    // from Eval
    stateManager.saveState(saveName);
  }

  public String getSavedState(String saveName) {
    return stateManager.getSavedState(saveName);
  }

  void saveStructure(String saveName) {
    // from Eval
    stateManager.saveStructure(saveName);
  }

  String getSavedStructure(String saveName) {
    return stateManager.getSavedStructure(saveName);
  }

  public void saveCoordinates(String saveName, BitSet bsSelected) {
    // from Eval
    stateManager.saveCoordinates(saveName, bsSelected);
  }

  String getSavedCoordinates(String saveName) {
    return stateManager.getSavedCoordinates(saveName);
  }

  void saveSelection(String saveName) {
    // from Eval
    stateManager.saveSelection(saveName, selectionManager.bsSelection);
    stateManager.restoreSelection(saveName); // just to register the # of
                                             // selected atoms
  }

  boolean restoreSelection(String saveName) {
    // from Eval
    return stateManager.restoreSelection(saveName);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to TransformManager
  // ///////////////////////////////////////////////////////////////

  public Matrix4f getMatrixtransform() {
    return transformManager.getMatrixtransform();
  }

  Quaternion getRotationQuaternion() {
    return transformManager.getRotationQuaternion();
  }
  
  public float getRotationRadius() {
    return transformManager.getRotationRadius();
  }

  void setRotationRadius(float angstroms, boolean doAll) {
    if (doAll)
      angstroms = transformManager.setRotationRadius(angstroms, false);
    if (!modelSet
        .setRotationRadius(animationManager.currentModelIndex, angstroms))
      global.setParameterValue("rotationRadius", angstroms);
  }

  public Point3f getRotationCenter() {
    return transformManager.getRotationCenter();
  }

  void setCenterAt(String relativeTo, Point3f pt) {
    // Eval centerAt boundbox|absolute|average {pt}
    if (isJmolDataFrame())
      return;
    transformManager.setCenterAt(relativeTo, pt);
  }

  void setCenterBitSet(BitSet bsCenter, boolean doScale) {
    // Eval
    // setCenterSelected

    Point3f center = (bsCenter != null
        && BitSetUtil.cardinalityOf(bsCenter) > 0 ? getAtomSetCenter(bsCenter)
        : null);
    if (isJmolDataFrame())
      return;
    transformManager.setNewRotationCenter(center, doScale);
  }

  void setNewRotationCenter(Point3f center) {
    // eval CENTER command
    if (isJmolDataFrame())
      return;
    transformManager.setNewRotationCenter(center, true);
  }

  public Point3f getNavigationCenter() {
    return transformManager.getNavigationCenter();
  }

  public float getNavigationDepthPercent() {
    return transformManager.getNavigationDepthPercent();
  }

  void navigate(int keyWhere, int modifiers) {
    if (isJmolDataFrame())
      return;
    transformManager.navigate(keyWhere, modifiers);
    if (!transformManager.vibrationOn)
      refresh(1, "Viewer:navigate()");
  }

  public Point3f getNavigationOffset() {
    return transformManager.getNavigationOffset();
  }

  float getNavigationOffsetPercent(char XorY) {
    return transformManager.getNavigationOffsetPercent(XorY);
  }

  public boolean isNavigating() {
    return transformManager.isNavigating();
  }

  boolean isInPosition(Vector3f axis, float degrees) {
    return transformManager.isInPosition(axis, degrees);
  }

  void move(Vector3f dRot, float dZoom, Vector3f dTrans, float dSlab,
            float floatSecondsTotal, int fps) {
    // from Eval
    transformManager.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
    moveUpdate(floatSecondsTotal);
  }

  void moveTo(float floatSecondsTotal, Point3f center, Vector3f pt,
              float degrees, float zoom, float xTrans, float yTrans,
              float rotationRadius, Point3f navCenter, float xNav, float yNav,
              float navDepth) {
    // from Eval
    transformManager.moveTo(floatSecondsTotal, center, pt, degrees, zoom,
        xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
    moveUpdate(floatSecondsTotal);
  }

  void moveTo(float floatSecondsTotal, Point3f center, Matrix3f rotationMatrix, 
              float zoom, float xTrans, float yTrans, float rotationRadius,
              Point3f navCenter, float xNav, float yNav, float navDepth) {
    // from StateManager -- -1 for time --> no repaint
    transformManager.moveTo(floatSecondsTotal, center, rotationMatrix, zoom,
        xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
    moveUpdate(floatSecondsTotal);
  }

  private void moveUpdate(float floatSecondsTotal) {
    if (floatSecondsTotal > 0)
      requestRepaintAndWait();
    else if (floatSecondsTotal == 0)
      setSync();
  }

  String getMoveToText(float timespan) {
    return transformManager.getMoveToText(timespan, false);
  }

  void navigate(float timeSeconds, Point3f[] path, float[] theta,
                int indexStart, int indexEnd) {
    if (isJmolDataFrame())
      return;
    transformManager.navigate(timeSeconds, path, theta, indexStart, indexEnd);
    moveUpdate(timeSeconds);
  }

  public void navigate(float timeSeconds, Point3f center) {
    if (isJmolDataFrame())
      return;
    transformManager.navigate(timeSeconds, center);
    moveUpdate(timeSeconds);
  }

  void navigate(float timeSeconds, Point3f[][] pathGuide) {
    if (isJmolDataFrame())
      return;
    transformManager.navigate(timeSeconds, pathGuide);
    moveUpdate(timeSeconds);
  }

  void navigateSurface(float timeSeconds, String name) {
    if (isJmolDataFrame())
      return;
    transformManager.navigateSurface(timeSeconds, name);
    moveUpdate(timeSeconds);
  }

  public void navigate(float timeSeconds, Vector3f rotAxis, float degrees) {
    if (isJmolDataFrame())
      return;
    transformManager.navigate(timeSeconds, rotAxis, degrees);
    moveUpdate(timeSeconds);
  }

  void navTranslate(float timeSeconds, Point3f center) {
    if (isJmolDataFrame())
      return;
    transformManager.navTranslate(timeSeconds, center);
    moveUpdate(timeSeconds);
  }

  void navTranslatePercent(float timeSeconds, float x, float y) {
    if (isJmolDataFrame())
      return;
    transformManager.navTranslatePercent(timeSeconds, x, y);
    moveUpdate(timeSeconds);
  }

  private boolean mouseEnabled = true;
  public void setMouseEnabled(boolean TF) {
    mouseEnabled = TF;
  }
  
  void zoomBy(int pixels) {
    // MouseManager.mouseSinglePressDrag
    if (mouseEnabled)
      transformManager.zoomBy(pixels);
    refresh(2, statusManager.syncingMouse ? "Mouse: zoomBy " + pixels : "");
  }

  void zoomByFactor(float factor) {
    // MouseManager.mouseWheel
    if (mouseEnabled)
      transformManager.zoomByFactor(factor);
    refresh(2, statusManager.syncingMouse ? "Mouse: zoomByFactor " + factor
        : "");
  }

  void rotateXYBy(int xDelta, int yDelta) {
    // mouseSinglePressDrag
    if (mouseEnabled)
      transformManager.rotateXYBy(xDelta, yDelta, null);
    refresh(2, statusManager.syncingMouse ? "Mouse: rotateXYBy " + xDelta + " "
        + yDelta : "");
  }

  void rotateZBy(int zDelta) {
    // mouseSinglePressDrag
    if (mouseEnabled)
      transformManager.rotateZBy(zDelta);
    refresh(2, statusManager.syncingMouse ? "Mouse: rotateZBy " + zDelta : "");
  }

  void rotateMolecule(int deltaX, int deltaY) {
    if (isJmolDataFrame())
      return;
    if (mouseEnabled) {
      transformManager.setRotateMolecule(true);
      transformManager.rotateXYBy(deltaX, deltaY, selectionManager.bsSelection);
      transformManager.setRotateMolecule(false);
      refreshMeasures();
    }
    refresh(2, statusManager.syncingMouse ? "Mouse: rotateMolecule " + deltaX
        + " " + deltaY : "");
  }

  void translateXYBy(int xDelta, int yDelta) {
    // mouseDoublePressDrag, mouseSinglePressDrag
    if (mouseEnabled)
      transformManager.translateXYBy(xDelta, yDelta);
    refresh(2, statusManager.syncingMouse ? "Mouse: translateXYBy " + xDelta
        + " " + yDelta : "");
  }

  public void rotateFront() {
    // deprecated
    transformManager.rotateFront();
    refresh(1, "Viewer:rotateFront()");
  }

  public void rotateX(float angleRadians) {
    // deprecated
    transformManager.rotateX(angleRadians);
    refresh(1, "Viewer:rotateX()");
  }

  public void rotateY(float angleRadians) {
    // deprecated
    transformManager.rotateY(angleRadians);
    refresh(1, "Viewer:rotateY()");
  }

  public void rotateZ(float angleRadians) {
    // deprecated
    transformManager.rotateZ(angleRadians);
    refresh(1, "Viewer:rotateZ()");
  }

  public void rotateX(int angleDegrees) {
    // deprecated
    rotateX(angleDegrees * Measure.radiansPerDegree);
  }

  public void rotateY(int angleDegrees) {
    // deprecated
    rotateY(angleDegrees * Measure.radiansPerDegree);
  }

  void translate(char xyz, float x, char type) {
    int xy = (type == '\0' ? 0 : type == '%' ? transformManager
        .percentToPixels(xyz, x) : transformManager.angstromsToPixels(x
        * (type == 'n' ? 10f : 1f)));
    switch (xyz) {
    case 'x':
      if (type == '\0')
        transformManager.translateToXPercent(x);
      else
        transformManager.translateXYBy(xy, 0);
      break;
    case 'y':
      if (type == '\0')
        transformManager.translateToYPercent(x);
      else
        transformManager.translateXYBy(0, xy);
      break;
    case 'z':
      if (type == '\0')
        transformManager.translateToZPercent(x);
      else
        transformManager.translateZBy(xy);
      break;
    }
    refresh(1, "Viewer:translate()");
  }

  public float getTranslationXPercent() {
    return transformManager.getTranslationXPercent();
  }

  public float getTranslationYPercent() {
    return transformManager.getTranslationYPercent();
  }

  float getTranslationZPercent() {
    return transformManager.getTranslationZPercent();
  }

  public String getTranslationScript() {
    return transformManager.getTranslationScript();
  }

  public int getZoomPercent() {
    // deprecated
    return (int) getZoomSetting();
  }

  float getZoomSetting() {
    return transformManager.getZoomSetting();
  }

  public float getZoomPercentFloat() {
    // note -- this value is only after rendering.
    return transformManager.getZoomPercentFloat();
  }

  float getMaxZoomPercent() {
    return TransformManager.MAXIMUM_ZOOM_PERCENTAGE;
  }

  void slabReset() {
    transformManager.slabReset();
  }

  boolean getZoomEnabled() {
    return transformManager.zoomEnabled;
  }

  public boolean getSlabEnabled() {
    return transformManager.slabEnabled;
  }

  void slabByPixels(int pixels) {
    // MouseManager.mouseSinglePressDrag
    transformManager.slabByPercentagePoints(pixels);
  }

  void depthByPixels(int pixels) {
    // MouseManager.mouseDoublePressDrag
    transformManager.depthByPercentagePoints(pixels);
  }

  void slabDepthByPixels(int pixels) {
    // MouseManager.mouseSinglePressDrag
    transformManager.slabDepthByPercentagePoints(pixels);
  }

  void slabToPercent(int percentSlab) {
    // Eval.slab
    transformManager.slabToPercent(percentSlab);
  }

  void slabInternal(Point4f plane, boolean isDepth) {
    transformManager.slabInternal(plane, isDepth);
  }

  void depthToPercent(int percentDepth) {
    // Eval.depth
    transformManager.depthToPercent(percentDepth);
  }

  void setSlabDepthInternal(boolean isDepth) {
    transformManager.setSlabDepthInternal(isDepth);
  }

  public int zValueFromPercent(int zPercent) {
    return transformManager.zValueFromPercent(zPercent);
  }

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  void finalizeTransformParameters() {
    // FrameRenderer
    // InitializeModel

    transformManager.finalizeTransformParameters();
    g3d.setSlabAndDepthValues(transformManager.slabValue,
        transformManager.depthValue, global.zShade);
  }

  public void rotatePoint(Point3f pt, Point3f ptRot) {
    transformManager.rotatePoint(pt, ptRot);
  }

  public Point3i transformPoint(Point3f pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  public Point3i transformPoint(Point3f pointAngstroms, Vector3f vibrationVector) {
    return transformManager.transformPoint(pointAngstroms, vibrationVector);
  }

  public void transformPoint(Point3f pointAngstroms, Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public void transformPointNoClip(Point3f pointAngstroms, Point3f pt) {
    transformManager.transformPointNoClip(pointAngstroms, pt);
  }

  public void transformPoint(Point3f pointAngstroms, Point3f pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public void transformPoints(Point3f[] pointsAngstroms, Point3i[] pointsScreens) {
    transformManager.transformPoints(pointsAngstroms.length, pointsAngstroms,
        pointsScreens);
  }

  public void transformVector(Vector3f vectorAngstroms,
                              Vector3f vectorTransformed) {
    transformManager.transformVector(vectorAngstroms, vectorTransformed);
  }

  public void unTransformPoint(Point3f pointScreen, Point3f pointAngstroms) {
    // called by Draw.move2D
    transformManager.unTransformPoint(pointScreen, pointAngstroms);
  }

  public float getScalePixelsPerAngstrom(boolean asAntialiased) {
    return transformManager.scalePixelsPerAngstrom
        * (asAntialiased || !global.antialiasDisplay ? 1f : 0.5f);
  }

  public short scaleToScreen(int z, int milliAngstroms) {
    // all shapes
    return transformManager.scaleToScreen(z, milliAngstroms);
  }

  public float unscaleToScreen(int z, int screenDistance) {
    // all shapes
    return transformManager.unscaleToScreen(z, screenDistance);
  }

  public float scaleToPerspective(int z, float sizeAngstroms) {
    // DotsRenderer
    return transformManager.scaleToPerspective(z, sizeAngstroms);
  }

  void setSpin(String key, int value) {
    // Eval
    if (!Parser.isOneOf(key, "x;y;z;fps;X;Y;Z;FPS"))
      return;
    int i = "x;y;z;fps;X;Y;Z;FPS".indexOf(key);
    switch (i) {
    case 0:
      transformManager.setSpinXYZ(value, Float.NaN, Float.NaN);
      break;
    case 2:
      transformManager.setSpinXYZ(Float.NaN, value, Float.NaN);
      break;
    case 4:
      transformManager.setSpinXYZ(Float.NaN, Float.NaN, value);
      break;
    case 6:
    default:
      transformManager.setSpinFps(value);
      break;
    case 10:
      transformManager.setNavXYZ(value, Float.NaN, Float.NaN);
      break;
    case 12:
      transformManager.setNavXYZ(Float.NaN, value, Float.NaN);
      break;
    case 14:
      transformManager.setNavXYZ(Float.NaN, Float.NaN, value);
      break;
    case 16:
      transformManager.setNavFps(value);
      break;
    }
    global.setParameterValue((i < 10 ? "spin" : "nav") + key, value);
  }

  String getSpinState() {
    return transformManager.getSpinState(false);
  }

  void setSpinOn(boolean spinOn) {
    // Eval
    // startSpinningAxis
    transformManager.setSpinOn(spinOn);
  }

  boolean getSpinOn() {
    return transformManager.getSpinOn();
  }

  void setNavOn(boolean navOn) {
    // Eval
    // startSpinningAxis
    transformManager.setNavOn(navOn);
  }

  boolean getNavOn() {
    return transformManager.getNavOn();
  }

  void setNavXYZ(float x, float y, float z) {
    transformManager.setNavXYZ((int)x, (int)y, (int)z);  
  }
  
  public String getOrientationText(int type) {
    return transformManager.getOrientationText(type);
  }

  Hashtable getOrientationInfo() {
    return transformManager.getOrientationInfo();
  }

  Matrix3f getMatrixRotate() {
    return transformManager.getMatrixRotate();
  }

  public void getAxisAngle(AxisAngle4f axisAngle) {
    transformManager.getAxisAngle(axisAngle);
  }

  String getTransformText() {
    return transformManager.getTransformText();
  }

  void getRotation(Matrix3f matrixRotation) {
    transformManager.getRotation(matrixRotation);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ColorManager
  // ///////////////////////////////////////////////////////////////

  private void setDefaultColors(String colorScheme) {
    colorManager.setDefaultColors(colorScheme);
    global.setParameterValue("colorRasmol", (colorScheme.equals("rasmol")));
  }

  float getDefaultTranslucent() {
    return global.defaultTranslucent;
  }

  public int getColixArgb(short colix) {
    return g3d.getColixArgb(colix);
  }

  void setRubberbandArgb(int argb) {
    // Eval
    colorManager.setRubberbandArgb(argb);
  }

  public short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  void setElementArgb(int elementNumber, int argb) {
    // Eval
    global.setParameterValue("=color "
        + JmolConstants.elementNameFromNumber(elementNumber), Escape
        .escapeColor(argb));
    colorManager.setElementArgb(elementNumber, argb);
  }

  public float getVectorScale() {
    return global.vectorScale;
  }

  public void setVectorScale(float scale) {
    global.setParameterValue("vectorScale", scale);
    global.vectorScale = scale;
  }

  public float getDefaultDrawArrowScale() {
    return global.defaultDrawArrowScale;
  }

  public void setDefaultDrawArrowScale(float scale) {
    global.setParameterValue("defaultDrawArrowScale", scale);
    global.defaultDrawArrowScale = scale;
  }

  float getVibrationScale() {
    return global.vibrationScale;
  }

  float getVibrationPeriod() {
    return global.vibrationPeriod;
  }

  public boolean isVibrationOn() {
    return transformManager.vibrationOn;
  }

  public void setVibrationScale(float scale) {
    // Eval

    transformManager.setVibrationScale(scale);
    global.vibrationScale = scale;
    // because this is public:
    global.setParameterValue("vibrationScale", scale);
  }

  void setVibrationOff() {
    transformManager.setVibrationPeriod(0);
  }

  public void setVibrationPeriod(float period) {
    // Eval
    transformManager.setVibrationPeriod(period);
    period = Math.abs(period);
    global.vibrationPeriod = period;
    // because this is public:
    global.setParameterValue("vibrationPeriod", period);
  }

  void setObjectColor(String name, String colorName) {
    if (colorName == null || colorName.length() == 0)
      return;
    setObjectArgb(name, Graphics3D.getArgbFromString(colorName));
  }

  void setObjectArgb(String name, int argb) {
    int objId = StateManager.getObjectIdFromName(name);
    if (objId < 0)
      return;
    global.objColors[objId] = argb;
    switch (objId) {
    case StateManager.OBJ_BACKGROUND:
      g3d.setBackgroundArgb(argb);
      colorManager.setColixBackgroundContrast(argb);
      global.backgroundImageFileName = null;
      break;
    }
    global.setParameterValue(name + "Color", Escape.escapeColor(argb));
  }

  void setBackgroundImage(String fileName, Image image) {
    global.backgroundImageFileName = fileName;
    g3d.setBackgroundImage(image);
  }

  int getObjectArgb(int objId) {
    return global.objColors[objId];
  }

  public short getObjectColix(int objId) {
    int argb = getObjectArgb(objId);
    if (argb == 0)
      return getColixBackgroundContrast();
    return Graphics3D.getColix(argb);
  }

  public String getObjectState(String name) {
    int objId = StateManager
        .getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
    if (objId < 0)
      return "";
    int mad = getObjectMad(objId);
    StringBuffer s = new StringBuffer("\n");
    Shape.appendCmd(s, name
        + (mad == 0 ? " off" : mad == 1 ? " on" : mad == -1 ? " dotted"
            : mad < 20 ? " " + mad : " " + (mad / 2000f)));
    return s.toString();
  }

  // for historical reasons, leave these two:

  public void setColorBackground(String colorName) {
    setObjectColor("background", colorName);
  }

  public int getBackgroundArgb() {
    return getObjectArgb(StateManager.OBJ_BACKGROUND);
  }

  void setObjectMad(int iShape, String name, int mad) {
    int objId = StateManager
        .getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
    if (objId < 0)
      return;
    if (mad == -2 || mad == -4) { // turn on if not set "showAxes = true"
      int m = mad + 3;
      mad = getObjectMad(objId);
      if (mad == 0)
        mad = m;
    }
    global.setParameterValue("show" + name, mad != 0);
    global.objStateOn[objId] = (mad != 0);
    if (mad == 0)
      return;
    global.objMad[objId] = mad;
    setShapeSize(iShape, mad, Float.NaN); // just loads it
  }

  public int getObjectMad(int objId) {
    return (global.objStateOn[objId] ? global.objMad[objId] : 0);
  }

  public void setPropertyColorScheme(String scheme, boolean isOverloaded) {
    global.propertyColorScheme = scheme;
    colorManager.setColorScheme(scheme, isOverloaded);
  }

  public String getPropertyColorScheme() {
    return global.propertyColorScheme;
  }

  public short getColixBackgroundContrast() {
    return colorManager.colixBackgroundContrast;
  }

  String getSpecularState() {
    return global.getSpecularState();
  }

  private static void setSpecular(boolean specular) {
    // Eval
    ColorManager.setSpecular(specular);
  }

  boolean getSpecular() {
    return ColorManager.getSpecular();
  }

  private static void setSpecularPower(int specularPower) {
    // Eval
    ColorManager.setSpecularPower(Math.abs(specularPower));
  }

  private static void setSpecularExponent(int specularExponent) {
    // Eval
    ColorManager.setSpecularPower(-Math.abs(specularExponent));
  }

  private static void setAmbientPercent(int ambientPercent) {
    // Eval
    ColorManager.setAmbientPercent(ambientPercent);
  }

  static int getAmbientPercent() {
    return ColorManager.getAmbientPercent();
  }

  private static void setDiffusePercent(int diffusePercent) {
    // Eval
    ColorManager.setDiffusePercent(diffusePercent);
  }

  static int getDiffusePercent() {
    return ColorManager.getDiffusePercent();
  }

  private static void setSpecularPercent(int specularPercent) {
    // Eval
    ColorManager.setSpecularPercent(specularPercent);
  }

  static int getSpecularPercent() {
    return ColorManager.getSpecularPercent();
  }

  boolean getZShade() {
    return global.zShade;
  }

  public short getColixAtomPalette(Atom atom, byte pid) {
    return colorManager.getColixAtomPalette(atom, pid);
  }

  public short getColixBondPalette(Bond bond, byte pid) {
    return colorManager.getColixBondPalette(bond, pid);
  }

  public int[] getColorSchemeArray(String colorScheme) {
    return colorManager.getColorSchemeArray(colorScheme);
  }

  public String getColorSchemeList(String colorScheme, boolean ifDefault) {
    return colorManager.getColorSchemeList(colorScheme, ifDefault);
  }

  public static void setUserScale(int[] scale) {
    ColorManager.setUserScale(scale);
  }

  public short getColixForPropertyValue(float val) {
    // isosurface
    return colorManager.getColixForPropertyValue(val);
  }

  Point3f getColorPointForPropertyValue(float val) {
    // x = {atomno=3}.partialcharge.color
    short colix = colorManager.getColixForPropertyValue(val);
    Point3f pt = new Point3f();
    return Graphics3D.colorPointFromInt(g3d.getColixArgb(colix), pt);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  // ///////////////////////////////////////////////////////////////

  void select(BitSet bs, boolean isQuiet) {
    // Eval
    selectionManager.select(bs, isQuiet);
    modelSet.setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MAX_VALUE,
        Float.NaN, null);
  }

  void selectBonds(BitSet bs) {
    modelSet.setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MAX_VALUE,
        Float.NaN, bs);
  }

  void hide(BitSet bs, boolean isQuiet) {
    // Eval
    selectionManager.hide(bs, isQuiet);
  }

  void display(BitSet bs, boolean isQuiet) {
    // Eval
    selectionManager.display(getModelAtomBitSet(-1, false), bs, isQuiet);
  }

  BitSet getHiddenSet() {
    return selectionManager.getHiddenSet();
  }

  public boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  boolean isInSelectionSubset(int atomIndex) {
    return selectionManager.isInSelectionSubset(atomIndex);
  }

  void reportSelection(String msg) {
    if (modelSet.getSelectionHaloEnabled())
      setTainted(true);
    if (isScriptQueued || global.debugScript)
      scriptStatus(msg);
  }

  public Point3f getAtomSetCenter(BitSet bs) {
    return modelSet.getAtomSetCenter(bs);
  }

  private void clearAtomSets() {
    setSelectionSubset(null);
    definedAtomSets.clear();
  }

  public void selectAll() {
    // initializeModel
    selectionManager.selectAll(false);
  }

  private boolean noneSelected;

  void setNoneSelected(boolean noneSelected) {
    this.noneSelected = noneSelected;
  }

  Boolean getNoneSelected() {
    return (noneSelected ? Boolean.TRUE : Boolean.FALSE);
  }

  public void clearSelection() {
    // not used in this project; in jmolViewer interface, though
    selectionManager.clearSelection(false);
    global.setParameterValue("hideNotSelected", false);
  }

  public void setSelectionSet(BitSet set) {
    // not used in this project; in jmolViewer interface, though
    selectionManager.setSelectionSet(set);
  }

  void setSelectionSubset(BitSet subset) {
    selectionManager.setSelectionSubset(subset);
  }

  public BitSet getSelectionSubset() {
    return selectionManager.bsSubset;
  }

  void invertSelection() {
    // Eval
    selectionManager.invertSelection();
    // only used from a script, so I do not think a refresh() is necessary
  }

  public BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }

  public int getSelectionCount() {
    return selectionManager.getSelectionCount();
  }

  void setFormalCharges(int formalCharge) {
    modelSet.setFormalCharges(selectionManager.bsSelection, formalCharge);
  }

  public void addSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  public void removeSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  BitSet getAtomBitSet(Object atomExpression) {
    // typically a string such as "(atomno < 3)"
    return ScriptEvaluator.getAtomBitSet(eval, atomExpression);
  }

  Vector getAtomBitSetVector(Object atomExpression) {
    return ScriptEvaluator.getAtomBitSetVector(eval, getAtomCount(), atomExpression);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to MouseManager
  // ///////////////////////////////////////////////////////////////

  public void setModeMouse(int modeMouse) {
    // call before setting viewer=null
    if (haveDisplay)
      mouseManager.setModeMouse(modeMouse);
    if (modeMouse == JmolConstants.MOUSE_NONE) {
      // applet is being destroyed
      clearScriptQueue();
      haltScriptExecution();
      transformManager.setSpinOn(false);
      transformManager.setNavOn(false);
      transformManager.setVibrationPeriod(0);
      scriptManager.startCommandWatcher(false);
      scriptManager.interruptQueueThreads();
      g3d.destroy();
      if (appConsole != null) {
        appConsole.dispose();
        appConsole = null;
      }
      if (scriptEditor != null) {
        scriptEditor.dispose();
        scriptEditor = null;
      }
    }
  }

  Rectangle getRubberBandSelection() {
    return mouseManager.getRubberBand();
  }

  public int getCursorX() {
    return mouseManager.xCurrent;
  }

  public int getCursorY() {
    return mouseManager.yCurrent;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to FileManager
  // ///////////////////////////////////////////////////////////////

  String getDefaultDirectory() {
    return global.defaultDirectory;
  }

  public BufferedInputStream getBufferedInputStream(String fullPathName) {
    Object ret = getBufferedReaderOrErrorMessageFromName(fullPathName,
        new String[2], true);
    return (ret instanceof BufferedInputStream ? (BufferedInputStream) ret
        : null);
  }

  Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary) {
    return fileManager.getBufferedReaderOrErrorMessageFromName(name,
        fullPathNameReturn, isBinary, true);
  }

  void addLoadScript(String script) {
    fileManager.addLoadScript(script);
  }

  private Hashtable setLoadParameters(Hashtable htParams) {
    if (htParams == null)
      htParams = new Hashtable();
    if (global.atomTypes.length() > 0)
      htParams.put("atomTypes", global.atomTypes);
    if (!htParams.containsKey("lattice"))
      htParams.put("lattice", global.getDefaultLattice());
    if (global.applySymmetryToBonds)
      htParams.put("applySymmetryToBonds", Boolean.TRUE);
    if (getPdbLoadInfo(2))
      htParams.put("getHeader", Boolean.TRUE);
    return htParams;
  }

  /**
   * for JmolSimpleViewer -- external applications only
   * 
   * @param fileName
   * @return null or error
   */
  public String openFile(String fileName) {
    zap(false, false);
    fileManager.createAtomSetCollectionFromFile(fileName, new Hashtable(), null, false);
    return createModelSetAndReturnError(false);
  }

  public void openFileAsynchronously(String fileName) {
    // Jmol app file dropper, main, OpenUrlAction, RecentFilesAction
    boolean allowScript = (!fileName.startsWith("\t"));
    if (!allowScript)
      fileName = fileName.substring(1);
    fileName = fileName.replace('\\', '/');
    String type = fileManager.getFileTypeName(fileName);
    checkHalt("exit");
    // assumes a Jmol script file if no other file type
    allowScript &= (type == null);
    if (scriptEditorVisible && allowScript)
      showEditor(new String[] { fileName, getFileAsString(fileName) });
    else
      evalString((allowScript ? "script " : "load ")
         + Escape.escape(fileName));
  }

  private final static int FILE_STATUS_NOT_LOADED = -1;
  private final static int FILE_STATUS_ZAPPED = 0;
  private final static int FILE_STATUS_CREATING_MODELSET = 2;
  private final static int FILE_STATUS_MODELSET_CREATED = 3;
  private final static int FILE_STATUS_MODELS_DELETED = 5;

  void openFile(String fileName, Hashtable htParams, String loadScript,
                boolean isAppend) {
    // Eval only - does NOT create the model set yet
    // errors encountered will be passed up to Eval.runEval()
    if (fileName == null)
      return;
    if (fileName.equalsIgnoreCase("string[]")) {
      // no reloading of string[] data -- just too complicated
      // openStringsInline(fileManager.getInlineDataArray(), htParams, false);
      return;
    }
    Logger.startTimer();
    if (fileName.equalsIgnoreCase("string")) {
      String fileData = (htParams.containsKey("fileData") 
          ? (String) htParams.get("fileData")
          : fileManager.getInlineData(-1)); 
      createModelSetInline(fileData, htParams, isAppend,
          false);
    } else {
      if (!isAppend && fileName.charAt(0) != '?')
        zap(false, false);
      fileManager.createAtomSetCollectionFromFile(fileName,
          setLoadParameters(htParams), loadScript, isAppend);
    }
    Logger.checkTimer("openFile(" + fileName + ")");
  }

  public String openFiles(String modelName, String[] fileNames) {
    openFiles(modelName, fileNames, null, false, null);
    return createModelSetAndReturnError(false);
  }

  String openFiles(String modelName, String[] fileNames, String loadScript,
                   boolean isAppend, Hashtable htParams) {
    // Eval -- names will be loaded with full path names
    // does not create model set
    if (!isAppend)
      zap(false, false);
    // keep old screen image while new file is being loaded
    // forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.createAtomSetCollectionFromFiles(modelName, fileNames,
        loadScript, isAppend, setLoadParameters(htParams));
    long ms = System.currentTimeMillis() - timeBegin;
    String msg = "";
    for (int i = 0; i < fileNames.length; i++)
      msg += (i == 0 ? "" : ",") + fileNames[i];
    Logger.info("openFiles(" + fileNames.length + ") " + ms + " ms");
    return msg;
  }

  public String openStringInline(String strModel) {
    // Jmol app file dropper
    return createModelSetInline(strModel, null, false, true);
  }

  private String createModelSetInline(String strModel, Hashtable htParams,
                                      boolean isAppend, boolean createModelSet) {
    // loadInline, openFile, openStringInline
    if (!isAppend)
      zap(true, false);
    fileManager.createAtomSetCollectionFromString(strModel,
        setLoadParameters(htParams), isAppend);
    return (createModelSet ? createModelSetAndReturnError(isAppend) : null);
  }

  private String openStringsInline(String[] arrayModels, Hashtable htParams,
                                   boolean isAppend) {
    // loadInline, openFile, openStringInline
    if (!isAppend)
      zap(true, false);
    fileManager.createAtomSeCollectionFromStrings(arrayModels,
        setLoadParameters(htParams), isAppend);
    return createModelSetAndReturnError(isAppend);
  }

  public char getInlineChar() {
    return global.inlineNewlineChar;
  }

  public String loadInline(String strModel) {
    // loadInline PARAMETER for APPLET ONLY
    return loadInline(strModel, global.inlineNewlineChar, false);
  }

  public String loadInline(String strModel, boolean isAppend) {
    // applet Console, loadInline functions, app PasteClipboard
    return loadInline(strModel, (char) 0, isAppend);
  }

  public String loadInline(String strModel, char newLine) {
    return loadInline(strModel, newLine, false);
  }

  String loadInline(String strModel, char newLine, boolean isAppend) {
    if (strModel == null)
      return null;
    int i;
    Hashtable htParams = new Hashtable();
    Logger.debug(strModel);
    String rep = (strModel.indexOf('\n') >= 0 ? "" : "\n");
    if (newLine != 0 && newLine != '\n') {
      int len = strModel.length();
      for (i = 0; i < len && strModel.charAt(i) == ' '; ++i) {
      }
      if (i < len && strModel.charAt(i) == newLine)
        strModel = strModel.substring(i + 1);
      strModel = TextFormat.simpleReplace(strModel, "" + newLine, rep);
    }
    String datasep = getDataSeparator();
    if (datasep != null && datasep != ""
        && (i = strModel.indexOf(datasep)) >= 0) {
      int n = 2;
      while ((i = strModel.indexOf(datasep, i + 1)) >= 0)
        n++;
      String[] strModels = new String[n];
      int pt = 0, pt0 = 0;
      for (i = 0; i < n; i++) {
        pt = strModel.indexOf(datasep, pt0);
        if (pt < 0)
          pt = strModel.length();
        strModels[i] = strModel.substring(pt0, pt);
        pt0 = pt + datasep.length();
      }
      return openStringsInline(strModels, htParams, isAppend);
    }
    return createModelSetInline(strModel, htParams, isAppend, true);
  }

  String getDataSeparator() {
    return (String) global.getParameter("dataseparator");
  }

  public String loadInline(String[] arrayModels) {
    return loadInline(arrayModels, false);
  }

  public String loadInline(String[] arrayModels, boolean isAppend) {
    // Eval data
    // loadInline
    return (arrayModels == null || arrayModels.length == 0 ? null
        : openStringsInline(arrayModels, null, isAppend));
  }

  public boolean getApplySymmetryToBonds() {
    return global.applySymmetryToBonds;
  }

  void setApplySymmetryToBonds(boolean TF) {
    global.applySymmetryToBonds = TF;
  }

  void setAtomData(int type, String name, String coordinateData) {
    modelSet.setAtomData(type, name, coordinateData);
  }

  public String openDOM(Object DOMNode) {
    // applet.loadDOMNode
    zap(true, false);
    long timeBegin = System.currentTimeMillis();
    fileManager.createAtomSetCollectionFromDOM(DOMNode);
    long ms = System.currentTimeMillis() - timeBegin;
    Logger.info("openDOM " + ms + " ms");
    return createModelSetAndReturnError(false);
  }

  /**
   * Opens the file, given the reader.
   * 
   * name is a text name of the file ... to be displayed in the window no need
   * to pass a BufferedReader ... ... the FileManager will wrap a buffer around
   * it
   * 
   * DO NOT USE IN JMOL -- THIS METHOD IS ONLY HERE BECAUSE IT IS PART OF THE
   * LEGACY INTERFACE -- will close the reader
   * 
   * @param fullPathName
   * @param fileName
   * @param reader
   * @return null or error message
   */

  public String openReader(String fullPathName, String fileName, Reader reader) {
    zap(true, false);
    fileManager.createAtomSetCollectionFromReader(fullPathName, fileName,
        reader);
    return createModelSetAndReturnError(false);
  }

  /**
   * 
   * @param isAppend
   * @return errMsg
   */
  String createModelSetAndReturnError(boolean isAppend) {
    String fullPathName = getFullPathName();
    String fileName = getFileName();
    Object atomSetCollection = fileManager.getAtomSetCollectionOrError();
    fileManager.deallocateReaderThreads();
    String errMsg;
    if (atomSetCollection instanceof String || atomSetCollection == null) {
      errMsg = (String) atomSetCollection;
      setFileLoadStatus(FILE_STATUS_NOT_LOADED, fullPathName, null, null,
          errMsg);
      if (errMsg != null && !isAppend && !errMsg.equals("#CANCELED#"))
        zap(errMsg);
    } else {
      if (isAppend)
        clearAtomSets();
      setFileLoadStatus(FILE_STATUS_CREATING_MODELSET, fullPathName, fileName,
          null, null);
      errMsg = createModelSet(fullPathName, fileName, atomSetCollection,
          isAppend);
      setFileLoadStatus(FILE_STATUS_MODELSET_CREATED, fullPathName, fileName,
          getModelSetName(), errMsg);
      if (isAppend) {
        selectAll(); // could be an issue here. Do we really want to
                     // "select all"?
        setTainted(true);
      }
      atomSetCollection = null;
      System.gc();
    }
    return errMsg;
  }

  String loadAtomDataAndReturnError(int tokType) {
    Object atomSetCollection = fileManager.getAtomSetCollectionOrError();
    fileManager.deallocateReaderThreads();
    String errMsg;
    if (atomSetCollection instanceof String || atomSetCollection == null) {
      errMsg = (String) atomSetCollection;
    } else {
      errMsg = createAtomDataSet(tokType, atomSetCollection);
    }
    return errMsg;
  }

  private String createAtomDataSet(int tokType, Object atomSetCollection) {
    // maybe there needs to be a call to clear()
    // or something like that here
    // for when CdkEditBus calls this directly
    // null fullPathName implies we are doing a merge
    setErrorMessage(null);
    try {
      ((ModelLoader) modelSet).createAtomDataSet(tokType, atomSetCollection,
          selectionManager.bsSelection);
      if (tokType == Token.vibration)
        setStatusFrameChanged(Integer.MIN_VALUE);
    } catch (Error er) {
      handleError(er, true);
      String errMsg = getShapeErrorState();
      errMsg = ("ERROR adding vibrations: " + er + (errMsg.length() == 0 ? ""
          : "|" + errMsg));
      zap(errMsg);
      setErrorMessage(errMsg);
    }
    return getErrorMessage();
  }

  void deallocateReaderThreads() {
    fileManager.deallocateReaderThreads();
  }

  /**
   * deprecated -- this method does not actually open the file
   * 
   * @param fullPathName
   * @param fileName
   * @param clientFile
   * @deprecated
   */
  public void openClientFile(String fullPathName, String fileName,
                             Object clientFile) {
    createModelSet(fullPathName, fileName, clientFile, false);
  }

  public String openClientFileNoInit(String fullPathName, String fileName,
      Object atomSetCollection) {
	// maybe there needs to be a call to clear()
	// or something like that here
	// for when CdkEditBus calls this directly
	// null fullPathName implies we are doing a merge
	pushHoldRepaint("createModelSet");
	try {
		setErrorMessage(null);
		try {
			modelSet = modelManager.createModelSet(fullPathName, fileName, atomSetCollection, false);
		} catch (Error er) {
			handleError(er, true);
			String errMsg = getShapeErrorState();
			errMsg = ("ERROR creating model: " + er + (errMsg.length() == 0 ? ""
			: "|" + errMsg));
			zap(errMsg);
			setErrorMessage(errMsg);
		}
	} finally{
		popHoldRepaint("createModelSet");
	}
	return getErrorMessage();
}

  
  /**
   * 
   * @param fullPathName
   * @param fileName
   * @param atomSetCollection
   * @param isAppend
   * @return null or error message
   */
  public String createModelSet(String fullPathName, String fileName,
                                Object atomSetCollection, boolean isAppend) {
    // maybe there needs to be a call to clear()
    // or something like that here
    // for when CdkEditBus calls this directly
    // null fullPathName implies we are doing a merge
    pushHoldRepaint("createModelSet");
    try {
	    setErrorMessage(null);
	    try {
	      modelSet = modelManager.createModelSet(fullPathName, fileName,
	          atomSetCollection, isAppend);
	      if (!isAppend)
	        initializeModel();
	    } catch (Error er) {
	      handleError(er, true);
	      String errMsg = getShapeErrorState();
	      errMsg = ("ERROR creating model: " + er + (errMsg.length() == 0 ? ""
	          : "|" + errMsg));
	      zap(errMsg);
	      setErrorMessage(errMsg);
	    }
    } finally {
    	popHoldRepaint("createModelSet");
    }
    return getErrorMessage();
  }

  public Object getCurrentFileAsBytes() {
    String filename = getFullPathName();
    if (filename.equals("string") || filename.equals("string[]")
        || filename.equals("JSNode")) {
      String str = getCurrentFileAsString();
      try {
        return str.getBytes("UTF8");
      } catch (UnsupportedEncodingException e) {
        return str;
      }
    }
    String pathName = modelManager.getModelSetPathName();
    if (pathName == null)
      return "";
    return fileManager.getFileAsBytes(pathName);
  }

  public Object getFileAsBytes(String pathName) {
    return fileManager.getFileAsBytes(pathName);
  }

  public String getCurrentFileAsString() {
    String filename = getFullPathName();
    if (filename == "string") {
      return fileManager.getInlineData(-1);
    }
    if (filename == "string[]") {
      int modelIndex = getDisplayModelIndex();
      if (modelIndex < 0)
        return "";
      return fileManager.getInlineData(modelIndex);
    }
    if (filename == "JSNode") {
      return "<DOM NODE>";
    }
    String pathName = modelManager.getModelSetPathName();
    if (pathName == null)
      return null;
    return getFileAsString(pathName, Integer.MAX_VALUE, true);
  }

  public String getFullPathName() {
    return fileManager.getFullPathName();
  }

  public String getFileName() {
    return fileManager.getFileName();
  }

  public String getFileAsString(String name) {
    return getFileAsString(name, Integer.MAX_VALUE, false);
  }
  
  public String getFileAsString(String name, int nBytesMax, boolean doSpecialLoad) {
    if (name == null)
      return getCurrentFileAsString();
    String[] data = new String[2];
    data[0] = name;
    // ignore error completely
    getFileAsString(data, nBytesMax, doSpecialLoad);
    return data[1];
  }

  public String getFullPath(String name) {
    return fileManager.getFullPath(name, false);
  }

  public boolean getFileAsString(String[] data, int nBytesMax, boolean doSpecialLoad) {
    return fileManager.getFileDataOrErrorAsString(data, nBytesMax, doSpecialLoad);
  }

  String[] getFileInfo() {
    return fileManager.getFileInfo();
  }

  void setFileInfo(String[] fileInfo) {
    fileManager.setFileInfo(fileInfo);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ModelManager
  // ///////////////////////////////////////////////////////////////

  public void autoCalculate(int tokProperty) {
    switch (tokProperty) {
    case Token.surfacedistance:
      modelSet.getSurfaceDistanceMax();
      break;
    case Token.straightness:
      modelSet.calculateStraightness();
      break;
    }
  }
  
  int getSurfaceDistanceMax() {
    return modelSet.getSurfaceDistanceMax();
  }

  public void calculateStraightness() {
    modelSet.setHaveStraightness(false);
    modelSet.calculateStraightness();
  }

  public Point3f[] calculateSurface(BitSet bsSelected, float envelopeRadius) {
    if (bsSelected == null)
      bsSelected = selectionManager.bsSelection;
    addStateScript("calculate surfaceDistance "
        + (envelopeRadius == Float.MAX_VALUE ? "FROM" : "WITHIN"), null,
        bsSelected, null, "", false, true);
    return modelSet.calculateSurface(bsSelected, envelopeRadius);
  }

  void calculateStructures(BitSet bsAtoms) {
    // Eval
    modelSet.calculateStructures(bsAtoms);
  }

  public AtomIndexIterator getWithinModelIterator(Atom atom, float distance) {
    return modelSet.getWithinModelIterator(atom, distance);
  }

  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex,
                                                    float distance,
                                                    BitSet bsSelected,
                                                    boolean isGreaterOnly,
                                                    boolean modelZeroBased) {
    return modelSet.getWithinAtomSetIterator(atomIndex, distance, bsSelected,
        isGreaterOnly, modelZeroBased);
  }

  public void fillAtomData(AtomData atomData, int mode) {
    atomData.programInfo = "Jmol Version " + getJmolVersion();
    atomData.fileName = getFileName();
    modelSet.fillAtomData(atomData, mode);
  }

  StateScript addStateScript(String script, boolean addFrameNumber,
                      boolean postDefinitions) {
    return addStateScript(script, null, null, null, null, addFrameNumber,
        postDefinitions);
  }

  StateScript addStateScript(String script1, BitSet bsBonds, BitSet bsAtoms1,
                      BitSet bsAtoms2, String script2, boolean addFrameNumber,
                      boolean postDefinitions) {
    return modelSet.addStateScript(script1, bsBonds, bsAtoms1, bsAtoms2, script2,
        addFrameNumber, postDefinitions);
  }

  public boolean getEchoStateActive() {
    return modelSet.getEchoStateActive();
  }

  void setEchoStateActive(boolean TF) {
    modelSet.setEchoStateActive(TF);
  }

  public void zap(boolean notify, boolean resetUndo) {
    stopAnimationThreads();
    if (modelSet != null) {
      clearModelDependentObjects();
      fileManager.clear();
      repaintManager.clear();
      animationManager.clear();
      transformManager.clear();
      pickingManager.clear();
      selectionManager.clear();
      clearAllMeasurements();
      if (minimizer != null)
        minimizer.setProperty("clear", null);
      modelSet = modelManager.clear();
      if (haveDisplay) {
        mouseManager.clear();
      }
      stateManager.clear();
      global.clear();
      tempManager.clear();
      colorManager.clear();
      definedAtomSets.clear();
      dataManager.clear();
      System.gc();
    }
    modelSet = modelManager.zap();
    initializeModel();
    if (notify)
      setFileLoadStatus(FILE_STATUS_ZAPPED, null, (resetUndo ? "resetUndo"
          : null), null, null);
    if (Logger.debugging)
      Logger.checkMemory();
  }

  private void zap(String msg) {
    zap(true, false);
    echoMessage(msg);
  }

  void echoMessage(String msg) {
    int iShape = JmolConstants.SHAPE_ECHO;
    loadShape(iShape);
    setShapeProperty(iShape, "font", getFont3D("SansSerif", "Plain", 9));
    setShapeProperty(iShape, "target", "error");
    setShapeProperty(iShape, "text", msg);
  }

  public String getMinimizationInfo() {
    return (minimizer == null ? "" : (String) minimizer.getProperty("log", 0));
  }

  public boolean useMinimizationThread() {
    return global.useMinimizationThread && !autoExit;
  }

  public void initializeModel() {
    stopAnimationThreads();
    reset();
    selectAll();
    noneSelected = false;
    transformManager.setCenter();
    clearAtomSets();
    animationManager.initializePointers(1);
    setCurrentModelIndex(0);
    setBackgroundModelIndex(-1);
    setFrankOn(getShowFrank());
    if (haveDisplay)
      mouseManager.startHoverWatcher(true);
    setTainted(true);
    finalizeTransformParameters();
  }

  public String getModelSetName() {
    if (modelSet == null)
      return null;
    return modelSet.getModelSetName();
  }

  public String getModelSetFileName() {
    return modelManager.getModelSetFileName();
  }

  public String getUnitCellInfoText() {
    return modelSet.getUnitCellInfoText();
  }

  public Hashtable getSpaceGroupInfo(String spaceGroup) {
    return modelSet.getSpaceGroupInfo(spaceGroup);
  }

  void getPolymerPointsAndVectors(BitSet bs, Vector vList) {
    modelSet.getPolymerPointsAndVectors(bs, vList);
  }

  public String getModelSetProperty(String strProp) {
    // no longer used in Jmol
    return modelSet.getModelSetProperty(strProp);
  }

  public Object getModelSetAuxiliaryInfo(String strKey) {
    return modelSet.getModelSetAuxiliaryInfo(strKey);
  }

  public String getModelSetPathName() {
    return modelManager.getModelSetPathName();
  }

  public String getModelSetTypeName() {
    return modelSet.getModelSetTypeName();
  }

  public boolean haveFrame() {
    return haveModelSet();
  }

  boolean haveModelSet() {
    return modelSet != null;
  }

  void clearBfactorRange() {
    // Eval
    modelSet.clearBfactorRange();
  }

  public String getHybridizationAndAxes(int atomIndex, Vector3f z, Vector3f x,
                                        String lcaoType,
                                        boolean hybridizationCompatible) {
    return modelSet.getHybridizationAndAxes(atomIndex, z, x, lcaoType,
        hybridizationCompatible);
  }

  public BitSet getModelAtomBitSet(int modelIndex, boolean asCopy) {
    return modelSet.getModelAtomBitSet(modelIndex, asCopy);
  }

  public BitSet getModelBitSet(BitSet atomList, boolean allTrajectories) {
    return modelSet.getModelBitSet(atomList, allTrajectories);
  }

  Object getClientFile() {
    // DEPRECATED - use getExportJmolAdapter()
    return null;
  }

  // this is a problem. SmarterJmolAdapter doesn't implement this;
  // it can only return null.

  public String getClientAtomStringProperty(Object clientAtom,
                                            String propertyName) {
    return (modelAdapter == null || propertyName == null
        || propertyName.length() == 0 ? null : modelAdapter
        .getClientAtomStringProperty(clientAtom, propertyName));
  }

  /*****************************************************************************
   * This is the method that should be used to extract the model data from Jmol.
   * Note that the API provided by JmolAdapter is used to import data into Jmol
   * and to export data out of Jmol.
   * 
   * When exporting, a few of the methods in JmolAdapter do not make sense.
   * openBufferedReader(...) Others may be implemented in the future, but are
   * not currently all pdb specific things Just pass in null for the methods
   * that want a clientFile. The main methods to use are getFrameCount(null) ->
   * currently always returns 1 getAtomCount(null, 0) getAtomIterator(null, 0)
   * getBondIterator(null, 0)
   * 
   * The AtomIterator and BondIterator return Objects as unique IDs to identify
   * the atoms. atomIterator.getAtomUid() bondIterator.getAtomUid1() &
   * bondIterator.getAtomUid2() The ExportJmolAdapter will return the 0-based
   * atom index as a boxed Integer. That means that you can cast the results to
   * get a zero-based atom index int atomIndex =
   * ((Integer)atomIterator.getAtomUid()).intValue(); ... int bondedAtom1 =
   * ((Integer)bondIterator.getAtomUid1()).intValue(); int bondedAtom2 =
   * ((Integer)bondIterator.getAtomUid2()).intValue();
   * 
   * post questions to jmol-developers@lists.sf.net
   * 
   * @return A JmolAdapter
   ****************************************************************************/

  JmolAdapter getExportJmolAdapter() {
    /*
     * 
     * return new FrameExportJmolAdapter(this, modelSet);
     */
    return null;
  }

  public ModelSet getModelSet() {
    return modelSet;
  }

  public String getBoundBoxCommand(boolean withOptions) {
    return modelSet.getBoundBoxCommand(withOptions);
  }

  void setBoundBox(Point3f pt1, Point3f pt2, boolean byCorner) {
    modelSet.setBoundBox(pt1, pt2, byCorner);
  }

  public Point3f getBoundBoxCenter() {
    return modelSet.getBoundBoxCenter(animationManager.currentModelIndex);
  }

  Point3f getAverageAtomPoint() {
    return modelSet.getAverageAtomPoint();
  }

  void calcBoundBoxDimensions(BitSet bs) {
    modelSet.calcBoundBoxDimensions(bs);
    axesAreTainted = true;
  }

  BoxInfo getBoxInfo(BitSet bs) {
    return modelSet.getBoxInfo(bs);
  }

  float calcRotationRadius(Point3f center) {
    return modelSet
        .calcRotationRadius(animationManager.currentModelIndex, center);
  }

  public float calcRotationRadius(BitSet bs) {
    return modelSet.calcRotationRadius(bs);
  }

  public Vector3f getBoundBoxCornerVector() {
    return modelSet.getBoundBoxCornerVector();
  }

  Hashtable getBoundBoxInfo() {
    return modelSet.getBoundBoxInfo();
  }

  public BitSet getBoundBoxModels() {
    return modelSet.getBoundBoxModels();
  }

  public int getBoundBoxCenterX() {
    // used by axes renderer
    return dimScreen.width / 2;
  }

  public int getBoundBoxCenterY() {
    return dimScreen.height / 2;
  }

  public int getModelCount() {
    return modelSet.getModelCount();
  }

  String getModelInfoAsString() {
    return modelSet.getModelInfoAsString();
  }

  String getSymmetryInfoAsString() {
    return modelSet.getSymmetryInfoAsString();
  }

  public Properties getModelSetProperties() {
    return modelSet.getModelSetProperties();
  }

  public Hashtable getModelSetAuxiliaryInfo() {
    return modelSet.getModelSetAuxiliaryInfo();
  }

  public int getModelNumber(int modelIndex) {
    if (modelIndex < 0)
      return modelIndex;
    return modelSet.getModelNumber(modelIndex);
  }

  public int getModelFileNumber(int modelIndex) {
    if (modelIndex < 0)
      return 0;
    return modelSet.getModelFileNumber(modelIndex);
  }

  public String getModelNumberDotted(int modelIndex) {
    return modelIndex < 0 ? "0" : modelSet == null ? null : modelSet
        .getModelNumberDotted(modelIndex);
  }

  public String getModelName(int modelIndex) {
    return modelSet == null ? null : modelSet.getModelName(modelIndex);
  }

  public Properties getModelProperties(int modelIndex) {
    return modelSet.getModelProperties(modelIndex);
  }

  public String getModelProperty(int modelIndex, String propertyName) {
    return modelSet.getModelProperty(modelIndex, propertyName);
  }

  public String getModelFileInfo() {
    return modelSet.getModelFileInfo(getVisibleFramesBitSet());
  }

  String getModelFileInfoAll() {
    return modelSet.getModelFileInfo(null);
  }

  public Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return modelSet.getModelAuxiliaryInfo(modelIndex);
  }

  public Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
    return modelSet.getModelAuxiliaryInfo(modelIndex, keyName);
  }

  int getModelNumberIndex(int modelNumber, boolean useModelNumber,
                          boolean doSetTrajectory) {
    return modelSet.getModelNumberIndex(modelNumber, useModelNumber,
        doSetTrajectory);
  }

  boolean modelSetHasVibrationVectors() {
    return modelSet.modelSetHasVibrationVectors();
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    return modelSet.modelHasVibrationVectors(modelIndex);
  }

  public int getChainCount() {
    return modelSet.getChainCount(true);
  }

  public int getChainCountInModel(int modelIndex) {
    // revised to NOT include water chain (for menu)
    return modelSet.getChainCountInModel(modelIndex, false);
  }

  public int getChainCountInModel(int modelIndex, boolean countWater) {
    return modelSet.getChainCountInModel(modelIndex, countWater);
  }

  public int getGroupCount() {
    return modelSet.getGroupCount();
  }

  public int getGroupCountInModel(int modelIndex) {
    return modelSet.getGroupCountInModel(modelIndex);
  }

  public int getPolymerCount() {
    return modelSet.getBioPolymerCount();
  }

  public int getPolymerCountInModel(int modelIndex) {
    return modelSet.getBioPolymerCountInModel(modelIndex);
  }

  public int getAtomCount() {
    return modelSet.getAtomCount();
  }

  public int getAtomCountInModel(int modelIndex) {
    return modelSet.getAtomCountInModel(modelIndex);
  }

  /**
   * For use in setting a for() construct max value
   * 
   * @return used size of the bonds array;
   */
  public int getBondCount() {
    return modelSet.getBondCount();
  }

  /**
   * from JmolPopup.udateModelSetComputedMenu
   * 
   * @param modelIndex
   *          the model of interest or -1 for all
   * @return the actual number of connections
   */
  public int getBondCountInModel(int modelIndex) {
    return modelSet.getBondCountInModel(modelIndex);
  }

  BitSet getBondsForSelectedAtoms(BitSet bsAtoms) {
    // eval
    return modelSet.getBondsForSelectedAtoms(bsAtoms, global.bondModeOr || BitSetUtil.cardinalityOf(bsAtoms) == 1);
  }

  boolean frankClicked(int x, int y) {
    return frankOn && modelSet.frankClicked(x, y);
  }

  public int findNearestAtomIndex(int x, int y) {
    return (modelSet == null || !getAtomPicking() ? -1 : modelSet
        .findNearestAtomIndex(x, y));
  }

  void selectRectangle(Rectangle rect, int modifiers) {
    BitSet bs = modelSet.findAtomsInRectangle(rect, getVisibleFramesBitSet());
    if (BitSetUtil.firstSetBit(bs) < 0)
      return;
    pickingManager.atomsPicked(bs, modifiers);
  }

  void toCartesian(Point3f pt) {
    int modelIndex = animationManager.currentModelIndex;
    if (modelIndex < 0)
      return;
    modelSet.toCartesian(modelIndex, pt);
  }

  void toUnitCell(Point3f pt, Point3f offset) {
    int modelIndex = animationManager.currentModelIndex;
    if (modelIndex < 0)
      return;
    modelSet.toUnitCell(modelIndex, pt, offset);
  }

  void toFractional(Point3f pt) {
    int modelIndex = animationManager.currentModelIndex;
    if (modelIndex < 0)
      return;
    modelSet.toFractional(modelIndex, pt);
  }

  public void setCenterSelected() {
    // depricated
    setCenterBitSet(selectionManager.bsSelection, true);
  }

  public void setBondTolerance(float bondTolerance) {
    global.setParameterValue("bondTolerance", bondTolerance);
    global.bondTolerance = bondTolerance;
  }

  public float getBondTolerance() {
    return global.bondTolerance;
  }

  public void setMinBondDistance(float minBondDistance) {
    // PreferencesDialog
    global.setParameterValue("minBondDistance", minBondDistance);
    global.minBondDistance = minBondDistance;
  }

  public float getMinBondDistance() {
    return global.minBondDistance;
  }

  int[] getAtomIndices(BitSet bs) {
    return modelSet.getAtomIndices(bs);
  }

  public BitSet getAtomBits(int tokType, Object specInfo) {
    return modelSet.getAtomBits(tokType, specInfo);
  }

  BitSet getSequenceBits(String specInfo, BitSet bs) {
    return modelSet.getSequenceBits(specInfo, bs);
  }

  BitSet getAtomsWithin(float distance, Point3f coord) {
    BitSet bs = new BitSet();
    modelSet.getAtomsWithin(distance, coord, bs, -1);
    if (distance < 0)
      modelSet.getAtomsWithin(-distance, coord, bs, -1);
    return bs;
  }

  BitSet getAtomsWithin(float distance, Point4f plane) {
    return modelSet.getAtomsWithin(distance, plane);
  }

  BitSet getAtomsWithin(float distance, BitSet bs, boolean isWithinModelSet) {
    return modelSet.getAtomsWithin(distance, bs, isWithinModelSet);
  }

  BitSet getAtomsConnected(float min, float max, int intType, BitSet bs) {
    return modelSet.getAtomsConnected(min, max, intType, bs);
  }

  BitSet getBranchBitSet(int atomIndex, int atomIndexNot) {
    return modelSet.getBranchBitSet(atomIndex, atomIndexNot);
  }

  int getAtomIndexFromAtomNumber(int atomNumber) {
    return modelSet.getAtomIndexFromAtomNumber(atomNumber,
        getVisibleFramesBitSet());
  }

  public BitSet getElementsPresentBitSet(int modelIndex) {
    return modelSet.getElementsPresentBitSet(modelIndex);
  }

  public Hashtable getHeteroList(int modelIndex) {
    return modelSet.getHeteroList(modelIndex);
  }

  BitSet getVisibleSet() {
    return modelSet.getVisibleSet();
  }

  BitSet getClickableSet() {
    return modelSet.getClickableSet();
  }

  void calcSelectedGroupsCount() {
    modelSet.calcSelectedGroupsCount(selectionManager.bsSelection);
  }

  void calcSelectedMonomersCount() {
    modelSet.calcSelectedMonomersCount(selectionManager.bsSelection);
  }

  void calcSelectedMoleculesCount() {
    modelSet.calcSelectedMoleculesCount(selectionManager.bsSelection);
  }

  String getFileHeader() {
    return modelSet.getFileHeader(animationManager.currentModelIndex);
  }

  Object getFileData() {
    return modelSet.getFileData(animationManager.currentModelIndex);
  }

  public Hashtable getCifData(int modelIndex) {
    String name = getModelFileName(modelIndex);
    String data = getFileAsString(name);
    if (data == null)
      return null;
    return CifDataReader.readCifData(new BufferedReader(new StringReader(data)));
  }

  String getPDBHeader() {
    return modelSet.getPDBHeader(animationManager.currentModelIndex);
  }

  public Hashtable getModelInfo(Object atomExpression) {
    return modelSet.getModelInfo(getModelBitSet(getAtomBitSet(atomExpression), false));
  }

  public Hashtable getAuxiliaryInfo(Object atomExpression) {
    return modelSet
        .getAuxiliaryInfo(getModelBitSet(getAtomBitSet(atomExpression), false));
  }

  public Hashtable getShapeInfo() {
    return modelSet.getShapeInfo();
  }

  int getShapeIdFromObjectName(String objectName) {
    return modelSet.getShapeIdFromObjectName(objectName);
  }

  Vector getAllAtomInfo(Object atomExpression) {
    return modelSet.getAllAtomInfo(getAtomBitSet(atomExpression));
  }

  Vector getAllBondInfo(Object atomExpression) {
    return modelSet.getAllBondInfo(getAtomBitSet(atomExpression));
  }

  Vector getMoleculeInfo(Object atomExpression) {
    return modelSet.getMoleculeInfo(getAtomBitSet(atomExpression));
  }

  public String getChimeInfo(int tok) {
    return modelSet.getChimeInfo(tok, selectionManager.bsSelection);
  }

  public Hashtable getAllChainInfo(Object atomExpression) {
    return modelSet.getAllChainInfo(getAtomBitSet(atomExpression));
  }

  public Hashtable getAllPolymerInfo(Object atomExpression) {
    return modelSet.getAllPolymerInfo(getAtomBitSet(atomExpression));
  }

  public String getStateInfo() {
    return getStateInfo(null);
  }

  final static String STATE_VERSION_STAMP = "# Jmol state version ";

  public String getStateInfo(String type) {
    boolean isAll = (type == null || type.equalsIgnoreCase("all"));
    StringBuffer s = new StringBuffer("");
    StringBuffer sfunc = (isAll ? new StringBuffer("function _setState() {\n")
        : null);
    if (isAll)
      s.append(STATE_VERSION_STAMP + getJmolVersion() + ";\n");
    if (isApplet && isAll) {
      StateManager.appendCmd(s, "# fullName = " + Escape.escape(fullName));
      StateManager.appendCmd(s, "# documentBase = "
          + Escape.escape(appletDocumentBase));
      StateManager
          .appendCmd(s, "# codeBase = " + Escape.escape(appletCodeBase));
      s.append("\n");
    }
    // window state
    if (isAll || type.equalsIgnoreCase("windowState"))
      s.append(global.getWindowState(sfunc));
    if (isAll)
      s.append(eval.getState());
    // file state
    if (isAll || type.equalsIgnoreCase("fileState"))
      s.append(fileManager.getState(sfunc));
    // all state scripts (definitions, dataFrames, calculations, configurations,
    // rebonding
    if (isAll || type.equalsIgnoreCase("definedState"))
      s.append(modelSet.getDefinedState(sfunc, true));
    // numerical values
    if (isAll || type.equalsIgnoreCase("variableState"))
      s.append(global.getState(sfunc));
    if (isAll || type.equalsIgnoreCase("dataState"))
      dataManager.getDataState(s, sfunc, modelSet.atoms, getAtomCount(),
          modelSet.getAtomicPropertyState(-1, null));
    // connections, atoms, bonds, labels, echos, shapes
    if (isAll || type.equalsIgnoreCase("modelState"))
      s.append(modelSet.getState(sfunc, true));
    // color scheme
    if (isAll || type.equalsIgnoreCase("colorState"))
      s.append(ColorManager.getState(sfunc));
    // frame information
    if (isAll || type.equalsIgnoreCase("frameState"))
      s.append(animationManager.getState(sfunc));
    // orientation and slabbing
    if (isAll || type.equalsIgnoreCase("perspectiveState"))
      s.append(transformManager.getState(sfunc));
    // display and selections
    if (isAll || type.equalsIgnoreCase("selectionState"))
      s.append(selectionManager.getState(sfunc));
    if (sfunc != null) {
      StateManager.appendCmd(sfunc, "set refreshing true");
      StateManager.appendCmd(sfunc, "set antialiasDisplay "
          + global.antialiasDisplay);
      StateManager.appendCmd(sfunc, "set antialiasTranslucent "
          + global.antialiasTranslucent);
      StateManager.appendCmd(sfunc, "set antialiasImages "
          + global.antialiasImages);
      if (getSpinOn())
        StateManager.appendCmd(sfunc, "spin on");
      sfunc.append("}\n\n_setState;\n");
    }
    if (isAll)
      s.append(sfunc);
    return s.toString();
  }

  public String getStructureState() {
    return modelSet.getState(null, false);
  }

  String getProteinStructureState() {
    return modelSet.getProteinStructureState(selectionManager.bsSelection,
        false, false, false);
  }

  public String getCoordinateState(BitSet bsSelected) {
    return modelSet.getAtomicPropertyState(AtomCollection.TAINT_COORD,
        bsSelected);
  }

  void setCurrentColorRange(String label) {
    float[] data = getDataFloat(label);
    BitSet bs = (data == null ? null : (BitSet) ((Object[]) dataManager
        .getData(label))[2]);
    setCurrentColorRange(data, bs);
  }

  void setCurrentColorRange(float[] data, BitSet bs) {
    colorManager.setCurrentColorRange(data, bs, global.propertyColorScheme);
  }

  public void setCurrentColorRange(float min, float max) {
    colorManager.setCurrentColorRange(min, max);
  }

  public float[] getCurrentColorRange() {
    return colorManager.getCurrentColorRange();
  }

  public void setData(String type, Object[] data, int atomCount,
                      int matchField, int matchFieldColumnCount, int field,
                      int fieldColumnCount) {
    dataManager.setData(this, type, data, atomCount, matchField,
        matchFieldColumnCount, field, fieldColumnCount);
  }

  public static Object testData; // for isosurface
  public static Object testData2; // for isosurface

  Object[] getData(String type) {
    return dataManager.getData(type);
  }

  public float[] getDataFloat(String label) {
    return dataManager.getDataFloat(label);
  }

  float[][] getDataFloat2D(String label) {
    return dataManager.getDataFloat2D(label);
  }

  float[][][] getDataFloat3D(String label) {
    return null;
    // not implemented yet return dataManager.getDataFloat3D(label);
  }

  public float getDataFloat(String label, int atomIndex) {
    return dataManager.getDataFloat(label, atomIndex);
  }

  public String getAltLocListInModel(int modelIndex) {
    return modelSet.getAltLocListInModel(modelIndex);
  }

  public BitSet setConformation() {
    // user has selected some atoms, now this sets that as a conformation
    // with the effect of rewriting the cartoons to match

    return modelSet.setConformation(-1, selectionManager.bsSelection);
  }

  // AKA "configuration"
  public BitSet setConformation(int conformationIndex) {
    return modelSet.setConformation(animationManager.currentModelIndex,
        conformationIndex);
  }

  int autoHbond(BitSet bsBonds) {
    // Eval
    return autoHbond(selectionManager.bsSelection,
        selectionManager.bsSelection, bsBonds, 0, 0);
  }

  int autoHbond(BitSet bsFrom, BitSet bsTo, BitSet bsBonds,
                float maxXYDistance, float minAttachedAngle) {
    // Eval
    if (maxXYDistance < 0)
      maxXYDistance = global.hbondsDistanceMaximum;
    if (minAttachedAngle < 0)
      minAttachedAngle = global.hbondsAngleMinimum;
    minAttachedAngle *= (float) (Math.PI / 180);
    return modelSet.autoHbond(bsFrom, bsTo, bsBonds, maxXYDistance,
        minAttachedAngle);
  }

  public boolean hasCalculatedHBonds(BitSet bsAtoms) {
    return modelSet.hasCalculatedHBonds(bsAtoms);
  }

  public boolean havePartialCharges() {
    return modelSet.getPartialCharges() != null;
  }

  public SymmetryInterface getCurrentUnitCell() {
    return modelSet.getUnitCell(getDisplayModelIndex());
  }

  void setCurrentUnitCellOffset(int offset) {
    int modelIndex = animationManager.currentModelIndex;
    if (modelSet.setUnitCellOffset(modelIndex, offset))
      global.setParameterValue("=frame " + getModelNumberDotted(modelIndex)
          + "; set unitcell ", offset);
  }

  void setCurrentUnitCellOffset(Point3f pt) {
    int modelIndex = animationManager.currentModelIndex;
    if (modelSet.setUnitCellOffset(modelIndex, pt))
      global.setParameterValue("=frame " + getModelNumberDotted(modelIndex)
          + "; set unitcell ", Escape.escape(pt));
  }

  /*
   * ****************************************************************************
   * delegated to MeasurementManager
   * **************************************************************************
   */

  public String getDefaultMeasurementLabel(int nPoints) {
    switch (nPoints) {
    case 2:
      return global.defaultDistanceLabel;
    case 3:
      return global.defaultAngleLabel;
    default:
      return global.defaultTorsionLabel;
    }
  }

  public int getMeasurementCount() {
    int count = getShapePropertyAsInt(JmolConstants.SHAPE_MEASURES, "count");
    return count <= 0 ? 0 : count;
  }

  public String getMeasurementStringValue(int i) {
    String str = ""
        + getShapeProperty(JmolConstants.SHAPE_MEASURES, "stringValue", i);
    return str;
  }

  Vector getMeasurementInfo() {
    return (Vector) getShapeProperty(JmolConstants.SHAPE_MEASURES, "info");
  }

  public String getMeasurementInfoAsString() {
    return (String) getShapeProperty(JmolConstants.SHAPE_MEASURES, "infostring");
  }

  public int[] getMeasurementCountPlusIndices(int i) {
    int[] List = (int[]) getShapeProperty(JmolConstants.SHAPE_MEASURES,
        "countPlusIndices", i);
    return List;
  }

  void setPendingMeasurement(MeasurementPending measurementPending) {
    // from MouseManager
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "pending",
        measurementPending);
  }

  MeasurementPending getPendingMeasurement() {
    return (MeasurementPending) getShapeProperty(JmolConstants.SHAPE_MEASURES,
        "pending");
  }

  void clearAllMeasurements() {
    // Eval only
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
  }

  public void clearMeasurements() {
    // depricated but in the API -- use "script" directly
    // see clearAllMeasurements()
    evalString("measures delete");
  }

  public boolean getJustifyMeasurements() {
    return global.justifyMeasurements;
  }

  void setMeasurementFormats(String strFormat) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormats", strFormat);
  }

  void defineMeasurement(Vector monitorExpressions, float[] rangeMinMax,
                         boolean isDelete, boolean isAll,
                         boolean isAllConnected, boolean isOn, boolean isOff,
                         String strFormat) {
    // Eval.monitor()
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setConnected",
        isAllConnected ? Boolean.TRUE : Boolean.FALSE);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setRange", rangeMinMax);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormat", strFormat);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, (isDelete ? "deleteVector"
        : isOn ? "showVector" : isOff ? "hideVector" : "defineVector")
        + (isAll ? "_All" : ""), monitorExpressions);
  }

  public void deleteMeasurement(int i) {
    // Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", new Integer(i));
  }

  void deleteMeasurement(int[] atomCountPlusIndices) {
    // Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete",
        atomCountPlusIndices);
  }

  public void showMeasurement(int[] atomCountPlusIndices, boolean isON) {
    // Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, isON ? "show" : "hide",
        atomCountPlusIndices);
  }

  void hideMeasurements(boolean isOFF) {
    // Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "hideAll", Boolean
        .valueOf(isOFF));
  }

  void toggleMeasurement(int[] atomCountPlusIndices, String strFormat) {
    // Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES,
        (strFormat == null ? "toggle" : "toggleOn"), atomCountPlusIndices);
    if (strFormat != null)
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormats", strFormat);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to RepaintManager
  // ///////////////////////////////////////////////////////////////

  void setAnimationDirection(int direction) {// 1 or -1
    // Eval
    animationManager.setAnimationDirection(direction);
  }

  void reverseAnimation() {
    // Eval
    animationManager.reverseAnimation();
  }

  int getAnimationDirection() {
    return animationManager.animationDirection;
  }

  Hashtable getAnimationInfo() {
    return animationManager.getAnimationInfo();
  }

  public void setAnimationFps(int fps) {
    if (fps < 1)
      fps = 1;
    if (fps > 50)
      fps = 50;
    global.setParameterValue("animationFps", fps);
    // Eval
    // app AtomSetChooser
    animationManager.setAnimationFps(fps);
  }

  public int getAnimationFps() {
    return animationManager.animationFps;
  }

  void setAnimationReplayMode(int replay, float firstFrameDelay,
                              float lastFrameDelay) {
    // Eval

    // 0 means once
    // 1 means loop
    // 2 means palindrome
    animationManager.setAnimationReplayMode(replay, firstFrameDelay,
        lastFrameDelay);
  }

  int getAnimationReplayMode() {
    return animationManager.animationReplayMode;
  }

  void setAnimationOn(boolean animationOn) {
    // Eval
    boolean wasAnimating = animationManager.animationOn;
    if (animationOn == wasAnimating)
      return;
    animationManager.setAnimationOn(animationOn);
  }

  void resumeAnimation() {
    // Eval
    if (animationManager.animationOn) {
      Logger.debug("animation is ON in resumeAnimation");
      return;
    }
    animationManager.resumeAnimation();
  }

  void pauseAnimation() {
    // Eval
    if (!animationManager.animationOn || animationManager.animationPaused) {
      return;
    }
    animationManager.pauseAnimation();
  }

  void setAnimationRange(int modelIndex1, int modelIndex2) {
    animationManager.setAnimationRange(modelIndex1, modelIndex2);
  }

  public BitSet getVisibleFramesBitSet() {
    BitSet bs = BitSetUtil.copy(animationManager.getVisibleFramesBitSet());
    modelSet.selectDisplayedTrajectories(bs);
    return bs;
  }

  boolean isAnimationOn() {
    return animationManager.animationOn;
  }

  void setAnimationNext() {
    // Eval
    animationManager.setAnimationNext();
  }

  void setAnimationPrevious() {
    // Eval
    animationManager.setAnimationPrevious();
  }

  void setAnimationLast() {
    // Eval
    animationManager.setAnimationLast();
  }

  void rewindAnimation() {
    // Eval
    animationManager.rewindAnimation();
  }

  void setCurrentModelIndex(int modelIndex) {
    // Eval
    // initializeModel
    if (modelIndex == Integer.MIN_VALUE) {
      // just forcing popup menu update
      prevFrame = Integer.MIN_VALUE;
      setCurrentModelIndex(animationManager.currentModelIndex, true);
      return;
    }
    animationManager.setCurrentModelIndex(modelIndex);
  }

  void setTrajectory(int modelIndex) {
    modelSet.setTrajectory(modelIndex);
  }

  public void setTrajectory(BitSet bsModels) {
    modelSet.setTrajectory(bsModels);
  }

  boolean isTrajectory(int modelIndex) {
    return modelSet.isTrajectory(modelIndex);
  }

  public BitSet getBitSetTrajectories() {
    return modelSet.getBitSetTrajectories();
  }

  String getTrajectoryInfo() {
    return modelSet.getTrajectoryInfo();
  }

  void setFrameOffset(int modelIndex) {
    transformManager.setFrameOffset(modelIndex);
  }

  BitSet bsFrameOffsets;
  Point3f[] frameOffsets;

  public void setFrameOffsets(BitSet bsAtoms) {
    bsFrameOffsets = bsAtoms;
    transformManager.setFrameOffsets(frameOffsets = modelSet
        .getFrameOffsets(bsFrameOffsets));
  }

  public BitSet getFrameOffsets() {
    return bsFrameOffsets;
  }

  public void setCurrentModelIndex(int modelIndex, boolean clearBackground) {
    // Eval
    // initializeModel
    animationManager.setCurrentModelIndex(modelIndex, clearBackground);
  }

  public int getCurrentModelIndex() {
    return animationManager.currentModelIndex;
  }

  public int getDisplayModelIndex() {
    // modified to indicate if there is also a background model index
    int modelIndex = animationManager.currentModelIndex;
    int backgroundIndex = getBackgroundModelIndex();
    return (backgroundIndex >= 0 ? -2 - modelIndex : modelIndex);
  }

  boolean haveFileSet() {
    return (getModelCount() > 1 && getModelNumber(0) > 1000000);
  }

  void setBackgroundModelIndex(int modelIndex) {
    // initializeModel
    animationManager.setBackgroundModelIndex(modelIndex);
    global.setParameterValue("backgroundModel", modelSet
        .getModelNumberDotted(modelIndex));
  }

  public int getBackgroundModelIndex() {
    return animationManager.backgroundModelIndex;
  }
  
  void setFrameVariables(int firstModelIndex, int lastModelIndex) {
    global.setParameterValue("_firstFrame",
        getModelNumberDotted(firstModelIndex));
    global
        .setParameterValue("_lastFrame", getModelNumberDotted(lastModelIndex));
  }

  boolean wasInMotion = false;
  int motionEventNumber;

  public int getMotionEventNumber() {
    return motionEventNumber;
  }

  void setInMotion(boolean inMotion) {
    // MouseManager, TransformManager
    if (wasInMotion ^ inMotion) {
      animationManager.setInMotion(inMotion);
      if (inMotion) {
        ++motionEventNumber;
      } else {
        repaintManager.refresh();
      }
      wasInMotion = inMotion;
    }
  }

  public boolean getInMotion() {
    // mps
    return animationManager.inMotion;
  }

  public void pushHoldRepaint() {
    pushHoldRepaint(null);
  }

  void pushHoldRepaint(String why) {
    repaintManager.pushHoldRepaint();
  }

  public void popHoldRepaint() {
    repaintManager.popHoldRepaint();
  }

  void popHoldRepaint(String why) {
    repaintManager.popHoldRepaint();
  }

  private boolean refreshing = true;

  void setRefreshing(boolean TF) {
    // also set by Eval error to TRUE
    refreshing = TF;
  }

  boolean getRefreshing() {
    return refreshing;
  }

  /**
   * initiate a repaint/update sequence if it has not already been requested.
   * invoked whenever any operation causes changes that require new rendering.
   * 
   * The repaint/update sequence will only be invoked if (a) no repaint is
   * already pending and (b) there is no hold flag set in repaintManager.
   * 
   * Sequence is as follows:
   * 
   * 1) RepaintManager.refresh() checks flags and then calls Viewer.repaint() 2)
   * Viewer.repaint() invokes display.repaint(), provided display is not null
   * (headless) 3) The system responds with an invocation of
   * Jmol.update(Graphics g), which we are routing through Jmol.paint(Graphics
   * g). 4) Jmol.update invokes Viewer.setScreenDimension(size), which makes the
   * necessary changes in parameters for any new window size. 5) Jmol.update
   * invokes Viewer.renderScreenImage(g, size, rectClip) 6)
   * Viewer.renderScreenImage checks object visibility, invokes render1 to do
   * the actual creation of the image pixel map and send it to the screen, and
   * then invokes repaintView() 7) Viewer.repaintView() invokes
   * RepaintManager.repaintDone(), to clear the flags and then use notify() to
   * release any threads holding on wait().
   * 
   * @param mode
   * @param strWhy
   * 
   */
  public void refresh(int mode, String strWhy) {
    // refresh(2) indicates this is a mouse motion -- not going through Eval
    // so we bypass Eval and mainline on the other viewer!
    // refresh(-1) is used in stateManager to force no repaint
    // refresh(3) is used by operations to ONLY do a repaint -- no syncing
    if (repaintManager == null || !refreshing)
      return;
    //System.out.println(" viewer.refresh " + strWhy);System.out.flush();
    if (mode > 0)
      repaintManager.refresh();
    if (mode % 3 != 0 && statusManager.doSync())
      statusManager.setSync(mode == 2 ? strWhy : null);
  }

  void requestRepaintAndWait() {
    if (!haveDisplay)
      return;
    repaintManager.requestRepaintAndWait();
    if (statusManager.doSync())
      statusManager.setSync(null);
  }

  void setSync() {
    if (statusManager.doSync())
      statusManager.setSync(null);
  }

  public void repaintView() {
    repaintManager.repaintDone();
  }

  private boolean axesAreTainted = false;

  public boolean areAxesTainted() {
    boolean TF = axesAreTainted;
    axesAreTainted = false;
    return TF;
  }

  // //////////// screen/image methods ///////////////

  final Dimension dimScreen = new Dimension();

  // final Rectangle rectClip = new Rectangle();

  private int maximumSize = Integer.MAX_VALUE;

  private void setMaximumSize(int x) {
    maximumSize = Math.max(x, 100);
  }

  public void setScreenDimension(Dimension dim) {
    // There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
    // so don't try dim1.equals(dim2)
    dim.height = Math.min(dim.height, maximumSize);
    dim.width = Math.min(dim.width, maximumSize);
    int height = dim.height;
    int width = dim.width;
    if (transformManager.stereoMode == JmolConstants.STEREO_DOUBLE)
      width = (width + 1) / 2;
    if (dimScreen.width == width && dimScreen.height == height)
      return;
    resizeImage(width, height, false, false, true);
  }

  private float imageFontScaling = 1;

  public float getImageFontScaling() {
    return imageFontScaling;
  }

  private void resizeImage(int width, int height, boolean isImageWrite,
                           boolean isGenerator, boolean isReset) {
    if (!isImageWrite && creatingImage)
      return;
    if (width > 0) {
      if (isImageWrite && !isReset)
        setImageFontScaling(width, height);
      dimScreen.width = width;
      dimScreen.height = height;
    }

    antialiasDisplay = false;
    
    if (isReset) {
      imageFontScaling = 1;
      antialiasDisplay = global.antialiasDisplay;
    } else if (isImageWrite && !isGenerator) {
      antialiasDisplay = global.antialiasImages;
    }
    if (antialiasDisplay)
      imageFontScaling *= 2;
    if (width > 0 && !isImageWrite) {
      global.setParameterValue("_width", width);
      global.setParameterValue("_height", height);
      setStatusResized(width, height);
    }
    if (width <= 0) {
      width = dimScreen.width;
      height = dimScreen.height;
    }
    transformManager.setScreenParameters(width, height,
        isImageWrite || isReset ? global.zoomLarge : false, antialiasDisplay,
        false, false);
    g3d.setWindowParameters(width, height, antialiasDisplay);
  }

  public int getScreenWidth() {
    return dimScreen.width;
  }

  public int getScreenHeight() {
    return dimScreen.height;
  }

  public int getScreenDim() {
    return (global.zoomLarge == (dimScreen.height > dimScreen.width) ? dimScreen.height
        : dimScreen.width);
  }

  public String generateOutput(String type, String fileName, int width,
                               int height) {
    if (isDataOnly)
      return "";
    mustRender = true;
    saveState("_Export");
    int saveWidth = dimScreen.width;
    int saveHeight = dimScreen.height;
    resizeImage(width, height, true, true, false);
    setModelVisibility();
    String data = repaintManager.generateOutput(type, g3d, modelSet, fileName);
    // mth 2003-01-09 Linux Sun JVM 1.4.2_02
    // Sun is throwing a NullPointerExceptions inside graphics routines
    // while the window is resized.
    resizeImage(saveWidth, saveHeight, true, true, true);
    return data;
  }

  public void renderScreenImage(Graphics gLeft, Graphics gRight,
                                Dimension size, Rectangle clip) {
    // from paint/update event
    // gRight is for second stereo applet
    // when this is the stereoSlave, no rendering occurs through this applet
    // directly, only from the other applet.
    // this is for relatively specialized geoWall-type installations

    if (creatingImage)
      return;
    if (isTainted || getSlabEnabled())
      setModelVisibility();
    isTainted = false;
    if (size != null)
      setScreenDimension(size);
    if (gRight == null) {
      Image image = getScreenImage();
      if (transformManager.stereoMode == JmolConstants.STEREO_DOUBLE) {
        render1(gLeft, image, dimScreen.width, 0);
        image = getImage(false);
      }
      render1(gLeft, image, 0, 0);
    } else {
      render1(gRight, getImage(true), 0, 0);
      render1(gLeft, getImage(false), 0, 0);
    }
    repaintView();
  }

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
    renderScreenImage(g, null, size, clip);
  }

  private Image getImage(boolean isDouble) {
    Image image = null;
    try {
      g3d.beginRendering(transformManager.getStereoRotationMatrix(isDouble));
      render();
      g3d.endRendering();
      image = g3d.getScreenImage();
    } catch (Error er) {
      handleError(er, false);
      setErrorMessage("Error during rendering: " + er);
    }
    return image;
  }

  private boolean antialiasDisplay;

  private void render() {
    boolean antialias2 = antialiasDisplay && global.antialiasTranslucent;
    repaintManager.render(g3d, modelSet);
    if (g3d.setPass2(antialias2)) {
      transformManager.setAntialias(antialias2);
      repaintManager.render(g3d, modelSet);
      transformManager.setAntialias(antialiasDisplay);
    }
  }

  private Image getStereoImage(int stereoMode) {
    g3d.beginRendering(transformManager.getStereoRotationMatrix(true));
    render();
    g3d.endRendering();
    g3d.snapshotAnaglyphChannelBytes();
    g3d.beginRendering(transformManager.getStereoRotationMatrix(false));
    render();
    g3d.endRendering();
    switch (stereoMode) {
    case JmolConstants.STEREO_REDCYAN:
      g3d.applyCyanAnaglyph();
      break;
    case JmolConstants.STEREO_CUSTOM:
      g3d.applyCustomAnaglyph(transformManager.stereoColors);
      break;
    case JmolConstants.STEREO_REDBLUE:
      g3d.applyBlueAnaglyph();
      break;
    default:
      g3d.applyGreenAnaglyph();
    }
    return g3d.getScreenImage();
  }

  private void render1(Graphics g, Image img, int x, int y) {
    if (g != null && img != null) {
      try {
        g.drawImage(img, x, y, null);
      } catch (NullPointerException npe) {
        Logger.error("Sun!! ... fix graphics your bugs!");
      }
    }
    g3d.releaseScreenImage();
  }

  public Image getScreenImage() {
    return (transformManager.stereoMode <= JmolConstants.STEREO_DOUBLE ? getImage(transformManager.stereoMode == JmolConstants.STEREO_DOUBLE)
        : getStereoImage(transformManager.stereoMode));
  }

  /**
   * @param type
   *          "PNG", "JPG", "JPEG", "JPG64", "PPM", "GIF"
   * @param quality
   * @param width
   * @param height
   * @param fileName
   * @param os
   * @return base64-encoded or binary version of the image
   */
  public Object getImageAs(String type, int quality, int width, int height,
                           String fileName, OutputStream os) {
    int saveWidth = dimScreen.width;
    int saveHeight = dimScreen.height;
    mustRender = true;
    resizeImage(width, height, true, false, false);
    setModelVisibility();
    creatingImage = true;
    JmolImageCreatorInterface c = null;
    Object bytes = null;
    type = type.toLowerCase();
    if (!Parser.isOneOf(type, "jpg;jpeg;jpg64;jpeg64"))
      try {
        c = (JmolImageCreatorInterface) Interface
            .getOptionInterface("export.image.ImageCreator");
      } catch (Error er) {
        // unsigned applet will not have this interface
        // and thus will not use os or filename
      }
    if (c == null) {
      Image eImage = getScreenImage();
      if (eImage != null) {
        try {
          if (quality < 0)
            quality = 75;
          bytes = JpegEncoder.getBytes(eImage, quality, "");
          releaseScreenImage();
          if (type.equals("jpg64") || type.equals("jpeg64"))
            bytes = (bytes == null ? "" : Base64.getBase64((byte[]) bytes)
                .toString());
        } catch (Error er) {
          releaseScreenImage();
          handleError(er, false);
          setErrorMessage("Error creating image: " + er);
          bytes = getErrorMessage();
        }
      }
    } else {
      c.setViewer(this);
      try {
        bytes = c.getImageBytes(type, quality, fileName, null, os);
      } catch (IOException e) {
        bytes = e;
        setErrorMessage("Error creating image: " + e);
      } catch (Error er) {
        handleError(er, false);
        setErrorMessage("Error creating image: " + er);
        bytes = getErrorMessage();
      }
    }
    creatingImage = false;
    resizeImage(saveWidth, saveHeight, true, false, true);
    return bytes;
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
  }

  // ///////////////////////////////////////////////////////////////
  // routines for script support
  // ///////////////////////////////////////////////////////////////

  boolean getAllowEmbeddedScripts() {
    return global.allowEmbeddedScripts;
  }

  public String evalFile(String strFilename) {
    // app -s flag
    int ptWait = strFilename.indexOf(" -noqueue"); // for TestScripts.java
    if (ptWait >= 0) {
      return (String) evalStringWaitStatus("String", strFilename.substring(0,
          ptWait), "", true, false, false);
    }
    return scriptManager.addScript(strFilename, true, false);
  }

  public String script(String strScript) {
    return evalString(strScript);
  }

  String interruptScript = "";

  String getInterruptScript() {
    String s = interruptScript;
    interruptScript = "";
    if (Logger.debugging && s != "")
      Logger.debug("interrupt: " + s);
    return s;
  }

  public String evalString(String strScript) {
    return evalStringQuiet(strScript, false, true);
  }

  public String evalStringQuiet(String strScript) {
    return evalStringQuiet(strScript, true, true);
  }

  String evalStringQuiet(String strScript, boolean isQuiet,
                         boolean allowSyncScript) {
    // central point for all incoming script processing
    // all menu items, all mouse movement -- everything goes through this method
    // by setting syncScriptTarget = ">" the user can direct that all scripts
    // initiated WITHIN this applet (not sent to it)
    // we append #NOSYNC; here so that the receiving applet does not attempt
    // to pass it back to us or any other applet.
    if (allowSyncScript && statusManager.syncingScripts
        && strScript.indexOf("#NOSYNC;") < 0)
      syncScript(strScript + " #NOSYNC;", null);
    if (eval.isExecutionPaused() && strScript.charAt(0) != '!')
      strScript = '!' + TextFormat.trim(strScript, "\n\r\t ");
    boolean isInterrupt = (strScript.length() > 0 && strScript.charAt(0) == '!');
    if (isInterrupt)
      strScript = strScript.substring(1);
    String msg = checkScriptExecution(strScript);
    if (msg != null)
      return msg;
    if (isScriptExecuting() && (isInterrupt || eval.isExecutionPaused())) {
      interruptScript = strScript;
      if (strScript.indexOf("moveto ") == 0)
        scriptManager.flushQueue("moveto ");
      return "!" + strScript;
    }
    interruptScript = "";
    if (isQuiet)
      strScript += JmolConstants.SCRIPT_EDITOR_IGNORE;
    return scriptManager.addScript(strScript, false, isQuiet
        && !getMessageStyleChime());
  }

  private String checkScriptExecution(String strScript) {
    String str = strScript;
    if (str.indexOf("\0##") >= 0)
      str = str.substring(0, str.indexOf("\0##"));
    if (checkResume(str))
      return "script processing resumed";
    if (checkStepping(str))
      return "script processing stepped";
    if (checkHalt(str))
      return "script execution halted";
    return null;
  }

  boolean usingScriptQueue() {
    return scriptManager.useQueue;
  }

  public void clearScriptQueue() {
    // Eval
    // checkHalt **
    scriptManager.clearQueue();
  }

  public boolean checkResume(String str) {
    if (str.equalsIgnoreCase("resume")) {
      scriptStatus("", "execution resumed", 0, null);
      resumeScriptExecution();
      return true;
    }
    return false;
  }

  public boolean checkStepping(String str) {
    if (str.equalsIgnoreCase("step")) {
      stepScriptExecution();
      return true;
    }
    if (str.equalsIgnoreCase("?")) {
      scriptStatus(eval.getNextStatement());
      return true;
    }
    return false;
  }

  public boolean checkHalt(String str) {
    if (str.equalsIgnoreCase("pause")) {
      pauseScriptExecution();
      if (scriptEditorVisible)
        scriptStatus("", "paused -- type RESUME to continue", 0, null);
      return true;
    }
    str = str.toLowerCase();
    if (str.startsWith("exit")) {
      haltScriptExecution();
      clearScriptQueue();
      if (isCmdLine_c_or_C_Option)
        Logger.info("exit -- stops script checking");
      isCmdLine_c_or_C_Option = false;
      return str.equals("exit");
    }
    if (str.startsWith("quit")) {
      haltScriptExecution();
      if (isCmdLine_c_or_C_Option)
        Logger.info("quit -- stops script checking");
      isCmdLine_c_or_C_Option = false;
      return str.equals("quit");
    }
    return false;
  }

  // / direct no-queue use:

  public String scriptWait(String strScript) {
    scriptManager.waitForQueue();
    boolean doTranslateTemp = GT.getDoTranslate();
    GT.setDoTranslate(false);
    String str = (String) evalStringWaitStatus("JSON", strScript,
        "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated", false,
        false, false);
    GT.setDoTranslate(doTranslateTemp);
    return str;
  }

  public Object scriptWaitStatus(String strScript, String statusList) {
    scriptManager.waitForQueue();
    boolean doTranslateTemp = GT.getDoTranslate();
    GT.setDoTranslate(false);
    Object ret = evalStringWaitStatus("object", strScript, statusList, false,
        false, false);
    GT.setDoTranslate(doTranslateTemp);
    return ret;
  }

  public Object evalStringWaitStatus(String returnType, String strScript,
                                     String statusList) {
    scriptManager.waitForQueue();
    return evalStringWaitStatus(returnType, strScript, statusList, false,
        false, false);
  }

  int scriptIndex;
  boolean isScriptQueued = true;

  synchronized Object evalStringWaitStatus(String returnType, String strScript,
                                           String statusList,
                                           boolean isScriptFile,
                                           boolean isQuiet, boolean isQueued) {
    // from the scriptManager or scriptWait()
    // System.out.println("DEBUG: evalStringWaitStatus " +
    // Thread.currentThread().getName()
    // + " " + Thread.currentThread().getId());
    if (strScript == null)
      return null;
    String str = checkScriptExecution(strScript);
    if (str != null)
      return str;

    // typically request:
    // "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated"
    // set up first with applet.jmolGetProperty("jmolStatus",statusList)
    // flush list
    String oldStatusList = statusManager.getStatusList();
    getProperty("String", "jmolStatus", statusList);
    if (isCmdLine_c_or_C_Option)
      Logger.info("--checking script:\n" + eval.getScript() + "\n----\n");
    boolean historyDisabled = (strScript.indexOf(")") == 0);
    if (historyDisabled)
      strScript = strScript.substring(1);
    historyDisabled = historyDisabled || !isQueued; // no history for scriptWait
                                                    // 11.5.45
    setErrorMessage(null);
    boolean isOK = (isScriptFile ? eval.compileScriptFile(strScript, isQuiet)
        : eval.compileScriptString(strScript, isQuiet));
    String strErrorMessage = eval.getErrorMessage();
    String strErrorMessageUntranslated = eval.getErrorMessageUntranslated();
    setErrorMessage(strErrorMessage, strErrorMessageUntranslated);
    if (isOK) {
      isScriptQueued = isQueued;
      if (!isQuiet)
        scriptStatus(null, strScript, -2 - (++scriptIndex), null);
      eval.evaluateCompiledScript(isCmdLine_c_or_C_Option, isCmdLine_C_Option,
          historyDisabled, listCommands);
      setErrorMessage(strErrorMessage = eval.getErrorMessage(),
          strErrorMessageUntranslated = eval.getErrorMessageUntranslated());
      if (!isQuiet)
        scriptStatus("Jmol script terminated", strErrorMessage, 1 + eval
            .getExecutionWalltime(), strErrorMessageUntranslated);
    } else {
      scriptStatus(strErrorMessage);
      scriptStatus("Jmol script terminated", strErrorMessage, 1,
          strErrorMessageUntranslated);
    }
    if (strErrorMessage != null && autoExit)
      exitJmol();      
    if (isCmdLine_c_or_C_Option) {
      if (strErrorMessage == null)
        Logger.info("--script check ok");
      else
        Logger.error("--script check error\n" + strErrorMessageUntranslated);
    }
    if (isCmdLine_c_or_C_Option)
      Logger.info("(use 'exit' to stop checking)");
    isScriptQueued = true;
    if (returnType.equalsIgnoreCase("String"))
      return strErrorMessageUntranslated;
    // get Vector of Vectors of Vectors info
    Object info = getProperty(returnType, "jmolStatus", statusList);
    // reset to previous status list
    getProperty("object", "jmolStatus", oldStatusList);
    return info;
  }

  void exitJmol() {
    Logger.debug("exitJmol -- exiting");
    System.out.flush();
    System.exit(0);
  }

  private Object scriptCheck(String strScript, boolean returnContext) {
    // from ConsoleTextPane.checkCommand() and applet Jmol.scriptProcessor()
    if (strScript.indexOf(")") == 0 || strScript.indexOf("!") == 0) // history disabled
      strScript = strScript.substring(1);
    ScriptContext sc = (new ScriptEvaluator(this)).checkScriptSilent(strScript);
    if (returnContext || sc.errorMessage == null)
      return sc;
    return sc.errorMessage;
  }

  public synchronized Object scriptCheck(String strScript) {
    return scriptCheck(strScript, false);
  }

  public boolean isScriptExecuting() {
    return eval.isScriptExecuting();
  }

  public void haltScriptExecution() {
    eval.haltExecution();
  }

  public void resumeScriptExecution() {
    eval.resumePausedExecution();
  }

  public void stepScriptExecution() {
    eval.stepPausedExecution();
  }

  public void pauseScriptExecution() {
    eval.pauseExecution();
  }

  String getDefaultLoadScript() {
    return global.defaultLoadScript;
  }

  String getLoadFormat() {
    return global.loadFormat;
  }

  public String getStandardLabelFormat() {
    return stateManager.getStandardLabelFormat();
  }

  public int getRibbonAspectRatio() {
    // mps
    return global.ribbonAspectRatio;
  }

  public float getSheetSmoothing() {
    // mps
    return global.sheetSmoothing;
  }

  public boolean getSsbondsBackbone() {
    return global.ssbondsBackbone;
  }

  public boolean getHbondsBackbone() {
    return global.hbondsBackbone;
  }

  public boolean getHbondsSolid() {
    return global.hbondsSolid;
  }

  public void setMarBond(short marBond) {
    global.bondRadiusMilliAngstroms = marBond;
    global.setParameterValue("bondRadiusMilliAngstroms", marBond);
    setShapeSize(JmolConstants.SHAPE_STICKS, marBond * 2, Float.NaN, BitSetUtil
        .setAll(getAtomCount()));
  }

  int hoverAtomIndex = -1;
  String hoverText;

  void hoverOn(int atomIndex, int modifiers) {
    if (eval != null && isScriptExecuting() || atomIndex == hoverAtomIndex
        || global.hoverDelayMs == 0)
      return;
    if (!isInSelectionSubset(atomIndex))
      return;
    loadShape(JmolConstants.SHAPE_HOVER);
    Atom atom;
    if (modifiers == MouseManager.SHIFT 
        && getPickingMode() == JmolConstants.PICKING_LABEL
        && (atom = modelSet.getAtomAt(atomIndex)) != null
        && atom.isShapeVisible(JmolConstants.getShapeVisibilityFlag(JmolConstants.SHAPE_LABELS))) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "specialLabel", GT.translate("Drag to move label"));
    }
    setShapeProperty(JmolConstants.SHAPE_HOVER, "text", null);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "target", new Integer(atomIndex));
    hoverText = null;
    hoverAtomIndex = atomIndex;
    refresh(3, "hover on atom");
  }

  int getHoverDelay() {
    return global.hoverDelayMs;
  }

  public void hoverOn(int x, int y, String text) {
    // from draw for drawhover on
    if (eval != null && isScriptExecuting())
      return;
    loadShape(JmolConstants.SHAPE_HOVER);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "xy", new Point3i(x, y, 0));
    setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "specialLabel", null);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "text", text);
    hoverAtomIndex = -1;
    hoverText = text;
    refresh(3, "hover on point");
  }

  void hoverOff() {
    if (hoverAtomIndex >= 0) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
      hoverAtomIndex = -1;
    }
    if (hoverText != null) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "text", null);
      hoverText = null;
    }
    setShapeProperty(JmolConstants.SHAPE_HOVER, "specialLabel", null);
    refresh(3, "hover off");
  }

  void setLabel(String strLabel) {
    modelSet.setLabel(strLabel, selectionManager.bsSelection);
  }

  void togglePickingLabel(BitSet bs) {
    // eval label toggle (atomset) and pickingManager
    if (bs == null)
      bs = selectionManager.bsSelection;
    loadShape(JmolConstants.SHAPE_LABELS);
    setShapeSize(JmolConstants.SHAPE_LABELS, 0, Float.NaN, bs);
    modelSet.setShapeProperty(JmolConstants.SHAPE_LABELS, "toggleLabel", null,
        bs);
  }

  BitSet getBitSetSelection() {
    return selectionManager.bsSelection;
  }

  public void clearShapes() {
    repaintManager.clear();
  }

  public void loadShape(int shapeID) {
    modelSet.loadShape(shapeID);
  }

  void setShapeSize(int shapeID, int size, float fsize) {
    // Eval - many
    // stateManager.setCrystallographicDefaults
    // Viewer - many
    setShapeSize(shapeID, size, fsize, selectionManager.bsSelection);
  }

  public void setShapeSize(int shapeID, int size, BitSet bsAtoms) {
    // deprecated
    setShapeSize(shapeID, size, Float.NaN, bsAtoms);
  }
  
  public void setShapeSize(int shapeID, int size, float fsize, BitSet bsAtoms) {
    // BondCollection.autoHbond()
    // ModelCollection.makeConnections()
    // Eval.configuration()
    // Eval.connect()
    // several points in Viewer
    modelSet.setShapeSize(shapeID, size, fsize, bsAtoms);
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value) {
    // Eval, BondCollection, StateManager, local
    if (shapeID < 0)
      return; // not applicable
    modelSet.setShapeProperty(shapeID, propertyName, value,
        selectionManager.bsSelection);
  }

  void setShapeProperty(int shapeID, String propertyName, Object value,
                        BitSet bs) {
    // Eval color
    if (shapeID < 0)
      return; // not applicable
    modelSet.setShapeProperty(shapeID, propertyName, value, bs);
  }

  void setShapePropertyArgb(int shapeID, String propertyName, int argb) {
    // Eval
    setShapeProperty(shapeID, propertyName, argb == 0 ? null : new Integer(
        argb | 0xFF000000));
  }

  public Object getShapeProperty(int shapeType, String propertyName) {
    return modelSet
        .getShapeProperty(shapeType, propertyName, Integer.MIN_VALUE);
  }

  public boolean getShapeProperty(int shapeType, String propertyName, Object[] data) {
    return modelSet.getShapeProperty(shapeType, propertyName, data);
  }

  Object getShapeProperty(int shapeType, String propertyName, int index) {
    return modelSet.getShapeProperty(shapeType, propertyName, index);
  }

  int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer) ? Integer.MIN_VALUE
        : ((Integer) value).intValue();
  }

  short getColix(Object object) {
    return Graphics3D.getColix(object);
  }

  boolean getRasmolHydrogenSetting() {
    return global.rasmolHydrogenSetting;
  }

  boolean getRasmolHeteroSetting() {
    return global.rasmolHeteroSetting;
  }

  boolean getDebugScript() {
    return global.debugScript;
  }

  public void setDebugScript(boolean debugScript) {
    global.debugScript = debugScript;
    global.setParameterValue("debugScript", debugScript);
    eval.setDebugging();
  }

  void atomPicked(int atomIndex, Point3fi ptClicked, int modifiers,
                  boolean isDoubleClick) {
    pickingManager.atomPicked(atomIndex, ptClicked, modifiers, isDoubleClick);
  }

  void clearClickCount() {
    // mouseManager.clearClickCount();
    setTainted(true);
  }

  public final static int CURSOR_DEFAULT = 0;
  public final static int CURSOR_HAND = 1;
  public final static int CURSOR_CROSSHAIR = 2;
  public final static int CURSOR_MOVE = 3;
  public final static int CURSOR_WAIT = 4;

  private int currentCursor = CURSOR_DEFAULT;

  public void setCursor(int cursor) {
    if (currentCursor == cursor || display == null)
      return;
    int c;
    switch (currentCursor = cursor) {
    case CURSOR_HAND:
      c = Cursor.HAND_CURSOR;
      break;
    case CURSOR_MOVE:
      c = Cursor.MOVE_CURSOR;
      break;
    case CURSOR_CROSSHAIR:
      c = Cursor.CROSSHAIR_CURSOR;
      break;
    case CURSOR_WAIT:
      c = Cursor.WAIT_CURSOR;
      break;
    default:
      display.setCursor(Cursor.getDefaultCursor());
      return;
    }
    display.setCursor(Cursor.getPredefinedCursor(c));
  }

  private void setPickingMode(String mode) {
    int pickingMode = JmolConstants.getPickingMode(mode);
    if (pickingMode < 0)
      pickingMode = JmolConstants.PICKING_IDENT;
    pickingManager.setPickingMode(pickingMode);
    global.setParameterValue("picking", JmolConstants
        .getPickingModeName(pickingManager.getPickingMode()));
  }

  public int getPickingMode() {
    return pickingManager.getPickingMode();
  }

  public boolean getDrawPicking() {
    return global.drawPicking;
  }

  public boolean getBondPicking() {
    return global.bondPicking;
  }

  private boolean getAtomPicking() {
    return global.atomPicking;
  }

  private void setPickingStyle(String style) {
    int pickingStyle = JmolConstants.getPickingStyle(style);
    if (pickingStyle < 0)
      pickingStyle = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
    pickingManager.setPickingStyle(pickingStyle);
    global.setParameterValue("pickingStyle", JmolConstants
        .getPickingStyleName(pickingManager.getPickingStyle()));
  }

  int getPickingStyle() {
    return pickingManager.getPickingStyle();
  }

  public boolean getDrawHover() {
    return pickingManager.getDrawHover();
  }

  public String getAtomInfo(int atomIndex) {
    // only for MeasurementTable and PickingManager
    return (atomIndex >= 0 ? modelSet.getAtomInfo(atomIndex, null)
        : (String) modelSet.getShapeProperty(JmolConstants.SHAPE_MEASURES,
            "pointInfo", -atomIndex));
  }

  public String getAtomInfoXYZ(int atomIndex, boolean useChimeFormat) {
    return modelSet.getAtomInfoXYZ(atomIndex, useChimeFormat);
  }

  // //////////////status manager dispatch//////////////

  public void setJmolCallbackListener(JmolCallbackListener jmolCallbackListener) {
    statusManager.setJmolCallbackListener(jmolCallbackListener);
  }

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    statusManager.setJmolStatusListener(jmolStatusListener, null);
  }

  public Hashtable getMessageQueue() {
    // called by PropertyManager.getPropertyAsObject for "messageQueue"
    return statusManager.getMessageQueue();
  }

  Object getStatusChanged(String statusNameList) {
    return statusManager.getStatusChanged(statusNameList);
  }

  void popupMenu(int x, int y) {
    if (isPreviewOnly || global.disablePopupMenu)
      return;
    if (jmolpopup == null)
      jmolpopup = JmolPopup.newJmolPopup(this, true, menuStructure, true);
    jmolpopup.show(x, y);
  }

  String getMenu(String type) {
    return (jmolpopup == null ? "" : jmolpopup.getMenu("Jmol version "
            + Viewer.getJmolVersion() + "|_GET_MENU|" + type));
  }

  void setMenu(String fileOrText, boolean isFile) {
    if (isFile)
      Logger.info("Setting menu "
          + (fileOrText.length() == 0 ? "to Jmol defaults" : "from file "
              + fileOrText));
    if (fileOrText.length() == 0)
      fileOrText = null;
    else if (isFile)
      fileOrText = getFileAsString(fileOrText);
    getProperty("DATA_API", "setMenu", fileOrText);
    statusManager.setCallbackFunction("menu", fileOrText);
  }

  // // JavaScript callback methods for the applet

  /*
   * 
   * animFrameCallback echoCallback (defaults to messageCallback) errorCallback
   * evalCallback hoverCallback loadStructCallback measureCallback (defaults to
   * messageCallback) messageCallback (no local version) minimizationCallback
   * pickCallback resizeCallback scriptCallback (defaults to messageCallback)
   * syncCallback
   */

  /*
   * animFrameCallback is called:
   * 
   * -- each time a frame is changed -- whenever the animation state is changed
   * -- whenever the visible frame range is changed
   * 
   * jmolSetCallback("animFrameCallback", "myAnimFrameCallback") function
   * myAnimFrameCallback(frameNo, fileNo, modelNo, firstNo, lastNo) {}
   * 
   * frameNo == the current frame in fileNo == the current file number, starting
   * at 1 modelNo == the current model number in the current file, starting at 1
   * firstNo == flag1 * (the first frame of the set, in file * 1000000 + model
   * notation) lastNo == flag2 * (the last frame of the set, in file * 1000000 +
   * model notation)
   * 
   * where flag1 = 1 if animationDirection > 1 or -1 otherwise where flag2 = 1
   * if currentDirection > 1 or -1 otherwise
   * 
   * RepaintManager.setStatusFrameChanged RepaintManager.setAnimationOff
   * RepaintManager.setCurrentModelIndex RepaintManager.clearAnimation
   * RepaintManager.rewindAnimation RepaintManager.setAnimationLast
   * RepaintManager.setAnimationRelative RepaintManager.setFrameRangeVisible
   * Viewer.setCurrentModelIndex Eval.file Eval.frame Eval.load
   * Viewer.createImage (when creating movie frames with the WRITE FRAMES
   * command) Viewer.initializeModel
   */

  int prevFrame = Integer.MIN_VALUE;

  void setStatusFrameChanged(int frameNo) {
    int modelIndex = animationManager.currentModelIndex;
    if (frameNo == Integer.MIN_VALUE) {
      // force reset (reading vibrations)
      prevFrame = Integer.MIN_VALUE;
      frameNo = modelIndex;
    }
    transformManager.setVibrationPeriod(Float.NaN);

    int firstIndex = animationManager.firstModelIndex;
    int lastIndex = animationManager.lastModelIndex;

    if (firstIndex == lastIndex)
      modelIndex = firstIndex;
    int frameID = getModelFileNumber(modelIndex);
    int fileNo = frameID;
    int modelNo = frameID % 1000000;
    int firstNo = getModelFileNumber(firstIndex);
    int lastNo = getModelFileNumber(lastIndex);
    String strModelNo;
    if (fileNo == 0) {
      strModelNo = getModelNumberDotted(firstIndex);
      if (firstIndex != lastIndex)
        strModelNo += " - " + getModelNumberDotted(lastIndex);
      if (firstNo / 1000000 == lastNo / 1000000)
        fileNo = firstNo;
    } else {
      strModelNo = getModelNumberDotted(modelIndex);
    }
    if (fileNo != 0)
      fileNo = (fileNo < 1000000 ? 1 : fileNo / 1000000);

    global.setParameterValue("_currentFileNumber", fileNo);
    global.setParameterValue("_currentModelNumberInFile", modelNo);
    global.setParameterValue("_frameID", frameID);
    global.setParameterValue("_modelNumber", strModelNo);
    global.setParameterValue("_modelName", (modelIndex < 0 ? ""
        : getModelName(modelIndex)));
    global.setParameterValue("_modelTitle", (modelIndex < 0 ? ""
        : getModelTitle(modelIndex)));
    global.setParameterValue("_modelFile", (modelIndex < 0 ? ""
        : getModelFileName(modelIndex)));

    if (modelIndex == prevFrame) {
      return;
    }
    prevFrame = modelIndex;

    statusManager.setStatusFrameChanged(frameNo, fileNo, modelNo,
        (animationManager.animationDirection < 0 ? -firstNo : firstNo),
        (animationManager.currentDirection < 0 ? -lastNo : lastNo));
  }

  /*
   * echoCallback is one of the two main status reporting mechanisms. Along with
   * scriptCallback, it outputs to the console. Unlike scriptCallback, it does
   * not output to the status bar of the application or applet. If
   * messageCallback is enabled but not echoCallback, these messages go to the
   * messageCallback function instead.
   * 
   * jmolSetCallback("echoCallback", "myEchoCallback") function
   * myEchoCallback(app, message, queueState) {}
   * 
   * queueState = 1 -- queued queueState = 0 -- not queued
   * 
   * serves:
   * 
   * Eval.instructionDispatchLoop when app has -l flag
   * ForceField.steepestDescenTakeNSteps for minimization done
   * Viewer.setPropertyError Viewer.setBooleanProperty error
   * Viewer.setFloatProperty error Viewer.setIntProperty error
   * Viewer.setStringProperty error Viewer.showString adds a Logger.warn()
   * message Eval.showString calculate, cd, dataFrame, echo, error, getProperty,
   * history, isosurface, listIsosurface, pointGroup, print, set, show, write
   * ForceField.steepestDescentInitialize for initial energy
   * ForceField.steepestDescentTakeNSteps for minimization update
   * Viewer.showParameter
   */

  public void scriptEcho(String strEcho) {
    statusManager.setScriptEcho(strEcho, isScriptQueued);
    if (listCommands && strEcho != null && strEcho.indexOf("$[") == 0)
      Logger.info(strEcho);
  }

  /*
   * errorCallback is a special callback that can be used to identify errors
   * during scripting and file i/o, and also indicate out of memory conditions
   * 
   * jmolSetCallback("errorCallback", "myErrorCallback") function
   * myErrorCallback(app, errType, errMsg, objectInfo, errMsgUntranslated) {}
   * 
   * errType == "Error" or "ScriptException" errMsg == error message, possibly
   * translated, with added information objectInfo == which object (such as an
   * isosurface) was involved errMsgUntranslated == just the basic message
   * 
   * Viewer.notifyError Eval.runEval on Error and file loading Exceptions
   * Viewer.handleError Eval.runEval on OOM Error Viewer.createModelSet on OOM
   * model initialization Error Viewer.getImage on OOM rendering Error
   */
  void notifyError(String errType, String errMsg, String errMsgUntranslated) {
    statusManager.notifyError(errType, errMsg, errMsgUntranslated);
  }

  /*
   * evalCallback is a special callback that evaluates expressions in JavaScript
   * rather than in Jmol.
   * 
   * Viewer.jsEval Eval.loadScriptFileInternal Eval.Rpn.evaluateScript
   * Eval.script
   */

  String jsEval(String strEval) {
    return statusManager.jsEval(strEval);
  }

  /*
   * hoverCallback reports information about the atom being hovered over.
   * 
   * jmolSetCallback("hoverCallback", "myHoverCallback") function
   * myHoverCallback(strInfo, iAtom) {}
   * 
   * strInfo == the atom's identity, including x, y, and z coordinates iAtom ==
   * the index of the atom being hovered over
   * 
   * Viewer.setStatusAtomHovered Hover.setProperty("target") Viewer.hoverOff
   * Viewer.hoverOn
   */

  public void setStatusAtomHovered(int atomIndex, String info) {
    global.setParameterValue("_atomhovered", atomIndex);
    statusManager.setStatusAtomHovered(atomIndex, info);
  }

  /*
   * loadStructCallback indicates file load status.
   * 
   * jmolSetCallback("loadStructCallback", "myLoadStructCallback") function
   * myLoadStructCallback(fullPathName, fileName, modelName, errorMsg, ptLoad)
   * {}
   * 
   * ptLoad == FILE_STATUS_NOT_LOADED == -1 ptLoad == FILE_STATUS_ZAPPED == 0
   * ptLoad == FILE_STATUS_CREATING_MODELSET == 2 ptLoad ==
   * FILE_STATUS_MODELSET_CREATED == 3 ptLoad == FILE_STATUS_MODELS_DELETED == 5
   * 
   * Only -1 (error loading), 0 (zapped), and 3 (model set created) messages are
   * passed on to the callback function. The others can be detected using
   * 
   * set loadStructCallback "jmolscript:someFunctionName"
   * 
   * At the time of calling of that method, the jmolVariable _loadPoint gives
   * the value of ptLoad. These load points are also recorded in the status
   * queue under types "fileLoaded" and "fileLoadError".
   * 
   * Viewer.setFileLoadStatus Viewer.createModelSet (2, 3)
   * Viewer.createModelSetAndReturnError (-1, 1, 4) Viewer.deleteAtoms (5)
   * Viewer.zap (0)
   */
  private void setFileLoadStatus(int ptLoad, String fullPathName,
                                 String fileName, String modelName,
                                 String strError) {
    setErrorMessage(strError);
    global.setParameterValue("_loadPoint", ptLoad);
    boolean doCallback = (ptLoad == FILE_STATUS_MODELSET_CREATED
        || ptLoad == FILE_STATUS_ZAPPED || ptLoad == FILE_STATUS_NOT_LOADED);
    statusManager.setFileLoadStatus(fullPathName, fileName, modelName,
        strError, ptLoad, doCallback);
  }

  /*
   * measureCallback reports completed or pending measurements. Pending
   * measurements are measurements that the user has started but has not
   * completed -- this call comes when the user hesitates with the mouse over an
   * atom and the "rubber band" is showing
   * 
   * jmolSetCallback("measureCallback", "myMeasureCallback") function
   * myMeasureCallback(strMeasure, intInfo, status) {}
   * 
   * intInfo == (see below) status == "measurePicked" (intInfo == the number of
   * atoms in the measurement) "measureComplete" (intInfo == the current number
   * measurements) "measureDeleted" (intInfo == the index of the measurement
   * deleted or -1 for all) "measurePending" (intInfo == number of atoms picked
   * so far)
   * 
   * strMeasure:
   * 
   * For "set picking MEASURE ..." each time the user clicks an atom, a message
   * is sent to the pickCallback function (see below), and if the picking is set
   * to measure distance, angle, or torsion, then after the requisite number of
   * atoms is picked and the pick callback message is sent, a call is also made
   * to measureCallback with a string that indicates the measurement, such as:
   * 
   * Angle O #9 - Si #7 - O #2 : 110.51877
   * 
   * Under default conditions, when picking is not set to MEASURE, then
   * measurement reports are sent when the measure is completed, deleted, or
   * pending. These reports are in a psuedo array form that can be parsed more
   * easily, involving the atoms and measurement with units, for example:
   * 
   * [Si #3, O #8, Si #7, 60.1 <degrees mark>]
   * 
   * Viewer.setStatusMeasuring Measures.clear Measures.define
   * Measures.deleteMeasurement Measures.pending PickingManager.atomPicked
   */

  public void setStatusMeasuring(String status, int intInfo, String strMeasure) {
    // measurement completed or pending or picked
    statusManager.setStatusMeasuring(status, intInfo, strMeasure);
  }

  /*
   * minimizationCallback reports the status of a currently running
   * minimization.
   * 
   * jmolSetCallback("minimizationCallback", "myMinimizationCallback") function
   * myMinimizationCallback(app, minStatus, minSteps, minEnergy, minEnergyDiff)
   * {}
   * 
   * minStatus is one of "starting", "calculate", "running", "failed", or "done"
   * 
   * Viewer.notifyMinimizationStatus Minimizer.endMinimization
   * Minimizer.getEnergyonly Minimizer.startMinimization
   * Minimizer.stepMinimization
   */

  public void notifyMinimizationStatus() {
    Object step =  getParameter("_minimizationStep");
    statusManager.notifyMinimizationStatus(
        (String) getParameter("_minimizationStatus"),
        step instanceof String ? new Integer(0) : (Integer) step,
        (Float) getParameter("_minimizationEnergy"),
        (Float) getParameter("_minimizationEnergyDiff"));
  }

  /*
   * pickCallback returns information about an atom, bond, or DRAW object that
   * has been picked by the user.
   * 
   * jmolSetCallback("pickCallback", "myPickCallback") function
   * myPickCallback(strInfo, iAtom) {}
   * 
   * iAtom == the index of the atom picked or -2 for a draw object or -3 for a
   * bond
   * 
   * strInfo depends upon the type of object picked:
   * 
   * atom: a string determinied by the PICKLABEL parameter, which if "" delivers
   * the atom identity along with its coordinates
   * 
   * bond: ["bond", bondIdentityString (quoted), x, y, z] where the coordinates
   * are of the midpoint of the bond
   * 
   * draw: ["draw", drawID(quoted), pickedModel, pickedVertex, x, y, z,
   * drawTitle(quoted)]
   * 
   * Viewer.setStatusAtomPicked Draw.checkObjectClicked (set picking DRAW)
   * Sticks.checkObjectClicked (set bondPicking TRUE; set picking IDENTIFY)
   * PickingManager.atomPicked (set atomPicking TRUE; set picking IDENTIFY)
   * PickingManager.queueAtom (during measurements)
   */

  public void setStatusAtomPicked(int atomIndex, String info) {
    if (info == null) {
      info = global.pickLabel;
      if (info.length() == 0)
        info = getAtomInfoXYZ(atomIndex, getMessageStyleChime());
      else
        info = modelSet.getAtomInfo(atomIndex, info);
    }
    global.setParameterValue("_atompicked", atomIndex);
    global.setParameterValue("_pickinfo", info);
    statusManager.setStatusAtomPicked(atomIndex, info);
  }

  /*
   * resizeCallback is called whenever the applet gets a resize notification
   * from the browser
   * 
   * jmolSetCallback("resizeCallback", "myResizeCallback") function
   * myResizeCallback(width, height) {}
   */

  public void setStatusResized(int width, int height) {
    statusManager.setStatusResized(width, height);
  }

  /*
   * scriptCallback is the primary way to monitor script status. In addition, it
   * serves to for passing information to the user over the status line of the
   * browser as well as to the console. Note that console messages are also sent
   * by echoCallback. If messageCallback is enabled but not scriptCallback,
   * these messages go to the messageCallback function instead.
   * 
   * jmolSetCallback("scriptCallback", "myScriptCallback") function
   * myScriptCallback(app, status, message, intStatus, errorMessageUntranslated)
   * {}
   * 
   * intStatus == -2 script start -- message is the script itself intStatus == 0
   * general messages during script execution; translated error message may be
   * present intStatus >= 1 script termination message; translated and
   * untranslated message may be present value is time for execution in
   * milliseconds
   * 
   * Eval.defineAtomSet -- compilation bug indicates problem in JmolConstants
   * array Eval.instructionDispatchLoop -- debugScript messages
   * Eval.logDebugScript -- debugScript messages Eval.pause -- script execution
   * paused message Eval.runEval -- "Script completed" message Eval.script --
   * Chime "script <exiting>" message Eval.scriptStatusOrBuffer -- various
   * messages for Eval.checkContinue (error message) Eval.connect Eval.delete
   * Eval.hbond Eval.load (logMessages message) Eval.message Eval.runEval (error
   * message) Eval.write (error reading file) Eval.zap (error message)
   * FileManager.createAtomSetCollectionFromFile "requesting..." for Chime-like
   * compatibility PickingManager.atomPicked
   * "pick one more atom in order to spin..." for example
   * Viewer.evalStringWaitStatus -- see above -2, 0 only if error, >=1 at
   * termination Viewer.reportSelection "xxx atoms selected"
   */

  void scriptStatus(String strStatus) {
    scriptStatus(strStatus, "", 0, null);
  }

  void scriptStatus(String strStatus, String statusMessage) {
    scriptStatus(strStatus, statusMessage, 0, null);
  }

  void scriptStatus(String strStatus, String statusMessage,
                            int msWalltime, String strErrorMessageUntranslated) {
    statusManager.setScriptStatus(strStatus, statusMessage, msWalltime,
        strErrorMessageUntranslated);
  }

  /*
   * syncCallback traps script synchronization messages and allows for
   * cancellation (by returning "") or modification
   * 
   * jmolSetCallback("syncCallback", "mySyncCallback") function
   * mySyncCallback(app, script, appletName) { ...[modify script here]... return
   * newScript }
   * 
   * StatusManager.syncSend Viewer.setSyncTarget Viewer.syncScript
   */

  // //////////

  private String getModelTitle(int modelIndex) {
    // necessary for status manager frame change?
    return modelSet == null ? null : modelSet.getModelTitle(modelIndex);
  }

  private String getModelFileName(int modelIndex) {
    // necessary for status manager frame change?
    return modelSet == null ? null : modelSet.getModelFileName(modelIndex);
  }

  public String dialogAsk(String type, String fileName) {
    return statusManager.dialogAsk(type, fileName);
  }

  int getScriptDelay() {
    return global.scriptDelay;
  }

  public void showUrl(String urlString) {
    // applet.Jmol
    // app Jmol
    // StatusManager
    if (urlString == null)
      return;
    if (urlString.indexOf(":") < 0) {
      String base = fileManager.getAppletDocumentBase();
      if (base == "")
        base = fileManager.getFullPathName();
      if (base.indexOf("/") >= 0) {
        base = base.substring(0, base.lastIndexOf("/") + 1);
      } else if (base.indexOf("\\") >= 0) {
        base = base.substring(0, base.lastIndexOf("\\") + 1);
      }
      urlString = base + urlString;
    }
    Logger.info("showUrl:" + urlString);
    statusManager.showUrl(urlString);
  }

  /**
   * an external applet or app with class that extends org.jmol.jvxl.MeshCreator might execute:
   * 
   * org.jmol.viewer.Viewer viewer = applet.getViewer();
   * viewer.setMeshCreator(this);
   *
   * then that class's updateMesh(String id) method will be called
   * whenever a mesh is rendered. 
   * 
   * @param meshCreator
   */
  public void setMeshCreator(Object meshCreator) {
    loadShape(JmolConstants.SHAPE_ISOSURFACE);
    setShapeProperty(JmolConstants.SHAPE_ISOSURFACE, "meshCreator", meshCreator);
  }
  
  void showConsole(boolean showConsole) {
    // Eval
      if (appConsole == null)
        getProperty("DATA_API", "getAppConsole", Boolean.TRUE);
      appConsole.setVisible(showConsole);
  }

  void clearConsole() {
    // Eval
    statusManager.clearConsole();
  }

  Object getParameterEscaped(String key) {
    return global.getParameterEscaped(key, 0);
  }

  public Object getParameter(String key) {
    return global.getParameter(key);
  }

  ScriptVariable getOrSetNewVariable(String key, boolean doSet) {
    return global.getOrSetNewVariable(key, doSet);
  }

  ScriptVariable setUserVariable(String name, ScriptVariable value) {
    return global.setUserVariable(name, value);
  }

  void unsetProperty(String name) {
    global.unsetUserVariable(name);
  }
  
  String getVariableList() {
    return global.getVariableList();
  }

  public boolean getBooleanProperty(String key) {
    return getBooleanProperty(key, true);
  }

  /*****************************************************************************
   * @param key
   * @param doICare
   *          true if you want an error message if it doesn't exist
   * @return the boolean property mth 2005 06 24 and/or these property names
   *         should be interned strings so that we can just do == comparisions
   *         between strings
   ****************************************************************************/

  public boolean getBooleanProperty(String key, boolean doICare) {
    // JmolPopup
    key = key.toLowerCase();
    if (global.htPropertyFlags.containsKey(key)) {
      return ((Boolean) global.htPropertyFlags.get(key)).booleanValue();
    }
    // special cases
    if (key.equalsIgnoreCase("executionPaused"))
      return eval.isExecutionPaused();
    if (key.equalsIgnoreCase("executionStepping"))
      return eval.isExecutionStepping();
    if (key.equalsIgnoreCase("haveBFactors"))
      return (modelSet.getBFactors() != null);
    if (key.equalsIgnoreCase("colorRasmol"))
      return colorManager.getDefaultColorRasmol();
    if (key.equalsIgnoreCase("frank"))
      return getShowFrank();
    if (key.equalsIgnoreCase("showSelections"))
      return getSelectionHaloEnabled();
    if (global.htUserVariables.containsKey(key)) {
      ScriptVariable t = global.getUserVariable(key);
      if (t.tok == Token.on)
        return true;
      if (t.tok == Token.off)
        return false;
    }
    if (doICare)
      Logger.error("viewer.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  public void setStringProperty(String key, String value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }
    // Eval
    boolean notFound = false;
    while (true) {

      // 11.7.7
      if (key.equalsIgnoreCase("atomTypes")) {
        global.atomTypes = value;
        break;
      }

      // /11.6.RC15
      if (key.equalsIgnoreCase("currentLocalPath"))
        break;
      // /11.5.42
      // pickLabel
      if (key.equalsIgnoreCase("pickLabel")) {
        global.pickLabel = value;
        break;
      }

      // /11.5.39//
      if (key.equalsIgnoreCase("quaternionFrame")) {
        if (value.length() == 2 && value.startsWith("R"))
          // C, P -- straightness from Ramachandran angles
          global.quaternionFrame = value.substring(0, 2);
        else
          global.quaternionFrame = "" + (value.toLowerCase() + "p").charAt(0);
        if (!Parser.isOneOf(global.quaternionFrame, "a;n;c;p;q;RC;RP"))
          global.quaternionFrame = "p";
        modelSet.setHaveStraightness(false);
        break;
      }

      // /11.5.11//
      if (key.equalsIgnoreCase("defaultVDW")) {
        setDefaultVdw(value);
        return;
      }

      // /11.1.30//
      if (key.equalsIgnoreCase("language")) {
        // fr cs en none, etc.
        setLanguage(value);
        value = GT.getLanguage();
        break;
      }

      // /11.1.22//

      if (key.equalsIgnoreCase("loadFormat")) {
        global.loadFormat = value;
        break;
      }

      // /11.1///

      if (key.equalsIgnoreCase("backgroundColor")) {
        setObjectColor("background", value);
        return;
      }

      if (key.equalsIgnoreCase("axesColor")) {
        setObjectColor("axis1", value);
        setObjectColor("axis2", value);
        setObjectColor("axis3", value);
        return;
      }

      if (key.equalsIgnoreCase("axis1Color")) {
        setObjectColor("axis1", value);
        return;
      }

      if (key.equalsIgnoreCase("axis2Color")) {
        setObjectColor("axis2", value);
        return;
      }

      if (key.equalsIgnoreCase("axis3Color")) {
        setObjectColor("axis3", value);
        return;
      }

      if (key.equalsIgnoreCase("boundBoxColor")) {
        setObjectColor("boundbox", value);
        return;
      }

      if (key.equalsIgnoreCase("unitCellColor")) {
        setObjectColor("unitcell", value);
        return;
      }

      if (key.equalsIgnoreCase("propertyColorScheme")) {
        setPropertyColorScheme(value, false);
        return;
      }

      if (key.equalsIgnoreCase("propertyColorSchemeOverload")) {
        setPropertyColorScheme(value, true);
        return;
      }

      if (key.equalsIgnoreCase("hoverLabel")) {
        setShapeProperty(JmolConstants.SHAPE_HOVER, "atomLabel", value);
        break;
      }
      // /11.0///
      if (key.equalsIgnoreCase("defaultDistanceLabel")) {
        global.defaultDistanceLabel = value;
        break;
      }
      if (key.equalsIgnoreCase("defaultAngleLabel")) {
        global.defaultAngleLabel = value;
        break;
      }
      if (key.equalsIgnoreCase("defaultTorsionLabel")) {
        global.defaultTorsionLabel = value;
        break;
      }
      if (key.equalsIgnoreCase("defaultLoadScript")) {
        global.defaultLoadScript = value;
        break;
      }
      if (key.equalsIgnoreCase("appletProxy")) {
        fileManager.setAppletProxy(value);
        break;
      }
      if (key.equalsIgnoreCase("defaultDirectory")) {
        if (value == null)
          value = "";
        value = value.replace('\\', '/');
        global.defaultDirectory = value;
        break;
      }
      if (key.equalsIgnoreCase("helpPath")) {
        global.helpPath = value;
        break;
      }
      if (key.equalsIgnoreCase("defaults")) {
        setDefaults(value);
        break;
      }
      if (key.equalsIgnoreCase("defaultColorScheme")) {
        setDefaultColors(value);
        break;
      }
      if (key.equalsIgnoreCase("picking")) {
        setPickingMode(value);
        return;
      }
      if (key.equalsIgnoreCase("pickingStyle")) {
        setPickingStyle(value);
        return;
      }
      if (key.equalsIgnoreCase("dataSeparator")) {
        // just saving this
        break;
      }
      if (key.toLowerCase().indexOf("callback") >= 0) {
        statusManager.setCallbackFunction(key, (value.length() == 0
            || value.equalsIgnoreCase("none") ? null : value));
        break;
      }
      notFound = true;
      break;
    }
    key = key.toLowerCase();
    boolean isJmol = global.htParameterValues.containsKey(key);
    if (!isJmol && notFound && key.charAt(0) != '@') {
      // not found -- @ is a silent mode indicator
      if (global.htPropertyFlags.containsKey(key)
          || global.htPropertyFlagsRemoved.containsKey(key)) {
        setPropertyError(GT.translate(
            "ERROR: cannot set boolean flag to string value: {0}", key));
        return;
      }
    }
    if (isJmol)
      global.setParameterValue(key, value);
    else
      global.setUserVariable(key, new ScriptVariable(Token.string, value));
  }

  private String language = GT.getLanguage();
  public String getLanguage() {
    return language;
  }
  
  private void setLanguage(String value) {
    //also serves to change language for callbacks and menu
      new GT(value);
      language = GT.getLanguage();
      if (jmolpopup != null)
        jmolpopup = JmolPopup.newJmolPopup(this, true, menuStructure, true);
      statusManager.setCallbackFunction("language", language);
  }

  private void setPropertyError(String msg) {
    Logger.error(msg);
    scriptEcho(msg);
  }

  void removeUserVariable(String key) {
    global.removeUserVariable(key);
    if (key.indexOf("callback") >= 0)
      statusManager.setCallbackFunction(key, null);
  }

  boolean isJmolVariable(String key) {
    return global.isJmolVariable(key);
  }

  public void setFloatProperty(String key, float value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }
    setFloatProperty(key, value, false);
  }

  private boolean setFloatProperty(String key, float value, boolean isInt) {
    // Eval
    boolean notFound = false;
    while (true) {
      // 11.7.47
      if (key.equalsIgnoreCase("navX")) {
        setSpin("X", (int) value);
        break;
      }
      if (key.equalsIgnoreCase("navY")) {
        setSpin("Y", (int) value);
        break;
      }
      if (key.equalsIgnoreCase("navZ")) {
        setSpin("Z", (int) value);
        break;
      }
      if (key.equalsIgnoreCase("navFPS")) {
        if (Float.isNaN(value))
          return true;
        setSpin("FPS", (int) value);
        break;
      }

      // 11.7.9
      if (key.equalsIgnoreCase("hbondsAngleMinimum")) {
        global.hbondsAngleMinimum = value;
        break;
      }

      // 11.7.9
      if (key.equalsIgnoreCase("hbondsDistanceMaximum")) {
        global.hbondsDistanceMaximum = value;
        break;
      }

      // 11.6.RC2//
      if (key.equalsIgnoreCase("pointGroupDistanceTolerance")) {
        global.pointGroupDistanceTolerance = value;
        break;
      }
      if (key.equalsIgnoreCase("pointGroupLinearTolerance")) {
        global.pointGroupLinearTolerance = value;
        break;
      }

      // 11.5.30//
      if (key.equalsIgnoreCase("ellipsoidAxisDiameter")) {
        if (isInt)
          value = value / 1000;
        break;
      }

      // /11.3.52//
      if (key.equalsIgnoreCase("spinX")) {
        setSpin("x", (int) value);
        break;
      }
      if (key.equalsIgnoreCase("spinY")) {
        setSpin("y", (int) value);
        break;
      }
      if (key.equalsIgnoreCase("spinZ")) {
        setSpin("z", (int) value);
        break;
      }
      if (key.equalsIgnoreCase("spinFPS")) {
        setSpin("fps", (int) value);
        break;
      }

      // /11.3.17//

      if (key.equalsIgnoreCase("defaultDrawArrowScale")) {
        setDefaultDrawArrowScale(value);
        break;
      }

      // /11.1///
      if (key.equalsIgnoreCase("defaultTranslucent")) {
        global.defaultTranslucent = value;
        break;
      }

      if (key.equalsIgnoreCase("axesScale")) {
        setAxesScale(value);
        break;
      }
      if (key.equalsIgnoreCase("visualRange")) {
        setVisualRange(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationDepth")) {
        setNavigationDepthPercent(0, value);
        break;
      }
      if (key.equalsIgnoreCase("navigationSpeed")) {
        global.navigationSpeed = value;
        break;
      }
      if (key.equalsIgnoreCase("navigationSlab")) {
        transformManager.setNavigationSlabOffsetPercent(value);
        break;
      }
      if (key.equalsIgnoreCase("cameraDepth")) {
        transformManager.setCameraDepthPercent(value);
        refresh(1, "set cameraDepth");
        break;
      }
      if (key.equalsIgnoreCase("rotationRadius")) {
        setRotationRadius(value, true);
        return true;
      }
      if (key.equalsIgnoreCase("hoverDelay")) {
        global.hoverDelayMs = (int) (value * 1000);
        break;
      }
      // /11.0///
      if (key.equalsIgnoreCase("sheetSmoothing")) {
        global.sheetSmoothing = value;
        break;
      }
      if (key.equalsIgnoreCase("dipoleScale")) {
        global.dipoleScale = value;
        break;
      }
      if (key.equalsIgnoreCase("stereoDegrees")) {
        transformManager.setStereoDegrees(value);
        break;
      }
      if (key.equalsIgnoreCase("vectorScale")) {
        // public -- no need to set
        setVectorScale(value);
        return true;
      }
      if (key.equalsIgnoreCase("vibrationPeriod")) {
        // public -- no need to set
        setVibrationPeriod(value);
        return true;
      }
      if (key.equalsIgnoreCase("vibrationScale")) {
        // public -- no need to set
        setVibrationScale(value);
        return true;
      }
      if (key.equalsIgnoreCase("bondTolerance")) {
        setBondTolerance(value);
        return true;
      }
      if (key.equalsIgnoreCase("minBondDistance")) {
        setMinBondDistance(value);
        return true;
      }
      if (key.equalsIgnoreCase("scaleAngstromsPerInch")) {
        transformManager.setScaleAngstromsPerInch(value);
        break;
      }
      if (key.equalsIgnoreCase("solventProbeRadius")) {
        global.solventProbeRadius = value;
        break;
      }
      if (key.equalsIgnoreCase("radius")) { // deprecated
        setFloatProperty("solventProbeRadius", value);
        return true;
      }
      // not found
      if (isInt)
        return false;
      notFound = true;
      break;
    }
    key = key.toLowerCase();
    boolean isJmol = global.htParameterValues.containsKey(key);
    if (!isJmol && notFound) {
      if (global.htPropertyFlags.containsKey(key)) {
        setPropertyError(GT.translate(
            "ERROR: cannot set boolean flag to numeric value: {0}", key));
        return true;
      }
    }
    if (isJmol)
      global.setParameterValue(key, value);
    else
      global.setUserVariable(key, new ScriptVariable(Token.decimal, new Float(value)));
    return true;
  }

  public void setIntProperty(String key, int value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }

    // Eval
    setIntProperty(key, value, true);
  }

  private void setIntProperty(String key, int value, boolean defineNew) {
    boolean notFound = false;
    while (true) {

      // 11.8.RC3//
      if (key.equalsIgnoreCase("helixStep")) {
        global.helixStep = value;
        modelSet.setHaveStraightness(false);
        break;
      }

      // 11.6.RC16//
      if (key.equalsIgnoreCase("propertyDataColumnCount")) {
        break;
      }

      if (key.equalsIgnoreCase("propertyAtomNumberColumnCount")) {
        break;
      }

      // 11.6.RC2//
      if (key.equalsIgnoreCase("dotDensity")) {
        global.dotDensity = value;
        break;
      }

      // 11.5.30//
      // ellipsoidDotCount" just handled as getParameter()

      // 11.5.4//
      if (key.equalsIgnoreCase("delayMaximumMs")) {
        global.delayMaximumMs = value;
        break;
      }

      // /11.3.52//
      
      if (key.equalsIgnoreCase("logLevel")) {
        Logger.setLogLevel(value);
        Logger.info("logging level set to " + value);
        global.setParameterValue("logLevel", value);
        eval.setDebugging();
        return;
      }

      if (key.equalsIgnoreCase("axesMode")) {
        switch (value) {
        case JmolConstants.AXES_MODE_MOLECULAR:
          setAxesModeMolecular(true);
          return;
        case JmolConstants.AXES_MODE_BOUNDBOX:
          setAxesModeMolecular(false);
          return;
        case JmolConstants.AXES_MODE_UNITCELL:
          setAxesModeUnitCell(true);
          return;
        }
        return;
      }
      // /11.1.31//

      if (key.equalsIgnoreCase("propertyDataField")) {
        break;
      }

      // /11.1///

      if (key.equalsIgnoreCase("strandCount")) {
        setStrandCount(0, value);
        return;
      }
      if (key.equalsIgnoreCase("strandCountForStrands")) {
        setStrandCount(JmolConstants.SHAPE_STRANDS, value);
        return;
      }
      if (key.equalsIgnoreCase("strandCountForMeshRibbon")) {
        setStrandCount(JmolConstants.SHAPE_MESHRIBBON, value);
        return;
      }
      if (key.equalsIgnoreCase("perspectiveModel")) {
        setPerspectiveModel(value);
        break;
      }
      if (key.equalsIgnoreCase("showScript")) {
        global.scriptDelay = value;
        break;
      }
      if (key.equalsIgnoreCase("specularPower")) {
        setSpecularPower(value);
        break;
      }
      if (key.equalsIgnoreCase("specularExponent")) {
        setSpecularExponent(value);
        break;
      }
      if (key.equalsIgnoreCase("specular")) {
        setIntProperty("specularPercent", value);
        return;
      }
      if (key.equalsIgnoreCase("diffuse")) {
        setIntProperty("diffusePercent", value);
        return;
      }
      if (key.equalsIgnoreCase("ambient")) {
        setIntProperty("ambientPercent", value);
        return;
      }
      if (key.equalsIgnoreCase("specularPercent")) {
        setSpecularPercent(value);
        break;
      }
      if (key.equalsIgnoreCase("diffusePercent")) {
        setDiffusePercent(value);
        break;
      }
      if (key.equalsIgnoreCase("ambientPercent")) {
        setAmbientPercent(value);
        break;
      }

      if (key.equalsIgnoreCase("ribbonAspectRatio")) {
        global.ribbonAspectRatio = value;
        break;
      }
      if (key.equalsIgnoreCase("pickingSpinRate")) {
        global.pickingSpinRate = (value < 1 ? 1 : value);
        break;
      }
      if (key.equalsIgnoreCase("animationFps")) {
        setAnimationFps(value);
        break;
      }
      if (key.equalsIgnoreCase("percentVdwAtom")) {
        setPercentVdwAtom(value);
        break;
      }
      if (key.equalsIgnoreCase("bondRadiusMilliAngstroms")) {
        setMarBond((short) value);
        // public method -- no need to set
        return;
      }
      if (key.equalsIgnoreCase("hermiteLevel")) {
        global.hermiteLevel = value;
        break;
      }
      // not found
      if ((value != 0 && value != 1)
          || !setBooleanProperty(key, value == 1, false)) {
        if (setFloatProperty(key, value, true))
          return;
      }
      notFound = true;
      break;
    }
    key = key.toLowerCase();
    boolean isJmol = global.htParameterValues.containsKey(key);
    if (!isJmol && notFound) {
      if (global.htPropertyFlags.containsKey(key)) {
        setPropertyError(GT.translate(
            "ERROR: cannot set boolean flag to numeric value: {0}", key));
        return;
      }
    }
    if (!defineNew)
      return;
    if (isJmol) {
      global.setParameterValue(key, value);
    } else {
      global.setUserVariable(key, ScriptVariable.intVariable(value));
    }
  }

  int getDelayMaximum() {
    return global.delayMaximumMs;
  }

  public void setBooleanProperty(String key, boolean value) {
    if (key.charAt(0) == '_') {
      global.setParameterValue(key, value);
      return;
    }
    setBooleanProperty(key, value, true);
  }

  boolean setBooleanProperty(String key, boolean value, boolean defineNew) {
    boolean notFound = false;
    boolean doRepaint = true;
    while (true) {

      // 11.8.RC6
      
      if (key.equalsIgnoreCase("imageState")) {
        global.imageState = value;
        break;
      }
      
      // 11.7.40
      
      if (key.equalsIgnoreCase("useMinimizationThread")) {
        global.useMinimizationThread = value;
        break;
      }
      
      // 11.7.30

      if (key.equalsIgnoreCase("autoLoadOrientation")) {
        global.autoLoadOrientation = value;
        break;
      }

      // 11.7.24

      if (key.equalsIgnoreCase("allowKeyStrokes")) {
        if (global.disablePopupMenu)
          value = false;
        global.allowKeyStrokes = value;
        break;
      }

      if (key.equalsIgnoreCase("showKeyStrokes")) {
        global.showKeyStrokes = value;
        break;
      }
      // 11.7.10
      if (key.equalsIgnoreCase("fontCaching")) {
        global.fontCaching = value;
        break;
      }

      // 11.6.RC13
      if (key.equalsIgnoreCase("atomPicking")) {
        global.atomPicking = value;
        break;
      }

      // 11.6.RC13
      if (key.equalsIgnoreCase("bondPicking")) {
        global.bondPicking = value;
        break;
      }

      // 11.5.52
      if (key.equalsIgnoreCase("selectAllModels")) {
        global.selectAllModels = value;
        break;
      }
      // 11.5.39
      if (key.equalsIgnoreCase("messageStyleChime")) {
        global.messageStyleChime = value;
        break;
      }
      if (key.equalsIgnoreCase("pdbSequential")) {
        global.pdbSequential = value;
        break;
      }
      if (key.equalsIgnoreCase("pdbGetHeader")) {
        global.pdbGetHeader = value;
        break;
      }

      // 11.5.30
      // ellipsoidAxes just handled as getBooleanProperty()
      // ellipsoidArcs just handled as getBooleanProperty()
      // ellipsoidDots just handled as getBooleanProperty()
      // ellipsoidBall just handled as getBooleanProperty()
      // 11.5.4
      if (key.equalsIgnoreCase("fontScaling")) {
        global.fontScaling = value;
        break;
      }
      // 11.3.56
      if (key.equalsIgnoreCase("syncMouse")) {
        setSyncTarget(0, value);
        break;
      }
      if (key.equalsIgnoreCase("syncScript")) {
        setSyncTarget(1, value);
        break;
      }

      // 11.3.55

      if (key.equalsIgnoreCase("wireframeRotation")) {
        global.wireframeRotation = value;
        break;
      }

      // 11.3.46

      if (key.equalsIgnoreCase("isosurfacePropertySmoothing")) {
        global.isosurfacePropertySmoothing = value;
        break;
      }

      // 11.3.43

      if (key.equalsIgnoreCase("drawPicking")) {
        global.drawPicking = value;
        break;
      }

      // 11.3.36

      if (key.equalsIgnoreCase("antialiasDisplay")) {
        setAntialias(0, value);
        break;
      }

      if (key.equalsIgnoreCase("antialiasTranslucent")) {
        setAntialias(1, value);
        break;
      }

      if (key.equalsIgnoreCase("antialiasImages")) {
        setAntialias(2, value);
        break;
      }

      // 11.3.29

      if (key.equalsIgnoreCase("smartAromatic")) {
        global.smartAromatic = value;
        break;
      }

      // 11.1.29

      if (key.equalsIgnoreCase("applySymmetryToBonds")) {
        setApplySymmetryToBonds(value);
        break;
      }

      // 11.1.22

      if (key.equalsIgnoreCase("appendNew")) {
        setAppendNew(value);
        break;
      }

      if (key.equalsIgnoreCase("autoFPS")) {
        global.autoFps = value;
        break;
      }

      // 11.1.21

      if (key.equalsIgnoreCase("useNumberLocalization")) {
        TextFormat
            .setUseNumberLocalization(global.useNumberLocalization = value);
        break;
      }

      // 11.1.20

      if (key.equalsIgnoreCase("showFrank")) {
        setFrankOn(value);
        break;
      }

      // ///

      if (key.equalsIgnoreCase("solventProbe")) {
        global.solventOn = value;
        break;
      }

      if (key.equalsIgnoreCase("dynamicMeasurements")) {
        setDynamicMeasurements(value);
        break;
      }

      // 11.1.14

      if (key.equalsIgnoreCase("allowRotateSelected")) {
        setAllowRotateSelected(value);
        break;
      }

      // /11.1.13///

      if (key.equalsIgnoreCase("showScript")) {
        setIntProperty("showScript", value ? 1 : 0);
        return true;
      }
      // /11.1///
      if (key.equalsIgnoreCase("allowEmbeddedScripts")) {
        global.allowEmbeddedScripts = value;
        break;
      }
      if (key.equalsIgnoreCase("navigationPeriodic")) {
        global.navigationPeriodic = value;
        break;
      }
      if (key.equalsIgnoreCase("zShade")) {
        global.zShade = value;
        break;
      }
      if (key.equalsIgnoreCase("drawHover")) {
        pickingManager.setDrawHover(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationMode")) {
        setNavigationMode(value);
        break;
      }
      if (key.equalsIgnoreCase("navigateSurface")) {
        global.navigateSurface = value;
        break;
      }
      if (key.equalsIgnoreCase("hideNavigationPoint")) {
        global.hideNavigationPoint = value;
        break;
      }
      if (key.equalsIgnoreCase("showNavigationPointAlways")) {
        global.showNavigationPointAlways = value;
        break;
      }

      // /11.0///
      if (key.equalsIgnoreCase("refreshing")) {
        setRefreshing(value);
        break;
      }
      if (key.equalsIgnoreCase("justifyMeasurements")) {
        global.justifyMeasurements = value;
        break;
      }
      if (key.equalsIgnoreCase("ssBondsBackbone")) {
        global.ssbondsBackbone = value;
        break;
      }
      if (key.equalsIgnoreCase("hbondsBackbone")) {
        global.hbondsBackbone = value;
        break;
      }
      if (key.equalsIgnoreCase("hbondsSolid")) {
        global.hbondsSolid = value;
        break;
      }
      if (key.equalsIgnoreCase("specular")) {
        setSpecular(value);
        break;
      }
      if (key.equalsIgnoreCase("slabEnabled")) {
        // Eval.slab
        transformManager.setSlabEnabled(value); // refresh?
        break;
      }
      if (key.equalsIgnoreCase("zoomEnabled")) {
        transformManager.setZoomEnabled(value);
        break;
      }
      if (key.equalsIgnoreCase("highResolution")) {
        global.highResolutionFlag = value;
        break;
      }
      if (key.equalsIgnoreCase("traceAlpha")) {
        global.traceAlpha = value;
        break;
      }
      if (key.equalsIgnoreCase("zoomLarge")) {
        global.zoomLarge = value;
        transformManager.scaleFitToScreen(false, value, false, true);
        break;
      }
      if (key.equalsIgnoreCase("languageTranslation")) {
        GT.setDoTranslate(value);
        break;
      }
      if (key.equalsIgnoreCase("hideNotSelected")) {
        selectionManager.setHideNotSelected(value);
        break;
      }
      if (key.equalsIgnoreCase("colorRasmol")) {
        setDefaultColors(value ? "rasmol" : "jmol");
        break;
      }
      if (key.equalsIgnoreCase("scriptQueue")) {
        scriptManager.setQueue(value);
        break;
      }
      if (key.equalsIgnoreCase("dotSurface")) {
        global.dotSurface = value;
        break;
      }
      if (key.equalsIgnoreCase("dotsSelectedOnly")) {
        global.dotsSelectedOnly = value;
        break;
      }
      if (key.equalsIgnoreCase("selectionHalos")) {
        setSelectionHalos(value); // volatile
        break;
      }
      if (key.equalsIgnoreCase("selectHydrogen")) {
        global.rasmolHydrogenSetting = value;
        break;
      }
      if (key.equalsIgnoreCase("selectHetero")) {
        global.rasmolHeteroSetting = value;
        break;
      }
      if (key.equalsIgnoreCase("showMultipleBonds")) {
        global.showMultipleBonds = value;
        break;
      }
      if (key.equalsIgnoreCase("showHiddenSelectionHalos")) {
        global.showHiddenSelectionHalos = value;
        break;
      }
      if (key.equalsIgnoreCase("windowCentered")) {
        transformManager.setWindowCentered(value);
        break;
      }
      if (key.equalsIgnoreCase("displayCellParameters")) {
        global.displayCellParameters = value;
        break;
      }
      if (key.equalsIgnoreCase("testFlag1")) {
        global.testFlag1 = value;
        break;
      }
      if (key.equalsIgnoreCase("testFlag2")) {
        global.testFlag2 = value;
        break;
      }
      if (key.equalsIgnoreCase("testFlag3")) {
        global.testFlag3 = value;
        break;
      }
      if (key.equalsIgnoreCase("testFlag4")) {
        global.testFlag4 = value;
        break;
      }
      if (key.equalsIgnoreCase("ribbonBorder")) {
        global.ribbonBorder = value;
        break;
      }
      if (key.equalsIgnoreCase("cartoonRockets")) {
        global.cartoonRockets = value;
        break;
      }
      if (key.equalsIgnoreCase("rocketBarrels")) {
        global.rocketBarrels = value;
        break;
      }
      if (key.equalsIgnoreCase("greyscaleRendering")) {
        g3d.setGreyscaleMode(global.greyscaleRendering = value);
        break;
      }
      if (key.equalsIgnoreCase("measurementLabels")) {
        global.measurementLabels = value;
        break;
      }

      // these next three remove parameters, so don't set htParameter key here

      if (key.equalsIgnoreCase("axesWindow")) {
        setAxesModeMolecular(!value);
        return true;
      }
      if (key.equalsIgnoreCase("axesMolecular")) {
        setAxesModeMolecular(value);
        return true;
      }
      if (key.equalsIgnoreCase("axesUnitCell")) {
        setAxesModeUnitCell(value);
        return true;
      }
      // public; no need to set here
      if (key.equalsIgnoreCase("axesOrientationRasmol")) {
        setAxesOrientationRasmol(value);
        return true;
      }
      if (key.equalsIgnoreCase("debugScript")) {
        setDebugScript(value);
        return true;
      }
      if (key.equalsIgnoreCase("perspectiveDepth")) {
        setPerspectiveDepth(value);
        return true;
      }
      if (key.equalsIgnoreCase("showAxes")) {
        setShowAxes(value);
        return true;
      }
      if (key.equalsIgnoreCase("showBoundBox")) {
        setShowBbcage(value);
        return true;
      }
      if (key.equalsIgnoreCase("showHydrogens")) {
        setShowHydrogens(value);
        return true;
      }
      if (key.equalsIgnoreCase("showMeasurements")) {
        setShowMeasurements(value);
        return true;
      }
      if (key.equalsIgnoreCase("showUnitcell")) {
        setShowUnitCell(value);
        return true;
      }
      // these next are deprecated because they don't
      // give much indication what they really do:
      if (key.equalsIgnoreCase("frank"))
        return setBooleanProperty("showFrank", value, true);
      if (key.equalsIgnoreCase("solvent"))
        return setBooleanProperty("solventProbe", value, true);
      if (key.equalsIgnoreCase("bonds"))
        return setBooleanProperty("showMultipleBonds", value, true);
      if (key.equalsIgnoreCase("hydrogen")) // deprecated
        return setBooleanProperty("selectHydrogen", value, true);
      if (key.equalsIgnoreCase("hetero")) // deprecated
        return setBooleanProperty("selectHetero", value, true);
      if (key.equalsIgnoreCase("showSelections")) // deprecated -- see
                                                  // "selectionHalos"
        return setBooleanProperty("selectionHalos", value, true);
      // these next return, because there is no need to repaint
      while (true) {
        doRepaint = false;
        if (key.equalsIgnoreCase("bondModeOr")) {
          global.bondModeOr = value;
          break;
        }
        if (key.equalsIgnoreCase("zeroBasedXyzRasmol")) {
          global.zeroBasedXyzRasmol = value;
          reset();
          break;
        }
        if (key.equalsIgnoreCase("rangeSelected")) {
          global.rangeSelected = value;
          break;
        }
        if (key.equalsIgnoreCase("measureAllModels")) {
          global.measureAllModels = value;
          break;
        }
        if (key.equalsIgnoreCase("statusReporting")) {
          // not part of the state
          statusManager.setAllowStatusReporting(value);
          break;
        }
        if (key.equalsIgnoreCase("chainCaseSensitive")) {
          global.chainCaseSensitive = value;
          break;
        }
        if (key.equalsIgnoreCase("hideNameInPopup")) {
          global.hideNameInPopup = value;
          break;
        }
        if (key.equalsIgnoreCase("disablePopupMenu")) {
          global.disablePopupMenu = value;
          break;
        }
        if (key.equalsIgnoreCase("forceAutoBond")) {
          global.forceAutoBond = value;
          break;
        }
        // public - no need to set
        if (key.equalsIgnoreCase("autobond")) {
          setAutoBond(value);
          return true;
        }
        notFound = true;
        break;
      }
      if (!defineNew)
        return !notFound;
      notFound = true;
      break;
    }
    if (!defineNew)
      return !notFound;
    key = key.toLowerCase();
    boolean isJmol = global.htPropertyFlags.containsKey(key);
    if (!isJmol && notFound) {
      if (global.htParameterValues.containsKey(key)) {
        setPropertyError(GT.translate(
            "ERROR: Cannot set value of this variable to a boolean: {0}", key));
        return true;
      }
    }
    if (isJmol)
      global.setParameterValue(key, value);
    else
      global.setUserVariable(key, ScriptVariable.getBoolean(value));
    if (notFound)
      return false;
    if (doRepaint) {
      setTainted(true);
    }
    return true;
  }

  public boolean getPdbLoadInfo(int type) {
    switch (type) {
    case 1:
      return global.pdbSequential;
    case 2:
      return global.pdbGetHeader;
    }
    return false;
  }

  boolean getSelectAllModels() {
    return global.selectAllModels;
  }

  boolean getMessageStyleChime() {
    return global.messageStyleChime;
  }

  public boolean getFontCaching() {
    return global.fontCaching;
  }

  public boolean getFontScaling() {
    return global.fontScaling;
  }

  void showParameter(String key, boolean ifNotSet, int nMax) {
    String sv = "" + global.getParameterEscaped(key, nMax);
    if (ifNotSet || sv.indexOf("<not defined>") < 0)
      showString(key + " = " + sv, false);
  }

  public void showString(String str, boolean isPrint) {
    if (isScriptQueued && (!isSilent || isPrint))
      Logger.warn(str);
   // scriptEcho(str); //<- REMOVE JMOL LOGGING (freyssj)
  }

  String getAllSettings(String prefix) {
    return global.getAllSettings(prefix);
  }

  // ////// flags and settings ////////

  public boolean getDotSurfaceFlag() {
    return global.dotSurface;
  }

  public boolean getDotsSelectedOnlyFlag() {
    return global.dotsSelectedOnly;
  }

  public int getDotDensity() {
    return global.dotDensity;
  }

  public boolean isRangeSelected() {
    return global.rangeSelected;
  }

  boolean getIsosurfacePropertySmoothing() {
    // Eval
    return global.isosurfacePropertySmoothing;
  }

  public boolean getWireframeRotation() {
    return global.wireframeRotation;
  }

  boolean isWindowCentered() {
    return transformManager.isWindowCentered();
  }

  void setNavigationDepthPercent(float timeSec, float percent) {
    transformManager.setNavigationDepthPercent(timeSec, percent);
    refresh(1, "set navigationDepth");
  }

  float getNavigationSpeed() {
    return global.navigationSpeed;
  }

  public boolean getShowNavigationPoint() {
    if (!global.navigationMode || !transformManager.canNavigate())
      return false;
    return (isNavigating() && !global.hideNavigationPoint
        || global.showNavigationPointAlways || getInMotion());
  }

  public void setVisualRange(float angstroms) {
    transformManager.setVisualRange(angstroms);
    refresh(1, "set visualRange");
  }

  float getSolventProbeRadius() {
    return global.solventProbeRadius;
  }

  public float getCurrentSolventProbeRadius() {
    return global.solventOn ? global.solventProbeRadius : 0;
  }

  boolean getSolventOn() {
    return global.solventOn;
  }

  public boolean getTestFlag1() {
    return global.testFlag1;
  }

  public boolean getTestFlag2() {
    return global.testFlag2;
  }

  public boolean getTestFlag3() {
    return global.testFlag3;
  }

  public boolean getTestFlag4() {
    return global.testFlag4;
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    // setBooleanProperty
    // stateManager.setCrystallographicDefaults
    // app preferences dialog
    global.setParameterValue("perspectiveDepth", perspectiveDepth);
    transformManager.setPerspectiveDepth(perspectiveDepth);
  }

  public void setAxesOrientationRasmol(boolean TF) {
    // app PreferencesDialog
    // stateManager
    // setBooleanproperty
    /*
     * *************************************************************** RasMol
     * has the +Y axis pointing down And rotations about the y axis are
     * left-handed setting this flag makes Jmol mimic this behavior
     * 
     * All versions of Jmol prior to 11.5.51 incompletely implement this flag.
     * Really all it is just a flag to tell Eval to flip the sign of the Y
     * rotation when specified specifically as "rotate/spin y 30".
     * 
     * In principal, we could display the axis opposite as well, but that is
     * only aesthetic and not at all justified if the axis is molecular.
     * **************************************************************
     */
    global.setParameterValue("axesOrientationRasmol", TF);
    global.axesOrientationRasmol = TF;
    reset();
  }

  public boolean getAxesOrientationRasmol() {
    return global.axesOrientationRasmol;
  }

  void setAxesScale(float scale) {
    global.axesScale = scale;
    axesAreTainted = true;
  }

  public Point3f[] getAxisPoints() {
    // for uccage renderer
    return (getObjectMad(StateManager.OBJ_AXIS1) == 0
        || getAxesMode() != JmolConstants.AXES_MODE_UNITCELL
        || ((Boolean) getShapeProperty(JmolConstants.SHAPE_AXES, "axesTypeXY"))
            .booleanValue() ? null : (Point3f[]) getShapeProperty(
        JmolConstants.SHAPE_AXES, "axisPoints"));
  }

  public float getAxesScale() {
    return global.axesScale;
  }

  private void setAxesModeMolecular(boolean TF) {
    global.axesMode = (TF ? JmolConstants.AXES_MODE_MOLECULAR
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
    global.removeJmolParameter("axesunitcell");
    global.removeJmolParameter(TF ? "axeswindow" : "axesmolecular");
    global.setParameterValue("axesMode", global.axesMode);
    global.setParameterValue(TF ? "axesMolecular" : "axesWindow", true);

  }

  void setAxesModeUnitCell(boolean TF) {
    // stateManager
    // setBooleanproperty
    global.axesMode = (TF ? JmolConstants.AXES_MODE_UNITCELL
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
    global.removeJmolParameter("axesmolecular");
    global.removeJmolParameter(TF ? "axeswindow" : "axesunitcell");
    global.setParameterValue(TF ? "axesUnitcell" : "axesWindow", true);
    global.setParameterValue("axesMode", global.axesMode);
  }

  public int getAxesMode() {
    return global.axesMode;
  }

  public boolean getDisplayCellParameters() {
    return global.displayCellParameters;
  }

  public boolean getPerspectiveDepth() {
    return transformManager.getPerspectiveDepth();
  }

  public void setSelectionHalos(boolean TF) {
    // display panel can hit this without a frame, apparently
    if (modelSet == null || TF == getSelectionHaloEnabled())
      return;
    global.setParameterValue("selectionHalos", TF);
    loadShape(JmolConstants.SHAPE_HALOS);
    // a frame property, so it is automatically reset
    modelSet.setSelectionHaloEnabled(TF);
  }

  public boolean getSelectionHaloEnabled() {
    return modelSet.getSelectionHaloEnabled();
  }

  public boolean getBondSelectionModeOr() {
    return global.bondModeOr;
  }

  public boolean getChainCaseSensitive() {
    return global.chainCaseSensitive;
  }

  public boolean getRibbonBorder() {
    // mps
    return global.ribbonBorder;
  }

  public boolean getCartoonRocketFlag() {
    return global.cartoonRockets;
  }

  public boolean getRocketBarrelFlag() {
    return global.rocketBarrels;
  }

  private void setStrandCount(int type, int value) {
    switch (type) {
    case JmolConstants.SHAPE_STRANDS:
      global.strandCountForStrands = value;
      break;
    case JmolConstants.SHAPE_MESHRIBBON:
      global.strandCountForMeshRibbon = value;
      break;
    default:
      global.strandCountForStrands = value;
      global.strandCountForMeshRibbon = value;
      break;
    }
    global.setParameterValue("strandCount", value);
    global.setParameterValue("strandCountForStrands",
        global.strandCountForStrands);
    global.setParameterValue("strandCountForMeshRibbon",
        global.strandCountForMeshRibbon);
  }

  public int getStrandCount(int type) {
    return (type == JmolConstants.SHAPE_STRANDS ? global.strandCountForStrands
        : global.strandCountForMeshRibbon);
  }

  boolean getHideNameInPopup() {
    return global.hideNameInPopup;
  }

  boolean getNavigationPeriodic() {
    return global.navigationPeriodic;
  }

  private void stopAnimationThreads() {
    setVibrationOff();
    setSpinOn(false);
    setNavOn(false);
    setAnimationOn(false);
  }

  private void setNavigationMode(boolean TF) {
    global.navigationMode = TF;
    if (TF && !transformManager.canNavigate()) {
      stopAnimationThreads();
      transformManager = transformManager.getNavigationManager(this,
          dimScreen.width, dimScreen.height);
      transformManager.homePosition();
    }
    transformManager.setNavigationMode(TF);
  }

  public boolean getNavigationMode() {
    return global.navigationMode;
  }

  public boolean getNavigateSurface() {
    return global.navigateSurface;
  }

  /** for an external application
   * 
   * @param transformManager
   */
  public void setTransformManager(TransformManager transformManager) {
    stopAnimationThreads();
    this.transformManager = transformManager;
    transformManager.setViewer(this, dimScreen.width, dimScreen.height);
    setTransformManagerDefaults();
    transformManager.homePosition();      
  }
  
  private void setPerspectiveModel(int mode) {
    stopAnimationThreads();
    switch (mode) {
    case 10:
      transformManager = new TransformManager10(this, dimScreen.width,
          dimScreen.height);
      break;
    default:
      transformManager = transformManager.getNavigationManager(this,
          dimScreen.width, dimScreen.height);
    }
    setTransformManagerDefaults();
    transformManager.homePosition();
  }

  private void setTransformManagerDefaults() {
    transformManager.setCameraDepthPercent(global.cameraDepth);
    transformManager.setPerspectiveDepth(global.perspectiveDepth);
    transformManager.setStereoDegrees(TransformManager.DEFAULT_STEREO_DEGREES);
    transformManager.setVisualRange(global.visualRange);
    transformManager.setSpinOn(false);
    transformManager.setVibrationPeriod(0);
    transformManager.setFrameOffsets(frameOffsets);
  }

  public float getCameraDepth() {
    return global.cameraDepth;
  }
  
  boolean getZoomLarge() {
    return global.zoomLarge;
  }

  public boolean getTraceAlpha() {
    // mps
    return global.traceAlpha;
  }

  public int getHermiteLevel() {
    // mps
    return global.hermiteLevel;
  }

  public boolean getHighResolution() {
    // mps
    return global.highResolutionFlag;
  }

  String getLoadState() {
    return global.getLoadState();
  }

  public void setAutoBond(boolean TF) {
    // setBooleanProperties
    global.setParameterValue("autobond", TF);
    global.autoBond = TF;
  }

  public boolean getAutoBond() {
    return global.autoBond;
  }

  int[] makeConnections(float minDistance, float maxDistance, short order,
                        int connectOperation, BitSet bsA, BitSet bsB,
                        BitSet bsBonds, boolean isBonds) {
    // eval
    clearModelDependentObjects();
    clearAllMeasurements(); // necessary for serialization
    return modelSet.makeConnections(minDistance, maxDistance, order,
        connectOperation, bsA, bsB, bsBonds, isBonds);
  }

  public void rebond() {
    // Eval, PreferencesDialog
    clearModelDependentObjects();
    modelSet.deleteAllBonds();
    modelSet.autoBond(null, null, null, null);
    addStateScript("connect;", false, true);
  }

  void setPdbConectBonding(boolean isAuto) {
    // from eval
    clearModelDependentObjects();
    modelSet.deleteAllBonds();
    BitSet bsExclude = new BitSet();
    modelSet.setPdbConectBonding(0, 0, bsExclude);
    if (isAuto) {
      modelSet.autoBond(null, null, bsExclude, null);
      addStateScript("connect PDB AUTO;", false, true);
      return;
    }
    addStateScript("connect PDB;", false, true);
  }

  // //////////////////////////////////////////////////////////////
  // Graphics3D
  // //////////////////////////////////////////////////////////////

  boolean getGreyscaleRendering() {
    return global.greyscaleRendering;
  }

  boolean getDisablePopupMenu() {
    return global.disablePopupMenu;
  }

  public boolean getForceAutoBond() {
    return global.forceAutoBond;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  public void setPercentVdwAtom(int percentVdwAtom) {
    global.setParameterValue("percentVdwAtom", percentVdwAtom);
    global.percentVdwAtom = percentVdwAtom;
    setShapeSize(JmolConstants.SHAPE_BALLS, -percentVdwAtom, Float.NaN);
  }

  public int getPercentVdwAtom() {
    return global.percentVdwAtom;
  }

  public short getDefaultMadAtom() {
    return (short) (global.percentVdwAtom == 0 ? 0 : -2000
        - global.percentVdwAtom);
  }

  public short getMadBond() {
    return (short) (global.bondRadiusMilliAngstroms * 2);
  }

  public short getMarBond() {
    return global.bondRadiusMilliAngstroms;
  }

  /*
   * void setModeMultipleBond(byte modeMultipleBond) { //not implemented
   * global.modeMultipleBond = modeMultipleBond; 
   * }
   */

  public byte getModeMultipleBond() {
    // sticksRenderer
    return global.modeMultipleBond;
  }

  public boolean getShowMultipleBonds() {
    return global.showMultipleBonds;
  }

  public void setShowHydrogens(boolean TF) {
    // PreferencesDialog
    // setBooleanProperty
    global.setParameterValue("showHydrogens", TF);
    global.showHydrogens = TF;
  }

  public boolean getShowHydrogens() {
    return global.showHydrogens;
  }

  public boolean getShowHiddenSelectionHalos() {
    return global.showHiddenSelectionHalos;
  }

  public void setShowBbcage(boolean value) {
    setObjectMad(JmolConstants.SHAPE_BBCAGE, "boundbox", (short) (value ? -4
        : 0));
    global.setParameterValue("showBoundBox", value);
  }

  public boolean getShowBbcage() {
    return getObjectMad(StateManager.OBJ_BOUNDBOX) != 0;
  }

  public void setShowUnitCell(boolean value) {
    setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitcell", (short) (value ? -2
        : 0));
    global.setParameterValue("showUnitCell", value);
  }

  public boolean getShowUnitCell() {
    return getObjectMad(StateManager.OBJ_UNITCELL) != 0;
  }

  public void setShowAxes(boolean value) {
    setObjectMad(JmolConstants.SHAPE_AXES, "axes", (short) (value ? -2 : 0));
    global.setParameterValue("showAxes", value);
  }

  public boolean getShowAxes() {
    return getObjectMad(StateManager.OBJ_AXIS1) != 0;
  }

  boolean frankOn = true;

  public void setFrankOn(boolean TF) {
    if (isPreviewOnly)
      TF = false;
    frankOn = TF;
    setObjectMad(JmolConstants.SHAPE_FRANK, "frank", (short) (TF ? 1 : 0));
  }

  public boolean getShowFrank() {
    if (isPreviewOnly || isApplet && creatingImage)
      return false;
    return (isSignedApplet || frankOn);
  }

  public boolean isSignedApplet() {
    return isSignedApplet;
  }

  public void setShowMeasurements(boolean TF) {
    // setbooleanProperty
    global.setParameterValue("showMeasurements", TF);
    global.showMeasurements = TF;
  }

  public boolean getShowMeasurements() {
    return global.showMeasurements;
  }

  public boolean getShowMeasurementLabels() {
    return global.measurementLabels;
  }

  public boolean getMeasureAllModelsFlag() {
    return global.measureAllModels;
  }

  void setMeasureDistanceUnits(String units) {
    // stateManager
    // Eval
    global.setMeasureDistanceUnits(units);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "reformatDistances", null);
  }

  public String getMeasureDistanceUnits() {
    return global.getMeasureDistanceUnits();
  }

  public boolean getUseNumberLocalization() {
    return global.useNumberLocalization;
  }

  void setAppendNew(boolean value) {
    // Eval dataFrame
    global.appendNew = value;
  }

  public boolean getAppendNew() {
    return global.appendNew;
  }

  boolean getAutoFps() {
    return global.autoFps;
  }

  public void setRasmolDefaults() {
    setDefaults("RasMol");
  }

  public void setJmolDefaults() {
    setDefaults("Jmol");
  }

  private void setDefaults(String type) {
    if (type.equalsIgnoreCase("RasMol")) {
      stateManager.setRasMolDefaults();
      return;
    }
    stateManager.setJmolDefaults();
    setShapeSize(JmolConstants.SHAPE_BALLS, getDefaultMadAtom(),
        Float.NaN, getModelAtomBitSet(-1, true));
  }

  public boolean getZeroBasedXyzRasmol() {
    return global.zeroBasedXyzRasmol;
  }

  private void setAntialias(int mode, boolean TF) {
    switch (mode) {
    case 0: // display
      global.antialiasDisplay = TF;
      break;
    case 1: // translucent
      global.antialiasTranslucent = TF;
      break;
    case 2: // images
      global.antialiasImages = TF;
      return;
    }
    resizeImage(0, 0, false, false, true);
  }

  // //////////////////////////////////////////////////////////////
  // temp manager
  // //////////////////////////////////////////////////////////////

  public Point3f[] allocTempPoints(int size) {
    // rockets renderer
    return tempManager.allocTempPoints(size);
  }

  public void freeTempPoints(Point3f[] tempPoints) {
    tempManager.freeTempPoints(tempPoints);
  }

  public Point3i[] allocTempScreens(int size) {
    // mesh and mps
    return tempManager.allocTempScreens(size);
  }

  public void freeTempScreens(Point3i[] tempScreens) {
    tempManager.freeTempScreens(tempScreens);
  }

  /*
   * public boolean[] allocTempBooleans(int size) { return
   * tempManager.allocTempBooleans(size); }
   * 
   * public void freeTempBooleans(boolean[] tempBooleans) {
   * tempManager.freeTempBooleans(tempBooleans); }
   */

  public byte[] allocTempBytes(int size) {
    // mps renderer
    return tempManager.allocTempBytes(size);
  }

  public void freeTempBytes(byte[] tempBytes) {
    tempManager.freeTempBytes(tempBytes);
  }

  // //////////////////////////////////////////////////////////////
  // font stuff
  // //////////////////////////////////////////////////////////////
  Font3D getFont3D(String fontFace, String fontStyle, float fontSize) {
    return g3d.getFont3D(fontFace, fontStyle, fontSize);
  }

  public String formatText(String text0) {
    int i;
    if ((i = text0.indexOf("@{")) < 0 && (i = text0.indexOf("%{")) < 0)
      return text0;

    // old style %{ now @{

    String text = TextFormat.simpleReplace(text0, "%{", "@{");
    String name;
    while ((i = text.indexOf("@{")) >= 0) {
      i++;
      int i0 = i + 1;
      int len = text.length();
      i = ScriptCompiler.ichMathTerminator(text, i, len);
      if (i >= len)
        return text;
      name = text.substring(i0, i);
      if (name.length() == 0)
        return text;
      Object v = evaluateExpression(name);
      if (v instanceof Point3f)
        v = Escape.escape((Point3f) v);
      text = text.substring(0, i0 - 2) + v.toString() + text.substring(i + 1);
    }
    return text;
  }

  // //////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  // //////////////////////////////////////////////////////////////

  String getElementSymbol(int i) {
    return modelSet.getElementSymbol(i);
  }

  int getElementNumber(int i) {
    return modelSet.getElementNumber(i);
  }

  public String getAtomName(int i) {
    return modelSet.getAtomName(i);
  }

  public int getAtomNumber(int i) {
    return modelSet.getAtomNumber(i);
  }

  float getAtomX(int i) {
    return modelSet.getAtomX(i);
  }

  float getAtomY(int i) {
    return modelSet.getAtomY(i);
  }

  float getAtomZ(int i) {
    return modelSet.getAtomZ(i);
  }

  public Point3f getAtomPoint3f(int i) {
    return modelSet.getAtomAt(i);
  }

  public float getAtomRadius(int i) {
    return modelSet.getAtomRadius(i);
  }

  public float getAtomVdwRadius(int i) {
    return modelSet.getAtomVdwRadius(i);
  }

  public int getAtomArgb(int i) {
    return g3d.getColixArgb(modelSet.getAtomColix(i));
  }

  String getAtomChain(int i) {
    return modelSet.getAtomChain(i);
  }

  public int getAtomModelIndex(int i) {
    return modelSet.getAtomModelIndex(i);
  }

  String getAtomSequenceCode(int i) {
    return modelSet.getAtomSequenceCode(i);
  }

  public float getBondRadius(int i) {
    return modelSet.getBondRadius(i);
  }

  public short getBondOrder(int i) {
    return modelSet.getBondOrder(i);
  }

  void assignAromaticBonds() {
    modelSet.assignAromaticBonds();
  }

  public boolean getSmartAromatic() {
    return global.smartAromatic;
  }

  void resetAromatic() {
    modelSet.resetAromatic();
  }

  public int getBondArgb1(int i) {
    return g3d.getColixArgb(modelSet.getBondColix1(i));
  }

  public int getBondModelIndex(int i) {
    // legacy
    return modelSet.getBondModelIndex(i);
  }

  public int getBondArgb2(int i) {
    return g3d.getColixArgb(modelSet.getBondColix2(i));
  }

  public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex) {
    return modelSet.getPolymerLeadMidPoints(modelIndex, polymerIndex);
  }

  // //////////////////////////////////////////////////////////////
  // stereo support
  // //////////////////////////////////////////////////////////////

  void setStereoMode(int[] twoColors, int stereoMode, float degrees) {
    setFloatProperty("stereoDegrees", degrees);
    setBooleanProperty("greyscaleRendering",
        stereoMode > JmolConstants.STEREO_DOUBLE);
    if (twoColors != null)
      transformManager.setStereoMode(twoColors);
    else
      transformManager.setStereoMode(stereoMode);
  }

  // //////////////////////////////////////////////////////////////
  //
  // //////////////////////////////////////////////////////////////

  public boolean isJvm12orGreater() {
    return jvm12orGreater;
  }

  public String getOperatingSystemName() {
    return strOSName;
  }

  public String getJavaVendor() {
    return strJavaVendor;
  }

  public String getJavaVersion() {
    return strJavaVersion;
  }

  public Graphics3D getGraphics3D() {
    return g3d;
  }

  public boolean showModelSetDownload() {
    return true; // deprecated
  }

  // /////////////// getProperty /////////////

  public Object getProperty(String returnType, String infoType, String paramInfo) {
    return getProperty(returnType, infoType, (Object) paramInfo);
  }

  private boolean scriptEditorVisible;
  
  boolean isScriptEditorVisible() {
    return scriptEditorVisible;
  }
  
  JmolAppConsoleInterface appConsole;
  JmolScriptEditorInterface scriptEditor;
  JmolPopup jmolpopup;
  String menuStructure;
  
  public Object getProperty(String returnType, String infoType, Object paramInfo) {
    // accepts a BitSet paramInfo
    // return types include "JSON", "String", "readable", and anything else
    // returns the Java object.
    // Jmol 11.7.45 also uses this method as a general API 
    // for getting and returning script data from the console and editor

    if ("DATA_API".equals(returnType)) {
      switch (("scriptCheck........." //0 
              +"scriptContext......." //20
              +"scriptEditor........" //40
              +"scriptEditorState..." //60
              +"getAppConsole......." //80
              +"getScriptEditor....." //100
              +"setMenu............." //120
              +"wrappedState........" //140
              +"spaceGroupInfo......" //160
              +"disablePopupMenu...." //180
              ).indexOf(infoType)) {

      case 0:
        return scriptCheck((String) paramInfo, true);
      case 20:
        return eval.getScriptContext();
      case 40:
        showEditor((String[]) paramInfo);
        return null;
      case 60:
        scriptEditorVisible = ((Boolean)paramInfo).booleanValue();
        return null;
      case 80:
        if (paramInfo instanceof JmolAppConsoleInterface) {
          appConsole = (JmolAppConsoleInterface) paramInfo;
        } else if (appConsole == null && paramInfo != null && ((Boolean) paramInfo).booleanValue()) {
          appConsole = (isApplet ? 
              (JmolAppConsoleInterface) Interface.getOptionInterface("console.AppletConsole")
              : (JmolAppConsoleInterface) Interface.getApplicationInterface("jmolpanel.AppConsole"))
                  .getAppConsole(this, display);
        }
        scriptEditor = (appConsole == null ? null : appConsole.getScriptEditor());
        return appConsole;
      case 100:
        if (appConsole == null && paramInfo != null && ((Boolean) paramInfo).booleanValue()) {
          getProperty("DATA_API", "appConsole", Boolean.TRUE);
          scriptEditor = (appConsole == null ? null : appConsole.getScriptEditor());
        }
        return scriptEditor;
      case 120:
        return menuStructure = (String) paramInfo;
      case 140:
        if (!global.imageState)
          return "";
        return "\n/**" + JmolConstants.EMBEDDED_SCRIPT_TAG + " \n" + getStateInfo() + "\n**/";
      case 160:
        return getSpaceGroupInfo(null);
      case 180:
        global.disablePopupMenu = true; // no false here, because it's a one-time setting
        return null;
      default:
        System.out.println("ERROR in getProperty DATA_API: " + infoType);
        return null;
      }
        
    } 
    return PropertyManager.getProperty(this, returnType, infoType, paramInfo);
  }

  void showEditor(String[] file_text) {
    if (file_text == null)
      file_text = new String[] { null, null };
    if (file_text[1] == null)
      file_text[1] = "<no data>";
    String filename = file_text[1];
    String msg = file_text[0];
    JmolScriptEditorInterface scriptEditor = (JmolScriptEditorInterface) getProperty(
        "DATA_API", "getScriptEditor", Boolean.TRUE);
    if (msg != null) {
      scriptEditor.setFilename(filename);
      scriptEditor.output(msg);
    }
    scriptEditor.setVisible(true);
  }
  
  String getModelExtract(Object atomExpression) {
    return fileManager.getFullPathName() + "\nJmol version " + getJmolVersion()
        + "\nEXTRACT: " + atomExpression + "\n"
        + modelSet.getModelExtract(getAtomBitSet(atomExpression));
  }

  public String getHexColorFromIndex(short colix) {
    return g3d.getHexColorFromIndex(colix);
  }

  // ////////////////////////////////////////////////

  void setModelVisibility() {
    // Eval -- ok - handled specially
    if (modelSet == null) // necessary for file chooser
      return;
    modelSet.setModelVisibility();
  }

  void setFrameTitle(int modelIndex, String title) {
    modelSet.setFrameTitle(modelIndex, title);
  }

  String getFrameTitle(int modelIndex) {
    return modelSet.getFrameTitle(modelIndex);
  }

  boolean isTainted = true;

  void setTainted(boolean TF) {
    isTainted = TF && refreshing;
    axesAreTainted = TF && refreshing;
  }

  public int notifyMouseClicked(int x, int y, int modifiers, int clickCount) {
    // change y to 0 at bottom
    global.setParameterValue("_mouseX", x);
    global.setParameterValue("_mouseY", dimScreen.height - y);
    global.setParameterValue("_mouseModifiers", modifiers);
    global.setParameterValue("_clickCount", modifiers);
    return statusManager.setStatusClicked(x, dimScreen.height - y, modifiers, clickCount);
  }

  Point3fi checkObjectClicked(int x, int y, int modifiers) {
    return modelSet.checkObjectClicked(x, y, modifiers,
        getVisibleFramesBitSet());
  }

  boolean checkObjectHovered(int x, int y) {
    if (modelSet == null)
      return false;
    return modelSet.checkObjectHovered(x, y, getVisibleFramesBitSet());
  }

  void checkObjectDragged(int prevX, int prevY, int x, int y, int modifiers) {
    int iShape = 0;
    switch (getPickingMode()) {
    case JmolConstants.PICKING_LABEL:
      iShape = JmolConstants.SHAPE_LABELS;
      break;
    case JmolConstants.PICKING_DRAW:
      iShape = JmolConstants.SHAPE_DRAW;
      break;
    }
    modelSet.checkObjectDragged(prevX, prevY, x, y, modifiers,
        getVisibleFramesBitSet(), iShape);
  }

  void rotateAxisAngleAtCenter(Point3f rotCenter, Vector3f rotAxis,
                               float degrees, float endDegrees, boolean isSpin,
                               BitSet bsSelected) {
    // Eval: rotate FIXED
    if (Float.isNaN(degrees) || degrees == 0)
      return;
    transformManager.rotateAxisAngleAtCenter(rotCenter, rotAxis, degrees,
        endDegrees, isSpin, bsSelected);
    refresh(-1, "rotateAxisAngleAtCenter");
  }

  void rotateAboutPointsInternal(Point3f point1, Point3f point2, float degrees,
                                 float endDegrees, boolean isSpin,
                                 BitSet bsSelected) {
    // Eval: rotate INTERNAL
    if (Float.isNaN(degrees) || degrees == 0)
      return;
    transformManager.rotateAboutPointsInternal(point1, point2, degrees,
        endDegrees, false, isSpin, bsSelected);
    refresh(-1, "rotateAxisAboutPointsInternal");
  }

  int getPickingSpinRate() {
    // PickingManager
    return global.pickingSpinRate;
  }

  public void startSpinningAxis(Point3f pt1, Point3f pt2, boolean isClockwise) {
    // Draw.checkObjectClicked ** could be difficult
    // from draw object click
    if (getSpinOn() || getNavOn()) {
      setSpinOn(false);
      setNavOn(false);
      return;
    }
    transformManager.rotateAboutPointsInternal(pt1, pt2,
        global.pickingSpinRate, Float.MAX_VALUE, isClockwise, true, null);
  }

  public Vector3f getModelDipole() {
    return modelSet.getModelDipole(getDisplayModelIndex());
  }

  public Vector3f calculateMolecularDipole() {
    return modelSet.calculateMolecularDipole(getDisplayModelIndex());
  }

  public float getDipoleScale() {
    return global.dipoleScale;
  }

  public void getAtomIdentityInfo(int atomIndex, Hashtable info) {
    modelSet.getAtomIdentityInfo(atomIndex, info);
  }

  void setDefaultLattice(Point3f ptLattice) {
    // Eval -- handled separately
    global.setDefaultLattice(ptLattice);
    global.setParameterValue("defaultLattice", Escape.escape(ptLattice));
  }

  Point3f getDefaultLattice() {
    return global.getDefaultLattice();
  }

  BitSet getTaintedAtoms(byte type) {
    return modelSet.getTaintedAtoms(type);
  }

  public void setTaintedAtoms(BitSet bs, byte type) {
    modelSet.setTaintedAtoms(bs, type);
  }

  public String getData(String atomExpression, String type) {
    String exp = "";
    if (type.toLowerCase().indexOf("property_") == 0)
      exp = "{selected}.label(\"%{" + type + "}\")";
    else if (type.equalsIgnoreCase("PDB"))
      // old crude
      exp = "{selected and not hetero}.label(\"ATOM  %5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines"
          + "+{selected and hetero}.label(\"HETATM%5i %-4a%1A%3.3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines";
    else if (type.equalsIgnoreCase("MOL"))
      exp = "\"line1\nline2\nline3\n\"+(\"\"+{selected}.size)%-3+(\"\"+{selected}.bonds.size)%-3+\"  0  0  0\n\""
          + "+{selected}.labels(\"%10.4x%10.4y%10.4z %-2e  0  0  0  0  0\").lines"
          + "+{selected}.bonds.labels(\"%3D1%3D2%3ORDER  0  0  0\").lines";
    else if (type.startsWith("USER:"))
      exp = "{selected}.label(\"" + type.substring(5) + "\").lines";
    else
      // if(type.equals("XYZ"))
      exp = "\"\" + {selected}.size + \"\n\n\"+{selected}.label(\"%-2e %10.5x %10.5y %10.5z\").lines";
    if (!atomExpression.equals("selected"))
      exp = TextFormat.simpleReplace(exp, "selected", atomExpression);
    return (String) evaluateExpression(exp);
  }

  public synchronized Object evaluateExpression(Object stringOrTokens) {
    return ScriptEvaluator.evaluateExpression(this, stringOrTokens);
  }

  Object getHelixData(BitSet bs, int tokType) {
    return modelSet.getHelixData(bs, tokType);
  }

  public String getPdbData(BitSet bs) {
    if (bs == null)
      bs = getSelectionSet();
    return modelSet.getPdbAtomData(bs);
  }

  String getPdbData(int modelIndex, String type) {
    return modelSet.getPdbData(modelIndex, type, selectionManager.bsSelection,
        false);
  }

  public boolean isJmolDataFrame(int modelIndex) {
    return modelSet.isJmolDataFrame(modelIndex);
  }

  public boolean isJmolDataFrame() {
    return modelSet.isJmolDataFrame(animationManager.currentModelIndex);
  }

  int getJmolDataFrameIndex(int modelIndex, String type) {
    return modelSet.getJmolDataFrameIndex(modelIndex, type);
  }

  void setJmolDataFrame(String type, int modelIndex, int dataIndex) {
    modelSet.setJmolDataFrame(type, modelIndex, dataIndex);
  }

  void setFrameTitle(String title) {
    loadShape(JmolConstants.SHAPE_ECHO);
    modelSet.setFrameTitle(animationManager.currentModelIndex, title);
  }

  public String getFrameTitle() {
    return modelSet.getFrameTitle(animationManager.currentModelIndex);
  }

  String getJmolFrameType(int modelIndex) {
    return modelSet.getJmolFrameType(modelIndex);
  }

  public int getJmolDataSourceFrame(int modelIndex) {
    return modelSet.getJmolDataSourceFrame(modelIndex);
  }

  void setAtomProperty(BitSet bs, int tok, int iValue, float fValue,
                       String sValue, float[] values, String[] list) {
    modelSet.setAtomProperty(bs, tok, iValue, fValue, sValue, values, list);
    switch (tok) {
    case Token.atomX:
    case Token.atomY:
    case Token.atomZ:
    case Token.fracX:
    case Token.fracY:
    case Token.fracZ:
    case Token.unitX:
    case Token.unitY:
    case Token.unitZ:
      refreshMeasures();
    }
  }

  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    // Frame equivalent used in DATA "coord set"
    modelSet.setAtomCoord(atomIndex, x, y, z);
    // no measure refresh here -- because it may involve hundreds of calls
  }

  void setAtomCoord(BitSet bs, int tokType, Object xyzValues) {
    modelSet.setAtomCoord(bs, tokType, xyzValues);
    refreshMeasures();
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    modelSet.setAtomCoordRelative(atomIndex, x, y, z);
    // no measure refresh here -- because it may involve hundreds of calls
  }

  void setAtomCoordRelative(Point3f offset) {
    // Eval
    modelSet.setAtomCoordRelative(offset, selectionManager.bsSelection);
    refreshMeasures();
  }

  void setAllowRotateSelected(boolean TF) {
    global.allowRotateSelected = TF;
  }

  boolean allowRotateSelected() {
    return global.allowRotateSelected;
  }

  void invertSelected(Point3f pt, BitSet bs) {
    // Eval
    modelSet.invertSelected(pt, null, bs);
    refreshMeasures();
  }

  void invertSelected(Point3f pt, Point4f plane) {
    // Eval
    modelSet.invertSelected(pt, plane, selectionManager.bsSelection);
    refreshMeasures();
  }

  synchronized void moveSelected(int deltaX, int deltaY, int x, int y,
                                 boolean isTranslation) {
    if (isJmolDataFrame())
      return;
    BitSet bsSelected = selectionManager.bsSelection;
    if (deltaX == Integer.MIN_VALUE) {
      setSelectionHalos(true);
      refresh(3, "moveSelected");
      return;
    }
    if (deltaX == Integer.MAX_VALUE) {
      setSelectionHalos(false);
      refresh(3, "moveSelected");
      return;
    }
    if (isTranslation) {
      Point3f ptCenter = getAtomSetCenter(bsSelected);
      Point3i pti = transformPoint(ptCenter);
      Point3f pt = new Point3f(pti.x + deltaX, pti.y + deltaY, pti.z);
      unTransformPoint(pt, pt);
      pt.sub(ptCenter);
      modelSet.setAtomCoordRelative(pt, bsSelected);
    } else {
      transformManager.setRotateMolecule(true);
      transformManager.rotateXYBy(deltaX, deltaY, bsSelected);
      transformManager.setRotateMolecule(false);
    }
    refreshMeasures();
  }

  void rotateAtoms(Matrix3f mNew, Matrix3f matrixRotate, boolean fullMolecule,
                   Point3f center, boolean isInternal, BitSet bsAtoms) {
    modelSet.rotateAtoms(mNew, matrixRotate, bsAtoms, fullMolecule, center,
        isInternal);
    refreshMeasures();
  }

  public void refreshMeasures() {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "refresh", null);
  }

  void setDynamicMeasurements(boolean TF) { // deprecated; unnecessary
    global.dynamicMeasurements = TF;
  }

  public boolean getDynamicMeasurements() {
    return global.dynamicMeasurements;
  }

  /**
   * fills an array with data -- if nX < 0 and this would involve JavaScript,
   * then this reads a full set of Double[][] in one function call. Otherwise it
   * reads the values using individual function calls, which each return Double.
   * 
   * If the functionName begins with "file:" then data are read from a file
   * specified after the colon. The sign of nX is not relevant in that case. The
   * file may contain mixed numeric and non-numeric values; the non-numeric
   * values will be skipped by Parser.parseFloatArray
   * 
   * @param functionName
   * @param nX
   * @param nY
   * @return nX by nY array of floating values
   */
  public float[][] functionXY(String functionName, int nX, int nY) {
    String data = null;
    if (functionName.indexOf("file:") == 0)
      data = getFileAsString(functionName.substring(5));
    else if (functionName.indexOf("data2d_") != 0)
      return statusManager.functionXY(functionName, nX, nY);
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    float[][] fdata;
    if (data == null) {
      fdata = getDataFloat2D(functionName);
      if (fdata != null)
        return fdata;
      data = "";
    }
    fdata = new float[nX][nY];
    float[] f = new float[nX * nY];
    Parser.parseFloatArray(data, null, f);
    for (int i = 0, n = 0; i < nX; i++)
      for (int j = 0; j < nY; j++)
        fdata[i][j] = f[n++];
    return fdata;
  }

  float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    String data = null;
    if (functionName.indexOf("file:") == 0)
      data = getFileAsString(functionName.substring(5));
    else if (functionName.indexOf("data3d_") != 0)
      return statusManager.functionXYZ(functionName, nX, nY, nZ);
    nX = Math.abs(nX);
    nY = Math.abs(nY);
    nZ = Math.abs(nZ);
    float[][][] xyzdata;
    if (data == null) {
      xyzdata = getDataFloat3D(functionName);
      if (xyzdata != null)
        return xyzdata;
      data = "";
    }
    xyzdata = new float[nX][nY][nZ];
    float[] f = new float[nX * nY * nZ];
    Parser.parseFloatArray(data, null, f);
    for (int i = 0, n = 0; i < nX; i++)
      for (int j = 0; j < nY; j++)
        for (int k = 0; k < nZ; k++)
          xyzdata[i][j][k] = f[n++];
    return xyzdata;
  }

  void getHelp(String what) {
    if (what.length() > 0 && what.indexOf("?") != 0
        && global.helpPath.indexOf("?") < 0)
      what = "?search=" + what;
    showUrl(global.helpPath + what);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  /*
   * Moved from the consoles to viewer, since this could be of general interest,
   * it's more a property of Eval/Viewer, and the consoles are really just a
   * mechanism for getting user input and sending results, not saving a history
   * of it all. Ultimately I hope to integrate the mouse picking and possibly
   * periodic updates of position into this history to get a full history. We'll
   * see! BH 9/2006
   */

  /**
   * Adds one or more commands to the command history
   * 
   * @param command
   *          the command to add
   */
  void addCommand(String command) {
    if (!autoExit)
      commandHistory.addCommand(TextFormat.replaceAllCharacters(command, "\r\n\t", " "));
  }

  /**
   * Removes one command from the command history
   * 
   * @return command removed
   */
  String removeCommand() {
    return commandHistory.removeCommand();
  }

  /**
   * Options include: ; all n == Integer.MAX_VALUE ; n prev n >= 1 ; next n ==
   * -1 ; set max to -2 - n n <= -3 ; just clear n == -2 ; clear and turn off;
   * return "" n == 0 ; clear and turn on; return "" n == Integer.MIN_VALUE;
   * 
   * @param howFarBack
   *          number of lines (-1 for next line)
   * @return one or more lines of command history
   */
  public String getSetHistory(int howFarBack) {
    return commandHistory.getSetHistory(howFarBack);
  }

  // ///////////////////////////////////////////////////////////////
  // image and file export
  // ///////////////////////////////////////////////////////////////

  public void writeTextFile(String fileName, String data) {
    createImage(fileName, "txt", data, Integer.MIN_VALUE, 0, 0);
  }

  /**
   * 
   * @param text
   *          null here clips image; String clips text
   * @return "OK" for image or "OK [number of bytes]"
   */
  public String clipImage(String text) {
    JmolImageCreatorInterface c;
    try {
      c = (JmolImageCreatorInterface) Interface
          .getOptionInterface("util.ImageCreator");
      c.setViewer(this);
      return c.clipImage(text);
    } catch (Error er) {
      // unsigned applet will not have this interface
      return GT.translate("clipboard is not accessible -- use signed applet");
    }
  }

  /**
   * 
   * from eval write command only includes option to write set of files
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @param width
   * @param height
   * @param bsFrames
   * @return message starting with "OK" or an error message
   */
  String createImage(String fileName, String type, Object text_or_bytes,
                     int quality, int width, int height, BitSet bsFrames) {
    if (bsFrames == null)
      return createImage(fileName, type, text_or_bytes, quality, width, height);
    int modelCount = getModelCount();
    String info = "";
    int n = 0;
    int ptDot = fileName.indexOf(".");
    if (ptDot < 0)
      ptDot = fileName.length();

    String froot = fileName.substring(0, ptDot);
    String fext = fileName.substring(ptDot);
    for (int i = 0; i < modelCount; i++)
      if (bsFrames.get(i)) {
        setCurrentModelIndex(i);
        fileName = "0000" + (++n);
        fileName = froot + fileName.substring(fileName.length() - 4) + fext;
        String msg = createImage(fileName, type, text_or_bytes, quality, width,
            height);
        Logger.info(msg);
        info += msg + "\n";
        if (!msg.startsWith("OK"))
          return "ERROR WRITING FILE SET: \n" + info;
      }
    if (info.length() == 0)
      info = "OK\n";
    return info + "\n" + n + " files created";
  }

  private boolean creatingImage;

  /**
   * general routine for creating an image or writing data to a file
   * 
   * passes request to statusManager to pass along to app or applet
   * jmolStatusListener interface
   * 
   * @param fileName
   *          starts with ? --> use file dialog; null --> to clipboard
   * @param type
   *          PNG, JPG, etc.
   * @param text_or_bytes
   *          String or byte[] or null if an image
   * @param quality
   *          Integer.MIN_VALUE --> not an image
   * @param width
   *          image width
   * @param height
   *          image height
   * @return null (canceled) or a message starting with OK or an error message
   */
  public String createImage(String fileName, String type, Object text_or_bytes,
                            int quality, int width, int height) {

    /*
     * 
     * org.jmol.export.image.AviCreator does create AVI animations from Jpegs
     * but these aren't read by standard readers, so that's pretty much useless.
     * 
     * files must have the designated width and height
     * 
     * text_or_bytes: new Object[] { (File[]) files, (String) outputFilename,
     * (int[]) params }
     * 
     * where for now we just read param[0] as frames per second
     * 
     * 
     * if (text_or_bytes != null && text_or_bytes instanceof Object[]) {
     * Object[] obj = (Object[]) text_or_bytes; String[] files = (String[])
     * obj[0]; String outputFileName = (String) obj[1]; int[] params = (int[])
     * obj[2]; int fps = params[0]; JmolMovieCreatorInterface ac =
     * (JmolMovieCreatorInterface) Interface
     * .getOptionInterface("export.image.AviCreator"); if (ac == null) return
     * "could not initialize org.jmol.export.image.AviCreator"; String ret =
     * ac.createMovie(this, files, width, height, fps, outputFileName); return
     * (ret == null ? "OK" : ret); }
     */

    int saveWidth = dimScreen.width;
    int saveHeight = dimScreen.height;
    if (quality != Integer.MIN_VALUE) {
      mustRender = true;
      resizeImage(width, height, true, false, false);
      setModelVisibility();
    }
    creatingImage = true;
    String err = null;
    try {
      if (fileName == null) {
        err = clipImage((String) text_or_bytes);
      } else {
        boolean useDialog = (fileName.indexOf("?") == 0);
        if (useDialog)
          fileName = fileName.substring(1);
        fileName = FileManager.setLocalPathForWritingFile(this, fileName);
        if (isApplet) {
          // applet calls creatImage itself
          err = statusManager.createImage((useDialog ? "?" : "") + fileName,
              type, text_or_bytes, quality);
        } else {
          // application can do it itself or allow Jmol to do it here
          err = statusManager.createImage(fileName, type, text_or_bytes, quality);
          if (err == null) {
            JmolImageCreatorInterface c = (JmolImageCreatorInterface) Interface
               .getOptionInterface("export.image.ImageCreator");
            c.setViewer(this);
            err = (String) c.createImage(fileName, type, text_or_bytes, quality);
            statusManager.createImage(err, type, null, quality);
          }
        }
      }
      // err may be null if user cancels operation involving dialog and "?"
    } catch (Throwable er) {
      Logger.error(setErrorMessage(err = "ERROR creating image: " + er));
    }
    creatingImage = false;
    if (quality != Integer.MIN_VALUE) {
      resizeImage(saveWidth, saveHeight, true, false, true);
    }
    return err;
  }

  private void setImageFontScaling(int width, int height) {
    float screenDimNew = (global.zoomLarge == (height > width) ? height : width);
    imageFontScaling = screenDimNew / getScreenDim();
  }

  private void setSyncTarget(int mode, boolean TF) {
    switch (mode) {
    case 0:
      statusManager.syncingMouse = TF;
      break;
    case 1:
      statusManager.syncingScripts = TF;
      break;
    case 2:
      statusManager.syncSend(TF ? SYNC_GRAPHICS_MESSAGE
          : SYNC_NO_GRAPHICS_MESSAGE, "*");
      if (Float.isNaN(transformManager.stereoDegrees))
        setFloatProperty("stereoDegrees",
            TransformManager.DEFAULT_STEREO_DEGREES);
      if (TF) {
        setBooleanProperty("syncMouse", false);
        setBooleanProperty("syncScript", false);
      }
      return;
    }
    // if turning both off, sync the orientation now
    if (!statusManager.syncingScripts && !statusManager.syncingMouse)
      refresh(-1, "set sync");
  }

  public final static String SYNC_GRAPHICS_MESSAGE = "GET_GRAPHICS";
  public final static String SYNC_NO_GRAPHICS_MESSAGE = "SET_GRAPHICS_OFF";

  public void syncScript(String script, String applet) {
    if (script.equalsIgnoreCase(SYNC_GRAPHICS_MESSAGE)) {
      statusManager.setSyncDriver(StatusManager.SYNC_STEREO);
      statusManager.syncSend(script, applet);
      setBooleanProperty("syncMouse", false);
      setBooleanProperty("syncScript", false);
      return;
    }
    // * : all applets
    // > : all OTHER applets
    // . : just me
    // ~ : disable send (just me)
    boolean disableSend = "~".equals(applet);
    // null same as ">" -- "all others"
    if (!disableSend && !".".equals(applet)) {
      statusManager.syncSend(script, applet);
      if (!"*".equals(applet))
        return;
    }
    if (script.equalsIgnoreCase("on")) {
      statusManager.setSyncDriver(StatusManager.SYNC_DRIVER);
      return;
    }
    if (script.equalsIgnoreCase("off")) {
      statusManager.setSyncDriver(StatusManager.SYNC_OFF);
      return;
    }
    if (script.equalsIgnoreCase("slave")) {
      statusManager.setSyncDriver(StatusManager.SYNC_SLAVE);
      return;
    }
    int syncMode = statusManager.getSyncMode();
    if (syncMode == StatusManager.SYNC_OFF)
      return;
    if (syncMode != StatusManager.SYNC_DRIVER)
      disableSend = false;
    if (Logger.debugging)
      Logger.debug(htmlName + " syncing with script: " + script);
    // driver is being positioned by another driver -- don't pass on the change
    // driver is being positioned by a mouse movement
    // format is from above refresh(2, xxx) calls
    // Mouse: [CommandName] [value1] [value2]
    if (disableSend)
      statusManager.setSyncDriver(StatusManager.SYNC_DISABLE);
    if (script.indexOf("Mouse: ") != 0) {
      evalStringQuiet(script, true, false);
      return;
    }
    String[] tokens = Parser.getTokens(script);
    String key = tokens[1];
    switch (tokens.length) {
    case 3:
      if (key.equals("zoomByFactor"))
        zoomByFactor(Parser.parseFloat(tokens[2]));
      else if (key.equals("zoomBy"))
        zoomBy(Parser.parseInt(tokens[2]));
      else if (key.equals("rotateZBy"))
        rotateZBy(Parser.parseInt(tokens[2]));
      break;
    case 4:
      if (key.equals("rotateXYBy"))
        rotateXYBy(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]));
      else if (key.equals("translateXYBy"))
        translateXYBy(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]));
      else if (key.equals("rotateMolecule"))
        rotateMolecule(Parser.parseInt(tokens[2]), Parser.parseInt(tokens[3]));
      break;
    }
    if (disableSend)
      setSyncDriver(StatusManager.SYNC_ENABLE);
  }

  void setSyncDriver(int mode) {
    statusManager.setSyncDriver(mode);
  }

  public float[] getPartialCharges() {
    return modelSet.getPartialCharges();
  }

  void setProteinType(byte iType, BitSet bs) {
    modelSet.setProteinType(bs == null ? selectionManager.bsSelection : bs,
        iType);
  }

  /*
   * void debugStack(String msg) { //what's the right way to do this? try {
   * Logger.error(msg); String t = null; t.substring(3); } catch (Exception e) {
   * e.printStackTrace(); } }
   */

  public Point3f getBondPoint3f1(int i) {
    // legacy -- no calls
    return modelSet.getBondAtom1(i);
  }

  public Point3f getBondPoint3f2(int i) {
    // legacy -- no calls
    return modelSet.getBondAtom2(i);
  }

  public Vector3f getVibrationVector(int atomIndex) {
    return modelSet.getVibrationVector(atomIndex, false);
  }

  public int getVanderwaalsMar(int i) {
    return (dataManager.defaultVdw == JmolConstants.VDW_USER ? dataManager.userVdwMars[i]
        : JmolConstants.getVanderwaalsMar(i, dataManager.defaultVdw));
  }

  public int getVanderwaalsMar(int i, int iMode) {
    if (iMode == JmolConstants.VDW_USER && dataManager.bsUserVdws == null)
      iMode = dataManager.defaultVdw;
    return (iMode == JmolConstants.VDW_USER ? dataManager.userVdwMars[i]
        : JmolConstants.getVanderwaalsMar(i, iMode));
  }

  void setDefaultVdw(String mode) {
    dataManager.setDefaultVdw(mode);
    global.setParameterValue("defaultVDW", getDefaultVdw(Integer.MIN_VALUE));
  }

  String getDefaultVdw(int iMode) {
    return dataManager.getDefaultVdw(iMode, null);
  }

  public int deleteAtoms(BitSet bs, boolean fullModels) {
    clearModelDependentObjects();
    if (!fullModels)
      return selectionManager.deleteAtoms(bs);
    fileManager.addLoadScript("zap " + Escape.escape(bs));
    setCurrentModelIndex(0, false);
    animationManager.setAnimationOn(false);
    BitSet bsDeleted = modelSet.deleteModels(bs);
    setAnimationRange(0, 0);
    eval.deleteAtomsInVariables(bsDeleted);
    repaintManager.clear();
    animationManager.clear();
    animationManager.initializePointers(1);
    if (getModelCount() > 1)
      setCurrentModelIndex(-1, true);
    hoverAtomIndex = -1;
    setFileLoadStatus(FILE_STATUS_MODELS_DELETED, null, null, null, null);
    refreshMeasures();
    return BitSetUtil.cardinalityOf(bsDeleted);
  }

  public void deleteModelAtoms(int firstAtomIndex, int nAtoms, BitSet bsDeleted) {
    // called from ModelCollection.deleteModel
    dataManager.deleteModelAtoms(firstAtomIndex, nAtoms, bsDeleted);
  }

  public BitSet getDeletedAtoms() {
    return selectionManager.bsDeleted;
  }

  public char getQuaternionFrame() {
    return global.quaternionFrame.charAt(global.quaternionFrame.length() == 2 ? 1 : 0);
  }

  public int getHelixStep() {
    return global.helixStep;
  }

  String calculatePointGroup() {
    return modelSet.calculatePointGroup(selectionManager.bsSelection);
  }

  public Hashtable getPointGroupInfo(Object atomExpression) {
    return modelSet.getPointGroupInfo(getAtomBitSet(atomExpression));
  }

  public String getPointGroupAsString(boolean asDraw, String type, int index,
                                      float scale) {
    return modelSet.getPointGroupAsString(selectionManager.bsSelection, asDraw,
        type, index, scale);
  }

  public float getPointGroupTolerance(int type) {
    switch (type) {
    case 0:
      return global.pointGroupDistanceTolerance;
    case 1:
      return global.pointGroupLinearTolerance;
    }
    return 0;
  }

  Object getFileAsImage(String pathName, Hashtable htParams) {
    if (!haveDisplay)
      return "no display";
    Object obj = fileManager.getFileAsImage(pathName, htParams);
    if (obj instanceof String)
      return obj;
    Image image = (Image) obj;
    MediaTracker tracker = new MediaTracker(display);
    tracker.addImage(image, 0);
    try {
      tracker.waitForID(0);
    } catch (InterruptedException e) {
      // Got to do something?
    }
    return image;
  }

  String cd(String dir) {
    if (dir == null) {
      dir = ".";
    } else if (dir.length() == 0) {
      setStringProperty("defaultDirectory", "");
      dir = ".";
    }
    dir = fileManager.getDefaultDirectory(dir
        + (dir.equals("=") || dir.endsWith("/") ? "" : "/X"));
    if (dir.length() > 0)
      setStringProperty("defaultDirectory", dir);
    String path = fileManager.getFullPath(dir + "/", true);
    if (path.startsWith("file:/"))
      FileManager.setLocalPath(this, dir, false);
    return dir;
  }

  // //// Error handling

  private String errorMessage;
  private String errorMessageUntranslated;

  private String setErrorMessage(String errMsg) {
    return setErrorMessage(errMsg, null);
  }

  private String setErrorMessage(String errMsg, String errMsgUntranslated) {
    errorMessageUntranslated = errMsgUntranslated;
    // System.out.println("viewer setErrorMessage " + errMsg + " " +
    // errMsgUntranslated);
    return (errorMessage = errMsg);
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getErrorMessageUntranslated() {
    // System.out.println("viewer getErrorMessage " + errorMessage + " " +
    // errorMessageUntranslated);
    return errorMessageUntranslated == null ? errorMessage
        : errorMessageUntranslated;
  }

  private int currentShapeID = -1;
  private String currentShapeState;
  
  public Shape getShape(int i) {
    return (modelSet == null ? null : modelSet.getShape(i));
  }

  public void setShapeErrorState(int shapeID, String state) {
    currentShapeID = shapeID;
    currentShapeState = state;
  }

  public String getShapeErrorState() {
    if (currentShapeID < 0)
      return "";
    if (modelSet != null)
      modelSet.releaseShape(currentShapeID);
    repaintManager.clear(currentShapeID);
    return JmolConstants.getShapeClassName(currentShapeID) + " "
        + currentShapeState;
  }

  public void handleError(Error er, boolean doClear) {
    // almost certainly out of memory; could be missing Jar file
    try {
      if (doClear)
        zap("" + er); // get some breathing room
      Logger.error("viewer handling error condition: " + er);
      notifyError("Error", "doClear=" + doClear + "; " + er, "" + er);
    } catch (Throwable e1) {
      try {
        Logger.error("Could not notify error " + er + ": due to " + e1);
      } catch (Throwable er2) {
        // tough luck.
      }
    }
  }

  float[] getAtomicCharges() {
    return modelSet.getAtomicCharges();
  }

  
  /// User-defined functions
  
  ScriptFunction getFunction(String name) {
    return stateManager.getFunction(name);
  }

  void addFunction(ScriptFunction f) {
    stateManager.addFunction(f);
  }

  void clearFunctions() {
    StateManager.globalFunctions.clear();
    stateManager.localFunctions.clear();
  }

  boolean isFunction(String name) {
    return stateManager.isFunction(name);
  }

  Hashtable getFunctions(boolean isLocal) {
    return (isLocal ? stateManager.localFunctions
        : StateManager.globalFunctions);
  }

  void showMessage(String s) {
    if (!isPrintOnly)
      Logger.warn(s);
  }

  public String getMoInfo(int modelIndex) {
    return modelSet.getMoInfo(modelIndex);
  }

  boolean isRepaintPending() {
    return repaintManager.repaintPending;
  }

}
