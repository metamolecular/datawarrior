/*
 * @(#)TorsionDB.java
 *
 * Copyright 2013 openmolecules.org, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of openmolecules.org
 * Use is subject to license terms.
 *
 * @author Thomas Sander
 */

package com.actelion.research.chem.conf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;

public class TorsionDB {
    public static final int MODE_ANGLES = 1;
    public static final int MODE_RANGES = 2;
    public static final int MODE_FREQUENCIES = 4;
    public static final int MODE_BINS = 8;
    public static final int MODE_ALL = 15;

    public static final int TORSION_NOT_FOUND = -1;
    public static final int TORSION_GREEN = 0;
    public static final int TORSION_YELLOW = 1;
    public static final int TORSION_RED = 2;

    private static final String cTorsionIDFile = "/resources/conformation/torsionID.txt";
    private static final String cTorsionAngleFile = "/resources/conformation/torsionAngle.txt";
    private static final String cTorsionRangeFile = "/resources/conformation/torsionRange.txt";
    private static final String cTorsionFrequencyFile = "/resources/conformation/torsionFrequency.txt";
    private static final String cTorsionBinsFile = "/resources/conformation/torsionBins.txt";

    private static final int MAX_SP_CHAIN_LENGTH = 15;

    private static TorsionDB sInstance;
    private int mSupportedModes;
    private TreeMap<String,TorsionInfo> mTreeMap;

    public static void initialize(int mode) {
        if (sInstance == null) {
        	synchronized(TorsionDB.class) {
                if (sInstance == null)
                	sInstance = new TorsionDB();
        		}
        	}

        sInstance.init(mode);
    	}

	/**
	 * Locates all relevant rotatable bonds, i.e. single bonds, which are not in a ring with less than 6 members,
	 * where both atoms are sp2 or sp3 and carry at least one more non-hydrogen neighbor,
	 * which are not redundant, and those where a torsion change modifies the relative location of atoms.
	 * For chains of conjugated triple bonds the following applies:
	 * If at least one terminal sp2/sp3 atom has no external neighbor, then no single bond is considered rotatable.
	 * Otherwise that terminal single bond connecting the smaller substituent is considered the only rotatable bond of the linear atom strand.
	 * Bonds considered rotatable by this method are guaranteed to produce a valid torsionID with getTorsionID().
	 * @param mol
	 * @param skipAllRingBonds if true, then considers bonds in small and large(!) rings as non-rotatable
	 * @param isRotatableBond null or empty(!) array that will be filled by this method
	 * @return number of rotatable bonds
	 */
	public static int findRotatableBonds(StereoMolecule mol, boolean skipAllRingBonds, boolean[] isRotatableBond) {
		mol.ensureHelperArrays(Molecule.cHelperRings);

		if (isRotatableBond == null)
			isRotatableBond = new boolean[mol.getBonds()];

		int count = 0;
		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (mol.getBondOrder(bond) == 1
			 && !mol.isAromaticBond(bond)
			 && mol.getConnAtoms(mol.getBondAtom(0, bond)) > 1
			 && mol.getConnAtoms(mol.getBondAtom(1, bond)) > 1
			 && !(skipAllRingBonds && mol.isRingBond(bond))
			 && !(mol.isSmallRingBond(bond) && mol.getBondRingSize(bond) <= 5)) {
				isRotatableBond[bond] = true;
				count++;
				}
			}

		int[] centralAtom = new int[2];
		int[] rearAtom = new int[2];
		boolean[] bondHandled = null;

