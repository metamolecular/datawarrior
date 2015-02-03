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

package com.actelion.research.datawarrior.task.elib;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.util.DoubleFormat;

public abstract class FitnessPanel extends JPanel implements ActionListener,ChangeListener {
	private static final long serialVersionUID = 20140724L;

	protected static final String[] OPTION_TEXT = { "Structural (dis)similarity", "Molecular weight", "cLogP", "cLogS",
													"Polar surface area", "Molecular flexibility", "Rotatable bond count",
													"Stereo center count", "Ring count (<= 7 atoms)", "Aromatic ring count",
													"Basic nitrogen count", "Acidic oxygen count" };
	protected static final String[] OPTION_CODE = { "structure", "molweight", "cLogP", "cLogS", "tpsa", "flexibility",
													"rotatableBonds", "stereoCenters", "smallRings", "aromaticRings",
													"basicN", "acidicO" };
	protected static final int OPTION_STRUCTURE = 0;
	protected static final int OPTION_MOLWEIGHT = 1;
	protected static final int OPTION_CLOGP = 2;
	protected static final int OPTION_CLOGS = 3;
	protected static final int OPTION_PSA = 4;
	protected static final int OPTION_FLEXIBILITY = 5;
	protected static final int OPTION_ROTATABLEBONDS = 6;
	protected static final int OPTION_STEREOCENTERS = 7;
	protected static final int OPTION_SMALLRINGCOUNT = 8;
	protected static final int OPTION_AROMRINGCOUNT = 9;
	protected static final int OPTION_BASIC_NITROGENS = 10;
	protected static final int OPTION_ACIDIC_OXYGENS = 11;

	private static ImageIcon sIcon;

	protected UIDelegateELib mUIDelegate;
	protected int mType;
	protected JSlider mSlider;
	protected JPanel mSliderPanel;
	private JLabel mLabelWeight1,mLabelWeight2;

	protected static FitnessPanel createFitnessPanel(Frame owner, UIDelegateELib delegate, String configuration) {
		int index = (configuration == null) ? -1 : configuration.indexOf('\t');
		int type = (index == -1) ? -1 : AbstractTask.findListIndex(configuration.substring(0, index), OPTION_CODE, -1);
		return (type == -1) ? null : (type == OPTION_STRUCTURE) ?
				new StructureFitnessPanel(owner, delegate, configuration.substring(index+1))
			  : new PropertyFitnessPanel(type, configuration.substring(index+1));
		}

	protected abstract String getConfiguration();

	protected static FitnessPanel createFitnessPanel(Frame owner, UIDelegateELib delegate, int type) {
		switch (type) {
		case OPTION_STRUCTURE:
			return new StructureFitnessPanel(owner, delegate);
		case FitnessPanel.OPTION_MOLWEIGHT:
			return new PropertyFitnessPanel(type, "", "400");
		case FitnessPanel.OPTION_CLOGP:
			return new PropertyFitnessPanel(type, "", "4");
		case FitnessPanel.OPTION_CLOGS:
			return new PropertyFitnessPanel(type, "-4", "");
		case FitnessPanel.OPTION_PSA:
			return new PropertyFitnessPanel(type, "", "120");
		case FitnessPanel.OPTION_FLEXIBILITY:
			return new PropertyFitnessPanel(type, "0.3", "0.5");
		case FitnessPanel.OPTION_ROTATABLEBONDS:
			return new PropertyFitnessPanel(type, "", "4");
		case FitnessPanel.OPTION_STEREOCENTERS:
			return new PropertyFitnessPanel(type, "1", "3");
		case FitnessPanel.OPTION_SMALLRINGCOUNT:
			return new PropertyFitnessPanel(type, "2", "");
		case FitnessPanel.OPTION_AROMRINGCOUNT:
			return new PropertyFitnessPanel(type, "", "2");
		case FitnessPanel.OPTION_BASIC_NITROGENS:
			return new PropertyFitnessPanel(type, "1", "");
		case FitnessPanel.OPTION_ACIDIC_OXYGENS:
			return new PropertyFitnessPanel(type, "1", "");
			}
		return null;
		}

	protected FitnessPanel() {
		super();
		createWeightSlider();
		}

	protected JPanel createCloseButton() {
		if (sIcon == null)
			sIcon = new ImageIcon(this.getClass().getResource("/images/closeButton.png"));

		JButton b = createButton(sIcon, 14, 14, "close");
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(b, BorderLayout.NORTH);
		return p;
		}

	private JButton createButton(ImageIcon icon, int w, int h, String command) {
		JButton button = new JButton(icon);
		if ("quaqua".equals(System.getProperty("com.actelion.research.laf"))) {
			w += 4;
			h += 3;
			button.putClientProperty("Quaqua.Component.visualMargin", new Insets(1,1,1,1));
			button.putClientProperty("Quaqua.Button.style", "bevel");
			}
		button.setPreferredSize(new Dimension(w, h));
		if (command != null) {
			button.addActionListener(this);
			button.setActionCommand(command);
			}
		return button;
		}

	private void createWeightSlider() {
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(-100), new JLabel("0.1"));
		labels.put(new Integer(0), new JLabel("1.0"));
		labels.put(new Integer(100), new JLabel("10.0"));
		mSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
		mSlider.setMinorTickSpacing(10);
		mSlider.setMajorTickSpacing(100);
		mSlider.setLabelTable(labels);
		mSlider.setPaintLabels(true);
		mSlider.setPaintTicks(true);
		int height = mSlider.getPreferredSize().height;
		mSlider.setMinimumSize(new Dimension(116, height));
		mSlider.setPreferredSize(new Dimension(116, height));
		mSlider.addChangeListener(this);
		mSliderPanel = new JPanel();
		double[][] size = { {TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL},
							{TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL} };
		mSliderPanel.setLayout(new TableLayout(size));
		mLabelWeight1 = new JLabel("Weight:");
		mSliderPanel.add(mLabelWeight1, "1,1");
		mLabelWeight2 = new JLabel("1.0", JLabel.CENTER);
		mSliderPanel.add(mLabelWeight2, "1,2");
		mSliderPanel.add(mSlider, "2,0,2,3");
		}

	/**
	 * @return 0.25 ... 1.0 ... 4.0
	 */
	public float getWeight() {
		return (float)Math.pow(4.0f, (float)mSlider.getValue() / 100.0f);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("close")) {
			Container theParent = getParent();
			theParent.remove(this);
			theParent.getParent().validate();
			theParent.getParent().repaint();
			}
		}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == mSlider) {
			mLabelWeight2.setText(DoubleFormat.toString(getWeight(), 2));
			}
		}
	}
