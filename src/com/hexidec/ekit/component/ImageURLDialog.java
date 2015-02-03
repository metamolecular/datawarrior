/*
GNU Lesser General Public License

ImageURLDialog
Copyright (C) 2010 Howard Kistler (new version supercedes previous class)

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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.hexidec.util.Translatrix;

/** Class for providing a dialog that lets the user specify a remote image URL and its attributes
  */
public class ImageURLDialog extends JDialog
{
	private String imageUrl    = new String();
	private String imageAlt    = new String();
	private String imageWidth  = new String();
	private String imageHeight = new String();

	private JOptionPane jOptionPane;

	private final JTextField jtxtUrl    = new JTextField(3);
	private final JTextField jtxfAlt    = new JTextField(3);
	private final JTextField jtxfWidth  = new JTextField(3);
	private final JTextField jtxfHeight = new JTextField(3);

	public ImageURLDialog(Frame parent, String title, boolean bModal)
	{
		super(parent, title, bModal);

		final Object[] buttonLabels = { Translatrix.getTranslationString("DialogAccept"), Translatrix.getTranslationString("DialogCancel") };
		Object[] panelContents = {
			Translatrix.getTranslationString("ImageSrc"),    jtxtUrl,
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
						imageUrl    = jtxtUrl.getText();
						imageAlt    = jtxfAlt.getText();
						imageWidth  = jtxfWidth.getText();
						imageHeight = jtxfHeight.getText();
						setVisible(false);
					}
					else if(value.equals(buttonLabels[1]))
					{
						imageUrl    = "";
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

	public String getImageUrl()    { return imageUrl; }
	public String getImageAlt()    { return imageAlt; }
	public String getImageWidth()  { return imageWidth; }
	public String getImageHeight() { return imageHeight; }

	public String getDecisionValue()
	{
		return jOptionPane.getValue().toString();
	}
}
