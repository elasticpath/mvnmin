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
 * A simple builder for toStrng methods.
 */
public class ToStringBuilder {
	private final StringBuilder builder;

	/**
	 * Create a new builder for object.
	 * @param object the object to build a toString for, must not be null or a NullPointerException will be thrown.
	 */
	public ToStringBuilder(final Object object) {
		if (object == null) {
			throw new NullPointerException("object cannot be null");
		}
		builder = new StringBuilder();
		builder.append(object.getClass().getSimpleName())
				.append("@")
				.append(System.identityHashCode(object))
				.append(" [\n");
	}

	/**
	 * Append the field and value.
	 * @param field the name of the field
	 * @param value the value of the field
	 * @return this builder
	 */
	public ToStringBuilder append(final String field, final Object value) {
		builder.append("  ")
				.append(field)
				.append(":")
				.append(value)
				.append("\n");
		return this;
	}

	/**
	 * @return the constructed value for toString()
	 */
	public String toString() {
		builder.append("]");
		return builder.toString();
	}
}
