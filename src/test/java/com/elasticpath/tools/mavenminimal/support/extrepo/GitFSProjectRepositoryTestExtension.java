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

package com.elasticpath.tools.mavenminimal.support.extrepo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.elasticpath.tools.mavenminimal.diff.GitFilesystemProjectRepository;
import com.elasticpath.tools.mavenminimal.support.GitRepoTestHarness;
import com.elasticpath.tools.mavenminimal.util.Pair;

/**
 * An extension to the prod git repo that actually creates a real git repo for testing purposes..
 */
public class GitFSProjectRepositoryTestExtension extends GitFilesystemProjectRepository implements ProjectRepositoryTestExtension {


	public void create() throws IOException, InterruptedException {
		setRepoPath(Files.createTempDirectory(getClass().getName()));
		getRepoPath().toFile().deleteOnExit();
		exec(getRepoPath().toFile(), "git", "init", ".");
	}

	public RepoFile newRepoFile(final String path, final String filename) {
		if (path == null || path.equals("")) {
			return new RepoFile(getRepoPath().toAbsolutePath() + "", filename);
		}
		return new RepoFile(getRepoPath().toAbsolutePath() + File.separator + path, filename);
	}

	@Override
	public Pair<Integer, String> exec(String ... args)
			throws IOException, InterruptedException {
		return exec(getRepoPath().toFile(), args);
	}

	private static Pair<Integer, String> exec(final File repoRoot, final String ... commands) throws IOException, InterruptedException {
		ProcessBuilder builder =
				new ProcessBuilder()
						.directory(repoRoot)
						.inheritIO()
						.command(commands);
		System.out.println(String.join(" ", commands));
		Process process = builder.start();
		process.waitFor();

		BufferedReader reader =
				new BufferedReader(new InputStreamReader(process.getInputStream()));
		StringBuilder strBuilder = new StringBuilder();
		String line;
		while ( (line = reader.readLine()) != null) {
			strBuilder.append(line);
			strBuilder.append(System.getProperty("line.separator"));
		}
		return Pair.of(process.exitValue(), strBuilder.toString());

	}

	@Override
	public void doWriteFile(final RepoFile fileToChange, final String contents) {
		try {
			Files.createDirectories(Paths.get(File.separator + fileToChange.getPath()));
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileToChange.asFile()));
			writer.write(contents);
			writer.close();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to change file:" + fileToChange.getAbsolutePath(), e);
		}
	}

	@Override
	public void assertFileExists(RepoFile fileToChange, GitRepoTestHarness gitRepoTestHarness) {
		if (!fileToChange.asFile().exists()) {
			throw new IllegalArgumentException("pom.xml file to change doesn't exist: " + fileToChange.getAbsolutePath());
		}
	}
}
