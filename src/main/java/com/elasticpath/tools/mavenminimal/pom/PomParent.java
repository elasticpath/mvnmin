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


package com.elasticpath.tools.mavenminimal.pom;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model representation of the parent node of a Maven POM file.
 */
@XmlRootElement(name = "parent", namespace = "http://maven.apache.org/POM/4.0.0")
public class PomParent {
	@XmlElement(name = "groupId", namespace = "http://maven.apache.org/POM/4.0.0")
	private String groupId;

	/**
	 * The group ID of the project parent.
	 *
	 * @return the group ID
	 */
	public String getGroupId() {
		return groupId;
	}
}
