package com.actelion.research.gui.viewer2d;

import java.awt.*;

import javax.vecmath.Point3i;


import com.actelion.research.chem.Coordinates;
import com.actelion.research.gui.viewer2d.jmol.*;

public class Line extends Shape {
	protected Color color;
	protected Coordinates realPoint2;
	private int style;

	public Line(Coordinates p1, Coordinates p2, Color c) {
		this(p1, p2, c, 0);
	}
	public Line(Coordinates p1, Coordinates p2, Color c, int style) {
		super(p1);
		this.color = c;
		this.realPoint2 = p2;
		this.style = style;
	}		
	public void paint(Canvas3D canvas3D, Graphics3D g) {
		super.paint(canvas3D, g);
		Point3i c2 = canvas3D.visualizer3D.screenPosition(realPoint2);
		if(screenCoordinates!=null && c2!=null) {			
			switch(style) {
				case BASIC_STROKE:
					g.drawLine(Colix.getColix(getAttenuatedColor(canvas3D, color)), screenCoordinates.x, screenCoordinates.y, screenCoordinates.z, c2.x, c2.y, c2.z);
					break;
				case DOTTED_STROKE:
					g.drawDottedLine(Colix.getColix(getAttenuatedColor(canvas3D, color)), screenCoordinates.x, screenCoordinates.y, screenCoordinates.z, c2.x, c2.y, c2.z);
					break;
				case DASHED_STROKE:
					g.drawDashedLine(Colix.getColix(getAttenuatedColor(canvas3D, color)), 1, 2, screenCoordinates.x, screenCoordinates.y, screenCoordinates.z, c2.x, c2.y, c2.z);
				case BOLD_STROKE:
					g.fillCylinder(Colix.getColix(getAttenuatedColor(canvas3D, color)), (byte)2, 10, screenCoordinates.x, screenCoordinates.y, screenCoordinates.z, c2.x, c2.y, c2.z);
			}
			
		} 		
	}
}