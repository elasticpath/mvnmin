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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.elasticpath.tools.mavenminimal.config.XmlMvnMinConfig;
import com.elasticpath.tools.mavenminimal.util.Logger;

public class ExtendedReactor {

	private final List<Reactor> subReactorsToBuild;
	private final Set<String> modulesToBuild;
	private final XmlMvnMinConfig mvnMinConfig;

	/**
	 * @param mvnMinConfig The hints to drive the reactor differently.
	 * @param moduleRequests a list of groups of module requests to consider for activation/deactivation.
	 * @param buildIfEnabled true if the reactor should consider build-if dependencies, false to ignore them.
	 */
	public ExtendedReactor(final XmlMvnMinConfig mvnMinConfig, final List<ModuleRequests> moduleRequests, final boolean buildIfEnabled) {
		this.mvnMinConfig = mvnMinConfig;
		Set<String> mods = determineModulesToBuild(moduleRequests, buildIfEnabled);
		modulesToBuild = new HashSet<>(mods);
		Logger.debug("modules to build: " + modulesToBuild);
		subReactorsToBuild = getReactorsToBuild(mods);
	}

	/**
	 * @return a list of the subreactors known by this Extended Reactor
	 */
	public List<Reactor> getSubReactors() {
		return subReactorsToBuild;
	}

	/**
	 * @return a list of all modules known by this ExtendedReactor
	 */
	public Set<String> getModules() {
		return modulesToBuild;
	}

	private Set<String> determineModulesToBuild(final List<ModuleRequests> moduleRequests, final boolean buildIfEnabled) {
		Set<String> modulesToBuild = new HashSet<>();

		moduleRequests.forEach(m -> modulesToBuild.addAll(m.getEnabledProjects()));
		moduleRequests.forEach(m -> modulesToBuild.removeAll(m.getDisabledProjects()));

		if (buildIfEnabled) {
			modulesToBuild.addAll(mvnMinConfig.determineBuildIfProjects(modulesToBuild));
		}

		// Some modules can break the reactor  (Needs fixing, or needs some 'additionalProjects" glue and some explaining)
		modulesToBuild.removeAll(mvnMinConfig.getModulesToIgnore());
		return modulesToBuild;
	}


	/**
	 * Organizes and populates all reactors.
	 *
	 * @param modulesToBuild the modules that the user has request be built.
	 * @return a build-ordered list of reactors.
	 */
	private List<Reactor> getReactorsToBuild(final Set<String> modulesToBuild) {
		List<Reactor> subReactorsToBuild = new ArrayList<>();
		for (Reactor reactor : mvnMinConfig.getSubReactors()) {
			reactor.consumeActiveModules(modulesToBuild);
			subReactorsToBuild.add(reactor);
		}

		Reactor primaryReactor = mvnMinConfig.getPrimaryReactor();
		primaryReactor.consumeActiveModules(modulesToBuild);

		// Put the primary reactor at the start of the list
		subReactorsToBuild.add(0, primaryReactor);
		if (!primaryReactor.hasActiveModules()) {
			primaryReactor.setSkipReactor(true);
		}

		subReactorsToBuild.forEach(Logger::debug);
		return subReactorsToBuild;
	}

}
