/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-30 00:35:55 +0200 (dim., 30 août 2009) $
 * $Revision: 11391 $
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import org.jmol.util.Logger;
import org.jmol.util.TextFormat;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.jmol.api.JmolAppConsoleInterface;
import org.jmol.api.JmolCallbackListener;
import org.jmol.api.JmolStatusListener;

/**
 * 
 * The StatusManager class handles all details of status reporting, including:
 * 
 * 1) saving the message in a queue that replaces the "callback" mechanism,
 * 2) sending messages off to the console, and
 * 3) delivering messages back to the main Jmol.java class in app or applet
 *    to handle differences in capabilities, including true callbacks.

atomPicked

fileLoaded
fileLoadError
frameChanged

measureCompleted
measurePending
measurePicked

newOrientation 

scriptEcho
scriptError
scriptMessage
scriptStarted
scriptStatus
scriptTerminated

userAction
viewerRefreshed

   
 * Bob Hanson hansonr@stolaf.edu  2/2006
 * 
 */

class StatusManager {

  StatusManager(Viewer viewer) {
    this.viewer = viewer;
  }

  private boolean allowStatusReporting; // set in StateManager.global
  
  void setAllowStatusReporting(boolean TF){
     allowStatusReporting = TF;
  }
  
  private Viewer viewer;
  private JmolStatusListener jmolStatusListener;
  private JmolCallbackListener jmolCallbackListener;
  private String statusList = "";

  String getStatusList() {
    return statusList;
  }
  
  /*
   * the messageQueue provided a mechanism for saving and recalling
   * information about the running of a script. The idea was to poll
   * the applet instead of using callbacks. 
   * 
   * As it turns out, polling of applets is fraught with problems, 
   * not the least of which is the most odd behavior of some 
   * versions of Firefox that makes text entry into the URL line 
   * enter in reverse order of characters during applet polling. 
   * This bug may or may not have been resolved, but in any case,
   * callbacks have proven far more efficient than polling anyway,
   * so this mechanism is probably not particularly generally useful. 
   * 
   * Still, the mechanism is here because in addition to applet polling,
   * it provides a way to get selective information back from the applet
   * to the calling page after a script has run synchronously (using applet.scriptWait). 
   * 
   * The basic idea involves:
   * 
   * 1) Setting what types of messages should be saved
   * 2) Executing the scriptWait(script) call
   * 3) Decoding the return value of that function
   * 
   * Note that this is not meant to be a complete record of the script.
   * Rather, each messsage type is saved in its own Vector, and 
   * only most recent MAX_QUEUE_LENGTH (16) entries are saved at any time.
   * 
   * For example:
   * 
   * 1) jmolGetStatus("scriptEcho,scriptMessage,scriptStatus,scriptError",targetSuffix)
   * 
   * Here we flush the message queue and identify the status types we want to maintain.
   *
   * 2) var ret = "" + applet.scriptWait("background red;echo ok;echo hello;")
   * 
   * The ret variable contains the array of messages in JSON format, which
   * can then be reconstructed as a JavaScript array object. In this case the return is:
   * 
   * 
   * {"jmolStatus": [ 
   *  [ 
   *    [ 3,"scriptEcho",0,"ok" ],
   *    [ 4,"scriptEcho",0,"hello" ] 
   *  ],[ 
   *    [ 1,"scriptStarted",6,"background red;echo ok;echo hello;" ] 
   *  ],[ 
   *    [ 6,"scriptTerminated",1,"Jmol script terminated successfully" ] 
   *  ],[ 
   *    [ 2,"scriptStatus",0,"script 6 started" ],
   *    [ 5,"scriptStatus",0,"Script completed" ],
   *    [ 7,"scriptStatus",0,"Jmol script terminated" ] 
   *  ] 
   *  ]}
   * 
   * Decoded, what we have is a "jmolStatus" JavaScript Array. This array has 4 elements, 
   * our scriptEcho, scriptStarted, scriptTerminated, and scriptStatus messages.
   * 
   * Within each of those elements we have the most recent 16 status messages.
   * Each status record consists of four entries:
   * 
   *   [ statusPtr, statusName, intInfo, strInfo ]
   *  
   * The first entry in each record is the sequential number when that record
   * was recorded, so to reconstruct the sequence of events, simply order the arrays:
   * 
   *    [ 1,"scriptStarted",6,"background red;echo ok;echo hello;" ] 
   *    [ 2,"scriptStatus",0,"script 6 started" ],
   *    [ 3,"scriptEcho",0,"ok" ],
   *    [ 4,"scriptEcho",0,"hello" ] 
   *    [ 5,"scriptStatus",0,"Script completed" ],
   *    [ 6,"scriptTerminated",1,"Jmol script terminated successfully" ] 
   *    [ 7,"scriptStatus",0,"Jmol script terminated" ] 
   *
   * While it could be argued that the presence of the statusName in each record is
   * redundant, and a better structure would be a Hashtable, this is the way it is 
   * implemented here and required for Jmol.js. 
   * 
   * Note that Jmol.js has a set of functions that manipulate this data. 
   * 
   */
  
