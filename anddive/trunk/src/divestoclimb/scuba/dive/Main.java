package divestoclimb.scuba.dive;

import java.io.File;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class Main extends ListActivity {

	// Some application-wide parameters
	public static final String MISSION_EXTENSION = "anddive";
	public static final File MISSION_DEFAULT_LOCATION = new File(Environment.getExternalStorageDirectory(), "anddive");

	public static final int CODE_BACKUP = 2;
	public static final int CODE_RESTORE = 3;

	public static final int RESULT_FAILED = RESULT_FIRST_USER + 1;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, new String[] {
					getString(R.string.missionlist),
					getString(R.string.divelist),
					getString(R.string.decosets),
					getString(R.string.settings)
				}
		));
		getListView().setTextFilterEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.about:
			startActivity(new Intent(this, About.class));
			break;
		case R.id.backup:
			//startActivityForResult(new Intent(this, Backup.class), CODE_BACKUP);
			break;
		case R.id.restore:
			//startActivityForResult(new Intent(this, Restore.class), CODE_RESTORE);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case CODE_BACKUP:
			Toast.makeText(this, resultCode == RESULT_OK? R.string.backup_successful: R.string.backup_failure, Toast.LENGTH_SHORT).show();
			break;
		case CODE_RESTORE:
			Toast.makeText(this, resultCode == RESULT_OK? R.string.restore_successful: R.string.restore_failure, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(position >= 0) {
			switch(position) {
			case 0:
				//startActivity(new Intent(this, MissionList.class));
				break;
			case 1:
				//startActivity(new Intent(this, DiveList.class));
				break;
			case 2:
				startActivity(new Intent(this, DecosetList.class));
				break;
			case 3:
				startActivity(new Intent(this, Settings.class));
				break;
			}
		}
	}
}