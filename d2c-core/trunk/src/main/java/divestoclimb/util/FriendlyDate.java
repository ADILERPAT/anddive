package divestoclimb.util;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A class of utility functions for outputting date/time information relative
 * to another date/time.
 * TODO: localize. This would be best done by keeping this class's logic Android-
 * independent so it returned some kind of structured format that could build
 * an output string for format strings stored in resources.
 * @author Ben Roberts (divestoclimb@gmail.com)
 */
public class FriendlyDate {

	/**
	 * Get a string representation of a date as it relates to the current time.
	 * @param time The Date object to use to generate the string representation
	 * @return A string explaining how long ago or in the future the date is from 
	 * now.
	 */
	public static String format(Date time) {
		return format(time, new Date());
	}

	// This implementation can't handle leap seconds, but it really doesn't
	// need to be that precise for my purposes
	public static final int MILLIS_PER_SEC=1000;
	public static final int MILLIS_PER_MIN=MILLIS_PER_SEC*60;
	public static final int MILLIS_PER_HR=MILLIS_PER_MIN*60;
	public static final int MILLIS_PER_DAY=MILLIS_PER_HR*24;
	public static final int MILLIS_PER_WEEK=MILLIS_PER_DAY*7;

	/**
	 * Get a string representation of a date as it relates to another date.
	 * @param time The Date object to use to generate the string representation
	 * @param relative The Date object to compare against
	 * @return A string explaining how far behind or ahead the date is from relative.
	 */
	public static String format(Date time, Date relative) {
		Calendar timeCal = Calendar.getInstance();
		timeCal.setTime(time);
		Calendar relCal = Calendar.getInstance();
		relCal.setTime(relative);

		// Compute the difference between these two dates in milliseconds, but
		// first account for any timezone differences between the two dates.
		long timeMillis = timeCal.getTimeInMillis();
		timeMillis += timeCal.getTimeZone().getOffset(timeMillis);
		long relMillis = relCal.getTimeInMillis();
		relMillis += relCal.getTimeZone().getOffset(relMillis);
		long diff = timeMillis - relMillis;
		String dir = (diff < 0)? "ago":"from now";
		diff = Math.abs(diff);

		if(diff / MILLIS_PER_MIN < 0.2) {
			return "within seconds";
		}
		if(diff / MILLIS_PER_MIN < 0.8) {
			return "less than a minute "+dir;
		}
		if(diff / MILLIS_PER_MIN < 1.5) {
			return "about a minute "+dir;
		}
		if(diff / MILLIS_PER_HR < 0.9) {
			return new Integer(Math.round(diff / MILLIS_PER_MIN)).toString()+" minutes "+dir;
		}
		if(diff / MILLIS_PER_HR < 1.2) {
			return "about an hour "+dir;
		}
		if(diff / MILLIS_PER_HR < 1.9) {
			return "more than an hour "+dir;
		}
		if(diff / MILLIS_PER_DAY < 0.8) {
			return new Integer(Math.round(diff / MILLIS_PER_HR)).toString()+" hours "+dir;
		}
		if(diff / MILLIS_PER_DAY < 1.2) {
			return "about a day "+dir;
		}
		if(diff / MILLIS_PER_DAY < 1.9) {
			return "more than a day "+dir;
		}
		if(diff / MILLIS_PER_WEEK < 1) {
			return new Integer(Math.round(diff / MILLIS_PER_DAY)).toString()+" days "+dir;
		}
		if(diff / MILLIS_PER_WEEK < 1.5) {
			return "about a week "+dir;
		}
		if(diff / MILLIS_PER_WEEK < 7) {
			return new Integer(Math.round(diff / MILLIS_PER_WEEK)).toString()+" weeks "+dir;
		}

		// Just give the raw date
		return DateFormat.getDateInstance().format(time);

	}
}