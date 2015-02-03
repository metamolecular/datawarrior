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
 * @author Joel Freyss
 */
package com.actelion.research.gui.jmol;

import java.util.Hashtable;

import javax.swing.JPopupMenu;

/**
 * Listener for Jmol. Most methods here are not used but they need an empty body
 */
public class JmolStatusListener implements org.jmol.api.JmolStatusListener {

	private MoleculeViewer viewer; 
	
	public JmolStatusListener(MoleculeViewer viewer) {
		this.viewer = viewer;
	}
	
	@Override
	public String createImage(String arg0, String arg1, Object arg2, int arg3) {
		System.out.println("JmolStatusListener.createImage() "+arg0+" "+arg1+" "+arg2+" "+arg3);
		return null;
	}
	@Override
	public String dialogAsk(String arg0, String arg1) {
		System.out.println("JmolStatusListener.dialogAsk()");
		return null;
	}
	@Override
	public String eval(String arg) {
		System.out.println("JmolStatusListener.eval() "+arg);
		return null;
	}
	@Override
	public float[][] functionXY(String arg0, int arg1, int arg2) {
		System.out.println("JmolStatusListener.functionXY()");
		return null;
	}
	@Override
	public Hashtable getRegistryInfo() {
		System.out.println("JmolStatusListener.getRegistryInfo()");
		return null;
	}
	public void handlePopupMenu(int x, int y) {
		int atom = viewer.findNearestAtomIndex(x, y);
		JPopupMenu jmolpopup = viewer.createPopupMenu(atom);
		jmolpopup.show(viewer, x, y);
	}
	@Override
	public boolean notifyEnabled(int callback) {
		return true; 
	}

	
	@Override	
	public void notifyCallback(int callback, Object[] args) {
	}

	@Override	
	public void setCallbackFunction(String arg0, String arg1) {
		System.out.println("JmolStatusListener.setCallbackFunction()");
		
	}

	public void showConsole(boolean arg0) {
		System.out.println("JmolStatusListener.showConsole()");
	}

	@Override
	public void showUrl(String arg0) {
		System.out.println("JmolStatusListener.showUrl()");
	}

	@Override
	public float[][][] functionXYZ(String functionName, int nx, int ny, int nz) {
		return null;
	}

}
