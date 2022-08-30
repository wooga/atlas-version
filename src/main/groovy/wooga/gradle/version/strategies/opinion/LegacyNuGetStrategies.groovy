package wooga.gradle.version.strategies.opinion


import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.internal.release.semver.StrategyUtil
import wooga.gradle.version.strategies.NewSemverV1Strategies

class LegacyNuGetStrategies {

    static final ChangeScope DEFAULT_SCOPE = ChangeScope.PATCH

    static final SemVerStrategy DEVELOPMENT = NewSemverV1Strategies.DEVELOPMENT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.DEVELOPMENT)
    )
    static final SemVerStrategy SNAPSHOT = NewSemverV1Strategies.SNAPSHOT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.SNAPSHOT)
    )
    static final SemVerStrategy PRE_RELEASE = NewSemverV1Strategies.PRE_RELEASE.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.PRE_RELEASE)
    )
    static final SemVerStrategy FINAL = NewSemverV1Strategies.FINAL.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV1Strategies.FINAL)
    )

    private static PartialSemVerStrategy chainDefaultScope(SemVerStrategy base, ChangeScope scope = DEFAULT_SCOPE) {
        return StrategyUtil.one(base.normalStrategy, Strategies.Normal.useScope(scope))
    }
}
