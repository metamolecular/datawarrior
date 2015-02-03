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
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Mutator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.gui.JProgressPanel;
import com.actelion.research.gui.JStructureView;
import com.actelion.research.gui.dock.ShadowBorder;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistHandler;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.DoubleFormat;

public class DETaskCreateEvolutionaryLibrary extends AbstractTask implements ActionListener,Runnable,TaskConstantsELib {
	public static final String TASK_NAME = "Create Evolutionary Library";

	private static Properties sRecentConfiguration;

	private static final int VIEW_BACKGROUND = 0xFF081068;
	private static final int AUTOMATIC_HISTORIC_GENERATIONS = 32;

	private Frame				mParentFrame;
	private DataWarrior			mApplication;
	private CompoundTableModel	mSourceTableModel;
	private JDialog				mControllingDialog;
	private DEFrame				mTargetFrame;
	private JStructureView[]	mCompoundView;
	private JLabel[]			mFitnessLabel;
	private JLabel				mLabelGeneration;
	private JProgressPanel		mProgressPanel;
	private FitnessEvolutionPanel mFitnessEvolutionPanel;
	private boolean				mIsInteractive;
	private volatile boolean	mKeepData,mStopProcessing;
	private float				mBestFitness;
	private int					mCurrentResultID;
	private TreeSet<EvolutionResult> mParentGeneration;

	public DETaskCreateEvolutionaryLibrary(DEFrame owner, DataWarrior application, boolean isInteractive) {
		super(owner, false);

		mParentFrame = owner;
		mSourceTableModel = owner.getTableModel();
		mApplication = application;
		mIsInteractive = isInteractive;
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
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#EvolutionaryLibraries";
		}

