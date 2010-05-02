package divestoclimb.android.database;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import android.database.Cursor;
import divestoclimb.lib.data.BaseRecord;

/**
 * An implementation of a List backed by an Android Cursor. It knows how to set
 * arbitrary order of Orderable elements and does so in a way that minimzes the
 * need for reindexing.
 * 
 * This implementation is not fully compatible with Java's Collections framework.
 * subList does not work.
 * @author Ben Roberts (divestoclimb@gmail.com)
 * 
 * @param <E> The type of BaseRecord included in the List. The elements must also
 * be of a class that implements Orderable.
 */
public class CursorList<E extends BaseRecord & BaseRecord.Orderable<?>> extends CursorCollection<E> implements List<E> {

	// The number to increment successive orders when appending to and reindexing the list.
	public static final int STD_ORDER_INCREMENT = 16;

	/**
	 * Constructor
	 * @param c The Cursor to reorder
	 * @param om An ObjectMapper implementation which can fetch the Orderable
	 * that corresponds to a Cursor row.
	 */
	public CursorList(Cursor c, Object[] params, CursorObjectMapper<E> om, Binder<E> binder) {
		super(c, params, om, binder);
	}
	
	@Override
	public boolean add(E i) {
		binder.bind(this, i);
		i.setOrder(get(cursor.getCount() - 1).getOrder() + STD_ORDER_INCREMENT).commit();
		cursor.requery();
		return true;
	}

	@Override
	public void add(int index, E i) {
		// The idea here is to eliminate the need for reindexing the entire list in the
		// database any time the user inserts a new record. If the database were using the
		// Cursor position to determine order, we'd have to reindex the list on every insert.
		// Instead, we use a separate field which by default gets a large increment, then
		// whenever a new item needs to be inserted between two other items we bisect the
		// gap in order between the two, and no existing records have to be updated. This
		// way reindexes only happen on collisions, when more than about
		// log2(STD_ORDER_INCREMENT) inserts happen in the same area.
		try {
			Cursor c = cursor;
			CursorObjectMapper<E> om = mapper;
			binder.bind(this, i);
			int order_prev, order;
			c.moveToPosition(index);

			// First we detect what the order of the item before us is.
			// If this is the beginning of the list, pretend there's an item with
			// order 0 before us. The cursor is already at the correct position to
			// detect the item after us.
			if(index == 0) {
				order_prev = 0;
			} else {
				// If this Item isn't already in the list, its new neighbors are currently at position - 1 and 
				// position. However, if this item is already in the list and listed earlier than position,
				// it's effectively getting removed from the list and reinserted so although its neighbors will
				// end up at position - 1 and position, they are currently at position and position + 1. We take
				// that into account here by choosing where we start the operation.
				if(i.getOrder() == -1 || i.getOrder() > om.getObjectFromCursor(c, true).getOrder()) {
					c.moveToPrevious();
				}
				order_prev = om.getObjectFromCursor(c, true).getOrder();
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
				int order_next = om.getObjectFromCursor(c, true).getOrder();

				// Now see if we can find an order in between order_last and order_next
				order = (order_next + order_prev) / 2;

				if(order_next == i.getOrder()) {
					// We've been asked to add an item that already exists in this position.
					// Therefore we have nothing to do.
					return;
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
						E it = om.getObjectFromCursor(c, false);
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
			return;
		} catch(Exception e) {
			return;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		for(E i : c) {
			add(index++, i);
		}
		return true;
	}

	@Override
	public E get(int index) {
		if(index < 0 || index >= cursor.getCount()) {
			throw new IndexOutOfBoundsException();
		}
		cursor.moveToPosition(index);
		return mapper.getObjectFromCursor(cursor, false);
	}

	@Override
	public int indexOf(Object o) {
		int i = 0;
		for(cursor.moveToFirst(); ! cursor.isAfterLast(); cursor.moveToNext()) {
			E item = mapper.getObjectFromCursor(cursor, false);
			if(item.equals(o)) {
				return i;
			}
			i ++;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		return indexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return new CursorListIterator<E>(this);
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new CursorListIterator<E>(this, index);
	}

	@Override
	public E remove(int index) {
		cursor.moveToPosition(index);
		final E obj = mapper.getObjectFromCursor(cursor, false);
		if(this.binder.unbind(this, obj) && obj.commit()) {
			cursor.requery();
		}
		return obj;
	}

	@Override
	public E set(int index, E element) {
		if(index < 0 || index >= cursor.getCount()) {
			throw new IndexOutOfBoundsException();
		}
		E currentElement = get(index);
		binder.bind(this, element);
		element.setOrder(currentElement.getOrder());
		binder.unbind(this, currentElement);
		element.commit();
		cursor.requery();
		return currentElement;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		// impossible to implement against a Cursor. The child
		// list would have to be backed by the same Cursor, but
		// it would have no way of knowing when underlying data
		// had changed through some other process.
		return null;
	}
}