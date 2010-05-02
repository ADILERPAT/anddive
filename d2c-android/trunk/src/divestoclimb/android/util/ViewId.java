package divestoclimb.android.util;

import java.util.Random;

import android.view.View;

/**
 * A class for dynamically generating View id's
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class ViewId {

	/**
	 * Find a valid ID that's not in use as a child of the given view.
	 * @param v The view to search from. You probably want to use getRootView()
	 * @return A valid ID that is not currently in use by any child view of v
	 */
	public static int generateUnique(View v) {
		Random r = new Random();
		
		int id;
		do {
			id = r.nextInt();
		} while(id <= 0 || v.findViewById(id) != null);
		return id;
	}
}
