package divestoclimb.android.util;

import android.text.InputFilter;
import android.text.Spanned;

public class ValidFilenameFilter implements InputFilter {

	public CharSequence filter(CharSequence source, int start, int end,
			Spanned dest, int dstart, int dend) {
		String valid = "";
		for(int i = start; i < end; i++) {
			char c = source.charAt(i);
			// These characters are the ones Windows doesn't
			// like, which appears to be the most restrictive
			// set. http://support.microsoft.com/kb/177506
			switch(c) {
			case '/':
			case '\\':
			case ':':
				c = '-';
				break;
			case '"':
				c = '\'';
				break;
			case '*':
			case '?':
			case '<':
			case '>':
			case '|':
				c = '_';
			}
			valid = valid.concat(String.valueOf(c));
		}
		return valid;
	}

}
