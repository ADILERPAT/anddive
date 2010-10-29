package divestoclimb.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Formatting {

	public static final DateFormat ISO8601_STANDARD_ZULU = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
	public static final DecimalFormatSymbols NORMALIZED_DECIMAL_SYMBOLS = new DecimalFormatSymbols();
	static {
		ISO8601_STANDARD_ZULU.setTimeZone(TimeZone.getTimeZone("GMT"));
		NORMALIZED_DECIMAL_SYMBOLS.setDecimalSeparator('.');
		NORMALIZED_DECIMAL_SYMBOLS.setMinusSign('-');
		NORMALIZED_DECIMAL_SYMBOLS.setZeroDigit('0');
	}

	public static DecimalFormat buildNormalizedFormat(String pattern) {
		DecimalFormat fmt = new DecimalFormat(pattern);
		fmt.setGroupingUsed(false);
		fmt.setDecimalFormatSymbols(NORMALIZED_DECIMAL_SYMBOLS);
		return fmt;
	}
}