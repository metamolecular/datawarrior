/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-08-24 06:08:26 +0200 (lun., 24 août 2009) $
 * $Revision: 11332 $
 *
 * Copyright (C) 2000-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.popup;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Properties;

import org.jmol.i18n.GT;

class PopupResourceBundle {

  PopupResourceBundle(String menuStructure, Properties menuText) {
    buildStructure(menuStructure);
    localize(menuStructure != null, menuText);
  }

  String getMenu(String title) {
    return "# Jmol.mnu " + title + "\n\n" +
           "# Part I -- Menu Structure\n" +
           "# ------------------------\n\n" +
           dumpStructure(menuContents) + "\n\n" +
           "# Part II -- Key Definitions\n" +
           "# --------------------------\n\n" +
           dumpStructure(structureContents) + "\n\n" +
           "# Part III -- Word Translations\n" +
           "# -----------------------------\n\n" +
           dumpWords();
  }
  
  String getStructure(String key) {
    return structure.getProperty(key);
  }

  void addStructure(String key, String value) {
    structure.setProperty(key, value);
  }

  String getWord(String key) {
    String str = words.getProperty(key);
    return (str == null ? key : str);
  }

  // Properties to store menu structure and contents
  private Properties structure = new Properties();
  private Properties words = new Properties();
  
  private static String Box(String cmd) {
    return "if not(showBoundBox);if not(showUnitcell);boundbox on;"+cmd+";boundbox off;else;"+cmd+";endif;endif;";
  }
  private static String[][] menuContents = {
    
      {   "@COLOR", "black white red orange yellow green cyan blue indigo violet"},      
      {   "@AXESCOLOR", "gray salmon maroon olive slateblue gold orchid"},
      
      {   "popupMenu",
          "modelSetMenu SIGNEDloadMenu SIGNEDwriteMenu FRAMESbyModelComputedMenu configurationComputedMenu - selectMenuText viewMenu renderMenu colorMenu - surfaceMenu SYMMETRYUNITCELLMenu - "
              + "zoomMenu spinMenu VIBRATIONMenu "
              + "FRAMESanimateMenu - "
              + "measureMenu pickingMenu - JVM12showConsole JVM12showMenu - "
              + "languageComputedMenu aboutComputedMenu" },
              
      {   "selectMenuText",
          "hideNotSelectedCheckbox showSelectionsCheckbox - selectAll selectNone invertSelection - elementsComputedMenu SYMMETRYSelectComputedMenu - "
              + "PDBproteinMenu PDBnucleicMenu PDBheteroMenu PDBcarboMenu PDBnoneOfTheAbove" },

      {   "PDBproteinMenu", 
          "PDBaaResiduesComputedMenu - "
              + "allProtein proteinBackbone proteinSideChains - "
              + "polar nonpolar - "
              + "positiveCharge negativeCharge noCharge" },
              
      {   "PDBcarboMenu",
          "PDBcarboResiduesComputedMenu - allCarbo" },

      {   "PDBnucleicMenu",
          "PDBnucleicResiduesComputedMenu - allNucleic nucleicBackbone nucleicBases - DNA RNA - "
              + "atPairs auPairs gcPairs" },
              
      {   "PDBheteroMenu",
          "PDBheteroComputedMenu - allHetero Solvent Water - "
              + "Ligand exceptWater nonWaterSolvent" },

      {   "viewMenu",
          "front left right top bottom back" },

      {   "renderMenu",
          "perspectiveDepthCheckbox showBoundBoxCheckbox showUNITCELLCheckbox showAxesCheckbox stereoMenu - renderSchemeMenu - atomMenu labelMenu bondMenu hbondMenu ssbondMenu - "
              + "PDBstructureMenu [set_axes]Menu [set_boundbox]Menu [set_UNITCELL]Menu" },

      {   "renderSchemeMenu",
          "renderCpkSpacefill renderBallAndStick "
              + "renderSticks renderWireframe PDBrenderCartoonsOnly PDBrenderTraceOnly" },
                            
      {   "atomMenu",
          "showHydrogensCheckbox - atomNone - "
              + "atom15 atom20 atom25 atom50 atom75 atom100" },

      {   "bondMenu",
          "bondNone bondWireframe - "
              + "bond100 bond150 bond200 bond250 bond300" },

      {   "hbondMenu",
          "PDBhbondCalc hbondNone hbondWireframe - "
              + "PDBhbondSidechain PDBhbondBackbone - "
              + "hbond100 hbond150 hbond200 hbond250 hbond300" },

      {   "ssbondMenu",
          "ssbondNone ssbondWireframe - "
              + "PDBssbondSidechain PDBssbondBackbone - "
              + "ssbond100 ssbond150 ssbond200 ssbond250 ssbond300" },

      {   "PDBstructureMenu",
          "structureNone - "
              + "backbone cartoon cartoonRockets ribbons rockets strands trace" },

      {   "VIBRATIONvectorMenu",
          "vectorOff vectorOn vector3 vector005 vector01 - "
              + "vectorScale02 vectorScale05 vectorScale1 vectorScale2 vectorScale5" },

      {   "stereoMenu",
          "stereoNone stereoRedCyan stereoRedBlue stereoRedGreen stereoCrossEyed stereoWallEyed" },

      {   "labelMenu",
          "labelNone - " + "labelSymbol labelName labelNumber - "
              + "labelPositionMenu" },

      {   "labelPositionMenu",
          "labelCentered labelUpperRight labelLowerRight labelUpperLeft labelLowerLeft" },

      {   "colorMenu",
          "colorrasmolCheckbox - [color_atoms]Menu [color_bonds]Menu [color_hbonds]Menu [color_ssbonds]Menu colorPDBStructuresMenu [color_isosurface]Menu"
              + " - [color_labels]Menu [color_vectors]Menu - [color_axes]Menu [color_boundbox]Menu [color_UNITCELL]Menu [color_background]Menu" },

      { "[color_atoms]Menu", "schemeMenu - @COLOR - opaque translucent" },
      { "[color_bonds]Menu", "none - @COLOR - opaque translucent" },
      { "[color_hbonds]Menu", null },
      { "[color_ssbonds]Menu", null },
      { "[color_labels]Menu", null },
      { "[color_vectors]Menu", null },
      { "[color_backbone]Menu", "none - schemeMenu - @COLOR - opaque translucent" },
      { "[color_cartoon]sMenu", null },
      { "[color_ribbon]sMenu", null },
      { "[color_rockets]Menu", null },
      { "[color_strands]Menu", null },
      { "[color_trace]Menu", null },
      { "[color_background]Menu", "@COLOR" },
      { "[color_isosurface]Menu", "@COLOR - opaque translucent" },
      { "[color_axes]Menu", "@AXESCOLOR" },
      { "[color_boundbox]Menu", null },
      { "[color_UNITCELL]Menu", null },


      {   "colorPDBStructuresMenu",
          "[color_backbone]Menu [color_cartoon]sMenu [color_ribbon]sMenu [color_rockets]Menu [color_strands]Menu [color_trace]Menu" },

      {   "schemeMenu",
          "cpk - formalcharge partialcharge#CHARGE - altloc#PDB amino#PDB chain#PDB group#PDB molecule monomer#PDB shapely#PDB structure#PDB relativeTemperature#BFACTORS fixedTemperature#BFACTORS" },

      {   "zoomMenu",
          "zoom50 zoom100 zoom150 zoom200 zoom400 zoom800 - "
              + "zoomIn zoomOut" },

      {   "spinMenu",
          "spinOn spinOff - " + "[set_spin_X]Menu [set_spin_Y]Menu [set_spin_Z]Menu - "
              + "[set_spin_FPS]Menu" },

      {   "VIBRATIONMenu", 
          "vibrationOff vibrationOn VIBRATIONvectorMenu" },

      {   "FRAMESanimateMenu",
          "animModeMenu - play pause resume stop - nextframe prevframe rewind - playrev restart - "
              + "FRAMESanimFpsMenu" },

      {   "FRAMESanimFpsMenu", 
          "animfps5 animfps10 animfps20 animfps30 animfps50" },

      {   "measureMenu",
          "showMeasurementsCheckbox - "
              + "measureOff measureDistance measureAngle measureTorsion - "
              + "measureDelete JVM12measureList - distanceNanometers distanceAngstroms distancePicometers" },

      {   "pickingMenu",
          "pickOff pickCenter pickIdent pickLabel pickAtom "
              + "pickMolecule pickElement PDBpickChain PDBpickGroup SYMMETRYpickSite pickSpin" },


      {   "JVM12showMenu",
          "showHistory showFile showFileHeader - "
              + "showOrient showMeasure - "
              + "showSpacegroup showState SYMMETRYshowSymmetry UNITCELLshow - showIsosurface showMo - extractMOL" },


      {   "SIGNEDloadMenu",
          "loadPdb loadFileOrUrl loadFileUnitCell - loadScript" },

      {   "SIGNEDwriteMenu",
          "writeFileTextVARIABLE writeState writeHistory - writeJpg writePng writePovray -  writeIsosurface writeVrml writeX3d writeMaya" },


      { "[set_spin_X]Menu", "s0 s5 s10 s20 s30 s40 s50" },
      { "[set_spin_Y]Menu", null },
      { "[set_spin_Z]Menu", null },
      { "[set_spin_FPS]Menu", null },

      {   "animModeMenu", 
          "onceThrough palindrome loop" },


      {   "surfaceMenu",
          "surfDots surfVDW surfSolventAccessible14 surfSolvent14 surfMolecular CHARGEsurfMEP surfMoComputedMenu - surfOpaque surfTranslucent surfOff" },

      {   "SYMMETRYUNITCELLMenu",
          "SYMMETRYShowComputedMenu SYMMETRYhide UNITCELLone UNITCELLnine UNITCELLnineRestricted UNITCELLninePoly" },

      {   "[set_axes]Menu", 
          "off#axes dotted - byPixelMenu byAngstromMenu" },

      { "[set_boundbox]Menu", null },
      { "[set_UNITCELL]Menu", null },

      {   "byPixelMenu", 
          "1p 3p 5p 10p" },

      {   "byAngstromMenu", 
          "10a 20a 25a 50a 100a" },

/*
 *       {   "optionsMenu", 
          "rasmolChimeCompatibility" },
*/

      {   "aboutComputedMenu", 
          "APPLETjmolUrl APPLETmouseManualUrl APPLETtranslationUrl" },

  };
  
  
  
