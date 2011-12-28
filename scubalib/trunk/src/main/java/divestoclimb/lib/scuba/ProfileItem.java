package divestoclimb.lib.scuba;

/**
 * A class that stores a single line of a dive profile. A line consists of the
 * entire time that a diver is at a constant depth, breathing from the same gas
 * source, for the same reason (e.g. planned or contingency)
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class ProfileItem {

	private Long id;
	protected long mDiveID;

	// The depth of this profile line
	private int mDepth = INHERIT_DEPTH;
	/**
	 * The depth of this line is the same as the depth of the previous line
	 */
	public static final int INHERIT_DEPTH = -1;
	// The time constraint on this profile line
	private int mTime = INHERIT_TIME;
	/**
	 * The time constraint on this profile line is the same as the time constraint
	 * of the previous line
	 */
	public static final int INHERIT_TIME = -1;
	// The type of constraint for the time
	private int mTimeType = INHERIT_TIME_TYPE;
	// Available time constraint types
	/**
	 * Time is the amount of time elapsed during this section of the dive
	 */
	public static final int TIME_TYPE_SEG = 0;
	/**
	 * Run time at which to change to next line
	 */
	public static final int TIME_TYPE_RUN = 1;
	/**
	 * The type of constraint on this profile line is the same as the type of
	 * the previous line
	 */
	public static final int INHERIT_TIME_TYPE = -1;
	// The gas source the diver is breathing from at this point in the dive
	private GasSource mGasSource = INHERIT_GASSOURCE;
	/**
	 * The gas source for this profile line is the same as for the previous line
	 */
	public static final GasSource INHERIT_GASSOURCE = null;
	// The source of this profile line; where did it come from?
	private int mLineSource;
	// Available source types
	/**
	 * The user entered this line manually
	 */
	public static final int SOURCE_USER = 0;
	/**
	 * A deco planner created this line
	 */
	public static final int SOURCE_DECO = 1;
	/**
	 * This line was created as part of a contingency plan
	 */
	public static final int SOURCE_CONTINGENCY = 2;
	/**
	 * This section of the dive is necessary to escape an overhead environment
	 */
	public static final int SOURCE_CAVE = 3;
	// TODO: break source
	// The active status of this line. Profile processors are not allowed to remove
	// a line that is not of the same type as their own, but they may deactivate a
	// line and override it with a different one.
	private boolean mActive = true;
	// Is this line currently valid?
	private int mValid;
	// Possible values:
	/**
	 * This item's values are based on the entered profile up to this point.
	 * If any item before this one changes, this item must be marked invalid.
	 */
	public static final int VALID = 1;
	/**
	 * This item was created based on an older version of this profile. It may
	 * no longer be accurate and should be regenerated.
	 */
	public static final int INVALID = 0;
	/**
	 * This item's values are independent of the rest of the profile, so it cannot
	 * possibly be invalid.
	 */
	public static final int ALWAYS_VALID = -1;
	
	// The seg time for this ProfileItem as computed by a DecoAlgorithm that
	// processed this Item
	private float mSegtime = -1;
	
	// The amount of time it took to change depth in order to get to this
	// ProfileItem as computed by a DecoAlgorithm that processed this Item
	private float mDepthChangeTime = -1;
	
	public ProfileItem() { }

	public ProfileItem(long dive_id, int source, boolean alwaysValid) {
		super();
		mDiveID = dive_id;
		mLineSource = source;
		mValid = alwaysValid? ALWAYS_VALID: VALID;
	}

	/*public ProfileItem(long dive_id, int order, int depth, int time, int timeType, GasSource gasSource, int source, boolean alwaysValid) {
		super(id);
		reset(id, dive_id, order, depth, time, timeType, gasSource, source, true, alwaysValid? ALWAYS_VALID: VALID);
	}
	
	public ProfileItem(long id, long dive_id, int order, int depth, int time, int timeType, GasSource gasSource, int source, boolean active, int valid) {
		super(id);
		reset(id, dive_id, order, depth, time, timeType, gasSource, source, active, valid);
	}
	
	public void reset(long id, long dive_id, int order, int depth, int time, int timeType, GasSource gasSource, int source, boolean active, int valid) {
		super.reset(id);
		mDiveID = dive_id;
		mOrder = order;
		mDepth = depth;
		mTime = time;
		mTimeType = timeType;
		mGasSource = gasSource;
		mLineSource = source;
		mActive = active;
		mValid = valid;
		mSegtime = mDepthChangeTime = -1;
	}*/
	
	public ProfileItem(long dive_id) {
		mDiveID = dive_id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public int getDepth() { return mDepth; }
	public ProfileItem setDepth(int depth) { mDepth = depth; return this; }	
	public int getTime() { return mTime; }
	public ProfileItem setTime(int time) { mTime = time; return this; }
	public int getTimeType() { return mTimeType; }
	public ProfileItem setTimeType(int timeType) { mTimeType = timeType; return this; }
	public GasSource getGasSource() { return mGasSource; }
	public ProfileItem setGasSource(GasSource gasSource) { mGasSource = gasSource; return this; }
	public int getLineSource() { return mLineSource; }
	public ProfileItem setLineSource(int source) { mLineSource = source; return this; }
	public boolean isActive() { return mActive; }
	public ProfileItem setActive(boolean active) { mActive = active; return this; }
	public int getValid() { return mValid; }
	public ProfileItem setValid(int valid) { mValid = valid; return this; }

	/**
	 * Only meant to be called by a DecoAlgorithm to store the runtime after
	 * processing
	 * @param runTime
	 * @return This ProfileItem instance
	 */
	public ProfileItem setSegtime(float segTime) {
		mSegtime = segTime;
		return this;
	}
	public float getSegtime() {
		return mSegtime;
	}
	
	public ProfileItem setDepthChangeTime(float depthChangeTime) {
		mDepthChangeTime = depthChangeTime;
		return this;
	}
	public float getDepthChangeTime() {
		return mDepthChangeTime;
	}

	public boolean isValid() {
		return mValid != INVALID;
	}
	
	/**
	 * Indicates if this ProfileItem is "raw", i.e. if any of its values need to
	 * be inherited from the previous item but they have not been set in this object.
	 * @return true if the ProfileItem is raw, false otherwise
	 */
	public boolean isRaw() {
		return mDepth == INHERIT_DEPTH || mTime == INHERIT_TIME || mTimeType == INHERIT_TIME_TYPE || mGasSource == INHERIT_GASSOURCE;
	}
	
	public ProfileItem merge(ProfileItem next) {
		if(next.getDepth() != INHERIT_DEPTH) {
			mDepth = next.getDepth();
		}
		if(next.getTime() != INHERIT_TIME) {
			mTime = next.getTime();
		}
		if(next.getTimeType() != INHERIT_TIME_TYPE) {
			mTimeType = next.getTimeType();
		}
		if(next.getGasSource() != INHERIT_GASSOURCE) {
			mGasSource = next.getGasSource();
		}
		// The rest are always taken from the next one; they cannot be inherited.
		mLineSource = next.getLineSource();
		mActive = next.isActive();
		mValid = next.getValid();
		return this;
	}

}