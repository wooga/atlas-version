package wooga.gradle.version;

import wooga.gradle.version.internal.release.base.VersionStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Version schemes available for use with this plugin.
 * Each scheme consists of at least 4 strategies (development, snapshot, pre-release and final)
 * plus additional strategies that can be provided in getStrategies() and fetched with strategyFor(ReleaseStage).
 *
 * A default strategy can also be provided.
 */
//This has to be a java class (not groovy) because groovy 2 doesn't support java 8 default interface methods.
public interface VersionScheme { //TODO: Rename to VersionScheme when breaking change
    VersionStrategy getDevelopment();
    VersionStrategy getSnapshot();
    VersionStrategy getPreRelease();
    //is not getFinal because final is a java keyword and this would mess with groovy's property invocation.
    VersionStrategy getFinalStrategy();

    /**
     * @return All available strategies for this scheme. Heavily recomended to include development, snapshot,
     * preRelease and Final here as well, put here all non-standard strategies that you wish to provide.
     */
    List<VersionStrategy> getStrategies();
    VersionStrategy getDefaultStrategy();

    /**
     * Finds a strategy in this scheme with the given ReleaseStage
     * @param stage - ReleaseStage for the desired strategy
     * @return the desired VersionStrategy or null if none are found.
     */
    default VersionStrategy strategyFor(ReleaseStage stage) {
        return getStrategies().stream().filter( it -> it.getReleaseStage() == stage).findFirst().orElse(null);
    }

    /**
     * Finds the matching strategy for a given stage name and returns its ReleaseStage object.
     * @param stageName - Desired stage name. Will be matched against the strategies in this scheme.
     * @return ReleaseStage for the first strategy with a matching stage name.
     */
    default ReleaseStage findStageForStageName(String stageName) {
        return Arrays.stream(ReleaseStage.values()).filter(
            candidateStage -> Optional
                    .ofNullable(strategyFor(candidateStage))
                    .map(strategy -> strategy.getStages().contains(stageName))
                    .orElse(false)
        ).findFirst().orElse(null);
    }
}
