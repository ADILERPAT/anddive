package divestoclimb.scuba.dive.data.android;

import android.database.Cursor;
import divestoclimb.lib.scuba.Dive;
import divestoclimb.lib.scuba.ProfileIterator;
import divestoclimb.lib.scuba.ProfileItem;
import divestoclimb.scuba.dive.storage.PublicORMapper;
import divestoclimb.util.android.CursorOrderer;

public class CursorProfileIterator extends ProfileIterator<ProfileItem> {

	protected long mDiveId;
	protected PublicORMapper mORMapper;
	protected CursorOrderer<ProfileItem> mOrderer;
	protected Cursor mCursor;
	
	public CursorProfileIterator(PublicORMapper mapper, Dive dive, int source) {
		super(dive, source);
		mORMapper = mapper;
		mCursor = mapper.fetchProfileItems(dive.getId());
	}
	
	public Cursor getCursor() {
		return mCursor;
	}

	@Override
	public int getCount() {
		return mCursor.getCount();
	}

	@Override
	protected ProfileItem getItemAtPosition(int position) {
		// TODO needs to handle flyweight logic
		mCursor.moveToPosition(position);
		return mORMapper.fetchProfileItem(mCursor);
	}

	@Override
	protected boolean insertItemAtPosition(ProfileItem item, int position) {
		if(mOrderer == null) {
			mOrderer = new CursorOrderer<ProfileItem>(mCursor, new CursorOrderer.ObjectMapper<ProfileItem>() {
				@Override
				public ProfileItem getFromCursor(Cursor c) {
					return mORMapper.fetchProfileItem(c, true);
				}
			});
		}
		return mOrderer.add(item, position);
	}
}