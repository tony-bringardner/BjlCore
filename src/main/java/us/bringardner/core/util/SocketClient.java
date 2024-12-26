/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.us/tony">Tony Bringardner</A>
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
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.SocketFactory;

import us.bringardner.core.SecureBaseObject;


public class SocketClient extends SecureBaseObject {


	public static final String PROPERTY_SOCKET_TIMEOUT = "SocketTimeout";

	public static final int DEFAULT_SOCKET_TIMEOUT = 60000;

	public static final String PROPERTY_SO_LINGER = "SoLinger";

	public static final int DEFAULT_SO_LINGER = 60000;

	public static final String PROPERTY_IS_SO_LINGER = "IsSoLinger";


	private int lingerTime=-1;

	private int socketTimeout=-1;

	private boolean isSoLinger;


	private volatile SocketFactory factory;
	
	public SocketClient() {
		this(false);
	}
	
	public SocketClient(boolean useSSL) {
		setSecure(useSSL);
	}


	/**
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
						factory = getSSLContext().getSocketFactory();						
					} else {
						factory = SocketFactory.getDefault();
					}
				}
			}
		}
		
		return factory;
	}
	

	/**
	 * Create a socket connected to the host:port and configured with the appropriate timeout values
	 * @param host
	 * @param port
	 * @return
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
		Socket ret = getSocketFactory().createSocket(host, port);
		configure(ret);
		return ret;
	}
	

	@Override
	protected void init() {
		super.init();
		String tmp = null;

		if( (tmp = getProperty(PROPERTY_IS_SO_LINGER)) != null ) {
			isSoLinger = tmp.toLowerCase().equals("true");
		}

	}

	/**
	 * @return the time to linger on a Socket.close()
	 * @see Socket#setSoLinger(boolean on, int linger)

	 */
	public int getLingerTime() {
		if( lingerTime < 0 ) {
			synchronized(this) {
				if( lingerTime < 0 ) {
					lingerTime = Integer.parseInt(getProperty(PROPERTY_SO_LINGER,""+DEFAULT_SO_LINGER));
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
		return isSoLinger;
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
					socketTimeout = Integer.parseInt(getProperty(PROPERTY_SOCKET_TIMEOUT, ""+DEFAULT_SOCKET_TIMEOUT));
				}
			}
		}

		return socketTimeout;
	}


}
