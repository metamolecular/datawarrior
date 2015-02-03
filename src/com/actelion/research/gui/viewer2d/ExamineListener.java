/*
 * Created on Dec 12, 2003
 */
package com.actelion.research.gui.viewer2d;

import java.awt.event.*;

/**
 * @author freyssj
 */
public class ExamineListener implements MouseListener, MouseMotionListener {
	
	private Visualizer3D v3d;
	private int mMouseX1, mMouseY1, mMouseX2, mMouseY2;
	private Canvas3D canvas3D;

	public ExamineListener(Visualizer3D v3d, Canvas3D canvas) {
		this.v3d = v3d;
		canvas3D = canvas;
	}


	public synchronized void mouseExited(MouseEvent e) {}
	public synchronized void mouseReleased(MouseEvent e) {
		if(canvas3D.getTool()!=null && canvas3D.getTool().mouseReleased(e, canvas3D)) return;				
	}
	public synchronized void mouseEntered(MouseEvent e) {}
	public synchronized void mousePressed(MouseEvent e) {
		if(canvas3D.getTool()!=null && canvas3D.getTool().mousePressed(e, canvas3D)) return;			
		mMouseX1 = e.getX();
		mMouseY1 = e.getY();
	}
	
	
	public synchronized void mouseClicked(MouseEvent e) {
		if(canvas3D.getTool()!=null) {
			canvas3D.getTool().mouseClicked(e, canvas3D);			
		}
		
		//check if a shape has been selected
//		PickableShape shape = canvas3D.getPickableShapeAt(e.getX(), e.getY());
//		if(shape!=null) {
			//canvas3D.setSelection(shape.getPickListener().getBounds());
		//} else {
		//	canvas3D.setSelection(null);
		//}
		//canvas3D.repaint();
	}
	
	IPickable shape = null;
	public synchronized void mouseMoved(MouseEvent e) {
		IPickable newShape;
		if(canvas3D.getTool()!=null) {
			newShape = canvas3D.getPickableShapeAt(e.getX(), e.getY(), canvas3D.getTool().getPickableClass());
		} else {
			newShape = null;				
		}
		if(shape==newShape) {
			//Nothing
		} else if(canvas3D.getPickedShapes().contains(shape)) {
			shape = newShape;
		} else {
			if(shape!=null) shape.setSelection(shape.getSelection() & ~1);
			if(newShape!=null) newShape.setSelection(newShape.getSelection() | 1);
			shape = newShape;
			canvas3D.repaint();
		}
	}	
	
	public synchronized void mouseDragged(MouseEvent e) {
		mouseMoved(e);
		if(canvas3D.getTool()!=null && canvas3D.getTool().mouseDragged(e, canvas3D)) return;			
		
		mMouseX2 = e.getX();
		mMouseY2 = e.getY();

		int dx = mMouseX2 - mMouseX1;
		int dy = mMouseY2 - mMouseY1;
		
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK)>0) {
			v3d.translation[0] += dx*2;
			v3d.translation[1] += dy*2;
		}
		if ((e.getModifiers() & InputEvent.BUTTON3_MASK)>0) {
			//v3d.translation[2] += dy*.5;
			v3d.zoomBy(-dy);
		} 
		if ((e.getModifiers() & InputEvent.BUTTON1_MASK) >0) {
			double angleX = -dx / Visualizer3D.screenZoom;
			double angleY = dy / Visualizer3D.screenZoom;
			v3d.rotate(angleX, angleY);			
		}
		mMouseX1 = mMouseX2;
		mMouseY1 = mMouseY2;

		canvas3D.repaint();
	}


}
