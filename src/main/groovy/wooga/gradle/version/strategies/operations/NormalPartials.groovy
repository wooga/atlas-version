package wooga.gradle.version.strategies.operations

import org.gradle.api.Project
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategyState

import java.util.regex.Pattern

import static wooga.gradle.version.internal.release.semver.StrategyUtil.closure
import static wooga.gradle.version.internal.release.semver.StrategyUtil.one

class NormalPartials {

    static final SCOPE_GITFLOW_BRANCH_TYPE = one(
            scopeForBranchPattern(~/hotfix(?:\/|-).+$/, ChangeScope.PATCH),
            scopeForBranchPattern(~/feature(?:\/|-).+$/, ChangeScope.MINOR),
            scopeForBranchPattern(~/fix(?:\/|-).+$/, ChangeScope.MINOR),
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

    /**
     * If the nearest any is higher from the nearest normal, sets the
     * normal component to the nearest any's normal component. Otherwise
     * do nothing.
     *
     * <p>
     * For example, if the nearest any is {@code 1.2.3-alpha.1} and the
     * nearest normal is {@code 1.2.2}, this will infer the normal
     * component as {@code 1.2.3}.
     * </p>
     */
    static final PartialSemVerStrategy NEAREST_HIGHER_ANY = closure { state ->
        def nearest = state.nearestVersion
        if (nearest.any.lessThanOrEqualTo(nearest.normal)) {
            return state
        } else {
            return state.copyWith(inferredNormal: nearest.any.normalVersion)
        }
    }

    static PartialSemVerStrategy scopeForBranchPattern(Pattern pattern, ChangeScope scope) {
        return closure { SemVerStrategyState state ->
            def m = state.currentBranch.name =~ pattern
            if (m.matches()) {
                return Strategies.Normal.useScope(scope).infer(state)
            }
            return state
        }
    }


}
