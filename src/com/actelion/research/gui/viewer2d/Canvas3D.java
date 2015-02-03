/*
 * Created on Dec 9, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.actelion.research.gui.viewer2d;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import com.actelion.research.gui.viewer2d.jmol.Colix;
import com.actelion.research.gui.viewer2d.jmol.Graphics3D;


/**
 * @author freyssj
 */
public class Canvas3D extends JPanel {

	private static final long serialVersionUID = 6610805441313812163L;
	
	protected AbstractTool pickingTool = null;
	private final ArrayList<Shape> shapes = new ArrayList<Shape>();
	protected final Visualizer3D visualizer3D = new Visualizer3D();
	protected Image buffer;
	protected final Graphics3D g3d = new Graphics3D(this);
	
	private ExamineListener examineListener;
	private boolean highQuality = false;
	protected Color background = Color.black;
	protected double minZ, maxZ;
	
	/** The List of PaintProcessors that have to be called 
	 * before and after rendering the scene */
	protected List<PaintProcessor> paintProcessors = new ArrayList<PaintProcessor>();
	protected boolean processedOk = false;

	/** The List of shapes that have been picked */
	private List<IPickable> pickedShaped = new ArrayList<IPickable>();
	
	public Canvas3D() {
		examineListener = new ExamineListener(visualizer3D, this);
		addMouseListener(examineListener);
		addMouseMotionListener(examineListener);
	}
	
	/**
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent(Graphics gr) {
		Dimension dim = getSize();

		//Create Buffer
		if(buffer==null || buffer.getHeight(null)!=dim.getHeight()
			|| buffer.getWidth(null)!=dim.getWidth()) {
				buffer = createImage(getWidth(), getHeight());
				if(buffer==null) {
					//This may happen if the component is not visible
					buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
				}				
		}	
		Graphics2D g = (Graphics2D) buffer.getGraphics();
		paint(g, dim.width, dim.height);
		paintBorder(g);
		g.dispose();
		
		//Refresh the buffer
		gr.drawImage(buffer, 0, 0, null);	
		gr.dispose();		
	}
	
	/**
	 * Paint the Canvas in an Image
	 * @param g
	 * @param buffer
	 * @param width
	 * @param height
	 */
	public final void paint(Graphics2D g, int width, int height) {	
		if(highQuality) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		}		
		background = Color.black;
		String stereo = getStereoMode();
		
		if("parallel".equals(stereo)) {			
			final double d = 3*Math.PI/180;
			visualizer3D.rotate(-d, 0);
			visualizer3D.setScreenDimension(width/2, height);
			g.setClip(0,0,width/2, height);
			paintShapes(g);
			visualizer3D.rotate(d, 0);
					
			visualizer3D.rotate(d, 0);
			visualizer3D.setScreenDimension(width/2, height);
			g.setClip(width/2,0,width/2, height);
			g.translate(width/2,0);
			paintShapes(g);
			g.translate(-width/2,0);
			g.setClip(null);
			visualizer3D.rotate(-d, 0);
			
		} else if("interlaced".equals(stereo)) {			
			background = new Color(100,100,150);
			//Point p = getLocationOnScreen();
			boolean even = true;//p.x%2==0;
			//Creates buffer
			BufferedImage buf = new BufferedImage(width*2, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = (Graphics2D) buf.getGraphics();
			final double d = 1.8*Math.PI/180;
			final double t = -100000000;
			visualizer3D.getCenterOfRotation().z+=t;
			visualizer3D.rotate(-d, 0);
			visualizer3D.getCenterOfRotation().z-=t;
			visualizer3D.setScreenDimension(width, height);
			paintShapes(g2);
			visualizer3D.getCenterOfRotation().z+=t;
			visualizer3D.rotate(d, 0);
			visualizer3D.getCenterOfRotation().z-=t;
			visualizer3D.getCenterOfRotation().z+=t;
			visualizer3D.rotate(d, 0);
			visualizer3D.getCenterOfRotation().z-=t;
			visualizer3D.setScreenDimension(width, height);
			g2.translate(width,0);
			paintShapes(g2);
			g2.translate(-width,0);
			visualizer3D.getCenterOfRotation().z+=t;
			visualizer3D.rotate(-d, 0);
			visualizer3D.getCenterOfRotation().z-=t;
			g2.dispose();

            //Interlace in g
			int width2 = 2 * width;
			//int[] rgb = (int[]) buf.getRaster().getDataElements(0, 0, width2, height, new int[width2*height]);
			DataBufferInt b = (DataBufferInt) buf.getRaster().getDataBuffer();
			int[] rgb = b.getData();
			//System.out.println(b.);
			//int[] rgb = (int[]) .;
			//buf.getRGB(0, 0, width2, height, rgb, 0, width2);
			final int dev = 5;
			for (int x = 0; x+1 < width-dev; x+=2) {
				{
					int i = x, j = x;
					for (int y = 0; y < height; y++) {
						rgb[i] = ((rgb[j] & 0xFEFEFEFE) >> 1) + ((rgb[j+1] & 0xFEFEFEFE) >> 1);
						i+=width2;
						j+=width2;
					}
				}
				{
					int i = x+1, j = width+x+dev;
					for (int y = 0; y < height; y++) {
						rgb[i] = ((rgb[j] & 0xFEFEFEFE) >> 1) + ((rgb[j+1] & 0xFEFEFEFE) >> 1);
						i+=width2;
						j+=width2;
					}
				}
			}
            //Arrays.fill(rgb, 0xFF);
			//buf.getRaster().setDataElements(0, 0, width2, height, rgb);
			//buf.setRGB(0, 0, width2, height, rgb, 0, width2);
			
			g.drawImage(buf, even?0:1, 0, width, height, 0, 0, width-(even?0:1), height, this);
		} else {
			visualizer3D.setScreenDimension(width, height);			
			paintShapes(g);
		}

	}
	
