/*	Copyright 2021 Elastic Path Software Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/


package com.elasticpath.tools.mavenminimal.util;

/**
 * A few simple String util methods.
 */
public final class StringUtil {

	// Prevent instantiation
	private StringUtil() { }

	/**
	 * @param value the value to test
	 * @return value if not null, empty string "" otherwise
	 */
	public static String defaultString(final String value) {
		return defaultString(value, "");
	}

	/**
	 * @param value the value to test
	 * @param defaultValue the default value if value is null
	 * @return value if not null, defaultValue otherwise
	 */
	public static String defaultString(final String value, final String defaultValue) {
		return value == null ? defaultValue : value;
	}

	/**
	 * @param value the value to test
	 * @return true if value has content, false if it is null or empty string ""
	 */
	public static boolean isNotBlank(final String value) {
		return !(value == null || "".equals(value));
	}
}
