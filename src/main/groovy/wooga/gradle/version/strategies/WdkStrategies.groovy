package wooga.gradle.version.strategies

import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.internal.release.semver.StrategyUtil

class WdkStrategies {
    static final SemVerStrategy DEVELOPMENT = SemverV1Strategies.DEVELOPMENT
    static final SemVerStrategy SNAPSHOT = SemverV1Strategies.SNAPSHOT
    static final SemVerStrategy PREFLIGHT = SemverV1Strategies.SNAPSHOT.copyWith(
        releaseStage: ReleaseStage.Preflight,
        // TODO: Must start Between m-r
        stages: ['pre', 'preflight'] as SortedSet,
        preReleaseStrategy: StrategyUtil.all(StrategyUtil.closure({ state ->
            def stage = Strategies.PreRelease.STAGE_FIXED.infer(state).inferredPreRelease
            def integration = Strategies.PreRelease.STAGE_TIMESTAMP.infer(state).inferredPreRelease
            state.copyWith(inferredPreRelease: "$stage$integration")
        })),
    )
    static final SemVerStrategy PRE_RELEASE = SemverV1Strategies.PRE_RELEASE
    static final SemVerStrategy FINAL = SemverV1Strategies.FINAL
}
