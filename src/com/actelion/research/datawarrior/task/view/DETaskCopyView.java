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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import com.actelion.research.datawarrior.DEFormView;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.clipboard.ImageClipboardHandler;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.form.FormModel;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableFormModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JCompoundTableForm;
import com.actelion.research.table.view.JStructureGrid;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.JVisualization3D;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel3D;


public class DETaskCopyView extends DEAbstractViewTask implements ActionListener {
	public static final String TASK_NAME = "Create View Image";

	private static final String[] DPI = { "75", "150", "300", "600" };	// need to be multiples of 75

	private static final String PROPERTY_IMAGE_WIDTH = "width";
	private static final String PROPERTY_IMAGE_HEIGHT = "height";
	private static final String PROPERTY_RESOLUTION = "dpi";
	private static final String PROPERTY_KEEP_ASPECT_RATIO = "keepAspect";
	private static final String PROPERTY_FORMAT = "format";
	private static final String PROPERTY_TARGET = "target";
	private static final String PROPERTY_FILENAME = "fileName";
	private static final String PROPERTY_TRANSPARENT_BG = "transparentBG";

	private static final String TARGET_CLIPBOARD = "clipboard";
	private static final String TARGET_FILE = "file";
	private static final String FORMAT_PNG = "png";
	private static final String FORMAT_SVG = "svg";

	private static Properties sRecentConfiguration;

	private DEMainPane		mMainPane;
	private JTextField		mTextFieldWidth,mTextFieldHeight;
	private JCheckBox		mCheckBoxKeepAspectRatio,mCheckBoxTransparentBG;
	private JComboBox		mComboBoxResolution;
	private JRadioButton	mRadioButtonCopy,mRadioButtonSaveAsPNG,mRadioButtonSaveAsSVG;
	private JButton			mButtonEdit;
	private JFilePathLabel	mLabelFileName;
	private int				mCurrentResolutionFactor;
	private boolean			mDisableEvents,mCheckOverwrite;

	public DETaskCopyView(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		super(parent, mainPane, view);
		mMainPane = mainPane;
		mCheckOverwrite = true;
		}