	@Override
	public TaskUIDelegate createUIDelegate() {
		return new UIDelegateELib(mParentFrame, mSourceTableModel);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	private JDialog createControllingDialog() {
		JPanel p2 = new JPanel();
		double[][] size2 = { {8, 160, 8, 160, 8, 160, 8, 160, 8},
							 {8, TableLayout.PREFERRED, 4, 120, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 4, 120, TableLayout.PREFERRED, 8} };
		p2.setLayout(new TableLayout(size2));
		p2.add(new JLabel("Parent Molecule"), "1,1");
		p2.add(new JLabel("Best Molecule"), "7,1");
		p2.add(new JLabel("Best molecules of current generation"), "1,6,7,6");
		mCompoundView = new JStructureView[6];
		mFitnessLabel = new JLabel[6];
		for (int i=0; i<6; i++) {
			mCompoundView[i] = new JStructureView(DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
			mCompoundView[i].setBorder(new ShadowBorder());
			mCompoundView[i].setOpaque(true);
			mFitnessLabel[i] = new JLabel("Fitness:");
			if (i == 0) {
				p2.add(mCompoundView[i], "1,3");
				p2.add(mFitnessLabel[i], "1,4");
				}
			else if (i == 1) {
				p2.add(mCompoundView[i], "7,3");
				p2.add(mFitnessLabel[i], "7,4");
				}
			else {
				p2.add(mCompoundView[i], ""+(i*2-3)+",8");
				p2.add(mFitnessLabel[i], ""+(i*2-3)+",9");
				}
			}
		mLabelGeneration = new JLabel();
		p2.add(mLabelGeneration, "3,1,5,1");

		mFitnessEvolutionPanel = new FitnessEvolutionPanel();
		p2.add(mFitnessEvolutionPanel, "3,3,5,3");

		JPanel bp = new JPanel();
		bp.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
		bp.setLayout(new BorderLayout());
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener(this);
		ibp.add(buttonCancel);
		JButton buttonStop = new JButton("Stop");
		buttonStop.addActionListener(this);
		ibp.add(buttonStop);
		bp.add(ibp, BorderLayout.EAST);

		mProgressPanel = new JProgressPanel(false);
		bp.add(mProgressPanel, BorderLayout.WEST);

		JDialog dialog = new JDialog(mParentFrame, TASK_NAME, true);
		dialog.getContentPane().add(p2, BorderLayout.CENTER);
		dialog.getContentPane().add(bp, BorderLayout.SOUTH);
		dialog.getRootPane().setDefaultButton(buttonStop);

		dialog.pack();
		dialog.setLocationRelativeTo(mParentFrame);
		return dialog;
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Cancel")) {
			mStopProcessing = true;
			mKeepData = false;
			}
		else if (e.getActionCommand().equals("Stop")) {
			mStopProcessing = true;
			mKeepData = true;
			}
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String startSet = configuration.getProperty(PROPERTY_START_SET, "");
		if (startSet.length() == 0) {
			showErrorMessage("No first generation molecules defined.");
			return false;
			}
		for (String idcode:startSet.split("\\t")) {
			try {
				new IDCodeParser(true).getCompactMolecule(idcode).validate();
				}
			catch (Exception e) {
				showErrorMessage("Some of your first generation compounds are invalid:\n"+e.toString());
				return false;
				}
			}

		try {
			int survivalCount = Integer.parseInt(configuration.getProperty(PROPERTY_SURVIVAL_COUNT, DEFAULT_SURVIVALS));
			int generationSize = Integer.parseInt(configuration.getProperty(PROPERTY_GENERATION_SIZE, DEFAULT_COMPOUNDS));
			String generations = configuration.getProperty(PROPERTY_GENERATION_COUNT, DEFAULT_GENERATIONS);
			if (!generations.equals(GENERATIONS_AUTOMATIC) && !generations.equals(GENERATIONS_UNLIMITED))
				Integer.parseInt(configuration.getProperty(PROPERTY_GENERATION_COUNT, DEFAULT_GENERATIONS));

			if (survivalCount >= generationSize/2) {
				showErrorMessage("The number of surviving compounds from a generation\nshould be less than half of the generation size.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Survival count, generation count or generation size are not numeric.");
			return false;
			}

		int fitnessOptionCount = Integer.parseInt(configuration.getProperty(PROPERTY_FITNESS_PARAM_COUNT, "0"));
		if (fitnessOptionCount == 0) {
			showErrorMessage("No fitness criteria defined.");
			return false;
			}
		for (int i=0; i<fitnessOptionCount; i++) {
			String errorMsg = FitnessOption.getParamError(configuration.getProperty(PROPERTY_FITNESS_PARAM_CONFIG+i, ""));
			if (errorMsg != null) {
				showErrorMessage(errorMsg);
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		mControllingDialog = createControllingDialog();

		Thread t = new Thread(this, "DETaskEvolutionaryLibrary");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

		mControllingDialog.setVisible(true);
		}

	public void run() {
		try {
			runEvolution();
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		catch (OutOfMemoryError e) {
			final String message = "Out of memory. Launch DataWarrior with Java option -Xms???m or -Xmx???m.";
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			}
		}

	private void runEvolution() {
		Properties configuration = getTaskConfiguration();

		int survivalCount = Integer.parseInt(configuration.getProperty(PROPERTY_SURVIVAL_COUNT, DEFAULT_SURVIVALS));
		int generationSize = Integer.parseInt(configuration.getProperty(PROPERTY_GENERATION_SIZE, DEFAULT_COMPOUNDS));
		String generationsString = configuration.getProperty(PROPERTY_GENERATION_COUNT, DEFAULT_GENERATIONS);
		int generationCount = generationsString.equals(GENERATIONS_AUTOMATIC) ? Integer.MAX_VALUE-1
							: generationsString.equals(GENERATIONS_UNLIMITED) ? Integer.MAX_VALUE : Integer.parseInt(generationsString);
		if (!mIsInteractive
		 && generationCount == Integer.MAX_VALUE)
			generationCount -= -1;	// don't allow unlimited processing, if is running a macro

		int fitnessOptionCount = Integer.parseInt(configuration.getProperty(PROPERTY_FITNESS_PARAM_COUNT));
		final FitnessOption[] fitnessOption = new FitnessOption[fitnessOptionCount];
		for (int i=0; i<fitnessOptionCount; i++)
			fitnessOption[i] = FitnessOption.createFitnessOption(configuration.getProperty(PROPERTY_FITNESS_PARAM_CONFIG+i), this);

		mCurrentResultID = 0;

		// Compile first parent generation including fitness calculation
		// if we don't use the parent generation from the previous run
		// or if the fitness criteria have changed
//		if (!mParentFitnessValid
//		 || !BEST_OF_PREVIOUS_RUN.equals(mComboBoxStartCompounds.getItemAt(mComboBoxStartCompounds.getItemCount()-1))) {
			mParentGeneration = new TreeSet<EvolutionResult>();
			for (String idcode:configuration.getProperty(PROPERTY_START_SET, "").split("\\t"))
				mParentGeneration.add(new EvolutionResult(new IDCodeParser(true).getCompactMolecule(idcode), null, null, -1, fitnessOption, ++mCurrentResultID));
//			}

//		TreeSet<String> moleculeHistory = new TreeSet<String>();
		TreeMap<String,String> moleculeHistory = new TreeMap<String,String>();
		int offspringCompounds = generationSize / (2*survivalCount);
		mProgressPanel.startProgress("Evolving...", 0, (generationCount>=Integer.MAX_VALUE-1) ? 0 : generationCount*survivalCount*2);

		int kind = findListIndex(configuration.getProperty(PROPERTY_COMPOUND_KIND), COMPOUND_KIND_CODE, 0);
		Mutator mutator = new Mutator("/resources/"+COMPOUND_KIND_FILE[kind]);

		// Create a new result set that contains the start generation.
		final TreeSet<EvolutionResult> resultSet = new TreeSet<EvolutionResult>();
		for (EvolutionResult parent:mParentGeneration)
			resultSet.add(parent);

		mBestFitness = 0.0f;
		mKeepData = true;	// default if is not cancelled
		
								// In automatic mode stop if no improvement over AUTOMATIC_HISTORIC_GENERATIONS generations
		float[] bestHistoricFitness = (generationCount == Integer.MAX_VALUE-1) ? new float[AUTOMATIC_HISTORIC_GENERATIONS] : null;

		for (int generation=0; (generation<generationCount) && !mStopProcessing; generation++) {
			mLabelGeneration.setText("Generation: "+(generation+1));

			// use all survived molecules from recent generation as parent structures
			TreeSet<EvolutionResult> currentGeneration = new TreeSet<EvolutionResult>();
			int parentIndex = 0;
			for (EvolutionResult parent:mParentGeneration) {
				if (mStopProcessing)
					break;

				mProgressPanel.updateProgress(survivalCount*generation+parentIndex++);
				parent.ensureCoordinates();
				mCompoundView[0].structureChanged(parent.getMolecule());
				mFitnessLabel[0].setText("Fitness: "+(float)((int)(100000*parent.getOverallFitness()))/100000);

				if (parent.getMutationList() == null)
					parent.setMutationList(mutator.generateMutationList(parent.getMolecule(), Mutator.MUTATION_ANY, false));
				int mutationCount = Math.min(offspringCompounds, parent.getMutationList().size());
				for (int i=0; (i<mutationCount) && !mStopProcessing; i++) {
					StereoMolecule mol = new StereoMolecule(parent.getMolecule());
					mutator.mutate(mol, parent.getMutationList());
					processCandidate(mol, currentGeneration, moleculeHistory, generation, survivalCount, parent, fitnessOption, mutator);
					}
				}

			// Generate fitness limits for for up to survivalCount structures
			// from older generations.
			float[] fitnessLimit = new float[survivalCount];
			parentIndex = 0;
			for (EvolutionResult parent:mParentGeneration) {
				if (parentIndex == survivalCount)
					break;
				fitnessLimit[parentIndex++] = parent.getOverallFitness();
				}

			// now also process previous best ranking molecules as parent structures
			int resultIndex = 0;
			for (EvolutionResult parent:resultSet) {
				if (mStopProcessing
				 || resultIndex == survivalCount)
					break;

				if (parent.getMutationList().size() == 0)
					continue;
				if (parent.getOverallFitness() < fitnessLimit[resultIndex++])
					continue;

				mProgressPanel.updateProgress(survivalCount*generation*2+survivalCount+resultIndex);
				parent.ensureCoordinates();
				mCompoundView[0].structureChanged(parent.getMolecule());
				mFitnessLabel[0].setText("Fitness: "+(float)((int)(100000*parent.getOverallFitness()))/100000);

				int mutationCount = Math.min(offspringCompounds, parent.getMutationList().size());
				for (int i=0; (i<mutationCount) && !mStopProcessing; i++) {
					StereoMolecule mol = new StereoMolecule(parent.getMolecule());
					mutator.mutate(mol, parent.getMutationList());
					processCandidate(mol, currentGeneration, moleculeHistory, generation, survivalCount, parent, fitnessOption, mutator);
					}
				}

			if (currentGeneration.size() == 0) {
				if (mIsInteractive) {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								JOptionPane.showMessageDialog(mParentFrame, "No valid molecules could be made in most recent generation");
								}
							} );
						} catch (Exception e) {}
					}
				break;
				}

			int resultNo = 0;
			mParentGeneration.clear();
			for (EvolutionResult r:currentGeneration) {
				r.setChildIndex(resultNo++);
				resultSet.add(r);
				mParentGeneration.add(r);
				}

			mFitnessEvolutionPanel.updateEvolution(generation+1, resultSet);

			if (generation > AUTOMATIC_HISTORIC_GENERATIONS
			 && bestHistoricFitness != null) {
				int index = generation % AUTOMATIC_HISTORIC_GENERATIONS;
				if (mBestFitness <= bestHistoricFitness[index])
					break;

				bestHistoricFitness[index] = mBestFitness;
				}
			}

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					mControllingDialog.setVisible(false);
					mControllingDialog.dispose();

					if (!mIsInteractive || mKeepData) {
						mTargetFrame = mApplication.getEmptyFrame("Evolutionary Library");
						createDocument(resultSet, fitnessOption);
						}
					}
				} );
			} catch (Exception e) {}
		}

	private void processCandidate(StereoMolecule candidate,
								  TreeSet<EvolutionResult> currentGeneration,
//								  TreeSet<String> moleculeHistory,
								  TreeMap<String,String> moleculeHistory,
								  int generation,
								  int survivalCount,
								  EvolutionResult parent,
								  FitnessOption[] fitnessOption,
								  Mutator mutator) {
		String idcode = new Canonizer(candidate).getIDCode();

//if (candidate.getFragments().length != 1)
// System.out.println("Multiple fragments! i1:"+parent.mIDCode+" i2:"+idcode+" m:"+mutator.getMostRecentMutation().toString());

//		if (moleculeHistory.contains(idcode)) {
		if (moleculeHistory.keySet().contains(idcode)) {
//		  System.out.println("Redundant molecule! g2:"+(generation+1)+" i2:"+idcode+" m2:"+mutator.getMostRecentMutation().toString());
//		  System.out.println("	 from molecule: "+moleculeHistory.get(idcode)+"g2:"+(generation+1)+" i2:"+idcode+" m2:"+mutator.getMostRecentMutation().toString());
			return;
			}
//		moleculeHistory.add(idcode);
		moleculeHistory.put(idcode,"g1:"+(generation+1)+" i1:"+idcode+" m1:"+mutator.getMostRecentMutation().toString());

		EvolutionResult result = new EvolutionResult(candidate, idcode, parent, generation, fitnessOption, ++mCurrentResultID);
		currentGeneration.add(result);
		if (currentGeneration.size() > survivalCount)
			currentGeneration.remove(currentGeneration.last());

		Iterator<EvolutionResult> iterator = currentGeneration.iterator();
		boolean structureInserted = false;
		for (int index=2; index<6; index++) {
			if (!iterator.hasNext()) {
				mCompoundView[index].structureChanged(null);
				mCompoundView[index].setBackground(new Color(Color.HSBtoRGB(0.0f, 0.0f, 0.9f)));
				mFitnessLabel[index].setText("Fitness:");
				}
			else {
				EvolutionResult r = iterator.next();
				if (r.getIDCode().equals(idcode)) {
					structureInserted = true;
					r.ensureCoordinates();
					}
				if (structureInserted) {
					mCompoundView[index].structureChanged(r.getMolecule());
					mCompoundView[index].setBackground(new Color(Color.HSBtoRGB((float)(r.getOverallFitness()/3.0), 0.8f, 0.9f)));
					mFitnessLabel[index].setText("Fitness: "+(float)((int)(100000*r.getOverallFitness()))/100000);
					}
				}
			}

		if (mBestFitness < result.getOverallFitness()) {
			mBestFitness = result.getOverallFitness();
			mCompoundView[1].structureChanged(result.getMolecule());
			mCompoundView[1].setBackground(new Color(Color.HSBtoRGB((float)(mBestFitness/3.0), 0.8f, 0.9f)));
			mFitnessLabel[1].setText("Fitness: "+(float)((int)(100000*mBestFitness))/100000);
			}
		}

	private void createDocument(TreeSet<EvolutionResult> resultSet, FitnessOption[] fitnessOption) {
		EvolutionResult[] result = resultSet.toArray(new EvolutionResult[0]);
		Arrays.sort(result, new Comparator<EvolutionResult>() {
			@Override
			public int compare(EvolutionResult r1, EvolutionResult r2) {
				return (r1.getID() == r2.getID()) ? 0 : (r1.getID() < r2.getID()) ? -1 : 1;
				}
			});

		ArrayList<String> columnNameList = new ArrayList<String>();
		columnNameList.add("ID");
		columnNameList.add("Parent ID");
		columnNameList.add("Generation");
		columnNameList.add("Parent Generation");
		columnNameList.add("Child No");
		columnNameList.add("Fitness");
		for (FitnessOption fo:fitnessOption) {
			columnNameList.add(fo.getName());
			if (fo instanceof PropertyFitnessOption)
				columnNameList.add(fo.getName()+" Fitness");
			}

		CompoundTableModel tableModel = mTargetFrame.getTableModel();
		tableModel.initializeTable(result.length, 2+columnNameList.size());

		tableModel.prepareStructureColumns(0, "Structure", false, true);
		int column = 2;
		for (String columnName:columnNameList)
			tableModel.setColumnName(columnName, column++);

		tableModel.setColumnProperty(3, CompoundTableModel.cColumnPropertyReferencedColumn, "ID");
		tableModel.setColumnProperty(3, CompoundTableModel.cColumnPropertyReferenceType,
										CompoundTableModel.cColumnPropertyReferenceTypeTopDown);
		tableModel.setColumnProperty(5, CompoundTableModel.cColumnPropertyReferencedColumn, "Generation");
		tableModel.setColumnProperty(5, CompoundTableModel.cColumnPropertyReferenceType,
										CompoundTableModel.cColumnPropertyReferenceTypeTopDown);

		int row = 0;
		float maxFitness = 0;
		EvolutionResult bestResult = null;
		for (EvolutionResult r:result) {
			if (maxFitness < r.getOverallFitness()) {
				maxFitness = r.getOverallFitness();
				bestResult = r;
				}

			tableModel.setTotalValueAt(r.getIDCode(), row, 0);
			column = 2;
			tableModel.setTotalValueAt(""+r.getID(), row, column++);
			tableModel.setTotalValueAt(""+r.getParentID(), row, column++);
			tableModel.setTotalValueAt(""+(1+r.getGeneration()), row, column++);
			tableModel.setTotalValueAt(""+(1+r.getParentGeneration()), row, column++);
			tableModel.setTotalValueAt(""+(1+r.getChildIndex()), row, column++);
			tableModel.setTotalValueAt(DoubleFormat.toString(r.getOverallFitness(), 5), row, column++);
			int foi = 0;
			for (FitnessOption fo:fitnessOption) {
				tableModel.setTotalValueAt(DoubleFormat.toString(r.getProperty(foi), 5), row, column++);
				if (fo instanceof PropertyFitnessOption)
					tableModel.setTotalValueAt(DoubleFormat.toString(r.getFitness(foi), 5), row, column++);
				foi++;
				}
			row++;
			}

		tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, mProgressPanel);

		setRuntimeSettings(createHitlist(result, bestResult));
		}

	private int createHitlist(EvolutionResult[] result, EvolutionResult bestResult) {
		CompoundTableModel tableModel = mTargetFrame.getTableModel();
		CompoundTableHitlistHandler hitlistHandler = tableModel.getHitlistHandler();
		String name = hitlistHandler.createHitlist("Direct Route", -1, CompoundTableHitlistHandler.EMPTY_LIST, -1, null);
		int hitlistFlagNo = hitlistHandler.getHitlistFlagNo(name);
		int wantedID = bestResult.getID();
		for (int row=result.length-1; row>=0; row--) {
			if (wantedID == result[row].getID()) {
				wantedID = result[row].getParentID();
				hitlistHandler.addRecordSilent(tableModel.getTotalRecord(row), hitlistFlagNo);
				}
			}
		return hitlistHandler.getHitlistIndex(name);
		}

	private void setRuntimeSettings(final int hitlist) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mTargetFrame.getMainFrame().getMainPane().removeAllViews();
				mTargetFrame.getMainFrame().getMainPane().addTableView("Table", null);
				mTargetFrame.getMainFrame().getMainPane().addStructureView("Structure", null, 0);

				mTargetFrame.getMainFrame().getPruningPanel().removeAllFilters();	// this is the automatically added list filter
				mTargetFrame.getMainFrame().getPruningPanel().addDefaultFilters();

				VisualizationPanel2D vpanel1 = mTargetFrame.getMainFrame().getMainPane().add2DView("Fitness Evolution", null);
				vpanel1.setAxisColumnName(0, "Generation");
				vpanel1.setAxisColumnName(1, "Fitness");

				JVisualization2D visualization = (JVisualization2D)vpanel1.getVisualization();
				visualization.setMarkerSize(0.6f, false);
				visualization.setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
				visualization.setConnectionColumns(mTargetFrame.getTableModel().findColumn("Parent ID"), -1);
				visualization.getMarkerColor().setColor(CompoundTableHitlistHandler.getColumnFromHitlist(hitlist));
				visualization.setFocusHitlist(hitlist);
				visualization.setViewBackground(new Color(VIEW_BACKGROUND));
				}
			} );
		}
	}
