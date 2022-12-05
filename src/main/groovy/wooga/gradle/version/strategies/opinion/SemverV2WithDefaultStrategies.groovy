package wooga.gradle.version.strategies.opinion

import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.internal.release.semver.StrategyUtil
import wooga.gradle.version.strategies.NewSemverV2Strategies

/**
 * SemverV2 strategies that uses MINOR as a fallback in case a change scope is not provided.
 */
class SemverV2WithDefaultStrategies {

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
     static final SemVerStrategy FINAL = NewSemverV2Strategies.FINAL.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.FINAL)
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
    static final SemVerStrategy PRE_RELEASE = NewSemverV2Strategies.PRE_RELEASE.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.PRE_RELEASE)
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
    static final SemVerStrategy DEVELOPMENT = NewSemverV2Strategies.DEVELOPMENT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.DEVELOPMENT),
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
    static final SemVerStrategy SNAPSHOT = NewSemverV2Strategies.SNAPSHOT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.SNAPSHOT),
    )

    private static PartialSemVerStrategy chainDefaultScope(SemVerStrategy base, ChangeScope scope = ChangeScope.MINOR) {
        return StrategyUtil.one(base.normalStrategy, Strategies.Normal.useScope(scope))
    }

}
