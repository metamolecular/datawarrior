package com.actelion.research.gui.viewer2d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.GeometryCalculator;
import com.actelion.research.chem.calculator.StructureCalculator;

/**
 * 
 * @author freyssj
 */
@SuppressWarnings("rawtypes")
public class MoleculeViewer extends MoleculeCanvas implements MouseListener {

	public static final int MODE_NONE = 0;
	public static final int MODE_ATOMS = 32;
	public static final int MODE_CLASSES = 64;
	public static final int MODE_INTERACTIONS = 128;
	public static final int MODE_FORCES = 256;
	public static final int MODE_FLAGS = 512;
	public static final int MODE_COORDINATES = 2048;
	public static final int MODE_DESCRIPTIONS = 4096;

	private static final long serialVersionUID = 5614581272287603559L;
	
	/** defaultStyles if <> null, will enable the default feature */ 
	private int[] defaultStyles = null;
	private List<FFMolecule> undo = new ArrayList<FFMolecule>();

	/**
	 * Creates a new Viewer3DPanel with a default configuration 
	 */
	public MoleculeViewer() {
		createPopupMenu();
		
		addMouseListener(this);
		setMolecule(mol);
		setPreferredSize(new Dimension(300,200));
		/*
		//Set up D&D
		DragSource ds = DragSource.getDefaultDragSource();
		DragGestureListener dgl = new DragGestureListener(){
			public void dragGestureRecognized(DragGestureEvent dge) {
				
				if(!dge.getTriggerEvent().isControlDown()) return;
				FFMolecule m;
				
				if(getMolecule() instanceof FFMolecule) {
					m = StructureCalculator.extractLigand(getMolecule());
				} else {
					m = new FFMolecule(getMolecule());
				}
				
				for (int i = m.getAllAtoms()-1; i >=0; i--) {
					if(m.getAtomicNo(i)<=0) m.deleteAtom(i);
				}
				Transferable transfer = new MoleculeTransferable(m.toStereoMolecule());
				System.out.println("Start drag "+m.getAllAtoms()+" "+getMolecule());
				dge.startDrag(DragSource.DefaultCopyDrop, transfer);
			}};
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dgl);
				*/
	}

	/**
	 * Creates a new Viewer3D
	 */
	public MoleculeViewer(FFMolecule mol) {		
		this();
		setMolecule(mol);
		
	}

	/**
	 * Saves the Image to the specified filename 
	 */
	public void saveImage(String name) throws IOException {
		BufferedImage bufferedImage = this.getBufferedImage();
		File file = new File(name);

		ImageIO.write(bufferedImage, "jpeg", file);

		/* JPEGImageEncoder is ancient and not supported by Java 6 anymore

		FileOutputStream fileOutputStream = new FileOutputStream(file);
		JPEGImageEncoder jpegImageEncoder =
			JPEGCodec.createJPEGEncoder(fileOutputStream);
		JPEGEncodeParam jpegEncodeParam =
			jpegImageEncoder.getDefaultJPEGEncodeParam(bufferedImage);
		jpegEncodeParam.setQuality(1.0f, false);
		jpegImageEncoder.setJPEGEncodeParam(jpegEncodeParam);
		jpegImageEncoder.encode(bufferedImage);
		fileOutputStream.close();*/
	}

