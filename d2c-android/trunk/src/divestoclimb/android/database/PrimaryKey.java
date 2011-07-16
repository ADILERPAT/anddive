package divestoclimb.android.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.database.Cursor;

public class PrimaryKey extends Key {

	private String fieldName;
	private Class<?> clazz;

	public PrimaryKey(String fieldName, Class<?> clazz) {
		this.fieldName = fieldName;
		this.clazz = clazz;
	}
	
	@Override
	public Object getValue(Object o) {
		Method methods[] = o.getClass().getMethods();
		try {
			for(int i = 0; i < methods.length; i ++) {
				if(methods[i].getName().equals("get" + fieldName) && methods[i].getParameterTypes().length == 0) {
					Object primaryKey = methods[i].invoke(o);
					return primaryKey;
				}
			}
		} catch(IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		throw new IllegalArgumentException("Primary key field " + fieldName + " not found on object of type " + o.getClass().getName());
	}
	
	@Override
	public Object getValue(Cursor c, ORMapper<?> orMapper) {
		return orMapper.getColumnValue(c, fieldName, clazz);
	}

	@Override
	public boolean isPart(String fieldName) {
		return this.fieldName.equals(fieldName);
	}
}