  private Hashtable messageQueue = new Hashtable();
  Hashtable getMessageQueue() {
    return messageQueue;
  }
  
  private int statusPtr = 0;
  private static int MAXIMUM_QUEUE_LENGTH = 16;
  
////////////////////Jmol status //////////////

  private boolean recordStatus(String statusName) {
    return (allowStatusReporting && statusList.length() > 0 
        && (statusList.equals("all") || statusList.indexOf(statusName) >= 0));
  }
  
  synchronized private void setStatusChanged(String statusName,
      int intInfo, Object statusInfo, boolean isReplace) {
    if (!recordStatus(statusName))
      return;
    statusPtr++;
    Vector statusRecordSet;
    Vector msgRecord = new Vector();
    msgRecord.addElement(new Integer(statusPtr));
    msgRecord.addElement(statusName);
    msgRecord.addElement(new Integer(intInfo));
    msgRecord.addElement(statusInfo);
    if (isReplace && messageQueue.containsKey(statusName)) {
      messageQueue.remove(statusName);
    }
    if (messageQueue.containsKey(statusName)) {
      statusRecordSet = (Vector)messageQueue.remove(statusName);
    } else {
      statusRecordSet = new Vector();
    }
    if (statusRecordSet.size() == MAXIMUM_QUEUE_LENGTH)
      statusRecordSet.removeElementAt(0);
    
    statusRecordSet.addElement(msgRecord);
    messageQueue.put(statusName, statusRecordSet);
  }
  
  private boolean asVector = true;

  synchronized Object getStatusChanged(String statusNameList) {
    /*
     * returns a Vector of statusRecordSets, one per status type,
     * where each statusRecordSet is itself a vector of vectors:
     * [int statusPtr,String statusName,int intInfo, String statusInfo]
     * 
     * This allows selection of just the type desired as well as sorting
     * by time overall.
     * 
     */
    
    if (statusNameList.indexOf("AS_") == 0) {
      asVector = (statusNameList.indexOf("VECTOR") == 3);
      return statusNameList;
    }
    Object msgList;
    if (asVector)
      msgList = new Vector();
    else
      msgList = new Hashtable();
    if (resetMessageQueue(statusNameList))
      return msgList;
    Enumeration e = messageQueue.keys();
    while (e.hasMoreElements()) {
      String statusName = (String)e.nextElement();
      Object record = messageQueue.remove(statusName);
      if (asVector)
        ((Vector) msgList).addElement(record);
      else
        ((Hashtable)msgList).put(statusName, record);
    }
    return msgList;
  }

