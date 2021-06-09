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

package com.elasticpath.tools.mavenminimal.diff;

import java.util.HashSet;
import java.util.Set;

import com.elasticpath.tools.mavenminimal.util.Logger;

/**
 * Builds up options to perform a diff on a ProjectRepository, and performs that diff.
 */
public class RepoDiffBuilder {

	private String commitish;
	private boolean includeDirtyFiles;
	private boolean includeAllPoms;
	private int maxDepth;

	/**
	 * Force all pom.xml files to be included.
	 * @return this instance.
	 */
	public RepoDiffBuilder withAllPomFiles() {
		includeAllPoms = true;
		return this;
	}

	/**
	 * @param maxDepth the maximum number of folders down to go
	 * @return this instance.
	 */
	public RepoDiffBuilder withMaxDepth(final int maxDepth) {
		this.maxDepth = maxDepth;
		return this;
	}

	/**
	 * @param commitish include files changes in the supplied git diff.
	 * @return this instance.
	 */
	public RepoDiffBuilder withFilesChangedInDiff(final String commitish) {
		this.commitish = commitish;
		return this;
	}

	/**
	 * Include all currently dirty file system files.
	 * @return this instance.
	 */
	public RepoDiffBuilder withAllCurrentlyDirtyFiles() {
		includeDirtyFiles = true;
		return this;
	}

	/**
	 * Find the set of differences.
	 * @param projectRepository the repository to run the diff against.
	 * @return the set of changed files.
	 */
	public Set<String> diff(final ProjectRepository projectRepository) {
		final Set<String> files = new HashSet<>();

		if (commitish != null) {
			files.addAll(projectRepository.gitDiffRange(commitish));
		}

		if (includeDirtyFiles) {
			files.addAll(projectRepository.findDirtyFiles());
		}

		if (includeAllPoms) {
			files.addAll(projectRepository.findAllPomFiles(maxDepth));
		}

		Logger.debug(files.toString());
		return projectRepository.determineProjectIdsForFilesOrFolders(files);
	}

}
