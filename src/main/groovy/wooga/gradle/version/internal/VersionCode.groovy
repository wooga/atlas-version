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

import wooga.gradle.version.VersionCodeSchemes
import wooga.gradle.version.internal.release.git.GitVersionRepository

class VersionCode {

    enum Schemes {
        none(VersionCodeSchemes.none, { -> 0}),
        semverBasic(VersionCodeSchemes.semverBasic, {
            String version, int offset -> generateSemverVersionCode(version) + offset
        }),
        semver(VersionCodeSchemes.semver, {
            String version, int offset -> generateSemverVersionCode(version, true) + offset
        }),
        releaseCountBasic(VersionCodeSchemes.releaseCountBasic, {
            GitVersionRepository git, int offset -> generateBuildNumberVersionCode(git, false) + offset
        }),
        releaseCount(VersionCodeSchemes.releaseCount, {
            GitVersionRepository git, int offset -> generateBuildNumberVersionCode(git, true) + offset
        }),
        timestamp(VersionCodeSchemes.timestamp, { -> generateBuildTimeVersionCode()});

        private final VersionCodeSchemes external
        private final Closure<Integer> generator

        static Schemes fromExternal(VersionCodeSchemes external) {
            return values().find { it.external == external}
        }

        Schemes(VersionCodeSchemes external, Closure<Integer> generator) {
            this.external = external
            this.generator = generator
        }

        int versionCodeFor(String version, GitVersionRepository versionRepo, int offset) {
            if(generator.parameterTypes.length == 0) {
                return this.generator.call()
            }
            if(generator.parameterTypes[0] == String) {
                return this.generator.call(version, offset)
            } else {
                return this.generator.call(versionRepo, offset)
            }
        }
    }


    /**
     * Returns a version code base on the given semver version.
     *
     * The returned versioncode is in the form of
     * <pre>
     * {@code
     *   major | minor | patch | revision
     *    00      00      00       0
     *}
     * </pre>
     * The method will only pull the {@code major}, @{code minor} and {@code patch} components out of the version.
     * Not all valid semver versions can be converted. The maximum value for {@code major}, @{code minor} and
     * {@code patch} components is @{code 99}. The greates valid version is @{code 99.99.99}.
     * The revision field will only be populated when passing a special {@code staging} version e.g. {@code 1.0.0-staging.1}.
     * The length of the revision value is only a single digit so a maximum of 8 staging versions can be represented.
     * <p>
     * Example:
     * <pre>
     * "1.0.0"             => 100000
     * "1.2.3"             => 102030
     * "1.2.3-staging.1"   => 102031
     * "0.22.0-develop.93" => 22000
     * </pre>
     *
     * @param version A semver 2.0 compatible version string
     * @return a versioncode for the provided version
     *
     */
    static Integer generateSemverVersionCode(String version, Boolean includeRevision = false) {
        def versionMatch = version =~ /(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)(-staging\.(?<rev>\d+))?.*?/

        if (!versionMatch.matches()) {
            throw new IllegalArgumentException("Failed to generate versioncode. Invalid version (${version}). Please provide version in form of '0.0.0'")
        }
        def versionCode = 0
        def versionMajor = versionMatch.group("major").toInteger()
        def versionMinor = versionMatch.group('minor').toInteger()
        def versionPatch = versionMatch.group('patch').toInteger()
        def versionRev = (versionMatch.group('rev') ?: "0").toInteger()

        def errors = []

        if (versionMajor > 99) {
            errors << "Major component > 99"
        }

        if (versionMinor > 99) {
            errors << "Minor component > 99"
        }

        if (versionPatch > 99) {
            errors << "Patch component > 99"
        }

        if (includeRevision && versionRev > 9) {
            errors << "Staging Revision > 9"
        }

        if (!errors.empty) {
            throw new IllegalArgumentException("Failed to generate versioncode. ${errors.join(". ")}(${version})")
        }

        versionCode += versionMajor * 10000
        versionCode += versionMinor * 100
        versionCode += versionPatch * 1

        if(includeRevision) {
            versionCode *= 10
            versionCode += versionRev
        }

        versionCode
    }

    /**
     * Returns a versioncode based on the number of version tags in the given git repository + 1
     *
     * @param git
     * @param countPrerelease
     * @return
     */
    static Integer generateBuildNumberVersionCode(GitVersionRepository versionRepo, Boolean countPrerelease = true) {
        return versionRepo.countReachableVersions(countPrerelease) + 1
    }

    /**
     * Returns the time since 1.1.2024 in seconds
     * should fit in 31 bits (we need one for the sign) for about 68 years until 2092
     * @return
     */
    static Integer generateBuildTimeVersionCode() {
        def date = new Date()
        def buildTime = date.getTime() - 1704063600000 // 1.1.2024
        return (int)(buildTime / 1000)
    }

}
