package divestoclimb.scuba.dive.storage;

import java.util.ArrayList;
import java.util.Collection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import divestoclimb.lib.data.Record;
import divestoclimb.lib.scuba.*;
import divestoclimb.lib.scuba.Decoset.Item;

/**
 * This class contains all the O-R mapping methods needed by andDive itself as well as any
 * other component application. This means it handles mapping for Decosets and Dives to/from
 * their Android classes and ContentProviders.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class PublicORMapper {

	public static final Uri CONTENT_URI = Uri.parse("content://divestoclimb.scuba.dive/");

	public static final String KEY_DECOSET_ID = "_id";
	public static final String KEY_DECOSET_NAME = "Name";
	
	public static final String KEY_DECOSETITEM_ID = "_id";
	public static final String KEY_DECOSETITEM_DECOSET = "DecoSet";
	public static final String KEY_DECOSETITEM_SETPOINT = "SetPointTimes10";
	public static final String KEY_DECOSETITEM_MIXO2 = "MixO2Times10";
	public static final String KEY_DECOSETITEM_MIXHE = "MixHeTimes10";
	public static final String KEY_DECOSETITEM_MAXDEPTH = "MaxDepth";
	
	public static final long BACKGAS_DECOSET_ID = 0;
	
	public static final String KEY_DIVE_ID = "_id";
	public static final String KEY_DIVE_NAME = "Name";
	public static final String KEY_DIVE_TYPE = "Type";
	public static final String KEY_DIVE_MISSION = "Mission";
	public static final String KEY_DIVE_MISSIONORDER = "MissionOrder";
	public static final String KEY_DIVE_SURFACEINTERVAL = "SurfaceInterval";
	public static final String KEY_DIVE_ALTITUDE = "Altitude";
	public static final String KEY_DIVE_ACCLIMATIZATIONTIME = "AcclimatizationTime";
	public static final String KEY_DIVE_DECOSET = "DecoSet";
	public static final String KEY_DIVE_DECOCONFIG = "DecoConfig";
	public static final String KEY_DIVE_FINALDECOSTATE = "FinalDecoState";
	public static final String KEY_DIVE_FINALCNSSTATE = "FinalCnsState";
	public static final String KEY_DIVE_FINALOTUSTATE = "FinalOtuState";
	
	public static final String KEY_PROFILEITEM_ID = "_id";
	public static final String KEY_PROFILEITEM_DIVE = "Dive";
	public static final String KEY_PROFILEITEM_ORDER = "ItemOrder";
	public static final String KEY_PROFILEITEM_DEPTH = "Depth";
	public static final String KEY_PROFILEITEM_TIME = "Time";
	public static final String KEY_PROFILEITEM_TIMETYPE = "TimeType";
	public static final String KEY_PROFILEITEM_SETPOINT = "SetPointTimes10";
	public static final String KEY_PROFILEITEM_MIXO2 = "MixO2Times10";
	public static final String KEY_PROFILEITEM_MIXHE = "MixHeTimes10";
	public static final String KEY_PROFILEITEM_SOURCE = "Source";
	public static final String KEY_PROFILEITEM_ACTIVE = "Active";
	public static final String KEY_PROFILEITEM_VALID = "Valid";

	protected Context mCtx;
	protected Units mUnits;
	
	protected Decoset mFlyDecoset = new Decoset(null);
	protected Decoset.Item mFlyDecosetItem = new Decoset.Item(Record.NO_ID, 0, null);
	protected Dive mFlyDive = new Dive(mUnits, Record.NO_ID, 0, null, Record.NO_ID);
	protected ProfileItem mFlyProfileItem = new ProfileItem(Record.NO_ID, -1, false);
	protected Mission mFlyMission = new Mission(Record.NO_ID, null);
	protected Mix mFlyMix = new Mix(0, 0);
	protected Setpoint mFlySetpoint = new Setpoint(0, mFlyMix);

	/**
	 * If a class does not need to retrieve anything unit-specific, this
	 * constructor may be used. It creates an internal Units object in Metric.
	 * @param ctx The current context
	 */
	public PublicORMapper(Context ctx) {
		this(ctx, new Units(Units.METRIC));
	}

	public PublicORMapper(Context ctx, Units units) {
		mCtx = ctx;
		mUnits = units;
	}

	/**
	 * Builds a GasSource instance from data in the passed Cursor
	 * @param c The Cursor to read, advanced to the correct position
	 * @param setpointIndex The column index of the setpoint field
	 * @param fo2Index The column index of the fO2 field
	 * @param fheIndex The column index of the fHe field
	 * @param mixInstance An optional existing Mix instance to use in the return
	 * @param setpointInstance An optional existing Setpoint instance to use in the return
	 * @return A GasSource instance corresponding to the data read from the Cursor
	 */
	protected GasSource decodeGasSource(Cursor c, int setpointIndex, int fo2Index, int fheIndex, Mix mixInstance, Setpoint setpointInstance) {
		// Although the columns are kept in the database as integers, I fetch them
		// as floats so the division doesn't round them off to 0.
		if(! c.isNull(fo2Index) && ! c.isNull(fheIndex)) {
			if(mixInstance == null) {
				mixInstance = new Mix(c.getFloat(fo2Index) / 1000, c.getFloat(fheIndex) / 1000);
			} else {
				mixInstance.setfO2(c.getFloat(fo2Index) / 1000);
				mixInstance.setfHe(c.getFloat(fheIndex) / 1000);
			}
		} else {
			mixInstance = null;
		}
		if(c.isNull(setpointIndex)) {
			return mixInstance;
		} else {
			if(setpointInstance == null) {
				setpointInstance = new Setpoint(c.getFloat(setpointIndex) / 10, mixInstance);
			} else {
				setpointInstance.setPo2(c.getFloat(setpointIndex) / 10);
				setpointInstance.setDiluent(mixInstance);
			}
			return setpointInstance;
		}
	}
	
	/**
	 * Encodes the given GasSource into the passed ContentValues structure, using
	 * the given keys.
	 * @param source The GasSource to encode
	 * @param v The ContentValues to encode the GasSource into
	 * @param setpointKey The key to use to store the setpoint
	 * @param fo2Key The key to use to store the fO2
	 * @param fheKey The key to use to store the fHe
	 */
	protected void encodeGasSource(GasSource source, ContentValues v, String setpointKey, String fo2Key, String fheKey) {
		Mix mix;
		if(source instanceof Setpoint) {
			v.put(setpointKey, Math.round(((Setpoint)source).getPo2() * 10));
			mix = ((Setpoint)source).getDiluent();
		} else if(source instanceof Mix) {
			v.putNull(setpointKey);
			mix = (Mix)source;
		} else {
			throw new IllegalArgumentException("invalid GasSource; must be a Mix or Setpoint");
		}
		if(mix == null) {
			v.putNull(fo2Key);
			v.putNull(fheKey);
		} else {
			v.put(fo2Key, Math.round((float)mix.getfO2() * 1000));
			v.put(fheKey, Math.round((float)mix.getfHe() * 1000));
		}
	}
	
	/**
	 * Fetch a Cursor of all Decosets in the system
	 * @return A Cursor of all Decosets
	 */
	public Cursor fetchDecosets() {
		return mCtx.getContentResolver().query(Uri.withAppendedPath(CONTENT_URI, "decosets"), null, null, null, null);
	}
	
	/**
	 * Fetch a specific Decoset by ID
	 * @param id The ID of the Decoset to retrieve
	 * @return A Decoset object representing the Decoset requested, or null if
	 * no decoset was found matching the given ID
	 */
	public Decoset fetchDecoset(long id) {
		final Cursor c = mCtx.getContentResolver().query(
				Uri.withAppendedPath(CONTENT_URI, "decosets/" + String.valueOf(id)
				), null, null, null, null);
		if(c != null) {
			c.moveToFirst();
			final Decoset decoset = fetchDecoset(c);
			c.close();
			return decoset;
		}
		return null;
	}

	protected Record.Fetcher<Decoset> mDecosetFetcher = new Record.Fetcher<Decoset>() {
		public Decoset fetch(long id) {
			return fetchDecoset(id);
		}
	};

	public Decoset fetchDecoset(Cursor c) { return fetchDecoset(c, false); }
	public Decoset fetchDecoset(Cursor c, boolean useFlyweight) { return fetchDecoset(c, useFlyweight? mFlyDecoset: null); }
	public Decoset fetchDecoset(Cursor c, Decoset instance) {
		final long id = c.getLong(c.getColumnIndexOrThrow(KEY_DECOSET_ID));
		final String name = c.getString(c.getColumnIndexOrThrow(KEY_DECOSET_NAME));
		if(instance == null) {
			instance = new Decoset(id, name);
		} else {
			instance.reset(id, name);
		}
		instance.setItemFetcher(mDecosetItemFetcher)
				.setItemUpdater(mDecosetItemUpdater)
				.setUpdater(mDecosetUpdater);
		return instance;
	}
	
	protected Decoset.ItemFetcher mDecosetItemFetcher = new Decoset.ItemFetcher() {
		@Override
		public Collection<Item> lookupItems(Decoset d) {
			if(d.isPhantom()) {
				// The decoset must be saved first 
				if(! d.commit()) {
					return null;
				}
			}
			Cursor c = fetchDecosetItems(d.getID());
			if(c == null) {
				return null;
			}
			Collection<Decoset.Item> items = new ArrayList<Decoset.Item>();
			while(! c.isLast()) {
				c.moveToNext();
				items.add(fetchDecosetItem(c));
			}
			return items;
		}
	};

	protected ContentValues getDecosetValues(Decoset set) {
		final ContentValues v = new ContentValues();
		v.put(KEY_DECOSET_NAME, set.getName());
		return v;
	}
	
	public Record.Updater mDecosetUpdater = new Record.Updater() {

		public long doCreate(Record set) {
			final Uri newItem = mCtx.getContentResolver().insert(Uri.withAppendedPath(CONTENT_URI, "decosets"), getDecosetValues((Decoset)set));
			if(newItem == null) {
				return -1;
			}
			return Long.parseLong(newItem.getLastPathSegment());
		}

		public boolean doUpdate(Record set) {
			final Uri uri = Uri.withAppendedPath(CONTENT_URI, "decosets/" + String.valueOf(set.getID()));
			return mCtx.getContentResolver().update(uri, getDecosetValues((Decoset)set), null, null) > 0;
		}

		public boolean doDelete(Record set) {
			return deleteDecoset(set.getID());
		}
	};
	
	public boolean deleteDecoset(long decoset_id) {
		final Uri uri = Uri.withAppendedPath(CONTENT_URI, "decosets/" + String.valueOf(decoset_id));
		return mCtx.getContentResolver().delete(uri, null, null) > 0;
	}

	public Cursor fetchDecosetItems(long set_id) {
		final Uri uri = Uri.withAppendedPath(CONTENT_URI, "decosets/" + String.valueOf(set_id) + "/items");
		return mCtx.getContentResolver().query(uri, null, null, null, null);
	}
	
	public Decoset.Item fetchDecosetItem(long id) {
		final Cursor c = mCtx.getContentResolver().query(
				Uri.withAppendedPath(CONTENT_URI, "decosets/items/" + String.valueOf(id)
				), null, null, null, null);
		if(c != null) {
			c.moveToFirst();
			final Decoset.Item i = fetchDecosetItem(c);
			c.close();
			return i;
		}
		return null;
	}

	public Decoset.Item fetchDecosetItem(Cursor c) {
		return fetchDecosetItem(c, false);
	}
	
	public Decoset.Item fetchDecosetItem(Cursor c, boolean useFlyweight) {
		return fetchDecosetItem(c, useFlyweight? mFlyDecosetItem: null,
				useFlyweight? mFlyMix: null, useFlyweight? mFlySetpoint: null);
	}
	
	public Decoset.Item fetchDecosetItem(Cursor c, Decoset.Item instance, Mix mixInstance, Setpoint setpointInstance) {
		final long id = c.getLong(c.getColumnIndexOrThrow(KEY_DECOSETITEM_ID)),
				decoset_id = c.getLong(c.getColumnIndexOrThrow(KEY_DECOSETITEM_DECOSET));
		final int depth = Math.round(
			mUnits.convertDepth(c.getFloat(c.getColumnIndexOrThrow(KEY_DECOSETITEM_MAXDEPTH)), Units.METRIC));
		final GasSource gasSource = decodeGasSource(c, c.getColumnIndexOrThrow(KEY_DECOSETITEM_SETPOINT),
				c.getColumnIndexOrThrow(KEY_DECOSETITEM_MIXO2),
				c.getColumnIndexOrThrow(KEY_DECOSETITEM_MIXHE),
				mixInstance, setpointInstance
		);
		if(instance == null) {
			instance = new Decoset.Item(id, decoset_id, depth, gasSource);
		} else {
			instance.reset(id, decoset_id, depth, gasSource);
		}
		instance.setUpdater(mDecosetItemUpdater);
		return instance;
	}
	
	protected ContentValues getDecosetItemValues(Decoset.Item item) {
		final ContentValues v = new ContentValues();
		encodeGasSource(item.getGasSource(), v, KEY_DECOSETITEM_SETPOINT, KEY_DECOSETITEM_MIXO2, KEY_DECOSETITEM_MIXHE);
		v.put(KEY_DECOSETITEM_MAXDEPTH, Units.convertDepth(item.getMaxDepth(), mUnits.getCurrentSystem(), Units.METRIC));
		return v;
	}
	
	public Record.Updater mDecosetItemUpdater = new Record.Updater() {
	
		public long doCreate(Record item) {
			final Decoset.Item i = (Decoset.Item)item;
			final Uri uri = Uri.withAppendedPath(CONTENT_URI, "decosets/" + String.valueOf(i.getDecosetID()) + "/items");
			final Uri newItem = mCtx.getContentResolver().insert(uri, getDecosetItemValues(i));
			if(newItem == null) {
				return -1;
			}
			return Long.parseLong(newItem.getLastPathSegment());
		}

		public boolean doUpdate(Record item) {
			final Uri uri = Uri.withAppendedPath(CONTENT_URI, "decosets/items/" + String.valueOf(item.getID()));
			return mCtx.getContentResolver().update(uri, getDecosetItemValues((Decoset.Item)item), null, null) > 0;
		}

		public boolean doDelete(Record item) {
			final Uri uri = Uri.withAppendedPath(CONTENT_URI, "decosets/items/" + String.valueOf(item.getID()));
			return mCtx.getContentResolver().delete(uri, null, null) > 0;
		}
	};

	public Cursor fetchDives() {
		final Uri uri = Uri.withAppendedPath(CONTENT_URI, "dives");
		return mCtx.getContentResolver().query(uri, null, null, null, null);
	}
	
	public Dive fetchDive(long id) {
		final Cursor c = mCtx.getContentResolver().query(
				Uri.withAppendedPath(CONTENT_URI, "dives/" + String.valueOf(id)),
				null, null, null, null);
		if(c != null) {
			c.moveToFirst();
			final Dive d = fetchDive(c);
			c.close();
			return d;
		}
		return null;
	}
	
	public Dive fetchDive(Cursor c) { return fetchDive(c, false); }
	public Dive fetchDive(Cursor c, boolean useFlyweight) { return fetchDive(c, useFlyweight? mFlyDive: null); }
	public Dive fetchDive(Cursor c, Dive instance) {
		final long id = c.getLong(c.getColumnIndexOrThrow(KEY_DIVE_ID)),
				mission_id = c.getLong(c.getColumnIndexOrThrow(KEY_DIVE_MISSION)),
				decoset_id = c.getLong(c.getColumnIndexOrThrow(KEY_DIVE_DECOSET));
		final int mission_order = c.getInt(c.getColumnIndexOrThrow(KEY_DIVE_MISSIONORDER)),
				surface_interval = c.getInt(c.getColumnIndexOrThrow(KEY_DIVE_SURFACEINTERVAL)),
				altitude = Math.round(mUnits.convertDepth(c.getInt(c.getColumnIndexOrThrow(KEY_DIVE_ALTITUDE)), Units.METRIC)),
				acclim_time = c.getInt(c.getColumnIndexOrThrow(KEY_DIVE_ACCLIMATIZATIONTIME));
		final String name = c.getString(c.getColumnIndexOrThrow(KEY_DIVE_NAME));
		final float cns = c.getFloat(c.getColumnIndexOrThrow(KEY_DIVE_FINALCNSSTATE)),
				otu = c.getFloat(c.getColumnIndexOrThrow(KEY_DIVE_FINALOTUSTATE));
		final byte[] decoConfig = c.getBlob(c.getColumnIndexOrThrow(KEY_DIVE_DECOCONFIG)),
				finalDecoState = c.getBlob(c.getColumnIndexOrThrow(KEY_DIVE_FINALDECOSTATE));
		if(instance == null) {
			instance = new Dive(mUnits, id, mission_id, mission_order, name, decoset_id, surface_interval, altitude, acclim_time, decoConfig, finalDecoState, cns, otu);
		} else {
			instance.reset(id, mission_id, mission_order, name, decoset_id, surface_interval, altitude, acclim_time, decoConfig, finalDecoState, cns, otu);
		}
		instance.setDecosetFetcher(mDecosetFetcher)
			.setPreviousDiveFetcher(mPreviousDiveFetcher)
			.setUpdater(mDiveUpdater);
		return instance;
	}
	
	protected Dive.PreviousDiveFetcher mPreviousDiveFetcher = new Dive.PreviousDiveFetcher() {
		public Dive fetchPreviousDive(Dive dive) {
			final Cursor c = mCtx.getContentResolver().query(
					Uri.withAppendedPath(CONTENT_URI, "dives"),
					null, KEY_DIVE_MISSIONORDER + "<? AND " + KEY_DIVE_MISSION + "=?",
					new String[] { String.valueOf(dive.getOrder()), String.valueOf(dive.getMissionID()) },
					KEY_DIVE_MISSIONORDER + " DESC");
			if(c != null) {
				c.moveToFirst();
				return fetchDive(c);
			}
			return null;
		}
	};
	
	public Record.Updater mDiveUpdater = new Record.Updater() {
		@Override
		public long doCreate(Record r) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean doDelete(Record r) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean doUpdate(Record r) {
			// TODO Auto-generated method stub
			return false;
		}
	};
	
	public Cursor fetchProfileItems(long dive_id) {
		final Uri uri = Uri.withAppendedPath(CONTENT_URI, "dives/" + String.valueOf(dive_id) + "/profileitems");
		return mCtx.getContentResolver().query(uri, null, null, null, null);
	}
	
	public ProfileItem fetchProfileItem(long id) {
		final Cursor c = mCtx.getContentResolver().query(
				Uri.withAppendedPath(CONTENT_URI, "dives/profileitems" + String.valueOf(id)
				), null, null, null, null);
		if(c != null) {
			c.moveToFirst();
			final ProfileItem p = fetchProfileItem(c);
			c.close();
			return p;
		}
		return null;
	}
	
	public ProfileItem fetchProfileItem(Cursor c) { return fetchProfileItem(c, false); }
	public ProfileItem fetchProfileItem(Cursor c, boolean useFlyweight) { return fetchProfileItem(c, useFlyweight? mFlyProfileItem: null,
			useFlyweight? mFlyMix: null, useFlyweight? mFlySetpoint: null); }
	public ProfileItem fetchProfileItem(Cursor c, ProfileItem profileItemInstance,
			Mix mixInstance, Setpoint setpointInstance) {
		final long id = c.getLong(c.getColumnIndexOrThrow(KEY_PROFILEITEM_ID)),
				dive_id = c.getLong(c.getColumnIndexOrThrow(KEY_PROFILEITEM_DIVE));
		final int order = c.getInt(c.getColumnIndexOrThrow(KEY_PROFILEITEM_ORDER)),
				depth = Math.round(mUnits.convertDepth(c.getInt(c.getColumnIndexOrThrow(KEY_PROFILEITEM_DEPTH)), Units.METRIC)),
				time = c.getInt(c.getColumnIndexOrThrow(KEY_PROFILEITEM_TIME)),
				timeType = c.getInt(c.getColumnIndexOrThrow(KEY_PROFILEITEM_TIMETYPE)),
				source = c.getInt(c.getColumnIndexOrThrow(KEY_PROFILEITEM_SOURCE)),
				valid = c.getInt(c.getColumnIndexOrThrow(KEY_PROFILEITEM_VALID));
		final boolean active = c.getInt(c.getColumnIndexOrThrow(KEY_PROFILEITEM_ACTIVE)) == 1;
		final GasSource gasSource = decodeGasSource(c, c.getColumnIndexOrThrow(KEY_PROFILEITEM_SETPOINT),
				c.getColumnIndexOrThrow(KEY_PROFILEITEM_MIXO2),
				c.getColumnIndexOrThrow(KEY_PROFILEITEM_MIXHE),
				mixInstance, setpointInstance
		);
		if(profileItemInstance == null) {
			profileItemInstance = new ProfileItem(id, dive_id, order, depth, time, timeType, gasSource, source, active, valid);
		} else {
			profileItemInstance.reset(id, dive_id, order, depth, time, timeType, gasSource, source, active, valid);
		}
		profileItemInstance.setUpdater(mProfileItemUpdater);
		return profileItemInstance;
	}
	
	public Record.Updater mProfileItemUpdater = new Record.Updater() {
		@Override
		public long doCreate(Record profileItem) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean doDelete(Record profileItem) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean doUpdate(Record profileItem) {
			// TODO Auto-generated method stub
			return false;
		}
	};
}