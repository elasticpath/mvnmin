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
 * Model representation of the project node of a Maven POM file.
 */
@XmlRootElement(name = "project", namespace = PomProject.NAMESPACE)
public class PomProject {

	static final String NAMESPACE = "http://maven.apache.org/POM/4.0.0";


	@XmlElement(name = "parent", namespace = NAMESPACE)
	private PomParent parent;

	@XmlElement(name = "groupId", namespace = NAMESPACE)
	private String groupId;

	@XmlElement(name = "artifactId", namespace = NAMESPACE)
	private String artifactId;


	/**
	 * The parent node of the project.
	 *
	 * @return the PomParent model
	 */
	public PomParent getParent() {
		return parent;
	}

	/**
	 * The group ID of the project.
	 *
	 * @return the group ID
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * The artifact ID of the project.
	 *
	 * @return the artifact ID
	 */
	public String getArtifactId() {
		return artifactId;
	}
}
