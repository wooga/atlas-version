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

import com.wooga.gradle.extensions.ProviderExtensions
import org.ajoberstar.grgit.Grgit
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import wooga.gradle.version.internal.DefaultVersionCodeExtension
import wooga.gradle.version.internal.DefaultVersionPluginExtension
import wooga.gradle.version.internal.ToStringProvider
import wooga.gradle.version.internal.VersionCode
import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.version.internal.release.semver.ChangeScope

import java.util.stream.Stream

class VersionPlugin implements Plugin<Project> {
    static String EXTENSION_NAME = "versionBuilder"
    static String VERSION_CODE_EXTENSION_NAME = "versionCode"

    @Override
    void apply(Project project) {
        VersionPluginExtension extension = createAndConfigureExtension(project)
        setProjectVersion(project, extension)
    }

    static void applyOnCurrentAndSubProjects(Project project, Closure operation) {
        operation(project)
        project.childProjects.values().each { prj ->
            operation(prj)
            applyOnCurrentAndSubProjects(prj, operation)
        }
        project.parent
    }

    private static void setProjectVersion(Project project, VersionPluginExtension extension) {
        def sharedVersion = new ToStringProvider(extension.version.map({ it.version }))
        applyOnCurrentAndSubProjects(project) { Project prj ->
            prj.setVersion(sharedVersion)
            def versionCodeExt = project.extensions.findByName(VERSION_CODE_EXTENSION_NAME) as VersionCodeExtension
            if(!versionCodeExt) {
                versionCodeExt = DefaultVersionCodeExtension.empty(prj, VERSION_CODE_EXTENSION_NAME)
            }
            versionCodeExt.convention(extension.versionCode)
        }
    }

    static File findNearestGitFolder(File projectDir, int maxDepth) {
        if(maxDepth > 0) {
            def maybeDotGit = Stream.of(projectDir.listFiles( { File file ->
                file.directory && file.name == ".git"
            } as FileFilter)).findFirst()

            return maybeDotGit.orElseGet {
                findNearestGitFolder(projectDir.parentFile, maxDepth-1)
            }
        }
        return null
    }

    protected static VersionPluginExtension createAndConfigureExtension(Project project) {

        def extension = project.extensions.create(VersionPluginExtension, EXTENSION_NAME, DefaultVersionPluginExtension, project)

        Provider<String> gitRoot = VersionPluginConventions.gitRoot.getStringValueProvider(project)
        .orElse(VersionPluginConventions.maxGitRootSearchDepth.getIntegerValueProvider(project).map {
            findNearestGitFolder(project.projectDir, it)?.absolutePath
        })

        extension.git.convention(ProviderExtensions.mapOnce(gitRoot) { String it ->
            try {
                Grgit git = Grgit.open(dir: it)
                project.gradle.buildFinished {
                    project.logger.info "Closing Git repo: ${git.repository.rootDir}"
                    git.close()
                }
                return git
            } catch (RepositoryNotFoundException ignore) {
                project.logger.warn("Git repository not found at $gitRoot ")
            }
        })

        extension.versionScheme.convention(VersionPluginConventions.versionScheme.getStringValueProvider(project).map({
            VersionSchemes.valueOf(it.trim())
        }))

        extension.versionCodeScheme.convention(VersionPluginConventions.versionCodeScheme.getStringValueProvider(project).map({
            VersionCodeSchemes.valueOf(it.trim())
        }))

        extension.versionCodeOffset.convention(VersionPluginConventions.versionCodeOffset.getIntegerValueProvider(project))

        extension.prefix.convention(VersionPluginConventions.prefix.getStringValueProvider(project))
        extension.stage.convention(VersionPluginConventions.stage.getStringValueProvider(project))
        extension.releaseBranchPattern.convention(VersionPluginConventions.releaseBranchPattern.getStringValueProvider(project))
        extension.mainBranchPattern.convention(VersionPluginConventions.mainBranchPattern.getStringValueProvider(project))

        extension.scope.convention(VersionPluginConventions.scope.getStringValueProvider(project)
                .map {it?.trim()?.empty? null: it }
                .map { ChangeScope.valueOf(it.toUpperCase()) })

        extension.version.convention(VersionPluginConventions.version.getStringValueProvider(project)
            .map {new ReleaseVersion(version: it) }
            .orElse(inferVersionIfGitIsPresent(project, extension))
            .orElse(new ReleaseVersion(version: VersionPluginConventions.UNINITIALIZED_VERSION))
        )

        extension.versionCode.convention(extension.versionCodeScheme.map({ VersionCodeSchemes scheme ->
            def version = extension.version.map { it.version }.orNull
            return VersionCode.Schemes
                    .fromExternal(scheme)
                    .versionCodeFor(version, extension.versionRepo.orNull, extension.versionCodeOffset.getOrElse(0))
        }.memoize()))

        extension.stage.convention(VersionPluginConventions.stage.getStringValueProvider(project))
        extension.releaseBranchPattern.set(VersionPluginConventions.releaseBranchPattern.getStringValueProvider(project))
        extension.mainBranchPattern.set(VersionPluginConventions.mainBranchPattern.getStringValueProvider(project))

        return extension
    }

    private static Provider<ReleaseVersion> inferVersionIfGitIsPresent(Project project, VersionPluginExtension extension) {
        def inferredVersion = ProviderExtensions.mapOnce(extension.git) { Grgit git ->
            def version = extension.inferVersion(extension.versionScheme.get(), extension.stage, extension.scope)
                    .orElse(project.provider {
                        throw new GradleException('No version strategies were selected. Run build with --info for more detail.')
                    }).get()
            project.logger.warn('Inferred project: {}, version: {}', project.name, version.version)
            return version
        }  as Provider<ReleaseVersion>

        return inferredVersion.orElse(new ReleaseVersion(version: VersionPluginConventions.UNINITIALIZED_VERSION))
    }
}
