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

package com.elasticpath.tools.mavenminimal.support.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.elasticpath.tools.mavenminimal.MvnMinCli;
import com.elasticpath.tools.mavenminimal.support.GitRepoTestHarness;
import com.elasticpath.tools.mavenminimal.util.Logger;
import com.elasticpath.tools.mavenminimal.util.Pair;

/**
 * Drives mvnmin command line interface against real git repositories.
 *
 * Provides test assertions to validate the correct output is produced as expected.
 */
public class MvnMinLauncher {


	public static void assertMvnMin(final GitRepoTestHarness repository, final String[] mvnminArgs, final int expectedReturnValue, final String expectedOutput)
			throws IOException, InterruptedException {
		assertMvnMin(repository, mvnminArgs, null, expectedReturnValue, expectedOutput);
	}

	public static void assertMvnMin(final GitRepoTestHarness repository, final String[] mvnminArgs, Set<String> projectsToActivateStdin,
			final int expectedReturnValue,
			final String expectedOutput)
			throws IOException, InterruptedException {

		Pair<Integer, String> mvnmin = mvnmin(repository, mvnminArgs, projectsToActivateStdin);
		assertEquals(expectedOutput, mvnmin.getRight(), "mvnmin output not as expected");
		assertEquals(expectedReturnValue, mvnmin.getLeft(), "mvnmin exit code not as expected");
	}

	public static void assertMvnMinInMem(final GitRepoTestHarness repository, final String[] mvnminArgs,
			final int expectedReturnValue,
			final String expectedOutput) {

		Pair<Integer, String> mvnmin = mvnminInMem(repository, mvnminArgs);
		assertEquals(expectedOutput, mvnmin.getRight(), "mvnmin output not as expected");
		assertEquals(expectedReturnValue, mvnmin.getLeft(), "mvnmin exit code not as expected");
	}


	private static Pair<Integer, String> mvnminInMem(final GitRepoTestHarness repository, final String[] mvnminArgs) {
		final String utf8 = StandardCharsets.UTF_8.name();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			int exitCode = MvnMinCli.run(repository.getRepoOperations(), mvnminArgs, new PrintStream(baos, true, utf8),
					false /* Disable stdin for testing, for now */);
			return Pair.of(exitCode, baos.toString(utf8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Problem encoding the output stream", e);
		}
	}


	private static Pair<Integer, String> mvnmin(final GitRepoTestHarness repository, final String [] args,
			final Set<String> projectsToActivateStdin) throws IOException,
			InterruptedException {
		String [] commands = {"java",
				"-classpath", System.getProperty("java.class.path"),
				MvnMinCli.class.getName()};
		List<String> finalCommand = new ArrayList<>(Arrays.asList(commands));
		finalCommand.addAll(Arrays.asList(args));
		ProcessBuilder builder = new ProcessBuilder().directory(repository.rootDirectory()).command(finalCommand);

		Process process = builder.start();
		if (projectsToActivateStdin == null) {
			process.getOutputStream().close();  // close stdin
		} else {
			Thread t = new Thread(() -> {
				PrintWriter writer = new PrintWriter(process.getOutputStream());
				projectsToActivateStdin.forEach(s -> writer.write(s + "\n"));
				writer.close();
			});
			t.start();
		}
		final BufferedReader reader =
				new BufferedReader(new InputStreamReader(process.getInputStream()));
		final StringBuilder strBuilder = new StringBuilder();

		boolean done = false;

		Thread t = new Thread(() -> {
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					strBuilder.append(line);
					strBuilder.append(System.getProperty("line.separator"));
				}
			} catch (IOException ioe) {
				Logger.debug("Failed reading mvnmin output", ioe);
			}
		});

		t.start();

		while (!done) {
			try {
				done = process.waitFor(100, TimeUnit.MILLISECONDS);
			} catch (InterruptedException ie) {
				// ignore, we'll come around again.
			}
		}

		t.join();

		return Pair.of(process.exitValue(), strBuilder.toString());
	}
}
