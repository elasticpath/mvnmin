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

package com.elasticpath.tools.mavenminimal.support.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

import com.elasticpath.tools.mavenminimal.support.GitRepoTestHarness;
import com.elasticpath.tools.mavenminimal.support.extrepo.GitFSProjectRepositoryTestExtension;

public class FakeProjectGenerator {


	public static final String javaPackage = "com/example/tools";
	public static final String javaSourceFolder = "src/main/java";
	public static final String [] fileNames = {"Example.java", "ExampleUtil.java", "ExampleCli.java"};

	public static Arbitrary<Arbitrary<String>> projectNames() {
		Arbitrary<Character> firstChars = Arbitraries.chars().range('a', 'z').range('A', 'Z');

		Arbitrary<String> remainders = Arbitraries.strings()
				.ofMinLength(1)
				.ofMaxLength(15)
				.withCharRange('A', 'Z')
				.withCharRange('a', 'z')
				.withChars("-_.")               // Don't include commas, they are used to separate maven projects on the command-line
				.filter(s -> !s.equals('.'))
				.filter(s -> !s.equals(".."));  // Exclude special file-system names



		return Arbitraries.of(Combinators.combine(firstChars, remainders).as((f, r) -> f + r));
	}

	public static Arbitrary<GitRepoTestHarness> repositories(boolean inMemory) {
		Arbitrary<Integer> maxChildren = Arbitraries.integers().between(3, 5);
		Arbitrary<Integer> depth = Arbitraries.integers().between(1, 10);
		Arbitrary<Arbitrary<String>> projectNames = FakeProjectGenerator.projectNames();
		Arbitrary<Random> random = Arbitraries.randoms();

		if (inMemory) {
			return Combinators.combine(random, maxChildren, depth, projectNames).as(FakeProjectGenerator::generateRepoInMemory);
		}
		return Combinators.combine(random, maxChildren, depth, projectNames).as(FakeProjectGenerator::generateRepo);
	}


	public static GitRepoTestHarness generateRepo(final Random random, final int childrenPerLevel, final int depth,
			final Arbitrary<String> projectNames) {

		GitRepoTestHarness repo = new GitRepoTestHarness(new GitFSProjectRepositoryTestExtension());
		new FakeProjectGenerator().genProjectRootStructure(random, repo, childrenPerLevel, depth, projectNames);
		repo.commitAll();
		return repo;
	}

	public static GitRepoTestHarness generateRepoInMemory(final Random random, int childrenPerLevel, int depth, Arbitrary<String> projectNames) {
		GitRepoTestHarness repo = new GitRepoTestHarness();
		new FakeProjectGenerator().genProjectRootStructure(random, repo, childrenPerLevel, depth, projectNames);
		repo.commitAll();
		return repo;
	}

	public static void dirtyProjectFiles(final GitRepoTestHarness repo) {
		Map<String, String> projectRoots = repo.getProjectRoots();
		Arbitraries.oneOf(Arbitraries.of(projectRoots.values())).forEachValue(v -> changeArbitraryProjectFile(v, repo));
	}

	public static void changeArbitraryPomFiles(final GitRepoTestHarness repo) {
		Map<String, String> projectRoots = repo.getProjectRoots();
		Arbitraries.oneOf(Arbitraries.of(projectRoots.keySet())).forEachValue((v) -> repo.changePom(projectRoots.get(v), v));
	}

	/**
	 * Dirty an arbitrary java file
	 */
	private static void changeArbitraryProjectFile(final String rootPath, final GitRepoTestHarness repo) {
		String filename = Arbitraries.oneOf(Arbitraries.of(fileNames)).sample();
		repo.changeFile(String.join(File.separator,
				rootPath, javaSourceFolder, javaPackage, filename));
	}

	private void genProjectRootStructure(final Random random, final GitRepoTestHarness repoHarness, final int maxChildren, final int depth,
			final Arbitrary<String> projectNames) {
		TreeGenerator<String> gen = new TreeGenerator<>(random, maxChildren);
		TreeGenerator<String>.TreeNode tree = gen.createTree(depth, projectNames);
		subPaths(tree).forEach( project -> writeMavenJavaProject(project, repoHarness));
	}

	private  void writeMavenJavaProject(final String rootPath, GitRepoTestHarness repoHarness) {
		Arrays.stream(fileNames).forEach( f -> repoHarness.writeNewFile(String.join(File.separator, rootPath, javaSourceFolder, javaPackage), f));
	}

	private List<String> subPaths(TreeGenerator<String>.TreeNode root) {
		if (root.getChildren().isEmpty()) {
			return Collections.singletonList(root.getValue());
		}

		List<String> pathsSoFar = new ArrayList<>();
		for (TreeGenerator<String>.TreeNode node :  root.getChildren()) {
			for (String subPath : subPaths(node)) {
				pathsSoFar.add(root.getValue() + File.separator + subPath);
			}
		}
		return pathsSoFar;
	}

}
