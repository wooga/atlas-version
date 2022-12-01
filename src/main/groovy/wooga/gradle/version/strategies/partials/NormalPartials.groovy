package wooga.gradle.version.strategies.partials


import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.ChangeScope
import static wooga.gradle.version.internal.release.semver.StrategyUtil.one

class NormalPartials {

    static final SCOPE_GITFLOW_BRANCH_TYPE = one(
            Strategies.Normal.scopeIfBranchMatchesPattern(~/hotfix(?:\/|-).+$/, ChangeScope.PATCH),
            Strategies.Normal.scopeIfBranchMatchesPattern(~/feature(?:\/|-).+$/, ChangeScope.MINOR),
            Strategies.Normal.scopeIfBranchMatchesPattern(~/fix(?:\/|-).+$/, ChangeScope.MINOR),
    )

    static final SCOPE_EMBED_IN_BRANCH = one(
            Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_X,
            Strategies.Normal.ENFORCE_GITFLOW_BRANCH_MAJOR_MINOR_X,
            Strategies.Normal.ENFORCE_BRANCH_MAJOR_X,
            Strategies.Normal.ENFORCE_BRANCH_MAJOR_MINOR_X,
    )

    static final SCOPE_FROM_BRANCH = one(
            SCOPE_GITFLOW_BRANCH_TYPE,
            SCOPE_EMBED_IN_BRANCH
    )
}