  private static String[][] structureContents = {

      { "colorrasmolCheckbox", ""},
      { "hideNotSelectedCheckbox", "set hideNotSelected true | set hideNotSelected false; hide(none)" },
      { "perspectiveDepthCheckbox", ""},
      { "showAxesCheckbox", "set showAxes true | set showAxes false;set axesMolecular" },
      { "showBoundBoxCheckbox", ""},
      { "showHydrogensCheckbox", ""},
      { "showMeasurementsCheckbox", ""},
      { "showSelectionsCheckbox", ""},
      { "showUNITCELLCheckbox", ""},

      { "selectAll", "SELECT all" },
      { "selectNone", "SELECT none" },
      { "invertSelection", "SELECT not selected" },
   
      { "allProtein", "SELECT protein" },
      { "proteinBackbone", "SELECT protein and backbone" },
      { "proteinSideChains", "SELECT protein and not backbone" },
      { "polar", "SELECT protein and polar" },
      { "nonpolar", "SELECT protein and not polar" },
      { "positiveCharge", "SELECT protein and basic" },
      { "negativeCharge", "SELECT protein and acidic" },
      { "noCharge", "SELECT protein and not (acidic,basic)" },
      { "allCarbo", "SELECT carbohydrate" },

      { "allNucleic", "SELECT nucleic" },
      { "DNA", "SELECT dna" },
      { "RNA", "SELECT rna" },
      { "nucleicBackbone", "SELECT nucleic and backbone" },
      { "nucleicBases", "SELECT nucleic and not backbone" },
      { "atPairs", "SELECT a,t" },
      { "gcPairs", "SELECT g,c" },
      { "auPairs", "SELECT a,u" },
      { "A", "SELECT a" },
      { "C", "SELECT c" },
      { "G", "SELECT g" },
      { "T", "SELECT t" },
      { "U", "SELECT u" },

      { "allHetero", "SELECT hetero" },
      { "Solvent", "SELECT solvent" },
      { "Water", "SELECT water" },
      // same as ligand    { "exceptSolvent", "SELECT hetero and not solvent" },
      { "nonWaterSolvent", "SELECT solvent and not water" },
      { "exceptWater", "SELECT hetero and not water" },
      { "Ligand", "SELECT ligand" },

      // not implemented    { "Lipid", "SELECT lipid" },
      { "PDBnoneOfTheAbove", "SELECT not(hetero,protein,nucleic,carbohydrate)" },

      { "front", Box( "moveto 2.0 front;delay 1" ) },
      { "left", Box( "moveto 1.0 front;moveto 2.0 left;delay 1"  ) },
      { "right", Box( "moveto 1.0 front;moveto 2.0 right;delay 1"  ) },
      { "top", Box( "moveto 1.0 front;moveto 2.0 top;delay 1"  ) },
      { "bottom", Box( "moveto 1.0 front;moveto 2.0 bottom;delay 1"  ) },
      { "back", Box( "moveto 1.0 front;moveto 2.0 back;delay 1"  ) },

      { "renderCpkSpacefill", "restrict not selected;select not selected;spacefill 100%;color cpk" },
      { "renderBallAndStick", "restrict not selected;select not selected;spacefill 20%;wireframe 0.15;color cpk" },
      { "renderSticks", "restrict not selected;select not selected;wireframe 0.3;color cpk" },
      { "renderWireframe", "restrict not selected;select not selected;wireframe on;color cpk" },
      { "PDBrenderCartoonsOnly", "restrict not selected;select not selected;cartoons on;color structure" },
      { "PDBrenderTraceOnly", "restrict not selected;select not selected;trace on;color structure" },

      { "atomNone", "cpk off" },
      { "atom15", "cpk 15%" },
      { "atom20", "cpk 20%" },
      { "atom25", "cpk 25%" },
      { "atom50", "cpk 50%" },
      { "atom75", "cpk 75%" },
      { "atom100", "cpk on" },

      { "bondNone", "wireframe off" },
      { "bondWireframe", "wireframe on" },
      { "bond100", "wireframe .1" },
      { "bond150", "wireframe .15" },
      { "bond200", "wireframe .2" },
      { "bond250", "wireframe .25" },
      { "bond300", "wireframe .3" },

      { "PDBhbondCalc", "hbonds calculate" },
      { "hbondNone", "hbonds off" },
      { "hbondWireframe", "hbonds on" },
      { "PDBhbondSidechain", "set hbonds sidechain" },
      { "PDBhbondBackbone", "set hbonds backbone" },
      { "hbond100", "hbonds .1" },
      { "hbond150", "hbonds .15" },
      { "hbond200", "hbonds .2" },
      { "hbond250", "hbonds .25" },
      { "hbond300", "hbonds .3" },

      { "ssbondNone", "ssbonds off" },
      { "ssbondWireframe", "ssbonds on" },
      { "PDBssbondSidechain", "set ssbonds sidechain" },
      { "PDBssbondBackbone", "set ssbonds backbone" },
      { "ssbond100", "ssbonds .1" },
      { "ssbond150", "ssbonds .15" },
      { "ssbond200", "ssbonds .2" },
      { "ssbond250", "ssbonds .25" },
      { "ssbond300", "ssbonds .3" },

      { "structureNone",
          "backbone off;cartoons off;ribbons off;rockets off;strands off;trace off;" },
      { "backbone", "restrict not selected;select not selected;backbone 0.3" },
      { "cartoon", "restrict not selected;select not selected;set cartoonRockets false;cartoons on" },
      { "cartoonRockets", "restrict not selected;select not selected;set cartoonRockets;cartoons on" },
      { "ribbons", "restrict not selected;select not selected;ribbons on" },
      { "rockets", "restrict not selected;select not selected;rockets on" },
      { "strands", "restrict not selected;select not selected;strands on" },
      { "trace", "restrict not selected;select not selected;trace 0.3" },

      { "vibrationOff", "vibration off" },
      { "vibrationOn", "vibration on" },

      { "vectorOff", "vectors off" },
      { "vectorOn", "vectors on" },
      { "vector3", "vectors 3" },
      { "vector005", "vectors 0.05" },
      { "vector01", "vectors 0.1" },
      { "vectorScale02", "vector scale 0.2" },
      { "vectorScale05", "vector scale 0.5" },
      { "vectorScale1", "vector scale 1" },
      { "vectorScale2", "vector scale 2" },
      { "vectorScale5", "vector scale 5" },

      { "stereoNone", "stereo off" },
      { "stereoRedCyan", "stereo redcyan 3" },
      { "stereoRedBlue", "stereo redblue 3" },
      { "stereoRedGreen", "stereo redgreen 3" },
      { "stereoCrossEyed", "stereo -5" },
      { "stereoWallEyed", "stereo 5" },

      { "labelNone", "label off" },
      { "labelSymbol", "label %e" },
      { "labelName", "label %a" },
      { "labelNumber", "label %i" },

      { "labelCentered", "set labeloffset 0 0" },
      { "labelUpperRight", "set labeloffset 4 4" },
      { "labelLowerRight", "set labeloffset 4 -4" },
      { "labelUpperLeft", "set labeloffset -4 4" },
      { "labelLowerLeft", "set labeloffset -4 -4" },

      { "zoom50", "zoom 50" },
      { "zoom100", "zoom 100" },
      { "zoom150", "zoom 150" },
      { "zoom200", "zoom 200" },
      { "zoom400", "zoom 400" },
      { "zoom800", "zoom 800" },
      { "zoomIn", "move 0 0 0 40 0 0 0 0 1" },
      { "zoomOut", "move 0 0 0 -40 0 0 0 0 1" },

      { "spinOn", "spin on" },
      { "spinOff", "spin off" },

      { "s0", "0" },
      { "s5", "5" },
      { "s10", "10" },
      { "s20", "20" },
      { "s30", "30" },
      { "s40", "40" },
      { "s50", "50" },

      { "onceThrough", "anim mode once#" },
      { "palindrome", "anim mode palindrome#" },
      { "loop", "anim mode loop#" },
      { "play", "anim play#" },
      { "pause", "anim pause#" },
      { "resume", "anim resume#" },
      { "stop", "anim off#" },
      
      { "nextframe", "frame next#" },
      { "prevframe", "frame prev#" },
      { "playrev", "anim playrev#" },
      
      { "rewind", "anim rewind#" },
      { "restart", "anim on#" },
      
      { "animfps5", "anim fps 5#" },
      { "animfps10", "anim fps 10#" },
      { "animfps20", "anim fps 20#" },
      { "animfps30", "anim fps 30#" },
      { "animfps50", "anim fps 50#" },

      { "measureOff", "set pickingstyle MEASURE OFF; set picking OFF" },
      { "measureDistance",
          "set pickingstyle MEASURE; set picking MEASURE DISTANCE" },
      { "measureAngle", "set pickingstyle MEASURE; set picking MEASURE ANGLE" },
      { "measureTorsion",
          "set pickingstyle MEASURE; set picking MEASURE TORSION" },
      { "measureDelete", "measure delete" },
      { "JVM12measureList", "console on;show measurements" },
      { "distanceNanometers", "select *; set measure nanometers" },
      { "distanceAngstroms", "select *; set measure angstroms" },
      { "distancePicometers", "select *; set measure picometers" },

      { "pickOff", "set picking off" },
      { "pickCenter", "set picking center" },
      //    { "pickDraw" , "set picking draw" },
      { "pickIdent", "set picking ident" },
      { "pickLabel", "set picking label" },
      { "pickAtom", "set picking atom" },
      { "PDBpickChain", "set picking chain" },
      { "pickElement", "set picking element" },
      { "PDBpickGroup", "set picking group" },
      { "pickMolecule", "set picking molecule" },
      { "SYMMETRYpickSite", "set picking site" },
      { "pickSpin", "set picking spin" },

      { "JVM12showConsole", "console" },
      { "showFile", "console on;show file" },
      { "showFileHeader", "console on;getProperty FileHeader" },
      { "showHistory", "console on;show history" },
      { "showIsosurface", "console on;show isosurface" },
      { "showMeasure", "console on;show measure" },
      { "showMo", "console on;show mo" },
      { "showModel", "console on;show model" },
      { "showOrient", "console on;show orientation" },
      { "showSpacegroup", "console on;show spacegroup" },
      { "showState", "console on;show state" },
      
      { "loadPdb", "load ?PdbId?" },      
      { "loadFileOrUrl", "load ?" },      
      { "loadFileUnitCell", "load ? {1 1 1}" },      
      { "loadScript", "script ?.spt" },      

      { "writeFileTextVARIABLE", "write file \"?FILE?\"" },      
      { "writeState", "write state \"?FILEROOT?.spt\"" },      
      { "writeHistory", "write history \"?FILEROOT?.his\"" },     
      { "writeIsosurface", "write isosurface \"?FILEROOT?.jvxl\"" },      
      { "writeJpg", "write image \"?FILEROOT?.jpg\"" },      
      { "writePng", "write image \"?FILEROOT?.png\"" },      
      { "writePovray", "write POVRAY \"?FILEROOT?.pov\"" },      
      { "writeVrml", "write VRML \"?FILEROOT?.wrl\"" },      
      { "writeX3d", "write X3D \"?FILEROOT?.x3d\"" },      
      { "writeMaya", "write MAYA \"?FILEROOT?.ma\"" },       
      { "SYMMETRYshowSymmetry", "console on;show symmetry" },
      { "UNITCELLshow", "console on;show unitcell" },
      { "extractMOL", "console on;getproperty extractModel \"visible\" " },

      { "surfDots", "dots on" },
      { "surfVDW", "isosurface delete resolution 0 solvent 0 translucent" },
      { "surfMolecular", "isosurface delete resolution 0 molecular translucent" },
      { "surfSolvent14",
          "isosurface delete resolution 0 solvent 1.4 translucent" },
      { "surfSolventAccessible14",
          "isosurface delete resolution 0 sasurface 1.4 translucent" },
      { "CHARGEsurfMEP",
          "isosurface delete resolution 0 molecular map MEP translucent" },
      { "surfOpaque", "mo opaque;isosurface opaque" },
      { "surfTranslucent", "mo translucent;isosurface translucent" },
      { "surfOff", "mo delete;isosurface delete;select *;dots off" },
      { "SYMMETRYhide", "draw sym_* delete" },
      { "UNITCELLone",
          "save orientation;load \"\" {1 1 1} ;restore orientation;center" },
      { "UNITCELLnine",
          "save orientation;load \"\" {444 666 1} ;restore orientation;center" },
      { "UNITCELLnineRestricted",
          "save orientation;load \"\" {444 666 1} ;restore orientation; unitcell on; display cell=555;center visible;zoom 200" },
      { "UNITCELLninePoly",
          "save orientation;load \"\" {444 666 1} ;restore orientation; unitcell on; display cell=555; polyhedra 4,6 (displayed);center (visible);zoom 200" },

      { "1p", "on" },
      { "3p", "3" },
      { "5p", "5" },
      { "10p", "10" },

      { "10a", "0.1" },
      { "20a", "0.20" },
      { "25a", "0.25" },
      { "50a", "0.50" },
      { "100a", "1.0" },

//      { "rasmolChimeCompatibility",
//        "set color rasmol; set zeroBasedXyzRasmol on; "
  //          + "set axesOrientationRasmol on; load \"\"; select *; cpk off; wireframe on; " },

      { "APPLETjmolUrl", "show url \"http://www.jmol.org\"" },
      { "APPLETmouseManualUrl", "show url \"http://wiki.jmol.org/index.php/Mouse_Manual\"" },
      { "APPLETtranslationUrl", "show url \"http://wiki.jmol.org/index.php/Internationalisation\"" }, };
  
