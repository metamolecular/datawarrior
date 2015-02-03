/*
 * Created on Dec 29, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.actelion.research.gui.viewer2d;

import java.awt.*;

import javax.vecmath.Point3i;


import com.actelion.research.chem.Coordinates;
import com.actelion.research.gui.viewer2d.jmol.*;

/**
 * @author freyssj
 */
public class Arc extends Shape {
	protected Color color;
	private Coordinates realPoint1, realPoint2;

	public Arc(Coordinates center, Coordinates c1, Coordinates c2, Color c) {
		super(center);
		this.color = c;
		this.realPoint1 = c1;
		this.realPoint2 = c2;
	}
	
	

	public void paint(Canvas3D canvas3D, Graphics3D g) {
		super.paint(canvas3D, g);
		
		
		if(screenCoordinates==null) return;
		
		Point3i screenCoordinates1 = canvas3D.visualizer3D.screenPosition(realPoint1);
		Point3i screenCoordinates2 = canvas3D.visualizer3D.screenPosition(realPoint2);
		if(screenCoordinates1==null || screenCoordinates2==null) return;

		if(screenCoordinates1.x==screenCoordinates.x || screenCoordinates2.x==screenCoordinates.x) return;
		double startAngle = -180.0/Math.PI * Math.atan((screenCoordinates1.y-screenCoordinates.y) / (screenCoordinates1.x-screenCoordinates.x));
		double endAngle = -180.0/Math.PI * Math.atan((screenCoordinates2.y-screenCoordinates.y) / (screenCoordinates2.x-screenCoordinates.x));
		if(screenCoordinates1.x-screenCoordinates.x<0) startAngle = 180 + startAngle;
		if(screenCoordinates2.x-screenCoordinates.x<0) endAngle = 180 + endAngle;
		
		g.fillTriangle(Colix.getColix(getAttenuatedColor(canvas3D, color)), screenCoordinates1, screenCoordinates2, screenCoordinates);
		//g.drawArc((int)(screenCoordinates.x - width), (int)(screenCoordinates.y - height), (int)(2*width), (int)(2*height), (int)startAngle, diff);
		
	}

}
