package divestoclimb.lib.scuba;

public class Localizer {

	// Named gases
	public static final int STRING_OXYGEN = 1;
	public static final int STRING_HELIUM = 2;
	public static final int STRING_NITROGEN = 3;
	public static final int STRING_AIR = 4;

	public interface Engine {
		public String getString(int resource);
	}

	private static Engine mEngine;
	
	public static Engine getEngine() { return mEngine; }
	public static void setEngine(Engine e) { mEngine = e; }
}