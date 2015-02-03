package com.actelion.research.forcefield.mm2;

import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.forcefield.FastMath;

/**
 * Out of Plane Angle term
 * 
 * @author freyssj
 */
public final class OutOfPlaneAngleTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

//	private static final double OP_UNIT = 0.02191418;
	private static final double OP_UNIT = 5.12191418;
	private static final double SANG = 0.00000007;

	private final FFParameters.OutOfPlaneBendParameters params;
	private double energy;	

	private OutOfPlaneAngleTerm(FFMolecule mol, int[] atoms, FFParameters.OutOfPlaneBendParameters p) {
		super(mol, atoms);
		this.params = p;
	}
	
	
	/**
	 * Out of Plane Bend Angle defined as angle of AB to the BCD plane
	 * <pre>
	 *            D
	 *           /
	 *          / 
	 *  A --- B
	 *          \
	 *           \
	 *            C
	 * </pre>
	 * 
	 * @param tl
	 * @param a1
	 * @param a2
	 * @param a3
	 * @param a4
	 */
	protected static OutOfPlaneAngleTerm create(MM2TermList tl, int a1, int a2, int a3, int a4) {
		FFMolecule mol = tl.getMolecule();
		int n1 = mol.getAtomMM2Class(a1);
		int n2 = mol.getAtomMM2Class(a2);		
		FFParameters.OutOfPlaneBendParameters params = parameters.getOutOfPlaneBendParameters(n1, n2);
		if(params!=null) {
			int[] atoms = new int[]{a1, a2, a3, a4};
			return new OutOfPlaneAngleTerm(mol, atoms, params);
		}
		return null;
	}
	
	private double dt;

	@Override
	public final double getFGValue(Coordinates[] gradient) {

		final Coordinates ca = getMolecule().getCoordinates(atoms[0]);
		final Coordinates cb = getMolecule().getCoordinates(atoms[1]);
		final Coordinates cc = getMolecule().getCoordinates(atoms[2]);
		final Coordinates cd = getMolecule().getCoordinates(atoms[3]);

		final Coordinates cab = ca.subC(cb);
		final Coordinates ccb = cc.subC(cb);
		final Coordinates cdb = cd.subC(cb);
		final Coordinates cad = ca.subC(cd);
		final Coordinates ccd = cc.subC(cd);
		
		final double rdb2 = cdb.distSq();
		final double rad2 = cad.distSq();
		final double rcd2 = ccd.distSq();
		
		final double ee = cab.dot(ccb.cross(cdb)); // project AB on the normal of BCD 
		final double dot = cad.dot(ccd);			//
		final double c = rad2 * rcd2 - dot * dot;  //C = 1 - cos2(DA,DC) = sin2(DA,DC)
		
		//Out of plane Angle		
		if(rdb2==0 || c==0) {
			energy = 0;
		} else {
			final double bkk2 = rdb2 - ee*ee/c; //sin angle
			dt = RADIAN * FastMath.acos(Math.sqrt(bkk2/rdb2)); //Equilibrum is always 0
			final double dt2 = dt * dt;
			final double dt4 = dt2 * dt2;
			energy = OP_UNIT * params.fopb * dt2 * (1f + SANG*dt4);
			
			if(gradient!=null) {
				
				final double deddt = OP_UNIT * params.fopb * dt * RADIAN * (2f + 6f*SANG*dt4);
				final double dedcos = -deddt *  (ee==0? 0:  ee>0? 1: -1) / FastMath.sqrt(c*bkk2);
				final double term = ee / c;
				final Coordinates dccdia = cad.scaleC(rcd2).subC(ccd.scaleC(dot)).scaleC(term);
				final Coordinates dccdic = ccd.scaleC(rad2).subC(cad.scaleC(dot)).scaleC(term);
				final Coordinates dccdid = new Coordinates().subC(dccdia).subC(dccdic);
			
				
				final Coordinates deedia = cdb.cross(ccb);
				final Coordinates deedic = cab.cross(cdb);
				final Coordinates deedid = ccb.cross(cab).addC(cdb.scaleC(ee / rdb2));			
			
				final Coordinates g0 = dccdia.addC(deedia).scaleC(dedcos);
				final Coordinates g2 = dccdic.addC(deedic).scaleC(dedcos);
				final Coordinates g3 = dccdid.addC(deedid).scaleC(dedcos);
				final Coordinates g1 = new Coordinates().subC(g0).subC(g2).subC(g3);

				if(atoms[0]<gradient.length) gradient[atoms[0]].add(g0);
				if(atoms[1]<gradient.length) gradient[atoms[1]].add(g1);
				if(atoms[2]<gradient.length) gradient[atoms[2]].add(g2);
				if(atoms[3]<gradient.length) gradient[atoms[3]].add(g3);
				
			}
		}		
		return energy;		
	}
	


	@Override
	public final String toString() {
		return "O-P-Bend    " +
			new DecimalFormat("00").format(atoms[0]) + " - " + 
			new DecimalFormat("00").format(atoms[1]) + "  " + 
			new DecimalFormat("0.0000").format(dt) + " -> " + 
			new DecimalFormat("0.0000").format(energy);
	}


	public final boolean isUsed() {
		return params!=null;
	}



	

}
