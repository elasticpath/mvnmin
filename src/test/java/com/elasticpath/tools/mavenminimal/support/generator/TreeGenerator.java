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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.jqwik.api.Arbitrary;

/**
 * A simple tree generator used for creating project tree structures,
 * @param <T> the type to be stored within the tree.
 */
public class TreeGenerator<T> {

	private final int maxChildren;
	private final Random random;

	public TreeGenerator(final Random random, final int maxChildren) {
		this.random = random;
		this.maxChildren = maxChildren;
	}

	/**
	 * Creates a tree, with each node value provided by valueProvider.
	 * @param depth the depth of the tree to generate
	 * @param valueGenerator provides the value for each node
	 * @return
	 */
	public TreeNode createTree(int depth, Arbitrary<T> valueGenerator) {
		TreeNode node = new TreeNode();
		node.value = valueGenerator.sample();
		if (depth > 0) {
			int children = random.nextInt(maxChildren);
			for (int i = 0; i < children; ++i) {
				node.children.add(createTree(depth - 1, valueGenerator));
			}
		}
		return node;
	}


	public class TreeNode {
		private T value;
		private final List<TreeNode> children;

		public TreeNode() {
			children = new ArrayList<>();
		}

		public T getValue() {
			return value;
		}

		public List<TreeNode> getChildren() {
			return children;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder(50);
			print(buffer, "", "");
			return buffer.toString();
		}

		private void print(StringBuilder buffer, String prefix, String childrenPrefix) {
			buffer.append(prefix);
			buffer.append(value);
			buffer.append('\n');
			for (Iterator<TreeNode> it = children.iterator(); it.hasNext();) {
				TreeNode next = it.next();
				if (it.hasNext()) {
					next.print(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ");
				} else {
					next.print(buffer, childrenPrefix + "└── ", childrenPrefix + "    ");
				}
			}
		}
	}

}
