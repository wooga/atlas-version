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
package wooga.gradle.version.internal.release.opinion

import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.SemVerStrategyState
import wooga.gradle.version.internal.release.semver.StrategyUtil

import java.sql.Timestamp
import java.text.SimpleDateFormat

import static wooga.gradle.version.internal.release.semver.StrategyUtil.*

import java.util.regex.Pattern

import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy

import org.gradle.api.GradleException

/**
 * Opinionated sample strategies. These can either be used as-is or as an
 * example for others.
 * @see wooga.gradle.version.internal.release.base.VersionStrategy
 * @see SemVerStrategy
 * @see wooga.gradle.version.internal.release.semver.SemVerStrategyState
 * @see PartialSemVerStrategy
 */
final class Strategies {
    /**
     * Sample strategies that infer the normal component of a version.
     */
    static final class Normal {
        /**
         * Do not modify the pre-release component.
         */
        static final PartialSemVerStrategy NONE = closure { state -> state }
        /**
         * Increments the nearest normal version using the scope specified
         * in the {@link SemVerStrategyState#scope}.
         */
        static final PartialSemVerStrategy USE_SCOPE_STATE = closure { state ->
            return incrementNormalFromScope(state, state.scope)
        }

        /**
         * If the nearest any is different from the nearest normal, sets the
         * normal component to the nearest any's normal component. Otherwise
         * do nothing.
         *
         * <p>
         * For example, if the nearest any is {@code 1.2.3-alpha.1} and the
         * nearest normal is {@code 1.2.2}, this will infer the normal
         * component as {@code 1.2.3}.
         * </p>
         */
        static final PartialSemVerStrategy USE_NEAREST_ANY = closure { state ->
            def nearest = state.nearestVersion
            if (nearest.any == nearest.normal) {
                return state
            } else {
                return state.copyWith(inferredNormal: nearest.any.normalVersion)
            }
        }

        /**
         * If the nearest any is higher from the nearest normal, sets the
         * normal component to the nearest any's normal component. Otherwise
         * do nothing.
         *
         * <p>
         * For example, if the nearest any is {@code 1.2.3-alpha.1} and the
         * nearest normal is {@code 1.2.2}, this will infer the normal
         * component as {@code 1.2.3}.
         * </p>
         */
        static final PartialSemVerStrategy NEAREST_HIGHER_ANY = closure { state ->
            def nearest = state.nearestVersion
            if (nearest.any.lessThanOrEqualTo(nearest.normal)) {
                return state
            } else {
                return state.copyWith(inferredNormal: nearest.any.normalVersion)
            }
        }

        /**
         * Enforces that the normal version complies with the current branch's major version.
         * If the branch is not in the format {@code #.x} (e.g. {@code 2.x}), this will do
         * nothing.
         *
         * <ul>
         *   <li>If the current branch doesn't match the pattern do nothing.</li>
         *   <li>If the the nearest normal already complies with the branch name.</li>
         *   <li>If the major component can be incremented to comply with the branch, do so.</li>
         *   <li>Otherwise fail, because the version can't comply with the branch.</li>
         * </ul>
         */
        static final PartialSemVerStrategy ENFORCE_BRANCH_MAJOR_X = fromBranchPattern(~/^(\d+)\.x$/)

        /**
         * Enforces that the normal version complies with the current branch's major version.
         * If the branch is not in the format {@code release/#.x} (e.g. {@code release/2.x}) or
         * {@code release-#.x} (e.g. {@code release-3.x}, this will do nothing.
         *
         * <ul>
         *   <li>If the current branch doesn't match the pattern do nothing.</li>
         *   <li>If the the nearest normal already complies with the branch name.</li>
         *   <li>If the major component can be incremented to comply with the branch, do so.</li>
         *   <li>Otherwise fail, because the version can't comply with the branch.</li>
         * </ul>
         */
        static final PartialSemVerStrategy ENFORCE_GITFLOW_BRANCH_MAJOR_X = fromBranchPattern(~/^release(?:\/|-)(\d+)\.x$/)

        /**
         * Enforces that the normal version complies with the current branch's major version.
         * If the branch is not in the format {@code #.#.x} (e.g. {@code 2.3.x}), this will do
         * nothing.
         *
         * <ul>
         *   <li>If the current branch doesn't match the pattern do nothing.</li>
         *   <li>If the the nearest normal already complies with the branch name.</li>
         *   <li>If the major component can be incremented to comply with the branch, do so.</li>
         *   <li>If the minor component can be incremented to comply with the branch, do so.</li>
         *   <li>Otherwise fail, because the version can't comply with the branch.</li>
         * </ul>
         */
        static final PartialSemVerStrategy ENFORCE_BRANCH_MAJOR_MINOR_X = fromBranchPattern(~/^(\d+)\.(\d+)\.x$/)

