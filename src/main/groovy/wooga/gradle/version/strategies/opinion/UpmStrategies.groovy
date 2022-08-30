package wooga.gradle.version.strategies.opinion


import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.*
import wooga.gradle.version.strategies.NewSemverV2Strategies

import static wooga.gradle.version.internal.release.semver.StrategyUtil.*

class UpmStrategies {

    static final SemVerStrategy DEVELOPMENT = NewSemverV2Strategies.DEVELOPMENT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.DEVELOPMENT)
    )
    static final SemVerStrategy SNAPSHOT = NewSemverV2Strategies.SNAPSHOT.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.SNAPSHOT)
    )
    static final SemVerStrategy PREFLIGHT = NewSemverV2Strategies.SNAPSHOT.copyWith(
        releaseStage: ReleaseStage.Preflight,
        stages: ['pre', 'preflight'] as SortedSet,
        normalStrategy: chainDefaultScope(NewSemverV2Strategies.SNAPSHOT),
        preReleaseStrategy: all(
                Strategies.PreRelease.STAGE_FIXED,
                Strategies.PreRelease.withTimestamp(".")
        ),
    )
    static final SemVerStrategy PRE_RELEASE = NewSemverV2Strategies.PRE_RELEASE.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.PRE_RELEASE),
//            preReleaseStrategy: all(,
//            closure { SemVerStrategyState state ->
//                def nearest = state.nearestVersion
//                NewSemverV2Strategies.PRE_RELEASE.preReleaseStrategy
//                nearest.any.preReleaseVersion
//                return state
//            }
    )
    static final SemVerStrategy FINAL = NewSemverV2Strategies.FINAL.copyWith(
            normalStrategy: chainDefaultScope(NewSemverV2Strategies.FINAL)
    )

    private static PartialSemVerStrategy chainDefaultScope(SemVerStrategy base,
                                                           ChangeScope scope = LegacyNuGetStrategies.DEFAULT_SCOPE) {
        return one(base.normalStrategy, Strategies.Normal.useScope(scope))
    }

}
