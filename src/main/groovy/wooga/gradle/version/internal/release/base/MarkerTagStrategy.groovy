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

package wooga.gradle.version.internal.release.base

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Tag

class MarkerTagStrategy implements TagStrategy {

    private final String prefix

    MarkerTagStrategy(String prefix) {
        this.prefix = prefix
    }

    @Override
    Version parseTag(Tag tag) {
        try {
            if (tag.name.startsWith(prefix)) {
                Version.valueOf(tag.name.replace(prefix, ""))
            } else {
                null
            }

        } catch (ParseException e) {
            null
        }
    }
}
