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

package wooga.gradle.version.internal.release.git

import com.github.zafarkhaja.semver.ParseException
import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Tag
import org.apache.commons.lang3.StringUtils

class PrefixTagStrategy implements TagStrategy {

    final String prefix
    final boolean tryParseWoutPrefix

    PrefixTagStrategy(String prefix, boolean tryParseWoutPrefix) {
        this.prefix = prefix
        this.tryParseWoutPrefix = tryParseWoutPrefix
    }

    @Override
    Version parseTag(Tag tag) {
        try {
            if (tryParseWoutPrefix && !tag.name.startsWith(prefix)) {
                return Version.valueOf(tag.name)
            } else if (tag.name.startsWith(prefix)) {
                return Version.valueOf(StringUtils.removeStart(tag.name, prefix))
            }
        } catch (ParseException ignore) {
        }
        return null
    }

}
