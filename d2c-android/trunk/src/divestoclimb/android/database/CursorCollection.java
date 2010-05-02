package divestoclimb.android.database;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

import android.database.Cursor;

import divestoclimb.lib.data.BaseRecord;

/**
 * A Collection implementation backed by an Android Cursor. A CursorObjectMapper
 * is used to map the Collection's elements from Cursor records. Since the elements'
 * properties in the database determine whether or not they are part of this
 * collection (i.e. if they are returned in a query or not), a Binder is required to
 * change an element to add or remove it from the collection.
 * 
 * This is abstract because a Cursor cannot contain duplicate elements; it would violate
 * the database's referential integrity. CursorSet is an instantiatable implementation of
 * this.
 * @author Ben Roberts (divestoclimb@gmail.com)
 *
 * @param <E> The type of BaseRecord that's included in this Collection
 */
public abstract class CursorCollection<E extends BaseRecord> implements Collection<E> {
	
	protected Cursor cursor;
	protected CursorObjectMapper<E> mapper;
	protected Object[] params;
	protected Binder<E> binder;
	
	public CursorCollection(Cursor c, Object[] params, CursorObjectMapper<E> mapper, Binder<E> binder) {
		cursor = c;
		this.mapper = mapper;
		this.params = params;
		this.binder = binder;
	}
	
	public Object[] getParams() {
		return params;
	}
	
	public Cursor getCursor() {
		return cursor;
	}
	
	/**
	 * A Binder handles database-specific tasks for a CursorCollection,
	 * specifically how to associate a record with a given Collection.
	 * Usually this is done by setting or unsetting one or more foreign
	 * keys.
	 * @author Ben Roberts (divestoclimb@gmail.com)
	 *
	 * @param <T> The record type
	 */
	public static interface Binder<T extends BaseRecord> {
		/**
		 * Associate the given record with a CursorCollection.
		 * Call getParams() on the CursorCollection to get the
		 * values of any custom parameters needed.
		 * You do NOT need to call commit() on the record to commit
		 * the change. The CursorCollection will handle that when it's
		 * ready.
		 * @param c The CursorCollection to associate the record with
		 * @param obj The record to associate
		 * @return True if the operation succeeded, false otherwise
		 */
		public boolean bind(CursorCollection<T> c, T obj);

		/**
		 * Disassociate the given record from a CursorCollection.
		 * Depending on database structure, you may need to delete
		 * the record entirely or just remove an entry in an intersection
		 * table for a n-to-m relationship.
		 * Unlike bind() above, this method MUST commit its changes to
		 * the database.
		 * @param c The CursorCollection to disassociate the record from
		 * @param obj The record to disassociate
		 * @return True if the operation succeeded, false otherwise
		 */
		public boolean unbind(CursorCollection<T> c, T obj);
		
		/**
		 * Disassociate all records in a CursorCollection. It's assumed this
		 * task can be implemented by going directly to the database, instead
		 * of having to loop through and delete individual objects.
		 * @param c The CursorCollection containing the records to disassociate
		 * @return True if the operation succeeded, false otherwise
		 */
		public boolean unbindAll(CursorCollection<T> c);
	}

	@Override
	public boolean add(E e) {
		if(contains(e)) {
			return false;
		} else {
			boolean result = binder.bind(this, e);
			e.commit();
			if(result) {
				cursor.requery();
			}
			return result;
		}
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean result = false;
		for(E i : c) {
			result |= add(i);
		}
		return true;
	}

	@Override
	public void clear() {
		this.binder.unbindAll(this);
	}

	@Override
	public boolean contains(Object o) {
		for(E i : this) {
			if(i.equals(o)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Is this the most efficient way?
	coll: for(Object o : c) {
			for(E i : this) {
				if(i.equals(o)) {
					continue coll;
				}
			}
			// If we get here, continue wasn't called indicating the current
			// item isn't in the list.
			return false;
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return cursor.getCount() == 0;
	}

	@Override
	public Iterator<E> iterator() {
		return new CursorIterator<E>(this);
	}

	@Override
	public boolean remove(Object o) {
		for(E i : this) {
			if(i.equals(o)) {
				boolean result = this.binder.unbind(this, i);
				if(result) {
					cursor.requery();
				}
				return result;
			}
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean result = false;
		for(E i : this) {
			if(c.contains(i)) {
				result |= this.binder.unbind(this, i);
			}
		}
		if(result) {
			cursor.requery();
		}
		return result;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean result = false;
		for(E i : this) {
			if(! c.contains(i)) {
				result |= this.binder.unbind(this, i);
			}
		}
		if(result) {
			cursor.requery();
		}
		return result;
	}

	@Override
	public int size() {
		return cursor.getCount();
	}

	@Override
	public Object[] toArray() {
		if(cursor.getCount() == 0) {
			return null;
		}
		Object result[] = new Object[cursor.getCount()];
		// An Iterator will use a flyweight object. We have to
		// iterate the cursor manually
		int i = 0;
		for(cursor.moveToFirst(); ! cursor.isAfterLast(); cursor.moveToNext()) {
			result[i ++] = mapper.getObjectFromCursor(cursor, false);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		Object[] result;
		if(a.length < cursor.getCount()) {
			// Not big enough. Return a new array
			result = (Object[])Array.newInstance(a.getClass(), cursor.getCount());
		} else {
			result = a;
		}
		int i = 0;
		for(cursor.moveToFirst(); ! cursor.isAfterLast(); cursor.moveToNext()) {
			result[i ++] = mapper.getObjectFromCursor(cursor, false);
		}
		if(a.length > cursor.getCount()) {
			result[i] = null;
		}
		return (T[])result;
	}
}