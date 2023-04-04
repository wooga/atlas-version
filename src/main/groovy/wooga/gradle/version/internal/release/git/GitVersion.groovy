package wooga.gradle.version.internal.release.git

import com.github.zafarkhaja.semver.Version
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Tag
import wooga.gradle.version.internal.release.semver.NearestVersion

class GitVersion {

    Version version
    Tag tag
    Commit commit
    int distance

    static GitVersion empty(int distance) {
        return new GitVersion(NearestVersion.EMPTY, null, null, distance)
    }

    GitVersion(Version version, Tag tag, Commit rev, int distance) {
        this.version = version
        this.tag = tag
        this.commit = rev
        this.distance = distance
    }
}