	public void saveImage() {
		try {
			String name = new File(System.getProperty("user.home"), "molecule.jpg").getPath();
			saveImage(name);
			JOptionPane.showMessageDialog(
			this,
				"<html><font color=#000000>The image has been saved to<br> <i>"
					+ name
					+ "</i></font></html>",
				"Image saved",
				JOptionPane.PLAIN_MESSAGE);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(
				this,
				"The image could not be saved\n" + e,
				"Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	protected void processSubPopupMenu(JPopupMenu menu) {}

	protected JPopupMenu createPopupMenu() {
		JMenuItem item;
		JPopupMenu menu = new JPopupMenu();

		JMenu menuRender = new JMenu("Render"); 
		menuRender.setMnemonic('R');
		menuRender.add(createMenuItem(new ActionRender(AtomShape.WIREFRAME, "WireFrame"), 'w', false));
		menuRender.add(createMenuItem(new ActionRender(AtomShape.BALLSTICKS, "Balls & Sticks"), 's', false));
		menuRender.add(createMenuItem(new ActionRender(AtomShape.STICKS, "Sticks"), 't', false));
		menuRender.add(createMenuItem(new ActionRender(AtomShape.BALLS, "Balls"), 'b', false));
		menuRender.add(new JSeparator());		
		
		item = createMenuItem(new ActionShowBackbone(), 'k', isMode(MoleculeCanvas.SHOW_BACKBONE));
		if(getMolecule()!=null && getMolecule().getAllAtoms()>50) menuRender.add(item);
		menuRender.add(createMenuItem(new ActionShowHideHydrogen(), 'h', false));
		menuRender.add(new JSeparator());
		menuRender.add(createMenuItem(new ActionStereo("Stereo Mode"), '-', false));
		menu.add(menuRender);

		JMenu menuColors = new JMenu("Colors"); 
		menuColors.setMnemonic('C');
		menuColors.add(createMenuItem(new ActionColors(0, "Atomic No"), 'a', false));
		menuColors.add(createMenuItem(new ActionColors(1, "Groups"), 'g', false));
		menuColors.add(createMenuItem(new ActionColors(2, "Aminos"), 'm', false));
		menu.add(menuColors);
				
		JMenu menuMeasurements = new JMenu("Measurements");
		menuMeasurements.setMnemonic('M');
		menuMeasurements.add(createMenuItem(new ActionPickDistance(), 'D', getTool() instanceof ToolMeasureDistance));
		menuMeasurements.add(createMenuItem(new ActionPickAngle(), 'A', getTool() instanceof ToolMeasureAngle));
		menuMeasurements.add(createMenuItem(new ActionPickDihedral(), 'T', getTool() instanceof ToolMeasureDihedral));
		menu.add(menuMeasurements);

		JMenu info = new JMenu("Informations");
		info.add(createMenuItem(new ActionInfoAxes(), (char)0, isMode(MODE_COORDINATES)));
		info.add(createMenuItem(new ActionInfoDescriptions(), (char)0, isMode(MODE_COORDINATES)));
		info.add(createMenuItem(new ActionInfoCoordinates(), (char)0, isMode(MODE_COORDINATES)));
		info.add(new JSeparator());
		info.add(createMenuItem(new ActionInfoInteractions(), (char)0, isMode(MODE_INTERACTIONS)));	
		info.add(new JSeparator());
		menu.add(info);
		
		
		
		
		processSubPopupMenu(menu);

				
		menu.add(createMenuItem(new ActionClearDecorations(), 'C', false));
		
		menu.add(new JSeparator());
		menu.add(createMenuItem(new ActionSlab(), 'S', false));
		menu.add(createMenuItem(new ActionResetView(), 'c', false));

		menu.add(new JSeparator());
		//menu.add(createMenuItem(new ActionCopy(), 'c', false, KeyEvent.CTRL_DOWN_MASK ));
		//menu.add(createMenuItem(new ActionPaste(), 'p', false, KeyEvent.CTRL_DOWN_MASK ));
		menu.add(createMenuItem(new ActionSaveImage(), 'i', false));
		
		return menu;
	}

	protected JMenuItem createMenuItem(final AbstractAction action, final char accelerator, boolean high) {
		return createMenuItem(action, accelerator, high, 0);
	}
	private final Set<Character> usedAccelerators = new HashSet<Character>(); 
	protected JMenuItem createMenuItem(final AbstractAction action, final char accelerator, boolean high, int modif) {
		JMenuItem menu = new JMenuItem(action);
		if(accelerator>0) {
			menu.setAccelerator(KeyStroke.getKeyStroke(new Character(accelerator), modif));
			if(!usedAccelerators.contains(accelerator)) {
				addKeyListener(new KeyAdapter(){
					public void keyPressed(KeyEvent e) {
						if(e.getKeyChar()==accelerator) action.actionPerformed(null);
					}	
				});
				usedAccelerators.add(accelerator);
			}
		}
		return menu;
	}
	
	////////////////// KEY LISTENER /////////////////////////
	public void keyPressed(KeyEvent event) {}
	public void keyReleased(KeyEvent event) {}	

	////////////////// MOUSE LISTENER /////////////////////////
	public void mouseClicked(MouseEvent e) {
		if (e.getModifiers() == MouseEvent.BUTTON3_MASK) {
			JPopupMenu menu = createPopupMenu();
			menu.show(this, e.getX(), e.getY());
		}
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mousePressed(MouseEvent e) {
		requestFocus();
	}

	/////////////////// ACTIONS ///////////////////////////////////
	public class ActionInfoAxes extends AbstractAction {
		public ActionInfoAxes() {super("Show Axes");}
		public void actionPerformed(ActionEvent e) {

			Coordinates g2 = StructureCalculator.getLigandCenter((FFMolecule) mol);
			if(g2==null) g2 = GeometryCalculator.getCenterGravity(mol);
			final Coordinates g = g2;
			
			addShape(new Text(g.addC(new Coordinates(0,0,0)), g.toString(), 10, Color.green));
			addShape(new Line(g, g.addC(new Coordinates(10,0,0)), Color.green, Line.DASHED_STROKE));
			addShape(new Text(g.addC(new Coordinates(10,0,0)), "X", 14, Color.green));
			addShape(new Line(g, g.addC(new Coordinates(0,10,0)), Color.green, Line.DASHED_STROKE));
			addShape(new Text(g.addC(new Coordinates(0,10,0)), "Y", 14, Color.green));
			addShape(new Line(g, g.addC(new Coordinates(0,0,10)), Color.green, Line.DASHED_STROKE));
			addShape(new Text(g.addC(new Coordinates(0,0,10)), "Z", 14, Color.green));
			repaint();
		}		
	}

	
	public class ActionInfoCoordinates extends AbstractAction {
		public ActionInfoCoordinates() {super("Show Coordinates");}
		public void actionPerformed(ActionEvent e) {
			if(mol==null) return;
		
			for(int i = 0; i<mol.getAllAtoms(); i++) {	
				if(isMode(HIDE_HYDROGENS) && mol.getAtomicNo(i)<=1) continue;
				Coordinates c = new Coordinates(mol.getAtomX(i), mol.getAtomY(i), mol.getAtomZ(i));
				Shape s = new Text(c , i+" "+c, 9, Color.orange);
				addShape(s);
			}					
			repaint();	
		}		
	}
	public class ActionInfoDescriptions extends AbstractAction {
		public ActionInfoDescriptions() {super("Show Descriptions");}
		public void actionPerformed(ActionEvent e) {
			if(mol==null || !(mol instanceof FFMolecule)) return;
			FFMolecule mol = (FFMolecule)getMolecule();
		
			for(int i = 0; i<mol.getAllAtoms(); i++) {	
				if(isMode(HIDE_HYDROGENS) && mol.getAtomicNo(i)<=1) continue;
				Shape s = new Text(mol.getCoordinates(i), " "+(mol.getAtomDescription(i)!=null?mol.getAtomDescription(i):""), 9, Color.white);
				addShape(s);
			}					
			repaint();	
		}		
	}
	
	
	public class ActionInfoInteractions extends AbstractAction {
		public ActionInfoInteractions() {super("Show H-Bonds");}
		public void actionPerformed(ActionEvent e) {
			if(mol==null || !(mol instanceof FFMolecule)) return;
			FFMolecule mol = (FFMolecule) getMolecule();
			List interactions = StructureCalculator.getHBonds(mol);
			Iterator iter = interactions.iterator();
			while(iter.hasNext()) {
				int[] couple = (int[]) iter.next();
				//if(!mol.isAtomFlag(couple[0], FFMolecule.LIGAND) && !mol.isAtomFlag(couple[1], FFMolecule.LIGAND)) continue;
				Shape line = new Line(mol.getCoordinates(couple[0]), mol.getCoordinates(couple[1]), Color.green, Line.DOTTED_STROKE);
				addShape(line);
			}

			repaint();	
		}
	}

	
	private class ActionSlab extends AbstractAction {
		public ActionSlab() {
			super(getSlab()==0?"Set Slab to 6 A":"Remove Slab");
		}
		public void actionPerformed(ActionEvent e) {
			setSlab(getSlab()==0?6:0);
			init(false, false);
			repaint();
		}
	}
	
	/*
	private class ActionCopy extends AbstractAction {
		public ActionCopy() {
			super("Copy");
		}
		public void actionPerformed(ActionEvent e) {
			StereoMolecule mol;
			if(getMolecule() instanceof FFMolecule) {
				mol = new StereoMolecule(StructureCalculator.extractLigand(getMolecule()).toStereoMolecule());
			} else {
				mol = new StereoMolecule(getMolecule().toStereoMolecule());
			}
			new ClipboardHandler().copyMolecule(mol);
			
		}
	}
	
	private class ActionPaste extends AbstractAction {
		public ActionPaste() {
			super("Paste");
		}
		public void actionPerformed(ActionEvent e) {
			ExtendedMolecule mol = new ClipboardHandler().pasteMolecule();
			if(mol==null) return;
			if(getMolecule() instanceof FFMolecule) {
				StructureCalculator.replaceLigand(getMolecule(), new FFMolecule(mol));
				System.out.println("mol has"+getMolecule());
			} else {
				setMolecule(new FFMolecule(mol));
			}

			init(true, false);
			repaint();
		}
	}	
	/*
	private class ActionPaste extends AbstractAction {
		public ActionPaste() {
			super("Paste");
		}
		public void actionPerformed(ActionEvent e) {
			ExtendedMolecule mol = new ClipboardHandler().pasteMolecule();
			System.out.println("PASTE "+mol.getAllAtoms());
			if(mol==null) return;
			if(getMolecule() instanceof Molecule3D) {
				StructureCalculator.replaceLigand((Molecule3D)getMolecule(), new FFMolecule(mol));
				System.out.println("mol has"+getMolecule());
			} else {
				setMolecule(mol);
			}

			init(true, false);
			repaint();
			
		}
	}	*/
	
	private class ActionRender extends AbstractAction {
		private final int style;
		public ActionRender(int style, String text) {
			super(text);
			this.style = style;
		}
		public void actionPerformed(ActionEvent e) {
			if(style==0 && defaultStyles!=null) setStyles(defaultStyles);
			else setStyle(style);
			

			init(false, false);
			repaint();
		}
	}
	
	private class ActionStereo extends AbstractAction {
		public ActionStereo(String text) {
			super(text);
		}
		public void actionPerformed(ActionEvent e) {
			setStereo(!isStereo());
			repaint();
		}
	}
	/*
	private class ActionSAS2 extends AbstractAction {
		boolean ligand;
		public ActionSAS2(String text, boolean ligand) {
			super(text);
			this.ligand = ligand;
		}
		public void actionPerformed(ActionEvent e) {
			new ActionDottedSurface(ligand).actionPerformed(MoleculeViewer.this);
		}
	}
	private class ActionSAS extends AbstractAction {
		boolean ligand;
		public ActionSAS(String text, boolean ligand) {
			super(text);
			this.ligand = ligand;
		}
		public void actionPerformed(ActionEvent e) {
			new ActionLigandSAS().actionPerformed(MoleculeViewer.this);
		}
	}*/

	private class ActionColors extends AbstractAction {
		private final int style;
		public ActionColors(int style, String text) {
			super(text);
			this.style = style;
		}
		public void actionPerformed(ActionEvent e) {
			if(style==0) {setMode(SHOW_GROUPS, false); setMode(SHOW_AMINO, false);} 
			else if(style==1) {setMode(SHOW_GROUPS, true); setMode(SHOW_AMINO, false);}
			else {setMode(SHOW_GROUPS, false) ; setMode(SHOW_AMINO, true);}
			repaint();
		}
	}

	private class ActionResetView extends AbstractAction {
		public ActionResetView() {super("Recenter View");}
		public void actionPerformed(ActionEvent e) {
			resetView();
			repaint();
		}
	}
	
	private class ActionSaveImage extends AbstractAction {
		public ActionSaveImage() {super("Save Image");}
		public void actionPerformed(ActionEvent e) {
			saveImage();
		}
	}
	private class ActionShowBackbone extends AbstractAction {
		public ActionShowBackbone() {super((!isMode(MoleculeCanvas.SHOW_BACKBONE)?"Show":"Hide") + " Backbone");}
		public void actionPerformed(ActionEvent e) {
			if(getMolecule()!=null && getMolecule().getAllAtoms()>50) setMode(MoleculeCanvas.SHOW_BACKBONE, !isMode(MoleculeCanvas.SHOW_BACKBONE)); repaint();
		}
	}	
	
	private class ActionPickDistance extends AbstractAction {
		public ActionPickDistance() {super("Measure Distance");}
		public void actionPerformed(ActionEvent e) {
			setTool(new ToolMeasureDistance());repaint();
		}
	}
	public class ActionPickAngle extends AbstractAction {
		public ActionPickAngle() {super("Measure Angle");}
		public void actionPerformed(ActionEvent e) {
			setTool(new ToolMeasureAngle());repaint();
		}
	}
	private class ActionPickDihedral extends AbstractAction {
		public ActionPickDihedral() {super("Measure Torsion");}
		public void actionPerformed(ActionEvent e) {
			setTool(new ToolMeasureDihedral());repaint();
		}
	}	
	public class ActionClearDecorations extends AbstractAction {
		public ActionClearDecorations() {super("Clear Decorations");}
		public void actionPerformed(ActionEvent e) {
			paintProcessors.clear();
			init();
			repaint();
		}
	}
	private class ActionShowHideHydrogen extends AbstractAction {
		public ActionShowHideHydrogen() {super((isMode(MoleculeCanvas.HIDE_HYDROGENS)?"Show":"Hide") + " Hydrogens");}
		public void actionPerformed(ActionEvent e) {
			setMode(MoleculeCanvas.HIDE_HYDROGENS, !isMode(MoleculeCanvas.HIDE_HYDROGENS));repaint();
		}
	}

	public static MoleculeViewer viewMolecule(FFMolecule mol) {
		JPanel panel = new JPanel(new GridLayout(1,1));
		final MoleculeViewer viewer = new MoleculeViewer();
		viewer.setMolecule(mol);
		viewer.repaint();
		panel.add(viewer);
	
		final JFrame frame = new JFrame();
		frame.setTitle("Test 3D molecule representation");
		frame.setSize(1000, 600);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(BorderLayout.CENTER, panel);
		frame.setVisible(true);
	
		
		frame.addComponentListener(new ComponentListener(){
			public void componentHidden(ComponentEvent e) {}
			public void componentResized(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {};

			public void componentMoved(ComponentEvent e) {
				Point p = frame.getLocationOnScreen();
				if(p.x%2==0) frame.setLocation(p.x+1, p.y);
			}
		});
		return viewer;	
	}
	

	/**
	 * @return
	 */
	public int[] getDefaultStyles() {
		return defaultStyles;
	}

	/**
	 * @param is
	 */
	public void setDefaultStyles(int[] is) {
		defaultStyles = is;
	}
	
	public void saveUndoStep() {
		FFMolecule copy = new FFMolecule(getMolecule());
		copy.compact();
		undo.add(copy);
		if(undo.size()>10) undo.remove(0);
	}

	public void undo() {
		if(undo.size()>0) updateMolecule(undo.remove(undo.size()-1));
	}

}
