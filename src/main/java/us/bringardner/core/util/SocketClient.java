/**
 * <PRE>
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
 *   
 *	@author Tony Bringardner   
 *
 *
 * ~version~V000.01.02-V000.00.01-V000.00.00-
 */
package us.bringardner.core.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import us.bringardner.core.SecureBaseObject;


public class SocketClient extends SecureBaseObject {


	public static final String PROPERTY_SOCKET_TIMEOUT = "SocketTimeout";

	public static final int DEFAULT_SOCKET_TIMEOUT = 60000;

	public static final String PROPERTY_SO_LINGER = "SoLinger";

	/**
	 * SO_LINGER is in SECONDS (see Socket.setSoLinger).
	 * This was 60000 (almost 17 hours) which could block Socket.close() for a very long time.
	 */
	public static final int DEFAULT_SO_LINGER = 10;

	public static final String PROPERTY_IS_SO_LINGER = "IsSoLinger";

	/** How long (in milliseconds) {@link #getSocket(String, int)} waits for the connection to be made. */
	public static final String PROPERTY_CONNECT_TIMEOUT = "ConnectTimeout";

	/**
	 * 60 seconds. Before this setting existed there was no limit, so an unreachable host
	 * blocked for as long as the operating system kept trying (about 75 seconds on macOS,
	 * several minutes on Linux). 0 means no limit.
	 */
	public static final int DEFAULT_CONNECT_TIMEOUT = 60000;

	/** "false" turns off the host name check for secure connections, see {@link #isVerifyHostname()}. */
	public static final String PROPERTY_VERIFY_HOSTNAME = "VerifyHostname";


	private volatile int lingerTime=-1;

	//  null means the VerifyHostname property has not been read yet
	private volatile Boolean verifyHostname;

	private volatile int socketTimeout=-1;

	private volatile int connectTimeout=-1;

	//  null means the IsSoLinger property has not been read yet
	private volatile Boolean isSoLinger;


	private volatile SocketFactory factory;
	
	public SocketClient() {
		this(false);
	}
	
	public SocketClient(boolean useSSL) {
		setSecure(useSSL);
	}


	/**
	 * When secure, the SSL sockets this factory makes check that the server's certificate
	 * was issued for the host being connected to, unless {@link #isVerifyHostname()} is false.
	 *
	 * @return the SocketFactory that a client should use to connect to a server
	 *
	 * @throws KeyManagementException
	 * @throws CertificateException
	 * @throws FileNotFoundException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws IOException
	 */
	public SocketFactory getSocketFactory() throws KeyManagementException, CertificateException, FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException {
		if( factory == null ) {
			synchronized(this) {
				if( factory == null ) {
					if( isSecure() ) {
						SSLSocketFactory sf = getSSLContext().getSocketFactory();
						factory = isVerifyHostname() ? new HostnameVerifyingFactory(sf) : sf;
					} else {
						factory = SocketFactory.getDefault();
					}
				}
			}
		}

		return factory;
	}

	/**
	 * @return true (the default) if secure connections check that the server's certificate was
	 *  issued for the host name (or address) being connected to. Without this check any certificate
	 *  the trust managers accept is accepted for every host, so a server with any trusted
	 *  certificate could pretend to be another.
	 */
	public boolean isVerifyHostname() {
		Boolean ret = verifyHostname;
		if( ret == null ) {
			ret = getBooleanProperty(PROPERTY_VERIFY_HOSTNAME, true);
			verifyHostname = ret;
		}
		return ret;
	}

	/**
	 * Turn the host name check for secure connections on or off. Turn it off only for servers
	 * whose certificate is known not to match the name used to reach them (a test certificate, say).
	 *
	 * @param verifyHostname
	 */
	public void setVerifyHostname(boolean verifyHostname) {
		this.verifyHostname = verifyHostname;
		//  The factory depends on it
		factory = null;
	}

