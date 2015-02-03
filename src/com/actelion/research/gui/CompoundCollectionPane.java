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
 * @author Thomas Sander
 */

package com.actelion.research.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.MolfileV3Creator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.clipboard.IClipboardHandler;
import com.actelion.research.gui.dnd.MoleculeDragAdapter;
import com.actelion.research.gui.dnd.MoleculeDropAdapter;
import com.actelion.research.gui.dnd.MoleculeTransferable;

public class CompoundCollectionPane<T> extends JScrollPane
            implements ActionListener,CompoundCollectionListener,MouseListener,MouseMotionListener,StructureListener {
    private static final long serialVersionUID = 0x20060904;

    private static final String ADD = "Add...";
    private static final String EDIT = "Edit...";
    private static final String REMOVE = "Remove";
    private static final String REMOVE_ALL = "Remove All";
    private static final String COPY = "Copy";
    private static final String PASTE = "Paste";
    private static final String OPEN = "Add From File...";
    private static final String SAVE_DWAR = "Save DataWarrior-File...";
    private static final String SAVE_SDF2 = "Save SD-File V2...";
    private static final String SAVE_SDF3 = "Save SD-File V3...";

    public static final int FILE_SUPPORT_NONE = 0;
    public static final int FILE_SUPPORT_OPEN_FILES = 1;
    public static final int FILE_SUPPORT_SAVE_FILES = 2;
    public static final int FILE_SUPPORT_OPEN_AND_SAVE_FILES = 3;

    private static final int ALLOWED_DRAG_ACTIONS = DnDConstants.ACTION_COPY_OR_MOVE;
    private static final int ALLOWED_DROP_ACTIONS = DnDConstants.ACTION_COPY_OR_MOVE;

    private final static int cWhiteSpace = 4;

	private CompoundCollectionModel<T> mModel;
    private IClipboardHandler   mClipboardHandler;
	private int			        mDisplayMode,mSelectedIndex,mHighlightedIndex,
	                            mEditedIndex,mFileSupport;
	private Dimension           mContentSize;
	private JPanel              mContentPanel;
	private boolean             mIsVertical,mIsEditable,mIsSelectable,mCreateFragments,
	                            mShowDropBorder,mIsEnabled,mShowValidationError;

	/**
	 * This is a visual component to display and edit a compound collection maintained
	 * by a CompoundCollectionModel. Three variations of DefaultCompoundCollectionModel
	 * (.Native, .Molecule, and .IDCode) are available, which internally keep molecule
	 * instance as Object, StereoMolecule or String, respectively. If one of these
	 * default model is used, than the CompoundCollectionPane's T must match this class.
	 * @param model
	 * @param isVertical
	 */
	public CompoundCollectionPane(CompoundCollectionModel<T> model, boolean isVertical) {
		this(model, isVertical, 0, ALLOWED_DRAG_ACTIONS, ALLOWED_DROP_ACTIONS);
		}

	public CompoundCollectionPane(CompoundCollectionModel<T> model, boolean isVertical, int displayMode,
	                              int dragAction, int dropAction) {
	    mModel = model;
	    mModel.addCompoundCollectionListener(this);
	    mIsEnabled = true;
        mIsVertical = isVertical;
        mDisplayMode = displayMode;
        mFileSupport = FILE_SUPPORT_OPEN_AND_SAVE_FILES;
        mSelectedIndex = -1;
        mHighlightedIndex = -1;
		init();
        initializeDragAndDrop(dragAction, dropAction);
		}

	public CompoundCollectionModel<T> getModel() {
	    return mModel;
	    }

	public void setEnabled(boolean b) {
	    super.setEnabled(b);
	    if (mIsVertical)
	        getVerticalScrollBar().setEnabled(b);
	    else
            getHorizontalScrollBar().setEnabled(b);
	    mIsEnabled = b;
	    repaint();
	    }

	/**
	 * Defines, whether the list and individual structures can be edited.
	 * @param editable
	 */
	public void setEditable(boolean editable) {
	    mIsEditable = editable;
        updateMouseListening();
	    }

	/**
	 * Defines, whether the popup menu contains 'Open' and/or 'Save' items.
	 * As default both, OPEN and SAVE options are active.
	 * @param fileSupport one of the FILE_SUPPORT_... options
	 */
	public void setFileSupport(int fileSupport) {
		mFileSupport = fileSupport;
		}

	public void setSelectable(boolean selectable) {
	    mIsSelectable = selectable;
	    updateMouseListening();
	    }

	/**
	 * Defines whether new created structures are fragments of molecules.
	 * @param createFragments
	 */
	public void setCreateFragments(boolean createFragments) {
	    mCreateFragments = createFragments;
	    }

	/**
	 * Defines whether a large red question mark is shown
	 * in case of a structure validation error. 
	 * @param showError
	 */
	public void setShowValidationError(boolean showError) {
	    mShowValidationError = showError;
	    }

	/**
     *  call this in order to get clipboard support
     */
    public void setClipboardHandler(IClipboardHandler h) {
        mClipboardHandler = h;
        }

    public IClipboardHandler getClipboardHandler() {
        return mClipboardHandler;
        }

	public void actionPerformed(ActionEvent e) {
	    if (e.getActionCommand().equals(COPY) && mHighlightedIndex != -1) {
	        mClipboardHandler.copyMolecule(mModel.getMolecule(mHighlightedIndex));
	        }
	    else if (e.getActionCommand().equals(PASTE)) {
            int index = (mHighlightedIndex == -1) ? mModel.getSize() : mHighlightedIndex;
            StereoMolecule mol = mClipboardHandler.pasteMolecule();
	        if (mol != null) {
	        	mol.setFragment(mCreateFragments);
	            mModel.addMolecule(index, mol);
	        	}
            }
        else if (e.getActionCommand().equals(ADD)) {
            editStructure(-1);
            }
	    else if (e.getActionCommand().equals(EDIT) && mHighlightedIndex != -1) {
	        editStructure(mHighlightedIndex);
	        }
        else if (e.getActionCommand().equals(REMOVE) && mHighlightedIndex != -1) {
            mModel.remove(mHighlightedIndex);
            mHighlightedIndex = -1;
            }
        else if (e.getActionCommand().equals(REMOVE_ALL)) {
            mModel.clear();
            mHighlightedIndex = -1;
            }
        else if (e.getActionCommand().equals(OPEN)) {
        	ArrayList<StereoMolecule> compounds = new FileHelper(null).readStructuresFromFile(false);
        	if (compounds != null) {
        		for (StereoMolecule compound:compounds)
        			compound.setFragment(mCreateFragments);
        		mModel.addMoleculeList(compounds);
        		}
        	}
        else if (e.getActionCommand().equals(SAVE_DWAR)) {
        	String filename = new FileHelper(null).selectFileToSave(
        			"Save DataWarrior File", FileHelper.cFileTypeDataWarrior, "Untitled");
        	if (filename != null) {
	        	try {
	        		String title = mCreateFragments ? "Fragment" : "Structure";
	        		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
	        		writer.write("<datawarrior-fileinfo>");
	        		writer.newLine();
	        		writer.write("<version=\"3.1\">");
	        		writer.newLine();
	        		writer.write("<rowcount=\""+mModel.getSize()+"\">");
	        		writer.newLine();
	        		writer.write("</datawarrior-fileinfo>");
	        		writer.newLine();
	        		writer.write("<column properties>");
	        		writer.newLine();
	        		writer.write("<columnName=\""+title+"\">");
	        		writer.newLine();
	        		writer.write("<columnProperty=\"specialType	idcode\">");
	        		writer.newLine();
	        		writer.write("</column properties>");
	        		writer.newLine();
	        		writer.write(title);
	        		writer.newLine();
	        		for (int i=0; i<mModel.getSize(); i++) {
	        			if (mModel instanceof DefaultCompoundCollectionModel.IDCode)
	        				writer.write((String)mModel.getCompound(i));
	        			else
	        				writer.write(new Canonizer(mModel.getMolecule(i)).getIDCode());
	            		writer.newLine();
	        			}
	    			writer.close();
	        		}
	        	catch (IOException ioe) {
	                JOptionPane.showMessageDialog(null, ioe.toString());
	        		}
        		}
        	}
        else if (e.getActionCommand().equals(SAVE_SDF2)
        	  || e.getActionCommand().equals(SAVE_SDF3)) {
        	String version = "Version " + (e.getActionCommand().equals(SAVE_SDF2) ? "2" : "3");
        	String filename = new FileHelper(null).selectFileToSave(
        			"Save SD-File "+version, FileHelper.cFileTypeSD, "Untitled");
        	if (filename != null) {
        		try {
	    			BufferedWriter theWriter = new BufferedWriter(new FileWriter(filename));
	
	    			for (int i=0; i<mModel.getSize(); i++) {
	    				StereoMolecule mol = mModel.getMolecule(i);
	
	                    if (e.getActionCommand().equals(SAVE_SDF3))
	                        new MolfileV3Creator(mol).writeMolfile(theWriter);
	                    else
	                        new MolfileCreator(mol).writeMolfile(theWriter);
	
	    				theWriter.write("$$$$");
	    				theWriter.newLine();
	    				}
	    			theWriter.close();
        			}
	        	catch (IOException ioe) {
	                JOptionPane.showMessageDialog(null, ioe.toString());
	        		}
        		}
        	}
	    }

	private void editStructure(int index) {
        Component c = this.getParent();
        while (!(c instanceof Frame))
            c = c.getParent();

        mEditedIndex = index;
        StereoMolecule mol = null;
        if (index == -1) {
        	mol = new StereoMolecule();
            mol.setFragment(mCreateFragments);
        	}
        else {
        	mol = mModel.getMolecule(mEditedIndex);
        	}
        JDrawDialog theDialog = new JDrawDialog((Frame)c, mol);
        theDialog.addStructureListener(this);
        theDialog.setVisible(true);
	    }

	private void updateMouseListening() {
	    if (mIsSelectable || mIsEditable) {
	        addMouseListener(this);
	        addMouseMotionListener(this);
	        }
	    else {
	        removeMouseListener(this);
	        removeMouseMotionListener(this);
	        }
	    }

	private void init() {
		mContentSize = new Dimension();
		mContentPanel = new JPanel() {
            private static final long serialVersionUID = 0x20060904;

            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

				validateSize();

				Rectangle clipRect = g.getClipBounds();

				g.setColor(Color.white);
				g.fillRect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);

		        if (mModel.getSize() != 0) {
    		        int i1 = Math.max(0, mIsVertical ? clipRect.y / mContentSize.width
    		                                         : clipRect.x / mContentSize.height);
    		        int i2 = Math.min(mModel.getSize(), mIsVertical ? 1+(clipRect.y+clipRect.height) / mContentSize.width
		                                                            : 1+(clipRect.x+clipRect.width) / mContentSize.height);

    		        for (int i=i1; i<i2; i++) {
                        Rectangle bounds = getMoleculeBounds(i);

                        StereoMolecule compound = mModel.getMoleculeForDisplay(i);
    					if (mShowValidationError) {
    					    try {
    					        compound.validate();
    					        }
    					    catch (Exception e) {
    					        int size = Math.min(bounds.width, bounds.height);
    					        g.setColor(new Color(255, 192, 192));
    					        g.setFont(new Font("Helvetica", Font.BOLD, size));
    					        FontMetrics m = g.getFontMetrics();
    					        Rectangle2D b = m.getStringBounds("?", g);
    					        g.drawString("?", bounds.x+(bounds.width-(int)b.getWidth())/2, bounds.y+(bounds.height-(int)b.getHeight())/2+m.getAscent());
    					        }
    					    }
    
    					Depictor2D d = new Depictor2D(compound, mDisplayMode);
    					d.validateView(g,
    								   new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height),
    								   AbstractDepictor.cModeInflateToMaxAVBL);
    					d.setDefaultColor(mIsEnabled ? Molecule.cAtomColorBlack : AbstractDepictor.cColorGray);
    					d.paint(g);

    					if (mSelectedIndex == i || mHighlightedIndex == i) {
    					    g.setColor(!mIsEnabled ? Color.GRAY 
    					             : (mSelectedIndex != i) ? Color.BLUE
    					             : (mHighlightedIndex != i) ? Color.RED : Color.MAGENTA);
                            g.drawRect(bounds.x-2, bounds.y-2, bounds.width+3, bounds.height+3);
    					    g.drawRect(bounds.x-1, bounds.y-1, bounds.width+1, bounds.height+1);
    					    }

    					if (mShowDropBorder) {
    			            g.setColor(Color.gray);
    			            Rectangle vb = getViewport().getViewRect();
    			            g.drawRect(vb.x, vb.y, vb.width-1,vb.height-1);
    			            g.drawRect(vb.x+1, vb.y+1, vb.width-3, vb.height-3);
    					    }      
    					}
					}
				}
			};
		setHorizontalScrollBarPolicy(mIsVertical ? JScrollPane.HORIZONTAL_SCROLLBAR_NEVER : JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		setVerticalScrollBarPolicy(mIsVertical ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		setViewportView(mContentPanel);
		}

    public void collectionUpdated(int fromIndex, int toIndex) {
        if (mSelectedIndex >= fromIndex && mSelectedIndex <= toIndex)
            mSelectedIndex = -1;
        if (mHighlightedIndex >= fromIndex && mHighlightedIndex <= toIndex)
            mHighlightedIndex = -1;

        repaint();
        }

	private Rectangle getMoleculeBounds(int molIndex) {
        int x = cWhiteSpace/2;
        int y = cWhiteSpace/2;
        int displaySize = mIsVertical ? mContentSize.width
                                      : mContentSize.height;
        if (mIsVertical)
            y += molIndex * displaySize;
        else
            x += molIndex * displaySize;

	    return new Rectangle(x, y, displaySize-cWhiteSpace, displaySize-cWhiteSpace);
	    }

    private int getMoleculeIndex(int x, int y) {
        if (mModel.getSize() == 0 || mContentSize.width == 0)
            return -1;

        Point p = getViewport().getViewPosition();
        int index = (mIsVertical) ? (y+p.y) / mContentSize.width
                                  : (x+p.x) / mContentSize.height;
        return (index < mModel.getSize()) ? index : -1;
        }

	public void mouseClicked(MouseEvent e) {
        if (mIsEnabled && mIsEditable && e.getClickCount() == 2 && mHighlightedIndex != -1) {
            editStructure(mHighlightedIndex);
            }
	    }

	public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
        if (mIsEnabled) {
            if (e.isPopupTrigger()) {
                handlePopupTrigger(e);
                }
            else if (mIsSelectable) {
                int index = getMoleculeIndex(e.getX(), e.getY());
                if (mSelectedIndex != index) {
                    mSelectedIndex = index;
                    setSelection(index);
                    repaint();
                    }
                }
            }
        }

    public void mouseReleased(MouseEvent e) {
        if (mIsEnabled && e.isPopupTrigger())
            handlePopupTrigger(e);
        }

    public void mouseDragged(MouseEvent e) {}

    public void mouseMoved(MouseEvent e) {
        if (mIsEnabled) {
            int index = getMoleculeIndex(e.getX(), e.getY());
            if (mHighlightedIndex != index) {
                mHighlightedIndex = index;
                repaint();
                }
            }
        }

    private void handlePopupTrigger(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem(ADD);
        item.addActionListener(this);
        popup.add(item);
        if (mHighlightedIndex != -1) {
            item = new JMenuItem(EDIT);
            item.addActionListener(this);
            popup.add(item);
            item = new JMenuItem(REMOVE);
            item.addActionListener(this);
            popup.add(item);
            }

        if (mModel.getSize() != 0) {
            item = new JMenuItem(REMOVE_ALL);
            item.addActionListener(this);
            popup.add(item);
        	}

        if (mClipboardHandler != null) {
            popup.addSeparator();
            if (mHighlightedIndex != -1) {
                item = new JMenuItem(COPY);
                item.addActionListener(this);
                popup.add(item);
                }
            item = new JMenuItem(PASTE);
            item.addActionListener(this);
            popup.add(item);
            }

        if (mFileSupport != 0) {
            popup.addSeparator();
            if ((mFileSupport & FILE_SUPPORT_OPEN_FILES) != 0) {
                item = new JMenuItem(OPEN);
                item.addActionListener(this);
                popup.add(item);
                }
            if ((mFileSupport & FILE_SUPPORT_SAVE_FILES) != 0 && mModel.getSize() != 0) {
	            item = new JMenuItem(SAVE_DWAR);
	            item.addActionListener(this);
	            popup.add(item);
	            item = new JMenuItem(SAVE_SDF2);
	            item.addActionListener(this);
	            popup.add(item);
	            item = new JMenuItem(SAVE_SDF3);
	            item.addActionListener(this);
	            popup.add(item);
            	}
            }

        popup.show(this, e.getX(), e.getY());
        }

    public void structureChanged(StereoMolecule mol) {
    	if (mEditedIndex == -1) {	// new structure
    		if (mol.getAllAtoms() != 0)
    			mModel.addMolecule(mModel.getSize(), mol);
    		}
    	else {
	        if (mol.getAllAtoms() == 0)
	            mModel.remove(mEditedIndex);
	        else
	            mModel.setMolecule(mEditedIndex, (StereoMolecule)mol);
    		}
        }

    /**
     * May be overridden to act on selection changes
     * @param molIndex
     */
    public void setSelection(int molIndex) {}

	private void validateSize() {
		Rectangle viewportBounds = getViewportBorderBounds();

		int size = mIsVertical ? viewportBounds.width : viewportBounds.height;
		int width = size;
		int height = size;

		if (mIsVertical) {
            height *= mModel.getSize();
            if (height < viewportBounds.height)
                height = viewportBounds.height;
		    }
		else {
		    width *= mModel.getSize();
	        if (width < viewportBounds.width)
	            width = viewportBounds.width;
		    }

		if (mContentSize.width != width
		 || mContentSize.height != height) {
			mContentSize.width = width;
			mContentSize.height = height;
	        mContentPanel.setPreferredSize(mContentSize);
			mContentPanel.revalidate();
		    }
		}

    private void initializeDragAndDrop(int dragAction, int dropAction) {
        if (dragAction != DnDConstants.ACTION_NONE) {
            new MoleculeDragAdapter(this) {
                public Transferable getTransferable(Point p) {
                    if (mHighlightedIndex == -1)
                        return null;
                    return new MoleculeTransferable(mModel.getMolecule(mHighlightedIndex));
                    }
                };
            }

        if (dropAction != DnDConstants.ACTION_NONE) {
            MoleculeDropAdapter d = new MoleculeDropAdapter() {
                public void onDropMolecule(StereoMolecule mol, Point pt) {
                    if (mIsEnabled && mIsEditable && mol != null && mol.getAllAtoms() != 0) {
                        for (int atom=0; atom<mol.getAllAtoms(); atom++) {
                            mol.setAtomColor(atom, Molecule.cAtomColorBlack); // don't copy atom coloring
                            }

                        int index = (mHighlightedIndex == -1) ? mModel.getSize() : mHighlightedIndex;
                        mol.setFragment(mCreateFragments);
                        mModel.addMolecule(index, mol);
                        }
                    updateBorder(false);
                    }

                public void dragEnter(DropTargetDragEvent e) {
                    boolean drop = mIsEnabled && mIsEditable && isDropOK(e) ;
                    if (!drop)
                        e.rejectDrag();
                    updateBorder(drop);
                    }

                public void dragExit(DropTargetEvent e) {
                    updateBorder(false);
                    }
                };

            new DropTarget(this, dropAction, d, true, new OurFlavorMap());
            }
        }

    private void updateBorder(boolean showBorder) {
        if (mIsEnabled && mIsEditable && (mShowDropBorder != showBorder)) {
            mShowDropBorder = showBorder;
            repaint();
            }
        }

        // This class is needed for inter-jvm drag&drop. Although not neccessary for standard environments, it prevents
        // nasty "no native data was transfered" errors. It still might create ClassNotFoundException in the first place by
        // the SystemFlavorMap, but as I found it does not hurt, since the context classloader will be installed after
        // the first call. I know, that this depends heavely on a specific behaviour of the systemflavormap, but for now
        // there's nothing I can do about it.
    static class OurFlavorMap implements java.awt.datatransfer.FlavorMap {
        public java.util.Map<DataFlavor,String> getNativesForFlavors(DataFlavor[] dfs) {
            java.awt.datatransfer.FlavorMap m = java.awt.datatransfer.SystemFlavorMap.getDefaultFlavorMap();
            return m.getNativesForFlavors(dfs);
            }

        public java.util.Map<String,DataFlavor> getFlavorsForNatives(String[] natives) {
            java.awt.datatransfer.FlavorMap m = java.awt.datatransfer.SystemFlavorMap.getDefaultFlavorMap();
            return m.getFlavorsForNatives(natives);
            }
        }
    }
