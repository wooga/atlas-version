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
import org.apache.commons.lang3.StringUtils

/**
 * Parses version based on a prefix. It can try to parse without a prefix if its necessary.
 */
class PrefixVersionParser implements VersionParser {

    final String prefix
    final boolean tryParseWoutPrefix

    PrefixVersionParser(String prefix, boolean tryParseWoutPrefix) {
        this.prefix = prefix
        this.tryParseWoutPrefix = tryParseWoutPrefix
    }

    @Override
    Version parse(String rawVersion) {
        try {
            if (tryParseWoutPrefix && !rawVersion.startsWith(prefix)) {
                return Version.valueOf(rawVersion)
            } else if (rawVersion.startsWith(prefix)) {
                return Version.valueOf(StringUtils.removeStart(rawVersion, prefix))
            }
        } catch (ParseException ignore) {
        }
        return null
    }

}
