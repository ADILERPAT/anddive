package divestoclimb.scuba.dive.data.android;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

import android.content.UriMatcher;
import android.net.Uri;

import divestoclimb.lib.scuba.GasSource;
import divestoclimb.lib.scuba.Mix;
import divestoclimb.lib.scuba.Setpoint;

/**
 * A simple utility class that maps GasSource objects back and forth to URI's,
 * since every GasSource subclass object can be 1-to-1 mapped to a URI.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class GasSourceUriMapper {

	/**
	 * A generic URI for any type of GasSource. Use this if you are requesting
	 * a new GasSource and don't care whether you get a Mix or Setpoint back.
	 */
	public static final Uri GASSOURCE_URI = Uri.parse("gassource://any/");
	
	/**
	 * A URI for a Setpoint. If you are specifically requesting a new Setpoint
	 * this URI may be used, but it's unlikely it will ever be needed publicly.
	 */
	public static final Uri SETPOINT_URI = Uri.parse("gassource://setpoint/");
	
	/**
	 * A URI for a Mix. If you are specifically requesting a new Mix this URI
	 * may be used.
	 */
	public static final Uri MIX_URI = Uri.parse("gassource://mix/");
	
	// URI Matching
	private static final int SETPOINT = 1;
	private static final int MIX = 2;
	private static final int SETPOINT_NODIL = 3;
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	static {
		URI_MATCHER.addURI(SETPOINT_URI.getAuthority(), "*/*/*", SETPOINT);
		URI_MATCHER.addURI(SETPOINT_URI.getAuthority(), "*", SETPOINT_NODIL);
		URI_MATCHER.addURI(MIX_URI.getAuthority(), "*/*", MIX);
	}

	/**
	 * Takes an existing GasSource and encodes it as a URI. Use this method to
	 * build an acceptable URI for starting an edit activity.
	 * @param source
	 * @return
	 */
	public static Uri getUri(GasSource source) {
		if(source == null) {
			return GASSOURCE_URI;
		}
		if(source instanceof Mix) {
			return Uri.withAppendedPath(MIX_URI,
					String.valueOf(((Mix)source).getO2()) + "/" +
					String.valueOf(((Mix)source).getHe())
			);
		} else if(source instanceof Setpoint) {
			final Mix diluent = ((Setpoint)source).getDiluent();
			if(diluent != null) {
				return Uri.withAppendedPath(SETPOINT_URI,
						String.valueOf(((Setpoint)source).getPo2()) + "/" +
						String.valueOf(diluent.getO2()) + "/" +
						String.valueOf(diluent.getHe())
				);
			} else {
				return Uri.withAppendedPath(SETPOINT_URI,
						String.valueOf(((Setpoint)source).getPo2())
				);
			}
		} else {
			return null;
		}
	}

	/**
	 * Takes a URI and decodes it into a GasSource instance. Use this method to
	 * retrieve a GasSource returned from an edit activity.
	 * @param uri
	 * @return
	 */
	public static GasSource getGasSource(Uri uri) {
		final List<String> args = uri.getPathSegments();
		final NumberFormat nf = DecimalFormat.getInstance();
		try {
			switch(URI_MATCHER.match(uri)) {
			case SETPOINT_NODIL:
				return new Setpoint(nf.parse(args.get(0)).floatValue(), null);
			case SETPOINT:
				return new Setpoint(nf.parse(args.get(0)).floatValue(),
						new Mix(nf.parse(args.get(1)).floatValue() / 100,
								nf.parse(args.get(2)).floatValue() / 100
						)
				);
			case MIX:
				return new Mix(nf.parse(args.get(0)).floatValue() / 100,
						nf.parse(args.get(1)).floatValue() / 100);
			default:
				return null;
			}
		} catch(ParseException e) {
			return null;
		}
	}
}