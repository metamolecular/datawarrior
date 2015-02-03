package com.actelion.research.gui.viewer2d;

import java.awt.Color;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.List;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.calculator.GeometryCalculator;
import com.actelion.research.chem.calculator.ProteinTools;
import com.actelion.research.chem.calculator.StructureCalculator;


/**
 * @author freyssj
 */
public class MoleculeCanvas extends Canvas3D implements ElementStyles {

	
	private static final long serialVersionUID = 1L;
	//Modes that can be setup using setMode
	public final static int SHOW_BACKBONE =  1<<0;
	public final static int SHOW_GROUPS = 	 1<<1;
	public final static int HIDE_HYDROGENS = 1<<2;
	public final static int SHOW_AMINO = 	 1<<3;
	public final static int SHOW_FLAGS =  	 1<<4;
	
	//The available colors for the Groups
	public final static Color[] GROUP_COLORS = new Color[]{Color.blue, Color.red, Color.cyan, Color.magenta, Color.yellow, Color.orange, Color.pink, Color.lightGray, Color.green};

	//The specific data needed for rendering the molecule 
	protected final MoleculeShapes moleculeShapes = new MoleculeShapes(this);
	
	private FFMolecule skeleton = null;
	
	protected FFMolecule mol;
	private int mode = SHOW_FLAGS;
	private boolean firstRendering = true;

	//The data needed for defining the style
	private int defaultStyle = AtomShape.BALLSTICKS;
	private int[] styles = null;

	//
	private int[] atomToGroup = null;
	public static final int MODE_SKELETON = 1024;
	private double slab = 0;
	
	/**
	 * Constructor
	 */
	public MoleculeCanvas() {
		this(null);	
	}
	
