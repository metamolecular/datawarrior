/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-07-01 05:39:15 +0200 (mer., 01 juil. 2009) $
 * $Revision: 11162 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development Team
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

import org.jmol.util.BinaryDocument;
import org.jmol.util.CompoundDocument;
import org.jmol.util.TextFormat;
import org.jmol.util.ZipUtil;

import org.jmol.util.Logger;

import org.jmol.api.JmolFileReaderInterface;
import org.jmol.api.JmolViewer;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.MalformedURLException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import java.util.Hashtable;

/* ***************************************************************
 * will not work with applet
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.util.Enumeration;
 import org.openscience.jmol.io.ChemFileReader;
 import org.openscience.jmol.io.ReaderFactory;
 */

public class FileManager {

  protected Viewer viewer;
  private String openErrorMessage;

  // for applet proxy
  private URL appletDocumentBase = null;
  private URL appletCodeBase = null; //unused currently
  private String appletProxy;

  // for expanding names into full path names
  //private boolean isURL;
  private String nameAsGiven = "zapped";
  private String fullPathName;
  private String fileName;
  private String fileType;

  private String inlineData;

  String getInlineData(int iData) {
    return (iData < 0 ? inlineData : "");//iData < inlineDataArray.length ? inlineDataArray[iData] : "");  
  }

  private String loadScript;

  FileReaderThread fileReaderThread;
  FilesReaderThread filesReaderThread;
  private DOMReaderThread aDOMReaderThread;

  FileManager(Viewer viewer) {
    this.viewer = viewer;
    clear();
  }

