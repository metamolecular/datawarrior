/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2006  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 *  02110-1301, USA.
 */

package org.jmol.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

  public static boolean isZipFile(String filePath) {
    try {
      URL url = new URL(filePath);
      URLConnection conn = url.openConnection();
      BufferedInputStream bis = new BufferedInputStream(conn.getInputStream(), 8192);
      boolean isOK = isZipFile(bis);
      bis.close();
      return isOK;
    } catch (Exception e) {
      //
    }
    return false;
  }
  
  public static boolean isZipFile(InputStream is) throws Exception {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x50 && abMagic[1] == (byte) 0x4B
        && abMagic[2] == (byte) 0x03 && abMagic[3] == (byte) 0x04);
  }

  public static boolean isZipFile(byte[] bytes) throws Exception {
    return (bytes.length > 4 
        && bytes[0] == 0x50  //PK<03><04> 
        && bytes[1] == 0x4B
        && bytes[2] == 0x03 
        && bytes[3] == 0x04);
  }

  public static ZipInputStream getStream(InputStream is) {
    return (is instanceof ZipInputStream ? (ZipInputStream) is 
        : is instanceof BufferedInputStream ? new ZipInputStream (is)
        : new ZipInputStream(new BufferedInputStream(is))); 
  }
  
  /**
   * reads a ZIP file and saves all data in a Hashtable
   * so that the files may be organized later in a different order. Also adds
   * a #Directory_Listing entry.
   * 
   * Files are bracketed by BEGIN Directory Entry and END Directory Entry lines, 
   * similar to CompoundDocument.getAllData.
   * 
   * @param is
   * @param subfileList
   * @param name0            prefix for entry listing 
   * @param binaryFileList   |-separated list of files that should be saved
   *                         as xx xx xx hex byte strings. The directory listing
   *                         is appended with ":asBinaryString"
   * @param fileData
   */
  public static void getAllData(InputStream is, String[] subfileList,
                                String name0, String binaryFileList,
                                Hashtable fileData) {
    ZipInputStream zis = getStream(is);
    ZipEntry ze;
    StringBuffer listing = new StringBuffer();
    binaryFileList = "|" + binaryFileList + "|";
    String prefix = TextFormat.join(subfileList, '/', 1);
    String prefixd = null;
    if (prefix != null) {
      prefixd = prefix.substring(0, prefix.indexOf("/") + 1);
      if (prefixd.length() == 0)
        prefixd = null;
    }
    try {
      while ((ze = zis.getNextEntry()) != null) {
        String name = ze.getName();
        if (prefix != null && prefixd != null
            && !(name.equals(prefix) || name.startsWith(prefixd)))
          continue;
        //System.out.println("ziputil: " + name);
        listing.append(name).append('\n');
        String sname = "|" + name.substring(name.lastIndexOf("/") + 1) + "|";
        boolean asBinaryString = (binaryFileList.indexOf(sname) >= 0);
        byte[] bytes = getZipEntryAsBytes(zis);
        String str;
        if (asBinaryString) {
          str = getBinaryStringForBytes(bytes);
          name += ":asBinaryString";
        } else {
          str = new String(bytes);
        }
        str = "BEGIN Directory Entry " + name + "\n" + str
            + "\nEND Directory Entry " + name + "\n";
        fileData.put(name0 + "|" + name, str);
      }
    } catch (Exception e) {
    }
    fileData.put("#Directory_Listing", listing.toString());
  }

  public static String getBinaryStringForBytes(byte[] bytes) {
    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < bytes.length; i++)
      ret.append(Integer.toHexString(((int) bytes[i]) & 0xFF))
          .append(' ');
    return ret.toString();
  }
  
  /**
   *  iteratively drills into zip files of zip files to extract file content
   *  or zip file directory. Also works with JAR files. 
   *  
   *  Does not return "__MACOS" paths
   * 
   * @param is
   * @param list
   * @param listPtr
   * @param asInputStream  for Pmesh
   * @return  directory listing or subfile contents
   */
  static public Object getZipFileContents(InputStream is, String[] list,
                                          int listPtr, boolean asInputStream) {
    StringBuffer ret;
    if (list == null || listPtr >= list.length)
      return getZipDirectoryAsStringAndClose(is);
    String fileName = list[listPtr];
    ZipInputStream zis = new ZipInputStream(is);
    ZipEntry ze;
    //System.out.println("fname=" + fileName);
    try {
      boolean isAll = (fileName.equals("."));
      if (isAll || fileName.lastIndexOf("/") == fileName.length() - 1) {
        ret = new StringBuffer();
        while ((ze = zis.getNextEntry()) != null) {
          String name = ze.getName();
          if (isAll || name.startsWith(fileName))
            ret.append(name).append('\n');
        }
        String str = ret.toString();
        if (asInputStream)
          return new StringBufferInputStream(str);
        return str;
      }
      boolean asBinaryString = false;
      if (fileName.indexOf(":asBinaryString") > 0) {
        fileName = fileName.substring(0, fileName.indexOf(":asBinaryString"));
        asBinaryString = true;
      }
      while ((ze = zis.getNextEntry()) != null) {
        if (!fileName.equals(ze.getName()))
          continue;
        byte[] bytes = getZipEntryAsBytes(zis);
        //System.out.println("ZipUtil::ZipEntry.name = " + ze.getName() + " " + bytes.length);
        if (isZipFile(bytes))
          return getZipFileContents(new BufferedInputStream(
              new ByteArrayInputStream(bytes)), list, ++listPtr, asInputStream);
        if (asInputStream)
          return new ByteArrayInputStream(bytes);
        if (asBinaryString) {
          ret = new StringBuffer();
          for (int i = 0; i < bytes.length; i++)
            ret.append(Integer.toHexString(((int)bytes[i]) & 0xFF)).append(' ');
          return ret.toString();
        }
      return new String(bytes);
      }
    } catch (Exception e) {
    }
    return "";
  }
  
  static public byte[] getZipFileContentsAsBytes(InputStream is, String[] list,
                                          int listPtr) {
    byte[] ret = new byte[0];
    String fileName = list[listPtr];
    if (fileName.lastIndexOf("/") == fileName.length() - 1)
      return ret;
    ZipInputStream zis = new ZipInputStream(is);
    ZipEntry ze;
    try {
      while ((ze = zis.getNextEntry()) != null) {
        if (!fileName.equals(ze.getName()))
          continue;
        byte[] bytes = getZipEntryAsBytes(zis);
        if (isZipFile(bytes) && list != null && ++listPtr < list.length)
          return getZipFileContentsAsBytes(new BufferedInputStream(
              new ByteArrayInputStream(bytes)), list, listPtr);
        return bytes;
      }
    } catch (Exception e) {
    }
    return ret;
  }
  
  static public String getZipDirectoryAsStringAndClose(InputStream is) {
    StringBuffer sb = new StringBuffer();
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(is, false);
      is.close();
    } catch (Exception e) { 
      Logger.error(e.getMessage());
    }
    for (int i = 0; i < s.length; i++)
      sb.append(s[i]).append('\n');
    return sb.toString();
  }
  
  static public String[] getZipDirectoryAndClose(InputStream is, boolean addManifest) {
    String[] s = new String[0];
    try {
      s = getZipDirectoryOrErrorAndClose(is, addManifest);
      is.close();
    } catch (Exception e) { 
      Logger.error(e.getMessage());
    }
    return s;
  }
  
  private static String[] getZipDirectoryOrErrorAndClose(InputStream is, boolean addManifest) throws IOException {
    Vector v = new Vector();
    ZipInputStream zis = new ZipInputStream(is);
    ZipEntry ze;
    String manifest = null;
    while ((ze = zis.getNextEntry()) != null) {
      String fileName = ze.getName();
      if (addManifest && fileName.equals("JmolManifest"))
        manifest = getZipEntryAsString(zis); 
      else if (!fileName.startsWith("__MACOS")) // resource fork not nec.
        v.addElement(fileName);
    }
    zis.close();
    if (addManifest)
      v.add(0, manifest == null ? "" : manifest + "\n############\n");
    int len = v.size();
    String[] dirList = new String[len];
    for (int i = 0; i < len; i++)
      dirList[i] = (String) v.elementAt(i);
    return dirList;
  }
  
  public static String getZipEntryAsString(ZipInputStream zis) throws IOException {
    StringBuffer sb = new StringBuffer();
    byte[] buf = new byte[1024];
    int len;
    while (zis.available() == 1 && (len = zis.read(buf)) > 0)
      sb.append(new String(buf, 0, len));
    return sb.toString();
  }
  
  public static byte[] getZipEntryAsBytes(ZipInputStream zis) throws IOException {
    
    //What is the efficient way to read an input stream into a byte array?
    
    byte[] buf = new byte[1024];
    byte[] bytes = new byte[4096];
    int len = 0;
    int totalLen = 0;
    while (zis.available() == 1 && (len = zis.read(buf)) > 0) {
      totalLen += len;
      if (totalLen >= bytes.length)
        bytes = ArrayUtil.ensureLength(bytes, totalLen * 2);
      System.arraycopy(buf, 0, bytes, totalLen - len, len);
    }
    buf = new byte[totalLen];
    System.arraycopy(bytes, 0, buf, 0, totalLen);
    return buf;
  }

  public static byte[] getStreamAsBytes(BufferedInputStream bis) throws IOException {
    byte[] buf = new byte[1024];
    byte[] bytes = new byte[4096];
    int len = 0;
    int totalLen = 0;
    while ((len = bis.read(buf)) > 0) {
      totalLen += len;
      if (totalLen >= bytes.length)
        bytes = ArrayUtil.ensureLength(bytes, totalLen * 2);
      System.arraycopy(buf, 0, bytes, totalLen - len, len);
    }
    buf = new byte[totalLen];
    System.arraycopy(bytes, 0, buf, 0, totalLen);
    return buf;
  }

}