	/**
	 * Turns on the HTTPS host name check (RFC 2818) for every socket the wrapped factory makes,
	 * including connected sockets (the TLS handshake doesn't start until the socket is used).
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
	

	/**
	 * Create a socket connected to the host:port and configured with the appropriate timeout values.
	 * The connection attempt gives up after {@link #getConnectTimeout()} milliseconds
	 * (with a SocketTimeoutException). If anything fails the socket is closed before the exception is thrown.
	 *
	 * @param host
	 * @param port
	 * @return a connected socket
	 * @throws KeyManagementException
	 * @throws UnrecoverableKeyException
	 * @throws UnknownHostException
	 * @throws CertificateException
	 * @throws FileNotFoundException
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public Socket getSocket(String host,int port) throws KeyManagementException, UnrecoverableKeyException, UnknownHostException, CertificateException, FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, IOException {
		SocketFactory sf = getSocketFactory();
		Socket ret = null;
		try {
			try {
				ret = sf.createSocket();
			} catch (SocketException | UnsupportedOperationException e) {
				//  Some custom factories can't create unconnected sockets, connect without a timeout.
				ret = null;
			}
			if( ret != null ) {
				ret.connect(new InetSocketAddress(host, port), getConnectTimeout());
			} else {
				ret = sf.createSocket(host, port);
			}
			configure(ret);
			return ret;
		} catch (IOException | RuntimeException e) {
			if( ret != null ) {
				try {
					ret.close();
				} catch (IOException e2) {
				}
			}
			throw e;
		}
	}

	/**
	 * @return how long (in milliseconds) to wait for a connection to be made. 0 means no limit.
	 */
	public int getConnectTimeout() {
		if( connectTimeout < 0 ) {
			synchronized(this) {
				if( connectTimeout < 0 ) {
					connectTimeout = getIntProperty(PROPERTY_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
				}
			}
		}

		return connectTimeout;
	}

	/**
	 * @param value how long (in milliseconds) to wait for a connection to be made. 0 means no limit.
	 */
	public void setConnectTimeout(int value) {
		connectTimeout = value;
	}
	

	@Override
	protected void resetSecurityContext() {
		super.resetSecurityContext();
		//  The factory was created from the old SSLContext
		factory = null;
	}


	/**
	 * @return the time to linger on a Socket.close()
	 * @see Socket#setSoLinger(boolean on, int linger)

	 */
	public int getLingerTime() {
		if( lingerTime < 0 ) {
			synchronized(this) {
				if( lingerTime < 0 ) {
					lingerTime = getIntProperty(PROPERTY_SO_LINGER,DEFAULT_SO_LINGER);
				}
			}
		}

		return lingerTime;
	}

	/**
	 * 
	 * @param lingerTime
	 * @see Socket#setSoLinger(boolean on, int linger)
	 */
	public void setLingerTime(int lingerTime) {
		this.lingerTime = lingerTime;
	}

	/**
	 * Configure a newly accepted Socket.
	 * By default SoTimeout and SoLinger are set based on current configuration.  
	 *  
	 * @param socket
	 * @throws SocketException
	 */
	public void configure(Socket socket) throws SocketException {
		socket.setSoTimeout(getSocketTimeout());

		if( isSoLinger() ) {
			socket.setSoLinger(true, getLingerTime());
		}		
	}

	/**
	 * @return true is SoLInger should be enabled for newly accepted Sockets.
	 */
	public boolean isSoLinger() {
		Boolean ret = isSoLinger;
		if( ret == null ) {
			ret = getBooleanProperty(PROPERTY_IS_SO_LINGER, false);
			isSoLinger = ret;
		}
		return ret;
	}

	/**
	 * Set to true will enable SoLinger for newly accepted Sockets.
	 * 
	 * @param isLinger 
	 */
	public void setSoLinger(boolean isLinger) {
		this.isSoLinger = isLinger;
	}


	/**
	 * Set the timeout value used to initialize all newly created Sockets.
	 * This will control the timeout of client read and write operations.
	 * 
	 * @param value
	 */
	public void setSocketTimeout(int value) {
		socketTimeout = value;
	}

	/**
	 * @return The timeout value used to initialize all newly created Sockets.
	 * This will control the timeout of client read and write operations. 
	 */
	public int getSocketTimeout() {
		if( socketTimeout < 0 ) {
			synchronized(this) {
				if( socketTimeout < 0 ) {
					socketTimeout = getIntProperty(PROPERTY_SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
				}
			}
		}

		return socketTimeout;
	}


}
