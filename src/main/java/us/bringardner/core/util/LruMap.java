// ~version~V000.00.01-V000.00.00-


package us.bringardner.core.util;


import java.util.Iterator;
import java.util.LinkedHashMap;

/**
* <PRE>
*
* LruMap is a strait forward implementation of a LRU (Least Recently Used) map
* as described by the Sun documentation for 
* java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry).
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
*/

public class LruMap<K,V> extends LinkedHashMap<K,V> {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_MAX_SIZE = 10000;

	private int maxSize = DEFAULT_MAX_SIZE;


	public LruMap() {
		super(2000,0.75f,true);
	}

	public LruMap(int maxEntries) {
		//  Size the table for the maximum number of entries (plus room for the load factor) so small maps don't waste memory.
		super(initialCapacity(maxEntries),0.75f,true);
		this.maxSize = maxEntries;
	}

	private static int initialCapacity(int maxEntries) {
		if( maxEntries <= 0 ) {
			return 16;
		}
		return (int) Math.min(2000, (maxEntries / 0.75f) + 2);
	}


	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * @param maxSize the most entries the map keeps (0 or less: no limit).
	 * When the new size is smaller, the least recently used entries are removed.
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		if( maxSize <= 0 ) {
			//  No limit (the same as the constructor), so nothing is removed.
			return;
		}
		
		int sz = size();
		
		while(sz > maxSize) {
			Iterator<K> it = keySet().iterator();			
			Object key = it.next();
			if( key == null ) {
				//  This should never happen
			} else {
				remove(key);
			}
			sz--;
		}
		
	}

	/*
	 * Calculate return values based on number of entries.
	 *  
	 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
	 */
	@SuppressWarnings("rawtypes")
	protected boolean removeEldestEntry(java.util.Map.Entry eldest) {
		boolean ret = false;
		if( maxSize > 0 ) {
			ret = size() > maxSize;
		}
		
		return ret;
	}


}
