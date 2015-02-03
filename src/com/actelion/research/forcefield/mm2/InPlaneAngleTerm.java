package com.actelion.research.forcefield.mm2;

import java.text.DecimalFormat;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.FFMolecule;
import com.actelion.research.chem.calculator.StructureCalculator;
import com.actelion.research.forcefield.AbstractTerm;
import com.actelion.research.forcefield.FFParameters;
import com.actelion.research.forcefield.FastMath;

/**
 * Special term used by Tinker only but not by a typical MM2 Forcefield 
 * 
 * @author freyssj
 */
public final class InPlaneAngleTerm extends AbstractTerm {
	private final static FFParameters parameters = MM2Parameters.getInstance();

	private static final double ANGLE_UNIT = 0.02191418;
	private static final double SANG = 0.00000007;
	private final FFParameters.AngleParameters p;
	
	private Coordinates ca, cb, cc, cd, cad, ccd, cbd;
	private Coordinates cap, ccp, cm, ct;
	private double rt2;
	private double rap2, rm, rcp2, delta, angle;
	private double energy;	

	
	private InPlaneAngleTerm(FFMolecule mol, int[] atoms, FFParameters.AngleParameters p){
		super(mol, atoms);
		this.p = p;
	}
	

	
	/**
	 * In Plane Bend Angle defined as angle of AB to the BCD plane
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
	protected static InPlaneAngleTerm create(MM2TermList tl, int a1, int a2, int a3, int a4) {
		
		int n1 = tl.getMolecule().getAtomMM2Class(a1);
		int n2 = tl.getMolecule().getAtomMM2Class(a2);
		int n3 = tl.getMolecule().getAtomMM2Class(a3);
		
		int ringSize = tl.getMolecule().getRingSize(new int[]{a1, a2, a3});
		int nHydro = StructureCalculator.getExplicitHydrogens(tl.getMolecule(), a2);
		int[] atoms = new int[]{a1, a2, a3, a4};
		if(tl.getMolecule().getAtomicNo(a1)==1) nHydro--;
		if(tl.getMolecule().getAtomicNo(a3)==1) nHydro--;
		
		FFParameters.AngleParameters p = parameters.getAngleParameters(n1, n2, n3,  nHydro, ringSize);
		return p!=null? new InPlaneAngleTerm(tl.getMolecule(), atoms, p): null;
	}
	
	@Override
	public final double getFGValue(Coordinates[] gradient) {

		energy = 0;

		ca = getMolecule().getCoordinates(atoms[0]);
		cb = getMolecule().getCoordinates(atoms[1]);
		cc = getMolecule().getCoordinates(atoms[2]);
		cd = getMolecule().getCoordinates(atoms[3]);


		cad = ca.subC(cd);
		ccd = cc.subC(cd);
		cbd = cb.subC(cd);

		ct = cad.cross(ccd); //0 if in the plane
		rt2 = ct.distSq();
		delta = -ct.dot(cbd)/rt2;			
		Coordinates cip = cb.addC(ct.scaleC(delta));
		cap = ca.subC(cip);
		ccp = cc.subC(cip);
		rap2 = cap.distSq();
		rcp2 = ccp.distSq();
		if(rap2==0) rap2 = 0.0001;
		if(rcp2==0) rcp2 = 0.0001;

		cm = ccp.cross(cap);
		rm = cm.dist();
		if(rm==0) rm = 0.0001;
		double cosine = cap.dot(ccp) / FastMath.sqrt(rap2*rcp2);
		angle = RADIAN * FastMath.acos(cosine);
		double dt = angle - p.eq;
		
		double dt2 = dt * dt;
		double dt4 = dt2 * dt2;
		energy = ANGLE_UNIT * p.fc * dt2 * (1.0 + SANG*dt4);
		
		if(gradient!=null) {
			double deddt = ANGLE_UNIT * p.fc * dt * RADIAN * (2.0 + SANG*dt4);
			double terma = - deddt / (rap2*rm);
			double termc =   deddt / (rcp2*rm);
		
			Coordinates dedia = cap.cross(cm).scaleC(terma); 
			Coordinates dedic = ccp.cross(cm).scaleC(termc); 
			Coordinates dedip = new Coordinates().subC(dedia).subC(dedic);

					
			double delta2 = 2.0 * delta;
			double ptrt2 = dedip.dot(ct) / rt2;
			Coordinates term = cbd.cross(ccd).addC(ct.cross(ccd).scaleC(delta2));
			Coordinates dpdia = ccd.cross(dedip).scaleC(delta).addC(term.scaleC(ptrt2));
			
			term = cad.cross(cbd).addC(cad.cross(ct).scaleC(delta2));
			Coordinates dpdic = dedip.cross(cad).scaleC(delta).addC(term.scaleC(ptrt2));
		
			if(atoms[0]<gradient.length) gradient[atoms[0]].add(dedia.addC(dpdia));
			if(atoms[1]<gradient.length) gradient[atoms[1]].add(dedip);
			if(atoms[2]<gradient.length) gradient[atoms[2]].add(dedic.addC(dpdic));
			if(atoms[3]<gradient.length) gradient[atoms[3]].add(new Coordinates().subC(dedia).subC(dpdia).subC(dedip).subC(dedic).subC(dpdic));
		}
		
		return energy;
	}

	public double getPreferredAngle() {
		return p.eq;
	}

	public final boolean isUsed() {
		return p!=null;
	}

	@Override
	public String toString() {
		return "Angle-IP    "  + new DecimalFormat("00").format(atoms[0])+" - "+new DecimalFormat("00").format(atoms[1])+" - "+new DecimalFormat("00").format(atoms[2])+"    " + new DecimalFormat("0.0000").format(p.eq) + " "+new DecimalFormat("0.0000").format(angle)+ " -> "+new DecimalFormat("0.0000").format(energy)+" "+p.fc;
	}

	

}
