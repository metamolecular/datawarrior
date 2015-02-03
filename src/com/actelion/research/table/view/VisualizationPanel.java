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

package com.actelion.research.table.view;

import info.clearthought.layout.TableLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.gui.ComboBoxColorItem;
import com.actelion.research.gui.JComboBoxWithColor;
import com.actelion.research.gui.JPruningBar;
import com.actelion.research.gui.PruningBarEvent;
import com.actelion.research.gui.PruningBarListener;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.NumericalCompoundTableColumn;

public abstract class VisualizationPanel extends JPanel
				implements ComponentListener,ItemListener,CompoundTableView,FocusableView,MouseWheelListener,Printable,PruningBarListener {
	private static final long serialVersionUID = 0x20100602;

	public static final String UNASSIGNED_TEXT = "<unassigned>";

	protected JVisualization	mVisualization;
	protected int				mDimensions;
	private Frame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private JPruningBar[]		mPruningBar;
	private JComboBox[]			mComboBoxColumn;
	private JWindow				mControls,mMessagePopup,mVisiblePopup;
	private int					mFirstChoiceColumns,mSecondChoiceColumns;
	private int[]				mQualifyingColumn;
	private float[]				mMinValue,mMaxValue;
	private boolean				mDisableEvents,mIsProgrammaticChange;
	private boolean[]			mIsLogarithmic;
	private VisualizationPanel	mMasterPanel;
	private Point				mPopupLocation;
	private ArrayList<VisualizationPanel> mSynchronizationChildList;
	private ArrayList<VisualizationListener> mListenerList;

	public VisualizationPanel(Frame parent, CompoundTableModel tableModel) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mMasterPanel = this;
		addMouseWheelListener(this);
		}

	@Override
	public void cleanup() {
		removeMouseWheelListener(this);

		// make all controlled views independent
		ArrayList<VisualizationPanel> dependentChildList = new ArrayList<VisualizationPanel>();
		for (VisualizationPanel vp:mSynchronizationChildList)
			if (vp != this)
				dependentChildList.add(vp);
		for (VisualizationPanel vp:dependentChildList)
			vp.setSynchronizationMaster(null);

		for (int axis=0; axis<mDimensions; axis++)
			mPruningBar[axis].removePruningBarListener(this);

		mVisualization.cleanup();
		}

	public void addVisualizationListener(VisualizationListener l) {
		mListenerList.add(l);
		}

	public void removeVisualizationListener(VisualizationListener l) {
		mListenerList.remove(l);
		}

	/**
	 * Shows or hides a popup at the top left corner containing
	 * comboboxes for column selection and pruning bars.
	 */
	public void showControls() {
		if (mVisiblePopup != null) {
			hideControls();
			return;
			}

		/* using JWindow as popup has the problem that it doesn't move with the parent frame and */
		mVisiblePopup = (mMasterPanel == this) ? mControls : mMessagePopup;
		mVisiblePopup.pack();

		mPopupLocation = getLocationOnScreen();
		mPopupLocation.translate(getWidth()-64, 0);
		mVisiblePopup.setLocation(mPopupLocation);
		mVisiblePopup.setVisible(true);
		mVisiblePopup.toFront();

		mParentFrame.addComponentListener(this);
		addComponentListener(this);
		}

	public void hideControls() {
		if (mVisiblePopup != null) {
			mVisiblePopup.setVisible(false);
			mVisiblePopup = null;
			mParentFrame.removeComponentListener(this);
			removeComponentListener(this);
			return;
			}
		}
	
	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentMoved(ComponentEvent e) {
		mPopupLocation = getLocationOnScreen();
		mPopupLocation.translate(getWidth()-64, 0);
		mVisiblePopup.setLocation(mPopupLocation);
		}

	@Override
	public void componentResized(ComponentEvent e) {
		mPopupLocation = getLocationOnScreen();
		mPopupLocation.translate(getWidth()-64, 0);
		mVisiblePopup.setLocation(mPopupLocation);
		}

	@Override
	public void componentShown(ComponentEvent e) {}

	protected void initialize() {
		setLayout(new BorderLayout());
		add(mVisualization, BorderLayout.CENTER);

		mPruningBar = new JPruningBar[mDimensions];
		mComboBoxColumn = new JComboBox[mDimensions];
		mMinValue = new float[mDimensions];
		mMaxValue = new float[mDimensions];
		mIsLogarithmic = new boolean[mDimensions];
		for (int axis=0; axis<mDimensions; axis++) {
			mPruningBar[axis] = new JPruningBar(true, axis);
			mPruningBar[axis].setMinAndMax(0.0f, 1.0f);	//@ new concept requires data initialization here 24-Oct-2012
			mPruningBar[axis].addPruningBarListener(this);
			}

		for (int axis=0; axis<mDimensions; axis++) {
			mComboBoxColumn[axis] = new JComboBoxWithColor();
			mComboBoxColumn[axis].addItemListener(this);
			}

		double[] size2 = new double[3*mDimensions+1];
		for (int i=0; i<mDimensions; i++) {
			if (i != 0)
				size2[3*i-1]   = 4;
			size2[3*i]   = TableLayout.PREFERRED;
			size2[3*i+1] = TableLayout.PREFERRED;
			}
		double[][] size = { {TableLayout.PREFERRED}, size2 };

		mControls = new JWindow(mParentFrame);
		mControls.getContentPane().setLayout(new TableLayout(size));
		for (int axis=0; axis<mDimensions; axis++) {
			mControls.getContentPane().add(mComboBoxColumn[axis], "0,"+(3*axis));
			mControls.getContentPane().add(mPruningBar[axis], "0,"+(3*axis+1));
			}
		mMessagePopup = new JWindow(mParentFrame);
		mMessagePopup.add(new JLabel("This view is controlled by another view."));
		mVisiblePopup = null;

		mVisualization.initializeDataPoints();

		mSynchronizationChildList = new ArrayList<VisualizationPanel>();
		mSynchronizationChildList.add(this);

		mListenerList = new ArrayList<VisualizationListener>();

		setupQualifyingColumns();
		for (int axis=0; axis<mDimensions; axis++) {
			setupColorChoice(mComboBoxColumn[axis], JVisualization.cColumnUnassigned);

			// this causes ItemEvents to be sent
			setComboBox(axis, mSecondChoiceColumns);
			}
		}

	/**
	 * If zooming, column-axis assignment and rotation state (3D only) of this panel's
	 * visualization is synchronized with an external master panel, then the master
	 * is returned.
	 * @return external master panel or null
	 */
	public VisualizationPanel getSynchronizationMaster() {
		return (mMasterPanel == this) ? null : mMasterPanel;
		}

	public ArrayList<VisualizationPanel> getSynchronizationChildList() {
		return mSynchronizationChildList;
		}

	/**
	 * Determines whether this VisualizationPanel serves as synchronization master
	 * for another VisualizationPanel being different from this instance.
	 * @return
	 */
	public boolean isSynchronizationMaster() {
		for (VisualizationPanel vp:mSynchronizationChildList)
			if (vp != this)
				return true;

		return false;
		}

	/**
	 * Changes the control of the panel from the current master to the given master panel.
	 * The control includes zoom & rotation (only 3D panels) state as well as column-axis
	 * assignment. A panel that is not its own master has no visible axis controls
	 * (comboboxes and pruning bars). When this method is called, it informs old and
	 * new master by calling addSynchronizedChild() and removeSynchronizedChild().
	 * @param newMaster new master or null, which is equivalent to this panel
	 */
	public void setSynchronizationMaster(VisualizationPanel newMaster) {
		if (newMaster == null)
			newMaster = this;

		if (mMasterPanel == newMaster)
			return;

		VisualizationPanel oldMaster = mMasterPanel;

		if (newMaster == this)
			synchronizeControlsWithMaster();

		oldMaster.removeSynchronizedChild(this);
		newMaster.addSynchronizedChild(this);

		if (newMaster != this) {
			if (mControls.isVisible())
				mControls.setVisible(false);
			synchronizeViewWithMaster(newMaster);
			}

		mMasterPanel = newMaster;
		}

	private void synchronizeControlsWithMaster() {
		for (int axis=0; axis<mDimensions; axis++) {
			int index = mMasterPanel.getComboBoxSelectedIndex(axis);
			if (mComboBoxColumn[axis].getSelectedIndex() != index) {
				setComboBox(axis, index);
				initializePruningBar(axis, mQualifyingColumn[index]);
				}
			mPruningBar[axis].setLowAndHigh(mMasterPanel.getPruningBar(axis).getLowValue(),
											mMasterPanel.getPruningBar(axis).getHighValue(), false);
			}
		}

	protected void synchronizeViewWithMaster(VisualizationPanel newMaster) {
		for (int axis=0; axis<mDimensions; axis++) {
			if (newMaster.getSelectedColumn(axis) != mMasterPanel.getSelectedColumn(axis))
				mVisualization.setColumnIndex(axis, newMaster.getSelectedColumn(axis));

			JPruningBar pb = newMaster.getPruningBar(axis);
			mVisualization.updateVisibleRange(axis, pb.getLowValue(), pb.getHighValue(), false);
			}		
		}

	private void addSynchronizedChild(VisualizationPanel vp) {
		mSynchronizationChildList.add(vp);
		}

	private void removeSynchronizedChild(VisualizationPanel vp) {
		mSynchronizationChildList.remove(vp);
		}

	@Override
	public int print(Graphics g, PageFormat f, int pageIndex) {
		return mVisualization.print(g, f, pageIndex);
		}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(64, 64);
		}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom(e.getX()-mVisualization.getX(), e.getY()-mVisualization.getY(), e.getWheelRotation());
		}

	/**
	 * Changes the current zoom state of this view and all synchronized views, such that
	 * the screen point (sx,sy) stays and its surrounding is zoomed in or out depending on the
	 * sign of scroll wheel steps.
	 * @param sx
	 * @param sy
	 * @param steps
	 */
	public abstract void zoom(int sx, int sy, int steps);

	/**
	 * Sets a new zoom state by defining low and high values for all dimensions.
	 * This effectively updates all zoom sliders and then causes an update of the view.
	 * If this view is a synchronization master or slave, then all synchronized views
	 * are effected.
	 * @param low
	 * @param high
	 * @param isAdjusting
	 */
	public void setZoom(float[] low, float[] high, boolean isAdjusting) {
		int dimensions = Math.min(low.length, mDimensions);
		for (int i=0; i<dimensions; i++)
			getActingPruningBar(i).setLowAndHigh(low[i], high[i], true);

		for (VisualizationPanel child:mMasterPanel.getSynchronizationChildList())
			for (int i=0; i<dimensions; i++)
				child.getVisualization().updateVisibleRange(i, low[i], high[i], isAdjusting);

		if (!isAdjusting)
			fireVisualizationChanged();
		}

	@Override
	public void pruningBarChanged(PruningBarEvent e) {
		for (VisualizationPanel child:getSynchronizationChildList())
			child.getVisualization().updateVisibleRange(e.getID(), e.getLowValue(), e.getHighValue(), e.isAdjusting());

		if (!e.isAdjusting())
			fireVisualizationChanged();
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED && !mDisableEvents) {
			for (int axis=0; axis<mDimensions; axis++) {
				if (e.getSource() == mComboBoxColumn[axis]) {
					int index = mComboBoxColumn[axis].getSelectedIndex();
					if (index != -1) {
						for (VisualizationPanel child:getSynchronizationChildList())
							child.setColumnIndex(axis, mQualifyingColumn[index]);

						fireVisualizationChanged();
						}
					}
				}
			}
		}

	private void fireVisualizationChanged() {
		if (!mIsProgrammaticChange)
			for (VisualizationListener vl:mListenerList)
				vl.visualizationChanged(new VisualizationEvent(this));
		}

	private void setColumnIndex(int axis, int column) {
		mVisualization.setColumnIndex(axis, column);
		initializePruningBar(axis, column);
		}

	/**
	 * Gets the column index that is currently selected by the combobox.
	 * This may be different from the displayed column if this panel is
	 * synchronized to another VisualizationPanel.
	 * @param axis
	 * @return
	 */
	public int getSelectedColumn(int axis) {
		return mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
		}

	public int getQualifyingColumn(int index) {
		return mQualifyingColumn[index];
		}

	private int getComboBoxSelectedIndex(int i) {
		return mComboBoxColumn[i].getSelectedIndex();
		}

	public int getFocusHitlist() {
		return mVisualization.getFocusHitlist();
		}

	public void setFocusHitlist(int no) {
		mVisualization.setFocusHitlist(no);
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		// don't make mVisualization a direct listener because in case of
		// a table structure change the pruning panel needs to be updated first
		boolean updatePruningBar = false;
		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows
		 || e.getType() == CompoundTableEvent.cChangeColumnData) {
			int[] selected = new int[mDimensions];
			for (int axis=0; axis<mDimensions; axis++)
				selected[axis] = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			setupQualifyingColumns();
			for (int axis=0; axis<mDimensions; axis++) {
				setupColorChoice(mComboBoxColumn[axis], selected[axis]);
				int column = mVisualization.getColumnIndex(axis);
				if (column != JVisualization.cColumnUnassigned
				 && (e.getType() != CompoundTableEvent.cChangeColumnData
				  || column == e.getSpecifier())) {
					boolean found = false;
					for (int j=0; j<=mSecondChoiceColumns; j++) {
						if (selected[axis] == mQualifyingColumn[j]) {
							mDisableEvents = true;
							setComboBox(axis, j);
							mDisableEvents = false;
							updatePruningBar = true;
							found = true;
							break;
							}
						}
					if (!found) {
						mDisableEvents = true;
						setComboBox(axis, mSecondChoiceColumns);
						mDisableEvents = false;
						for (VisualizationPanel vp:mSynchronizationChildList)
							if (axis < vp.getDimensionCount())
								vp.getVisualization().setColumnIndex(axis, JVisualization.cColumnUnassigned);
						initializePruningBar(axis, JVisualization.cColumnUnassigned);
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cAddColumns) {
			int[] selected = new int[mDimensions];
			for (int axis=0; axis<mDimensions; axis++)
				selected[axis] = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			setupQualifyingColumns();
			for (int axis=0; axis<mDimensions; axis++)
				setupColorChoice(mComboBoxColumn[axis], selected[axis]);
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			int[] selected = new int[mDimensions];
			for (int axis=0; axis<mDimensions; axis++)
				selected[axis] = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			setupQualifyingColumns();
			for (int axis=0; axis<mDimensions; axis++) {
				int newSelected = (selected[axis] == JVisualization.cColumnUnassigned) ?
						JVisualization.cColumnUnassigned : columnMapping[selected[axis]];
				setupColorChoice(mComboBoxColumn[axis], newSelected);
				if (selected[axis] != JVisualization.cColumnUnassigned
				 && newSelected == JVisualization.cColumnUnassigned) {
					initializePruningBar(axis, JVisualization.cColumnUnassigned);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			int column = e.getSpecifier();
			for (int i=0; i<mSecondChoiceColumns; i++) {
				if (mQualifyingColumn[i] == column) {
					for (int axis=0; axis<mDimensions; axis++) {
						((ComboBoxColorItem)mComboBoxColumn[axis].getItemAt(i)).setText(mVisualization.getAxisTitle(column));
						mComboBoxColumn[axis].validate();
						mComboBoxColumn[axis].repaint();
						}
					break;
					}
				}
			}

		for (VisualizationPanel vp:mSynchronizationChildList)
			vp.getVisualization().compoundTableChanged(e);

		if (updatePruningBar) {
			for (int axis=0; axis<mDimensions; axis++) {
				mPruningBar[axis].setLowAndHigh(mVisualization.getPruningBarLow(axis),
												mVisualization.getPruningBarHigh(axis), false);
				int column = mVisualization.getColumnIndex(axis);
				mPruningBar[axis].setUseRedColor(column != JVisualization.cColumnUnassigned
											  && !mTableModel.isColumnDataComplete(column)
											  && mTableModel.isColumnTypeDouble(column));
				}
			}
		}

	@Override
	public void hitlistChanged(CompoundTableHitlistEvent e) {
		mVisualization.hitlistChanged(e);
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	public JVisualization getVisualization() {
		return mVisualization;
		}

	public int getDimensionCount() {
		return mDimensions;
		}

	/**
	 * Returns this panel's pruning bar, which may currently not be used if
	 * the panel is synchronized to another master panel.
	 * If this is not what you want, check getActingPruningBar(int axis).
	 * @param axis
	 * @return
	 */
	public JPruningBar getPruningBar(int axis) {
		return mPruningBar[axis];
		}

	/**
	 * Returns the pruning bar that is currently controlling the axis of this panel.
	 * It may belong to another panel, if this panel is synchronized with another master panel.
	 * @param axis
	 * @return
	 */
	public JPruningBar getActingPruningBar(int axis) {
		return mMasterPanel.mPruningBar[axis];
		}

	public String getAxisColumnName(int axis) {
		int index = mComboBoxColumn[axis].getSelectedIndex();
		if (index == mSecondChoiceColumns)
			return UNASSIGNED_TEXT;

		return mTableModel.getColumnTitleNoAlias(mQualifyingColumn[index]);
		}

	/**
	 * Assigns a column to a given axis by specifying the column name.
	 * If there is no column with this name then nothing happens.
	 * Effectively, this updates the column selecting combo box of this
	 * visualization, which in turn updates all synchronized child views.
	 * If this view is child view of another, then this method has no effect
	 * until the synchronization is broken.
	 * @param axis
	 * @param name
	 * @return true if the column assigned to this axis was changed
	 */
	public boolean setAxisColumnName(int axis, String name) {
		int index = mSecondChoiceColumns;		// default: unassigned
		if (!name.equals(UNASSIGNED_TEXT)) {
			int column = mTableModel.findColumn(name);
			if (column != -1) {
				for (int i=0; i<mSecondChoiceColumns; i++) {
					if (column == mQualifyingColumn[i]) {
						index = i;
						break;
						}
					}
				}
			}

		if (index != mComboBoxColumn[axis].getSelectedIndex()) {
			setComboBox(axis, index);
			return true;
			}

		return false;
		}

	/**
	 * Changes the index of a combobox without sending VisualizationEvents
	 * @param axis
	 * @param index
	 */
	private void setComboBox(int axis, int index) {
		mIsProgrammaticChange = true;
		mComboBoxColumn[axis].setSelectedIndex(index);
		mIsProgrammaticChange = false;
		}

	public void resetAllFilters() {
		for (int axis=0; axis<mDimensions; axis++) {
			int column = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			if (column != JVisualization.cColumnUnassigned) {
				if (!mTableModel.isColumnDataComplete(column)) {
					setComboBox(axis, mSecondChoiceColumns);
					initializePruningBar(axis, JVisualization.cColumnUnassigned);
					}
				else {
					mPruningBar[axis].reset();
					}
				}
			}
		}

	public void setDefaultColumns() {
		int count = 0;
		for (int i=0; i<mSecondChoiceColumns; i++)
			if (mTableModel.isColumnTypeDouble(mQualifyingColumn[i]))
				count++;
		NumericalCompoundTableColumn[] numericalColumn = new NumericalCompoundTableColumn[count];
		count = 0;
		for (int i=0; i<mSecondChoiceColumns; i++)
			if (mTableModel.isColumnTypeDouble(mQualifyingColumn[i]))
				numericalColumn[count++] = new NumericalCompoundTableColumn(mTableModel ,mQualifyingColumn[i]);

		int[] index = new int[mDimensions];
		for (int axis=0; axis<mDimensions; axis++)
			index[axis] = -1;

		if (numericalColumn.length >= 2) {
			double[][] correlation = CorrelationCalculator.calculateMatrix(numericalColumn, CorrelationCalculator.TYPE_BRAVAIS_PEARSON);
	
			double maxCorrelation = 0;
			for (int i=1; i<correlation.length; i++) {
				for (int j=0; j<correlation[i].length; j++) {
					if (!Double.isNaN(correlation[i][j])) {
						if (maxCorrelation < Math.abs(correlation[i][j])) {
							maxCorrelation = Math.abs(correlation[i][j]);
							index[0] = j;
							index[1] = i;
							}
						}
					}
				}

			if (mDimensions == 3 && index[0] != -1) {
				maxCorrelation = 0;
				for (int i=0; i<correlation.length; i++) {
					if (i != index[0] && i != index[1]) {
						for (int j=0; j<2; j++) {
							double c = (i < index[j]) ? correlation[index[j]][i] : correlation[i][index[j]];
							if (!Double.isNaN(c)) {
								if (maxCorrelation < Math.abs(c)) {
									maxCorrelation = Math.abs(c);
									index[2] = i;
									}
								}
							}
						}
					}
				}
			}

		int nonCorrelationIndex = 0;
		boolean[] inUse = new boolean[mSecondChoiceColumns];
		for (int axis=0; axis<mDimensions; axis++) {
			if (index[axis] != -1) {
				for (int j=0; j<mSecondChoiceColumns; j++) {
					if (numericalColumn[index[axis]].getColumn() == mQualifyingColumn[j]) {
						setComboBox(axis, j);
						inUse[j] = true;
						break;
						}
					}
				}
			else {
				while (nonCorrelationIndex<mFirstChoiceColumns && inUse[nonCorrelationIndex])
					nonCorrelationIndex++;

				setComboBox(axis, (nonCorrelationIndex<mFirstChoiceColumns) ? nonCorrelationIndex : mSecondChoiceColumns);
				}
			}
		}

	private void setupQualifyingColumns() {
		mQualifyingColumn = new int[mTableModel.getTotalColumnCount()+1];
		mFirstChoiceColumns = 0;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.hasNumericalVariance(i)
			 && (mTableModel.isColumnDataComplete(i) || !mTableModel.isColumnTypeDouble(i)))
				mQualifyingColumn[mFirstChoiceColumns++] = i;
		mSecondChoiceColumns = mFirstChoiceColumns;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.hasNumericalVariance(i)
			 && (!mTableModel.isColumnDataComplete(i) && mTableModel.isColumnTypeDouble(i)))
				mQualifyingColumn[mSecondChoiceColumns++] = i;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.isDescriptorColumn(i))
				mQualifyingColumn[mSecondChoiceColumns++] = i;
		mQualifyingColumn[mSecondChoiceColumns] = JVisualization.cColumnUnassigned;
		}

	/**
	 * Populates the combobox with all entries of mQualifyingColumn
	 * without sending any events.
	 * @param choice
	 * @param column
	 */
	private void setupColorChoice(JComboBox choice, int column) {
		mDisableEvents = true;
		mIsProgrammaticChange = true;

		choice.removeAllItems();
		for (int j=0; j<mFirstChoiceColumns; j++) {
			Color color = (mTableModel.isColumnTypeCategory(mQualifyingColumn[j])
					   && !mTableModel.isColumnTypeDouble(mQualifyingColumn[j])) ?
					   			Color.blue : Color.black;
			choice.addItem(new ComboBoxColorItem(mVisualization.getAxisTitle(mQualifyingColumn[j]), color));
			}
		for (int j=mFirstChoiceColumns; j<mSecondChoiceColumns; j++) {
			Color color = (mTableModel.isDescriptorColumn(j) && mTableModel.isColumnDataComplete(j)) ? Color.BLACK : Color.RED;
			choice.addItem(new ComboBoxColorItem(mVisualization.getAxisTitle(mQualifyingColumn[j]), color));
			}
		choice.addItem(new ComboBoxColorItem(UNASSIGNED_TEXT, Color.black));

		for (int j=0; j<=mSecondChoiceColumns; j++) {
			if (column == mQualifyingColumn[j]) {
				choice.setSelectedIndex(j);
				break;
				}
			}

		mIsProgrammaticChange = false;
		mDisableEvents = false;
		}

	/**
	 * Maximizes pruning bar's setting to cover the full range.
	 * Updates the pruning bar color to reflect (in-)completeness of data.
	 * Does not send any events.
	 * @param axis
	 * @param column
	 */
	private void initializePruningBar(int axis, int column) {
		mPruningBar[axis].setMinAndMax(0.0f, 1.0f);
		if (column == JVisualization.cColumnUnassigned) {
			mIsLogarithmic[axis] = false;
			mPruningBar[axis].setUseRedColor(false);
			}
		else if (mTableModel.isDescriptorColumn(column)) {
			mIsLogarithmic[axis] = false;
			mPruningBar[axis].setUseRedColor(mTableModel.isColumnDataComplete(column));
			}
		else {
			mMinValue[axis] = mTableModel.getMinimumValue(column);
			mMaxValue[axis] = mTableModel.getMaximumValue(column);
			mIsLogarithmic[axis] = mTableModel.isLogarithmicViewMode(column);
			mPruningBar[axis].setUseRedColor(!mTableModel.isColumnDataComplete(column)
										  && mTableModel.isColumnTypeDouble(column));
			}
		}
	}
