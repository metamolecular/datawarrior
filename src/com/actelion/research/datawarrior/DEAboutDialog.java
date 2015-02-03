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

package com.actelion.research.datawarrior;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JDialog;
import com.actelion.research.gui.*;

public class DEAboutDialog extends JDialog implements MouseListener,Runnable {
	private static final long serialVersionUID = 20140219L;
	private int mMillis;

    public DEAboutDialog(Frame owner) {
		super(owner, "About OSIRIS DataWarrior", true);

		getContentPane().add(new JImagePanelFixedSize("/images/about.jpg"));

		addMouseListener(this);

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
		}

    public DEAboutDialog(Frame owner, int millis) {
		super(owner, "About OSIRIS DataWarrior", true);

		getContentPane().add(new JImagePanelFixedSize("/images/about.jpg"));

		pack();
		setLocationRelativeTo(owner);

		mMillis = millis;
		new Thread(this).start();

		setVisible(true);
		}

	public void run() {
		try {
			Thread.sleep(mMillis);
			}
		catch (Exception e) {}
		dispose();
		}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {
		dispose();
		}
	}