	public void keyTyped(KeyEvent arg0) {}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel)
			|| (view instanceof JStructureGrid)
			|| (view instanceof DEFormView) ? null
					: "Images can be created from 2D-, 3D- and structure views only.";
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
	public String getDialogTitle() {
		return "Create Image From View";
		}

	@Override
	public JComponent createInnerDialogContent() {
		final JComponent _view = getViewComponent(getInteractiveView());

		JPanel mainpanel = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 2, TableLayout.PREFERRED,
							 8,	TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED,
								TableLayout.PREFERRED, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 16,
								TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };
		mainpanel.setLayout(new TableLayout(size));

		Dimension imageSize = getDefaultImageSize(_view);
		mTextFieldWidth = new JTextField(""+imageSize.width);
		mTextFieldHeight = new JTextField(""+imageSize.height);
		mCheckBoxKeepAspectRatio = new JCheckBox("Keep aspect ratio");
		mCheckBoxKeepAspectRatio.addActionListener(this);
 
		if (_view != null && _view instanceof JStructureGrid) {
			mTextFieldHeight.setEnabled(false);
			mCheckBoxKeepAspectRatio.setEnabled(false);
			}

		mTextFieldWidth.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent arg0) {
				try {
					int width = Integer.parseInt(mTextFieldWidth.getText());
					int height = calculateImageHeight(_view, width, mCheckBoxKeepAspectRatio.isSelected());
					if (height != -1)
						mTextFieldHeight.setText(""+height);
					}
				catch (NumberFormatException nfe) {
					mTextFieldHeight.setText("");
					}
				}
			});
		mTextFieldHeight.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent arg0) {
				try {
					int height = Integer.parseInt(mTextFieldHeight.getText());
					int width = calculateImageWidth(_view, height, mCheckBoxKeepAspectRatio.isSelected());
					if (width != -1)
						mTextFieldWidth.setText(""+width);
					}
				catch (NumberFormatException nfe) {
					mTextFieldWidth.setText("");
					}
				}
			});

		mTextFieldWidth.setColumns(6);
		mTextFieldHeight.setColumns(6);
		mainpanel.add(new JLabel("Image width:", JLabel.RIGHT), "1,1");
		mainpanel.add(mTextFieldWidth, "3,1");
		mainpanel.add(new JLabel("Image height:", JLabel.RIGHT), "1,3");
		mainpanel.add(mTextFieldHeight, "3,3");
		mainpanel.add(mCheckBoxKeepAspectRatio, "1,5,3,5");

		mComboBoxResolution = new JComboBox(DPI);
		mComboBoxResolution.addActionListener(this);
		mainpanel.add(new JLabel("Image resolution in dpi:"), "1,7");
		mainpanel.add(mComboBoxResolution, "3,7");

		ButtonGroup group = new ButtonGroup();
		mRadioButtonCopy = new JRadioButton("Copy image to clipboard", true);
		mRadioButtonCopy.addActionListener(this);
		group.add(mRadioButtonCopy);
		mainpanel.add(mRadioButtonCopy, "1,9,3,9");

		mRadioButtonSaveAsPNG = new JRadioButton("Save image as PNG-file", false);
		mRadioButtonSaveAsPNG.addActionListener(this);
		group.add(mRadioButtonSaveAsPNG);
		mainpanel.add(mRadioButtonSaveAsPNG, "1,10,3,10");

		mRadioButtonSaveAsSVG = new JRadioButton("Save image as SVG-file", false);
		mRadioButtonSaveAsSVG.addActionListener(this);
		group.add(mRadioButtonSaveAsSVG);
		if (_view != null && !supportsSVG(_view))
			mRadioButtonSaveAsSVG.setEnabled(false);
		mainpanel.add(mRadioButtonSaveAsSVG, "1,11,3,11");

		mCheckBoxTransparentBG = new JCheckBox("Use transparent background");
		mainpanel.add(mCheckBoxTransparentBG, "1,13,3,13");

		if (_view != null && _view instanceof VisualizationPanel3D)
			mCheckBoxTransparentBG.setEnabled(false);

		mainpanel.add(new JLabel("File name:"), "1,15");
		mButtonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		mButtonEdit.addActionListener(this);
		mainpanel.add(mButtonEdit, "3,15");

		mLabelFileName = new JFilePathLabel(_view == null);
		mainpanel.add(mLabelFileName, "1,17,3,17");

		return mainpanel;
		}

	private Dimension getDefaultImageSize(Component c) {
		Dimension size = new Dimension(1200, 1200);
		if (c != null && c instanceof JStructureGrid)
			size.height = ((JStructureGrid)c).getTotalHeight(1200);
		return size;
		}

	private Dimension getImageSize(Properties configuration, JComponent viewComponent) {
		boolean keepAspectRatio = "true".equals(configuration.getProperty(PROPERTY_KEEP_ASPECT_RATIO));
		int width = Integer.parseInt(configuration.getProperty(PROPERTY_IMAGE_WIDTH, "0"));
		int height = Integer.parseInt(configuration.getProperty(PROPERTY_IMAGE_HEIGHT, "0"));
		if (viewComponent != null && keepAspectRatio) {
			if (width == 0 && height != 0)
				width = viewComponent.getWidth() * height / viewComponent.getHeight();
			if (height == 0 && width != 0)
				height = viewComponent.getHeight() * width / viewComponent.getWidth();
			}
		return new Dimension(width, height);
		}

	private void ensureSizeConstraints(boolean keepAspectRatio) {
		JComponent viewComponent = getViewComponent(getInteractiveView());
		if (viewComponent != null) {
			int width = -1;
			try {
				width = Integer.parseInt(mTextFieldWidth.getText());
				}
			catch (NumberFormatException nfe) {}
			int height = -1;
			try {
				height = Integer.parseInt(mTextFieldHeight.getText());
				}
			catch (NumberFormatException nfe) {}
			if (width > 0) {
				height = calculateImageHeight(viewComponent, width, keepAspectRatio);
				if (height != -1)
					mTextFieldHeight.setText(""+height);
				}
			else if (height > 0) {
				width = calculateImageWidth(viewComponent, height, keepAspectRatio);
				if (width != -1)
					mTextFieldWidth.setText(""+width);
				}
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (mDisableEvents)
			return;

		if (e.getSource() == mRadioButtonCopy
		 || e.getSource() == mRadioButtonSaveAsPNG
		 || e.getSource() == mRadioButtonSaveAsSVG) {
			mButtonEdit.setEnabled(!mRadioButtonCopy.isSelected());
			if (e.getSource() != mRadioButtonCopy) {	// correct file extension if needed
				String path = mLabelFileName.getPath();
				if (path != null) {
					int filetype = mRadioButtonSaveAsSVG.isSelected() ? FileHelper.cFileTypeSVG : FileHelper.cFileTypePNG;
					if (filetype != FileHelper.getFileType(path)) {
						path = FileHelper.removeExtension(path) + FileHelper.getExtension(filetype);
						mLabelFileName.setPath(path);
						mCheckOverwrite = true;
						}
					}
				}
			return;
			}
		if (e.getSource() == mButtonEdit) {
			int filetype = mRadioButtonSaveAsSVG.isSelected() ? FileHelper.cFileTypeSVG : FileHelper.cFileTypePNG;
			String filename = resolveVariables(mLabelFileName.getPath());
			if (filename == null) {
				Dockable dockable = mMainPane.getSelectedDockable();
				filename = (dockable == null) ? null : dockable.getTitle();
				}
			filename = new FileHelper(getParentFrame()).selectFileToSave("Save Image To File", filetype, filename);
			if (filename != null) {
				mLabelFileName.setPath(filename);
				mCheckOverwrite = false;
				}
			return;
			}
		if (e.getSource() == mCheckBoxKeepAspectRatio) {
			if (mCheckBoxKeepAspectRatio.isSelected())
			   	ensureSizeConstraints(true);

			return;
			}
		if (e.getSource() == mComboBoxResolution) {
			int newResolutionFactor = Integer.parseInt((String)mComboBoxResolution.getSelectedItem())/75;
			try {
				int width = Integer.parseInt(mTextFieldWidth.getText());
				mTextFieldWidth.setText(""+(width*newResolutionFactor/mCurrentResolutionFactor));
				}
			catch (NumberFormatException nfe) {}
			try {
				int height = Integer.parseInt(mTextFieldHeight.getText());
				mTextFieldHeight.setText(""+(height*newResolutionFactor/mCurrentResolutionFactor));
				}
			catch (NumberFormatException nfe) {}
			mCurrentResolutionFactor = newResolutionFactor;
			return;
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		configuration.setProperty(PROPERTY_IMAGE_WIDTH, mTextFieldWidth.getText());
		configuration.setProperty(PROPERTY_IMAGE_HEIGHT, mTextFieldHeight.getText());
		configuration.setProperty(PROPERTY_RESOLUTION, DPI[mComboBoxResolution.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_KEEP_ASPECT_RATIO, mCheckBoxKeepAspectRatio.isSelected()?"true":"false");
		configuration.setProperty(PROPERTY_TRANSPARENT_BG, mCheckBoxTransparentBG.isSelected()?"true":"false");
		if (mRadioButtonCopy.isSelected()) {
			configuration.setProperty(PROPERTY_TARGET, TARGET_CLIPBOARD);
			configuration.setProperty(PROPERTY_FORMAT, FORMAT_PNG);
			}
		else if (mRadioButtonSaveAsPNG.isSelected()) {
			configuration.setProperty(PROPERTY_TARGET, TARGET_FILE);
			configuration.setProperty(PROPERTY_FORMAT, FORMAT_PNG);
			}
		else if (mRadioButtonSaveAsSVG.isSelected()) {
			configuration.setProperty(PROPERTY_TARGET, TARGET_FILE);
			configuration.setProperty(PROPERTY_FORMAT, FORMAT_SVG);
			}

		if (!mRadioButtonCopy.isSelected()) {
			String fileName = mLabelFileName.getPath();
			if (fileName != null)
				configuration.setProperty(PROPERTY_FILENAME, fileName);
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mDisableEvents = true;
		mTextFieldWidth.setText(configuration.getProperty(PROPERTY_IMAGE_WIDTH));
		mTextFieldHeight.setText(configuration.getProperty(PROPERTY_IMAGE_HEIGHT));
		mCheckBoxKeepAspectRatio.setSelected("true".equals(configuration.getProperty(PROPERTY_KEEP_ASPECT_RATIO, "true")));
		mCheckBoxTransparentBG.setSelected(mCheckBoxTransparentBG.isEnabled() && "true".equals(configuration.getProperty(PROPERTY_TRANSPARENT_BG)));
		mComboBoxResolution.setSelectedItem(configuration.getProperty(PROPERTY_RESOLUTION, "300"));
		mCurrentResolutionFactor = Integer.parseInt((String)mComboBoxResolution.getSelectedItem())/75;
	   	ensureSizeConstraints(mCheckBoxKeepAspectRatio.isSelected());
	   	if (TARGET_CLIPBOARD.equals(configuration.getProperty(PROPERTY_TARGET)))
	   		mRadioButtonCopy.setSelected(true);
	   	else if (FORMAT_PNG.equals(configuration.getProperty(PROPERTY_FORMAT)))
	   		mRadioButtonSaveAsPNG.setSelected(true);
	   	else
	   		mRadioButtonSaveAsSVG.setSelected(true);
	   	mButtonEdit.setEnabled(!mRadioButtonCopy.isSelected());

		String filename = configuration.getProperty(PROPERTY_FILENAME);
		if (filename != null && !new File(filename).exists())
			filename = null;
		mLabelFileName.setPath(filename);
		mDisableEvents = false;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mDisableEvents = true;
		JComponent viewComponent = getViewComponent(getInteractiveView());
		mCurrentResolutionFactor = 2;	// reflects 150 dpi set below
		mTextFieldWidth.setText(""+(mCurrentResolutionFactor*(viewComponent == null ? 640 : viewComponent.getWidth())));
		mTextFieldHeight.setText(""+(mCurrentResolutionFactor*(viewComponent == null ? 640 : viewComponent.getHeight())));
		ensureSizeConstraints(true);
		mCheckBoxKeepAspectRatio.setSelected(true);
		mCheckBoxTransparentBG.setSelected(false);
		mComboBoxResolution.setSelectedItem("150");	// reflects resolution factor 2 above
   		mRadioButtonCopy.setSelected(true);
	   	mButtonEdit.setEnabled(false);
		mLabelFileName.setPath(null);
		mDisableEvents = false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		JComponent viewComponent = getViewComponent(getConfiguredView(configuration));
		try {
			Dimension size = getImageSize(configuration, viewComponent);
			if (size.width < 32 || size.height < 32) {
				showErrorMessage("Width or height is too small.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Width or height is not numerical.");
			return false;
			}

		if (isLive && !supportsSVG(viewComponent) && FORMAT_SVG.equals(configuration.getProperty(PROPERTY_FORMAT))) {
			showErrorMessage("Only structure-, form-, and 2D-views can be saved as SVG.");
			return false;
			}

		if (isLive && viewComponent instanceof JVisualization3D && "true".equals(configuration.getProperty(PROPERTY_TRANSPARENT_BG))) {
			showErrorMessage("The 3D-view does not support a transparent background.");
			return false;
			}

		if (isLive) {
			String target = configuration.getProperty(PROPERTY_TARGET, TARGET_CLIPBOARD);
			if (target.equals(TARGET_FILE)
			 && !isFileAndPathValid(configuration.getProperty(PROPERTY_FILENAME), true, mCheckOverwrite))
				return false;
			}

		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public Properties getRecentConfiguration() {
		return sRecentConfiguration;
		}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
		}

	@Override
	public void runTask(Properties configuration) {
		JComponent viewComponent = getViewComponent(getConfiguredView(configuration));
		Dimension size = getImageSize(configuration, viewComponent);

		int dpi = 300;
		try { dpi = Integer.parseInt(configuration.getProperty(PROPERTY_RESOLUTION)); } catch (NumberFormatException nfe) {}

		try {
			boolean transparentBG = "true".equals(configuration.getProperty(PROPERTY_TRANSPARENT_BG, "false"));
			if (FORMAT_SVG.equals(configuration.getProperty(PROPERTY_FORMAT))) {
				File file = new File(resolveVariables(configuration.getProperty(PROPERTY_FILENAME)));
				try {
					Writer writer = new FileWriter(file);
					writeSVG(viewComponent, size.width, size.height, dpi/75, transparentBG, writer);
					writer.close();
					}
				catch (IOException ioe) {
					showErrorMessage("Couldn't write image file.");
					}
				}
			else {	// png
				Image image = createComponentImage(viewComponent, size.width, size.height, dpi/75, transparentBG);
				if (TARGET_FILE.equals(configuration.getProperty(PROPERTY_TARGET))) {
					File file = new File(resolveVariables(configuration.getProperty(PROPERTY_FILENAME)));
					try {
						/* do something like this for setting the image to 300 dpi
						PNGEncodeParam png = PNGEncodeParam.getDefaultEncodeParam((BufferedImage)image);
						png.setPhysicalDimension(11812, 11812, 1);  // 11812 dots per meter = 300dpi
						JAI.create("filestore", (BufferedImage)image, "analemma.png", "PNG");   */
						javax.imageio.ImageIO.write((BufferedImage)image, "png", file);
						}
					catch (IOException ioe) {
						showErrorMessage("Couldn't write image file.");
						}
					}
				else {
					ImageClipboardHandler.copyImage(image);
					}
				}
			}
		catch (Exception ex) {
			showErrorMessage("Unexpected exception creating image.");
			ex.printStackTrace();
			}
		catch (OutOfMemoryError ex) {
			showErrorMessage("This exceeds your available memory. Try a smaller size.");
			}
		}

	private Image createComponentImage(JComponent viewComponent, int width, int height, float fontScaling, boolean transparentBG) {
		Image image = null;
		if (viewComponent instanceof JVisualization3D) {
			JVisualization3D v3D = (JVisualization3D)viewComponent;
			image = v3D.getViewImage(new Rectangle(0, 0, width, height), fontScaling, JVisualization3D.STEREO_MODE_NONE);
			}
		else if (viewComponent instanceof JVisualization2D) {
			JVisualization2D v2D = (JVisualization2D)viewComponent;
//			image = v2D.getParent().createImage(width, height);
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);	// we need ARGB for transparency
			Graphics imageG = image.getGraphics();
			v2D.paintHighResolution(imageG, new Rectangle(0, 0, width, height), fontScaling, transparentBG, false);
			}
		else if (viewComponent instanceof JStructureGrid) {
			height = ((JStructureGrid)viewComponent).getTotalHeight(width);
//			image = c.createImage(width, height);
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);	// we need ARGB for transparency
			Graphics imageG = image.getGraphics();
			if (!transparentBG) {
				imageG.setColor(Color.WHITE);
				imageG.fillRect(0, 0, width, height);
				}
			((JStructureGrid)viewComponent).paintHighResolution(imageG, new Dimension(width, height), fontScaling, transparentBG);
			}
		else if (viewComponent instanceof JCompoundTableForm) {
//			image = c.createImage(width, height);
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);	// we need ARGB for transparency
			Graphics imageG = image.getGraphics();
			if (!transparentBG) {
				imageG.setColor(Color.WHITE);
				imageG.fillRect(0, 0, width, height);
				}
			CompoundTableModel tableModel = mMainPane.getTableModel();
            FormModel model = new CompoundTableFormModel(tableModel, tableModel.getActiveRow());
            ((JCompoundTableForm)viewComponent).updatePrintColors(tableModel.getActiveRow());
			((JCompoundTableForm)viewComponent).print((Graphics2D)imageG, new Rectangle2D.Float(0, 0, width, height), fontScaling, model);
			}
		return image;
		}

	private int calculateImageHeight(JComponent viewComponent, int width, boolean keepAspectRatio) {
		if (viewComponent == null)
			return -1;
		if (viewComponent instanceof JStructureGrid)
			return ((JStructureGrid)viewComponent).getTotalHeight(width);
		if (keepAspectRatio)
			return width*viewComponent.getHeight()/viewComponent.getWidth();
		return -1;
		}

	private JComponent getViewComponent(CompoundTableView view) {
			return (view == null) ? null
				 : (view instanceof DEFormView) ? ((DEFormView)view).getCompoundTableForm()
				 : (view instanceof VisualizationPanel) ? ((VisualizationPanel)view).getVisualization()
				 : (JComponent)view;
			}

	private int calculateImageWidth(JComponent viewComponent, int height, boolean keepAspectRatio) {
		if (viewComponent == null)
			return -1;
		if (keepAspectRatio)
			return height*viewComponent.getWidth()/viewComponent.getHeight();
		return -1;
		}

	private boolean supportsSVG(JComponent viewComponent) {
		return (viewComponent instanceof JVisualization2D
			|| (viewComponent instanceof JCompoundTableForm)
			|| (viewComponent instanceof JStructureGrid));
		}

	/**
	 * This creates and writes an SVG to the given writer using the JFreeSVG library.
	 * (not used because the tested version 2.1 of JFreeSVG seems to create larger files
	 * and converts non-filed reactangles into 4 lines which do not touch at the corners)
	 * @param c
	 * @param width
	 * @param height
	 * @param fontScaling
	 * @param transparentBG
	 * @param writer
	 * @throws IOException
	 *
	private void writeSVG(JVisualization2D v2D, int width, int height, int fontScaling, boolean transparentBG, Writer writer) throws IOException {
		SVGGraphics2D g2d = new SVGGraphics2D(width, height);

		Map<SVGHints.Key,Object> hints = new HashMap<SVGHints.Key,Object>();
		hints.put(SVGHints.KEY_IMAGE_HANDLING, SVGHints.VALUE_IMAGE_HANDLING_EMBED);
		g2d.addRenderingHints(hints);

		v2D.paintHighResolution(g2d, new Rectangle(0, 0, width, height), fontScaling, transparentBG, false);
		String svgDoc = g2d.getSVGDocument();
		writer.write(svgDoc);
		}*/

	/*
	 * This creates and writes an SVG to the given writer using the JFreeSVG library.
	 */
	private void writeSVG(JComponent viewComponent, int width, int height, float fontScaling,
							boolean transparentBG, Writer writer) throws IOException {
		// example from: http://xmlgraphics.apache.org/batik/using/svg-generator.html

		// Get a DOMImplementation.
		DOMImplementation impl = GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document myFactory = impl.createDocument(svgNS, "svg", null);

		SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(myFactory);
		ctx.setEmbeddedFontsOn(true);
		ctx.setComment("Visualization generated by DataWarrior with Batik SVG Generator");
		SVGGraphics2D g2d = new SVGGraphics2D(ctx, false);

		if (viewComponent instanceof JStructureGrid)
			height = ((JStructureGrid)viewComponent).getTotalHeight(width);

		g2d.setSVGCanvasSize(new Dimension(width,height));

		if (viewComponent instanceof JVisualization2D) {
			((JVisualization2D)viewComponent).paintHighResolution(g2d, new Rectangle(0, 0, width, height), fontScaling, transparentBG, false);
			}
		else if (viewComponent instanceof JCompoundTableForm) {
			if (!transparentBG) {
				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, width, height);
				}
			CompoundTableModel tableModel = mMainPane.getTableModel();
            FormModel model = new CompoundTableFormModel(tableModel, tableModel.getActiveRow());
            ((JCompoundTableForm)viewComponent).updatePrintColors(tableModel.getActiveRow());
            ((JCompoundTableForm)viewComponent).print(g2d, new Rectangle2D.Float(0, 0, width, height), fontScaling, model);
			}
		else if (viewComponent instanceof JStructureGrid) {
			if (!transparentBG) {
				g2d.setColor(Color.WHITE);
				g2d.fillRect(0, 0, width, height);
				}
			((JStructureGrid)viewComponent).paintHighResolution(g2d, new Dimension(width, height), fontScaling, transparentBG);
			}

		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
//		Writer writer = new OutputStreamWriter(System.out, "UTF-8");	we have our own writer
		g2d.stream(writer, useCSS);
		}
	}
