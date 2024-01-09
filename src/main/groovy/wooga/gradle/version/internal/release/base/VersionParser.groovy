package wooga.gradle.version.internal.release.base

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Tag

interface VersionParser {
    /**
     * Parses rawVersion into proper version strings. Should return null if rawVersion does not fit an expected pattern.
     * @param rawVersion
     * @return parsed rawVersion into Version object.
     */
    Version parse(String rawVersion)
}
