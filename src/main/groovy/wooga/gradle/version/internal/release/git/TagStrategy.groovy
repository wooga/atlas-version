package wooga.gradle.version.internal.release.git

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Tag

interface TagStrategy {
    //TODO: decouple this from git by making it accept a String with a tag name instead of Tag
    Version parseTag(Tag tag)
}
