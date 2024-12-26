// ~version~V000.01.01-V000.00.00-
/**
 *	Copyright 1999-2024 Tony Bringardner
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 *	You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for 
 *	the specific language governing permissions and limitations under the License.
 * 
 * 
 */
package us.bringardner.core.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SearchableClassLoader extends URLClassLoader {	

	public static SearchableClassLoader getClassPathLoader() {
		return SearchableClassLoader.getLoader(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
	}

	public static SearchableClassLoader getLoader(List<String>  paths) {
		SearchableClassLoader loader = new SearchableClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
		if( paths != null ) {
			for(String path : paths) {
				File file = new File(path);
				try {
					loader.addUrl(file.toURI().toURL());
				} catch (MalformedURLException e) {
				}	
			}
		}


		return loader;
	}

	public void addUrl(URL url) {
		this.addURL(url);
	}

	public List<Class<?>> findTarget(Class<?> target) {

		List<Class<?>> ret = new ArrayList<>();
		URL[] urls = getURLs();
		if( urls != null ) {
			for (URL url : urls) {
				try {
					String protocol = url.getProtocol();
					if("file".equals(protocol)) {
						String path = url.getPath();
						File file = new File(path);
						if( file.isDirectory()) {
							proccessDir(file,ret,target);
						} else if(path.endsWith(".class")) {
							parseFile(file,ret,target);
						} else if(path.endsWith(".jar") || path.endsWith(".zip")) {
							parseJar(url,ret,target);	
						}
					}
				} catch (Throwable e) {
				}
			}
		}

		return ret;
	}

	static Class<?> findFromPath(String path) {
		Class<?> ret = null;
		String []parts = path.split("[/]");
		StringBuilder className = new StringBuilder();

		for(int idx=parts.length-1;ret == null && idx >=0; idx--) {
			if(className.length()!=0) {
				className.insert(0, '.');
			}
			className.insert(0, parts[idx]);
			try {
				try {
					String name = className.toString();
					//  this prevents an annoying error message
					if(! name.endsWith("MulticastDnsAdvertiser")) {
						ret = Class.forName(className.toString());		
					}
				} catch (Throwable e) {				
				}
			} catch (Exception e) {
			}
		}
		return ret;
	}

	private void parseFile(File file, List<Class<?>> ret, Class<?> target) {
		String path = file.getAbsolutePath();
		if( !path.endsWith(".class")) {
			return;	
		}

		String name = path.substring(0, path.length()-6);

		checkClass(name,ret,target);
	}

	private void checkClass(String name, List<Class<?>> ret,Class<?> target) {
		try {
			Class<?> cls = findFromPath(name);
			if( cls != null ) {
				if (cls.isAssignableFrom(target) || cls.getSuperclass() == target) {
					ret.add(cls);				
				} 	else {
					if(target.isInterface()) {
						Class<?>[] in = cls.getInterfaces();
						for (Class<?> class1 : in) {
							if( class1 == target) {
								ret.add(cls);
							}
						}
					}
				}

			}
		} catch (Throwable e) {
			//System.out.println(e);
		}


	}

	private void proccessDir(File dir, List<Class<?>> ret, Class<?> target) {
		File[] kids = dir.listFiles();
		if( kids != null) {
			for(File file: kids) {
				String path = file.getAbsolutePath();
				if( file.isDirectory()) {
					proccessDir(file,ret,target);
				} else if(path.endsWith(".class")) {
					parseFile(file,ret,target);
				} else if(path.endsWith(".jar") || path.endsWith(".zip")) {
					try {
						parseJar(file.toURI().toURL(),ret,target);
					} catch (Throwable e) {
					}	
				}			
			}
		}

	}

	private void parseJar(URL url, List<Class<?>> ret,Class<?> target) throws IOException {
		ZipFile file = new ZipFile(new File(url.getFile()));
		try {
			Enumeration<? extends ZipEntry> i = file.entries();
			while( i.hasMoreElements()) {
				ZipEntry ze = i.nextElement();
				String name = ze.getName().replace('/', '.');
				if( ze.getName().endsWith(".class") ) {
					name = name.substring(0, name.length()-6);
					checkClass(name,ret,target);
				}
			}

			//System.out.println("Parse url file = "+url.getFile());
		} finally {
			file.close();
		}
	}


	public SearchableClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}



}
