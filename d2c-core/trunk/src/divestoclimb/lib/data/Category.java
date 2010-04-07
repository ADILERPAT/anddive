package divestoclimb.lib.data;

public class Category extends Record {
	protected String mName;	// The name of this category
	
	// Getters and setters
	public String getName() { return mName; }
	public Category setName(String name) { if(mName != name) { mName = name; mDirty = true; } return this; }
	
	// Constructor for creating a new category
	public Category(String name) {
		super();
		mName = name;
	}
	
	public Category(long id, String name) {
		super(id);
		mName = name;
	}
	
	public void reset(long id, String name) {
		super.reset(id);
		mName = name;
	}
}