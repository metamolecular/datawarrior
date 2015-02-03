package com.actelion.research.gui.viewer2d;

import java.awt.event.MouseEvent;


/**
 * Abstract Tool used to manipulate the Canvas
 * 
 * @author freyssj
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractTool {

	public Class getPickableClass() {return IPickable.class;}

	public boolean mouseClicked(MouseEvent e, Canvas3D canvas) {return false;}
	public boolean mouseDragged(MouseEvent e, Canvas3D canvas) {return false;}
	public boolean mousePressed(MouseEvent e, Canvas3D canvas) {return false;}
	public boolean mouseReleased(MouseEvent e, Canvas3D canvas) {return false;}

	public static void unselectAll(Canvas3D canvas) {
		for (Shape element: canvas.getShapes()) {
			if(element instanceof IPickable) {
				((IPickable) element).setSelection(((IPickable) element).getSelection()&~1);
			}			
		}
	}
}
