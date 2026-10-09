/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.com/tony">Tony Bringardner</A>
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
package us.bringardner.parley.files.viewer;

import java.io.IOException;
import java.util.regex.Pattern;

import us.bringardner.parley.files.FileSource;



public class SearchRequest {
	public static enum Operator {BETWEEN,LESS_THAN,EQUAL,GREATER_THAN};
	public static enum Type {FILE,DIR,LINK};

	public Runnable postProcessor;
	public ActivityLayer activity;
	public SearchFileTableModel model;
	public String name;

	public boolean caseSensative;

	public Operator dateOp;
	public long date1;
	public long date2;

	public Operator sizeOp;
	public long size1;
	public long size2;

	public Type type;
	private Pattern pattern;
	private String namePAttern;

	public boolean isCanceled() {

		return activity != null && !activity.isRunning();
	}


	public boolean isMatch(FileSource f) throws IOException {
		boolean ret = false;
		boolean nameReq = name != null && name.length()>0;
		if( nameReq) {
			ret = matchName(f);
		}

		if(  dateOp != null && (!nameReq || ret)) {
			// must match
			ret = matchDate(f);
		}

		if( sizeOp != null && ((!nameReq || ret) && (dateOp==null || ret) ) ) {
			//  must match
			ret = matchSize(f);
		}
		if( type != null && ((!nameReq || ret) && (dateOp==null || ret) && (sizeOp == null || ret))) {
			ret = matchType(f);
		}

		return ret;
	}

	private boolean matchDate(FileSource f) throws IOException {
		boolean ret = false;
		long lm = f.lastModified();
		if( dateOp == Operator.LESS_THAN) {
			ret = lm < date1;
		} else 	if( dateOp == Operator.GREATER_THAN) {
			ret = lm > date1;
		} else 	if( dateOp == Operator.BETWEEN) {
			ret = lm >= date1 && lm <= date2;
		}
		return ret ;
	}
	private boolean matchSize(FileSource f) throws IOException {
		boolean ret = false;
		if( f.isFile()) {
			long lm = f.length();
			if( sizeOp == Operator.LESS_THAN) {
				ret = lm <= size1;
			} else 	if( sizeOp == Operator.GREATER_THAN) {
				ret = lm >= size1;
			} else 	if( sizeOp == Operator.BETWEEN) {
				ret = lm >= size1 && lm <= size2;
			}
		}
		return ret ;
	}

	private boolean matchName(FileSource f) {
		String nm = f.getName();
		if( pattern == null ) {
			pattern = Pattern.compile(getNamePattern(), caseSensative ? Pattern.CASE_INSENSITIVE:0);
		}
		return pattern.matcher(nm).matches();
	}

	private String getNamePattern() {
		if( namePAttern == null ) {
			byte[] data = name.getBytes();
			StringBuilder buf = new StringBuilder(); 
			for (int idx = 0; idx < data.length; idx++) {
				switch (data[idx]) {
				case '.':
					if(idx == 0 ||  idx < data.length-1) {
						if( data[idx] != '*') {
							buf.append("[.]");
						} else {
							buf.append('.');
						}
					} else {
						buf.append('.');
					}
					break;
				case '*':
					if( idx == 0 || data[idx-1] != '.') {
						buf.append(".*");
					} else {
						buf.append('*');
					}
					break;

				default:
					buf.append((char)data[idx]);
					break;
				}
			}
			namePAttern = buf.toString();
		}
		return namePAttern;
	}


	private boolean matchType(FileSource f) throws IOException {
		boolean ret = false;
		if(type == Type.DIR) {
			ret = f.isDirectory();
		} else if(type == Type.FILE) {
			ret = f.isFile();
		} else {
			// islink?
		}

		return ret;
	}


	
}