        /**
         * Enforces that the normal version complies with the current branch's major version.
         * If the branch is not in the format {@code release/#.#.x} (e.g. {@code release/2.3.x}) or
         * {@code release-#.#.x} (e.g. {@code release-3.11.x}, this will do nothing.
         *
         * <ul>
         *   <li>If the current branch doesn't match the pattern do nothing.</li>
         *   <li>If the the nearest normal already complies with the branch name.</li>
         *   <li>If the major component can be incremented to comply with the branch, do so.</li>
         *   <li>Otherwise fail, because the version can't comply with the branch.</li>
         * </ul>
         */
        static final PartialSemVerStrategy ENFORCE_GITFLOW_BRANCH_MAJOR_MINOR_X = fromBranchPattern(~/^release(?:\/|-)(\d+)\.(\d+)\.x$/)

        /**
         * Uses the specified pattern to enforce that versions inferred on this branch
         * comply. Patterns should have 1 or 2 capturing groups representing the
         * major and, optionally, the minor component of the version.
         *
         * <ul>
         *   <li>If the current branch doesn't match the pattern do nothing.</li>
         *   <li>If only the major is specified in the branch name, and the nearest normal complies with that major, do nothing.</li>
         *   <li>If the patch component can be incremented and still comply with the branch, do so.</li>
         *   <li>If the minor component can be incremented to comply with the branch, do so.</li>
         *   <li>If the major component can be incremented to comply with the branch, do so.</li>
         *   <li>Otherwise fail, because the version can't comply with the branch.</li>
         * </ul>
         */
        static PartialSemVerStrategy fromBranchPattern(Pattern pattern) {
            return closure { SemVerStrategyState state ->
                return fromMatchingBranchName(state.currentBranch.name, pattern).infer(state)
            }
        }

        /**
         * Uses the specified pattern to enforce that versions inferred on this branch
         * comply. Patterns should have 1 or 2 capturing groups representing the
         * major and, optionally, the minor component of the version.
         *
         * <ul>
         *   <li>If the current branch doesn't match the pattern do nothing.</li>
         *   <li>If only the major is specified in the branch name, and the nearest normal complies with that major, do nothing.</li>
         *   <li>If the patch component can be incremented and still comply with the branch, do so.</li>
         *   <li>If the minor component can be incremented to comply with the branch, do so.</li>
         *   <li>If the major component can be incremented to comply with the branch, do so.</li>
         *   <li>Otherwise fail, because the version can't comply with the branch.</li>
         * </ul>
         */
        static PartialSemVerStrategy fromMatchingBranchName(String branchName, Pattern pattern) {
            return closure { SemVerStrategyState state ->
                def m = branchName =~ pattern
                if (m) {
                    def major = m.groupCount() >= 1 ? parseIntOrZero(m[0][1]) : -1
                    def minor = m.groupCount() >= 2 ? parseIntOrZero(m[0][2]) : -1
                    def normal = state.nearestVersion.normal
                    def majorDiff = major - normal.majorVersion
                    def minorDiff = minor - normal.minorVersion

                    if (majorDiff == 1 && minor <= 0) {
                        // major is off by one and minor is either 0 or not in the branch name
                        return incrementNormalFromScope(state, ChangeScope.MAJOR)
                    } else if (minorDiff == 1 && minor > 0) {
                        // minor is off by one and specified in the branch name
                        return incrementNormalFromScope(state, ChangeScope.MINOR)
                    } else if (majorDiff == 0 && minorDiff == 0 && minor >= 0) {
                        // major and minor match, both are specified in branch name
                        return incrementNormalFromScope(state, ChangeScope.PATCH)
                    } else if (majorDiff == 0 && minor < 0) {
                        // only major specified in branch name and already matches
                        return state
                    } else {
                        throw new GradleException("Invalid branch (${branchName}) for nearest normal (${state.nearestVersion.normal}).", e)
                    }
                } else {
                    return state
                }
            }

        }
        /**
         * Uses given scope if state's branch name matches pattern
         * @param pattern - Pattern to match with state's branch name
         * @param scope - Scope to be used in strategy if pattern matches
         * @return PartialSemVerStrategy that executes the operation described above
         */
        static PartialSemVerStrategy scopeIfBranchMatchesPattern(Pattern pattern, ChangeScope scope) {
            return closure { SemVerStrategyState state ->
                def m = state.currentBranch.name =~ pattern
                if (m.matches()) {
                    return useScope(scope).infer(state)
                }
                return state
            }
        }
        /**
         * Always use the scope provided to increment the normal component.
         */
        static PartialSemVerStrategy useScope(ChangeScope scope) {
            return closure { state -> incrementNormalFromScope(state, scope) }
        }

