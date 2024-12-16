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
*/

public class LruMap<K,V> extends LinkedHashMap<K,V> {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_MAX_SIZE = 10000;

	private int maxSize = DEFAULT_MAX_SIZE;


	public LruMap() {
		super(2000,0.75f,true);
	}

	public LruMap(int maxEntries) {
		this();
		this.maxSize = maxEntries;
	}


	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		
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

	

	public static void main(String [] args) {
		LruMap<String, String> c = new LruMap<String, String>(5);
		c.put ("1","one");                            // 1
		c.put ("2","two");                            // 2 1
		c.put ("3","three");                          // 3 2 1
		c.put ("4","four");                           // 4 3 2 1
		
	
		
		if (c.get("2")==null) {       				  // 2 4 3 1
			System.out.println("Error ");
		}
		
		c.put ("5","five");                           // 5 2 4 3 1
		c.put ("6","six");		 	                  // 6 5 2 4 3 
		c.put ("4","second four");                    // 4 6 5 2 3 
		// Verify cache content.
		 
		
		if (!c.get("4").equals("second four")) {	  // 4 6 5 2 3
			throw new Error();
		}
		if (!c.get("5").equals("five"))       {		// 5 4 6 2 3
			throw new Error();
		}
		if (!c.get("2").equals("two"))        {		// 2 5 4 6 3
			throw new Error();
		}
		
		
		// List cache content.
		for (Iterator<String> it = c.keySet().iterator(); it.hasNext(); ) {
			String key = it.next().toString();
			System.out.println ("Step 1="+key); 
		}
		System.out.println();
		c.setMaxSize(3);
		
		// List cache content.
		for (Iterator<String> it = c.keySet().iterator(); it.hasNext(); ) {
			String key = it.next().toString();
			System.out.println ("Step 2="+key); 
		}
	}

}
