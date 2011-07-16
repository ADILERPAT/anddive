package divestoclimb.android.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

public abstract class ORMapper<T> extends AbsMapper<T> {

	public static int FLAG_ENABLE_CACHE = 1;

	// TODO:
	// - for performance, cache some critical methods like the primary key getter to reduce
	//   O(n) operation of basic things like objectsEqual to O(1)

	protected Key key = null;
	private int flags;
	private T flyweight = null;
	
	private HashMap<Object, T> cache = new HashMap<Object, T>();

	protected ORMapper(Class<T> clazz) {
		this(clazz, 0);
	}
	
	protected ORMapper(Class<T> clazz, int flags) {
		super(clazz);
		this.flags = flags;
		ignoreField("Class");
	}
	
	public void open() { }
	public void close() { }

	/**
	 * If this class contains a primary key, call this from the
	 * subclass with the field name. You do not need to do this
	 * if a superclass is responsible for storing the key.
	 * @param primaryKey The field name of the primary key
	 */
	protected void setPrimaryKey(String primaryKey) {
		// Find the primary key's class
		Method setter = getSetters(clazz).get(primaryKey);
		this.key = new PrimaryKey(primaryKey, setter.getParameterTypes()[0]);
	}
	
	protected void setKey(Key key) {
		this.key = key;
	}
	
	protected Map<Object, T> getCache() {
		return cache;
	}
	
	/**
	 * Performs a basic query. Useful for grouping as much query
	 * logic as possible together so it is easy to override in
	 * subclasses.
	 * @param projection
	 * @param selection
	 * @param selectionArgs
	 * @param sortOrder
	 * @return The Cursor containing the results of the query
	 */
	abstract protected Cursor doQuery(String[] projection, String selection, String[] selectionArgs, String sortOrder);
	
	/**
	 * Maps data from the given Cursor at the current position
	 * into an object. If this mapper is configured for caching,
	 * entities returned by this method will be cached and reused
	 * in future requests.
	 * @param c The Cursor to map
	 * @return The corresponding entity, or null if no data could be
	 * read from the cursor.
	 */
	public T fetch(Cursor c) {
		if(key != null && (flags & FLAG_ENABLE_CACHE) > 0) {
			// Check to see if the data is already in the cache.
			// If so, return the cached instance
			Object v = key.getValue(c, this);
			if(cache.containsKey(v)) {
				return cache.get(v);
			}
		}
		T entity = fetch(c, false);
		if(entity != null & key != null && (flags & FLAG_ENABLE_CACHE) > 0) {
			cache.put(key.getValue(entity), entity);
		}
		return entity;
	}

	/**
	 * Maps data from the given Cursor at the current position
	 * into a new object, or a global flyweight object.
	 * @param c The Cursor to map
	 * @param useFlyweight If true, populates a global flyweight
	 * object and returns it instead of creating a new instance.
	 * @return The populated object, or null if no data could be
	 * read from the cursor.
	 */
	public T fetch(Cursor c, boolean useFlyweight) {
		T instance;
		if(useFlyweight) {
			if(flyweight == null) {
				flyweight = createObjectInstance();
			}
			instance = flyweight;
		} else if(c == null || c.isBeforeFirst() || c.isAfterLast()) {
			// A little optimization to avoid creating a new
			// object instance if it's not going to get used
			return null;
		} else {
			instance = createObjectInstance();
		}

		return fetch(c, instance);
	}
	
	private static Map<String, Method> getSetters(Class<?> clazz) {
		Method methods[] = clazz.getMethods();

		Map<String, Method> setters = new HashMap<String, Method>();
		for(int i = 0; i < methods.length; i ++) {
			if(Modifier.isStatic(methods[i].getModifiers()) || methods[i].getParameterTypes().length != 1)
				continue;
			final String name = methods[i].getName();
			if(name.startsWith("init") && name.length() > 4) {
				// Ensure the next character is uppercase
				char ch = name.charAt(4);
				if(ch < 'A' || ch > 'Z')
					continue;
				// Found an initializer. Unconditionally add it to the setters hash
				setters.put(name.substring(4), methods[i]);
			} else if(name.startsWith("set")) {
				// Ensure the next character is uppercase
				char ch = name.charAt(3);
				if(ch < 'A' || ch > 'Z')
					continue;
				// Found a setter. Add it to the setters hash only if an initializer
				// wasn't found.
				if(! setters.containsKey(name.substring(3)))
					setters.put(name.substring(3), methods[i]);
			}
		}
		return setters;
	}

