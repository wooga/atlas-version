package wooga.gradle.version.internal.release.base

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Tag

interface VersionParser {
    Version parse(String rawVersion)
}
