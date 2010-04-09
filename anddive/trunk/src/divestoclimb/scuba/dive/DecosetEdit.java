package divestoclimb.scuba.dive;

import java.text.NumberFormat;
import java.text.ParseException;

import divestoclimb.lib.data.Record;
import divestoclimb.lib.scuba.*;
import divestoclimb.scuba.dive.data.android.AndroidLocalizer;
import divestoclimb.scuba.dive.data.android.GasSourceUriMapper;
import divestoclimb.scuba.dive.storage.PublicORMapper;
import divestoclimb.widget.android.ObjectMappedCursorAdapter;
import divestoclimb.widget.android.BaseNumberSelector;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class DecosetEdit extends ListActivity implements View.OnClickListener,
		ObjectMappedCursorAdapter.ObjectMapper<Decoset.Item>,
		ObjectMappedCursorAdapter.ViewBinder<Decoset.Item>, BaseNumberSelector.ValueChangedListener {

	private static final String KEY_DECOSET_ID = "decoset_id";
	private static final String KEY_OPENITEM_ID = "openitem";
	
	private static final int CODE_GASPICK = 1; 

	private PublicORMapper mORMapper;

	private boolean mInitialized = false;
	private Button mGasSource;
	// Various clickable items. We don't care what kind (Button vs. ImageButton)
	private View mGo, mDelete;
	private ToggleButton mOpenCircuit;
	private NumberSelector mDepth;
	private TextView mDepthLabel;

	// The Decoset this Activity is showing
	private Decoset mDecoset;
	// The currently open Decoset Item
	private Decoset.Item mOpenItem;
	// I prefer not to keep references to the cursor in use in a ListActivity,
	// but in this case it's better because the ListView has a HeaderViewListAdapter
	private Cursor mItemCursor;

	// Flyweight objects for the ObjectMapper
	private Decoset.Item mFlyweightItem;
	private final Mix mFlyweightMix = new Mix(0, 0);
	private final Setpoint mFlyweightSetpoint = new Setpoint(0, mFlyweightMix);
	
	protected SharedPreferences mSettings;
	private Units mUnits;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		mSettings = PreferenceManager.getDefaultSharedPreferences(this);
		
		int unit;
		// Android issue 2096 - ListPreference won't work with an integer
		// array for values. Unit values are being stored as Strings then
		// we convert them here for use.
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		mUnits = new Units(unit);
		
		// Set the Localizer Engine for displaying GasSources
		Localizer.setEngine(new AndroidLocalizer(this));
		
		mORMapper = new PublicORMapper(this, mUnits);
		mFlyweightItem = new Decoset.Item(Record.NO_ID, 0, null);

		long id;
		Uri uri = getIntent().getData();
		// Use the ContentProvider to make sure this URI is correct
		if(uri != null) {
			id = Long.valueOf(uri.getLastPathSegment());
		} else {
			id = icicle.getLong(KEY_DECOSET_ID);
		}
		mDecoset = mORMapper.fetchDecoset(id);
		setTitle(String.format(getString(R.string.edit_decoset), mDecoset.getName()));

		if(icicle != null && icicle.containsKey(KEY_OPENITEM_ID)) {
			mOpenItem = mORMapper.fetchDecosetItem(icicle.getLong(KEY_OPENITEM_ID));
		}

		setContentView(R.layout.decoset_edit);
		// Build the header view
		LayoutInflater i = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View headerView = i.inflate(android.R.layout.simple_list_item_1, null);
		// TODO: make "Add" text less generic, italicize
		((TextView)headerView.findViewById(android.R.id.text1)).setText(getString(R.string.create_new));
		getListView().addHeaderView(headerView);

		mOpenCircuit = (ToggleButton)findViewById(R.id.oc_toggle);

		mGasSource = (Button)findViewById(R.id.gasSrc);
		mGasSource.setOnClickListener(this);
		mGasSource.setEnabled(false);

		mDepth = (NumberSelector)findViewById(R.id.depth);
		mDepth.setValueChangedListener(this);

		mDepthLabel = (TextView)findViewById(R.id.depth_unit);

		mDelete = findViewById(R.id.delete);
		mDelete.setOnClickListener(this);
		mDelete.setEnabled(false);

		mGo = findViewById(R.id.go);
		mGo.setOnClickListener(this);
		mGo.setEnabled(false);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(! mInitialized) {
			mItemCursor = mORMapper.fetchDecosetItems(mDecoset.getId());
			Log.w("DecosetEdit", String.valueOf(mItemCursor.getCount()));
			startManagingCursor(mItemCursor);
			ListAdapter adapter = new ObjectMappedCursorAdapter<Decoset.Item>(this,
					android.R.layout.simple_list_item_1,
					mItemCursor,
					new int [] { android.R.id.text1 },
					this,
					this
			);
			setListAdapter(adapter);
			mInitialized = true;
		} else {
			mItemCursor.requery();
		}

		final Units units = mUnits;
		// Units
		Integer unit, last_unit = null;
		try {
			unit = NumberFormat.getIntegerInstance().parse(mSettings.getString("units", "0")).intValue();
		} catch(ParseException e) { unit = 0; }
		if(unit.compareTo(units.getCurrentSystem()) != 0) {
			// The user switched units since we were last loaded.
			last_unit = units.getCurrentSystem();
			units.change(unit);
		}
		
		ocCheck();
		
		if(mOpenItem != null) {
			mGasSource.setText(mOpenItem.getGasSource().toString());
		}
		
		updateUnits(last_unit);
	}

	@Override
	public Decoset.Item getObjectFromCursor(Cursor c) {
		return mORMapper.fetchDecosetItem(c, mFlyweightItem, mFlyweightMix, mFlyweightSetpoint);
	}

	@Override
	public void setViewValue(View view, Decoset.Item obj) {
		if(view.getId() == android.R.id.text1) {
			final int depth_unit = mUnits.getCurrentSystem() == Units.METRIC? R.string.depth_metric: R.string.depth_imperial;
			((TextView)view).setText(getString(R.string.decoset_item,
					obj.getMaxDepth(), getString(depth_unit),
					obj.getGasSource().toString()
			));
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(KEY_DECOSET_ID, mDecoset.getId());
		if(mOpenItem != null) {
			// Auto-save the item
			mOpenItem.commit();
			outState.putLong(KEY_OPENITEM_ID, mOpenItem.getId());
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == CODE_GASPICK && resultCode == RESULT_OK) {
			GasSource source = GasSourceUriMapper.getGasSource(data.getData());
			if(mOpenItem == null) {
				int depth;
				if(source.getClass() == Mix.class) {
					// TODO: fetch max deco po2 from preferences
					depth = (int)Math.floor(((Mix)source).MOD(mUnits, 1.6f) / 10) * 10;
				} else {
					// TODO: decide how to determine initial depth for setpoint
					depth = 100;
				}
				mOpenItem = new Decoset.Item(mDecoset.getId(), depth, source);
			} else {
				mOpenItem.setGasSource(source);
			}
			mGo.setEnabled(true);
		}
	}
	
	@Override
	public void onChange(BaseNumberSelector ns, Float newVal, boolean fromUser) {
		if(fromUser) {
			// FIXME this will throw a NullPointerException if the user tries to set depth
			// before setting a GasSource. Need a solution...
			mOpenItem.setMaxDepth(newVal.intValue());
			mGo.setEnabled(true);
		}
	}

	protected void resetFields() {
		mGo.setEnabled(false);
		mDelete.setEnabled(false);
		mOpenItem = null;
		mGasSource.setText(getString(R.string.select_gas));
		// TODO: clear the depth selector
	}
	
	// Iterate through all decoset items and the currently open item; check if
	// each item's GasSource is a Mix or Setpoint. If any are Setpoints, turn
	// off the OC ToggleButton and disable it; otherwise, turn it on
	private void ocCheck() {
		Cursor listCur = mItemCursor;
		boolean isOC = true;
		for(listCur.moveToFirst(); ! listCur.isAfterLast(); listCur.moveToNext()) {
			Decoset.Item i = mORMapper.fetchDecosetItem(listCur, true);
			if(i.getGasSource().getClass() == Setpoint.class) {
				isOC = false;
				break;
			}
		}
		// If we're editing/adding an item, check the currently loaded value
		isOC &= mOpenItem == null || mOpenItem.getGasSource().getClass() == Mix.class;
		mOpenCircuit.setChecked(isOC);
		mOpenCircuit.setEnabled(isOC);
	}
	
	private void updateUnits(Integer last_unit) {
		mDepth.setDecimalPlaces(0);
		mDepth.setLimits(0f, new Float(mUnits.depthMax()));
		mDepth.setIncrement(new Float(mUnits.depthIncrement()));
		mDepthLabel.setText(getString(mUnits.depthUnit() == Units.IMPERIAL? R.string.depth_imperial: R.string.depth_metric) + ":");
		
		if(last_unit != null && mDepth.getValue() != null) {
			mDepth.setValue(mUnits.convertDepth(mDepth.getValue(), last_unit));
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(position == 0) {
			// mOpenItem will be set after a gas selector activity comes back.
			// Before that time, we'll have nothing to save if this activity
			// stops.
			resetFields();
		} else {
			Cursor c = (Cursor)l.getItemAtPosition(position);
			mOpenItem = mORMapper.fetchDecosetItem(c, mOpenItem, null, null);
			mGasSource.setText(mOpenItem.getGasSource().toString());
			mDepth.setValue(mOpenItem.getMaxDepth());
		}
		mGasSource.setEnabled(true);
		mGasSource.requestFocus();
	}

	@Override
	public void onClick(View v) {
		final ListView lv = getListView();
		switch(v.getId()) {
		case R.id.gasSrc:
			Uri uri = null;
			// BUG: This code doesn't let you turn a Mix into a GasSource; editing
			// a Mix will always open a Mix Selector. This isn't necessarily a
			// bad thing, it just means if you wanted to turn a Mix into a
			// Setpoint you'd have to delete the line and re-add it.
			if(mOpenItem != null) {
				uri = GasSourceUriMapper.getUri(mOpenItem.getGasSource());
			} else if(mOpenCircuit.isChecked()) {
				uri = GasSourceUriMapper.MIX_URI;
			} else {
				uri = GasSourceUriMapper.GASSOURCE_URI;
			}
			Intent i = new Intent(Intent.ACTION_EDIT, uri);
			if(mOpenItem != null) {
				i.putExtra(GasSourceSelector.KEY_DEPTH, mOpenItem.getMaxDepth());
			}
			startActivityForResult(i, CODE_GASPICK);
			break;
		case R.id.delete:
			mOpenItem.delete();
			mItemCursor.requery();
			ocCheck();
			break;
		case R.id.go:
			// TODO check for success. Show error if unsuccessful, otherwise continue with the rest
			mOpenItem.commit();
			mItemCursor.requery();
			if(lv.getSelectedItemPosition() == 0) {
				// reset in preparation for another add
				resetFields();
			} else {
				mGo.setEnabled(false);
			}
			break;
		}
	}
}