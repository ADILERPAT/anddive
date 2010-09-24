package divestoclimb.lib.scuba;

/**
 * An abstract class for a source of breathing gas at depth.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public abstract class GasSource {

	public static float pressureAtAltitude(int altitude, Units units) {
		// Valid for nonzero temperature lapse rate, but only works in
		// the troposphere.
		final float stdTemp = units.absTempStd(), lapseRate = units.lowAtmTempLapse();
		return (float)Math.pow(stdTemp / (stdTemp + lapseRate * altitude),
				units.standardGravity() * units.atmMolarMass() / (units.gasConstant() * lapseRate));
	}

	/**
	 * Returns the partial pressure of oxygen at depth while breathing this gas.
	 * @param depth The depth in the currently set units
	 * @return The partial pressure of oxygen in ATA
	 */
	public double pO2AtDepth(int depth, Units units) {
		return pO2AtDepth(depth, 0, units);
	}

	abstract public double pO2AtDepth(int depth, float surfacePressure, Units units);

	/**
	 * Returns the partial pressure of nitrogen at depth while breathing this gas.
	 * @param depth The depth in the currently set units
	 * @return The partial pressure of nitrogen in ATA
	 */
	public double pN2AtDepth(int depth, Units units) {
		return pN2AtDepth(depth, 1, units);
	}

	abstract public double pN2AtDepth(int depth, float surfacePressure, Units units);

	/**
	 * Returns the partial pressure of helium at depth while breathing this gas. 
	 * @param depth The depth in the currently set units
	 * @return The partial pressure of helium in ATA
	 */
	public double pHeAtDepth(int depth, Units units) {
		return pHeAtDepth(depth, 1, units);
	}
	
	abstract public double pHeAtDepth(int depth, float surfacePressure, Units units);
	
	abstract public String toString();

	/**
	 * Return the Equivalent Air Depth of this gas source at a given depth.
	 * @param depth The depth to determine the EAD for.
	 * @param units The unit system to use
	 * @return The equivalent air depth, rounded up to the nearest standard depth increment
	 */
	public float EAD(int depth, Units units) {
		return Math.max(((int) Math.ceil((pN2AtDepth(depth, units) / 0.79f - 1) * units.depthPerAtm()) / units.depthIncrement()) * units.depthIncrement(), 0);
	}

	/**
	 * Return the Equivalent Narcotic Depth of this gas source at a given depth.
	 * @param depth The depth to determine the END for.
	 * @param units The unit system to use
	 * @param oxygenIsNarcotic Whether or not to consider the effects of oxygen in the calculation
	 * @return The equivalent narcotic depth, rounded up to the nearest standard depth increment
	 */
	public float END(int depth, Units units, boolean oxygenIsNarcotic) {
		double pNarc = oxygenIsNarcotic? pO2AtDepth(depth, units) + pN2AtDepth(depth, units): pN2AtDepth(depth, units);
		return Math.max(((int) Math.ceil((pNarc - 1) * units.depthPerAtm()) / units.depthIncrement()) * units.depthIncrement(), 0);
	}

	/*public float computeCNS(int depth, Units units, float time, float currentCNS) throws CnsOtu.MaxPo2ExceededException {
		return currentCNS + time * CnsOtu.getCNSPerMinute(pO2AtDepth(depth, units));
	}*/
	
	/**
	 * Computes the amount of time it would take to reach maxCNS from
	 * currentCNS breathing this gas at depth. This can be thought of as a
	 * maximum exposure time for the gas at a given depth, but it does not
	 * account for time needed to ascend from that depth.
	 * @param depth The depth at which the gas is being used
	 * @param units A Units object set to the system matching the unit of depth
	 * @param currentCNS The current CNS loading as a percentage
	 * @param maxCNS The maximum CNS desired
	 * @return The time in minutes it would take to reach maxCNS, rounded down
	 * to the nearest minute.
	 * @throws MaxPo2ExceededException 
	 */
	public int computeMaxCNSExposure(int depth, Units units, float currentCNS, int maxCNS) throws CnsOtu.MaxPo2ExceededException {
		return (int)Math.floor((maxCNS - currentCNS) / CnsOtu.getCNSPerMinute(pO2AtDepth(depth, units)));
	}

	/*public float computeOTU(int depth, Units units, float time, float currentOTU) {
		return currentOTU + time * CnsOtu.getOTUPerMinute(pO2AtDepth(depth, units));
	}*/
	
	public int computeMaxOTUExposure(int depth, Units units, float currentOTU, int maxOTU) {
		return (int)Math.floor((maxOTU - currentOTU) / CnsOtu.getOTUPerMinute(pO2AtDepth(depth, units)));
	}
}