package divestoclimb.android.database;

import java.util.Set;

import android.database.Cursor;

/**
 * An implementation of Set backed by an Android Cursor.
 * @author Ben Roberts (divestoclimb@gmail.com)
 *
 * @param <E> The type of BaseRecord that's included in this Set
 */
public class CursorSet<E> extends CursorCollection<E> implements Set<E> {

	public CursorSet(Cursor c, ORMapper<E> mapper) {
		super(c, mapper);
	}

	public CursorSet(Cursor c, ORMapper<E> mapper, CursorCollection.Binder<E> binder) {
		super(c, mapper, binder);
	}
	public CursorSet(Cursor c, ORMapper<E> mapper, CursorCollection.Binder<E> binder, boolean useFlyweightInIterator) {
		super(c, mapper, binder, useFlyweightInIterator);
	}
}