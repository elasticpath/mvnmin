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

package com.elasticpath.tools.mavenminimal.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.elasticpath.tools.mavenminimal.reactor.Reactor;
import com.elasticpath.tools.mavenminimal.util.Logger;
import com.elasticpath.tools.mavenminimal.util.ToStringBuilder;

/**
 * Provides a way to pass mvnmin a set of hints about a Maven project's reactor structure.
 * Mvnmin uses these hints to drive the underlying project structure in an optimized manner.
 */
public final class XmlMvnMinConfig {

	public static final String MVNMIN_CONFIG_FILE_NAME = "mvnmin.xml";
	private static final int BUILD_IF_LOOPS = 4;

	private final MvnMinConfigFile config;

	private XmlMvnMinConfig() {
		config = new MvnMinConfigFile();
	}

	private XmlMvnMinConfig(final MvnMinConfigFile config) {
		this.config = config;
	}

	/**
	 * Resolves the build-if project activation, ensuring dependent projects build.
	 * @param modulesRequested the set of modules currently activated.
	 * @return the super set of the input and all dependent projects
	 */
	public List<String> determineBuildIfProjects(final Set<String> modulesRequested) {

		List<String> allDependentModules = new ArrayList<>(modulesRequested);
		List<String> modulesToCheck = new ArrayList<>(allDependentModules);

		for (int x = 0; x  < BUILD_IF_LOOPS; x++) {  // resolve dependencies a few times, to ensure we get all triggers
			// Add dependent projects developers need so everything needed is built
			modulesToCheck.forEach((module) -> getBuildIfModules().forEach((keyModule, dependentModules) -> {
				if (module.matches(keyModule)) {
					allDependentModules.addAll(dependentModules);
				}
			}));
			modulesToCheck.addAll(allDependentModules);
		}
		return allDependentModules;
	}

	/**
	 * @return get a list of the sub-reactors included in this project
	 */
	public List<Reactor> getSubReactors() {
		List<Reactor> reactors = new ArrayList<>();

		int reactorNum = 1;
		if (config.reactors != null) {
			for (MvnMinConfigFile.ReactorDefinition reactor : config.reactors.reactors) {
				if (!reactor.primary) {
					reactors.add(createReactorFromDefinition(reactorNum++, reactor));
				}
			}
		}
		return reactors;
	}

	private Reactor createReactorFromDefinition(final int reactorNum, final MvnMinConfigFile.ReactorDefinition reactor) {
		return new Reactor(reactorNum, reactor.name, reactor.pom, new HashSet<>(reactor.patterns),
				reactor.singleThread, reactor.extraParams, reactor.skipIf);
	}

	/**
	 * @return the list of modules to ignore.
	 */
	public List<String> getModulesToIgnore() {
		return config.ignoredModules == null ? Collections.emptyList() : config.ignoredModules.modules;
	}

	/**
	 * @return a list of the build-if modules.
	 */
	public Map<String, List<String>> getBuildIfModules() {
		Map<String, List<String>> buildIfModules = new HashMap<>();

		if (config.buildIfs != null) {
			for (MvnMinConfigFile.BuildIf buildIf : config.buildIfs.buildIf) {

				for (MvnMinConfigFile.Match keyModule : buildIf.match) {
					buildIfModules.put(keyModule.regex, buildIf.modules);
				}
			}
		}
		return buildIfModules;
	}

	/**
	 * Return the configured maven command.
	 * @return the mvn command to execute (optional)
	 */
	public String getMvnCommand() {
		return config.mvnCommand;
	}


	/**
	 * Loads the configuration from 'mvnmin.xml'.
	 * @return an config object.  This will be an empty instance if the file is missing.
	 */
	public static XmlMvnMinConfig load() {

		File mvnminConfigFile = new File(MVNMIN_CONFIG_FILE_NAME);

		if (!mvnminConfigFile.exists()) {
			Logger.debug(MVNMIN_CONFIG_FILE_NAME + " does not exist, skipping.");
			return new XmlMvnMinConfig();
		}

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(MvnMinConfigFile.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			MvnMinConfigFile configFile = (MvnMinConfigFile) jaxbUnmarshaller.unmarshal(mvnminConfigFile);
			XmlMvnMinConfig config = new XmlMvnMinConfig(configFile);
			Logger.debug(config);
			return config;
		} catch (JAXBException e) {
			throw new MvnMinConfigurationException("Failed to load configuration:", e);
		}
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("config", config)
				.toString();
	}

