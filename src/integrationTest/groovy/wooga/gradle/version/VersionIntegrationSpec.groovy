/*
 * Copyright 2019-2022 Wooga GmbH
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
 */

package wooga.gradle.version

import com.wooga.gradle.test.IntegrationSpec
import wooga.gradle.version.internal.release.semver.ChangeScope

class VersionIntegrationSpec extends IntegrationSpec {

    def setup() {
        def gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion) {
            this.gradleVersion = gradleVersion
            fork = true
        }
    }

    static wrapValueFallback = { Object rawValue, String type, Closure<String> fallback ->
        switch (type) {
            case "ChangeScope":
                if(rawValue instanceof ChangeScope) {
                    return "wooga.gradle.version.internal.release.semver.ChangeScope.${rawValue.toString()}"
                } else {
                    return "wooga.gradle.version.internal.release.semver.ChangeScope.valueOf('${rawValue.toString()}')"
                }
            case "VersionScheme":
                if(VersionSchemes.isInstance(rawValue)) {
                    return "wooga.gradle.version.VersionSchemes.${rawValue.toString()}"
                } else {
                    return "wooga.gradle.version.VersionSchemes.valueOf('${rawValue.toString()}')"
                }
                break
            case "VersionCodeScheme":
            case "VersionCodeSchemes":
                if(VersionCodeSchemes.isInstance(rawValue)) {
                    return "wooga.gradle.version.VersionCodeSchemes.${rawValue.toString()}"
                } else {
                    return "wooga.gradle.version.VersionCodeSchemes.valueOf('${rawValue.toString()}')"
                }
                break
            case "Grgit":
                if(rawValue == _) {
                    return "org.ajoberstar.grgit.Grgit.init()"
                }
                break
            default:
                return rawValue.toString()
        }
    }

    String wrapValueBasedOnType(Object rawValue, String type) {
        wrapValueBasedOnType(rawValue, type, wrapValueFallback)
    }
}
