package divestoclimb.lib.scuba;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This is a class for managing "decosets" in a deco planner. A decoset is a collection of GasSources
 * and the depths at which each source should be switched to on ascent. 
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class Decoset {

	private Long id;
	protected String mName;
	protected SortedSet<Item> mItems = new TreeSet<Item>(mItemComparator);
	
	/*protected ItemFetcher mItemFetcher;
	protected Record.Updater mItemUpdater;*/

	public void setId(long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	public String getName() { return mName; }
	public Decoset setName(String name) { mName = name; return this; }
	public SortedSet<Item> getItems() { return mItems; }
	
	/*public Decoset setItemUpdater(Record.Updater u) { mItemUpdater = u; return this; }
	public Decoset setItemFetcher(ItemFetcher f) { mItemFetcher = f; return this; }*/

	/**
	 * An item in a Decoset. This stores a GasSource and the depth at which to switch to that source.
	 */
	public static class Item {
		
		private Long id;
		protected long mDecosetID;
		protected GasSource mGasSource;
		protected int mMaxDepth;

		public void setId(long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
		public long getDecosetID() { return mDecosetID; }
		public GasSource getGasSource() { return mGasSource; }
		public Item setGasSource(GasSource source) { mGasSource = source; return this; }
		public int getMaxDepth() { return mMaxDepth; }
		public Item setMaxDepth(int depth) { mMaxDepth = depth; return this; }

		/**
		 * This constructor is used for creating a new Item from scratch.
		 * @param decoset_id The ID of the Decoset this Item is part of
		 * @param max_depth The switch depth
		 * @param source The GasSource to switch to at the given depth
		 */
		public Item(long decoset_id, int max_depth, GasSource source) {
			//super();
			mDecosetID = decoset_id;
			mMaxDepth = max_depth;
			mGasSource = source;
		}

		/**
		 * This constructor is used for creating an instance of an Item from
		 * a database.
		 */
		public Item() { }
		
		/*public void reset(long id, long decoset_id, int max_depth, GasSource source) {
			super.reset(id);
			mDecosetID = decoset_id;
			mMaxDepth = max_depth;
			mGasSource = source;
		}*/

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
		mName = name;
	}

	/**
	 * This constructor is used for creating an instance of a Decoset from
	 * a database.
	 */
	public Decoset() { }

	/**
	 * Used for re-purposing a Decoset instance as a different Decoset, without having
	 * to waste objects.
	 * @param id The ID of this Decoset's database record
	 * @param name The name of this Decoset
	 */
	/*public void reset(long id, String name) {
		super.reset(id);
		mName = name;
		mItems.clear();
	} */

	/**
	 * Determine which gas to use at any depth according to this Decoset.
	 * @param depth The depth to get the desired gas source for
	 * @return The GasSource to use, or null if depth is deeper than any entry in the set
	 */
	public GasSource getGasSourceAtDepth(int depth) {
		/*if(mItems.isEmpty()) {
			mItems.addAll(mItemFetcher.lookupItems(this));
		}*/
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
		/*if(mItems.isEmpty()) {
			mItems.addAll(mItemFetcher.lookupItems(this));
		}*/
		final Iterator<Item> it = mItems.iterator();
		while(it.hasNext()) {
			final Item i = it.next();
			if(i.getMaxDepth() == depth) {
				if(source == null) {
					it.remove();
				} else {
					i.setGasSource(source);
				}
				return;
			}
		}
		// If we get here, an existing item at this depth wasn't found. Add one.
		if(source != null) {
			Item i = new Item(getId(), depth, source);
			//i.setUpdater(mItemUpdater);
			mItems.add(i);
		}
	}
	
	/*public static interface ItemFetcher {

		/**
		 * Builds a collection of all Items in this Decoset
		 * @return The Collection of Items
		 
		public Collection<Item> lookupItems(Decoset d);

	}*/
}