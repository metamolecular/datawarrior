/* $RCSfile: Shade3D.java,v $
 * $Author: nicove $
 * $Date: 2004/11/12 09:38:02 $
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

/**
 *<p>
 * Implements the shading of RGB values to support shadow and lighting
 * highlights.
 *</p>
 *<p>
 * Each RGB value has 64 shades. shade[0] represents ambient lighting.
 * shade[63] is white ... a full specular highlight.
 *</p>
 */
final class Shade3D {

  // there are 64 shades of a given color
  // 0 = ambient
  // 63 = brightest ... white
  static final int shadeMax = 64;
  static final int shadeLast = shadeMax - 1;

  static byte shadeNormal = 52;

  // the light source vector
  static final float xLightsource = -1;
  static final float yLightsource = -1;
  static float zLightsource = 2.5f;
  static float magnitudeLight =
    (float)Math.sqrt(xLightsource * xLightsource +
                     yLightsource * yLightsource +
                     zLightsource * zLightsource);
  // the light source vector normalized
  static float xLight = xLightsource / magnitudeLight;
  static float yLight = yLightsource / magnitudeLight;
  static float zLight = zLightsource / magnitudeLight;

  // the viewer vector is always 0,0,1

  // set specular on|off
  static boolean specularOn = true;
  // set specular 0-100
  static float intensitySpecular = 0.22f;
  // set specpower -5
  static int specularExponent = 6;
  // set specpower 0-100
  static float intenseFraction = 0.4f;
  // set diffuse 0-100
  static float intensityDiffuse = 0.84f;
  // set ambient 0-100
  static float ambientFraction = 0.45f;

  static int[] getShades(int rgb) {
    int[] shades = new int[shadeMax];

    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;

    float ambientRange = 1 - ambientFraction;

    shades[shadeNormal] = rgb(red, grn, blu);
    for (int i = 0; i < shadeNormal; ++i) {
      float fraction = ambientFraction + ambientRange*i/shadeNormal;
      shades[i] = rgb((int)(red*fraction + 0.5f),
                      (int)(grn*fraction + 0.5f),
                      (int)(blu*fraction + 0.5f));
    }

    int nSteps = shadeMax - shadeNormal - 1;
    float redRange = (255 - red) * intenseFraction;
    float grnRange = (255 - grn) * intenseFraction;
    float bluRange = (255 - blu) * intenseFraction;

    for (int i = 1; i <= nSteps; ++i) {
      shades[shadeNormal + i] = rgb(red + (int)(redRange * i / nSteps + 0.5f),
                                    grn + (int)(grnRange * i / nSteps + 0.5f),
                                    blu + (int)(bluRange * i / nSteps + 0.5f));
    }
    return shades;
  }

  private final static int rgb(int red, int grn, int blu) {
    return 0xFF000000 | (red << 16) | (grn << 8) | blu;
  }

  static String StringFromRgb(int rgb) {
    int red = (rgb >> 16) & 0xFF;
    int grn = (rgb >>  8) & 0xFF;
    int blu = rgb         & 0xFF;

    return "[" + red + "," + grn + "," + blu + "]";
  }

  final static byte intensitySpecularSurfaceLimit = (byte)(shadeNormal + 4);

  static byte calcIntensity(float x, float y, float z) {
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return (byte)(calcFloatIntensityNormalized((float)(x/magnitude),
                                               (float)(y/magnitude),
                                               (float)(z/magnitude))
                  * shadeLast + 0.5f);
  }

  static int calcFp8Intensity(float x, float y, float z) {
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return (int)(calcFloatIntensityNormalized((float)(x/magnitude),
                                              (float)(y/magnitude),
                                              (float)(z/magnitude))
                 * shadeLast * (1 << 8));
  }

  static float calcFloatIntensity(float x, float y, float z) {
    double magnitude = Math.sqrt(x*x + y*y + z*z);
    return calcFloatIntensityNormalized((float)(x/magnitude),
                                        (float)(y/magnitude),
                                        (float)(z/magnitude));
  }

