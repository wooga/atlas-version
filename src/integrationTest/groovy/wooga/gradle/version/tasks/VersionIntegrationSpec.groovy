package wooga.gradle.version.tasks

import wooga.gradle.version.VersionPlugin

class VersionIntegrationSpec extends wooga.gradle.version.IntegrationSpec {

    def setup() {

        buildFile << """
            ${applyPlugin(VersionPlugin)}
        """.stripIndent()
    }

    def "can run task successfully"() {
        expect:
        runTasksSuccessfully("example")
    }

}
