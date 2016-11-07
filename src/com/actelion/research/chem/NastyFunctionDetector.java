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

package com.actelion.research.chem;

import java.util.ArrayList;

public class NastyFunctionDetector {
	public static final String[] cNastyFunctionsUnknown = null;

	public static final String sNastyFunction[][] = { // TODO: ADD mono-halogenated silanes 
		{ "sGP@LdbKT`NiFrVceCAhXKg@HAEXR@", "polar activated DB" },
		{ "sGP@DjVePGVCELT\\xA@Hn\\@`DUVHd", "twice activated DB" },
		{ "QM@HvAuCZHmg@HABTaDXeYVB@", "acyl-halogenide type" },
		{ "RFDDQFCELbLRlfxACpBDRl@", "Cl,Br,I on N,O,P,S,Se,I" },
		{ "qC`PBHRVdCjQkI`", "allyl/benzyl chloride" },
		{ "QM@HzAaEjcJXeX", "prim. alkyl-bromide/iodide" },
		{ "qC`@Qz`MeEFYLRl@", "sec./tert. alkyl-bromide/iodide" },
		{ "qCaPQ@HRmhCjY@", "alkyl sulfonate/sulfate type" },
		{ "sJQ`@bdjt`H", "acid anhydride" },
		{ "sJXA@IczhA@", "quat" },
		{ "qCpB@SGZ@`", "tert. immonium" },
		{ "`Hi@`", "carbenium" },
		{ "qCqAPJRnRYhCjZ@", "aromatic nitro" },
		{ "qCh@CIKTAaARoSP", "1,2-diamino-aryl" },
		{ "sJT@@TeZhA``ZJH", "1,3-diamino-aryl" },
		{ "sGT@ATeVj`FBEihl", "1,4-diamino-aryl" },
		{ "qCh@BIRtDWLP", "azo" },
		{ "sJU@h`NdiLsPRL", "azoxy" },
		{ "QMPRIncTD", "diazo" },
		{ "QMPRI^cxD", "diazo" },
		{ "sJT@@Te^lA@", "1,1-dinitrile" },
		{ "QMHAIhFBTYk`DO@HQ@", "formaldehyde aduct" },
		{ "QO@HyjAleFTxA@", "epoxide/aziridine" },
		{ "QMPBchFBUi@", "hydrazine" },
		{ "QM`AITFLypB@P", "isocyanate type" },
		{ "`H@[T[|A`xABPtCAp|@bHrBcDk@", "unwanted atom" },
		{ "`IoAHD", "phosphonium" },
		{ "qCrAPCiJSS@P", "nitrone" },
		{ "QMhHRVAmH", "nitroso" },
		{ "qCc@AYIj`H", "orthoester/acid" },
		{ "RFHEFB", "organic peroxide!" },
		{ "sGY@HDiViPFrVbcg@HAEs`D@`", "N-acyloxy-amide" },
		{ "qC`@Qv`LTlbLRlvQFIV@", "1,1-dihalo-alkene" },
		{ "qC`@IRtDVJVQFIV[HcDk@", "1,2-dihalo-alkene" },
		{ "`Jd`]T", "pyrylium" },
		{ "qCbPPNBRt`ZrP", "silylenol-ether" },
		{ "qCh@BISJAsD", "dimethylene-hydrazine" },
		{ "QMPARZAeS~j@", "methanediamine" },
		{ "HaFD`Bs`BLdTTIUSRp@`", "limit! methylene-thiazolidine-2,4-dione" },
		{ "sOtHLPDISOkSM@LHP", "limit! thiazol-2-ylamine" },
		{ "sGU@DPdsmR@rb\\", "acyl-hydrazone" },
		{ "qCp@AJV`LIIROFIFC`@", "imine/hydrazone of aldehyde" },
		{ "HeUD@BxIrJJKPlKTmL@MXhP", "2,3-diamino-quinone" },
		{ "HifL@DBarJIPhfZif@FDT", "limit! 4-acyl-3-azoline-2-one-3-ol" },
		{ "sGT`EPTfyi`D", "limit! oxal-diamide" },
		{ "HaED@DpFRYUJfjV@D", "limit! 5-methylene-imidazolidine-2,4-dione" },
		{ "sJQ@@dls@LX\\bLRl@", "2-halo-enone" },
		{ "sJQ@@djsAEXpyDXeX", "3-halo-enone" },
		{ "qCiASARUrSM@P", "N-nitro" },
		{ "qCpPHABSM@XsHp\\@", "thio-amide/urea" } };