	/**
	 * Maps data from the given Cursor at the current position into
	 * the given instance of the corresponding class. Uses reflection
	 * to find the class setters and initializers, and calls
	 * columnToField with each.
	 * If this class is a subclass, you may wish to override this
	 * method and call a separate ORMapper instance to map the
	 * superclass first, then call this method to finish the job. 
	 * @param c The Cursor to map
	 * @param instance An instance of the corresponding class 
	 * @return A reference to instance if data could be read from
	 * the Cursor, null otherwise.
	 */
	public T fetch(Cursor c, T instance) {
		if(c == null || c.isBeforeFirst() || c.isAfterLast()) {
			return null;
		}
		Map<String, Method> setters = getSetters(instance.getClass());

		for(String field : setters.keySet()) {
			if(isIgnored(field))
				continue;
			try {
				columnToField(c, instance, field, setters.get(field));
			} catch(IllegalArgumentException e) {
				continue;
			}
		}
		// Look for a reset method. If it exists, call it.
		resetDirty(instance);
		return instance;
	}
	
	/**
	 * Fetch a single, unique result from the given cursor,
	 * handling exceptional cases safely. The cursor will
	 * be closed after the result is fetched.
	 * @param c The cursor to retrieve an object from
	 * @return The object mapped from the cursor or null
	 */
	protected T fetchUniqueResult(Cursor c) {
		if(c != null) {
			if(c.getCount() > 0) {
				c.moveToFirst();
			}
			T obj = fetch(c);
			c.close();
			return obj;
		}
		return null;
	}
	
	/**
	 * Maps a class field name to a column name. In subclasses, override
	 * this method to handle exceptions for mapping between field and
	 * column names.
	 * @param fieldName The name of the class field to map
	 * @return The corresponding column name in the database
	 */
	String generateColumnName(String fieldName) {
		if(isMapped(fieldName)) {
			return getMapping(fieldName);
		}
		// Convert name to all lowercase, prepend _ before all uppercase letters
		String newName = Character.toString(fieldName.charAt(0)).toLowerCase() + fieldName.substring(1);
		return newName;
	}
	
	protected Object getColumnValue(Cursor c, String fieldName, Class<?> clazz) throws UnsupportedOperationException {
		String colName = generateColumnName(fieldName);
		int colIndex = c.getColumnIndexOrThrow(colName);
		Object value;
		if(c.isNull(colIndex)) {
			value = null;
		} else if(String.class.isAssignableFrom(clazz)) {
			value = c.getString(colIndex);
		} else if(Integer.class.isAssignableFrom(clazz) || clazz == int.class) {
			value = c.getInt(colIndex);
		} else if(Long.class.isAssignableFrom(clazz) || clazz == long.class) {
			value = c.getLong(colIndex);
		} else if(Float.class.isAssignableFrom(clazz) || clazz == float.class) {
			value = c.getFloat(colIndex);
		} else if(Double.class.isAssignableFrom(clazz) || clazz == double.class) {
			value = c.getDouble(colIndex);
		} else if(Boolean.class.isAssignableFrom(clazz) || clazz == boolean.class) {
			value = c.getInt(colIndex) == 1;
		} else if(Date.class.isAssignableFrom(clazz)) {
			try {
				final String rawValue = c.getString(colIndex);
				value = rawValue == null? null: getDateFormat().parse(rawValue);
			} catch(ParseException e) {
				throw new IllegalArgumentException(e);
			}
		} else {
			throw new UnsupportedOperationException("Unhandled type " + clazz.getName() + " for field " + fieldName + ". Either ignore this field or override the getColumnValue method to support its type.");
		}
		return value;
	}

