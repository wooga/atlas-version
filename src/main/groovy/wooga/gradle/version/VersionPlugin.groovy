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

import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.slf4j.Logger
import wooga.gradle.version.internal.DefaultVersionCodeExtension
import wooga.gradle.version.internal.DefaultVersionPluginExtension
import wooga.gradle.version.internal.ToStringProvider

class VersionPlugin implements Plugin<Project> {
    static String EXTENSION_NAME = "versionBuilder"
    static Logger logger = Logging.getLogger(VersionPlugin)

    @Override
    void apply(Project project) {

        if (project != project.getRootProject()) {
            logger.warn("net.wooga.atlas-version can only be applied to the root project.")
            return
        }

        // TODO: Does this have to be evaluated here?
        String gitRoot = project.properties.getOrDefault(VersionPluginConventions.GIT_ROOT_PROPERTY, project.rootProject.projectDir.path)
        VersionPluginExtension extension = createAndConfigureExtension(project)
        setProjectVersion(gitRoot, project, extension)
    }

    private static void setProjectVersion(String gitRoot, Project project, VersionPluginExtension extension) {
        try {
            Grgit git = Grgit.open(dir: gitRoot)
            project.gradle.buildFinished {
                project.logger.info "Closing Git repo: ${git.repository.rootDir}"
                git.close()
            }
            extension.git.set(git)
        }
        catch (RepositoryNotFoundException e) {
            project.logger.warn("Git repository not found at $gitRoot ")
        }
        finally {
            def sharedVersion = new ToStringProvider(extension.version.map({ it.version }))
            project.allprojects{ Project prj ->
                prj.setVersion(sharedVersion)
                prj.extensions.create(VersionCodeExtension, "versionCode", DefaultVersionCodeExtension.class, prj, extension.versionCode)
            }
        }
    }

    protected static VersionPluginExtension createAndConfigureExtension(Project project) {
        def extension = project.extensions.create(VersionPluginExtension, EXTENSION_NAME, DefaultVersionPluginExtension, project)

        extension.versionScheme.set(VersionPluginConventions.versionScheme.getStringValueProvider(project).map({
            VersionSchemes.valueOf(it.trim())
        }))

        extension.versionCodeScheme.set(VersionPluginConventions.versionCodeScheme.getStringValueProvider(project).map({
            VersionCodeScheme.valueOf(it.trim())
        }))

        extension.versionCodeOffset.set(VersionPluginConventions.versionCodeOffset.getIntegerValueProvider(project))
        extension.prefix.convention("v")
        return extension
    }
}