	private static StereoMolecule[]	sFragmentList;
	private static int[][]			sIndexList;
	private static boolean			sInitialized;

	public NastyFunctionDetector() {
		synchronized(NastyFunctionDetector.class) {
			if (!sInitialized) {
		        try {
					sFragmentList = new StereoMolecule[sNastyFunction.length];
					sIndexList = new int[sNastyFunction.length][];
					SSSearcherWithIndex sss = new SSSearcherWithIndex(SSSearcher.cMatchAtomCharge);
					for (int i=0; i<sNastyFunction.length; i++) {
						sFragmentList[i] = new IDCodeParser(false).getCompactMolecule(sNastyFunction[i][0]);
						sIndexList[i] = sss.createIndex(sFragmentList[i]);
						}
					sInitialized = true;
					}
				catch (Exception e) {
		            System.out.println("Unable to initialize NastyFunctionDetector");
					}
				}
			}
		}

	/**
	 * @param mol
	 * @param index FragFp descriptor
	 * @return list of names of detected nasty function or null in case of initialization error
	 */
	public String[] getNastyFunctionList(StereoMolecule mol, int[] index) {
		if (!sInitialized)
			return cNastyFunctionsUnknown;

		ArrayList<String> nastyFunctionList = new ArrayList<String>();

		SSSearcherWithIndex sss = new SSSearcherWithIndex(SSSearcher.cMatchAtomCharge);
		sss.setMolecule(mol, index);
		for (int i=0; i<sNastyFunction.length; i++) {
			sss.setFragment(sFragmentList[i], sIndexList[i]);
			if (sss.isFragmentInMolecule())
				nastyFunctionList.add(sNastyFunction[i][1]);
			}

		addPolyHaloAromaticRings(mol, nastyFunctionList);

		return nastyFunctionList.toArray(new String[0]);
		}

	/**
	 * @param mol
	 * @param index FragFp descriptor
	 * @return '; ' separated list of detected nasty function names
	 */
	public String getNastyFunctionString(StereoMolecule mol, int[] index) {
		String[] nfl = getNastyFunctionList(mol, index);
		if (nfl == null)
			return "initialization error";
		if (nfl.length == 0)
			return "";
		StringBuilder sb = new StringBuilder(nfl[0]);
		for (int i=1; i<nfl.length; i++)
			sb.append("; "+nfl[i]);
		return sb.toString();
		}

	private void addPolyHaloAromaticRings(StereoMolecule mol, ArrayList<String> nastyFunctionList) {
		RingCollection ringSet = mol.getRingSet();
		for (int ring=0; ring<ringSet.getSize(); ring++) {
			if (ringSet.isAromatic(ring)) {
				int halogenes = 0;
				int[] ringAtom = ringSet.getRingAtoms(ring);
				for (int i=0; i<ringAtom.length; i++) {
					for (int j=0; j<mol.getConnAtoms(ringAtom[i]); j++) {
						int connAtom = mol.getConnAtom(ringAtom[i], j);
						if (!mol.isRingAtom(connAtom)
						 && (mol.getAtomicNo(connAtom) == 9
						  || mol.getAtomicNo(connAtom) == 17
						  || mol.getAtomicNo(connAtom) == 35
						  || mol.getAtomicNo(connAtom) == 53))
							halogenes++;
						}
					}

				if (halogenes > 2) {
					nastyFunctionList.add("polyhalo aromatic ring");
					}
				}
			}
		}
	}