	/**
	 * Performs the mapping between columns and fields. Subclasses should override this
	 * method to map custom datatypes. This method will natively handle basic types such
	 * as Integer, Boolean, Double, etc.
	 * @param c The Cursor containing the column to map
	 * @param instance The instance of the corresponding class
	 * @param fieldName The field to map the column data to
	 * @param setter The method to call to get the field value 
	 * @throws UnsupportedOperationException When a field is passed which is not a basic
	 * type. Either ignore the field or add support for its type in a subclass override.
	 */
	protected void columnToField(Cursor c, T instance, String fieldName, Method setter) throws UnsupportedOperationException {
		Class<?> clazz = setter.getParameterTypes()[0];
		Object value = getColumnValue(c, fieldName, clazz);
		try {
			setter.invoke(instance, value);
		} catch(IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Looks for an optional "isDirty" method on the given object.
	 * @param o The object to check
	 * @return True if the object reports that it is dirty, false otherwise.
	 * Non-dirty objects will not be saved.
	 */
	public boolean isDirty(T o) {
		Method methods[] = o.getClass().getMethods();
		try {
			for(int i = 0; i < methods.length; i ++) {
				if(methods[i].getName().equals("isDirty")) {
					Boolean result = (Boolean)methods[i].invoke(o);
					return result.booleanValue();
				}
			}
		} catch(IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		// No isDirty method detected, so we must assume it's always dirty
		return true;
	}
	
	/**
	 * Looks for an optional "resetDirty" method on the object and invokes it
	 * if found. "isDirty" must also exist if "resetDirty" exists, and after this
	 * method is called, it must return false.
	 * @param o The object to reset
	 */
	protected void resetDirty(T o) {
		Method methods[] = o.getClass().getMethods();
		try {
			for(int i = 0; i < methods.length; i ++) {
				if(methods[i].getName().equals("resetDirty")) {
					methods[i].invoke(o);
				}
			}
		} catch(IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Determines if the given object is a "phantom", i.e. it has not yet
	 * been saved. If this class is a subclass, you will want to override
	 * this method and call it on an instance configured for the
	 * superclass.
	 * @param o The object to check
	 * @return True if the object appears to be a phantom, false otherwise.
	 */
	public boolean isPhantom(T o) {
		// Currently only records containing a primary key are supported. This will be
		// extended in the future.
		if(key == null) {
			throw new UnsupportedOperationException("Key not defined, can't check phantom status");
		}
		return ! key.isDefined(o);
	}

	/**
	 * Does a comparison between two entities to see if they represent
	 * the same backing data. This is done by comparing primary keys.
	 * @param o1 An object to compare.
	 * @param o2 An object to compare.
	 * @return True if the objects are referring to the same persisted
	 * data (the data in the entities may not be the same, but attempting
	 * to save both would overwrite each other's data); false otherwise
	 * @throws UnsupportedOperationException If the entity type does not
	 * have a primary key defined.
	 */
	public boolean objectsEqual(T o1, T o2) throws UnsupportedOperationException {
		if(o1 == null) {
			return o1 == o2;
		}
		if(o1.equals(o2)) {
			return true;
		}
		if(o1.getClass() != o2.getClass()) {
			return false;
		}
		if(key == null) {
			throw new UnsupportedOperationException("Key not defined, can't check equality");
		}
		Object v1 = key.getValue(o1), v2 = key.getValue(o2);
		return v1 == v2 || v1 != null && v1.equals(v2);
	}
	
	/**
	 * Saves the given object to the database, creating it if necessary.
	 * @param o The object to save
	 * @return true if the object is properly saved, false otherwise.
	 */
	public boolean save(T o) {
		if(! isDirty(o)) {
			return true;
		}
		ContentValues values = new ContentValues();
		boolean phantom = isPhantom(o);
		flatten(o, values, phantom);
		try {
			boolean result = phantom? doCreate(o, values): doUpdate(o, values);
			if(result) {
				resetDirty(o);
			}
			return result;
		} catch(SQLiteConstraintException e) {
			return false;
		}
	}
	
	/**
	 * Handles mapping all fields of the object into a ContentValues
	 * object. If this class is a subclass, you will want to override
	 * this method to also map superclass fields using a separate instance.
	 * @param o The object to map
	 * @param values The ContentValues object to map all data to
	 * @param phantom Whether or not the object being mapped is a phantom
	 */
	protected void flatten(T o, ContentValues values, boolean phantom) {
		Method methods[] = o.getClass().getMethods();

		for(int i = 0; i < methods.length; i ++) {
			String name = methods[i].getName();
			if(Modifier.isStatic(methods[i].getModifiers())) {
				continue;
			}
			if(! name.startsWith("get") || methods[i].getParameterTypes().length != 0)
				continue;
			if(isIgnored(name.substring(3)))
				continue;
			if(! phantom && key != null && key.isPart(name.substring(3)))
				continue;
			Object value;
			try {
				value = methods[i].invoke(o);
			} catch(IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch(IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch(InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			fieldToColumn(methods[i], value, o, values);
		}
	}
	
	/**
	 * Map the given field of the given object to the given ContentValues object.
	 * Override this method in subclasses to define custom mapping logic.
	 * @param field The field to map
	 * @param o The object containing the data to map
	 * @param values The ContentValues object to map the field's data to
	 * @throws UnsupportedOperationException If the field is of an unsupported type.
	 */
	protected void fieldToColumn(Method getter, Object value, T o, ContentValues values) throws UnsupportedOperationException {
		final String colName = generateColumnName(getter.getName().substring(3));
		if(value == null) {
			values.putNull(colName);
			return;
		}
		final Class<?> clazz = value.getClass();
		if(String.class.isAssignableFrom(clazz)) {
			values.put(colName, (String)value);
		} else if(Integer.class.isAssignableFrom(clazz) || clazz == int.class) {
			values.put(colName, (Integer)value);
		} else if(Long.class.isAssignableFrom(clazz) || clazz == long.class) {
			values.put(colName, (Long)value);
		} else if(Float.class.isAssignableFrom(clazz) || clazz == float.class) {
			values.put(colName, (Float)value);
		} else if(Double.class.isAssignableFrom(clazz) || clazz == double.class) {
			values.put(colName, (Double)value);
		} else if(Boolean.class.isAssignableFrom(clazz) || clazz == boolean.class) {
			values.put(colName, (Boolean)value);
		} else if(Date.class.isAssignableFrom(clazz)) {
			values.put(colName, getDateFormat().format((Date)value));
		} else {
			throw new UnsupportedOperationException("Unhandled type " + clazz.getName() + " for field " + getter.getName().substring(3) + ". Either ignore this field or override the columnToField method to support its type.");
		}
	}
	
	/**
	 * Subclasses must implement this method to handle creating new
	 * records in the database. After the record is created, update
	 * any autogenerated fields on the object.
	 * @param o The object that was mapped
	 * @param values The mapped data from the object that needs to be
	 * saved
	 * @return True if the operation succeeded, false otherwise
	 */
	abstract protected boolean doCreate(T o, ContentValues values);
	
	/**
	 * Subclasses must implement this method to handle updating
	 * records in the database.
	 * @param o The object that was mapped
	 * @param values The mapped data from the object that needs to be
	 * saved
	 * @return True if the operation succeeded, false otherwise
	 */
	abstract protected boolean doUpdate(T o, ContentValues values);
	
	/**
	 * Delete an entity from the backing storage
	 * @param o The entity to delete
	 * @return True if the operation succeeded or the entity hasn't
	 * been saved, false otherwise
	 */
	public boolean delete(T o) {
		if(! isPhantom(o)) {
			return doDelete(o);
		} else {
			return true;
		}
	}

	/**
	 * Subclasses must implement this method to handle deleting
	 * records from the database.
	 * @param o The object to delete
	 * @return True if the operation succeeded, false otherwise
	 */
	abstract protected boolean doDelete(T o);
}