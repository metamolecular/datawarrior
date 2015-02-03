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

import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import javax.swing.*;

import com.actelion.research.calc.*;
import com.actelion.research.table.*;
import com.actelion.research.util.DoubleFormat;


public class DECorrelationDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 0x20080507;

	private JComboBox			mComboBoxCorrelationType;
	private Frame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private int[]				mNumericalColumn;
	private double[][][]		mMatrix;

    public DECorrelationDialog(Frame parent, CompoundTableModel tableModel) {
		super(parent, "Correlation Matrix", true);
		mParentFrame = parent;
		mTableModel = tableModel;

		mNumericalColumn = getNumericalColumns(mTableModel);
		if (mNumericalColumn.length < 2) {
            JOptionPane.showMessageDialog(parent, "Sorry, you need at least two numerical columns\n"
                                                 +"in order to calculate a correlation matrix.");
		    return;
		    }
		mMatrix = new double[CorrelationCalculator.TYPE_NAME.length][][];

		JPanel p = new JPanel();
        double[][] size = { {8, 80, TableLayout.FILL, 80, 8},
                            {8, TableLayout.PREFERRED, 8, TableLayout.FILL, 12, TableLayout.PREFERRED, 8 } };
        p.setLayout(new TableLayout(size));

		JPanel cbp = new JPanel();
		cbp.add(new JLabel("Correlation Coefficient:"));
		mComboBoxCorrelationType = new JComboBox(CorrelationCalculator.TYPE_NAME);
		mComboBoxCorrelationType.addActionListener(this);
		cbp.add(mComboBoxCorrelationType);
		p.add(cbp, "1,1,3,1");

		JPanel matrixPanel = new JPanel() {
		    private static final long serialVersionUID = 0x20080507;

		    private final int SPACING = 4;
		    private final int NUM_CELL_WIDTH = 24;
		    private final int CELL_WIDTH = 72;
		    private final int CELL_HEIGHT = 16;
            private Dimension size;
            private int titleWidth;

		    public Dimension getPreferredSize() {
		        if (size == null) {
		            for (int i=0; i<mNumericalColumn.length; i++)
		                titleWidth = Math.max(titleWidth, mParentFrame.getGraphics().getFontMetrics().stringWidth(mTableModel.getColumnTitle(mNumericalColumn[i])));

		            size = new Dimension(CELL_WIDTH * mNumericalColumn.length + titleWidth + 2*SPACING + NUM_CELL_WIDTH,
		                                 CELL_HEIGHT * mNumericalColumn.length + CELL_HEIGHT);
		            }

		        return size;
		        }

		    public void paint(Graphics g) {
		        super.paint(g);
		        g.setColor(getBackground().darker());
                for (int i=0; i<mNumericalColumn.length; i++) {
                    g.fillRect(2*SPACING+titleWidth+1, (i+1)*CELL_HEIGHT+1, NUM_CELL_WIDTH-2, CELL_HEIGHT-2);
                    g.fillRect(NUM_CELL_WIDTH+2*SPACING+titleWidth+i*CELL_WIDTH+1, 1, CELL_WIDTH-2, CELL_HEIGHT-2);
                    }
                g.setColor(Color.WHITE);
                for (int i=0; i<mNumericalColumn.length; i++) {
                    g.fillRect(1, (i+1)*CELL_HEIGHT+1, titleWidth+2*SPACING-2, CELL_HEIGHT-2);
                    }
                g.setColor(Color.BLACK);
                for (int i=0; i<mNumericalColumn.length; i++) {
                    String s = ""+(i+1);
                    int stringWidth = g.getFontMetrics().stringWidth(s);
                    g.drawString(s, 2*SPACING+titleWidth+(NUM_CELL_WIDTH-stringWidth)/2, (i+2)*CELL_HEIGHT-3);
                    g.drawString(s, NUM_CELL_WIDTH+2*SPACING+titleWidth+i*CELL_WIDTH+(CELL_WIDTH-stringWidth)/2, CELL_HEIGHT-3);
                    g.drawString(mTableModel.getColumnTitle(mNumericalColumn[i]), SPACING, (i+2)*CELL_HEIGHT-3);
                    }

                int type = mComboBoxCorrelationType.getSelectedIndex();
                if (mMatrix[type] == null) {
                	NumericalCompoundTableColumn[] nc = new NumericalCompoundTableColumn[mNumericalColumn.length];
            		for (int i=0; i<mNumericalColumn.length; i++)
            			nc[i] = new NumericalCompoundTableColumn(mTableModel, mNumericalColumn[i]);
                    mMatrix[type] = CorrelationCalculator.calculateMatrix(nc, type);
                	}

                int xOffset = NUM_CELL_WIDTH+2*SPACING+titleWidth;
                int yOffset = CELL_HEIGHT;
		        for (int i=1; i<mNumericalColumn.length; i++) {
		            for (int j=0; j<i; j++) {
		                g.setColor(new Color(Color.HSBtoRGB((float)0.4, (float)Math.abs(mMatrix[type][i][j]), (float)0.8)));
		                g.fillRect(xOffset+i*CELL_WIDTH+1, yOffset+j*CELL_HEIGHT+1, CELL_WIDTH-2, CELL_HEIGHT-2);
                        g.fillRect(xOffset+j*CELL_WIDTH+1, yOffset+i*CELL_HEIGHT+1, CELL_WIDTH-2, CELL_HEIGHT-2);
		                g.setColor(Color.BLACK);
		                g.drawString(DoubleFormat.toString(mMatrix[type][i][j], 3), xOffset+i*CELL_WIDTH+SPACING, yOffset+CELL_HEIGHT+j*CELL_HEIGHT-3);
                        g.drawString(DoubleFormat.toString(mMatrix[type][i][j], 3), xOffset+j*CELL_WIDTH+SPACING, yOffset+CELL_HEIGHT+i*CELL_HEIGHT-3);
		                }
		            }
		        }
		    };
		matrixPanel.setSize(matrixPanel.getPreferredSize());
        if (mNumericalColumn.length > 10) {
            JScrollPane scrollPane = new JScrollPane(matrixPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setPreferredSize(new Dimension(720, 192));
            p.add(scrollPane, "1,3,3,3");
            }
        else {
            p.add(matrixPanel, "1,3,3,3");
            }

        JButton bcopy = new JButton("Copy");
        bcopy.addActionListener(this);
        p.add(bcopy, "1,5");
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		p.add(bok, "3,5");

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(p, BorderLayout.CENTER);
		getRootPane().setDefaultButton(bok);
	
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
		}

    public static int[] getNumericalColumns(CompoundTableModel tableModel) {
        int count = 0;
        for (int column=0; column<tableModel.getTotalColumnCount(); column++)
            if (tableModel.isColumnTypeDouble(column))
                count++;

        int[] numericalColumn = new int[count];
        count = 0;
        for (int column=0; column<tableModel.getTotalColumnCount(); column++)
            if (tableModel.isColumnTypeDouble(column))
                numericalColumn[count++] = column;

        return numericalColumn;
        }

    public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == mComboBoxCorrelationType) {
	        repaint();
			return;
	    	}

	    if (e.getActionCommand().equals("Copy")) {
            int type = mComboBoxCorrelationType.getSelectedIndex();

            StringBuilder buf = new StringBuilder("r ("+CorrelationCalculator.TYPE_NAME[type]+")\t");
	        for (int i=0; i<mNumericalColumn.length; i++)
	            buf.append('\t').append(""+(i+1));
	        buf.append('\n');

	        for (int i=0; i<mNumericalColumn.length; i++) {
	            buf.append(mTableModel.getColumnTitle(mNumericalColumn[i])).append('\t').append(""+(i+1));
	            for (int j=0; j<mNumericalColumn.length; j++) {
	                buf.append('\t');
	                if (i<j)
	                    buf.append(DoubleFormat.toString(mMatrix[type][j][i], 3));
	                else if (i>j)
                        buf.append(DoubleFormat.toString(mMatrix[type][i][j], 3));
	                }
	            buf.append('\n');
	            }

            StringSelection theData = new StringSelection(buf.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);

	        return;
	        }

	    setVisible(false);
	    dispose();
		return;
		}
	}