	public void preProcess() {
		//postProcess();
		if(processedOk) return;
		processedOk = true; 
		for(int i=0; i<paintProcessors.size(); i++) {
			paintProcessors.get(i).preProcess();
		}				
	}		


	protected void calculateScreenCoordinates() {
		//Calculate coordinates
		minZ = Double.MAX_VALUE;
		maxZ = -minZ;
		synchronized(shapes) {
			for (Shape shape: shapes) {
				if(shape==null || shape.realCoordinates==null) continue;
				shape.screenCoordinates = visualizer3D.screenPosition(shape.realCoordinates);
		
				if(shape.screenCoordinates!=null) {
					minZ = Math.min(minZ, shape.screenCoordinates.z); 
					maxZ = Math.max(maxZ, shape.screenCoordinates.z);
				}
			}
		}
		minZ-=visualizer3D.scalePixelsPerAngstrom*2;
		maxZ+=visualizer3D.scalePixelsPerAngstrom*4;
	}
	
	public void paintShapes(final Graphics2D g) {
		
		//Preprocess
		preProcess();
		
		calculateScreenCoordinates();
		
		g3d.setSlabAndDepthValues((int)minZ, (int)maxZ);	
		g3d.setSize(getSize(), highQuality);
		g3d.setBackground(Colix.getColix(background));
		g3d.beginRendering(null, highQuality);
		synchronized (shapes) {			
			for (Shape shape: shapes) {
				shape.paint(Canvas3D.this, g3d);			 
			}
		}
		
		g3d.endRendering();
		Image img = g3d.getScreenImage();
		g.drawImage(img, 0, 0, getWidth(), getHeight(), Canvas3D.this);
	}
	

	
	public void update(Graphics g) {		
		paint(g);
	}
	
	public BufferedImage getBufferedImage() {
		BufferedImage image = new BufferedImage(Math.max(100, getWidth()), Math.max(100, getHeight()), BufferedImage.TYPE_INT_RGB);
		paint(image.getGraphics());		
		return image;
	}	
	
	
	public final void addShape(Shape s) {
		synchronized (shapes) {
			shapes.add(s);
		}		
	}
	public final void addShapes(Collection<Shape> c) {
		synchronized (shapes) {
			shapes.addAll(c);
		}
	}	
	public final void removeShape(Shape s) {
		synchronized (shapes) {
			shapes.remove(s);
		}
	}	
	public final void removeShapes(Collection<Shape> c) {
		if(c.size()==0) return;		
		Set<Shape> all = new HashSet<Shape>(shapes);
		all.removeAll(c);
		synchronized (shapes) {
			shapes.clear();
			shapes.addAll(all);
		}
	}	
	public void clearShapes() {
		synchronized (shapes) {
			shapes.clear();
			shapes.trimToSize();
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public IPickable getPickableShapeAt(int x, int y, Class type) {
		IPickable res = null;
		int bestDist = Integer.MAX_VALUE;
		synchronized(shapes) {
			for(int i = shapes.size()-1; i>=0; i--) {
				Shape shape = shapes.get(i);
				if(shape.screenCoordinates!=null &&
					((type==null && shape instanceof IPickable) || 
					(type!=null && type.isAssignableFrom(shape.getClass())))) {
						
					int distSq = (shape.screenCoordinates.x-x)*(shape.screenCoordinates.x-x)
						+ (shape.screenCoordinates.y-y)*(shape.screenCoordinates.y-y);
					if(distSq>shape.diameter*shape.diameter/4+7*7) continue;
					distSq+=shape.screenCoordinates.z;
					if(distSq<bestDist) {
						bestDist = distSq;
						res = (IPickable) shape;
					}
				}	
			}
		}
		return res;
	}
			
	public Visualizer3D getVisualizer3D() {
		return visualizer3D;
	}

	public boolean isHighQuality() {
		return highQuality;
	}

	public void setHighQuality(boolean b) {
		highQuality = b;
	}

	public void addPaintProcessor(PaintProcessor p) {
		paintProcessors.add(p);
		processedOk = false;
	}

	public AbstractTool getTool() {
		return pickingTool;
	}


	public List<IPickable> getPickedShapes() {
		return pickedShaped;
	} 

	public void removePickedShapes() {
		if(pickedShaped.size()==0) return;
		for(int i=0; i<pickedShaped.size(); i++) {
			IPickable shape = pickedShaped.get(i);
			shape.setSelection(shape.getSelection() & ~1);
		}
		pickedShaped.clear();
		repaint();
	} 	
	
	public void setTool(AbstractTool tool) {
		this.pickingTool = tool;
		removePickedShapes();
	}

	public boolean isStereo() {
		return getStereoMode()!="";
	}
	boolean stereo = false;
	public void setStereo(boolean b) {
		stereo = b;
		try {
		if(b) System.setProperty("stereo", "interlaced");
		else System.getProperties().remove("stereo");
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}

	public String getStereoMode() {
		try {
			return System.getProperty("stereo");
		} catch (Exception e) {
			return stereo?"interlaced":"";
		}
	}
	

	public List<Shape> getShapes() {
		return Collections.unmodifiableList(shapes);
	}



}
