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

package wooga.gradle.version.strategies

import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.SemVerStrategy

import static wooga.gradle.version.internal.release.semver.StrategyUtil.all
import static wooga.gradle.version.internal.release.semver.StrategyUtil.one

class StaticMarkerStrategies {

    private static final scopes = one(Strategies.Normal.USE_MARKER_TAG)

    static final SemVerStrategy DEFAULT = new SemVerStrategy(
            name: '',
            stages: [] as SortedSet,
            allowDirtyRepo: true,
            normalStrategy: scopes,
            preReleaseStrategy: Strategies.PreRelease.NONE,
            buildMetadataStrategy: Strategies.BuildMetadata.NONE,
            createTag: true,
            enforcePrecedence: true
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
            stages: ['production'] as SortedSet
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
            stages: ['staging'] as SortedSet,
            preReleaseStrategy: all(Strategies.PreRelease.STAGE_FIXED, Strategies.PreRelease.COUNT_INCREMENTED)
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
            buildMetadataStrategy: NetflixOssStrategies.BuildMetadata.DEVELOPMENT_METADATA_STRATEGY)

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
            stages: ['ci'] as SortedSet,
            createTag: false,
            preReleaseStrategy: all(SemverV2Strategies.PreRelease.STAGE_BRANCH_NAME, Strategies.PreRelease.COUNT_COMMITS_SINCE_MARKER),
            enforcePrecedence: false
    )
}
