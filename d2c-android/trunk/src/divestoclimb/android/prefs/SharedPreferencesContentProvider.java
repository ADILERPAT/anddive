package divestoclimb.android.prefs;

import java.util.Set;
import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;

public abstract class SharedPreferencesContentProvider extends ContentProvider {

	abstract protected SharedPreferences getSharedPreferences();

	private static class PreferencesCursor extends AbstractCursor implements SharedPreferences.OnSharedPreferenceChangeListener {

		private SharedPreferences preferences;
		private String[] prefKeys;

		public PreferencesCursor(SharedPreferences prefs) {
			preferences = prefs;
			prefs.registerOnSharedPreferenceChangeListener(this);

			Set<String> keys = preferences.getAll().keySet();
			prefKeys = (String[])keys.toArray(new String[keys.size()]);
			registerDataSetObserver(new DataSetObserver() {
				public void onChanged() {
					Set<String> keys = preferences.getAll().keySet();
					prefKeys = (String[])keys.toArray(new String[keys.size()]);
				}
			});
		}

		@Override
		public void finalize() {
			preferences.unregisterOnSharedPreferenceChangeListener(this);
			super.finalize();
		}

		@Override
		public String[] getColumnNames() {
			return prefKeys;
		}

		@Override
		public int getCount() {
			return 1;
		}

		@Override
		public double getDouble(int index) {
			return preferences.getFloat(prefKeys[index], -1);
		}

		@Override
		public float getFloat(int index) {
			return preferences.getFloat(prefKeys[index], -1);
		}

		@Override
		public int getInt(int index) {
			return preferences.getInt(prefKeys[index], -1);
		}

		@Override
		public long getLong(int index) {
			return preferences.getLong(prefKeys[index], -1);
		}

		@Override
		public short getShort(int index) {
			return (short) preferences.getInt(prefKeys[index], -1);
		}

		@Override
		public String getString(int index) {
			Object val = preferences.getAll().get(prefKeys[index]);
			return val == null? null: val.toString();
		}

		@Override
		public boolean isNull(int index) {
			return ! preferences.contains(prefKeys[index]);
		}

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			requery();
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor c = new PreferencesCursor(getSharedPreferences());
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SharedPreferences prefs = getSharedPreferences();
		Editor e = prefs.edit();
		for(Entry<String, Object> v : values.valueSet()) {
			Object value = v.getValue();
			if(value instanceof Float) {
				e.putFloat(v.getKey(), (Float)value);
			} else if(value instanceof Boolean) {
				e.putBoolean(v.getKey(), (Boolean)value);
			} else if(value instanceof Integer) {
				e.putInt(v.getKey(), (Integer)value);
			} else if(value instanceof Long) {
				e.putLong(v.getKey(), (Long)value);
			} else if(value instanceof String) {
				e.putString(v.getKey(), (String)value);
			}
		}
		e.commit();
		getContext().getContentResolver().notifyChange(uri, null);
		return 1;
	}
	
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
	}

}