/*
 * Created on Nov 9, 2004
 *
 */
package com.actelion.research.gui.viewer2d;

import java.util.*;

/**
 * 
 * @author freyssj
 */
public class MoleculeShapes extends ArrayList<Shape> {
	private final Canvas3D canvas3D;
	
	protected MoleculeShapes(Canvas3D canvas) {
		this.canvas3D = canvas;
	}
	
	void transform() {
		synchronized(this) {
			for (Shape element: this) {
				if( (element instanceof AtomShape)) {
					if(element.screenCoordinates==null) continue;
					AtomShape s = (AtomShape) element;
					s.renderColor = s.getAttenuatedColor(canvas3D, s.color);
				}
			}
		}
	}
}