		// For every triple bond (or conjugated chain of triple bonds) keep
		// only one terminal single bond as rotatable, more precisely that bond
		// that is connected to the smaller molecule part.
		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (isRotatableBond[bond] && (bondHandled == null || !bondHandled[bond])) {
				int alkyneAtomCount = 0;

				for (int i=0; i<2; i++) {
		    		centralAtom[i] = mol.getBondAtom(i, bond);
		        	rearAtom[i] = mol.getBondAtom(1-i, bond);
		
		        	// walk along sp-chains to first sp2 or sp3 atom
		        	while (mol.getAtomPi(centralAtom[i]) == 2
		        		&& mol.getConnAtoms(centralAtom[i]) == 2
		        		&& mol.getAtomicNo(centralAtom[i]) < 10) {
		        		for (int j=0; j<2; j++) {
		        			int connAtom = mol.getConnAtom(centralAtom[i], j);
		        			if (connAtom != rearAtom[i]) {
		        				int connBond = mol.getConnBond(centralAtom[i], j);
		        				if (isRotatableBond[connBond]
		        				 && mol.getBondOrder(connBond) == 1) {
		        					isRotatableBond[connBond] = false;
		        					count--;
		        					}
		        				rearAtom[i] = centralAtom[i];
		        				centralAtom[i] = connAtom;
		        				alkyneAtomCount++;
		        				break;
		        				}
		        			}
		        		}
		        	}

				if (alkyneAtomCount != 0) {
					isRotatableBond[bond] = false;
					count--;
					if (mol.getConnAtoms(centralAtom[0]) > 1
					 && mol.getConnAtoms(centralAtom[1]) > 1) {
						int substituentSize0 = mol.getSubstituentSize(rearAtom[0], centralAtom[0]);
						int substituentSize1 = mol.getSubstituentSize(rearAtom[1], centralAtom[1]);
						int i = (substituentSize0 < substituentSize1) ? 0 : 1;
						int relevantBond = mol.getBond(rearAtom[i], centralAtom[i]);
						if (bondHandled == null)
							bondHandled = new boolean[mol.getBonds()];
						bondHandled[relevantBond] = true;
						isRotatableBond[relevantBond] = true;
						count++;
						}
					}
				}
			}

		return count;
		}

    private TorsionDB() {
        mTreeMap = new TreeMap<String,TorsionInfo>();
        mSupportedModes = 0;
    	}

    private void init(int mode) {
    	mode &= ~mSupportedModes;
    	if (mode == 0)
    		return;

    	synchronized(TorsionDB.class) {
	    	mSupportedModes |= mode;
	
	    	try {
	            BufferedReader tr = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(cTorsionIDFile)));
	            BufferedReader ar = ((mode & MODE_ANGLES) == 0) ? null
	                              : new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(cTorsionAngleFile)));
	            BufferedReader rr = ((mode & MODE_RANGES) == 0) ? null
	            				  : new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(cTorsionRangeFile)));
	            BufferedReader fr = ((mode & MODE_FREQUENCIES) == 0) ? null
	                              : new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(cTorsionFrequencyFile)));
	            BufferedReader br = ((mode & MODE_BINS) == 0) ? null
	                              : new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(cTorsionBinsFile)));
	
	            String type = tr.readLine();
	            while (type != null) {
	                TorsionInfo torsionInfo = mTreeMap.get(type);
	                if (torsionInfo == null) {
	                    torsionInfo = new TorsionInfo(getSymmetryType(type));
	                    mTreeMap.put(type, torsionInfo);
	                	}
	
	                if (ar != null) {
	                    String[] angle = ar.readLine().split(",");
	                    torsionInfo.angle = new short[angle.length];
	                    for (int i=0; i<angle.length; i++)
	                        torsionInfo.angle[i] = Short.parseShort(angle[i]);
	                    }
	                if (rr != null) {
	                    String[] range = rr.readLine().split(",");
	                    torsionInfo.range = new short[range.length][2];
	                    for (int i=0; i<range.length; i++) {
	                    	int index = range[i].indexOf('-', 1);
	                        torsionInfo.range[i][0] = Short.parseShort(range[i].substring(0, index));
	                        torsionInfo.range[i][1] = Short.parseShort(range[i].substring(index+1));
	                    	}
	                    }
	                if (fr != null) {
	                    String[] frequency = fr.readLine().split(",");
	                    torsionInfo.frequency = new short[frequency.length];
	                    for (int i=0; i<frequency.length; i++)
	                        torsionInfo.frequency[i] = Byte.parseByte(frequency[i]);
	                    }
	                if (br != null) {
	                    String[] binSize = br.readLine().split(",");
	                    torsionInfo.binSize = new byte[binSize.length];
	                    for (int i=0; i<binSize.length; i++)
	                        torsionInfo.binSize[i] = Byte.parseByte(binSize[i]);
	                    }
	
	                type = tr.readLine();
	                }
	
	            tr.close();
	            if (ar != null)
	                ar.close();
	            if (rr != null)
	                rr.close();
	            if (fr != null)
	                fr.close();
	            if (br != null)
	                br.close();
	            }
	        catch (IOException e) {}
    		}
        }

    /**
     * Returns an array of maxima of the smoothened torsion histogram
     * as short values in the range: 0 <= v < 360. Every v[i]
     * represents a bin from v-0.5 to v+0.5.
     * @param torsionID
     * @return null, if torsion type was not found
     */
    public static short[] getTorsions(String torsionID) {
        TorsionInfo ti = sInstance.getTorsionInfo(torsionID);
        return (ti == null) ? null : ti.get360DegreeAngles();
    	}

    /**
     * Returns an array containing left and right limits of the torsion curves,
     * of which the maxima are returned by getTorsions().
     * First index matches the torsion angle array returned by getTorsions().
     * The second index being 0 or 1 gives lower or higher curve limit, respectively.
     * These limits are either those angles where the curve has been fallen below 50%
     * of the maximum frequency or where the curve has a minimum value and touches
     * the next peak.
     * @param torsionID
     * @return null, if torsion type was not found
     */
    public static short[][] getTorsionRanges(String torsionID) {
        TorsionInfo ti = sInstance.getTorsionInfo(torsionID);
        return (ti == null) ? null : ti.get360DegreeRanges();
        }

    /**
     * Returns an array of frequencies in rounded percent.
     * Indices match the torsion angle array returned by getTorsions().
     * @param torsionID
     * @return null, if torsion type was not found
     */
    public static short[] getTorsionFrequencies(String torsionID) {
        TorsionInfo ti = sInstance.getTorsionInfo(torsionID);
        return (ti == null) ? null : ti.get360DegreeFrequencies();
        }

    /**
     * Returns the full circle histogram of torsion angles. Bin size is
     * 5 degrees. Bins are centered around values 0,5,10,...,355 degrees.
     * Counts are normalized to 127 as maximum value.
     * @param torsionID
     * @return null if torsionID not in collection.
     */
    public static byte[] getTorsionBinCounts(String torsionID) {
        TorsionInfo ti = sInstance.getTorsionInfo(torsionID);
        return (ti == null) ? null : ti.get72BinCounts();
        }

    private TorsionInfo getTorsionInfo(String torsionID) {
        if (torsionID == null)
            return null;
        TorsionInfo ti = mTreeMap.get(torsionID);
        if (ti != null)
            return ti;
        if (isInverted(torsionID)) {
            ti = mTreeMap.get(normalizeID(torsionID));
            if (ti != null) {
                ti = new TorsionInfo(ti); // TorsionInfo with inverted stereocenter(s)
                mTreeMap.put(torsionID, ti);
                return ti;
                }
            }
        return null;
        }

    /**
     * Determines uniquely an identifying name for the rotatable bond and its vicinity.
     * Based on this name various statistic parameters can be retrieved from this class.
     * If the bond is not a single bond, is aromatic, is a member of a <=5-membered ring,
     * if one of its atoms has 0,3 or more neighbours other than the other bond atom,
     * or if one of its atoms is a stereo center with unknown parity, then this method returns null.
     * Otherwise a fragment being unique for the this particular torsion situation is determined.
     * If one of the bond's atoms is a stereo center, the fragment may be inverted for
     * normalization, which would be represented with a trailing '<'. A trailing '>' indicates
     * a non-inverted stereo center.
     * If a TorsionDetail is given, then this will be filled with all classification information.
     * If an torsionAtom array is given instead, then this is filled with the four torsion defining atoms.
     * Terminal torsion atoms in torsionAtom (indexes 0,3) may be -1, which indicates a virtual atom
     * assumed to lie on the third terminal sp3 position, when we have two real terminal atoms with
     * equal symmetry ranks. If bond is part of a consecutive sp-sp atom chain, then
     * the classifying fragment covers all linear atoms plus two end atoms not being member
     * of the linear sp-atom strand.
     * @param mol source molecule
     * @param bond of which to determine torsion statistics
     * @param torsionAtom null or int[4] to receive torsion atoms
     * @param TorsionDetail null or empty TorsionDetail to receive torsion fragment, atom map, etc.
     * @return null or idcode identifying the bond and its vicinity
     */
    public static String getTorsionID(StereoMolecule mol, int bond, int[] torsionAtom, TorsionDetail detail) {
        if (detail == null)
        	detail = new TorsionDetail();

        detail.classify(mol, bond);

        if (torsionAtom != null) {
        	torsionAtom[0] = detail.getReferenceAtom(0);
        	torsionAtom[1] = detail.getCentralAtom(0);
        	torsionAtom[2] = detail.getCentralAtom(1);
        	torsionAtom[3] = detail.getReferenceAtom(1);
        	}

        return detail.getID();
        }

    /**
     * A fragment's symmetry type defines whether one part of a full 360 degree
     * matches symmetrically another range.
     * SYMMETRY_D1D1 (0 -> 180 match 0 -> -180): Two times D1 symmetry, i.e. one
     * terminal neighbor or two symmetrical sp3 neighbors.
     * SYMMETRY_C1D2 (0 -> 180 match -180 -> 0): One stereo center on one side
     * and two symmetrical sp2 neighbors on the other.
     * SYMMETRY_D1D2_OR_D2D2 (0 -> 90 match 180 -> 90, 0 -> -90, -180 -> -90):
     * Two symmetrical sp2 neighbors on one side and at least D1 (mirror plane) on the other.
     * SYMMETRY_C1C1_OR_C1D1 (no symmetry): one stereo center on one side and
     * not more than D1 (mirror plane) on the other.
     * This is a quick lookup, because this information is encoded in the torsionID itself.
     * @param torsionID
     * @return
     */
    public static int getSymmetryType(String torsionID) {
        return torsionID.endsWith("<") || torsionID.endsWith(">") ? TorsionDetail.SYMMETRY_C1C1_OR_C1D1
             : torsionID.endsWith("-") || torsionID.endsWith("+") ? TorsionDetail.SYMMETRY_C1D2
             : torsionID.endsWith("=") ? TorsionDetail.SYMMETRY_D1D2_OR_D2D2
             : TorsionDetail.SYMMETRY_D1D1;
        }

    public static boolean isInverted(String torsionID) {
        return torsionID.endsWith("<") || torsionID.endsWith("-");
        }

    public static String normalizeID(String torsionID) {
        return torsionID.endsWith("<") ? torsionID.substring(0, torsionID.length()-1)+">"
             : torsionID.endsWith("-") ? torsionID.substring(0, torsionID.length()-1)+"+"
             : torsionID;
        }

    /**
     * Calculates a signed torsion like calculateTorsion(). However, terminal atoms
     * of the defined atom sequence may be set to -1, referring to a hydrogen
     * or electron pair in the 3rd terminal sp2 position, when the other two
     * positions are occupied by two atoms that share the same symmetry rank.
     * @param mol
     * @param atom valid 4-atom sequence with terminal atoms optionally set to -1
     * @return torsion in the range: -pi <= torsion <= pi
     */
    public static double calculateTorsionExtended(StereoMolecule mol, int[] atom) {
    	if (atom[0] != -1 && atom[3] != -1)
    		return calculateTorsion(mol, atom);

    	return calculateTorsionExtended(mol, null, atom);
    	}

    /**
     * Calculates a signed torsion like calculateTorsion(). However, terminal atoms
     * of the defined atom sequence may be set to -1, referring to a hydrogen
     * or electron pair in the 3rd terminal sp2 position, when the other two
     * positions are occupied by two atoms that share the same symmetry rank.
     * @param conformer
     * @param atom valid 4-atom sequence with terminal atoms optionally set to -1
     * @return torsion in the range: -pi <= torsion <= pi
     */
    public static double calculateTorsionExtended(Conformer conformer, int[] atom) {
    	if (atom[0] != -1 && atom[3] != -1)
    		return calculateTorsion(conformer, atom);

    	return calculateTorsionExtended(conformer.getMolecule(), conformer, atom);
    	}

    /**
     * @param mol used for connectivity info, used for coords if conformer==null
     * @param conformer provides coords or is null
     * @param atom
     * @return
     */
    private static double calculateTorsionExtended(StereoMolecule mol, Conformer conformer, int[] atom) {
    	double[] angle1 = new double[2];

    	int[] rearAtom = findRearAtoms(mol, atom);

    	for (int i=0; i<2; i++) {
        	if (atom[3*i] != -1) {
                int central = 2-i;
                int terminal = 3-3*i;
                assert(mol.getConnAtoms(atom[central]) == 3);
    
                int index = 0;
        	    for (int j=0; j<3; j++) {
        	        int connAtom = mol.getConnAtom(atom[central], j);
           	        if (connAtom != rearAtom[1-i]) {
//        	        if (connAtom != atom[3-central]) {
        	            atom[terminal] = connAtom;
        	            if (conformer != null)
        	            	angle1[index++] = calculateTorsion(conformer, atom);
        	            else
        	            	angle1[index++] = calculateTorsion(mol, atom);
        	            }
        	        }
        	    atom[terminal] = -1;   // restore value for virtual atom
        	    return calculateVirtualTorsion(angle1);
        	    }
    	    }

        double[] angle2 = new double[2];

        int index1 = 0;
        assert(mol.getConnAtoms(atom[1]) == 3);
        for (int i=0; i<3; i++) {
            int terminal1 = mol.getConnAtom(atom[1], i);
            if (terminal1 != rearAtom[0]) {
//			if (terminal1 != atom[2]) {
                assert(mol.getConnAtoms(atom[2]) == 3);
                atom[0] = terminal1;
                int index2 = 0;
                for (int j=0; j<3; j++) {
                    int terminal2 = mol.getConnAtom(atom[2], j);
                    if (terminal2 != rearAtom[1]) {
//                  if (terminal2 != atom[1]) {
                        assert(mol.getConnAtoms(atom[2]) == 3);
                        atom[3] = terminal2;
        	            if (conformer != null)
        	            	angle2[index2++] = calculateTorsion(conformer, atom);
        	            else
        	            	angle2[index2++] = calculateTorsion(mol, atom);
                        }
                    }
                angle1[index1++] = calculateVirtualTorsion(angle2);
                }
            }
        atom[0] = -1;
        atom[3] = -1;
        return calculateVirtualTorsion(angle1);
        }

    /**
     * If the atom sequence contains a straight chain of sp-hybridized atoms,
     * then the atom array contain the first two and last two atoms of a sequence
     * of 2n+4 atoms (n: number of conjugated triple bonds). This method finds
     * the 3rd and 3rd-last atoms of the chain. rearAtom[0] is connected to atom[1]
     * and rearAtom[1] is connected to atom[2].
     * @param mol
     * @param torsionAtom
     * @return
     */
    public static int[] findRearAtoms(StereoMolecule mol, int[] torsionAtom) {
    	int[] rearAtom = new int[2];
    	if (mol.getBond(torsionAtom[1], torsionAtom[2]) != -1) {
    		rearAtom[0] = torsionAtom[2];
    		rearAtom[1] = torsionAtom[1];
    		}
    	else {
    		int[] pathAtom = new int[MAX_SP_CHAIN_LENGTH+1];
    		int pathLength = mol.getPath(pathAtom, torsionAtom[1], torsionAtom[2], MAX_SP_CHAIN_LENGTH, null);
    		rearAtom[0] = pathAtom[1];
    		rearAtom[1] = pathAtom[pathLength-1];
    		}
    	return rearAtom;
    	}

    /**
     * If the atom sequence contains a straight chain of sp-hybridized atoms,
     * then the atom array contain the first two and last two atoms of a sequence
     * of 2n+4 atoms (n: number of conjugated triple bonds). This method finds
     * extends the chain by inserting all sp-hybridised atoms into the strand.
     * @param mol
     * @param atom torsion defining (1st two and last two) atoms of atom sequence
     * @return 2n+4 atom indexes (n: number of conjugated triple bonds)
     */
    public static int[] getExtendedAtomSequence(StereoMolecule mol, int[] atom) {
    	if (mol.getBond(atom[1], atom[2]) != -1)
    		return atom;

    	int[] pathAtom = new int[MAX_SP_CHAIN_LENGTH+1];
   		int pathLength = mol.getPath(pathAtom, atom[1], atom[2], MAX_SP_CHAIN_LENGTH, null);
   		int[] realAtom = new int[3+pathLength];
   		realAtom[0] = atom[0];
   		for (int i=0; i<=pathLength; i++)
   	   		realAtom[i+1] = pathAtom[i];
   		realAtom[pathLength+2] = atom[3];
   		return realAtom;
    	}

    private static double calculateVirtualTorsion(double[] angle) {
        double meanAngle = (angle[1] + angle[0]) / 2;
        double angleDif = Math.abs(angle[1] - angle[0]);
        if (angleDif > Math.PI)
            return meanAngle;
        return (meanAngle < 0) ? meanAngle + Math.PI : meanAngle - Math.PI;
        }

    /**
     * Calculates a signed torsion as an exterior spherical angle
     * from a valid 4-atom strand.
     * Looking along the central bond, the torsion angle is 0.0, if the
     * projection of front and rear bonds point in the same direction.
     * If the front bond is rotated in the clockwise direction, the angle
     * increases, i.e. has a positive value.
     * http://en.wikipedia.org/wiki/Dihedral_angle
     * @param mol
     * @param atom 4 valid atom indices defining a connected atom sequence
     * @return torsion in the range: -pi <= torsion <= pi
     */
    public static double calculateTorsion(StereoMolecule mol, int[] atom) {
    	Point3D c1 = getCoordinates(mol, atom[0]);
    	Point3D c2 = getCoordinates(mol, atom[1]);
    	Point3D c3 = getCoordinates(mol, atom[2]);
    	Point3D c4 = getCoordinates(mol, atom[3]);

        Point3D v1 = new Point3D(c2.x-c1.x, c2.y-c1.y, c2.z-c1.z);
        Point3D v2 = new Point3D(c3.x-c2.x, c3.y-c2.y, c3.z-c2.z);
        Point3D v3 = new Point3D(c4.x-c3.x, c4.y-c3.y, c4.z-c3.z);

        Point3D n1 = getCrossProduct(v1, v2);
        Point3D n2 = getCrossProduct(v2, v3);

        return -Math.atan2(getLength(v2) * getDotProduct(v1, n2), getDotProduct(n1, n2));
        }

    /**
     * Calculates a signed torsion as an exterior spherical angle
     * from a valid 4-atom strand.
     * Looking along the central bond, the torsion angle is 0.0, if the
     * projection of front and rear bonds point in the same direction.
     * If the front bond is rotated in the clockwise direction, the angle
     * increases, i.e. has a positive value.
     * http://en.wikipedia.org/wiki/Dihedral_angle
     * @param conformer
     * @param atom 4 valid atom indices defining a connected atom sequence
     * @return torsion in the range: -pi <= torsion <= pi
     */
    public static double calculateTorsion(Conformer conformer, int[] atom) {
    	Point3D c1 = getCoordinates(conformer, atom[0]);
    	Point3D c2 = getCoordinates(conformer, atom[1]);
    	Point3D c3 = getCoordinates(conformer, atom[2]);
    	Point3D c4 = getCoordinates(conformer, atom[3]);

        Point3D v1 = new Point3D(c2.x-c1.x, c2.y-c1.y, c2.z-c1.z);
        Point3D v2 = new Point3D(c3.x-c2.x, c3.y-c2.y, c3.z-c2.z);
        Point3D v3 = new Point3D(c4.x-c3.x, c4.y-c3.y, c4.z-c3.z);

        Point3D n1 = getCrossProduct(v1, v2);
        Point3D n2 = getCrossProduct(v2, v3);

        return -Math.atan2(getLength(v2) * getDotProduct(v1, n2), getDotProduct(n1, n2));
        }

    /**
     * Checks, whether the torsion angle lies within statistical limits.
     * The torsion angle used should be obtained from a call to calculateTorsionExtended()
     * in order to be canonical concerning to the chosen terminal (pseudo)atoms.
     * Requires TorsionDB to be initialized with MODE_RANGES.
     * @param torsionID as obtained by getTorsionID()
     * @param angle obtained from a call to calculateTorsionExtended()
     * @return TORSION_GREEN,TORSION_YELLOW,TORSION_RED or TORSION_NOT_FOUND
     */
    public static int getTorsionStrainClass(String torsionID, double angle) {
    	short[][] rangeList = getTorsionRanges(normalizeID(torsionID));
    	if (rangeList == null)
    		return TORSION_NOT_FOUND;

    	int strain = TORSION_RED;
    	int angleIndex = getNormalizedTorsionIndex(torsionID, angle);
    	for (short[] range:rangeList) {
    		if (angleIndex >= range[0] && angleIndex <= range[1])
    			return TORSION_GREEN;
    		if (angleIndex >= range[0]-5 && angleIndex <= range[1]+5)
    			strain = TORSION_YELLOW;
    		}

    	return strain;
    	}

    /**
     * Normalizes a torsion angle considering the fragments symmetry type
     * by returning the lowest symmetrically equivalent torsion that
     * is >= 0.
     * @param torsionID
     * @param angle 
     * @return angle within native range of symmetry type
     */
    public static int getNormalizedTorsionIndex(String torsionID, double angle) {
        if (isInverted(torsionID))
            angle = -angle;

        // indices range from -180 to 179 indicating an angle = index
        angle = 180.0 * angle / Math.PI;
        int index = (int)Math.floor(0.5 + angle);
        if (index == 180)
        	index = -180;

        int symmetryClass = getSymmetryType(torsionID);
        switch (symmetryClass) {
        case TorsionDetail.SYMMETRY_C1C1_OR_C1D1:
            if (index < 0)
                index += 360;
            break;
        case TorsionDetail.SYMMETRY_C1D2:
            if (index < 0)
                index += 180;
            break;
        case TorsionDetail.SYMMETRY_D1D1:
            index = Math.abs(index);
            break;
        case TorsionDetail.SYMMETRY_D1D2_OR_D2D2:
            if (index < 0)
                index += 180;
            if (index > 90)
                index = 180 - index;
            break;
            }

        return index;
    	}

	/**
	 * In a consecutive sequence of sp-hybridized atoms multiple single bonds
	 * cause redundant torsions. Only that single bond with the smallest bond index
	 * is considered really rotatable; all other single bonds are pseudo rotatable.
	 * If one/both end(s) of the sp-atom sequence doesn't carry atoms
	 * outside of the straight line then no bond is considered rotatable.
	 * @param mol with valid helper arrays: neighbors.
	 * @param bond
	 * @return
	 */
	public static boolean isPseudoRotatableBond(StereoMolecule mol, int bond) {
		for (int i=0; i<2; i++) {
			int atom = mol.getBondAtom(i, bond);
			int rearAtom = mol.getBondAtom(1-i, bond);

			while (mol.getAtomPi(atom) == 2
				&& mol.getConnAtoms(atom) == 2
				&& mol.getAtomicNo(atom) < 10) {
				for (int j=0; j<2; j++) {
					int connAtom = mol.getConnAtom(atom, j);
					if (connAtom != rearAtom) {
						if (mol.getConnAtoms(connAtom) == 1)
							return true;

						int connBond = mol.getConnBond(atom, j);
						if (mol.getBondOrder(connBond) == 1
						 && connBond < bond)
							return true;

						rearAtom = atom;
						atom = connAtom;
						break;
						}
					}
				}
			}
		return false;
		}

    private static Point3D getCoordinates(Conformer conformer, int atom) {
        return new Point3D(conformer.x[atom], conformer.y[atom], conformer.z[atom]);
        }
    
    private static Point3D getCoordinates(Molecule mol, int atom) {
        return new Point3D(mol.getAtomX(atom), mol.getAtomY(atom), mol.getAtomZ(atom));
        }

    private static Point3D getCrossProduct(Point3D a, Point3D b) {
        return new Point3D(a.y*b.z - a.z*b.y, a.z*b.x - a.x*b.z, a.x*b.y - a.y*b.x);
        }

   private static double getDotProduct(Point3D a, Point3D b) {
        return a.x*b.x + a.y*b.y + a.z*b.z;
        }

    private static double getLength(Point3D a) {
        return Math.sqrt(a.x*a.x + a.y*a.y + a.z*a.z);
        }
	}

