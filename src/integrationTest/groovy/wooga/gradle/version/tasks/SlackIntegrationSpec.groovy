package wooga.gradle.version.tasks

import wooga.gradle.version.versionPlugin

class SlackIntegrationSpec extends wooga.gradle.version.IntegrationSpec {

    def setup() {

        buildFile << """
            ${applyPlugin(versionPlugin)}
        """.stripIndent()
    }

    def "can run task successfully"() {
        expect:
        runTasksSuccessfully("example")
    }

}