        static final PartialSemVerStrategy USE_MARKER_TAG = closure { SemVerStrategyState state ->
            def markerVersion = state.nearestCiMarker
            if (state.currentBranch.name.matches(state.releaseBranchPattern)) {
                markerVersion = state.nearestReleaseMarker
            }
            state = state.copyWith(inferredNormal: markerVersion.normal)
            return state
        }
    }

    /**
     * Sample strategies that infer the pre-release component of a version.
     */
    static final class PreRelease {
        /**
         * Do not modify the pre-release component.
         */
        static final PartialSemVerStrategy NONE = closure { state -> state }

        /**
         * Sets the pre-release component to the value of {@link SemVerStrategyState#stage}.
         */
        static final PartialSemVerStrategy STAGE_FIXED = closure { state -> state.copyWith(inferredPreRelease: state.stage) }

        /**
         * Appends a timestamp in the format yyyyMMddHHmm to the pre-release component
         * @param separator - join component between the pre-release component and the timestamp.
         * @return partial strategy function appendint timestamp to the pre-release component.
         */
        static PartialSemVerStrategy withTimestamp(String separator = ".") {
            closure { SemVerStrategyState state ->
                state.copyWith(inferredPreRelease: "${state.inferredPreRelease}${separator}${GenerateTimestamp()}")
            }
        }

        /**
         * The format of the generated timestamp
         */
        static final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmm")

        /**
         * @return Generates a timestamp to be used for versioning
         */
        static String GenerateTimestamp() {
            def timestamp = new Timestamp(System.currentTimeMillis())
            def result = timestampFormat.format(timestamp)
            result
        }

        /**
         * If the value of {@link SemVerStrategyState#stage} has a higher or the same precedence than
         * the nearest any's pre-release component, set the pre-release component to
         * {@link SemVerStrategyState#scope}. If not, append the {@link SemVerStrategyState#scope}
         * to the nearest any's pre-release.
         */
        static final PartialSemVerStrategy STAGE_FLOAT = closure { state ->
            def sameNormal = state.inferredNormal == state.nearestVersion.any.normalVersion
            def nearestAnyPreRelease = state.nearestVersion.any.preReleaseVersion
            if (sameNormal && nearestAnyPreRelease != null && nearestAnyPreRelease > state.stage) {
                state.copyWith(inferredPreRelease: "${nearestAnyPreRelease}.${state.stage}")
            } else {
                state.copyWith(inferredPreRelease: state.stage)
            }
        }

        /**
         * Appends a branch name to the pre-release component.
         * @param separator - string used to join prerelease and branch strings defaults to Dot(.)
         * @param branchTransform - potential transformations to branch name, defaults to identity
         * @return
         */
        static PartialSemVerStrategy withBranchName(String separator = ".",
                                                    Closure<String> branchTransform = {it -> it}) {
            closure { SemVerStrategyState state ->
                String branchName = branchFromEnvIfDetached(state)
                branchName = branchTransform(~state.mainBranchPattern, branchName)

                def inferred = state.inferredPreRelease ?
                        "${state.inferredPreRelease}${separator}${branchName}" :
                        "${branchName}"
                state.copyWith(inferredPreRelease: inferred)
            }
        }

        static final PartialSemVerStrategy WITH_BRANCH_NAME = withBranchName(".") {
            Pattern mainBranchPattern, String branchName ->
                branchName = branchName.matches(mainBranchPattern)? branchName : "branch.${branchName.toLowerCase()}"
                //Split at branch delimiter /-_+ and replace with .
                branchName = branchName.replaceAll(/((\/|-|_|\.)+)([\w])/) { _, __, ___, firstAfter -> ".${firstAfter}" }
                //Remove all hanging /-_+
                branchName = branchName.replaceAll(/[-\/_\+]+$/, "")
                //parse all digits and replace with unpadded value e.g. 001 -> 1
                branchName = branchName.replaceAll(/([\w\.])([0-9]+)/) { all, s, delimiter ->
                    "${s == "."? "" : s}.${Integer.parseInt(delimiter).toString()}"
                }
                return branchName
        }

