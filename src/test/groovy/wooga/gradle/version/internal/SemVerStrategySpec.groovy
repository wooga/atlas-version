package wooga.gradle.version.internal

import org.ajoberstar.grgit.Status
import spock.lang.Specification
import wooga.gradle.version.internal.release.semver.SemVerStrategy

class SemVerStrategySpec extends Specification {

    Map changes() {
        Map result = [:]
        result["added"] = []
        result["modified"] = []
        result["removed"] = []
        result
    }

    def "generates status string"() {
        given:
        def args = [:]

        def staged = changes()
        staged["added"] = ["foo"]
        args["staged"] = staged

        def unstaged = changes()
        unstaged["removed"] = ["bar"]
        args["unstaged"] = unstaged

        def conflicts = []
        args["conflicts"] = conflicts

        def status = new Status(args)

        when:
        def str = SemVerStrategy.composeRepositoryStatus(status)

        then:
        str.contains("[ADDED] foo")
        str.contains("[REMOVED] bar")
    }
}
