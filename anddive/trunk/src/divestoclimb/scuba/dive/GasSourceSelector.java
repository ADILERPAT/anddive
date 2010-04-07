package divestoclimb.scuba.dive;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.scuba.CnsOtu.MaxPo2ExceededException;
import divestoclimb.lib.scuba.GasSource;
import divestoclimb.lib.scuba.Localizer;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Setpoint;
import divestoclimb.lib.scuba.Units;

import divestoclimb.scuba.dive.data.android.AndroidLocalizer;
import divestoclimb.scuba.dive.data.android.GasSourceUriMapper;
import divestoclimb.widget.android.BaseNumberSelector;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class GasSourceSelector extends Activity implements View.OnClickListener,
		RadioGroup.OnCheckedChangeListener, BaseNumberSelector.ValueChangedListener {

	/**
	 * A boolean extra that can be added to the Intent. If true, the user will
	 * not be allowed to return a setpoint with no explicit diluent defined.
	 */
	public static final String KEY_MIX_REQUIRED = "mixRequired";
	/**
	 * User's current CNS accumulation. Used to compute maximum exposure time
	 */
	public static final String KEY_CURRENT_CNS = "currentCns";
	/**
	 * User's maximum CNS setting. Default is 80%
	 */
	public static final String KEY_MAX_CNS = "maxCns";
	/**
	 * User's current accumulated OTU's from previous dives. Used to compute
	 * maximum exposure time
	 */
	public static final String KEY_CURRENT_OTU = "currentOtu";
	/**
	 * User's maximum OTU setting. Default is 300, the NOAA recommended 10-day maximum.
	 */
	public static final String KEY_MAX_OTU = "maxOtu";
	/**
	 * Depth at which mix will be used, in meters
	 */
	public static final String KEY_DEPTH = "depth";

	// Private constants for instance states and activity request codes
	private static final int CODE_MIXPICK = 1;
	private static final String KEY_SETPOINT = "setpoint";
	private static final String KEY_FO2 = "fo2";
	private static final String KEY_FHE = "fhe";

	// Configuration
	private static final float DEFAULT_SETPOINT = 1.0f;
	
	// Activity state
	private GasSource mGasSource;
	private boolean mMixRequired;
	private float mCurrentCNS, mCurrentOTU;
	private int mMaxCNS, mMaxOTU, mDepth = -1;
	
	protected SharedPreferences mSettings;
	protected Units mUnits;

	// UI
	private RadioGroup mOCCC;
	private TextView mMixLabel;
	private Button mMixButton;
	private ImageButton mDeleteMix;
	private NumberSelector mPo2Selector;
	private TextView mLimits;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Bundle params = icicle != null? icicle: getIntent().getExtras();
		mMixRequired = params.getBoolean(KEY_MIX_REQUIRED);
		
		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		
		int unit;
		// Android issue 2096 - ListPreference won't work with an integer
		// array for values. Unit values are being stored as Strings then
		// we convert them here for use.
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		mUnits = new Units(unit);
		
		// Depth, CNS, and OTU parameters
		mDepth = Math.round(mUnits.convertDepth(params.getFloat(KEY_DEPTH, -1), Units.METRIC));
		mCurrentCNS = params.getFloat(KEY_CURRENT_CNS, -1);
		mMaxCNS = params.getInt(KEY_MAX_CNS, 80);
		mCurrentOTU = params.getFloat(KEY_CURRENT_OTU, -1);
		mMaxOTU = params.getInt(KEY_MAX_OTU, 300);

		if(icicle == null) {
			mGasSource = GasSourceUriMapper.getGasSource(getIntent().getData());
		} else {
			Mix mix = null;
			if(icicle.containsKey(KEY_FO2) && icicle.containsKey(KEY_FHE)) {
				mix = new Mix(icicle.getDouble(KEY_FO2), icicle.getDouble(KEY_FHE));
			}
			if(icicle.containsKey(KEY_SETPOINT)) {
				mGasSource = new Setpoint(icicle.getFloat(KEY_SETPOINT), mix);
			} else {
				mGasSource = mix;
			}
		}
		if(mGasSource == null) {
			// Set a default GasSource to start
			Mix mix = mMixRequired? new Mix(0.21f, 0): null;
			mGasSource = new Setpoint(DEFAULT_SETPOINT, mix);
		}

		// Set the Localizer Engine for displaying GasSources
		Localizer.setEngine(new AndroidLocalizer(this));

		setContentView(R.layout.gassource_selector);

		mOCCC = (RadioGroup)findViewById(R.id.occc);
		mOCCC.setOnCheckedChangeListener(this);
		mMixLabel = (TextView)findViewById(R.id.mixlabel);

		mPo2Selector = (NumberSelector)findViewById(R.id.po2);
		mPo2Selector.setValue(DEFAULT_SETPOINT);
		mPo2Selector.setValueChangedListener(this);

		mMixButton = (Button)findViewById(R.id.mix);
		mMixButton.setOnClickListener(this);

		mDeleteMix = (ImageButton)findViewById(R.id.mix_remove);
		if(! mMixRequired) {
			mDeleteMix.setOnClickListener(this);
		} else {
			mDeleteMix.setVisibility(View.GONE);
		}
		mLimits = (TextView)findViewById(R.id.limits);

		Button help = (Button)findViewById(R.id.help);
		help.setOnClickListener(this);

		Button cancel = (Button)findViewById(R.id.cancel);
		cancel.setOnClickListener(this);

		Button ok = (Button)findViewById(R.id.ok);
		ok.setOnClickListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if(mGasSource instanceof Mix) {
			mOCCC.check(R.id.open_circuit);
		} else {
			mOCCC.check(R.id.closed_circuit);
			mPo2Selector.setValue(((Setpoint)mGasSource).getPo2());
		}
		if(mGasSource instanceof Mix) {
			mMixButton.setText(mGasSource.toString());
		} else {
			Mix diluent = ((Setpoint)mGasSource).getDiluent();
			if(diluent != null) {
				mMixButton.setText(diluent.toString());
			} else {
				mMixButton.setText(getString(R.string.unspecified));
			}
		}
		setLimits();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_MIX_REQUIRED, mMixRequired);
		// Save the current GasSource
		if(mGasSource instanceof Setpoint) {
			outState.putFloat(KEY_SETPOINT, ((Setpoint)mGasSource).getPo2());
			Mix dil = ((Setpoint)mGasSource).getDiluent();
			if(dil != null) {
				outState.putDouble(KEY_FO2, dil.getfO2());
				outState.putDouble(KEY_FHE, dil.getfHe());
			}
		} else {
			outState.putDouble(KEY_FO2, ((Mix)mGasSource).getfO2());
			outState.putDouble(KEY_FHE, ((Mix)mGasSource).getfHe());
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == CODE_MIXPICK && resultCode == RESULT_OK) {
			Mix result = (Mix)GasSourceUriMapper.getGasSource(data.getData());
			if(mGasSource instanceof Setpoint) {
				((Setpoint)mGasSource).setDiluent(result);
			} else {
				mGasSource = result;
			}
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if(checkedId == R.id.closed_circuit) {
			mDeleteMix.setEnabled(true);
			mPo2Selector.setVisibility(View.VISIBLE);
			mMixLabel.setText(getString(R.string.diluent));
			if(! (mGasSource instanceof Setpoint)) {
				Mix diluent = (Mix)mGasSource;
				mGasSource = new Setpoint(DEFAULT_SETPOINT, diluent);
			}
		} else {
			mDeleteMix.setEnabled(false);
			mPo2Selector.setVisibility(View.INVISIBLE);
			if(mGasSource instanceof Setpoint) {
				mGasSource = ((Setpoint)mGasSource).getDiluent();
			}
			if(mGasSource == null) {
				mGasSource = new Mix(0.21f, 0);
			}
		}
		if(checkedId == R.id.open_circuit) {
			mMixLabel.setText(getString(R.string.mix));
		}
		setLimits();
	}

	@Override
	public void onChange(BaseNumberSelector ns, Float value, boolean fromUser) {
		if(mGasSource instanceof Setpoint) {
			((Setpoint)mGasSource).setPo2(value);
			setLimits();
		}
	}
	
	private void setLimits() {
		String format;
		int depth;
		if(mDepth > -1) {
			// Case 1: a depth is known. Show max time that can be spent at that depth and limiting factor
			format = getString(R.string.o2_limit_depth);
			depth = mDepth;
		} else if(mGasSource instanceof Setpoint) {
			// Case 2: no depth, but mGasSource is a Setpoint. Max time is independent of depth, so show max time that can be spent on setpoint and limiting factor
			format = getString(R.string.o2_limit);
			depth = 50;
		} else {
			// Case 3: no depth, mGasSource is a Mix. Clear mLimits, we don't have enough information.
			mLimits.setText("");
			return;
		}

		try {
			int maxCns = mGasSource.computeMaxCNSExposure(depth, mUnits, mCurrentCNS, mMaxCNS);
			int maxOtu = mGasSource.computeMaxOTUExposure(depth, mUnits, mCurrentOTU, mMaxOTU);
			String limitingFactor = getString(maxCns < maxOtu? R.string.cns: R.string.otu);
			String depthUnit = getString(mUnits.depthUnit() == Units.IMPERIAL? R.string.depth_imperial: R.string.depth_metric);
			mLimits.setText(String.format(format, Math.min(maxCns, maxOtu), limitingFactor, depth, depthUnit));
		} catch(MaxPo2ExceededException e) {
			mLimits.setText(getString(R.string.max_po2_exceeded));
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.help:
			// Show help window
			break;
		case R.id.cancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		case R.id.mix_remove:
			if(mGasSource instanceof Setpoint) {
				((Setpoint) mGasSource).setDiluent(null);
			}
			mMixButton.setText(getString(R.string.unspecified));
			break;
		case R.id.mix:
			// Launch mix selector activity
			Mix mix = mGasSource instanceof Setpoint? ((Setpoint)mGasSource).getDiluent(): (Mix)mGasSource;
			Uri uri = mix != null? GasSourceUriMapper.getUri(mix): GasSourceUriMapper.MIX_URI;
			Intent i = new Intent(Intent.ACTION_EDIT, uri);
			if(mGasSource instanceof Setpoint) {
				i.putExtra("po2", ((Setpoint)mGasSource).getPo2());
			}
			if(mDepth > -1) {
				i.putExtra("depth", Units.convertDepth(mDepth, mUnits.getCurrentSystem(), Units.METRIC));
			}
			i.putExtra("currentCns", mCurrentCNS)
				.putExtra("maxCns", mMaxCNS)
				.putExtra("currentOtu", mCurrentOTU)
				.putExtra("maxOtu", mMaxOTU);
			startActivityForResult(i, CODE_MIXPICK);
			break;
		case R.id.ok:
			Intent result = new Intent();
			result.setData(GasSourceUriMapper.getUri(mGasSource));
			setResult(RESULT_OK, result);
			finish();
			break;
		}
	}
}