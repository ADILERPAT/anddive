package divestoclimb.lib.scuba;

public class Mission {

	private Long id;
	protected String mName;			// The name of this mission

	// Getters and Setters
	public void setId(long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	public String getName() { return mName; }
	public Mission setName(String name) { mName = name; return this; }
	
	// Constructor for creating a new Mission
	public Mission(String name) {
		mName = name;
	}
}