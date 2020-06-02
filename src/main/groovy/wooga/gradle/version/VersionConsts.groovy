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

class VersionConsts {
    static final String GIT_ROOT_PROPERTY = "git.root"
    static final String UNINITIALIZED_VERSION = '0.1.0-dev.0.uninitialized'

    static final VersionScheme VERSION_SCHEME_DEFAULT = VersionScheme.semver
    static final VersionCodeScheme VERSION_CODE_SCHEME_DEFAULT = VersionCodeScheme.none

    static final String LEGACY_VERSION_SCHEME_OPTION = "version.scheme"
    static final String LEGACY_VERSION_SCOPE_OPTION = "release.scope"
    static final String LEGACY_VERSION_STAGE_OPTION = "release.stage"

    static final String VERSION_SCHEME_OPTION = "versionBuilder.versionScheme"
    static final String VERSION_SCHEME_ENV_VAR = "VERSION_BUILDER_VERSION_SCHEME"

    static final String VERSION_CODE_SCHEME_OPTION = "versionBuilder.versionCodeScheme"
    static final String VERSION_CODE_SCHEME_ENV_VAR = "VERSION_BUILDER_VERSION_CODE_SCHEME"

    static final String VERSION_CODE_OFFSET_OPTION = "versionBuilder.versionCodeOffset"
    static final String VERSION_CODE_OFFSET_ENV_VAR = "VERSION_BUILDER_VERSION_CODE_OFFSET"

    static final String VERSION_SCOPE_OPTION = "versionBuilder.scope"
    static final String VERSION_SCOPE_ENV_VAR = "VERSION_BUILDER_SCOPE"

    static final String VERSION_STAGE_OPTION = "versionBuilder.stage"
    static final String VERSION_STAGE_ENV_VAR = "VERSION_BUILDER_STAGE"
}
