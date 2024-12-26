// ~version~V000.01.03
package us.bringardner.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	public static final String PROPERTY_FORCE_TLS_VERSION = "ForceTlsVersion";

	private static TrustManager [] defaultTrustManagers = null;


	public static TrustManager[] getDefaultTrustManagers() {
		return defaultTrustManagers;
	}

	public static void setDefaultTrustManagers(TrustManager[] defaultTrustManagers) {
		SecureBaseObject.defaultTrustManagers = defaultTrustManagers;
	}

	/**
	 * File name of the KeyStore
	 */
	private String keyStoreFileName;
	/**
	 * Example JKS,PKCS12
	 */
	private String keyStoreType;

	/**
	 * Example SunX509
	 */
	private String algorithm;

	/**
	 * Example TLS,SSL
	 */
	private String protocol;

	private String keyStorePassword;

	private boolean secure;

	private volatile KeyStore keyStore;

	private volatile TrustManager[] trustManagers = getDefaultTrustManagers();
	private volatile KeyManager[] keyManagers;
	private volatile SecureRandom secureRandom;
	private volatile KeyManagerFactory keyManagerFactory;
	private volatile SSLContext sslContext;

	/**
	 * @return true if Object represent a secure connection.  Otherwise, false.
	 */
	public boolean isSecure() {		
		return secure;
	}

	/**
	 * Set true if Object represent a secure connection.  Otherwise, false.
	 * @param secure
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}


	/**
	 * Initialize fields that can't use lazy initialization (the 'secure' flag).
	 * 
	 */
	protected void init() {
		String tmp = null;

		if( (tmp = getProperty(PROPERTY_SECURE)) != null ) {
			secure = tmp.toLowerCase().equals("true");
		}

	}

	/**
	 * @return An SSLContext initialized based on the current configuration. 
	 * A properly configured SSLContext may be used for both client and server
	 * secure connections. 
	 *  
	 * @throws CertificateException
	 * 
	 * @throws IOException
	 */
	public SSLContext getSSLContext() throws IOException {

		if( sslContext == null ) {
			synchronized(this) {
				if( sslContext == null ) {
					SSLContext tmp;
					try {
						tmp = SSLContext.getInstance(getProtocol());
						tmp.init(getKeyManagers(), getTrustManagers() , getSecureRandom());
						sslContext = tmp;

					} catch (Throwable e) {
						throw new IOException(e);
					}
				}
			}
		}

		return sslContext;

	}

	/**
	 * Set the SSLContext used by this Object to create secure connections.
	 * 
	 * @param context
	 */
	public void setSSLContext(SSLContext context) {
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
	public void setSecureRandom(SecureRandom secureRandom) {
		this.secureRandom = secureRandom;
	}


	/**
	 * Set the KeyManager[]  used to initialize the SSLContext.
	 * 
	 * @param keyManagers
	 */
	public void setKeyManagers(KeyManager[] keyManagers) {
		this.keyManagers = keyManagers;
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

		if( keyManagers == null ) {
			synchronized(this) {
				if( keyManagers == null ) {
					char[] passphrase = null;
					String tmp = getKeyStorePassword();

					if( tmp != null ) {
						passphrase = tmp.toCharArray();	
						KeyStore ks = getKeyStore(passphrase); 
						KeyManagerFactory kmf = getKeyManagerFactory() ;		
						kmf.init(ks, passphrase);
						keyManagers = kmf.getKeyManagers();
					} else {
						logDebug("No password defined. No KeyManagers availible (may be okay for a clinet).");
					}
				}
			}
		}

		return keyManagers;
	}

	/**
	 * @return the KeyManagerFactory used to generate the KeyManagers and initialize the SSLContext.
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	public KeyManagerFactory getKeyManagerFactory() throws NoSuchAlgorithmException {
		if( keyManagerFactory == null ) {
			synchronized(this) {
				if( keyManagerFactory == null ) {
					keyManagerFactory = KeyManagerFactory.getInstance(getAlgorithm()) ;
				}
			}
		}

		return keyManagerFactory;
	}


	/**
	 * Set the KeyManagerFactory used to generate the KeyManagers and initialize the SSLContext.
	 * @param keyManagerFactory
	 */
	public void setKeyManagerFactory(KeyManagerFactory keyManagerFactory) {
		this.keyManagerFactory = keyManagerFactory;
	}

	/**
	 * @param passphrase
	 * @return the KeyStore used to initialize the SSLContext.
	 * 
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 */
	public KeyStore getKeyStore(char[] passphrase) throws IOException {


		if( keyStore == null ) {
			synchronized(this) {
				if( keyStore == null ) {

					try {

						KeyStore ks = KeyStore.getInstance(getKeyStoreType());

						String keyStoreFileName = getKeyStoreFileName();

						if( keyStoreFileName == null ) {
							throw new IllegalStateException("Keystore file location is not defined.  Set the 'keys' system property.");
						}

						/*
						 * If you run a bug detector this will show up as a bug...
						 * Calling this.getClass().getResource(...) could give results other than expected if this class is extended by a class in another package
						 * In this case we want that behavior.  It allows a property file to be replaced or overwritten by the extending class. 
						 */
						//  See if it's available as a resource
						InputStream in = getClass().getResourceAsStream(keyStoreFileName);
						if( in == null ) {
							File f = new File(keyStoreFileName);

							if( f.exists() == false) {
								throw new IllegalStateException("KeyStore file not found ("+f+")");
							}

							in = new FileInputStream(f);			
						}


						ks.load(in, passphrase);

						try {in.close();} catch (Throwable e) {}

						keyStore = ks;
					} catch ( KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
						throw new IOException(e);
					}

				}
			}
		}

		return keyStore;
	}


	/**
	 * @return the password to initialize the KeyStore 'and' the KeyManagerFactory.
	 */
	public String getKeyStorePassword() {

		if( keyStorePassword == null ) {
			synchronized (this) {
				if( keyStorePassword == null ) {
					keyStorePassword = getProperty(PROPERTY_PASS_PHRASE);
					logDebug(PROPERTY_PASS_PHRASE+"="+keyStorePassword);
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
	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	/**
	 * @return the name of the KeyStore file.
	 */
	public String getKeyStoreFileName() {
		if( keyStoreFileName == null ) {
			synchronized (this) {
				if( keyStoreFileName == null ) {
					keyStoreFileName = getProperty(PROPERTY_KEY_STORE_NAME);	
					logDebug(PROPERTY_KEY_STORE_NAME+"="+keyStoreFileName);
				}
			}
		}

		return keyStoreFileName;
	}



	/**
	 * @return the KeyStore type Example JKS,PKCS12
	 */
	public String getKeyStoreType() {
		if( keyStoreType == null ) {
			synchronized (this) {
				if( keyStoreType == null ) {
					keyStoreType = getProperty(PROPERTY_KEY_STORE_TYPE);
					logDebug(PROPERTY_KEY_STORE_TYPE+"="+keyStoreType);
				}
			}
		}
		return keyStoreType;
	}

	/**
	 * @return the currently configured algorithm (Example SunX509)
	 */
	public String getAlgorithm() {
		if( algorithm == null ) {
			synchronized (this) {
				if( algorithm == null ) {
					algorithm = getProperty(PROPERTY_ALGORITHM);
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
	public void setKeyStoreFileName(String keyStore) {
		this.keyStoreFileName = keyStore;
	}

	/**
	 * Set the KeyStore type (Example JKS,PKCS12)
	 * 
	 * @param keyStoreType
	 */
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	/**
	 * 
	 * @param algorithm (Example SunX509)
	 */
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * Set the protocol (Example: SSL, TSL)
	 * 
	 * @param protocol
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
		// the context is based on the protocol so if it's already created we'll need to reset it.
		sslContext = null;
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
	public void setTrustManagers(TrustManager[] mgr) {
		trustManagers = mgr;
	}


}
