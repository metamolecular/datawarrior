/* $RCSfile$
 * $Author: egonw $
 * $Date: 2006-03-18 15:59:33 -0600 (Sat, 18 Mar 2006) $
 * $Revision: 4652 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: hansonr@stolaf.edu
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
package org.jmol.util;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

//import java.io.RandomAccessFile;

/* a basic binary file reader (extended by CompoundDocument). 
 * 
 * random access file info: 
 * http://java.sun.com/docs/books/tutorial/essential/io/rafs.html
 * 
 * SHOOT! random access is only for applications, not applets!
 * 
 * Note that YOU are responsible for determining whether a file
 * is bigEndian or littleEndian; the default is bigEndian.
 * 
 */

public class BinaryDocument {

  public BinaryDocument() {  
  }
  
//  RandomAccessFile file;
  
  protected DataInputStream stream;
  protected boolean isRandom = false;
  protected boolean isBigEndian = true;

  public void close() {
    try {
      stream.close();
    } catch (IOException e) {
      //ignore
    }
  }
  
  public void setStream(BufferedInputStream bis, boolean isBigEndian) {
    if (bis == null)
      return;
    stream = new DataInputStream(bis);
    this.isBigEndian = isBigEndian;
  }
  
  public void setStream(DataInputStream stream) {
    this.stream = stream;
  }
  
  public void setRandom(boolean TF) {
    isRandom = TF;
    //CANNOT be random for web 
  }
  
  public byte readByte() throws Exception {
    return stream.readByte();
  }

  public void readByteArray(byte[] b) throws Exception {
    stream.read(b);
  }

  public void readByteArray(byte[] b, int off, int len) throws Exception {
    stream.read(b, off, len);
  }

  public short readShort() throws Exception {
    return (isBigEndian ? stream.readShort()
        : (short) ((((int) stream.readByte()) & 0xff) 
                 | (((int) stream.readByte()) & 0xff) << 8));
  }

  public int readInt() throws Exception {
    return (isBigEndian ? stream.readInt() : readLEInt());
  }
  
  public int readUnsignedShort() throws Exception {
    int a = (((int) stream.readByte()) & 0xff);
    int b = (((int) stream.readByte()) & 0xff);
    return (isBigEndian ? (a << 8) + b : (b << 8) + a);
  }
  
  public long readLong() throws Exception {
    return (isBigEndian ? stream.readLong()
       : ((((long) stream.readByte()) & 0xff)
        | (((long) stream.readByte()) & 0xff) << 8
        | (((long) stream.readByte()) & 0xff) << 16
        | (((long) stream.readByte()) & 0xff) << 24
        | (((long) stream.readByte()) & 0xff) << 32
        | (((long) stream.readByte()) & 0xff) << 40
        | (((long) stream.readByte()) & 0xff) << 48 
        | (((long) stream.readByte()) & 0xff) << 54));
  }

  public float readFloat() throws Exception {
    return (isBigEndian ? stream.readFloat() 
        : Float.intBitsToFloat(readLEInt()));
  }
  
  public double readDouble() throws Exception {
    return (isBigEndian ? stream.readDouble() : Double.longBitsToDouble(readLELong()));  
  }
  
  
  private int readLEInt() throws Exception {
    return ((((int) stream.readByte()) & 0xff)
          | (((int) stream.readByte()) & 0xff) << 8
          | (((int) stream.readByte()) & 0xff) << 16 
          | (((int) stream.readByte()) & 0xff) << 24);
  }

  private long readLELong() throws Exception {
    return ((((long) stream.readByte()) & 0xff)
          | (((long) stream.readByte()) & 0xff) << 8
          | (((long) stream.readByte()) & 0xff) << 16 
          | (((long) stream.readByte()) & 0xff) << 24
          | (((long) stream.readByte()) & 0xff) << 32
          | (((long) stream.readByte()) & 0xff) << 40
          | (((long) stream.readByte()) & 0xff) << 48
          | (((long) stream.readByte()) & 0xff) << 56);
  }

  public void seek(long offset) {
    // slower, but all that is available using the applet
    try {
      stream.reset();
      stream.skipBytes((int)offset);
    } catch (Exception e) {
      Logger.error(null, e);
    }
  }

/*  random access -- application only:
 * 
    void seekFile(long offset) {
    try {
      file.seek(offset);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
*/
}
