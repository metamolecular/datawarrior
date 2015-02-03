package com.actelion.research.gui.viewer2d;

import java.awt.Color;

import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.GeometryCalculator;
import com.actelion.research.chem.conf.VDWRadii;
import com.actelion.research.gui.viewer2d.jmol.Colix;
import com.actelion.research.gui.viewer2d.jmol.Graphics3D;

/**
 * An AtomShape is used to render an atom.
 * 
 * @author freyssj
 */
public class AtomShape extends Shape implements IPickable {
	
	public static final int OUTLINE = 0;
	public static final int WIREFRAME = 1;
	public static final int BALLSTICKS = 2; 
	public static final int STICKS = 3; 
	public static final int BALLS = 4; 
	public static final int[] ATOM_SIZES = new int[]{20, 0, 20, 0, 100};
	public static final int[] BOND_SIZES = new int[]{150, 0, 150, 200, -1};
	
	protected Color color;
	protected Color renderColor;
	protected float radius;
	protected int outlineD;
	protected int atm;
	protected int value;
	private int style;
	private int selection;
	private boolean isolated;
	private Color outlineColor;
	private int outlineRadius = 0;
	private FFMolecule mol;

	public AtomShape(FFMolecule mol, int atm, int style, int value) {
		super();
		this.atm = atm;
		this.value = value;
		color = MoleculeCanvas.getColor(mol.getAtomicNo(atm));
		
		setStyle(style);
		
		this.isolated = mol.getAllConnAtoms(atm)==0;
		this.mol = mol;
		realCoordinates = GeometryCalculator.getCoordinates(mol, atm);
		this.radius = (mol.getAtomicNo(atm)<VDWRadii.VDW_RADIUS.length?VDWRadii.VDW_RADIUS[mol.getAtomicNo(atm)]:2f) * ATOM_SIZES[style] / 100;		
	}
			
	public void paint(Canvas3D canvas3D, Graphics3D g3d) {
		if(canvas3D==null || canvas3D.visualizer3D==null || !isolated && (radius<0 || screenCoordinates==null)) return;
		this.diameter = canvas3D.visualizer3D.projectedDistance(2*radius, screenCoordinates.z);
				
		if(diameter>500) return;
		if(selection>0) {
			g3d.fillCircleCentered(Colix.getColix(getAttenuatedColor(canvas3D, (selection&1)>0?Color.yellow:Color.green)), (diameter+diameter/10+2), screenCoordinates.x, screenCoordinates.y, screenCoordinates.z);
		} 	
		if(outlineRadius>0) {
			g3d.fillCircleCentered(Colix.getColix(outlineColor), (diameter+outlineRadius*2), screenCoordinates.x, screenCoordinates.y, screenCoordinates.z);
		} 	
		if(isolated) {
			if(diameter<=1) diameter = canvas3D.visualizer3D.projectedDistance(VDWRadii.VDW_RADIUS[mol.getAtomicNo(atm)]*.4, screenCoordinates.z)+1;
		} else if(diameter<=1) return;

		//d=30;
		//System.out.println(screenCoordinates+" "+realCoordinates);
		if(style==OUTLINE) {
			g3d.drawCircleCentered(Colix.getColix(super.getAttenuatedColor(canvas3D, color)), diameter, screenCoordinates.x, screenCoordinates.y, screenCoordinates.z);			
			g3d.fillCircleCentered(Colix.getColix(Color.black), diameter-3, screenCoordinates.x, screenCoordinates.y, screenCoordinates.z);			
		} else {
			g3d.fillSphereCentered(Colix.getColix(super.getAttenuatedColor(canvas3D, color)), diameter, screenCoordinates.x, screenCoordinates.y, screenCoordinates.z);
		}

	}

	/**
	 * @see com.actelion.research.gui.viewer2d.IPickable#setSelection(boolean)
	 */
	public void setSelection(int v) {
		selection = v;		
	}

		
	/**
	 * @see com.actelion.research.gui.viewer2d.IPickable#contains(int, int)
	 
	public boolean contains(int x, int y) {
		if(screenCoordinates==null) return false;
		int cx = screenCoordinates.x;
		int cy = screenCoordinates.y;
		return (cx-x)*(cx-x)+(cy-y)*(cy-y)<(diameter/2+3)*(diameter/2+3);
	}
*/
	/**
	 * @see com.actelion.research.gui.viewer2d.IPickable#getValue()
	 */
	public int getValue() {
		return value;
	}

	
	/**
	 * @return
	 */
	public int getAtm() {
		return atm;
	}

	/**
	 * @return
	 */
	public int getStyle() {
		return style;
	}

	/**
	 * @param color
	 */
	public void setColor(Color color) {
		this.color = color;
	}

	/**
	 * @param f
	 */
	public void setRadius(float f) {
		radius = f;
	}

	/**
	 * @param i
	 */
	public void setStyle(int i) {
		style = i;
		if(mol!=null)this.radius = (mol.getAtomicNo(atm)<VDWRadii.VDW_RADIUS.length? VDWRadii.VDW_RADIUS[mol.getAtomicNo(atm)]:2f) * ATOM_SIZES[style] / 100;		
	}
    
    public void setOutline(Color color, int radius) {
 		this.outlineColor = color;
 		this.outlineRadius = radius;   	
    }

	/**
	 * @see com.actelion.research.gui.viewer2d.IPickable#isPickable()
	 */
	public boolean isPickable() {
		return value>=0;
	}
	

	public int getSelection() {
		return selection;
	}
    
	public boolean equals(Object obj) {
		return obj instanceof AtomShape && ((AtomShape)obj).getValue()==getValue();
	}
}