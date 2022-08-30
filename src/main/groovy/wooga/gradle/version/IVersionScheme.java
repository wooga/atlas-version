package wooga.gradle.version;

import wooga.gradle.version.internal.release.semver.SemVerStrategy;

import java.util.List;
//This has to be a java class (not groovy) because groovy 2 doesn't support java 8 default interface methods.
public interface IVersionScheme { //TODO: Rename to VersionScheme when breaking change
    SemVerStrategy getDevelopment();
    SemVerStrategy getSnapshot();
    SemVerStrategy getPreRelease();
    SemVerStrategy getFinalStrategy();

    List<SemVerStrategy> getStrategies();
    SemVerStrategy getDefaultStrategy();

    default SemVerStrategy strategyFor(ReleaseStage stage) {
        return getStrategies().stream().filter( it -> it.getReleaseStage() == stage).findFirst().orElse(null);
    }
}
