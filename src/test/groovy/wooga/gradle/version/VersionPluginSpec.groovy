/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.version

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Grgit
import org.gradle.cache.internal.VersionStrategy
import spock.lang.Ignore
import spock.lang.Unroll
import wooga.gradle.version.internal.ToStringProvider

class VersionPluginSpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.version'

    Grgit git

    def setup() {
        new File(projectDir, '.gitignore') << """
        userHome/
        """.stripIndent()

        git = Grgit.init(dir: projectDir)
        git.add(patterns: ['.gitignore'])
        git.commit(message: 'initial commit')
        git.tag.add(name: 'v0.0.1')
    }

    @Ignore
    @Unroll("creates the task #taskName")
    def 'Creates needed tasks'(String taskName, Class taskType) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.tasks.findByName(taskName)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def task = project.tasks.findByName(taskName)
        taskType.isInstance(task)

        where:
        taskName  | taskType
        "example" | Version
    }

    @Unroll
    def 'Creates the [#extensionName] extension with type #extensionType'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.extensions.findByName(extensionName)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def extension = project.extensions.findByName(extensionName)
        extensionType.isInstance extension

        where:
        extensionName    | extensionType
        'versionBuilder' | VersionPluginExtension
    }

    @Unroll
    def 'Creates the [#extensionName] extension with type #extensionType when no git repo is in project'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.extensions.findByName(extensionName)

        and: "a project without git repo"
        git.close()
        def gitDir = new File(git.repository.getRootDir(), ".git")
        gitDir.deleteDir()

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def extension = project.extensions.findByName(extensionName)
        extensionType.isInstance extension

        where:
        extensionName    | extensionType
        'versionBuilder' | VersionPluginExtension
    }

    def findStrategyByName(List<VersionStrategy> strategies, name) {
        strategies.find { it.name == name }
    }

    @Unroll('verify wooga packages version strategy for #tagVersion, #scope and #stage')
    def "uses custom wooga semver strategies"() {

        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
        if (scope) {
            project.ext.set('release.scope', scope)
        }

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }

        commitsBefore.times {
            git.commit(message: 'feature commit')
        }

        git.tag.add(name: "v$tagVersion")

        commitsAfter.times {
            git.commit(message: 'fix commit')
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion

        where:

        tagVersion      | commitsBefore | commitsAfter | stage      | scope   | branchName      | expectedVersion
        '1.0.0'         | 1             | 3            | "SNAPSHOT" | "minor" | "master"        | "1.1.0-master00003"
        '1.0.0'         | 1             | 3            | "rc"       | "minor" | "master"        | "1.1.0-rc00001"
        '1.0.0'         | 1             | 3            | "final"    | "minor" | "master"        | "1.1.0"
        '1.0.0'         | 1             | 3            | "final"    | "major" | "master"        | "2.0.0"
        '1.0.0-rc00001' | 10            | 5            | "rc"       | "major" | "master"        | "1.0.0-rc00002"
        '1.0.0-rc00022' | 0             | 1            | "rc"       | "major" | "master"        | "1.0.0-rc00023"
        '0.1.0-rc00002' | 22            | 5            | "final"    | "minor" | "master"        | "0.1.0"
        '0.1.0'         | 3             | 5            | "SNAPSHOT" | "patch" | "master"        | "0.1.1-master00005"
        '1.1.0'         | 3             | 5            | "SNAPSHOT" | null    | "release/2.x"   | "2.0.0-branchReleaseTwoDotx00005"
        '2.0.1'         | 3             | 5            | "SNAPSHOT" | null    | "release/2.1.x" | "2.1.0-branchReleaseTwoDotOneDotx00005"
        '1.1.1'         | 3             | 5            | "rc"       | null    | "release/2.x"   | "2.0.0-rc00001"
        '2.0.1'         | 3             | 5            | "final"    | null    | "release/2.1.x" | "2.1.0"
        '1.1.0'         | 3             | 5            | "SNAPSHOT" | "minor" | "release/2.x"   | "1.2.0-branchReleaseTwoDotx00005"
        '2.0.1'         | 3             | 5            | "SNAPSHOT" | "patch" | "release/2.1.x" | "2.0.2-branchReleaseTwoDotOneDotx00005"
        '1.1.0'         | 3             | 5            | "SNAPSHOT" | null    | "2.x"           | "2.0.0-branchTwoDotx00005"
        '2.0.1'         | 3             | 5            | "SNAPSHOT" | null    | "2.1.x"         | "2.1.0-branchTwoDotOneDotx00005"
        '1.1.1'         | 3             | 5            | "rc"       | null    | "2.x"           | "2.0.0-rc00001"
        '2.0.1'         | 3             | 5            | "final"    | null    | "2.1.x"         | "2.1.0"
        '1.1.0'         | 3             | 5            | "SNAPSHOT" | "minor" | "2.x"           | "1.2.0-branchTwoDotx00005"
        '2.0.1'         | 3             | 5            | "SNAPSHOT" | "patch" | "2.1.x"         | "2.0.2-branchTwoDotOneDotx00005"
    }

    @Unroll('verify inferred custom semver 1.x version from nearestNormal: #nearestNormal, nearestAny: #nearestAnyTitle, scope: #scopeTitle, stage: #stage and branch: #branchName to be #expectedVersion')
    def "uses custom wooga application strategies v1"() {

        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
        if (scope != _) {
            project.ext.set('release.scope', scope)
        }

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }

        5.times {
            git.commit(message: 'feature commit')
        }

        git.tag.add(name: "v$nearestNormal")

        distance.times {
            git.commit(message: 'fix commit')
        }

        if (nearestAny != _) {
            git.tag.add(name: "v$nearestAny")
            distance.times {
                git.commit(message: 'fix commit')
            }
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion

        where:
        nearestAny      | distance | stage      | scope   | branchName      | expectedVersion
        _               | 1        | "snapshot" | _       | "master"        | "1.0.1-master00001"
        _               | 2        | "snapshot" | "major" | "master"        | "2.0.0-master00002"
        _               | 3        | "snapshot" | "minor" | "master"        | "1.1.0-master00003"
        _               | 4        | "snapshot" | "patch" | "master"        | "1.0.1-master00004"

        _               | 1        | "snapshot" | _       | "develop"       | "1.0.1-branchDevelop00001"
        _               | 2        | "snapshot" | "major" | "develop"       | "2.0.0-branchDevelop00002"
        _               | 3        | "snapshot" | "minor" | "develop"       | "1.1.0-branchDevelop00003"
        _               | 4        | "snapshot" | "patch" | "develop"       | "1.0.1-branchDevelop00004"

        _               | 1        | "snapshot" | _       | "feature/check" | "1.0.1-branchFeatureCheck00001"
        _               | 2        | "snapshot" | _       | "hotfix/check"  | "1.0.1-branchHotfixCheck00002"
        _               | 3        | "snapshot" | _       | "fix/check"     | "1.0.1-branchFixCheck00003"
        _               | 4        | "snapshot" | _       | "feature-check" | "1.0.1-branchFeatureCheck00004"
        _               | 5        | "snapshot" | _       | "hotfix-check"  | "1.0.1-branchHotfixCheck00005"
        _               | 6        | "snapshot" | _       | "fix-check"     | "1.0.1-branchFixCheck00006"
        _               | 7        | "snapshot" | _       | "PR-22"         | "1.0.1-branchPRTwoTwo00007"

        _               | 1        | "snapshot" | _       | "release/1.x"   | "1.0.1-branchReleaseOneDotx00001"
        _               | 2        | "snapshot" | _       | "release-1.x"   | "1.0.1-branchReleaseOneDotx00002"
        _               | 3        | "snapshot" | _       | "release/1.0.x" | "1.0.1-branchReleaseOneDotZeroDotx00003"
        _               | 4        | "snapshot" | _       | "release-1.0.x" | "1.0.1-branchReleaseOneDotZeroDotx00004"

        _               | 2        | "snapshot" | _       | "1.x"           | "1.0.1-branchOneDotx00002"
        _               | 4        | "snapshot" | _       | "1.0.x"         | "1.0.1-branchOneDotZeroDotx00004"

        _               | 1        | "rc"       | _       | "master"        | "1.0.1-rc00001"
        _               | 2        | "rc"       | "major" | "master"        | "2.0.0-rc00001"
        _               | 3        | "rc"       | "minor" | "master"        | "1.1.0-rc00001"
        _               | 4        | "rc"       | "patch" | "master"        | "1.0.1-rc00001"

        '1.1.0-rc00001' | 1        | "rc"       | _       | "master"        | "1.1.0-rc00002"
        '1.1.0-rc00002' | 1        | "rc"       | _       | "master"        | "1.1.0-rc00003"

        _               | 1        | "rc"       | _       | "release/1.x"   | "1.0.1-rc00001"
        _               | 2        | "rc"       | _       | "release-1.x"   | "1.0.1-rc00001"
        _               | 3        | "rc"       | _       | "release/1.0.x" | "1.0.1-rc00001"
        _               | 4        | "rc"       | _       | "release-1.0.x" | "1.0.1-rc00001"

        _               | 1        | "rc"       | _       | "1.x"           | "1.0.1-rc00001"
        _               | 3        | "rc"       | _       | "1.0.x"         | "1.0.1-rc00001"

        "1.1.0-rc00001" | 1        | "rc"       | _       | "release/1.x"   | "1.1.0-rc00002"
        "1.1.0-rc00001" | 2        | "rc"       | _       | "release-1.x"   | "1.1.0-rc00002"
        "1.0.1-rc00001" | 3        | "rc"       | _       | "release/1.0.x" | "1.0.1-rc00002"
        "1.0.1-rc00001" | 4        | "rc"       | _       | "release-1.0.x" | "1.0.1-rc00002"

        "1.1.0-rc00001" | 1        | "rc"       | _       | "1.x"           | "1.1.0-rc00002"
        "1.0.1-rc00001" | 3        | "rc"       | _       | "1.0.x"         | "1.0.1-rc00002"

        _               | 1        | "final"    | _       | "master"        | "1.0.1"
        _               | 2        | "final"    | "major" | "master"        | "2.0.0"
        _               | 3        | "final"    | "minor" | "master"        | "1.1.0"
        _               | 4        | "final"    | "patch" | "master"        | "1.0.1"

        _               | 1        | "final"    | _       | "release/1.x"   | "1.0.1"
        _               | 1        | "final"    | _       | "release/1.x"   | "1.0.1"
        _               | 2        | "final"    | _       | "release-1.x"   | "1.0.1"
        _               | 3        | "final"    | _       | "release/1.0.x" | "1.0.1"
        _               | 4        | "final"    | _       | "release-1.0.x" | "1.0.1"

        _               | 1        | "final"    | _       | "1.x"           | "1.0.1"
        _               | 3        | "final"    | _       | "1.0.x"         | "1.0.1"

        nearestNormal = '1.0.0'
        nearestAnyTitle = (nearestAny == _) ? "unset" : nearestAny
        scopeTitle = (scope == _) ? "unset" : scope
    }

    @Unroll('verify inferred custom semver versioncode with scheme #versionCodeScheme from nearestNormal: #nearestNormal, nearestAny: #nearestAnyTitle, scope: #scopeTitle, stage: #stage and branch: #branchName to be #expectedVersion')
    def "uses custom wooga versioncode strategy semver"() {

        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
        project.ext.set('version.scheme', 'semver2')
        project.ext.set('versionBuilder.versionCodeScheme', versionCodeScheme)
        if (scope != _) {
            project.ext.set('release.scope', scope)
        }

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }

        5.times {
            git.commit(message: 'feature commit')
        }

        git.tag.add(name: "v$nearestNormal")

        distance.times {
            git.commit(message: 'fix commit')
        }

        if (nearestAny != _) {
            git.tag.add(name: "v$nearestAny")
            distance.times {
                git.commit(message: 'fix commit')
            }
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion
        def versionBuilder = project.extensions.getByType(VersionPluginExtension)
        versionBuilder.versionCode.get() == expectedVersionCode
        project.versionCode.toString() == expectedVersionCode.toString()

        where:
        nearestAny        | distance | stage      | scope   | branchName | versionCodeScheme             | expectedVersion   | expectedVersionCode
        _                 | 1        | "snapshot" | _       | "master"   | VersionCodeScheme.semver      | "1.1.0-master.1"  | 101000
        _                 | 2        | "snapshot" | "major" | "master"   | VersionCodeScheme.semver      | "2.0.0-master.2"  | 200000
        _                 | 3        | "snapshot" | "minor" | "master"   | VersionCodeScheme.semver      | "1.1.0-master.3"  | 101000
        _                 | 4        | "snapshot" | "patch" | "master"   | VersionCodeScheme.semver      | "1.0.1-master.4"  | 100010
        _                 | 1        | "snapshot" | _       | "develop"  | VersionCodeScheme.semver      | "1.1.0-develop.1" | 101000
        _                 | 2        | "snapshot" | "major" | "develop"  | VersionCodeScheme.semver      | "2.0.0-develop.2" | 200000
        _                 | 3        | "snapshot" | "minor" | "develop"  | VersionCodeScheme.semver      | "1.1.0-develop.3" | 101000
        _                 | 4        | "snapshot" | "patch" | "develop"  | VersionCodeScheme.semver      | "1.0.1-develop.4" | 100010
        _                 | 1        | "rc"       | _       | "master"   | VersionCodeScheme.semver      | "1.1.0-rc.1"      | 101000
        _                 | 2        | "rc"       | "major" | "master"   | VersionCodeScheme.semver      | "2.0.0-rc.1"      | 200000
        _                 | 3        | "rc"       | "minor" | "master"   | VersionCodeScheme.semver      | "1.1.0-rc.1"      | 101000
        _                 | 4        | "rc"       | "patch" | "master"   | VersionCodeScheme.semver      | "1.0.1-rc.1"      | 100010
        '1.1.0-staging.1' | 1        | "staging"  | _       | "master"   | VersionCodeScheme.semver      | "1.1.0-staging.2" | 101002
        '1.1.0-staging.2' | 1        | "staging"  | _       | "master"   | VersionCodeScheme.semver      | "1.1.0-staging.3" | 101003

        _                 | 1        | "snapshot" | _       | "master"   | VersionCodeScheme.semverBasic | "1.1.0-master.1"  | 10100
        _                 | 2        | "snapshot" | "major" | "master"   | VersionCodeScheme.semverBasic | "2.0.0-master.2"  | 20000
        _                 | 3        | "snapshot" | "minor" | "master"   | VersionCodeScheme.semverBasic | "1.1.0-master.3"  | 10100
        _                 | 4        | "snapshot" | "patch" | "master"   | VersionCodeScheme.semverBasic | "1.0.1-master.4"  | 10001
        _                 | 1        | "snapshot" | _       | "develop"  | VersionCodeScheme.semverBasic | "1.1.0-develop.1" | 10100
        _                 | 2        | "snapshot" | "major" | "develop"  | VersionCodeScheme.semverBasic | "2.0.0-develop.2" | 20000
        _                 | 3        | "snapshot" | "minor" | "develop"  | VersionCodeScheme.semverBasic | "1.1.0-develop.3" | 10100
        _                 | 4        | "snapshot" | "patch" | "develop"  | VersionCodeScheme.semverBasic | "1.0.1-develop.4" | 10001
        _                 | 1        | "rc"       | _       | "master"   | VersionCodeScheme.semverBasic | "1.1.0-rc.1"      | 10100
        _                 | 2        | "rc"       | "major" | "master"   | VersionCodeScheme.semverBasic | "2.0.0-rc.1"      | 20000
        _                 | 3        | "rc"       | "minor" | "master"   | VersionCodeScheme.semverBasic | "1.1.0-rc.1"      | 10100
        _                 | 4        | "rc"       | "patch" | "master"   | VersionCodeScheme.semverBasic | "1.0.1-rc.1"      | 10001
        '1.1.0-staging.1' | 1        | "staging"  | _       | "master"   | VersionCodeScheme.semverBasic | "1.1.0-staging.2" | 10100
        '1.1.0-staging.2' | 1        | "staging"  | _       | "master"   | VersionCodeScheme.semverBasic | "1.1.0-staging.3" | 10100

        nearestNormal = '1.0.0'
        nearestAnyTitle = (nearestAny == _) ? "unset" : nearestAny
        scopeTitle = (scope == _) ? "unset" : scope
    }

    @Unroll("verify inferred custom semver versioncode #expectedVersionCode from scheme #scheme with #numberOfFinalReleases release tags, #numberOfPrereleases pre release tags and offset #offset")
    def "uses custom wooga versioncode strategy"() {
        given: "a project with specified versionCodeScheme"
        if (scheme != _) {
            project.ext.set('versionBuilder.versionCodeScheme', scheme)
        }

        project.ext.set('versionBuilder.versionCodeOffset', offset)

        git.commit(message: 'initial commit')

        and: "correct number of tags since the repo contains one tag already"
        numberOfFinalReleases -= 1

        numberOfFinalReleases.times {
            git.commit(message: 'a commit')
            if (it < numberOfPrereleases) {
                git.tag.add(name: "${it}.0.0-rc.1")
                git.tag.add(name: "v${it}.0.0-rc.1")
            }
            git.tag.add(name: "v${it}.0.0")
        }

        Math.max(numberOfPrereleases - numberOfFinalReleases, 0).times {
            git.commit(message: 'a commit')
            git.tag.add(name: "v${numberOfFinalReleases + 1}.0.0-rc.${it}")
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def versionBuilder = project.extensions.getByType(VersionPluginExtension)
        versionBuilder.versionCode.get() == expectedVersionCode
        project.versionCode.toString() == expectedVersionCode.toString()

        where:
        numberOfFinalReleases | numberOfPrereleases | offset | scheme                              | expectedVersionCode
        5                     | 5                   | 0      | VersionCodeScheme.releaseCountBasic | 6
        5                     | 5                   | 3      | VersionCodeScheme.releaseCountBasic | 9
        10                    | 2                   | 0      | VersionCodeScheme.releaseCount      | 13
        7                     | 0                   | 5      | VersionCodeScheme.releaseCount      | 13
        3                     | 8                   | 5      | VersionCodeScheme.none              | 0
        3                     | 8                   | 5      | _                                   | 0
    }


    def "returns initial version '#expectedVersion' when no git repo is found"() {
        given: "a project without git repo"
        git.close()
        def gitDir = new File(git.repository.getRootDir(), ".git")
        gitDir.deleteDir()

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion

        where:
        expectedVersion = '0.1.0-dev.0.uninitialized'
    }

    @Unroll('verify inferred semver 2.x version from nearestNormal: #nearestNormal, nearestAny: #nearestAnyTitle, scope: #scopeTitle, stage: #stage and branch: #branchName to be #expectedVersion')
    def "uses custom wooga application strategies v2"() {
        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
        project.ext.set('version.scheme', 'semver2')
        if (scope != _) {
            project.ext.set('release.scope', scope)
        }

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }

        5.times {
            git.commit(message: 'feature commit')
        }

        git.tag.add(name: "v$nearestNormal")

        distance.times {
            git.commit(message: 'fix commit')
        }

        if (nearestAny != _) {
            git.tag.add(name: "v$nearestAny")
            distance.times {
                git.commit(message: 'fix commit')
            }
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion

        where:
        nearestAny        | distance | stage        | scope   | branchName                 | expectedVersion
        _                 | 1        | "ci"         | _       | "master"                   | "1.1.0-master.1"
        _                 | 2        | "ci"         | "major" | "master"                   | "2.0.0-master.2"
        _                 | 3        | "ci"         | "minor" | "master"                   | "1.1.0-master.3"
        _                 | 4        | "ci"         | "patch" | "master"                   | "1.0.1-master.4"

        _                 | 1        | "ci"         | _       | "develop"                  | "1.1.0-develop.1"
        _                 | 2        | "ci"         | "major" | "develop"                  | "2.0.0-develop.2"
        _                 | 3        | "ci"         | "minor" | "develop"                  | "1.1.0-develop.3"
        _                 | 4        | "ci"         | "patch" | "develop"                  | "1.0.1-develop.4"

        _                 | 1        | "ci"         | _       | "feature/check"            | "1.1.0-branch.feature.check.1"
        _                 | 2        | "ci"         | _       | "hotfix/check"             | "1.0.1-branch.hotfix.check.2"
        _                 | 3        | "ci"         | _       | "fix/check"                | "1.1.0-branch.fix.check.3"
        _                 | 4        | "ci"         | _       | "feature-check"            | "1.1.0-branch.feature.check.4"
        _                 | 5        | "ci"         | _       | "hotfix-check"             | "1.0.1-branch.hotfix.check.5"
        _                 | 6        | "ci"         | _       | "fix-check"                | "1.1.0-branch.fix.check.6"
        _                 | 7        | "ci"         | _       | "PR-22"                    | "1.1.0-branch.pr.22.7"

        _                 | 1        | "ci"         | _       | "release/1.x"              | "1.1.0-branch.release.1.x.1"
        _                 | 2        | "ci"         | _       | "release-1.x"              | "1.1.0-branch.release.1.x.2"
        _                 | 3        | "ci"         | _       | "release/1.0.x"            | "1.0.1-branch.release.1.0.x.3"
        _                 | 4        | "ci"         | _       | "release-1.0.x"            | "1.0.1-branch.release.1.0.x.4"

        _                 | 2        | "ci"         | _       | "1.x"                      | "1.1.0-branch.1.x.2"
        _                 | 4        | "ci"         | _       | "1.0.x"                    | "1.0.1-branch.1.0.x.4"
        '2.0.0-rc.2'      | 0        | "ci"         | _       | "release/2.x"              | "2.0.0-branch.release.2.x.0"

        _                 | 1        | "snapshot"   | _       | "master"                   | "1.1.0-master.1"
        _                 | 2        | "snapshot"   | "major" | "master"                   | "2.0.0-master.2"
        _                 | 3        | "snapshot"   | "minor" | "master"                   | "1.1.0-master.3"
        _                 | 4        | "snapshot"   | "patch" | "master"                   | "1.0.1-master.4"

        _                 | 1        | "snapshot"   | _       | "develop"                  | "1.1.0-develop.1"
        _                 | 2        | "snapshot"   | "major" | "develop"                  | "2.0.0-develop.2"
        _                 | 3        | "snapshot"   | "minor" | "develop"                  | "1.1.0-develop.3"
        _                 | 4        | "snapshot"   | "patch" | "develop"                  | "1.0.1-develop.4"

        _                 | 1        | "snapshot"   | _       | "feature/check"            | "1.1.0-branch.feature.check.1"
        _                 | 2        | "snapshot"   | _       | "hotfix/check"             | "1.0.1-branch.hotfix.check.2"
        _                 | 3        | "snapshot"   | _       | "fix/check"                | "1.1.0-branch.fix.check.3"
        _                 | 4        | "snapshot"   | _       | "feature-check"            | "1.1.0-branch.feature.check.4"
        _                 | 5        | "snapshot"   | _       | "hotfix-check"             | "1.0.1-branch.hotfix.check.5"
        _                 | 6        | "snapshot"   | _       | "fix-check"                | "1.1.0-branch.fix.check.6"
        _                 | 7        | "snapshot"   | _       | "PR-22"                    | "1.1.0-branch.pr.22.7"

        _                 | 1        | "snapshot"   | _       | "release/1.x"              | "1.1.0-branch.release.1.x.1"
        _                 | 2        | "snapshot"   | _       | "release-1.x"              | "1.1.0-branch.release.1.x.2"
        _                 | 3        | "snapshot"   | _       | "release/1.0.x"            | "1.0.1-branch.release.1.0.x.3"
        _                 | 4        | "snapshot"   | _       | "release-1.0.x"            | "1.0.1-branch.release.1.0.x.4"

        _                 | 2        | "snapshot"   | _       | "1.x"                      | "1.1.0-branch.1.x.2"
        _                 | 4        | "snapshot"   | _       | "1.0.x"                    | "1.0.1-branch.1.0.x.4"

        _                 | 1        | "staging"    | _       | "master"                   | "1.1.0-staging.1"
        _                 | 2        | "staging"    | "major" | "master"                   | "2.0.0-staging.1"
        _                 | 3        | "staging"    | "minor" | "master"                   | "1.1.0-staging.1"
        _                 | 4        | "staging"    | "patch" | "master"                   | "1.0.1-staging.1"

        '1.1.0-staging.1' | 1        | "staging"    | _       | "master"                   | "1.1.0-staging.2"
        '1.1.0-staging.2' | 1        | "staging"    | _       | "master"                   | "1.1.0-staging.3"

        _                 | 1        | "staging"    | _       | "release/1.x"              | "1.1.0-staging.1"
        _                 | 2        | "staging"    | _       | "release-1.x"              | "1.1.0-staging.1"
        _                 | 3        | "staging"    | _       | "release/1.0.x"            | "1.0.1-staging.1"
        _                 | 4        | "staging"    | _       | "release-1.0.x"            | "1.0.1-staging.1"

        _                 | 1        | "staging"    | _       | "1.x"                      | "1.1.0-staging.1"
        _                 | 3        | "staging"    | _       | "1.0.x"                    | "1.0.1-staging.1"

        "1.1.0-staging.1" | 1        | "staging"    | _       | "release/1.x"              | "1.1.0-staging.2"
        "1.1.0-staging.1" | 2        | "staging"    | _       | "release-1.x"              | "1.1.0-staging.2"
        "1.0.1-staging.1" | 3        | "staging"    | _       | "release/1.0.x"            | "1.0.1-staging.2"
        "1.0.1-staging.1" | 4        | "staging"    | _       | "release-1.0.x"            | "1.0.1-staging.2"

        "1.1.0-staging.1" | 1        | "staging"    | _       | "1.x"                      | "1.1.0-staging.2"
        "1.0.1-staging.1" | 3        | "staging"    | _       | "1.0.x"                    | "1.0.1-staging.2"

        _                 | 1        | "rc"         | _       | "master"                   | "1.1.0-rc.1"
        _                 | 2        | "rc"         | "major" | "master"                   | "2.0.0-rc.1"
        _                 | 3        | "rc"         | "minor" | "master"                   | "1.1.0-rc.1"
        _                 | 4        | "rc"         | "patch" | "master"                   | "1.0.1-rc.1"

        '1.1.0-rc.1'      | 1        | "rc"         | _       | "master"                   | "1.1.0-rc.2"
        '1.1.0-rc.2'      | 1        | "rc"         | _       | "master"                   | "1.1.0-rc.3"

        _                 | 1        | "rc"         | _       | "release/1.x"              | "1.1.0-rc.1"
        _                 | 2        | "rc"         | _       | "release-1.x"              | "1.1.0-rc.1"
        _                 | 3        | "rc"         | _       | "release/1.0.x"            | "1.0.1-rc.1"
        _                 | 4        | "rc"         | _       | "release-1.0.x"            | "1.0.1-rc.1"

        _                 | 1        | "rc"         | _       | "1.x"                      | "1.1.0-rc.1"
        _                 | 3        | "rc"         | _       | "1.0.x"                    | "1.0.1-rc.1"

        "1.1.0-rc.1"      | 1        | "rc"         | _       | "release/1.x"              | "1.1.0-rc.2"
        "1.1.0-rc.1"      | 2        | "rc"         | _       | "release-1.x"              | "1.1.0-rc.2"
        "1.0.1-rc.1"      | 3        | "rc"         | _       | "release/1.0.x"            | "1.0.1-rc.2"
        "1.0.1-rc.1"      | 4        | "rc"         | _       | "release-1.0.x"            | "1.0.1-rc.2"

        "1.1.0-rc.1"      | 1        | "rc"         | _       | "1.x"                      | "1.1.0-rc.2"
        "1.0.1-rc.1"      | 3        | "rc"         | _       | "1.0.x"                    | "1.0.1-rc.2"

        "1.1.0-rc.1"      | 1        | "staging"    | _       | "1.x"                      | "1.1.0-staging.1"
        "1.0.1-rc.1"      | 3        | "staging"    | _       | "1.0.x"                    | "1.0.1-staging.1"

        _                 | 1        | "production" | _       | "master"                   | "1.1.0"
        _                 | 2        | "production" | "major" | "master"                   | "2.0.0"
        _                 | 3        | "production" | "minor" | "master"                   | "1.1.0"
        _                 | 4        | "production" | "patch" | "master"                   | "1.0.1"

        _                 | 1        | "production" | _       | "release/1.x"              | "1.1.0"
        _                 | 1        | "production" | _       | "release/1.x"              | "1.1.0"
        _                 | 2        | "production" | _       | "release-1.x"              | "1.1.0"
        _                 | 3        | "production" | _       | "release/1.0.x"            | "1.0.1"
        _                 | 4        | "production" | _       | "release-1.0.x"            | "1.0.1"

        _                 | 1        | "production" | _       | "1.x"                      | "1.1.0"
        _                 | 3        | "production" | _       | "1.0.x"                    | "1.0.1"

        _                 | 1        | "final"      | _       | "master"                   | "1.1.0"
        _                 | 2        | "final"      | "major" | "master"                   | "2.0.0"
        _                 | 3        | "final"      | "minor" | "master"                   | "1.1.0"
        _                 | 4        | "final"      | "patch" | "master"                   | "1.0.1"

        _                 | 1        | "final"      | _       | "release/1.x"              | "1.1.0"
        _                 | 1        | "final"      | _       | "release/1.x"              | "1.1.0"
        _                 | 2        | "final"      | _       | "release-1.x"              | "1.1.0"
        _                 | 3        | "final"      | _       | "release/1.0.x"            | "1.0.1"
        _                 | 4        | "final"      | _       | "release-1.0.x"            | "1.0.1"

        _                 | 1        | "final"      | _       | "1.x"                      | "1.1.0"
        _                 | 3        | "final"      | _       | "1.0.x"                    | "1.0.1"

        _                 | 1        | "final"      | _       | "1.x"                      | "1.1.0"
        _                 | 3        | "final"      | _       | "1.0.x"                    | "1.0.1"
        _                 | 10       | "ci"         | _       | "test/build01-"            | "1.1.0-branch.test.build.1.10"
        _                 | 22       | "ci"         | _       | "test/build01+"            | "1.1.0-branch.test.build.1.22"
        _                 | 45       | "ci"         | _       | "test/build01_"            | "1.1.0-branch.test.build.1.45"
        _                 | 204      | "ci"         | _       | "test/build01"             | "1.1.0-branch.test.build.1.204"
        _                 | 100      | "ci"         | _       | "test/build.01"            | "1.1.0-branch.test.build.1.100"
        _                 | 55       | "ci"         | _       | "test/build002"            | "1.1.0-branch.test.build.2.55"
        _                 | 66       | "ci"         | _       | "test/build.002"           | "1.1.0-branch.test.build.2.66"
        _                 | 789      | "ci"         | _       | "test/build000000000003"   | "1.1.0-branch.test.build.3.789"
        _                 | 777      | "ci"         | _       | "test/build.000000000003"  | "1.1.0-branch.test.build.3.777"
        _                 | 789      | "ci"         | _       | "test/build000000.000003"  | "1.1.0-branch.test.build.0.3.789"
        _                 | 3        | "ci"         | _       | "test/build.000000.000003" | "1.1.0-branch.test.build.0.3.3"
        _                 | 3        | "ci"         | _       | "release/1.00.x"           | "1.0.1-branch.release.1.0.x.3"

        nearestNormal = '1.0.0'
        nearestAnyTitle = (nearestAny == _) ? "unset" : nearestAny
        scopeTitle = (scope == _) ? "unset" : scope
    }

    @Unroll('verify inferred semver 2.x version from productionMarker: #productionMarkerTitle, ciMarker: #ciMarkerTitle, stage: #stage and branch: #branchName to be #expectedVersion')
    def "uses custom wooga application strategies static marker"() {
        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
        project.ext.set('version.scheme', 'staticMarker')

        if (releaseBranchPattern != _) {
            project.ext.set('versionBuilder.releaseBranchPattern', releaseBranchPattern)
        }

        if (mainBranchPattern != _) {
            project.ext.set('versionBuilder.mainBranchPattern', mainBranchPattern)
        }

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }

        5.times {
            git.commit(message: 'feature commit')
        }

        if (ciMarker != _) {
            git.tag.add(name: "ci-${ciMarker}")
        }

        if (productionMarker != _) {
            git.tag.add(name: "release-${productionMarker}")
        }

        (distance - distanceNearestAny).times {
            git.commit(message: 'fix commit')
        }

        if (nearestAny != _) {
            git.tag.add(name: "v${nearestAny}")
        }

        distanceNearestAny.times {
            git.commit(message: 'fix commit')
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion


        where:
        nearestAny        | ciMarker | productionMarker | distance | distanceNearestAny | stage        | branchName                 | releaseBranchPattern         | mainBranchPattern       | expectedVersion
        _                 | "0.2.0"  | "0.1.0"          | 1        | 0                  | "ci"         | "develop"                  | _                            | _                       | "0.2.0-develop.1"
        _                 | "0.2.0"  | "0.1.0"          | 2        | 0                  | "ci"         | "develop"                  | _                            | /^master$/              | "0.2.0-branch.develop.2"
        _                 | "0.2.0"  | "0.1.0"          | 3        | 0                  | "ci"         | "master"                   | _                            | _                       | "0.1.0-master.3"
        _                 | "0.2.0"  | "0.1.0"          | 4        | 0                  | "ci"         | "master"                   | /^(release\/.*|production)$/ | _                       | "0.2.0-master.4"
        _                 | "0.2.0"  | "0.1.0"          | 5        | 0                  | "ci"         | "production"               | /^(release\/.*|production)$/ | /^(master|production)$/ | "0.1.0-production.5"
        _                 | "0.2.0"  | "0.1.0"          | 6        | 0                  | "ci"         | "feature/test"             | _                            | _                       | "0.2.0-branch.feature.test.6"
        "0.2.0"           | "0.3.0"  | "0.2.0"          | 7        | 3                  | "ci"         | "master"                   | _                            | _                       | "0.2.0-master.7"
        "0.2.0"           | "0.3.0"  | "0.2.0"          | 7        | 3                  | "ci"         | "develop"                  | _                            | _                       | "0.3.0-develop.7"

        _                 | "0.2.0"  | "0.1.0"          | 1        | 0                  | "staging"    | "develop"                  | _                            | _                       | "0.2.0-staging.1"
        _                 | "0.2.0"  | "0.1.0"          | 2        | 0                  | "staging"    | "develop"                  | _                            | /^master$/              | "0.2.0-staging.1"
        _                 | "0.2.0"  | "0.1.0"          | 3        | 0                  | "staging"    | "master"                   | _                            | _                       | "0.1.0-staging.1"
        _                 | "0.2.0"  | "0.1.0"          | 4        | 0                  | "staging"    | "master"                   | /^(release\/.*|production)$/ | _                       | "0.2.0-staging.1"
        _                 | "0.2.0"  | "0.1.0"          | 5        | 0                  | "staging"    | "production"               | /^(release\/.*|production)$/ | /^(master|production)$/ | "0.1.0-staging.1"
        _                 | "0.2.0"  | "0.1.0"          | 6        | 0                  | "staging"    | "feature/test"             | _                            | _                       | "0.2.0-staging.1"
        "0.2.0-staging.1" | "0.3.0"  | "0.2.0"          | 7        | 3                  | "staging"    | "master"                   | _                            | _                       | "0.2.0-staging.2"
        "0.2.0-staging.1" | "0.3.0"  | "0.2.0"          | 7        | 3                  | "staging"    | "develop"                  | _                            | _                       | "0.3.0-staging.1"

        _                 | "0.2.0"  | "0.1.0"          | 1        | 0                  | "production" | "develop"                  | _                            | _                       | "0.2.0"
        _                 | "0.2.0"  | "0.1.0"          | 2        | 0                  | "production" | "develop"                  | _                            | /^master$/              | "0.2.0"
        _                 | "0.2.0"  | "0.1.0"          | 3        | 0                  | "production" | "master"                   | _                            | _                       | "0.1.0"
        _                 | "0.2.0"  | "0.1.0"          | 4        | 0                  | "production" | "master"                   | /^(release\/.*|production)$/ | _                       | "0.2.0"
        _                 | "0.2.0"  | "0.1.0"          | 5        | 0                  | "production" | "production"               | /^(release\/.*|production)$/ | /^(master|production)$/ | "0.1.0"
        _                 | "0.2.0"  | "0.1.0"          | 6        | 0                  | "production" | "feature/test"             | _                            | _                       | "0.2.0"
        "0.2.0-staging.1" | "0.3.0"  | "0.2.0"          | 7        | 3                  | "production" | "master"                   | _                            | _                       | "0.2.0"
        "0.2.0-staging.1" | "0.3.0"  | "0.2.0"          | 7        | 3                  | "production" | "develop"                  | _                            | _                       | "0.3.0"

        _                 | "0.2.0"  | "0.1.0"          | 10       | 0                  | "ci"         | "test/build01-"            | _                            | _                       | "0.2.0-branch.test.build.1.10"
        _                 | "0.2.0"  | _                | 22       | 0                  | "ci"         | "test/build01+"            | _                            | _                       | "0.2.0-branch.test.build.1.22"
        _                 | "0.2.0"  | _                | 45       | 0                  | "ci"         | "test/build01_"            | _                            | _                       | "0.2.0-branch.test.build.1.45"
        _                 | "0.2.0"  | _                | 204      | 0                  | "ci"         | "test/build01"             | _                            | _                       | "0.2.0-branch.test.build.1.204"
        _                 | "0.2.0"  | _                | 100      | 0                  | "ci"         | "test/build.01"            | _                            | _                       | "0.2.0-branch.test.build.1.100"
        _                 | "0.2.0"  | _                | 55       | 0                  | "ci"         | "test/build002"            | _                            | _                       | "0.2.0-branch.test.build.2.55"
        _                 | "0.2.0"  | _                | 66       | 0                  | "ci"         | "test/build.002"           | _                            | _                       | "0.2.0-branch.test.build.2.66"
        _                 | "0.2.0"  | _                | 789      | 0                  | "ci"         | "test/build000000000003"   | _                            | _                       | "0.2.0-branch.test.build.3.789"
        _                 | "0.2.0"  | _                | 777      | 0                  | "ci"         | "test/build.000000000003"  | _                            | _                       | "0.2.0-branch.test.build.3.777"
        _                 | "0.2.0"  | _                | 789      | 0                  | "ci"         | "test/build000000.000003"  | _                            | _                       | "0.2.0-branch.test.build.0.3.789"
        _                 | "0.2.0"  | _                | 3        | 0                  | "ci"         | "test/build.000000.000003" | _                            | _                       | "0.2.0-branch.test.build.0.3.3"
        _                 | "0.2.0"  | "0.1.0"          | 3        | 0                  | "ci"         | "release/1.00.x"           | _                            | _                       | "0.1.0-branch.release.1.0.x.3"

        nearestNormal = '1.0.0'
        productionMarkerTitle = (productionMarker == _) ? "unset" : productionMarker
        ciMarkerTitle = (ciMarker == _) ? "unset" : ciMarker
    }

    @Unroll("Finds correct nearest version #useTagPrefix from nearestNormal: #nearestNormal, nearestAny: #nearestAnyTitle, scope: #scopeTitle, stage: #stage and branch: #branchName")
    def "finds nearest version tag"() {
        given: "a project with specified release stage and scope"

        project.ext.set('release.stage', stage)
        project.ext.set('version.scheme', scheme)
        if (scope != _) {
            project.ext.set('release.scope', scope)
        }

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }

        5.times {
            git.commit(message: 'feature commit')
        }

        git.tag.add(name: "$tagPrefix$nearestNormal")

        distance.times {
            git.commit(message: 'fix commit')
        }

        if (nearestAny != _) {
            git.tag.add(name: "$tagPrefix$nearestAny")
            distance.times {
                git.commit(message: 'fix commit')
            }
        }

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion

        where:
        nearestAny | distance | stage      | scope | scheme                | branchName | prefixTag | expectedVersion
        _          | 1        | "ci"       | _     | VersionScheme.semver2 | "master"   | true      | "1.1.0-master.1"
        _          | 1        | "ci"       | _     | VersionScheme.semver2 | "master"   | false     | "1.1.0-master.1"
        _          | 1        | "snapshot" | _     | VersionScheme.semver  | "master"   | true      | "1.0.1-master00001"
        _          | 1        | "snapshot" | _     | VersionScheme.semver  | "master"   | false     | "1.0.1-master00001"

        nearestNormal = '1.0.0'
        nearestAnyTitle = (nearestAny == _) ? "unset" : nearestAny
        scopeTitle = (scope == _) ? "unset" : scope
        tagPrefix = (prefixTag) ? "v" : ""
        useTagPrefix = (prefixTag) ? "with tag prefix v" : "without tag prefix"
    }

    @Unroll
    def "project property #property is a #type"() {
        given:
        project.plugins.apply(PLUGIN_NAME)
        def value = project.property(property)

        expect:
        type.isInstance(value)

        where:
        property      | type
        "version"     | ToStringProvider.class
        "versionCode" | VersionCodeExtension.class
    }

    def "can set custom versionCode"() {
        given:
        project.plugins.apply(PLUGIN_NAME)

        when:
        project.ext.versionCode = 12345

        then:
        project.property("versionCode") == 12345
    }

    def "can set custom versionCode in provided extension"() {
        given:
        project.plugins.apply(PLUGIN_NAME)
        def extension = project.extensions.getByType(VersionCodeExtension)

        when:
        extension.set(12345)

        then:
        project.property("versionCode").get() == 12345
    }

    @Unroll('verify version branch rename for branch #branchName')
    def "applies branch rename for nuget repos"() {

        given: "a project with specified SNAPSHOT release stage"

        project.ext.set('release.stage', "SNAPSHOT")

        and: "a history"

        if (branchName != "master") {
            git.checkout(branch: "$branchName", startPoint: 'master', createBranch: true)
        }

        git.tag.add(name: "v1.0.0")
        git.commit(message: 'feature commit')

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.version.toString() == expectedVersion

        where:
        branchName          | expectedVersion
        "master"            | "1.0.1-master00001"
        "with/slash"        | "1.0.1-branchWithSlash00001"
        "numbers0123456789" | "1.0.1-branchNumbersZeroOneTwoThreeFourFiveSixSevenEightNine00001"
        "with/slash"        | "1.0.1-branchWithSlash00001"
        "with_underscore"   | "1.0.1-branchWithUnderscore00001"
        "with-dash"         | "1.0.1-branchWithDash00001"
    }
}
