package wooga.gradle.version.strategies.opinion

import wooga.gradle.version.ReleaseStage
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.SemVerStrategy

import static wooga.gradle.version.internal.release.semver.StrategyUtil.all

class WdkNuGetStrategies {

    static final SemVerStrategy DEVELOPMENT = LegacyNuGetStrategies.DEVELOPMENT
    static final SemVerStrategy SNAPSHOT = LegacyNuGetStrategies.SNAPSHOT
    static final SemVerStrategy PREFLIGHT = LegacyNuGetStrategies.SNAPSHOT.copyWith(
        releaseStage: ReleaseStage.Preflight,
        // TODO: Must start Between m-r
        stages: ['pre', 'preflight'] as SortedSet,
        preReleaseStrategy: all(Strategies.PreRelease.STAGE_FIXED, Strategies.PreRelease.withTimestamp(""))
    )
    static final SemVerStrategy PRE_RELEASE = LegacyNuGetStrategies.PRE_RELEASE
    static final SemVerStrategy FINAL = LegacyNuGetStrategies.FINAL
}
