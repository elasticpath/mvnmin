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

package com.elasticpath.tools.mavenminimal.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.elasticpath.tools.mavenminimal.support.extrepo.ProjectRepositoryTestExtension;
import com.elasticpath.tools.mavenminimal.support.extrepo.ProjectRepositoryTestExtension.RepoFile;
import com.elasticpath.tools.mavenminimal.diff.ProjectRepository;
import com.elasticpath.tools.mavenminimal.util.ToStringBuilder;

public class GitRepoTestHarness {

	private static final String POM_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
			+ "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
			+ "<modelVersion>4.0.0</modelVersion>\n"
			+ "<groupId>@GROUP@</groupId>\n"
			+ "<artifactId>@ARTIFACT@</artifactId>\n"
			+ "<version>0.0.1-SNAPSHOT</version>\n"
			+ "</project>\n";

	private final List<String> activePoms = new ArrayList<>();
	private final Map<String, String> files = new HashMap<>();
	private String lastPom;
	private final Map<String, String> projectRoots = new HashMap<>();
	private ProjectRepositoryTestExtension repoOperations;

	public GitRepoTestHarness() {
		this.repoOperations = new InMemoryProjectRepositoryTestExtension();
		init();
	}

	public GitRepoTestHarness(final ProjectRepositoryTestExtension repoOperations) {
		this.repoOperations = repoOperations;
		init();
	}

	private void init() {
		try {
			repoOperations.create();
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Couldn't create repo", e);
		}
	}

	public File rootDirectory() {
		return repoOperations.getRepoPath().toFile();
	}

	public void commitAll() {
		try {
			repoOperations.exec("git", "add", "--all");
			repoOperations.exec("git", "commit", "-a", "-m 'commit'");
		} catch (IOException | InterruptedException e) {
			throw new IllegalStateException("Unable to commit all", e);
		}
		activePoms.clear();
	}

	public void changePom(final String path, final String artifactId) {
		RepoFile fileToChange = repoOperations.newRepoFile(path, "pom.xml");
		repoOperations.assertFileExists(fileToChange, this);
		activePoms.add(files.get(fileToChange.getAbsolutePath()));
		String contents = POM_TEMPLATE.replace("@GROUP@", "group").replace("@ARTIFACT@", artifactId);
		contents += "<!-- faked change -->";
		repoOperations.doWriteFile(fileToChange, contents);
	}

	public String getActivePoms() {
		Collections.sort(activePoms);
		return String.join("\n", activePoms) + "\n";
	}

	public Map<String, String> getProjectRoots() {
		return projectRoots;
	}

	public ProjectRepository getRepoOperations() {
		return repoOperations;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("activePoms", activePoms)
				.append("files", files)
				.append("lastPom", lastPom)
				.append("projectRoots", projectRoots)
				.append("operations", repoOperations)
				.toString();
	}

	class InMemoryProjectRepositoryTestExtension implements ProjectRepositoryTestExtension {

		public InMemoryProjectRepositoryTestExtension() {
		}

		@Override
		public Set<String> findDirtyFiles() {
			return new HashSet<>(files.keySet());
		}

		@Override
		public Set<String> gitDiffRange(final String commitRange) {
			return null;
		}

		@Override
		public Set<String> findAllPomFiles(final int maxDepth) {
			return null;
		}

		@Override
		public Set<String> determineProjectIdsForFilesOrFolders(final Set<String> files) {
			return new HashSet<>(activePoms);
		}

		@Override
		public Path getRepoPath() {
			return null;
		}

		private boolean fileExists(RepoFile file) {return files.containsKey(file.getAbsolutePath());}

		@Override
		public void assertFileExists(RepoFile fileToChange, GitRepoTestHarness gitRepoTestHarness) {
			if (!fileExists(fileToChange)) {
				throw new IllegalArgumentException("pom.xml file to change doesn't exist: " + fileToChange.getAbsolutePath());
			}
		}
	}


	public void writePom(final String path, final String artifactId) {
		projectRoots.put(artifactId, path);
		lastPom = "group:" + artifactId;

		String contents = POM_TEMPLATE.replace("@GROUP@", "group").replace("@ARTIFACT@", artifactId);
		writeNewFile(path, "pom.xml", contents);
	}

	public void writeNewFile(final String path, final String filename) {
		writeNewFile(path, filename, "test file");
	}

	private void writeNewFile(final String path, final String filename, final String contents) {
		RepoFile rfile = repoOperations.newRepoFile(path, filename);
		files.put(rfile.getAbsolutePath(), lastPom);

		repoOperations.doWriteFile(rfile, contents);
	}

	public void changeFile(final String filename) {
		RepoFile fileToChange = repoOperations.newRepoFile("", filename);
		repoOperations.assertFileExists(fileToChange, this);
		activePoms.add(files.get(fileToChange.getAbsolutePath()));
		repoOperations.doWriteFile(fileToChange, "changed test file");
	}

}
