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

import static com.elasticpath.tools.mavenminimal.support.launcher.MvnMinLauncher.assertMvnMinInMem;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import com.elasticpath.tools.mavenminimal.support.GitRepoTestHarness;
import com.elasticpath.tools.mavenminimal.support.generator.FakeProjectGenerator;

/**
 * Tests various invariant properties of mvnmin.  This is done with this Property-Based (vs. Example-based) tests.
 *
 * More information on Property-Based testing can be found here: https://jqwik.net/
 */
public class MvnMinTest {

	@Property
	void cleanReposNeverHaveActiveProjects(@ForAll("cleanRepos") GitRepoTestHarness cleanRepo) {
		assertMvnMinInMem(cleanRepo, new String[] { "-p" }, 0, "\n");
	}

	@Property
	void anyCombinationOfChangedFilesActivesTheClosestParentPom(@ForAll("cleanRepos") GitRepoTestHarness repo) {

		FakeProjectGenerator.dirtyProjectFiles(repo);

		assertMvnMinInMem(repo, new String [] {"-p"}, 0, repo.getActivePoms());

		repo.commitAll();
		assertMvnMinInMem(repo, new String [] {"-p"} , 0, "\n");
	}

	@Property
	void anyChangesToPomsActivateProjects(@ForAll("cleanRepos") GitRepoTestHarness repo) {

		FakeProjectGenerator.changeArbitraryPomFiles(repo);

		assertMvnMinInMem(repo,new String [] {"-p"} , 0, repo.getActivePoms());
	}

	@Provide
	Arbitrary<GitRepoTestHarness> cleanRepos()  {
		return FakeProjectGenerator.repositories(true);
	}

}
