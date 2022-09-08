/*
 * Copyright 2018-2020 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package wooga.gradle.version.internal

import org.ajoberstar.grgit.Grgit
import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.VersionCodeScheme
import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.version.internal.release.semver.ChangeScope
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import wooga.gradle.version.VersionPluginConventions
import wooga.gradle.version.VersionPluginExtension

class DefaultVersionPluginExtension implements VersionPluginExtension {

    private final Project project;

    DefaultVersionPluginExtension(Project project) {
        this.project = project

        scope = VersionPluginConventions.scope.getStringValueProvider(project).map {
            ChangeScope.valueOf(it.toUpperCase())
        }

        stage = VersionPluginConventions.stage.getStringValueProvider(project)
        releaseBranchPattern.set(VersionPluginConventions.releaseBranchPattern.getStringValueProvider(project))
        mainBranchPattern.set(VersionPluginConventions.mainBranchPattern.getStringValueProvider(project))

        version = VersionPluginConventions.version.getStringValueProvider(project).map({
            new ReleaseVersion(version: it)
        }).orElse(inferVersionIfGitIsPresent())
                .orElse(new ReleaseVersion(version: VersionPluginConventions.UNINITIALIZED_VERSION))

        versionCode = versionCodeScheme.map({
            VersionCodeScheme scheme -> scheme.versionCodeFor(this.version.map { it.version }, git, versionCodeOffset.getOrElse(0))
        }.memoize())

        // It's development if the development strategy contains the set `stage` OR
        // if the default strategy's release stage is development
        isDevelopment = canRunStageWithName(ReleaseStage.Development, stage)
        isSnapshot = canRunStageWithName(ReleaseStage.Snapshot, stage)
        isPrerelease = canRunStageWithName(ReleaseStage.Prerelease, stage)
        isFinal = canRunStageWithName(ReleaseStage.Final, stage)
    }

    private Provider<Boolean> canRunStageWithName(ReleaseStage releaseStage, Provider<String> stageProvider) {
        def strategy = versionScheme.map{ it.strategyFor(releaseStage) }

        return strategy.flatMap { versionStrategy ->
            stageProvider.map{ stage -> versionStrategy.stages.contains(stage) }
        }
        .orElse(versionScheme.map({
            it.defaultStrategy.releaseStage == releaseStage
        }))
    }

    private Provider<ReleaseVersion> inferVersionIfGitIsPresent() {
        def project = this.project
        return git.map({ Grgit git ->
            def version = inferVersion(versionScheme.get(), stage, scope)
                    .orElse(providers.provider {
                        throw new GradleException('No version strategies were selected. Run build with --info for more detail.')
                    }).get()
            project.logger.warn('Inferred project: {}, version: {}', project.name, version.version)
            return version
        }.memoize())
                .orElse(new ReleaseVersion(version: VersionPluginConventions.UNINITIALIZED_VERSION)) as Provider<ReleaseVersion>
    }
}
