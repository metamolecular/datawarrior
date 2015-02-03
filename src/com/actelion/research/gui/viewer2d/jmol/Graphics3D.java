/* $RCSfile: Graphics3D.java,v $
 * $Author: migueljmol $
 * $Date: 2004/11/12 04:16:37 $
 * $Revision: 1.30 $
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.PixelGrabber;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;



final public class Graphics3D {

  Platform3D platform;
  Line3D line3d;
  Circle3D circle3d;
  Sphere3D sphere3d;
  Triangle3D triangle3d;
  Cylinder3D cylinder3d;
  Hermite3D hermite3d;

  boolean isFullSceneAntialiasingEnabled;
  boolean antialiasThisFrame;

  boolean tPaintingInProgress;

  int windowWidth, windowHeight;
  int width, height;
  int slab, depth;
  int xLast, yLast;
  int[] pbuf;
  short[] zbuf;

  short colixBackground;
  int argbBackground;
  private final Rectangle rectClip = new Rectangle();

  int argbCurrent;
  int argbNoisyUp, argbNoisyDn;

  Font3D font3dCurrent;

  final static int ZBUFFER_BACKGROUND = Platform3D.ZBUFFER_BACKGROUND;

  public Graphics3D(Component awtComponent) {
    platform = Platform3D.createInstance(awtComponent);
    //    Font3D.initialize(platform);
    this.line3d = new Line3D(this);
    this.circle3d = new Circle3D(this);
    this.sphere3d = new Sphere3D(this);
    this.triangle3d = new Triangle3D(this);
    this.cylinder3d = new Cylinder3D(this);
    this.hermite3d = new Hermite3D(this);
    //    setFontOfSize(13);
  }
  
  public void setSize(Dimension dim, boolean enableFullSceneAntialiasing) {
    if (dim.width == windowWidth && dim.height == windowHeight &&
        enableFullSceneAntialiasing == isFullSceneAntialiasingEnabled)
      return;
    windowWidth = dim.width;
    windowHeight = dim.height;
    isFullSceneAntialiasingEnabled = enableFullSceneAntialiasing;
    width = -1; height = -1;
    pbuf = null;
    zbuf = null;
    platform.releaseBuffers();
  }

  public boolean fullSceneAntialiasRendering() {
    return false;
  }

  public int getWindowWidth() {
    return width;
  }

  public int getWindowHeight() {
    return height;
  }

  public int getRenderWidth() {
    return width;
  }

  public int getRenderHeight() {
    return height;
  }

  public void setBackground(short colix) {
    colixBackground = colix;
    argbBackground = getArgb(colix);
    platform.setBackground(argbBackground);
  }
  
  public void setSlabAndDepthValues(int slabValue, int depthValue) {
    slab =
      slabValue < 0 ? 0
      : slabValue > ZBUFFER_BACKGROUND ? ZBUFFER_BACKGROUND : slabValue;
    depth =
      depthValue < 0 ? 0
      : depthValue > ZBUFFER_BACKGROUND ? ZBUFFER_BACKGROUND : depthValue;
  }

  private void downSampleFullSceneAntialiasing() {
    int[] pbuf1 = pbuf;
    int[] pbuf4 = pbuf;
    int width4 = width;
    int offset1 = 0;
    int offset4 = 0;
    for (int i = windowHeight; --i >= 0; ) {
      for (int j = windowWidth; --j >= 0; ) {
        int argb;
        argb  = (pbuf4[offset4         ] >> 2) & 0x3F3F3F3F;
        argb += (pbuf4[offset4 + width4] >> 2) & 0x3F3F3F3F;
        ++offset4;
        argb += (pbuf4[offset4         ] >> 2) & 0x3F3F3F3F;
        argb += (pbuf4[offset4 + width4] >> 2) & 0x3F3F3F3F;
        argb += (argb & 0xC0C0C0C0) >> 6;
        argb |= 0xFF000000;
        pbuf1[offset1] = argb;
        ++offset1;
        ++offset4;
      }
      offset4 += width4;
    }
  }

  public boolean hasContent() {
    return platform.hasContent();
  }

  public void setColor(Color color) {
    argbCurrent = argbNoisyUp = argbNoisyDn = color.getRGB();
  }

  public void setColorArgb(int argb) {
    argbCurrent = argbNoisyUp = argbNoisyDn = argb;
  }

  public void setColorNoisy(short colix, int intensity) {
    int[] shades = getShades(colix);
    argbCurrent = shades[intensity];
    argbNoisyUp = shades[intensity < shadeLast ? intensity + 1 : shadeLast];
    argbNoisyDn = shades[intensity > 0 ? intensity - 1 : 0];
  }

  public void setColix(short colix) {
    argbCurrent = argbNoisyUp = argbNoisyDn = getArgb(colix);
  }

  int[] imageBuf = new int[0];

  public void drawImage(Image image, int x, int y, int z) {
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);
    int imageSize = imageWidth * imageHeight;
    if (imageSize > imageBuf.length)
      imageBuf = new int[imageSize];
    PixelGrabber pg = new PixelGrabber(image, 0, 0, imageWidth, imageHeight,
                                       imageBuf, 0, imageWidth);
    try {
      pg.grabPixels();
    } catch (InterruptedException e) {
      System.out.println("pg.grabPixels Interrupted");
    }
    int offsetSrc = 0;
    if (x >= 0 && y >= 0 && x+imageWidth <= width && y+imageHeight <= height) {
      do {
        plotPixelsUnclipped(imageBuf, offsetSrc, imageWidth, x, y, z);
        offsetSrc += imageWidth;
        ++y;
      } while (--imageHeight > 0);
    } else {
      do {
        plotPixelsClipped(imageBuf, offsetSrc, imageWidth, x, y, z);
        offsetSrc += imageWidth;
        ++y;
      } while (--imageHeight > 0);
    }
  }

  public void drawCircleCentered(short colix, long xyzd) {
    drawCircleCentered(colix, Xyzd.getD(xyzd),
                       Xyzd.getX(xyzd), Xyzd.getY(xyzd), Xyzd.getZ(xyzd));
  }

  public void drawCircleCentered(short colix, int diameter,
                                 int x, int y, int z) {
    if (z < slab || z > depth)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = getArgb(colix);
    if ((x >= r && x + r < width) && (y >= r && y + r < height)) {
      switch (diameter) {
      case 2:
        plotPixelUnclipped(  x, y-1, z);
        plotPixelUnclipped(x-1, y-1, z);
        plotPixelUnclipped(x-1,   y, z);
      case 1:
        plotPixelUnclipped(x, y, z);
      case 0:
        break;
      default:
        circle3d.plotCircleCenteredUnclipped(x, y, z, diameter);
      }
    } else {
      switch (diameter) {
      case 2:
        plotPixelClipped(  x, y-1, z);
        plotPixelClipped(x-1, y-1, z);
        plotPixelClipped(x-1,   y, z);
      case 1:
        plotPixelClipped(x, y, z);
      case 0:
        break;
      default:
        circle3d.plotCircleCenteredClipped(x, y, z, diameter);
      }
    }
  }

  public void fillScreenedCircleCentered(short colixFill, int diameter, 
                                         int x, int y, int z) {
    if (diameter == 0 || z < slab || z > depth)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = getArgb(colixFill);
    if (x >= r && x + r < width && y >= r && y + r < height) {
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter, true);
      circle3d.plotCircleCenteredUnclipped(x, y, z, diameter);
    } else {
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter, true);
      circle3d.plotCircleCenteredClipped(x, y, z, diameter);
    }
  }

  public void fillCircleCentered(short colixFill, int diameter, 
                                 int x, int y, int z) {
    if (diameter == 0 || z < slab || z > depth)
      return;
    int r = (diameter + 1) / 2;
    argbCurrent = getArgb(colixFill);
    if (x >= r && x + r < width && y >= r && y + r < height) {
      circle3d.plotFilledCircleCenteredUnclipped(x, y, z, diameter, false);
    } else {
      circle3d.plotFilledCircleCenteredClipped(x, y, z, diameter, false);
    }
  }

  public void fillSphereCentered(short colix, long xyzd) {
    fillSphereCentered(colix, Xyzd.getD(xyzd),
                       Xyzd.getX(xyzd), Xyzd.getY(xyzd), Xyzd.getZ(xyzd));
  }

  public void fillSphereCentered(short colix, Point3i center, int diameter) {
    fillSphereCentered(colix, diameter, center.x, center.y, center.z);
  }

  public void fillSphereCentered(short colix, int diameter,
                                 int x, int y, int z) {
    if (diameter <= 1) {
      plotPixelClipped(colix, x, y, z);
    } else {
      sphere3d.render(colix, diameter, x, y, z);
    }
  }

  public void fillSphereCentered(short colix, int diameter, Point3i screen) {
    fillSphereCentered(colix, diameter, screen.x, screen.y, screen.z);
  }

  public void fillSphereCentered(short colix, int diameter, Point3f screen) {
    fillSphereCentered(colix, diameter, (int)screen.x, (int)screen.y, (int)screen.z);
  }

  public void drawRect(short colix, int x, int y, int z,
                       int width, int height) {
    argbCurrent = getArgb(colix);
    int xRight = x + width - 1;
    drawLine(x, y, z, xRight, y, z);
    int yBottom = y + height - 1;
    drawLine(x, y, z, x, yBottom, z);
    drawLine(xRight, y, z, xRight, yBottom, z);
    drawLine(x, yBottom, z, xRight + 1, yBottom, z);
  }

  public void drawString(String str, short colix,
                         int xBaseline, int yBaseline, int z) {
    drawString(str, font3dCurrent, colix, (short)0, xBaseline, yBaseline, z);
  }
  
  public void drawString(String str, Font3D font3d, short colix, short bgcolix,
                         int xBaseline, int yBaseline, int z) {
    //    System.out.println("Graphics3D.drawString(" + str + "," + font3d +
    //                       ", ...)");

    font3dCurrent = font3d;
    argbCurrent = getArgb(colix);
    if (z < slab || z > depth)
      return;
    //    System.out.println("ready to call");
    Text3D.plot(xBaseline, yBaseline - font3d.fontMetrics.getAscent(),
                z, argbCurrent, getArgb(bgcolix), str, font3dCurrent, this);
    //    System.out.println("done");
  }

  public void setFontOfSize(int fontsize) {
    font3dCurrent = getFont3D(fontsize);
  }

  public void setFont(byte fid) {
    font3dCurrent = Font3D.getFont3D(fid);
  }
  
  public void setFont(Font3D font3d) {
    font3dCurrent = font3d;
  }
  
  public Font3D getFont3DCurrent() {
    return font3dCurrent;
  }

  public byte getFontFidCurrent() {
    return font3dCurrent.fid;
  }
  
  public FontMetrics getFontMetrics() {
    return font3dCurrent.fontMetrics;
  }

  boolean currentlyRendering;

  private void setRectClip(Rectangle clip) {
    if (clip == null) {
      rectClip.x = rectClip.y = 0;
      rectClip.width = width;
      rectClip.height = height;
    } else {
      rectClip.setBounds(clip);
      // on Linux platform with Sun 1.4.2_02 I am getting a clipping rectangle
      // that is wider than the current window during window resize
      if (rectClip.x < 0)
        rectClip.x = 0;
      if (rectClip.y < 0)
        rectClip.y = 0;
      if (rectClip.x + rectClip.width > windowWidth)
        rectClip.width = windowWidth - rectClip.x;
      if (rectClip.y + rectClip.height > windowHeight)
        rectClip.height = windowHeight - rectClip.y;

      if (antialiasThisFrame) {
        rectClip.x *= 2;
        rectClip.y *= 2;
        rectClip.width *= 2;
        rectClip.height *= 2;
      }
    }
  }


  // 3D specific routines
  public void beginRendering(Rectangle rectClip, boolean antialiasThisFrame) {
    if (currentlyRendering)
      endRendering();
    antialiasThisFrame &= isFullSceneAntialiasingEnabled;
    this.antialiasThisFrame = antialiasThisFrame;
    currentlyRendering = true;
    if (pbuf == null) {
      platform.allocateBuffers(windowWidth, windowHeight,
                               isFullSceneAntialiasingEnabled);
      pbuf = platform.pBuffer;
      zbuf = platform.zBuffer;
      width = windowWidth;
      xLast = width - 1;
      height = windowHeight;
      yLast = height - 1;
    }
    width = windowWidth;
    height = windowHeight;
    if (antialiasThisFrame) {
      width *= 2;
      height *= 2;
    }
    xLast = width - 1;
    yLast = height - 1;
    setRectClip(rectClip);
    platform.obtainScreenBuffer();
  }

  public void endRendering() {
    if (currentlyRendering) {
      if (antialiasThisFrame)
        downSampleFullSceneAntialiasing();
      platform.notifyEndOfRendering();
      currentlyRendering = false;
    }
  }
  
  public Image getScreenImage() {
    return platform.imagePixelBuffer;
  }

  public void releaseScreenImage() {
    platform.clearScreenBufferThreaded();
  }

  public void drawDashedLine(short colix, int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getArgb(colix);
    line3d.drawDashedLine(argb, argb, run, rise, x1, y1, z1, x2, y2, z2);
  }

  public void drawDottedLine(short colix,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getArgb(colix);
    line3d.drawDashedLine(argb, argb, 2, 1, x1, y1, z1, x2, y2, z2);
  }

  public void drawDashedLine(short colix1, short colix2, int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
        
    line3d.drawDashedLine(getArgb(colix1), getArgb(colix2),
                          run, rise, x1, y1, z1, x2, y2, z2);
  }
  

  public void drawLine(Point3i pointA, Point3i pointB) {
    line3d.drawLine(argbCurrent, argbCurrent,
                    pointA.x, pointA.y, pointA.z,
                    pointB.x, pointB.y, pointB.z);
  }

  public void drawLine(short colix, Point3i pointA, Point3i pointB) {
    int argb = getArgb(colix);
    line3d.drawLine(argb, argb,
                    pointA.x, pointA.y, pointA.z,
                    pointB.x, pointB.y, pointB.z);
  }

  public void drawDottedLine(short colix, Point3i pointA, Point3i pointB) {
    drawDashedLine(colix, 2, 1, pointA, pointB);
  }

  public void drawDashedLine(short colix, int run, int rise,
                             Point3i pointA, Point3i pointB) {
    int argb = getArgb(colix);
    line3d.drawDashedLine(argb, argb, run, rise,
                          pointA.x, pointA.y, pointA.z,
                          pointB.x, pointB.y, pointB.z);
  }

  public void drawDashedLine(int run, int rise,
                             int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawDashedLine(argbCurrent, argbCurrent, run, rise,
                          x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawLine(argbCurrent, argbCurrent, x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    int argb = getArgb(colix);
    line3d.drawLine(argb, argb, x1, y1, z1, x2, y2, z2);
  }

  public void drawLine(short colix1, short colix2,
                       int x1, int y1, int z1, int x2, int y2, int z2) {
    line3d.drawLine(getArgb(colix1), getArgb(colix2),
                    x1, y1, z1, x2, y2, z2);
  }
  
  public void drawPolygon4(int[] ax, int[] ay, int[] az) {
    drawLine(ax[0], ay[0], az[0], ax[3], ay[3], az[3]);
    for (int i = 3; --i >= 0; )
      drawLine(ax[i], ay[i], az[i], ax[i+1], ay[i+1], az[i+1]);
  }

  public void fillQuadrilateral(short colix,
                                Point3f screenA, Point3f screenB,
                                Point3f screenC, Point3f screenD) {
    /*
    System.out.println("fillQuad----------------");
    System.out.println("screenA="+ screenA +
                       "\nscreenB=" + screenB +
                       "\nscreenC=" + screenC +
                       "\nscreenD=" + screenD);
    */
    setColorNoisy(colix, calcIntensityScreen(screenA, screenB, screenC));
    fillTriangle(screenA, screenB, screenC);
    fillTriangle(screenA, screenC, screenD);
  }

  public void fillTriangle(short colix, Point3i screenA,
                           Point3i screenB, Point3i screenC) {
    calcSurfaceShade(colix, screenA, screenB, screenC);
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    triangle3d.fillTriangleNoisy();
  }

  public void fillTriangle(short colix, Point3f screenA,
                           Point3f screenB, Point3f screenC) {
    setColorNoisy(colix, calcIntensityScreen(screenA, screenB, screenC));
    fillTriangle(screenA, screenB, screenC);
  }

  public void fillQuadrilateral(int argb,
                                Point3i screenA, Point3i screenB,
                                Point3i screenC, Point3i screenD) {
    fillTriangle(argb, screenA, screenB, screenC);
    fillTriangle(argb, screenA, screenC, screenD);
  }
  
  public void fillTriangle(int argb, Point3i screenA,
                                    Point3i screenB, Point3i screenC) {
    
    /*
      System.out.println("fillTriangle----------------");
      System.out.println("screenA="+ screenA +
      " screenB=" + screenB +
      " screenC=" + screenC);
    */
    argbCurrent = argbNoisyUp = argbNoisyDn = argb;
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    triangle3d.fillTriangleNoisy();
  }

  public void fillTriangle(Point3i screenA, Point3i screenB, Point3i screenC) {
    
    int[] t;
    t = triangle3d.ax;
    t[0] = screenA.x; t[1] = screenB.x; t[2] = screenC.x;
    t = triangle3d.ay;
    t[0] = screenA.y; t[1] = screenB.y; t[2] = screenC.y;
    t = triangle3d.az;
    t[0] = screenA.z; t[1] = screenB.z; t[2] = screenC.z;

    triangle3d.fillTriangleNoisy();
  }

  public void fillTriangle(Point3f screenA, Point3f screenB, Point3f screenC) {
    int[] t;
    t = triangle3d.ax;
    t[0] = (int)screenA.x; t[1] = (int)screenB.x; t[2] = (int)screenC.x;
    t = triangle3d.ay;
    t[0] = (int)screenA.y; t[1] = (int)screenB.y; t[2] = (int)screenC.y;
    t = triangle3d.az;
    t[0] = (int)screenA.z; t[1] = (int)screenB.z; t[2] = (int)screenC.z;

    triangle3d.fillTriangleNoisy();
  }

  int intensity = 0;
  
  void diff(Vector3f v, Point3i s1, Point3i s2) {
    v.x = s1.x - s2.x;
    v.y = s1.y - s2.y;
    v.z = s1.z - s2.z;
  }

  public void calcSurfaceShade(short colix, Point3i screenA,
                               Point3i screenB, Point3i screenC) {
    diff(vectorAB, screenB, screenA);
    diff(vectorAC, screenC, screenA);
    vectorNormal.cross(vectorAB, vectorAC);
    int intensity =
      vectorNormal.z >= 0
      ? calcIntensity(-vectorNormal.x, -vectorNormal.y, vectorNormal.z)
      : calcIntensity(vectorNormal.x, vectorNormal.y, -vectorNormal.z);
    setColorNoisy(colix, intensity);
  }


  int foo = 0;

  public void calcSurfaceShade(short colix, Point3f screenA,
                               Point3f screenB, Point3f screenC) {
    vectorAB.sub(screenB, screenA);
    vectorAC.sub(screenC, screenA);
    vectorNormal.cross(vectorAB, vectorAC);
    int intensity =
      vectorNormal.z >= 0
      ? calcIntensity(-vectorNormal.x, -vectorNormal.y, vectorNormal.z)
      : calcIntensity(vectorNormal.x, vectorNormal.y, -vectorNormal.z);
    /*
    System.out.println("intensity=" + intensity + " : " + foo++ + " : " + vectorNormal);
    System.out.println("vectorAB="+ vectorAB + " vectorAC="+ vectorAC);
    */
    argbCurrent = getShades(colix)[intensity];
  }

  public void drawfillTriangle(short colix, int xA, int yA, int zA, int xB,
                               int yB, int zB, int xC, int yC, int zC) {
    int argb = argbCurrent = getArgb(colix);
    line3d.drawLine(argb, argb, xA, yA, zA, xB, yB, zB);
    line3d.drawLine(argb, argb, xA, yA, zA, xC, yC, zC);
    line3d.drawLine(argb, argb, xB, yB, zB, xC, yC, zC);
    int[] t;
    t = triangle3d.ax;
    t[0] = xA; t[1] = xB; t[2] = xC;
    t = triangle3d.ay;
    t[0] = yA; t[1] = yB; t[2] = yC;
    t = triangle3d.az;
    t[0] = zA; t[1] = zB; t[2] = zC;

    triangle3d.fillTriangleNoisy();
  }

  public void fillTriangle(short colix, int xA, int yA, int zA,
                           int xB, int yB, int zB, int xC, int yC, int zC) {
    /*
    System.out.println("fillTriangle:" + xA + "," + yA + "," + zA + "->" +
                       xB + "," + yB + "," + zB + "->" +
                       xC + "," + yC + "," + zC);
    */
    argbCurrent = getArgb(colix);
    int[] t;
    t = triangle3d.ax;
    t[0] = xA; t[1] = xB; t[2] = xC;
    t = triangle3d.ay;
    t[0] = yA; t[1] = yB; t[2] = yC;
    t = triangle3d.az;
    t[0] = zA; t[1] = zB; t[2] = zC;

    triangle3d.fillTriangleNoisy();
  }

  public void drawTriangle(short colix, int xA, int yA, int zA,
                           int xB, int yB, int zB, int xC, int yC, int zC) {
    /*
    System.out.println("drawTriangle:" + xA + "," + yA + "," + zA + "->" +
                       xB + "," + yB + "," + zB + "->" +
                       xC + "," + yC + "," + zC);
    */
    int argb = getArgb(colix);
    line3d.drawLine(argb, argb, xA, yA, zA, xB, yB, zB);
    line3d.drawLine(argb, argb, xA, yA, zA, xC, yC, zC);
    line3d.drawLine(argb, argb, xB, yB, zB, xC, yC, zC);
  }

  public final static byte ENDCAPS_NONE = 0;
  public final static byte ENDCAPS_OPEN = 1;
  public final static byte ENDCAPS_FLAT = 2;
  public final static byte ENDCAPS_SPHERICAL = 3;

  public void fillCylinder(short colixA, short colixB, byte endcaps,
                           int diameter,
                           int xA, int yA, int zA, int xB, int yB, int zB) {
    cylinder3d.render(colixA, colixB, endcaps, diameter,
                      xA, yA, zA, xB - xA, yB - yA, zB - zA);
  }

  public void fillCylinder(short colix, byte endcaps,
                           int diameter,
                           int xA, int yA, int zA, int xB, int yB, int zB) {
    cylinder3d.render(colix, colix, endcaps, diameter,
                      xA, yA, zA, xB - xA, yB - yA, zB - zA);
  }

  public void fillCylinder(short colix, byte endcaps, int diameter,
                           Point3i screenA, Point3i screenB) {
    cylinder3d.render(colix, colix, endcaps, diameter,
                      screenA.x, screenA.y, screenA.z,
                      screenB.x - screenA.x, screenB.y - screenA.y,
                      screenB.z - screenA.z);
  }

  public void fillCone(short colix, byte endcap, int diameter,
                       int xBase, int yBase, int zBase,
                       int xTip, int yTip, int zTip) {
    cylinder3d.renderCone(colix, endcap, diameter,
                      xBase, yBase, zBase, xTip, yTip, zTip);

  }

  public void fillCone(short colix, byte endcap, int diameter,
                       Point3i screenBase, Point3i screenTip) {
    cylinder3d.renderCone(colix, endcap, diameter,
                          screenBase.x, screenBase.y, screenBase.z,
                          screenTip.x, screenTip.y, screenTip.z);

  }

  public void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    hermite3d.render(true, colix, tension, diameterBeg, diameterMid, diameterEnd,
                     s0, s1, s2, s3);
  }
  
  public void drawHermite(short colix, int tension,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3) {
    hermite3d.render(false, colix, tension, 0, 0, 0, s0, s1, s2, s3);
  }

  public void drawHermite(boolean fill, short colix, int tension,
                          Point3i s0, Point3i s1, Point3i s2, Point3i s3,
                          Point3i s4, Point3i s5, Point3i s6, Point3i s7) {
    hermite3d.render2(fill, colix, tension, s0, s1, s2, s3, s4, s5, s6, s7);
  }
  
  public void fillRect(short colix,
                       int x, int y, int z, int widthFill, int heightFill) {
    argbCurrent = getArgb(colix);
    if (x < 0) {
      widthFill += x;
      if (widthFill <= 0)
        return;
      x = 0;
    }
    if (x + widthFill > width) {
      widthFill = width - x;
      if (widthFill == 0)
        return;
    }
    if (y < 0) {
      heightFill += y;
      if (heightFill <= 0)
        return;
      y = 0;
    }
    if (y + heightFill > height)
      heightFill = height - y;
    while (--heightFill >= 0)
      plotPixelsUnclipped(widthFill, x, y++, z, false);
  }

  public void drawPixel(Point3i point) {
    plotPixelClipped(point);
  }

  public void drawPixel(int x, int y, int z) {
    plotPixelClipped(x, y, z);
  }

  /* ***************************************************************
   * the plotting routines
   * ***************************************************************/


  void plotPixelClipped(int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
  }

  void plotPixelClipped(Point3i screen) {
    int x = screen.x; if (x < 0 || x >= width) return;
    int y = screen.y; if (y < 0 || y >= height) return;
    int z = screen.z; if (z < slab || z > depth) return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
  }

  void plotPixelClipped(int argb, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
    }
  }

  void plotPixelClipped(short colix, int x, int y, int z) {
    if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
      return;
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = getArgb(colix);
    }
  }

  void forcePixel(Color co, int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height)
      return;
    int offset = y * width + x;
    zbuf[offset] = 0;
    pbuf[offset] = co.getRGB();
  }

  void plotPixelUnclipped(int x, int y, int z) {
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argbCurrent;
    }
  }

  void plotPixelUnclipped(int argb, int x, int y, int z) {
    int offset = y * width + x;
    if (z < zbuf[offset]) {
      zbuf[offset] = (short)z;
      pbuf[offset] = argb;
    }
  }

  void plotPixelsClipped(int count, int x, int y, int z, boolean tScreened) {
    if (y < 0 || y >= height || x >= width || z < slab || z > depth)
      return;
    if (x < 0) {
      count += x; // x is negative, so this is subtracting -x
      if (count <= 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    int offsetMax = offsetPbuf + count;
    int step = 1;
    if (tScreened) {
      step = 2;
      if (((x ^ y) & 1) != 0)
        if (++offsetPbuf == offsetMax)
          return;
    }
    do {
      if (z < zbuf[offsetPbuf]) {
        zbuf[offsetPbuf] = (short)z;
        pbuf[offsetPbuf] = argbCurrent;
      }
      offsetPbuf += step;
    } while (offsetPbuf < offsetMax);
  }

  void plotNoisyPixelsClipped(int count, int x, int y,
                         int zAtLeft, int zPastRight) {
    //    System.out.print("plotPixelsClipped z values:");
    /*
    System.out.println("plotPixelsClipped count=" + count + "x,y,z=" +
                       x + "," + y + "," + zAtLeft + " -> " + zPastRight);
    */
    if (count <= 0 || y < 0 || y >= height || x >= width ||
        (zAtLeft < slab && zPastRight < slab) ||
        (zAtLeft > depth && zPastRight > depth))
      return;
    int seed = (x << 16) + (y << 1) ^ 0x33333333;
    // scale the z coordinates;
    int zScaled = (zAtLeft << 10) + (1 << 9);
    int dz = zPastRight - zAtLeft;
    int roundFactor = count / 2;
    int zIncrementScaled =
      ((dz << 10) + (dz >= 0 ? roundFactor : -roundFactor))/count;
    if (x < 0) {
      x = -x;
      zScaled += zIncrementScaled * x;
      count -= x;
      if (count <= 0)
        return;
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      int z = zScaled >> 10;
      //      System.out.print(" " + z);
      if (z >= slab && z <= depth && z < zbuf[offsetPbuf]) {
        zbuf[offsetPbuf] = (short)z;
        seed = ((seed << 16) + (seed << 1) + seed) & 0x7FFFFFFF;
        int bits = (seed >> 16) & 0x07;
        pbuf[offsetPbuf] = (bits == 0
                            ? argbNoisyDn
                            : (bits == 1 ? argbNoisyUp : argbCurrent));
      }
      ++offsetPbuf;
      zScaled += zIncrementScaled;
    }
    //    System.out.println("");
  }

  void plotPixelsUnclipped(int count, int x, int y, int z, boolean tScreened) {
    int offsetPbuf = y * width + x;
    if (! tScreened) {
      while (--count >= 0) {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = argbCurrent;
        }
        ++offsetPbuf;
      }
    } else {
      int offsetMax = offsetPbuf + count;
      if (((x ^ y) & 1) != 0)
        if (++offsetPbuf == offsetMax)
          return;
      do {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = argbCurrent;
        }
        offsetPbuf += 2;
      } while (offsetPbuf < offsetMax);
    }
  }

  void plotPixelsClipped(int[] pixels, int offset, int count,
                         int x, int y, int z) {
    if (y < 0 || y >= height || x >= width || z < slab || z > depth)
      return;
    if (x < 0) {
      count += x; // x is negative, so this is subtracting -x
      if (count < 0)
        return;
      offset -= x; // and this is adding -x
      x = 0;
    }
    if (count + x > width)
      count = width - x;
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      int pixel = pixels[offset++];
      int alpha = pixel & 0xFF000000;
      if (alpha >= 0x80000000) {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = pixel;
          }
      }
      ++offsetPbuf;
    }
  }

  void plotPixelsUnclipped(int[] pixels, int offset, int count,
                           int x, int y, int z) {
    int offsetPbuf = y * width + x;
    while (--count >= 0) {
      int pixel = pixels[offset++];
      int alpha = pixel & 0xFF000000;
      if ((alpha & 0x80000000) != 0) {
        if (z < zbuf[offsetPbuf]) {
          zbuf[offsetPbuf] = (short)z;
          pbuf[offsetPbuf] = pixel;
        }
      }
      ++offsetPbuf;
    }
  }
  
  void plotLineDelta(int[] shades1, int[] shades2, int fp8Intensity,
                     int x, int y, int z, int dx, int dy, int dz) {
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height ||
        z < slab || z + dz < slab || z > depth || z + dz > depth)
      line3d.plotLineDeltaClipped(shades1, shades2, fp8Intensity,
                                  x, y, z, dx, dy, dz);
    else 
      line3d.plotLineDeltaUnclipped(shades1, shades2, fp8Intensity,
                                    x, y, z, dx, dy, dz);
  }

  void plotLineDelta(int argb1, int argb2,
                     int x, int y, int z, int dx, int dy, int dz) {
    if (x < 0 || x >= width || x + dx < 0 || x + dx >= width ||
        y < 0 || y >= height || y + dy < 0 || y + dy >= height ||
        z < slab || z + dz < slab || z > depth || z + dz > depth)
      line3d.plotLineDeltaClipped(argb1, argb2, x, y, z, dx, dy, dz);
    else 
      line3d.plotLineDeltaUnclipped(argb1, argb2, x, y, z, dx, dy, dz);
  }

  public void plotPoints(short colix, int count, int[] coordinates) {
    int argb = argbCurrent = getArgb(colix);
    for (int i = count * 3; i > 0; ) {
      int z = coordinates[--i];
      int y = coordinates[--i];
      int x = coordinates[--i];
      if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
        continue;
      int offset = y * width + x;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        pbuf[offset] = argb;
      }
    }
  }

  public void plotPoints(int count,
                         short colix, byte[] intensities, int[] coordinates) {
    int[] shades = getShades(colix);
    for (int i = count * 3, j = count-1; i > 0; --j) {
      int z = coordinates[--i];
      int y = coordinates[--i];
      int x = coordinates[--i];
      if (x < 0 || x >= width || y < 0 || y >= height || z < slab || z > depth)
        continue;
      int offset = y * width + x;
      if (z < zbuf[offset]) {
        zbuf[offset] = (short)z;
        //        pbuf[offset] = getArgb(colix);
        pbuf[offset] = shades[intensities[j]];
      }
    }
  }

  void averageOffsetArgb(int offset, int argb) {
    pbuf[offset] =((((pbuf[offset] >> 1) & 0x007F7F7F) +
                    ((argb >> 1) & 0xFF7F7F7F)) |
                   (argb & 0xFF010101));
  }

  public final static short NULL = 0;
  public final static short BLACK = 1;
  public final static short ORANGE = 2;
  public final static short PINK = 3;
  public final static short BLUE = 4;
  public final static short WHITE = 5;
  public final static short AQUA = 6;
  public final static short CYAN = 6;
  public final static short RED = 7;
  public final static short GREEN = 8;
  public final static short GRAY = 9;
  public final static short SILVER = 10;
  public final static short LIGHTGRAY = 10;
  public final static short LIME = 11;
  public final static short MAROON = 12;
  public final static short NAVY = 13;
  public final static short OLIVE = 14;
  public final static short PURPLE = 15;
  public final static short TEAL = 16;
  public final static short MAGENTA = 17;
  public final static short FUCHSIA = 17;
  public final static short YELLOW = 18;
  public final static short HOTPINK = 19;

  static Color[] colorsPredefined = {
    Color.black, Color.orange, Color.pink, Color.blue,
    Color.white, Color.cyan, Color.red, new Color(0, 128, 0),
    Color.gray, Color.lightGray, Color.green, new Color(128, 0, 0),
    new Color(0, 0, 128), new Color(128, 128, 0), new Color(128, 0, 128),
    new Color(0, 128, 128), Color.magenta, Color.yellow,
    new Color(0xFF, 0x69, 0xB4),
  };

  static {
    for (int i = 0; i < colorsPredefined.length; ++i)
      Colix.getColix(colorsPredefined[i]);
  }

  public int getArgb(short colix) {
    return Colix.getArgb(colix >= 0 ? colix : changableColixMap[-colix]);
  }

  public int[] getShades(short colix) {
    return Colix.getShades(colix >= 0 ? colix : changableColixMap[-colix]);
  }

  public short getColix(int argb) {
    return Colix.getColix(argb);
  }

  public short getColix(Color color) {
    return Colix.getColix(color);
  }

  public short getColix(String colorName) {
    return getColix(getColorFromString(colorName));
  }

  public short getColix(Object obj) {
    if (obj == null)
      return 0;
    if (obj instanceof Color)
      return getColix((Color)obj);
    if (obj instanceof Integer)
      return getColix(((Integer)obj).intValue());
    if (obj instanceof String)
      return getColix((String)obj);
    System.out.println("?? getColix(" + obj + ")");
    return HOTPINK;
  }

  public Color getColor(short colix) {
    return Colix.getColor(colix >= 0 ? colix : changableColixMap[-colix]);
  }

  /****************************************************************
   * changable colixes
   * give me a short ID and a color, and I will give you a colix
   * later, you can reassign the color if you want
   ****************************************************************/

  short[] changableColixMap = new short[16];

  public short getChangableColix(short id, int argb) {
    ++id; // to deal with 0;
    if (id >= changableColixMap.length) {
      short[] t = new short[id + 16];
      System.arraycopy(changableColixMap, 0, t, 0, changableColixMap.length);
      changableColixMap = t;
    }
    if (changableColixMap[id] == 0)
      changableColixMap[id] = getColix(argb);
    return (short)-id;
  }

  public void changeColixArgb(short id, int argb) {
    ++id;
    if (id < changableColixMap.length && changableColixMap[id] != 0)
      changableColixMap[id] = getColix(argb);
  }

  public void flushShadesAndImageCaches() {
    Colix.flushShades();
    Sphere3D.flushImageCache();
  }

  public final static byte shadeMax = Shade3D.shadeMax;
  public final static byte shadeLast = Shade3D.shadeMax - 1;
  public final static byte shadeNormal = Shade3D.shadeNormal;
  public final static byte intensitySpecularSurfaceLimit =
    Shade3D.intensitySpecularSurfaceLimit;

  public void setSpecular(boolean specular) {
    Shade3D.setSpecular(specular);
  }

  public boolean getSpecular() {
    return Shade3D.getSpecular();
  }

  public void setSpecularPower(int specularPower) {
    Shade3D.setSpecularPower(specularPower);
  }

  public void setAmbientPercent(int ambientPercent) {
    Shade3D.setAmbientPercent(ambientPercent);
  }

  public void setDiffusePercent(int diffusePercent) {
    Shade3D.setDiffusePercent(diffusePercent);
  }

  public void setSpecularPercent(int specularPercent) {
    Shade3D.setSpecularPercent(specularPercent);
  }

  public void setLightsourceZ(float dist) {
    Shade3D.setLightsourceZ(dist);
  }

  private final Vector3f vectorAB = new Vector3f();
  private final Vector3f vectorAC = new Vector3f();
  private final Vector3f vectorNormal = new Vector3f();

  // these points are in screen coordinates
  public int calcIntensityScreen(Point3f screenA,
                                 Point3f screenB, Point3f screenC) {
    vectorAB.sub(screenB, screenA);
    vectorAC.sub(screenC, screenA);
    vectorNormal.cross(vectorAB, vectorAC);
    return
      (vectorNormal.z >= 0
            ? Shade3D.calcIntensity(-vectorNormal.x, -vectorNormal.y,
                                    vectorNormal.z)
            : Shade3D.calcIntensity(vectorNormal.x, vectorNormal.y,
                                    -vectorNormal.z));
  }


  static public int calcIntensity(float x, float y, float z) {
    return Shade3D.calcIntensity(x, y, z);
  }

  /* ***************************************************************
   * fontID stuff
   * a fontID is a byte that contains the size + the face + the style
   * ***************************************************************/

  public Font3D getFont3D(int fontSize) {
    return Font3D.getFont3D(Font3D.FONT_FACE_SANS,
                            Font3D.FONT_STYLE_PLAIN, fontSize, platform);
  }

  public Font3D getFont3D(String fontFace, int fontSize) {
    return Font3D.getFont3D(Font3D.getFontFaceID(fontFace),
                            Font3D.FONT_STYLE_PLAIN, fontSize, platform);
  }
    
  // {"Plain", "Bold", "Italic", "BoldItalic"};
  public Font3D getFont3D(String fontFace, String fontStyle, int fontSize) {
    return Font3D.getFont3D(Font3D.getFontFaceID(fontFace),
                            Font3D.getFontStyleID(fontStyle), fontSize, platform);
  }

  public byte getFontFid(int fontSize) {
    return getFont3D(fontSize).fid;
  }

  public byte getFontFid(String fontFace, int fontSize) {
    return getFont3D(fontFace, fontSize).fid;
  }

  public byte getFontFid(String fontFace, String fontStyle, int fontSize) {
    return getFont3D(fontFace, fontStyle, fontSize).fid;
  }

  // 140 JavaScript color names
  // includes 16 official HTML 4.0 color names & values
  // plus a few extra rasmol names

  public final static String[] colorNames = {
    "aliceblue",            // F0F8FF
    "antiquewhite",         // FAEBD7
    "aqua",                 // 00FFFF
    "aquamarine",           // 7FFFD4
    "azure",                // F0FFFF
    "beige",                // F5F5DC
    "bisque",               // FFE4C4
    "black",                // 000000
    "blanchedalmond",       // FFEBCD
    "blue",                 // 0000FF
    "blueviolet",           // 8A2BE2
    "brown",                // A52A2A
    "burlywood",            // DEB887
    "cadetblue",            // 5F9EA0
    "chartreuse",           // 7FFF00
    "chocolate",            // D2691E
    "coral",                // FF7F50
    "cornflowerblue",       // 6495ED
    "cornsilk",             // FFF8DC
    "crimson",              // DC143C
    "cyan",                 // 00FFFF
    "darkblue",             // 00008B
    "darkcyan",             // 008B8B
    "darkgoldenrod",        // B8860B
    "darkgray",             // A9A9A9
    "darkgreen",            // 006400
    "darkkhaki",            // BDB76B
    "darkmagenta",          // 8B008B
    "darkolivegreen",       // 556B2F
    "darkorange",           // FF8C00
    "darkorchid",           // 9932CC
    "darkred",              // 8B0000
    "darksalmon",           // E9967A
    "darkseagreen",         // 8FBC8F
    "darkslateblue",        // 483D8B
    "darkslategray",        // 2F4F4F
    "darkturquoise",        // 00CED1
    "darkviolet",           // 9400D3
    "deeppink",             // FF1493
    "deepskyblue",          // 00BFFF
    "dimgray",              // 696969
    "dodgerblue",           // 1E90FF
    "firebrick",            // B22222
    "floralwhite",          // FFFAF0 16775920
    "forestgreen",          // 228B22
    "fuchsia",              // FF00FF
    "gainsboro",            // DCDCDC
    "ghostwhite",           // F8F8FF
    "gold",                 // FFD700
    "goldenrod",            // DAA520
    "gray",                 // 808080
    "green",                // 008000
    "greenyellow",          // ADFF2F
    "honeydew",             // F0FFF0
    "hotpink",              // FF69B4
    "indianred",            // CD5C5C
    "indigo",               // 4B0082
    "ivory",                // FFFFF0
    "khaki",                // F0E68C
    "lavender",             // E6E6FA
    "lavenderblush",        // FFF0F5
    "lawngreen",            // 7CFC00
    "lemonchiffon",         // FFFACD
    "lightblue",            // ADD8E6
    "lightcoral",           // F08080
    "lightcyan",            // E0FFFF
    "lightgoldenrodyellow", // FAFAD2
    "lightgreen",           // 90EE90
    "lightgrey",            // D3D3D3
    "lightpink",            // FFB6C1
    "lightsalmon",          // FFA07A
    "lightseagreen",        // 20B2AA
    "lightskyblue",         // 87CEFA
    "lightslategray",       // 778899
    "lightsteelblue",       // B0C4DE
    "lightyellow",          // FFFFE0
    "lime",                 // 00FF00
    "limegreen",            // 32CD32
    "linen",                // FAF0E6
    "magenta",              // FF00FF
    "maroon",               // 800000
    "mediumaquamarine",     // 66CDAA
    "mediumblue",           // 0000CD
    "mediumorchid",         // BA55D3
    "mediumpurple",         // 9370DB
    "mediumseagreen",       // 3CB371
    "mediumslateblue",      // 7B68EE
    "mediumspringgreen",    // 00FA9A
    "mediumturquoise",      // 48D1CC
    "mediumvioletred",      // C71585
    "midnightblue",         // 191970
    "mintcream",            // F5FFFA
    "mistyrose",            // FFE4E1
    "moccasin",             // FFE4B5
    "navajowhite",          // FFDEAD
    "navy",                 // 000080
    "oldlace",              // FDF5E6
    "olive",                // 808000
    "olivedrab",            // 6B8E23
    "orange",               // FFA500
    "orangered",            // FF4500
    "orchid",               // DA70D6
    "palegoldenrod",        // EEE8AA
    "palegreen",            // 98FB98
    "paleturquoise",        // AFEEEE
    "palevioletred",        // DB7093
    "papayawhip",           // FFEFD5
    "peachpuff",            // FFDAB9
    "peru",                 // CD853F
    "pink",                 // FFC0CB
    "plum",                 // DDA0DD
    "powderblue",           // B0E0E6
    "purple",               // 800080
    "red",                  // FF0000
    "rosybrown",            // BC8F8F
    "royalblue",            // 4169E1
    "saddlebrown",          // 8B4513
    "salmon",               // FA8072
    "sandybrown",           // F4A460
    "seagreen",             // 2E8B57
    "seashell",             // FFF5EE
    "sienna",               // A0522D
    "silver",               // C0C0C0
    "skyblue",              // 87CEEB
    "slateblue",            // 6A5ACD
    "slategray",            // 708090
    "snow",                 // FFFAFA 16775930
    "springgreen",          // 00FF7F
    "steelblue",            // 4682B4
    "tan",                  // D2B48C
    "teal",                 // 008080
    "thistle",              // D8BFD8
    "tomato",               // FF6347
    "turquoise",            // 40E0D0
    "violet",               // EE82EE
    "wheat",                // F5DEB3
    "white",                // FFFFFF 16777215
    "whitesmoke",           // F5F5F5
    "yellow",               // FFFF00
    "yellowgreen",          // 9ACD32
    // plus a few rasmol names/values
    "bluetint",             // AFD7FF
    "greenblue",            // 2E8B57
    "greentint",            // 98FFB3
    "grey",                 // 808080
    "pinktint",             // FFABBB
    "redorange",            // FF4500
    "yellowtint",           // F6F675
    "pecyan",               // 00ffff
    "pepurple",             // d020ff
    "pegreen",              // 00ff00
    "peblue",               // 6060ff
    "peviolet",             // ff80c0
    "pebrown",              // a42028
    "pepink",               // ffd8d8
    "peyellow",             // ffff00
    "pedarkgreen",          // 00c000
    "peorange",             // ffb000
    "pelightblue",          // b0b0ff
    "pedarkcyan",           // 00a0a0
    "pedarkgray",           // 606060
    "pewhite",              // ffffff
  };

  public final static int[] colorArgbs = {
    0xFFF0F8FF, // aliceblue
    0xFFFAEBD7, // antiquewhite
    0xFF00FFFF, // aqua
    0xFF7FFFD4, // aquamarine
    0xFFF0FFFF, // azure
    0xFFF5F5DC, // beige
    0xFFFFE4C4, // bisque
    0xFF000000, // black
    0xFFFFEBCD, // blanchedalmond
    0xFF0000FF, // blue
    0xFF8A2BE2, // blueviolet
    0xFFA52A2A, // brown
    0xFFDEB887, // burlywood
    0xFF5F9EA0, // cadetblue
    0xFF7FFF00, // chartreuse
    0xFFD2691E, // chocolate
    0xFFFF7F50, // coral
    0xFF6495ED, // cornflowerblue
    0xFFFFF8DC, // cornsilk
    0xFFDC143C, // crimson
    0xFF00FFFF, // cyan
    0xFF00008B, // darkblue
    0xFF008B8B, // darkcyan
    0xFFB8860B, // darkgoldenrod
    0xFFA9A9A9, // darkgray
    0xFF006400, // darkgreen

    0xFFBDB76B, // darkkhaki
    0xFF8B008B, // darkmagenta
    0xFF556B2F, // darkolivegreen
    0xFFFF8C00, // darkorange
    0xFF9932CC, // darkorchid
    0xFF8B0000, // darkred
    0xFFE9967A, // darksalmon
    0xFF8FBC8F, // darkseagreen
    0xFF483D8B, // darkslateblue
    0xFF2F4F4F, // darkslategray
    0xFF00CED1, // darkturquoise
    0xFF9400D3, // darkviolet
    0xFFFF1493, // deeppink
    0xFF00BFFF, // deepskyblue
    0xFF696969, // dimgray
    0xFF1E90FF, // dodgerblue
    0xFFB22222, // firebrick
    0xFFFFFAF0, // floralwhite
    0xFF228B22, // forestgreen
    0xFFFF00FF, // fuchsia
    0xFFDCDCDC, // gainsboro
    0xFFF8F8FF, // ghostwhite
    0xFFFFD700, // gold
    0xFFDAA520, // goldenrod
    0xFF808080, // gray
    0xFF008000, // green
    0xFFADFF2F, // greenyellow
    0xFFF0FFF0, // honeydew
    0xFFFF69B4, // hotpink
    0xFFCD5C5C, // indianred
    0xFF4B0082, // indigo
    0xFFFFFFF0, // ivory
    0xFFF0E68C, // khaki
    0xFFE6E6FA, // lavender
    0xFFFFF0F5, // lavenderblush
    0xFF7CFC00, // lawngreen
    0xFFFFFACD, // lemonchiffon
    0xFFADD8E6, // lightblue
    0xFFF08080, // lightcoral
    0xFFE0FFFF, // lightcyan
    0xFFFAFAD2, // lightgoldenrodyellow
    0xFF90EE90, // lightgreen
    0xFFD3D3D3, // lightgrey
    0xFFFFB6C1, // lightpink
    0xFFFFA07A, // lightsalmon
    0xFF20B2AA, // lightseagreen
    0xFF87CEFA, // lightskyblue
    0xFF778899, // lightslategray
    0xFFB0C4DE, // lightsteelblue
    0xFFFFFFE0, // lightyellow
    0xFF00FF00, // lime
    0xFF32CD32, // limegreen
    0xFFFAF0E6, // linen
    0xFFFF00FF, // magenta
    0xFF800000, // maroon
    0xFF66CDAA, // mediumaquamarine
    0xFF0000CD, // mediumblue
    0xFFBA55D3, // mediumorchid
    0xFF9370DB, // mediumpurple
    0xFF3CB371, // mediumseagreen
    0xFF7B68EE, // mediumslateblue
    0xFF00FA9A, // mediumspringgreen
    0xFF48D1CC, // mediumturquoise
    0xFFC71585, // mediumvioletred
    0xFF191970, // midnightblue
    0xFFF5FFFA, // mintcream
    0xFFFFE4E1, // mistyrose
    0xFFFFE4B5, // moccasin
    0xFFFFDEAD, // navajowhite
    0xFF000080, // navy
    0xFFFDF5E6, // oldlace
    0xFF808000, // olive
    0xFF6B8E23, // olivedrab
    0xFFFFA500, // orange
    0xFFFF4500, // orangered
    0xFFDA70D6, // orchid
    0xFFEEE8AA, // palegoldenrod
    0xFF98FB98, // palegreen
    0xFFAFEEEE, // paleturquoise
    0xFFDB7093, // palevioletred
    0xFFFFEFD5, // papayawhip
    0xFFFFDAB9, // peachpuff
    0xFFCD853F, // peru
    0xFFFFC0CB, // pink
    0xFFDDA0DD, // plum
    0xFFB0E0E6, // powderblue
    0xFF800080, // purple
    0xFFFF0000, // red
    0xFFBC8F8F, // rosybrown
    0xFF4169E1, // royalblue
    0xFF8B4513, // saddlebrown
    0xFFFA8072, // salmon
    0xFFF4A460, // sandybrown
    0xFF2E8B57, // seagreen
    0xFFFFF5EE, // seashell
    0xFFA0522D, // sienna
    0xFFC0C0C0, // silver
    0xFF87CEEB, // skyblue
    0xFF6A5ACD, // slateblue
    0xFF708090, // slategray
    0xFFFFFAFA, // snow
    0xFF00FF7F, // springgreen
    0xFF4682B4, // steelblue
    0xFFD2B48C, // tan
    0xFF008080, // teal
    0xFFD8BFD8, // thistle
    0xFFFF6347, // tomato
    0xFF40E0D0, // turquoise
    0xFFEE82EE, // violet
    0xFFF5DEB3, // wheat
    0xFFFFFFFF, // white
    0xFFF5F5F5, // whitesmoke
    0xFFFFFF00, // yellow
    0xFF9ACD32, // yellowgreen
    // plus a few rasmol names/values
    0xFFAFD7FF, // bluetint
    0xFF2E8B57, // greenblue
    0xFF98FFB3, // greentint
    0xFF808080, // grey
    0xFFFFABBB, // pinktint
    0xFFFF4500, // redorange
    0xFFF6F675, // yellowtint
    // plus the PE chain colors
    0xFF00ffff, // pecyan
    0xFFd020ff, // pepurple
    0xFF00ff00, // pegreen
    0xFF6060ff, // peblue
    0xFFff80c0, // peviolet
    0xFFa42028, // pebrown
    0xFFffd8d8, // pepink
    0xFFffff00, // peyellow
    0xFF00c000, // pedarkgreen
    0xFFffb000, // peorange
    0xFFb0b0ff, // pelightblue
    0xFF00a0a0, // pedarkcyan
    0xFF606060, // pedarkgray
    0xFFffffff, // pewhite
  };

  private static final Hashtable mapJavaScriptColors = new Hashtable();
  static {
    for (int i = colorNames.length; --i >= 0; )
      mapJavaScriptColors.put(colorNames[i], new Color(colorArgbs[i]));
  }

  public static int getArgbFromString(String strColor) {
    /*
    System.out.println("ColorManager.getArgbFromString(" + strColor + ")");
    */
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = Integer.parseInt(strColor.substring(1, 3), 16);
          int grn = Integer.parseInt(strColor.substring(3, 5), 16);
          int blu = Integer.parseInt(strColor.substring(5, 7), 16);
          return (0xFF000000 |
                  (red & 0xFF) << 16 |
                  (grn & 0xFF) << 8  |
                  (blu & 0xFF));
        } catch (NumberFormatException e) {
        }
      } else {
        Color color = (Color)mapJavaScriptColors.get(strColor.toLowerCase());
        if (color != null)
          return color.getRGB();
      }
    }
    return 0;
  }

  public static Color getColorFromString(String strColor) {
    if (strColor != null) {
      if (strColor.length() == 7 && strColor.charAt(0) == '#') {
        try {
          int red = Integer.parseInt(strColor.substring(1, 3), 16);
          int grn = Integer.parseInt(strColor.substring(3, 5), 16);
          int blu = Integer.parseInt(strColor.substring(5, 7), 16);
          return new Color(red, grn, blu);
        } catch (NumberFormatException e) {
        }
      } else {
        Color color = (Color)mapJavaScriptColors.get(strColor.toLowerCase());
        if (color != null)
          return color;
      }
    }
    System.out.println("error converting string to color:" + strColor);
    return Color.pink;
  }
}
