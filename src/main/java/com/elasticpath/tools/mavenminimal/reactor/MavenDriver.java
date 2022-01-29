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

package com.elasticpath.tools.mavenminimal.reactor;

import static com.elasticpath.tools.mavenminimal.util.StringUtil.defaultString;
import static com.elasticpath.tools.mavenminimal.util.StringUtil.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import com.elasticpath.tools.mavenminimal.util.Logger;

/**
 * Service for determining what maven command to execute.
 */
public class MavenDriver {
	private static final Map<String, String> ARGUMENT_TRANSFORMATIONS = new HashMap<>();
	private static final Map<String, String> ARGUMENT_TO_GOAL_TRANSFORMATIONS = new HashMap<>();
	private static final String MVN_COMMAND_ENV_ARG = "MVN_COMMAND";

	static {
		ARGUMENT_TRANSFORMATIONS.put("--debugsurefire",
				"-Dmaven.surefire.debug=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
						+ " -Xnoagent -Djava.compiler=NONE\"");
		ARGUMENT_TO_GOAL_TRANSFORMATIONS.put("--debugsurefire", "test");
		ARGUMENT_TRANSFORMATIONS.put("--debugfailsafe",
				"-Dmaven.failsafe.debug=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
						+ " -Xnoagent -Djava.compiler=NONE\"");
		ARGUMENT_TO_GOAL_TRANSFORMATIONS.put("--debugfailsafe", "test");
	}

	/**
	 * Runs maven for the specified reactor.
	 * @param reactor the reactor to build
	 * @param args command line arguments to consider when invoking maven.
	 * @param overrideMvnCommand an override to use for a maven command
	 * @param dryRun false to invoke maven, true to skip
	 * @param printer the output printer
	 * @return the exit status of the maven invocation (or zero if dryRun is true)
	 */
	public static int runMvnForReactor(
			final Reactor reactor, final List<String> args, final String overrideMvnCommand,
			final boolean dryRun, final ReactorPrinter printer, final String resumeFromModule) {
		CommandLine command = determineMavenCommand(reactor, args, overrideMvnCommand, resumeFromModule);
		printer.commandSummary(reactor, command);

		if (dryRun || !reactor.shouldBuild()) {
			return 0;
		}

		return executeMaven(command, printer);
	}

	private static int executeMaven(final CommandLine command, final ReactorPrinter printer) {
		DefaultExecutor executor = new DefaultExecutor();
		try {
			return executor.execute(command, createCustomizedSubProcessEnvironment());
		} catch (IOException e) {
			Logger.debug("Failed to execute maven", e);
			if (e.getMessage().contains(command.getExecutable())) {
				// Mentioning the executable in the exception likely
				// means the command couldn't be found.
				printer.commandNotExecutable(command.getExecutable());
			}
		}
		return 1;
	}

	/**
	 * Determines what Maven command to execute based on the passed parameters.
	 *
	 * @param reactor the list of project IDs to be passed to maven to build
	 * @param inputArgs the command line arguments that were passed to the tool
	 * @param overrideMvnCommand the mvn command to used (can be null)
	 * @return the Maven command to execute
	 */
	private static CommandLine determineMavenCommand(final Reactor reactor, final List<String> inputArgs, final String overrideMvnCommand, final String resumeFromModule) {
		AtomicReference<String> goal = new AtomicReference<>("");

		List<String> mavenMinimalArguments = new ArrayList<>(inputArgs);
		List<String> mavenArguments = new ArrayList<>();

		mavenMinimalArguments.forEach(argument -> {
			mavenArguments.add(ARGUMENT_TRANSFORMATIONS.getOrDefault(argument, argument));
			if (ARGUMENT_TO_GOAL_TRANSFORMATIONS.containsKey(argument)) {
				goal.set(ARGUMENT_TO_GOAL_TRANSFORMATIONS.get(argument));
			}
		});

		// Handle profiles like -P\!cm
		mavenMinimalArguments.forEach(arg -> {
			if (reactor.getSkipReactorIf() != null && arg.matches(reactor.getSkipReactorIf())) {
				reactor.setSkipReactor(true);
			}
		});

		mavenArguments.add("-f ");
		mavenArguments.add(reactor.getPomLocation());

		if (isNotBlank(reactor.getExtraParams())) {
			mavenArguments.addAll(Arrays.asList(defaultString(reactor.getExtraParams()).split(" ")));
		}

		if (reactor.isSingleThread()) {
			removeThreadingFlags(mavenArguments);
			mavenArguments.add("-T1");
		}

		if (reactor.hasActiveModules()) {
			mavenArguments.add("--projects");
			mavenArguments.add(String.join(",", reactor.getActiveModules()));
		}

		// Skip the reactors before the one containing the resume-from module, let the rest run.
		for (String activeModule : reactor.getActiveModules()) {  // can't simply do `contains()`
			if (activeModule.contains(resumeFromModule)) { // look for the abbreviated module name
				mavenArguments.add("-rf");
				mavenArguments.add(resumeFromModule);
				break;
			}
		}

		String mvnCommand = determineMvnExecutable(overrideMvnCommand);

		CommandLine cmdLine = new CommandLine(mvnCommand);
		if (isNotBlank(goal.get())) {
			cmdLine.addArgument(goal.get());
		}

		cmdLine.addArguments(mavenArguments.toArray(new String[] {}), false);

		return cmdLine;
	}


	/**
	 * The number of threads can be specified in several ways for Maven, each possibility needs to be removed.
	 * @param mavenArguments the arguments to pass to maven.
	 */
	static void removeThreadingFlags(final List<String> mavenArguments) {

		mavenArguments.removeIf(s -> s != null && s.matches("(-T.+.*|--threads=.*)"));

		for (Iterator<String> iter = mavenArguments.iterator(); iter.hasNext();) {
			String arg = iter.next();
			if (arg.matches("(-T|--threads)")) {
				iter.remove();          // Remove the flag
				if (iter.hasNext()) {   // Remove the argument
					iter.next();
					iter.remove();
				}
			}
		}
	}

	private static String determineMvnExecutable(final String overrideMvnCommand) {
		String mvnCommand;
		String envOverrideMvnCommand = System.getenv(MVN_COMMAND_ENV_ARG);

		if (envOverrideMvnCommand != null) {
			mvnCommand = envOverrideMvnCommand;   // use environment variable override if specified
		} else if (isNotBlank(overrideMvnCommand)) {
			mvnCommand = overrideMvnCommand;      // use the mvn command from the mvnmin properties if specified
		} else {
			File cwd = FileSystems.getDefault().getPath("").toAbsolutePath().toFile();  // check for maven wrapper
			File mvnwFile = new File(cwd, "mvnw");
			if (mvnwFile.exists()) {
				mvnCommand = "./mvnw";
			} else {
				mvnCommand = "mvn";  // use "mvn" on the PATH if no maven wrapper
			}
		}
		return mvnCommand;
	}

	private static Map<String, String> createCustomizedSubProcessEnvironment() {
		Map<String, String> subprocessEnv = new HashMap<>(System.getenv());
		preserveTerminalColoring(subprocessEnv);
		return subprocessEnv;
	}

	private static void preserveTerminalColoring(final Map<String, String> subProcessEnv) {
		String mavenOpts = defaultString(subProcessEnv.get("MAVEN_OPTS"));
		mavenOpts = mavenOpts.concat(" -Djansi.passthrough=true").trim();
		Logger.debug("Running with MAVEN_OPTS = " + mavenOpts);
		subProcessEnv.put("MAVEN_OPTS", mavenOpts);
	}

}
