package wooga.gradle.version.strategies.opinion

import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.SemVerStrategy

import static wooga.gradle.version.internal.release.semver.StrategyUtil.all

/**
 * WDK strategies for NuGet/Paket. Based on LegacyNuGetStrategies, plus an extra PREFLIGHT stage.
 */
class WdkNuGetStrategies {
    /**
     * Same as LegacyNuGetStrategies.DEVELOPMENT
     */
    static final SemVerStrategy DEVELOPMENT = LegacyNuGetStrategies.DEVELOPMENT
    /**
     * Same as LegacyNuGetStrategies.SNAPSHOT
     */
    static final SemVerStrategy SNAPSHOT = LegacyNuGetStrategies.SNAPSHOT
    /**
     * Returns a version strategy to be used for {@code preflight} builds.
     * <p>
     * This strategy creates a wdk-specific preflight version based on Semver 1.0.
     * The pre-release part of the version consists of the stage name(pre or preflight) and a timestamp (yyyyMMddHHmm), with no separation.
     * There are no metadata part in the generated versions.
     * <p>
     * Example:
     <pre>
     {@code
     stage = "pre"
     releaseScope = "minor"
     nearestVersion = "1.3.0"
     current date = 01/01/2022 10:10:30
     inferred = "1.4.0-pre202201011010"
     }
     */
    static final SemVerStrategy PREFLIGHT = LegacyNuGetStrategies.SNAPSHOT.copyWith(
        releaseStage: ReleaseStage.Preflight,
        // TODO: Must start Between m-r
        stages: ['pre', 'preflight'] as SortedSet,
        preReleaseStrategy: all(Strategies.PreRelease.STAGE_FIXED, Strategies.PreRelease.withTimestamp(""))
    )
    /**
     * Same as LegacyNuGetStrategies.PRE_RELEASE
     */
    static final SemVerStrategy PRE_RELEASE = LegacyNuGetStrategies.PRE_RELEASE
    /**
     * Same as LegacyNuGetStrategies.FINAL
     */
    static final SemVerStrategy FINAL = LegacyNuGetStrategies.FINAL
}
