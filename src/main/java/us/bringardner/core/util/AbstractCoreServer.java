// ~version~V000.01.02-V000.00.01-V000.00.00-
package us.bringardner.core.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;

import us.bringardner.core.BaseThread;


/**
 * <PRE>
 * This class is a general purpose TCP/IP Server.
 * It implements all of the functionality required to accepts connection 
 * requests for both secure and insecure channels and establishes a 
 * framework for managing sessions.
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
 *  @author Tony Bringardner   
 *
 */
public abstract class AbstractCoreServer extends BaseThread  {

	
	public static final int DEFAULT_PORT = 9200;

	private static final String PROPERTY_PORT = "Port";

	public static final String PROPERTY_ACCEPT_TIMEOUT = "AcceptTimeout";

	public static final int DEFAULT_ACCEPT_TIMEOUT = 60000;

	public static final String PROPERTY_SOCKET_TIMEOUT = "SocketTimeout";

	public static final int DEFAULT_SOCKET_TIMEOUT = 60000;

	public static final String PROPERTY_BACKLOG = "Backlog";

	public static final int DEFAULT_BACKLOG = 0;

	public static final String PROPERTY_SO_LINGER = "SoLinger";

	/**
	 * SO_LINGER is in SECONDS (see Socket.setSoLinger).
	 * This was 60000 (almost 17 hours) which could block Socket.close() for a very long time.
	 */
	public static final int DEFAULT_SO_LINGER = 10;

	public static final String PROPERTY_IS_SO_LINGER = "IsSoLinger";

	public static final String PROPERTY_BIND_ADDRESS = "BindAddress";

	private volatile ServerSocketFactory factory;
	
	
	private volatile int port=-1;

	//  Attributes may be accessed by many connection threads at the same time.
	private volatile Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private volatile boolean needClientAuth=false;

	private volatile int acceptTimeout=-1;

	private volatile int lingerTime=-1;

	private volatile int socketTimeout=-1;

	private volatile int backlog = -1;

	private volatile InetAddress bindAddr;
	//  The bind address may legitimately be null so we need a separate flag to know if the property has been read.
	private volatile boolean bindAddrConfigured = false;

	//  null means the IsSoLinger property has not been read yet
	private volatile Boolean isSoLinger;

	private volatile ServerSocket serverSocket;
	
	
	
	public AbstractCoreServer(boolean secure) {
		setSecure(secure);
	}

	public AbstractCoreServer(int port, boolean secure) {
		this(port);
		setSecure(secure);
	}

	public AbstractCoreServer(int port) {
		this();
		setPort(port);
	}

	public AbstractCoreServer() {
		super();
	}

	@Override
	protected void init() {
		super.init();
		String tmp = null;

		if( (tmp = getProperty(PROPERTY_IS_SO_LINGER)) != null ) {
			isSoLinger = tmp.trim().equalsIgnoreCase("true");
		}
		
		if( (tmp = getProperty(PROPERTY_BIND_ADDRESS)) != null ) {
			try {
				bindAddr = InetAddress.getByName(tmp.trim());
				bindAddrConfigured = true;
			} catch (UnknownHostException e) {
				logError("Cannot create BindAddress.",e);
				throw new IllegalStateException(e);
			}
		}
	}
	
	/**
	 * @return ServerSocket used by this Server
	 * 
	 * @throws IOException
	 */
	public ServerSocket getServerSocket() throws IOException {
		if( serverSocket == null ) {
			synchronized (this) {
				if( serverSocket == null ) {
					ServerSocket svr = getServerSocketFactory().createServerSocket(getPort(),getBacklog(),getBindAddr());
					svr.setSoTimeout(getAcceptTimeout());
					if( svr instanceof SSLServerSocket ) {
						((SSLServerSocket)svr).setNeedClientAuth(isNeedClientAuth());
					}
					serverSocket = svr;
				}
			}
		}
		
		return serverSocket;
	}

	
	/**
	 * @return the backlog used to create the ServerSocket.
	 * 
	 * @see java.net.ServerSocket   
	 */
	public int getBacklog() {
		if( backlog < 0 ) {
			synchronized(this) {
				if( backlog < 0 ) {
					backlog = getIntProperty(PROPERTY_BACKLOG,DEFAULT_BACKLOG);
				}
			}
		}
		
		return backlog;
	}

