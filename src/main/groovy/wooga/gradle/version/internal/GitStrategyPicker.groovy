package wooga.gradle.version.internal

import org.ajoberstar.grgit.Grgit
import wooga.gradle.version.VersionScheme
import wooga.gradle.version.internal.release.base.DefaultVersionStrategy
import wooga.gradle.version.internal.release.base.VersionStrategy

import javax.annotation.Nullable

/**
 * Picks strategies based on data from the given git repository.
 */
class GitStrategyPicker {

    final Grgit git

    GitStrategyPicker(Grgit git) {
        this.git = git
    }

    /**
     * Picks one of the strategies in the given scheme, if any of them matches with the stage.
     * @param scheme - IVersionScheme containing the available strategies and a possible default
     * @param stage - Stage name to be be matched with strategies. Can be null.
     * If null, the stage to be used in execution will depend if a fallback is set in the selected strategy itself.
     * @return Selected VersionStrategy or null if no strategy matches.
     */
    VersionStrategy pickStrategy(VersionScheme scheme, @Nullable String stage) {
        return pickStrategy(scheme.strategies, scheme.defaultStrategy, stage)
    }

    /**
     * Picks a strategy from available strategies, falling back to a default if it can be used.
     * @param availableStrategies - Strategies available to be selected
     * @param defaultStrategy - Default strategy to be used if none of the strategies from availableStrategies can be used. Can be null
     * @param stage - Stage name to be be matched with strategies. Can be null.
     * If null, the stage to be used in execution will depend if a fallback is set in the selected strategy itself.
     * @return Selected VersionStrategy or null if no strategy matches
     */
    VersionStrategy pickStrategy(List<VersionStrategy> availableStrategies,
                                   VersionStrategy defaultStrategy,
                                   @Nullable String stage) {
        Optional<VersionStrategy> selectedStrategy = availableStrategies.stream()
                .filter { strategy -> strategy.selector(stage, git) }
                .findFirst()
        Optional<VersionStrategy> fallbackStrategy = Optional.ofNullable(defaultStrategy).map { defaultStrat ->
            shouldUseDefaultStrategy(defaultStrat, stage)? defaultStrat : null
        }
        return selectedStrategy.orElse(fallbackStrategy.orElse(null));
    }

    private boolean shouldUseDefaultStrategy(VersionStrategy defaultStrategy, String stage) {
        if (defaultStrategy instanceof DefaultVersionStrategy) {
            return ((DefaultVersionStrategy)defaultStrategy).defaultSelector(stage, git);
        }
        return defaultStrategy.selector(stage, git);
    }
}
