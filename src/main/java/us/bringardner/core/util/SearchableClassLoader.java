// ~version~V000.01.01-V000.00.00-
/**
 *	Copyright 1999-2026 Tony Bringardner
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A URLClassLoader that can search its URLs (directories, jar and zip files) for classes
 * that extend or implement a target class.
 *
 * Only the classes that match are loaded (but not initialized). The others are ruled out by
 * reading the names in their class file header (the class, its super class and interfaces),
 * so searching a large class path doesn't load, and keep in memory, every class on it.
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
	 * Find classes that extend or implement the target directly (cls.getSuperclass() == target or
	 * target is one of cls.getInterfaces()), or are the target itself.
	 *
	 * @param target
	 * @return a list of classes found in this loader's URLs
	 */
	public List<Class<?>> findTarget(Class<?> target) {
		return findTarget(target, false);
	}

	/**
	 * Find classes that extend or implement the target.
	 *
	 * @param target
	 * @param includeIndirect if true, also return classes that extend or implement the target through
	 *  other classes (any class assignable to target), otherwise only direct sub classes / implementations.
	 * @return a list of classes found in this loader's URLs, in the order they were found
	 */
	public List<Class<?>> findTarget(Class<?> target, boolean includeIndirect) {

		Search search = new Search(target, includeIndirect);
		URL[] urls = getURLs();
		if( urls != null ) {
			for (URL url : urls) {
				try {
					String protocol = url.getProtocol();
					if("file".equals(protocol)) {
						File file = toFile(url);
						String path = file.getPath();
						if( file.isDirectory()) {
							proccessDir(file,file,search);
						} else if(path.endsWith(".class")) {
							parseFile(null,file,search);
						} else if(path.endsWith(".jar") || path.endsWith(".zip")) {
							parseJar(file,search);
						}
					}
				} catch (IOException | RuntimeException e) {
					// skip entries that can't be read
				}
			}
		}

		return new ArrayList<>(search.found);
	}

	/** The state of one findTarget call */
	private final class Search {
		final Class<?> target;
		final String targetName;
		final boolean includeIndirect;
		//  A set, so a class found twice (in a directory and a jar, say) is only listed once
		final Set<Class<?>> found = new LinkedHashSet<>();
		//  For includeIndirect: class name -> could it be a sub type of target
		final Map<String, Boolean> mayExtend = new HashMap<>();
		//  The real paths of the directories and jars already searched. A symbolic link back to a
		//  parent directory made the search go round and round: with two such links it never ended.
		final Set<String> searched = new java.util.HashSet<>();

		/** @return true the first time this file (or directory) is seen, by its real path */
		boolean firstVisit(File file) {
			String key;
			try {
				key = file.toPath().toRealPath().toString();
			} catch (IOException | RuntimeException e) {
				key = file.getAbsolutePath();
			}
			return searched.add(key);
		}

		Search(Class<?> target, boolean includeIndirect) {
			this.target = target;
			this.targetName = target.getName();
			this.includeIndirect = includeIndirect;
		}

		/**
		 * @return false only if the class can't match. True if it may, it is then loaded to make sure.
		 */
		boolean mayMatch(ClassHeader header) {
			if( header.name.equals(targetName) ) {
				return true;
			}
			if( !includeIndirect ) {
				if( targetName.equals(header.superName) ) {
					return true;
				}
				for (String in : header.interfaces) {
					if( in.equals(targetName) ) {
						return true;
					}
				}
				return false;
			}
			return mayExtend(header, new java.util.HashSet<>());
		}

		private boolean mayExtend(ClassHeader header, Set<String> visiting) {
			Boolean ret = mayExtend.get(header.name);
			if( ret != null ) {
				return ret;
			}
			if( !visiting.add(header.name) ) {
				// a cycle (only in broken class files)
				return true;
			}
			boolean may = header.name.equals(targetName)
					|| supertypeMayExtend(header.superName, visiting);
			for(int i=0; !may && i < header.interfaces.length; i++ ) {
				may = supertypeMayExtend(header.interfaces[i], visiting);
			}
			mayExtend.put(header.name, may);
			return may;
		}

		private boolean supertypeMayExtend(String name, Set<String> visiting) {
			if( name == null ) {
				// java.lang.Object (or a module-info)
				return false;
			}
			if( name.equals(targetName) ) {
				return true;
			}
			Boolean known = mayExtend.get(name);
			if( known != null ) {
				return known;
			}
			//  Read the super type's header the same way the class would be loaded (this loader, then its parents)
			ClassHeader header = null;
			try (InputStream in = getResourceAsStream(name.replace('.', '/')+".class")) {
				if( in != null ) {
					header = ClassHeader.read(in);
				}
			} catch (IOException | RuntimeException e) {
				header = null;
			}
			if( header == null ) {
				// Can't tell without loading it, so let the real check decide
				return true;
			}
			return mayExtend(header, visiting);
		}
	}

	/**
	 * The names in a class file header. Only the start of the file is read.
	 */
	static final class ClassHeader {
		final String name;
		/** null for java.lang.Object and module-info */
		final String superName;
		final String[] interfaces;

		private ClassHeader(String name, String superName, String[] interfaces) {
			this.name = name;
			this.superName = superName;
			this.interfaces = interfaces;
		}

		/**
		 * @param in a class file
		 * @return its header, or null if it is not a class file this method understands
		 */
		static ClassHeader read(InputStream in) throws IOException {
			DataInputStream data = new DataInputStream(new BufferedInputStream(in, 4096));
			if( data.readInt() != 0xCAFEBABE ) {
				return null;
			}
			data.readUnsignedShort(); // minor version
			data.readUnsignedShort(); // major version
			int count = data.readUnsignedShort();
			String[] utf8 = new String[count];
			int[] classNameIndex = new int[count];
			for(int i=1; i < count; i++ ) {
				int tag = data.readUnsignedByte();
				switch (tag) {
				case 1: utf8[i] = data.readUTF(); break;                        // Utf8
				case 7: classNameIndex[i] = data.readUnsignedShort(); break;    // Class
				case 8: case 16: case 19: case 20: data.skipBytes(2); break;    // String, MethodType, Module, Package
				case 15: data.skipBytes(3); break;                              // MethodHandle
				case 3: case 4: case 9: case 10: case 11: case 12: case 17: case 18: data.skipBytes(4); break;
				case 5: case 6: data.skipBytes(8); i++; break;                  // Long and Double take two entries
				default: return null;                                           // a newer class file format
				}
			}
			data.readUnsignedShort(); // access flags
			String name = className(data.readUnsignedShort(), utf8, classNameIndex);
			if( name == null ) {
				return null;
			}
			int superIndex = data.readUnsignedShort();
			String superName = superIndex == 0 ? null : className(superIndex, utf8, classNameIndex);
			String[] interfaces = new String[data.readUnsignedShort()];
			for(int i=0; i < interfaces.length; i++ ) {
				interfaces[i] = className(data.readUnsignedShort(), utf8, classNameIndex);
				if( interfaces[i] == null ) {
					return null;
				}
			}
			return new ClassHeader(name, superName, interfaces);
		}

		private static String className(int index, String[] utf8, int[] classNameIndex) {
			if( index <= 0 || index >= classNameIndex.length ) {
				return null;
			}
			int nameIndex = classNameIndex[index];
			if( nameIndex <= 0 || nameIndex >= utf8.length || utf8[nameIndex] == null ) {
				return null;
			}
			return utf8[nameIndex].replace('/', '.');
		}
	}

	/*
	 * Convert a file URL to a File, handling escaped characters such as spaces (%20).
	 */
	private static File toFile(URL url) {
		try {
			return new File(url.toURI());
		} catch (URISyntaxException | IllegalArgumentException e) {
			return new File(url.getPath());
		}
	}

	/**
	 * Find the class for a file when we don't know the root of the class path.
	 * Try the last element of the path, then the last two ... until one of them loads.
	 *
	 * @param path of a class file, without the .class extension
	 * @return the class or null if it could not be loaded
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
	 * Load a class without failing on classes that can't be linked (missing dependencies).
	 */
	private Class<?> tryLoad(String name) {
		try {
			return loadClass(name);
		} catch (ClassNotFoundException | LinkageError | SecurityException e) {
			// Not a class (or it depends on classes that are not available)
			return null;
		}
	}

	private static ClassHeader readHeader(File file) {
		try (InputStream in = new FileInputStream(file)) {
			return ClassHeader.read(in);
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	private void parseFile(File root, File file, Search search) {
		String path = file.getAbsolutePath();
		if( !path.endsWith(".class")) {
			return;
		}

		ClassHeader header = readHeader(file);
		if( header != null ) {
			if( !search.mayMatch(header) ) {
				return;
			}
			//  The header has the class name, so we don't have to work it out from the path
			Class<?> cls = tryLoad(header.name);
			if( cls != null ) {
				checkClass(cls,search);
				return;
			}
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
		checkClass(cls,search);
	}

	private void checkClass(Class<?> cls, Search search) {
		if( cls == null ) {
			return;
		}
		try {
			if( matches(cls, search.target, search.includeIndirect) ) {
				search.found.add(cls);
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

	private void proccessDir(File root, File dir, Search search) {
		if( !search.firstVisit(dir) ) {
			return;
		}
		File[] kids = dir.listFiles();
		if( kids != null) {
			//  sort so the results are the same on every platform / file system
			Arrays.sort(kids);
			for(File file: kids) {
				String path = file.getPath();
				if( file.isDirectory()) {
					proccessDir(root,file,search);
				} else if(path.endsWith(".class")) {
					parseFile(root,file,search);
				} else if(path.endsWith(".jar") || path.endsWith(".zip")) {
					try {
						parseJar(file,search);
					} catch (IOException e) {
					}
				}
			}
		}

	}

	private void parseJar(File jar, Search search) throws IOException {
		if( !search.firstVisit(jar) ) {
			return;
		}
		try(ZipFile file = new ZipFile(jar)) {
			Enumeration<? extends ZipEntry> i = file.entries();
			while( i.hasMoreElements()) {
				ZipEntry ze = i.nextElement();
				String entryName = ze.getName();
				if( entryName.endsWith(".class") && !entryName.endsWith("module-info.class") && !entryName.startsWith("META-INF/")) {
					String name = entryName.substring(0, entryName.length()-6).replace('/', '.');
					ClassHeader header;
					try (InputStream in = file.getInputStream(ze)) {
						header = ClassHeader.read(in);
					} catch (IOException | RuntimeException e) {
						header = null;
					}
					if( header != null && !search.mayMatch(header) ) {
						continue;
					}
					checkClass(tryLoad(name),search);
				}
			}
		}
	}


	public SearchableClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}



}
