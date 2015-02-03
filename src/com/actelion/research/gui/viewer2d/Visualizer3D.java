package com.actelion.research.gui.viewer2d;

import javax.vecmath.*;

import com.actelion.research.chem.Coordinates;

/**
 * @author freyssj
 */
public class Visualizer3D{
	
	private static final double LOCATION = 8.0;
	private Matrix4d transform = new Matrix4d();
	private double zoomPercent = 100;
	private double cameraDistancedouble = 3;
	private Coordinates centerOfRotation = new Coordinates();
	
	protected double scalePixelsPerAngstrom = 200;
	protected Matrix3d rotation = new Matrix3d();
	protected double[] translation;
	protected static double screenZoom = 0.29;
	
	private Coordinates boundsMin, boundsMax;
	private int width = 100, height = 100;

	public Visualizer3D() {
		rotation.m00=1;
		rotation.m11=1;
		rotation.m22=1;
		translation = new double[]{0, 0, 0};
	}

	public Matrix3d getRotationMatrix() {
		return rotation;
	}

	/**
	 * Returns the Screen position (x,y) and depth of  Coordinates in the real world
	 * @param c
	 * @return
	 */
	public Point3i screenPosition(final Coordinates c) {
		//ROT = matrix*(c-CoR)+T
		//Zoom = L / L - (ROT[3]] 
		//Screen = [[W/2],[H/2], [0]] + ROT * [zoom, -zoom, 1] 
		double x = c.x;
		double y = c.y;
		double z = c.z;
		
		Point3d p = new Point3d();
		transform.transform(new Point3d(x,y,z), p);

		Point3i res = new Point3i(
		(int) (p.x + width / 2 + translation[0]),
		(int) (p.y + height / 2 + translation[1]),
		(int) (p.z));

		double perspectiveFactor = cameraDistancedouble / p.z;
		res.x *= perspectiveFactor;
		res.y *= perspectiveFactor;
		
		return res;
	}

	/**
	 * Return the real Coordinates (x,y,z) given screen coordinates (x,y) and its depth
	 * @param c
	 * @param z
	 * @return
	 */
	public Coordinates realPosition(final Point3i c) {
		Point3d t = new Point3d(c.x, c.y, c.z);
		double perspectiveFactor = cameraDistancedouble / c.z;
		t.x /= perspectiveFactor;
		t.y /= perspectiveFactor;
		
		t.x -= width / 2 + translation[0];
		t.y -= height / 2 + translation[1];
		
		//2. Inverse transform matrix
		Matrix4d mat = new Matrix4d();
		mat.invert(transform);			
		mat.transform(t);	
		
		//3. Get coordinates:  c = matrix^-1*(Rot-T) + CoR
		Coordinates res = new Coordinates(t.x, t.y, t.z); 
				
		return res;
	
	}
	
	public Coordinates realVector(Coordinates c) {
		//Coordinates c1 = new Coordinates(width, height, translation[2]);
		//Coordinates c2 = new Coordinates(width+c.x, height+c.y, translation[2]+c.z);		
		//Coordinates res = realPosition(c2).sub(realPosition(c1));
		//System.out.print(">"+res);
		Point3i p1 = new Point3i(
				(int)(width/2+c.x + translation[0]), 
				(int)(height/2+c.y + translation[1]), 
				(int)(cameraDistancedouble+c.z));
		Point3i p2 = new Point3i(
			(int)(width/2 + translation[0]), 
			(int)(height/2 + translation[1]), 
			(int)(cameraDistancedouble));				
		Coordinates res = realPosition(p1).subC(realPosition(p2));
		return res;
	}

	public int projectedDistance(double d, double z) {
		if(z<0) return 0;
		double pixelSize = d * scalePixelsPerAngstrom * 1.08; //why 1.08??
		return (int)((pixelSize * cameraDistancedouble) / z);
	}
	
	public double realDistance(int dist, double z) {
		if(z<0) return 0;
		double pixelSize = dist * z / cameraDistancedouble;
		return pixelSize / scalePixelsPerAngstrom / 1.08;
	}

