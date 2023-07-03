/*
 * Copyright 2018-2020 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package wooga.gradle.version.internal

import org.ajoberstar.grgit.Grgit
import spock.lang.Specification
import spock.lang.Unroll
import wooga.gradle.version.internal.release.git.GitVersionRepository
import wooga.gradle.version.internal.release.base.PrefixVersionParser

class VersionCodeSpec extends Specification {

    @Unroll
    def ":generateSemverVersionCode generates unique versioncode #message #expectedVersionCode from version #version"() {
        expect:
        VersionCode.generateSemverVersionCode(version, includeRevision) == expectedVersionCode
        where:
        version              | expectedVersionCode | includeRevision
        "1.0.0"              | 100000              | true
        "1.2.3"              | 102030              | true
        "10.20.30"           | 1020300             | true
        "1.2.3-staging.1"    | 102031              | true
        "3.4.1-rc.5"         | 304010              | true
        "0.22.0-develop.93"  | 22000               | true
        "1.22.0-develop.111" | 122000              | true
        "3.2.2develop.111"   | 302020              | true
        "99.99.99-staging.9" | 9999999             | true
        "99.99.99-dev.9"     | 9999990             | true
        "1.0.0"              | 10000               | false
        "1.2.3"              | 10203               | false
        "10.20.30"           | 102030              | false
        "1.2.3-staging.1"    | 10203               | false
        "3.4.1-rc.5"         | 30401               | false
        "0.22.0-develop.93"  | 2200                | false
        "1.22.0-develop.111" | 12200               | false
        "3.2.2develop.111"   | 30202               | false
        "99.99.99-staging.9" | 999999              | false
        "99.99.99-dev.9"     | 999999              | false
        message = (includeRevision) ? "with revision" : ""
    }

    @Unroll
    def ":generateSemverVersionCode throws error for #version #message because #expectedError"() {
        when:
        VersionCode.generateSemverVersionCode(version, includeRevision)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(expectedError)

        where:
        version                  | expectedError          | includeRevision
        "100.2.3"                | "Major component > 99" | true
        "1.100.3"                | "Minor component > 99" | true
        "1.1.100"                | "Patch component > 99" | true
        "1.2.3-staging.11"       | "Staging Revision > 9" | true
        "100.100.100-staging.10" | "Major component > 99" | true
        "100.100.100-staging.10" | "Minor component > 99" | true
        "100.100.100-staging.10" | "Patch component > 99" | true
        "100.100.100-staging.10" | "Staging Revision > 9" | true
        "aaaa"                   | "Invalid version"      | true
        "1.a.3"                  | "Invalid version"      | true
        "100.2.3"                | "Major component > 99" | false
        "1.100.3"                | "Minor component > 99" | false
        "1.1.100"                | "Patch component > 99" | false
        "100.100.100-staging.10" | "Major component > 99" | false
        "100.100.100-staging.10" | "Minor component > 99" | false
        "100.100.100-staging.10" | "Patch component > 99" | false
        "aaaa"                   | "Invalid version"      | false
        "1.a.3"                  | "Invalid version"      | false
        message = (includeRevision) ? "with revision" : ""
    }

    @Unroll
    def ":generateSemverVersionCode throws no error for #version when no revision counter is requested"() {
        when:
        VersionCode.generateSemverVersionCode(version, includeRevision)

        then:
        noExceptionThrown()

        where:
        version                  | includeRevision
        "1.2.3-staging.11"       | false
    }

    @Unroll
    def ":generateBuildNumberVersionCode returns number of releases #message"() {
        given: "a repo with a specific number of releases"
        def tempDir = File.createTempDir()
        tempDir.deleteOnExit()

        Grgit git = Grgit.init(dir: tempDir)
        git.commit(message: 'initial commit')

        numberOfNormalTags.times {
            git.commit(message: 'a commit')
            if (it < numberOfPrereleaseTags) {
                git.tag.add(name: "${it}.0.0-rc.1")
                git.tag.add(name: "v${it}.0.0-rc.1")
            }
            git.tag.add(name: "v${it}.0.0")
        }

        Math.max(numberOfPrereleaseTags - numberOfNormalTags, 0).times {
            git.commit(message: 'a commit')
            git.tag.add(name: "v${numberOfNormalTags + 1}.0.0-rc.${it}")
        }

        expect:
        def versionRepository = GitVersionRepository.fromTagStrategy(git, tagStrategy)
        VersionCode.generateBuildNumberVersionCode(versionRepository, countPrerelease) + offset == expectedValue

        cleanup:
        tempDir.deleteDir()

        where:
        numberOfNormalTags | numberOfPrereleaseTags | offset | countPrerelease
        1                  | 0                      | 0      | false
        1                  | 1                      | 0      | false
        1                  | 1                      | 40     | false
        1                  | 1                      | 0      | true
        3                  | 2                      | 100    | true
        3                  | 5                      | 10     | true
        tagStrategy = new PrefixVersionParser("v", false)
        message = "when number of normal tags: ${numberOfNormalTags} prerelease tags: ${numberOfPrereleaseTags} and offset: ${offset}"
        expectedValue = numberOfNormalTags + offset + (countPrerelease ? numberOfPrereleaseTags : 0) + 1
    }

    @Unroll
    def ":generateBuildNumberVersionCode only counts reachable release tags"() {
        given: "a repo with a specific number of releases"
        def tempDir = File.createTempDir()
        tempDir.deleteOnExit()

        Grgit git = Grgit.init(dir: tempDir)
        git.commit(message: 'initial commit')
        git.commit(message: 'a commit')
        git.tag.add(name: "v1.0.0-rc.1")
        git.commit(message: 'a commit')
        git.tag.add(name: "v1.0.0-rc.2")
        git.tag.add(name: "v1.0.0")
        git.branch.add(name: 'develop', startPoint: 'master')
        git.checkout(branch: "develop")
        git.commit(message: 'a commit')
        git.commit(message: 'a commit')
        git.commit(message: 'a commit')
        git.tag.add(name: "v1.0.1-rc.1")

        and: "the correct branch"
        git.checkout(branch: branch)

        expect:
        def versionRepository = GitVersionRepository.fromTagStrategy(git, tagStrategy)
        VersionCode.generateBuildNumberVersionCode(versionRepository, countPrerelease) + offset == expectedValue

        cleanup:
        tempDir.deleteDir()

        where:
        branch    | offset | countPrerelease | expectedValue
        "master"  | 0      | false           | 2
        "master"  | 0      | true            | 4
        "master"  | 10     | true            | 14
        "master"  | 20     | false           | 22
        "develop" | 0      | false           | 2
        "develop" | 0      | true            | 5
        "develop" | 30     | false           | 32
        "develop" | 40     | true            | 45
        tagStrategy = new PrefixVersionParser("v", false)
    }
}
