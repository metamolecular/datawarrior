/* $RCSfile: Awt3D.java,v $
 * $Author: migueljmol $
 * $Date: 2004/11/07 16:40:28 $
 * $Revision: 1.8 $
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
import java.awt.Image;
import java.awt.image.ImageProducer;
import java.awt.image.ImageConsumer;
import java.awt.image.ColorModel;


/**
 *<p>
 * Uses AWT classes to implement Platform3D when running on 1.1 JVMs.
 *</p>
 *<p>
 * Uses the AWT imaging routines to convert an int[] of ARGB values
 * into an Image by implementing the ImageProducer interface.
 *</p>
 *<p>
 * This is used by MSFT Internet Explorer with the MSFT JVM,
 * and Netscape 4.* on both Win32 and MacOS 9.
 *</p>
 */
  
final class Awt3D extends Platform3D implements ImageProducer {

  Component component;

  ColorModel colorModelRGB;
  ImageConsumer ic;

  Awt3D(Component component) {
    this.component = component;
    colorModelRGB = ColorModel.getRGBdefault();
  }

  Image allocateImage() {
    return component.createImage(this);
  }

  void notifyEndOfRendering() {
    if (this.ic != null)
      startProduction(ic);
  }

  Image allocateOffscreenImage(int width, int height) {
    //    System.out.println("allocateOffscreenImage(" + width + "," + height + ")");
    Image img = component.createImage(width, height);
    //    System.out.println("img=" + img);
    return img;
  }

  public synchronized void addConsumer(ImageConsumer ic) {
    startProduction(ic);
  }

  public boolean isConsumer(ImageConsumer ic) {
    return (this.ic == ic);
  }

  public void removeConsumer(ImageConsumer ic) {
    if (this.ic == ic)
      this.ic = null;
  }

  public void requestTopDownLeftRightResend(ImageConsumer ic) {
  }

  public void startProduction(ImageConsumer ic) {
    if (this.ic != ic) {
      this.ic = ic;
      ic.setDimensions(windowWidth, windowHeight);
      ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT |
                  ImageConsumer.COMPLETESCANLINES |
                  ImageConsumer.SINGLEPASS);
    }
    ic.setPixels(0, 0, windowWidth, windowHeight, colorModelRGB,
                 pBuffer, 0, windowWidth);
    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
  }
}