class Point3D {
	double x,y,z;

	public Point3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		}
	}

class TorsionInfo {
    short[] angle;
    short[][] range;
    short[] frequency;
    byte[] binSize;
    int symmetryClass;

    public TorsionInfo(int symmetryClass) {
        this.symmetryClass = symmetryClass;
        }

    /**
     * Create TorsionInfo describing a fragment with inverted stereo situation.
     * @param ti TorsionInfo of non-symmetrical fragment
     */
    public TorsionInfo(TorsionInfo ti) {
        symmetryClass = ti.symmetryClass;
        if (ti.angle != null) {
            angle = new short[ti.angle.length];
            for (int i=0; i<angle.length; i++)
                angle[i] = (short)(360 - ti.angle[angle.length-i-1]);
            }
        if (ti.range != null) {
            range = new short[ti.range.length][2];
            for (int i=0; i<range.length; i++) {
                range[i][0] = (short)(360 - ti.range[angle.length-i-1][1]);
                range[i][1] = (short)(360 - ti.range[angle.length-i-1][0]);
            	}
            }
        if (ti.frequency != null) {
            frequency = new short[ti.frequency.length];
            for (int i=0; i<frequency.length; i++)
                frequency[i] = ti.frequency[frequency.length-i-1];
            }
        if (ti.binSize != null) {
            binSize = new byte[ti.binSize.length];
            for (int i=0; i<binSize.length; i++)
                binSize[i] = ti.binSize[binSize.length-i-1];
            }
        }

