/* $RCSfile: Platform3D.java,v $
 * $Author: migueljmol $
 * $Date: 2004/11/07 16:40:29 $
 * $Revision: 1.14 $
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package com.actelion.research.gui.viewer2d.jmol;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;


/**
 *<p>
 * Specifies the API to an underlying int[] buffer of ARGB values that
 * can be converted into an Image object and a short[] for z-buffer depth.
 *</p>
 */ 
abstract class Platform3D {

  int windowWidth, windowHeight, windowSize;
  int bufferWidth, bufferHeight, bufferSize;

  Image imagePixelBuffer;
  int[] pBuffer;
  short[] zBuffer;
  int argbBackground;

  int widthOffscreen, heightOffscreen;
  Image imageOffscreen;
  Graphics gOffscreen;

  final static boolean forcePlatformAWT = false;
  final static boolean desireClearingThread = false;
  boolean useClearingThread = true;

  ClearingThread clearingThread;

  static Platform3D createInstance(Component awtComponent) {
    boolean jvm12orGreater =
      System.getProperty("java.version").compareTo("1.2") >= 0;
    boolean useSwing = jvm12orGreater && !forcePlatformAWT;
    Platform3D platform =(useSwing
                          ? allocateSwing3D() : new Awt3D(awtComponent));
    platform.initialize(desireClearingThread & useSwing);
    return platform;
  }

  private static Platform3D allocateSwing3D() {
    // this method is necessary in order to prevent Swing-related
    // classes from getting touched on the MacOS9 platform
    // otherwise the Mac crashes *badly* when the classes are not found
    return new Swing3D();
  }

  final void initialize(boolean useClearingThread) {
    this.useClearingThread = useClearingThread;
    if (useClearingThread) {
      System.out.println("using ClearingThread");
      clearingThread = new ClearingThread();
      clearingThread.start();
    }
  }

  final static short ZBUFFER_BACKGROUND = 32767;

  abstract Image allocateImage();

  void allocateBuffers(int width, int height, boolean tFsaa4) {
    System.out.println("allocateBuffers(" + width + "," +
                       height + "," + tFsaa4 + ")");
    windowWidth = width;
    windowHeight = height;
    windowSize = width * height;
    if (tFsaa4) {
      System.out.println("I am doubling!");
      bufferWidth = width * 2;
      bufferHeight = height * 2;
    } else {
      bufferWidth = width;
      bufferHeight = height;
    }
    bufferSize = bufferWidth * bufferHeight;
    zBuffer = new short[bufferSize];
    pBuffer = new int[bufferSize];
    imagePixelBuffer = allocateImage();
    System.out.println("  width:" + width + " bufferWidth=" + bufferWidth +
                       "\nheight:" + height + "bufferHeight=" + bufferHeight);
  }

  void releaseBuffers() {
    windowWidth = windowHeight = bufferWidth = bufferHeight = bufferSize = -1;
    if (imagePixelBuffer != null) {
      imagePixelBuffer.flush();
      imagePixelBuffer = null;
    }
    pBuffer = null;
    zBuffer = null;
  }

  void setBackground(int argbBackground) {
    if (this.argbBackground != argbBackground) {
      this.argbBackground = argbBackground;
      if (useClearingThread)
        clearingThread.notifyBackgroundChange(argbBackground);
    }
  }

  boolean hasContent() {
    for (int i = bufferSize; --i >= 0; )
      if (zBuffer[i] != ZBUFFER_BACKGROUND)
        return true;
    return false;
  }

  void clearScreenBuffer(int argbBackground) {
    for (int i = bufferSize; --i >= 0; ) {
      zBuffer[i] = ZBUFFER_BACKGROUND;
      pBuffer[i] = argbBackground;
    }
    /*
    for (int i = width; --i >= 0; ) {
      zBuffer[i] = ZBUFFER_BACKGROUND;
      pBuffer[i] = argbBackground;
    }
    for (int i = height, offset = size; --i > 0; ) {
      System.arraycopy(pBuffer, 0, pBuffer, offset -= width, width);
      System.arraycopy(zBuffer, 0, zBuffer, offset, width);
    }
    */
  }
  
  final void obtainScreenBuffer() {
    if (useClearingThread) {
      clearingThread.obtainBufferForClient();
    } else {
      clearScreenBuffer(argbBackground);
    }
  }

  final void clearScreenBufferThreaded() {
    if (useClearingThread)
      clearingThread.releaseBufferForClearing();
  }
  
  void notifyEndOfRendering() {
  }

  abstract Image allocateOffscreenImage(int width, int height);
  
  void checkOffscreenSize(int width, int height) {
    if (width <= widthOffscreen && height <= heightOffscreen)
      return;
    if (imageOffscreen != null) {
      gOffscreen.dispose();
      imageOffscreen.flush();
    }
    if (width > widthOffscreen)
      widthOffscreen = (width + 63) & ~63;
    if (height > heightOffscreen)
      heightOffscreen = (height + 15) & ~15;
    imageOffscreen = allocateOffscreenImage(widthOffscreen, heightOffscreen);
    gOffscreen = imageOffscreen.getGraphics();
  }

  class ClearingThread extends Thread implements Runnable {


    boolean bufferHasBeenCleared = false;
    boolean clientHasBuffer = false;

    synchronized void notifyBackgroundChange(int argbBackground) {
      //      System.out.println("notifyBackgroundChange");
      bufferHasBeenCleared = false;
      notify();
      // for now do nothing
    }

    synchronized void obtainBufferForClient() {
      //      System.out.println("obtainBufferForClient()");
      while (! bufferHasBeenCleared)
        try { wait(); } catch (InterruptedException ie) {}
      clientHasBuffer = true;
    }

    synchronized void releaseBufferForClearing() {
      //      System.out.println("releaseBufferForClearing()");
      clientHasBuffer = false;
      bufferHasBeenCleared = false;
      notify();
    }

    synchronized void waitForClientRelease() {
      //      System.out.println("waitForClientRelease()");
      while (clientHasBuffer || bufferHasBeenCleared)
        try { wait(); } catch (InterruptedException ie) {}
    }

    synchronized void notifyBufferReady() {
      //      System.out.println("notifyBufferReady()");
      bufferHasBeenCleared = true;
      notify();
    }

    public void run() {
      /*
      System.out.println("running clearing thread:" +
                         Thread.currentThread().getPriority());
      */
      while (true) {
        waitForClientRelease();
        int bg;
        do {
          bg = argbBackground;
          clearScreenBuffer(bg);
        } while (bg != argbBackground); // color changed underneath us
        notifyBufferReady();
      }
    }
  }
}
