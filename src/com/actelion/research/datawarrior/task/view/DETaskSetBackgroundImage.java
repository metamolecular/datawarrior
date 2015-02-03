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

package com.actelion.research.datawarrior.task.view;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.clipboard.ImageClipboardHandler;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.BinaryDecoder;
import com.actelion.research.util.BinaryEncoder;


public class DETaskSetBackgroundImage extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Background Image";

	private static final String PROPERTY_IMAGE_DATA = "imageData";
	private static final String PROPERTY_HIDE_SCALE = "showScale";

	private static Properties sRecentConfiguration;

	private Frame		   mParentFrame;
	private JCheckBox	   mCheckboxHideScale;
	private JPanel		  mBackgroundImagePreview;
	private BufferedImage	mBackgroundImage;

	public DETaskSetBackgroundImage(Frame owner, DEMainPane mainPane, VisualizationPanel2D view) {
		super(owner, mainPane, view);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public JComponent createInnerDialogContent() {
		double size2[][] = { {8, TableLayout.PREFERRED, 4, TableLayout.FILL, 4, TableLayout.PREFERRED, 8},
							 {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16, 160, 16, TableLayout.PREFERRED, 8} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size2));
		p.add(new JLabel("Import from file:"), "1,1,3,1");
		p.add(new JLabel("Import from clipboard:"), "1,3,3,3");
		p.add(new JLabel("Remove background image:"), "1,5,3,5");
		JButton bOpen = new JButton("Open...");
		bOpen.addActionListener(this);
		p.add(bOpen, "5,1");
		JButton bPaste = new JButton("Paste");
		bPaste.addActionListener(this);
		p.add(bPaste, "5,3");
		JButton bClear = new JButton("Clear");
		bClear.addActionListener(this);
		p.add(bClear, "5,5");
		JLabel previewLabel = new JLabel("Preview:");
		previewLabel.setVerticalAlignment(SwingConstants.TOP);
		p.add(previewLabel, "1,7");
		mBackgroundImagePreview = new JPanel() {
			private static final long serialVersionUID = 0x20080611;
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				Dimension size = getSize();
				if (mBackgroundImage == null) {
					g.setColor(Color.LIGHT_GRAY);
					g.drawRect(0, 0, size.width-1, size.height-1);
					}
				else {
					g.drawImage(mBackgroundImage, 0, 0, size.width, size.height, null);
					}
				}
			};
		p.add(mBackgroundImagePreview, "3,7,5,7");
		mCheckboxHideScale = new JCheckBox("Hide scale and grid lines");
		mCheckboxHideScale.setHorizontalAlignment(SwingConstants.CENTER);
		mCheckboxHideScale.addActionListener(this);
		p.add(mCheckboxHideScale, "1,9,5,9");

		return p;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel2D) ? null : "Background images can only be shown in 2D-Views.";
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Clear")) {
			mCheckboxHideScale.setSelected(false);
			mBackgroundImage = null;
			mBackgroundImagePreview.repaint();
			}

		if (e.getActionCommand().equals("Open...")) {
			FileFilter filter = CompoundFileHelper.createFileFilter(CompoundFileHelper.cFileTypeJPG
																  | CompoundFileHelper.cFileTypePNG, false);
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory(FileHelper.getCurrentDirectory());
			fileChooser.setFileFilter(filter);
			int option = fileChooser.showOpenDialog(mParentFrame);
			FileHelper.setCurrentDirectory(fileChooser.getCurrentDirectory());
			if (option != JFileChooser.APPROVE_OPTION)
				return;
			try {
				File file = fileChooser.getSelectedFile();
				InputStream is = new FileInputStream(file);
				long length = file.length();

				if (length > 4000000) {
					JOptionPane.showMessageDialog(mParentFrame, "Image file size exceeds limit.");
					is.close();
					return;
					}
			
				byte[] imageData = new byte[(int)length];
				int offset = 0;
				int numRead = 0;
				while (offset < imageData.length
					&& (numRead=is.read(imageData, offset, imageData.length-offset)) >= 0) {
					offset += numRead;
					}
				is.close();
			
				if (offset < imageData.length) {
					JOptionPane.showMessageDialog(mParentFrame, "Could not completely read file "+file.getName());
					return;
					}

				mBackgroundImage = ImageIO.read(new ByteArrayInputStream(imageData));
				mBackgroundImagePreview.repaint();
				mCheckboxHideScale.setEnabled(true);
				}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(mParentFrame, "Couldn't read file.");
				return;
				}
			}

		if (e.getActionCommand().equals("Paste")) {
			Image image = ImageClipboardHandler.pasteImage();
			if (image != null) {
				mBackgroundImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
				Graphics g = mBackgroundImage.createGraphics();
				g.drawImage(image, 0, 0, null);
				g.dispose();
				mBackgroundImagePreview.repaint();
				mCheckboxHideScale.setEnabled(true);
				}
			}

		super.actionPerformed(e);
		}

	@Override
	public void addViewConfiguration(Properties configuration) {
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel2D)getInteractiveView()).getVisualization();
		byte[] image = visualization.getBackgroundImageData();
		if (image != null) {
			configuration.put(PROPERTY_IMAGE_DATA, BinaryEncoder.toString(image, 8));
			configuration.put(PROPERTY_HIDE_SCALE, visualization.isScaleSuppressed() ? "true" : "false");
			}
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		if (mBackgroundImage != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(mBackgroundImage, "png", baos);
				} catch (IOException e) {}
			byte[] imageData = baos.toByteArray();
			configuration.put(PROPERTY_IMAGE_DATA, BinaryEncoder.toString(imageData, 8));
			configuration.put(PROPERTY_HIDE_SCALE, mCheckboxHideScale.isSelected() ? "true" : "false");
			}
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String imageString = configuration.getProperty(PROPERTY_IMAGE_DATA);
		if (imageString != null) {
			try {
				byte[] bytes = BinaryDecoder.toBytes(imageString, 8);
				mBackgroundImage = ImageIO.read(new ByteArrayInputStream(bytes));
				}
			catch (IOException e) {
				mBackgroundImage = null;
				}
			}
		else {
			mBackgroundImage = null;
			}

		mBackgroundImagePreview.repaint();
		mCheckboxHideScale.setSelected("true".equals(configuration.getProperty(PROPERTY_HIDE_SCALE)));
		}

	@Override
	public void enableItems() {
		mCheckboxHideScale.setEnabled(mBackgroundImage != null);
		}

	@Override
	public void setDialogToDefault() {
		mBackgroundImage = null;
		mBackgroundImagePreview.repaint();
		mCheckboxHideScale.setSelected(false);
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel2D)view).getVisualization();
		String imageString = configuration.getProperty(PROPERTY_IMAGE_DATA);
		if (imageString == null) {
			visualization.setBackgroundImageData(null);
			visualization.setSuppressScale(false, false);
			}
		else {
			visualization.setBackgroundImageData(BinaryDecoder.toBytes(imageString, 8));
			boolean hideScale = "true".equals(configuration.getProperty(PROPERTY_HIDE_SCALE));
			visualization.setSuppressScale(hideScale, hideScale);
			}
		}
	
	@Override
	public Properties getRecentConfigurationLocal() {
		return sRecentConfiguration;
		}
	
	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}
	}
