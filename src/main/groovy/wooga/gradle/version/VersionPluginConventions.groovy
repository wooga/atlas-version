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

import com.wooga.gradle.PropertyLookup

class VersionPluginConventions {

    static final String GIT_ROOT_PROPERTY = "git.root"
    static final String UNINITIALIZED_VERSION = '0.1.0-dev.0.uninitialized'

    static final PropertyLookup gitRoot = new PropertyLookup(
            [],
            GIT_ROOT_PROPERTY,
            null
    )

    static final PropertyLookup maxGitRootSearchDepth = new PropertyLookup(
            "VERSION_BUILDER_MAX_GIT_ROOT_SEARCH_DEPTH",
            "versionBuilder.maxGitRootSearchDepth",
            3)

    static final PropertyLookup version = new PropertyLookup(
            "VERSION_BUILDER_VERSION",
            "versionBuilder.version",
            null)

    static final PropertyLookup versionScheme = new PropertyLookup(
            "VERSION_BUILDER_VERSION_SCHEME",
            ["versionBuilder.versionScheme", "version.scheme"],
            VersionSchemes.semver)

    static final PropertyLookup versionCodeScheme = new PropertyLookup(
            "VERSION_BUILDER_VERSION_CODE_SCHEME",
            "versionBuilder.versionCodeScheme",
            VersionCodeSchemes.none)

    static final PropertyLookup versionCodeOffset = new PropertyLookup(
            "VERSION_BUILDER_VERSION_CODE_OFFSET",
            "versionBuilder.versionCodeOffset",
            0)


    static final PropertyLookup  prefix = new PropertyLookup(
            "VERSION_BUILDER_PREFIX",
            "versionBuilder.prefix",
            "v")

    static final PropertyLookup mainBranchPattern = new PropertyLookup(
            "VERSION_BUILDER_MAIN_BRANCH_PATTERN",
            "versionBuilder.mainBranchPattern",
            /(^master$|^develop$)/)

    static final PropertyLookup releaseBranchPattern = new PropertyLookup(
            "VERSION_BUILDER_RELEASE_BRANCH_PATTERN",
            "versionBuilder.releaseBranchPattern",
            /(^release\/.*|^master$)/)

    static final PropertyLookup stage = new PropertyLookup(
            "VERSION_BUILDER_STAGE",
            ["versionBuilder.stage", "release.stage"],
            null)

    static final PropertyLookup scope = new PropertyLookup(
            "VERSION_BUILDER_SCOPE",
            ["versionBuilder.scope", "release.scope"],
            null)
}
