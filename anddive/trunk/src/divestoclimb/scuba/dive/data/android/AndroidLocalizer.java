package divestoclimb.scuba.dive.data.android;

import divestoclimb.scuba.dive.R;
import android.content.Context;
import divestoclimb.lib.scuba.Localizer;

public class AndroidLocalizer implements Localizer.Engine {

	private Context mCtx;

	public AndroidLocalizer(Context ctx) {
		mCtx = ctx;
	}

	@Override
	public String getString(int resource) {
		int androidRes;
		switch(resource) {
		case Localizer.STRING_AIR:
			androidRes = R.string.air;
			break;
		case Localizer.STRING_OXYGEN:
			androidRes = R.string.oxygen;
			break;
		case Localizer.STRING_HELIUM:
			androidRes = R.string.helium;
			break;
		case Localizer.STRING_NITROGEN:
			androidRes = R.string.nitrogen;
			break;
		default:
			return null;
		}
		return mCtx.getString(androidRes);
	}

}