package divestoclimb.android.widget;

import java.util.WeakHashMap;

import divestoclimb.android.database.CursorObjectMapper;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;

/**
 * An adapter which binds a cursor to a layout through an intermediate
 * object, instead of having direct knowledge of the cursor's columns, their
 * meaning and desired formatting. Interfaces handle the mapping of Cursor data
 * to an object, then from the object to individual views in the layout.
 * @author Ben Roberts (divestoclimb@gmail.com)
 * 
 * @param <T> The intermediate object class through which to map
 */
public class ObjectMappedCursorAdapter<T> extends ResourceCursorAdapter {

	protected int[] mViewsToBind;
	protected CursorObjectMapper<T> mObjectMapper;
	protected ViewBinder<T> mViewBinder;
	protected WeakHashMap<View, View[]> mHolders = new WeakHashMap<View, View[]>();

	/**
	 * Constructor.
	 * 
	 * @param context The context where the ListView is running
	 * @param layout resource identifier of a layout file that defines the views
	 * 				for this list item
	 * @param c The database cursor
	 * @param viewsToBind The views within the layout to pass to ViewBinder for binding
	 * @param objectMapper An implementation of ObjectMapper that maps each
	 * 				cursor row to an object
	 * @param viewBinder An implementation of ViewBinder that populates a passed
	 * 				view from viewsToBind with data from the row's object
	 */
	public ObjectMappedCursorAdapter(Context context, int layout, Cursor c,
			int[] viewsToBind, CursorObjectMapper<T> objectMapper, ViewBinder<T> viewBinder) {
		super(context, layout, c);
		mViewsToBind = viewsToBind;
		mObjectMapper = objectMapper;
		mViewBinder = viewBinder;
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return generateViewHolder(super.newView(context, cursor, parent));
	}
	
	@Override
	public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
		return generateViewHolder(super.newDropDownView(context, cursor, parent));
	}
	
	// Taken from SimpleCursorAdapter. Caches views we wish to bind to
	private View generateViewHolder(View v) {
		final int[] to = mViewsToBind;
		final int count = to.length;
		final View[] holder = new View[count];
		
		for(int i = 0; i < count; i ++) {
			holder[i] = v.findViewById(to[i]);
		}
		mHolders.put(v, holder);
		
		return v;
	}

	/**
	 * Binds all of the views passed in the "viewsToBind" parameter of the
	 * constructor via the ObjectMapper and ViewBinder.
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final View[] holder = mHolders.get(view);
		final ViewBinder<T> viewBinder = mViewBinder;
		final int count = mViewsToBind.length;
		T obj = mObjectMapper.getObjectFromCursor(cursor, true);
		
		for(int i = 0; i < count; i ++) {
			viewBinder.setViewValue(holder[i], obj);
		}
	}
	
	/**
	 * Binds data from the backing object to individual Views.
	 * @author Ben Roberts (divestoclimb@gmail.com)
	 *
	 * @param <T> The type of backing object
	 */
	public static interface ViewBinder<T> {
		/**
		 * Bind the appropriate data from obj to the given view. For a
		 * given row of the cursor, this method will be called for each
		 * view ID in the CursorAdapter's viewsToBind parameter.
		 * @param view The View to set up
		 * @param obj The backing object whose data should be applied to
		 * view
		 */
		public void setViewValue(View view, T obj);
	}
}