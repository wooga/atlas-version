/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wooga.gradle.version.internal.release.semver

import groovy.transform.Immutable

import com.github.zafarkhaja.semver.Version

/**
 * Nearest version tags reachable from the current HEAD. The version 0.0.0
 * will be returned for any
 * @since 0.8.0
 */
@Immutable(knownImmutableClasses=[Version])
class NearestVersion {
    static final Version EMPTY = Version.valueOf('0.0.0')

    /**
     * The nearest version that is tagged.
     */
    Version any

    /**
     * The nearest normal (i.e. non-prerelease) version that is tagged.
     */
    Version normal

    /**
     * The number of commits since {@code any} reachable from HEAD.
     */
    int distanceFromAny

    /**
     * The number of commits since {@code normal} reachable from HEAD.
     */
    int distanceFromNormal
}