  private synchronized boolean resetMessageQueue(String statusList) {
    boolean isRemove = (statusList.length() > 0 && statusList.charAt(0) == '-');
    boolean isAdd = (statusList.length() > 0 && statusList.charAt(0) == '+');
    String oldList = this.statusList;
    if (isRemove) {
      this.statusList = TextFormat.simpleReplace(oldList, statusList.substring(1,statusList.length()), "");
      messageQueue = new Hashtable();
      statusPtr = 0;
      return true;
    }
    statusList = TextFormat.simpleReplace(statusList, "+", "");
    if(oldList.equals(statusList) 
        || isAdd && oldList.indexOf(statusList) >= 0)
      return false;
    if (! isAdd) {
      messageQueue = new Hashtable();
      statusPtr = 0;
      this.statusList = "";
    }
    this.statusList += statusList;
    if (Logger.debugging) {
      Logger.debug(oldList + "\nmessageQueue = " + this.statusList);
    }
    return true;
  }

  synchronized void setJmolStatusListener(JmolStatusListener jmolStatusListener, JmolCallbackListener jmolCallbackListener) {
    this.jmolStatusListener = jmolStatusListener;
    this.jmolCallbackListener = (jmolCallbackListener == null ? (JmolCallbackListener) jmolStatusListener : jmolCallbackListener);
  }
  
  synchronized void setJmolCallbackListener(JmolCallbackListener jmolCallbackListener) {
    this.jmolCallbackListener = jmolCallbackListener;
  }
  
  private String[] jmolScriptCallbacks = new String[JmolConstants.CALLBACK_COUNT];
  
  private String jmolScriptCallback(int iCallback) {
    String s = jmolScriptCallbacks[iCallback];
    if (s != null)
      viewer.evalStringQuiet(s, true, false);
    return s;
  }
  
  synchronized void setCallbackFunction(String callbackType,
                                        String callbackFunction) {
    // menu and language setting also use this route
    int iCallback = JmolConstants.getCallbackId(callbackType);
    if (iCallback >= 0) {
      int pt = (callbackFunction == null ? 0
          : callbackFunction.length() > 7
              && callbackFunction.toLowerCase().indexOf("script:") == 0 ? 7
              : callbackFunction.length() > 11
                  && callbackFunction.toLowerCase().indexOf("jmolscript:") == 0 ? 11
                  : 0);
      jmolScriptCallbacks[iCallback] = (pt == 0 ? null : callbackFunction
          .substring(pt).trim());
    }
    if (jmolCallbackListener != null)
      jmolCallbackListener.setCallbackFunction(callbackType, callbackFunction);
  }
  
  private boolean notifyEnabled(int type) {
    return jmolCallbackListener != null && jmolCallbackListener.notifyEnabled(type);
  }

