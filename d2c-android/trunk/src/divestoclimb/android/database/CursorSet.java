package divestoclimb.android.database;

import java.util.Set;

import android.database.Cursor;

import divestoclimb.lib.data.BaseRecord;

/**
 * An implementation of Set backed by an Android Cursor.
 * @author Ben Roberts (divestoclimb@gmail.com)
 *
 * @param <E> The type of BaseRecord that's included in this Set
 */
public class CursorSet<E extends BaseRecord> extends CursorCollection<E> implements Set<E> {

	public CursorSet(Cursor c, Object[] params, CursorObjectMapper<E> mapper, CursorCollection.Binder<E> binder) {
		super(c, params, mapper, binder);
	}
}