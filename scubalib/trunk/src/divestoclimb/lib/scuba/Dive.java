package divestoclimb.lib.scuba;

import divestoclimb.lib.data.Record;

// TODO add Type support
public class Dive extends Record implements Record.Orderable<Dive> {

	protected String mName;
	protected long mMissionID;
	protected Mission mMission;
	protected Record.Fetcher<Mission> mMissionFetcher;
	protected int mMissionOrder;
	protected int mAltitude = 0;
	protected int mAcclimatizationTime = 0;
	protected long mDecosetID;
	protected Decoset mDecoset;
	protected Record.Fetcher<Decoset> mDecosetFetcher;
	protected ProfileIterator<? extends ProfileItem> mProfile = null;
	protected int mSurfaceInterval = 0;
	protected byte[] mDecoConfig;
	protected byte[] mFinalDecoState;
	protected float mFinalCnsState, mFinalOtuState;
	protected PreviousDiveFetcher mPreviousDiveFetcher;
	
	protected Units mUnits;
	
	public Dive(Units units, long mission_id, int mission_order, String name, long decoset_id) {
		super();
		mUnits = units;
		mMissionID = mission_id;
		mMissionOrder = mission_order;
		mName = name;
		mDecosetID = decoset_id;
	}
	
	public Dive(Units units, long id, long mission_id, int mission_order, String name, long decoset_id, int surface_interval, int altitude, int acclimatization_time, byte[] decoConfig, byte[] finalDecoState, float finalCnsState, float finalOtuState) {
		super(id);
		reset(id, mission_id, mission_order, name, decoset_id, surface_interval, altitude, acclimatization_time, decoConfig, finalDecoState, finalCnsState, finalOtuState);
	}

	public void reset(long id, long mission_id, int mission_order, String name, long decoset_id, int surface_interval, int altitude, int acclimatization_time, byte[] decoConfig, byte[] finalDecoState, float finalCnsState, float finalOtuState) {
		super.reset(id);
		mName = name;
		if(mMissionID != mission_id) {
			mMission = null;
		}
		mMissionID = mission_id;
		mMissionOrder = mission_order;
		if(mDecosetID != decoset_id) {
			mDecoset = null;
		}
		mDecosetID = decoset_id;
		mSurfaceInterval = surface_interval;
		mAltitude = altitude;
		mAcclimatizationTime = acclimatization_time;
		mDecoConfig = decoConfig;
		mFinalDecoState = finalDecoState;
		mFinalCnsState = finalCnsState;
		mFinalOtuState = finalOtuState;
	}
	
	public String getName() { return mName; }
	public Dive setName(String name) { if(mName != name) { mName = name; mDirty = true; } return this; }
	public long getMissionID() { return mMissionID; }
	public Dive setMissionFetcher(Record.Fetcher<Mission> f) { mMissionFetcher = f; return this; }
	public long getDecosetID() { return mDecosetID; }
	public Dive setDecosetFetcher(Record.Fetcher<Decoset> f) { mDecosetFetcher = f; return this; }
	public int getAltitude() { return mAltitude; }
	public Dive setAltitude(int altitude) { if(altitude != mAltitude) { mAltitude = altitude; mDirty = true; } return this; }
	public int getAcclimatizationTime() { return mAcclimatizationTime; }
	public Dive setAcclimatizationTime(int acclimatizationTime) { if(acclimatizationTime != mAcclimatizationTime) { mAcclimatizationTime = acclimatizationTime; mDirty = true; } return this; }
	public int getSurfaceInterval() { return mSurfaceInterval; }
	public Dive setSurfaceInterval(int surfaceInterval) { if(surfaceInterval != mSurfaceInterval) { mSurfaceInterval = surfaceInterval; mDirty = true; } return this; }

	public Mission getMission() {
		if(mMissionFetcher == null) {
			return null;
		}
		if(mMission == null) {
			mMission = mMissionFetcher.fetch(mMissionID);
		}
		return mMission;
	}

	public Decoset getDecoset() {
		if(mDecosetFetcher == null) {
			return null;
		}
		if(mDecoset == null) {
			mDecoset = mDecosetFetcher.fetch(mDecosetID);
		}
		return mDecoset;
	}
	public Dive setDecoset(Decoset decoset) {
		if(decoset.getId() != mDecosetID) {
			mDecoset = decoset;
			mDecosetID = decoset.getId();
			mDirty = true;
		}
		return this;
	}

	public Dive setPreviousDiveFetcher(PreviousDiveFetcher f) { mPreviousDiveFetcher = f; return this; }
	
	@Override
	public int getOrder() {
		return mMissionOrder;
	}
	
	@Override
	public Dive setOrder(int order) {
		if(order != mMissionOrder) {
			mMissionOrder = order;
			mDirty = true;
			// TODO if the order of dives in a mission changes, all the deco info needs to be cleared and
			// recomputed.
		}
		return this;
	}
	
	/**
	 * Construct the CnsOtu state object initialized to the beginning of the dive.
	 * This object must have a DiveFetcher defined or this
	 * method will throw a NullPointerException. 
	 * @return The initialized CnsOtu object
	 */
	public CnsOtu buildCnsOtu() {
		Dive previous = mPreviousDiveFetcher.fetchPreviousDive(this);
		return new CnsOtu(mAltitude, mUnits, previous.mFinalCnsState, previous.mFinalOtuState);
	}
	
	public void saveCnsOtu(CnsOtu state) {
		float cns = state.getCns();
		if(cns != mFinalCnsState) {
			mFinalCnsState = cns;
			mDirty = true;
		}
		float otu = state.getOtu();
		if(otu != mFinalOtuState) {
			mFinalOtuState = cns;
			mDirty = true;
		}
	}

	public void initializeDeco(DecoAlgorithm<?> alg) {
		alg.loadConfig(mDecoConfig);
		Dive previous = mPreviousDiveFetcher.fetchPreviousDive(this);
		if(previous != null) {
			alg.loadState(previous.mFinalDecoState);
		}
		alg.setDecoset(getDecoset());

		Mix air = new Mix(0.21f, 0);
		if(mSurfaceInterval > mAcclimatizationTime && previous != null) {
			// The diver ascended from the last dive before
			// changing altitude. Account for that time first.
			alg.surfaceInterval(previous.getAltitude(), mSurfaceInterval - mAcclimatizationTime, air);
		}
		alg.surfaceInterval(mAltitude, mAcclimatizationTime, air);
	}
	
	public void saveDeco(DecoAlgorithm<?> alg) {
		byte[] finalDecoState = alg.getState();
		if(finalDecoState != mFinalDecoState) {
			mFinalDecoState = finalDecoState;
			mDirty = true;
		}
	}
	
	public static interface PreviousDiveFetcher {
		/**
		 * Retrieve the Dive that was performed prior to the passed one, for use in
		 * repetitive calculations
		 * @param dive The Dive in this mission following the one to look for.
		 * @return The Dive preceding dive. If dive is null, return the last dive in this Mission.
		 */
		public Dive fetchPreviousDive(Dive dive);
	}
}