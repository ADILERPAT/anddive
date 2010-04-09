package divestoclimb.util.android;

import java.util.Stack;

import android.database.Cursor;
import divestoclimb.lib.data.BaseRecord;
import divestoclimb.lib.data.Record;

/**
 * A Cursor wrapper which manipulates orderable records' internal order fields
 * to enforce an arbitrary order.
 * The idea here is to reduce the need for reindexing the entire list in the
 * database any time the user inserts a new record. If the database were using the
 * Cursor position to determine order, we'd have to reindex the list on every insert.
 * Instead, we use a separate field which by default gets a large increment, then
 * whenever a new item needs to be inserted between two other items we bisect the
 * gap in order between the two, and no existing records have to be updated. This
 * way reindexes only happen on collisions, when more than about
 * log2(STD_ORDER_INCREMENT) inserts happen in the same area.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class CursorOrderer<E extends Record & BaseRecord.Orderable<?>> {

	// The number to increment successive orders when appending to and reindexing the list.
	public static final int STD_ORDER_INCREMENT = 16;
	
	public static interface ObjectMapper<O> {
		/**
		 * Returns an Orderable object based on the data in the given Cursor.
		 * It is safe (in fact desired) to use a global flyweight object for
		 * this instead of constructing a new instance on every call.
		 * @param c The Cursor for which to retrieve an Orderable, already
		 * advanced to the desired position
		 * @return The Orderable
		 */
		public O getFromCursor(Cursor c);
	}

	private Cursor mCursor;
	private ObjectMapper<E> mObjectMapper;
	
	/**
	 * Constructor
	 * @param c The Cursor to reorder
	 * @param om An ObjectMapper implementation which can fetch the Orderable
	 * that corresponds to a Cursor row.
	 */
	public CursorOrderer(Cursor c, ObjectMapper<E> om) {
		mCursor = c;
		mObjectMapper = om;
	}
	
	public Cursor getCursor() { return mCursor; }
	public void setCursor(Cursor c) { mCursor = c; }
	
	/**
	 * Add Orderable i to the end of the list. If it already is in the list, it is
	 * moved to the end.
	 * @param i The Orderable to add or move
	 */
	public void add(E i) {
		add(i, mCursor.getCount());
	}
	
	/**
	 * Make Orderable i have position position in the Cursor. If i is already in
	 * this list, it is moved; otherwise it is added and the list length increases
	 * by one.
	 * @param i The Orderable to add or move
	 * @param position The new position for the Orderable
	 */
	public boolean add(E i, int position) {
		try {
			Cursor c = mCursor;
			ObjectMapper<E> om = mObjectMapper;
			int order_prev, order;
			c.moveToPosition(position);

			// First we detect what the order of the item before us is.
			// If this is the beginning of the list, pretend there's an item with
			// order 0 before us. The cursor is already at the correct position to
			// detect the item after us.
			if(position == 0) {
				order_prev = 0;
			} else {
				// If this Item isn't already in the list, its new neighbors are currently at position - 1 and 
				// position. However, if this item is already in the list and listed earlier than position,
				// it's effectively getting removed from the list and reinserted so although its neighbors will
				// end up at position - 1 and position, they are currently at position and position + 1. We take
				// that into account here by choosing where we start the operation.
				if(i.getOrder() == -1 || i.getOrder() > om.getFromCursor(c).getOrder()) {
					c.moveToPrevious();
				}
				order_prev = om.getFromCursor(c).getOrder();
				// Now we move the cursor to the next position so we can detect the order
				// of the next item.
				c.moveToNext();
			}

			if(c.getPosition() == c.getCount()) {
				// Trivial case. We're at the end of the list.
				if(! i.setOrder(order_prev + STD_ORDER_INCREMENT).commit()) {
					throw new Exception();
				}
			} else {
				// Now we have to detect the order of the item after us.
				int order_next = om.getFromCursor(c).getOrder();

				// Now see if we can find an order in between order_last and order_next
				order = (order_next + order_prev) / 2;

				if(order_next == i.getOrder()) {
					// We've been asked to add an item that already exists in this position.
					// Therefore we have nothing to do.
					return true;
				} else if(order < order_next && order > order_prev) {
					// Normal scenario. Move the item.
					if(! i.setOrder(order).commit()) {
						throw new Exception();
					}
				} else {
					// Collision, too many inserts. We have to reindex the rest of the
					// list.
				
					// Start with what i's new order will be.
					int new_order = order_prev + STD_ORDER_INCREMENT;

					if(! i.setOrder(new_order).commit()) {
						throw new Exception();
					}

					// We have to update all Items at once or things could get screwed
					// up. So keep an array of all Items as we reindex, then save them
					// all once we get the orders right.
					Stack<E> s = new Stack<E>();
					s.push(i);
					do {
						// Retrieve the Item at this index
						E it = om.getFromCursor(c);
						// If we encounter the original item we're "adding" it's because
						// this item is really being moved. Skip it for now.
						if(it.equals(i)) { continue; }

						new_order += STD_ORDER_INCREMENT;

						if(it.getOrder() >= new_order) {
							// This indicates the rest of the list is fine. There's
							// no need to continue reindexing.
							break;
						}

						// Update the order of this item
						it.setOrder(new_order);
						s.push(it);
					} while(c.moveToNext());

					// Now save everything we changed
					do {
						if(! s.pop().commit()) {
							throw new Exception();
						}
					} while(! s.empty());
				}
			}
			// Re-query the cursor to update the list.
			c.requery();
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	/**
	 * Insert an Orderable above another item in the list with the
	 * given ID. If no Orderable in the list exists with the given
	 * ID, the Orderable is inserted at the end.
	 * @param i The Item itself
	 * @param id The ID above which to place the Item
	 */
	public void insertAbove(E i, long id) {
		Cursor c = mCursor;
		ObjectMapper<E> om = mObjectMapper;
		boolean found = false;
		E item = null;
		if(c.getCount() > 0) {
			c.moveToFirst();
			do {
				item = om.getFromCursor(c);
				if(item.getId() == id) {
					found = true;
					break;
				}
			} while(c.moveToNext());
		}

		if(found) {
			add(i, c.getPosition());
		} else {
			add(i);
		}
	}
}