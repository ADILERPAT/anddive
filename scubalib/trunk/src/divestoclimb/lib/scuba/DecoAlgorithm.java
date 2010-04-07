package divestoclimb.lib.scuba;

public interface DecoAlgorithm<E extends ProfileItem> {

	// TODO: at least some of this data should be encapsulated in a DecoSettings class
	// to make it easier to add to in the future without breaking legacy implementations
	public void setDecoset(Decoset set);

	/**
	 * Perform a run of the deco algorithm on the passed dive segment. Perform an
	 * ascent/descent at the configured rate to the desired depth, checking if
	 * decompression will be required before reaching it. The class's internal
	 * compartment state should be according to the end of the time passed.
	 * @param item The ProfileItem to process
	 * @return An empty array if the calculation went uneventfully. If mandatory
	 * stops are required before reaching the given depth, they are returned
	 * as ProfileItems.
	 */
	public E[] run(E item);

	/**
	 * perform a run of the deco algorithm to ascend to the surface, checking if
	 * decompression will be required before reaching it. The class's internal
	 * compartment state should be according to the moment the surface is reached.
	 * @return An empty array if the calculation went uneventfully. If mandatory
	 * stops are required before reaching the surface, they are returned as
	 * ProfileItems.
	 */
	public E[] surface();
	
	/**
	 * Perform a run of the deco algorithm for a diver out of the water. The diver
	 * is at a constant pressure for the given amount of time and breathing the given
	 * gas source. 
	 * @param pressure The ambient pressure of the diver, in whatever altitude is in use
	 * by the current unit system
	 * @param time The amount of time spent at pressure, in minutes
	 * @param source The GasSource being breathed during this time. Usually this will
	 * be air, but a diver could be breathing something else like oxygen to try to
	 * accelerate acclimatization.
	 */
	public void surfaceInterval(int altitude, int time, GasSource source);

	/**
	 * Get the runtime of the current dive so far, as of the end of the last run()
	 * @return The runtime
	 */
	public float getRuntime();
	
	/**
	 * Get the last gas source used when the algorithm was running. This is
	 * needed in case run() had to perform a gas switch on ascent
	 * @return
	 */
	public GasSource getGasSource();

	public byte[] getState();
	public void loadState(byte[] state);
	
	public void loadConfig(byte[] config);
}