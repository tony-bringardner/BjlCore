package us.bringardner.core.util;

/**
 * <PRE>
 * Hexadecimal encoding and decoding, e.g. for digests, fingerprints and DNS record data.
 *
 * java.util.HexFormat does this from Java 17; bjl_core runs on Java 11. These methods also avoid
 * String.format("%02x") per byte, which is slow.
 *
 * Copyright 1998-2026 <A href="http://bringardner.us/tony">Tony Bringardner</A>
 *
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *
 *  @author Tony Bringardner
 */
public final class Hex {

	private static final char[] LOWER = "0123456789abcdef".toCharArray();
	private static final char[] UPPER = "0123456789ABCDEF".toCharArray();

	private Hex() {
	}

	/**
	 * @param data the bytes
	 * @return two lower case hex digits per byte, e.g. "0aff"
	 */
	public static String encode(byte[] data) {
		return encode(data, 0, data.length, false, null);
	}

	/**
	 * @param data the bytes
	 * @return two upper case hex digits per byte, e.g. "0AFF"
	 */
	public static String encodeUpper(byte[] data) {
		return encode(data, 0, data.length, true, null);
	}

	/**
	 * @param data the bytes
	 * @param upperCase true for A-F, false for a-f
	 * @param separator put between bytes, e.g. ":" for a fingerprint ("0A:FF"); null or "" for none
	 * @return the hex text
	 */
	public static String encode(byte[] data, boolean upperCase, String separator) {
		return encode(data, 0, data.length, upperCase, separator);
	}

	/**
	 * @param data the bytes
	 * @param off the first byte
	 * @param len how many bytes
	 * @param upperCase true for A-F, false for a-f
	 * @param separator put between bytes; null or "" for none
	 * @return the hex text
	 * @throws IndexOutOfBoundsException if off and len are not inside data
	 */
	public static String encode(byte[] data, int off, int len, boolean upperCase, String separator) {
		java.util.Objects.checkFromIndexSize(off, len, data.length);
		char[] digits = upperCase ? UPPER : LOWER;
		boolean sep = separator != null && !separator.isEmpty();
		StringBuilder ret = new StringBuilder(len * (2 + (sep ? separator.length() : 0)));
		for(int i = off; i < off + len; i++ ) {
			if( sep && i > off ) {
				ret.append(separator);
			}
			int b = data[i] & 0xff;
			ret.append(digits[b >>> 4]).append(digits[b & 0x0f]);
		}
		return ret.toString();
	}

	/**
	 * @param hex hex digits (either case), two per byte, with no separators; may be empty
	 * @return the bytes
	 * @throws IllegalArgumentException if hex has an odd length or a character that isn't a hex digit
	 */
	public static byte[] decode(CharSequence hex) {
		int len = hex.length();
		if( len % 2 != 0 ) {
			throw new IllegalArgumentException("Hex text has an odd number of digits: "+len);
		}
		byte[] ret = new byte[len / 2];
		for(int i = 0; i < ret.length; i++ ) {
			ret[i] = (byte) ((digit(hex, 2*i) << 4) | digit(hex, 2*i + 1));
		}
		return ret;
	}

	private static int digit(CharSequence hex, int i) {
		int d = Character.digit(hex.charAt(i), 16);
		if( d < 0 ) {
			throw new IllegalArgumentException("Not a hex digit at "+i+": '"+hex.charAt(i)+"'");
		}
		return d;
	}
}
