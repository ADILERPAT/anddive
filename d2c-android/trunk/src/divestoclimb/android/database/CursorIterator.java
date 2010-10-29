package divestoclimb.android.database;

import java.util.Iterator;
import java.util.NoSuchElementException;

import android.database.Cursor;

public class CursorIterator<T> implements Iterator<T> {

	private CursorCollection<T> cursorColl;
	protected int position = -1;
	protected boolean removed = false;
	protected boolean useFlyweight;

	public CursorIterator(CursorCollection<T> c) {
		this(c, true);
	}

	public CursorIterator(CursorCollection<T> c, boolean useFlyweight) {
		cursorColl = c;
		this.useFlyweight = useFlyweight;
	}

	@Override
	public boolean hasNext() {
		return position < cursorColl.cursor.getCount() - 1;
	}

	@Override
	public T next() {
		Cursor c = cursorColl.getCursor();
		if(position + 1 == c.getCount()) {
			throw new NoSuchElementException();
		}
		c.moveToPosition(removed? position: ++position);
		removed = false;
		return cursorColl.mapper.fetch(c, useFlyweight);
	}

	@Override
	public void remove() {
		if(removed) {
			throw new IllegalStateException("remove can only be called once before calling next()");
		}
		Cursor c = cursorColl.cursor;
		c.moveToPosition(position);
		cursorColl.binder.unbind(cursorColl, cursorColl.mapper.fetch(c, useFlyweight));
		c.requery();
		removed = true;
	}
}