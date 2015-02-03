/*
 * Copyright 2014 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Joel Freyss
 */
package com.actelion.research.gui.jmol;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.vecmath.Point3f;

import com.actelion.research.chem.CoordinateInventor;
import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.calculator.AdvancedTools;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.mm2.MM2Parameters;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.util.JExceptionDialog;
import com.actelion.research.util.Console;
import com.actelion.research.util.Settings;


/**
 * Viewer on a FFMolecule without the forcefield functionalities
 */
public class MoleculeViewer extends DisplayPanel implements MouseListener, MouseMotionListener {

	private static FFMolecule copyPasteMolecule = null;
	
	public enum Labeling {
		NONE,
		COORDINATES,
		PDB,
		MM2,
		DESCRIPTIONS,
		PARTIAL_CHARGES;
	}

	public static final int SCHEME_LIGAND_SURFACE = 4;
	public static final int SCHEME_SOLVENT_ACCESSIBLE_SURFACE = 6;
	public static final int SCHEME_LIGAND_PROTEIN_SURFACE = 5;
	
	public static final int SCHEME_PROTEIN_CAVITIES = 7;
	public static final int SCHEME_PROTEIN_SURFACE = 1;
	public static final int SCHEME_PROTEIN_WIREFRAME = 3;
	
    private static final String STEREO_MODE_PROPERTY = "actelion.stereo";
    public static final String STEREO_MODE_NONE = "";
    public static final String STEREO_MODE_CROSS = "cross";
    public static final String STEREO_MODE_PARALLEL = "parallel";
  	public static final String STEREO_MODE_RED_BLUE = "redBlue";
    public static final String STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST = "hInterlacedLeftEyeFirst";
    public static final String STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST = "hInterlacedRightEyeFirst";
    public static final String STEREO_MODE_V_INTERLACE = "vInterlaced";
	
	public static final int DISPLAY_HYDROGENS = 1<<1;
	public static final int DISPLAY_SIDECHAINS = 1<<2;
	public static final int DISPLAY_SKELETON = 1<<3;
	public static final int DISPLAY_RIBBON = 1<<4;
	public static final int DISPLAY_HBONDS = 1<<5;
	
	public static final int COLOR_ATOMICNO = 0;
	public static final int COLOR_CHAIN = 1;
	public static final int COLOR_AMINO = 2;
	public static final int COLOR_PARTIALCHARGES = 3;

	private int scheme = SCHEME_PROTEIN_WIREFRAME;
	
	protected ViewerWrapper viewer;
	protected BitSet mode = new BitSet(); 
	protected int color;
	protected int display = DISPLAY_HYDROGENS | DISPLAY_SIDECHAINS | DISPLAY_SKELETON | DISPLAY_RIBBON ;
	protected Console console = new Console(); 

	private String stereoMode = STEREO_MODE_NONE;
	private java.util.List<String> shapes = new ArrayList<String>(); 	
	private FFMolecule skeleton = null;
	protected FFMolecule mol;
	private ActelionJmolAdapter jmolAdapter = new ActelionJmolAdapter();		
	private Coordinates cavityCenter = null;
	
	public static double P_BOND_THICKNESS = .15; 
	public static double P_ATOM_THICKNESS = .3; 
	public static String P_BG_FILE = null; 
	public static String P_BG_COLOR = null; 
	
