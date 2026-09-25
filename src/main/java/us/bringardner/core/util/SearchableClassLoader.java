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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A URLClassLoader that can search its URLs (directories, jar and zip files) for classes 
 * that extend or implement a target class.
 * 
 * Note: every class that is examined is loaded (but not initialized) by this loader.
 * Call close() when the loader is no longer needed to release open jar files.
 */
public class SearchableClassLoader extends URLClassLoader {	

	public static SearchableClassLoader getClassPathLoader() {
		return SearchableClassLoader.getLoader(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
	}

	public static SearchableClassLoader getLoader(List<String>  paths) {
		SearchableClassLoader loader = new SearchableClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
		if( paths != null ) {
			for(String path : paths) {
				if( path == null || path.isEmpty()) {
					continue;
				}
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

	/**
	 * Find classes that are the target, directly extend the target or directly implement the target (if it's an interface).
	 * 
	 * @param target
	 * @return the list of matching classes 
	 */
	public List<Class<?>> findTarget(Class<?> target) {
		return findTarget(target, false);
	}

	/**
	 * Find classes that are the target or extend / implement the target.
	 * 
	 * @param target
	 * @param includeIndirect if true, include classes that extend or implement the target indirectly 
	 * (for example a subclass of a subclass). If false, only direct subclasses / implementations are included.
	 * @return the list of matching classes 
	 */
	public List<Class<?>> findTarget(Class<?> target, boolean includeIndirect) {

		List<Class<?>> ret = new ArrayList<>();
		URL[] urls = getURLs();
		if( urls != null ) {
			for (URL url : urls) {
				try {
					String protocol = url.getProtocol();
					if("file".equals(protocol)) {
						File file = toFile(url);
						String path = file.getPath();
						if( file.isDirectory()) {
							proccessDir(file,file,ret,target,includeIndirect);
						} else if(path.endsWith(".class")) {
							parseFile(null,file,ret,target,includeIndirect);
						} else if(path.endsWith(".jar") || path.endsWith(".zip")) {
							parseJar(file,ret,target,includeIndirect);	
						}
					}
				} catch (IOException | RuntimeException e) {
					// skip entries that can't be read
				}
			}
		}

		return ret;
	}

	/*
	 * URL.getPath() is URL encoded (a space is %20), so convert through a URI. 
	 */
	private static File toFile(URL url) {
		try {
			return new File(url.toURI());
		} catch (URISyntaxException | IllegalArgumentException e) {
			return new File(url.getPath());
		}
	}

	/**
	 * Find a class by trying each sub path (from the end) as a class name.
	 * This is used when the file is not under a class path root. 
	 * 
	 * @param path of a class file without the .class extension
	 * @return the Class or null
	 */
	Class<?> findFromPath(String path) {
		Class<?> ret = null;
		char sep = File.separatorChar;
		String rx = "["+sep+"]";
		if( sep == '\\') {
			rx = "[\\\\]";
		}
		String []parts = path.split(rx);
		StringBuilder className = new StringBuilder();

		for(int idx=parts.length-1;ret == null && idx >=0; idx--) {
			if(className.length()!=0) {
				className.insert(0, '.');
			}
			className.insert(0, parts[idx]);
			String name = className.toString();
			//  this prevents an annoying error message
			if(! name.endsWith("MulticastDnsAdvertiser")) {
				ret = tryLoad(name);
			}
		}
		return ret;
	}

	/*
	 * Load a class without initializing it. Returns null if it can't be loaded.
	 */
	private Class<?> tryLoad(String name) {
		try {
			return loadClass(name);
		} catch (ClassNotFoundException | LinkageError | SecurityException e) {
			// Not a class (or it depends on classes that are not available)
			return null;
		}
	}

	private void parseFile(File root, File file, List<Class<?>> ret, Class<?> target, boolean includeIndirect) {
		String path = file.getAbsolutePath();
		if( !path.endsWith(".class")) {
			return;	
		}

		String name = path.substring(0, path.length()-6);
		Class<?> cls = null;
		if( root != null ) {
			//  The class name is the path relative to the class path root (fast, one lookup)
			String rootPath = root.getAbsolutePath();
			if( name.startsWith(rootPath) && name.length() > rootPath.length()+1) {
				String relative = name.substring(rootPath.length()+1).replace(File.separatorChar, '.');
				cls = tryLoad(relative);
			}
		}
		if( cls == null ) {
			//  The directory may not be a class path root, so try each sub path.
			cls = findFromPath(name);
		}
		checkClass(cls,ret,target,includeIndirect);
	}

	private void checkClass(String name, List<Class<?>> ret,Class<?> target, boolean includeIndirect) {
		checkClass(tryLoad(name), ret, target, includeIndirect);
	}

	private void checkClass(Class<?> cls, List<Class<?>> ret,Class<?> target, boolean includeIndirect) {
		if( cls == null ) {
			return;
		}
		try {
			if( matches(cls, target, includeIndirect) && !ret.contains(cls)) {
				ret.add(cls);
			}
		} catch (LinkageError | RuntimeException e) {
			// The class could not be resolved (missing dependencies)
		}
	}

	private static boolean matches(Class<?> cls, Class<?> target, boolean includeIndirect) {
		if( cls == target ) {
			return true;
		}
		if( includeIndirect ) {
			return target.isAssignableFrom(cls);
		}
		if( cls.getSuperclass() == target ) {
			return true;
		}
		if(target.isInterface()) {
			for (Class<?> in : cls.getInterfaces()) {
				if( in == target) {
					return true;
				}
			}
		}
		return false;
	}

	private void proccessDir(File root, File dir, List<Class<?>> ret, Class<?> target, boolean includeIndirect) {
		File[] kids = dir.listFiles();
		if( kids != null) {
			//  sort so the results are the same on every platform / file system
			Arrays.sort(kids);
			for(File file: kids) {
				String path = file.getPath();
				if( file.isDirectory()) {
					proccessDir(root,file,ret,target,includeIndirect);
				} else if(path.endsWith(".class")) {
					parseFile(root,file,ret,target,includeIndirect);
				} else if(path.endsWith(".jar") || path.endsWith(".zip")) {
					try {
						parseJar(file,ret,target,includeIndirect);
					} catch (IOException e) {
					}	
				}			
			}
		}

	}

	private void parseJar(File jar, List<Class<?>> ret,Class<?> target, boolean includeIndirect) throws IOException {
		try(ZipFile file = new ZipFile(jar)) {
			Enumeration<? extends ZipEntry> i = file.entries();
			while( i.hasMoreElements()) {
				ZipEntry ze = i.nextElement();
				String entryName = ze.getName();
				if( entryName.endsWith(".class") && !entryName.endsWith("module-info.class") && !entryName.startsWith("META-INF/")) {
					String name = entryName.substring(0, entryName.length()-6).replace('/', '.');
					checkClass(name,ret,target,includeIndirect);
				}
			}
		}
	}


	public SearchableClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}



}
