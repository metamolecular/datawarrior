/*
 * Created on Sep 27, 2004
 *
 */
package com.actelion.research.gui.viewer2d;

import java.awt.event.MouseEvent;

/**
 * 
 * @author freyssj
 */
public abstract class ToolMeasure extends AbstractTool {
	
	@SuppressWarnings("rawtypes")
	public Class getPickableClass() {
		return AtomShape.class;
	}
	
	/**
	 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
	 */
	public synchronized boolean mouseClicked(MouseEvent e, Canvas3D canvas) {
		if((e.getModifiers()&MouseEvent.BUTTON1_MASK)==0) return false; 
		IPickable shape = canvas.getPickableShapeAt(e.getX(), e.getY(), AtomShape.class);
		if(shape==null) return false;
		int index = canvas.getPickedShapes().indexOf(shape); 
		if(index>=0) {
			canvas.getPickedShapes().remove(index);
			shape.setSelection(shape.getSelection() & ~1);
			canvas.repaint();
		} else {
			canvas.getPickedShapes().add(shape);
			shape.setSelection(shape.getSelection() | 1);
			callSub(shape, canvas);
			canvas.repaint();
		}
		return true;
	}

	public abstract void callSub(IPickable shape, Canvas3D canvas);
	
	
}
