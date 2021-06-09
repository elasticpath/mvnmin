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

import static com.elasticpath.tools.mavenminimal.util.StringUtil.defaultString;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.elasticpath.tools.mavenminimal.pom.PomProject;
import com.elasticpath.tools.mavenminimal.util.Logger;

public class GitFilesystemProjectRepository implements ProjectRepository {

	private static final int GIT_RESULT_MODIFIER_PREFIX_LENGTH = 3;
	private Path repoPath;

	@Override
	public Set<String> findDirtyFiles() {
		try {
			Set<String> results = new HashSet<>();
			Process process = Runtime.getRuntime().exec("git status -s");
			try (Scanner scanner = new Scanner(process.getInputStream()).useDelimiter(System.lineSeparator())) {
				while (scanner.hasNext()) {
					String line = scanner.next();
					results.add(line.substring(GIT_RESULT_MODIFIER_PREFIX_LENGTH));
				}
			}
			return results;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to determine currently dirty files ", e);
		}
	}


	/**
	 * Run git diff, return a list of the changed files.
	 * @param commitRange the commit range in 'git diff' format: "branch..otherbranch"
	 * @return the list of files changed
	 */
	@Override
	public Set<String> gitDiffRange(final String commitRange) {
		try {
			Set<String> results = new HashSet<>();
			Process process = Runtime.getRuntime().exec("git diff --name-only " + commitRange);
			try (Scanner scanner = new Scanner(process.getInputStream()).useDelimiter(System.lineSeparator())) {
				while (scanner.hasNext()) {
					String line = scanner.next();
					results.add(line);
				}
			}
			return results;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to find files in diff for: " + commitRange, e);
		}
	}

	@Override
	public Set<String> findAllPomFiles(final int maxDepth) {
		try {
			Set<String> results;
			results = Files.walk(Paths.get("."), maxDepth)
					.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().equals("pom.xml"))
					.filter(p -> !p.toString().contains(File.separator + "target" + File.separator))  // ignore target folders
					.map(Path::toString)
					.collect(Collectors.toSet());
			return results;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to walk filesystem for all poms", e);
		}
	}


	/**
	 * Find all the projects containing the specified changed files.
	 * @param changedFileOrFolderStrings a set of all the changed files and folders
	 * @return a set of project identifiers
	 */
	public Set<String> determineProjectIdsForFilesOrFolders(final Set<String> changedFileOrFolderStrings) {
		Set<String> results = Collections.synchronizedSet(new HashSet<>());
		changedFileOrFolderStrings.parallelStream().forEach(changedFileOrFolder -> {
			File pomForChangedFile = findPomForChangedFileOrFolder(changedFileOrFolder);
			if (pomForChangedFile != null) {
				try {
					String projectIdentifier = readProjectIdentifierFromPom(pomForChangedFile);
					results.add(projectIdentifier);
				} catch (JAXBException e) {
					throw new IllegalStateException("Failed to parse pom: " + pomForChangedFile, e);
				}
			}
		});
		Logger.debug("found " + results.size() + " projects");
		return results;
	}

	private File findPomForChangedFileOrFolder(final String changedFileOrFolderString) {
		File currentFileOrFolder = new File(defaultString(changedFileOrFolderString));

		File possiblePomFile = getPomFile(changedFileOrFolderString);
		if (possiblePomFile != null) {
			return possiblePomFile;
		}

		if (changedFileOrFolderString == null) {
			return null;  // We're at the top-level and found no pom.xml
		}
		return findPomForChangedFileOrFolder(currentFileOrFolder.getParent());
	}

	private File getPomFile(final String basepath) {
		File cwd = FileSystems.getDefault().getPath("").toAbsolutePath().toFile();

		File pomFile;
		if (basepath == null) {
			pomFile = new File(cwd, "pom.xml");
		} else {
			pomFile = new File(cwd, basepath + File.separator + "pom.xml");
		}
		if (pomFile.exists()) {
			return pomFile;
		}
		return null;
	}

	private  String readProjectIdentifierFromPom(final File pomFile) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(PomProject.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		PomProject pomProject = (PomProject) jaxbUnmarshaller.unmarshal(pomFile);
		String groupId;
		if (pomProject.getGroupId() == null) {
			groupId = pomProject.getParent().getGroupId();
		} else {
			groupId = pomProject.getGroupId();
		}
		return groupId + ":" + pomProject.getArtifactId();
	}

	/**
	 * Return the root of this repository.
	 * @return the root path of this repository.
	 */
	public Path getRepoPath() {
		return repoPath;
	}

	protected void setRepoPath(final Path repoPath) {
		this.repoPath = repoPath;
	}


}
