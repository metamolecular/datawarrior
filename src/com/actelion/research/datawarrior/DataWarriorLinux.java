/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior;

import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.fonts.FontPolicy;
import org.jvnet.substance.fonts.FontSet;
import org.jvnet.substance.theme.SubstanceBrownTheme;
import org.jvnet.substance.theme.SubstanceMixTheme;
import org.jvnet.substance.theme.SubstanceSunGlareTheme;
import org.jvnet.substance.theme.SubstanceTheme;
import org.jvnet.substance.utils.SubstanceConstants.TabContentPaneBorderKind;

import com.actelion.research.util.Platform;

public class DataWarriorLinux extends DataWarrior {
	protected static DataWarriorLinux sDataExplorer;
	protected static ArrayList<String> sPendingDocumentList;

	private static class SubstanceFontSet implements FontSet {
		private int extra;
		private FontSet delegate;

		/**
		 * @param delegate The base Substance font set.
		 * @param extra Extra size in pixels. Can be positive or negative.
		 */
		public SubstanceFontSet(FontSet delegate, int extra) {
			super();
			this.delegate = delegate;
			this.extra = extra;
			}

		/**
		 * @param systemFont Original font.
		 * @return Wrapped font.
		 */
		private FontUIResource getWrappedFont(FontUIResource systemFont) {
			return new FontUIResource(systemFont.getFontName(), systemFont.getStyle(),
									  systemFont.getSize() + this.extra);
			}

		public FontUIResource getControlFont() {
			return this.getWrappedFont(this.delegate.getControlFont());
			}

		public FontUIResource getMenuFont() {
			return this.getWrappedFont(this.delegate.getMenuFont());
			}

		public FontUIResource getMessageFont() {
			return this.getWrappedFont(this.delegate.getMessageFont());
			}

		public FontUIResource getSmallFont() {
			return this.getWrappedFont(this.delegate.getSmallFont());
			}

		public FontUIResource getTitleFont() {
			return this.getWrappedFont(this.delegate.getTitleFont());
			}

		public FontUIResource getWindowTitleFont() {
			return this.getWrappedFont(this.delegate.getWindowTitleFont());
			}
		}

	/**
	 *  This is called from the Windows bootstrap process instead of main(), when
	 *  the user tries to open a new DataWarrior instance while one is already running.
	 * @param args
	 */
	public static void initSingleApplication(String[] args) {
		if (sDataExplorer == null) {
			if (sPendingDocumentList == null)
				sPendingDocumentList = new ArrayList<String>();
			for (String arg:args)
				sPendingDocumentList.add(arg);
			}
		else {
			for (final String arg:args) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							sDataExplorer.readFile(arg);
							}
						});
					}
				catch(Exception e) {}
				}
			}
		}

	public boolean isMacintosh() {
		return false;
		}

	protected static void setSubstanceLookAndFeel() {
		try {
			if (Platform.isLinux()) {
				// reduce the default font size a little
				final FontSet substanceCoreFontSet = SubstanceLookAndFeel.getFontPolicy().getFontSet("Substance", null);
				FontPolicy newFontPolicy = new FontPolicy() {
					public FontSet getFontSet(String lafName, UIDefaults table) {
						return new SubstanceFontSet(substanceCoreFontSet, -1);
						}
					};
				SubstanceLookAndFeel.setFontPolicy(newFontPolicy);
				}

			UIManager.setLookAndFeel(new SubstanceLookAndFeel());

			if (System.getProperty("development") != null) {
				// nice yellow-brown based mixed look and feel
				SubstanceTheme t2 = new SubstanceSunGlareTheme();
				SubstanceTheme t3 = new SubstanceBrownTheme();
				SubstanceLookAndFeel.setCurrentTheme(new SubstanceMixTheme(t3, t2));
				}
			else {
				SubstanceTheme t1 = new org.jvnet.substance.theme.SubstanceLightAquaTheme().hueShift(0.04);
				SubstanceLookAndFeel.setCurrentTheme(t1);
				}
			UIManager.put(SubstanceLookAndFeel.TABBED_PANE_CONTENT_BORDER_KIND, TabContentPaneBorderKind.SINGLE_PLACEMENT);

//javax.imageio.ImageIO.write(SubstanceImageCreator.getArrow(11,7,2,3,new org.jvnet.substance.theme.SubstanceLightAquaTheme().hueShift(0.04)),
//"png", new java.io.File("/home/sandert/arrow0.png"));

//			JFrame.setDefaultLookAndFeelDecorated(true);
//			JDialog.setDefaultLookAndFeelDecorated(true);
			
/*		  System.out.println(SubstanceLookAndFeel.getColorScheme().getForegroundColor()+" ForegroundColor");
			System.out.println(SubstanceLookAndFeel.getColorScheme().getUltraLightColor()+" UltraLight");
			System.out.println(SubstanceLookAndFeel.getColorScheme().getExtraLightColor()+" ExtraLight");
			System.out.println(SubstanceLookAndFeel.getColorScheme().getLightColor()+" Light");
			System.out.println(SubstanceLookAndFeel.getColorScheme().getMidColor()+" MidColor");
			System.out.println(SubstanceLookAndFeel.getColorScheme().getDarkColor()+" DarkColor");
			System.out.println(SubstanceLookAndFeel.getColorScheme().getUltraDarkColor()+" UltraDarkColor"); */

//				  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//				  UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Unexpected Exception: "+e);
			}
		}

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setSubstanceLookAndFeel();
//				try { UIManager.setLookAndFeel( "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"); } catch(Exception e) {}

				try {
					sDataExplorer = new DataWarriorLinux();

					if (args != null)
						for (String arg:args)
							sDataExplorer.readFile(arg);
					if (sPendingDocumentList != null)
						for (String doc:sPendingDocumentList)
							sDataExplorer.readFile(doc);
					}
				catch(Exception e) {
					e.printStackTrace();
					}
				}
			} );
		}
	}