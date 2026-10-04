package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import us.bringardner.core.util.Hex;

/**
 * Hex gives exactly what the String.format("%02x") loops it replaces gave.
 */
public class TestHex {

	private static byte[] allBytes() {
		byte[] ret = new byte[256];
		for(int i=0; i < 256; i++ ) {
			ret[i] = (byte) i;
		}
		return ret;
	}

	@Test
	public void testSameAsStringFormat() {
		byte[] all = allBytes();
		StringBuilder lower = new StringBuilder();
		StringBuilder upper = new StringBuilder();
		StringBuilder colons = new StringBuilder();
		for(byte b : all) {
			lower.append(String.format("%02x", b & 0xff));
			upper.append(String.format("%02X", b & 0xff));
			colons.append(colons.length() > 0 ? ":" : "").append(String.format("%02X", b));
		}
		assertEquals(lower.toString(), Hex.encode(all));
		assertEquals(upper.toString(), Hex.encodeUpper(all));
		assertEquals(colons.toString(), Hex.encode(all, true, ":"));
	}

	@Test
	public void testRangesAndEmpty() {
		byte[] d = {0x0a, (byte) 0xff, 0x10, 0x7f};
		assertEquals("ff10", Hex.encode(d, 1, 2, false, null));
		assertEquals("0a-ff-10-7f", Hex.encode(d, false, "-"));
		assertEquals("", Hex.encode(new byte[0]));
		assertEquals("", Hex.encode(d, 4, 0, true, ":"));
		assertThrows(IndexOutOfBoundsException.class, () -> Hex.encode(d, 3, 2, false, null));
	}

	@Test
	public void testDecode() {
		byte[] all = allBytes();
		assertArrayEquals(all, Hex.decode(Hex.encode(all)));
		assertArrayEquals(all, Hex.decode(Hex.encodeUpper(all)));
		assertArrayEquals(new byte[0], Hex.decode(""));
		assertThrows(IllegalArgumentException.class, () -> Hex.decode("abc"), "odd length");
		assertThrows(IllegalArgumentException.class, () -> Hex.decode("zz"), "not hex");
		assertThrows(IllegalArgumentException.class, () -> Hex.decode("0a:ff"), "separators aren't accepted");
	}
}
