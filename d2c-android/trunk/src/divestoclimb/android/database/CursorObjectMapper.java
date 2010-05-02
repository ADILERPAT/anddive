package divestoclimb.android.database;

import android.database.Cursor;

public interface CursorObjectMapper<T> {
	/**
	 * Returns an object based on the data in the given Cursor at the
	 * current position.
	 * @param c The Cursor from which to retrieve an object, already
	 * advanced to the desired position
	 * @return The object
	 */
	public T getObjectFromCursor(Cursor c, boolean useFlyweight);
}