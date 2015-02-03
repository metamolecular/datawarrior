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

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.table.CompoundTableEvent;
import com.actelion.research.table.CompoundTableHitlistEvent;
import com.actelion.research.table.CompoundTableModel;
import com.actelion.research.table.view.data.Handler;

public class ExplanationView extends JScrollPane implements CompoundTableConstants,CompoundTableView {
    private static final long serialVersionUID = 0x20130114;

    private CompoundTableModel	mTableModel;
    private JTextPane			mTextPane;

	public ExplanationView(CompoundTableModel tableModel) {
		mTableModel = tableModel;

		setBorder(BorderFactory.createEmptyBorder());
		mTextPane = new JTextPane();
		mTextPane.setBorder(null);
		mTextPane.setContentType("text/html");
		mTextPane.setEditable(false);

		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
		setViewportView(mTextPane);

		String explanation = (String)mTableModel.getExtensionData(cExtensionNameFileExplanation);
//		String testImage = "iVBORw0KGgoAAAANSUhEUgAAAGQAAAAyCAYAAACqNX6+AAACeklEQVR42u1bHZBCURgNgiBYCINgIVhYCIKFhSBYCIIgCIKFxSBoZpsJgjAIgmAhCIIgCIKFIAiChSAIF4IgCL7d82abnWl69Xq9+7r1Dhyp93PfOff7ufd+n8/nEyF0AkmgIAQFoSDEjQgSCn1LPD6SbPZDSqWKNBqv0m5nZDh8lsnkUebziIH1OiC/d+wF/tteN50+GPfiGbVaQcrld8nnm8Y78C4K8odAYC3R6Jfkci2pVosGaYtFWDYbvynRKgDx8G4Ij7FgTBjbzQuC2ZhOd4wZCgIOzfBLYysSxooxh8OL2xAEH4KPGo3irs98pwF3CZcXi42vS5CtCPiAaxfBDLPZvRQKNUWW49CDEomBdDrpmxXBDN1uSlKprvj9m8sLgkHAx47HMU+JYObSkBmenxDYvDGTaRum63UhdoFUG9maa4IgW4KZkvzD6PVebMaYEy6GSS6XdyTcIlaroA1rsRgr6vU3zwVsp4BFZzC4ckYQBCmYH4k9D4NBwmLAP2IZFMNZUY6nxwf+rFRKJNJhYLVvSxAs9Bgz1ADcniQIzIprDLVbL+aua8+PyWSfxCkGOLYsSKuVI2mKAY4tC4LlP0lTv8ViWRAS5g4oyLUKQpelmctiUNcsqDPt1Szt5cJQs4Uht0402zrh5qKGm4tb19XvJ0mkq2ciPKC6ngOq3SNcEms/xXXsCJdFDhoWOeyWAdGFWSsDikTm7hXKwVq4VjEvlLNfWnpmKSkqGFlK+l9Kaj1WuFBs7cWKRrgmbYqtvdyOUCxW9W5HOCQOXBobdtjSxpY2J5o+L0W+55o+7bZFN5t5JW3RT0+fbIsmKAgFISgIBSHU4QdCoO0W7Xd4AwAAAABJRU5ErkJggg==";
//		String explanation = "<html><body>Local image<br><img src=\"data:image/png;charset=utf-8;base64,"+testImage+"\"></body></html>";

		Handler.install();
		mTextPane.setText(explanation == null ? "" : explanation);
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeExtensionData
		 && cExtensionNameFileExplanation.equals(mTableModel.getExtensionHandler().getName(e.getSpecifier()))) {
			String explanation = (String)mTableModel.getExtensionData(cExtensionNameFileExplanation);
			mTextPane.setText(explanation == null ? "" : explanation);
			}
		if (e.getType() == CompoundTableEvent.cNewTable) {
			mTextPane.setText("");
			}
		}

	public String getText() {
		return mTextPane.getText();
		}

	public void setText(String text) {
		mTextPane.setText(text);
		}

	@Override
	public void hitlistChanged(CompoundTableHitlistEvent e) {
		}

	@Override
	public void cleanup() {
		mTextPane.setText("");
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}
	}
