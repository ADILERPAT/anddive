package divestoclimb.scuba.dive.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * The DbAdapter handles data access for private datatypes. This
 * includes Missions and their Categories.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class DbAdapter {
	private static final String TABLE_CATEGORY = "category";
	public static final String KEY_CATEGORY_ID = "_id";
	public static final String KEY_CATEGORY_NAME = "Name";

	// This is a special category which is used as a default
	public static final long KEY_CATEGORY_ID_UNFILED = 0;

	private static final String TABLE_MISSION = "mission";
	public static final String KEY_MISSION_ID = "_id";
	public static final String KEY_MISSION_NAME = "Name";
	public static final String KEY_MISSION_LASTUPDATE = "LastUpdate";
	public static final String KEY_MISSION_CATEGORY = "Category";
	
	// More internal variables
	private final Context mCtx;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	// Constructor
	public DbAdapter(Context c) {
		mCtx = c;
		mDbHelper = null;
	}
	
	/**
	 * Open the database. If it cannot be opened, try to create a new instance
	 * of the database. If it cannot be created, throw an exception to signal
	 * the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public DbAdapter open() throws SQLException {
		return open(false);
	}
	
	public DbAdapter open(boolean readOnly) throws SQLException {
		if(mDbHelper == null) {
			mDbHelper = new DatabaseHelper(mCtx);
		}
		mDb = readOnly? mDbHelper.getReadableDatabase(): mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}
	
	/**
	 * Return a Cursor over the list of all defined categories, alphabetically
	 * sorted
	 * 
	 * @return Cursor over all categories
	 */
	public Cursor fetchCategories() {
		return mDb.query(TABLE_CATEGORY, null,
				null, null, null, null, KEY_CATEGORY_NAME);
	}

	// TODO: fetchCategory(String), fetchCategory(long)
	public Cursor fetchCategory(String name) {
		return mDb.query(TABLE_CATEGORY, null,
				KEY_CATEGORY_NAME+"=?", new String[] { name }, null, null, null);
	}

	public Cursor fetchCategory(long category_id) {
		final String idAsString = new Long(category_id).toString();
		return mDb.query(TABLE_CATEGORY, null,
				KEY_CATEGORY_ID+"=?", new String[] { idAsString }, null, null, null);
	}

	public long createCategory(ContentValues initialValues) {
		return mDb.insert(TABLE_CATEGORY, null, initialValues);
	}

	public boolean updateCategory(long category_id, ContentValues newValues) {
		final String idAsString = new Long(category_id).toString();
		return mDb.update(TABLE_CATEGORY, newValues, KEY_CATEGORY_ID+"=?", new String[] { idAsString }) > 0;
	}

	/**
	 * Delete a category. Any missions in this category will be moved to the special Unfiled
	 * category. This method will never delete the Unfiled category itself because of this
	 * requirement.
	 * @param cat_id The ID of the category to delete
	 * @return true if the operation succeeded, false otherwise.
	 */
	public boolean deleteCategory(long cat_id) {
		// Refuse to delete the Unfiled category
		if(cat_id == KEY_CATEGORY_ID_UNFILED) {
			return false;
		}
		String idAsStringArray[] = { new Long(cat_id).toString() };
		// In order to delete a category, we have to move every Mission in that
		// category to the Unfiled category.
		ContentValues newCategory = new ContentValues();
		newCategory.put(KEY_MISSION_CATEGORY, KEY_CATEGORY_ID_UNFILED);
		mDb.update(TABLE_MISSION, newCategory, KEY_MISSION_CATEGORY + "=?", idAsStringArray);
		
		// Now we can delete the category
		return mDb.delete(TABLE_CATEGORY, KEY_CATEGORY_ID+"=?", idAsStringArray) > 0;
	}
	
	public Cursor fetchMissions(String sort, long category) {
		final String where = category != -1? KEY_MISSION_CATEGORY + "=?": null;
		final String args[] = category != -1? new String[] { (new Long(category)).toString() }: null;
		return mDb.query(TABLE_MISSION, null, where, args,
				null, null, sort);
	}
	
	public Cursor fetchMission(long mission_id) {
		final String idAsString = new Long(mission_id).toString();
		return mDb.query(TABLE_MISSION, null, KEY_MISSION_ID + "=?", new String[] { idAsString }, null, null, null);
	}
	
	public long createMission(ContentValues initialValues) {
		return mDb.insert(TABLE_MISSION, null, initialValues);
	}
	
	public boolean updateMission(long mission_id, ContentValues newValues) {
		final String idAsString = new Long(mission_id).toString();
		return mDb.update(TABLE_MISSION, newValues, KEY_MISSION_ID + "=?", new String[] { idAsString }) > 0;
	}
	
	public boolean deleteMission(long mission_id) {
		// TODO: need to clean up dive table in order to do this. Either delete
		// all dives in this mission or change the mission FK to null
		final String idAsStringArray[] = { new Long(mission_id).toString() };
		
		// Now we can delete the mission
		return mDb.delete(TABLE_MISSION, KEY_MISSION_ID + "=?", idAsStringArray) > 0;
	}
}