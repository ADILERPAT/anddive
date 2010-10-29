package divestoclimb.lib.data;

/**
 * A class that handles common logic for any type of data record
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
@Deprecated
public abstract class BaseRecord {

	public static final long NO_ID = -1;
	
	/**
	 * Tracks whether or not this record is "dirty", i.e. if any data has been
	 * changed in the object which has not been committed back to the database.
	 * Subclass setters are responsible for setting mDirty to true if they
	 * change any committable value.
	 */
	protected boolean mDirty;
	
	public boolean isDirty() { return mDirty; }
	
	/**
	 * If a type of Record needs to be kept in a specific order, have that
	 * subclass implement Orderable. The generic allows setOrder to return
	 * an instance of the class for which it's a method, so when implementing
	 * make sure you set E to your own class. e.g.
	 * public class Employee extends Record implements Orderable&lt;Employee&gt; { }
	 * @author Ben Roberts (divestoclimb@gmail.com)
	 */
	public static interface Orderable<E extends BaseRecord> {
		/**
		 * Get the current value of the order field
		 * @return A number which, when sorted ascending across all Records in
		 * the set, would cause the set to be in the correct order.
		 */
		public int getOrder();
		
		/**
		 * Change the order field value
		 * @param order The new value of order. This method does not need to
		 * ensure that the value is unique across the Record set; for such tools
		 * see CursorOrderer (an Android implementation)
		 * @return this
		 */
		public E setOrder(int order);	
	}
	
	public abstract boolean commit();
	
	public abstract boolean isPhantom();
}