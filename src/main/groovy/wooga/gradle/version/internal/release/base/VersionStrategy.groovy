/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wooga.gradle.version.internal.release.base

import org.ajoberstar.grgit.Grgit

import org.gradle.api.Project
import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.internal.release.semver.VersionInferenceParameters

/**
 * Strategy to infer a version from the project's and Git repository's state.
 * @see wooga.gradle.version.internal.release.semver.SemVerStrategy
 * @see wooga.gradle.version.internal.release.opinion.Strategies
 */
interface VersionStrategy {
    /**
     * The name of the strategy.
     * @return the name of the strategy
     */
    String getName()

    /**
     * Determines if the strategy should be used to infer the project's version.
     * A return of {@code false} does not mean that the strategy cannot be used
     * as the default.
     * @param project the project the version should be inferred for
     * @param grgit the repository the version should be inferred from
     * @return {@code true} if the strategy should be used to infer the version
     */
    boolean selector(Project project, Grgit grgit)

    /**
     * Determines if the strategy should be used to infer the project's version.
     * A return of {@code false} does not mean that the strategy cannot be used
     * as the default.
     * @param project the project the version should be inferred for
     * @param grgit the repository the version should be inferred from
     * @return {@code true} if the strategy should be used to infer the version
     */
    boolean selector(String stage, Grgit grgit)

    /**
     * Infers the project version from the repository.
     * @param extension VersionPluginExtension configured with project data for the inference
     * @return the inferred version
     */
    ReleaseVersion infer(VersionPluginExtension extension)

    /**
     * Infers the project version from given inference parameters.
     * @param strategyState containing data for new version inference
     * @return the inferred version
     */
    ReleaseVersion infer(VersionInferenceParameters inferenceParams)

    /**
     * The stages supported by this strategy.
     */
    SortedSet<String> getStages()

    /**
     * @return The release stage by this strategy
     */
    ReleaseStage getReleaseStage()
}
