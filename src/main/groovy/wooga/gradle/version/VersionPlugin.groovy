/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.version

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.slf4j.Logger
import wooga.gradle.version.internal.DefaultVersionPluginExtension
import wooga.gradle.version.tasks.Version

class VersionPlugin implements Plugin<Project> {

    static Logger logger = Logging.getLogger(VersionPlugin)

    static String EXTENSION_NAME = "version"

    @Override
    void apply(Project project) {
        def extension = create_and_configure_extension(project)

        def exampleTask = project.tasks.create("example", Version)
        exampleTask.group = "version"
        exampleTask.description = "example task"

    }

    protected static VersionPluginExtension create_and_configure_extension(Project project) {
        def extension = project.extensions.create(VersionPluginExtension, EXTENSION_NAME, DefaultVersionPluginExtension, project)

        extension
    }
}
