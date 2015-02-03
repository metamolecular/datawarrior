package com.actelion.research.forcefield.mm2;

import com.actelion.research.forcefield.FFConfig;
import com.actelion.research.forcefield.interaction.ClassInteractionStatistics;

/**
 * MM2Config is used to specify which terms of the forcefield have to be used
 * 
 * <pre>
 * The MM2 Terms used for the conformation of a structure are
 *  - bonds
 *  - angles
 *  - out of plane angles
 *  - torsion
 *  - vdw                 --
 *  - dipoles              | _ if the atoms are separated by less than maxDistance
 *  - charge               |     
 *  - charge - dipole     --
 * 
 * 
 * The intermolecular interactions are set using:
 *  - usePLStats  (statistics)
 *  - usePLDipole (include vdw, dipoles from MM2, much slower)
 * Those terms are added only if the distance is less than maxPLDistance
 * </pre>
 * 
 */
public class MM2Config extends FFConfig {

	// ////////////MM2 Terms ////////////////////////////////
	private boolean useOutOfPlaneAngle = true;
	private boolean useStretchBend = true;
	private boolean useTorsion;
	private int vdwType = 1; // 0 -> normal, 1 ->4-8 with H, 2->4-8 potential
								// without H, 3->PL
	private boolean useDipole = true;
	private boolean useCharge = true;
	private boolean useChargeDipole = true;
	private boolean useInPlaneAngle = false; // used to compare with Tinker
												// (False is default)
	private double maxDistance = 9.5;
	private boolean useHydrogenOnProtein = false;

	// ////////// Protein Ligand interactions ///////////////
	/** The maximum distance considered to add a intermolecular term */
	private double maxPLDistance = 9;
	/** Do we use the stats */
	private boolean usePLInteractions = true;
	/**
	 * Do we use the intermolecular dipoles (Note: already considered partially
	 * in the PLStats)
	 */
	private boolean usePLDipole = true;

	// Orbitals
	private boolean useOrbitals = true;

	// ////////// Initialization //////////////////////////////
	private boolean addLonePairs = true;
	private boolean addHydrogens = true;

	// ////////// Existing Config ////////////////////////////

	public final boolean isUseDipole() {
		return useDipole;
	}

	public final boolean isUseOutOfPlaneAngle() {
		return useOutOfPlaneAngle;
	}

	public final boolean isUseInPlaneAngle() {
		return useInPlaneAngle;
	}

	public final void setUseDipole(boolean b) {
		useDipole = b;
	}

	public final void setUseOutOfPlaneAngle(boolean b) {
		useOutOfPlaneAngle = b;
	}

	public final void setUseInPlaneAngle(boolean b) {
		useInPlaneAngle = b;
	}

	public final boolean isUseStretchBend() {
		return useStretchBend;
	}

	public final void setUseStretchBend(boolean b) {
		useStretchBend = b;
	}

	public final boolean isUseCharge() {
		return useCharge;
	}

	public final boolean isUseChargeDipole() {
		return useChargeDipole;
	}

	public final void setUseCharge(boolean b) {
		useCharge = b;
	}

	public final void setUseChargeDipole(boolean b) {
		useChargeDipole = b;
	}

	public final boolean isAddLonePairs() {
		return addLonePairs;
	}

	public final void setAddLonePairs(boolean b) {
		addLonePairs = b;
	}

	/**
	 * @return
	 */
	public final boolean isAddHydrogens() {
		return addHydrogens;
	}

	/**
	 * @param b
	 */
	public final void setAddHydrogens(boolean b) {
		addHydrogens = b;
	}

	/**
	 * @return
	 */
	public final double getMaxPLDistance() {
		return maxPLDistance;
	}

	/**
	 * @param d
	 */
	public final void setMaxPLDistance(double d) {
		maxPLDistance = d;
	}

	/**
	 * @return
	 */
	public final double getMaxDistance() {
		return maxDistance;
	}

	/**
	 * @return
	 */
	public final boolean isUsePLDipole() {
		return usePLDipole;
	}

	/**
	 * @param d
	 */
	public final void setMaxDistance(double d) {
		maxDistance = d;
	}

	/**
	 * @param b
	 */
	public final void setUsePLDipole(boolean b) {
		usePLDipole = b;
	}

	/**
	 * @return
	 */
	public boolean isUsePLInteractions() {
		return usePLInteractions;
	}

	/**
	 * @param b
	 */
	public void setUsePLInteractions(boolean b) {
		usePLInteractions = b;
	}

	public boolean isUseOrbitals() {
		return useOrbitals;
	}

	public void setUseOrbitals(boolean useOrbitals) {
		this.useOrbitals = useOrbitals;
	}

	public int getVdwType() {
		return vdwType;
	}

	public void setVdwType(int vdwType) {
		this.vdwType = vdwType;
	}

	public ClassInteractionStatistics getClassStatistics() {
		return ClassInteractionStatistics.getInstance(useHydrogenOnProtein);
	}

	public void setUseHydrogenOnProtein(boolean useHydrogenOnProtein) {
		this.useHydrogenOnProtein = useHydrogenOnProtein;
	}

	public boolean isUseHydrogenOnProtein() {
		return useHydrogenOnProtein;
	}

	public static class MM2Basic extends MM2Config {
		public MM2Basic() {

			setUseDipole(false);
			setMaxPLDistance(0);
			setUsePLInteractions(false);
			setUsePLDipole(false);
			setUseChargeDipole(false);
			setUseCharge(false);
			setUseHydrogenOnProtein(false);
		}
	}

	public static class PreoptimizeConfig extends MM2Basic {
		public PreoptimizeConfig() {
			setUseOutOfPlaneAngle(false);
			setUseOrbitals(false);
		}
	}

	public static class DockConfig extends MM2Config {
		public DockConfig() {
			setMaxDistance(100);
			setMaxPLDistance(100);
			setUseHydrogenOnProtein(false);
		}
	}

	public static class DockConfigFinal extends DockConfig {
		public DockConfigFinal() {
			setUseHydrogenOnProtein(true);
		}
	}

	public void setUseTorsion(boolean useTorsion) {
		this.useTorsion = useTorsion;
	}

	public boolean isUseTorsion() {
		return useTorsion;
	}
}
