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
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.Reactor;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.JStructureView;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableModel;

public class DEReactantDialog extends JDialog implements ActionListener,Runnable {
    private static final long serialVersionUID = 0x20060904;

    private Frame			mParentFrame;
	private DataWarrior mApplication;
	private DEFrame			mTargetFrame;
	private Reaction		mReaction;
    private JComboBox       mComboBox;
	private CompoundCollectionPane<StereoMolecule>[] mReactantPane;
	private JProgressDialog	mProgressDialog;

	@SuppressWarnings("unchecked")
	public DEReactantDialog(Frame owner, DataWarrior application, Reaction reaction) {
		super(owner, "Define All Reactants", true);

		mParentFrame = owner;
		mApplication = application;
		mReaction = reaction;

		JPanel p1 = new JPanel();
		p1.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		p1.setLayout(new GridLayout(reaction.getReactants(), 1, 0, 8));
		mReactantPane = new CompoundCollectionPane[reaction.getReactants()];
		for (int i=0; i<reaction.getReactants(); i++) {
			JPanel prw = new JPanel();
			prw.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
			prw.setLayout(new BorderLayout());
			JStructureView sview = new JStructureView(reaction.getReactant(i),
					DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
			sview.setClipboardHandler(new ClipboardHandler());
			prw.add(sview, BorderLayout.CENTER);
			JPanel prwb = new JPanel();
			prwb.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
			JButton bload = new JButton("Open File...");
			bload.setActionCommand("open"+i);
			bload.addActionListener(this);
			prwb.add(bload);
			prw.add(prwb, BorderLayout.SOUTH);

			JPanel pr = new JPanel();
			pr.setLayout(new BorderLayout());
			pr.add(prw, BorderLayout.WEST);
			mReactantPane[i] = new CompoundCollectionPane<StereoMolecule>(new DefaultCompoundCollectionModel.Molecule(), false);
			mReactantPane[i].setFileSupport(CompoundCollectionPane.FILE_SUPPORT_NONE);
			mReactantPane[i].setEditable(true);
			pr.add(mReactantPane[i], BorderLayout.CENTER);

			p1.add(pr);
			}
		getContentPane().add(p1, BorderLayout.CENTER);

        mComboBox = new JComboBox();
        mComboBox.addItem("one of");
        mComboBox.addItem("all");
        JPanel cbp = new JPanel();
        cbp.add(new JLabel("Generate"));
        cbp.add(mComboBox);
        cbp.add(new JLabel("multiple possible products"));
        cbp.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        
		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout());
		JPanel bp = new JPanel();
		bp.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
		bp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		bp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		bp.add(bok);
		p2.add(bp, BorderLayout.EAST);
        p2.add(cbp, BorderLayout.WEST);

		getContentPane().add(p2, BorderLayout.SOUTH);
//		getRootPane().setDefaultButton(bok);

		setSize(640, 150*reaction.getReactants()+40);
		setLocationRelativeTo(owner);
		setVisible(true);
		}

	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if (actionCommand.equals("OK")) {
			for (int i=0; i<mReaction.getReactants(); i++) {
				if (mReactantPane[i].getModel().getSize() == 0) {
					JOptionPane.showMessageDialog(mParentFrame, "Reactant(s) "+(i+1)+" wasn't defined yet.");
					return;
					}
				}

			setVisible(false);
    		dispose();

			createLibrary();
			}
		else if (actionCommand.equals("Cancel")) {
		    setVisible(false);
    	    dispose();
			}
		else if (actionCommand.startsWith("open")) {
			int reactant = actionCommand.charAt(4) - '0';

			ArrayList<StereoMolecule> compounds = new FileHelper(mParentFrame).readStructuresFromFile(true);

			if (compounds != null) {
			    SSSearcher searcher = new SSSearcher();
			    searcher.setFragment(mReaction.getReactant(reactant));
			    int matchErrors = 0;
			    for (int i=compounds.size()-1; i>=0; i--) {
			        searcher.setMolecule(compounds.get(i));
			        if (!searcher.isFragmentInMolecule()) {
			            compounds.remove(i);
			            matchErrors++;
			            }
			        }

			    if (matchErrors != 0) {
			        String message = (compounds.size() == 0) ?
			                "None of your file's compounds have generic reactant "+(char)('A'+reactant)+" as substructure.\n"
			              + "Therefore no compound could be added to the reactant list."
			              : ""+matchErrors+" of your file's compounds don't contain generic reactant "+(char)('A'+reactant)+" as substructure.\n"
                          + "Therefore these compounds were not added to the reactant list.";
			        JOptionPane.showMessageDialog(mParentFrame, message);
			        }

			    if (compounds.size() == 0)
			        compounds = null;
			    }

            if (compounds != null) {
			    if (mReactantPane[reactant].getModel().getSize() != 0 && 0 == JOptionPane.showOptionDialog(mParentFrame,
							"Do you want to add these compounds or to replace the current list?",
							"Add Or Replace Compounds", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, new String[] {"Add", "Replace"}, "Replace" ))
			    	mReactantPane[reactant].getModel().addCompoundList(compounds);
			    else
			    	mReactantPane[reactant].getModel().setCompoundList(compounds);
				}
			}
		}

	public void createLibrary() {
		mProgressDialog = new JProgressDialog(mParentFrame);

		Thread t = new Thread(this, "CombiChemLibraryCreation");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
		}

	public void run() {
		try {
			runLibraryCreation();
			}
		catch (OutOfMemoryError e) {
			final String message = "Out of memory. Launch DataWarrior with Java option -Xms???m or -Xmx???m.";
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			}

		mProgressDialog.close(mTargetFrame);
		}

	private void runLibraryCreation() {
		int rowCount = 1;
		for (int i=0; i<mReaction.getReactants(); i++)
			rowCount *= mReactantPane[i].getModel().getSize();

		mProgressDialog.startProgress("Creating Products...", 0, rowCount);

		Reactor theReactor = new Reactor(mReaction, true);
		int dimensions = mReaction.getReactants();
		for (int i=0; i<dimensions; i++)
			theReactor.setReactant(i, mReactantPane[i].getModel().getCompound(0));
		int[] index = new int[dimensions];
        ArrayList<Object[]> recordList = new ArrayList<Object[]>();
        boolean oneProductOnly = (mComboBox.getSelectedIndex() == 0);
		int row = 0;
		while (row < rowCount) {
            int productCount = oneProductOnly ? 1 : theReactor.getProducts(0);
            for (int p=0; p<productCount; p++) {
                Object[] record = new Object[2+3*mReaction.getReactants()];
    			StereoMolecule product = theReactor.getProduct(0, p);
    			if (product != null) {
    				Canonizer canonizer = new Canonizer(product);
                    record[0] = canonizer.getIDCode().getBytes();
                    record[1] = canonizer.getEncodedCoordinates().getBytes();
                    }
    			for (int i=0; i<dimensions; i++) {
    			    StereoMolecule reactant = mReactantPane[i].getModel().getCompound(index[i]);
    				String id = reactant.getName();
                    record[2+i] = (id!=null) ? id.getBytes() : (""+(index[i]+1)).getBytes();
                    Canonizer canonizer = new Canonizer(reactant);
                    record[2+dimensions+2*i] = canonizer.getIDCode().getBytes();
                    record[3+dimensions+2*i] = canonizer.getEncodedCoordinates().getBytes();
    				}
                recordList.add(record);
                }

			int currentDimension = dimensions-1;
			while (currentDimension >= 0
				&& index[currentDimension] == mReactantPane[currentDimension].getModel().getSize()-1) {
				index[currentDimension] = 0;
				theReactor.setReactant(currentDimension, mReactantPane[currentDimension].getModel().getCompound(0));
				currentDimension--;
				}
		    if (currentDimension < 0)
				break;

			index[currentDimension]++;
			theReactor.setReactant(currentDimension, mReactantPane[currentDimension].getModel().getCompound(index[currentDimension]));

			if (mProgressDialog.threadMustDie())
				break;

			mProgressDialog.updateProgress(row++);
			}

        if (!mProgressDialog.threadMustDie())
            populateTable(recordList);
        }
    
    private void populateTable(ArrayList<Object[]> recordList) {
    	mTargetFrame = mApplication.getEmptyFrame("Combinatorial Library");
        mProgressDialog.startProgress("Populating Table...", 0, recordList.size());

        CompoundTableModel tableModel = mTargetFrame.getTableModel();
        tableModel.initializeTable(recordList.size(), 3+3*mReaction.getReactants());
        tableModel.prepareStructureColumns(0, "Product", true, true);
        for (int i=0; i<mReaction.getReactants(); i++) {
        	tableModel.setColumnName("Reactant-ID "+(i+1), 3+i);
        	tableModel.prepareStructureColumns(3+mReaction.getReactants()+2*i, "Reactant "+(i+1), true, false);
            }

        int row = 0;
        for (Object[] record:recordList) {
        	tableModel.setTotalDataAt(record[0], row, 0);
        	tableModel.setTotalDataAt(record[1], row, 1);
            for (int column=2; column<record.length; column++)
            	tableModel.setTotalDataAt(record[column], row, column+1);

            mProgressDialog.updateProgress(row++);
            }

        tableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultRuntimeProperties, mProgressDialog);
        }
	}

