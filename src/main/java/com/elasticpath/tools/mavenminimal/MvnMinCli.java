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

package com.elasticpath.tools.mavenminimal;

import static com.elasticpath.tools.mavenminimal.util.StringUtil.defaultString;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.Set;

import org.fusesource.jansi.internal.CLibrary;

import com.elasticpath.tools.mavenminimal.config.XmlMvnMinConfig;
import com.elasticpath.tools.mavenminimal.diff.RepoDiffBuilder;
import com.elasticpath.tools.mavenminimal.reactor.ExtendedReactor;
import com.elasticpath.tools.mavenminimal.reactor.MavenDriver;
import com.elasticpath.tools.mavenminimal.reactor.ModuleRequests;
import com.elasticpath.tools.mavenminimal.reactor.Reactor;
import com.elasticpath.tools.mavenminimal.reactor.ReactorPrinter;
import com.elasticpath.tools.mavenminimal.diff.ProjectRepository;
import com.elasticpath.tools.mavenminimal.diff.GitFilesystemProjectRepository;
import com.elasticpath.tools.mavenminimal.util.Logger;

/**
 * The main Maven Minimal class.
 */
public class MvnMinCli {

	private static final String MVNMIN_MAXDEPTH_ENVVAR = "MVNMIN_MAXDEPTHS";

	private static final int DEFAULT_MAX_DEPTH = 6;
	private static final String MAIN_BRANCH = "master";

	private static boolean allPomMode;
	private static boolean diffCommitMode;
	private static String commitDiffArg = MAIN_BRANCH;
	private static boolean dryRunMode;
	private static boolean printMode;
	private static boolean versionMode;
	private static boolean buildIfEnabled = true;
	private static String resumeFromModule;

	/**
	 * The default command line entry point for mvnmin.
	 *
	 * This method ALWAYS calls System.exit() as it's last step.  If you don't want this, look at run() instead.
	 *
	 * @param args the commmand line arguments
	 */
	public static void main(final String[] args) {
		int mavenExitValue = run(new GitFilesystemProjectRepository(), args, System.out, true);
		exit(mavenExitValue);
	}

	/**
	 * A secondary entry point for mnvmin.  The cli should call main().  run() is more for programmatic invocation
	 * of mvnmin.
	 *
	 * Within the project this is used to launch mvnmin for testing.
	 *
	 * @param projectRepository the RepoOperations instance to operate upon.
	 * @param args the arguments to drive mvnmin's different functions.
	 * @param out a PrintStream where all output should go.
	 * @param enabledStdIn reading from stdin can cause hangs if not corrently handled - this flag allows that do be disabled.
	 * @return 0 if mvnmin runs successfully, >1 otherwise.  If maven is actually invoked then its exit value is returned.
	 */
	public static int run(final ProjectRepository projectRepository, final String[] args, final PrintStream out, final boolean enabledStdIn) {

		Logger.init(out);

		parseArgs(args, out);

		if (versionMode) {
			out.println(getVersionString());
			return 0;
		}

		List<ModuleRequests> moduleRequests = determineRequestedModules(projectRepository, args, enabledStdIn);
		XmlMvnMinConfig mvnMinConfig = XmlMvnMinConfig.load();
		ExtendedReactor reactor = new ExtendedReactor(mvnMinConfig, moduleRequests, buildIfEnabled);

		if (printMode) {
			out.println(projectsAsString(reactor.getModules()));
			return 0;
		}

		if (reactor.getModules().isEmpty()) {
			if (outputIsATerminal()) {
				out.println("No modified project files detected. This usually means that you don't have any"
						+ " uncommitted changes in the repo.");
			}
			return 1;
		}

		return executeMavenOnReactors(reactor, args, dryRunMode, mvnMinConfig, out);
	}

	private static String getVersionString() {
		return "mvnmin " + defaultString(MvnMinCli.class.getPackage().getImplementationVersion(), "version unknown");
	}

	private static List<ModuleRequests> determineRequestedModules(
			final ProjectRepository repository, final String[] args, final boolean enableStdIn) {
		List<ModuleRequests> moduleRequests = new ArrayList<>();
		if (enableStdIn) {
			moduleRequests.add(getProjectsFromStdin());
		}
		moduleRequests.add(getProjectsFromArgs(args));
		moduleRequests.add(findChangedProjectsIds(repository));
		return moduleRequests;
	}

