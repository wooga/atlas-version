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

package wooga.gradle.version

import wooga.gradle.version.internal.release.semver.ChangeScope
import spock.lang.Unroll

class VersionPluginIntegrationSpec extends IntegrationSpec {
    def setup() {
        buildFile << """
            ${applyPlugin(VersionPlugin)}
        """.stripIndent()
    }

    enum PropertyLocation {
        none, script, property, env

        String reason() {
            switch (this) {
                case script:
                    return "value is provided in script"
                case property:
                    return "value is provided in props"
                case env:
                    return "value is set in env"
                default:
                    return "no value was configured"
            }
        }
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

        def escapedValue = (value instanceof String) ? escapedPath(value) : value

        switch (location) {
            case PropertyLocation.script:
                buildFile << "${extensionName}.${property} = ${escapedValue}"
                break
            case PropertyLocation.property:
                propertiesFile << "${extensionName}.${property} = ${escapedValue}"
                break
            case PropertyLocation.env:
                environmentVariables.set(envNameFromProperty(property), "${value}")
                break
            default:
                break
        }

        and: "the test value with replace placeholders"
        if (testValue instanceof String) {
            testValue = testValue.replaceAll("#projectDir#", escapedPath(projectDir.path))
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("${extensionName}.${property}: ${testValue}")

        where:
        property | value      | expectedValue     | location
        "scope"  | "major"    | ChangeScope.MAJOR | PropertyLocation.env
        "scope"  | "MAJOR"    | ChangeScope.MAJOR | PropertyLocation.env
        "scope"  | "Major"    | ChangeScope.MAJOR | PropertyLocation.env
        "scope"  | "minor"    | ChangeScope.MINOR | PropertyLocation.env
        "scope"  | "MINOR"    | ChangeScope.MINOR | PropertyLocation.env
        "scope"  | "Minor"    | ChangeScope.MINOR | PropertyLocation.env
        "scope"  | "patch"    | ChangeScope.PATCH | PropertyLocation.env
        "scope"  | "PATCH"    | ChangeScope.PATCH | PropertyLocation.env
        "scope"  | "Patch"    | ChangeScope.PATCH | PropertyLocation.env
        "scope"  | "major"    | ChangeScope.MAJOR | PropertyLocation.property
        "scope"  | "MAJOR"    | ChangeScope.MAJOR | PropertyLocation.property
        "scope"  | "Major"    | ChangeScope.MAJOR | PropertyLocation.property
        "scope"  | "minor"    | ChangeScope.MINOR | PropertyLocation.property
        "scope"  | "MINOR"    | ChangeScope.MINOR | PropertyLocation.property
        "scope"  | "Minor"    | ChangeScope.MINOR | PropertyLocation.property
        "scope"  | "patch"    | ChangeScope.PATCH | PropertyLocation.property
        "scope"  | "PATCH"    | ChangeScope.PATCH | PropertyLocation.property
        "scope"  | "Patch"    | ChangeScope.PATCH | PropertyLocation.property
        "scope"  | "null"     | _                 | PropertyLocation.none
        "stage"  | "snapshot" | _                 | PropertyLocation.env
        "stage"  | "final"    | _                 | PropertyLocation.property
        "stage"  | "null"     | _                 | PropertyLocation.none

        extensionName = "versionBuilder"
        testValue = (expectedValue == _) ? value : expectedValue
        reason = location.reason() + ((location == PropertyLocation.none) ? "" : " with '$value'")
    }

    @Unroll
    def "can set property :#property with :#method and type '#type'"() {
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
            value = value.replaceAll("#projectDir#", escapedPath(projectDir.path))
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("${extensionName}.${property}: ${rawValue}")

        where:
        property | method       | rawValue | type
        "scheme" | "scheme"     | "value1" | "String"
        "scheme" | "scheme.set" | "value2" | "String"
        "scheme" | "scheme.set" | "value2" | "Provider<String>"
        "scheme" | "setScheme"  | "value2" | "String"
        "scheme" | "setScheme"  | "value2" | "Provider<String>"

        value = wrapValueBasedOnType(rawValue, type)
        extensionName = "versionBuilder"
    }

}
