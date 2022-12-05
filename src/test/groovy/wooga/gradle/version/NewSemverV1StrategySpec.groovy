package wooga.gradle.version

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit
import spock.lang.Unroll
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.strategies.NewSemverV1Strategies

class NewSemverV1StrategySpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.version'
    static final IVersionScheme semverV1Scheme = new IVersionScheme() {
        final SemVerStrategy development = NewSemverV1Strategies.DEVELOPMENT
        final SemVerStrategy snapshot = NewSemverV1Strategies.SNAPSHOT
        final SemVerStrategy preRelease = NewSemverV1Strategies.PRE_RELEASE
        final SemVerStrategy finalStrategy = NewSemverV1Strategies.FINAL
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

        if (expectedVersion?.contains("#COMMITHASH")) {
            expectedVersion = expectedVersion.replace("#COMMITHASH", git.head().abbreviatedId)
        }

        when:
        project.plugins.apply(PLUGIN_NAME)
        and:
        def versionExt = project.extensions.findByType(VersionPluginExtension)
        def version = versionExt.inferVersion(semverV1Scheme)

        then:
        version.map { it.version.toString() }.orNull == expectedVersion

        where:
        nearestNormal | nearestAny          | distance | stage      | scope   | branchName      | expectedVersion
        _             | _                   | 2        | "snapshot" | "major" | "master"        | "1.0.0-master00003"
        _             | _                   | 3        | "snapshot" | "minor" | "master"        | "0.1.0-master00004"
        _             | _                   | 4        | "snapshot" | "patch" | "master"        | "0.0.1-master00005"

        '1.0.0'       | _                   | 2        | "snapshot" | "major" | "master"        | "2.0.0-master00002"
        '1.0.0'       | _                   | 3        | "snapshot" | "minor" | "master"        | "1.1.0-master00003"
        '1.0.0'       | _                   | 4        | "snapshot" | "patch" | "master"        | "1.0.1-master00004"

        '1.0.0'       | _                   | 2        | "snapshot" | "major" | "develop"       | "2.0.0-branchDevelop00002"
        '1.0.0'       | _                   | 3        | "snapshot" | "minor" | "develop"       | "1.1.0-branchDevelop00003"
        '1.0.0'       | _                   | 4        | "snapshot" | "patch" | "develop"       | "1.0.1-branchDevelop00004"

        '1.0.0'       | _                   | 7        | "snapshot" | "patch" | "PR-22"         | "1.0.1-branchPRTwoTwo00007"

        '0.0.1'       | _                   | 1        | "snapshot" | _       | "release/1.x"   | "1.0.0-branchReleaseOneDotx00001"
        '1.0.0'       | _                   | 1        | "snapshot" | "patch" | "release/1.x"   | "1.0.1-branchReleaseOneDotx00001"
        '1.0.0'       | _                   | 2        | "snapshot" | "patch" | "release-1.x"   | "1.0.1-branchReleaseOneDotx00002"
        '1.0.0'       | _                   | 3        | "snapshot" | _       | "release/1.1.x" | "1.1.0-branchReleaseOneDotOneDotx00003"
        '1.0.0'       | _                   | 3        | "snapshot" | "patch" | "release/1.0.x" | "1.0.1-branchReleaseOneDotZeroDotx00003"
        '1.0.0'       | _                   | 4        | "snapshot" | _       | "release-1.1.x" | "1.1.0-branchReleaseOneDotOneDotx00004"
        '1.0.0'       | _                   | 4        | "snapshot" | "patch" | "release-1.0.x" | "1.0.1-branchReleaseOneDotZeroDotx00004"
        '1.0.0'       | '2.0.0-rc.2'        | 0        | "snapshot" | _       | "release/2.x"   | "2.0.0-branchReleaseTwoDotx00000"

        '0.0.1'       | _                   | 1        | "snapshot" | _       | "1.x"           | "1.0.0-branchOneDotx00001"
        '1.0.0'       | _                   | 2        | "snapshot" | "patch" | "1.x"           | "1.0.1-branchOneDotx00002"
        '1.0.0'       | _                   | 3        | "snapshot" | _       | "1.1.x"         | "1.1.0-branchOneDotOneDotx00003"
        '1.0.0'       | _                   | 3        | "snapshot" | "patch" | "1.0.x"         | "1.0.1-branchOneDotZeroDotx00003"

        '1.0.0'       | _                   | 2        | "dev"      | "major" | "master"        | "2.0.0-dev.2+#COMMITHASH"
        '1.0.0'       | _                   | 3        | "dev"      | "minor" | "master"        | "1.1.0-dev.3+#COMMITHASH"
        '1.0.0'       | _                   | 4        | "dev"      | "patch" | "master"        | "1.0.1-dev.4+#COMMITHASH"

        '0.0.1'       | _                   | 1        | "dev"      | _       | "release/1.x"   | "1.0.0-dev.1+#COMMITHASH"
        '1.0.0'       | _                   | 1        | "dev"      | "patch" | "release/1.x"   | "1.0.1-dev.1+#COMMITHASH"
        '1.0.0'       | _                   | 2        | "dev"      | "patch" | "release-1.x"   | "1.0.1-dev.2+#COMMITHASH"
        '1.0.0'       | _                   | 3        | "dev"      | _       | "release/1.1.x" | "1.1.0-dev.3+#COMMITHASH"
        '1.0.0'       | _                   | 3        | "dev"      | "patch" | "release/1.0.x" | "1.0.1-dev.3+#COMMITHASH"
        '1.0.0'       | _                   | 4        | "dev"      | _       | "release-1.1.x" | "1.1.0-dev.4+#COMMITHASH"
        '1.0.0'       | _                   | 4        | "dev"      | "patch" | "release-1.0.x" | "1.0.1-dev.4+#COMMITHASH"

        '0.0.1'       | _                   | 1        | "dev"      | _       | "1.x"           | "1.0.0-dev.1+#COMMITHASH"
        '1.0.0'       | _                   | 2        | "dev"      | "patch" | "1.x"           | "1.0.1-dev.2+#COMMITHASH"
        '1.0.0'       | _                   | 3        | "dev"      | _       | "1.1.x"         | "1.1.0-dev.3+#COMMITHASH"
        '1.0.0'       | _                   | 3        | "dev"      | "patch" | "1.0.x"         | "1.0.1-dev.3+#COMMITHASH"

        '1.0.0'       | _                   | 2        | "rc"       | "major" | "master"        | "2.0.0-rc00001"
        '1.0.0'       | _                   | 3        | "rc"       | "minor" | "master"        | "1.1.0-rc00001"
        '1.0.0'       | _                   | 4        | "rc"       | "patch" | "master"        | "1.0.1-rc00001"

        '1.0.0'       | '1.1.0-rc00001'     | 1        | "rc"       | _       | "master"        | "1.1.0-rc00002"
        '1.0.0'       | '1.1.0-rc00002'     | 1        | "rc"       | _       | "master"        | "1.1.0-rc00003"

        '0.0.1'       | _                   | 1        | "rc"       | _       | "release/1.x"   | "1.0.0-rc00001"
        '1.0.0'       | _                   | 3        | "rc"       | _       | "release/1.1.x" | "1.1.0-rc00001"
        '1.0.0'       | _                   | 4        | "rc"       | _       | "release-1.1.x" | "1.1.0-rc00001"
        '1.0.0'       | _                   | 1        | "rc"       | "patch" | "release/1.x"   | "1.0.1-rc00001"
        '1.0.0'       | _                   | 2        | "rc"       | "patch" | "release-1.x"   | "1.0.1-rc00001"
        '1.0.0'       | _                   | 3        | "rc"       | "patch" | "release/1.0.x" | "1.0.1-rc00001"
        '1.0.0'       | _                   | 4        | "rc"       | "patch" | "release-1.0.x" | "1.0.1-rc00001"

        '0.0.1'       | "1.0.0-rc00001"     | 1        | "rc"       | _       | "release/1.x"   | "1.0.0-rc00002"
        '1.0.0'       | "1.1.0-rc00001"     | 3        | "rc"       | _       | "release/1.1.x" | "1.1.0-rc00002"
        '1.0.0'       | '2.0.0-master00002' | 0        | "rc"       | _       | "release/2.x"   | "2.0.0-rc00001"

        '0.0.1'       | _                   | 1        | "rc"       | _       | "1.x"           | "1.0.0-rc00001"
        '1.0.0'       | _                   | 3        | "rc"       | _       | "1.1.x"         | "1.1.0-rc00001"
        '1.0.0'       | _                   | 2        | "rc"       | "patch" | "1.x"           | "1.0.1-rc00001"
        '1.0.0'       | _                   | 3        | "rc"       | "patch" | "1.0.x"         | "1.0.1-rc00001"

        '0.0.1'       | "1.0.0-rc00001"     | 0        | "rc"       | _       | "1.x"           | "1.0.0-rc00002"
        '1.0.0'       | "1.1.0-rc00001"     | 3        | "rc"       | _       | "1.1.x"         | "1.1.0-rc00002"

        '1.0.0'       | _                   | 2        | "final"    | "major" | "master"        | "2.0.0"
        '1.0.0'       | _                   | 3        | "final"    | "minor" | "master"        | "1.1.0"
        '1.0.0'       | _                   | 4        | "final"    | "patch" | "master"        | "1.0.1"

        '0.0.1'       | _                   | 1        | "final"    | _       | "release/1.x"   | "1.0.0"
        '1.0.0'       | _                   | 1        | "final"    | "patch" | "release/1.x"   | "1.0.1"
        '1.0.0'       | _                   | 2        | "final"    | "patch" | "release-1.x"   | "1.0.1"
        '1.0.0'       | _                   | 3        | "final"    | _       | "release/1.1.x" | "1.1.0"
        '1.0.0'       | _                   | 3        | "final"    | "patch" | "release/1.0.x" | "1.0.1"
        '1.0.0'       | _                   | 4        | "final"    | _       | "release-1.1.x" | "1.1.0"
        '1.0.0'       | _                   | 4        | "final"    | "patch" | "release-1.0.x" | "1.0.1"

        '0.0.1'       | _                   | 1        | "final"    | _       | "1.x"           | "1.0.0"
        '1.0.0'       | _                   | 2        | "final"    | "patch" | "1.x"           | "1.0.1"
        '1.0.0'       | _                   | 3        | "final"    | _       | "1.1.x"         | "1.1.0"
        '1.0.0'       | _                   | 3        | "final"    | "patch" | "1.0.x"         | "1.0.1"


        nearestAnyTitle = (nearestAny == _) ? "unset" : nearestAny
        scopeTitle = (scope == _) ? "unset" : scope
    }
}
