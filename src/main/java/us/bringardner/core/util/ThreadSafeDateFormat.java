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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * ThreadSafeDateFormat is exactly that, a thread safe wrapper around the 
 * SimpleDateFormat.  It implements only the format and parse functions
 * in a thread safe way.
 * 
 * @author Tony Bringardner
 *
 */
public class ThreadSafeDateFormat {

	private SimpleDateFormat format;
	
	public ThreadSafeDateFormat(String dateTimeFormat) {
		format = new SimpleDateFormat(dateTimeFormat);
	}

	public synchronized String format(Date date) {
		return format.format(date);
	}
	
	public synchronized Date parse(String dateTimeValue) throws ParseException {
		return format.parse(dateTimeValue);
	}

	public synchronized void setTimeZone(TimeZone tz) {
		format.setTimeZone(tz);
	}

}
