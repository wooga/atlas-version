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
package wooga.gradle.version.internal.release.base

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Tag


/**
 * Strategy for creating a Git tag associated with a release.
 */
class BasicTagStrategy implements TagStrategy {
    final boolean parseVersionsWithoutPrefixV

    /**
     * Closure taking a {@link Tag tag} as an argument and returning a {@link Version version} if the tag could be
     * parsed, else '<code>null</code>'
     */
    Closure<Version> parseTag = { Tag tag ->
        try {
            if (parseVersionsWithoutPrefixV) {
                Version.valueOf(tag.name[0] == 'v' ? tag.name[1..-1] : tag.name)
            } else {
                Version.valueOf(tag.name[1..-1])
            }

        } catch (ParseException e) {
            null
        }
    }

    BasicTagStrategy(boolean parseVersionsWithoutPrefixV) {
        this.parseVersionsWithoutPrefixV = parseVersionsWithoutPrefixV
    }

    @Override
    Version parseTag(Tag tag) {
        return parseTag.call(tag)
    }

}
