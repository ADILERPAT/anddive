package divestoclimb.lib.scuba;

import divestoclimb.lib.data.Record;

public class Mission extends Record {

	protected String mName;			// The name of this mission

	// Getters and Setters
	public String getName() { return mName; }
	public Mission setName(String name) { if(mName != name) { mName = name; mDirty = true; } return this; }
	
	// Constructor for creating a new Mission
	public Mission(String name) {
		super();
		mName = name;
	}
	
	public Mission(long id, String name) {
		super(id);
		mName = name;
	}

	public void reset(long id, String name) {
		super.reset(id);
		mName = name;
		mDirty = false;
	}
}