package divestoclimb.android.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;

public abstract class AbsSyncedPrefsHelper implements
		OnSharedPreferenceChangeListener {

	private Context mContext;
	private Uri[] mPeers;

	protected AbsSyncedPrefsHelper(Context context, Uri[] peers) {
		mContext = context;
		mPeers = peers;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Intent i = prepareIntent(sharedPreferences, key);
		if(i != null) {
			mContext.sendBroadcast(i);
		}
	}
	
	/**
	 * Prepare an Intent to be broadcast as a result of the given preference value changing.
	 * @param sharedPreferences The SharedPreferences object where the change occurred
	 * @param key The key that changed
	 * @return An intent to broadcast, or null if none should be sent.
	 */
	abstract protected Intent prepareIntent(SharedPreferences sharedPreferences, String key);
	
	/**
	 * Queries all my peer preference ContentProviders for one with a value for
	 * the given key
	 * @param key
	 * @return A Cursor that contains a value for the requested preference from a peer
	 * application, or null if none could be found.
	 */
	public Cursor findSetValue(String key) {
		for(int i = 0; i < mPeers.length; i ++) {
			final Uri p = mPeers[i];
			if(mContext.getPackageManager().resolveContentProvider(p.getAuthority(), 0) == null) {
				continue;
			}
			final Cursor c = mContext.getContentResolver().query(p, null, null, null, null);
			if(c == null) {
				continue;
			}
			c.moveToFirst();
			final int index = c.getColumnIndex(key);
			if(index == -1) {
				continue;
			}
			if(! c.isNull(index)) {
				return c;
			}
		}
		return null;
	}

}