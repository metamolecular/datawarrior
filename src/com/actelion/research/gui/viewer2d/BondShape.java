/*
 * Created on Dec 15, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.actelion.research.gui.viewer2d;

import java.awt.Color;

import javax.vecmath.Point3i;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.GeometryCalculator;
import com.actelion.research.gui.viewer2d.jmol.Colix;
import com.actelion.research.gui.viewer2d.jmol.Graphics3D;


public class BondShape extends Shape implements IPickable {
	
	private int bond;
	private int value;
	private AtomShape a1, a2;
	private int selection;
	private FFMolecule mol;
	protected double radius;
	
	public BondShape(FFMolecule mol, int bondNo, AtomShape a1, AtomShape a2, int value) {
		super();
		this.a1 = a1;
		this.a2 = a2;
		this.mol = mol;
		this.bond = bondNo;
		this.value = value;		
		this.order = mol.getBondOrder(bond);
		if(order==0) throw new IllegalArgumentException("BOnd of length 0 "+a1.getAtm()+"-"+a2.getAtm());
		realCoordinates = GeometryCalculator.getCoordinates(mol, a1.atm).addC(GeometryCalculator.getCoordinates(mol, a2.atm)).scaleC(.5);
	}
	
	/*
	 * @see com.actelion.research.gui.viewer2d.Shape#getScreenCoordinates()
	public Point3i getScreenCoordinates() {
		screenCoordinates = a1.screenCoordinates==null?null :
			a2.screenCoordinates==null? null:
			new Point3i((a1.screenCoordinates.x+a2.screenCoordinates.x)/2,
				(a1.screenCoordinates.y+a2.screenCoordinates.y)/2,
				(a1.screenCoordinates.z+a2.screenCoordinates.z)/2);
		return screenCoordinates;
	}
	 */

	
	private int w, order;
	public void paint(Canvas3D canvas3D, Graphics3D g3d) {
		Point3i c1 = a1.screenCoordinates;
		Point3i c2 = a2.screenCoordinates;

		this.radius = AtomShape.BOND_SIZES[a1.getStyle()]/1000.0;
		if(radius<0 || c1==null || c2==null) return;
			
		
		if(order<=0) return;
		int atm = a1.atm;
		int atm2 = a2.atm;
			
		if(a1.diameter>500 || a2.diameter>500 || screenCoordinates==null) return;		
		diameter = canvas3D.visualizer3D.projectedDistance(2*radius, screenCoordinates.z);

		//diameter+=style*4;
		
		if(selection>0) {
			g3d.fillCylinder(Colix.getColix((selection&1)>0?Color.yellow: Color.green), (byte) 0, w+3, c1, c2 );
		} 

		w = diameter / order;
		
		Color col1 = a1.renderColor;
		Color col2 = a2.renderColor;
		
		
		if(order>1) {
			//Establish a normal vector if the bond order is greater than 1			
			Coordinates u = a2.realCoordinates.subC(a1.realCoordinates);

			Coordinates normal = null;
			for(int j=0; normal==null && j<mol.getAllConnAtoms(atm); j++) {
				if(mol.getConnAtom(atm, j)!=atm2) {
					Coordinates v =  a1.realCoordinates.subC(GeometryCalculator.getCoordinates(mol, mol.getConnAtom(atm,j)));
					normal = u.cross(v).cross(u);
				} 
			}
			for(int j=0; normal==null && j<mol.getAllConnAtoms(atm2); j++) {
				if(mol.getConnAtom(atm2, j)!=atm) {
					Coordinates v =  a2.realCoordinates.subC(GeometryCalculator.getCoordinates(mol, mol.getConnAtom(atm2,j)));
					normal = u.cross(v).cross(u);
				} 
			}
			if(normal==null) normal = u.cross(new Coordinates(1,0,0));				
			if(normal.distSq()==0) normal = new Coordinates(0,0,1);

			if(order==2) { 
				normal = normal.unit().scaleC(0.15);
				Point3i cc1 = canvas3D.visualizer3D.screenPosition(a1.realCoordinates.addC(normal)); 
				Point3i cc2 = canvas3D.visualizer3D.screenPosition(a2.realCoordinates.addC(normal)); 
				fillCylinder(g3d, Colix.getColix(col1), Colix.getColix(col2), 
					w, cc1, cc2 );
					
				cc1 = canvas3D.visualizer3D.screenPosition(a1.realCoordinates.subC(normal));
				cc2 = canvas3D.visualizer3D.screenPosition(a2.realCoordinates.subC(normal));
				fillCylinder(g3d, Colix.getColix(col1), Colix.getColix(col2), 
					w, cc1, cc2 );
			} else {
				normal = normal.unit().scaleC(0.17);
				Point3i cc1 = canvas3D.visualizer3D.screenPosition(a1.realCoordinates.addC(normal)); 
				Point3i cc2 = canvas3D.visualizer3D.screenPosition(a2.realCoordinates.addC(normal)); 
				fillCylinder(g3d, Colix.getColix(col1), Colix.getColix(col2), 
					w, cc1, cc2 );
					
				cc1 = canvas3D.visualizer3D.screenPosition(a1.realCoordinates.subC(normal));
				cc2 = canvas3D.visualizer3D.screenPosition(a2.realCoordinates.subC(normal));
				fillCylinder(g3d, Colix.getColix(col1), Colix.getColix(col2), 
					w, cc1, cc2 );

				cc1 = new Point3i(c1.x, c1.y, c1.z);
				cc2 = new Point3i(c2.x, c2.y, c2.z);
				fillCylinder(g3d, Colix.getColix(col1), Colix.getColix(col2), 
					w, cc1, cc2 );
			}	
		} else {				
			Point3i cc1 = c1;
			Point3i cc2 = c2;
			fillCylinder(g3d, Colix.getColix(col1), Colix.getColix(col2), 
				 w, cc1, cc2 );

		}				
					
	}
	
	private void fillCylinder(Graphics3D g3d, short colix1, short colix2, int w, Point3i p1, Point3i p2) {
		if(a1.getStyle()==AtomShape.OUTLINE ) {
			Coordinates normal = new Coordinates(0,0,1);
			Coordinates cu = new Coordinates(p1.x-p2.x, p1.y-p2.y, p1.z-p2.z).cross(normal);
			if(cu.distSq()==0) return;
			cu = cu.unit().scaleC(w/2);
			Point3i u = new Point3i((int)cu.x, (int)cu.y, 0);
			
			g3d.drawLine(colix1, p1.x-u.x, p1.y-u.y, p1.z, p1.x+u.x, p1.y+u.y, p2.z);			
			g3d.drawLine(colix1, colix2, p1.x-u.x, p1.y-u.y, p1.z, p2.x-u.x, p2.y-u.y, p2.z);			
			g3d.drawLine(colix1, colix2, p1.x+u.x, p1.y+u.y, p1.z, p2.x+u.x, p2.y+u.y, p2.z);			
			g3d.drawLine(colix2, p2.x-u.x, p2.y-u.y, p2.z, p2.x+u.x, p2.y+u.y, p2.z);			
			
		} else {
			g3d.fillCylinder(colix1, colix2, (byte) 3, w, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z);
		}
		
	}

	/**
	 * @see com.actelion.research.gui.viewer2d.IPickable#getValue()
	 */
	public int getValue() {
		return value;
	}

	/*
	private Polygon getPolygon() {
		Point3i c1 = a1.screenCoordinates;
		Point3i c2 = a2.screenCoordinates;
		int xp1, yp1, xp2, yp2;
		double d = Math.sqrt((c1.x-c2.x)*(c1.x-c2.x) + (c1.y-c2.y)*(c1.y-c2.y) + (c1.z-c2.z)*(c1.z-c2.z));
		if (a1.d > 0 && d>0) {
			xp1 = (int)((c2.x - c1.x) * a1.d / d + c1.x);
			yp1 = (int)((c2.y - c1.y) * a1.d / d  + c1.y);
		} else {
			xp1 = (int)c1.x;
			yp1 = (int)c1.y;
		}
		if (a2.d > 0 && d>0) {
			xp2 = (int)((c1.x - c2.x) * a2.d / d  + c2.x);
			yp2 = (int)((c1.y - c2.y) * a2.d / d  + c2.y);
		} else {
			xp2 = (int)c2.x;
			yp2 = (int)c2.y;
		}
		
		double nx = Math.abs(xp2-xp1)<=1? 0: 1; 
		double ny = Math.abs(xp2-xp1)<=1? 1: Math.abs(1.0*(yp2-yp1)/(xp2-xp1));		

		double norm = Math.sqrt(1 + ny*ny);
		nx=nx/norm*(w+8)*order;
		ny=ny/norm*(w+8)*order;

		if(nx<=2) nx = 2; 
		if(ny<=2) ny = 2; 

System.out.println(nx+" "+ny);
		Polygon p = new Polygon(
		new int[]{(int)(xp1-nx), (int)(xp1+nx), (int)(xp2+nx), (int)(xp2-nx)},
		new int[]{(int)(yp1-ny), (int)(yp1+ny), (int)(yp2+ny), (int)(yp2-ny)},
		4
		);
		return p;		
	}*/
	/*
	public boolean contains(int x, int y) {
		if(a1.screenCoordinates==null || a2.screenCoordinates==null) return false; //Not on screen
		System.out.println(getPolygon().contains(x, y));
		
		
		return getPolygon().contains(x, y);
	}
	
	*/
	
	/**
	 * @see com.actelion.research.gui.viewer2d.IPickable#isPickable()
	 */
	public boolean isPickable() {
		return value>=0;
	}

	public void setSelection(int sel) {
		selection = sel;
		
	}

	public int getSelection() {
		return selection;
	}

}