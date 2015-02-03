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
 * Distance Measurement Tool 
 * @author freyssj
 */
public class ToolMeasureDistance extends ToolMeasure {
			
	public Coordinates[] getBounds() {
		return null;
	}
	
	public void callSub(IPickable shape, final Canvas3D canvas) {

		if(canvas.getPickedShapes().size()>=2) {	

			final int a1 = canvas.getPickedShapes().get(0).getValue();
			final int a2 = canvas.getPickedShapes().get(1).getValue();
			if(a1<0 || a2<0) return;
			canvas.addPaintProcessor(new PaintProcessor() {
				public void preProcess() {
					List<Shape> shapes = new ArrayList<Shape>();
					FFMolecule mol = ((MoleculeCanvas)canvas).getMolecule();
					if(a1>=mol.getAllAtoms() || a2>=mol.getAllAtoms()) return;
					Coordinates c1 = GeometryCalculator.getCoordinates(mol, a1);
					Coordinates c2 = GeometryCalculator.getCoordinates(mol, a2);
					shapes.add(new Line(c1, c2, Color.green, Line.DOTTED_STROKE));

					Coordinates middle = new Coordinates((c1.x + c2.x)/2, (c1.y + c2.y)/2, (c1.z + c2.z)/2); 
					shapes.add(new Text(middle, new DecimalFormat("#.###").format(Math.sqrt(c1.distSquareTo(c2))) +" A"));

					canvas.addShapes(shapes);			
				}

			});					
						
			canvas.removePickedShapes();
		}
	}
}