  String getState(StringBuffer sfunc) {
    StringBuffer commands = new StringBuffer();
    if (sfunc != null) {
      sfunc.append("  _setFileState;\n");
      commands.append("function _setFileState() {\n\n");
    }
    commands.append(loadScript);
    if (viewer.getModelSetFileName().equals("zapped"))
      commands.append("  zap;\n");
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  String getFileTypeName(String fileName) {
    int pt = fileName.indexOf("::");
    if (pt >= 0)
      return fileName.substring(0, pt);
    Object br = getUnzippedBufferedReaderOrErrorMessageFromName(fileName, true,
        false, true, true);
    if (br instanceof BufferedReader)
      return viewer.getModelAdapter().getFileTypeName((BufferedReader) br);
    if (br instanceof ZipInputStream) {
      String zipDirectory = getZipDirectoryAsString(fileName);
      return viewer.getModelAdapter().getFileTypeName(
          getBufferedReaderForString(zipDirectory));
    }
    if (br instanceof String[]) {
      return ((String[]) br)[0];
    }
    return null;
  }

  void clear() {
    setLoadScript("", false);
    fullPathName = fileName = nameAsGiven = "zapped";
  }

  String getLoadScript() {
    return loadScript;
  }

  private void setLoadScript(String script, boolean isAppend) {
    if (loadScript == null || !isAppend)
      loadScript = "";
    loadScript += viewer.getLoadState();
    addLoadScript(script);
  }

  void addLoadScript(String script) {
    if (script == null)
      return;
    if (script.equals("-")) {
      loadScript = "";
      return;
    }
    loadScript += "  " + script + ";\n";
  }

  
  /*
   * note -- getAtomSetCollectionFromXXX methods
   * were "openXXX" before refactoring 11/29/2008 -- BH
   * 
   * The problem was that while they did open the file, they
   * (mostly) also closed them, and this was confusing.
   * 
   * The term "clientFile" was replaced by "atomSetCollection"
   * here because that's what it is --- an AtomSetCollection,
   * not a file. The file is closed at this point. What is
   * returned is the atomSetCollection object.
   * 
   * One could say this is just semantics, but there were
   * subtle bugs here, where readers were not always being 
   * closed explicitly. In the process of identifying Out of
   * Memory Errors, I felt it was necessary to clarify all this.
   * 
   * Apologies to those who feel the original clientFile notation
   * was more generalizable or understandable. 
   * 
   */
  void createAtomSetCollectionFromFile(String name, Hashtable htParams,
                                    String loadScript, boolean isAppend) {
    if (htParams.get("atomDataOnly") == null)
      setLoadScript(loadScript, isAppend);
    int pt = name.indexOf("::");
    nameAsGiven = (pt >= 0 ? name.substring(pt + 2) : name);
    fileType = (pt >= 0 ? name.substring(0, pt) : null);
    Logger.info("\nFileManager.getAtomSetCollectionFromFile(" + nameAsGiven
        + ") //" + name);
    openErrorMessage = fullPathName = fileName = null;
    String[] names = classifyName(nameAsGiven, true);
    if (names == null)
      return;
    setNames(names);
    htParams.put("fullPathName", (fileType == null ? "" : fileType + "::")
        + fullPathName.replace('\\', '/'));
    if (openErrorMessage != null) {
      Logger.error("file ERROR: " + openErrorMessage);
      return;
    }
    if (viewer.getMessageStyleChime() && viewer.getDebugScript())
      viewer.scriptStatus("Requesting " + fullPathName);
    fileReaderThread = new FileReaderThread(fullPathName, nameAsGiven,
        fileType, null, htParams);
    fileReaderThread.run();
  }

  void createAtomSetCollectionFromFiles(String modelName, String[] fileNames,
                                     String loadScript, boolean isAppend,
                                     Hashtable htParams) {
    setLoadScript(loadScript, isAppend);
    String[] fullPathNames = new String[fileNames.length];
    String[] namesAsGiven = new String[fileNames.length];
    String[] fileTypes = new String[fileNames.length];
    for (int i = 0; i < fileNames.length; i++) {
      int pt = fileNames[i].indexOf("::");
      nameAsGiven = (pt >= 0 ? fileNames[i].substring(pt + 2) : fileNames[i]);
      fileType = (pt >= 0 ? fileNames[i].substring(0, pt) : null);
      openErrorMessage = fullPathName = fileName = null;
      String[] thenames = classifyName(nameAsGiven, true);
      if (thenames == null)
        return;
      setNames(thenames);
      if (openErrorMessage != null) {
        Logger.error("file ERROR: " + openErrorMessage);
        return;
      }
      fullPathNames[i] = fullPathName;
      fileNames[i] = fullPathName.replace('\\', '/');
      fileTypes[i] = fileType;
      namesAsGiven[i] = nameAsGiven;
    }

    fullPathName = fileName = nameAsGiven = modelName;
    inlineData = "";
    //inlineDataArray = null;
    filesReaderThread = new FilesReaderThread(fullPathNames, namesAsGiven,
        fileTypes, null, htParams);
    filesReaderThread.run();
  }

  void createAtomSetCollectionFromString(String strModel, Hashtable htParams,
                                      boolean isAppend) {
    String tag = (isAppend ? "append" : "model");
    String script = "data \"" + tag + " inline\"\n" + strModel + "end \"" + tag
        + " inline\";";
    setLoadScript(script, isAppend);
    Logger.info("FileManager.getAtomSetCollectionFromString()");
    openErrorMessage = null;
    fullPathName = fileName = "string";
    inlineData = strModel;
    //inlineDataArray = null;
    fileReaderThread = new FileReaderThread("string", "string", null,
        getBufferedReaderForString(strModel), htParams);
    fileReaderThread.run();
  }

  void createAtomSeCollectionFromStrings(String[] arrayModels, Hashtable htParams,
                                      boolean isAppend) {
    String oldSep = "\"" + viewer.getDataSeparator() + "\"";
    String tag = "\"" + (isAppend ? "append" : "model") + " inline\"";
    String script = "set dataSeparator \"~~~next file~~~\";\ndata " + tag;
    for (int i = 0; i < arrayModels.length; i++) {
      if (i > 0)
        script += "~~~next file~~~";
      script += arrayModels[i];
    }
    script += "end " + tag + ";set dataSeparator " + oldSep;
    setLoadScript(script, isAppend);
    Logger.info("FileManager.getAtomSetCollectionFromStrings(string[])");
    openErrorMessage = null;
    fullPathName = fileName = "string[]";
    inlineData = "";
    //just too complicated and space-absorbing inlineDataArray = arrayModels;
    String[] fullPathNames = new String[arrayModels.length];
    StringReader[] readers = new StringReader[arrayModels.length];
    for (int i = 0; i < arrayModels.length; i++) {
      fullPathNames[i] = "string[" + i + "]";
      readers[i] = new StringReader(arrayModels[i]);
    }
    filesReaderThread = new FilesReaderThread(fullPathNames, fullPathNames,
        null, readers, null);
    filesReaderThread.run();
  }

  void createAtomSetCollectionFromDOM(Object DOMNode) {
    openErrorMessage = null;
    fullPathName = fileName = "JSNode";
    inlineData = "";
    //inlineDataArray = null;
    aDOMReaderThread = new DOMReaderThread(DOMNode);
    aDOMReaderThread.run();
  }

  /**
   * not used in Jmol project -- will close reader
   * 
   * @param fullPathName
   * @param name
   * @param reader
   */
  void createAtomSetCollectionFromReader(String fullPathName, String name,
                                      Reader reader) {
    openErrorMessage = null;
    this.fullPathName = fullPathName;
    fileName = name;
    fileType = null;
    fileReaderThread = new FileReaderThread(fullPathName, fullPathName,
        fileType, new BufferedReader(reader), null);
    fileReaderThread.run();
  }

  static boolean isGzip(InputStream is) throws Exception {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
  }

  public Object getFileAsBytes(String name) {
    //?? used by eval of "WRITE FILE"
    // will be full path name
    if (name == null)
      return null;
    String[] subFileList = null;
    if (name.indexOf("|") >= 0)
      name = (subFileList = TextFormat.split(name, "|"))[0];
    Object t = getInputStreamOrErrorMessageFromName(name, false);
    if (t instanceof String)
      return "Error:" + t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
      InputStream is = bis;      
      Object bytes = (ZipUtil.isZipFile(is) && subFileList != null
          && 1 < subFileList.length ? ZipUtil.getZipFileContentsAsBytes(is,
          subFileList, 1) : ZipUtil.getStreamAsBytes(bis));
      is.close();
      return bytes;
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  /**
   * 
   * @param data
   *          [0] initially path name, but returned as full path name; [1]file
   *          contents (directory listing for a ZIP/JAR file) or error string
   * @param nBytesMax
   * @param doSpecialLoad 
   * @return true if successful; false on error
   */

  boolean getFileDataOrErrorAsString(String[] data, int nBytesMax, boolean doSpecialLoad) {
    data[1] = "";
    String name = data[0];
    if (name == null)
      return false;
    Object t = getBufferedReaderOrErrorMessageFromName(name, data, false, doSpecialLoad);
    if (t instanceof String) {
      data[1] = (String) t;
      return false;
    }
    try {
      BufferedReader br = (BufferedReader) t;
      StringBuffer sb = new StringBuffer(8192);
      String line;
      if (nBytesMax == Integer.MAX_VALUE) {
        while ((line = br.readLine()) != null)
          sb.append(line).append('\n');
      } else {
        int n = 0;
        int len;
        while (n < nBytesMax && (line = br.readLine()) != null) {
          if (nBytesMax - n < (len = line.length()) + 1)
            line = line.substring(0, nBytesMax - n - 1);
          sb.append(line).append('\n');
          n += len + 1;
        }
      }
      br.close();
      data[1] = sb.toString();
      return true;
    } catch (Exception ioe) {
      data[1] = ioe.getMessage();
      return false;
    }
  }

  Object getFileAsImage(String name, Hashtable htParams) {
    if (name == null)
      return "";
    String[] names = classifyName(name, true);
    if (names == null)
      return "cannot read file name: " + name;
    Image image = null;
    //try {
    fullPathName = names[0].replace('\\', '/');
    if (urlTypeIndex(fullPathName) >= 0)
      try {
        image = Toolkit.getDefaultToolkit().createImage(new URL(fullPathName));
      } catch (Exception e) {
        return "bad URL: " + fullPathName;
      }
    else
      image = Toolkit.getDefaultToolkit().createImage(fullPathName);
    try {
      MediaTracker mediaTracker = new MediaTracker(viewer.getDisplay());
      mediaTracker.addImage(image, 0);
      mediaTracker.waitForID(0);
      /* SUN but here for malformed URL - can't trap
       Uncaught error fetching image:
       java.lang.NullPointerException
       at sun.net.www.ParseUtil.toURI(Unknown Source)
       at sun.net.www.protocol.http.HttpURLConnection.plainConnect(Unknown Source)
       at sun.net.www.protocol.http.HttpURLConnection.connect(Unknown Source)
       at sun.net.www.protocol.http.HttpURLConnection.getInputStream(Unknown Source)
       at sun.awt.image.URLImageSource.getDecoder(Unknown Source)
       at sun.awt.image.InputStreamImageSource.doFetch(Unknown Source)
       at sun.awt.image.ImageFetcher.fetchloop(Unknown Source)
       at sun.awt.image.ImageFetcher.run(Unknown Source)
       */
    } catch (Exception e) {
      return e.getMessage() + " opening " + fullPathName;
    }
    if (image.getWidth(null) < 1)
      return "invalid or missing image " + fullPathName;
    htParams.put("fullPathName", fullPathName);
    return image;
  }

  private String getFileDataAsSections(String[] info, Hashtable fileData) {
    // load each file individually, but return files IN ORDER
    String header = info[1];
    StringBuffer sb = new StringBuffer();
    sb.append(fileData.get(fileData.get("OUTPUT")));
    for (int i = 2; i < info.length; i++) {
      String name = info[i];
      name = getFileDataAsSections(name, header, fileData);
      Logger.info("reading " + name);
      String s = (String) fileData.get(name);
      sb.append(s);
    }
    return sb.toString();
  }

  /**
   * delivers file contents and directory listing for a ZIP/JAR file into sb
   * 
   * 
   * @param name
   * @param header
   * @param fileData
   * @return name of entry
   */
  private String getFileDataAsSections(String name, String header,
                                     Hashtable fileData) {
    if (name == null)
      return null;
    String[] subFileList = null;
    boolean asBinaryString = false;
    String name0 = name.replace('\\', '/');
    if (name.indexOf(":asBinaryString") >= 0) {
      asBinaryString = true;
      name = name.substring(0, name.indexOf(":asBinaryString"));
    }
    StringBuffer sb = null;
    if (fileData.containsKey(name0))
      return name0;
    if (name.indexOf("#JMOL_MODEL ") >= 0) {
      fileData.put(name0, name0 + "\n");
      return name0;
    }
    if (name.indexOf("|") >= 0) {
      name = (subFileList = TextFormat.split(name, "|"))[0];
    }
    BufferedInputStream bis = null;
    try {
      Object t = getInputStreamOrErrorMessageFromName(name, false);
      if (t instanceof String) {
        fileData.put(name0, (String) t + "\n");
        return name0;
      }
      bis = new BufferedInputStream((InputStream) t, 8192);
      if (CompoundDocument.isCompoundDocument(bis)) {
        CompoundDocument doc = new CompoundDocument(bis);
        doc.getAllData(name.replace('\\', '/'), "Molecule", fileData);
      } else if (ZipUtil.isZipFile(bis)) {
        ZipUtil.getAllData(bis, subFileList, name.replace('\\', '/'), "Molecule", fileData);
      } else if (asBinaryString) {
        // used for Spartan binary file reading
        BinaryDocument bd = new BinaryDocument();
        bd.setStream(bis, false);
        sb = new StringBuffer();
        //note -- these headers must match those in ZipUtil.getAllData and CompoundDocument.getAllData
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        try {
          while (true)
            sb.append(Integer.toHexString(((int) bd.readByte()) & 0xFF))
                .append(' ');
        } catch (Exception e1) {
          sb.append('\n');
        }
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      } else {
        BufferedReader br = new BufferedReader(new InputStreamReader(
            isGzip(bis) ? new GZIPInputStream(bis) : (InputStream) bis));
        String line;
        sb = new StringBuffer();
        if (header != null)
          sb.append("BEGIN Directory Entry " + name0 + "\n");
        while ((line = br.readLine()) != null) {
          sb.append(line);
          sb.append('\n');
        }
        br.close();
        if (header != null)
          sb.append("\nEND Directory Entry " + name0 + "\n");
        fileData.put(name0, sb.toString());
      }
    } catch (Exception ioe) {
      fileData.put(name0, ioe.getMessage());
    }
    if (bis != null)
      try {
        bis.close();
      } catch (Exception e) {
        //
      }
    if (!fileData.containsKey(name0))
      fileData.put(name0, "FILE NOT FOUND: " + name0 + "\n");
    return name0;
  }

  /**
   * after a file or other object has been opened and
   * the atom set collection has been created, this
   * method returns that atom set collection or an error message
   * and closes the file. 
   * 
   * @return string error or an AtomSetCollection
   */
  Object getAtomSetCollectionOrError() {
    Object atomSetCollection = null;
    if (fileReaderThread != null) {
      atomSetCollection = fileReaderThread.atomSetCollection;
      if (fileReaderThread.errorMessage != null)
        openErrorMessage = fileReaderThread.errorMessage;
    } else if (filesReaderThread != null) {
      atomSetCollection = filesReaderThread.atomSetCollection;
      if (filesReaderThread.errorMessage != null)
        openErrorMessage = filesReaderThread.errorMessage;
    } else if (aDOMReaderThread != null) {
      atomSetCollection = aDOMReaderThread.atomSetCollection;
      if (aDOMReaderThread.errorMessage != null)
        openErrorMessage = aDOMReaderThread.errorMessage;
    }
    if (openErrorMessage != null)
      return openErrorMessage;
    if (atomSetCollection == null)
      return "Client file is null loading:" + nameAsGiven;
    return atomSetCollection;
  }

  void deallocateReaderThreads() {
    fileReaderThread = null;
    filesReaderThread = null;
    aDOMReaderThread = null;
    openErrorMessage = null;
  }

  String getFullPathName() {
    return fullPathName != null ? fullPathName : nameAsGiven;
  }

  void setFileInfo(String[] fileInfo) {
    try {
      fullPathName = fileInfo[0];
      fileName = fileInfo[1];
      inlineData = fileInfo[2];
      loadScript = fileInfo[3];
    } catch (Exception e) {
      Logger.error("Exception saving file info: " + e.getMessage());
    }
  }

  String[] getFileInfo() {
    return new String[] { fullPathName, fileName, inlineData, loadScript };
  }

  String getFileName() {
    return fileName != null ? fileName : nameAsGiven;
  }

  String getAppletDocumentBase() {
    if (appletDocumentBase == null)
      return "";
    return appletDocumentBase.toString();
  }

  void setAppletContext(URL documentBase, URL codeBase, String jmolAppletProxy) {
    appletDocumentBase = documentBase;
    appletCodeBase = codeBase;
    Logger.info("appletDocumentBase=" + appletDocumentBase
        + "\nappletCodeBase=" + appletCodeBase);
    //    dumpDocumentBase("" + documentBase);
    appletProxy = jmolAppletProxy;
  }

  void setAppletProxy(String appletProxy) {
    this.appletProxy = (appletProxy == null || appletProxy.length() == 0 ? null
        : appletProxy);
  }

  private final static int URL_LOCAL = 3;
  private final static String[] urlPrefixes = { "http:", "https:", "ftp:",
      "file:" };

  private static int urlTypeIndex(String name) {
    for (int i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i])) {
        return i;
      }
    }
    return -1;
  }

  private void setNames(String[] names) {
    if (names == null)
      return;
    fullPathName = names[0];
    fileName = names[1];
  }

  
  static String shortNameOf(String name) {
    name = TextFormat.trim(name.replace('\\', '/'), "/");
    return name.substring(name.lastIndexOf("/") + 1);
  }
  /**
   * 
   * @param name
   * @param isFullLoad
   * @return [0] full path name, [1] file name without path, [2] full URL
   */
  private String[] classifyName(String name, boolean isFullLoad) {
    if (name == null)
      return null;
    if (name.startsWith("?")
        && (name = viewer.dialogAsk("load", name.substring(1))) == null) {
      if (isFullLoad)
        openErrorMessage = "#CANCELED#";
      return null;
    }
    File file = null;
    URL url = null;
    String[] names = null;
    if (name.indexOf("=") == 0)
      name = TextFormat.formatString(viewer.getLoadFormat(), "FILE", name
          .substring(1));
    if (name.indexOf(":") < 0 && name.indexOf("/") != 0)
      name = addDirectory(viewer.getDefaultDirectory(), name);
    if (appletDocumentBase != null) {
      // This code is only for the applet
      try {
        if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
          name = "file:/" + name;
        else if (name.indexOf("/") == 0
            && viewer.getBooleanProperty("_signedApplet"))
          name = "file:" + name;
        url = new URL(appletDocumentBase, name);
      } catch (MalformedURLException e) {
        if (isFullLoad)
          openErrorMessage = e.getMessage();
        return null;
      }
    } else {
      // This code is for the app
      if (urlTypeIndex(name) >= 0) {
        try {
          url = new URL(name);
        } catch (MalformedURLException e) {
          if (isFullLoad)
            openErrorMessage = e.getMessage();
          return null;
        }
      } else {
        file = new File(name);
        names = new String[] { file.getAbsolutePath(), file.getName(),
            "file:/" + file.getAbsolutePath().replace('\\', '/') };
      }
    }
    if (url != null) {
      names = new String[3];
      names[0] = names[2] = url.toString();
      names[1] = names[0].substring(names[0].lastIndexOf('/') + 1);
    }
    if (isFullLoad && (file != null || urlTypeIndex(names[0]) == URL_LOCAL)) {
      String path = (file == null ? TextFormat.trim(names[0].substring(5), "/")
          : names[0]);
      int pt = path.length() - names[1].length() - 1;
      if (pt > 0) {
        path = path.substring(0, pt);
        setLocalPath(viewer, path, true);
      }
    }
    return names;
  }

  String getDefaultDirectory(String name) {
    String[] names = classifyName(name, true);
    if (names == null)
      return "";
    name = fixPath(names[0]);
    return (names == null ? "" : name.substring(0, name.lastIndexOf("/")));
  }

  private String fixPath(String path) {
    path = path.replace('\\', '/');
    path = TextFormat.simpleReplace(path, "/./", "/");
    int pt = path.lastIndexOf("//") + 1;
    if (pt < 1)
      pt = path.indexOf(":/") + 1;
    if (pt < 1)
      pt = path.indexOf("/");
    String protocol = path.substring(0, pt);
    path = path.substring(pt);

    while ((pt = path.lastIndexOf("/../")) >= 0) {
      int pt0 = path.substring(0, pt).lastIndexOf("/");
      if (pt0 < 0)
        return TextFormat.simpleReplace(protocol + path, "/../", "/");
      path = path.substring(0, pt0) + path.substring(pt + 3);
    }
    if (path.length() == 0)
      path = "/";
    return protocol + path;
  }

  public String getFullPath(String name, boolean addUrlPrefix) {
    String[] names = classifyName(name, false);
    return (names == null ? "" : addUrlPrefix ? names[2] : names[0].replace(
        '\\', '/'));
  }

  private final static String[] urlPrefixPairs = { "http:", "http://", "www.",
      "http://www.", "https:", "https://", "ftp:", "ftp://", "file:",
      "file:///" };

  public static String getLocalUrl(File file) {
    // entering a url on a file input box will be accepted,
    // but cause an error later. We can fix that...
    // return null if there is no problem, the real url if there is
    if (file.getName().startsWith("="))
      return file.getName();
    String path = file.getAbsolutePath().replace('\\', '/');
    for (int i = 0; i < urlPrefixPairs.length; i++)
      if (path.indexOf(urlPrefixPairs[i]) == 0)
        return null;
    // looking for /xxx/xxxx/file://...
    for (int i = 0; i < urlPrefixPairs.length; i += 2)
      if (path.indexOf(urlPrefixPairs[i]) > 0)
        return urlPrefixPairs[i + 1]
            + TextFormat.trim(path.substring(path.indexOf(urlPrefixPairs[i])
                + urlPrefixPairs[i].length()), "/");
    return null;
  }

  public static File getLocalDirectory(JmolViewer viewer, boolean forDialog) {
    String localDir = (String) viewer
        .getParameter(forDialog ? "currentLocalPath" : "defaultDirectoryLocal");
    if (localDir.length() == 0 && forDialog)
      localDir = (String) viewer.getParameter("defaultDirectoryLocal");
    if (localDir.length() == 0)
      return (viewer.isApplet() ? null : new File(System.getProperty("user.dir")));
    if (viewer.isApplet() && localDir.indexOf("file:/") == 0)
        localDir =setLocalPathForWritingFile(viewer, localDir);
    
    File f = new File(localDir);
    return f.isDirectory() ? f : f.getParentFile();
  }

  public static void setLocalPath(JmolViewer viewer, String path,
                                  boolean forDialog) {
    while (path.endsWith("/") || path.endsWith("\\"))
      path = path.substring(0, path.length() - 1);
    viewer.setStringProperty("currentLocalPath", path);
    if (!forDialog)
      viewer.setStringProperty("defaultDirectoryLocal", path);
  }

  public static String setLocalPathForWritingFile(JmolViewer viewer, String file) {
    if (file.indexOf("file:/") == 0)
      return file.substring(6);
    if (file.indexOf("/") == 0 || file.indexOf(":") >= 0)
      return file;
    File dir = getLocalDirectory(viewer, false);
    return (dir == null ? file : dir.toString().replace('\\', '/') + "/" + file);
  }

  private static String addDirectory(String defaultDirectory, String name) {
    if (defaultDirectory.length() == 0)
      return name;
    char ch = (name.length() > 0 ? name.charAt(0) : ' ');
    String s = defaultDirectory.toLowerCase();
    if ((s.endsWith(".zip") || s.endsWith(".tar")) && ch != '|' && ch != '/')
      defaultDirectory += "|";
    return defaultDirectory
        + (ch == '/'
            || ch == '/'
            || (ch = defaultDirectory.charAt(defaultDirectory.length() - 1)) == '|'
            || ch == '/' ? "" : "/") + name;
  }

  Object getInputStreamOrErrorMessageFromName(String name, boolean showMsg) {
    return getInputStream(name, showMsg, appletDocumentBase, appletProxy);
  }

  public static Object getInputStream(String name, boolean showMsg,
                                      URL appletDocumentBase, String appletProxy) {
    String errorMessage = null;
    int iurlPrefix;
    for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0;)
      if (name.startsWith(urlPrefixes[iurlPrefix]))
        break;
    boolean isURL = (iurlPrefix >= 0);
    boolean isApplet = (appletDocumentBase != null);
    InputStream in = null;
    int length;
    try {
      if (isApplet || isURL) {
        if (isApplet && isURL && appletProxy != null)
          name = appletProxy + "?url=" + URLEncoder.encode(name, "utf-8");
        URL url = (isApplet ? new URL(appletDocumentBase, name) : new URL(name));
        name = url.toString();
        if (showMsg)
          Logger.info("FileManager opening " + url.toString());
        URLConnection conn = url.openConnection();
        length = conn.getContentLength();
        in = conn.getInputStream();
      } else {
        if (showMsg)
          Logger.info("FileManager opening " + name);
        File file = new File(name);
        length = (int) file.length();
        in = new FileInputStream(file);
      }
      return new MonitorInputStream(in, length);
    } catch (Exception e) {
      try {
        if (in != null)
          in.close();
      } catch (IOException e1) {
        // TODO
      }
      errorMessage = "" + e;
    }
    return errorMessage;
  }

  public static BufferedReader getBufferedReaderForString(String string) {
    return new BufferedReader(new StringReader(string));
  }

  Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn,
                                                 boolean isBinary, boolean doSpecialLoad) {
    String[] names = classifyName(name, true);
    if (names == null)
      return "cannot read file name: " + name;
    if (fullPathNameReturn != null)
      fullPathNameReturn[0] = names[0].replace('\\', '/');
    return getUnzippedBufferedReaderOrErrorMessageFromName(names[0], false,
        isBinary, false, doSpecialLoad);
  }

  Object getUnzippedBufferedReaderOrErrorMessageFromName(
                                                         String name,
                                                         boolean allowZipStream,
                                                         boolean asInputStream,
                                                         boolean isTypeCheckOnly, boolean doSpecialLoad) {
    String[] subFileList = null;
    String[] info = (doSpecialLoad ? viewer.getModelAdapter().specialLoad(name, "filesNeeded?") : null);
    if (info != null) {
      if (isTypeCheckOnly)
        return info;
      if (info[2] != null) {
        String header = info[1];
        Hashtable fileData = new Hashtable();
        if (info.length == 3) {
          // we need information from the output file, info[2]
          String name0 = getFileDataAsSections(info[2], header, fileData);
          fileData.put("OUTPUT", name0);
          info = viewer.getModelAdapter().specialLoad(name,
              (String) fileData.get(name0));
          if (info.length == 3) {
            // might have a second option
            name0 = getFileDataAsSections(info[2], header, fileData);
            fileData.put("OUTPUT", name0);
            info = viewer.getModelAdapter().specialLoad(info[1],
                (String) fileData.get(name0));
          }
        }
        return getBufferedReaderForString(getFileDataAsSections(info, fileData));
      }
      // continuing...
      // here, for example, for an SPT file load that is not just a type check
      // (type check is only for application file opening and drag-drop to
      // determine if
      // script or load command should be used)
    }
    if (name.indexOf("|") >= 0)
      name = (subFileList = TextFormat.split(name, "|"))[0];
    Object t = getInputStreamOrErrorMessageFromName(name, true);
    if (t instanceof String)
      return t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream) t, 8192);
      InputStream is = bis;
      if (CompoundDocument.isCompoundDocument(is)) {
        CompoundDocument doc = new CompoundDocument(bis);
        return getBufferedReaderForString(doc.getAllData("Molecule").toString());
      } else if (isGzip(is)) {
        do {
          is = new BufferedInputStream(new GZIPInputStream(is));
        } while (isGzip(is));
      } else if (ZipUtil.isZipFile(is)) {
        if (allowZipStream)
          return new ZipInputStream(bis);
        if (asInputStream)
          return (InputStream) ZipUtil.getZipFileContents(is, subFileList, 1,
              true);
        // danger -- converting bytes to String here.
        // we lose 128-156 or so.
        String s = (String) ZipUtil.getZipFileContents(is, subFileList, 1,
            false);
        is.close();
        return getBufferedReaderForString(s);
      }
      if (asInputStream)
        return is;
      return new BufferedReader(new InputStreamReader(is));
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  String[] getZipDirectory(String fileName, boolean addManifest) {
    return ZipUtil.getZipDirectoryAndClose(
        (InputStream) getInputStreamOrErrorMessageFromName(fileName, false),
        addManifest);
  }

  String getZipDirectoryAsString(String fileName) {
    return ZipUtil
        .getZipDirectoryAsStringAndClose((InputStream) getInputStreamOrErrorMessageFromName(
            fileName, false));
  }

  class DOMReaderThread implements Runnable {
    //boolean terminated;
    String errorMessage;
    Object aDOMNode;
    Object atomSetCollection;

    DOMReaderThread(Object DOMNode) {
      this.aDOMNode = DOMNode;
    }

    public void run() {
      atomSetCollection = viewer.getModelAdapter().getAtomSetCollectionFromDOM(
          aDOMNode);
      errorMessage = null;
      //terminated = true;
    }
  }

  class FileReaderThread implements Runnable {
    //boolean terminated;
    String errorMessage;
    String fullPathNameInThread;
    String nameAsGivenInThread;
    String fileTypeInThread;
    Object atomSetCollection;
    BufferedReader reader;
    Hashtable htParams;

    FileReaderThread(String name, String nameAsGiven, String type,
        BufferedReader reader, Hashtable htParams) {
      fullPathNameInThread = name;
      nameAsGivenInThread = nameAsGiven;
      fileTypeInThread = type;
      this.reader = reader;
      this.htParams = htParams;
    }

    public void run() {
      if (reader != null) {
        createAtomSetCollection();
      } else {
        String name = fullPathNameInThread;
        String[] subFileList = null;
        Object t = getUnzippedBufferedReaderOrErrorMessageFromName(name, true,
            false, false, true);
        if (name.indexOf("|") >= 0)
          name = (subFileList = TextFormat.split(name, "|"))[0];
        if (t instanceof BufferedReader) {
          reader = (BufferedReader) t;
          createAtomSetCollection();
        } else if (t instanceof ZipInputStream) {
          if (subFileList != null)
            htParams.put("subFileList", subFileList);
          createAtomSetCollection(name, (ZipInputStream) t);
        } else {
          errorMessage = (t == null ? "error opening:" + nameAsGivenInThread
              : (String) t);
        }
      }
      if (errorMessage != null) {
        Logger.error("file ERROR: " + fullPathNameInThread + "\n"
            + errorMessage);
      }
      //terminated = true;
    }

    private void createAtomSetCollection(String fileName, ZipInputStream zis) {
      String[] zipDirectory = getZipDirectory(fileName, true);
      Object atomSetCollectionOrError = viewer.getModelAdapter()
          .getAtomSetCollectionOrBufferedReaderFromZip(zis, fileName,
              zipDirectory, htParams, false);
      if (atomSetCollectionOrError instanceof String)
        errorMessage = (String) atomSetCollectionOrError;
      else
        atomSetCollection = atomSetCollectionOrError;
      try {
        zis.close();
      } catch (Exception e) {
        //
      }
    }

    private void createAtomSetCollection() {
      Object atomSetCollectionOrError = viewer.getModelAdapter()
          .getAtomSetCollectionFromReader(fullPathNameInThread,
              fileTypeInThread, reader, htParams);
      if (atomSetCollectionOrError instanceof String)
        errorMessage = (String) atomSetCollectionOrError;
      else
        atomSetCollection = atomSetCollectionOrError;
    }
  }

  class FilesReaderThread implements Runnable, JmolFileReaderInterface {
    //boolean terminated;
    String errorMessage;
    private String[] fullPathNamesInThread;
    private String[] namesAsGivenInThread;
    private String[] fileTypesInThread;
    Object atomSetCollection;
    private Reader[] stringReaders;
    private Hashtable[] htParamsSet;
    private Hashtable htParams;

    FilesReaderThread(String[] name, String[] nameAsGiven, String[] types,
        Reader[] readers, Hashtable htParams) {
      fullPathNamesInThread = name;
      namesAsGivenInThread = nameAsGiven;
      fileTypesInThread = types;
      stringReaders = readers;
      this.htParams = htParams;
    }

    public void run() {
      if (stringReaders != null) {
        createAtomSetCollection();
        stringReaders = null;
      } else {
        htParamsSet = new Hashtable[fullPathNamesInThread.length];
        for (int i = 0; i < htParamsSet.length; i++)
          htParamsSet[i] = htParams; //for now, just one common parameter set
        Object atomSetCollectionOrError = viewer.getModelAdapter()
            .getAtomSetCollectionFromReaders(this, fullPathNamesInThread,
                fileTypesInThread, htParamsSet);
        if (atomSetCollectionOrError instanceof String)
          errorMessage = (String) atomSetCollectionOrError;
        else
          atomSetCollection = atomSetCollectionOrError;
        if (errorMessage != null)
          Logger.error("file ERROR: " + errorMessage);
      }
    }

    private void createAtomSetCollection() {
      Object atomSetCollectionOrError = viewer.getModelAdapter()
          .getAtomSetCollectionFromReaders(this, fullPathNamesInThread,
              fileTypesInThread, null);
      if (atomSetCollectionOrError == null)
        return; // errorMessage has been set in getBufferedReader(int i);
      if (atomSetCollectionOrError instanceof String)
        errorMessage = (String) atomSetCollectionOrError;
      else
        atomSetCollection = atomSetCollectionOrError;
    }

    /**
     * called by SmartJmolAdapter to request another buffered reader,
     * rather than opening all the readers at once.
     * 
     * @param i   the reader index
     * @return    a BufferedReader or null in the case of an error
     * 
     */
    public BufferedReader getBufferedReader(int i) {
      if (stringReaders != null)
        return new BufferedReader(stringReaders[i]);
      String name = fullPathNamesInThread[i];
      String[] subFileList = null;
      Hashtable htParams = htParamsSet[0]; // for now -- just reusing this
      htParams.remove("subFileList");
      if (name.indexOf("|") >= 0)
        name = (subFileList = TextFormat.split(name, "|"))[0];
      Object t = getUnzippedBufferedReaderOrErrorMessageFromName(name, true,
          false, false, true);
      if (t instanceof ZipInputStream) {
        if (subFileList != null)
          htParams.put("subFileList", subFileList);
        String[] zipDirectory = getZipDirectory(name, true);
        InputStream is = new BufferedInputStream(
            (InputStream) getInputStreamOrErrorMessageFromName(name, false),
            8192);
        t = viewer.getModelAdapter()
            .getAtomSetCollectionOrBufferedReaderFromZip(is, name,
                zipDirectory, htParams, true);
      }
      if (t instanceof BufferedReader)
        return (BufferedReader) t;
      errorMessage = (t == null ? "error opening:" + namesAsGivenInThread[i]
          : (String) t);
      return null;
    }
  }
}

class MonitorInputStream extends FilterInputStream {
  int length;
  int position;
  int markPosition;
  int readEventCount;

  MonitorInputStream(InputStream in, int length) {
    super(in);
    this.length = length;
    this.position = 0;
  }

  public int read() throws IOException {
    ++readEventCount;
    int nextByte = super.read();
    if (nextByte >= 0)
      ++position;
    return nextByte;
  }

  public int read(byte[] b) throws IOException {
    ++readEventCount;
    int cb = super.read(b);
    if (cb > 0)
      position += cb;
    return cb;
  }

  public int read(byte[] b, int off, int len) throws IOException {
    ++readEventCount;
    int cb = super.read(b, off, len);
    if (cb > 0)
      position += cb;
    return cb;
  }

  public long skip(long n) throws IOException {
    long cb = super.skip(n);
    // this will only work in relatively small files ... 2Gb
    position = (int) (position + cb);
    return cb;
  }

  public void mark(int readlimit) {
    super.mark(readlimit);
    markPosition = position;
  }

  public void reset() throws IOException {
    position = markPosition;
    super.reset();
  }

  int getPosition() {
    return position;
  }

  int getLength() {
    return length;
  }

  int getPercentageRead() {
    return position * 100 / length;
  }

}
