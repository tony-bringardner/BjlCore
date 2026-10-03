package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;

import us.bringardner.core.util.AddressMatcher;

/**
 * AddressMatcher: address and network (CIDR) lists, address literals only.
 */
public class TestAddressMatcher {

	private static InetAddress ip(String s) throws Exception {
		return InetAddress.getByName(s);
	}

	@Test
	public void testMatches() throws Exception {
		AddressMatcher m = AddressMatcher.parse("192.0.2.10, 10.0.0.0/8 2001:db8::/32,::1");
		assertTrue(m.matches(ip("192.0.2.10")));
		assertFalse(m.matches(ip("192.0.2.11")));
		assertTrue(m.matches(ip("10.200.3.4")));
		assertTrue(m.matches(ip("2001:db8:1::5")));
		assertFalse(m.matches(ip("2001:db9::5")));
		assertTrue(m.matches(ip("::1")));
		assertFalse(m.matches(null));
		assertEquals("192.0.2.10, 10.0.0.0/8 2001:db8::/32,::1", m.toString());

		//  Prefixes that don't end on a byte boundary
		assertFalse(AddressMatcher.parse("172.16.0.0/12").matches(ip("172.32.0.1")));
		assertTrue(AddressMatcher.parse("172.16.0.0/12").matches(ip("172.31.255.255")));
		assertTrue(AddressMatcher.parse("192.168.1.128/25").matches(ip("192.168.1.200")));
		assertFalse(AddressMatcher.parse("192.168.1.128/25").matches(ip("192.168.1.127")));
	}

	@Test
	public void testEmpty() throws Exception {
		assertSame(AddressMatcher.NONE, AddressMatcher.parse(null));
		assertSame(AddressMatcher.NONE, AddressMatcher.parse("  "));
		assertTrue(AddressMatcher.NONE.isEmpty());
		assertFalse(AddressMatcher.NONE.matches(ip("127.0.0.1")));
	}

	@Test
	public void testZeroPrefixMatchesOneFamilyOnly() throws Exception {
		AddressMatcher v4 = AddressMatcher.parse("0.0.0.0/0");
		assertTrue(v4.matches(ip("203.0.113.9")));
		assertFalse(v4.matches(ip("2001:db8::1")), "An IPv4 network never matches an IPv6 address");
		AddressMatcher v6 = AddressMatcher.parse("::/0");
		assertTrue(v6.matches(ip("2001:db8::1")));
		assertFalse(v6.matches(ip("203.0.113.9")));
	}

	@Test
	public void testRejectsBadEntries() {
		for(String bad : new String[] {
				"example.com",          // a host name: never looked up
				"localhost",
				"1.2.3.999",            // InetAddress.getByName would look this up as a host name
				"1.2.3",
				"10.0.0.0/33",          // longer than the address
				"2001:db8::/129",
				"10.0.0.0/-1",          // negative: must not match everything
				"10.0.0.0/",
				"10.0.0.0/x",
				"10.0.0.0/1000",
				"::g",
		}) {
			assertThrows(IllegalArgumentException.class, () -> AddressMatcher.parse(bad), bad);
		}
	}

	@Test
	public void testIsAddressLiteral() {
		assertTrue(AddressMatcher.isAddressLiteral("127.0.0.1"));
		assertTrue(AddressMatcher.isAddressLiteral(" ::1 "));
		assertTrue(AddressMatcher.isAddressLiteral("2001:db8::1"));
		assertFalse(AddressMatcher.isAddressLiteral("localhost"));
		assertFalse(AddressMatcher.isAddressLiteral("mail.example.com"));
		assertFalse(AddressMatcher.isAddressLiteral("1.2.3.256"));
		assertFalse(AddressMatcher.isAddressLiteral(null));
	}

	@Test
	public void testAnd() throws Exception {
		AddressMatcher a = AddressMatcher.parse("10.0.0.0/8");
		AddressMatcher b = AddressMatcher.parse("::1");
		AddressMatcher both = a.and(b);
		assertTrue(both.matches(ip("10.1.1.1")));
		assertTrue(both.matches(ip("::1")));
		assertFalse(a.matches(ip("::1")), "and() doesn't change either list");
		assertSame(a, a.and(AddressMatcher.NONE));
		assertSame(b, AddressMatcher.NONE.and(b));
		assertSame(a, a.and(null));
	}
}
