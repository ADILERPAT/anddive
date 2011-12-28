package divestoclimb.lib.scuba;

import java.util.List;

// TODO add Type support
public class Dive {

	private Long id;
	protected String mName;
	protected long mMissionID;
	protected Mission mMission;
	//protected Record.Fetcher<Mission> mMissionFetcher;
	protected int mMissionOrder;
	protected int mAltitude = 0;
	protected int mAcclimatizationTime = 0;
	protected long mDecosetID;
	protected Decoset mDecoset;
	//protected Record.Fetcher<Decoset> mDecosetFetcher;
	protected List<ProfileItem> mProfile = null;
	//protected ProfileFetcher mProfileFetcher;
	protected Dive mPreviousDive = null;
	protected int mSurfaceInterval = 0;
	protected byte[] mDecoConfig;
	protected byte[] mFinalDecoState;
	protected float mFinalCnsState, mFinalOtuState;
	//protected PreviousDiveFetcher mPreviousDiveFetcher;
	
	protected Units mUnits;
	
	public Dive(Units units, long mission_id, int mission_order, String name, long decoset_id) {
		mUnits = units;
		mMissionID = mission_id;
		mMissionOrder = mission_order;
		mName = name;
		mDecosetID = decoset_id;
	}
	
	public void setId(long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	public String getName() { return mName; }
	public Dive setName(String name) { mName = name; return this; }
	public long getMissionID() { return mMissionID; }
	//public Dive setMissionFetcher(Record.Fetcher<Mission> f) { mMissionFetcher = f; return this; }
	public long getDecosetID() { return mDecosetID; }
	//public Dive setDecosetFetcher(Record.Fetcher<Decoset> f) { mDecosetFetcher = f; return this; }
	//public Dive setProfileFetcher(ProfileFetcher f) { mProfileFetcher = f; return this; }
	public int getAltitude() { return mAltitude; }
	public Dive setAltitude(int altitude) { mAltitude = altitude; return this; }
	public int getAcclimatizationTime() { return mAcclimatizationTime; }
	public Dive setAcclimatizationTime(int acclimatizationTime) { mAcclimatizationTime = acclimatizationTime; return this; }
	public int getSurfaceInterval() { return mSurfaceInterval; }
	public Dive setSurfaceInterval(int surfaceInterval) { mSurfaceInterval = surfaceInterval; return this; }

	public Mission getMission() {
		/*if(mMissionFetcher == null) {
			return null;
		}
		if(mMission == null) {
			mMission = mMissionFetcher.fetch(mMissionID);
		}*/
		return mMission;
	}

	public Decoset getDecoset() {
		/*if(mDecoset == null && mDecosetFetcher != null) {
			mDecoset = mDecosetFetcher.fetch(mDecosetID);
		}*/
		return mDecoset;
	}
	public Dive setDecoset(Decoset decoset) {
		if(decoset.getId() != mDecosetID) {
			mDecoset = decoset;
			mDecosetID = decoset.getId();
		}
		return this;
	}
	
	public List<ProfileItem> getProfile() {
		/*if(mProfile == null && mProfileFetcher != null) {
			mProfile = mProfileFetcher.fetchProfile(this);
		}*/
		return mProfile;
	}

	//public Dive setPreviousDiveFetcher(PreviousDiveFetcher f) { mPreviousDiveFetcher = f; return this; }
	
	/**
	 * Construct the CnsOtu state object initialized to the beginning of the dive.
	 * This object must have a DiveFetcher defined or this
	 * method will throw a NullPointerException. 
	 * @return The initialized CnsOtu object
	 */
	public CnsOtu buildCnsOtu() {
		/*if(mPreviousDive == null) {
			mPreviousDive = mPreviousDiveFetcher.fetchPreviousDive(this);
		}*/
		float cns = 0, otu = 0;
		if(mPreviousDive != null) {
			cns = mPreviousDive.mFinalCnsState;
			otu = mPreviousDive.mFinalOtuState;
		}
		return new CnsOtu(mAltitude, mUnits, cns, otu);
	}
	
	public void saveCnsOtu(CnsOtu state) {
		float cns = state.getCns();
		mFinalCnsState = cns;
		float otu = state.getOtu();
		mFinalOtuState = otu;
	}

	public void initializeDeco(DecoAlgorithm alg) {
		alg.loadConfig(mDecoConfig);
		/*if(mPreviousDive == null) {
			mPreviousDive = mPreviousDiveFetcher.fetchPreviousDive(this);
		}*/
		Dive previous = mPreviousDive;
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
	
	public void saveDeco(DecoAlgorithm alg) {
		byte[] finalDecoState = alg.getState();
		mFinalDecoState = finalDecoState;
	}
	
	/*public static interface PreviousDiveFetcher {
		/**
		 * Retrieve the Dive that was performed prior to the passed one, for use in
		 * repetitive calculations
		 * @param dive The Dive in this mission following the one to look for.
		 * @return The Dive preceding dive. If dive is null, return the last dive in this Mission.
		
		public Dive fetchPreviousDive(Dive dive);
	}
	
	public static interface ProfileFetcher {
		public List<ProfileItem> fetchProfile(Dive dive);
	}*/
}