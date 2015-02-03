/*
* @(#)ClipboardHandler.java
*
* Copyright 2005 Actelion Ltd. All Rights Reserved.
*
* This software is the proprietary information of Actelion Ltd.
* Use is subject to license terms.
*
 */
package com.actelion.research.gui.clipboard;

import com.actelion.research.chem.*;
import com.actelion.research.chem.io.RXNFileCreator;
import com.actelion.research.chem.io.RXNFileParser;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.gui.ReactionDepictor;
import com.actelion.research.gui.wmf.WMF;
import com.actelion.research.gui.wmf.WMFGraphics2D;
import com.actelion.research.util.Sketch;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;

/**
 * <p>Title: Actelion Library</p>
 * <p>Description: Actelion Java Library</p>
 * <p>Copyright: Copyright (c) 2002-2003</p>
 * <p>Company: Actelion Ltd</p>
 *
 * @author Thomas Sander, Christian Rufener
 * @version 1.0
 */
public class ClipboardHandler implements IClipboardHandler
{
    private static final byte MDLSK[] = {(byte) 'M', (byte) 'D', (byte) 'L', (byte) 'S', (byte) 'K', 0, 0};

    /**
     * Get a Molecule from the Clipboard. The supported formats are: MDLSK,MDLCT,MDL_MOL,CF_ENHMETAFILE with embedded sketch
     *
     * @return Molecule found or null if no molecule present on the clipboard
     */
    public StereoMolecule pasteMolecule()
    {
        byte[] buffer = null;
        StereoMolecule mol = null;

        if ((buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_SERIALIZEMOLECULE)) != null) {
            try {
                ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(buffer));
                Object o = is.readObject();
                System.out.println("Object read from Bytearray input " + o);
                if (o instanceof StereoMolecule) {
                    mol = (StereoMolecule) o;
                }
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("NativeClipboardAccessor.pasteMolecule(): Exception " + e);
            }
        }
        System.out.println("Mol is " + mol);
        if (mol == null) {
            if ((buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_CTAB)) != null || (buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_MOLFILE)) != null) {
                MolfileParser p = new MolfileParser();
                mol = new StereoMolecule();
                if (!p.parse(mol, new String(buffer))) {
                    mol = null;
                    System.err.println("Error Parsing CTAB during clipboard paste");
                }
            }
            if (mol == null) {
                if ((buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_SKETCH)) != null || (buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_EMBEDDEDSKETCH)) != null) {
                    try {
                        mol = new StereoMolecule();
                        if (!Sketch.createMolFromSketchBuffer(mol, buffer)) {
                            mol = null;
                        }
                    } catch (IOException e) {
                        mol = null;
                        e.printStackTrace();
                        System.out.println("NativeClipboardAccessor.pasteMolecule(): Exception " + e);
                    }
                }
            }
        }
        if (mol != null && is3DMolecule(mol)) {
            mol.ensureHelperArrays(Molecule.cHelperParities);    // to ensure stereo parities
            new CoordinateInventor().invent(mol);
            mol.setStereoBondsFromParity();
        }

        System.out.println("returned Mol is " + mol);
        return mol;
    }


    /**
     * Get a Reaction from the Clipboard
     *
     * @return Reaction or null if no reaction present
     */
    public Reaction pasteReaction()
    {
        byte[] buffer = null;
        Reaction rxn = null;


        if ((buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_SERIALIZEMOLECULE)) != null) {
            try {
                ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(buffer));
                Object o = is.readObject();
                if (o instanceof Reaction) {
                    rxn = (Reaction) o;
                }
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("NativeClipboardAccessor.pasteMolecule(): Exception " + e);
            }
        } else if ((buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_CTAB)) != null || (buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_MOLFILE)) != null) {
            RXNFileParser p = new RXNFileParser();
            rxn = new Reaction();
            try {
                if (!p.parse(rxn, new String(buffer)))
                    rxn = null;
            } catch (Exception e) {
                System.err.println("Error parsing Reaction Buffer " + e);
                rxn = null;
            }
        } else if ((buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_SKETCH)) != null