  private String[] getWordContents() {
    
    boolean wasTranslating = GT.getDoTranslate();
    if (!wasTranslating)
      GT.setDoTranslate(true);
    String[] words = new String[] {
        "modelSetMenu", GT.translate("No atoms loaded"),
        
        "configurationComputedMenu", GT.translate("Configurations"),
        "elementsComputedMenu", GT.translate("Element"),
        "FRAMESbyModelComputedMenu", GT.translate("Model/Frame"),
        "languageComputedMenu", GT.translate("Language"),
        "PDBaaResiduesComputedMenu", GT.translate("By Residue Name"),
        "PDBnucleicResiduesComputedMenu", GT.translate("By Residue Name"),
        "PDBcarboResiduesComputedMenu", GT.translate("By Residue Name"),
        "PDBheteroComputedMenu", GT.translate("By HETATM"),
        "surfMoComputedMenu", GT.translate("Molecular Orbitals"),
        "SYMMETRYSelectComputedMenu", GT.translate("Symmetry"),
        "SYMMETRYShowComputedMenu", GT.translate("Space Group"),
        "SYMMETRYhide", GT.translate("Hide Symmetry"),
        "hiddenModelSetText", GT.translate("Model information"),
        "selectMenuText", GT.translate("Select ({0})"),
        "allModelsText", GT.translate("All {0} models"),
        "configurationMenuText", GT.translate("Configurations ({0})"),
        "modelSetCollectionText", GT.translate("Collection of {0} models"),
        "atomsText", GT.translate("atoms: {0}"),
        "bondsText", GT.translate("bonds: {0}"),
        "groupsText", GT.translate("groups: {0}"),
        "chainsText", GT.translate("chains: {0}"),
        "polymersText", GT.translate("polymers: {0}"),
        "modelMenuText", GT.translate("model {0}"),
        "viewMenuText", GT.translate("View {0}"),
        "mainMenuText", GT.translate("Main Menu"),
        "biomoleculesMenuText", GT.translate("Biomolecules"),
        "biomoleculeText", GT.translate("biomolecule {0} ({1} atoms)"),
        "loadBiomoleculeText", GT.translate("load biomolecule {0} ({1} atoms)"),
        
        
//        "selectMenu", GT._("Select"),
        "selectAll", GT.translate("All"),
        "selectNone", GT.translate("None"),
        "hideNotSelectedCheckbox", GT.translate("Display Selected Only"),
        "invertSelection", GT.translate("Invert Selection"),

        "viewMenu", GT.translate("View"),
        "front", GT.translate("Front"),
        "left", GT.translate("Left"),
        "right", GT.translate("Right"),
        "top", GT.translate("Top"),
        "bottom", GT.translate("Bottom"),
        "back", GT.translate("Back"),

        "PDBproteinMenu", GT.translate("Protein"),
        "allProtein", GT.translate("All"),
        "proteinBackbone", GT.translate("Backbone"),
        "proteinSideChains", GT.translate("Side Chains"),
        "polar", GT.translate("Polar Residues"),
        "nonpolar", GT.translate("Nonpolar Residues"),
        "positiveCharge", GT.translate("Basic Residues (+)"),
        "negativeCharge", GT.translate("Acidic Residues (-)"),
        "noCharge", GT.translate("Uncharged Residues"),
        "PDBnucleicMenu", GT.translate("Nucleic"),
        "allNucleic", GT.translate("All"),
        "DNA", GT.translate("DNA"),
        "RNA", GT.translate("RNA"),
        "nucleicBackbone", GT.translate("Backbone"),
        "nucleicBases", GT.translate("Bases"),
        "atPairs", GT.translate("AT pairs"),
        "gcPairs", GT.translate("GC pairs"),
        "auPairs", GT.translate("AU pairs"),
        "PDBheteroMenu", GT.translate("Hetero"),
        "allHetero", GT.translate("All PDB \"HETATM\""),
        "Solvent", GT.translate("All Solvent"),
        "Water", GT.translate("All Water"),
        "nonWaterSolvent",
            GT.translate("Nonaqueous Solvent") + " (solvent and not water)",
        "exceptWater", GT.translate("Nonaqueous HETATM") + " (hetero and not water)",
        "Ligand", GT.translate("Ligand") + " (hetero and not solvent)",

        "allCarbo", GT.translate("All"),
        "PDBcarboMenu", GT.translate("Carbohydrate"),
        "PDBnoneOfTheAbove", GT.translate("None of the above"),

        "renderMenu", GT.translate("Style"),
        "renderSchemeMenu", GT.translate("Scheme"),
        "renderCpkSpacefill", GT.translate("CPK Spacefill"),
        "renderBallAndStick", GT.translate("Ball and Stick"),
        "renderSticks", GT.translate("Sticks"),
        "renderWireframe", GT.translate("Wireframe"),
        "PDBrenderCartoonsOnly", GT.translate("Cartoon"),
        "PDBrenderTraceOnly", GT.translate("Trace"),

        "atomMenu", GT.translate("Atoms"),
        "atomNone", GT.translate("Off"),
        "atom15", GT.translate("{0}% van der Waals", "15"),
        "atom20", GT.translate("{0}% van der Waals", "20"),
        "atom25", GT.translate("{0}% van der Waals", "25"),
        "atom50", GT.translate("{0}% van der Waals", "50"),
        "atom75", GT.translate("{0}% van der Waals", "75"),
        "atom100", GT.translate("{0}% van der Waals", "100"),

        "bondMenu", GT.translate("Bonds"),
        "bondNone", GT.translate("Off"),
        "bondWireframe", GT.translate("On"),
        "bond100", GT.translate("{0} \u00C5", "0.10"),
        "bond150", GT.translate("{0} \u00C5", "0.15"),
        "bond200", GT.translate("{0} \u00C5", "0.20"),
        "bond250", GT.translate("{0} \u00C5", "0.25"),
        "bond300", GT.translate("{0} \u00C5", "0.30"),

        "hbondMenu", GT.translate("Hydrogen Bonds"),
        "hbondNone", GT.translate("Off"),
        "PDBhbondCalc", GT.translate("Calculate"),
        "hbondWireframe", GT.translate("On"),
        "PDBhbondSidechain", GT.translate("Set H-Bonds Side Chain"),
        "PDBhbondBackbone", GT.translate("Set H-Bonds Backbone"),
        "hbond100", GT.translate("{0} \u00C5", "0.10"),
        "hbond150", GT.translate("{0} \u00C5", "0.15"),
        "hbond200", GT.translate("{0} \u00C5", "0.20"),
        "hbond250", GT.translate("{0} \u00C5", "0.25"),
        "hbond300", GT.translate("{0} \u00C5", "0.30"),

        "ssbondMenu", GT.translate("Disulfide Bonds"),
        "ssbondNone", GT.translate("Off"),
        "ssbondWireframe", GT.translate("On"),
        "PDBssbondSidechain", GT.translate("Set SS-Bonds Side Chain"),
        "PDBssbondBackbone", GT.translate("Set SS-Bonds Backbone"),
        "ssbond100", GT.translate("{0} \u00C5", "0.10"),
        "ssbond150", GT.translate("{0} \u00C5", "0.15"),
        "ssbond200", GT.translate("{0} \u00C5", "0.20"),
        "ssbond250", GT.translate("{0} \u00C5", "0.25"),
        "ssbond300", GT.translate("{0} \u00C5", "0.30"),

        "PDBstructureMenu", GT.translate("Structures"),
        "structureNone", GT.translate("Off"),
        "backbone", GT.translate("Backbone"),
        "cartoon", GT.translate("Cartoon"),
        "cartoonRockets", GT.translate("Cartoon Rockets"),
        "ribbons", GT.translate("Ribbons"),
        "rockets", GT.translate("Rockets"),
        "strands", GT.translate("Strands"),
        "trace", GT.translate("Trace"),

        "VIBRATIONMenu", GT.translate("Vibration"),
        "vibrationOff", GT.translate("Off"),
        "vibrationOn", GT.translate("On"),
        "VIBRATIONvectorMenu", GT.translate("Vectors"),
        "vectorOff", GT.translate("Off"),
        "vectorOn", GT.translate("On"),
        "vector3", GT.translate("{0} pixels", "3"),
        "vector005", GT.translate("{0} \u00C5", "0.05"),
        "vector01", GT.translate("{0} \u00C5", "0.10"),
        "vectorScale02", GT.translate("Scale {0}", "0.2"),
        "vectorScale05", GT.translate("Scale {0}", "0.5"),
        "vectorScale1", GT.translate("Scale {0}", "1"),
        "vectorScale2", GT.translate("Scale {0}", "2"),
        "vectorScale5", GT.translate("Scale {0}", "5"),

        "stereoMenu", GT.translate("Stereographic"),
        "stereoNone", GT.translate("None"),
        "stereoRedCyan", GT.translate("Red+Cyan glasses"),
        "stereoRedBlue", GT.translate("Red+Blue glasses"),
        "stereoRedGreen", GT.translate("Red+Green glasses"),
        "stereoCrossEyed", GT.translate("Cross-eyed viewing"),
        "stereoWallEyed", GT.translate("Wall-eyed viewing"),

        "labelMenu", GT.translate("Labels"),

        "labelNone", GT.translate("None"),
        "labelSymbol", GT.translate("With Element Symbol"),
        "labelName", GT.translate("With Atom Name"),
        "labelNumber", GT.translate("With Atom Number"),

        "labelPositionMenu", GT.translate("Position Label on Atom"),
        "labelCentered", GT.translate("Centered"),
        "labelUpperRight", GT.translate("Upper Right"),
        "labelLowerRight", GT.translate("Lower Right"),
        "labelUpperLeft", GT.translate("Upper Left"),
        "labelLowerLeft", GT.translate("Lower Left"),

        "colorMenu", GT.translate("Color"),
        "[color_atoms]Menu", GT.translate("Atoms"),

        "schemeMenu", GT.translate("By Scheme"),
        "cpk", GT.translate("Element (CPK)"),
        "altloc#PDB", GT.translate("Alternative Location"),
        "molecule", GT.translate("Molecule"),
        "formalcharge", GT.translate("Formal Charge"),
        "partialcharge#CHARGE", GT.translate("Partial Charge"),
        "relativeTemperature#BFACTORS", GT.translate("Temperature (Relative)"),
        "fixedTemperature#BFACTORS", GT.translate("Temperature (Fixed)"),

        "amino#PDB", GT.translate("Amino Acid"),
        "structure#PDB", GT.translate("Secondary Structure"),
        "chain#PDB", GT.translate("Chain"),
        "group#PDB", GT.translate("Group"),
        "monomer#PDB", GT.translate("Monomer"),
        "shapely#PDB", GT.translate("Shapely"),

        "none", GT.translate("Inherit"),
        "black", GT.translate("Black"),
        "white", GT.translate("White"),
        "cyan", GT.translate("Cyan"),

        "red", GT.translate("Red"),
        "orange", GT.translate("Orange"),
        "yellow", GT.translate("Yellow"),
        "green", GT.translate("Green"),
        "blue", GT.translate("Blue"),
        "indigo", GT.translate("Indigo"),
        "violet", GT.translate("Violet"),

        "salmon", GT.translate("Salmon"),
        "olive", GT.translate("Olive"),
        "maroon", GT.translate("Maroon"),
        "gray", GT.translate("Gray"),
        "slateblue", GT.translate("Slate Blue"),
        "gold", GT.translate("Gold"),
        "orchid", GT.translate("Orchid"),

        "opaque", GT.translate("Make Opaque"),
        "translucent", GT.translate("Make Translucent"),

        "[color_bonds]Menu", GT.translate("Bonds"),
        "[color_hbonds]Menu", GT.translate("Hydrogen Bonds"),
        "[color_ssbonds]Menu", GT.translate("Disulfide Bonds"),
        "colorPDBStructuresMenu", GT.translate("Structures"),
        "[color_backbone]Menu", GT.translate("Backbone"),
        "[color_trace]Menu", GT.translate("Trace"),
        "[color_cartoon]sMenu", GT.translate("Cartoon"),
        "[color_ribbon]sMenu", GT.translate("Ribbons"),
        "[color_rockets]Menu", GT.translate("Rockets"),
        "[color_strands]Menu", GT.translate("Strands"),
        "[color_labels]Menu", GT.translate("Labels"),
        "[color_background]Menu", GT.translate("Background"),
        "[color_isosurface]Menu", GT.translate("Surfaces"),
        "[color_vectors]Menu", GT.translate("Vectors"),
        "[color_axes]Menu", GT.translate("Axes"),
        "[color_boundbox]Menu", GT.translate("Boundbox"),
        "[color_UNITCELL]Menu", GT.translate("Unit cell"),

        "zoomMenu", GT.translate("Zoom"),
        "zoom50", "50%",
        "zoom100", "100%",
        "zoom150", "150%",
        "zoom200", "200%",
        "zoom400", "400%",
        "zoom800", "800%",
        "zoomIn", GT.translate("Zoom In"),
        "zoomOut", GT.translate("Zoom Out"),

        "spinMenu", GT.translate("Spin"),
        "spinOn", GT.translate("On"),
        "spinOff", GT.translate("Off"),

        "[set_spin_X]Menu", GT.translate("Set X Rate"),
        "[set_spin_Y]Menu", GT.translate("Set Y Rate"),
        "[set_spin_Z]Menu", GT.translate("Set Z Rate"),
        "[set_spin_FPS]Menu", GT.translate("Set FPS"),

        "s0", "0",
        "s5", "5",
        "s10", "10",
        "s20", "20",
        "s30", "30",
        "s40", "40",
        "s50", "50",

        "FRAMESanimateMenu", GT.translate("Animation"),
        "animModeMenu", GT.translate("Animation Mode"),
        "onceThrough", GT.translate("Play Once"),
        "palindrome", GT.translate("Palindrome"),
        "loop", GT.translate("Loop"),
        
        "play", GT.translate("Play"),
        "pause", GT.translate("Pause"),
        "resume", GT.translate("Resume"),
        "stop", GT.translate("Stop"),
        "nextframe", GT.translate("Next Frame"),
        "prevframe", GT.translate("Previous Frame"),
        "rewind", GT.translate("Rewind"),
        "playrev", GT.translate("Reverse"),
        "restart", GT.translate("Restart"),

        "FRAMESanimFpsMenu", GT.translate("Set FPS"),
        "animfps5", "5",
        "animfps10", "10",
        "animfps20", "20",
        "animfps30", "30",
        "animfps50", "50",

        "measureMenu", GT.translate("Measurements"),
        "measureOff", GT.translate("Double-Click begins and ends all measurements"),
        "measureDistance", GT.translate("Click for distance measurement"),
        "measureAngle", GT.translate("Click for angle measurement"),
        "measureTorsion", GT.translate("Click for torsion (dihedral) measurement"),
        "measureDelete", GT.translate("Delete measurements"),
        "JVM12measureList", GT.translate("List measurements"),
        "distanceNanometers", GT.translate("Distance units nanometers"),
        "distanceAngstroms", GT.translate("Distance units Angstroms"),
        "distancePicometers", GT.translate("Distance units picometers"),

        "pickingMenu", GT.translate("Set picking"),
        "pickOff", GT.translate("Off"),
        "pickCenter", GT.translate("Center"),
        //    "pickDraw" , GT._("moves arrows"),
        "pickIdent", GT.translate("Identity"),
        "pickLabel", GT.translate("Label"),
        "pickAtom", GT.translate("Select atom"),
        "PDBpickChain", GT.translate("Select chain"),
        "pickElement", GT.translate("Select element"),
        "PDBpickGroup", GT.translate("Select group"),
        "pickMolecule", GT.translate("Select molecule"),
        "SYMMETRYpickSite", GT.translate("Select site"),
        "pickSpin", GT.translate("Spin"),

        "JVM12showMenu", GT.translate("Show"),
        "JVM12showConsole", GT.translate("Console"),
        "showFile", GT.translate("File Contents"),
        "showFileHeader", GT.translate("File Header"),
        "showHistory", GT.translate("History"),
        "showIsosurface", GT.translate("Isosurface JVXL data"),
        "showMeasure", GT.translate("Measurements"),
        "showMo", GT.translate("Molecular orbital JVXL data"),
        "showModel", GT.translate("Model"),
        "showOrient", GT.translate("Orientation"),
        "showSpacegroup", GT.translate("Space group"),
        "SYMMETRYshowSymmetry", GT.translate("Symmetry"),
        "showState", GT.translate("Current state"),
        
        "SIGNEDloadMenu", GT.translate("Load"),      
        "loadPdb", GT.translate("File from PDB"),      
        "loadFileOrUrl", GT.translate("File or URL"),      
        "loadFileUnitCell", GT.translate("Load full unit cell"),      
        "loadScript", GT.translate("Script"),      

        "SIGNEDwriteMenu", GT.translate("Save"),      
        "writeFileTextVARIABLE", GT.translate("File {0}"),
        "writeState", GT.translate("Script with state"),      
        "writeHistory", GT.translate("Script with history"),      
        "writeJpg", GT.translate("{0} Image", "JPG"),      
        "writePng", GT.translate("{0} Image", "PNG"),      
        "writePovray", GT.translate("{0} Image", "POV-Ray"),      
        "writeIsosurface", GT.translate("JVXL Isosurface"),      
        "writeVrml", GT.translate("{0} 3D Model", "VRML"),      
        "writeX3d", GT.translate("{0} 3D Model", "X3D"),      
        "writeMaya", GT.translate("{0} 3D Model", "Maya"),      

        
        "UNITCELLshow", GT.translate("Unit cell"),
        "extractMOL", GT.translate("Extract MOL data"),

        "surfaceMenu", GT.translate("Surfaces"),
        "surfDots", GT.translate("Dot Surface"),
        "surfVDW", GT.translate("van der Waals Surface"),
        "surfMolecular", GT.translate("Molecular Surface"),
        "surfSolvent14", GT.translate("Solvent Surface ({0}-Angstrom probe)", "1.4"),
        "surfSolventAccessible14",
            GT.translate("Solvent-Accessible Surface (VDW + {0} Angstrom)", "1.4"),
        "CHARGEsurfMEP", GT.translate("Molecular Electrostatic Potential"),
        "surfOpaque", GT.translate("Make Opaque"),
        "surfTranslucent", GT.translate("Make Translucent"),
        "surfOff", GT.translate("Off"),

        "SYMMETRYUNITCELLMenu", GT.translate("Symmetry"),
        "UNITCELLone", GT.translate("Reload {0}", "{1 1 1}"),
        "UNITCELLnine", GT.translate("Reload {0}", "{444 666 1}"),
        "UNITCELLnineRestricted", GT.translate("Reload {0} + Display {1}", new Object[] { "{444 666 1}", "555" } ),
        "UNITCELLninePoly", GT.translate("Reload + Polyhedra"),
        

        "[set_axes]Menu", GT.translate("Axes"), 
        "[set_boundbox]Menu", GT.translate("Boundbox"),
        "[set_UNITCELL]Menu", GT.translate("Unit cell"),

        "off#axes", GT.translate("Hide"), 
        "dotted", GT.translate("Dotted"),

        "byPixelMenu", GT.translate("Pixel Width"), 
        "1p", GT.translate("{0} px", "1"),
        "3p", GT.translate("{0} px", "3"), 
        "5p", GT.translate("{0} px", "5"),
        "10p", GT.translate("{0} px", "10"),

        "byAngstromMenu", GT.translate("Angstrom Width"),
        "10a", GT.translate("{0} \u00C5", "0.10"),
        "20a", GT.translate("{0} \u00C5", "0.20"),
        "25a", GT.translate("{0} \u00C5", "0.25"),
        "50a", GT.translate("{0} \u00C5", "0.50"),
        "100a", GT.translate("{0} \u00C5", "1.0"),

//        "optionsMenu", GT._("Compatibility"),
        "showSelectionsCheckbox", GT.translate("Selection Halos"),
        "showHydrogensCheckbox", GT.translate("Show Hydrogens"),
        "showMeasurementsCheckbox", GT.translate("Show Measurements"),
        "perspectiveDepthCheckbox", GT.translate("Perspective Depth"),      
        "showBoundBoxCheckbox", GT.translate("Boundbox"),
        "showAxesCheckbox", GT.translate("Axes"),
        "showUNITCELLCheckbox", GT.translate("Unit cell"),      
        "colorrasmolCheckbox", GT.translate("RasMol Colors"),
        "aboutComputedMenu", GT.translate("About Jmol"),
        
        //"rasmolChimeCompatibility", GT._("RasMol/Chime Settings"),

        "APPLETjmolUrl", "http://www.jmol.org",
        "APPLETmouseManualUrl", GT.translate("Mouse Manual"),
        "APPLETtranslationUrl", GT.translate("Translations")
    };
 
    if (!wasTranslating)
      GT.setDoTranslate(wasTranslating);

    return words;
  }
  
