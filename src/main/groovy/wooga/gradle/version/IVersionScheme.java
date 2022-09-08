package wooga.gradle.version;

import wooga.gradle.version.internal.release.base.VersionStrategy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//This has to be a java class (not groovy) because groovy 2 doesn't support java 8 default interface methods.
public interface IVersionScheme { //TODO: Rename to VersionScheme when breaking change
    VersionStrategy getDevelopment();
    VersionStrategy getSnapshot();
    VersionStrategy getPreRelease();
    VersionStrategy getFinalStrategy();

    List<VersionStrategy> getStrategies();
    VersionStrategy getDefaultStrategy();

    default VersionStrategy strategyFor(ReleaseStage stage) {
        return getStrategies().stream().filter( it -> it.getReleaseStage() == stage).findFirst().orElse(null);
    }

    default ReleaseStage findStageForStageName(String stageName) {
        return Arrays.stream(ReleaseStage.values()).filter(
            candidateStage -> Optional
                    .ofNullable(strategyFor(candidateStage))
                    .map(strategy -> strategy.getStages().contains(stageName))
                    .orElse(false)
        ).findFirst().orElse(null);
    }
}
