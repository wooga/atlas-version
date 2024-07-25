package wooga.gradle.version.internal

import org.ajoberstar.grgit.Grgit
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Implemented from https://docs.gradle.org/current/userguide/build_services.html#build_services
 */
abstract class GitBuildService implements BuildService<BuildServiceParameters.None>, AutoCloseable {

    List<Grgit> instances = []

    GitBuildService() {
    }

    /**
     * Opens the repository at the given directory path. When the gradle build has finished,
     * will automatically close it
     * @param directory The file path to the git repository
     * @return A managed git repository
     */
    Grgit getRepository(String directory) {
        Grgit git = Grgit.open(dir: directory)
        instances.add(git)
        return git
    }

    @Override
    void close() throws Exception {
        for(instance in instances) {
            System.out.println("Closing Git repository: ${instance.repository.rootDir}")
            instance.close()
        }
    }
}
