package wooga.gradle.version.strategies.opinion


import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.internal.release.semver.StrategyUtil
import wooga.gradle.version.strategies.SemverV1Strategies

/**
 * NuGet/Paket compatible strategies. Uses SemverV1Strategies as basis,
 * plus using MINOR as a fallback if no change scope is provided.
 */
class LegacyNuGetStrategies {

    /**
     * Scope that will be set when no scope is provided to the strategies under this class
     */
    static final ChangeScope DEFAULT_SCOPE = ChangeScope.MINOR

    /**
     * Similar to SemverV1Strategies.DEVELOPMENT, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy DEVELOPMENT = SemverV1Strategies.DEVELOPMENT.copyWith(
            normalStrategy: chainDefaultScope(SemverV1Strategies.DEVELOPMENT)
    )
    /**
     * Similar to SemverV1Strategies.SNAPSHOT, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy SNAPSHOT = SemverV1Strategies.SNAPSHOT.copyWith(
            normalStrategy: chainDefaultScope(SemverV1Strategies.SNAPSHOT)
    )
    /**
     * Similar to SemverV1Strategies.PRE_RELEASE, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy PRE_RELEASE = SemverV1Strategies.PRE_RELEASE.copyWith(
            normalStrategy: chainDefaultScope(SemverV1Strategies.PRE_RELEASE)
    )
    /**
     * Similar to SemverV1Strategies.FINAL, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy FINAL = SemverV1Strategies.FINAL.copyWith(
            normalStrategy: chainDefaultScope(SemverV1Strategies.FINAL)
    )

    private static PartialSemVerStrategy chainDefaultScope(SemVerStrategy base, ChangeScope scope = DEFAULT_SCOPE) {
        return StrategyUtil.one(base.normalStrategy, Strategies.Normal.useScope(scope))
    }
}
