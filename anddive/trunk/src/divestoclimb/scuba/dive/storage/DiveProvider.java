package divestoclimb.scuba.dive.storage;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * DiveProvider is a ContentProvider that allows external access to all
 * dives, profile items, decosets, and decoset items.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class DiveProvider extends ContentProvider {
	
	public static final Uri CONTENT_URI = Uri.parse("content://divestoclimb.scuba.dive/");
	
	private static final String TABLE_DIVE = "dive";
	private static final String TABLE_PROFILEITEM = "profileitem";
	private static final String TABLE_DECOSET = "decoset";
	private static final String TABLE_DECOSETITEM = "decosetitem";

	private DatabaseHelper mDbHelper;

	// URI matching
	private static final int DECOSETS = 1;
	private static final int SPECIFIC_SET = 2;
	private static final int SET_ITEMS = 3;
	private static final int SPECIFIC_ITEM = 4;
	private static final int DIVES = 5;
	private static final int SPECIFIC_DIVE = 6;
	private static final int DIVE_PROFILEITEMS = 7;
	private static final int SPECIFIC_PROFILEITEM = 8;
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "decosets", DECOSETS);
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "decosets/#", SPECIFIC_SET);
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "decosets/#/items", SET_ITEMS);
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "decosets/items/#", SPECIFIC_ITEM);
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "dives", DIVES);
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "dives/#", SPECIFIC_DIVE);
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "dives/#/profileitems", DIVE_PROFILEITEMS);
		URI_MATCHER.addURI(CONTENT_URI.getAuthority(), "dives/profileitems/#", SPECIFIC_PROFILEITEM);
	}

	@Override
	public String getType(Uri uri) {
		switch(URI_MATCHER.match(uri)) {
		case DECOSETS:				return "vnd.android.cursor.dir/vnd.divestoclimb.scuba.dive.decoset";
		case SPECIFIC_SET:			return "vnd.android.cursor.item/vnd.divestoclimb.scuba.dive.decoset";
		case SET_ITEMS:				return "vnd.android.cursor.dir/vnd.divestoclimb.scuba.dive.decoset.item";
		case SPECIFIC_ITEM:			return "vnd.android.cursor.item/vnd.divestoclimb.scuba.dive.decoset.item";
		case DIVES:					return "vnd.android.cursor.dir/vnd.divestoclimb.scuba.dive.dive";
		case SPECIFIC_DIVE:			return "vnd.android.cursor.item/vnd.divestoclimb.scuba.dive.dive";
		case DIVE_PROFILEITEMS:		return "vnd.android.cursor.dir/vnd.divestoclimb.scuba.dive.profile";
		case SPECIFIC_PROFILEITEM:	return "vnd.android.cursor.item/vnd.divestoclimb.scuba.dive.profile";
		default:					return null;
		}
	}

	@Override
	public boolean onCreate() {
		mDbHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	private static String addToSelection(String oldSelection, String addition) {
		if(oldSelection.length() > 0) {
			return addition + " AND (" + oldSelection + ")";
		} else {
			return addition;
		}
	}
	
	private static String[] addToSelectionArgs(String[] oldArgs, String newArg) {
		final String[] newSelectionArgs = new String[oldArgs.length + 1];
		newSelectionArgs[0] = newArg;
		for(int i = 0; i < oldArgs.length; i ++) {
			newSelectionArgs[i + 1] = oldArgs[i];
		}
		return newSelectionArgs;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		final SQLiteDatabase db = mDbHelper.getReadableDatabase();
		final SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		String table;
		switch(URI_MATCHER.match(uri)) {
		case SPECIFIC_SET:
			selection = PublicORMapper.KEY_DECOSET_ID + "=?";
			selectionArgs = new String[] { uri.getLastPathSegment() };
		case DECOSETS:
			table = TABLE_DECOSET;
			if(sortOrder == null) {
				sortOrder = PublicORMapper.KEY_DECOSET_NAME;
			}
			break;
		case SPECIFIC_ITEM:
			selection = PublicORMapper.KEY_DECOSETITEM_ID + "=?";
			selectionArgs = new String[] { uri.getLastPathSegment() };
			table = TABLE_DECOSETITEM;
			break;
		case SET_ITEMS:
			qBuilder.appendWhere(PublicORMapper.KEY_DECOSETITEM_DECOSET + "=");
			qBuilder.appendWhereEscapeString(uri.getPathSegments().get(1));
			table = TABLE_DECOSETITEM;
			if(sortOrder == null) {
				sortOrder = PublicORMapper.KEY_DECOSETITEM_MAXDEPTH + " DESC";
			}
			break;
		case SPECIFIC_DIVE:
			selection = PublicORMapper.KEY_DIVE_ID + "=?";
			selectionArgs = new String[] { uri.getLastPathSegment() };
		case DIVES:
			table = TABLE_DIVE;
			break;
		case DIVE_PROFILEITEMS:
			table = TABLE_PROFILEITEM;
			qBuilder.appendWhere(PublicORMapper.KEY_PROFILEITEM_DIVE + "=");
			qBuilder.appendWhereEscapeString(uri.getPathSegments().get(1));
			if(sortOrder == null) {
				sortOrder = PublicORMapper.KEY_PROFILEITEM_ORDER;
			}
			break;
		case SPECIFIC_PROFILEITEM:
			selection = PublicORMapper.KEY_PROFILEITEM_ID + "=?";
			selectionArgs = new String[] { uri.getLastPathSegment() };
			table = TABLE_PROFILEITEM;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		qBuilder.setTables(table);
		Cursor c = qBuilder.query(db, projection, selection, selectionArgs,
				null, null, sortOrder);
		if(c != null) {
			c.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		String newPathBase, table;
		switch(URI_MATCHER.match(uri)) {
		case SET_ITEMS:
			final String decoset_id = uri.getPathSegments().get(1);
			if(Long.valueOf(decoset_id) == PublicORMapper.BACKGAS_DECOSET_ID) {
				// Cannot add items to the backgas decoset
				return null;
			}
			values.put(PublicORMapper.KEY_DECOSETITEM_DECOSET, decoset_id);
			newPathBase = "decosets/items/";
			table = TABLE_DECOSETITEM;
			break;
		case DECOSETS:
			newPathBase = "decosets/";
			table = TABLE_DECOSET;
			break;
		case DIVES:
			newPathBase = "dives/";
			table = TABLE_DIVE;
		case DIVE_PROFILEITEMS:
			final String dive_id = uri.getPathSegments().get(1);
			values.put(PublicORMapper.KEY_PROFILEITEM_DIVE, dive_id);
			newPathBase = "dives/profileitems/";
			table = TABLE_PROFILEITEM;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		final long new_id = db.insert(table, null, values);
		if(new_id == -1) {
			return null;
		}
		final Uri newUri = Uri.withAppendedPath(CONTENT_URI, newPathBase + String.valueOf(new_id));
		getContext().getContentResolver().notifyChange(newUri, null);
		return newUri;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		String table;
		switch(URI_MATCHER.match(uri)) {
		case SPECIFIC_SET:
			selection = PublicORMapper.KEY_DECOSET_ID + "=?";
			selectionArgs = new String[] { uri.getLastPathSegment() };
		case DECOSETS:
			table = TABLE_DECOSET;
			// Add to selection and selectionArgs to exclude the possibility
			// of the backgas decoset being updated
			selection = addToSelection(selection, PublicORMapper.KEY_DECOSET_ID + "<>?");
			selectionArgs = addToSelectionArgs(selectionArgs, String.valueOf(PublicORMapper.BACKGAS_DECOSET_ID));
			break;
		case SPECIFIC_ITEM:
			selection = PublicORMapper.KEY_DECOSETITEM_ID + "=?";
			selectionArgs = new String[] { uri.getLastPathSegment() };
		case SET_ITEMS:
			table = TABLE_DECOSETITEM;
			break;
		case SPECIFIC_DIVE:
			selection = PublicORMapper.KEY_DIVE_ID + "=?";
		case DIVES:
			table = TABLE_DIVE;
			break;
		case SPECIFIC_PROFILEITEM:
			selection = PublicORMapper.KEY_PROFILEITEM_ID + "=?";
		case DIVE_PROFILEITEMS:
			table = TABLE_PROFILEITEM;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		final int count = db.update(table, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table;
		final String idAsStringArray[] = new String[] { uri.getLastPathSegment() };
		final SQLiteDatabase db = mDbHelper.getWritableDatabase();
		switch(URI_MATCHER.match(uri)) {
		case SPECIFIC_SET:
			if(Long.valueOf(uri.getLastPathSegment()) == PublicORMapper.BACKGAS_DECOSET_ID) {
				// Cannot delete the backgas decoset
				return 0;
			}
			selection = PublicORMapper.KEY_DECOSET_ID + "=?";
			selectionArgs = idAsStringArray;
			// We first have to delete all items in this set
			db.delete(TABLE_DECOSETITEM, PublicORMapper.KEY_DECOSETITEM_DECOSET + "=?", idAsStringArray);
			// Move any dives using this decoset to the Backgas set
			final Uri diveUri = Uri.withAppendedPath(PublicORMapper.CONTENT_URI, "dives");
			ContentValues values = new ContentValues();
			values.put(PublicORMapper.KEY_DIVE_DECOSET, 1);
			getContext().getContentResolver().update(diveUri, values, PublicORMapper.KEY_DIVE_DECOSET + "=?", idAsStringArray);
		case DECOSETS:
			table = TABLE_DECOSET;
			// Add to selection and selectionArgs to exclude the possibility
			// of the back gas decoset being deleted
			selection = addToSelection(selection, PublicORMapper.KEY_DECOSET_ID + "<>?");
			selectionArgs = addToSelectionArgs(selectionArgs, String.valueOf(PublicORMapper.BACKGAS_DECOSET_ID));
			break;
		case SPECIFIC_ITEM:
			selection = PublicORMapper.KEY_DECOSETITEM_ID + "=?";
		case SET_ITEMS:
			table = TABLE_DECOSETITEM;
			break;
		case SPECIFIC_DIVE:
			selection = PublicORMapper.KEY_DIVE_ID + "=?";
			// We first have to delete all profile items in this dive
			db.delete(TABLE_PROFILEITEM, PublicORMapper.KEY_PROFILEITEM_DIVE + "=?", idAsStringArray);
		case DIVES:
			table = TABLE_DIVE;
			break;
		case SPECIFIC_PROFILEITEM:
			selection = PublicORMapper.KEY_PROFILEITEM_ID + "=?";
		case DIVE_PROFILEITEMS:
			table = TABLE_PROFILEITEM;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		final int rows = db.delete(table, selection, idAsStringArray);
		getContext().getContentResolver().notifyChange(uri, null);
		return rows;
	}
}