	protected boolean readOnly = true;
	
	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
		doScheme();
	}

	public boolean isDisplay(int option) {
		return (display & option) == option;
	}
	
	public void setDisplay(int option, boolean active) {
		if(option>0) {
			if(!active) {
				this.display &= ~option;
			} else {
				this.display |= option;			
			}
		}
		toggleDisplay(0);		
	}
	
	
	/**
	 * Constructor
	 */
	public MoleculeViewer() {
		this(new FFMolecule());
	}
	
	public MoleculeViewer(FFMolecule molecule) {
		super(true, 640, 480);
		viewer = new ViewerWrapper(this, jmolAdapter);
		viewer.setJmolStatusListener(new JmolStatusListener(this));
		
		viewer.zap(false, false);
		
		viewer.setFrankOn(false);
		viewer.setAutoBond(false);
		setViewer(viewer);
			
		addMouseListener(this);
		addMouseMotionListener(this);
		
		refreshThread.start();
		setFocusable(true);
		
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("F10"), "SCRIPT");
		getActionMap().put("SCRIPT", new ActionScript());

		
		setBackground(Color.black);
		setMolecule(molecule);
		
		//Initialize shortcuts
		createPopupMenu(-1);
		
		//Get Stereo Mode
		stereoMode = Settings.getProperty(STEREO_MODE_PROPERTY);
		setStereo(stereoMode);
	}
	
	public void setScheme(int scheme) {
		this.scheme = scheme;
		doScheme();
	}

	public void doScheme() {
		viewer.scriptManager.clearQueue();
//		viewer.scriptManager.flushQueue("");;

		StringBuilder sb = new StringBuilder();
		sb.append("set refreshing false;selectionHalos OFF; select *;");			
		sb.append("backbone off;ribbon off;trace off;cartoon off;strands off;cartoon off;meshribbon off;rockets off; isosurface off;");
		sb.append("set hoverDelay 0.1; hover ON; set bondPicking OFF;font label 13; set measurementUnits ANGSTROMS;");
		
		
		String colorIsoSurface = "";
		
		//Colors
		
		switch(color) {
			case COLOR_ATOMICNO: sb.append("color cpk;"); colorIsoSurface = "range all  property element"; break;
			case COLOR_AMINO:    sb.append("color amino;"); colorIsoSurface = "white"; break;
			case COLOR_PARTIALCHARGES:    sb.append("color atoms partialcharge;"); colorIsoSurface = "property partialcharge colorscheme bwr color absolute -0.5 0.5" ; break;
			case COLOR_CHAIN:    sb.append("color chain;"); colorIsoSurface = "element"; break;
		}

		//Protein rendering
		sb.append("select *; wireframe; cpk off; select not connected; cpk 0.2; select *; ");
		
		//Surface rendering
		switch(scheme) {
		case SCHEME_LIGAND_SURFACE:
			sb.append("set isosurfacePropertySmoothing off;isoSurface ignore(not hetero or {elemNo=99}) sasurface 0 colorscheme jmol color "+colorIsoSurface+";");
			break;
		case SCHEME_SOLVENT_ACCESSIBLE_SURFACE:
			sb.append("select hetero;set isosurfacePropertySmoothing off;isoSurface ignore({elemNo=99}) sasurface fullylit colorscheme jmol color "+colorIsoSurface+";");
			break;
		case SCHEME_LIGAND_PROTEIN_SURFACE:
			sb.append("select(within(7,hetero));set isosurfacePropertySmoothing off; isoSurface  ignore(hetero) solvent colorscheme jmol color "+colorIsoSurface+" translucent 0.3;");
			break;
		case SCHEME_PROTEIN_CAVITIES:				
			sb.append("set isosurfacePropertySmoothing off; isosurface ignore(hetero or water) pocket cavity color green;");
			break;
		case SCHEME_PROTEIN_WIREFRAME:
			break;			
		case SCHEME_PROTEIN_SURFACE:
			sb.append("set isosurfacePropertySmoothing off;isoSurface ignore(hetero or water) molecular colorscheme jmol color "+colorIsoSurface+";");
			break;
		}		

		//Hetero Atom rendering
		sb.append("select hetero;");
		if(P_BOND_THICKNESS>0) {
			sb.append("wireframe " + P_BOND_THICKNESS + ";");	
		} else {
			sb.append("wireframe;");
		}
		if(P_ATOM_THICKNESS>0) {
			sb.append("cpk " + P_ATOM_THICKNESS + ";");	
		} else {
			sb.append("cpk off;");
		}
		sb.append("select {elemNo=99};cpk 0;");
		
		
		

		
		sb.append(getAfterScript());
		script(sb.toString());
		script("set refreshing true;");
		toggleDisplay(0);
		
	}
	
	/**
	 * 
	 * @param option
	 */
	public void toggleDisplay(int option) {
		if(option>0) {
			if((this.display & option)>0) {
				this.display &= ~option;
			} else {
				this.display |= option;			
			}
		}
		
		StringBuilder backboneList = new StringBuilder();
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if(mol.isAtomFlag(i, FFMolecule.BACKBONE) && !mol.isAtomFlag(i, FFMolecule.LIGAND)) {
				if(backboneList.length()>0) backboneList.append(",");
				backboneList.append("{atomno="+(i+1)+"}");
			}
		}

		StringBuilder sb = new StringBuilder();
		StringBuilder hide = new StringBuilder();
		
		
		if((display & DISPLAY_SIDECHAINS)>0 && (display & DISPLAY_RIBBON)>0) {
			sb.append("select not hetero; wireframe on; ribbons on;");
		} else if((display & DISPLAY_SIDECHAINS)>0 && (display & DISPLAY_RIBBON)==0) {
			sb.append("select not hetero; wireframe on; ribbons off; ");
		} else if((display & DISPLAY_SIDECHAINS)==0 && (display & DISPLAY_RIBBON)>0) {
			sb.append("select not hetero; wireframe off;");
			sb.append("select "+backboneList+"; ribbons on;");			
		} else if((display & DISPLAY_SIDECHAINS)==0 && (display & DISPLAY_RIBBON)==0) {
			sb.append("select not hetero; wireframe off;");
			sb.append("select "+backboneList+"; wireframe on; ribbons off; ");			
		} else {
			System.err.println("Invalid combination");
		}

		if((display & DISPLAY_HYDROGENS)>0) {
			sb.append("set showHydrogens TRUE; select {_O and not connected}; cpk .2; select water; cpk 1;");
		} else {
			sb.append("set showHydrogens FALSE;select {_O and not connected}; cpk off; select water; cpk off;");			
			hide.append((hide.length()>0?" or ":"") + "{elemno=99} ");
		}
		if((display & DISPLAY_HBONDS)>0) {
			sb.append("select *;hbonds on;");
		} else {
			sb.append("select *;hbonds off;");				
		}
		if(hide.length()>0) sb.append("hide "+hide+";");
		else sb.append("hide none;");
		
		//Draw skeleton
		if(skeleton!=null) {
			for (int i = 0; i < skeleton.getAllBonds(); i++) {
				int a1 = skeleton.getBondAtom(0, i);
				int a2 = skeleton.getBondAtom(1, i);
				if(skeleton.getAtomicNo(a1)>1 && skeleton.getAtomicNo(a2)>1) {
					if(isDisplay(DISPLAY_SKELETON)) sb.append("draw skeleton"+i+" CYLINDER {" + skeleton.getAtomX(a1) + " " + skeleton.getAtomY(a1) + " "+ skeleton.getAtomZ(a1) + "} {" + skeleton.getAtomX(a2) + " " + skeleton.getAtomY(a2) + " "+ skeleton.getAtomZ(a2) + "} diameter .4 translucent 0.7;");
					else sb.append("draw skeleton" + i + " delete;");
				}				
			}			
		}
		//Mark Flag1
		for (int i = 0; i < mol.getAllAtoms(); i++) {
			if(mol.isAtomFlag(i, FFMolecule.IMPORTANT)) {
				sb.append("select (*)["+(i+1) + "]; color magenta opaque;");		
			}
		}

		script(sb.toString());
		
	}

	

	@SuppressWarnings("unchecked")
	public StringBuilder getAfterScript() {
		StringBuilder sb = new StringBuilder();
		

		if(mol.getAuxiliaryInfos().get("Hbonds")!=null) {
			try {
				List<int[]> hBonds = (List<int[]>) mol.getAuxiliaryInfos().get("Hbonds");
				int lno = 0;
				for (int[] b : hBonds) {
					sb.append("draw line"+(lno++)+" (atomno="+(b[0]+1)+") (atomno="+(b[1]+1)+") width .1 color green;");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		

		for (String script : shapes) {
			sb.append(script);
		}
		return sb;
	}
	
	
	public synchronized void setMolecule(FFMolecule m) {
		this.mol = m;
		cavityCenter = StructureCalculator.getLigandCenter((FFMolecule) m);
		if( m.getAuxiliaryInfo("isPDB")!=null && !(m.getAuxiliaryInfo("isPDB") instanceof Boolean)) m.setAuxiliaryInfo("isPDB", Boolean.TRUE);
		viewer.pushHoldRepaint();
		try {
			viewer.openClientFileNoInit("????", m.getName(), mol);
			
			
			//Recenter if the center is not within the molecule's bound
			if(m.getAllAtoms()>0) {
				Coordinates rot = getRotationCenter();
				Coordinates[] bounds = m.getNMovables()>0? StructureCalculator.getLigandBounds(m): StructureCalculator.getBounds(m);				
				if(bounds!=null && !rot.insideBounds(bounds)) {
					center();
				}
			}
			
			doScheme();
		} finally {
			viewer.popHoldRepaint();
		}
		
	}


	public void refresh() {
		if(viewer==null) return;
		viewer.pushHoldRepaint();   
		try {
			boolean changed = viewer.refresh(mol);
			if(changed) {
				doScheme();
			} else {
				script(getAfterScript().toString());
			}
		} finally {		
			viewer.popHoldRepaint();
		}
	}
	
	
	/**
	 * Cleanup the shapes and keep the molecule
	 */	
	public void cleanup() {
		script("select;label OFF;draw DELETE; select 0;");
		
	}
	
	/**
	 * Init the shapes
	 */
	public void init() {
		scheme = 0;
		mode.clear();
		color = 0;
		cleanup();
		setMolecule(mol);
	}
	
	public FFMolecule getMolecule() {
		return mol;
	}
	
	public boolean isMode(int mode) {
		return this.mode.get(mode);
	}

	public void setMode(int mode, boolean v) {
		if(v) this.mode.set(mode);
		else this.mode.clear(mode);
		doScheme();
		
	}


	public void script(final String s) {
		viewer.evalString(s);
	}
	
	
	
	/**
	 * add a shape 
	 * @param s
	 */
	public synchronized void addAfterScript(String script) {
		shapes.add(script);
	}
	/**
	 * add a shape 
	 * @param s
	 */
	public synchronized void clearAfterScript() {
		shapes.clear();
		script("draw delete;");
	}
	
	
	/**
	 * Makes everything visible
	 */
	public void center() {
		if(mol.getNMovables()>0) {
			script("reset;select hetero;center selected;");
		} else {
			script("reset;select *;center selected;");
		}
	}


	public void setSkeleton(FFMolecule mol) {
		this.skeleton = mol;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 50; i++) {
			sb.append("draw skeleton" + i + " delete;");
		}
		script(sb.toString());
		if(mol!=null) doScheme();
	}
	public FFMolecule getSkeleton() {
		return skeleton;
	}
	
	
	protected AbstractTool tool;
	
	public void setTool(AbstractTool tool) {
		if(tool==this.tool) return;
		if(this.tool!=null) this.tool.deactivateTool(this);
		this.tool = tool;
		if(this.tool!=null) this.tool.activateTool(this);
	}
	public AbstractTool getTool() {
		return tool;
	}	
	
	
	
	public void setStereo(String stereoMode) {
		this.stereoMode = stereoMode==null? STEREO_MODE_NONE: stereoMode ;
		if( stereoMode!=null && stereoMode.equals(STEREO_MODE_CROSS)) {
			script("stereo -5 ;");			
		} else if( stereoMode!=null && stereoMode.equals(STEREO_MODE_PARALLEL)) {
			script("stereo 5;");			
		} else if(stereoMode!=null && stereoMode.equals(STEREO_MODE_RED_BLUE)) {
			script("stereo REDBLUE;");			
		} else {
			script("stereo off;");
		}
		Settings.setProperty(STEREO_MODE_PROPERTY, stereoMode==null?"":stereoMode);

	}
	
	/**
	 * To be overriden by subclasses if needed
	 * @param popup
	 * @param atm
	 */
	public void populateFileMenu(JPopupMenu popup, int atm) {}
	/**
	 * To be overriden by subclasses if needed
	 * @param popup
	 * @param atm
	 */
	public void populateAtomMenu(JPopupMenu popup, int atm) {}

	/**
	 * Creates a contextual popup menu
	 * @param selectedAtom (-1 if no atom selected)
	 * @return
	 */
	public JPopupMenu createPopupMenu(int selectedAtom) {
		JPopupMenu menu = new JPopupMenu();
		
		//Atom Menu
		JLabel lbl = new JLabel("Atom:  " 
				+ (selectedAtom<0?"":
					(getMolecule().isAtomFlag(selectedAtom, FFMolecule.LIGAND)?"Ligand ": "Protein ") + Molecule.cAtomLabel[mol.getAtomicNo(selectedAtom)] + (getMolecule().getAtomDescription(selectedAtom)==null?"": " - " + getMolecule().getAtomDescription(selectedAtom)) + "(" + selectedAtom + ")"));
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
		menu.add(lbl);
		menu.add(new JSeparator());
		populateAtomMenu(menu, selectedAtom);
		if(selectedAtom<0) {
			JLabel lbl2 = new JLabel("       No atom selected");
			lbl2.setFont(lbl2.getFont().deriveFont(Font.ITALIC));
			menu.add(lbl2);
		} else {
			JMenu setAtomicNo = new JMenu("Set AtomicNo"); 
			setAtomicNo.setEnabled(!isReadOnly());
			setAtomicNo.add(new ActionSetAtomicNo(selectedAtom, 1));		
			setAtomicNo.add(new ActionSetAtomicNo(selectedAtom, 6));		
			setAtomicNo.add(new ActionSetAtomicNo(selectedAtom, 7));		
			setAtomicNo.add(new ActionSetAtomicNo(selectedAtom, 8));		
			setAtomicNo.add(new ActionSetAtomicNo(selectedAtom, 15));		
			setAtomicNo.add(new ActionSetAtomicNo(selectedAtom, 16));		
			menu.add(setAtomicNo);
			
//			JMenu submenu = new JMenu("Change Torsion");
//			submenu.setEnabled(getMolecule().isAtomFlag(selectedAtom, FFMolecule.LIGAND));
//			submenu.add(new ActionChangeTorsion(selectedAtom, true));		
//			submenu.add(new ActionChangeTorsion(selectedAtom, false));				
//			menu.add(submenu);
//			
			ActionMoveAtom move  = new ActionMoveAtom(selectedAtom);
			move.setEnabled(!isReadOnly() && getMolecule().isAtomFlag(selectedAtom, FFMolecule.LIGAND));
			menu.add(move);
				
			JMenu submenu = new JMenu("Ring Conformation");
			submenu.setEnabled(!isReadOnly() && getMolecule().isAtomFlag(selectedAtom, FFMolecule.LIGAND));
			submenu.add(new ActionRing(selectedAtom, 0));		
			submenu.add(new ActionRing(selectedAtom, 1));		
			submenu.add(new ActionRing(selectedAtom, 2));		
			submenu.add(new ActionRing(selectedAtom, 3));		
			menu.add(submenu);
			
			menu.add(new ActionSetRotationCenter(selectedAtom));
		}
		menu.add(new JSeparator());	
		menu.add(new JSeparator());

		//Chain Menu
		lbl = new JLabel("Chain: ");
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
		menu.add(lbl);
		menu.add(new JSeparator());
		if(selectedAtom<0) {
			JLabel lbl2 = new JLabel("       No atom selected");
			lbl2.setFont(lbl2.getFont().deriveFont(Font.ITALIC));
			menu.add(lbl2);
		} else {
			AbstractAction action = new ActionMarkLigand(selectedAtom);
			action.setEnabled(!isReadOnly());
			menu.add(action);
			
			action = new ActionMarkProtein(selectedAtom);
			action.setEnabled(!isReadOnly() && getMolecule().isAtomFlag(selectedAtom, FFMolecule.LIGAND));
			menu.add(action);								
			
		}
		
		
		menu.add(new JSeparator());	
		menu.add(new JSeparator());

		//File Menu		
		String name = getMolecule()==null?null: getMolecule().getName();
		JLabel fileLbl = new JLabel("Molecule: " + (name==null?"": name));
		fileLbl.setFont(fileLbl.getFont().deriveFont(Font.BOLD));
		menu.add(fileLbl);
		menu.add(new JSeparator());

		
		populateFileMenu(menu, selectedAtom);
		if(getMolecule()!=null) {
			JMenu menuDisplay = new JMenu("Display"); 
			menuDisplay.setMnemonic('D');
			menuDisplay.add(new ActionRender("Wireframe", 0, 0));		
			menuDisplay.add(new ActionRender("Sticks", 0.15, 0));		
			menuDisplay.add(new ActionRender("Balls&Sticks", .15, .3));		
			menuDisplay.add(new ActionRender("Spacefill", 0, 1));		
			menuDisplay.add(new JSeparator());		
			menuDisplay.add(new ActionSetSkeleton());		
			menuDisplay.add(new ActionRemoveSkeleton());		
			menuDisplay.add(new JSeparator());		
			menuDisplay.add(createCheckboxMenuItem(new ActionDisplay("Show Sidechains", DISPLAY_SIDECHAINS), 'k', 0, isDisplay(DISPLAY_SIDECHAINS)));		
			menuDisplay.add(createCheckboxMenuItem(new ActionDisplay("Show Ribbon", DISPLAY_RIBBON), 'b', 0, isDisplay(DISPLAY_RIBBON)));		
			menuDisplay.add(createCheckboxMenuItem(new ActionDisplay("Show Hydrogens", DISPLAY_HYDROGENS), 'h', 0, isDisplay(DISPLAY_HYDROGENS)));		
			menuDisplay.add(createCheckboxMenuItem(new ActionDisplay("Show Skeleton", DISPLAY_SKELETON), 'l', 0, isDisplay(DISPLAY_SKELETON)));		
			menu.add(menuDisplay);
	
			JMenu menuStereo = new JMenu("Stereo"); 
			ButtonGroup stereoGroup = new ButtonGroup();
			menuStereo.add(createRadioMenuItem(new ActionStereo("No stereo", STEREO_MODE_NONE), (char)0, 0, STEREO_MODE_NONE.equals(stereoMode), stereoGroup));		
			menuStereo.add(createRadioMenuItem(new ActionStereo("Stereoscopic (Cross)", STEREO_MODE_CROSS), (char)0, 0, STEREO_MODE_CROSS.equals(stereoMode), stereoGroup));		
			menuStereo.add(createRadioMenuItem(new ActionStereo("Stereoscopic (Parallel)", STEREO_MODE_PARALLEL), (char)0, 0, STEREO_MODE_PARALLEL.equals(stereoMode), stereoGroup));		
			menuStereo.add(createRadioMenuItem(new ActionStereo("Red Blue", STEREO_MODE_RED_BLUE), (char)0, 0, STEREO_MODE_RED_BLUE.equals(stereoMode), stereoGroup));		
			menuStereo.add(createRadioMenuItem(new ActionStereo("H-Interlace LR (Zalman)", STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST), (char)0, 0, STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST.equals(stereoMode), stereoGroup));		
			menuStereo.add(createRadioMenuItem(new ActionStereo("H-Interlace RL (Zalman)", STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST), (char)0, 0, STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST.equals(stereoMode), stereoGroup));		
			menuStereo.add(createRadioMenuItem(new ActionStereo("V-Interlace (Tridelity)", STEREO_MODE_V_INTERLACE), (char)0, 0, STEREO_MODE_V_INTERLACE.equals(stereoMode), stereoGroup));		
			menu.add(menuStereo);
			
			JMenu menuColors = new JMenu("Colors"); 
			menuColors.setMnemonic('C');
			ButtonGroup colorGroup = new ButtonGroup();
			menuColors.add(createRadioMenuItem(new ActionColor("Color by AtomicNo", COLOR_ATOMICNO), '7', 0, color==COLOR_ATOMICNO, colorGroup));
			menuColors.add(createRadioMenuItem(new ActionColor("Color by Chain", COLOR_CHAIN), '8', 0, color==COLOR_CHAIN, colorGroup));
			menuColors.add(createRadioMenuItem(new ActionColor("Color by Amino", COLOR_AMINO), '9', 0, color==COLOR_AMINO, colorGroup));
//			menuColors.add(createRadioMenuItem(new ActionColor("Color by Partial Charges", COLOR_PARTIALCHARGES), (char)0, 0, color==COLOR_AMINO, colorGroup));
			menu.add(menuColors);
			
			JMenu labels = new JMenu("Labels");
			labels.add(new ActionLabelAtom("None", Labeling.NONE));
			labels.add(new ActionLabelAtom("Coordinates", Labeling.COORDINATES));
			labels.add(new ActionLabelAtom("PDB", Labeling.PDB));
			labels.add(new ActionLabelAtom("MM2", Labeling.MM2));
			labels.add(new ActionLabelAtom("Descriptions", Labeling.DESCRIPTIONS));
			labels.add(new ActionLabelAtom("Partial Charges (after MOPAC or GAMESS)", Labeling.PARTIAL_CHARGES));
			menu.add(labels);
			
	
			menu.add(new JSeparator());
			{
				JMenu surface = new JMenu("Protein Surface");			
				if(mol.getNMovables()>=mol.getAllAtoms()) surface.setEnabled(false);
				
				surface.add(new ActionScript("None", "isosurface pro off"));
				surface.add(new ActionScript("Molecular", 
						"select not hetero; " +
						"isoSurface pro ignore(hetero or {elemNo=99} or water) resolution 1 molecular translucent 0.25"));
				surface.add(new ActionScript("Cavity", "select not hetero; set isosurfacePropertySmoothing off; isoSurface pro ignore(hetero or {elemNo=99} or water) pocket cavity; color isosurface green;"));
				menu.add(surface);
			}
			{
				JMenu surface = new JMenu("Ligand Surface");
				if(mol.getNMovables()==0) surface.setEnabled(false);
	
				surface.add(new ActionScript("None", "isosurface lig off"));
				surface.add(new ActionScript("Solvent Surface", 
						"select hetero; set isosurfacePropertySmoothing off; " +
						"isoSurface lig ignore(not hetero or {elemNo=99}) solvent resolution 10 fullylit color property element translucent .4"));
			
				
				ActionScript a = new ActionScript("SAS", "select hetero; set isosurfacePropertySmoothing off; isoSurface lig sasurface fullylit colorscheme jmol translucent; color isosurface greem;");				
				a.setEnabled(mol.getNMovables()<mol.getAllAtoms());
				surface.add(a);

				a = new ActionScript("Partial Charges (after MOPAC or GAMESS)", "select hetero; set isosurfacePropertySmoothing on; isosurface lig ignore(not hetero) resolution 4 molecular property partialcharge translucent 0.6");
				a.setEnabled(Math.abs(mol.getPartialCharge(0))>0.01);
				surface.add(a);
				menu.add(surface);
			}
		}
		
		menu.add(new JSeparator());
		menu.add(createMenuItem(new ActionResetView(), 'r'));


		menu.add(new JSeparator());
		menu.add(createMenuItem(new ActionSaveImage(), 'i'));
		menu.add(new JSeparator());
		menu.add(createMenuItem(new ActionCopy(), 'c', KeyEvent.CTRL_DOWN_MASK ));
//		menu.add(createMenuItem(new ActionPaste(), 'v', KeyEvent.CTRL_DOWN_MASK ));
			
		return menu;
	}
	
	private class ActionRender extends AbstractAction {
		private double bondW, atomW;
		public ActionRender(String name, double bondW, double atomW) {
			super(name);
			this.bondW = bondW;
			this.atomW = atomW;
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			P_ATOM_THICKNESS = atomW;
			P_BOND_THICKNESS = bondW;
			P_BG_COLOR = null;
			P_BG_FILE = null;
			doScheme();
		}
		
	}

	private final Set<String> usedAccelerators = new HashSet<String>(); 
	protected JMenuItem createMenuItem(final AbstractAction action, final char accelerator) {
		return createMenuItem(action, accelerator, 0);
	}
	protected JMenuItem createMenuItem(final AbstractAction action, final char accelerator, int modif) {		
		JMenuItem menu = new JMenuItem(action);
		if(accelerator>0) {
			KeyStroke s = modif==0? KeyStroke.getKeyStroke(accelerator): KeyStroke.getKeyStroke(Character.toUpperCase(accelerator), modif);
			menu.setAccelerator(s);
			if(!usedAccelerators.contains(accelerator+"_"+modif)) { 
				usedAccelerators.add(accelerator+"_"+modif);
				getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(s, "pressed " + accelerator + "_" + modif);
				getActionMap().put("pressed " + accelerator + "_" + modif, action);			
			}
		}
		return menu;
	}
	
	protected JRadioButtonMenuItem createRadioMenuItem(final AbstractAction action, final char accelerator, int modif, boolean selected, ButtonGroup group) {		
		JRadioButtonMenuItem menu = new JRadioButtonMenuItem(action);
		menu.setSelected(selected);
		group.add(menu);
		if(accelerator>0) {
			KeyStroke s = modif==0? KeyStroke.getKeyStroke(accelerator): KeyStroke.getKeyStroke(Character.toUpperCase(accelerator), modif);
			menu.setAccelerator(s);
			if(!usedAccelerators.contains(accelerator+"_"+modif)) { 
				usedAccelerators.add(accelerator+"_"+modif);
				getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(s, "pressed "+accelerator + "_" + modif);
				getActionMap().put("pressed "+accelerator + "_" + modif, action);
			}
		}
		return menu;
	}
	
	protected JCheckBoxMenuItem createCheckboxMenuItem(final AbstractAction action, final char accelerator, int modif, boolean selected) {		
		JCheckBoxMenuItem menu = new JCheckBoxMenuItem(action);
		menu.setSelected(selected);
		if(accelerator>0) {
			KeyStroke s = modif==0? KeyStroke.getKeyStroke(accelerator): KeyStroke.getKeyStroke(Character.toUpperCase(accelerator), modif);
			menu.setAccelerator(s);
			if(!usedAccelerators.contains(accelerator+"_"+modif)) { 
				usedAccelerators.add(accelerator+"_"+modif);
				getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(s, "pressed "+accelerator + "_" + modif);
				getActionMap().put("pressed "+accelerator + "_" + modif, action);
			}
		}
		return menu;
	}
	
	public class ActionCopy extends AbstractAction {
		public ActionCopy() {
			super("Copy");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			copy();
		}
	}
	
	public void copy() {
//		copyPasteMolecule = StructureCalculator.extractLigand(getMolecule());
		FFMolecule lig = StructureCalculator.extractLigand(getMolecule());
		StructureCalculator.deleteHydrogens(lig);		
		StereoMolecule mol = lig.toStereoMolecule();
		mol.ensureHelperArrays(Molecule.cHelperCIP);
		new CoordinateInventor().invent(mol);
		mol.setStereoBondsFromParity();
		new ClipboardHandler().copyMolecule(mol);				
	}
	

	private static int nPaste = 0;	
	public static FFMolecule getFromClipboard() {
		FFMolecule m;
		if(copyPasteMolecule==null) {
			StereoMolecule mol = new ClipboardHandler().pasteMolecule();
			if(mol==null) return null;
			List<String> errors = new ArrayList<String>();
			m = AdvancedTools.orientMoleculeForParity(mol, false, errors).get(0);
			if(errors.size()>0) JExceptionDialog.show(null, errors);
			if(m.getName()==null || m.getName().length()==0) {
				m.setName("Clipboard-" + (++nPaste));
			}
		} else {
			m = copyPasteMolecule;
		}
		return m;
	}
	
	private class ActionSaveImage extends AbstractAction {
		public ActionSaveImage() {super("Save Image");}
		@Override
		public void actionPerformed(ActionEvent ev) {
			try {
				JFileChooser chooser = new JFileChooser();
				chooser.setSelectedFile(new File("c:/", (getMolecule().getName()==null?"molecule":getMolecule().getName())+ ".png"));
				int res = chooser.showSaveDialog(MoleculeViewer.this);
				if(res!=JFileChooser.APPROVE_OPTION) return;
				saveImage(chooser.getSelectedFile());
				JOptionPane.showMessageDialog(
				MoleculeViewer.this,
					"<html><font color=#000000>The image has been saved to<br> <i>"
						+ chooser.getSelectedFile()
						+ "</i></font></html>",
					"Image saved",
					JOptionPane.PLAIN_MESSAGE);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(
					MoleculeViewer.this,
					"The image could not be saved\n" + e,
					"Error",
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private class PaintingThread extends Thread {
		Graphics2D g;
		float d;
		Rectangle rectClip = new Rectangle();
		public PaintingThread(Graphics2D g, float d) {
			this.g = g;
			this.d = d;
		}
		@Override
		public void run() {
			viewer.transformManager.rotateYRadians(d, null);
			
			 g.getClipBounds(rectClip);
			    if (dimSize.width == 0) return;
			    try {
			       viewer.renderScreenImage(g, dimSize, rectClip);
			    } catch (Exception e) {
			    	//Ignore
			    	System.err.println("DisplayPanel.paint() error: "+e);
				}
			//MoleculeViewer.super.paint(g);
			viewer.transformManager.rotateYRadians(-d, null);
		}
	}
	
	@Override
	public synchronized void paint(Graphics g) {
		if(STEREO_MODE_V_INTERLACE.equals(stereoMode)) {
			Point s = new Point(0, 0);
	        SwingUtilities.convertPointToScreen(s, this);	        
			boolean even = (s.x & 1)==0;
				
				
			//Creates buffer
			int width = getWidth();
			int height = getHeight();
			BufferedImage buf = new BufferedImage(width*2, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = (Graphics2D) buf.getGraphics();
			final float d = (float) (2.5 * Math.PI/180);
			
			viewer.transformManager.rotateYRadians(-d, null);
			super.paint(g2);
			viewer.transformManager.rotateYRadians(2*d, null);
			g2.translate(width,0);
			super.paint(g2);
			g2.translate(-width,0);
			viewer.transformManager.rotateYRadians(-d, null);

            //Interlace
			int width2 = 2 * width;
			DataBufferInt b = (DataBufferInt) buf.getRaster().getDataBuffer();
			int[] rgb = b.getData();
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
			g.drawImage(buf, even?0:1, 0, width, height, 0, 0, width-(even?0:1), height, this);
		} else if(STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST.equals(stereoMode) || STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST.equals(stereoMode)) {

			Point s = new Point(0, 0);
	        SwingUtilities.convertPointToScreen(s, this);	        
			boolean even = (s.y & 1)== (STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST.equals(stereoMode)? 0: 1);
			
			//Creates buffer
			int width = getWidth();
			int height = getHeight();
			//int height2 = 2 * height;
			//BufferedImage buf = new BufferedImage(width, height2, BufferedImage.TYPE_INT_RGB);

			final float d = (float) (2.5 * Math.PI/180);
			BufferedImage bufLeft = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D gLeft = (Graphics2D) bufLeft.getGraphics();
			PaintingThread threadLeft = new PaintingThread(gLeft, -d);
			
			
			BufferedImage bufRight = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D gRight = (Graphics2D) bufRight.getGraphics();
			PaintingThread threadRight = new PaintingThread(gRight, d);
			
			threadLeft.run();			
			threadRight.run();
			/*
			try {
				threadLeft.join();
				System.out.println("Left Done");
			} catch (Exception e) {
				// TODO: handle exception
			}
			try {
				threadRight.join();
				System.out.println("Right Done");
			} catch (Exception e) {
				// TODO: handle exception
			}*/
			
            //Interlace
			DataBufferInt bLeft = (DataBufferInt) bufLeft.getRaster().getDataBuffer();
			DataBufferInt bRight = (DataBufferInt) bufRight.getRaster().getDataBuffer();
			int[] rgbLeft = bLeft.getData();
			int[] rgbRight = bRight.getData();
			for (int y = 0; y+1 < height; y+=2) {
				{
					int i = y * width;
					int j = (y+1) * width;
					for (int x = 0; x < width; x++) {
						rgbLeft[i] = ((rgbLeft[i] & 0xFEFEFEFE) >> 1) + ((rgbLeft[j] & 0xFEFEFEFE) >> 1);
						rgbLeft[j] = ((rgbRight[i] & 0xFEFEFEFE) >> 1) + ((rgbRight[j] & 0xFEFEFEFE) >> 1);
						//rgbLeft[i] = (rgbLeft[i] & 0xFEFEFEFE);
						//rgbLeft[j] = (rgbRight[i] & 0xFEFEFEFE);
						i++;
						j++;
					}
				}
			}			
			g.drawImage(bufLeft, 0, even?0:1, width, height, 0, 0, width, height-(even?0:1), this);
			//script("set refreshing true;");

		} else {
			super.paint(g);
		}
	}
	
	
	/**
	 * Saves the Image to the specified filename 
	 */
	public void saveImage(File file) throws Exception {
		BufferedImage image = new BufferedImage(Math.max(100, getWidth()), Math.max(100, getHeight()), BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics(); 
		paint(g);		
		g.dispose();
		ImageIO.write(image, "png", new FileOutputStream(file));
	}
	
	
	public void optimize(final boolean hydrogenOnly) {
		new Thread() {
			@Override
			public void run() {
				startRefresh(100);
				try{					
					AdvancedTools.optimizeByVibrating(mol, hydrogenOnly, 150);
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
				stopRefresh();
			}		
		}.start();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////
	public class ActionSetAtomicNo extends AbstractAction {
		private final int atm;
		private final int atomicNo;
		public ActionSetAtomicNo(int atm, int atomicNo) {
			super(Molecule.cAtomLabel[atomicNo]);
			this.atm = atm;
			this.atomicNo = atomicNo;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			getMolecule().setAtomicNo(atm, atomicNo);
			optimize(true);
		}
	}	
	
	public class ActionSetRotationCenter extends AbstractAction {
		private final int atm;
		public ActionSetRotationCenter(int atm) {
			super("Set As Center of Rotation");
			this.atm = atm;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			Coordinates c = mol.getCoordinates(atm);
			script("center {" + c.toStringSpaceDelimited() + "};");
		}
	}
	
	public class ActionSetLigand extends AbstractAction {
		private final int atm;
		public ActionSetLigand(int atm) {
			super("Set As Ligand");
			this.atm = atm;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			ToolSelectMarkLigand.markLigand(MoleculeViewer.this, atm);
		}
	}
	
	
	public class ActionChangeTorsion extends AbstractAction {
		private final int atm;
		private final boolean smallestGroup;
		public ActionChangeTorsion(int atm, boolean smallestGroup) {
			super("Rotate " + (smallestGroup?"Smallest Group": "Biggest Group"));
			this.atm = atm;
			this.smallestGroup = smallestGroup;
			setEnabled(ToolTorsion.canRotate(getMolecule(), atm, smallestGroup));
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new ToolTorsion(MoleculeViewer.this, atm, smallestGroup);
		}
	}	

	public class ActionMoveAtom extends AbstractAction {
		private final int atm;
		public ActionMoveAtom(int atm) {
			super("Move Atom");
			this.atm = atm;
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			new ToolMoveAtom(MoleculeViewer.this, atm);
		}
	}	

	public class ActionRing extends AbstractAction {
		private final int atm;
		private final int type;
		public ActionRing(int atm, int type) {
			super(type==0? "Chair Conformation-1": type==1? "Chair Conformation-2": type==2? "Boat Conformation-1": "Boat Conformation-2");
			this.atm = atm;
			this.type = type;
			
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				AdvancedTools.changeRingConformationByAtom(getMolecule(), atm, type);
				viewer.refresh(mol);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(MoleculeViewer.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}	

	public class ActionColor extends AbstractAction {
		private final int c;
		public ActionColor(String text, int c) {
			super(text);
			this.c = c;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setColor(c);
		}
	}
	
	public class ActionMode extends AbstractAction {
		private final int c;
		public ActionMode(String text, int c) {
			super(text);
			this.c = c;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setMode(c, !isMode(c));
		}
	}
	
	public class ActionDisplay extends AbstractAction {
		private final int c;
		public ActionDisplay(String text, int c) {
			super(text);
			this.c = c;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			toggleDisplay(c);
		}
	}	
	public class ActionStereo extends AbstractAction {
		private final String c;
		public ActionStereo(String text, String c) {
			super(text);
			this.c = c;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setStereo(c);
		}
	}
	
	private class ActionResetView extends AbstractAction {
		public ActionResetView() {
			super("Reset Camera");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			doScheme();
			cleanup();
			center();
		}
	}
	
	private class ActionScript extends AbstractAction {
		private String script = null;
		private String previousMessage = null;
		public ActionScript() {
			super("Script");
		}
		public ActionScript(String name, String script) {
			super(name);
			this.script = script;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(script==null) {
				String s = JOptionPane.showInputDialog(MoleculeViewer.this, "JMOL Script", previousMessage);
				if(s==null) return;
				System.out.println(viewer.script(s));
				previousMessage = s;
			} else {
				viewer.script(script);
			}
		}
	}
	
	

	private class ActionLabelAtom extends AbstractAction {
		private Labeling label;
		
		public ActionLabelAtom(String name, Labeling label) {
			super(name);
			this.label = label;
			
			if(label==Labeling.PARTIAL_CHARGES || label==Labeling.DESCRIPTIONS || label==Labeling.PDB) {
				setEnabled(false);
				for (int a=0; a<mol.getAllAtoms(); a++) {
					if(label==Labeling.PARTIAL_CHARGES && Math.abs(mol.getPartialCharge(a))>.1) {setEnabled(true); break;}
					if(label==Labeling.DESCRIPTIONS && mol.getAtomDescription(a)!=null && mol.getAtomDescription(a).length()>0) {setEnabled(true); break;}
					if(label==Labeling.PDB && mol.getAtomAmino(a)!=null && mol.getAtomAmino(a).length()>0) {setEnabled(true); break;}
				}
			}
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			setLabeling(label);

		}
	}
	

	private class ActionMarkLigand extends AbstractAction {
		private int atom;
		
		public ActionMarkLigand(int atom) {
			super("Mark as Ligand");
			this.atom = atom;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			ToolSelectMarkLigand.markLigand(MoleculeViewer.this, atom);
		}
	}
	
	private class ActionMarkProtein extends AbstractAction {
		private int atom;
		
		public ActionMarkProtein(int atom) {
			super("Mark as Protein");
			this.atom = atom;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			ToolSelectMarkLigand.markProtein(MoleculeViewer.this, atom);
		}
	}
	

	public class ActionLabel extends AbstractAction {
		private Labeling lbl;
		public ActionLabel(String name, Labeling labeling) {super(name); this.lbl = labeling;}
		@Override
		public void actionPerformed(ActionEvent e) {
			setLabeling(lbl);
		}
	}

	
	public void setLabeling(Labeling label) {
		if(label==Labeling.NONE) {
			cleanup();
		} else if(label==Labeling.COORDINATES) {
			cleanup();
			for(int i = 0; i<mol.getAllAtoms(); i++) {	
				if((display & DISPLAY_HYDROGENS)==0 && mol.getAtomicNo(i)<=1) continue;				
				script("select {atomno="+(i+1)+"}; label \"" + i + ". "+mol.getCoordinates(i).toStringSpaceDelimited()+"\"");
			}
		} else if(label==Labeling.PDB) {
			cleanup();
			script("select *; label %U");
		} else if(label==Labeling.MM2) {
			if(mol==null) return;
			//int[] a2g = StructureCalculator.getAtomToGroups(mol);
			cleanup();
			try {
				MM2Parameters.getInstance().setAtomClassesForMolecule(mol);
			} catch(Exception ex) {
				ex.printStackTrace();						
			}		
			for(int i = 0; i<mol.getAllAtoms(); i++) {	
				//if(a2g[i]!=a2g[atm]) continue;				
				if((display & DISPLAY_HYDROGENS)==0 && mol.getAtomicNo(i)<=1) continue;				
				script("select {atomno="+(i+1)+"}; label \"" + i + ". " +mol.getAtomMM2Description(i)+"\"");
			}
		} else if(label==Labeling.DESCRIPTIONS) {
			if(mol==null) return;
			//int[] a2g = StructureCalculator.getAtomToGroups(mol);
			cleanup();
			for(int i = 0; i<mol.getAllAtoms(); i++) {	
				if(mol.getAtomDescription(i)==null) continue;	
				script("select {atomno="+(i+1)+"}; label \"   " + mol.getAtomDescription(i) + "\" ");
			}
		} else if(label==Labeling.PARTIAL_CHARGES) {
			if(mol==null) return;
			script("select *; label %P");
		}
		
	}
	
	public int findNearestBondIndex(int x, int y) {
		return viewer.modelSet == null? -1 : viewer.modelSet.findNearestBondIndex(x, y);
	}
	public int findNearestAtomIndex(int x, int y) {
		return viewer.modelSet == null? -1 : viewer.modelSet.findNearestAtomIndex(x, y);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getButton()==MouseEvent.BUTTON3) {
			getMolecule().reorderAtoms();
			int atm = viewer.findNearestAtomIndex(e.getX(), e.getY());
			JPopupMenu popupMenu = createPopupMenu(atm);
			popupMenu.show(this, e.getX(), e.getY());
		} 
		if(e.getButton()!=MouseEvent.BUTTON1) return;		
		if(tool!=null) tool.mouseClicked(e);			
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if(tool!=null) tool.mouseEntered(e);			
	}

	@Override
	public void mouseExited(MouseEvent e) {
		if(tool!=null) tool.mouseExited(e);			
	}

	@Override
	public void mousePressed(MouseEvent e) {
		requestFocusInWindow();
		if(e.getButton()!=MouseEvent.BUTTON1) return;
		if(tool!=null) tool.mousePressed(e);
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.getButton()!=MouseEvent.BUTTON1) return;
		if(tool!=null) tool.mouseReleased(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(tool!=null && (tool instanceof MouseMotionListener)) {							
			((MouseMotionListener) tool).mouseDragged(e);
		}	
	}


	private int haloAtom = -1;
	private int haloBond = -1;
	@Override
	public void mouseMoved(MouseEvent e) {
		
		
		int atom = findNearestAtomIndex(e.getX(), e.getY());
		int bond = findNearestBondIndex(e.getX(), e.getY());
		boolean ok = tool!=null && tool.hover(atom, bond);
		
		if(bond>=0 && ok) {
			int a1 = mol.getBondAtom(0, bond);
			int a2 = mol.getBondAtom(1, bond);
			haloBond = bond;
			script("draw halobond cylinder {atomno="+(a1+1)+"} {atomno="+(a2+1)+"} diameter " + (mol.getBondOrder(bond)==1?.5: .8) + " translucent .45;");			
		} else if(haloBond>=0) {
			haloBond = -1;
			script("draw halobond delete;");
		}
		
		if(atom>=0 && ok) {
			haloAtom = atom;
			script("draw halocircle circle {atomno="+(atom+1)+"} diameter 1.1 translucent .45;");
		} else if(haloAtom>=0) {
			haloAtom = -1;
			script("draw halocircle delete;");
		}
		
		if(tool!=null && (tool instanceof MouseMotionListener)) {							
			((MouseMotionListener) tool).mouseMoved(e);
		}			
	}
	
	private class RefreshThread extends Thread {
		private int refreshRate = -1;
		@Override
		public void run() {
			while(true) {
				
				try{if(refreshRate>0) {refresh();}}catch (Exception e) {}
				try {sleep(refreshRate>0? refreshRate: 100);}catch(Exception e){}				
			}
		}
		public void setRefreshRate(int refreshRate) {this.refreshRate = refreshRate;}
	}
	
	private class ActionSetSkeleton extends AbstractAction{
		public ActionSetSkeleton() {
			super("Set current ligand as skeleton");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setSkeleton(StructureCalculator.extractLigand(mol));
		}
	}
	private class ActionRemoveSkeleton extends AbstractAction{
		public ActionRemoveSkeleton() {
			super("Remove skeleton");
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			setSkeleton(null);
		}
	}
	
	
	private final RefreshThread refreshThread = new RefreshThread();
	
	
	
	public void startRefresh(int ms) {
		synchronized (refreshThread) {
			refreshThread.setRefreshRate(ms);
			refreshThread.notify();			
		}
	}
	
	public void stopRefresh() {
		synchronized (refreshThread) {
			refreshThread.setRefreshRate(-1);
			refreshThread.notify();
		}
		refresh();
	}
	
	public void stopRotation(boolean stop) {
		viewer.stopRotation(stop);
	}


	public Coordinates getCavityCenter() {
		return cavityCenter;
	}

	public Coordinates getRotationCenter() {
		Point3f c = viewer.transformManager.getRotationCenter();
		return new Coordinates(c.x, c.y, c.z);
	}

	public void setRotationCenter(Coordinates center) {
		script("center {" + center.toStringSpaceDelimited() + "};");
	}


	public void setCavityCenter(Coordinates cavityCenter) {
		this.cavityCenter = cavityCenter;
	}

	public Console getConsole() {
		return console;
	}

	public void setConsole(Console console) {
		this.console = console;
	}
	
	/**
	 * @return the viewer
	 */
	public ViewerWrapper getViewer() {
		return viewer;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}

}
