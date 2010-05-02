package divestoclimb.android.database;

import java.util.Iterator;
import java.util.NoSuchElementException;

import android.database.Cursor;

import divestoclimb.lib.data.BaseRecord;

public class CursorIterator<T extends BaseRecord> implements Iterator<T> {

	private CursorCollection<T> cursorColl;
	protected int position = -1;
	protected boolean removed = false;

	public CursorIterator(CursorCollection<T> c) {
		cursorColl = c;
	}

	@Override
	public boolean hasNext() {
		return position < cursorColl.cursor.getCount();
	}

	@Override
	public T next() {
		Cursor c = cursorColl.getCursor();
		if(position + 1 == c.getCount()) {
			throw new NoSuchElementException();
		}
		c.moveToPosition(removed? position: ++position);
		removed = false;
		return cursorColl.mapper.getObjectFromCursor(c, true);
	}

	@Override
	public void remove() {
		if(removed) {
			throw new IllegalStateException("remove can only be called once before calling next()");
		}
		Cursor c = cursorColl.cursor;
		c.moveToPosition(position);
		cursorColl.binder.unbind(cursorColl, cursorColl.mapper.getObjectFromCursor(c, true));
		c.requery();
		removed = true;
	}
}