        /**
         * Appends the pre-release component to a string compatible with semver v1
         * pre-release processing on all of Paket, Nuget and JFrog/Artifactory.
         *
         * Numbers are only allowed at the end of the string, so numbers not belonging to the count element are expressed in verbose textual form.
         * Also, only [A-Za-z0-9] is permitted in the the pre-release component as well, so any '.' is replaced by the 'Dot' string.
         */
        static final PartialSemVerStrategy PAKET_BRANCH_NAME = withBranchName("") {
            Pattern mainBranchPattern, String branchName ->
                branchName = branchName == "master"? branchName : "branch${branchName.capitalize()}"

                branchName = branchName.replaceAll(/(\/|-|_)([\w])/) { _, __, firstAfter -> "${firstAfter.capitalize()}" }
                        .replaceAll(/\./, "Dot")
                return StrategyUtil.numbersAsLiteralNumbers(branchName)
        }

        /**
         * If the nearest any's pre-release component starts with the so far inferred pre-release component,
         * increment the count of the nearest any and append it to the so far inferred pre-release
         * component. Otherwise append 1 to the so far inferred pre-release component.
         */
        static final PartialSemVerStrategy COUNT_INCREMENTED = countIncremented()

        /**
         * If the nearest any's pre-release component starts with the so far inferred pre-release component,
         * increment the count of the nearest any and append it to the so far inferred pre-release
         * component. Otherwise append 1 to the so far inferred pre-release component.
         * @param separator - string to join the string and count pre-release together. Defaults to dot (.)
         * @param countPadding - padding for the count pre-release. For instance ['rc', '1'] with a padding of 3 turns into ['rc', '001']. Defaults to zero.
         * @return PartialSemVerStrategy that executes the described operation.
         */
        static final PartialSemVerStrategy countIncremented(String separator = ".", int countPadding = 0) {
            closure { SemVerStrategyState state ->
                state.copyWith(inferredPreRelease: incrementsPreRelease(state, separator, countPadding))
            }
        }

        /**
         * Append the count of commits since the nearest any to the so far inferred pre-release component.
         */
        static final PartialSemVerStrategy COUNT_COMMITS_SINCE_ANY = closure { state ->
            def count = state.nearestVersion.distanceFromAny
            def inferred = state.inferredPreRelease ? "${state.inferredPreRelease}.${count}" : "${count}"
            return state.copyWith(inferredPreRelease: inferred)
        }

        /**
         * If the repo has uncommitted changes append "uncommitted" to the so far inferred pre-release component.
         */
        static final PartialSemVerStrategy SHOW_UNCOMMITTED = closure { state ->
            if (state.repoDirty) {
                def inferred = state.inferredPreRelease ? "${state.inferredPreRelease}.uncommitted" : 'uncommitted'
                state.copyWith(inferredPreRelease: inferred)
            } else {
                state
            }
        }

        /**
         * Appends the commit count since the nearest CI marker to the pre-release compnent using a dot(.).
         */
        static final PartialSemVerStrategy COUNT_COMMITS_SINCE_MARKER = closure { SemVerStrategyState state ->
            def markerVersion = state.nearestCiMarker
            if (state.currentBranch.name.matches(state.releaseBranchPattern)) {
                markerVersion = state.nearestReleaseMarker
            }

            def count = markerVersion.distanceFromAny
            def inferred = state.inferredPreRelease ? "${state.inferredPreRelease}.${count}" : "${count}"
            return state.copyWith(inferredPreRelease: inferred)
        }

        /**
         * Appends the commit count since the nearest normal version
         * @param separator - join component between the pre-release component and the count.
         * @param paddingCount - with hom many zeros to pad the generate count.
         * @return PartialSemverStrategy executing the operation described above.
         */
        static final PartialSemVerStrategy countCommitsSinceNormal(String separator = ".", int paddingCount = 0) {
            return closure { SemVerStrategyState state ->
                def buildSinceAny = state.nearestVersion.distanceFromNormal
                def padded = "$buildSinceAny".padLeft(paddingCount, '0')
                return state.copyWith(inferredPreRelease: [state.inferredPreRelease, padded].join(separator))
            }
        }
    }

    /**
     * Sample strategies that infer the build metadata component of a version.
     */
    static final class BuildMetadata {
        /**
         * Do not modify the build metadata.
         */
        static final PartialSemVerStrategy NONE = closure { state -> state }
        /**
         * Set the build metadata to the abbreviated ID of the current HEAD.
         */
        static final PartialSemVerStrategy COMMIT_ABBREVIATED_ID = closure { state -> state.copyWith(inferredBuildMetadata: state.currentHead.abbreviatedId) }
        /**
         * Set the build metadata to the full ID of the current HEAD.
         */
        static final PartialSemVerStrategy COMMIT_FULL_ID = closure { state -> state.copyWith(inferredBuildMetadata: state.currentHead.id) }
        /**
         * Set the build metadata to the current timestamp in {@code YYYY.MM.DD.HH.MM.SS} format.
         */
        static final PartialSemVerStrategy TIMESTAMP = closure { state -> state.copyWith(inferredBuildMetadata: new Date().format('yyyy.MM.dd.hh.mm.ss')) }
    }

