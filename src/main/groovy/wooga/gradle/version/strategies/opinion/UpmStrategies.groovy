package wooga.gradle.version.strategies.opinion


import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.*
import wooga.gradle.version.strategies.SemverV2Strategies

import static wooga.gradle.version.internal.release.semver.StrategyUtil.*

/**
 * UPM-Specific strategies aimed at WDKs. Based on SemverV2Strategies,
 * plus the same default stage as the older LegacyNuGetStrategies.
 */
class UpmStrategies {

    /**
     * Same as SemverV2Strategies.DEVELOPMENT plus LegacyNuGetStrategies.DEFAULT_SCOPE as a default in case no change scope is provided.
     */
    static final SemVerStrategy DEVELOPMENT = SemverV2Strategies.DEVELOPMENT.copyWith(
            normalStrategy: chainDefaultScope(SemverV2Strategies.DEVELOPMENT)
    )
    /**
     * Same as SemverV2Strategies.SNAPSHOT plus LegacyNuGetStrategies.DEFAULT_SCOPE as a default in case no change scope is provided.
     */
    static final SemVerStrategy SNAPSHOT = SemverV2Strategies.SNAPSHOT.copyWith(
            normalStrategy: chainDefaultScope(SemverV2Strategies.SNAPSHOT)
    )
    /**
     * Returns a version strategy to be used for {@code preflight} builds.
     * <p>
     * This strategy creates a wdk-specific preflight version based on Semver 2.0.
     * The pre-release part of the version consists of the stage name(pre or preflight) and a timestamp (yyyyMMddHHmm), separated by dot(.).
     * There are no metadata part in the generated versions.
     * <p>
     * Example:
     <pre>
      {@code
      stage = "pre"
      releaseScope = "minor"
      nearestVersion = "1.3.0"
      current date = 01/01/2022 10:10:30
      inferred = "1.4.0-pre.202201011010"
      }
     */
    static final SemVerStrategy PREFLIGHT = SemverV2Strategies.SNAPSHOT.copyWith(
        releaseStage: ReleaseStage.Preflight,
        stages: ['pre', 'preflight'] as SortedSet,
        normalStrategy: chainDefaultScope(SemverV2Strategies.SNAPSHOT),
        preReleaseStrategy: all(
                Strategies.PreRelease.STAGE_FIXED,
                Strategies.PreRelease.withTimestamp(".")
        ),
    )
    /**
     * Same as SemverV2Strategies.PRE_RELEASE plus LegacyNuGetStrategies.DEFAULT_SCOPE as a default in case no change scope is provided.
     */
    static final SemVerStrategy PRE_RELEASE = SemverV2Strategies.PRE_RELEASE.copyWith(
            normalStrategy: chainDefaultScope(SemverV2Strategies.PRE_RELEASE)
    )
    /**
     * Same as SemverV2Strategies.FINAL plus LegacyNuGetStrategies.DEFAULT_SCOPE as a default in case no change scope is provided.
     */
    static final SemVerStrategy FINAL = SemverV2Strategies.FINAL.copyWith(
            normalStrategy: chainDefaultScope(SemverV2Strategies.FINAL)
    )

    private static PartialSemVerStrategy chainDefaultScope(SemVerStrategy base,
                                                           ChangeScope scope = LegacyNuGetStrategies.DEFAULT_SCOPE) {
        return one(base.normalStrategy, Strategies.Normal.useScope(scope))
    }
}
