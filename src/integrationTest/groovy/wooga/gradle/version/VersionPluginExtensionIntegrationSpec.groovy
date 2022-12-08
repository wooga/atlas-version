package wooga.gradle.version

import com.wooga.gradle.test.PropertyLocation
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import com.wooga.gradle.test.writers.PropertySetterWriter
import org.gradle.api.file.Directory
import spock.lang.Unroll

class VersionPluginExtensionIntegrationSpec extends VersionIntegrationSpec {

    def setup() {
        buildFile << """
            ${applyPlugin(VersionPlugin)}
        """.stripIndent()
    }

    @Unroll("can set property versionBuilder.#property with gradle property #gradlePropName=#rawValue")
    def "can set extension property with gradle property"() {
        given:
        set.location = rawValue == null ? PropertyLocation.none : set.location
        when:
        def propertyQuery = runPropertyQuery(get, set).withSerializer(Directory) {
            String dir -> new File(projectDir, dir).absolutePath
        }

        then:
        propertyQuery.matches(expectedValue)

        where:
        property | gradlePropName  | type   | rawValue   | expectedValue
        "stage"  | "release.stage" | String | "snapshot" | "snapshot"
        "stage"  | "release.stage" | String | "rc"       | "rc"
        "stage"  | "release.stage" | String | "final"    | "final"
        "stage"  | "release.stage" | String | "custom"   | "custom"
        "stage"  | "release.stage" | String | null       | null
        "scope"  | "release.scope" | String | "major"    | "MAJOR"
        "scope"  | "release.scope" | String | "minor"    | "MINOR"
        "scope"  | "release.scope" | String | "patch"    | "PATCH"
        "scope"  | "release.scope" | String | "PatCh"    | "PATCH"
        "scope"  | "release.scope" | String | "PATCH"    | "PATCH"
        "scope"  | "release.scope" | String | null       | null


        set = new PropertySetterWriter("versionBuilder", property)
                .set(rawValue, type)
                .withPropertyKey(gradlePropName)
                .to(PropertyLocation.propertyCommandLine)
        get = new PropertyGetterTaskWriter(set)
    }

    //Line bellow doesn't work on gradle-commons-test framework so I'm doing it myself.
    //        "stage"  | "release.stage" | String | ""       | null
    @Unroll("can set property versionBuilder.#property with gradle property #gradlePropName=#rawValue")
    def "can set extension property with gradle property but w/out gradle-commons-test"() {
        given:
        buildFile << """
            tasks.register("$taskName") {
                doLast {
                    println("[[[$gradlePropName=\${versionBuilder.${property}.orNull}]]]")
                }
            }
        """

        when:
        def result = runTasks(taskName, "-P${gradlePropName}=${rawValue}",)

        then:
        result.standardOutput.contains("[[[${gradlePropName}=${expectedValue}]]]")

        where:
        property | gradlePropName  | type   | rawValue | expectedValue
        "stage"  | "release.stage" | String | ""       | ""
        "scope"  | "release.scope" | String | ""       | null
        taskName = "getterTask"
    }


}
