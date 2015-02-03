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
 * Angle Measurement Tool 
 * @author freyssj
 */
public final class ToolMeasureAngle extends ToolMeasure {
	
	public Coordinates[] getBounds() {
		return null;
	}
	
	public void callSub(IPickable shape, final Canvas3D canvas) {

		if(canvas.getPickedShapes().size()>=3) {				

			final int a1 = canvas.getPickedShapes().get(1).getValue();
			final int a2 = canvas.getPickedShapes().get(0).getValue();
			final int a3 = canvas.getPickedShapes().get(2).getValue();
			if(a1<0 || a2<0 || a3<0) return;

			canvas.addPaintProcessor(new PaintProcessor() {
				public void preProcess() {
					List<Shape> shapes = new ArrayList<Shape>();
					FFMolecule mol = ((MoleculeCanvas)canvas).getMolecule();
					if(a1>=mol.getAllAtoms() || a2>=mol.getAllAtoms() || a3>=mol.getAllAtoms()) return;
					
					Coordinates c1 = GeometryCalculator.getCoordinates(mol, a1);
					Coordinates c2 = GeometryCalculator.getCoordinates(mol, a2);
					Coordinates c3 = GeometryCalculator.getCoordinates(mol, a3);
			
					Coordinates cc2 = new Coordinates((3*c1.x + c2.x)/4, (3*c1.y + c2.y)/4, (3*c1.z + c2.z)/4);
					Coordinates cc3 = new Coordinates((3*c1.x + c3.x)/4, (3*c1.y + c3.y)/4, (3*c1.z + c3.z)/4);
				
			
					shapes.add(new Arc(c1, cc2, cc3, Color.gray));
					
					Coordinates middle = new Coordinates((6*c1.x + c2.x + c3.x)/8, (6*c1.y + c2.y + c3.y)/8, (6*c1.z + c2.z + c3.z)/8);
					double angle = (180/Math.PI*c2.subC(c1).getAngle(c3.subC(c1))); 
		
					shapes.add(new Line(c1, c2, Color.green, Line.DOTTED_STROKE));
					shapes.add(new Line(c1, c3, Color.green, Line.DOTTED_STROKE));
					shapes.add(new Text(middle, new DecimalFormat("###.#").format(angle) +""));
					canvas.addShapes(shapes);
				}
				
			});				
			
			canvas.removePickedShapes();
		}
	}
}