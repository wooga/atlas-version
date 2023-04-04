package wooga.gradle.version

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit
import spock.lang.Unroll
import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.strategies.NewSemverV2Strategies

class NewSemverV2StrategySpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.version'

    static final IVersionScheme semverV2Scheme = new IVersionScheme() {
        final SemVerStrategy development = NewSemverV2Strategies.DEVELOPMENT
        final SemVerStrategy snapshot = NewSemverV2Strategies.SNAPSHOT
        final SemVerStrategy preRelease = NewSemverV2Strategies.PRE_RELEASE
        final SemVerStrategy finalStrategy = NewSemverV2Strategies.FINAL
        final SemVerStrategy defaultStrategy = development
        final List<SemVerStrategy> strategies = [development, snapshot, preRelease, finalStrategy]
    }

    Grgit git

    def setup() {
        new File(projectDir, '.gitignore') << """
        userHome/
        """.stripIndent()

        git = Grgit.init(dir: projectDir)
        git.add(patterns: ['.gitignore'])
        git.commit(message: 'initial commit')
    }

    @Unroll('verify inferred new semver v1 version from nearestNormal: #nearestNormal, nearestAny: #nearestAnyTitle, scope: #scopeTitle, stage: #stage and branch: #branchName to be #expectedVersion')
    def "uses custom wooga application strategies for new semver v1"() {
        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
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
            git.tag.add(name: "$prefix$nearestNormal")
        }

        distance.times {
            git.commit(message: 'fix commit')
        }

        if (nearestAny != _) {
            git.tag.add(name: "$prefix$nearestAny")
            distance.times {
                git.commit(message: 'fix commit')
            }
        }

        if (expectedVersion?.contains("#TIMESTAMP")) {
            expectedVersion = expectedVersion.replace("#TIMESTAMP", Strategies.PreRelease.GenerateTimestamp())
        }
        if (expectedVersion?.contains("#COMMITHASH")) {
            expectedVersion = expectedVersion.replace("#COMMITHASH", git.head().abbreviatedId)
        }


        when:
        project.plugins.apply(PLUGIN_NAME)
        and:
        def versionExt = project.extensions.findByType(VersionPluginExtension).with {
            it.prefix = prefix
            return it
        }
        def version = versionExt.inferVersion(semverV2Scheme)

        then:
        version.map { it.version.toString() }.orNull == expectedVersion

        where:
        nearestNormal | nearestAny        | distance | stage        | scope   | branchName                 | expectedVersion
        _             | _                 | 2        | "ci"         | "major" | "master"                   | "1.0.0-master.3"
        _             | _                 | 3        | "ci"         | "minor" | "master"                   | "0.1.0-master.4"
        _             | _                 | 4        | "ci"         | "patch" | "master"                   | "0.0.1-master.5"

        '1.0.0'       | _                 | 2        | "ci"         | "major" | "master"                   | "2.0.0-master.2"
        '1.0.0'       | _                 | 3        | "ci"         | "minor" | "master"                   | "1.1.0-master.3"
        '1.0.0'       | _                 | 4        | "ci"         | "patch" | "master"                   | "1.0.1-master.4"

        '1.0.0'       | _                 | 2        | "ci"         | "major" | "develop"                  | "2.0.0-develop.2"
        '1.0.0'       | _                 | 3        | "ci"         | "minor" | "develop"                  | "1.1.0-develop.3"
        '1.0.0'       | _                 | 4        | "ci"         | "patch" | "develop"                  | "1.0.1-develop.4"

        '1.0.0'       | _                 | 2        | "ci"         | _       | "hotfix/check"             | "1.0.1-branch.hotfix.check.2"
        '1.0.0'       | _                 | 3        | "ci"         | _       | "fix/check"                | "1.1.0-branch.fix.check.3"
        '1.0.0'       | _                 | 1        | "ci"         | _       | "feature/check"            | "1.1.0-branch.feature.check.1"
        '1.0.0'       | _                 | 4        | "ci"         | _       | "feature-check"            | "1.1.0-branch.feature.check.4"
        '1.0.0'       | _                 | 5        | "ci"         | _       | "hotfix-check"             | "1.0.1-branch.hotfix.check.5"
        '1.0.0'       | _                 | 6        | "ci"         | _       | "fix-check"                | "1.1.0-branch.fix.check.6"
        '1.0.0'       | _                 | 7        | "ci"         | "patch" | "PR-22"                    | "1.0.1-branch.pr.22.7"

        '0.0.1'       | _                 | 1        | "ci"         | _       | "release/1.x"              | "1.0.0-branch.release.1.x.1"
        '1.0.0'       | _                 | 1        | "ci"         | "patch" | "release/1.x"              | "1.0.1-branch.release.1.x.1"
        '1.0.0'       | _                 | 2        | "ci"         | "patch" | "release-1.x"              | "1.0.1-branch.release.1.x.2"
        '1.0.0'       | _                 | 3        | "ci"         | _       | "release/1.1.x"            | "1.1.0-branch.release.1.1.x.3"
        '1.0.0'       | _                 | 3        | "ci"         | "patch" | "release/1.0.x"            | "1.0.1-branch.release.1.0.x.3"
        '1.0.0'       | _                 | 4        | "ci"         | _       | "release-1.1.x"            | "1.1.0-branch.release.1.1.x.4"
        '1.0.0'       | _                 | 4        | "ci"         | "patch" | "release-1.0.x"            | "1.0.1-branch.release.1.0.x.4"

        '0.0.1'       | _                 | 1        | "ci"         | _       | "1.x"                      | "1.0.0-branch.1.x.1"
        '1.0.0'       | _                 | 2        | "ci"         | "patch" | "1.x"                      | "1.0.1-branch.1.x.2"
        '1.0.0'       | _                 | 3        | "ci"         | _       | "1.1.x"                    | "1.1.0-branch.1.1.x.3"
        '1.0.0'       | _                 | 3        | "ci"         | "patch" | "1.0.x"                    | "1.0.1-branch.1.0.x.3"
        '1.0.0'       | '2.0.0-rc.2'      | 0        | "ci"         | _       | "release/2.x"              | "2.0.0-branch.release.2.x.0"

        _             | _                 | 2        | "ci"         | "major" | "master"                   | "1.0.0-master.3"
        _             | _                 | 3        | "ci"         | "minor" | "master"                   | "0.1.0-master.4"
        _             | _                 | 4        | "ci"         | "patch" | "master"                   | "0.0.1-master.5"

        '1.0.0'       | _                 | 2        | "ci"         | "major" | "master"                   | "2.0.0-master.2"
        '1.0.0'       | _                 | 3        | "ci"         | "minor" | "master"                   | "1.1.0-master.3"
        '1.0.0'       | _                 | 4        | "ci"         | "patch" | "master"                   | "1.0.1-master.4"

        '1.0.0'       | _                 | 2        | "snapshot"   | "major" | "develop"                  | "2.0.0-develop.2"
        '1.0.0'       | _                 | 3        | "snapshot"   | "minor" | "develop"                  | "1.1.0-develop.3"
        '1.0.0'       | _                 | 4        | "snapshot"   | "patch" | "develop"                  | "1.0.1-develop.4"


        '1.0.0'       | _                 | 2        | "snapshot"   | _       | "hotfix/check"             | "1.0.1-branch.hotfix.check.2"
        '1.0.0'       | _                 | 3        | "snapshot"   | _       | "fix/check"                | "1.1.0-branch.fix.check.3"
        '1.0.0'       | _                 | 1        | "snapshot"   | _       | "feature/check"            | "1.1.0-branch.feature.check.1"
        '1.0.0'       | _                 | 4        | "snapshot"   | _       | "feature-check"            | "1.1.0-branch.feature.check.4"
        '1.0.0'       | _                 | 5        | "snapshot"   | _       | "hotfix-check"             | "1.0.1-branch.hotfix.check.5"
        '1.0.0'       | _                 | 6        | "snapshot"   | _       | "fix-check"                | "1.1.0-branch.fix.check.6"
        '1.0.0'       | _                 | 7        | "snapshot"   | "patch" | "PR-22"                    | "1.0.1-branch.pr.22.7"

        '0.0.1'       | _                 | 1        | "snapshot"   | _       | "release/1.x"              | "1.0.0-branch.release.1.x.1"
        '1.0.0'       | _                 | 1        | "snapshot"   | "patch" | "release/1.x"              | "1.0.1-branch.release.1.x.1"
        '1.0.0'       | _                 | 2        | "snapshot"   | "patch" | "release-1.x"              | "1.0.1-branch.release.1.x.2"
        '1.0.0'       | _                 | 3        | "snapshot"   | _       | "release/1.1.x"            | "1.1.0-branch.release.1.1.x.3"
        '1.0.0'       | _                 | 3        | "snapshot"   | "patch" | "release/1.0.x"            | "1.0.1-branch.release.1.0.x.3"
        '1.0.0'       | _                 | 4        | "snapshot"   | _       | "release-1.1.x"            | "1.1.0-branch.release.1.1.x.4"
        '1.0.0'       | _                 | 4        | "snapshot"   | "patch" | "release-1.0.x"            | "1.0.1-branch.release.1.0.x.4"

        '0.0.1'       | _                 | 1        | "snapshot"   | _       | "1.x"                      | "1.0.0-branch.1.x.1"
        '1.0.0'       | _                 | 2        | "snapshot"   | "patch" | "1.x"                      | "1.0.1-branch.1.x.2"
        '1.0.0'       | _                 | 3        | "snapshot"   | _       | "1.1.x"                    | "1.1.0-branch.1.1.x.3"
        '1.0.0'       | _                 | 3        | "snapshot"   | "patch" | "1.0.x"                    | "1.0.1-branch.1.0.x.3"
        '1.0.0'       | '2.0.0-rc.2'      | 0        | "snapshot"   | _       | "release/2.x"              | "2.0.0-branch.release.2.x.0"

        '1.0.0'       | _                 | 2        | "staging"    | "major" | "develop"                  | "2.0.0-staging.1"
        '1.0.0'       | _                 | 3        | "staging"    | "minor" | "develop"                  | "1.1.0-staging.1"
        '1.0.0'       | _                 | 4        | "staging"    | "patch" | "develop"                  | "1.0.1-staging.1"

        '1.0.0'       | "1.0.1-staging.1" | 1        | "staging"    | "patch" | "develop"                  | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1" | 2        | "staging"    | "patch" | "develop"                  | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1" | 3        | "staging"    | "patch" | "develop"                  | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1" | 4        | "staging"    | "patch" | "develop"                  | "1.0.1-staging.2"

        '1.0.0'       | _                 | 2        | "staging"    | _       | "hotfix/check"             | "1.0.1-staging.1"
        '1.0.0'       | _                 | 1        | "staging"    | _       | "feature/check"            | "1.1.0-staging.1"
        '1.0.0'       | _                 | 3        | "staging"    | _       | "fix/check"                | "1.1.0-staging.1"
        '1.0.0'       | _                 | 5        | "staging"    | _       | "hotfix-check"             | "1.0.1-staging.1"
        '1.0.0'       | _                 | 4        | "staging"    | _       | "feature-check"            | "1.1.0-staging.1"
        '1.0.0'       | _                 | 6        | "staging"    | _       | "fix-check"                | "1.1.0-staging.1"
        '1.0.0'       | _                 | 7        | "staging"    | "patch" | "PR-22"                    | "1.0.1-staging.1"

        '1.0.0'       | "1.0.1-staging.1" | 2        | "staging"    | _       | "hotfix/check"             | "1.0.1-staging.2"
        '1.0.0'       | "1.1.0-staging.1" | 3        | "staging"    | _       | "feature/check"            | "1.1.0-staging.2"
        '1.0.0'       | "1.1.0-staging.1" | 3        | "staging"    | _       | "fix/check"                | "1.1.0-staging.2"

        '0.0.1'       | _                 | 1        | "staging"    | _       | "release/1.x"              | "1.0.0-staging.1"
        '1.0.0'       | _                 | 3        | "staging"    | _       | "release/1.1.x"            | "1.1.0-staging.1"
        '1.0.0'       | _                 | 4        | "staging"    | _       | "release-1.1.x"            | "1.1.0-staging.1"
        '1.0.0'       | _                 | 1        | "staging"    | "patch" | "release/1.x"              | "1.0.1-staging.1"
        '1.0.0'       | _                 | 2        | "staging"    | "patch" | "release-1.x"              | "1.0.1-staging.1"
        '1.0.0'       | _                 | 3        | "staging"    | "patch" | "release/1.0.x"            | "1.0.1-staging.1"
        '1.0.0'       | _                 | 4        | "staging"    | "patch" | "release-1.0.x"            | "1.0.1-staging.1"

        '0.0.1'       | "1.0.0-staging.1" | 1        | "staging"    | _       | "release/1.x"              | "1.0.0-staging.2"
        '1.0.0'       | "1.1.0-staging.1" | 3        | "staging"    | _       | "release/1.1.x"            | "1.1.0-staging.2"
        '1.0.0'       | '2.0.0-rc.2'      | 0        | "staging"    | _       | "release/2.x"              | "2.0.0-staging.1"

        '0.0.1'       | _                 | 1        | "staging"    | _       | "1.x"                      | "1.0.0-staging.1"
        '1.0.0'       | _                 | 3        | "staging"    | _       | "1.1.x"                    | "1.1.0-staging.1"
        '1.0.0'       | _                 | 2        | "staging"    | "patch" | "1.x"                      | "1.0.1-staging.1"
        '1.0.0'       | _                 | 3        | "staging"    | "patch" | "1.0.x"                    | "1.0.1-staging.1"

        '0.0.1'       | "1.0.0-staging.1" | 0        | "staging"    | _       | "1.x"                      | "1.0.0-staging.2"
        '1.0.0'       | "1.1.0-staging.1" | 3        | "staging"    | _       | "1.1.x"                    | "1.1.0-staging.2"

        '1.0.0'       | "1.0.1-staging.1" | 1        | "staging"    | _       | "release/1.x"              | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1" | 2        | "staging"    | _       | "release-1.x"              | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1" | 3        | "staging"    | _       | "release/1.0.x"            | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1" | 4        | "staging"    | _       | "release-1.0.x"            | "1.0.1-staging.2"

        '1.0.0'       | "1.0.1-staging.1" | 1        | "staging"    | _       | "1.x"                      | "1.0.1-staging.2"
        '1.0.0'       | "1.0.1-staging.1" | 3        | "staging"    | _       | "1.0.x"                    | "1.0.1-staging.2"

        '1.0.0'       | _                 | 2        | "rc"         | "major" | "master"                   | "2.0.0-rc.1"
        '1.0.0'       | _                 | 3        | "rc"         | "minor" | "master"                   | "1.1.0-rc.1"
        '1.0.0'       | _                 | 4        | "rc"         | "patch" | "master"                   | "1.0.1-rc.1"

        '1.0.0'       | _                 | 2        | "rc"         | _       | "hotfix/check"             | "1.0.1-rc.1"
        '1.0.0'       | _                 | 1        | "rc"         | _       | "feature/check"            | "1.1.0-rc.1"
        '1.0.0'       | _                 | 3        | "rc"         | _       | "fix/check"                | "1.1.0-rc.1"
        '1.0.0'       | _                 | 5        | "rc"         | _       | "hotfix-check"             | "1.0.1-rc.1"
        '1.0.0'       | _                 | 4        | "rc"         | _       | "feature-check"            | "1.1.0-rc.1"
        '1.0.0'       | _                 | 6        | "rc"         | _       | "fix-check"                | "1.1.0-rc.1"
        '1.0.0'       | _                 | 7        | "rc"         | "patch" | "PR-22"                    | "1.0.1-rc.1"

        '1.0.0'       | "1.0.1-rc.1"      | 2        | "rc"         | _       | "hotfix/check"             | "1.0.1-rc.2"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "rc"         | _       | "feature/check"            | "1.1.0-rc.2"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "rc"         | _       | "fix/check"                | "1.1.0-rc.2"

        '0.0.1'       | _                 | 1        | "rc"         | _       | "release/1.x"              | "1.0.0-rc.1"
        '1.0.0'       | _                 | 3        | "rc"         | _       | "release/1.1.x"            | "1.1.0-rc.1"
        '1.0.0'       | _                 | 4        | "rc"         | _       | "release-1.1.x"            | "1.1.0-rc.1"
        '1.0.0'       | _                 | 1        | "rc"         | "patch" | "release/1.x"              | "1.0.1-rc.1"
        '1.0.0'       | _                 | 2        | "rc"         | "patch" | "release-1.x"              | "1.0.1-rc.1"
        '1.0.0'       | _                 | 3        | "rc"         | "patch" | "release/1.0.x"            | "1.0.1-rc.1"
        '1.0.0'       | _                 | 4        | "rc"         | "patch" | "release-1.0.x"            | "1.0.1-rc.1"

        '0.0.1'       | "1.0.0-rc.1"      | 1        | "rc"         | _       | "release/1.x"              | "1.0.0-rc.2"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "rc"         | _       | "release/1.1.x"            | "1.1.0-rc.2"
        '1.0.0'       | '2.0.0-master.2'  | 0        | "rc"         | _       | "release/2.x"              | "2.0.0-rc.1"

        '0.0.1'       | _                 | 1        | "rc"         | _       | "1.x"                      | "1.0.0-rc.1"
        '1.0.0'       | _                 | 3        | "rc"         | _       | "1.1.x"                    | "1.1.0-rc.1"
        '1.0.0'       | _                 | 2        | "rc"         | "patch" | "1.x"                      | "1.0.1-rc.1"
        '1.0.0'       | _                 | 3        | "rc"         | "patch" | "1.0.x"                    | "1.0.1-rc.1"

        '0.0.1'       | "1.0.0-rc.1"      | 0        | "rc"         | _       | "1.x"                      | "1.0.0-rc.2"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "rc"         | _       | "1.1.x"                    | "1.1.0-rc.2"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "rc"         | _       | "release/1.x"              | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"      | 2        | "rc"         | _       | "release-1.x"              | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "rc"         | _       | "release/1.0.x"            | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"      | 4        | "rc"         | _       | "release-1.0.x"            | "1.0.1-rc.2"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "rc"         | _       | "1.x"                      | "1.0.1-rc.2"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "rc"         | _       | "1.0.x"                    | "1.0.1-rc.2"

        '1.0.0'       | _                 | 2        | "dev"        | "major" | "master"                   | "2.0.0-dev.2+#COMMITHASH"
        '1.0.0'       | _                 | 3        | "dev"        | "minor" | "master"                   | "1.1.0-dev.3+#COMMITHASH"
        '1.0.0'       | _                 | 4        | "dev"        | "patch" | "master"                   | "1.0.1-dev.4+#COMMITHASH"

        '0.0.1'       | _                 | 1        | "dev"        | _       | "release/1.x"              | "1.0.0-dev.1+#COMMITHASH"
        '1.0.0'       | _                 | 1        | "dev"        | "patch" | "release/1.x"              | "1.0.1-dev.1+#COMMITHASH"
        '1.0.0'       | _                 | 2        | "dev"        | "patch" | "release-1.x"              | "1.0.1-dev.2+#COMMITHASH"
        '1.0.0'       | _                 | 3        | "dev"        | _       | "release/1.1.x"            | "1.1.0-dev.3+#COMMITHASH"
        '1.0.0'       | _                 | 3        | "dev"        | "patch" | "release/1.0.x"            | "1.0.1-dev.3+#COMMITHASH"
        '1.0.0'       | _                 | 4        | "dev"        | _       | "release-1.1.x"            | "1.1.0-dev.4+#COMMITHASH"
        '1.0.0'       | _                 | 4        | "dev"        | "patch" | "release-1.0.x"            | "1.0.1-dev.4+#COMMITHASH"

        '0.0.1'       | _                 | 1        | "dev"        | _       | "1.x"                      | "1.0.0-dev.1+#COMMITHASH"
        '1.0.0'       | _                 | 2        | "dev"        | "patch" | "1.x"                      | "1.0.1-dev.2+#COMMITHASH"
        '1.0.0'       | _                 | 3        | "dev"        | _       | "1.1.x"                    | "1.1.0-dev.3+#COMMITHASH"
        '1.0.0'       | _                 | 3        | "dev"        | "patch" | "1.0.x"                    | "1.0.1-dev.3+#COMMITHASH"

        '1.0.0'       | _                 | 2        | "production" | "major" | "develop"                  | "2.0.0"
        '1.0.0'       | _                 | 3        | "production" | "minor" | "develop"                  | "1.1.0"
        '1.0.0'       | _                 | 4        | "production" | "patch" | "develop"                  | "1.0.1"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "production" | "patch" | "develop"                  | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 2        | "production" | "patch" | "develop"                  | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "production" | "patch" | "develop"                  | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 4        | "production" | "patch" | "develop"                  | "1.0.1"

        '1.0.0'       | _                 | 2        | "production" | _       | "hotfix/check"             | "1.0.1"
        '1.0.0'       | _                 | 1        | "production" | _       | "feature/check"            | "1.1.0"
        '1.0.0'       | _                 | 3        | "production" | _       | "fix/check"                | "1.1.0"
        '1.0.0'       | _                 | 5        | "production" | _       | "hotfix-check"             | "1.0.1"
        '1.0.0'       | _                 | 4        | "production" | _       | "feature-check"            | "1.1.0"
        '1.0.0'       | _                 | 6        | "production" | _       | "fix-check"                | "1.1.0"
        '1.0.0'       | _                 | 7        | "production" | "patch" | "PR-22"                    | "1.0.1"

        '1.0.0'       | "1.0.1-rc.1"      | 2        | "production" | _       | "hotfix/check"             | "1.0.1"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "production" | _       | "feature/check"            | "1.1.0"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "production" | _       | "fix/check"                | "1.1.0"

        '0.0.1'       | _                 | 1        | "production" | _       | "release/1.x"              | "1.0.0"
        '1.0.0'       | _                 | 3        | "production" | _       | "release/1.1.x"            | "1.1.0"
        '1.0.0'       | _                 | 4        | "production" | _       | "release-1.1.x"            | "1.1.0"
        '1.0.0'       | _                 | 1        | "production" | "patch" | "release/1.x"              | "1.0.1"
        '1.0.0'       | _                 | 2        | "production" | "patch" | "release-1.x"              | "1.0.1"
        '1.0.0'       | _                 | 3        | "production" | "patch" | "release/1.0.x"            | "1.0.1"
        '1.0.0'       | _                 | 4        | "production" | "patch" | "release-1.0.x"            | "1.0.1"

        '0.0.1'       | "1.0.0-rc.1"      | 1        | "production" | _       | "release/1.x"              | "1.0.0"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "production" | _       | "release/1.1.x"            | "1.1.0"
        '1.0.0'       | '2.0.0-master.2'  | 0        | "production" | _       | "release/2.x"              | "2.0.0"

        '0.0.1'       | _                 | 1        | "production" | _       | "1.x"                      | "1.0.0"
        '1.0.0'       | _                 | 3        | "production" | _       | "1.1.x"                    | "1.1.0"
        '1.0.0'       | _                 | 2        | "production" | "patch" | "1.x"                      | "1.0.1"
        '1.0.0'       | _                 | 3        | "production" | "patch" | "1.0.x"                    | "1.0.1"

        '0.0.1'       | "1.0.0-rc.1"      | 0        | "production" | _       | "1.x"                      | "1.0.0"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "production" | _       | "1.1.x"                    | "1.1.0"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "production" | _       | "release/1.x"              | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 2        | "production" | _       | "release-1.x"              | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "production" | _       | "release/1.0.x"            | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 4        | "production" | _       | "release-1.0.x"            | "1.0.1"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "production" | _       | "1.x"                      | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "production" | _       | "1.0.x"                    | "1.0.1"

        '1.0.0'       | _                 | 2        | "final"      | "major" | "develop"                  | "2.0.0"
        '1.0.0'       | _                 | 3        | "final"      | "minor" | "develop"                  | "1.1.0"
        '1.0.0'       | _                 | 4        | "final"      | "patch" | "develop"                  | "1.0.1"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "final"      | "patch" | "develop"                  | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 2        | "final"      | "patch" | "develop"                  | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "final"      | "patch" | "develop"                  | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 4        | "final"      | "patch" | "develop"                  | "1.0.1"

        '1.0.0'       | _                 | 2        | "final"      | _       | "hotfix/check"             | "1.0.1"
        '1.0.0'       | _                 | 1        | "final"      | _       | "feature/check"            | "1.1.0"
        '1.0.0'       | _                 | 3        | "final"      | _       | "fix/check"                | "1.1.0"
        '1.0.0'       | _                 | 5        | "final"      | _       | "hotfix-check"             | "1.0.1"
        '1.0.0'       | _                 | 4        | "final"      | _       | "feature-check"            | "1.1.0"
        '1.0.0'       | _                 | 6        | "final"      | _       | "fix-check"                | "1.1.0"
        '1.0.0'       | _                 | 7        | "final"      | "patch" | "PR-22"                    | "1.0.1"

        '1.0.0'       | "1.0.1-rc.1"      | 2        | "final"      | _       | "hotfix/check"             | "1.0.1"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "final"      | _       | "feature/check"            | "1.1.0"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "final"      | _       | "fix/check"                | "1.1.0"

        '0.0.1'       | _                 | 1        | "final"      | _       | "release/1.x"              | "1.0.0"
        '1.0.0'       | _                 | 3        | "final"      | _       | "release/1.1.x"            | "1.1.0"
        '1.0.0'       | _                 | 4        | "final"      | _       | "release-1.1.x"            | "1.1.0"
        '1.0.0'       | _                 | 1        | "final"      | "patch" | "release/1.x"              | "1.0.1"
        '1.0.0'       | _                 | 2        | "final"      | "patch" | "release-1.x"              | "1.0.1"
        '1.0.0'       | _                 | 3        | "final"      | "patch" | "release/1.0.x"            | "1.0.1"
        '1.0.0'       | _                 | 4        | "final"      | "patch" | "release-1.0.x"            | "1.0.1"

        '0.0.1'       | "1.0.0-rc.1"      | 1        | "final"      | _       | "release/1.x"              | "1.0.0"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "final"      | _       | "release/1.1.x"            | "1.1.0"
        '1.0.0'       | '2.0.0-master.2'  | 0        | "final"      | _       | "release/2.x"              | "2.0.0"

        '0.0.1'       | _                 | 1        | "final"      | _       | "1.x"                      | "1.0.0"
        '1.0.0'       | _                 | 3        | "final"      | _       | "1.1.x"                    | "1.1.0"
        '1.0.0'       | _                 | 2        | "final"      | "patch" | "1.x"                      | "1.0.1"
        '1.0.0'       | _                 | 3        | "final"      | "patch" | "1.0.x"                    | "1.0.1"

        '0.0.1'       | "1.0.0-rc.1"      | 0        | "final"      | _       | "1.x"                      | "1.0.0"
        '1.0.0'       | "1.1.0-rc.1"      | 3        | "final"      | _       | "1.1.x"                    | "1.1.0"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "final"      | _       | "release/1.x"              | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 2        | "final"      | _       | "release-1.x"              | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "final"      | _       | "release/1.0.x"            | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 4        | "final"      | _       | "release-1.0.x"            | "1.0.1"

        '1.0.0'       | "1.0.1-rc.1"      | 1        | "final"      | _       | "1.x"                      | "1.0.1"
        '1.0.0'       | "1.0.1-rc.1"      | 3        | "final"      | _       | "1.0.x"                    | "1.0.1"

        '1.0.0'       | _                 | 10       | "ci"         | "patch" | "test/build01-"            | "1.0.1-branch.test.build.1.10"
        '1.0.0'       | _                 | 22       | "ci"         | "patch" | "test/build01+"            | "1.0.1-branch.test.build.1.22"
        '1.0.0'       | _                 | 45       | "ci"         | "patch" | "test/build01_"            | "1.0.1-branch.test.build.1.45"
        '1.0.0'       | _                 | 204      | "ci"         | "patch" | "test/build01"             | "1.0.1-branch.test.build.1.204"
        '1.0.0'       | _                 | 100      | "ci"         | "patch" | "test/build.01"            | "1.0.1-branch.test.build.1.100"
        '1.0.0'       | _                 | 55       | "ci"         | "patch" | "test/build002"            | "1.0.1-branch.test.build.2.55"
        '1.0.0'       | _                 | 66       | "ci"         | "patch" | "test/build.002"           | "1.0.1-branch.test.build.2.66"
        '1.0.0'       | _                 | 789      | "ci"         | "patch" | "test/build000000000003"   | "1.0.1-branch.test.build.3.789"
        '1.0.0'       | _                 | 777      | "ci"         | "patch" | "test/build.000000000003"  | "1.0.1-branch.test.build.3.777"
        '1.0.0'       | _                 | 789      | "ci"         | "patch" | "test/build000000.000003"  | "1.0.1-branch.test.build.0.3.789"
        '1.0.0'       | _                 | 3        | "ci"         | "patch" | "test/build.000000.000003" | "1.0.1-branch.test.build.0.3.3"
        '1.0.0'       | _                 | 3        | "ci"         | "patch" | "release/1.00.x"           | "1.0.1-branch.release.1.0.x.3"

        '1.0.0'       | _                 | 10       | "ci"         | "patch" | "test/build01-"            | "1.0.1-branch.test.build.1.10"
        '1.0.0'       | _                 | 22       | "ci"         | "patch" | "test/build01+"            | "1.0.1-branch.test.build.1.22"
        '1.0.0'       | _                 | 45       | "ci"         | "patch" | "test/build01_"            | "1.0.1-branch.test.build.1.45"
        '1.0.0'       | _                 | 204      | "ci"         | "patch" | "test/build01"             | "1.0.1-branch.test.build.1.204"
        '1.0.0'       | _                 | 100      | "ci"         | "patch" | "test/build.01"            | "1.0.1-branch.test.build.1.100"
        '1.0.0'       | _                 | 55       | "ci"         | "patch" | "test/build002"            | "1.0.1-branch.test.build.2.55"
        '1.0.0'       | _                 | 66       | "ci"         | "patch" | "test/build.002"           | "1.0.1-branch.test.build.2.66"
        '1.0.0'       | _                 | 789      | "ci"         | "patch" | "test/build000000000003"   | "1.0.1-branch.test.build.3.789"
        '1.0.0'       | _                 | 777      | "ci"         | "patch" | "test/build.000000000003"  | "1.0.1-branch.test.build.3.777"
        '1.0.0'       | _                 | 789      | "ci"         | "patch" | "test/build000000.000003"  | "1.0.1-branch.test.build.0.3.789"
        '1.0.0'       | _                 | 3        | "ci"         | "patch" | "test/build.000000.000003" | "1.0.1-branch.test.build.0.3.3"
        '1.0.0'       | _                 | 3        | "ci"         | "patch" | "release/1.00.x"           | "1.0.1-branch.release.1.0.x.3"
        prefix = "prefix-"
        nearestAnyTitle = (nearestAny == _) ? "unset" : nearestAny
        scopeTitle = (scope == _) ? "unset" : scope
    }
}