//			  || (buffer = NativeClipboardAccessor.getClipboardData(NativeClipboardAccessor.NC_METAFILE)) != null) {
            || (buffer = NativeClipboardHandler.getClipboardData(NativeClipboardHandler.NC_EMBEDDEDSKETCH)) != null) {
            try {
                rxn = new Reaction();
                if (!Sketch.createReactionFromSketchBuffer(rxn, buffer)) {
                    rxn = null;
                }
            } catch (IOException e) {
                rxn = null;
            }
        }
        return rxn;
    }

    public boolean copyMolecule(String molfile)
    {
        StereoMolecule m = new StereoMolecule();
        MolfileParser p = new MolfileParser();
        p.parse(m, molfile);
        return copyMolecule(m);
    }


    /**
     * Copies a molecule to the clipboard in various formats:
     * ENHMETAFILE with an embedded sketch
     * MDLSK Sketch
     * MDLCT MDL molfile
     */
    public boolean copyMolecule(StereoMolecule mol)
    {
        boolean ok = false;
        try {
            StereoMolecule m = mol.getCompactCopy();
            for (int atom=0; atom<m.getAllAtoms(); atom++)
            	m.setAtomMapNo(atom, 0, false);

            byte buffer[] = Sketch.createSketchFromMol(m);

            File temp = File.createTempFile("actnca", ".wmf");
            temp.deleteOnExit();

            if (writeMol2Metafile(temp, m, buffer)) {
                // Serialize to a byte array
                System.out.println("CopyMolecule");
                com.actelion.research.gui.clipboard.external.ChemDrawCDX cdx = new com.actelion.research.gui.clipboard.external.ChemDrawCDX();
                byte[] cdbuffer = cdx.getChemDrawBuffer(m);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(m);
                out.close();
                bos.close();
//				ok = NativeClipboardHandler.copyMoleculeToClipboard(temp.getAbsolutePath(),buffer,bos.toByteArray());
                ok = NativeClipboardHandler.copyMoleculeToClipboard(temp.getAbsolutePath(), cdbuffer, bos.toByteArray());
                temp.delete();
            }
        } catch (IOException e) {
            System.err.println("Error copying Molecule " + e);
        }
        return ok;
    }


    /**
     * Copies a molecule to the clipboard in various formats:
     * ENHMETAFILE with an embedded sketch
     * MDLSK Sketch
     * MDLCT MDL molfile
     */
    public boolean copyReaction(Reaction r)
    {
        boolean ok = false;
        try {
            Reaction rxn = makeRXNCopy(r);
            RXNFileCreator mc = new RXNFileCreator(rxn);
            String ctab = mc.getRXNfile();
            ok = copyReactionToClipboard(rxn, ctab);
        } catch (IOException e) {
            System.err.println("Error Copying Reaction " + e);
        }
        return ok;
    }

    private Reaction makeRXNCopy(Reaction r)
    {
        Reaction rxn = new Reaction(r);
        int mols = rxn.getMolecules();
        for (int i = 0; i < mols; i++) {
            rxn.getMolecule(i).ensureHelperArrays(Molecule.cHelperCIP);
        }
        return rxn;
    }
    /**
     * Copies a molecule to the clipboard in various formats:
     * ENHMETAFILE with an embedded sketch
     * MDLSK Sketch
     * MDLCT MDL molfile
     */
    public boolean copyReaction(String ctab)
    {
        boolean ok = false;
        try {
            Reaction rxn = new Reaction();
            RXNFileParser p = new RXNFileParser();
            p.parse(rxn, ctab);
            ok = copyReactionToClipboard(rxn, ctab);
        } catch (IOException e) {
            System.err.println("Error copy reaction " + e);
        } catch (Exception e) {
            System.err.println("Error copy reaction " + e);
        }
        return ok;
    }

    private boolean copyReactionToClipboard(Reaction m, String molfile) throws IOException
    {
        boolean ok = false;
        byte buffer[] = Sketch.createSketchFromReaction(m);
        File temp = File.createTempFile("actnca", ".wmf");
        temp.deleteOnExit();
        if (writeRXN2Metafile(temp, buffer, m)) {
            com.actelion.research.gui.clipboard.external.ChemDrawCDX cdx = new com.actelion.research.gui.clipboard.external.ChemDrawCDX();
            byte[] cdbuffer = cdx.getChemDrawBuffer(m);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(m);
            out.close();
            bos.close();
            byte t[] = bos.toByteArray();
            System.out.println("Reaction copy with serialized object " + (t != null) + " " + (t != null ? t.length : 0));

//            ok = NativeClipboardHandler.copyReactionToClipboard(temp.getAbsolutePath(),buffer,bos.toByteArray());
            ok = NativeClipboardHandler.copyReactionToClipboard(temp.getAbsolutePath(), cdbuffer, bos.toByteArray());
            temp.delete();
        }
        return ok;
    }

    private boolean writeMol2Metafile(File temp, StereoMolecule m, byte[] sketch)
    {
        boolean ok = false;
        try {
            ok = writeMol2Metafile(new FileOutputStream(temp), m, sketch);
        } catch (Exception e) {
            System.err.println("error writing molfile " + e);
        }
        return ok;
    }


    private boolean writeMol2Metafile(OutputStream out, StereoMolecule m, byte[] sketch) throws IOException
    {
        int w = 300;
        int h = 200;
        WMF wmf = new WMF();
        WMFGraphics2D g = new WMFGraphics2D(wmf, w, h, Color.black, Color.white);

        Depictor d = new Depictor(m);
        d.updateCoords(g, new Rectangle2D.Float(0, 0, w, h), AbstractDepictor.cModeInflateToMaxAVBL);
        d.paint(g);

        if (sketch != null) {
            byte temp[] = new byte[MDLSK.length + sketch.length];
            System.arraycopy(MDLSK, 0, temp, 0, MDLSK.length);
            System.arraycopy(sketch, 0, temp, MDLSK.length, sketch.length);
            wmf.escape(WMF.MFCOMMENT, temp);
        }
        wmf.writeWMF(out);
        out.close();
//		g.dispose();
        return true;
    }

    private boolean writeRXN2Metafile(File temp, byte sketch[], Reaction m)
    {
        try {
            return writeRXN2Metafile(new FileOutputStream(temp), sketch, m);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean writeRXN2Metafile(OutputStream out, byte sketch[], Reaction m) throws IOException
    {
        int w = 400;
        int h = 300;
        WMF wmf = new WMF();
        WMFGraphics2D g = new WMFGraphics2D(wmf, w, h, Color.black, Color.white);
        ReactionDepictor d = new ReactionDepictor(m);
        d.updateCoords(g, 8, 8, w - 16, h - 16, AbstractDepictor.cModeInflateToMaxAVBL);
        d.paint(g);
        if (sketch != null) {
//			byte MDLSK[] = {(byte)'M',(byte)'D',(byte)'L',(byte)'S',(byte)'K',0,0};
            byte temp[] = new byte[MDLSK.length + sketch.length];
            System.arraycopy(MDLSK, 0, temp, 0, MDLSK.length);
            System.arraycopy(sketch, 0, temp, MDLSK.length, sketch.length);
            wmf.escape(WMF.MFCOMMENT, temp);
        }
        wmf.writeWMF(out);
        out.close();
        return true;
    }

    /**
     * outcommented to avoid knowledge about ConformationSampler
     * (something like this should be done in place if a 3D molecule is desired)
     *
     public ExtendedMolecule pasteMolecule3D()
     {
     ExtendedMolecule mol = NativeClipboardAccessor.pasteMolecule();

     if (mol != null && !is3DMolecule(mol)) {
     mol.ensureHelperArrays(Molecule.cHelperParities);	// to ensure stereo parities
     new ConformationSampler((StereoMolecule)mol).generateConformer(false);
     }

     return mol;
     }
     */

    /**
     * Copies an Image to the clipboard
     *
     * @param img Image to be copied
     * @return true on success
     */
    public boolean copyImage(java.awt.Image img)
    {
        return ImageClipboardHandler.copyImage(img);
    }

    public java.awt.Image pasteImage()
    {
        return ImageClipboardHandler.pasteImage();
    }

    private boolean is3DMolecule(ExtendedMolecule mol)
    {
        for (int atom = 0; atom < mol.getAllAtoms(); atom++)
            if (mol.getAtomZ(atom) != 0.0)
                return true;

        return false;
    }

    /**
     * @deprecated Use ImageClipboardHandler.pasteImage for consistency reasons
     */
    public static Image getImage()
    {
        return ImageClipboardHandler.pasteImage();
    }

    /**
     * @deprecated You may use ImageClipboardHandler.copyImage for consistency reasons
     */
    public static void putImage(Image image)
    {
        ImageClipboardHandler.copyImage(image);
    }
}