	public void rotate(double angleX, double angleY) {
		Matrix3d t = new Matrix3d();
		t.rotX(angleY);		
		rotation.mul(t, rotation);
		t.rotY(angleX);		
		rotation.mul(t, rotation);
		calcTransformMatrix();		
	}
	
		
	public final void setCenterOfRotation(Coordinates c) {
		centerOfRotation = c;
		calcTransformMatrix();		
	}
	
	public final void setTranslation(Coordinates c) {
		translation = new double[] {c.x, c.y, c.z};
		//zoomPercent = c.z;
		//System.out.println(zoomPercent);
		calcTransformMatrix();
	}
	
	public final void translate(double x, double y, double z) {
		translation[0]+=x;
		translation[1]+=y;
		translation[2]+=z;
		calcTransformMatrix();
	}
	
	void zoomBy(int pixels) {
	  	if (pixels > 20) pixels = 20;
	  	else if (pixels < -20) pixels = -20;
	  	double deltaPercent = pixels * zoomPercent / 50;
	  	if (deltaPercent == 0) deltaPercent = (pixels > 0 ? 1 : (deltaPercent < 0 ? -1 : 0));
		zoomPercent = deltaPercent + zoomPercent;
		if(zoomPercent<.3) zoomPercent = .3;
		calcTransformMatrix();
	}
		
	public final void setBounds(Coordinates min, Coordinates max) {
		this.boundsMin = min;
		this.boundsMax = max;
	}
	
	/**
	 * Recenter the view, so that everything is on screen.
	 * SetBounds and SetScreenDimension should have been called before
	 */
	public void resetView() {
		translation = new double[]{0, 0, 0};
		rotation = new Matrix3d(1, 0, 0 , 0, 1, 0 , 0, 0, 1);
		setScreenDimension(width, height);
		
		zoomPercent = 150;
		//Go backward until we see everything
		Point3i c1, c2;
		do {
			zoomPercent *= .8;
			calcTransformMatrix();
			c1 = screenPosition(boundsMin);
			c2 = screenPosition(boundsMax); 
		} while(c1==null || c2==null || c1.x<0 || c1.x>width || c2.x<0 || c2.x>width || c1.y<0 || c1.y>height || c2.y<0 || c2.y>height);
		zoomPercent *= .6;
	
	}
	
	public void setScreenDimension(int width, int height) {
		screenZoom = 0.5 * Math.min(width, height);
		this.width = width;
		this.height = height;

		cameraDistancedouble = LOCATION * Math.min(width, height);
		calcTransformMatrix();
	}

	private void calcTransformMatrix() {
		scalePixelsPerAngstrom = screenZoom * zoomPercent / 100;
		Matrix4d matrixTemp = new Matrix4d();
		transform.setIdentity();

		// first, translate the coordinates back to the center
		matrixTemp.setZero();
		matrixTemp.setTranslation(new Vector3d(centerOfRotation.x, centerOfRotation.y, centerOfRotation.z) );
		transform.sub(matrixTemp);

		// now, multiply by angular rotations  this is *not* the same as  matrixTransform.mul(matrixRotate);
		matrixTemp.set(rotation);
		transform.mul(matrixTemp, transform);

		// we want all z coordinates >= 0, with larger coordinates further away
		// this is important for scaling, and is the way our zbuffer works
		// so first, translate an make all z coordinates negative
		Vector3d vectorTemp = new Vector3d();
		vectorTemp.x = 0; //translation[0]/500.0;
		vectorTemp.y = 0; //-translation[1]/500.0;
	    vectorTemp.z = cameraDistancedouble / scalePixelsPerAngstrom;// translation[2]/500.0;// + cameraDistancedouble / scalePixelsPerAngstrom;
		matrixTemp.setZero();
		matrixTemp.setTranslation(vectorTemp);

//		transform.add(matrixTemp, transform);  TLS 23Nov09: float version of this seems to delete the matrix before adding on android
		transform.add(matrixTemp); // make all z negative

		// now scale to screen coordinates
		matrixTemp.setZero();
		matrixTemp.set(scalePixelsPerAngstrom);

		transform.mul(matrixTemp, transform);

	}


	public Coordinates getCenterOfRotation() {
		return centerOfRotation;
	}


}
