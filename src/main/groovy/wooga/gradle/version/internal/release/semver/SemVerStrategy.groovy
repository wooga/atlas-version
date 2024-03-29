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
package wooga.gradle.version.internal.release.semver

import groovy.transform.Immutable
import groovy.transform.PackageScope
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Status
import org.gradle.api.GradleException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.base.DefaultVersionStrategy
import wooga.gradle.version.internal.release.base.ReleaseVersion

import javax.annotation.Nullable
/**
 * Strategy to infer versions that comply with Semantic Versioning.
 * @see PartialSemVerStrategy* @see SemVerStrategyState* @see <ahref="https://github.com/ajoberstar/gradle-git/wiki/SemVer%20Support" > Wiki Doc</a>
 */
@Immutable(copyWith = true, knownImmutableClasses = [PartialSemVerStrategy])
final class SemVerStrategy implements DefaultVersionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SemVerStrategy)

    /**
     * The name of the strategy.
     */
    String name

    /**
     * The stages supported by this strategy.
     */
    SortedSet<String> stages

    /**
     * Whether or not this strategy can be used if the repo has uncommited changes.
     */
    boolean allowDirtyRepo

    ReleaseStage releaseStage

    /**
     * The strategy used to infer the normal component of the version. There is no enforcement that
     * this strategy only modify that part of the state.
     */
    PartialSemVerStrategy normalStrategy

    /**
     * The strategy used to infer the pre-release component of the version. There is no enforcement that
     * this strategy only modify that part of the state.
     */
    PartialSemVerStrategy preReleaseStrategy

    /**
     * The strategy used to infer the build metadata component of the version. There is no enforcement that
     * this strategy only modify that part of the state.
     */
    PartialSemVerStrategy buildMetadataStrategy

    /**
     * Whether or not to create tags for versions inferred by this strategy.
     */
    boolean createTag

    /**
     * Whether or not to enforce that versions inferred by this strategy are of higher precedence
     * than the nearest any.
     */
    boolean enforcePrecedence

    /**
     * Determines whether this strategy can be used to infer the version as a default.
     * <ul>
     * <li>Return {@code false}, if the {@code release.stage} is not one listed in the {@code stages} property.</li>
     * <li>Return {@code false}, if the repository has uncommitted changes and {@code allowDirtyRepo} is {@code false}.</li>
     * <li>Return {@code true}, otherwise.</li>
     * </ul>
     */
    @Override
    boolean defaultSelector(@Nullable String stage, Grgit grgit) {
        if (stage != null && !stages.contains(stage)) {
            logger.info('Skipping {} default strategy because stage ({}) is not one of: {}', name, stage, stages)
            return false
        }

        def status = grgit.status()
        if (!allowDirtyRepo && !status.clean) {
            logger.info('Skipping {} default strategy because repo is dirty.', name)
            logger.info(composeRepositoryStatus(status))
            return false
        } else {
            String statusString = status.clean ? 'clean' : 'dirty'
            logger.info('Using {} default strategy because repo is {} and no stage defined', name, statusString)
            return true
        }
    }
    /**
     * Determines whether this strategy can be used to infer the version.
     * <ul>
     * <li>Return {@code false}, if the {@code release.stage} is not one listed in the {@code stages} property.</li>
     * <li>Return {@code false}, if the repository has uncommitted changes and {@code allowDirtyRepo} is {@code false}.</li>
     * <li>Return {@code true}, otherwise.</li>
     * </ul>
     */
    @Override
    boolean selector(@Nullable String stage, Grgit grgit) {
        if (stage == null || !stages.contains(stage)) {
            logger.info('Skipping {} strategy because stage ({}) is not one of: {}', name, stage, stages)
            return false
        }
        def status = grgit.status()
         if (!allowDirtyRepo && !status.clean) {
            logger.info('Skipping {} strategy because repo is dirty...', name)
            logger.info(composeRepositoryStatus(status))
            return false
        } else {
            String statusString = status.clean ? 'clean' : 'dirty'
            logger.info('Using {} strategy because repo is {} and stage ({}) is one of: {}', name, statusString, stage, stages)
            return true
        }
    }

    /**
     * Composes a string detailing the current repository staged/unstaged files
     */
    static String composeRepositoryStatus(Status status) {
        StringBuilder str = new StringBuilder()

        Closure printChangeSet = { label, changeSet ->
            str.append("\n> ${label}\n")
            str.append(changeSet.added.collect({ "[ADDED] ${it}" }).join("\n"))
            str.append(changeSet.modified.collect({ "[MODIFIED] ${it}" }).join("\n"))
            str.append(changeSet.removed.collect({ "[REMOVED] ${it}" }).join("\n"))
        }

        str.append("Repository Status:")
        printChangeSet("Staged", status.staged)
        printChangeSet("Unstaged", status.unstaged)

        str.toString()
    }

    /**
     * Infers the version to use for this build. Uses the normal, pre-release, and build metadata
     * strategies in order to infer the version. If the {@code release.stage} is not set, uses the
     * first value in the {@code stages} set (i.e. the one with the lowest precedence). After inferring
     * the version precedence will be enforced, if required by this strategy.
     *
     */
    @Override
    ReleaseVersion infer(VersionInferenceParameters params) {
        logger.debug('Located nearest version: {}', params.nearestVersion)
        return doInfer(generateState(params))
    }

    @PackageScope
    ReleaseVersion doInfer(SemVerStrategyState semVerState) {
        logger.info('Beginning version inference using {} strategy and input scope ({}) and stage ({})', name, semVerState.scopeFromProp, semVerState.stageFromProp)
        def finalSemverState = StrategyUtil.all(
            normalStrategy, preReleaseStrategy, buildMetadataStrategy).infer(semVerState)
        def version = finalSemverState.toVersion()

        def nearestVersion = finalSemverState.nearestVersion
        if (enforcePrecedence && version < nearestVersion.any) {
            throw new GradleException("Inferred version (${version}) cannot be lower than nearest (${nearestVersion.any}). Required by selected strategy '${name}'.")
        }
        def nearestNormal = nearestVersion.normal == NearestVersion.EMPTY ? null : nearestVersion.normal
        return new ReleaseVersion(version.toString(), nearestNormal?.toString(), createTag)
    }

    /**
     * Validates inference parameters and generate a valid SemVerStrategyState to be used in the version generation process.
     * @param params - VersionInferenceParameters object containing the parameters for version inference.
     * @return valid SemverStrategyState
     * @throws org.gradle.api.GradleException if stage is not one of the allowed states for this strategy.
     */
    private SemVerStrategyState generateState(VersionInferenceParameters params) {
        if (params.stage && !stages.contains(params.stage)) {
            throw new GradleException("Stage ${params.stage} is not one of ${stages} allowed for strategy ${name}.")
        }
        return new SemVerStrategyState(
            scopeFromProp: params.scope,
            stageFromProp: params.stage ?: stages.first(),
            currentHead: params.currentHead,
            currentBranch: params.currentBranch,
            repoDirty: params.repoDirty,
            nearestVersion: params.nearestVersion,
            nearestCiMarker: params.nearestCiMarker,
            nearestReleaseMarker: params.nearestReleaseMarker,
            releaseBranchPattern: params.releaseBranchPattern,
            mainBranchPattern: params.mainBranchPattern
        )
    }
}
