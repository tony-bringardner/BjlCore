// ~version~V000.01.03
package us.bringardner.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

/**
 * <PRE>
 * This class can be used to implement both clients and servers that 
 * support secure connections. 
 * 
 * When isSecure() returns false the getSSLContext()
 * method is not called and none of the attributes (fields) are required.
 * 
 * When isSecure() returns true;
 * 	A Server will typically required most if not all of the attributes to
 * 		be properly configured.
 * 
 *  A Client typically requires only the protocol to be properly configured.
 * 
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
 *	@author Tony Bringardner   
 *
 *
 */
public class SecureBaseObject extends BaseObject {
	public static final String PROPERTY_KEY_STORE_NAME = "KeyStoreName";
	public static final String PROPERTY_PASS_PHRASE = "KeyStorePassword";
	public static final String PROPERTY_KEY_STORE_TYPE = "KeyStoreType";
	public static final String PROPERTY_ALGORITHM = "Algorithm";
	public static final String PROPERTY_PROTOCOL = "Protocol";
	public static final String PROPERTY_SECURE = "secure";
	public static final String PROTOCOL_TLS = "TLS";	
	/**
	 * System property that forces a TLS version (for example TLSv1.2) because some clients
	 * don't support TLSv1.3. bjl_core doesn't read it; bjl_net_framework and BjlNetFtp do.
	 * Removed in 1.1.0 and restored in 1.2.0 because they still use it (BJL-53).
	 */
	public static final String PROPERTY_FORCE_TLS_VERSION = "ForceTlsVersion";

	private static volatile TrustManager [] defaultTrustManagers = null;


	public static TrustManager[] getDefaultTrustManagers() {
		return defaultTrustManagers;
	}

	public static void setDefaultTrustManagers(TrustManager[] defaultTrustManagers) {
		SecureBaseObject.defaultTrustManagers = defaultTrustManagers;
	}

	/**
	 * File name of the KeyStore
	 */
	private volatile String keyStoreFileName;
	/**
	 * Example JKS,PKCS12
	 */
	private volatile String keyStoreType;

	/**
	 * Example SunX509
	 */
	private volatile String algorithm;

	/**
	 * Example TLS,SSL
	 */
	private volatile String protocol;

	private volatile String keyStorePassword;
	//  true once the KeyStorePassword / KeyStoreName properties have been looked up, so a value that isn't
	//  set is not looked up (and logged) again on every call
	private volatile boolean keyStorePasswordRead;
	private volatile boolean keyStoreFileNameRead;

	//  null means "not configured yet", the 'secure' property is read the first time isSecure() is called.
	private volatile Boolean secure;

	private volatile KeyStore keyStore;

	private volatile TrustManager[] trustManagers = getDefaultTrustManagers();
	private volatile KeyManager[] keyManagers;
	private volatile SecureRandom secureRandom;
	private volatile KeyManagerFactory keyManagerFactory;
	private volatile SSLContext sslContext;

	/**
	 * Used to read the configuration eagerly. Since 1.1.0 every setting is read the first
	 * time it's used, so this does nothing; it's here so subclasses that override it and call
	 * super.init() still compile (restored in 1.2.0, BJL-53).
	 * 
	 * @deprecated nothing needs to call it; settings are read when first used.
	 */
	@Deprecated
	protected void init() {
		// settings are read lazily
	}

	/**
	 * @return true if Object represent a secure connection.  Otherwise, false.
	 */
	public boolean isSecure() {		
		Boolean ret = secure;
		if( ret == null ) {
			ret = getBooleanProperty(PROPERTY_SECURE, false);
			secure = ret;
		}
		return ret;
	}

	/**
	 * Set true if Object represent a secure connection.  Otherwise, false.
	 * @param secure
	 */
	public synchronized void setSecure(boolean secure) {
		this.secure = secure;
		resetSecurityContext();
	}

	/**
	 * Called when any part of the security configuration changes so that objects 
	 * built from the old configuration (the SSLContext, socket factories) are re-created on next use.
	 * Subclasses that cache objects built from the SSLContext should override this 
	 * (and call super.resetSecurityContext()).
	 */
	protected synchronized void resetSecurityContext() {
		sslContext = null;
	}


	/**
	 * @return An SSLContext initialized based on the current configuration. 
	 * A properly configured SSLContext may be used for both client and server
	 * secure connections. 
	 *  
	 * @throws IOException if the SSLContext can't be created (the cause has the details).
	 */
	public SSLContext getSSLContext() throws IOException {

		SSLContext ret = sslContext;
		if( ret == null ) {
			synchronized(this) {
				ret = sslContext;
				if( ret == null ) {
					SSLContext tmp;
					try {
						tmp = SSLContext.getInstance(getProtocol());
						tmp.init(getKeyManagers(), getTrustManagers() , getSecureRandom());
						sslContext = ret = tmp;

					} catch (GeneralSecurityException | IOException | RuntimeException e) {
						//  Not Throwable: an Error (OutOfMemoryError, say) must not be turned into an IOException
						throw new IOException(e);
					}
				}
			}
		}

		//  Return the value read or built here, not the field: a setter (or resetSecurityContext) that
		//  ran after the lock was released could have set the field to null again.
		return ret;

	}

