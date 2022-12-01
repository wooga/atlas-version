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

import org.ajoberstar.grgit.Grgit
import wooga.gradle.version.internal.VersionCode

enum VersionCodeScheme {
    none({ -> 0}),
    semverBasic({
        String version, int offset -> VersionCode.generateSemverVersionCode(version) + offset
    }),
    semver({
        String version, int offset -> VersionCode.generateSemverVersionCode(version, true) + offset
    }),
    releaseCountBasic({
        Grgit git, int offset -> VersionCode.generateBuildNumberVersionCode(git, false, offset)
    }),
    releaseCount({
        Grgit git, int offset -> VersionCode.generateBuildNumberVersionCode(git, true, offset)
    })

    final Closure<Integer> generate

    VersionCodeScheme(Closure generate) {
        this.generate = generate
    }
}