  synchronized void setStatusAtomPicked(int atomIndex, String strInfo) {
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_PICK);
    Logger.info("setStatusAtomPicked(" + atomIndex + "," + strInfo + ")");
    setStatusChanged("atomPicked", atomIndex, strInfo, false);
    if (notifyEnabled(JmolConstants.CALLBACK_PICK))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_PICK,
          new Object[] { sJmol, strInfo, new Integer(atomIndex) });
  }

  synchronized int setStatusClicked(int x, int y, int modifiers, int clickCount) {
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_CLICK);
    if (!notifyEnabled(JmolConstants.CALLBACK_CLICK))
      return modifiers;
    // allows modification of modifiers
    int[] m = new int[] { modifiers };
    jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_CLICK,
        new Object[] { sJmol, new Integer(x), new Integer(y), new Integer(modifiers), new Integer(clickCount), m });
    return m[0];
  }

  synchronized void setStatusResized(int width, int height){
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_RESIZE);
    if (notifyEnabled(JmolConstants.CALLBACK_RESIZE))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_RESIZE,
          new Object[] { sJmol, new Integer(width), new Integer(height) }); 
  }

  synchronized void setStatusAtomHovered(int iatom, String strInfo) {
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_HOVER);
    if (notifyEnabled(JmolConstants.CALLBACK_HOVER))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_HOVER, 
          new Object[] {sJmol, strInfo, new Integer(iatom) });
  }
  
  synchronized void setFileLoadStatus(String fullPathName, String fileName,
                                        String modelName, String errorMsg,
                                        int ptLoad, boolean doCallback) {
    if (fullPathName == null) {
      JmolAppConsoleInterface appConsole = (JmolAppConsoleInterface) viewer.getProperty("DATA_API", "getAppConsole", null);
      if (fileName != null && appConsole != null)
        appConsole.zap();
      return;
    }
    setStatusChanged("fileLoaded", ptLoad, fullPathName, false);
    if (errorMsg != null)
      setStatusChanged("fileLoadError", ptLoad, errorMsg, false);
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_LOADSTRUCT);
    if (doCallback && notifyEnabled(JmolConstants.CALLBACK_LOADSTRUCT)) 
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_LOADSTRUCT,
          new Object[] { sJmol, 
              fullPathName, fileName, modelName, errorMsg, new Integer(ptLoad) });
  }

  synchronized void setStatusFrameChanged(int frameNo, int fileNo, int modelNo,
                                          int firstNo, int lastNo) {
    //System.out.println("setStatusFrameChanged modelSet=" + viewer.getModelSet());
    if (viewer.getModelSet() == null)
      return;
    boolean isAnimationRunning = (frameNo <= -2);
    int f = frameNo;
    if (isAnimationRunning)
      f = -2 - f;
    setStatusChanged("frameChanged", frameNo, (f >= 0 ? viewer
        .getModelNumberDotted(f) : ""), false);
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_ANIMFRAME);
    if (notifyEnabled(JmolConstants.CALLBACK_ANIMFRAME)) {
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_ANIMFRAME,
          new Object[] { sJmol,
              new int[] { frameNo, fileNo, modelNo, firstNo, lastNo } });
    }
    
    if (viewer.jmolpopup != null && !isAnimationRunning)
      viewer.jmolpopup.updateComputedMenus();

  }

  synchronized void setScriptEcho(String strEcho,
                                  boolean isScriptQueued) {
    if (strEcho == null)
      return;
    setStatusChanged("scriptEcho", 0, strEcho, false);
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_ECHO);
    if (notifyEnabled(JmolConstants.CALLBACK_ECHO))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_ECHO,
          new Object[] { sJmol, strEcho, new Integer(isScriptQueued ? 1 : 0) });
  }

  synchronized void setStatusMeasuring(String status, int intInfo, String strMeasure) {
    setStatusChanged(status, intInfo, strMeasure, false);
    String sJmol = null;
    if(status.equals("measureCompleted")) { 
      Logger.info("measurement["+intInfo+"] = "+strMeasure);
      sJmol = jmolScriptCallback(JmolConstants.CALLBACK_MEASURE);
    } else if (status.equals("measurePicked")) {
        setStatusChanged("measurePicked", intInfo, strMeasure, false);
        Logger.info("measurePicked " + intInfo + " " + strMeasure);
    }
        
    if (notifyEnabled(JmolConstants.CALLBACK_MEASURE))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_MEASURE, 
          new Object[] { sJmol, strMeasure,  new Integer(intInfo), status });
  }
  
  synchronized void notifyError(String errType, String errMsg,
                                String errMsgUntranslated) {
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_ERROR);
    if (notifyEnabled(JmolConstants.CALLBACK_ERROR))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_ERROR,
          new Object[] { sJmol, errType, errMsg, viewer.getShapeErrorState(),
              errMsgUntranslated });
  }
  
  synchronized void notifyMinimizationStatus(String minStatus, Integer minSteps, 
                                             Float minEnergy, Float minEnergyDiff) {
    String sJmol = jmolScriptCallback(JmolConstants.CALLBACK_MINIMIZATION);
    if (notifyEnabled(JmolConstants.CALLBACK_MINIMIZATION))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_MINIMIZATION,
          new Object[] { sJmol, minStatus, minSteps, minEnergy, minEnergyDiff });
  }
  
  synchronized void setScriptStatus(String strStatus, String statusMessage,
                                    int msWalltime,
                                    String strErrorMessageUntranslated) {
    // only allow trapping of script information of type 0

    if (msWalltime < -1) {
      int iscript = -2 - msWalltime;
      setStatusChanged("scriptStarted", iscript, statusMessage, false);
      strStatus = "script " + iscript + " started";
    } else if (strStatus == null) {
      return;
    }
    String sJmol = (msWalltime == 0 ? jmolScriptCallback(JmolConstants.CALLBACK_SCRIPT)
        : null);
    boolean isScriptCompletion = (strStatus == ScriptManager.SCRIPT_COMPLETED);

    if (recordStatus("script")) {
      boolean isError = (strErrorMessageUntranslated != null);
      setStatusChanged((isError ? "scriptError" : "scriptStatus"), 0,
          strStatus, false);
      if (isError || isScriptCompletion)
        setStatusChanged("scriptTerminated", 1, "Jmol script terminated"
            + (isError ? " unsuccessfully: " + strStatus : " successfully"),
            false);
    }

    Object[] data;
    if (isScriptCompletion && viewer.getMessageStyleChime()
        && viewer.getDebugScript()) {
      data = new Object[] { null, "script <exiting>", statusMessage,
          new Integer(-1), strErrorMessageUntranslated };
      if (notifyEnabled(JmolConstants.CALLBACK_SCRIPT))
        jmolCallbackListener
            .notifyCallback(JmolConstants.CALLBACK_SCRIPT, data);
      processScript(data);
      strStatus = "Jmol script completed.";
    }
    data = new Object[] { sJmol, strStatus, statusMessage,
        new Integer(isScriptCompletion ? -1 : msWalltime),
        strErrorMessageUntranslated };
    if (notifyEnabled(JmolConstants.CALLBACK_SCRIPT))
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_SCRIPT, data);
    processScript(data);
  }

  private void processScript(Object[] data) {
    int msWalltime = ((Integer) data[3]).intValue();
    // general message has msWalltime = 0
    // special messages have msWalltime < 0
    // termination message has msWalltime > 0 (1 + msWalltime)
    // "script started"/"pending"/"script terminated"/"script completed"
    // do not get sent to console
    
    if (viewer.scriptEditor != null) {
      if (msWalltime > 0) {
        // termination -- button legacy
        viewer.scriptEditor.notifyScriptTermination();
      } else if (msWalltime < 0) {
        if (msWalltime == -2)
          viewer.scriptEditor.notifyScriptStart();
      } else if (viewer.scriptEditor.isVisible()
          && ((String) data[2]).length() > 0) {
        viewer.scriptEditor.notifyContext((ScriptContext) viewer.getProperty(
            "DATA_API", "scriptContext", null), data);
      }
    }

    if (viewer.appConsole != null) {
      if (msWalltime == 0) {
        String strInfo = (data == null || data[1] == null ? null : data[1]
            .toString());
        viewer.appConsole.sendConsoleMessage(strInfo);
      }
    }
  }
  
  private int minSyncRepeatMs = 100;
  boolean syncingScripts = false;
  boolean syncingMouse = false;
  boolean doSync() {
    return (isSynced && drivingSync && !syncDisabled);
  }
  
  synchronized void setSync(String mouseCommand) {
    if (syncingMouse) {
      if (mouseCommand != null)
        syncSend(mouseCommand, "*");
    } else if (!syncingScripts)
      syncSend("!" + viewer.getMoveToText(minSyncRepeatMs / 1000f), "*");
  }

  boolean drivingSync = false;
  boolean isSynced = false;
  boolean syncDisabled = false;
  boolean stereoSync = false;
  
  final static int SYNC_OFF = 0;
  final static int SYNC_DRIVER = 1;
  final static int SYNC_SLAVE = 2;
  final static int SYNC_DISABLE = 3;
  final static int SYNC_ENABLE = 4;
  final static int SYNC_STEREO = 5;
  
  void setSyncDriver(int syncMode) {
 
    // -1 slave   turn off driving, but not syncing
    //  0 off
    //  1 driving on as driver
    //  2 sync    turn on, but set as slave
    //  5 stereo
    //System.out.println(viewer.getHtmlName() +" setting mode=" + syncMode);
    if (stereoSync && syncMode != SYNC_ENABLE) {
      syncSend(Viewer.SYNC_NO_GRAPHICS_MESSAGE, "*");
      stereoSync = false;
    }
    switch (syncMode) {
    case SYNC_ENABLE:
      if (!syncDisabled)
        return;
      syncDisabled = false;
      break;
    case SYNC_DISABLE:
      syncDisabled = true;
      break;
    case SYNC_STEREO:
      drivingSync = true;
      isSynced = true;
      stereoSync = true;
      break;
    case SYNC_DRIVER:
      drivingSync = true;
      isSynced = true;
      break;
    case SYNC_SLAVE:
      drivingSync = false;
      isSynced = true;
      break;
    default:
      drivingSync = false;
      isSynced = false;
    }
    if (Logger.debugging) {
      Logger.debug(
          viewer.getHtmlName() + " sync mode=" + syncMode +
          "; synced? " + isSynced + "; driving? " + drivingSync + "; disabled? " + syncDisabled);
    }
  }

  void syncSend(String script, String appletName) {
    // no jmolscript option for syncSend
    if (jmolStatusListener != null)
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_SYNC,
          new Object[] { null, script, appletName });
  }
  
  int getSyncMode() {
    return (!isSynced ? SYNC_OFF : drivingSync ? SYNC_DRIVER : SYNC_SLAVE);
  }
  
  synchronized void showUrl(String urlString) {
    if (jmolStatusListener != null)
      jmolStatusListener.showUrl(urlString);
  }

  synchronized void clearConsole() {
    if (viewer.appConsole != null) {
      viewer.appConsole.sendConsoleMessage(null);
    }
    if (jmolStatusListener != null)
      jmolCallbackListener.notifyCallback(JmolConstants.CALLBACK_MESSAGE, null);
  }

  float[][] functionXY(String functionName, int nX, int nY) {
    return (jmolStatusListener == null ? new float[Math.abs(nX)][Math.abs(nY)] :
      jmolStatusListener.functionXY(functionName, nX, nY));
  }
  
  float[][][] functionXYZ(String functionName, int nX, int nY, int nZ) {
    return (jmolStatusListener == null ? new float[Math.abs(nX)][Math.abs(nY)][Math.abs(nY)] :
      jmolStatusListener.functionXYZ(functionName, nX, nY, nZ));
  }
  
  String jsEval(String strEval) {
    return (jmolStatusListener == null ? "" : jmolStatusListener.eval(strEval));
  }

  /**
   * 
   * @param fileName
   * @param type
   * @param text_or_bytes
   * @param quality
   * @return null (canceled) or a message starting with OK or an error message
   */
  String createImage(String fileName, String type, Object text_or_bytes,
                     int quality) {
    if (fileName == null) // check for embedding program action
      return (jmolStatusListener == null ? null : jmolStatusListener
          .createImage(null, type, text_or_bytes, quality));
    if (fileName != null && fileName.startsWith("?")) {
      fileName = fileName.substring(1);
      if (!viewer.isSignedApplet()) {
        fileName = dialogAsk(quality == Integer.MIN_VALUE ? "save"
            : "saveImage", fileName);
      }
    }
    return (fileName == null // user canceled operation 
        || jmolStatusListener == null ? null : jmolStatusListener.createImage(
        fileName, type, text_or_bytes, quality));
  }

  Hashtable getRegistryInfo() {
    /* 

     //note that the following JavaScript retrieves the registry:
     
        var registry = jmolGetPropertyAsJavaObject("appletInfo").get("registry")
      
     // and the following code then retrieves an array of applets:
     
        var AppletNames = registry.keySet().toArray()
      
     // and the following sends commands to an applet in the registry:
      
        registry.get(AppletNames[0]).script("background white")
        
     */
    return (jmolStatusListener == null ? null : jmolStatusListener.getRegistryInfo());
  }

  String dialogAsk(String type, String fileName) {
    if (jmolStatusListener != null)
      return jmolStatusListener.dialogAsk(type, fileName);
    return "";
  }

}

