package divestoclimb.lib.data;

/**
 * A class that handles common logic for any type of data record
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public abstract class Record {

	/**
	 * A unique key that describes this record. It's assumed that the storage
	 * mechanism assigns these. If the record is a "phantom", i.e. hasn't
	 * actually been committed to storage somewhere, this will be set to NO_ID.
	 */
	protected long mID;
	
	public static final long NO_ID = -1;
	
	/**
	 * Tracks whether or not this record is "dirty", i.e. if any data has been
	 * changed in the object which has not been committed back to the database.
	 * Subclass setters are responsible for setting mDirty to true if they
	 * change any committable value.
	 */
	protected boolean mDirty;
	
	protected Updater mUpdater = null;
	
	/**
	 * If a type of Record needs to be kept in a specific order, have that
	 * subclass implement Orderable. The generic allows setOrder to return
	 * an instance of the class for which it's a method, so when implementing
	 * make sure you set E to your own class. e.g.
	 * public class Employee extends Record implements Orderable&lt;Employee&gt; { }
	 * @author Ben Roberts (divestoclimb@gmail.com)
	 */
	public static interface Orderable<E extends Record> {
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
	
	/**
	 * A constructor for building a new, empty Record instance. Use this when
	 * a new Record is being created that does not exist in backend storage yet.
	 */
	public Record() {
		mID = NO_ID;
		mDirty = true;
	}
	
	/**
	 * A constructor for building an instance that represents the data for a
	 * record in backend storage. Use this to build instances of pre-existing
	 * records for inspection or for making changes.
	 * @param id The unique key of this record as represented in backend storage.
	 */
	public Record(long id) {
		mID = id;
		mDirty = false;
	}

	/**
	 * Get the unique key for this record
	 * @return The key, or -1 if this record has not been assigned one.
	 */
	public long getID() { return mID; }
	
	public boolean isDirty() { return mDirty; }
	
	public boolean isPhantom() { return mID == NO_ID; }
	
	protected void reset(long id) {
		mID = id;
		mDirty = false;
	}
	
	// This might get me in trouble because it's possible for two Records of
	// the same subclass and ID to have different hash codes if they each
	// represent the same record being modified simultaneously by two different
	// operations.
	@Override
	public boolean equals(Object o2) {
		return (o2.getClass() == getClass()) && mID == ((Record)o2).getID();
	}
	
	/**
	 * An Updater can be assigned to a Record to make it possible to change the
	 * data in backend storage. Call setUpdater to set one.
	 * @param updater The Updater instance to use for this Record
	 */
	public Record setUpdater(Updater updater) {
		mUpdater = updater;
		return this;
	}
	
	public boolean delete() {
		if(mUpdater == null) {
			return false;
		}
		return mUpdater.doDelete(this);
	}

	/**
	 * Commit any pending changes to this record back to its storage location,
	 * creating the record there if it doesn't already exist.
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean commit() {
		if(! mDirty) {
			return true;
		}
		if(mUpdater == null) {
			return false;
		}
		if(mID == NO_ID) {
			mID = mUpdater.doCreate(this);
			return mID != NO_ID;
		} else {
			return mUpdater.doUpdate(this);
		}
	}
	
	/**
	 * Fetcher is an interface that allows looking up records that have a
	 * 1-to-many relationship.
	 * @author Ben Roberts (divestoclimb@gmail.com)
	 *
	 * @param <R> The type of Record this Fetcher is capable of retrieving
	 */
	public static interface Fetcher<R extends Record> {
		public R fetch(long id);
	}
	
	public static interface Updater {

		/**
		 * Commits the data being stored in this Record as a new record in the
		 * backend storage.
		 * @return The ID assigned by backend storage for this Record, or NO_ID
		 * if the create failed.
		 */
		public long doCreate(Record r);
	
		/**
		 * Commits the data being stored in this Record back to the original
		 * record in backend storage.
		 * @return true if the operation succeeds, false otherwise.
		 */
		public boolean doUpdate(Record r);

		/**
		 * Delete this record from the storage location
		 * @return true if the operation succeeds, false otherwise
		 */
		public boolean doDelete(Record r);
	}
}