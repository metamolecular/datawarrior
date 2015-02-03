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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.ForceField;
import com.actelion.research.forcefield.transformation.TorsionTransform;

/**
 * 
 */
public class ToolTorsion extends ToolEdit implements MouseMotionListener {
	
	
	
	private boolean quickMode = false;
	private TorsionTransform tt = null;
	private int index = -1;
	private Point pt;
	private FFMolecule copy;
	private double initial;

	
	public ToolTorsion() {
		super("Change Torsion");
		quickMode = false;
	}
	
	public ToolTorsion(MoleculeViewer viewer, int atom, boolean rotateSmallestGroup) {
		super("Change Torsion - "+(rotateSmallestGroup?"Smallest Group": "Biggest Group"));
		quickMode = true;
		Point pt = viewer.getMousePosition();
		viewer.setTool(this);
		startTorsion(atom, pt, rotateSmallestGroup);
	}
	
	@Override
	protected boolean hover(int atom, int bond) {
		return false;
	}
	
	
	@Override
	protected void deactivateTool(MoleculeViewer viewer) {
		clean(viewer);
		super.deactivateTool(viewer);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		if(viewer==null) return;
		boolean smallestGroup = !e.isShiftDown() && !e.isControlDown();
		
		FFMolecule mol = viewer.getMolecule();
		mol.compact();
		int atom = viewer.findNearestAtomIndex(e.getX(), e.getY());		

		if(atom<0 || atom>=mol.getNMovables()) return;
		startTorsion(atom, e.getPoint(), smallestGroup);
		
	}
	
	
	public static boolean canRotate(FFMolecule mol, int atom, boolean rotateSmallestGroup) {
		if(atom>=mol.getNMovables()) return false; //Safeguard
		TorsionTransform tt = new TorsionTransform(mol, atom, null, -1, true, rotateSmallestGroup);
		int[] bonds2 = tt.getBondAtoms2();
		
		for (int i = 0; i < bonds2.length; i++) {
			if(bonds2[i]==atom) {
				return true;
			}
		}
		return false;
	}
	public void startTorsion(int atom, Point pt, boolean rotateSmallestGroup) {
		if(viewer==null) return;
		clean(viewer);
		this.pt = pt;

		FFMolecule mol = viewer.getMolecule();

		tt = new TorsionTransform(mol, atom, null, -1, true, rotateSmallestGroup);
		int[] bonds1 = tt.getBondAtoms1();
		int[] bonds2 = tt.getBondAtoms2();
		
		for (int i = 0; i < bonds2.length; i++) {
			if(bonds2[i]==atom) {
				index = i;
				initial = tt.getParameters()[i];
				break;
			}
		}
		if(index<0) {
			return;
		}
		
		copy = new FFMolecule(mol);
		viewer.stopRotation(true);

		
		//Draw the potential if possible
		if(mol.getAllAtoms()==mol.getAtoms()) return;
		
		FFMolecule mol2 = StructureCalculator.extractLigand(mol);
		mol2.compact();
		FFMolecule copy2 = new FFMolecule(mol2);
		ForceField f = new ForceField(mol2);
		//EvaluableConformation eval = new EvaluableConformation(f, true);
		
		int relevantAtom = -1;
		for (int i = 0; i < mol2.getConnAtoms(bonds2[index]); i++) {
			int a = mol.getConnAtom(bonds2[index], i);
			if(a!=bonds1[index] && ((mol2.getAtomicNo(a)<20 && mol2.getAtomicNo(a)>1) || relevantAtom<0)) relevantAtom = a;			
		}
		if(relevantAtom<0) return; //mmm problem
		
		//MultiVariate v = eval.getState();
		double val[] = new double[128];
		double minE = 1000;
		double maxE = -1000;
		double[] energy = new double[128];
		Coordinates[] coords = new Coordinates[val.length];
		for (int i = 0; i < val.length; i++) {
			double rad = 2*Math.PI/val.length*i;
			tt.getParameters()[index]=rad;
			System.arraycopy(tt.getTransformation(copy2.getCoordinates()), 0, mol2.getCoordinates(), 0, copy2.getCoordinates().length);
			energy[i] = f.getTerms().getFGValue(null);

			coords[i] = mol2.getCoordinates()[relevantAtom];
			
			minE = Math.min(minE, energy[i]);
			maxE = Math.max(maxE, energy[i]);
		}
		//double maxScale = Math.min(4, maxE); 
		for (int i = 0; i < val.length; i++) {
			val[i] = (energy[i] - minE) / (maxE-minE) * 3 + 0.8;
		}
		
		
		Coordinates C = mol.getCoordinates()[bonds2[index]];
		Coordinates normal = mol.getCoordinates()[bonds1[index]].subC(mol.getCoordinates()[bonds2[index]]).unit();
		
		
		viewer.script("draw torsion1 line {" + C.x + " " + C.y + " " + C.z + "} {" + mol.getCoordinates()[bonds1[index]].x + " " + mol.getCoordinates()[bonds1[index]].y + " " + mol.getCoordinates()[bonds1[index]].z + "} diameter .5 ;");				

		StringBuilder sb = new StringBuilder();
		String NL = System.getProperty("line.separator");
		sb.append((val.length+1) + NL);
		int torsionMin = 0;
		for (int i = 0; i < val.length; i++) {
			double scale = val[i];
			
			Coordinates r = coords[i];
			Coordinates p = C.addC(normal.scaleC(normal.dot(r.subC(C))));
			Coordinates c2 = p.addC(r.subC(p).scaleC(scale));
			
			if(i==0) {
				sb.append(C.x + " " + C.y + " " + C.z + NL);
			}
			sb.append(c2.x + " " + c2.y + " " + c2.z + NL);

			if(val[i%val.length]<val[(i-1+val.length)%val.length] && val[i%val.length]<val[(i+1)%val.length]) {
				viewer.script("draw torsionmin"+(torsionMin++)+ " line {" + c2.x + " " + c2.y + " " + c2.z + "} {"+C.x + " " + C.y + " " + C.z + "} \"" + new DecimalFormat("0.00").format(energy[i]) + "\" diameter .4 color translucent orange;");
			}
		}
		sb.append(val.length+NL);
		for (int i = 0; i < val.length; i++) {
			sb.append("4"+NL+"0"+NL+((i%val.length)+1)+NL+(((i+1)%val.length)+1)+NL+"0"+NL);
		}
		viewer.script("isosurface torsionmesh pmesh inline \"" + sb + "\" mesh translucent .4");
		tt.getParameters()[index] = initial;
	}
	
