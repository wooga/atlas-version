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

import org.ajoberstar.gradle.git.release.base.DefaultVersionStrategy
import org.ajoberstar.gradle.git.release.base.ReleaseVersion
import org.ajoberstar.gradle.git.release.base.TagStrategy
import org.ajoberstar.gradle.git.release.base.VersionStrategy
import org.ajoberstar.gradle.git.release.semver.ChangeScope
import org.ajoberstar.grgit.Grgit
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import wooga.gradle.version.VersionConsts
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.strategies.SemverV1Strategies
import wooga.gradle.version.strategies.SemverV2Strategies

class DefaultVersionPluginExtension implements VersionPluginExtension {

    private final Project project;

    protected final Property<VersionStrategy> snapshotStrategy
    protected final Property<VersionStrategy> developmentStrategy
    protected final Property<VersionStrategy> preReleaseStrategy
    protected final Property<VersionStrategy> finalStrategy
    protected final Property<VersionStrategy> defaultStrategy

    final Provider<List<VersionStrategy>> versionStrategies
    final Property<String> scheme
    final Provider<ChangeScope> scope
    final Provider<String> stage
    final Property<Grgit> git
    final TagStrategy tagStrategy = new TagStrategy()
    final Provider<ReleaseVersion> version

    @Override
    void scheme(String value) {
        setScheme(value)
    }

    @Override
    void setScheme(String value) {
        scheme.set(value)
    }

    @Override
    void setScheme(Provider<String> value) {
        scheme.set(value)
    }

    DefaultVersionPluginExtension(Project project) {
        this.project = project
        scheme = project.objects.property(String)
        git = project.objects.property(Grgit)

        snapshotStrategy = project.objects.property(VersionStrategy)
        developmentStrategy = project.objects.property(VersionStrategy)
        preReleaseStrategy = project.objects.property(VersionStrategy)
        finalStrategy = project.objects.property(VersionStrategy)
        defaultStrategy = project.objects.property(VersionStrategy)

        snapshotStrategy.set(scheme.map({ scheme ->
            scheme == VersionConsts.VERSION_SCHEME_SEMVER_2 ? SemverV2Strategies.SNAPSHOT : SemverV1Strategies.SNAPSHOT
        }))

        developmentStrategy.set(scheme.map({ scheme ->
            scheme == VersionConsts.VERSION_SCHEME_SEMVER_2 ? SemverV2Strategies.DEVELOPMENT : SemverV1Strategies.DEVELOPMENT
        }))

        preReleaseStrategy.set(scheme.map({ scheme ->
            scheme == VersionConsts.VERSION_SCHEME_SEMVER_2 ? SemverV2Strategies.PRE_RELEASE : SemverV1Strategies.PRE_RELEASE
        }))

        finalStrategy.set(scheme.map({ scheme ->
            scheme == VersionConsts.VERSION_SCHEME_SEMVER_2 ? SemverV2Strategies.FINAL : SemverV1Strategies.FINAL
        }))

        defaultStrategy.set(developmentStrategy)

        versionStrategies = project.provider({
            [snapshotStrategy.getOrNull(), developmentStrategy.getOrNull(),
             preReleaseStrategy.getOrNull(), finalStrategy.getOrNull()].findAll { it != null }
        })

        scope = project.provider({
            String scope = System.getenv()[VersionConsts.VERSION_SCOPE_ENV_VAR] ?:
                    project.properties.get(VersionConsts.VERSION_SCOPE_OPTION)
            if(!scope) {
                scope = project.properties.get(VersionConsts.LEGACY_VERSION_SCOPE_OPTION)
            }

            if(scope) {
                return ChangeScope.valueOf(scope.toUpperCase())
            }
            null
        })

        stage = project.provider({
            System.getenv()[VersionConsts.VERSION_STAGE_ENV_VAR] ?:
                    project.properties.get(VersionConsts.VERSION_STAGE_OPTION) ?:
                            project.properties.get(VersionConsts.LEGACY_VERSION_STAGE_OPTION)
        })

        version = project.provider({
            def versionStrategies = versionStrategies.get()
            def defaultStrategy = defaultStrategy.get()
            def git = git.getOrNull()
            if(git) {
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
                new ReleaseVersion(version: VersionConsts.UNINITIALIZED_VERSION)
            }
        })
    }
}