    /**
     * Returns list of preferred angles (0 <= angle < 360).
     * If fragment is symmetrical, then the quarter or half circle
     * angle list is mirrored on the fly to complete the full circle.
     * @return
     */
    public short[] get360DegreeAngles() {
        int i1,i2,length;
        short[] fullAngle = null;
        switch (symmetryClass) {
        case TorsionDetail.SYMMETRY_C1D2:
            fullAngle = new short[2*angle.length];
            for (int i=0; i<angle.length; i++) {
                fullAngle[i] = angle[i];
                fullAngle[angle.length+i] = (short)(180+angle[i]);
                }
            return fullAngle;
        case TorsionDetail.SYMMETRY_D1D1:
            i1 = (angle[0] == 0) ? 1 : 0;
            i2 = (angle[angle.length-1] == 180) ? angle.length-1 : angle.length;
            length = i2 - i1;
            fullAngle = new short[angle.length + length];
            for (int i=0; i<angle.length; i++)
                fullAngle[i] = angle[i];
            for (int i=i1; i<i2; i++)
                fullAngle[fullAngle.length-1-i+i1] = (short)(360-angle[i]);
            return fullAngle;
        case TorsionDetail.SYMMETRY_D1D2_OR_D2D2:
            i1 = (angle[0] == 0) ? 1 : 0;
            i2 = (angle[angle.length-1] == 90) ? angle.length-1 : angle.length;
            length = i2 - i1;
            fullAngle = new short[2*angle.length + 2*length];
            for (int i=0; i<angle.length; i++) {
                fullAngle[i] = angle[i];
                fullAngle[angle.length+length+i] = (short)(180+angle[i]);
                }
            for (int i=i1; i<i2; i++) {
                fullAngle[angle.length+length-1-i+i1] = (short)(180-angle[i]);
                fullAngle[fullAngle.length-1-i+i1] = (short)(360-angle[i]);
                }
            return fullAngle;
        default:    // TorsionDetail.SYMMETRY_C1C1_OR_C1D1
            return angle;
            }
        }

