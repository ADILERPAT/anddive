package divestoclimb.lib.data;

/**
 * If a type of record needs to be kept in a specific order, have that
 * subclass implement Orderable.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public interface Orderable {
	/**
	 * Get the current value of the order field
	 * @return A number which, when sorted ascending across all Records in
	 * the set, would cause the set to be in the correct order.
	 */
	public int getOrder();
	
	/**
	 * Change the order field value
	 * @param order The new value of order. This method does not need to
	 * ensure that the value is unique across the record set; for such tools
	 * see CursorList (an Android implementation)
	 * @return this
	 */
	public void setOrder(int order);
}