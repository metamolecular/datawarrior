/*
 * Created on Nov 10, 2004
 *
 */
package com.actelion.research.gui.viewer2d;


/**
 * 
 * @author freyssj
 */
public interface IPickable {

	public int getValue();
	public void setSelection(int sel);
	public int getSelection();
	public boolean isPickable();
		
}
