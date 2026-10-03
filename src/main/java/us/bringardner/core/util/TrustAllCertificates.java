package us.bringardner.core.util;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * <PRE>
 * A trust manager that accepts EVERY certificate: expired, self signed, issued for another host
 * or by nobody anyone trusts. A connection that uses it is encrypted but not authenticated, so
 * anyone able to intercept it can read and change it.
 *
 * Only for cases where that is the intent: opportunistic TLS (SMTP STARTTLS between mail servers,
 * which falls back to plain text anyway), or a test against a server with a throw away certificate.
 * It has one well known name so these uses are easy to find; don't write another.
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
public final class TrustAllCertificates implements X509TrustManager {

	/** The one instance (it has no state). */
	public static final TrustAllCertificates INSTANCE = new TrustAllCertificates();

	private TrustAllCertificates() {
	}

	/**
	 * @return a new array holding the trust all manager, for SSLContext.init or SecureBaseObject.setTrustManagers
	 */
	public static TrustManager[] trustManagers() {
		return new TrustManager[] {INSTANCE};
	}

	/**
	 * @param protocol the SSLContext protocol, e.g. "TLS"
	 * @return a new SSLContext that accepts every server certificate (no key managers)
	 * @throws GeneralSecurityException if the protocol isn't available
	 */
	public static SSLContext sslContext(String protocol) throws GeneralSecurityException {
		SSLContext ret = SSLContext.getInstance(protocol);
		ret.init(null, trustManagers(), null);
		return ret;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) {
		//  Accepts everything, see the class comment
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) {
		//  Accepts everything, see the class comment
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}
}
