/*
 * Created on Sep 23, 2004
 *
 */
package com.actelion.research.gui.viewer2d;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.GeometryCalculator;


/**
 * Dihedral Measurement Tool 
 * @author freyssj
 */
public final class ToolMeasureDihedral extends ToolMeasure {

	public Coordinates[] getBounds() {
		return null;
	}
	
	public void callSub(IPickable shape, final Canvas3D canvas) {

		if(canvas.getPickedShapes().size()>=4) {				


			final int a1 = canvas.getPickedShapes().get(0).getValue();
			final int a2 = canvas.getPickedShapes().get(1).getValue();
			final int a3 = canvas.getPickedShapes().get(2).getValue();
			final int a4 = canvas.getPickedShapes().get(3).getValue();
			if(a1<0 || a2<0 || a3<0 || a4<0) return;

			canvas.addPaintProcessor(new PaintProcessor() {
				public void preProcess() {
					List<Shape> shapes = new ArrayList<Shape>();
					FFMolecule mol = ((MoleculeCanvas) canvas).getMolecule();
					if(a1>=mol.getAllAtoms() || a2>=mol.getAllAtoms() || a3>=mol.getAllAtoms() || a4>=mol.getAllAtoms()) return;
					
					Coordinates c1 = GeometryCalculator.getCoordinates(mol, a1);
					Coordinates c2 = GeometryCalculator.getCoordinates(mol, a2);
					Coordinates c3 = GeometryCalculator.getCoordinates(mol, a3);
					Coordinates c4 = GeometryCalculator.getCoordinates(mol, a4);
							
					double angle = (180/Math.PI*c1.getDihedral(c2, c3, c4)); 
					Coordinates middle = new Coordinates((c1.x + c2.x + c3.x + c4.x)/4, (c1.y + c2.y + c3.y + c4.y)/4, (c1.z + c2.z + c3.z + c4.z)/4);
	  
					shapes.add(new Line(c1, c2, Color.green, Line.DOTTED_STROKE));	
					shapes.add(new Line(c2, c3, Color.green, Line.DOTTED_STROKE));	
					shapes.add(new Line(c1, c4, Color.green, Line.DOTTED_STROKE));	
					shapes.add(new Line(c3, c4, Color.green, Line.DOTTED_STROKE));		
					shapes.add(new Text(middle, new DecimalFormat("###.#").format(angle) +""));
					canvas.addShapes(shapes);
				}
			});				
			
			canvas.removePickedShapes();
		}
	}
}