package us.bringardner.core.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * <PRE>
 * Client and server TLS set up shared by the BJL protocol clients and servers, so they all
 * check certificates the same way.
 *
 * {@link #layer} puts TLS on a connected socket (STARTTLS, FTP's AUTH TLS ...),
 * {@link #configureClient} applies the client settings (SNI and host name verification) to an
 * SSLSocket before its handshake, and {@link #hostnameVerifying} wraps a factory so every
 * socket it makes verifies the host name.
 *
 * Host name verification is the HTTPS check (RFC 2818 / RFC 6125): the server's certificate must
 * have been issued for the host being connected to. Without it any certificate the trust managers
 * accept is accepted for every host, so a server with any trusted certificate could pretend to be another.
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
public final class TlsSockets {

	private TlsSockets() {
	}

	/**
	 * Put TLS on a connected socket. The handshake has not been done, so settings such as the
	 * enabled protocols can still be made; it happens on {@link SSLSocket#startHandshake()} or the
	 * first read or write.
	 *
	 * @param ctx the SSLContext to use
	 * @param plain a connected socket
	 * @param host the peer's host name (client mode, for SNI and host name verification); ignored in server mode
	 * @param clientMode true for the client end of the connection
	 * @param verifyHostname true (client mode) to check that the server's certificate was issued for host
	 * @param autoClose true if closing the TLS socket also closes plain
	 * @return the TLS socket
	 * @throws IOException if the socket can't be created; plain is left open
	 */
	public static SSLSocket layer(SSLContext ctx, Socket plain, String host, boolean clientMode,
			boolean verifyHostname, boolean autoClose) throws IOException {
		String peer = clientMode ? host : null;
		Socket s = ctx.getSocketFactory().createSocket(plain, peer, plain.getPort(), autoClose);
		if( !(s instanceof SSLSocket) ) {
			throw new IOException("TLS socket factory returned "+s.getClass().getName());
		}
		SSLSocket ret = (SSLSocket) s;
		ret.setUseClientMode(clientMode);
		if( clientMode ) {
			configureClient(ret, host, verifyHostname);
		}
		return ret;
	}

	/**
	 * Apply the client settings to an SSLSocket before its handshake: the server name (SNI) when
	 * host is a name, not an address, and, with verifyHostname, the host name check.
	 *
	 * @param socket a client side SSLSocket (before the handshake)
	 * @param host the host being connected to (may be null: then neither is set)
	 * @param verifyHostname true to check that the server's certificate was issued for host
	 * @throws IllegalArgumentException if verifyHostname is true and host is null, so a missing
	 *  host can't quietly turn the check off
	 */
	public static void configureClient(SSLSocket socket, String host, boolean verifyHostname) {
		if( host == null || host.trim().isEmpty() ) {
			if( verifyHostname ) {
				throw new IllegalArgumentException("A host name is needed to verify the server's certificate");
			}
			return;
		}
		host = host.trim();
		SSLParameters params = socket.getSSLParameters();
		if( verifyHostname ) {
			params.setEndpointIdentificationAlgorithm("HTTPS");
		}
		if( !AddressMatcher.isAddressLiteral(host) ) {
			try {
				params.setServerNames(Collections.singletonList(new SNIHostName(host)));
			} catch (IllegalArgumentException e) {
				//  Not a valid SNI name (e.g. it has an underscore): connect without SNI
			}
		}
		socket.setSSLParameters(params);
	}

	/**
	 * @param factory the factory to wrap
	 * @return a factory whose client sockets verify the server's host name (the HTTPS check). A
	 *  socket created unconnected verifies the host it is later connected to.
	 */
	public static SSLSocketFactory hostnameVerifying(SSLSocketFactory factory) {
		return factory instanceof HostnameVerifyingFactory ? factory : new HostnameVerifyingFactory(factory);
	}

	/**
	 * Turns on the HTTPS host name check for every socket the wrapped factory makes,
	 * including unconnected sockets (the TLS handshake doesn't start until the socket is used).
	 */
	private static final class HostnameVerifyingFactory extends SSLSocketFactory {
		private final SSLSocketFactory delegate;

		HostnameVerifyingFactory(SSLSocketFactory delegate) {
			this.delegate = delegate;
		}

		private static Socket verify(Socket socket) {
			if( socket instanceof SSLSocket ) {
				SSLSocket ssl = (SSLSocket) socket;
				SSLParameters params = ssl.getSSLParameters();
				params.setEndpointIdentificationAlgorithm("HTTPS");
				ssl.setSSLParameters(params);
			}
			return socket;
		}

		@Override
		public String[] getDefaultCipherSuites() {
			return delegate.getDefaultCipherSuites();
		}

		@Override
		public String[] getSupportedCipherSuites() {
			return delegate.getSupportedCipherSuites();
		}

		@Override
		public Socket createSocket() throws IOException {
			return verify(delegate.createSocket());
		}

		@Override
		public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
			return verify(delegate.createSocket(s, host, port, autoClose));
		}

		@Override
		public Socket createSocket(String host, int port) throws IOException {
			return verify(delegate.createSocket(host, port));
		}

		@Override
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
			return verify(delegate.createSocket(host, port, localHost, localPort));
		}

		@Override
		public Socket createSocket(InetAddress host, int port) throws IOException {
			return verify(delegate.createSocket(host, port));
		}

		@Override
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
			return verify(delegate.createSocket(address, port, localAddress, localPort));
		}
	}
}
