package divestoclimb.lib.scuba;

public class CnsOtu {
	
	public static class MaxPo2ExceededException extends Exception {
		private static final long serialVersionUID = 9110983151742007956L;
	}
	
	private float mSurfacePressure;
	private Units mUnits;
	
	private float mCns;
	private float mOtu;
	
	private int mDepth = 0;
	
	public CnsOtu(int altitude, Units units, float resCNS, float resOTU) {
		mSurfacePressure = GasSource.pressureAtAltitude(altitude, units);
		mUnits = units;
		mCns = resCNS;
		mOtu = resOTU;
	}
	
	public float getCns() { return mCns; }
	public float getOtu() { return mOtu; }
	
	/**
	 * Execute a depth change on the CNS/OTU model, and compute the result.
	 * @param newDepth The new depth to change to
	 * @param time The amount of time it's expected to take to get to the
	 * desired depth from the current one
	 * @param source the GasSource being breathed during the depth change
	 * @throws MaxPo2ExceededException 
	 */
	public void changeDepth(int newDepth, float time, GasSource source) throws MaxPo2ExceededException {
		final double po2i = source.pO2AtDepth(mDepth, mSurfacePressure, mUnits);
		final double po2f = source.pO2AtDepth(mDepth, mSurfacePressure, mUnits);
		mCns += getCNSPerMinute(po2i, po2f) * time;
		mOtu += getOTUPerMinute(po2i, po2f) * time;
	}
	
	/**
	 * Execute a constant exposure on the CNS/OTU model based on the current
	 * depth, and compute the result.
	 * @param time The amount of time at which to stay at the current depth
	 * @param source The GasSource being breathed during this time
	 * @throws MaxPo2ExceededException 
	 */
	public void run(float time, GasSource source) throws MaxPo2ExceededException {
		final double po2 = source.pO2AtDepth(mDepth, mSurfacePressure, mUnits);
		mCns += getCNSPerMinute(po2) * time;
		mOtu += getOTUPerMinute(po2) * time;
	}

	// Uses NOAA data with linear interpolation at lower pO2's to get CNS
	// loading rate in percent per minute at the passed pO2 in ATA.
	public static float getCNSPerMinute(double po2) throws MaxPo2ExceededException {
		// Source of this data is the 2004 edition TDI Advanced Trimix manual,
		// combined with an older table I got from TDI somewhere that was more
		// precise, but less conservative. The most conservative values
		// available from each were combined into this model.
		if(po2 < 0.55) {
			return 0;
		} else if(po2 <= 0.8) {
			// NOAA data has CNS accumulation per minute go up by 0.04 for every
			// 0.1 ata increase in pO2, with 0.6 ata creating 0.14% CNS per min.
			// This linear relationship models that data.
			return 0.4f * (float)po2 - 0.1f;
		} else if(po2 <= 0.9) {
			// From here on out we can't assume the relationship is linear, so
			// we go conservative and round up.
			return 0.33f;
		} else if(po2 <= 1) {
			return 0.42f;
		} else if(po2 <= 1.1) {
			return 0.48f;
		} else if(po2 <= 1.2) {
			// At 1.2 and above, the older more precise table I have corresponds
			// exactly to the 2004 table. So I'm using the older data with 0.05
			// increments as long as the latest data continues to agree with it.
			return 0.48f;
		} else if(po2 <= 1.25) {
			return 0.51f;
		} else if(po2 <= 1.3) {
			return 0.56f;
		} else if(po2 <= 1.35) {
			return 0.61f;
		} else if(po2 <= 1.4) {
			return 0.67f;
		} else if(po2 <= 1.45) {
			return 0.73f;
		} else if(po2 <= 1.5) {
			return 0.83f;
		} else if(po2 <= 1.55) {
			return 1.12f;
		} else if(po2 <= 1.6) {
			return 2.22f;
		} else {
			throw new MaxPo2ExceededException();
		}
	}

	public static float getCNSPerMinute(double po2i, double po2f) throws MaxPo2ExceededException {
		// This method does a sum of the contributions of the highest pO2 and
		// all po2's on a 0.05 interval and weights them by the interval width
		// (the first and last pO2's can be off-interval, affecting the end
		// widths)
		final double po2Hi = Math.max(po2i, po2f);
		final double po2Lo = Math.min(po2i, po2f);
		double change = po2Hi - po2Lo;
		float result = 0;
		float interval;
		for(double p = po2Hi; p > po2Lo; p -= interval) {
			final float pDev = (float)p / 0.05f - (int)(p / 0.05);
			if(pDev > 0) {
				// p is not a multiple of our interval. Set interval to the
				// difference
				interval = pDev;
			} else {
				// Set the interval to either 0.05 or, if we're at the last
				// segment, the distance between the current value and the
				// low pO2.
				interval = Math.min((float)(p - po2Lo), 0.05f);
			}
			result += getCNSPerMinute(p) * interval / change;
		}

		return result;
	}

	public static float getOTUPerMinute(double po2) {
		// Uses equation published in 2004 edition TDI Advanced Trimix manual
		return (float)Math.pow(0.5 / (po2 - 0.05), -5/6);
	}

	public static float getOTUPerMinute(double po2i, double po2f) {
		// Uses equation published in 2004 edition TDI Advanced Trimix manual
		if(po2i == po2f) {
			// Avoid divide by 0
			return getOTUPerMinute(po2i);
		}
		return (float)(3 / (11 * po2f - po2i) * (Math.pow((po2f - 0.5) / 0.5, 11/6) - Math.pow((po2f - 0.5) / 0.5, 11/6)));
	}
}