    /**
     * Provides opinionated defaults for a strategy. The primary behavior is for the normal component.
     * If the {@code release.scope} property is set, use it. Or if the nearest any's normal component is different
     * than the nearest normal version, use it. Or, if nothing else, use PATCH scope.
     */
    static final SemVerStrategy DEFAULT = new SemVerStrategy(
        name: '',
        stages: [] as SortedSet,
        allowDirtyRepo: false,
        normalStrategy: one(Normal.USE_SCOPE_STATE, Normal.USE_NEAREST_ANY, Normal.useScope(ChangeScope.PATCH)),
        preReleaseStrategy: PreRelease.NONE,
        buildMetadataStrategy: BuildMetadata.NONE,
        createTag: true,
        enforcePrecedence: true,
        releaseStage: ReleaseStage.Unknown
    )

    /**
     * Provides a single "SNAPSHOT" stage that can be used in dirty repos and will
     * not enforce precedence. The pre-release compoment will always be "SNAPSHOT"
     * and no build metadata will be used. Tags will not be created for these versions.
     */
    static final SemVerStrategy SNAPSHOT = DEFAULT.copyWith(
        name: 'snapshot',
        stages: ['SNAPSHOT'] as SortedSet,
        allowDirtyRepo: true,
        preReleaseStrategy: PreRelease.STAGE_FIXED,
        createTag: false,
        enforcePrecedence: false,
        releaseStage: ReleaseStage.Snapshot
    )

    /**
     * Provides a single "dev" stage that can be used in dirty repos but will
     * enforce precedence. If this strategy is used after a nearest any with a
     * higher precedence pre-release component (e.g. "rc.1"), the dev component
     * will be appended rather than replace. The commit count since the nearest
     * any will be used to disambiguate versions and the pre-release component
     * will note if the repository is dirty. The abbreviated ID of the HEAD will
     * be used as build metadata.
     */
    static final SemVerStrategy DEVELOPMENT = DEFAULT.copyWith(
        name: 'development',
        stages: ['dev'] as SortedSet,
        allowDirtyRepo: true,
        preReleaseStrategy: all(PreRelease.STAGE_FLOAT, PreRelease.COUNT_COMMITS_SINCE_ANY, PreRelease.SHOW_UNCOMMITTED),
        buildMetadataStrategy: BuildMetadata.COMMIT_ABBREVIATED_ID,
        createTag: false,
        releaseStage: ReleaseStage.Development
    )

    /**
     * Provides "milestone" and "rc" stages that can only be used in clean repos
     * and will enforce precedence. The pre-release component will always be set
     * to the stage with an incremented count to disambiguate successive
     * releases of the same stage. No build metadata component will be added. Please
     * note that this strategy uses the same name as {@code PRE_RELEASE_ALPHA_BETA}
     * so it cannot be used at the same time.
     */
    static final SemVerStrategy PRE_RELEASE = DEFAULT.copyWith(
        name: 'pre-release',
        stages: ['milestone', 'rc'] as SortedSet,
        preReleaseStrategy: all(PreRelease.STAGE_FIXED, PreRelease.COUNT_INCREMENTED),
        releaseStage: ReleaseStage.Prerelease
    )

    /**
     * Provides "alpha", "beta" and "rc" stages that can only be used in clean repos
     * and will enforce precedence. The pre-release-alpha-beta component will always be set
     * to the stage with an incremented count to disambiguate successive
     * releases of the same stage. No build metadata component will be added. Please note
     * that this strategy uses the same name as {@code PRE_RELEASE} so it cannot be used
     * at the same time.
     */
    static final SemVerStrategy PRE_RELEASE_ALPHA_BETA = PRE_RELEASE.copyWith(
        name: 'pre-release',
        stages: ['alpha', 'beta', 'rc'] as SortedSet,
        releaseStage: ReleaseStage.Prerelease
    )

    /**
     * Provides a single "final" stage that can only be used in clean repos and
     * will enforce precedence. The pre-release and build metadata components
     * will always be empty.
     */
    static final SemVerStrategy FINAL = DEFAULT.copyWith(
        name: 'final',
        stages: ['final'] as SortedSet,
        releaseStage: ReleaseStage.Final
    )
}