	public void clean(MoleculeViewer viewer) {
		if(viewer==null) return;
		for (int i = 0; i < 12; i++) {
			viewer.script("draw torsionmin"+i+" off;");			
		}
		viewer.script("isosurface torsionmesh off;");			
		viewer.script("draw torsion1 off;");			
		viewer.script("draw torsion2 off;");			
	}

	
	
	@Override
	public void mouseMoved(MouseEvent e) {
		FFMolecule mol = viewer.getMolecule();
		int atom = viewer.findNearestAtomIndex(e.getX(), e.getY());
		if(atom>=0) {
			TorsionTransform tt = new TorsionTransform(mol, atom, null, -1, true, true);
			int[] bonds1 = tt.getBondAtoms1();
			int[] bonds2 = tt.getBondAtoms2();
			int bond = -1;
			for (int i = 0; i < bonds2.length; i++) {
				if(bonds2[i]==atom) {
					bond = mol.getBondBetween(bonds1[i], bonds2[i]);
					break;
				}
			}
			if(bond>=0) {
				int a1 = mol.getBondAtom(0, bond);
				int a2 = mol.getBondAtom(1, bond);
				viewer.script("draw torsionbond cylinder {atomno="+(a1+1)+"} {atomno="+(a2+1)+"} diameter " + (mol.getBondOrder(bond)==1?.5: .8) + " translucent .45;");
				viewer.script("draw halocircle circle {atomno="+(atom+1)+"} diameter 1.1 translucent .45;");				

			} else {
				viewer.script("draw torsionbond delete; draw halocircle delete;");
			}
		} else {
			viewer.script("draw torsionbond delete; draw halocircle delete;");			
		}
		
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(viewer==null) return;
		FFMolecule mol = viewer.getMolecule();
		if(tt==null || index<0) return;
		
		Point pt2 = e.getPoint();
		double dist = pt2.x - pt.x;// + pt2.y - pt.y;
		if(Math.abs(dist)<2) return;
		tt.getParameters()[index] = initial + dist/30.0*Math.PI/4;
		Coordinates[] coords = tt.getTransformation(StructureCalculator.getLigandCoordinates(copy));
		for (int i = 0; i < coords.length; i++) {
			mol.getCoordinates()[i].x = coords[i].x;
			mol.getCoordinates()[i].y = coords[i].y;
			mol.getCoordinates()[i].z = coords[i].z;
		}
		viewer.refresh();
				
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(viewer==null) return;		
		index = -1;
		viewer.stopRotation(false);

		if(quickMode) {
			deactivateTool(viewer);
		}
	}

	
	

}
