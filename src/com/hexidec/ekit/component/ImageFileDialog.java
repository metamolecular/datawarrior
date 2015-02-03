/*
GNU Lesser General Public License

ImageFileDialog
Copyright (C) 2010 Howard Kistler

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.hexidec.ekit.component;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.hexidec.ekit.component.JButtonNoFocus;
import com.hexidec.ekit.component.ImageFileChooser;

import com.hexidec.util.Translatrix;

/** Class for providing a dialog that lets the user load a local image and specify its attributes
  */
public class ImageFileDialog extends JDialog implements ActionListener
{
	private String imageDir    = new String();
	private String[] imageExts = new String[0];
	private String imageDesc   = new String();
	private File   imageFile   = (File)null;
	private String imageSrc    = new String();
	private String imageAlt    = new String();
	private String imageWidth  = new String();
	private String imageHeight = new String();

	private JOptionPane jOptionPane;

	private final JLabel     jlblSrc    = new JLabel("----");
	private final JTextField jtxfAlt    = new JTextField(3);
	private final JTextField jtxfWidth  = new JTextField(3);
	private final JTextField jtxfHeight = new JTextField(3);
	private final JButtonNoFocus jbtnBrowse = new JButtonNoFocus("Browse");

	public ImageFileDialog(Frame parent, String imgDir, String[] imgExts, String imgDesc, String imgSrc, String title, boolean bModal)
	{
		super(parent, title, bModal);

		this.imageDir  = imgDir;
		this.imageExts = imgExts;
		this.imageDesc = imgDesc;
		this.imageSrc  = imgSrc;

		jbtnBrowse.getModel().setActionCommand("browse"); jbtnBrowse.addActionListener(this);
		final Object[] buttonLabels = { Translatrix.getTranslationString("DialogAccept"), Translatrix.getTranslationString("DialogCancel") };
		Object[] panelContents = {
			Translatrix.getTranslationString("ImageSrc"),    jlblSrc,    jbtnBrowse,
			Translatrix.getTranslationString("ImageAlt"),    jtxfAlt,
			Translatrix.getTranslationString("ImageWidth"),  jtxfWidth,
			Translatrix.getTranslationString("ImageHeight"), jtxfHeight
		};
		jOptionPane = new JOptionPane(panelContents, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, buttonLabels, buttonLabels[0]);

		setContentPane(jOptionPane);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we)
			{
				jOptionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});

		jOptionPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e)
			{
				String prop = e.getPropertyName();
				if(isVisible() 
					&& (e.getSource() == jOptionPane)
					&& (prop.equals(JOptionPane.VALUE_PROPERTY) || prop.equals(JOptionPane.INPUT_VALUE_PROPERTY)))
				{
					Object value = jOptionPane.getValue();
					if(value == JOptionPane.UNINITIALIZED_VALUE)
					{
						return;
					}
					if(value.equals(buttonLabels[0]))
					{
						imageSrc    = jlblSrc.getText();
						imageAlt    = jtxfAlt.getText();
						imageWidth  = jtxfWidth.getText();
						imageHeight = jtxfHeight.getText();
						setVisible(false);
					}
					else if(value.equals(buttonLabels[1]))
					{
						imageSrc    = "";
						imageAlt    = "";
						imageWidth  = "";
						imageHeight = "";
						setVisible(false);
					}
					else
					{
						jOptionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
					}
				}
			}
		});
		this.pack();
	}

	public void actionPerformed(ActionEvent ae)
	{
		if(ae.getActionCommand().equals("browse"))
		{
			imageFile = browseForImage();
			if(imageFile != null)
			{
				jlblSrc.setText(imageFile.getName());
			}
		}
	}

	public File   getImageFile()   { return imageFile; }
	public String getImageSrc()    { return imageSrc; }
	public String getImageAlt()    { return imageAlt; }
	public String getImageWidth()  { return imageWidth; }
	public String getImageHeight() { return imageHeight; }

	public File browseForImage()
	{
		ImageFileChooser jImageDialog = new ImageFileChooser(imageDir);
		jImageDialog.setDialogType(JFileChooser.CUSTOM_DIALOG);
		jImageDialog.setFileFilter(new MutableFilter(imageExts, imageDesc));
		jImageDialog.setDialogTitle(Translatrix.getTranslationString("ImageDialogTitle"));
		int optionSelected = JFileChooser.CANCEL_OPTION;
		optionSelected = jImageDialog.showDialog(this, Translatrix.getTranslationString("Insert"));
		if(optionSelected == JFileChooser.APPROVE_OPTION)
		{
			return jImageDialog.getSelectedFile();
		}
		return (File)null;
	}

	public String getDecisionValue()
	{
		return jOptionPane.getValue().toString();
	}
}
