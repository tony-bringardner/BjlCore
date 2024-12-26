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
 * ~version~V000.00.01-V000.00.00-
 */
package us.bringardner.core.util;

import us.bringardner.core.BaseObject;
import us.bringardner.core.ILogger;

/**
 * The objective of the LogHelper is to provide an easy way 
 * to access the functionality of the BaseObject (logging and properties) 
 * from static methods or in objects where it's not practical to extend BAseObject
 *  
 * @author Tony Bringardner
 * 
 *
 */
public class LogHelper extends BaseObject {
	
	private String name;
	
	public LogHelper(Class<?> cls) {
		this(cls.getName());
	}
	
	public LogHelper(String name) {
		this.name = name;
	}

	@Override
	protected ILogger getLogger(String name) {		
		return super.getLogger(this.name);
	}
}