  private void buildStructure(String menuStructure) {
    addItems(menuContents);
    addItems(structureContents);
    setStructure(menuStructure);
  }
  
  
  private String dumpWords() {
    String[] wordContents = getWordContents();
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < wordContents.length; i++) {
      String key = wordContents[i++];
      if (structure.getProperty(key) == null)
        s.append(key).append(" | ").append(wordContents[i]).append('\n');
    }
    return s.toString();
  }
  
  private String dumpStructure(String[][] items) {
    String previous = "";
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < items.length; i++) {
      String key = items[i][0];
      String label = words.getProperty(key);
      if (label != null)
        key += " | " + label;
      s.append(key).append(" = ")
       .append(items[i][1] == null ? previous : (previous = items[i][1]))
       .append('\n');
    }
    return s.toString();
  }
 
   
   
  public void setStructure(String slist) {
    if (slist == null)
      return;
    BufferedReader br = new BufferedReader(new StringReader(slist));
    String line;
    int pt;
    try {
      while ((line = br.readLine()) != null) {
        if (line.length() == 0 || line.charAt(0) == '#') 
          continue;
        pt = line.indexOf("=");
        if (pt < 0) {
          pt = line.length();
          line += "=";
        }
        String name = line.substring(0, pt).trim();
        String value = line.substring(pt + 1).trim();
        String label = null;
        if ((pt = name.indexOf("|")) >= 0) {
          label = name.substring(pt + 1).trim();
          name = name.substring(0, pt).trim();
        }
        if (name.length() == 0)
          continue;
        if (value.length() > 0)
          structure.setProperty(name, value);
        if (label != null && label.length() > 0)
          words.setProperty(name, GT.translate(label));
        /* note that in this case we are using a variable in 
         * the GT._() method. That's because all standard labels
         * have been preprocessed already, so any standard label
         * will be translated. Any other label MIGHT be translated
         * if by chance that word or phrase appears in some other
         * GT._() call somewhere else in Jmol. Otherwise it will not
         * be translated by this call, because it hasn't been 
         * internationalized. 
         */
      }
    } catch (Exception e) {
      //
    }
    try {
      br.close();
    } catch (Exception e) {
    }
  }
  
  private void addItems(String[][] itemPairs) {   
    String previous = "";
    for (int i = 0; i < itemPairs.length; i++) {
      String str = itemPairs[i][1];
      if (str == null)
        str = previous;
      previous = str;
      structure.setProperty(itemPairs[i][0], str);
    }
  }
  
  private void localize(boolean haveUserMenu, Properties menuText) {
    String[] wordContents = getWordContents();
    for (int i = 0; i < wordContents.length;)      
      /*if (haveUserMenu && words.getProperty(wordContents[i]) != null) {
        i += 2;
      } else*/ {
        String item = wordContents[i++];
        String word = wordContents[i++];
        words.setProperty(item, word);
        // save a few names for later
        if (menuText != null && item.indexOf("Text") >= 0)
          menuText.setProperty(item, word);
      }
  }
}
