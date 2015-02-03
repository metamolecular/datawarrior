/* $RCSfile: Swing3D.java,v $
 * $Author: migueljmol $
 * $Date: 2004/11/07 16:40:29 $
 * $Revision: 1.6 $
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

import java.awt.Image;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;

final class Swing3D extends Platform3D {

  final static DirectColorModel rgbColorModel =
    new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF, 0x00000000);

  final static int[] sampleModelBitMasks =
  { 0x00FF0000, 0x0000FF00, 0x000000FF };
  
  Image allocateImage() {
    SinglePixelPackedSampleModel sppsm =
      new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT,
                                       windowWidth,
                                       windowHeight,
                                       sampleModelBitMasks);
    DataBufferInt dbi = new DataBufferInt(pBuffer, windowSize);
    WritableRaster wr =
      Raster.createWritableRaster(sppsm, dbi, null);
    BufferedImage bi = new BufferedImage(rgbColorModel, wr, false, null);
    return bi;
  }

  Image allocateOffscreenImage(int width, int height) {
    return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
  }
}
