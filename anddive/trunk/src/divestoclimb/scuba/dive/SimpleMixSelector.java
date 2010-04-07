package divestoclimb.scuba.dive;

import divestoclimb.lib.scuba.Mix;
import divestoclimb.scuba.dive.data.android.GasSourceUriMapper;
import divestoclimb.widget.android.BaseNumberSelector;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SimpleMixSelector extends Activity implements BaseNumberSelector.ValueChangedListener,
		View.OnClickListener {

	// Private constants for instance states
	private static final String KEY_FO2 = "fo2";
	private static final String KEY_FHE = "fhe";

	// Activity state
	private Mix mMix;
	
	// UI
	private NumberSelector mO2Selector, mHeSelector;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		if(icicle == null) {
			mMix = (Mix)GasSourceUriMapper.getGasSource(getIntent().getData());
		} else if(icicle.containsKey(KEY_FO2) && icicle.containsKey(KEY_FHE)) {
			mMix = new Mix(icicle.getDouble(KEY_FO2), icicle.getDouble(KEY_FHE));
		}
		if(mMix == null) {
			mMix = new Mix(0.21f, 0);
		}
		
		setContentView(R.layout.mixselector);
		
		mO2Selector = (NumberSelector)findViewById(R.id.o2);
		mO2Selector.setValueChangedListener(this);
		mHeSelector = (NumberSelector)findViewById(R.id.he);
		mHeSelector.setValueChangedListener(this);
		
		// OK and cancel buttons
		findViewById(R.id.cancel).setOnClickListener(this);
		findViewById(R.id.ok).setOnClickListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		mO2Selector.setValue(mMix.getO2());
		mHeSelector.setValue(mMix.getHe());
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putDouble(KEY_FO2, mMix.getfO2());
		outState.putDouble(KEY_FHE, mMix.getfHe());
	}

	@Override
	public void onChange(BaseNumberSelector ns, Float newVal, boolean fromUser) {
		if(fromUser) {
			BaseNumberSelector other_field;
			if(ns == mO2Selector) {
				other_field = mHeSelector;
			} else {
				other_field = mO2Selector;
			}
			if(newVal + other_field.getValue() > 100) {
				other_field.setValue(100 - newVal);
				if(other_field.getValue() > 100 - newVal) {
					// The other field must have run into a limiting
					// constraint with its value. As an alternative,
					// set our value to keep the total at or below 100.
					ns.setValue(100 - other_field.getValue());
				}
			}
			mMix.setfO2(mO2Selector.getValue() / 100);
			mMix.setfHe(mHeSelector.getValue() / 100);
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.cancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		case R.id.ok:
			Intent result = new Intent();
			result.setData(GasSourceUriMapper.getUri(mMix));
			setResult(RESULT_OK, result);
			finish();
		}
	}
}