  static float calcFloatIntensityNormalized(float x, float y, float z) {
    float cosTheta = x*xLight + y*yLight + z*zLight;
    float intensity = 0; // ambient component
    if (cosTheta > 0) {
      intensity += cosTheta * intensityDiffuse; // diffuse component
      
      if (specularOn) {
        // this is the dot product of the reflection and the viewer
        // but the viewer only has a z component
        float dotProduct = z * 2 * cosTheta - zLight;
        if (dotProduct > 0) {
          for (int n = specularExponent; --n >= 0 && dotProduct > .0001f; )
            dotProduct *= dotProduct;
          // specular component
          intensity += dotProduct * intensitySpecular;
        }
      }
    }
    if (intensity > 1)
      return 1;
    return intensity;
  }

  static byte calcDitheredNoisyIntensity(float x, float y, float z) {
    // add some randomness to prevent banding
    int fp8Intensity = calcFp8Intensity(x, y, z);
    int intensity = fp8Intensity >> 8;
    // this cannot overflow because the if the float intensity is 1.0
    // then intensity will be == shadeLast
    // but there will be no fractional component, so the next test will fail
    if ((fp8Intensity & 0xFF) > nextRandom8Bit())
      ++intensity;
    int random16bit = seed & 0xFFFF;
    if (random16bit < 65536 / 3 && intensity > 0)
      --intensity;
    else if (random16bit > 65536 * 2 / 3 && intensity < shadeLast)
      ++intensity;
    return (byte)intensity;
  }

  static byte calcDitheredNoisyIntensity(float x, float y, float z, float r) {
    // add some randomness to prevent banding
    int fp8Intensity = (int)(calcFloatIntensityNormalized(x/r, y/r, z/r)
			* shadeLast * (1 << 8));
    int intensity = fp8Intensity >> 8;
    // this cannot overflow because the if the float intensity is 1.0
    // then intensity will be == shadeLast
    // but there will be no fractional component, so the next test will fail
    if ((fp8Intensity & 0xFF) > nextRandom8Bit())
      ++intensity;
    int random16bit = seed & 0xFFFF;
    if (random16bit < 65536 / 3 && intensity > 0)
      --intensity;
    else if (random16bit > 65536 * 2 / 3 && intensity < shadeLast)
      ++intensity;
    return (byte)intensity;
  }

  /*
    This is a linear congruential pseudorandom number generator,
    as defined by D. H. Lehmer and described by Donald E. Knuth in
    The Art of Computer Programming,
    Volume 2: Seminumerical Algorithms, section 3.2.1.

  static long seed = 1;
  static int nextRandom8Bit() {
    seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    //    return (int)(seed >>> (48 - bits));
    return (int)(seed >>> 40);
  }
  */

  // this doesn't really need to be synchronized
  // no serious harm done if two threads write seed at the same time
  private static int seed = 0x12345679; // turn lo bit on
  /**
   *<p>
   * Implements RANDU algorithm for random noise in lighting/shading.
   *</p>
   *<p>
   * RANDU is the classic example of a poor random number generator.
   * But it is very cheap to calculate and is good enough for our purposes.
   *</p>
   *
   * @return Next random
   */
  static int nextRandom8Bit() {
    int t = seed;
    seed = t = ((t << 16) + (t << 1) + t) & 0x7FFFFFFF;
    return t >> 23;
  }

  static void setSpecular(boolean specular) {
    specularOn = specular;
    dump();
  }

  static boolean getSpecular() {
    return specularOn;
  }

  static void setLightsourceZ(float z) {
    zLightsource = z;
    magnitudeLight =
      (float)Math.sqrt(xLightsource * xLightsource +
                       yLightsource * yLightsource +
                       zLightsource * zLightsource);
    dump();
  }

  static void setSpecularPower(int specularPower) {
    if (specularPower >= 0)
      intenseFraction = specularPower / 100f;
    else
      specularExponent = -specularPower;
    dump();
  }

  static void setAmbientPercent(int ambientPercent) {
    ambientFraction = ambientPercent / 100f;
    dump();
  }

  static void setDiffusePercent(int diffusePercent) {
    intensityDiffuse = diffusePercent / 100f;
    dump();
  }

  static void setSpecularPercent(int specularPercent) {
    intensitySpecular = specularPercent / 100f;
    dump();
  }

  static void dump() {
    System.out.println("\n ambientPercent=" + ambientFraction +
                       "\n diffusePercent=" + intensityDiffuse +
                       "\n specularOn=" + specularOn +
                       "\n specularPercent=" + intensitySpecular +
                       "\n specularPower=" + intenseFraction +
                       "\n specularExponent=" + specularExponent +
                       "\n zLightsource=" + zLightsource +
                       "\n shadeNormal=" + shadeNormal);
  }
}
