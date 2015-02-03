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

package com.actelion.research.datawarrior.action;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.RXNFileParser;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JDrawPanel;
import com.actelion.research.gui.clipboard.ClipboardHandler;

public class DEReactionDialog extends JDialog implements ActionListener,KeyListener {
    private static final long serialVersionUID = 0x20060904;

    private Frame				mParentFrame;
	private DataWarrior		mApplication;
	private JDrawPanel			mDrawPanel;

	public DEReactionDialog(Frame owner, DataWarrior application) {
		super(owner, "Define Generic Reaction", true);
		addKeyListener(this);

		mParentFrame = owner;
		mApplication = application;

		StereoMolecule mol = new StereoMolecule();
		mol.setFragment(true);
		mDrawPanel = new JDrawPanel(owner, mol, true);
		mDrawPanel.getDrawArea().setClipboardHandler(new ClipboardHandler());
		getContentPane().add(mDrawPanel, BorderLayout.CENTER);

		JPanel bp = new JPanel();
		bp.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
		bp.setLayout(new GridLayout(1, 8, 8, 0));
		JButton bopen = new JButton("Open...");
		bopen.addActionListener(this);
		bp.add(bopen);
		JButton bsave = new JButton("Save...");
		bsave.addActionListener(this);
		bp.add(bsave);
		bp.add(new JLabel());
		bp.add(new JLabel());
		bp.add(new JLabel());
		bp.add(new JLabel());
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		bp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		bp.add(bok);

		getContentPane().add(bp, BorderLayout.SOUTH);
//		getRootPane().setDefaultButton(bok);

		setSize(720, 434);
		setLocationRelativeTo(owner);
		setVisible(true);
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Open...")) {
			File rxnFile = FileHelper.getFile(mParentFrame, "Please select a reaction file", FileHelper.cFileTypeRXN);
			if (rxnFile == null)
				return;

			try {
				Reaction reaction = new RXNFileParser().getReaction(rxnFile);

					// allow for query features
				for (int i=0; i<reaction.getMolecules(); i++)
					reaction.getMolecule(i).setFragment(true);

				mDrawPanel.getDrawArea().setReaction(reaction);
				}
			catch (Exception ex) {}
			return;
			}
		else if (e.getActionCommand().equals("Save...")) {
			Reaction rxn = mDrawPanel.getDrawArea().getReaction();
			if (isReactionValid(rxn))
				new FileHelper(mParentFrame).saveRXNFile(rxn);
			}
		else if (e.getActionCommand().equals("OK")) {
			Reaction rxn = mDrawPanel.getDrawArea().getReaction();
			if (isReactionValid(rxn)) {
				new DEReactantDialog(mParentFrame, mApplication, rxn);
			    setVisible(false);
   			    dispose();
				}
			}
		else if (e.getActionCommand().equals("Cancel")) {
		    setVisible(false);
    	    dispose();
			}
		}

	private boolean isReactionValid(Reaction rxn) {
		try {
			if (rxn.getReactants() < 1)
				throw new Exception("For combinatorial enumeration you need at least one reactant.");
			if (rxn.getReactants() > 4)
				throw new Exception("Combinatorial enumeration is limited to a maximum of 4 reactants.");
			if (rxn.getProducts() == 0)
				throw new Exception("No product defined.");
			rxn.validateMapping();
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, e);
			return false;
			}
		return true;
		}

	public void keyPressed(KeyEvent e) {
        mDrawPanel.getDrawArea().keyPressed(e);
        }

	public void keyReleased(KeyEvent e) {
        mDrawPanel.getDrawArea().keyReleased(e);
		}

	public void keyTyped(KeyEvent e) {}
	}