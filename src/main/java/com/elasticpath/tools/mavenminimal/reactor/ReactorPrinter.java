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

import java.io.PrintStream;

import org.apache.commons.exec.CommandLine;

import com.elasticpath.tools.mavenminimal.util.Logger;

public class ReactorPrinter {

	private final PrintStream out;

	private final int maxReactorNameLength;

	/**
	 * Create a new printer.
	 * @param maxReactorNameLength the maximum number of characters of the reactor name to print out.
	 * @param out the PrintStream to print to.
	 */
	public ReactorPrinter(final int maxReactorNameLength, final PrintStream out) {
		this.maxReactorNameLength = maxReactorNameLength;
		this.out = out;
	}

	/**
	 * Output a newline.
	 */
	public void newline() {
		out.println();
	}

	/**
	 * Print a human friendly summary of the command that would execute for the specified reactor.
	 * @param reactor the reactor to summarize the command for
	 * @param command the command to summarize
	 */
	public void commandSummary(final Reactor reactor, final CommandLine command) {
		String action = reactor.shouldBuild() ? "RUN" : "SKIP";
		Logger.debug(command);
		out.println(
				String.format("%-4.4s %s %-" + maxReactorNameLength + "s : %s",
						action,
						reactor.getReactorNumber(),
						trim(reactor.getReactorName()),
						String.join(" ", command.toStrings())));
	}

	/**
	 * Report that the maven command couldn't be executed.
	 * @param command the name of the command that was attempted
	 */
	public void commandNotExecutable(final String command) {
		out.println(String.format("Failed to execute '%s', either it couldn't be found, or it isn't executable.", command));
	}

	private String trim(final String input) {
		return input.substring(0, Math.min(input.length(), maxReactorNameLength));
	}

}
