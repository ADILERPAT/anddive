package divestoclimb.android.database;

import android.database.Cursor;

/**
 * Abstract class that constitutes a database key definition (something that
 * makes each record unique).
 * @author benr
 */
public abstract class Key {

	/**
	 * Determine if the given object has a key defined
	 * @param o The object to check for a key
	 * @return True if the object has a key, false otherwise
	 */
	public boolean isDefined(Object o) {
		return getValue(o) != null;
	}
	
	/**
	 * Determine if the given field name matches a portion of the key.
	 * @param fieldName The field name to check
	 * @return True if the field is part of the key, false otherwise.
	 */
	abstract public boolean isPart(String fieldName);
	
	/**
	 * Retrieve the value of the key. The object returned should be
	 * immutable, and properly implement equals() and hashCode(). 
	 * @param o The object to get the key for
	 * @return The key's value, or null if the object has no key defined
	 */
	abstract public Object getValue(Object o);
	
	/**
	 * Retrieve the value of the key in the given cursor, before the
	 * data has been mapped into an object. This is used by ORMapper
	 * when determining if the current object is already cached.
	 * In order for this method to be implemented correctly, the same
	 * value should be returned by this method and getValue(Object o)
	 * after the data in this cursor is mapped.
	 * @param c The cursor at the correct position to read from
	 * @param orMapper The ORMapper making the request (needed to map
	 * field names to column names from the cursor)
	 * @return The key's value
	 */
	abstract public Object getValue(Cursor c, ORMapper<?> orMapper);
}