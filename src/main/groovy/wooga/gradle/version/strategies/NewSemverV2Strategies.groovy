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

import wooga.gradle.version.VersionScheme
import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.strategies.partials.NormalPartials

import static wooga.gradle.version.internal.release.semver.StrategyUtil.*


final class NewSemverV2Strategies {

    static final NORMAL_STRATEGY = one(
            Strategies.Normal.USE_SCOPE_STATE,
            NormalPartials.SCOPE_FROM_BRANCH,
            Strategies.Normal.USE_NEAREST_ANY
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
    static final SemVerStrategy FINAL = Strategies.FINAL.copyWith(
            name: 'production',
            allowDirtyRepo: true,
            normalStrategy: NORMAL_STRATEGY,
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
    static final SemVerStrategy PRE_RELEASE = Strategies.PRE_RELEASE.copyWith(
            name: 'pre-release',
            normalStrategy: NORMAL_STRATEGY,
            stages: ['rc', 'staging'] as SortedSet,
            preReleaseStrategy: all(Strategies.PreRelease.STAGE_FIXED, Strategies.PreRelease.COUNT_INCREMENTED),
            releaseStage: ReleaseStage.Prerelease,
            allowDirtyRepo: true,
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
            normalStrategy: NORMAL_STRATEGY,
            buildMetadataStrategy: Strategies.BuildMetadata.COMMIT_ABBREVIATED_ID,
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
    static final SemVerStrategy SNAPSHOT = Strategies.SNAPSHOT.copyWith(
            stages: ['ci', 'snapshot', 'SNAPSHOT'] as SortedSet,
            normalStrategy: NORMAL_STRATEGY,
            allowDirtyRepo: true,
            preReleaseStrategy: all(Strategies.PreRelease.WITH_BRANCH_NAME,
                                    Strategies.PreRelease.COUNT_COMMITS_SINCE_ANY),
    )

    static final VersionScheme scheme = new VersionScheme() {
        final SemVerStrategy development = DEVELOPMENT
        final SemVerStrategy snapshot = SNAPSHOT
        final SemVerStrategy preRelease = PRE_RELEASE
        final SemVerStrategy finalStrategy = FINAL
        final SemVerStrategy defaultStrategy = DEVELOPMENT
        final List<SemVerStrategy> strategies = [development, snapshot, preRelease, finalStrategy]
    }
}
