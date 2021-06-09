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

import static com.elasticpath.tools.mavenminimal.support.launcher.MvnMinLauncher.assertMvnMin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.PropertyDefaults;
import net.jqwik.api.Provide;

import com.elasticpath.tools.mavenminimal.support.GitRepoTestHarness;
import com.elasticpath.tools.mavenminimal.support.generator.FakeProjectGenerator;

/**
 * Tests various invariant properties of mvnmin.  This is done with this Property-Based (vs. Example-based) tests.
 *
 * More information on Property-Based testing can be found here: https://jqwik.net/
 */
@PropertyDefaults(tries=10)  // real git repos are a little slow to create, constrain the number of tries by default.
public class MvnMinCliTest {

	@Property
	void cleanReposNeverHaveActiveProjects(@ForAll("cleanRepos") GitRepoTestHarness cleanRepo) throws IOException, InterruptedException {
		assertMvnMin(cleanRepo,new String [] {"-p"}, 0, "\n");
		// We need a different approach to test for the NO_MODIFICATIONS_FOUND_MESSAGE message - mvnmin doesn't output
		// that when it's streaming it's output - so the output is clean for use in scripting.
	}

	@Property
	void anyCombinationOfChangedFilesActivesTheClosestParentPom(@ForAll("cleanRepos") GitRepoTestHarness repo)
			throws IOException, InterruptedException {

		FakeProjectGenerator.dirtyProjectFiles(repo);

		assertMvnMin(repo,new String [] {"-p"}, 0, repo.getActivePoms());

		repo.commitAll();

		assertMvnMin(repo,new String [] {"-p"} , 0, "\n");
	}

	@Property
	void anyChangesToPomsActivateProjects(@ForAll("cleanRepos") GitRepoTestHarness repo)
			throws IOException, InterruptedException {

		FakeProjectGenerator.changeArbitraryPomFiles(repo);

		assertMvnMin(repo,new String [] {"-p"} , 0, repo.getActivePoms());
	}

	/**
	 * Validate that activating projects via command arg, and via stdin are equivalent.
	 * In set theory, the output is a union of the two inputs.
	 *
	 * @param repo a set of clean repos to test against.
	 * @throws IOException if unexpected issues arise
	 * @throws InterruptedException if unexpected issues arise
	 */
	@Property
	void sameProjectsActivatedViaStdinAndOrArgument(@ForAll("cleanRepos") GitRepoTestHarness repo)
			throws IOException, InterruptedException {

		Map<String, String> projectRoots = repo.getProjectRoots();

		int numToActivate = Arbitraries.integers().between(0, projectRoots.keySet().size()).sample();

		Set<String> projectsToActivateStdin = new HashSet<>();
		for (int x = 0; x < numToActivate; x++) {
			projectsToActivateStdin.add("group:" + Arbitraries.oneOf(Arbitraries.of(projectRoots.keySet())).sample());
		}

		String projectsToActivateArgument = String.join(",", projectsToActivateStdin);

		List<String> sortedProjects = new ArrayList<>(projectsToActivateStdin);
		Collections.sort(sortedProjects);
		String expectedOutput = String.join("\n", sortedProjects) + "\n";

		int expectedReturnValue = 0;

		//  The following activations should all be equivalent
		assertMvnMin(repo, new String [] {"-p", "-pl", projectsToActivateArgument}, expectedReturnValue, expectedOutput);
		assertMvnMin(repo, new String [] {"-p", "--projects", projectsToActivateArgument}, expectedReturnValue, expectedOutput);

		// Check stdin activation
		assertMvnMin(repo, new String [] {"-p"}, projectsToActivateStdin, expectedReturnValue, expectedOutput);

		// Check union of stdin and arg params
		assertMvnMin(repo, new String [] {"-p", "--pl", projectsToActivateArgument}, projectsToActivateStdin, expectedReturnValue, expectedOutput);
		assertMvnMin(repo, new String [] {"-p", "--projects", projectsToActivateArgument}, projectsToActivateStdin, expectedReturnValue, expectedOutput);

	}


	@Provide
	Arbitrary<GitRepoTestHarness> cleanRepos()  {
		return FakeProjectGenerator.repositories(false);
	}

}
