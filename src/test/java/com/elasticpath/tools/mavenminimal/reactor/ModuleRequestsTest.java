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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Validate that ModuleRequests are combined appropriately
 */
class ModuleRequestsTest {

	@Test
	void testEmptyRequests() {
		ModuleRequests requests = new ModuleRequests(Collections.emptySet());
		assertThat(Collections.emptySet()).isEqualTo(requests.getEnabledProjects());
		assertThat(Collections.emptySet()).isEqualTo(requests.getDisabledProjects());
	}

	@Test
	void testEnableSingleProject() {
		Set<String> moduleRequests = new HashSet<>(Arrays.asList("a"));

		ModuleRequests requests = new ModuleRequests(moduleRequests);
		assertThat(Collections.singleton("a")).isEqualTo(requests.getEnabledProjects());
		assertThat(Collections.emptySet()).isEqualTo(requests.getDisabledProjects());
	}

	@Test
	void testDisableSingleProjectWithHyphen() {
		Set<String> moduleRequests = new HashSet<>(Arrays.asList("-a"));

		ModuleRequests requests = new ModuleRequests(moduleRequests);
		assertThat(Collections.emptySet()).isEqualTo(requests.getEnabledProjects());
		assertThat(Collections.singleton("a")).isEqualTo(requests.getDisabledProjects());
	}

	@Test
	void testDisableSingleProjectWithPling() {
		Set<String> moduleRequests = new HashSet<>(Arrays.asList("!a"));

		ModuleRequests requests = new ModuleRequests(moduleRequests);
		assertThat(Collections.emptySet()).isEqualTo(requests.getEnabledProjects());
		assertThat(Collections.singleton("a")).isEqualTo(requests.getDisabledProjects());
	}


	@Test
	void testDisablingOverridesEnabling() {
		Set<String> moduleRequests = new HashSet<>(Arrays.asList("!a", "a"));

		ModuleRequests requests = new ModuleRequests(moduleRequests);
		assertThat(Collections.emptySet()).isEqualTo(requests.getEnabledProjects());
		assertThat(Collections.singleton("a")).isEqualTo(requests.getDisabledProjects());

		moduleRequests = new HashSet<>(Arrays.asList("a", "-a"));

		requests = new ModuleRequests(moduleRequests);
		assertThat(Collections.emptySet()).isEqualTo(requests.getEnabledProjects());
		assertThat(Collections.singleton("a")).isEqualTo(requests.getDisabledProjects());
	}

}
