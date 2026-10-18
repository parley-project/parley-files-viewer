/**
 * Copyright Tony Bringardner 1998, 2026 <a href="http://bringardner.com/tony">Tony Bringardner</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.bringardner.parley.files.viewer.fx;

import java.io.IOException;
import java.util.regex.Pattern;

import us.bringardner.parley.files.FileSource;

/**
 * What a search looks for. Every criterion that is set must match. Names are matched whole,
 * with * for any characters and ? for one; everything else in the name is literal.
 */
final class SearchCriteria {

	enum Operator { BETWEEN, AT_MOST, EXACTLY, AT_LEAST }

	enum Type { FILE, DIRECTORY }

	private Pattern name;
	private Operator sizeOp;
	private long size1;
	private long size2;
	private Long modifiedFrom;
	private Long modifiedTo;
	private Type type;

	/** Files whose whole name matches pattern (* and ? are wild); null or empty for any name. */
	SearchCriteria name(String pattern, boolean caseSensitive) {
		if( pattern == null || pattern.trim().isEmpty()) {
			name = null;
		} else {
			name = Pattern.compile(glob(pattern.trim()), caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}
		return this;
	}

	/** Files (not folders) by size in bytes; size2 is used only for BETWEEN. */
	SearchCriteria size(Operator op, long size1, long size2) {
		this.sizeOp = op;
		this.size1 = size1;
		this.size2 = size2;
		return this;
	}

	/** Modified from (inclusive) up to (exclusive), in milliseconds; null for no limit at that end. */
	SearchCriteria modified(Long from, Long to) {
		this.modifiedFrom = from;
		this.modifiedTo = to;
		return this;
	}

	SearchCriteria type(Type type) {
		this.type = type;
		return this;
	}

	/** True if nothing is set, so everything would match. */
	boolean isEmpty() {
		return name == null && sizeOp == null && modifiedFrom == null && modifiedTo == null && type == null;
	}

	boolean matches(FileSource f) throws IOException {
		if( name != null && !name.matcher(f.getName()).matches()) {
			return false;
		}
		if( type != null ) {
			boolean dir = f.isDirectory();
			if( type == Type.DIRECTORY ? !dir : dir ) {
				return false;
			}
		}
		if( modifiedFrom != null || modifiedTo != null ) {
			long m = f.lastModified();
			if( modifiedFrom != null && m < modifiedFrom ) {
				return false;
			}
			if( modifiedTo != null && m >= modifiedTo ) {
				return false;
			}
		}
		if( sizeOp != null ) {
			if( f.isDirectory()) {
				return false;
			}
			long len = f.length();
			switch( sizeOp ) {
			case AT_MOST:
				return len <= size1;
			case AT_LEAST:
				return len >= size1;
			case EXACTLY:
				return len == size1;
			case BETWEEN:
				return len >= Math.min(size1, size2) && len <= Math.max(size1, size2);
			default:
				return false;
			}
		}
		return true;
	}

	/** A glob as a regular expression: * and ? are wild, the rest is quoted. */
	static String glob(String glob) {
		StringBuilder buf = new StringBuilder();
		StringBuilder literal = new StringBuilder();
		for(int idx=0; idx < glob.length(); idx++) {
			char c = glob.charAt(idx);
			if( c == '*' || c == '?' ) {
				if( literal.length() > 0 ) {
					buf.append(Pattern.quote(literal.toString()));
					literal.setLength(0);
				}
				buf.append(c == '*' ? ".*" : ".");
			} else {
				literal.append(c);
			}
		}
		if( literal.length() > 0 ) {
			buf.append(Pattern.quote(literal.toString()));
		}
		return buf.toString();
	}
}
