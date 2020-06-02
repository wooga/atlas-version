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
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.slf4j.Logger
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

        String gitRoot = project.properties.getOrDefault(VersionConsts.GIT_ROOT_PROPERTY, project.rootProject.projectDir.path)
        VersionPluginExtension extension = create_and_configure_extension(project)
        try {
            Grgit git = Grgit.open(dir: gitRoot)
            project.gradle.buildFinished {
                project.logger.info "Closing Git repo: ${git.repository.rootDir}"
                git.close()
            }
            extension.git.set(git)
        }
        catch(RepositoryNotFoundException e) {
            project.logger.warn("Git repository not found at $gitRoot ")
        }
        finally {
            def sharedVersion = new ToStringProvider(extension.version.map({it.version}))
            project.allprojects(new Action<Project>() {
                @Override
                void execute(Project prj) {
                    prj.setVersion(sharedVersion)
                }
            })
        }
    }

    protected static VersionPluginExtension create_and_configure_extension(Project project) {
        def extension = project.extensions.create(VersionPluginExtension, EXTENSION_NAME, DefaultVersionPluginExtension, project)

        extension.versionScheme.set(project.provider({
            def rawValue = System.getenv()[VersionConsts.VERSION_SCHEME_ENV_VAR] ?:
                    project.properties.getOrDefault(VersionConsts.VERSION_SCHEME_OPTION,
                            project.properties.get(VersionConsts.LEGACY_VERSION_SCHEME_OPTION))
            if(rawValue) {
                return VersionScheme.valueOf(rawValue.toString().trim())
            }
            VersionConsts.VERSION_SCHEME_DEFAULT
        }))

        extension
    }
}
