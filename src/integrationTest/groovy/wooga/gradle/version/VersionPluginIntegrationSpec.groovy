/*
 * Copyright 2018-2022 Wooga GmbH
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
 */

package wooga.gradle.version

import com.wooga.gradle.PlatformUtils
import com.wooga.gradle.test.PropertyLocation
import com.wooga.gradle.test.PropertyQueryTaskWriter
import wooga.gradle.version.internal.release.semver.ChangeScope
import spock.lang.Unroll

class VersionPluginIntegrationSpec extends VersionIntegrationSpec {
    def setup() {
        buildFile << """
            ${applyPlugin(VersionPlugin)}
        """.stripIndent()
    }

    String envNameFromProperty(String property) {
        "VERSION_BUILDER_${property.replaceAll(/([A-Z])/, "_\$1").toUpperCase()}"
    }

    @Unroll()
    def "extension property :#property returns '#testValue' if #reason"() {
        given:
        buildFile << """
            task(custom) {
                doLast {
                    def value = ${extensionName}.${property}.getOrNull()
                    println("${extensionName}.${property}: " + value)
                }
            }
        """

        and: "a gradle.properties"
        def propertiesFile = createFile("gradle.properties")

        def escapedValue = (value instanceof String) ? PlatformUtils.escapedPath(value) : value

        switch (location) {
            case PropertyLocation.script:
                buildFile << "${extensionName}.${property} = ${escapedValue}"
                break
            case PropertyLocation.property:
                propertiesFile << "${extensionName}.${property} = ${escapedValue}"
                break
            case PropertyLocation.environment:
                environmentVariables.set(envNameFromProperty(property), "${value}")
                break
            default:
                break
        }

        and: "the test value with replace placeholders"
        if (testValue instanceof String) {
            testValue = testValue.replaceAll("#projectDir#", PlatformUtils.escapedPath(projectDir.path))
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("${extensionName}.${property}: ${testValue}")

        where:
        property               | value               | expectedValue                       | location
        "scope"                | "major"             | ChangeScope.MAJOR                   | PropertyLocation.environment
        "scope"                | "MAJOR"             | ChangeScope.MAJOR                   | PropertyLocation.environment
        "scope"                | "Major"             | ChangeScope.MAJOR                   | PropertyLocation.environment
        "scope"                | "minor"             | ChangeScope.MINOR                   | PropertyLocation.environment
        "scope"                | "MINOR"             | ChangeScope.MINOR                   | PropertyLocation.environment
        "scope"                | "Minor"             | ChangeScope.MINOR                   | PropertyLocation.environment
        "scope"                | "patch"             | ChangeScope.PATCH                   | PropertyLocation.environment
        "scope"                | "PATCH"             | ChangeScope.PATCH                   | PropertyLocation.environment
        "scope"                | "Patch"             | ChangeScope.PATCH                   | PropertyLocation.environment
        "scope"                | "major"             | ChangeScope.MAJOR                   | PropertyLocation.property
        "scope"                | "MAJOR"             | ChangeScope.MAJOR                   | PropertyLocation.property
        "scope"                | "Major"             | ChangeScope.MAJOR                   | PropertyLocation.property
        "scope"                | "minor"             | ChangeScope.MINOR                   | PropertyLocation.property
        "scope"                | "MINOR"             | ChangeScope.MINOR                   | PropertyLocation.property
        "scope"                | "Minor"             | ChangeScope.MINOR                   | PropertyLocation.property
        "scope"                | "patch"             | ChangeScope.PATCH                   | PropertyLocation.property
        "scope"                | "PATCH"             | ChangeScope.PATCH                   | PropertyLocation.property
        "scope"                | "Patch"             | ChangeScope.PATCH                   | PropertyLocation.property
        "scope"                | "null"              | _                                   | PropertyLocation.none
        "stage"                | "snapshot"          | _                                   | PropertyLocation.environment
        "stage"                | "final"             | _                                   | PropertyLocation.property
        "stage"                | "null"              | _                                   | PropertyLocation.none
        "versionScheme"        | "semver2"           | VersionSchemes.semver2 | PropertyLocation.environment
        "versionScheme"        | "semver"            | VersionSchemes.semver  | PropertyLocation.environment
        "versionScheme"        | "semver2"           | VersionSchemes.semver2 | PropertyLocation.property
        "versionScheme"        | "semver"            | VersionSchemes.semver  | PropertyLocation.property
        "versionCodeScheme"    | "releaseCountBasic" | VersionCodeSchemes.releaseCountBasic | PropertyLocation.environment
        "versionCodeScheme"    | "releaseCount"      | VersionCodeSchemes.releaseCount      | PropertyLocation.environment
        "versionCodeScheme"    | "semver"            | VersionCodeSchemes.semver            | PropertyLocation.environment
        "versionCodeScheme"    | "none"              | VersionCodeSchemes.none              | PropertyLocation.environment
        "versionCodeScheme"    | "releaseCountBasic" | VersionCodeSchemes.releaseCountBasic | PropertyLocation.property
        "versionCodeScheme"    | "releaseCount"      | VersionCodeSchemes.releaseCount      | PropertyLocation.property
        "versionCodeScheme"    | "semver"            | VersionCodeSchemes.semver            | PropertyLocation.property
        "versionCodeScheme"    | "none"              | VersionCodeSchemes.none              | PropertyLocation.property
        "versionCodeOffset"    | 100                 | _                                   | PropertyLocation.environment
        "versionCodeOffset"    | 200                 | _                                   | PropertyLocation.property
        "releaseBranchPattern" | /^m.*/              | _                                   | PropertyLocation.property
        "releaseBranchPattern" | /(some|value)/      | _                                   | PropertyLocation.environment
        "releaseBranchPattern" | '/.*/'              | /.*/                                | PropertyLocation.script
        "mainBranchPattern"    | /^m.*/              | _                                   | PropertyLocation.property
        "mainBranchPattern"    | /(some|value)/      | _                                   | PropertyLocation.environment
        "mainBranchPattern"    | '/.*/'              | /.*/                                | PropertyLocation.script
        "prefix"               | "v_"                | _                                   | PropertyLocation.property
        "prefix"               | "v_"                | _                                   | PropertyLocation.environment

        extensionName = "versionBuilder"
        testValue = (expectedValue == _) ? value : expectedValue
        reason = location.reason() + ((location == PropertyLocation.none) ? "" : " with '$value'")
    }

    @Unroll
    def "can set extension property :#property with :#method and type '#type'"() {
        given: "a task to print appCenter properties"
        buildFile << """
            task(custom) {
                doLast {
                    def value = ${extensionName}.${property}.get()
                    println("${extensionName}.${property}: " + value)
                }
            }
        """

        and: "some configured property"
        buildFile << "${extensionName}.${method}(${value})"

        and: "the test value with replace placeholders"
        if (value instanceof String) {
            value = value.replaceAll("#projectDir#", PlatformUtils.escapedPath(projectDir.path))
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("${extensionName}.${property}: ${rawValue}")

        where:
        property               | method                     | rawValue                            | type
        "versionScheme"        | "versionScheme"            | "semver"                            | "String"
        "versionScheme"        | "versionScheme"            | VersionSchemes.semver2      | "VersionScheme"
        "versionScheme"        | "versionScheme"            | VersionSchemes.semver       | "Provider<VersionScheme>"
        "versionScheme"        | "versionScheme"            | VersionSchemes.staticMarker | "Provider<VersionScheme>"
        "versionScheme"        | "versionScheme.set"        | VersionSchemes.semver2      | "VersionScheme"
        "versionScheme"        | "versionScheme.set"        | VersionSchemes.semver       | "Provider<VersionScheme>"
        "versionScheme"        | "versionScheme.set"        | VersionSchemes.staticMarker | "Provider<VersionScheme>"
        "versionScheme"        | "setVersionScheme"         | "semver"                            | "String"
        "versionScheme"        | "setVersionScheme"         | VersionSchemes.semver2      | "VersionScheme"
        "versionScheme"        | "setVersionScheme"         | VersionSchemes.semver       | "Provider<VersionScheme>"

        "versionCodeScheme"    | "versionCodeScheme"        | "releaseCountBasic"                 | "String"
        "versionCodeScheme"    | "versionCodeScheme"        | VersionCodeSchemes.semver            | "VersionCodeScheme"
        "versionCodeScheme"    | "versionCodeScheme"        | VersionCodeSchemes.releaseCountBasic | "Provider<VersionCodeScheme>"
        "versionCodeScheme"    | "versionCodeScheme.set"    | VersionCodeSchemes.semver            | "VersionCodeScheme"
        "versionCodeScheme"    | "versionCodeScheme.set"    | VersionCodeSchemes.releaseCountBasic | "Provider<VersionCodeScheme>"
        "versionCodeScheme"    | "setVersionCodeScheme"     | "releaseCount"                      | "String"
        "versionCodeScheme"    | "setVersionCodeScheme"     | VersionCodeSchemes.none              | "VersionCodeScheme"
        "versionCodeScheme"    | "setVersionCodeScheme"     | VersionCodeSchemes.releaseCount      | "Provider<VersionCodeScheme>"

        "versionCodeOffset"    | "versionCodeOffset"        | 1                                   | "Integer"
        "versionCodeOffset"    | "versionCodeOffset"        | 2                                   | "Provider<Integer>"
        "versionCodeOffset"    | "versionCodeOffset.set"    | 3                                   | "Integer"
        "versionCodeOffset"    | "versionCodeOffset.set"    | 4                                   | "Provider<Integer>"
        "versionCodeOffset"    | "setVersionCodeOffset"     | 5                                   | "Integer"
        "versionCodeOffset"    | "setVersionCodeOffset"     | 6                                   | "Provider<Integer>"

        "releaseBranchPattern" | "releaseBranchPattern"     | /.*/                                | "String"
        "releaseBranchPattern" | "releaseBranchPattern"     | /(some|value)/                      | "Provider<String>"
        "releaseBranchPattern" | "releaseBranchPattern.set" | /[a-z]/                             | "String"
        "releaseBranchPattern" | "releaseBranchPattern.set" | /[0-9]/                             | "Provider<String>"
        "releaseBranchPattern" | "setReleaseBranchPattern"  | /[alto]-[sierra]/                   | "String"
        "releaseBranchPattern" | "setReleaseBranchPattern"  | /[whooom]/                          | "Provider<String>"

        "mainBranchPattern"    | "mainBranchPattern"        | /.*/                                | "String"
        "mainBranchPattern"    | "mainBranchPattern"        | /(some|value)/                      | "Provider<String>"
        "mainBranchPattern"    | "mainBranchPattern.set"    | /[a-z]/                             | "String"
        "mainBranchPattern"    | "mainBranchPattern.set"    | /[0-9]/                             | "Provider<String>"
        "mainBranchPattern"    | "setMainBranchPattern"     | /[alto]-[sierra]/                   | "String"
        "mainBranchPattern"    | "setMainBranchPattern"     | /[whooom]/                          | "Provider<String>"

        "prefix"               | "prefix"                   | "prefix"                            | "String"
        "prefix"               | "prefix"                   | "v-"                                | "Provider<String>"

        value = wrapValueBasedOnType(rawValue, type)
        extensionName = "versionBuilder"
    }

    @Unroll
    def "can custom versionCode with :#method and type '#type'"() {
        given: "a task to print appCenter properties"
        buildFile << """
            task(custom) {
                doLast {
                    def value = ${fetchMethod}
                    println("project.${property}: " + value)
                }
            }
        """

        and: "some configured property"
        buildFile << methodInvocation

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("project.${property}: ${rawValue}")

        where:
        property      | method            | rawValue | type                | isProperty

        "versionCode" | "ext.versionCode" | 1        | "Integer"           | true
        "versionCode" | "versionCode.set" | 5        | "Integer"           | false
        "versionCode" | "versionCode.set" | 6        | "Provider<Integer>" | false

        value = wrapValueBasedOnType(rawValue, type)
        methodInvocation = isProperty ? "${method} = ${value}" : "${method}(${value})"
        fetchMethod = method.startsWith("ext") ? "project.${property}" : "project.${property}.get()"
    }

    @Unroll
    def "can deduce release stage #expected from #stage with version scheme #versionScheme"() {
        given: "a task to print appCenter properties"
        buildFile << """
            versionBuilder {
                versionScheme = ${wrapValueBasedOnType(versionScheme, String)}
            }
        """
        // If the stage is set
        if (stage != _) {
            buildFile << """
            versionBuilder {
                stage = ${wrapValueBasedOnType(stage, String)}
            }
        """
        }

        when: ""
        def query = new PropertyQueryTaskWriter("${VersionPlugin.EXTENSION_NAME}.releaseStage")
        query.write(buildFile)
        def result = runTasksSuccessfully(query.taskName)

        then:
        query.matches(result, expected)

        where:
        stage      | versionScheme | expected
        "pancakes" | "semver2"     | ReleaseStage.Unknown
        _          | "semver2"     | ReleaseStage.Development
        "dev"      | "semver2"     | ReleaseStage.Development
        "snapshot" | "semver2"     | ReleaseStage.Snapshot
        "rc"       | "semver2"     | ReleaseStage.Prerelease
        "final"    | "semver2"     | ReleaseStage.Final

        "pancakes" | "semver"      | ReleaseStage.Unknown
        _          | "semver"      | ReleaseStage.Development
        "dev"      | "semver"      | ReleaseStage.Development
        "snapshot" | "semver"      | ReleaseStage.Snapshot
        "rc"       | "semver"      | ReleaseStage.Prerelease
        "final"    | "semver"      | ReleaseStage.Final
    }

    @Unroll
    def "can set custom version directly by #location in provided extension"() {

        given: "the setting of said version"
        def propertiesFile = createFile("gradle.properties")
        def escapedValue = (value instanceof String) ? PlatformUtils.escapedPath(value) : value
        switch (location) {
            case PropertyLocation.script:
                buildFile << "project.${property} = ${wrapValueBasedOnType(escapedValue, String)}"
                break
            case PropertyLocation.property:
                propertiesFile << "${extensionName}.${property} = ${escapedValue}"
                break
            case PropertyLocation.environment:
                environmentVariables.set(envNameFromProperty(property), "${value}")
                break
            default:
                break
        }

        when: ""
        def query = new PropertyQueryTaskWriter("project.version", "")
        query.write(buildFile)
        def result = runTasksSuccessfully(query.taskName)

        then:
        query.matches(result, value)

        where:
        property  | value   | location
        "version" | "1.2.3" | PropertyLocation.script
        "version" | "1.2.3" | PropertyLocation.property
        "version" | "1.2.3" | PropertyLocation.environment
        extensionName = "versionBuilder"
    }

}