	/**
	 * Set the SSLContext used by this Object to create secure connections.
	 * 
	 * @param context
	 */
	public synchronized void setSSLContext(SSLContext context) {
		resetSecurityContext();
		this.sslContext = context;
	}



	/**
	 * @return the SecureRandom used to initialize the SSLContext (may be null).
	 */
	public SecureRandom getSecureRandom() {
		return secureRandom;
	}

	/**
	 * Set the SecureRandom used to initialize the SSLContext (may be null).
	 * @param secureRandom
	 */
	public synchronized void setSecureRandom(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
		resetSecurityContext();
	}


	/**
	 * Set the KeyManager[]  used to initialize the SSLContext.
	 * 
	 * @param keyManagers
	 */
	public synchronized void setKeyManagers(KeyManager[] keyManagers) {
		this.keyManagers = keyManagers;
		resetSecurityContext();
	}


	/**
	 * @return the KeyManager[]  used to initialize the SSLContext.
	 * 
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws CertificateException
	 * @throws IOException
	 */
	public KeyManager[] getKeyManagers() throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException, IOException {

		KeyManager[] ret = keyManagers;
		if( ret == null ) {
			synchronized(this) {
				ret = keyManagers;
				if( ret == null ) {
					char[] passphrase = null;
					String tmp = getKeyStorePassword();

					if( tmp != null ) {
						passphrase = tmp.toCharArray();	
						try {
							KeyStore ks = getKeyStore(passphrase); 
							KeyManagerFactory kmf = getKeyManagerFactory() ;		
							kmf.init(ks, passphrase);
							keyManagers = ret = kmf.getKeyManagers();
						} finally {
							//  The KeyStore and KeyManagerFactory keep what they need (the factory copies the
							//  password), so don't leave another copy of the password in memory.
							java.util.Arrays.fill(passphrase, '\0');
						}
					} else {
						logDebug("No password defined. No KeyManagers availible (may be okay for a clinet).");
					}
				}
			}
		}

		//  Return the value read or built here, not the field: a setter (or resetSecurityContext) that
		//  ran after the lock was released could have set the field to null again.
		return ret;
	}

