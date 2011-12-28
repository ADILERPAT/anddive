package divestoclimb.lib.data;

public class Category {
	private Long id;
	protected String mName;	// The name of this category
	protected boolean mDirty = false;
	
	// Getters and setters
	public Long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return mName;
	}
	public void setName(String name) {
		if(! mName.equals(name)) {
			mName = name;
			mDirty = true;
		}
	}
	
	public Category() {
	}
	
	public boolean isDirty() {
		return mDirty;
	}
	
	public void resetDirty() {
		mDirty = false;
	}
}