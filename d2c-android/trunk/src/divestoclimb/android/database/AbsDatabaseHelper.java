package divestoclimb.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

/**
 * Some common framework for all my DatabaseHelpers
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public abstract class AbsDatabaseHelper extends SQLiteOpenHelper {

	protected Context mContext;

	protected AbsDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		mContext = context;
	}
	
	/**
	 * Executes a series of SQL statements from a String array resource
	 * @param db The SQLiteDatabase against which to execute the statements
	 * @param res_id The database resource containing the statements
	 */
	protected void execStatements(SQLiteDatabase db, int res_id) {
		final String[] statements = mContext.getResources().getStringArray(res_id);
		for(int i = 0; i < statements.length; i ++) {
			db.execSQL(statements[i]);
		}
	}
}