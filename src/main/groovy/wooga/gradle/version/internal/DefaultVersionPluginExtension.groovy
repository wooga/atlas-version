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

import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.VersionCodeScheme
import wooga.gradle.version.VersionScheme
import wooga.gradle.version.internal.release.base.DefaultVersionStrategy
import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.version.internal.release.base.VersionStrategy
import wooga.gradle.version.internal.release.semver.ChangeScope
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import wooga.gradle.version.VersionPluginConventions
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.strategies.SemverV1Strategies
import wooga.gradle.version.strategies.SemverV2Strategies
import wooga.gradle.version.strategies.StaticMarkerStrategies

class DefaultVersionPluginExtension implements VersionPluginExtension {

    private final Project project;

    protected final Property<VersionStrategy> snapshotStrategy
    protected final Property<VersionStrategy> developmentStrategy
    protected final Property<VersionStrategy> preReleaseStrategy
    protected final Property<VersionStrategy> finalStrategy
    protected final Property<VersionStrategy> defaultStrategy


    final Provider<List<VersionStrategy>> versionStrategies

    DefaultVersionPluginExtension(Project project) {
        this.project = project

        snapshotStrategy = project.objects.property(VersionStrategy)
        developmentStrategy = project.objects.property(VersionStrategy)
        preReleaseStrategy = project.objects.property(VersionStrategy)
        finalStrategy = project.objects.property(VersionStrategy)
        defaultStrategy = project.objects.property(VersionStrategy)
        releaseBranchPattern = project.objects.property(String)
        mainBranchPattern = project.objects.property(String)

        snapshotStrategy.set(versionScheme.map({ scheme ->
            switch (scheme) {
                case VersionScheme.semver2:
                    SemverV2Strategies.SNAPSHOT
                    break;
                case VersionScheme.staticMarker:
                    StaticMarkerStrategies.SNAPSHOT
                    break
                default:
                    SemverV1Strategies.SNAPSHOT
            }
        }))

        developmentStrategy.set(versionScheme.map({ scheme ->
            switch (scheme) {
                case VersionScheme.semver2:
                    SemverV2Strategies.DEVELOPMENT
                    break;
                case VersionScheme.staticMarker:
                    StaticMarkerStrategies.DEVELOPMENT
                    break
                default:
                    SemverV1Strategies.DEVELOPMENT
            }
        }))

        preReleaseStrategy.set(versionScheme.map({ scheme ->
            switch (scheme) {
                case VersionScheme.semver2:
                    SemverV2Strategies.PRE_RELEASE
                    break;
                case VersionScheme.staticMarker:
                    StaticMarkerStrategies.PRE_RELEASE
                    break
                default:
                    SemverV1Strategies.PRE_RELEASE
            }
        }))

        finalStrategy.set(versionScheme.map({ scheme ->
            switch (scheme) {
                case VersionScheme.semver2:
                    SemverV2Strategies.FINAL
                    break;
                case VersionScheme.staticMarker:
                    StaticMarkerStrategies.FINAL
                    break
                default:
                    SemverV1Strategies.FINAL
            }
        }))

        defaultStrategy.set(developmentStrategy)

        versionStrategies = project.provider({
            [snapshotStrategy.getOrNull(), developmentStrategy.getOrNull(),
             preReleaseStrategy.getOrNull(), finalStrategy.getOrNull()].findAll { it != null }
        })

        scope = VersionPluginConventions.scope.getStringValueProvider(project).map({
            if (it) {
                return ChangeScope.valueOf(it.toUpperCase())
            }
            null
        })

        stage = VersionPluginConventions.stage.getStringValueProvider(project)
        releaseBranchPattern.set(VersionPluginConventions.releaseBranchPattern.getStringValueProvider(project))
        mainBranchPattern.set(VersionPluginConventions.mainBranchPattern.getStringValueProvider(project))

        version = project.provider({
            def versionStrategies = versionStrategies.get()
            def defaultStrategy = defaultStrategy.get()
            def git = git.getOrNull()
            if (git) {
                VersionStrategy selectedStrategy = versionStrategies.find { strategy ->
                    strategy.selector(project, git)
                }

                if (!selectedStrategy) {
                    boolean useDefault
                    if (defaultStrategy instanceof DefaultVersionStrategy) {
                        useDefault = defaultStrategy.defaultSelector(project, git)
                    } else {
                        useDefault = defaultStrategy?.selector(project, git)
                    }

                    if (useDefault) {
                        //logger.info("Falling back to default strategy: ${defaultVersionStrategy.name}")
                        selectedStrategy = defaultStrategy
                    } else {
                        throw new GradleException('No version strategies were selected. Run build with --info for more detail.')
                    }
                }
                return selectedStrategy.infer(project, git)
            } else {
                new ReleaseVersion(version: VersionPluginConventions.UNINITIALIZED_VERSION)
            }
        }.memoize())

        versionCode = versionCodeScheme.map({ scheme ->
            def offset = versionCodeOffset.getOrElse(0)
            switch (scheme) {
                case VersionCodeScheme.semverBasic:
                    def version = this.version.get().version
                    return VersionCode.generateSemverVersionCode(version) + offset
                    break
                case VersionCodeScheme.semver:
                    def version = this.version.get().version
                    return VersionCode.generateSemverVersionCode(version, true) + offset
                    break
                case VersionCodeScheme.releaseCountBasic:
                    return VersionCode.generateBuildNumberVersionCode(git.get(), false, offset)
                    break
                case VersionCodeScheme.releaseCount:
                    return VersionCode.generateBuildNumberVersionCode(git.get(), true, offset)
                default:
                    0
            }
        }.memoize())

        // TODO: Test these providers are resolved correctly
        // Set up providers to evaluate the release stage of a project,
        // which  can be used by client projects
        isDevelopment = developmentStrategy.map({
            it.stages.contains(this.stage.get())
        })
        isSnapshot = snapshotStrategy.map({
            it.stages.contains(this.stage.get())
        })
        isPrerelease = preReleaseStrategy.map({
            it.stages.contains(this.stage.get())
        })
        isFinal = finalStrategy.map({
            it.stages.contains(this.stage.get())
        })
    }
}
