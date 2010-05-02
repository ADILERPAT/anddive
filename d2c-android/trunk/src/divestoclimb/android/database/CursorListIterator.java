package divestoclimb.android.database;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import android.database.Cursor;

import divestoclimb.lib.data.BaseRecord;

public class CursorListIterator<E extends BaseRecord & BaseRecord.Orderable<?>> extends CursorIterator<E>
		implements ListIterator<E> {

	private CursorList<E> cursorList;
	protected boolean added = false;

	public CursorListIterator(CursorList<E> l) {
		super(l);
		cursorList = l;
	}
	
	public CursorListIterator(CursorList<E> l, int start) {
		super(l);
		cursorList = l;
		position = start;
	}

	@Override
	public void add(E e) {
		cursorList.add(++ position, e);
		added = true;
	}

	@Override
	public boolean hasPrevious() {
		return position > -1;
	}
	
	@Override
	public E next() {
		E obj = super.next();
		added = false;
		return obj;
	}

	@Override
	public int nextIndex() {
		return position + 1;
	}

	@Override
	public E previous() {
		Cursor c = cursorList.getCursor();
		if(position < 0) {
			throw new NoSuchElementException();
		}
		c.moveToPosition(-- position);
		removed = false;
		return cursorList.mapper.getObjectFromCursor(c, true);
	}

	@Override
	public int previousIndex() {
		return position;
	}

	@Override
	public void set(E e) {
		remove();
		add(e);
		removed = false;
		added = false;
	}
}