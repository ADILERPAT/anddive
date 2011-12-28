package divestoclimb.lib.scuba;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A GasSource that maintains a constant pO2 setpoint for the diver to breathe
 * (i.e. a closed-circuit mixed gas rebreather)
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class Setpoint extends GasSource {

	private float mpO2;
	private Mix mDiluent;

	/**
	 * The constructor
	 * @param po2 The partial pressure of oxygen being maintained in the breathing
	 * loop, in ATA
	 * @param diluent The Mix being used as diluent in the loop
	 */
	public Setpoint(float po2, Mix diluent) {
		mpO2 = po2;
		mDiluent = diluent;
	}

	public float getPo2() {
		return mpO2;
	}

	public void setPo2(float po2) {
		mpO2 = po2;
	}

	public Mix getDiluent() {
		return mDiluent;
	}

	public void setDiluent(Mix diluent) {
		mDiluent = diluent;
	}

	/**
	 * Get the fraction of oxygen present in the loop at a given depth
	 * @param depth The depth for which to compute the fraction of oxygen
	 * @param units The units system depth is in
	 * @return The fraction of oxygen, a number between 0 and 1
	 */
	public double fO2AtDepth(int depth, Units units) {
		return Math.min(mpO2 / (depth / units.depthPerAtm() + 1), 1);
	}
	
	// These next two are solutions to the following set of equations:
	// Pdepth = pN2 + pHe + pO2		(Dalton's Law for the breathing loop contents)
	// pN2 / fN2 = pHe / fHe		(Partial pressures of N2 and He in the loop must
	//								be proportional to their ratio in the diluent)
	@Override
	public double pHeAtDepth(int depth, float surfacePressure, Units units) {
		Mix diluent = mDiluent;
		return diluent.getfHe() * (depth / units.depthPerAtm() + surfacePressure - mpO2) / (1 - diluent.getfO2());
	}

	@Override
	public double pN2AtDepth(int depth, float surfacePressure, Units units) {
		Mix diluent = mDiluent;
		return diluent.getfN2() * (depth / units.depthPerAtm() + surfacePressure - mpO2) / (1 - diluent.getfO2());
	}

	@Override
	public double pO2AtDepth(int depth, float surfacePressure, Units units) {
		// Return either the setpoint or the absolute pressure at depth,
		// whichever is smaller (accounts for 1.4 at 10 ft case)
		return Math.min(mpO2, depth / units.depthPerAtm() + surfacePressure);
	}

	@Override
	public String toString() {
		NumberFormat nf = new DecimalFormat("#.#");
		if(mDiluent != null) {
			return String.format("p%s; %s", nf.format(mpO2), mDiluent.toString());
		} else {
			return "p" + nf.format(mpO2);
		}
	}
}