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

package com.elasticpath.tools.mavenminimal.support.extrepo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.elasticpath.tools.mavenminimal.diff.ProjectRepository;
import com.elasticpath.tools.mavenminimal.support.GitRepoTestHarness;
import com.elasticpath.tools.mavenminimal.util.Pair;

/**
 * And extended interface provide methods to expose for test purposes.
 */
public interface ProjectRepositoryTestExtension extends ProjectRepository {

	default RepoFile newRepoFile(String path, String filename) {
		return new RepoFile(path, filename);
	}

	default void create() throws IOException, InterruptedException {}

	default Pair<Integer, String> exec(String ... args) throws IOException, InterruptedException {
		return null;
	}

	default void doWriteFile(RepoFile fileToChange, String contents) {
	}

	Path getRepoPath();

	void assertFileExists(RepoFile fileToChange, GitRepoTestHarness gitRepoTestHarness);

	class RepoFile {

		private final String base;
		private final String filename;

		public RepoFile(String base, String filename) {
			this.base = base;
			this.filename = filename;
		}

		public File asFile() {
			return new File(base, filename);
		}

		public String getAbsolutePath() {
			if (base == null || base.equals("")) {
				if (filename == null) {
					throw new IllegalStateException("null filename isn't allowed");
				}
				return filename;
			}
			return base + File.separator + filename;
		}

		public String getPath() {
			return base;
		}
	}

}
