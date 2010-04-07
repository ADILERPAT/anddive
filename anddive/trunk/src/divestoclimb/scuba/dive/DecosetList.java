package divestoclimb.scuba.dive;

import divestoclimb.lib.scuba.Decoset;
import divestoclimb.scuba.dive.storage.PublicORMapper;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class DecosetList extends ListActivity implements View.OnClickListener {

	private PublicORMapper mORMapper;
	private EditText mNewName;
	private boolean mInitialized = false;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.decoset_list);

		mORMapper = new PublicORMapper(this);

		registerForContextMenu(getListView());

		// OnClickListener for add button
		findViewById(R.id.create_new).setOnClickListener(this);

		mNewName = (EditText)findViewById(R.id.name);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(! mInitialized) {
			final Cursor c = mORMapper.fetchDecosets();
			startManagingCursor(c);
			final ListAdapter adapter = new SimpleCursorAdapter(this,
					android.R.layout.simple_list_item_1,
					c,
					new String[] { PublicORMapper.KEY_DECOSET_NAME },
					new int [] { android.R.id.text1 }
			);
			setListAdapter(adapter);
			mInitialized = true;
		} else {
			((CursorAdapter)getListView().getAdapter()).getCursor().requery();
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.decoset_list_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
		case R.id.edit:
			edit(info.id);
			break;
		case R.id.delete:
			// TODO check if this is the back gas decoset. If so, warn that it can't be deleted
			mORMapper.deleteDecoset(info.id);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		final String name = mNewName.getText().toString();
		if(name.length() == 0) {
			Toast.makeText(this, getString(R.string.missing_name), Toast.LENGTH_SHORT);
		}
		final Decoset decoset = new Decoset(name);
		decoset.setUpdater(mORMapper.mDecosetUpdater);
		decoset.commit();
		edit(decoset.getID());
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		edit(id);
	}
	
	private void edit(long id) {
		// TODO don't allow editing the Backgas decoset
		final Uri uri = Uri.withAppendedPath(PublicORMapper.CONTENT_URI, "decosets/" + String.valueOf(id));
		startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}
}