	private static ModuleRequests findChangedProjectsIds(final ProjectRepository repository) {
		RepoDiffBuilder diffSource = new RepoDiffBuilder();
		if (allPomMode) {
			diffSource.withAllPomFiles();
			diffSource.withMaxDepth(getMaxDepthSetting());
		} else {
			diffSource.withAllCurrentlyDirtyFiles();
			if (diffCommitMode) {
				diffSource.withFilesChangedInDiff(commitDiffArg);
			}
		}
		return new ModuleRequests(diffSource.diff(repository));
	}

	/**
	 * Parses the incoming arguments, setting flags as appropriate.
	 * @param args the args from the command line.
	 * @param out the PrintStream to report any issues to.
	 */
	private static void parseArgs(final String[] args, final PrintStream out) {
		boolean parsingResumeFrom = false;
		for (String arg : args) {
			if (parsingResumeFrom) {
				parsingResumeFrom = false;
				resumeFromModule = arg;
			} else {
				if (arg.matches("--all")) {
					allPomMode = true;
				} else if (arg.equals("-d") || arg.equals("--dry-run")) {
					dryRunMode = true;
				} else if (arg.matches("--diff.*")) {
					int equalsIndex = arg.indexOf('=');
					if (equalsIndex > -1 && equalsIndex + 1 < arg.length()) {
						commitDiffArg = arg.substring(equalsIndex + 1);
					}
					if (!commitDiffArg.contains("..")) {
						commitDiffArg += "..";
					}
					diffCommitMode = true;
				} else if (arg.matches("--help")) {
					printUsage(out);
					exit(0);
				} else if (arg.equals("-p")) {
					printMode = true;
				} else if (arg.equals("--nbi")) {
					buildIfEnabled = false;
				} else if (arg.equals("--version")) {
					versionMode = true;
				} else if (arg.equals("-f") || arg.equals("--file")) {
					out.println("The options '-f' and '--file' are not supported by mvnmin, exiting.");
					exit(1);
				} else if (arg.equals("-rf") || arg.equals("--resume-from")) {
					parsingResumeFrom = true;
					// the next param is out project
				}
			}
		}
	}

	private static void printUsage(final PrintStream out) {
		out.println("usage: mvmin [options] [<maven goal(s)>] [<maven phase(s)>] [<maven arg(s)>]");
		out.println();
		out.println("  Project Activation/Deactivation");
		out.println("    --all                      Activate all `pom.xml` files in all sub directories");
		out.println("                               (default max depth: " + DEFAULT_MAX_DEPTH + ")");
		out.println("    --diff[=commit[..commit]]  Activate all projects changed since the specified commit, ");
		out.println("                               or range of specified commits.");
		out.println("                               (default: 'master')");
		out.println("    -pl,--projects <arg>       Comma-delimited list of specified reactor projects");
		out.println("                               to build as well as those otherwise activated.");
		out.println("                               A project can be specified by `groupId:artifactId`");
		out.println("                               A project can be deactivated by leading with an");
		out.println("                               exclamation mark or hyphen: `-groupId:artifactId`");
		out.println("    --nbi                      No build-if dependencies are considered, just ");
		out.println("                               changed modules.");
		out.println();
		out.println("  Intercepted Maven Options:");
		out.println("    -rf,--resume-from <arg>    Resume reactor from specified project (and sub-reactor).");
		out.println("    -f,--file <arg>            Not supported, mvnmin will exit.");
		out.println();
		out.println("  Scripting");
		out.println("    -p                         Don't invoke maven, print out activated projects,");
		out.println("                               sorted, newline separated.");
		out.println();
		out.println("  Debug");
		out.println("    -d --dry-run               Don't invoke maven, print out the commands that");
		out.println("                               would have been executed.");
		out.println("       --version               Print the version number of mvnmin and exit.");
		out.println();

	}




	private static String projectsAsString(final Set<String> projectIds) {
		List<String> sorted = new ArrayList<>(projectIds);
		Collections.sort(sorted);
		return String.join("\n", sorted);
	}

	private static int getMaxDepthSetting() {
		try {
			return Integer.parseInt(System.getenv(MVNMIN_MAXDEPTH_ENVVAR));
		} catch (NumberFormatException nfe) {
			return DEFAULT_MAX_DEPTH;
		}
	}

	private static boolean outputIsATerminal() {
		return CLibrary.isatty(1) == 1;
	}

