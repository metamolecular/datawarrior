package com.actelion.research.gui.viewer2d;

import java.awt.Color;

import javax.vecmath.Point3i;


import com.actelion.research.chem.Coordinates;
import com.actelion.research.gui.viewer2d.jmol.Graphics3D;

@SuppressWarnings("rawtypes")
public abstract class Shape implements Comparable {
	public static final int BASIC_STROKE = 0;
	public static final int DOTTED_STROKE = 1;		
	public static final int DASHED_STROKE = 2;		
	public static final int BOLD_STROKE = 3;		

	protected Coordinates realCoordinates;
	public Point3i screenCoordinates;
	protected int diameter;
	protected AbstractTool pickListener;
	
	public Shape() {}
	public Shape(Coordinates center) {
		realCoordinates = center;
	}
	public void paint( Canvas3D canvas3D, Graphics3D g) {
		if(screenCoordinates==null) return;
	}
	
	/**
	 * Checks if the shape contains the point at screen coordinates x,y
	 */
	public boolean contains(int x, int y) {
		return false;
	}
	
	public Point3i getScreenCoordinates() {
		return screenCoordinates; 
	}
	
	public Coordinates getRealCoordinates() {
		return realCoordinates;
	}

	/**
	 * Comparator used to compare Shape according to their screen depth
	 */
	public int compareTo(Object o) {
		Shape s = (Shape) o;
		if(getScreenCoordinates()==null) return 1;
		else if(s.getScreenCoordinates()==null) return -1;
		else return s.getScreenCoordinates().z - getScreenCoordinates().z>0?1:-1;
	}
	
	protected final Color getAttenuatedColor(Canvas3D canvas3D, Color c) {
		double min = canvas3D.minZ;
		double max = canvas3D.maxZ;
		double med = (max+2*min)/3;
		Color background = canvas3D.background;
		
		int alpha;
		if(getScreenCoordinates().z>med) {
			alpha = 100-(int)(120*(getScreenCoordinates().z-med)/(max-med));
		} else {
			alpha = 100+(int)(50*(getScreenCoordinates().z-med)/(min-med));
		}
		
		if(alpha<0) alpha = 0;
		if(alpha>200) alpha = 200;
		return attenuateColor(c, background, alpha);
	}

	protected static final Color attenuateColor(Color c, Color b, int alpha) {
		if(alpha<100) return new Color(alpha*(c.getRed()-b.getRed())/100 + b.getRed(), alpha*(c.getGreen()-b.getGreen())/100+b.getGreen(), alpha*(c.getBlue()-b.getBlue())/100+b.getBlue());		
		else return new Color( (255-c.getRed())*(alpha-100)/100+c.getRed(), (255-c.getGreen())*(alpha-100)/100+c.getGreen(), (255-c.getBlue())*(alpha-100)/100+c.getBlue());
	}
	
}