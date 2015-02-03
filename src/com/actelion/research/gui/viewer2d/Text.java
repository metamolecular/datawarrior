package com.actelion.research.gui.viewer2d;

import java.awt.*;


import com.actelion.research.chem.Coordinates;
import com.actelion.research.gui.viewer2d.jmol.*;

/**
 * @author freyssj
 */
public class Text extends Shape {
	
	private int size;
	private String text;
	protected Color color;
	
	public Text(Coordinates center, String text) {
		this(center, text, 9, Color.green);
	}

	public Text(Coordinates center, String text, int size, Color c) {
		super(center);
		this.color = c;
		this.text = text;
		this.size = size;
	}
	
	public void paint(Canvas3D canvas3D, Graphics3D g) {
		super.paint(canvas3D, g);
		if(screenCoordinates==null) return;
		
		int newSize = canvas3D.getVisualizer3D().projectedDistance((size-4)/20.0, screenCoordinates.z);
		if(newSize<3) return; 
		
		g.setFont(g.getFont3D(newSize));
		g.drawString(text, Colix.getColix(getAttenuatedColor(canvas3D, color)), screenCoordinates.x, screenCoordinates.y, screenCoordinates.z);		
	}

}
