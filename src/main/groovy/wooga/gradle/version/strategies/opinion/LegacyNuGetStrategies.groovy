package wooga.gradle.version.strategies.opinion


import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.internal.release.semver.StrategyUtil
import wooga.gradle.version.strategies.NewSemverV1Strategies

/**
 * NuGet/Paket compatible strategies. Uses NewSemverV1Strategies as basis.
 */
class LegacyNuGetStrategies {

    /**
     * Scope that will be set when no scope is provided to the strategies under this class
     */
    static final ChangeScope DEFAULT_SCOPE = ChangeScope.PATCH

    /**
     * Similar to NewSemverV1Strategies.DEVELOPMENT, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy DEVELOPMENT = NewSemverV1Strategies.DEVELOPMENT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.DEVELOPMENT)
    )
    /**
     * Similar to NewSemverV1Strategies.SNAPSHOT, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy SNAPSHOT = NewSemverV1Strategies.SNAPSHOT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.SNAPSHOT)
    )
    /**
     * Similar to NewSemverV1Strategies.PRE_RELEASE, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy PRE_RELEASE = NewSemverV1Strategies.PRE_RELEASE.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.PRE_RELEASE)
    )
    /**
     * Similar to NewSemverV1Strategies.FINAL, but its scope defaults to DEFAULT_SCOPE when no scope is provide
     */
    static final SemVerStrategy FINAL = NewSemverV1Strategies.FINAL.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.FINAL)
    )

    // Adds a default to
    private static PartialSemVerStrategy chainDefaultScope(SemVerStrategy base, ChangeScope scope = DEFAULT_SCOPE) {
        return StrategyUtil.one(base.normalStrategy, Strategies.Normal.useScope(scope))
    }
}
