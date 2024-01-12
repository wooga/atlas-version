package wooga.gradle.version

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit
import spock.lang.Unroll
import wooga.gradle.version.internal.release.opinion.Strategies

class UPMStrategySpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.version'

    Grgit git

    def setup() {
        new File(projectDir, '.gitignore') << """
        userHome/
        """.stripIndent()

        git = Grgit.init(dir: projectDir)
        git.add(patterns: ['.gitignore'])
        git.commit(message: 'initial commit')
    }

    @Unroll('verify inferred upm version from nearestNormal: #nearestNormal, nearestAny: #nearestAnyTitle, scope: #scopeTitle, stage: #stage and branch: #branchName to be #expectedVersion')
    def "uses custom wooga application strategies for upm"() {
        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
        project.ext.set('version.scheme', 'upm')
        if (scope != _) {
            project.ext.set('release.scope', scope)
        }

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }
        if (nearestNormal != _) {
            1.times {
                git.commit(message: 'feature commit')
            }
            git.tag.add(name: "v$nearestNormal")
        }

        distance.times {
            git.commit(message: 'fix commit')
        }

        if (nearestAny != _) {
            git.tag.add(name: "v$nearestAny")
            distance.times {
                git.commit(message: 'fix commit')
            }
        }

        if (expectedVersion.contains("#TIMESTAMP")) {
            expectedVersion = expectedVersion.replace("#TIMESTAMP", Strategies.PreRelease.GenerateTimestamp())
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion

        where:
        nearestNormal | nearestAny          | distance | stage        | scope   | branchName                 | expectedVersion
        _             | _                   | 1        | "ci"         | _       | "master"                   | "0.1.0-master.2"
        _             | _                   | 2        | "ci"         | "major" | "master"                   | "1.0.0-master.3"
        _             | _                   | 3        | "ci"         | "minor" | "master"                   | "0.1.0-master.4"
        _             | _                   | 4        | "ci"         | "patch" | "master"                   | "0.0.1-master.5"

        '1.0.0'       | _                   | 1        | "ci"         | _       | "master"                   | "1.1.0-master.1"
        '1.0.0'       | _                   | 2        | "ci"         | "major" | "master"                   | "2.0.0-master.2"
        '1.0.0'       | _                   | 3        | "ci"         | "minor" | "master"                   | "1.1.0-master.3"
        '1.0.0'       | _                   | 4        | "ci"         | "patch" | "master"                   | "1.0.1-master.4"


        '1.0.0'       | _                   | 1        | "ci"         | _       | "develop"                  | "1.1.0-develop.1"
        '1.0.0'       | _                   | 2        | "ci"         | "major" | "develop"                  | "2.0.0-develop.2"
        '1.0.0'       | _                   | 3        | "ci"         | "minor" | "develop"                  | "1.1.0-develop.3"
        '1.0.0'       | _                   | 4        | "ci"         | "patch" | "develop"                  | "1.0.1-develop.4"

        '1.0.0'       | _                   | 1        | "ci"         | _       | "feature/check"            | "1.1.0-branch.feature.check.1"
        '1.0.0'       | _                   | 2        | "ci"         | _       | "hotfix/check"             | "1.0.1-branch.hotfix.check.2"
        '1.0.0'       | _                   | 3        | "ci"         | _       | "fix/check"                | "1.1.0-branch.fix.check.3"
        '1.0.0'       | _                   | 4        | "ci"         | _       | "feature-check"            | "1.1.0-branch.feature.check.4"
        '1.0.0'       | _                   | 5        | "ci"         | _       | "hotfix-check"             | "1.0.1-branch.hotfix.check.5"
        '1.0.0'       | _                   | 6        | "ci"         | _       | "fix-check"                | "1.1.0-branch.fix.check.6"
        '1.0.0'       | _                   | 7        | "ci"         | _       | "PR-22"                    | "1.1.0-branch.pr.22.7"

        '1.0.0'       | _                   | 1        | "ci"         | _       | "release/1.x"              | "1.1.0-branch.release.1.x.1"
        '1.0.0'       | _                   | 2        | "ci"         | _       | "release-1.x"              | "1.1.0-branch.release.1.x.2"
        '1.0.0'       | _                   | 3        | "ci"         | _       | "release/1.0.x"            | "1.0.1-branch.release.1.0.x.3"
        '1.0.0'       | _                   | 4        | "ci"         | _       | "release-1.0.x"            | "1.0.1-branch.release.1.0.x.4"

        '0.0.1'       | _                   | 1        | "ci"         | _       | "1.x"                      | "1.0.0-branch.1.x.1"
        '1.0.0'       | _                   | 2        | "ci"         | _       | "1.x"                      | "1.1.0-branch.1.x.2"
        '1.0.0'       | _                   | 3        | "ci"         | _       | "1.0.x"                    | "1.0.1-branch.1.0.x.3"

        '1.0.0'       | '2.0.0-rc.2'        | 0        | "ci"         | _       | "release/2.x"              | "2.0.0-branch.release.2.x.0"

        _             | _                   | 1        | "snapshot"   | _       | "master"                   | "0.1.0-master.2"
        _             | _                   | 2        | "snapshot"   | "major" | "master"                   | "1.0.0-master.3"
        _             | _                   | 3        | "snapshot"   | "minor" | "master"                   | "0.1.0-master.4"
        _             | _                   | 4        | "snapshot"   | "patch" | "master"                   | "0.0.1-master.5"

        '1.0.0'       | _                   | 1        | "snapshot"   | _       | "master"                   | "1.1.0-master.1"
        '1.0.0'       | _                   | 2        | "snapshot"   | "major" | "master"                   | "2.0.0-master.2"
        '1.0.0'       | _                   | 3        | "snapshot"   | "minor" | "master"                   | "1.1.0-master.3"
        '1.0.0'       | _                   | 4        | "snapshot"   | "patch" | "master"                   | "1.0.1-master.4"

        '1.0.0'       | _                   | 1        | "snapshot"   | _       | "develop"                  | "1.1.0-develop.1"
        '1.0.0'       | _                   | 2        | "snapshot"   | "major" | "develop"                  | "2.0.0-develop.2"
        '1.0.0'       | _                   | 3        | "snapshot"   | "minor" | "develop"                  | "1.1.0-develop.3"
        '1.0.0'       | _                   | 4        | "snapshot"   | "patch" | "develop"                  | "1.0.1-develop.4"

        '1.0.0'       | _                   | 1        | "snapshot"   | _       | "feature/check"            | "1.1.0-branch.feature.check.1"
        '1.0.0'       | _                   | 2        | "snapshot"   | _       | "hotfix/check"             | "1.0.1-branch.hotfix.check.2"
        '1.0.0'       | _                   | 3        | "snapshot"   | _       | "fix/check"                | "1.1.0-branch.fix.check.3"
        '1.0.0'       | _                   | 4        | "snapshot"   | _       | "feature-check"            | "1.1.0-branch.feature.check.4"
        '1.0.0'       | _                   | 5        | "snapshot"   | _       | "hotfix-check"             | "1.0.1-branch.hotfix.check.5"
        '1.0.0'       | _                   | 6        | "snapshot"   | _       | "fix-check"                | "1.1.0-branch.fix.check.6"
        '1.0.0'       | _                   | 7        | "snapshot"   | _       | "PR-22"                    | "1.1.0-branch.pr.22.7"

        '0.0.1'       | _                   | 1        | "snapshot"   | _       | "release/1.x"              | "1.0.0-branch.release.1.x.1"
        '1.0.0'       | _                   | 1        | "snapshot"   | _       | "release/1.x"              | "1.1.0-branch.release.1.x.1"
        '1.0.0'       | _                   | 2        | "snapshot"   | _       | "release-1.x"              | "1.1.0-branch.release.1.x.2"
        '1.0.0'       | _                   | 3        | "snapshot"   | _       | "release/1.0.x"            | "1.0.1-branch.release.1.0.x.3"
        '1.0.0'       | _                   | 4        | "snapshot"   | _       | "release-1.0.x"            | "1.0.1-branch.release.1.0.x.4"

        '0.0.1'       | _                   | 2        | "snapshot"   | _       | "1.x"                      | "1.0.0-branch.1.x.2"
        '1.0.0'       | _                   | 2        | "snapshot"   | _       | "1.x"                      | "1.1.0-branch.1.x.2"
        '1.0.0'       | _                   | 4        | "snapshot"   | _       | "1.0.x"                    | "1.0.1-branch.1.0.x.4"

        _             | _                   | 1        | "staging"    | _       | "master"                   | "0.1.0-staging.1"
        _             | _                   | 2        | "staging"    | "major" | "master"                   | "1.0.0-staging.1"
        _             | _                   | 3        | "staging"    | "minor" | "master"                   | "0.1.0-staging.1"
        _             | _                   | 4        | "staging"    | "patch" | "master"                   | "0.0.1-staging.1"

        _             | "0.0.1-staging.1"   | 4        | "staging"    | "patch" | "master"                   | "0.0.1-staging.2"
        _             | "0.0.1-staging0001" | 4        | "staging"    | "patch" | "master"                   | "0.0.1-staging.2"

        '1.0.0'       | _                   | 1        | "staging"    | _       | "master"                   | "1.1.0-staging.1"
        '1.0.0'       | _                   | 2        | "staging"    | "major" | "master"                   | "2.0.0-staging.1"
        '1.0.0'       | _                   | 3        | "staging"    | "minor" | "master"                   | "1.1.0-staging.1"
        '1.0.0'       | _                   | 4        | "staging"    | "patch" | "master"                   | "1.0.1-staging.1"

        '1.0.0'       | '1.1.0-staging.1'   | 1        | "staging"    | _       | "master"                   | "1.1.0-staging.2"
        '1.0.0'       | '1.1.0-staging.2'   | 1        | "staging"    | _       | "master"                   | "1.1.0-staging.3"

        '0.0.1'       | _                   | 1        | "staging"    | _       | "release/1.x"              | "1.0.0-staging.1"
        '1.0.0'       | _                   | 1        | "staging"    | _       | "release/1.x"              | "1.1.0-staging.1"
        '1.0.0'       | _                   | 2        | "staging"    | _       | "release-1.x"              | "1.1.0-staging.1"
        '1.0.0'       | _                   | 3        | "staging"    | _       | "release/1.0.x"            | "1.0.1-staging.1"
        '1.0.0'       | _                   | 4        | "staging"    | _       | "release-1.0.x"            | "1.0.1-staging.1"

        '0.0.1'       | _                   | 1        | "staging"    | _       | "1.x"                      | "1.0.0-staging.1"
        '1.0.0'       | _                   | 1        | "staging"    | _       | "1.x"                      | "1.1.0-staging.1"
        '1.0.0'       | _                   | 3        | "staging"    | _       | "1.0.x"                    | "1.0.1-staging.1"

        '1.0.0'       | "1.0.1-staging.1"   | 1        | "staging"    | _       | "release/1.x"              | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1"   | 2        | "staging"    | _       | "release-1.x"              | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1"   | 3        | "staging"    | _       | "release/1.0.x"            | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1"   | 4        | "staging"    | _       | "release-1.0.x"            | "1.0.1-staging.2"

        '1.0.0'       | "1.0.1-staging.1"   | 1        | "staging"    | _       | "1.x"                      | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1"   | 3        | "staging"    | _       | "1.0.x"                    | "1.0.1-staging.2"

        _             | _                   | 1        | "rc"         | _       | "master"                   | "0.1.0-rc.1"
        _             | _                   | 2        | "rc"         | "major" | "master"                   | "1.0.0-rc.1"
        _             | _                   | 3        | "rc"         | "minor" | "master"                   | "0.1.0-rc.1"
        _             | _                   | 4        | "rc"         | "patch" | "master"                   | "0.0.1-rc.1"

        '1.0.0'       | _                   | 1        | "rc"         | _       | "master"                   | "1.1.0-rc.1"
        '1.0.0'       | _                   | 2        | "rc"         | "major" | "master"                   | "2.0.0-rc.1"
        '1.0.0'       | _                   | 3        | "rc"         | "minor" | "master"                   | "1.1.0-rc.1"
        '1.0.0'       | _                   | 4        | "rc"         | "patch" | "master"                   | "1.0.1-rc.1"

        '1.0.0'       | '1.1.0-rc.1'        | 1        | "rc"         | _       | "master"                   | "1.1.0-rc.2"
        '1.0.0'       | '1.1.0-rc.2'        | 1        | "rc"         | _       | "master"                   | "1.1.0-rc.3"

        '0.0.1'       | _                   | 1        | "rc"         | _       | "release/1.x"              | "1.0.0-rc.1"
        '1.0.0'       | _                   | 1        | "rc"         | _       | "release/1.x"              | "1.1.0-rc.1"
        '1.0.0'       | _                   | 2        | "rc"         | _       | "release-1.x"              | "1.1.0-rc.1"
        '1.0.0'       | _                   | 3        | "rc"         | _       | "release/1.0.x"            | "1.0.1-rc.1"
        '1.0.0'       | _                   | 4        | "rc"         | _       | "release-1.0.x"            | "1.0.1-rc.1"

        '0.0.1'       | _                   | 1        | "rc"         | _       | "1.x"                      | "1.0.0-rc.1"
        '1.0.0'       | _                   | 1        | "rc"         | _       | "1.x"                      | "1.1.0-rc.1"
        '1.0.0'       | _                   | 3        | "rc"         | _       | "1.0.x"                    | "1.0.1-rc.1"

        '1.0.0'       | "1.0.1-rc.1"        | 1        | "rc"         | _       | "release/1.x"              | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"        | 2        | "rc"         | _       | "release-1.x"              | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"        | 3        | "rc"         | _       | "release/1.0.x"            | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"        | 4        | "rc"         | _       | "release-1.0.x"            | "1.0.1-rc.2"

        '1.0.0'       | "1.0.1-rc.1"        | 1        | "rc"         | _       | "1.x"                      | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"        | 3        | "rc"         | _       | "1.0.x"                    | "1.0.1-rc.2"

        // Pre uses timestamp
        '1.0.0'       | _                   | 1        | "pre"        | _       | "master"                   | "1.1.0-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 2        | "pre"        | "major" | "master"                   | "2.0.0-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 3        | "pre"        | "minor" | "master"                   | "1.1.0-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 4        | "pre"        | "patch" | "master"                   | "1.0.1-pre.#TIMESTAMP"
        '1.0.0'       | '1.1.0-pre12345'    | 1        | "pre"        | _       | "master"                   | "1.1.0-pre.#TIMESTAMP"
        '1.0.0'       | '1.1.0-pre22345'    | 1        | "pre"        | _       | "master"                   | "1.1.0-pre.#TIMESTAMP"

        '1.0.0'       | _                   | 1        | "pre"        | _       | "release/1.x"              | "1.1.0-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 2        | "pre"        | _       | "release-1.x"              | "1.1.0-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 3        | "pre"        | _       | "release/1.0.x"            | "1.0.1-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 4        | "pre"        | _       | "release-1.0.x"            | "1.0.1-pre.#TIMESTAMP"

        '0.0.1'       | _                   | 1        | "pre"        | _       | "1.x"                      | "1.0.0-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 1        | "pre"        | _       | "1.x"                      | "1.1.0-pre.#TIMESTAMP"
        '1.0.0'       | _                   | 3        | "pre"        | _       | "1.0.x"                    | "1.0.1-pre.#TIMESTAMP"

        '0.0.1'       | "0.0.2-pre12345"    | 1        | "pre"        | _       | "release/1.x"              | "1.0.0-pre.#TIMESTAMP"
        '1.0.0'       | "1.0.1-pre12345"    | 1        | "pre"        | _       | "release/1.x"              | "1.0.1-pre.#TIMESTAMP"
        '1.0.0'       | "1.0.1-pre12345"    | 2        | "pre"        | _       | "release-1.x"              | "1.0.1-pre.#TIMESTAMP"
        '1.0.0'       | "1.0.1-pre12345"    | 3        | "pre"        | _       | "release/1.0.x"            | "1.0.1-pre.#TIMESTAMP"
        '1.0.0'       | "1.0.1-pre12345"    | 4        | "pre"        | _       | "release-1.0.x"            | "1.0.1-pre.#TIMESTAMP"

        '1.0.0'       | "1.0.1-pre12345"    | 1        | "pre"        | _       | "1.x"                      | "1.0.1-pre.#TIMESTAMP"
        '1.0.0'       | "1.0.1-pre12345"    | 3        | "pre"        | _       | "1.0.x"                    | "1.0.1-pre.#TIMESTAMP"

        '1.0.0'       | _                   | 4        | "pre"        | _       | "1.0.x"                    | "1.0.1-pre.#TIMESTAMP"

        '0.0.1'       | "0.0.2-rc.1"        | 1        | "staging"    | _       | "1.x"                      | "1.0.0-staging.1"
        '1.0.0'       | "1.0.1-rc.1"        | 1        | "staging"    | _       | "1.x"                      | "1.0.1-staging.1"
        '1.0.0'       | "1.0.1-rc.1"        | 3        | "staging"    | _       | "1.0.x"                    | "1.0.1-staging.1"

        _             | _                   | 1        | "production" | _       | "master"                   | "0.1.0"
        _             | _                   | 2        | "production" | "major" | "master"                   | "1.0.0"
        _             | _                   | 3        | "production" | "minor" | "master"                   | "0.1.0"
        _             | _                   | 4        | "production" | "patch" | "master"                   | "0.0.1"

        '1.0.0'       | _                   | 1        | "production" | _       | "master"                   | "1.1.0"
        '1.0.0'       | _                   | 2        | "production" | "major" | "master"                   | "2.0.0"
        '1.0.0'       | _                   | 3        | "production" | "minor" | "master"                   | "1.1.0"
        '1.0.0'       | _                   | 4        | "production" | "patch" | "master"                   | "1.0.1"

        '0.0.1'       | _                   | 1        | "production" | _       | "release/1.x"              | "1.0.0"
        '1.0.0'       | _                   | 1        | "production" | _       | "release/1.x"              | "1.1.0"
        '1.0.0'       | _                   | 2        | "production" | _       | "release-1.x"              | "1.1.0"
        '1.0.0'       | _                   | 3        | "production" | _       | "release/1.0.x"            | "1.0.1"
        '1.0.0'       | _                   | 4        | "production" | _       | "release-1.0.x"            | "1.0.1"

        '0.0.1'       | _                   | 1        | "production" | _       | "1.x"                      | "1.0.0"
        '1.0.0'       | _                   | 1        | "production" | _       | "1.x"                      | "1.1.0"
        '1.0.0'       | _                   | 3        | "production" | _       | "1.0.x"                    | "1.0.1"

        _             | _                   | 1        | "final"      | _       | "master"                   | "0.1.0"
        _             | _                   | 2        | "final"      | "major" | "master"                   | "1.0.0"
        _             | _                   | 3        | "final"      | "minor" | "master"                   | "0.1.0"
        _             | _                   | 4        | "final"      | "patch" | "master"                   | "0.0.1"

        '1.0.0'       | _                   | 1        | "final"      | _       | "master"                   | "1.1.0"
        '1.0.0'       | _                   | 2        | "final"      | "major" | "master"                   | "2.0.0"
        '1.0.0'       | _                   | 3        | "final"      | "minor" | "master"                   | "1.1.0"
        '1.0.0'       | _                   | 4        | "final"      | "patch" | "master"                   | "1.0.1"

        '0.0.1'       | _                   | 1        | "final"      | _       | "release/1.x"              | "1.0.0"
        '1.0.0'       | _                   | 1        | "final"      | _       | "release/1.x"              | "1.1.0"
        '1.0.0'       | _                   | 2        | "final"      | _       | "release-1.x"              | "1.1.0"
        '1.0.0'       | _                   | 3        | "final"      | _       | "release/1.0.x"            | "1.0.1"
        '1.0.0'       | _                   | 4        | "final"      | _       | "release-1.0.x"            | "1.0.1"

        '1.0.0'       | _                   | 1        | "final"      | _       | "1.x"                      | "1.1.0"
        '1.0.0'       | _                   | 3        | "final"      | _       | "1.0.x"                    | "1.0.1"

        '1.0.0'       | _                   | 10       | "ci"         | _       | "test/build01-"            | "1.1.0-branch.test.build.1.10"
        '1.0.0'       | _                   | 22       | "ci"         | _       | "test/build01+"            | "1.1.0-branch.test.build.1.22"
        '1.0.0'       | _                   | 45       | "ci"         | _       | "test/build01_"            | "1.1.0-branch.test.build.1.45"
        '1.0.0'       | _                   | 204      | "ci"         | _       | "test/build01"             | "1.1.0-branch.test.build.1.204"
        '1.0.0'       | _                   | 100      | "ci"         | _       | "test/build.01"            | "1.1.0-branch.test.build.1.100"
        '1.0.0'       | _                   | 55       | "ci"         | _       | "test/build002"            | "1.1.0-branch.test.build.2.55"
        '1.0.0'       | _                   | 66       | "ci"         | _       | "test/build.002"           | "1.1.0-branch.test.build.2.66"
        '1.0.0'       | _                   | 789      | "ci"         | _       | "test/build000000000003"   | "1.1.0-branch.test.build.3.789"
        '1.0.0'       | _                   | 777      | "ci"         | _       | "test/build.000000000003"  | "1.1.0-branch.test.build.3.777"
        '1.0.0'       | _                   | 789      | "ci"         | _       | "test/build000000.000003"  | "1.1.0-branch.test.build.0.3.789"
        '1.0.0'       | _                   | 3        | "ci"         | _       | "test/build.000000.000003" | "1.1.0-branch.test.build.0.3.3"
        '1.0.0'       | _                   | 3        | "ci"         | _       | "release/1.00.x"           | "1.0.1-branch.release.1.0.x.3"

        '1.0.0'       | _                   | 10       | "ci"         | _       | "test/build01-"            | "1.1.0-branch.test.build.1.10"
        '1.0.0'       | _                   | 22       | "ci"         | _       | "test/build01+"            | "1.1.0-branch.test.build.1.22"
        '1.0.0'       | _                   | 45       | "ci"         | _       | "test/build01_"            | "1.1.0-branch.test.build.1.45"
        '1.0.0'       | _                   | 204      | "ci"         | _       | "test/build01"             | "1.1.0-branch.test.build.1.204"
        '1.0.0'       | _                   | 100      | "ci"         | _       | "test/build.01"            | "1.1.0-branch.test.build.1.100"
        '1.0.0'       | _                   | 55       | "ci"         | _       | "test/build002"            | "1.1.0-branch.test.build.2.55"
        '1.0.0'       | _                   | 66       | "ci"         | _       | "test/build.002"           | "1.1.0-branch.test.build.2.66"
        '1.0.0'       | _                   | 789      | "ci"         | _       | "test/build000000000003"   | "1.1.0-branch.test.build.3.789"
        '1.0.0'       | _                   | 777      | "ci"         | _       | "test/build.000000000003"  | "1.1.0-branch.test.build.3.777"
        '1.0.0'       | _                   | 789      | "ci"         | _       | "test/build000000.000003"  | "1.1.0-branch.test.build.0.3.789"
        '1.0.0'       | _                   | 3        | "ci"         | _       | "test/build.000000.000003" | "1.1.0-branch.test.build.0.3.3"
        '1.0.0'       | _                   | 3        | "ci"         | _       | "release/1.00.x"           | "1.0.1-branch.release.1.0.x.3"

        nearestAnyTitle = (nearestAny == _) ? "unset" : nearestAny
        scopeTitle = (scope == _) ? "unset" : scope
    }

}
