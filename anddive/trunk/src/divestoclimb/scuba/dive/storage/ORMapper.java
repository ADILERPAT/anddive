package divestoclimb.scuba.dive.storage;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import divestoclimb.lib.data.Category;
import divestoclimb.lib.data.Record;
import divestoclimb.lib.scuba.Mission;
import divestoclimb.lib.scuba.Units;
import divestoclimb.scuba.dive.data.android.MetaMission;

/**
 * This is andDive's O-R mapping class. It inherits the base O-R mapping routines that are
 * used for the publicly available objects, and adds objects needed internal to andDive. This
 * includes Categories and Missions.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class ORMapper extends PublicORMapper {

	protected DbAdapter mDbAdapter;
	
	// This DateFormat is kept in the default time zone. It is used to parse Date
	// objects for display in local time.
	protected static final DateFormat mDateFormatLocal = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// This DateFormat has its time zone changed to UTC. It is used to parse and
	// format Dates directly to and from the database.
	protected static final DateFormat mDateFormatGMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static {
		mDateFormatGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	// Flyweight objects
	// These objects are optionally reused by the Cursor mapping methods below. They are more efficient
	// than creating and destroying lots of objects over and over, but they are not thread-safe.
	private Category mFlyCategory;
	private MetaMission mFlyMission;
	
	public ORMapper(Context ctx, Units units) {
		super(ctx, units);
		mDbAdapter = new DbAdapter(ctx);
		mFlyMission = null;
		mFlyCategory = null;
	}
	
	public DbAdapter getDbAdapter() {
		return mDbAdapter;
	}
	
	public Category fetchCategory(String name) {
		Cursor c = mDbAdapter.fetchCategory(name);
		if(c != null) {
			c.moveToFirst();
			return fetchCategory(c);
		} else {
			return null;
		}
	}

	public Category fetchCategory(long category_id) {
		Cursor c = mDbAdapter.fetchCategory(category_id);
		if(c != null) {
			c.moveToFirst();
			return fetchCategory(c);
		} else {
			return null;
		}
	}

	public Category fetchCategory(Cursor c) { return fetchCategory(c, false); }
	public Category fetchCategory(Cursor c, boolean useFlyweight) { return fetchCategory(c, useFlyweight? mFlyCategory: null); }
	public Category fetchCategory(Cursor c, Category instance) {
		final long id = c.getLong(c.getColumnIndexOrThrow(DbAdapter.KEY_CATEGORY_ID));
		final String name = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_CATEGORY_NAME));
		if(instance == null) {
			instance = new Category(id, name);
		} else {
			instance.reset(id, name);
		}
		instance.setUpdater(mCategoryUpdater);
		return instance;
	}
	
	protected ContentValues getCategoryValues(Category category) {
		final ContentValues v = new ContentValues();
		v.put(DbAdapter.KEY_CATEGORY_NAME, category.getName());
		return v;
	}
	
	protected Record.Updater mCategoryUpdater = new Record.Updater() {
		public long doCreate(Record category) {
			return mDbAdapter.createCategory(getCategoryValues((Category)category));
		}
	
		public boolean doUpdate(Record category) {
			return mDbAdapter.updateCategory(category.getID(), getCategoryValues((Category)category));
		}
		
		public boolean doDelete(Record category) {
			return mDbAdapter.deleteCategory(category.getID());
		}
	};

	public Mission fetchMission(long mission_id) {
		Cursor c = mDbAdapter.fetchMission(mission_id);
		if(c != null) {
			c.moveToFirst();
			return fetchMission(c);
		} else {
			return null;
		}
	}

	public MetaMission fetchMission(Cursor c) { return fetchMission(c, false); }
	public MetaMission fetchMission(Cursor c, boolean useFlyweight) { return fetchMission(c, useFlyweight? mFlyMission: null); }
	public MetaMission fetchMission(Cursor c, MetaMission instance) {
		final long id = c.getLong(c.getColumnIndexOrThrow(DbAdapter.KEY_MISSION_ID)),
				category = c.getLong(c.getColumnIndexOrThrow(DbAdapter.KEY_MISSION_CATEGORY));
		final String name = c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_MISSION_NAME));
		Date lastupdate = null;
		try {
			lastupdate = mDateFormatGMT.parse(c.getString(c.getColumnIndexOrThrow(DbAdapter.KEY_MISSION_LASTUPDATE)));
		} catch(ParseException e) { }

		if(instance == null) {
			instance = new MetaMission(id, category, name, lastupdate);
		}
		instance.reset(id, category, name, lastupdate);
		instance.setUpdater(mMissionUpdater);
		return instance;
	}
	
	protected ContentValues getMissionValues(MetaMission mission) {
		ContentValues v = new ContentValues();
		v.put(DbAdapter.KEY_MISSION_CATEGORY, mission.getCategoryID());
		v.put(DbAdapter.KEY_MISSION_NAME, mission.getName());
		v.put(DbAdapter.KEY_MISSION_LASTUPDATE, mDateFormatGMT.format(mission.getLastUpdate()));
		return v;
	}
	
	protected Record.Updater mMissionUpdater = new Record.Updater() {
		public long doCreate(Record mission) {
			return mDbAdapter.createMission(getMissionValues((MetaMission)mission));
		}

		public boolean doUpdate(Record mission) {
			return mDbAdapter.updateMission(mission.getID(), getMissionValues((MetaMission)mission));
		}
		
		public boolean doDelete(Record mission) {
			return mDbAdapter.deleteMission(mission.getID());
		}
	};
}