	/**
	 * Parse out the -pl and --projects arguments passed into mvnmin via command line arg.
	 * @param args the arguments passed to mvnmin
	 * @return the modules requested
	 */
	private static ModuleRequests getProjectsFromArgs(final String[] args) {
		Set<String> modulesRequestedParam = new HashSet<>();

		for (int x = 0; x < args.length; x++) {
			if ("-pl".equals(args[x]) || "--projects".equals(args[x])) {
				if (x + 1 < args.length) {  // we need the next arg, make sure it's there first
					try (Scanner inputScanner = new Scanner(args[ x + 1 ])) {
						inputScanner.useDelimiter(",|$");
						while (inputScanner.hasNext()) {
							modulesRequestedParam.add(inputScanner.next());
						}
					}
				}
			}
		}
		return new ModuleRequests(modulesRequestedParam);
	}

	private static ModuleRequests getProjectsFromStdin() {
		Set<String> modulesRequestedStdin = new HashSet<>();

		final int stdin = 0;
		if (CLibrary.isatty(stdin) == 1) {  // stdin is the terminal, not a file/stream ignore
			return new ModuleRequests(modulesRequestedStdin);
		}

		try (Scanner scanner = new Scanner(System.in)) {
			scanner.useDelimiter("\n|$");
			while (scanner.hasNext()) {
				modulesRequestedStdin.add(scanner.next());
			}
		}
		return new ModuleRequests(modulesRequestedStdin);
	}

	private static int executeMavenOnReactors(final ExtendedReactor xreactor, final String[] args, final boolean dryRun,
			final XmlMvnMinConfig mvnMinConfig, final PrintStream out) {
		List<String> filteredArgs = removeNonMavenArgs(args);

		OptionalInt maxReactorNameLength = xreactor.getSubReactors().stream()
				.map(Reactor::getReactorName)
				.mapToInt(String::length)
				.max();

		ReactorPrinter printer = new ReactorPrinter(maxReactorNameLength.getAsInt(), out);
		printer.newline();     // add some spacing

		// Skip the reactors before the one containing the resume-from module, let the rest run.
		if (resumeFromModule != null) {
			boolean resumeFromActivated = false;
			for (Reactor subReactor : xreactor.getSubReactors()) {
				if (!resumeFromActivated) {
					for (String activeModule : subReactor.getActiveModules()) {  // can't simply do `contains()`
						Logger.debug(activeModule);
						if (activeModule.contains(resumeFromModule)) { // look for the abbreviated module name
							resumeFromActivated = true;
						} else {
							Logger.debug("Skipping " + subReactor.getReactorName()
									+ " looking for resume-from module: " + resumeFromModule);
							subReactor.setSkipReactor(true);
						}
					}
				}
			}
		}

		int mavenExitValue = 0;
		for (Reactor subReactor : xreactor.getSubReactors()) {
			if (mavenExitValue == 0) {
				mavenExitValue =
						MavenDriver.runMvnForReactor(
								subReactor, filteredArgs, mvnMinConfig.getMvnCommand(),
								dryRun, printer, resumeFromModule);
				printer.newline();
			} else {
				out.println("mvnmin: Maven failed to run successfully.");
				break;
			}
		}
		return mavenExitValue;
	}

	private static List<String> removeNonMavenArgs(final String[] args) {
		List<String> mavenArguments = new ArrayList<>(Arrays.asList(args));
		removeArgPair(mavenArguments, "-pl");
		removeArgPair(mavenArguments, "--projects");
		removeArgPair(mavenArguments, "-rf");
		removeArgPair(mavenArguments, "--resume-from");

		mavenArguments.removeIf(s -> s.equals("--all"));
		mavenArguments.removeIf(s -> s.equals("--d"));
		mavenArguments.removeIf(s -> s.matches("--diff.*"));
		mavenArguments.removeIf(s -> s.equals("--dry-run"));
		mavenArguments.removeIf(s -> s.equals("--nbi"));
		mavenArguments.removeIf(s -> s.equals("-p"));
		mavenArguments.removeIf(s -> s.equals("--version"));
		return mavenArguments;
	}

	private static void removeArgPair(final List<String> mavenArguments, final String arg) {
		int argIndex = mavenArguments.indexOf(arg);
		if (argIndex > -1) {
			// Remove twice to delete the arg marker and the value
			mavenArguments.remove(argIndex);
			mavenArguments.remove(argIndex);
		}
	}

	private static void exit(final int mavenExitValue) {
		System.exit(mavenExitValue);
	}

}
