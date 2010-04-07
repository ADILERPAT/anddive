package divestoclimb.scuba.dive.data.android;

import java.util.Date;

import divestoclimb.lib.data.Category;
import divestoclimb.lib.data.Record;
import divestoclimb.lib.scuba.Mission;

public class MetaMission extends Mission {

	protected long mCategoryID;
	protected Category mCategory;	// The category of this template
	protected Date mLastUpdate;
	protected Record.Fetcher<Category> mCategoryFetcher;
	
	public MetaMission(String name) {
		super(name);
	}

	public MetaMission(long id, long category, String name, Date lastUpdate) {
		super(id, name);
		mCategoryID = category;
		mLastUpdate = lastUpdate;
	}
	
	public long getCategoryID() { return mCategoryID; }
	
	public Date getLastUpdate() { return mLastUpdate; }

	public Mission setCategory(Category cat) {
		if(cat.getID() != mCategoryID) {
			mCategory = cat;
			mCategoryID = cat.getID();
			mDirty = true;
		}
		return this;
	}
	
	public Mission setCategoryFetcher(Record.Fetcher<Category> f) {
		mCategoryFetcher = f;
		return this;
	}
	
	public Category getCategory() {
		if(mCategory == null && mCategoryFetcher != null) {
			mCategory = mCategoryFetcher.fetch(mCategoryID);
		}
		return mCategory;
	}
	
	public void reset(long id, long category, String name, Date lastUpdate) {
		super.reset(id, name);
		if(mCategoryID != category) {
			mCategory = null;
		}
		mCategoryID = category;
		mLastUpdate = lastUpdate;
	}
}