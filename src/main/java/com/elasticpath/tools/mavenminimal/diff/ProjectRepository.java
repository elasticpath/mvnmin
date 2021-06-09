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

import java.util.Set;

/**
 * Abstracts the operations on a project's repository.
 * Allowing for alternate implementations.  This is specifically useful to allow the use of a test double which doesn't use
 * physical file system or scm operations.
 */
public interface ProjectRepository {

	/**
	 * Find all the files in the repository that have changes.
	 * @return a set of all the currently dirty files.
	 */
	Set<String> findDirtyFiles();

	/**
	 * Find all the files in source control change by the specified commit range.
	 * @param commitRange a git commitish string.
	 * @return a set of all the files changed by the commit range.
	 */
	Set<String> gitDiffRange(String commitRange);

	/**
	 * Find all maven project pom.xml files in the repository.
	 * @param maxDepth the maximum folder depth to scan.
	 * @return a set of all the pom.xml files found
	 */
	Set<String> findAllPomFiles(int maxDepth);

	/**
	 * Find the list of project identifiers for the specified changed files.
	 * @param files the set of changed files to find projects for.
	 * @return a Set of project identifiers.
	 */
	Set<String> determineProjectIdsForFilesOrFolders(Set<String> files);

}
