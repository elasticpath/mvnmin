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

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import com.elasticpath.tools.mavenminimal.util.ToStringBuilder;


/**
 * Represent a maven reactor, as mvnmin needs to understand it.
 */
public class Reactor {

	private final int reactorNumber;
	private final String skipReactorIf;
	private final String reactorName;
	private boolean skipReactor;
	private final Set<String> modulesInReactor;
	private final String pathFromProjectRoot;
	private final boolean singleThread;
	private final String extraParams;
	private final Set<String> activeModules = new HashSet<>();

	/**
	 * Creates a reactor instance.
	 * @param aReactorNumber the ordering of the reactor
	 * @param reactorName a short name for the reactor
	 * @param pomPathFromProjectRoot the path of this reactor's pom, from the root of the larger project
	 * @param modulesInReactor a list of the modules in this reactor
	 * @param singleThread whether to restrict this reactor to only run single threaded.
	 * @param extraParams extra parameters to pass to maven when running this reactor.
	 * @param skipReactorIf a regex, which if present in the args will cause the reactor to skip.
	 */
	public Reactor(final int aReactorNumber, final String reactorName, final String pomPathFromProjectRoot, final Set<String> modulesInReactor,
			final Boolean singleThread,
			final String extraParams,
			final String skipReactorIf) {
		this.reactorNumber = aReactorNumber;
		this.reactorName = reactorName;
		this.pathFromProjectRoot = pomPathFromProjectRoot;
		this.modulesInReactor = Collections.unmodifiableSet(modulesInReactor);
		this.singleThread = singleThread;
		this.extraParams = extraParams;
		this.skipReactorIf = skipReactorIf;
	}

	/**
	 * @return the project's pom location from the root of the multi-maven project
	 */
	public String getPomLocation() {
		return pathFromProjectRoot;
	}

	/**
	 * @return the number of the reactor
	 */
	public int getReactorNumber() {
		return reactorNumber;
	}

	/**
	 * @return the name of the reactor
	 */
	public String getReactorName() {
		return reactorName;
	}

	/**
	 * Have this Reactor 'claim' its modules from the superset mvnmin knows about.
	 * It does this by matching against the patterns provided.
	 * This is a destructive operation, any claimed modules will be removed from the input set.
	 * @param modules the list of modules to collect this Reactor's from
	 */
	public void consumeActiveModules(final Set<String> modules) {
		for (Iterator<String> iter = modules.iterator(); iter.hasNext();) {
			String module = iter.next();
			if (moduleBelongsToReactor(module)) {
				activeModules.add(module);
				iter.remove(); // mutate the incoming list
			}
		}
	}

	/**
	 * Determine if this Reactor currently has any active modules.  That is, is it worth building?
	 * @return true if this consumed a module
	 */
	public boolean hasActiveModules() {
		return activeModules.size() > 0;
	}

	/**
	 * @param skipReactor true to skip building this reactor, false to have it
	 */
	public void setSkipReactor(final boolean skipReactor) {
		this.skipReactor = skipReactor;
	}

	/**
	 * @return the set of active modules
	 */
	public Set<String> getActiveModules() {
		return activeModules;
	}

	/**
	 * @return true if this Reactor must run in single-threaded mode, false otherwise.
	 */
	public boolean isSingleThread() {
		return singleThread;
	}

	/**
	 * @return the arbitrary maven parameters to add to the maven invocation.
	 */
	public String getExtraParams() {
		return extraParams;
	}

	/**
	 * @return the pattern that, if matched, would cause this Reactor to skip building
	 */
	public String getSkipReactorIf() {
		return skipReactorIf;
	}

	private boolean moduleBelongsToReactor(final String module) {
		for (String modulePattern : modulesInReactor) {
			if (Pattern.matches(modulePattern, module)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("reactorNumber", reactorNumber)
				.append("skipReactor", skipReactor)
				.append("modulesInReactor", modulesInReactor)
				.append("pathFromProjectRoot", pathFromProjectRoot)
				.append("singleThread", singleThread)
				.append("activeModules", activeModules)
				.append("extraParams", extraParams)
				.toString();
	}

	/**
	 * @return true if this Reactor has active modules, and isn't skipped.
	 */
	public boolean shouldBuild() {
		return !skipReactor && hasActiveModules();
	}
}