    /**
     * Returns low and high peak limits, i.e. the angle ranges
     * with indices matching the array returned by get360DegreeAngles().
     * The second index
     * @return
     */
    public short[][] get360DegreeRanges() {
        short[][] fullRange = null;
        int size = range.length;
        switch (symmetryClass) {
        case TorsionDetail.SYMMETRY_C1D2:
        	fullRange = new short[2*size][2];
            for (int i=0; i<size; i++) {
            	fullRange[i][0] = range[i][0];
            	fullRange[i][1] = range[i][1];
            	fullRange[size+i][0] = (short)(180+range[i][0]);
            	fullRange[size+i][1] = (short)(180+range[i][1]);
                }
            return fullRange;
        case TorsionDetail.SYMMETRY_D1D1:
            int i1 = (angle[0] == 0) ? 1 : 0;
            int i2 = (angle[size-1] == 180) ? size-1 : size;
            int length = i2 - i1;
            fullRange = new short[size+length][2];
            for (int i=0; i<size; i++) {
            	fullRange[i][0] = range[i][0];
            	fullRange[i][1] = range[i][1];
            	}
            for (int i=i1; i<i2; i++) {
            	fullRange[fullRange.length-1-i+i1][0] = (short)(360-range[i][1]);
            	fullRange[fullRange.length-1-i+i1][1] = (short)(360-range[i][0]);
            	}
            return fullRange;
        case TorsionDetail.SYMMETRY_D1D2_OR_D2D2:
            i1 = (angle[0] == 0) ? 1 : 0;
            i2 = (angle[size-1] == 90) ? size-1 : size;
            length = i2 - i1;
            fullRange = new short[2*size + 2*length][2];
            for (int i=0; i<size; i++) {
            	fullRange[i][0] = range[i][0];
            	fullRange[i][1] = range[i][1];
                fullRange[size+length+i][0] = (short)(180+range[i][0]);
                fullRange[size+length+i][1] = (short)(180+range[i][1]);
                }
            for (int i=i1; i<i2; i++) {
            	fullRange[size+length-1-i+i1][0] = (short)(180-range[i][1]);
            	fullRange[size+length-1-i+i1][1] = (short)(180-range[i][0]);
            	fullRange[fullRange.length-1-i+i1][0] = (short)(360-range[i][1]);
            	fullRange[fullRange.length-1-i+i1][1] = (short)(360-range[i][0]);
                }
            return fullRange;
        default:    // TorsionDetail.SYMMETRY_C1C1_OR_C1D1
            return range;
            }
        }

