package com.actelion.research.chem.conf;

import com.actelion.research.chem.StereoMolecule;

public class Conformer {
	public float[] x,y,z;
	private StereoMolecule mMol;
	private short[] bondTorsion;

	/**
	 * Creates a new set of atom coordinates of the given Molecule.
	 * @param mol
	 */
	public Conformer(StereoMolecule mol) {
		mMol = mol;
		x = new float[mol.getAllAtoms()];
		y = new float[mol.getAllAtoms()];
		z = new float[mol.getAllAtoms()];
		}

	/**
	 * Returns the current bond torsion angle in degrees, it is was set before.
	 * @param bond
	 * @return -1 or previously set torsion angle in the range 0 ... 359
	 */
	public int getBondTorsion(int bond) {
		return bondTorsion == null ? -1 : bondTorsion[bond];
		}

	/**
	 * Sets the current bond torsion to be retrieved later.
	 * @param bond
	 * @param torsion in degrees
	 */
	public void setBondTorsion(int bond, short torsion) {
		if (bondTorsion == null)
			bondTorsion = new short[mMol.getAllBonds()];
		bondTorsion[bond] = torsion;
		}

	/**
	 * Creates a new conformer as an exact copy of the given one.
	 * @param c
	 */
	public Conformer(Conformer c) {
		mMol = c.mMol;
		x = new float[c.x.length];
		y = new float[c.x.length];
		z = new float[c.x.length];
		for (int atom=0; atom<mMol.getAllAtoms(); atom++) {
			x[atom] = c.x[atom];
			y[atom] = c.y[atom];
			z[atom] = c.z[atom];
			}
		if (c.bondTorsion != null) {
			bondTorsion = new short[c.bondTorsion.length];
			for (int i=0; i<c.bondTorsion.length; i++)
				bondTorsion[i] = c.bondTorsion[i];
			}
		}

	/**
	 * @return reference to the original molecule
	 */
	public StereoMolecule getMolecule() {
		return mMol;
		}

	/**
	 * Copies this Conformer's atom coordinates to the given molecule.
	 * @param mol molecule identical to the original molecule passed in the Constructor
	 */
	public void copyTo(StereoMolecule mol) {
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, x[atom]);
			mol.setAtomY(atom, y[atom]);
			mol.setAtomZ(atom, z[atom]);
			}
		}

	/**
	 * Creates a new molecule with the conformer's atom coordinates.
	 * @return
	 */
	public StereoMolecule toMolecule() {
		StereoMolecule mol = mMol.getCompactCopy();
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, x[atom]);
			mol.setAtomY(atom, y[atom]);
			mol.setAtomZ(atom, z[atom]);
			}
		return mol;
		}
	}
