/*
 * Copyright 2017 the original author or authors.
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

package wooga.gradle.version.strategies

import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.opinion.Strategies.BuildMetadata
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategyState

import java.util.regex.Pattern

import static wooga.gradle.version.internal.release.semver.StrategyUtil.*

/**
 * Please use SemverV2WithDefaultStrategies instead
 */
@Deprecated
final class SemverV2Strategies {

    private static final scopes = one(
            Strategies.Normal.USE_SCOPE_STATE,
            Normal.matchBranchPatternAndUseScope(~/feature(?:\/|-).+$/, ChangeScope.MINOR),
            Normal.matchBranchPatternAndUseScope(~/hotfix(?:\/|-).+$/, ChangeScope.PATCH),
            Normal.matchBranchPatternAndUseScope(~/fix(?:\/|-).+$/, ChangeScope.MINOR),
            Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_X,
            Strategies.Normal.ENFORCE_BRANCH_MAJOR_X,
            Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_MINOR_X,
            Strategies.Normal.ENFORCE_BRANCH_MAJOR_MINOR_X,
            Strategies.Normal.USE_NEAREST_ANY,
            Strategies.Normal.useScope(ChangeScope.MINOR)
    )

    static final class Normal {
        static PartialSemVerStrategy matchBranchPatternAndUseScope(Pattern pattern, ChangeScope scope) {
            return closure { SemVerStrategyState state ->
                def m = state.currentBranch.name =~ pattern
                if (m.matches()) {
                    return incrementNormalFromScope(state, scope)
                }

                return state
            }
        }
    }

    static final class PreRelease {
        static PartialSemVerStrategy STAGE_BRANCH_NAME = closure { SemVerStrategyState state ->
            String branchName = state.currentBranch.name
            String prefix = "branch"

            if (branchName == "HEAD" && System.getenv("BRANCH_NAME")) {
                branchName = System.getenv("BRANCH_NAME")
            }

            if (!branchName.matches(state.mainBranchPattern)) {
                branchName = "$prefix.${branchName.toLowerCase()}"
            }
            //Split at branch delimiter /-_+ and replace with .
            branchName = branchName.replaceAll(/((\/|-|_|\.)+)([\w])/) { all, delimiterAll, delimiter , firstAfter -> ".${firstAfter}" }
            //Remove all hanging /-_+
            branchName = branchName.replaceAll(/[-\/_\+]+$/) { "" }
            //parse all digits and replace with unpadded value e.g. 001 -> 1
            branchName = branchName.replaceAll(/([\w\.])([0-9]+)/) { all, s, delimiter ->
                if(s == ".") {
                    s = ""
                }

                "${s}.${Integer.parseInt(delimiter).toString()}"
            }
            def inferred = state.inferredPreRelease ? "${state.inferredPreRelease}.${branchName}" : "${branchName}"
            state.copyWith(inferredPreRelease: inferred)
        }
    }

    static final SemVerStrategy DEFAULT = new SemVerStrategy(
            name: '',
            stages: [] as SortedSet,
            allowDirtyRepo: true,
            normalStrategy: scopes,
            preReleaseStrategy: Strategies.PreRelease.NONE,
            buildMetadataStrategy: BuildMetadata.NONE,
            createTag: true,
            enforcePrecedence: true,
            releaseStage: ReleaseStage.Unknown
    )

    /**
     * Returns a version strategy to be used for {@code final} builds.
     * <p>
     * This strategy infers the release version by checking the nearest release.
     * <p>
     * Example:
     * <pre>
     * {@code
     * releaseScope = "minor"
     * nearestVersion = "1.3.0"
     * inferred = "1.4.0"
     * }
     * </pre>
     */
    static final SemVerStrategy FINAL = DEFAULT.copyWith(
            name: 'production',
            stages: ['final','production'] as SortedSet,
            releaseStage: ReleaseStage.Final
    )

    /**
     * Returns a version strategy to be used for {@code pre-release}/{@code candidate} builds.
     * <p>
     * This strategy infers the release version by checking the nearest any release.
     * If a {@code pre-release} with the same {@code major}.{@code minor}.{@code patch} version exists, bumps the count part.
     * <p>
     * Example <i>new pre release</i>:
     * <pre>
     * {@code
     * releaseScope = "minor"
     * nearestVersion = "1.2.0"
     * nearestAnyVersion = ""
     * inferred = "1.3.0-rc.1"
     * }
     * </pre>
     * <p>
     * Example <i>pre release version exists</i>:
     * <pre>
     * {@code
     * releaseScope = "minor"
     * nearestVersion = "1.2.0"
     * nearestAnyVersion = "1.3.0-rc.1"
     * inferred = "1.3.0-rc.2"
     * }
     * </pre>
     * <p>
     * Example <i>last final release higher than pre-release</i>:
     * <pre>
     * {@code
     * releaseScope = "minor"
     * nearestVersion = "1.3.0"
     * nearestAnyVersion = "1.3.0-rc.1"
     * inferred = "1.4.0-rc.1"
     * }
     * </pre>
     */
    static final SemVerStrategy PRE_RELEASE = DEFAULT.copyWith(
            name: 'pre-release',
            stages: ['rc', 'staging'] as SortedSet,
            preReleaseStrategy: all(Strategies.PreRelease.STAGE_FIXED, Strategies.PreRelease.COUNT_INCREMENTED),
            releaseStage: ReleaseStage.Prerelease
    )

    /**
     * Returns a version strategy to be used for {@code development} builds.
     * <p>
     * This strategy creates a unique version string based on last commit hash and the
     * distance of commits to nearest normal version.
     * This version string is not compatible with <a href="https://fsprojects.github.io/Paket/">Paket</a> or
     * <a href="https://www.nuget.org/">Nuget</a>
     * <p>
     * Example:
     * <pre>
     * {@code
     * releaseScope = "minor"
     * nearestVersion = "1.3.0"
     * commitHash = "90c90b9"
     * distance = 22
     * inferred = "1.3.0-dev.22+90c90b9"
     * }
     * </pre>
     */
    static final SemVerStrategy DEVELOPMENT = Strategies.DEVELOPMENT.copyWith(
            normalStrategy: scopes,
            buildMetadataStrategy: NetflixOssStrategies.BuildMetadata.DEVELOPMENT_METADATA_STRATEGY
    )

    /**
     * Returns a version strategy to be used for {@code snapshot} builds.
     * <p>
     * This strategy creates a snapshot version based on <a href="https://semver.org/spec/v2.0.0.html">Semver 2.0.0</a>.
     * Branch names will be encoded in the pre-release part of the version.
     * <p>
     * Example <i>from master branch</i>:
     * <pre>
     * {@code
     * releaseScope = "minor"
     * nearestVersion = "1.3.0"
     * branch = "master"
     * distance = 22
     * inferred = "1.4.0-master.22"
     * }
     * </pre>
     * <p>
     * Example <i>from topic branch</i>:
     * <pre>
     * {@code
     * releaseScope = "minor"
     * nearestVersion = "1.3.0"
     * branch = "feature/fix_22"
     * distance = 34
     * inferred = "1.4.0-branch.feature.fix.22.34"
     * }
     * </pre>
     */
    static final SemVerStrategy SNAPSHOT = DEFAULT.copyWith(
            name: 'snapshot',
            stages: ['ci', 'snapshot', 'SNAPSHOT'] as SortedSet,
            createTag: false,
            preReleaseStrategy: all(PreRelease.STAGE_BRANCH_NAME, Strategies.PreRelease.COUNT_COMMITS_SINCE_ANY),
            enforcePrecedence: false,
            releaseStage: ReleaseStage.Snapshot
    )
}
