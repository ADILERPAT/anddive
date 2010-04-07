package divestoclimb.lib.scuba;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import divestoclimb.lib.data.Record;

/**
 * This is a class for managing "decosets" in a deco planner. A decoset is a collection of GasSources
 * and the depths at which each source should be switched to on ascent. 
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class Decoset extends Record {

	protected String mName;
	protected SortedSet<Item> mItems;
	
	protected ItemFetcher mItemFetcher;
	protected Record.Updater mItemUpdater;

	public String getName() { return mName; }
	public Decoset setName(String name) { if(name != mName) { mName = name; mDirty = true; } return this; }
	
	public Decoset setItemUpdater(Record.Updater u) { mItemUpdater = u; return this; }
	public Decoset setItemFetcher(ItemFetcher f) { mItemFetcher = f; return this; }

	/**
	 * An item in a Decoset. This stores a GasSource and the depth at which to switch to that source.
	 */
	public static class Item extends Record {
		
		protected long mDecosetID;
		protected GasSource mGasSource;
		protected int mMaxDepth;

		public long getDecosetID() { return mDecosetID; }
		public GasSource getGasSource() { return mGasSource; }
		public void setGasSource(GasSource source) { if(! source.equals(mGasSource)) { mGasSource = source; mDirty = true; } }
		public int getMaxDepth() { return mMaxDepth; }
		public void setMaxDepth(int depth) { if(depth != mMaxDepth) { mMaxDepth = depth; mDirty = true; } }

		/**
		 * This constructor is used for creating a new Item from scratch.
		 * @param decoset_id The ID of the Decoset this Item is part of
		 * @param max_depth The switch depth
		 * @param source The GasSource to switch to at the given depth
		 */
		public Item(long decoset_id, int max_depth, GasSource source) {
			super();
			mDecosetID = decoset_id;
			mMaxDepth = max_depth;
			mGasSource = source;
		}

		/**
		 * This constructor is used for creating an instance of an Item from
		 * a database.
		 * @param id The ID of this Item's database record
		 * @param decoset_id The ID of the Decoset this Item is part of
		 * @param max_depth The switch depth
		 * @param source The GasSource to switch to at the given depth
		 */
		public Item(long id, long decoset_id, int max_depth, GasSource source) {
			super(id);
			reset(id, decoset_id, max_depth, source);
		}
		
		public void reset(long id, long decoset_id, int max_depth, GasSource source) {
			mID = id;
			mDecosetID = decoset_id;
			mMaxDepth = max_depth;
			mGasSource = source;
			mDirty = false;
		}

	}

	/**
	 * A Comparator for a Decoset. Organizes Items in descending order by depth.
	 */
	private static final Comparator<Item> mItemComparator = new Comparator<Item>() {
		/**
		 * Returns a negative value if i1's max depth is greater than i2's max depth,
		 * positive in the opposite case, and 0 if the two max depths are equal (BAD)
		 */
		@Override
		public int compare(Item i1, Item i2) {
			return i2.getMaxDepth() - i1.getMaxDepth();
		}
	};

	/**
	 * This constructor is used for creating a new Decoset from scratch.
	 * @param name The name of this Decoset
	 */
	public Decoset(String name) {
		super();
		mName = name;
		mItems = new TreeSet<Item>(mItemComparator);
	}

	/**
	 * This constructor is used for creating an instance of a Decoset from
	 * a database.
	 * @param id The ID of this Decoset's database record
	 * @param name The name of this Decoset
	 */
	public Decoset(long id, String name) {
		super(id);
		mName = name;
		mItems = new TreeSet<Item>(mItemComparator);
	}

	/**
	 * Used for re-purposing a Decoset instance as a different Decoset, without having
	 * to waste objects.
	 * @param id The ID of this Decoset's database record
	 * @param name The name of this Decoset
	 */
	public void reset(long id, String name) {
		super.reset(id);
		mName = name;
		mItems.clear();
	}

	/**
	 * Determine which gas to use at any depth according to this Decoset.
	 * @param depth The depth to get the desired gas source for
	 * @return The GasSource to use, or null if depth is deeper than any entry in the set
	 */
	public GasSource getGasSourceAtDepth(int depth) {
		if(mItems.isEmpty()) {
			mItems.addAll(mItemFetcher.lookupItems(this));
		}
		final Iterator<Item> it = mItems.iterator();
		GasSource last_gas = null;
		while(it.hasNext()) {
			final Item i = it.next();
			if(i.getMaxDepth() > depth) {
				break;
			} else {
				last_gas = i.getGasSource();
			}
		}
		return last_gas;
	}

	/**
	 * Modify this Decoset's Items. Set a new GasSource to use at a given depth
	 * @param depth The switch depth for the gas
	 * @param source The GasSource to switch to (pass null to prevent a switch at this depth
	 * and remove any pre-existing entry)
	 */
	public void setGasSource(int depth, GasSource source) {
		if(mItems.isEmpty()) {
			mItems.addAll(mItemFetcher.lookupItems(this));
		}
		final Iterator<Item> it = mItems.iterator();
		while(it.hasNext()) {
			final Item i = it.next();
			if(i.getMaxDepth() == depth) {
				if(source == null) {
					i.delete();
					it.remove();
				} else {
					i.setGasSource(source);
					i.commit();
				}
				return;
			}
		}
		// If we get here, an existing item at this depth wasn't found. Add one.
		if(source != null) {
			Item i = new Item(getID(), depth, source);
			i.setUpdater(mItemUpdater);
			mItems.add(i);
		}
	}
	
	public static interface ItemFetcher {

		/**
		 * Builds a collection of all Items in this Decoset
		 * @return The Collection of Items
		 */
		public Collection<Item> lookupItems(Decoset d);

	}
}