	/**
	 * @return the primary reactor if defined, null otherwise.
	 */
	public Reactor getPrimaryReactor() {
		MvnMinConfigFile.ReactorDefinition overrideReactorDefinition = new MvnMinConfigFile.ReactorDefinition();
		if (config.reactors != null) {
			for (MvnMinConfigFile.ReactorDefinition reactorDefinition : config.reactors.reactors) {
				if (reactorDefinition.primary) {
					overrideReactorDefinition = reactorDefinition;
					break;
				}
			}
		}

		MvnMinConfigFile.ReactorDefinition primaryReactorDefinition = getDefaultPrimaryReactorDefinition();
		return createReactorFromDefinition(0, primaryReactorDefinition.withOverrides(overrideReactorDefinition));
	}

	private MvnMinConfigFile.ReactorDefinition getDefaultPrimaryReactorDefinition() {
		MvnMinConfigFile.ReactorDefinition defaultPrimaryReactorDefinition = new MvnMinConfigFile.ReactorDefinition();
		defaultPrimaryReactorDefinition.name = "Main reactor";
		defaultPrimaryReactorDefinition.pom = "pom.xml";
		defaultPrimaryReactorDefinition.patterns = Arrays.asList(".*");
		defaultPrimaryReactorDefinition.singleThread = false;
		defaultPrimaryReactorDefinition.extraParams = "";
		defaultPrimaryReactorDefinition.skipIf = "";
		return defaultPrimaryReactorDefinition;
	}

	/**
	 * Model representation of the project node of a Maven POM file.
	 */
	@XmlRootElement(name = "mvnmin")
	static class MvnMinConfigFile {
		@XmlElement(name = "ignored-modules")
		private IgnoredModules ignoredModules;

		@XmlElement(name = "build-ifs")
		private BuildGlue buildIfs;

		@XmlElement(name = "reactors")
		private Reactors reactors;

		@XmlElement(name = "maven-command")
		private String mvnCommand;

		private static class IgnoredModules {
			@XmlElement(name = "module")
			private List<String> modules;

			@Override
			public String toString() {
				return new ToStringBuilder(this)
						.append("modules", modules)
						.toString();
			}
		}

		private static class BuildGlue {
			@XmlElement(name = "build-if")
			private List<BuildIf> buildIf;

			@Override
			public String toString() {
				return new ToStringBuilder(this)
						.append("buildIf", buildIf)
						.toString();
			}
		}

		private static class ReactorDefinition {

			@XmlAttribute(name = "primary")
			private boolean primary;

			@XmlAttribute(name = "name")
			private String name;

			@XmlAttribute(name = "pom")
			private String pom;

			@XmlAttribute(name = "skip-if")
			private String skipIf;

			@XmlAttribute(name = "single-thread")
			private boolean singleThread;

			@XmlAttribute(name = "extra-params")
			private String extraParams;

			@XmlElement(name = "pattern")
			private List<String> patterns = new ArrayList<>();


			@Override
			public String toString() {
				return new ToStringBuilder(this)
						.append("name", name)
						.append("pom", pom)
						.append("skipIf", skipIf)
						.append("singleThread", singleThread)
						.append("extraParams", extraParams)
						.append("patterns", patterns)
						.toString();
			}

			public ReactorDefinition withOverrides(final ReactorDefinition overridingDefinition) {
				ReactorDefinition result = new ReactorDefinition();
				result.name = overrideString(name, overridingDefinition.name);
				result.pom = overrideString(pom, overridingDefinition.pom);
				result.skipIf = overrideString(skipIf, overridingDefinition.skipIf);
				result.extraParams = overrideString(extraParams, overridingDefinition.extraParams);
				result.singleThread = overridingDefinition.singleThread;
				result.patterns = patterns;  // patterns cannot be overridden
				return result;
			}

			private String overrideString(final String defaultString, final String overrideString) {
				if (overrideString == null || overrideString.equals("")) {
					return defaultString;
				}
				return overrideString;
			}
		}

		private static class BuildIf {
			@XmlElement(name = "match")
			private List<Match> match;

			@XmlElement(name = "module")
			private List<String> modules;

			@Override
			public String toString() {
				return new ToStringBuilder(this)
						.append("match", match)
						.append("modules", modules)
						.toString();
			}
		}

		private static class Match {
			@XmlAttribute(name = "regex")
			private String regex;

			@Override
			public String toString() {
				return new ToStringBuilder(this)
						.append("regex", regex)
						.toString();
			}
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this)
					.append("ignoredModules", ignoredModules)
					.append("buildGlue", buildIfs)
					.append("reactors", reactors)
					.toString();
		}
	}

	private static class Reactors {
		@XmlElement(name = "reactor")
		private List<MvnMinConfigFile.ReactorDefinition> reactors;
	}

}