	/**
	 * @return the KeyManagerFactory used to generate the KeyManagers and initialize the SSLContext.
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	public KeyManagerFactory getKeyManagerFactory() throws NoSuchAlgorithmException {
		KeyManagerFactory ret = keyManagerFactory;
		if( ret == null ) {
			synchronized(this) {
				ret = keyManagerFactory;
				if( ret == null ) {
					keyManagerFactory = ret = KeyManagerFactory.getInstance(getAlgorithm()) ;
				}
			}
		}

		//  Return the value read or built here, not the field: a setter (or resetSecurityContext) that
		//  ran after the lock was released could have set the field to null again.
		return ret;
	}


	/**
	 * Set the KeyManagerFactory used to generate the KeyManagers and initialize the SSLContext.
	 * @param keyManagerFactory
	 */
	public synchronized void setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
		this.keyManagerFactory = keyManagerFactory;
	}

	/**
	 * @param passphrase used to load the KeyStore (only the first time, the KeyStore is cached).
	 * @return the KeyStore used to initialize the SSLContext.
	 * 
	 * @throws IOException if the KeyStore can't be loaded (the cause has the details).
	 */
	public KeyStore getKeyStore(char[] passphrase) throws IOException {


		KeyStore ret = keyStore;
		if( ret == null ) {
			synchronized(this) {
				ret = keyStore;
				if( ret == null ) {

					try {

						KeyStore ks = KeyStore.getInstance(getKeyStoreType());

						String keyStoreFileName = getKeyStoreFileName();

						if( keyStoreFileName == null ) {
							throw new IllegalStateException("Keystore file location is not defined.  Set the '"+PROPERTY_KEY_STORE_NAME+"' property.");
						}

						/*
						 * If you run a bug detector this will show up as a bug...
						 * Calling this.getClass().getResource(...) could give results other than expected if this class is extended by a class in another package
						 * In this case we want that behavior.  It allows a property file to be replaced or overwritten by the extending class. 
						 */
						//  See if it's available as a resource
						InputStream resource = getClass().getResourceAsStream(keyStoreFileName);
						if( resource == null ) {
							File f = new File(keyStoreFileName);

							if( f.exists() == false) {
								throw new IllegalStateException("KeyStore file not found ("+f+")");
							}

							resource = new FileInputStream(f);			
						}

						//  try-with-resources so the stream is closed even if load fails (bad password, corrupt file)
						try(InputStream in = resource) {
							ks.load(in, passphrase);
						}

						keyStore = ret = ks;
					} catch ( KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
						throw new IOException(e);
					}

				}
			}
		}

		//  Return the value read or built here, not the field: a setter (or resetSecurityContext) that
		//  ran after the lock was released could have set the field to null again.
		return ret;
	}


	/**
	 * @return the password to initialize the KeyStore 'and' the KeyManagerFactory.
	 */
	public String getKeyStorePassword() {

		if( !keyStorePasswordRead ) {
			synchronized (this) {
				if( !keyStorePasswordRead ) {
					if( keyStorePassword == null ) {
						keyStorePassword = getProperty(PROPERTY_PASS_PHRASE);
						//  Never log the password itself
						logDebug(PROPERTY_PASS_PHRASE+(keyStorePassword == null ? " is not defined":" is defined"));
					}
					keyStorePasswordRead = true;
				}
			}
		}

		return keyStorePassword;
	}


	/**
	 * Set the password used to initialize the KeyStore 'and' the KeyManagerFactory
	 * 
	 * @param keyStorePassword
	 */
	public synchronized void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
		//  null: look the property up again, as before
		keyStorePasswordRead = keyStorePassword != null;
		keyStore = null;
		keyManagers = null;
		resetSecurityContext();
	}

	/**
	 * @return the name of the KeyStore file.
	 */
	public String getKeyStoreFileName() {
		if( !keyStoreFileNameRead ) {
			synchronized (this) {
				if( !keyStoreFileNameRead ) {
					if( keyStoreFileName == null ) {
						keyStoreFileName = getProperty(PROPERTY_KEY_STORE_NAME);	
						logDebug(PROPERTY_KEY_STORE_NAME+"="+keyStoreFileName);
					}
					keyStoreFileNameRead = true;
				}
			}
		}

		return keyStoreFileName;
	}



	/**
	 * @return the KeyStore type Example JKS,PKCS12.
	 * Defaults to the JVM default type (KeyStore.getDefaultType(), PKCS12 since Java 9).
	 */
	public String getKeyStoreType() {
		if( keyStoreType == null ) {
			synchronized (this) {
				if( keyStoreType == null ) {
					keyStoreType = getProperty(PROPERTY_KEY_STORE_TYPE, KeyStore.getDefaultType());
					logDebug(PROPERTY_KEY_STORE_TYPE+"="+keyStoreType);
				}
			}
		}
		return keyStoreType;
	}

	/**
	 * @return the currently configured key manager algorithm (Example SunX509).
	 * Defaults to the JVM default algorithm (KeyManagerFactory.getDefaultAlgorithm()).
	 */
	public String getAlgorithm() {
		if( algorithm == null ) {
			synchronized (this) {
				if( algorithm == null ) {
					algorithm = getProperty(PROPERTY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
					logDebug(PROPERTY_ALGORITHM+"="+algorithm);
				}
			}
		}

		return algorithm;
	}

	/**
	 * @return the protocol used to create the SSLContext.  Example TLS,SSL.
	 * 
	 */
	public String getProtocol() {
		if( protocol == null ) {
			synchronized (this) {
				if( protocol == null ) {
					protocol = getProperty(PROPERTY_PROTOCOL,PROTOCOL_TLS);
					logDebug(PROPERTY_PROTOCOL+"="+protocol);
				}
			}
		}
		return protocol;
	}



	/**
	 * Set the KeyStore file name.
	 * 
	 * @param keyStore
	 */
	public synchronized void setKeyStoreFileName(String keyStore) {
		this.keyStoreFileName = keyStore;
		//  null: look the property up again, as before
		keyStoreFileNameRead = keyStore != null;
		this.keyStore = null;
		keyManagers = null;
		resetSecurityContext();
	}

	/**
	 * Set the KeyStore type (Example JKS,PKCS12)
	 * 
	 * @param keyStoreType
	 */
	public synchronized void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
		keyStore = null;
		keyManagers = null;
		resetSecurityContext();
	}

	/**
	 * 
	 * @param algorithm (Example SunX509)
	 */
	public synchronized void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
		keyManagerFactory = null;
		keyManagers = null;
		resetSecurityContext();
	}

	/**
	 * Set the protocol (Example: SSL, TSL)
	 * 
	 * @param protocol
	 */
	public synchronized void setProtocol(String protocol) {
		this.protocol = protocol;
		// the context is based on the protocol so if it's already created we'll need to reset it.
		resetSecurityContext();
	}

	/**
	 * @return TrustManager[] to use with secure connection or null (default TrustManagers will be used).
	 */
	public TrustManager[] getTrustManagers() {
		return trustManagers;
	}

	/**
	 * Set the TrustManager[] to use with secure connection or null (default TrustManagers will be used).
	 * 
	 * @param mgr
	 */
	public synchronized void setTrustManagers(TrustManager[] mgr) {
		trustManagers = mgr;
		resetSecurityContext();
	}


}
