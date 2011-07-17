package divestoclimb.lib.scuba;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ProfileIterator {

	// A ProfileItem with all inherited attributes filled in from scanning
	// the previous items, representing the current item
	private ProfileItem mCurrentLine;
	private int mPosition;
	protected Dive mDive;

	// The source to use for any changes
	private int mSource;

	public ProfileIterator(Dive dive, int source) {
		mPosition = 0;
		mCurrentLine = null;
		mDive = dive;
		mSource = source;
	}

	/**
	 * Get the current position within the profile
	 * @return The current zero-indexed position
	 */
	public int getPosition() {
		return mPosition;
	}

	/**
	 * Move the current profile position to an arbitrary position. The position
	 * will be at the next active line in the profile at or after position
	 * @param position The new zero-indexed position. 0 <= position < count
	 * @return true if the operation succeeded, false if position is invalid or
	 * a data error occurred
	 */
	public boolean moveToPosition(int position) {
		if(mPosition > position) {
			// We're moving backwards. There's no easy way to do this except
			// to start from the beginning again
			mPosition = -1;
		} else if(mPosition == position) {
			// Trivial case. We're already there!
			return true;
		}
		while(mPosition < position) {
			if(! moveToNext()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Moves the Profile position to the first active line
	 * @return true if the operation succeeded, false if there are no lines in
	 * this Profile or a data error occurred
	 */
	public boolean moveToFirst() {
		return moveToPosition(0);
	}

	/**
	 * Moves the Profile position to the next active line
	 * @return true if the operation succeeded, false if we're at the end or if
	 * a data error occurred
	 */
	public boolean moveToNext() {
		do {
			List<ProfileItem> profile = mDive.getProfile();
			// Fetch the next ProfileItem and merge its values with mPreviousState
			if(mPosition + 1 >= profile.size()) {
				// No more items
				return false;
			}
			final ProfileItem next = profile.get(++mPosition);
			if(mPosition == 0) {
				mCurrentLine = next;
			} else if(mCurrentLine == null) {
				// This is an error. mCurrentLine cannot be null if position is nonzero.
				return false;
			} else {
				mCurrentLine.merge(next);
			}
		} while(! mCurrentLine.isActive());
		// As a sanity check, make sure the state is not raw. It should never be
		// after a merge.
		return ! mCurrentLine.isRaw();
	}

	public int getSource() {
		return mSource;
	}

	public ProfileIterator setSource(int source) {
		mSource = source;
		return this;
	}

	protected ProfileItem getCurrentItem() {
		return mDive.getProfile().get(mPosition);
	}

	public boolean appendItem(ProfileItem newItem) {
		newItem.setLineSource(mSource);
		List<ProfileItem> profile = mDive.getProfile();
		profile.add(mPosition + 1, newItem);
		// Go through all items after this one and mark them invalid if they
		// can be marked as such
		ListIterator<ProfileItem> i = profile.listIterator(mPosition + 1);
		while(i.hasNext()) {
			ProfileItem item = i.next();
			if(item.getValid() != ProfileItem.ALWAYS_VALID) {
				item.setValid(ProfileItem.INVALID);
				//item.commit();
			}
		}
		return false;
	}
	
	public boolean replaceItem(ProfileItem newItem) {
		final ProfileItem current = mDive.getProfile().get(mPosition);
		if(mSource == current.getLineSource()) {
			// The profile line has the same source as the processor that's
			// currently editing. We can go ahead and replace it.
			newItem.setLineSource(mSource);
			current.merge(newItem);
			//current.commit();
		} else {
			// We can't overwrite the item. Instead, we deactivate it and
			// append a new one after it.
			current.setActive(false);
			//current.commit();
			appendItem(newItem);
		}
		return true;
	}
	
	/**
	 * Remove all profile items that match the current source.
	 */
	public void removeMyItems() {
		int p = 0;
		List<ProfileItem> profile = mDive.getProfile();
		for(Iterator<ProfileItem> it = profile.iterator(); it.hasNext(); ) {
			ProfileItem i = it.next();
			if(i.getLineSource() == mSource) {
				if(p > 0) {
					ProfileItem lastItem = profile.get(p - 1);
					if(i.isActive() && ! lastItem.isActive()) {
						lastItem.setActive(true);
						//lastItem.commit();
					}
				}
				it.remove();
			} else {
				p++;
			}
		}
	}
	
	/*
	 * TODO: move this to my DecoAlgorithm implementation
	private Mix mFlyMix = null;
	private Setpoint mFlySetpoint = null;

	private GasSource copyGasSourceToFlyweight(GasSource source) {
		Mix mixToCopy;
		if(source instanceof Mix) {
			mixToCopy = (Mix)source;
		} else {
			mixToCopy = ((Setpoint)source).getDiluent();
		}
		if(mFlyMix == null) {
			mFlyMix = new Mix(mixToCopy.getfO2(), mixToCopy.getfHe());
		} else {
			mFlyMix.setfO2(mixToCopy.getfO2());
			mFlyMix.setfHe(mixToCopy.getfHe());
		}
		if(source instanceof Setpoint) {
			if(mFlySetpoint == null) {
				mFlySetpoint = new Setpoint(((Setpoint)source).getPo2(), mFlyMix);
			} else {
				mFlySetpoint.setPo2(((Setpoint)source).getPo2());
			}
			return mFlySetpoint;
		} else {
			return mFlyMix;
		}
	}*/

	/**
	 * Runs the current DecoAlgorithm and CnsOtu against the given profile
	 * @param deco The DecoAlgorithm to use. It should already be
	 * initialized with everything it needs to run, including any previous
	 * dives for which to account for residual nitrogen and helium.
	 * @throws IllegalStateException
	 * @throws CnsOtu.MaxPo2ExceededException thrown by CnsOtu if the max pO2
	 * is exceeded during the operation
	 */
	public void runDeco(DecoAlgorithm deco) throws IllegalStateException, CnsOtu.MaxPo2ExceededException {
		GasSource lastGasSource = null;
		mDive.initializeDeco(deco);
		CnsOtu cnsOtuState = mDive.buildCnsOtu();
		moveToFirst();
		List<ProfileItem> profile = mDive.getProfile();
		while(moveToNext()) {
			ProfileItem i = mCurrentLine;
			// Skip inactive items
			if(! i.isActive()) {
				continue;
			}
			if(lastGasSource == null) {
				lastGasSource = i.getGasSource();
			}

			// Run the deco algorithm
			ProfileItem deco_items[] = deco.run(i);

			// Any ProfileItems that were returned need to be added to the profile
			// before the current item as deco stops
			for(int j = 0; j < deco_items.length; j++) {
				final ProfileItem item = deco_items[j];
				profile.add(mPosition ++ - 1, item);
				
				// CNS/OTU for deco stop
				cnsOtuState.changeDepth(item.getDepth(), item.getDepthChangeTime(), lastGasSource);
				cnsOtuState.run(item.getSegtime(), item.getGasSource());
				
				// Set lastGasSource so we know what to use for the next
				// CNS/OTU depth change
				lastGasSource = item.getGasSource();
			}
			
			// CNS/OTU for the original ProfileItem we processed
			cnsOtuState.changeDepth(i.getDepth(), i.getSegtime(), lastGasSource);
			cnsOtuState.run(i.getSegtime(), i.getGasSource());
			
			// Set lastGasSource so we know what to use for the next
			// CNS/OTU depth change
			lastGasSource = deco.getGasSource();
		}
		// Now ascend to the surface
		float lastRuntime = deco.getRuntime();
		ProfileItem deco_items[] = deco.surface();
		
		// Final deco stops before surfacing
		for(int j = 0; j < deco_items.length; j++) {
			ProfileItem i = deco_items[j];
			profile.add(mPosition ++ - 1, i);
			
			// CNS/OTU for deco stop
			cnsOtuState.changeDepth(i.getDepth(), i.getDepthChangeTime(), lastGasSource);
			cnsOtuState.run(i.getSegtime(), i.getGasSource());
			
			// Set lastGasSource so we know what to use for the next
			// CNS/OTU depth change
			lastGasSource = i.getGasSource();
			
			// Update lastRuntime so we can compute the surface depth change time
			lastRuntime += i.getDepthChangeTime() + i.getSegtime();
		}
		// The final ascent time is computed by comparing the runtime at the
		// end of the last item we processed to deco.getRuntime()
		cnsOtuState.changeDepth(0, deco.getRuntime() - lastRuntime, lastGasSource);
		
		mDive.saveDeco(deco);
		mDive.saveCnsOtu(cnsOtuState);
		//mDive.commit();
	}

}