	/**
	 * Constructor
	 */
	public MoleculeCanvas(FFMolecule molecule) {
		setMolecule(molecule);
		
		addComponentListener(new ComponentListener() {
			public void componentHidden(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentResized(ComponentEvent e) {
				if(firstRendering && getSize().getWidth()>0) {resetView(); repaint(); firstRendering = false;}
			}
			public void componentShown(ComponentEvent e) {}
		});
		
	}

	public void setMolecule(Molecule m) {
		setMolecule(m==null? null : new FFMolecule((Molecule)m));
	}

	/**
	 * Sets the molecule without changing the visualizer position
	 * Clear the processors (skeleton, forces, ...)
	 * @param molecule
	 */
	public void setMolecule(FFMolecule m) {
		this.mol = m;
		init(true, true);
		clearPaintProcessors();
		repaint();
	}
	
	public void clearPaintProcessors() {
		paintProcessors.clear();
		paintProcessors.add(new SkeletonProcessor());
	}
	
	public void setMolecule(FFMolecule molecule, Coordinates centerOfRotation) {
		this.mol = molecule;
		init(true, true);
		getVisualizer3D().setCenterOfRotation(centerOfRotation);
		repaint();
	}
	
	/**
	 * Update the molecule object without recreating the processors
	 * @param molecule
	 */
	public void updateMolecule(FFMolecule molecule) {
		this.mol = molecule;
		init(false, false);
		repaint();
	}

	
	
	/**
	 * Cleanup the shapes and keep the molecule
	 */	
	public void cleanup() {
		clearShapes();		
		synchronized (moleculeShapes) {			
			addShapes(moleculeShapes);
		}
	}
	
	/**
	 * Init the shapes
	 * @param resetCenter (computes the bounds and center of rotation)
	 * @param resetZ (reset the camera setting and set the Z to view the whole molecule)
	 */
	public void init(boolean resetCenter, boolean resetZ) {
		processedOk = false;
		
		if(mol!=null && (resetCenter || firstRendering)) {
			//Sets Center of Gravity
			Coordinates cg = GeometryCalculator.getCenterGravity(mol);			
			visualizer3D.setCenterOfRotation(cg);
		
			//Calculates Bounds
			Coordinates[] bounds = GeometryCalculator.getBounds(mol);						
			visualizer3D.setBounds(bounds[0], bounds[1]);
		}		
		
		//if(resetCenter) paintProcessors.clear();
		init(mol, atomToGroup, styles);
		
		if(resetZ) resetView();
		
		firePropertyChange("init", !resetCenter, resetCenter);
		
				
	}
	public void init() {
		init(false, false);
	}

	/**
	 * Cleanup the Molecule shapes but keep the other shapes
	 */
	protected void init(FFMolecule mol, int[] atomToColor, int[] styles) {
		//Clear the shapes coming from the molecule
		clearShapes();
		synchronized (moleculeShapes) {			
			moleculeShapes.clear();
			moleculeShapes.trimToSize();
		}
		processedOk = false;
		
		if(mol!=null) {			
	
			//Calculate the Groups
			if(isMode(SHOW_GROUPS)) {
				
				List<List<Integer>> groups = StructureCalculator.getConnexComponents(mol);			
				atomToColor = new int[mol.getAllAtoms()];
				for (int i = 0; i < groups.size(); i++) {
					List<Integer> group = groups.get(i);
					for (int j = 0; j < group.size(); j++) {						
						atomToColor[group.get(j)] = i;

						if(mol instanceof FFMolecule) {
							try {
								atomToColor[group.get(j)] = ((FFMolecule) mol).getAtomSequence(group.get(j));								
							} catch (Exception e) {
								// Nothing
							}
						}
						
					}
				}
				
			} else if(isMode(SHOW_AMINO) && mol instanceof FFMolecule) {
				atomToColor = new int[mol.getAllAtoms()];
				FFMolecule m = (FFMolecule) mol;
				for (int a = 0; a < atomToColor.length; a++) {
					try {
						char c = (ProteinTools.getAminoLetter(m.getAtomAmino(a)));
						atomToColor[a] = Math.max(c,0);
					} catch (Exception e) {
						atomToColor[a] = 0;
					}
				}					
				
			} 
			if(isMode(SHOW_FLAGS) && mol instanceof FFMolecule) {
				FFMolecule m = (FFMolecule) mol;
				if(atomToColor==null || atomToColor.length!=m.getAllAtoms()) atomToColor = new int[m.getAllAtoms()];
				for (int a = 0; a < m.getAllAtoms(); a++) {
					boolean flag = m.isAtomFlag(a, FFMolecule.FLAG1);
					if(flag) atomToColor[a] = GROUP_COLORS.length-1;
				}					
			}
			
			//Creates the shapes
			createShapes(mol, true);
			setAtomToGroup(atomToColor);
			setStyles(styles);
		}
		
	}
	
	

	protected void calculateScreenCoordinates() {
		if(slab<=0 || !(mol instanceof FFMolecule)) {
			super.calculateScreenCoordinates();
		} else {
			minZ = Double.MAX_VALUE;
			maxZ = -minZ;
			int n = 0;
			//Coordinates med = new Coordinates();
			synchronized(getShapes()) {
				for (Shape shape: getShapes()) {
					shape.screenCoordinates = visualizer3D.screenPosition(shape.realCoordinates);
					if(shape instanceof AtomShape) {
						AtomShape s = (AtomShape) shape;
						if(((FFMolecule)mol).isAtomFlag(s.atm, FFMolecule.LIGAND)) {
							minZ = Math.min(minZ, shape.screenCoordinates.z); 
							maxZ = Math.max(maxZ, shape.screenCoordinates.z);
							//med.add(shape.realCoordinates); 
							n++;
						}
					}
				}
			}	
			
			if(n>0) {
				minZ-=(slab-2.5)*visualizer3D.scalePixelsPerAngstrom;
				maxZ+=(slab+2.5)*visualizer3D.scalePixelsPerAngstrom;
			} else {
				super.calculateScreenCoordinates();
			
			}
		}
		moleculeShapes.transform();
	}

	
	private void createShapes(final FFMolecule mol, int style, boolean allowPickable) {
		//Add the AtomShapes
		synchronized (moleculeShapes) {			
			AtomShape[] atomShapes = new AtomShape[mol.getAllAtoms()];
			for(int i=0; i<mol.getAllAtoms(); i++) {
				if(isMode(HIDE_HYDROGENS) &&  mol.getAtomicNo(i)<=1) continue;
				if(isMode(SHOW_BACKBONE) && (mol instanceof FFMolecule) && !((FFMolecule)mol).isAtomFlag(i, FFMolecule.BACKBONE)&& !((FFMolecule)mol).isAtomFlag(i, FFMolecule.LIGAND)) continue;
				atomShapes[i] = new AtomShape(mol, i, style, allowPickable? i: -1);
				moleculeShapes.add(atomShapes[i]);
			}
			
			//Add the BondShapes
			for(int i=0; i<mol.getAllBonds(); i++) {
				int atm1 = mol.getBondAtom(0, i);
				int atm2 = mol.getBondAtom(1, i);
				if(atomShapes[atm1]==null || atomShapes[atm2]==null) continue;
				moleculeShapes.add(new BondShape(mol, i, atomShapes[atm1], atomShapes[atm2], allowPickable? i: -1));
			}
			addShapes(moleculeShapes);
		}
	}

	public void createShapes(final FFMolecule mol, boolean allowPickable) {
		createShapes(mol, 0, allowPickable);
	}

	public FFMolecule getMolecule() {
		return mol;
	}
	
	public boolean isMode(int mode) {
		return (this.mode & mode) >0;
	}

	public void setMode(int mode, boolean v) {
		if(v) this.mode |= mode;
		else this.mode &= ~mode;
		init();
		
	}

	public static Color getColor(int eltNo) {
		Color rgb;
		if(eltNo<ELEMENT_COLORS.length) rgb = ELEMENT_COLORS[eltNo];
		else rgb = Color.white;
		return rgb;
	}


	/**
	 * Makes everything visible
	 */
	public void resetView() {
		if(mol==null) return;
		visualizer3D.resetView();
		repaint();
	}



	/**
	 * @param is
	 */
	private void setAtomToGroup(int[] is) {
		atomToGroup = is;
		if(is!=null && (isMode(SHOW_GROUPS) || isMode(SHOW_AMINO))) {
			synchronized (moleculeShapes) {			
				for (Shape element : moleculeShapes ) {								
					if(element instanceof AtomShape) {
						AtomShape as = (AtomShape) element;
						if(as.atm<0 || as.atm>=atomToGroup.length || atomToGroup[as.atm]<0) continue;							
						as.color = GROUP_COLORS[atomToGroup[as.atm] % GROUP_COLORS.length];					
					}
				}
			}			
		}
	}

	/**
	 * @param is
	 */
	public void setStyles(int[] is) {
		styles = is;
		synchronized (moleculeShapes) {			
			for (Object element : moleculeShapes) {
				if(element instanceof AtomShape) {
					AtomShape as = (AtomShape) element;
					if(styles==null || as.atm>=styles.length) {
						as.setStyle((mol instanceof FFMolecule && as.value>=0 && !((FFMolecule)mol).isAtomFlag(as.value, FFMolecule.LIGAND))? AtomShape.WIREFRAME: defaultStyle); 
					} else {
						as.setStyle(styles[as.atm]);
					}
				}
			}
		}
	}

	/**
	 * @return
	 */
	public int getStyle() {
		return defaultStyle;
	}

	/**
	 * @param i
	 */
	public void setStyle(int i) {
		defaultStyle = i;
		setStyles(null);
	}

	public int[] getAtomToStyles() {
		return styles;
	}

	public void setSkeleton(FFMolecule mol) {
		this.skeleton = mol;		
	}
	public FFMolecule getSkeleton() {
		return skeleton;
	}
		
	private class SkeletonProcessor extends PaintProcessor {
		public void preProcess() {
			if(skeleton!=null && isMode(MoleculeCanvas.MODE_SKELETON)) createShapes(skeleton, false);
		}		
	}

	public double getSlab() {
		return slab;
	}

	/**
	 * Sets the slab. The default value of 0 means no slab. 5 is a nice value to cut 5A in 
	 * front and in the back of the ligand ( if any)
	 * @param slab
	 */
	public void setSlab(double slab) {
		this.slab = slab;
	}
	
}
