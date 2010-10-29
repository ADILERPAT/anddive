package divestoclimb.android.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public abstract class AbsMapper<T> {

	private static final DateFormat DEFAULT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static {
		DEFAULT_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	private Set<String> ignoredFields = new HashSet<String>();
	private Map<String, String> fieldMappings = new HashMap<String, String>();
	private DateFormat dateFormat = DEFAULT_FORMAT;
	protected Class<T> clazz;
	
	/**
	 * Default constructor for subclasses
	 * @param clazz The class to map
	 */
	protected AbsMapper(Class<T> clazz) {
		this.clazz = clazz;
	}
	
	/**
	 * Creates a new instance of the given class.
	 * If this class is abstract, you will need
	 * to override this method.
	 * @return A new instance of the given class.
	 */
	protected T createObjectInstance() {
		try {
			return clazz.newInstance();
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InstantiationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	protected DateFormat getDateFormat() {
		return dateFormat;
	}
	
	/**
	 * A utility method that allows formatting dates for use in database queries.
	 * This is public so queries can be done that bypass the builtin query methods
	 * of the ORMapper.
	 * @param date The Date to format
	 */
	public String formatDate(Date date) {
		return dateFormat.format(date);
	}
	
	/**
	 * Ignores a field so it is not mapped
	 * @param fieldName The name of the field to ignore
	 */
	public void ignoreField(String fieldName) {
		ignoredFields.add(fieldName);
	}
	
	public void ignoreField(String fieldNames[]) {
		ignoredFields.addAll(Arrays.asList(fieldNames));
	}
	
	public void mapField(String fieldName, String mappedName) {
		fieldMappings.put(fieldName, mappedName);
	}
	
	protected boolean isIgnored(String fieldName) {
		return ignoredFields.contains(fieldName);
	}
	
	protected boolean isMapped(String fieldName) {
		return fieldMappings.containsKey(fieldName);
	}
	
	protected String getMapping(String fieldName) {
		return fieldMappings.get(fieldName);
	}
}