    /**
     * Returns list of preferred angle frequencies (0 <= angle < 360)
     * with indices matching the array returned by get360DegreeAngles().
     * @return
     */
    public short[] get360DegreeFrequencies() {
        short[] fullFrequency = null;
        int size = frequency.length;
        switch (symmetryClass) {
        case TorsionDetail.SYMMETRY_C1D2:
            fullFrequency = new short[2*size];
            for (int i=0; i<size; i++) {
                fullFrequency[i] = frequency[i];
                fullFrequency[size+i] = frequency[i];
                }
            return fullFrequency;
        case TorsionDetail.SYMMETRY_D1D1:
            int i1 = (angle[0] == 0) ? 1 : 0;
            int i2 = (angle[size-1] == 180) ? size-1 : size;
            int length = i2 - i1;
            fullFrequency = new short[size+length];
            for (int i=0; i<size; i++)
                fullFrequency[i] = frequency[i];
            for (int i=i1; i<i2; i++)
                fullFrequency[fullFrequency.length-1-i+i1] = frequency[i];
            return fullFrequency;
        case TorsionDetail.SYMMETRY_D1D2_OR_D2D2:
            i1 = (angle[0] == 0) ? 1 : 0;
            i2 = (angle[size-1] == 90) ? size-1 : size;
            length = i2 - i1;
            fullFrequency = new short[2*size + 2*length];
            for (int i=0; i<size; i++) {
                fullFrequency[i] = frequency[i];
                fullFrequency[size+length+i] = frequency[i];
                }
            for (int i=i1; i<i2; i++) {
                fullFrequency[size+length-1-i+i1] = frequency[i];
                fullFrequency[fullFrequency.length-1-i+i1] = frequency[i];
                }
            return fullFrequency;
        default:    // TorsionDetail.SYMMETRY_C1C1_OR_C1D1
            return frequency;
            }
        }

    public byte[] get72BinCounts() {
        if (binSize.length == 72)   // SYMMETRY_C1C1_OR_C1D1
            return binSize;

        byte[] fullBin = new byte[72];

        switch (symmetryClass) {
        case TorsionDetail.SYMMETRY_C1D2:
            for (int i=0; i<36; i++) {
                fullBin[i] = binSize[i];
                fullBin[i+36] = binSize[i];
                }
            break;
        case TorsionDetail.SYMMETRY_D1D1:
            for (int i=0; i<36; i++) {
                fullBin[i] = binSize[i];
                fullBin[i+36] = binSize[36-i];
            	}
            break;
        case TorsionDetail.SYMMETRY_D1D2_OR_D2D2:
            for (int i=0; i<18; i++) {
                fullBin[i] = binSize[i];
                fullBin[i+18] = binSize[18-i];
                fullBin[i+36] = binSize[i];
                fullBin[i+54] = binSize[18-i];
                }
            break;
            }
        return fullBin;
        }
    }