	/**
	 * set the listen backlog for the server
	 *  
	 * @param backlog
	 * @see java.net.ServerSocket
	 */
	public void setBacklog(int backlog) {
		this.backlog = backlog;
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
	 * @return the local InetAddress the server will bind to 
	 * @see java.net.ServerSocket 
	 */
	public InetAddress getBindAddr() {
		//  The bind address may be null (all local addresses) so we use a flag to know if the property has been read.
		if( !bindAddrConfigured ) {
			synchronized (this) {
				if( !bindAddrConfigured ) {
					String tmp = getProperty(PROPERTY_BIND_ADDRESS);
					if( tmp != null ) {
						try {
							bindAddr = InetAddress.getByName(tmp.trim());
						} catch (UnknownHostException e) {
							logError("Cannot create BindAddress ("+tmp+").",e);
							throw new IllegalStateException(e);
						}
					}
					bindAddrConfigured = true;
				}
			}
		}
		return bindAddr;
	}

	/**
	 * set the local InetAddress the server will bind to
	 * 
	 * @param bindAddr
	 * @see java.net.ServerSocket
	 */
	public void setBindAddr(InetAddress bindAddr) {
		this.bindAddr = bindAddr;
		this.bindAddrConfigured = true;
	}

	/**
	 * 
	 * @param needClientAuth true is client authentication is required on a secure connection.
	 */
	public void setNeedClientAuth(boolean needClientAuth) {
		this.needClientAuth = needClientAuth;
	}

	/**
	 * @return true is client authentication is required on a secure connection.
	 */
	public boolean isNeedClientAuth() {		
		return needClientAuth;
	}


	/**
	 * 
	 * @return the attributes maintained by this server.
	 */
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	/**
	 * @param attributes replace the server attributes.
	 */
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}


	
	/**
	 * @return the ServerSocketFactory that this server should use to listen for connections.
	 * 
	 * @throws IOException
	 */
	public ServerSocketFactory getServerSocketFactory() throws IOException {
		if( factory == null ) {
			synchronized(this) {
				if( factory == null ) {
					if( isSecure() ) {
						factory = getSSLContext().getServerSocketFactory();						
					} else {
						factory = ServerSocketFactory.getDefault();
					}
				}
			}
		}
		
		return factory;
	}

	/**
	 * @param factory to use when creating the ServerSocket 
	 */
	public void setServerSocketFactory(ServerSocketFactory factory) {
		this.factory = factory;
	}

	@Override
	protected void resetSecurityContext() {
		super.resetSecurityContext();
		//  The factory was created from the old SSLContext
		factory = null;
	}


	/**
	 * The Server maintains a set of arbitrary values called attributes.  
	 * The attributes may be set or retrieved by any clients via the IProcessor api.
	 * 
	 * @param name
	 * @return the Object associated with the named attribute.
	 */
	public Object getAttribute(String name) {
		return name == null ? null : attributes.get(name);
	}

	/**
	 * The Server maintains a set of arbitrary values called attributes.  
	 * The attributes may be set or retrieved by any clients via the IProcessor api.
	 *  
	 * @param name
	 * @param attribute
	 */
	public void setAttribute(String name, Object attribute) {
		if( attribute == null ) {
			//  ConcurrentHashMap does not allow null values, setting null is the same as removing it.
			removeAttribute(name);
		} else {
			attributes.put(name, attribute);
		}
	}

	/**
	 * The Server maintains a set of arbitrary values called attributes.  
	 * The attributes may be set or retrieved by any clients via the IProcessor api.
	 * 
	 * @param name
	 * @return the values of the attribute that was removed or null in none existed.
	 */
	public Object removeAttribute(String name) {
		return name == null ? null : attributes.remove(name);
	}

	/**
	 * @return the port that the server should listen on for connection requests.
	 */
	public int getPort() {
		if( port == -1 ) {
			port = getIntProperty(PROPERTY_PORT,DEFAULT_PORT);
		}
		return port;
	}


	/**
	 * @param port that the server should listen on for connection requests.
	 */
	public void setPort(int port) {
		this.port = port;
	}



	/**
	 * Called by processors and/or connections to notify the server that a connection was closed
	 * 
	 * @param connectionObject
	 */
	public void connectionClosed(Object connectionObject) {
		//  Nothing to do is core server
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

	/**
	 * Set the timeout value used for the accept call
	 * @param value
	 */
	public void setAcceptTimeout(int value) {
		acceptTimeout = value;
	}
	
	/**
	 * @return the timeout value for ServerSocket.accept
	 * @see ServerSocket#accept()
	 */
	public int getAcceptTimeout() {
		if( acceptTimeout < 0 ) {
			synchronized(this) {
				if(acceptTimeout < 0 ) {
					acceptTimeout = getIntProperty(PROPERTY_ACCEPT_TIMEOUT, DEFAULT_ACCEPT_TIMEOUT);
				}
			}
		}
		
		return acceptTimeout;
	}

	
	

}
