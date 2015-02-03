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

import java.awt.Component;

import org.jmol.api.JmolAdapter.AtomIterator;
import org.jmol.viewer.Viewer;

import com.actelion.research.gui.jmol.ActelionJmolAdapter.BondIterator;



public class ViewerWrapper extends Viewer {
	
	private final ActelionJmolAdapter modelAdapter;
	private boolean stopRotation;
	
	public ViewerWrapper(Component display, ActelionJmolAdapter jmolAdapter) {
		super(display, jmolAdapter);
		this.modelAdapter = jmolAdapter;
		isPreviewOnly = true;
	}
	
	public void stopRotation(boolean stopRotation) {
		this.stopRotation = stopRotation;
	}
	
	@Override
	public int notifyMouseClicked(int x, int y, int modifiers, int clickCount) {
		if(stopRotation) return 0;
		return super.notifyMouseClicked(x, y, modifiers, clickCount);
	}
	
	
	public boolean refresh(Object clientFile) {
//		forceRefresh(clientFile);
//		return true;
		boolean changed;
		if (modelAdapter.getEstimatedAtomCount(clientFile) != modelSet.atoms.length) {
			forceRefresh(clientFile);
			changed = true;
		} else {
			
			changed = false;
			int i = 0;
			for (BondIterator iter = modelAdapter.getBondIterator(clientFile); iter.hasNext(); ) {
				i++;
			}
			if(i!=modelSet.getBondCount()){
				forceRefresh(clientFile);
				return true;		
			}
			{
				AtomIterator iter = modelAdapter.getAtomIterator(clientFile);
				i = 0;
				for (; iter.hasNext() && i < modelSet.atoms.length; i++) {
					if(modelSet.atoms[i].isHetero()!=iter.getIsHetero()) {
						forceRefresh(clientFile);
						return true;					
					}
					if(modelSet.atoms[i].atomicAndIsotopeNumber!=iter.getElementNumber()) {
						forceRefresh(clientFile);
						return true;
					}
				}
				if(iter.hasNext() || i!=modelSet.atoms.length) {
					forceRefresh(clientFile);
					return true;				
				}
		
				i = 0;
				for (iter = modelAdapter.getAtomIterator(clientFile); iter.hasNext() && iter.getIsHetero() && i < modelSet.atoms.length; i++) {							
					modelSet.atoms[i].x = iter.getX();
					modelSet.atoms[i].y = iter.getY();
					modelSet.atoms[i].z = iter.getZ();
					modelSet.atoms[i].atomicAndIsotopeNumber = (short) iter.getElementNumber();
				}
			}
		}
		
		return changed;
		
	}
	
	public void forceRefresh(Object clientFile) {
		modelSet = modelManager.createModelSet(null, null, clientFile, false);
		
	}
	
}
