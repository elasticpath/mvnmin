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

import java.util.HashSet;
import java.util.Set;

/**
 * Splits a set of module names according to whether they should
 * be included or excluded.  Modules starting with '!' are excluded,
 * all others, included.
 */
public class ModuleRequests {

    private final Set<String> enabledProjects = new HashSet<>();
    private final Set<String> disabledProjects = new HashSet<>();

    /**
     * Creates an instance.
     * @param modules the list of modules
     */
    public ModuleRequests(final Set<String> modules) {
        for (String module : modules) {
            if (module.startsWith("!") || module.startsWith("-")) {
                disabledProjects.add(module.substring(1));
            } else {
                enabledProjects.add(module);
            }
        }
        // Disabling a project has precedence over enabling a project
        enabledProjects.removeAll(disabledProjects);
    }

    /**
     * @return the list of all disabled projects
     */
    public Set<String> getDisabledProjects() {
        return disabledProjects;
    }

    /**
     * @return the list of all enabled projects
     */
    public Set<String> getEnabledProjects() {
        return enabledProjects;
    }
}
