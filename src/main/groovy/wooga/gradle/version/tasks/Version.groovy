package wooga.gradle.version.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class Version extends DefaultTask {

    @TaskAction
    protected void action() {
        logger.info('execute')
    }

}
