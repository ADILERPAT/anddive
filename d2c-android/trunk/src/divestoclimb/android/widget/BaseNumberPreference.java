package divestoclimb.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A DialogPreference containing a single NumberSelector
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class BaseNumberPreference extends DialogPreference {
	protected BaseNumberSelector mNumberSelector;
	private LinearLayout mLayout;
	protected TextView mUnitLabel;
	private boolean mLayoutInit;

	private float mValue;

	public BaseNumberPreference(Context context, ResourceReader reader, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		// Initialize our views that make up the content of the dialog.
		// We reuse the same instance of the layout each time the dialog
		// is closed and opened.
		mNumberSelector = new BaseNumberSelector(context, reader, attrs);
		mLayout = new LinearLayout(context);
		mLayout.setGravity(Gravity.CENTER);

		// Read our custom attributes
		TypedArray a = reader.getNPStyledAttributes(context, attrs);
		mUnitLabel = new TextView(context);
		String label = reader.readNPUnitLabel(a);
		if(label != null) {
			mUnitLabel.setText(label);
		}
		a.recycle();
		
		// mLayoutInit lets us know if we've build mLayout yet. This occurs
		// in onCreateDialogView
		mLayoutInit = false;
	}

	public BaseNumberPreference(Context context, ResourceReader reader, AttributeSet attrs) {
		this(context, reader, attrs, android.R.attr.dialogPreferenceStyle);
	}
	
	public BaseNumberPreference(Context context, ResourceReader reader) {
		this(context, reader, null);
	}

	public static interface ResourceReader extends BaseNumberSelector.ResourceReader {
		abstract TypedArray getNPStyledAttributes(Context context, AttributeSet attrs);
		abstract String readNPUnitLabel(TypedArray a);
	}

	public void setValue(float value) {
		mValue = value;
		persistFloat(value);
	}

	public float getValue() {
		return mValue;
	}
	
	@Override
	protected View onCreateDialogView() {
		if(! mLayoutInit) {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
			mLayout.addView(mUnitLabel, params);
			mLayout.addView(mNumberSelector, params);
			mLayoutInit=true;
		}
		return mLayout;
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		// All we have to do here is initialize the value. The
		// superclass did the rest.
		mNumberSelector.setValue(getValue());
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if(positiveResult) {
			Float value = mNumberSelector.getValue();
			if(value != null && callChangeListener(value)) {
				setValue(value);
			}
		}
		// Remove the layout from the parent view so it can be reused
		// if the dialog is reopened.
		((ViewGroup)mLayout.getParent()).removeView(mLayout);
	}
	
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getFloat(index, 0);
	}
	
	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		mValue = restoreValue? getPersistedFloat(mValue): (Float)defaultValue;
	}
	
	public BaseNumberSelector getNumberSelector() {
		return mNumberSelector;
	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		Float val = mNumberSelector.getValue();
		if(val != null) {
			final SavedState myState = new SavedState(superState);
			myState.value = val;
			return myState;
		} else {
			return superState;
		}

	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		mNumberSelector.setValue(myState.value);
	}
	
	private static class SavedState extends BaseSavedState {
		float value;

		public SavedState(Parcel source) {
			super(source);
			value = source.readFloat();
        }

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeFloat(value);
        }

		public SavedState(Parcelable superState) {
			super(superState);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR =
			new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

}